package com.healthmine.testforge.template

import com.healthmine.testforge.template.dtos.BasicEisRequest
import com.healthmine.testforge.template.dtos.BasicEisResponse
import com.healthmine.testforge.template.entities.EmployerIncentiveStrategy
import com.healthmine.testforge.template.entities.EmployerIncentiveStrategyId
import com.healthmine.testforge.template.entities.FeatureEmployerGroupXref
import com.healthmine.testforge.template.entities.FeatureEmployerGroupXrefId
import com.healthmine.testforge.template.repositories.ClientRepository
import com.healthmine.testforge.template.repositories.CompliancePeriodRepository
import com.healthmine.testforge.template.repositories.EmployerIncentiveStrategyRepository
import com.healthmine.testforge.template.repositories.FeatureEmployerGroupXrefRepository
import com.healthmine.testforge.template.repositories.IncentiveStrategyRepository
import com.healthmine.testforge.utility.logger
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.CallableStatement
import java.sql.Date
import java.sql.Types
import java.time.LocalDate

@Service
class BasicEisService(
    private val clientRepo: ClientRepository,
    private val compliancePeriodRepo: CompliancePeriodRepository,
    private val incentiveStrategyRepo: IncentiveStrategyRepository,
    private val eisRepo: EmployerIncentiveStrategyRepository,
    private val featureRepo: FeatureEmployerGroupXrefRepository,
    private val entityManager: EntityManager,
    private val jdbcTemplate: JdbcTemplate
) {
    private val log = logger

    @Transactional
    fun setup(request: BasicEisRequest): BasicEisResponse {
        log.info("Starting basic EIS setup for session={}", request.testSession)

        val client = clientRepo.findFirst()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No client found")

        val strategy = incentiveStrategyRepo.findTopByStrategyCdOrderByIdDesc(request.strategyCd)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Incentive strategy not found: ${request.strategyCd}")

        val compliancePeriod = compliancePeriodRepo.findCurrent()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No active compliance period found")

        log.info("clientId={}, strategyId={}, cpId={}", client.id, strategy.id, compliancePeriod.id)

        // Steps 4, 5, 8: ah_test.test_coverage is an Oracle PL/SQL record type that cannot cross the
        // JDBC boundary, so employer group, medical plan, and test case creation live in one block.
        val (employerGroupId, medicalPlanId, memberId) =
            createTestSetup(request.testSession, compliancePeriod.startDate)

        log.info("Created employerGroupId={}, medicalPlanId={}, memberId={}", employerGroupId, medicalPlanId, memberId)

        // Step 6: Upsert employer_incentive_strategy (replaces MERGE — insert only if absent)
        val eisId = EmployerIncentiveStrategyId(employerGroupId, strategy.id, compliancePeriod.id)
        if (!eisRepo.existsById(eisId)) {
            eisRepo.save(EmployerIncentiveStrategy(eisId))
        }

        // Step 7: Upsert feature flags for each health action code (replaces MERGE)
        request.healthActionCodes.forEach { code ->
            val xrefId = FeatureEmployerGroupXrefId(employerGroupId, "healthaction:${code.lowercase()}")
            featureRepo.findById(xrefId)
                .orElse(FeatureEmployerGroupXref(xrefId))
                .also { it.isEnabled = 1 }
                .let { featureRepo.save(it) }
        }

        // Step 9: Enable SSO
        callProc("ah_test.enable_sso_for_session(?, v_web_reg_date => ?)",
            request.testSession, Date.valueOf(compliancePeriod.startDate))

        // Step 10: Optional online registration
        if (request.shouldOnlineReg == true) {
            callProc("ah_test.online_web_reg(?, ?, ?, ?)",
                memberId, request.contactEmail, request.contactNumber, request.mfaType)
        }

        return BasicEisResponse(
            testSession = request.testSession,
            memberId = memberId,
            employerGroupId = employerGroupId,
            medicalPlanId = medicalPlanId,
            incentiveStrategyId = strategy.id
        )
    }

    // Called by the controller AFTER setup() commits, so views reflect the committed data.
    // Not @Transactional — JdbcTemplate manages its own auto-committed connection per call.
    fun processAndWait(testSession: String) {
        jdbcTemplate.update("BEGIN ah_test.refresh_ah_mviews(); END;")
        jdbcTemplate.update("BEGIN ah_test.run_ah_for_session(?); END;", testSession)
        Thread.sleep(5_000)
    }

    // Employer group, medical plan, and test case creation in one anonymous PL/SQL block.
    // ah_test.test_coverage is a PL/SQL record type — it stays Oracle-side.
    // Returns (employerGroupId, medicalPlanId, memberId).
    private fun createTestSetup(testSession: String, cpStartDate: LocalDate): Triple<Long, Long, Long> =
        hibernateSession().doReturningWork { conn ->
            conn.prepareCall(
                """
                DECLARE
                  v_cov ah_test.test_coverage;
                  v_tc  ah_test.test_case;
                  v_eg  NUMBER;
                  v_mp  NUMBER;
                BEGIN
                  v_eg  := ah_test.create_test_employer_group(v_session_id => ?);
                  v_mp  := ah_test.create_test_medical_plan(v_session_id => ?, v_medical_plan_type_code => 'UNK');
                  v_cov := ah_test.test_coverage(employer_group_id => v_eg, medical_plan_id => v_mp);
                  v_tc  := ah_test.create_test_case(
                             'basic_eis',
                             v_case_id    => 0,
                             v_session_id => ?,
                             v_dob        => ah_test.iso_date('1985-01-01'),
                             v_gender     => 'F',
                             v_start_date => ?,
                             v_coverages  => ah_test.test_coverages(v_cov)
                           );
                  ? := v_eg;
                  ? := v_mp;
                  ? := v_tc.member_id;
                END;
                """.trimIndent()
            ).use { cs: CallableStatement ->
                cs.setString(1, testSession)
                cs.setString(2, testSession)
                cs.setString(3, testSession)
                cs.setDate(4, Date.valueOf(cpStartDate))
                cs.registerOutParameter(5, Types.NUMERIC)
                cs.registerOutParameter(6, Types.NUMERIC)
                cs.registerOutParameter(7, Types.NUMERIC)
                cs.execute()
                Triple(cs.getLong(5), cs.getLong(6), cs.getLong(7))
            }
        }

    private fun callProc(plsql: String, vararg params: Any?) {
        hibernateSession().doWork { conn ->
            conn.prepareCall("BEGIN $plsql; END;").use { cs ->
                params.forEachIndexed { i, v ->
                    when (v) {
                        null -> cs.setNull(i + 1, Types.NULL)
                        is String -> cs.setString(i + 1, v)
                        is Long -> cs.setLong(i + 1, v)
                        is Int -> cs.setInt(i + 1, v)
                        is Date -> cs.setDate(i + 1, v)
                        is LocalDate -> cs.setDate(i + 1, Date.valueOf(v))
                        else -> cs.setObject(i + 1, v)
                    }
                }
                cs.execute()
            }
        }
    }

    private fun hibernateSession(): Session = entityManager.unwrap(Session::class.java)
}

package com.healthmine.testforge.template.repositories

import com.healthmine.testforge.template.entities.CompliancePeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CompliancePeriodRepository : JpaRepository<CompliancePeriod, Long> {
    @Query("SELECT cp FROM CompliancePeriod cp WHERE :now BETWEEN cp.startDate AND cp.endDate ORDER BY cp.id DESC LIMIT 1")
    fun findCurrent(now: LocalDate = LocalDate.now()): CompliancePeriod?
}

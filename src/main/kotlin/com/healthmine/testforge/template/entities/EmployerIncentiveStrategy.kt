package com.healthmine.testforge.template.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable

@Embeddable
class EmployerIncentiveStrategyId(
    @Column(name = "EMPLOYER_GROUP_ID")
    val employerGroupId: Long = 0,

    @Column(name = "INCENTIVE_STRATEGY_ID")
    val incentiveStrategyId: Long = 0,

    @Column(name = "COMPLIANCE_PERIOD_ID")
    val compliancePeriodId: Long = 0
) : Serializable

@Entity
@Table(name = "EMPLOYER_INCENTIVE_STRATEGY")
class EmployerIncentiveStrategy(
    @EmbeddedId
    val id: EmployerIncentiveStrategyId,

    @Column(name = "INCENTIVE_CATALOG_CD")
    val incentiveCatalogCd: String = "IncentiveProduct25"
)

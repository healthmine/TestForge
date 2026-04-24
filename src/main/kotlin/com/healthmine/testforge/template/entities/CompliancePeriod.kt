package com.healthmine.testforge.template.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "COMPLIANCE_PERIOD", schema = "COM")
class CompliancePeriod(
    @Id
    @Column(name = "ID")
    val id: Long = 0,

    @Column(name = "START_DATE")
    val startDate: LocalDate = LocalDate.MIN,

    @Column(name = "END_DATE")
    val endDate: LocalDate = LocalDate.MAX
)

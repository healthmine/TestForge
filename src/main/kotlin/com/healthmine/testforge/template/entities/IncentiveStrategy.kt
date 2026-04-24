package com.healthmine.testforge.template.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable

@Entity
@Immutable
@Table(name = "INCENTIVE_STRATEGY", schema = "COM")
class IncentiveStrategy(
    @Id
    @Column(name = "ID")
    val id: Long = 0,

    @Column(name = "STRATEGY_CD")
    val strategyCd: String = ""
)

package com.healthmine.testforge.template.repositories

import com.healthmine.testforge.template.entities.IncentiveStrategy
import org.springframework.data.jpa.repository.JpaRepository

interface IncentiveStrategyRepository : JpaRepository<IncentiveStrategy, Long> {
    fun findTopByStrategyCdOrderByIdDesc(strategyCd: String): IncentiveStrategy?
}

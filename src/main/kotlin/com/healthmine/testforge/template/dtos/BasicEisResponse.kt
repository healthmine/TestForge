package com.healthmine.testforge.template.dtos

data class BasicEisResponse(
    val testSession: String,
    val memberId: Long,
    val employerGroupId: Long,
    val medicalPlanId: Long,
    val incentiveStrategyId: Long,
    val username: String,
    val password: String
)

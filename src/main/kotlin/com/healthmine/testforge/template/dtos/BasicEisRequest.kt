package com.healthmine.testforge.template.dtos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class BasicEisRequest(
    @field:NotBlank val testSession: String,
    @field:NotEmpty val healthActionCodes: List<String>,
    @field:NotBlank val strategyCd: String,
    val shouldOnlineReg: Boolean = false,
    val contactEmail: String? = null,
    val contactNumber: String? = null,
    val mfaType: String? = null
)

package com.healthmine.testforge.template.api

import com.healthmine.testforge.template.BasicEisService
import com.healthmine.testforge.template.dtos.BasicEisRequest
import com.healthmine.testforge.template.dtos.BasicEisResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/template")
class TemplateController(private val basicEisService: BasicEisService) {

    @PostMapping("/basic-eis")
    fun basicEis(@Valid @RequestBody request: BasicEisRequest): ResponseEntity<BasicEisResponse> {
        val response = basicEisService.setup(request)
        basicEisService.processAndWait(request.testSession)
        return ResponseEntity.ok(response)
    }
}

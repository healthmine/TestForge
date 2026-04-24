package com.healthmine.testforge.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "testforge.jwt")
class JwtProperties {
    var secret: String = ""
    var expirationTime: Long = 3_600_000
}

package com.healthmine.testforge.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "testforge.test-member")
class TestMemberProperties {
    var password: String = ""
}

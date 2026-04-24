package com.healthmine.testforge.config

object SecurityConstants {
    val ANONYMOUS_ROUTES = arrayOf(
        "/actuator/health",
        "/jwt-test/**"
    )

    val FULLY_QUALIFIED_ANONYMOUS_ROUTES = ANONYMOUS_ROUTES.map { "/testforge/api$it" }
}

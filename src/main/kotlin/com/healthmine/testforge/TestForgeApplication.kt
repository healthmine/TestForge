package com.healthmine.testforge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestForgeApplication

fun main(args: Array<String>) {
    runApplication<TestForgeApplication>(*args)
}

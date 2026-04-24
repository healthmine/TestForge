package com.healthmine.testforge.utility

class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)
class MissingConfigurationException(message: String) : RuntimeException(message)

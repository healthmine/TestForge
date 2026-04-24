package com.healthmine.testforge.utility

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.logger: Logger
    get() = LoggerFactory.getLogger(T::class.java)

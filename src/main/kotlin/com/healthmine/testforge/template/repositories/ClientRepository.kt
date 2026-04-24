package com.healthmine.testforge.template.repositories

import com.healthmine.testforge.template.entities.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClientRepository : JpaRepository<Client, Long> {
    @Query("SELECT c FROM Client c ORDER BY c.id ASC LIMIT 1")
    fun findFirst(): Client?
}

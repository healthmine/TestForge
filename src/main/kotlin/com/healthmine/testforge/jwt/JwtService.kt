package com.healthmine.testforge.jwt

import com.healthmine.testforge.config.JwtProperties
import com.healthmine.testforge.utility.logger
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(private val jwtProperties: JwtProperties) {
    private val log = logger

    init {
        log.info("JwtService initialized with secret length: {}", jwtProperties.secret.length)
    }

    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    fun extractRoles(token: String): List<String> = extractClaim(token) {
        @Suppress("UNCHECKED_CAST")
        (it["roles"] as? List<String>) ?: emptyList()
    }

    fun isTokenValid(token: String): Boolean = try {
        val exp = extractClaim(token, Claims::getExpiration)
        val valid = !exp.before(Date())
        log.debug("Token expiration check: exp={}, valid={}", exp, valid)
        valid
    } catch (e: JwtException) {
        log.warn("JWT Exception validating token: {}", e.message)
        false
    } catch (e: IllegalArgumentException) {
        log.warn("Illegal Argument Exception validating token: {}", e.message)
        false
    } catch (e: Exception) {
        log.error("Unexpected exception validating token", e)
        false
    }

    private fun <T> extractClaim(token: String, resolver: (Claims) -> T): T =
        resolver(parseClaims(token))

    private fun parseClaims(token: String): Claims {
        log.debug("Parsing JWT token")
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun signingKey(): SecretKey =
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
}

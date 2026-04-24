package com.healthmine.testforge.jwt

import com.healthmine.testforge.config.JwtProperties
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

    fun extractUsername(token: String): String = extractClaim(token, Claims::getSubject)

    fun extractRoles(token: String): List<String> = extractClaim(token) {
        @Suppress("UNCHECKED_CAST")
        (it["roles"] as? List<String>) ?: emptyList()
    }

    fun isTokenValid(token: String): Boolean = try {
        !extractClaim(token, Claims::getExpiration).before(Date())
    } catch (e: JwtException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun <T> extractClaim(token: String, resolver: (Claims) -> T): T =
        resolver(parseClaims(token))

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload

    private fun signingKey(): SecretKey =
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))
}

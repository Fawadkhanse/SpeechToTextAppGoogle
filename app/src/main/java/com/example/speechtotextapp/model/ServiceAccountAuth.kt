package com.example.speechtotextapp.model

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Generates short-lived OAuth2 Bearer tokens from a Google Service Account.
 *
 * Flow:
 *   1. Build a JWT (header.payload) signed with the RSA private key
 *   2. POST it to https://oauth2.googleapis.com/token
 *   3. Get back an access_token valid for 1 hour
 *   4. Cache it — reuse until 5 minutes before expiry
 */
object ServiceAccountAuth {

    private const val TAG       = "ServiceAccountAuth"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    private const val SCOPE     = "https://www.googleapis.com/auth/cloud-platform"

    // ── Service account credentials ───────────────────────────────────────────
    // ⚠️  In production: load these from encrypted storage, NOT hardcoded.
    //     For this demo app they are embedded for simplicity.
    private const val CLIENT_EMAIL =
        "speech-v2@hale-carport-488819-q4.iam.gserviceaccount.com"

    private const val PRIVATE_KEY_PEM = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDarMVGK4BH+Srw
ktiR6tZmpQuKxdTT6+8iLaJiJiyDiJK82yU5lFtk2qjul2d/CyCDX+QZ56CdOY2Q
lSTfU4vePz7hZ+ojRh01X8gYHcn4KAt2CxFt968xglnjcSmuFhtiPoADLDUThRkT
9CG1IhFf0IfP0R5h2rhdUWhaH8zYcvZWLfnDKhWYMXcEekjKyYx0+a5Ra/KFQOLw
kFpZpPpRgfD50Ju7eJ0A2dAESdhY1j4IgEzUki0fPj1jRdbb1otkNuC2d6WwlB2a
S9cfwjHwR/CXixrJ1eqb7WkNjP5Wt577p3XwZmap1opmImI6u379NyiJ8/1cTVRD
P58o+uXdAgMBAAECggEALIblJZhqht1K+9Ee9blonl6JluCZKWzflOlPaSZmRoNL
A9cowYx+vq7SmsCsI5+fp3ihYQ/78b/cHl5sM2hW8PKWan4HHFckz+84toZsT5JR
4R+4VRjL7OwiwxASIV9hhJP+64Z3KUofFOPPcEtnV8gFu5CUm+7WPzQ6KC3U/5aq
jdABo6WOo8bWyhnrkTD3M/YWsOORArDQHhqvU8NdUQ3T7dJhHG+vLjMqQGScGp/r
sN8doczruofppjTiYhGsYOIfoGsbd7+P7D5GWc6WfOQYEA6/26/uUDm8WBPAf91B
zgsoa8qPLfhYQ4ViOQNhFJ72BhJYGg0g49OLvwZg8QKBgQD+Gq2xEQ3YAP0Plft2
spW+mdCLOGUJ7QnJQmVs5HhIGbsOTorxvjjC5jzbpFLX4EH41oMV+2lMyIUNSLME
T44NIzJHAI9Il2QzrPeThwpa2ITPZpQVeLIpfCJC8tLKynxaXr+TKBhZHE4CvHwC
b0LYPyfRr8BR5cPd1PVUPbA08QKBgQDcTmyvH52wzjp4NXQs5J80lKt7VUIrJuCU
zpFXJBnj0sg+NJPS8Kx4z9fh9lozPZqF24SgUP5f4vAvCqfCdWpPXkShVdJ/H4vC
7jtWhj2gJsbPGhKrs7zZUQSFUiRd2GeRuaEOVb2wAOwLh0KxEWhpH/F8oj7yRsK6
ChYGjp0PrQKBgA4k7gYtLNgZNfzoHFc/GZbCeRlGylkDGMhbKcol7YwV4pOpS5Kp
Q/+VUU3ol7Psh7+SMTnIBNSBVOaoZU6YHxAcJXBOV6tyweEef6l2mtzzsHDbBOMt
FL26ay3O1mzzWHivTXqjgLd1G+KLG1wHVXE0EsNZRRtJ7t0qPX2y8VwBAoGAYpdS
OjkK6AIS1pMNb73MpcpWx7YLC6a1YMLk9jt4vqUo6fW7pe4BMXvKYBxQl5fdHER2
IQy+GglEdbjuBK9pKSXFzvHKZwumD1FwCrO+xno0BKDldCPWwuZoAIYXMkxTZTrO
ocyrPCXdfPdGWFmzAUDDYIR3aRNTt9AltT+DeG0CgYEAzGIBSlU2qFJCkQeL3QaY
ozBHeoG8k5LkqvljxZ9N7/EAxR8rYdHAZzYCfEuXllvWXe4kuWURliGfQRDv8yzo
QdrHQM3SEPqr+0k98ZUl++JOe1T3pQP2nfq6NYo+Tq+fzHk+P3KtPTPkAEVblhep
UOa35pOe/mT75TxugNsG7A8=
-----END PRIVATE KEY-----"""

    // ── Token cache ───────────────────────────────────────────────────────────
    private var cachedToken: String? = null
    private var tokenExpiryMs: Long  = 0L

    /**
     * Returns a valid Bearer token, fetching a new one if needed.
     * Call from a coroutine (uses Dispatchers.IO internally).
     */
    suspend fun getBearerToken(): String = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        if (cachedToken != null && nowMs < tokenExpiryMs - 5 * 60 * 1000) {
            Log.d(TAG, "Using cached token")
            return@withContext cachedToken!!
        }
        Log.d(TAG, "Fetching new OAuth2 token...")
        val token = fetchNewToken()
        cachedToken   = token
        tokenExpiryMs = nowMs + 3600_000L
        token
    }

    private fun fetchNewToken(): String {
        val nowSec = System.currentTimeMillis() / 1000
        val expSec = nowSec + 3600

        val header  = base64url("""{"alg":"RS256","typ":"JWT"}""")
        val payload = base64url("""
            {
              "iss":   "$CLIENT_EMAIL",
              "scope": "$SCOPE",
              "aud":   "$TOKEN_URL",
              "iat":   $nowSec,
              "exp":   $expSec
            }
        """.trimIndent())

        val signingInput = "$header.$payload"
        val privateKey   = loadPrivateKey()
        val sig          = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.US_ASCII))
        }.sign()
        val signature = Base64.encodeToString(sig, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val jwt = "$signingInput.$signature"

        val postBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt"
        val conn = (URL(TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 15_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { it.write(postBody.toByteArray()) }

        val responseCode = conn.responseCode
        val body = if (responseCode == 200)
            conn.inputStream.bufferedReader().readText()
        else
            conn.errorStream.bufferedReader().readText()
        conn.disconnect()

        if (responseCode != 200) {
            Log.e(TAG, "Token fetch failed $responseCode: $body")
            throw RuntimeException("OAuth2 token error $responseCode: $body")
        }

        return JSONObject(body).getString("access_token")
    }

    private fun loadPrivateKey(): PrivateKey {
        val cleaned = PRIVATE_KEY_PEM
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .trim()
        val decoded = Base64.decode(cleaned, Base64.DEFAULT)
        return KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(decoded))
    }

    private fun base64url(json: String): String =
        Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
}
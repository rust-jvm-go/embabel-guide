package com.embabel.hub.integrations

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Configuration
class DeepgramConfig {
    @Bean
    @Qualifier("deepgramRestClient")
    fun deepgramRestClient(
        builder: RestClient.Builder,
        @Value("\${deepgram.api.base-url:https://api.deepgram.com}") baseUrl: String,
    ): RestClient = builder.baseUrl(baseUrl).build()
}

@RestController
@RequestMapping("/api/v1/deepgram")
class DeepgramProxyController(
    @Qualifier("deepgramRestClient") private val restClient: RestClient,
) {

    private val logger = LoggerFactory.getLogger(DeepgramProxyController::class.java)

    @GetMapping("/models")
    fun getModels(
        @RequestHeader("x-deepgram-key", required = false) apiKey: String?,
    ): ResponseEntity<String> {
        if (apiKey.isNullOrBlank()) {
            return ResponseEntity.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"error":"Missing Deepgram API key"}""")
        }

        return try {
            val body = restClient.get()
                .uri("/v1/models")
                .header("Authorization", "Token $apiKey")
                .retrieve()
                .body(String::class.java)

            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
        } catch (e: RestClientResponseException) {
            ResponseEntity.status(e.statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"error":"Failed to fetch models"}""")
        } catch (e: Exception) {
            logger.error("Failed to proxy Deepgram models request: {}", e.message, e)
            ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"error":"Internal server error"}""")
        }
    }
}
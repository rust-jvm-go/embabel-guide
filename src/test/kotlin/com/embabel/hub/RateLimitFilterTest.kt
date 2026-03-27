package com.embabel.hub

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RateLimitFilterTest {

    private val filter = RateLimitFilter()

    @Test
    fun `normal request passes through`() {
        val request = MockHttpServletRequest("GET", "/api/hub/sessions")
        request.remoteAddr = "10.0.0.1"
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        assertNotNull(chain.request, "Filter chain should have been invoked")
    }

    @Test
    fun `tight-path requests are limited at 40 per minute`() {
        val ip = "10.0.0.2"

        repeat(40) { i ->
            val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
            req.remoteAddr = ip
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, MockFilterChain())
            assertEquals(200, res.status, "Request $i should pass")
        }

        val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
        req.remoteAddr = ip
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, MockFilterChain())
        assertEquals(429, res.status, "41st request should be rate-limited")
    }

    @Test
    fun `global limit applies to all requests`() {
        val ip = "10.0.0.3"

        repeat(300) { i ->
            val req = MockHttpServletRequest("GET", "/api/hub/sessions")
            req.remoteAddr = ip
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, MockFilterChain())
            assertEquals(200, res.status, "Request $i should pass")
        }

        val req = MockHttpServletRequest("GET", "/api/hub/sessions")
        req.remoteAddr = ip
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, MockFilterChain())
        assertEquals(429, res.status, "301st request should be rate-limited")
    }

    @Test
    fun `different IPs have independent limits`() {
        repeat(10) {
            val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
            req.remoteAddr = "10.0.0.10"
            filter.doFilter(req, MockHttpServletResponse(), MockFilterChain())
        }

        // A different IP should still be allowed
        val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
        req.remoteAddr = "10.0.0.11"
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, MockFilterChain())
        assertEquals(200, res.status)
    }

    @Test
    fun `tight-path request also counts against global limit`() {
        val ip = "10.0.0.20"

        // Burn 295 global tokens on normal requests
        repeat(295) {
            val req = MockHttpServletRequest("GET", "/api/hub/sessions")
            req.remoteAddr = ip
            filter.doFilter(req, MockHttpServletResponse(), MockFilterChain())
        }

        // 5 validate requests should pass (tight limit allows 10, global allows 5 more)
        repeat(5) { i ->
            val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
            req.remoteAddr = ip
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, MockFilterChain())
            assertEquals(200, res.status, "Validate request $i should pass")
        }

        // Next validate request should hit global limit
        val req = MockHttpServletRequest("POST", "/api/hub/integrations/keys/validate")
        req.remoteAddr = ip
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, MockFilterChain())
        assertEquals(429, res.status, "Should hit global limit")
    }
}
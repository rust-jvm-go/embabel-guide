package com.embabel.guide.rag

import com.embabel.agent.rag.ingestion.ContentFetcher
import com.embabel.agent.rag.ingestion.FetchResult
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Tries [primary] first; on any exception falls back to [fallback].
 * Useful for sites with unreliable/intermittent blocking: prefer a cheap path
 * (e.g. RSS) and only pay the fallback cost (e.g. direct HTTP) when needed.
 */
class FallbackContentFetcher(
    private val primary: ContentFetcher,
    private val fallback: ContentFetcher,
) : ContentFetcher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetch(uri: URI): FetchResult = try {
        primary.fetch(uri)
    } catch (e: Exception) {
        logger.info("Primary fetch failed for {} ({}); trying fallback", uri, e.message)
        fallback.fetch(uri)
    }
}
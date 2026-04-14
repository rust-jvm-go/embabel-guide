package com.embabel.guide.rag

import com.embabel.agent.rag.ingestion.AbstractChunkTransformer
import com.embabel.agent.rag.ingestion.ChunkTransformationContext
import com.embabel.agent.rag.model.Chunk
import com.embabel.guide.GuideProperties

/**
 * Stamps chunks with a `version` metadata property based on the source document URI.
 *
 * - Versioned docs (URI starts with [ContentConfig.versioned.baseUrl]): version extracted from URI path.
 * - Supplementary docs: stamped with "supplementary".
 *
 * This enables version-aware search filtering via [PropertyFilter].
 */
class VersionChunkTransformer(
    private val guideProperties: GuideProperties,
) : AbstractChunkTransformer() {

    override fun additionalMetadata(chunk: Chunk, context: ChunkTransformationContext): Map<String, Any> {
        val uri = context.document?.uri ?: return mapOf("version" to SUPPLEMENTARY)
        val baseUrl = guideProperties.content.versioned.baseUrl

        if (uri.startsWith(baseUrl)) {
            val afterBase = uri.removePrefix(baseUrl).trimEnd('/')
            val version = afterBase.split("/").firstOrNull()
            if (!version.isNullOrBlank()) {
                return mapOf("version" to version)
            }
        }

        return mapOf("version" to SUPPLEMENTARY)
    }

    companion object {
        const val SUPPLEMENTARY = "supplementary"
    }
}

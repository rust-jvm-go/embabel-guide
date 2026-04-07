package com.embabel.guide.domain

import org.drivine.annotation.Direction
import org.drivine.annotation.GraphRelationship
import org.drivine.annotation.GraphView
import org.drivine.annotation.Root

/**
 * GraphView specifically for GuideUsers with anonymous WebUser relationships.
 * The AnonymousWebUserData has labels ["WebUser", "Anonymous"] which ensures
 * only anonymous users are matched.
 */
@GraphView
data class AnonymousGuideUser(
    @Root
    val core: GuideUserData,

    @GraphRelationship(type = "IS_WEB_USER", direction = Direction.OUTGOING)
    val webUser: AnonymousWebUserData,

    @GraphRelationship(type = "USES_PERSONA", direction = Direction.OUTGOING)
    val persona: PersonaData,
) {
    /**
     * Convert to the unified GuideUser type for consistent API.
     */
    fun toGuideUser(): GuideUser = GuideUser(
        core = core,
        webUser = webUser,
        persona = persona,
    )
}

package com.embabel.guide.domain

import com.embabel.agent.api.identity.User
import org.drivine.annotation.Direction
import org.drivine.annotation.GraphRelationship
import org.drivine.annotation.GraphView
import org.drivine.annotation.Root

/**
 * Lightweight GraphView for GuideUser that skips the USES_PERSONA traversal.
 * Use this when only the user's identity data is needed (core + webUser).
 */
@GraphView
data class GuideWebUser(
    @Root
    val core: GuideUserData,

    @GraphRelationship(type = "IS_WEB_USER", direction = Direction.OUTGOING)
    val webUser: WebUserData? = null,
) : User, HasGuideUserData {

    override fun guideUserData(): GuideUserData = core

    override val id: String
        get() = webUser?.id ?: core.id

    override val displayName: String
        get() = webUser?.displayName ?: core.displayName

    override val username: String
        get() = webUser?.userName ?: core.username

    override val email: String?
        get() = webUser?.userEmail
}

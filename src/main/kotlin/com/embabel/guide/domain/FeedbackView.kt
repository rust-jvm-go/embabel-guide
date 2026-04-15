package com.embabel.guide.domain

import org.drivine.annotation.Direction
import org.drivine.annotation.GraphRelationship
import org.drivine.annotation.GraphView
import org.drivine.annotation.Root

@GraphView
data class FeedbackView(
    @Root
    val feedback: FeedbackData,

    @GraphRelationship(type = "GAVE", direction = Direction.INCOMING)
    val user: GuideUserData? = null,
)

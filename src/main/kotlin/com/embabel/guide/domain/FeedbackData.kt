package com.embabel.guide.domain

import org.drivine.annotation.NodeFragment
import org.drivine.annotation.NodeId
import java.time.Instant

@NodeFragment(labels = ["Feedback"])
data class FeedbackData(
    @NodeId
    val id: String,
    val page: String,
    val helpful: Boolean,
    val comment: String? = null,
    val userId: String? = null,
    val createdAt: Instant = Instant.now(),
)

package org.jetbrains.plugins.designer.models

data class Connection(
    val sourceScreenId: String,
    val targetScreenId: String,
    val componentId: String,
    val label: String?
)

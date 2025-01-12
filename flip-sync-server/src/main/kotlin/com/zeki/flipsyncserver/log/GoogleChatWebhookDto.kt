package com.zeki.flipsyncserver.log

data class GoogleChatWebhookDto(
    val text: String,
    val name: String = "",
    val annotations: List<Any> = emptyList(),
    val argumentText: String = "",
    val cards: List<Card> = emptyList(),
    val createTime: String = "",
    val fallbackText: String = "",
    val previewText: String = "",
    val sender: Sender? = null,
    val space: Space? = null,
    val thread: Thread? = null
)

data class Card(
    val cardActions: List<Any> = emptyList(),
    val header: Header? = null,
    val name: String = "",
    val sections: List<Any> = emptyList()
)

data class Header(
    val imageAltText: String = "",
    val imageStyle: String = "",
    val imageUrl: String = "",
    val subtitle: String = "",
    val title: String = ""
)

data class Sender(
    val avatarUrl: String = "",
    val displayName: String = "",
    val email: String = "",
    val name: String = "",
    val type: String = ""
)

data class Space(
    val displayName: String = "",
    val name: String = "",
    val type: String = ""
)

data class Thread(
    val name: String = ""
)
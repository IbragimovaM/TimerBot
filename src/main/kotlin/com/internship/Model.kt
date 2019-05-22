package com.internship

import java.time.Instant

data class Update(
    val message: Message?
)

data class Message(
    val chat: Chat,
    val text: String?
)

data class Chat(
    val id: Long
)

data class RemindMeRequest(
    val msg: Message,
    val instant: Instant
)
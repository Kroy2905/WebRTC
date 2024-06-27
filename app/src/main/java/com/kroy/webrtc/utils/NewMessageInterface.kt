package com.kroy.webrtc.utils

import com.kroy.webrtc.models.MessageModel


interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}
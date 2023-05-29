package dev.franco.securechat.ui.chats

import dev.franco.securechat.domain.PlainMessage

sealed class ChatsViewState {
    object Idle : ChatsViewState()
    class OnLoaded(val messages: List<PlainMessage>) : ChatsViewState()
    class OnInfo(val message: Int) : ChatsViewState()
    object OnEmpty : ChatsViewState()
}

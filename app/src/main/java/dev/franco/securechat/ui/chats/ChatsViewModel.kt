package dev.franco.securechat.ui.chats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.franco.securechat.R
import dev.franco.securechat.di.IoDispatcher
import dev.franco.securechat.usecase.GetMessagesUseCase
import dev.franco.securechat.usecase.ReadMessagesResult
import dev.franco.securechat.usecase.SendMessageResult
import dev.franco.securechat.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    @IoDispatcher private val backgroundDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _viewState = MutableStateFlow<ChatsViewState>(ChatsViewState.Idle)
    val viewState = _viewState.asStateFlow()

    var currentMessage by mutableStateOf("")

    fun getMessages() = viewModelScope.launch(backgroundDispatcher) {
        getMessagesUseCase.getMessages().collect {
            when (it) {
                is ReadMessagesResult.OnSuccess -> {
                    _viewState.value = ChatsViewState.OnLoaded(it.messages)
                }

                is ReadMessagesResult.OnEmpty -> {
                    _viewState.value = ChatsViewState.OnEmpty
                }
            }
        }
    }

    fun sendMessage(message: String, from: String) {
        viewModelScope.launch(backgroundDispatcher) {
            when (sendMessageUseCase.send(message, from)) {
                is SendMessageResult.OnSuccess -> {
                    currentMessage = ""
                }

                is SendMessageResult.OnEmpty -> {
                    _viewState.value = ChatsViewState.OnInfo(R.string.empty_message)
                }
            }
        }
    }
}

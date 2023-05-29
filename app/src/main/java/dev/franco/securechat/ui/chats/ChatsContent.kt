package dev.franco.securechat.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.franco.securechat.R
import dev.franco.securechat.domain.PlainMessage

@Composable
internal fun ChatContent(
    chatsViewModel: ChatsViewModel,
) {
    val state by chatsViewModel.viewState.collectAsState()
    val focusManager = LocalFocusManager.current
    var messages by remember {
        mutableStateOf<List<PlainMessage>>(emptyList())
    }
    val from = stringResource(R.string.another_person)

    LaunchedEffect(Unit) {
        chatsViewModel.getMessages()
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (state) {
            is ChatsViewState.OnLoaded -> {
                messages = (state as ChatsViewState.OnLoaded).messages
                ChatList(
                    messages = messages,
                )
            }

            is ChatsViewState.OnEmpty -> {
                Text(
                    text = stringResource(R.string.no_messages),
                    modifier = Modifier
                        .align(Alignment.Center),
                )
            }

            else -> Unit
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .align(Alignment.BottomCenter),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                value = chatsViewModel.currentMessage,
                onValueChange = {
                    chatsViewModel.currentMessage = it
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.write_down_message_placeholder),
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            chatsViewModel.sendMessage(
                                chatsViewModel.currentMessage,
                                from,
                            )
                        },
                    ) {
                        Icon(imageVector = Icons.Rounded.Send, contentDescription = "")
                    }
                },
                shape = RoundedCornerShape(50),
                keyboardActions = KeyboardActions(
                    onSend = {
                        focusManager.clearFocus()
                        chatsViewModel.sendMessage(chatsViewModel.currentMessage, from)
                    },
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                ),
                colors = TextFieldDefaults.colors(),
            )
        }
    }
}

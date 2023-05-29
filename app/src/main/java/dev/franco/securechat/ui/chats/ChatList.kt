package dev.franco.securechat.ui.chats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.franco.securechat.domain.PlainMessage

@Composable
internal fun ChatList(
    messages: List<PlainMessage>,
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        lazyListState.scrollToItem(messages.size - 1)
    }
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = lazyListState,
            contentPadding = PaddingValues(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 96.dp,
            ),
        ) {
            items(messages) {
                ChatItem(
                    from = it.from,
                    message = it.message,
                    date = it.date,
                    self = it.self,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatsContentPreview() {
    val messages = mutableListOf<PlainMessage>()
    repeat(1) {
        messages.add(
            PlainMessage(
                from = "Franco",
                message = "Hola como estas amigo",
                date = "Wed 24 May 3:17",
                self = true,
            ),
        )
    }
    MaterialTheme {
        ChatList(
            messages = messages,
        )
    }
}

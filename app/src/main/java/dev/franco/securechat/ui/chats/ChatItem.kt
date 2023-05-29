package dev.franco.securechat.ui.chats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatItem(
    from: String,
    message: String,
    date: String,
    modifier: Modifier = Modifier,
    self: Boolean,
) {
    val shape: Shape
    val newModifier = if (self) {
        shape = RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = 16.dp,
        )
        Modifier
            .padding(
                start = 32.dp,
            )
    } else {
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
        )
        Modifier
            .padding(
                end = 32.dp,
            )
    }
    OutlinedCard(
        modifier = modifier
            .then(newModifier)
            .fillMaxWidth()
            .then(modifier),
        shape = shape,
    ) {
        ChatItemContent(
            modifier = Modifier
                .padding(16.dp),
            from = from,
            message = message,
            date = date,
            self = self,
        )
    }
}

@Preview
@Composable
private fun ChatItemPreview() {
    MaterialTheme {
        ChatItem(
            from = "Omar",
            message = "Hola como estas?",
            date = "20:38 23/05/2023",
            self = true,
        )
    }
}

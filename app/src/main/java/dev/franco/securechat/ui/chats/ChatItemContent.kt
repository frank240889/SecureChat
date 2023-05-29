package dev.franco.securechat.ui.chats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.franco.securechat.R

@Composable
internal fun ChatItemContent(
    from: String,
    message: String,
    date: String,
    self: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = if (self) stringResource(R.string.me) else from,
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .weight(0.5f),
            )

            Text(
                text = date,
                textAlign = TextAlign.End,
                style = TextStyle(fontSize = 12.sp),
                modifier = Modifier
                    .weight(0.5f),
            )
        }

        Spacer(
            modifier = Modifier
                .height(8.dp),
        )

        Text(
            text = message,
            style = TextStyle(fontFamily = FontFamily.Monospace),
        )
    }
}

@Preview
@Composable
private fun ChatItemContentPreview() {
    MaterialTheme {
        ChatItemContent(
            from = "Omar",
            message = "Hola como estas?",
            date = "20:38 23/05/2023",
            self = true,
        )
    }
}

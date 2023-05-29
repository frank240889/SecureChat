package dev.franco.securechat.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.franco.securechat.R

@Composable
internal fun StartContent(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.not_connected),
    buttonEnabled: Boolean,
    onConnect: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(
                    24.dp,
                ),
        )

        Spacer(
            modifier = Modifier
                .height(24.dp),
        )

        OutlinedButton(
            onClick = onConnect,
            enabled = buttonEnabled,
        ) {
            Text(
                text = stringResource(R.string.connect),
            )
        }
    }
}

@Preview
@Composable
private fun StartContentPreview() {
    MaterialTheme {
        StartContent(
            onConnect = {},
            buttonEnabled = true,
        )
    }
}

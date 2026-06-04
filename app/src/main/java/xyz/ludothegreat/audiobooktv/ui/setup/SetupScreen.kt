package xyz.ludothegreat.audiobooktv.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun SetupScreen(
    onConnected: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val urlFocus = remember { FocusRequester() }
    val userFocus = remember { FocusRequester() }
    val passFocus = remember { FocusRequester() }

    // Suppress Back on the setup screen so a stray press from the IME closing
    // doesn't kill the app mid-typing. Long-press / Home still works to exit.
    BackHandler(enabled = true) { /* swallow */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(720.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Connect to Audiobookshelf",
                color = colors.onBackground,
                fontSize = 28.sp,
            )

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                cursorColor = colors.primary,
                focusedBorderColor = colors.secondary,
                unfocusedBorderColor = colors.onSurfaceVariant,
                focusedLabelColor = colors.secondary,
                unfocusedLabelColor = colors.onSurfaceVariant,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
            )

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onUrlChange,
                label = { Text("Server URL") },
                placeholder = { Text("http://your-abs-host:13378") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { userFocus.requestFocus() }),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().focusRequester(urlFocus),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passFocus.requestFocus() }),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().focusRequester(userFocus),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().focusRequester(passFocus),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val trustInteractionSource = remember { MutableInteractionSource() }
                val trustFocused by trustInteractionSource.collectIsFocusedAsState()
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .border(
                            width = 2.dp,
                            color = if (trustFocused) colors.secondary else Color.Transparent,
                            shape = RoundedCornerShape(50),
                        )
                        .padding(4.dp),
                ) {
                    Switch(
                        checked = state.trustSelfSignedCert,
                        onCheckedChange = viewModel::onTrustToggle,
                        interactionSource = trustInteractionSource,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.primary,
                            checkedThumbColor = colors.onPrimary,
                            uncheckedTrackColor = colors.surface,
                            uncheckedThumbColor = colors.onSurfaceVariant,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Trust this server's certificate (skip TLS verification)",
                    color = colors.onBackground,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            state.error?.let { msg ->
                Text(
                    text = msg,
                    color = colors.error,
                    fontSize = 18.sp,
                )
            }

            Surface(
                onClick = { viewModel.submit(onSuccess = onConnected) },
                enabled = !state.submitting,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    focusedContainerColor = colors.primary,
                    focusedContentColor = colors.onPrimary,
                    disabledContainerColor = colors.surface,
                    disabledContentColor = colors.onSurfaceVariant,
                ),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, colors.secondary),
                        shape = RoundedCornerShape(8.dp),
                    ),
                ),
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.submitting) "Connecting..." else "Connect",
                        color = colors.onPrimary,
                        fontSize = 22.sp,
                    )
                }
            }
        }
    }
}

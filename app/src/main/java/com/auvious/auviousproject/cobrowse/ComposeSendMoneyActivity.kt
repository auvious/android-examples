package com.auvious.auviousproject.cobrowse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auvious.auvioussdk.cobrowse.AuviousCobrowse
import com.auvious.auvioussdk.cobrowse.auviousMasked

/**
 * Co-browse demo in Jetpack Compose. Compose content is masked with `Modifier.auviousMasked()`;
 * the identifier, class and text input rules only apply to Android Views.
 */
class ComposeSendMoneyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SendMoneyScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun SendMoneyScreen(onBack: () -> Unit) {
    var iban by remember { mutableStateOf("") }
    var card by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var debug by remember { mutableStateOf(AuviousCobrowse.debugHighlightMasks) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Send money (Compose)", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Column(Modifier.demoMasked()) {
                Text("Everyday account · GR16 0110 1250 0000 0001 2300 695")
                Text("Balance €2,430.18", fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            OutlinedTextField(iban, { iban = it }, label = { Text("Recipient IBAN") }, modifier = Modifier.fillMaxWidth().demoMasked())
        }
        item {
            OutlinedTextField(
                card, { card = it },
                label = { Text("Card number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().demoMasked()
            )
        }
        item {
            OutlinedTextField(reference, { reference = it }, label = { Text("Reference (visible to the agent)") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(
                pin, { pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().demoMasked()
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Highlight masks (debug)", modifier = Modifier.weight(1f))
                Switch(debug, { debug = it; AuviousCobrowse.debugHighlightMasks = it })
            }
        }
        item {
            Text("Recent payments", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        }
        // Fling these while sharing: the masked amounts must stay black
        items((1..30).toList()) { index ->
            Column {
                Text("Payment $index")
                Text("GR16 0110 1250 0000 0001 23%02d 695 · €%d.00".format(index, index * 25), modifier = Modifier.demoMasked())
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }
        }
        item {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            TextButton(onClick = { AuviousCobrowse.stop() }, modifier = Modifier.fillMaxWidth()) { Text("End co-browse") }
        }
    }
}

/** `Modifier.auviousMasked()`, unless marked views are turned off on the main screen */
private fun Modifier.demoMasked(): Modifier =
    if (CobrowseDemoSettings.marksViews) auviousMasked() else this

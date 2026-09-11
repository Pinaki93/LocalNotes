package dev.pinaki.localnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import dev.pinaki.localnotes.navigation.LocalNotesNavGraph
import dev.pinaki.localnotes.ui.theme.LocalNotesTheme

class ContainerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalNotesTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    LocalNotesNavGraph()
                }
            }
        }
    }
}

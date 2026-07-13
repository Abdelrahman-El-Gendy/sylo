package com.sylo.com

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.sylo.com.navigation.SyloNavHost
import com.sylo.core.ui.theme.SyloTheme
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity (not ComponentActivity) so androidx BiometricPrompt can attach.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyloTheme {
                // Root Surface paints the brand's dark background behind every screen —
                // including the auth flow and first-run gates, which render outside the
                // main Scaffold and would otherwise fall through to the window default.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SyloNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

package com.daunpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.daunpos.app.ui.theme.DaunPOSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DaunPOSViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaunPOSTheme {
                DaunPOSApp(viewModel)
            }
        }
    }
}

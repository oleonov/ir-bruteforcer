package com.pandorika.irbruteforcer

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.pandorika.irbruteforcer.ui.remote.RemoteRoute
import com.pandorika.irbruteforcer.ui.theme.IRBruteForcerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val dark = SystemBarStyle.dark(Color.parseColor("#17120B"))
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)
        setContent {
            IRBruteForcerTheme {
                RemoteRoute(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

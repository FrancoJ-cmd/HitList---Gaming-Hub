package com.hitlist

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hitlist.presentation.navigation.AppNavGraph

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HitList — Gamer Hub",
        state = rememberWindowState(width = 900.dp, height = 700.dp)
    ) {
        AppNavGraph()
    }
}

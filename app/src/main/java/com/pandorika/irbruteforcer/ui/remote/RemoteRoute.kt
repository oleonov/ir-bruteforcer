package com.pandorika.irbruteforcer.ui.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pandorika.irbruteforcer.remote.RemoteViewModel

@Composable
fun RemoteRoute(modifier: Modifier = Modifier) {
    val viewModel: RemoteViewModel = viewModel(
        factory = RemoteViewModel.factory(LocalContext.current.applicationContext),
    )
    val state by viewModel.state.collectAsState()
    val actions = remember(viewModel) {
        RemoteActions(
            selectCategory = viewModel::selectCategory,
            press = viewModel::press,
            stop = viewModel::stop,
            confirmWorked = viewModel::confirmWorked,
            reset = viewModel::reset,
            cycleDelay = viewModel::cycleDelay,
        )
    }
    RemoteScreen(state, actions, modifier)
}

package com.rpeters.jellyfin.core.architecture

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

abstract class BaseMviViewModel<S : Any, SE : Any, I : Any>(
    initialState: S
) : ViewModel(), ContainerHost<S, SE> {
    override val container = container<S, SE>(initialState)

    abstract fun onIntent(intent: I)
}

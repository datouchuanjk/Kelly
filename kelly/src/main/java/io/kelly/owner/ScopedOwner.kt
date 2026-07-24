package io.kelly.owner

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import io.kelly.KellyInternal

class ScopedOwner(val bundle: Bundle = bundleOf()) : ViewModelStoreOwner,
    SavedStateRegistryOwner,
    LifecycleOwner,
    HasDefaultViewModelProviderFactory {

    private val _lifecycleRegistry = LifecycleRegistry(this)
    private val _savedStateController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()
    private val _creationExtras = MutableCreationExtras().apply {
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@ScopedOwner)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@ScopedOwner)
        set(DEFAULT_ARGS_KEY, bundle)
    }
    private val _factory = SavedStateViewModelFactory(
        application = KellyInternal.context,
        owner = this,
        defaultArgs = bundle
    )

    override val lifecycle: Lifecycle get() = _lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = _savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    override val defaultViewModelCreationExtras: CreationExtras get() = _creationExtras
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() = _factory

    init {
        _savedStateController.performAttach()
        enableSavedStateHandles()
        _savedStateController.performRestore(bundle)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun clear() {
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
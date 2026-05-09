package io.kmpbits.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val getStartDestination: GetStartDestinationUseCase,
) : ViewModel() {

    private val _destination = MutableStateFlow<FirstDestination?>(null)
    val destination: StateFlow<FirstDestination?> = _destination

    init {
        viewModelScope.launch {
            _destination.value = getStartDestination()
        }
    }
}

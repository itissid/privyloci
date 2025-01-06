package me.itissid.privyloci.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.kvrepository.ExperimentsPreferencesManager
import javax.inject.Inject


@HiltViewModel
class ExperimentFlagViewModel @Inject constructor(
    private val experimentStore: ExperimentsPreferencesManager
) : ViewModel() {

    val onboardingExperimentOn: StateFlow<Boolean> = experimentStore.headPhoneOnboardingExperimentOn
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setExperimentFlag(value: Boolean) {
        viewModelScope.launch {
            experimentStore.setExperimentFlag(value)
        }
    }

}
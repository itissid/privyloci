package me.itissid.privyloci.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.kvrepository.ExperimentsPreferencesManager
import javax.inject.Inject


@HiltViewModel
class ExperimentFlagViewModel @Inject constructor(
    private val experimentStore: ExperimentsPreferencesManager
) : ViewModel() {

    val experimentOn: StateFlow<Boolean> = experimentStore.headPhoneOnboardingExperimentOn
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setExperimentFlag(value: Boolean) {
        viewModelScope.launch {
            experimentStore.setExperimentFlag(value)
        }
    }

}
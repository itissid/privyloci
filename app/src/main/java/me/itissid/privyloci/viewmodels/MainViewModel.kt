package me.itissid.privyloci.viewmodels

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.itissid.privyloci.datamodels.PlaceTagDao
import me.itissid.privyloci.kvrepository.UserPreferences
import me.itissid.privyloci.kvrepository.Repository
import me.itissid.privyloci.util.Logger
import javax.inject.Inject

enum class ForeGroundServiceRationaleState {
    /** Dialogue rationale states for controlling the foreground service
     * */
    FG_PERMISSION_RATIONALE_DISMISSED,
    PERSISTENT_NOTIFICATION_DISMISSED,
    REACTIVATE_FG_RATIONALE_DISMISSED,
}

enum class RationaleState {

    /**
    Dialogue rationale states For the foreground permissions.
     * */
    LOCATION_PERMISSION_RATIONALE_SHOULD_BE_SHOWN,
    MULTIPLE_PERMISSIONS_SHOULD_BE_LAUNCHED,
    VISIT_SETTINGS,
    PAUSE_LOCATION_COLLECTION,
    RESUME_LOCATION_COLLECTION,
}

data class ForegroundPermissionRationaleState(
    val reason: RationaleState,
    val rationaleText: String = "",
    val proceedButtonText: String = "Proceed",
    val dismissButtonText: String = "Cancel"

)


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
) :
    AndroidViewModel(application) {

    val isServiceRunning: StateFlow<Boolean>
        get() = repository.isServiceRunning.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(15000L),
            initialValue = false
        )

}

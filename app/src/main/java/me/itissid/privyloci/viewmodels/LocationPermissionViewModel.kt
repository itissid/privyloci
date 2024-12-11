package me.itissid.privyloci.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import me.itissid.privyloci.kvrepository.Repository
import me.itissid.privyloci.kvrepository.UserPreferences
import javax.inject.Inject

class LocationPermissionViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
    private val repository: Repository
) : AndroidViewModel(application) {
// TODO: Move the permission code from MainActvity's MainScreenWrapper to this ViewModel

}

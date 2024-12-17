// File: app/src/test/java/me/itissid/privyloci/viewmodels/BlePermissionViewModelTest.kt
package me.itissid.privyloci.viewmodels

import android.app.Application
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.itissid.privyloci.BaseViewModelTest
import me.itissid.privyloci.kvrepository.UserPreferences
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlePermissionViewModelTest : BaseViewModelTest() {

    // Mock dependencies
    private lateinit var userPreferences: UserPreferences
    private lateinit var bleRepository: BleRepository
    private lateinit var application: Application

    // System Under Test
    private lateinit var viewModel: BlePermissionViewModel

    private val blePermissionsFlow = MutableStateFlow(false)


    @Before
    fun setup() {
        // Initialize MockK annotations
        userPreferences = mockk()
        bleRepository = mockk()
        application = mockk()

        // Mock default flows
        every { userPreferences.userVisitedBlePermissionLauncher } returns MutableStateFlow(false)
        every { bleRepository.bluetoothPermissionsGranted } returns blePermissionsFlow
        coEvery { bleRepository.updateBluetoothPermissions(any()) } just Runs


        // Initialize ViewModel with mocked dependencies
        viewModel = BlePermissionViewModel(
            application = application,
            userPreferences = userPreferences,
            bleRepository = bleRepository
        )
    }

    @Test
    fun `onBleIconClicked when permissions granted does nothing`() = runTest {
        // Arrange
        every { userPreferences.userVisitedBlePermissionLauncher } returns flowOf(false)

        // Re-initialize ViewModel to apply new mocks
        viewModel = BlePermissionViewModel(
            application = application,
            userPreferences = userPreferences,
            bleRepository = bleRepository
        )
        viewModel.setBlePermissionGranted(true)

        // Act
        viewModel.onBleIconClicked()

        // Assert
        val events = mutableListOf<BlePermissionEvent>()
        val job = launch {
            viewModel.permissionEvents.collect {
                events.add(it)
            }
        }

        // Advance coroutine until idle
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify no events were emitted
        assertThat(events).isEmpty()

        job.cancel()
    }

    @Test
    fun `onBleIconClicked when shouldShowRationale is true emits rationale dialog`() = runTest {
        // Arrange
        every { userPreferences.userVisitedBlePermissionLauncher } returns flowOf(false)

        // Re-initialize ViewModel with new mocks
        viewModel = BlePermissionViewModel(
            application = application,
            userPreferences = userPreferences,
            bleRepository = bleRepository
        )
        // Arrage a bit more
        viewModel.setBlePermissionGranted(false)

        // Set shouldShowRationale to true
        viewModel.setShouldShowRationale(true)

        // Act
        viewModel.onBleIconClicked()

        // Assert
        val rationaleState = viewModel.blePermissionRationaleState.value
        assertThat(rationaleState).isNotNull()
        assertThat(rationaleState?.reason).isEqualTo(BleRationaleState.BLE_PERMISSION_RATIONALE_SHOULD_BE_SHOWN)
        assertThat(rationaleState?.rationaleText).isEqualTo("To discover and connect to your Bluetooth devices, we need Bluetooth permission.")
    }

    @Test
    fun `onBleIconClicked first-time request emits RequestBlePermissions event`() = runTest {
        // Arrange
        blePermissionsFlow.value = false
        every { userPreferences.userVisitedBlePermissionLauncher } returns flowOf(false)

        // Re-initialize ViewModel with new mocks
        viewModel = BlePermissionViewModel(
            application = application,
            userPreferences = userPreferences,
            bleRepository = bleRepository
        )

        // Ensure shouldShowRationale is false
        viewModel.setShouldShowRationale(false)

        // Act
        viewModel.onBleIconClicked()

        // Assert
        val events = mutableListOf<BlePermissionEvent>()
        val job = launch {
            viewModel.permissionEvents.collect {
                events.add(it)
            }
        }

        // Advance coroutine until idle
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that RequestBlePermissions event was emitted
        assertThat(events).contains(BlePermissionEvent.RequestBlePermissions)

        // Verify that userVisitedBlePermissionLauncher was set to true
        coVerify { userPreferences.setUserVisitedBlePermissionLauncher(true) }

        job.cancel()
    }

    @Test
    fun `onBleIconClicked previously denied emits OpenSettings event`() = runTest {
        // Arrange
        blePermissionsFlow.value = false
        every { userPreferences.userVisitedBlePermissionLauncher } returns flowOf(true)

        // Re-initialize ViewModel with new mocks
        viewModel = BlePermissionViewModel(
            application = application,
            userPreferences = userPreferences,
            bleRepository = bleRepository
        )

        // Ensure shouldShowRationale is false
        viewModel.setShouldShowRationale(false)

        // Act
        viewModel.onBleIconClicked()

        // Assert
        val events = mutableListOf<BlePermissionEvent>()
        val job = launch {
            viewModel.permissionEvents.collect {
                events.add(it)
            }
        }

        // Advance coroutine until idle
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that OpenSettings event was emitted
        assertThat(events).contains(BlePermissionEvent.OpenSettings)

        job.cancel()
    }

    @Test
    fun `onBleIconClicked emits rationale dialog with correct content and handles onConfirm`() =
        runTest {
            // Arrange
            blePermissionsFlow.value = false
            every { userPreferences.userVisitedBlePermissionLauncher } returns flowOf(false)

            // Re-initialize ViewModel with new mocks
            viewModel = BlePermissionViewModel(
                application = application,
                userPreferences = userPreferences,
                bleRepository = bleRepository
            )

            // Set shouldShowRationale to true
            viewModel.setShouldShowRationale(true)

            // Act
            viewModel.onBleIconClicked()

            // Assert rationale dialog state
            val rationaleState = viewModel.blePermissionRationaleState.value
            assertThat(rationaleState).isNotNull()
            assertThat(rationaleState?.reason).isEqualTo(BleRationaleState.BLE_PERMISSION_RATIONALE_SHOULD_BE_SHOWN)
            assertThat(rationaleState?.rationaleText).isEqualTo("To discover and connect to your Bluetooth devices, we need Bluetooth permission.")
            assertThat(rationaleState?.proceedButtonText).isEqualTo("Proceed")
            assertThat(rationaleState?.dismissButtonText).isEqualTo("Cancel")

            // Collect events
            val events = mutableListOf<BlePermissionEvent>()
            val job = launch {
                viewModel.permissionEvents.collect {
                    events.add(it)
                }
            }

            // Simulate user confirming the rationale dialog
            rationaleState?.onConfirm?.invoke()

            // Advance coroutine until idle
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify that RequestBlePermissions event was emitted
            assertThat(events).contains(BlePermissionEvent.RequestBlePermissions)

            // Verify that userVisitedBlePermissionLauncher was set to false
            coVerify { userPreferences.setUserVisitedBlePermissionLauncher(false) }

            job.cancel()
        }

    // Additional test cases can be added here following the same pattern
}

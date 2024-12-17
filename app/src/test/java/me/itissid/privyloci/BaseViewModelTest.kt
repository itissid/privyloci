// File: app/src/test/java/me/itissid/privyloci/BaseViewModelTest.kt
package me.itissid.privyloci

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseViewModelTest {
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()
    protected val testScope: TestScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}

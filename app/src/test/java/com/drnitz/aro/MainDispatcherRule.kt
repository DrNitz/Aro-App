package com.drnitz.aro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule installing a stable Main dispatcher for the duration of each test.
 *
 * ViewModels run work in [androidx.lifecycle.viewModelScope], which is built on
 * `Dispatchers.Main`. In JVM unit tests there is no real Android main thread,
 * so without [Dispatchers.setMain] those launches either fail to initialize or
 * sit queued on a dispatcher nothing advances — a nondeterministic hang that
 * depends on leftover scheduler state from whatever ran in the same worker
 * JVM beforehand. `UnconfinedTestDispatcher` makes [kotlinx.coroutines.launch]
 * blocks execute eagerly, which matches how these ViewModel APIs are expected
 * to behave in tests.
 */
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

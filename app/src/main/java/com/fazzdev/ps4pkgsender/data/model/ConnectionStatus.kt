package com.fazzdev.ps4pkgsender.data.model

sealed class TestState {
    object Idle : TestState()
    object Loading : TestState()
    data class Success(val message: String) : TestState()
    data class Error(val message: String, val troubleshootingHelp: String? = null) : TestState()
}

data class ConnectionTestResult(
    val localServerState: TestState = TestState.Idle,
    val ps4RpiState: TestState = TestState.Idle,
    val localIpAddress: String? = null,
    val localPort: Int = 8080,
    val ps4IpAddress: String = "",
    val ps4Port: Int = 12800
) {
    val isAllConnected: Boolean
        get() = localServerState is TestState.Success && ps4RpiState is TestState.Success
}

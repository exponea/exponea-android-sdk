package com.exponea.sdk.testutil

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher

internal object ExponeaMockServer {

    val mockResponse = MockResponse()

    /**
     * Return success response from a given json file.
     */

    fun setResponseSuccess(server: MockWebServer, responseFile: String) {

        val content = this.javaClass.classLoader.getResource(responseFile).readText()

        mockResponse.clearHeaders()
        mockResponse.setBody(content)
        mockResponse.setResponseCode(200)

        server.enqueue(mockResponse)
    }

    /**
     * Return error response from a given json file.
     */

    fun setResponseError(server: MockWebServer, responseFile: String, errorCode: Int = 400) {

        val content = this.javaClass.classLoader.getResource(responseFile).readText()

        mockResponse.clearHeaders()
        mockResponse.setBody(content)
        mockResponse.setResponseCode(errorCode)

        server.enqueue(mockResponse)
    }

    fun createServer(defaultResponse: MockResponse? = null): MockWebServer {
        val server = MockWebServer()
        val dispatcher = QueueDispatcher()
        if (defaultResponse != null) {
            dispatcher.setFailFast(defaultResponse)
        } else {
            dispatcher.setFailFast(true)
        }
        server.setDispatcher(dispatcher)
        return server
    }
}

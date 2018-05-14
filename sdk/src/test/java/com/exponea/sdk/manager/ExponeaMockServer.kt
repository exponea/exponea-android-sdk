package com.exponea.sdk.manager

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object ExponeaMockServer {

    /**
     * Create a new instance of MockWebServer to simulate all
     * http requests and responses.
     */

    // Mockwebserver instance.
    lateinit var server: MockWebServer
    // Mockresponse instance.
    lateinit var mockResponse: MockResponse
    // Return the Mockwebserver address.
    val address: String
        get() = server.url("/").toString()
    // Control if the server was started.
    var serverStarted: Boolean = false

    /**
     * Set up the Mock Server.
     */

    fun setUp() {

        // Initialize the servers.
        server = MockWebServer()
        mockResponse = MockResponse()

        if (!serverStarted) {
            server.start()
            serverStarted = true
        }
    }

    fun shutDown() {
        if (serverStarted) server.shutdown()
    }

    /**
     * Print out the mock server address.
     */

    fun printServerAddress(){
        println("mockServerAddress: $address")
    }

    /**
     * Return success response from a given json file.
     */

    fun setResponseSuccess(responseFile: String) {

        val content = this.javaClass.classLoader.getResource(responseFile).readText()

        mockResponse.setBody(content)
        mockResponse.setResponseCode(200)

        server.enqueue(mockResponse)
    }

    /**
     * Return error response from a given json file.
     */

    fun setResponseError(responseFile: String, errorCode: Int = 400) {

        val content = this.javaClass.classLoader.getResource(responseFile).readText()

        mockResponse.setBody(content)
        mockResponse.setResponseCode(errorCode)

        server.enqueue(mockResponse)
    }

    fun getResult(): RecordedRequest {
        return server.takeRequest()
    }

}
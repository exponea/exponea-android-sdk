package com.exponea.example.view.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.*
import kotlinx.android.synthetic.main.activity_authentication.*
import kotlinx.android.synthetic.main.fragment_main.*


class MainFragment : BaseFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackPage(Constants.ScreenNames.mainScreen)
        setupListeners()
    }

    private fun setProgressBarVisible(visible: Boolean) {
        if (visible) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun setupListeners() {
        // Initialize customer to target
        val uuid = App.instance.userIdManager.uniqueUserID
        val customerIds = CustomerIds(cookie = uuid)

        attributesButton.setOnClickListener {

            // Initialize our attributes structure
            val attributes = CustomerAttributes(customerIds)

            // Select attributes to fetch
            attributes.apply {
                withProperty("first_name")
            }
            setProgressBarVisible(true)

            // Specify callbacks and start loading
            Exponea.fetchCustomerAttributes(
                    customerAttributes = attributes,
                    onFailure = { onFetchFailed(it) },
                    onSuccess = {onFetchSuccess(it)}
            )
        }

        recommendationsButton.setOnClickListener {

            // Init out recommendation structure
            val recommendation = CustomerRecommendation(
                    id = uuid,
                    strategy = "winner"
            )
            setProgressBarVisible(true)

            // Specify callbacks and start loading
            Exponea.fetchRecommendation(
                    customerIds = customerIds,
                    customerRecommendation = recommendation,
                    onSuccess = {onFetchSuccess(it)},
                    onFailure = {onFetchFailed(it)}

            )
        }

        eventsButton.setOnClickListener {

            // Initialize our events structure
            val events = CustomerEvents(
                    customerIds = customerIds,
                    eventTypes = mutableListOf("event1", "event2"),
                    sortOrder = "desc"
            )
            setProgressBarVisible(true)
            // Specify callback and start loading
            Exponea.fetchCustomerEvents(
                    customerEvents = events,
                    onFailure = {onFetchFailed(it)},
                    onSuccess = {
                        Handler(Looper.getMainLooper()).post({
                            setProgressBarVisible(false)
                            resultTextView.text = it.toString()
                        })
                    }
            )

        }


    }

    // Fetch successful callback
    private fun onFetchSuccess(result: Result<List<CustomerAttributeModel>>) {
        Handler(Looper.getMainLooper()).post({
            setProgressBarVisible(false)
            resultTextView.text = result.toString()
        })

    }

    // Fetch failed callback
    private fun onFetchFailed(result: Result<FetchError>) {

        Handler(Looper.getMainLooper()).post({
            setProgressBarVisible(false)
            resultTextView.text = "Message: ${result.results.message}" +
                    "\nJson: ${result.results.jsonBody}"
        })


    }
}
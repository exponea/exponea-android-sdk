package com.exponea.example.view.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.example.view.dialogs.FetchRecommendationDialog
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Result
import kotlinx.android.synthetic.main.fragment_fetch.consentsButton
import kotlinx.android.synthetic.main.fragment_fetch.progressBar
import kotlinx.android.synthetic.main.fragment_fetch.recommendationsButton
import kotlinx.android.synthetic.main.fragment_fetch.resultTextView
import kotlinx.android.synthetic.main.fragment_fetch.segmentationButton
import kotlinx.android.synthetic.main.fragment_fetch.view.buttonsContainer

class FetchFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = layoutInflater.inflate(R.layout.fragment_fetch, container, false)
        layout.buttonsContainer.addView(
            Exponea.getAppInboxButton(requireActivity()),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Track visited screen
        trackPage(Constants.ScreenNames.mainScreen)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "fetching"

        // Init listeners
        initListeners()
    }

    private fun setProgressBarVisible(visible: Boolean) {
        if (visible) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Initialize button listener
     */
    private fun initListeners() {
        recommendationsButton.setOnClickListener {
            FetchRecommendationDialog.show(childFragmentManager) { fetchRecommended(it) }
        }
        consentsButton.setOnClickListener {
            setProgressBarVisible(true)
            Exponea.getConsents({ onFetchSuccess(it) }, { onFetchFailed(it) })
        }
        segmentationButton.setOnClickListener {
            val exposingCategory = "discovery"
            Exponea.getSegments(exposingCategory) { segments ->
                runOnUiThread {
                    resultTextView.text = "Segments for $exposingCategory category:\n$segments"
                }
            }
        }
    }

    /**
     * Method handles recommendations loading
     */
    private fun fetchRecommended(options: CustomerRecommendationOptions) {
        setProgressBarVisible(true)
        Exponea.fetchRecommendation(
            recommendationOptions = options,
            onSuccess = { onFetchSuccess(it) },
            onFailure = { onFetchFailed(it) }
        )
    }

    /**
     * Our success callback
     */
    private fun <T> onFetchSuccess(result: Result<ArrayList<T>>) {
        runOnUiThread {
            setProgressBarVisible(false)
            resultTextView.text = result.toString()
        }
    }

    /**
     * Our failure callback
     */
    private fun onFetchFailed(result: Result<FetchError>) {
        runOnUiThread {
            setProgressBarVisible(false)
            resultTextView.text = "Message: ${result.results.message}" +
                "\nJson: ${result.results.jsonBody}"
        }
    }

    /**
     * Method to update ui
     */
    private fun runOnUiThread(runnable: () -> Unit) {
        Handler(Looper.getMainLooper()).post(runnable)
    }
}

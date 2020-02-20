package com.exponea.example.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.fragment_flush.progressBar
import kotlinx.android.synthetic.main.fragment_flush.settingsBtnFlush

class FlushFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_flush, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "flush"

        // Track visited screen
        trackPage(Constants.ScreenNames.settingsScreen)

        settingsBtnFlush.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            Exponea.flushData { result ->
                if (!this.isVisible) return@flushData
                Handler(Looper.getMainLooper()).post {
                    progressBar.visibility = View.INVISIBLE
                    AlertDialog.Builder(context)
                        .setTitle(if (result.isSuccess) "Flush successful" else "Flush failed")
                        .setMessage(if (result.isFailure) result.exceptionOrNull()?.localizedMessage else null)
                        .setPositiveButton("OK") { _, _ -> }
                        .create()
                        .show()
                }
            }
        }
    }
}

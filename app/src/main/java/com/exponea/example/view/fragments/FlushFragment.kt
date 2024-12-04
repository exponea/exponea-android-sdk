package com.exponea.example.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.databinding.FragmentFlushBinding
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea

class FlushFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentFlushBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentFlushBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "flush"

        // Track visited screen
        trackPage(Constants.ScreenNames.settingsScreen)

        viewBinding.settingsBtnFlush.setOnClickListener {
            viewBinding.progressBar.visibility = View.VISIBLE
            Exponea.flushData { result ->
                if (!this.isVisible) return@flushData
                Handler(Looper.getMainLooper()).post {
                    viewBinding.progressBar.visibility = View.INVISIBLE
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

package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.exponea.example.R
import com.exponea.sdk.models.CustomerRecommendationOptions

class FetchRecommendationDialog : DialogFragment() {
    private lateinit var onFetch: (CustomerRecommendationOptions) -> Unit

    companion object {
        const val TAG = "FetchRecommendationDialog"

        fun show(fragmentManager: FragmentManager, onFetch: (CustomerRecommendationOptions) -> Unit) {
            val fragment = fragmentManager.findFragmentByTag(TAG)
                as? FetchRecommendationDialog ?: FetchRecommendationDialog()

            fragment.onFetch = onFetch
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_fetch_recommendation, null, false)
        builder.setView(view)
        initListeners(view)
        return builder.create()
    }

    private fun initListeners(view: View) {
        val idView = view.findViewById(R.id.editTextId) as EditText
        view.findViewById<Button>(R.id.buttonFetch).setOnClickListener {
            onFetch(
                CustomerRecommendationOptions(
                    id = idView.text.toString(),
                    fillWithRandom = true
                )
            )
            dismiss()
        }
    }
}

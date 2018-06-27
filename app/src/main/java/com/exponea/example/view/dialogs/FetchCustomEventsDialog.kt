package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.FetchEventsRequest

class FetchCustomEventsDialog : DialogFragment() {

    lateinit var onFetch: (FetchEventsRequest) -> Unit


    companion object {
        private val TAG = FetchCustomEventsDialog::class.java.canonicalName

        fun show(fragmentManager: FragmentManager, onFetch: (FetchEventsRequest) -> Unit) {

            val fragment = fragmentManager.findFragmentByTag(TAG)
                    as? FetchCustomEventsDialog ?: FetchCustomEventsDialog()

            fragment.onFetch = onFetch
            fragment.show(fragmentManager, TAG)
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_fetch_custom_events, null, false)
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme).setView(view)
        initView(view)
        return builder.create()
    }

    private fun initView(view: View) {
        val buttonFetch : Button = view.findViewById(R.id.buttonFetchEvents)
        val input : EditText = view.findViewById(R.id.eventsEditText)


        buttonFetch.setOnClickListener {
            val customerIds = CustomerIds()
                    .withId("registered", App.instance.registeredIdManager.registeredID)
            val fetchRequest = FetchEventsRequest(
                    customerIds = customerIds,
                    eventTypes = input.text.split(",").map { it.trim() }.toMutableList(),
                    skip = 0,
                    limit = 10
            )
            onFetch(fetchRequest)
            dismiss()
        }

    }

}
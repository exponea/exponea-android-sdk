package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import com.exponea.example.R

class CustomEventDialog : DialogFragment() {

    private lateinit var onConfirmed: () -> Unit

    companion object {

        const val TAG = "CustomEventDialog"

        fun show(fragmentManager: FragmentManager, callback : () -> (Unit) ) {
            val fragment = fragmentManager.findFragmentByTag(TAG)
                    as? CustomEventDialog
                    ?: CustomEventDialog()

            fragment.onConfirmed = callback
            fragment.show(fragmentManager, TAG)
        }

    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
        val inflate = LayoutInflater.from(context)
        val view = inflate.inflate(R.layout.dialog_custom_event, null ,false)
        builder.setView(view)
            .setPositiveButton("Track", {_, _ -> })
            .setNegativeButton(android.R.string.cancel, {_, _ ->})
        return builder.create()
    }

}
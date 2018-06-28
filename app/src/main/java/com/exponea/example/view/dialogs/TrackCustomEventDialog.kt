package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.utils.asJson
import com.exponea.sdk.models.PropertiesList

class TrackCustomEventDialog : DialogFragment() {

    private lateinit var onConfirmed: (eventName: String, properties: PropertiesList) -> Unit
    private val propsMap =  hashMapOf("property" to "some value" as Any)
    companion object {

        const val TAG = "TrackCustomEventDialog"

        fun show(
                fragmentManager: FragmentManager,
                callback : (eventName: String, properties: PropertiesList) -> (Unit)
        ) {
            val fragment = fragmentManager.findFragmentByTag(TAG)
                    as? TrackCustomEventDialog
                    ?: TrackCustomEventDialog()

            fragment.onConfirmed = callback
            fragment.show(fragmentManager, TAG)
        }

    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
        val inflate = LayoutInflater.from(context)
        val view = inflate.inflate(R.layout.dialog_track_custom_event, null ,false)
        builder.setView(view)
        initListeners(view)
        return builder.create()
    }

    private fun initListeners(view: View) {
        val propName : EditText = view.findViewById(R.id.editTextPropName)
        val propValue : EditText = view.findViewById(R.id.editTextValue)
        val eventName : EditText = view.findViewById(R.id.editTextEventName)
        val propsTextView : TextView = view.findViewById(R.id.textViewProperties)


        propsTextView.text = propsMap.asJson()

        view.findViewById<Button>(R.id.buttonAddProperty).setOnClickListener {
            if (!propValue.text.isEmpty() && !propName.text.isEmpty()) {
                Log.d(TAG, propsMap.toString())
                propsMap[propName.text.toString()] = propValue.text.toString()
                propsTextView.text = propsMap.asJson()
            }
        }

        view.findViewById<Button>(R.id.buttonTrack).setOnClickListener {
            val name = eventName.text.toString()
            val properties = PropertiesList(propsMap)
            onConfirmed(name, properties)
            dismiss()
        }

    }



}
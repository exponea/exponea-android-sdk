package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.models.Constants
import com.exponea.example.models.SdkSetupState
import com.exponea.example.utils.asJson

class IdentifyCustomerDialog : DialogFragment() {

    private val attributes = HashMap<String, Any>()

    companion object {
        private const val TAG = "IdentifyCustomerDialog"

        const val REQUEST_KEY = "IdentifyCustomerDialog"
        const val KEY_PROPERTIES = "properties"
        const val KEY_WITH_AUTH_TOKEN = "withAuthToken"

        fun show(fragmentManager: FragmentManager) {
            val fragment = fragmentManager.findFragmentByTag(TAG)
                    as? IdentifyCustomerDialog ?: IdentifyCustomerDialog()
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_track_custom_attributes, null, false)
        builder.setView(view)
        initListeners(view)
        return builder.create()
    }

    private fun initListeners(view: View) {
        val nameView = view.findViewById<EditText>(R.id.editTextName)
        val valueView = view.findViewById<EditText>(R.id.editTextValue)
        val propertiesView = view.findViewById<TextView>(R.id.textViewAttributes)
        val idsView = view.findViewById<TextView>(R.id.idsTextView)

        idsView.text = "${Constants.CUSTOMER_ID_REGISTERED}: ${App.instance.registeredIdManager.registeredID}"

        propertiesView.text = attributes.asJson()
        view.findViewById<Button>(R.id.buttonAddAttr).setOnClickListener {
            if (!nameView.text.isEmpty() && !valueView.text.isEmpty()) {
                attributes[nameView.text.toString()] = valueView.text.toString()
                propertiesView.text = attributes.asJson()
            }
        }

        view.findViewById<Button>(R.id.buttonIdentify).setOnClickListener {
            deliverResult(withAuthToken = false)
        }

        view.findViewById<Button>(R.id.buttonIdentifyWithAuthToken).apply {
            visibility = if (SdkSetupState.isStreamConfig) View.VISIBLE else View.GONE
        }.setOnClickListener {
            if (!LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
                Toast.makeText(
                    requireContext(),
                    "JWT Key ID and Secret must be provided during configuration.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            deliverResult(withAuthToken = true)
        }
    }

    private fun deliverResult(withAuthToken: Boolean) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putSerializable(KEY_PROPERTIES, attributes)
                putBoolean(KEY_WITH_AUTH_TOKEN, withAuthToken)
            })
        dismiss()
    }
}

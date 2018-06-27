package com.exponea.example.view.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerIds

class FetchCustomAttributeDialog : DialogFragment(), AdapterView.OnItemSelectedListener{

    lateinit var onFetch: (CustomerAttributes) -> Unit
    lateinit var attribute: String

    companion object {

        const val TAG = "FetchCustomerAttributesDialog"

        fun show(fragmentManager: FragmentManager, onFetch: (CustomerAttributes) -> Unit) {
            val fragment = fragmentManager.findFragmentByTag(TAG) as?
                    FetchCustomAttributeDialog ?: FetchCustomAttributeDialog()
            fragment.onFetch = onFetch
            fragment.show(fragmentManager, TAG)
        }

        val types = arrayListOf(
                CustomerAttributes.TYPE_PROPERTY,
                CustomerAttributes.TYPE_AGGREGATE,
                CustomerAttributes.TYPE_EXPRESSION,
                CustomerAttributes.TYPE_ID,
                CustomerAttributes.TYPE_PREDICTION,
                CustomerAttributes.TYPE_SEGMENTATION
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_fetch_custom_attributes, null, false)

        val builder = AlertDialog.Builder(context, R.style.MyDialogTheme)
                .setView(view)

        initView(view)
        return builder.create()
    }

    private fun initView(view: View) {
        val spinner : Spinner = view.findViewById(R.id.spinner)
        val buttonFetch : Button = view.findViewById(R.id.buttonFetchAttribute)
        val editTextName: EditText = view.findViewById(R.id.editTextName)

        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, types).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.apply {
            this.adapter = adapter
            onItemSelectedListener = this@FetchCustomAttributeDialog
            setSelection(0)
        }

        buttonFetch.setOnClickListener {
            val customerAttributes = buildCustomAttributeFrom(attribute, editTextName.text.toString())
            onFetch(customerAttributes)
            dismiss()
        }

    }

    private fun buildCustomAttributeFrom(type: String, value: String) : CustomerAttributes {
        val customerIds = CustomerIds().withId("registered", (App.instance.registeredIdManager.registeredID))
        return when(type) {
            CustomerAttributes.TYPE_PROPERTY -> CustomerAttributes(customerIds).also { it.withProperty(value) }
            CustomerAttributes.TYPE_AGGREGATE -> CustomerAttributes(customerIds).also { it.withAggregation(value) }
            CustomerAttributes.TYPE_EXPRESSION -> CustomerAttributes(customerIds).also { it.withExpression(value) }
            CustomerAttributes.TYPE_SEGMENTATION -> CustomerAttributes(customerIds).also { it.withSegmentation(value) }
            CustomerAttributes.TYPE_PREDICTION -> CustomerAttributes(customerIds).also { it.withPrediction(value) }
            else -> CustomerAttributes(customerIds).also { it.withId(value) }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        attribute = types[position]
    }
}
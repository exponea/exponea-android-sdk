package com.exponea.example.view.fragments

import TokenTracker
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.App
import com.exponea.example.databinding.FragmentTrackBinding
import com.exponea.example.managers.CustomerTokenStorage
import com.exponea.example.managers.LocalJwtTokenGenerator
import com.exponea.example.models.Constants
import com.exponea.example.models.SdkSetupState
import com.exponea.example.view.base.BaseFragment
import com.exponea.example.view.dialogs.IdentifyCustomerDialog
import com.exponea.example.view.dialogs.TrackCustomEventDialog
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIdentity
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem

class TrackFragment : BaseFragment(), AdapterView.OnItemClickListener {

    private val tag = this::class.simpleName
    private lateinit var viewBinding: FragmentTrackBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentTrackBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    companion object {
        fun mockItems(): ArrayList<String> {
            val list = arrayListOf<String>()
            for (i in 1..14) {
                list.add("Item #$i")
            }
            return list
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "tracking"

        // Track visited screen
        trackPage(Constants.ScreenNames.purchaseScreen)

        viewBinding.listView.adapter = Adapter()

        viewBinding.authTokenGroup.visibility = if (SdkSetupState.isStreamConfig) View.VISIBLE else View.GONE

        childFragmentManager.setFragmentResultListener(
            IdentifyCustomerDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            val properties = PropertiesList(
                bundle.getSerializable(IdentifyCustomerDialog.KEY_PROPERTIES) as HashMap<String, Any>
            )
            val withAuthToken = bundle.getBoolean(IdentifyCustomerDialog.KEY_WITH_AUTH_TOKEN)
            trackUpdateCustomerProperties(properties, withAuthToken)
        }

        // Init buttons listeners
        initListeners()
    }

    private fun initListeners() {
        viewBinding.listView.onItemClickListener = this

        viewBinding.buttonTrackClicked.setOnClickListener { trackPushClicked() }
        viewBinding.buttonTrackDelivered.setOnClickListener { trackPushDelivered() }
        viewBinding.buttonTrackToken.setOnClickListener { trackToken() }
        viewBinding.buttonAuthorizePush.setOnClickListener { requestPushAuthorization() }

        viewBinding.buttonUpdateProperties.setOnClickListener {
            IdentifyCustomerDialog.show(childFragmentManager)
        }

        viewBinding.buttonCustomEvent.setOnClickListener {
            TrackCustomEventDialog.show(childFragmentManager) { eventName, properties ->
                trackCustomEvent(eventName, properties) }
        }

        viewBinding.buttonSetAuthToken.setOnClickListener {
            val context = requireContext()
            if (!LocalJwtTokenGenerator.INSTANCE.isConfigured()) {
                Toast.makeText(
                    context,
                    "JWT Key ID and Secret must be provided during SDK configuration.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (!SdkSetupState.isCustomerIdentified) {
                Toast.makeText(
                    context,
                    "Customer must be identified first.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val customerIds = mapOf(Constants.CUSTOMER_ID_REGISTERED to App.instance.registeredIdManager.registeredID)

            val token = LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds)

            if (token != null) {
                Exponea.setSdkAuthToken(token)
                Toast.makeText(context, "Auth token set.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Token generation failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPushAuthorization() {
        Exponea.requestPushAuthorization(requireContext()) { granted ->
            Log.i(tag, "Push notifications are allowed: $granted")
        }
    }

    /**
     * Method to handle custom event tracking obtained by TrackCustomEventDialog
     */
    private fun trackCustomEvent(eventName: String, propertiesList: PropertiesList) {
        Exponea.trackEvent(
                eventType = eventName,
                properties = propertiesList
        )
    }

    /**
     * Method to handle push clicked event tracking
     */
    private fun trackPushClicked() {
        Exponea.trackClickedPush(
                NotificationData(hashMapOf("campaign_id" to "id"))
        )
    }

    /**
     * Method to handle updating customer properties
     */
    private fun trackUpdateCustomerProperties(propertiesList: PropertiesList, setAuthToken: Boolean = false) {
        val registeredIdUpdate = propertiesList.properties.remove(Constants.CUSTOMER_ID_REGISTERED) as? String

        if (registeredIdUpdate != null) {
            App.instance.registeredIdManager.registeredID = registeredIdUpdate
        }

        val customerIds = hashMapOf(Constants.CUSTOMER_ID_REGISTERED to App.instance.registeredIdManager.registeredID)

        CustomerTokenStorage.INSTANCE.configure(customerIds = customerIds)

        Exponea.identifyCustomer(
            customerIdentity = CustomerIdentity(
                customerIds = customerIds,
                sdkAuthToken = if (setAuthToken) LocalJwtTokenGenerator.INSTANCE.generateToken(customerIds) else null
            ),
            properties = propertiesList.properties
        )

        SdkSetupState.isCustomerIdentified = true
    }

    /**
     * Method to handle push delivered event tracking
     */
    private fun trackPushDelivered() {
        Exponea.trackDeliveredPush(
                data = NotificationData(hashMapOf("campaign_id" to "id"))
        )
    }

    /**
     * Method to handle token tracking
     */
    private fun trackToken() {
        TokenTracker().trackToken(requireContext())
    }

    /**
     * Method to manually track customer's purchases
     */
    private fun trackPayment(position: Int) {
        val purchasedItem = PurchasedItem(
                value = 2011.1,
                currency = "USD",
                paymentSystem = "System",
                productId = id.toString(),
                productTitle = mockItems()[position]
        )
        Exponea.trackPaymentEvent(
                purchasedItem = purchasedItem)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        // Track purchase at position
        trackPayment(position)
        Toast.makeText(requireContext(), "Payment Tracked", Toast.LENGTH_SHORT).show()
    }

    class Adapter : BaseAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val inflater = LayoutInflater.from(parent?.context)
            if (convertView == null) {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                view.findViewById<TextView>(android.R.id.text1).text = mockItems()[position]
                return view
            }
            convertView.findViewById<TextView>(android.R.id.text1).text = mockItems()[position]
            return convertView
        }

        override fun getItem(position: Int): Any {
            return mockItems()[position]
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount() = mockItems().size
    }
}

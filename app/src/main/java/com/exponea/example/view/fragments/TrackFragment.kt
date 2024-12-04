package com.exponea.example.view.fragments

import TokenTracker
import android.os.Bundle
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
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.example.view.dialogs.TrackCustomAttributesDialog
import com.exponea.example.view.dialogs.TrackCustomEventDialog
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.util.Logger

class TrackFragment : BaseFragment(), AdapterView.OnItemClickListener {

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
            TrackCustomAttributesDialog.show(childFragmentManager) {
                trackUpdateCustomerProperties(it)
            }
        }

        viewBinding.buttonCustomEvent.setOnClickListener {
            TrackCustomEventDialog.show(childFragmentManager) { eventName, properties ->
                trackCustomEvent(eventName, properties) }
        }
    }

    private fun requestPushAuthorization() {
        Exponea.requestPushAuthorization(requireContext()) { granted ->
            Logger.i(this, "Push notifications are allowed: $granted")
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
    private fun trackUpdateCustomerProperties(propertiesList: PropertiesList) {
        val registeredIdUpdate = propertiesList.properties.remove("registered") as? String
        if (registeredIdUpdate != null) {
            App.instance.registeredIdManager.registeredID = registeredIdUpdate
        }
        val customerIds = CustomerIds().withId("registered", (App.instance.registeredIdManager.registeredID))
        CustomerTokenStorage.INSTANCE.configure(
            customerIds = hashMapOf(
                "registered" to (App.instance.registeredIdManager.registeredID ?: "")
            )
        )
        Exponea.identifyCustomer(
                customerIds = customerIds,
                properties = propertiesList
        )
    }

    /**
     * Method to handle push delivered event tracking"
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
        TokenTracker().trackToken(context)
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
        Toast.makeText(context, "Payment Tracked", Toast.LENGTH_SHORT).show()
    }

    inner class Adapter : BaseAdapter() {

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

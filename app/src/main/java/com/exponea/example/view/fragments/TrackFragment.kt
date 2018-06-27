package com.exponea.example.view.fragments

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.example.view.dialogs.TrackCustomAttributesDialog
import com.exponea.example.view.dialogs.TrackCustomEventDialog
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.PurchasedItem
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_track.*
import java.util.*

class TrackFragment : BaseFragment(), AdapterView.OnItemClickListener {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_track, container, false)
    }


    companion object {
        fun mockItems() : ArrayList<String> {
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

        listView.adapter = Adapter()

        // Init buttons listeners
        initListeners()

    }


    private fun initListeners() {
        listView.onItemClickListener = this
        buttonTrackClicked.setOnClickListener { trackPushClicked() }

        buttonTrackDelivered.setOnClickListener { trackPushDelivered() }

        buttonTrackToken.setOnClickListener { trackFCMToken() }

        buttonUpdateProperties.setOnClickListener {
            TrackCustomAttributesDialog.show(childFragmentManager, {
                trackUpdateCustomerProperties(it)
            })
        }

        buttonCustomEvent.setOnClickListener {
            TrackCustomEventDialog.show(childFragmentManager, { eventName, properties ->
                trackCustomEvent(eventName, properties)})
        }
    }

    /**
     * Method to handle custom event tracking obtained by TrackCustomEventDialog
     */
    private fun trackCustomEvent(eventName: String, propertiesList: PropertiesList) {
        Exponea.trackCustomerEvent(
                eventType = eventName,
                properties = propertiesList,
                timestamp = Date().time
        )
    }


    /**
     * Method to handle push clicked event tracking
     */
    private fun trackPushClicked() {
        Exponea.trackClickedPush(
                fcmToken = "Fcm Token"
        )
    }

    /**
     * Method to handle updating customer properties
     */
    private fun trackUpdateCustomerProperties(propertiesList: PropertiesList) {
        val customerIds = CustomerIds().withId("registered", (App.instance.registeredIdManager.registeredID))

        Exponea.updateCustomerProperties(
                customerIds = customerIds,
                properties = propertiesList
        )
    }

    /**
     * Method to handle push delivered event tracking"
     */
    private fun trackPushDelivered() {
        Exponea.trackDeliveredPush(
                fcmToken = "Fcm Token"
        )

    }

    /**
     * Method to handle token tracking
     */
    private fun trackFCMToken() {
        Exponea.trackPushToken(
                fcmToken = FirebaseInstanceId.getInstance().token ?: ""
        )
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

    inner class Adapter: BaseAdapter() {


        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val inflater = LayoutInflater.from(parent?.context)
            if (convertView == null) {
                val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                view.findViewById<TextView>(android.R.id.text1).text = mockItems()[position]
                return  view
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
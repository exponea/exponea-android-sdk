package com.exponea.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.CalendarView
import android.widget.Toast
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.models.Route

import kotlinx.android.synthetic.main.activity_event_tracking.*
import kotlinx.android.synthetic.main.content_event_tracking.*
import java.util.*
import kotlin.collections.HashMap

class EventTrackingActivity : AppCompatActivity() {

    companion object {

        private const val ARG_EVENT = "EventType"

        fun startAsIntent(context: Context, event: String) {
            val intent = Intent(context, EventTrackingActivity::class.java)
            intent.putExtra(ARG_EVENT, event)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_tracking)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Event Tracking"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }


        val event = intent.getStringExtra(ARG_EVENT)
        setUpEventTracking(event)

        //Manual Flush Button
        buttunFlush.setOnClickListener {
            Exponea.flush()
        }

    }

    private fun setUpEventTracking(event: String) {
        val userId = App.instance.userIdManager.uniqueUserID
        when (event)  {
            Constants.Events.EVENT_UPDATE_CUSTOMER -> {
                Exponea.updateCustomerProperties(
                        customerIds = CustomerIds(userId),
                        properties = PropertiesList(hashMapOf(Pair("first_name", "New User Name"), Pair("email", "new@mail.com")))
                )
            }

            Constants.Events.EVENT_TRACK_CUSTOMER -> {
                Exponea.trackCustomerEvent(
                        customerIds = CustomerIds(cookie = userId),
                        properties = PropertiesList(hashMapOf( Pair("name", "customerEventTracking"))),
                        eventType =  "page_view",
                        timestamp = Date().time
                )
            }

        }
    }



}

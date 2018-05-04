package com.exponea.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.Route

import kotlinx.android.synthetic.main.activity_event_tracking.*
import kotlinx.android.synthetic.main.content_event_tracking.*

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
        when (event)  {

            Constants.Events.EVENT_TRACK -> {
                Toast.makeText(this, "Tracking event", Toast.LENGTH_SHORT).show()
                Exponea.trackEvent(
                        eventType =  "page_view",
                        properties = hashMapOf( Pair("name", "eventTracking")),
                        route = Route.TRACK_EVENTS
                )
            }

            Constants.Events.EVENT_TRACK_CUSTOMER -> {
                Toast.makeText(this, "Tracking customer event", Toast.LENGTH_SHORT).show()
                val userId = App.instance.userIdManager.uniqueUserID
                Exponea.trackEvent(
                        eventType =  "page_view",
                        customerId = CustomerIds(cookie = userId),
                        properties = hashMapOf( Pair("name", "customerEventTracking")),
                        route = Route.TRACK_EVENTS
                )
            }

        }
    }



}

package com.exponea.example.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.exponea.example.R

import kotlinx.android.synthetic.main.activity_event_tracking.*

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
    }

}

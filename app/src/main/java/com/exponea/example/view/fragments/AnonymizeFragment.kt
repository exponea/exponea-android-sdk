package com.exponea.example.view.fragments

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exponea.example.R
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import kotlinx.android.synthetic.main.fragment_anonymize.*

class AnonymizeFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_anonymize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.subtitle = "anonymize"
        btnAnonymize.setOnClickListener {
            Exponea.anonymize()
        }
    }


}
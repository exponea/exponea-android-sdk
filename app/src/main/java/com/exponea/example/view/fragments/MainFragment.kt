package com.exponea.example.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerIds


class MainFragment : BaseFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackPage(Constants.ScreenNames.mainScreen)

        var attrs = CustomerAttributes(CustomerIds())

        attrs.attributes = mutableListOf(
                hashMapOf(
                        Pair("type", "property" as Any),
                        Pair("property", "first_name" as Any)
                )
        )

//        Exponea.fetchCustomerAttributes(
//                attrs,
//                onFailure = {Log.d("Call", it)},
//                onSuccess = {Log.d("Call", it.toString())}
//        )
    }
}
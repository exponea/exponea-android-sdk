package com.exponea.example.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exponea.example.App
import com.exponea.example.R
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds

class SearchFragment : BaseFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trackPage(Constants.ScreenNames.searchScreen)

        val userID = App.instance.userIdManager.uniqueUserID

        Exponea.showBanners(CustomerIds(cookie = userID))
    }
}
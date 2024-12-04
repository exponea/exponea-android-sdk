package com.exponea.example.view.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.exponea.example.databinding.FragmentAnonymizeBinding
import com.exponea.example.models.Constants
import com.exponea.example.view.base.BaseFragment
import com.exponea.sdk.Exponea

class AnonymizeFragment : BaseFragment() {

    private lateinit var viewBinding: FragmentAnonymizeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentAnonymizeBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Track visited screen
        trackPage(Constants.ScreenNames.anonymizeScreen)

        (activity as AppCompatActivity).supportActionBar?.subtitle = "anonymize"
        viewBinding.btnAnonymize.setOnClickListener {
            Exponea.anonymize()
            AlertDialog.Builder(context)
                .setTitle("Customer anonymized")
                .setMessage("Stored customer data cleared.")
                .setPositiveButton("OK") { _, _ -> }
                .create()
                .show()
        }
    }
}

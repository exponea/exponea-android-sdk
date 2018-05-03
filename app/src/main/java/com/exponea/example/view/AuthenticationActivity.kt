package com.exponea.example.view

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.exponea.example.R
import com.exponea.example.utils.isValid
import com.exponea.example.utils.onTextChanged
import kotlinx.android.synthetic.main.activity_authentication.*

class AuthenticationActivity : AppCompatActivity() {

    var authCode = ""
    var projectToken = ""
    var customerIds = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        editTextAuthCode.onTextChanged { authCode = it  }
        editTextCustomersIds.onTextChanged { customerIds = it }
        editTextProjectToken.onTextChanged { projectToken = it }

        button.setOnClickListener {
            if (!editTextProjectToken.isValid() || !editTextCustomersIds.isValid()
                    || !editTextAuthCode.isValid()) {
                Toast.makeText(this, "Empty field", Toast.LENGTH_SHORT).show()
            } else {
                // login
            }
        }

    }
}

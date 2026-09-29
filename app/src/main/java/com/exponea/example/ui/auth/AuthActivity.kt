package com.exponea.example.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exponea.example.config.EnvConfiguration
import com.exponea.example.config.SdkInitResult
import com.exponea.example.config.SdkInitializer
import com.exponea.example.ui.MainActivity
import com.exponea.example.ui.theme.BloomreachTheme

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EnvConfiguration.resolve(intent)?.let { form ->
            when (val result = SdkInitializer.init(form)) {
                is SdkInitResult.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    return
                }
                is SdkInitResult.Error -> {
                    Toast.makeText(
                        this,
                        "Env init failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        setContent {
            BloomreachTheme {
                AuthScreen(
                    onInitSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

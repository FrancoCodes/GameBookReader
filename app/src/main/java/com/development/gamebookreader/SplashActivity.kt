package com.development.gamebookreader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashActivity : AppCompatActivity() {

    // Time in milliseconds for the splash screen delay
    private val SPLASH_DELAY: Long = 3500 // 3.5 seconds


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set your splash layout XML here



        // Use a Handler to delay the redirect
        Handler().postDelayed({
            // Create an Intent to start the MuseumActivity
            val intent = Intent(this, Library::class.java)
            startActivity(intent)
            finish() // Finish this activity so the user can't go back to the splash screen
        }, SPLASH_DELAY)
    }
}

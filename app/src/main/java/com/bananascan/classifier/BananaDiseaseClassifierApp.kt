package com.bananascan.classifier

import android.app.Application
import com.google.firebase.FirebaseApp

class BananaDiseaseClassifierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
package com.bananascan.classifier.data

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val email: String = "",
    val name: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now()
)

data class Classification(
    val userId: String = "",
    val result: String = "",
    val confidence: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String = "",
    val locationId: String = ""
)

data class Location(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now()
)

data class ClassificationWithLocation(
    val classification: Classification,
    val location: Location?
)
package com.bananascan.classifier

import com.google.firebase.firestore.DocumentId

data class Classification(
    @DocumentId
    val id: String = "", // Firestore document ID
    val userId: String = "",
    val result: String = "",
    val confidence: Double = 0.0,
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val _name_: String = ""
)

/*class Classification(userId: String, result: String, confidence: Double, timestamp: Any) {

}*/


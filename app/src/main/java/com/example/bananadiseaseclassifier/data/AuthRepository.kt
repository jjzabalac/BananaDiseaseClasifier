package com.example.bananadiseaseclassifier.data

import android.util.Log
import com.example.bananadiseaseclassifier.Classification
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Attempting to sign in user: $email")
            auth.signInWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "User signed in successfully: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing in user: $email", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Attempting to create new user: $email")
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.d("AuthRepository", "User created successfully: $email")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            val errorMessage = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Invalid email format"
                "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already in use"
                else -> "An error occurred: ${e.message}"
            }
            Log.e("AuthRepository", "Error creating user: $email. $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unexpected error creating user: $email", e)
            Result.failure(Exception("An unexpected error occurred"))
        }
    }

    suspend fun saveClassification(classification: Classification): Result<String> {
        return try {
            Log.d("AuthRepository", "Attempting to save classification: $classification")
            if (classification.userId.isBlank()) {
                Log.e("AuthRepository", "Attempt to save classification without user ID")
                return Result.failure(IllegalArgumentException("User ID is required"))
            }
            val docRef = firestore.collection("classifications").add(classification).await()
            Log.d("AuthRepository", "Classification saved successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving classification", e)
            Result.failure(e)
        }
    }

    suspend fun getClassificationsForUser(userId: String): Result<List<Classification>> {
        return try {
            Log.d("AuthRepository", "Fetching classifications for user: $userId")
            val snapshot = firestore.collection("classifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val classifications = snapshot.toObjects(Classification::class.java)
            Log.d("AuthRepository", "Retrieved ${classifications.size} classifications for user: $userId")
            Result.success(classifications)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching classifications for user: $userId", e)
            Result.failure(e)
        }
    }

    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = auth.currentUser != null
        Log.d("AuthRepository", "Checking if user is logged in: $isLoggedIn")
        return isLoggedIn
    }

    fun getCurrentUserId(): String? {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d("AuthRepository", "Current user ID: $userId")
        } else {
            Log.w("AuthRepository", "No user is currently logged in")
        }
        return userId
    }

    suspend fun signOut() {
        try {
            Log.d("AuthRepository", "Attempting to sign out user")
            auth.signOut()
            Log.d("AuthRepository", "User signed out successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error signing out user", e)
        }
    }
}
package com.github.se.pooltrack.model.auth

/**
 * The signed-in user, decoupled from Firebase's own `FirebaseUser` type so the rest of the app
 * doesn't need to depend on the Firebase SDK directly.
 *
 * @property uid A stable identifier for the user, used to scope their data in Firestore.
 * @property displayName The user's name, or `null` if their Google account doesn't expose one.
 * @property email The user's email, or `null` if their Google account doesn't expose one.
 * @property isAnonymous Whether this is a device-only anonymous session rather than a real Google
 *   account - its data is backed up the same way, but won't follow the user to another device.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isAnonymous: Boolean,
)

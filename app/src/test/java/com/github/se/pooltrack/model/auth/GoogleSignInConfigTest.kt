package com.github.se.pooltrack.model.auth

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Google sign-in needs the web client ID that the google-services plugin generates from
 * google-services.json. It is only there once Google sign-in is enabled in Firebase and the JSON
 * was downloaded afterwards, so a stale JSON (locally or in the CI secret) fails here instead of on
 * a phone.
 */
@RunWith(RobolectricTestRunner::class)
class GoogleSignInConfigTest {

  @Test
  fun build_hasTheGoogleSignInWebClientId() {
    val context = RuntimeEnvironment.getApplication()

    val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

    assertNotEquals("default_web_client_id is missing from google-services.json", 0, id)
    assertTrue(context.getString(id).endsWith(".apps.googleusercontent.com"))
  }
}

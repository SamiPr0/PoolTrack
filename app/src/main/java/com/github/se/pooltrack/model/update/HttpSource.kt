package com.github.se.pooltrack.model.update

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 20_000

/** An open HTTP response. [contentLength] is -1 when the server did not send one. */
class HttpResponse(val body: InputStream, val contentLength: Long) : Closeable {
  override fun close() = body.close()
}

/** Opens HTTP GET requests. A seam so the updater can be tested without a network. */
fun interface HttpSource {
  /** Throws an [IOException] if the request fails or the status is not 2xx. */
  fun open(url: String): HttpResponse
}

/** The [HttpSource] used in the app, backed by [HttpURLConnection]. */
object UrlConnectionSource : HttpSource {
  override fun open(url: String): HttpResponse {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = CONNECT_TIMEOUT_MS
    connection.readTimeout = READ_TIMEOUT_MS
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.setRequestProperty("User-Agent", "PoolTrack")
    val status = connection.responseCode
    if (status !in 200..299) {
      connection.disconnect()
      throw IOException("HTTP $status for $url")
    }
    return HttpResponse(connection.inputStream, connection.contentLengthLong)
  }
}

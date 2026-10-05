package com.github.se.pooltrack.model.update

import java.io.IOException
import java.net.ServerSocket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** Runs [UrlConnectionSource] against a tiny HTTP server on localhost. */
class UrlConnectionSourceTest {

  private lateinit var server: ServerSocket
  private lateinit var thread: Thread

  private val body = "hello"

  @Before
  fun setUp() {
    server = ServerSocket(0)
    thread = Thread {
      try {
        while (true) serve(server.accept())
      } catch (_: IOException) {
        // The socket was closed by tearDown.
      }
    }
    thread.start()
  }

  @After
  fun tearDown() {
    server.close()
    thread.join()
  }

  private fun serve(socket: java.net.Socket) {
    socket.use {
      val path = it.getInputStream().bufferedReader().readLine().split(' ')[1]
      val response =
          when (path) {
            "/ok" ->
                "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
            "/redirect" ->
                "HTTP/1.1 302 Found\r\nLocation: http://localhost:${server.localPort}/ok\r\n" +
                    "Content-Length: 0\r\nConnection: close\r\n\r\n"
            else -> "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
          }
      it.getOutputStream().write(response.toByteArray())
    }
  }

  private fun url(path: String) = "http://localhost:${server.localPort}$path"

  @Test
  fun open_returnsTheBodyAndItsLength() {
    UrlConnectionSource.open(url("/ok")).use {
      assertEquals(body, it.body.bufferedReader().readText())
      assertEquals(body.length.toLong(), it.contentLength)
    }
  }

  @Test
  fun open_followsRedirects() {
    UrlConnectionSource.open(url("/redirect")).use {
      assertEquals(body, it.body.bufferedReader().readText())
    }
  }

  @Test
  fun open_throwsIOException_whenTheStatusIsNotSuccessful() {
    val error = assertThrows(IOException::class.java) { UrlConnectionSource.open(url("/missing")) }

    assertEquals("HTTP 404 for ${url("/missing")}", error.message)
  }
}

package com.github.se.pooltrack.model.update

/**
 * A published release that is newer than the installed app.
 *
 * @property version The version name, without a leading `v` (e.g. `1.2.0`).
 * @property notes The release notes, as plain text. Empty if the release has none.
 * @property apkUrl Where to download the APK from.
 * @property sizeBytes The size of the APK in bytes, or 0 if unknown.
 * @property sha256 The expected SHA-256 of the APK as lowercase hex, or `null` if the release does
 *   not publish one.
 */
data class AppUpdate(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
    val sha256: String?,
)

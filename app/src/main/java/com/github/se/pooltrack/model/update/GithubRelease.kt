package com.github.se.pooltrack.model.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SHA256_PREFIX = "sha256:"

/** The subset of GitHub's "get the latest release" response that the updater needs. */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

/** A file attached to a [GithubRelease]. [digest] looks like `sha256:<hex>` when GitHub has one. */
@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
    val digest: String? = null,
)

/**
 * Turns this release into an [AppUpdate], or `null` if it cannot be installed: a draft or
 * pre-release, no `.apk` asset, or an APK that is not hosted under [trustedUrlPrefix]. The prefix
 * check keeps the app from downloading a file from an unexpected host.
 */
fun GithubRelease.toAppUpdate(trustedUrlPrefix: String): AppUpdate? {
  if (draft || prerelease) return null
  val apk = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
  if (!apk.downloadUrl.startsWith(trustedUrlPrefix)) return null
  return AppUpdate(
      version = tagName.trim().removePrefix("v"),
      notes = body.orEmpty().trim(),
      apkUrl = apk.downloadUrl,
      sizeBytes = apk.size,
      sha256 =
          apk.digest
              ?.takeIf { it.startsWith(SHA256_PREFIX) }
              ?.removePrefix(SHA256_PREFIX)
              ?.lowercase(),
  )
}

package com.github.se.pooltrack.model.update

private const val MAX_PARTS = 3

/**
 * Parses `1`, `1.2` or `1.2.3` (with an optional leading `v`) into `[major, minor, patch]`, padded
 * with zeros. Returns `null` for anything else, including pre-release suffixes like `1.2.3-beta`.
 */
internal fun parseVersion(raw: String): List<Int>? {
  val parts = raw.trim().removePrefix("v").split('.')
  if (parts.size > MAX_PARTS) return null
  val numbers = parts.map { part -> part.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
  return numbers + List(MAX_PARTS - numbers.size) { 0 }
}

/**
 * Whether [candidate] is strictly newer than [current]. A version that cannot be parsed is never
 * considered newer, so a malformed release tag cannot trigger an update prompt.
 */
fun isNewerVersion(candidate: String, current: String): Boolean {
  val candidateParts = parseVersion(candidate) ?: return false
  val currentParts = parseVersion(current) ?: return false
  for (i in 0 until MAX_PARTS) {
    if (candidateParts[i] != currentParts[i]) return candidateParts[i] > currentParts[i]
  }
  return false
}

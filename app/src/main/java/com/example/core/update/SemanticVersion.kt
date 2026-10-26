package com.example.core.update

/**
 * Strict Semantic Versioning (SemVer 2.0) parser and comparator used to compare
 * TVeoClean's installed version (`1.0.0`) against GitHub Release tags (`v1.0.1`, `1.2.0`, etc.).
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) : Comparable<SemanticVersion> {

    val normalizedString: String
        get() = buildString {
            append("$major.$minor.$patch")
            if (!preRelease.isNullOrBlank()) {
                append("-$preRelease")
            }
        }

    fun isNewerThan(other: SemanticVersion): Boolean = compareTo(other) > 0

    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // Stable release (preRelease == null) has higher precedence than pre-release (e.g. 1.0.0 > 1.0.0-rc.1)
        val thisPre = preRelease?.takeIf { it.isNotBlank() }
        val otherPre = other.preRelease?.takeIf { it.isNotBlank() }
        if (thisPre == null && otherPre == null) return 0
        if (thisPre == null) return 1
        if (otherPre == null) return -1

        val thisParts = thisPre.split('.')
        val otherParts = otherPre.split('.')
        val maxLen = maxOf(thisParts.size, otherParts.size)
        for (i in 0 until maxLen) {
            if (i >= thisParts.size) return -1
            if (i >= otherParts.size) return 1
            val a = thisParts[i]
            val b = otherParts[i]
            val aNum = a.toLongOrNull()
            val bNum = b.toLongOrNull()
            val cmp = when {
                aNum != null && bNum != null -> aNum.compareTo(bNum)
                aNum != null -> -1 // Numeric identifiers have lower precedence than non-numeric per SemVer 2.0
                bNum != null -> 1
                else -> a.compareTo(b)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    override fun toString(): String = normalizedString

    companion object {
        private val SEMVER_REGEX = Regex(
            pattern = """^(?:v|release[-_]?|tveoclean[-_]?v?)?(\d+)\.(\d+)(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?(?:\+([0-9A-Za-z.-]+))?$""",
            option = RegexOption.IGNORE_CASE
        )

        fun parseOrNull(rawTag: String?): SemanticVersion? {
            if (rawTag.isNullOrBlank()) return null
            val trimmed = rawTag.trim()
            val match = SEMVER_REGEX.matchEntire(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null
            val preRelease = match.groupValues[4].takeIf { it.isNotBlank() }
            val buildMeta = match.groupValues[5].takeIf { it.isNotBlank() }
            return SemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                preRelease = preRelease,
                buildMetadata = buildMeta
            )
        }
    }
}

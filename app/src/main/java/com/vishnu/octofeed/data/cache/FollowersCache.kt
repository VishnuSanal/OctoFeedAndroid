package com.vishnu.octofeed.data.cache

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cache for tracking followers to detect new follows
 */
class FollowersCache(private val cacheDir: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Serializable
    data class CachedFollower(
        val login: String,
        val avatarUrl: String,
        val id: Long,
        val timestamp: Long = System.currentTimeMillis()
    )

    @Serializable
    data class FollowersData(
        val followers: List<CachedFollower> = emptyList(),
        val lastUpdated: Long = System.currentTimeMillis()
    )

    private val cacheFile: File
        get() = File(cacheDir, "followers_cache.json")

    /**
     * Get cached followers data
     */
    fun getCachedFollowers(): FollowersData? {
        return try {
            if (cacheFile.exists()) {
                val content = cacheFile.readText()
                json.decodeFromString<FollowersData>(content)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FollowersCache", "Error reading cache", e)
            null
        }
    }

    /**
     * Save followers to cache
     */
    fun saveFollowers(followers: List<CachedFollower>) {
        try {
            val data = FollowersData(
                followers = followers,
                lastUpdated = System.currentTimeMillis()
            )
            val content = json.encodeToString(FollowersData.serializer(), data)
            cacheFile.writeText(content)
        } catch (e: Exception) {
            Log.e("FollowersCache", "Error writing cache", e)
        }
    }

    /**
     * Detect new followers by comparing current with cached
     * Returns list of new followers
     */
    fun detectNewFollowers(currentFollowers: List<CachedFollower>): List<CachedFollower> {
        val cached = getCachedFollowers()
        if (cached == null) {
            // First time - save current followers and return empty list
            saveFollowers(currentFollowers)
            return emptyList()
        }

        val cachedLogins = cached.followers.map { it.login }.toSet()
        val newFollowers = currentFollowers.filter { it.login !in cachedLogins }

        // Update cache with current followers
        saveFollowers(currentFollowers)

        return newFollowers
    }

    /**
     * Clear the cache
     */
    fun clear() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            Log.e("FollowersCache", "Error clearing cache", e)
        }
    }
}


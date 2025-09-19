package com.talauncher.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class CommitInfo(
    val commit: String,
    val message: String,
    val date: String,
    val branch: String,
    val buildTime: String
)

object CommitInfoReader {
    suspend fun readCommitInfo(context: Context): CommitInfo? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("commit_info.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            CommitInfo(
                commit = jsonObject.getString("commit"),
                message = jsonObject.getString("message"),
                date = jsonObject.getString("date"),
                branch = jsonObject.getString("branch"),
                buildTime = jsonObject.getString("buildTime")
            )
        } catch (e: Exception) {
            null
        }
    }
}
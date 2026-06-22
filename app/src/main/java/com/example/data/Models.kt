package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val description: String?,
    val html_url: String,
    val language: String?,
    val stargazers_count: Int,
    val default_branch: String
)

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitNode>,
    val truncated: Boolean
)

@JsonClass(generateAdapter = true)
data class GitNode(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class OAuthTokenResponse(
    val access_token: String,
    val scope: String?,
    val token_type: String?
)

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val avatar_url: String?
)

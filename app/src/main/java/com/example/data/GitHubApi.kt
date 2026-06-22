package com.example.data

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import retrofit2.http.Headers

interface GitHubApi {
    @GET("users/{username}/repos?sort=updated&per_page=100")
    suspend fun getUserRepos(@Path("username") username: String): List<GitHubRepo>

    @GET("user/repos?sort=updated&per_page=100")
    suspend fun getMyRepos(): List<GitHubRepo>

    @GET("user")
    suspend fun getAuthenticatedUser(): GitHubUser

    @GET("repos/{owner}/{repo}/git/trees/{branch}")
    suspend fun getRepoTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Query("recursive") recursive: String = "1"
    ): GitTreeResponse
    
    @Headers("Accept: application/json")
    @POST
    suspend fun getAccessToken(
        @Url url: String,
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("code") code: String
    ): OAuthTokenResponse
}

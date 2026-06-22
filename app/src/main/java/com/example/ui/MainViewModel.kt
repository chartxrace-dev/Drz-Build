package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.GitHubApi
import com.example.data.GitHubRepo
import com.example.data.GitHubUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MainViewModel : ViewModel() {

    private val api: GitHubApi
    
    private var accessToken: String? = null

    private val _authState = MutableStateFlow<UiState<GitHubUser>>(UiState.Idle)
    val authState: StateFlow<UiState<GitHubUser>> = _authState.asStateFlow()

    init {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val authInterceptor = okhttp3.Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            accessToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer \$it")
            }
            chain.proceed(requestBuilder.build())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            
        api = retrofit.create(GitHubApi::class.java)
    }

    private val _reposState = MutableStateFlow<UiState<List<GitHubRepo>>>(UiState.Idle)
    val reposState: StateFlow<UiState<List<GitHubRepo>>> = _reposState.asStateFlow()
    
    var currentUsername = MutableStateFlow("")

    fun fetchUserRepos() {
        viewModelScope.launch {
            _reposState.value = UiState.Loading
            try {
                val allRepos = api.getMyRepos()
                val androidRepos = allRepos.filter { 
                    it.language == "Kotlin" || it.language == "Java"
                }
                _reposState.value = UiState.Success(androidRepos)
            } catch (e: Exception) {
                _reposState.value = UiState.Error(e.message ?: "Failed to fetch your repositories")
            }
        }
    }

    fun fetchRepos(username: String) {
        if (username.isBlank()) return
        currentUsername.value = username
        viewModelScope.launch {
            _reposState.value = UiState.Loading
            try {
                val allRepos = api.getUserRepos(username)
                val androidRepos = allRepos.filter { 
                    it.language == "Kotlin" || it.language == "Java"
                }
                _reposState.value = UiState.Success(androidRepos)
            } catch (e: Exception) {
                _reposState.value = UiState.Error(e.message ?: "Failed to fetch repositories")
            }
        }
    }
    
    fun handleOAuthCode(code: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            try {
                val clientId = BuildConfig.GITHUB_CLIENT_ID
                val clientSecret = BuildConfig.GITHUB_CLIENT_SECRET
                
                val response = api.getAccessToken(
                    url = "https://github.com/login/oauth/access_token",
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code
                )
                
                accessToken = response.access_token
                
                // Get authenticated user info
                val user = api.getAuthenticatedUser()
                _authState.value = UiState.Success(user)
                
            } catch (e: Exception) {
                _authState.value = UiState.Error(e.message ?: "OAuth Failed")
            }
        }
    }
}

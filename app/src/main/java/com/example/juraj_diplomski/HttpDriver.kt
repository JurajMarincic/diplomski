package com.example.juraj_diplomski

interface HttpRequest {
    val url: String
    val method: String
    val properties: Map<String, String>
    val body: String?
}

interface HttpNetworkDriver {
    suspend fun makeHttpRequest(request: HttpRequest): String
}
package com.example.com.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val token: String)

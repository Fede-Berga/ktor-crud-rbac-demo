package com.example.com.common

class NotFoundException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
class TooManyRequestsException(message: String = "Too many requests") : Exception(message)
class InvalidCredentialsException(message: String = "Invalid username or password") : Exception(message)

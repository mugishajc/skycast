package com.mugishajc.weather.ui

import retrofit2.HttpException
import java.io.IOException

internal fun Throwable.toUserMessage(): String = when (this) {
    is IOException -> "Can't reach the weather service. Check your connection and try again."
    is HttpException -> "The weather service returned an error (HTTP ${code()}). Try again in a moment."
    else -> "Something went wrong while loading the weather."
}

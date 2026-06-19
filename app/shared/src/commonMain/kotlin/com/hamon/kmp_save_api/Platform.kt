package com.hamon.kmp_save_api

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
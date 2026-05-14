package com.personal.mangarock.di

/**
 * Holds the current server base URL in memory.
 * The OkHttp interceptor reads from this on every request, so changing it
 * takes effect immediately without recreating Retrofit.
 */
class ServerUrlHolder {
    var url: String = "http://192.168.1.65:3000/"
}

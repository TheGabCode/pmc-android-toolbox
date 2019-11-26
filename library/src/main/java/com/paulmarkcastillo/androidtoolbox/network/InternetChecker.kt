package com.paulmarkcastillo.androidtoolbox.network

import android.net.TrafficStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

open class InternetChecker {
    private lateinit var internetCheckJob: Job

    open fun hasInternetAccess(callback: InternetAccessCallback) {
        internetCheckJob = GlobalScope.launch {
            withContext(Dispatchers.Main) {
                callback.onInternetAccessResult(checkInternetAccess())
            }
        }
    }

    fun cancelChecking() {
        if (::internetCheckJob.isInitialized && internetCheckJob.isActive) {
            internetCheckJob.cancel()
        }
    }

    private suspend fun checkInternetAccess(): Boolean {
        return checkInternetAccess("8.8.8.8")
    }

    private suspend fun checkInternetAccess(dns: String): Boolean {
        val socket = Socket()

        return withContext(Dispatchers.IO) {
            try {
                val googleDns = dns
                val dnsPort = 53
                val timeout = 5000
                val tag = 100000
                val socketAddress = InetSocketAddress(googleDns, dnsPort)
                TrafficStats.setThreadStatsTag(tag)
                socket.connect(socketAddress, timeout)
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } finally {
                socket.close()
            }
        }
    }

    interface InternetAccessCallback {
        fun onInternetAccessResult(hasInternetAccess: Boolean)
    }
}
package io.arunbuilds.libraryapp.utils

import android.app.ActivityManager
import android.app.Service
import android.content.Context

fun Context.isServiceRunningInForeground(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager
    manager.getRunningServices(Int.MAX_VALUE).forEach { service ->
        if (serviceClass.name == service.service.className) {
            if (service.foreground) {
                return true
            }
        }
    }
    return false
}

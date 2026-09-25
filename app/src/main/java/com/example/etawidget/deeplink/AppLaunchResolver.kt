package com.example.etawidget.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.etawidget.data.model.ServiceId

class AppLaunchResolver(private val context: Context) {

    /** Launch intent for the first installed app that serves [serviceId], or null if none is installed. */
    fun installedLaunchIntent(serviceId: ServiceId): Intent? =
        ServiceAppInfoRegistry.forService(serviceId).packageNames
            .firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }

    /** Opens the service's app if installed, otherwise its Play Store listing. */
    fun resolveLaunchIntent(serviceId: ServiceId): Intent =
        installedLaunchIntent(serviceId)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(ServiceAppInfoRegistry.forService(serviceId).playStoreUrl))
}

package com.example.etawidget.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.etawidget.data.model.ServiceId
import com.example.etawidget.deeplink.AppLaunchResolver

val ServiceIdKey = ActionParameters.Key<String>("service_id")

/** Opens the tapped service's app to order — which also captures a fresh ETA from its screen. */
class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val serviceId = ServiceId.valueOf(parameters[ServiceIdKey]!!)
        val intent = AppLaunchResolver(context).resolveLaunchIntent(serviceId)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

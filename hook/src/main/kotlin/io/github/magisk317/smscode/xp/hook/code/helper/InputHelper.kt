package io.github.magisk317.smscode.xp.hook.code.helper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import io.github.magisk317.smscode.hook.BuildConfig
import io.github.magisk317.smscode.runtime.bridge.HookRuntimeBridge
import io.github.magisk317.smscode.runtime.verification.AutoInputBroadcastHelper
import io.github.magisk317.smscode.xposed.helper.LocalInputActions
import io.github.magisk317.smscode.xposed.hook.system.SystemInputInjectorHook

/** Scope code broadcasts to trusted receivers, with accessibility fallback after system injection. */
object InputHelper {

    @JvmStatic
    fun sendText(
        context: Context,
        text: String?,
        autoEnter: Boolean = false,
        inputIntervalMs: Long = 0L,
        attemptId: Long? = null,
    ) {
        AutoInputBroadcastHelper.sendText(
            context = context,
            text = text,
            autoEnter = autoEnter,
            inputIntervalMs = inputIntervalMs,
            attemptId = attemptId,
            tokenProvider = {
                HookRuntimeBridge.prefsAccess.getIpcToken(context).takeIf(String::isNotBlank) ?: ""
            },
            actionResolver = SystemInputInjectorHook::resolveActionAutoInput,
            broadcaster = { senderContext, request ->
                // An implicit broadcast exposes the code and token to arbitrary receivers.
                senderContext.sendOrderedBroadcast(
                    Intent(request).setPackage("android"),
                    null,
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (resultCode != Activity.RESULT_OK) {
                                senderContext.sendOrderedBroadcast(
                                    Intent(request).setPackage(BuildConfig.APPLICATION_ID),
                                    null,
                                )
                            }
                        }
                    },
                    Handler(Looper.getMainLooper()),
                    Activity.RESULT_CANCELED,
                    null,
                    null,
                )
            },
        )
    }

    @JvmStatic
    fun sendToast(
        context: Context,
        text: String?,
        duration: Int = android.widget.Toast.LENGTH_LONG,
    ) {
        LocalInputActions.sendToast(context, text, duration)
    }
}

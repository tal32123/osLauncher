package com.talauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageChangeReceiver(
    private val onPackageChanged: () -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"

        fun register(
            context: Context,
            onPackageChanged: () -> Unit
        ): PackageChangeReceiver {
            val receiver = PackageChangeReceiver(onPackageChanged)
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, intentFilter)
            }

            Log.d(TAG, "PackageChangeReceiver registered")
            return receiver
        }

        fun unregister(context: Context, receiver: PackageChangeReceiver?) {
            receiver?.let {
                try {
                    context.unregisterReceiver(it)
                    Log.d(TAG, "PackageChangeReceiver unregistered")
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Attempted to unregister receiver that was not registered", e)
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }

        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.d(TAG, "Package added: $packageName")
                triggerRefresh()
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "Package removed: $packageName")
                triggerRefresh()
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced: $packageName")
                triggerRefresh()
            }
            else -> {
                Log.d(TAG, "Received unexpected action: $action")
            }
        }
    }

    private fun triggerRefresh() {
        scope.launch(Dispatchers.IO) {
            try {
                onPackageChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Error during package change callback", e)
            }
        }
    }
}

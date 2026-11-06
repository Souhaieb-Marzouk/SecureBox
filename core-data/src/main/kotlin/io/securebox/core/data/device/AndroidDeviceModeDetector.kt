package io.securebox.core.data.device

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.device.DeviceModeDetector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the detection rule from the specification: Leanback support OR a television UI mode.
 * Some cheap TV boxes ship without the Leanback feature, which is why the user can override the
 * result during onboarding and later in Settings.
 */
@Singleton
class AndroidDeviceModeDetector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceModeDetector {
        override fun isTelevision(): Boolean {
            val packageManager = context.packageManager
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) return true

            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        }
    }

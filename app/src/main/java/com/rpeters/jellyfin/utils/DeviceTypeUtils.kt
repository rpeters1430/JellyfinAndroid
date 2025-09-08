package com.rpeters.jellyfin.utils

import android.content.Context
import android.content.res.Configuration
import android.view.inputmethod.InputMethodManager

object DeviceTypeUtils {
    enum class DeviceType {
        MOBILE,
        TV,
        TABLET,
    }

    private var cachedDeviceType: DeviceType? = null

    /**
     * Determine the current device type. The result is cached so subsequent calls return the
     * previously computed value. Call [invalidateCache] if configuration changes at runtime.
     */
    fun getDeviceType(context: Context): DeviceType {
        cachedDeviceType?.let { return it }

        val deviceType = when {
            // Check if running on Android TV
            context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
                Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV

            // Check if touchscreen is available (TVs typically don't have touchscreens)
            !context.packageManager.hasSystemFeature("android.hardware.touchscreen") &&
                context.packageManager.hasSystemFeature("android.software.leanback") -> DeviceType.TV

            // Check screen size for tablet detection
            isTablet(context) -> DeviceType.TABLET

            else -> DeviceType.MOBILE
        }

        cachedDeviceType = deviceType
        return deviceType
    }

    /**
     * Clears the cached device type. Use when configuration changes may affect the device type.
     */
    fun invalidateCache() {
        cachedDeviceType = null
    }

    private fun isTablet(context: Context): Boolean {
        val screenSize = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

    fun isTvDevice(context: Context): Boolean {
        return getDeviceType(context) == DeviceType.TV
    }

    fun isKeyboardAvailable(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.size > 0
    }
}

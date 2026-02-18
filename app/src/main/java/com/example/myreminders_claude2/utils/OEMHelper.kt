package com.example.myreminders_claude2.utils

import android.os.Build

object OEMHelper {

    enum class Manufacturer {
        SAMSUNG,
        XIAOMI,
        OPPO,
        VIVO,
        HUAWEI,
        ONEPLUS,
        REALME,
        GOOGLE,
        OTHER
    }

    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> Manufacturer.XIAOMI
            manufacturer.contains("oppo") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("realme") -> Manufacturer.REALME
            manufacturer.contains("google") -> Manufacturer.GOOGLE
            else -> Manufacturer.OTHER
        }
    }

    fun getManufacturerName(): String {
        return when (getManufacturer()) {
            Manufacturer.SAMSUNG -> "Samsung"
            Manufacturer.XIAOMI -> "Xiaomi/MIUI"
            Manufacturer.OPPO -> "Oppo/ColorOS"
            Manufacturer.VIVO -> "Vivo"
            Manufacturer.HUAWEI -> "Huawei/Honor"
            Manufacturer.ONEPLUS -> "OnePlus"
            Manufacturer.REALME -> "Realme"
            Manufacturer.GOOGLE -> "Google Pixel"
            Manufacturer.OTHER -> "Your device"
        }
    }

    fun getBatteryOptimizationSteps(): List<String> {
        return when (getManufacturer()) {
            Manufacturer.SAMSUNG -> listOf(
                "Open Settings",
                "Go to Apps",
                "Tap the three dots (⋮) in the top right",
                "Select 'Special access'",
                "Tap 'Optimize battery usage'",
                "Tap 'Apps not optimized' dropdown and select 'All'",
                "Find 'My Reminders' and toggle it OFF",
                "Also check: Settings → Device care → Battery → Background usage limits → remove My Reminders if listed"
            )

            Manufacturer.XIAOMI -> listOf(
                "Open Settings",
                "Go to Apps → Manage apps",
                "Find and tap 'My Reminders'",
                "Tap 'Battery saver' and select 'No restrictions'",
                "Go back and tap 'Autostart' → Enable",
                "Also enable: Settings → Battery & performance → Battery saver → Enable 'No restrictions' for My Reminders"
            )

            Manufacturer.OPPO -> listOf(
                "Open Settings",
                "Go to Battery → App Battery Management",
                "Find 'My Reminders' and disable 'Optimized'",
                "Go to Settings → Privacy → Permission manager → Autostart",
                "Enable autostart for My Reminders",
                "Also: Settings → Battery → High background power consumption → Allow My Reminders"
            )

            Manufacturer.VIVO -> listOf(
                "Open Settings",
                "Go to Battery",
                "Tap 'Background power consumption'",
                "Find 'My Reminders' and enable 'Allow high background power consumption'",
                "Also enable: Settings → More settings → Applications → Autostart → Enable My Reminders"
            )

            Manufacturer.HUAWEI -> listOf(
                "Open Settings",
                "Go to Apps → Apps",
                "Find and tap 'My Reminders'",
                "Tap 'Battery' and select 'No restrictions' or 'Manual'",
                "Enable 'Run in background'",
                "Go to Settings → Battery → App launch",
                "Find My Reminders and toggle to 'Manage manually'",
                "Enable all three options: Auto-launch, Secondary launch, Run in background"
            )

            Manufacturer.ONEPLUS -> listOf(
                "Open Settings",
                "Go to Battery → Battery optimization",
                "Tap 'All apps' and find 'My Reminders'",
                "Select 'Don't optimize'",
                "Also: Settings → Apps → My Reminders → Battery → Battery optimization → Don't optimize"
            )

            Manufacturer.REALME -> listOf(
                "Open Settings",
                "Go to Battery → More battery settings",
                "Tap 'App battery usage management'",
                "Find 'My Reminders' and disable optimization",
                "Also enable: Settings → App Management → My Reminders → App auto-launch → Enable"
            )

            Manufacturer.GOOGLE -> listOf(
                "Open Settings",
                "Go to Apps → See all apps",
                "Find and tap 'My Reminders'",
                "Tap 'Battery'",
                "Select 'Unrestricted'"
            )

            Manufacturer.OTHER -> listOf(
                "Open Settings",
                "Go to Apps or Battery settings",
                "Find 'My Reminders'",
                "Disable battery optimization",
                "Enable autostart if available",
                "Allow running in background"
            )
        }
    }
}
package com.example.myreminders_claude2.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val hasNotificationPermission: Boolean,
    val hasExactAlarmPermission: Boolean,
    val hasMicrophonePermission: Boolean,
    val hasAnyMissing: Boolean
)

object PermissionChecker {
    fun checkAllPermissions(context: Context): PermissionStatus {
        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val hasMicrophone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasAnyMissing = !hasNotification || !hasExactAlarm || !hasMicrophone

        return PermissionStatus(
            hasNotificationPermission = hasNotification,
            hasExactAlarmPermission = hasExactAlarm,
            hasMicrophonePermission = hasMicrophone,
            hasAnyMissing = hasAnyMissing
        )
    }
}
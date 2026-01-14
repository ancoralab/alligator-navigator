package uk.nktnet.webviewkiosk.ui.components.setting.fielditems.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import uk.nktnet.webviewkiosk.config.UserSettings
import uk.nktnet.webviewkiosk.config.UserSettingsKeys
import uk.nktnet.webviewkiosk.ui.components.setting.fields.TextSettingFieldItem

@Composable
fun AutoShutdownTimeSetting() {
    val context = LocalContext.current
    val userSettings = remember { UserSettings(context) }

    val timeValidator = { input: String ->
        if (input.isEmpty()) {
            true // Empty means disabled
        } else {
            // Validate HH:MM format (24-hour)
            val regex = Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$")
            regex.matches(input)
        }
    }

    TextSettingFieldItem(
        label = "Auto Shutdown Time",
        infoText = """
            Set a time to automatically turn off the TV.
            
            Format: HH:MM (24-hour format)
            Example: 23:00 for 11 PM
            
            Leave empty to disable auto-shutdown.
        """.trimIndent(),
        placeholder = "e.g. 23:00",
        initialValue = userSettings.autoShutdownTime,
        isMultiline = false,
        restricted = userSettings.isRestricted(UserSettingsKeys.Device.AUTO_SHUTDOWN_TIME),
        validator = timeValidator,
        validationMessage = "Please enter a valid time in HH:MM format (00:00 - 23:59)",
        onSave = { 
            userSettings.autoShutdownTime = it
            // Reschedule auto-shutdown with the new time
            uk.nktnet.webviewkiosk.handlers.AutoShutdownReceiver.scheduleAutoShutdown(context)
        }
    )
}

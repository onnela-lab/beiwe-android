package org.beiwe.app.storage

import org.beiwe.app.MainService
import org.beiwe.app.networking.PostRequest
import org.beiwe.app.printe
import org.json.JSONException
import org.json.JSONObject

object SetDeviceSettings {
    @JvmStatic
    @Throws(JSONException::class)
    fun writeDeviceSettings(deviceSettings: JSONObject): Boolean {
        var enablement_change = false
        // Write data stream booleans  --  enablement_changes must be set to true if any of these
        // change. DO NOT CHANGE TO || THAT ENABLES SHORT CIRCUITING WHICH WOULD BREAK THINGS.
        // All of these require the a background service restart if they change.
        enablement_change = enablement_change or PersistentData.setAccelerometerEnabled(deviceSettings.getBoolean("accelerometer"))
        enablement_change = enablement_change or PersistentData.setGyroscopeEnabled(deviceSettings.getBoolean("gyro"))
        enablement_change = enablement_change or PersistentData.setGpsEnabled(deviceSettings.getBoolean("gps"))
        enablement_change = enablement_change or PersistentData.setCallLoggingEnabled(deviceSettings.getBoolean("calls"))
        enablement_change = enablement_change or PersistentData.setTextsEnabled(deviceSettings.getBoolean("texts"))
        PersistentData.setWifiEnabled(deviceSettings.getBoolean("wifi"))  // wifi doesn't have any active state, can ignore.
        enablement_change = enablement_change or PersistentData.setBluetoothEnabled(deviceSettings.getBoolean("bluetooth"))
        enablement_change = enablement_change or PersistentData.setPowerStateEnabled(deviceSettings.getBoolean("power_state"))
        // any sections in try-catch blocks were added after go-live, so must be caught in case the

        try {
            PersistentData.setAllowUploadOverCellularData(deviceSettings.getBoolean("allow_upload_over_cellular_data"))
        } catch (e: JSONException) {
            PersistentData.setAllowUploadOverCellularData(false)
        }

        // Write timer settings  --  regarding restarts, these will be handled by timer logic.
        PersistentData.setAccelerometerOffDuration(deviceSettings.getLong("accelerometer_off_duration_seconds"))
        PersistentData.setAccelerometerOnDuration(deviceSettings.getLong("accelerometer_on_duration_seconds"))
        PersistentData.setAccelerometerFrequency(deviceSettings.getLong("accelerometer_frequency"))
        PersistentData.setGyroscopeOffDuration(deviceSettings.getLong("gyro_off_duration_seconds"))
        PersistentData.setGyroscopeOnDuration(deviceSettings.getLong("gyro_on_duration_seconds"))
        PersistentData.setGyroscopeFrequency(deviceSettings.getLong("gyro_frequency"))
        PersistentData.setBluetoothOnDuration(deviceSettings.getLong("bluetooth_on_duration_seconds"))
        PersistentData.setBluetoothTotalDuration(deviceSettings.getLong("bluetooth_total_duration_seconds"))
        PersistentData.setBluetoothGlobalOffset(deviceSettings.getLong("bluetooth_global_offset_seconds"))
        PersistentData.setCheckForNewSurveysFrequency(deviceSettings.getLong("check_for_new_surveys_frequency_seconds"))
        PersistentData.setCreateNewDataFilesFrequency(deviceSettings.getLong("create_new_data_files_frequency_seconds"))
        PersistentData.setGpsOffDuration(deviceSettings.getLong("gps_off_duration_seconds"))
        PersistentData.setGpsOnDuration(deviceSettings.getLong("gps_on_duration_seconds"))
        PersistentData.setTimeBeforeAutoLogout(deviceSettings.getLong("seconds_before_auto_logout"))
        PersistentData.setUploadDataFilesFrequency(deviceSettings.getLong("upload_data_files_frequency_seconds"))
        PersistentData.setVoiceRecordingMaxTimeLength(deviceSettings.getLong("voice_recording_max_time_length_seconds"))

        // wifi periodicity needs to have a minimum because it creates a new file every scan
        var wifiLogFrequencySeconds = deviceSettings.getLong("wifi_log_frequency_seconds")
        if (wifiLogFrequencySeconds < 10)
            wifiLogFrequencySeconds = 10
        PersistentData.setWifiLogFrequency(wifiLogFrequencySeconds)

        // Write text strings
        PersistentData.setAboutPageText(deviceSettings.getString("about_page_text"))
        PersistentData.setCallClinicianButtonText(deviceSettings.getString("call_clinician_button_text"))
        PersistentData.setConsentFormText(deviceSettings.getString("consent_form_text"))
        PersistentData.setSurveySubmitSuccessToastText(deviceSettings.getString("survey_submit_success_toast_text"))

        // Anonymized hashing
        try {
            enablement_change = enablement_change or PersistentData.setUseAnonymizedHashing(deviceSettings.getBoolean("use_anonymized_hashing"))
        } catch (e: JSONException) {
            PersistentData.setUseAnonymizedHashing(false)
        }

        // Use GPS Fuzzing
        try {
            enablement_change = enablement_change or PersistentData.setUseGpsFuzzing(deviceSettings.getBoolean("use_gps_fuzzing"))
        } catch (e: JSONException) {
            PersistentData.setUseGpsFuzzing(false)
        }

        // Call button toggles
        try {
            PersistentData.setCallClinicianButtonEnabled(deviceSettings.getBoolean("call_clinician_button_enabled"))
        } catch (e: JSONException) {
            PersistentData.setCallClinicianButtonEnabled(true)
        }
        try {
            PersistentData.setCallResearchAssistantButtonEnabled(deviceSettings.getBoolean("call_research_assistant_button_enabled"))
        } catch (e: JSONException) {
            PersistentData.setCallResearchAssistantButtonEnabled(true)
        }
        return enablement_change
    }


    fun dispatchUpdateDeviceSettings() {
        // this is a very simple wrapper around a thread that catches and suppresses all exceptions
        val thread = Thread( {
            val jsonResponseString: String
            val deviceSettingsJSON: JSONObject

            try {
                jsonResponseString = PostRequest.httpRequestString(
                        "",  PostRequest.addWebsitePrefix("/get_latest_device_settings")
                )
            } catch (e: NullPointerException) {
                printe("error getting device settings 1")
                return@Thread
            }

            try {
                deviceSettingsJSON = JSONObject(jsonResponseString)
            } catch (e: JSONException) {
                printe("error parsing device settings JSON 1")
                e.printStackTrace()
                return@Thread
            }

            try {
                // if this returns true we need to restart the app.  The app is plausibly doing
                // important stuff, because timers just went off, and restarting is not... fully
                // safe, so we will wait 5 seconds (extreme overkill).  The only Real situation
                // where we don't want to explode the the app is if the user is taking a survey.
                // (could also use login state)
                // I have tested this and it works. We could show a toast.
                if (writeDeviceSettings(deviceSettingsJSON)){
                    while (PersistentData.getTakingSurvey()) {
                        // printw("waiting for survey to finish")
                        Thread.sleep(5000)
                    }
                    printe("restarting service?")
                    MainService.localHandle!!.exit_and_restart_background_service()
                }
            } catch (e: JSONException) {
                printe("error parsing device settings JSON 2")
                e.printStackTrace()
                return@Thread
            }
        },  "DeviceSettingsThread")
        thread.start()
    }
}

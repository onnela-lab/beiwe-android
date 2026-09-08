package org.beiwe.app.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.beiwe.app.BuildConfig
import org.beiwe.app.JSONUtils
import org.beiwe.app.R
import org.json.JSONArray
import org.json.JSONException

const val NULL_ID = "NULLID"

/**  Editor key-strings  */
const val PREF_NAME = "BeiwePref"
const val SERVER_URL_KEY = "serverUrl"
const val KEY_ID = "uid"
const val KEY_PASSWORD = "password"
const val IS_REGISTERED = "IsRegistered"
const val STUDY_NAME = "study_name"
const val STUDY_ID = "study_id"
const val IS_TAKING_SURVEY = "is_taking_survey"

const val LOGIN_EXPIRATION = "loginExpirationTimestamp"
const val PCP_PHONE_KEY = "primary_care"
const val PASSWORD_RESET_NUMBER_KEY = "reset_number"

const val FCM_INSTANCE_ID = "fcmInstanceID"

const val ACCELEROMETER_ENABLED = "accelerometer"
const val GYROSCOPE_ENABLED = "gyroscope"
const val GPS_ENABLED = "gps"
const val CALLS_ENABLED = "calls"
const val TEXTS_ENABLED = "texts"
const val WIFI_ENABLED = "wifi"
const val BLUETOOTH_ENABLED = "bluetooth"
const val POWER_STATE_ENABLED = "power_state"
const val AMBIENT_AUDIO_ENABLED = "ambient_audio"
const val ALLOW_UPLOAD_OVER_CELLULAR_DATA = "allow_upload_over_cellular_data"

// you can never never change these const values; ever.  If you do it will break the study data gathering
const val ACCELEROMETER_OFF_SECONDS = "accelerometer_off_duration_seconds"
const val ACCELEROMETER_ON_SECONDS = "accelerometer_on_duration_seconds"
const val ACCELEROMETER_FREQUENCY = "accelerometer_frequency"
const val AMBIENT_AUDIO_OFF_SECONDS = "ambient_audio_off_duration_seconds"
const val AMBIENT_AUDIO_ON_SECONDS = "ambient_audio_on_duration_seconds"
const val AMBIENT_AUDIO_SAMPLE_RATE = "ambient_audio_sample_rate"
const val AMBIENT_AUDIO_BITRATE = "ambient_audio_bitrate"
const val GYROSCOPE_ON_SECONDS = "gyro_on_duration_seconds"
const val GYROSCOPE_OFF_SECONDS = "gyro_off_duration_seconds"
const val GYROSCOPE_FREQUENCY = "gyro_frequency"
const val BLUETOOTH_ON_SECONDS = "bluetooth_on_duration_seconds"
const val BLUETOOTH_TOTAL_SECONDS = "bluetooth_total_duration_seconds"
const val BLUETOOTH_GLOBAL_OFFSET_SECONDS = "bluetooth_global_offset_seconds"
const val CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS = "check_for_new_surveys_frequency_seconds"
const val CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS = "create_new_data_files_frequency_seconds"
const val GPS_OFF_SECONDS = "gps_off_duration_seconds"
const val GPS_ON_SECONDS = "gps_on_duration_seconds"
const val SECONDS_BEFORE_AUTO_LOGOUT = "seconds_before_auto_logout"
const val UPLOAD_DATA_FILES_FREQUENCY_SECONDS = "upload_data_files_frequency_seconds"
const val VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS = "voice_recording_max_time_length_seconds"
const val WIFI_LOG_FREQUENCY_SECONDS = "wifi_log_frequency_seconds"
const val SURVEY_IDS = "survey_ids"
const val LAST_REQUESTED_PERMISSION = "last_requested_permission"
// last-known permission/setting states, used to log permission_changed app log events on foreground
const val LAST_KNOWN_LOCATION_PERMISSION_STATE = "last_known_location_permission_state"
const val LAST_KNOWN_NOTIFICATION_PERMISSION_STATE = "last_known_notification_permission_state"
const val LAST_KNOWN_BATTERY_OPTIMIZATION_STATE = "last_known_battery_optimization_state"

const val ABOUT_PAGE_TEXT_KEY = "about_page_text"
const val CALL_CLINICIAN_BUTTON_TEXT_KEY = "call_clinician_button_text"
const val CONSENT_FORM_TEXT_KEY = "consent_form_text"
const val SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY = "survey_submit_success_toast_text"

const val USE_GPS_FUZZING_KEY = "gps_fuzzing_key"
const val LATITUDE_OFFSET_KEY = "latitude_offset_key"
const val LONGITUDE_OFFSET_KEY = "longitude_offset_key"

const val CALL_CLINICIAN_BUTTON_ENABLED_KEY = "call_clinician_button_enabled"
const val CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY = "call_research_assistant_button_enabled"

const val MOST_RECENT_ONCREATE_ACTIVITY_STATE = "most_recent_oncreate_activity_state"
const val MOST_RECENT_ONRESUME_ACTIVITY_STATE = "most_recent_onresume_activity_state"
const val MOST_RECENT_ONPAUSE_ACTIVITY_STATE = "most_recent_onresume_activity_state"
const val MOST_RECENT_ONSERVICEBOUND_ACTIVITY_STATE = "most_recent_onservecbound_activity_state"
const val MOST_RECENT_ONSERVICEUNBOUND_ACTIVITY_STATE = "most_recent_onservecbound_activity_state"
const val MOST_RECENT_UPLOAD_ATTEMPT = "most_recent_upload_attempt"
const val MOST_RECENT_UPLOAD_START = "most_recent_upload_start"

const val MOST_RECENT_SERVICE_START = "most_recent_service_start"
const val MOST_RECENT_SERVICE_START_FIRST_RUN = "most_recent_service_start_initial"
const val MOST_RECENT_SERVICE_RUN_ALL_LOGIC = "most_recent_run_all_logic"
const val MOST_RECENT_SERVICE_ON_START_COMMAND = "most_recent_on_start_command"
const val MOST_RECENT_SERVICE_ON_UNBIND = "most_recent_on_unbind"
const val MOST_RECENT_SERVICE_ON_DESTROY = "most_recent_on_destroy"
const val MOST_RECENT_SERVICE_ON_LOW_MEMORY = "most_recent_on_low_memory"
const val MOST_RECENT_SERVICE_ON_TRIM_MEMORY = "most_recent_on_trim_memory"
const val MOST_RECENT_SERVICE_ON_TASK_REMOVED = "most_recent_on_task_removed"

const val MOST_RECENT_ACCELEROMETER_START = "most_recent_accelerometer_start"
const val MOST_RECENT_ACCELEROMETER_STOP = "most_recent_accelerometer_stop"
const val MOST_RECENT_AMBIENT_AUDIO_START = "most_recent_ambient_audio_start"
const val MOST_RECENT_AMBIENT_AUDIO_STOP = "most_recent_ambient_audio_stop"
const val MOST_RECENT_BLUETOOTH_START = "most_recent_bluetooth_start"
const val MOST_RECENT_BLUETOOTH_STOP = "most_recent_bluetooth_stop"
const val MOST_RECENT_GPS_START = "most_recent_gps_start"
const val MOST_RECENT_GPS_STOP = "most_recent_gps_stop"
const val MOST_RECENT_GYROSCOPE_START = "most_recent_gyroscope_start"
const val MOST_RECENT_GYROSCOPE_STOP = "most_recent_gyroscope_stop"

const val REGISTRATION_PHONE_NUMBER_EVER_PROMPTED = "registration_phone_number_ever_prompted"

/*#################################################################################################
################################### Initializing and Editing ######################################
#################################################################################################*/

object PersistentData {
    var isInitialized = false
    // some critical global scope variables...
    lateinit var pref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var appContext: Context

    @Synchronized fun initialize(applicationContext: Context) {
        if (isInitialized)
            return
        isInitialized = true
        appContext = applicationContext
        pref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        editor = pref.edit()
        editor.commit()
    }

    /*#################################################################################################
    ################################### Initializing and Editing ######################################
    #################################################################################################*/

    // convenience methods for different types but all with the same name, makes everything easier.
    @JvmStatic fun putCommit(name: String, b: Boolean): Boolean {
        var prior_value: Boolean? = null
        if (pref.contains(name)) prior_value = pref.getBoolean(name, true) // default value can't be null for booleans (fair enough)
        editor.putBoolean(name, b)
        editor.commit()
        // return True if it changed
        return if (prior_value == null) true else prior_value != b
    }
    // we can't return that boolean in setters, any return value is invalid.
    @JvmStatic fun _putCommit(name: String, b: Boolean) {
        editor.putBoolean(name, b)
        editor.commit()
    }
    @JvmStatic fun putCommit(name: String, l: Long) {
        editor.putLong(name, l)
        editor.commit()
    }
    @JvmStatic fun putCommit(name: String, s: String) {
        editor.putString(name, s)
        editor.commit()
    }
    @JvmStatic fun putCommit(name: String, f: Float) {
        editor.putFloat(name, f)
        editor.commit()
    }
    @JvmStatic fun putCommit(name: String, i: Int) {
        editor.putInt(name, i)
        editor.commit()
    }

    /*##############################################################################################
    ########################################### User State #########################################
    ##############################################################################################*/

    /** Quick check for login.  */
    @JvmStatic fun isLoggedIn(): Boolean {
        if (pref == null) Log.w("LoginManager", "FAILED AT ISLOGGEDIN")
        // If the current time is earlier than the expiration time, return TRUE; else FALSE
        return System.currentTimeMillis() < pref.getLong(LOGIN_EXPIRATION, 0)
    }

    /** Set the login session to expire a fixed amount of time in the future  */
    @JvmStatic fun loginOrRefreshLogin() { putCommit(LOGIN_EXPIRATION, System.currentTimeMillis() + getTimeBeforeAutoLogout()) }

    /** Set the login session to "expired"  */
    @JvmStatic fun logout() {
        // LOGIN_EXPIRATION must be stored as a long
        putCommit(LOGIN_EXPIRATION, 0L)
    }

    // IS_REGISTERED
    @JvmStatic fun getIsRegistered(): Boolean { return pref.getBoolean(IS_REGISTERED, false) }
    @JvmStatic fun setIsRegistered(value: Boolean) { putCommit(IS_REGISTERED, value) }
    @JvmStatic fun setLastRequestedPermission(value: String) { putCommit(LAST_REQUESTED_PERMISSION, value) }
    @JvmStatic fun getLastRequestedPermission(): String { return pref.getString(LAST_REQUESTED_PERMISSION, "")?: "" }
    @JvmStatic fun setLastKnownLocationPermissionState(value: String) { putCommit(LAST_KNOWN_LOCATION_PERMISSION_STATE, value) }
    @JvmStatic fun getLastKnownLocationPermissionState(): String { return pref.getString(LAST_KNOWN_LOCATION_PERMISSION_STATE, "")?: "" }
    @JvmStatic fun setLastKnownNotificationPermissionState(value: String) { putCommit(LAST_KNOWN_NOTIFICATION_PERMISSION_STATE, value) }
    @JvmStatic fun getLastKnownNotificationPermissionState(): String { return pref.getString(LAST_KNOWN_NOTIFICATION_PERMISSION_STATE, "")?: "" }
    @JvmStatic fun setLastKnownBatteryOptimizationState(value: String) { putCommit(LAST_KNOWN_BATTERY_OPTIMIZATION_STATE, value) }
    @JvmStatic fun getLastKnownBatteryOptimizationState(): String { return pref.getString(LAST_KNOWN_BATTERY_OPTIMIZATION_STATE, "")?: "" }
    @JvmStatic fun getTakingSurvey(): Boolean { return pref.getBoolean(IS_TAKING_SURVEY, false) }
    @JvmStatic fun setTakingSurvey() { putCommit(IS_TAKING_SURVEY, true) }
    @JvmStatic fun setNotTakingSurvey() { putCommit(IS_TAKING_SURVEY, false) }


    /*##############################################################################################
    ########################################### App State ##########################################
    ##############################################################################################*/

    // app service recent events
    @JvmStatic var serviceStartCommand: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_START_COMMAND, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_START_COMMAND, value)
    @JvmStatic var serviceOnUnbind: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_UNBIND, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_UNBIND, value)
    @JvmStatic var serviceOnDestroy: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_DESTROY, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_DESTROY, value)
    @JvmStatic var serviceOnLowMemory: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_LOW_MEMORY, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_LOW_MEMORY, value)
    @JvmStatic var serviceOnTrimMemory: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_TRIM_MEMORY, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_TRIM_MEMORY, value)
    @JvmStatic var serviceOnTaskRemoved: String
        get() =  pref.getString(MOST_RECENT_SERVICE_ON_TASK_REMOVED, "")?: ""
        set(value) =  putCommit(MOST_RECENT_SERVICE_ON_TASK_REMOVED, value)
    @JvmStatic var appOnRunAllLogic: String
        get() = pref.getString(MOST_RECENT_SERVICE_RUN_ALL_LOGIC, "")?: ""
        set(value) = putCommit(MOST_RECENT_SERVICE_RUN_ALL_LOGIC, value)
    @JvmStatic var appOnServiceStart: String
        get() = pref.getString(MOST_RECENT_SERVICE_START, "")?: ""
        set(value) = putCommit(MOST_RECENT_SERVICE_START, value)
    @JvmStatic var appOnServiceStartFirstRun: String
        get() = pref.getString(MOST_RECENT_SERVICE_START_FIRST_RUN, "")?: ""
        set(value) = putCommit(MOST_RECENT_SERVICE_START_FIRST_RUN, value)

    // app activity recent events
    @JvmStatic var appOnServiceBoundActivity: String
        get() = pref.getString(MOST_RECENT_ONSERVICEBOUND_ACTIVITY_STATE, "")?: ""
        set(value) = putCommit(MOST_RECENT_ONSERVICEBOUND_ACTIVITY_STATE, value)
    @JvmStatic var appOnServiceUnboundActivity: String
        get() = pref.getString(MOST_RECENT_ONSERVICEUNBOUND_ACTIVITY_STATE, "")?: ""
        set(value) = putCommit(MOST_RECENT_ONSERVICEUNBOUND_ACTIVITY_STATE, value)
    @JvmStatic var appOnCreateActivity: String
        get() = pref.getString(MOST_RECENT_ONCREATE_ACTIVITY_STATE, "")?: ""
        set(value) = putCommit(MOST_RECENT_ONCREATE_ACTIVITY_STATE, value)
    @JvmStatic var appOnResumeActivity: String
        get() = pref.getString(MOST_RECENT_ONRESUME_ACTIVITY_STATE, "")?: ""
        set(value) = putCommit(MOST_RECENT_ONRESUME_ACTIVITY_STATE, value)
    @JvmStatic var appOnPauseActivity: String
        get() = pref.getString(MOST_RECENT_ONPAUSE_ACTIVITY_STATE, "")?: ""
        set(value) = putCommit(MOST_RECENT_ONPAUSE_ACTIVITY_STATE, value)

    // app data stream recent events
    @JvmStatic var accelerometerStart: String
        get() =  pref.getString(MOST_RECENT_ACCELEROMETER_START, "")?: ""
        set(value) =  putCommit(MOST_RECENT_ACCELEROMETER_START, value)
    @JvmStatic var accelerometerStop: String
        get() =  pref.getString(MOST_RECENT_ACCELEROMETER_STOP, "")?: ""
        set(value) =  putCommit(MOST_RECENT_ACCELEROMETER_STOP, value)
    @JvmStatic var ambientAudioStart: String
        get() =  pref.getString(MOST_RECENT_AMBIENT_AUDIO_START, "")?: ""
        set(value) =  putCommit(MOST_RECENT_AMBIENT_AUDIO_START, value)
    @JvmStatic var ambientAudioStop: String
        get() =  pref.getString(MOST_RECENT_AMBIENT_AUDIO_STOP, "")?: ""
        set(value) =  putCommit(MOST_RECENT_AMBIENT_AUDIO_STOP, value)
    @JvmStatic var bluetoothStart: String
        get() =  pref.getString(MOST_RECENT_BLUETOOTH_START, "")?: ""
        set(value) =  putCommit(MOST_RECENT_BLUETOOTH_START, value)
    @JvmStatic var bluetoothStop: String
        get() =  pref.getString(MOST_RECENT_BLUETOOTH_STOP, "")?: ""
        set(value) =  putCommit(MOST_RECENT_BLUETOOTH_STOP, value)
    @JvmStatic var gpsStart: String
        get() =  pref.getString(MOST_RECENT_GPS_START, "")?: ""
        set(value) =  putCommit(MOST_RECENT_GPS_START, value)
    @JvmStatic var gpsStop: String
        get() =  pref.getString(MOST_RECENT_GPS_STOP, "")?: ""
        set(value) =  putCommit(MOST_RECENT_GPS_STOP, value)
    @JvmStatic var gyroscopeStart: String
        get() =  pref.getString(MOST_RECENT_GYROSCOPE_START, "")?: ""
        set(value) =  putCommit(MOST_RECENT_GYROSCOPE_START, value)
    @JvmStatic var gyroscopeStop: String
        get() =  pref.getString(MOST_RECENT_GYROSCOPE_STOP, "")?: ""
        set(value) =  putCommit(MOST_RECENT_GYROSCOPE_STOP, value)
    @JvmStatic var appUploadAttempt: String
        get() = pref.getString(MOST_RECENT_UPLOAD_ATTEMPT, "")?: ""
        set(value) = putCommit(MOST_RECENT_UPLOAD_ATTEMPT, value)
    @JvmStatic var appUploadStart: String
        get() = pref.getString(MOST_RECENT_UPLOAD_START, "")?: ""
        set(value) = putCommit(MOST_RECENT_UPLOAD_START, value)

    @JvmStatic var registrationPhoneNumberEverPrompted: Boolean
        get() =  pref.getBoolean(REGISTRATION_PHONE_NUMBER_EVER_PROMPTED, false)
        set(value) = _putCommit(REGISTRATION_PHONE_NUMBER_EVER_PROMPTED, value)

    /*######################################################################################
    ##################################### Passwords ########################################
    ######################################################################################*/

    /**Checks that an input matches valid password requirements. (this only checks length)
     * Throws up an alert notifying the user if the password is not valid.
     * @param password
     * @return true or false based on password requirements. */
    @JvmStatic fun passwordMeetsRequirements(password: String): Boolean { return password.length >= minPasswordLength() }
    @JvmStatic fun minPasswordLength(): Int { return if (BuildConfig.APP_IS_BETA) 1 else 6 }

    /**Takes an input string and returns a boolean value stating whether the input matches the current password.  */
    @JvmStatic fun checkPassword(input: String?): Boolean { return getPassword() == EncryptionEngine.safeHash(input) }

    /**Sets a password to a hash of the provided value.  */
    @JvmStatic fun setPassword(password: String?) { putCommit(KEY_PASSWORD, EncryptionEngine.safeHash(password)) }

    /*#####################################################################################
    ################################# Firebase Cloud Messaging Instance ID ################
    #####################################################################################*/
    @JvmStatic fun getFCMInstanceID(): String? { return pref.getString(FCM_INSTANCE_ID, null) }
    @JvmStatic fun setFCMInstanceID(fcmInstanceID: String) { putCommit(FCM_INSTANCE_ID, fcmInstanceID) }

    /*#####################################################################################
    ################################# Listener Settings ###################################
    #####################################################################################*/
    @JvmStatic fun getAccelerometerEnabled(): Boolean { return pref.getBoolean(ACCELEROMETER_ENABLED, false) }
    @JvmStatic fun setAccelerometerEnabled(enabled: Boolean): Boolean { return putCommit(ACCELEROMETER_ENABLED, enabled) }
    @JvmStatic fun getAllowUploadOverCellularData(): Boolean { return pref.getBoolean(ALLOW_UPLOAD_OVER_CELLULAR_DATA, false) }
    @JvmStatic fun setAllowUploadOverCellularData(enabled: Boolean) { putCommit(ALLOW_UPLOAD_OVER_CELLULAR_DATA, enabled) }
    @JvmStatic fun getAmbientAudioEnabled(): Boolean { return pref.getBoolean(AMBIENT_AUDIO_ENABLED, false) }
    @JvmStatic fun setAmbientAudioCollectionIsEnabled(enabled: Boolean): Boolean { return putCommit(AMBIENT_AUDIO_ENABLED, enabled) }
    @JvmStatic fun getBluetoothEnabled(): Boolean { return pref.getBoolean(BLUETOOTH_ENABLED, false) }
    @JvmStatic fun setBluetoothEnabled(enabled: Boolean): Boolean { return putCommit(BLUETOOTH_ENABLED, enabled) }
    @JvmStatic fun getCallLoggingEnabled(): Boolean { return pref.getBoolean(CALLS_ENABLED, false) }
    @JvmStatic fun setCallLoggingEnabled(enabled: Boolean): Boolean { return putCommit(CALLS_ENABLED, enabled) }
    @JvmStatic fun getGpsEnabled(): Boolean { return pref.getBoolean(GPS_ENABLED, false) }
    @JvmStatic fun setGpsEnabled(enabled: Boolean): Boolean { return putCommit(GPS_ENABLED, enabled) }
    @JvmStatic fun getGyroscopeEnabled(): Boolean { return pref.getBoolean(GYROSCOPE_ENABLED, false) }
    @JvmStatic fun setGyroscopeEnabled(enabled: Boolean): Boolean { return putCommit(GYROSCOPE_ENABLED, enabled) }
    @JvmStatic fun getPowerStateEnabled(): Boolean { return pref.getBoolean(POWER_STATE_ENABLED, false) }
    @JvmStatic fun setPowerStateEnabled(enabled: Boolean): Boolean { return putCommit(POWER_STATE_ENABLED, enabled) }
    @JvmStatic fun getTextsEnabled(): Boolean { return pref.getBoolean(TEXTS_ENABLED, false) }
    @JvmStatic fun setTextsEnabled(enabled: Boolean): Boolean { return putCommit(TEXTS_ENABLED, enabled) }
    @JvmStatic fun getWifiEnabled(): Boolean { return pref.getBoolean(WIFI_ENABLED, false) }
    @JvmStatic fun setWifiEnabled(enabled: Boolean) { putCommit(WIFI_ENABLED, enabled) }

    /*#####################################################################################
    ################################## Timer Settings #####################################
    #####################################################################################*/
    // Default values are only used if app doesn't download custom timings, which shouldn't ever happen.
    @JvmStatic fun getAccelerometerOffDuration(): Long { return 1000L * pref.getLong(ACCELEROMETER_OFF_SECONDS, 10) }
    @JvmStatic fun setAccelerometerOffDuration(seconds: Long) { putCommit(ACCELEROMETER_OFF_SECONDS, seconds) }
    @JvmStatic fun getAccelerometerOnDuration(): Long { return 1000L * pref.getLong(ACCELEROMETER_ON_SECONDS, (10 * 60).toLong()) }
    @JvmStatic fun setAccelerometerOnDuration(seconds: Long) { putCommit(ACCELEROMETER_ON_SECONDS, seconds) }
    @JvmStatic fun getAccelerometerFrequency(): Long { return pref.getLong(ACCELEROMETER_FREQUENCY, 5) }
    @JvmStatic fun setAccelerometerFrequency(frequency: Long) { putCommit(ACCELEROMETER_FREQUENCY, frequency) }
    @JvmStatic fun getAmbientAudioOffDuration(): Long { return 1000L * pref.getLong(AMBIENT_AUDIO_OFF_SECONDS, (10 * 60).toLong()) }
    @JvmStatic fun setAmbientAudioOffDuration(seconds: Long) { putCommit(AMBIENT_AUDIO_OFF_SECONDS, seconds) }
    @JvmStatic fun getAmbientAudioOnDuration(): Long { return 1000L * pref.getLong(AMBIENT_AUDIO_ON_SECONDS, (10 * 60).toLong()) }
    @JvmStatic fun setAmbientAudioOnDuration(seconds: Long) { putCommit(AMBIENT_AUDIO_ON_SECONDS, seconds) }
    @JvmStatic fun getAmbientAudioSampleRate(): Long { return pref.getLong(AMBIENT_AUDIO_SAMPLE_RATE, 22050) }
    @JvmStatic fun setAmbientAudioSampleRate(rate: Long) { putCommit(AMBIENT_AUDIO_SAMPLE_RATE, rate) }
    @JvmStatic fun getAmbientAudioBitrate(): Long { return pref.getLong(AMBIENT_AUDIO_BITRATE, 24000) }
    @JvmStatic fun setAmbientAudioBitrate(rate: Long) { putCommit(AMBIENT_AUDIO_BITRATE, rate) }
    @JvmStatic fun getBluetoothGlobalOffset(): Long { return 1000L * pref.getLong(BLUETOOTH_GLOBAL_OFFSET_SECONDS, (0 * 60).toLong()) }
    @JvmStatic fun setBluetoothGlobalOffset(seconds: Long) { putCommit(BLUETOOTH_GLOBAL_OFFSET_SECONDS, seconds) }
    @JvmStatic fun getBluetoothOnDuration(): Long { return 1000L * pref.getLong(BLUETOOTH_ON_SECONDS, (1 * 60).toLong()) }
    @JvmStatic fun setBluetoothOnDuration(seconds: Long) { putCommit(BLUETOOTH_ON_SECONDS, seconds) }
    @JvmStatic fun getBluetoothTotalDuration(): Long { return 1000L * pref.getLong(BLUETOOTH_TOTAL_SECONDS, (5 * 60).toLong()) }
    @JvmStatic fun setBluetoothTotalDuration(seconds: Long) { putCommit(BLUETOOTH_TOTAL_SECONDS, seconds) }
    @JvmStatic fun getCheckForNewSurveysFrequency(): Long { return 1000L * pref.getLong(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, (24 * 60 * 60).toLong()) }
    @JvmStatic fun setCheckForNewSurveysFrequency(seconds: Long) { putCommit(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, seconds) }
    @JvmStatic fun getCreateNewDataFilesFrequency(): Long { return 1000L * pref.getLong(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, (15 * 60).toLong()) }
    @JvmStatic fun setCreateNewDataFilesFrequency(seconds: Long) { putCommit(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, seconds) }
    @JvmStatic fun getGpsOffDuration(): Long { return 1000L * pref.getLong(GPS_OFF_SECONDS, (5 * 60).toLong()) }
    @JvmStatic fun setGpsOffDuration(seconds: Long) { putCommit(GPS_OFF_SECONDS, seconds) }
    @JvmStatic fun getGpsOnDuration(): Long { return 1000L * pref.getLong(GPS_ON_SECONDS, (5 * 60).toLong()) }
    @JvmStatic fun setGpsOnDuration(seconds: Long) { putCommit(GPS_ON_SECONDS, seconds) }
    @JvmStatic fun getGyroscopeOffDuration(): Long { return 1000L * pref.getLong(GYROSCOPE_OFF_SECONDS, 10) }
    @JvmStatic fun setGyroscopeOffDuration(seconds: Long) { putCommit(GYROSCOPE_OFF_SECONDS, seconds) }
    @JvmStatic fun getGyroscopeOnDuration(): Long { return 1000L * pref.getLong(GYROSCOPE_ON_SECONDS, (10 * 60).toLong()) }
    @JvmStatic fun setGyroscopeOnDuration(seconds: Long) { putCommit(GYROSCOPE_ON_SECONDS, seconds) }
    @JvmStatic fun getGyroscopeFrequency(): Long { return pref.getLong(GYROSCOPE_FREQUENCY, 5) }
    @JvmStatic fun setGyroscopeFrequency(frequency: Long) { putCommit(GYROSCOPE_FREQUENCY, frequency) }
    @JvmStatic fun getTimeBeforeAutoLogout(): Long { return 1000L * pref.getLong(SECONDS_BEFORE_AUTO_LOGOUT, (5 * 60).toLong()) }
    @JvmStatic fun setTimeBeforeAutoLogout(seconds: Long) { putCommit(SECONDS_BEFORE_AUTO_LOGOUT, seconds) }
    @JvmStatic fun setUploadDataFilesFrequency(seconds: Long) { putCommit(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, seconds) }
    @JvmStatic fun getUploadDataFilesFrequency(): Long { return 1000L * pref.getLong(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, 60) }
    @JvmStatic fun getVoiceRecordingMaxTimeLength(): Long { return 1000L * pref.getLong(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, (4 * 60).toLong()) }
    @JvmStatic fun setVoiceRecordingMaxTimeLength(seconds: Long) { putCommit(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, seconds) }
    @JvmStatic fun getWifiLogFrequency(): Long { return 1000L * pref.getLong(WIFI_LOG_FREQUENCY_SECONDS, (5 * 60).toLong()) }
    @JvmStatic fun setWifiLogFrequency(seconds: Long) { putCommit(WIFI_LOG_FREQUENCY_SECONDS, seconds) }

    // accelerometer, gyroscope bluetooth, new surveys, create data files, gps, logout,upload, wifilog (not voice recording, that doesn't apply
    @JvmStatic fun setMostRecentAlarmTime(identifier: String, time: Long) { putCommit("$identifier-prior_alarm", time) }
    @JvmStatic fun getMostRecentAlarmTime(identifier: String): Long { return pref.getLong("$identifier-prior_alarm", 0) }
    // we want default to be 0 so that checks "is this value less than the current expected value" (eg "did this timer event pass already")



    /*###########################################################################################
    ################################### Text Strings ############################################
    ###########################################################################################*/
    @JvmStatic fun getAboutPageText(): String {
        val defaultText = appContext.getString(R.string.default_about_page_text)
        return pref.getString(ABOUT_PAGE_TEXT_KEY, defaultText)?: defaultText
    }
    @JvmStatic fun getCallClinicianButtonText(): String {
        val defaultText = appContext.getString(R.string.default_call_clinician_text)
        return pref.getString(CALL_CLINICIAN_BUTTON_TEXT_KEY, defaultText)?: defaultText
    }
    @JvmStatic fun getConsentFormText(): String {
        val defaultText = appContext.getString(R.string.default_consent_form_text)
        return pref.getString(CONSENT_FORM_TEXT_KEY, defaultText)?: defaultText
    }
    @JvmStatic fun getSurveySubmitSuccessToastText(): String {
        val defaultText: String = appContext.getString(R.string.default_survey_submit_success_message)
        return pref.getString(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, defaultText)?: defaultText
    }
    @JvmStatic fun setAboutPageText(text: String) { putCommit(ABOUT_PAGE_TEXT_KEY, text) }
    @JvmStatic fun setCallClinicianButtonText(text: String) { putCommit(CALL_CLINICIAN_BUTTON_TEXT_KEY, text) }
    @JvmStatic fun setConsentFormText(text: String) { putCommit(CONSENT_FORM_TEXT_KEY, text) }
    @JvmStatic fun setSurveySubmitSuccessToastText(text: String) { putCommit(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, text) }

    /*###########################################################################################
    ################################### User Credentials ########################################
    ###########################################################################################*/
    @JvmStatic fun getServerUrl(): String? { return pref.getString(SERVER_URL_KEY, null) }
    @JvmStatic fun setServerUrl(serverUrl: String) {
        if (editor == null) Log.e("LoginManager.java", "editor is null in setServerUrl()")
        putCommit(SERVER_URL_KEY, prependHttpsToServerUrl(serverUrl))
    }

    @JvmStatic fun prependHttpsToServerUrl(serverUrl: String): String {
        return if (serverUrl.startsWith("https://")) {
            serverUrl
        } else if (serverUrl.startsWith("http://")) {
            "https://" + serverUrl.substring(7, serverUrl.length)
        } else {
            "https://$serverUrl"
        }
    }

    @JvmStatic fun setLoginCredentials(userID: String, password: String) {
        if (editor == null) Log.e("LoginManager.java", "editor is null in setLoginCredentials()")
        putCommit(KEY_ID, userID)
        setPassword(password)
    }

    @JvmStatic fun getPassword(): String? { return pref.getString(KEY_PASSWORD, null) }
    @JvmStatic fun getPatientID(): String? { return pref.getString(KEY_ID, NULL_ID) }

    /*###########################################################################################
    ###################################### ERROR INFO ###########################################
    ###########################################################################################*/
    @JvmStatic fun setStudyID(studyID: String) { putCommit(STUDY_ID, studyID) }
    @JvmStatic fun setStudyName(studyName: String) { putCommit(STUDY_NAME, studyName) }
    @JvmStatic fun getStudyID(): String { return pref.getString(STUDY_ID, NULL_ID)?: NULL_ID }
    @JvmStatic fun getStudyName(): String { return pref.getString(STUDY_NAME, NULL_ID)?: NULL_ID }

    /*###########################################################################################
    #################################### Contact Numbers ########################################
    ###########################################################################################*/
    @JvmStatic fun getPrimaryCareNumber(): String { return pref.getString(PCP_PHONE_KEY, "")?: "" }
    @JvmStatic fun setPrimaryCareNumber(phoneNumber: String) { putCommit(PCP_PHONE_KEY, phoneNumber) }
    @JvmStatic fun getPasswordResetNumber(): String { return pref.getString(PASSWORD_RESET_NUMBER_KEY, "")?: "" }
    @JvmStatic fun setPasswordResetNumber(phoneNumber: String) { putCommit(PASSWORD_RESET_NUMBER_KEY, phoneNumber) }

    /*###########################################################################################
    ###################################### Survey Info ##########################################
    ###########################################################################################*/
    @JvmStatic fun getSurveyIds(): List<String> { return JSONUtils.jsonArrayToStringList(getSurveyIdsJsonArray()) }
    @JvmStatic fun getSurveyQuestionMemory(surveyId: String): MutableList<String?> { return JSONUtils.jsonArrayToStringList(getSurveyQuestionMemoryJsonArray(surveyId)) }
    @JvmStatic fun getSurveyTimes(surveyId: String): String? { return pref.getString("$surveyId-times", null) }
    @JvmStatic fun getSurveyContent(surveyId: String): String? { return pref.getString("$surveyId-content", null) }
    @JvmStatic fun getSurveyType(surveyId: String): String? { return pref.getString("$surveyId-type", null) }
    @JvmStatic fun getSurveySettings(surveyId: String): String? { return pref.getString("$surveyId-settings", null) }
    @JvmStatic fun getSurveyName(surveyId: String): String? { return pref.getString("$surveyId-name", null) }

    // getSurveyNotificationState is the closest thing we have for a source of truth for whether the
    // survey has passed through logic where it should be activated. (button and notification))
    // obviously always-available surveys are... alway active, but they may or may not have a notification.
    @JvmStatic fun getSurveyNotificationState(surveyId: String): Boolean { return pref.getBoolean("$surveyId-notificationState", false) }
    @JvmStatic fun getMostRecentSurveyAlarmTime(surveyId: String): Long { return pref.getLong("$surveyId-prior_alarm", 9223372036854775807L) }
    @JvmStatic fun createSurveyData(
            surveyId: String, content: String, timings: String, type: String, settings: String, name: String,
    ) {
        setSurveyContent(surveyId, content)
        setSurveyTimes(surveyId, timings)
        setSurveyType(surveyId, type)
        setSurveySettings(surveyId, settings)
        setSurveyName(surveyId, name)
    }

    // individual setters
    @JvmStatic fun setSurveyContent(surveyId: String, content: String) { putCommit("$surveyId-content", content) }
    @JvmStatic fun setSurveyTimes(surveyId: String, times: String) { putCommit("$surveyId-times", times) }
    @JvmStatic fun setSurveyType(surveyId: String, type: String) { putCommit("$surveyId-type", type) }
    @JvmStatic fun setSurveySettings(surveyId: String, settings: String) { putCommit("$surveyId-settings", settings) }
    @JvmStatic fun setSurveyName(surveyId: String, name: String) { putCommit("$surveyId-name", name) }

    // survey state storage
    @JvmStatic fun setSurveyNotificationState(surveyId: String, bool: Boolean) { putCommit("$surveyId-notificationState", bool) }
    @JvmStatic fun setMostRecentSurveyAlarmTime(surveyId: String, time: Long) { putCommit("$surveyId-prior_alarm", time) }
    @JvmStatic fun deleteSurvey(surveyId: String) {
        editor.remove("$surveyId-content")
        editor.remove("$surveyId-times")
        editor.remove("$surveyId-type")
        editor.remove("$surveyId-notificationState")
        editor.remove("$surveyId-settings")
        editor.remove("$surveyId-name")
        editor.remove("$surveyId-questionIds")
        editor.commit()
        removeSurveyId(surveyId)
    }

    // array style storage and removal for surveyIds and questionIds
    @JvmStatic fun getSurveyIdsJsonArray(): JSONArray {
        val jsonString = pref.getString(SURVEY_IDS, "0")
        // Log.d("persistant data", "getting ids: " + jsonString);
        return if (jsonString === "0") {
            JSONArray()
        } else try {
            JSONArray(jsonString)
        } catch (e: JSONException) {
            throw NullPointerException("getSurveyIds failed, json string was: $jsonString")
        } // return empty if the list is empty
    }
    @JvmStatic fun addSurveyId(surveyId: String) {
        val list = JSONUtils.jsonArrayToStringList(getSurveyIdsJsonArray())
        if (!list.contains(surveyId)) {
            list.add(surveyId)
            putCommit(SURVEY_IDS, JSONArray(list).toString())
        } else {
            // create salt if it does not exist
            throw NullPointerException("duplicate survey id added: $surveyId")
        }
    }
    @JvmStatic fun removeSurveyId(surveyId: String) {
        val list = JSONUtils.jsonArrayToStringList(getSurveyIdsJsonArray())
        if (list.contains(surveyId)) {
            list.remove(surveyId)
            putCommit(SURVEY_IDS, JSONArray(list).toString())
        } else {
            // create salt if it does not exist
            throw NullPointerException("survey id does not exist: $surveyId")
        }
    }
    @JvmStatic fun getSurveyQuestionMemoryJsonArray(surveyId: String): JSONArray {
        val jsonString = pref.getString("$surveyId-questionIds", "0")
        return if (jsonString === "0") {
            JSONArray()
        } else try {
            JSONArray(jsonString)
        } catch (e: JSONException) {
            throw NullPointerException("getSurveyIds failed, json string was: $jsonString")
        } // return empty if the list is empty
    }
    @JvmStatic fun addSurveyQuestionMemory(surveyId: String, questionId: String) {
        val list = getSurveyQuestionMemory(surveyId)
        // Log.d("persistent data", "adding questionId: " + questionId);
        if (!list.contains(questionId)) {
            list.add(questionId)
            putCommit("$surveyId-questionIds", JSONArray(list).toString())
        } else {
            // create salt if it does not exist
            throw NullPointerException("duplicate question id added: $questionId")
        }
    }
    @JvmStatic fun clearSurveyQuestionMemory(surveyId: String) { putCommit("$surveyId-questionIds", JSONArray().toString()) }

    /*###########################################################################################
    ###################################### FUZZY GPS ############################################
    ###########################################################################################*/

    @JvmStatic fun getLatitudeOffset(): Double {
        val latitudeOffset = pref.getFloat(LATITUDE_OFFSET_KEY, 0.0f)
        // create latitude offset if it does not exist
        if (latitudeOffset == 0.0f && getUseGpsFuzzing()) {
            val newLongitudeOffset = generate_lattitude_offset()
            putCommit(LATITUDE_OFFSET_KEY, newLongitudeOffset)
            return newLongitudeOffset.toDouble()
        } else {
            return latitudeOffset.toDouble()
        }
    }

    // original comment: create random latitude offset between (-1, -.2) or (.2, 1)
    @JvmStatic fun generate_lattitude_offset (): Float {
        // 1.6 * (0 to 1) = 0 to 1.6, +0.2 gives a range of 0.2 to 1.8.
        var newLatitudeOffset = (.2 + Math.random() * 1.6).toFloat()
        // if it's greater than 1, subtract 0.8 (range 0.2 to 1), and then change sign
        if (newLatitudeOffset > 1) {
            newLatitudeOffset = (newLatitudeOffset - .8f) * -1
        }
        return newLatitudeOffset
    }

    @JvmStatic fun getLongitudeOffset(): Float {
        val longitudeOffset: Float = pref.getFloat(LONGITUDE_OFFSET_KEY, 0.0f)
        // create longitude offset if it does not exist
        if (longitudeOffset == 0.0f && getUseGpsFuzzing()) {
            val newLongitudeOffset: Float = generate_longitude_offset()
            putCommit(LONGITUDE_OFFSET_KEY, newLongitudeOffset)
            return newLongitudeOffset
        } else {
            return longitudeOffset
        }
    }

    // original comment: create random longitude offset between (10, 180), or (-180, -10) or
    @JvmStatic fun generate_longitude_offset() : Float {
        // math.random returns a number between 0 and 1, the starting value is between 10 and 350.
        // if it's between 180 and 350 subtract 170 - output is between 10 and 170
        // then multiply by -1 to make it negative. so it is between -10 and -170.
        var newLongitudeOffset = (10 + Math.random() * 340).toFloat()
        if (newLongitudeOffset > 180) {
            newLongitudeOffset = (newLongitudeOffset - 170) * -1
        }
        return newLongitudeOffset
    }

    @JvmStatic fun setUseGpsFuzzing(useFuzzyGps: Boolean): Boolean { return putCommit(USE_GPS_FUZZING_KEY, useFuzzyGps) }
    @JvmStatic fun getUseGpsFuzzing(): Boolean { return pref.getBoolean(USE_GPS_FUZZING_KEY, false) }
    @JvmStatic fun setUseAnonymizedHashing(useAnonymizedHashing: Boolean): Boolean { return putCommit(EncryptionEngine.USE_ANONYMIZED_HASHING_KEY, useAnonymizedHashing) }
    @JvmStatic fun getUseAnonymizedHashing(): Boolean {
        // If not present, default to safe hashing
        return pref.getBoolean(EncryptionEngine.USE_ANONYMIZED_HASHING_KEY, true)
    }

    /*###########################################################################################
    ###################################### Call Buttons #########################################
    ###########################################################################################*/
    @JvmStatic fun getCallClinicianButtonEnabled(): Boolean { return pref.getBoolean(CALL_CLINICIAN_BUTTON_ENABLED_KEY, false) }
    @JvmStatic fun setCallClinicianButtonEnabled(enabled: Boolean) { putCommit(CALL_CLINICIAN_BUTTON_ENABLED_KEY, enabled) }
    @JvmStatic fun getCallResearchAssistantButtonEnabled(): Boolean { return pref.getBoolean(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, false) }
    @JvmStatic fun setCallResearchAssistantButtonEnabled(enabled: Boolean) { putCommit(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, enabled) }
}
package org.beiwe.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import org.beiwe.app.networking.NetworkUtility
import org.beiwe.app.storage.PersistentData
import org.json.JSONException
import org.json.JSONObject
import java.util.*

object PermissionHandler {
    const val PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED

    @JvmField
    var POWER_EXCEPTION_PERMISSION = "POWER_EXCEPTION_PERMISSION"

    @JvmField
    var permissionMessages: MutableMap<String, Int> = HashMap()

    init {
        permissionMessages[Manifest.permission.ACCESS_FINE_LOCATION] = R.string.permission_access_fine_location
        permissionMessages[Manifest.permission.ACCESS_NETWORK_STATE] = R.string.permission_access_network_state
        permissionMessages[Manifest.permission.ACCESS_WIFI_STATE] = R.string.permission_access_wifi_state
        permissionMessages[Manifest.permission.READ_SMS] = R.string.permission_read_sms
        permissionMessages[Manifest.permission.BLUETOOTH] = R.string.permission_bluetooth
        permissionMessages[Manifest.permission.BLUETOOTH_ADMIN] = R.string.permission_bluetooth_admin
        permissionMessages[Manifest.permission.CALL_PHONE] = R.string.permission_call_phone
        permissionMessages[Manifest.permission.INTERNET] = R.string.permission_internet
        permissionMessages[Manifest.permission.READ_CALL_LOG] = R.string.permission_read_call_log
        permissionMessages[Manifest.permission.READ_CONTACTS] = R.string.permission_read_contacts
        permissionMessages[Manifest.permission.READ_PHONE_STATE] = R.string.permission_read_phone_state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            permissionMessages[Manifest.permission.READ_PHONE_NUMBERS] = R.string.permission_read_phone_state
        permissionMessages[Manifest.permission.RECEIVE_BOOT_COMPLETED] = R.string.permission_receive_boot_completed
        permissionMessages[Manifest.permission.RECORD_AUDIO] = R.string.permission_record_audio
        permissionMessages[Manifest.permission.ACCESS_COARSE_LOCATION] = R.string.permission_access_coarse_location
        permissionMessages[Manifest.permission.RECEIVE_MMS] = R.string.permission_receive_mms
        permissionMessages[Manifest.permission.RECEIVE_SMS] = R.string.permission_receive_sms

        // access background location and post notifications are only available on newer versions of
        // android. Check must be identical to the one in their respective check function or the app will crash.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissionMessages[Manifest.permission.ACCESS_BACKGROUND_LOCATION] = R.string.permission_access_background_location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissionMessages[Manifest.permission.POST_NOTIFICATIONS] = R.string.permission_notifications

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionMessages[Manifest.permission.BLUETOOTH_CONNECT] = R.string.permission_bluetooth
            permissionMessages[Manifest.permission.BLUETOOTH_SCAN] = R.string.permission_bluetooth_admin
        }
        permissionMessages = Collections.unmodifiableMap(permissionMessages)
    }

    @JvmStatic
    fun getNormalPermissionMessage(permission: String, appContext: Context): String {
        return String.format(appContext.getString(R.string.permission_normal_request_template),
            appContext.getString(permissionMessages[permission]!!))
    }

    @JvmStatic
    fun getBumpingPermissionMessage(permission: String, appContext: Context): String {
        return String.format(appContext.getString(R.string.permission_bumping_request_template),
            appContext.getString(permissionMessages[permission]!!))
    }

    /* The following are enabled by default.
	 *  AccessNetworkState
	 *  AccessWifiState
	 *  Bluetooth
	 *  BluetoothAdmin
	 *  Internet
	 *  ReceiveBootCompleted
	 *
	 * the following are enabled on the registration screen
	 *  ReadSms
	 *  ReceiveMms
	 *  ReceiveSms
	 *
	 * This leaves the following to be prompted for at session activity start
	 *  AccessFineLocation - GPS
	 *  CallPhone - Calls
	 *  ReadCallLog - Calls
	 *  ReadContacts - Calls and SMS
	 *  ReadPhoneState - Calls
	 *  WriteCallLog - Calls
	 *  Bluetooth Scan and Connect - Android 12+
	 *  PostNotifications - Android 12+ (10+?)
	 *  Background Location - Android 12+
	 *
	 * We check for microphone recording as a special condition on the audio recording screen,
	 *   or when background audio is enabled */

    /* Simple permission checks */

    @JvmStatic
    fun checkAccessCoarseLocation(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkAccessFineLocation(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
    }

    @JvmStatic
    fun checkAccessBackgroundLocation(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED
        else
            return true
    }

    fun checkAccessNetworkState(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PERMISSION_GRANTED
    }

    fun checkAccessWifiState(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PERMISSION_GRANTED
    }

    fun checkAccessBluetooth(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PERMISSION_GRANTED
    }

    fun checkAccessBluetoothAdmin(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PERMISSION_GRANTED
    }

    fun checkAccessBluetoothConnect(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED
    }

    fun checkAccessBluetoothScan(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PERMISSION_GRANTED
    }

    fun checkAccessReadCallLog(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PERMISSION_GRANTED
    }

    fun checkAccessReadContacts(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PERMISSION_GRANTED
    }

    fun checkAccessReadPhoneState(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PERMISSION_GRANTED
    }

    fun checkAccessReadPhoneNumbers(context: Context): Boolean {
        // read phone numbers got separated into its own thing in 0 (11)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PERMISSION_GRANTED
        else
            return checkAccessReadPhoneState(context)
    }

    @JvmStatic
    fun checkAccessReadSms(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_SMS) == PERMISSION_GRANTED
    }

    fun checkAccessReceiveMms(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECEIVE_MMS) == PERMISSION_GRANTED
    }

    fun checkAccessReceiveSms(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PERMISSION_GRANTED
    }

    fun checkAccessRecordAudio(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED
    }

    fun checkAccessNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PERMISSION_GRANTED
        else
            true
    }

    fun checkCanMakePhoneCall(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PERMISSION_GRANTED
    }

    /* Complex permission checks */

    @JvmStatic
    fun checkGpsPermissions(context: Context): Boolean {
        return checkAccessFineLocation(context) // we don't want coarse, fine means GPS.
    }

    fun checkCallLoggingPermissions(context: Context): Boolean {
        return checkAccessReadPhoneState(context) && checkAccessReadCallLog(context)
    }

    fun checkTextsPermissions(context: Context): Boolean {
        return checkAccessReadContacts(context) && checkAccessReadSms(context) && checkAccessReceiveMms(context) && checkAccessReceiveSms(context)
    }

    @JvmStatic
    fun checkWifiPermissions(context: Context): Boolean {
        return checkAccessWifiState(context) && checkAccessNetworkState(context)
    }

    @JvmStatic
    fun checkBluetoothPermissions(context: Context): Boolean {
        // android versions below 12 use permission.BLUETOOTH and permission.BLUETOOTH_ADMIN,
        // 12+ uses permission.BLUETOOTH_CONNECT and permission.BLUETOOTH_SCAN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkAccessBluetoothConnect(context) && checkAccessBluetoothScan(context)
        }
        return checkAccessBluetooth(context) && checkAccessBluetoothAdmin(context)
    }

    @JvmStatic
    fun confirmGps(context: Context): Boolean {
        return PersistentData.getGpsEnabled() && checkGpsPermissions(context)
    }

    @JvmStatic
    fun confirmCallLogging(context: Context): Boolean {
        return PersistentData.getCallLoggingEnabled() && checkCallLoggingPermissions(context)
    }

    @JvmStatic
    fun confirmTexts(context: Context): Boolean {
        return PersistentData.getTextsEnabled() && checkTextsPermissions(context)
    }

    @JvmStatic
    fun confirmWifi(context: Context): Boolean {
        return PersistentData.getWifiEnabled() && checkWifiPermissions(context) && checkAccessFineLocation(context) && checkAccessCoarseLocation(context)
    }

    @JvmStatic
    fun confirmBluetooth(context: Context): Boolean {
        return PersistentData.getBluetoothEnabled() && checkBluetoothPermissions(context)
    }

    @JvmStatic
    fun confirmAmbientAudioCollection(context: Context): Boolean {
        return PersistentData.getAmbientAudioEnabled() && checkAccessRecordAudio(context)
    }

    @JvmStatic
    fun getNextPermission(context: Context, includeRecording: Boolean): String? {
        // os version check handled inside checkAccessNotifications
        if (!checkAccessNotifications(context)) return Manifest.permission.POST_NOTIFICATIONS

        if (PersistentData.getGpsEnabled()) {
            if (!checkAccessFineLocation(context)) return Manifest.permission.ACCESS_FINE_LOCATION
            // os version check handled inside checkAccessBackgroundLocation
            if (!checkAccessBackgroundLocation(context)) return Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
        if (PersistentData.getWifiEnabled()) {
            if (!checkAccessWifiState(context)) return Manifest.permission.ACCESS_WIFI_STATE
            if (!checkAccessNetworkState(context)) return Manifest.permission.ACCESS_NETWORK_STATE
            if (!checkAccessCoarseLocation(context)) return Manifest.permission.ACCESS_COARSE_LOCATION
            if (!checkAccessFineLocation(context)) return Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (PersistentData.getBluetoothEnabled()) {
            // android versions below 12 use permission.BLUETOOTH and permission.BLUETOOTH_ADMIN,
            // 12+ uses permission.BLUETOOTH_CONNECT and permission.BLUETOOTH_SCAN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!checkAccessBluetoothConnect(context)) return Manifest.permission.BLUETOOTH_CONNECT
                if (!checkAccessBluetoothScan(context)) return Manifest.permission.BLUETOOTH_SCAN
            } else {
                if (!checkAccessBluetooth(context)) return Manifest.permission.BLUETOOTH
                if (!checkAccessBluetoothAdmin(context)) return Manifest.permission.BLUETOOTH_ADMIN
            }
        }

        // read phone numbers was introduced in O, only check it if on O or higher, otherwise its read_phone_state
        if (PersistentData.getCallLoggingEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!checkAccessReadPhoneNumbers(context)) return Manifest.permission.READ_PHONE_NUMBERS
        }

        if (PersistentData.getCallLoggingEnabled()) {
            if (!checkAccessReadPhoneState(context)) return Manifest.permission.READ_PHONE_STATE
            if (!checkAccessReadCallLog(context)) return Manifest.permission.READ_CALL_LOG
        }
        if (PersistentData.getTextsEnabled()) {
            if (!checkAccessReadContacts(context)) return Manifest.permission.READ_CONTACTS
            if (!checkAccessReadSms(context)) return Manifest.permission.READ_SMS
            if (!checkAccessReceiveMms(context)) return Manifest.permission.RECEIVE_MMS
            if (!checkAccessReceiveSms(context)) return Manifest.permission.RECEIVE_SMS
        }
        if (includeRecording || PersistentData.getAmbientAudioEnabled()) {
            if (!checkAccessRecordAudio(context)) return Manifest.permission.RECORD_AUDIO
        }

        // The phone call permission is dependent on the presence of either phone number being present
        if ((PersistentData.getCallClinicianButtonEnabled() || PersistentData.getCallResearchAssistantButtonEnabled())
                && !checkCanMakePhoneCall(context)) {
            return Manifest.permission.CALL_PHONE
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            return POWER_EXCEPTION_PERMISSION
        }

        return null
    }

    /* Permission change detection for the app log */

    /** Location permission as a single level string, roughly comparable to the iOS authorization
     * levels: Denied, CoarseWhenInUse, CoarseAlways, FineWhenInUse, FineAlways. "Always" means
     * background location is also granted (below Android 10 there is no separate background
     * permission, so any grant reports as Always). */
    fun getLocationPermissionLevel(context: Context): String {
        val precision = when {
            checkAccessFineLocation(context) -> "Fine"
            checkAccessCoarseLocation(context) -> "Coarse"
            else -> return "Denied"
        }
        return precision + (if (checkAccessBackgroundLocation(context)) "Always" else "WhenInUse")
    }

    /** Notifications are "Enabled" only if the participant has not blocked them in system settings.
     * areNotificationsEnabled covers every supported OS version (on 13+ it also reflects the
     * POST_NOTIFICATIONS runtime permission), which checkSelfPermission alone does not. */
    fun getNotificationPermissionState(context: Context): String {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (notificationManager.areNotificationsEnabled()) "Enabled" else "Disabled"
    }

    fun getBatteryOptimizationState(context: Context): String {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (pm.isIgnoringBatteryOptimizations(context.packageName)) "Exempt" else "NotExempt"
    }

    /** Android has no callback for permission changes made while the app is in the background, so
     * this is a check-and-diff: it reads the current state of each permission/setting Beiwe depends
     * on, compares it against the last state stored in PersistentData, updates the stored state,
     * and returns one "permission_changed: ..." app log statement per changed item.  The caller is
     * responsible for writing the statements to the app log.  On the very first run the previous
     * state is reported as "unknown", which doubles as a record of the initial state. */
    @JvmStatic
    fun checkForPermissionChanges(context: Context): List<String> {
        val statements = ArrayList<String>()

        val location = getLocationPermissionLevel(context)
        val previousLocation = PersistentData.getLastKnownLocationPermissionState()
        if (location != previousLocation) {
            statements.add("permission_changed: location " + previousLocation.ifEmpty { "unknown" } + " -> " + location)
            PersistentData.setLastKnownLocationPermissionState(location)
        }

        val notifications = getNotificationPermissionState(context)
        val previousNotifications = PersistentData.getLastKnownNotificationPermissionState()
        if (notifications != previousNotifications) {
            statements.add("permission_changed: notifications " + previousNotifications.ifEmpty { "unknown" } + " -> " + notifications)
            PersistentData.setLastKnownNotificationPermissionState(notifications)
        }

        val battery = getBatteryOptimizationState(context)
        val previousBattery = PersistentData.getLastKnownBatteryOptimizationState()
        if (battery != previousBattery) {
            statements.add("permission_changed: battery_optimization " + previousBattery.ifEmpty { "unknown" } + " -> " + battery)
            PersistentData.setLastKnownBatteryOptimizationState(battery)
        }

        return statements
    }

    // https://stackoverflow.com/questions/65479363/android-adaptive-battery-setting-detection
    fun isAdaptiveBatteryEnabled(ctx: Context): Boolean {
        val intValue = Settings.Global.getInt(
            ctx.contentResolver,
            "adaptive_battery_management_enabled",
            -1
        )
        return intValue == 1
    }

    @JvmStatic
    fun getDeviceStatusReport(context: Context): String {
        return _getDeviceStatusReport(context)
    }

    // This function is probably sitting in the wrong file, but its so broad, and we need to have a
    // it as a json object for serialization, that there isn't really anywhere good to stick it.

    @JvmStatic
    @Throws(JSONException::class)    // its almost impossible to use the IDE when EVERY LINE has a warning - these can't fail.
    fun _getDeviceStatusReport(context: Context): String {
        val permissions = JSONObject()

        // get the time, convert to calendar object, get local time, insert timezone.
        val time_locale = Date(System.currentTimeMillis()).toLocaleString()

        permissions.put("time_locale", time_locale + " " + DeviceInfo.timeZoneInfo())
        permissions.put("is_registered", PersistentData.getIsRegistered())
        permissions.put("can_upload", NetworkUtility.canUpload(context))
        permissions.put("allow_upload_over_cellular", PersistentData.getAllowUploadOverCellularData())

        // a bunch of app state reporting
        permissions.put("most_recent_activity_OnCreate", PersistentData.appOnCreateActivity)
        permissions.put("most_recent_activity_OnResume", PersistentData.appOnResumeActivity)
        permissions.put("most_recent_activity_OnPause", PersistentData.appOnPauseActivity)
        permissions.put("most_recent_activity_OnServiceBound", PersistentData.appOnServiceBoundActivity)
        permissions.put("most_recent_activity_OnServiceUnBound", PersistentData.appOnServiceUnboundActivity)
        permissions.put("most_recent_service_start", PersistentData.appOnServiceStart)
        permissions.put("most_recent_service_start_first_run", PersistentData.appOnServiceStartFirstRun)
        permissions.put("most_recent_run_all_logic", PersistentData.appOnRunAllLogic)
        permissions.put("most_recent_service_start_command", PersistentData.serviceStartCommand)
        permissions.put("most_recent_service_on_unbind", PersistentData.serviceOnUnbind)
        permissions.put("most_recent_service_on_destroy", PersistentData.serviceOnDestroy)
        permissions.put("most_recent_service_on_low_memory", PersistentData.serviceOnLowMemory)
        permissions.put("most_recent_service_on_trim_memory", PersistentData.serviceOnTrimMemory)
        permissions.put("most_recent_service_on_task_removed", PersistentData.serviceOnTaskRemoved)
        permissions.put("most_recent_accelerometer_start", PersistentData.accelerometerStart)
        permissions.put("most_recent_accelerometer_stop", PersistentData.accelerometerStop)
        permissions.put("most_recent_ambient_audio_start", PersistentData.ambientAudioStart)
        permissions.put("most_recent_ambient_audio_stop", PersistentData.ambientAudioStop)
        permissions.put("most_recent_bluetooth_start", PersistentData.bluetoothStart)
        permissions.put("most_recent_bluetooth_stop", PersistentData.bluetoothStop)
        permissions.put("most_recent_gps_start", PersistentData.gpsStart)
        permissions.put("most_recent_gps_stop", PersistentData.gpsStop)
        permissions.put("most_recent_gyroscope_start", PersistentData.gyroscopeStart)
        permissions.put("most_recent_gyroscope_stop", PersistentData.gyroscopeStop)
        permissions.put("most_recent_upload_attempt", PersistentData.appUploadAttempt)
        permissions.put("most_recent_upload_start", PersistentData.appUploadStart)

        // the normal permissions
        permissions.put("permission_access_background_location", checkAccessBackgroundLocation(context))
        permissions.put("permission_access_coarse_location", checkAccessCoarseLocation(context))
        permissions.put("permission_access_fine_location", checkAccessFineLocation(context))
        permissions.put("permission_access_network_state", checkAccessNetworkState(context))
        permissions.put("permission_access_wifi_state", checkAccessWifiState(context))
        permissions.put("permission_bluetooth", checkAccessBluetooth(context))
        permissions.put("permission_bluetooth_admin", checkAccessBluetoothAdmin(context))
        permissions.put("permission_bluetooth_connect", checkAccessBluetoothConnect(context))
        permissions.put("permission_bluetooth_scan", checkAccessBluetoothScan(context))
        permissions.put("permission_call_phone", checkCanMakePhoneCall(context))
        permissions.put("permission_post_notifications", checkAccessNotifications(context))
        permissions.put("permission_read_call_log", checkAccessReadCallLog(context))
        permissions.put("permission_read_contacts", checkAccessReadContacts(context))
        permissions.put("permission_read_phone_state (receive_boot_completed?)", checkAccessReadPhoneState(context))
        permissions.put("permission_read_phone_numbers", checkAccessReadPhoneNumbers(context))
        permissions.put("permission_read_sms", checkAccessReadSms(context))
        permissions.put("permission_receive_boot_completed", checkAccessReadPhoneState(context))
        permissions.put("permission_receive_mms", checkAccessReceiveMms(context))
        permissions.put("permission_receive_sms", checkAccessReceiveSms(context))
        permissions.put("permission_record_audio", checkAccessRecordAudio(context))

        // properties of the powermanager do not always exist on older android versions (8 at least)
        // so we need to wrap all of these

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        // get the names of all the methods and attributes on the power manager - whole process with
        // a list is basically same speed as a set, up to 30ms on slow devices is fine.
        val names = List<String>(pm.javaClass.methods.size) { pm.javaClass.methods[it].name }

        // All properties here are known to exist on a Pixel 6 running Android 13
        // The marked items were missing on a Nexus 7 2013 tablet running Android 8.
        permissions.put( // missing Android 8
            "power_battery_discharge_prediction",
            if (names.contains("getBatteryDischargePrediction")) pm.batteryDischargePrediction else "missing")
        permissions.put( // missing Android 8
            "power_current_thermal_status",
            if (names.contains("getCurrentThermalStatus")) pm.currentThermalStatus else "missing"
        )
        permissions.put( // missing Android 8
            "power_is_battery_discharge_prediction_personalized",
            if (names.contains("isBatteryDischargePredictionPersonalized")) pm.isBatteryDischargePredictionPersonalized else "missing"
        )
        permissions.put(
            "power_is_device_idle_mode",
            if (names.contains("isDeviceIdleMode")) pm.isDeviceIdleMode else "missing"
        )
        permissions.put(
            "power_is_interactive",
            if (names.contains("isInteractive")) pm.isInteractive else "missing"
        )
        permissions.put(
            "power_is_power_save_mode",
            if (names.contains("isPowerSaveMode")) pm.isPowerSaveMode else "missing")
        permissions.put(
            "power_is_sustained_performance_mode_supported",
            if (names.contains("isSustainedPerformanceModeSupported")) pm.isSustainedPerformanceModeSupported else "missing"
        )
        permissions.put( // missing android 8
            "power_location_power_save_mode",
            if (names.contains("getLocationPowerSaveMode")) pm.locationPowerSaveMode else "missing"
        )
        permissions.put(
            "power_is_ignoring_battery_optimizations",
            if (names.contains("isIgnoringBatteryOptimizations")) pm.isIgnoringBatteryOptimizations(context.packageName) else "missing"
        )

        // not a power manager property
        permissions.put("power_is_adaptive_battery_management_enabled", isAdaptiveBatteryEnabled(context))

        // storage details
        permissions.put("storage_free_space", DeviceInfo.freeSpace())
        return permissions.toString()
    }
}
package org.beiwe.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.beiwe.app.storage.PersistentData
import java.util.*


/** In API 19 and above, alarms are inexact (to save power).  In API 18 and
 * below, alarms are exact.
 * This function checks the phone's operating system's API version and
 * returns TRUE if alarms are exact in this version (i.e., if it's API 18
 * or below), and returns FALSE if alarms are inexact.   */
fun alarmsAreExactInThisApiVersion(): Boolean {
	return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
}


/** The Timer class provides a meeans of setting various timers.  These are used by the BackgroundService
 * for devices that must be turned on/off, and timing the user to automatically logout after a period of time.
 * This class includes all the Intents and IntentFilters we for triggered broadcasts.  */
class Timer(mainService: MainService) {
	companion object {
		// Control Message Intents
		lateinit var accelerometerOffIntent: Intent
		lateinit var accelerometerOnIntent: Intent
		lateinit var gyroscopeOffIntent: Intent
		lateinit var gyroscopeOnIntent: Intent
		lateinit var bluetoothOffIntent: Intent
		lateinit var bluetoothOnIntent: Intent

		lateinit var gpsOffIntent: Intent
		lateinit var gpsOnIntent: Intent
		lateinit var signoutIntent: Intent

		lateinit var wifiLogIntent: Intent
		lateinit var uploadDatafilesIntent: Intent
		lateinit var createNewDataFilesIntent: Intent
		lateinit var checkForNewSurveysIntent: Intent
		lateinit var checkForSMSEnabledIntent: Intent
		lateinit var checkForCallsEnabledIntent: Intent
		lateinit var sendCurrentFCMTokenIntent: Intent
		lateinit var checkForNewDeviceSettingsIntent: Intent
		// a MutableMap<String, Intent> for intent lookup.
		val intent_map = mutableMapOf<String, Intent>()

		/** Snippet for our common intent setup pattern, includes adding the intents to our intent map */
		fun setupIntent(action: String): Intent {
			val newIntent = Intent()
			newIntent.action = action
			intent_map[action] = newIntent
			return newIntent
		}
	}

	private val alarmManager: AlarmManager
	private val appContext: Context

	// Constructor
	init {
		appContext = mainService.applicationContext
		alarmManager = mainService.getSystemService(Context.ALARM_SERVICE) as AlarmManager

		// double alarm intents
		accelerometerOffIntent = setupIntent(appContext.getString(R.string.turn_accelerometer_off))
		accelerometerOnIntent = setupIntent(appContext.getString(R.string.turn_accelerometer_on))
		gyroscopeOffIntent = setupIntent(appContext.getString(R.string.turn_gyroscope_off))
		gyroscopeOnIntent = setupIntent(appContext.getString(R.string.turn_gyroscope_on))
		bluetoothOffIntent = setupIntent(appContext.getString(R.string.turn_bluetooth_off))
		bluetoothOnIntent = setupIntent(appContext.getString(R.string.turn_bluetooth_on))
		gpsOffIntent = setupIntent(appContext.getString(R.string.turn_gps_off))
		gpsOnIntent = setupIntent(appContext.getString(R.string.turn_gps_on))

		// Set up event triggering alarm intents
		signoutIntent = setupIntent(appContext.getString(R.string.signout_intent))
		wifiLogIntent = setupIntent(appContext.getString(R.string.run_wifi_log))
		uploadDatafilesIntent = setupIntent(appContext.getString(R.string.upload_data_files_intent))
		createNewDataFilesIntent = setupIntent(appContext.getString(R.string.create_new_data_files_intent))
		checkForNewSurveysIntent = setupIntent(appContext.getString(R.string.check_for_new_surveys_intent))
		checkForSMSEnabledIntent = setupIntent(appContext.getString(R.string.check_for_sms_enabled))
		checkForCallsEnabledIntent = setupIntent(appContext.getString(R.string.check_for_call_log_enabled))
		sendCurrentFCMTokenIntent = setupIntent(appContext.getString(R.string.fcm_upload))
		checkForNewDeviceSettingsIntent = setupIntent(appContext.getString(R.string.check_for_new_device_settings_intent))
	}

	/* ###############################################################################################
	 * ############################ The Various Types of Alarms Creation #############################
	 * #############################################################################################*/

	/** Single exact alarm for an event that happens once.
	 * @return a long of the system time in milliseconds that the alarm was set for. */
	fun setupExactSingleAlarm(milliseconds: Long, intentToBeBroadcast: Intent): Long {
		val triggerTime = System.currentTimeMillis() + milliseconds

		val flags = pending_intent_flag_fix(0) // no flags
		val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToBeBroadcast, flags)
		setExactAlarm(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
		return triggerTime
	}

	fun setupSingleAlarmAt(triggerTime: Long, intentToBeBroadcast: Intent): Long {
		val flags = pending_intent_flag_fix(0) // no flags
		val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToBeBroadcast, flags)
		setExactAlarm(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
		return triggerTime
	}

	/** setupExactTimeAlarm creates an Exact Alarm that will go off at a specific time within a
	 * period, e.g. every hour (period), at 47 minutes past the hour (start time within period).
	 * setupExactTimeAlarm is used for the Bluetooth timer, so that every device that has this app
	 * turns on its Bluetooth at the same moment.  */
	fun setupExactSingleAbsoluteTimeAlarm(period: Long, startTimeInPeriod: Long, intentToBeBroadcast: Intent) {
		val currentTime = System.currentTimeMillis()
		// current unix time mod (example) 3,600,000 milliseconds = the next hour-boundry
		var nextTriggerTime = currentTime - currentTime % period + startTimeInPeriod
		if (nextTriggerTime < currentTime)
			nextTriggerTime += period

		val flags = pending_intent_flag_fix(0) // no flags
		val pendingTimerIntent = PendingIntent.getBroadcast(appContext, 0, intentToBeBroadcast, flags)
		setExactAlarm(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingTimerIntent)
	}

	fun startSurveyAlarm(surveyId: String, alarmTime: Calendar) {
		val intentToBeBroadcast = Intent(surveyId)
		// Log.d("timer", "action: " + intentToBeBroadcast.getAction() );
		setupSurveyAlarm(surveyId, intentToBeBroadcast, alarmTime)
	}

	/**Takes a specially prepared intent and sets it to go off at the day and time provided, also
	 * updates PersistentData with that timestamp the alarm was set for.
	 * @param intentToBeBroadcast an intent that has been prepared by the startWeeklyAlarm function. */
	private fun setupSurveyAlarm(surveyId: String, intentToBeBroadcast: Intent, alarmTime: Calendar) {
		val flags = pending_intent_flag_fix(0) // no flags
		val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToBeBroadcast, flags)
		setExactAlarm(AlarmManager.RTC_WAKEUP, alarmTime.timeInMillis, pendingIntent)
		PersistentData.setMostRecentSurveyAlarmTime(surveyId, alarmTime.timeInMillis)
	}

	/** Calls AlarmManager.set() for API < 19, and AlarmManager.setExact() for API 19+
	 * For an exact alarm, it seems you need to use .set() for API 18 and below, and
	 * .setExact() for API 19 (KitKat) and above.  Android 12 (api 31) also requires a new
	 * setExact() permission. */
	private fun setExactAlarm(alarm_type: Int, triggerAtMillis: Long, operation: PendingIntent) {
		if (alarmsAreExactInThisApiVersion())
			alarmManager.set(alarm_type, triggerAtMillis, operation)
		else
			alarmManager.setExact(alarm_type, triggerAtMillis, operation)
	}

	/* ##################################################################################
	* ############################ Other Utility Functions #############################
	* ################################################################################*/

	/**Cancels an alarm, does not return any info about whether the alarm existed.
	 * @param intentToBeBroadcast an Intent identifying the alarm to cancel. */
	fun cancelAlarm(intentToBeBroadcast: Intent) {
		val flags = pending_intent_flag_fix(0) // no flags
		val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intentToBeBroadcast, flags)
		alarmManager.cancel(pendingIntent)
	}

	/**Checks if an alarm is set.
	 * @param intent an Intent identifying the alarm to check.
	 * @return Returns TRUE if there is an alarm set matching that intent; otherwise false. */
	fun alarmIsSet(intent: Intent): Boolean {
		val flags = pending_intent_flag_fix(PendingIntent.FLAG_NO_CREATE)
		return PendingIntent.getBroadcast(appContext, 0, intent, flags) != null
	}
}

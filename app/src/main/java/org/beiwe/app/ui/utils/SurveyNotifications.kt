package org.beiwe.app.ui.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import org.beiwe.app.JSONUtils
import org.beiwe.app.R
import org.beiwe.app.is_nightmode
import org.beiwe.app.pending_intent_flag_fix
import org.beiwe.app.printe
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import org.beiwe.app.survey.AudioRecorderActivity
import org.beiwe.app.survey.AudioRecorderEnhancedActivity
import org.beiwe.app.survey.SurveyActivity
import org.beiwe.app.ui.user.MainMenuActivity
import org.json.JSONException
import org.json.JSONObject


val BEIWE_VIBRATION_PATTERN = longArrayOf(0, 1000, 500, 1000)


/* Behavior of message notifications:
* - Notification is not received if the app is force-stopped, but it will be received at some point
*     after the app is reopened.
* - If the app is paused the notification is received, but delayed until the app is unpaused.
* - Message is pulled down and made visible if the app is in the foreground.
* - Message is ... a normal notification when the app is in the background.
* - Notifications with the same message in them are replaced by the new notification.
* - notifications with different messages are displayed as separate notifications.
* - Notifications do not auto-clear when you open the app. */
object MessageNotification {
	private const val CHANNEL_ID = "messages_notification_channel"

	fun showNotificationMessage(appContext: Context, message: String) {
		// call the appropriate app version.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ensureMessagesNotificationChannelExists(appContext)
			displayMessageNotificationNew(appContext, message)
		} else
			displayMessageNotificationOld(appContext, message)
	}

	// Apps targeting api 26 or later need a notification channel to display notifications
	private fun ensureMessagesNotificationChannelExists(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // redundant check to suppress error
			// if it already exists return early
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			if (notificationManager.getNotificationChannel(MessageNotification.CHANNEL_ID) != null)
				return

			// Originally copied these from an example, these values could change but its fine?
			val notificationChannel = NotificationChannel(CHANNEL_ID, "Beiwe Messages", NotificationManager.IMPORTANCE_HIGH)
			notificationChannel.description = "The Beiwe App Special Notifications"
			notificationChannel.enableLights(true)
			notificationChannel.lightColor = Color.YELLOW
			notificationChannel.vibrationPattern = BEIWE_VIBRATION_PATTERN
			notificationChannel.enableVibration(true)
			notificationManager.createNotificationChannel(notificationChannel)
		}
	}

	private fun displayMessageNotificationNew(appContext: Context, message: String) {
		//TODO: virtually only this first line that differs, the type is different but almost everything else is the same. find a way to refactor.
		val notificationBuilder = Notification.Builder(appContext, MessageNotification.CHANNEL_ID)
		notificationBuilder.setContentTitle(appContext.getString(R.string.survey_notification_app_name))
		notificationBuilder.setShowWhen(true) // As of API 24 this no longer defaults to true and must be set explicitly
		notificationBuilder.setTicker(message)
		notificationBuilder.setContentText(message)
		notificationBuilder.setSmallIcon(getExclamationMarkIcon(appContext))
		notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, getExclamationMarkIcon(appContext)))
		notificationBuilder.setGroup("messages")
		notificationBuilder.setContentIntent(thePendingIntent(appContext, message))
		notificationBuilder.setContentIntent(thePendingIntent(appContext, message))
		val messageNotification = notificationBuilder.build()
		val requestCode = message.hashCode()
		doNotification(appContext, requestCode, messageNotification)
	}

	private fun displayMessageNotificationOld(appContext: Context, message: String) {
		//TODO: virtually only this first line that differs, the type is different but almost everything else is the same. find a way to refactor.
		val notificationBuilder = NotificationCompat.Builder(appContext)
		notificationBuilder.setContentTitle(appContext.getString(R.string.survey_notification_app_name))
		notificationBuilder.setShowWhen(true) // As of API 24 this no longer defaults to true and must be set explicitly
		notificationBuilder.setTicker(message)
		notificationBuilder.setContentText(message)
		notificationBuilder.setSmallIcon(getExclamationMarkIcon(appContext))
		notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, getExclamationMarkIcon(appContext)))
		notificationBuilder.setGroup("messages")
		notificationBuilder.setContentIntent(thePendingIntent(appContext, message))
		val messageNotification = notificationBuilder.build()
		val requestCode = message.hashCode()
		doNotification(appContext, requestCode, messageNotification)
	}

	// does the actual call to pop up a notification
	fun doNotification(appContext: Context, requestCode: Int, messageNotification: Notification) {
		// this is the notification type we always want
		messageNotification.flags = Notification.FLAG_ONGOING_EVENT
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(requestCode)
		notificationManager.notify(requestCode, messageNotification)

		if (!notificationManager.areNotificationsEnabled()) {
			TextFileManager.writeDebugLogStatement("Participant has blocked notifications (3)")
			Log.e("SurveyNotifications", "Participant has blocked notifications (3)")
		}
	}

	// common code for setting up a pending intent that opens the app to the main menu page
	fun thePendingIntent(appContext: Context, message: String): PendingIntent {
		// naming everything to keep it clear
		val activityIntent = mainActivityIntent(appContext, message)
		// should do nothing when the participant is in the app....
		val intent_flag = pending_intent_flag_fix(PendingIntent.FLAG_CANCEL_CURRENT)
		val requestCode = message.hashCode() // duplicated code, 100000% don't care.
		val pendingActivityIntent = PendingIntent.getActivity(appContext, requestCode, activityIntent, intent_flag)
		return pendingActivityIntent
	}

	// common code for setting up a regular intent that opens the app to the main menu page
	fun mainActivityIntent(appContext: Context, message: String): Intent {
		val activityIntent = Intent(appContext, MainMenuActivity::class.java)
		activityIntent.action = message
		//modifies behavior when the user is already in the app.
		activityIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
		return activityIntent
	}

	// night and dark mode icon handling
	fun getExclamationMarkIcon(appContext: Context): Int {
		if (is_nightmode(appContext))
			return R.drawable.exclamation_mark_white
		else
			return R.drawable.exclamation_mark_black
	}
}


/**The purpose of this class is to deal with all that has to do with Survey Notifications.
 * This is a STATIC method, and is called from the main service.
 * @author Eli Jones */
object SurveyNotifications {
	private const val CHANNEL_ID = "survey_notification_channel"

	/**Show notifications for each survey in surveyIds, as long as that survey exists in PersistentData. */
	@JvmStatic
	fun showSurveyNotifications(appContext: Context, surveyIds: List<String>?) {
		// return early if empty
		if (surveyIds == null || surveyIds.isEmpty())  // ah, || short circuits, keep, don't use 'or'
			return

		val idsOfStoredSurveys = JSONUtils.jsonArrayToStringList(PersistentData.getSurveyIdsJsonArray())
		for (surveyId in surveyIds) {

			// displaySurveyNotification handle's android version details.
			if (idsOfStoredSurveys.contains(surveyId)) {
				displaySurveyNotification(appContext, surveyId)
			} else {
				val errorMsg = "Tried to show notification for survey ID $surveyId but didn't have" +
						" that survey stored in PersistentData."
				printe(errorMsg)
				TextFileManager.writeDebugLogStatement(errorMsg)
			}
		}
	}

	/**Creates a survey notification that transfers the user to the survey activity.
	 * Note: the notification can only be dismissed through submitting the survey
	 * @param appContext */
	@JvmStatic
	fun displaySurveyNotification(appContext: Context, surveyId: String) {
		//activityIntent contains information on the action triggered by tapping the notification.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ensureSurveyNotificationChannelExists(appContext)
			displaySurveyNotificationNew(appContext, surveyId)
		} else 
			displaySurveyNotificationOld(appContext, surveyId)
	}

	/** Survey notification function for phones running api versions O or newer
	 * Calls PersistentData.setSurveyNotificationState
	 * Uses Notification.Builder
	 * @param appContext
	 * @param surveyId */
	private fun displaySurveyNotificationNew(appContext: Context, surveyId: String) {
		val notificationBuilder = Notification.Builder(appContext, CHANNEL_ID)
		notificationBuilder.setContentTitle(appContext.getString(R.string.survey_notification_app_name))
		notificationBuilder.setShowWhen(true) // As of API 24 this no longer defaults to true and must be set explicitly
		var survey_name = PersistentData.getSurveyName(surveyId)?: ""
		if (survey_name != "")
			survey_name = " \"$survey_name\""

		// build the activity intent based on the type of survey
		val activityIntent: Intent
		if (PersistentData.getSurveyType(surveyId) == "tracking_survey") {
			activityIntent = Intent(appContext, SurveyActivity::class.java)
			activityIntent.action = appContext.getString(R.string.start_tracking_survey)
			notificationBuilder.setTicker(appContext.resources.getString(R.string.new_android_survey_notification_ticker))
			notificationBuilder.setContentText(appContext.resources.getString(R.string.new_android_survey_notification_details) + survey_name)
			notificationBuilder.setSmallIcon(R.drawable.survey_icon)
			notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.survey_icon))
			notificationBuilder.setGroup(surveyId)
		} else if (PersistentData.getSurveyType(surveyId) == "audio_survey") {
			activityIntent = Intent(appContext, getAudioSurveyClass(surveyId))
			activityIntent.action = appContext.getString(R.string.start_audio_survey)
			notificationBuilder.setTicker(appContext.resources.getString(R.string.new_audio_survey_notification_ticker))
			notificationBuilder.setContentText(appContext.resources.getString(R.string.new_audio_survey_notification_details) + survey_name)
			notificationBuilder.setSmallIcon(R.drawable.voice_recording_icon)
			notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.voice_recording_icon))
			notificationBuilder.setGroup(surveyId)
		} else {
			TextFileManager.writeDebugLogStatement(
				"encountered unknown survey type: " + PersistentData.getSurveyType(surveyId) + ", cannot schedule survey."
			)
			return
		}
		
		activityIntent.putExtra("surveyId", surveyId)
		activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP //modifies behavior when the user is already in the app.

		//This value is used inside the notification (and the pending intent) as the unique Identifier of that notification, this value must be an int.
		//note: recommendations about not using the .hashCode function in java are in usually regards to Object.hashCode(),
		// or are about the fact that the specific hash algorithm is not necessarily consistent between versions of the JVM.
		// If you look at the source of the String class hashCode function you will see that it operates on the value of the string, this is all we need.
		val surveyIdHash = surveyId.hashCode()

		/* The pending intent defines properties of the notification itself.
		 * BUG. Cannot use FLAG_UPDATE_CURRENT, which handles conflicts of multiple notification with the same id,
		 * so that the new notification replaces the old one.  if you use FLAG_UPDATE_CURRENT the notification will
		 * not launch the provided activity on android api 19.
		 * Solution: use FLAG_CANCEL_CURRENT, it provides the same functionality for our purposes.
		 * (or add android:exported="true" to the activity's permissions in the Manifest.)
		 * http://stackoverflow.com/questions/21250364/notification-click-not-launch-the-given-activity-on-nexus-phones */
		// We manually cancel the notification anyway, so this is likely moot.
		// UPDATE: when targetting api version 31 and above we have to set FLAG_IMMUTABLE (or mutable)

		val intent_flag = pending_intent_flag_fix(PendingIntent.FLAG_CANCEL_CURRENT)
		val pendingActivityIntent = PendingIntent.getActivity(appContext, surveyIdHash, activityIntent, intent_flag)
		notificationBuilder.setContentIntent(pendingActivityIntent)
		val surveyNotification = notificationBuilder.build()
		surveyNotification.flags = Notification.FLAG_ONGOING_EVENT
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(surveyIdHash) //cancel any current notification with this id hash
		notificationManager.notify(surveyIdHash, surveyNotification)  // If another notification with the same ID pops up, this notification will be updated/cancelled.

		//And, finally, set the notification state for zombie alarms.
		PersistentData.setSurveyNotificationState(surveyId, true)

		// Check if notifications have been disabled
		if (!notificationManager.areNotificationsEnabled()) {
			TextFileManager.writeDebugLogStatement("Participant has blocked notifications (1)")
			Log.e("SurveyNotifications", "Participant has blocked notifications (1)")
		}
	}

	/**Survey notification function for phones running api versions older than O
	 * Uses NotificationCompat.Builder
	 * @param appContext
	 * @param surveyId */
	@SuppressLint("UnspecifiedImmutableFlag")  // this is the compat version of the function
	private fun displaySurveyNotificationOld(appContext: Context, surveyId: String) {
		//activityIntent contains information on the action triggered by tapping the notification.
		val notificationBuilder = NotificationCompat.Builder(appContext)
		notificationBuilder.setContentTitle(appContext.getString(R.string.survey_notification_app_name))
		var survey_name = PersistentData.getSurveyName(surveyId)?: ""
		if (survey_name != "")
			survey_name = " \"$survey_name\""
		val activityIntent: Intent
		if (PersistentData.getSurveyType(surveyId) == "tracking_survey") {
			activityIntent = Intent(appContext, SurveyActivity::class.java)
			activityIntent.action = appContext.getString(R.string.start_tracking_survey)
			notificationBuilder.setTicker(appContext.resources.getString(R.string.new_android_survey_notification_ticker))
			notificationBuilder.setContentText(appContext.resources.getString(R.string.new_android_survey_notification_details) + survey_name)
			notificationBuilder.setSmallIcon(R.drawable.survey_icon)
			notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.survey_icon))
			notificationBuilder.setGroup(surveyId)
		} else if (PersistentData.getSurveyType(surveyId) == "audio_survey") {
			activityIntent = Intent(appContext, getAudioSurveyClass(surveyId))
			activityIntent.action = appContext.getString(R.string.start_audio_survey)
			notificationBuilder.setTicker(appContext.resources.getString(R.string.new_audio_survey_notification_ticker))
			notificationBuilder.setContentText(appContext.resources.getString(R.string.new_audio_survey_notification_details) + survey_name)
			notificationBuilder.setSmallIcon(R.drawable.voice_recording_icon)
			notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.voice_recording_icon))
			notificationBuilder.setGroup(surveyId)
		} else {
			TextFileManager.writeDebugLogStatement("encountered unknown survey type: " + PersistentData.getSurveyType(surveyId) + ", cannot schedule survey.")
			return
		}
		activityIntent.putExtra("surveyId", surveyId)
		activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP //modifies behavior when the user is already in the app.

		//This value is used inside the notification (and the pending intent) as the unique Identifier of that notification, this value must be an int.
		//note: recommendations about not using the .hashCode function in java are in usually regards to Object.hashCode(),
		// or are about the fact that the specific hash algorithm is not necessarily consistent between versions of the JVM.
		// If you look at the source of the String class hashCode function you will see that it operates on the value of the string, this is all we need.
		val surveyIdHash = surveyId.hashCode()

		/* The pending intent defines properties of the notification itself.
		 * BUG. Cannot use FLAG_UPDATE_CURRENT, which handles conflicts of multiple notification with the same id,
		 * so that the new notification replaces the old one.  if you use FLAG_UPDATE_CURRENT the notification will
		 * not launch the provided activity on android api 19.
		 * Solution: use FLAG_CANCEL_CURRENT, it provides the same functionality for our purposes.
		 * (or add android:exported="true" to the activity's permissions in the Manifest.)
		 * http://stackoverflow.com/questions/21250364/notification-click-not-launch-the-given-activity-on-nexus-phones */
		//we manually cancel the notification anyway, so this is likely moot.
		val pendingActivityIntent = PendingIntent.getActivity(appContext, surveyIdHash, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
		notificationBuilder.setContentIntent(pendingActivityIntent)
		val surveyNotification = notificationBuilder.build()
		surveyNotification.flags = Notification.FLAG_ONGOING_EVENT
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(surveyIdHash) //cancel any current notification with this id hash
		notificationManager.notify(surveyIdHash,  // If another notification with the same ID pops up, this notification will be updated/cancelled.
				surveyNotification)

		//And, finally, set the notification state for zombie alarms.
		PersistentData.setSurveyNotificationState(surveyId, true)

		// Check if notifications have been disabled
		if (!notificationManager.areNotificationsEnabled()) {
			TextFileManager.writeDebugLogStatement("Participant has blocked notifications (2)")
			Log.e("SurveyNotifications", "Participant has blocked notifications (2)")
		}
	}

	// Apps targeting api 26 or later need a notification channel to display notifications
	private fun ensureSurveyNotificationChannelExists(context: Context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			// if it already exists return early
			val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			if (notificationManager.getNotificationChannel(CHANNEL_ID) != null)
				return

			// Originally copied these from an example, these values could change but its fine?
			val notificationChannel = NotificationChannel(
					CHANNEL_ID, "Survey Notification", NotificationManager.IMPORTANCE_LOW)
			notificationChannel.description = "The Beiwe App notification channel"
			notificationChannel.enableLights(true)
			notificationChannel.lightColor = Color.RED
			notificationChannel.vibrationPattern = BEIWE_VIBRATION_PATTERN
			notificationChannel.enableVibration(true)
			notificationManager.createNotificationChannel(notificationChannel)
		}
	}

	/**Use to dismiss the notification corresponding the surveyIdInt.
	 * @param appContext
	 * @param surveyId */
	@JvmStatic
	fun dismissSurveyNotification(appContext: Context, surveyId: String) {
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(surveyId.hashCode())
		PersistentData.setSurveyNotificationState(surveyId, false)
		// if there are no future alarms / alarms at all set for a survey (as can occur when a
		// relative or absolute surveys that age-out of the weekly timings list) the survey timer
		// will become stuck in the "on" state.  setting it to this (long maximum) value will
		// prevent the survey from being instantly triggered.
		PersistentData.setMostRecentSurveyAlarmTime(surveyId, 9223372036854775807L)
	}

	fun isNotificationActive(appContext: Context, surveyId: String): Boolean {
		val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		// loop over notifications and check if any of them are the surveyId passed in
		for (notification: StatusBarNotification in notificationManager.activeNotifications) {
			if (notification.id == surveyId.hashCode()) {
				return true
			}
		}
		return false
	}

	/**Tries to determine the type of audio survey.  If it is an Enhanced audio survey AudioRecorderEnhancedActivity.class is returned,
	 * any other outcome (including an inability to determine type) returns AudioRecorderActivity.class instead.  */
	fun getAudioSurveyClass(surveyId: String): Class<*> {
		try {
			val surveySettings = JSONObject(PersistentData.getSurveySettings(surveyId))
			if (surveySettings.getString("audio_survey_type") == "raw")
				return AudioRecorderEnhancedActivity::class.java
		} catch (e: JSONException) {
			e.printStackTrace()
		}

		return AudioRecorderActivity::class.java
	}
}
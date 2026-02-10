package org.beiwe.app.networking

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.beiwe.app.JSONUtils
import org.beiwe.app.printe
import org.beiwe.app.printi
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import org.beiwe.app.ui.utils.MessageNotification
import org.json.JSONArray
import org.json.JSONException

class FCMService : FirebaseMessagingService() {
	companion object {
		const val errorMessage = "Unable to get FCM token, will not be able to receive push notifications."
	}

	/**Called if InstanceID token is updated. This may occur if the security of the previous token
	 * had been compromised. Note that this is called when the InstanceID token is initially
	 * generated so this is where you would retrieve the token.  */
	override fun onNewToken(token: String) {
		// If you want to send messages to this application instance or manage this app's
		// subscriptions on the server side, send the Instance ID token to your app server.
		val fcmBlockerThread = Thread(Runnable {
			while (!PersistentData.getIsRegistered()) {
				try {
					Thread.sleep(1000)
				} catch (ignored: InterruptedException) {
					printe("${Companion.errorMessage}(3)")
					TextFileManager.writeDebugLogStatement("${Companion.errorMessage}(3)")
					return@Runnable
				}
			}
			PersistentData.setFCMInstanceID(token)
			PostRequest.sendFCMInstanceID(token)
		}, "fcmBlockerThread")
		fcmBlockerThread.start()
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		printi("FCM", "From: " + remoteMessage.from)
		printi("FCM", "Message data payload: " + remoteMessage.data)
		printi("FCM", "Message Notification Body: " + remoteMessage.notification?.body)

		val data = remoteMessage.data
		// for now we only really care about the survey notification type, we may add later types
		val notification_type = data["type"]
		if (notification_type == null) {
			val errorMsg = "Received push notification with no type."
			printe(errorMsg)
			TextFileManager.writeDebugLogStatement(errorMsg)
			return
		}

		// handle survey notifications
		if (notification_type == "survey") {
			// case: device may not have a particular survey.
			// Get the list of survey_ids from the push notification's JSON data
			val notificationSurveyIds: List<String> = try {
				JSONUtils.jsonArrayToStringList(JSONArray(data["survey_ids"]))
			} catch (e: JSONException) {
				e.printStackTrace()
				val errorMsg = "received unparsable push notification for new surveys"
				printe(errorMsg)
				TextFileManager.writeDebugLogStatement(errorMsg)
				// exit function early if the json was invalid
				return
			}
			// this call to downloadSurveys will run the full download, update, schedule, and show
			// known notifications that it has in internal state, as well as force notifications for
			// all surveys it received FCM push notification for.  Cases are safely handled, the
			// whole thing runs on a background thread.
			SurveyDownloader.downloadSurveys(applicationContext, notificationSurveyIds)
		}

		// Message notifications cause the app to show a notification with the message text.
		if (notification_type == "message") {
			val message = data["message"]
			if (message == null) {
				printe("FCM", "received message push notification with no message")
				return
			}
			MessageNotification.showNotificationMessage(applicationContext, message)
		}
	}
}
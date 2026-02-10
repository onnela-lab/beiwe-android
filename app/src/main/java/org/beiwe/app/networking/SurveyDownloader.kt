package org.beiwe.app.networking

import android.content.Context
import android.util.Log
import org.beiwe.app.CrashHandler.Companion.writeCrashlog
import org.beiwe.app.JSONUtils
import org.beiwe.app.MainService.Companion.cancelSurveyAlarm
import org.beiwe.app.MainService.Companion.registerTimers
import org.beiwe.app.R
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.survey.SurveyScheduler
import org.beiwe.app.ui.utils.SurveyNotifications.dismissSurveyNotification
import org.beiwe.app.ui.utils.SurveyNotifications.showSurveyNotifications
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object SurveyDownloader {
	/* @param notificationSurveyIds if any IDs are passed in, immediately show notifications for
	/ * those surveys (after the app finishes downloading all surveys). */
	fun downloadSurveys(appContext: Context, notificationSurveyIds: List<String>?) {
		doDownload(PostRequest.addWebsitePrefix(appContext.resources
				.getString(R.string.download_surveys_url)), appContext, notificationSurveyIds)
	}

	@JvmStatic
	/* Sets off an asynctask that downloads surveys and triggers any notifications we may need to be
	/ * displayed to the user */
	private fun doDownload(
			url: String, appContext: Context, notificationSurveyIds: List<String>?,
	) {
		object : HTTPAsync(url) {
			var jsonResponseString: String? = null
			override fun doInBackground(vararg arg0: Void?): Void? {
				try {
					jsonResponseString = PostRequest.httpRequestString("", url)
				} catch (ignored: NullPointerException) {
				}
				return null // hate.
			}

			override fun onPostExecute(arg: Void?) {
				responseCode = updateSurveys(appContext, jsonResponseString)
				showSurveyNotifications(appContext, notificationSurveyIds)
				super.onPostExecute(arg)
			}
		}.execute()
	}

	// Returns an appropriate return code for the httpAsync error parsing.  -1 if something goes
	// wrong, 200 if it works.
	private fun updateSurveys(appContext: Context, jsonString: String?): Int {
		if (jsonString == null) {
			Log.e("Survey Downloader", "jsonString is null, probably have no network connection. squashing.")
			return -1
		}
		val surveys: List<String>
		surveys = try {
			JSONUtils.jsonArrayToStringList(JSONArray(jsonString))
		} catch (e: JSONException) {
//			CrashHandler.writeCrashlog(e, appContext) // this crash report has causes problems.
			Log.e("Survey Downloader", "JSON PARSING FAIL FAIL FAIL")
			return -1
		}

		// go through the list of surveys, updating or creating as necessary.
		val oldSurveyIds = PersistentData.getSurveyIds()
		val newSurveyIds = ArrayList<String>()

		for (surveyString in surveys) {
			val surveyJSON = try {
				JSONObject(surveyString)
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON fail 1")
				return -1
			}

			// Log.d("debugging survey update", "whole thing: " + surveyJSON.toString())
			val surveyId = try {
				surveyJSON.getString("_id")
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON fail 2")
				return -1
			}
			// Log.d("debugging survey update", "id: " + surveyId.toString())
			val surveyType = try {
				surveyJSON.getString("survey_type")
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON fail 2.5")
				return -1
			}
			// Log.d("debugging survey update", "type: " + surveyType.toString())
			val jsonQuestionsString = try {
				surveyJSON.getString("content")
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON fail 3")
				return -1
			}
			// Log.d("debugging survey update", "questions: " + jsonQuestionsString)
			val jsonTimingsString = try {
				surveyJSON.getString("timings")
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON fail 4")
				return -1
			}
			// Log.d("debugging survey update", "timings: " + jsonTimingsString)
			val jsonSettingsString = try {
				surveyJSON.getString("settings")
			} catch (e: JSONException) {
				writeCrashlog(e, appContext)
				Log.e("Survey Downloader", "JSON settings not present")
				return -1
			}
			// Log.d("debugging survey update", "settings: " + jsonSettingsString)

			// name - force to empty string if somehow present as a null.
			val surveyName = surveyJSON.optString("name", "") ?: ""

			if (oldSurveyIds.contains(surveyId)) {
				// if surveyId already exists, check for changes, add to list of new survey ids.
				// Log.d("debugging survey update", "checking for changes")
				PersistentData.setSurveyContent(surveyId, jsonQuestionsString)
				PersistentData.setSurveyType(surveyId, surveyType)
				PersistentData.setSurveySettings(surveyId, jsonSettingsString)
				PersistentData.setSurveyName(surveyId, surveyName)
				// Log.d("debugging survey update", "A is incoming, B is current.")
				// Log.d("debugging survey update", "A) " + jsonTimingsString)
				// Log.d("debugging survey update", "B) " + PersistentData.getSurveyTimes(surveyId) )
				if (PersistentData.getSurveyTimes(surveyId) != jsonTimingsString) {
					// Log.i("SurveyDownloader.java", "The survey times, they are a changin!")
					cancelSurveyAlarm(surveyId)
					PersistentData.setSurveyTimes(surveyId, jsonTimingsString)
					SurveyScheduler.scheduleSurvey(surveyId)
				}
				newSurveyIds.add(surveyId)
			} else { // if survey is new, create new survey entry.
				// Log.d("debugging survey update", "CREATE A SURVEY")
				PersistentData.addSurveyId(surveyId)
				PersistentData.createSurveyData(
					surveyId, jsonQuestionsString, jsonTimingsString, surveyType, jsonSettingsString, surveyName
				)
				registerTimers(appContext) // We need to register the surveyId before we can schedule it
				SurveyScheduler.scheduleSurvey(surveyId)
				SurveyScheduler.checkImmediateTriggerSurvey(appContext, surveyId)
			}
		}

		// remove any surveys that are no longer present.
		for (oldSurveyId in oldSurveyIds) { // for each old survey id
			if (!newSurveyIds.contains(oldSurveyId)) {
				// check if it is still a valid survey (it the list of new survey ids.)
				// Log.d("survey downloader", "deleting survey " + oldSurveyId)
				PersistentData.deleteSurvey(oldSurveyId)
				// It is almost definitely not worth the effort to cancel any ongoing alarms for a
				// survey. They are one-time, and there is de minimus value to actually cancelling
				// it. Also, that requires accessing the main service, which means using ugly hacks
				// like we do with the survey scheduler (though it would be okay because this code
				// can only actually run if the main service is already instantiated.)
				dismissSurveyNotification(appContext, oldSurveyId)
				registerTimers(appContext)
			}
		}
		return 200
	}
}
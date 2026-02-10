package org.beiwe.app.ui.user

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main_menu.main_menu_call_clinician
import org.beiwe.app.R
import org.beiwe.app.printe
import org.beiwe.app.printi
import org.beiwe.app.session.SessionActivity
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.survey.SurveyActivity
import org.beiwe.app.ui.utils.SurveyNotifications
import org.json.JSONException
import org.json.JSONObject


class MainMenuActivity : SessionActivity() {
	val mainMenuHandlerThread = HandlerThread("main_activity_handler_thread")
	val mainMenuLooper: Looper
	val mainMenuHandler: Handler

	init {
		mainMenuHandlerThread.start()
		mainMenuLooper = mainMenuHandlerThread.looper
		mainMenuHandler = Handler(mainMenuLooper)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main_menu)

		if (PersistentData.getCallClinicianButtonEnabled())
			main_menu_call_clinician.text = PersistentData.getCallClinicianButtonText()
		else
			main_menu_call_clinician.visibility = View.GONE
	}

	// run an task every 5 seconds to refresh the available survey list
	override fun onResume() {
		super.onResume()
		setupSurveyList()
		mainMenuHandler.post(mainUiUpdater)
	}

	override fun onPause() {
		super.onPause()
		setupSurveyList()
		mainMenuHandler.removeCallbacks(mainUiUpdater)
	}

	fun setupSurveyList() {
		// get the active and always available surveys
		val surveyIds = ArrayList<String>()
		for (surveyId: String in PersistentData.getSurveyIds()) {
			try {
				// default to an empty dictionary to handle nulls
				val surveySettings = JSONObject(PersistentData.getSurveySettings(surveyId) ?: "{}")
				// persistent data notification state appears to be the source of truth
				// for whether a survey should have be takeable.
				val isActive = PersistentData.getSurveyNotificationState(surveyId)

				if (surveySettings.getBoolean("always_available") || isActive) {
					// printi("adding survey $surveyId")
					surveyIds.add(surveyId)
				}
			} catch (e: JSONException) {
				// printe("survey $surveyId broke inside json parsing")
				e.printStackTrace()
			}
		}

		// Go through the surveys, get any that should be active, set button text, enable button.
		var buttonCount = 0
		for (i in surveyIds.indices) {
			// printi("looking for permSurvey$i")
			buttonCount = i
			var surveyName = PersistentData.getSurveyName(surveyIds[i])
			val surveyType = PersistentData.getSurveyType(surveyIds[i])
			val button = findViewById<View>(resources.getIdentifier(
				"permSurvey$i", "id", this.packageName)) as Button

			// set textAllCaps and surveyName if there is no survey name
			if (surveyName == "") {
				button.isAllCaps = true
				surveyName = if (surveyType == "audio_survey")
					getString(R.string.permaaudiosurvey)
				else
					getString(R.string.perm_survey)
			} else
				button.isAllCaps = false

			// add microphone emoji to survey name if it is an audio survey
			if (surveyType == "audio_survey")
				surveyName = "$surveyName 🎙"

			// set button text, tag, and visibility
			button.text = surveyName
			button.setTag(R.string.permasurvey, surveyIds[i])
			button.visibility = View.VISIBLE
			// printi("setting button permSurvey$i text to $surveyName")
			// printi("setting button permSurvey$i tag to ${surveyIds[i]}")
			// printi("setting button permSurvey$i visibility to VISIBLE")

			// (there are 17 buttons numbered 0 to 16)
			if (i >= 16)
				break
		}

		// iterate over every unused button, disable button and GONE it to fix scroll bugs
		// (there are 17 buttons numbered 0 to 16, )
		for (i in buttonCount + 1..16) {
			val button = findViewById<View>(resources.getIdentifier(
				"permSurvey$i", "id", this.packageName)) as Button
			button.visibility = View.GONE
			// printi("setting button visibility for permSurvey$i to GONE")
		}
	}

	// this requires both the annotation and the lazy initialization to be allowed to reference
	// itself inside itself, and also creating a function that returns the Runnable object seems
	// to create a new object every time it is called, breaking removeCallbacks.
	val mainUiUpdater: Runnable by lazy {
		Runnable {
			// printi("update_main_buttons")
			runOnUiThread {
				setupSurveyList()
			}
			mainMenuHandler.removeCallbacks(mainUiUpdater) // fix possible where multiple are stacked?
			mainMenuHandler.postDelayed(mainUiUpdater, 1000)
		}
	}

	/*#########################################################################
	############################## Buttons ####################################
	#########################################################################*/

	fun displaySurvey(view: View) {
		val activityIntent: Intent
		val surveyId = view.getTag(R.string.permasurvey) as String
		activityIntent = if (PersistentData.getSurveyType(surveyId) == "audio_survey") {
			Intent(applicationContext, SurveyNotifications.getAudioSurveyClass(surveyId))
		} else {
			Intent(applicationContext, SurveyActivity::class.java)
		}
		activityIntent.action = applicationContext.getString(R.string.start_tracking_survey)
		activityIntent.putExtra("surveyId", surveyId)
		activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
		startActivity(activityIntent)
	}
}
package org.beiwe.app.survey

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.beiwe.app.JSONUtils
import org.beiwe.app.R
import org.beiwe.app.printe
import org.beiwe.app.session.SessionActivity
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import org.beiwe.app.survey.QuestionFragment.OnGoToNextQuestionListener
import org.beiwe.app.survey.SurveySubmitFragment.OnSubmitButtonClickedListener
import org.beiwe.app.ui.user.MainMenuActivity
import org.beiwe.app.ui.utils.SurveyNotifications.dismissSurveyNotification
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**The SurveyActivity displays to the user the survey that has been pushed to the device.
 * Layout in this activity is rendered, not static.
 * @author Josh Zagorsky, Eli Jones
 */
class SurveyActivity : SessionActivity(), OnGoToNextQuestionListener, OnSubmitButtonClickedListener {
	lateinit var surveyId: String
	var surveyLogic: JsonSkipLogic? = null
	var hasLoadedBefore = false
	var initialViewMoment: Long = 0
	var questionFragment: QuestionFragment? = null

	override fun onCreate(bundle: Bundle?) {
		PersistentData.setTakingSurvey()
		super.onCreate(bundle) // reload saved instance state
		initialViewMoment = System.currentTimeMillis()
		setContentView(R.layout.activity_survey)
		val triggerIntent = intent
		// if you didn't hand it a survey id, it crashes. that is literally the most core thing in
		// the app, so we're going to crash over it. Don't set it up to do that.
		this.surveyId = triggerIntent.getStringExtra("surveyId")!!
	}

	override fun onDestroy() {
		super.onDestroy()
		PersistentData.setNotTakingSurvey()
	}

	override fun doBackgroundDependentTasks() {
		super.doBackgroundDependentTasks()
		if (!hasLoadedBefore) {
			setUpQuestions(surveyId)
			// Run the logic as if we had just pressed next without answering a hypothetical question -1
			_go_to_next_question()
			// Record the time that the survey was first visible to the user
			SurveyTimingsRecorder.recordSurveyFirstDisplayed(surveyId)
			// Onnela lab requested this line in the debug log
			TextFileManager.writeDebugLogStatement("$initialViewMoment opened survey $surveyId.")
			hasLoadedBefore = true
		}
	}

	override fun goToNextQuestion(questionData: QuestionData) {
		// in this context the dataFromCurrentQuestion is the data from the previous question.
		// questionData.pprint() // debugging...
		if (surveyLogic!!.currentQuestionRequired!! && !questionData.questionIsAnswered()) {
			Toast.makeText(this, "This question is required.", Toast.LENGTH_SHORT).show()
			return
		}
		_go_to_next_question()
	}

	/** code block that just goes to the next question */
	fun _go_to_next_question() {
		val nextQuestion = surveyLogic!!.nextQuestion()
		// If you've run out of questions, display the Submit button, otherwise go to next question.
		if (nextQuestion == null) {
			displaySurveySubmitFragment()
		} else {
			displaySurveyQuestionFragment(nextQuestion, surveyLogic!!.onFirstQuestion())
		}
	}

	override fun onBackPressed() {
		super.onBackPressed()
		val previousQuestion = surveyLogic!!.previousQuestion()
		// running the current version of the display logic on the previous question allows answers
		// to be saved on a back button press, but can cause overwriting of visible activity...
		// stuff. Point is need to work out correct backstack management (I think) for this to work.
		// displaySurveyQuestionFragment(previousQuestion!!, surveyLogic!!.onFirstQuestion())
	}

	private fun displaySurveyQuestionFragment(jsonQuestion: JSONObject, isFirstQuestion: Boolean) {
		// Create a question fragment with the attributes of the question
		questionFragment = QuestionFragment()
		questionFragment!!.arguments = QuestionJSONParser.getQuestionArgsFromJSONString(jsonQuestion)

		// Put the fragment into the view
		val fragmentTransaction = fragmentManager.beginTransaction()
		if (isFirstQuestion) {
			fragmentTransaction.add(R.id.questionFragmentGoesHere, questionFragment)
		} else {
			fragmentTransaction.replace(R.id.questionFragmentGoesHere, questionFragment)
			fragmentTransaction.addToBackStack(null)
		}
		fragmentTransaction.commit()
	}

	private fun displaySurveySubmitFragment() {
		val args = Bundle()
		args.putStringArrayList("unansweredQuestions", surveyLogic!!.unansweredQuestions)
		val submitFragment = SurveySubmitFragment()
		submitFragment.arguments = args
		val fragmentTransaction = fragmentManager.beginTransaction()
		fragmentTransaction.replace(R.id.questionFragmentGoesHere, submitFragment)
		fragmentTransaction.addToBackStack(null)
		fragmentTransaction.commit()
	}

	private fun setUpQuestions(surveyId: String) {
		// Get survey settings
		var randomizeWithMemory = false
		var randomize = false
		var numberQuestions = 0

		try {
			// default to an empty object
			val surveySettings = JSONObject(PersistentData.getSurveySettings(surveyId)?: "{}")
			randomizeWithMemory = surveySettings.optBoolean(getString(R.string.randomizeWithMemory), false)
			randomize = surveySettings.optBoolean(getString(R.string.randomize), false)
			numberQuestions = surveySettings.optInt(getString(R.string.numberQuestions), 0)
		} catch (e: JSONException) {
			Log.e("Survey Activity", "There was an error parsing survey settings")
			e.printStackTrace()
		}

		try { // Get survey content as an array of questions; each question is a JSON object
			var jsonQuestions: JSONArray? = JSONArray(PersistentData.getSurveyContent(surveyId))
			// If randomizing the question order, reshuffle the questions in the JSONArray
			if (randomize && !randomizeWithMemory)
				jsonQuestions = JSONUtils.shuffleJSONArray(jsonQuestions, numberQuestions)
			if (randomize && randomizeWithMemory)
				jsonQuestions = JSONUtils.shuffleJSONArrayWithMemory(jsonQuestions, numberQuestions, surveyId)

			// construct the survey's skip logic.
			// (param 2: If randomization is enabled do not run the skip logic for the survey.)
			this.surveyLogic = JsonSkipLogic(jsonQuestions!!, !randomize, applicationContext)
		} catch (e: JSONException) {
			e.printStackTrace()  // should we throw/report this?
		}
	}

	/**Called when the user presses "Submit" at the end of the survey,
	 * saves the answers, and takes the user back to the main page.  */
	override fun submitButtonClicked() {
		SurveyTimingsRecorder.recordSubmit(applicationContext)

		// Write the data to a SurveyAnswers file
		val success = SurveyAnswersRecorder().writeLinesToFile(surveyId, surveyLogic!!)
		val toastMsg: String = if (success) {
			PersistentData.getSurveySubmitSuccessToastText()
		} else {
			applicationContext.resources.getString(R.string.survey_submit_error_message)
		}
		// Show a Toast telling the user either "Thanks, success!" or "Oops, there was an error"
		Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()

		// Close the Activity
		dismissSurveyNotification(applicationContext, surveyId!!)
		val activityIntent = Intent(applicationContext, MainMenuActivity::class.java)
		activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
		startActivity(activityIntent)
		finish()
	}
}
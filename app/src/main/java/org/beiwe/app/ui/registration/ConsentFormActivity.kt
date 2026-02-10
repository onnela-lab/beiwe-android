package org.beiwe.app.ui.registration

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_consent_form.*
import org.beiwe.app.R
import org.beiwe.app.RunningBackgroundServiceActivity
import org.beiwe.app.networking.SurveyDownloader
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import org.beiwe.app.ui.LoadingActivity

class ConsentFormActivity : RunningBackgroundServiceActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_consent_form)
		consent_form_body.text = PersistentData.getConsentFormText()
	}

	/** On the press of the do not consent button, we pop up an alert, allowing the user
	 * to press "Cancel" if they did not mean to press the do not consent.  */
	fun doNotConsentButton(view: View?) {
		val alertBuilder = AlertDialog.Builder(this@ConsentFormActivity)
		alertBuilder.setTitle(getString(R.string.doNotConsentButton))
		alertBuilder.setMessage(getString(R.string.doNotConsentAlert))
		alertBuilder.setPositiveButton(getString(R.string.i_understand_button_text)) { dialog, which ->
			finish()
			System.exit(0)
		}
		alertBuilder.setNegativeButton(getString(R.string.alert_cancel_button_text), DialogInterface.OnClickListener { dialog, which ->
			return@OnClickListener
		})
		alertBuilder.create().show()
	}

	fun consentButton(view: View?) {
		consent(this)
	}

	companion object {
		@JvmStatic
		fun consent(activity: Activity) {
			PersistentData.setIsRegistered(true)
			PersistentData.loginOrRefreshLogin()

			// Download the survey questions and schedule the surveys
			SurveyDownloader.downloadSurveys(activity.applicationContext, null)

			// Create new data files, these will now have a patientID prepended to those files
			TextFileManager.initialize(activity.applicationContext)
			TextFileManager.makeNewFilesForEverything()

			//This is important.  we need to start timers...
			(activity as RunningBackgroundServiceActivity).mainService!!.doSetup()

			// Start the Main Screen Activity, destroy this activity
			activity.startActivity(Intent(activity.applicationContext, LoadingActivity::class.java))
			activity.finish()
		}
	}
}
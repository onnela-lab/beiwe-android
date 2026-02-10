package org.beiwe.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_debug_interface.*
import org.beiwe.app.*
import org.beiwe.app.Timer
import org.beiwe.app.networking.PostRequest
import org.beiwe.app.networking.SurveyDownloader
import org.beiwe.app.session.SessionActivity
import org.beiwe.app.storage.EncryptionEngine
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.SetDeviceSettings
import org.beiwe.app.storage.TextFileManager
import org.beiwe.app.survey.JsonSkipLogic
import org.beiwe.app.ui.user.MainMenuActivity
import org.beiwe.app.ui.utils.SurveyNotifications
import org.beiwe.app.ui.utils.SurveyNotifications.isNotificationActive
import org.json.JSONArray
import org.json.JSONException
import java.security.spec.InvalidKeySpecException
import java.util.*

@Suppress("UNUSED_PARAMETER")
class DebugInterfaceActivity : SessionActivity() {
	//extends a session activity.

	override fun onCreate(bundle: Bundle?) {
		// bundle is the saved instance state, if there is one.
		super.onCreate(bundle)
		setContentView(R.layout.activity_debug_interface)

		if (BuildConfig.APP_IS_DEV) {
			debugtexttwenty.visibility = View.VISIBLE
			testJsonLogicParser.visibility = View.VISIBLE
			buttonPrintSurveys.visibility = View.VISIBLE
			buttonClearInternalLog.visibility = View.VISIBLE
			buttonDeleteEverything.visibility = View.VISIBLE
			buttonListFiles.visibility = View.VISIBLE
			buttonStartTimer.visibility = View.VISIBLE
			buttonLogDataToggles.visibility = View.VISIBLE
			buttonAlarmStates.visibility = View.VISIBLE
			buttonFeaturesEnabled.visibility = View.VISIBLE
			buttonFeaturesPermissable.visibility = View.VISIBLE
			sendTestNotification.visibility = View.VISIBLE
			sendSurveyNotification.visibility = View.VISIBLE
			printSurveyContent.visibility = View.VISIBLE

			// These are specialized and only need to be enabled when testing app resurrection.
			// buttonCrashUi.visibility = View.VISIBLE
			// buttonCrashBackground.visibility = View.VISIBLE
			// buttonCrashBackgroundInFive.visibility = View.VISIBLE
			// buttonTestManualErrorReport.visibility = View.VISIBLE
			// stopBackgroundService.visibility = View.VISIBLE
			// buttonEnterANRUI.visibility = View.VISIBLE
			// buttonEnterANRBackground.visibility = View.VISIBLE

			// These are seriously old and don't need to be visible under almost all circumstances.
			// testEncryption.visibility View.VISIBLE
			// buttonPrintInternalprintvisibility View.VISIBLE
			// buttonGetKeyFile.visibility View.VISIBLE
		}
	}

	//Intent triggers caught in BackgroundService
	fun accelerometerOn(view: View?) {
		mainService!!.accelerometerListener!!.turn_on()
	}

	fun accelerometerOff(view: View?) {
		mainService!!.accelerometerListener!!.turn_off()
	}

	fun gyroscopeOn(view: View?) {
		mainService!!.gyroscopeListener!!.turn_on()
	}

	fun gyroscopeOff(view: View?) {
		mainService!!.gyroscopeListener!!.turn_off()
	}

	fun gpsOn(view: View?) {
		mainService!!.gpsListener!!.turn_on()
	}

	fun gpsOff(view: View?) {
		mainService!!.gpsListener!!.turn_off()
	}

	fun scanWifi(view: View?) {
		this.applicationContext.sendBroadcast(Timer.wifiLogIntent)
	}

	fun bluetoothButtonStart(view: View?) {
		this.applicationContext.sendBroadcast(Timer.bluetoothOnIntent)
	}

	fun bluetoothButtonStop(view: View?) {
		this.applicationContext.sendBroadcast(Timer.bluetoothOffIntent)
	}

	//raw debugging info
	fun printInternalLog(view: View?) {
		// printi("print log button pressed", "press.");
		val log = TextFileManager.getDebugLogFile().read()
		for (line in log.split("\n".toRegex()).toTypedArray()) {
			printi("log file...", line)
		}
		//		printi("log file encrypted", EncryptionEngine.encryptAES(log) );
	}

	fun printSurveys(view: View?) {
		val surveyIDs = PersistentData.getSurveyIds()
		printi("debug", surveyIDs.toString())
		for (survey_id in surveyIDs) {
			printi("survey", survey_id)
			printi("survey", PersistentData.getSurveyContent(survey_id))
		}
	}

	fun printSurveySettings(view: View) {
		for (survey_id in PersistentData.getSurveyIds() ){
			print(survey_id)
			print(PersistentData.getSurveySettings(survey_id));
		}
	}

	fun testEncrypt(view: View?) {
		printi("Debug..", TextFileManager.getKeyFile().read())
		val data = TextFileManager.getKeyFile().read()
		printi("reading keyFile:", data)

		try {
			EncryptionEngine.readKey()
		} catch (e: InvalidKeySpecException) {
			printe("DebugInterfaceActivity", "this is only partially implemented, unknown behavior")
			e.printStackTrace()
			throw NullPointerException("some form of encryption error, type 1")
		}

		val encrypted = try {
			EncryptionEngine.encryptRSA("ThIs Is a TeSt".toByteArray()).toString()
		} catch (e: InvalidKeySpecException) {
			printe("DebugInterfaceActivity", "this is only partially implemented, unknown behavior")
			e.printStackTrace()
			throw NullPointerException("some form of encryption error, type 2")
		}

		printi("test encrypt - length:", "" + encrypted.length)
		printi("test encrypt - output:", encrypted)
		printi("test hash:", EncryptionEngine.safeHash(encrypted))
		printi("test hash:", EncryptionEngine.hashMAC(encrypted))
	}

	fun logDataToggles(view: View?) {
		printi("Debug.DataToggles", "Accelerometer: " + PersistentData.getAccelerometerEnabled().toString() )
		printi("Debug.DataToggles", "Gyroscope: " + PersistentData.getGyroscopeEnabled().toString() )
		printi("Debug.DataToggles", "GPS: " + PersistentData.getGpsEnabled().toString() )
		printi("Debug.DataToggles", "Calls: " + PersistentData.getCallLoggingEnabled().toString() )
		printi("Debug.DataToggles", "Texts: " + PersistentData.getTextsEnabled().toString() )
		printi("Debug.DataToggles", "WiFi: " + PersistentData.getWifiEnabled().toString() )
		printi("Debug.DataToggles", "Bluetooth: " + PersistentData.getBluetoothEnabled().toString() )
		printi("Debug.DataToggles", "Power State: " + PersistentData.getPowerStateEnabled().toString() )
	}

	fun getAlarmStates(view: View?) {
		val ids = PersistentData.getSurveyIds()
		for (surveyId in ids) {
			printi(
					"most recent alarm state",
					"survey id: " + surveyId + ", most recent: " + PersistentData.getMostRecentSurveyAlarmTime(surveyId) + ", active: " + PersistentData.getSurveyNotificationState(surveyId)
			)
			printi("survey timings", "(first element is Sunday): " + PersistentData.getSurveyTimes(surveyId))

		}
	}

	fun getActiveNotifications(view: View?) {
		val ids = PersistentData.getSurveyIds()
		for (surveyId in ids) {
			print("survey id: " + surveyId)
			print("should be active: " + PersistentData.getSurveyNotificationState(surveyId))
			print("is active: " + isNotificationActive(applicationContext, surveyId))
		}
	}

	fun getEnabledFeatures(view: View?) {
		if (PersistentData.getAccelerometerEnabled())
			printi("features", "Accelerometer Enabled.")
		else
			printe("features", "Accelerometer Disabled.")

		if (PersistentData.getGyroscopeEnabled())
			printi("features", "Gyroscope Enabled.")
		else
			printe("features", "Gyroscope Disabled.")

		if (PersistentData.getGpsEnabled())
			printi("features", "Gps Enabled.")
		else
			printe("features", "Gps Disabled.")

		if (PersistentData.getCallLoggingEnabled())
			printi("features", "Calls Enabled.")
		else
			printe("features", "Calls Disabled.")

		if (PersistentData.getTextsEnabled())
			printi("features", "Texts Enabled.")
		else
			printe("features", "Texts Disabled.")

		if (PersistentData.getWifiEnabled())
			printi("features", "Wifi Enabled.")
		else
			printe("features", "Wifi Disabled.")

		if (PersistentData.getBluetoothEnabled())
			printi("features", "Bluetooth Enabled.")
		else
			printe("features", "Bluetooth Disabled.")

		if (PersistentData.getPowerStateEnabled())
			printi("features", "PowerState Enabled.")
		else
			printe("features", "PowerState Disabled.")

	}

	fun getPermissableFeatures(view: View?) {
		if (PermissionHandler.checkAccessFineLocation(applicationContext))
			printi("permissions", "AccessFineLocation enabled.")
		else
			printe("permissions", "AccessFineLocation disabled.")

		if (PermissionHandler.checkAccessNetworkState(applicationContext))
			printi("permissions", "AccessNetworkState enabled.")
		else
			printe("permissions", "AccessNetworkState disabled.")

		if (PermissionHandler.checkAccessWifiState(applicationContext))
			printi("permissions", "AccessWifiState enabled.")
		else
			printe("permissions", "AccessWifiState disabled.")

		if (PermissionHandler.checkAccessBluetooth(applicationContext))
			printi("permissions", "Bluetooth enabled.")
		else
			printe("permissions", "Bluetooth disabled.")

		if (PermissionHandler.checkAccessBluetoothAdmin(applicationContext))
			printi("permissions", "BluetoothAdmin enabled.")
		else
			printe("permissions", "BluetoothAdmin disabled.")

		if (PermissionHandler.checkCanMakePhoneCall(applicationContext))
			printi("permissions", "CallPhone enabled.")
		else
			printe("permissions", "CallPhone disabled.")

		if (PermissionHandler.checkAccessReadCallLog(applicationContext))
			printi("permissions", "ReadCallLog enabled.")
		else
			printe("permissions", "ReadCallLog disabled.")

		if (PermissionHandler.checkAccessReadContacts(applicationContext))
			printi("permissions", "ReadContacts enabled.")
		else
			printe("permissions", "ReadContacts disabled.")

		if (PermissionHandler.checkAccessReadPhoneState(applicationContext))
			printi("permissions", "ReadPhoneState enabled.")
		else
			printe("permissions", "ReadPhoneState disabled.")

		if (PermissionHandler.checkAccessReadSms(applicationContext))
			printi("permissions", "ReadSms enabled.")
		else
			printe("permissions", "ReadSms disabled.")

		if (PermissionHandler.checkAccessReceiveMms(applicationContext))
			printi("permissions", "ReceiveMms enabled.")
		else
			printe("permissions", "ReceiveMms disabled.")

		if (PermissionHandler.checkAccessReceiveSms(applicationContext))
			printi("permissions", "ReceiveSms enabled.")
		else
			printe("permissions", "ReceiveSms disabled.")

		if (PermissionHandler.checkAccessRecordAudio(applicationContext))
			printi("permissions", "RecordAudio enabled.")
		else
			printe("permissions", "RecordAudio disabled.")
	}

	fun clearInternalLog(view: View?) {
		TextFileManager.getDebugLogFile().deleteSafely()
	}

	fun getKeyFile(view: View?) {
		printi("DEBUG", "key file data: " + TextFileManager.getKeyFile().read())
	}

	//network operations
	fun uploadDataFiles(view: View?) {
		PostRequest.uploadAllFiles()
	}

	fun updateDeviceSettings(view: View?) {
		SetDeviceSettings.dispatchUpdateDeviceSettings()
	}

	fun runSurveyDownload(view: View?) {
		SurveyDownloader.downloadSurveys(applicationContext, null)
	}

	fun buttonStartTimer(view: View?) {
		mainService!!.startTimers()
	}

	//file operations
	fun makeNewFiles(view: View?) {
		TextFileManager.makeNewFilesForEverything()
	}

	fun deleteEverything(view: View?) {
		printi("Delete Everything button pressed", "poke.")
		val files = TextFileManager.getAllFiles()
		Arrays.sort(files)
		for (file in files) {
			printi("files...", file)
		}
		TextFileManager.deleteEverything()
	}

	fun listFiles(view: View?) {
		val prefix = Thread.currentThread().id.toString() + " - "
		printw("files...", "${prefix}UPLOADABLE FILES")
		var files = TextFileManager.getAllUploadableFiles()
		Arrays.sort(files)

		for (file in files) {
			var len = this.applicationContext.getFileStreamPath(file).length()
			printi("files...", "${prefix}${file} ${len}B")
		}

		printw("files...", "${prefix}ALL FILES")
		files = TextFileManager.getAllFiles()
		Arrays.sort(files)

		for (file in files) {
			var len = this.applicationContext.getFileStreamPath(file).length()
			printi("files...", "${prefix}${file} ${len}B")
		}
	}

	//ui operations
	fun loadMainMenu(view: View?) {
		// this is correct, we do not want to include extra flags here.
		startActivity(Intent(this.applicationContext, MainMenuActivity::class.java))
	}

	fun popSurveyNotifications(view: View?) {
		for (surveyId in PersistentData.getSurveyIds()) {
			SurveyNotifications.displaySurveyNotification(this.applicationContext, surveyId)
		}
	}

	//crash operations (No, really, we actually need this.)
	fun crashUi(view: View?) {
		throw NullPointerException("oops, you bwoke it.")
	}

	fun crashBackground(view: View?) {
		MainService.timer!!.setupExactSingleAlarm(0.toLong(), Intent("crashBeiwe"))
	}

	fun crashBackgroundInFive(view: View?) {
		MainService.timer!!.setupExactSingleAlarm(5000.toLong(), Intent("crashBeiwe"))
	}

	fun enterANRUI(view: View?) {
		try {
			Thread.sleep(100000)
		} catch (ie: InterruptedException) {
			ie.printStackTrace()
		}
	}

	fun enterANRBackground(view: View?) {
		MainService.timer!!.setupExactSingleAlarm(0.toLong(), Intent("enterANR"))
	}

	fun stopBackgroundService(view: View?) {
		mainService!!.stop()
	}

	fun testManualErrorReport(view: View?) {
		try {
			throw NullPointerException("this is a test null pointer exception from the debug interface")
		} catch (e: Exception) {
			CrashHandler.writeCrashlog(e, applicationContext)
		}
	}

	//runs tests on the json logic parser
	fun testJsonLogicParser(view: View?) {
		val JsonQuestionsListString = "[{\"question_text\": \"In the last 7 days, how OFTEN did you EAT BROCCOLI?\", \"question_type\": \"radio_button\", \"answers\": [{\"text\": \"Never\"}, {\"text\": \"Rarely\"}, {\"text\": \"Occasionally\"}, {\"text\": \"Frequently\"}, {\"text\": \"Almost Constantly\"}], \"question_id\": \"6695d6c4-916b-4225-8688-89b6089a24d1\"}, {\"display_if\": {\">\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 0]}, \"question_text\": \"In the last 7 days, what was the SEVERITY of your CRAVING FOR BROCCOLI?\", \"question_type\": \"radio_button\", \"answers\": [{\"text\": \"None\"}, {\"text\": \"Mild\"}, {\"text\": \"Moderate\"}, {\"text\": \"Severe\"}, {\"text\": \"Very Severe\"}], \"question_id\": \"41d54793-dc4d-48d9-f370-4329a7bc6960\"}, {\"display_if\": {\"and\": [{\">\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 0]}, {\">\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 0]}]}, \"question_text\": \"In the last 7 days, how much did your CRAVING FOR BROCCOLI INTERFERE with your usual or daily activities, (e.g. eating cauliflower)?\", \"question_type\": \"radio_button\", \"answers\": [{\"text\": \"Not at all\"}, {\"text\": \"A little bit\"}, {\"text\": \"Somewhat\"}, {\"text\": \"Quite a bit\"}, {\"text\": \"Very much\"}], \"question_id\": \"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\"}, {\"display_if\": {\"or\": [{\"and\": [{\"<=\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 3]}, {\"==\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 2]}, {\"<\": [\"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\", 3]}]}, {\"and\": [{\"<=\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 3]}, {\"<\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 3]}, {\"==\": [\"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\", 2]}]}, {\"and\": [{\"==\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 4]}, {\"<=\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 1]}, {\"<=\": [\"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\", 1]}]}]}, \"question_text\": \"While broccoli is a nutritious and healthful food, it's important to recognize that craving too much broccoli can have adverse consequences on your health.  If in a single day you find yourself eating broccoli steamed, stir-fried, and raw with a 'vegetable dip', you may be a broccoli addict.  This is an additional paragraph (following a double newline) warning you about the dangers of broccoli consumption.\", \"question_type\": \"info_text_box\", \"question_id\": \"9d7f737d-ef55-4231-e901-b3b68ca74190\"}, {\"display_if\": {\"or\": [{\"and\": [{\"==\": [\"6695d6c4-916b-4225-8688-89b6089a24d1\", 4]}, {\"or\": [{\">=\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 2]}, {\">=\": [\"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\", 2]}]}]}, {\"or\": [{\">=\": [\"41d54793-dc4d-48d9-f370-4329a7bc6960\", 3]}, {\">=\": [\"5cfa06ad-d907-4ba7-a66a-d68ea3c89fba\", 3]}]}]}, \"question_text\": \"OK, it sounds like your broccoli habit is getting out of hand.  Please call your clinician immediately.\", \"question_type\": \"info_text_box\", \"question_id\": \"59f05c45-df67-40ed-a299-8796118ad173\"}, {\"question_text\": \"How many pounds of broccoli per day could a woodchuck chuck if a woodchuck could chuck broccoli?\", \"text_field_type\": \"NUMERIC\", \"question_type\": \"free_response\", \"question_id\": \"9745551b-a0f8-4eec-9205-9e0154637513\"}, {\"display_if\": {\"<\": [\"9745551b-a0f8-4eec-9205-9e0154637513\", 10]}, \"question_text\": \"That seems a little low.\", \"question_type\": \"info_text_box\", \"question_id\": \"cedef218-e1ec-46d3-d8be-e30cb0b2d3aa\"}, {\"display_if\": {\"==\": [\"9745551b-a0f8-4eec-9205-9e0154637513\", 10]}, \"question_text\": \"That sounds about right.\", \"question_type\": \"info_text_box\", \"question_id\": \"64a2a19b-c3d0-4d6e-9c0d-06089fd00424\"}, {\"display_if\": {\">\": [\"9745551b-a0f8-4eec-9205-9e0154637513\", 10]}, \"question_text\": \"What?! No way- that's way too high!\", \"question_type\": \"info_text_box\", \"question_id\": \"166d74ea-af32-487c-96d6-da8d63cfd368\"}, {\"max\": \"5\", \"question_id\": \"059e2f4a-562a-498e-d5f3-f59a2b2a5a5b\", \"question_text\": \"On a scale of 1 (awful) to 5 (delicious) stars, how would you rate your dinner at Chez Broccoli Restaurant?\", \"question_type\": \"slider\", \"min\": \"1\"}, {\"display_if\": {\">=\": [\"059e2f4a-562a-498e-d5f3-f59a2b2a5a5b\", 4]}, \"question_text\": \"Wow, you are a true broccoli fan.\", \"question_type\": \"info_text_box\", \"question_id\": \"6dd9b20b-9dfc-4ec9-cd29-1b82b330b463\"}, {\"question_text\": \"THE END. This survey is over.\", \"question_type\": \"info_text_box\", \"question_id\": \"ec0173c9-ac8d-449d-d11d-1d8e596b4ec9\"}]"
		val steve: JsonSkipLogic
		val questions: JSONArray
		val runDisplayLogic = true
		try {
			questions = JSONArray(JsonQuestionsListString)
			steve = JsonSkipLogic(questions, runDisplayLogic, applicationContext)
		} catch (e: JSONException) {
			printe("Debug", "it dun gon wronge.")
			e.printStackTrace()
			throw NullPointerException("it done gon wronge")
		}
		var i = 0
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
		i++
		steve.nextQuestion()
		printv("debug", "" + i)
	}

	fun sendTestNotification(view: View?) {
		PostRequest.sendToTestNotificationEndpoint()
	}

	fun sendSurveyNotification(view: View?) {
		PostRequest.sendToSurveyNotificationEndpoint()
	}

	fun clearNotifications(view: View?) {
		for (surveyId in PersistentData.getSurveyIds())
			SurveyNotifications.dismissSurveyNotification(this.applicationContext, surveyId)
	}
}

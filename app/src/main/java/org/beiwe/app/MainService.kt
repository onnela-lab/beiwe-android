package org.beiwe.app

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.dsn.InvalidDsnException
import org.beiwe.app.PermissionHandler.checkBluetoothPermissions
import org.beiwe.app.PermissionHandler.confirmBluetooth
import org.beiwe.app.PermissionHandler.confirmCallLogging
import org.beiwe.app.PermissionHandler.confirmTexts
import org.beiwe.app.listeners.*
import org.beiwe.app.networking.PostRequest
import org.beiwe.app.networking.SurveyDownloader
import org.beiwe.app.storage.*
import org.beiwe.app.survey.SurveyScheduler
import org.beiwe.app.ui.user.LoginActivity
import org.beiwe.app.ui.utils.SurveyNotifications.displaySurveyNotification
import org.beiwe.app.ui.utils.SurveyNotifications.isNotificationActive
import java.util.*

// Notification Channel constants
const val NOTIFICATION_CHANNEL_ID = "_service_channel"
const val NOTIFICATION_CHANNEL_NAME = "Beiwe Data Collection" // user facing name, seen if they hold press the notification

// Timer constants
const val FCM_TIMER = 1000L * 60 * 30 // 30 minutes between sending fcm checkins
const val DEVICE_SETTINGS_UPDATE_PERIODICITY = 1000L * 60 * 30  // 30 between checking for updated device settings updates
const val HEARTBEAT_TIMER = 1000L * 60 * 5 // 5 minutes between sending heartbeats

// set our repeating timers to 30 seconds (threadhandler is offset by half the period)
const val FOREGROUND_SERVICE_RESTART_PERIODICITY = 1000L * 30
const val THREADHANDLER_PERIODICITY = 1000L * 30

// 6 hours for a foreground service notification, this is JUST the notification, not the service itself.
const val FOREGROUND_SERVICE_NOTIFICATION_TIMER = 1000L * 60 * 60 * 6

const val BLLUETOOTH_MESSAGE_1 = "bluetooth Failure, device should not have gotten to this line of code"
const val BLLUETOOTH_MESSAGE_2 = "Device does not support bluetooth LE, bluetooth features disabled."
const val FCM_ERROR_MESSAGE = "Unable to get FCM token, will not be able to receive push notifications."

// We were getting some weird null intent exceptions with invalid stack traces pointing to line 2 of
// this file. Line 2 is either an import, a package declaration, an error suppression annotation, or
// blank. All overridden functions in this file that have non-primitive variables (usually intents)
// must be declared as optional and handle that null case (usually we ignore it).

class MainService : Service() {
	// the various listeners for sensor data
	var gpsListener: GPSListener? = null
	var powerStateListener: PowerStateListener? = null
	var accelerometerListener: AccelerometerListener? = null
	var gyroscopeListener: GyroscopeListener? = null
	var bluetoothListener: BluetoothListener? = null

	// these assets don't require android assets, they can go in the common init.
	val background_handlerThread = HandlerThread("background_handler_thread")
	var background_handler: Handler
	var background_looper: Looper
	var hasInitializedOnce = false

	init {
		background_handlerThread.start()
		background_looper = background_handlerThread.looper
		background_handler = Handler(background_looper)
	}

	/*##############################################################################################
	##############################           App Core Setup              ###########################
	##############################################################################################*/

	/** onCreate is essentially the constructor for the service, initialize variables here.  */
	override fun onCreate() {
		localHandle = this // yes yes, gross, I know. must instantiate before registerTimers()

		try {
			val sentryDsn = BuildConfig.SENTRY_DSN
			Sentry.init(sentryDsn, AndroidSentryClientFactory(applicationContext))
		} catch (ie: InvalidDsnException) {
			Sentry.init(AndroidSentryClientFactory(applicationContext))
		}

		// report errors from the service to sentry only when this is not the development build
		if (!BuildConfig.APP_IS_DEV)
			Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

		// Accessing a survey requires thhe user opening the app or an activet survey notification,
		// which means the background service is always running before that point, even in the
		// corner case of when the background starts an system-on.

		PersistentData.initialize(applicationContext)
		PersistentData.setNotTakingSurvey()

		// Initialize everything that is necessary for the app!
		initializeFireBaseIDToken()
		TextFileManager.initialize(applicationContext)
		PostRequest.initialize(applicationContext)
		registerTimers(applicationContext)
		createNotificationChannel()
		doSetup()

		// dispatch the ThreadHandler based run_all_app_logic call with a 1/2 duration offset.
		background_handler.postDelayed(periodic_run_app_logic, THREADHANDLER_PERIODICITY / 2)
		val start_time = Date(System.currentTimeMillis()).toLocaleString()
		PersistentData.appOnServiceStart = start_time
		if (!this.hasInitializedOnce) {
			PersistentData.appOnServiceStartFirstRun = start_time
			this.hasInitializedOnce = true
		}
	}

	// namespace hack, see comment
	fun get_periodic_run_app_logic(): () -> Unit = periodic_run_app_logic
	val periodic_run_app_logic: () -> Unit = {
		// printd("run_all_app_logic - ThreadHandler")
		run_all_app_logic()
		// in the scope of this closure "periodic_run_app_logic" doesn't exist, we need to access it, not referency it.
		background_handler.postDelayed(get_periodic_run_app_logic(), THREADHANDLER_PERIODICITY)
	}

	fun doSetup() {
		// Accelerometer, gyro, power state, and wifi don't need permissions or they are checked in
		// the broadcastreceiver logic
		startPowerStateListener()
		gpsListener = GPSListener(applicationContext)
		WifiListener.initialize(applicationContext)

		if (PersistentData.getAccelerometerEnabled())
			accelerometerListener = AccelerometerListener(applicationContext)
		if (PersistentData.getGyroscopeEnabled())
			gyroscopeListener = GyroscopeListener(applicationContext)

		// Bluetooth, wifi, gps, calls, and texts need permissions
		if (confirmBluetooth(applicationContext))
			startBluetooth()

		if (confirmTexts(applicationContext)) {
			startSmsSentLogger()
			startMmsSentLogger()
		} else if (PersistentData.getTextsEnabled()) {
			sendBroadcast(Timer.checkForSMSEnabledIntent)
		}
		if (confirmCallLogging(applicationContext))
			startCallLogger()
		else if (PersistentData.getCallLoggingEnabled())
			sendBroadcast(Timer.checkForCallsEnabledIntent)

		// Only do the following if the device is registered
		if (PersistentData.getIsRegistered()) {
			DeviceInfo.initialize(applicationContext) // if at registration this has already been initialized. (we don't care.)
			startTimers()
		}
	}

	private fun createNotificationChannel() {
		// setup the notification channel so the service can run in the foreground
		val chan = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
		chan.lightColor = Color.BLUE
		chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
		chan.setSound(null, null)
		val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		manager.createNotificationChannel(chan)
	}

	/*#############################################################################
	#########################         Starters              #######################
	#############################################################################*/

	/** Initializes the Bluetooth listener
	 * Bluetooth has several checks to make sure that it actually exists on the device with the
	 * capabilities we need. Checking for Bluetooth LE is necessary because it is an optional
	 * extension to Bluetooth 4.0. */
	fun startBluetooth() {
		// Note: the Bluetooth listener is a BroadcastReceiver, which means it must have a 0-argument
		// constructor in order for android to instantiate it on broadcast receipts. The following
		// check must be made, but it requires a Context that we cannot pass into the
		// BluetoothListener, so we do the check in the BackgroundService.
		if (applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
				&& PersistentData.getBluetoothEnabled()) {
			bluetoothListener = BluetoothListener()
			if (bluetoothListener!!.isBluetoothEnabled) {
				val intent_filter = IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED")
				registerReceiver(bluetoothListener, intent_filter)
			} else {
				// TODO: Track down why this error occurs, cleanup.  The above check should be for
				//  the (new) doesBluetoothCapabilityExist function instead of isBluetoothEnabled
				Log.e("Main Service", BLLUETOOTH_MESSAGE_1)
				TextFileManager.writeDebugLogStatement(BLLUETOOTH_MESSAGE_1)
			}
		} else {
			if (PersistentData.getBluetoothEnabled()) {
				TextFileManager.writeDebugLogStatement(BLLUETOOTH_MESSAGE_2)
				Log.w("MainS bluetooth init", BLLUETOOTH_MESSAGE_2)
			}
			bluetoothListener = null
		}
	}

	/** Initializes the sms logger.  */
	fun startSmsSentLogger() {
		val smsSentLogger = SmsSentLogger(Handler(), applicationContext)
		this.contentResolver.registerContentObserver(
				Uri.parse("content://sms/"), true, smsSentLogger)
	}

	fun startMmsSentLogger() {
		val mmsMonitor = MMSSentLogger(Handler(), applicationContext)
		this.contentResolver.registerContentObserver(
				Uri.parse("content://mms/"), true, mmsMonitor)
	}

	/** Initializes the call logger.  */
	private fun startCallLogger() {
		val callLogger = CallLogger(Handler(), applicationContext)
		this.contentResolver.registerContentObserver(
				Uri.parse("content://call_log/calls/"), true, callLogger)
	}

	/** Initializes the PowerStateListener.
	 *  The PowerStateListener requires the ACTION_SCREEN_OFF and ACTION_SCREEN_ON intents be
	 *  registered programatically. They do not work if registered in the app's manifest. Same for
	 *  the ACTION_POWER_SAVE_MODE_CHANGED and ACTION_DEVICE_IDLE_MODE_CHANGED filters, though they
	 *  are for monitoring deeper power state changes in 5.0 and 6.0, respectively.  */
	private fun startPowerStateListener() {
		if (powerStateListener == null) {
			val filter = IntentFilter()
			filter.addAction(Intent.ACTION_SCREEN_ON)
			filter.addAction(Intent.ACTION_SCREEN_OFF)
			if (Build.VERSION.SDK_INT >= 21)
				filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
			if (Build.VERSION.SDK_INT >= 23)
				filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
			powerStateListener = PowerStateListener()
			registerReceiver(powerStateListener, filter)
			PowerStateListener.start(applicationContext)
		}
	}

	/** Gets, sets, and pushes the FCM token to the backend.  */
	fun initializeFireBaseIDToken() {
		// Set up the oncomplete listener for the FCM getter code, which in turn sets up a thread
		// that will wait until the participant is registered to actually push it off to the server.
		val fcm_closure = OnCompleteListener { task: Task<InstanceIdResult> ->
			// If the task failed, log the error and return, we will resend in the firebase
			// token-changed code, or the FCM_TIMER periodic event.
			if (!task.isSuccessful) {
				Log.e("FCM", FCM_ERROR_MESSAGE, task.exception)
				TextFileManager.writeDebugLogStatement("$FCM_ERROR_MESSAGE(1)")
				return@OnCompleteListener
			}

			// Get new Instance ID token - literally can't access task.result in blocker_closure ...?!
			val taskResult = task.result
			if (taskResult == null) {
				TextFileManager.writeDebugLogStatement("$FCM_ERROR_MESSAGE(2)")
				return@OnCompleteListener
			}

			// We need to wait until the participant is registered to send the fcm token.
			// (This is a Runnable because we need to return early in an error case with @Runnable)
			val blocker_closure = Runnable {
				while (!PersistentData.getIsRegistered()) {
					try {
						Thread.sleep(1000)
					} catch (ignored: InterruptedException) {
						TextFileManager.writeDebugLogStatement("$FCM_ERROR_MESSAGE(3)")
						return@Runnable
					}
				}
				PersistentData.setFCMInstanceID(taskResult.token)
				PostRequest.sendFCMInstanceID(taskResult.token)
			}

			// kick off the blocker thread
			Thread(blocker_closure, "fcm_blocker_thread").start()
		}

		// setup oncomplete listener
		FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener(fcm_closure)
	}

	/*#############################################################################
	####################            Timer Logic             #######################
	#############################################################################*/

	fun startTimers() {
		Log.i("BackgroundService", "running startTimer logic.")
		// printd("run_all_app_logic - startTimers")
		run_all_app_logic()

		// if Bluetooth recording is enabled and there is no scheduled next-bluetooth-enable event,
		// set up the next Bluetooth-on alarm. (Bluetooth needs to run at absolute points in time,
		// it should not be started if a scheduled event is missed.)
		if (PersistentData.getBluetoothEnabled()) {
			if (confirmBluetooth(applicationContext) && !timer!!.alarmIsSet(Timer.bluetoothOnIntent))
				timer!!.setupExactSingleAbsoluteTimeAlarm(
						PersistentData.getBluetoothTotalDuration(),
						PersistentData.getBluetoothGlobalOffset(),
						Timer.bluetoothOnIntent
				)
		}

		// this is a repeating alarm that ensures the service is running, it starts the service if it isn't.
		// Periodicity is FOREGROUND_SERVICE_RESTART_PERIODICITY.
		// This is a special intent, it has a  construction that targets the MainService's onStartCommand method.
		val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
		val restartServiceIntent = Intent(applicationContext, MainService::class.java)
		restartServiceIntent.setPackage(packageName)
		val flags = pending_intent_flag_fix(PendingIntent.FLAG_UPDATE_CURRENT)
		// no clue what this requestcode means, it is 0 on normal pending intents
		val repeatingRestartServicePendingIntent = PendingIntent.getService(
				applicationContext, 1, restartServiceIntent, flags
		)
		alarmService.setRepeating(
				AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + FOREGROUND_SERVICE_RESTART_PERIODICITY,
				FOREGROUND_SERVICE_RESTART_PERIODICITY,
				repeatingRestartServicePendingIntent
		)
	}

	/**The timerReceiver is an Android BroadcastReceiver that listens for our timer events to trigger,
	 * and then runs the appropriate code for that trigger.
	 * Note: every condition has a return statement at the end; this is because the trigger survey
	 * notification action requires a fairly expensive dive into PersistantData JSON unpacking. */
	private val timerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(applicationContext: Context, intent: Intent?) {
			Log.e("BackgroundService", "Received broadcast: $intent")
			TextFileManager.writeDebugLogStatement("Received Broadcast: " + intent.toString())

			if (intent == null)
				return

			val broadcastAction = intent.action
			// printd("run_all_app_logic - timerReceiver")
			run_all_app_logic()

			/* Bluetooth timers are unlike GPS and Accelerometer because it uses an
			 * absolute-point-in-time as a trigger, and therefore we don't need to store
			 * most-recent-timer state. The Bluetooth-on action sets the corresponding Bluetooth-off
			 * timer, the Bluetooth-off action sets the next Bluetooth-on timer.*/
			if (broadcastAction == applicationContext.getString(R.string.turn_bluetooth_on)) {
				printe("Bluetooth on timer triggered")
				if (!PersistentData.getBluetoothEnabled())  // return, don't set another alarm
					return

				if (checkBluetoothPermissions(applicationContext)) {
					if (bluetoothListener != null) bluetoothListener!!.enableBLEScan()
				} else {
					TextFileManager.writeDebugLogStatement("user has not provided permission for Bluetooth.")
				}
				timer!!.setupExactSingleAlarm(PersistentData.getBluetoothOnDuration(), Timer.bluetoothOffIntent)
				return
			}

			if (broadcastAction == applicationContext.getString(R.string.turn_bluetooth_off)) {
				printe("Bluetooth off timer triggered")
				if (checkBluetoothPermissions(applicationContext) && bluetoothListener != null) {
					bluetoothListener!!.disableBLEScan()
				}
				timer!!.setupExactSingleAbsoluteTimeAlarm(
						PersistentData.getBluetoothTotalDuration(),
						PersistentData.getBluetoothGlobalOffset(),
						Timer.bluetoothOnIntent
				)
				return
			}

			// I don't know if we pull this one out
			// Signs out the user. (does not set up a timer, that is handled in activity and sign-in logic) 
			if (broadcastAction == applicationContext.getString(R.string.signout_intent)) {
				// FIXME: does this need to run on the main thread in do_signout_check?
				PersistentData.logout()
				val loginPage = Intent(applicationContext, LoginActivity::class.java) // yup that is still java
				loginPage.flags = Intent.FLAG_ACTIVITY_NEW_TASK
				applicationContext.startActivity(loginPage)
				return
			}

			// leave the SMS/MMS/calls logic as-is, it is like this to ensure they are never
			// enabled until the particiant presses the accept button.
			if (broadcastAction == applicationContext.getString(R.string.check_for_sms_enabled)) {
				if (confirmTexts(applicationContext)) {
					startSmsSentLogger()
					startMmsSentLogger()
				} else if (PersistentData.getTextsEnabled())
					timer!!.setupExactSingleAlarm(30000L, Timer.checkForSMSEnabledIntent)
			}
			// logic for the call (metadata) logger
			if (broadcastAction == applicationContext.getString(R.string.check_for_call_log_enabled)) {
				if (confirmCallLogging(applicationContext)) {
					startCallLogger()
				} else if (PersistentData.getCallLoggingEnabled())
					timer!!.setupExactSingleAlarm(30000L, Timer.checkForCallsEnabledIntent)
			}


			// This code has been removed, the app now explicitly checks app state, and the call
			// to send this particular broadcast is no longer used. We will retain this for now
			// (June 2024) in case we had a real good reason for retaining this pattern now that
			// survey state checking.
			// checks if the action is the id of a survey (expensive), if so pop up the notification
			// for that survey, schedule the next alarm.
			// if (PersistentData.getSurveyIds().contains(broadcastAction)) {
			//     // Log.i("MAIN SERVICE", "new notification: " + broadcastAction);
			//     displaySurveyNotification(applicationContext, broadcastAction!!)
			//     SurveyScheduler.scheduleSurvey(broadcastAction)
			//     return
			// }

			// these are special actions that will only run if the app device is in debug mode.
			if (broadcastAction == "crashBeiwe" && BuildConfig.APP_IS_BETA) {
				throw NullPointerException("beeeeeoooop.")
			}
			if (broadcastAction == "enterANR" && BuildConfig.APP_IS_BETA) {
				try {
					Thread.sleep(100000)
				} catch (ie: InterruptedException) {
					ie.printStackTrace()
				}
			}
		}
	}

	/*##############################################################################
	########################## Application State Logic #############################
	##############################################################################*/

	// TODO: if we make this use rtc time that will solve time-reset issues.  Could also run a sanity check.
	// TODO: test default (should be zero? out of PersistentData) cases.
	// TODO: is there an advantage to sticking the callables onto a queue that is then consumed? leaning no.

	/* Abstract function, checks the time, runs the action, sets the next time. */
	fun do_an_event_session_check(
			now: Long,
			identifier_string: String,
			periodicity_in_milliseconds: Long,
			do_action: () -> Unit,
	) {
		// val t1 = System.currentTimeMillis()
		val most_recent_event_time = PersistentData.getMostRecentAlarmTime(identifier_string)
		if (now - most_recent_event_time > periodicity_in_milliseconds) {
			// printe("'$identifier_string' - time to trigger")
			do_action()
			PersistentData.setMostRecentAlarmTime(identifier_string, System.currentTimeMillis())
			// TODO: this purely mimicks the old behavior that was of printing the broadcast, refine it.
			TextFileManager.writeDebugLogStatement(
					"Received Broadcast: " + Timer.intent_map[identifier_string]!!.toString())

			// printv("'$identifier_string - trigger - ${System.currentTimeMillis() - t1}")
		} else {
			// printi("'$identifier_string' - not yet time to trigger")
			// printv("'$identifier_string - not trigger - ${System.currentTimeMillis() - t1}")
		}
	}

	/* Abstracted timing logic for sessions that have a duration to their recording session. All
	 * events that have a timed on-off pattern run through this logic. We check the event's current
	 * state, recorded (PersistentData) status, compare that to the current timer values for what
	 * the state SHOULD be, and also set off timers to in an attempt to get a well run_all_app_logic
	 * check. (we pad with an extra second to ensure that check hits an inflection point where
	 * action is required.) */
	fun do_an_on_off_session_check(
			now: Long,
			is_running: Boolean,
			should_turn_off_at: Long,
			should_turn_on_again_at: Long,
			identifier_string: String,
			intent_off_string: String,
			on_action: () -> Unit,
			off_action: () -> Unit,
	) {
		// val t1 = System.currentTimeMillis()
		if (is_running && now <= should_turn_off_at) {
			// printw("'$identifier_string' is running, not time to turn of")
			// printv("'$identifier_string - is running - ${System.currentTimeMillis() - t1}")
			return
		}

		// running, should be off, off is in the past
		if (is_running && should_turn_off_at < now) {
			// printi("'$identifier_string' time to turn off")
			off_action()
			val should_turn_on_again_at_safe = should_turn_on_again_at + 1000 // add a second to ensure we pass the timer
			print("setting ON TIMER for $identifier_string to $should_turn_on_again_at_safe")
			timer!!.setupSingleAlarmAt(should_turn_on_again_at_safe, Timer.intent_map[identifier_string]!!)
			// printv("'$identifier_string - turn off - ${System.currentTimeMillis() - t1}")
		}

		// not_running, should turn on is still in the future, do nothing
		if (!is_running && should_turn_on_again_at >= now) {
			// printw("'$identifier_string' correctly off")
			// printv("'$identifier_string - correctly off - ${System.currentTimeMillis() - t1}")
			return
		}

		// not running, should be on, (on is in the past)
		if (!is_running && should_turn_on_again_at < now) {
			// always get the current time, the now value could be stale - unlikely but possible we
			// care that we get data, not that data be rigidly accurate to a clock.
			PersistentData.setMostRecentAlarmTime(identifier_string, System.currentTimeMillis())
			// printe("'$identifier_string' turn it on!")
			on_action()
			val should_turn_off_at_safe = should_turn_off_at + 1000  // add a second to ensure we pass the timer
			print("setting OFF TIMER for $identifier_string to $should_turn_off_at_safe")
			timer!!.setupSingleAlarmAt(should_turn_off_at_safe, Timer.intent_map[intent_off_string]!!)
			// printv("'$identifier_string - on action - ${System.currentTimeMillis() - t1}")
		}
	}

	/* If there is something with app state logic that should be idempotently checked, stick it
	 * here. Returns the value used for the current time.  @Synchronized because this is the core
	 * logic loop.  Provided potentially-expensive operations, like upload logic, run on anynchronously
	 * and with reasonably widely separated real-time values, */
	@Synchronized
	fun run_all_app_logic(): Long {
		PersistentData.appOnRunAllLogic = Date(System.currentTimeMillis()).toLocaleString()

		val now = System.currentTimeMillis()
		// ALL of these actions wait until the participant is registered
		if (!PersistentData.getIsRegistered())
			return now

		// These are currently syncronous (block) unless they say otherwise, profiling was done
		// on a Pixel 6. No-action always measures 0-1ms.
		do_new_files_check(now)  // always 10s of ms (30-70ms)
		do_heartbeat_check(now)  // always 10s of ms (30-70ms)
		accelerometer_logic(now)
		gyro_logic(now)  // on action ~20-50ms, off action 10-20ms
		gps_logic(now)  // on acction <10-20ms, off action ~2ms (yes two)
		do_fcm_upload_logic_check(now)  // asynchronous, runs network request on a thread.
		do_wifi_logic_check(now)  // on action <10-40ms
		do_upload_logic_check(now)  // asynchronous, runs network request on a thread, single digit ms.
		do_new_surveys_check(now)  // asynchronous, runs network request on a thread, single digit ms.
		do_new_device_settings_check(now) // asynchronous, runs network request on a thread, single digit ms.
		do_survey_notifications_check(now)  // 1 survey notification <10-30ms.
		// highest total time was 159ms, but insufficient data points to be confident.
		// printd("run_all_app_logic total time - ${System.currentTimeMillis() - now}")
		return now
	}

	fun accelerometer_logic(now: Long) {
		// accelerometer may not exist, or be disabled for the study, but it does not require permissions.
		if (!PersistentData.getAccelerometerEnabled() || !accelerometerListener!!.exists)
			return

		// assemble all the variables we need for on-off with duration
		val on_string = getString(R.string.turn_accelerometer_on)
		val off_string = getString(R.string.turn_accelerometer_off)
		val most_recent_on = PersistentData.getMostRecentAlarmTime(on_string)
		val should_turn_off_at = most_recent_on + PersistentData.getAccelerometerOnDuration()
		val should_turn_on_again_at = should_turn_off_at + PersistentData.getAccelerometerOffDuration()
		do_an_on_off_session_check(
				now,
				accelerometerListener!!.running,
				should_turn_off_at,
				should_turn_on_again_at,
				on_string,
				off_string,
				accelerometerListener!!.accelerometer_on_action,
				accelerometerListener!!.accelerometer_off_action
		)
	}

	fun gyro_logic(now: Long) {
		// gyro may not exist, or be disabled for the study, but it does not require permissions.
		if (!PersistentData.getGyroscopeEnabled() || !gyroscopeListener!!.exists)
			return

		// assemble all the variables we need for on-off with duration
		val on_string = getString(R.string.turn_gyroscope_on)
		val off_string = getString(R.string.turn_gyroscope_off)
		val most_recent_on = PersistentData.getMostRecentAlarmTime(on_string)
		val should_turn_off_at = most_recent_on + PersistentData.getGyroscopeOnDuration()
		val should_turn_on_again_at = should_turn_off_at + PersistentData.getGyroscopeOffDuration()
		do_an_on_off_session_check(
				now,
				gyroscopeListener!!.running,
				should_turn_off_at,
				should_turn_on_again_at,
				on_string,
				off_string,
				gyroscopeListener!!.gyro_on_action,
				gyroscopeListener!!.gyro_off_action
		)
	}

	fun gps_logic(now: Long) {
		// GPS (location service) always _exists to a degree_, checked inside the gpsListener,
		//  but may not be enabled on a study.  GPS requires permissions.
		if (!PermissionHandler.confirmGps(applicationContext))
			return

		// assemble all the variables we need for on-off with duration
		val on_string = getString(R.string.turn_gps_on)
		val off_string = getString(R.string.turn_gps_off)
		val most_recent_on = PersistentData.getMostRecentAlarmTime(on_string)
		val should_turn_off_at = most_recent_on + PersistentData.getGpsOnDuration()
		val should_turn_on_again_at = should_turn_off_at + PersistentData.getGpsOffDuration()
		do_an_on_off_session_check(
				now,
				gpsListener!!.running,
				should_turn_off_at,
				should_turn_on_again_at,
				on_string,
				off_string,
				gpsListener!!.gps_on_action,
				gpsListener!!.gps_off_action
		)
	}

	fun do_wifi_logic_check(now: Long) {
		// wifi has permissions and may be disabled on the study
		if (!PermissionHandler.confirmWifi(applicationContext))
			return

		val event_string = getString(R.string.run_wifi_log)
		val event_frequency = PersistentData.getWifiLogFrequency()
		val wifi_do_action = {  // wifi will need some attention to convert to kotlin...
			WifiListener.scanWifi()
		}
		do_an_event_session_check(now, event_string, event_frequency, wifi_do_action)
	}

	fun do_upload_logic_check(now: Long) {
		val upload_string = applicationContext.getString(R.string.upload_data_files_intent)
		val periodicity = PersistentData.getUploadDataFilesFrequency()
		val do_uploads_action = {
			PostRequest.uploadAllFiles()
		}
		do_an_event_session_check(now, upload_string, periodicity, do_uploads_action)
	}

	fun do_fcm_upload_logic_check(now: Long) {
		// we can just literally hardcode this one, sendFcmToken is this plus a timer
		val event_string = applicationContext.getString(R.string.fcm_upload)
		val send_fcm_action = {
			val fcm_token = PersistentData.getFCMInstanceID()
			if (fcm_token != null)
				PostRequest.sendFCMInstanceID(fcm_token)
		}
		do_an_event_session_check(now, event_string, FCM_TIMER, send_fcm_action)
	}

	fun do_heartbeat_check(now: Long) {
		val event_string = getString(R.string.heartbeat_intent)
		val periodicity = HEARTBEAT_TIMER
		val heartbeat_action = {
			PostRequest.sendHeartbeat()
		}
		do_an_event_session_check(now, event_string, periodicity, heartbeat_action)
	}

	fun do_new_files_check(now: Long) {
		val event_string = getString(R.string.create_new_data_files_intent)
		val periodicity = PersistentData.getCreateNewDataFilesFrequency()
		val new_files_action = {
			TextFileManager.makeNewFilesForEverything()
		}
		do_an_event_session_check(now, event_string, periodicity, new_files_action)
	}

	fun do_new_surveys_check(now: Long) {
		val event_string = getString(R.string.check_for_new_surveys_intent)
		val periodicity = PersistentData.getCheckForNewSurveysFrequency()
		val dowwnload_surveys_action = {
			SurveyDownloader.downloadSurveys(applicationContext, null)
		}
		do_an_event_session_check(now, event_string, periodicity, dowwnload_surveys_action)
	}

	fun do_new_device_settings_check(now: Long) {
		val event_string = getString(R.string.check_for_new_device_settings_intent)
		val download_device_settings_action = {
			SetDeviceSettings.dispatchUpdateDeviceSettings()
		}
		do_an_event_session_check(now, event_string, DEVICE_SETTINGS_UPDATE_PERIODICITY, download_device_settings_action)
	}

	/** Checks for the current expected state for survey notifications, and the app state for
	 *  scheduled alarms. */
	fun do_survey_notifications_check(now: Long) {
		// val t1 = System.currentTimeMillis()
		// var counter = 0
		for (surveyId in PersistentData.getSurveyIds()) {
			var app_state_says_on = PersistentData.getSurveyNotificationState(surveyId)
			var alarm_in_past = PersistentData.getMostRecentSurveyAlarmTime(surveyId) < now

			// the behavior is that it ... replaces the notification.
			if ((app_state_says_on || alarm_in_past) && !isNotificationActive(applicationContext, surveyId)) {
				// this calls PersistentData.setSurveyNotificationState
				displaySurveyNotification(applicationContext, surveyId)
				// counter++
			}

			// TODO: fix this naming mismatch.
			// Never call this:
			//   timer!!.cancelAlarm(Intent(surveyId))  // BAD!
			// setMostRecentSurveyAlarmTime is called in Timer.setupSurveyAlarm (when the alarm
			// is set in the scheduler logic), e.g. the application state is updated to say that
			// the _next_ survey alarm time is at X o'clock - there is a naming mismatch.

			// if there is no alarm set for this survey, set it.
			if (!timer!!.alarmIsSet(Intent(surveyId)))
				SurveyScheduler.scheduleSurvey(surveyId)
		}
		// printv("$counter survey notifications took ${System.currentTimeMillis() - t1} ms")
	}

	/*##########################################################################################
	############## code related to onStartCommand and binding to an activity ###################
	##########################################################################################*/

	override fun onBind(arg0: Intent?): IBinder? {
		return BackgroundServiceBinder()
	}

	/**A public "Binder" class for Activities to access.
	 *  Provides a (safe) handle to the Main Service using the onStartCommand code used in every
	 *  RunningBackgroundServiceActivity  */
	inner class BackgroundServiceBinder : Binder() {
		val service: MainService
			get() = this@MainService
	}

	/*##############################################################################
	########################## Android Service Lifecycle ###########################
	##############################################################################*/

	/** The BackgroundService is meant to be all the time, so we return START_STICKY  */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		// Log.d("BackgroundService onStartCommand", "started with flag " + flags );
		TextFileManager.writeDebugLogStatement(
				System.currentTimeMillis().toString() + " started with flag " + flags)
		val now = System.currentTimeMillis()
		val millisecondsSincePrevious = now - foregroundServiceLastStarted

		// if it has been FOREGROUND_SERVICE_TIMER or longer since the last time we started the
		// foreground service notification, start it again.
		if (foregroundServiceLastStarted == 0L || millisecondsSincePrevious > FOREGROUND_SERVICE_NOTIFICATION_TIMER) {
			val intent_to_start_foreground_service = Intent(applicationContext, MainService::class.java)
			val intent_flags = pending_intent_flag_fix(PendingIntent.FLAG_UPDATE_CURRENT) // no flags
			val onStartCommandPendingIntent = PendingIntent.getService(
					applicationContext, 0, intent_to_start_foreground_service, intent_flags
			)
			val notification = Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
					.setContentTitle("Beiwe App")
					.setContentText("Beiwe data collection running")
					.setSmallIcon(R.mipmap.ic_launcher)
					.setContentIntent(onStartCommandPendingIntent)
					.setTicker("Beiwe data collection running in the background, no action required")
					.build()

			// multiple sources recommend an ID of 1 because it works. documentation is very spotty about this
			startForeground(1, notification)
			foregroundServiceLastStarted = now
		}
		
		PersistentData.serviceStartCommand = Date(System.currentTimeMillis()).toLocaleString()

		// onStartCommand is called every 30 seconds due to repeating high-priority-or-whatever
		// alarms, so we will stick a core logic check here.
		// printd("run_all_app_logic - onStartCommand")
		run_all_app_logic()

		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY
		// in testing out this restarting behavior for the service it is entirely unclear if changing
		// this return will have any observable effect despite the documentation's claim that it does.
		// return START_REDELIVER_INTENT;
	}

	// the rest of these are ~identical
	override fun onTaskRemoved(rootIntent: Intent?) {
		// Log.d("BackroundService onTaskRemoved", "onTaskRemoved called with intent: " + rootIntent.toString() );
		TextFileManager.writeDebugLogStatement("onTaskRemoved called with intent: $rootIntent")
		PersistentData.serviceOnTaskRemoved = Date(System.currentTimeMillis()).toLocaleString()
		restartService()
	}

	override fun onUnbind(intent: Intent?): Boolean {
		// Log.d("BackroundService onUnbind", "onUnbind called with intent: " + intent.toString() );
		TextFileManager.writeDebugLogStatement("onUnbind called with intent: $intent")
		PersistentData.serviceOnUnbind = Date(System.currentTimeMillis()).toLocaleString()
		restartService()
		return super.onUnbind(intent)
	}

	override fun onDestroy() { // Log.w("BackgroundService", "BackgroundService was destroyed.");
		// note: this does not run when the service is killed in a task manager, OR when the stopService() function is called from debugActivity.
		TextFileManager.writeDebugLogStatement("BackgroundService was destroyed.")
		PersistentData.serviceOnDestroy = Date(System.currentTimeMillis()).toLocaleString()
		restartService()
		super.onDestroy()
	}

	override fun onLowMemory() { // Log.w("BackroundService onLowMemory", "Low memory conditions encountered");
		TextFileManager.writeDebugLogStatement("onLowMemory called.")
		PersistentData.serviceOnLowMemory = Date(System.currentTimeMillis()).toLocaleString()
		restartService()
	}

	override fun onTrimMemory(level: Int) {
		// Log.w("BackroundService onTrimMemory", "Trim memory conditions encountered");
		TextFileManager.writeDebugLogStatement("onTrimMemory called.")
		PersistentData.serviceOnTrimMemory = Date(System.currentTimeMillis()).toLocaleString()
		super.onTrimMemory(level)
	}

	/** Stops the BackgroundService instance, development tool  */
	fun stop() {
		if (BuildConfig.APP_IS_BETA)
			this.stopSelf()
	}

	/** Sets a timer that starts the service if it is not running after a half second.  */
	fun restartService() {
		val restartServiceIntent = Intent(applicationContext, this.javaClass)
		restartServiceIntent.setPackage(packageName)
		val restartServicePendingIntent = PendingIntent.getService(
				applicationContext,
				1,
				restartServiceIntent,
				pending_intent_flag_fix(PendingIntent.FLAG_ONE_SHOT)
		)
		//  kotlin port action turned this into a very weird setter syntax using [] access...
		(applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager).set(
				AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + 500,
				restartServicePendingIntent
		)
	}

	/** We sometimes need to restart the background service */
	fun exit_and_restart_background_service() {
		TextFileManager.writeDebugLogStatement("manually restarting background service")
		// if this takes more than 500ms to restart, the app will ~crash... hmm.  This is fine.
		restartService()
		System.exit(0)
	}

	// static assets
	companion object {
		// I guess we need access to this one in static contexts...
		public var timer: Timer? = null

		// localHandle is how static scopes access the currently instantiated main service.
		// It is to be used ONLY to register new surveys with the running main service, because
		// that code needs to be able to update the IntentFilters associated with timerReceiver.
		// This is Really Hacky and terrible style, but it is okay because the scheduling code can only ever
		// begin to run with an already fully instantiated main service.
		var localHandle: MainService? = null

		private var foregroundServiceLastStarted = 0L

		// FIXME: in order to make this non-static we probably need to port PostReqeuest to kotlin
		/** create timers that will trigger events throughout the program, and
		 * register the custom Intents with the controlMessageReceiver.  */
		@JvmStatic
		fun registerTimers(applicationContext: Context) {
			timer = Timer(localHandle!!)
			val filter = IntentFilter()
			filter.addAction(applicationContext.getString(R.string.turn_accelerometer_off))
			filter.addAction(applicationContext.getString(R.string.turn_accelerometer_on))
			filter.addAction(applicationContext.getString(R.string.turn_gyroscope_on))
			filter.addAction(applicationContext.getString(R.string.turn_gyroscope_off))
			filter.addAction(applicationContext.getString(R.string.turn_bluetooth_on))
			filter.addAction(applicationContext.getString(R.string.turn_bluetooth_off))
			filter.addAction(applicationContext.getString(R.string.turn_gps_on))
			filter.addAction(applicationContext.getString(R.string.turn_gps_off))
			filter.addAction(applicationContext.getString(R.string.signout_intent))
			filter.addAction(applicationContext.getString(R.string.voice_recording))
			filter.addAction(applicationContext.getString(R.string.run_wifi_log))
			filter.addAction(applicationContext.getString(R.string.upload_data_files_intent))
			filter.addAction(applicationContext.getString(R.string.create_new_data_files_intent))
			filter.addAction(applicationContext.getString(R.string.check_for_new_surveys_intent))
			filter.addAction(applicationContext.getString(R.string.check_for_sms_enabled))
			filter.addAction(applicationContext.getString(R.string.check_for_call_log_enabled))
			filter.addAction(applicationContext.getString(R.string.fcm_upload))
			filter.addAction(applicationContext.getString(R.string.check_for_new_device_settings_intent))
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
			filter.addAction("crashBeiwe")
			filter.addAction("enterANR")

			for (surveyId in PersistentData.getSurveyIds()) {
				filter.addAction(surveyId)
			}
			applicationContext.registerReceiver(localHandle!!.timerReceiver, filter)
		}

		/**Refreshes the logout timer.
		 * This function has a THEORETICAL race condition, where the BackgroundService is not fully instantiated by a session activity,
		 * in this case we log an error to the debug log, print the error, and then wait for it to crash.  In testing on a (much) older
		 * version of the app we would occasionally see the error message, but we have never (august 10 2015) actually seen the app crash
		 * inside this code.  */
		fun startAutomaticLogoutCountdownTimer() {
			if (timer == null) {
				Log.e("bacgroundService", "timer is null, BackgroundService may be about to crash, the Timer was null when the BackgroundService was supposed to be fully instantiated.")
				TextFileManager.writeDebugLogStatement("our not-quite-race-condition encountered, Timer was null when the BackgroundService was supposed to be fully instantiated")
			}
			timer!!.setupExactSingleAlarm(PersistentData.getTimeBeforeAutoLogout(), Timer.signoutIntent)
			PersistentData.loginOrRefreshLogin()
		}

		/** cancels the signout timer  */
		fun clearAutomaticLogoutCountdownTimer() {
			timer!!.cancelAlarm(Timer.signoutIntent)
		}

		/** The Timer requires the BackgroundService in order to create alarms, hook into that functionality here.  */
		@JvmStatic
		fun setSurveyAlarm(surveyId: String?, alarmTime: Calendar?) {
			timer!!.startSurveyAlarm(surveyId!!, alarmTime!!)
		}

		@JvmStatic
		fun cancelSurveyAlarm(surveyId: String?) {
			timer!!.cancelAlarm(Intent(surveyId))
		}
	}
}

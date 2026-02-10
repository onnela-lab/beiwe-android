package org.beiwe.app.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import io.sentry.dsn.InvalidDsnException
import org.beiwe.app.BuildConfig
import org.beiwe.app.MainService
import org.beiwe.app.MainService.BackgroundServiceBinder
import org.beiwe.app.R
import org.beiwe.app.RunningBackgroundServiceActivity
import org.beiwe.app.storage.EncryptionEngine
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.ui.registration.RegisterActivity
import org.beiwe.app.ui.user.MainMenuActivity
import org.beiwe.app.ui.utils.AlertsManager
import java.io.UnsupportedEncodingException
import java.security.NoSuchAlgorithmException

/**The LoadingActivity is a temporary RunningBackgroundServiceActivity (Not a SessionActivity,
 * check out those classes if you are confused) that pops up when the user opens the app.
 * This activity runs some simple checks to make sure that the device can actually run the app,
 * and then bumps the user to the correct screen (Register or MainMenu).
 *
 * note: this cannot be a SessionActvity, doing so would cause it to instantiate itself infinitely when a user is logged out.
 * @author Eli Jones, Dor Samet */

class LoadingActivity : RunningBackgroundServiceActivity() {

	/**For some reason we have to override the serviceconnection in order to call finish()
	 * (inside loadingSequence()). otherwise we can't unbind the background service.
	 * IllegalArgumentException: Service not registered: org.beiwe.app.RunningBackgroundServiceActivit...
	 * using the name mainServiceConnection2 is what works it kotlin, in java we shadow the original.  */
	private var mainServiceConnection2: ServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, binder: IBinder) {
			// Log.d("loading ServiceConnection", "Main Service Connected");
			val some_binder = binder as BackgroundServiceBinder
			mainService = some_binder.service
			loadingSequence()
		}

		override fun onServiceDisconnected(name: ComponentName) {
			// Log.d("loading ServiceConnection", "Main Service Disconnected");
			mainService = null
		}
	}

	/**onCreate - right now it just calls on checkLogin() in SessionManager, and moves the activity
	 * to the appropriate page. In the future it could hold a splash screen before redirecting activity.  */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		try {
			val sentryDsn = BuildConfig.SENTRY_DSN
			Sentry.init(sentryDsn, AndroidSentryClientFactory(applicationContext))
		} catch (ie: InvalidDsnException) {
			Sentry.init(AndroidSentryClientFactory(applicationContext))
		}
		setContentView(R.layout.activity_loading)
		if (testHashing()) {
			val startingIntent = Intent(this.applicationContext, MainService::class.java)
			startingIntent.addFlags(Intent.FLAG_FROM_BACKGROUND)
			// ContextCompat correctly handles old and new android APIs
			ContextCompat.startForegroundService(applicationContext, startingIntent)
			bindService(startingIntent, mainServiceConnection2, BIND_AUTO_CREATE)
		} else
			failureExit()
	}

	/**CHecks whether device is registered, sends user to the correct screen.  */
	private fun loadingSequence() {
		//if the device is not registered, push the user to the register activity
		if (!PersistentData.getIsRegistered()) {
			val activityIntent = Intent(this, RegisterActivity::class.java)
			activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
			startActivity(activityIntent)
		} else {
			if (BuildConfig.APP_IS_BETA) {
				// beta builds go to debug interface
				val activityIntent = Intent(applicationContext, DebugInterfaceActivity::class.java)
				activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
				startActivity(activityIntent)
			} else {
				// the normal case: registered users go to the main menu
				val activityIntent = Intent(applicationContext, MainMenuActivity::class.java)
				activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
				startActivity(activityIntent)
			}
		}

		unbindService(mainServiceConnection2)
		finish() //destroy the loading screen
	}

	/*##################################################################################
	############################### Testing Function ###################################
	##################################################################################*/

	/**Tests whether the device can run the hash algorithm the app requires
	 * @return boolean of whether hashing works */
	private fun testHashing(): Boolean {
		// Runs the unsafe hashing function and catches errors, if it catches errors.
		// The hashMAC function does not need to be tested here because it should not actually blow up.
		// The source indicates that it should not blow up.
		try {
			EncryptionEngine.unsafeHash("input")
		} catch (noSuchAlgorithm: NoSuchAlgorithmException) {
			return false
		} catch (unSupportedEncoding: UnsupportedEncodingException) {
			return false
		}
		return true
	}

	/**Displays error, then exit. */
	private fun failureExit() {
		AlertsManager.showErrorAlert(getString(R.string.invalid_device), this, 1)
	}
}
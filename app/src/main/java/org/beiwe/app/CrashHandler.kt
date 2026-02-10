package org.beiwe.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import org.beiwe.app.networking.PostRequest
import org.beiwe.app.storage.PersistentData

class CrashHandler(private val errorHandlerContext: Context) : Thread.UncaughtExceptionHandler {
	private val millisecondsUntilRestart = 500

	/** This function is where any errors that occur in any Activity that inherits RunningBackgroundServiceActivity
	 * will dump its errors.  We send them to Sentry.  */
	override fun uncaughtException(thread: Thread, exception: Throwable) {
		// print the stack trace so that it is visible in logs
		Log.w("CrashHandler Raw", "start original stacktrace")
		exception.printStackTrace()
		Log.w("CrashHandler Raw", "end original stacktrace")

		// Write that log file
		Sentry.getContext().recordBreadcrumb(
			BreadcrumbBuilder().setMessage("Attempting application restart").build()
		)
		writeCrashlog(exception, errorHandlerContext)

		// keep this line for debugging crashes in the crash handler (yup.)
		// printi("inside crashlog", "does this line happen")

		// setup to restart service
		val restartServiceIntent = Intent(errorHandlerContext, MainService::class.java)
		restartServiceIntent.setPackage(errorHandlerContext.packageName)

		val restartServicePendingIntent = PendingIntent.getService(
			errorHandlerContext, 1, restartServiceIntent, pending_intent_flag_fix(PendingIntent.FLAG_ONE_SHOT)
		)
		val alarmService = errorHandlerContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + millisecondsUntilRestart] = restartServicePendingIntent
		// exit beiwe
		Process.killProcess(Process.myPid())
		System.exit(10)
	}

	companion object {
		/**Creates a crash log file that will be uploaded at the next upload event.
		 * Also writes error to the error log so that it is visible in logcat.
		 * @param exception A Throwable (probably your error).
		 * @param context An android Context*/
		@JvmStatic
		fun writeCrashlog(exception: Throwable?, context: Context?) {
			Sentry.getContext().addTag("user_id", PersistentData.getPatientID())
			Sentry.getContext().addTag("server_url", PostRequest.addWebsitePrefix(""))
			Sentry.getContext().addTag("study_name", PersistentData.getStudyName())
			Sentry.getContext().addTag("study_id", PersistentData.getStudyID())
			Sentry.getContext().addTag("product_flavor", BuildConfig.FLAVOR)
			Sentry.capture(exception);
		}
	}

}

package org.beiwe.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.beiwe.app.storage.TextFileManager

/** Process-level app lifecycle logging.
 *
 * Android has no single "the app went to the background" callback, so we count started Activities
 * via Application.ActivityLifecycleCallbacks.  The transition 0 -> 1 started activities is the app
 * being foregrounded, 1 -> 0 is the app being backgrounded.  (Same technique as
 * ProcessLifecycleOwner, without pulling in the androidx lifecycle-process dependency.)
 *
 * There is no Application subclass in this app, so registration happens from
 * RunningBackgroundServiceActivity.onCreate (the base class of every Activity) and is idempotent.
 *
 * Statements are written to the app log with TextFileManager.writeDebugLogStatement.  On a cold
 * start the first foreground event fires before the MainService has initialized TextFileManager
 * (LoadingActivity starts the service *after* its own onStart), and getDebugLogFile() would block
 * the UI thread and then throw.  So statements are queued with their real timestamp until the
 * service exists, and flushed from RunningBackgroundServiceActivity's onServiceConnected. */
object AppLifecycleLogger : Application.ActivityLifecycleCallbacks {
    private var registered = false
    private var startedActivityCount = 0
    // set when the most recent stop was caused by a configuration change (e.g. rotation), so the
    // matching start of the recreated activity is not misreported as a foreground event.
    private var lastStopWasConfigurationChange = false
    // (timestamp, message) pairs that could not be written yet because the MainService was not up.
    private val pendingStatements = ArrayList<Pair<Long, String>>()

    @JvmStatic
    @Synchronized
    fun register(application: Application) {
        if (registered)
            return
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /** Writes a statement to the app log, or queues it if the MainService (and therefore
     * TextFileManager) is not available yet.  Runs on the main thread. */
    @JvmStatic
    @Synchronized
    fun log(message: String) {
        val now = System.currentTimeMillis()
        // MainService.onCreate assigns localHandle first and then calls TextFileManager.initialize,
        // both synchronously on the main thread, so a non-null handle means the log file exists.
        if (MainService.localHandle == null) {
            pendingStatements.add(Pair(now, message))
            printw("AppLifecycleLogger", "MainService not running, queued app log statement: $message")
            return
        }
        flushPending()
        TextFileManager.writeDebugLogStatement(now, message)
    }

    /** Writes any queued statements using the timestamps they were queued with. */
    @JvmStatic
    @Synchronized
    fun flushPending() {
        if (pendingStatements.isEmpty() || MainService.localHandle == null)
            return
        for ((timecode, message) in pendingStatements)
            TextFileManager.writeDebugLogStatement(timecode, message)
        pendingStatements.clear()
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        if (startedActivityCount == 1) {
            if (lastStopWasConfigurationChange) {
                lastStopWasConfigurationChange = false
                return
            }
            log("app_foregrounded: " + activity.localClassName)
            // the participant may have changed a permission or setting while we were backgrounded.
            logPermissionChanges(activity)
        }
    }

    /** Diffs the permissions/settings Beiwe depends on against their last known state and logs any
     * changes.  Called on every foreground transition and after in-app permission prompts. */
    @JvmStatic
    fun logPermissionChanges(activity: Activity) {
        for (statement in PermissionHandler.checkForPermissionChanges(activity.applicationContext))
            log(statement)
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount == 0) {
            if (activity.isChangingConfigurations) {
                lastStopWasConfigurationChange = true
                return
            }
            log("app_backgrounded: " + activity.localClassName)
        }
        if (startedActivityCount < 0) // defensive; only possible if a start was missed
            startedActivityCount = 0
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

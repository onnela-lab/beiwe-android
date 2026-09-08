package org.beiwe.app

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.beiwe.app.MainService.BackgroundServiceBinder
import org.beiwe.app.PermissionHandler.getBumpingPermissionMessage
import org.beiwe.app.PermissionHandler.getNextPermission
import org.beiwe.app.PermissionHandler.getNormalPermissionMessage
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.ui.user.AboutActivityLoggedOut
import java.util.Date

/**All Activities in the app extend this Activity.  It ensures that the app's key services (i.e.
 * BackgroundService, LoginManager, PostRequest, DeviceInfo, and WifiListener) are running before
 * the interface tries to interact with any of those.
 *
 * Activities that require the user to be logged in (SurveyActivity, GraphActivity,
 * AudioRecorderActivity, etc.) extend SessionActivity, which extends this.
 * Activities that do not require the user to be logged in (the login, registration, and password-
 * reset Activities) extend this activity directly.
 * Therefore all Activities have this Activity's functionality (binding the BackgroundService), but
 * the login-protected Activities have additional functionality that forces the user to log in.
 *
 * @author Eli Jones, Josh Zagorsky */
open class RunningBackgroundServiceActivity : AppCompatActivity() {
    /** The backgroundService variable is an Activity's connection to the ... BackgroundService.
     * We ensure the BackgroundService is running in the onResume call, and functionality that
     * relies on the BackgroundService is always tied to UI elements, reducing the chance of
     * a null backgroundService variable to essentially zero.  */
    @JvmField
    var mainService: MainService? = null

    // we need access to this inside mainServiceConnection
    val localClassNameHandle: String get() = this.localClassName

    /**The ServiceConnection Class is our trigger for events that rely on the BackgroundService  */
    private var mainServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
//			Log.w("ServiceConnection", "Main Service Connected")
            val some_binder = binder as BackgroundServiceBinder
            mainService = some_binder.service
            PersistentData.appOnServiceBoundActivity =
                localClassNameHandle + " " + (Date(System.currentTimeMillis()).toLocaleString())
            doBackgroundDependentTasks()
        }

        override fun onServiceDisconnected(name: ComponentName) {
//	        Log.w("ServiceConnection", "Main Service Disconnected")
            mainService = null
            PersistentData.appOnServiceUnboundActivity =
                localClassNameHandle + " " + (Date(System.currentTimeMillis()).toLocaleString())
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        if (!BuildConfig.APP_IS_DEV)
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
        PersistentData.initialize(applicationContext)
        // process-level foreground/background app log events, registration is idempotent.
        AppLifecycleLogger.register(application)

        // this.localClassName returns the subclass name.
        // oncreate timesstamp sent back to the server for debugging purposes.
        PersistentData.appOnCreateActivity =
            this.localClassName + " " + (Date(System.currentTimeMillis()).toLocaleString())
    }

    /** Override this function to do tasks on creation, but only after the Main Service has been initialized.  */
    protected open fun doBackgroundDependentTasks() {}

    /**On creation of RunningBackgroundServiceActivity we guarantee that the BackgroundService is
     * actually running, we then bind to it so we can access program resources.  */
    override fun onResume() {
        super.onResume()
        val startingIntent = Intent(this.applicationContext, MainService::class.java)
        startingIntent.addFlags(Intent.FLAG_FROM_BACKGROUND)
        // this will only start a new service if it is not already running, and check API version for appropriate version
        ContextCompat.startForegroundService(this.applicationContext, startingIntent)
        bindService(startingIntent, mainServiceConnection, BIND_AUTO_CREATE)

        // oncreate timesstamp sent back to the server for debugging purposes.
        PersistentData.appOnResumeActivity =
            this.localClassName + " " + (Date(System.currentTimeMillis()).toLocaleString())
    }

    /** disconnect BackgroundServiceConnection when the Activity closes, otherwise we have a
     * memory leak warning (and probably an actual memory leak, too).  */
    override fun onPause() {
        super.onPause()
        activityNotVisible = true
        PersistentData.appOnPauseActivity =
            this.localClassName + " " + (Date(System.currentTimeMillis()).toLocaleString())
        unbindService(mainServiceConnection)
    }

    /*####################################################################
	########################## Common UI #################################
	####################################################################*/

    /** Common UI element, the menu button. */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.logged_out_menu, menu)
        if (PersistentData.getCallClinicianButtonEnabled())
            menu.findItem(R.id.menu_call_clinician).title = PersistentData.getCallClinicianButtonText()
         else
            menu.findItem(R.id.menu_call_clinician).isVisible = false
        if (!PersistentData.getCallResearchAssistantButtonEnabled())
            menu.findItem(R.id.menu_call_research_assistant).isVisible = false
        return true
    }

    /** Common UI element, items in menu. */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.menu_about -> {
                startActivity(Intent(applicationContext, AboutActivityLoggedOut::class.java))
                true
            }
            R.id.menu_call_clinician -> {
                callClinician(null)
                true
            }
            R.id.menu_call_research_assistant -> {
                callResearchAssistant(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** sends user to phone, calls the user's clinician.  */
    fun callClinician(v: View?) {
        startPhoneCall(PersistentData.getPrimaryCareNumber())
    }

    /** sends user to phone, calls the study's research assistant.  */
    fun callResearchAssistant(v: View?) {
        startPhoneCall(PersistentData.getPasswordResetNumber())
    }

    private fun startPhoneCall(phoneNumber: String?) {
        if (phoneNumber == null || phoneNumber == "") {
            Log.e("sessionActivity", "no phone number")
            return
         }

        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            showMinimalAlertForRedirectToSettings(this,
                    getString(R.string.cant_make_a_phone_call_permissions_alert),
                    getString(R.string.cant_make_phone_call_alert_title),
                    0)
        }
    }

    open val isAudioRecorderActivity: Boolean?
        get() = false

    private fun goToSettings(permissionIdentifier: Int?) {
        // Log.i("sessionActivity", "goToSettings");
        val myAppSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
        myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityForResult(myAppSettings, permissionIdentifier!!)
    }

    @TargetApi(23)
    private fun goToPowerSettings(powerCallbackIdentifier: Int) {
        // Log.i("sessionActivity", "goToSettings");
        @SuppressLint("BatteryLife") val powerSettings = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        powerSettings.addCategory(Intent.CATEGORY_DEFAULT)
        powerSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityForResult(powerSettings, powerCallbackIdentifier)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Log.i("sessionActivity", "onActivityResult. requestCode: " + requestCode + ", resultCode: " + resultCode );
        aboutToResetFalseActivityReturn = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Log.i("sessionActivity", "onRequestPermissionResult");
        // the system permission dialog does not stop this activity, so the foreground-transition
        // permission check in AppLifecycleLogger would otherwise miss grants made through it.
        AppLifecycleLogger.logPermissionChanges(this)
        if (!activityNotVisible) checkPermissionsLogic()
    }

    protected fun checkPermissionsLogic() {
        // gets called as part of onResume.
        activityNotVisible = false
        // Log.i("sessionactivity", "checkPermissionsLogic");
        // Log.i("sessionActivity", "prePromptActive: " + prePromptActive);
        // Log.i("sessionActivity", "postPromptActive: " + postPromptActive);
        // Log.i("sessionActivity", "thisResumeCausedByFalseActivityReturn: " + thisResumeCausedByFalseActivityReturn);
        // Log.i("sessionActivity", "aboutToResetFalseActivityReturn: " + aboutToResetFalseActivityReturn);
        if (aboutToResetFalseActivityReturn) {
            aboutToResetFalseActivityReturn = false
            thisResumeCausedByFalseActivityReturn = false
            return
        }
        if (!thisResumeCausedByFalseActivityReturn) {
            val permission = getNextPermission(applicationContext, isAudioRecorderActivity!!)
            if (permission == null || prePromptActive || postPromptActive)
                return

            if (!powerPromptActive) {
                if (permission == PermissionHandler.POWER_EXCEPTION_PERMISSION) {
                    showPowerManagementAlert(this, getString(R.string.power_management_exception_alert), 1000)
                    return
                }
                // Log.d("sessionActivity", "shouldShowRequestPermissionRationale "+ permission +": " + shouldShowRequestPermissionRationale( permission ) );

                //if the user has declined this permission before, redirect them to the settings page instead of sending another request for the notification
                if (PersistentData.getLastRequestedPermission() == permission || shouldShowRequestPermissionRationale(permission))
                    showAlertThatForcesUserToGrantPermission(
                            this,
                            getBumpingPermissionMessage(permission, applicationContext),
                            PermissionHandler.permissionMessages[permission])
                else showRegularPermissionAlert(
                        this,
                        getNormalPermissionMessage(permission, applicationContext),
                        permission,
                        PermissionHandler.permissionMessages[permission])
                PersistentData.setLastRequestedPermission(permission)
            }
        }
    }

    companion object {
        /*####################################################################
	    ###################### Permission Prompting ##########################
	    ####################################################################*/
        private var prePromptActive = false
        private var postPromptActive = false
        private var powerPromptActive = false
        private var thisResumeCausedByFalseActivityReturn = false
        private var aboutToResetFalseActivityReturn = false
        private var activityNotVisible = false

        //the following 'alert' functions all send simple popup messages to the user related to getting necessary app permissions
        //the showRegularPermissionAlert function prompts with a message, and then sends the system request for the permission
        fun showRegularPermissionAlert(activity: RunningBackgroundServiceActivity,
                                       message: String?, permission: String, permissionCallback: Int?) {
            // Log.i("sessionActivity", "showPreAlert");
            if (prePromptActive)
                return
            prePromptActive = true
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.permissions_alert_title))
            builder.setMessage(message)
            builder.setOnDismissListener {
                activity.requestPermissions(arrayOf(permission), permissionCallback!!)
                prePromptActive = false
            }
            builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text)) { arg0, arg1 -> } //Okay button
            builder.create().show()
        }

        //the showAlertThatForcesUserToGrantPermission function assumes the user has already declined the permission, and redirects them to the system settings page for this app
        fun showAlertThatForcesUserToGrantPermission(activity: RunningBackgroundServiceActivity,
                                                     message: String?, permissionCallback: Int?) {
            // Log.i("sessionActivity", "showPostAlert");
            if (postPromptActive) {
                return
            }
            postPromptActive = true
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.permissions_alert_title))
            builder.setMessage(message)
            builder.setOnDismissListener {
                thisResumeCausedByFalseActivityReturn = true
                activity.goToSettings(permissionCallback)
                postPromptActive = false
            }
            builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text)) { arg0, arg1 -> } //Okay button
            builder.create().show()
        }

        //this is called inside of startPhoneCall function only and is used to direct users to the settings page without interfering with the checkPermissionLogic function
        fun showMinimalAlertForRedirectToSettings(activity: RunningBackgroundServiceActivity,
                                                  message: String?, title: String?, permissionCallback: Int?) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(activity.getString(R.string.go_to_settings_button)) { dialog, arg1 -> activity.goToSettings(permissionCallback) }
            builder.setNegativeButton(activity.getString(R.string.alert_cancel_button_text)) { dialog, arg1 -> }
            builder.create().show()
        }

        fun showPowerManagementAlert(activity: RunningBackgroundServiceActivity,
                                     message: String?, powerCallbackIdentifier: Int) {
            Log.i("sessionActivity", "power alert")
            if (powerPromptActive)
                return
            powerPromptActive = true
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.permissions_alert_title))
            builder.setMessage(message)
            builder.setOnDismissListener {
                Log.d("power management alert", "bumping")
                thisResumeCausedByFalseActivityReturn = true
                activity.goToPowerSettings(powerCallbackIdentifier)
                powerPromptActive = false
            }
            builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text)) { arg0, arg1 -> } //Okay button
            builder.create().show()
        }
    }
}
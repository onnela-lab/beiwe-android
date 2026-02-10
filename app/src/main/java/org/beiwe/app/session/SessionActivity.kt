package org.beiwe.app.session

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import org.beiwe.app.MainService
import org.beiwe.app.R
import org.beiwe.app.RunningBackgroundServiceActivity
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.ui.registration.ResetPasswordActivity
import org.beiwe.app.ui.user.AboutActivityLoggedIn
import org.beiwe.app.ui.user.GraphActivity
import org.beiwe.app.ui.user.LoginActivity
import org.beiwe.app.ui.user.MainMenuActivity

/**All Activities in the app WHICH REQUIRE THE USER TO BE LOGGED IN extend this Activity.
 * If the user is not logged in, he/she is bumped to a login screen.
 * This Activity also extends RunningBackgroundServiceActivity, which makes the app's key
 * services run before the interface is allowed to interact with it.
 * @author Eli Jones, Josh Zagorsky */
open class SessionActivity : RunningBackgroundServiceActivity() {
	/*####################################################################
	########################## Log-in Logic ##############################
	####################################################################*/
	/** when onResume is called we need to authenticate the user and
	 * bump them to the login screen if they have timed out.  */
	override fun onResume() {
		super.onResume()
		PersistentData.initialize(applicationContext) // this function has been rewritten to efficiently handle getting called too much.  Don't worry about it.
		checkPermissionsLogic()
	}

	/** When onPause is called we need to set the timeout.  */
	override fun onPause() {
		super.onPause()
		if (mainService != null) {
			//If an activity is active there is a countdown to bump a user to a login screen after
			// some amount of time (setting is pushed by study).  If we leave the session activity
			// we need to cancel that action.
			//This issue has occurred literally once ever (as of February 27 2016) but the prior
			// behavior was broken and caused the app to crash.  Really, this state is incomprehensible
			// (activity is open an mature enough that onPause can occur, yet the main service
			// has not started?) so a crash does at least reboot Beiwe into a functional state,
			// but that obviously has its own problems.  Updated code should merely be bad UX as
			// a user could possibly get bumped to the login screen from another app.
			MainService.clearAutomaticLogoutCountdownTimer()
		} else
			Log.w("SessionActivity bug", "the main service was not running, could not cancel UI bump to login screen.")
	}

	/** Sets the logout timer, should trigger whenever onResume is called.  */
	override fun doBackgroundDependentTasks() {
		// Log.i("SessionActivity", "printed from SessionActivity");
		authenticateAndLoginIfNecessary()
	}

	/** If the user is NOT logged in, take them to the login page  */
	protected fun authenticateAndLoginIfNecessary() {
		if (PersistentData.isLoggedIn())
			MainService.startAutomaticLogoutCountdownTimer()
		else {
			val activityIntent = Intent(this, LoginActivity::class.java)
			activityIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
			startActivity(Intent(this, LoginActivity::class.java))
		}
	}

	/** Display the LoginActivity, and invalidate the login in SharedPreferences  */
	protected fun logoutUser() {
		PersistentData.logout()
		startActivity(Intent(this, LoginActivity::class.java))
	}

	/*####################################################################
	########################## Common UI #################################
	####################################################################*/

	/** Sets up the contents of the menu button.  */
	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// Inflate the menu; adds items to the action bar if it is present.
		menuInflater.inflate(R.menu.logged_in_menu, menu)
		if (PersistentData.getCallClinicianButtonEnabled())
			menu.findItem(R.id.menu_call_clinician).title = PersistentData.getCallClinicianButtonText()
		else
			menu.findItem(R.id.menu_call_clinician).isVisible = false

		if (!PersistentData.getCallResearchAssistantButtonEnabled())
			menu.findItem(R.id.menu_call_research_assistant).isVisible = false
		return true
	}

	/** Sets up the behavior of the items in the menu.  */
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return when (item.itemId) {
			R.id.menu_change_password -> {
				startActivity(Intent(applicationContext, ResetPasswordActivity::class.java))
				true
			}
			R.id.menu_signout -> {
				logoutUser()
				true
			}
			R.id.menu_about -> {
				startActivity(Intent(applicationContext, AboutActivityLoggedIn::class.java))
				true
			}
			R.id.view_survey_answers -> {
				startActivity(Intent(applicationContext, GraphActivity::class.java))
				super.onOptionsItemSelected(item)
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
}
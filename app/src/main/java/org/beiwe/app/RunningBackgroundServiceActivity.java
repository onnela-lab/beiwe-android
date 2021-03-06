package org.beiwe.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.beiwe.app.MainService.BackgroundServiceBinder;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.ui.user.AboutActivityLoggedOut;

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
 * @author Eli Jones, Josh Zagorsky
 */
public class RunningBackgroundServiceActivity extends AppCompatActivity {
	/** The backgroundService variable is an Activity's connection to the ... BackgroundService.
	 * We ensure the BackgroundService is running in the onResume call, and functionality that
	 * relies on the BackgroundService is always tied to UI elements, reducing the chance of
	 * a null backgroundService variable to essentially zero. */
	protected MainService mainService;

	//an unused variable for tracking whether the main Service is connected, uncomment if we ever need that.
//	protected boolean isBound = false;
	
	/**The ServiceConnection Class is our trigger for events that rely on the BackgroundService */
	protected ServiceConnection mainServiceConnection = new ServiceConnection() {
	    @Override
	    public void onServiceConnected(ComponentName name, IBinder binder) {
	        // Log.d("ServiceConnection", "Main Service Connected");
	        BackgroundServiceBinder some_binder = (BackgroundServiceBinder) binder;
	        mainService = some_binder.getService();
	        doBackgroundDependentTasks();
//	        isBound = true;
	    }
	    
	    @Override
	    public void onServiceDisconnected(ComponentName name) {
	        Log.w("ServiceConnection", "Main Service Disconnected");
	        mainService = null;
//	        isBound = false;
	    }
	};
	
	@Override
	protected void onCreate(Bundle bundle){ 
		super.onCreate(bundle);
		if (!BuildConfig.APP_IS_DEV) {
			Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
		}
		
		PersistentData.initialize(getApplicationContext());
	}
	
	/** Override this function to do tasks on creation, but only after the Main Service has been initialized. */
	protected void doBackgroundDependentTasks() { /*Log.d("RunningBackgroundServiceActivity", "doBackgroundDependentTasks ran as default (do nothing)");*/ }
	
	@Override
	/**On creation of RunningBackgroundServiceActivity we guarantee that the BackgroundService is
	 * actually running, we then bind to it so we can access program resources. */
	protected void onResume() {
		super.onResume();

		Intent startingIntent = new Intent(this.getApplicationContext(), MainService.class);
		startingIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
		// this will only start a new service if it is not already running, and check API version for appropriate version
		ContextCompat.startForegroundService(this.getApplicationContext(), startingIntent);
        bindService( startingIntent, mainServiceConnection, Context.BIND_AUTO_CREATE);
	}


	@Override
	/** disconnect BackgroundServiceConnection when the Activity closes, otherwise we have a
	 * memory leak warning (and probably an actual memory leak, too). */
	protected void onPause() {
		super.onPause();
		activityNotVisible = true;
		unbindService(mainServiceConnection);
	}
	
	
	/*####################################################################
	########################## Common UI #################################
	####################################################################*/
	
	@Override
	/** Common UI element, the menu button.*/
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.logged_out_menu, menu);

		if(PersistentData.getCallClinicianButtonEnabled()) {
			menu.findItem(R.id.menu_call_clinician).setTitle(PersistentData.getCallClinicianButtonText());
		}
		else {
			menu.findItem(R.id.menu_call_clinician).setVisible(false);
		}

		if(!PersistentData.getCallResearchAssistantButtonEnabled()) {
			menu.findItem(R.id.menu_call_research_assistant).setVisible(false);
		}

		return true;
	}
	
	
	/** Common UI element, items in menu.*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		case R.id.menu_about:
			startActivity(new Intent(getApplicationContext(), AboutActivityLoggedOut.class));
			return true;
		case R.id.menu_call_clinician:
			callClinician(null);
			return true;
		case R.id.menu_call_research_assistant:
			callResearchAssistant(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	/** sends user to phone, calls the user's clinician. */
	@SuppressWarnings("MissingPermission")
	public void callClinician(View v) {
		startPhoneCall(PersistentData.getPrimaryCareNumber());
	}
	
	/** sends user to phone, calls the study's research assistant. */
	@SuppressWarnings("MissingPermission")
	public void callResearchAssistant(View v) {
		startPhoneCall(PersistentData.getPasswordResetNumber());
	}

	private void startPhoneCall(String phoneNumber) {
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + phoneNumber));
		try {
			startActivity(callIntent);
		} catch (SecurityException e) {
			showMinimalAlertForRedirectToSettings(this,
					getString(R.string.cant_make_a_phone_call_permissions_alert),
					getString(R.string.cant_make_phone_call_alert_title),
					0);
		}
	}

	/*####################################################################
	###################### Permission Prompting ##########################
	####################################################################*/

	private static Boolean prePromptActive = false;
	private static Boolean postPromptActive = false;
	private static Boolean powerPromptActive = false;
	private static Boolean thisResumeCausedByFalseActivityReturn = false;
	private static Boolean aboutToResetFalseActivityReturn = false;
	private static Boolean activityNotVisible = false;
	public Boolean isAudioRecorderActivity() { return false; }

	private void goToSettings(Integer permissionIdentifier) {
		// Log.i("sessionActivity", "goToSettings");
		Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
		myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
		myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(myAppSettings, permissionIdentifier);
	}

	@TargetApi(23)
	private void goToPowerSettings(Integer powerCallbackIdentifier) {
		// Log.i("sessionActivity", "goToSettings");
		@SuppressLint("BatteryLife") Intent powerSettings = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
		powerSettings.addCategory(Intent.CATEGORY_DEFAULT);
		powerSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(powerSettings, powerCallbackIdentifier);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Log.i("sessionActivity", "onActivityResult. requestCode: " + requestCode + ", resultCode: " + resultCode );
		aboutToResetFalseActivityReturn = true;
	}

	@Override
	public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
		// Log.i("sessionActivity", "onRequestPermissionResult");
		if (!activityNotVisible) checkPermissionsLogic();
	}

	protected void checkPermissionsLogic() {
		//gets called as part of onResume,
		activityNotVisible = false;
		// Log.i("sessionactivity", "checkPermissionsLogic");
		// Log.i("sessionActivity", "prePromptActive: " + prePromptActive);
		// Log.i("sessionActivity", "postPromptActive: " + postPromptActive);
		// Log.i("sessionActivity", "thisResumeCausedByFalseActivityReturn: " + thisResumeCausedByFalseActivityReturn);
		// Log.i("sessionActivity", "aboutToResetFalseActivityReturn: " + aboutToResetFalseActivityReturn);

		if (aboutToResetFalseActivityReturn) {
			aboutToResetFalseActivityReturn = false;
			thisResumeCausedByFalseActivityReturn = false;
			return;
		}

		if ( !thisResumeCausedByFalseActivityReturn )  {
			String permission = PermissionHandler.getNextPermission( getApplicationContext(), this.isAudioRecorderActivity() );
			if (permission == null || prePromptActive || postPromptActive ) { return; }

			if (!powerPromptActive) {
				if (permission.equals(PermissionHandler.POWER_EXCEPTION_PERMISSION)) {
					showPowerManagementAlert(this, getString(R.string.power_management_exception_alert), 1000);
					return;
				}
				// Log.d("sessionActivity", "shouldShowRequestPermissionRationale "+ permission +": " + shouldShowRequestPermissionRationale( permission ) );

				//if the user has declined this permission before, redirect them to the settings page instead of sending another request for the notification
				if (PersistentData.getLastRequestedPermission().equals(permission) || shouldShowRequestPermissionRationale( permission ) ) {
					showAlertThatForcesUserToGrantPermission(
							this,
							PermissionHandler.getBumpingPermissionMessage(permission, getApplicationContext()),
							PermissionHandler.permissionMessages.get(permission)
					);
				}
				else {
					showRegularPermissionAlert(
							this,
							PermissionHandler.getNormalPermissionMessage(permission, getApplicationContext()),
							permission,
							PermissionHandler.permissionMessages.get(permission)
					);
				}
				PersistentData.setLastRequestedPermission(permission);
			}
		}
	}


	//the following 'alert' functions all send simple popup messages to the user related to getting necessary app permissions

	//the showRegularPermissionAlert function prompts with a message, and then sends the system request for the permission
	public static void showRegularPermissionAlert(final RunningBackgroundServiceActivity activity, final String message, final String permission, final Integer permissionCallback) {
		// Log.i("sessionActivity", "showPreAlert");
		if (prePromptActive) { return; }
		prePromptActive = true;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(activity.getString(R.string.permissions_alert_title));
		builder.setMessage(message);
		builder.setOnDismissListener( new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) {
			activity.requestPermissions(new String[]{ permission }, permissionCallback );
			prePromptActive = false;
		} } );
		builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface arg0, int arg1) { } } ); //Okay button
		builder.create().show();
	}

	//the showAlertThatForcesUserToGrantPermission function assumes the user has already declined the permission, and redirects them to the system settings page for this app
	public static void showAlertThatForcesUserToGrantPermission(final RunningBackgroundServiceActivity activity, final String message, final Integer permissionCallback) {
		// Log.i("sessionActivity", "showPostAlert");
		if (postPromptActive) { return; }
		postPromptActive = true;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(activity.getString(R.string.permissions_alert_title));
		builder.setMessage(message);
		builder.setOnDismissListener( new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				thisResumeCausedByFalseActivityReturn = true;
				activity.goToSettings(permissionCallback);
				postPromptActive = false;
			}
		});
		builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text), new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface arg0, int arg1) {}
		}); //Okay button
		builder.create().show();
	}

	//this is called inside of startPhoneCall function only and is used to direct users to the settings page without interfering with the checkPermissionLogic function
	public static void showMinimalAlertForRedirectToSettings(final RunningBackgroundServiceActivity activity, final String message, final String title, final Integer permissionCallback) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(activity.getString(R.string.go_to_settings_button), new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int arg1) {
				activity.goToSettings(permissionCallback);
			}
		});
		builder.setNegativeButton(activity.getString(R.string.alert_cancel_button_text), new DialogInterface.OnClickListener() {
			@Override public void onClick(DialogInterface dialog, int arg1) {}
		});
		builder.create().show();
	}

	public static void showPowerManagementAlert(final RunningBackgroundServiceActivity activity, final String message, final Integer powerCallbackIdentifier) {
		Log.i("sessionActivity", "power alert");
		if (powerPromptActive) { return; }
		powerPromptActive = true;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(activity.getString(R.string.permissions_alert_title));
		builder.setMessage(message);
		builder.setOnDismissListener( new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) {
			Log.d("power management alert", "bumping");
			thisResumeCausedByFalseActivityReturn = true;
			activity.goToPowerSettings(powerCallbackIdentifier);
			powerPromptActive = false;
		} } );
		builder.setPositiveButton(activity.getString(R.string.alert_ok_button_text), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface arg0, int arg1) {  } } ); //Okay button
		builder.create().show();
	}
}

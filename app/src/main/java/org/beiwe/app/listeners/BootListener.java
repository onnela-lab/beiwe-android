package org.beiwe.app.listeners;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.beiwe.app.MainService;

/**The BootListener is never actually instantiated elsewhere in the app.  It's job is to sit
 * and wait for either the boot broadcast or the SD (external) applications available.
 * @author Eli */
public class BootListener extends BroadcastReceiver {
	
	/** Checks whether the app is installed on the SD card; needs a context passed in 
	 *  Grab a pagkageManager (general info) -> get packageInfo (info about this package) ->
	 *  ApplicationInfo (information about this application instance).
	 *  http://stackoverflow.com/questions/5814474/how-can-i-find-out-if-my-app-is-installed-on-sd-card */
//	private Boolean checkForSDCardInstall(Context externalContext) throws NameNotFoundException{
//		PackageManager pkgManager = externalContext.getPackageManager();
//		try {
//			PackageInfo pkgInfo = pkgManager.getPackageInfo(externalContext.getPackageName(), 0);
//			ApplicationInfo appInfo = pkgInfo.applicationInfo;
//			//appInfo.flags is an int; docs say: "Flags associated with the application. Any combination of... [list_of_flags]."  
//			// the following line returns true if the app is installed on an SD card.  
//			return (appInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE; }
//		catch (NameNotFoundException e) {
//			Log.i("PowerStateListener", "something is broken in the check for installation on an SD card.");
//			throw e; }
//	}
	
	/** Does what it says, starts the main service running.
	 *  called when SDcard available and on device startup. */	
	private void startMainService(Context externalContext){
		Intent intent_to_start_foreground_service = new Intent(externalContext, MainService.class);
		intent_to_start_foreground_service.addFlags(Intent.FLAG_FROM_BACKGROUND);
		// ContextCompat correctly handles old and new android APIs
		ContextCompat.startForegroundService(externalContext, intent_to_start_foreground_service);
	}
	
	@Override
	public void onReceive(Context externalContext, Intent intent) {
		// Device turned on or other intents (see manifest)
		startMainService(externalContext);
	}
}

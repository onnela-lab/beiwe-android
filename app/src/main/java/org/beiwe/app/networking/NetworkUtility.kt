package org.beiwe.app.networking

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import org.beiwe.app.storage.PersistentData

/** Contains a single function to check whether wifi is active and functional.
 * @author Eli Jones, Joshua Zagorsky */

//FIXME: Everything here is deprecated, rewrite.

object NetworkUtility {
	/**Return TRUE if WiFi is connected; FALSE otherwise.
	 * Android 6 adds support for multiple network connections of the same type and the older
	 * get-network-by-type command is deprecated.
	 * We need to handle both cases.
	 * @return boolean value of whether the wifi is on and network connectivity is available. */
	@JvmStatic
	fun canUpload(appContext: Context): Boolean {
		// If you're allowed to upload over cellular data, simply check whether the phone's
		// connected to the internet at all.
		if (PersistentData.getAllowUploadOverCellularData()) {
			// Log.i("WIFICHECK", "ALLOW OVER CELLULAR!!!!")
			if (networkIsAvailable(appContext)) return true
		}

		// If you're only allowed to upload over WiFi, or if the simple networkIsAvailable() check
		// returned false, check if a WiFi network is connected.
		val connManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		// Check if WiFi is connected for Android version 6 and above
		return wiFiConnectivityCheck(connManager)
	}

	@JvmStatic
	fun networkIsAvailable(appContext: Context): Boolean {
		val connManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val activeNetwork = connManager.activeNetworkInfo
		return activeNetwork != null && activeNetwork.isConnected && activeNetwork.isAvailable
	}

	/** This is the function for running Android 6+ wifi connectivity checks.  */
	private fun wiFiConnectivityCheck(connManager: ConnectivityManager): Boolean {
		// Log.i("WIFICHECK", "Deprecated WiFi Connectivity Check")
		val networks = connManager.allNetworks

		// this code has been around since the beginning of time, just leave it in.
		for (network in networks) {
			// The documentation says this function returns null if it is not sent a valid network
			// but the function is marked as not-null.
			val networkInfo = connManager.getNetworkInfo(network) ?: return false

			// Apparently we can't trust connManager.getAllNetworks() to return valid networks
			// return true if there is a connected and available wifi connection.
			if (networkInfo.type == ConnectivityManager.TYPE_WIFI
					&& networkInfo.isConnected && networkInfo.isAvailable)
				return true

		}
		return false  // no wifi-type network connections active and available
	}
}
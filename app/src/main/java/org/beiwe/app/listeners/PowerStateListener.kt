package org.beiwe.app.listeners

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import org.beiwe.app.storage.TextFileManager

/** Listens for power state changes.
 * Screen On/Off, Power Connect/Disconnect, Airplane Mode.
 * @author Josh Zagorsky, Eli Jones, May/June 2014 */

class PowerStateListener : BroadcastReceiver() {
	// Handles the logging, includes a new line for the CSV files.  This code is otherwise reused everywhere.
	private fun makeLogStatement(message: String) {
		Log.i("PowerStateListener", message)
		val javaTimeCode = System.currentTimeMillis()
		TextFileManager.getPowerStateFile().writeEncrypted(javaTimeCode.toString() + TextFileManager.DELIMITER + message)
	}

	override fun onReceive(externalContext: Context, intent: Intent?) {
		if (!started || intent == null)
			return

		val action = intent.action

		// Screen on/off
		if (action == Intent.ACTION_SCREEN_OFF)
			makeLogStatement("Screen turned off")
		if (action == Intent.ACTION_SCREEN_ON)
			makeLogStatement("Screen turned on")

		// Power connected/disconnected
		if (action == Intent.ACTION_POWER_CONNECTED)
			makeLogStatement("Power connected")
		if (action == Intent.ACTION_POWER_DISCONNECTED)
			makeLogStatement("Power disconnected")

		// Shutdown/Restart
		if (action == Intent.ACTION_SHUTDOWN)
			makeLogStatement("Device shut down signal received")
		if (action == Intent.ACTION_REBOOT)
			makeLogStatement("Device reboot signal received")

		// Power save mode is a low-battery state where android turns off battery draining features.
		if (action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
			if (powerManager!!.isPowerSaveMode)
				makeLogStatement("Power Save Mode state change signal received; device in power save state.")
			else
				makeLogStatement("Power Save Mode change signal received; device not in power save state.")
		}

		// This indicates that Doze mode has been entered
		if (action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
			if (powerManager!!.isDeviceIdleMode)
				makeLogStatement("Device Idle (Doze) state change signal received; device in idle state.")
			else
				makeLogStatement("Device Idle (Doze) state change signal received; device not in idle state.")
			// Log.d("device idle state", "" + powerManager!!.isDeviceIdleMode)
		}
	}

	companion object {
		// The Power State Manager can receive broadcasts before the app is even running.
		// This would cause a a crash because we need the TextFileManager to be available.
		// The started variable is set to true during the startup process for the app.
		private var started = false
		private var powerManager: PowerManager? = null

		fun start(context: Context) {
			started = true
			powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		}

		@JvmField
		var header = "timestamp, event"
	}
}
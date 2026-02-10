package org.beiwe.app.listeners

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.beiwe.app.storage.EncryptionEngine
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import java.util.Date

// import android.content.pm.PackageManager;
// http://code.tutsplus.com/tutorials/android-quick-look-bluetoothadapter--mobile-7813
/* Tests.
4.4.2, nexus 7 tablet
	The UI does not allow toggling bluetooth on and off quickly.  It waits for the turning on/off state to finish.
	There is about a ... half second? lag between the turning on/off state broadcast and the actually on/off broadcast.

LG G2 does not interrupt the whole service of turning off and turning on :) There is a lag of about a half a second in
between phases

https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
If you want to declare that your app is available to BLE-capable devices only, include the following in your app's manifest:
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>

*/
/** BluetoothListener
 * The BluetoothListener handles the location of nearby patients in the study, but is limited by
 * the way Android handles Bluetooth interactions.
 *
 * BluetoothListener keeps track of the state of the device's Bluetooth Adaptor, and will
 * intelligently enable/disable Bluetooth as needed.  It only enables Bluetooth in order to make
 * a Bluetooth Low Energy scan and record any Bluetooth MAC addresses that show up, and then will
 * disable Bluetooth.  If the Bluetooth adaptor was already enabled it will not turn Bluetooth off.
 *
 * @author Eli Jones
 */
class BluetoothListener : BroadcastReceiver() {
	companion object {
		@JvmField
		var header = "timestamp, hashed MAC, RSSI"
		private var scanActive = false

		@JvmStatic
		fun getScanActive(): Boolean {
			return scanActive
		}
	}

	// the access to the bluetooth adaptor
	private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

	// bluetoothExists can be set to false if the device does not meet our needs.
	private var bluetoothExists: Boolean? = null

	// Stateful variables - we need to stach the state of the bluetooth adapter as it was when beiwe started
	private var internalBluetoothState: Boolean
	private var externalBluetoothState: Boolean

	/**The BluetoothListener constructor needs to gracefully handle Bluetooth existence issues.
	 * we test in MainService for the device feature bluetooth low energy, here  we check that
	 * ANY Bluetooth device exists.  */
	init {
		// Log.i("bluetooth", "required: " + Build.VERSION_CODES.JELLY_BEAN_MR2  + ", SDK INT: " + Build.VERSION.SDK_INT);
		// We have to check if the BluetoothAdaptor is null, or if the device is not running api 18+
		if (bluetoothAdapter == null) {
			bluetoothExists = false
			externalBluetoothState = false
			internalBluetoothState = false
		} else {
			bluetoothExists = true
			// set the external state variable to the state the device was in on instantiation,
			// and set the internernal state variable to be the same.
			externalBluetoothState = this.isBluetoothEnabled
			internalBluetoothState = this.externalBluetoothState
		}
	}

	/** Checks that bluetooth exists and is enabled.  */
	val isBluetoothEnabled: Boolean
		get() = (if (bluetoothExists!!) bluetoothAdapter!!.isEnabled else false)

	/** Intelligently and safely disables the Bluetooth adaptor.
	 * @return True if bluetooth exists, false if bluetooth does not exist*/
	private fun disableBluetooth(): Boolean {
		PersistentData.bluetoothStop = Date(System.currentTimeMillis()).toLocaleString()

		if (!bluetoothExists!!)
			return false

		// Log.d("BluetoothListener", "disable bluetooth.");
		internalBluetoothState = false
		// this check was incorrect for 13 months, however bonded devices are not the same as connected devices.
		// This check was never relevent before (nobody ever noticed), so now we are just removing the check entirely.
		// If we want to implement more bluetooth safety checks, see http://stackoverflow.com/questions/3932228/list-connected-bluetooth-devices
		// if ( bluetoothAdapter.getBondedDevices().isEmpty() ) {
		// 	Log.d("BluetoothListener", "found a bonded bluetooth device, will not be turning off bluetooth.");
		// 	externalBluetoothState = true; }

		if (!externalBluetoothState) { // if the outside world and us agree that it should be off, turn it off
			bluetoothAdapter!!.disable()
			return true
		}
		return false
	}

	/** Intelligently and safely enables the bluetooth adaptor.
	 * @return True if bluetooth exists, false if bluetooth does not exist.
	 */
	private fun enableBluetooth(): Boolean {
		PersistentData.bluetoothStart = Date(System.currentTimeMillis()).toLocaleString()

		if (!bluetoothExists!!) {
			return false
		}
		// Log.d("BluetoothListener", "enable bluetooth.");
		internalBluetoothState = true
		if (!externalBluetoothState) {  // if we want it on and the external world wants it off, turn it on. (we retain state)
			bluetoothAdapter!!.enable()
			return true
		}
		return false
	}

	/** Intelligently and safely starts a Bluetooth LE scan.
	 * Sets the scanActive variable to true, then checks if bluetooth is already on.
	 * If Bluetooth is already on start the scan, otherwise depend on the Bluetooth
	 * State Change On broadcast.  This can take a few seconds.  */
	@SuppressLint("NewApi")
	fun enableBLEScan() {
		if (!bluetoothExists!!) {
			return
		}
		Log.d("BluetoothListener", "enable BLE scan.")
		// set the scan variable, enable Bluetooth.
		scanActive = true
		if (isBluetoothEnabled) {
			tryScanning()
		} else {
			enableBluetooth()
		}
		TextFileManager.getBluetoothLogFile().newFile()
	}

	/** Intelligently and safely disables bluetooth.
	 * Sets the scanActive variable to false, and stops any running Bluetooth LE scan,
	 * then disable Bluetooth (intelligently).
	 * Note: we cannot actually guarantee the scan has stopped, that function returns void.  */
	@Suppress("deprecation") // Yeah. This is totally safe.
	@SuppressLint("NewApi")
	fun disableBLEScan() {
		if (!bluetoothExists!!) {
			return
		}
		Log.i("BluetoothListener", "disable BLE scan.")
		scanActive = false
		bluetoothAdapter!!.stopLeScan(bluetoothCallback)
		disableBluetooth()
	}

	/** Intelligently ACTUALLY STARTS a Bluetooth LE scan.
	 * If Bluetooth is available, start scanning.  Makes verbose logging statements  */
	@Suppress("deprecation")
	@SuppressLint("NewApi")
	private fun tryScanning() {
		Log.i("bluetooth", "starting a scan: " + scanActive)
		if (isBluetoothEnabled) {
			if (bluetoothAdapter!!.startLeScan(bluetoothCallback)) { /*Log.d("bluetooth", "bluetooth LE scan started successfully.");*/
			} else {
				Log.w("bluetooth", "bluetooth LE scan NOT started successfully.")
			}
		} else {
			Log.w("bluetooth", "bluetooth could not be enabled?")
		}
	}

	/** LeScanCallback is code that is run when a Bluetooth LE scan returns some data.
	 * We take the returned data and log it.  */
	@SuppressLint("NewApi")
	private val bluetoothCallback = LeScanCallback { device, rssi, scanRecord ->
		TextFileManager.getBluetoothLogFile().writeEncrypted(
				System.currentTimeMillis().toString() + "," + EncryptionEngine.hashMAC(device.toString()) + "," + rssi
		)
		// Log.i("Bluetooth",  System.currentTimeMillis() + "," + device.toString() + ", " + rssi )
	}


	/*####################################################################################
	################# the onReceive Stack for Bluetooth state messages ###################
	####################################################################################*/
	@Synchronized
	/** The onReceive method for the BluetoothListener listens for Bluetooth State changes.
	 * The Bluetooth adaptor can be in any of 4 states: on, off, turning on, and turning off.
	 * Whenever the turning on or off state comes in, we update the externalBluetoothState variable
	 * so that we never turn Bluetooth off when the user wants it on.
	 * Additionally, if a Bluetooth On notification comes in AND the scanActive variable is set to TRUE
	 * we start a Bluetooth LE scan.  */
	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action
		if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
			val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
			if (state == BluetoothAdapter.ERROR) {
				Log.e("bluetooth", "BLUETOOTH ADAPTOR ERROR?")
			} else if (state == BluetoothAdapter.STATE_ON) {
				// Log.i("bluetooth", "state change: on" );
				if (scanActive)
					enableBLEScan()

			} else if (state == BluetoothAdapter.STATE_TURNING_ON) {
				// Log.i("bluetooth", "state change: turning on");
				if (!internalBluetoothState)
					externalBluetoothState = true

			} else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
				// Log.i("bluetooth", "state change: turning off");
				if (internalBluetoothState)
					externalBluetoothState = false

			}
		}
	}

	/*###############################################################################
	############################# Debugging Code ####################################
	###############################################################################*/
	// val state: String
	//     get() {
	//         if (!bluetoothExists!!)
	//             return "does not exist."
	//         val state = bluetoothAdapter!!.state
	//         //		STATE_OFF, STATE_TURNING_ON, STATE_ON, STATE_TURNING_OFF
	//         if (state == BluetoothAdapter.STATE_OFF)
	//             return "off"
	//         else if (state == BluetoothAdapter.STATE_TURNING_ON)
	//             return "turning on"
	//         else if (state == BluetoothAdapter.STATE_ON)
	//             return "on"
	//         else if (state == BluetoothAdapter.STATE_TURNING_OFF)
	//             return "turning off"
	//         else
	//             return "getstate is broken, value was $state"
	//     }
	//
	// fun bluetoothInfo() {
	//     Log.i("bluetooth", "bluetooth existence: " + bluetoothExists.toString())
	//     Log.i("bluetooth", "bluetooth enabled: " + isBluetoothEnabled)
	//     // Log.i("bluetooth", "bluetooth address: " + bluetoothAdapter!!.address)
	//     Log.i("bluetooth", "bluetooth state: " + state)
	//     Log.i("bluetooth", "bluetooth scan mode: " + bluetoothAdapter.scanMode)
	//     Log.i("bluetooth", "bluetooth bonded devices:" + bluetoothAdapter.bondedDevices)
	// }
}
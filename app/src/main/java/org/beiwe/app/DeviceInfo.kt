package org.beiwe.app

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import org.beiwe.app.storage.EncryptionEngine
import java.util.TimeZone

/** This class is a collection information about the device, static so that it can be accessed from
 * anywhere.  Only AndroidId needs initialization, which is called in MainService */

object DeviceInfo {
	// this is to be used in an increasing number of places where we hae system elapsed time for sensor events
	var boot_time_milli = System.currentTimeMillis() - SystemClock.elapsedRealtime()
	var boot_time_nano = System.nanoTime() - SystemClock.elapsedRealtimeNanos()
	private var androidID: String? = null

	/** grab the Android ID and the Bluetooth's MAC address  */
	@JvmStatic
	@SuppressLint("HardwareIds")
	fun initialize(appContext: Context) {
		// android ID appears to be a 64 bit string
		androidID = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
	}

	@JvmStatic
	val beiweVersion: String
		get() = BuildConfig.FLAVOR + "-" + BuildConfig.VERSION_NAME

	@JvmStatic
	val androidVersion: String
		get() = Build.VERSION.RELEASE

	@JvmStatic
	val product: String
		get() = Build.PRODUCT

	@JvmStatic
	val brand: String
		get() = Build.BRAND

	@JvmStatic
	val hardwareId: String
		get() = Build.HARDWARE

	@JvmStatic
	val manufacturer: String
		get() = Build.MANUFACTURER

	@JvmStatic
	val model: String
		get() = Build.MODEL

	@JvmStatic
	fun getAndroidID(): String {
		return EncryptionEngine.safeHash(androidID)
	}

	@JvmStatic
	fun timeZoneInfo(): String {
		return  TimeZone.getDefault().id  // this is a string like "America/New_York"
	}

	@JvmStatic
	fun freeSpace(): String {
		// profiled on a Pixel 6, it takes less than a millisecond.
		return android.os.StatFs("/data").availableBytes.toString()
	}
}
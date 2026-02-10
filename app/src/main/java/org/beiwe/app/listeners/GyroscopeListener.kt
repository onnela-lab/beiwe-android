package org.beiwe.app.listeners

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import org.beiwe.app.DeviceInfo
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import java.util.Date
import java.util.Locale

class GyroscopeListener(private val appContext: Context) : SensorEventListener {
	companion object {
		@JvmField
		var header = "timestamp,accuracy,x,y,z"
	}

	private var gyroSensorManager: SensorManager? = null
	private var gyroSensor: Sensor? = null
	var running: Boolean = false
	private var accuracy ="unknown"
	private var lineCount = 0

	var exists: Boolean = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)

	/**Listens for gyroscope updates.  NOT activated on instantiation. Use the turn_on() function
	 * to log any gyroscope updates to the gyroscope log. */
	init {
		if (exists) {
			// logic tests two layers of nullability
			gyroSensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
			if (gyroSensorManager == null) {
				Log.e("Gyroscope Problems", "gyroSensorManager does not exist? (1)")
				TextFileManager.writeDebugLogStatement("gyroSensorManager does not exist? (1)")
				exists = false
			}

			gyroSensor = gyroSensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
			if (gyroSensor == null) {
				Log.e("Gyroscope Problems", "gyroSensor does not exist? (2)")
				TextFileManager.writeDebugLogStatement("gyroSensor does not exist? (2)")
				exists = false
			}
			// TODO: reenable after we get the data formatting correct / as dev log item
			// only runs once per app launch
			// TextFileManager.writeDebugLogStatement("gyroscope sensor info: $gyroSensor")
		}
	}

	@Synchronized
	fun turn_on() {
		PersistentData.gyroscopeStart = Date(System.currentTimeMillis()).toLocaleString()

		if (gyroSensorManager == null)
			return

		// 1 / frequency equals the period in seconds, times one million gets period in microseconds
		val delay_period_microseconds = ((1.0 / PersistentData.getGyroscopeFrequency().toDouble()) * 1000000).toInt()
//        print("starting gyro with delay of $delay_period_microseconds")
//        print("gyro frequency was " + PersistentData.getGyroscopeFrequency())

		if (!gyroSensorManager!!.registerListener(this, gyroSensor, delay_period_microseconds)) {
			Log.e("Gyroscope", "Gyroscope is broken")
			TextFileManager.writeDebugLogStatement("Trying to start gyroscope session, device cannot find gyroscope.")
		} else
			running = true
	}

	@Synchronized
	fun turn_off() {
		PersistentData.gyroscopeStop = Date(System.currentTimeMillis()).toLocaleString()
		gyroSensorManager?.unregisterListener(this)
		running = false
	}

	val gyro_off_action: () -> Unit = { turn_off() }
	val gyro_on_action: () -> Unit = { turn_on() }

	/** Update the accuracy, synchronized so very closely timed trigger events do not overlap.
	 * (only triggered by the system.)  */
	@Synchronized
	override fun onAccuracyChanged(arg0: Sensor, arg1: Int) {
		accuracy = arg1.toString()
	}

	// private var prior_timecode: Long = 0
	/** On receipt of a sensor change, record it.  Include accuracy.
	 * (only ever triggered by the system.)  */
	@Synchronized
	override fun onSensorChanged(arg0: SensorEvent) {
		// we record the system boot time once and use that as a reference.
		val javaTimeCode = DeviceInfo.boot_time_milli + (arg0.timestamp / 1000000)
		// print("gyro milliseconds since prior: ${javaTimeCode-prior_timecode})")
		// prior_timecode = javaTimeCode
		val values = arg0.values
		val value0 = String.format(Locale.US, "%.16f", values[0])
		val value1 = String.format(Locale.US, "%.16f", values[1])
		val value2 = String.format(Locale.US, "%.16f", values[2])
		// if the linecount is over 10,000 then we create a new file:
		if (lineCount > 10000) {
			TextFileManager.getGyroFile().newFile()
			lineCount = 0
		}
		TextFileManager.getGyroFile().writeEncrypted("$javaTimeCode,$accuracy,$value0,$value1,$value2")
		lineCount++
	}
}
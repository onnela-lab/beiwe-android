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

class AccelerometerListener(appContext: Context) : SensorEventListener {
    companion object {
        @JvmField
        var header = "timestamp,accuracy,x,y,z"
    }

    private var accelSensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    var running: Boolean = false
    private var accuracy = "unknown"
    private var lineCount = 0

    var exists: Boolean = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)

    /**Listens for accelerometer updates.  NOT activated on instantiation. Use the turn_on()
     * function to log any accelerometer updates to the accelerometer log. */
    init {
        if (exists) {
            // logic tests two layers of nullability
            accelSensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (accelSensorManager == null) {
                Log.e("Accelerometer Problems", "accelSensorManager does not exist? (1)")
                TextFileManager.writeDebugLogStatement("accelSensorManager does not exist? (1)")
                exists = false
            }

            accelSensor = accelSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelSensor == null) {
                Log.e("Accelerometer Problems", "accelSensor does not exist? (2)")
                TextFileManager.writeDebugLogStatement("accelSensor does not exist? (2)")
                exists = false
            }
            // TODO: reenable after we get the data formatting correct / as dev log item
            // only runs once per app launch
            // TextFileManager.writeDebugLogStatement(
            //         System.currentTimeMillis().toString() + " accelerometer sensor info: $accelSensor")
        }
    }

    @Synchronized
    fun turn_on() {
        PersistentData.accelerometerStart = Date(System.currentTimeMillis()).toLocaleString()
        if (accelSensorManager == null)
            return

        // 1 / frequency equals the period in seconds, times one million gets period in microseconds
        val delay_period_microseconds = ((1.0 / PersistentData.getAccelerometerFrequency().toDouble()) * 1000000).toInt()
//        print("starting accelerometer with delay of $delay_period_microseconds")
//        print("accelerometer frequency was " + PersistentData.getAccelerometerFrequency())

        if (!accelSensorManager!!.registerListener(this, accelSensor, delay_period_microseconds)) {
            Log.e("Accelerometer", "Accelerometer is broken")
            // this one happens occasionally, for 5% of users, across many manufacturers (samsung, motorola, HMD, HUAWEI, Yulong, LGE, OnePlus, Google, OPPO)
            // The other log statements do not.
            TextFileManager.writeDebugLogStatement("Trying to start Accelerometer session, device cannot find accelerometer.")
        }
        else
            running = true
    }

    @Synchronized
    fun turn_off() {
        PersistentData.accelerometerStop = Date(System.currentTimeMillis()).toLocaleString()
        accelSensorManager?.unregisterListener(this)
        running = false
    }

    val accelerometer_off_action: () -> Unit = { turn_off() }
    val accelerometer_on_action: () -> Unit = { turn_on() }

    /** Update the accuracy, synchronized so very closely timed trigger events do not overlap.
     * (only triggered by the system.)  */
    @Synchronized
    override fun onAccuracyChanged(arg0: Sensor, arg1: Int) {
        accuracy = arg1.toString()
    }

    // private var prior_timecode: Long = 0  // purely for debugging

    /** On receipt of a sensor change, record it.  Include accuracy.
     * (only ever triggered by the system.)  */
    @Synchronized
    override fun onSensorChanged(arg0: SensorEvent) {
        // we record the system boot time once and use that as a reference, actual info of the
        // timestamp comes from the sensor event and is in nanoseconds. Device precision is unknown.
        val javaTimeCode = DeviceInfo.boot_time_milli + (arg0.timestamp / 1000000)

        // needs to be error log or else we get no logcat output. wut.
        // printe("accelerometer milliseconds since prior: ${javaTimeCode-prior_timecode})")
        val values = arg0.values

        // force values to 16 decimal places in order to avoid scientific notation, and force english locale.
        // String.format is locale-aware, non-us locales may use commas.
        val value0 = String.format(Locale.US,"%.16f", values[0])
        val value1 = String.format(Locale.US,"%.16f", values[1])
        val value2 = String.format(Locale.US,"%.16f", values[2])

        // if the linecount is over 10,000 then we create a new file:
        if (lineCount > 10000) {
            TextFileManager.getAccelFile().newFile()
            lineCount = 0
        }

        // for testing high speeds print only every 100th line to avoid spam
        // if (lineCount % 100 == 0)
        //     printe("accelerometer milliseconds since prior: ${javaTimeCode-prior_timecode})")

        TextFileManager.getAccelFile().writeEncrypted("$javaTimeCode,$accuracy,$value0,$value1,$value2")
        lineCount++
        // prior_timecode = javaTimeCode
    }
}

/* The following are the results of values passed in to the microsecond delay parameter of the
 * accelerometer sensor on the left, and the milliseconds between recorded events on the right.
 *
 * This was done on Pixel 6 (not 6a) running android 12, and then later 13.
 *
 * Sometimes the value would just snap back to 18 after starting at a different value, but if I used
 * developer functions to crash the background service and reopen the app, it would snap to whatever
 * the "correct" "allowed" value.  Baffling.
 *
 * This chunk of testing was done with the app open, plugged in.  Values with / means it varied a bit.
 *
 * "fastest" (0): 1 millisecond
 * 1000: 1
 * 2000: 2/3
 * 4000: 2/3
 * 3000: 2/3
 * 5000: 4 or 5
 * 6000: 4/5
 * 7000: 4/5
 * 8000: 4/5
 * 9000: 4/5
 * 9999: 9/10
 * 10000: 9/10
 * 15000: 9/10
 * 20000: 18
 * 30000: 18
 * 35000: 18
 * 40000: 36/37
 * 50000: 36/37, and then 18
 * 60000: 36/37, and then 18
 * 100000: 72
 * 150000: 72, and then 18
 * 200000: 145
 * 300000: 145
 * 400000: 145 and then 18
 * 500000: 145 and then 18
 * 600000: 145 and then 18
 *
 *
 *
 * other observations:
 * when the device screen turns off due to the screen timer it would step down to a lower value.
 * but I don't know what the value was, this test wos not intentional.
 *  the step was 145 milliseconds -> 288
 * I  tried 200000, it got stuck on 18 milliseconds and did not replicate the behavior.
 * I tried 300000 and it got stuck on 18 milliseconds for several minutes, then jumped to 145 BEFORE
 *  the device's screen turned off... then went BACK to 18 again still before the screen was off...
 *  then it went to 145 with the screen on, THEN the screen turned off, then it went back to 18.
 *    The only observation I can make in addition is that this switch would be ROUGHLY around when
 *    other timer events kicked off actions, like uploads or other sensors.  Its fairly reliable in
 *    this co-occurrence.  Maybe there is some kind of calculation where the system assigns more
 *    resources when these timers go off.  (it this case these were minutely timers.
 *      OKAY and it then EVENTUALLY like 10 minutes later started at 288... and then when there was
 *      another set of timer events that kicked it back up to 18.
 *      AFTER returning to the device about 45 minutes later it was still vacillating between these values.
 *
 * - I let a faster data rate, 4000, run for a while and it was fairly stable.
 *
 * - Using the dev tools to update the app with a custom value while the device has the screen off and
 *   was previously running at the lower, power-saving value would kick it up to the higher value.
 * - Accidentally unplugging the test device and plugging it back in a couple seconds later made
 *   no apparent change to the sampling rate.
 */

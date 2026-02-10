package org.beiwe.app.listeners

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import org.beiwe.app.DeviceInfo
import org.beiwe.app.PermissionHandler.checkAccessCoarseLocation
import org.beiwe.app.PermissionHandler.checkAccessFineLocation
import org.beiwe.app.printe
import org.beiwe.app.printi
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import java.util.Date
import java.util.Locale

/* Notes/observation on Location Services:
 * We are passing in "0" as the minimum time for location updates to be pushed to us, this results in about
 * 1 update every second.  This is based on logs made using a nexus 7 tablet.
 * This makes sense, GPSs on phones do not Have that kind of granularity/resolution.
 * However, we need the resolution in milliseconds for the line-by-line encryption scheme.
 * So, we grab the system time instead.  This may add a fraction of a second to the timestamp.
 * 
 * We are NOT recording which location provider provided the update, or which location providers
 * are available on a given device. */
class GPSListener(private val appContext: Context) : LocationListener {
	companion object {
		const val header = "timestamp, latitude, longitude, altitude, accuracy"
	}

	private val pkgManager: PackageManager = appContext.packageManager
	private var locationManager: LocationManager
	var running = false
	private var first_instantiation = true

	/** Listens for GPS updates from the network GPS location provider and/or the true
	 * GPS provider, both if possible.  It is NOT activated upon instantiation.  Requires an
	 * application Context object be passed in in order to interface with location services.
	 * When activated using the turn_on() function it will log any location updates to the GPS log.
	 * @param appContext A Context provided an Activity or Service. */
	init {
		locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		Log.d("initializing GPS...", "initializing GPS...")
	}

	/** Turns on GPS providers, provided they are accessible. Handles permission errors appropriately
	 * Tests many permissions */
	@SuppressLint("MissingPermission")
	@Synchronized
	fun turn_on() {
		PersistentData.gpsStart = Date(System.currentTimeMillis()).toLocaleString()

		val coarsePermissible = checkAccessCoarseLocation(appContext)
		val finePermissible = checkAccessFineLocation(appContext)

		if (!coarsePermissible)
			makeDebugLogStatement("Beiwe has not been granted permissions for coarse location updates.")
		if (!finePermissible)
			makeDebugLogStatement("Beiwe has not been granted permissions for fine location updates.")

		val fineExists = pkgManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
		val coarseExists = pkgManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)
		if (!fineExists)
			makeDebugLogStatement("Fine location updates are unsupported on this device.")
		if (!coarseExists)
			makeDebugLogStatement("Coarse location updates are unsupported on this device.")

		if (!fineExists and !coarseExists)
			return

		// if already enabled return true.  We want the above logging, do not refactor to earlier in the logic.
		if (running)
			return

		// Instantiate a new location manager (looks like the fine and coarse available variables get confused if we use an old one.)
		locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

		// Uncomment to view all location providers, prints on gps session.
		// printe("locationmanager gnssYearOfHardware ${locationManager.gnssYearOfHardware}")
		// printe("locationmanager gnssAntennaInfos ${locationManager.gnssAntennaInfos}")
		// printe("locationmanager gnssCapabilities ${locationManager.gnssCapabilities}")
		// locationManager.allProviders.forEach { printe("provider: $it") }

		// If the feature exists, request locations from it. (enable if their boolean flag is true.)
		if (fineExists && finePermissible && coarsePermissible) {
			// AndroidStudio insists that both of these require the same location permissions, which seems to be correct
			// since there is only one toggle in userland anyway, yes or no to location permissions.
			// parameters: provider, minTime, minDistance, listener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
		}

		if (coarseExists && finePermissible && coarsePermissible) {
			// parameters: provider, minTime, minDistance, listener);
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0f, this)
		}

		// Verbose statements on the quality of GPS data streams.
		val fineAvailable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
		val coarseAvailable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

		if (!fineAvailable)
			makeDebugLogStatement("GPS data stream warning: fine location updates are currently disabled.")
		if (!coarseAvailable)
			makeDebugLogStatement("GPS data stream warning: coarse location updates are currently disabled.")

		// There is more information to be had but its kind of junk, output looks like this:
		//  GPS properties: ProviderProperties[powerUsage=Low, accuracy=Fine, supports=[bearing,speed,altitude]]
		//  Network properties: ProviderProperties[powerUsage=Low, accuracy=Fine]
		// if (first_instantiation && fineAvailable) {
		//     val properties: ProviderProperties? = locationManager.getProviderProperties(LocationManager.GPS_PROVIDER)
		//     if (properties != null)
		//         printe("GPS properties: $properties")
		// }
		// if (first_instantiation && coarseAvailable) {
		//     val properties: ProviderProperties? = locationManager.getProviderProperties(LocationManager.PASSIVE_PROVIDER)
		//     if (properties != null)
		//         printe("Network properties: $properties")
		// }


		running = true
		// TODO: reenable after we get the data formatting correct / as dev log item
		// if (first_instantiation) {
		//     first_instantiation = false
		//     val device_info = "GPS GNSS Hardware Model Name: ${locationManager.gnssHardwareModelName}"
		//     printi(device_info)
		//     makeDebugLogStatement(device_info)
		// }
	}

	/** Disable all location updates  */
	@Synchronized
	fun turn_off() {
		PersistentData.gpsStop = Date(System.currentTimeMillis()).toLocaleString()
		// pretty confident this cannot fail.
		locationManager.removeUpdates(this)
		running = false
	}

	val gps_off_action: () -> Unit = { turn_off() }
	val gps_on_action: () -> Unit = { turn_on() }

	/** pushes an update to us whenever there is a location update.  */
	override fun onLocationChanged(location: Location) {
		// val javaTimeCode = System.currentTimeMillis()
		// order: time, latitude, longitude, altitude, horizontal_accuracy\n
		// printe(System.currentTimeMillis())
		// printe(location.elapsedRealtimeNanos / 1_000_000)

		// we record the system boot time once and use that as a reference.
		val javaTimeCode = DeviceInfo.boot_time_milli + (location.elapsedRealtimeNanos / 1_000_000)

		// Latitude and longitude offset should be 0 unless GPS fuzzing is enabled, which provides
		// an offset of (-0.2 to -1) and (0.2 to 1) This offset has to remain small because
		// otherwise distance calculations become innacurate.
		val latitude = location.latitude + PersistentData.getLatitudeOffset()
		// the range for highly negative numbers (up to -360.0) creates out-of-range output, the fix is to add
		// multiple of 360 + 180 so that the truncating remainder is always a positive value -  and then at
		// the end account for the negative range by subtracting 180. (1260 = 360*3 + 180)
		val longitude = (location.longitude + PersistentData.getLongitudeOffset() + 1260) % 360 - 180.0
		val data = (
				javaTimeCode.toString() + TextFileManager.DELIMITER
					+ String.format(Locale.US, "%.16f", latitude) + TextFileManager.DELIMITER
					+ String.format(Locale.US, "%.16f", longitude) + TextFileManager.DELIMITER
					+ String.format(Locale.US, "%.16f", location.altitude) + TextFileManager.DELIMITER
					+ String.format(Locale.US, "%.16f", location.accuracy)
				)
		// note, altitude is notoriously inaccurate, getAccuracy only applies to latitude/longitude
		TextFileManager.getGPSFile().writeEncrypted(data)
	}

	/*  We do not actually need to implement any of the following overrides.
	 *  When a provider has a changed we do not need to record it, and we have
	 *  not encountered any corner cases where these are relevant. */
	//  arg0 for Provider Enabled/Disabled is a string saying "network" or "gps".
	override fun onProviderDisabled(arg0: String) {} // Log.d("A location provider was disabled.", arg0); }
	override fun onProviderEnabled(arg0: String) {} // Log.d("A location provider was enabled.", arg0); }

	private fun makeDebugLogStatement(message: String) {
		TextFileManager.writeDebugLogStatement(message)
		Log.w("GPS recording warning", message)
	}
}
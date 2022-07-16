package org.beiwe.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import org.beiwe.app.storage.PersistentData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PermissionHandler {
	public static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;
	public static int PERMISSION_DENIED = PackageManager.PERMISSION_DENIED;
	public static String POWER_EXCEPTION_PERMISSION = "POWER_EXCEPTION_PERMISSION";
	
	public static Map <String, Integer> permissionMessages = new HashMap <> ();
	static {
		permissionMessages.put(Manifest.permission.ACCESS_FINE_LOCATION, R.string.permission_access_fine_location);
		permissionMessages.put(Manifest.permission.ACCESS_NETWORK_STATE, R.string.permission_access_network_state);
		permissionMessages.put(Manifest.permission.ACCESS_WIFI_STATE, R.string.permission_access_wifi_state);
		permissionMessages.put(Manifest.permission.READ_SMS, R.string.permission_read_sms);
		permissionMessages.put(Manifest.permission.BLUETOOTH, R.string.permission_bluetooth);
		permissionMessages.put(Manifest.permission.BLUETOOTH_ADMIN, R.string.permission_bluetooth_admin);
		permissionMessages.put(Manifest.permission.CALL_PHONE, R.string.permission_call_phone);
		permissionMessages.put(Manifest.permission.INTERNET, R.string.permission_internet);
		permissionMessages.put(Manifest.permission.READ_CALL_LOG, R.string.permission_read_call_log);
		permissionMessages.put(Manifest.permission.READ_CONTACTS, R.string.permission_read_contacts);
		permissionMessages.put(Manifest.permission.READ_PHONE_STATE, R.string.permission_read_phone_state);
		permissionMessages.put(Manifest.permission.RECEIVE_BOOT_COMPLETED, R.string.permission_receive_boot_completed);
		permissionMessages.put(Manifest.permission.RECORD_AUDIO, R.string.permission_record_audio);
		permissionMessages.put(Manifest.permission.ACCESS_COARSE_LOCATION, R.string.permission_access_coarse_location);
		permissionMessages.put(Manifest.permission.RECEIVE_MMS, R.string.permission_receive_mms);
		permissionMessages.put(Manifest.permission.RECEIVE_SMS, R.string.permission_receive_sms);
		permissionMessages.put(Manifest.permission.ACCESS_BACKGROUND_LOCATION, R.string.permission_access_background_location);
		permissionMessages = Collections.unmodifiableMap(permissionMessages);
	}

	public static String getNormalPermissionMessage(String permission, Context appContext) {
		return String.format(appContext.getString(R.string.permission_normal_request_template),
				appContext.getString(permissionMessages.get(permission)));
	}
	public static String getBumpingPermissionMessage(String permission, Context appContext) {
		return String.format(appContext.getString(R.string.permission_bumping_request_template),
				appContext.getString(permissionMessages.get(permission)));
	}
	
	/* The following are enabled by default.
	 *  AccessNetworkState
	 *  AccessWifiState
	 *  Bluetooth
	 *  BluetoothAdmin
	 *  Internet
	 *  ReceiveBootCompleted
	 * 
	 * the following are enabled on the registration screen
	 *  ReadSms
	 *  ReceiveMms
	 *  ReceiveSms
	 * 
	 * This leaves the following to be prompted for at session activity start
	 *  AccessFineLocation - GPS
	 *  CallPhone - Calls
	 *  ReadCallLog - Calls
	 *  ReadContacts - Calls and SMS
	 *  ReadPhoneState - Calls
	 *  WriteCallLog - Calls
	 *  
	 *  We will check for microphone recording as a special condition on the audio recording screen. */
	public static Boolean checkAccessCoarseLocation(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessFineLocation(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessNetworkState(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessWifiState(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessBluetooth(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessBluetoothAdmin(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessCallPhone(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReadCallLog(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReadContacts(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReadPhoneState(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReadSms(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.READ_SMS) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReceiveMms(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.RECEIVE_MMS) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessReceiveSms(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PERMISSION_GRANTED; } else { return true; } }
	public static Boolean checkAccessRecordAudio(Context context) { if ( android.os.Build.VERSION.SDK_INT >= 23) { return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED;} else { return true; } }
	
	public static boolean checkGpsPermissions( Context context ) { return ( checkAccessFineLocation(context) ); }
	public static boolean checkCallsPermissions( Context context ) { return ( checkAccessReadPhoneState(context) && checkAccessCallPhone(context) && checkAccessReadCallLog(context) ); }
	public static boolean checkTextsPermissions( Context context ) { return ( checkAccessReadContacts(context) && checkAccessReadSms(context) && checkAccessReceiveMms(context) && checkAccessReceiveSms(context) ); }
	// TODO: for unknown reasons, at some point in the past, the checkwifipermissions function included checkAccessCoarseLocation. This has been removed and tested on chris' android 6.0 phone and the nexus 7 tablet and does not appear to be necessary.
	// TODO: We may need to re-enable this function because course location is required for wifi on Google Pixel phone as if Android 8.1.0
	// public static boolean checkWifiPermissions( Context context ) { return ( checkAccessWifiState(context) && checkAccessNetworkState(context) && checkAccessCoarseLocation(context) ) ; }
	public static boolean checkWifiPermissions( Context context ) { return ( checkAccessWifiState(context) && checkAccessNetworkState(context)) ; }
	public static boolean checkBluetoothPermissions( Context context ) { return ( checkAccessBluetooth(context) && checkAccessBluetoothAdmin(context)); }

	public static boolean confirmGps( Context context ) { return ( PersistentData.getGpsEnabled() && checkGpsPermissions(context) ); }
	public static boolean confirmCalls( Context context ) { return ( PersistentData.getCallsEnabled() && checkCallsPermissions(context) ); }
	public static boolean confirmTexts( Context context ) { return ( PersistentData.getTextsEnabled() && checkTextsPermissions(context) ); }
	public static boolean confirmWifi( Context context ) { return ( PersistentData.getWifiEnabled() && checkWifiPermissions(context) && checkAccessFineLocation(context) && checkAccessCoarseLocation(context) ) ; }
	public static boolean confirmBluetooth( Context context ) { return ( PersistentData.getBluetoothEnabled() && checkBluetoothPermissions(context)); }
	public static boolean confirmAmbientAudioCollection(Context context ) { return ( PersistentData.getAmbientAudioCollectionIsEnabled() && checkAccessRecordAudio(context)); }

	public static String getNextPermission(Context context, Boolean includeRecording) {
		if (PersistentData.getGpsEnabled()) {
			if ( !checkAccessFineLocation(context) ) return Manifest.permission.ACCESS_FINE_LOCATION; }
		if (PersistentData.getWifiEnabled()) {
			if ( !checkAccessWifiState(context)) return Manifest.permission.ACCESS_WIFI_STATE;
			if ( !checkAccessNetworkState(context)) return Manifest.permission.ACCESS_NETWORK_STATE;
			if ( !checkAccessCoarseLocation(context)) return Manifest.permission.ACCESS_COARSE_LOCATION;
			if ( !checkAccessFineLocation(context)) return Manifest.permission.ACCESS_FINE_LOCATION; }
		if (PersistentData.getBluetoothEnabled()) {
			if ( !checkAccessBluetooth(context)) return Manifest.permission.BLUETOOTH;
			if ( !checkAccessBluetoothAdmin(context)) return Manifest.permission.BLUETOOTH_ADMIN; }
		if (PersistentData.getCallsEnabled() && BuildConfig.READ_SMS_AND_PHONE_CALL_STATS) {
			if ( !checkAccessReadPhoneState(context)) return Manifest.permission.READ_PHONE_STATE;  
			if ( !checkAccessReadCallLog(context)) return Manifest.permission.READ_CALL_LOG; }
		if (PersistentData.getTextsEnabled() && BuildConfig.READ_SMS_AND_PHONE_CALL_STATS) {
			if ( !checkAccessReadContacts(context)) return Manifest.permission.READ_CONTACTS;  
			if ( !checkAccessReadSms(context)) return Manifest.permission.READ_SMS;
			if ( !checkAccessReceiveMms(context)) return Manifest.permission.RECEIVE_MMS;
			if ( !checkAccessReceiveSms(context)) return Manifest.permission.RECEIVE_SMS; }
		if (includeRecording || PersistentData.getAmbientAudioCollectionIsEnabled()) {
			if ( !checkAccessRecordAudio(context)) { return Manifest.permission.RECORD_AUDIO; } }

		//The phone call permission is invariant, it is required for all studies in order for the
		// call clinician functionality to work
		if ( !checkAccessCallPhone(context)) return Manifest.permission.CALL_PHONE;

		if ( android.os.Build.VERSION.SDK_INT >= 23 ) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			if (! pm.isIgnoringBatteryOptimizations(context.getPackageName()) ) {
				return POWER_EXCEPTION_PERMISSION;
			}
		}
		return null;
	}
}
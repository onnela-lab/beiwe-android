package org.beiwe.app.storage;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import org.beiwe.app.BuildConfig;
import org.beiwe.app.JSONUtils;
import org.beiwe.app.R;
import org.json.JSONArray;
import org.json.JSONException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**A class for managing patient login sessions.
 * Uses SharedPreferences in order to save username-password combinations.
 * @author Dor Samet, Eli Jones, Josh Zagorsky */
public class PersistentData {
	public static String NULL_ID = "NULLID";
	private static final long MAX_LONG = 9223372036854775807L;
	
	private static boolean isInitialized = false;
	
	
	// Private things that are encapsulated using functions in this class 
	private static SharedPreferences pref;
	private static Editor editor;
	private static Context appContext;
	
	/**  Editor key-strings */
	private static final String PREF_NAME = "BeiwePref";
	private static final String SERVER_URL_KEY = "serverUrl";
	private static final String KEY_ID = "uid";
	private static final String KEY_PASSWORD = "password";
	private static final String IS_REGISTERED = "IsRegistered";
	private static final String STUDY_NAME = "study_name";
	private static final String STUDY_ID = "study_id";
	
	private static final String LOGIN_EXPIRATION = "loginExpirationTimestamp";
	private static final String PCP_PHONE_KEY = "primary_care";
	private static final String PASSWORD_RESET_NUMBER_KEY = "reset_number";

	private static final String FCM_INSTANCE_ID = "fcmInstanceID";

	private static final String ACCELEROMETER = "accelerometer";
	private static final String GYROSCOPE = "gyroscope";
	private static final String GPS = "gps";
	private static final String CALLS = "calls";
	private static final String TEXTS = "texts";
	private static final String WIFI = "wifi";
	private static final String BLUETOOTH = "bluetooth";
	private static final String POWER_STATE = "power_state";
	private static final String ALLOW_UPLOAD_OVER_CELLULAR_DATA = "allow_upload_over_cellular_data";
	private static final String AMBIENT_AUDIO = "ambient_audio";

	private static final String ACCELEROMETER_OFF_DURATION_SECONDS = "accelerometer_off_duration_seconds";
	private static final String ACCELEROMETER_ON_DURATION_SECONDS = "accelerometer_on_duration_seconds";
	private static final String GYROSCOPE_ON_DURATION_SECONDS = "gyro_on_duration_seconds";
	private static final String GYROSCOPE_OFF_DURATION_SECONDS = "gyro_off_duration_seconds";
	private static final String BLUETOOTH_ON_DURATION_SECONDS = "bluetooth_on_duration_seconds";
	private static final String BLUETOOTH_TOTAL_DURATION_SECONDS = "bluetooth_total_duration_seconds";
	private static final String BLUETOOTH_GLOBAL_OFFSET_SECONDS = "bluetooth_global_offset_seconds";
	private static final String CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS = "check_for_new_surveys_frequency_seconds";
	private static final String CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS = "create_new_data_files_frequency_seconds";
	private static final String GPS_OFF_DURATION_SECONDS = "gps_off_duration_seconds";
	private static final String GPS_ON_DURATION_SECONDS = "gps_on_duration_seconds";
	private static final String SECONDS_BEFORE_AUTO_LOGOUT = "seconds_before_auto_logout";
	private static final String UPLOAD_DATA_FILES_FREQUENCY_SECONDS = "upload_data_files_frequency_seconds";
	private static final String VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS = "voice_recording_max_time_length_seconds";
	private static final String WIFI_LOG_FREQUENCY_SECONDS = "wifi_log_frequency_seconds";
	private static final String SURVEY_IDS = "survey_ids";
	private static final String LastRequestedPermission = "last_requested_permission";
//	private static final String SURVEY_QUESTION_IDS = "question_ids";

	/*#################################################################################################
	################################### Initializing and Editing ######################################
	#################################################################################################*/

	/**The publicly accessible initializing function for the LoginManager, initializes the internal variables. */
	public static void initialize( Context context ) {
		if ( isInitialized ) { return; }
		appContext = context;
		pref = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		editor = pref.edit();
		editor.commit();
		isInitialized = true;
	}

	private static void putCommit(String name, long l) {
		editor.putLong(name, l);
		editor.commit();
	}
	private static void putCommit(String name, boolean b) {
		editor.putBoolean(name, b);
		editor.commit();
	}
	private static void putCommit(String name, String s) {
		editor.putString(name, s);
		editor.commit();
	}
	private static void putCommit(String name, float f) {
		editor.putFloat(name, f);
		editor.commit();
	}
	private static void putCommit(String name, int i) {
		editor.putInt(name, i);
		editor.commit();
	}
	
	/*#####################################################################################
	##################################### User State ######################################
	#####################################################################################*/

	/** Quick check for login. **/
	public static boolean isLoggedIn(){
		if (pref == null) Log.w("LoginManager", "FAILED AT ISLOGGEDIN");
		// If the current time is earlier than the expiration time, return TRUE; else FALSE
		return (System.currentTimeMillis() < pref.getLong(LOGIN_EXPIRATION, 0));
	}

	/** Set the login session to expire a fixed amount of time in the future */
	public static void loginOrRefreshLogin() {
		putCommit(LOGIN_EXPIRATION, System.currentTimeMillis() + getTimeBeforeAutoLogout());
	}

	/** Set the login session to "expired" */
	public static void logout() {
		// LOGIN_EXPIRATION must be stored as a long
		putCommit(LOGIN_EXPIRATION, 0L);
	}

	/**Getter for the IS_REGISTERED value. */
	public static boolean isRegistered() {
		if (pref == null) Log.w("LoginManager", "FAILED AT ISREGISTERED");
		return pref.getBoolean(IS_REGISTERED, false); }

	/**Setter for the IS_REGISTERED value. */
	public static void setRegistered(boolean value) {
		putCommit(IS_REGISTERED, value);
	}

	public static void setLastRequestedPermission(String value)  {
		putCommit(LastRequestedPermission, value);
	}

	public static String getLastRequestedPermission() {
		return pref.getString(LastRequestedPermission, "");
	}

	/*######################################################################################
	##################################### Passwords ########################################
	######################################################################################*/

	/**Checks that an input matches valid password requirements. (this only checks length)
	 * Throws up an alert notifying the user if the password is not valid.
	 * @param password
	 * @return true or false based on password requirements.*/
	public static boolean passwordMeetsRequirements(String password) {
		return (password.length() >= minPasswordLength());
	}

	public static int minPasswordLength() {
		if (BuildConfig.APP_IS_BETA) return 1;
		else return 6;
	}

 	/**Takes an input string and returns a boolean value stating whether the input matches the current password. */
	public static boolean checkPassword(String input){ return ( getPassword().equals( EncryptionEngine.safeHash(input) ) ); }

	/**Sets a password to a hash of the provided value. */
	public static void setPassword(String password) {
		putCommit(KEY_PASSWORD, EncryptionEngine.safeHash(password) );
	}

	/*#####################################################################################
	################################# Firebase Cloud Messaging Instance ID ################
	#####################################################################################*/

	public static void setFCMInstanceID(String fcmInstanceID) {
		putCommit(FCM_INSTANCE_ID, fcmInstanceID);
	}

	public static String getFCMInstanceID() {
		return pref.getString(FCM_INSTANCE_ID, null); }
	
	
	/*#####################################################################################
	################################# Listener Settings ###################################
	#####################################################################################*/

	public static boolean getAccelerometerEnabled(){ return pref.getBoolean(ACCELEROMETER, false); }
	public static boolean getGyroscopeEnabled(){return pref.getBoolean(GYROSCOPE, false); }
	public static boolean getGpsEnabled(){ return pref.getBoolean(GPS, false); }
	public static boolean getCallsEnabled(){ return pref.getBoolean(CALLS, false); }
	public static boolean getTextsEnabled(){ return pref.getBoolean(TEXTS, false); }
	public static boolean getWifiEnabled(){ return pref.getBoolean(WIFI, false); }
	public static boolean getBluetoothEnabled(){ return pref.getBoolean(BLUETOOTH, false); }
	public static boolean getPowerStateEnabled(){ return pref.getBoolean(POWER_STATE, false); }
	public static boolean getAllowUploadOverCellularData(){ return pref.getBoolean(ALLOW_UPLOAD_OVER_CELLULAR_DATA, false); }
	public static boolean getAmbientAudioCollectionIsEnabled(){ return pref.getBoolean(AMBIENT_AUDIO, false); }
	
	public static void setAccelerometerEnabled(boolean enabled) {
		putCommit(ACCELEROMETER, enabled);
	}
	public static void setGyroscopeEnabled(boolean enabled) {
		putCommit(GYROSCOPE, enabled);
	}
	public static void setGpsEnabled(boolean enabled) {
		putCommit(GPS, enabled);
	}
	public static void setCallsEnabled(boolean enabled) {
		putCommit(CALLS, enabled);
	}
	public static void setTextsEnabled(boolean enabled) {
		putCommit(TEXTS, enabled);
	}
	public static void setWifiEnabled(boolean enabled) {
		putCommit(WIFI, enabled);
	}
	public static void setBluetoothEnabled(boolean enabled) {
		putCommit(BLUETOOTH, enabled);
	}
	public static void setPowerStateEnabled(boolean enabled) {
		putCommit(POWER_STATE, enabled);
	}
	public static void setAllowUploadOverCellularData(boolean enabled) { putCommit(ALLOW_UPLOAD_OVER_CELLULAR_DATA, enabled); }
	public static void setAmbientAudioCollectionIsEnabled(boolean enabled) { putCommit(AMBIENT_AUDIO, enabled); }
	
	/*#####################################################################################
	################################## Timer Settings #####################################
	#####################################################################################*/

	// Default timings (only used if app doesn't download custom timings)
	private static final long DEFAULT_ACCELEROMETER_OFF_MINIMUM_DURATION = 10;
	private static final long DEFAULT_ACCELEROMETER_ON_DURATION = 10 * 60;
	private static final long DEFAULT_GYROSCOPE_OFF_MINIMUM_DURATION = 10;
	private static final long DEFAULT_GYROSCOPE_ON_DURATION = 10 * 60;
	private static final long DEFAULT_BLUETOOTH_ON_DURATION = 1 * 60;
	private static final long DEFAULT_BLUETOOTH_TOTAL_DURATION = 5 * 60;
	private static final long DEFAULT_BLUETOOTH_GLOBAL_OFFSET = 0 * 60;
	private static final long DEFAULT_CHECK_FOR_NEW_SURVEYS_PERIOD = 24 * 60 * 60;
	private static final long DEFAULT_CREATE_NEW_DATA_FILES_PERIOD = 15 * 60;
	private static final long DEFAULT_GPS_OFF_MINIMUM_DURATION = 5 * 60;
	private static final long DEFAULT_GPS_ON_DURATION = 5 * 60;
	private static final long DEFAULT_SECONDS_BEFORE_AUTO_LOGOUT = 5 * 60;
	private static final long DEFAULT_UPLOAD_DATA_FILES_PERIOD = 60;
	private static final long DEFAULT_VOICE_RECORDING_MAX_TIME_LENGTH = 4 * 60;
	private static final long DEFAULT_WIFI_LOG_FREQUENCY = 5 * 60;
	
	
	public static long getAccelerometerOffDuration () { return 1000L * pref.getLong(ACCELEROMETER_OFF_DURATION_SECONDS, DEFAULT_ACCELEROMETER_OFF_MINIMUM_DURATION); }
	public static void setAccelerometerOffDuration (long seconds) { putCommit(ACCELEROMETER_OFF_DURATION_SECONDS, seconds); }
	
	public static long getAccelerometerOnDuration () { return 1000L * pref.getLong(ACCELEROMETER_ON_DURATION_SECONDS, DEFAULT_ACCELEROMETER_ON_DURATION); }
	public static void setAccelerometerOnDuration (long seconds) { putCommit(ACCELEROMETER_ON_DURATION_SECONDS, seconds); }
	
	public static long getBluetoothGlobalOffset () { return 1000L * pref.getLong(BLUETOOTH_GLOBAL_OFFSET_SECONDS, DEFAULT_BLUETOOTH_GLOBAL_OFFSET); }
	public static void setBluetoothGlobalOffset (long seconds) { putCommit(BLUETOOTH_GLOBAL_OFFSET_SECONDS, seconds); }
	
	public static long getBluetoothOnDuration () { return 1000L * pref.getLong(BLUETOOTH_ON_DURATION_SECONDS, DEFAULT_BLUETOOTH_ON_DURATION); }
	public static void setBluetoothOnDuration (long seconds) { putCommit(BLUETOOTH_ON_DURATION_SECONDS, seconds); }
	
	public static long getBluetoothTotalDuration () { return 1000L * pref.getLong(BLUETOOTH_TOTAL_DURATION_SECONDS, DEFAULT_BLUETOOTH_TOTAL_DURATION); }
	public static void setBluetoothTotalDuration (long seconds) { putCommit(BLUETOOTH_TOTAL_DURATION_SECONDS, seconds); }
	
	public static long getCheckForNewSurveysFrequency () { return 1000L * pref.getLong(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, DEFAULT_CHECK_FOR_NEW_SURVEYS_PERIOD); }
	public static void setCheckForNewSurveysFrequency (long seconds) { putCommit(CHECK_FOR_NEW_SURVEYS_FREQUENCY_SECONDS, seconds); }
	
	public static long getCreateNewDataFilesFrequency () { return 1000L * pref.getLong(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, DEFAULT_CREATE_NEW_DATA_FILES_PERIOD); }
	public static void setCreateNewDataFilesFrequency (long seconds) { putCommit(CREATE_NEW_DATA_FILES_FREQUENCY_SECONDS, seconds); }
	
	public static long getGpsOffDuration () { return 1000L * pref.getLong(GPS_OFF_DURATION_SECONDS, DEFAULT_GPS_OFF_MINIMUM_DURATION); }
	public static void setGpsOffDuration (long seconds) { putCommit(GPS_OFF_DURATION_SECONDS, seconds); }
	
	public static long getGpsOnDuration () { return 1000L * pref.getLong(GPS_ON_DURATION_SECONDS, DEFAULT_GPS_ON_DURATION); }
	public static void setGpsOnDuration (long seconds) { putCommit(GPS_ON_DURATION_SECONDS, seconds); }
	
	public static long getGyroscopeOffDuration () { return 1000L * pref.getLong(GYROSCOPE_OFF_DURATION_SECONDS, DEFAULT_GYROSCOPE_OFF_MINIMUM_DURATION); }
	public static void setGyroscopeOffDuration (long seconds) { putCommit(GYROSCOPE_OFF_DURATION_SECONDS, seconds); }
	
	public static long getGyroscopeOnDuration () { return 1000L * pref.getLong(GYROSCOPE_ON_DURATION_SECONDS, DEFAULT_GYROSCOPE_ON_DURATION); }
	public static void setGyroscopeOnDuration (long seconds) { putCommit(GYROSCOPE_ON_DURATION_SECONDS, seconds); }
	
	public static long getTimeBeforeAutoLogout () { return 1000L * pref.getLong(SECONDS_BEFORE_AUTO_LOGOUT, DEFAULT_SECONDS_BEFORE_AUTO_LOGOUT); }
	public static void setTimeBeforeAutoLogout (long seconds) { putCommit(SECONDS_BEFORE_AUTO_LOGOUT, seconds); }
	
	public static void setUploadDataFilesFrequency (long seconds) { putCommit(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, seconds); }
	public static long getUploadDataFilesFrequency () { return 1000L * pref.getLong(UPLOAD_DATA_FILES_FREQUENCY_SECONDS, DEFAULT_UPLOAD_DATA_FILES_PERIOD); }
	
	public static long getVoiceRecordingMaxTimeLength () { return 1000L * pref.getLong(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, DEFAULT_VOICE_RECORDING_MAX_TIME_LENGTH); }
	public static void setVoiceRecordingMaxTimeLength (long seconds) { putCommit(VOICE_RECORDING_MAX_TIME_LENGTH_SECONDS, seconds); }
	
	public static long getWifiLogFrequency () { return 1000L * pref.getLong(WIFI_LOG_FREQUENCY_SECONDS, DEFAULT_WIFI_LOG_FREQUENCY); }
	public static void setWifiLogFrequency (long seconds) { putCommit(WIFI_LOG_FREQUENCY_SECONDS, seconds); }
	
	//accelerometer, gyroscope bluetooth, new surveys, create data files, gps, logout,upload, wifilog (not voice recording, that doesn't apply
	public static void setMostRecentAlarmTime(String identifier, long time) { putCommit(identifier + "-prior_alarm", time); }
	public static long getMostRecentAlarmTime(String identifier) { return pref.getLong( identifier + "-prior_alarm", 0); }
	//we want default to be 0 so that checks "is this value less than the current expected value" (eg "did this timer event pass already")
	
	/*###########################################################################################
	################################### Text Strings ############################################
	###########################################################################################*/

	private static final String ABOUT_PAGE_TEXT_KEY = "about_page_text";
	private static final String CALL_CLINICIAN_BUTTON_TEXT_KEY = "call_clinician_button_text";
	private static final String CONSENT_FORM_TEXT_KEY = "consent_form_text";
	private static final String SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY = "survey_submit_success_toast_text";
	
	public static String getAboutPageText() {
		String defaultText = appContext.getString(R.string.default_about_page_text);
		return pref.getString(ABOUT_PAGE_TEXT_KEY, defaultText); }
	public static String getCallClinicianButtonText() {
		String defaultText = appContext.getString(R.string.default_call_clinician_text);
		return pref.getString(CALL_CLINICIAN_BUTTON_TEXT_KEY, defaultText); }
	public static String getConsentFormText() {
		String defaultText = appContext.getString(R.string.default_consent_form_text);
		return pref.getString(CONSENT_FORM_TEXT_KEY, defaultText); }
	public static String getSurveySubmitSuccessToastText() {
		String defaultText = appContext.getString(R.string.default_survey_submit_success_message);
		return pref.getString(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, defaultText); }
	
	public static void setAboutPageText(String text) {
		putCommit(ABOUT_PAGE_TEXT_KEY, text);
	}
	public static void setCallClinicianButtonText(String text) {
		putCommit(CALL_CLINICIAN_BUTTON_TEXT_KEY, text);
	}
	public static void setConsentFormText(String text) {
		putCommit(CONSENT_FORM_TEXT_KEY, text);
	}
	public static void setSurveySubmitSuccessToastText(String text) {
		putCommit(SURVEY_SUBMIT_SUCCESS_TOAST_TEXT_KEY, text);
	}

	/*###########################################################################################
	################################### User Credentials ########################################
	###########################################################################################*/

	public static void setServerUrl(String serverUrl) {
		if (editor == null) Log.e("LoginManager.java", "editor is null in setServerUrl()");
		putCommit(SERVER_URL_KEY, prependHttpsToServerUrl(serverUrl));
	}
	
	private static String prependHttpsToServerUrl(String serverUrl) {
		if (serverUrl.startsWith("https://")) {
			return serverUrl;
		} else if (serverUrl.startsWith("http://")) {
			return "https://" + serverUrl.substring(7, serverUrl.length());
		} else {
			return "https://" + serverUrl;
		}
	}
	
	public static String getServerUrl() { return pref.getString(SERVER_URL_KEY, null); }

	public static void setLoginCredentials( String userID, String password ) {
		if (editor == null) Log.e("LoginManager.java", "editor is null in setLoginCredentials()");
		putCommit(KEY_ID, userID);
		setPassword(password);
	}

	public static String getPassword() { return pref.getString( KEY_PASSWORD, null ); }
	public static String getPatientID() { return pref.getString(KEY_ID, NULL_ID); }


	/*###########################################################################################
	###################################### ERROR INFO #########################################
	###########################################################################################*/

	public static void setStudyID(String studyID) {
		putCommit(STUDY_ID, studyID);
	}
	public static void setStudyName(String studyName) {
		putCommit(STUDY_NAME, studyName);
	}
	public static String getStudyID() { return pref.getString(STUDY_ID, NULL_ID); }
	public static String getStudyName() { return pref.getString(STUDY_NAME, NULL_ID); }

	/*###########################################################################################
	#################################### Contact Numbers ########################################
	###########################################################################################*/

	public static String getPrimaryCareNumber() { return pref.getString(PCP_PHONE_KEY, ""); }
	public static void setPrimaryCareNumber( String phoneNumber) {
		putCommit(PCP_PHONE_KEY, phoneNumber );
	}

	public static String getPasswordResetNumber() { return pref.getString(PASSWORD_RESET_NUMBER_KEY, ""); }
	public static void setPasswordResetNumber( String phoneNumber ){
		putCommit(PASSWORD_RESET_NUMBER_KEY, phoneNumber );
	}

	/*###########################################################################################
	###################################### Survey Info ##########################################
	###########################################################################################*/
	
	public static List<String> getSurveyIds() { return JSONUtils.jsonArrayToStringList(getSurveyIdsJsonArray()); }
	public static List<String> getSurveyQuestionMemory(String surveyId) { return JSONUtils.jsonArrayToStringList(getSurveyQuestionMemoryJsonArray(surveyId)); }
	public static String getSurveyTimes(String surveyId){ return pref.getString(surveyId + "-times", null); }
	public static String getSurveyContent(String surveyId){ return pref.getString(surveyId + "-content", null); }
	public static String getSurveyType(String surveyId){ return pref.getString(surveyId + "-type", null); }
	public static String getSurveySettings(String surveyId){ return pref.getString(surveyId + "-settings", null); }
	public static Boolean getSurveyNotificationState( String surveyId) { return pref.getBoolean(surveyId + "-notificationState", false ); }
	public static long getMostRecentSurveyAlarmTime(String surveyId) { return pref.getLong( surveyId + "-prior_alarm", MAX_LONG); }
	
	public static void createSurveyData(String surveyId, String content, String timings, String type, String settings){
		setSurveyContent(surveyId,  content);
		setSurveyTimes(surveyId, timings);
		setSurveyType(surveyId, type);
		setSurveySettings(surveyId, settings);
	}
	//individual setters
	public static void setSurveyContent(String surveyId, String content){
		putCommit(surveyId + "-content", content);
	}
	public static void setSurveyTimes(String surveyId, String times){
		putCommit(surveyId + "-times", times);
	}
	public static void setSurveyType(String surveyId, String type){
		putCommit(surveyId + "-type", type);
	}
	public static void setSurveySettings(String surveyId, String settings){
		putCommit(surveyId + "-settings", settings);
	}
	
	//survey state storage
	public static void setSurveyNotificationState(String surveyId, Boolean bool ) {
		putCommit(surveyId + "-notificationState", bool );
	}
	public static void setMostRecentSurveyAlarmTime(String surveyId, long time) {
		putCommit(surveyId + "-prior_alarm", time);
	}
	
	public static void deleteSurvey(String surveyId) {
		editor.remove(surveyId + "-content");
		editor.remove(surveyId + "-times");
		editor.remove(surveyId + "-type");
		editor.remove(surveyId + "-notificationState");
		editor.remove(surveyId + "-settings");
		editor.remove(surveyId + "-questionIds");
		editor.commit();
		removeSurveyId(surveyId);
	}
	
	//array style storage and removal for surveyIds and questionIds	
	public static JSONArray getSurveyIdsJsonArray() {
		String jsonString = pref.getString(SURVEY_IDS, "0");
		// Log.d("persistant data", "getting ids: " + jsonString);
		if (jsonString == "0") { return new JSONArray(); } //return empty if the list is empty
		try { return new JSONArray(jsonString); }
		catch (JSONException e) { throw new NullPointerException("getSurveyIds failed, json string was: " + jsonString ); }
	}
	
	public static void addSurveyId(String surveyId) {
		List<String> list = JSONUtils.jsonArrayToStringList( getSurveyIdsJsonArray() );
		if ( !list.contains(surveyId) ) {
			list.add(surveyId);
			putCommit(SURVEY_IDS, new JSONArray(list).toString() );
		}
		else {
			// create salt if it does not exist
			throw new NullPointerException("duplicate survey id added: " + surveyId);
		}
	}
	
	private static void removeSurveyId(String surveyId) {
		List<String> list = JSONUtils.jsonArrayToStringList( getSurveyIdsJsonArray() );
		if ( list.contains(surveyId) ) {
			list.remove(surveyId);
			putCommit(SURVEY_IDS, new JSONArray(list).toString() );
		}
		else {
			// create salt if it does not exist
			throw new NullPointerException("survey id does not exist: " + surveyId);
		}
	}
	
	private static JSONArray getSurveyQuestionMemoryJsonArray( String surveyId ) {
		String jsonString = pref.getString(surveyId + "-questionIds", "0");
		if (jsonString == "0") { return new JSONArray(); } //return empty if the list is empty
		try { return new JSONArray(jsonString); }
		catch (JSONException e) { throw new NullPointerException("getSurveyIds failed, json string was: " + jsonString ); }
	}
	
	public static void addSurveyQuestionMemory(String surveyId, String questionId) {
		List<String> list = getSurveyQuestionMemory(surveyId);
		// Log.d("persistent data", "adding questionId: " + questionId);
		if ( !list.contains(questionId) ) {
			list.add(questionId);
			putCommit(surveyId + "-questionIds", new JSONArray(list).toString() );
		}
		else {
			// create salt if it does not exist
			throw new NullPointerException("duplicate question id added: " + questionId);
		}
	}
	
	public static void clearSurveyQuestionMemory(String surveyId) {
		putCommit(surveyId + "-questionIds", new JSONArray().toString() );
	}

	/*###########################################################################################
	###################################### Encryption ###########################################
	###########################################################################################*/

	private static final String HASH_SALT_KEY = "hash_salt_key";
	private static final String HASH_ITERATIONS_KEY = "hash_iterations_key";
	private static final String USE_ANONYMIZED_HASHING_KEY = "use_anonymized_hashing";

	// Get salt for pbkdf2 hashing
	public static byte[] getHashSalt() {
		String saltString = pref.getString(HASH_SALT_KEY, null);
		// create salt if it does not exist
		if(saltString == null) {
			byte[] newSalt = SecureRandom.getSeed(64);
			putCommit(HASH_SALT_KEY, new String(newSalt));
			return newSalt;
		}
		else {
			return saltString.getBytes();
		}
	}

	// Get iterations for pbkdf2 hashing
	public static int getHashIterations() {
		int iterations = pref.getInt(HASH_ITERATIONS_KEY, 0);
		// create iterations if it does not exist
		if(iterations == 0) {
			// create random iteration count from 900 to 1100
			int newIterations = 1100 - new Random().nextInt(200);
			putCommit(HASH_ITERATIONS_KEY, newIterations);
			return newIterations;
		}
		else {
			return iterations;
		}
	}

	public static void setUseAnonymizedHashing(boolean useAnonymizedHashing) {
		putCommit(USE_ANONYMIZED_HASHING_KEY, useAnonymizedHashing);
	}
	public static boolean getUseAnonymizedHashing() {
		//If not present, default to safe hashing
		return pref.getBoolean(USE_ANONYMIZED_HASHING_KEY, true);
	}

	/*###########################################################################################
	###################################### FUZZY GPS ############################################
	###########################################################################################*/

	private static final String USE_GPS_FUZZING_KEY = "gps_fuzzing_key";
	private static final String LATITUDE_OFFSET_KEY = "latitude_offset_key";
	private static final String LONGITUDE_OFFSET_KEY = "longitude_offset_key";

	public static double getLatitudeOffset() {
		float latitudeOffset = pref.getFloat(LATITUDE_OFFSET_KEY, 0.0f);
		//create latitude offset if it does not exist
		if(latitudeOffset == 0.0f && getUseGpsFuzzing()) {
			// create random latitude offset between (-1, -.2) or (.2, 1)
			float newLatitudeOffset = (float)(.2 + Math.random()*1.6);
			if(newLatitudeOffset > 1) {
				newLatitudeOffset = (newLatitudeOffset-.8f) * -1;
			}
			putCommit(LATITUDE_OFFSET_KEY, newLatitudeOffset);
			return newLatitudeOffset;
		}
		else {
			return latitudeOffset;
		}
	}

	public static float getLongitudeOffset() {
		float longitudeOffset = pref.getFloat(LONGITUDE_OFFSET_KEY, 0.0f);
		//create longitude offset if it does not exist
		if(longitudeOffset == 0.0f && getUseGpsFuzzing()) {
			// create random longitude offset between (-180, -10) or (10, 180)
			float newLongitudeOffset = (float)(10 + Math.random()*340);
			if(newLongitudeOffset > 180) {
				newLongitudeOffset = (newLongitudeOffset-170) * -1;
			}
			putCommit(LONGITUDE_OFFSET_KEY, newLongitudeOffset);
			return newLongitudeOffset;
		}
		else {
			return longitudeOffset;
		}
	}

	public static void setUseGpsFuzzing(boolean useFuzzyGps) {
		putCommit(USE_GPS_FUZZING_KEY, useFuzzyGps);
	}
	private static boolean getUseGpsFuzzing() {
		return pref.getBoolean(USE_GPS_FUZZING_KEY, false);
	}

	/*###########################################################################################
	###################################### Call Buttons #########################################
	###########################################################################################*/

	private static final String CALL_CLINICIAN_BUTTON_ENABLED_KEY = "call_clinician_button_enabled";
	private static final String CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY = "call_research_assistant_button_enabled";

	public static boolean getCallClinicianButtonEnabled() {
		return pref.getBoolean(CALL_CLINICIAN_BUTTON_ENABLED_KEY, false);
	}

	public static void setCallClinicianButtonEnabled(boolean enabled) {
		putCommit(CALL_CLINICIAN_BUTTON_ENABLED_KEY, enabled);
	}

	public static boolean getCallResearchAssistantButtonEnabled() {
		return pref.getBoolean(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, false);
	}

	public static void setCallResearchAssistantButtonEnabled(boolean enabled) {
		putCommit(CALL_RESEARCH_ASSISTANT_BUTTON_ENABLED_KEY, enabled);
	}
}

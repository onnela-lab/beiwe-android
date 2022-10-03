package org.beiwe.app.storage;

import org.json.JSONException;
import org.json.JSONObject;

public class SetDeviceSettings {
	public static void writeDeviceSettings(JSONObject deviceSettings) throws JSONException {
		// Write data stream booleans
		Boolean accelerometerEnabled = deviceSettings.getBoolean("accelerometer");
		PersistentData.setAccelerometerEnabled(accelerometerEnabled);
		Boolean gyroscopeEnabled = deviceSettings.getBoolean("gyro");
		PersistentData.setGyroscopeEnabled(gyroscopeEnabled);
		Boolean gpsEnabled = deviceSettings.getBoolean("gps");
		PersistentData.setGpsEnabled(gpsEnabled);
		Boolean callsEnabled = deviceSettings.getBoolean("calls");
		PersistentData.setCallsEnabled(callsEnabled);
		Boolean textsEnabled = deviceSettings.getBoolean("texts");
		PersistentData.setTextsEnabled(textsEnabled);
		Boolean wifiEnabled = deviceSettings.getBoolean("wifi");
		PersistentData.setWifiEnabled(wifiEnabled);
		Boolean bluetoothEnabled = deviceSettings.getBoolean("bluetooth");
		PersistentData.setBluetoothEnabled(bluetoothEnabled);
		Boolean powerStateEnabled = deviceSettings.getBoolean("power_state");
		PersistentData.setPowerStateEnabled(powerStateEnabled);

		// Ambient audio collection. This key was added late, and if the server is old it may not be present.
		boolean ambientAudioCollectionIsEnabled;
		try { ambientAudioCollectionIsEnabled = deviceSettings.getBoolean("ambient_audio"); }
		catch (JSONException e) { ambientAudioCollectionIsEnabled = false; }
		PersistentData.setAmbientAudioCollectionIsEnabled(ambientAudioCollectionIsEnabled);

		Boolean allowUploadOverCellularData; // This key was added late, and if the server is old it may not be present
		try { allowUploadOverCellularData = deviceSettings.getBoolean("allow_upload_over_cellular_data");}
		catch (JSONException e) { allowUploadOverCellularData = false; }
		PersistentData.setAllowUploadOverCellularData(allowUploadOverCellularData);
		
		// Write timer settings
		int accelerometerOffDuration = deviceSettings.getInt("accelerometer_off_duration_seconds");
		PersistentData.setAccelerometerOffDuration(accelerometerOffDuration);
		int accelerometerOnDuration = deviceSettings.getInt("accelerometer_on_duration_seconds");
		PersistentData.setAccelerometerOnDuration(accelerometerOnDuration);
		int gyroscopeOffDuration = deviceSettings.getInt("gyro_off_duration_seconds");
		PersistentData.setGyroscopeOffDuration(gyroscopeOffDuration);
		int gyroscopeOnDuration = deviceSettings.getInt("gyro_on_duration_seconds");
		PersistentData.setGyroscopeOnDuration(gyroscopeOnDuration);
		int bluetoothOnDurationSeconds = deviceSettings.getInt("bluetooth_on_duration_seconds");
		PersistentData.setBluetoothOnDuration(bluetoothOnDurationSeconds);
		int bluetoothTotalDurationSeconds = deviceSettings.getInt("bluetooth_total_duration_seconds");
		PersistentData.setBluetoothTotalDuration(bluetoothTotalDurationSeconds);
		int bluetoothGlobalOffsetSeconds = deviceSettings.getInt("bluetooth_global_offset_seconds");
		PersistentData.setBluetoothGlobalOffset(bluetoothGlobalOffsetSeconds);
		int checkForNewSurveysSeconds = deviceSettings.getInt("check_for_new_surveys_frequency_seconds");
		PersistentData.setCheckForNewSurveysFrequency(checkForNewSurveysSeconds);
		int createNewDataFilesFrequencySeconds = deviceSettings.getInt("create_new_data_files_frequency_seconds");
		PersistentData.setCreateNewDataFilesFrequency(createNewDataFilesFrequencySeconds);
		int gpsOffDurationSeconds = deviceSettings.getInt("gps_off_duration_seconds");
		PersistentData.setGpsOffDuration(gpsOffDurationSeconds);
		int gpsOnDurationSeconds = deviceSettings.getInt("gps_on_duration_seconds");
		PersistentData.setGpsOnDuration(gpsOnDurationSeconds);
		int secondsBeforeAutoLogout = deviceSettings.getInt("seconds_before_auto_logout");
		PersistentData.setTimeBeforeAutoLogout(secondsBeforeAutoLogout);
		int uploadDataFilesFrequencySeconds = deviceSettings.getInt("upload_data_files_frequency_seconds");
		PersistentData.setUploadDataFilesFrequency(uploadDataFilesFrequencySeconds);
		int voiceRecordingMaxTimeLengthSeconds = deviceSettings.getInt("voice_recording_max_time_length_seconds");
		PersistentData.setVoiceRecordingMaxTimeLength(voiceRecordingMaxTimeLengthSeconds);

		// wifi periodicity needs to have a minimum because it creates a new file every week
		int wifiLogFrequencySeconds = deviceSettings.getInt("wifi_log_frequency_seconds");
		if (wifiLogFrequencySeconds < 10){
			wifiLogFrequencySeconds = 10;
		}
		PersistentData.setWifiLogFrequency(wifiLogFrequencySeconds);
		
		// Write text strings
		String aboutPageText = deviceSettings.getString("about_page_text");
		PersistentData.setAboutPageText(aboutPageText);
		String callClinicianButtonText = deviceSettings.getString("call_clinician_button_text");
		PersistentData.setCallClinicianButtonText(callClinicianButtonText);
		String consentFormText = deviceSettings.getString("consent_form_text");
		PersistentData.setConsentFormText(consentFormText);
		String surveySubmitSuccessToastText = deviceSettings.getString("survey_submit_success_toast_text");
		PersistentData.setSurveySubmitSuccessToastText(surveySubmitSuccessToastText);

		// Anonymized hashing
		boolean useAnonymizedHashing; // This key was added late, and if the server is old it may not be present
		try { useAnonymizedHashing = deviceSettings.getBoolean("use_anonymized_hashing"); }
		catch (JSONException e) { useAnonymizedHashing = false; }
		PersistentData.setUseAnonymizedHashing(useAnonymizedHashing);

		// Use GPS Fuzzing
		boolean useGpsFuzzing; // This key was added late, and if the server is old it may not be present
		try { useGpsFuzzing = deviceSettings.getBoolean("use_gps_fuzzing"); }
		catch (JSONException e) { useGpsFuzzing = false; }
		PersistentData.setUseGpsFuzzing(useGpsFuzzing);

		// Call button toggles
		boolean callClinicianButtonEnabled;
		try { callClinicianButtonEnabled = deviceSettings.getBoolean("call_clinician_button_enabled"); }
		catch (JSONException e) { callClinicianButtonEnabled = true; }
		PersistentData.setCallClinicianButtonEnabled(callClinicianButtonEnabled);

		boolean callResearchAssistantButtonEnabled;
		try { callResearchAssistantButtonEnabled = deviceSettings.getBoolean("call_research_assistant_button_enabled"); }
		catch (JSONException e) { callResearchAssistantButtonEnabled = true; }
		PersistentData.setCallResearchAssistantButtonEnabled(callResearchAssistantButtonEnabled);
	}
}

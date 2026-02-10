package org.beiwe.app.survey;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.beiwe.app.R;
import org.beiwe.app.session.SessionActivity;
import org.beiwe.app.storage.AudioFileManager;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.ui.user.MainMenuActivity;
import org.beiwe.app.ui.utils.SurveyNotifications;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;



/**Audio Recorder Common
 * This is the superclass which the audio survey types extend.
 * Provides a GUI to record audio clips and save them as files.
 * Heavily based on code from:
 * http://developer.android.com/guide/topics/media/audio-capture.html
 *
 * @author Eli Jones, Josh Zagorsky */
public class AudioRecorderCommon extends SessionActivity {

	/**	This function is overridden here because audio recorder activities requires some different
	 * behavior to occur inside of SessionActivity.  This was the least complex way to handle that detail. */
	@Override public Boolean isAudioRecorderActivity() { return true; }

	protected static final String LOG_TAG = "AudioRecorderActivity";
	protected final float DISABLED_BUTTON_ALPHA = 0.5f;

	protected String unencryptedTempAudioFilePath; //This is to be overridden in subclasses.
	protected Boolean notEncrypting = true; //This is a lock on deleting that temp file
	protected Boolean currentlyRecording = false; //This flag is set when the app is recording

	// If everEncrypted is set to True then a file has been processed for submission.
	// everEncrypted is the flag we use on whether to pop the submitted toast.
	protected Boolean everEncrypted = false;
	protected Boolean currentlyPlaying = false; //this flag is set when the device is playing back a recording

	// Buttons
	protected Button playButton;
	protected Button recordingButton;
	protected Button saveButton;

	// Flag used to determine whether to display the play button (may no longer be necessary).
	protected static boolean displayPlaybackButton = false;

	protected MediaPlayer mediaPlayer = null; //Media player for audio playback.
	protected final Handler recordingTimeoutHandler = new Handler(); //handler for the recording timeout

	// Temporary audio file name.
	public static final String unencryptedTempAudioFileName = "unencryptedTempAudioFile";

	protected String surveyId;

	/**	To be overridden with the appropriate file extension in a subclass. */
	protected String getFileExtension() { throw new NullPointerException("BAD CODE."); }


	/**On create, the activity presents the message to the user, and only a record button.
	 * After recording, the app will present the user with the play button. */
	@Override
	public void onCreate( Bundle savedInstanceState ) {
		// set app to know user is taking a survey right now.
		PersistentData.setTakingSurvey();
		
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_audio_recorder );
		surveyId = getIntent().getStringExtra("surveyId");

		// grab the layout element objects that we will add questions to:
		MarkDownTextView textbox = (MarkDownTextView) findViewById(R.id.record_activity_textview );
		textbox.setText( getPromptText(surveyId, getApplicationContext() ) );
		// Handle file path issues with this variable
		
		unencryptedTempAudioFilePath = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + unencryptedTempAudioFileName;
		
		playButton = (Button) findViewById(R.id.play_button);
		recordingButton = (Button) findViewById(R.id.recording_button);
		saveButton = (Button) findViewById(R.id.done_button);

		Button callClinicianButton = (Button) findViewById(R.id.record_activity_call_clinician);
		if(PersistentData.getCallClinicianButtonEnabled()) {
			callClinicianButton.setText(PersistentData.getCallClinicianButtonText());
		}
		else {
			callClinicianButton.setVisibility(View.GONE);
		}

		// Each time the screen is flipped, the app checks if it's time to show the play button
		if (!displayPlaybackButton) { playButton.setVisibility(Button.INVISIBLE); }
		else { playButton.setVisibility(Button.VISIBLE) ; }

		// Disable the "Save" button; only enable it once you've made a recording
		disableSaveButton();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// if !isfinishing: The activity is getting restarted because the screen rotated.
		if (isFinishing()) {
			if (mediaPlayer != null) { stopPlaying(); }
			displayPlaybackButton = false;
			/* Delete the temporary, unencrypted audio file so that nobody can play it back after
	         * the user leaves this screen. may be redundant. */
			if(notEncrypting) { AudioFileManager.delete(unencryptedTempAudioFileName); }
			if (everEncrypted) {
				Toast.makeText(getApplicationContext(), PersistentData.getSurveySubmitSuccessToastText(), Toast.LENGTH_LONG).show();
			}
		}
		PersistentData.setNotTakingSurvey();
	}
	
	private static String getPromptText(String surveyId, Context appContext) {
		try {
			JSONObject contentArray = new JSONArray(PersistentData.getSurveyContent(surveyId)).getJSONObject(0);
			return contentArray.getString("prompt");
		} catch (JSONException e) {
			Log.e("Audio Survey", "audio survey received either no or invalid prompt text.");
			e.printStackTrace();
			return appContext.getString(R.string.record_activity_default_message);
		}
	}
	
	/*#########################################################
	#################### Recording Timeout ####################
	#########################################################*/

	/** Automatically stop recording if the recording runs longer than n seconds. */
	protected void startRecordingTimeout() {
		recordingTimeoutHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				showTimeoutToast();
				stopRecording();
			}
		}, PersistentData.getVoiceRecordingMaxTimeLength());
	}

	/** Show a Toast with message "the recording timed out after n minutes" */
	protected void showTimeoutToast() {
		Resources resources = getApplicationContext().getResources();
		String msg = (String) resources.getText(R.string.timeout_msg_1st_half);
		msg += ((float) PersistentData.getVoiceRecordingMaxTimeLength() / 60 / 1000);
		msg += resources.getText(R.string.timeout_msg_2nd_half);
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
	}

	/**Cancel the stop-recording timer (this should be called when
	 * stopRecording() has already been called somewhere else, so that we don't
	 * call stopRecording twice. */
	protected void cancelRecordingTimeout() { recordingTimeoutHandler.removeCallbacksAndMessages(null); }
	
	/*#########################################################
	################# Button functionalities ##################
	#########################################################*/

	/** When the user presses the "record" button toggle (start/stop) recording. */
	public void buttonRecordPressed(View view) {
		if (!currentlyRecording) { startRecording(); }
		else { stopRecording(); }
	}

	/** When the user presses the "play" button, toggle (start/stop) playback. */
	public void buttonPlayPressed(View view) {
		if (!currentlyPlaying) { startPlaying(); }
		else { stopPlaying(); }
	}

	//Ensure you override, need subclass's private variables, Java references the parent class if the function is not overridden would have to use an interface(?) for ensuring functionality.
	protected void startRecording() {
		currentlyRecording = true;
		// Toggles button
	    setRecordButtonToStop();
	    disableSaveButton();
	}

	//Ensure you override, need subclass's private variables, Java references the parent class if the function is not overridden would have to use an interface(?) for ensuring functionality.
	public void stopRecording(){
	    cancelRecordingTimeout();
	    currentlyRecording = false;
	    setRecordButtonToRecord();
	    disableRecordButton();
	}

	//Ensure you override, need subclass's private variables, Java references the parent class if the function is not overridden would have to use an interface(?) for ensuring functionality.
	/** Stops playing back the recording, and reset the button to "play" */
	protected void stopPlaying() {
		currentlyPlaying = false;
	    setPlayButtonTextToPlay();
		mediaPlayer.stop();
		mediaPlayer.reset();
		mediaPlayer.release();
		mediaPlayer = null;
	}

	//Ensure you override, need subclass's private variables, Java references the parent class if the function is not overridden would have to use an interface(?) for ensuring functionality.
	/** Starts playing back the recording */
	protected void startPlaying() {
		currentlyPlaying = true;
	    setPlayButtonTextToStop();
	    mediaPlayer = new MediaPlayer();
		try {
			// Play the temporary unencrypted file, because you can't read the encrypted file
			mediaPlayer.setDataSource(unencryptedTempAudioFilePath);
			mediaPlayer.prepare();
			mediaPlayer.start();
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) { stopPlaying(); }
			} );
		}
		catch (IOException e) { Log.e(LOG_TAG, "prepare() failed"); }
	}


	/** When the user presses "Done", just kill this activity and take them
	 * back to the last one; the audio file should already be saved, so we
	 * don't need to do anything other than kill the activity.  */
	public void buttonSavePressed(View v) { //I love the name of this function...
		if (currentlyRecording) { stopRecording(); }
		SurveyNotifications.dismissSurveyNotification( getApplicationContext(), surveyId );
		Intent activityIntent = new Intent(getApplicationContext(), MainMenuActivity.class);
		activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(activityIntent);
		finish();
	}


	/*#########################################################
	#################### Button Visibility ####################
	#########################################################*/

	/** This function should be called from subclasses, because the enhanced audio recorder probably
	 * needs to copy the file with the appropriate headers before it can be played back. */
	public void displayPlaybackButton(){
		playButton.setVisibility(Button.VISIBLE);
		displayPlaybackButton = true; }

	public void setPlayButtonTextToStop(){
		playButton.setText(getApplicationContext().getString(R.string.play_button_stop_text));
		playButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.stop_button, 0, 0); }

	public void setPlayButtonTextToPlay(){
		playButton.setText(getApplicationContext().getString(R.string.play_button_text));
		playButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.play_button, 0, 0); }

	public void setRecordButtonToStop(){
		recordingButton.setText( getApplicationContext().getString(R.string.record_button_stop_text) );
		recordingButton.setCompoundDrawablesWithIntrinsicBounds( 0, R.drawable.stop_recording_button, 0, 0 ); }

	public void setRecordButtonToRecord(){
		recordingButton.setText(getApplicationContext().getString(R.string.record_button_text));
		recordingButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.record_button, 0, 0); }

	public void enableRecordButton(){ recordingButton.setClickable(true); recordingButton.setAlpha(1f); }
	public void disableRecordButton(){ recordingButton.setClickable(false); recordingButton.setAlpha(DISABLED_BUTTON_ALPHA); }
	public void enableSaveButton(){ saveButton.setClickable(true); saveButton.setAlpha(1f); }
	public void disableSaveButton(){ saveButton.setClickable(false); saveButton.setAlpha(DISABLED_BUTTON_ALPHA); }

	/*#########################################################
	##################### Encryption ##########################
	#########################################################*/

	/** While encrypting the audio file we block out user interaction.*/
	protected class EncryptAudioFileTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			notEncrypting = false;
			disableRecordButton();
//			Log.d("audio recording", "started encrypting");
		}
		@Override
		protected Void doInBackground(Void... params) {
			String filename = AudioFileManager.generateNewEncryptedAudioFileName(surveyId, getFileExtension());
			AudioFileManager.encryptAudioFile(unencryptedTempAudioFilePath, filename, getApplicationContext());
			return null;
		}
		@Override
		protected void onPostExecute(Void arg) {
			notEncrypting = true;  // Can now delete audio file
			everEncrypted = true;  // can now show toast
			// If isFinishing(), the other call to delete the temp file won't get triggered, so do it here.
			if (isFinishing()) { AudioFileManager.delete(unencryptedTempAudioFileName); }
			enableSaveButton();
			enableRecordButton();
//			Log.d("audio recording", "finished encrypting");
		}
	}
}
package org.beiwe.app.survey;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import org.beiwe.app.storage.PersistentData;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class AudioRecorderActivity extends AudioRecorderCommon {    
	
	private int BIT_RATE = 64000;
	protected MediaRecorder mRecorder = null;

	@Override
	protected String getFileExtension() { return ".mp4"; }
		
	/*#########################################################
	################## Activity Overrides ##################### 
	#########################################################*/

	@Override
	public void onDestroy() {
		if (isFinishing()) { // If the activity is being finished()...
			if (mRecorder != null) { stopRecording(); }
		}
	    super.onDestroy();
	}
	
	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		//extract bit rate from survey parameters.  If this fails default to the default value (64000).
		try { JSONObject surveySettings = new JSONObject( PersistentData.getSurveySettings(surveyId) );
			Log.i("regular audio", PersistentData.getSurveySettings(surveyId));
			  BIT_RATE = surveySettings.getInt("bit_rate"); }
		catch (JSONException e) { e.printStackTrace();
			Log.e("Regular audio recording", "WUH-OH, no bit_rate found, using default (64000).");
		}
	}
	
	/*#########################################################
	################# Recording and Playing ################### 
	#########################################################*/
	
	/** Start recording from the device's microphone, uses MediaRecorder,
	 * output file is the unencryptedTempAudioFilePath */
	@Override
	protected void startRecording() {
		super.startRecording();
		mRecorder = new MediaRecorder();
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mRecorder.setOutputFile( unencryptedTempAudioFilePath );
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mRecorder.setAudioChannels(1);
		mRecorder.setAudioSamplingRate(44100);
		mRecorder.setAudioEncodingBitRate(BIT_RATE);
		
		try { mRecorder.prepare(); }
		catch (IOException e) { Log.e(LOG_TAG, "prepare() failed"); }
		startRecordingTimeout();
		mRecorder.start();
	}
	
	/** Stop recording, and reset the button to "record" */
	@Override
	public void stopRecording() {
		super.stopRecording();
		mRecorder.stop();
		mRecorder.reset();
		mRecorder.release();
		mRecorder = null;
	    //recorder has finished, can now display playback button()
	    displayPlaybackButton();
		// Encrypt the audio file as soon as recording is finished
		new EncryptAudioFileTask().execute();
	}
}
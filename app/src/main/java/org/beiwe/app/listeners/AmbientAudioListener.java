package org.beiwe.app.listeners;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import org.beiwe.app.MainService;
import org.beiwe.app.R;
import org.beiwe.app.Timer;
import org.beiwe.app.storage.AudioFileManager;
import org.beiwe.app.storage.PersistentData;
import org.beiwe.app.storage.TextFileManager;

import java.io.IOException;

/**
 * AmbientAudioListener is a Singleton class: it should only be started and instantiated once.
 */
public class AmbientAudioListener {
    private static AmbientAudioListener ambientAudioListenerInstance = null;
    private static Context appContext;
    private static MediaRecorder mRecorder;
    private static final String filenameExtension = ".mp4";
    private static final long fileDurationInMilliseconds = 15 * 60 * 1000;
    public static final String unencryptedTempAudioFilename = "tempUnencryptedAmbientAudioFile";
    public static String currentlyBeingWrittenEncryptedFilename = null;

    private AmbientAudioListener() {};

    private static String getUnencryptedAudioFilepath() {
        return appContext.getFilesDir().getAbsolutePath() + "/" + unencryptedTempAudioFilename;
    }

    public static synchronized void startRecording(Context applicationContext) {
        TextFileManager.writeDebugLogStatement("AmbientAudioListener.startRecording()");
        if (ambientAudioListenerInstance == null) {
            // Instantiate the AmbientAudioRecorder only if it has not yet been instantiated
            ambientAudioListenerInstance = new AmbientAudioListener();
            appContext = applicationContext;
        }
        if (mRecorder == null) {
            // Start the Media Recorder only if it is not currently running
            MediaRecorder mRecorder = null;
            
            try {
                mRecorder = new MediaRecorder();
                mRecorder.reset();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setOutputFile(getUnencryptedAudioFilepath());
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mRecorder.setAudioChannels(1);
                mRecorder.setAudioSamplingRate(44100);
                mRecorder.setAudioEncodingBitRate(64000);
            } catch (java.lang.RuntimeException e) {
                Log.e("AmbientAudioListener", "AmbientAudioListener device setup failed");
                e.printStackTrace();
                TextFileManager.writeDebugLogStatement("AmbientAudioListener device setup failed");
                TextFileManager.writeDebugLogStatement(e.getMessage());
                mRecorder = null;
                ambientAudioListenerInstance = null;
                return;
            }
            
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e("AmbientAudioListener", "MediaRecorder.prepare() failed");
                e.printStackTrace();
                TextFileManager.writeDebugLogStatement("AmbientAudioListener MediaRecorder.prepare() failed");
                TextFileManager.writeDebugLogStatement(e.getMessage());
            }
            mRecorder.start();
        
            // Set a timer for how long this should run before calling encryptAmbientAudioFile()
            long alarmTime = MainService.timer.setupExactSingleAlarm(fileDurationInMilliseconds, Timer.encryptAmbientAudioIntent);
            PersistentData.setMostRecentAlarmTime(appContext.getString(R.string.encrypt_ambient_audio_file), alarmTime);
        }
    }


    public static synchronized void encryptAmbientAudioFile() {
        TextFileManager.writeDebugLogStatement("AmbientAudioListener.encryptAmbientAudioFile()");
        if (ambientAudioListenerInstance != null && mRecorder != null) {
            // If the audio recorder exists, stop recording and start encrypting the file
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            new EncryptAmbientAudioFileTask().execute();
        }
    }


    private static class EncryptAmbientAudioFileTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            // Before doing anything else, set the filename of the encrypted file. This tells
            // TextFileManager.getAllUploadableFiles NOT to upload it until it's finished writing.
            currentlyBeingWrittenEncryptedFilename = AudioFileManager.generateNewEncryptedAudioFileName(null, filenameExtension);
        }

        @Override
        protected Void doInBackground(Void... params) {
            AudioFileManager.encryptAudioFile(getUnencryptedAudioFilepath(), currentlyBeingWrittenEncryptedFilename, appContext);
            return null;
        }

        @Override
        protected void onPostExecute(Void arg) {
            // Clear the filename, so TextFileManager.getAllUploadableFiles can now upload it
            currentlyBeingWrittenEncryptedFilename = null;
            // Set the Media Recorder back to null to free it up to run again
            mRecorder = null;
            // Delete the unencrypted temp audio file
            AudioFileManager.delete(unencryptedTempAudioFilename);
            // Restart recording
            startRecording(appContext);
        }
    }
}

package org.beiwe.app.listeners

import android.content.Context
import android.media.MediaRecorder
import android.os.AsyncTask
import android.util.Log
import org.beiwe.app.printe
import org.beiwe.app.storage.AudioFileManager
import org.beiwe.app.storage.AudioFileManager.generateAmbientEncryptedAudioFileName
import org.beiwe.app.storage.PersistentData
import org.beiwe.app.storage.TextFileManager
import java.io.IOException
import java.util.Date

/**
 * AmbientAudioListener is a !Singleton class: it is only instantiated once.
 * The pattern followed here is not in line with the rest of the codebase's listeners, but
 * might be the more correct pattern avoiding application context leaks, which I don't understand anyway.
 */

const val filenameExtension = ".mp4"
const val ambientTempAudioFilename = "tempUnencryptedAmbientAudioFile"
const val FILE_ERROR = "recovered potentially lost or corrupted audio file, timestamp will be inaccurate: "
const val AUDIO_FAIL_COUNT = "AmbientAudioListener device setup failed, count is at "
const val AMBIENT_SETUP_SUCCESS = "AmbientAudioListener device setup success after "

object AmbientAudioListener {
    private var mRecorder: MediaRecorder? = null
    var appContext: Context? = null

    @JvmField
    var currentlyWritingEncryptedFilename: String? = null
    private var deviceSetupCount = 0
    private var instantiated = false

    // if both class variables are instantiated then we SHOULD be recording, would have to watch
    // the audio file size to confirm I guess?
    val isCurrentlyRunning: Boolean
        get() = instantiated && mRecorder != null

    val unencryptedAudioFilepath: String
        get() = appContext!!.filesDir.absolutePath + "/" + ambientTempAudioFilename

    val ambient_audio_off_action = {
        encryptAmbientAudioFile()
    }

    @Synchronized
    fun startRecording(applicationContext: Context) {
        PersistentData.ambientAudioStart = Date(System.currentTimeMillis()).toLocaleString()
        TextFileManager.writeDebugLogStatement("AmbientAudioListener.startRecording()")

        if (!instantiated) {  // first run
            appContext = applicationContext
            // if there is an extant temp audio file and the ambient recording feature is just
            // starting up then we need to encrypt the existing file.
            val temp_audio_file = applicationContext.getFileStreamPath(ambientTempAudioFilename)
            if (temp_audio_file.exists() && currentlyWritingEncryptedFilename == null) {
                forceEncrypt() // for now we don't care if this stalls the background service for a second
            }
            instantiated = true
        }

        // Start the Media Recorder only if it is not currently running
        if (mRecorder == null) {
            try {
                deviceSetupCount += 1
                mRecorder = MediaRecorder(appContext!!)
                mRecorder!!.reset()
                mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mRecorder!!.setOutputFile(unencryptedAudioFilepath)
                mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                mRecorder!!.setAudioChannels(1)
                mRecorder!!.setAudioSamplingRate(PersistentData.getAmbientAudioSampleRate().toInt())
                mRecorder!!.setAudioEncodingBitRate(PersistentData.getAmbientAudioBitrate().toInt())
                TextFileManager.writeDebugLogStatement(AMBIENT_SETUP_SUCCESS + deviceSetupCount + " attempt(s)")
            } catch (e: RuntimeException) {
                // TODO: does this occur only when certain security details (device lock) are active,
                //  or is it truly manufacturer-based.
                // this causes a lot of spam when the screen is off and the microphone is being turned on.
                printe("AmbientAudioListener1", AUDIO_FAIL_COUNT + deviceSetupCount)
                e.printStackTrace()
                if (deviceSetupCount == 1) {
                    TextFileManager.writeDebugLogStatement("AmbientAudioListener device setup failed")
                    TextFileManager.writeDebugLogStatement(e.message)
                }
                mRecorder = null
                return
            } catch (e: NoSuchMethodError) {
                // this error occurrs on old versions of android, 2013 nexus 7 running android 8.
                // because MediaRecorder doesn't appear to exist:
                // No direct method <init>(Landroid/content/Context;)V in class Landroid/media/MediaRecorder; or its super classes (declaration of 'android.media.MediaRecorder' appears in /system/framework/framework.jar)
                printe("AmbientAudioListener2", AUDIO_FAIL_COUNT + deviceSetupCount)
                e.printStackTrace()
                if (deviceSetupCount == 1) {
                    TextFileManager.writeDebugLogStatement("AmbientAudioListener device setup failed due to incompatible android version")
                    TextFileManager.writeDebugLogStatement(e.message)
                }
                mRecorder = null
                return
            }

            deviceSetupCount = 0  // reset setup attempt count

            try {
                mRecorder!!.prepare()
            } catch (e: IOException) {
                Log.e("AmbientAudioListener", "MediaRecorder.prepare() failed")
                e.printStackTrace()
                TextFileManager.writeDebugLogStatement("AmbientAudioListener MediaRecorder.prepare() failed")
                TextFileManager.writeDebugLogStatement(e.message)
                return
            }
            mRecorder!!.start()  // ok now actually start (never seen to fail)
        }
    }

    @Synchronized
    fun encryptAmbientAudioFile() {
        PersistentData.ambientAudioStop = Date(System.currentTimeMillis()).toLocaleString()
        TextFileManager.writeDebugLogStatement("AmbientAudioListener.encryptAmbientAudioFile()")
        // If the audio recorder exists, stop recording and start encrypting the file
        if (instantiated && mRecorder != null) {
            mRecorder!!.stop()  //FIXME this line can crash with an illegal state exception
            mRecorder!!.reset()
            mRecorder!!.release()
            EncryptAmbientAudioFileTask().execute()  // executes on a thread and then starts a new recording
        }
    }

    private fun forceEncrypt() {
        // we need an encrypt capacity in case the app starts and sees an unencrypted audio file.
        currentlyWritingEncryptedFilename = generateAmbientEncryptedAudioFileName(filenameExtension)
        AudioFileManager.encryptAudioFile(
                unencryptedAudioFilepath,
                currentlyWritingEncryptedFilename,
                appContext
        )
        AudioFileManager.delete(ambientTempAudioFilename)
        currentlyWritingEncryptedFilename = null
        TextFileManager.writeDebugLogStatement(FILE_ERROR + currentlyWritingEncryptedFilename)
    }

    //TODO: make this thread, not an asynctask
    private class EncryptAmbientAudioFileTask : AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            // Before doing anything else, set the filename of the encrypted file. This tells
            // TextFileManager.getAllUploadableFiles NOT to upload it until it's finished writing.
            // TODO: currently using null for special behavior; build the special behavior.
            currentlyWritingEncryptedFilename = generateAmbientEncryptedAudioFileName(filenameExtension)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            AudioFileManager.encryptAudioFile(
                    unencryptedAudioFilepath,
                    currentlyWritingEncryptedFilename,
                    appContext
            )
            return null
        }

        override fun onPostExecute(arg: Void?) {
            // Clear the filename, so TextFileManager.getAllUploadableFiles can now upload it
            currentlyWritingEncryptedFilename = null
            // Set the Media Recorder back to null to free it up to run again (apparently necessary?)
            mRecorder = null
            // Delete the unencrypted temp audio file, Restart recording
            AudioFileManager.delete(ambientTempAudioFilename)
            startRecording(appContext!!)
        }
    }
}

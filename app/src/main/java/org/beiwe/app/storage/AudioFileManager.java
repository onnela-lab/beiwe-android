package org.beiwe.app.storage;

import android.content.Context;
import android.util.Log;

import org.beiwe.app.BuildConfig;
import org.beiwe.app.CrashHandler;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;

// Over time TextFileManager got complex enough that it was easier to separate out code for writing
// binary files into its own class. This isn't greate for maintainability...

public class AudioFileManager {
	
	public static void delete (String fileName) {
		TextFileManager.delete(fileName);
	}
	
	/** Filename includes the time the recording is encrypted. */
	public static String generateNewEncryptedAudioFileName (String surveyId, String filenameExtension) {
		String timecode = ((Long) (System.currentTimeMillis() / 1000L)).toString();
		String patientId = PersistentData.getPatientID();
		return patientId + "_voiceRecording_" + surveyId + "_" + timecode + filenameExtension;
	}

	/** Reads in the existing temporary audio file and encrypts it. Generates AES keys as needed.
	 * Behavior is to spend as little time writing the file as possible, at the expense of memory.*/
	public static void encryptAudioFile (String unencryptedTempAudioFilePath, String filename, Context appContext) {
		if (unencryptedTempAudioFilePath != null) {
			// If the audio file has been written to, encrypt the audio file
			byte[] aesKey = EncryptionEngine.newAESKey();
			String encryptedRSA = null;
			String encryptedAudio = null;
			try {
				encryptedRSA = EncryptionEngine.encryptRSA(aesKey);
				encryptedAudio = EncryptionEngine.encryptAES(readInAudioFile(unencryptedTempAudioFilePath, appContext), aesKey);
			} catch (InvalidKeySpecException e) {
				Log.e("AudioFileManager", "encrypted write operation to the audio file without a keyFile.");
				if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
					CrashHandler.writeCrashlog(e, appContext);
			} catch (InvalidKeyException e) {
				Log.e("AudioFileManager", "encrypted write operation to the audio file without an aes key? how is that even...");
				if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
					CrashHandler.writeCrashlog(e, appContext);
			}
			writePlaintext(encryptedRSA, filename, appContext);
			writePlaintext(encryptedAudio, filename, appContext);
		}
	}
	
	/** Writes string data to a the audio file. */
	public static synchronized void writePlaintext (String data, String outputFileName, Context appContext) {
		FileOutputStream outStream;
		try {  //We use MODE_APPEND because... we know it works.
			outStream = appContext.openFileOutput(outputFileName, Context.MODE_APPEND);
			outStream.write((data).getBytes());
			outStream.write("\n".getBytes());
			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e) {
			Log.e("AudioRecording", "could not find file to write to, " + outputFileName);
			e.printStackTrace();
			if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
				CrashHandler.writeCrashlog(e, appContext);
		} catch (IOException e) {
			Log.e("AudioRecording", "error in the write operation: " + e.getMessage());
			e.printStackTrace();
			if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
				CrashHandler.writeCrashlog(e, appContext);
		}
	}
	
	/** Reads a byte array of the current temp audio file's contents.
	 * @return byte array of file contents. */
	public static synchronized byte[] readInAudioFile (String unencryptedTempAudioFilePath, Context appContext) {
		DataInputStream dataInputStream;
		byte[] data = null;
		File file = new File(unencryptedTempAudioFilePath);
		try {  //Read the (data) input stream, into a bytearray.  Catch exceptions.
			dataInputStream = new DataInputStream(new FileInputStream(file));
			data = new byte[(int) file.length()];
			try {
				dataInputStream.readFully(data);
			} catch (IOException e) {
				Log.e("DataFileManager", "error reading " + unencryptedTempAudioFilePath);
				if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
					CrashHandler.writeCrashlog(e, appContext);
			}
			dataInputStream.close();
		} catch (FileNotFoundException e) {
			Log.e("AudioRecording", "file " + unencryptedTempAudioFilePath + " does not exist");
			if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
				CrashHandler.writeCrashlog(e, appContext);
		} catch (IOException e) {
			Log.e("AudioRecording", "could not close " + unencryptedTempAudioFilePath);
			if (BuildConfig.APP_IS_DEV || BuildConfig.APP_IS_BETA || BuildConfig.DEBUG)
				CrashHandler.writeCrashlog(e, appContext);
		}
		return data;
	}
	
	/** Used to transform a raw recording file into a wav file.
	 * @param inFilename File name of the raw file
	 * @param outFilename Name of the file to copy it to
	 * @param sampleRate The sample rate of the provided raw file
	 * @param bitDepth The bit depth (bits per sample) of the raw file
	 * @param bufferSize size of the read-in buffer */
	public static void copyToWaveFile (String inFilename, String outFilename, long sampleRate, int bitDepth, int bufferSize) {
		int channels = 1;
		long byteRate = (bitDepth * sampleRate * channels) / 8;
		byte[] data = new byte[bufferSize];
		try {
			FileInputStream rawFileIn = new FileInputStream(inFilename);
			FileOutputStream waveFileOut = new FileOutputStream(outFilename);
			long totalAudioLen = rawFileIn.getChannel().size();
			long totalDataLen = totalAudioLen + 36;
			writeWaveFileHeader(waveFileOut, totalAudioLen, totalDataLen,
				sampleRate, channels, byteRate, bitDepth);
			
			while (rawFileIn.read(data) != -1) {
				waveFileOut.write(data);
			}
			
			rawFileIn.close();
			waveFileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**Handles the gory details of writing a wav header to the file.
	 * @param audioFile the output stream for the file.
	 * @param totalAudioLen number of bytes in the audio stream.
	 * @param totalDataLen number of bytes in the file total.
	 * @param longSampleRate sample rate of the wav file.
	 * @param channels number of channels (1)
	 * @param byteRate effective byte rate of the stream
	 * @param bitDepth bits per sample (sample depth)
	 * @throws IOException */
	private static void writeWaveFileHeader (FileOutputStream audioFile, long totalAudioLen, long totalDataLen,
	                                         long longSampleRate, int channels, long byteRate, int bitDepth) throws IOException {
		/* this was pulled, along with a bunch of other code, from
		 * //http://www.edumobile.org/android/audio-recording-in-wav-format-in-android-programming/ */
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = (byte) bitDepth; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		audioFile.write(header, 0, 44);
	}
}

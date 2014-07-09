package org.beiwe.app.storage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.methods.HttpGet;
import org.beiwe.app.R;

import android.content.Context;
import android.util.Log;

public class Upload {
	
	private Context appContext;
	
	public Upload(Context applicationContext) {
		this.appContext = applicationContext;
	}
	
	
	public void uploadAllFiles() {
		
		
	    // Run the HTTP GET on a separate, non-blocking thread
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<HttpGet> thread = new Callable<HttpGet>() {
			@Override
			public HttpGet call() {
				String[] files = TextFileManager.getAllFilesSafely();
				
				for (String fileName : files) {
					try {
						Log.i("Upload.java", "Trying to upload file: " + fileName);
						//TODO: this works for debugging, but you have to change TextFileManager.fileName to "public" 
						//tryToUploadFile(TextFileManager.getCurrentQuestionsFile().fileName);
						tryToUploadFile(fileName);
					}
					catch (Exception e) {
						Log.i("Upload.java", "Failed to upload file: " + fileName);
						//Log.w("Upload", "File " + fileName + " didn't exist");
						e.printStackTrace();
					}
				}
				Log.i("Upload.java", "Finished upload loop");				
				
				return null;
			}
		};
		executor.submit(thread);
	}
	
	
	private void tryToUploadFile(String filename) {
		try {
			String filePath = appContext.getFilesDir() + "/" + filename;
			File file = new File(filePath);

			URL uploadUrl = new URL(getUploadUrl(filename));
			PostRequestFileUpload postRequest = new PostRequestFileUpload();
			postRequest.sendPostRequest(file, uploadUrl);
		}
		catch (IOException e) {
			Log.w("Upload", "Failed to upload file. Raised exception " + e.getCause());
			e.printStackTrace();
		}
	}
	
	
	private String getUploadUrl(String filename) {
		String url = appContext.getResources().getString(R.string.data_upload_base_url);
		return url + "upload_gps";
		/* Upload URLs:
		 * http://54.204.178.17/upload_gps/
		 * http://54.204.178.17/upload_accel/
		 * http://54.204.178.17/upload_powerstate/
		 * http://54.204.178.17/upload_calls/
		 * http://54.204.178.17/upload_texts/
		 * http://54.204.178.17/upload_surveyresposne/
		 * http://54.204.178.17/upload_audio/
		 */
	}

}
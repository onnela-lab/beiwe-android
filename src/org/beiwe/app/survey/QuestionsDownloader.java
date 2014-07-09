package org.beiwe.app.survey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import org.beiwe.app.R;
import org.beiwe.app.storage.TextFileManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class QuestionsDownloader {

	private Context appContext;
	
	public QuestionsDownloader(Context applicationContext) {
		this.appContext = applicationContext; 
	}
	
	
	public String getJsonSurveyString() {
		
		Log.i("QuestionsDownloader", "Called getJsonSurveyString()");
		
		try {
			// Try to download the questions from the server
			String surveyQuestions = getSurveyQuestionsFromServer();
			
			// If it works, save a copy of those questions
			writeStringToFile(surveyQuestions);
			return surveyQuestions;
		}
		catch (Exception e) {
			e.printStackTrace();
			try {
				// If that failed, try loading questions from the local filesystem
				return getSurveyQuestionsFromFilesystem();
			}
			catch (Exception e2) {
				e2.printStackTrace();
				// Last resort: load questions that are hard-coded into the app
				return getSurveyQuestionsFromAppResources();
			}
		}
		
		// TODO: update the file in the local filesystem if you download a new 
		// one from the server, even if the download timed out
		
		// TODO: decide whether & how to handle the case where you pull a new file,
		// but you can't JSON-parse it.
		
		// TODO: handle the case where the user first takes a survey, but there
		// is no copy on the local filesystem (could be handled by the same code
		// as above (JSON-parsing error).
	}
	
	
	/**
	 * Read a file from the server, and return the file as a String 
	 * @throws NotFoundException 
	 * @throws IOException 
	 * @throws JSONException 
	 */
	private String getSurveyQuestionsFromServer() throws NotFoundException, IOException, JSONException {
		Log.i("QuestionsDownloader", "Called getSurveyQuestionsFromServer()");
		
		// Get the URL of the Survey Questions JSON file
		String urlString = appContext.getResources().getString(R.string.survey_questions_url);
		URL questionsFileURL = new URL(urlString);
		
		// Set up an HTTP connection
		HttpURLConnection connection = (HttpURLConnection) (questionsFileURL.openConnection());

		// Throw a TimeoutException after this many milliseconds if
		connection.setConnectTimeout(3000); // The server hasn't accepted the connection
		connection.setReadTimeout(5000); // This device hasn't received the response
		connection.connect();
		
		// Set up a BufferedReader from the HTTP connection
		InputStreamReader inputReader = new InputStreamReader(connection.getInputStream());
		BufferedReader reader = new BufferedReader(inputReader);
		
		// Set up a StringBuilder (uses less memory than repeatedly appending to a String)
		StringBuilder builder = new StringBuilder();
		String aux = "";
		
		// Read in the file from the BufferedReader, and append it to the StringBuilder
		while ((aux = reader.readLine()) != null) {
			builder.append(aux);
		}
		connection.disconnect();
		
		// Save the Survey Questions JSON file to the local filesystem
		String surveyQuestions = builder.toString();
		
		if (isValidJson(surveyQuestions)) {
			return surveyQuestions;
		}
		else {
			throw new JSONException("Invalid JSON");
		}
	}
	
	
	/**
	 * Read a file from the local Android filesystem, and return it as a String
	 * @throws JSONException 
	 */
	private String getSurveyQuestionsFromFilesystem() throws JSONException {
		Log.i("QuestionsDownloader", "Called getSurveyQuestionsFromFilesystem()");
		
		String surveyQuestions = TextFileManager.getCurrentQuestionsFile().read();

		if (isValidJson(surveyQuestions)) {
			return surveyQuestions;
		}
		else {
			throw new JSONException("Invalid JSON");
		}
	}
	
	
	/**
	 * Returns as a String the JSON survey file that's hard-coded into res/raw/
	 * @return
	 * @throws JSONException 
	 */
	private String getSurveyQuestionsFromAppResources() {
		Log.i("QuestionsDownloader", "Called getSurveyQuestionsFromAppResources()");
		
		InputStream inputStream = 
				appContext.getResources().openRawResource(R.raw.sample_survey);
		return fileToString(inputStream);		
	}
	
	
	/**
	 * Write a string (the JSON representation of survey questions) to a file
	 * in the local Android Filesystem
	 * @param string the JSON representation of survey questions
	 */
	private void writeStringToFile(String string) {
		TextFileManager.getCurrentQuestionsFile().deleteSafely();
		TextFileManager.getCurrentQuestionsFile().write(string);
	}

		
	/**
	 * Read a file (really an InputStream) and return a string
	 * @param inputStream the file you want to read
	 * @return String that contains the contents of the file
	 */
	private String fileToString(InputStream inputStream) {
		try {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
		    Reader reader;
			reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		    int n;
		    while ((n = reader.read(buffer)) != -1) {
		        writer.write(buffer, 0, n);
		    }
			inputStream.close();
			return writer.toString();
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
		catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	
	/**
	 * Tells you whether a String is valid JSON
	 * Based on: http://stackoverflow.com/a/10174938
	 * @param
	 * @return true if valid JSON; false otherwise
	 */
	private boolean isValidJson(String input) {
		try {
			new JSONObject(input);
		}
		catch (JSONException e) {
			try {
				new JSONArray(input);
			}
			catch (JSONException e2) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Get the current survey questions from the server as a JSON file
	 * @throws Exception
	 */
	/*private void updateSurveyQuestions() throws Exception {
	    // Run the HTTP GET on a separate, non-blocking thread
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<HttpGet> thread = new Callable<HttpGet>() {
			@Override
			public HttpGet call() throws Exception {
				// Copy the survey questions from the server
				getSurveyQuestionsFromServer();
				return null;
			}
		};
		executor.submit(thread);
	}*/
	
}
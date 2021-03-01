package org.beiwe.app.survey;

import android.content.Context;
import android.util.Log;

import org.beiwe.app.storage.TextFileManager;

public class SurveyTimingsRecorder {
	
	public static String header = "timestamp,question id,question type,question text,question answer options,answer";
		
	
	/**Create a new Survey Response file, and record the timestamp of when
	 * the survey first displayed to the user */
	public static void recordSurveyFirstDisplayed(String surveyId) {
		TextFileManager.getSurveyTimingsFile().newFile(surveyId);
		/* In the unlikely event that the user starts one survey, doesn't finish it, and starts a
		 * second survey, a new surveyTimingsFile should be created when the second survey is
		 * started, so hopefully there will never be more than one survey associated with a
		 * surveyTimingsFile. There should be no mechanism that allows a user to switch back and
		 * forth from one open survey to another, since there can be only one SurveyActivity. */
		
		String message = "Survey first rendered and displayed to user";
		appendLineToLogFile(message);
	}
	
	
	/**
	 * Record (in the Survey Response file) the answer to a single survey question
	 * @param answer the user's input answer
	 * @param questionData the question that was answered
	 */
	public static void recordAnswer(String answer, QuestionData questionData) {
		String message = "";
		message += sanitizeString(questionData.getId()) + TextFileManager.DELIMITER;
		message += sanitizeString(questionData.getType().getStringName()) + TextFileManager.DELIMITER;
		message += sanitizeString(questionData.getText()) + TextFileManager.DELIMITER;
		message += sanitizeString(questionData.getOptions()) + TextFileManager.DELIMITER;
		message += sanitizeString(answer);

		Log.i("SurveyResponse timings", message);
		appendLineToLogFile(message);
	}

	
	/**
	 * Record (in the Survey Response file) that the user pressed the "Submit" 
	 * button at the bottom of a survey
	 * @param appContext
	 */
	public static void recordSubmit(Context appContext) {
		String message = "User hit submit";
		appendLineToLogFile(message);
		TextFileManager.getSurveyTimingsFile().closeFile();
	}
	
	
	/**
	 * Write a line to the bottom of the Survey Response file
	 * @param message
	 */
	private static void appendLineToLogFile(String message) {
		/** Handles the logging, includes a new line for the CSV files.
		 * This code is otherwised reused everywhere.*/
		Long javaTimeCode = System.currentTimeMillis();
		String line = javaTimeCode.toString() + TextFileManager.DELIMITER + message; 

		TextFileManager.getSurveyTimingsFile().writeEncrypted(line);
	}

	
	/**
	 * Sanitize a string for use in a Tab-Separated Values file
	 * @param input string to be sanitized
	 * @return String with tabs and newlines removed
	 */
	public static String sanitizeString(String input) {
		input = input.replaceAll("[\t\n\r]", "  ");
		// Replace all commas in the text with semicolons, because commas are the delimiters
		input = input.replaceAll(",", ".");
		return input;
	}

}

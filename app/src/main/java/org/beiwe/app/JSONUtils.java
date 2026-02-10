package org.beiwe.app;

import android.util.Log;

import org.beiwe.app.storage.PersistentData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class JSONUtils {

	public static List<String> jsonArrayToStringList (JSONArray array) {
		ArrayList<String> ret = new ArrayList<String>(array.length());
		for (int i = 0; i < array.length(); i++) { //Wow, JSONArrays are not iterable.
			try {
				ret.add(array.getString(i));
			} catch (JSONException e) {
				throw new NullPointerException("unpacking json array failed, json string was: " + array.toString());
			}
		}
		return ret;
	}
	
	public static ArrayList<Integer> jsonArrayToIntegerList (JSONArray array) {
		ArrayList<Integer> ret = new ArrayList<Integer>(array.length());
		for (int i = 0; i < array.length(); i++) { //Wow, JSONArrays are not iterable.
			try {
				ret.add(array.getInt(i));
			} catch (JSONException e) {
				throw new NullPointerException("unpacking json array failed, json string was: " + array.toString());
			}
		}
		return ret;
	}
	
	//this is the hackiest...
	public static JSONArray stringListToJSONArray (List<String> list) {
		try {
			return new JSONArray(list.toString());
		} catch (JSONException e) {
			try {
				Log.e("JSONUtils", "a list could not be converted to json");
				e.printStackTrace();
				return new JSONArray(new ArrayList<String>().toString());
			} catch (JSONException e1) {
				throw new NullPointerException("The syntax of the toString function for arraylists is incorrect");
			}
		}
	}
	
	public static JSONArray shuffleJSONArray (JSONArray jsonArray, int numberElements) {
		List<String> javaList = JSONUtils.jsonArrayToStringList(jsonArray);
		Collections.shuffle(javaList, new Random(System.currentTimeMillis()));
		//if length supplied is 0 or greater than number of elements...
		if (numberElements == 0 || numberElements > javaList.size()) {
			jsonArray = JSONUtils.stringListToJSONArray(javaList);
		} else {
			jsonArray = JSONUtils.stringListToJSONArray(javaList.subList(0, numberElements));
		}
		return jsonArray;
	}
	
	
	public static JSONArray shuffleJSONArrayWithMemory (JSONArray jsonArray, int numberElements, String surveyId) {
		List<String> javaQuestionsList = JSONUtils.jsonArrayToStringList(jsonArray);
		Collections.shuffle(javaQuestionsList, new Random(System.currentTimeMillis()));
		List<String> returnList = new ArrayList<String>(numberElements);
		List<String> existingQuestionIds = PersistentData.getSurveyQuestionMemory(surveyId);
		
		JSONObject questionJSON = null;
		String questionId;
		
		for (String questionString: javaQuestionsList) {
			try {
				questionJSON = new JSONObject(questionString);
			} catch (JSONException e) {
				e.printStackTrace();
				throw new NullPointerException("question string is not a json object: " + questionString);
			}
			
			questionId = questionJSON.optString("question_id");
			if (questionId == null) {
				throw new NullPointerException("question_id does not exist in question...");
			}
			
			if (existingQuestionIds.contains(questionId)) {
				continue;
			} else {
				PersistentData.addSurveyQuestionMemory(surveyId, questionId);
				returnList.add(questionString);
				if (returnList.size() == numberElements) {
					break;
				}
			}
		}
		
		if (returnList.size() == 0 && javaQuestionsList.size() > 0) {
			PersistentData.clearSurveyQuestionMemory(surveyId);
			return shuffleJSONArrayWithMemory(jsonArray, numberElements, surveyId);
		}
		
		return JSONUtils.stringListToJSONArray(returnList);
	}
}

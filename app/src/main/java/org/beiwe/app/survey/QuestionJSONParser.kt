package org.beiwe.app.survey

import android.os.Bundle
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays

// TODO: clean up the usage of this parser and eliminate the need for the
//  getQuestionArgsFromJSONString function. We are currently calling it twice when displaying a
//  survey question, once in SurveyActivity, and once in SurveyFragment (its a couple function calls
//  deep). We should change it so that only the question ID is passed to QuestionFragment and shift
//  all options-parsing code to unique functions in this file.

object QuestionJSONParser {

	fun getQuestionArgsFromJSONString(jsonQuestion: JSONObject): Bundle {
		val args = Bundle()
		val questionType = getStringFromJSONObject(jsonQuestion, "question_type")
		args.putString("question_type", questionType)
		if (questionType == "slider")
			return getArgsForSliderQuestion(jsonQuestion, args)
		else if (questionType == "radio_button")
			return getArgsForRadioButtonQuestion(jsonQuestion, args)
		else if (questionType == "checkbox")
			return getArgsForCheckboxQuestion(jsonQuestion, args)
		else if (questionType == "free_response")
			return getArgsForFreeResponseQuestion(jsonQuestion, args)
		else if (questionType == "date_time" || questionType == "time" || questionType == "date" || questionType == "info_text_box")
			return getDefaultArgsForQuestion(jsonQuestion, args)
		throw RuntimeException("Invalid question type: $questionType")
	}

	// The dfault parameters for all questions, question_id and question_text
	fun getDefaultArgsForQuestion(jsonQuestion: JSONObject, args: Bundle): Bundle {
		args.putString("question_id", getStringFromJSONObject(jsonQuestion, "question_id"))
		args.putString("question_text", getStringFromJSONObject(jsonQuestion, "question_text"))
		return args
	}

	// Gets and cleans the parameters necessary to create a Slider Question
	fun getArgsForSliderQuestion(jsonQuestion: JSONObject, args: Bundle): Bundle {
		getDefaultArgsForQuestion(jsonQuestion, args)
		args.putInt("min", getIntFromJSONObject(jsonQuestion, "min"))
		args.putInt("max", getIntFromJSONObject(jsonQuestion, "max"))
		return args
	}

	// Gets and cleans the parameters necessary to create a Radio Button Question
	fun getArgsForRadioButtonQuestion(jsonQuestion: JSONObject, args: Bundle): Bundle {
		getDefaultArgsForQuestion(jsonQuestion, args)
		args.putStringArray("answers", getStringArrayFromJSONObject(jsonQuestion, "answers"))
		return args
	}

	// Gets and cleans the parameters necessary to create a Checkbox Question
	fun getArgsForCheckboxQuestion(jsonQuestion: JSONObject, args: Bundle): Bundle {
		getDefaultArgsForQuestion(jsonQuestion, args)
		args.putStringArray("answers", getStringArrayFromJSONObject(jsonQuestion, "answers"))
		return args
	}

	// Gets and cleans the parameters necessary to create a Free-Response Question
	fun getArgsForFreeResponseQuestion(jsonQuestion: JSONObject, args: Bundle): Bundle {
		getDefaultArgsForQuestion(jsonQuestion, args)
		args.putInt("text_field_type", getTextFieldTypeAsIntFromJSONObject(jsonQuestion, "text_field_type"))
		return args
	}

	/**Get a String from a JSONObject key
	 * @param obj a generic JSONObject
	 * @param key the JSON key
	 * @return return an empty String instead of throwing a JSONException */
	fun getStringFromJSONObject(obj: JSONObject, key: String): String {
		return try {
			obj.getString(key)
		} catch (e: JSONException) {
			""
		}
	}

	/**Get an int from a JSONObject key
	 * @param obj a generic JSONObject
	 * @param key the JSON key
	 * @return return -1 instead of throwing a JSONException */
	fun getIntFromJSONObject(obj: JSONObject, key: String): Int {
		return try {
			obj.getInt(key)
		} catch (e: JSONException) {
			-1
		}
	}

	/**Get an array of Strings from a JSONObject key
	 * @param obj a generic JSONObject
	 * @param key the JSON key
	 * @return return a one-String array instead of throwing a JSONException */
	fun getStringArrayFromJSONObject(obj: JSONObject, key: String): Array<String?> {
		val jsonArray: JSONArray
		jsonArray = try {
			obj.getJSONArray(key)
		} catch (e1: JSONException) {
			return arrayOf("")
		}
		val strings = arrayOfNulls<String>(jsonArray.length())
		for (i in 0 until jsonArray.length()) {
			try {
				strings[i] = jsonArray.getJSONObject(i).getString("text")
			} catch (e: JSONException) {
				strings[i] = ""
			}
		}
		return strings
	}

	/**Get an Enum TextFieldType from a JSONObject key
	 * @param obj a generic JSONObject
	 * @param key the JSON key
	 * @return return SINGLE_LINE_TEXT as the default instead of throwing a JSONException */
	fun getTextFieldTypeAsIntFromJSONObject(obj: JSONObject, key: String): Int {
		return try {
			TextFieldType.Type.valueOf(obj.getString(key)).ordinal
		} catch (e: JSONException) {
			TextFieldType.Type.SINGLE_LINE_TEXT.ordinal
		}
	}


	/** takes a question json, get's the type, hands it off to the appropriate QuestionData creation
	 * method.  */
	fun getQuestionDataFromJSONString(jsonQuestion: JSONObject): QuestionData {
		val args = Bundle()
		val questionType = getStringFromJSONObject(jsonQuestion, "question_type")
		args.putString("question_type", questionType)
		if (questionType == "slider")
			return slider_QuestionData(jsonQuestion)
		else if (questionType == "radio_button")
			return radio_button_QuestionData(jsonQuestion)
		else if (questionType == "checkbox")
			return checkbox_QuestionData(jsonQuestion)
		else if (questionType == "free_response")
			return free_response_QuestionData(jsonQuestion)
		else if (questionType == "date_time")
			return date_time_QuestionData(jsonQuestion)
		else if (questionType == "time")
			return time_QuestionData(jsonQuestion)
		else if  (questionType == "date")
			return date_QuestionData(jsonQuestion)
		else if ( questionType == "info_text_box")
			return info_text_box_QuestionData()
		throw RuntimeException("Invalid question type: $questionType")
	}

	fun info_text_box_QuestionData(): QuestionData {
		return QuestionData(null, QuestionType.Type.INFO_TEXT_BOX, null, null)
	}

	fun slider_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getArgsForSliderQuestion(jsonQuestion, Bundle())
		val min = args.getInt("min")
		val max = args.getInt("max")
		val options = "min = $min; max = $max" // options state min and max.
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.SLIDER,
				args.getString("question_text"),
				options
		)
	}

	fun radio_button_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getArgsForRadioButtonQuestion(jsonQuestion, Bundle())
		// val answers = args.getStringArray("answers").toString()
		val answers_array = args.getStringArray("answers")
		val answers = Arrays.toString(answers_array)
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.RADIO_BUTTON,
				args.getString("question_text"),
				answers
		)
	}

	fun checkbox_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getArgsForCheckboxQuestion(jsonQuestion, Bundle())
		val answers_array = args.getStringArray("answers")
		val answers = Arrays.toString(answers_array)
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.CHECKBOX,
				args.getString("question_text"),
				answers
			)
	}

	fun date_time_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getDefaultArgsForQuestion(jsonQuestion, Bundle())
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.DATE_TIME,
				args.getString("question_text"),
				""
		)
	}

	fun time_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getDefaultArgsForQuestion(jsonQuestion, Bundle())
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.TIME,
				args.getString("question_text"),
				""
		)
	}

	fun date_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getDefaultArgsForQuestion(jsonQuestion, Bundle())
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.DATE,
				args.getString("question_text"),
				""
		)
	}

	fun free_response_QuestionData(jsonQuestion: JSONObject): QuestionData {
		val args = getArgsForFreeResponseQuestion(jsonQuestion, Bundle())
		val textFieldTypeInt = args.getInt("text_field_type")
		val inputTextType = TextFieldType.Type.values()[textFieldTypeInt]
		val options = "Text-field input type = $inputTextType" // options indicates free response type
		return QuestionData(
				args.getString("question_id"),
				QuestionType.Type.FREE_RESPONSE,
				args.getString("question_text"),
				options
		)
	}

}
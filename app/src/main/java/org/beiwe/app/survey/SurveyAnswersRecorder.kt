package org.beiwe.app.survey

import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatCheckBox
import org.beiwe.app.R
import org.beiwe.app.storage.TextFileManager

class SurveyAnswersRecorder {
	/**Create a line (that will get written to a CSV file) that includes
	 * question metadata and the user's answer
	 * @param questionData metadata on the question
	 * @return a String that can be written as a line to a file */
	fun generateAnswerFileLine(questionData: QuestionData): String {
		var line = ""
		line += SurveyTimingsRecorder.sanitizeString(questionData.id)
		line += TextFileManager.DELIMITER
		line += SurveyTimingsRecorder.sanitizeString(questionData.type.stringName)
		line += TextFileManager.DELIMITER
		line += SurveyTimingsRecorder.sanitizeString(questionData.text)
		line += TextFileManager.DELIMITER
		line += SurveyTimingsRecorder.sanitizeString(questionData.answerOptions)
		line += TextFileManager.DELIMITER
		line += if (questionData.answerString == null || questionData.answerString == "") {
			NO_ANSWER_SELECTED
		} else {
			SurveyTimingsRecorder.sanitizeString(questionData.answerString)
		}
		// printe(line)
		// printe("finished line generation")
		return line
	}

	/** Create a new SurveyAnswers file, and write all of the answers to it
	 * @return TRUE if wrote successfully; FALSE if caught an exception */
	fun writeLinesToFile(surveyId: String?, surveyLogic: JsonSkipLogic): Boolean {
		try {
			TextFileManager.getSurveyAnswersFile().newFile(surveyId)
			for (questionData: QuestionData in surveyLogic.questionsForSerialization()) {
				val line = generateAnswerFileLine(questionData)
				// printe("SurveyResponse answers", line)
				TextFileManager.getSurveyAnswersFile().writeEncrypted(line)
			}
			TextFileManager.getSurveyAnswersFile().closeFile()
			return true
		} catch (e: Exception) {
			e.printStackTrace() // this isn't working??
			throw e  // this is absolutely critical code, if it fails we want to know about it.
			return false
		}
	}

	// The commented-out functions below are old code, the factoring was not great, commented out
	// ones were inlined into QuestionFragment.kt.

	companion object {
		@JvmField
		var header = "question id,question type,question text,question answer options,answer"

		const val NO_ANSWER_SELECTED = "NO_ANSWER_SELECTED"
		// const val errorString = "ERROR_QUESTION_NOT_RECORDED"

		/** Return a String representation of the answer to a question. If the question is not answered,
		 * return null.  */
		// fun getAnswerString(questionLayout: View, questionType: QuestionType.Type): String? {
		//     return when {
		//         questionType === QuestionType.Type.SLIDER ->
		//             answerFromSliderQuestion(questionLayout)
		//
		//         questionType === QuestionType.Type.RADIO_BUTTON ->
		//             answerFromRadioButtonQuestion(questionLayout)
		//
		//         questionType === QuestionType.Type.CHECKBOX ->
		//             answerFromCheckboxQuestion(questionLayout)
		//
		//         questionType === QuestionType.Type.FREE_RESPONSE ->
		//             answerFromOpenResponseQuestion(questionLayout)
		//
		//         // questionType === QuestionType.Type.DATE ->
		//         //     answerFromDateResponseQuestion(questionLayout)
		//         //
		//         // questionType === QuestionType.Type.TIME ->
		//         //     answerFromTimeResponseQuestion(questionLayout)
		//         //
		//         // questionType === QuestionType.Type.DATE_TIME ->
		//         //     answerFromDateTimeResponseQuestion(questionLayout)
		//
		//         questionType === QuestionType.Type.INFO_TEXT_BOX ->
		//             null
		//
		//         else -> throw RuntimeException("Question type $questionType not implemented")
		//     }
		// }

		// /**Get the answer from a Slider Question
		//  * @return the answer as a String */
		// fun answerFromSliderQuestion(questionLayout: View): String? {
		//     // return a null instead of a string of "null" on no answer.
		//     val answer = nullableIntAnswerFromSliderQuestion(questionLayout)?: return null
		//     return answer.toString()
		// }

		// fun nullableIntAnswerFromSliderQuestion(questionLayout: View): Int? {
		//     val slider = questionLayout.findViewById<View>(R.id.slider) as SeekBarEditableThumb
		//     return if (slider.hasBeenTouched) slider.progress + slider.min else null
		// }

		/**Get the answer from an Open Response question
		 * @return the answer as a String */
		fun answerFromOpenResponseQuestion(questionLayout: View): String? {
			val textFieldContainer = questionLayout.findViewById<View>(R.id.textFieldContainer) as LinearLayout
			val textField = textFieldContainer.getChildAt(0) as EditText
			val answer = textField.text.toString()
			return if (answer != null && answer != "") answer else null
		}

		// fun answerFromDateResponseQuestion(questionLayout: View): String? {
		//     val dateFieldContainer = questionLayout.findViewById<View>(R.id.dateFieldContainer) as LinearLayout
		//     val dateField = dateFieldContainer.getChildAt(0) as EditText
		//     val answer = dateField.text.toString()
		//     return if (answer != null && answer != "") answer else null
		// }

		/**Get the answer from a Radio Button Question
		 * @return the answer as a String */
		fun answerFromRadioButtonQuestion(questionLayout: View): String? {
			val selectedRadioButtonIndex = indexOfSelectedRadioButton(questionLayout)
			Log.e("SurveyAnswersRecorder", "selected answer index: $selectedRadioButtonIndex")
			if (selectedRadioButtonIndex != null) {
				val radioGroup = questionLayout.findViewById<View>(R.id.radioGroup) as RadioGroup
				val selectedButton = radioGroup.getChildAt(selectedRadioButtonIndex) as RadioButton
				return selectedButton.text.toString()
			}
			return null
		}

		fun indexOfSelectedRadioButton(questionLayout: View): Int? {
			val radioGroup = questionLayout.findViewById<View>(R.id.radioGroup) as RadioGroup
			for (i in 0 until radioGroup.childCount) {
				if ((radioGroup.getChildAt(i) as RadioButton).isChecked)
					return i
			}
			return null
		}

		/**Get the answer from a Checkbox Question
		 * @return the answer as a String */
		fun answerFromCheckboxQuestion(questionLayout: View): String? {
			val checkboxesList = questionLayout.findViewById<View>(R.id.checkboxesList) as LinearLayout
			val selectedAnswers = selectedCheckboxes(checkboxesList)
			return if (selectedAnswers != "[]") selectedAnswers else null
		}

		/**Return a list of the selected checkboxes in a list of checkboxes
		 * @param checkboxesList a LinearLayout, presumably containing only checkboxes
		 * @return a String formatted like a String[] printed to a single String */
		fun selectedCheckboxes(checkboxesList: LinearLayout): String {
			// This is the ORIGINAL format - a list of the checked answers that reads like a printed array of strings
			// when this data is serialied to a file the commas get replaced with semicolons. (we want to change this)
			var answersList = "["

			// Iterate over the whole list of CheckBoxes in this LinearLayout
			for (i in 0 until checkboxesList.childCount) {
				val childView = checkboxesList.getChildAt(i)
				if (childView is CheckBox) {
					val checkBox = childView
					// print("checkbox: " + checkBox.getText() + " is checked: " + checkBox.isChecked());
					// If this CheckBox is selected, add it to the list of selected answers
					if (checkBox.isChecked)
						answersList += checkBox.text.toString() + ", "
				}
			}

			// Trim the last comma off the list so that it's formatted like a String[] printed to a String
			if (answersList.length > 3)
				answersList = answersList.substring(0, answersList.length - 2)

			// close list and return
			answersList += "]"
			return answersList
		}

		// checkboxes may have many answers, assemble a list of the indices of the selected answers.
		fun indicesOfSelectedCheckboxButtons(questionLayout: View): MutableList<Int> {
			val questions_layout = questionLayout.findViewById<View>(R.id.checkboxesList) as LinearLayout
			// val numberOfChoices = radioGroup.childCount
			val selected_indices = mutableListOf<Int>()
			for (i in 0 until questions_layout.childCount) {
				if ((questions_layout.getChildAt(i) as AppCompatCheckBox).isChecked)
					selected_indices.add(i)
			}
			return selected_indices
		}

		// // externally accessed function
		// /** If the question is a radio button or slider question, return the answer as a nullable Java
		//  * Integer. If it's any other type of question, or if it wasn't answered, return null.  */
		// fun answerIntegerValue(questionLayout: View, questionType: QuestionType.Type): Int? {
		//     return if (questionType === QuestionType.Type.SLIDER)
		//         nullableIntAnswerFromSliderQuestion(questionLayout)
		//     else if (questionType === QuestionType.Type.RADIO_BUTTON)
		//         indexOfSelectedRadioButton(questionLayout)
		//     else
		//         null
		// }
	}
}
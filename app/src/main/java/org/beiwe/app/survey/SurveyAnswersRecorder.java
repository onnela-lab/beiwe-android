package org.beiwe.app.survey;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.beiwe.app.R;
import org.beiwe.app.storage.TextFileManager;

import java.util.List;

public class SurveyAnswersRecorder {
	public static String header = "question id,question type,question text,question answer options,answer";
	private static String noAnswer = "NO_ANSWER_SELECTED";
	private static String errorCode = "ERROR_QUESTION_NOT_RECORDED";


	/** Return a String representation of the answer to a question. If the question is not answered,
	 *  return null. */
	public static String getAnswerString(View questionLayout, QuestionType.Type questionType) {
		if (questionType == QuestionType.Type.SLIDER) {
			return SurveyAnswersRecorder.getStringAnswerFromSliderQuestion(questionLayout);
		} else if (questionType == QuestionType.Type.RADIO_BUTTON) {
			return SurveyAnswersRecorder.getAnswerFromRadioButtonQuestion(questionLayout);
		} else if (questionType == QuestionType.Type.CHECKBOX) {
			return SurveyAnswersRecorder.getAnswerFromCheckboxQuestion(questionLayout);
		} else if (questionType == QuestionType.Type.FREE_RESPONSE) {
			return SurveyAnswersRecorder.getAnswerFromOpenResponseQuestion(questionLayout);
		} else {
			return null;
		}
	}

	/** If the question is a radio button or slider question, return the answer as a nullable Java
	 *  Integer. If it's any other type of question, or if it wasn't answered, return null. */
	public static Integer getAnswerIntegerValue(View questionLayout, QuestionType.Type questionType) {
		if (questionType == QuestionType.Type.SLIDER) {
			return getNullableIntAnswerFromSliderQuestion(questionLayout);
			//TODO: Josh. Check if a slider in beiwe can be a floating point value.
		} else if (questionType == QuestionType.Type.RADIO_BUTTON) {
            return getIndexOfSelectedRadioButton(questionLayout);
		} else {
            return null;
        }
	}


	/**Get the answer from a Slider Question
	 * @return the answer as a String */
	public static String getStringAnswerFromSliderQuestion(View questionLayout) {
		Integer answer = getNullableIntAnswerFromSliderQuestion(questionLayout);
		if (answer == null) { return null; } //return a null instead of a string of "null" on no answer.
		return "" + getNullableIntAnswerFromSliderQuestion(questionLayout);
	}

	public static Integer getNullableIntAnswerFromSliderQuestion(View questionLayout) {
		SeekBarEditableThumb slider = (SeekBarEditableThumb) questionLayout.findViewById(R.id.slider);
		if (slider.getHasBeenTouched()) {
			return slider.getProgress() + slider.getMin();
		}
		return null;
	}


	/**Get the answer from a Radio Button Question
	 * @return the answer as a String */
	public static String getAnswerFromRadioButtonQuestion(View questionLayout) {
        Integer selectedRadioButtonIndex = getIndexOfSelectedRadioButton(questionLayout);
        Log.i("SurveyAnswersRecorder", "selected answer index: " + selectedRadioButtonIndex);
        if (selectedRadioButtonIndex != null) {
            RadioGroup radioGroup = (RadioGroup) questionLayout.findViewById(R.id.radioGroup);
            RadioButton selectedButton = (RadioButton) radioGroup.getChildAt(selectedRadioButtonIndex);
            return selectedButton.getText().toString();
        }
		return null;
	}

	private static Integer getIndexOfSelectedRadioButton(View questionLayout) {
		RadioGroup radioGroup = (RadioGroup) questionLayout.findViewById(R.id.radioGroup);
		int numberOfChoices = radioGroup.getChildCount();
		for (int i=0; i < numberOfChoices; i++) {
            if (((RadioButton) radioGroup.getChildAt(i)).isChecked()) {
                return i;
            }
		}
        return null;
	}


	/**Get the answer from a Checkbox Question
	 * @return the answer as a String */
	public static String getAnswerFromCheckboxQuestion(View questionLayout) {
		LinearLayout checkboxesList = (LinearLayout) questionLayout.findViewById(R.id.checkboxesList);
		String selectedAnswers = getSelectedCheckboxes(checkboxesList);
		if (selectedAnswers.equals("[]")) {
			return null;
		} else {
			return selectedAnswers;
		}
	}


	/**Get the answer from an Open Response question
	 * @return the answer as a String */
	public static String getAnswerFromOpenResponseQuestion(View questionLayout) {
		LinearLayout textFieldContainer = (LinearLayout) questionLayout.findViewById(R.id.textFieldContainer);
		EditText textField = (EditText) textFieldContainer.getChildAt(0);
		String answer = textField.getText().toString();
		if (answer == null || answer.equals("")) {
			return null;
		}
		return answer;
	}
	
	
	/**Create a line (that will get written to a CSV file) that includes
	 * question metadata and the user's answer
	 * @param questionData metadata on the question
	 * @return a String that can be written as a line to a file */
	private String answerFileLine(QuestionData questionData) {
		String line = "";
		line += SurveyTimingsRecorder.sanitizeString(questionData.getId());
		line += TextFileManager.DELIMITER;
		line += SurveyTimingsRecorder.sanitizeString(questionData.getType().getStringName());
		line += TextFileManager.DELIMITER;
		line += SurveyTimingsRecorder.sanitizeString(questionData.getText());
		line += TextFileManager.DELIMITER;
		line += SurveyTimingsRecorder.sanitizeString(questionData.getOptions());
		line += TextFileManager.DELIMITER;
		if (questionData.getAnswerString() == null) {
			line += noAnswer;
		} else {
			line += SurveyTimingsRecorder.sanitizeString(questionData.getAnswerString());
		}
		return line;
	}

	
	/** Create a new SurveyAnswers file, and write all of the answers to it
	 * @return TRUE if wrote successfully; FALSE if caught an exception */
	public Boolean writeLinesToFile(String surveyId, List<QuestionData> answers) {
		try {
			TextFileManager.getSurveyAnswersFile().newFile(surveyId);
			for (QuestionData answer : answers) {
				String line = answerFileLine(answer);
				Log.i("SurveyResponse answers", line);
				TextFileManager.getSurveyAnswersFile().writeEncrypted(line);
			}
			TextFileManager.getSurveyAnswersFile().closeFile();
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}


	/**
	 * Return a list of the selected checkboxes in a list of checkboxes
	 * @param checkboxesList a LinearLayout, presumably containing only checkboxes
	 * @return a String formatted like a String[] printed to a single String
	 */
	public static String getSelectedCheckboxes(LinearLayout checkboxesList) {

		// Make a list of the checked answers that reads like a printed array of strings
		String answersList = "[";

		// Iterate over the whole list of CheckBoxes in this LinearLayout
		for (int i = 0; i < checkboxesList.getChildCount(); i++) {

			View childView = checkboxesList.getChildAt(i);
			if (childView instanceof CheckBox) {
				CheckBox checkBox = (CheckBox) childView;

				// If this CheckBox is selected, add it to the list of selected answers
				if (checkBox.isChecked()) {
					answersList += checkBox.getText().toString() + "; ";
				}
			}
		}

		// Trim the last comma off the list so that it's formatted like a String[] printed to a String
		if (answersList.length() > 3) {
			answersList = answersList.substring(0, answersList.length() - 2);
		}
		answersList += "]";

		return answersList;
	}
}

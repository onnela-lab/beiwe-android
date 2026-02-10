package org.beiwe.app.survey;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.beiwe.app.R;

import java.util.ArrayList;

/**
 * Created by Josh Zagorsky on 12/10/16.
 */

public class SurveySubmitFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout surveySubmitLayout = (LinearLayout) inflater.inflate(R.layout.fragment_survey_submit, null);
		FrameLayout submitScreenContent = (FrameLayout) surveySubmitLayout.findViewById(R.id.submitScreenContent);
		ArrayList<String> unansweredQuestions = getArguments().getStringArrayList("unansweredQuestions");
		if (unansweredQuestions.size() > 0) {
			// If any questions haven't been answered, display the list of them with a "Submit Anyway" button
			submitScreenContent.addView(showUnansweredQuestionsListAndTheSubmitButton(inflater, unansweredQuestions));
		} else {
			// If all questions have been answered, just display the "Submit" button
			submitScreenContent.addView(showJustTheSubmitButton(inflater));
		}
		return surveySubmitLayout;
	}

	OnSubmitButtonClickedListener submitButtonClickedListener;


	// Interface for the "Submit" button to pass a signal back to the Activity
	public interface OnSubmitButtonClickedListener {
		void submitButtonClicked();
	}
	@Override
	/** This function will get called on NEW versions of Android (6+). */
	public void onAttach(Context context) {
		super.onAttach(context);
		submitButtonClickedListener = (OnSubmitButtonClickedListener) context;
	}
	@Override
	/** This function will get called on OLD versions of Android (<6). */
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		submitButtonClickedListener = (OnSubmitButtonClickedListener) activity;
	}


	private LinearLayout showJustTheSubmitButton(LayoutInflater inflater) {
		return renderSubmitButton(inflater, "Submit Answers");
	}


	private LinearLayout showUnansweredQuestionsListAndTheSubmitButton(LayoutInflater inflater,
																	   ArrayList<String> unansweredQuestions) {
		LinearLayout unansweredQuestionsLayout = (LinearLayout) inflater.inflate(R.layout.survey_unanswered_questions_list, null);
		// Show a message about the number of unanswered questions
		TextView unansweredQuestionsMessage = (TextView) unansweredQuestionsLayout.findViewById(R.id.unansweredQuestionsMessage);
		if (unansweredQuestions.size() == 1) {
			unansweredQuestionsMessage.setText("You did not answer 1 question:");
		} else {
			unansweredQuestionsMessage.setText("You did not answer " + unansweredQuestions.size() + " questions:");
		}
		
		// unansweredQuestionsMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // doesn't do anything
		
		// Show a list of the unanswered questions
		ListView unansweredQuestionsListView = (ListView) unansweredQuestionsLayout.findViewById(R.id.unansweredQuestionsListView);
		ArrayAdapter<String> adapter;
		try {
			adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, unansweredQuestions);
		} catch (NoSuchMethodError e) {
			adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, unansweredQuestions);
		}
		unansweredQuestionsListView.setAdapter(adapter);
		
		// Attach the submit button to the bottom of the list
		LinearLayout submitButton = (LinearLayout) renderSubmitButton(inflater, "Submit Answers Anyway");
		unansweredQuestionsListView.addFooterView(submitButton);
		return unansweredQuestionsLayout;
	}


	private LinearLayout renderSubmitButton(LayoutInflater inflater, String labelText) {
		LinearLayout submitButtonLayout = (LinearLayout) inflater.inflate(R.layout.survey_submit_button, null);
		Button submitButton = (Button) submitButtonLayout.findViewById(R.id.buttonSubmit);
		submitButton.setText(labelText);
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				submitButtonClickedListener.submitButtonClicked();
			}
		});
		
		Button backButton = (Button) submitButtonLayout.findViewById(R.id.backSubmitButton);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().onBackPressed();
			}
		});
		return submitButtonLayout;
	}
	
}

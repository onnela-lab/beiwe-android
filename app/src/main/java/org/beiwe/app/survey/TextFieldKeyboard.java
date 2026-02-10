package org.beiwe.app.survey;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * TextFieldKeyboard contains methods to improve the user experience with the
 * EditText (editable text-field) software keyboard.  When the user taps any-
 * where outside the currently opened EditText, the keyboard disappears.
 * 
 * @author Josh Zagorsky <josh@zagaran.com>  June-July 2014
 */
public class TextFieldKeyboard {
	
	private Context appContext;

	public TextFieldKeyboard(Context applicationContext) {
		this.appContext = applicationContext;
	}
	
	
	/**
	 * Set up the UI so that if the user taps outside the current EditText, the
	 * software keyboard goes away.
	 * THIS FUNCTION HAS ONE ANNOYING SIDE-EFFECT THAT MESSES WITH
	 * ONTOUCHLISTENERS.
	 * It works by setting onTouchListeners on EVERYTHING else in the view, so
	 * that when anything outside the keyboard gets touched, the keyboard
	 * disappears.  But objects can only have one onTouchListener at a time,
	 * so if you set another onTouchListener on anything else in the view, one
	 * onTouchListener will override the other.
	 * @param editText the EditText the user is currently typing in
	 */
	public void makeKeyboardBehave(EditText editText) {
		View topParentView = getTopParentView(editText);
		setupUiToHideKeyboard(topParentView, editText);
	}
	

	/**
	 * Gets the view's parent's parent's parent... and so on, until you find a parent-less view
	 * @param view the starting View
	 * @return the highest parent View available
	 */
	private View getTopParentView(View view) {
		ViewParent parent = view.getParent();
		if ((parent != null) && (parent instanceof View)) {
			return getTopParentView((View) parent);
		}
		else {
			return view;
		}
	}
	
	
	/**
	 * If you tap anywhere outside the given text box, hide the keyboard
	 * Based on: http://stackoverflow.com/a/11656129
	 * @param rootView the highest parent View available
	 * @param editText the EditText that the user is currently typing in
	 */
	private void setupUiToHideKeyboard(View rootView, final EditText editText) {
		/* If the user taps on an EditText; keep the keyboard open. If the user
		 * taps on a SeekBar (Slider); don't add an OnTouchListener, because
		 * that'll overwrite the Slider's existing OnTouchListener. */
		if(!(rootView instanceof EditText) && !(rootView instanceof SeekBarEditableThumb)) {
			rootView.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					hideSoftKeyboard(editText);
					return false;
				}
			});
		}

		//If a layout container, iterate over children and seed recursion.
		if (rootView instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) rootView).getChildCount(); i++) {
				View innerView = ((ViewGroup) rootView).getChildAt(i);
				setupUiToHideKeyboard(innerView, editText);
			}
		}
	}

	
	/**
	 * Hide the keyboard and close the Search text box
	 * Based on: http://stackoverflow.com/a/11656129
	 * @param editText the EditText that the user is currently typing in
	 */
	private void hideSoftKeyboard(EditText editText) {
		InputMethodManager imm = (InputMethodManager) appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
		
		editText.clearFocus();
		View topParentView = getTopParentView(editText);
		topParentView.requestFocus();
	}
	
}

package org.beiwe.app.survey

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.TimePicker
import org.beiwe.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Arrays


class QuestionFragment : Fragment() {
	// can't late-init, it errors with:
	// kotlin.UninitializedPropertyAccessException: lateinit property questionData has not been initialized
	// but it is initialized correctly in each createquestion function and cannot be missing at real runtime.
	var questionData: QuestionData? = null

	// late-init'd in onCreateView
	lateinit var the_questionLayout: View

	var goToNextQuestionListener: OnGoToNextQuestionListener? = null
	var questionType: QuestionType.Type? = null

	var nextButtonPreAction: (() -> Unit)? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		/* XML views inflated by an Activity render with the app's default style (set in the
		 * Manifest.XML), but for some reason, XML views inflated by this class don't render with
		 * the app's default style, unless we set it manually: */

		if (questionData == null) {
			// this call can return null, that means the question has not loaded nor saved an answer
			questionData = (activity as SurveyActivity).surveyLogic!!.currentQuestionData
			// if (questionData == null)
			//     printe("did not load a questionData object for question ${(activity as SurveyActivity).surveyLogic!!.currentQuestion + 1}")
			// else
			//     printe("loaded a questionData object! " + questionData)
		}

		// Render the question (inflate the layout) for this fragment
		val fragmentQuestionLayout = inflater.inflate(R.layout.fragment_question, null) as ScrollView
		val questionContainer = fragmentQuestionLayout.findViewById<View>(R.id.questionContainer) as FrameLayout

		// questionAnswer will only be late-init'd during createQuestion, the_questionLayout here
		this.the_questionLayout = createQuestion(inflater, arguments)
		questionContainer.addView(this.the_questionLayout)

		// Set an onClickListener for the next and back button (need to cast)
		val nextButton = fragmentQuestionLayout.findViewById<View>(R.id.nextButton) as Button
		val backButton = fragmentQuestionLayout.findViewById<View>(R.id.backButton) as Button

		// next button has a pre-action, assign a closure to nextButtonPreAction in the
		// createQuestion function and it will execute before the next button's regular action.
		nextButton.setOnClickListener {
			if (nextButtonPreAction != null)
				nextButtonPreAction!!()
			goToNextQuestionListener!!.goToNextQuestion(questionData!!)
			// getAnswer(this.the_questionLayout, questionType!!)) // comment this out last
		}
		backButton.setOnClickListener {
			activity.onBackPressed()
		}

		// conform to onCreateView's return type
		return fragmentQuestionLayout
	}


	/* The following dual declaration is due to a change/deprecation in the Android Fragment
	 * _handling_ code.  It is difficult to determine exactly which API version this change occurs
	 * in, but the linked stack overflow article assumes API 23 (6.0). The difference in the
	 * declarations is that one takes an activity, and one takes a context.  Starting in 6 the OS
	 * guarantees a call to the one that takes an activity, previous versions called the one that
	 * takes an activity.
	 * ...
	 * If one of these is missing then goToNextQuestionListener fails to instantiate, causing a
	 * crash inside the onClick function for the next button.
	 * ...
	 * http://stackoverflow.com/questions/32604552/onattach-not-called-in-fragment */

	/** This function will get called on NEW versions of Android (6+). ... and now that's been deprecated tooooo!  */
	override fun onAttach(context: Context) {
		super.onAttach(context)
		// printe("onAttach(Context) called")
		goToNextQuestionListener = context as OnGoToNextQuestionListener
	}

	/** This function will get called on OLD versions of Android (<6).  */
	override fun onAttach(activity: Activity) {
		super.onAttach(activity)
		// printe("onAttach(Activity) called")
		goToNextQuestionListener = activity as OnGoToNextQuestionListener
	}


	// executes the correct instantiation logic for the question's ui element
	private fun createQuestion(inflater: LayoutInflater, args: Bundle): View {
		// all optionals
		val questionID = args.getString("question_id")!! // required
		val questionType = args.getString("question_type")
		val questionText = args.getString("question_text")

		if (questionType == "info_text_box") {
			this.questionType = QuestionType.Type.INFO_TEXT_BOX
			return createInfoTextbox(inflater, questionID, questionText)
		} else if (questionType == "slider") {
			this.questionType = QuestionType.Type.SLIDER
			val min = args.getInt("min")
			val max = args.getInt("max")
			return createSliderQuestion(inflater, questionID, questionText, min, max)
		} else if (questionType == "radio_button") {
			this.questionType = QuestionType.Type.RADIO_BUTTON
			val answers = args.getStringArray("answers")
			return createRadioButtonQuestion(inflater, questionID, questionText, answers)
		} else if (questionType == "checkbox") {
			this.questionType = QuestionType.Type.CHECKBOX
			val answers = args.getStringArray("answers")
			return createCheckboxQuestion(inflater, questionID, questionText, answers)
		} else if (questionType == "free_response") {
			this.questionType = QuestionType.Type.FREE_RESPONSE
			val textFieldTypeInt = args.getInt("text_field_type")
			val textFieldType = TextFieldType.Type.values()[textFieldTypeInt]
			return createFreeResponseQuestion(inflater, questionID, questionText, textFieldType)
		} else if (questionType == "date") {
			this.questionType = QuestionType.Type.DATE
			return createDateQuestion(inflater, questionID, questionText)
		} else if (questionType == "time") {
			this.questionType = QuestionType.Type.TIME
			return createTimeQuestion(inflater, questionID, questionText)
		} else if (questionType == "date_time") {
			this.questionType = QuestionType.Type.DATE_TIME
			return createDateTimeQuestion(inflater, questionID, questionText)
		}
		// Default: return an error message
		return inflater.inflate(R.layout.survey_info_textbox, null)
	}


	/** Creates an informational text view that does not have an answer type
	 * @param infoText The informational text
	 * @return TextView (to be displayed as question text) */
	private fun createInfoTextbox(inflater: LayoutInflater, questionID: String, infoText: String?): TextView {
		// just blindly create a new questionData object, infotextboxes don't have answers.
		this.questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		var infoText = infoText // needs to be a var to be reassigned in the try-catch
		val infoTextbox = inflater.inflate(R.layout.survey_info_textbox, null) as MarkDownTextView

		// Clean inputs
		if (infoText == null) {
			// Set the question text to the error string - no clue what this try-catch is for
			infoText = try {
				context.resources.getString(R.string.question_error_text)
			} catch (e: NoSuchMethodError) {
				activity.resources.getString(R.string.question_error_text)
			}
		}
		infoTextbox.setText(infoText)
		return infoTextbox
	}

	fun generateQuestionText(inflater: LayoutInflater, layout: Int, questionText: String?): LinearLayout {
		// Set the text of the question itself
		val question = inflater.inflate(layout, null) as LinearLayout
		val questionTextView = question.findViewById<View>(R.id.questionText) as MarkDownTextView
		if (questionText != null)
			questionTextView.setText(questionText)
		return question
	}


	/** Creates a slider with a range of discrete values
	 * @param questionText The text of the question to be asked
	 * @return LinearLayout A slider bar */
	private fun createSliderQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?, min: Int, max: Int,
	): LinearLayout {
		if (questionData == null) {
			this.questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)
		}
		// if min is greater than max we need to swap them, so we have to redeclare them.
		var min = min
		var max = max
		if (min > max) {
			val temp_min = min
			min = max
			max = temp_min
		}

		// get the question and the slider, set the slider's range and default/starting value
		val question = generateQuestionText(inflater, R.layout.survey_slider_question, questionText)
		val slider = question.findViewById<View>(R.id.slider) as SeekBarEditableThumb
		slider.max = max
		slider.min = min

		// set the slider value based on the question data, otherwise make it "untouched".
		// if we have a number set the text displlayed above the slider to that number, otherwise empty.
		if (this.questionData!!.answerInteger != null) {
			slider.progress = this.questionData!!.answerInteger!!
			question.findViewById<TextView>(R.id.sliderSelectionText).text = slider.progress.toString()
			slider.markAsTouched()
		} else {
			// Make the slider invisible until it's touched (so there's effectively no default value)
			// if value set to min or lower it goes to the left extreme, unfilled, max to the right
			// extreme, filled. User must _stop_ touching element to run answer logic.
			slider.progress = min
			slider.markAsUntouched()
		}

		// Add a label above the slider with numbers that mark points on a scale
		addNumbersLabelingToSlider(inflater, question, min, max)
		// Set the slider to listen for and record user input
		slider.setOnSeekBarChangeListener(SliderListener(this.questionData!!))
		return question
	}

	/** Creates a group of radio buttons
	 * @param questionText The text of the question
	 * @param answers An array of strings that are options matched with radio buttons
	 * @return RadioGroup A vertical set of radio buttons */
	private fun createRadioButtonQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?, answers: Array<String?>?,
	): LinearLayout {
		if (questionData == null)
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		val question = generateQuestionText(inflater, R.layout.survey_radio_button_question, questionText)
		var answers = answers  // need to be a var to be reassigned in the try-catch for bad optionss
		val radioGroup = question.findViewById<View>(R.id.radioGroup) as RadioGroup

		// If the array of answers is null or too short, replace it with an error message
		if (answers == null || answers.size < 2) {
			val replacementAnswer = try {
				context.resources.getString(R.string.question_error_text)
			} catch (e: NoSuchMethodError) {
				activity.resources.getString(R.string.question_error_text)
			}
			val replacementAnswers = arrayOf<String?>(replacementAnswer, replacementAnswer)
			answers = replacementAnswers
		}

		// Loop through the answer strings, make each one a radio button option, set text.
		for (i in answers.indices) {
			val radioButton = inflater.inflate(R.layout.survey_radio_button, null) as RadioButton
			if (answers.get(i) != null)
				radioButton.text = answers.get(i)
			radioGroup.addView(radioButton)
		}

		// set the radio button value based on the question data
		if (this.questionData!!.answerInteger != null)
			radioGroup.check(radioGroup.getChildAt(this.questionData!!.answerInteger!!).id)

		// Set the group of radio buttons to listen for and record user input
		radioGroup.setOnCheckedChangeListener(RadioButtonListener())
		return question
	}

	/**Creates a question with an array of checkboxes
	 * @param questionText The text of the question
	 * @param options Each string in options[] will caption one checkbox
	 * @return LinearLayout a question with a list of checkboxes */
	private fun createCheckboxQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?, options: Array<String?>?,
	): LinearLayout {
		if (questionData == null)
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		val question = generateQuestionText(inflater, R.layout.survey_checkbox_question, questionText)
		val checkboxesList = question.findViewById<View>(R.id.checkboxesList) as LinearLayout
		var checkedAnswers: Array<String?>? = null

		// set the checkbox values based on the question data
		if (this.questionData!!.answerString != null) {
			val answerString = this.questionData!!.answerString
			// If the answer string is long enough to contain answers, split it into an array
			if (answerString!!.length > 2) {
				checkedAnswers = answerString.substring(1, answerString.length - 1)
						.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			}
		}

		// Loop through the options strings, and make each one a checkbox option (options should never be null
		if (options != null) {
			for (i in options.indices) {
				val checkbox = inflater.inflate(R.layout.survey_checkbox, null) as CheckBox
				// Set the text if it's provided; otherwise leave text as default error message
				if (options[i] != null) {
					checkbox.text = options[i]
					// If it should be checked, check it - this was autogenerated during kotlin conversion...
					if (checkedAnswers != null && Arrays.asList<String?>(*checkedAnswers).contains(options[i]))
						checkbox.isChecked = true
				}

				// Make the checkbox listen for and record user input
				checkbox.setOnClickListener(CheckboxListener())
				// Add the checkbox to the list of checkboxes
				checkboxesList.addView(checkbox)
			}
		}
		return question
	}

	private fun createDateTimeQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?,
	): LinearLayout {
		if (questionData == null)
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		val question = generateQuestionText(inflater, R.layout.survey_open_response_question, questionText)
		val time_picker = inflater.inflate(R.layout.survey_time_input, null) as TimePicker
		val date_picker = inflater.inflate(R.layout.survey_date_input, null) as DatePicker
		time_picker.setIs24HourView(false)

		// extract time values and date string values from questionData
		if (this.questionData!!.answerString != null) {
			// time is easy
			time_picker.minute = this.questionData!!.time_minute!!
			time_picker.hour = this.questionData!!.time_hour!!

			// date is harder
			val datetime_string = this.questionData!!.answerString
			if (datetime_string != null) {
				// split on a space, get first component
				val just_the_date_string = datetime_string
						.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
				// date is in the format YYYY-MM-DD
				val date = LocalDate.parse(just_the_date_string, DateTimeFormatter.ISO_DATE)
				date_picker.updateDate(date.year, date.monthValue, date.dayOfMonth)
			}
		}

		// date/time entries don't work with onFocusChangeListener, so we have to hack into the next button
		nextButtonPreAction = {
			time_pre_action(time_picker, false)
			date_pre_action(date_picker)
		}

		// add both elements to the layout (requires scrolling even on a large phone)
		(question.findViewById<View>(R.id.textFieldContainer) as LinearLayout).addView(time_picker)
		(question.findViewById<View>(R.id.textFieldContainer) as LinearLayout).addView(date_picker)
		return question
	}

	private fun createTimeQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?,
	): LinearLayout {
		if (questionData == null)
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		// Set the text of the question itself
		val question = generateQuestionText(inflater, R.layout.survey_open_response_question, questionText)
		val time_picker = inflater.inflate(R.layout.survey_time_input, null) as TimePicker
		time_picker.setIs24HourView(false)

		// time components are at least easy to extract
		if (this.questionData!!.answerString != null) {
			time_picker.minute = this.questionData!!.time_minute!!
			time_picker.hour = this.questionData!!.time_hour!!
		}

		// Time entries don't work with onFocusChangeListener, so we have to hack into the next button
		nextButtonPreAction = {
			time_pre_action(time_picker, true)
		}

		(question.findViewById<View>(R.id.textFieldContainer) as LinearLayout).addView(time_picker)
		return question
	}


	private fun createDateQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?,
	): LinearLayout {
		if (questionData == null)
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)

		val question = generateQuestionText(inflater, R.layout.survey_open_response_question, questionText)
		val date_picker = inflater.inflate(R.layout.survey_date_input, null) as DatePicker

		// extract the date string from questionData
		if (this.questionData!!.answerString != null) {
			// extract the isoformat date string from the answer and set the value of the datepicker
			val date_string = this.questionData!!.answerString // could be the qd.date_string...
			val date = LocalDate.parse(date_string, DateTimeFormatter.ISO_DATE)
			date_picker.updateDate(date.year, date.monthValue, date.dayOfMonth)
		}

		// date entries don't work with onFocusChangeListener, so we have to hack into the next button
		nextButtonPreAction = {
			date_pre_action(date_picker)
		}

		// Set the text field to listen for and record user input
		// date_picker.onFocusChangeListener = DatePickerResponseListener(questionData)
		(question.findViewById<View>(R.id.textFieldContainer) as LinearLayout).addView(date_picker)
		return question
	}


	fun date_pre_action(date_picker: DatePicker) {
		// pad zeros for month and day of month if the values are less than 10.
		val month = if (date_picker.month < 10) "0${date_picker.month}" else date_picker.month.toString()
		val dayOfMonth = if (date_picker.dayOfMonth < 10) "0${date_picker.dayOfMonth}" else date_picker.dayOfMonth.toString()
		// isoformat date string is pretty trivial
		this.questionData!!.date_string = "${date_picker.year}-${month}-${dayOfMonth}"
		// this.questionData!!.coerceAnswer() // called in setAnswer
		(activity as SurveyActivity).surveyLogic!!.setAnswer(this.questionData!!)
	}


	// coerce flag - is false when inside datetime question because we only want to coerce the
	// answer once? I don't think it actually matters but I'm not testing it otherwise.
	// (in fact I tried to make sure the date-time coerce method handled this case - Don't Care!)
	fun time_pre_action(time_picker: TimePicker, coerce_and_set: Boolean) {
		// pad zeros if the values are less than 10
		this.questionData!!.time_hour = time_picker.hour
		this.questionData!!.time_minute = time_picker.minute
		if (coerce_and_set) {
			this.questionData!!.coerceAnswer()
			(activity as SurveyActivity).surveyLogic!!.setAnswer(this.questionData!!)
		}
	}


	/**Creates a question with an open-response, text-input field
	 * @param questionText The text of the question
	 * @param inputTextType The type of answer (number, text, etc.)
	 * @return LinearLayout question and answer */
	private fun createFreeResponseQuestion(
			inflater: LayoutInflater, questionID: String, questionText: String?, inputTextType: TextFieldType.Type,
	): LinearLayout {
		// TODO: Give open response questions autofocus and make the keyboard appear
		// TODO: when the user presses Enter, jump to the next input field
		if (questionData == null) {
			val options = "Text-field input type = $inputTextType" // options indicates free response type
			questionData = (activity as SurveyActivity).surveyLogic!!.getCorrectlyPopulatedQuestionAnswer(questionID)
		}

		val question = generateQuestionText(inflater, R.layout.survey_open_response_question, questionText)

		// set the input type of the text field based on the question type
		val editText: EditText = when (inputTextType) {
			TextFieldType.Type.NUMERIC -> inflater.inflate(R.layout.survey_free_number_input, null) as EditText
			TextFieldType.Type.SINGLE_LINE_TEXT -> inflater.inflate(R.layout.survey_free_text_input, null) as EditText
			TextFieldType.Type.MULTI_LINE_TEXT -> inflater.inflate(R.layout.survey_multiline_text_input, null) as EditText
		}

		// extract the answer string from questionData
		if (this.questionData!!.answerString != null)
			editText.setText(this.questionData!!.answerString)

		nextButtonPreAction = {
			openResponsePreNextButton()
		}
		// Set the text field to listen for and record user input
		editText.onFocusChangeListener = OpenResponseListener()
		(question.findViewById<View>(R.id.textFieldContainer) as LinearLayout).addView(editText)
		return question
	}

	/*************************************** Slider UI ***************************************** */

	/** Adds a numeric scale above a Slider Question
	 * @param question the Slider Question that needs a number scale
	 * @param min the lowest number on the scale
	 * @param max the highest number on the scale */
	private fun addNumbersLabelingToSlider(inflater: LayoutInflater, question: LinearLayout, min: Int, max: Int) {
		// Replace the numbers label placeholder view (based on http://stackoverflow.com/a/3760027)
		var numbersLabel = question.findViewById(R.id.numbersPlaceholder) as View
		val index = question.indexOfChild(numbersLabel)
		question.removeView(numbersLabel)
		// this is the survey_slider_numbers_label file
		numbersLabel = inflater.inflate(R.layout.survey_slider_numbers_label, question, false)
		// this is the "linearLayoutNumbers" file
		val label = numbersLabel.findViewById<View>(R.id.linearLayoutNumbers) as LinearLayout

		// up to 11 labels - 11 works by giving your 0-10 options _correctly_, e.g.
		// 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, and 10 ~~ that's 11 labels.
		// otherwise you get weird splits, like this for 1-100:
		// 1, 12, 23, 34, 45, 56, 67, 78, 89, 100
		// we can always fit the full list if it is <= 11
		val range = max - min
		val numberOfLabels = if (range < 11) range + 1 else 11

		// Create labels and spacers - the survey_slider_single_number_label file
		val numberResourceID = R.layout.survey_slider_single_number_label

		// low value must be zero due to the placeholder required for no-value, I think.
		// if numberOfLabels must be -1 because we manually add the highest
		for (i in 0 until numberOfLabels - 1) {
			// create the label container(?)
			val number = inflater.inflate(numberResourceID, label, false) as TextView
			label.addView(number)
			// calculate the label - i starts at zero so it sets minimum correctly
			val label_number = (min + (i * range) / (numberOfLabels - 1))
			number.text = label_number.toString()
			/// and add the space
			val spacer = inflater.inflate(R.layout.horizontal_spacer, label, false) as View
			label.addView(spacer)
		}
		// Create the last, highest, rightmost label without a spacer to its right
		val number = inflater.inflate(numberResourceID, label, false) as TextView
		label.addView(number)
		number.text = max.toString()

		// Add the set of numeric labels to the question
		question.addView(numbersLabel, index)
	}

	/************************************* ANSWER LISTENERS ************************************* */

	/** Listens for a touch/answer to a Slider Question, and records the answer  */
	private inner class SliderListener(var questionDescription: QuestionData) : OnSeekBarChangeListener {
		// This does not trigger until the finger _slides_. Annoying. If you hook with an on-touch
		// listener to mark the slider as touched, it will trigger on every touch, which is bad,
		// and also it makes the progress visible until the first slide, which is also bad. Even
		// if this is... slightly odd because it is just-barely possible to touch the screen
		// and have nothing happen until you lift your finger, that appears to be the way android
		// intends this to work, and the way it works in other apps I'm pretty sure
		override fun onStartTrackingTouch(seekBar: SeekBar) {
			(seekBar as SeekBarEditableThumb).markAsTouched()  // make the thumb visible
		}

		// update the text above the slider as the user moves the slider.
		override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
			(seekBar.parent as LinearLayout).findViewById<TextView>(R.id.sliderSelectionText).text = progress.toString()
		}

		override fun onStopTrackingTouch(seekBar: SeekBar) {
			if (questionData !== questionDescription)
				throw RuntimeException("QuestionData mismatch slider")

			val real_seekbar = seekBar as SeekBarEditableThumb
			if (real_seekbar.hasBeenTouched!!)
				questionData!!.answerInteger = real_seekbar.progress
			else
				questionData!!.answerInteger = null
			// standard answer rigamarole
			questionData!!.coerceAnswer()
			SurveyTimingsRecorder.recordAnswer(questionData!!.answerString, questionData)
			(activity as SurveyActivity).surveyLogic!!.setAnswer(questionData!!)
		}
	}

	/** Listens for a touch/answer to a Radio Button Question, and records the answer. Must set
	 * integer value of index in order to restore question correctly */
	private inner class RadioButtonListener : RadioGroup.OnCheckedChangeListener {
		override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
			val selectedButton = group.findViewById<View>(checkedId) as RadioButton
			if (selectedButton.isChecked) {
				// this is the way surveytimings have always been recorded, they are the same afaik
				SurveyTimingsRecorder.recordAnswer(selectedButton.text.toString(), questionData)
				questionData!!.answerInteger = SurveyAnswersRecorder.indexOfSelectedRadioButton(the_questionLayout)
				questionData!!.answerString = SurveyAnswersRecorder.answerFromRadioButtonQuestion(the_questionLayout)
				(activity as SurveyActivity).surveyLogic!!.setAnswer(questionData!!)
				questionData!!.coerceAnswer()
			} else {
				/* It should not be possible to un-check a radio button, but if that happens, record
				 * the answer as an empty string. */
				SurveyTimingsRecorder.recordAnswer("", questionData)
			}
		}
	}

	/** Listens for a touch/answer to a Checkbox Question, and records the answer  */
	private inner class CheckboxListener : View.OnClickListener {
		override fun onClick(view: View) {
			// If it's a CheckBox... and its parent is a LinearLayout? (we set it up how can this fail?)
			if (view is CheckBox && view.getParent() is LinearLayout) {
				val checkboxesList = view.getParent() as LinearLayout
				val answersList = SurveyAnswersRecorder.answerFromCheckboxQuestion(checkboxesList)
				// survey timings use an empty list representation survey answers uses the
				// NO_ANSWER_SELECTED constant. This decision was made at the beginning of time
				// and can never change. fantastic.
				SurveyTimingsRecorder.recordAnswer(answersList ?: "[]", questionData)
				questionData!!.answerString = answersList
				// we need answer indices for checkbox logic.
				questionData!!.indicesList = SurveyAnswersRecorder.indicesOfSelectedCheckboxButtons(checkboxesList)
				(activity as SurveyActivity).surveyLogic!!.setAnswer(questionData!!)
			}
		}
	}

	// the OpenResponseListener was triggering ~4ms-apart events for every touch action, and it
	// could crash the app - I think it was a threading issue?? very unclear.
	/** Get the answer from the answer field, record timings and send answer off. */
	fun openResponsePreNextButton() {
		val answer_string = SurveyAnswersRecorder.answerFromOpenResponseQuestion(the_questionLayout)
		SurveyTimingsRecorder.recordAnswer(answer_string ?: "", questionData!!)
		questionData!!.answerString = answer_string
		(activity as SurveyActivity).surveyLogic!!.setAnswer(questionData!!)
	}

	/** Listens for an input/answer to an Open/Free Response Question, and records the answer  */
	private inner class OpenResponseListener : OnFocusChangeListener {
		// TODO: replace this with a listener on the Next button - this gets called for every updated input event on the screen and is bad
		override fun onFocusChange(v: View, hasFocus: Boolean) {
			if (hasFocus) {
				// The user just selected the input box
				// Improvement idea: record when the user first touched the input field; right now
				// it only records when the user selected away from the input field. */

				// Set the EditText so that if the user taps outside, the keyboard disappears
				if (v is EditText) {
					val keyboard: TextFieldKeyboard = try {
						TextFieldKeyboard(context)
					} catch (e: NoSuchMethodError) {
						TextFieldKeyboard(activity)
					}
					keyboard.makeKeyboardBehave(v)
				}
			}
		}
	}

	// Interface for the "Next" button to signal the Activity
	interface OnGoToNextQuestionListener {
		fun goToNextQuestion(questionData: QuestionData)
	}
}
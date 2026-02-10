package org.beiwe.app.survey

/**
 * Created by Josh Zagorsky on 12/11/16.
 */
class QuestionType {
	enum class Type(val stringName: String) {
		INFO_TEXT_BOX("Info Text Box"),
		SLIDER("Slider Question"),
		RADIO_BUTTON("Radio Button Question"),
		CHECKBOX("Checkbox Question"),
		FREE_RESPONSE("Open Response Question"),
		DATE("date"),
		DATE_TIME("date_time"),
		TIME("time")

		// this was the initial version of the names of these three, but it looks like ios is using lower case identifiers
		// confirmed: we don't match the ios app on these string values, except now we do for date/time
		// DATE("Date Question"),
		// DATE_TIME("Date and Time Question"),
		// TIME("Time Question")
	}
}
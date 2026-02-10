package org.beiwe.app.survey

/** this was an extremely common code pattern, so we pulled it out and made it a class.  */
class TextFieldType {
	enum class Type {
		NUMERIC, SINGLE_LINE_TEXT, MULTI_LINE_TEXT
	}
}
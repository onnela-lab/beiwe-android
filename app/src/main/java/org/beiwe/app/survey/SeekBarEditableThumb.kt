package org.beiwe.app.survey

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.SeekBar

/** A customized slider ui element so that we can have a grayed out effect for untouched sliders.
 * Based on http://stackoverflow.com/a/19008611
 * @author Josh Zagorsky */

class SeekBarEditableThumb : androidx.appcompat.widget.AppCompatSeekBar {
	// these constructors are somewhat pointless, we only use the second one, we only instantiate
	// these in createSliderQuestion, and we manually set variables based on the question.
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

	private var compatThumb: Drawable? = null

	var hasBeenTouched: Boolean? = null // Boolean of whether or not the user has touched the SeekBar yet
		private set

	override fun setThumb(thumb: Drawable) {
		super.setThumb(thumb)
		compatThumb = thumb
	}

	/** Make the SeekBar's "Thumb" invisible, and mark it as "user hasn't touched this yet" */
	fun markAsUntouched() {
		compatThumb!!.mutate().alpha = 0
		hasBeenTouched = false
	}

	/** Make the SeekBar's "Thumb" visible, and mark it as "user has touched this" */
	fun markAsTouched() {
		compatThumb!!.mutate().alpha = 255
		hasBeenTouched = true
	}
}
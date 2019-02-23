package com.rizlee.rangeseekbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.rizlee.rangeseekbar.utils.BitmapUtil
import com.rizlee.rangeseekbar.utils.PixelUtil

const val INT = 6
const val FLOAT = 7

private const val DEFAULT_MIN = 0f
private const val DEFAULT_MAX = 100f
private const val DEFAULT_STEP = 1f

private const val DEFAULT_HEIGHT_IN_DP = 40
private const val DEFAULT_BAR_HEIGHT_IN_DP = 1
private const val DEFAULT_TEXT_SIZE_IN_DP = 12
private const val DEFAULT_TEXT_DISTANCE_TO_BUTTON_IN_DP = 8

private const val THUMB_TEXT_POSITION_NONE = 0
private const val THUMB_TEXT_POSITION_BELOW = 1
private const val THUMB_TEXT_POSITION_ABOVE = 2

private const val ADDITIONAL_TEXT_POSITION_NONE = 3
private const val ADDITIONAL_TEXT_POSITION_BELOW = 4
private const val ADDITIONAL_TEXT_POSITION_ABOVE = 5

class RangeSeekBar constructor(context: Context,
                               attributesSet: AttributeSet? = null,
                               listener: SeekBar.OnSeekBarChangeListener? = null) : View(context) {

    /* Attrs values */
    private var thumbImage: Bitmap
    private var thumbDisabledImage: Bitmap // when isActive = false
    private var thumbPressedImage: Bitmap

    var isActive = true //dragable or not
        set(value) {
            field = value; invalidate()
        }

    var leftText: String = getContext().getString(R.string.text_left)
    var rightText: String = getContext().getString(R.string.text_right)
    var centerText: String = getContext().getString(R.string.text_center)

    var minValue = DEFAULT_MIN
    var maxValue = DEFAULT_MAX
    var stepValue = DEFAULT_STEP
    var valueType = INT

    var sideBarColor = Color.RED
    var centerBarColor = Color.GREEN
    var transitionBarColor = Color.YELLOW
    var isGradientNeed = false

    private var textColor = Color.GRAY
    private var textFont = ResourcesCompat.getFont(getContext(), R.font.worksans_semibold)
    private var textSize = PixelUtil.dpToPx(getContext(), DEFAULT_TEXT_SIZE_IN_DP)
    private var thumbTextPosition = THUMB_TEXT_POSITION_NONE
    private var additionalTextPosition = ADDITIONAL_TEXT_POSITION_NONE

    private var isRoundedCorners = false

    private var barHeight = PixelUtil.dpToPx(getContext(), DEFAULT_BAR_HEIGHT_IN_DP)

    /* System values */
    private var isDragging = false

    private var leftValue = minValue
    private var rightValue = maxValue

    private val scaledTouchSlop by lazy { ViewConfiguration.get(getContext()).scaledTouchSlop }

    init {
        getContext().resources.apply {
            BitmapUtil.apply {
                thumbImage = toBitmap(getDrawable(R.drawable.thumb_normal))
                thumbPressedImage = toBitmap(getDrawable(R.drawable.thumb_pressed))
                thumbDisabledImage = toBitmap(getDrawable(R.drawable.thumb_disabled))
            }
        }

        attributesSet?.let {
            getContext().obtainStyledAttributes(it, R.styleable.RangeSeekBar).apply {
                getDrawable(R.styleable.RangeSeekBar_thumbsNormal)?.let { drawable -> thumbImage = BitmapUtil.toBitmap(drawable) }
                getDrawable(R.styleable.RangeSeekBar_thumbsDisabled)?.let { drawable -> thumbDisabledImage = BitmapUtil.toBitmap(drawable) }
                getDrawable(R.styleable.RangeSeekBar_thumbsPressed)?.let { drawable -> thumbPressedImage = BitmapUtil.toBitmap(drawable) }

                centerBarColor = getColor(R.styleable.RangeSeekBar_centerColor, centerBarColor)
                sideBarColor = getColor(R.styleable.RangeSeekBar_sideColor, sideBarColor)
                transitionBarColor = getColor(R.styleable.RangeSeekBar_transitionColor, transitionBarColor)
                isGradientNeed = getBoolean(R.styleable.RangeSeekBar_enableGradient, isGradientNeed)

                textColor = getColor(R.styleable.RangeSeekBar_textColor, textColor)
                //textFont = this.resources.getFont(R.styleable.RangeSeekBar_textFont) // todo font
                thumbTextPosition = getInt(R.styleable.RangeSeekBar_showThumbsText, thumbTextPosition)
                additionalTextPosition = getInt(R.styleable.RangeSeekBar_showAdditionalText, additionalTextPosition)
                textSize = getDimensionPixelSize(R.styleable.RangeSeekBar_textSize, textSize) //todo maybe problems with text size here

                centerText = getString(R.styleable.RangeSeekBar_centerText)?.let { centerText }!!
                leftText = getString(R.styleable.RangeSeekBar_leftText)?.let { leftText }!!
                rightText = getString(R.styleable.RangeSeekBar_rightText)?.let { rightText }!!

                isActive = getBoolean(R.styleable.RangeSeekBar_active, isActive)

                minValue = getFloat(R.styleable.RangeSeekBar_minValue, minValue)
                maxValue = getFloat(R.styleable.RangeSeekBar_maxValue, maxValue)
                stepValue = getFloat(R.styleable.RangeSeekBar_stepValue, stepValue)
                valueType = getInt(R.styleable.RangeSeekBar_valueType, valueType)

                isRoundedCorners = getBoolean(R.styleable.RangeSeekBar_roundedCorners, isRoundedCorners)

                barHeight = getDimensionPixelSize(R.styleable.RangeSeekBar_barHeight, barHeight)

                recycle()
            }
        }

        leftValue = minValue
        rightValue = maxValue

        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isActive) {
            if (!isEnabled) return false
            event?.let {
                when (it.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN ->{

                    }

                    MotionEvent.ACTION_MOVE ->{

                    }

                    MotionEvent.ACTION_UP -> {

                    }
                    MotionEvent.ACTION_POINTER_DOWN ->{

                    }

                    MotionEvent.ACTION_POINTER_UP ->{

                    }
                    MotionEvent.ACTION_CANCEL -> {

                    }
                }
            } ?: run { return false }


        }
    }

    fun setRangeValues(leftValue: Float? = this.leftValue, rightValue: Float? = this.rightValue, stepValue: Float? = this.stepValue) {
        this.leftValue = leftValue!!
        this.rightValue = rightValue!!
        this.stepValue = stepValue!!
    }

    interface OnRangeSeekBarListener {
        fun onValuesChanged()
    }
}
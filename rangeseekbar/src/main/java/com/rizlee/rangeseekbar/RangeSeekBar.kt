package com.rizlee.rangeseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import com.rizlee.rangeseekbar.utils.BitmapUtil
import com.rizlee.rangeseekbar.utils.PixelUtil
import java.util.logging.Logger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val INT = 6
const val FLOAT = 7

private const val DEFAULT_MIN = 0f
private const val DEFAULT_MAX = 100f
private const val DEFAULT_STEP = 1f

private const val DEFAULT_BAR_HEIGHT_IN_DP = 8
private const val DEFAULT_TEXT_SIZE_IN_DP = 12
private const val DEFAULT_TEXT_DISTANCE_TO_TOP_IN_DP = 8
private const val TEXT_LATERAL_PADDING_IN_DP = 3

private const val THUMB_TEXT_POSITION_NONE = 0
private const val THUMB_TEXT_POSITION_BELOW = 1
private const val THUMB_TEXT_POSITION_ABOVE = 2
private const val THUMB_TEXT_POSITION_CENTER = 3

private const val THUMB_TEXT_MARGIN_IN_DP = 0
private const val ADDITIONAL_TEXT_MARGIN_IN_DP = 0

private const val ADDITIONAL_TEXT_POSITION_NONE = 0
private const val ADDITIONAL_TEXT_POSITION_BELOW = 1
private const val ADDITIONAL_TEXT_POSITION_ABOVE = 2
private const val ADDITIONAL_TEXT_POSITION_CENTER = 3

private const val INVALID_POINTER_ID = 255

class RangeSeekBar @JvmOverloads constructor(
        context: Context,
        private val attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /* Attrs values */
    private lateinit var thumbImage: Bitmap
    private lateinit var thumbDisabledImage: Bitmap // when isActive = false
    private lateinit var thumbPressedImage: Bitmap

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

    var listener: OnRangeSeekBarListener? = null

    private var textColor = Color.GRAY
    private var textFont = ResourcesCompat.getFont(getContext(), R.font.worksans_semibold)
    private var textSize = PixelUtil.dpToPx(getContext(), DEFAULT_TEXT_SIZE_IN_DP)
    private var thumbTextPosition = THUMB_TEXT_POSITION_NONE
    private var additionalTextPosition = ADDITIONAL_TEXT_POSITION_NONE

    private var isRoundedCorners = false

    private var barHeight = PixelUtil.dpToPx(getContext(), DEFAULT_BAR_HEIGHT_IN_DP)

    private var additionalTextMargin = ADDITIONAL_TEXT_MARGIN_IN_DP
    private var thumbTextMargin = THUMB_TEXT_MARGIN_IN_DP

    /* System values */
    private var isDragging = false

    private var stepsCount = 0

    private val scaledTouchSlop by lazy { ViewConfiguration.get(getContext()).scaledTouchSlop }

    private var activePointerId = INVALID_POINTER_ID
    private var downMotionX = 0f

    private var padding = 0f
    private var distanceToTop = 0
    //private var thumbTextOffset = 0

    private var normalizedMinValue = 0.0
    private var normalizedMaxValue = 1.0

    private var pressedThumb: Thumb? = null

    private lateinit var rectF: RectF
    private lateinit var center: Point

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        init()
    }

    private fun init() {
        context.resources.apply {
            BitmapUtil.apply {
                thumbImage = toBitmap(getDrawable(R.drawable.thumb_normal))
                thumbPressedImage = toBitmap(getDrawable(R.drawable.thumb_pressed))
                thumbDisabledImage = toBitmap(getDrawable(R.drawable.thumb_disabled))
            }
        }

        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.RangeSeekBar).apply {
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

                centerText = getString(R.styleable.RangeSeekBar_centerText) ?: centerText
                leftText = getString(R.styleable.RangeSeekBar_leftText) ?: leftText
                rightText = getString(R.styleable.RangeSeekBar_rightText) ?: rightText

                isActive = getBoolean(R.styleable.RangeSeekBar_active, isActive)

                minValue = getFloat(R.styleable.RangeSeekBar_minValue, minValue)
                maxValue = getFloat(R.styleable.RangeSeekBar_maxValue, maxValue)
                stepValue = getFloat(R.styleable.RangeSeekBar_stepValue, stepValue)
                if (maxValue < minValue) throw Exception("Min value can't be higher than max value")
                if (!stepValueValidation(minValue, maxValue, stepValue)) throw Exception("Incorrect min/max/step, it must be: (maxValue - minValue) % stepValue == 0f")
                valueType = getInt(R.styleable.RangeSeekBar_valueType, valueType)
                stepsCount = ((maxValue - minValue) / stepValue).toInt()

                isRoundedCorners = getBoolean(R.styleable.RangeSeekBar_roundedCorners, isRoundedCorners)

                barHeight = getDimensionPixelSize(R.styleable.RangeSeekBar_barHeight, barHeight)

                additionalTextMargin = getDimensionPixelSize(R.styleable.RangeSeekBar_additionalTextMargin, additionalTextMargin)
                thumbTextMargin = getDimensionPixelSize(R.styleable.RangeSeekBar_thumbsTextMargin, thumbTextMargin)

                recycle()
            }
        }

        distanceToTop = PixelUtil.dpToPx(context, DEFAULT_TEXT_DISTANCE_TO_TOP_IN_DP)

        isFocusable = true
        isFocusableInTouchMode = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isActive) {
            if (!isEnabled) return false
            event?.let { event ->
                var pointerIndex: Int
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        Logger.getLogger("test").warning("ACTION_DOWN")
                        activePointerId = event.getPointerId(event.pointerCount - 1)
                        pointerIndex = event.findPointerIndex(activePointerId)
                        downMotionX = event.getX(pointerIndex)

                        pressedThumb = evalPressedThumb(downMotionX)
                        pressedThumb ?: return super.onTouchEvent(event)

                        isPressed = true
                        invalidate()
                        isDragging = true
                        trackTouchEvent(event)
                        parent?.requestDisallowInterceptTouchEvent(true)
                        println()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        Logger.getLogger("test").warning("ACTION_MOVE")
                        pressedThumb?.let {
                            if (isDragging) {
                                trackTouchEvent(event)
                            } else {
                                pointerIndex = event.findPointerIndex(activePointerId)
                                val x = event.getX(pointerIndex)

                                if (abs(x - downMotionX) > scaledTouchSlop) {
                                    isPressed = true
                                    invalidate()
                                    isDragging = true
                                    trackTouchEvent(event)
                                    parent?.requestDisallowInterceptTouchEvent(true)
                                    println()
                                }
                            }
                        }
                        println()
                    }

                    MotionEvent.ACTION_UP -> {
                        Logger.getLogger("test").warning("ACTION_UP")
                        if (isDragging) {
                            trackTouchEvent(event)
                            isDragging = false
                            isPressed = false
                        } else {
                            isDragging = true
                            trackTouchEvent(event)
                            isDragging = false
                        }

                        listener?.let {
                            when (valueType) {
                                INT -> it.onValuesChanged(getSelectedMinValue().toInt(), getSelectedMaxValue().toInt())
                                FLOAT -> it.onValuesChanged(getSelectedMinValue().toFloat(), getSelectedMaxValue().toFloat())
                                else -> throw Exception("Unknown value type")
                            }
                        }

                        pressedThumb = null
                        invalidate()
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        Logger.getLogger("test").warning("ACTION_POINTER_DOWN")
                        val index = event.pointerCount - 1
                        downMotionX = event.getX(index)
                        activePointerId = event.getPointerId(index)
                        invalidate()
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        Logger.getLogger("test").warning("ACTION_POINTER_UP")
                        onSecondaryPointerUp(event)
                        invalidate()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        Logger.getLogger("test").warning("ACTION_CANCEL")
                        if (isDragging) isDragging = false; isPressed = false
                        invalidate()
                    }
                }
            } ?: run { return false }
        }
        return true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        center = Point(width / 2, height / 2)
        rectF = RectF(padding,
                center.y - barHeight / 2.toFloat(),
                width - padding,
                center.y + barHeight / 2.toFloat())

        paint.textSize = textSize.toFloat()
        paint.style = Paint.Style.FILL
        paint.typeface = textFont
        paint.isAntiAlias = true

        padding = getThumbHalfWidth()

        /* pre draw rect */
        rectF.left = padding
        rectF.right = width - padding

        canvas?.apply {
            //drawRect(rectF, paint)  // this line just for debugging

            /* center rect */
            rectF.left = normalizedToScreen(normalizedMinValue)
            rectF.right = normalizedToScreen(normalizedMaxValue)

            /* left rect */
            val rectUnusedArea = RectF()
            rectUnusedArea.top = rectF.top
            rectUnusedArea.bottom = rectF.bottom
            rectUnusedArea.left = padding
            rectUnusedArea.right = rectF.left

            if (isGradientNeed) {
                paint.shader = LinearGradient(
                        rectUnusedArea.left,
                        0f,
                        rectUnusedArea.right,
                        0f,
                        intArrayOf(sideBarColor, sideBarColor, transitionBarColor),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.REPEAT)
            } else {
                paint.color = sideBarColor
            }
            if (!isRoundedCorners) {
                drawRect(rectUnusedArea, paint)
            } else {
                val bufRect = RectF(rectUnusedArea)
                bufRect.left += 10
                drawRect(bufRect, paint)

                drawRoundRect(rectUnusedArea, 10f, 10f, paint)
            }

            /* right rect */
            rectUnusedArea.right = width - padding
            rectUnusedArea.left = rectF.right

            if (isGradientNeed) {
                paint.shader = LinearGradient(
                        rectUnusedArea.right,
                        0f,
                        rectUnusedArea.left,
                        0f,
                        intArrayOf(sideBarColor, sideBarColor, transitionBarColor),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.REPEAT)
            } else {
                paint.color = sideBarColor
            }
            if (!isRoundedCorners) {
                drawRect(rectUnusedArea, paint)
            } else {
                val bufRect1 = RectF(rectUnusedArea)
                bufRect1.right -= 10
                drawRect(bufRect1, paint)

                drawRoundRect(rectUnusedArea, 10f, 10f, paint)
            }

            rectF.left = normalizedToScreen(normalizedMinValue)
            rectF.right = normalizedToScreen(normalizedMaxValue)

            if (isGradientNeed) {
                paint.shader = LinearGradient(
                        rectF.left,
                        0f,
                        rectF.right,
                        0f,
                        intArrayOf(transitionBarColor, centerBarColor, transitionBarColor),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.REPEAT)
            } else {
                paint.color = centerBarColor
            }
            drawRect(rectF, paint)

            // todo maybe need check on isActive

            /* clear gradient */
            paint.shader = null

            /* draw thumb */
            drawThumb(normalizedToScreen(normalizedMaxValue), Thumb.MAX == pressedThumb, canvas)
            drawThumb(normalizedToScreen(normalizedMinValue), Thumb.MIN == pressedThumb, canvas)

            /* draw text (thumb values) if need and left/center/right text if need*/
            paint.textSize = textSize.toFloat()
            paint.color = textColor                 //todo in future change textColor thumb values and left/center/right text

            val minText = removeRedundantNumberPart(getSelectedMinValue().toString())
            val maxText = removeRedundantNumberPart(getSelectedMaxValue().toString())



            val minTextWidth = paint.measureText(minText)
            val maxTextWidth = paint.measureText(maxText)
            val centerTextWidth = paint.measureText(centerText)

            var minPosition = max(0f, normalizedToScreen(normalizedMinValue) - minTextWidth * 0.5f)
            var maxPosition = min(width - maxTextWidth, normalizedToScreen(normalizedMaxValue) - maxTextWidth * 0.5f)

            val spacing = PixelUtil.dpToPx(context, TEXT_LATERAL_PADDING_IN_DP)
            val overlap = minPosition + minTextWidth - maxPosition + spacing
            if (overlap > 0f) {
                minPosition -= (overlap * normalizedMinValue / (normalizedMinValue + 1 - normalizedMaxValue)).toFloat()
                maxPosition += (overlap * (1 - normalizedMaxValue) / (normalizedMinValue + 1 - normalizedMaxValue)).toFloat()
            }

            if (thumbTextPosition != THUMB_TEXT_POSITION_NONE) {
                val yPosition = when (thumbTextPosition) {
                    ADDITIONAL_TEXT_POSITION_BELOW -> {
                        center.y + barHeight / 2 + getThumbHalfHeight() + thumbTextMargin
                    }
                    ADDITIONAL_TEXT_POSITION_ABOVE -> {
                        center.y + barHeight / 2 - getThumbHalfHeight() - thumbTextMargin
                    }
                    ADDITIONAL_TEXT_POSITION_CENTER -> {
                        (center.y + barHeight / 2).toFloat()
                    }
                    else -> {
                        (center.y + barHeight / 2).toFloat()
                    }
                }
                drawText(minText,
                        minPosition,
                        yPosition,
                        paint)

                drawText(maxText,
                        maxPosition,
                        yPosition,
                        paint)
            }

            val leftTextWidth = padding + paint.measureText(leftText) + spacing.toFloat()
            val rightTextWidth = padding + paint.measureText(rightText) + spacing.toFloat()
            val centerTextPosition = (maxPosition + minPosition) * 0.5f - centerTextWidth * 0.5f + spacing * 4
            var textPosition = centerTextPosition

            if (centerTextPosition < leftTextWidth + spacing) textPosition = leftTextWidth + spacing
            if (centerTextPosition + centerTextWidth > width - rightTextWidth - padding + spacing * 4) textPosition = width - rightTextWidth - padding - centerTextWidth + spacing * 4

            if (additionalTextPosition != ADDITIONAL_TEXT_POSITION_NONE) {
                val yPosition = when (additionalTextPosition) {
                    ADDITIONAL_TEXT_POSITION_BELOW -> {
                        center.y + barHeight / 2 + getThumbHalfHeight() + additionalTextMargin
                    }
                    ADDITIONAL_TEXT_POSITION_ABOVE -> {
                        center.y + barHeight / 2 - getThumbHalfHeight() - additionalTextMargin
                    }
                    ADDITIONAL_TEXT_POSITION_CENTER -> {
                        (center.y + barHeight / 2).toFloat()
                    }
                    else -> {
                        (center.y + barHeight / 2).toFloat()
                    }
                }

                drawText(centerText,
                        textPosition,
                        yPosition,
                        paint)

                drawText(leftText,
                        normalizedToScreen(0.0),
                        yPosition,
                        paint)

                drawText(rightText,
                        normalizedToScreen(1.0) - rightTextWidth + spacing + padding,
                        yPosition,
                        paint)
            }
        }
    }

    private fun stepValueValidation(minValue: Float, maxValue: Float, stepValue: Float) = (maxValue - minValue).toBigDecimal() % stepValue.toBigDecimal() == 0f.toBigDecimal()

    private fun removeRedundantNumberPart(number: String) =
            when (valueType) {
                INT -> number.substring(0, number.indexOf("."))
                FLOAT -> stepValue.toString().apply {
                    return when {
                        contains(".") ->{
                            val numbersCountDifference = (length - indexOf(".") - 1) - (number.length - number.indexOf(".") - 1)
                            when {
                                (numbersCountDifference) > 0 -> {
                                    var stringWithZeros = ""
                                    for (i in 1..abs(numbersCountDifference))
                                        stringWithZeros += "0"
                                    number + stringWithZeros
                                }
                                (numbersCountDifference) < 0 -> number.substring(0, number.indexOf(".") + (length - indexOf(".")))
                                else -> number
                            }
                        }
                        number.contains(".") -> number.substring(0, number.indexOf(".") + 2)
                        else -> number
                    }
                }
                else -> throw Exception("Invalid type or values")
            }

    private fun drawThumb(screenCoord: Float, pressed: Boolean, canvas: Canvas) {
        val buttonToDraw = if (isActive) {
            if (pressed) thumbPressedImage else thumbImage
        } else {
            thumbDisabledImage
        }

        canvas.drawBitmap(buttonToDraw,
                screenCoord - if (pressed) getThumbPressedHalfWidth() else getThumbHalfWidth(),
                center.y.toFloat() - getThumbHalfHeight(),
                paint)
    }

    private fun evalPressedThumb(touchX: Float): Thumb? {
        val minThumbPressed = isInThumbRange(touchX, normalizedMinValue)
        val maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue)
        return if (minThumbPressed && maxThumbPressed) {
            if (touchX / width > 0.5f) Thumb.MIN else Thumb.MAX
        } else if (minThumbPressed) {
            Thumb.MIN
        } else if (maxThumbPressed) {
            Thumb.MAX
        } else null
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        ev.action.apply {
            if (ev.getPointerId(this) == activePointerId) {
                val newPointerIndex = if (this == 0) 1 else 0
                downMotionX = ev.getX(newPointerIndex)
                activePointerId = ev.getPointerId(newPointerIndex)
            }
        }
    }

    private fun isInThumbRange(touchX: Float, normalizedThumbValue: Double) = abs(touchX - normalizedToScreen(normalizedThumbValue)) <= getThumbHalfWidth()

    private fun normalizedToScreen(normalizedPos: Double) = (padding + normalizedPos * (width - 2 * padding)).toFloat()

    private fun getThumbHalfHeight() = thumbImage.height * 0.5f
    private fun getThumbHalfWidth() = thumbImage.width * 0.5f
    private fun getThumbPressedHalfHeight() = thumbPressedImage.height * 0.5f
    private fun getThumbPressedHalfWidth() = thumbPressedImage.width * 0.5f

    private fun trackTouchEvent(event: MotionEvent) {
        val pointerIndex = event.findPointerIndex(activePointerId)
        val x = event.getX(pointerIndex)

        if (Thumb.MIN == pressedThumb) {
            setNormalizedMinValue(screenToNormalized(x))
        } else if (Thumb.MAX == pressedThumb) {
            setNormalizedMaxValue(screenToNormalized(x))
        }
    }

    private fun setNormalizedMinValue(value: Double) {
        normalizedMinValue = max(0.0, min(1.0, min(value, normalizedMaxValue)))
        invalidate()
    }

    private fun setNormalizedMaxValue(value: Double) {
        normalizedMaxValue = max(0.0, min(1.0, max(value, normalizedMinValue)))
        invalidate()
    }

    private fun screenToNormalized(screenPos: Float): Double {
        val width = width
        return if (width <= 2 * padding) {
            0.0 //divide by zero safe
        } else {
            val result = ((screenPos - padding) / (width - 2 * padding)).toDouble()
            min(1.0, max(0.0, result))
        }
    }

    private fun valueToNormilize(value: Double) = ((maxValue - minValue) * (value * 100)) / 100 + minValue

    private fun getSelectedMinValue() = getValueAccordingToStep(valueToNormilize(normalizedMinValue))
    private fun getSelectedMaxValue() = getValueAccordingToStep(valueToNormilize(normalizedMaxValue))

    private fun getValueAccordingToStep(value: Double) = ((value.toBigDecimal() / stepValue.toBigDecimal()).toInt()).toBigDecimal() * stepValue.toBigDecimal()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = 200
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec)
        }

        val heightThumbText = if (thumbTextPosition == THUMB_TEXT_POSITION_NONE ||
                thumbTextPosition == THUMB_TEXT_POSITION_CENTER) 0 else PixelUtil.dpToPx(context, thumbTextMargin) + PixelUtil.dpToPx(context, textSize) / 2

        val heightAdditionalText = if (additionalTextPosition == ADDITIONAL_TEXT_POSITION_NONE ||
                additionalTextPosition == ADDITIONAL_TEXT_POSITION_CENTER) 0 else PixelUtil.dpToPx(context, additionalTextMargin) + PixelUtil.dpToPx(context, textSize) / 2

        var height = thumbImage.height + max(heightThumbText, heightAdditionalText)

        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = min(height, MeasureSpec.getSize(heightMeasureSpec))
        }
        setMeasuredDimension(width, height)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("SUPER", super.onSaveInstanceState())
        bundle.putDouble("MIN", normalizedMinValue)
        bundle.putDouble("MAX", normalizedMaxValue)
        return bundle
    }

    override fun onRestoreInstanceState(parcel: Parcelable) {
        val bundle = parcel as Bundle
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"))
        normalizedMinValue = bundle.getDouble("MIN")
        normalizedMaxValue = bundle.getDouble("MAX")
    }

    interface OnRangeSeekBarListener {
        fun onValuesChanged(minValue: Float, maxValue: Float)
        fun onValuesChanged(minValue: Int, maxValue: Int)
    }

    enum class Thumb {
        MIN, MAX
    }
}
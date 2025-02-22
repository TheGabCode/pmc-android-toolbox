package com.paulmarkcastillo.androidtoolbox.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.paulmarkcastillo.androidtoolbox.R
import com.paulmarkcastillo.androidtoolbox.converters.DisplayUnitConverter

@SuppressLint("AppCompatCustomView")
class CustomButton(context: Context, attrs: AttributeSet?) : MaterialButton(context, attrs) {
    private lateinit var mainTextPaint: Paint
    private lateinit var subTextPaint: Paint
    private var primaryColor = 0
    private var secondaryColor = 0
    private var highlighted: Boolean
    private var roundedCorners: Boolean
    private var subText: String?
    private var subTextSize = textSize
    private var subTextColor = 0

    // Padding
    private var leftPadding = 0f
    private var rightPadding = 0f
    private var topPadding = 0f
    private var bottomPadding = 0f
    private var padding = 0f
    private var startPadding = 0f
    private var endPadding = 0f

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomButton,
            0,
            0
        ).apply {
            highlighted = getBoolean(R.styleable.CustomButton_highlighted, false)

            primaryColor = getColor(
                R.styleable.CustomButton_primaryColor,
                ContextCompat.getColor(context, android.R.color.black)
            )

            secondaryColor = getColor(
                R.styleable.CustomButton_secondaryColor,
                ContextCompat.getColor(context, android.R.color.white)
            )

            roundedCorners = getBoolean(R.styleable.CustomButton_roundedCorners, true)

            cornerRadius = if (roundedCorners) {
                getDimension(R.styleable.CustomButton_cornerRadius, convertDpToPx(8f)).toInt()
            } else {
                0
            }

            strokeWidth =
                getDimension(R.styleable.CustomButton_strokeWidth, convertDpToPx(1.5f)).toInt()

            if (highlighted) {
                setTextColor(secondaryColor)
            } else {
                setTextColor(primaryColor)
            }

            subText = getString(R.styleable.CustomButton_subText)

            subTextColor = getColor(R.styleable.CustomButton_subTextColor, currentTextColor)

            subTextSize =
                getDimension(R.styleable.CustomButton_subTextSize, (textSize * .72).toFloat())

            icon = getDrawable(R.styleable.CustomButton_icon)
            iconGravity = ICON_GRAVITY_TEXT_START

            padding = getDimension(R.styleable.CustomButton_android_padding, 0f)
            startPadding =
                getDimension(R.styleable.CustomButton_android_paddingStart, 0f)
            endPadding =
                getDimension(R.styleable.CustomButton_android_paddingEnd, 0f)
            leftPadding =
                getDimension(R.styleable.CustomButton_android_paddingLeft, startPadding)
            rightPadding =
                getDimension(R.styleable.CustomButton_android_paddingRight, endPadding)
            topPadding =
                getDimension(R.styleable.CustomButton_android_paddingTop, 0f)
            bottomPadding =
                getDimension(R.styleable.CustomButton_android_paddingBottom, 0f)

            gravity = getInt(R.styleable.CustomButton_android_gravity, Gravity.CENTER)
        }

        context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textAllCaps)).apply {
            isAllCaps = getBoolean(0, false)
            recycle()
        }

        if (!subText.isNullOrEmpty()) {
            mainTextPaint = Paint()
            subTextPaint = Paint()
            mainTextPaint.textSize = textSize
            mainTextPaint.color = currentTextColor
            mainTextPaint.typeface = typeface
            mainTextPaint.textAlign = Paint.Align.CENTER
            mainTextPaint.isAntiAlias = true

            subTextPaint.textSize = subTextSize
            subTextPaint.typeface = typeface
            subTextPaint.color = subTextColor
            subTextPaint.textAlign = Paint.Align.CENTER
            subTextPaint.isAntiAlias = true
        }

        if (highlighted) {
            backgroundTintList = ColorStateList.valueOf(primaryColor)
        } else {
            strokeColor = ColorStateList.valueOf(primaryColor)
            backgroundTintList = ColorStateList.valueOf(secondaryColor)
            val intPadding = padding.toInt()
            if (intPadding > 0) {
                setPadding(
                    paddingLeft + intPadding,
                    paddingTop + intPadding,
                    paddingRight + intPadding,
                    paddingBottom + intPadding
                )
            } else {
                setPadding(
                    paddingLeft + leftPadding.toInt(),
                    paddingTop + topPadding.toInt(),
                    paddingRight + rightPadding.toInt(),
                    paddingBottom + bottomPadding.toInt()
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!subText.isNullOrEmpty()) {
            val xCenterCoordinate = (width * .5).toFloat()
            val mainTextYCoordinates = (height * .48).toFloat()
            val subTextYCoordinates = (height * .78).toFloat()
            canvas.drawText(
                text.toString(),
                xCenterCoordinate,
                mainTextYCoordinates,
                mainTextPaint
            )
            canvas.drawText(
                subText ?: "",
                xCenterCoordinate,
                subTextYCoordinates,
                subTextPaint
            )
        } else {
            super.onDraw(canvas)
        }
    }

    private fun convertDpToPx(dp: Float): Float {
        val displayUnitConverter = DisplayUnitConverter()
        return displayUnitConverter.convertDpToPx(dp)
    }
}
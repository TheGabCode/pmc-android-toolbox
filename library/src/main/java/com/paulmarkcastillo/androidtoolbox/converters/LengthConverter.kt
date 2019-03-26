package com.paulmarkcastillo.androidtoolbox.converters

class LengthConverter : BaseConverter() {

    fun convertInchToFt(inch: Double): Double {
        return inch / 12
    }

    fun convertInchToFtString(inch: Double): String {
        val feet: Int = (inch / 12).toInt()
        val remainingInches = (inch % 12).toInt()
        return "$feet'$remainingInches\""
    }

    fun convertInchToCm(inch: Double): Double {
        return inch * 2.54
    }

    fun convertCmToInch(cm: Double): Double {
        return cm / 2.54
    }

}
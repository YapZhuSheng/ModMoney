package com.modmoney.util

import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Currency-agnostic money formatting.
 *
 * Documented decision: the app stores plain numbers and shows a neutral
 * thousands-separated figure rather than hardcoding a currency symbol, so the
 * same build works for any market. Amounts are shown without decimals because
 * mod pricing is realistically whole-unit.
 */
object Money {

    private val plain = DecimalFormat("#,##0")
    private val oneDp = DecimalFormat("#,##0.0")

    fun format(value: Double): String = plain.format(value.roundToLong())

    /** Signed format for variance display, e.g. "+1,200" / "-600". */
    fun signed(value: Double): String {
        val rounded = value.roundToLong()
        return when {
            rounded > 0 -> "+${plain.format(rounded)}"
            rounded < 0 -> "-${plain.format(abs(rounded))}"
            else -> "0"
        }
    }

    /** Compact form for tight spaces: 118,500 -> "118.5k". */
    fun compact(value: Double): String {
        val v = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            v >= 10_000_000 -> "$sign${oneDp.format(v / 1_000_000)}M"
            v >= 100_000 -> "$sign${plain.format((v / 1_000).roundToLong())}k"
            v >= 10_000 -> "$sign${oneDp.format(v / 1_000)}k"
            else -> "$sign${plain.format(v.roundToLong())}"
        }
    }

    fun percent(fraction: Double): String =
        "${(fraction * 100).roundToLong()}%"
}

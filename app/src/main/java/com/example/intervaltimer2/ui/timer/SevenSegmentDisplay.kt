package com.example.intervaltimer2.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 本物のカクカクした7セグメントLEDデジタル表示を行なうコンポーネント
 */
@Composable
fun SevenSegmentDisplay(
    text: String,
    activeColor: Color,
    inactiveColor: Color = Color(0xFF222222),
    digitWidth: Dp = 38.dp,
    digitHeight: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { _, char ->
            if (char == ':') {
                ColonSegment(
                    activeColor = activeColor,
                    height = digitHeight
                )
            } else if (char.isDigit()) {
                SingleDigitSegment(
                    digit = char,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    width = digitWidth,
                    height = digitHeight
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun SingleDigitSegment(
    digit: Char,
    activeColor: Color,
    inactiveColor: Color,
    width: Dp,
    height: Dp
) {
    val flags = segmentMap[digit] ?: booleanArrayOf(false, false, false, false, false, false, false)

    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val thickness = w * 0.18f // バーの太さ

        // 7セグメント (a: Top, b: TopRight, c: BottomRight, d: Bottom, e: BottomLeft, f: TopLeft, g: Center)

        // a (Top)
        drawRoundRect(
            color = if (flags[0]) activeColor else inactiveColor,
            topLeft = Offset(thickness * 0.5f, 0f),
            size = Size(w - thickness, thickness),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // b (Top-Right)
        drawRoundRect(
            color = if (flags[1]) activeColor else inactiveColor,
            topLeft = Offset(w - thickness, thickness * 0.5f),
            size = Size(thickness, (h * 0.5f) - thickness * 0.5f),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // c (Bottom-Right)
        drawRoundRect(
            color = if (flags[2]) activeColor else inactiveColor,
            topLeft = Offset(w - thickness, (h * 0.5f) + thickness * 0.2f),
            size = Size(thickness, (h * 0.5f) - thickness * 0.5f),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // d (Bottom)
        drawRoundRect(
            color = if (flags[3]) activeColor else inactiveColor,
            topLeft = Offset(thickness * 0.5f, h - thickness),
            size = Size(w - thickness, thickness),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // e (Bottom-Left)
        drawRoundRect(
            color = if (flags[4]) activeColor else inactiveColor,
            topLeft = Offset(0f, (h * 0.5f) + thickness * 0.2f),
            size = Size(thickness, (h * 0.5f) - thickness * 0.5f),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // f (Top-Left)
        drawRoundRect(
            color = if (flags[5]) activeColor else inactiveColor,
            topLeft = Offset(0f, thickness * 0.5f),
            size = Size(thickness, (h * 0.5f) - thickness * 0.5f),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )

        // g (Center)
        drawRoundRect(
            color = if (flags[6]) activeColor else inactiveColor,
            topLeft = Offset(thickness * 0.5f, (h * 0.5f) - (thickness * 0.5f)),
            size = Size(w - thickness, thickness),
            cornerRadius = CornerRadius(thickness * 0.3f, thickness * 0.3f)
        )
    }
}

@Composable
private fun ColonSegment(
    activeColor: Color,
    height: Dp
) {
    Canvas(
        modifier = Modifier
            .width(18.dp)
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val dotRadius = w * 0.25f

        // 上のドット
        drawCircle(
            color = activeColor,
            radius = dotRadius,
            center = Offset(w / 2f, h * 0.32f)
        )

        // 下のドット
        drawCircle(
            color = activeColor,
            radius = dotRadius,
            center = Offset(w / 2f, h * 0.68f)
        )
    }
}

// 0~9のセグメントフラグ (a, b, c, d, e, f, g)
private val segmentMap = mapOf(
    '0' to booleanArrayOf(true, true, true, true, true, true, false),
    '1' to booleanArrayOf(false, true, true, false, false, false, false),
    '2' to booleanArrayOf(true, true, false, true, true, false, true),
    '3' to booleanArrayOf(true, true, true, true, false, false, true),
    '4' to booleanArrayOf(false, true, true, false, false, true, true),
    '5' to booleanArrayOf(true, false, true, true, false, true, true),
    '6' to booleanArrayOf(true, false, true, true, true, true, true),
    '7' to booleanArrayOf(true, true, true, false, false, false, false),
    '8' to booleanArrayOf(true, true, true, true, true, true, true),
    '9' to booleanArrayOf(true, true, true, true, false, true, true)
)

package com.kenza.callsim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Key(val digit: Char, val letters: String)

private val KEYS = listOf(
    Key('1', ""), Key('2', "ABC"), Key('3', "DEF"),
    Key('4', "GHI"), Key('5', "JKL"), Key('6', "MNO"),
    Key('7', "PQRS"), Key('8', "TUV"), Key('9', "WXYZ"),
    Key('*', ""), Key('0', "+"), Key('#', "")
)

/**
 * The 3x4 iOS dial pad. [keyBackground] lets the in-call variant use a
 * translucent fill while the home dialer uses solid grey circles.
 */
@Composable
fun Keypad(
    onKey: (Char) -> Unit,
    modifier: Modifier = Modifier,
    keyBackground: Color = Color(0xFF1C1C1E),
    digitColor: Color = Color.White,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        KEYS.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        background = keyBackground,
                        digitColor = digitColor,
                        onClick = { onKey(key.digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    background: Color,
    digitColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(
                text = key.digit.toString(),
                color = digitColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
            if (key.letters.isNotEmpty()) {
                androidx.compose.material3.Text(
                    text = key.letters,
                    color = digitColor.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

/** A round control button (mute / speaker / keypad / end-call etc.). */
@Composable
fun RoundControl(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    size: Int = 72,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}

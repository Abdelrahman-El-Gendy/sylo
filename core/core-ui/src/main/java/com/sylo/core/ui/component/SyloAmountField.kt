package com.sylo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing

/**
 * A large, optically-centered money input used wherever the user types an amount
 * (opening balance, new expense, …).
 *
 * The currency symbol hugs the number as a single unit, and the field grows with the
 * value it holds: an invisible "sizer" [Text] (the current value, or `0.00` while
 * empty) establishes the exact width and the [BasicTextField] fills it, so digits
 * never clip or drift off-centre as you type. A short brand-tinted underline hints
 * that the amount is editable.
 *
 * Input is not filtered here — pass an [onAmountChange] that keeps only the
 * characters you want (digits and a decimal point).
 */
@Composable
fun SyloAmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    textStyle: TextStyle = MaterialTheme.typography.displayMedium,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currencySymbol.isNotEmpty()) {
                Text(currencySymbol, style = MaterialTheme.typography.headlineMedium, color = SyloBrandCyan)
                Spacer(Modifier.width(SyloSpacing.stackSm))
            }
            Box(contentAlignment = Alignment.Center) {
                // Sizer: invisible, but sets the field width to the content PLUS a little
                // trailing room so the caret at the end never forces the field to scroll
                // and clip the leading digits.
                Text(
                    text = amount.ifEmpty { "0.00" },
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .alpha(0f)
                        .padding(end = 12.dp),
                )
                if (amount.isEmpty()) {
                    Text("0.00", style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    textStyle = textStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(SyloBrandCyan),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Spacer(Modifier.height(SyloSpacing.stackSm))
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(2.dp)
                .clip(MaterialTheme.shapes.small)
                .background(SyloBrandCyan.copy(alpha = 0.7f)),
        )
    }
}

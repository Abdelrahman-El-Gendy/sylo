package com.sylo.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sylo.core.ui.theme.SyloIncomeGreen
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.PI
import kotlin.math.sin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.Sparkline
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.SyloTopBar
import com.sylo.core.ui.component.TransactionRow
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing

@Composable
fun DashboardRoute(
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onAddExpense: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(uiState, onTransactionClick, onSeeAllClick, onAddExpense)
}

@Composable
private fun DashboardScreen(
    uiState: DashboardUiState,
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onAddExpense: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SyloSpacing.containerMargin),
        verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackMd),
    ) {
        SyloTopBar(onAddClick = onAddExpense)

        if (!uiState.hasData) {
            EmptyDashboard(onAddExpense)
            return@Column
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BalanceRing(
                totalBalance = uiState.totalBalance,
                remainingBalance = uiState.remainingBalance,
                fraction = uiState.remainingFraction,
                changePercent = uiState.changePercent,
                changePositive = uiState.changePositive,
            )
            Spacer(Modifier.height(SyloSpacing.stackMd))
            WithdrawalIndicator(amount = uiState.withdrawals)
        }

        // Spending this month + trend
        SyloCard {
            Column(Modifier.padding(SyloSpacing.stackMd)) {
                Text("This Month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uiState.monthSpend, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (uiState.spendTrend.size >= 2) {
                    Spacer(Modifier.height(SyloSpacing.stackSm))
                    Sparkline(points = uiState.spendTrend, modifier = Modifier.fillMaxWidth().height(80.dp))
                }
            }
        }

        // Computed insight (only when we have a dominant category)
        uiState.topCategory?.let { category ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().border(1.dp, SyloBrandCyan.copy(alpha = 0.5f), MaterialTheme.shapes.large),
            ) {
                Row(Modifier.padding(SyloSpacing.stackMd), horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = SyloBrandCyan)
                    Column {
                        Text("Sylo Insight", style = MaterialTheme.typography.labelSmall, color = SyloBrandCyan)
                        Text(
                            "$category is your top spending category this month (${uiState.topCategoryPercent}%).",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        // Recent transactions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Recent Transactions")
            Text(
                "View all",
                style = MaterialTheme.typography.labelSmall,
                color = SyloBrandCyan,
                // clickable + padding gives the tap target a 48dp-tall hit area (a11y).
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            )
        }
        uiState.recentTransactions.forEach { tx ->
            TransactionRow(transaction = tx, onClick = { onTransactionClick(tx.id) })
        }

        Spacer(Modifier.height(SyloSpacing.sectionGap))
    }
}

/**
 * The (static) Total Balance inside a thin, glowing cyan ring. The arc + an animated
 * water fill both rise to the share of the balance still remaining; the remaining
 * amount is shown beside the (unchanging) total.
 */
@Composable
private fun BalanceRing(
    totalBalance: String,
    remainingBalance: String,
    fraction: Float,
    changePercent: String?,
    changePositive: Boolean,
    modifier: Modifier = Modifier,
) {
    // Start from a full ring (the whole balance) and sweep down to what remains.
    val ringAnim = remember { Animatable(1f) }
    LaunchedEffect(fraction) {
        ringAnim.animateTo(fraction.coerceIn(0f, 1f), animationSpec = tween(durationMillis = 1400))
    }
    val animated = ringAnim.value
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        // Animated water fill, clipped to the inner circle behind the text.
        WaterFill(
            level = fraction,
            modifier = Modifier.size(228.dp).clip(CircleShape),
        )
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = 6.dp.toPx()
            // Full track.
            drawArc(track, -90f, 360f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            if (animated > 0f) {
                val sweep = animated * 360f
                // Soft glow: a wider, translucent cyan arc under the sharp one.
                drawArc(SyloBrandCyan.copy(alpha = 0.22f), -90f, sweep, false, style = Stroke(width = stroke * 3.5f, cap = StrokeCap.Round))
                drawArc(SyloBrandCyan, -90f, sweep, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "TOTAL BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(totalBalance, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Remaining ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    remainingBalance,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SyloBrandCyan,
                )
            }
            changePercent?.let {
                Spacer(Modifier.height(6.dp))
                val pillColor = if (changePositive) SyloIncomeGreen else MaterialTheme.colorScheme.error
                Surface(shape = RoundedCornerShape(50), color = pillColor.copy(alpha = 0.15f)) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = pillColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

/**
 * A cyan liquid that rises to [level] (0..1) of the height with two gently animated
 * sine waves on its surface. Meant to be placed inside a circular clip.
 */
@Composable
private fun WaterFill(level: Float, modifier: Modifier = Modifier) {
    // Start full and drain down to the remaining level, matching the ring.
    val waterAnim = remember { Animatable(1f) }
    LaunchedEffect(level) {
        waterAnim.animateTo(level.coerceIn(0f, 1f), animationSpec = tween(durationMillis = 1400))
    }
    val animatedLevel = waterAnim.value
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
        label = "wavePhase",
    )

    Canvas(modifier = modifier) {
        if (animatedLevel <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val waveHeight = h * 0.025f
        val baseY = h * (1f - animatedLevel)
        val steps = 64

        fun wave(shift: Float): Path = Path().apply {
            moveTo(0f, baseY)
            for (i in 0..steps) {
                val x = w * i / steps
                val y = baseY + waveHeight * sin(phase + shift + (x / w) * 2f * PI.toFloat() * 1.5f)
                lineTo(x, y)
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Back wave (dimmer, offset) then front wave for a sense of depth.
        drawPath(wave(PI.toFloat()), color = SyloBrandCyan.copy(alpha = 0.16f))
        drawPath(wave(0f), color = SyloBrandCyan.copy(alpha = 0.30f))
    }
}

/** "● Withdrawals: $X — This Month" indicator shown below the ring. */
@Composable
private fun WithdrawalIndicator(amount: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(SyloBrandCyan))
            Spacer(Modifier.width(8.dp))
            Text("Withdrawals: ", style = MaterialTheme.typography.bodyLarge)
            Text(amount, style = MaterialTheme.typography.bodyLarge, color = SyloBrandCyan, fontWeight = FontWeight.Bold)
        }
        Text(
            "This Month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyDashboard(onAddExpense: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.padding(bottom = 12.dp))
        Text("No transactions yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tap the mic to add your first expense by voice, or add one manually.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SyloSpacing.stackLg))
        com.sylo.core.ui.component.SyloPrimaryButton(text = "Add Expense", onClick = onAddExpense)
    }
}

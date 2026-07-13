package com.sylo.feature.auth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import kotlinx.coroutines.launch

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Registered/returning users skip straight through once the state has loaded.
    LaunchedEffect(state) {
        if (state == OnboardingState.Skip) onFinished()
    }

    if (state == OnboardingState.Show) {
        OnboardingPager(onGetStarted = viewModel::complete)
    }
}

private data class OnbPage(
    val icon: ImageVector?,
    val label: String,
    val value: String,
    val fraction: Float,
    val showBars: Boolean,
    val title: String,
    val body: String,
)

private val onboardingPages = listOf(
    OnbPage(
        icon = Icons.Filled.Mic,
        label = "CAPTURE",
        value = "Voice",
        fraction = 0.55f,
        showBars = false,
        title = "Track expenses by voice",
        body = "Just say what you spent — Sylo captures the amount and category in seconds.",
    ),
    OnbPage(
        icon = null,
        label = "INSIGHTS",
        value = "84%",
        fraction = 0.72f,
        showBars = true,
        title = "See the full picture",
        body = "Automated insights and clear visualizations help you understand your spending habits at a glance.",
    ),
    OnbPage(
        icon = Icons.Filled.Lock,
        label = "SECURE",
        value = "Locked",
        fraction = 1f,
        showBars = false,
        title = "Private & secure by design",
        body = "Your data is encrypted on-device and locked behind your PIN and biometrics. Nothing leaves your phone.",
    ),
)

@Composable
private fun OnboardingPager(onGetStarted: () -> Unit) {
    val pages = remember { onboardingPages }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = SyloSpacing.containerMargin, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Sylo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = SyloBrandCyan,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        PageDots(count = pages.size, current = pagerState.currentPage)
        Spacer(Modifier.height(SyloSpacing.stackLg))

        val isLast = pagerState.currentPage == pages.lastIndex
        OnboardingButton(
            label = if (isLast) "Get Started" else "Next",
            filled = isLast,
        ) {
            if (isLast) onGetStarted()
            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        }
        Spacer(Modifier.height(SyloSpacing.stackSm))
    }
}

@Composable
private fun OnboardingPageContent(page: OnbPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HeroRing(page)
        Spacer(Modifier.height(SyloSpacing.sectionGap))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SyloSpacing.stackMd))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(280.dp),
        )
    }
}

@Composable
private fun HeroRing(page: OnbPage) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val stroke = 12.dp.toPx()
            drawArc(track, -90f, 360f, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
            val sweep = page.fraction.coerceIn(0f, 1f) * 360f
            drawArc(SyloBrandCyan.copy(alpha = 0.25f), -90f, sweep, false, style = Stroke(width = stroke * 2.4f, cap = StrokeCap.Round))
            drawArc(SyloBrandCyan, -90f, sweep, false, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }

        if (page.showBars) {
            Row(
                modifier = Modifier.width(140.dp).height(90.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(0.30f, 0.50f, 0.45f, 0.75f, 1f).forEachIndexed { i, h ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(h)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(SyloBrandCyan.copy(alpha = (0.25f + i * 0.15f).coerceAtMost(1f))),
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (page.icon != null) {
                Icon(page.icon, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(SyloSpacing.stackSm))
            }
            Text(
                page.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                page.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SyloBrandCyan,
            )
        }

        // Floating glass mini-cards, echoing the Stitch design.
        GlassChip(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            label = "+12.4%",
            modifier = Modifier.align(Alignment.TopEnd),
        )
        GlassChip(
            icon = Icons.Filled.ShoppingBag,
            label = "Saving",
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun GlassChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val active = i == current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) SyloBrandCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
private fun OnboardingButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val container = if (filled) SyloBrandCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val content = if (filled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(container)
            .then(if (filled) Modifier else Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = content)
        if (!filled) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = content)
        }
    }
}

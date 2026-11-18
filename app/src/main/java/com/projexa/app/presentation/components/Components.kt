package com.projexa.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projexa.app.R
import com.projexa.app.domain.models.Direction
import com.projexa.app.domain.models.ProximityLevel
import com.projexa.app.domain.models.ProximityThresholds
import com.projexa.app.domain.models.SensorSnapshot
import com.projexa.app.presentation.theme.ProjexaColors
import kotlin.math.cos
import kotlin.math.sin

private val CardShape = RoundedCornerShape(20.dp)

/** Small uppercase section label. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = ProjexaColors.TextSecondary) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(ProjexaColors.Card)
            .border(1.dp, ProjexaColors.Outline, CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) Overline(title, color = ProjexaColors.Cyan)
        content()
    }
}

/** Large (>=56dp) primary action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ProjexaColors.Blue,
            disabledContainerColor = ProjexaColors.Track,
            disabledContentColor = ProjexaColors.TextSecondary,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (enabled) ProjexaColors.Blue.copy(alpha = 0.6f) else ProjexaColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = ProjexaColors.Blue.copy(alpha = 0.06f)),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = ProjexaColors.Cyan)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) ProjexaColors.TextPrimary else ProjexaColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

/** Compact rounded status chip, e.g. "Connected". */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusDot(color, 8)
        Text(text, style = MaterialTheme.typography.labelLarge, color = ProjexaColors.TextPrimary, maxLines = 1)
    }
}

@Composable
fun BannerText(text: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusDot(color)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = ProjexaColors.TextPrimary)
    }
}

/** Large tappable tile with an icon, title and subtitle. */
@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(CardShape)
            .background(ProjexaColors.Card)
            .border(1.dp, ProjexaColors.Outline, CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 120.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(ProjexaColors.Blue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = ProjexaColors.Cyan) }
        Text(title, style = MaterialTheme.typography.titleMedium, color = ProjexaColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = ProjexaColors.TextSecondary)
    }
}

/**
 * One direction of the belt. Shows real data only: when [live] is false the tile shows "--"
 * instead of the last (possibly stale) value.
 */
@Composable
fun DirectionTile(
    direction: Direction,
    distanceCm: Float?,
    level: ProximityLevel,
    motorOn: Boolean?,
    live: Boolean,
    modifier: Modifier = Modifier,
) {
    val target = if (live) ProjexaColors.forLevel(level) else ProjexaColors.TextSecondary
    val accent by animateColorAsState(target, tween(250), label = "tileAccent")
    val distanceText = when {
        !live -> "--"
        distanceCm == null -> stringResource(R.string.tile_no_echo)
        else -> "%.0f cm".format(distanceCm)
    }
    val levelText = if (live) level.localized() else stringResource(R.string.tile_unavailable)
    val motorText = when {
        !live || motorOn == null -> stringResource(R.string.motor_unknown)
        motorOn -> stringResource(R.string.motor_on)
        else -> stringResource(R.string.motor_off)
    }
    val directionName = direction.localized()
    val description = stringResource(R.string.tile_description, directionName, distanceText, levelText, motorText)
    val tileShape = RoundedCornerShape(18.dp)

    Column(
        modifier
            .heightIn(min = 118.dp)
            .clip(tileShape)
            .background(ProjexaColors.Surface)
            .border(1.dp, if (live) accent.copy(alpha = 0.55f) else ProjexaColors.Outline, tileShape)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
            Text(
                directionName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ProjexaColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (live && motorOn == true) StatusDot(ProjexaColors.Cyan, 10)
        }
        Text(
            distanceText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ProjexaColors.TextPrimary,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Row(
            Modifier.clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusDot(accent, 8)
            Text(levelText, style = MaterialTheme.typography.bodyMedium, color = accent, fontWeight = FontWeight.SemiBold)
        }
        Text(
            motorText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (live && motorOn == true) ProjexaColors.Cyan else ProjexaColors.TextSecondary,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/**
 * Circular belt view: four arcs (front at top) coloured by proximity, a cyan dot outside an arc when
 * that motor is on, and the nearest obstacle in the centre. Screen readers get one full summary.
 */
@Composable
fun BeltRadar(
    snapshot: SensorSnapshot?,
    live: Boolean,
    thresholds: ProximityThresholds,
    modifier: Modifier = Modifier,
) {
    val isLive = live && snapshot != null
    val colors = Direction.entries.associateWith { d ->
        val target = if (isLive) ProjexaColors.forLevel(thresholds.classify(snapshot?.distancesCm?.get(d)))
        else ProjexaColors.TextSecondary.copy(alpha = 0.35f)
        animateColorAsState(target, tween(300), label = "arc$d").value
    }
    val motors = Direction.entries.associateWith { isLive && snapshot?.motorsOn?.get(it) == true }
    val nearest = if (isLive) {
        snapshot!!.distancesCm.entries.filter { it.value != null }.minByOrNull { it.value!! }
    } else {
        null
    }

    // One spoken summary for TalkBack.
    val parts = Direction.entries.map { d ->
        val dist = snapshot?.distancesCm?.get(d)
        val distanceText = when {
            !isLive -> "--"
            dist == null -> stringResource(R.string.tile_no_echo)
            else -> "%.0f cm".format(dist)
        }
        val levelText = if (isLive) thresholds.classify(dist).localized() else stringResource(R.string.tile_unavailable)
        val motorText = when {
            !isLive || snapshot?.motorsOn == null -> stringResource(R.string.motor_unknown)
            motors[d] == true -> stringResource(R.string.motor_on)
            else -> stringResource(R.string.motor_off)
        }
        stringResource(R.string.tile_description, d.localized(), distanceText, levelText, motorText)
    }
    val summary = parts.joinToString(". ")

    val track = ProjexaColors.Track
    val outline = ProjexaColors.Outline
    val cyan = ProjexaColors.Cyan

    Box(
        modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) { contentDescription = summary },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(22.dp).clearAndSetSemantics { }) {
            val stroke = 22.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f

            // Subtle inner glow disc and rings.
            drawCircle(
                brush = Brush.radialGradient(listOf(cyan.copy(alpha = 0.08f), Color.Transparent), center, radius),
                radius = radius,
                center = center,
            )
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            val centers = mapOf(
                Direction.FRONT to -90f,
                Direction.RIGHT to 0f,
                Direction.BACK to 90f,
                Direction.LEFT to 180f,
            )
            val sweep = 70f
            centers.forEach { (d, mid) ->
                drawArc(
                    color = colors.getValue(d),
                    startAngle = mid - sweep / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                if (motors[d] == true) {
                    val r = radius + stroke / 2f + 10.dp.toPx()
                    val rad = Math.toRadians(mid.toDouble())
                    val p = Offset(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat())
                    drawCircle(cyan.copy(alpha = 0.25f), 9.dp.toPx(), p)
                    drawCircle(cyan, 5.dp.toPx(), p)
                }
            }
        }

        // Direction letters just inside the ring.
        val labelMod = Modifier.padding(58.dp).clearAndSetSemantics { }
        Box(Modifier.fillMaxSize().then(labelMod)) {
            Text(Direction.FRONT.localized(), Modifier.align(Alignment.TopCenter), style = MaterialTheme.typography.labelSmall, color = ProjexaColors.TextSecondary)
            Text(Direction.BACK.localized(), Modifier.align(Alignment.BottomCenter), style = MaterialTheme.typography.labelSmall, color = ProjexaColors.TextSecondary)
            Text(Direction.LEFT.localized(), Modifier.align(Alignment.CenterStart), style = MaterialTheme.typography.labelSmall, color = ProjexaColors.TextSecondary)
            Text(Direction.RIGHT.localized(), Modifier.align(Alignment.CenterEnd), style = MaterialTheme.typography.labelSmall, color = ProjexaColors.TextSecondary)
        }

        Column(
            Modifier.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.radar_nearest).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ProjexaColors.TextSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                nearest?.value?.let { "%.0f".format(it) } ?: "--",
                style = MaterialTheme.typography.displaySmall,
                color = nearest?.let { colors.getValue(it.key) } ?: ProjexaColors.TextPrimary,
            )
            Text(
                when {
                    nearest != null -> "cm · ${nearest.key.localized()}"
                    isLive -> stringResource(R.string.lvl_no_reading)
                    else -> stringResource(R.string.tile_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ProjexaColors.TextSecondary,
            )
        }
    }
}

/** Radar plus a 2x2 grid of detailed sensor tiles. */
@Composable
fun BeltMonitor(
    snapshot: SensorSnapshot?,
    live: Boolean,
    thresholds: ProximityThresholds,
    modifier: Modifier = Modifier,
    showRadar: Boolean = true,
) {
    @Composable
    fun tile(d: Direction, m: Modifier) {
        val dist = snapshot?.distancesCm?.get(d)
        DirectionTile(
            direction = d,
            distanceCm = dist,
            level = thresholds.classify(dist),
            motorOn = snapshot?.motorsOn?.get(d),
            live = live && snapshot != null,
            modifier = m,
        )
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (showRadar) BeltRadar(snapshot, live, thresholds)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tile(Direction.FRONT, Modifier.weight(1f))
            tile(Direction.BACK, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tile(Direction.LEFT, Modifier.weight(1f))
            tile(Direction.RIGHT, Modifier.weight(1f))
        }
    }
}

/** Accessible segmented control: options side by side, exactly one selected (radio-button semantics). */
@Composable
fun <T> SegmentedChoice(options: List<Pair<String, T>>, current: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val outer = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(outer)
            .background(ProjexaColors.Surface)
            .border(1.dp, ProjexaColors.Outline, outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (label, value) ->
            val selected = value == current
            val bg by animateColorAsState(if (selected) ProjexaColors.Blue else Color.Transparent, tween(200), label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else ProjexaColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Branded text field matching the card style. */
@Composable
fun ProjexaTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ProjexaColors.Blue,
            unfocusedBorderColor = ProjexaColors.Outline,
            focusedContainerColor = ProjexaColors.Surface,
            unfocusedContainerColor = ProjexaColors.Surface,
            focusedLabelColor = ProjexaColors.Cyan,
            unfocusedLabelColor = ProjexaColors.TextSecondary,
            cursorColor = ProjexaColors.Cyan,
            focusedTextColor = ProjexaColors.TextPrimary,
            unfocusedTextColor = ProjexaColors.TextPrimary,
        ),
    )
}

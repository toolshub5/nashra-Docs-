package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class PendingSignature(
    val signerName: String,
    val type: String, // "DRAWN" or "TYPED"
    val data: String,
    val colorHex: String,
    val pageIndex: Int
)

/**
 * SignatureOverlayPlacer provides an interactive overlay directly on top of the document.
 * Users drag the signature canvas/badge into position on the document page, adjust scaling,
 * and confirm placement which calculates and stores the normalized coordinates (normX, normY)
 * to Room database.
 */
@Composable
fun SignatureOverlayPlacer(
    pendingSignature: PendingSignature,
    onConfirmPlacement: (normX: Float, normY: Float) -> Unit,
    onCancel: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // Initial position at center bottom
        var offsetX by remember { mutableFloatStateOf(containerWidthPx * 0.5f - 100f) }
        var offsetY by remember { mutableFloatStateOf(containerHeightPx * 0.65f) }
        var scale by remember { mutableFloatStateOf(1.0f) }

        val sigColor = parseColorSafely(pendingSignature.colorHex)

        // Semi-transparent backdrop to focus attention on placing signature
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Draggable Signature Overlay Box
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(
                    width = (200.dp * scale),
                    height = (90.dp * scale)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(10f, containerWidthPx - (200f * scale) - 10f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(10f, containerHeightPx - (90f * scale) - 100f)
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Drag indicator handle at corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = "Drag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (pendingSignature.type == "TYPED") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pendingSignature.data,
                        color = sigColor,
                        fontSize = (22 * scale).sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Cursive
                    )
                    Text(
                        text = "وقّع بواسطة: ${pendingSignature.signerName}",
                        color = Color.Gray,
                        fontSize = (9 * scale).sp
                    )
                }
            } else {
                // Render Drawn Points Path
                val points = remember(pendingSignature.data) {
                    parseDrawnPoints(pendingSignature.data)
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (points.size >= 2) {
                        val path = Path()
                        var first = true
                        for (pt in points) {
                            val px = (pt.x / 300f) * size.width
                            val py = (pt.y / 150f) * size.height
                            if (first) {
                                path.moveTo(px, py)
                                first = false
                            } else {
                                path.lineTo(px, py)
                            }
                        }
                        drawPath(
                            path = path,
                            color = sigColor,
                            style = Stroke(
                                width = 3.5f * scale,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }

        // Bottom Placement Action Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اسحب التوقيع لموضعه الدقيق في المستند",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }

                // Scale slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("الحجم:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.7f..1.8f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(scale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            // Compute normalized coordinates (0.0 to 1.0)
                            val normX = ((offsetX + (100f * scale)) / containerWidthPx).coerceIn(0.05f, 0.95f)
                            val normY = ((offsetY + (45f * scale)) / containerHeightPx).coerceIn(0.05f, 0.95f)
                            onConfirmPlacement(normX, normY)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تثبيت التوقيع هنا", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun parseDrawnPoints(data: String): List<Offset> {
    if (!data.startsWith("POINTS:")) return emptyList()
    val coords = data.removePrefix("POINTS:").split(";")
    val result = mutableListOf<Offset>()
    for (coord in coords) {
        val parts = coord.split(",")
        if (parts.size == 2) {
            val x = parts[0].toFloatOrNull() ?: continue
            val y = parts[1].toFloatOrNull() ?: continue
            result.add(Offset(x, y))
        }
    }
    return result
}

private fun parseColorSafely(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF0F766E)
    }
}

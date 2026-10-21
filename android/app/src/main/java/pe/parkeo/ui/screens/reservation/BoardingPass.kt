package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.theme.*
import pe.parkeo.util.DirectionsButton

/**
 * Componentes compartidos del pase de abordar (boarding pass) con QR real:
 * usados por ReservationsScreen y por la vista de reservas del admin/operador.
 */

@Composable
internal fun TicketBoardingPassDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val code = reservation.confirmationCode ?: "PKO-${reservation.id}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = extended.surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.spacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "BOARDING PASS DIGITAL",
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = extended.textTertiary,
                            modifier = Modifier.size(Dimens.iconSizeMedium)
                        )
                    }
                }

                // QR Code Presentation Box
                Surface(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeMatrix(
                            content = code,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Monospace code display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CÓDIGO DE RESERVA",
                        style = Typography.MonospaceTechnical,
                        fontSize = 10.sp,
                        color = extended.textTertiary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = code,
                        style = Typography.MonospaceTechnical,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Muestra este código al operador en puerta",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = extended.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    color = extended.borderSubtle,
                    thickness = Dimens.borderHairline
                )

                // Details Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardingPassRow(label = "Estacionamiento", value = reservation.parkingLotName)
                    BoardingPassRow(label = "Dirección", value = reservation.parkingLotAddress)
                    BoardingPassRow(
                        label = "Espacio asignado",
                        value = if (reservation.spaceNumber.isNotBlank()) reservation.spaceNumber else "En puerta",
                        isHighlight = true
                    )
                    BoardingPassRow(
                        label = "Placa del vehículo",
                        value = reservation.vehicleLicensePlate,
                        isHighlight = true
                    )
                    BoardingPassRow(label = "Inicio", value = formatDisplayDate(reservation.startTime))
                    BoardingPassRow(label = "Salida programada", value = formatDisplayDate(reservation.endTime))
                    reservation.totalPrice?.let { price ->
                        BoardingPassRow(
                            label = if (reservation.status == "COMPLETED") "Monto final" else "Tarifa estimada",
                            value = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                            isHighlight = true
                        )
                    }
                }

                // Route action if available
                val lat = reservation.parkingLotLatitude
                val lng = reservation.parkingLotLongitude
                if (lat != null && lng != null) {
                    Spacer(Modifier.height(4.dp))
                    DirectionsButton(latitude = lat, longitude = lng)
                }

                ParkeoButton(
                    text = "Cerrar",
                    onClick = onDismiss,
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun BoardingPassRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    val extended = ParkeoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extended.textSecondary
        )
        Text(
            text = value,
            style = if (isHighlight) Typography.MonospaceTechnical else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) extended.accent else extended.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * QR real (zxing) escaneable por cualquier lector: codifica el código de
 * confirmación de la reserva (ej. PKO-XXXXXXX).
 */
@Composable
internal fun QrCodeMatrix(
    content: String,
    modifier: Modifier = Modifier
) {
    val bitMatrix = remember(content) {
        try {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                512,
                512,
                mapOf(EncodeHintType.MARGIN to 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    Canvas(modifier = modifier) {
        val matrix = bitMatrix ?: return@Canvas
        val cols = matrix.width
        val rows = matrix.height
        if (cols == 0 || rows == 0) return@Canvas

        val cellSize = minOf(size.width / cols, size.height / rows)
        val paddingX = (size.width - cellSize * cols) / 2f
        val paddingY = (size.height - cellSize * rows) / 2f
        if (cellSize <= 0f) return@Canvas

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (matrix.get(c, r)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(paddingX + c * cellSize, paddingY + r * cellSize),
                        size = Size(cellSize * 0.96f, cellSize * 0.96f)
                    )
                }
            }
        }
    }
}

internal fun formatDisplayDate(raw: String): String {
    val clean = raw.take(16).replace("T", " ")
    return if (clean.length >= 10 && clean.contains("-")) {
        val parts = clean.split(" ")
        val date = parts[0].split("-")
        if (date.size == 3 && parts.size >= 2) "${date[2]}/${date[1]} ${parts[1]}" else clean
    } else clean
}

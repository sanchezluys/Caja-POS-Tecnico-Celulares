package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import com.example.util.CostValidator
import com.example.util.CurrencyFormatHelper
import com.example.util.PdfReceiptGenerator
import com.example.util.WhatsAppHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: RepairOrder,
    parts: List<OrderItemPart>,
    warrantyOrders: List<RepairOrder>,
    inventory: List<InventoryPart>,
    config: WorkshopConfig,
    onBack: () -> Unit,
    onUpdateStatusAndDiagnosis: (status: String, outcome: String?, diagnosis: String, laborCost: Double) -> Unit,
    onAddPart: (partName: String, quantity: Int, unitCost: Double, unitPrice: Double, inventoryPartId: Long?) -> Unit,
    onRemovePart: (OrderItemPart) -> Unit,
    onDeliverAndClose: (paymentMethod: String, amountPaid: Double) -> Unit,
    onCreateWarrantyOrder: (reportedIssue: String, warrantyCost: Double, conditionNotes: String) -> Unit,
    onDeleteOrder: () -> Unit,
    onUpdateOrderPhotos: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val currency = config.currency.ifBlank { "$" }

    var currentStatus by remember(order.status) { mutableStateOf(order.status) }
    var currentOutcome by remember(order.repairOutcome) { mutableStateOf(order.repairOutcome) }
    var currentDiagnosis by remember(order.repairDiagnosis) { mutableStateOf(order.repairDiagnosis) }
    var currentLaborCostStr by remember(order.laborCost) { mutableStateOf(if (order.laborCost > 0) String.format(Locale.US, "%.2f", order.laborCost) else "") }

    // Dialog state
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showDeliverDialog by remember { mutableStateOf(false) }
    var showWarrantyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReceiptGeneratedDialog by remember { mutableStateOf(false) }
    var generatedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }

    val photoList = remember(order.photoUri) { order.getPhotoList() }

    // Multiple Photo picker launcher (Gallery)
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newUris = uris.map { it.toString() }.filter { it.isNotBlank() }
        if (newUris.isNotEmpty()) {
            val updated = (photoList + newUris).distinct()
            onUpdateOrderPhotos?.invoke(RepairOrder.joinPhotos(updated))
            Toast.makeText(context, "${newUris.size} foto(s) agregada(s)", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Capture launcher
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val updated = (photoList + tempCameraUri.toString()).distinct()
            tempCameraUri = null
            onUpdateOrderPhotos?.invoke(RepairOrder.joinPhotos(updated))
            Toast.makeText(context, "Foto capturada y guardada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File.createTempFile("order_${order.id}_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCamera: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val photoFile = File.createTempFile("order_${order.id}_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.orderNumber,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (order.isWarrantyOrder) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "GARANTÍA",
                                        color = Color(0xFF92400E),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${order.deviceBrand} ${order.deviceModel} • ${order.clientName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_from_detail")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("btn_delete_order")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Status Tracker Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTADO DEL SERVICIO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        val statusObj = OrderStatus.entries.find { it.name == currentStatus } ?: OrderStatus.INGRESADO
                        Surface(
                            color = Color(statusObj.colorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusObj.label,
                                color = Color(statusObj.colorHex),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Dropdown Selector
                    var expandedStatus by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedStatus,
                        onExpandedChange = { expandedStatus = !expandedStatus }
                    ) {
                        OutlinedTextField(
                            value = OrderStatus.entries.find { it.name == currentStatus }?.label ?: currentStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Actualizar Estado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("dropdown_order_status")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStatus,
                            onDismissRequest = { expandedStatus = false }
                        ) {
                            OrderStatus.entries.forEach { statusOption ->
                                DropdownMenuItem(
                                    text = { Text(statusOption.label) },
                                    onClick = {
                                        currentStatus = statusOption.name
                                        if (statusOption == OrderStatus.LISTO_ENTREGA && currentOutcome == null) {
                                            currentOutcome = "EXITOSA"
                                        }
                                        expandedStatus = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Outcome Selector Chips (Reparación Exitosa vs No Reparada)
                    Text(
                        text = "Resultado de la Reparación:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentOutcome == "EXITOSA",
                            onClick = { currentOutcome = if (currentOutcome == "EXITOSA") null else "EXITOSA" },
                            label = { Text("✓ Exitosa") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_outcome_success")
                        )
                        FilterChip(
                            selected = currentOutcome == "NO_EXITOSA",
                            onClick = { currentOutcome = if (currentOutcome == "NO_EXITOSA") null else "NO_EXITOSA" },
                            label = { Text("✗ No Reparado") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_outcome_failed")
                        )
                        FilterChip(
                            selected = currentOutcome == "CANCELADA",
                            onClick = { currentOutcome = if (currentOutcome == "CANCELADA") null else "CANCELADA" },
                            label = { Text("Cancelada") },
                            modifier = Modifier.testTag("chip_outcome_canceled")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Diagnosis field
                    OutlinedTextField(
                        value = currentDiagnosis,
                        onValueChange = { currentDiagnosis = it },
                        label = { Text("Diagnóstico Técnico / Solución Aplicada") },
                        placeholder = { Text("Detalle de las pruebas realizadas, reemplazo de componentes...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_diagnosis")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Labor Cost field
                    val laborValidation = CostValidator.validate(currentLaborCostStr, isRequired = false)
                    OutlinedTextField(
                        value = currentLaborCostStr,
                        onValueChange = { input ->
                            currentLaborCostStr = CostValidator.filterNumericInput(input)
                        },
                        label = { Text("Mano de Obra (COP)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = !laborValidation.isValid,
                        supportingText = {
                            if (!laborValidation.isValid) {
                                Text(laborValidation.errorMessage ?: "Costo inválido", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_labor_cost")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val check = CostValidator.validate(currentLaborCostStr, isRequired = false)
                            if (check.isValid) {
                                onUpdateStatusAndDiagnosis(currentStatus, currentOutcome, currentDiagnosis, check.value)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_save_status_diagnosis"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Estado y Diagnóstico")
                    }
                }
            }

            // 2. Client and Device Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLIENTE Y EQUIPO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(Date(order.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Client details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = order.clientName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = order.clientPhone.ifBlank { "Sin teléfono" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // WhatsApp Action button
                        if (order.clientPhone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val msg = WhatsAppHelper.buildNotificationMessage(order, config)
                                    WhatsAppHelper.openWhatsAppChat(context, order.clientPhone, msg)
                                },
                                modifier = Modifier.testTag("btn_quick_whatsapp")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "WhatsApp",
                                    tint = Color(0xFF25D366)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, "tel:${order.clientPhone}".toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Llamar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Device details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${order.deviceBrand} ${order.deviceModel}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (order.imeiOrSerial.isNotBlank()) {
                                Text(
                                    text = if (order.imei2.isNotBlank()) "IMEI 1: ${order.imeiOrSerial}" else "IMEI/Serial: ${order.imeiOrSerial}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (order.imei2.isNotBlank()) {
                                Text(
                                    text = "IMEI 2: ${order.imei2}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Problema reportado: ${order.reportedIssue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (order.deviceConditionNotes.isNotBlank()) {
                                Text(
                                    text = "Observaciones físicas: ${order.deviceConditionNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Photo Evidence Section (Multiple Photos)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Evidencia Fotográfica:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (photoList.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${photoList.size} ${if (photoList.size == 1) "foto" else "fotos"}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (photoList.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(photoList) { index, uriStr ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .clickable { fullscreenPhotoIndex = index }
                                ) {
                                    AsyncImage(
                                        model = uriStr,
                                        contentDescription = "Foto ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(bottomStart = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Shortcut card to add more
                            if (!order.isClosed && onUpdateOrderPhotos != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                            .clickable { launchCamera() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "+ Foto",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!order.isClosed && onUpdateOrderPhotos != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { launchCamera() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Cámara", fontSize = 12.sp)
                                }
                                FilledTonalButton(
                                    onClick = { multiplePhotoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Galería", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Empty photo state with add options
                        if (!order.isClosed && onUpdateOrderPhotos != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchCamera() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tomar Foto", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { multiplePhotoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Galería", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Sin evidencia fotográfica registrada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Spare Parts (Repuestos e Insumos) Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REPUESTOS E INSUMOS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showAddPartDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_add_part_to_order")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Repuesto", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (parts.isEmpty()) {
                        Text(
                            text = "No se han agregado repuestos a esta orden de servicio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        parts.forEach { part ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${part.quantity}x ${part.partName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Costo Taller: ${CurrencyFormatHelper.formatCop(part.unitCost, config)} | Cobro Cliente: ${CurrencyFormatHelper.formatCop(part.unitPrice, config)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = CurrencyFormatHelper.formatCop(part.unitPrice * part.quantity, config),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onRemovePart(part) },
                                    modifier = Modifier.testTag("btn_remove_part_${part.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar repuesto",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Totals breakdown
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Presupuesto Inicial:", style = MaterialTheme.typography.bodySmall)
                                Text(text = CurrencyFormatHelper.formatCop(order.budgetEstimated, config), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Mano de Obra:", style = MaterialTheme.typography.bodySmall)
                                Text(text = CurrencyFormatHelper.formatCop(order.laborCost, config), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Subtotal Repuestos:", style = MaterialTheme.typography.bodySmall)
                                Text(text = CurrencyFormatHelper.formatCop(order.partsTotalPrice, config), style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL COBRADO:",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = CurrencyFormatHelper.formatCop(order.finalTotal, config),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Estimated Net profit for technician
                            val estimatedNetProfit = order.finalTotal - order.partsTotalCost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ganancia Neta Estimada:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = CurrencyFormatHelper.formatCop(estimatedNetProfit, config),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF059669)
                                )
                            }

                            if (order.isClosed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Pagado (${order.paymentMethod}):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = CurrencyFormatHelper.formatCop(order.finalAmountPaid, config),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Linked Warranty Orders Section (if any)
            if (warrantyOrders.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF92400E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ÓRDENES DE GARANTÍA VINCULADAS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        warrantyOrders.forEach { wOrder ->
                            Text(
                                text = "• ${wOrder.orderNumber}: ${wOrder.reportedIssue} (Costo: ${CurrencyFormatHelper.formatCop(wOrder.warrantyCost, config)}) - Estado: ${wOrder.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // 5. Actions Grid
            Text(
                text = "ACCIONES DE LA ORDEN",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // PDF Receipt Button
            Button(
                onClick = {
                    val pdfUri = PdfReceiptGenerator.generateReceiptPdf(
                        context = context,
                        order = order,
                        parts = parts,
                        config = config
                    )
                    generatedPdfUri = pdfUri
                    showReceiptGeneratedDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_generate_pdf_receipt"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generar Recibo PDF para WhatsApp",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Deliver & Close Order Button
            if (!order.isClosed) {
                Button(
                    onClick = { showDeliverDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_deliver_order"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Entregar y Cerrar Orden",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
            }

            // Create Warranty Order Button (if delivered or completed)
            if (order.isClosed || order.status == OrderStatus.ENTREGADO.name || order.status == OrderStatus.LISTO_ENTREGA.name) {
                OutlinedButton(
                    onClick = { showWarrantyDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_create_warranty_order"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gestionar Reingreso por Garantía")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Fullscreen Photo Viewer Dialog
    if (fullscreenPhotoIndex != null && fullscreenPhotoIndex!! in photoList.indices) {
        val currentIndex = fullscreenPhotoIndex!!
        Dialog(onDismissRequest = { fullscreenPhotoIndex = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Foto ${currentIndex + 1} de ${photoList.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row {
                            if (!order.isClosed && onUpdateOrderPhotos != null) {
                                IconButton(
                                    onClick = {
                                        val updated = photoList.toMutableList().apply { removeAt(currentIndex) }
                                        onUpdateOrderPhotos(RepairOrder.joinPhotos(updated))
                                        if (updated.isEmpty()) {
                                            fullscreenPhotoIndex = null
                                        } else if (currentIndex >= updated.size) {
                                            fullscreenPhotoIndex = updated.size - 1
                                        }
                                        Toast.makeText(context, "Foto eliminada", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar foto",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { fullscreenPhotoIndex = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Main Photo Display
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photoList[currentIndex],
                            contentDescription = "Foto ampliada ${currentIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Footer Navigation Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) fullscreenPhotoIndex = currentIndex - 1
                            },
                            enabled = currentIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Foto anterior",
                                tint = if (currentIndex > 0) Color.White else Color.Gray
                            )
                        }

                        Text(
                            text = "${currentIndex + 1} / ${photoList.size}",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = {
                                if (currentIndex < photoList.size - 1) fullscreenPhotoIndex = currentIndex + 1
                            },
                            enabled = currentIndex < photoList.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Foto siguiente",
                                tint = if (currentIndex < photoList.size - 1) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Spare Part Dialog
    if (showAddPartDialog) {
        AddPartDialog(
            inventory = inventory,
            config = config,
            onDismiss = { showAddPartDialog = false },
            onAdd = { name, qty, cost, price, invId ->
                onAddPart(name, qty, cost, price, invId)
                showAddPartDialog = false
            }
        )
    }

    // Deliver and Close Order Dialog
    if (showDeliverDialog) {
        DeliverOrderDialog(
            config = config,
            finalTotal = order.finalTotal,
            onDismiss = { showDeliverDialog = false },
            onConfirm = { paymentMethod, amountPaid ->
                onDeliverAndClose(paymentMethod, amountPaid)
                showDeliverDialog = false
            }
        )
    }

    // Create Warranty Order Dialog
    if (showWarrantyDialog) {
        CreateWarrantyDialog(
            parentOrder = order,
            config = config,
            onDismiss = { showWarrantyDialog = false },
            onConfirm = { reportedIssue, cost, notes ->
                onCreateWarrantyOrder(reportedIssue, cost, notes)
                showWarrantyDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("¿Eliminar Orden?") },
            text = { Text("Esta acción eliminará la orden ${order.orderNumber} y sus repuestos asociados. No se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteOrder()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_order")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Receipt Generated Modal
    if (showReceiptGeneratedDialog && generatedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { showReceiptGeneratedDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Recibo PDF Generado") },
            text = {
                Text("El recibo de servicio técnico para la orden ${order.orderNumber} ha sido generado exitosamente. Puede compartirlo por WhatsApp o abrirlo con el visor de PDF.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReceiptGeneratedDialog = false
                        WhatsAppHelper.sharePdfReceipt(context, generatedPdfUri!!, order, config)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.testTag("btn_share_pdf_whatsapp")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar por WhatsApp")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showReceiptGeneratedDialog = false
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(generatedPdfUri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Abrir Recibo PDF"))
                    }
                ) {
                    Text("Ver PDF")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartDialog(
    inventory: List<InventoryPart>,
    config: WorkshopConfig,
    onDismiss: () -> Unit,
    onAdd: (name: String, quantity: Int, unitCost: Double, unitPrice: Double, inventoryPartId: Long?) -> Unit
) {
    var isFromInventory by remember { mutableStateOf(inventory.isNotEmpty()) }
    var selectedInventoryPart by remember { mutableStateOf<InventoryPart?>(inventory.firstOrNull()) }
    var customName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var unitCostStr by remember { mutableStateOf(selectedInventoryPart?.let { if (it.purchaseCost > 0) String.format(Locale.US, "%.0f", it.purchaseCost) else "" } ?: "") }
    var unitPriceStr by remember { mutableStateOf(selectedInventoryPart?.let { if (it.salePrice > 0) String.format(Locale.US, "%.0f", it.salePrice) else "" } ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Agregar Repuesto a la Orden",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (inventory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isFromInventory,
                            onClick = {
                                isFromInventory = true
                                selectedInventoryPart?.let {
                                    unitCostStr = if (it.purchaseCost > 0) String.format(Locale.US, "%.0f", it.purchaseCost) else ""
                                    unitPriceStr = if (it.salePrice > 0) String.format(Locale.US, "%.0f", it.salePrice) else ""
                                }
                            },
                            label = { Text("Del Inventario") }
                        )
                        FilterChip(
                            selected = !isFromInventory,
                            onClick = { isFromInventory = false },
                            label = { Text("Repuesto Manual") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isFromInventory && inventory.isNotEmpty()) {
                    var expandedInv by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedInv,
                        onExpandedChange = { expandedInv = !expandedInv }
                    ) {
                        OutlinedTextField(
                            value = selectedInventoryPart?.let { "${it.name} (Stock: ${it.stockQuantity})" } ?: "Seleccionar Repuesto",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repuesto de Inventario") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInv) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedInv,
                            onDismissRequest = { expandedInv = false }
                        ) {
                            inventory.forEach { inv ->
                                DropdownMenuItem(
                                    text = { Text("${inv.name} • Stock: ${inv.stockQuantity} • ${CurrencyFormatHelper.formatCop(inv.salePrice, config)}") },
                                    onClick = {
                                        selectedInventoryPart = inv
                                        unitCostStr = if (inv.purchaseCost > 0) String.format(Locale.US, "%.0f", inv.purchaseCost) else ""
                                        unitPriceStr = if (inv.salePrice > 0) String.format(Locale.US, "%.0f", inv.salePrice) else ""
                                        expandedInv = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nombre / Descripción del Repuesto *") },
                        placeholder = { Text("Ej: Pantalla OLED iPhone 13") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { input ->
                            quantityStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    val costValCheck = CostValidator.validate(unitCostStr, isRequired = false)
                    OutlinedTextField(
                        value = unitCostStr,
                        onValueChange = { input ->
                            unitCostStr = CostValidator.filterNumericInput(input)
                        },
                        label = { Text("Costo Taller (COP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !costValCheck.isValid,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val priceValCheck = CostValidator.validate(unitPriceStr, isRequired = true)
                OutlinedTextField(
                    value = unitPriceStr,
                    onValueChange = { input ->
                        unitPriceStr = CostValidator.filterNumericInput(input)
                    },
                    label = { Text("Precio al Cliente (COP) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !priceValCheck.isValid,
                    supportingText = {
                        if (!priceValCheck.isValid) {
                            Text(priceValCheck.errorMessage ?: "Precio inválido", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val name = if (isFromInventory) selectedInventoryPart?.name ?: "" else customName.trim()
                            val qty = quantityStr.toIntOrNull() ?: 1
                            val costCheck = CostValidator.validate(unitCostStr, isRequired = false)
                            val priceCheck = CostValidator.validate(unitPriceStr, isRequired = true)
                            val invId = if (isFromInventory) selectedInventoryPart?.id else null

                            if (name.isNotBlank() && costCheck.isValid && priceCheck.isValid) {
                                onAdd(name, qty, costCheck.value, priceCheck.value, invId)
                            }
                        }
                    ) {
                        Text("Agregar")
                    }
                }
            }
        }
    }
}

@Composable
fun DeliverOrderDialog(
    config: WorkshopConfig,
    finalTotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String, amountPaid: Double) -> Unit
) {
    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var amountPaidStr by remember { mutableStateOf(if (finalTotal > 0) String.format(Locale.US, "%.0f", finalTotal) else "0") }

    val methods = listOf("Efectivo", "Transferencia", "Pago Móvil", "Dólares Efectivo", "Punto de Venta / Tarjeta", "Zelle", "Binance")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Entrega de Equipo y Cierre de Orden",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Al entregar, el sistema registrará el pago, actualizará el inventario de repuestos y cerrará la orden de servicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Método de Pago:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    methods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (paymentMethod == method) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable { paymentMethod = method }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (paymentMethod == method) Icons.Default.CheckCircle else Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = if (paymentMethod == method) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (paymentMethod == method) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val amountPaidCheck = CostValidator.validate(amountPaidStr, isRequired = true)
                OutlinedTextField(
                    value = amountPaidStr,
                    onValueChange = { input ->
                        amountPaidStr = CostValidator.filterNumericInput(input)
                    },
                    label = { Text("Monto Cobrado (COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !amountPaidCheck.isValid,
                    supportingText = {
                        if (!amountPaidCheck.isValid) {
                            Text(amountPaidCheck.errorMessage ?: "Monto inválido", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val check = CostValidator.validate(amountPaidStr, isRequired = true)
                            if (check.isValid) {
                                onConfirm(paymentMethod, check.value)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Confirmar Entrega")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWarrantyDialog(
    parentOrder: RepairOrder,
    config: WorkshopConfig,
    onDismiss: () -> Unit,
    onConfirm: (reportedIssue: String, cost: Double, notes: String) -> Unit
) {
    var reportedIssue by remember { mutableStateOf("Falla reincidente o detalle de garantía") }
    var warrantyCostStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("Garantía por servicio de orden ${parentOrder.orderNumber}") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF92400E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nueva Orden de Garantía",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vinculada a la orden: ${parentOrder.orderNumber} (${parentOrder.deviceBrand} ${parentOrder.deviceModel} - ${parentOrder.clientName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reportedIssue,
                    onValueChange = { reportedIssue = it },
                    label = { Text("Problema Reportado en Garantía *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val warrantyCostCheck = CostValidator.validate(warrantyCostStr, isRequired = false)
                OutlinedTextField(
                    value = warrantyCostStr,
                    onValueChange = { input ->
                        warrantyCostStr = CostValidator.filterNumericInput(input)
                    },
                    label = { Text("Costo de Garantía (COP) - Puede ser 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !warrantyCostCheck.isValid,
                    supportingText = {
                        if (!warrantyCostCheck.isValid) {
                            Text(warrantyCostCheck.errorMessage ?: "Costo inválido", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones del Técnico") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val costCheck = CostValidator.validate(warrantyCostStr, isRequired = false)
                            if (reportedIssue.isNotBlank() && costCheck.isValid) {
                                onConfirm(reportedIssue.trim(), costCheck.value, notes.trim())
                            }
                        }
                    ) {
                        Text("Crear Orden de Garantía")
                    }
                }
            }
        }
    }
}

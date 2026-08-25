package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.WorkshopConfig
import com.example.ui.components.BarcodeScannerDialog
import com.example.util.ContactPickerHelper
import com.example.util.CostValidator
import com.example.util.PhoneCatalogHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOrderDialog(
    config: WorkshopConfig,
    onDismiss: () -> Unit,
    onSubmit: (
        clientName: String,
        clientPhone: String,
        deviceBrand: String,
        deviceModel: String,
        imeiOrSerial: String,
        imei2: String,
        reportedIssue: String,
        conditionNotes: String,
        budget: Double,
        photoUri: String?
    ) -> Unit
) {
    val context = LocalContext.current

    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var deviceBrand by remember { mutableStateOf("Samsung") }
    var deviceModel by remember { mutableStateOf("") }

    var imeiOrSerial by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    var reportedIssue by remember { mutableStateOf("") }
    var conditionNotes by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    // Brand catalog list and dialog state
    var brandList by remember { mutableStateOf(PhoneCatalogHelper.getBrands(context)) }
    var expandedBrand by remember { mutableStateOf(false) }
    var showAddBrandDialog by remember { mutableStateOf(false) }
    var newBrandNameInput by remember { mutableStateOf("") }

    // Barcode scanner state
    var activeScannerTarget by remember { mutableStateOf<Int?>(null) } // 1 for IMEI 1, 2 for IMEI 2

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
        }
    }

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contactData = ContactPickerHelper.extractContactDetails(context, uri)
            if (contactData != null) {
                if (contactData.name.isNotBlank()) {
                    clientName = contactData.name
                }
                if (contactData.phoneNumber.isNotBlank()) {
                    clientPhone = contactData.phoneNumber
                }
                Toast.makeText(context, "Contacto importado: ${contactData.name}", Toast.LENGTH_SHORT).show()
                showError = false
            } else {
                Toast.makeText(context, "No se pudo leer los datos del contacto seleccionado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Contact permission launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            // Still try launching picker as many Android versions allow selection without full read permission
            contactPickerLauncher.launch(null)
        }
    }

    // Barcode Scanner Dialog
    if (activeScannerTarget != null) {
        val target = activeScannerTarget!!
        BarcodeScannerDialog(
            title = if (target == 1) "Escanear IMEI 1 / Serial" else "Escanear IMEI 2 (Opcional)",
            onDismiss = { activeScannerTarget = null },
            onBarcodeScanned = { scannedCode ->
                if (target == 1) {
                    imeiOrSerial = scannedCode
                } else {
                    imei2 = scannedCode
                }
                Toast.makeText(context, "Código capturado: $scannedCode", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ingreso de Dispositivo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Registro de orden y recepción técnica",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_new_order_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Client Information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATOS DEL CLIENTE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Import from Contacts Button
                    OutlinedButton(
                        onClick = {
                            val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                            if (perm == PackageManager.PERMISSION_GRANTED) {
                                contactPickerLauncher.launch(null)
                            } else {
                                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_import_contact")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Importar Contacto",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar Contacto", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        showError = false
                    },
                    label = { Text("Nombre y Apellido *") },
                    placeholder = { Text("Ej: Carlos Mendoza") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    isError = showError && clientName.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_client_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = {
                        clientPhone = it
                        showError = false
                    },
                    label = { Text("Teléfono / WhatsApp *") },
                    placeholder = { Text("Ej: +58 414 1234567") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = showError && clientPhone.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_client_phone")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Device Information (Brand & Model)
                Text(
                    text = "DATOS DEL DISPOSITIVO",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Brand Selector with + Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedBrand,
                        onExpandedChange = { expandedBrand = !expandedBrand },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = deviceBrand,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Marca del Dispositivo *") },
                            leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                            isError = showError && deviceBrand.isBlank(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                                .testTag("input_new_device_brand")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBrand,
                            onDismissRequest = { expandedBrand = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "+ Agregar nueva marca",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    expandedBrand = false
                                    newBrandNameInput = ""
                                    showAddBrandDialog = true
                                }
                            )
                            HorizontalDivider()
                            brandList.forEach { brandOption ->
                                DropdownMenuItem(
                                    text = { Text(brandOption) },
                                    onClick = {
                                        if (brandOption == "Otro") {
                                            expandedBrand = false
                                            newBrandNameInput = ""
                                            showAddBrandDialog = true
                                        } else {
                                            deviceBrand = brandOption
                                            expandedBrand = false
                                            showError = false
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalIconButton(
                        onClick = {
                            newBrandNameInput = ""
                            showAddBrandDialog = true
                        },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(48.dp)
                            .testTag("btn_add_brand")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar nueva marca",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Free Text Input
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = {
                        deviceModel = it
                        showError = false
                    },
                    label = { Text("Modelo del Dispositivo *") },
                    placeholder = { Text("Ej: Redmi Note 13 Pro+, iPhone 15 Pro, Galaxy A54...") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    singleLine = true,
                    isError = showError && deviceModel.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_device_model")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // IMEI 1 with Barcode Scanner Button
                OutlinedTextField(
                    value = imeiOrSerial,
                    onValueChange = { imeiOrSerial = it },
                    label = { Text("IMEI 1 / Serial (Opcional)") },
                    placeholder = { Text("Ej: 358941092837461") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { activeScannerTarget = 1 },
                            modifier = Modifier.testTag("btn_scan_imei_1")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear código de barras IMEI 1",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_imei")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // IMEI 2 (Opcional) with Barcode Scanner Button
                OutlinedTextField(
                    value = imei2,
                    onValueChange = { imei2 = it },
                    label = { Text("IMEI 2 (Opcional - Dual SIM)") },
                    placeholder = { Text("Ej: 358941092837462") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { activeScannerTarget = 2 },
                            modifier = Modifier.testTag("btn_scan_imei_2")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear código de barras IMEI 2",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_imei_2")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reportedIssue,
                    onValueChange = {
                        reportedIssue = it
                        showError = false
                    },
                    label = { Text("Problema Reportado *") },
                    placeholder = { Text("Ej: Pantalla partida, no carga, se apaga...") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    minLines = 2,
                    maxLines = 3,
                    isError = showError && reportedIssue.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_reported_issue")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = conditionNotes,
                    onValueChange = { conditionNotes = it },
                    label = { Text("Detalles Visuales / Estado del Equipo") },
                    placeholder = { Text("Ej: Tapa trasera rayada, sin bandeja SIM...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_condition_notes")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Budget & Numeric Cost Validation
                Text(
                    text = "PRESUPUESTO Y EVIDENCIA FOTOGRÁFICA",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val budgetValidation = CostValidator.validate(budgetStr, isRequired = false)

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { input ->
                        budgetStr = CostValidator.filterNumericInput(input)
                    },
                    label = { Text("Presupuesto Estimado (COP)") },
                    placeholder = { Text("0") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = !budgetValidation.isValid,
                    supportingText = {
                        if (!budgetValidation.isValid) {
                            Text(
                                text = budgetValidation.errorMessage ?: "Costo inválido",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_budget")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Photo Evidence Picker
                Text(
                    text = "Foto del Equipo (Opcional - Evidencia de Garantía)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (photoUriString != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = photoUriString,
                            contentDescription = "Foto del equipo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        IconButton(
                            onClick = { photoUriString = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar foto",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_pick_photo"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar Foto de Evidencia")
                    }
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Por favor complete los campos obligatorios (*) y valide que los costos sean números válidos",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        val finalBrand = deviceBrand.trim()
                        val finalModel = deviceModel.trim()
                        val costCheck = CostValidator.validate(budgetStr, isRequired = false)

                        if (clientName.isBlank() || clientPhone.isBlank() || finalBrand.isBlank() || finalModel.isBlank() || reportedIssue.isBlank() || !costCheck.isValid) {
                            showError = true
                        } else {
                            val budgetVal = costCheck.value
                            onSubmit(
                                clientName.trim(),
                                clientPhone.trim(),
                                finalBrand,
                                finalModel,
                                imeiOrSerial.trim(),
                                imei2.trim(),
                                reportedIssue.trim(),
                                conditionNotes.trim(),
                                budgetVal,
                                photoUriString
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_submit_new_order"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Registrar Ingreso de Orden",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }

    // Add New Brand Dialog
    if (showAddBrandDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddBrandDialog = false
                newBrandNameInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar Nueva Marca")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Ingrese el nombre de la marca que desea añadir a la lista:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newBrandNameInput,
                        onValueChange = { newBrandNameInput = it },
                        label = { Text("Nombre de la Marca *") },
                        placeholder = { Text("Ej: Nothing, TCL, Ulefone...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanBrand = newBrandNameInput.trim()
                        if (cleanBrand.isNotBlank()) {
                            PhoneCatalogHelper.saveCustomBrand(context, cleanBrand)
                            brandList = PhoneCatalogHelper.getBrands(context)
                            deviceBrand = cleanBrand
                            showAddBrandDialog = false
                            newBrandNameInput = ""
                            showError = false
                            Toast.makeText(context, "Marca agregada: $cleanBrand", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Marca")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddBrandDialog = false
                        newBrandNameInput = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

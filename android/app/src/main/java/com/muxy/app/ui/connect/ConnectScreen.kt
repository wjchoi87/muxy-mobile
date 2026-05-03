package com.muxy.app.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.muxy.app.BuildConfig
import com.muxy.app.data.Entitlement
import com.muxy.app.data.SavedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(viewModel: ConnectionViewModel, onOpenSettings: () -> Unit = {}) {
    val devices by viewModel.savedDevices.collectAsState()
    val entitlement by viewModel.billing.entitlement.collectAsState()
    val product by viewModel.billing.productDetails.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showTrialInfo by remember { mutableStateOf(false) }
    val priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                title = { Text("Devices") },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add device")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (devices.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { EmptyState(onAdd = { showAdd = true }) }
                    EntitlementFooter(entitlement, priceText, onClick = { showTrialInfo = true })
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(devices, key = { it.id }) { device ->
                            DeviceRow(
                                device = device,
                                onClick = { viewModel.connect(device) },
                                onDelete = { viewModel.removeDevice(device) },
                            )
                            HorizontalDivider()
                        }
                    }
                    EntitlementFooter(entitlement, priceText, onClick = { showTrialInfo = true })
                }
            }
        }
    }

    if (showTrialInfo) {
        TrialInfoSheet(
            entitlement = entitlement,
            price = priceText,
            onDismiss = { showTrialInfo = false },
            onUnlock = {
                (context as? Activity)?.let { viewModel.billing.launchPurchase(it) }
                showTrialInfo = false
            },
            onRestore = {
                viewModel.billing.restore()
                showTrialInfo = false
            },
        )
    }

    if (showAdd) {
        AddDeviceDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, host, port ->
                viewModel.addDevice(name, host, port)
                showAdd = false
            },
        )
    }
}

@Composable
private fun DeviceRow(device: SavedDevice, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Computer, contentDescription = null)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(end = 8.dp)) {
            Text(device.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${device.host}:${device.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
private fun EntitlementFooter(entitlement: Entitlement, price: String?, onClick: () -> Unit) {
    if (!BuildConfig.BILLING_ENABLED) return
    val message = when (entitlement) {
        Entitlement.Beta -> "Purchase the app${price?.let { " for $it" } ?: ""} (optional during testing). Tap for details."
        Entitlement.Loading -> "Free for 3 days after your first connection${price?.let { ", then $it" } ?: ""}. Tap for details."
        is Entitlement.Trial -> {
            val days = (entitlement.msRemaining / (24 * 60 * 60 * 1000L)).toInt() + 1
            "Trial: $days day${if (days == 1) "" else "s"} left${price?.let { ", then $it" } ?: ""}. Tap for details."
        }
        Entitlement.Expired -> "Trial ended${price?.let { " — unlock for $it" } ?: ""}. Tap for details."
        Entitlement.Unlocked -> return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrialInfoSheet(
    entitlement: Entitlement,
    price: String?,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    onRestore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priceLabel = price ?: "—"

    val (title, status) = when (entitlement) {
        Entitlement.Beta -> "Support Muxy" to "Muxy is in beta — connecting is free for now. You can purchase early if you'd like to support development; you'll keep your purchase when the trial-and-pay model goes live."
        Entitlement.Loading -> "Unlock Muxy" to "Pair your Mac to start a 3-day free trial."
        is Entitlement.Trial -> {
            val days = (entitlement.msRemaining / (24 * 60 * 60 * 1000L)).toInt() + 1
            "Trial active" to "$days day${if (days == 1) "" else "s"} left in your free trial."
        }
        Entitlement.Expired -> "Trial ended" to "Unlock to keep connecting to your Mac."
        Entitlement.Unlocked -> "Unlocked" to "You already own Muxy. Thanks!"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider()

            Text("How it works", style = MaterialTheme.typography.titleSmall)
            if (entitlement is Entitlement.Beta) {
                BulletLine("During beta, connecting to your Mac is completely free — no countdown, no limits.")
                BulletLine("When Muxy reaches 1.0, a 3-day free trial will start on first pairing. After that, connecting will require a one-time purchase of $priceLabel.")
                BulletLine("Purchasing now is optional. If you do, you keep access for life — no subscription, no recurring charges.")
                BulletLine("Tied to your Google account — works on all your Android devices.")
                BulletLine("If you reinstall or switch devices, tap \"Restore purchase\" to recover access.")
            } else {
                BulletLine("Free for 3 days starting from your first successful pairing.")
                BulletLine("After the trial ends, connecting to a Mac requires a one-time purchase of $priceLabel.")
                BulletLine("Pay once. No subscription, no recurring charges.")
                BulletLine("Tied to your Google account — works on all your Android devices.")
                BulletLine("If you reinstall or switch devices, tap \"Restore purchase\" to recover access.")
            }

            Spacer(Modifier.height(4.dp))

            if (entitlement !is Entitlement.Unlocked) {
                val ctaLabel = when {
                    price == null -> "Unlock unavailable"
                    entitlement is Entitlement.Beta -> "Purchase early — $priceLabel"
                    else -> "Unlock now — $priceLabel"
                }
                Button(
                    onClick = onUnlock,
                    enabled = price != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(ctaLabel)
                }
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore purchase")
                }
            } else {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Computer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("No Devices", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add your Mac to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) { Text("Add Device") }
    }
}

@Composable
private fun AddDeviceDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("4865") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Device") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("My Mac") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("Host") },
                    placeholder = { Text("10.0.2.2 (emulator) or 192.168.1.10") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Use 10.0.2.2 from the Android emulator to reach the Mac host. Real devices use the LAN/Tailscale IP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, host, port.toIntOrNull() ?: 4865) },
                enabled = host.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

package com.muxy.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.muxy.app.data.BillingRepository
import com.muxy.app.data.Entitlement

@Composable
fun PaywallScreen(billing: BillingRepository, entitlement: Entitlement) {
    val context = LocalContext.current
    val product by billing.productDetails.collectAsState()
    val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "—"

    val title = when (entitlement) {
        is Entitlement.Trial -> "Trial Active"
        Entitlement.Expired -> "Trial Ended"
        else -> "Unlock Muxy"
    }
    val subtitle = when (entitlement) {
        is Entitlement.Trial -> {
            val days = (entitlement.msRemaining / (24 * 60 * 60 * 1000L)).toInt() + 1
            "$days day${if (days == 1) "" else "s"} left in your free trial."
        }
        Entitlement.Expired -> "Your 3-day trial has ended. Unlock Muxy to keep connecting."
        else -> "Pay once to connect to your Mac."
    }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { (context as? Activity)?.let { billing.launchPurchase(it) } }) {
                Text("Unlock — $price")
            }
            TextButton(onClick = { billing.restore() }) {
                Text("Restore purchase")
            }
        }
    }
}

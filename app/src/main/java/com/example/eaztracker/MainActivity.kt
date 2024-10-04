package com.example.eaztracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eaztracker.ui.theme.EazTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EazTrackerTheme {
                val viewModel = viewModel<MainViewModel>()

                /*
                val context = LocalContext.current
                var isCharging by remember { mutableStateOf(false) }
                var chargeCounter by remember { mutableStateOf<Int?>(null) }
                var capacity by remember { mutableStateOf<Int?>(null) }


                // Create the BroadcastReceiver
                val batteryReceiver =
                    rememberUpdatedState(BatteryBroadcastReceiver { charging, _, _ ->
                        isCharging = charging
                        // Update chargeCounter and capacity using BatteryManager
                        val batteryManager =
                            context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

                        chargeCounter =
                            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                        capacity =
                            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    })

                viewModel.checkRegistration(
                    applicationContext,
                    isCharging = isCharging,
                    batteryCapacity = capacity,
                    batteryCycle = chargeCounter
                )

                // Register the receiver when the composable enters the composition
                DisposableEffect(Unit) {
                    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    context.registerReceiver(batteryReceiver.value, filter)

                    // Cleanup when the composable is removed
                    onDispose {
                        context.unregisterReceiver(batteryReceiver.value)
                    }
                }



                Column(Modifier.verticalScroll(rememberScrollState())) {

                    Spacer(modifier = Modifier.height(100.dp))

                    Text(text = "Working!")

                }


                 */
            }
        }
    }
}

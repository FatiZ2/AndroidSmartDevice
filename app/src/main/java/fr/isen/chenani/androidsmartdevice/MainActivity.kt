package fr.isen.chenani.androidsmartdevice

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import fr.isen.chenani.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Enregistre le launcher pour la demande d'activation Bluetooth
        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth activé → lancement de l'activité de scan
                startActivity(Intent(this, ScanActivity::class.java))
            } else {
                Toast.makeText(this, "Bluetooth requis pour scanner", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            AndroidSmartDeviceTheme {
                MainScreen {
                    if (bluetoothAdapter == null) {
                        Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
                    } else if (bluetoothAdapter.isEnabled) {
                        startActivity(Intent(this, ScanActivity::class.java))
                    } else {
                        // Lance l'intent pour activer le Bluetooth
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(enableBtIntent)
                    }
                }
            }
        }
    }
}

val lightBlue = Color(0xFFADD8E6)
@Composable
fun MainScreen(onScanClick: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(lightBlue)
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Icon",
                modifier = Modifier.size(400.dp)
            )

            Text(
                text = "Cette application permet de scanner les appareils BLE autour de vous.",
                fontSize = 16.sp,
                color = Color.Black // Texte en blanc
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onScanClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White) // Fond du bouton blanc
            ) {
                Text("Scanner les appareils BLE", color = Color.Black) //
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidSmartDeviceTheme {
        Greeting("Android")
    }
}

package fr.isen.chenani.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import fr.isen.chenani.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ConnectActivity : ComponentActivity() {
    private lateinit var deviceToConnect: BluetoothDevice
    private var bluetoothGatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Récupérer l'adresse de l'appareil BLE envoyé depuis la page précédente
        val deviceToConnect = intent.getParcelableExtra<BluetoothDevice>("device")
        if (deviceToConnect != null) {
            connectToDevice(deviceToConnect)
        }

        setContent {
            // Appel de la fonction de mise en page
            ConnectPage(deviceToConnect?.name ?: "Inconnu", deviceToConnect?.address ?: "Aucune adresse", ::turnOnLED)
        }
    }

    // Méthode pour tenter la connexion à l'appareil
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        // Connexion à l'appareil BLE
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Toast.makeText(this, "Connexion à l'appareil ${device.name}", Toast.LENGTH_SHORT).show()
    }

    // Méthode pour allumer les LEDs (envoie une commande à la carte STM32)
    @SuppressLint("MissingPermission")
    private fun turnOnLED(ledNumber: Int) {

        val ledCharacteristic: BluetoothGattCharacteristic? = bluetoothGatt?.services?.get(2)?.characteristics?.get(0)

        // Exemple : Allumer l'LED en envoyant un 1 pour allumer et 0 pour éteindre
        val ledValue = when (ledNumber) {
            1 -> 0x01.toByte() // LED 1
            2 -> 0x02.toByte() // LED 2
            3 -> 0x03.toByte() // LED 3
            else -> return
        }

        if (ledCharacteristic != null) {
            // Allumer la LED
            ledCharacteristic.setValue(byteArrayOf(ledValue))
            bluetoothGatt?.writeCharacteristic(ledCharacteristic)
            Toast.makeText(this, "LED $ledNumber allumée", Toast.LENGTH_SHORT).show()
        }
    }

    // BluetoothGattCallback pour gérer les événements de connexion et les services disponibles
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // Appareil connecté avec succès
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Appareil connecté", Toast.LENGTH_SHORT).show()
                }
                // Découverte des services du périphérique
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // Appareil déconnecté
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Déconnexion de l'appareil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {

            } else {
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Échec de la découverte des services", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                val value = characteristic.value
                val clicks = value[0].toInt() // Exemple : Le nombre de clics est stocké dans la première position de la valeur
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Nombre de clics: $clicks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()  // Fermer la connexion BluetoothGatt lorsque l'activité est détruite
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectPage(deviceName: String, deviceAddress: String, turnOnLED: (Int) -> Unit) {
    val context = LocalContext.current
    // Structure de la page de connexion avec un fond bleu clair et un design moderne
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retour") },
                navigationIcon = {
                    IconButton(onClick = { (context as ComponentActivity).finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5B7DB1))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF5B7DB1))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Appareil connecté: $deviceName",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Adresse de l'appareil : $deviceAddress",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(30.dp))

            // Affichage des boutons pour contrôler les LEDs
            Row {
                Button(
                    onClick = { turnOnLED(1) },
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Allumer LED 1", color = Color.White)
                }

                Button(
                    onClick = { turnOnLED(2) },
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Allumer LED 2", color = Color.White)
                }
            }

            Row {
                Button(
                    onClick = { turnOnLED(3) },
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Allumer LED 3", color = Color.White)
                }

                Button(
                    onClick = {  },
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Lire les clics", color = Color.White)
                }
            }
        }
    }
}


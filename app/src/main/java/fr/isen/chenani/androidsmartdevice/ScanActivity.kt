package fr.isen.chenani.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import fr.isen.chenani.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scanTimeout = 10000L // 10 secondes
    private var isScanning by mutableStateOf(false)
    private lateinit var scanResults: MutableList<ScanResult>

    // ✅ Permission launcher bien placé
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Permissions BLE requises", Toast.LENGTH_LONG).show()
            finish()
        } else {
            // ✅ Si tout est accordé, on continue l'initialisation
            initBluetoothAndUI()
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            arrayOf(
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredPermissions = getRequiredPermissions()

        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            permissionLauncher.launch(requiredPermissions)
            return
        }

        initBluetoothAndUI()
    }

    private fun initBluetoothAndUI() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth désactivé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        launchScanUI()
    }

    private fun launchScanUI() {
        setContent {
            scanResults = remember { mutableStateListOf() }
            AndroidSmartDeviceTheme {
                ScanScreen(
                    devices = scanResults,
                    isScanning = isScanning,
                    onToggleScan = {
                        if (isScanning) stopBleScan() else startBleScan()
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (isScanning) return

        scanResults.clear()
        isScanning = true
        bluetoothLeScanner?.startScan(scanCallback)

        handler.postDelayed({
            stopBleScan()
        }, scanTimeout)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        if (!isScanning) return

        isScanning = false
        bluetoothLeScanner?.stopScan(scanCallback)
        scanResults.clear() // Vide la liste des résultats de scan
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexOfResult = scanResults.indexOfFirst { it.device.address == result.device.address}
            if (indexOfResult == -1) {
                scanResults.add(result)
            } else {
                scanResults[indexOfResult] = result
            }
        }
    }

    override fun onDestroy() {
        stopBleScan()
        super.onDestroy()
    }
}


@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    devices: List<ScanResult>,
    isScanning: Boolean,
    onToggleScan: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retour") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(lightBlue)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan des appareils BLE",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = if (isScanning) R.drawable.stoppp else R.drawable.scan),
                contentDescription = "Scan Icon",
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onToggleScan() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isScanning) "Appuyer pour arrêter" else "Appuyer pour scanner",
                style = MaterialTheme.typography.bodyLarge
            )


            Spacer(modifier = Modifier.height(24.dp))

            val context = LocalContext.current
            LazyColumn {
                items(devices) { device ->
                    // Affichage de chaque appareil
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                // Créer l'intent pour rediriger vers ConnectActivity
                                val intent = Intent(context, ConnectActivity::class.java)
                                intent.putExtra("device", device.device) // Passer l'adresse de l'appareil
                                context.startActivity(intent) // Lancer l'activité
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = device.device.name ?: "Appareil Inconnu",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                            Text(
                                text = "Adresse : ${device.device.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

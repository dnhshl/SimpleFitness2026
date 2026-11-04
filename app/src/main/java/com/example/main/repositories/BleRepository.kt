package com.example.main.repositories

import com.example.main.statemgmt.DiscoveredDevice
import com.example.main.statemgmt.Esp32Command
import com.example.main.statemgmt.Esp32Data
import com.example.main.statemgmt.FitnessData
import com.juul.kable.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * LEARNING POINT: BleRepository (Sensor & Hardware Integration)
 * 
 * Das BleRepository kapselt die Bluetooth Low Energy (BLE) Kommunikation mit dem ESP32.
 * Es übernimmt das Scannen, Verbinden, Senden von Kommandos (LED) sowie das Parsen und Mappen 
 * der rohen ESP32-Poti-Werte in unser einheitliches Domain-Modell (`FitnessData`).
 */
@OptIn(ExperimentalUuidApi::class)
class BleRepository {

    private val serviceUuid = Uuid.parse("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUuid = Uuid.parse("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val bleCharacteristic = characteristicOf(serviceUuid, characteristicUuid)

    // 1. Setup the Scanner
    private val scanner = Scanner {
        filters {
            match {
                services = listOf(serviceUuid)
            }
        }
    }

    /**
     * A Flow that emits discovered BLE devices during a scan.
     */
    val advertisements: Flow<DiscoveredDevice> = scanner.advertisements
        .map { adv -> DiscoveredDevice(adv.name ?: "Unknown Device", adv.address) }

    private var activePeripheral: Peripheral? = null

    /**
     * Connects to a device and returns a Flow of mapped FitnessData from BLE notifications.
     */
    suspend fun connect(address: String): Flow<FitnessData> {
        val p = Peripheral(address) {
            onServicesDiscovered {
                requestMtu(512)
            }
        }
        activePeripheral = p
        
        p.connect()
        
        // Observe characteristic, parse JSON, and map hardware potentiometer (0..4095) to FitnessData
        return p.observe(bleCharacteristic)
            .map { bytes -> String(bytes) }
            .map { rawJson ->
                try {
                    val espData = Json { ignoreUnknownKeys = true }.decodeFromString<Esp32Data>(rawJson)
                    val rawPoti = espData.potiArray.lastOrNull() ?: 2048
                    
                    // Map potentiometer (0..4095) to Heart Rate (60..180 BPM)
                    val bpm = 60 + ((rawPoti * 120) / 4095).coerceIn(0, 120)
                    val fitness = (rawPoti.toDouble() / 4095.0) * 100.0
                    val isotimestamp = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)

                    FitnessData(fitness = fitness, isotimestamp = isotimestamp, puls = bpm)
                } catch (e: Exception) {
                    FitnessData(
                        fitness = 50.0,
                        isotimestamp = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME),
                        puls = 75
                    )
                }
            }
    }

    /**
     * Disconnects from the current device.
     */
    suspend fun disconnect() {
        activePeripheral?.disconnect()
        activePeripheral = null
    }

    /**
     * Sends a command (LED status) to the ESP32.
     */
    suspend fun sendCommand(command: Esp32Command) {
        val jsonString = Json.encodeToString(command)
        activePeripheral?.write(
            bleCharacteristic,
            jsonString.toByteArray(),
            WriteType.WithResponse
        )
    }
}

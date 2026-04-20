package com.example.main.statemgmt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Example of a custom Serializer for a type that is not serializable by default.
 * This tells Kotlinx.Serialization how to convert a Color to a Long and back.
 */
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeLong(value.toArgb().toLong())
    override fun deserialize(decoder: Decoder): Color = Color(decoder.decodeLong())
}

/**
 * HELPER AREA: Time and Date Utilities
 * 
 * Working with timestamps from APIs can be tricky because they often come in 
 * ISO 8601 format (e.g., "2026-03-12T09:27:04.482897+00:00").
 * 
 * You can use these functions to make the data more "human-readable".
 */
object TimeHelper {

    /**
     * Converts an ISO 8601 string into a friendly format like "12. March 2026, 09:27".
     * Adjusts the time to the device's local time zone.
     * 
     * @param isoString The raw string from the API (e.g. data.isotimestamp)
     * @return A formatted string for your UI
     */
    fun formatIsoTimestamp(isoString: String): String {
        return try {
            // 1. Parse the string and convert to the system's local time zone
            val zonedDateTime = ZonedDateTime.parse(isoString)
                .withZoneSameInstant(ZoneId.systemDefault())
            
            // 2. Define how you want the date to look
            val formatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy, HH:mm", Locale.getDefault())
            
            // 3. Return the formatted string
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    /**
     * Extracts just the time from the timestamp, adjusted to local time.
     * Useful if you only want to show "09:27:04" in a small UI element.
     */
    fun formatToTimeOnly(isoString: String): String {
        return try {
            val zonedDateTime = ZonedDateTime.parse(isoString)
                .withZoneSameInstant(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            "--:--:--"
        }
    }
}

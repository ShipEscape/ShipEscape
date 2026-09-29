package com.shipescape.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object MapSerializer : Serializer<Map<String, String>> {
    override val defaultValue = emptyMap<String, String>()

    override suspend fun readFrom(input: InputStream): Map<String, String> =
        runCatching { Json.decodeFromString<Map<String, String>>(input.readBytes().decodeToString()) }
            .getOrDefault(defaultValue)

    override suspend fun writeTo(t: Map<String, String>, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(t).toByteArray())
        }
}
object MapDoubleSerializer : Serializer<Map<String, Double>> {
    override val defaultValue = emptyMap<String, Double>()

    override suspend fun readFrom(input: InputStream): Map<String, Double> =
        runCatching { Json.decodeFromString<Map<String, Double>>(input.readBytes().decodeToString()) }
            .getOrDefault(defaultValue)

    override suspend fun writeTo(t: Map<String, Double>, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(t).toByteArray())
        }
}

object ServerUrlSerializer: Serializer<String>{
    override val defaultValue = ""

    override suspend fun readFrom(input: InputStream): String =
        runCatching {
            val text = input.readBytes().decodeToString()
            text.ifBlank { defaultValue }
        }.getOrDefault(defaultValue)

    override suspend fun writeTo(t: String, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(t.toByteArray())
        }
}

val Context.beaconStore: DataStore<Map<String, String>> by dataStore("beacons.json", MapSerializer)
val Context.beaconPositionStore: DataStore<Map<String, String>> by dataStore("beaconPositions.json", MapSerializer)
val Context.exitPositionStore: DataStore<Map<String, String>> by dataStore("exits.json", MapSerializer)
val Context.beaconTxPowerStore: DataStore<Map<String, Double>> by dataStore("beaconTxPowers.json",
    MapDoubleSerializer)
val Context.serverUrlStore: DataStore<String> by dataStore("serverUrl.txt", ServerUrlSerializer)
package com.muxy.app.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val MuxyJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    classDiscriminator = "__internal_discriminator_unused__"
}

@Serializable
data class MuxyError(val code: Int, val message: String)

@Serializable
data class TaggedValue(val type: String, val value: JsonElement? = null)

@Serializable
data class MuxyRequest(
    val id: String,
    val method: String,
    val params: TaggedValue? = null,
)

@Serializable
data class MuxyResponse(
    val id: String,
    val result: TaggedValue? = null,
    val error: MuxyError? = null,
)

@Serializable
data class MuxyEvent(
    val event: String,
    val data: TaggedValue? = null,
)

sealed class MuxyMessage {
    data class Request(val payload: MuxyRequest) : MuxyMessage()
    data class Response(val payload: MuxyResponse) : MuxyMessage()
    data class Event(val payload: MuxyEvent) : MuxyMessage()
}

object MuxyMessageSerializer : KSerializer<MuxyMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MuxyMessage") {
        element<String>("type")
        element<JsonElement>("payload")
    }

    override fun serialize(encoder: Encoder, value: MuxyMessage) {
        val (type, payloadJson) = when (value) {
            is MuxyMessage.Request -> "request" to MuxyJson.encodeToJsonElement(MuxyRequest.serializer(), value.payload)
            is MuxyMessage.Response -> "response" to MuxyJson.encodeToJsonElement(MuxyResponse.serializer(), value.payload)
            is MuxyMessage.Event -> "event" to MuxyJson.encodeToJsonElement(MuxyEvent.serializer(), value.payload)
        }
        val obj = buildJsonObject {
            put("type", JsonPrimitive(type))
            put("payload", payloadJson)
        }
        encoder.encodeSerializableValue(JsonElement.serializer(), obj)
    }

    override fun deserialize(decoder: Decoder): MuxyMessage {
        val element = decoder.decodeSerializableValue(JsonElement.serializer())
        val obj = element.jsonObject
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
            ?: error("MuxyMessage missing 'type'")
        val payload = obj["payload"] ?: error("MuxyMessage missing 'payload'")
        return when (type) {
            "request" -> MuxyMessage.Request(MuxyJson.decodeFromJsonElement(MuxyRequest.serializer(), payload))
            "response" -> MuxyMessage.Response(MuxyJson.decodeFromJsonElement(MuxyResponse.serializer(), payload))
            "event" -> MuxyMessage.Event(MuxyJson.decodeFromJsonElement(MuxyEvent.serializer(), payload))
            else -> error("Unknown MuxyMessage type: $type")
        }
    }
}

fun encodeMessage(message: MuxyMessage): String =
    MuxyJson.encodeToString(MuxyMessageSerializer, message)

fun decodeMessage(text: String): MuxyMessage =
    MuxyJson.decodeFromString(MuxyMessageSerializer, text)

@Serializable
data class AuthenticateDeviceParams(
    val deviceID: String,
    val deviceName: String,
    val token: String,
)

@Serializable
data class PairDeviceParams(
    val deviceID: String,
    val deviceName: String,
    val token: String,
)

@Serializable
data class PairingResult(
    val clientID: String,
    val deviceName: String,
    val themeFg: Long? = null,
    val themeBg: Long? = null,
    val themePalette: List<Long>? = null,
)

fun authenticateDeviceRequest(id: String, params: AuthenticateDeviceParams): MuxyMessage.Request =
    MuxyMessage.Request(
        MuxyRequest(
            id = id,
            method = "authenticateDevice",
            params = TaggedValue(
                type = "authenticateDevice",
                value = MuxyJson.encodeToJsonElement(AuthenticateDeviceParams.serializer(), params),
            ),
        )
    )

fun pairDeviceRequest(id: String, params: PairDeviceParams): MuxyMessage.Request =
    MuxyMessage.Request(
        MuxyRequest(
            id = id,
            method = "pairDevice",
            params = TaggedValue(
                type = "pairDevice",
                value = MuxyJson.encodeToJsonElement(PairDeviceParams.serializer(), params),
            ),
        )
    )

fun decodePairingResult(result: TaggedValue?): PairingResult? {
    if (result == null || result.type != "pairing" || result.value == null) return null
    return MuxyJson.decodeFromJsonElement(PairingResult.serializer(), result.value)
}

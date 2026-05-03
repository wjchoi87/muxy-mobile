package com.muxy.app

import com.muxy.app.model.AuthenticateDeviceParams
import com.muxy.app.model.MuxyError
import com.muxy.app.model.MuxyEvent
import com.muxy.app.model.MuxyJson
import com.muxy.app.model.MuxyMessage
import com.muxy.app.model.MuxyResponse
import com.muxy.app.model.PairingResult
import com.muxy.app.model.TaggedValue
import com.muxy.app.model.authenticateDeviceRequest
import com.muxy.app.model.decodeMessage
import com.muxy.app.model.decodePairingResult
import com.muxy.app.model.encodeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @Test
    fun encodesAuthenticateDeviceRequestWithExpectedShape() {
        val request = authenticateDeviceRequest(
            id = "req-1",
            params = AuthenticateDeviceParams(
                deviceID = "00000000-0000-0000-0000-000000000001",
                deviceName = "Pixel",
                token = "abc",
            ),
        )
        val text = encodeMessage(request)
        val tree = json.parseToJsonElement(text).jsonObject

        assertEquals("request", tree["type"]!!.jsonPrimitive.content)
        val payload = tree["payload"]!!.jsonObject
        assertEquals("req-1", payload["id"]!!.jsonPrimitive.content)
        assertEquals("authenticateDevice", payload["method"]!!.jsonPrimitive.content)

        val params = payload["params"]!!.jsonObject
        assertEquals("authenticateDevice", params["type"]!!.jsonPrimitive.content)
        val value = params["value"]!!.jsonObject
        assertEquals("00000000-0000-0000-0000-000000000001", value["deviceID"]!!.jsonPrimitive.content)
        assertEquals("Pixel", value["deviceName"]!!.jsonPrimitive.content)
        assertEquals("abc", value["token"]!!.jsonPrimitive.content)
    }

    @Test
    fun decodesPairingSuccessResponse() {
        val raw = """
            {
              "type": "response",
              "payload": {
                "id": "req-1",
                "result": {
                  "type": "pairing",
                  "value": {
                    "clientID": "11111111-1111-1111-1111-111111111111",
                    "deviceName": "Saeed's Mac",
                    "themeFg": 16777215,
                    "themeBg": 0,
                    "themePalette": [0, 16711680, 65280]
                  }
                }
              }
            }
        """.trimIndent()
        val message = decodeMessage(raw)
        assertTrue(message is MuxyMessage.Response)
        val response = (message as MuxyMessage.Response).payload
        assertEquals("req-1", response.id)
        assertNull(response.error)
        val pairing = decodePairingResult(response.result)
        assertNotNull(pairing)
        assertEquals("11111111-1111-1111-1111-111111111111", pairing!!.clientID)
        assertEquals("Saeed's Mac", pairing.deviceName)
        assertEquals(16777215L, pairing.themeFg)
        assertEquals(0L, pairing.themeBg)
        assertEquals(listOf(0L, 16711680L, 65280L), pairing.themePalette)
    }

    @Test
    fun decodesAuthFailureResponse() {
        val raw = """
            {
              "type": "response",
              "payload": {
                "id": "req-2",
                "error": { "code": 401, "message": "Authentication required" }
              }
            }
        """.trimIndent()
        val message = decodeMessage(raw) as MuxyMessage.Response
        assertEquals(401, message.payload.error?.code)
        assertEquals("Authentication required", message.payload.error?.message)
        assertNull(message.payload.result)
    }

    @Test
    fun decodesEventEnvelopeAndPreservesUnknownData() {
        val raw = """
            {
              "type": "event",
              "payload": {
                "event": "themeChanged",
                "data": { "type": "deviceTheme", "value": { "fg": 1, "bg": 2 } }
              }
            }
        """.trimIndent()
        val message = decodeMessage(raw) as MuxyMessage.Event
        assertEquals("themeChanged", message.payload.event)
        assertEquals("deviceTheme", message.payload.data?.type)
        assertNotNull(message.payload.data?.value)
    }

    @Test
    fun roundTripsResponseWithUnknownResultType() {

        val raw = """
            {
              "type": "response",
              "payload": {
                "id": "x",
                "result": { "type": "futureThing", "value": { "anything": 42 } }
              }
            }
        """.trimIndent()
        val message = decodeMessage(raw) as MuxyMessage.Response
        assertEquals("futureThing", message.payload.result?.type)
        assertNull(decodePairingResult(message.payload.result))
    }
}

package com.muxy.app.model

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class PaneOwner {
    abstract val displayName: String

    data class Mac(override val displayName: String) : PaneOwner()
    data class Remote(val deviceID: String, override val displayName: String) : PaneOwner()

    companion object {
        fun fromJson(element: JsonElement): PaneOwner? {
            val obj = (element as? JsonElement)?.jsonObject ?: return null
            obj["mac"]?.jsonObject?.let { mac ->
                val name = mac["deviceName"]?.jsonPrimitive?.contentOrNull ?: "Mac"
                return Mac(name)
            }
            obj["remote"]?.jsonObject?.let { remote ->
                val id = remote["deviceID"]?.jsonPrimitive?.contentOrNull ?: return null
                val name = remote["deviceName"]?.jsonPrimitive?.contentOrNull ?: "Remote"
                return Remote(id, name)
            }
            return null
        }
    }
}

fun decodeEventPaneOwnership(data: TaggedValue?): PaneOwnershipEventDTO? {
    if (data?.type != "paneOwnership" || data.value == null) return null
    return MuxyJson.decodeFromJsonElement(PaneOwnershipEventDTO.serializer(), data.value)
}

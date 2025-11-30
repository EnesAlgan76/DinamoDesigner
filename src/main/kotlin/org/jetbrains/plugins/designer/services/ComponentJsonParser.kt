package org.jetbrains.plugins.designer.services

import com.google.gson.JsonParser
import org.jetbrains.plugins.designer.components.PropertyDescriptor
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.template.designer.components.ComponentRegistry

object ComponentJsonParser {

    fun parseAndAddComponents(
        jsonString: String,
        screen: Screen,
        componentManager: org.jetbrains.plugins.designer.models.ComponentManager
    ): Int {
        try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val componentsArray = jsonObject.getAsJsonArray("components")
                ?: throw IllegalArgumentException("Missing 'components' array in JSON")

            var addedCount = 0

            for (element in componentsArray) {
                val componentObj = element.asJsonObject
                val type = componentObj.get("type")?.asString
                    ?: throw IllegalArgumentException("Component missing 'type' field")

                val propertiesJson = componentObj.getAsJsonObject("properties")
                    ?: throw IllegalArgumentException("Component missing 'properties' field")

                val componentDef = ComponentRegistry.getAllComponents().find { it.type == type }
                    ?: throw IllegalArgumentException("Unknown component type: $type")

                val properties = mutableMapOf<String, Any>()

                componentDef.propertyDescriptors.forEach { descriptor ->

                    properties[descriptor.key] = descriptor.default
                }

                propertiesJson.keySet().forEach { key ->
                    val value = propertiesJson.get(key)
                    val actualValue = when {
                        value.isJsonPrimitive -> {
                            val primitive = value.asJsonPrimitive
                            when {
                                primitive.isBoolean -> primitive.asBoolean
                                primitive.isNumber -> primitive.asInt
                                primitive.isString -> primitive.asString
                                else -> primitive.asString
                            }
                        }
                        value.isJsonObject -> {
                            val nestedMap = mutableMapOf<String, Any>()
                            value.asJsonObject.keySet().forEach { nestedKey ->
                                val nestedValue = value.asJsonObject.get(nestedKey)
                                nestedMap[nestedKey] = when {
                                    nestedValue.isJsonPrimitive -> {
                                        val primitive = nestedValue.asJsonPrimitive
                                        when {
                                            primitive.isBoolean -> primitive.asBoolean
                                            primitive.isNumber -> primitive.asInt
                                            primitive.isString -> primitive.asString
                                            else -> primitive.asString
                                        }
                                    }
                                    else -> nestedValue.toString()
                                }
                            }
                            nestedMap
                        }
                        else -> value.toString()
                    }
                    properties[key] = actualValue
                }

                componentManager.addComponentToScreen(screen, type, properties)
                addedCount++
            }

            return addedCount

        } catch (e: Exception) {
            throw RuntimeException("Failed to parse component JSON: ${e.message}", e)
        }
    }

    fun validateJson(jsonString: String): Boolean {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            jsonObject.has("components") && jsonObject.getAsJsonArray("components") != null
        } catch (e: Exception) {
            false
        }
    }
}

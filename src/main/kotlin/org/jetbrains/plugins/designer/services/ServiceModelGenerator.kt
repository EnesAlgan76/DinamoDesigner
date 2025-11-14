package org.jetbrains.plugins.designer.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.File
import kotlin.collections.associate

data class ServiceGenerationResult(
    val filesCreated: Int,
    val createdFiles: List<String>
)

class ServiceModelGenerator(private val project: Project) {

    private val basePath = "/Users/enesalgan/Projeler/TurkiyeFinans.MW/src/com/pozitron/turkiyefinans"
    private val nestedClasses = mutableMapOf<String, Map<String, String>>()

    fun generateService(
        serviceName: String,
        requestJson: String?,
        responseJson: String
    ): ServiceGenerationResult {
        val createdFiles = mutableListOf<String>()
        nestedClasses.clear()

        if (requestJson != null && !requestJson.isBlank()) {
            val requestFile = generateModel("${serviceName}Request", parseJson(requestJson), "requestmodels")
            createdFiles.add(requestFile)
        }

        val responseStructure = parseJson(responseJson)

        val resultKey = responseStructure.keys.firstOrNull { it.endsWith("Result") }
        if (resultKey != null) {
            val resultValue = responseStructure[resultKey]
            if (resultValue is JsonObject) {
                val resultStructure = parseJson(resultValue.toString())
                val resultFile = generateModel("${serviceName}Result", resultStructure, "resultmodels")
                createdFiles.add(resultFile)
            }
        }

        val responseFile = generateResponseModel(serviceName, resultKey)
        createdFiles.add(responseFile)

        nestedClasses.forEach { (className, fields) ->
            val nestedFile = generateModel(className, fields, "models")
            createdFiles.add(nestedFile)
        }

        addToServiceEndpoint(serviceName)
        addToRestClient(serviceName, requestJson != null)
        reformatCreatedFiles(createdFiles)

        return ServiceGenerationResult(
            filesCreated = createdFiles.size,
            createdFiles = createdFiles
        )
    }

    private fun reformatCreatedFiles(filePaths: List<String>) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                filePaths.forEach { filePath ->
                    try {
                        val file = File(filePath)
                        if (file.exists()) {
                            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
                            virtualFile?.let { vFile ->
                                val psiFile = PsiManager.getInstance(project).findFile(vFile)
                                psiFile?.let { psi ->
                                    CodeStyleManager.getInstance(project).reformat(psi)
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    private fun parseJson(json: String): Map<String, Any> {
        val jsonObject = JsonParser.parseString(json).asJsonObject
        return jsonObject.entrySet().associate { (key, value) ->
            key to when {
                value.isJsonPrimitive -> inferJavaType(value)
                value.isJsonArray -> {
                    val array = value.asJsonArray
                    if (array.size() > 0 && array[0].isJsonObject) {
                        val nestedClassName = key.replaceFirstChar { it.titlecase() }.removeSuffix("s")
                        nestedClasses[nestedClassName] = parseJson(array[0].toString()) as Map<String, String>
                        "$nestedClassName[]"
                    } else if (array.size() > 0) {
                        "${inferJavaType(array[0])}[]"
                    } else {
                        "String[]"
                    }
                }
                value.isJsonObject -> {
                    val nestedClassName = key.replaceFirstChar { it.titlecase() }
                    nestedClasses[nestedClassName] = parseJson(value.toString()) as Map<String, String>
                    nestedClassName
                }
                else -> "String"
            }
        }
    }

    private fun generateModel(className: String, structure: Map<String, Any>, subPackage: String): String {
        val fields = structure.map { (key, type) ->
            val javaType = type.toString()
            "    @JsonProperty(\"$key\")\n    private $javaType $key;"
        }.joinToString("\n\n")

        val gettersSetters = structure.map { (key, type) ->
            val javaType = type.toString()
            val capitalizedKey = key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            """
    public $javaType get$capitalizedKey() {
        return $key;
    }

    public void set$capitalizedKey($javaType $key) {
        this.$key = $key;
    }
            """.trimIndent()
        }.joinToString("\n\n")

        val packagePath = if (subPackage == "models") "models.rest" else "models.rest.$subPackage"
        val content = """
package com.pozitron.turkiyefinans.$packagePath;

import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

$fields

$gettersSetters
}
        """.trimIndent()

        val filePath = "$basePath/$packagePath/${className}.java".replace(".", "/")
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun generateResponseModel(serviceName: String, resultKey: String?): String {
        val resultFieldName = resultKey ?: "${serviceName}Result"
        val resultType = "${serviceName}Result"

        val content = """
package com.pozitron.turkiyefinans.models.rest.responsemodels;

import com.pozitron.turkiyefinans.models.rest.resultmodels.$resultType;
import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class ${serviceName}Response implements Serializable {

    @JsonProperty("$resultFieldName")
    private $resultType ${resultType.replaceFirstChar { it.lowercase() }};

    public $resultType get${resultFieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}() {
        return ${resultType.replaceFirstChar { it.lowercase() }};
    }

    public void set${resultFieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}($resultType ${resultType.replaceFirstChar { it.lowercase() }}) {
        this.${resultType.replaceFirstChar { it.lowercase() }} = ${resultType.replaceFirstChar { it.lowercase() }};
    }
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/responsemodels/${serviceName}Response.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun addToServiceEndpoint(serviceName: String) {
        val enumFile = File("$basePath/rest/ServiceEndpoint.java")
        if (!enumFile.exists()) return

        val content = enumFile.readText()

        if (content.contains("$serviceName(\"$serviceName\")")) {
            return
        }

        val lastCommaIndex = content.lastIndexOf(");")
        if (lastCommaIndex > 0) {
            val newEntry = ",\n    $serviceName(\"$serviceName\")"
            val updatedContent = content.substring(0, lastCommaIndex) + newEntry + content.substring(lastCommaIndex)
            enumFile.writeText(updatedContent)
        }
    }

    private fun addToRestClient(serviceName: String, hasRequest: Boolean) {
        val clientFile = File("$basePath/rest/AdcIntegrationRestClient.java")
        if (!clientFile.exists()) return

        val methodName = serviceName.replaceFirstChar { it.lowercase() }
        val requestParam = if (hasRequest) "${serviceName}Request request" else ""
        val requestArg = if (hasRequest) "request" else "null"

        val methodCode = """
    public ${serviceName}Result $methodName($requestParam) throws RestClientException, MiddlewareException {
        ${serviceName}Response response = call(ServiceEndpoint.$serviceName, getRestServicePath(), $requestArg, ${serviceName}Response.class);
        return response.get${serviceName}Result();
    }
        """.trimIndent()

        val content = clientFile.readText()
        val lastBraceIndex = content.lastIndexOf("}")
        if (lastBraceIndex > 0) {
            val updatedContent = content.substring(0, lastBraceIndex) + "\n$methodCode\n" + content.substring(lastBraceIndex)
            clientFile.writeText(updatedContent)
        }
    }

    private fun inferJavaType(value: com.google.gson.JsonElement): String {
        return when {
            value.isJsonPrimitive -> {
                when {
                    value.asJsonPrimitive.isString -> "String"
                    value.asJsonPrimitive.isNumber -> {
                        val numStr = value.asString
                        if (numStr.contains(".")) "double" else "int"
                    }
                    value.asJsonPrimitive.isBoolean -> "boolean"
                    else -> "String"
                }
            }
            else -> "String"
        }
    }
}

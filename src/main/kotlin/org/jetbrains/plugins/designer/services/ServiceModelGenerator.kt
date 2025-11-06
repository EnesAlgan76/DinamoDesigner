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

data class ServiceGenerationResult(
    val filesCreated: Int,
    val createdFiles: List<String>
)

class ServiceModelGenerator(private val project: Project) {

    private val geminiService = GeminiService(project)
    private val basePath = "/Users/enesalgan/Projeler/TurkiyeFinans.MW/src/com/pozitron/turkiyefinans"

    fun generateService(
        serviceName: String,
        requestJson: String?,
        responseJson: String,
        returnType: String?,
        useGemini: Boolean = true
    ): ServiceGenerationResult {
        val createdFiles = mutableListOf<String>()

        nestedClasses.clear()

        val requestStructure = requestJson?.let { parseJson(it) }
        val responseStructure = parseJson(responseJson)

        val pattern = determineServicePattern(responseStructure, returnType, requestJson != null)

        val resultDataStructure = extractResultDataStructure(responseStructure, pattern)

        if (requestJson != null && !requestJson.isBlank()) {
            val requestClassName = "${serviceName}Request"
            val requestFile = generateRequestModel(requestClassName, requestStructure!!)
            createdFiles.add(requestFile)
        }

        var resultUXClassName: String? = null
        if (resultDataStructure != null && pattern.dataFieldName != null) {
            resultUXClassName = "${serviceName}ResultUX"
            val uxFile = generateUXModelFromFieldInfo(resultUXClassName, resultDataStructure)
            createdFiles.add(uxFile)
        }

        nestedClasses.forEach { (className, fields) ->
            val uxFile = generateUXModelFromFieldInfo(className, fields)
            createdFiles.add(uxFile)
        }

        if (pattern.hasResultModel) {
            val resultClassName = "${serviceName}Result"
            val resultFile = generateResultModel(resultClassName, responseStructure, pattern, resultUXClassName)
            createdFiles.add(resultFile)
        }

        val responseClassName = "${serviceName}Response"
        val responseFile = generateResponseModel(responseClassName, serviceName, responseStructure, pattern)
        createdFiles.add(responseFile)

        addToServiceEndpoint(serviceName)

        addToRestClient(serviceName, pattern, requestJson != null, resultUXClassName)

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

    private fun parseWithGemini(json: String): Map<String, Any> {
        val prompt = """
Analyze this JSON and extract field information:

$json

Return a JSON object with this structure:
{
  "fields": [
    {"name": "fieldName", "type": "String|Integer|Double|Boolean|Array|Object", "javaType": "String|int|double|boolean|List|Object"}
  ]
}
        """.trimIndent()

        val result = geminiService.generateScreenComponents(prompt, null)
        return if (result != null) {
            parseJson(result)
        } else {
            parseJson(json)
        }
    }

    private fun parseJson(json: String): Map<String, Any> {
        val jsonObject = JsonParser.parseString(json).asJsonObject
        return jsonObject.entrySet().associate { (key, value) ->
            key to when {
                value.isJsonPrimitive -> {
                    when {
                        value.asJsonPrimitive.isString -> "String"
                        value.asJsonPrimitive.isNumber -> {
                            if (value.asString.contains(".")) "Double" else "Integer"
                        }
                        value.asJsonPrimitive.isBoolean -> "Boolean"
                        else -> "String"
                    }
                }
                value.isJsonArray -> "Array"
                value.isJsonObject -> value.asJsonObject
                else -> "Object"
            }
        }
    }

    private data class ServicePattern(
        val type: String, // "RESULT_RETURN", "DATA_RETURN", "VOID"
        val hasResultModel: Boolean,
        val resultFieldName: String?,
        val dataFieldName: String?,
        val wrapperClass: String?
    )

    private fun determineServicePattern(
        responseStructure: Map<String, Any>,
        returnType: String?,
        hasRequest: Boolean
    ): ServicePattern {
        if (returnType?.lowercase() == "void") {
            return ServicePattern(
                type = "VOID",
                hasResultModel = false,
                resultFieldName = null,
                dataFieldName = null,
                wrapperClass = "MobileMethodResultUX"
            )
        }

        val resultKey = responseStructure.keys.firstOrNull { it.endsWith("Result") }

        if (resultKey != null) {
            val resultValue = responseStructure[resultKey]
            if (resultValue is JsonObject) {
                val resultFields = resultValue.asJsonObject.entrySet().map { it.key }

                val hasResultField = resultFields.contains("Result")

                return if (hasResultField) {
                    ServicePattern(
                        type = "RESULT_RETURN",
                        hasResultModel = true,
                        resultFieldName = resultKey,
                        dataFieldName = "Result",
                        wrapperClass = null
                    )
                } else {
                    val dataField = resultFields.firstOrNull { it != "MobileValidationResult" }
                    ServicePattern(
                        type = "DATA_RETURN",
                        hasResultModel = true,
                        resultFieldName = resultKey,
                        dataFieldName = dataField,
                        wrapperClass = null
                    )
                }
            }
        }

        return ServicePattern(
            type = "DATA_RETURN",
            hasResultModel = true,
            resultFieldName = null,
            dataFieldName = "Result",
            wrapperClass = null
        )
    }

    private fun generateRequestModel(className: String, structure: Map<String, Any>): String {

        val fields = structure.map { (key, type) ->
            val javaType = mapJsonTypeToJava(type)
            "    @JsonProperty(\"$key\")\n    private $javaType $key;"
        }.joinToString("\n\n")

        val gettersSetters = structure.map { (key, type) ->
            val javaType = mapJsonTypeToJava(type)
            val capitalizedKey = key.capitalize()
            """
    public $javaType get$capitalizedKey() {
        return $key;
    }

    public void set$capitalizedKey($javaType $key) {
        this.$key = $key;
    }
            """.trimIndent()
        }.joinToString("\n\n")

        val content = """
package com.pozitron.turkiyefinans.models.rest.requestmodels;

import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

$fields

$gettersSetters
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/requestmodels/$className.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun generateResponseModel(
        className: String,
        serviceName: String,
        structure: Map<String, Any>,
        pattern: ServicePattern
    ): String {
        val resultKey = pattern.resultFieldName ?: "${serviceName}Result"
        val resultType = if (pattern.hasResultModel) "${serviceName}Result" else "MobileMethodResultUX"
        val fieldName = resultType.replaceFirstChar { it.lowercase() }
        val getterName = resultKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val content = """
package com.pozitron.turkiyefinans.models.rest.responsemodels;

import ${if (pattern.hasResultModel) "com.pozitron.turkiyefinans.models.rest.resultmodels.${serviceName}Result" else "com.pozitron.turkiyefinans.models.rest.MobileMethodResultUX"};
import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

    @JsonProperty("$resultKey")
    private $resultType $fieldName;

    public $resultType get$getterName() {
        return $fieldName;
    }

    public void set$getterName($resultType $fieldName) {
        this.$fieldName = $fieldName;
    }
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/responsemodels/$className.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    data class FieldInfo(
        val javaType: String,
        val isNested: Boolean = false,
        val nestedClassName: String? = null
    )

    private val nestedClasses = mutableMapOf<String, Map<String, FieldInfo>>()

    private fun extractResultDataStructure(responseStructure: Map<String, Any>, pattern: ServicePattern): Map<String, FieldInfo>? {
        val resultKey = pattern.resultFieldName ?: return null
        val resultValue = responseStructure[resultKey]

        if (resultValue is JsonObject) {
            val resultFields = resultValue.asJsonObject

            if (pattern.dataFieldName != null && resultFields.has(pattern.dataFieldName)) {
                val dataField = resultFields.get(pattern.dataFieldName)

                if (dataField.isJsonObject) {
                    val dataObject = dataField.asJsonObject
                    return analyzeJsonStructure(dataObject)
                }
            }
        }

        return null
    }

    private fun analyzeJsonStructure(jsonObject: JsonObject): Map<String, FieldInfo> {
        val fields = mutableMapOf<String, FieldInfo>()

        jsonObject.entrySet().forEach { (key, value) ->
            when {
                value.isJsonObject -> {
                    val nestedClassName = "${key.replaceFirstChar { it.titlecase() }}UX"
                    val nestedFields = analyzeJsonStructure(value.asJsonObject)
                    nestedClasses[nestedClassName] = nestedFields
                    fields[key] = FieldInfo(nestedClassName, isNested = true, nestedClassName = nestedClassName)
                }
                value.isJsonArray -> {
                    val array = value.asJsonArray
                    if (array.size() > 0 && array[0].isJsonObject) {
                        val itemClassName = "${key.replaceFirstChar { it.titlecase() }.removeSuffix("s")}UX"
                        val itemFields = analyzeJsonStructure(array[0].asJsonObject)
                        nestedClasses[itemClassName] = itemFields
                        fields[key] = FieldInfo("$itemClassName[]", isNested = true, nestedClassName = itemClassName)
                    } else {
                        val primitiveType = if (array.size() > 0) {
                            inferJavaType(array[0])
                        } else {
                            "String"
                        }
                        fields[key] = FieldInfo("$primitiveType[]", isNested = false)
                    }
                }
                else -> {
                    fields[key] = FieldInfo(inferJavaType(value), isNested = false)
                }
            }
        }

        return fields
    }

    private fun generateResultModel(
        className: String,
        structure: Map<String, Any>,
        pattern: ServicePattern,
        resultUXClassName: String?
    ): String {
        val resultKey = pattern.resultFieldName ?: ""
        val resultValue = structure[resultKey]

        val fields = if (resultValue is JsonObject) {
            resultValue.asJsonObject.entrySet().map { (key, value) ->
                val javaType = when (key) {
                    "MobileValidationResult" -> "MobileValidationResultUX"
                    pattern.dataFieldName -> resultUXClassName ?: inferJavaType(value)
                    else -> inferJavaType(value)
                }
                "    @JsonProperty(\"$key\")\n    private $javaType ${key.replaceFirstChar { it.lowercase() }};"
            }.joinToString("\n\n")
        } else {
            "    @JsonProperty(\"Result\")\n    private String result;\n\n    @JsonProperty(\"MobileValidationResult\")\n    private MobileValidationResultUX mobileValidationResult;"
        }

        val gettersSetters = if (resultValue is JsonObject) {
            resultValue.asJsonObject.entrySet().map { (key, value) ->
                val javaType = when (key) {
                    "MobileValidationResult" -> "MobileValidationResultUX"
                    pattern.dataFieldName -> resultUXClassName ?: inferJavaType(value)
                    else -> inferJavaType(value)
                }
                val fieldName = key.replaceFirstChar { it.lowercase() }
                val getterSetterName = key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                """
    public $javaType get$getterSetterName() {
        return $fieldName;
    }

    public void set$getterSetterName($javaType $fieldName) {
        this.$fieldName = $fieldName;
    }
                """.trimIndent()
            }.joinToString("\n\n")
        } else {
            """
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public MobileValidationResultUX getMobileValidationResult() {
        return mobileValidationResult;
    }

    public void setMobileValidationResult(MobileValidationResultUX mobileValidationResult) {
        this.mobileValidationResult = mobileValidationResult;
    }
            """.trimIndent()
        }

        val imports = buildString {
            appendLine("import com.pozitron.turkiyefinans.models.rest.MobileValidationResultUX;")
            if (resultUXClassName != null) {
                appendLine("import com.pozitron.turkiyefinans.models.rest.$resultUXClassName;")
            }
        }

        val content = """
package com.pozitron.turkiyefinans.models.rest.resultmodels;

$imports
import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

$fields

$gettersSetters
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/resultmodels/$className.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun generateUXModelFromFieldInfo(className: String, fields: Map<String, FieldInfo>): String {
        val fieldDeclarations = fields.map { (name, fieldInfo) ->
            "    @JsonProperty(\"$name\")\n    private ${fieldInfo.javaType} ${name.replaceFirstChar { it.lowercase() }};"
        }.joinToString("\n\n")

        val gettersSetters = fields.map { (name, fieldInfo) ->
            val fieldName = name.replaceFirstChar { it.lowercase() }
            val getterSetterName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            """
    public ${fieldInfo.javaType} get$getterSetterName() {
        return $fieldName;
    }

    public void set$getterSetterName(${fieldInfo.javaType} $fieldName) {
        this.$fieldName = $fieldName;
    }
            """.trimIndent()
        }.joinToString("\n\n")

        val content = """
package com.pozitron.turkiyefinans.models.rest;

import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

$fieldDeclarations

$gettersSetters
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/$className.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun generateUXModel(className: String, fields: Map<String, String>): String {
        val fieldDeclarations = fields.map { (name, type) ->
            "    @JsonProperty(\"$name\")\n    private $type ${name.decapitalize()};"
        }.joinToString("\n\n")

        val gettersSetters = fields.map { (name, type) ->
            val fieldName = name.decapitalize()
            """
    public $type get$name() {
        return $fieldName;
    }

    public void set$name($type $fieldName) {
        this.$fieldName = $fieldName;
    }
            """.trimIndent()
        }.joinToString("\n\n")

        val content = """
package com.pozitron.turkiyefinans.models.rest;

import org.codehaus.jackson.annotate.JsonProperty;
import java.io.Serializable;

public class $className implements Serializable {

$fieldDeclarations

$gettersSetters
}
        """.trimIndent()

        val filePath = "$basePath/models/rest/$className.java"
        File(filePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        return filePath
    }

    private fun extractNestedModels(structure: Map<String, Any>): Map<String, Map<String, String>> {
        // For now, return empty. Can be extended to detect nested UX objects
        return emptyMap()
    }

    private fun addToServiceEndpoint(serviceName: String) {
        val enumFile = File("$basePath/rest/ServiceEndpoint.java")
        if (!enumFile.exists()) return

        val content = enumFile.readText()

        // Check if already exists
        if (content.contains("$serviceName(\"$serviceName\")")) {
            return
        }

        // Find last enum entry and add after it
        val lastCommaIndex = content.lastIndexOf(");")
        if (lastCommaIndex > 0) {
            val newEntry = ",\n    $serviceName(\"$serviceName\")"
            val updatedContent = content.substring(0, lastCommaIndex) + newEntry + content.substring(lastCommaIndex)
            enumFile.writeText(updatedContent)
        }
    }

    private fun addToRestClient(serviceName: String, pattern: ServicePattern, hasRequest: Boolean, resultUXClassName: String?) {
        val clientFile = File("$basePath/rest/AdcIntegrationRestClient.java")
        if (!clientFile.exists()) return

        val methodName = serviceName.replaceFirstChar { it.lowercase() }
        val requestParam = if (hasRequest) "${serviceName}Request request" else ""
        val requestArg = if (hasRequest) "request" else "null"
        val resultKey = pattern.resultFieldName ?: "${serviceName}Result"
        val getterName = resultKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val methodCode = when (pattern.type) {
            "VOID" -> """
    public void $methodName(${if (hasRequest) "${serviceName}Request request" else ""}) throws RestClientException, MiddlewareException {
        ${serviceName}Response response = call(ServiceEndpoint.$serviceName, getRestServicePath(), $requestArg, ${serviceName}Response.class);
        handleMobileValidationResult(response.get$getterName().getMobileValidationResult());
    }
            """.trimIndent()

            "RESULT_RETURN" -> {
                val dataGetter = pattern.dataFieldName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Result"
                val returnTypeActual = resultUXClassName ?: (pattern.dataFieldName ?: "String")
                """
    public $returnTypeActual $methodName($requestParam) throws RestClientException, MiddlewareException {
        ${serviceName}Response response = call(ServiceEndpoint.$serviceName, getRestServicePath(), $requestArg, ${serviceName}Response.class);
        handleMobileValidationResult(response.get$getterName().getMobileValidationResult());
        return response.get$getterName().get$dataGetter();
    }
                """.trimIndent()
            }

            else -> {
                val dataGetter = pattern.dataFieldName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Result"
                val returnTypeActual = resultUXClassName ?: (pattern.dataFieldName ?: "Object")
                """
    public $returnTypeActual $methodName($requestParam) throws RestClientException, MiddlewareException {
        ${serviceName}Response response = call(ServiceEndpoint.$serviceName, getRestServicePath(), $requestArg, ${serviceName}Response.class);
        handleMobileValidationResult(response.get$getterName().getMobileValidationResult());
        return response.get$getterName().get$dataGetter();
    }
                """.trimIndent()
            }
        }

        // Append method at the end of class (before last })
        val content = clientFile.readText()
        val lastBraceIndex = content.lastIndexOf("}")
        if (lastBraceIndex > 0) {
            val updatedContent = content.substring(0, lastBraceIndex) + "\n$methodCode\n" + content.substring(lastBraceIndex)
            clientFile.writeText(updatedContent)
        }
    }

    private fun mapJsonTypeToJava(type: Any): String {
        return when (type) {
            is String -> when (type) {
                "String" -> "String"
                "Integer" -> "int"
                "Double" -> "double"
                "Boolean" -> "boolean"
                "Array" -> "List<Object>"
                else -> type
            }
            is JsonObject -> "Object" // Should be replaced with proper UX class
            else -> "String"
        }
    }

    private fun inferJavaType(value: com.google.gson.JsonElement): String {
        return when {
            value.isJsonPrimitive -> {
                when {
                    value.asJsonPrimitive.isString -> "String"
                    value.asJsonPrimitive.isNumber -> {
                        val numStr = value.asString
                        when {
                            numStr.contains(".") -> "double"
                            else -> {
                                val num = value.asLong
                                if (num > Int.MAX_VALUE || num < Int.MIN_VALUE) "long" else "int"
                            }
                        }
                    }
                    value.asJsonPrimitive.isBoolean -> "boolean"
                    else -> "String"
                }
            }
            value.isJsonObject -> "Object"
            value.isJsonArray -> {
                val array = value.asJsonArray
                if (array.size() > 0) {
                    val firstElement = array[0]
                    when {
                        firstElement.isJsonObject -> "Object[]"
                        firstElement.isJsonPrimitive -> {
                            val primitiveType = inferJavaType(firstElement)
                            "$primitiveType[]"
                        }
                        else -> "Object[]"
                    }
                } else {
                    "Object[]"
                }
            }
            else -> "Object"
        }
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun String.decapitalize(): String {
        return this.replaceFirstChar { it.lowercase() }
    }
}

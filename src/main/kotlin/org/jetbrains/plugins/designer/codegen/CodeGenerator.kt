package org.jetbrains.plugins.designer.codegen

import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import org.jetbrains.plugins.template.designer.components.ComponentRegistry

object CodeGenerator {

    fun generateFullFlowCode(className: String, screens: List<Screen>): String {
        if (screens.isEmpty()) {
            return "// No screens to generate. Please add screens first."
        }

        val code = StringBuilder()

        code.append(generateClassHeader(className))
        code.append(generateScreenIdentifiers(screens))
        code.append(generateGetPropertiesResponse(screens))
        code.append(generateGetActionResponse(screens))

        screens.forEach { screen ->
            when (screen.type) {
                ScreenType.Form -> code.append(generateFormScreenMethods(screen, screens))
                ScreenType.Confirm -> code.append(generateConfirmScreenMethods(screen))
                ScreenType.Success -> code.append(generateSuccessScreenMethods(screen, screens))
                ScreenType.List -> code.append(generateListScreenMethods(screen))
                else -> {}
            }
        }

        code.append(generateUtilityMethods(screens))
        code.append(generateClassFooter())

        return code.toString()
    }

    // ========== CLASS STRUCTURE ==========

    private fun generateClassHeader(className: String): String {
        return """
package com.pozitron.turkiyefinans.flow.generated;

import com.dinamo.components.textfield.TFComponentTextFieldInput;
import com.dinamo.components.textfield.TFComponentAmountTextFieldInput;
import com.dinamo.components.combobox.TFComponentComboBoxInput;
import com.dinamo.components.datepicker.TFComponentDateInput;
import com.dinamo.components.button.TFComponentButton;
import com.dinamo.components.paymentinstruction.TFComponentPaymentToolSelection;
import com.dinamo.models.*;
import com.pozitron.turkiyefinans.actions.screen.action.*;
import com.pozitron.turkiyefinans.actions.screen.get.*;
import com.pozitron.turkiyefinans.core.*;
import com.pozitron.turkiyefinans.core.exceptions.MiddlewareException;
import com.pozitron.turkiyefinans.core.messages.Messages;
import com.pozitron.turkiyefinans.core.utils.*;
import com.pozitron.turkiyefinans.flow.TFBaseFlow;
import com.pozitron.turkiyefinans.models.Dictionary;
import com.pozitron.turkiyefinans.models.DictionaryLine;
import org.datacontract.schemas._2004._07.TurkiyeFinans_MsmqLogger_AdcLog.AdcLogType;

import java.rmi.RemoteException;
import java.util.*;

public class $className extends TFBaseFlow {

""".trimIndent()
    }

    private fun generateScreenIdentifiers(screens: List<Screen>): String {
        val code = StringBuilder()
        code.append("    // ========== SCREEN IDENTIFIERS ==========\n")
        screens.forEach { screen ->
            code.append("    public static final String ${screen.name} = \"${screen.name}\";\n")
        }
        code.append("\n")
        return code.toString()
    }

    private fun generateClassFooter(): String {
        return "}\n"
    }

    // ========== MAIN METHODS ==========

    private fun generateGetPropertiesResponse(screens: List<Screen>): String {
        val code = StringBuilder()

        code.append("""
    @Override
    public GetScreenPropertiesResponse getPropertiesResponse(
            final IPozitronIntegrationService service,
            final GetScreenPropertiesRequest request,
            final Messages messages,
            final CachedUtil cachedUtil) throws MiddlewareException, RemoteException {

        HashMap properties = new HashMap();
        TFScreenType screenType = TFScreenType.None;
        String identifier = request.getIdentifier();
        HashMap navigationAction = Util.getNavigationActionForPush();

""".trimIndent())

        val entryScreens = screens.filter { it.isEntryScreen }

        if (entryScreens.isEmpty()) {
            code.append("""
        
        return new GetScreenPropertiesResponse(properties, screenType, identifier, navigationAction);
    }

""".trimIndent())
            return code.toString()
        }

        entryScreens.forEachIndexed { index, screen ->
            val condition = if (index == 0) "if" else "} else if"

            code.append("\n        $condition (identifier.equals(${screen.name})) {\n")
            code.append("            screenType = TFScreenType.${screen.type};\n")
            code.append("            properties.put(TFIdentifier.TITLE, messages.getMessage(\"${screen.name.lowercase()}_title\"));\n")

            when (screen.type) {
                ScreenType.Form -> {
                    code.append("            properties.put(TFIdentifier.INPUTS, get${screen.name}Inputs(cachedUtil, service, messages));\n")
                    code.append("            ThemeUtil.configureFormProperties(getThemeValue(), properties, Util.getNavigationBarContinueButton());\n")
                }
                ScreenType.List -> {
                    code.append("            properties.put(TFIdentifier.DATASOURCE, get${screen.name}DataSource(service, messages));\n")
                    code.append("            ThemeUtil.configureProperties(screenType, getThemeValue(), properties);\n")
                }
                ScreenType.Empty -> {
                    code.append("            properties.put(TFIdentifier.EMPTYSTATEIMAGE, ThemeUtil.getImage(getThemeValue(), ThemeUtil.EMPTYIMAGENAME));\n")
                    code.append("            properties.put(TFIdentifier.EMPTYSTATETITLETEXT, messages.getMessage(\"no_data_warning\"));\n")
                }
                else -> {}
            }
            code.append("        ")
        }

        code.append("""
}

        return new GetScreenPropertiesResponse(properties, screenType, identifier, navigationAction);
    }

""".trimIndent())

        return code.toString()
    }

    private fun generateGetActionResponse(screens: List<Screen>): String {
        val code = StringBuilder()

        code.append("""
    @Override
    public ScreenActionResponse getActionResponse(
            final IPozitronIntegrationService service,
            final ScreenActionRequest request,
            final Messages messages,
            final CachedUtil cachedUtil) throws MiddlewareException, RemoteException {

        HashMap properties = new HashMap();
        String identifier = "";
        TFScreenType screenType = TFScreenType.None;
        HashMap navigationAction = Util.getNavigationActionForPush();

""".trimIndent())

        // Group screens by flow (Form -> Confirm -> Success)
        val flows = groupScreensByFlow(screens)
        var isFirst = true

        flows.forEach { (baseName, flowScreens) ->
            val formScreen = flowScreens.find { it.type == ScreenType.Form }
            val confirmScreen = flowScreens.find { it.type == ScreenType.Confirm }
            val successScreen = flowScreens.find { it.type == ScreenType.Success }

            // Form -> Confirm transition
            if (formScreen != null && confirmScreen != null) {
                val condition = if (isFirst) "if" else "} else if"
                isFirst = false

                code.append("\n        $condition (request.getIdentifier().equals(TFScreenType.Form + ${formScreen.name})) {\n")
                code.append("            validate${formScreen.name}(request);\n\n")
                code.append("            identifier = ${confirmScreen.name};\n")
                code.append("            screenType = TFScreenType.Confirm;\n")
                code.append("            properties = get${confirmScreen.name}Properties(service, messages, request);\n")
                code.append("        ")
            }

            // Confirm -> Success transition
            if (confirmScreen != null && successScreen != null) {
                if (isFirst) {
                    code.append("\n        if")
                    isFirst = false
                } else {
                    code.append("} else if")
                }

                code.append(" (request.getIdentifier().equals(TFScreenType.Confirm + ${confirmScreen.name})) {\n")
                code.append("            submit$baseName(service, messages, request);\n\n")
                code.append("            identifier = ${successScreen.name};\n")
                code.append("            screenType = TFScreenType.Success;\n")
                code.append("            properties = get${successScreen.name}Properties(messages);\n")
                code.append("            properties.put(TFIdentifier.DATAMODELS, getReloadDataModels());\n")
                code.append("        ")
            }

            // Button navigation from Form screens
            formScreen?.components?.filter { it.type == "BUTTON" }?.forEach { button ->
                val targetScreenId = button.properties["targetScreen"] as? String
                if (!targetScreenId.isNullOrEmpty()) {
                    val targetScreen = screens.find { it.id == targetScreenId }
                    if (targetScreen != null) {
                        if (isFirst) {
                            code.append("\n        if")
                            isFirst = false
                        } else {
                            code.append("} else if")
                        }

                        val buttonId = button.properties["identifier"] as? String ?: ""
                        code.append(" (request.getIdentifier().equals(${formScreen.name}) &&\n")
                        code.append("                \"$buttonId\".equals(request.getParameters().get(TFIdentifier.IDENTIFIER))) {\n")
                        code.append("            identifier = ${targetScreen.name};\n")
                        code.append("            screenType = TFScreenType.${targetScreen.type};\n")

                        when (targetScreen.type) {
                            ScreenType.Form -> {
                                code.append("            properties.put(TFIdentifier.TITLE, messages.getMessage(\"${targetScreen.name.lowercase()}_title\"));\n")
                                code.append("            properties.put(TFIdentifier.INPUTS, get${targetScreen.name}Inputs(cachedUtil, service, messages));\n")
                                code.append("            ThemeUtil.configureFormProperties(getThemeValue(), properties, Util.getNavigationBarContinueButton());\n")
                            }
                            else -> {
                                code.append("            properties = get${targetScreen.name}Properties(service, messages);\n")
                            }
                        }
                        code.append("        ")
                    }
                }
            }
        }

        code.append("""
}

        return new ScreenActionResponse(properties, screenType, identifier, navigationAction);
    }

""".trimIndent())

        return code.toString()
    }

    // ========== SCREEN-SPECIFIC METHODS ==========

    private fun generateFormScreenMethods(screen: Screen, allScreens: List<Screen>): String {
        val code = StringBuilder()

        code.append("""
    private List<HashMap> get${screen.name}Inputs(
            final CachedUtil cachedUtil,
            final IPozitronIntegrationService service,
            final Messages messages) throws MiddlewareException {

        List<HashMap> rowViewModelList = new ArrayList<HashMap>();

""".trimIndent())

        screen.components.forEach { component ->
            val componentDef = ComponentRegistry.getComponentByType(component.type)
            if (componentDef != null) {
                code.append("\n")
                code.append(CodeFormatter.indent(componentDef.generateCode(component, allScreens), 2))
                code.append("\n")
            }
        }

        code.append("""

        return rowViewModelList;
    }

""".trimIndent())

        return code.toString()
    }

    private fun generateConfirmScreenMethods(screen: Screen): String {
        val code = StringBuilder()

        code.append("""
    private HashMap get${screen.name}Properties(
            final IPozitronIntegrationService service,
            final Messages messages,
            final ScreenActionRequest request) throws MiddlewareException {

        HashMap properties = new HashMap();
        properties.put(TFIdentifier.TITLE, messages.getMessage("${screen.name.lowercase()}_title"));
        properties.put(TFIdentifier.INFO, get${screen.name}Info(service, messages, request));
        return properties;
    }

    private List<Dictionary> get${screen.name}Info(
            final IPozitronIntegrationService service,
            final Messages messages,
            final ScreenActionRequest request) throws MiddlewareException {

        List<Dictionary> info = new ArrayList<Dictionary>();

        ArrayList<DictionaryLine> transactionInfoArray = new ArrayList<DictionaryLine>();
        // TODO: Add confirmation details from request parameters
        
        Dictionary transactionInfo = new Dictionary();
        transactionInfo.setHeading(messages.getMessage("transaction_info"));
        transactionInfo.setDictionaryLines(transactionInfoArray);
        info.add(transactionInfo);

        return info;
    }

""".trimIndent())

        return code.toString()
    }

    private fun generateSuccessScreenMethods(screen: Screen, allScreens: List<Screen>): String {
        val code = StringBuilder()
        val firstScreen = allScreens.firstOrNull()?.name ?: "MAIN"

        code.append("""
    private HashMap get${screen.name}Properties(final Messages messages) {
        HashMap properties = new HashMap();
        
        properties.put(TFIdentifier.TITLE, messages.getMessage("${screen.name.lowercase()}_title"));
        properties.put(TFIdentifier.CONTENTTITLE, messages.getMessage("transaction_successful_title"));
        properties.put(TFIdentifier.NOTE, messages.getMessage("success_result_note"));

        HashMap actionHashMap = new HashMap();
        actionHashMap.put(TFIdentifier.ACTION, TFIdentifier.POPTOROOTANDPUSH);
        actionHashMap.put(TFIdentifier.ACTIONVALUE, $firstScreen);
        properties.put(TFIdentifier.ACTION, actionHashMap);
        properties.put(TFIdentifier.ACTIONBUTTONTITLE, messages.getMessage("back_to_main"));

        return properties;
    }

""".trimIndent())

        return code.toString()
    }

    private fun generateListScreenMethods(screen: Screen): String {
        val code = StringBuilder()

        code.append("""
    private List<HashMap> get${screen.name}DataSource(
            final IPozitronIntegrationService service,
            final Messages messages) throws MiddlewareException {

        List<HashMap> dataSource = new ArrayList<HashMap>();
        
        // TODO: Fetch and populate data source
        
        return dataSource;
    }

""".trimIndent())

        return code.toString()
    }

    // ========== UTILITY METHODS ==========

    private fun generateUtilityMethods(screens: List<Screen>): String {
        val code = StringBuilder()

        // Generate validation methods for each form screen
        screens.filter { it.type == ScreenType.Form }.forEach { screen ->
            code.append(generateValidationMethod(screen))
        }

        // Generate submit methods for each flow
        val flows = groupScreensByFlow(screens)
        flows.forEach { (baseName, flowScreens) ->
            if (flowScreens.any { it.type == ScreenType.Form }) {
                code.append(generateSubmitMethod(baseName, flowScreens))
            }
        }

        code.append(generateReloadDataModelsMethod())

        return code.toString()
    }

    private fun generateValidationMethod(screen: Screen): String {
        val code = StringBuilder()

        code.append("""
    private void validate${screen.name}(final ScreenActionRequest request) throws MiddlewareException {
""".trimIndent())

        screen.components.forEach { component ->
            val identifier = component.properties["identifier"] as? String ?: return@forEach

            when (component.type) {
                "TEXT_FIELD", "AMOUNT_FIELD" -> {
                    code.append("\n        String $identifier = TFFlowUtil.parseValue(request.getParameters(), \"$identifier\");")

                    val required = component.properties["required"] as? Boolean ?: false
                    if (required) {
                        code.append("""
        if (Util.isNullOrEmpty($identifier)) {
            throw new MiddlewareException(messages.getMessage("${identifier}_required_message"), AdcLogType.Warning, 1);
        }""")
                    }
                }
                "COMBO_BOX" -> {
                    code.append("\n        Integer ${identifier}_index = TFFlowUtil.parseIndex(request.getParameters(), \"$identifier\");")
                }
                "DATE_PICKER" -> {
                    code.append("\n        String ${identifier}_date = TFFlowUtil.parseValue(request.getParameters(), \"$identifier\");")
                }
            }
        }

        code.append("""

    }

""".trimIndent())

        return code.toString()
    }

    private fun generateSubmitMethod(baseName: String, flowScreens: List<Screen>): String {
        return """
    private void submit$baseName(
            final IPozitronIntegrationService service,
            final Messages messages,
            final ScreenActionRequest request) throws MiddlewareException {
        
        // TODO: Implement transaction submission logic
        // Extract data from request parameters
        // Call service methods
        // Handle response
    }

""".trimIndent()
    }

    private fun generateReloadDataModelsMethod(): String {
        return """
    private List<String> getReloadDataModels() {
        List<String> dataModels = new ArrayList<String>();
        // TODO: Add data models that need to be reloaded
        return dataModels;
    }

""".trimIndent()
    }

    // ========== HELPER METHODS ==========

    private fun groupScreensByFlow(screens: List<Screen>): Map<String, List<Screen>> {
        val flows = mutableMapOf<String, MutableList<Screen>>()

        screens.forEach { screen ->
            val baseName = screen.name
                .replace("_FORM", "")
                .replace("_CONFIRM", "")
                .replace("_SUCCESS", "")

            flows.getOrPut(baseName) { mutableListOf() }.add(screen)
        }

        return flows
    }
}

// ========== CODE FORMATTER ==========

object CodeFormatter {

    fun indent(code: String, level: Int): String {
        val indentation = "    ".repeat(level)
        return code.lines().joinToString("\n") { line ->
            if (line.isBlank()) line else indentation + line
        }
    }

    fun formatMethodName(screenName: String): String {
        return screenName.split("_")
            .joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    fun formatVariableName(identifier: String): String {
        return identifier.lowercase().replace("_", "")
    }
}
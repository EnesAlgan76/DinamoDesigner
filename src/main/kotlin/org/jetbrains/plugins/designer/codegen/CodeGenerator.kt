package org.jetbrains.plugins.designer.codegen

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.designer.models.Screen
import org.jetbrains.plugins.designer.models.ScreenType
import org.jetbrains.plugins.template.designer.components.ComponentRegistry

object CodeGenerator {

    fun generateFullFlowCode(project: Project, className: String, screens: List<Screen>): String {
        if (screens.isEmpty()) {
            return "// No screens to generate. Please add screens first."
        }

        val code = StringBuilder()

        code.append(generateClassHeader(className))
        code.append(generateScreenIdentifiers(project, screens))
        code.append(generateGetPropertiesResponse(project, screens))
        code.append(generateGetActionResponse(project, screens))

        screens.forEach { screen ->
            when (screen.type) {
                ScreenType.Form -> code.append(generateFormScreenMethods(project, screen, screens))
                ScreenType.Confirm -> code.append(generateConfirmScreenMethods(project, screen))
                ScreenType.Success -> code.append(generateSuccessScreenMethods(project, screen, screens))
                ScreenType.List -> code.append(generateListScreenMethods(project, screen))
                else -> {}
            }
        }
        code.append(generateClassFooter())

        return code.toString()
    }

    /**
     * ScreenActionImpl.java için else-if bloğu üretir
     * Bu kod bloğu ScreenActionImpl.java dosyasına manuel olarak eklenmelidir
     */
    fun generateScreenActionImplBlock(project: Project, className: String, screens: List<Screen>): String {
        if (screens.isEmpty()) {
            return "// No screens to generate"
        }

        val code = StringBuilder()

        val allIdentifiers = mutableListOf<String>()

        // Tüm screen identifierlarını topla
        screens.forEach { screen ->
            val screenIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, screen.name)

            when (screen.type) {
                ScreenType.Form -> {
                    allIdentifiers.add("identifier.equals(TFScreenType.Form + $screenIdentifier)")
                }
                ScreenType.Confirm -> {
                    allIdentifiers.add("identifier.equals(TFScreenType.Confirm + $screenIdentifier)")
                }
                ScreenType.Success -> {
                    allIdentifiers.add("identifier.equals(TFScreenType.Success + $screenIdentifier)")
                }
                else -> {
                    allIdentifiers.add("identifier.equals($screenIdentifier)")
                }
            }
        }

        // Entry screen identifier'ı da ekle
        val entryScreen = screens.find { it.isEntryScreen }
        if (entryScreen != null) {
            val entryIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, entryScreen.name)
            if (!allIdentifiers.contains("identifier.equals($entryIdentifier)")) {
                allIdentifiers.add(0, "identifier.equals($entryIdentifier)")
            }
        }

        // else if bloğunu oluştur
        code.append("else if (")
        code.append(allIdentifiers.joinToString(" ||\n                "))
        code.append(") {\n\n")
        code.append("    flow = new $className();\n\n")

        return code.toString()
    }

    // ========== CLASS STRUCTURE ==========

    private fun generateClassHeader(className: String): String {
        return """
package com.pozitron.turkiyefinans.flow.generated;

import com.pozitron.turkiyefinans.flow.TFBaseFlow;
import com.dinamo.TFComponentIdentifier;
import com.dinamo.actions.TFActionSelect;
import com.dinamo.components.account.*;
import com.dinamo.components.button.TFComponentTextButton;
import com.dinamo.components.campaign.*;
import com.dinamo.components.card.TFComponentDoubleOperation;
import com.dinamo.components.card.TFComponentGenericCard;
import com.dinamo.components.card.TFComponentOperation;
import com.dinamo.components.keyvaluelabel.*;
import com.dinamo.components.label.TFComponentGenericLabel;
import com.dinamo.components.paymentinstruction.TFComponentPaymentToolSelection;
import com.dinamo.components.radiobutton.TFComponentRadioButtonSelection;
import com.dinamo.components.segment.TFComponentSegmentOption;
import com.dinamo.components.tablayout.TFComponentTabBar;
import com.dinamo.components.textfield.TFComponentDigitTextFieldInput;
import com.dinamo.models.*;
import com.pozitron.turkiyefinans.actions.datasource.GetDataSourceRequest;
import com.pozitron.turkiyefinans.actions.datasource.GetDataSourceResponse;
import com.pozitron.turkiyefinans.actions.favorite.list.GetFavoriteOperationsResponse;
import com.pozitron.turkiyefinans.actions.screen.action.ScreenActionRequest;
import com.pozitron.turkiyefinans.actions.screen.action.ScreenActionResponse;
import com.pozitron.turkiyefinans.actions.screen.get.GetScreenPropertiesRequest;
import com.pozitron.turkiyefinans.actions.screen.get.GetScreenPropertiesResponse;
import com.pozitron.turkiyefinans.artifacts.*;

import com.dinamo.actions.TFActionFetchData;
import com.dinamo.actions.TFActionVisible;
import com.dinamo.attribute.TFViewLayoutAttribute;
import com.dinamo.components.account.TFComponentAccountTransaction;
import com.dinamo.components.account.TFComponentAccountTransactionListItem;
import com.dinamo.components.additional.TFComponentAdditional;
import com.dinamo.components.agreement.TFComponentApprovedAgreement;
import com.dinamo.components.button.TFComponentButton;
import com.dinamo.components.button.TFComponentLinkButton;
import com.dinamo.components.checkbox.TFComponentCheckBox;
import com.dinamo.components.combobox.TFComponentComboBoxInput;
import com.dinamo.components.combobox.TFComponentComboBoxInputForList;
import com.dinamo.components.label.TFComponentLabel;
import com.dinamo.components.list.TFComponentList;
import com.dinamo.components.loading.TFComponentLoading;
import com.dinamo.components.navigationbarbutton.TFComponentFilterNavigationBarButton;
import com.dinamo.components.switchinput.TFComponentSwitchInput;
import com.dinamo.components.table.TFComponentKeyValueTable;
import com.dinamo.components.textfield.TFComponentAmountTextFieldInput;
import com.dinamo.components.textfield.TFComponentTextFieldInput;
import com.pozitron.turkiyefinans.artifacts.CreditCardFilterPackage;
import com.pozitron.turkiyefinans.core.*;
import com.pozitron.turkiyefinans.core.exceptions.MiddlewareException;
import com.pozitron.turkiyefinans.core.integration.codevo.transaction.AuthorizationHelper;
import com.pozitron.turkiyefinans.core.interceptors.wrappers.BooleanWrapper;
import com.pozitron.turkiyefinans.core.messages.Messages;
import com.pozitron.turkiyefinans.core.utils.*;
import com.pozitron.turkiyefinans.core.validation.TFValidationHelper;
import com.pozitron.turkiyefinans.flow.TFBaseCardFlow;
import com.pozitron.turkiyefinans.flow.authorization.TFAuthorizationFlow;
import com.pozitron.turkiyefinans.flow.digifinance.TFDigiFinanceFlow;
import com.pozitron.turkiyefinans.flow.digifinance.TFQRPaymentFlow;
import com.pozitron.turkiyefinans.flow.operation.TFOperationListFlow;
import com.pozitron.turkiyefinans.flow.password.TFPasswordFlow;
import com.pozitron.turkiyefinans.flow.personal.TFPersonalInfoFlow;
import com.pozitron.turkiyefinans.flow.receipt.TFReceiptFlow;
import com.pozitron.turkiyefinans.flow.securevalidation.ISecureValidationCallback;
import com.pozitron.turkiyefinans.flow.securevalidation.TFSecureValidationDataContainer;
import com.pozitron.turkiyefinans.flow.securevalidation.TFSecureValidationFlow;
import com.pozitron.turkiyefinans.models.Dictionary;
import com.pozitron.turkiyefinans.models.*;
import com.pozitron.turkiyefinans.models.rest.*;
import com.pozitron.turkiyefinans.models.rest.requestmodels.*;
import com.pozitron.turkiyefinans.models.rest.responsemodels.*;
import com.pozitron.turkiyefinans.rest.AdcIntegrationRestClient;
import com.pozitron.turkiyefinans.rest.mapper.AccountMapper;
import com.pozitron.turkiyefinans.rest.mapper.CommonMapper;
import com.pozitron.turkiyefinans.rest.mapper.CreditCardMapper;
import com.pozitron.turkiyefinans.rest.RestClientException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.datacontract.schemas._2004._07.TurkiyeFinans_MsmqLogger_AdcLog.AdcLogType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.*;

import static com.pozitron.turkiyefinans.core.Util.convertTimestampToDateString;
import static com.pozitron.turkiyefinans.core.integration.codevo.transaction.AuthorizationHelper.*;

public class $className extends TFBaseFlow {

""".trimIndent()
    }

    private fun generateScreenIdentifiers(project: Project, screens: List<Screen>): String {
        val code = StringBuilder()
        code.append("    // ========== SCREEN IDENTIFIERS ==========\n")
        code.append("    // Note: Screen identifiers are managed in TFIdentifier class\n")
        screens.forEach { screen ->
            // Register the identifier in TFIdentifier but don't generate duplicate constants here
            TFIdentifierManager.getOrCreateIdentifier(project, screen.name)
        }
        code.append("\n")
        return code.toString()
    }

    private fun generateClassFooter(): String {
        return "}\n"
    }

    // ========== MAIN METHODS ==========

    private fun generateGetPropertiesResponse(project: Project, screens: List<Screen>): String {
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
            val screenIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, screen.name)

            code.append("\n        $condition (identifier.equals($screenIdentifier)) {\n")
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

    private fun generateGetActionResponse(project: Project, screens: List<Screen>): String {
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

        var isFirst = true

        screens.filter { it.type == ScreenType.Form && it.nextScreenId != null }.forEach { formScreen ->
            val nextScreen = screens.find { it.id == formScreen.nextScreenId }
            if (nextScreen != null) {
                val condition = if (isFirst) "if" else "} else if"
                isFirst = false

                val formIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, formScreen.name)
                val nextIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, nextScreen.name)

                code.append("\n        $condition (request.getIdentifier().equals(TFScreenType.Form + $formIdentifier)) {\n")
                code.append("            // Default continue button transition\n")
                code.append("            validate${formScreen.name}(request);\n\n")
                code.append("            identifier = $nextIdentifier;\n")
                code.append("            screenType = TFScreenType.${nextScreen.type};\n")

                when (nextScreen.type) {
                    ScreenType.Form -> {
                        code.append("            properties.put(TFIdentifier.TITLE, messages.getMessage(\"${nextScreen.name.lowercase()}_title\"));\n")
                        code.append("            properties.put(TFIdentifier.INPUTS, get${nextScreen.name}Inputs(cachedUtil, service, messages));\n")
                        code.append("            ThemeUtil.configureFormProperties(getThemeValue(), properties, Util.getNavigationBarContinueButton());\n")
                    }
                    ScreenType.Confirm -> {
                        code.append("            properties = get${nextScreen.name}Properties(service, messages, request);\n")
                        code.append("            ThemeUtil.configureFormProperties(getThemeValue(), properties, Util.getNavigationBarContinueButton());\n")
                    }
                    else -> {
                        code.append("            properties = get${nextScreen.name}Properties(service, messages);\n")
                    }
                }
                code.append("        ")
            }
        }

        // 2. Sonra conventional flow-based transitions (Form -> Confirm -> Success)
        val flows = groupScreensByFlow(screens)

        flows.forEach { (baseName, flowScreens) ->
            val formScreen = flowScreens.find { it.type == ScreenType.Form }
            val confirmScreen = flowScreens.find { it.type == ScreenType.Confirm }
            val successScreen = flowScreens.find { it.type == ScreenType.Success }

            if (formScreen != null && confirmScreen != null && formScreen.nextScreenId == null) {
                val condition = if (isFirst) "if" else "} else if"
                isFirst = false
                val formIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, formScreen.name)
                val confirmIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, confirmScreen.name)

                code.append("\n        $condition (request.getIdentifier().equals(TFScreenType.Form + $formIdentifier)) {\n")
                code.append("            // Conventional flow: Form -> Confirm\n")
                code.append("            validate${formScreen.name}(request);\n\n")
                code.append("            identifier = $confirmIdentifier;\n")
                code.append("            screenType = TFScreenType.Confirm;\n")
                code.append("            properties = get${confirmScreen.name}Properties(service, messages, request);\n")
                code.append("            ThemeUtil.configureFormProperties(getThemeValue(), properties, Util.getNavigationBarContinueButton());\n")
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

                val confirmIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, confirmScreen.name)
                val successIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, successScreen.name)

                code.append(" (request.getIdentifier().equals(TFScreenType.Confirm + $confirmIdentifier)) {\n")
                code.append("            submit$baseName(service, messages, request);\n\n")
                code.append("            identifier = $successIdentifier;\n")
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

                        val formIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, formScreen.name)
                        val targetIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, targetScreen.name)
                        val buttonId = button.properties["identifier"] as? String ?: ""
                        val buttonIdentifier = TFIdentifierManager.getOrCreateIdentifier(project, buttonId)

                        code.append(" (request.getIdentifier().equals($formIdentifier) &&\n")
                        code.append("                $buttonIdentifier.equals(request.getParameters().get(TFIdentifier.IDENTIFIER))) {\n")
                        code.append("            identifier = $targetIdentifier;\n")
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

    private fun generateFormScreenMethods(project: Project, screen: Screen, allScreens: List<Screen>): String {
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
                code.append(CodeFormatter.indent(componentDef.generateCode(project, component, allScreens), 2))
                code.append("\n")
            }
        }

        code.append("""

        return rowViewModelList;
    }

""".trimIndent())

        return code.toString()
    }

    private fun generateConfirmScreenMethods(project: Project, screen: Screen): String {
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

    private fun generateSuccessScreenMethods(project: Project, screen: Screen, allScreens: List<Screen>): String {
        val code = StringBuilder()
        val firstScreenName = allScreens.firstOrNull()?.name ?: "MAIN"
        val firstScreen = TFIdentifierManager.getOrCreateIdentifier(project, firstScreenName)

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

    private fun generateListScreenMethods(project: Project, screen: Screen): String {
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
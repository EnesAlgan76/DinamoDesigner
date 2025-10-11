package org.jetbrains.plugins.template.services

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.*

data class IfStatementInfo(
    val condition: String,
    val lineNumber: Int,
    val hasElse: Boolean,
    val isElseIf: Boolean = false,
    val nestingLevel: Int = 0,
    val isElse: Boolean = false,
    val hasContent: Boolean = true
)

data class IfAnalysisResult(
    val fileName: String,
    val statements: List<IfStatementInfo>
) {
    val isEmpty: Boolean get() = statements.isEmpty()
    val totalCount: Int get() = statements.size
    val regularIfs: Int get() = statements.count { !it.isElseIf && !it.isElse }
    val elseIfs: Int get() = statements.count { it.isElseIf }
    val elseBlocks: Int get() = statements.count { it.isElse }
    val nestedBlocks: Int get() = statements.count { it.nestingLevel > 0 }
    val maxNestingLevel: Int get() = statements.maxOfOrNull { it.nestingLevel } ?: 0
    val emptyElseBlocks: Int get() = statements.count { it.isElse && !it.hasContent }

    fun formatAsJson(): String {
        if (statements.isEmpty()) {
            return "// ${fileName.ifEmpty { "No file selected" }}\n\nNo if statements found"
        }

        val sb = StringBuilder()
        sb.append("// $fileName\n\n")

        statements.sortedBy { it.lineNumber }.forEach { statement ->
            val indent = "  ".repeat(statement.nestingLevel)
            val type = when {
                statement.isElse -> "else"
                statement.isElseIf -> "else if"
                else -> "if"
            }

            sb.append("${indent}$type")
            if (!statement.isElse) {
                sb.append(" (${statement.condition})")
            }
            sb.append(" { // Line ${statement.lineNumber}, Level ${statement.nestingLevel}")
            if (statement.isElse && !statement.hasContent) {
                sb.append(", empty")
            }
            sb.append(" }\n")
        }

        return sb.toString()
    }

    fun getSummary(): String {
        if (isEmpty) return "No if statements"

        val parts = mutableListOf<String>()
        parts.add("$totalCount blocks")

        if (elseBlocks > 0) {
            parts.add("$elseBlocks else")
        }

        if (nestedBlocks > 0) {
            parts.add("$nestedBlocks nested (max depth: $maxNestingLevel)")
        }

        return parts.joinToString(", ")
    }
}

class IfStatementAnalyzerService(private val project: Project) {

    fun analyze(psiFile: PsiJavaFile): IfAnalysisResult {
        val fileName = psiFile.virtualFile?.name ?: ""
        val statements = mutableListOf<IfStatementInfo>()
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)

        analyzeElement(psiFile, statements, document, 0)

        return IfAnalysisResult(fileName, statements)
    }

    private fun analyzeElement(
        element: PsiElement,
        ifStatements: MutableList<IfStatementInfo>,
        document: Document?,
        nestingLevel: Int
    ) {
        if (element is PsiIfStatement) {
            val isElseIf = isElseIfStatement(element)

            if (!isElseIf) {
                val condition = element.condition?.text ?: "unknown"
                val lineNumber = document?.getLineNumber(element.textOffset)?.plus(1) ?: -1
                val hasElse = element.elseBranch != null && element.elseBranch !is PsiIfStatement

                ifStatements.add(
                    IfStatementInfo(
                        condition = condition,
                        lineNumber = lineNumber,
                        hasElse = hasElse,
                        isElseIf = isElseIf,
                        nestingLevel = nestingLevel
                    )
                )

                // Analyze else-if chain
                var currentElse = element.elseBranch
                while (currentElse is PsiIfStatement) {
                    val elseIfCondition = currentElse.condition?.text ?: "unknown"
                    val elseIfLineNumber = document?.getLineNumber(currentElse.textOffset)?.plus(1) ?: -1
                    val elseIfHasElse = currentElse.elseBranch != null && currentElse.elseBranch !is PsiIfStatement

                    ifStatements.add(
                        IfStatementInfo(
                            condition = elseIfCondition,
                            lineNumber = elseIfLineNumber,
                            hasElse = elseIfHasElse,
                            isElseIf = true,
                            nestingLevel = nestingLevel
                        )
                    )

                    currentElse = currentElse.elseBranch
                }

                // Add final else block
                if (currentElse != null && currentElse !is PsiIfStatement) {
                    val elseLineNumber = document?.getLineNumber(currentElse.textOffset)?.plus(1) ?: -1
                    val hasContent = hasElseContent(currentElse)

                    ifStatements.add(
                        IfStatementInfo(
                            condition = "",
                            lineNumber = elseLineNumber,
                            hasElse = false,
                            isElseIf = false,
                            nestingLevel = nestingLevel,
                            isElse = true,
                            hasContent = hasContent
                        )
                    )
                }

                // Analyze nested ifs
                element.thenBranch?.let { analyzeElement(it, ifStatements, document, nestingLevel + 1) }
                element.elseBranch?.let { elseBranch ->
                    if (elseBranch !is PsiIfStatement) {
                        analyzeElement(elseBranch, ifStatements, document, nestingLevel + 1)
                    }
                }
            }
        } else {
            element.children.forEach { child ->
                analyzeElement(child, ifStatements, document, nestingLevel)
            }
        }
    }

    private fun hasElseContent(elseStatement: PsiElement): Boolean {
        return when (elseStatement) {
            is PsiBlockStatement -> {
                val statements = elseStatement.codeBlock.statements
                statements.isNotEmpty() && statements.any { it !is PsiEmptyStatement }
            }
            is PsiEmptyStatement -> false
            else -> true
        }
    }

    private fun isElseIfStatement(ifStatement: PsiIfStatement): Boolean {
        val parent = ifStatement.parent
        return parent is PsiIfStatement && parent.elseBranch == ifStatement
    }
}
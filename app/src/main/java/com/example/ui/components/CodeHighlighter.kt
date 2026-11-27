package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodePlain
import com.example.ui.theme.CodeScratchApi
import com.example.ui.theme.CodeString

object CodeHighlighter {

    private val keywords = setOf(
        "function", "const", "let", "var", "class", "return", "if", "else",
        "throw", "new", "true", "false", "null", "undefined", "async", "await",
        "for", "while", "Boolean", "Number", "String", "Error"
    )

    fun highlightJs(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = CodePlain), 0, code.length)

            // Keywords
            val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            for (match in wordRegex.findAll(code)) {
                val word = match.value
                if (word in keywords) {
                    addStyle(
                        SpanStyle(color = CodeKeyword, fontWeight = FontWeight.Medium),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // Scratch APIs & vm
            val scratchApiRegex = Regex("\\b(Scratch(?:\\.[A-Za-z0-9_]+)*|vm(?:\\.[A-Za-z0-9_]+)*)\\b")
            for (match in scratchApiRegex.findAll(code)) {
                addStyle(
                    SpanStyle(color = CodeScratchApi, fontWeight = FontWeight.Medium),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Numbers & hex colors
            val numRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
            for (match in numRegex.findAll(code)) {
                addStyle(
                    SpanStyle(color = CodeNumber),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Strings ('...' or "...")
            val stringRegex = Regex("'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"")
            for (match in stringRegex.findAll(code)) {
                addStyle(
                    SpanStyle(color = CodeString),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Single-line comments (last so they override keywords inside comments)
            val commentRegex = Regex("//.*")
            for (match in commentRegex.findAll(code)) {
                addStyle(
                    SpanStyle(color = CodeComment),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}

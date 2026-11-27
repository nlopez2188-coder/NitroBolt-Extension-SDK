package com.example.data

import com.example.model.CustomShapeDefinition
import kotlin.math.abs
import kotlin.math.min

sealed class SvgRelStep {
    data class Line(val dx: Float, val dy: Float) : SvgRelStep()
    data class Horiz(val dx: Float) : SvgRelStep()
    data class Vert(val dy: Float) : SvgRelStep()

    val totalDx: Float
        get() = when (this) {
            is Line -> dx
            is Horiz -> dx
            is Vert -> 0f
        }

    val totalDy: Float
        get() = when (this) {
            is Line -> dy
            is Horiz -> 0f
            is Vert -> dy
        }
}

data class CustomShapePadding(
    val left: Float,
    val right: Float
)

data class EdgeValidationResult(
    val isValid: Boolean,
    val leftSumsMatch: Boolean,
    val rightSumsMatch: Boolean,
    val message: String
)

object CustomShapePathEngine {

    fun computeEdgeWidth(edgeWidthBody: String, totalHeight: Float): Float {
        val body = edgeWidthBody.trim()
        if (body.isBlank()) return totalHeight / 2f

        val minCapMatch = Regex("Math\\.min\\s*\\(\\s*([^,]+)\\s*,\\s*([^)]+)\\)").find(body)
        if (minCapMatch != null) {
            val a = evalHeightExpr(minCapMatch.groupValues[1], totalHeight)
            val b = evalHeightExpr(minCapMatch.groupValues[2], totalHeight)
            if (a != null && b != null) {
                return min(a, b).coerceIn(2f, totalHeight / 2f)
            }
        }

        val returnExpr = Regex("return\\s+([^;\\n}]+)").find(body)?.groupValues?.getOrNull(1) ?: body
        val evaluated = evalHeightExpr(returnExpr, totalHeight)
        return (evaluated ?: (totalHeight / 2f)).coerceIn(2f, totalHeight / 2f)
    }

    private fun evalHeightExpr(expr: String, totalHeight: Float): Float? {
        val clean = expr.trim()
        clean.toFloatOrNull()?.let { return it }
        val replaced = clean.replace("totalHeight", totalHeight.toString())
        return evalSimpleArithmetic(replaced)
    }

    fun evaluatePadding(paddingBody: String): CustomShapePadding {
        val body = paddingBody.trim()
        if (body.isBlank()) return CustomShapePadding(16f, 16f)

        val leftMatch = Regex("\\bleft\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").find(body)
        val rightMatch = Regex("\\bright\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").find(body)
        if (leftMatch != null || rightMatch != null) {
            val l = leftMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 16f
            val r = rightMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 16f
            return CustomShapePadding(l.coerceIn(6f, 80f), r.coerceIn(6f, 80f))
        }

        val singleNum = Regex("return\\s+([0-9]+(?:\\.[0-9]+)?)").find(body)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            ?: Regex("^([0-9]+(?:\\.[0-9]+)?)$").find(body)?.groupValues?.getOrNull(1)?.toFloatOrNull()
        if (singleNum != null) {
            val clamped = singleNum.coerceIn(6f, 80f)
            return CustomShapePadding(clamped, clamped)
        }

        return CustomShapePadding(16f, 16f)
    }

    fun resolveRightEdgeSteps(
        shape: CustomShapeDefinition,
        w: Float,
        halfStraight: Float
    ): List<SvgRelStep> {
        if (shape.rightEdgeBody.isNotBlank()) {
            val explicit = parseSvgEdgeSteps(shape.rightEdgeBody, w, halfStraight)
            if (explicit.isNotEmpty()) return explicit
        }
        // Mirror leftEdge horizontally and reverse direction (top-right -> bottom-right)
        val leftSteps = parseSvgEdgeSteps(shape.leftEdgeBody, w, halfStraight)
        return leftSteps.reversed().map { step ->
            when (step) {
                is SvgRelStep.Line -> SvgRelStep.Line(step.dx, -step.dy)
                is SvgRelStep.Horiz -> SvgRelStep.Horiz(step.dx)
                is SvgRelStep.Vert -> SvgRelStep.Vert(-step.dy)
            }
        }
    }

    fun parseSvgEdgeSteps(body: String, w: Float, halfStraight: Float): List<SvgRelStep> {
        if (body.isBlank()) return emptyList()
        val steps = mutableListOf<SvgRelStep>()

        val lines = body.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("//")) continue

            // Respect simple `if (halfStraight > 0)` guard
            if (line.contains("halfStraight > 0") && halfStraight <= 0.001f) {
                continue
            }

            val pushRegex = Regex("steps\\.push\\s*\\(([^)]+)\\)")
            for (match in pushRegex.findAll(line)) {
                val argsRaw = match.groupValues[1]
                val expanded = expandPushArgs(argsRaw, w, halfStraight)
                steps.addAll(tokenizeSvgString(expanded))
            }
        }
        return steps
    }

    private fun expandPushArgs(argsRaw: String, w: Float, halfStraight: Float): String {
        val parts = argsRaw.split(",").map { part ->
            val trimmed = part.trim().trim('`', '\'', '"')
            val templateExpanded = Regex("\\$\\{([^}]+)}").replace(trimmed) { m ->
                val value = evalEdgeVarExpr(m.groupValues[1], w, halfStraight)
                value?.toString() ?: "0"
            }
            if (templateExpanded in listOf("l", "h", "v", "L", "H", "V")) {
                templateExpanded
            } else {
                evalEdgeVarExpr(templateExpanded, w, halfStraight)?.toString() ?: templateExpanded
            }
        }
        return parts.joinToString(" ")
    }

    private fun tokenizeSvgString(svg: String): List<SvgRelStep> {
        val tokens = svg.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val result = mutableListOf<SvgRelStep>()
        var i = 0
        var activeCmd = ""
        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok.equals("l", ignoreCase = true) ||
                tok.equals("h", ignoreCase = true) ||
                tok.equals("v", ignoreCase = true)
            ) {
                activeCmd = tok.lowercase()
                i++
                continue
            }
            when (activeCmd) {
                "l" -> {
                    val dx = tok.toFloatOrNull()
                    val dy = tokens.getOrNull(i + 1)?.toFloatOrNull()
                    if (dx != null && dy != null) {
                        result.add(SvgRelStep.Line(dx, dy))
                        i += 2
                    } else {
                        i++
                    }
                }
                "h" -> {
                    val dx = tok.toFloatOrNull()
                    if (dx != null) result.add(SvgRelStep.Horiz(dx))
                    i++
                }
                "v" -> {
                    val dy = tok.toFloatOrNull()
                    if (dy != null) result.add(SvgRelStep.Vert(dy))
                    i++
                }
                else -> i++
            }
        }
        return result
    }

    private fun evalEdgeVarExpr(expr: String, w: Float, halfStraight: Float): Float? {
        val clean = expr.trim()
        clean.toFloatOrNull()?.let { return it }
        val replaced = clean
            .replace("halfStraight", halfStraight.toString())
            .replace("width", w.toString())
            .replace(Regex("\\bw\\b"), w.toString())
        return evalSimpleArithmetic(replaced)
    }

    private fun evalSimpleArithmetic(expr: String): Float? {
        val s = expr.replace(" ", "")
        if (s.isBlank()) return null
        s.toFloatOrNull()?.let { return it }
        // Handle unary leading minus with single multiplication or division
        val mulIdx = s.indexOf('*')
        if (mulIdx > 0) {
            val left = evalSimpleArithmetic(s.substring(0, mulIdx)) ?: return null
            val right = evalSimpleArithmetic(s.substring(mulIdx + 1)) ?: return null
            return left * right
        }
        val divIdx = s.indexOf('/')
        if (divIdx > 0) {
            val left = evalSimpleArithmetic(s.substring(0, divIdx)) ?: return null
            val right = evalSimpleArithmetic(s.substring(divIdx + 1)) ?: return null
            return if (abs(right) > 1e-5f) left / right else null
        }
        if (s.startsWith("-")) {
            return evalSimpleArithmetic(s.substring(1))?.let { -it }
        }
        return null
    }

    fun validateShapeEdges(shape: CustomShapeDefinition): EdgeValidationResult {
        if (shape.leftEdgeBody.isBlank()) {
            return EdgeValidationResult(
                isValid = false,
                leftSumsMatch = false,
                rightSumsMatch = true,
                message = "leftEdge(steps, w, halfStraight) is required."
            )
        }

        val testHeight = 80f
        val w = computeEdgeWidth(shape.edgeWidthBody, testHeight)
        val halfStraight = ((testHeight - 2f * w) / 2f).coerceAtLeast(0f)
        val expectedHeight = 2f * w + 2f * halfStraight

        val leftSteps = parseSvgEdgeSteps(shape.leftEdgeBody, w, halfStraight)
        if (leftSteps.isEmpty()) {
            return EdgeValidationResult(
                isValid = true,
                leftSumsMatch = true,
                rightSumsMatch = true,
                message = "Custom JS edge expression (preview uses fallback geometry)."
            )
        }

        val leftDy = leftSteps.sumOf { it.totalDy.toDouble() }.toFloat()
        val leftOk = abs(leftDy + expectedHeight) < 0.75f

        val rightOk = if (shape.rightEdgeBody.isBlank()) {
            true
        } else {
            val rightSteps = parseSvgEdgeSteps(shape.rightEdgeBody, w, halfStraight)
            if (rightSteps.isEmpty()) {
                true
            } else {
                val rightDy = rightSteps.sumOf { it.totalDy.toDouble() }.toFloat()
                abs(rightDy - expectedHeight) < 0.75f
            }
        }

        val msg = when {
            leftOk && rightOk -> "Vertical path sum matches ±(2w + 2·halfStraight)"
            !leftOk -> "leftEdge vertical sum (${leftDy.toInt()}) should equal -${expectedHeight.toInt()} (use w and halfStraight)"
            else -> "rightEdge vertical sum should equal +${expectedHeight.toInt()} (use w and halfStraight)"
        }
        return EdgeValidationResult(
            isValid = leftOk && rightOk,
            leftSumsMatch = leftOk,
            rightSumsMatch = rightOk,
            message = msg
        )
    }
}

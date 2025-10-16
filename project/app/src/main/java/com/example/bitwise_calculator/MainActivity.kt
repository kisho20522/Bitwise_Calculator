package com.example.bitwise_calculator

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
//import com.example.bitwise_calculator.ui.theme.Bitwise_CalculatorTheme
import android.widget.Button
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var inputText: TextView
    private lateinit var outputText: TextView
    private lateinit var buttonBackspace : Button
    private lateinit var buttonEquals : Button
    private var expression: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize TextViews
        inputText = findViewById(R.id.input)
        outputText = findViewById(R.id.output)

        // Initialize Buttons
        buttonBackspace = findViewById(R.id.button_backspace)
        buttonEquals = findViewById(R.id.button_equals)


        // Number buttons
        val numberButtons = listOf(
            R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3,
            R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7,
            R.id.button_8, R.id.button_9
        )

        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                appendToExpression((it as Button).text.toString())
            }
        }

        // Operator buttons
        findViewById<Button>(R.id.button_and).setOnClickListener { appendToExpression("&") }
        findViewById<Button>(R.id.button_or).setOnClickListener { appendToExpression("|") }
        findViewById<Button>(R.id.button_xor).setOnClickListener { appendToExpression("^") }
        findViewById<Button>(R.id.button_not).setOnClickListener { appendToExpression("~") }
        findViewById<Button>(R.id.button_shift_left).setOnClickListener { appendToExpression("<<") }
        findViewById<Button>(R.id.button_shift_right).setOnClickListener { appendToExpression(">>") }
        findViewById<Button>(R.id.button_bracket_left).setOnClickListener { appendToExpression("(") }
        findViewById<Button>(R.id.button_bracket_right).setOnClickListener { appendToExpression(")") }

        // Backspace button
        buttonBackspace.setOnClickListener {
            val currentText = inputText.text.toString()
            if (currentText.isNotEmpty()) {
                val newText = when {
                    currentText.endsWith("<<") || currentText.endsWith(">>") ->
                        currentText.dropLast(2)
                    else ->
                        currentText.dropLast(1)
                }
                inputText.text = newText
                expression = newText // keep internal state in sync
            }
            outputText.text = ""
        }

        // Clear all if backspace is long pressed
        buttonBackspace.setOnLongClickListener {
            inputText.text = ""
            outputText.text = ""
            expression = ""
            true
        }

        // Equals button, to display the answer to the outputText
        buttonEquals.setOnClickListener {
            if (expression.isBlank()) return@setOnClickListener

            try {
                val result = evaluateBitwise(expression)
                outputText.text = result.toString()
            } catch (e: IllegalArgumentException) {
                outputText.text = e.message
            } catch (e: Exception) {
                outputText.text = "Syntax Error"
            }
        }

    }

    private fun appendToExpression(value: String) {
        expression += value
        inputText.text = expression
        outputText.text = ""
    }

    // Validates the bitwise first before it combines all the functions to evaluate the answer to be used by buttonEquals
    private fun evaluateBitwise(expr: String): Int {
        val tokens = tokenize(expr)
        val error = validateExpression(tokens)
        if (error != null) {
            throw IllegalArgumentException(error)
        }
        val postfix = infixToPostfix(tokens)
        return evaluatePostfix(postfix)
    }


    // Splits the whole expression into parts
    private fun tokenize(expr: String): List<String> {
        val regex = Regex("(\\d+|<<|>>|\\^|&|\\||~|\\(|\\))")
        return regex.findAll(expr).map { it.value }.toList()
    }

    // Infix to postfix using Shunting Yard Algorithm
    // Basically converts human readable expressions (infix) to computer readable expressions (postfix)
    private fun infixToPostfix(tokens: List<String>): List<String> {
        val precedence = mapOf("~" to 5, "<<" to 4, ">>" to 4, "&" to 3, "^" to 2, "|" to 1)
        val rightAssociative = setOf("~")
        val output = mutableListOf<String>()
        val stack = Stack<String>()

        for (token in tokens) {
            when {
                token.matches(Regex("\\d+")) -> output.add(token)
                token == "(" -> stack.push(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.peek() != "(") {
                        output.add(stack.pop())
                    }
                    stack.pop()
                }
                else -> {
                    // while there is an operator at the top of the operator stack with greater precedence
                    // or equal precedence and token is left-associative -> pop it to output
                    while (stack.isNotEmpty() && stack.peek() != "(") {
                        val o2 = stack.peek()
                        val p2 = precedence[o2] ?: 0
                        val p1 = precedence[token] ?: 0

                        val shouldPop = when {
                            p2 > p1 -> true
                            p2 == p1 -> !rightAssociative.contains(token) // pop when current token is left-assoc
                            else -> false
                        }

                        if (shouldPop) output.add(stack.pop()) else break
                    }

                    stack.push(token)
                }
            }
        }

        while (stack.isNotEmpty()) output.add(stack.pop())
        return output
    }

    // Solves the postfix into an actual answer
    private fun evaluatePostfix(postfix: List<String>): Int {
        val stack = Stack<Int>()

        for (token in postfix) {
            when {
                token.matches(Regex("\\d+")) -> stack.push(token.toInt())
                token == "~" -> stack.push(stack.pop().inv())
                else -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    val result = when (token) {
                        "&" -> a and b
                        "|" -> a or b
                        "^" -> a xor b
                        "<<" -> a shl b
                        ">>" -> a shr b
                        else -> throw IllegalArgumentException("Invalid operator: $token")
                    }
                    stack.push(result)
                }
            }
        }
        return stack.pop()
    }

    // Checks for invalid usage of the NOT operator
    private fun validateExpression(tokens: List<String>): String? {
        for (i in tokens.indices) {
            val token = tokens[i]

            if (token == "~" && i == tokens.size - 1) {
                return "Invalid placement: '~'"
            }
            if (token == "~" && i > 0 && (tokens[i - 1].matches(Regex("\\d+")) || tokens[i - 1] == ")")) {
                return "Invalid placement: '~'"
            }
            if (i > 0) {
                val prev = tokens[i - 1]
                if ((prev == ")" && token == "(") ||
                    (prev.matches(Regex("\\d+")) && token == "(") ||
                    (prev == ")" && token.matches(Regex("\\d+")))
                ) {
                    return "Missing operator between parentheses"
                }
            }
        }
        return null
    }





}


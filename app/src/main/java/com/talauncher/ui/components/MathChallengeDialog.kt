package com.talauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.talauncher.data.model.MathDifficulty
import kotlin.random.Random

data class MathProblem(
    val question: String,
    val answer: Int
)

object MathGenerator {
    fun generateProblem(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> generateEasyProblem()
            MathDifficulty.MEDIUM -> generateMediumProblem()
            MathDifficulty.HARD -> generateHardProblem()
        }
    }

    private fun generateEasyProblem(): MathProblem {
        val a = Random.nextInt(1, 20)
        val b = Random.nextInt(1, 20)
        val operation = Random.nextInt(0, 2)

        return when (operation) {
            0 -> MathProblem("$a + $b", a + b)
            else -> {
                val larger = maxOf(a, b)
                val smaller = minOf(a, b)
                MathProblem("$larger - $smaller", larger - smaller)
            }
        }
    }

    private fun generateMediumProblem(): MathProblem {
        val a = Random.nextInt(10, 50)
        val b = Random.nextInt(10, 50)
        val operation = Random.nextInt(0, 3)

        return when (operation) {
            0 -> MathProblem("$a + $b", a + b)
            1 -> {
                val larger = maxOf(a, b)
                val smaller = minOf(a, b)
                MathProblem("$larger - $smaller", larger - smaller)
            }
            else -> {
                val smaller = Random.nextInt(2, 12)
                val product = Random.nextInt(2, 12)
                val result = smaller * product
                MathProblem("$result ÷ $smaller", product)
            }
        }
    }

    private fun generateHardProblem(): MathProblem {
        val a = Random.nextInt(50, 200)
        val b = Random.nextInt(50, 200)
        val operation = Random.nextInt(0, 4)

        return when (operation) {
            0 -> MathProblem("$a + $b", a + b)
            1 -> {
                val larger = maxOf(a, b)
                val smaller = minOf(a, b)
                MathProblem("$larger - $smaller", larger - smaller)
            }
            2 -> {
                val smaller = Random.nextInt(12, 25)
                val product = Random.nextInt(12, 25)
                val result = smaller * product
                MathProblem("$result ÷ $smaller", product)
            }
            else -> {
                val base = Random.nextInt(2, 10)
                val exponent = Random.nextInt(2, 4)
                val result = when (exponent) {
                    2 -> base * base
                    3 -> base * base * base
                    else -> base * base
                }
                MathProblem("$base^$exponent", result)
            }
        }
    }
}

@Composable
fun MathChallengeDialog(
    difficulty: MathDifficulty = MathDifficulty.EASY,
    onCorrect: () -> Unit,
    onDismiss: () -> Unit,
    isTimeExpired: Boolean = false  // If true, this is a mandatory challenge due to time expiration
) {
    val problem = remember(difficulty) { MathGenerator.generateProblem(difficulty) }
    var answerText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    LaunchedEffect(problem, difficulty) {
        answerText = ""
        isError = false
        errorMessage = ""
    }
    var timeLeft by remember { mutableStateOf(if (isTimeExpired) 30 else 0) }  // 30 seconds countdown for expired sessions

    // Auto-dismiss timer for expired sessions
    LaunchedEffect(isTimeExpired) {
        if (isTimeExpired) {
            while (timeLeft > 0) {
                kotlinx.coroutines.delay(1000) // Wait 1 second
                timeLeft--
            }
            // Time's up - automatically dismiss
            onDismiss()
        }
    }

    // Prevent back button from dismissing the dialog
    BackHandler {
        // Do nothing - force user to solve the math problem
    }

    Dialog(
        onDismissRequest = {
            // Prevent dismissing by clicking outside
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTimeExpired) "Time's up!" else "Solve to continue",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = if (isTimeExpired)
                        "Your time limit has expired. Solve this math problem to continue using the app, or it will close automatically."
                    else
                        "Complete this math problem to close the app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = if (isTimeExpired) 12.dp else 24.dp)
                )

                // Show countdown timer for expired sessions
                if (isTimeExpired && timeLeft > 0) {
                    Text(
                        text = "Auto-close in: ${timeLeft}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = problem.question,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                OutlinedTextField(
                    value = answerText,
                    onValueChange = {
                        answerText = it
                        isError = false
                    },
                    label = { Text("Your answer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isTimeExpired) {
                    // For expired sessions, only show submit button (no cancel option)
                    Button(
                        onClick = {
                            val userAnswer = answerText.toIntOrNull()
                            if (userAnswer == null) {
                                isError = true
                                errorMessage = "Please enter a valid number"
                            } else if (userAnswer == problem.answer) {
                                onCorrect()
                            } else {
                                isError = true
                                errorMessage = "Incorrect answer, try again"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit")
                    }
                } else {
                    // For non-expired sessions, show both cancel and submit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val userAnswer = answerText.toIntOrNull()
                                if (userAnswer == null) {
                                    isError = true
                                    errorMessage = "Please enter a valid number"
                                } else if (userAnswer == problem.answer) {
                                    onCorrect()
                                } else {
                                    isError = true
                                    errorMessage = "Incorrect answer, try again"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}
package test

import movegen.MoveGenerator
import model.BitboardState
import model.FenParser
import java.io.File

object MoveGenerationTester {

    private const val INPUT_DIR = "src/test/testcases_fen"

    fun runTest() {
        val testFiles = File(INPUT_DIR).listFiles { file -> file.extension == "txt" } ?: return
        for (file in testFiles) {
            println(" Testowanie pliku: ${file.name}")
            val lines = file.readLines()

            var currentFen: String? = null
            val expectedPositions = mutableSetOf<String>()
            val generatedPositions = mutableSetOf<String>()

            for (line in lines) {
                when {
                    line.startsWith("START:") -> {
                        if (currentFen != null) {
                            comparePositions(currentFen, expectedPositions, generatedPositions)
                        }
                        currentFen = line.removePrefix("START: ").trim()
                        expectedPositions.clear()
                        generatedPositions.clear()
                    }
                    line.startsWith("EXPECTED:") -> {
                        expectedPositions.add(line.removePrefix("EXPECTED: ").trim())
                    }
                }
            }

            if (currentFen != null) {
                comparePositions(currentFen, expectedPositions, generatedPositions)
            }
        }
    }

    private fun comparePositions(startFen: String, expected: Set<String>, generated: MutableSet<String>) {
        val state = FenParser.fenToBitboard(startFen)
        val moves = MoveGenerator.generateLegalMoves(state)

        for (move in moves) {
            val newState = state.applyMove(move)
            val newFen = FenParser.bitboardStateToFen(newState)
            generated.add(newFen)
        }

        val missing = expected - generated
        val extra = generated - expected

        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            println("\n Błąd w generowaniu ruchów dla pozycji: $startFen")

            if (missing.isNotEmpty()) {
                println("Brakujące pozycje:")
                missing.forEach { println("   -> $it") }
            }

            if (extra.isNotEmpty()) {
                println("Nadmiarowe pozycje:")
                extra.forEach { println("   -> $it") }
            }
        } else {
            println("Wszystkie ruchy poprawne dla: $startFen")
        }
    }
}

fun main() {
    MoveGenerationTester.runTest()
}
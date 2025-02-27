package test

import movegen.MoveGenerator
import model.FenParser
import java.io.File

object MoveUndoTester {

    private const val INPUT_DIR = "src/test/testcases_fen"

    fun runTest() {
        val testFiles = File(INPUT_DIR).listFiles { file -> file.extension == "txt" } ?: return
        for (file in testFiles) {
            println("Testing file: ${file.name}")
            val lines = file.readLines()

            var currentFen: String? = null

            for (line in lines) {
                if (line.startsWith("START:")) {
                    currentFen = line.removePrefix("START: ").trim()
                    testMoveUndo(currentFen)
                }
            }
        }
    }

    private fun testMoveUndo(startFen: String) {
        val state = FenParser.fenToBitboard(startFen)
        val moves = MoveGenerator.generateLegalMoves(state)

        for (move in moves) {
            state.makeMove(move)
            val newFen = FenParser.bitboardStateToFen(state)

            state.unmakeMove()
            val undoFen = FenParser.bitboardStateToFen(state)

            if (undoFen != startFen) {
                println("\n ERROR: Wrong FEN after undoing move!")
                println("   Initial FEN: $startFen")
                println("   Move: $move")
                println("   FEN after move: $newFen")
                println("   FEN after undoing : $undoFen\n")
            }
        }
    }
}

fun main() {
    MoveUndoTester.runTest()
}
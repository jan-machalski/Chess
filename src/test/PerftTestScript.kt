package test

import movegen.*
import model.*
import java.util.LinkedList
import java.io.File

data class PositionNode(
    val state: BitboardState,
    val ply: Int
)

fun generatePositionsUpToDepth(fen: String, maxDepth: Int,outputFile: String) {
    val startState = FenParser.fenToBitboard(fen)
    val queue = LinkedList<PositionNode>()
    val file = File(outputFile)
    //file.writeText("")
    var maxLength = 0

    queue.add(PositionNode(startState, 0))

    while (queue.isNotEmpty()) {
        val node = queue.poll()
        val ply = node.ply

        val moves = MoveGenerator.generateLegalMoves(node.state)

        if(ply < maxDepth){
            for (move in moves) {
                val newState = node.state.applyMove(move)
                queue.add(PositionNode(newState, ply + 1))
            }
        }
        else{
            if(maxLength == 0) {
                maxLength = queue.size
                print("Positions left to process: ${queue.size}/${maxLength}")
            }
            else{
                print("\rPositions left to process: ${queue.size}/${maxLength}")
            }

            file.appendText("NEW\n")
            file.appendText("${FenParser.bitboardStateToFen(node.state)}\n")
            for(move in moves) {
                val newState = node.state.applyMove(move)
                file.appendText("${FenParser.bitboardStateToFen(newState)}\n")
            }

        }
    }
}


fun main(){
    /*val fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
    generatePositionsUpToDepth(fen,4,"perf_debug.txt")*/

    val fen_initial_board = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    var expectedResults = listOf(20L, 400L, 8902L, 197281L, 4865609L, 119060324L)
    val perft_initial_board = PerftTest(fen_initial_board, expectedResults, 6)
    perft_initial_board.run()

    val fen_midgame = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
    expectedResults = listOf(48L,2039L,97862L,4085603L,193690690L)
    val perft_midgame = PerftTest(fen_midgame, expectedResults, 5)
    perft_midgame.run()

    val fen_endgame = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
    expectedResults = listOf(14L,191L,2812L,43238L,674624L,11030083L,178633661L)
    val perft_endgame = PerftTest(fen_endgame, expectedResults, 7)
    perft_endgame.run()


}
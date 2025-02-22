package test

import model.*
import movegen.MoveGenerator
import kotlin.system.measureTimeMillis

class PerftTest (private val fen: String, private val expectedResults: List<Long>, private val depth: Int){

    fun run(){
        println("Running Perft Test for FEN: $fen")
        val state =  FenParser.fenToBitboard(fen)

        for(d in 1..depth){
            var nodes: Long
            val timeTaken = measureTimeMillis {
                nodes = perft(state,d)
            }
            println("Depth: $d, Nodes: $nodes, Expected: ${expectedResults[d-1]} TimeTaken: ${timeTaken}ms")

            if(nodes != expectedResults[d-1]){
                println("Test failed at dpeth $d")
                return
            }
        }
        println("All depths passed succesfully")
    }
    private fun perft(state: BitboardState, depth: Int): Long {
        if(depth == 0) return 1

        val moves = MoveGenerator.generateLegalMoves(state)
        if(depth == 1)  return moves.size.toLong()

        var nodes = 0L
        for(move in moves){
            val newState = state.applyMove(move)
            nodes += perft(newState, depth - 1)
        }
        return nodes
    }

}
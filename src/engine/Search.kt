package engine

import model.BitboardState
import model.Move
import movegen.MoveGenerator
import movegen.BitboardAnalyzer

object Search {

    fun findBestMove(state:BitboardState, depth: Int): Move? {
        val moves = MoveGenerator.generateLegalMoves(state)
        if(moves.isEmpty()) return null

        MoveOrdering.sortMoves(moves,state)

        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE + 1
        val beta = Int.MAX_VALUE - 1

        for (move in moves) {
            state.makeMove(move)
            val score = -alphaBeta(state,depth - 1, -beta, -alpha)
            state.unmakeMove()

            if(score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = maxOf(alpha,bestScore)
        }
        return bestMove
    }

    private fun alphaBeta(state: BitboardState, depth:Int, alpha:Int, beta: Int):Int{
        if(depth == 0){
            return Evaluation.evaluate(state)
        }

        val moves = MoveGenerator.generateLegalMoves(state)
        if(moves.isEmpty()){
            return if(BitboardAnalyzer.getChecks(state) != 0uL) Int.MIN_VALUE + 1 else 0
        }

       MoveOrdering.sortMoves(moves,state)

        var alphaVar = alpha
        for(move in moves){
            state.makeMove(move)
            val score = -alphaBeta(state,depth - 1, -beta, -alphaVar)
            state.unmakeMove()

            if(score >= beta) return beta
            alphaVar = maxOf(alphaVar,score)
        }
        return alphaVar
    }

}
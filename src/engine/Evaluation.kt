package engine

import model.Move
import model.BitboardState

object Evaluation {
    internal const val PAWN_VALUE = 100
    internal const val ROOK_VALUE = 500
    internal const val KNIGHT_VALUE = 300
    internal const val BISHOP_VALUE = 320
    internal const val QUEEN_VALUE = 900

    fun evaluate(state:BitboardState): Int{
        var score = 0

        score += materialBalance(state)

        return score
    }

    private fun materialBalance(state: BitboardState): Int{
        val whiteMaterial = countMaterial(state,true)
        val blackMaterial = countMaterial(state, false)

        return if(state.whiteToMove) whiteMaterial - blackMaterial else blackMaterial - whiteMaterial
    }

    private fun countMaterial(state: BitboardState, evaluateWhite: Boolean): Int{
        val ourPieces = if(evaluateWhite) state.whitePieces else state.blackPieces
        return (ourPieces and state.pawns).countOneBits() * PAWN_VALUE +
                (ourPieces and state.rooks).countOneBits() * ROOK_VALUE +
                (ourPieces and state.bishops).countOneBits() * BISHOP_VALUE +
                (ourPieces and state.knights).countOneBits() * KNIGHT_VALUE +
                (ourPieces and state.queens).countOneBits() * QUEEN_VALUE

    }

}
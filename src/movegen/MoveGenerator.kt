package movegen

import model.BitboardState
import model.Move

object MoveGenerator {

    fun generateLegalMoves(state: BitboardState): MutableList<Move> {
        val legalMoves = mutableListOf<Move>()

        val isWhite = state.whiteToMove
        val kingPos = (state.kings and if(isWhite) state.whitePieces else state.blackPieces).countTrailingZeroBits()

        val checkingFigures = BitboardAnalyzer.getChecks(state,kingPos)
        val checkingFiguresCount = checkingFigures.countOneBits()

        KingMoves.generateKingMoves(state,legalMoves)

        if(checkingFiguresCount > 1)
            return legalMoves

        val pinnedPieces = BitboardAnalyzer.getPinnedPieces(state)

        val checkingFigurePos = checkingFigures.countTrailingZeroBits()

        KnightMoves.generateKnightMoves(state,pinnedPieces,legalMoves,checkingFigurePos)
        RookMoves.generateRookMoves(state,pinnedPieces,legalMoves,checkingFigurePos)
        BishopMoves.generateBishopMoves(state,pinnedPieces,legalMoves,checkingFigurePos)
        PawnMoves.generatePawnMoves(state,pinnedPieces,legalMoves,checkingFigurePos)


        return legalMoves
    }


}
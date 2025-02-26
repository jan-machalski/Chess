package movegen

import model.BitboardState
import model.Move

object MoveGenerator {
    private const val RANK_4 = 0x00000000FF000000uL
    private const val RANK_5 = 0x000000FF00000000uL
    private val KING_CASTLE_MOVES = listOf(2, 6, 58, 62)

    fun generateLegalMoves(state: BitboardState): List<Move> {
        val legalMoves = mutableListOf<Move>()

        val isWhite = state.whiteToMove
        val kingPos = (state.kings and if(isWhite) state.whitePieces else state.blackPieces).countTrailingZeroBits()

        val checkingFigures = BitboardAnalyzer.getChecks(state,kingPos)
        val checkingFiguresCount = checkingFigures.countOneBits()

        KingMoves.generateKingMoves(state,legalMoves)

        if(checkingFiguresCount > 1)
            return legalMoves

        val pseudoLegalMoves = mutableListOf<Move>()
        pseudoLegalMoves.addAll(KnightMoves.generateKnightMoves(state))
        pseudoLegalMoves.addAll(RookMoves.generateRookMoves(state))
        pseudoLegalMoves.addAll(BishopMoves.generateBishopMoves(state))
        pseudoLegalMoves.addAll(PawnMoves.generatePawnMoves(state))

        val checkingFigurePos = checkingFigures.countTrailingZeroBits()
        val enPassantTargetPos = state.enPassantTarget.countTrailingZeroBits()

        val pinnedPieces = BitboardAnalyzer.getPinnedPieces(state)
        var potentialCapturedPiece:Int

        for(move in pseudoLegalMoves) {
            potentialCapturedPiece = move.to
            if((BitboardAnalyzer.SINGLE_BIT_MASKS[move.from]) and pinnedPieces != 0uL){
                if(!BitboardAnalyzer.PINNED_MOVES_LOOKUP[kingPos][move.from][move.to])
                    continue
            }
            if(move.to == enPassantTargetPos && move.pieceType == Move.PIECE_PAWN){
                potentialCapturedPiece = if(isWhite) move.to - 8 else move.to + 8
                if(!isEnPassantValid(state,move))
                    continue
            }
            if(checkingFiguresCount == 1){
                if(!(BitboardAnalyzer.BLOCK_MOVES_LOOKUP[kingPos][checkingFigurePos][move.to] || BitboardAnalyzer.BLOCK_MOVES_LOOKUP[kingPos][checkingFigurePos][potentialCapturedPiece]))
                    continue
            }

            legalMoves.add(move)
        }
        return legalMoves
    }



    private fun filterKingMoves(state: BitboardState, moves:List<Move>): List<Move> {
        val legalMoves = mutableListOf<Move>()
        val attackedFieldsMask = BitboardAnalyzer.getAttackedFields(state)

        for (move in moves) {
            if (isCastleMove(state, move)) {
                var isValidCastle = true
                val range = if (move.to < move.from) move.to..move.from else move.from..move.to

                for (pos in range) {
                    if (BitboardAnalyzer.SINGLE_BIT_MASKS[pos] and attackedFieldsMask != 0uL) {
                        isValidCastle = false
                        break
                    }
                }

                if (isValidCastle) {
                    legalMoves.add(move)
                }
            }
            else if(BitboardAnalyzer.SINGLE_BIT_MASKS[move.to] and attackedFieldsMask == 0uL)
                legalMoves.add(move)
        }
        return legalMoves
    }



    private fun isCastleMove(state: BitboardState, move: Move): Boolean {
        val kingStartPos = if (state.whiteToMove) 4 else 60

        return move.from == kingStartPos && move.to in KING_CASTLE_MOVES
    }

    private fun isEnPassantValid(state: BitboardState, move: Move): Boolean {
        val ourPieces = if(state.whiteToMove) state.whitePieces else state.blackPieces
        val opponentPieces = if(state.whiteToMove) state.blackPieces else state.whitePieces
        val rankToCheck = if(state.whiteToMove) RANK_5 else RANK_4
        val attackingOurPawn = RookMoves.getAttackedFieldsMask(state,move.from) and rankToCheck
        val attackingOpponentsPawn = RookMoves.getAttackedFieldsMask(state,if(state.whiteToMove) move.to - 8 else move.to+8) and rankToCheck

        if((attackingOurPawn and ourPieces and state.kings != 0uL && attackingOpponentsPawn and opponentPieces and (state.rooks or state.queens) != 0uL) ||
            (attackingOpponentsPawn and ourPieces and state.kings != 0uL && attackingOurPawn and opponentPieces and (state.rooks or state.queens) != 0uL))
            return false
        return true
    }

}
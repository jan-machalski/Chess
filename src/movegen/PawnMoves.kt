package movegen
import model.Move
import model.BitboardState

object PawnMoves {
    private const val RANK_2: ULong = 0x000000000000FF00uL
    private const val RANK_7: ULong = 0x00FF000000000000uL
    private const val FILE_A: ULong = 0x0101010101010101uL
    private const val FILE_H: ULong = 0x8080808080808080uL

    val PAWN_ATTACKS_WHITE = precomputePawnAttacks(isWhite = true)
    val PAWN_ATTACKS_BLACK = precomputePawnAttacks(isWhite = false)

    fun generatePawnMoves(state: BitboardState): List<Move> {
        val moves = mutableListOf<Move>()
        val isWhite = state.whiteToMove
        val ourPawns = (if(isWhite) state.whitePieces else state.blackPieces) and state.pawns
        val enemyPieces = if (isWhite) state.blackPieces else state.whitePieces
        val emptySquares = (state.whitePieces or state.blackPieces).inv()

        val shift = if(isWhite) 8 else -8
        val leftCaptureShift = if(isWhite) 7 else -9
        val rightCaptureShift = if(isWhite) 9 else -7

        var singlePushes = if (isWhite) (ourPawns shl shift) and emptySquares else (ourPawns shr -shift) and emptySquares
        while(singlePushes != 0uL){
            val to = singlePushes.countTrailingZeroBits()
            val from = to - shift
            addPromotionMoves(moves, from, to,isWhite)
            singlePushes = singlePushes and (singlePushes - 1uL)
        }

        var doublePushes = if (isWhite) {
            ((ourPawns and RANK_2) shl shift) and emptySquares
        } else {
            ((ourPawns and RANK_7) shr -shift) and emptySquares
        }
        doublePushes = emptySquares and if(isWhite) doublePushes shl shift else doublePushes shr -shift
        while(doublePushes != 0uL){
            val to = doublePushes.countTrailingZeroBits()
            val from = to - shift - shift
            moves.add(Move.create(from,to,Move.PIECE_PAWN))
            doublePushes = doublePushes and (doublePushes - 1uL)
        }

        generateCaptures(moves, ourPawns, enemyPieces, leftCaptureShift, FILE_A.inv(), isWhite)
        generateCaptures(moves, ourPawns, enemyPieces, rightCaptureShift, FILE_H.inv(), isWhite)

        if (state.enPassantTarget != 0uL) {
            generateCaptures(moves, ourPawns, state.enPassantTarget, leftCaptureShift, FILE_A.inv(), isWhite)
            generateCaptures(moves, ourPawns, state.enPassantTarget, rightCaptureShift, FILE_H.inv(), isWhite)
        }

        return moves
    }
    private fun generateCaptures(
        moves: MutableList<Move>,
        pawns: ULong,
        target: ULong,
        shift: Int,
        fileMask: ULong,
        isWhite: Boolean
    ) {
        var captures = if (isWhite) (pawns and fileMask) shl shift and target else (pawns and fileMask) shr -shift and target
        while (captures != 0uL) {
            val to = captures.countTrailingZeroBits()
            val from = to - shift
            addPromotionMoves(moves, from, to, isWhite)
            captures = captures and (captures - 1uL)
        }
    }

    private fun addPromotionMoves(moves: MutableList<Move>, from: Int, to: Int, isWhite: Boolean) {
        val isPromotion = (isWhite && to in 56..63) || (!isWhite && to in 0..7)
        if (isPromotion) {
            moves.add(Move.create(from, to,Move.PIECE_PAWN, Move.PROMOTION_QUEEN))
            moves.add(Move.create(from, to,Move.PIECE_PAWN, Move.PROMOTION_ROOK))
            moves.add(Move.create(from, to,Move.PIECE_PAWN, Move.PROMOTION_BISHOP))
            moves.add(Move.create(from, to,Move.PIECE_PAWN, Move.PROMOTION_KNIGHT))
        } else {
            moves.add(Move.create(from, to,Move.PIECE_PAWN))
        }
    }

    private fun precomputePawnAttacks(isWhite: Boolean): Array<ULong> {
        val attacks = Array(64) { 0uL }

        for (square in 0 until 64) {
            val bitboard = 1uL shl square
            var attackMoves = 0uL

            if (isWhite) {

                if ((bitboard and FILE_A) == 0uL) attackMoves = attackMoves or (bitboard shl 7)
                if ((bitboard and FILE_H) == 0uL) attackMoves = attackMoves or (bitboard shl 9)
            } else {
                // Czarne pionki atakują w dół
                if ((bitboard and FILE_A) == 0uL) attackMoves = attackMoves or (bitboard shr 9)
                if ((bitboard and FILE_H) == 0uL) attackMoves = attackMoves or (bitboard shr 7)
            }

            attacks[square] = attackMoves
        }
        return attacks
    }
}
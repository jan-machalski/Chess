package movegen

import model.Move
import model.BitboardState

object PawnMoves {
    private const val RANK_2: ULong = 0x000000000000FF00uL
    private const val RANK_4: ULong = 0x00000000FF000000uL
    private const val RANK_5: ULong = 0x000000FF00000000uL
    private const val RANK_7: ULong = 0x00FF000000000000uL
    private const val FILE_A: ULong = 0x0101010101010101uL
    private const val FILE_H: ULong = 0x8080808080808080uL

    internal val PAWN_ATTACKS_WHITE = precomputePawnAttacks(isWhite = true)
    internal val PAWN_ATTACKS_BLACK = precomputePawnAttacks(isWhite = false)

    fun generatePawnMoves(state: BitboardState,pinnedPieces: ULong,moves:MutableList<Move>,checkingFigurePos:Int) {
        val isWhite = state.whiteToMove
        val ourPieces = if(isWhite) state.whitePieces else state.blackPieces
        val ourPawns = ourPieces and state.pawns
        val ourKingPos = (state.kings and ourPieces).countTrailingZeroBits()
        val enemyPieces = if (isWhite) state.blackPieces else state.whitePieces
        val emptySquares = (state.whitePieces or state.blackPieces).inv()

        val shift = if(isWhite) 8 else -8
        val leftCaptureShift = if(isWhite) 7 else -9
        val rightCaptureShift = if(isWhite) 9 else -7

        var singlePushes = if (isWhite) (ourPawns shl shift) and emptySquares else (ourPawns shr -shift) and emptySquares
        while(singlePushes != 0uL){
            val to = singlePushes.countTrailingZeroBits()
            val from = to - shift
            val isPinned = (BitboardAnalyzer.SINGLE_BIT_MASKS[from] and pinnedPieces) != 0uL
            if(BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][to] &&
                (!isPinned || BitboardAnalyzer.PINNED_MOVES_LOOKUP[ourKingPos][from][to])) {
                addPromotionMoves(moves, from, to, isWhite,state.getPieceAt(to))
            }
            singlePushes = singlePushes xor BitboardAnalyzer.SINGLE_BIT_MASKS[to]
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
            val isPinned = (BitboardAnalyzer.SINGLE_BIT_MASKS[from] and pinnedPieces) != 0uL
            if(BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][to] &&
                (!isPinned || BitboardAnalyzer.PINNED_MOVES_LOOKUP[ourKingPos][from][to])) {
                moves.add(Move.create(from, to, Move.PIECE_PAWN,Move.PIECE_NONE))
            }
            doublePushes = doublePushes xor BitboardAnalyzer.SINGLE_BIT_MASKS[to]
        }

        generateCaptures(moves, ourPawns, enemyPieces, leftCaptureShift, FILE_A.inv(), isWhite, ourKingPos, checkingFigurePos,pinnedPieces, false,state)
        generateCaptures(moves, ourPawns, enemyPieces, rightCaptureShift, FILE_H.inv(), isWhite, ourKingPos, checkingFigurePos,pinnedPieces, false,state)

        if (state.enPassantTarget != 0uL) {
            generateCaptures(moves, ourPawns, state.enPassantTarget, leftCaptureShift, FILE_A.inv(), isWhite, ourKingPos, checkingFigurePos,pinnedPieces, true,state)
            generateCaptures(moves, ourPawns, state.enPassantTarget, rightCaptureShift, FILE_H.inv(), isWhite, ourKingPos, checkingFigurePos,pinnedPieces, true,state)
        }
    }
    private fun generateCaptures(
        moves: MutableList<Move>,
        pawns: ULong,
        target: ULong,
        shift: Int,
        fileMask: ULong,
        isWhite: Boolean,
        ourKingPos: Int,
        checkingFigurePos: Int,
        pinnedPieces: ULong,
        isEnPassant: Boolean,
        state: BitboardState
    ) {
        var captures = if (isWhite) (pawns and fileMask) shl shift and target else (pawns and fileMask) shr -shift and target
        while (captures != 0uL) {
            val to = captures.countTrailingZeroBits()
            val from = to - shift
            val isPinned = (BitboardAnalyzer.SINGLE_BIT_MASKS[from] and pinnedPieces) != 0uL
            val capturedPiece = if(isEnPassant) if(isWhite) to - 8 else to + 8 else to

            if((BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][to] || BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][capturedPiece]) &&
                (!isPinned || BitboardAnalyzer.PINNED_MOVES_LOOKUP[ourKingPos][from][to])) {

                if(!isEnPassant || isEnPassantValid(state,from,to))
                    addPromotionMoves(moves, from, to, isWhite,state.getPieceAt(to))
            }
            captures = captures xor BitboardAnalyzer.SINGLE_BIT_MASKS[to]
        }
    }

    private fun addPromotionMoves(moves: MutableList<Move>, from: Int, to: Int, isWhite: Boolean,capturedPieceType: Int) {
        val isPromotion = (isWhite && to in 56..63) || (!isWhite && to in 0..7)
        if (isPromotion) {
            moves.add(Move.create(from, to,Move.PIECE_PAWN,capturedPieceType, Move.PIECE_QUEEN))
            moves.add(Move.create(from, to,Move.PIECE_PAWN,capturedPieceType, Move.PIECE_ROOK))
            moves.add(Move.create(from, to,Move.PIECE_PAWN,capturedPieceType, Move.PIECE_BISHOP))
            moves.add(Move.create(from, to,Move.PIECE_PAWN,capturedPieceType, Move.PIECE_KNIGHT))
        } else {
            moves.add(Move.create(from, to,Move.PIECE_PAWN,capturedPieceType))
        }
    }

    private fun isEnPassantValid(state: BitboardState, from:Int, to:Int): Boolean {
        val ourPieces = if(state.whiteToMove) state.whitePieces else state.blackPieces
        val opponentPieces = if(state.whiteToMove) state.blackPieces else state.whitePieces
        val rankToCheck = if(state.whiteToMove) RANK_5 else RANK_4
        val attackingOurPawn = RookMoves.getAttackedFieldsMask(state,from) and rankToCheck
        val attackingOpponentsPawn = RookMoves.getAttackedFieldsMask(state,if(state.whiteToMove) to - 8 else to+8) and rankToCheck

        if((attackingOurPawn and ourPieces and state.kings != 0uL && attackingOpponentsPawn and opponentPieces and (state.rooks or state.queens) != 0uL) ||
            (attackingOpponentsPawn and ourPieces and state.kings != 0uL && attackingOurPawn and opponentPieces and (state.rooks or state.queens) != 0uL))
            return false
        return true
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
                if ((bitboard and FILE_A) == 0uL) attackMoves = attackMoves or (bitboard shr 9)
                if ((bitboard and FILE_H) == 0uL) attackMoves = attackMoves or (bitboard shr 7)
            }

            attacks[square] = attackMoves
        }
        return attacks
    }
}
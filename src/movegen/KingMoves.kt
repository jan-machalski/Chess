package movegen

import model.Move
import model.BitboardState

object KingMoves {
    val KING_MOVES = precomputeKingMoves()

    fun generateKingMoves(state: BitboardState): List<Move>{
        val moves = mutableListOf<Move>()
        val isWhite = state.whiteToMove
        val king = if(isWhite) state.kings and state.whitePieces else state.kings and state.blackPieces
        val ourPieces = if(isWhite) state.whitePieces else state.blackPieces

        if(king == 0uL) return moves

        val from = king.countTrailingZeroBits()
        var possibleMoves = KING_MOVES[from] and ourPieces.inv()

        while(possibleMoves != 0uL){
            val to = possibleMoves.countTrailingZeroBits()
            moves.add(Move.create(from,to,Move.PIECE_KING))
            possibleMoves = possibleMoves and (possibleMoves - 1uL)
        }

        generateCastlingMoves(state,moves)

        return moves
    }

    private fun generateCastlingMoves(state: BitboardState, moves: MutableList<Move>){
        val isWhite = state.whiteToMove
        val castlingRights = state.castlingRights
        val emptySquares = (state.whitePieces or state.blackPieces).inv()
        val ourRooks = state.rooks and if(isWhite) state.whitePieces else state.blackPieces

        if(isWhite) {
            if ((castlingRights and 0b01) != 0 && (emptySquares and 0x0000000000000060uL) == 0x0000000000000060uL && (ourRooks and 0x0000000000000080uL) != 0uL) {
                moves.add(Move.create(4, 6,Move.PIECE_KING))
            }
            if((castlingRights and 0b10) != 0 && (emptySquares and 0x000000000000000EuL) == 0x000000000000000EuL && (ourRooks and 0x0000000000000001uL) != 0uL) {
                moves.add(Move.create(4,2,Move.PIECE_KING))
            }
        }
        else{
            if((castlingRights and 0b100) != 0 && (emptySquares and 0x6000000000000000uL) == 0x6000000000000000uL && (ourRooks and 0x8000000000000000uL) != 0uL) {
                moves.add(Move.create(60, 62,Move.PIECE_KING))
            }
            if((castlingRights and 0b1000) != 0 && (emptySquares and 0x0E00000000000000uL) == 0x0E00000000000000uL && (ourRooks and 0x0100000000000000uL) != 0uL) {
                moves.add(Move.create(60, 58,Move.PIECE_KING))
            }
        }

    }

    private fun precomputeKingMoves(): Array<ULong>{
        val moves = Array(64){0uL}
        for(square in 0 until 64){
            moves[square] = generateKingBitboard(square)
        }
        return moves
    }
    private fun generateKingBitboard(square: Int): ULong{
        val bitboard = 1uL shl square
        var moves = 0uL

        val notAFile = 0xFEFEFEFEFEFEFEFEuL
        val notHFile = 0x7F7F7F7F7F7F7F7FuL

        moves = moves or (bitboard shl 8)
        moves = moves or (bitboard shr 8)
        moves = moves or (((bitboard and notAFile) shl 1) )
        moves = moves or (((bitboard and notHFile) shr 1) )
        moves = moves or (((bitboard and notHFile) shl 9) )
        moves = moves or (((bitboard and notAFile) shl 7) )
        moves = moves or (((bitboard and notAFile) shr 9) )
        moves = moves or (((bitboard and notHFile) shr 7) )

        return moves
    }
}
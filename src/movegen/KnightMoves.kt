package movegen

import model.Move
import model.BitboardState

object KnightMoves {
    val KNIGHT_MOVES = precomputeKnightMoves()

    fun generateKnightMoves(state: BitboardState, pinnedPieces:ULong, moves:MutableList<Move>,checkingFigurePos:Int){

        val isWhite = state.whiteToMove
        var knights = pinnedPieces.inv() and if(isWhite) state.knights and state.whitePieces else state.knights and state.blackPieces
        val ourPieces = if(isWhite) state.whitePieces else state.blackPieces
        val ourKingPos = (ourPieces and state.kings).countTrailingZeroBits()
        val availableSquares = ourPieces.inv()


        while(knights != 0uL){
            val from = knights.countTrailingZeroBits()
            var possibleMoves = KNIGHT_MOVES[from] and availableSquares

            while(possibleMoves != 0uL){
                val to = possibleMoves.countTrailingZeroBits()
                if(BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][to])
                    moves.add(Move.create(from,to,Move.PIECE_KNIGHT))
                possibleMoves = possibleMoves and (possibleMoves - 1u)
            }
            knights = knights and (knights - 1u)
        }
    }

    private fun precomputeKnightMoves(): Array<ULong>{
        val moves = Array(64){0uL}
        for(square in 0..<64){
            moves[square] = generateKnightBitboard(square)
        }
        return moves
    }
    private fun generateKnightBitboard(square:Int):ULong{
        val bitboard = 1uL shl square
        var moves = 0uL

        val notAFile = 0xFEFEFEFEFEFEFEFEuL
        val notABFile = 0xFCFCFCFCFCFCFCFCuL
        val notHFile = 0x7F7F7F7F7F7F7F7FuL
        val notGHFile = 0x3F3F3F3F3F3F3F3FuL

        moves = moves or((bitboard shl 17) and notAFile)
        moves = moves or ((bitboard shl 10) and notABFile)
        moves = moves or ((bitboard shr 6) and notABFile)
        moves = moves or ((bitboard shr 15) and notAFile)
        moves = moves or ((bitboard shr 17) and notHFile)
        moves = moves or ((bitboard shr 10) and notGHFile)
        moves = moves or ((bitboard shl 6) and notGHFile)
        moves = moves or ((bitboard shl 15) and notHFile)

        return moves
    }
}
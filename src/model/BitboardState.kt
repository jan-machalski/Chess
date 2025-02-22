package model

data class BitboardState(
    var pawns: ULong = 0uL,
    var knights: ULong = 0uL,
    var bishops: ULong = 0uL,
    var rooks: ULong = 0uL,
    var queens: ULong = 0uL,
    var kings: ULong = 0uL,

    var whitePieces: ULong = 0uL,
    var blackPieces: ULong = 0uL,

    var whiteToMove: Boolean = true,
    var castlingRights: Int = 0b1111,
    var enPassantTarget: ULong = 0uL,
    var halfMoveClock: Int = 0,
    var fullMoveNumber: Int = 1
){
    fun applyMove(move: Move): BitboardState {
        val newState = this.copy()

        val fromMask = 1uL shl move.from
        val toMask = 1uL shl move.to
        val moveMask = fromMask or toMask
        val isWhite = this.whiteToMove
        val opponentPieces = if(isWhite) this.blackPieces else this.whitePieces
        var isCapture = false

        // capture
        if(opponentPieces and toMask != 0uL){
            isCapture = true
            val invMask = toMask.inv()
            newState.pawns = newState.pawns and invMask
            newState.knights = newState.knights and invMask
            newState.bishops = newState.bishops and invMask
            newState.rooks = newState.rooks and invMask
            newState.queens = newState.queens and invMask
            if(isWhite)
                newState.blackPieces = newState.blackPieces xor toMask
            else
                newState.whitePieces = newState.whitePieces xor toMask
        }

        // move
        when(move.pieceType) {
            Move.PIECE_PAWN -> newState.pawns = newState.pawns xor moveMask
            Move.PIECE_KNIGHT -> newState.knights = newState.knights xor moveMask
            Move.PIECE_BISHOP -> newState.bishops = newState.bishops xor moveMask
            Move.PIECE_ROOK -> newState.rooks = newState.rooks xor moveMask
            Move.PIECE_QUEEN -> newState.queens = newState.queens xor moveMask
            Move.PIECE_KING -> newState.kings = newState.kings xor moveMask
        }
        if(isWhite){
            newState.whitePieces = newState.whitePieces xor moveMask
        }
        else{
            newState.blackPieces = newState.blackPieces xor moveMask
        }

        // en passant capture
        if(toMask and enPassantTarget != 0uL && move.pieceType == Move.PIECE_PAWN){
            val capturedPawnMask = 1uL shl (move.to + if(isWhite) - 8 else 8)
            newState.pawns = newState.pawns xor capturedPawnMask
            if(isWhite)
                newState.blackPieces = newState.blackPieces xor capturedPawnMask
            else
                newState.whitePieces = newState.whitePieces xor capturedPawnMask
        }

        // new en passant target
        newState.enPassantTarget = 0uL
        if (move.pieceType == Move.PIECE_PAWN) {
            val diff = move.from - move.to
            if (diff == 16 || diff == -16) {
                newState.enPassantTarget = if (isWhite) (1uL shl (move.to - 8)) else (1uL shl (move.to + 8))
            }
        }

        if(move.pieceType == Move.PIECE_KING){
            newState.castlingRights = newState.castlingRights and (if (isWhite) 0b1100 else 0b0011)

            if (move.from == 4 && move.to == 6) {
                newState.rooks = newState.rooks xor (1uL shl 7) xor (1uL shl 5)
                newState.whitePieces = newState.whitePieces xor (1uL shl 7) xor (1uL shl 5)
            } else if (move.from == 4 && move.to == 2) {
                newState.rooks = newState.rooks xor (1uL shl 0) xor (1uL shl 3)
                newState.whitePieces = newState.whitePieces xor (1uL shl 0) xor (1uL shl 3)
            }

            if (move.from == 60 && move.to == 62) {
                newState.rooks = newState.rooks xor (1uL shl 63) xor (1uL shl 61)
                newState.blackPieces = newState.blackPieces xor (1uL shl 63) xor (1uL shl 61)
            } else if (move.from == 60 && move.to == 58) {
                newState.rooks = newState.rooks xor (1uL shl 56) xor (1uL shl 59)
                newState.blackPieces = newState.blackPieces xor (1uL shl 56) xor (1uL shl 59)
            }

        }

        if (move.pieceType == Move.PIECE_ROOK) {
            when (move.from) {
                0 -> newState.castlingRights = newState.castlingRights and 0b1101
                7 -> newState.castlingRights = newState.castlingRights and 0b1110
                56 -> newState.castlingRights = newState.castlingRights and 0b0111
                63 -> newState.castlingRights = newState.castlingRights and 0b1011
            }
        }
        when(move.to){
            0 -> newState.castlingRights = newState.castlingRights and 0b1101
            7 -> newState.castlingRights = newState.castlingRights and 0b1110
            56 -> newState.castlingRights = newState.castlingRights and 0b0111
            63 -> newState.castlingRights = newState.castlingRights and 0b1011
        }

        // promotion
        if(move.promotionType != Move.PROMOTION_NONE) {
            when(move.promotionType){
                Move.PROMOTION_KNIGHT -> newState.knights = newState.knights or toMask
                Move.PROMOTION_ROOK -> newState.rooks = newState.rooks or toMask
                Move.PROMOTION_QUEEN -> newState.queens = newState.queens or toMask
                Move.PROMOTION_BISHOP -> newState.bishops = newState.bishops or toMask
            }
            newState.pawns = newState.pawns xor toMask
        }

        if(isCapture || move.pieceType == Move.PIECE_PAWN)
            newState.halfMoveClock = 0
        else
            newState.halfMoveClock++

        if(!isWhite)
            newState.fullMoveNumber++


        newState.whiteToMove = !newState.whiteToMove

        return newState

    }
}
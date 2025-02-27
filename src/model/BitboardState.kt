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
    var halfMoveClock: Int = 0, //moves without capture or pawn advancing
    var fullMoveNumber: Int = 1
){
    companion object {
        val SINGLE_BIT_MASKS = Array(64) { 1uL shl it }

        val WHITE_KING_CASTLE_MASK = SINGLE_BIT_MASKS[7] or SINGLE_BIT_MASKS[5]
        val WHITE_QUEEN_CASTLE_MASK = SINGLE_BIT_MASKS[0] or SINGLE_BIT_MASKS[3]
        val BLACK_KING_CASTLE_MASK = SINGLE_BIT_MASKS[63] or SINGLE_BIT_MASKS[61]
        val BLACK_QUEEN_CASTLE_MASK = SINGLE_BIT_MASKS[56] or SINGLE_BIT_MASKS[59]
    }

    private data class MoveUndoData(
        val move: Move,
        val capturedPieceType: Int,
        val previousEnPassantTarget: Int,
        val previousCastlingRights: Int,
        val previousHalfMoveClock: Int
    )

    private val moveHistory = ArrayDeque<MoveUndoData>()

    fun makeMove(move: Move){
        val fromMask =  SINGLE_BIT_MASKS[move.from]
        val toMask = SINGLE_BIT_MASKS[move.to]
        val moveMask = fromMask or toMask
        val isWhite = this.whiteToMove
        val opponentPieces = if(isWhite) this.blackPieces else this.whitePieces

        val capturedPieceType = when {
            (pawns and toMask) != 0uL -> Move.PIECE_PAWN
            (knights and toMask) != 0uL -> Move.PIECE_KNIGHT
            (bishops and toMask) != 0uL -> Move.PIECE_BISHOP
            (rooks and toMask) != 0uL -> Move.PIECE_ROOK
            (queens and toMask) != 0uL -> Move.PIECE_QUEEN
            else -> Move.PIECE_NONE
        }
        moveHistory.addLast(
            MoveUndoData(
                move = move,
                capturedPieceType = capturedPieceType,
                previousEnPassantTarget = enPassantTarget.countTrailingZeroBits(),
                previousCastlingRights = castlingRights,
                previousHalfMoveClock = halfMoveClock
            )
        )

        // capture - clear the field to which a piece is being moved
        if(opponentPieces and toMask != 0uL){
            val invMask = toMask.inv()
            pawns = pawns and invMask
            knights = knights and invMask
            bishops = bishops and invMask
            rooks = rooks and invMask
            queens = queens and invMask
            if(isWhite) blackPieces = blackPieces xor toMask
            else whitePieces = whitePieces xor toMask
            halfMoveClock = 0
        } else if (move.pieceType == Move.PIECE_PAWN) {
            halfMoveClock = 0
        }else{
            halfMoveClock++
        }

        // move
        when(move.pieceType) {
            Move.PIECE_PAWN -> pawns = pawns xor moveMask
            Move.PIECE_KNIGHT -> knights = knights xor moveMask
            Move.PIECE_BISHOP -> bishops = bishops xor moveMask
            Move.PIECE_ROOK -> rooks = rooks xor moveMask
            Move.PIECE_QUEEN -> queens = queens xor moveMask
            Move.PIECE_KING -> kings = kings xor moveMask
        }
        if (isWhite) whitePieces = whitePieces xor moveMask
        else blackPieces = blackPieces xor moveMask

        // en passant capture
        if(toMask and enPassantTarget != 0uL && move.pieceType == Move.PIECE_PAWN){
            val capturedPawnMask =  SINGLE_BIT_MASKS[move.to + if (isWhite) -8 else 8]
            pawns = pawns xor capturedPawnMask
            if (isWhite) blackPieces = blackPieces xor capturedPawnMask
            else whitePieces = whitePieces xor capturedPawnMask
        }

        // new en passant target
        enPassantTarget = 0uL
        if (move.pieceType == Move.PIECE_PAWN) {
            val diff = move.from - move.to
            if (diff == 16 || diff == -16) {
                enPassantTarget = if (isWhite) SINGLE_BIT_MASKS[move.to - 8] else SINGLE_BIT_MASKS[move.to + 8]
            }
        }

        if(move.pieceType == Move.PIECE_KING){
            castlingRights = castlingRights and (if (isWhite) 0b1100 else 0b0011)

            if (move.from == 4 && move.to == 6) {
                rooks = rooks xor WHITE_KING_CASTLE_MASK
                whitePieces = whitePieces xor WHITE_KING_CASTLE_MASK
            } else if (move.from == 4 && move.to == 2) {
                rooks = rooks xor WHITE_QUEEN_CASTLE_MASK
                whitePieces = whitePieces xor WHITE_QUEEN_CASTLE_MASK
            }

            if (move.from == 60 && move.to == 62) {
                rooks = rooks xor BLACK_KING_CASTLE_MASK
                blackPieces = blackPieces xor BLACK_KING_CASTLE_MASK
            } else if (move.from == 60 && move.to == 58) {
                rooks = rooks xor BLACK_QUEEN_CASTLE_MASK
                blackPieces = blackPieces xor BLACK_QUEEN_CASTLE_MASK
            }

        }

        if (move.pieceType == Move.PIECE_ROOK) {
            when (move.from) {
                0 -> castlingRights = castlingRights and 0b1101
                7 -> castlingRights = castlingRights and 0b1110
                56 -> castlingRights = castlingRights and 0b0111
                63 -> castlingRights = castlingRights and 0b1011
            }
        }
        when(move.to){
            0 -> castlingRights = castlingRights and 0b1101
            7 -> castlingRights = castlingRights and 0b1110
            56 -> castlingRights = castlingRights and 0b0111
            63 -> castlingRights = castlingRights and 0b1011
        }

        // promotion
        if(move.promotionType != Move.PROMOTION_NONE) {
            when(move.promotionType){
                Move.PROMOTION_KNIGHT -> knights = knights or toMask
                Move.PROMOTION_ROOK -> rooks = rooks or toMask
                Move.PROMOTION_QUEEN -> queens = queens or toMask
                Move.PROMOTION_BISHOP -> bishops = bishops or toMask
            }
            pawns = pawns xor toMask
        }

        if(!isWhite)
            fullMoveNumber++


        whiteToMove = !whiteToMove

    }

    fun unmakeMove(){
        if(moveHistory.isEmpty()){
            throw IllegalStateException("No moves to undo")
        }

        val undoData = moveHistory.removeLast()

        val fromMask = SINGLE_BIT_MASKS[undoData.move.from]
        val toMask = SINGLE_BIT_MASKS[undoData.move.to]
        val moveMask = fromMask or toMask
        val isWhite = !this.whiteToMove

        // move the piece back
        if(undoData.move.promotionType == Move.PROMOTION_NONE) {
            when (undoData.move.pieceType) {
                Move.PIECE_PAWN -> pawns = pawns xor moveMask
                Move.PIECE_KNIGHT -> knights = knights xor moveMask
                Move.PIECE_BISHOP -> bishops = bishops xor moveMask
                Move.PIECE_ROOK -> rooks = rooks xor moveMask
                Move.PIECE_QUEEN -> queens = queens xor moveMask
                Move.PIECE_KING -> kings = kings xor moveMask
            }
        }
        else{
            when(undoData.move.promotionType){
                Move.PROMOTION_QUEEN -> queens = queens xor toMask
                Move.PROMOTION_ROOK -> rooks = rooks xor toMask
                Move.PROMOTION_KNIGHT -> knights = knights xor toMask
                Move.PROMOTION_BISHOP -> bishops = bishops xor toMask
            }
            pawns = pawns or fromMask
        }
        if(isWhite) whitePieces = whitePieces xor moveMask
        else blackPieces = blackPieces xor moveMask

        // bring the captured piece back
        if(undoData.capturedPieceType != Move.PIECE_NONE){
            when (undoData.capturedPieceType) {
                Move.PIECE_PAWN -> pawns = pawns or toMask
                Move.PIECE_KNIGHT -> knights = knights or toMask
                Move.PIECE_BISHOP -> bishops = bishops or toMask
                Move.PIECE_ROOK -> rooks = rooks or toMask
                Move.PIECE_QUEEN -> queens = queens or toMask
            }
            if (isWhite) blackPieces = blackPieces or toMask
            else whitePieces = whitePieces or toMask
        }
        // bring the piece back from en passant capture
        if(undoData.move.to == undoData.previousEnPassantTarget && undoData.move.pieceType == Move.PIECE_PAWN){
            val capturedPawn = if(isWhite) undoData.move.to - 8 else undoData.move.to + 8
            pawns = pawns or SINGLE_BIT_MASKS[capturedPawn]
            if(isWhite) blackPieces = blackPieces or SINGLE_BIT_MASKS[capturedPawn]
            else whitePieces = whitePieces or SINGLE_BIT_MASKS[capturedPawn]
        }

        //move the rook back after castling
        if (undoData.move.pieceType == Move.PIECE_KING) {
            when (undoData.move.from to undoData.move.to) {
                4 to 6 -> {
                    rooks = rooks xor WHITE_KING_CASTLE_MASK
                    whitePieces = whitePieces xor WHITE_KING_CASTLE_MASK
                }
                4 to 2 -> {
                    rooks = rooks xor WHITE_QUEEN_CASTLE_MASK
                    whitePieces = whitePieces xor WHITE_QUEEN_CASTLE_MASK
                }
                60 to 62 -> {
                    rooks = rooks xor BLACK_KING_CASTLE_MASK
                    blackPieces = blackPieces xor BLACK_KING_CASTLE_MASK
                }
                60 to 58 -> {
                    rooks = rooks xor BLACK_QUEEN_CASTLE_MASK
                    blackPieces = blackPieces xor BLACK_QUEEN_CASTLE_MASK
                }
            }
        }


        enPassantTarget = if(undoData.previousEnPassantTarget < 64) SINGLE_BIT_MASKS[undoData.previousEnPassantTarget] else 0uL
        castlingRights = undoData.previousCastlingRights
        halfMoveClock = undoData.previousHalfMoveClock
        if(!isWhite) fullMoveNumber--

        whiteToMove = isWhite
    }

    // return a copy of the given Bitboard position
    fun applyMove(move: Move): BitboardState {
        val newState = this.copy()

        val fromMask =  SINGLE_BIT_MASKS[move.from]
        val toMask = SINGLE_BIT_MASKS[move.to]
        val moveMask = fromMask or toMask
        val isWhite = this.whiteToMove
        val opponentPieces = if(isWhite) this.blackPieces else this.whitePieces

        // capture - clear the field to which a piece is being moved
        if(opponentPieces and toMask != 0uL){
            val invMask = toMask.inv()
            newState.pawns = newState.pawns and invMask
            newState.knights = newState.knights and invMask
            newState.bishops = newState.bishops and invMask
            newState.rooks = newState.rooks and invMask
            newState.queens = newState.queens and invMask
            if(isWhite) newState.blackPieces = newState.blackPieces xor toMask
            else newState.whitePieces = newState.whitePieces xor toMask
            newState.halfMoveClock = 0
        } else if (move.pieceType == Move.PIECE_PAWN) {
            newState.halfMoveClock = 0
        }else{
            newState.halfMoveClock++
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
        if (isWhite) newState.whitePieces = newState.whitePieces xor moveMask
        else newState.blackPieces = newState.blackPieces xor moveMask


        // en passant capture
        if(toMask and enPassantTarget != 0uL && move.pieceType == Move.PIECE_PAWN){
            val capturedPawnMask =  SINGLE_BIT_MASKS[move.to + if (isWhite) -8 else 8]
            newState.pawns = newState.pawns xor capturedPawnMask
            if (isWhite) newState.blackPieces = newState.blackPieces xor capturedPawnMask
            else newState.whitePieces = newState.whitePieces xor capturedPawnMask
        }

        // new en passant target
        newState.enPassantTarget = 0uL
        if (move.pieceType == Move.PIECE_PAWN) {
            val diff = move.from - move.to
            if (diff == 16 || diff == -16) {
                newState.enPassantTarget = if (isWhite) SINGLE_BIT_MASKS[move.to - 8] else SINGLE_BIT_MASKS[move.to + 8]
            }
        }

        if(move.pieceType == Move.PIECE_KING){
            newState.castlingRights = newState.castlingRights and (if (isWhite) 0b1100 else 0b0011)

            if (move.from == 4 && move.to == 6) {
                newState.rooks = newState.rooks xor WHITE_KING_CASTLE_MASK
                newState.whitePieces = newState.whitePieces xor WHITE_KING_CASTLE_MASK
            } else if (move.from == 4 && move.to == 2) {
                newState.rooks = newState.rooks xor WHITE_QUEEN_CASTLE_MASK
                newState.whitePieces = newState.whitePieces xor WHITE_QUEEN_CASTLE_MASK
            }

            if (move.from == 60 && move.to == 62) {
                newState.rooks = newState.rooks xor BLACK_KING_CASTLE_MASK
                newState.blackPieces = newState.blackPieces xor BLACK_KING_CASTLE_MASK
            } else if (move.from == 60 && move.to == 58) {
                newState.rooks = newState.rooks xor BLACK_QUEEN_CASTLE_MASK
                newState.blackPieces = newState.blackPieces xor BLACK_QUEEN_CASTLE_MASK
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

        if(!isWhite)
            newState.fullMoveNumber++


        newState.whiteToMove = !newState.whiteToMove

        return newState

    }
}
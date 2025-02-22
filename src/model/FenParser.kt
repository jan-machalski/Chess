package model

object FenParser {
    fun fenToBitboard(fen: String): BitboardState {
        val parts = fen.split(" ")
        val boardPart = parts[0]
        val turnPart = parts[1]
        val castlingPart = parts[2]
        val enPassantPart = parts[3]
        val halfMoveClockPart = parts[4].toInt()
        val fullMoveNumberPart = parts[5].toInt()

        var pawns = 0uL
        var knights = 0uL
        var bishops = 0uL
        var rooks = 0uL
        var queens = 0uL
        var kings = 0uL
        var whitePieces = 0uL
        var blackPieces = 0uL
        var enPassantTarget = 0uL

        var squareIndex = 56

        for (char in boardPart) {
            when {
                char.isDigit() -> squareIndex += char.toString().toInt()
                char == '/' -> squareIndex -= 16
                else -> {
                    val bitPosition = 1uL shl squareIndex
                    when (char) {
                        'P', 'N', 'B', 'R', 'Q', 'K' -> {
                            when (char) {
                                'P' -> pawns = pawns or bitPosition
                                'N' -> knights = knights or bitPosition
                                'B' -> bishops = bishops or bitPosition
                                'R' -> rooks = rooks or bitPosition
                                'Q' -> queens = queens or bitPosition
                                'K' -> kings = kings or bitPosition
                            }
                            whitePieces = whitePieces or bitPosition
                        }
                        'p', 'n', 'b', 'r', 'q', 'k' -> {
                            when (char) {
                                'p' -> pawns = pawns or bitPosition
                                'n' -> knights = knights or bitPosition
                                'b' -> bishops = bishops or bitPosition
                                'r' -> rooks = rooks or bitPosition
                                'q' -> queens = queens or bitPosition
                                'k' -> kings = kings or bitPosition
                            }
                            blackPieces = blackPieces or bitPosition
                        }
                    }
                    squareIndex++
                }
            }
        }

        val whiteToMove = turnPart == "w"

        var castlingRights = 0b0000
        if ('K' in castlingPart) castlingRights = castlingRights or 0b0001
        if ('Q' in castlingPart) castlingRights = castlingRights or 0b0010
        if ('k' in castlingPart) castlingRights = castlingRights or 0b0100
        if ('q' in castlingPart) castlingRights = castlingRights or 0b1000

        if (enPassantPart != "-") {
            val file = enPassantPart[0] - 'a'
            val rank = enPassantPart[1] - '1'
            enPassantTarget = 1uL shl (rank * 8 + file)
        }

        return BitboardState(
            pawns, knights, bishops, rooks, queens, kings,
            whitePieces, blackPieces,
            whiteToMove, castlingRights, enPassantTarget,
            halfMoveClockPart, fullMoveNumberPart
        )
    }

    fun bitboardStateToFen(state: BitboardState): String {
        val sb = StringBuilder()

        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val squareIndex = rank * 8 + file
                val mask = 1uL shl squareIndex

                val pieceChar = getPieceChar(state, mask)
                if (pieceChar == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(pieceChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) {
                sb.append('/')
            }
        }

        sb.append(' ')
        sb.append(if (state.whiteToMove) 'w' else 'b')
        sb.append(' ')

        val castling = getCastlingFen(state.castlingRights)
        sb.append(if (castling.isEmpty()) "-" else castling)
        sb.append(' ')

        sb.append(getEnPassantFen(state.enPassantTarget))
        sb.append(' ')

        sb.append(state.halfMoveClock)
        sb.append(' ')

        sb.append(state.fullMoveNumber)

        return sb.toString()
    }
    private fun getPieceChar(state: BitboardState, mask: ULong): Char? {
        val isWhite = (state.whitePieces and mask) != 0uL
        val isBlack = (state.blackPieces and mask) != 0uL
        if (!isWhite && !isBlack) {
            return null
        }

        val white = isWhite // alias
        return when {
            (state.pawns and mask) != 0uL -> if (white) 'P' else 'p'
            (state.rooks and mask) != 0uL -> if (white) 'R' else 'r'
            (state.knights and mask) != 0uL -> if (white) 'N' else 'n'
            (state.bishops and mask) != 0uL -> if (white) 'B' else 'b'
            (state.queens and mask) != 0uL -> if (white) 'Q' else 'q'
            (state.kings and mask) != 0uL -> if (white) 'K' else 'k'
            else -> null
        }
    }
    private fun getCastlingFen(castlingRights: Int): String {
        val sb = StringBuilder()
        if ((castlingRights and 0b0001) != 0) sb.append('K')
        if ((castlingRights and 0b0010) != 0) sb.append('Q')
        if ((castlingRights and 0b0100) != 0) sb.append('k')
        if ((castlingRights and 0b1000) != 0) sb.append('q')
        return sb.toString()
    }
    private fun getEnPassantFen(enPassant: ULong): String {
        if (enPassant == 0uL) return "-"

        val square = enPassant.countTrailingZeroBits()
        val file = square % 8
        val rank = square / 8
        val fileChar = ('a'.code + file).toChar()
        val rankChar = ('1'.code + rank).toChar()
        return "$fileChar$rankChar"
    }
}
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
}
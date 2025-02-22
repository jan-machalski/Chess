package model

object BoardRenderer {
    private val pieceSymbols = mapOf(
        "pawns" to Pair('P', 'p'),
        "knights" to Pair('N', 'n'),
        "bishops" to Pair('B', 'b'),
        "rooks" to Pair('R', 'r'),
        "queens" to Pair('Q', 'q'),
        "kings" to Pair('K', 'k')
    )

    fun drawBoard(state: BitboardState) {
        val boardArray = Array(8) { Array(8) { '.' } }

        for (square in 0..<64) {
            val bit = 1uL shl square
            val rank = square / 8
            val file = square % 8

            for ((bitboardName, symbol) in pieceSymbols) {
                val pieceBitboard = when (bitboardName) {
                    "pawns" -> state.pawns
                    "knights" -> state.knights
                    "bishops" -> state.bishops
                    "rooks" -> state.rooks
                    "queens" -> state.queens
                    "kings" -> state.kings
                    else -> 0uL
                }

                if ((pieceBitboard and bit) != 0uL) {
                    boardArray[rank][file] = if ((state.blackPieces and bit) != 0uL) symbol.second else symbol.first
                }
            }
        }

        println("  +-----------------+")
        for (rank in 7 downTo 0) {
            print("${rank + 1} | ")
            for (file in 0 ..< 8) {
                print("${boardArray[rank][file]} ")
            }
            println("|")
        }
        println("  +-----------------+")
        println("    a b c d e f g h")
    }
}
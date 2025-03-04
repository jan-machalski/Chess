package model

@JvmInline
value class Move(private val data: Int) {
    val from: Int get() = (data shr 26) and 0x3F
    val to: Int get() = (data shr 20) and 0x3F
    val promotionType: Int get() = (data shr 16) and 0xF
    val pieceType: Int get() = (data shr 12) and 0xF
    val capturedPieceType: Int get() = (data shr 8) and 0xF

    companion object {
        const val PIECE_NONE = 0
        const val PIECE_PAWN = 1
        const val PIECE_KNIGHT = 2
        const val PIECE_BISHOP = 3
        const val PIECE_ROOK = 4
        const val PIECE_QUEEN = 5
        const val PIECE_KING = 6

        fun create(from: Int, to: Int, pieceType: Int, capturedPieceType: Int, promotionType: Int = PIECE_NONE): Move {
            val moveData = (from shl 26) or (to shl 20) or (promotionType shl 16) or (pieceType shl 12) or (capturedPieceType shl 8)
            return Move(moveData)
        }
    }

    override fun toString(): String {
        return "Move(from=$from, to=$to, pieceType=$pieceType, promotion=$promotionType)"
    }
}
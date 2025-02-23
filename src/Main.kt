import model.*
import movegen.*
import movegen.BishopMoves.BISHOP_ATTACKS_LOOKUP
import movegen.RookMoves.ROOK_MAGIC_NUMBERS
import movegen.RookMoves.ROOK_MAGIC_SHIFTS

fun main() {
    val fen = "8/6kR/8/8/8/bq6/1rqqqqqq/K1nqnbrq b - - 0 1"
    var state = FenParser.fenToBitboard(fen)
    BoardRenderer.drawBoard(state)
    println(MoveGenerator.generateLegalMoves(state))
}
import model.*
import movegen.*
import movegen.BishopMoves.BISHOP_ATTACKS_LOOKUP
import movegen.RookMoves.ROOK_MAGIC_NUMBERS
import movegen.RookMoves.ROOK_MAGIC_SHIFTS

fun main() {
    val fen = "8/8/3p4/1Pp3kr/1K3R2/8/4P1P1/8 w - c6 0 3"
    var state = FenParser.fenToBitboard(fen)
    BoardRenderer.drawBoard(state)
    println(MoveGenerator.generateLegalMoves(state))
}
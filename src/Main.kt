import model.*
import movegen.*
import movegen.BishopMoves.BISHOP_ATTACKS_LOOKUP
import movegen.RookMoves.ROOK_MAGIC_NUMBERS
import movegen.RookMoves.ROOK_MAGIC_SHIFTS

fun main() {
    val bs = FenParser.fenToBitboard("rnbqkbn1/pppp1ppp/8/r2PpK2/8/8/PPP1PPPP/RNBQ1BNR w KQkq e6 0 1")
    BoardRenderer.drawBoard(bs)
    println(bs)
    val moves = MoveGenerator.generateLegalMoves(bs)
    for(move in moves) {
        println(move)
    }

}
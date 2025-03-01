import model.*
import movegen.*
import engine.*

fun main() {
    val fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R b KQkq - 0 1"
    var state = FenParser.fenToBitboard(fen)
    BoardRenderer.drawBoard(state)
    val start = System.nanoTime()
    val move = Search.findBestMove(state,4)
    val time = System.nanoTime() - start
    println("$time $move")
}
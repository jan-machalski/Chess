package engine

import model.BitboardState
import model.Move
import movegen.BitboardAnalyzer


object MoveOrdering {

    fun sortMoves(moves: MutableList<Move>, state: BitboardState) {
        val oppPawnAttacks = BitboardAnalyzer.getPawnAttackedFields(state)

        val moveScores = moves.map {move ->
            move to moveScoreGuess(move,oppPawnAttacks)
        }

        moveScores.sortedByDescending {it.second}
            .map {it.first}
            .let{sortedList ->
                moves.clear()
                moves.addAll(sortedList)
            }
    }

    private fun moveScoreGuess(move:Move, oppPawnAttacks: ULong):Int{
        var scoreGuess = 0
        val movePieceType = move.pieceType

        if(move.capturedPieceType != Move.PIECE_NONE){
            scoreGuess += 10 * getPieceValue(move.capturedPieceType) - getPieceValue(movePieceType)
        }
        scoreGuess += getPieceValue(move.promotionType)
        if(BitboardAnalyzer.SINGLE_BIT_MASKS[move.to] and oppPawnAttacks != 0uL){
            scoreGuess -= getPieceValue(move.pieceType)
        }
        return scoreGuess
    }

    private fun getPieceValue(pieceType: Int) = when (pieceType) {
        Move.PIECE_PAWN -> Evaluation.PAWN_VALUE
        Move.PIECE_KNIGHT -> Evaluation.KNIGHT_VALUE
        Move.PIECE_ROOK -> Evaluation.ROOK_VALUE
        Move.PIECE_BISHOP -> Evaluation.BISHOP_VALUE
        Move.PIECE_QUEEN -> Evaluation.QUEEN_VALUE
        else -> 0
    }
}
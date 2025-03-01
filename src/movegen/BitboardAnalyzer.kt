package movegen
import model.BitboardState


object BitboardAnalyzer {
    internal val SINGLE_BIT_MASKS = Array(64){1uL shl it}
    internal val PINNED_MOVES_LOOKUP = precomputePinnedMoves() // [ourKingPos][move.from][move.to], used for pinned pieces, true if the move does not uncover our king
    internal val BLOCK_MOVES_LOOKUP = precomputeBlockMoves() //[ourKingPos][attackerPos][move.to], true if such move prevents check (block/capture). attackerPos = 64 -> no check, true everywhere

    // return the mask of pinned pieces for the player to move
    internal fun getPinnedPieces(state: BitboardState): ULong{
        val ourPieces = if(state.whiteToMove) state.whitePieces else state.blackPieces
        val ourKing = ourPieces and state.kings
        val ourKingPos = ourKing.countTrailingZeroBits()
        val opponentPieces = if(state.whiteToMove) state.blackPieces else state.whitePieces

        var opponentRooks = opponentPieces and (state.rooks or state.queens) and RookMoves.ROW_COLUMN_MASKS[ourKingPos]
        var opponentBishops = opponentPieces and (state.bishops or state.queens) and BishopMoves.DIAGONAL_MASKS[ourKingPos]

        val potentialRookBlockers = RookMoves.getAttackedFieldsMask(state, ourKingPos)
        val potentialBishopBlockers = BishopMoves.getAttackedFieldsMask(state, ourKingPos)

        var rookAttackedPieces = 0uL
        while(opponentRooks != 0uL){
            val pos = opponentRooks.countTrailingZeroBits()
            rookAttackedPieces = rookAttackedPieces or RookMoves.getAttackedFieldsMask(state, pos)
            opponentRooks = opponentRooks xor SINGLE_BIT_MASKS[pos]
        }
        var pinnedPieces = rookAttackedPieces and potentialRookBlockers

        var bishopAttackedPieces = 0uL
        while(opponentBishops != 0uL){
            val pos = opponentBishops.countTrailingZeroBits()
            bishopAttackedPieces = bishopAttackedPieces or BishopMoves.getAttackedFieldsMask(state, pos)
            opponentBishops = opponentBishops xor SINGLE_BIT_MASKS[pos]
        }
        pinnedPieces = pinnedPieces or (bishopAttackedPieces and potentialBishopBlockers)

        return pinnedPieces
    }

    // return mask of opponents pieces from which our king is currently under check
    fun getChecks(state: BitboardState): ULong{
        var checks = 0uL
        val kingPos = (state.kings and if(state.whiteToMove) state.whitePieces else state.blackPieces).countTrailingZeroBits()
        val opponentPieces = if(state.whiteToMove) state.blackPieces else state.whitePieces
        val allPieces = (state.blackPieces or state.whitePieces) and
                (state.kings and if (state.whiteToMove) state.whitePieces else state.blackPieces).inv()

        checks = checks or ((if(state.whiteToMove) PawnMoves.PAWN_ATTACKS_WHITE[kingPos] else PawnMoves.PAWN_ATTACKS_BLACK[kingPos]) and opponentPieces and state.pawns)

        checks = checks or (KingMoves.KING_MOVES[kingPos] and opponentPieces and state.kings)
        checks = checks or (KnightMoves.KNIGHT_MOVES[kingPos] and opponentPieces and state.knights)

        val rookBlockers = RookMoves.ROOK_BLOCKER_MASKS[kingPos] and allPieces
        val possibleRookChecks = RookMoves.ROOK_ATTACKS_LOOKUP[kingPos][((RookMoves.ROOK_MAGIC_NUMBERS[kingPos] * rookBlockers) shr RookMoves.ROOK_MAGIC_SHIFTS[kingPos]).toInt()]
        checks = checks or (possibleRookChecks and (state.queens or state.rooks) and opponentPieces)

        val bishopBlockers = BishopMoves.BISHOP_BLOCKER_MASKS[kingPos] and allPieces
        val possibleBishopChecks = BishopMoves.BISHOP_ATTACKS_LOOKUP[kingPos][((BishopMoves.BISHOP_MAGIC_NUMBERS[kingPos] * bishopBlockers)shr BishopMoves.BISHOP_MAGIC_SHIFTS[kingPos]).toInt()]
        checks = checks or (possibleBishopChecks and (state.queens or state.bishops) and opponentPieces)

        return checks
    }

    //return mask of fields currently attacked by the opponent
    fun getAttackedFields(state: BitboardState):ULong{
        var attackedFields = 0uL
        val isWhite = state.whiteToMove
        val opponentPieces = if(isWhite) state.blackPieces else state.whitePieces

        val oppKing = opponentPieces and state.kings
        attackedFields = attackedFields or KingMoves.KING_MOVES[oppKing.countTrailingZeroBits()]

        val ourKing = state.kings xor oppKing
        state.kings = oppKing
        if(isWhite) state.whitePieces = state.whitePieces xor ourKing else state.blackPieces = state.blackPieces xor ourKing

        var pawns = opponentPieces and state.pawns
        while(pawns != 0uL){
            val field = pawns.countTrailingZeroBits()
            attackedFields = attackedFields or if(isWhite) PawnMoves.PAWN_ATTACKS_BLACK[field] else PawnMoves.PAWN_ATTACKS_WHITE[field]
            pawns = pawns xor SINGLE_BIT_MASKS[field]
        }

        var knights = opponentPieces and state.knights
        while(knights != 0uL){
            val field = knights.countTrailingZeroBits()
            attackedFields = attackedFields or KnightMoves.KNIGHT_MOVES[field]
            knights = knights xor SINGLE_BIT_MASKS[field]
        }

        var rooks = opponentPieces and (state.rooks or state.queens)
        while(rooks != 0uL){
            val field = rooks.countTrailingZeroBits()
            attackedFields = attackedFields or RookMoves.getAttackedFieldsMask(state, field)
            rooks = rooks xor SINGLE_BIT_MASKS[field]
        }

        var bishops = opponentPieces and (state.bishops or state.queens)
        while(bishops != 0uL){
            val field = bishops.countTrailingZeroBits()
            attackedFields = attackedFields or BishopMoves.getAttackedFieldsMask(state, field)
            bishops = bishops xor SINGLE_BIT_MASKS[field]
        }

        state.kings = ourKing or oppKing
        if(isWhite) state.whitePieces = state.whitePieces or ourKing else state.blackPieces = state.blackPieces or ourKing
        return attackedFields
    }

    fun getPawnAttackedFields(state: BitboardState): ULong{
        var pawns = state.pawns and if(state.whiteToMove) state.blackPieces else state.whitePieces
        var attackedFieldsMask = 0uL

        while(pawns != 0uL){
            val pawn = pawns.countTrailingZeroBits()
            attackedFieldsMask = attackedFieldsMask or if(state.whiteToMove) PawnMoves.PAWN_ATTACKS_BLACK[pawn] else PawnMoves.PAWN_ATTACKS_WHITE[pawn]
            pawns = pawns xor SINGLE_BIT_MASKS[pawn]
        }
        return attackedFieldsMask
    }

    private fun precomputePinnedMoves(): Array<Array<Array<Boolean>>> {
        val pinnedMoves = Array(64) { Array(64) { Array(64) { false } } }

        for (kingPos in 0 until 64) {
            for (piecePos in 0 until 64) {
                val pinnedMask = computePinnedMoves(kingPos, piecePos)
                for (targetPos in 0 until 64) {
                    if ((1uL shl targetPos) and pinnedMask != 0uL) {
                        pinnedMoves[kingPos][piecePos][targetPos] = true
                    }
                }
            }
        }
        return pinnedMoves
    }

    private fun computePinnedMoves(kingPos: Int, piecePos: Int): ULong {
        var pinnedMask = 0uL

        if (sameFile(kingPos, piecePos)) {
            val step = if (piecePos > kingPos) 8 else -8
            var pos = kingPos
            while (pos in 0..63) {
                pinnedMask = pinnedMask or (1uL shl pos)
                pos += step
            }
            return pinnedMask
        }

        if (sameRank(kingPos, piecePos)) {
            val step = if (piecePos > kingPos) 1 else -1
            var pos = kingPos
            while (pos / 8 == kingPos / 8) {
                pinnedMask = pinnedMask or (1uL shl pos)
                pos += step
            }
            return pinnedMask
        }

        if (sameDiagonal(kingPos, piecePos)) {
            var step = if ((kingPos - piecePos) % 9 == 0) 9 else 7
            if(kingPos > piecePos)
                step = -step
            var pos = kingPos + step
            while (pos in 8..55 && pos % 8 != 0 && pos % 8 != 7) {
                pinnedMask = pinnedMask or (1uL shl pos)
                pos += step
            }
            return pinnedMask or (1uL shl pos)
        }

        return 0uL
    }

    private fun precomputeBlockMoves(): Array<Array<Array<Boolean>>> {
        val blockMoves = Array(64) { Array(65) { Array(64) { false } } }

        for (kingPos in 0 until 64) {
            for (attackerPos in 0 until 64) {
                val blockMask = computeBlockMoves(kingPos, attackerPos)
                for (targetPos in 0 until 64) {
                    if ((1uL shl targetPos) and blockMask != 0uL) {
                        blockMoves[kingPos][attackerPos][targetPos] = true
                    }
                }
            }
            for(targetPos in 0 until 64) blockMoves[kingPos][64][targetPos] = true
        }
        return blockMoves
    }

    private fun computeBlockMoves(kingPos: Int, attackerPos: Int): ULong {
        var blockMask = 0uL

        if (isKnightJump(kingPos, attackerPos)) {
            return 1uL shl attackerPos
        }

        if (sameFile(kingPos, attackerPos)) {
            val step = if (attackerPos > kingPos) 8 else -8
            var pos = kingPos + step
            while (pos in 0..63 && pos != attackerPos) {
                blockMask = blockMask or (1uL shl pos)
                pos += step
            }
            return blockMask or (1uL shl attackerPos)
        }

        if (sameRank(kingPos, attackerPos)) {
            val step = if (attackerPos > kingPos) 1 else -1
            var pos = kingPos + step
            while (pos / 8 == kingPos / 8 && pos != attackerPos) {
                blockMask = blockMask or (1uL shl pos)
                pos += step
            }
            return blockMask or (1uL shl attackerPos)
        }

        if (sameDiagonal(kingPos, attackerPos)) {
            val step = if ((kingPos - attackerPos) % 9 == 0) 9 else 7
            var pos = step + minOf(attackerPos, kingPos)
            while (pos != maxOf(attackerPos, kingPos)) {
                blockMask = blockMask or (1uL shl pos)
                pos += step
            }
            return blockMask or (1uL shl attackerPos)
        }

        return 0uL
    }

    private fun sameFile(a: Int, b: Int) = (a % 8) == (b % 8)
    private fun sameRank(a: Int, b: Int) = (a / 8) == (b / 8)
    private fun sameDiagonal(a: Int, b: Int) = ((a - b) % 9 == 0) || ((a - b) % 7 == 0)
    private fun isKnightJump(kingPos: Int, attackerPos: Int): Boolean {
        val fileDiff = kotlin.math.abs((kingPos % 8) - (attackerPos % 8))
        val rankDiff = kotlin.math.abs((kingPos / 8) - (attackerPos / 8))

        return (fileDiff == 1 && rankDiff == 2) || (fileDiff == 2 && rankDiff == 1)
    }
}
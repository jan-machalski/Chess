package movegen

import model.Move
import model.BitboardState

object RookMoves {
    val ROOK_BLOCKER_MASKS = precomputeRookMasks()
    val ROOK_MAGIC_SHIFTS = computeShifts()
    val ROOK_MAGIC_NUMBERS = arrayOf(
        0x80008061400210uL, 0x40100020004009uL, 0x1180100380a86000uL, 0x48800800c4100080uL, 0xa001020a8842600uL, 0x3200060008041001uL, 0x8c000cb028040a05uL, 0x100005200208100uL, 0x308800567804000uL, 0x49c2002208834104uL, 0x2163001300422004uL, 0x8042001a0012e140uL, 0x6270011000c2800uL, 0x1062002200081045uL, 0xa10400090208d004uL, 0x10c2000086440112uL, 0x3d41898001204006uL, 0x45c3020041820029uL, 0x42c0820018420020uL, 0x22901001000a14cuL, 0x5811910004080100uL, 0x80ca80800a002401uL, 0x4908240090072812uL, 0x50080a00006e8904uL, 0x840008880094020uL, 0x8014200080804000uL, 0x7210100180200284uL, 0x428c492100300100uL, 0xfe2e002200090450uL, 0xb92002a00084411uL, 0xa04052400080230uL, 0x10001420004840fuL, 0x3108400024800384uL, 0x7080e20082004100uL, 0xa321410295002000uL, 0x4102801000800800uL, 0x8005480080800c00uL, 0x202000822000490uL, 0xc514101284004128uL, 0x718490942000094uL, 0x240108160c48004uL, 0xad46008021020043uL, 0x1001012001310042uL, 0xcc42210090010048uL, 0x1110920032620028uL, 0xc3c0020004008080uL, 0x90f0b018010c000auL, 0x12810c105960004uL, 0x2698490482002a00uL, 0x94404080a2010600uL, 0xc085024620013100uL, 0x1110900048008180uL, 0x821c00808d080080uL, 0x412e01081004c200uL, 0x6080e89a01302400uL, 0x500d840881670200uL, 0x4000908000402107uL, 0x620d012040018415uL, 0x300c2000410098d5uL, 0x100a00081060c442uL, 0x82008420089082uL, 0x108a002c10780502uL, 0x2080061009880324uL, 0x204004c904002082uL)
    val ROOK_ATTACKS_LOOKUP = precomputeRookAttacks()

    fun generateRookMoves(state: BitboardState): List<Move>{
        val moves = mutableListOf<Move>()
        val isWhite = state.whiteToMove
        var rooks = state.rooks or state.queens and if(isWhite) state.whitePieces else state.blackPieces
        val ourPieces = if(isWhite) state.whitePieces else state.blackPieces
        val allPieces = state.blackPieces or state.whitePieces

        while(rooks != 0uL){
            val from = rooks.countTrailingZeroBits()
            val blockers = ROOK_BLOCKER_MASKS[from] and allPieces
            var possibleMoves = ROOK_ATTACKS_LOOKUP[from][((ROOK_MAGIC_NUMBERS[from] * blockers) shr ROOK_MAGIC_SHIFTS[from]).toInt()]
            possibleMoves = possibleMoves and ourPieces.inv()

            while(possibleMoves != 0uL){
                val to = possibleMoves.countTrailingZeroBits()
                moves.add(Move.create(from, to,Move.PIECE_ROOK))
                possibleMoves = possibleMoves and (possibleMoves - 1uL)
            }
            rooks = rooks and (rooks - 1uL)
        }
        return moves
    }

    private fun precomputeRookMasks(): Array<ULong>{
        val masks = Array(64){0uL}
        for(square in 0 until 64){
            masks[square] = generateRookMask(square)
        }
        return masks
    }

    private fun generateRookMask(square: Int): ULong{
        var mask = 0uL
        val file = square % 8
        val rank = square / 8

        for (i in file + 1 until 7) mask = mask or (1uL shl (rank * 8 + i))
        for (i in file -1 downTo 1) mask = mask or (1uL shl (rank * 8 + i))

        for (i in rank + 1 until 7) mask = mask or (1uL shl (8 * i + file))
        for (i in rank -1 downTo 1) mask = mask or (1uL shl (8 * i + file))

        return mask
    }

    private fun computeShifts(): Array<Int>{
        val shifts = Array(64){0}
        for(square in 0 until 64){
            shifts[square] = 64 - ROOK_BLOCKER_MASKS[square].countOneBits()
        }
        return shifts
    }

    fun generateBlockers(mask: ULong, index: Int): ULong{
        var blockers = 0uL
        var bits = mask
        var idx = index
        var bitCount = 0

        while (bits != 0uL) {
            val lsb = 1uL shl bits.countTrailingZeroBits()
            if ((idx and (1 shl bitCount)) != 0) {
                blockers = blockers or lsb
            }
            bits = bits xor lsb
            bitCount++
        }
        return blockers
    }
    fun computeRookAttacks(square:Int, blockers:ULong):ULong{
        var attacks = 0uL
        val file = square % 8
        val rank = square / 8

        for(i in file + 1 until 8){
            attacks = attacks or (1uL shl (rank * 8 + i))
            if(blockers and (1uL shl (rank * 8 + i)) != 0uL) break
        }
        for(i in file -1 downTo 0){
            attacks = attacks or (1uL shl (rank * 8 + i))
            if(blockers and (1uL shl (rank * 8 + i)) != 0uL) break
        }
        for(i in rank + 1 until 8){
            attacks = attacks or (1uL shl (i * 8 + file))
            if(blockers and (1uL shl (i * 8 + file)) != 0uL) break
        }
        for(i in rank -1 downTo 0){
            attacks = attacks or (1uL shl (i * 8 + file))
            if(blockers and (1uL shl (i * 8 + file)) != 0uL) break
        }
        return attacks
    }

    private fun precomputeRookAttacks(): Array<Array<ULong>>{
        val attacks = Array(64){square -> Array(1 shl (64- ROOK_MAGIC_SHIFTS[square])){0uL} }
        for (square in 0 until 64){
            val attackMask = ROOK_BLOCKER_MASKS[square]
            val relevantBits = attackMask.countOneBits()
            val blockerPermutations = 1 shl relevantBits
            for(i in 0 until blockerPermutations){
                val blockers = generateBlockers(attackMask,i)
                val magicIndex = (blockers * ROOK_MAGIC_NUMBERS[square]) shr (64 - relevantBits)

                attacks[square][magicIndex.toInt()] = computeRookAttacks(square,blockers)
            }
        }
        return attacks
    }
    fun getAttackedFieldsMask(state: BitboardState,pos: Int): ULong{
        val allPieces = state.blackPieces or state.whitePieces
        val blockers = ROOK_BLOCKER_MASKS[pos] and allPieces
        val attackedFields = ROOK_ATTACKS_LOOKUP[pos][((ROOK_MAGIC_NUMBERS[pos] * blockers) shr ROOK_MAGIC_SHIFTS[pos]).toInt()]
        return attackedFields
    }
}
package movegen

import model.Move
import model.BitboardState

object BishopMoves {
    val BISHOP_BLOCKER_MASKS = precomputeBishopMasks()
    val BISHOP_MAGIC_SHIFTS = computeShifts()
    val BISHOP_MAGIC_NUMBERS = arrayOf(
        0x62e0200408504048uL, 0x2ac4846282020380uL, 0xc190208483080682uL, 0xd8282042c0108029uL, 0x1422021000788848uL, 0x9003039840000038uL, 0x290d480a401040uL, 0x8c020700c8064801uL, 0x1114a01899030c00uL, 0x220802040146uL, 0x272e080825007404uL, 0xb8ac182481001806uL, 0x14a44106872de04uL, 0x424602500580c0uL, 0x2404108627202100uL, 0x81011c86089004e0uL, 0x8c0015d9ca80200uL, 0x4420402a521a1601uL, 0x85000680ac01022uL, 0x816800018200440duL, 0x5c04048080a04848uL, 0x6050418201100101uL, 0x251108405080206uL, 0x5c4080130400a604uL, 0x88e2683642104408uL, 0x24482002380d0505uL, 0xc0882800901180e4uL, 0x32a8080080a20120uL, 0xe019080507004000uL, 0x40ac2008d011510uL, 0x3004044309081204uL, 0x18ac029081620101uL, 0x1090500504b00400uL, 0xc4880210428e0401uL, 0x320d04800300081uL, 0x340a00800d90304uL, 0xc482128400aa0020uL, 0x60610a200810800uL, 0x2418280149198209uL, 0xb021160600058140uL, 0x5557245040800481uL, 0x494788241009825euL, 0x80034006804cc06uL, 0x490102204215801uL, 0x4003286300402c01uL, 0xd22400848d000202uL, 0x169d900083240645uL, 0x100e348206900a01uL, 0x784d5230142115b1uL, 0x9480c414a808621duL, 0x800080a68c106016uL, 0x900032e0e0880524uL, 0x2244401102120444uL, 0x206085041460400uL, 0x1ba008a148008630uL, 0xd141302140900c4uL, 0x126020501a82414uL, 0x4aa222a01142045uL, 0x112260084cc41016uL, 0x88138018282a0800uL, 0x240b086810060a00uL, 0x129a044104082980uL, 0x1521409c682e0059uL, 0x10901010410160a0uL)
    val BISHOP_ATTACKS_LOOKUP = precomputeBishopAttacks()
    val DIAGONAL_MASKS = computeDiagonalMasks()

    fun generateBishopMoves(state: BitboardState,pinnedPieces: ULong, moves: MutableList<Move>, checkingFigurePos: Int){
        val isWhite = state.whiteToMove
        var bishops = state.bishops or state.queens and if(isWhite) state.whitePieces else state.blackPieces
        val ourPieces = if(isWhite) state.whitePieces else state.blackPieces
        val ourKingPos = (ourPieces and state.kings).countTrailingZeroBits()

        while(bishops != 0uL){
            val from = bishops.countTrailingZeroBits()
            val movedPiece = if(BitboardAnalyzer.SINGLE_BIT_MASKS[from] and state.queens != 0uL) Move.PIECE_QUEEN else Move.PIECE_BISHOP
            val isPinned = (BitboardAnalyzer.SINGLE_BIT_MASKS[from] and pinnedPieces) != 0uL
            var possibleMoves = getAttackedFieldsMask(state,from) and ourPieces.inv()

            while(possibleMoves != 0uL){
                val to = possibleMoves.countTrailingZeroBits()
                if(BitboardAnalyzer.BLOCK_MOVES_LOOKUP[ourKingPos][checkingFigurePos][to] &&
                    (!isPinned || BitboardAnalyzer.PINNED_MOVES_LOOKUP[ourKingPos][from][to])) {
                    moves.add(Move.create(from, to, movedPiece))
                }
                possibleMoves = possibleMoves xor BitboardAnalyzer.SINGLE_BIT_MASKS[to]
            }
            bishops = bishops xor BitboardAnalyzer.SINGLE_BIT_MASKS[from]
        }

    }

    private fun precomputeBishopMasks():Array<ULong>{
        val masks = Array(64){0uL}
        for(square in 0 until 64){
            masks[square] = generateBishopMask(square)
        }
        return masks
    }

    private fun generateBishopMask(square: Int): ULong{
        var mask = 0uL
        val file = square % 8
        val rank = square / 8

        var f = file + 1
        var r = rank + 1
        while (f < 7 && r < 7){
            mask = mask or (1uL shl (8 * r + f))
            r++
            f++
        }

        f = file + 1
        r = rank - 1
        while(f<7 && r > 0 ){
            mask = mask or (1uL shl (8 * r + f))
            r--
            f++
        }

        f = file - 1
        r = rank + 1
        while(f > 0 && r < 7){
            mask = mask or (1uL shl (8 * r + f))
            f--
            r++
        }

        f = file - 1
        r = rank - 1
        while(f > 0 && r > 0 ){
            mask = mask or (1uL shl (8 * r + f))
            f--
            r--
        }

        return mask
    }

    private fun computeShifts(): Array<Int>{
        val shifts = Array(64){0}
        for(square in 0 until 64){
            shifts[square] = 64 - BISHOP_BLOCKER_MASKS[square].countOneBits()
        }
        return shifts
    }

    fun generateBlockers(mask: ULong, index: Int): ULong{
        var blockers = 0uL
        var bits = mask
        var idx = index
        var bitCount = 0

        while (bits != 0uL) {
            val lsb = BitboardAnalyzer.SINGLE_BIT_MASKS[bits.countTrailingZeroBits()]
            if ((idx and (1 shl bitCount)) != 0) {
                blockers = blockers or lsb
            }
            bits = bits xor lsb
            bitCount++
        }
        return blockers
    }

    fun computeBishopAttacks(square: Int, blockers: ULong): ULong {
        var attacks = 0uL
        val file = square % 8
        val rank = square / 8

        var r = rank + 1
        var f = file - 1
        while (r < 8 && f >= 0) {
            attacks = attacks or (1uL shl (r * 8 + f))
            if (blockers and (1uL shl (r * 8 + f)) != 0uL) break
            r++
            f--
        }

        r = rank + 1
        f = file + 1
        while (r < 8 && f < 8) {
            attacks = attacks or (1uL shl (r * 8 + f))
            if (blockers and (1uL shl (r * 8 + f)) != 0uL) break
            r++
            f++
        }

        r = rank - 1
        f = file - 1
        while (r >= 0 && f >= 0) {
            attacks = attacks or (1uL shl (r * 8 + f))
            if (blockers and (1uL shl (r * 8 + f)) != 0uL) break
            r--
            f--
        }

        r = rank - 1
        f = file + 1
        while (r >= 0 && f < 8) {
            attacks = attacks or (1uL shl (r * 8 + f))
            if (blockers and (1uL shl (r * 8 + f)) != 0uL) break
            r--
            f++
        }

        return attacks
    }
    private fun precomputeBishopAttacks(): Array<Array<ULong>>{
        val attacks = Array(64){square -> Array(1 shl (64- BISHOP_MAGIC_SHIFTS[square])){0uL} }

        for(square in 0 until 64){
            val attackMask = BISHOP_BLOCKER_MASKS[square]
            val relevantBits = attackMask.countOneBits()
            val blockerPermutations = 1 shl relevantBits
            for(i in 0 until blockerPermutations){
                val blockers = generateBlockers(attackMask,i)
                val magicIndex = (blockers * BISHOP_MAGIC_NUMBERS[square]) shr (64 - relevantBits)

                attacks[square][magicIndex.toInt()] = computeBishopAttacks(square,blockers)
            }
        }
        return attacks
    }
    fun getAttackedFieldsMask(state: BitboardState,pos: Int): ULong{
        val allPieces = state.blackPieces or state.whitePieces
        val blockers = BISHOP_BLOCKER_MASKS[pos] and allPieces
        val attackedFields = BISHOP_ATTACKS_LOOKUP[pos][((BISHOP_MAGIC_NUMBERS[pos] * blockers) shr BISHOP_MAGIC_SHIFTS[pos]).toInt()]
        return attackedFields
    }

    private fun computeDiagonalMasks(): Array<ULong> {
        val masks = Array(64){0uL}
        for (square in 0 until 64) {
            masks[square] = computeDiagonalMask(square)
        }
        return masks
    }

    private fun computeDiagonalMask(square: Int): ULong {
        val file = square % 8
        val rank = square / 8
        var mask = 0uL

        var f = file
        var r = rank
        while (f < 7 && r < 7) { f++; r++; mask = mask or (1uL shl (r * 8 + f)) }
        f = file; r = rank
        while (f > 0 && r > 0) { f--; r--; mask = mask or (1uL shl (r * 8 + f)) }

        f = file; r = rank
        while (f < 7 && r > 0) { f++; r--; mask = mask or (1uL shl (r * 8 + f)) }
        f = file; r = rank
        while (f > 0 && r < 7) { f--; r++; mask = mask or (1uL shl (r * 8 + f)) }

        return mask
    }
}
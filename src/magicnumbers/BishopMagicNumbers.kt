package magicnumbers

import movegen.BishopMoves.BISHOP_BLOCKER_MASKS
import movegen.BishopMoves.computeBishopAttacks
import movegen.BishopMoves.generateBlockers
import kotlin.random.Random
import kotlin.random.nextULong

fun main() {
    val magicNumbers = precomputeMagicNumbers()

    println("val BISHOP_MAGIC_NUMBERS = arrayOf(")
    for (i in 0 until 64) {
        print("0x${magicNumbers[i].toString(16)}uL")
        if (i < 63) print(", ")
    }
    println(")")
}

private fun precomputeMagicNumbers(): Array<ULong> {
    val magics = Array(64) { 0uL }
    for (square in 0 until 64) {
        magics[square] = findMagicNumber(square)
        println("Generated magic number for square $square: 0x${magics[square].toString(16)}")
    }
    return magics
}

private fun findMagicNumber(square: Int): ULong {
    val attackMask = BISHOP_BLOCKER_MASKS[square]
    val relevantBits = attackMask.countOneBits()
    val blockerPermutations = 1 shl relevantBits

    // Pre-calculate blockers and attacks
    val blockerList = Array(blockerPermutations) { i -> generateBlockers(attackMask, i) }
    val attackList = Array(blockerPermutations) { i -> computeBishopAttacks(square, blockerList[i]) }

    // Track used indices with boolean array
    val used = BooleanArray(blockerPermutations)

    while (true) {
        val candidate = Random.nextULong() and Random.nextULong() // Sparse random numbers
        used.fill(false)
        var valid = true

        for (i in 0 until blockerPermutations) {
            val index = ((blockerList[i] * candidate) shr (64 - relevantBits)).toInt()

            if (!used[index]) {
                used[index] = true
            } else if (attackList[i] != attackList[getFirstCollision(used, blockerList, candidate, relevantBits, i)]) {
                valid = false
                break
            }
        }

        if (valid) return candidate
    }
}


private fun getFirstCollision(
    used: BooleanArray,
    blockerList: Array<ULong>,
    candidate: ULong,
    relevantBits: Int,
    currentIndex: Int
): Int {
    val targetIndex = ((blockerList[currentIndex] * candidate) shr (64 - relevantBits)).toInt()

    for (i in 0 until currentIndex) {
        if (((blockerList[i] * candidate) shr (64 - relevantBits)).toInt() == targetIndex) {
            return i
        }
    }
    return -1
}
package magicnumbers

import movegen.RookMoves.ROOK_BLOCKER_MASKS
import movegen.RookMoves.computeRookAttacks
import movegen.RookMoves.generateBlockers
import kotlin.random.Random
import kotlin.random.nextULong

fun main(){
    val magicNumbers = precomputeMagicNumbers()

    println("VAL ROOK_MAGIC_NUMBERS = arrayOf(")
    for (i in 0 until 64) {
        print("0x${magicNumbers[i].toString(16)}uL")
        if (i < 63) print(", ")

    }
    println(")")
}

private fun precomputeMagicNumbers(): Array<ULong>{
    val magics = Array(64){0uL}
    for(square in 0 until 64){
        magics[square] = findMagicNumber(square)
    }
    return magics
}
private fun findMagicNumber(square: Int): ULong {
    val attackMask = ROOK_BLOCKER_MASKS[square]
    val relevantBits = attackMask.countOneBits()
    val blockerPermutations = 1 shl relevantBits

    // Pre-calculate blockers and attacks once
    val blockerList = Array(blockerPermutations) { i -> generateBlockers(attackMask, i) }
    val attackList = Array(blockerPermutations) { i -> computeRookAttacks(square, blockerList[i]) }

    // Use smaller array for failed attempts tracking
    val used = BooleanArray(blockerPermutations)

    // Fallback to random search if pattern-based search fails
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
    // Find the first index that collided with the current index
    val targetIndex = ((blockerList[currentIndex] * candidate) shr (64 - relevantBits)).toInt()

    for (i in 0 until currentIndex) {
        if (((blockerList[i] * candidate) shr (64 - relevantBits)).toInt() == targetIndex) {
            return i
        }
    }
    return -1
}
package model

data class BitboardState(
    var pawns: ULong = 0uL,
    var knights: ULong = 0uL,
    var bishops: ULong = 0uL,
    var rooks: ULong = 0uL,
    var queens: ULong = 0uL,
    var kings: ULong = 0uL,

    var whitePieces: ULong = 0uL,
    var blackPieces: ULong = 0uL,

    var whiteToMove: Boolean = true,
    var castlingRights: Int = 0b1111,
    var enPassantTarget: ULong = 0uL,
    var halfMoveCLock: Int = 0,
    var fullMoveNumber: Int = 1
)
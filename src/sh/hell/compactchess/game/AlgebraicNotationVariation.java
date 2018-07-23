package sh.hell.compactchess.game;

public enum AlgebraicNotationVariation
{
	/**
	 * Standard Algebraic Notation as used in PGN.
	 */
	SAN,
	/**
	 * FIDE SAN is SAN with the difference that castling is indicated using the digit zero instead of an uppercase O.
	 */
	FIDE_SAN,
	/**
	 * Figurine Algebraic Notation uses Unicode Symbols instead of letters, e.g. Rxd6 -> ♖xd6
	 */
	FAN,
	/**
	 * Minimal Algebraic Notation omits the indicators for capture ("x") and ("+"), e.g. Rook takes d6 -> Rd6.
	 */
	MAN,
	/**
	 * Long Algebraic Notation always includes the starting file and rank, followed by a dash unless the move is a capture, e.g. e2-e4.
	 */
	LAN,
	/**
	 * Reversible Algebraic Notation is LAN with the difference that also the captured piece is being indicated, e.g. Rd2xBd6.
	 */
	RAN
}

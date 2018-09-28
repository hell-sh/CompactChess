package sh.hell.compactchess.game;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public enum Language
{
	ENGLISH("P", "N", "B", "R", "Q", "K"),
	CZECH("P", "J", "S", "V", "D", "K"),
	DANISH("B", "S", "L", "T", "D", "K"),
	DUTCH("O", "P", "L", "T", "D", "K"),
	ESTONIAN("P", "R", "O", "V", "L", "K"),
	FINNISH("P", "R", "L", "T", "D", "K"),
	FRENCH("P", "C", "F", "T", "D", "R"),
	GERMAN("B", "S", "L", "T", "D", "K"),
	HUNGARIAN("G", "H", "F", "B", "V", "K"),
	ICELANDIC("P", "R", "B", "H", "D", "K"),
	ITALIAN("P", "C", "A", "T", "D", "R"),
	NORWEGIAN("B", "S", "L", "T", "D", "K"),
	POLISH("P", "S", "G", "W", "H", "K"),
	PORTUGUESE("P", "C", "B", "T", "D", "R"),
	ROMANIAN("P", "C", "N", "T", "D", "R"),
	SPANISH("P", "C", "A", "T", "D", "R"),
	SWEDISH("B", "S", "L", "T", "D", "K");

	public final String pawnChar;
	public final String knightChar;
	public final String bishopChar;
	public final String rookChar;
	public final String queenChar;
	public final String kingChar;

	Language(String pawnChar, String knightChar, String bishopChar, String rookChar, String queenChar, String kingChar)
	{
		this.pawnChar = pawnChar;
		this.knightChar = knightChar;
		this.bishopChar = bishopChar;
		this.rookChar = rookChar;
		this.queenChar = queenChar;
		this.kingChar = kingChar;
	}

	public PieceType pieceFromChar(String c)
	{
		if(c.equalsIgnoreCase(this.pawnChar))
		{
			return PieceType.PAWN;
		}
		if(c.equalsIgnoreCase(this.knightChar))
		{
			return PieceType.KNIGHT;
		}
		if(c.equalsIgnoreCase(this.bishopChar))
		{
			return PieceType.BISHOP;
		}
		if(c.equalsIgnoreCase(this.rookChar))
		{
			return PieceType.ROOK;
		}
		if(c.equalsIgnoreCase(this.queenChar))
		{
			return PieceType.QUEEN;
		}
		if(c.equalsIgnoreCase(this.kingChar))
		{
			return PieceType.KING;
		}
		return null;
	}
}

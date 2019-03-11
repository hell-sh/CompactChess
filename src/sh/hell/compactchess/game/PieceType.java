package sh.hell.compactchess.game;

public enum PieceType
{
	PAWN(1, "♙", "♟"),
	KNIGHT(3, "♘", "♞"),
	BISHOP(3, "♗", "♝"),
	ROOK(5, "♖", "♜"),
	QUEEN(9, "♕", "♛"),
	KING(0, "♔", "♚");

	public final byte materialValue;
	public final String whiteSymbol;
	public final String blackSymbol;

	PieceType(int materialValue, String whiteSymbol, String blackSymbol)
	{
		this.materialValue = (byte) materialValue;
		this.whiteSymbol = whiteSymbol;
		this.blackSymbol = blackSymbol;
	}

	public static PieceType fromOrdinal(final byte ordinal)
	{
		for(PieceType pieceType : PieceType.values())
		{
			if(pieceType.ordinal() == ordinal)
			{
				return pieceType;
			}
		}
		return null;
	}

	public String getChar(Language language)
	{
		if(this == PAWN)
		{
			return language.pawnChar;
		}
		if(this == KNIGHT)
		{
			return language.knightChar;
		}
		if(this == BISHOP)
		{
			return language.bishopChar;
		}
		if(this == ROOK)
		{
			return language.rookChar;
		}
		if(this == QUEEN)
		{
			return language.queenChar;
		}
		if(this == KING)
		{
			return language.kingChar;
		}
		throw new RuntimeException();
	}
}

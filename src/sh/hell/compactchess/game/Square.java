package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Square
{
	public final byte file;
	public final byte rank;
	public Color pieceColor = null;
	public PieceType pieceType = null;

	Square(final byte file, final byte rank)
	{
		this.file = file;
		this.rank = rank;
	}

	Square(final byte file, final byte rank, Color pieceColor, final PieceType pieceType)
	{
		this.file = file;
		this.rank = rank;
		this.pieceColor = pieceColor;
		this.pieceType = pieceType;
	}

	static byte index(final byte file, final byte rank)
	{
		return (byte) ((rank * 8) + file);
	}

	public static byte file(String file) throws ChessException
	{
		switch(file)
		{
			case "a":
				return 0;
			case "b":
				return 1;
			case "c":
				return 2;
			case "d":
				return 3;
			case "e":
				return 4;
			case "f":
				return 5;
			case "g":
				return 6;
			case "h":
				return 7;
			default:
				throw new ChessException("Invalid File: " + file);
		}
	}

	public static String fileChar(byte file) throws ChessException
	{
		switch(file)
		{
			case 0:
				return "a";
			case 1:
				return "b";
			case 2:
				return "c";
			case 3:
				return "d";
			case 4:
				return "e";
			case 5:
				return "f";
			case 6:
				return "g";
			case 7:
				return "h";
			default:
				throw new ChessException("Invalid File: " + file);
		}
	}

	public boolean hasPiece()
	{
		return this.pieceType != null;
	}

	public byte index()
	{
		return Square.index(file, rank);
	}

	public String getAlgebraicNotation()
	{
		return this.getFileChar().toLowerCase() + (this.rank + 1);
	}

	public String getCharacter(Language language)
	{
		if(this.hasPiece())
		{
			return (pieceColor == Color.WHITE ? pieceType.getDisplayChar(language).toUpperCase() : pieceType.getDisplayChar(language).toLowerCase());
		}
		return " ";
	}

	public boolean isWhite()
	{
		return ((file & 1) == 0 ^ (rank & 1) == 0);
	}

	public boolean isBlack()
	{
		return !isWhite();
	}

	public String getSymbol(final boolean invertColor)
	{
		if(this.hasPiece())
		{
			return ((pieceColor == Color.WHITE) ^ invertColor ? pieceType.whiteSymbol : pieceType.blackSymbol);
		}
		return (isWhite() ^ invertColor ? "□" : "■");
	}

	public String getSVG()
	{
		return (pieceColor == Color.WHITE ? pieceType.whiteSVG : pieceType.blackSVG);
	}

	public byte getMaterialValue()
	{
		if(this.pieceType != null)
		{
			return this.pieceType.materialValue;
		}
		return 0;
	}

	public String getFileChar()
	{
		try
		{
			return Square.fileChar(file);
		}
		catch(ChessException ignored)
		{
		}
		return null;
	}

	public String toString()
	{
		return "{Square " + getAlgebraicNotation() + " containing " + pieceColor + " " + pieceType + "}";
	}

	public Square copy()
	{
		return new Square(file, rank, pieceColor, pieceType);
	}

	public boolean coordinateEquals(final Object o2)
	{
		return o2 instanceof Square && this.file == ((Square) o2).file && this.rank == ((Square) o2).rank;
	}

	@Override
	public boolean equals(final Object o2)
	{
		return o2 instanceof Square && this.file == ((Square) o2).file && this.rank == ((Square) o2).rank && this.pieceColor == ((Square) o2).pieceColor && this.pieceType == ((Square) o2).pieceType;
	}
}

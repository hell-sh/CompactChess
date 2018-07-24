package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;

public class Square
{
	public final byte file;
	public final byte rank;
	private Piece piece;

	Square(final byte file, final byte rank)
	{
		this.file = file;
		this.rank = rank;
	}

	Square(final byte file, final byte rank, final Piece piece)
	{
		this.file = file;
		this.rank = rank;
		this.piece = piece;
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
		return this.piece != null;
	}

	public Piece getPiece()
	{
		return piece;
	}

	void setPiece(final Piece piece)
	{
		this.piece = piece;
	}

	void unsetPiece()
	{
		this.piece = null;
	}

	byte index()
	{
		return Square.index(file, rank);
	}

	public String getAlgebraicNotation() throws ChessException
	{
		return this.getFileChar().toLowerCase() + (this.rank + 1);
	}

	public String getCharacter()
	{
		if(this.piece == null)
		{
			return " ";
		}
		else
		{
			return this.getPiece().getCharacter();
		}
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
		if(this.piece == null)
		{
			return ((invertColor ? isBlack() : isWhite()) ? "□" : "■");
		}
		else
		{
			return this.getPiece().getSymbol(invertColor);
		}
	}

	public String getFileChar() throws ChessException
	{
		return Square.fileChar(file);
	}

	public String toString()
	{
		try
		{
			if(piece != null)
			{
				return "{Square " + getAlgebraicNotation() + " with " + getPiece().toString() + "}";
			}
			return "{Square " + getAlgebraicNotation() + "}";
		}
		catch(ChessException e)
		{
			e.printStackTrace();
		}
		return "{Square}";
	}

	public Square copy()
	{
		Square square = new Square(file, rank);
		if(piece != null)
		{
			square.setPiece(getPiece().copy(square));
			return square;
		}
		return square;
	}

	Square copy(final Piece piece)
	{
		return new Square(file, rank, piece);
	}

	public boolean equals(final Object o2)
	{
		return o2 instanceof Square && (this.file == ((Square) o2).file && this.rank == ((Square) o2).rank);
	}
}

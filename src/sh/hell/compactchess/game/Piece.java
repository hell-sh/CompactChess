package sh.hell.compactchess.game;

import java.lang.ref.WeakReference;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Piece
{
	public final Color color;
	public PieceType type;
	private WeakReference<Square> square;

	public Piece(final PieceType type, final Color color)
	{
		this.type = type;
		this.color = color;
	}

	public Piece(final PieceType type, final Color color, final Square square)
	{
		this.type = type;
		this.color = color;
		this.setSquare(square);
	}

	public boolean hasSquare()
	{
		return this.square != null;
	}

	public Square getSquare()
	{
		if(square == null)
		{
			return null;
		}
		return square.get();
	}

	void setSquare(final Square square)
	{
		this.square = new WeakReference<>(square);
	}

	public String getCharacter(Language language)
	{
		return (color == Color.WHITE ? type.getDisplayChar(language).toUpperCase() : type.getDisplayChar(language).toLowerCase());
	}

	public String getSymbol(final boolean invertColor)
	{
		boolean isWhite = (color == Color.WHITE);
		if(invertColor)
		{
			isWhite = !isWhite;
		}
		return (isWhite ? type.whiteSymbol : type.blackSymbol);
	}

	public byte getValue()
	{
		return this.type.value;
	}

	public String getSVG()
	{
		return (color == Color.WHITE ? type.whiteSVG : type.blackSVG);
	}

	public String toString()
	{
		if(!this.hasSquare())
		{
			return this.toStringLite();
		}
		return "{Piece " + color.name() + " " + type.name() + " on " + this.getSquare().toStringLite() + "}";
	}

	public String toStringLite()
	{
		return "{Piece " + color.name() + " " + type.name() + "}";
	}

	public Piece copy()
	{
		final Piece piece = new Piece(type, color, getSquare().copy());
		if(square != null)
		{
			piece.setSquare(getSquare().copy(piece));
		}
		return piece;
	}

	Piece copy(final Square square)
	{
		return new Piece(type, color, square);
	}

	@Override
	public boolean equals(Object o2)
	{
		return o2 instanceof Piece && ((Piece) o2).color == this.color && ((Piece) o2).type == this.type && ((Piece) o2).square == this.square;
	}
}

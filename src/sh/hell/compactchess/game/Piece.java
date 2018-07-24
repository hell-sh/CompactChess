package sh.hell.compactchess.game;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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

	public static Piece fromNotationChar(final String notationChar)
	{
		return new Piece(PieceType.fromDisplayChar(notationChar), (notationChar.toUpperCase().equals(notationChar) ? Color.WHITE : Color.BLACK));
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

	public String getCharacter()
	{
		return (color == Color.WHITE ? type.displayChar.toUpperCase() : type.displayChar.toLowerCase());
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

	public ArrayList<Square> getControlledSquares(Game game)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		final Square square = getSquare();
		if(type == PieceType.PAWN)
		{
			final byte file = square.file;
			final byte rank = square.rank;
			Square square_;
			if(color == Color.WHITE ? rank < 7 : rank > 0)
			{
				square_ = game.square(file, (byte) (rank + (color == Color.WHITE ? 1 : -1)));
				if(!square_.hasPiece())
				{
					squares.add(game.square(file, (byte) (rank + (color == Color.WHITE ? 1 : -1))));
					if((rank == 1 && color == Color.WHITE) || (rank == 6 && color == Color.BLACK))
					{
						square_ = game.square(file, (byte) (rank + (color == Color.WHITE ? 2 : -2)));
						if(!square_.hasPiece())
						{
							squares.add(game.square(file, (byte) (rank + (color == Color.WHITE ? 2 : -2))));
						}
					}
				}
			}
			if(file > 0)
			{
				square_ = game.square((byte) (file - 1), (byte) (rank + (color == Color.WHITE ? 1 : -1)));
				if((square_.hasPiece() && square_.getPiece().color != this.color) || square_.equals(game.enPassantSquare))
				{
					squares.add(square_);
				}
			}
			if(file < 7)
			{
				square_ = game.square((byte) (file + 1), (byte) (rank + (color == Color.WHITE ? 1 : -1)));
				if((square_.hasPiece() && square_.getPiece().color != this.color) || square_.equals(game.enPassantSquare))
				{
					squares.add(square_);
				}
			}
		}
		else if(type == PieceType.KING)
		{
			final byte file = square.file;
			final byte rank = square.rank;
			for(byte file_add = -1; file_add <= 1; file_add++)
			{
				final byte file_ = (byte) (file + file_add);
				if(file_ >= 0 && file_ < 8)
				{
					for(byte rank_add = -1; rank_add <= 1; rank_add++)
					{
						final byte rank_ = (byte) (rank + rank_add);
						if(rank_ >= 0 && rank_ < 8)
						{
							final Square square_ = game.square(file_, rank_);
							if(!square_.hasPiece() || square_.getPiece().color != this.color)
							{
								squares.add(square_);
							}
						}
					}
				}
			}
		}
		else if(type == PieceType.KNIGHT)
		{
			if(square.rank > 1)
			{
				if(square.file > 0)
				{
					final Square square_ = game.square((byte) (square.file - 1), (byte) (square.rank - 2));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
				if(square.file < 7)
				{
					final Square square_ = game.square((byte) (square.file + 1), (byte) (square.rank - 2));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
			}
			if(square.rank < 6)
			{
				if(square.file > 0)
				{
					final Square square_ = game.square((byte) (square.file - 1), (byte) (square.rank + 2));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
				if(square.file < 7)
				{
					final Square square_ = game.square((byte) (square.file + 1), (byte) (square.rank + 2));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
			}
			if(square.file > 1)
			{
				if(square.rank > 0)
				{
					final Square square_ = game.square((byte) (square.file - 2), (byte) (square.rank - 1));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
				if(square.rank < 7)
				{
					final Square square_ = game.square((byte) (square.file - 2), (byte) (square.rank + 1));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
			}
			if(square.file < 6)
			{
				if(square.rank > 0)
				{
					final Square square_ = game.square((byte) (square.file + 2), (byte) (square.rank - 1));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
				if(square.rank < 7)
				{
					final Square square_ = game.square((byte) (square.file + 2), (byte) (square.rank + 1));
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
				}
			}
		}
		else
		{
			if(type == PieceType.ROOK || type == PieceType.QUEEN)
			{
				byte rank = square.rank;
				while(rank < 7)
				{
					rank++;
					final Square square_ = game.square(square.file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				rank = square.rank;
				while(rank > 0)
				{
					rank--;
					final Square square_ = game.square(square.file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				byte file = square.file;
				while(file < 7)
				{
					file++;
					final Square square_ = game.square(file, square.rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				file = square.file;
				while(file > 0)
				{
					file--;
					final Square square_ = game.square(file, square.rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
			}
			if(type == PieceType.BISHOP || type == PieceType.QUEEN)
			{
				byte file = square.file;
				byte rank = square.rank;
				while(file < 7 && rank < 7)
				{
					file++;
					rank++;
					final Square square_ = game.square(file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				file = square.file;
				rank = square.rank;
				while(file < 7 && rank > 0)
				{
					file++;
					rank--;
					final Square square_ = game.square(file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				file = square.file;
				rank = square.rank;
				while(file > 0 && rank < 7)
				{
					file--;
					rank++;
					final Square square_ = game.square(file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
				file = square.file;
				rank = square.rank;
				while(file > 0 && rank > 0)
				{
					file--;
					rank--;
					final Square square_ = game.square(file, rank);
					if(!square_.hasPiece() || square_.getPiece().color != color)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
			}
		}
		return squares;
	}

	public String getSVG()
	{
		return (color == Color.WHITE ? type.whiteSVG : type.blackSVG);
	}

	public String toString()
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

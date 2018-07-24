package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static sh.hell.compactchess.game.CastlingType.KINGSIDE;
import static sh.hell.compactchess.game.CastlingType.NONE;
import static sh.hell.compactchess.game.CastlingType.QUEENSIDE;

public class Move
{
	public final Square fromSquare;
	public final Square toSquare;
	public final PieceType promoteTo;
	public final boolean isEnPassant;
	public final CastlingType castlingType;
	private final WeakReference<Game> game;
	private final Game _game;
	public String annotation = "";

	Move(Game game, Square fromSquare, Square toSquare) throws ChessException
	{
		this(game, fromSquare, toSquare, null);
	}

	Move(Game game, Square fromSquare, Square toSquare, PieceType promoteTo) throws ChessException
	{
		if(game.status == GameStatus.BUILDING)
		{
			throw new ChessException("The game has not started yet");
		}
		if(!fromSquare.hasPiece())
		{
			throw new ChessException("There's no piece on " + fromSquare.getAlgebraicNotation());
		}
		if(fromSquare.equals(toSquare))
		{
			throw new ChessException("Can't move to the same square");
		}
		if(promoteTo != null && fromSquare.getPiece().type != PieceType.PAWN)
		{
			throw new ChessException("Only pawns can promote");
		}
		if(promoteTo == PieceType.PAWN)
		{
			throw new ChessException("Promoting to a pawn makes no sense");
		}
		if(promoteTo == PieceType.KING && game.variant != Variant.ANTICHESS)
		{
			throw new ChessException("You can't promote to king in " + game.variant.name());
		}
		this.game = new WeakReference<>(game);
		this._game = game.copy();
		this.fromSquare = fromSquare;
		this.toSquare = toSquare;
		this.promoteTo = promoteTo;
		this.isEnPassant = toSquare.equals(game.enPassantSquare);
		final Piece piece = fromSquare.getPiece();
		if(piece.type == PieceType.KING && fromSquare.file == 4)
		{
			if((piece.color == Color.WHITE && fromSquare.rank == 0) || (piece.color == Color.BLACK && fromSquare.rank == 7))
			{
				if(toSquare.file == 6)
				{
					this.castlingType = KINGSIDE;
				}
				else if(toSquare.file == 2)
				{
					this.castlingType = QUEENSIDE;
				}
				else
				{
					this.castlingType = NONE;
				}
			}
			else
			{
				this.castlingType = NONE;
			}
		}
		else
		{
			this.castlingType = NONE;
		}
	}

	public Move annotate(String annotation)
	{
		this.annotation = annotation;
		return this;
	}

	private void handle(Game game, boolean doCounting) throws ChessException
	{
		if(doCounting)
		{
			switch(this.castlingType)
			{
				case KINGSIDE:
					new Move(game, game.square((byte) 7, fromSquare.rank).copy(), game.square((byte) 5, fromSquare.rank).copy()).handle(game, false);
					break;

				case QUEENSIDE:
					new Move(game, game.square((byte) 0, fromSquare.rank).copy(), game.square((byte) 3, fromSquare.rank).copy()).handle(game, false);
					break;
			}
		}
		Square fromSquare = game.square(this.fromSquare);
		Square toSquare = game.square(this.toSquare);
		boolean capture = false;
		if(toSquare.hasPiece())
		{
			capture = true;
			synchronized(game.pieces)
			{
				game.pieces.remove(toSquare.getPiece());
			}
		}
		toSquare.setPiece(fromSquare.getPiece());
		toSquare.getPiece().setSquare(toSquare);
		if(fromSquare.getPiece().type == PieceType.PAWN)
		{
			if(promoteTo != null)
			{
				toSquare.getPiece().type = promoteTo;
			}
			if(doCounting)
			{
				game.hundredPliesRuleTimer = 0;
			}
		}
		else if(doCounting)
		{
			if(capture)
			{
				game.hundredPliesRuleTimer = 0;
			}
			else
			{
				game.hundredPliesRuleTimer++;
			}
		}
		fromSquare.unsetPiece();
	}

	Game commitTo(Game game, boolean dontCalculate) throws ChessException
	{
		if(toSquare.equals(game.enPassantSquare))
		{
			Square epPieceSquare;
			if(game.enPassantSquare.rank == 2)
			{
				epPieceSquare = game.square(game.enPassantSquare.file, (byte) 3);
			}
			else
			{
				epPieceSquare = game.square(game.enPassantSquare.file, (byte) 4);
			}
			if(epPieceSquare.hasPiece())
			{
				synchronized(game.pieces)
				{
					game.pieces.remove(epPieceSquare.getPiece());
				}
				epPieceSquare.unsetPiece();
			}
		}
		else
		{
			game.enPassantSquare = getEnPassantSquare();
		}
		this.handle(game, true);
		synchronized(game.moves)
		{
			game.moves.add(this);
		}
		if(game.timeControl != TimeControl.UNLIMITED)
		{
			if(game.plyStart > 0)
			{
				if(game.timeControl == TimeControl.INCREMENT)
				{
					if(game.toMove == Color.WHITE)
					{
						game.whitemsecs += game.increment;
					}
					else
					{
						game.blackmsecs += game.increment;
					}
				}
				if(game.toMove == Color.WHITE)
				{
					game.whitemsecs -= (System.currentTimeMillis() - game.plyStart);
				}
				else
				{
					game.blackmsecs -= (System.currentTimeMillis() - game.plyStart);
				}
				if(!dontCalculate)
				{
					if((game.toMove == Color.WHITE ? game.whitemsecs : game.blackmsecs) - (System.currentTimeMillis() - game.plyStart) <= 0)
					{
						game.endReason = EndReason.TIMEOUT;
					}
				}
			}
			if(game.plyCount > 1)
			{
				game.plyStart = System.currentTimeMillis();
			}
		}
		game.plyCount++;
		game.opponentToMove();
		if(!dontCalculate)
		{
			game.determineCastlingAbilities();
			boolean isCheck = game.isCheck();
			if(isCheck)
			{
				if(game.toMove == Color.WHITE)
				{
					game.whitechecks++;
				}
				else
				{
					game.blackchecks++;
				}
			}
			game.recalculateEndReason(isCheck);
		}
		else
		{
			game.recalculateStatus();
		}
		return game;
	}

	public Game commit() throws ChessException
	{
		return this.commit(false, false);
	}

	public Game commit(boolean allowIllegal, boolean dontCalculate) throws ChessException
	{
		if(!allowIllegal)
		{
			String illegalReason = this.getIllegalReason();
			if(illegalReason != null)
			{
				throw new ChessException("Move " + this.toUCI() + " is illegal: " + illegalReason);
			}
		}
		return this.commitTo(this.game.get(), dontCalculate);
	}

	public boolean isLegal() throws ChessException
	{
		return this.getIllegalReason() == null;
	}

	public String getIllegalReason() throws ChessException
	{
		if(this._game.variant == Variant.ANTICHESS)
		{
			if(!this._game.square(this.toSquare).hasPiece())
			{
				for(Square s : this._game.getSquaresControlledBy(this._game.toMove))
				{
					if(s.hasPiece())
					{
						return "You have to capture when you can";
					}
				}
			}
		}
		else
		{
			if(this._game.variant == Variant.RACING_KINGS && this.isCheck())
			{
				return "You can't put your opponent in check";
			}
			if(this.commitTo(this._game.copy(), true).opponentToMove().isCheck())
			{
				if(this._game.isCheck())
				{
					return "You need to get out of check";
				}
				else
				{
					return "This would put you in check";
				}
			}
			if(this._game.toMove == Color.WHITE)
			{
				if(this.castlingType == CastlingType.KINGSIDE)
				{
					if(!this._game.whiteCanCastle)
					{
						return "You can't castle kingside";
					}
					final ArrayList<Square> opponentControlledSquares = this._game.getSquaresControlledBy(Color.BLACK);
					if(opponentControlledSquares.contains(this._game.square("e1")))
					{
						return "You can't castle while in check";
					}
					if(this._game.square("f1").hasPiece())
					{
						return "You can't castle because f1 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("f1")))
					{
						return "You can't castle because f1 is under attack";
					}
					if(this._game.square("g1").hasPiece())
					{
						return "You can't castle because g1 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("g1")))
					{
						return "You can't castle because g1 is under attack";
					}
				}
				else if(this.castlingType == CastlingType.QUEENSIDE)
				{
					if(!this._game.whiteCanCastleQueenside)
					{
						return "You can't castle queenside";
					}
					final ArrayList<Square> opponentControlledSquares = this._game.getSquaresControlledBy(Color.BLACK);
					if(opponentControlledSquares.contains(this._game.square("e1")))
					{
						return "You can't castle while in check";
					}
					if(this._game.square("d1").hasPiece())
					{
						return "You can't castle because d1 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("d1")))
					{
						return "You can't castle because d1 is under attack";
					}
					if(this._game.square("c1").hasPiece())
					{
						return "You can't castle because c1 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("c1")))
					{
						return "You can't castle because c1 is under attack";
					}
				}
			}
			else if(this._game.toMove == Color.BLACK)
			{
				if(this.castlingType == CastlingType.KINGSIDE)
				{
					if(!this._game.blackCanCastle)
					{
						return "You can't castle kingside";
					}
					final ArrayList<Square> opponentControlledSquares = this._game.getSquaresControlledBy(Color.WHITE);
					if(opponentControlledSquares.contains(this._game.square("e8")))
					{
						return "You can't castle while in check";
					}
					if(this._game.square("f8").hasPiece())
					{
						return "You can't castle because f8 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("f8")))
					{
						return "You can't castle because f8 is under attack";
					}
					if(this._game.square("g8").hasPiece())
					{
						return "You can't castle because g8 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("g8")))
					{
						return "You can't castle because g8 is under attack";
					}
				}
				else if(this.castlingType == CastlingType.QUEENSIDE)
				{
					if(!this._game.blackCanCastleQueenside)
					{
						return "You can't castle queenside";
					}
					final ArrayList<Square> opponentControlledSquares = this._game.getSquaresControlledBy(Color.WHITE);
					if(opponentControlledSquares.contains(this._game.square("e8")))
					{
						return "You can't castle while in check";
					}
					if(this._game.square("c8").hasPiece())
					{
						return "You can't castle because c8 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("c8")))
					{
						return "You can't castle because d8 is under attack";
					}
					if(this._game.square("d1").hasPiece())
					{
						return "You can't castle because d1 is occupied";
					}
					if(opponentControlledSquares.contains(this._game.square("d8")))
					{
						return "You can't castle because d8 is under attack";
					}
				}
			}
			/*
			boolean canMove = false;
			for(Square square : this.fromSquare.getPiece().getControlledSquares(this._game))
			{
				if(this.toSquare.equals(square))
				{
					canMove = true;
					break;
				}
			}
			if(!canMove)
			{
				return this.fromSquare.getPiece().type.name() + " can't move to " + toSquare.getAlgebraicNotation();
			}
			*/
		}
		return null;
	}

	public boolean isCheck() throws ChessException
	{
		return this.commitTo(this._game.copy(), true).isCheck();
	}

	public boolean isStalemate() throws ChessException
	{
		return this.commitTo(this._game.copy(), true).isStalemate();
	}

	public boolean isStalemate(boolean isCheck) throws ChessException
	{
		return this.commitTo(this._game.copy(), true).isStalemate(isCheck);
	}

	public boolean isCheckmate() throws ChessException
	{
		return this.commitTo(this._game.copy(), true).isCheckmate();
	}

	public boolean isCheckmate(boolean isCheck) throws ChessException
	{
		return this.commitTo(this._game.copy(), true).isCheckmate(true);
	}

	public String toUCI() throws ChessException
	{
		StringBuilder uci = new StringBuilder(fromSquare.getAlgebraicNotation()).append(toSquare.getAlgebraicNotation());
		if(this.promoteTo != null)
		{
			uci.append(this.promoteTo.notationChar.toLowerCase());
		}
		return uci.toString();
	}

	public String toAlgebraicNotation() throws ChessException
	{
		return this.toAlgebraicNotation(AlgebraicNotationVariation.SAN);
	}

	public String toAlgebraicNotation(final AlgebraicNotationVariation variation) throws ChessException
	{
		StringBuilder an = new StringBuilder();
		switch(this.castlingType)
		{
			case QUEENSIDE:
				if(variation == AlgebraicNotationVariation.FIDE_SAN)
				{
					an.append("0-0-0");
				}
				else
				{
					an.append("O-O-O");
				}
				break;
			case KINGSIDE:
				if(variation == AlgebraicNotationVariation.FIDE_SAN)
				{
					an.append("0-0");
				}
				else
				{
					an.append("O-O");
				}
				break;
			default:
				final Piece piece = _game.square(fromSquare).getPiece();
				final boolean capture = _game.square(toSquare).hasPiece();
				if(piece.type != PieceType.PAWN)
				{
					if(variation == AlgebraicNotationVariation.FAN)
					{
						an.append(piece.getSymbol(false));
					}
					else
					{
						an.append(piece.type.notationChar);
					}
				}
				if(variation == AlgebraicNotationVariation.LAN || variation == AlgebraicNotationVariation.RAN)
				{
					an.append(fromSquare.getAlgebraicNotation());
				}
				else
				{
					final ArrayList<Piece> ambiguities = new ArrayList<>();
					for(Piece p : _game.pieces)
					{
						if(p.getSquare().equals(fromSquare) || p.color != piece.color || p.type != piece.type)
						{
							continue;
						}
						if(p.getControlledSquares(_game).contains(toSquare))
						{
							ambiguities.add(p);
						}
					}
					if(ambiguities.size() > 0 || (capture && piece.type == PieceType.PAWN))
					{
						boolean solutionWorks = true;
						for(Piece p : ambiguities)
						{
							if(p.getSquare().file == fromSquare.file)
							{
								solutionWorks = false;
								break;
							}
						}
						if(solutionWorks)
						{
							an.append(Square.fileChar(fromSquare.file));
						}
						else
						{
							solutionWorks = true;
							for(Piece p : ambiguities)
							{
								if(p.getSquare().rank == fromSquare.rank)
								{
									solutionWorks = false;
									break;
								}
							}
							if(solutionWorks)
							{
								an.append(fromSquare.rank + 1);
							}
							else
							{
								an.append(fromSquare.getAlgebraicNotation());
							}
						}
					}
				}
				if(variation != AlgebraicNotationVariation.MAN)
				{
					if(capture)
					{
						an.append("x");
						if(variation == AlgebraicNotationVariation.RAN)
						{
							final Piece piece_ = _game.square(toSquare).getPiece();
							if(piece_ != null)
							{
								an.append(piece_.type.displayChar);
							}
						}
					}
					else if(variation == AlgebraicNotationVariation.LAN || variation == AlgebraicNotationVariation.RAN)
					{
						an.append("-");
					}
				}
				an.append(toSquare.getAlgebraicNotation());
				if(this.promoteTo != null)
				{
					an.append("=");
					if(variation == AlgebraicNotationVariation.FAN)
					{
						an.append(piece.color == Color.WHITE ? this.promoteTo.whiteSymbol : this.promoteTo.blackSymbol);
					}
					else
					{
						an.append(this.promoteTo.notationChar);
					}
				}
				break;
		}
		if(isCheckmate())
		{
			an.append("#");
		}
		else if(variation != AlgebraicNotationVariation.MAN && this.isCheck())
		{
			an.append("+");
		}
		return an.toString();
	}

	public Square getEnPassantSquare()
	{
		Piece piece = _game.square(fromSquare).getPiece();
		if(piece.type == PieceType.PAWN)
		{
			if(piece.color == Color.WHITE && fromSquare.rank == 1)
			{
				return _game.square(fromSquare.file, (byte) (fromSquare.rank + 1));
			}
			else if(piece.color == Color.BLACK && fromSquare.rank == 6)
			{
				return _game.square(fromSquare.file, (byte) (fromSquare.rank - 1));
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o2)
	{
		if(o2 instanceof Move)
		{
			return this.fromSquare.equals(((Move) o2).fromSquare) && this.toSquare.equals(((Move) o2).toSquare) && ((this.promoteTo == null && ((Move) o2).promoteTo == null) || (this.promoteTo != null && ((Move) o2).promoteTo != null && this.promoteTo.equals(((Move) o2).promoteTo))) && this.isEnPassant == ((Move) o2).isEnPassant && this.castlingType == ((Move) o2).castlingType;
		}
		return false;
	}
}

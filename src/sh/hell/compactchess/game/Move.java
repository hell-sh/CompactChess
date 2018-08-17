package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.exceptions.InvalidMoveException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


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

	public Move(Game game, Square fromSquare, Square toSquare, PieceType promoteTo, boolean validate) throws ChessException
	{
		final Piece piece = fromSquare.getPiece();
		if(game.status == GameStatus.BUILDING)
		{
			throw new InvalidMoveException("The game has not started yet");
		}
		if(piece == null)
		{
			throw new InvalidMoveException("There's no piece on " + fromSquare.getAlgebraicNotation());
		}
		if(fromSquare.equals(toSquare))
		{
			throw new InvalidMoveException("Can't move to the same square");
		}
		if(promoteTo != null)
		{
			if(piece.type != PieceType.PAWN)
			{
				throw new InvalidMoveException("Only pawns can be promoted");
			}
			if(!game.variant.getPossiblePromotions().contains(promoteTo))
			{
				throw new InvalidMoveException("You can't promote to " + promoteTo.name().toLowerCase() + " in " + game.variant.name);
			}
		}
		if(piece.type == PieceType.KING && (piece.color == Color.WHITE ? (fromSquare.rank == 0 && (game.whiteCanCastle || game.whiteCanCastleQueenside)) : (fromSquare.rank == 7 && (game.blackCanCastle || game.blackCanCastleQueenside))))
		{
			if(game.variant == Variant.CHESS960)
			{
				final Piece toPiece = toSquare.getPiece();
				if(toPiece != null && toPiece.type == PieceType.ROOK && toPiece.color == piece.color)
				{
					if(toSquare.file > fromSquare.file)
					{
						this.castlingType = CastlingType.KINGSIDE;
					}
					else
					{
						this.castlingType = CastlingType.QUEENSIDE;
					}
				}
				else
				{
					this.castlingType = CastlingType.NONE;
				}
			}
			else if(fromSquare.file == 4)
			{
				if(toSquare.file == 6)
				{
					this.castlingType = CastlingType.KINGSIDE;
				}
				else if(toSquare.file == 2)
				{
					this.castlingType = CastlingType.QUEENSIDE;
				}
				else
				{
					this.castlingType = CastlingType.NONE;
				}
			}
			else
			{
				this.castlingType = CastlingType.NONE;
			}
		}
		else
		{
			this.castlingType = CastlingType.NONE;
		}
		if(validate && this.castlingType == CastlingType.NONE && !game.getSquaresControlledBy(fromSquare.getPiece()).contains(toSquare))
		{
			throw new InvalidMoveException("Your " + fromSquare.getPiece().type.name().toLowerCase() + " on " + fromSquare.getAlgebraicNotation() + " can't move to " + toSquare.getAlgebraicNotation());
		}
		this.game = new WeakReference<>(game);
		this._game = game.copy();
		this._game.tags.clear();
		this.fromSquare = fromSquare;
		this.toSquare = toSquare;
		this.promoteTo = promoteTo;
		this.isEnPassant = fromSquare.getPiece().type == PieceType.PAWN && toSquare.equals(game.enPassantSquare);
	}

	public Move annotate(String annotation)
	{
		this.annotation = annotation;
		return this;
	}

	private void handle(Game game, boolean doCounting, boolean dontCalculate) throws ChessException
	{
		Square fromSquare = game.square(this.fromSquare);
		Piece fromPiece = fromSquare.getPiece();
		fromSquare.unsetPiece();
		Square toSquare = null;
		if(doCounting && this.castlingType != CastlingType.NONE)
		{
			if(this.castlingType == CastlingType.KINGSIDE)
			{
				if(game.variant == Variant.CHESS960)
				{
					if(this.toSquare.file != 5)
					{
						new Move(game, game.square(this.toSquare), game.square((byte) 5, fromSquare.rank), null, false).handle(game, false, true);
					}
					toSquare = game.square((byte) 6, fromSquare.rank);
				}
				else
				{
					new Move(game, game.square((byte) 7, fromSquare.rank), game.square((byte) 5, fromSquare.rank), null, false).handle(game, false, true);
				}
			}
			else
			{
				if(game.variant == Variant.CHESS960)
				{
					if(this.toSquare.file != 3)
					{
						new Move(game, game.square(this.toSquare), game.square((byte) 3, fromSquare.rank), null, false).handle(game, false, true);
					}
					toSquare = game.square((byte) 2, fromSquare.rank);
				}
				else
				{
					new Move(game, game.square((byte) 0, fromSquare.rank), game.square((byte) 3, fromSquare.rank), null, false).handle(game, false, true);
				}
			}
		}
		if(toSquare == null)
		{
			toSquare = game.square(this.toSquare);
		}
		boolean capture = false;
		if(toSquare.hasPiece())
		{
			capture = true;
			synchronized(game.pieces)
			{
				if(!dontCalculate && toSquare.getPiece().type == PieceType.PAWN)
				{
					synchronized(game.repetitionPostitions)
					{
						game.repetitionPostitions.clear();
					}
				}
				game.pieces.remove(toSquare.getPiece());
			}
		}
		toSquare.setPiece(fromPiece);
		toSquare.getPiece().setSquare(toSquare);
		if(fromPiece.type == PieceType.PAWN)
		{
			if(promoteTo != null)
			{
				toSquare.getPiece().type = promoteTo;
			}
			if(doCounting)
			{
				game.drawPlyTimer = 0;
			}
		}
		else if(doCounting)
		{
			if(capture)
			{
				game.drawPlyTimer = 0;
			}
			else
			{
				game.drawPlyTimer++;
			}
			if(!dontCalculate && game.variant == Variant.CHESS960)
			{
				if(fromPiece.type == PieceType.KING)
				{
					if(fromPiece.color == Color.WHITE)
					{
						game.whiteCanCastle = false;
						game.whiteCanCastleQueenside = false;
					}
					else
					{
						game.blackCanCastle = false;
						game.blackCanCastleQueenside = false;
					}
				}
				else if(fromPiece.type == PieceType.ROOK)
				{
					byte kingFile = 8;
					synchronized(game.pieces)
					{
						for(Piece p : game.pieces)
						{
							if(p.color == fromPiece.color && p.type == PieceType.KING)
							{
								kingFile = p.getSquare().file;
							}
						}
					}
					if(kingFile != 8)
					{
						if(fromSquare.file > kingFile)
						{
							if(fromPiece.color == Color.WHITE)
							{
								game.whiteCanCastle = false;
							}
							else
							{
								game.blackCanCastle = false;
							}
						}
						else
						{
							if(fromPiece.color == Color.WHITE)
							{
								game.whiteCanCastleQueenside = false;
							}
							else
							{
								game.blackCanCastleQueenside = false;
							}
						}
					}
				}
			}
		}
	}

	Game commitTo(Game game, boolean dontCalculate) throws ChessException
	{
		if(this.isEnPassant)
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
				synchronized(game.repetitionPostitions)
				{
					game.repetitionPostitions.clear();
				}
			}
		}
		else
		{
			Piece piece = game.square(fromSquare).getPiece();
			if(piece.type == PieceType.PAWN)
			{
				if(piece.color == Color.WHITE && fromSquare.rank == 1 && toSquare.rank == 3)
				{
					game.enPassantSquare = game.square(fromSquare.file, (byte) (fromSquare.rank + 1));
				}
				else if(piece.color == Color.BLACK && fromSquare.rank == 6 && toSquare.rank == 4)
				{
					game.enPassantSquare = game.square(fromSquare.file, (byte) (fromSquare.rank - 1));
				}
				else
				{
					game.enPassantSquare = null;
				}
			}
			else
			{
				game.enPassantSquare = null;
			}
		}
		this.handle(game, true, dontCalculate);
		synchronized(game.moves)
		{
			game.moves.add(this);
		}
		if(game.timeControl != TimeControl.UNLIMITED)
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
			if(game.plyStart > 0)
			{
				if(game.toMove == Color.WHITE)
				{
					game.whitemsecs -= (System.currentTimeMillis() - game.plyStart);
				}
				else
				{
					game.blackmsecs -= (System.currentTimeMillis() - game.plyStart);
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
			String fen = game.getPositionalFEN(true);
			synchronized(game.repetitionPostitions)
			{
				if(game.repetitionPostitions.containsKey(fen))
				{
					int repetitions = game.repetitionPostitions.get(fen) + 1;
					game.repetitionPostitions.put(fen, repetitions);
					if(repetitions >= 5)
					{
						game.endReason = EndReason.FIVEFOLD_REPETITION;
						game.recalculateStatus();
					}
					else if(repetitions >= 3)
					{
						game.claimableDraw = EndReason.THREEFOLD_REPETITION;
						game.recalculateEndReason(isCheck);
					}
				}
				else
				{
					game.repetitionPostitions.put(fen, 1);
					game.recalculateEndReason(isCheck);
				}
			}
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

	public Game commit(boolean illegalIsLegal, boolean dontCalculate) throws ChessException
	{
		Game game = this.game.get();
		if(game == null)
		{
			throw new ChessException("Can't commit move to null");
		}
		if(!illegalIsLegal && this.getIllegalReason() != null)
		{
			game.endReason = EndReason.RULES_INFRACTION;
			game.status = game.toMove == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
			return game;
		}
		return this.commitTo(game, dontCalculate);
	}

	public Game commitInCopy() throws ChessException
	{
		return this.commitInCopy(false, false);
	}

	public Game commitInCopy(boolean illegalIsLegal, boolean dontCalculate) throws ChessException
	{
		if(this._game == null)
		{
			throw new ChessException("Can't commit move to null");
		}
		Game game = this._game.copy();
		if(!illegalIsLegal && this.getIllegalReason() != null)
		{
			game.endReason = EndReason.RULES_INFRACTION;
			game.status = game.toMove == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
			return game;
		}
		return this.commitTo(game, dontCalculate);
	}

	public boolean isLegal() throws ChessException
	{
		return this.getIllegalReason() == null;
	}

	public String getIllegalReason() throws ChessException
	{
		if(this._game.square(this.fromSquare).getPiece().color != this._game.toMove)
		{
			return "You can only move your own pieces";
		}
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
			if(this.commitInCopy(true, true).opponentToMove().isCheck())
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
			if(this.castlingType != CastlingType.NONE)
			{
				final byte rank = (byte) (this._game.toMove == Color.WHITE ? 0 : 7);
				final byte rookFile;
				final ArrayList<Square> opponentControlledSquares;
				final byte kingDestination;
				final byte rookDestination;
				if(this.castlingType == CastlingType.KINGSIDE)
				{
					if(this._game.toMove == Color.WHITE ? !this._game.whiteCanCastle : !this._game.blackCanCastle)
					{
						return "You can't castle kingside";
					}
					opponentControlledSquares = this._game.getSquaresControlledBy(this._game.toMove == Color.WHITE ? Color.BLACK : Color.WHITE);
					if(opponentControlledSquares.contains(fromSquare))
					{
						return "You can't castle while in check";
					}
					rookFile = (this._game.variant == Variant.CHESS960 ? toSquare.file : 7);
					kingDestination = 6;
					rookDestination = 5;
				}
				else
				{
					if(this._game.toMove == Color.WHITE ? !this._game.whiteCanCastleQueenside : !this._game.blackCanCastleQueenside)
					{
						return "You can't castle queenside";
					}
					opponentControlledSquares = this._game.getSquaresControlledBy(this._game.toMove == Color.WHITE ? Color.BLACK : Color.WHITE);
					if(opponentControlledSquares.contains(fromSquare))
					{
						return "You can't castle while in check";
					}
					rookFile = (this._game.variant == Variant.CHESS960 ? toSquare.file : 0);
					kingDestination = 2;
					rookDestination = 3;
				}
				if(fromSquare.file != kingDestination)
				{
					if(fromSquare.file < kingDestination)
					{
						for(byte file = (byte) (fromSquare.file + 1); file <= kingDestination; file++)
						{
							if(file != rookFile)
							{
								Square s = this._game.square(file, rank);
								if(s.hasPiece())
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
								}
								if(opponentControlledSquares.contains(s))
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is under attack";
								}
							}
						}
					}
					else
					{
						for(byte file = (byte) (fromSquare.file - 1); file >= kingDestination; file--)
						{
							if(file != rookFile)
							{
								Square s = this._game.square(file, rank);
								if(s.hasPiece())
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
								}
								if(opponentControlledSquares.contains(s))
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is under attack";
								}
							}
						}
					}
				}
				if(rookFile != rookDestination)
				{
					if(rookFile < rookDestination)
					{
						for(byte file = (byte) (rookFile + 1); file <= rookDestination; file++)
						{
							Square s = this._game.square(file, rank);
							if(s.hasPiece())
							{
								Piece p = s.getPiece();
								if(p.type != PieceType.KING || p.color != this._game.toMove)
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
								}
							}
						}
					}
					else
					{
						for(byte file = (byte) (rookFile - 1); file >= rookDestination; file--)
						{
							Square s = this._game.square(file, rank);
							if(s.hasPiece())
							{
								Piece p = s.getPiece();
								if(p.type != PieceType.KING || p.color != this._game.toMove)
								{
									return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
								}
							}
						}
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
		return this.commitInCopy(true, true).isCheck();
	}

	public boolean isStalemate() throws ChessException
	{
		return this.commitInCopy(true, true).isStalemate();
	}

	public boolean isStalemate(boolean isCheck) throws ChessException
	{
		return this.commitInCopy(true, true).isStalemate(isCheck);
	}

	public boolean isCheckmate() throws ChessException
	{
		return this.commitInCopy(true, true).isCheckmate();
	}

	public boolean isCheckmate(boolean isCheck) throws ChessException
	{
		return this.commitInCopy(true, true).isCheckmate(true);
	}

	public short getScore(Color perspective) throws ChessException
	{
		return this.commitInCopy(true, false).getScore(perspective);
	}

	public String toUCI()
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
						if(_game.getSquaresControlledBy(p).contains(toSquare))
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

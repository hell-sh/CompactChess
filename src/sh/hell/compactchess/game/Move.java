package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.exceptions.InvalidMoveException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Move
{
	public final Square fromSquare;
	public final Square toSquare;
	public final PieceType promoteTo;
	public final boolean isEnPassant;
	public final CastlingType castlingType;
	public final ArrayList<String> annotations = new ArrayList<>();
	public final ArrayList<String> annotationTags = new ArrayList<>();
	private final WeakReference<Game> game;
	private final Game _game;
	private boolean annotate = true;

	public Move(Game game, Square fromSquare, Square toSquare, PieceType promoteTo, boolean validate) throws ChessException
	{
		if(game.status == GameStatus.BUILDING)
		{
			throw new InvalidMoveException("The game has not started yet");
		}
		if(!fromSquare.hasPiece())
		{
			throw new InvalidMoveException("There's no piece on " + fromSquare.getAlgebraicNotation());
		}
		if(fromSquare.equals(toSquare))
		{
			throw new InvalidMoveException("Can't move to the same square");
		}
		if(fromSquare.pieceType == PieceType.KING && (fromSquare.pieceColor == Color.WHITE ? (fromSquare.rank == 0 && (game.whiteCanCastle || game.whiteCanCastleQueenside)) : (fromSquare.rank == 7 && (game.blackCanCastle || game.blackCanCastleQueenside))))
		{
			if(game.variant == Variant.CHESS960)
			{
				if(toSquare.pieceType == PieceType.ROOK && toSquare.pieceColor == fromSquare.pieceColor)
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
		if(validate && this.castlingType == CastlingType.NONE && !game.getSquaresControlledBy(fromSquare).contains(toSquare))
		{
			throw new InvalidMoveException("Your " + fromSquare.pieceType.name().toLowerCase() + " on " + fromSquare.getAlgebraicNotation() + " can't move to " + toSquare.getAlgebraicNotation());
		}
		this.game = new WeakReference<>(game);
		this._game = game.copy();
		this._game.tags.clear();
		this.fromSquare = fromSquare;
		this.toSquare = toSquare;
		this.promoteTo = promoteTo;
		this.isEnPassant = fromSquare.pieceType == PieceType.PAWN && toSquare.equals(game.enPassantSquare);
	}

	public Move annotate(String annotation)
	{
		synchronized(annotations)
		{
			this.annotations.add(annotation);
		}
		return this;
	}

	public boolean hasAnnotation()
	{
		return this.hasAnnotation(true);
	}

	public boolean hasAnnotation(boolean noTags)
	{
		synchronized(annotations)
		{
			synchronized(annotationTags)
			{
				return (annotations.size() != 0 || (!noTags && annotationTags.size() != 0));
			}
		}
	}

	public String getAnnotation()
	{
		return this.getAnnotation(true);
	}

	public String getAnnotation(boolean noTags)
	{
		ArrayList<String> annotations = new ArrayList<>();
		if(!noTags)
		{
			synchronized(annotationTags)
			{
				annotations.addAll(annotationTags);
			}
		}
		synchronized(this.annotations)
		{
			annotations.addAll(this.annotations);
		}
		if(annotations.size() == 0)
		{
			return "";
		}
		if(annotations.size() == 1)
		{
			return annotations.get(0);
		}
		StringBuilder str = new StringBuilder(annotations.get(0));
		for(int i = 1; i < annotations.size(); i++)
		{
			str.append(" ").append(annotations.get(i));
		}
		return str.toString();
	}

	private void handle(Game game, boolean doCounting, boolean dontCalculate) throws ChessException
	{
		final Square fromSquare = game.square(this.fromSquare);
		Color fromPieceColor = fromSquare.pieceColor;
		PieceType fromPieceType = fromSquare.pieceType;
		game.unsetPiece(fromSquare);
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
			if(toSquare.pieceType == PieceType.PAWN)
			{
				synchronized(game.repetitionPostitions)
				{
					game.repetitionPostitions.clear();
				}
			}
			game.unsetPiece(toSquare);
		}
		game.setPiece(toSquare, fromPieceColor, fromPieceType);
		if(fromPieceType == PieceType.PAWN)
		{
			if(promoteTo != null)
			{
				toSquare.pieceType = promoteTo;
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
				if(fromPieceType == PieceType.KING)
				{
					if(fromPieceColor == Color.WHITE)
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
				else if(fromPieceType == PieceType.ROOK)
				{
					byte kingFile = game.getPieces(fromPieceColor, PieceType.KING).get(0).file;
					if(kingFile != 8)
					{
						if(fromSquare.file > kingFile)
						{
							if(fromPieceColor == Color.WHITE)
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
							if(fromPieceColor == Color.WHITE)
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

	public Game commitTo(Game game, boolean dontCalculate) throws ChessException
	{
		if(this.isEnPassant)
		{
			final Square epPieceSquare;
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
				game.unsetPiece(epPieceSquare);
				synchronized(game.repetitionPostitions)
				{
					game.repetitionPostitions.clear();
				}
			}
		}
		else
		{
			final Square fromSquare = game.square(this.fromSquare);
			if(fromSquare.pieceType == PieceType.PAWN)
			{
				if(fromSquare.pieceColor == Color.WHITE && fromSquare.rank == 1 && toSquare.rank == 3)
				{
					game.enPassantSquare = game.square(fromSquare.file, (byte) (fromSquare.rank + 1));
				}
				else if(fromSquare.pieceColor == Color.BLACK && fromSquare.rank == 6 && toSquare.rank == 4)
				{
					game.enPassantSquare = game.square(fromSquare.file, (byte) (fromSquare.rank - 1));
				}
				else
				{
					game.enPassantSquare = null;
				}
				synchronized(game.repetitionPostitions)
				{
					game.repetitionPostitions.clear();
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
		if(game.plyStart > 0)
		{
			long timeTaken = (System.currentTimeMillis() - game.plyStart);
			if(game.timeControl != TimeControl.UNLIMITED)
			{
				if(game.timeControl == TimeControl.INCREMENT && (game.plyCount > 2 || game.start.whitemsecs == 0 || game.start.blackmsecs == 0))
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
				if(game.plyCount > 2 || game.start.whitemsecs > 0 || game.start.blackmsecs > 0)
				{
					if(game.toMove == Color.WHITE)
					{
						game.whitemsecs -= timeTaken;
					}
					else
					{
						game.blackmsecs -= timeTaken;
					}
				}
				if(annotate)
				{
					this.annotationTags.add("[%clk " + Game.formatTime((game.toMove == Color.WHITE ? game.whitemsecs : game.blackmsecs), true) + "]");
				}
				game.plyStart = System.currentTimeMillis();
			}
			if(annotate)
			{
				this.annotationTags.add("[%emt " + Game.formatTime(timeTaken, true) + "]");
				annotate = false;
			}
		}
		game.plyCount++;
		game.toMove = game.toMove.opposite();
		if(dontCalculate)
		{
			game.recalculateStatus();
		}
		else
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
		final Square fromSquare = this._game.square(this.fromSquare);
		if(fromSquare.pieceColor != this._game.toMove)
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
				return "You can't play a move which would leave you in check";
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
					opponentControlledSquares = this._game.getSquaresControlledBy(this._game.toMove.opposite());
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
					opponentControlledSquares = this._game.getSquaresControlledBy(this._game.toMove.opposite());
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
					final boolean add = fromSquare.file < kingDestination;
					byte file = fromSquare.file;
					do
					{
						if(add)
						{
							file++;
						}
						else
						{
							file--;
						}
						if(add ? file > kingDestination : file < kingDestination)
						{
							break;
						}
						if(file != rookFile)
						{
							final Square s = this._game.square(file, rank);
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
					while(true);
				}
				if(rookFile != rookDestination)
				{
					if(rookFile < rookDestination)
					{
						for(byte file = (byte) (rookFile + 1); file <= rookDestination; file++)
						{
							Square s = this._game.square(file, rank);
							if(s.hasPiece() && (s.pieceType != PieceType.KING || s.pieceColor != this._game.toMove))
							{
								return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
							}
						}
					}
					else
					{
						for(byte file = (byte) (rookFile - 1); file >= rookDestination; file--)
						{
							Square s = this._game.square(file, rank);
							if(s.hasPiece() && (s.pieceType != PieceType.KING || s.pieceColor != this._game.toMove))
							{
								return "You can't castle because " + s.getAlgebraicNotation() + " is occupied";
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
		if(this.promoteTo != null)
		{
			if(fromSquare.pieceType != PieceType.PAWN)
			{
				return "Only pawns can be promoted";
			}
			if(!this._game.variant.getPossiblePromotions().contains(promoteTo))
			{
				return "You can't promote to " + promoteTo.name().toLowerCase() + " in the " + this._game.variant.name + " variant";
			}
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

	public String toUCI()
	{
		StringBuilder uci = new StringBuilder(fromSquare.getAlgebraicNotation()).append(toSquare.getAlgebraicNotation());
		if(this.promoteTo != null)
		{
			uci.append(this.promoteTo.getChar(Language.ENGLISH).toLowerCase());
		}
		return uci.toString();
	}

	public String toAlgebraicNotation() throws ChessException
	{
		return this.toAlgebraicNotation(AlgebraicNotationVariation.SAN, Language.ENGLISH);
	}

	public String toAlgebraicNotation(AlgebraicNotationVariation variation) throws ChessException
	{
		return this.toAlgebraicNotation(variation, Language.ENGLISH);
	}

	public String toAlgebraicNotation(AlgebraicNotationVariation variation, Language language) throws ChessException
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
				final Square fromSquare = _game.square(this.fromSquare);
				final boolean capture = _game.square(toSquare).hasPiece();
				if(fromSquare.pieceType != PieceType.PAWN)
				{
					if(variation == AlgebraicNotationVariation.FAN)
					{
						an.append(fromSquare.getSymbol(false));
					}
					else
					{
						an.append(fromSquare.pieceType.getChar(language));
					}
				}
				if(variation == AlgebraicNotationVariation.LAN || variation == AlgebraicNotationVariation.RAN)
				{
					an.append(fromSquare.getAlgebraicNotation());
				}
				else
				{
					final ArrayList<Square> ambiguities = new ArrayList<>();
					for(Square s : _game.getPieces(fromSquare.pieceColor, fromSquare.pieceType))
					{
						if(!s.equals(fromSquare) && _game.getSquaresControlledBy(s).contains(toSquare))
						{
							ambiguities.add(s);
						}
					}
					if(ambiguities.size() > 0 || (capture && fromSquare.pieceType == PieceType.PAWN))
					{
						boolean solutionWorks = true;
						for(Square s : ambiguities)
						{
							if(s.file == fromSquare.file)
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
							for(Square s : ambiguities)
							{
								if(s.rank == fromSquare.rank)
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
							Square toSquare = _game.square(this.toSquare);
							if(toSquare.hasPiece())
							{
								an.append(toSquare.pieceType.getChar(language));
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
						an.append(fromSquare.pieceColor == Color.WHITE ? this.promoteTo.whiteSymbol : this.promoteTo.blackSymbol);
					}
					else
					{
						an.append(this.promoteTo.getChar(language));
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
			return this.fromSquare.coordinateEquals(((Move) o2).fromSquare) && this.toSquare.coordinateEquals(((Move) o2).toSquare) && ((this.promoteTo == null && ((Move) o2).promoteTo == null) || (this.promoteTo != null && ((Move) o2).promoteTo != null && this.promoteTo.equals(((Move) o2).promoteTo))) && this.isEnPassant == ((Move) o2).isEnPassant && this.castlingType == ((Move) o2).castlingType;
		}
		return false;
	}
}

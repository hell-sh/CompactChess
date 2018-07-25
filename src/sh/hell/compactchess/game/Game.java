package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.exceptions.InvalidFENException;
import sh.hell.compactchess.exceptions.InvalidMoveException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Game
{
	public final ArrayList<Move> moves = new ArrayList<>();
	public final ArrayList<Piece> pieces = new ArrayList<>();
	final public HashMap<String, String> tags = new HashMap<>();
	public Game start;
	public short plyCount = 1;
	public Variant variant = Variant.STANDARD;
	public Square[] squares;
	public Color toMove = Color.WHITE;
	public Square enPassantSquare;
	public TimeControl timeControl = TimeControl.UNLIMITED;
	public GameStatus status = GameStatus.BUILDING;
	public EndReason endReason = EndReason.UNTERMINATED;
	public long plyStart;
	public long increment = 0;
	public long whitemsecs;
	public long blackmsecs;
	public byte hundredPliesRuleTimer;
	public boolean whiteCanCastle = true;
	public boolean whiteCanCastleQueenside = true;
	public boolean blackCanCastle = true;
	public boolean blackCanCastleQueenside = true;
	public byte whitechecks = 0;
	public byte blackchecks = 0;

	public Game()
	{
		this.tags.put("Event", "https://hell.sh/CompactChess");
		this.tags.put("Site", "https://hell.sh/CompactChess");
	}

	public Game(final Variant variant)
	{
		super();
		this.variant = variant;
	}

	public static ArrayList<Game> fromPGN(final String pgn) throws ChessException
	{
		return Game.fromPGN(pgn, false);
	}

	public static ArrayList<Game> fromPGN(String pgn, final boolean dontCalculate) throws ChessException
	{
		pgn = pgn.replace("\r", "").replace("]\n\n", "]\n");
		final ArrayList<Game> games = new ArrayList<>();
		final String pgnTagRegex = "\\[([A-Za-z0-9]+) \"(.*[^\\\\])\"\\]";
		for(final String pgnGame : pgn.split("\n\n"))
		{
			final Game game = new Game();
			GameStatus _status = null;
			TimeControl _timeControl = null;
			byte excluded = 0;
			byte annotation = 0;
			Move move = null;
			for(final String line : pgnGame.split("\n"))
			{
				if(_status == null && line.matches(pgnTagRegex))
				{
					final Matcher matcher = Pattern.compile("\\[([A-Za-z0-9]+) \"(.*[^\\\\])\"\\]").matcher(line);
					if(matcher.find() && matcher.groupCount() == 2)
					{
						Game.processTag(game, matcher.group(1), matcher.group(2));
					}
				}
				else
				{
					if(_status == null)
					{
						_status = game.status;
						_timeControl = game.timeControl;
						game.status = GameStatus.BUILDING;
						game.timeControl = TimeControl.UNLIMITED;
						game.start();
					}
					for(String section : line.split(" "))
					{
						section = section.trim();
						if(section.equals(""))
						{
							continue;
						}
						if(section.startsWith("("))
						{
							excluded++;
						}
						if(excluded > 0)
						{
							if(section.endsWith(")"))
							{
								excluded--;
							}
						}
						else
						{
							if(section.startsWith("{"))
							{
								annotation++;
								section = section.substring(1);
							}
							if(annotation > 0)
							{
								if(section.endsWith("}"))
								{
									annotation--;
									section = section.substring(0, section.length() - 1);
								}
								if(move != null && !section.equals(""))
								{
									if(move.annotation.equals(""))
									{
										move.annotation = section;
									}
									else
									{
										move.annotation += " " + section;
									}
								}
							}
							else if(!section.equals("") && !section.endsWith(".") && !section.equals("1-0") && !section.equals("0-1") && !section.equals("1/2-1/2") && !section.equals("*"))
							{
								move = game.move(section);
								move.commit(false, dontCalculate);
							}
						}
					}
				}
			}
			if(_status != null)
			{
				if(_status != GameStatus.BUILDING)
				{
					game.status = _status;
					if(game.endReason == EndReason.UNTERMINATED)
					{
						if(game.status == GameStatus.DRAW)
						{
							game.endReason = EndReason.DRAW_AGREEMENT;
						}
						else if(game.status == GameStatus.WHITE_WINS || game.status == GameStatus.BLACK_WINS)
						{
							game.endReason = EndReason.RESIGNATION;
						}
					}
				}
				game.timeControl = _timeControl;
			}
			games.add(game);
		}
		return games;
	}

	public static ArrayList<Game> fromCGN(final InputStream is) throws IOException, ChessException
	{
		return Game.fromCGN(is, false);
	}

	public static ArrayList<Game> fromCGN(final InputStream is, boolean dontCalculate) throws IOException, ChessException
	{
		final ArrayList<Game> games = new ArrayList<>();
		while(is.available() > 1)
		{
			final Game game = new Game();
			byte tags = (byte) is.read();
			while(tags > 0)
			{
				Game.processTag(game, Game.readNullTerminatedString(is), Game.readNullTerminatedString(is));
				tags--;
			}
			Move lastMove = null;
			final GameStatus _status = game.status;
			final TimeControl _timeControl = game.timeControl;
			game.status = GameStatus.BUILDING;
			game.timeControl = TimeControl.UNLIMITED;
			game.start();
			while(is.available() > 0)
			{
				final byte b1 = (byte) is.read();
				final boolean controlMove = ((b1 & 0b10000000) >> 7) == 1;
				if(controlMove)
				{
					if(b1 == (byte) 0b10000000)
					{
						break;
					}
					else if(b1 == (byte) 0b10000001)
					{
						if(lastMove != null)
						{
							lastMove.annotation = Game.readNullTerminatedString(is);
						}
					}
				}
				else
				{
					final byte b2 = (byte) is.read();
					final byte fromSquareFile = (byte) ((b1 >>> 4) & 0b111);
					final byte fromSquareRank = (byte) ((b1 >>> 1) & 0b111);
					final byte toSquareFile = (byte) (((b1 << 2) & 0b100) | ((b2 >>> 6) & 0b011));
					final byte toSquareRank = (byte) (b2 >>> 3 & 0b111);
					final byte promotionValue = (byte) (b2 & 0b111);
					final PieceType promoteTo;
					if(promotionValue == 0b000)
					{
						promoteTo = null;
					}
					else
					{
						promoteTo = PieceType.fromOrdinal(promotionValue);
					}
					final Square fromSquare = game.square(fromSquareFile, fromSquareRank);
					final Square toSquare = game.square(toSquareFile, toSquareRank);
					lastMove = new Move(game, fromSquare, toSquare, promoteTo);
					lastMove.commit(false, dontCalculate);
				}
			}
			if(_status != GameStatus.BUILDING)
			{
				game.status = _status;
				if(game.endReason == EndReason.UNTERMINATED && game.status != GameStatus.DRAW)
				{
					game.endReason = EndReason.RESIGNATION;
				}
			}
			game.timeControl = _timeControl;
			games.add(game);
		}
		return games;
	}

	private static String readNullTerminatedString(final InputStream is) throws IOException
	{
		final ArrayList<Byte> byteArrList = new ArrayList<>();
		if(is.available() == 0)
		{
			throw new IOException("Insufficient bytes to read null-terminated string");
		}
		do
		{
			byte b = (byte) is.read();
			if(b == 0x00)
			{
				break;
			}
			byteArrList.add(b);
		}
		while(true);
		byte[] byteArr = new byte[byteArrList.size()];
		for(int i = 0; i < byteArr.length; i++)
		{
			byteArr[i] = byteArrList.get(i);
		}
		return new String(byteArr, "UTF-8");
	}

	private static void processTag(final Game game, final String key, final String val) throws ChessException
	{
		if(key.equalsIgnoreCase("FEN"))
		{
			final GameStatus _status = game.status;
			game.status = GameStatus.BUILDING;
			game.loadFEN(val);
			game.status = _status;
		}
		else if(key.equalsIgnoreCase("Variant"))
		{
			Variant variant = Variant.fromName(val);
			if(variant == null)
			{
				throw new ChessException("Unknown variant: " + val);
			}
			game.setVariant(variant);
		}
		else if(key.equalsIgnoreCase("Result"))
		{
			if(val.equals("1-0"))
			{
				game.status = GameStatus.WHITE_WINS;
			}
			else if(val.equals("0-1"))
			{
				game.status = GameStatus.BLACK_WINS;
			}
			else if(val.equals("1/2-1/2"))
			{
				game.status = GameStatus.DRAW;
			}
			else if(!val.equals("*"))
			{
				throw new ChessException("Invalid Result: " + val);
			}
		}
		else if(key.equalsIgnoreCase("Termination"))
		{
			if(!val.equalsIgnoreCase("Normal"))
			{
				for(EndReason er : EndReason.values())
				{
					if(er.pgn_name.equalsIgnoreCase(val))
					{
						game.endReason = er;
						break;
					}
				}
			}
		}
		else if(key.equalsIgnoreCase("TimeControl"))
		{
			if(val.equals("-"))
			{
				game.timeControl = TimeControl.UNLIMITED;
			}
			else if(val.contains("+"))
			{
				String[] timeArr = val.split("\\+");
				if(timeArr.length != 2)
				{
					throw new ChessException("Invalid TimeControl: " + val);
				}
				game.timeControl = TimeControl.INCREMENT;
				game.increment = Long.valueOf(timeArr[1].replace("seconds", "").trim()) * 1000;
				if(timeArr[0].contains(":"))
				{
					String[] timeArr2 = timeArr[0].trim().split(":");
					if(timeArr2.length != 2)
					{
						throw new ChessException("Invalid TimeControl: " + val);
					}
					game.whitemsecs = (((Long.valueOf(timeArr2[0]) * 60) + Long.valueOf(timeArr2[1])) * 60) * 1000;
				}
				else
				{
					game.whitemsecs = Long.valueOf(timeArr[0].trim()) * 1000;
				}
				game.blackmsecs = game.whitemsecs;
			}
			else
			{
				game.timeControl = TimeControl.SUDDEN_DEATH;
				game.whitemsecs = Long.valueOf(val) * 1000;
				game.blackmsecs = game.whitemsecs;
			}
		}
		else if(!key.equalsIgnoreCase("SetUp") && !key.equalsIgnoreCase("PlyCount") && !(key.equalsIgnoreCase("Event") && val.equals("-")))
		{
			game.tags.put(key, val);
		}
	}

	public Move uciMove(String uci) throws ChessException
	{
		if(uci == null || uci.equals("(none)"))
		{
			return null;
		}
		if(uci.length() != 4 && uci.length() != 5)
		{
			throw new InvalidMoveException("Invalid UCI notation: " + uci);
		}
		if(uci.length() == 5)
		{
			return new Move(this, square(uci.substring(0, 2)).copy(), square(uci.substring(2, 4)).copy(), PieceType.fromDisplayChar(uci.substring(4, 5)));
		}
		else
		{
			return new Move(this, square(uci.substring(0, 2)).copy(), square(uci.substring(2, 4)).copy());
		}
	}

	@Deprecated // Use Game.move instead.
	public Move anMove(String an) throws ChessException
	{
		return this.move(an);
	}

	public Move move(String move) throws ChessException
	{
		if(move == null || move.equals("(none)"))
		{
			return null;
		}
		move = move.replace("x", "").replace("+", "").replace("?", "").replace("!", "").replace("#", "").replace("=", "").replace("(", "").replace(")", "");
		if(move.equalsIgnoreCase("O-O-O") || move.equals("0-0-0"))
		{
			return this.uciMove(toMove == Color.WHITE ? "e1c1" : "e8c8");
		}
		else if(move.equalsIgnoreCase("O-O") || move.equals("0-0"))
		{
			return this.uciMove(toMove == Color.WHITE ? "e1g1" : "e8g8");
		}
		move = move.replace("-", "");
		PieceType promoteTo = null;
		final Square toSquare;
		String piece;
		if(move.substring(move.length() - 1).matches("[12345678]"))
		{
			toSquare = this.square(move.substring(move.length() - 2));
			piece = move.substring(0, move.length() - 2);
		}
		else
		{
			for(PieceType pt : PieceType.values())
			{
				if(pt.notationChar.equalsIgnoreCase(move.substring(move.length() - 1)) || pt.whiteSymbol.equals(move.substring(move.length() - 1)) || pt.blackSymbol.equals(move.substring(move.length() - 1)))
				{
					promoteTo = pt;
					break;
				}
			}
			toSquare = this.square(move.substring(move.length() - 3, move.length() - 1));
			piece = move.substring(0, move.length() - 3);
		}
		final Square fromSquare;
		PieceType pieceType = PieceType.PAWN;
		if(piece.length() > 0)
		{
			for(PieceType pt : PieceType.values())
			{
				if(pt.notationChar.equals(piece.substring(0, 1)) || pt.whiteSymbol.equals(piece.substring(0, 1)) || pt.blackSymbol.equals(piece.substring(0, 1)))
				{
					pieceType = pt;
					piece = piece.substring(1);
					break;
				}
			}
		}
		if(piece.length() >= 2)
		{
			fromSquare = this.square(piece.substring(0, 2));
		}
		else
		{
			final ArrayList<Square> squares = new ArrayList<>();
			if(piece.length() == 1)
			{
				if(piece.matches("[12345678]"))
				{
					byte rank = (byte) (Byte.valueOf(piece) - 1);
					synchronized(this.pieces)
					{
						for(Piece p : this.pieces)
						{
							final Square pSquare = p.getSquare();
							if(p.color == this.toMove && p.type == pieceType && pSquare.rank == rank && p.getControlledSquares(this).contains(toSquare) && new Move(this, p.getSquare(), toSquare).isLegal())
							{
								squares.add(pSquare);
							}
						}
					}
				}
				else
				{
					byte file = Square.file(piece);
					synchronized(this.pieces)
					{
						for(Piece p : this.pieces)
						{
							final Square pSquare = p.getSquare();
							if(p.color == this.toMove && p.type == pieceType && pSquare.file == file && p.getControlledSquares(this).contains(toSquare) && new Move(this, p.getSquare(), toSquare).isLegal())
							{
								squares.add(pSquare);
							}
						}
					}
				}
			}
			else
			{
				synchronized(this.pieces)
				{
					for(Piece p : this.pieces)
					{
						if(p.color == this.toMove && p.type == pieceType && p.getControlledSquares(this).contains(toSquare) && new Move(this, p.getSquare(), toSquare).isLegal())
						{
							squares.add(p.getSquare());
						}
					}
				}
			}
			if(squares.size() == 0)
			{
				throw new InvalidMoveException("No such piece '" + move.substring(0, move.length() - 2) + "' for " + move);
			}
			if(squares.size() > 1)
			{
				throw new InvalidMoveException("Ambiguous piece '" + move.substring(0, move.length() - 2) + "' for " + move);
			}
			fromSquare = squares.get(0);
		}
		return new Move(this, fromSquare, toSquare, promoteTo);
	}

	public Game setVariant(Variant variant)
	{
		if(variant == null)
		{
			throw new RuntimeException("Variant can't be null.");
		}
		this.variant = variant;
		return this;
	}

	public Game loadFEN(String fen) throws ChessException
	{
		if(this.status != GameStatus.BUILDING)
		{
			throw new ChessException("The game has already started");
		}
		if(fen.trim().equalsIgnoreCase("startpos"))
		{
			return this.loadFEN(this.variant.startFEN);
		}
		String[] arr = fen.split(" ");
		if(arr.length < 2)
		{
			throw new InvalidFENException("Not enough information in FEN: " + fen);
		}
		String pieceSequence = arr[0].replace("/", "").replace("8", "        ").replace("7", "       ").replace("6", "      ").replace("5", "     ").replace("4", "    ").replace("3", "   ").replace("2", "  ").replace("1", " ");
		this.squares = new Square[64];
		byte file = 0;
		byte rank = 7;
		for(char c : pieceSequence.toCharArray())
		{
			String s = String.valueOf(c);
			Square square = new Square(file, rank);
			if(!s.equals(" "))
			{
				Piece piece = Piece.fromNotationChar(s);
				this.pieces.add(piece);
				piece.setSquare(square);
				square.setPiece(piece);
			}
			this.squares[Square.index(file, rank)] = square;
			file++;
			if(file == 8)
			{
				rank--;
				file = 0;
			}
		}
		this.toMove = ((arr[1].equals("w")) ? Color.WHITE : Color.BLACK);
		if(arr.length > 2)
		{
			if(!arr[2].equals("-"))
			{
				this.disallowAllCastling();
				for(char c : arr[2].toCharArray())
				{
					if(c == 'K')
					{
						whiteCanCastle = true;
					}
					else if(c == 'Q')
					{
						whiteCanCastleQueenside = true;
					}
					else if(c == 'k')
					{
						blackCanCastle = true;
					}
					else if(c == 'q')
					{
						blackCanCastleQueenside = true;
					}
				}
			}
			else
			{
				this.determineCastlingAbilities();
			}
			if(arr.length > 3)
			{
				if(!arr[3].equals("-"))
				{
					this.enPassantSquare = this.square(arr[3]);
				}
				if(arr.length > 4)
				{
					this.hundredPliesRuleTimer = Byte.valueOf(arr[4]);
				}
			}
		}
		else
		{
			this.determineCastlingAbilities();
		}
		return this;
	}

	public Game blackToMove()
	{
		if(toMove != Color.BLACK)
		{
			this.toMove = Color.BLACK;
		}
		return this;
	}

	public Game opponentToMove()
	{
		this.toMove = (this.toMove == Color.WHITE ? Color.BLACK : Color.WHITE);
		return this;
	}

	public Game start() throws ChessException
	{
		boolean defaultStartPosition = (this.squares == null);
		if(defaultStartPosition)
		{
			this.loadFEN(variant.startFEN);
		}
		this.start = this.copy();
		this.status = GameStatus.ONGOING;
		if(!defaultStartPosition)
		{
			this.recalculateEndReason(this.isCheck());
		}
		return this;
	}

	void recalculateEndReason(boolean isCheck) throws ChessException
	{
		if(this.isCheckmate(isCheck))
		{
			endReason = EndReason.CHECKMATE;
		}
		else if(this.isStalemate(isCheck))
		{
			endReason = EndReason.STALEMATE;
		}
		else if(timeControl != TimeControl.UNLIMITED && (toMove == Color.WHITE ? whitemsecs : blackmsecs) <= 0)
		{
			endReason = EndReason.TIMEOUT;
		}
		else if(hundredPliesRuleTimer == 100)
		{
			endReason = EndReason.FIFTY_MOVE_RULE;
		}
		else if(variant == Variant.STANDARD)
		{
			boolean definitelySufficientMaterial = false;
			synchronized(pieces)
			{
				for(Piece p : pieces)
				{
					if(p.type != PieceType.KING && p.type != PieceType.KNIGHT && p.type != PieceType.BISHOP)
					{
						definitelySufficientMaterial = true;
						break;
					}
				}
			}
			if(!definitelySufficientMaterial)
			{
				byte whiteKnights = 0;
				byte blackKnights = 0;
				boolean whiteHasWhiteBishop = false;
				boolean blackHasWhiteBishop = false;
				boolean whiteHasBlackBishop = false;
				boolean blackHasBlackBishop = false;
				synchronized(pieces)
				{
					for(Piece p : pieces)
					{
						if(p.type == PieceType.KNIGHT)
						{
							if(p.color == Color.WHITE)
							{
								whiteKnights++;
							}
							else
							{
								blackKnights++;
							}
						}
						else if(p.type == PieceType.BISHOP)
						{
							if(p.color == Color.WHITE)
							{
								if(p.getSquare().isWhite())
								{
									whiteHasWhiteBishop = true;
								}
								else
								{
									whiteHasBlackBishop = true;
								}
							}
							else
							{
								if(p.getSquare().isWhite())
								{
									blackHasWhiteBishop = true;
								}
								else
								{
									blackHasBlackBishop = true;
								}
							}
						}
					}
				}
				if((!whiteHasWhiteBishop || !whiteHasBlackBishop) && (!blackHasWhiteBishop || !blackHasBlackBishop))
				{
					if(whiteKnights <= 1 && blackKnights <= 1)
					{
						endReason = EndReason.INSUFFICIENT_MATERIAL;
					}
				}
			}
		}
		else if(variant == Variant.THREE_CHECK)
		{
			boolean sufficientMaterial = false;
			synchronized(pieces)
			{
				for(Piece p : pieces)
				{
					if(p.type != PieceType.KING)
					{
						sufficientMaterial = true;
						break;
					}
				}
			}
			if(!sufficientMaterial)
			{
				endReason = EndReason.INSUFFICIENT_MATERIAL;
			}
		}
		this.recalculateStatus();
	}

	void recalculateStatus()
	{
		if(endReason != EndReason.UNTERMINATED)
		{
			if(endReason.isDraw())
			{
				status = GameStatus.DRAW;
			}
			else
			{
				if(variant == Variant.ANTICHESS)
				{
					status = (toMove == Color.WHITE ? GameStatus.WHITE_WINS : GameStatus.BLACK_WINS);
				}
				else
				{
					status = (toMove == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS);
				}
			}
		}
	}

	public Square square(String algebraicNotation) throws ChessException
	{
		return square(Square.file(algebraicNotation.substring(0, 1)), (byte) (Byte.valueOf(algebraicNotation.substring(1)) - 1));
	}

	public Square square(Square square)
	{
		return square(square.file, square.rank);
	}

	public Square square(byte file, byte rank)
	{
		return squares[Square.index(file, rank)];
	}

	public Game insertPiece(final Piece piece, final Square square)
	{
		piece.setSquare(square);
		square.setPiece(piece);
		return this;
	}

	public ArrayList<Square> getSquaresControlledBy(Color color)
	{
		ArrayList<Square> squares = new ArrayList<>();
		synchronized(this.pieces)
		{
			for(Piece p : this.pieces)
			{
				if(p.color == color)
				{
					squares.addAll(p.getControlledSquares(this));
				}
			}
		}
		return squares;
	}

	public boolean isCheck()
	{
		synchronized(this.pieces)
		{
			final ArrayList<Square> squaresControlledByOpponent = this.getSquaresControlledBy(this.toMove == Color.WHITE ? Color.BLACK : Color.WHITE);
			for(Piece p : pieces)
			{
				if(p.color == this.toMove && p.type == PieceType.KING)
				{
					Square kingSquare = p.getSquare();
					for(Square s : squaresControlledByOpponent)
					{
						if(s.equals(kingSquare))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean isStalemate() throws ChessException
	{
		return this.isStalemate(this.isCheck());
	}

	public boolean isStalemate(boolean isCheck) throws ChessException
	{
		if(this.endReason == EndReason.STALEMATE)
		{
			return true;
		}
		if(isCheck)
		{
			return false;
		}
		synchronized(pieces)
		{
			for(Piece p : pieces)
			{
				if(p.color == this.toMove)
				{
					final Square s = p.getSquare();
					for(Square s_ : p.getControlledSquares(this))
					{
						if(new Move(this, s, s_).isLegal())
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public boolean isCheckmate() throws ChessException
	{
		return this.isCheckmate(this.isCheck());
	}

	public boolean isCheckmate(boolean isCheck) throws ChessException
	{
		if(this.endReason == EndReason.CHECKMATE)
		{
			return true;
		}
		if(this.variant == Variant.THREE_CHECK)
		{
			if((this.toMove == Color.WHITE ? this.whitechecks : this.blackchecks) >= 3)
			{
				return true;
			}
		}
		else if(this.variant == Variant.KING_OF_THE_HILL)
		{
			synchronized(this.pieces)
			{
				for(Piece p : this.pieces)
				{
					if(p.color != this.toMove && p.type == PieceType.KING)
					{
						byte file = p.getSquare().file;
						byte rank = p.getSquare().rank;
						if((file == 3 || file == 4) && (rank == 3 || rank == 4))
						{
							return true;
						}
					}
				}
			}
		}
		else if(this.variant == Variant.RACING_KINGS)
		{
			synchronized(this.pieces)
			{
				for(Piece p : this.pieces)
				{
					if(p.color != this.toMove && p.type == PieceType.KING && p.getSquare().rank == 7)
					{
						return true;
					}
				}
			}
			return false;
		}
		else if(this.variant == Variant.HORDE && this.toMove == Color.WHITE)
		{
			synchronized(this.pieces)
			{
				boolean whiteHasPieces = false;
				for(Piece p : this.pieces)
				{
					if(p.color == Color.WHITE)
					{
						whiteHasPieces = true;
						break;
					}
				}
				if(!whiteHasPieces)
				{
					return true;
				}
			}
		}
		{
			if(!isCheck)
			{
				return this.variant == Variant.ANTICHESS && isStalemate(false);
			}
		}
		synchronized(this.pieces)
		{
			for(Piece p : this.pieces)
			{
				if(p.color == this.toMove)
				{
					final Square s = p.getSquare();
					for(Square s_ : p.getControlledSquares(this))
					{
						if(!new Move(this, s, s_).commitTo(this.copy(), true).opponentToMove().isCheck())
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public String getTimeControl()
	{
		switch(this.timeControl)
		{
			case SUDDEN_DEATH:
				return String.valueOf(this.start.whitemsecs / 1000);

			case INCREMENT:
				return (this.start.whitemsecs / 1000) + "+" + (this.increment / 1000);

			default:
				return "-";
		}
	}

	public String getPositionalFEN() throws ChessException
	{
		return this.getPositionalFEN(false);
	}

	public String getPositionalFEN(final boolean compact) throws ChessException
	{
		StringBuilder sb = new StringBuilder();
		byte emptySquares = 0;
		for(byte rank = 7; rank >= 0; rank--)
		{
			for(byte file = 0; file < 8; file++)
			{
				Square square = this.square(file, rank);
				if(!square.hasPiece())
				{
					emptySquares++;
				}
				else
				{
					if(emptySquares > 0)
					{
						sb.append(emptySquares);
						emptySquares = 0;
					}
					sb.append(square.getPiece().getCharacter());
				}
			}
			if(emptySquares > 0)
			{
				sb.append(emptySquares);
				emptySquares = 0;
			}
			if(!compact && rank != 0)
			{
				sb.append("/");
			}
		}
		sb.append(" ").append((toMove == Color.WHITE) ? "w" : "b");
		if(whiteCanCastle || whiteCanCastleQueenside || blackCanCastle || blackCanCastleQueenside)
		{
			sb.append(" ");
			if(whiteCanCastle)
			{
				sb.append("K");
			}
			if(whiteCanCastleQueenside)
			{
				sb.append("Q");
			}
			if(blackCanCastle)
			{
				sb.append("k");
			}
			if(blackCanCastleQueenside)
			{
				sb.append("q");
			}
			sb.append(" ");
		}
		else
		{
			sb.append(" - ");
		}
		sb.append(enPassantSquare == null ? "-" : enPassantSquare.getAlgebraicNotation());
		return sb.toString();
	}

	public String getFEN() throws ChessException
	{
		return this.getFEN(false);
	}

	public String getFEN(final boolean compact) throws ChessException
	{
		if(compact && hundredPliesRuleTimer == 0 && plyCount == 1)
		{
			return this.getPositionalFEN(true);
		}
		return this.getPositionalFEN(compact) + " " + hundredPliesRuleTimer + " " + (int) Math.ceil((double) plyCount / 2);
	}

	public Game allowAllCastling()
	{
		whiteCanCastle = true;
		whiteCanCastleQueenside = true;
		blackCanCastle = true;
		blackCanCastleQueenside = true;
		return this;
	}

	public Game disallowAllCastling()
	{
		whiteCanCastle = false;
		whiteCanCastleQueenside = false;
		blackCanCastle = false;
		blackCanCastleQueenside = false;
		return this;
	}

	public Game determineCastlingAbilities()
	{
		Piece piece = square((byte) 4, (byte) 0).getPiece();
		if(piece == null || piece.type != PieceType.KING || piece.color != Color.WHITE)
		{
			whiteCanCastle = false;
			whiteCanCastleQueenside = false;
		}
		else
		{
			piece = square((byte) 7, (byte) 0).getPiece();
			if(piece == null || piece.type != PieceType.ROOK || piece.color != Color.WHITE)
			{
				whiteCanCastle = false;
			}
			piece = square((byte) 0, (byte) 0).getPiece();
			if(piece == null || piece.type != PieceType.ROOK || piece.color != Color.WHITE)
			{
				whiteCanCastleQueenside = false;
			}
		}
		piece = square((byte) 4, (byte) 7).getPiece();
		if(piece == null || piece.type != PieceType.KING || piece.color != Color.BLACK)
		{
			blackCanCastle = false;
			blackCanCastleQueenside = false;
		}
		else
		{
			piece = square((byte) 7, (byte) 7).getPiece();
			if(piece == null || piece.type != PieceType.ROOK || piece.color != Color.BLACK)
			{
				blackCanCastle = false;
			}
			piece = square((byte) 0, (byte) 7).getPiece();
			if(piece == null || piece.type != PieceType.ROOK || piece.color != Color.BLACK)
			{
				blackCanCastleQueenside = false;
			}
		}
		return this;
	}

	public HashMap<String, String> getExportableTags(boolean compact) throws ChessException
	{
		final HashMap<String, String> tags = new HashMap<>();
		if(!compact || this.variant != Variant.STANDARD)
		{
			tags.put("Variant", this.variant.name);
		}
		final String startFEN = this.start.getFEN(compact);
		if(!startFEN.equals(this.variant.startFEN))
		{
			if(!compact)
			{
				tags.put("SetUp", "1");
			}
			tags.put("FEN", startFEN);
		}
		tags.putAll(this.tags);
		final String timeControl = this.getTimeControl();
		if(timeControl != null)
		{
			tags.put("TimeControl", timeControl);
		}
		if(!compact)
		{
			tags.put("PlyCount", String.valueOf(this.plyCount - this.start.plyCount));
		}
		if(this.status == GameStatus.WHITE_WINS)
		{
			if(!compact || this.endReason == EndReason.RESIGNATION)
			{
				tags.put("Result", "1-0");
			}
		}
		else if(this.status == GameStatus.BLACK_WINS)
		{
			if(!compact || this.endReason == EndReason.RESIGNATION)
			{
				tags.put("Result", "0-1");
			}
		}
		else if(this.status == GameStatus.DRAW)
		{
			tags.put("Result", "1/2-1/2");
		}
		else if(!compact)
		{
			tags.put("Result", "*");
		}
		if(!compact || (this.endReason != EndReason.UNTERMINATED && !this.endReason.pgn_name.equals("Normal")))
		{
			tags.put("Termination", endReason.pgn_name);
		}
		if(tags.get("Site").equals("https://hell.sh/CompactChess") && tags.get("Event").equals("https://hell.sh/CompactChess"))
		{
			tags.remove("Event");
		}
		return tags;
	}

	public String toPGN() throws ChessException
	{
		return this.toPGN(false, AlgebraicNotationVariation.SAN);
	}

	public String toPGN(boolean noTags) throws ChessException
	{
		return this.toPGN(noTags, AlgebraicNotationVariation.SAN);
	}

	public String toPGN(AlgebraicNotationVariation anvariation) throws ChessException
	{
		return this.toPGN(false, anvariation);
	}

	public String toPGN(boolean noTags, AlgebraicNotationVariation anvariation) throws ChessException
	{
		StringBuilder pgn = new StringBuilder();
		final HashMap<String, String> tags = this.getExportableTags(false);
		if(!noTags)
		{
			for(Map.Entry<String, String> tag : tags.entrySet())
			{
				pgn.append("[").append(tag.getKey()).append(" \"").append(tag.getValue().replace("\"", "\\\"")).append("\"]\n");
			}
			pgn.append("\n");
		}
		boolean firstMove = true;
		boolean white = this.start.toMove == Color.WHITE;
		int moveNum = (int) Math.ceil((double) this.start.plyCount / 2);
		synchronized(this.moves)
		{
			if(this.moves.size() > 0)
			{
				for(Move move : this.moves)
				{
					if(firstMove)
					{
						pgn.append(moveNum);
						if(white)
						{
							pgn.append(". ");
						}
						else
						{
							pgn.append("... ");
						}
						firstMove = false;
					}
					else if(white)
					{
						moveNum++;
						pgn.append(moveNum).append(". ");
					}
					pgn.append(move.toAlgebraicNotation(anvariation)).append(" ");
					if(!move.annotation.equals(""))
					{
						pgn.append("{ ").append(move.annotation).append(" } ");
					}
					white = !white;
				}
			}
		}
		pgn.append(tags.get("Result")).append("\n");
		return pgn.toString();
	}

	public byte[] toCGN() throws IOException, ChessException
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.toCGN(os);
		final byte[] bytes = os.toByteArray();
		os.close();
		return bytes;
	}

	public void toCGN(OutputStream os) throws ChessException, IOException
	{
		final HashMap<String, String> tags = this.getExportableTags(true);
		os.write(tags.size());
		for(Map.Entry<String, String> tag : tags.entrySet())
		{
			String key = tag.getKey();
			if(key.equalsIgnoreCase("PlyCount") || key.equalsIgnoreCase("SetUp") || (key.equalsIgnoreCase("Result") && this.endReason != EndReason.RESIGNATION && this.status != GameStatus.DRAW))
			{
				continue;
			}
			String value = tag.getValue();
			if((key.equalsIgnoreCase("Termination") && value.equalsIgnoreCase("Normal")) || (key.equalsIgnoreCase("Variant") && value.equalsIgnoreCase("Standard")))
			{
				continue;
			}
			os.write(key.getBytes(Charset.forName("UTF-8")));
			os.write(0x00);
			os.write(value.getBytes(Charset.forName("UTF-8")));
			os.write(0x00);
		}
		synchronized(this.moves)
		{
			for(Move m : this.moves)
			{
				int promotionValue = 0;
				if(m.promoteTo != null)
				{
					promotionValue = m.promoteTo.ordinal();
				}
				os.write((byte) (m.fromSquare.file << 4 | m.fromSquare.rank << 1 | (m.toSquare.file & 0b100) >>> 2));
				os.write((byte) ((m.toSquare.file & 0b011) << 6 | m.toSquare.rank << 3 | promotionValue));
				if(!m.annotation.equals(""))
				{
					os.write(0b10000001);
					os.write(m.annotation.getBytes(Charset.forName("UTF-8")));
					os.write(0x00);
				}
			}
		}
		os.write(0b10000000);
	}

	public String toString(boolean whitesPerspective) throws ChessException
	{
		return toString(whitesPerspective, false, false, false);
	}

	public String toString(boolean whitesPerspective, boolean noCoordinates, boolean noUnicode, boolean invertColor) throws ChessException
	{
		final StringBuilder sb = new StringBuilder();
		byte rank = (byte) (whitesPerspective ? 7 : 0);
		while(whitesPerspective ? rank >= 0 : rank <= 7)
		{
			if(!noCoordinates)
			{
				sb.append(rank + 1).append(" ");
			}
			byte file = (byte) (whitesPerspective ? 0 : 7);
			while(whitesPerspective ? file < 8 : file >= 0)
			{
				if(noUnicode)
				{
					sb.append(square(file, rank).getCharacter());
				}
				else
				{
					sb.append(square(file, rank).getSymbol(invertColor));
				}
				if(whitesPerspective)
				{
					file++;
				}
				else
				{
					file--;
				}
			}
			sb.append("\n");
			if(whitesPerspective)
			{
				rank--;
			}
			else
			{
				rank++;
			}
		}
		if(!noCoordinates)
		{
			sb.append("  ");
			byte file = (byte) (whitesPerspective ? 0 : 7);
			while(whitesPerspective ? file < 8 : file >= 0)
			{
				sb.append(Square.fileChar(file).toLowerCase());
				if(whitesPerspective)
				{
					file++;
				}
				else
				{
					file--;
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String toSVG()
	{
		//StringBuilder svg = new StringBuilder("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 800 800\"><rect width=\"800\" height=\"800\" fill=\"#b58863\"/><g id=\"a\"><g id=\"b\"><g id=\"c\"><g id=\"d\"><rect width=\"100\" height=\"100\" fill=\"#f0d9b5\" id=\"e\"/><use x=\"200\" xlink:href=\"#e\"/></g><use x=\"400\" xlink:href=\"#d\"/></g><use x=\"100\" y=\"100\" xlink:href=\"#c\"/></g><use y=\"200\" xlink:href=\"#b\"/></g><use y=\"400\" xlink:href=\"#a\"/>");
		StringBuilder svg = new StringBuilder("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 360 360\"><rect width=\"360\" height=\"360\" fill=\"#b58863\"/><g id=\"a\"><g id=\"b\"><g id=\"c\"><g id=\"d\"><rect width=\"45\" height=\"45\" fill=\"#f0d9b5\" id=\"e\"/><use x=\"90\" xlink:href=\"#e\"/></g><use x=\"180\" xlink:href=\"#d\"/></g><use x=\"45\" y=\"45\" xlink:href=\"#c\"/></g><use y=\"90\" xlink:href=\"#b\"/></g><use y=\"180\" xlink:href=\"#a\"/>");
		for(byte rank = 0; rank < 8; rank++)
		{
			for(byte file = 0; file < 8; file++)
			{
				Square square = this.square(file, rank);
				if(square.hasPiece())
				{
					String piece_svg = square.getPiece().getSVG();
					svg.append(piece_svg, 0, 2).append(" transform=\"translate(").append(file * 45).append(",").append(315 - (rank * 45)).append(")\"").append(piece_svg.substring(2));
				}
			}
		}
		return svg.append("</svg>").toString();
	}

	public Game copy() throws ChessException
	{
		final Game game = new Game();
		game.loadFEN(this.getPositionalFEN());
		if(start != null)
		{
			game.start = start.copy();
		}
		game.moves.addAll(moves);
		game.tags.putAll(tags);
		game.plyCount = plyCount;
		game.variant = variant;
		game.toMove = toMove;
		if(enPassantSquare != null)
		{
			game.enPassantSquare = enPassantSquare.copy();
		}
		game.timeControl = timeControl;
		game.status = status;
		game.endReason = endReason;
		game.plyStart = plyStart;
		game.increment = increment;
		game.whitemsecs = whitemsecs;
		game.blackmsecs = blackmsecs;
		game.hundredPliesRuleTimer = hundredPliesRuleTimer;
		return game;
	}

	@Override
	public boolean equals(Object o2)
	{
		if(o2 instanceof Game)
		{
			try
			{
				return this.getFEN(true).equals(((Game) o2).getFEN(true)) && ((this.start == null && ((Game) o2).start == null) || (this.start != null && ((Game) o2).start != null && this.start.getFEN(true).equals(((Game) o2).start.getFEN(true)))) && this.plyCount == ((Game) o2).plyCount && this.moves.equals(((Game) o2).moves) && this.variant.equals(((Game) o2).variant) && this.toMove.equals(((Game) o2).toMove) && this.timeControl.equals(((Game) o2).timeControl) && this.status == ((Game) o2).status && this.endReason == ((Game) o2).endReason && this.plyStart == ((Game) o2).plyStart && this.tags.equals(((Game) o2).tags) && this.increment == ((Game) o2).increment && this.whitemsecs == ((Game) o2).whitemsecs && this.blackmsecs == ((Game) o2).blackmsecs;
			}
			catch(ChessException ignored)
			{
			}
		}
		return false;
	}
}

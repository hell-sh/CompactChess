package sh.hell.compactchess.game;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.exceptions.InvalidFENException;
import sh.hell.compactchess.exceptions.InvalidMoveException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Game
{
	public static final short MAX_SCORE = 12800;
	public final ArrayList<Move> moves = new ArrayList<>();
	public final ArrayList<Square> pieces = new ArrayList<>();
	final public TreeMap<String, String> tags = new TreeMap<>(new PGNTagComparator());
	final HashMap<String, Integer> repetitionPostitions = new HashMap<>();
	public Game start;
	public short plyCount = 1;
	public Variant variant = Variant.STANDARD;
	public Square[] squares;
	public Color toMove = Color.WHITE;
	public Square enPassantSquare;
	public TimeControl timeControl = TimeControl.UNLIMITED;
	public GameStatus status = GameStatus.BUILDING;
	public EndReason claimableDraw = EndReason.UNTERMINATED;
	public EndReason endReason = EndReason.UNTERMINATED;
	public long plyStart;
	public long increment = 0;
	public long whitemsecs;
	public long blackmsecs;
	public short drawPlyTimer;
	public boolean whiteCanCastle = true;
	public boolean whiteCanCastleQueenside = true;
	public boolean blackCanCastle = true;
	public boolean blackCanCastleQueenside = true;
	public byte whitechecks = 0;
	public byte blackchecks = 0;
	private boolean exportable = true;

	public Game()
	{
	}

	public Game(final Variant variant)
	{
		super();
		this.variant = variant;
	}

	public static ArrayList<Game> fromPGN(String pgn) throws ChessException
	{
		return Game.fromPGN(pgn, Language.ENGLISH, false);
	}

	public static ArrayList<Game> fromPGN(String pgn, Language language) throws ChessException
	{
		return Game.fromPGN(pgn, language, false);
	}

	public static ArrayList<Game> fromPGN(String pgn, boolean dontCalculate) throws ChessException
	{
		return Game.fromPGN(pgn, Language.ENGLISH, dontCalculate);
	}

	public static ArrayList<Game> fromPGN(String pgn, Language language, boolean dontCalculate) throws ChessException
	{
		pgn = pgn.replace("\r", "").replace("]\n\n", "]\n");
		final ArrayList<Game> games = new ArrayList<>();
		final String pgnTagPattern = "\\[([A-Za-z0-9]+) \"(.*[^\\\\])\"]";
		for(final String pgnGame : pgn.split("\n\n"))
		{
			final Game game = new Game();
			TimeControl _timeControl = null;
			byte excluded = 0;
			byte annotation = 0;
			Move move = null;
			for(final String line : pgnGame.split("\n"))
			{
				if(_timeControl == null && line.matches(pgnTagPattern))
				{
					final Matcher matcher = Pattern.compile(pgnTagPattern).matcher(line);
					if(matcher.find() && matcher.groupCount() == 2)
					{
						if(!matcher.group(1).equalsIgnoreCase("Result"))
						{
							Game.processTag(game, matcher.group(1), matcher.group(2));
						}
					}
				}
				else
				{
					if(line.equals(""))
					{
						continue;
					}
					if(_timeControl == null)
					{
						_timeControl = game.timeControl;
						game.status = GameStatus.BUILDING;
						game.timeControl = TimeControl.UNLIMITED;
						game.start();
						game.plyStart = 0;
					}
					for(String section : line.split(" "))
					{
						section = section.trim();
						if(section.equals(""))
						{
							continue;
						}
						if(section.startsWith(";"))
						{
							break;
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
									move.annotate(section);
								}
							}
							else if(!section.equals("") && !section.endsWith("."))
							{
								if(section.equals("*"))
								{
									break;
								}
								if(section.equals("1-0") || section.equalsIgnoreCase("1-o"))
								{
									game.status = GameStatus.WHITE_WINS;
									break;
								}
								if(section.equals("0-1") || section.equalsIgnoreCase("o-1"))
								{
									game.status = GameStatus.BLACK_WINS;
									break;
								}
								if(section.equals("1/2-1/2") || section.equals("½-½"))
								{
									game.status = GameStatus.DRAW;
									break;
								}
								String moveNum = String.valueOf((int) Math.ceil((double) game.plyCount / 2)) + ".";
								if(section.startsWith(moveNum))
								{
									if(section.startsWith(moveNum + ".."))
									{
										section = section.substring(moveNum.length() + 2);
									}
									else
									{
										section = section.substring(moveNum.length());
									}
								}
								if(!section.equals(""))
								{
									move = game.move(section, language);
									move.commit(false, dontCalculate);
								}
							}
						}
					}
				}
			}
			if(_timeControl != null)
			{
				game.timeControl = _timeControl;
				if(game.status != GameStatus.BUILDING && game.status != GameStatus.ONGOING && game.endReason == EndReason.UNTERMINATED)
				{
					if(game.status == GameStatus.DRAW)
					{
						game.endReason = EndReason.DRAW_AGREEMENT;
					}
					else
					{
						game.endReason = EndReason.RESIGNATION;
					}
				}
			}
			games.add(game);
		}
		return games;
	}

	public static ArrayList<Game> fromCGN(InputStream is) throws IOException, ChessException
	{
		return Game.fromCGN(is, false, CGNVersion.latest);
	}

	public static ArrayList<Game> fromCGN(InputStream is, boolean dontCalculate) throws IOException, ChessException
	{
		return Game.fromCGN(is, dontCalculate, CGNVersion.latest);
	}

	public static ArrayList<Game> fromCGN(InputStream is, boolean dontCalculate, CGNVersion version) throws IOException, ChessException
	{
		final ArrayList<Game> games = new ArrayList<>();
		while(is.available() > 1)
		{
			final Game game = new Game();
			if(version == CGNVersion.V1)
			{
				byte tags = (byte) is.read();
				while(tags > 0)
				{
					Game.processTag(game, Game.readNullTerminatedString(is), Game.readNullTerminatedString(is));
					tags--;
				}
			}
			else
			{
				do
				{
					int tagByte = is.read();
					CGNTagMap tag = CGNTagMap.fromOrdinal(tagByte);
					if(tag == null)
					{
						throw new ChessException("Invalid CGN tag: " + String.format("%02X", tagByte));
					}
					if(tag == CGNTagMap._ENDOFTAGS)
					{
						break;
					}
					if(tag == CGNTagMap._FROMSTRING)
					{
						Game.processTag(game, Game.readNullTerminatedString(is), Game.readNullTerminatedString(is));
					}
					else
					{
						Game.processTag(game, tag.name(), Game.readNullTerminatedString(is));
					}
				}
				while(true);
			}
			Move lastMove = null;
			final GameStatus _status = game.status;
			final TimeControl _timeControl = game.timeControl;
			game.status = GameStatus.BUILDING;
			game.timeControl = TimeControl.UNLIMITED;
			game.start();
			game.plyStart = 0;
			while(is.available() > 0)
			{
				final byte b1 = (byte) is.read();
				final boolean controlMove = ((b1 & 0b10000000) >> 7) == 1;
				if(controlMove)
				{
					if(version == CGNVersion.V1)
					{
						if(b1 == (byte) 0b10000000)
						{
							break;
						}
						else if(b1 == (byte) 0b10000001)
						{
							if(lastMove != null)
							{
								lastMove.annotate(Game.readNullTerminatedString(is));
							}
						}
					}
					else
					{
						if(b1 == (byte) 0b10000000)
						{
							if(lastMove != null)
							{
								lastMove.annotate(Game.readNullTerminatedString(is));
							}
						}
						else
						{
							// 0b10000001: There are no more moves/bytes but the game is still ongoing.
							if(b1 == (byte) 0b10000010)
							{
								game.status = GameStatus.WHITE_WINS;
							}
							else if(b1 == (byte) 0b10000011)
							{
								game.status = GameStatus.BLACK_WINS;
							}
							else if(b1 == (byte) 0b10000100)
							{
								game.status = GameStatus.DRAW;
							}
							break;
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
					PieceType promoteTo = null;
					if(promotionValue > 0)
					{
						promoteTo = PieceType.fromOrdinal(promotionValue);
					}
					final Square fromSquare = game.square(fromSquareFile, fromSquareRank);
					final Square toSquare = game.square(toSquareFile, toSquareRank);
					lastMove = new Move(game, fromSquare, toSquare, promoteTo, true);
					lastMove.commit(false, dontCalculate);
				}
			}
			if(version == CGNVersion.V1 && _status != GameStatus.BUILDING)
			{
				game.status = _status;
			}
			if(game.endReason == EndReason.UNTERMINATED && game.status != GameStatus.BUILDING && game.status != GameStatus.ONGOING)
			{
				if(game.status == GameStatus.DRAW)
				{
					game.endReason = EndReason.DRAW_AGREEMENT;
				}
				else
				{
					game.endReason = EndReason.RESIGNATION;
				}
			}
			game.timeControl = _timeControl;
			games.add(game);
		}
		return games;
	}

	private static String readNullTerminatedString(InputStream is) throws IOException
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

	private static void processTag(Game game, String key, String val) throws ChessException
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
			if(val.equals("1-0") || val.equalsIgnoreCase("1-o"))
			{
				game.status = GameStatus.WHITE_WINS;
			}
			else if(val.equals("0-1") || val.equalsIgnoreCase("o-1"))
			{
				game.status = GameStatus.BLACK_WINS;
			}
			else if(val.equals("1/2-1/2") || val.equals("½-½"))
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
					if(er.pgnName.equalsIgnoreCase(val))
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
			else
			{
				if(val.contains("+"))
				{
					String[] timeArr = val.split("\\+");
					if(timeArr.length != 2)
					{
						throw new ChessException("Invalid TimeControl: " + val);
					}
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
				}
				else
				{
					game.whitemsecs = Long.valueOf(val) * 1000;
					game.increment = 0;
				}
				game.blackmsecs = game.whitemsecs;
				game.timeControl = (game.increment == 0 ? TimeControl.SUDDEN_DEATH : TimeControl.INCREMENT);
			}
		}
		else if(!key.equalsIgnoreCase("SetUp") && !key.equalsIgnoreCase("PlyCount") && !val.equals("-") && !val.equals("?") && !val.equals("????.??.??") && !val.equals("http://compactchess.cc") && !val.equals("http://hell.sh/CompactChess"))
		{
			game.tags.put(key, val);
		}
	}

	public static String getRandomChess960Position()
	{
		return Game.getChess960Position(ThreadLocalRandom.current().nextInt(1, 961));
	}

	public static String getChess960Position(int id)
	{
		String firstRank = Game.getChess960Positions().get(id);
		return firstRank + "/pppppppp/8/8/8/8/PPPPPPPP/" + firstRank.toUpperCase() + " w KQkq - 0 1";
	}

	public static ArrayList<String> getChess960Positions()
	{
		// Don't hit me for this. It's just that someone decided it would be a good idea to number every possible chess960 starting position from 0 to 959 and now it's kind of a standard...
		return new ArrayList<>(Arrays.asList("bbqnnrkr", "bqnbnrkr", "bqnnrbkr", "bqnnrkrb", "qbbnnrkr", "qnbbnrkr", "qnbnrbkr", "qnbnrkrb", "qbnnbrkr", "qnnbbrkr", "qnnrbbkr", "qnnrbkrb", "qbnnrkbr", "qnnbrkbr", "qnnrkbbr", "qnnrkrbb", "bbnqnrkr", "bnqbnrkr", "bnqnrbkr", "bnqnrkrb", "nbbqnrkr", "nqbbnrkr", "nqbnrbkr", "nqbnrkrb", "nbqnbrkr", "nqnbbrkr", "nqnrbbkr", "nqnrbkrb", "nbqnrkbr", "nqnbrkbr", "nqnrkbbr", "nqnrkrbb", "bbnnqrkr", "bnnbqrkr", "bnnqrbkr", "bnnqrkrb", "nbbnqrkr", "nnbbqrkr", "nnbqrbkr", "nnbqrkrb", "nbnqbrkr", "nnqbbrkr", "nnqrbbkr", "nnqrbkrb", "nbnqrkbr", "nnqbrkbr", "nnqrkbbr", "nnqrkrbb", "bbnnrqkr", "bnnbrqkr", "bnnrqbkr", "bnnrqkrb", "nbbnrqkr", "nnbbrqkr", "nnbrqbkr", "nnbrqkrb", "nbnrbqkr", "nnrbbqkr", "nnrqbbkr", "nnrqbkrb", "nbnrqkbr", "nnrbqkbr", "nnrqkbbr", "nnrqkrbb", "bbnnrkqr", "bnnbrkqr", "bnnrkbqr", "bnnrkqrb", "nbbnrkqr", "nnbbrkqr", "nnbrkbqr", "nnbrkqrb", "nbnrbkqr", "nnrbbkqr", "nnrkbbqr", "nnrkbqrb", "nbnrkqbr", "nnrbkqbr", "nnrkqbbr", "nnrkqrbb", "bbnnrkrq", "bnnbrkrq", "bnnrkbrq", "bnnrkrqb", "nbbnrkrq", "nnbbrkrq", "nnbrkbrq", "nnbrkrqb", "nbnrbkrq", "nnrbbkrq", "nnrkbbrq", "nnrkbrqb", "nbnrkrbq", "nnrbkrbq", "nnrkrbbq", "nnrkrqbb", "bbqnrnkr", "bqnbrnkr", "bqnrnbkr", "bqnrnkrb", "qbbnrnkr", "qnbbrnkr", "qnbrnbkr", "qnbrnkrb", "qbnrbnkr", "qnrbbnkr", "qnrnbbkr", "qnrnbkrb", "qbnrnkbr", "qnrbnkbr", "qnrnkbbr", "qnrnkrbb", "bbnqrnkr", "bnqbrnkr", "bnqrnbkr", "bnqrnkrb", "nbbqrnkr", "nqbbrnkr", "nqbrnbkr", "nqbrnkrb", "nbqrbnkr", "nqrbbnkr", "nqrnbbkr", "nqrnbkrb", "nbqrnkbr", "nqrbnkbr", "nqrnkbbr", "nqrnkrbb", "bbnrqnkr", "bnrbqnkr", "bnrqnbkr", "bnrqnkrb", "nbbrqnkr", "nrbbqnkr", "nrbqnbkr", "nrbqnkrb", "nbrqbnkr", "nrqbbnkr", "nrqnbbkr", "nrqnbkrb", "nbrqnkbr", "nrqbnkbr", "nrqnkbbr", "nrqnkrbb", "bbnrnqkr", "bnrbnqkr", "bnrnqbkr", "bnrnqkrb", "nbbrnqkr", "nrbbnqkr", "nrbnqbkr", "nrbnqkrb", "nbrnbqkr", "nrnbbqkr", "nrnqbbkr", "nrnqbkrb", "nbrnqkbr", "nrnbqkbr", "nrnqkbbr", "nrnqkrbb", "bbnrnkqr", "bnrbnkqr", "bnrnkbqr", "bnrnkqrb", "nbbrnkqr", "nrbbnkqr", "nrbnkbqr", "nrbnkqrb", "nbrnbkqr", "nrnbbkqr", "nrnkbbqr", "nrnkbqrb", "nbrnkqbr", "nrnbkqbr", "nrnkqbbr", "nrnkqrbb", "bbnrnkrq", "bnrbnkrq", "bnrnkbrq", "bnrnkrqb", "nbbrnkrq", "nrbbnkrq", "nrbnkbrq", "nrbnkrqb", "nbrnbkrq", "nrnbbkrq", "nrnkbbrq", "nrnkbrqb", "nbrnkrbq", "nrnbkrbq", "nrnkrbbq", "nrnkrqbb", "bbqnrknr", "bqnbrknr", "bqnrkbnr", "bqnrknrb", "qbbnrknr", "qnbbrknr", "qnbrkbnr", "qnbrknrb", "qbnrbknr", "qnrbbknr", "qnrkbbnr", "qnrkbnrb", "qbnrknbr", "qnrbknbr", "qnrknbbr", "qnrknrbb", "bbnqrknr", "bnqbrknr", "bnqrkbnr", "bnqrknrb", "nbbqrknr", "nqbbrknr", "nqbrkbnr", "nqbrknrb", "nbqrbknr", "nqrbbknr", "nqrkbbnr", "nqrkbnrb", "nbqrknbr", "nqrbknbr", "nqrknbbr", "nqrknrbb", "bbnrqknr", "bnrbqknr", "bnrqkbnr", "bnrqknrb", "nbbrqknr", "nrbbqknr", "nrbqkbnr", "nrbqknrb", "nbrqbknr", "nrqbbknr", "nrqkbbnr", "nrqkbnrb", "nbrqknbr", "nrqbknbr", "nrqknbbr", "nrqknrbb", "bbnrkqnr", "bnrbkqnr", "bnrkqbnr", "bnrkqnrb", "nbbrkqnr", "nrbbkqnr", "nrbkqbnr", "nrbkqnrb", "nbrkbqnr", "nrkbbqnr", "nrkqbbnr", "nrkqbnrb", "nbrkqnbr", "nrkbqnbr", "nrkqnbbr", "nrkqnrbb", "bbnrknqr", "bnrbknqr", "bnrknbqr", "bnrknqrb", "nbbrknqr", "nrbbknqr", "nrbknbqr", "nrbknqrb", "nbrkbnqr", "nrkbbnqr", "nrknbbqr", "nrknbqrb", "nbrknqbr", "nrkbnqbr", "nrknqbbr", "nrknqrbb", "bbnrknrq", "bnrbknrq", "bnrknbrq", "bnrknrqb", "nbbrknrq", "nrbbknrq", "nrbknbrq", "nrbknrqb", "nbrkbnrq", "nrkbbnrq", "nrknbbrq", "nrknbrqb", "nbrknrbq", "nrkbnrbq", "nrknrbbq", "nrknrqbb", "bbqnrkrn", "bqnbrkrn", "bqnrkbrn", "bqnrkrnb", "qbbnrkrn", "qnbbrkrn", "qnbrkbrn", "qnbrkrnb", "qbnrbkrn", "qnrbbkrn", "qnrkbbrn", "qnrkbrnb", "qbnrkrbn", "qnrbkrbn", "qnrkrbbn", "qnrkrnbb", "bbnqrkrn", "bnqbrkrn", "bnqrkbrn", "bnqrkrnb", "nbbqrkrn", "nqbbrkrn", "nqbrkbrn", "nqbrkrnb", "nbqrbkrn", "nqrbbkrn", "nqrkbbrn", "nqrkbrnb", "nbqrkrbn", "nqrbkrbn", "nqrkrbbn", "nqrkrnbb", "bbnrqkrn", "bnrbqkrn", "bnrqkbrn", "bnrqkrnb", "nbbrqkrn", "nrbbqkrn", "nrbqkbrn", "nrbqkrnb", "nbrqbkrn", "nrqbbkrn", "nrqkbbrn", "nrqkbrnb", "nbrqkrbn", "nrqbkrbn", "nrqkrbbn", "nrqkrnbb", "bbnrkqrn", "bnrbkqrn", "bnrkqbrn", "bnrkqrnb", "nbbrkqrn", "nrbbkqrn", "nrbkqbrn", "nrbkqrnb", "nbrkbqrn", "nrkbbqrn", "nrkqbbrn", "nrkqbrnb", "nbrkqrbn", "nrkbqrbn", "nrkqrbbn", "nrkqrnbb", "bbnrkrqn", "bnrbkrqn", "bnrkrbqn", "bnrkrqnb", "nbbrkrqn", "nrbbkrqn", "nrbkrbqn", "nrbkrqnb", "nbrkbrqn", "nrkbbrqn", "nrkrbbqn", "nrkrbqnb", "nbrkrqbn", "nrkbrqbn", "nrkrqbbn", "nrkrqnbb", "bbnrkrnq", "bnrbkrnq", "bnrkrbnq", "bnrkrnqb", "nbbrkrnq", "nrbbkrnq", "nrbkrbnq", "nrbkrnqb", "nbrkbrnq", "nrkbbrnq", "nrkrbbnq", "nrkrbnqb", "nbrkrnbq", "nrkbrnbq", "nrkrnbbq", "nrkrnqbb", "bbqrnnkr", "bqrbnnkr", "bqrnnbkr", "bqrnnkrb", "qbbrnnkr", "qrbbnnkr", "qrbnnbkr", "qrbnnkrb", "qbrnbnkr", "qrnbbnkr", "qrnnbbkr", "qrnnbkrb", "qbrnnkbr", "qrnbnkbr", "qrnnkbbr", "qrnnkrbb", "bbrqnnkr", "brqbnnkr", "brqnnbkr", "brqnnkrb", "rbbqnnkr", "rqbbnnkr", "rqbnnbkr", "rqbnnkrb", "rbqnbnkr", "rqnbbnkr", "rqnnbbkr", "rqnnbkrb", "rbqnnkbr", "rqnbnkbr", "rqnnkbbr", "rqnnkrbb", "bbrnqnkr", "brnbqnkr", "brnqnbkr", "brnqnkrb", "rbbnqnkr", "rnbbqnkr", "rnbqnbkr", "rnbqnkrb", "rbnqbnkr", "rnqbbnkr", "rnqnbbkr", "rnqnbkrb", "rbnqnkbr", "rnqbnkbr", "rnqnkbbr", "rnqnkrbb", "bbrnnqkr", "brnbnqkr", "brnnqbkr", "brnnqkrb", "rbbnnqkr", "rnbbnqkr", "rnbnqbkr", "rnbnqkrb", "rbnnbqkr", "rnnbbqkr", "rnnqbbkr", "rnnqbkrb", "rbnnqkbr", "rnnbqkbr", "rnnqkbbr", "rnnqkrbb", "bbrnnkqr", "brnbnkqr", "brnnkbqr", "brnnkqrb", "rbbnnkqr", "rnbbnkqr", "rnbnkbqr", "rnbnkqrb", "rbnnbkqr", "rnnbbkqr", "rnnkbbqr", "rnnkbqrb", "rbnnkqbr", "rnnbkqbr", "rnnkqbbr", "rnnkqrbb", "bbrnnkrq", "brnbnkrq", "brnnkbrq", "brnnkrqb", "rbbnnkrq", "rnbbnkrq", "rnbnkbrq", "rnbnkrqb", "rbnnbkrq", "rnnbbkrq", "rnnkbbrq", "rnnkbrqb", "rbnnkrbq", "rnnbkrbq", "rnnkrbbq", "rnnkrqbb", "bbqrnknr", "bqrbnknr", "bqrnkbnr", "bqrnknrb", "qbbrnknr", "qrbbnknr", "qrbnkbnr", "qrbnknrb", "qbrnbknr", "qrnbbknr", "qrnkbbnr", "qrnkbnrb", "qbrnknbr", "qrnbknbr", "qrnknbbr", "qrnknrbb", "bbrqnknr", "brqbnknr", "brqnkbnr", "brqnknrb", "rbbqnknr", "rqbbnknr", "rqbnkbnr", "rqbnknrb", "rbqnbknr", "rqnbbknr", "rqnkbbnr", "rqnkbnrb", "rbqnknbr", "rqnbknbr", "rqnknbbr", "rqnknrbb", "bbrnqknr", "brnbqknr", "brnqkbnr", "brnqknrb", "rbbnqknr", "rnbbqknr", "rnbqkbnr", "rnbqknrb", "rbnqbknr", "rnqbbknr", "rnqkbbnr", "rnqkbnrb", "rbnqknbr", "rnqbknbr", "rnqknbbr", "rnqknrbb", "bbrnkqnr", "brnbkqnr", "brnkqbnr", "brnkqnrb", "rbbnkqnr", "rnbbkqnr", "rnbkqbnr", "rnbkqnrb", "rbnkbqnr", "rnkbbqnr", "rnkqbbnr", "rnkqbnrb", "rbnkqnbr", "rnkbqnbr", "rnkqnbbr", "rnkqnrbb", "bbrnknqr", "brnbknqr", "brnknbqr", "brnknqrb", "rbbnknqr", "rnbbknqr", "rnbknbqr", "rnbknqrb", "rbnkbnqr", "rnkbbnqr", "rnknbbqr", "rnknbqrb", "rbnknqbr", "rnkbnqbr", "rnknqbbr", "rnknqrbb", "bbrnknrq", "brnbknrq", "brnknbrq", "brnknrqb", "rbbnknrq", "rnbbknrq", "rnbknbrq", "rnbknrqb", "rbnkbnrq", "rnkbbnrq", "rnknbbrq", "rnknbrqb", "rbnknrbq", "rnkbnrbq", "rnknrbbq", "rnknrqbb", "bbqrnkrn", "bqrbnkrn", "bqrnkbrn", "bqrnkrnb", "qbbrnkrn", "qrbbnkrn", "qrbnkbrn", "qrbnkrnb", "qbrnbkrn", "qrnbbkrn", "qrnkbbrn", "qrnkbrnb", "qbrnkrbn", "qrnbkrbn", "qrnkrbbn", "qrnkrnbb", "bbrqnkrn", "brqbnkrn", "brqnkbrn", "brqnkrnb", "rbbqnkrn", "rqbbnkrn", "rqbnkbrn", "rqbnkrnb", "rbqnbkrn", "rqnbbkrn", "rqnkbbrn", "rqnkbrnb", "rbqnkrbn", "rqnbkrbn", "rqnkrbbn", "rqnkrnbb", "bbrnqkrn", "brnbqkrn", "brnqkbrn", "brnqkrnb", "rbbnqkrn", "rnbbqkrn", "rnbqkbrn", "rnbqkrnb", "rbnqbkrn", "rnqbbkrn", "rnqkbbrn", "rnqkbrnb", "rbnqkrbn", "rnqbkrbn", "rnqkrbbn", "rnqkrnbb", "bbrnkqrn", "brnbkqrn", "brnkqbrn", "brnkqrnb", "rbbnkqrn", "rnbbkqrn", "rnbkqbrn", "rnbkqrnb", "rbnkbqrn", "rnkbbqrn", "rnkqbbrn", "rnkqbrnb", "rbnkqrbn", "rnkbqrbn", "rnkqrbbn", "rnkqrnbb", "bbrnkrqn", "brnbkrqn", "brnkrbqn", "brnkrqnb", "rbbnkrqn", "rnbbkrqn", "rnbkrbqn", "rnbkrqnb", "rbnkbrqn", "rnkbbrqn", "rnkrbbqn", "rnkrbqnb", "rbnkrqbn", "rnkbrqbn", "rnkrqbbn", "rnkrqnbb", "bbrnkrnq", "brnbkrnq", "brnkrbnq", "brnkrnqb", "rbbnkrnq", "rnbbkrnq", "rnbkrbnq", "rnbkrnqb", "rbnkbrnq", "rnkbbrnq", "rnkrbbnq", "rnkrbnqb", "rbnkrnbq", "rnkbrnbq", "rnkrnbbq", "rnkrnqbb", "bbqrknnr", "bqrbknnr", "bqrknbnr", "bqrknnrb", "qbbrknnr", "qrbbknnr", "qrbknbnr", "qrbknnrb", "qbrkbnnr", "qrkbbnnr", "qrknbbnr", "qrknbnrb", "qbrknnbr", "qrkbnnbr", "qrknnbbr", "qrknnrbb", "bbrqknnr", "brqbknnr", "brqknbnr", "brqknnrb", "rbbqknnr", "rqbbknnr", "rqbknbnr", "rqbknnrb", "rbqkbnnr", "rqkbbnnr", "rqknbbnr", "rqknbnrb", "rbqknnbr", "rqkbnnbr", "rqknnbbr", "rqknnrbb", "bbrkqnnr", "brkbqnnr", "brkqnbnr", "brkqnnrb", "rbbkqnnr", "rkbbqnnr", "rkbqnbnr", "rkbqnnrb", "rbkqbnnr", "rkqbbnnr", "rkqnbbnr", "rkqnbnrb", "rbkqnnbr", "rkqbnnbr", "rkqnnbbr", "rkqnnrbb", "bbrknqnr", "brkbnqnr", "brknqbnr", "brknqnrb", "rbbknqnr", "rkbbnqnr", "rkbnqbnr", "rkbnqnrb", "rbknbqnr", "rknbbqnr", "rknqbbnr", "rknqbnrb", "rbknqnbr", "rknbqnbr", "rknqnbbr", "rknqnrbb", "bbrknnqr", "brkbnnqr", "brknnbqr", "brknnqrb", "rbbknnqr", "rkbbnnqr", "rkbnnbqr", "rkbnnqrb", "rbknbnqr", "rknbbnqr", "rknnbbqr", "rknnbqrb", "rbknnqbr", "rknbnqbr", "rknnqbbr", "rknnqrbb", "bbrknnrq", "brkbnnrq", "brknnbrq", "brknnrqb", "rbbknnrq", "rkbbnnrq", "rkbnnbrq", "rkbnnrqb", "rbknbnrq", "rknbbnrq", "rknnbbrq", "rknnbrqb", "rbknnrbq", "rknbnrbq", "rknnrbbq", "rknnrqbb", "bbqrknrn", "bqrbknrn", "bqrknbrn", "bqrknrnb", "qbbrknrn", "qrbbknrn", "qrbknbrn", "qrbknrnb", "qbrkbnrn", "qrkbbnrn", "qrknbbrn", "qrknbrnb", "qbrknrbn", "qrkbnrbn", "qrknrbbn", "qrknrnbb", "bbrqknrn", "brqbknrn", "brqknbrn", "brqknrnb", "rbbqknrn", "rqbbknrn", "rqbknbrn", "rqbknrnb", "rbqkbnrn", "rqkbbnrn", "rqknbbrn", "rqknbrnb", "rbqknrbn", "rqkbnrbn", "rqknrbbn", "rqknrnbb", "bbrkqnrn", "brkbqnrn", "brkqnbrn", "brkqnrnb", "rbbkqnrn", "rkbbqnrn", "rkbqnbrn", "rkbqnrnb", "rbkqbnrn", "rkqbbnrn", "rkqnbbrn", "rkqnbrnb", "rbkqnrbn", "rkqbnrbn", "rkqnrbbn", "rkqnrnbb", "bbrknqrn", "brkbnqrn", "brknqbrn", "brknqrnb", "rbbknqrn", "rkbbnqrn", "rkbnqbrn", "rkbnqrnb", "rbknbqrn", "rknbbqrn", "rknqbbrn", "rknqbrnb", "rbknqrbn", "rknbqrbn", "rknqrbbn", "rknqrnbb", "bbrknrqn", "brkbnrqn", "brknrbqn", "brknrqnb", "rbbknrqn", "rkbbnrqn", "rkbnrbqn", "rkbnrqnb", "rbknbrqn", "rknbbrqn", "rknrbbqn", "rknrbqnb", "rbknrqbn", "rknbrqbn", "rknrqbbn", "rknrqnbb", "bbrknrnq", "brkbnrnq", "brknrbnq", "brknrnqb", "rbbknrnq", "rkbbnrnq", "rkbnrbnq", "rkbnrnqb", "rbknbrnq", "rknbbrnq", "rknrbbnq", "rknrbnqb", "rbknrnbq", "rknbrnbq", "rknrnbbq", "rknrnqbb", "bbqrkrnn", "bqrbkrnn", "bqrkrbnn", "bqrkrnnb", "qbbrkrnn", "qrbbkrnn", "qrbkrbnn", "qrbkrnnb", "qbrkbrnn", "qrkbbrnn", "qrkrbbnn", "qrkrbnnb", "qbrkrnbn", "qrkbrnbn", "qrkrnbbn", "qrkrnnbb", "bbrqkrnn", "brqbkrnn", "brqkrbnn", "brqkrnnb", "rbbqkrnn", "rqbbkrnn", "rqbkrbnn", "rqbkrnnb", "rbqkbrnn", "rqkbbrnn", "rqkrbbnn", "rqkrbnnb", "rbqkrnbn", "rqkbrnbn", "rqkrnbbn", "rqkrnnbb", "bbrkqrnn", "brkbqrnn", "brkqrbnn", "brkqrnnb", "rbbkqrnn", "rkbbqrnn", "rkbqrbnn", "rkbqrnnb", "rbkqbrnn", "rkqbbrnn", "rkqrbbnn", "rkqrbnnb", "rbkqrnbn", "rkqbrnbn", "rkqrnbbn", "rkqrnnbb", "bbrkrqnn", "brkbrqnn", "brkrqbnn", "brkrqnnb", "rbbkrqnn", "rkbbrqnn", "rkbrqbnn", "rkbrqnnb", "rbkrbqnn", "rkrbbqnn", "rkrqbbnn", "rkrqbnnb", "rbkrqnbn", "rkrbqnbn", "rkrqnbbn", "rkrqnnbb", "bbrkrnqn", "brkbrnqn", "brkrnbqn", "brkrnqnb", "rbbkrnqn", "rkbbrnqn", "rkbrnbqn", "rkbrnqnb", "rbkrbnqn", "rkrbbnqn", "rkrnbbqn", "rkrnbqnb", "rbkrnqbn", "rkrbnqbn", "rkrnqbbn", "rkrnqnbb", "bbrkrnnq", "brkbrnnq", "brkrnbnq", "brkrnnqb", "rbbkrnnq", "rkbbrnnq", "rkbrnbnq", "rkbrnnqb", "rbkrbnnq", "rkrbbnnq", "rkrnbbnq", "rkrnbnqb", "rbkrnnbq", "rkrbnnbq", "rkrnnbbq", "rkrnnqbb"));
	}

	public static String formatTime(long msecs, boolean forAnnotation)
	{
		String time = "";
		if(msecs < 0)
		{
			msecs = msecs * -1;
			time = "-";
		}
		long seconds = (msecs / 1000) % 60;
		time += String.format("%02d", seconds);
		if(!forAnnotation)
		{
			time += "." + String.format("%04d", (msecs % 1000));
		}
		if(msecs >= 60000 || forAnnotation)
		{
			long minutes = (msecs / 60000) % 60;
			time = String.format("%02d:", minutes) + time;
			if(msecs >= 3600000 || forAnnotation)
			{
				long hours = (msecs / 3600000) % 24;
				if(forAnnotation)
				{
					time = String.format("%01d:", hours) + time;
				}
				else
				{
					time = String.format("%02d:", hours) + time;
				}
			}
		}
		return time;
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
		if(this.variant == Variant.CHESS960)
		{
			CastlingType castlingType = CastlingType.NONE;
			if((whiteCanCastleQueenside && toMove == Color.WHITE && uci.equals("e1c1")) || (blackCanCastleQueenside && toMove == Color.BLACK && uci.equals("e8c8")))
			{
				castlingType = CastlingType.QUEENSIDE;
			}
			else if((whiteCanCastle && toMove == Color.WHITE && uci.equals("e1g1")) || (blackCanCastleQueenside && toMove == Color.BLACK && uci.equals("e8g8")))
			{
				castlingType = CastlingType.KINGSIDE;
			}
			if(castlingType != CastlingType.NONE)
			{
				byte rank = (byte) (toMove == Color.WHITE ? 0 : 7);
				byte kingFile = 8;
				synchronized(this.pieces)
				{
					for(Square s : this.pieces)
					{
						if(s.pieceColor == toMove && s.pieceType == PieceType.KING)
						{
							if(s.rank == rank)
							{
								kingFile = s.file;
							}
						}
					}
				}
				if(kingFile != 8)
				{
					String rookSquare = null;
					if(castlingType == CastlingType.QUEENSIDE)
					{
						synchronized(this.pieces)
						{
							for(Square s : this.pieces)
							{
								if(s.pieceColor == toMove && s.pieceType == PieceType.ROOK)
								{
									if(s.rank == rank && s.file < kingFile)
									{
										rookSquare = s.getAlgebraicNotation();
										break;
									}
								}
							}
						}
					}
					else
					{
						synchronized(this.pieces)
						{
							for(Square s : this.pieces)
							{
								if(s.pieceColor == toMove && s.pieceType == PieceType.ROOK)
								{
									if(s.rank == rank && s.file > kingFile)
									{
										rookSquare = s.getAlgebraicNotation();
										break;
									}
								}
							}
						}
					}
					if(rookSquare != null)
					{
						return new Move(this, square(kingFile, rank), square(rookSquare), null, true);
					}
				}
			}
		}
		if(uci.length() == 5)
		{
			return new Move(this, square(uci.substring(0, 2)).copy(), square(uci.substring(2, 4)).copy(), Language.ENGLISH.pieceFromChar(uci.substring(4, 5)), true);
		}
		else
		{
			return new Move(this, square(uci.substring(0, 2)).copy(), square(uci.substring(2, 4)).copy(), null, true);
		}
	}

	public Move move(String move) throws ChessException
	{
		return move(move, Language.ENGLISH);
	}

	public Move move(String move, Language language) throws ChessException
	{
		if(move == null || move.equals("(none)"))
		{
			return null;
		}
		move = move.replace("x", "").replace("+", "").replace("?", "").replace("!", "").replace("#", "").replace("=", "").replace("(", "").replace(")", "");
		if(move.equalsIgnoreCase("O-O-O") || move.equals("0-0-0") || move.equals("e1c1") || move.equals("e8c8"))
		{
			return this.uciMove(toMove == Color.WHITE ? "e1c1" : "e8c8");
		}
		else if(move.equalsIgnoreCase("O-O") || move.equals("0-0") || move.equals("e1g1") || move.equals("e8g8"))
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
			String promoteChar = move.substring(move.length() - 1).toUpperCase();
			for(PieceType pt : PieceType.values())
			{
				if(pt.getDisplayChar(language).equalsIgnoreCase(promoteChar) || pt.whiteSymbol.equals(promoteChar) || pt.blackSymbol.equals(promoteChar))
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
				if(pt.getNotationChar(language).equalsIgnoreCase(piece.substring(0, 1)) || pt.whiteSymbol.equals(piece.substring(0, 1)) || pt.blackSymbol.equals(piece.substring(0, 1)))
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
						for(Square s : pieces)
						{
							if(s.pieceColor == this.toMove && s.pieceType == pieceType && s.rank == rank && this.getSquaresControlledBy(s).contains(toSquare) && new Move(this, s, toSquare, null, true).isLegal())
							{
								squares.add(s);
							}
						}
					}
				}
				else
				{
					byte file = Square.file(piece);
					synchronized(this.pieces)
					{
						for(Square s : this.pieces)
						{
							if(s.pieceColor == this.toMove && s.pieceType == pieceType && s.file == file && this.getSquaresControlledBy(s).contains(toSquare) && new Move(this, s, toSquare, null, true).isLegal())
							{
								squares.add(s);
							}
						}
					}
				}
			}
			else
			{
				synchronized(this.pieces)
				{
					for(Square s : this.pieces)
					{
						if(s.pieceColor == this.toMove && s.pieceType == pieceType && this.getSquaresControlledBy(s).contains(toSquare) && new Move(this, s, toSquare, null, true).isLegal())
						{
							squares.add(s);
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
		return new Move(this, fromSquare, toSquare, promoteTo, true);
	}

	public Game setVariant(Variant variant)
	{
		if(variant == null)
		{
			throw new RuntimeException("Variant can't be null.");
		}
		if(this.status != GameStatus.BUILDING)
		{
			this.exportable = false;
		}
		this.variant = variant;
		return this;
	}

	public Game setPiece(Square square, Color color, PieceType pieceType)
	{
		synchronized(this.pieces)
		{
			square.pieceColor = color;
			square.pieceType = pieceType;
			if(!this.pieces.contains(square))
			{
				this.pieces.add(square);
			}
		}
		return this;
	}

	public Game unsetPiece(Square square)
	{
		synchronized(this.pieces)
		{
			square.pieceColor = null;
			square.pieceType = null;
			this.pieces.remove(square);
		}
		return this;
	}

	public Game loadFEN(String fen) throws ChessException
	{
		if(this.status != GameStatus.BUILDING)
		{
			this.exportable = false;
		}
		fen = fen.trim();
		if(fen.equalsIgnoreCase("startpos"))
		{
			this.loadFEN(this.variant.startFEN);
		}
		else if(fen.equalsIgnoreCase("random960"))
		{
			this.loadFEN(Game.getRandomChess960Position());
		}
		else
		{
			String[] arr = fen.split(" ");
			if(arr.length < 2)
			{
				throw new InvalidFENException("Not enough information in FEN: " + fen);
			}
			String pieceSequence = arr[0].replace("/", "").replace("8", "        ").replace("7", "       ").replace("6", "      ").replace("5", "     ").replace("4", "    ").replace("3", "   ").replace("2", "  ").replace("1", " ");
			synchronized(this.pieces)
			{
				this.pieces.clear();
				this.squares = new Square[64];
				byte file = 0;
				byte rank = 7;
				for(char c : pieceSequence.toCharArray())
				{
					String s = String.valueOf(c);
					final Square square;
					if(s.equals(" "))
					{
						square = new Square(file, rank);
					}
					else
					{
						square = new Square(file, rank, (s.toUpperCase().equals(s) ? Color.WHITE : Color.BLACK), Language.ENGLISH.pieceFromChar(s));
						this.pieces.add(square);
					}
					this.squares[Square.index(file, rank)] = square;
					file++;
					if(file == 8)
					{
						rank--;
						file = 0;
					}
				}
			}
			this.toMove = ((arr[1].equals("w")) ? Color.WHITE : Color.BLACK);
			if(arr.length > 2)
			{
				whiteCanCastle = false;
				whiteCanCastleQueenside = false;
				blackCanCastle = false;
				blackCanCastleQueenside = false;
				if(!arr[2].equals("-"))
				{
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
				if(arr.length > 3)
				{
					if(!arr[3].equals("-"))
					{
						this.enPassantSquare = this.square(arr[3]);
					}
					if(arr.length > 4)
					{
						this.drawPlyTimer = Byte.valueOf(arr[4]);
					}
				}
			}
			else
			{
				this.determineCastlingAbilities();
			}
		}
		return this;
	}

	public Game loadChess960Position(int id) throws ChessException
	{
		this.loadFEN(Game.getChess960Position(id));
		return this;
	}

	public int getChess960PositionID()
	{
		String pos;
		if(this.start != null)
		{
			pos = this.start.getPositionalFEN(true).substring(0, 8);
		}
		else
		{
			pos = this.getPositionalFEN(true).substring(0, 8);
		}
		ArrayList<String> chess960positions = Game.getChess960Positions();
		return chess960positions.indexOf(pos);
	}

	public Game blackToMove()
	{
		if(this.toMove != Color.BLACK)
		{
			if(this.status != GameStatus.BUILDING)
			{
				this.exportable = false;
			}
			this.toMove = Color.BLACK;
		}
		return this;
	}

	public Game opponentToMove()
	{
		if(this.status != GameStatus.BUILDING)
		{
			this.exportable = false;
		}
		this.toMove = this.toMove.opposite();
		return this;
	}

	public Game resetMoveTime()
	{
		this.plyStart = System.currentTimeMillis();
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
		else if((toMove == Color.WHITE ? blackmsecs : whitemsecs) < 0)
		{
			endReason = EndReason.TIMEOUT;
		}
		else if(drawPlyTimer > 150)
		{
			endReason = EndReason.SEVENTY_FIVE_MOVE_RULE;
		}
		else if(drawPlyTimer > 100)
		{
			claimableDraw = EndReason.FIFTY_MOVE_RULE;
		}
		else if(variant == Variant.STANDARD || variant == Variant.CHESS960)
		{
			boolean definitelySufficientMaterial = false;
			synchronized(pieces)
			{
				for(Square s : pieces)
				{
					if(s.pieceType != PieceType.KING && s.pieceType != PieceType.KNIGHT && s.pieceType != PieceType.BISHOP)
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
					for(Square s : pieces)
					{
						if(s.pieceType == PieceType.KNIGHT)
						{
							if(s.pieceColor == Color.WHITE)
							{
								whiteKnights++;
							}
							else
							{
								blackKnights++;
							}
						}
						else if(s.pieceType == PieceType.BISHOP)
						{
							if(s.pieceColor == Color.WHITE)
							{
								if(s.isWhite())
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
								if(s.isWhite())
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
				if(((whiteKnights == 0 && (!whiteHasWhiteBishop || !whiteHasBlackBishop)) || (whiteKnights == 1 && !whiteHasWhiteBishop && !whiteHasBlackBishop)) && ((blackKnights == 0 && (!blackHasWhiteBishop || !blackHasBlackBishop)) || (blackKnights == 1 && !blackHasWhiteBishop && !blackHasBlackBishop)))
				{
					endReason = EndReason.INSUFFICIENT_MATERIAL;
				}
			}
		}
		else if(variant == Variant.THREE_CHECK)
		{
			boolean sufficientMaterial = false;
			synchronized(pieces)
			{
				for(Square s : pieces)
				{
					if(s.pieceType != PieceType.KING)
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
			if(endReason.isDraw)
			{
				status = GameStatus.DRAW;
			}
			else
			{
				if(endReason == EndReason.TIMEOUT || variant == Variant.ANTICHESS)
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

	public ArrayList<Square> getPieces(Color owner)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		synchronized(this.pieces)
		{
			for(Square s : this.pieces)
			{
				if(s.pieceColor == owner)
				{
					squares.add(s);
				}
			}
		}
		return squares;
	}

	public ArrayList<Square> getPieces(Color owner, PieceType pieceType)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		synchronized(this.pieces)
		{
			for(Square s : this.pieces)
			{
				if(s.pieceColor == owner && s.pieceType == pieceType)
				{
					squares.add(s);
				}
			}
		}
		return squares;
	}

	public ArrayList<Square> getPieces(PieceType pieceType)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		synchronized(this.pieces)
		{
			for(Square s : this.pieces)
			{
				if(s.pieceType == pieceType)
				{
					squares.add(s);
				}
			}
		}
		return squares;
	}

	public ArrayList<Square> getSquaresControlledBy(Square square)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		if(square.pieceType == PieceType.PAWN)
		{
			final byte file = square.file;
			final byte rank = square.rank;
			Square square_;
			if(square.pieceColor == Color.WHITE ? rank < 7 : rank > 0)
			{
				square_ = this.square(file, (byte) (rank + (square.pieceColor == Color.WHITE ? 1 : -1)));
				if(!square_.hasPiece())
				{
					squares.add(this.square(file, (byte) (rank + (square.pieceColor == Color.WHITE ? 1 : -1))));
					if((rank == 1 && square.pieceColor == Color.WHITE) || (rank == 6 && square.pieceColor == Color.BLACK))
					{
						square_ = this.square(file, (byte) (rank + (square.pieceColor == Color.WHITE ? 2 : -2)));
						if(!square_.hasPiece())
						{
							squares.add(this.square(file, (byte) (rank + (square.pieceColor == Color.WHITE ? 2 : -2))));
						}
					}
				}
			}
			if(square.pieceColor == Color.WHITE ? rank < 7 : rank > 0)
			{
				if(file > 0)
				{
					square_ = this.square((byte) (file - 1), (byte) (rank + (square.pieceColor == Color.WHITE ? 1 : -1)));
					if((square_.pieceColor == square.pieceColor.opposite()) || square_.equals(this.enPassantSquare))
					{
						squares.add(square_);
					}
				}
				if(file < 7)
				{
					square_ = this.square((byte) (file + 1), (byte) (rank + (square.pieceColor == Color.WHITE ? 1 : -1)));
					if(square_.pieceColor == square.pieceColor.opposite() || square_.equals(this.enPassantSquare))
					{
						squares.add(square_);
					}
				}
			}
		}
		else if(square.pieceType == PieceType.KING)
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
							final Square square_ = this.square(file_, rank_);
							if(square_.pieceColor != square.pieceColor)
							{
								squares.add(square_);
							}
						}
					}
				}
			}
		}
		else if(square.pieceType == PieceType.KNIGHT)
		{
			if(square.rank > 1)
			{
				if(square.file > 0)
				{
					final Square square_ = this.square((byte) (square.file - 1), (byte) (square.rank - 2));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
				if(square.file < 7)
				{
					final Square square_ = this.square((byte) (square.file + 1), (byte) (square.rank - 2));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
			}
			if(square.rank < 6)
			{
				if(square.file > 0)
				{
					final Square square_ = this.square((byte) (square.file - 1), (byte) (square.rank + 2));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
				if(square.file < 7)
				{
					final Square square_ = this.square((byte) (square.file + 1), (byte) (square.rank + 2));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
			}
			if(square.file > 1)
			{
				if(square.rank > 0)
				{
					final Square square_ = this.square((byte) (square.file - 2), (byte) (square.rank - 1));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
				if(square.rank < 7)
				{
					final Square square_ = this.square((byte) (square.file - 2), (byte) (square.rank + 1));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
			}
			if(square.file < 6)
			{
				if(square.rank > 0)
				{
					final Square square_ = this.square((byte) (square.file + 2), (byte) (square.rank - 1));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
				if(square.rank < 7)
				{
					final Square square_ = this.square((byte) (square.file + 2), (byte) (square.rank + 1));
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
				}
			}
		}
		else
		{
			if(square.pieceType == PieceType.ROOK || square.pieceType == PieceType.QUEEN)
			{
				byte rank = square.rank;
				while(rank < 7)
				{
					rank++;
					final Square square_ = this.square(square.file, rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(square.file, rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(file, square.rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(file, square.rank);
					if(square_.pieceColor != square.pieceColor)
					{
						squares.add(square_);
					}
					if(square_.hasPiece())
					{
						break;
					}
				}
			}
			if(square.pieceType == PieceType.BISHOP || square.pieceType == PieceType.QUEEN)
			{
				byte file = square.file;
				byte rank = square.rank;
				while(file < 7 && rank < 7)
				{
					file++;
					rank++;
					final Square square_ = this.square(file, rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(file, rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(file, rank);
					if(square_.pieceColor != square.pieceColor)
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
					final Square square_ = this.square(file, rank);
					if(square_.pieceColor != square.pieceColor)
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

	public ArrayList<Square> getSquaresControlledBy(Color color)
	{
		final ArrayList<Square> squares = new ArrayList<>();
		for(Square s : this.getPieces(color))
		{
			squares.addAll(this.getSquaresControlledBy(s));
		}
		return squares;
	}

	public ArrayList<Square> getControllers(Square square)
	{
		final ArrayList<Square> controllers = new ArrayList<>();
		synchronized(this.pieces)
		{
			for(Square s : pieces)
			{
				if(this.getSquaresControlledBy(s).contains(square))
				{
					controllers.add(s);
				}
			}
		}
		return controllers;
	}

	public ArrayList<Square> getControllers(Square square, Color by)
	{
		final ArrayList<Square> controllers = new ArrayList<>();
		for(Square s : this.getPieces(by))
		{
			if(s.pieceColor == by && this.getSquaresControlledBy(s).contains(square))
			{
				controllers.add(s);
			}
		}
		return controllers;
	}

	public Game start() throws ChessException
	{
		if(timeControl != TimeControl.UNLIMITED && (this.whitemsecs == 0 || this.blackmsecs == 0) && this.increment == 0)
		{
			throw new ChessException("Refusing to start a 0+0 game.");
		}
		boolean defaultStartPosition = (this.squares == null);
		if(defaultStartPosition)
		{
			this.loadFEN(variant.startFEN);
		}
		this.start = this.copy();
		this.status = GameStatus.ONGOING;
		synchronized(this.repetitionPostitions)
		{
			this.repetitionPostitions.put(this.getPositionalFEN(true), 1);
		}
		if(!defaultStartPosition)
		{
			this.recalculateEndReason(this.isCheck());
		}
		Date date = new Date();
		if(!this.tags.containsKey("UTCDate") && !this.tags.containsKey("Date"))
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
			this.tags.put("Date", formatter.format(date));
			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
			this.tags.put("UTCDate", formatter.format(date));
		}
		if(!this.tags.containsKey("UTCTime") && !this.tags.containsKey("Time"))
		{
			SimpleDateFormat formatter = new SimpleDateFormat("kk:mm:ss");
			tags.put("Time", formatter.format(date));
			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
			tags.put("UTCTime", formatter.format(date));
		}
		if(!this.tags.containsKey("Round"))
		{
			this.tags.put("Round", "?");
		}
		this.plyStart = System.currentTimeMillis();
		return this;
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
		for(Square s : this.getPieces(this.toMove))
		{
			for(Square s_ : this.getSquaresControlledBy(s))
			{
				if(new Move(this, s, s_, null, false).isLegal())
				{
					return false;
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
			Square s = this.getPieces(this.toMove.opposite(), PieceType.KING).get(0);
			if((s.file == 3 || s.file == 4) && (s.rank == 3 || s.rank == 4))
			{
				return true;
			}
		}
		else if(this.variant == Variant.RACING_KINGS)
		{
			return this.getPieces(this.toMove.opposite(), PieceType.KING).get(0).rank == 7;
		}
		else if(this.variant == Variant.HORDE && this.toMove == Color.WHITE)
		{
			if(this.getPieces(Color.WHITE).size() == 0)
			{
				return true;
			}
		}
		if(!isCheck)
		{
			return this.variant == Variant.ANTICHESS && isStalemate(false);
		}
		for(Square s : this.getPieces(this.toMove))
		{
			for(Square s_ : this.getSquaresControlledBy(s))
			{
				if(!new Move(this, s, s_, null, true).commitTo(this.copy(), true).opponentToMove().isCheck())
				{
					return false;
				}
			}
		}
		return true;
	}

	public Game setUnlimitedTime()
	{
		this.timeControl = TimeControl.UNLIMITED;
		return this;
	}

	public Game setTimed(long msecs, long increment)
	{
		this.timeControl = (increment == 0 ? TimeControl.SUDDEN_DEATH : TimeControl.INCREMENT);
		this.whitemsecs = msecs;
		this.blackmsecs = msecs;
		this.increment = increment;
		return this;
	}

	public Game setTag(String key, String value)
	{
		this.tags.put(key, value);
		return this;
	}

	public Game setPlayerNames(String white, String black)
	{
		return this.setWhite(white).setBlack(black);
	}

	public Game setWhite(String name)
	{
		return this.setWhite(name, "?");
	}

	public Game setWhite(String name, String elo)
	{
		return this.setTag("White", name).setTag("WhiteElo", elo);
	}

	public Game setBlack(String name)
	{
		return this.setBlack(name, "?");
	}

	public Game setBlack(String name, String elo)
	{
		return this.setTag("Black", name).setTag("BlackElo", elo);
	}

	public boolean isCheck()
	{
		final ArrayList<Square> squaresControlledByOpponent = this.getSquaresControlledBy(this.toMove.opposite());
		for(Square s : this.getPieces(this.toMove, PieceType.KING))
		{
			if(squaresControlledByOpponent.contains(s))
			{
				return true;
			}
		}
		return false;
	}

	public String getWhiteTime()
	{
		return Game.formatTime(this.whitemsecs, false);
	}

	public String getBlackTime()
	{
		return Game.formatTime(this.blackmsecs, false);
	}

	public String getPositionalFEN()
	{
		return this.getPositionalFEN(false);
	}

	public String getPositionalFEN(final boolean compact)
	{
		StringBuilder sb = new StringBuilder();
		byte emptySquares = 0;
		for(byte rank = 7; rank >= 0; rank--)
		{
			for(byte file = 0; file < 8; file++)
			{
				Square square = this.square(file, rank);
				if(square.hasPiece())
				{
					if(emptySquares > 0)
					{
						sb.append(emptySquares);
						emptySquares = 0;
					}
					sb.append(square.getCharacter(Language.ENGLISH));
				}
				else
				{
					emptySquares++;
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

	public String getFEN()
	{
		return this.getFEN(false);
	}

	public String getFEN(final boolean compact)
	{
		if(compact && drawPlyTimer == 0 && plyCount == 1)
		{
			return this.getPositionalFEN(true);
		}
		return this.getPositionalFEN(compact) + " " + drawPlyTimer + " " + (int) Math.ceil((double) plyCount / 2);
	}

	void determineCastlingAbilities()
	{
		if(this.variant == Variant.CHESS960 || this.variant == Variant.ANTICHESS || this.variant == Variant.RACING_KINGS)
		{
			return;
		}
		Square square = square((byte) 4, (byte) 0);
		if(square.pieceType != PieceType.KING || square.pieceColor != Color.WHITE)
		{
			whiteCanCastle = false;
			whiteCanCastleQueenside = false;
		}
		else
		{
			square = square((byte) 7, (byte) 0);
			if(square.pieceType != PieceType.ROOK || square.pieceColor != Color.WHITE)
			{
				whiteCanCastle = false;
			}
			square = square((byte) 0, (byte) 0);
			if(square.pieceType != PieceType.ROOK || square.pieceColor != Color.WHITE)
			{
				whiteCanCastleQueenside = false;
			}
		}
		square = square((byte) 4, (byte) 7);
		if(square.pieceType != PieceType.KING || square.pieceColor != Color.BLACK)
		{
			blackCanCastle = false;
			blackCanCastleQueenside = false;
		}
		else
		{
			square = square((byte) 7, (byte) 7);
			if(square.pieceType != PieceType.ROOK || square.pieceColor != Color.BLACK)
			{
				blackCanCastle = false;
			}
			square = square((byte) 0, (byte) 7);
			if(square.pieceType != PieceType.ROOK || square.pieceColor != Color.BLACK)
			{
				blackCanCastleQueenside = false;
			}
		}
	}

	public TreeMap<String, String> getExportableTags(CGNVersion cgnVersion)
	{
		final TreeMap<String, String> tags = new TreeMap<>(new PGNTagComparator());
		tags.putAll(this.tags);
		if(tags.containsKey("Annotator"))
		{
			if(tags.containsKey("Event"))
			{
				if(!tags.containsKey("Site"))
				{
					tags.put("Site", "http://compactchess.cc");
				}
			}
			else
			{
				tags.put("Event", "http://compactchess.cc");
			}
		}
		else
		{
			tags.put("Annotator", "http://compactchess.cc");
		}
		if(!tags.containsKey("Event"))
		{
			tags.put("Event", "-");
		}
		if(!tags.containsKey("Site"))
		{
			tags.put("Site", "-");
		}
		if(cgnVersion == null)
		{
			if(!tags.containsKey("Date"))
			{
				if(tags.containsKey("UTCDate"))
				{
					tags.put("Date", tags.get("UTCDate"));
				}
				else
				{
					tags.put("Date", "????.??.??");
				}
			}
		}
		else
		{
			if(tags.containsKey("Round"))
			{
				String rnd = tags.get("Round");
				if(rnd.equals("?") || rnd.equals("-"))
				{
					tags.remove("Round");
				}
			}
		}
		if(cgnVersion == null || cgnVersion == CGNVersion.V1)
		{
			if(this.status == GameStatus.WHITE_WINS)
			{
				tags.put("Result", "1-0");
			}
			else if(this.status == GameStatus.BLACK_WINS)
			{
				tags.put("Result", "0-1");
			}
			else if(this.status == GameStatus.DRAW)
			{
				tags.put("Result", "1/2-1/2");
			}
			else
			{
				tags.put("Result", "*");
			}
		}
		if(this.timeControl == TimeControl.INCREMENT)
		{
			tags.put("TimeControl", (this.start.whitemsecs / 1000) + "+" + (this.increment / 1000));
		}
		else if(this.timeControl == TimeControl.SUDDEN_DEATH)
		{
			tags.put("TimeControl", String.valueOf(this.start.whitemsecs / 1000) + "+0");
		}
		else if(cgnVersion == null)
		{
			tags.put("TimeControl", "-");
		}
		if(cgnVersion == null || (this.endReason != EndReason.UNTERMINATED && !this.endReason.pgnName.equals("Normal")))
		{
			tags.put("Termination", endReason.pgnName);
		}
		if(this.variant != Variant.STANDARD)
		{
			tags.put("Variant", this.variant.name);
		}
		if(this.variant == Variant.CHESS960 || !this.start.getFEN(false).equals(this.variant.startFEN))
		{
			if(cgnVersion == null)
			{
				tags.put("SetUp", "1");
			}
			tags.put("FEN", this.start.getFEN(cgnVersion != null));
		}
		if(cgnVersion == null)
		{
			tags.put("PlyCount", String.valueOf(this.plyCount - this.start.plyCount));
		}
		return tags;
	}

	public String toPGN() throws ChessException
	{
		return this.toPGN(false, false, false, AlgebraicNotationVariation.SAN, Language.ENGLISH);
	}

	public String toPGN(AlgebraicNotationVariation anvariation) throws ChessException
	{
		return this.toPGN(false, false, false, anvariation, Language.ENGLISH);
	}

	public String toPGN(Language language) throws ChessException
	{
		return this.toPGN(false, false, false, AlgebraicNotationVariation.SAN, language);
	}

	public String toPGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags) throws ChessException
	{
		return this.toPGN(noTags, noAnnotations, noAnnotationTags, AlgebraicNotationVariation.SAN, Language.ENGLISH);
	}

	public String toPGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags, AlgebraicNotationVariation anvariation) throws ChessException
	{
		return this.toPGN(noTags, noAnnotations, noAnnotationTags, anvariation, Language.ENGLISH);
	}

	public String toPGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags, Language language) throws ChessException
	{
		return this.toPGN(noTags, noAnnotations, noAnnotationTags, AlgebraicNotationVariation.SAN, language);
	}

	public String toPGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags, AlgebraicNotationVariation anvariation, Language language) throws ChessException
	{
		if(!exportable)
		{
			throw new ChessException("The game has been modified in a way that PGN can not express");
		}
		StringBuilder pgn = new StringBuilder();
		final TreeMap<String, String> tags = this.getExportableTags(null);
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
					pgn.append(move.toAlgebraicNotation(anvariation, language)).append(" ");
					if(!noAnnotations && move.hasAnnotation(noAnnotationTags))
					{
						pgn.append("{ ").append(move.getAnnotation(noAnnotationTags)).append(" } ");
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
		return this.toCGN(false, false, false, CGNVersion.latest);
	}

	public byte[] toCGN(CGNVersion version) throws IOException, ChessException
	{
		return this.toCGN(false, false, false, version);
	}

	public byte[] toCGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags) throws IOException, ChessException
	{
		return this.toCGN(noTags, noAnnotations, noAnnotationTags, CGNVersion.latest);
	}

	public byte[] toCGN(boolean noTags, boolean noAnnotations, boolean noAnnotationTags, CGNVersion version) throws IOException, ChessException
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.toCGN(os, noTags, noAnnotations, noAnnotationTags, version);
		final byte[] bytes = os.toByteArray();
		os.close();
		return bytes;
	}

	public void toCGN(OutputStream os) throws IOException, ChessException
	{
		this.toCGN(os, false, false, false, CGNVersion.latest);
	}

	public void toCGN(OutputStream os, CGNVersion version) throws IOException, ChessException
	{
		this.toCGN(os, false, false, false, version);
	}

	public void toCGN(OutputStream os, boolean noTags, boolean noAnnotations, boolean noAnnotationTags) throws IOException, ChessException
	{
		this.toCGN(os, noTags, noAnnotations, noAnnotationTags, CGNVersion.latest);
	}

	public void toCGN(OutputStream os, boolean noTags, boolean noAnnotations, boolean noAnnotationTags, CGNVersion version) throws IOException, ChessException
	{
		if(!exportable)
		{
			throw new ChessException("The game has been modified in a way that CGN can not express");
		}
		if(!noTags)
		{
			final TreeMap<String, String> tags = this.getExportableTags(version);
			if(version == CGNVersion.V1)
			{
				os.write(tags.size());
			}
			for(Map.Entry<String, String> tag : tags.entrySet())
			{
				String key = tag.getKey();
				if(key.equalsIgnoreCase("PlyCount") || key.equalsIgnoreCase("SetUp") || (version != CGNVersion.V1 && key.equalsIgnoreCase("Result")))
				{
					continue;
				}
				String value = tag.getValue();
				if((key.equalsIgnoreCase("Termination") && (value.equalsIgnoreCase("Normal") || value.equalsIgnoreCase("Unterminated"))) || (key.equalsIgnoreCase("Variant") && value.equalsIgnoreCase("Standard")))
				{
					continue;
				}
				CGNTagMap mappedTag;
				if(version == CGNVersion.V1)
				{
					mappedTag = CGNTagMap._FROMSTRING;
				}
				else
				{
					mappedTag = CGNTagMap.fromName(key);
					os.write(mappedTag.ordinal());
				}
				if(mappedTag == CGNTagMap._FROMSTRING)
				{
					os.write(key.getBytes(Charset.forName("UTF-8")));
					os.write(0x00);
				}
				os.write(value.getBytes(Charset.forName("UTF-8")));
				os.write(0x00);
			}
			if(version == CGNVersion.V2)
			{
				os.write(0x00);
			}
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
				if(!noAnnotations && m.hasAnnotation(noAnnotationTags))
				{
					if(version == CGNVersion.V1)
					{
						os.write(0b10000001);
					}
					else
					{
						os.write(0b10000000);
					}
					os.write(m.getAnnotation(noAnnotationTags).getBytes(Charset.forName("UTF-8")));
					os.write(0x00);
				}
			}
		}
		if(version == CGNVersion.V1)
		{
			os.write(0b10000000);
		}
		else
		{
			switch(status)
			{
				case WHITE_WINS:
					os.write(0b10000010);
					break;
				case BLACK_WINS:
					os.write(0b10000011);
					break;
				case DRAW:
					os.write(0b10000100);
					break;
				default:
					os.write(0b10000001);
			}
		}
	}

	public boolean canDrawBeClaimed()
	{
		return this.endReason == EndReason.UNTERMINATED && this.claimableDraw != EndReason.UNTERMINATED;
	}

	public Game claimDraw() throws ChessException
	{
		if(!canDrawBeClaimed())
		{
			throw new ChessException("A draw can not be claimed right now");
		}
		this.endReason = this.claimableDraw;
		this.status = GameStatus.DRAW;
		return this;
	}

	public Game agreeToDraw() throws ChessException
	{
		if(this.endReason != EndReason.UNTERMINATED)
		{
			throw new ChessException("The game is not ongoing");
		}
		this.endReason = EndReason.DRAW_AGREEMENT;
		this.status = GameStatus.DRAW;
		return this;
	}

	public Game resign(Color resigner)
	{
		this.endReason = EndReason.RESIGNATION;
		this.status = (resigner == Color.WHITE ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS);
		return this;
	}

	public ArrayList<Move> getPossibleMoves() throws ChessException
	{
		return this.getPossibleMoves(false);
	}

	public ArrayList<Move> getPossibleMoves(boolean includeIllegal) throws ChessException
	{
		final ArrayList<Move> moves = new ArrayList<>();
		for(Square _s : this.getPieces(this.toMove))
		{
			for(Square s : this.getSquaresControlledBy(_s))
			{
				Move m = new Move(this, _s, s, null, true);
				if(includeIllegal || m.isLegal())
				{
					if(s.pieceType == PieceType.PAWN && (toMove == Color.WHITE ? s.rank == 7 : s.rank == 0))
					{
						for(PieceType pt : this.variant.getPossiblePromotions())
						{
							moves.add(new Move(this, _s, s, pt, true));
						}
					}
					else
					{
						moves.add(m);
					}
				}
			}
		}
		if(toMove == Color.WHITE)
		{
			if(whiteCanCastle)
			{
				try
				{
					Move m = this.uciMove("e1g1");
					if(includeIllegal || m.isLegal())
					{
						moves.add(m);
					}
				}
				catch(InvalidMoveException ignored)
				{

				}
			}
			if(whiteCanCastleQueenside)
			{
				try
				{
					Move m = this.uciMove("e1c1");
					if(includeIllegal || m.isLegal())
					{
						moves.add(m);
					}
				}
				catch(InvalidMoveException ignored)
				{

				}
			}
		}
		else
		{
			if(blackCanCastle)
			{
				try
				{
					Move m = this.uciMove("e8g8");
					if(includeIllegal || m.isLegal())
					{

						moves.add(m);
					}
				}
				catch(InvalidMoveException ignored)
				{

				}
			}
			if(blackCanCastleQueenside)
			{
				try
				{
					Move m = this.uciMove("e8c8");
					if(includeIllegal || m.isLegal())
					{
						moves.add(m);
					}
				}
				catch(InvalidMoveException ignored)
				{

				}
			}
		}
		return moves;
	}

	public ArrayList<Square> getAttackers(Square square)
	{
		if(square.hasPiece())
		{
			return this.getControllers(square, square.pieceColor.opposite());
		}
		return new ArrayList<>();
	}

	public ArrayList<Square> getDefenders(Square square)
	{
		if(square.hasPiece())
		{
			return this.getControllers(square, square.pieceColor);
		}
		return new ArrayList<>();
	}

	public boolean isHanging(Square square)
	{
		boolean attacked = false;
		for(Square s : this.getControllers(square))
		{
			if(s.pieceColor == square.pieceColor)
			{
				return false;
			}
			else if(!attacked)
			{
				attacked = true;
			}
		}
		return attacked;
	}

	public short getMaterialScore(Color perspective)
	{
		short whitescore = 0;
		short blackscore = 0;
		synchronized(this.pieces)
		{
			for(Square s : this.pieces)
			{
				if(s.pieceColor == Color.WHITE)
				{
					whitescore += s.getMaterialValue();
				}
				else
				{
					blackscore += s.getMaterialValue();
				}
			}
		}
		if(perspective == Color.WHITE)
		{
			return (short) (whitescore - blackscore);
		}
		else
		{
			return (short) (blackscore - whitescore);
		}
	}

	public short getMaterialScoreOf(Color color)
	{
		short score = 0;
		for(Square s : this.getPieces(color))
		{
			score += s.getMaterialValue();
		}
		return score;
	}

	public String toString()
	{
		return toString(true, false, null, false);
	}

	public String toString(boolean whitesPerspective)
	{
		return toString(whitesPerspective, false, null, false);
	}

	public String toString(boolean whitesPerspective, boolean noCoordinates)
	{
		return toString(whitesPerspective, noCoordinates, null, false);
	}

	public String toString(boolean whitesPerspective, boolean noCoordinates, Language useLetters)
	{
		return toString(whitesPerspective, noCoordinates, useLetters, false);
	}

	public String toString(boolean whitesPerspective, boolean noCoordinates, Language useLetters, boolean invertColor)
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
				if(useLetters == null)
				{
					sb.append(square(file, rank).getSymbol(invertColor));
				}
				else
				{
					sb.append(square(file, rank).getCharacter(useLetters));
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
				try
				{
					sb.append(Square.fileChar(file).toLowerCase());
				}
				catch(ChessException ignored)
				{

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
		}
		return sb.toString();
	}

	public String toSVG()
	{
		return this.toSVG(true, true);
	}

	public String toSVG(boolean indicateLastMove)
	{
		return this.toSVG(indicateLastMove, true);
	}

	public String toSVG(boolean indicateLastMove, boolean indicateCheck)
	{
		StringBuilder svg = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 360 360\"><rect width=\"360\" height=\"360\" fill=\"#b58863\"/><g id=\"a\"><g id=\"b\"><g id=\"c\"><g id=\"d\"><rect width=\"45\" height=\"45\" fill=\"#f0d9b5\" id=\"e\"/><use x=\"90\" xlink:href=\"#e\"/></g><use x=\"180\" xlink:href=\"#d\"/></g><use x=\"45\" y=\"45\" xlink:href=\"#c\"/></g><use y=\"90\" xlink:href=\"#b\"/></g><use y=\"180\" xlink:href=\"#a\"/>");
		if(indicateLastMove)
		{
			synchronized(moves)
			{
				if(moves.size() > 0)
				{
					Move move = moves.get(moves.size() - 1);
					svg.append("<g transform=\"translate(").append(move.fromSquare.file * 45).append(",").append(315 - (move.fromSquare.rank * 45)).append(")\"><rect width=\"45\" height=\"45\" style=\"fill:rgba(155,199,0,.41)\"></rect></g>");
					Square toSquare = move.toSquare;
					if(this.variant == Variant.CHESS960 && move.castlingType != CastlingType.NONE)
					{
						if(move.castlingType == CastlingType.KINGSIDE)
						{
							if(move.fromSquare.file != 6)
							{
								toSquare = this.square((byte) 6, toSquare.rank);
							}
						}
						else
						{
							if(move.fromSquare.file != 2)
							{
								toSquare = this.square((byte) 2, toSquare.rank);
							}
						}
					}
					svg.append("<g transform=\"translate(").append(toSquare.file * 45).append(",").append(315 - (toSquare.rank * 45)).append(")\"><rect width=\"45\" height=\"45\" style=\"fill:rgba(155,199,0,.41)\"></rect></g>");
				}
			}
		}
		boolean isCheck = (indicateCheck && this.variant != Variant.ANTICHESS && this.isCheck());
		for(byte rank = 0; rank < 8; rank++)
		{
			for(byte file = 0; file < 8; file++)
			{
				Square square = this.square(file, rank);
				if(square.hasPiece())
				{
					String piece_svg = square.getSVG();
					if(isCheck && square.pieceColor == this.toMove && square.pieceType == PieceType.KING)
					{
						svg.append("<g transform=\"translate(").append(file * 45).append(",").append(315 - (rank * 45)).append(")\"><rect width=\"45\" height=\"45\" style=\"fill:rgba(255,0,0,.56)\"></rect></g>");
					}
					svg.append(piece_svg, 0, 2).append(" transform=\"translate(").append(file * 45).append(",").append(315 - (rank * 45)).append(")\"").append(piece_svg.substring(2));
				}
			}
		}
		return svg.append("</svg>").toString();
	}

	public Game copy()
	{
		final Game game = new Game();
		try
		{
			game.loadFEN(this.getPositionalFEN());
		}
		catch(ChessException ignored)
		{
		}
		if(start != null)
		{
			game.start = start.copy();
		}
		game.moves.addAll(moves);
		game.tags.putAll(tags);
		game.repetitionPostitions.putAll(repetitionPostitions);
		game.plyCount = plyCount;
		game.variant = variant;
		game.toMove = toMove;
		if(enPassantSquare != null)
		{
			game.enPassantSquare = enPassantSquare.copy();
		}
		game.timeControl = timeControl;
		game.status = status;
		game.claimableDraw = claimableDraw;
		game.endReason = endReason;
		game.plyStart = plyStart;
		game.increment = increment;
		game.whitemsecs = whitemsecs;
		game.blackmsecs = blackmsecs;
		game.drawPlyTimer = drawPlyTimer;
		game.exportable = exportable;
		return game;
	}

	@Override
	public boolean equals(Object o2)
	{
		if(o2 instanceof Game)
		{
			if(this.getFEN(true).equals(((Game) o2).getFEN(true)) && ((this.start == null && ((Game) o2).start == null) || (this.start != null && ((Game) o2).start != null && this.start.getFEN(true).equals(((Game) o2).start.getFEN(true)))) && this.plyCount == ((Game) o2).plyCount && this.repetitionPostitions.equals(((Game) o2).repetitionPostitions) && this.variant.equals(((Game) o2).variant) && this.toMove.equals(((Game) o2).toMove) && this.timeControl.equals(((Game) o2).timeControl) && this.status == ((Game) o2).status && this.claimableDraw == ((Game) o2).claimableDraw && this.endReason == ((Game) o2).endReason && this.tags.entrySet().equals(((Game) o2).tags.entrySet()) && this.increment == ((Game) o2).increment && this.whitemsecs == ((Game) o2).whitemsecs && this.blackmsecs == ((Game) o2).blackmsecs && this.exportable == ((Game) o2).exportable && this.moves.size() == ((Game) o2).moves.size())
			{
				for(int i = 0; i < this.moves.size(); i++)
				{
					if(!this.moves.get(i).equals(((Game) o2).moves.get(i)))
					{
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}
}

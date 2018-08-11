import org.junit.Test;
import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.engine.EngineBuilder;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.AlgebraicNotationVariation;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.EndReason;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.Piece;
import sh.hell.compactchess.game.PieceType;
import sh.hell.compactchess.game.Square;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class Tests
{
	private static void visualize(Game game)
	{
		System.out.println(game.toString(true));
	}

	private static void evaluatePossibleMoves(int expectedSquares, Game game, ArrayList<Square> squares)
	{
		visualize(game);
		int uniqueSquares = 0;
		for(Square square : squares)
		{
			if(!game.square(square).hasPiece() || game.square(square).getPiece().type != PieceType.KING)
			{
				game.insertPiece(new Piece(PieceType.KING, Color.WHITE), square);
				uniqueSquares++;
			}
		}
		visualize(game);
		assertEquals(expectedSquares, uniqueSquares);
	}

	@Test(timeout = 1000L)
	public void enPassant() throws ChessException
	{
		System.out.println("En Passant\n");
		Game game = new Game().loadFEN("8/8/8/8/1p6/8/P6P/8 w - -").start();
		visualize(game);
		game.uciMove("h2h3").commit();
		visualize(game);
		assertNull(game.enPassantSquare);
		game.opponentToMove();
		game.uciMove("a2a4").commit();
		visualize(game);
		assertEquals("a3", game.enPassantSquare.getAlgebraicNotation());
		game.uciMove("b4a3").commit();
		visualize(game);
		synchronized(game.pieces)
		{
			assertEquals(2, game.pieces.size());
			Piece piece = game.pieces.get(0);
			assertEquals(PieceType.PAWN, piece.type);
			assertEquals(Color.BLACK, piece.color);
			assertEquals("a3", piece.getSquare().getAlgebraicNotation());
		}
	}

	@Test(timeout = 1000L)
	public void promotion() throws ChessException
	{
		System.out.println("Promotion\n");
		Game game = new Game().loadFEN("8/7P/8/8/8/8/7p/8 w").start();
		visualize(game);
		assertEquals(2, game.pieces.size());
		assertEquals(PieceType.PAWN, game.pieces.get(0).type);
		assertEquals(Color.WHITE, game.pieces.get(0).color);
		assertEquals(PieceType.PAWN, game.pieces.get(1).type);
		assertEquals(Color.BLACK, game.pieces.get(1).color);
		Move move = game.uciMove("h7h8q");
		move.toUCI();
		move.commit();
		visualize(game);
		assertEquals(2, game.pieces.size());
		assertEquals(PieceType.QUEEN, game.pieces.get(0).type);
		assertEquals(Color.WHITE, game.pieces.get(0).color);
		move = game.uciMove("h2h1q");
		move.toUCI();
		move.commit();
		visualize(game);
		assertEquals(2, game.pieces.size());
		assertEquals(PieceType.QUEEN, game.pieces.get(1).type);
		assertEquals(Color.BLACK, game.pieces.get(1).color);
		move = game.uciMove("h8h1");
		move.toUCI();
		move.commit();
		visualize(game);
		assertEquals(1, game.pieces.size());
		assertEquals(PieceType.QUEEN, game.pieces.get(0).type);
		assertEquals(Color.WHITE, game.pieces.get(0).color);
	}

	@Test(timeout = 1000L)
	public void castling() throws ChessException
	{
		System.out.println("Castling\n");
		Game game = new Game().loadFEN("4k2r/8/8/3q3Q/8/8/8/R3K3 b Qk -").start();
		game.determineCastlingAbilities();
		assertFalse(game.whiteCanCastle);
		assertTrue(game.whiteCanCastleQueenside);
		assertTrue(game.blackCanCastle);
		assertFalse(game.blackCanCastleQueenside);
		visualize(game);
		assertFalse(game.uciMove("e8g8").isLegal());
		game.uciMove("d5h5").commit();
		visualize(game);
		Move move = game.uciMove("e1c1");
		move.commit(true, false);
		visualize(game);
		assertFalse(move.isLegal());
		synchronized(game.pieces)
		{
			for(Piece p : game.pieces)
			{
				if(p.type == PieceType.ROOK && p.color == Color.WHITE)
				{
					assertEquals(3, p.getSquare().file);
					break;
				}
			}
		}
		game.uciMove("e8g8").commit();
		visualize(game);
		synchronized(game.pieces)
		{
			for(Piece p : game.pieces)
			{
				if(p.type == PieceType.ROOK && p.color == Color.BLACK)
				{
					assertEquals(5, p.getSquare().file);
					break;
				}
			}
		}
	}

	@Test(timeout = 1000L)
	public void legality() throws ChessException
	{
		System.out.println("Legality\n");
		final Game game = new Game().loadFEN("8/8/8/8/4b3/8/6P1/7K w").start();
		visualize(game);
		final Move move = game.uciMove("g2g3");
		move.commit(true, false);
		visualize(game);
		assertFalse(move.isLegal());
	}

	@Test(timeout = 1000L)
	public void check() throws ChessException
	{
		System.out.println("Check\n");
		Game game = new Game().loadFEN("5k2/8/8/8/8/8/8/4K2R w K").start();
		visualize(game);
		assertFalse(game.isCheck());
		assertNotEquals(EndReason.CHECKMATE, game.endReason);
		Move move = game.uciMove("e1g1");
		move.commit();
		visualize(game);
		assertTrue(move.isLegal());
		assertTrue(move.isCheck());
		assertNotEquals(EndReason.CHECKMATE, game.endReason);
		move = game.uciMove("f8f7");
		move.commit(true, false);
		visualize(game);
		assertFalse(move.isLegal());
	}

	@Test(timeout = 1000L)
	public void stalemate() throws ChessException
	{
		System.out.println("Stalemate\n");
		final Game game = new Game().loadFEN("k1K5/pp6/N7/8/8/8/8/7B b").start();
		visualize(game);
		assertFalse(game.isCheck());
		assertEquals(EndReason.STALEMATE, game.endReason);
		game.opponentToMove();
		assertFalse(game.isCheck());
	}

	@Test(timeout = 1000L)
	public void checkmate() throws ChessException
	{
		System.out.println("Checkmate\n");
		Game game = new Game().loadFEN("8/8/8/3b4/8/Kk6/8/8 w - -").start();
		visualize(game);
		assertTrue(game.isCheck());
		assertEquals(EndReason.CHECKMATE, game.endReason);
		game = new Game().loadFEN("5r2/3q2kp/6p1/1bpBP1K1/1p1p3P/6P1/3P1P2/r7 b - - 0 40").start();
		visualize(game);
		assertFalse(game.isCheck());
		game.uciMove("d7f5").commit();
		visualize(game);
		assertTrue(game.isCheck());
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(game.endReason, EndReason.CHECKMATE);
		assertEquals(game.status, GameStatus.BLACK_WINS);
	}

	@Test(timeout = 1000L)
	public void materialDraw() throws ChessException
	{
		System.out.println("Draw by Insufficient Material\n");
		final Game game = new Game().loadFEN("4k3/8/2b5/8/4P3/8/6B1/4K3 b").start();
		visualize(game);
		game.uciMove("c6e4").commit();
		visualize(game);
		assertEquals(game.endReason, EndReason.INSUFFICIENT_MATERIAL);
		assertEquals(game.status, GameStatus.DRAW);
	}

	@Test(timeout = 1000L)
	public void algebraicNotation() throws ChessException
	{
		System.out.println("Algebraic Notation (AN)\n");
		Game game = new Game().loadFEN("7r/6P1/8/8/8/8/8/8 w").start();
		visualize(game);
		Move move = game.uciMove("g7h8q");
		assertEquals(game.move("g7h8q"), move);
		assertEquals(game.move("gxh8=Q"), move);
		assertEquals(game.move("gxh8=♕"), move);
		assertEquals(game.move("gh8=Q"), move);
		assertEquals(game.move("g7h8=Q"), move);
		assertEquals(game.move("g7xRh8=Q"), move);
		move.commit();
		visualize(game);
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.SAN), "gxh8=Q");
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.FIDE_SAN), "gxh8=Q");
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.FAN), "gxh8=♕");
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.MAN), "gh8=Q");
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.LAN), "g7xh8=Q");
		assertEquals(move.toAlgebraicNotation(AlgebraicNotationVariation.RAN), "g7xRh8=Q");
		game = new Game().loadFEN("8/8/8/8/8/3R4/8/3R4 w - -").start();
		visualize(game);
		move = game.move("R3d2");
		assertNotNull(move);
		move.commit();
		visualize(game);
	}

	@Test(timeout = 1000L)
	public void fen() throws ChessException
	{
		System.out.println("FEN\n");
		String fen = "NnBbRrQq/nnBbRrQq/BBBbRrQq/bbbbRrQq/RRRRRrQq/rrrrrrQq/QQQQQQQq/qqqqqqqq w - -";
		final Game game = new Game().loadFEN(fen).start();
		visualize(game);
		assertEquals(64, game.pieces.size());
		assertEquals(64, game.squares.length);
		assertEquals(fen, game.getPositionalFEN());
	}

	@Test(timeout = 1000L)
	public void pgn() throws ChessException
	{
		System.out.println("PGN\n");
		Game game = new Game().loadFEN("3k4/8/8/8/8/8/7p/R3K3 w Q -").start();
		visualize(game);
		game.uciMove("e1c1").annotate("Annotation!").commit();
		visualize(game);
		game.uciMove("d8e7").commit();
		visualize(game);
		game.uciMove("c1b1").commit();
		visualize(game);
		game.uciMove("h2h1q").commit();
		visualize(game);
		final String pgn = game.toPGN();
		System.out.println(pgn);
		Game game_ = Game.fromPGN(pgn).get(0);
		visualize(game_);
		assertTrue(pgn.contains("1. O-O-O+ { Annotation! } Ke7 2. Kb1 h1=Q *"));
		assertEquals(game, game_);
		assertEquals("Annotation!", game_.moves.get(0).annotation);
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.FIDE_SAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.FAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.MAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.LAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.RAN)).get(0));
		game = new Game().loadFEN("8/2K5/4q3/8/3N1N2/6b1/8/7k w - -").start();
		game.move("Nxe6");
	}

	@Test(timeout = 1000L)
	public void cgn() throws ChessException, IOException
	{
		System.out.println("CGN\n");
		final Game game = new Game().loadFEN("3k4/8/8/8/8/8/7p/R3K3 w Q -").start();
		visualize(game);
		game.uciMove("e1c1").annotate("Annotation!").commit();
		visualize(game);
		game.uciMove("d8e7").commit();
		visualize(game);
		game.uciMove("c1b1").commit();
		visualize(game);
		game.uciMove("h2h1q").commit();
		visualize(game);
		final byte[] cgn = game.toCGN();
		for(byte b : cgn)
		{
			System.out.println(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0') + " " + new String(new byte[]{b}, Charset.forName("UTF-8")));
		}
		System.out.println();
		final Game game_ = Game.fromCGN(new ByteArrayInputStream(cgn)).get(0);
		visualize(game_);
		assertEquals(game, game_);
		assertEquals("Annotation!", game_.moves.get(0).annotation);
	}

	// Engine

	@Test(timeout = 2000L)
	public void engine() throws ChessException, IOException
	{
		System.out.println("Engine\n");
		final Game game = new Game().loadFEN("1k6/8/K2BB3/8/8/8/8/8 b").start();
		visualize(game);
		Engine engine = new Engine("stockfish_9.exe", 1);
		engine.evaluate(game, 1000).awaitConclusion();
		assertNotNull(engine.getPonder());
		assertEquals("e6d5", engine.getPonder().toUCI());
		assertTrue(engine.foundMate());
		assertEquals(1, engine.getMateIn());
		assertEquals(Color.WHITE, engine.getMater());
		assertEquals(Color.BLACK, engine.getMatee());
		assertNotNull(engine.bestMove);
		assertEquals("b8a8", engine.bestMove);
		engine.getBestMove().commit();
		visualize(game);
		engine.evaluate(game, 1000).awaitConclusion();
		assertTrue(engine.foundMate());
		assertEquals(1, engine.getMateIn());
		assertEquals(Color.WHITE, engine.getMater());
		assertEquals(Color.BLACK, engine.getMatee());
		assertNotNull(engine.bestMove);
		assertEquals("e6d5", engine.bestMove);
		engine.getBestMove().commit();
		visualize(game);
		engine.dispose();
	}

	@Test(timeout = 2000L)
	public void engineTimeouter() throws ChessException, IOException
	{
		final Game game = new Game().loadFEN("5Bbk/q7/8/p7/8/K7/5P2/6Q1 w").start();
		Engine engine = new EngineBuilder("stockfish_9.exe", 1).build();
		engine.evaluateDepth(game, 40, 200).awaitConclusion().dispose();
	}

	@Test(timeout = 2000L)
	public void engineKiller() throws ChessException, IOException
	{
		System.out.println("Engine Killer\n");
		final Game game = new Game().loadFEN("5Bbk/q7/8/p7/8/K7/5P2/6Q1 w").start();
		Engine engine = new EngineBuilder("stockfish_9.exe", 1).build();
		engine.evaluateDepth(game, 40, 0);
		engine.killer.killIn(200);
		engine.awaitConclusion();
		engine.dispose();
	}

	// Variants

	@Test(timeout = 500L)
	public void antichess() throws ChessException
	{
		System.out.println("Antichess\n");
		final Game game = new Game(Variant.ANTICHESS).loadFEN("8/8/8/2Kp4/8/8/8/8 w").start();
		visualize(game);
		assertFalse(game.uciMove("c5b5").isLegal());
		final Move move = game.uciMove("c5d5");
		assertTrue(move.isLegal());
		move.commit();
		visualize(game);
		assertFalse(game.isCheck());
		assertFalse(move.isCheck());
		assertTrue(game.isCheckmate(false));
		assertTrue(move.isCheckmate(false));
		assertEquals(GameStatus.BLACK_WINS, game.status);
		assertEquals(EndReason.CHECKMATE, game.endReason);
	}

	@Test(timeout = 1000L)
	public void threeCheck() throws ChessException
	{
		final Game game = Game.fromPGN("[Variant \"Three-check\"]\n[FEN \"k7/8/8/8/8/8/3q4/7K w\"]\n\n1. Kg1 Qd1+ 2. Kg2 Qd2+ 3. Kg3 Qe1+").get(0);
		assertTrue(game.isCheck());
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void kingOfTheHill() throws ChessException
	{
		final Game game = new Game(Variant.KING_OF_THE_HILL).loadFEN("8/8/4k3/8/3P4/2P1K3/8/8 w").start();
		game.uciMove("e3e4").commit();
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.WHITE_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void horde() throws ChessException
	{
		System.out.println("Horde\n");
		Game game = new Game(Variant.HORDE).loadFEN("k7/1Q6/2P5/8/8/8/8/8 b").start();
		visualize(game);
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.WHITE_WINS, game.status);
		game = new Game(Variant.HORDE).loadFEN("8/8/2Pk4/8/8/8/8/8 b - -").start();
		visualize(game);
		game.uciMove("d6c6").commit();
		visualize(game);
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void racingKings() throws ChessException
	{
		System.out.println("Racing Kings\n");
		final Game game = new Game(Variant.RACING_KINGS).loadFEN("8/k6K/8/4q3/8/8/8/8 b").start();
		visualize(game);
		assertFalse(game.uciMove("e5h6").isLegal());
		final Move move = game.uciMove("a7a8");
		move.commit();
		visualize(game);
		assertTrue(move.isCheckmate());
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	// Possible Moves

	@Test(timeout = 1000L)
	public void rook() throws ChessException
	{
		System.out.println("Possible Moves: Rook\n");
		final Game game = new Game().loadFEN("8/5p2/8/8/8/8/8/2B2RN1 w").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.type == PieceType.ROOK)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(8, game, squares);
	}

	@Test(timeout = 1000L)
	public void pawn() throws ChessException
	{
		System.out.println("Possible Moves: Pawn\n");
		final Game game = new Game().loadFEN("8/8/8/4Pp2/8/8/P2P3P/8 w - f6 0 2").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.color == Color.WHITE && piece.type == PieceType.PAWN)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(8, game, squares);
	}

	@Test(timeout = 1000L)
	public void king() throws ChessException
	{
		System.out.println("Possible Moves: King\n");
		final Game game = new Game().loadFEN("7k/8/3k2k1/8/8/1k6/8/k7 w - -").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.type == PieceType.KING)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(26, game, squares);
	}

	@Test(timeout = 1000L)
	public void bishop() throws ChessException
	{
		System.out.println("Possible Moves: Bishop\n");
		final Game game = new Game().loadFEN("8/8/8/8/3B4/8/8/8 w").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.type == PieceType.BISHOP)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(13, game, squares);
	}

	@Test(timeout = 1000L)
	public void queen() throws ChessException
	{
		System.out.println("Possible Moves: Queen\n");
		final Game game = new Game().loadFEN("8/8/8/8/3Q4/8/8/8 w").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.type == PieceType.QUEEN)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(27, game, squares);
	}

	@Test(timeout = 1000L)
	public void knight() throws ChessException
	{
		System.out.println("Possible Moves: Queen\n");
		final Game game = new Game().loadFEN("8/1N4N1/8/8/3N4/8/1N4N1/8 w").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Piece piece : game.pieces)
		{
			if(piece.type == PieceType.KNIGHT)
			{
				squares.addAll(piece.getControlledSquares(game));
			}
		}
		evaluatePossibleMoves(22, game, squares);
	}

	@Test(timeout = 1000L)
	public void timeControl() throws ChessException
	{
		final Game game = new Game();
		game.setUnlimitedTime();
		assertEquals(TimeControl.UNLIMITED, game.timeControl);
		game.setTimed(60000, 0);
		assertEquals(TimeControl.SUDDEN_DEATH, game.timeControl);
		assertEquals(60000, game.whitemsecs);
		assertEquals(60000, game.blackmsecs);
		game.setTimed(30000, 2000);
		assertEquals(TimeControl.INCREMENT, game.timeControl);
		assertEquals(30000, game.whitemsecs);
		assertEquals(30000, game.blackmsecs);
		assertEquals(2000, game.increment);
	}
}

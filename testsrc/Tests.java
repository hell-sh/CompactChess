import org.junit.Test;
import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.AlgebraicNotationVariation;
import sh.hell.compactchess.game.CGNVersion;
import sh.hell.compactchess.game.CastlingType;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.EndReason;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Language;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.PieceType;
import sh.hell.compactchess.game.Square;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class Tests
{
	private static void visualize(Game game)
	{
		System.out.println(game.toString(true, false, null, true));
	}

	private static void evaluatePossibleMoves(int expectedSquares, Game game, ArrayList<Square> squares)
	{
		visualize(game);
		int uniqueSquares = 0;
		for(Square square : squares)
		{
			if(square.pieceType != PieceType.KING)
			{
				game.setPiece(square, Color.WHITE, PieceType.KING);
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
		Game game = new Game().loadFEN("8/2p5/8/8/1p6/8/P7/8 b - -").start();
		visualize(game);
		game.uciMove("c7c6").commit();
		visualize(game);
		assertNull(game.enPassantSquare);
		game.uciMove("a2a4").commit();
		visualize(game);
		assertEquals("a3", game.enPassantSquare.getAlgebraicNotation());
		game.uciMove("b4a3").commit();
		visualize(game);
		assertEquals(2, game.pieces.size());
		assertTrue(game.square("c6").hasPiece());
		game.unsetPiece(game.square("c6"));
		assertEquals(1, game.pieces.size());
		Square square = game.pieces.get(0);
		assertEquals(PieceType.PAWN, square.pieceType);
		assertEquals(Color.BLACK, square.pieceColor);
		assertEquals("a3", square.getAlgebraicNotation());
	}

	@Test(timeout = 1000L)
	public void promotion() throws ChessException
	{
		System.out.println("Promotion\n");
		Game game = new Game().loadFEN("8/7P/8/8/8/8/7p/8 w").start();
		visualize(game);
		int whitePawns = 0;
		int blackPawns = 0;
		for(Square s : game.squares)
		{
			if(s.hasPiece())
			{
				if(s.pieceType == PieceType.PAWN)
				{
					if(s.pieceColor == Color.WHITE)
					{
						whitePawns++;
					}
					else
					{
						blackPawns++;
					}
				}
				else
				{
					fail();
				}
			}
		}
		assertEquals(1, whitePawns);
		assertEquals(1, blackPawns);
		Move move = game.uciMove("h7h8q");
		move.toUCI();
		move.commit();
		visualize(game);
		blackPawns = 0;
		int whiteQueens = 0;
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.QUEEN)
			{
				assertEquals(Color.WHITE, s.pieceColor);
				whiteQueens++;
			}
			else if(s.pieceType == PieceType.PAWN)
			{
				assertEquals(Color.BLACK, s.pieceColor);
				blackPawns++;
			}
			else
			{
				fail();
			}
		}
		assertEquals(1, whiteQueens);
		assertEquals(1, blackPawns);
		move = game.uciMove("h2h1q");
		move.toUCI();
		move.commit();
		visualize(game);
		assertQueens(game, 1);
		move = game.uciMove("h8h1");
		move.toUCI();
		move.commit();
		visualize(game);
		assertQueens(game, 0);
	}

	private void assertQueens(Game game, int expectedBlackQueens)
	{
		int whiteQueens = 0;
		int blackQueens = 0;
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.QUEEN)
			{
				if(s.pieceColor == Color.WHITE)
				{
					whiteQueens++;
				}
				else
				{
					blackQueens++;
				}
			}
			else
			{
				fail();
			}
		}
		assertEquals(1, whiteQueens);
		assertEquals(expectedBlackQueens, blackQueens);
	}

	@Test(timeout = 1000L)
	public void castling() throws ChessException
	{
		System.out.println("Castling\n");
		Game game = new Game().loadFEN("4k2r/8/8/3q3Q/8/8/8/R3K3 b").start();
		assertFalse(game.whiteCanCastle);
		assertTrue(game.whiteCanCastleQueenside);
		assertTrue(game.blackCanCastle);
		assertFalse(game.blackCanCastleQueenside);
		visualize(game);
		assertNotNull(game.uciMove("e8g8").getIllegalReason());
		game.uciMove("d5h5").commit();
		visualize(game);
		Move move = game.uciMove("e1c1");
		move.commit(true, false);
		visualize(game);
		assertEquals(CastlingType.QUEENSIDE, move.castlingType);
		assertNotNull(move.getIllegalReason());
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.ROOK && s.pieceColor == Color.WHITE)
			{
				assertEquals(3, s.file);
				break;
			}
		}
		move = game.uciMove("e8g8");
		move.commit();
		visualize(game);
		assertEquals(CastlingType.KINGSIDE, move.castlingType);
		assertNull(move.getIllegalReason());
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.ROOK && s.pieceColor == Color.BLACK)
			{
				assertEquals(5, s.file);
				break;
			}
		}
		game = new Game().loadFEN("rn2k3/8/1R6/8/8/8/8/4K3 b q -").start();
		visualize(game);
		assertNotNull(game.uciMove("e8c8").getIllegalReason());
		move = game.uciMove("b8c6");
		visualize(move.commit());
		assertNull(move.getIllegalReason());
		game.opponentToMove();
		move = game.uciMove("e8c8");
		visualize(move.commit());
		assertNull(move.getIllegalReason());
	}

	@Test(timeout = 1000L)
	public void controllers() throws ChessException
	{
		Game game = new Game().loadFEN("rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq -").start();
		Square s = game.square("e4");
		assertEquals(1, game.getControllers(s).size());
		assertEquals(1, game.getAttackers(s).size());
		assertEquals(0, game.getDefenders(s).size());
		assertTrue(game.isHanging(s));
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
		assertNotNull(move.getIllegalReason());
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
		assertNull(move.getIllegalReason());
		assertTrue(move.isCheck());
		assertNotEquals(EndReason.CHECKMATE, game.endReason);
		move = game.uciMove("f8f7");
		move.commit(true, false);
		visualize(game);
		assertNotNull(move.getIllegalReason());
		move.commit();
		assertEquals(EndReason.RULES_INFRACTION, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void stalemate() throws ChessException
	{
		System.out.println("Stalemate\n");
		final Game game = new Game().loadFEN("k1K5/pp6/N7/8/8/8/8/7B b").start();
		visualize(game);
		assertEquals(EndReason.STALEMATE, game.endReason);
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
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void repetitionDraw() throws ChessException
	{
		final Game game = new Game().loadFEN("8/6k1/8/8/8/8/1KQ5/8 w - -").start(); // 1
		game.uciMove("c2d2").commit();
		game.uciMove("g7f7").commit();
		game.uciMove("d2c2").commit();
		game.uciMove("f7g7").commit(); // 2
		game.uciMove("c2d2").commit();
		game.uciMove("g7f7").commit();
		game.uciMove("d2c2").commit();
		assertFalse(game.canDrawBeClaimed());
		game.uciMove("f7g7").commit(); // 3
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		assertEquals(GameStatus.ONGOING, game.status);
		assertTrue(game.canDrawBeClaimed());
		assertEquals(EndReason.THREEFOLD_REPETITION, game.claimableDraw);
		final Game gameCopy = game.copy();
		gameCopy.claimDraw();
		assertEquals(EndReason.THREEFOLD_REPETITION, gameCopy.endReason);
		assertEquals(GameStatus.DRAW, gameCopy.status);
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		assertEquals(GameStatus.ONGOING, game.status);
		game.uciMove("c2d2").commit();
		game.uciMove("g7f7").commit();
		game.uciMove("d2c2").commit();
		game.uciMove("f7g7").commit(); // 4
		game.uciMove("c2d2").commit();
		game.uciMove("g7f7").commit();
		game.uciMove("d2c2").commit();
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		assertEquals(GameStatus.ONGOING, game.status);
		game.uciMove("f7g7").commit(); // 5
		assertEquals(EndReason.FIVEFOLD_REPETITION, game.endReason);
		assertEquals(GameStatus.DRAW, game.status);
	}

	@Test(timeout = 1000L)
	public void materialDraw() throws ChessException
	{
		System.out.println("Draw by Insufficient Material\n");
		Game game = new Game().loadFEN("4k3/8/2b5/8/4P3/8/6B1/4K3 b").start();
		visualize(game);
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		assertEquals(GameStatus.ONGOING, game.status);
		game.uciMove("c6e4").commit();
		visualize(game);
		assertEquals(EndReason.INSUFFICIENT_MATERIAL, game.endReason);
		assertEquals(GameStatus.DRAW, game.status);
		game = new Game().loadFEN("8/8/8/2k1NK2/4B3/8/8/8 w - -").start();
		visualize(game);
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		assertEquals(GameStatus.ONGOING, game.status);
		game = new Game().loadFEN("8/8/8/2k2K2/2b2B2/8/8/8 w - -").start();
		visualize(game);
		assertEquals(EndReason.INSUFFICIENT_MATERIAL, game.endReason);
		assertEquals(GameStatus.DRAW, game.status);
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
		String pgn = game.toPGN(false, false, true);
		System.out.println(pgn);
		Game game_ = Game.fromPGN(pgn).get(0);
		visualize(game_);
		assertTrue(pgn.contains("1. O-O-O+ { Annotation! } Ke7 2. Kb1 h1=Q *"));
		assertEquals(game, game_);
		assertEquals("Annotation!", game_.moves.get(0).getAnnotation());
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.FIDE_SAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.FAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.MAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.LAN)).get(0));
		assertEquals(game, Game.fromPGN(game.toPGN(AlgebraicNotationVariation.RAN)).get(0));
		pgn = game.toPGN(false, false, true, Language.GERMAN);
		System.out.println(pgn);
		game_ = Game.fromPGN(pgn, Language.GERMAN).get(0);
		visualize(game_);
		assertEquals(game, game_);
		assertTrue(pgn.contains("1. O-O-O+ { Annotation! } Ke7 2. Kb1 h1=D *"));
		game = new Game().loadFEN("8/2K5/4q3/8/3N1N2/6b1/8/7k w - -").start();
		game.move("Nxe6");
		game = Game.fromPGN("1.e4").get(0);
		assertEquals(1, game.moves.size());
		assertEquals("e4", game.moves.get(0).toAlgebraicNotation());
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
		byte[] cgn = game.toCGN(false, false, true);
		for(byte b : cgn)
		{
			System.out.println(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0') + " " + new String(new byte[]{b}, Charset.forName("UTF-8")).replace("\n", "NL"));
		}
		System.out.println();
		Game game_ = Game.fromCGN(new ByteArrayInputStream(cgn)).get(0);
		visualize(game_);
		assertEquals(game, game_);
		assertEquals("Annotation!", game_.moves.get(0).getAnnotation());
		cgn = game.toCGN(false, false, true, CGNVersion.V1);
		for(byte b : cgn)
		{
			System.out.println(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0') + " " + new String(new byte[]{b}, Charset.forName("UTF-8")).replace("\n", "NL"));
		}
		System.out.println();
		game_ = Game.fromCGN(new ByteArrayInputStream(cgn), false, CGNVersion.V1).get(0);
		visualize(game_);
		assertEquals(game, game_);
		assertEquals("Annotation!", game_.moves.get(0).getAnnotation());
	}

	@Test(timeout = 10000L)
	public void engine() throws ChessException, IOException, InterruptedException
	{
		System.out.println("Engine\n");
		final Game game = new Game().loadFEN("1k6/8/K2BB3/8/8/8/8/8 b").start();
		visualize(game);
		final HashMap<String, String> uciOptions = new HashMap<>();
		uciOptions.put("Ponder", "true");
		final String osName = System.getProperty("os.name", "generic").toLowerCase();
		final Engine engine = new Engine(osName.contains("win") && !osName.contains("dar") ? "stockfish 10.exe" : "./stockfish_10_x64", null, uciOptions, true);
		engine.evaluateDepth(game, 3).awaitConclusion();
		assertNotNull(engine.getPonder());
		assertEquals("e6d5", engine.getPonder().toUCI());
		assertTrue(engine.foundMate());
		assertEquals(1, engine.getMateIn());
		assertEquals(Color.WHITE, engine.getMater());
		assertEquals(Color.BLACK, engine.getMatee());
		assertNotNull(engine.bestMove);
		assertEquals("b8a8", engine.bestMove);
		visualize(engine.getBestMove().commit());
		engine.evaluateDepth(game, 3).awaitConclusion();
		assertTrue(engine.foundMate());
		assertEquals(1, engine.getMateIn());
		assertEquals(Color.WHITE, engine.getMater());
		assertEquals(Color.BLACK, engine.getMatee());
		assertNotNull(engine.bestMove);
		assertEquals("e6d5", engine.bestMove);
		visualize(engine.getBestMove().commit());
		engine.interrupt();
	}

	// Variants

	@Test(timeout = 500L)
	public void antichess() throws ChessException
	{
		System.out.println("Antichess\n");
		final Game game = new Game(Variant.ANTICHESS).loadFEN("8/8/8/2Kp4/8/8/8/8 w").start();
		visualize(game);
		assertNotNull(game.uciMove("c5b5").getIllegalReason());
		final Move move = game.uciMove("c5d5");
		assertNull(move.getIllegalReason());
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
		assertNotNull(game.uciMove("e5h5").getIllegalReason());
		final Move move = game.uciMove("a7a8");
		move.commit();
		visualize(game);
		assertTrue(move.isCheckmate());
		assertEquals(EndReason.CHECKMATE, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}

	@Test(timeout = 1000L)
	public void chess960() throws ChessException
	{
		System.out.println("Chess960\n");
		Game game = new Game(Variant.CHESS960).loadChess960Position(959).start();
		visualize(game);
		assertEquals(959, game.getChess960PositionID());
		game = new Game(Variant.CHESS960).loadFEN("3rk3/8/8/8/8/8/8/4KR2 w Kq -").start();
		visualize(game);
		assertTrue(game.whiteCanCastle);
		Move move = game.uciMove("e1f1");
		move.commit(true, false);
		visualize(game);
		assertFalse(game.whiteCanCastle);
		assertEquals(CastlingType.KINGSIDE, move.castlingType);
		assertNull(move.getIllegalReason());
		for(Square s : game.getPieces(Color.WHITE))
		{
			if(s.pieceType == PieceType.ROOK)
			{
				assertEquals(5, s.file);
			}
			else if(s.pieceType == PieceType.KING)
			{
				assertEquals(6, s.file);
			}
		}
		assertTrue(game.blackCanCastleQueenside);
		move = game.move("O-O-O");
		move.commit(true, false);
		visualize(game);
		assertFalse(game.blackCanCastleQueenside);
		assertEquals(CastlingType.QUEENSIDE, move.castlingType);
		assertNull(move.getIllegalReason());
		for(Square s : game.getPieces(Color.BLACK))
		{
			if(s.pieceType == PieceType.ROOK)
			{
				assertEquals(3, s.file);
				break;
			}
		}
		for(String fen : new String[]{"rk3r2/8/8/8/4R3/8/8/8 b q -", "r2kr3/8/8/8/8/8/8/8 b q -"})
		{
			game = new Game(Variant.CHESS960).loadFEN(fen).start();
			visualize(game);
			assertTrue(game.blackCanCastleQueenside);
			move = game.move("O-O-O");
			move.commit(true, false);
			visualize(game);
			assertFalse(game.blackCanCastleQueenside);
			assertEquals(CastlingType.QUEENSIDE, move.castlingType);
			assertNull(move.getIllegalReason());
			for(Square s : game.getPieces(Color.BLACK))
			{
				if(s.pieceType == PieceType.ROOK && s.file < 4)
				{
					assertEquals(3, s.file);
					break;
				}
			}
		}
	}

	// Possible Moves

	@Test(timeout = 1000L)
	public void rook() throws ChessException
	{
		System.out.println("Possible Moves: Rook\n");
		final Game game = new Game().loadFEN("8/5p2/8/8/8/8/8/2B2RN1 w").start();
		final ArrayList<Square> squares = new ArrayList<>();
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.ROOK)
			{
				squares.addAll(game.getSquaresControlledBy(s));
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
		for(Square s : game.pieces)
		{
			if(s.pieceColor == Color.WHITE && s.pieceType == PieceType.PAWN)
			{
				squares.addAll(game.getSquaresControlledBy(s));
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
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.KING)
			{
				squares.addAll(game.getSquaresControlledBy(s));
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
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.BISHOP)
			{
				squares.addAll(game.getSquaresControlledBy(s));
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
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.QUEEN)
			{
				squares.addAll(game.getSquaresControlledBy(s));
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
		for(Square s : game.pieces)
		{
			if(s.pieceType == PieceType.KNIGHT)
			{
				squares.addAll(game.getSquaresControlledBy(s));
			}
		}
		evaluatePossibleMoves(22, game, squares);
	}

	@Test(timeout = 1000L)
	public void timeControl() throws ChessException, InterruptedException
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
		game.whitemsecs = -100;
		assertEquals("-00.0100", game.getWhiteTime());
		game.setTimed(0, 100);
		game.start();
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		Thread.sleep(50);
		game.move("e4").commit();
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		game.move("e5").commit();
		assertEquals(EndReason.UNTERMINATED, game.endReason);
		Thread.sleep(200);
		game.move("Nf3").commit();
		assertEquals(EndReason.TIMEOUT, game.endReason);
		assertEquals(GameStatus.BLACK_WINS, game.status);
	}
}

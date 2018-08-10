package sh.hell.compactchess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Move;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ExampleEngineVSEngine
{
	public static void main(String[] args) throws ChessException, IOException, InterruptedException
	{
		final Engine whiteEngine = new Engine("lc0.exe", 4).debug(true);
		final Engine blackEngine = new Engine("stockfish_9.exe", 4).debug(true);
		final NumberFormat formatter = new DecimalFormat("#0.00");
		//Game game = new Game().loadFEN("5Bbk/q7/8/p7/8/K7/5P2/6Q1 w - -").start();
		do
		{
			final Game game = new Game().setSuddenDeath(60000).setTag("White", "Leela Chess Zero 594").setTag("Black", "Stockfish 9").start();
			FileWriter fw = new FileWriter("board.svg", false);
			fw.write(game.toSVG());
			fw.close();
			do
			{
				Engine engine = (game.toMove == Color.WHITE ? whiteEngine : blackEngine);
				engine.evaluate(game).awaitConclusion();
				fw = new FileWriter("info.txt", false);
				fw.write("LCZero 594 VS Stockfish 9\n" + game.getWhiteTime() + "     " + game.getBlackTime() + "\n");
				final Move move = engine.getBestMove();
				if(move == null)
				{
					System.out.println("No further move suggested.");
					fw.write("\nNew game in 3 seconds.\n");
					fw.close();
					break;
				}
				//move.requireLegality();
				System.out.println(game.toMove.name() + " played " + move.toUCI() + "\n");
				fw.write("\n" + game.toMove.name() + "'s evaluation: " + formatter.format((double) engine.score / 100) + "\n");
				if(engine.foundMate())
				{
					fw.write(engine.getMatee().name() + " is getting mated in " + engine.getMateIn() + "!\n");
				}
				move.commit();
				System.out.println(game.toString(true));
				System.out.println(game.getFEN() + "\n");
				if(game.status != GameStatus.ONGOING)
				{
					System.out.println("Game over: " + game.status.name() + " by " + game.endReason.name());
					fw.write("\nNew game in 3 seconds.\n");
					fw.close();
				}
				fw.close();
				fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
			}
			while(game.status == GameStatus.ONGOING);
			System.out.println("\n" + game.toPGN());
			Thread.sleep(3000);
		}
		while(true);
	}
}

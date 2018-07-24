package sh.hell.compactchess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.engine.EngineBuilder;
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
		final Engine engine = new EngineBuilder("stockfish_9.exe", 4).build()
				//.debug(true)
				;
		final NumberFormat formatter = new DecimalFormat("#0.00");
		//Game game = new Game().loadFEN("5Bbk/q7/8/p7/8/K7/5P2/6Q1 w - -").start();
		do
		{
			final Game game = new Game().start();
			FileWriter fw = new FileWriter("board.svg", false);
			fw.write(game.toSVG());
			fw.close();
			do
			{
				engine.evaluateInfinitely(game);
				Thread.sleep(game.toMove == Color.WHITE ? 1000 : 500);
				engine.conclude();
				fw = new FileWriter("info.txt", false);
				fw.write("Stockfish 9 VS Stockfish 9!\nBlack has half the time white has.\n");
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
				fw.write("\nEvaluation for white: " + formatter.format(((double) engine.score * (game.toMove == Color.WHITE ? 1 : -1)) / 100) + "\n");
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
					break;
				}
				fw.close();
				fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
			}
			while(true);
			System.out.println("\n" + game.toPGN());
			Thread.sleep(3000);
			//engine.dispose();
		}
		while(true);
	}
}

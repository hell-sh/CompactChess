package sh.hell.compactchess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.Variant;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ExampleEngineVSEngine
{
	public static void main(String[] args) throws IOException
	{
		final String whiteName = "Stockfish 9";
		final Engine whiteEngine = new Engine("stockfish_9_multivariant.exe", 4).debug(true);
		//final String whiteName = "LCZero 594";
		//final Engine whiteEngine = new Engine("lc0.exe", 4).debug(true);
		final String blackName = "Stockfish 9";
		final Engine blackEngine = new Engine("stockfish_9_multivariant.exe", 4).debug(true);
		//final String blackName = "LCZero 594";
		//final Engine blackEngine = new Engine("lc0.exe", 4).debug(true);
		try
		{
			final NumberFormat formatter = new DecimalFormat("#0.00");
			//do
			//{
			final Game game = new Game(Variant.THREE_CHECK).setTimed(60000, 1000).setPlayerNames(whiteName, blackName).start();
			FileWriter fw = new FileWriter("board.svg", false);
			fw.write(game.toSVG());
			fw.close();
			do
			{
				Engine engine = (game.toMove == Color.WHITE ? whiteEngine : blackEngine);
				engine.evaluate(game).awaitConclusion();
				fw = new FileWriter("info.txt", false);
				fw.write(whiteName + " VS " + blackName + "\n" + game.getWhiteTime() + "  " + game.getBlackTime() + "  +" + (game.increment / 1000) + "s\n");
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
					fw.write("\nNew game in 5 seconds.\n");
					fw.close();
				}
				fw.close();
				try
				{
					fw = new FileWriter("board.svg", false);
					fw.write(game.toSVG());
					fw.close();
				}
				catch(IOException ignored)
				{
				}
			}
			while(game.status == GameStatus.ONGOING);
			System.out.println("\n" + game.toPGN());
			Thread.sleep(5000);
			//}
			//while(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			whiteEngine.dispose();
			blackEngine.dispose();
		}
	}
}

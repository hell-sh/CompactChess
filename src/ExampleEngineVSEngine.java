import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Move;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ExampleEngineVSEngine
{
	public static void main(String[] args) throws IOException, InterruptedException
	{
		System.out.println("Initializing engines...");
		final HashMap<String, String> uciOptions = new HashMap<>();
		uciOptions.put("Threads", "3");
		final String whiteName = "Stockfish 9";
		final Engine whiteEngine = new Engine("stockfish_9_multivariant.exe", null, uciOptions, true);
		//final String whiteName = "LCZero 594";
		//final Engine whiteEngine = new Engine("lc0.exe", null, uciOptions, true);
		//whiteEngine.evaluate(new Game().loadFEN("8/8/8/8/8/8/8/8 w - -").setTimed(1, 0).start()).awaitConclusion();
		final String blackName = "Stockfish 9";
		final Engine blackEngine = new Engine("stockfish_9_multivariant.exe", null, uciOptions, true);
		//final String blackName = "LCZero 594";
		//final Engine blackEngine = new Engine("lc0.exe", null, uciOptions, true);
		//blackEngine.evaluate(new Game().loadFEN("8/8/8/8/8/8/8/8 w - -").setTimed(1, 0).start()).awaitConclusion();
		System.out.println("Engines are ready.");
		try
		{
			//noinspection InfiniteLoopStatement
			do
			{
				final Game game = new Game().setTimed(15000, 0).setPlayerNames(whiteName, blackName).setTag("Event", "Engine VS Engine").start();
				FileWriter fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
				fw = new FileWriter("info.txt", false);
				fw.write(whiteName + " VS " + blackName + "\n" + game.getWhiteTime() + "  " + game.getBlackTime() + "  +" + (game.increment / 1000) + "s\n");
				fw.close();
				do
				{
					Color mover = game.toMove;
					Engine engine = (mover == Color.WHITE ? whiteEngine : blackEngine);
					final Move move;
					move = engine.evaluate(game).awaitConclusion().getBestMove();
					fw = new FileWriter("info.txt", false);
					fw.write(whiteName + " VS " + blackName + "\n");
					if(engine.score == 0 && game.canDrawBeClaimed())
					{
						game.claimDraw();
					}
					else if(move == null)
					{
						game.resign(mover);
					}
					else
					{
						System.out.println(mover.name() + " played " + move.toUCI() + "\n");
						move.commit();
						fw.write(game.getWhiteTime() + "  " + game.getBlackTime() + "  +" + (game.increment / 1000) + "s\n");
						fw.write("\n" + mover.name() + "'s evaluation: " + engine.getEvaluation() + "\n");
					}
					System.out.println(game.toString(true));
					System.out.println(game.getFEN() + "\n");
					System.out.println(game.getWhiteTime() + "  " + game.getBlackTime() + "\n");
					if(game.status != GameStatus.ONGOING)
					{
						System.out.println(game.status.name() + " by " + game.endReason.name());
						fw.write(game.status.name() + " by " + game.endReason.name() + "\n");
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
			}
			while(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		whiteEngine.interrupt();
		blackEngine.interrupt();
	}
}

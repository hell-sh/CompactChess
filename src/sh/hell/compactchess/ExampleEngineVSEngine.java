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
		System.out.println("Initializing engines...");
		final String whiteName = "Stockfish 9";
		final Engine whiteEngine = new Engine("stockfish_9_multivariant.exe", 3).debug(true);
		//final String whiteName = "LCZero 594";
		//final Engine whiteEngine = new Engine("lc0.exe", 3).debug(true);
		//whiteEngine.evaluate(new Game().loadFEN("8/8/8/8/8/8/8/8 w - -").setTimed(1, 0).start()).awaitConclusion();
		//final String whiteName = "CompactChess";
		//final Engine whiteEngine = new BuiltInEngine(3);
		final String blackName = "Stockfish 9";
		final Engine blackEngine = new Engine("stockfish_9_multivariant.exe", 3).debug(true);
		//final String blackName = "LCZero 594";
		//final Engine blackEngine = new Engine("lc0.exe", 3).debug(true);
		//blackEngine.evaluate(new Game().loadFEN("8/8/8/8/8/8/8/8 w - -").setTimed(1, 0).start()).awaitConclusion();
		//final String blackName = "CompactChess";
		//final Engine blackEngine = new BuiltInEngine(3);
		System.out.println("Engines are ready.");
		try
		{
			final NumberFormat formatter = new DecimalFormat("#0.00");
			do
			{
				final Game game = new Game(Variant.CHESS960).setTimed(60000, 1000).setPlayerNames(whiteName, blackName).setTag("Event", "Engine VS Engine").start();
				FileWriter fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
				do
				{
					Color mover = game.toMove;
					Engine engine = (mover == Color.WHITE ? whiteEngine : blackEngine);
					final Move move;
					move = engine.evaluate(game).awaitConclusion().getBestMove();
					fw = new FileWriter("info.txt", false);
					fw.write(whiteName + " VS " + blackName + "\n" + game.getWhiteTime() + "  " + game.getBlackTime() + "  +" + (game.increment / 1000) + "s\n");
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
						fw.write("\n" + mover.name() + "'s evaluation: " + engine.getEvaluation() + "\n");
						move.commit();
						if(game.variant == Variant.STANDARD || game.variant == Variant.CHESS960)
						{
							fw.write("CompactChess eval.: " + formatter.format((double) game.getScore(mover) / 100) + "\n");
						}
					}
					System.out.println(game.toString(true));
					System.out.println(game.getFEN() + "\n");
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
		whiteEngine.dispose();
		blackEngine.dispose();
	}
}

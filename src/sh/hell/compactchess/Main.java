package sh.hell.compactchess;

import sh.hell.compactchess.engine.Engine;
import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.AlgebraicNotationVariation;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.GameStatus;
import sh.hell.compactchess.game.Move;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main
{
	public static void main(String[] args) throws IOException, ChessException, InterruptedException
	{
		System.out.println();
		main();
	}

	private static void main() throws IOException, ChessException, InterruptedException
	{
		System.out.println("# CompactChess");
		System.out.println();
		System.out.println("CompactChess is a library which you can use to do chess-related stuff with.");
		System.out.println("However, this little UI can help as well without any coding required.");
		System.out.println();
		System.out.println("- [C]onvert Notation");
		System.out.println("- [E]ngine Operations");
		System.out.println("- E[x]it");
		System.out.println();
		do
		{
			System.out.print("Your choice: ");
			final char selection = (char) System.in.read();
			while(System.in.available() > 0)
			{
				System.in.read();
			}
			System.out.println();
			switch(selection)
			{
				case 'C':
				case 'c':
					convertNotation();
					return;

				case 'E':
				case 'e':
					engineOperations();
					return;

				case 'X':
				case 'x':
					return;
			}
		}
		while(true);
	}

	private static void convertNotation() throws IOException, ChessException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation");
		System.out.println();
		System.out.println("What do you want to convert form?");
		System.out.println();
		System.out.println("- [F]EN");
		System.out.println("- [P]GN");
		System.out.println("- PGN F[i]le");
		System.out.println("- [C]GN File");
		System.out.println("- E[x]it");
		System.out.println();
		do
		{
			System.out.print("Your choice: ");
			final char selection = (char) System.in.read();
			while(System.in.available() > 0)
			{
				System.in.read();
			}
			System.out.println();
			switch(selection)
			{
				case 'F':
				case 'f':
					convertNotationFromFEN();
					return;

				case 'P':
				case 'p':
					convertNotationFromPGN();
					return;

				case 'i':
				case 'I':
					convertNotationFromPGNFile();
					return;

				case 'c':
				case 'C':
					convertNotationFromCGNFile();
					return;

				case 'X':
				case 'x':
					main();
					return;
			}
		}
		while(true);
	}

	private static void convertNotationFromFEN() throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > From FEN");
		System.out.println();
		System.out.print("FEN: ");
		final String fen = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		convertNotationTo(new Game().loadFEN(fen).start());
	}

	private static void convertNotationFromPGN() throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > From PGN");
		System.out.println();
		System.out.println("Paste your PGN and terminate it with a line containing a minus (-):");
		final StringBuilder pgn = new StringBuilder();
		final Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
		do
		{
			String line = scanner.next();
			if(line.equals("-"))
			{
				break;
			}
			else
			{
				pgn.append(line).append("\n");
			}
		}
		while(true);
		System.out.println();
		convertNotationTo(Game.fromPGN(pgn.toString()));
	}

	private static void convertNotationFromPGNFile() throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > From PGN File");
		System.out.println();
		System.out.print("File: ");
		final String file = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println("Reading content...");
		final InputStream is = new FileInputStream(file);
		final Scanner scanner = new Scanner(is);
		scanner.useDelimiter("\\A");
		String content = scanner.next();
		scanner.close();
		is.close();
		System.out.println("Parsing PGN...");
		ArrayList<Game> games = Game.fromPGN(content);
		System.out.println();
		convertNotationTo(games);
	}

	private static void convertNotationFromCGNFile() throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > From CGN File");
		System.out.println();
		System.out.print("File: ");
		final String file = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		convertNotationTo(Game.fromCGN(new FileInputStream(file)));
	}

	private static void convertNotationTo(final Game game) throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > Game to...");
		System.out.println();
		System.out.println("- [F]EN");
		System.out.println("- [P]GN");
		System.out.println("- FA[N]");
		System.out.println("- [C]GN File");
		System.out.println("- [S]VG");
		System.out.println("- E[x]it");
		System.out.println();
		do
		{
			System.out.print("Your choice: ");
			final char selection = (char) System.in.read();
			while(System.in.available() > 0)
			{
				System.in.read();
			}
			System.out.println();
			switch(selection)
			{
				case 'F':
				case 'f':
					System.out.println(game.getFEN());
					System.out.println();
					convertNotationTo(game);
					return;

				case 'P':
				case 'p':
					System.out.println(game.toPGN());
					System.out.println();

					convertNotationTo(game);
					return;

				case 'N':
				case 'n':
					System.out.println(game.toPGN(AlgebraicNotationVariation.FAN));
					System.out.println();
					convertNotationTo(game);
					return;

				case 'C':
				case 'c':
					convertNotationToCGNFile(game);
					return;

				case 'S':
				case 's':
					System.out.println(game.toSVG());
					System.out.println();
					convertNotationTo(game);
					return;

				case 'X':
				case 'x':
					convertNotation();
					return;
			}
		}
		while(true);
	}

	private static void convertNotationTo(final ArrayList<Game> games) throws ChessException, IOException, InterruptedException
	{
		if(games.size() == 0)
		{
			System.out.println("No game recognized.\n");
			convertNotation();
		}
		else if(games.size() == 1)
		{
			convertNotationTo(games.get(0));
		}
		else
		{
			System.out.println("# Compact Chess > Convert Notation > Games to...");
			System.out.println();
			System.out.println("- [P]GN");
			System.out.println("- [C]GN File");
			System.out.println("- E[x]it");
			System.out.println();
			do
			{
				System.out.print("Your choice: ");
				final char selection = (char) System.in.read();
				while(System.in.available() > 0)
				{
					System.in.read();
				}
				System.out.println();
				switch(selection)
				{

					case 'P':
					case 'p':
						for(Game game : games)
						{
							System.out.println(game.toPGN());
							System.out.println();
						}
						convertNotationTo(games);
						return;

					case 'C':
					case 'c':
						convertNotationToCGNFile(games);
						return;

					case 'X':
					case 'x':
						convertNotation();
						return;
				}
			}
			while(true);
		}
	}

	private static void convertNotationToCGNFile(final Game game) throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > To CGN File");
		System.out.println();
		System.out.print("File: ");
		final String file = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		FileOutputStream os = new FileOutputStream(file);
		game.toCGN(os);
		os.close();
		convertNotationTo(game);
	}

	private static void convertNotationToCGNFile(final ArrayList<Game> games) throws ChessException, IOException, InterruptedException
	{
		System.out.println("# Compact Chess > Convert Notation > To CGN File");
		System.out.println();
		System.out.print("File: ");
		final String file = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		FileOutputStream os = new FileOutputStream(file);
		for(Game game : games)
		{
			game.toCGN(os);
		}
		os.close();
		convertNotationTo(games);
	}

	private static void engineOperations() throws IOException, ChessException, InterruptedException
	{
		System.out.println("# CompactChess > Engine Operations > Configure Engine");
		System.out.println();
		int threads = Runtime.getRuntime().availableProcessors() - 1;
		if(threads < 1)
		{
			threads = 1;
		}
		System.out.println("You have " + Runtime.getRuntime().availableProcessors() + " logical processor(s), so I'll give the engine " + threads + " thread(s).");
		System.out.println();
		System.out.print("Engine Binary: ");
		final String binary = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		final HashMap<String, String> uciOptions = new HashMap<>();
		uciOptions.put("Threads", String.valueOf(threads));
		final Engine engine = new Engine(binary, null, uciOptions, true);
		final Game game = new Game().loadFEN("4k3/8/R3K3/8/8/8/8/8 w - -").start();
		final long timeStart = System.currentTimeMillis();
		final Move move = engine.evaluateDepth(game, 1).awaitConclusion().getBestMove();
		if(move == null)
		{
			System.out.println("Warning: Engine failed to solve a mate in 1.");
		}
		else if(move.isCheckmate())
		{
			System.out.println("Engine solved a mate in 1 in about " + (System.currentTimeMillis() - timeStart) + " ms.");
		}
		else
		{
			System.out.println("Warning: Engine didn't correctly solve a mate in 1.");
			System.out.println("If this is your custom engine, you might want to fix that:");
			System.out.println();
			System.out.println(move.commit().toPGN());
		}
		System.out.println();
		engineOperations(engine);
	}

	private static void engineOperations(final Engine engine) throws IOException, ChessException, InterruptedException
	{
		System.out.println("# CompactChess > Engine Operations");
		System.out.println();
		System.out.println("What do you want to do with this engine?");
		System.out.println();
		System.out.println("- [F]inish Position");
		System.out.println("- [P]lay Against");
		System.out.println("- [D]ispose of Engine");
		System.out.println();
		do
		{
			System.out.print("Your choice: ");
			final char selection = (char) System.in.read();
			while(System.in.available() > 0)
			{
				System.in.read();
			}
			System.out.println();
			switch(selection)
			{
				case 'F':
				case 'f':
					engineOperationsFinishPosition(engine);
					return;

				case 'P':
				case 'p':
					engineOperationsPlayAgainst(engine);
					return;

				case 'D':
				case 'd':
					engine.interrupt();
					main();
					return;
			}
		}
		while(true);
	}

	private static void engineOperationsFinishPosition(final Engine engine) throws ChessException, IOException, InterruptedException
	{
		System.out.println("# CompactChess > Engine Operations > Finish Position");
		System.out.println();
		System.out.print("FEN: ");
		final String fen = new Scanner(System.in).useDelimiter("\\n").next();
		System.out.println();
		System.out.print("Depth: ");
		final int depth = Integer.valueOf(new Scanner(System.in).useDelimiter("\\n").next());
		System.out.println();
		final Game game = new Game().loadFEN(fen).start();
		do
		{
			final Move move = engine.evaluateDepth(game, depth).awaitConclusion().getBestMove();
			if(move == null)
			{
				System.out.println("No further move suggested.");
				break;
			}
			System.out.println(game.toMove.name() + " played " + move.toUCI() + "\n");
			move.commit();
			System.out.println(game.toString(true));
			System.out.println(game.getFEN() + "\n");
			System.out.println(game.toPGN() + "\n");
			System.out.println(game.toPGN(AlgebraicNotationVariation.FAN) + "\n");
			if(game.status != GameStatus.ONGOING)
			{
				System.out.println("Game over: " + game.status.name() + " by " + game.endReason.name());
				break;
			}
		}
		while(true);
		System.out.println();
		engineOperations(engine);
	}

	private static void engineOperationsPlayAgainst(Engine engine) throws ChessException, IOException, InterruptedException
	{
		System.out.println("# CompactChess > Engine Operations > Play Against");
		System.out.println();
		final Game game = new Game().start();
		do
		{
			System.out.println(game.toString(true));
			try
			{
				FileWriter fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
			}
			catch(IOException ignored)
			{
			}
			System.out.print("Your move: ");
			String rawMove = new Scanner(System.in).useDelimiter("\\n").next();
			System.out.println();
			try
			{
				Move move = game.move(rawMove);
				move.commit();
				String illegalReason = move.getIllegalReason();
				if(illegalReason != null)
				{
					System.out.println("That move was illegal: " + illegalReason);
				}
			}
			catch(ChessException e)
			{
				System.out.println(e.getMessage());
				continue;
			}
			System.out.println(game.toString(false));
			try
			{
				FileWriter fw = new FileWriter("board.svg", false);
				fw.write(game.toSVG());
				fw.close();
			}
			catch(IOException ignored)
			{
			}
			if(game.status != GameStatus.ONGOING)
			{
				break;
			}
			engine.evaluate(game).awaitConclusion().getBestMove().commit();
		}
		while(game.status == GameStatus.ONGOING);
		System.out.println();
		System.out.println(game.status + " by " + game.endReason);
		System.out.println();
		engineOperations(engine);
	}
}

package sh.hell.compactchess.engine;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.Color;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.TimeControl;
import sh.hell.compactchess.game.Variant;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Engine extends EngineBuilder
{
	public final ArrayList<Variant> supportedVariants = new ArrayList<>();
	final Object LOCK = new Object();
	final Scanner input;
	private final Process process;
	private final OutputStreamWriter output;
	public boolean isReady = false;
	public boolean isEvaluating = false;
	public String bestMove = null;
	public String ponder = null;
	public short score = 0;
	public Game evaluatingGame = null;
	int mateIn = 0;
	private boolean goingInfinite;

	protected Engine(boolean doDebug)
	{
		super(doDebug);
		this.process = null;
		this.output = null;
		this.input = null;
	}

	public Engine(String binary) throws IOException, InterruptedException
	{
		this(binary, null, null, false);
	}

	public Engine(String binary, List<String> binaryArguments) throws IOException, InterruptedException
	{
		this(binary, binaryArguments, null, false);
	}

	public Engine(String binary, Map<String, String> uciOptions) throws IOException, InterruptedException
	{
		this(binary, null, uciOptions, false);
	}

	public Engine(String binary, List<String> binaryArguments, Map<String, String> uciOptions) throws IOException, InterruptedException
	{
		this(binary, binaryArguments, uciOptions, false);
	}

	public Engine(String binary, List<String> binaryArguments, Map<String, String> uciOptions, boolean debug) throws IOException, InterruptedException
	{
		super(binary, binaryArguments, uciOptions, debug);
		supportedVariants.add(Variant.STANDARD);
		if(binaryArguments == null)
		{
			process = new ProcessBuilder(binary).start();
		}
		else
		{
			List<String> command = new ArrayList<>();
			command.add(binary);
			command.addAll(binaryArguments);
			process = new ProcessBuilder(command).start();
		}
		output = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
		input = new Scanner(new InputStreamReader(process.getInputStream())).useDelimiter("\n");
		this.start();
		if(debug)
		{
			System.out.println("< uci");
		}
		output.write("uci\n");
		if(debug)
		{
			System.out.println("< debug on");
			output.write("debug on\n");
		}
		if(uciOptions != null)
		{
			for(Map.Entry<String, String> option : uciOptions.entrySet())
			{
				if(debug)
				{
					System.out.println("< setoption name " + option.getKey() + " value " + option.getValue());
				}
				output.write("setoption name " + option.getKey() + " value " + option.getValue() + "\n");
			}
		}
		output.flush();
		awaitReady();
	}

	public Engine copy() throws IOException, InterruptedException
	{
		return build();
	}

	public Engine evaluate(Game game) throws IOException, ChessException
	{
		return evaluate(game, "go");
	}

	public Engine evaluateTime(Game game, long ms) throws ChessException, IOException
	{
		return evaluate(game, "go movetime " + ms);
	}

	public Engine evaluateDepth(Game game, int depth) throws ChessException, IOException
	{
		return evaluate(game, "go depth " + depth);
	}

	public Engine evaluateInfinitely(Game game) throws IOException, ChessException
	{
		return evaluate(game, "go infinite");
	}

	private Engine evaluate(Game game, String command) throws IOException, ChessException
	{
		synchronized(LOCK)
		{
			if(isEvaluating)
			{
				throw new ChessException("Can't give busy Engine another task.");
			}
			isEvaluating = true;
			score = 0;
			mateIn = 0;
			bestMove = null;
			ponder = null;
			if(evaluatingGame != game)
			{
				evaluatingGame = game;
				output.write("ucinewgame\n");
			}
			goingInfinite = (command.equals("go infinite"));
			if(debug)
			{
				System.out.println("< setoption name UCI_Variant value " + evaluatingGame.variant.uciName);
			}
			output.write("setoption name UCI_Variant value " + evaluatingGame.variant.uciName + "\n");
			if(evaluatingGame.variant == Variant.CHESS960)
			{
				if(debug)
				{
					System.out.println("< setoption name UCI_Chess960 value true");
				}
				output.write("setoption name UCI_Chess960 value true\n");
			}
			StringBuilder position;
			//if(game.start.toMove == game.toMove)
			//{
			position = new StringBuilder("position fen " + evaluatingGame.start.getFEN());
			if(evaluatingGame.moves.size() > 0)
			{
				position.append(" moves");
				synchronized(evaluatingGame.moves)
				{
					for(Move move : evaluatingGame.moves)
					{
						position.append(" ").append(move.toUCI());
					}
				}
			}
			//}
			//else
			//{
			//	position = new StringBuilder("position fen " + game.getFEN());
			//}
			if(debug)
			{
				System.out.println("< " + position.toString());
			}
			output.write("" + position.append("\n").toString());
			if(evaluatingGame.timeControl != TimeControl.UNLIMITED)
			{
				command += " wtime " + evaluatingGame.whitemsecs + " btime " + evaluatingGame.blackmsecs + " winc " + evaluatingGame.increment + " binc " + evaluatingGame.increment;
			}
			if(debug)
			{
				System.out.println("< " + command);
			}
			output.write(command + "\n");
			output.flush();
			game.resetMoveTime();
		}
		return this;
	}

	public String getEvaluation()
	{
		final NumberFormat formatter = new DecimalFormat("#0.00");
		synchronized(LOCK)
		{
			if(mateIn != 0)
			{
				return "#" + this.mateIn;
			}
			return formatter.format((double) score / 100);
		}
	}

	public Move getBestMove() throws ChessException
	{
		synchronized(LOCK)
		{
			if(bestMove == null)
			{
				return null;
			}
			Move move = evaluatingGame.uciMove(bestMove);
			synchronized(move.annotationTags)
			{
				move.annotationTags.add("[%eval " + getEvaluation().replace(",", ".") + "]");
			}
			return move;
		}
	}

	public Move getPonder() throws ChessException
	{
		synchronized(LOCK)
		{
			if(ponder == null)
			{
				return null;
			}
			return evaluatingGame.uciMove(ponder);
		}
	}

	public Engine awaitReady() throws InterruptedException
	{
		do
		{
			synchronized(LOCK)
			{
				if(isReady)
				{
					return this;
				}
			}
			Thread.sleep(10);
		}
		while(true);
	}

	public Engine conclude() throws IOException, InterruptedException
	{
		output.write("stop\n");
		output.flush();
		synchronized(LOCK)
		{
			goingInfinite = false;
		}
		return this.awaitConclusion();
	}

	public Engine awaitConclusion() throws InterruptedException, IOException
	{
		synchronized(LOCK)
		{
			if(goingInfinite)
			{
				return conclude();
			}
		}
		do
		{
			synchronized(LOCK)
			{
				if(!isEvaluating)
				{
					return this;
				}
			}
			Thread.sleep(10);
		}
		while(true);
	}

	public boolean foundMate()
	{
		synchronized(LOCK)
		{
			return mateIn != 0;
		}
	}

	public int getMateIn()
	{
		synchronized(LOCK)
		{
			return Math.abs(mateIn);
		}
	}

	public Color getMater()
	{
		synchronized(LOCK)
		{
			if(!foundMate())
			{
				return null;
			}
			return mateIn > 0 ? evaluatingGame.toMove : (evaluatingGame.toMove.opposite());
		}
	}

	public Color getMatee()
	{
		synchronized(LOCK)
		{
			if(!foundMate())
			{
				return null;
			}
			return getMater().opposite();
		}
	}

	/**
	 * @deprecated Use {@link Engine#interrupt()} instead.
	 */
	@Deprecated
	public void dispose()
	{
		this.interrupt();
	}

	@Override
	public void interrupt()
	{
		synchronized(LOCK)
		{
			super.interrupt();
			goingInfinite = false;
		}
		try
		{
			awaitConclusion();
		}
		catch(InterruptedException | IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		try
		{
			do
			{
				String line = input.next();
				synchronized(LOCK)
				{
					if(debug)
					{
						System.out.println("> " + line);
					}
				}
				if(line.startsWith("uciok"))
				{
					synchronized(LOCK)
					{
						isReady = true;
					}
				}
				else if(line.startsWith("option name UCI_Chess960 type check"))
				{
					synchronized(supportedVariants)
					{
						supportedVariants.add(Variant.CHESS960);
					}
				}
				else if(line.startsWith("option name UCI_Variant type combo "))
				{
					StringBuilder var = null;
					for(String section : line.split(" "))
					{
						if(section.equals("var"))
						{
							if(var != null)
							{
								synchronized(supportedVariants)
								{
									supportedVariants.add(Variant.fromKey(var.toString()));
								}
							}
							var = new StringBuilder();
						}
						else
						{
							if(var != null)
							{
								if(var.length() != 0)
								{
									var.append(" ");
								}
								var.append(section);
							}
						}
					}
					if(var != null && var.length() > 0)
					{
						synchronized(supportedVariants)
						{
							supportedVariants.add(Variant.fromKey(var.toString()));
						}
					}
				}
				else if(line.startsWith("info "))
				{
					String[] arr = line.split(" ");
					String key = "";
					for(int i = 1; i < arr.length; i++)
					{
						if(key.equals(""))
						{
							if(!arr[i].equals("score"))
							{
								key = arr[i];
							}
						}
						else
						{
							String value = arr[i].trim();
							synchronized(LOCK)
							{
								switch(key)
								{
									case "cp":
										score = Short.parseShort(value);
										break;
									case "mate":
										mateIn = Integer.parseInt(value);
										break;
									default:
								}
							}
							key = "";
						}
					}
				}
				else if(line.startsWith("bestmove "))
				{
					String[] arr = line.split(" ");
					synchronized(LOCK)
					{
						bestMove = arr[1].trim();
						if(bestMove.equals("empty") || bestMove.equals("(none)"))
						{
							bestMove = null;
						}
						if(arr.length > 3)
						{
							ponder = arr[3].trim();
							if(ponder.equals("empty") || ponder.equals("(none)"))
							{
								ponder = null;
							}
						}
						isEvaluating = false;
					}
				}
			}
			while(!Thread.interrupted());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		process.destroy();
	}
}

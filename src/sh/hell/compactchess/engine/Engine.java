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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Engine extends Thread
{
	public final EngineKiller killer;
	private final Object LOCK = new Object();
	private final Object CONCLUDE_LOCK = new Object();
	private final String binary;
	private final List<String> binaryArguments;
	private final byte threads;
	private final int moveOverhead;
	private final boolean doPonder;
	private final EngineTimeouter timeouter;
	private final Thread thread;
	public String bestMove = null;
	public String ponder = null;
	public short score = 0;
	Game evaluatingGame = null;
	private Process process;
	private boolean doDebug = false;
	private int mateIn = 0;
	private EngineTask task = EngineTask.IDLE;
	private boolean goingInfinite;
	private OutputStreamWriter output;
	private Scanner input;

	Engine()
	{
		this.killer = null;
		this.binary = null;
		this.binaryArguments = null;
		this.threads = 0;
		this.moveOverhead = 0;
		this.doPonder = false;
		this.timeouter = null;
		this.thread = null;
	}

	public Engine(String binary, int threads) throws IOException
	{
		this(binary, threads, 30, true);
	}

	public Engine(String binary, List<String> binaryArguments, int threads) throws IOException
	{
		this(binary, binaryArguments, threads, 30, true);
	}

	public Engine(String binary, int threads, int moveOverhead) throws IOException
	{
		this(binary, new ArrayList<String>()
		{
		}, threads, moveOverhead, true);
	}

	public Engine(String binary, List<String> binaryArguments, int threads, int moveOverhead) throws IOException
	{
		this(binary, binaryArguments, threads, moveOverhead, true);
	}

	public Engine(String binary, int threads, int moveOverhead, boolean doPonder) throws IOException
	{
		this(binary, new ArrayList<String>()
		{
		}, threads, moveOverhead, doPonder);
	}

	public Engine(String binary, List<String> binaryArguments, int threads, int moveOverhead, boolean doPonder) throws IOException
	{
		this.binary = binary;
		this.binaryArguments = binaryArguments;
		this.threads = (byte) threads;
		this.moveOverhead = moveOverhead;
		this.doPonder = doPonder;
		this.assumeDead();
		this.timeouter = new EngineTimeouter(this);
		this.killer = new EngineKiller(this);
		this.thread = new Thread(this, "Engine " + binary);
		this.thread.start();
	}

	void assumeDead() throws IOException
	{
		synchronized(LOCK)
		{
			boolean kill = (this.process != null);
			if(kill)
			{
				//System.out.println("# Killing Engine...");
				task = EngineTask.IDLE;
				this.output.close();
				this.output = null;
				this.input = null;
				this.process.destroy();
				this.process = null;
			}
			if(binaryArguments.size() > 0)
			{
				List<String> binaryArguments_ = new ArrayList<>();
				binaryArguments_.add(binary);
				binaryArguments_.addAll(binaryArguments);
				this.process = new ProcessBuilder(binaryArguments_).start();
			}
			else
			{
				this.process = new ProcessBuilder(binary).start();
			}
			this.output = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream()));
			this.input = new Scanner(new InputStreamReader(process.getInputStream())).useDelimiter("\n");
			output.write("uci\n");
			output.write("debug on\n");
			output.write("setoption name Threads value " + threads + "\n");
			output.write("setoption name Move Overhead value " + moveOverhead + "\n");
			output.write("setoption name Ponder value " + doPonder + "\n");
			//output.write("setoption name SlowMover value 30\n");
			//output.write("setoption name Slow Mover value 30\n");
			output.write("isready\n");
			if(kill)
			{
				output.flush();
			}
		}
	}

	public Engine evaluate(Game game) throws IOException, ChessException
	{
		return evaluate(game, "go", 0);
	}

	public Engine evaluate(Game game, long mslimit) throws IOException, ChessException
	{
		return evaluate(game, "go", mslimit);
	}

	public Engine evaluateDepth(Game game, int depth) throws ChessException, IOException
	{
		return evaluate(game, "go depth " + depth, 0);
	}

	public Engine evaluateDepth(Game game, int depth, long mslimit) throws ChessException, IOException
	{
		return evaluate(game, "go depth " + depth, mslimit);
	}

	public Engine evaluateTime(Game game, long ms) throws ChessException, IOException
	{
		return evaluate(game, "go movetime " + ms, ms + 500);
	}

	public Engine evaluateInfinitely(Game game) throws IOException, ChessException
	{
		return evaluate(game, "go infinite", 0);
	}

	private Engine evaluate(Game game, String command, long mslimit) throws IOException, ChessException
	{
		synchronized(LOCK)
		{
			if(task != EngineTask.IDLE)
			{
				throw new ChessException("Engine is still working.");
			}
			this.score = 0;
			this.mateIn = 0;
			this.bestMove = null;
			this.ponder = null;
			this.timeouter.stopEnforcing();
			this.killer.abortMission();
			if(this.evaluatingGame != game)
			{
				this.evaluatingGame = game;
				output.write("ucinewgame\n");
			}
		}
		synchronized(CONCLUDE_LOCK)
		{
			this.goingInfinite = (command.equals("go infinite"));
			if(this.doDebug)
			{
				System.out.println("> setoption name UCI_Variant value " + evaluatingGame.variant.uciName);
			}
			output.write("setoption name UCI_Variant value " + evaluatingGame.variant.uciName + "\n");
			if(evaluatingGame.variant == Variant.CHESS960)
			{
				if(this.doDebug)
				{
					System.out.println("> setoption name UCI_Chess960 value true");
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
			if(this.doDebug)
			{
				System.out.println("> " + position.toString());
			}
			output.write("" + position.append("\n").toString());
			if(evaluatingGame.timeControl != TimeControl.UNLIMITED)
			{
				command += " wtime " + evaluatingGame.whitemsecs + " btime " + evaluatingGame.blackmsecs + " winc " + evaluatingGame.increment + " binc " + evaluatingGame.increment;
			}
			if(this.doDebug)
			{
				System.out.println("> " + command);
			}
			synchronized(LOCK)
			{
				task = EngineTask.START_EVALUATING;
			}
			output.write(command + "\n");
			if(game.timeControl != TimeControl.UNLIMITED)
			{
				game.resetMoveTime();
			}
			output.flush();
		}
		do
		{
			synchronized(LOCK)
			{
				if(task == EngineTask.EVALUATING)
				{
					break;
				}
			}
			try
			{
				Thread.sleep(1);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		while(true);
		if(mslimit > 0)
		{
			timeouter.enforceTimeout(mslimit);
		}
		return this;
	}

	public Engine debug(boolean debug)
	{
		synchronized(LOCK)
		{
			this.doDebug = debug;
		}
		return this;
	}

	public Move getBestMove() throws ChessException
	{
		synchronized(LOCK)
		{
			if(this.bestMove == null)
			{
				return null;
			}
			return evaluatingGame.uciMove(this.bestMove);
		}
	}

	public Move getPonder() throws ChessException
	{
		synchronized(LOCK)
		{
			if(this.ponder == null)
			{
				return null;
			}
			return evaluatingGame.uciMove(this.ponder);
		}
	}

	public Engine conclude() throws IOException, ChessException
	{
		output.write("stop\n");
		output.flush();
		goingInfinite = false;
		return this.awaitConclusion();
	}

	public Engine awaitConclusion() throws ChessException
	{
		if(goingInfinite)
		{
			throw new ChessException("Refusing to await conclusion of infinite evaluation. Use Engine.conclude() instead.");
		}
		do
		{
			synchronized(CONCLUDE_LOCK)
			{
				synchronized(LOCK)
				{
					if(task == EngineTask.IDLE)
					{
						return this;
					}
				}
			}
			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		while(true);
	}

	public void dispose()
	{
		this.thread.interrupt();
		this.timeouter.thread.interrupt();
		this.killer.thread.interrupt();
		this.process.destroy();
	}

	public boolean foundMate()
	{
		synchronized(LOCK)
		{
			return this.mateIn != 0;
		}
	}

	public int getMateIn()
	{
		synchronized(LOCK)
		{
			return Math.abs(this.mateIn);
		}
	}

	public Color getMater()
	{
		synchronized(LOCK)
		{
			if(!this.foundMate())
			{
				return null;
			}
			return this.mateIn > 0 ? evaluatingGame.toMove : (evaluatingGame.toMove == Color.WHITE ? Color.BLACK : Color.WHITE);
		}
	}

	public Color getMatee()
	{
		synchronized(LOCK)
		{
			if(!this.foundMate())
			{
				return null;
			}
			return (this.getMater() == Color.WHITE ? Color.BLACK : Color.WHITE);
		}
	}

	@Override
	public void run()
	{
		try
		{
			do
			{
				synchronized(LOCK)
				{
					if(task == EngineTask.START_EVALUATING)
					{
						task = EngineTask.EVALUATING;
					}
					if(task != EngineTask.EVALUATING)
					{
						continue;
					}
				}
				synchronized(CONCLUDE_LOCK)
				{
					try
					{
						while(task == EngineTask.EVALUATING && !this.thread.isInterrupted() && input != null)
						{
							String line = input.next();
							if(this.doDebug)
							{
								System.out.println("< " + line);
							}
							if(line.startsWith("info "))
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
										//System.out.println(key+"="+value);
										synchronized(LOCK)
										{
											switch(key)
											{
												case "cp":
													this.score = Short.parseShort(value);
													break;
												case "mate":
													this.mateIn = Integer.parseInt(value);
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
									this.bestMove = arr[1].trim();
									if(this.bestMove.equals("empty") || this.bestMove.equals("(none)"))
									{
										this.bestMove = null;
									}
									if(arr.length > 3)
									{
										this.ponder = arr[3].trim();
										if(this.ponder.equals("empty") || this.ponder.equals("(none)"))
										{
											this.ponder = null;
										}
									}
								}
								break;
							}
						}
					}
					catch(NoSuchElementException ignored)
					{

					}
					synchronized(LOCK)
					{
						task = EngineTask.IDLE;
					}
				}
				this.timeouter.stopEnforcing();
				this.killer.abortMission();
				Thread.sleep(10);
			}
			while(!this.thread.isInterrupted());
		}
		catch(InterruptedException ignored)
		{

		}
	}
}

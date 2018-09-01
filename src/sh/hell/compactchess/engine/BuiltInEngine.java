package sh.hell.compactchess.engine;

import sh.hell.compactchess.exceptions.ChessException;
import sh.hell.compactchess.game.Game;
import sh.hell.compactchess.game.Move;
import sh.hell.compactchess.game.TimeControl;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class BuiltInEngine extends Engine
{
	public int defaultDepth;

	public BuiltInEngine(int defaultDepth)
	{
		super();
		this.defaultDepth = defaultDepth;
	}

	@Override
	public BuiltInEngine evaluate(Game game) throws ChessException
	{
		return this.evaluateDepth(game, defaultDepth);
	}

	@Override
	public BuiltInEngine evaluate(Game game, long mslimit) throws ChessException
	{
		return this.evaluate(game);
	}

	@Override
	public BuiltInEngine evaluateDepth(Game game, int depth) throws ChessException
	{
		this.evaluatingGame = game;
		if(game.timeControl != TimeControl.UNLIMITED)
		{
			game.resetMoveTime();
		}
		Move move = game.getBestMove(depth);
		if(move == null)
		{
			this.bestMove = null;
		}
		else
		{
			this.bestMove = move.toUCI();
		}
		this.score = game.getScore(game.toMove);
		return this;
	}

	@Override
	public BuiltInEngine evaluateDepth(Game game, int depth, long mslimit) throws ChessException
	{
		return this.evaluateDepth(game, depth);
	}

	@Override
	public BuiltInEngine evaluateTime(Game game, long time) throws ChessException
	{
		return this.evaluate(game);
	}

	@Override
	public BuiltInEngine evaluateInfinitely(Game game) throws ChessException
	{
		return this.evaluate(game);
	}

	@Override
	public BuiltInEngine conclude()
	{
		return this;
	}

	@Override
	public BuiltInEngine awaitConclusion()
	{
		return this;
	}

	@Override
	public void dispose()
	{
	}
}

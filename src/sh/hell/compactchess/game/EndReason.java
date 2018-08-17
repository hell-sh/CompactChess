package sh.hell.compactchess.game;

public enum EndReason
{
	UNTERMINATED("Unterminated", false),
	STALEMATE("Normal", true),
	CHECKMATE("Normal", false),
	RESIGNATION("Normal", false),
	DRAW_AGREEMENT("Normal", true),
	FIFTY_MOVE_RULE("Normal", true),
	SEVENTY_FIVE_MOVE_RULE("Normal", true),
	THREEFOLD_REPETITION("Normal", true),
	FIVEFOLD_REPETITION("Normal", true),
	INSUFFICIENT_MATERIAL("Normal", true),
	TIMEOUT("Time Forfeit", false),
	ABANDONED("Abandoned", false),
	ADJUDICATION("Adjudication", false),
	DEATH("Death", false),
	EMERGENCY("Emergency", false),
	RULES_INFRACTION("Rules Infraction", false);

	public final String pgnName;
	public final boolean isDraw;

	EndReason(String pgnName, boolean isDraw)
	{
		this.pgnName = pgnName;
		this.isDraw = isDraw;
	}

	/**
	 * @deprecated Use the EndReason.isDraw property instead
	 */
	@Deprecated
	public boolean isDraw()
	{
		return this.isDraw;
	}
}

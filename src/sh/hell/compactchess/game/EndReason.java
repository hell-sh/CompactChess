package sh.hell.compactchess.game;

public enum EndReason
{
	UNTERMINATED("Unterminated"),
	STALEMATE("Normal"),
	CHECKMATE("Normal"),
	RESIGNATION("Normal"),
	DRAW_AGREEMENT("Normal"),
	FIFTY_MOVE_RULE("Normal"),
	INSUFFICIENT_MATERIAL("Normal"),
	TIMEOUT("Time Forfeit"),
	ABANDONED("Abandoned"),
	ADJUDICATION("Adjudication"),
	DEATH("Death"),
	EMERGENCY("Emergency"),
	RULES_INFRACTION("Rules Infraction");

	public final String pgn_name;

	EndReason(String pgn_name)
	{
		this.pgn_name = pgn_name;
	}

	public boolean isDraw()
	{
		return this == EndReason.STALEMATE || this == EndReason.FIFTY_MOVE_RULE || this == EndReason.INSUFFICIENT_MATERIAL;
	}
}

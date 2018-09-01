package sh.hell.compactchess.game;

public enum PGNSevenTagRoster
{
	Event,
	Site,
	Date,
	Round,
	White,
	Black,
	Result;

	public static PGNSevenTagRoster get(String tag)
	{
		for(PGNSevenTagRoster pgnTag : PGNSevenTagRoster.values())
		{
			if(pgnTag.name().equalsIgnoreCase(tag))
			{
				return pgnTag;
			}
		}
		return null;
	}
}

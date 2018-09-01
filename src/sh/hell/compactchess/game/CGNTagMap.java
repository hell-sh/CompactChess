package sh.hell.compactchess.game;

public enum CGNTagMap
{
	_ENDOFTAGS,
	_FROMSTRING,
	Event,
	EventDate,
	EventSponsor,
	Section,
	Stage,
	Board,
	Site,
	Date,
	Time,
	UTCDate,
	UTCTime,
	Round,
	White,
	Black,
	WhiteElo,
	BlackElo,
	WhiteTitle,
	BlackTitle,
	WhiteUSCF,
	BlackUSCF,
	WhiteNA,
	BlackNA,
	WhiteType,
	BlackType,
	Variant,
	TimeControl,
	ECO,
	NIC,
	Opening,
	Variation,
	SubVariation,
	Termination,
	Annotator,
	Mode,
	FEN;

	public final CGNVersion since;

	CGNTagMap()
	{
		this.since = CGNVersion.V2;
	}

	CGNTagMap(CGNVersion since)
	{
		this.since = since;
	}

	public static CGNTagMap fromName(String tag)
	{
		for(CGNTagMap cgnTag : CGNTagMap.values())
		{
			if(!cgnTag.name().startsWith("_") && cgnTag.name().equalsIgnoreCase(tag))
			{
				return cgnTag;
			}
		}
		return _FROMSTRING;
	}

	public static CGNTagMap fromOrdinal(int ordinal)
	{
		for(CGNTagMap cgnTag : CGNTagMap.values())
		{
			if(cgnTag.ordinal() == ordinal)
			{
				return cgnTag;
			}
		}
		return null;
	}
}

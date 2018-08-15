package sh.hell.compactchess.game;

import java.util.ArrayList;

public enum Variant
{
	STANDARD("Standard", "chess", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
	ANTICHESS("Antichess", "giveaway", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"),
	THREE_CHECK("Three-check", "3check", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
	KING_OF_THE_HILL("King of the Hill", "kingofthehill", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
	RACING_KINGS("Racing Kings", "racingkings", "8/8/8/8/8/8/krbnNBRK/qrbnNBRQ w - - 0 1"),
	HORDE("Horde", "horde", "rnbqkbnr/pppppppp/8/1PP2PP1/PPPPPPPP/PPPPPPPP/PPPPPPPP/PPPPPPPP w kq - 0 1"),
	CHESS960("Chess960", "chess960", "random960");
	// TODO: Crazyhouse
	// TODO: Atomic

	public final String name;
	public final String uciName;
	public final String startFEN;

	Variant(String name, String uciName, String startFEN)
	{
		this.name = name;
		this.uciName = uciName;
		this.startFEN = startFEN;
	}

	public static Variant fromName(final String name)
	{
		for(Variant variant : Variant.values())
		{
			if(variant.name.equalsIgnoreCase(name))
			{
				return variant;
			}
		}
		if(name.equalsIgnoreCase("Fischerandom"))
		{
			return Variant.CHESS960;
		}
		return null;
	}

	public static Variant fromKey(final String key)
	{
		switch(key.toLowerCase())
		{
			case "chess":
			case "standard":
			case "fromposition":
				return Variant.STANDARD;
			case "antichess":
				return Variant.ANTICHESS;
			case "3check":
			case "threecheck":
				return Variant.THREE_CHECK;
			case "kingofthehill":
				return Variant.KING_OF_THE_HILL;
			case "racingkings":
				return Variant.RACING_KINGS;
			case "horde":
				return Variant.HORDE;
		}
		return null;
	}

	public ArrayList<PieceType> getPossiblePromotions()
	{
		final ArrayList<PieceType> promotions = new ArrayList<>();
		promotions.add(PieceType.KNIGHT);
		promotions.add(PieceType.BISHOP);
		promotions.add(PieceType.ROOK);
		promotions.add(PieceType.QUEEN);
		if(this == Variant.ANTICHESS)
		{
			promotions.add(PieceType.KING);
		}
		return promotions;
	}
}

package sh.hell.compactchess.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EngineBuilder
{
	public String binary;
	public List<String> binaryArguments;
	public byte threads;
	public int moveOverhead;
	public boolean doPonder;

	public EngineBuilder(String binary, int threads)
	{
		this(binary, threads, 30, true);
	}

	public EngineBuilder(String binary, List<String> binaryArguments, int threads)
	{
		this(binary, binaryArguments, threads, 30, true);
	}

	public EngineBuilder(String binary, int threads, int moveOverhead)
	{
		this(binary, new ArrayList<String>()
		{
		}, threads, moveOverhead, true);
	}

	public EngineBuilder(String binary, List<String> binaryArguments, int threads, int moveOverhead)
	{
		this(binary, binaryArguments, threads, moveOverhead, true);
	}

	public EngineBuilder(String binary, int threads, int moveOverhead, boolean doPonder)
	{
		this(binary, new ArrayList<String>()
		{
		}, threads, moveOverhead, doPonder);
	}

	public EngineBuilder(String binary, List<String> binaryArguments, int threads, int moveOverhead, boolean doPonder)
	{
		this.binary = binary;
		this.binaryArguments = binaryArguments;
		this.threads = (byte) threads;
		this.moveOverhead = moveOverhead;
		this.doPonder = doPonder;
	}

	public EngineBuilder setMoveOverhead(int moveOverhead)
	{
		this.moveOverhead = moveOverhead;
		return this;
	}

	public EngineBuilder ponder(boolean doPonder)
	{
		this.doPonder = doPonder;
		return this;
	}

	public Engine build() throws IOException
	{
		return new Engine(binary, binaryArguments, threads, moveOverhead, doPonder);
	}
}

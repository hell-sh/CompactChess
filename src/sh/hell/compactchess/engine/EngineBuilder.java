package sh.hell.compactchess.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class EngineBuilder extends Thread
{
	final String binary;
	final List<String> binaryArguments;
	final byte threads;
	final int moveOverhead;
	final boolean doPonder;

	EngineBuilder()
	{
		this.binary = null;
		this.binaryArguments = null;
		this.threads = 0;
		this.moveOverhead = 0;
		this.doPonder = false;
	}

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

	public Engine build() throws IOException
	{
		return new Engine(binary, binaryArguments, threads, moveOverhead, doPonder);
	}
}

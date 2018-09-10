package sh.hell.compactchess.engine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class EngineBuilder extends Thread
{
	public final String binary;
	public final List<String> binaryArguments;
	public final Map<String, String> uciOptions;
	public final boolean debug;

	protected EngineBuilder(boolean debug)
	{
		this.binary = null;
		this.binaryArguments = null;
		this.uciOptions = null;
		this.debug = debug;
	}

	public EngineBuilder(String binary)
	{
		this(binary, null, null, false);
	}

	public EngineBuilder(String binary, List<String> binaryArguments)
	{
		this(binary, binaryArguments, null, false);
	}

	public EngineBuilder(String binary, Map<String, String> uciOptions)
	{
		this(binary, null, uciOptions, false);
	}

	public EngineBuilder(String binary, List<String> binaryArguments, Map<String, String> uciOptions)
	{
		this(binary, binaryArguments, uciOptions, false);
	}

	public EngineBuilder(String binary, List<String> binaryArguments, Map<String, String> uciOptions, boolean debug)
	{
		this.binary = binary;
		this.binaryArguments = binaryArguments;
		this.uciOptions = uciOptions;
		this.debug = debug;
	}

	public Engine build() throws IOException, InterruptedException
	{
		return new Engine(binary, binaryArguments, uciOptions);
	}
}

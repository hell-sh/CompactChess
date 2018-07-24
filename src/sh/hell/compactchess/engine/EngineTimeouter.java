package sh.hell.compactchess.engine;

public class EngineTimeouter extends Thread
{
	final Thread thread;
	private final Object LOCK = new Object();
	private Engine engine;
	private long expires;

	EngineTimeouter(Engine engine)
	{
		this.engine = engine;
		this.thread = new Thread(this, "Engine Timeouter");
		this.thread.start();
	}

	void enforceTimeout(long mslimit)
	{
		synchronized(LOCK)
		{
			this.expires = System.currentTimeMillis() + mslimit;
		}
	}

	void stopEnforcing()
	{
		synchronized(LOCK)
		{
			this.expires = 0;
		}
	}

	@Override
	public void run()
	{
		try
		{
			do
			{
				Thread.sleep(100);
				synchronized(LOCK)
				{
					if(this.expires > 0 && System.currentTimeMillis() >= this.expires)
					{
						this.expires = 0;
						//System.out.println("# Engine time limit reached — Requesting conclusion.");
						engine.killer.killIn(1500);
						//engine.debug(true);
						engine.conclude();
					}
				}
			}
			while(!Thread.currentThread().isInterrupted());
		}
		catch(InterruptedException ignored)
		{

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}

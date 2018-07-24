package sh.hell.compactchess.engine;

public class EngineKiller extends Thread
{
	final Thread thread;
	private final Object LOCK = new Object();
	private Engine engine;
	private long expires;

	EngineKiller(Engine engine)
	{
		this.engine = engine;
		this.thread = new Thread(this, "Engine Killer");
		this.thread.start();
	}

	public void killIn(long mslimit)
	{
		synchronized(LOCK)
		{
			this.expires = System.currentTimeMillis() + mslimit;
		}
	}

	void abortMission()
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
						engine.assumeDead();
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

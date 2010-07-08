package com.metrocave.embedded;
public class Talker 
{

	private  boolean decoderReady=false;
	
	public synchronized boolean isDecoderReady() 
	{
		return decoderReady;		
	}
	
	public synchronized void JustWait()
	{
		try
		{
			wait();
		}
		catch (InterruptedException e) 
		{
			System.out.println("InterruptedException caught");
		}
	}
	public synchronized void Proceed()
	{
		notifyAll();
	}
	

	public synchronized void setDecoderReady(boolean status)
	{
		decoderReady = status;
	}
}

/*
* Copyright 2001 by New Idea Engineering, All Rights Reserved
* Written by Kevin-Neil Klop
*
* $Id: ExitProcessor.java,v 1.1 2004/03/03 20:00:49 mbennett Exp $
*
* $Log: ExitProcessor.java,v $
* Revision 1.1  2004/03/03 20:00:49  mbennett
* Source tree from niesrv 226, from Feb 2004
*
* Revision 1.1  2001/10/31 22:07:42  kevin
* Initial revision
*
* revision 1.4 2001/10/02 mbennett
* Grrr... made the delays into member constants
*
* Revision 1.3  2001/09/03 15:32:15  kevin
* Finally debugged this fershlugginer thing.
*
* Revision 1.2  2001/09/03 00:23:18  kevin
* Moved the scanner to an inner class of the ExitProcessor so that noone would instantiate one accidentally.
*
* Revision 1.1  2001/09/03 00:11:28  kevin
* Initial revision
*
*
*/

//package nie.processors;
package nie.pump.processors;

import org.jdom.*;
import java.util.Hashtable;
import java.util.Enumeration;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

public class ExitProcessor extends Processor
{
	public String kClassName() { return "ExitProcessor"; }


	////////////
	//
	// This is the class that actually does the scanning.
	//
	////////////

	class ExitScanner implements Runnable
	{
		public ExitScanner( Queue inOutputQueue, DPump inDPump )
		{
			fDPump = inDPump;
			fQueue = inOutputQueue;
		}

		///////
		//
		// Check a list of StatusReporters to see if they are
		// all ready to exit.
		//
		///////

		private boolean checkReporters( Enumeration inEnumerator )
		{
			while( inEnumerator.hasMoreElements() )
			{
				StatusReporter lStatusReporter = (StatusReporter)inEnumerator.nextElement();
				if( !lStatusReporter.canExit() )
				{
					return true;
				}
			}
			return false;
		}

		///////
		//
		// Main run loop
		//
		///////

		public void run()
		{
			final String kFName = "run";
			boolean shouldNotExit = true;

			Object lObject = new Object();
			synchronized( lObject )
			{
				try
				{
					shouldNotExit = true;
					while( shouldNotExit )
					{
						//lObject.wait(10000);
						lObject.wait(WAIT_TIME);

						shouldNotExit = checkReporters( fDPump.getQueues().elements() );
						if( !shouldNotExit )
							shouldNotExit = checkReporters( fDPump.getProcessors().elements() );
					}
				}
				catch( InterruptedException ie )
				{
					// System.err.println( "Exit Processor was interrupted.  This is not normally a good thing." );
					// mWorkUnit.errorMsg( this, kFName, "Exit Processor was interrupted.  This is not normally a good thing." );
					// System.err.println( ie );
					// mWorkUnit.errorMsg( this, kFName, ie );
					// System.err.println( ie.getMessage() );
					// mWorkUnit.errorMsg( this, kFName, ie.getMessage() );
					// ie.printStackTrace( System.err );
				}
			}

			if( fQueue != null )
			{
				try
				{
					WorkUnit lWorkUnit = new WorkUnit();
					enqueue( fQueue, lWorkUnit );
					lWorkUnit = null;
				}
				catch( Exception e )
				{
					// System.out.println( "Warning - Could not create work unit for exit processor.  Shutting down." );
					stackTrace( kFName, e, "Could not create work unit for exit processor.  Shutting down." );
					fatalErrorMsg( kFName, "Forcing shutdowhn." );
					fDPump.shutdown();
				}
			}
			else
			{
				fDPump.shutdown();
			}

		}

		DPump fDPump;
		Queue fQueue;
	};


	////////////
	//
	// Constructor for the ExitProcessor.  Standard DPump Processor stuff.
	//
	////////////

	public ExitProcessor(Application inApplication,
			 Queue[] inReadQueueList,
			 Queue[] inWriteQueueList,
			 Queue[] inUsesQueueList,
			 Element inParameter,
			 String inID )
	{
		super( inApplication, inReadQueueList, inWriteQueueList, inUsesQueueList, inParameter, inID );

		if( (inWriteQueueList != null) && (inWriteQueueList.length >= 1) )
			fWriteQueue = inWriteQueueList[0];
		if( (inReadQueueList != null) && (inReadQueueList.length >= 1) )
			fReadQueue = inReadQueueList[0];
		fApplication = (DPump)inApplication;
	}

	////////////
	//
	// Although we could have done this in the constructor, that would have been a bad idea.
	// Since all Processors are instantiated and then their RUN method called, starting up
	// the scanner task in the constructor would have led to the scanner thinking the world
	// was done since all conditions would have been met.
	//
	////////////

	public void run()
	{
		if( fReadQueue == null )
		{
			Thread lScanningThread = null;
			ExitScanner lScanner = new ExitScanner( fWriteQueue, fApplication);
			lScanningThread = new Thread( lScanner );
			lScanningThread.setPriority( Thread.MIN_PRIORITY );
			lScanningThread.start();
		}
		else
		{
			try
			{
				WorkUnit lWorkUnit = dequeue( fReadQueue );
				fApplication.shutdown();
			}
			catch( InterruptedException ie )
			{
			}
		}
	}

	/////
	//
	// And, of course, the Exit Processor is always ready to exit.
	//
	/////

	public boolean canExit()
	{
		return true;
	}

	Queue fWriteQueue = null;
	Queue fReadQueue = null;
	DPump fApplication = null;

	// The delay time for the wait loop
	//private static final int WAIT_TIME = 10000;
	private static final int WAIT_TIME = 1000;
	// This proc doesn't output work units, so nothing to tag with errors
	WorkUnit _mWorkUnit;
}

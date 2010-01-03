//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

import org.jdom.Element;

public class TestTriggers extends Processor
{
	public String kClassName() { return "TestTriggers"; }


	public TestTriggers( Application inApplication,
		 Queue[] inReadFromQueue,
		 Queue[] inWriteToQueue,
		 Queue[] inUsesQueue,
		 Element inParameters,
		 String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		final String kFName = "constructor";
		// System.err.println( "Initializing fTriggerQueue..." );
		infoMsg( kFName, "Initializing fTriggerQueue..." );
		// System.err.println( "inUsesQueue = " + inUsesQueue[0].getName() );
		infoMsg( kFName, "inUsesQueue = " + inUsesQueue[0].getName() );

		fTriggerQueue = inUsesQueue[0];

	}


	public void run()
	{
		final String kFName = "run";
		///////
		//
		// Subscribe to the triggers
		//
		///////

		try
		{
			WorkUnit lWorkUnit = new WorkUnit();
			lWorkUnit.addNamedField( TriggerQueue.SUBSCRIBE_TRIGGERS_TAG, "Trigger 1" );
			// System.out.println( "Enqueueing:" );
			statusMsg( kFName, "Enqueueing:" );
			// System.out.println( lWorkUnit.toString() );
			statusMsg( kFName, lWorkUnit.toString() );
			fTriggerQueue.enqueue( lWorkUnit );
		}
		catch( Exception e )
		{
			// System.err.println( "Could not subscribe to my triggers because I could not create a work unit." );
		    stackTrace( kFName, e, "exception during subscribe to triggers" );
		    fatalErrorMsg( kFName, "Shutting down because of exception: " + e );
			System.exit( -1 );
		}

		///////
		//
		// Send some triggers...
		//
		///////

		try
		{
			WorkUnit lWorkUnit = new WorkUnit();
			lWorkUnit.addNamedField( TriggerQueue.TRIGGER_TAG, "Trigger 2" );
			fTriggerQueue.enqueue( lWorkUnit );
			lWorkUnit = new WorkUnit();
			lWorkUnit.addNamedField( TriggerQueue.TRIGGER_TAG, "Trigger 1" );
			fTriggerQueue.enqueue( lWorkUnit );
		}
		catch( Exception wue )
		{
			// System.err.println( "Error causing some triggers - could not create work unit." );
			stackTrace( kFName, wue, "Error causing some triggers - could not create work unit." );
		}

		///////
		//
		// Dequeue any triggers
		//
		///////

		try
		{
			// System.err.println( "Entering dequeue loop..." );
			debugMsg( kFName, "Entering dequeue loop..." );

			while( true )
			{
				try
				{
					WorkUnit lWorkUnit = (WorkUnit)fTriggerQueue.dequeue();

					// System.err.println( "\n\n\nTestTriggers: Successfully dequeued a work unit." );
					debugMsg( kFName, "Successfully dequeued a work unit." );
					// System.err.println( "TestTriggers: Here's the work unit:" );
					debugMsg( kFName, "Here's the work unit:" );
					// System.err.println( lWorkUnit.toString() );
					debugMsg( kFName, lWorkUnit.toString() );
					// System.err.println( "\n\n\n" );
				}
				catch( InterruptedException ie )
				{
					return;
				}
			}
		}
		catch( Exception wue )
		{
			stackTrace( kFName, wue, null );
			fatalErrorMsg( kFName, "Exiting because of exception: " + wue );
			System.exit( -1 );
		}
	}

	private Queue fTriggerQueue = null;
	WorkUnit _mWorkUnit;
}


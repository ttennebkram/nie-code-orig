/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop and Mark L. Bennett
 *
 * Simple processor that passes items from input queue to output
 * queue or error queue.
 *
 * This is more of an assist for the XPump compiler than anything else.
 *
 */

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;

//import java.net.*;
//import java.io.*;

import org.jdom.Element;
//import org.jdom.Attribute;

/////////////////////////////////////////////////////
//
// This takes up to two output queues...
//
//	Queue 1: Content Queue - feeds out those URLs that were successfully
//		 retrieved, along with their content.
//	Queue 2: Error queue (optional) - feeds out those URLs that caused
//		 an error during retrieve for any reason.
// Queue 3: optional trigger queue
//		if set, hold all work until we get a signal on that queue
//
////////////////////////////////////////////////////

public class SimpleRedirector2 extends Processor
{
	public String kClassName() { return "SimpleRedirector2"; }


	// static final boolean debug = false;

	public SimpleRedirector2( Application inApplication,
			  Queue[] inReadFromQueue,
			  Queue[] inWriteToQueue,
			  Queue[] inUsesQueue,
			  Element inParameters,
			  String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		final String kFName = "constructor";

		if( (inReadFromQueue != null)
			&& (inReadFromQueue.length == 1 /*> 0*/ )
			&& (inReadFromQueue[0] != null) )
		{
		    fReadQueue = inReadFromQueue[0];
		}
		else {
			// System.err.println( "SimpleRedirector: passed an invalid queue set." );
			fatalErrorMsg( kFName, "passed an invalid queue set." );
			System.exit( -1 );
		}

		if( inWriteToQueue != null )
		{
			if( (inWriteToQueue.length > 0)
					&& (inWriteToQueue[0] != null) )
			{
				fContentQueue = inWriteToQueue[0];
			}
			else {
				badQueueList();
			}
				
			if( inWriteToQueue.length == 2
				&& (inWriteToQueue[1] != null) )
			{
				fErrorRetrievingQueue = inWriteToQueue[1];
			}

			if( inWriteToQueue.length > 2 ) {
			    badQueueList();
			}
		}
		else {
			badQueueList();
		}

		if( inUsesQueue != null ) {
		    if( inUsesQueue.length == 1
				&& (inUsesQueue[0] != null) )
			{
				fTriggerQueue = inUsesQueue[0];
			}
			else {
				badQueueList();
			}
		}

	}

	private void badQueueList()
	{
		final String kFName = "badQueueList";
		// System.err.println( "Bad output queue list given to SimpleRedirector." );
		fatalErrorMsg( kFName, "Bad output queue list given to SimpleRedirector." );
		//System.err.println( "The output queue list must have at least one item," );
		fatalErrorMsg( kFName, "The output queue list must have at least one item," );
		// System.err.println( "    queue to which to put retrieved URLs." );
		fatalErrorMsg( kFName, "    queue to which to put retrieved URLs." );
		// System.err.println( "    A second queue may be specified to which we will" );
		fatalErrorMsg( kFName, "    A second queue may be specified to which we will" );
		// System.err.println( "    queue URLs that caused errors." );
		fatalErrorMsg( kFName, "    queue URLs that caused errors." );
		System.exit( -1 );
	}

	/////////////////////////////////////////////
	//
	// And this is where the work gets done.
	//
	/////////////////////////////////////////////

	public void _runV0()
	{
		final String kFName = "run";
		// Main Loop
		while( true )
		{
			// WorkUnit lWorkUnit = null;
			WorkUnit mWorkUnit = null;
			boolean isValid = false;
			String _problemMsg = null;

			// Try to dqueue and process a work unit
			try
			{
				// Get a work unit
			    mWorkUnit = (WorkUnit)dequeue( fReadQueue );

				isValid = mWorkUnit.getIsValidRecord();

			}   // End of try to dqueue and process a work unit
			// Normal interruption
			catch( InterruptedException ie )
			{
				return;
			}
			catch( Exception lException )
			{
				// System.err.println( "SimpleRedirector: got exception: " +
				//	lException
				//	);
				isValid = false;
				// problemMsg = "Got exception: " + lException;
				if( mWorkUnit != null ) {
				    mWorkUnit.setIsValidRecord( false );
					mWorkUnit.stackTrace( this, kFName, lException, "Generic exception");
				}
				else {
					stackTrace( kFName, lException, "Generic exception");
				}
			}


			// Re-Queue the work unit, if we have a work unit
			if( mWorkUnit != null )
			{
				// Add it to the correct queue
				// If the work unit was alright, or there's nowhere
				// else to send it
				if( isValid || fErrorRetrievingQueue == null )
				{
					mWorkUnit.debugMsg( this, kFName, "Sending to regular output" );
					enqueue( fContentQueue, mWorkUnit );
					mWorkUnit = null;
				}
				// Else we did have a problem and we do have an error queue
				else
				{
					mWorkUnit.debugMsg( this, kFName, "Sending to error output");
					enqueue( fErrorRetrievingQueue, mWorkUnit );
					mWorkUnit = null;
				}
			}   // End if work unit not null

		}   // End of Main Loop
	}

	public void run()
	{
		final String kFName = "run";
		//boolean debug = false;
		boolean trace = shouldDoTraceMsg( kFName );

		try
		{
			// if(debug) System.err.println( "Debug:MasterLookup2:run: Start." );
			// if(debug) System.err.println( "Debug:ML:run: Phase 1: read master values." );
			debugMsg( kFName, "Phase 1: wait for trigger." );

			boolean gotTrigger = false;
			while( ! gotTrigger )
			{
				// System.err.println( "\tML:Ph1: top of loop." );
				if(trace) traceMsg( kFName, "top of loop." );
				
				if( ! fTriggerQueue.isEmpty() )
				{
					if(trace) traceMsg( kFName,
						"appears to have a trigger waiting."
						);
					WorkUnit lTriggerWU = dequeue( fTriggerQueue );
					if( lTriggerWU != null )
					{
						gotTrigger = true;
						if(trace) traceMsg( kFName,
							"got a non-null trigger record."
							// + " # of triggers seen: " + fTriggerCount
							);
						lTriggerWU = null;
					}
					else
						errorMsg( kFName, "got a null trigger record"
								+ " but queue reported that there WAS a trigger waiting."
								+ " Will keep trying."
								);
				}
				else
					if(trace) traceMsg( kFName,
						"appears to NOT have a trigger waiting."
						);

				if(trace) traceMsg( kFName,
					"gotTrigger=" + gotTrigger
					);

			}   // Botton of phase 1 while loop

			//debug = true;

			debugMsg( kFName,
				"Phase 2: allow regular to work to flow through"
				);

			
			// Main Loop
			while( true )
			{
				// WorkUnit lWorkUnit = null;
				WorkUnit mWorkUnit = null;
				boolean isValid = false;
				String _problemMsg = null;

				// Try to dqueue and process a work unit
				try
				{
					// Get a work unit
				    mWorkUnit = (WorkUnit)dequeue( fReadQueue );

					isValid = mWorkUnit.getIsValidRecord();

				}   // End of try to dqueue and process a work unit
				// Normal interruption
				catch( InterruptedException ie )
				{
					return;
				}
				catch( Exception lException )
				{
					// System.err.println( "SimpleRedirector: got exception: " +
					//	lException
					//	);
					isValid = false;
					// problemMsg = "Got exception: " + lException;
					if( mWorkUnit != null ) {
					    mWorkUnit.setIsValidRecord( false );
						mWorkUnit.stackTrace( this, kFName, lException, "Generic exception");
					}
					else {
						stackTrace( kFName, lException, "Generic exception");
					}
				}


				// Re-Queue the work unit, if we have a work unit
				if( mWorkUnit != null )
				{
					// Add it to the correct queue
					// If the work unit was alright, or there's nowhere
					// else to send it
					if( isValid || fErrorRetrievingQueue == null )
					{
						mWorkUnit.debugMsg( this, kFName, "Sending to regular output" );
						enqueue( fContentQueue, mWorkUnit );
						mWorkUnit = null;
					}
					// Else we did have a problem and we do have an error queue
					else
					{
						mWorkUnit.debugMsg( this, kFName, "Sending to error output");
						enqueue( fErrorRetrievingQueue, mWorkUnit );
						mWorkUnit = null;
					}
				}   // End if work unit not null

			}   // End of Main Loop

		}
		catch( InterruptedException ie )
		{
		    // This is normal
		}
	}

	// The main queues
	Queue fErrorRetrievingQueue;
	Queue fContentQueue;
	Queue fReadQueue;
	Queue fTriggerQueue;
	WorkUnit mWorkUnit;
}

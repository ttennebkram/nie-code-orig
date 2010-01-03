/*
 *  Copyright 2001 New Idea Engineering
 *  Written by Kevin-Neil Klop
 *
 *  $Id: Replicator.java,v 1.5 2001/08/31 16:29:50 kevin Exp kevin $
 *
 *  $Log: Replicator.java,v $
 *  Revision 1.5  2001/08/31 16:29:50  kevin
 *  Implemented new WorkUnits stuff into these processors.
 *
 *  Revision 1.4  2001/08/24 17:11:53  kevin
 *  Added new constructor parameter - String inID
 *
 *  Revision 1.3  2001/08/21 03:56:59  kevin
 *  re-indented to match current NIE, Inc. style giude.
 *  No code changes.
 *
 *  Revision 1.2  2001/08/15 20:17:57  kevin
 *  Minor changes in preparation for the Major language/specification.
 *
 *  Revision 1.1  2001/08/07 01:16:21  kevin
 *  Initial revision
 *
 */

package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

import org.jdom.Element;

public class Replicator extends Processor
{
	public String kClassName() { return "Replicator"; }


	public Replicator( Application inApplication,
			   Queue[] inReadFromQueue,
			   Queue[] inWriteToQueue,
			   Queue[] inUsesQueue,
			   Element inParameters,
			   String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		final String kFName = "constructor";

		///////
		//
		// Catch any obvious errors in the parameters sent to us.
		//
		///////

		if( (inReadFromQueue == null) ||
			(inReadFromQueue.length == 0) ||
			(inReadFromQueue[0] == null ) )
		{
			// System.err.println( "Invalid input queue specified for RegExURLFilter." );
			fatalErrorMsg( kFName, "Invalid input queue specified for RegExURLFilter." );
			System.exit( -1 );
		}

		fReadQueue = inReadFromQueue[0];
	}


	public void run()
	{
		final String kFName = "run";
		Queue[] lWriteQueues = this.getWriteQueues();

		while( true )
		{
			try
			{
				WorkUnit lObject = fReadQueue.dequeue();

				if( lWriteQueues != null )
				{
					for( int i = 0; i < lWriteQueues.length; i++ )
					{
						WorkUnit lNewWorkUnit = lObject.Clone();
						lWriteQueues[i].enqueue( lNewWorkUnit );
					}
				}
			}
			catch( InterruptedException ie )
			{
				return;
			}
			catch( CloneNotSupportedException cnse )
			{
				stackTrace( kFName, cnse, "Could not clone work unit" );
			}
		}
	}

	private Queue fReadQueue = null;
	WorkUnit _mWorkUnit;
}


/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: Sink.java,v 1.1 2004/03/03 20:00:50 mbennett Exp $
 *
 *  $Log: Sink.java,v $
 *  Revision 1.1  2004/03/03 20:00:50  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.5  2001/08/24 17:11:53  kevin
 *  Added new constructor parameter - String inID
 *
 *  Revision 1.4  2001/08/21 03:56:59  kevin
 *  re-indented to match current NIE, Inc. style giude.
 *  No code changes.
 *
 *  Revision 1.3  2001/08/15 20:17:57  kevin
 *  Minor changes in preparation for the Major language/specification.
 *
 *  Revision 1.2  2001/08/07 01:16:21  kevin
 *  Slimmed down the system, added a d few new processors.
 *  Alse made the processors a bit more generic since they were very spider-centric.
 *
 *  Revision 1.1  2001/07/31 21:46:35  kevin
 *  Initial revision
 *
 *
 */

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

import org.jdom.Element;

public class Sink extends Processor
{
	public String kClassName() { return "Sink"; }


	public Sink( Application inApplication,
		 Queue[] inReadFromQueue,
		 Queue[] inWriteToQueue,
		 Queue[] inUsesQueue,
		 Element inParameters,
		 String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		final String kFName = "constructor";
		if( null==inReadFromQueue || inReadFromQueue.length != 1 ) {
		    fatalErrorMsg( kFName, "Must have exactly one input queue." );
		    System.exit(1);
		}
		if( null != inWriteToQueue && inWriteToQueue.length != 0 ) {
		    fatalErrorMsg( kFName, "Must not have any output queue(s)." );
		    System.exit(1);
		}
		if( null != inUsesQueue && inUsesQueue.length != 0 ) {
		    fatalErrorMsg( kFName, "Must not have any uses queue(s)." );
		    System.exit(1);
		}
		fReadQueue = inReadFromQueue[0];
	}


	public void run()
	{
		while( true )
		{
			try
			{
				Object lObject = dequeue( fReadQueue );
				lObject = null;
			}
			catch( InterruptedException ie )
			{
				return;
			}
		}
	}

	private Queue fReadQueue = null;
}


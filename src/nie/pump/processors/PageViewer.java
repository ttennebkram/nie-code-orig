/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: PageViewer.java,v 1.1 2004/03/03 20:00:49 mbennett Exp $
 *
 *  $Log: PageViewer.java,v $
 *  Revision 1.1  2004/03/03 20:00:49  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
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

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

import org.jdom.Element;

public class PageViewer extends Processor
{
	public String kClassName() { return "PageViewer"; }


	private Queue fReadQueue = null;

	public PageViewer( Application inApplication,
			   Queue[] inReadFromQueue,
			   Queue[] inWriteToQueue,
			   Queue[] inUsesQueue,
			   Element inParameters,
			   String inID )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue, inUsesQueue, inParameters, inID );
		fReadQueue = inReadFromQueue[0];
	}


	public void run()
	{
		final String kFName = "run";
		WorkUnit lWorkUnit = null;
		Element lElement = null;
		String lURL = null;
		String lContent = null;

		while( true )
		{
			WorkUnit record = null;
			try
			{
				lWorkUnit = fReadQueue.dequeue();
				lElement = lWorkUnit.findElement( WorkUnit.USER_DATA_PATH + "/URL" );
				lURL = lElement.getAttribute( "URI").getValue();
				lContent = lElement.getText();

				// System.out.println( "URL:     " + lURL );
				infoMsg( kFName, "URL: " + lURL );
				// System.out.println( "Content: " + lContent );
				infoMsg( kFName, "Content: " + lContent );
			}
			catch( InterruptedException ie )
			{
				return;
			}
		}
	}
	WorkUnit _mWorkUnit;

}

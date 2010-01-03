/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: BasicQueue.java,v 1.1 2004/03/03 20:00:51 mbennett Exp $
 *
 *  $Log: BasicQueue.java,v $
 *  Revision 1.1  2004/03/03 20:00:51  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.1  2001/10/18 23:06:40  kevin
 *  Initial revision
 *
 *  Revision 1.7  2001/09/03 00:11:03  kevin
 *  Created the infrastructure needed for the ExitProcessor.
 *
 *  Revision 1.6  2001/08/31 16:43:39  kevin
 *  Had to add some import statements to get compilations to work cleanly.
 *
 *  Revision 1.5  2001/08/31 14:37:27  kevin
 *  Added WorkUnit processing
 *
 *  Revision 1.4  2001/08/21 03:32:15  kevin
 *  Reindented according to the agreed upon style guide for NIE, Inc.
 *
 *  Revision 1.3  2001/08/15 18:01:20  kevin
 *  First working version of "expanded" DPump specification language
 *
 *  Revision 1.2  2001/08/03 20:50:12  kevin
 *  First working version
 *
 *  Revision 1.1  2001/07/31 21:44:20  kevin
 *  Initial revision
 *
 *
 */


//package nie.core;
package nie.pump.base;


//import nie.core.Queue;
import nie.pump.base.Queue;
import java.util.*;
import org.jdom.*;
//import nie.core.DPump;
import nie.pump.base.DPump;



public class BasicQueue extends Queue

{

	///////////////////////////////////////////////////

	//

	// Constructor for the queue.

	// Note that queues must have a name

	//

	///////////////////////////////////////////////////



	public BasicQueue( String inQueueName, DPump inDPump,
		Element inSpecification
		)
	{
		super( inQueueName, inSpecification );
		common_init( inQueueName, inDPump, inSpecification );
	}



	public BasicQueue( String inQueueName, DPump inDPump )
	{
		super( inQueueName, null );
		common_init( inQueueName, inDPump, null );
	}

	private void common_init( String inQueueName, DPump inDPump, Element inSpecification )
	{
		fItems = new Vector();
		fDPump = inDPump;
	}

	protected boolean getIsMonitoringOn()
	{
		return fDPump.getIsMonintoringOn();
	}



	/////////////////////////////////////////////////////

	//

	// The items necessary to define a new queue handler.

	//

	/////////////////////////////////////////////////////



	public void putNextWorkUnit( WorkUnit inWorkUnit )

	{

		fItems.add( inWorkUnit );

	}



	public WorkUnit getNextWorkUnit()

	{

		if( fItems.size() == 0 )

			return null;



		WorkUnit lReturnedWorkUnit = (WorkUnit)fItems.elementAt(0);

		fItems.remove( lReturnedWorkUnit );

		return lReturnedWorkUnit;

	}



	public String getProcessorName( )
	{
		return fDPump.getProcessorName();
	}

	public Application getApplication()
	{
		return fDPump;
	}

	//Vector fItems = null;
	Vector fItems;
	DPump fDPump;

}






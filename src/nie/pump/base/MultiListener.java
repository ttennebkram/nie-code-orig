//package nie.core;
package nie.pump.base;

//import nie.core.Queue;
//import nie.core.DPump;
import nie.pump.base.Queue;
import nie.pump.base.DPump;

/**
 * Title:        MultiListener
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      New Idea Engineering, Inc.
 * @author Kevin-Neil Klop and Mark Benett
 * @version 1.0
 */

public class MultiListener
{

	static final String MULTI_LISTENER_ID = "_MultiListener";

	public MultiListener( )
	{
		fWaitingObject = new Object();
	}

	public WorkUnit dequeue( Queue[] inQueueList ) throws InterruptedException
	{

		if( null == inQueueList || inQueueList.length == 0 )
			return null;

		if( inQueueList.length == 1 )
			return inQueueList[0].dequeue();

		WorkUnit lWorkUnit;

		try
		{
			while( true )
			{
				for( int i = 0; i < inQueueList.length; i++ )
				{
					lWorkUnit = inQueueList[i].dequeue( fWaitingObject );
					if( lWorkUnit != null )
					{
						if( i > 0 )
							for( int j = i-1; j >= 0; j++ )
								inQueueList[i].abortDequeue( fWaitingObject );
						return lWorkUnit;
					}
				}

				synchronized( fWaitingObject )
				{
					fWaitingObject.wait();
				}

				lWorkUnit = null;

				for( int i = 0; i < inQueueList.length; i++ )
				{
					synchronized( inQueueList[i] )
					{
						inQueueList[i].abortDequeue( fWaitingObject );
						if( !inQueueList[i].isEmpty() && lWorkUnit == null )
						{
							lWorkUnit = inQueueList[i].dequeue();
						}
					}
				}

				if( lWorkUnit != null )
					return lWorkUnit;
			}
		}
		catch( InterruptedException ie )
		{
			for( int i = 0; i < inQueueList.length; i++ )
				inQueueList[i].abortDequeue( fWaitingObject );

			throw ie;
		}
	}

	Object fWaitingObject;
}

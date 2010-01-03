//package nie.processors;
package nie.pump.processors;

import java.util.*;
import org.jdom.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

/**
 * mbennett: 8/12/05 is this even defined as an XPump processor?
 * 		maybe obsolete in favor of ReplicateOnField
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop
 * @version 1.0
 */

public class UserDataSplitter extends Processor
{
	public String kClassName() { return "UserDataSplitter"; }


	public UserDataSplitter( Application inApplication,
						Queue[] inReadQueues,
						Queue[] inWriteQueues,
						Queue[] inUsesQueues,
						Element inParameter,
						String inID )
	{
		super( inApplication, inReadQueues, inWriteQueues, inUsesQueues, inParameter, inID );
		final String kFName = "constructor";
		if( inReadQueues == null  || inReadQueues[0] == null ||
			inWriteQueues == null || inWriteQueues[0] == null )
		{
			// System.err.println( inID + ": Both an input and an output queue needs to be specified to the WUSplitter." );
			fatalErrorMsg( kFName, inID + ": Both an input and an output queue needs to be specified to the WUSplitter." );
			System.exit( -1 );
		}

		fReadQueue = inReadQueues[0];
		fWriteQueue = inWriteQueues[0];

		JDOMHelper lHelper = null;
		try
		{
			lHelper = new JDOMHelper( inParameter );
		}
		catch( Exception le )
		{
			// System.err.println( "Could not parse the parameters for " + inID );
			fatalErrorMsg( kFName, "Could not parse the parameters for " + inID
			        + " Error: " + le
			        );
			// System.err.println( le );
			// errorMsg( kFName, ""+le);
			// System.err.println( le.getMessage() );
			// stackTrace( kFName,le.getMessage (), null);
			// le.printStackTrace( System.err );
			stackTrace( kFName, le, null);
			System.exit( -1 );
		}

		Element lLoopField = lHelper.findElementByPath( LOOP_FIELD );
		if( lLoopField == null )
		{
			// System.err.println( inID + ": You must specify a loop field to use in splitting." );
			fatalErrorMsg( kFName, inID + ": You must specify a loop field to use in splitting." );
			System.exit( -1 );
		}

		fLoopField = lLoopField.getTextTrim();
	}

	public void run()
	{
	    final String kFName = "run";
		try
		{
			while( true )
			{
			    mWorkUnit = null;
				// WorkUnit lWorkUnit = fReadQueue.dequeue();
				mWorkUnit = fReadQueue.dequeue();
				List lLoopValues = JDOMHelper.findElementsByPath( mWorkUnit.getUserData(), fLoopField );
				if( lLoopValues.size() > 0 )
				{
					WorkUnit lTemplateWorkUnit = mWorkUnit.Clone();
					int lNumFieldsDeleted = lTemplateWorkUnit.deleteUserDataFieldMulti( fLoopField );
					for( Iterator lIterator = lLoopValues.iterator(); lIterator.hasNext(); )
					{
						WorkUnit lOutputWorkUnit = lTemplateWorkUnit.Clone();
						Element lNewElement = (Element)((Element)lIterator.next()).clone();
						lOutputWorkUnit.addElement( WorkUnit.USER_DATA_PATH, lNewElement );
						fWriteQueue.enqueue( lOutputWorkUnit );
						lOutputWorkUnit = null;
					}
					mWorkUnit = null;
				}
			}
		}
		catch( CloneNotSupportedException cnse )
		{
			// System.err.println( "In " + this.getID() + " " + cnse );
			// System.err.println( cnse.getMessage() );
			// cnse.printStackTrace( System.err );
			stackTrace( kFName, cnse, "Cloning Exception" );
			fatalErrorMsg( kFName, "Exiting due to exception: " + cnse );
			System.exit( -1 );
		}
		catch( InterruptedException ie )
		{
		}
	}

	Queue fReadQueue;
	Queue fWriteQueue;
	String fLoopField;
	WorkUnit mWorkUnit;

	static final String LOOP_FIELD = "loop_field";
}

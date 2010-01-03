//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;
import java.util.*;
import org.jdom.*;

/**
 * Title:   ReplicateOnFieldV2
 * replaces UserDataSplitter
 * Uses work unit items instead
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @author Kevin-Neil Klop
 * @version 1.0
 */

public class ReplicateOnFieldV2 extends Processor
{
	public String kClassName() { return "ReplicateOnFieldV2"; }


	public ReplicateOnFieldV2( Application inApplication,
						nie.pump.base.Queue[] inReadQueues,
						nie.pump.base.Queue[] inWriteQueues,
						nie.pump.base.Queue[] inUsesQueues,
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
		if( inWriteQueues.length > 1 )
			fErrorQueue = inWriteQueues[1];

		JDOMHelper lHelper = null;
		try
		{
			fJdh = new JDOMHelper( inParameter );
		}
		catch( Exception le )
		{
			// System.err.println( "Could not parse the parameters for " + inID );
			// fatalErrorMsg( kFName, "Could not parse the parameters for " + inID );
			// System.err.println( le );
			stackTrace( kFName, le, "Processing input parameters" );
			// System.err.println( le.getMessage() );
			// fatalErrorMsg( kFName, le.getMessage() );
			// le.printStackTrace( System.err );
			// stackTrace( kFName, le, null);
			fatalErrorMsg( kFName, "Exiting due to exception: " + le );
			System.exit( -1 );
		}

		fHaveDoneInit = false;
		getLoopFieldName();
		fHaveDoneInit = true;

	}

	public void run()
	{
		final String kFName = "run";
		try
		{
			while( true )
			{
				WorkUnit lWorkUnit = dequeue( fReadQueue );
				doLoop( lWorkUnit );
				lWorkUnit = null;

				// Note that doLoop en-queues the result.
				// It has to, it creates many work units from
				// this one.

			}
		}
		catch( CloneNotSupportedException cnse )
		{
			stackTrace( kFName, cnse, "Got Exception cloning" );
			fatalErrorMsg( kFName, "Exiting due to cloning exception: " + cnse );
			System.exit( -1 );
		}
		catch( InterruptedException ie )
		{
		}
	}

	void doLoop( WorkUnit inWU )
		throws CloneNotSupportedException
	{
		
		final String kFName = "doLoop";
		List lLoopValues = inWU.getUserFields( getLoopFieldName() );

		if( lLoopValues.size() <= 0 )
		{
			// System.err.println( "Warning: ReplicateOnField: doLoop:" +
			//	" no matching fields for '" + getLoopFieldName() + "'" +
			//	" and therefore nothing to enqueue"
			//	);
			String msg = "no matching fields for '" + getLoopFieldName() + "'" +
				" and therefore nothing to enqueue"
				;
			// If there's an error queue, use it!
			if( fErrorQueue != null )
			{
				// System.err.println( "\tMarking invalid and sending to error queue" );
				inWU.setIsValidRecord( false );
				inWU.errorMsg( this, kFName, msg );
				enqueue( fErrorQueue, inWU );
			}
			else
				// System.err.println( kFName + msg + "\n\tdropping work unit because no error queue set" );
				errorMsg( kFName, msg + " - DROPPING work unit because no error queue set" );
			return;
		}

		// Create a template
		WorkUnit lTemplateWorkUnit = inWU.Clone();
		// Delete the fields from the template
		int lNumFieldsDeleted = lTemplateWorkUnit.deleteUserDataFieldMulti(
			fLoopField
			);

		// For each of the old children (from the original work unit)
		for( Iterator it = lLoopValues.iterator(); it.hasNext(); )
		{
			// Get the child element from the list
			Element tmpElement = (Element)it.next();
			// Clone it, just to be safe
			Element lNewElement = (Element)tmpElement.clone();

			// Get a copy of the template work unit
			WorkUnit lOutputWorkUnit = lTemplateWorkUnit.Clone();
			// Add the new child
			lOutputWorkUnit.addUserDataElement( lNewElement );

			// Now add it to the output queue
			enqueue( fWriteQueue, lOutputWorkUnit );

			// Set our refs to null asap, race condition paranoia
			lOutputWorkUnit = null;
			tmpElement = null;
			lNewElement = null;
		}
		lTemplateWorkUnit = null;
		inWU = null;
	}

	String getLoopFieldName()
	{
		final String kFName = "getLoopFieldName";
		
		if( fHaveDoneInit )
			return fLoopField;
		else
		{
			String tmpString = fJdh.getTextByPath(
				LOOP_FIELD
				);
			if( tmpString == null || tmpString.trim().equals("") )
			{
				// System.out.println(
				//	"ReplicateOnFieldV3: Must specify a loop field"
				//	);
				fatalErrorMsg( kFName, "Must specify a loop field" );
				System.exit(1);
			}
			fLoopField = tmpString.trim();
		}
		return fLoopField;
	}


	JDOMHelper fJdh;
	nie.pump.base.Queue fReadQueue;
	nie.pump.base.Queue fWriteQueue;
	nie.pump.base.Queue fErrorQueue;
	String fLoopField;
	boolean fHaveDoneInit;

	static final String LOOP_FIELD = "loop_field";
	// We often don't pass the originating work unit through, so nothing
	// to hang errors on
	WorkUnit _mWorkUnit;
}

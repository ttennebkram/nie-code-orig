//package nie.core;
package nie.pump.base;

import org.jdom.*;
import java.io.*;
import java.util.*;

import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;

/**
 * Title:        PrioritizingBasicQueue
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      New Idea Engineering, Inc.
 * @author Kevin-Neil Klop and Mark Bennett
 * @version 1.0
 */

public class PrioritizingBasicQueue extends BasicQueue
{

	private final static boolean debug = false;

	public final static long kDEFAULT_MAX_PRIORITY_INCREASE_AMOUNT = 5;
	public final static long kDEFAULT_FREQUENCY = 100;
	public final static long kDEFAULT_MARGIN = 25;
	public final static long kDEFAULT_MIN_PRIORITY = Thread.MIN_PRIORITY;
	//public final static long kDEFAULT_MAX_PRIORITY = Thread.MAX_PRIORITY;
	public final static long kDEFAULT_MAX_PRIORITY = 7;
	public final static String MARGIN_TAG = "margin";
	public final static String MAX_PRIORITY_TAG = "maximum_priority";
	public final static String MIN_PRIORITY_TAG = "minimum_priority";
	public final static String FREQUENCY_TAG = "frequency";


	public PrioritizingBasicQueue( String inQueueName,
		DPump inDPump, Element inSpecification
		)
	{
		super( inQueueName, inDPump, inSpecification );
		fDPump = inDPump;

		//long lCurrentPriority = Thread.currentThread().getPriority();
		long lCurrentPriority = getCurrentPriority();
		//fMaxPriority = lCurrentPriority + kDEFAULT_MAX_PRIORITY_INCREASE_AMOUNT;
		fMaxPriority = kDEFAULT_MAX_PRIORITY;
		// Autocorrect the bounds
		fMaxPriority = fMaxPriority <= Thread.MAX_PRIORITY ? fMaxPriority
			: Thread.MAX_PRIORITY ;
		//fMinPriority = lCurrentPriority - kDEFAULT_MAX_PRIORITY_INCREASE_AMOUNT;
		fMinPriority = kDEFAULT_MIN_PRIORITY;
		fMinPriority = fMinPriority >= Thread.MIN_PRIORITY ? fMinPriority
			: Thread.MIN_PRIORITY ;
		fFrequency = kDEFAULT_FREQUENCY;
		fMargin = kDEFAULT_MARGIN;

		if(debug)
		{
			System.err.println( "Debug: PrioritizingBaseQueue Constructor" );

			System.err.println( "\tlCurrentPriority="
				+ lCurrentPriority
				);
			System.err.println( "\tkDEFAULT_MAX_PRIORITY_INCREASE_AMOUNT="
				+ kDEFAULT_MAX_PRIORITY_INCREASE_AMOUNT
				);
			System.err.println( "\tfMinPriority=" + fMinPriority );
			System.err.println( "\tfMaxPriority=" + fMaxPriority );
			System.err.println( "\tSystem min is: " + Thread.MIN_PRIORITY );
			System.err.println( "\tSystem max is: " + Thread.MAX_PRIORITY );
			System.err.println( "\tfFrequency=" + fFrequency );
			System.err.println( "\tfMargin=" + fMargin );
		}


		if( inSpecification != null )
		{

			if(debug) System.err.println( "PriQ: Found parameters, parsing:" );

			fFrequency = JDOMHelper.getLongFromPathText( inSpecification,
				MARGIN_TAG, kDEFAULT_MARGIN
				);
			fFrequency = JDOMHelper.getLongFromPathText( inSpecification,
				FREQUENCY_TAG, kDEFAULT_FREQUENCY
				);

			fFrequency = JDOMHelper.getLongFromPathText( inSpecification,
				MIN_PRIORITY_TAG, fMinPriority
				);
			fFrequency = JDOMHelper.getLongFromPathText( inSpecification,
				MAX_PRIORITY_TAG, fMaxPriority
				);

			if(debug)
			{
				System.err.println( "PriQ: After parsing parameters new values:" );

				System.err.println( "\tfMinPriority=" + fMinPriority );
				System.err.println( "\tfMaxPriority=" + fMaxPriority );
				System.err.println( "\tfFrequency=" + fFrequency );
				System.err.println( "\tfMargin=" + fMargin );
			}

		}   // End if there was a specification

		// Do some error checking
		if( fMinPriority >= fMaxPriority )
		{
			System.err.println( "You must set the minimum priority greater than or equal to the maximum priority in queue " + inQueueName + "." );
			System.err.println( "Current values are:\n     Min: " + fMinPriority + "\n    Max: " + fMaxPriority );
			System.exit( -1 );
		}

		if( fMinPriority < Thread.MIN_PRIORITY )
		{
			System.err.println( "Invalid minimum priority:"
				+ " Must be >= system minimum."
				+ ", queue=" + inQueueName
				+ ", Requested min=" + fMinPriority
				+ ", System min=" + Thread.MIN_PRIORITY
				);
			System.exit( -1 );
		}

		if( fMaxPriority > Thread.MAX_PRIORITY )
		{
			System.err.println( "Invalid maximum priority:"
				+ " Must be <= system maximum."
				+ ", queue=" + inQueueName
				+ ", Requested min=" + fMaxPriority
				+ ", System min=" + Thread.MAX_PRIORITY
				);
			System.exit( -1 );
		}

	}


	public synchronized WorkUnit dequeue() throws InterruptedException
	{

		WorkUnit lReturnedWorkUnit = super.dequeue();
		long lNumDequeueOperations = getNumberOfDequeueOperations();

		if( lNumDequeueOperations >= fFrequency )
		{

			if( lNumDequeueOperations == fFrequency )
				fQueueLevel = getSize();

			else if( lNumDequeueOperations > fNextDequeueCheck )
			{
				fNextDequeueCheck += fFrequency;
				long lSize = getSize();

				// Is the queue growing?
				if( lSize > fQueueLevel + fMargin )
				{

					// Queue is growing in size.
					// Up the priority of the dequeue agent

					// IF it hasn't exceeded max priority.
					long currPri = getCurrentPriority();
					if( currPri < fMaxPriority )
					{
						attemptToRaisePriority();
					}

				}
				// Else is the queue shrinking?
				else if( lSize < fQueueLevel - fMargin )
				{

					// Queue is shrinking in size.  Down the priority of the dequeue agent
					// IF it hasn't shrunk under the min priority.
					long currPri = getCurrentPriority();
					if( currPri > fMinPriority )
					{
						attemptToLowerPriority();
					}
				}
			}
		}

		// Actually return the work unit
		return lReturnedWorkUnit;
	}


	public synchronized void enqueue( WorkUnit inObject )
	{

		super.enqueue( inObject );
		long lNumEnqueueOperations = getNumberOfEnqueueOperations();

		// Are we past T0
		if( lNumEnqueueOperations >= fFrequency )
		{
			// If we're just at the end of T0, make an initial measurement
			if( lNumEnqueueOperations == fFrequency )
				fQueueLevel = getSize();
			// Else we're past t0 we can do some interesting stuff
			// If we've past the next scheduled checkpoint
			else if( lNumEnqueueOperations > fNextEnqueueCheck )
			{
				// Schedule the next one
				fNextEnqueueCheck += fFrequency;
				long lSize = getSize();
				// Has the queue grown a signifocant amount
				if( lSize > fQueueLevel + fMargin )
				{

					// Queue is growing in size.
					// Lower the priority of the enqueue agent
					// IF it hasn't exceeded max priority.
					long currPri = getCurrentPriority();
					// Can we lower priority?
					if( currPri > fMinPriority )
					{
						attemptToLowerPriority();
					}
				}
				// Was the queue below by a sign amount
				else if( lSize < fQueueLevel - fMargin )
				{

					// Queue is shrinking in size.
					// Raise the priority of the enqueue agent
					// IF it hasn't shrunk under the min priority.
					long currPri = getCurrentPriority();
					if( currPri < fMaxPriority )
					{
						attemptToRaisePriority();
					}
				}
			}
		}

	}


	long getCurrentPriority()
	{
			Thread lCurrentThread = Thread.currentThread();
			return lCurrentThread.getPriority();
	}

	void attemptToSetAbsPriority( long inNewPri )
	{

		//final boolean debug = true;
		if(debug)
		{
			System.err.println( "PriQ: attemping to set new abs priority" );
			System.err.println( "\tqueue=" + fName );
			System.err.println( "\tnew requested abs value=" + inNewPri );
			//System.err.println( "\tSystem min is: " + Thread.MIN_PRIORITY );
			//System.err.println( "\tSystem max is: " + Thread.MAX_PRIORITY );
		}
		if( inNewPri >= Thread.MIN_PRIORITY &&
			inNewPri <= Thread.MAX_PRIORITY
			)
		{
			if(debug) System.err.println( "\tnew requested value"
				+ " IS in allowed range, will attempt to set" );
			fAdjustments++;
			try
			{
				Thread lCurrentThread = Thread.currentThread();
				lCurrentThread.setPriority( (int)inNewPri );
			}
			catch(Exception e)
			{
				System.err.println( "Error: PriQ: got exception"
					+ "setting thread priority"
					+ ", exception=" + e
					);
			}
		}
		else
		{
			if(debug) System.err.println( "\tnew requested value is out of range, ignoring" );
		}

	}

	void attemptToLowerPriority()
	{
		long currPri = getCurrentPriority();
		long newPri = currPri - 1;
		if(debug) System.err.println( "PriQ: asked to please lower priority"
			+ ", old value=" + currPri
			+ ", new value=" + newPri
			);
		attemptToSetAbsPriority( newPri );
	}
	void attemptToRaisePriority()
	{
		long currPri = getCurrentPriority();
		long newPri = currPri + 1;
		if(debug) System.err.println( "PriQ: asked to please raise priority"
			+ ", old value=" + currPri
			+ ", new value=" + newPri
			);
		attemptToSetAbsPriority( newPri );
	}

	/////////////////////////////////////////////////////////////////////////

	//

	// Extend the status reporting beyond the basic Queue Reporting...

	//

	/////////////////////////////////////////////////////////////////////////


	public JDOMHelper getStatusXML()
	{

		JDOMHelper superStatusXML = super.getStatusXML();
		superStatusXML.setAttributeString( "class", "PrioritizingBasicQueue" );

		// The main opening tag
		JDOMHelper tmpXML = null;
		try
		{
			tmpXML = new JDOMHelper( "<prioritizing_info/>", null );
		}
		catch (JDOMHelperException e)
		{
			System.err.println( "ERROR: PrioritizingBasicQueue:getStatusXML:"
				+ " Unable to create JDOM Helper."
				+ " Exception: " + e
				);
			return null;
		}

		String tmpStr;

		tmpStr = "" + fFrequency;
		tmpXML.addSimpleTextToNewPath( "check_frequency", tmpStr );
		tmpStr = "" + fNextEnqueueCheck;
		tmpXML.addSimpleTextToNewPath( "next_enqueue_check", tmpStr );
		tmpStr = "" + fNextEnqueueCheck;
		tmpXML.addSimpleTextToNewPath( "next_dequeue_check", tmpStr );
		tmpStr = "" + fMaxPriority;
		tmpXML.addSimpleTextToNewPath( "max_priority", tmpStr );
		tmpStr = "" +  fMinPriority;
		tmpXML.addSimpleTextToNewPath( "min_priority", tmpStr );
		tmpStr = "" +  fQueueLevel;
		tmpXML.addSimpleTextToNewPath( "queue_size_level", tmpStr );
		tmpStr = "" +  fMargin;
		tmpXML.addSimpleTextToNewPath( "queue_margin", tmpStr );
		tmpStr = "" +  fAdjustments;
		tmpXML.addSimpleTextToNewPath( "adjustment_count", tmpStr );

		superStatusXML.addContent( tmpXML );

		return superStatusXML;
	}



//	public String getStatusXML_OLD()
//	{
//
//		try
//		{
//			JDOMHelper lJDOMHelper = new JDOMHelper( super.getStatusXML(), null );
//			lJDOMHelper.addXMLTextToPath ( "",
//										  "<check frequency=\"" + fFrequency + "\" >"
//										  + "<enqueue next_check=\"" + fNextEnqueueCheck + "\"/>"
//										  + "<dequeue next_check=\"" + fNextDequeueCheck + "\"/>"
//										  + "</check>"
//										  + "<priority max=\"" + fMaxPriority + "\" "
//										  + "min=\"" + fMinPriority + "\" />"
//										  + "<queue_size_comparison num_items=\"" + fQueueLevel + "\" "
//										  + "margin=\"" + fMargin + "\" />"
//										  + "<adjustments>" + fAdjustments + "</fadjustments>" );
//			return lJDOMHelper.JDOMToString();
//		}
//		catch( JDOMHelperException jhe )
//		{
//			return "";
//		}
//	}


	DPump fDPump;

	long fFrequency;
	long fQueueLevel;
	long fNextDequeueCheck;
	long fNextEnqueueCheck;
	long fMargin;
	long fMaxPriority;
	long fMinPriority;
	long fAdjustments;

}


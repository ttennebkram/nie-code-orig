/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Kevin-Neil Klop
 *
 *  $Id: Queue.java,v 1.1 2004/03/03 20:00:51 mbennett Exp $
 *
 *  $Log: Queue.java,v $
 *  Revision 1.1  2004/03/03 20:00:51  mbennett
 *  Source tree from niesrv 226, from Feb 2004
 *
 *  Revision 1.1  2001/10/18 23:06:40  kevin
 *  Initial revision
 *
 */

//package nie.core;
package nie.pump.base;

import org.jdom.*;
import nie.core.NIEUtil;
import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;
//import nie.core.WorkUnit;
//import nie.core.StatusReporter;
import nie.pump.base.WorkUnit;
import nie.pump.base.StatusReporter;
import java.util.*;


class QueueClientTracker
{
	public QueueClientTracker( String inProcessorName, Thread inThread )
	{
		fProcessorName = inProcessorName;
		fQueue = 0;
		fNumQueues = 0;
	}

	public void TrackOperation()
	{
		fNumQueues++;
		fQueue = new Date().getTime();
		fState = PROCESSING;
	}

	public void setInWait()
	{
		fState = IN_WAIT;
	}

	public String report()
	{
		return "<queue_name>" + fProcessorName +
				"</queue_name><operations num_queues=" + fNumQueues +
				" last_operation=" + fQueue +
				"/>";
	}

	final static int IN_WAIT = 0;
	final static int PROCESSING = 1;

	String fProcessorName;
	long fQueue;
	long fNumQueues;
	int fState;
}


public abstract class Queue implements StatusReporter
{

	private static final boolean debug = false;


	public static final String SKIP_EXIT_SCAN_TAG = "no_exit_scan";
	public static final String PARAMETERS_TAG = "parameters";

	abstract WorkUnit getNextWorkUnit();
	abstract void putNextWorkUnit( WorkUnit inWorkUnit );
	public abstract String getProcessorName();
	public abstract Application getApplication();

	/////////////////////////////////////////
	//
	// Find a queue by name...
	//
	/////////////////////////////////////////

	public static Queue findQueueByName( String inQueueName )
	{
		Queue lReturnedQueue;

		lReturnedQueue = (Queue)gQueueTable.get( inQueueName );
		return lReturnedQueue;
	}

	/////////////////////////////////////////
	//
	// Constructor for the queue item
	//
	/////////////////////////////////////////

	public Queue( String inQueueName, Element inSpecification )
	{
		if(debug) System.err.println( "Debug:Q:Constr:start:"
			+ " q=" + inQueueName
			);


		fName = inQueueName;

		if( gQueueTable == null )
		{
			gQueueTable = new Hashtable();
		}

		gQueueTable.put( inQueueName, this );

		if( inSpecification != null )
		{

			if(debug) System.err.println( "Debug:Q:Constr:options:"
				+ " spec=" + JDOMHelper.JDOMToString( inSpecification )
				);

			Element lParamElement = inSpecification.getChild( PARAMETERS_TAG );
			if(debug) System.err.println( "Debug:Q:Constr:"
				+ " lParamElement=" + lParamElement
				);
			Element lSpecification = (lParamElement != null) ? lParamElement.getChild( SKIP_EXIT_SCAN_TAG ) : null ;
			if(debug) System.err.println( "Debug:Q:Constr:"
				+ " lSpecification=" + lSpecification
				);
			if( lSpecification != null )
			{
				// fIncludedInExitScan = false;
				setShouldIncludeInExitScan( false );
			}
			else
			{
				// fIncludedInExitScan = true;
				setShouldIncludeInExitScan( true );
			}
		}

		fProcessorsWhoWriteToMe = new Hashtable();
		fProcessorsWhoReadFromMe = new Hashtable();
		fProcessorsWithWorkUnits = new HashSet();
	}

	///////////////////////////////////////////////
	//
	// Dequeue an object from the queue.  The first one
	// is a non-blocking call.  If there is an item to
	// be dequeued, then it is returned.  If there is not
	// an item to be dequeued, then the "inWaitObject" is
	// added to the list of objects that will be notified
	// when an object is enqueued.
	//
	// The second Dequeue is a blocking call.  It will return
	// only if:
	//
	//  A) an item is successfully dequeued
	//  B) while waiting for an item, it is interrupted,
	//      in which case it, in turn, throws InterruptedException
	//
	///////////////////////////////////////////////

	public synchronized WorkUnit dequeue( Object inWaitObject ) throws InterruptedException
	{
		if( !isEmpty() )
			return dequeue();

		else
			fWaitingObjects.add( inWaitObject );

		return null;
	};

	public synchronized WorkUnit dequeue( ) throws InterruptedException
	{
		while( true )
		{
			if( ! isEmpty() )
			{
				WorkUnit lReturnedObject = getNextWorkUnit();
				if( lReturnedObject != null )
				{
					if( !lReturnedObject.getIsBackwash() )
						lReturnedObject.createRunpathEntry( getProcessorName() );

					TrackDequeue( Thread.currentThread() );

					// Now handled via TrackDequeue
					// if( !fProcessorsWithWorkUnits.contains( Thread.currentThread() ) )
					//	fProcessorsWithWorkUnits.add( Thread.currentThread() );

					if( ++fDequeueOps == Integer.MAX_VALUE )
						fDequeueOps = 0;

					if( getIsMonitoringOn() )
						fTimeLastDequeue = new Date().getTime();
					return lReturnedObject;
				}
			}
			else
			{
				synchronized( fProcessorsWithWorkUnits )
				{
					if( fProcessorsWithWorkUnits.contains( Thread.currentThread() ) )
						fProcessorsWithWorkUnits.remove( Thread.currentThread() );
				}

				TrackDequeueWaiting( Thread.currentThread() );
				wait();
			}
		}
	}

	//////////////////////////////////////////////////
	//
	// These routines are part of the monitor processor.
	//
	// TrackDequeueWaiting tracks whether a client is waiting
	// for an item or not.
	//
	// TrackDequeue tracks that an item has been successfully
	// dequeued.
	//
	// TrackEnqueue tracks that an item has been successfully enqueued.
	//
	// report returns an XML string that gives the status of this queue.
	//
	//////////////////////////////////////////////////

	private void TrackDequeueWaiting( Thread inThread )
	{
		if( getIsMonitoringOn() )
		{
			if( !fProcessorsWhoReadFromMe.keySet().contains( inThread ) )
			{
				QueueClientTracker lQueueClientTracker = new QueueClientTracker( getProcessorName(), inThread );
				lQueueClientTracker.setInWait();
				fProcessorsWhoReadFromMe.put( inThread, lQueueClientTracker );
			}
		}
	}

	private void TrackDequeue( Thread inThread )
	{
		if( !fProcessorsWithWorkUnits.contains( inThread ) )
			fProcessorsWithWorkUnits.add( inThread );

		if( getIsMonitoringOn() )
		{
			if( !fProcessorsWhoReadFromMe.keySet().contains( inThread ) )
			{
				QueueClientTracker lQueueClientTracker = new QueueClientTracker( getProcessorName(), inThread );
				lQueueClientTracker.TrackOperation();
				fProcessorsWhoReadFromMe.put( inThread, lQueueClientTracker );
			}
			else
			{
				QueueClientTracker lQueueClientTracker = (QueueClientTracker)fProcessorsWhoReadFromMe.get( inThread );
				lQueueClientTracker.TrackOperation();
			}
		}
	}

	private void TrackEnqueue( Thread inThread )
	{
		if( getIsMonitoringOn() )
		{
			fTimeLastEnqueue = new Date().getTime();

			if( !fProcessorsWhoWriteToMe.keySet().contains( inThread ) )
			{
				QueueClientTracker lQueueClientTracker = new QueueClientTracker( getProcessorName(), inThread );
				lQueueClientTracker.TrackOperation();
				fProcessorsWhoWriteToMe.put( inThread, lQueueClientTracker );
			}
			else
			{
				QueueClientTracker lQueueClientTracker = (QueueClientTracker)fProcessorsWhoWriteToMe.get( inThread );
				lQueueClientTracker.TrackOperation();
			}
		}
	}

	//////////////////////////////////////////////////////////////
	//
	// StatusReporter routines.  Note that this is the default
	// getStatusXML() for all Queues.  If a Queue wants to add
	// information to this, then they should call this routine,
	// save the returned string, and then add into it (probably
	// through JDOMHelper() ).
	//
	//////////////////////////////////////////////////////////////

	// Should we be concerned with keeping and reporting monitoring statistics
	// Typically you would override this with a call to the app, which has
	// a helper function.
	// Here, since we don't have access to the app object, we just return true.
	// This is already overridden in BasicQueue.
	protected boolean getIsMonitoringOn()
	{
		return true;
	}

	public JDOMHelper getStatusXML()
	{

		// The main opening tag
		JDOMHelper tmpXML = null;
		try
		{
			tmpXML = new JDOMHelper( "<queue_status/>", null );
		}
		catch (JDOMHelperException e)
		{
			System.err.println( "ERROR: Queue:getStatusXML:"
				+ " Unable to create JDOM Helper."
				+ " Exception: " + e
				);
			return null;
		}

		String tmpStr, tmpStr2;

		// Set the name
		tmpXML.setAttributeString( "name", getName() );

		// Queue size
		tmpStr = "" + getSize();
		tmpXML.addSimpleTextToNewPath(
			"items_in_queue", tmpStr
			);

		// Enqueue info
		tmpStr = "" + getNumberOfEnqueueOperations();
		tmpXML.addSimpleTextToNewPath(
			"enqueue_information/number_operations", tmpStr
			);
		tmpStr = "" + getTimeLastEnqueue();
		tmpXML.addSimpleTextToNewPath(
			"enqueue_information/time_last_operation", tmpStr
			);

		// Get and store DURAION info, how long ago
		if( getTimeLastDequeue() > 0 )
		{
			long eqDuration = new Date().getTime() - getTimeLastDequeue();
			tmpStr = "" + eqDuration;
			tmpStr2 = NIEUtil.formatTimeIntervalFancyMS( eqDuration );
			// System.err.println( "tmpStr2='" + tmpStr2 + "'" );
			tmpXML.addSimpleTextToNewPath(
				"enqueue_information/since_last_operation", tmpStr2
				);
			tmpXML.setAttributeStringForPath(
				"enqueue_information/since_last_operation[-1]",
				"ms", tmpStr
				);
		}

		// Dequeue info
		tmpStr = "" + getNumberOfDequeueOperations();
		tmpXML.addSimpleTextToNewPath(
			"dequeue_information/number_operations", tmpStr
			);
		tmpStr = "" + getTimeLastDequeue();
		tmpXML.addSimpleTextToNewPath(
			"dequeue_information/time_last_operation", tmpStr
			);

		// Get and store DURAION info, how long ago
		if( getTimeLastDequeue() > 0 )
		{
			long dqDuration = new Date().getTime() - getTimeLastDequeue();
			tmpStr = "" + dqDuration;
			tmpStr2 = NIEUtil.formatTimeIntervalFancyMS( dqDuration );
			// System.err.println( "tmpStr2='" + tmpStr2 + "'" );
			tmpXML.addSimpleTextToNewPath(
				"dequeue_information/since_last_operation", tmpStr2
				);
			tmpXML.setAttributeStringForPath(
				"dequeue_information/since_last_operation[-1]",
				"ms", tmpStr
				);
		}


		// Can it exit or not
		tmpStr = canExit() ? "1" : "0";
		tmpXML.addSimpleTextToNewPath( "can_exit", tmpStr );

		// Get all the connections and add them
		JDOMHelper connections = ((DPump)getApplication()).getProcessorsForQueueAsXML(
			getName()
			);
		tmpXML.addContent( connections );

		return tmpXML;
	}


	public String getStatusXML_OLD()
	{
		//fIsMonitored = true;
		String lReturnXML = "<queue_status name=\"" + getName() + "\">";

		lReturnXML += "<items_in_queue>" + getSize()
			+ "</items_in_queue>";

		// Enqueue info
		lReturnXML += "<enqueue_information>";
		lReturnXML += "<number_operations>"	+ getNumberOfEnqueueOperations()
			+ "</number_operations>";
		lReturnXML += "<time_last_operation>" + getTimeLastEnqueue()
			+ "</time_last_operation>";
		lReturnXML += "</enqueue_information>";

		// Dequeue info
		lReturnXML += "<dequeue_information>";
		lReturnXML += "<number_operations>"	+ getNumberOfDequeueOperations()
			+ "</number_operations>";
		lReturnXML += "<time_last_operation>" + getTimeLastDequeue()
			+ "</time_last_operation>";
		lReturnXML += "</dequeue_information>";

		lReturnXML += "<can_exit>";
		if( canExit() )
			lReturnXML += "1";
		else
			lReturnXML += "0";
		lReturnXML += "</can_exit>";


		lReturnXML += "</queue_status>";
		return lReturnXML;
	}

	public boolean canExit()
	{
		if( getShouldIncludeInExitScan() )
			return isEmpty();
		else
			return true;
	}


	//////////////////////////////////////////////////////////
	//
	// Status and information routines used by the old monitoring system
	//
	//////////////////////////////////////////////////////////

	public long getNumberOfEnqueueOperations()
	{
		return fEnqueueOps;
	}

	public long getNumberOfDequeueOperations()
	{
		return fDequeueOps;
	}

	public boolean isEmpty()
	{
		return fDequeueOps == fEnqueueOps;
	}

	public long getSize()
	{
		if( fDequeueOps > fEnqueueOps )
			return Long.MAX_VALUE - fDequeueOps + fEnqueueOps;
		else
			return fEnqueueOps - fDequeueOps;
	}

	public long getTimeLastDequeue()
	{
		//fIsMonitored = true;
		return fTimeLastDequeue;
	}

	public long getTimeLastEnqueue()
	{
		//fIsMonitored = true;
		return fTimeLastEnqueue;
	}

	//public boolean shouldMonitor()
	public boolean getShouldIncludeInExitScan()
	{
		return fIncludedInExitScan;
	}
	public void setShouldIncludeInExitScan( boolean flag )
	{
		fIncludedInExitScan = flag ;
	}

	public String getName()
	{
		return fName;
	}

	/////////////////////////////////////////////////////////////
	//
	// Enqueue an item into the queue.
	//
	/////////////////////////////////////////////////////////////

	public synchronized void enqueue( WorkUnit inObject )
	{
		if( ++fEnqueueOps == Integer.MAX_VALUE )
			fEnqueueOps = 0;

		putNextWorkUnit( inObject );
		if( getIsMonitoringOn() )
			TrackEnqueue( Thread.currentThread() );

		notifyAll();
	}

	/////////////////////////////////////////////////////////////
	//
	// Determine if the queue is ready to exit or not.
	//
	/////////////////////////////////////////////////////////////

	//public boolean isReadyToExit()
	// Giving a clearer name, distinct from canExit(), per discussion
	// with Kevin.
	// Todo: this isn't called by anybody at this time
	public boolean isCompletelyFinished()
	{
		//if( fIncludedInExitScan == false )
		//if( ! getShouldIncludeInExitScan() )
		//	return true;
		// ^^^ I don't think our answer should change just because
		// nobody's watching

		if( fProcessorsWithWorkUnits.isEmpty() && isEmpty() )
			return true;
		else
			return false;
	}

	////////////////////////////////////////////////////////////
	//
	// With the introduction of the non-blocking dequeue operations,
	// we had to have a way of removing an item from the notification
	// list.
	//
	////////////////////////////////////////////////////////////

	public void abortDequeue( Object inObject )
	{
		fWaitingObjects.remove( inObject );
	}

	////////////////////////////////////////////////////////////
	//
	// private Member variables
	//
	////////////////////////////////////////////////////////////
	static protected Hashtable gQueueTable = null;

	HashSet fProcessorsWithWorkUnits;
	Hashtable fProcessorsWhoReadFromMe;
	Hashtable fProcessorsWhoWriteToMe;

	String fName;
	long fDequeueOps;
	long fEnqueueOps;
	//boolean fIsMonitored;
	long fTimeLastDequeue;
	long fTimeLastEnqueue;
	boolean fIncludedInExitScan;
	Vector fWaitingObjects;
}



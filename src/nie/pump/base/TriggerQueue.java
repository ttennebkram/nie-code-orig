//package nie.core;
package nie.pump.base;

import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;
import nie.core.RunLogBasicImpl;
import nie.core.RunLogInterface;
//import nie.core.Queue;
//import nie.core.WorkUnit;
import nie.pump.base.Queue;
import nie.pump.base.WorkUnit;
import org.jdom.Element;
import java.util.Vector;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;

public class TriggerQueue extends Queue
{
	public static final String SUBSCRIBE_TRIGGERS_TAG = "subscribe_trigger";
	public static final String TRIGGER_TAG = "trigger";
	public static final String PROCESSOR_NAME_TAG = "processor_id";

	// Common messages you might want to send
	public static final String DONE_TRIGGER_TEXT = "done";

	public static String kClassName() { return "TriggerQueue"; }

	/////
	//
	// Constructor for TriggerQueue
	//
	/////

	public TriggerQueue( String inQueueName, DPump inDPump, Element inSpecification )
	{
		super( inQueueName, inSpecification );
		// inDPump is the overall application, NOT a specific processor

		final String kFName = "constructor";
		if( inDPump == null )
		{
			fatalErrorMsg( kFName, "When constructing trigger queue, inDPump is null!!!!" );
			System.exit(-1);
		}

		commonInit( inQueueName, inDPump, inSpecification );
	}

	void commonInit( String inQueueName, DPump inDPump, Element inSpecification )
	{
		fQueueName = inQueueName;
		fTriggerTrackerList = new Hashtable();
		fDPump = inDPump;
	}

	/////
	//
	// Enqueue a trigger.  This might be a subscription request or a
	// real trigger.  If it's a subscription request, then we have to
	// update the thread's TriggerTracker.  if it's a real trigger
	// then we need to go through the various TriggerTrackers to see
	// if someone has subscribed to the trigger.
	//
	/////

	public synchronized void enqueue( WorkUnit inWorkUnit )
	{
		processSubscriptions( inWorkUnit );
		processTriggers( inWorkUnit );
	}

	/////
	//
	// There are two forms of dequeue.  The first kind is a blocking
	// call in which the caller will block until there is something
	// available.  note that the "available" statement means something
	// different on a trigger queue than on a normal queue.
	//
	/////
	//
	// This is the blocking dequeue.  Note that if there's a "waiting
	// object" defined, then this becomes a non-blocking call.
	//
	/////

	public synchronized WorkUnit dequeue()
		throws InterruptedException
	{

		/////
		//
		// Get the tracker that corresponds to the currently executing
		// processor.  DPump has a useful routine that returns the ID
		// of the currently executing processor.  Use this routine to
		// find out the name of the processor that is executing the
		// dequeue operation.
		//
		/////

		String lProcessorID = fDPump.getProcessorName();
		TriggerTracker lTriggerTracker = getTracker( lProcessorID );

		/////
		//
		// We loop until we're either interrupted or we manage to
		// dequeue an operation.  note that within the loop, there's a
		// check if this is a non-blocking opertaion.  if it is
		// non-blocking, then we exit the loop regardless of whether
		// we've successfully dequeued a work unit or not.
		//
		/////

		while( true )
		{
			synchronized( lTriggerTracker )
			{
				if( lTriggerTracker.fTrippedTriggersQueue.size() > 0 )
				{
					//
					// Get (and remove) the top queued trigger from
					// the trigger FIFO Queue.
					//

					String lTriggerName = (String)lTriggerTracker.fTrippedTriggersQueue.elementAt( 0 );
					lTriggerTracker.fTrippedTriggersQueue.remove( 0 );

					//
					// Create a work unit to return the trigger to the
					// caller.
					//

					WorkUnit lReturnedWorkUnit = createTriggerWorkUnit( lTriggerName );
					if( lReturnedWorkUnit == null )
					{
						// Creating the work unit failed!!! Requeue
						// the trigger and wait (and hope) for the
						// system to become more steady.

						lTriggerTracker.fTrippedTriggersQueue.add( lTriggerName );
					}
					else
						//
						// Work unit created - return the work unit.
						//

						return lReturnedWorkUnit;
				}

				//
				// Check if a "wait object" has been defined.  If it
				// has not then this is a BLOCKING dequeue and we
				// should wait until something is enqueued to the
				// trigger queue.
				//

				if( lTriggerTracker.fWaitObject == null )
				{
					//
					// We are a blocking operation.  Wait for
					// something to be enqueue to the trigger queue
					// for this processor.
					//

					lTriggerTracker.wait();
				}
				else
				{
					//
					// We are not a blocking operation.  Return null
					// to tell the caller that there is nothing in the
					// queue.
					//

					return null;
				}
			}   // End of sync on trigger tracker
		}   // End of while true
	}

	/////
	//
	// Of course, this is the non-blocking call.  It sets the waiting
	// object in the TriggerTracker then calls the "blocking" version.
	//
	/////

	public synchronized WorkUnit dequeue( Object inWaitObject )
		throws InterruptedException
	{
		TriggerTracker lTriggerTracker = getTracker( fDPump.getProcessorName() );
		lTriggerTracker.fWaitObject = inWaitObject;
		return dequeue();
	}

	/////
	//
	// Get the correct TriggerTracker for the processor with ID of
	// "inProcessID" (that is passed in).
	//
	/////

	TriggerTracker getTracker( String inProcessID )
	{
	    final String kFName = "getTracker";
		//
		// We want to define this outside the try
		// catch block.  Otherwise the last return
		// statement fails.
		//

		TriggerTracker lReturnedTriggerTracker = null;

		//
		// Catch both the smart alecks and the idiots who try to
		// put in null or empty process ids.
		//

		if( (inProcessID == null) || (inProcessID == "") )
		{
			fatalErrorMsg( kFName, "A Processor with a null or empty processor ID is attempting to find a trigger queue." );
			errorMsg( kFName, "fDPump="+fDPump );
			if( null!=fDPump ) {
			    errorMsg( kFName, "fDPump.getProcessorName()="+fDPump.getProcessorName() );
			    fDPump.showThreads();
			}
		    // System.err.println( "Such behaviour is strictly verboten.  Further abuse can lead" );
			// System.err.println( "to blindness and alopecia.  Please do not further feed the animals." );
			throw new RuntimeException();
			// System.exit( -1 );
		}

		//
		// Okay, it's not a null.  Under those circumstances, I
		// believe you will NOT ever get an exception from a
		// Hashtable.put() but, rather, will be returned a null if the
		// key does not exists.  Still, we'll be cautious and wrap it
		// in a try/catch.
		//

		try
		{
			lReturnedTriggerTracker = (TriggerTracker)fTriggerTrackerList.get( inProcessID );
		}
		catch(Exception e)
		{
			lReturnedTriggerTracker = null;
		}

		if( lReturnedTriggerTracker == null )
		{
			lReturnedTriggerTracker = new TriggerTracker( inProcessID );
			fTriggerTrackerList.put( inProcessID, lReturnedTriggerTracker );
		}
		return lReturnedTriggerTracker;
	}

	/////
	//
	// Check if the queue of triggers ALREADY SENT is empty.  This is
	// by checking if the queue size is 0.
	//
	/////

	public boolean isEmpty()
	{
		return getSize() == 0;
	}

	/////
	//
	// Get the size of the current trigger list.
	//
	// There are two versions.  One version returns the size
	// for the currently executing processor.  The other one
	// returns the size for the specified processor.
	//
	/////

	public long getSize( String inProcessID )
	{
		TriggerTracker lTriggerTracker = getTracker( inProcessID );
		return lTriggerTracker.fTrippedTriggersQueue.size();
	}

	public long getSize()
	{
		return getSize( fDPump.getProcessorName() );
	}

	/////
	//
	// Return a status string for monitoring
	//
	/////

	public JDOMHelper getStatusXML()
	{
	    final String kFName = "getStatusXML";

		// The main opening tag
		JDOMHelper tmpXML = null;
		try
		{
			tmpXML = new JDOMHelper( "<queue_status/>", null );
		}
		catch (JDOMHelperException e)
		{
			errorMsg( kFName,
				"Unable to create JDOM Helper."
				+ " Exception: " + e
				);
			return null;
		}

		String tmpStr;

		// Set the name and type
		tmpXML.setAttributeString( "name", getName() );
		tmpXML.setAttributeString( "class", "TriggerQueue" );

		// Get the status for the trigger trackers
		for( Enumeration lEnumeration = fTriggerTrackerList.elements() ;
			lEnumeration.hasMoreElements() ;
			)
		{
			TriggerTracker lTriggerTracker =
				(TriggerTracker)lEnumeration.nextElement();

			tmpStr = "" + lTriggerTracker.fWhichProcessor;
			tmpXML.addSimpleTextToNewPath(
				"trigger_client", tmpStr
				);

			for( int i = 0; i < lTriggerTracker.fTrippedTriggersQueue.size(); i++ )
			{
				tmpStr = "" + lTriggerTracker.fTrippedTriggersQueue.elementAt( i );
				tmpXML.addSimpleTextToNewPath(
					"trigger_client[-1]/trigger", tmpStr
					);
			}
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

		String lString = "<trigger_queue name=\"" + getName() + "\">" ;
		for( Enumeration lEnumeration = fTriggerTrackerList.elements() ;
			lEnumeration.hasMoreElements() ;
			)
		{
			TriggerTracker lTriggerTracker =
				(TriggerTracker)lEnumeration.nextElement();
			lString += "<trigger_client name=\"" + lTriggerTracker.fWhichProcessor + "\">";
			for( int i = 0; i < lTriggerTracker.fTrippedTriggersQueue.size(); i++ )
			{
				lString += "<trigger>" + lTriggerTracker.fTrippedTriggersQueue.elementAt( i ) + "</trigger>";
			}
			lString += "</trigger_client>";
		}
		lString += "</trigger_queue>";
		return lString;
	}

	/////
	//
	// Various getters/status information...
	//
	/////

	public String getProcessorName()
	{
		return fQueueName;
	}

	public Application getApplication()
	{
		return fDPump;
	}

	/////
	//
	// Stubs so that this isn't an abstract class...
	// They are stubbed out since we've overridden the "enqueue" and "dequeue"
	// methods from Queue and so should never be called.
	//
	/////

	public void putNextWorkUnit( WorkUnit inWorkUnit )
	{
		return;
	}

	public WorkUnit getNextWorkUnit()
	{
		return null;
	}

	/////
	//
	// processSubscriptions - check a work unit for subscription requests and, if present,
	// add them to the triggers being subscribed to.
	//
	/////

	void processSubscriptions( WorkUnit inWorkUnit )
	{
	    final String kFName = "processSubscriptions";

	    //
		// Get a list of all the triggers to which we wish to
		// subscribe AND the ProcessorID that wishes to subscribe.
		// Both come from the input work unit.
		//

		List lSubscriptionRequests = inWorkUnit.getUserFields( SUBSCRIBE_TRIGGERS_TAG );
		String lProcessorID = inWorkUnit.getUserFieldText( PROCESSOR_NAME_TAG );

		debugMsg( kFName, "lProcessorID=" + lProcessorID );

		//
		// Catch someone being a wise guy or anti-social by passing in
		// a null or empty processor id.  Both of those would cause
		// severe problems.
		//

		if( (lProcessorID == null) || (lProcessorID == "") )
		{
			errorMsg( kFName, "A Processor is subscribing with a null or empty processor ID." );
			// System.err.println( "Such behaviour is strictly verboten.  Further abuse can lead" );
			// System.err.println( "to blindness and alopecia.  Please do not further feed the animals." );
			System.exit( -1 );
		}

		// How many subscriptions did we get?
		int numSubs = (lSubscriptionRequests != null) ? lSubscriptionRequests.size() : 0 ;

		// Sanity check
		if( numSubs < 1 )
		{
			return;
		}

		//
		// Since we have to track the subscriptions and trigger queues
		// on a per-processor basis, find out which "trigger tracker"
		// has the information about the calling processor.
		//

		TriggerTracker lTriggerTracker = getTracker( lProcessorID );

		//
		// Loop through all the <subscribe_trigger> tags and add them
		// to the fSubscribedTriggerSet that contains the list of triggers to
		// which we've subscribed.
		//

		for( Iterator i = lSubscriptionRequests.iterator(); i.hasNext(); )
		{
			//
			// Get a subscribe_trigger element from the list.
			//

			Element lElement = (Element)i.next();
			String lTriggerName = lElement.getTextTrim();

			//
			// Ensure that noone's being a wise guy and passing in
			// null or empty trigger names
			//

			if( lTriggerName == null || lTriggerName.trim().equals("") )
			{
				continue;
			}

			//
			// Add the trigger name to our subscribed set.
			//

			lTriggerTracker.fSubscribedTriggerSet.add( lTriggerName );
		}
	}

	/////
	//
	// ProcessTriggers - take the triggers being requested and
	// propagate them to all the subscribers of those triggers.
	//
	/////

	void processTriggers( WorkUnit inWorkUnit )
	{
	    final String kFName = "processTriggers";

	    //
		// Extract the list of triggers that are being tripped
		//

		List lTriggers = inWorkUnit.getUserFields( TRIGGER_TAG );

		// How many triggers did we get?
		int numTrigs = lTriggers != null ? lTriggers.size() : 0 ;

		// Sanity check
		if( numTrigs < 1 )
		{
			return;
		}

		//
		// For each of the triggers being tripped, scan through all
		// the trigger trackers and find out who has subscribed to
		// that trigger.  When a triggertracker HAS subscribed to the
		// trigger, enqueue it to THAT triggertracker's
		// "TrippedTriggersQueue".  In addition, if the queue was
		// empty, then notify that queue's waitObject or that queue to
		// wake up anyone who's waiting.
		//

		for( Iterator i = lTriggers.iterator(); i.hasNext(); )
		{
			Element lElement = (Element)i.next();
			String lTriggerName = lElement.getTextTrim();

			//
			// Ensure that noone's being a wise guy
			//

			if( (lTriggerName == null) || (lTriggerName.trim().equals("")) )
			{
				errorMsg( kFName,
									  "" + Thread.currentThread()
									  + ":"
					+ " Null trigger name found"
					);
				continue;
			}

			//
			// Scan through all the trigger trackers looking for someone who's
			// subscribed.  Note that we indiscriminately call "setTrigger".
			// setTrigger will not actually set the trigger unless it's been
			// subscribed to.
			//

			for( Enumeration j = fTriggerTrackerList.elements() ;
				j.hasMoreElements() ;
				)
			{
				TriggerTracker lTriggerTracker = (TriggerTracker) j.nextElement();
				lTriggerTracker.setTrigger( lTriggerName );
			}
		}
	}

	/////
	//
	// Create a trigger Work unit given a trigger name.
	//
	/////

	public WorkUnit createTriggerWorkUnit( String inTriggerName )
	{
		WorkUnit lReturnedWorkUnit;

		try
		{
			lReturnedWorkUnit = new WorkUnit();
		}
		catch( Exception e )
		{
			return null;
		}

		lReturnedWorkUnit.addNamedField( TRIGGER_TAG, inTriggerName );

		return lReturnedWorkUnit;
	}

	private static void ___Sep__Run_Logging__(){}

	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName(), inFromRoutine );
	}

	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}

	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}

	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( kClassName(), inFromRoutine );
	}

	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName(), inFromRoutine,
			e, optMessage
			);
	}

	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}


	
	/////
	//
	// Instance variables.
	//
	/////

	String fQueueName;
	Hashtable fTriggerTrackerList;
	DPump fDPump;
}


/////
//
// This class is used to track each processor's subscribed and tripped
// triggers.
//
/////

class TriggerTracker
{
	public static String kClassName() { return "TriggerTracker"; }

	/////
	//
	// Constructor... Ensure that everything's initialized.
	//
	/////

	public TriggerTracker(String inProcessorID )
	{
	    final String kFName = "constructor";

	    fTrippedTriggersQueue = new Vector();
		fSubscribedTriggerSet = new HashSet();
		fWhichProcessor = inProcessorID;

		if( (inProcessorID == null) || (inProcessorID == "") )
		{
			fatalErrorMsg( kFName, "A Processor with a null or empty processor ID is attempting to use a trigger queue." );
			// System.err.println( "Such behaviour is strictly verboten.  Further abuse can lead" );
			// System.err.println( "to blindness and alopecia.  Please do not further feed the animals." );
			// System.err.println();

			//
			// Essentially, throw an exception so that the user can
			// help track down the problem.
			//

			Exception e = new Exception();
			// e.printStackTrace();
			stackTrace( kFName, e, "null/empty process id for trigger tracker" );

			System.exit( -1 );
		}
	}

	/////
	//
	// If the processor is subscribed to the specified trigger, then
	// enqueue the trigger and, if necessary, wake up the processor.
	//
	/////

	public void setTrigger( String inTriggerName )
	{
		if( ! fSubscribedTriggerSet.contains( inTriggerName ) )
		{
			// If it's not in the trigger set then this thread did not
			// subscribe to the trigger.

			return;
		}
		else
		{
			//
			// If it's in the trigger set, then this thread subscribed
			// to the trigger and we should add it to the trigger
			// queue.
			//

			fTrippedTriggersQueue.add( inTriggerName );

			//
			// If a wait object has been given, then we should notify
			// the wait object to wake up the processor.
			//

			if( fWaitObject != null )
			{
				synchronized( fWaitObject )
				{
					fWaitObject.notifyAll();
				}
			}
		}
	}

	private static void ___Sep__Run_Logging__(){}

	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}

	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName(), inFromRoutine );
	}

	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}

	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}

	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( kClassName(), inFromRoutine );
	}

	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	private static boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName(), inFromRoutine,
			e, optMessage
			);
	}

	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}



	/////
	//
	// The instance variables.
	//
	/////

	//
	// This set contains the names of the triggers to which we've
	// subscribed.
	//

	public HashSet fSubscribedTriggerSet;

	//
	// This Vector contains the time-ordered list of triggers that
	// have been tripped.
	//

	public Vector fTrippedTriggersQueue;

	//
	// If a wait object has been specified for the queue by a
	// subscriber, this is where we keep it.
	//

	public Object fWaitObject;

	//
	// This contains the name of the processor we are tracking.
	//

	public String fWhichProcessor;
}


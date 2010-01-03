/*
 *  Copyright 2001-2009 by New Idea Engineering, Inc.
 */

//package nie.core;
package nie.xpump2;

import org.jdom.*;
import java.util.*;

import nie.core.NIEUtil;
import nie.core.JDOMHelper;
import nie.core.JDOMHelperException;
import nie.core.RunLogInterface;
import nie.core.RunLogBasicImpl;

//import nie.core.StatusReporter;
// import nie.pump.base.StatusReporter;

public abstract class ProcessorBase implements Runnable, StatusReporter
{

	abstract public String kClassName();

	abstract public int getMinInputQs();
	abstract public int getMaxInputQs();
	// abstract public List getInputQNames();
	abstract public int getMinOutputQs();
	abstract public int getMaxOutputQs();
	public int getMinErrorQs() { return 1; }
	public int getMaxErrorQs() { return 1; }
	// TODO: need "uses" ?

	// Simple status logic
	public boolean getRequiresInputQs()
	{
		return getMinInputQs() > 0;
	}
	public boolean getAllowsInputQs()
	{
		return getMaxInputQs() != 0;
	}
	public boolean getRequiresOutputQs()
	{
		return getMinOutputQs() > 0;
	}
	public boolean getAllowsOutputQs()
	{
		return getMaxOutputQs() != 0;
	}
	public boolean getRequiresErrorQs()
	{
		return getMinOutputQs() > 0;
	}
	public boolean getAllowsErrorQs()
	{
		return getMaxOutputQs() != 0;
	}

	void validateInternalConfig()
		throws ExceptionInvalidQs
	{
		// Input Qs
		if( getMinInputQs()>0 && getMaxInputQs()>0 && getMinInputQs()>getMaxInputQs() )
			throw new ExceptionInvalidQs( "Max Input Qs less than Min Input Qs"
					+ ", Min=" + getMinInputQs() + ", Max=" + getMaxInputQs()
					);
		if( getRequiresInputQs() && ! getAllowsInputQs() )
			throw new ExceptionInvalidQs( "Input Qs Required but not Allowed" );

		// Output Qs
		if( getMinOutputQs()>0 && getMaxOutputQs()>0 && getMinOutputQs()>getMaxOutputQs() )
			throw new ExceptionInvalidQs( "Max Output Qs less than Min Output Qs"
					+ ", Min=" + getMinOutputQs() + ", Max=" + getMaxOutputQs()
					);
		if( getRequiresOutputQs() && ! getAllowsOutputQs() )
			throw new ExceptionInvalidQs( "Output Qs Required but not Allowed" );

		// Error Qs
		if( getMinErrorQs()>0 && getMaxErrorQs()>0 && getMinErrorQs()>getMaxErrorQs() )
			throw new ExceptionInvalidQs( "Max Error Qs less than Min Error Qs"
					+ ", Min=" + getMinErrorQs() + ", Max=" + getMaxErrorQs()
					);
		if( getRequiresErrorQs() && ! getAllowsErrorQs() )
			throw new ExceptionInvalidQs( "Error Qs Required but not Allowed" );
	}
	
	public static final boolean _debug = false;

	/********************************************************************
	 **
	 ** Constants of use to other classes and/or status reporting systems
	 **
	 ********************************************************************/
	private static void ___Sep__Constannts__(){}

	// WARNING!!!!!
	// If you change / add to these, you should probably update
	// canExit().  Make sure to check all classes.
	public static final int kUnknown = 0;
	public static final int kWaiting = 1;
	public static final int kProcessing = 2;
	public static final int kInitializing = 3;
	public static final int kFinished = 4;
	public static final int kBroken = 5;
	public static final String gStateStrings[] = {
		"Unknown",
		"Waiting",
		"Processing",
		"Initializing",
		"Finished",
		"Broken"
		};

//	public static final int kWaiting = 0;
//	public static final int kProcessing = 1;
//	public static final int kInitializing = 2;
//	public static final String gStateStrings[] = { "Waiting", "Processing", "Initializing" };

	//////////////////////////////////////////////////////////////////////
	//
	// Constructor
	//
	//////////////////////////////////////////////////////////////////////
	private static void ___Sep__Constructor__(){}

	public ProcessorBase( Application inApplication,
			  Queue[] inReadQueueList,
			  Queue[] inWriteQueueList,
			  Queue[] inUsesQueueList,
			  Element inParameter,
			  String inID )
	{
		super();

		fReadQueueList = inReadQueueList;
		fWriteQueueList = inWriteQueueList;
		fUsesQueueList = inUsesQueueList;
		fParamElement = inParameter;
		fApplication = inApplication;
		fID = inID;

		cacheCurrentPriority();
	}


	private static void ___Sep__Simple_Logic__(){}

	protected boolean _getIsMonitoringOn()
	{
		// return fApplication.getIsMonintoringOn();
		return false;
	}

	public boolean canExit()
	{
		return (
			fState == kWaiting ||
			fState == kFinished ||
			fState == kBroken
			);
	}

	protected void setState( int inNewState )
	{
		// Tell the world we're ready to exit
		fState = inNewState;
	}

	protected int getState()
	{
		return fState;
	}

	protected void setStateWaiting()
	{
		// Tell the world we're ready to exit
		fState = kWaiting;
	}
	protected void setStateProcessing()
	{
		// Tell the world we're ready to exit
		fState = kProcessing;
	}
	protected void setStateFinished()
	{
		// Tell the world we're ready to exit
		fState = kFinished;
	}
	protected void setStateBroken()
	{
		// Tell the world we're ready to exit
		fState = kBroken;
	}

	public JDOMHelper _getStatusXML()
	{
	    final String kFName = "getStatusXML";
		//fIsMonitored = true;

		// The main opening tag
		JDOMHelper tmpXML = null;
		try
		{
			tmpXML = new JDOMHelper( "<processor_status/>", null );
		}
		catch (JDOMHelperException e)
		{
			stackTrace( kFName, e,
				"Unable to create JDOM Helper."
				);
			return null;
		}

		String tmpStr;
		String tmpStr2;

		// Set the name
		tmpXML.setAttributeString( "name", getID() );

		// Set the state/status
		tmpStr = gStateStrings[fState];
		tmpXML.addSimpleTextToNewPath( "state", tmpStr );

		// Can it exit or not
		tmpStr = canExit() ? "1" : "0";
		tmpXML.addSimpleTextToNewPath( "can_exit", tmpStr );

		// Priority info
		tmpStr = "" + getCachedPriority();
		tmpXML.addSimpleTextToNewPath( "current_priority", tmpStr );

		// Set the dequeue info
		// Num ops
		tmpStr = "" + fDequeueOperations;
		tmpXML.addSimpleTextToNewPath(
			"dequeue_information/number_operations", tmpStr
			);
		// last retrieved
		if( fExitedWait != 0 )
		{
			tmpStr = "" + fExitedWait;
			tmpXML.addSimpleTextToNewPath(
				"dequeue_information/time_last_woke_up", tmpStr
				);
			// Get and store DURAION info, how long ago
			long duration = new Date().getTime() - fExitedWait;
			tmpStr = "" + duration;
			tmpStr2 = NIEUtil.formatTimeIntervalFancyMS( duration );
			// System.err.println( "tmpStr2='" + tmpStr2 + "'" );
			tmpXML.addSimpleTextToNewPath(
				"dequeue_information/since_last_woke_up", tmpStr2
				);
			tmpXML.setAttributeStringForPath(
				"dequeue_information/since_last_woke_up[-1]",
				"ms", tmpStr
				);

		}
		else
		{
			tmpXML.addSimpleTextToNewPath(
				"dequeue_information/no_last_wokeup_time", "sorry"
				);
		}
		// enqueue
		tmpStr = "" + fEnqueueOperations;
		tmpXML.addSimpleTextToNewPath(
			"enqueue_information/number_operations", tmpStr
			);
		// when entered wait
		if( fWentIntoWait != 0 )
		{
			tmpStr = "" + fWentIntoWait;
			tmpXML.addSimpleTextToNewPath(
				"dequeue_information/time_entered_wait", tmpStr
				);
		}


		// Get all the connections and add them
/***
		JDOMHelper connections = ((DPump)fApplication).getQueuesForProcessorAsXML(
			getID()
			);
		tmpXML.addContent( connections );

		// Get any parameters that were passed in
		Element params = getParameterElement();
		if( params != null )
		{
			tmpStr = JDOMHelper.JDOMToString( params, true );
			tmpXML.addSimpleTextToNewPath(
				"parameters", tmpStr
				);
		}
***/
		
		return tmpXML;
	}


	public String getStatusXML_OLD()
	{
		//fIsMonitored = true;

		// The main opening tag
		String lReturnXML = "<processor_status name=\"" + getID() + "\">";

		// Add the general status
		lReturnXML += "<state>" + gStateStrings[fState] + "</state>";

		// Whether it can exit or not
		lReturnXML += "<can_exit>";
		if( canExit() )
			lReturnXML += "1";
		else
			lReturnXML += "0";
		lReturnXML += "</can_exit>";

//  old stuff
//							+ "</state><flags>";
//		switch( fState )
//		{
//			case kWaiting:
//				lReturnXML += "1";
//				break;
//			default:
//				lReturnXML += "0";
//				break;
//		}
//		lReturnXML += "</flags><enqueue_information><number_operations>"

		long currPri = getCachedPriority();
		// Thread.MAX_PRIORITY
		lReturnXML += "<current_priority>" + currPri + "</current_priority>";

		// Set the dequeue info
		lReturnXML += "<dequeue_information>";
		lReturnXML += "<number_operations>" + fDequeueOperations
			+ "</number_operations>";
		if( fExitedWait != 0 )
			lReturnXML += "<time_last_retrieved>" + fExitedWait
						+ "</time_last_retrieved>";
		if( fWentIntoWait != 0 )
			lReturnXML += "<time_entered_wait>" + fWentIntoWait
						+ "</time_entered_wait>";
		lReturnXML += "</dequeue_information>";

		// Set the equeue info
		lReturnXML += "<enqueue_information><number_operations>"
						+ fEnqueueOperations + "</number_operations>";
		if( fTimeLastEnqueued != 0 )
			lReturnXML += "<time_last_operation>" + fTimeLastEnqueued
						+ "</time_last_operation>";
		lReturnXML += "</enqueue_information>";

		// Set processing info
		lReturnXML += "<processing_information><last_wait_time>"
						+ fLastWaitTime + "</last_wait_time><total_wait_time>"
						+ fTotalWaitTime + "</total_wait_time>";
		lReturnXML += "<last_processing_time>" + fLastWorkTime
						+ "</last_processing_time><total_processing_time>"
						+ fTotalProcessingTime + "</total_processing_time>";
		lReturnXML += "</processing_information>";

		// Finish up
		lReturnXML += "</processor_status>";


		// getQueuesForProcessAsXML( getID() );

		return lReturnXML;
	}

	long getCachedPriority()
	{
		return fLastPriority;
	}
	void cacheCurrentPriority()
	{
		Thread lCurrentThread = Thread.currentThread();
		fLastPriority = lCurrentThread.getPriority();
	}

	///////////////////////////////////////////////////////////////////
	//
	// Mark made a good point that we should have an enqueue() and dequeue()
	// within the processor class so that we could track operations for status
	// and debugging purposes.
	//
	///////////////////////////////////////////////////////////////////

	// needs to be public vs protected or default
	// because of ExitProcessor.ExitScanner, some warning about
	// synthetic access or something
	public void _enqueue( Queue inQueueToEnqueue /*, WorkUnit inObject*/ )
	{

/***
		inQueueToEnqueue.enqueue( inObject );

		if( getIsMonitoringOn() )
		{
			cacheCurrentPriority();
			if( ++fEnqueueOperations == Integer.MAX_VALUE )
				fEnqueueOperations = 0;
			fTimeLastEnqueued = new Date().getTime();
		}
***/
	}

	protected /*WorkUnit*/void _dequeue( Queue inQueueToDequeue ) throws InterruptedException
	{
/***
		if( getIsMonitoringOn() )
		{
			cacheCurrentPriority();
			fWentIntoWait = new Date().getTime();
			if( fExitedWait != 0 )
			{
				fLastWorkTime = fWentIntoWait - fExitedWait;
				fTotalProcessingTime += fLastWorkTime;
			}
		};

		fState = kWaiting;

		WorkUnit lWorkUnit = inQueueToDequeue.dequeue();
		fState = kProcessing;

		if( getIsMonitoringOn() )
		{
			cacheCurrentPriority();

			if( ++fDequeueOperations == Integer.MAX_VALUE )
				fDequeueOperations = 0;

			fExitedWait = new Date().getTime();
			fLastWaitTime = fExitedWait - fWentIntoWait;
			fTotalWaitTime += fLastWaitTime;
			fWentIntoWait = 0;
		}
		
		return lWorkUnit;
***/
		// return null;
	}


	public void _subscribeTrigger( String inTriggerName, Queue inTriggerQueue )
	{
	    final String kFName = "subscribeTrigger";
		final boolean _debug = true;

/***
		debugMsg( kFName,
			"Will subscribe to trigger " + inTriggerName
			);
		WorkUnit lWU = null;
		try
		{
			lWU = new WorkUnit();
		}
		catch(Exception e)
		{
			stackTrace( kFName, e, "Unable to create trigger subscribe work unit" );
			fatalErrorMsg( kFName, "Exiting due to exception: " + e );
			System.exit(1);
		}

		// Add the processor ID to the work unit
		Element tmpResults1 = lWU.addNamedField(
			TriggerQueue.PROCESSOR_NAME_TAG,
			getID()
			);
		// Add the name of the trigger we're subscribing to
		Element tmpResults2 = lWU.addNamedField(
			TriggerQueue.SUBSCRIBE_TRIGGERS_TAG,
			inTriggerName
			);

		// Sanity check
		if( tmpResults1 == null || tmpResults2 == null )
			errorMsg( kFName, "Unable to add trigger fields"
				+ " Will enqueue trigger work unit anyway"
				);

		debugMsg( kFName,
			"Sending work unit " + lWU
			);

		enqueue( inTriggerQueue, lWU );
***/
	}
	public void _sendTrigger( String inTriggerName, Queue inTriggerQueue )
	{
	    final String kFName = "sendTrigger";
		debugMsg( kFName,
			"Will subscribe to trigger " + inTriggerName
			);

/***
		WorkUnit lWU = null;
		try
		{
			lWU = new WorkUnit();
		}
		catch(Exception e)
		{
			stackTrace( kFName, e, "Unable to create trigger work unit" );
			return;
		}
		// Add the processor ID to the work unit
		Element tmpResults1 = lWU.addNamedField(
			TriggerQueue.PROCESSOR_NAME_TAG,
			getID()
			);
		// Element tmpResults = lWU.addUserDataElement( DONE_TRIGGER_XML_TEXT );
		Element tmpResults2 = lWU.addNamedField(
			TriggerQueue.TRIGGER_TAG,
			inTriggerName
			);

		if( tmpResults1 == null || tmpResults2 == null )
			errorMsg( kFName, "Unable to add trigger field"
				+ " Will enqueue trigger work unit anyway"
				);

		enqueue( inTriggerQueue, lWU );
***/
	}

	public void _addMyRunpathEntry( /*WorkUnit inWU*/ )
	{
		// inWU.createRunpathEntry( getID() );
	}

	/////////////////////////////////////////////////////////////////////
	//
	// Accessors
	//
	/////////////////////////////////////////////////////////////////////



	public Queue[] getReadQueues()
	{
		return fReadQueueList;
	}

	public Queue[] getWriteQueues()
	{
		return fWriteQueueList;
	}

	public Element getParametersSection()
	{
		return fParamElement;
	}

	public Application getApplication( )
	{
		return fApplication;
	}



	public String getID()

	{

		return fID;

	}

	public Element getParameterElement()
	{
		return fParamElement;
	}

	////////////////////////////////////////////////////////////////
	//
	// Ensure that we can't instantiate a queue directly but, rather,
	// must instantiate one of the subclasses.
	//
	////////////////////////////////////////////////////////////////

	abstract public void run();


	private static void ___Sep__Run_Logging__(){}




	//////////////////////////////////////////////////////////////


	///////////////////////////////////////////////////////////
	//
	//  Logging
	//
	////////////////////////////////////////////////////////////

	// This gets us to the logging object
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	public boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoStatusMsg( this.kClassName(), inFromRoutine );
	}




	public boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( this.kClassName(), inFromRoutine );
	}




	public boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( this.kClassName(), inFromRoutine );
	}




	public boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( this.kClassName(), inFromRoutine );
	}




	public boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( this.kClassName(), inFromRoutine );
	}




	public boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}



	public boolean shouldDoWarningMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoWarningMsg( this.kClassName(), inFromRoutine );
	}




	public boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}

	public boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( this.kClassName(), inFromRoutine,
			inMessage
			);
	}

	public boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( this.kClassName(), inFromRoutine,
			e, optMessage
			);
	}


	////////////////////////////////////////////////////////////////
	//
	// Private member variables.
	//
	////////////////////////////////////////////////////////////////

	private Queue[] fReadQueueList = null;
	private Queue[] fWriteQueueList = null;
	private Queue[] fUsesQueueList = null;
	private Element fParamElement = null;
	private Application fApplication = null;
	private String fID = null;


	//////////////////////////////////////////////////////////////
	//
	// The following variables are used for the monitoring system.
	// Note that most of them are not updated/changed until AFTER
	// the first time that someone calls getStatusXML();
	//
	//////////////////////////////////////////////////////////////



	private int fState = kInitializing;

	private long fLastPriority;

	// ??? mbennett: These seem to be absolute times
	// abs time last woke up
	private long fExitedWait = 0;
	// abs time last went to sleep/wait
	// "how long since I did anything"
	private long fWentIntoWait = 0;

	// I believe these are simple counters
	// counters
	private long fEnqueueOperations = 0;
	private long fDequeueOperations = 0;

	// ??? mbennett durations of time?
	// how long I worked on the most recent work unit
	private long fLastWorkTime = 0;
	// How long I waited, the last time I had to wait
	private long fLastWaitTime = 0;

	// This seems to be an absolute time?
	// Abs time last performed enqueue
	private long fTimeLastEnqueued = 0;
	// For dqueue, look at fExitedWait

	// Cumulative time in ms waiting?
	private long fTotalWaitTime = 0;

	// Cumulative time in ms processing?
	private long fTotalProcessingTime = 0;

}


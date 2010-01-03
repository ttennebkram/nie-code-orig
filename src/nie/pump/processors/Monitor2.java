//package nie.processors;
package nie.pump.processors;

import org.jdom.*;

import java.net.*;
import java.io.*;
import java.util.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;


/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      New Idea Engineering
 * @version 1.0
 */

////////////
//
// Thread to poll the queues.
//
////////////

// TODO: make it respect auto exit, option to stay up
// TODO: self contained default XSLT

class QueueMonitorTask2 implements Runnable
{
    private static final String kClassName = "QueueMonitorTask2";

    public QueueMonitorTask2( Monitor2 inMonitor )
	{
		fMonitor = inMonitor;
	}

	public void run()
	{
		
		final String kFName = "run";
		Thread lMyThread = Thread.currentThread();
		Object lWaitObject = new Object();
		DPump lDPump = fMonitor.getDPump();

		//setStateProcessing();
		// Not derived from processor
		
		try
		{
			while( !lMyThread.interrupted() )
			{

				// Create the top level element
				JDOMHelper lStatsRoot = new JDOMHelper(
					Monitor2.STAT_ROOT_ELEMENT_XML_TEXT, null
					);

				// Add the processor info
				lStatsRoot.addXMLTextToPath(
					Monitor2.STAT_ROOT_ELEMENT,
					Monitor2.STAT_PROCESSOR_ROOT_ELEMENT_XML_TEXT
					);
				Vector lProcessors = lDPump.getProcessorList();
				for( int i = 0; i < lProcessors.size(); i++ )
				{
					Processor lProcessor =
						((DPump.ProcessorEntry)lProcessors.elementAt( i )).getProcessor();
					JDOMHelper lProcessorXML = lProcessor.getStatusXML();
					lStatsRoot.addElementToPath(
						Monitor2.STAT_PROCESSOR_ROOT_ELEMENT,
						lProcessorXML
						);
				}


				// Add the queue info
				lStatsRoot.addXMLTextToPath(
					Monitor2.STAT_ROOT_ELEMENT,
					Monitor2.STAT_QUEUES_ROOT_ELEMENT_XML_TEXT
					);
				for( Enumeration lIterator = lDPump.getQueues().elements();
					 lIterator.hasMoreElements();
					 )
				{
					Queue lQueue = (Queue)lIterator.nextElement();
					JDOMHelper lQueueXML = lQueue.getStatusXML();
					lStatsRoot.addElementToPath(
						Monitor2.STAT_QUEUES_ROOT_ELEMENT,
						lQueueXML
						);
				}


				// Save the results
				fMonitor.setStats( lStatsRoot );

				synchronized( lWaitObject )
				{
					lWaitObject.wait( fMonitor.getPollInterval() );
				}
			}

		}
		catch( InterruptedException ie )
		{
			//System.out.println( "Got an InterruptedException." );
		}
		catch( JDOMHelperException jhe )
		{
			// System.err.println( "QueueMonitorTask2::JDOMHelper exception!" );
			// mWorkUnit.errorMsg( this, kFName, "JDOMHelper exception!" );
			stackTrace( kFName, jhe, "JDOMHelper exception" );
			// System.err.println( jhe + "\n\n" );
		}

		catch( Exception e )
		{
			// Until further notice, ASSUME that this is being thrown
			// by the JDOMHelper constructor.
			// System.err.println( e );
			// mWorkUnit.errorMsg( this, kFName, "Got Exception:" + e );
			stackTrace( kFName, e, "General Exception" );
		
		}
	};

	public boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	Monitor2 fMonitor = null;
}



////////////
//
// Thread to wait on the server port and instantiate handlers whenever a request comes in.
//
////////////

class QueueMonitorServer2 implements Runnable
{
    private static final String kClassName = "QueueMonitorServer2";

	public QueueMonitorServer2( int inSocketNumber, Monitor2 inMonitor )
	{
		fSocketNumber = inSocketNumber;
		fMonitor = inMonitor;
	}

	public void run()
	{
		
		final String kFName = "run";
		ServerSocket lServerSocket = null;
		Thread lMyThread = Thread.currentThread();

		try
		{
			lServerSocket = new ServerSocket( fSocketNumber );

			while( !lMyThread.interrupted() )
			{
				Socket lSocket = lServerSocket.accept();
				if( !lMyThread.interrupted() )
				{
					QueueMonitorHandler2 lHandler =
						new QueueMonitorHandler2( lSocket, fMonitor );
					Thread lHandlerThread = new Thread( lHandler );
					if( lHandlerThread != null )
						lHandlerThread.start();
				}
			}
		}
		catch( IOException ioe )
		{
			fatalErrorMsg( kFName, "IO Exception on socket at port " + fSocketNumber + ", Exception: " + ioe );
			stackTrace( kFName, ioe, "IO Exception on socket at port " + fSocketNumber );
			System.exit( -1 );
		}
	}

	public boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	public boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}


	int fSocketNumber = 80;
	Monitor2 fMonitor = null;
}



////////////
//
// Thread to handle HTTP requests for information.
//
////////////

class QueueMonitorHandler2 implements Runnable
{
    private static final String kClassName = "QueueMonitorHandler2";

    public QueueMonitorHandler2( Socket inSocket, Monitor2 inMonitor )
	{
		fSocket = inSocket;
		fMonitor = inMonitor;
	}


	private void __Answers_Web_Requests__() {};

	public void run()
	{
		final String kFName = "run";
		// final boolean debug = true;

		try
		{
//			InputStream lInputStream = fSocket.getInputStream();
//			boolean isDoneReadingRequest = false;
//			boolean didFindEOL = false;
//
//			StringBuffer buffer = new StringBuffer();
//			List lines = new Vector();
//
//			while( !isDoneReadingRequest )
//			{
//
//				/////
//				//
//				// Wait for all the headers to come in so that
//				// browsers don't have a conniption.
//				//
//				/////
//
//				int lChar = lInputStream.read();
//
//				if( lChar != '\r' )
//				{
//					if( lChar == '\n' )
//					{
//						if( !didFindEOL )
//						{
//							didFindEOL = true;
//							if( buffer.length() > 0 )
//							{
//								lines.add( new String(buffer) );
//								buffer = new StringBuffer();
//							}
//						}
//						else
//							isDoneReadingRequest = true;
//					}
//					else
//					{
//						didFindEOL = false;
//						buffer.append( (char)lChar );
//					}
//				}   // End if not \r
//			}   // End while ! isDoneReadingRequest
//
//			if(debug)
//			{
//				System.err.println( "Debug: Monitor:"
//					+ " Got " + lines.size() + " header lines"
//					);
//				for( Iterator it = lines.iterator(); it.hasNext(); )
//				{
//					String line = (String) it.next();
//					System.err.println( "\t" + line );
//				}
//			}
//
//			Hashtable cgiVars = null;
//			if( lines.size() > 0 )
//			{
//				String pathSection = NIEUtil.cgiParsePathSectionFromCGIHeader(
//					(String)lines.get(0)
//					);
//				if(debug) System.err.println( "Debug: Monitor:"
//					+ " Path section is '" + pathSection + "'"
//					);
//
//				cgiVars = NIEUtil.cgiParseGETVariablesFromCGIHeader(
//					(String)lines.get(0)
//					);
//			}

			AuxIOInfo lRequestInfo = NIEUtil.readHTTPRequestFromSocket( fSocket, false, false );

			String lOutputContent;
			boolean isXML = true;

			// lOutputContent = fMonitor.getStatsFormatted( cgiVars );

			Map vars = lRequestInfo.getCgiVarsHash();
			Hashtable newVars = new Hashtable();
			for( Iterator keys = vars.keySet().iterator() ; keys.hasNext() ; ) {
				String key = (String) keys.next();
				Object valObj = vars.get( key );
				if( valObj instanceof String ) {
					newVars.put( key, (String)valObj );
				}
				else if( valObj instanceof Collection ) {
					Collection coll = (Collection) valObj;
					StringBuffer buff = new StringBuffer();
					for( Iterator cit = coll.iterator(); cit.hasNext() ; ) {
						String val = (String) cit.next();
						if( buff.length() > 0 )
							buff.append( ' ' );
						buff.append( val );	
					}
					if( buff.length() > 0 )
						newVars.put( key, new String(buff) );
				}
			}

			// lOutputContent = fMonitor.getStatsFormatted( (Hashtable)lRequestInfo.getCgiVarsHash() );
			lOutputContent = fMonitor.getStatsFormatted( newVars );
			if( lOutputContent != null )
			{
				debugMsg( kFName, "got formatted output" );
				isXML = false;
			}
			else
			{
			    debugMsg( kFName, "no formatted output" );
				isXML = true;
				JDOMHelper lStatsRoot = fMonitor.getStats();
				OutputStream lOutputStream = fSocket.getOutputStream();
				lOutputStream.write( PREAMBLE.getBytes() );

				if( lStatsRoot != null )
					lOutputContent = lStatsRoot.JDOMToString( true );
					//lOutputContent = lStatsRoot.JDOMToString();
			}
			// Backup plan
			if( lOutputContent == null )
			{
				lOutputContent = "<status>no status available</status>";
				// System.err.println( "Monitor: no status available?" );
				errorMsg( kFName, "no status available?" );
			}

			// Open up the output
			OutputStream lOutputStream = fSocket.getOutputStream();
			// Output the header stuff
			if( isXML )
				lOutputStream.write( PREAMBLE.getBytes() );
			else
				lOutputStream.write( PREAMBLE_HTML.getBytes() );

			// The content length
			lOutputStream.write( Integer.toString( lOutputContent.length() ).getBytes() );
			// Finish the header
			lOutputStream.write( "\n\n".getBytes() );
			// And now the actual data
			lOutputStream.write( lOutputContent.getBytes() );
			// And then close it all down
			lOutputStream.flush();
			fSocket.shutdownOutput();
			fSocket.shutdownInput();
			fSocket.close();
		}
		catch( IOException ioe )
		{
			stackTrace( kFName, ioe, "Non-fatal IO Exception (2)" );
		}
		catch( SpuriousHTTPRequestException se )
		{
			// System.err.println( "Spurious request: " + se );
			errorMsg( kFName, "Spurious request: " + se );
		}
	}





	public void _runOBS()
	{

		final String kFName = "_runOBS";
		// final boolean debug = true;

		try
		{
			InputStream lInputStream = fSocket.getInputStream();
			boolean isDoneReadingRequest = false;
			boolean didFindEOL = false;

			StringBuffer buffer = new StringBuffer();
			List lines = new Vector();

			while( !isDoneReadingRequest )
			{

				/////
				//
				// Wait for all the headers to come in so that
				// browsers don't have a conniption.
				//
				/////

				int lChar = lInputStream.read();

				if( lChar != '\r' )
				{
					if( lChar == '\n' )
					{
						if( !didFindEOL )
						{
							didFindEOL = true;
							if( buffer.length() > 0 )
							{
								lines.add( new String(buffer) );
								buffer = new StringBuffer();
							}
						}
						else
							isDoneReadingRequest = true;
					}
					else
					{
						didFindEOL = false;
						buffer.append( (char)lChar );
					}
				}   // End if not \r
			}   // End while ! isDoneReadingRequest

			// if(debug)
			// {
				debugMsg( kFName,
				        "Got " + lines.size() + " header lines"
					);
				// for( Iterator it = lines.iterator(); it.hasNext(); )
				// {
				//	String line = (String) it.next();
				//	System.err.println( "\t" + line );
				// }
			// }

			Hashtable cgiVars = null;
			if( lines.size() > 0 )
			{
				String pathSection = NIEUtil.cgiParsePathSectionFromCGIHeader(
					(String)lines.get(0)
					);
				debugMsg( kFName,
					"Path section is '" + pathSection + "'"
					);

				// cgiVars = NIEUtil.cgiParseGETVariablesFromCGIHeader(
				//	(String)lines.get(0)
				//	);
			}

			String lOutputContent;
			boolean isXML = true;

			lOutputContent = fMonitor.getStatsFormatted( cgiVars );
			if( lOutputContent != null )
			{
				debugMsg( kFName, "got formatted output" );
				isXML = false;
			}
			else
			{
				debugMsg( kFName, "no formatted output" );
				isXML = true;
				JDOMHelper lStatsRoot = fMonitor.getStats();
				OutputStream lOutputStream = fSocket.getOutputStream();
				lOutputStream.write( PREAMBLE.getBytes() );

				if( lStatsRoot != null )
					lOutputContent = lStatsRoot.JDOMToString( true );
					//lOutputContent = lStatsRoot.JDOMToString();
			}
			// Backup plan
			if( lOutputContent == null )
			{
				lOutputContent = "<status>no status available</status>";
				// System.err.println( "Monitor: no status available?" );
				errorMsg( kFName, "No status available?" );
			}

			// Open up the output
			OutputStream lOutputStream = fSocket.getOutputStream();
			// Output the header stuff
			if( isXML )
				lOutputStream.write( PREAMBLE.getBytes() );
			else
				lOutputStream.write( PREAMBLE_HTML.getBytes() );

			// The content length
			lOutputStream.write( Integer.toString( lOutputContent.length() ).getBytes() );
			// Finish the header
			lOutputStream.write( "\n\n".getBytes() );
			// And now the actual data
			lOutputStream.write( lOutputContent.getBytes() );
			// And then close it all down
			lOutputStream.flush();
			fSocket.shutdownOutput();
			fSocket.shutdownInput();
			fSocket.close();
		}
		catch( IOException ioe )
		{
			stackTrace( kFName, ioe, "Non-fatal IO Exception (1)" );
		}
	}

	public boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	public boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	public boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName, inFromRoutine,
			e, optMessage
			);
	}
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	Socket fSocket = null;
	Monitor2 fMonitor = null;

	static final String PREAMBLE =
		"HTTP/1.0 200 OK\n"
		+ "Server: DPump Monitor 1.0\n"
		+ "Content-Type: text/xml\n"
		+ "Content-Length: ";
	static final String PREAMBLE_HTML =
		"HTTP/1.0 200 OK\n"
		+ "Server: DPump Monitor 1.0\n"
		+ "Content-Type: text/html\n"
		+ "Content-Length: ";

}



////////////
//
// Instantiable DPump processor class.
//
// This class instantiates two thread.  one thread polls the queues for their
// current load.  The other thread handles the port.
//
// This class also serves as the repository for data collected by the queue poller
// for dissemination by the port handler.
//
////////////

public class Monitor2 extends Processor
{
	public String kClassName() { return "Monitor2"; }


	public Monitor2( Application inApplication,
					Queue[] inReadQueue,
					Queue[] inWriteQueue,
					Queue[] inUsesQueue,
					Element inParameter,
					String inID )
	{

		super( inApplication, inReadQueue, inWriteQueue, inUsesQueue, inParameter, inID );
		final String kFName = "constructor";

		debugMsg( kFName,
			"parameters=" + inParameter
			);
		if( null != inParameter )
			debugMsg( kFName, "Contents:"
					+ JDOMHelper.JDOMToString( inParameter )
					);

		// Set the global flag that, YES, we will be doing monitoring
		inApplication.setMonitoringOn();

		fQueueInfo = null;
		fQueueInfoFormatted = null;
		fDPump = (DPump) inApplication;
//		String lParameterString = JDOMHelper.getTextByPath( inParameter, INTERVAL_PATH );
//		if( lParameterString != null )
//			fPollInterval = Integer.parseInt( lParameterString );
		fPollInterval = JDOMHelper.getIntFromPathText( inParameter,
			INTERVAL_PATH, DEFAULT_INTERVAL
			);
		debugMsg( kFName, "fPollInterval='"
			+ fPollInterval + "'"
			);

//		lParameterString = JDOMHelper.getTextByPath( inParameter, PORT_PATH );
//		if( lParameterString != null )
//			fPort = Integer.parseInt( lParameterString );
//		else
//			fPort = 80;

		fPort = JDOMHelper.getIntFromPathText( inParameter,
			PORT_PATH, DEFAULT_PORT
			);
		debugMsg( kFName, "fPort='"
			+ fPort + "'"
			);


		fXsltPath = JDOMHelper.getTextByPath( inParameter, XSLT_PATH );
		debugMsg( kFName, "xslt='" + fXsltPath + "'" );
		if( fXsltPath != null && fXsltPath.trim().equals("") )
			fXsltPath = null;

	}

	public void run()
	{

		QueueMonitorTask2 lMonitorTask = new QueueMonitorTask2( this );
		Thread lThread = new Thread( lMonitorTask );
		lThread.setDaemon( true );
		lThread.start();


		QueueMonitorServer2 lMonitorServer = new QueueMonitorServer2( fPort, this );
		lThread = new Thread( lMonitorServer );
		lThread.setDaemon( true );
		lThread.start();



		/**@todo: implement this nie.core.Processor abstract method*/


		setStateProcessing();

	}

	public boolean canExit()
	{
		return true;
	}


	public int getPollInterval()
	{
		return fPollInterval;
	}

	public DPump getDPump()
	{
		return fDPump;
	}

	/////////////////////////////////////////////////////////
	private void __XML_and_HTML_Status__() {}

	public synchronized void setStats( JDOMHelper inStatsRoot )
	{

		// final boolean debug = false;

		// Whatever we did above, do save the jdom tree
		fQueueInfo = inStatsRoot;


	}

	public synchronized String calculateStatsFormatted()
	{
		return calculateStatsFormatted( null );
	}
	public synchronized String calculateStatsFormatted( Hashtable inParamsHash )
	{

		final String kFName = "calculateStatsFormatted";
		// System.err.println( "inParamsHash=" + inParamsHash );

		String outBuffer = null;

		if( fXsltPath != null )
		{
		    debugMsg( kFName,
		    	"xslt path=" + fXsltPath
				);
			JDOMHelper lXMLStats = getStats();

			if( null != lXMLStats )
			{

			    debugMsg( kFName,
					"lXMLStats = '" + lXMLStats
					);
				try
				{

					//Document tmpDoc = inStatsRoot.xsltElementToDoc( fXsltPath );
					Document tmpDoc = lXMLStats.xsltElementToDoc(
						fXsltPath,
						inParamsHash
						);

					//outBuffer = JDOMHelper.JDOMToString( tmpDoc );
					// Preseve linefeeds in text nodes by setting
					// PrettyFormat
					outBuffer = JDOMHelper.JDOMToString( tmpDoc, true );
					// System.err.println( "Outbuffer=" + outBuffer );
					setStatsFormattedCache( outBuffer );
				}
				catch (Exception e)
				{
					// System.err.println( "Error:Monitor:setStats:"
					//	+ " Unable to convert jdom status tree into formatted string"
					//	+ " for style sheet '" + fXsltPath + "'"
					//	+ ", so will not store formatted status (just raw xml)"
					//	+ ", Exception: " + e
					//	);
					stackTrace( kFName, e,
					        "Unable to convert jdom status tree into formatted string"
							+ " for style sheet '" + fXsltPath + "'"
							+ ", so will not store formatted status (just raw xml)"
							);
				}
			}
			// Else null stats
			else
			    errorMsg( kFName,
					"XML status was null"
					);
		}
		// Else no XSLT path
		else
		    errorMsg( kFName,
				"xslt was null"
				);

		return outBuffer;
	}

	public synchronized void setStatsFormattedCache( String inNewStats )
	{
		final String kFName = "setStatsFormattedCache";
		if( inNewStats != null && ! inNewStats.trim().equals("") )
			fQueueInfoFormatted = inNewStats;
		else
		{
			// System.err.println( "Error: Monitor: setStatusFormatted:"
			//	+ " Was passed a null or empty String, ignoring"
			//	);
			errorMsg( kFName, "Was passed a null or empty string, ignoring" );
		}
	}

	public synchronized JDOMHelper getStats()
	{
		return fQueueInfo;
	}
	public synchronized String getStatsFormatted( Hashtable inParamsHash )
	{
		// We IGNORE the cache here
		return calculateStatsFormatted( inParamsHash );
	}
	public synchronized String getStatsFormatted()
	{
		// We IGNORE the cache here
		return calculateStatsFormatted();
	}
	public synchronized String getStatsFormattedCashed()
	{
		return fQueueInfoFormatted;
	}
	

	JDOMHelper fQueueInfo = null;
	String fQueueInfoFormatted = null;
	DPump fDPump = null;
	String fXsltPath;
	int fPollInterval = DEFAULT_INTERVAL;
	int fPort = DEFAULT_PORT;


	static final String STAT_ROOT_ELEMENT_XML_TEXT = "<status />";
	static final String STAT_ROOT_ELEMENT = "/status";


	static final String STAT_QUEUES_ROOT_ELEMENT = "/status/queues";
	static final String STAT_QUEUES_ROOT_ELEMENT_XML_TEXT = "<queues />";


	static final String STAT_PROCESSOR_ROOT_ELEMENT = "/status/processors";
	static final String STAT_PROCESSOR_ROOT_ELEMENT_XML_TEXT = "<processors />";
	
	static final String QUEUE_TAG = "queue";
	static final String NAME_TAG = "name";
	static final String DEQUEUE_OPERATIONS_TAG = "dequeue_operations";
	static final String ENQUEUE_OPERATIONS_TAG = "enqueue_operations";
	static final String TIME_LAST_DEQUEUE_TAG = "last_dequeue";
	static final String TIME_LAST_ENQUEUE_TAG = "last_enqueue";
	static final String QUEUE_SIZE_TAG = "size";
	static final String INTERVAL_PATH = "interval";
	static final int DEFAULT_INTERVAL = 1000;
	static final String PORT_PATH = "port";
	static final int DEFAULT_PORT = 80;
	static final String XSLT_PATH = "xslt";
	static final String ABSOLUTE_TIME_ATTRIBUTE = "absolute_time";
	static final String RELATIVE_TIME_ATTRIBUTE = "relative_time";
	// Not used here, we don't output work units, so nothing to hang
	// errors on
	WorkUnit _mWorkUnit;
}


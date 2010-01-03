/*
 *  Copyright 2001 New Idea Engineering, Inc.
 *  Written by Mark Bennett
 *
 *  Read in a file and send work units to a queue.

	This version understands triggers
	It is a trigger producer
 *
 */

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

//import Processor;
//import Application;
//import Queue;
//import RetrievedURL;

import java.io.*;
import java.net.*;
import java.util.*;
import org.jdom.Element;

public class CSVInT extends Processor
{
	public String kClassName() { return "CSVInT"; }


	private void __sep__Constructor__() {}
	//////////////////////////////////////////////////////////////////

	public CSVInT( Application inApplication,
		  Queue[] inReadFromQueue,
		  Queue[] inWriteToQueue,
		  Queue[] inUsesQueue,
		  Element inParameters,
		  String inProcID
		  )
	{
		super( inApplication, inReadFromQueue, inWriteToQueue,
		  inUsesQueue, inParameters, inProcID
		  );

		final String kFName = "constructor";
		if( inReadFromQueue != null )
		{
			// System.err.println( "CSVInT: does not accept inputs" );
			fatalErrorMsg( kFName, "does not accept inputs" );
			System.exit(1);
		}

		if( inWriteToQueue != null && inWriteToQueue[0] != null )
			fWriteQueue = inWriteToQueue[0];
		else
		{
			// System.err.println( "Input: No queue to write to." );
			fatalErrorMsg( kFName, "No queue to write to." );
			System.exit( -1 );
		}

		if( inUsesQueue != null && inUsesQueue[0] != null )
			fUsesQueue = inUsesQueue[0];
		else
		{
			// System.err.println( "Uses: No uses queue to trigger to." );
			fatalErrorMsg( kFName, "No uses queue to trigger to." );
			System.exit( -1 );
		}

		// If parameters were sent in, save them
		if( inParameters != null )
		{
			try
			{
				fJdh = new JDOMHelper( inParameters );
			}
			catch (Exception e)
			{
				fJdh = null;
				// System.err.println(
				//	"Error creating jdom helper for parameters\n" + e
				//	);
				stackTrace( kFName, e, "Error processing XML parameters" );
				fatalErrorMsg( kFName,
						"Shutting down (1) due to exception: " + e
						);
				System.exit(1);
			}

		}

		//System.err.println( "Input successfully instantiated." );

	}

	/////////////////////////////////////////////
	//
	// And this is where the work gets done.
	//
	/////////////////////////////////////////////
	private void __sep__Main_Logic__ () {}
	///////////////////////////////////////////////////////////////////////

	public void run()
	{
		
		final String kFName = "run";
		// final boolean debug = false;

		// System.out.println( "Debug: CSVInT: run: Start." );
		debugMsg( kFName, "Start." );

		setStateProcessing();

		try {
			doOpen();
		}
		catch( Exception e ) {
			// System.err.println( "can't open" );
			// errorMsg( kFName, "can't open" );
			//System.err.println( e );
			// errorMsg( kFName, "Got Exception:" + e );
			stackTrace( kFName, e, "Got Exception opening CSV File" );
			fatalErrorMsg( kFName, "Shutting down (2) due to exception:" + e );
			System.exit( -1 );
		}


		while( true )
		{
			// WorkUnit record = null;
		    mWorkUnit = null;
		    try
			{
		        mWorkUnit = getNextRecord();
			}
			catch( Exception e )
			{
				close();
				// System.err.println( "error reading record." );
				// errorMsg( kFName, "error reading record." );
				// System.err.println( e );
				// errorMsg( kFName, "Got Exception:" + e );
				stackTrace( kFName, e , "Exception fetching next CSV record" );
				fatalErrorMsg( kFName, "Shutting down (3) due to exception: " + e );
				System.exit( -1 );
			}

			if( mWorkUnit != null )
			{

				// System.out.println( "Debug: CSVInT: Got a record." );
				debugMsg( kFName, "Got a record." );

				//System.out.println( "Got a record." );
				enqueue( fWriteQueue, mWorkUnit );
				mWorkUnit = null;
			}
			else
			{
				// System.out.println( "Debug: CSVInT: null record, done." );
				debugMsg( kFName, "null record, done." );
				break;
			}

			fCounter ++;
			if( fCounter % kReportInterval == 0 )
			{
				// System.out.println( "CSVIn: Processed '" + fCounter +
				//	"' records." );
				infoMsg( kFName, "Processed '" + fCounter +
					"' records." );
			}
		}

		close();

		// System.out.println( "Debug: CSVInT: run: sending done trigger." );
		debugMsg( kFName, "sending done trigger." );
		sendDoneTrigger();

		// Tell the world we're ready to exit
		// fState = kWaiting;
		setExitOK();
		setStateFinished();

		// System.out.println( "Debug: CSVInT: run: End." );
		debugMsg( kFName, "End." );

		//System.err.println( "Shutting down Input processor." );
	}

	void setExitOK()
	{
		fCanExit = true;
	}

	public boolean canExit()
	{
		return fCanExit;
	}

	void sendDoneTrigger()
	{
		final String kFName = "sendDoneTrigger";
		// System.out.println( "Debug: CSVInT: sendDoneTrigger: Start." );
		debugMsg( kFName, "Start." );
		sendTrigger( TriggerQueue.DONE_TRIGGER_TEXT,
			fUsesQueue
			);
	}

	private void __sep__Simple_Get_and_Set_Logic__ () {}
	////////////////////////////////////////////////////////

	//public String getDataFormat() {
	//	return "csv";
	//}

	public String getLocation()
	{
		final String kFName = "getLocation";
//		if( fJdh != null )
//			return fJdh.getTextByPath("location");
//		else
//			return null;
		String loc = fJdh.getStringFromAttribute( "location" );
		if( loc == null || loc.trim().equals("") )
		{
			// System.err.println( "Error: CSVIn: location is a required attribute" );
			fatalErrorMsg( kFName, "location is a required attribute" );
			System.exit(-1);
		}
		return loc;
	}

	boolean getForceFreeForm()
	{
//		// Look for a <free_form/> tag
//		Element tmp = fJdh.findElementByPath(
//			"free_form"
//			);
//		// It's being backwashed if we DID find the tag.
//		return tmp != null;
		return fJdh.getBooleanFromAttribute( "free_form", false );
	}

	List getFieldMap()
	{
		return fJdh.getListFromAttribute( FIELD_MAP_NAME );
	}


	boolean getHasFieldMap()
	{
		List fields = getFieldMap();
		// It's fine if there's no field map
		return null!=fields && ! fields.isEmpty();
	}

	// Returns NULL if there's no map for this index
	String getFieldMapNameN( int ordinal )
	{
		final String kFName = "getFieldMapNameN";
		if( ordinal < 1 )
		{
			// System.err.println( "Error: CSVIn: lookup of mapped names"
			//	+ "was given an invalid index of '" +
			//	ordinal + "', must be a positive integer"
			//	);
			
			String tmpMsg = "lookup of mapped names"
				+ "was given an invalid index of '" +
				ordinal + "', must be a positive integer"
				;
			if( null!=mWorkUnit )
			    mWorkUnit.errorMsg( this, kFName, tmpMsg );
			else
			    errorMsg( kFName, tmpMsg );
			return null;
		}

		if( ! getHasFieldMap() ) {
			errorMsg( kFName, "No active field map." );
			return null;
		}

		List fields = getFieldMap();
		// It's fine if there's no field map
		// if( fields == null || fields.size() < 1 )
		//	return null;
		if( ordinal > fields.size() )
		{
			// System.err.println( "Warning: CSVIn: lookup of mapped name for index '" +
			//	ordinal + "' is past end of field map which only has '" +
			//	fields.size() + "' entries"
			//	);
			String tmpMsg = "lookup of mapped name for index '" +
				ordinal + "' is past end of field map which only has '" +
				fields.size() + "' entries"
				;
			if( null!=mWorkUnit )
			    mWorkUnit.errorMsg( this, kFName, tmpMsg );
			else
			    errorMsg( kFName, tmpMsg );
			return null;
			
		}
	
		return (String)fields.get( ordinal - 1 );
		
	}

	/***************************************************
	*
	*	Base Stream Operations
	*
	*****************************************************/
	private void __sep__Basic_Stream_Operators__ () {}
	////////////////////////////////////////////////////////

	private void doOpen() throws Exception
	{
		final String kFName = "doOpen";
		// Bail if it's already open
		if(  inStream != null )
			return;

		// System.out.println( "\twill open." );
		debugMsg( kFName, "will open." );

		String loc = getLocation();
		if( loc == null || loc.trim().equals("") )
		{
			throw new Exception( "got a null location attribute" );
		}

		loc = loc.trim();

		// Is it a URL?
		if( loc.startsWith("http://") ||
			loc.startsWith("ftp://")
			)
		{
				// System.out.println( "\tSeems to be a URL." );
				debugMsg( kFName, "Seems to be a URL." );

				// From O'Reily Networking pg 210
				URL url = new URL( loc );
				inStream = url.openStream();
				// BufferedReader has readLine()
				// LineNumberReader has readLine AND getLineNumber
				reader = new LineNumberReader(
					new InputStreamReader( inStream ), 2048 );

		}
		// Else assume it's a file
		else
		{
				// System.out.println( "\tSeems to be a File, opening for reading" );
				debugMsg( kFName, "Seems to be a File, opening for reading" );

				File theFile = null;
				try
				{
					theFile = new File(
						System.getProperty( "user.dir" ), loc
						);
					inStream = new FileInputStream( theFile );
				}
				catch (Exception e)
				{
					theFile = new File( loc );
					inStream = new FileInputStream( theFile );
				}

				reader = new LineNumberReader(
					new InputStreamReader( inStream ), 2048 );

		} // end Else it's a normal stream
	}

	private String readNextBlock() throws IOException {
		String buffer = null;
		buffer = ((BufferedReader)reader).readLine();
		return buffer;
	}


	public void close() {
		// Todo: See if we need to flush anything

		if( reader != null ) {
			try { reader.close(); } catch (Exception e) {}
			reader = null;
		}
		if( inStream != null ) {
			try { inStream.close(); } catch (Exception e) {}
			inStream = null;
		}
	}


	/***************************************************
	*
	*	Simple Process Logic
	*
	****************************************************/
	private void __sep__Simple_Logic__() {}
	////////////////////////////////////////////////////////////////

	private WorkUnit parseCSVLine( String inputString )
		throws ExceptionPrematureEndOfData
	{
		return parseCSVLine( inputString, false );
	}

	// Given a line from a CSV file, return a work unit or null
	private WorkUnit parseCSVLine( String inputString, boolean inForceOutputRecord )
		throws ExceptionPrematureEndOfData
	{
		final String kFName = "parseCSVLine";
		// Todo: Obsess over edge cases, null fields, spaces
		// before an opening field quote, etc.
		boolean trace = shouldDoTraceMsg( kFName );
		// boolean debug = false;

		WorkUnit returnRecord = null;
		try
		{
			returnRecord = new WorkUnit();
		}
		catch (Exception e)
		{
			close();
			// System.err.println( "Error creating WorkUnit" );
			// errorMsg( kFName, "Error creating WorkUnit" );
			// System.err.println( e );
			// errorMsg( kFName, "Got Exception:" + e );
			stackTrace( kFName, e, "Got Exception parsing CSV" );
			fatalErrorMsg( kFName, "Shutting down (4) due to exception: " + e );
			System.exit( 1 );
		}

		// If we need freeform
		if( getForceFreeForm() )
			returnRecord.forceUserDataToFreeForm();

		StringBuffer buff1 = new StringBuffer( inputString );
		StringBuffer buff2 = null;
		boolean isInField = false;
		boolean isQuotedField = false;
		char c;
		int fieldCounter = 0;

		for( int i=0; i<buff1.length(); i++ )
		{
			c = buff1.charAt(i);

			if(trace) traceMsg( kFName, "Char "+i+" is '"+c+"'");

			// if NOT inside a field
			if( ! isInField )
			{
				// Check for null fields, keep them
				if( c == ',' )
				{
					fieldCounter++;
					//returnRecord.addAnonymousField("");
					if( ! registerAField( returnRecord, "", fieldCounter ) )
					{
						errorMsg( kFName, "Error fully recording field # " + fieldCounter + " for record '" + inputString + "'" );
					}
				}
				// Else not a null field, start recording
				else
				{
					buff2 = new StringBuffer();
					isInField = true;
					if( c == '"' )
						isQuotedField = true;
					else
						buff2.append(c);
				}
			}
			// Else we are in a field
			else {
				// Do we have a comma?
				if( c == ',' ) {
					// If it's in a quoted field, keep it
					if( isQuotedField )
						buff2.append(c);
					// Not in quotes, so it's the end
					// of a field
					else
					{
						fieldCounter++;
						//returnRecord.addAnonymousField( new String(buff2) );
						if( ! registerAField( returnRecord, new String(buff2), fieldCounter ) )
							errorMsg( kFName, "Error (2) fully recording field # " + fieldCounter + " for record '" + inputString + "'" );
						// Adjust states and pointers
						buff2 = null;
						isInField = false;
					}
				}
				// Is it a quote?
				else if( c == '"' ) {
					// Are we in a quoted field?
					if( isQuotedField )
						// Is it an escaped quote
						if( i < buff1.length()-1 &&
							buff1.charAt(i+1)=='"'
						) {
							// Keep the quote
							buff2.append('"');
							// Skip the second quote
							i++;
						}
						// Else it IS an ending quote
						else
						{
							// Skip that char, of course
							// Then create the field
							fieldCounter++;
							//returnRecord.addAnonymousField( new String(buff2) );
							if( ! registerAField( returnRecord, new String(buff2), fieldCounter ) )
								errorMsg( kFName, "Error (3) fully recording field # " + fieldCounter + " for record '" + inputString + "'" );
							// Make sure we don't add it again
							buff2 = null;
							// Fix states
							isInField = false;
							isQuotedField = false;
							// Eat chars until we see a comma
							for( i=i+1; i<buff1.length(); i++ )
								if( buff1.charAt(i)==',' )
									break;
						}
					// else we're not in a quoted field
					else
						// Odd, but go ahead and keep it
						buff2.append('"');
				}
				// Else it's some other character
				else
					// Keep it
					buff2.append(c);

			} // End Else we're in a field

		} // End while loop

		// inForceOutputRecord ) throws ExceptionPrematureEndOfData
		
		// Double check that, when got to the end of the buffer
		// we may have been in the middle of a field
		if( isInField )
		{
			// Check for newlines inside of quoted fields
			if( isQuotedField && ! inForceOutputRecord )
				throw new ExceptionPrematureEndOfData( "Missing end quote" );
			// Otherwise we're OK...
			if( buff2 != null )
			{
				fieldCounter++;
				//returnRecord.addAnonymousField( new String(buff2) );
				if( ! registerAField( returnRecord, new String(buff2), fieldCounter ) )
					errorMsg( kFName, "Error (4) fully recording field # " + fieldCounter + " for record '" + inputString + "'" );
			}
		}

		return returnRecord;
	}


	/*void*/ boolean registerAField( WorkUnit wu, String contents, int fieldCount )
	{
		final String kFName = "registerAField";
		String tmpFieldName = null;
		if( getHasFieldMap() )
		{
			tmpFieldName = getFieldMapNameN( fieldCount );
			if( tmpFieldName == null )
			{
				String msg = "No field name found, will add as anonymous."
					+ " count=" + fieldCount
					+ ", value='" + contents + "'"
					;
				warningMsg( kFName, msg );
				wu.addAnonymousField( contents );
				wu.setIsValidRecord( false );
				wu.errorMsg( this, kFName, msg );
				return false;
			}
			else {
				wu.addNamedField( tmpFieldName, contents );
				return true;
			}
		}
		// Else no field map
		else {
			wu.addAnonymousField( contents );
			return true;
		}
	}

	/***************************************************
	*
	*	Higher Level Process Logic
	*
	****************************************************/


	public WorkUnit getNextRecord()
		// throws Exception
		throws IOException
	{
		final String kFName = "getNextRecord";

		// form an element
		WorkUnit record = null;
		StringBuffer mainBuffer = new StringBuffer();
		while( true )
		{
			String lineeBuffer = readNextBlock();
			if( lineeBuffer == null )
			{
				// System.out.println( "Debug: StreamTag:getNextRecord: end of file" );
				debugMsg( kFName, "end of file" );
				// Double check, were we in the middle of handling a truncated record?
				if( mainBuffer.length() > 0 )
				{
					try {
						record = parseCSVLine( new String(mainBuffer), true );
					}
					catch( ExceptionPrematureEndOfData e )
					{
						// warningMsg( kFName, "Unable to force parsing of possibly truncated CSV record, and no more lines to read." );
						// return null;
						// This is pretty weird, should make a louder complaint
						throw new IOException( "Unable to force parsing of possibly truncated CSV record, and no more lines to read." );
					}
					
					// Stamp the runpath of the work unit
					addMyRunpathEntry( record );
				}
				// Else we're fine, no pending data
				else {
					return null;
				}
			}
			// In case we're adding to a partial record
			// delimit with a newline
			if( mainBuffer.length() > 0 )
				mainBuffer.append(NIEUtil.NL);
			// Add the data we got this time through
			mainBuffer.append( lineeBuffer );
			// Try to parse the record
			try
			{
				record = parseCSVLine( new String(mainBuffer), false );
			}
			// Maybe truncated, but there might be more data
			catch( ExceptionPrematureEndOfData e )
			{
				warningMsg( kFName, "Premature end of CSV record, will try reading another line."
					+ "  Current buffer = '" + mainBuffer + "'"
					);
				// Go back to top of loop and try to get another line of data
				continue;
			}
			// Else we're OK, so break
			break;
		}

		// Stamp the runpath of the work unit
		addMyRunpathEntry( record );

		// return
		return record;
	}

	public WorkUnit getNextRecord_v1()
		throws Exception
	{
		final String kFName = "getNextRecord";
		String buffer = readNextBlock();
	
		if( buffer == null )
		{
			// System.out.println( "Debug: StreamTag:getNextRecord: end of file" );
			debugMsg( kFName, "end of file" );
			return null;
		}
	
		// form an element
		WorkUnit record = parseCSVLine( buffer );
	
		// Stamp the runpath of the work unit
		addMyRunpathEntry( record );
	
		// return
		return record;
	}

//	void addRunpathEntry( WorkUnit inWU )
//	{
//		inWU.createRunpathEntry( getID() );
//	}

	private void __sep__Member_Fields_and_Constants__ () {}
	///////////////////////////////////////////////////////////////

	// private static final boolean debug = false;

	private static long fCounter;
	private static final long kReportInterval = 1000;

	private static final String FIELD_MAP_NAME = "field_map";

	//private static final String DONE_TRIGGER_XML_TEXT =
	//	"<trigger>work_done</trigger>";
	//private static final String DONE_TRIGGER_NAME =
	//	"done";


	////////////////////////////////////////////////////////////////

	private boolean fCanExit;

	private Queue fWriteQueue;
	private Queue fUsesQueue;

	
	private JDOMHelper fJdh;
	private String lastLocation;
	private InputStream inStream;
	private Reader reader;
	WorkUnit mWorkUnit;

}


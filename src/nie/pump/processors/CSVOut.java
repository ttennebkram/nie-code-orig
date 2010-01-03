/* output the work unit data in CSV format
 * Write the user data of work units to a CSV file
 * In the <parameters> section you should have:
 * <location>output_file.csv</location>
 * And optinally:
 * <exception>bad_records_file.csv</exception>
 *
 * You can also specify which fields are to be output, and in
 * which order with:
 * <output_field> field_or_path </output_field>
 * <output_field> field_or_path2 </output_field>
 * ... etc ....
 *
 * We do NOT write bad records to the normal output file,
 * so if you want those records you need to put in an exception.
 */

//package nie.processors;
package nie.pump.processors;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

//import Processor;
//import Application;
//import Queue;

import java.net.*;
import java.io.*;
import java.util.*;

import org.jdom.Element;
import org.jdom.Attribute;

public class CSVOut extends Processor
{
	public String kClassName() { return "CSVOut"; }


	// private static final boolean debug = false;


	public CSVOut( Application inApplication,
		Queue[] inReadFromQueue, Queue[] inWriteToQueue,
		Queue[] inUsesQueue,
		Element inParameters, String inID
		)
	{
		super( inApplication, inReadFromQueue, inWriteToQueue,
			inUsesQueue, inParameters, inID
			);
		final String kFName = "constructor";
		// If parameters were sent in, save them
		if( inParameters != null )
		{
			try
			{
				jh = new JDOMHelper( inParameters );
			}
			catch (Exception e)
			{
				jh = null;
				// System.err.println(
				//	"Error creating jdom helper for parameters\n" + e
				//	);
				fatalErrorMsg( kFName,
						"Error creating jdom helper for parameters\n" + e
						);
				System.exit(1);
			}
		}

		// Make sure we have at least one queue in the array of input queues
		if( (inReadFromQueue != null) &&
			(inReadFromQueue.length > 0) &&
			(inReadFromQueue[0] != null)
			)
		{
			// read from that queue
			fReadQueue = inReadFromQueue[0];
		}
		else
		{
			// System.err.println( "CSVOut passed an invalid queue set." );
			fatalErrorMsg( kFName, "CSVOut passed an invalid queue set." );
			System.exit( -1 );
		}
	}

	private void badQueueList()
	{
		final String kFName = "constructor";
		
		// System.err.println( "Bad output queue list given to CSVOut." );
		fatalErrorMsg( kFName, "Bad output queue list given to CSVOut." );
		// System.err.println( "The output queue list must have at least one item," );
		fatalErrorMsg( kFName, "The output queue list must have at least one item," );
		// System.err.println( "    queue to which to put good records" );
		fatalErrorMsg( kFName, "    queue to which to put good records" );
		// System.err.println( "    A second queue may be specified to which we will" );
		fatalErrorMsg( kFName, "    A second queue may be specified to which we will" );
		// System.err.println( "    queue bad records." );
		fatalErrorMsg( kFName, "    queue bad records." );
		System.exit( -1 );
	}

	/////////////////////////////////////////////
	//
	// And this is where the work gets done.
	//
	/////////////////////////////////////////////

	public void run()
	{

		final String kFName = "run";
		try
		{
			doOpen();
		}
		catch (Exception e)
		{
			// System.err.println( "CSVOut:could not open output" );
			// errorMsg( kFName, "could not open output" );
			//System.err.println( e );
			stackTrace( kFName, e, "Exception opening CSV output file" );
			fatalErrorMsg( kFName, "Exiting (1) due to exception: " + e );
			// return;
			System.exit(1);
		}

		/*
		 * Read a work unit in
		 * if the retrieve works well, send it to output
		 * if the retrieve goes badly, send it to exceptions, if present
		 */

		while( true )
		{
			WorkUnit lWorkUnit = null;
			// NO mWorkUnit, since nobody would ever see it

			try
			{
			    lWorkUnit = (WorkUnit)dequeue( fReadQueue );

				if( lWorkUnit == null )
				{
					//System.out.println( "CSVOut: WARNING: dequeued null work unit." );
					//continue;
					errorMsg( kFName, "dequeued null work unit." );
					continue;
				}

				outputRecord( lWorkUnit );
				lWorkUnit = null;
			}
			catch( InterruptedException ie )
			{
				// System.out.println( "CSVOut: got interrupt, returning now." );
				// return;
				// infoMsg( this, kFName, "got interrupt, returning now." );
				return;
			}
			catch( Exception lException )
			{
				stackTrace( kFName, lException, "Generic Exception" );
			}
				

			fCounter ++;
			if( fCounter % kReportInterval == 0 )
			{
				// System.out.println( "CSVOut: Processed '" + fCounter +
				//	"' records." );
				infoMsg( kFName, "Processed '" + fCounter +
					"' records." );
			}


		}	// Emd main while loop
	}



	/***************************************************
	*
	*	Simple Get/Set Accessors
	*
	****************************************************/


	// Location is an attribute
	public String getLocation()
	{
		if( jh != null )
			return jh.getTextByPath("location");
		else
			return null;
	}
	public String getExcLocation()
	{
		final String kFName = "getLocation";
		if( jh != null )
			return jh.getTextByPath("exception");
		else
			return null;
		//return getStringFromAttribute("exception", true);
	}

	// In the parameters section we will allow a field like:
	// <parameters>
	//  <output_field>field1</output_field>
	//  <output_field>field2 etc... </output_field>
	// </parameters>
	// This assumes the normal field style storage
	List getDesiredFields()
	{
		if( jh != null )
			return jh.getTextListByPathNotNullTrim(	"output_field" );
		else
			return new Vector();
	}

	public void outputRecord( WorkUnit wu )
		throws Exception
	{
		final String kFName = "outputRecord";
		// final boolean debug = false;

		if( wu == null )
			throw new Exception( "Attempt to output null work unit" );

		String buffer = null;

		buffer = getCSVTextFromRecord( wu );

		// System.err.println( "csvout: buffer='" + buffer + "'" );
		debugMsg( kFName, "buffer='" + buffer + "'" );

		if( buffer != null )
		{
			if( wu.getIsValidRecord() )
			{
				writeBlock( buffer, writer );
				debugMsg( kFName,
						"good record to normal output"
						);
			}
			else
			{
				if( excWriter != null )
				{
					writeBlock( buffer, excWriter );
					debugMsg( kFName,
							"bad record to exception output"
							);
				}
				else
				{
					writeBlock( buffer, writer );
					// System.err.println( "CSVOut: Warning:" +
					//	" Wrote invalid work unit to primary output because" +
					//	" no exception=location was set."
					//	);
					errorMsg( kFName,
						"Wrote invalid work unit to primary output because" +
						" no exception=location was set."
						);
				}
			}
		}
		// TODO: do something if it fails and we got a null
	}

	/***************************************************
	*
	*	Base Stream Operations
	*
	*****************************************************/

	private void doOpen() throws Exception {
		final String kFName = "doOpen";
		// System.out.println( "Debug: CSVOut:doOpen(): Start." );
		debugMsg( kFName, "Start." );

		// Open the ouptut
		String loc = getLocation();
		if( loc == null || loc.trim().equals("") )
			throw new Exception("No location for stream" );

		loc = loc.trim();

		// Is it a URL?
		if( loc.startsWith("http://") ||
			loc.startsWith("ftp://")
			)
		{
			throw new Exception("Can't output to urls, not implemented." );
		}

		File theFile = null;
		try
		{
			theFile = new File(
				System.getProperty( "user.dir" ), loc
				);
			outStream = new FileOutputStream( theFile );
		}
		catch (Exception e)
		{
			theFile = new File( loc );
			outStream = new FileOutputStream( theFile );
		}

		//outStream = new FileOutputStream( theFile );
		OutputStream tmpOut = new BufferedOutputStream( outStream );
		writer = new OutputStreamWriter( tmpOut );

		// open the exceptions
		String eloc = getExcLocation();
		if( eloc == null || eloc.equals("") )
			return;


		// Is it a URL?
		if( eloc.startsWith("http://") ||
			eloc.startsWith("ftp://")
			)
		{
			throw new Exception("Can't output exceptions to urls, not implemented." );
		}



		File eFile = null;
		try
		{
			eFile = new File(
				System.getProperty( "user.dir" ), eloc
				);
			excOutStream = new FileOutputStream( eFile );
		}
		catch (Exception e)
		{
			eFile = new File( eloc );
			excOutStream = new FileOutputStream( eFile );
		}

		OutputStream tmpOut2 = new BufferedOutputStream( excOutStream );
		excWriter = new OutputStreamWriter( tmpOut2 );

	}

	// ===========================================
	public void _never_called___close()
	{
		// Todo: See if we need to flush anything

		if( writer != null ) {
			try { writer.close(); } catch (Exception e) {}
			writer = null;
		}
		if( outStream != null ) {
			try { outStream.close(); } catch (Exception e) {}
			outStream = null;
		}
		if( excWriter != null ) {
			try { excWriter.close(); } catch (Exception e) {}
			excWriter = null;
		}
		if( excOutStream != null ) {
			try { excOutStream.close(); } catch (Exception e) {}
			excOutStream = null;
		}

	}

	private void writeBlock( String buffer, Writer inWriter )
		throws IOException
	{
		inWriter.write( buffer );
		//inWriter.println( buffer );
		inWriter.flush();
	}



	/***************************************************
	*
	*	Simple Process Logic
	*
	****************************************************/


	// Given a record, produce a CSV style string
	// representing it.
	// BTW, this csv record will, of course, not mention the
	// record name or specific field names
	private String getCSVTextFromRecord( WorkUnit wu )
	{
		final String kFName = "getCSVTextFromRecord";
		// final boolean debug = false;

		// We will build a return buffer as we go
		// to be converted to a normal string at the very end
		StringBuffer retBuff = new StringBuffer();

		// See if we have been instructed to use a custom list
		List customList = getDesiredFields();

		// If using a custom list
		if( customList != null && customList.size() > 0 )
		{
			boolean haveAddedField = false;
			Iterator it = customList.iterator();
			while( it.hasNext() )
			{
				String fieldName = (String)it.next();
				String fieldValue = (String)wu.getUserFieldText( fieldName );
				if( fieldValue == null )
				{
					// problem?
					//continue;
					// no problem
					fieldValue = "";
				}
				boolean tmpReturn = addNewCSVFieldToBuffer(
					retBuff, fieldValue, haveAddedField
					);

				haveAddedField = haveAddedField || tmpReturn;
			}
		}
		// Else we are defaulting to looking at <field> tags
		else
		{
			// Double check that this is a flat record
			if( ! wu.getIsUserDataFlat() )
				return null;

			boolean haveAddedField = false;
			Iterator it = wu.getAllFlatFields().iterator();
			// For each field in the record
			while( it.hasNext() )
			{

				// pull the next field
				Element fieldElem = (Element)it.next();
				// Check if we're to include this, by default we do
				String attr = fieldElem.getAttributeValue( "default_output" );
				// If the attribute is present and it is "0" then don't
				// include, just skip this field
				if( attr != null && attr.equals("0") )
					continue;
				// Else if there's no default_output attribute
				else if( attr == null )
				{
					// By default we don't include _fields
					String fieldName = fieldElem.getAttributeValue( "name" );
					traceMsg( kFName,
							"fieldname='" + fieldName + "'"
							);
					if( fieldName == null || fieldName.trim().startsWith("_") )
					{
						// System.out.println( "\tskipping" );
						traceMsg( kFName, "skipping" );
						continue;
					}
					else
						// System.out.println( "\tIncluding" );
						traceMsg( kFName, "Including" );
				}

				// Get the element's text
				String fieldValue = fieldElem.getText();

				if( fieldValue == null )
					fieldValue = "";

				// Add it to the buffer
				boolean tmpReturn = addNewCSVFieldToBuffer(
					retBuff, fieldValue, haveAddedField
					);

				haveAddedField = haveAddedField || tmpReturn;

				// Todo: we really should complain loudly
				// if this comes back false

			} // End for each field in the record

		}   // End else use standard fields

		// By now we've added all the fields to the buffer

		// Add a line feed for good measure
		retBuff.append('\n');
		// No, let java do this in write buffer

		// Return as a string
		return new String( retBuff );

	}

	// Utility function for previous method
	private boolean addNewCSVFieldToBuffer( StringBuffer ioBuffer,
		String inData, boolean inHaveAddedField
		)
	{

		boolean outHaveAddedField = false;

		// Check a few things
		boolean hasQuote = inData.indexOf('"') >=0 ? true : false;
		boolean hasComma = inData.indexOf(',') >=0 ? true : false;
		boolean hasNewline = inData.indexOf('\r') >=0 ||
			inData.indexOf('\n') >=0 ? true : false;
		// Set a flag
		boolean needsQuoting = hasQuote || hasComma || hasNewline;

		// Add a trailing comma if we need it
		if( inHaveAddedField )
		{
			ioBuffer.append(',');
		}

		// If we don't need quoting/escaping, just add it to the buffer
		if( ! needsQuoting )
		{
			ioBuffer.append( inData );
			outHaveAddedField = true;
		}
		// Else we do need special quoting/character escaping
		else
		{
			// Add the opening quote
			ioBuffer.append('"');
			// If no quotes in the data, we can just add the field
			if( ! hasQuote && ! hasNewline )
			{
				ioBuffer.append( inData );
			}
			// Else it does have quotes or newlines,
			// we need to copy character
			// by character
			else
			{
				// Convert to a string buffer
				StringBuffer buff2 = new StringBuffer( inData );
				// For each character in the string buffer
				for( int i=0; i<buff2.length(); i++ )
				{
					char c = buff2.charAt(i);
					// If it's a newline, zap it with a space
					if( c == '\r' || c == '\n' )
					{
						ioBuffer.append(' ');
					}
					// If it's a quote, add an extra quote
					else if( c == '"' )
					{
						ioBuffer.append(c);
						ioBuffer.append('"');
					}
					// Else regular character, pass it on
					// including commas since the entire thing
					// is in quotes
					else
					{
						ioBuffer.append(c);
					}
				}  // End for each character in the buffer
			}   // End else it does not have quotes or newlines
			// Add the closing quote
			ioBuffer.append('"');
			// and we did add a field
			outHaveAddedField = true;
		}   // End Else we did need special quoting/escaping

		return outHaveAddedField;

	}


	private static long fCounter;
	private static final long kReportInterval = 1000;

	Queue fReadQueue;   // Main input queue
	//Queue fWriteQueue;  // Main output queue
	//Queue fExceptionsQueue; // Exceptions
	private OutputStream outStream; // file outputs
	private Writer writer;
	private OutputStream excOutStream;  // file outputs for bad records
	private Writer excWriter;
	private JDOMHelper jh;
	// Since we don't output any work units, nothing to hang an error on
	WorkUnit _mWorkUnit;

}


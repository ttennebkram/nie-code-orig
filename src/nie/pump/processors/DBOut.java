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

import java.net.*;
import java.io.*;
import java.util.*;

import nie.core.*;
import nie.pump.base.*;
import nie.pump.base.Queue;

//import Processor;
//import Application;
//import Queue;

import org.jdom.Element;
import org.jdom.Attribute;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.Connection;


public class DBOut extends Processor
{
	private final static String kClassName = "DBOut";
	public String kClassName() { return kClassName; }


	// private static final boolean debug = false;


	public DBOut( Application inApplication,
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
		else {
			// System.err.println(
			//	"No db_out parameters"
			//	);
			fatalErrorMsg( kFName,
					"No db_out parameters"
					);
			System.exit(1);
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
			// System.err.println( "db_0ut passed an invalid queue set." );
			fatalErrorMsg( kFName, "db_0ut passed an invalid queue set." );
			System.exit( -1 );
		}

		// Open the ouptut
		String tableName = getTableName();
		if( null==tableName ) {
			// throw new Exception("No table for data" );
			fatalErrorMsg( kFName, "No table for data" );
			System.exit( -1 );
		}


		initDB();
	}

	private void initDB()
	{
		final String kFName = "initDB";
		final String kExTag = kClassName + '.' + kFName + ": ";

		Element lDBConfElem = jh.findElementByPath(
				DB_CONFIG_PATH
				);

		if( null==lDBConfElem ) {
			// Make sure we don't see an old one
			fDBConfig = null;
			// System.out.println(
			//		"No Database configuration was found."
			//		+ " Checked /(rootelement)/" + DB_CONFIG_PATH
			//		);
			fatalErrorMsg( kFName,
				"No Database configuration was found."
				+ " Checked /(rootelement)/" + DB_CONFIG_PATH
				);
			System.exit(1);
		}

		// Now check for, instantiate, and store the dbconfig section
		// This uses the new JDOMHelper object-by-path factory
		try
		{
			fDBConfig = new DBConfig( lDBConfElem, null );
		}
		catch(Exception dbe)
		{
			String tmpMsg = "Unable to instantiate database configuration."
				+ " This may be caused by a simple database configuration error."
				+ " Reason/Exception = \"" + dbe + "\""
				;
	
			errorMsg( kFName, tmpMsg );
			errorMsg( kFName,
				"*** Even though the database is not reachable"
				+ ", we will still allow the Search Track Server"
				+ " to process searches, so the site will REMAIN UP."
				+ " However searches will likely NOT be logged"
				+ " and other operations requiring the database will not available. ***"
				);
		}

		// Configure the database and cache a statement
		try
		{
			// fDBConf = new DBConfig( fConfigFileURI );
			// ^^^ moved to setupDBConfigFromURI()

			// cStatement = getDBConfig().createStatement();
			Object [] objs = getDBConfig().createStatement();
			cStatementUpdate = (Statement) objs[0];
			cConnectionUpdate = (Connection) objs[1];
		}
		catch( Exception e )
		{
			fatalErrorMsg( kFName, "Error connecting to DB: " + e );
			System.exit(1);
		}



	}







	private void badQueueList()
	{
		final String kFName = "constructor";
		
		// System.err.println( "Bad output queue list given to DBOut." );
		fatalErrorMsg( kFName, "Bad output queue list given to DBOut." );
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
		/*
		 * Read a work unit in
		 * if the retrieve works well, send it to output
		 * if the retrieve goes badly, send it to exceptions, if present
		 */

		while( true )
		{
			WorkUnit lWorkUnit = null;

			try
			{
				lWorkUnit = (WorkUnit)dequeue( fReadQueue );

				if( lWorkUnit == null )
				{
					// System.out.println( "DBOut: WARNING: dequeued null work unit." );
					errorMsg( kFName, "dequeued null work unit." );
					continue;
				}

				outputRecord( lWorkUnit );
				lWorkUnit = null;
			}
			catch( InterruptedException ie )
			{
				// System.out.println( "DBOut: got interrupt, returning now." );
				// infoMsg( kFName, "got interrupt, returning now." );
				return;
			}
			catch( Exception lException )
			{
				stackTrace( kFName, lException, "Generic Exception" );
			}

			fCounter ++;
			if( fCounter % kReportInterval == 0 )
			{
				// System.out.println( "DBVOut: Processed '" + fCounter +
				//	"' records." );
				infoMsg( kFName, "Processed '" + fCounter +
					"' records." );
			}


		}	// Emd main while loop

		// closeDB();
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
	private String getSqlTextFromRecord( WorkUnit wu )
	{
		final String kFName = "getSqlTextFromRecord";
		// final boolean _debug = false;

		// We will build a return buffer as we go
		// to be converted to a normal string at the very end
		StringBuffer retBuff = new StringBuffer();

		retBuff.append( "INSERT INTO " );
		retBuff.append( getTableName() );
		retBuff.append( " (" );

		// See if we have been instructed to use a custom list
		List customList = getDesiredFields();

		// Step 1: Add all the field NAMES
		boolean haveFieldNames = false;
		if( customList != null && customList.size() > 0 )
		{
			for( Iterator nit=customList.iterator(); nit.hasNext() ; ) {
				String fieldName = (String) nit.next();
				retBuff.append( fieldName );
				haveFieldNames = true;
				if( nit.hasNext() )
					retBuff.append( ", " );
			}
		}
		else if( wu.getIsUserDataFlat() ) {
			// For each field in the record
			boolean haveAddedFieldName = false;
			for( Iterator it = wu.getAllFlatFields().iterator(); it.hasNext() ; )
			{

				// pull the next field
				Element fieldElem = (Element)it.next();
				// Check if we're to include this, by default we do
				String attr = fieldElem.getAttributeValue( "default_output" );
				// If the attribute is present and it is "0" then don't
				// include, just skip this field
				String fieldName = null;
				if( attr != null && attr.equals("0") )
					continue;
				// Else if there's no default_output attribute
				else if( attr == null )
				{
					// By default we don't include _fields
					fieldName = fieldElem.getAttributeValue( "name" );
					traceMsg( kFName, "fieldname='" + fieldName + "'"
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

				if( haveAddedFieldName )
					retBuff.append( ", " );

				retBuff.append( fieldName );

				haveAddedFieldName = true;
				haveFieldNames = true;

				// Todo: we really should complain loudly
				// if this comes back false

			} // End for each field in the record
		}
		retBuff.append( ") VALUES (" );
		// if haveFieldNames


		// Step 2: Add the field VALUES
		// If using a custom list
		if( customList != null && customList.size() > 0 )
		{
			boolean haveAddedField = false;
			Iterator it = customList.iterator();
			while( it.hasNext() )
			{
				String fieldName = (String)it.next();
				String fieldValue = (String)wu.getUserFieldText( fieldName );

				if( DEFAULT_DO_TRIM_AND_NULL )
					fieldValue = NIEUtil.trimmedStringOrNull( fieldValue );

				boolean needsEnclosingQuotes = true;
				if( null == fieldValue )
				{
					// problem?
					//continue;
					// no problem
					fieldValue = "NULL";
					needsEnclosingQuotes = false;
				}
				else {
					fieldValue = NIEUtil.sqlEscapeString( fieldValue, true );
					int fieldType = getFieldType( fieldName );
					needsEnclosingQuotes =
						DBTableDef.FIELD_TYPE_TEXT==fieldType
						|| DBTableDef.FIELD_TYPE_DATETIME==fieldType
						;
				}
				if( needsEnclosingQuotes )
					retBuff.append( '\'' );
				retBuff.append( fieldValue );
				if( needsEnclosingQuotes )
					retBuff.append( '\'' );
				if( it.hasNext() )
					retBuff.append( ", " );

				haveAddedField = true;
			}
		}
		// Else we are defaulting to looking at <field> tags
		else
		{
			// Double check that this is a flat record
			if( ! wu.getIsUserDataFlat() ) {
				errorMsg( kFName, "Can't automatically output freeform data to database - need custom list (2)" );
				return null;
			}

			boolean haveAddedField = false;
			// For each field in the record
			for( Iterator it = wu.getAllFlatFields().iterator(); it.hasNext() ; )
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
					if( fieldName == null || fieldName.trim().startsWith("_") )
						continue;
				}

				// Get the element's text
				String fieldValue = fieldElem.getText();
				if( DEFAULT_DO_TRIM_AND_NULL )
					fieldValue = NIEUtil.trimmedStringOrNull( fieldValue );

				boolean needsEnclosingQuotes = true;

				if( fieldValue == null ) {
					fieldValue = "NULL";
					needsEnclosingQuotes = false;
				}
				else
					fieldValue = NIEUtil.sqlEscapeString( fieldValue, true );

				if( haveAddedField )
					retBuff.append( ", " );

				if( needsEnclosingQuotes )
					retBuff.append( '\'' );
				retBuff.append( fieldValue );
				if( needsEnclosingQuotes )
					retBuff.append( '\'' );

				haveAddedField = true;


				// Todo: we really should complain loudly
				// if this comes back false

			} // End for each field in the record

		}   // End else use standard fields
		// By now we've added all the fields to the buffer

		retBuff.append( ")" );

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





	// Log a transaction
	public boolean outputRecord( WorkUnit wu )
			throws SQLException,
				DBConfigException
				//, DBConfigInServerReconnectWait
	{
		final String kFName = "logTransaction";
		final String kExTag = kClassName + ':' + kFName + ": ";

		if( getDBConfig() == null )
		{
		    /***
		    if( ! fHaveIssuesBrokenWarning )
			{
				errorMsg( kFName,
					"The database is NOT configured, so we can not write to it."
					+ " To avoid filling up your log file, this is the ONLY time"
					+ " we will issue this error message."
					+ " Please check earlier log errors and your configuration."
					);
				fHaveIssuesBrokenWarning = true;
			}
			return false;
			***/
		    fatalErrorMsg( kFName, "NO DATABASE configuration" );
		    System.exit(1);
		}

		// boolean debug = shouldDoDebugMsg( kFName );




		if( wu == null )
			throw new SQLException( "Attempt to output null work unit" );

		String buffer = null;

		buffer = getSqlTextFromRecord( wu );

		debugMsg( kFName, "buffer='" + buffer + "'" );

		if( null != buffer )
		{

			debugMsg( kFName, "Issuing SQL=\n" + buffer );

			cStatementUpdate.execute( buffer );

			cConnectionUpdate.commit();

			return true;
		}
		// TODO: do something if it fails and we got a null
		else
			return false;

	}









	/***************************************************
	*
	*	Simple Get/Set Accessors
	*
	****************************************************/

	DBConfig getDBConfig() {
		return fDBConfig;
	}

	// Location is an attribute
	public String getTableName()
	{
		if( jh != null )
			return jh.getTextByPath("table");
		else
			return null;
	}
	public String getExcLocation()
	{
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
			return jh.getTextListByPathNotNullTrim( DESIRED_FIELD_TAG );
		else
			return new Vector();
	}

	int getFieldType( String inFieldName ) {
		final String kFName = "getFieldType";
		List defs = jh.findElementsByPath( DESIRED_FIELD_TAG );
		int outType = DBTableDef.FIELD_TYPE_UNKNOWN;
		if( null!=defs && ! defs.isEmpty() ) {
			for( Iterator it = defs.iterator(); it.hasNext() ; ) {
				Element def = (Element) it.next();
				String thisFieldName = JDOMHelper.getTextSuperTrimOrNull( def );
				if( null!=thisFieldName && thisFieldName.equalsIgnoreCase(inFieldName) ) {
					String fieldTypeStr = JDOMHelper.getStringFromAttributeTrimOrNull( def, FIELD_TYPE_ATTR );
					if( null!=fieldTypeStr ) {
						outType = DBTableDef.fieldTypeStringToInt( fieldTypeStr );
					}
					break;
				}
			}
		}
		if( DBTableDef.FIELD_TYPE_UNKNOWN==outType || DBTableDef.FIELD_TYPE_INVALID==outType )
			outType = DEFAULT_FIELD_TYPE;
		return outType;
	}

	/***************************************************
	*
	*	Base Stream Operations
	*
	*****************************************************/
/*
	// ===========================================
	public void closeDB()
	{
		final String kFName = "closeDB";
		cStatementUpdate = DBConfig.closeStatement( cStatementUpdate, kClassName, kFName, false );
		cConnectionUpdate = DBConfig.closeConnection( cConnectionUpdate, kClassName, kFName, false );
	}

*/


	private static long fCounter;
	private static final long kReportInterval = 1000;

	Queue fReadQueue;   // Main input queue
	//Queue fWriteQueue;  // Main output queue
	//Queue fExceptionsQueue; // Exceptions
	private OutputStream outStream; // file outputs
	private Writer _writer;
	private OutputStream _excOutStream;  // file outputs for bad records
	private Writer _excWriter;
	private JDOMHelper jh;
	// Store the instance we get back
	private DBConfig fDBConfig;
	Statement cStatementUpdate;
	Connection cConnectionUpdate;

	private boolean fHaveIssuesBrokenWarning;

	public static final String DESIRED_FIELD_TAG = "output_field";
	public static final String FIELD_TYPE_ATTR = DBTableDef.TYPE_ATTR;

	public static final String DB_CONFIG_PATH =
		DBConfig.MAIN_ELEMENT_NAME; // "database";

	public static final int DEFAULT_FIELD_TYPE = DBTableDef.FIELD_TYPE_TEXT;

	public static final boolean DEFAULT_DO_TRIM_AND_NULL = true;
	// No output work unit, we don't output one so nothing to hang errors on
	WorkUnit _mWorkUnit;

}


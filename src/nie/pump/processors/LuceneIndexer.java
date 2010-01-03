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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

public class LuceneIndexer extends Processor
{
	public String kClassName() { return "LuceneIndexer"; }


	// private static final boolean debug = false;


	public LuceneIndexer( Application inApplication,
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
			// System.err.println( "LucIdx passed an invalid queue set." );
			fatalErrorMsg( kFName, "LucIdx passed an invalid queue set." );
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
			stackTrace( kFName, e, "Exception opening Lucene collection" );
			fatalErrorMsg( kFName, "Can't index data if I can't open the collection." );
			System.exit(1);
			// return;
		}

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
					// System.out.println( "LucIdx: WARNING: dequeued null work unit." );
					errorMsg( kFName, "dequeued null work unit." );
					continue;
				}

				outputRecord( lWorkUnit );
				lWorkUnit = null;
			}
			catch( InterruptedException ie )
			{
				
				// System.out.println( "LucIdx: got interrupt, shutting down." );
				// infoMsg( kFName, "got interrupt, shutting down." );
				doClose();
			}
			catch( Exception lException )
			{
				stackTrace( kFName, lException, "Generic exception" );
			}

			fCounter ++;
			if( fCounter % kReportInterval == 0 )
			{
				// System.out.println( "LucIdx: Processed '" + fCounter +
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
		final String kFName = "getLocation";
		if( jh != null )
			return jh.getTextByPath("location");
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
			// return jh.getTextListByPathNotNullTrim( "index_field" );
			return jh.getJdomElement().getChildren( FIELD_ELEM );
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

		// buffer = getCSVTextFromRecord( wu );

		Document lucDoc = createLuceneDocumentFromFields( wu );

		// System.err.println( "lucidx: lucdoc='" + lucDoc + "'" );
		debugMsg( kFName, "lucdoc='" + lucDoc + "'" );

		mLucWriter.addDocument( lucDoc );

	}

	/***************************************************
	*
	*	Base Stream Operations
	*
	*****************************************************/

	private void doOpen()
		throws Exception
	{
		final String kFName = "constructor";
		// System.out.println( "Debug: lucidx:doOpen(): Start." );
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

		/***
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
		***/


		// Lucene stuff
		// ry {

			if( null == loc || loc.trim().equals("") )
				loc = DEFAULT_LUCENE_INDEX_NAME;

			// System.err.println( "loc = " + loc );

			mLucWriter = new IndexWriter( loc,
				new StandardAnalyzer(), true
				);
			
		// }
		// catch( Exception e ) {
			// System.err.println( "LucIdx:Lucne expceiton: " + e );
		//	fatalErrorMsg( kFName, "Lucne expceiton: " + e );
			// e.printStackTrace( System.err );
		//	stackTrace( kFName, e,"Lucne expceiton:" );
		//	System.exit(1);
		// }

	}

	// ===========================================
	public void doClose()
	{
		final String kFName = "doClose";
		// All done
		if( null!=mLucWriter ) {
			try {
				mLucWriter.optimize();
				mLucWriter.close();
			}
			catch( IOException ioe ) {
				stackTrace( kFName, ioe, "got IO exception closing lucene" );
			}
		}

	}


	/***************************************************
	*
	*	Simple Process Logic
	*
	****************************************************/

	// When assigning defaults, we sometimes want to know whether
	// a value was explicitly set or not
	// Also in this class we allow for some prefixes
	// BOOLEAN_ATTR_PREFIX_1 = "should_";, BOOLEAN_ATTR_PREFIX_2 = "do_";

	String getVariableAttrOrNull( Element inFieldDef, String inAttrNameBase ) {
		if( null==inFieldDef )
			return null;
		inAttrNameBase = NIEUtil.trimmedLowerStringOrNull( inAttrNameBase );
		if( null==inAttrNameBase )
			return null;
		// String valueStr = inFieldDef.getAttributeValue( inAttrName );
		String valueStr = JDOMHelper.getStringFromAttributeTrimOrNull(
			inFieldDef, inAttrNameBase, false
			);
		if( null==valueStr )
			valueStr = JDOMHelper.getStringFromAttributeTrimOrNull(
				inFieldDef, BOOLEAN_ATTR_PREFIX_1 + inAttrNameBase, false
				);
		if( null==valueStr )
			valueStr = JDOMHelper.getStringFromAttributeTrimOrNull(
				inFieldDef, BOOLEAN_ATTR_PREFIX_2 + inAttrNameBase, false
				);
		return valueStr;
	}

	public static boolean getDefaultDoStore( String inFieldName ) {
		if( null == inFieldName )
			return DEFAULT_SHOULD_STORE;
		// Override default and specifially turn off long fields
		if( inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_RAW_CONTENT_FIELD_NAME)
			|| inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_CONTENT_FIELD_NAME)
			)
			return false;
		if( inFieldName.equalsIgnoreCase(PumpConstants.LIKELY_TREE_FIELD_NAME) )
			return false;
		if( null!=PumpConstants.DEFAULT_TREE_FIELD_NAME
			&& inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_TREE_FIELD_NAME)
			)
			return false;
		// And turn ON important fields
		if( inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_KEY_FIELD_NAME)
			|| inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_KEY_FIELD_NAME2)
			)
			return true;
		if( inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_TITLE_FIELD_NAME)
			|| inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_TITLE_FIELD_NAME2)
			)
			return true;
		if( inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_DESC_FIELD_NAME)
			|| inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_DESC_FIELD_NAME2)
			)
			return true;

		// OK, trust the default
		return DEFAULT_SHOULD_STORE;
	}

	public static boolean getDefaultDoTokenize( String inFieldName ) {
		if( null == inFieldName )
			return DEFAULT_SHOULD_TOKENIZE;
		// Override default and specifially turn off key fields
		if( inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_KEY_FIELD_NAME)
			|| inFieldName.equalsIgnoreCase(PumpConstants.DEFAULT_KEY_FIELD_NAME2)
			)
			return false;
		// OK, trust the default
		return DEFAULT_SHOULD_TOKENIZE;
	}

	public static boolean getDefaultDoIndex( String inFieldName ) {
		return DEFAULT_SHOULD_INDEX;
	}

	public static boolean getDefaultDoTrim( String inFieldName ) {
		return DEFAULT_SHOULD_TRIM;
	}

	private boolean addFieldToDocument(
		WorkUnit inWu,
		Document inDocument,
		Element inFieldDef, String inFieldContent
		// ,boolean inDoStore, boolean inDoIndex, boolean inDoTokenize
	) {
		final String kFName = "addFieldToDocument(1)";
		if( null == inWu ) {
			inWu.errorMsg( this, kFName, "Internal: NULL work unit passed in" );
			return false;
		}
		if( null == inDocument ) {
			inWu.errorMsg( this, kFName, "Internal: NULL Lucene document object passed in" );
			return false;
		}
		if( null == inFieldDef ) {
			inWu.errorMsg( this, kFName, "Internal: NULL output field definition passed in" );
			return false;
		}
		if( null == inFieldContent ) {
			inWu.errorMsg( this, kFName, "Internal: NULL field content passed in" );
			return false;
		}
		String fieldName = JDOMHelper.getTextTrimOrNull( inFieldDef );
		if( null == fieldName ) {
			inWu.errorMsg( this, kFName,
				"No fieldname given in definition!"
				+ " Must be defined as the main text of the \"" + FIELD_ELEM + "\" node."
				);
			return false;
		}

		String storeStr = getVariableAttrOrNull( inFieldDef, SHOULD_STORE_ATTR );
		boolean defaultDoStore = getDefaultDoStore( fieldName );
		boolean doStore = NIEUtil.decodeBooleanString( storeStr, defaultDoStore );

		String indexStr = getVariableAttrOrNull( inFieldDef, SHOULD_INDEX_ATTR );
		// boolean defaultDoIndex = DEFAULT_SHOULD_INDEX;
		boolean defaultDoIndex = getDefaultDoIndex( fieldName );
		boolean doIndex = NIEUtil.decodeBooleanString( indexStr, defaultDoIndex );

		String tokenizeStr = getVariableAttrOrNull( inFieldDef, SHOULD_TOKENIZE_ATTR );
		boolean defaultDoTokenize = getDefaultDoTokenize( fieldName );
		boolean doTokenize = NIEUtil.decodeBooleanString( tokenizeStr, defaultDoTokenize );

		String trimStr = getVariableAttrOrNull( inFieldDef, SHOULD_TRIM_ATTR );
		// boolean defaultDoTrim = DEFAULT_SHOULD_TRIM;
		boolean defaultDoTrim = getDefaultDoTrim( fieldName );
		boolean doTrim = NIEUtil.decodeBooleanString( trimStr, defaultDoTrim );

		return addFieldToDocument(
			inWu, inDocument,
			fieldName, inFieldContent,
			doStore, doIndex, doTokenize, doTrim
			);
	}


	private boolean addFieldToDocument(
		WorkUnit inWu,
		Document inDocument,
		String inFieldName, String inFieldContent
	) {
		final String kFName = "addFieldToDocument(2)";
		if( null == inWu ) {
			inWu.errorMsg( this, kFName, "Internal: NULL work unit passed in" );
			return false;
		}
		if( null == inDocument ) {
			inWu.errorMsg( this, kFName, "Internal: NULL Lucene document object passed in" );
			return false;
		}
		NIEUtil.trimmedStringOrNull( inFieldName );
		if( null == inFieldName ) {
			inWu.errorMsg( this, kFName, "Internal: NULL/empty Lucene field name passed in" );
			return false;
		}
		if( null == inFieldContent ) {
			inWu.errorMsg( this, kFName, "Internal: NULL field content passed in" );
			return false;
		}

		return addFieldToDocument(
			inWu, inDocument,
			inFieldName, inFieldContent,
			getDefaultDoStore(inFieldName),
			getDefaultDoIndex(inFieldName),
			getDefaultDoTokenize(inFieldName),
			getDefaultDoTrim(inFieldName)
			);

	}


	private boolean addFieldToDocument(
		WorkUnit inWu,
		Document inDocument,
		String inFieldName, String inFieldContent,
		boolean inDoStore, boolean inDoIndex, boolean inDoTokenize, boolean inDoTrim
	) {
		final String kFName = "addFieldToDocument(3)";

		inFieldName = inFieldName.toLowerCase();

		if( null == inFieldContent ) {
			inWu.warningMsg( this, kFName, "NULL field content passed in for field \"" + inFieldName + "\"" );
			return false;
		}
		if( null == NIEUtil.trimmedStringOrNull(inFieldContent) ) {
			inWu.warningMsg( this, kFName, "Empty (but non-NULL) field content passed in for field \"" + inFieldName + "\"" );
			// Still go ahead
		}

		// New in Lucene, translate boolean options to Lucene class options
		// Whether to store it or not
		Field.Store storeOption = inDoStore ?
				Field.Store.YES : Field.Store.NO;
		// Is it searchable, and maybe tokenized?
		// Usually Yes to both
		Field.Index indexOption = null;
		if( inDoIndex ) {
			if( inDoTokenize ) {
				indexOption = Field.Index.TOKENIZED;
			}
			else {
				indexOption = Field.Index.UN_TOKENIZED;
			}
		}
		else {
			indexOption = Field.Index.NO;
		}

		if( inDoTrim )
			inFieldContent = inFieldContent.trim();

		// Field newField = new Field( inFieldName, inFieldContent,
		//	inDoStore, inDoIndex, inDoTokenize
		//		);
		Field newField = new Field( inFieldName, inFieldContent,
				storeOption, indexOption
				);

		inDocument.add( newField );
		return true;	
	}

	private Document createLuceneDocumentFromFields( WorkUnit wu )
	{
		final String kFName = "createLuceneDocumentFromFields";
		// final boolean debug = false;

		// See if we have been instructed to use a custom list
		List customList = getDesiredFields();

		Document outDoc = new Document();

		// If using a custom list
		if( customList != null && customList.size() > 0 )
		{
			boolean haveAddedField = false;
			Iterator it = customList.iterator();
			while( it.hasNext() )
			{
				// String fieldName = (String)it.next();
				Element fieldDef = (Element)it.next();
				String fieldName = JDOMHelper.getTextTrimOrNull( fieldDef );
				String fieldValue = (String)wu.getUserFieldText( fieldName );
				if( fieldValue == null )
				{
					// problem?
					//continue;
					// no problem
					fieldValue = "";
				}
				addFieldToDocument(
					wu, outDoc,
					fieldDef, fieldValue
					);

				// haveAddedField = haveAddedField || tmpReturn;
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
				String fieldName = null;
				if( attr != null && attr.equals("0") )
					continue;
				// Else if there's no default_output attribute
				else if( attr == null )
				{
					// By default we don't include _fields
					fieldName = fieldElem.getAttributeValue( "name" );
					// if(debug)
						// System.out.println(
						//		"lucids: fieldname='" + fieldName + "'"
						//		);
						traceMsg( kFName,
								"fieldname='" + fieldName + "'"
								);
					if( fieldName == null || fieldName.trim().startsWith("_") )
					{
						// if(debug)
								// System.out.println( "\tskipping" );
								traceMsg( kFName, "skipping" );
						continue;
					}
					else
						// if(debug) 	// System.out.println( "\tIncluding" );
									traceMsg( kFName, "Including" );
				}

				// Get the element's text
				String fieldValue = fieldElem.getText();

				if( fieldValue == null )
					fieldValue = "";

				// Add it to the buffer
				// boolean tmpReturn = addNewCSVFieldToBuffer(
				//	retBuff, fieldValue, haveAddedField
				//	);

				addFieldToDocument(
					wu, outDoc,
					fieldName, fieldValue
					);

				// haveAddedField = haveAddedField || tmpReturn;

				// Todo: we really should complain loudly
				// if this comes back false

			} // End for each field in the record

		}   // End else use standard fields

		// By now we've added all the fields to the buffer

		// Return as a string
		return outDoc;

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

	IndexWriter mLucWriter;

	public static final String DEFAULT_LUCENE_INDEX_NAME = "lucene_index";
	public static final String FIELD_ELEM = "index_field";
	public static final String SHOULD_STORE_ATTR = "store";
	public static final String SHOULD_INDEX_ATTR = "index";
	public static final String SHOULD_TOKENIZE_ATTR = "tokenize";
	public static final String SHOULD_TRIM_ATTR = "trim";
	public static final String _SHOULD_NORMALIZE_CASE_ATTR = "normalize_case";

	public static final String BOOLEAN_ATTR_PREFIX_1 = "should_";
	public static final String BOOLEAN_ATTR_PREFIX_2 = "do_";

	// Default store is IGNORED for know long fields, which default to FALSE,
	// and some common display fields, which always default to TRUE
	public static final boolean DEFAULT_SHOULD_STORE = true;
	public static final boolean DEFAULT_SHOULD_INDEX = true;
	// Default tokenize is IGNORED for key/id field, which defaults to FALSE
	public static final boolean DEFAULT_SHOULD_TOKENIZE = true;
	public static final boolean DEFAULT_SHOULD_TRIM = true;
	// Not useful here since we don't pass through any work units, so
	// nothing to hang errors on
	WorkUnit _mWorkUnit;

}


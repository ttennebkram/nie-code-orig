package nie.core;
import java.util.*;
import java.io.*;
import java.sql.*;
import org.jdom.Element;
import org.jdom.Document;



/**
 * @author mbennett
 *
 * A class that encapsulates NIE's XML table definitions
 */
public class DBTableDef
{
	private final static String kClassName = "DBTableDef";

	// Users do not call this directly
	// Use getTableDef() below
	// This is so that we only need to construct each definition once
	// private DBTableDef( String inTableName, String inXMLURI )
	private DBTableDef( String inTableName,
		String inXMLURI, DBConfig inDBConfiguration
		)
		throws DBConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inTableName = normalizeTableName( inTableName );
		inXMLURI = NIEUtil.trimmedStringOrNull( inXMLURI );

//		if( null == inTableName )
//			throw new DBConfigException( kExTag
//				+ "Null/empty input table name."
//				);
//		^^^ change of heart, I guess this is OK, we can get it from xml file
//		but BETTER to set it both places as an error check
//		The current factory methods DO set it

		if( null == inXMLURI )
			throw new DBConfigException( kExTag
				+ "Null/empty input configuration URI"
				);

		// Some additional sanity checking
		if( null == cTableDefCache )
			throw new DBConfigException( kExTag
				+ "table def cache is null."
				);
		if( null == inDBConfiguration )
			throw new DBConfigException( kExTag
				+ "Database configuration object that was passed in is null."
				);

		// Store the table name and config path, we may need them later
		// if( inTableName != null && ! mCasen )
		//	inTableName = inTableName.toLowerCase();
		fTableNameV0 = inTableName;
		fConfigURI = inXMLURI;
		fDBConf = inDBConfiguration;

		// Read the config
		fMainNode = null;
		try
		{
			// fOverallMasterTree = new JDOMHelper( fConfigFileURI );

			// Instead, call the JDOMHelper constructor that allows for <include>
			// tags.  We're the top level caller, level=0, and there was no parent
			// of the config file to search against
			fMainNode = new JDOMHelper( fConfigURI, null, 0 );

		}
		catch (JDOMHelperException e)
		{
			throw new DBConfigException(
				"Error loading config file (1)."
				+ " JDOMHelperException: " + e
				);
		}
		// More sanity checks
		if( null == fMainNode )
			throw new DBConfigException(
				"Got back a NULL xml tree"
				+ " from file \"" + fConfigURI + "\""
				);

		// Get the table name declared inside the xml file
		fTableNameV1 = fMainNode.getStringFromAttributeTrimOrNull( NAME_ATTR );
		fTableNameV1 = normalizeTableName( fTableNameV1 );
		if( null == fTableNameV1 )
			throw new DBConfigException( kExTag
				+ "Null/empty table name; a table name is required."
				+ " Please set attribute " + NAME_ATTR
				+ " From configuration URI \"" + fConfigURI + "\""
				);
		// Normalize
		// if( ! mCasen )
		// 	fTableNameV1 = fTableNameV1.toLowerCase();
		// v1 is the real name, declared in the XML

		// IF we had one passed to the constructor, and if it does NOT match
		// what we found in the file, then that's really bad
		if( null != fTableNameV0 && ! fTableNameV0.equals( fTableNameV1 ) )
			throw new DBConfigException( kExTag
				+ "Mismatched table names."
				+ " Constructor was told to expect \"" + fTableNameV0 + "\""
				+ " but found \"" + fTableNameV1 + "\""
				+ " from attribute " + NAME_ATTR
				+ " in configuration \"" + fConfigURI + "\""
				);


		// Get the jdom's name and compare
		if( cTableDefCache.containsKey( getTableName() ) )
			throw new DBConfigException( kExTag
				+ "Table definition has already been loaded"
				+ ", table=\"" + getTableName() + "\""
				);

		// setup a cache to the field nodes for speed
		initAndPopulateFieldCache();

	}


	public static DBTableDef getTableDef( String inTableName,
		DBConfig inDBConfig
		)
		throws DBConfigException
	{
		return getTableDef( inTableName, null, inDBConfig );
	}
	// Use this INSTEAD of the constructor
	// If you pass in null for the config URI we will assume it's
	// defined in the system
	public static DBTableDef getTableDef( String inTableName,
		String optXMLURI, DBConfig inDBConfig
		)
		throws DBConfigException
	{
		final String kFName = "getTableDef";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inTableName = normalizeTableName( inTableName );
		if( inTableName == null )
			throw new DBConfigException( kExTag
				+ "Null table name passed in."
				);
		// Normalize to lower case if not case sensitive
		// if( ! mCasen )
		//	inTableName = inTableName.toLowerCase();

		DBTableDef answer = null;

		// Return the answer if we've already done this table name
		if( cTableDefCache.containsKey( inTableName ) )
		{
			answer = (DBTableDef)cTableDefCache.get( inTableName );
		}
		else
		{
			// figure out the path to use
			String lURI = NIEUtil.trimmedStringOrNull( optXMLURI );
			if( lURI == null )
				lURI = generateDefaultSchemaURI( inTableName );
			
			// call the constructor
			answer = new DBTableDef( inTableName, lURI, inDBConfig );

			// store the results in the cache hash
			cTableDefCache.put( inTableName, answer );
		}

		// And we're done
		// any exceptions will have already been thrown above		
		return answer;

	}

	private DBConfig getDBConfig()
	{
		return fDBConf;
	}

	public String getTableName()
	{
		return fTableNameV1;
	}


	public void dropAndReinstantiateTableInDatabase()
		throws DBConfigException
	{
		final String kFName = "dropAndReinstantiateTableInDatabase";
		try {
			dropTableFromDatabase( false );
		}
		catch( Exception e ) {
			warningMsg( kFName,
				"Drop commands did not, but perhaps because the tables didn't exist."
				+ " Will continue attempting to add."
				);
		}
		instantiateTableInDatabase();
	}

	public void instantiateTableInDatabase()
		throws DBConfigException
	{
		final String kFName = "instantiateTableInDatabase";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String lVendor = getDBConfig().getConfiguredVendorTag();
		if( null == lVendor )
			throw new DBConfigException( kExTag
				+ "A vendor tag is required in order to generate database tables."
				);

		if( null == fMainNode )
			throw new DBConfigException( kExTag
				+ "Null XML table definition."
				);


		// create the parameters hash
		Hashtable lParamsHash = new Hashtable();
		// We need to tell the style sheet which flavor of SQL to output
		lParamsHash.put( "db", lVendor );

	
		Document statements = null;
		try
		{
			statements = JDOMHelper.xsltElementToDoc(
				fMainNode.getJdomElement(),
				CREATE_TABLE_XSLT_SHEET,
				lParamsHash,
				false
				);
		}
		catch( JDOMHelperException e )
		{
			throw new DBConfigException( kExTag
				+ "Unable to generate SQL statements to create table"
				+ " \"" + getTableName() + "\"."
				+ " Reason: " + e
				);
		}

		statusMsg( kFName,
				"Creating database table \"" + getTableName() + "\"."
			);
		// For now, show it
		// statusMsg( kFName,
		//	JDOMHelper.JDOMToString( statements, true )
		//	);

		boolean success = getDBConfig().runXmlSqlStatements(
			statements.getRootElement()
			);
		if( ! success )
			throw new DBConfigException( kExTag
				+ "The generated SQL statements did not run correctly"
				+ " for \"" + getTableName() + "\"."
				+ " Statements: "
				+ JDOMHelper.JDOMToString( statements, true )
				);

	}


	public void dropTableFromDatabase( boolean inIgnoreErrors )
		throws DBConfigException
	{
		final String kFName = "dropTableFromDatabase";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String lVendor = getDBConfig().getConfiguredVendorTag();
		if( null == lVendor )
			throw new DBConfigException( kExTag
				+ "A vendor tag is required in order to generate database tables."
				);

		if( null == fMainNode )
			throw new DBConfigException( kExTag
				+ "Null XML table definition."
				);


		// create the parameters hash
		Hashtable lParamsHash = new Hashtable();
		// We need to tell the style sheet which flavor of SQL to output
		lParamsHash.put( "db", lVendor );
		lParamsHash.put( "ignore_error", (inIgnoreErrors ? "TRUE" : "FALSE") );
		// ^^^ the xslt param is singular
	
		Document statements = null;
		try
		{
			statements = JDOMHelper.xsltElementToDoc(
				fMainNode.getJdomElement(),
				DROP_TABLE_XSLT_SHEET,
				lParamsHash,
				false
				);
		}
		catch( JDOMHelperException e )
		{
			throw new DBConfigException( kExTag
				+ "Unable to generate SQL statements to drop table"
				+ " \"" + getTableName() + "\"."
				+ " Reason: " + e
				);
		}

		statusMsg( kFName,
				"Dropping database table \"" + getTableName() + "\"."
			);
		// For now, show it
		// statusMsg( kFName,
		//	JDOMHelper.JDOMToString( statements, true )
		//	);

		boolean success = getDBConfig().runXmlSqlStatements(
			statements.getRootElement()
			);
		if( ! success )
			throw new DBConfigException( kExTag
				+ "The generated SQL statements did not run correctly"
				+ " for \"" + getTableName() + "\"."
				+ " Statements: "
				+ JDOMHelper.JDOMToString( statements, true )
				);

	}





	private void initAndPopulateFieldCache()
		throws DBConfigException
	{
		final String kFName = "initAndPopulateFieldCache";
		final String kExTag = kClassName + '.' + kFName + ": ";

		List fields = fMainNode.findElementsByPath( FIELD_PATH );
		// Sanity check
		if( null == fields || fields.size() < 1 )
			throw new DBConfigException( kExTag
				+ "No fields defined for this table."
				);

		// The list of implemented fields
		cImplementedFieldCache = new Hashtable();
		// cImplementedFieldList = new Vector();
		// the list of UNimplemented fields, just for error message sake, etc.
		cUnimplementedFieldCache = new Hashtable();
		// The list of all fields referenced
		cAllFields = new Hashtable();
		// To save time, we also cache the integer field type
		cFieldTypeCache = new Hashtable();
		// To save time, the name of any associated CGI field
		cCGIFieldNameCache = new Hashtable();
		// Requried
		cIsRequiredFieldCache = new Hashtable();
		// Indexed field
		cIsIndexedFieldCache = new Hashtable();

		// Some lists of fields to cache set-related gets
		// This is faster at run time, and ALSO much easier to impelement!
		cAllFieldsList = new Vector();
		cAllImplementedFieldsList = new Vector();
		cAllUnimplementedFieldsList = new Vector();
		cRequiredFieldsList = new Vector();
		cIndexedFieldsList = new Vector();

		// TODO: cache other items (?)

		// Loop through the field tags
		for( int i = 0; i < fields.size(); i++ )
		{
			// Get the field
			Element lField = (Element)fields.get( i );

			// Get the field's name
			String lFieldName = JDOMHelper.getStringFromAttributeTrimOrNull( lField, NAME_ATTR );
			// Sanity check
			if( null == lFieldName )
				throw new DBConfigException( kExTag
					+ "Null/empty field name for field # " + (i+1)
					+ " from config URI \"" + fConfigURI + "\""
					);
			// Normalize
			lFieldName = normalizeFieldName( lFieldName );
			// Sanity check 2
			if( null == lFieldName )
				throw new DBConfigException( kExTag
					+ "Invalid field name for field # " + (i+1)
					+ " from config URI \"" + fConfigURI + "\""
					);

			// Double check for dupes
			if( cAllFields.containsKey( lFieldName ) )
				throw new DBConfigException( kExTag
					+ "Duplicate field name \"" + lFieldName + "\" found at field # " + (i+1)
					+ " from config URI \"" + fConfigURI + "\""
					);
			// OK, we've seen it, store that fact immediately
			cAllFields.put( lFieldName, lField );
			// We'd like to know all the fields (and in order?)
			cAllFieldsList.add( lFieldName );

			// Is it even implemented?
			boolean isImplemented = JDOMHelper.getBooleanFromAttribute(
				lField, IMPLEMENTED_ATTR, DEFAULT_IMPLEMENTED_ASSUMPTION
				);

			debugMsg( kFName, "field \"" + lFieldName + "\" isImpl=" + isImplemented );

			if( ! isImplemented )
			{
				cAllUnimplementedFieldsList.add( lFieldName );
				// We do need to remember these
				cUnimplementedFieldCache.put( lFieldName, lField );
				infoMsg( kFName,
					// "Skipping unimplemented field \"" + lFieldName + "\"."
					// + " Future operations involving this field will produce warning messages."
					"Encountered unimplemented field \"" + lFieldName + "\"."
					+ " Future operations involving this field will produce warning messages."
					+ " Values set for this field will not be send to the database."
					);
				// continue;
			}
			else
			{
				cAllImplementedFieldsList.add( lFieldName );
				cImplementedFieldCache.put( lFieldName, lField );
				// cImplementedFieldList.add( lFieldName );
			}

			// So we will continue on

			// The type is also required
			String lFieldType = JDOMHelper.getStringFromAttributeTrimOrNull(
				lField, TYPE_ATTR
				);
			// Sanity check
			if( null == lFieldType )
				throw new DBConfigException( kExTag
					+ "Null/empty field type for field \"" + lFieldName + "\", # " + (i+1)
					+ " from config URI \"" + fConfigURI + "\""
					);
			// Convert to a fixed int
			int lFieldTypeInt = fieldTypeStringToInt( lFieldType );
			// Double check for dupes
			if( lFieldTypeInt <= FIELD_TYPE_INVALID )
				throw new DBConfigException( kExTag
					+ "Invalid field type \"" + lFieldType + "\""
					);

			// Get any possible CGI field name
			String cgiFieldName = JDOMHelper.getStringFromAttributeTrimOrNull(
				lField, CGI_NAME_ATTR
				);

			// Cache a few other bits of into

			// Required / not null fields
			Boolean lRequiredObj = new Boolean(
				JDOMHelper.getBooleanFromAttribute(
					lField, REQUIRED_ATTR, DEFAULT_REQUIRED_ASSUMPTION
					)
				);
			if( lRequiredObj.booleanValue() )
				cRequiredFieldsList.add( lFieldName );

			// Indexed fields
			Boolean lIndexedObj = new Boolean(
				JDOMHelper.getBooleanFromAttribute(
					lField, INDEXED_ATTR, DEFAULT_INDEXED_ASSUMPTION
					)
				);
			if( lIndexedObj.booleanValue() )
				cIndexedFieldsList.add( lFieldName );

			// It's pretty darn odd to have an indexed field that is
			// not required.  I'm not sure we should reject it completely,
			// but they certainly should be warned
			// Todo: revisit, should this be an exception?
			if( lIndexedObj.booleanValue() && ! lRequiredObj.booleanValue() )
			{
				infoMsg( kFName,
					"Field \"" + lFieldName + "\" is marked as indexed"
					+ " but is NOT marked as REQUIRED"
					+ ".  This is a bit odd, some databases may not allow this."
					+ " You may want to double check this table definition."
					);
			}

			// TODO: Add in support for primary key

			// OK, we're happy, cache the rest of the values
			// cFieldCache.put( lFieldName, lField );
			cFieldTypeCache.put( lFieldName, new Integer(lFieldTypeInt) );
			cIsRequiredFieldCache.put( lFieldName, lRequiredObj );
			cIsIndexedFieldCache.put( lFieldName, lIndexedObj );
			if( null != cgiFieldName )
				cCGIFieldNameCache.put( lFieldName, cgiFieldName );

			// TODO: cache other items (like what?)
		}

		// not much else to say, we're done, any problems we would have bailed	
	}

	public int getFieldType( String inFieldName )
		throws DBConfigException
	{
		final String kFName = "getFieldType";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Sanity checks
		inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);
		if( ! hasField( inFieldName, true ) )
			throw new DBConfigException( kExTag
				+ "Invalid field name \"" + inFieldName + "\""
				);
		if( hasUnimplementedField( inFieldName, true ) )
			warningMsg( kFName,
				"Unimplemented field \"" + inFieldName + "\" was referenced."
				);

		// Get the object and return the int value
		Integer lObj = (Integer)cFieldTypeCache.get( inFieldName );
		return lObj.intValue();

	}

	public String getCGIFieldNameOrNull( String inFieldName )
	{
		String answer = null;
		try
		{
			answer = getCGIFieldName( inFieldName );
		}
		catch( DBConfigException e )
		{
			answer = null;
		}
		return answer;
	}


	public String getCGIFieldName( String inFieldName )
		throws DBConfigException
	{
		final String kFName = "getCGIFieldName";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Sanity checks
		inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);
		if( ! hasField( inFieldName, true ) )
			throw new DBConfigException( kExTag
				+ "Invalid field name \"" + inFieldName + "\""
				);
		if( hasUnimplementedField( inFieldName, true ) )
			warningMsg( kFName,
				"Unimplemented field \"" + inFieldName + "\" was referenced."
				);

		// Get the object and return it
		return (String)cCGIFieldNameCache.get( inFieldName );

	}

	public boolean getIsRequired( String inFieldName )
		throws DBConfigException
	{
		final String kFName = "getIsRequired";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Sanity checks
		inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);
		if( ! hasField( inFieldName, true ) )
			throw new DBConfigException( kExTag
				+ "Invalid field name \"" + inFieldName + "\""
				);
		if( hasUnimplementedField( inFieldName, true ) )
			warningMsg( kFName,
				"Unimplemented field \"" + inFieldName + "\" was referenced."
				);

		// Get the object and return the boolean value
		Boolean resultObj = (Boolean)cIsRequiredFieldCache.get( inFieldName );
		return resultObj.booleanValue();
	}

	public boolean getIsIndexed( String inFieldName )
		throws DBConfigException
	{
		final String kFName = "getIsIndexed";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Sanity checks
		inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);
		if( ! hasField( inFieldName, true ) )
			throw new DBConfigException( kExTag
				+ "Invalid field name \"" + inFieldName + "\""
				);
		if( hasUnimplementedField( inFieldName, true ) )
			warningMsg( kFName,
				"Unimplemented field \"" + inFieldName + "\" was referenced."
				);

		// Get the object and return the boolean value
		Boolean resultObj = (Boolean)cIsIndexedFieldCache.get( inFieldName );
		return resultObj.booleanValue();
	}

	// Todo: should have a getDescription and also use it in error messages

	public List getAllFieldNames()
	{
		return cAllFieldsList;
	}
	public List getImplementedFieldNames()
	{
		return cAllImplementedFieldsList;
	}
	public List getUnimplementedFieldNames()
	{
		return cAllUnimplementedFieldsList;
	}
	public List getRequiredFieldNames()
	{
		return cRequiredFieldsList;
	}
	public List getIndexedFieldNames()
	{
		return cIndexedFieldsList;
	}



	public static int fieldTypeStringToInt( String inType )
	{
		final String kFName = "fieldTypeStringToInt";
		inType = NIEUtil.trimmedLowerStringOrNull( inType );
		if( null == inType )
		{
			errorMsg( kFName, "Null/empty type passed in." );
			return FIELD_TYPE_INVALID;
		}
		// Boolean
		if( inType.equals( FIELD_TYPE_BOOLEAN_STR ) )
			return FIELD_TYPE_BOOLEAN;
		// Integer / int
		else if( inType.equals( FIELD_TYPE_INTEGER_STR ) )
			return FIELD_TYPE_INTEGER;
		// Long
		else if( inType.equals( FIELD_TYPE_LONG_STR ) )
			return FIELD_TYPE_LONG;
		// Text
		else if( inType.equals( FIELD_TYPE_TEXT_STR ) )
			return FIELD_TYPE_TEXT;
		// Timestamp / date / time
		else if( inType.equals( FIELD_TYPE_DATETIME_STR ) )
			return FIELD_TYPE_DATETIME;

		// Todo: add support for other types

		// else we don't know
		else
		{
			errorMsg( kFName, "Unknown field type \"" + inType + "\"" );
			return FIELD_TYPE_INVALID;
		}

	}

	// no warnings, if you send in null you'll get null back
	// For now, static, so not settable on a per table basis
	public static String normalizeFieldName( String inName )
	{
		if( ! mCasen )
			return NIEUtil.trimmedLowerStringOrNull( inName );
		else
			return NIEUtil.trimmedStringOrNull( inName );
	}
	// For now, save logic as field name
	public static String normalizeTableName( String inName )
	{
		return normalizeFieldName( inName );
//		if( ! mCasen )
//			return NIEUtil.trimmedLowerStringOrNull( inName );
//		else
//			return NIEUtil.trimmedStringOrNull( inName );
	}



	public void loadCsvDataIntoTable( String inFullDataURI,
		String optDefaultURIBaseDirectory,
		boolean inDoOverwrite
		)
		throws DBConfigException, IOException
	{
		final String kFName = "loadCsvDataIntoTable";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String tableName = getTableName();

		if( ! getDBConfig().verifyASpecificDBTable( tableName, false, false ) )
			throw new DBConfigException( kExTag +
				"No such table \"" + tableName + "\""
				);

		// Default the name of CSV file
		if( null==inFullDataURI )
			inFullDataURI = generateDefaultDataURI( tableName, optDefaultURIBaseDirectory );

		// Will throw IO if not found, which may be OK
		Vector lines = NIEUtil.fetchURIContentsLines(
			inFullDataURI,	// String inBaseName,
			null,	// String optRelativeRoot,
			null,	// String optUsername,
			null,	// String optPassword,
			true,	// boolean inDoTrim,
			true,	// boolean inDoSkipBlankLines,
			null,	// AuxIOInfo inoutAuxIOInfo,
			false
			);

		// At this point we do appear to have data, so nuke any
		// existing data if we were asked to do so
		if( inDoOverwrite ) {
			// String cmd = "DELETE * from " + tableName;
			String cmd = "DELETE FROM " + tableName;
			getDBConfig().executeStatementOrFalse( cmd, true );
			/*** NO, the return value is not success/failure, it's resultsset or not
			 you get an EXCEPTION if it's not happy
			if( ! getDBConfig().executeStatementOrFalse( cmd ) )
				throw new DBConfigException( kExTag +
					"Unable to purge data in table \"" + tableName + "\""
					+ " (results status was false)"
					);
			***/
		}


		Iterator it = lines.iterator();
		if( ! it.hasNext() ) {
			warningMsg( kFName, "Empty record set for table \"" + tableName + "\"" );
			return;
		}
		String line0 = (String)it.next();
		List fieldNames = null;
		// If the first line starts with #, then it's a list of field names
		if( line0.startsWith("#") ) {
			if( line0.length() < 2 )
				throw new DBConfigException( kExTag +
					"Invalid first line \"" + line0 + "\" for table \"" + tableName + "\" in file \"" + inFullDataURI + "\""
					);
			// Drop the #
			line0 = line0.substring( 1 );
			fieldNames = NIEUtil.parseCSVLine( line0 );
			if( null==fieldNames || fieldNames.isEmpty() )
				throw new DBConfigException( kExTag +
					"No field names on first # line \"" + line0 + "\" for table \"" + tableName + "\" in file \"" + inFullDataURI + "\""
					);
			line0 = null;

			// Normalize the field names
			Vector newFields = new Vector();
			for( Iterator oldFnameIt = fieldNames.iterator() ; oldFnameIt.hasNext() ; ) {
				String oldName = (String)oldFnameIt.next();
				String newName = normalizeFieldName( oldName );
				if( null==newName )
					throw new DBConfigException( kExTag +
						"Invalid field name \"" + oldName + "\" on first # line \"" + line0 + "\" for table \"" + tableName + "\" in file \"" + inFullDataURI + "\""
						);
				newFields.add( newName );
			}
			fieldNames = newFields;

			// Check field names
			HashSet seenFields = new HashSet();
			for( Iterator fnameIt = fieldNames.iterator() ; fnameIt.hasNext() ; ) {
				String checkName = (String)fnameIt.next();
				if( seenFields.contains(checkName) )
					throw new DBConfigException( kExTag +
						"Duplicate field name \"" + checkName + "\" on first # line \"" + line0 + "\" for table \"" + tableName + "\" in file \"" + inFullDataURI + "\""
						);
				seenFields.add( checkName );
				if( ! hasField( checkName, true ) )
					throw new DBConfigException( kExTag +
						"Unknown field name \"" + checkName + "\" on first # line \"" + line0 + "\" for table \"" + tableName + "\" in file \"" + inFullDataURI + "\""
						+ " Valid field names: " + getAllFieldNames()
						);
			}
		}
		// Else no field names defined, assume we want all of them
		else {
			fieldNames = getAllFieldNames();
		}

		// Process the rest of the lines
		int lineCounter = 1;
		int recordCounter = 0;
		while( line0!=null || it.hasNext() ) {
			String thisLine = null;
			// Remember to process the intial line as values if it was NOT field names
			if( null!=line0 ) {
				thisLine = line0;
				line0 = null;
			}
			else {
				thisLine = (String)it.next();
				lineCounter++;
			}

			// Get the values
			Vector values = NIEUtil.parseCSVLine( thisLine );

			// Some sanity checking
			if( null==values || values.isEmpty() ) {
				errorMsg( kFName,
					"No values on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Will continue reading any subsequent lines."
					+ " Line=\"" + thisLine + "\""
					);
				continue;
			}
			if( values.size() != fieldNames.size() ) {
				errorMsg( kFName,
					"Incorrect number of values on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Expected " + fieldNames.size() + ", but got " + values.size()
					+ " Will continue reading any subsequent lines."
					+ " Line=\"" + thisLine + "\""
					+ ", Values=" + values
					);
				continue;
			}

			// add the values into an update statement
			DBUpdateStmt statement = new DBUpdateStmt(
				tableName, getDBConfig()
				);
			for( int i=0; i<fieldNames.size() ; i++ ) {
				String fieldName = (String)fieldNames.get(i);
				String fieldValue = (String)values.get(i);
				if( null!=fieldValue && fieldValue.length() > 0 )
					statement.setValue( fieldName, fieldValue );
			}
			// Send the update
			try {
				statement.sendUpdate( getDBConfig().getVendorNeedsCommitByDefault() );
				recordCounter++;
			}
			catch( Exception e ) {
				throw new DBConfigException( kExTag +
					"Error loading row # " + lineCounter
					+ " for table \"" + tableName + "\" from file \"" + inFullDataURI + "\""
					+ " Line=\"" + thisLine + "\""
					+ " Error: " + e
					);
			}
		}	// End for each line of the file

		if( recordCounter < 1 )
			warningMsg( kFName,
				"No records loaded for table \"" + tableName + "\" from file \"" + inFullDataURI + "\""
				);
		else
			statusMsg( kFName,
				"" + recordCounter + " records loaded into table \"" + tableName + "\" from CSV file \"" + inFullDataURI + "\""
				);

	}


	public void saveCsvDataToFile( String optFullDataURI,
		String optDefaultURIBaseDirectory
		)
		throws DBConfigException, IOException
	{
		final String kFName = "saveCsvDataToFile";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String tableName = getTableName();

		if( ! getDBConfig().verifyASpecificDBTable( tableName, false, false ) )
			throw new DBConfigException( kExTag +
				"No such table \"" + tableName + "\""
				);

		if( null==optFullDataURI && null==optDefaultURIBaseDirectory )
			throw new DBConfigException( kExTag +
				"Must set at least one of Full URI or Export Dictory, table was \"" + tableName + "\""
				);

		// Default the name of CSV file
		if( null==optFullDataURI )
			optFullDataURI = generateDefaultDataURI( tableName, optDefaultURIBaseDirectory );

		String firstField = null;
		List fieldNames = new Vector();
		StringBuffer cmdBuff = new StringBuffer( "SELECT " );
		for( Iterator it = getImplementedFieldNames().iterator() ; it.hasNext() ; ) {
			String fieldName = (String) it.next();
			if( null==firstField ) {
				firstField = fieldName;
				fieldNames.add( "#" + fieldName );
			}
			else {
				cmdBuff.append( ", " );
				fieldNames.add( fieldName );
			}
			cmdBuff.append( fieldName );
		}
		if( null==firstField )
			throw new DBConfigException( kExTag +
				"No active/implemented fields in this schema."
				);
		cmdBuff.append( " FROM " );
		cmdBuff.append( tableName );
		cmdBuff.append( " ORDER BY " );
		cmdBuff.append( firstField );
		String cmd = new String( cmdBuff );

		debugMsg( kFName, "cmd=" + cmd );

		// How many values we will expect from the database
		int fieldCount = fieldNames.size();

		// Open the file
		File lFile = new File( optFullDataURI );
		FileOutputStream lStream = new FileOutputStream( lFile );
		BufferedWriter writer = new BufferedWriter(
			new OutputStreamWriter( lStream ), 2048
			);

		// Format and output the header
		String headerLine = NIEUtil.recordToCSVText( fieldNames );
		writer.write( headerLine );

		ResultSet records = null;
		Statement myStatement = null;
		Connection myConnection = null;
		int recordCounter = 0;
		try {

			// Get the data
			Object [] objs = getDBConfig().runQuery( cmd, true );

			records = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];
	
			int lRowCount = 0;
			int lMapCount = 0;

			// For each mapping record
			while( records.next() )
			{
				lRowCount++;
				List fieldValues = new Vector();
				for( int i=1 ; i <= fieldCount ; i++ ) {
					String thisValue = records.getString( i );
					// Nulls are OK from DB, but we can't add true nulls to list
					thisValue = (null!=thisValue ) ? thisValue : "";
					fieldValues.add( thisValue );
				}
				// Format and output the record
				String dataLine = NIEUtil.recordToCSVText( fieldValues );
				writer.write( dataLine );
				recordCounter++;
			}

		}
		catch( SQLException e1 ) {
			throw new DBConfigException( kExTag +
				"Error getting data from the database: " + e1
				);
		}
		catch( DBConfigInServerReconnectWait e2 ) {
			throw new DBConfigException( kExTag +
				"Error connecting to the database: " + e2
				);
		}
		finally {
			records = DBConfig.closeResults( records, kClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName, kFName, false );
			if( null!=writer )
				writer.close();
			if( null!=lStream )
				lStream.close();
		}

		if( recordCounter < 1 )
			warningMsg( kFName,
				"No records exported from table \"" + tableName + "\" to file \"" + optFullDataURI + "\""
				);
		else
			statusMsg( kFName,
				"" + recordCounter + " record(s) exported from table \"" + tableName + "\" to CSV file \"" + optFullDataURI + "\""
				);

	}


	private static String generateDefaultSchemaURI( String inTableName )
		throws DBConfigException
	{
		final String kFName = "generateDefaultSchemaURI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// somewhat redundant sanity checks
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inTableName = normalizeTableName( inTableName );
		if( inTableName == null )
			throw new DBConfigException( kExTag
				+ "Null table name passed in."
				);
		// Normalize to lower case if not case sensitive
		// if( ! mCasen )
		//	inTableName = inTableName.toLowerCase();

		// Form the name and return it
		return DEFAULT_URI_SCHEMA_PREFIX
			+ inTableName
			+ DEFAULT_URI_SCHEMA_SUFFIX
			;

	}

	private static String generateDefaultDataURI(
			String inTableName,
			String optDefaultURIBaseDirectory
		)
		throws DBConfigException
	{
		final String kFName = "generateDefaultDataURI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// somewhat redundant sanity checks
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inTableName = normalizeTableName( inTableName );
		if( inTableName == null )
			throw new DBConfigException( kExTag
				+ "Null table name passed in."
				);
		// Normalize to lower case if not case sensitive
		// if( ! mCasen )
		//	inTableName = inTableName.toLowerCase();

		optDefaultURIBaseDirectory = NIEUtil.trimmedStringOrNull( optDefaultURIBaseDirectory );
		if( null==optDefaultURIBaseDirectory )
			optDefaultURIBaseDirectory = DEFAULT_URI_SYSTEM_DATA_PREFIX;
		// We know this is a directory, so make sure it has the correct
		// tailing slash
		String sep = File.separator;
		if( null!=optDefaultURIBaseDirectory
			&& ! optDefaultURIBaseDirectory.endsWith(sep)
			// we also check for the standard suspects
			&& ! optDefaultURIBaseDirectory.endsWith("/")
			&& ! optDefaultURIBaseDirectory.endsWith("\\")
			)
		{
			optDefaultURIBaseDirectory += sep;
		}

		// Form the name and return it
		// return DEFAULT_URI_SAMPLE_DATA_PREFIX
		//	+ inTableName
		//	+ DEFAULT_URI_SAMPLE_DATA_SUFFIX
		//	;

		return optDefaultURIBaseDirectory
			+ inTableName
			+ DEFAULT_URI_SAMPLE_DATA_SUFFIX
			;

	}


	// Does the table have a certain field
	// by default we DO normalize
	public boolean hasField( String inFieldName )
	{
		return hasField( inFieldName, false );
	}
	public boolean hasImplementedField( String inFieldName )
	{
		return hasImplementedField( inFieldName, false );
	}
	public boolean hasUnimplementedField( String inFieldName )
	{
		return hasUnimplementedField( inFieldName, false );
	}
	// An opportunity to not normalize if that has just been done
	// by the caller, don't lie to us!
	public boolean hasField( String inFieldName, boolean inHasBeenNormalized )
	{
		if( ! inHasBeenNormalized )
			inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName )
			return false;
		// As the other two methods, and tell them we've already normalized
		// the field
		return hasImplementedField( inFieldName, true )
			|| hasUnimplementedField( inFieldName, true )
			;
	}
	public boolean hasImplementedField( String inFieldName, boolean inHasBeenNormalized )
	{
		if( ! inHasBeenNormalized )
			inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName || null == cImplementedFieldCache )
			return false;
		return cImplementedFieldCache.containsKey( inFieldName );
	}
	public boolean hasUnimplementedField( String inFieldName, boolean inHasBeenNormalized )
	{
		if( ! inHasBeenNormalized )
			inFieldName = normalizeFieldName( inFieldName );
		if( null == inFieldName || null == cUnimplementedFieldCache )
			return false;
		return cUnimplementedFieldCache.containsKey( inFieldName	);
	}
	public List getImplmentedFieldNames() {
		return cAllImplementedFieldsList;
	}



	// This gets us to the logging object
	private static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}
	private static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}




	// Our cache of constructed objects
	private static Hashtable cTableDefCache = new Hashtable();
	// A list of "field" nodes, indexed by name
	// This is NOT shared, it's instance-specific
	private Hashtable cImplementedFieldCache;
	private Vector _cImplementedFieldList;
	// a list of UNIMPLEMENTED field objects
	// items in this list cause warnings instead of exceptions
	private Hashtable cUnimplementedFieldCache;

	// ALL fields that were mentioned, if if not implemented
	private Hashtable cAllFields;
	// We cache some other answers
	private Hashtable cFieldTypeCache;
	// The cached names of any CGI field
	// ??? and if we don't cache a name, we'll use other rules
	// to associate one to the other?
	private Hashtable cCGIFieldNameCache;
	// Required field
	private Hashtable cIsRequiredFieldCache;
	// Indexed field
	private Hashtable cIsIndexedFieldCache;

	// Some lists to cache answers that return sets of fields
	private List cAllFieldsList;
	private List cAllImplementedFieldsList;
	private List cAllUnimplementedFieldsList;
	private List cRequiredFieldsList;
	private List cIndexedFieldsList;
	// Todo: add other cache hashes for fields

	// Whether or not field names are case insensitive
	// For now we're saying they are NOT
	// so everything will NORMALIZED to lower case
	private final static boolean mCasen = false;

	// The main tree
	private JDOMHelper fMainNode;

	// The name we got from the constructor
	private String fTableNameV0;
	// the name we read from the config
	private String fTableNameV1;
	// The URI we read the definition from
	private String fConfigURI;
	// The main DB configuration
	private DBConfig fDBConf;

	private final static String DEFAULT_URI_SCHEMA_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "system/db/schema/"
		;

	private final static String DEFAULT_URI_SYSTEM_DATA_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "system/db/sample_data/"
		;

	// private final static String DEFAULT_URI_PREFIX =
	//	AuxIOInfo.SYSTEM_RESOURCE_PREFIX
	//	+ "system.db.schema."
	//	;
	private final static String DEFAULT_URI_SCHEMA_SUFFIX = "_schema.xml";

	private final static String DEFAULT_URI_SAMPLE_DATA_SUFFIX = ".csv";

	private final static String DEFAULT_URI_XSLT_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "system/db/transform/"
		;

	// XSLT system style sheets that generate SQL statements
	// private final static String DROP_AND_CREATE_TABLE_XSLT_SHEET =
	//	DEFAULT_URI_XSLT_PREFIX
	//	+ "gen_sql_drop_and_create_table.xslt";
	private final static String DROP_TABLE_XSLT_SHEET =
		DEFAULT_URI_XSLT_PREFIX
		+ "gen_sql_drop_table.xslt";
	private final static String CREATE_TABLE_XSLT_SHEET =
		DEFAULT_URI_XSLT_PREFIX
		+ "gen_sql_create_table.xslt";


	// The name of the table name attribute
	public final static String NAME_ATTR = "name";
	// The name of the table type attribute
	public final static String TYPE_ATTR = "type";
	// Whether or not the field is implemented
	private final static String IMPLEMENTED_ATTR = "is_implemented";
	// Whether or not the field is implemented
	// private final static String REQUIRED_ATTR = "is_required";
	private final static String REQUIRED_ATTR = "is_not_null";
	// ^^^ TODO: should this be is_required after all?  state in postive?
	// If so, must update ALL schemas
	// Whether or not the field is implemented
	private final static String INDEXED_ATTR = "is_indexed";
	// If a CGI request object is present, what field name would this
	// be listed under?
	private final static String CGI_NAME_ATTR = "cgi_field_name";

	// By default, we assume fields are implemented, unless told otherwise
	private final static boolean DEFAULT_IMPLEMENTED_ASSUMPTION = true;
	// By default, we assume fields are NOT required
	private final static boolean DEFAULT_REQUIRED_ASSUMPTION = false;
	// By default, we assume fields are NOT indexed
	private final static boolean DEFAULT_INDEXED_ASSUMPTION = false;

	// The path to individual fields
	private final static String FIELD_PATH = "field";


	// Predefined types of fields
	public final static int FIELD_TYPE_UNKNOWN = 99;
	public final static int FIELD_TYPE_INVALID = 0;
	public final static int FIELD_TYPE_BOOLEAN = 1;
	public final static String FIELD_TYPE_BOOLEAN_STR = "boolean";
	public final static int FIELD_TYPE_INTEGER = 2;
	public final static String FIELD_TYPE_INTEGER_STR = "int";
	public final static int FIELD_TYPE_LONG = 3;
	public final static String FIELD_TYPE_LONG_STR = "long";
	public final static int FIELD_TYPE_TEXT = 4;
	public final static String FIELD_TYPE_TEXT_STR = "text";
	public final static int FIELD_TYPE_DATETIME = 5;
	public final static String FIELD_TYPE_DATETIME_STR = "timestamp";
	// Todo: add support for more types


}

package nie.core;
import java.util.*;
import java.sql.*;
// import org.jdom.Element;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */

/* Todo items (also with TableDef and DB config )
 * check that REQUIRED fields have been set
 * check for unimplemented fields
 * handle attrib validation so that XSLT for XML to SQL is reliable
 * add more object conversions
 * (strings to xyz, numbers to numbers, etc.)
 * obsess about strings that are too long
 * inherit some behaviors from db config
 * allow for CGI field grabbing
 * revisit casen: field, table, cgi field
 */

public class DBUpdateStmt
{
	public static final String kClassName = "DBUpdateStmt";


	// Update statements are related to table definitions
	// table definitions are related to DB configurations
	// DB configurations get us the actual connections we need
	// to create prepared sql statements
	public DBUpdateStmt( String inTableName, DBConfig inDBConfig )
		throws DBConfigException
	{

		fTable = DBTableDef.getTableDef( inTableName, inDBConfig );

		fValueObjectsHash = new Hashtable();
		fSetFields = new Vector();
		fDBConfig = inDBConfig;
	}

	// Cache a sql connection, this tests things up front
	// before the caller spends a lot of time preparing a statement
	// that can't be logged
	// This is OPTIONAL, if not called, we'll do it ourselves later
	public void cacheConnection()
		throws DBConfigException,
			SQLException,
			DBConfigInServerReconnectWait
	{
		cDBConnection = getDBConfig().getConnection();
	}



	// Gather up all of the value objects and send them!
	public int sendUpdate()
		throws DBConfigException,
			SQLException,
			DBConfigInServerReconnectWait
	{
		return sendUpdate( false );
	}


	// Gather up all of the value objects and send them!
	public int sendUpdate( boolean inDoCommit )
		throws DBConfigException,
			SQLException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "sendUpdate";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );

		PreparedStatement statement = null;
		Connection connection = null;

		// First we check for missing required fields
		List missing = getMissingRequiredFieldNames();
		if( missing!=null && missing.size() > 0 )
		{
			String msg = "Missing required field";
			if( missing.size() == 1 )
				msg += ": \"" + (String)missing.get(0) + "\"";
			else
			{
				msg += "s: " + NIEUtil.printStringListToBufferCompact( missing );
			}
			// no need to close statement, not opened yet
			// no need to close connection, no error, no problem caching
			throw new DBConfigException( kExTag + msg );
		}


		// First we prepare a statement
		// PreparedStatement statement = buildStatement();
		Object [] objs = buildStatement();
		statement = (PreparedStatement)objs[0];
		connection = (Connection)objs[1];

		// Now we fill it in with the objects we have collected
		// for( Iterator it = fSetFields.iterator(); it.hasNext(); )
		// We need a field counter anyway
		// And we want a ONE-based counter
		for( int fieldCounter = 1; fieldCounter <= fSetFields.size(); fieldCounter++ )
		{
			// String lFieldName = (String)it.next();
			// Get the element, correct for ONE-based vs. ZERO-based access
			String lFieldName = (String)fSetFields.get( fieldCounter-1 );
			if(trace)
				traceMsg( kFName, "Field " + fieldCounter + " \"" + lFieldName + "\"" );
			// Sanity check, this should not be possible
			if( ! fValueObjectsHash.containsKey( lFieldName ) )
				throw new DBConfigException( kExTag
					+ "Field name has no associated value object."
					+ " field = \"" + lFieldName + "\""
					);
			// Get the value object
			Object lValueObj = (Object)fValueObjectsHash.get( lFieldName );
			String lObjClass = lValueObj.getClass().getName();
			// Get the type
			int lFieldType = getTableDef().getFieldType( lFieldName );
			// Sanity check, should have already been caught
			if( lFieldType <= DBTableDef.FIELD_TYPE_INVALID ) {
				statement = DBConfig.closeStatement( statement, kClassName, kFName, false );
				// No need to close the cached connection
				throw new DBConfigException( kExTag
					+ "Invalid field type " + lFieldType
					);
			}

			// A general catch-all for class cast exceptions, which
			// we would not expect
			try
			{

				// This is where we do the real work, converting field value
				// objects to sql calls
				switch( lFieldType )
				{

				// Do case braces / indents at same level as switch braces

				// STINGS / text, these are the easiest
				case( DBTableDef.FIELD_TYPE_TEXT ):
				{
					// Use the object's native toString operator
					String tmpStr = "" + lValueObj;
					// We don't actually set unimplemented fields
					// and they would have already got a warning by this point
					if( getTableDef().hasUnimplementedField( lFieldName, true ) )
						break;
					statement.setString( fieldCounter, tmpStr );
					break;
				}

				// BOOLEAN
				// *** NOTE ***
				// NIE currently maps Boolean's to SQL Int's, as zero or one
				case( DBTableDef.FIELD_TYPE_BOOLEAN ):
				{
					// Is is a Boolean?
					if( lObjClass.equals( "java.lang.Boolean" ) )
					{
						Boolean tmpBoolObj = (Boolean)lValueObj;
						boolean boolVal = tmpBoolObj.booleanValue();
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						// Map to int 0 or int 1
						if( boolVal )
							statement.setInt( fieldCounter, 1 );
						else
							statement.setInt( fieldCounter, 0 );
					}
					// Is is a genuine Integer?
					else if( lObjClass.equals( "java.lang.Integer" ) )
					{
						Integer tmpIntObj = (Integer)lValueObj;
						int intVal = tmpIntObj.intValue();
						if( intVal != 0 && intVal != 1 )
							throw new DBConfigException( kExTag
								+ "Unable to convert integer \"" + intVal + "\""
								+ " to boolean, must be exactly 0 or 1"
								);
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setInt( fieldCounter, intVal );
					}
					// Else is it a String?
					else if( lObjClass.equals( "java.lang.String" ) )
					{
						String tmpStringObj = (String)lValueObj;
						try
						{
							boolean answer =
							NIEUtil.decodeBooleanStringWithExceptions(tmpStringObj);
							if( getTableDef().hasUnimplementedField( lFieldName, true ) )
								break;
							if( answer )
								statement.setInt( fieldCounter, 1 );
							else
								statement.setInt( fieldCounter, 0 );
						}
						catch(Exception e)
						{
							throw new DBConfigException( kExTag
								+ "Unable to decode boolean string \"" + tmpStringObj + "\""
								);
						}
					}	// End else is it a string?
					// Else is it a something else we can convert?
					// Todo: could support numbers
					// Else we don't know how to convert it
					else
					{
						throw new DBConfigException( kExTag
							+ "Don't know how to convert value object to boolean."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
					}
					break;
				}	// end case Boolean

				// LONG, will accept long or int
				case( DBTableDef.FIELD_TYPE_LONG ):
				{

					// Is is a genuine long?
					if( lObjClass.equals( "java.lang.Long" ) )
					{
						Long tmpLongObj = (Long)lValueObj;
						long longVal = tmpLongObj.longValue();
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setLong( fieldCounter, longVal );
					}
					// Else is it an Integer?
					else if( lObjClass.equals( "java.lang.Integer" ) )
					{
						Integer tmpIntObj = (Integer)lValueObj;
						long longVal = tmpIntObj.longValue();
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setLong( fieldCounter, longVal );
					}	// End else is it an integer?
					// Else is it a String?
					else if( lObjClass.equals( "java.lang.String" ) )
					{
						String tmpStr = (String)lValueObj;
						long longVal = 0L;
						try
						{
							Long longObj = Long.decode( tmpStr );
							longVal = longObj.longValue();
						}
						catch(NumberFormatException e)
						{
							throw new DBConfigException( kExTag
								+ "Unable to convert String to long."
								+ " String=\"" + tmpStr + "\""
								+ ", for field \"" + lFieldName + "\"."
								+ " Error: " + e
								);
						}
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setLong( fieldCounter, longVal );
					}	// End else is it a String?
					// Else is it a something else we can convert?
					// Todo: could support floats and strings with warnings
					// Else we don't know how to convert it
					else
					{
						throw new DBConfigException( kExTag
							+ "Don't know how to convert value object to long."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
					}
					break;
				}	// End case long


				// INT, will accept only int and String for now
				case( DBTableDef.FIELD_TYPE_INTEGER ):
				{

					// Is is a genuine Integer?
					if( lObjClass.equals( "java.lang.Integer" ) )
					{
						Integer tmpIntObj = (Integer)lValueObj;
						int intVal = tmpIntObj.intValue();
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setInt( fieldCounter, intVal );
					}
					// Else is it a String?
					else if( lObjClass.equals( "java.lang.String" ) )
					{
						String tmpStr = (String)lValueObj;
						int intVal = 0;
						try
						{
							Integer intObj = Integer.decode( tmpStr );
							intVal = intObj.intValue();
						}
						catch(NumberFormatException e)
						{
							throw new DBConfigException( kExTag
								+ "Unable to convert String to integer."
								+ " String=\"" + tmpStr + "\""
								+ ", for field \"" + lFieldName + "\"."
								+ " Error: " + e
								);
						}
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setInt( fieldCounter, intVal );
					}	// End else is it a String?
					// Else is it a Long give a very specific error message
					else if( lObjClass.equals( "java.lang.Long" ) )
					{
						throw new DBConfigException( kExTag
							+ "Can't down-convert long to int."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);

					}	// End else is it a long?
					// Else is it a something else we can convert?
					// Todo: could support floats and strings with warnings
					// Else we don't know how to convert it
					else
					{
						throw new DBConfigException( kExTag
							+ "Don't know how to convert value object to int."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
					}
					break;
				}	// End case int


				// Dates and times
				case( DBTableDef.FIELD_TYPE_DATETIME ):
				{

					// It is a genuine SQL date / time timestamp?
					if( lObjClass.equals( "java.sql.Timestamp" ) )
					{
						Timestamp tmpTimeObj = (Timestamp)lValueObj;
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setTimestamp( fieldCounter, tmpTimeObj );
					}

					// It's a regular Java object
					else if( lObjClass.equals( "java.util.Date" ) )
					{
						java.util.Date tmpDateObj = (java.util.Date)lValueObj;
						Timestamp tmpTimeObj = new Timestamp( tmpDateObj.getTime() );
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setTimestamp( fieldCounter, tmpTimeObj );
					}
					
					// Else is it a String?
					else if( lObjClass.equals( "java.lang.String" ) )
					{
						String dateStr = (String) lValueObj;
						java.util.Date tmpDateObj = NIEUtil.stringToDate( dateStr );
						Timestamp tmpTimeObj = new Timestamp( tmpDateObj.getTime() );
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setTimestamp( fieldCounter, tmpTimeObj );

						/***
						throw new DBConfigException( kExTag
							+ "Date parsing of strings to dates/times is currently unsupported."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
						***/
					}	// End else is it a String?
					// Else is it a Long give a very specific error message
					else if( lObjClass.equals( "java.lang.Long" ) )
					{
						Long longObj = (Long) lValueObj;
						Timestamp tmpTimeObj = new Timestamp( longObj.longValue() );
						if( getTableDef().hasUnimplementedField( lFieldName, true ) )
							break;
						statement.setTimestamp( fieldCounter, tmpTimeObj );

						/***
						throw new DBConfigException( kExTag
							+ "Can't down-convert long to date (not yet implemented)."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
						***/
					}
					// Else is it a something else we can convert?
					// Todo: could support floats and strings with warnings
					// and longs with time since epoch
					// Else we don't know how to convert it
					else
					{
						throw new DBConfigException( kExTag
							+ "Don't know how to convert value object to timestamp."
							+ " Object was of type \"" + lObjClass + "\""
							+ " which was \"" + lValueObj + "\""
							+ ", for field \"" + lFieldName + "\""
							+ " which was configured as type-code " + lFieldType
							);
					}
					break;
				}	// End case timestamp



				// Else we don't know what to do
				default:
				{
					throw new DBConfigException( kExTag
						+ "Don't know how to convert this value object"
						+ " for field \"" + lFieldName + "\""
						+ ", which was configured as type-code " + lFieldType + "."
						+ " Value Object was of type \"" + lObjClass + "\""
						+ " which was \"" + lValueObj + "\""
						);
				}
				

				}	// End switch on field integer code
				// Note: Switch and case braces at same level


			}
			catch( Exception e )
			{
				statement = DBConfig.closeStatement( statement, kClassName, kFName, false );
				throw new DBConfigException( kExTag
					+ "Problem mapping value object to field type."
					+ " field = \"" + lFieldName + "\""
					+ ", object type = \"" + lObjClass + "\""
					+ ", object value = \"" + lValueObj + "\""
					+ ", configured field type code = " + lFieldType
					+ " Error: " + e
					);
			}

		}	// End of for each field with a value

		// Now run the thing

		// We catch and rethrow the SQL exception so we can tell the
		// main database config object about it
		int numRows = 0;
		try
		{
			numRows = statement.executeUpdate();
			if( inDoCommit ) {
				Connection lConn = statement.getConnection();
				if( null!=lConn )
					lConn.commit();
				else
					errorMsg( kFName,
						"Got null connection from statement, so no commit, program will continue."
						);
			}
			// statement.close();
		}
		catch(SQLException sqle2)
		{
			// Make sure we un-cache the connection
			cDBConnection = null;
			// Tell the main database object about it
			errorMsg( kFName,
				"Got an error executing statement"
				+ ", reporting to central database object."
				+ " Error: " + sqle2
				);
			getDBConfig().reportError();
			// Re-throw the esception
			throw new SQLException( kExTag + ": " + sqle2 );
		}
		finally {
			statement = DBConfig.closeStatement( statement, kClassName, kFName, false );
			// No need to close cached connection
		}

		debugMsg( kFName, "Finishing up." );

		// The above throws SQLException, but we also double check the results
		if( numRows != 1 )
		{
			// Make sure we un-cache the connection
			// cDBConnection = DBConfig.closeConnection( cDBConnection, kClassName, kFName, false );
			warningMsg( kFName,
				"Insert statement did not return expected row count of 1;"
				+ " actual count = " + numRows
				);
			// not calling DBConfig .reportError
		}

		return numRows;

	}

	// Some convenience versions for native types
	// If you want to allow overwrite, you have to use the Object version
	// yourself and pass in the flag
	public void setValue( String inFieldName, int inValue )
		throws DBConfigException
	{
		setValue( inFieldName, new Integer( inValue ) );
	}

	public void setValue( String inFieldName, long inValue )
		throws DBConfigException
	{
		setValue( inFieldName, new Long( inValue ) );
	}

	public void setValue( String inFieldName, boolean inValue )
		throws DBConfigException
	{
		setValue( inFieldName, new Boolean( inValue ) );
	}


	public void setValue( String inFieldName, Object inValue )
		throws DBConfigException
	{
		setValue( inFieldName, inValue, false );
	}
	public void setValue( String inFieldName, Object inValue, boolean inOverwriteOK )
		throws DBConfigException
	{
		final String kFName = "setValue";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Some sanity checks
		if( null == inValue )
			throw new DBConfigException( kExTag
				+ "Null value passed in, nothing to store."
				+ " If you are trying to clear the value, call clearValue instead."
				);
		inFieldName = DBTableDef.normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in, no way to store value."
				);
		// Check if the field exists, and tell it we've already normalized it
		if( ! getTableDef().hasField( inFieldName, true ) ) 
			throw new DBConfigException( kExTag
				+ "Attempt to set value for unknown field \"" + inFieldName + "\"."
				);
		// we need this answer later so store in a boolean
		boolean alreadyHasThatField = fValueObjectsHash.containsKey( inFieldName );
		if( ! inOverwriteOK && alreadyHasThatField )
			throw new DBConfigException( kExTag
				+ "Attempt to overwrite previously set field value"
				+ " for field \"" + inFieldName + "\"."
				+ " Call with overwrite flag set, or call clearValue first."
				);

		// OK, go ahead and set the value
		fValueObjectsHash.put( inFieldName, inValue );
		// also store the name in the list if it's not there already
		if( ! alreadyHasThatField )
			fSetFields.add( inFieldName );

		// And a warning msg about unimplemented fields
		if( getTableDef().hasUnimplementedField( inFieldName, true ) )
			warningMsg( kFName,
				"Unimplemented field \"" + inFieldName + "\" had a value set."
				+ " This value will not be sent to the database."
				);

	}

	// conceptually similar to calling setValue
	// except that we can also pull values out of a CGI variable
	// which we normally pull from the table definition, if present.
	// In order to work well, the underlying code in sendUpdate() should know
	// how to convert STRINGS (from the CGI fields) into any non-text database
	// fields you have.
	//
	// Default behaviors in shorter method version:
	//
	// By default we do NOT auto-generate a CGI field name nor do we allow
	// you to override what's configured.
	//
	// By default we do NOT complain about missing CGI field values, we treat
	// this as a fairly normal occurance.
	//
	// If the CGI field is missing AND you gave us a NULL default
	// we do NOT complain by default.  The default value is OPTIONAL.
	// We won't set anything and won't complain.
	//
	// Todo: We could allow for clearing fields, but not without a flag.
	//
	// By default we DO complain if there is no CGI data structure at all.
	//
	public void setValueFromCGI_MissingIsOK(
		String inDBFieldName,
		AuxIOInfo inCGIData
		// ,
		// String optSpecificCGIFieldName
		)
		throws DBConfigException
	{
		setValueFromCGIOrDefault(
			inDBFieldName,
			inCGIData,
			null,	// optDefaultValue,
			false,	// inOverwriteOK,
			false,	// inAutoGenerateDefaultCGIFieldName,
			null,	// optAutoGeneratePrefix,
			null,	// optAutoGenerateSuffix,
			null,	// optSpecificCGIFieldName,	// optOverrideWithSpecificCGIFieldName,
			true,	// inErrorOnNullCGIObject,
			false,	// inErrorOnMissingCGIField
			false	// inErrorOnAlsoMissingDefaultValue
			);
	}

	// This version DOES complain if the CGI field is not found AND
	// no default was passed in
	public void setValueFromCGIOrDefault(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		Object optDefaultValue,
		boolean inOverwriteOK
		)
		throws DBConfigException
	{
		setValueFromCGIOrDefault(
			inDBFieldName,
			inCGIData,
			optDefaultValue,
			inOverwriteOK,
			false,	// inAutoGenerateDefaultCGIFieldName,
			null,	// optAutoGeneratePrefix,
			null,	// optAutoGenerateSuffix,
			null,	// optOverrideWithSpecificCGIFieldName,
			true,	// inErrorOnNullCGIObject,
			false,	// inErrorOnMissingCGIField
			true	// inErrorOnAlsoMissingDefaultValue
			);
	}
	// The LONG fancy version
	// See method logic, there are some invalid conbinations of options
	public void setValueFromCGIOrDefault(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		Object optDefaultValue,
		boolean inOverwriteOK,
		boolean inAutoGenerateDefaultCGIFieldName,
		String optAutoGeneratePrefix,
		String optAutoGenerateSuffix,
		String optOverrideWithSpecificCGIFieldName,
		boolean inErrorOnNullCGIObject,
		boolean inErrorOnMissingCGIField,
		boolean inErrorOnAlsoMissingDefaultValue
		)
		throws DBConfigException
	{
		final String kFName = "setValueFromCGIOrDefault";
		final String kExTag = kClassName + '.' + kFName + ": ";

		inDBFieldName = NIEUtil.trimmedStringOrNull( inDBFieldName );
		if( null == inDBFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);

		// And it's an error to call us with no CGI data AND no default value
		if( null == inCGIData && null == optDefaultValue )
			throw new DBConfigException( kExTag
				+ "Null CGI field data AND no default value passed in."
				);

		// Another case that does not make sense
		// We're told to require that a cgi field be present, but
		// we're also given a default value if it is not.
		if( inErrorOnMissingCGIField && null!=optDefaultValue )
			throw new DBConfigException( kExTag
				+ "Inconsistency: told to REQUIRE that individual CGI fields"
				+ " be present, but also given a default value to use if they"
				+ " are mssing"
				+ "; the default value could never be used."
				);

		// Done error checking, let's get down to work

		// Lookup field from CGI data item
		// This also performs quite a few other checks
		// This also throws DBConfigExceptions for a number of reasons
		String strVal = getStringFromCGI(
			inDBFieldName,
			inCGIData,
			inAutoGenerateDefaultCGIFieldName,
			optAutoGeneratePrefix,
			optAutoGenerateSuffix,
			optOverrideWithSpecificCGIFieldName,
			inErrorOnNullCGIObject,
			inErrorOnMissingCGIField
			);


		// Start deciding what we will pass on to the field setter
		Object answer = strVal;
		// Check the defaults
		if( null==answer )
		{
			// If we have no default
			if( null == optDefaultValue )
				// How serious to treat this
				// Fatal
				if( inErrorOnAlsoMissingDefaultValue )
					throw new DBConfigException( kExTag
						+ "No default value to backup for missing cgi field"
						+ ", for field \"" + inDBFieldName + "\""
						);
				// Or else just bail at this point
				else
					return;
			// Else we do have a default, use it
			else
				answer = optDefaultValue;
		}
		// Sanity check
		// At this point we should have an answer or we should have bailed
		// so this should never happen
		if( null==answer )
			if( inErrorOnAlsoMissingDefaultValue )
				throw new DBConfigException( kExTag
					+ "No value found for field \"" + inDBFieldName + "\""
					);
			else
				return;

		// NOW we can call the main value setter!
		// If that routine has a problem, it will complain
		// and it is also responsible for converting CGI strings
		// to other object types, and will complain as well if it can't
		// do that.  So there's still many potential causes of
		// a dbconfig exception beyond this point, even though it
		// seems like we're almost done!
		setValue( inDBFieldName, answer, inOverwriteOK );

	}


	// Get values from the CGI field corresponding to the DB field
	// Note: you probably don't want this, the schema defintitions
	// control the CGI name
	public int _getIntFromCGIOrDefaultUsingCgiPrefix(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		int inDefault,
		String inPrefix
		)
	{
		String tmpStr = getStringFromCGIOrNull(
			inDBFieldName,
			inCGIData,
			true,
			inPrefix,
			null,
			null
			);
		return NIEUtil.stringToIntOrDefaultValue(
			tmpStr, inDefault,
			false,	// inDoEmptyStringWarnings
			true	// inDoFormatWarnings
			);
	}



	// Get values from the CGI field corresponding to the DB field
	public int getIntFromCGIOrDefault(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		int inDefault
		)
	{
		String tmpStr = getStringFromCGIOrNull(
			inDBFieldName,
			inCGIData
			);
		return NIEUtil.stringToIntOrDefaultValue(
			tmpStr, inDefault,
			false,	// inDoEmptyStringWarnings
			true	// inDoFormatWarnings
			);
	}



	public String getStringFromCGIOrNull(
		String inDBFieldName,
		AuxIOInfo inCGIData
		)
	{
		return getStringFromCGIOrNull(
			inDBFieldName,
			inCGIData,
			false,	// inAutoGenerateDefaultCGIFieldName,
			null,	// optAutoGeneratePrefix,
			null,	// optAutoGenerateSuffix,
			null	// optOverrideWithSpecificCGIFieldName
			);
	}
	public String getStringFromCGIOrNull(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		boolean inAutoGenerateDefaultCGIFieldName,
		String optAutoGeneratePrefix,
		String optAutoGenerateSuffix,
		String optOverrideWithSpecificCGIFieldName
		)
	{
	    final String kFName = "getStringFromCGIOrNull";
		String answer = null;
		try
		{
			answer = getStringFromCGI(
				inDBFieldName,
				inCGIData,
				inAutoGenerateDefaultCGIFieldName,
				optAutoGeneratePrefix,
				optAutoGenerateSuffix,
				optOverrideWithSpecificCGIFieldName,
				false,	// inErrorOnNullCGIObject,
				false	// inErrorOnMissingCGIField
				);
			debugMsg( kFName, "Got answer=\""+answer+'"');
		}
		catch( Exception e )
		{
			debugMsg( kFName, "Caught exception: " + e );
			answer = null;
		}
		return answer;
	}

	// Get values from the CGI field corresponding to the DB field
	public String getStringFromCGI(
		String inDBFieldName,
		AuxIOInfo inCGIData,
		boolean inAutoGenerateDefaultCGIFieldName,
		String optAutoGeneratePrefix,
		String optAutoGenerateSuffix,
		String optOverrideWithSpecificCGIFieldName,
		boolean inErrorOnNullCGIObject,
		boolean inErrorOnMissingCGIField
		)
		throws DBConfigException
	{
		final String kFName = "getStringFromCGI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		debugMsg( kFName,
		        "Start: inDBFieldName="+inDBFieldName
		        + " inAutoGenerateDefaultCGIFieldName="+inAutoGenerateDefaultCGIFieldName
		        + " optAutoGeneratePrefix="+optAutoGeneratePrefix
		        + " optAutoGenerateSuffix="+optAutoGenerateSuffix
		        + " optOverrideWithSpecificCGIFieldName="+optOverrideWithSpecificCGIFieldName
		        + " inErrorOnNullCGIObject="+inErrorOnNullCGIObject
		        + " inErrorOnMissingCGIField="+inErrorOnMissingCGIField
		        );
		inDBFieldName = NIEUtil.trimmedStringOrNull( inDBFieldName );
		if( null == inDBFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in."
				);

		// Complain about missing CGI data if told to do so
		if( null == inCGIData && inErrorOnNullCGIObject )
			throw new DBConfigException( kExTag
				+ "Null CGI field data passed in."
				);

		// Normalize any specific field name they may have given us
		optOverrideWithSpecificCGIFieldName = NIEUtil.trimmedStringOrNull(
			optOverrideWithSpecificCGIFieldName
			);
		if( inAutoGenerateDefaultCGIFieldName
			&& optOverrideWithSpecificCGIFieldName != null
			)
		{
			throw new DBConfigException( kExTag
				+ "Was told to automatically generate a CGI field name"
				+ " but was also given a specific name to use of \""
				+ optOverrideWithSpecificCGIFieldName + "\""
				);
		}

		// Catch a logical inconsistency in passed-in arguments
		// they don't care if there's no cgi data, but they DO care if a field
		// is missing?  That would make no sense.  If there's no CGI data, then
		// OF COURSE fields will be missing.  We do not allow this.
		if( ! inErrorOnNullCGIObject && inErrorOnMissingCGIField )
		{
			throw new DBConfigException( kExTag
				+ "Inconsistency: told to allow null CGI data"
				+ " but to require that individual CGI fields to be present"
				+ "; if there is no CGI data then all fields will be missing."
				);
		}

		// We start to look for a string value
		String outStr = null;
		// If we have CGI data, we should search it
		if( null != inCGIData )
		{
			// We need to figure out what cgi field name to look for
			// Todo: no normalization at this point, seems it should be
			// auxioinfo's job?

			// We start with knowing nothing
			String lCgiFieldName = null;

			// If they gave us a specific field name to use, use it!
			if( null != optOverrideWithSpecificCGIFieldName )
			{
				lCgiFieldName = optOverrideWithSpecificCGIFieldName;
				debugMsg( kFName, "Using override name, now lCgiFieldName=\""+lCgiFieldName+'"');
			}
			// Else no specific name given
			else
			{
				// Check the table definition, it may have it
				try
				{
					lCgiFieldName = getTableDef().getCGIFieldName( inDBFieldName );
					debugMsg( kFName, "Checking schema, now lCgiFieldName=\""+lCgiFieldName+'"');
				}
				catch( DBConfigException dbe )
				{
					// If it doesn't have it, and if we're not allowed to
					// generate a default name, then we're done
					if( ! inAutoGenerateDefaultCGIFieldName )
						throw new DBConfigException( kExTag
							+ "No configured CGI field name for field \""
							+ inDBFieldName + "\""
							);
					// OK, we'll auto-generate one, force this to null for now
					lCgiFieldName = null;
				}
			}

			// If the cgi field is still null, and we're allowed to
			// auto generate, go ahead and do so!
			if( null==lCgiFieldName && inAutoGenerateDefaultCGIFieldName )
			{
				StringBuffer buff = new StringBuffer();
				// Add the prefix, if any given
				if( null != optAutoGeneratePrefix )
					buff.append( optAutoGeneratePrefix );
				// Add the field name itself
				buff.append( inDBFieldName );
				// Add the suffix, if any given
				if( null != optAutoGenerateSuffix )
					buff.append( optAutoGenerateSuffix );
				// Convert the buffer back into a string
				lCgiFieldName = new String( buff );
				debugMsg( kFName, "Will look for CGI field \""+lCgiFieldName+'"');
			}
			
			// Now one final sanity check
			if( null==lCgiFieldName )
				throw new DBConfigException( kExTag
					+ "Unable to determine CGI field name for field \""
					+ inDBFieldName + "\""
					);

			debugMsg( kFName, "finally lCgiFieldName=\""+lCgiFieldName+'"');

			// Now look up the value
			outStr = inCGIData.getScalarCGIFieldTrimOrNull( lCgiFieldName );

			// Sanity check
			if( null==outStr && inErrorOnMissingCGIField )
				throw new DBConfigException( kExTag
					+ "Unable to find a CGI value for field field \""
					+ inDBFieldName + "\""
					+ ", CGI field name \"" + lCgiFieldName + "\""
					);

		}	// End if there is CGI data to look at
		else {
		    debugMsg( kFName, "Null CGI data" );
		}

		// return the answer (or null if none and we didn't bail)
		return outStr;
	
	}

	public void clearValue( String inFieldName )
		throws DBConfigException
	{
		final String kFName = "clearValue";
		final String kExTag = kClassName + '.' + kFName + ": ";
		// Some sanity checks
		inFieldName = DBTableDef.normalizeFieldName( inFieldName );
		if( null == inFieldName )
			throw new DBConfigException( kExTag
				+ "Null/empty field name passed in, no way to store value."
				);
		// we need this answer later so store in a boolean
		boolean alreadyHasThatField = fValueObjectsHash.containsKey( inFieldName );
		if( ! alreadyHasThatField )
			throw new DBConfigException( kExTag
				+ "Attempt to clear field that has never been set,"
				+ " for field \"" + inFieldName + "\"."
				);

		// OK, go ahead and remove the values
		fValueObjectsHash.remove( inFieldName );
		// also store the name in the list if it's not there already
		fSetFields.remove( inFieldName );
	}




	public List getMissingRequiredFieldNames()
	{
		List fields = getTableDef().getRequiredFieldNames();
		List missingFields = new Vector();
		for( Iterator it = fields.iterator(); it.hasNext(); )
		{
			String field = (String)it.next();
			if( ! fValueObjectsHash.containsKey( field ) )
				missingFields.add( field );
		}
		// None missing, so return true
		return missingFields;
	}
	boolean hasAllRequiredFields()
	{
		List missing = getMissingRequiredFieldNames();
		if( missing != null && missing.size() > 0 )
			return true;
		else
			return false;
	}

	// private PreparedStatement buildStatement()
	private Object [] buildStatement()
		throws DBConfigException,
			DBConfigInServerReconnectWait
	{
		final String kFName = "buildStatement";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String statementStr = createSQLStringWithPlaceholders();

		PreparedStatement statement = null;
		Connection connection = null;
		try
		{
			// statement = getDBConfig().createPreparedStatement(
			Object [] myStateAndConnAry = getDBConfig().createPreparedStatement(
				statementStr,
				cDBConnection
				);
			statement = (PreparedStatement) myStateAndConnAry[0];
			connection = (Connection) myStateAndConnAry[1];

			// Try without caching, Todo: revisit
			// statement = getDBConfig().createPreparedStatement(
			//	statementStr
			//	);
		}
		catch(SQLException sqe)
		{
			throw new DBConfigException( kExTag
				+ "There appears to be problem with the generated SQL statement."
				+ " (Or the database may be down)."
				+ " Statement = \"" + statementStr + "\" (question marks are normal)"
				+ " Error: " + sqe
				);
		}
		// An eception at this point is a bit different than above
		// This will also have closed any connections and/or statements
		catch(DBConfigException dbe)
		{
			throw new DBConfigException( kExTag
				+ "The database connection appears to be down"
				+ "; this may be temporary."
				+ " Error: " + dbe
				);
		}

		// maybe try
		// statement..clearParameters();

		// We're good!
		// return statement;
		return new Object [] { statement, connection };

	}

	private String createSQLStringWithPlaceholders()
		throws DBConfigException
	{
		final String kFName = "createSQLStringWithPlaceholders";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean trace = shouldDoTraceMsg( kFName );

		// The table we will insert into
		String lTableName = getTableDef().getTableName();
		// This really should not be necessary
		if( null == lTableName )
			throw new DBConfigException( kExTag,
				"No table name to use."
				);

		// Start working on our answer
		StringBuffer buff = new StringBuffer();

		// Preamble
		buff.append( "INSERT INTO " + lTableName + " (" );

		// The list of fields
		// Sanity checks
		if( null == fSetFields || fSetFields.size() < 1 )
			throw new DBConfigException( kExTag,
				"No fields to set."
				);

		// Iterate through all the fields
		boolean isFirst = true;
		int fieldCounter = 0;
		for( Iterator it = fSetFields.iterator(); it.hasNext() ; )
		{
			String fieldName = (String) it.next();

			// And a warning msg about unimplemented fields
			if( getTableDef().hasUnimplementedField( fieldName, true ) )
			{
				warningMsg( kFName,
					"Skipping unimplemented field \"" + fieldName + "\"."
					);
				continue;
			}

			fieldCounter++;
			if( ! isFirst )
				buff.append( ',' );
			buff.append( fieldName );
			if(trace)
				traceMsg( kFName, "Field " + fieldCounter + " \"" + fieldName + "\"" );
			isFirst = false;
		}

		// We can't handle ZEO implemented fields in a statement
		// that's an error
		if( 0 == fieldCounter )
			throw new DBConfigException( kExTag,
				"No IMPLEMENTED fields to set."
				);
		
		// The value place holders
		buff.append( ") VALUES (" );
		// the correct number of question marks
		for( int i = 0 ; i < fieldCounter ; i++ )
		{
			if( i > 0 )
				buff.append( ',' );
			buff.append( '?' );
			if(trace)
				traceMsg( kFName, "Mark " + (i+1) );
		}


		// wrap up
		buff.append( ')' );

		String answer = new String( buff );
		debugMsg( kFName,
			"Built SQL Query \"" + answer + "\""
			);
		return answer;

	}


	DBConfig getDBConfig()
	{
		return fDBConfig;
	}
	DBTableDef getTableDef()
	{
		return fTable;
	}


	// The main database configuration
	private DBTableDef fTable;
	// The hash of fielded objects and the order they were entered in
	private Hashtable fValueObjectsHash;
	// The list of fields, in the order of appearance
	private List fSetFields;
	// The main database configuration
	private DBConfig fDBConfig;

	// A cached DB connection
	private Connection cDBConnection;



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


}

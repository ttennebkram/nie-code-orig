/*
 * Created on Jan 8, 2004
 *
 * OLD: Utility to import terms related to particular numbered maps
 * Used with AOPA in 2004
 */
package nie.webui.util;

import java.util.*;
import java.io.*;
import nie.core.*;
import nie.webui.xml_screens.CreateMapForm;

/**
 * @author mbennett
 *
 */
public class TermImporter
{
	final static String kClassName = "TermImporter";

	public static void main(String[] args)
	{
		final String kFName = "main";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( args.length!=3 ) {
			fatalErrorMsg( kFName, getSyntax() );
			System.exit(1);
		}

		String mode = args[2];
		if( mode==null || ( ! mode.equalsIgnoreCase("-terms") && ! mode.equalsIgnoreCase("-alt_terms") ) ) {
			fatalErrorMsg( kFName,
				"Invalid mode \"" + mode + "\"" + NIEUtil.NL
				+ getSyntax()
				);
			System.exit(1);
		}

		// Do the work
		try {
			String configURI = args[0];
			DBConfig dbConfig = new DBConfig( configURI );

			String csvURI = args[1];
			Hashtable terms = loadCsvData( csvURI /*, configURI*/ );	

			for( Iterator it = terms.keySet().iterator() ; it.hasNext() ; ) {
				Integer key = (Integer) it.next();
				String termsStr = (String) terms.get( key );

				List newTerms = NIEUtil.singleCommaStringToUniqueListOfStrings(
					termsStr, true
					);
				if( null==newTerms || newTerms.size() < 1 )
					throw new Exception( kExTag +
						"No terms in \"" + termsStr + "\""
						+ " for map ID " + key
						);

				nie.webui.xml_screens.CreateMapForm.updateTermsForMapInDb(
					dbConfig,
					null,	// Connection to use
					key.intValue(),
					newTerms,
					mode.equalsIgnoreCase("-terms")	// Is term or alt term
					);
			}



		}
		catch( Exception e ) {
			fatalErrorMsg( kFName, "Error importing: " + e );
			System.exit(2);
		}

	}


	public static Hashtable loadCsvData( String inFullDataURI /*, String inBaseURI*/ )
		throws IOException
	{
		final String kFName = "loadCsvData";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Will throw IO if not found, which may be OK
		Vector lines = NIEUtil.fetchURIContentsLines(
			inFullDataURI,	// String inBaseName,
			null,	// inBaseURI,	// String optRelativeRoot,
			null,	// String optUsername,
			null,	// String optPassword,
			true,	// boolean inDoTrim,
			true,	// boolean inDoSkipBlankLines,
			null,	// AuxIOInfo inoutAuxIOInfo
			false	// use POST
			);

		Iterator it = lines.iterator();

		Hashtable outHash = new Hashtable();

		// Process the rest of the lines
		int lineCounter = 0;
		while( it.hasNext() ) {
			String thisLine = (String)it.next();
			lineCounter++;

			// If the first line starts with #, then it's a list of field names
			if( thisLine.startsWith("#") )
				continue;

			// Get the values
			Vector values = NIEUtil.parseCSVLine( thisLine );

			// Some sanity checking
			if( null==values || values.size() < 2 )
				throw new IOException( kExTag +
					"Wrong number of values on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Line=\"" + thisLine + "\""
					);

			// The map ID
			String mapIDStr = (String) values.get( 0 );
			int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, true, true );
			if( mapID < 1 )
				throw new IOException( kExTag +
					"Invalid map ID \"" + mapIDStr + "\" on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Line=\"" + thisLine + "\""
					);
			Integer mapKey = new Integer( mapID );
			if( outHash.containsKey(mapKey) )
				throw new IOException( kExTag +
					"Duplicate map ID \"" + mapID + "\" on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Line=\"" + thisLine + "\""
					);

			// The terms
			String termsStr = (String) values.get( 1 );
			termsStr = NIEUtil.trimmedStringOrNull( termsStr );
			if( null == termsStr )
				throw new IOException( kExTag +
					"Empty/null terms on line # " + lineCounter + " of file \"" + inFullDataURI + "\""
					+ " Line=\"" + thisLine + "\""
					);

			// Add data to hash
			outHash.put( mapKey, termsStr );

		}	// End for each line of the file

		if( outHash.isEmpty() )
			throw new IOException( kExTag +
				"No data from file \"" + inFullDataURI + "\""
				);

		return outHash;
	}



	public static String getSyntax() {
		return
			"Utility to import terms related to particular numbered maps." + NIEUtil.NL
			+ NIEUtil.NL
			+ "Syntax:" + NIEUtil.NL
			+ "$0 config.xml info.csv -terms|-alt_terms" + NIEUtil.NL
			;
	}

	// This gets us to the logging object
	static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}

	static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}

	static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	static boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}

	static boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}

	static boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	static boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}






}

/*
 * Created on Jul 2, 2004
 *
 */
package nie.checkup.verity;

import java.util.*;
import java.io.*;
import nie.core.*;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class VBrowseSession {

	static final String kClassName = "VBrowseSession";

	public static void main(String[] args) {
		File root = null;
		if( args.length > 0 ) {
			root = new File( args[0] );
		}
		// VBrowse1 vb = new VBrowse1( coll );
		// vb.browse();

		// findColls( root );
	}

	// public VBrowseSession( String inVdbPath )
	public VBrowseSession( File inVdbPath, File optVerityBinDir )
		throws IOException
	{
		mVdbPath = inVdbPath;
		mVBin = optVerityBinDir;
		try {
			openSession();
		}
		catch( IOException e ) {
			throw new IOException(
				"Error opening Verity VDB:"
				+ " vdb=\"" + inVdbPath + "\""
				+ ", (optional) Verity binary directory path=\"" + optVerityBinDir + "\""
				+ " (Are you sure Verity is in your path?)"
				+ " Error: " + e
				);
		}
	}


	public String getFieldValue( int inRowNum, String inFieldName )
		throws IOException
	{
		if( inRowNum < 0 )
			throw new IOException( "Invalid row number " + inRowNum + " (reminder: they are 0-based)" );

		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null == inFieldName )
			throw new IOException( "Null/empty field name" );

		sendCmd( ""+inRowNum );
// System.out.println( "Have sent command" );

		boolean haveEatenCmdEcho = false;
		while( true ) {
			String line = readLine();
// System.out.println( "Line=" + line );
			if( null==line )
				continue;
			if( ! haveEatenCmdEcho ) {
				haveEatenCmdEcho = true;
				continue;
			}
			if( line.length()==PROMPT_LEN && line.equals(READY_PROMPT) )
				break;
			String fieldName = extractMormalizedFieldNameFromRecordOrNull( line );
			if( null!=fieldName && fieldName.equals(inFieldName) ) {
// System.out.println( "Found field \"" + fieldName + "\", line=\"" + line + "\"" );
				String value = extractValueFromRecordOrNull( line );
// System.out.println( "Vale=\"" + value + "\"" );

				consumeLinesThroughPrompt();
				return value;
			}

		}
		return null;
	}

	public List getUnfilteredFieldValues( String inFieldName, boolean inKeepOnlyUnique )
		throws IOException
	{
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null == inFieldName )
			throw new IOException( "Null/empty field name" );

		int recCount = getRawRecordCountForField( inFieldName );

		if( recCount < 0 )
			throw new IOException( "Invalid raw record count found: " + recCount );

		List outList = new ArrayList();
		// Create a unique values set if asked for uniqueues
		HashSet unique = inKeepOnlyUnique ? new HashSet() : null;

		// For each expected record
		for( int i=0; i<recCount; i++ ) {
			String value = getFieldValue( i, inFieldName );
// System.out.println( "" + i + "=\"" + value + "\"" );
			// if null, should have got error previoulsy reported
			if( null!=value ) {
				// If want all values or haven't logged value then log it now
				if( null==unique || ! unique.contains(value) ) {
					outList.add( value );
					// and if tracking unique, record this as a key
					if( null!=unique )
						unique.add( value );
				}
			}
		}

		return outList;
	}

	public int getRawRecordCountForField( String inFieldName )
		throws IOException
	{
		final String kFName = "getRawRecordCountForField";

		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null == inFieldName )
			throw new IOException( "Null/empty field name" );

		// final long kBigNum = 1000000000;

		sendCmd( ""+MAX_RECORD );
		debugMsg( kFName, "Have sent command " + MAX_RECORD );

		int outMax = -1;
		boolean haveEatenCmdEcho = false;
		while( true ) {
			String line = readLine();
// System.out.println( "Line=" + line );
			if( null==line )
				continue;
			if( ! haveEatenCmdEcho ) {
				haveEatenCmdEcho = true;
				continue;
			}
			if( line.length()==PROMPT_LEN && line.equals(READY_PROMPT) )
				break;
			String fieldName = extractMormalizedFieldNameFromRecordOrNull( line );
			if( null!=fieldName && fieldName.equals(inFieldName) ) {
// System.out.println( "Found field \"" + fieldName + "\", line=\"" + line + "\"" );
				outMax = extractMaxFieldCountFromRecord( line, true );
// System.out.println( "Vale=\"" + outMax + "\"" );
				consumeLinesThroughPrompt();
				break;
			}

		}
		return outMax;
	}


	void cacheFieldsInfo(
		List inFieldNameList, HashSet inFieldNameSet, Hashtable FieldNameRawCounts
		)
			throws IOException
	{
		final String kFName = "cacheFieldsInfo";

		if( null == inFieldNameSet || null == inFieldNameSet || null == FieldNameRawCounts )
			throw new IOException( "Null/empty cache(s)" );

		sendCmd( ""+MAX_RECORD );

		// For each field
		boolean haveEatenCmdEcho = false;
		while( true ) {
			String line = readLine();
			if( null==line )
				continue;
			if( ! haveEatenCmdEcho ) {
				haveEatenCmdEcho = true;
				continue;
			}
			if( line.length()==PROMPT_LEN && line.equals(READY_PROMPT) )
				break;
			String fieldName = extractMormalizedFieldNameFromRecordOrNull( line );
			if( inFieldNameSet.contains(fieldName) ) {
				errorMsg( kFName, "skipping duplicate field \"" + fieldName + "\"" );
				continue;
			}

			int rawCount = extractMaxFieldCountFromRecord( line, true );
			if( rawCount < 0 )
				throw new IOException( "Invalid record count " + rawCount + " for field \"" + fieldName + "\"" );

			inFieldNameList.add( fieldName );
			inFieldNameSet.add( fieldName );
			FieldNameRawCounts.put( fieldName, new Integer(rawCount) );
		}

	}



	static String extractValueFromRecordOrNull( String inRecord ) {
		return extractValueFromRecordOrNull( inRecord,
			true,	// inDoTrim
			false,	// inForceEmptyToNull
			true	// doWarnIfOutOfRange
		);
	}

	static String extractValueFromRecordOrNull( String inRecord,
		boolean inDoTrim, boolean inForceEmptyToNull, boolean doWarnIfOutOfRange
		)
	{
		final String kFName = "extractValueFromRecordOrNull";

		// final String valMarker = ") = ";
		final int valMarkLen = START_VALUE_PATTERN.length();

		if( null==inRecord ) {
			staticErrorMsg( kFName, "null record passed in" );
			return null;
		}
		int markerAt = inRecord.indexOf( START_VALUE_PATTERN );
		if( markerAt < 0 ) {
			staticErrorMsg( kFName, "no value delimiter found in record \"" + inRecord + "\"" );
			return null;
		}
		String outValue = "";
		if( inRecord.length() > markerAt+valMarkLen ) {
			outValue = inRecord.substring( markerAt+valMarkLen );
			if( outValue.startsWith(RECORD_COUNT_PREFIX_PATTERN)
					&& outValue.endsWith(RECORD_COUNT_SUFFIX_PATTERN)
			) {
				if( doWarnIfOutOfRange )
					staticErrorMsg( kFName, "Error: index out of range " + outValue );
				outValue = null;
			}
		}
		if( inDoTrim && null!=outValue )
			outValue = outValue.trim();
		if( inForceEmptyToNull && null!=outValue && outValue.length() < 1 )
			outValue = null;

		return outValue;
	}



	static String extractMormalizedFieldNameFromRecordOrNull( String inRecord )
	{
		final String kFName = "extractMormalizedFieldNameFromRecordOrNull";

		if( null==inRecord ) {
			staticErrorMsg( kFName, "null record passed in" );
			return null;
		}
		StringBuffer inLine = new StringBuffer( inRecord );
		StringBuffer outField = new StringBuffer();
		int offset=0;
		char c = ' ';
		// Skip past the digits
		while( offset < inLine.length() ) {
			c=inLine.charAt(offset++);
			if( c==' ' || c=='\t' )
				break;
		}
		// Skip past the spaces after the digits
		while( offset < inLine.length() ) {
			c=inLine.charAt(offset++);
			if( c!=' ' && c!='\t' )
				break;
		}
		// Eat up all the field name contents
		// don't increment yet, we want to save the last c
		while( offset < inLine.length() ) {
			if( c==' ' || c=='\t' )
				break;
			outField.append( c );
			c=inLine.charAt(offset++);
		}

		String outStr = null;
		if( outField.length() > 0 ) {
			outStr = new String( outField );
			outStr = NIEUtil.trimmedLowerStringOrNull( outStr );
		}

		return outStr;
	}


	static int extractMaxFieldCountFromRecord( String inRecord,
		boolean doNotFoundWarnings
		)
	{
		final String kFName = "extractMaxFieldCountFromRecord";

		// ") = " + "(only " nnn " records)"
		final String startMarker = START_VALUE_PATTERN + RECORD_COUNT_PREFIX_PATTERN;
		final int startLen = startMarker.length();
		final String endMarker = RECORD_COUNT_SUFFIX_PATTERN;	// " records)"

		int outVal = -1;


		if( null==inRecord ) {
			staticErrorMsg( kFName, "null record passed in" );
			return outVal;
		}

		int startAt = inRecord.indexOf( startMarker );
		if( startAt < 0 ) {
			staticErrorMsg( kFName, "no start pattern found in record \"" + inRecord + "\"" );
			return outVal;
		}
		if( startAt+startLen == inRecord.length()-1 ) {
			staticErrorMsg( kFName, "no end pattern found (1) in record \"" + inRecord + "\"" );
			return outVal;
		}
		int endAt = inRecord.lastIndexOf( endMarker );
		if( endAt < 0 ) {
			staticErrorMsg( kFName, "no end pattern found (2) in record \"" + inRecord + "\"" );
			return outVal;
		}

		String countStr = inRecord.substring( startAt+startLen, endAt );

// System.out.println( "countStr=\"" + countStr + "\"" );

		outVal = NIEUtil.stringToIntOrDefaultValue( countStr, outVal, true, true );

		return outVal;
	}




	void sendCmd( String inCmd )
		throws IOException
	{
		if( null==mToProcWriter )
			throw new IOException( "To-process writer not initialized" );
		
		mToProcWriter.write( (null!=inCmd ? inCmd : "") + '\n' );
		// NO mToProcWriter.write( (null!=inCmd ? inCmd : "") + '\r' );
		// mToProcWriter.write( (null!=inCmd ? inCmd : "") + '\r' + '\n' );
		mToProcWriter.flush();
	}

	public void _browse() {
		String cmd = prepCmd();
		System.out.println( cmd );

		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec( cmd ); 

			// Runtime me = Runtime.getRuntime();
			// me.addShutdownHook(hook)

			InputStream fromProc = proc.getInputStream();
			OutputStream toProc = proc.getOutputStream();
			BufferedReader fromProcBf = new BufferedReader( new InputStreamReader(fromProc) );
			OutputStreamWriter toProcW = new OutputStreamWriter(toProc);


			// InputStreamReader fromProcRd = new InputStreamReader( fromProc );
			// Reader fromProcRd = new Reader( fromProc );
			// LineNumberReader fromProcLines = new LineNumberReader( fromProcBf );
			// while( String line = fromProcLines.re )

			String line;
			while( (line = fromProcBf.readLine()) != null ) {
				System.out.println( line );
				// System.out.println( "Ready=" + fromProcBf.ready() );
				if( line.endsWith(_PRE_PROMPT) )
					break;
			}

			toProcW.write( "q\r" );

			toProcW.close();
			toProc.close();
			fromProcBf.close();
			fromProc.close();
			

		}
		// catch( IOException e ) {
		catch( Throwable e ) {
			System.out.println( "Exec error: " + e );
		}
		finally {
			if( null!=proc )
				proc.destroy();
		}
		// getInputStream(), getOuputStream(), getErrorStream().


	}

	String readLine()
		throws IOException
	{
		if(null==mFromProcReader)
			throw new IOException( "Process not initialized: null from-process reader" );

		// What character ENDs a line on this platform
		// final String NL = System.getProperty("line.separator");
		// final char EOL = NL.charAt( NL.length()-1 );
		final char EOL = '\n';
		// ^^^ No, Verity always uses newline (0x0A) regardless of platform

		final int pLen = READY_PROMPT.length();

		StringBuffer line = new StringBuffer();
		int b = -1;
		boolean doneLine = false;
		long startTime = System.currentTimeMillis();

		// We allow for slow processing
		while( true ) {
// System.out.print( "." );
			while( (b=mFromProcReader.read()) >= 0 ) {
				char c = (char)b;
				if( c != '\r' && c != '\n' )
					line.append( c );
				if( c == EOL ) {
					doneLine = true;
					break;
				}
	
				// Workaround for prompt line, which does not return a newline
				if( line.length() == pLen )
					if( (new String(line)).equals(READY_PROMPT) ) {
						doneLine = true;
						break;
					}
			}
			if( doneLine )
				break;
			long currTime = System.currentTimeMillis();
			if( currTime > startTime + MAX_LINE_TIME_MS )
				break;
		}

		String outLine = null;
		if( line.length() > 0 )
			outLine = new String( line );

// System.out.println( "line=\"" + outLine + "\"" );

		return outLine;

		// TODO: should check if process is dead if we got a null
	}




	String _readLine()
		throws IOException
	{
		if(null==mFromProcReader)
			throw new IOException( "Process not initialized: null from-process reader" );

		// What character ENDs a line on this platform
		final String NL = System.getProperty("line.separator");
		final char EOL = NL.charAt( NL.length()-1 );

		final int pLen = READY_PROMPT.length();

		StringBuffer line = new StringBuffer();
		int b = -1;
		while( (b=mFromProcReader.read()) >= 0 ) {
			char c = (char)b;
			if( c != '\r' && c != '\n' )
				line.append( c );
			if( c == EOL )
				break;

			// Workaround for prompt line, which does not return a newline
			if( line.length() == pLen )
				if( (new String(line)).equals(READY_PROMPT) )
					break;
		}

		if( line.length() > 0 )
			return new String( line );
		else
			return null;
	}

	// private void readHeaderLines()
	public void consumeLinesThroughPrompt()
		throws IOException
	{
		while( true ) {
			String line = readLine();
// System.out.println( "Header: " + line );
			// if( null==line || line.equals(READY_PROMPT) )
			// No, Verity header includes blank/null lines
			if( null!=line && line.equals(READY_PROMPT) )
				break;
		}
	}	

	String prepCmd() {
		// String outCmd = BROWSE_EXE + ' ' + mCollPath + "/parts/00000001.ddd";
		// String outCmd = BROWSE_EXE + ' ' + DEF_PART_PATH;

		File browseExe = null;
		if( null == mVBin )
			browseExe = new File( BROWSE_EXE );
		else
			browseExe = new File( mVBin, BROWSE_EXE );

		// String outCmd = BROWSE_EXE + " \"" + mVdbPath.getAbsolutePath() + '"';

		String outCmd = "\"" + browseExe + "\" \"" + mVdbPath.getAbsolutePath() + '"';

		return outCmd;

		// TODO: should use alternate system call that takes args as an array
	}


	public void openSession()
		throws IOException
	{
		final String kFName = "openSession";

		String cmd = prepCmd();
		debugMsg( kFName, "cmd=" + cmd );

		mBrowseProcess = Runtime.getRuntime().exec( cmd ); 

		// Runtime me = Runtime.getRuntime();
		// me.addShutdownHook(hook)

		mFromProcStream = mBrowseProcess.getInputStream();
		mToProcStream = mBrowseProcess.getOutputStream();

		// mFromProcReader = new InputStreamReader( mFromProcStream );
		// BufferedReader fromProcBf = new BufferedReader( new InputStreamReader(fromProc) );
		mFromProcReader = new BufferedReader( new InputStreamReader(mFromProcStream) );


		// mToProcWriter = new OutputStreamWriter( mToProcStream );
		mToProcWriter = new BufferedWriter( new OutputStreamWriter(mToProcStream) );

		// take care of foreplay
		// readHeaderLines();
		consumeLinesThroughPrompt();
	}

	public void closeSession()
		throws IOException
	{
		final String kFName = "closeSession";

		try {

			if( null!=mFromProcReader ) {
				mFromProcReader.close();
				mFromProcReader = null;
			}
			if( null!=mFromProcStream ) {
				mFromProcStream.close();
				mFromProcStream = null;
			}
	
			if( null!=mToProcWriter ) {
				mToProcWriter.close();
				mToProcWriter = null;
			}
			if( null!=mToProcStream ) {
				mToProcStream.close();
				mToProcStream = null;
			}

		}
		finally {

			if( null!=mBrowseProcess ) {
				mBrowseProcess.destroy();
				mBrowseProcess = null;
				// System.out.println( "destroyed" );
			}
			else {
				warningMsg( kFName, "no process to destroy" );
			}

		}

	}

	private static void __sep__Runtime_Logging__() {}


	//////////////////////////////////////////////////

	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}



	// Return the same thing casted to allow access to impl extensions
	private static RunLogBasicImpl getRunLogImplObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogImplObject();
	}



	private boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath + ": " + inMessage
			);
	}

	private boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath + ": " + inMessage
			);
	}



	private boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName, inFromRoutine );
	}



	private boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath + ": " + inMessage
			);
	}

	private boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}



	private boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath +  ": " + inMessage
			);
		// return getRunLogObject().statusMsg( kClassName, inFromRoutine,
		// 	"DEBUG: " + inMessage
		// 	);
	}



	private boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath +  ": " + inMessage
			);
	}



	private boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName, inFromRoutine );
	}



	private boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath +  ": " + inMessage
			);
	}



	private boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath +  ": " + inMessage
			);
	}

	private boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			// inMessage
			"" + mVdbPath +  ": " + inMessage
			);
	}




	private static void __sep__Member_Fields_and_Constants__() {}
	///////////////////////////////////////////////////////////////////



	private static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



	private static boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}



	private static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}





	// Raw stream and then character stream
	InputStream mFromProcStream;
	// InputStreamReader mFromProcReader;
	BufferedReader mFromProcReader;

	OutputStream mToProcStream;
	// OutputStreamWriter mToProcWriter;
	BufferedWriter mToProcWriter;

	Process mBrowseProcess;

	// String mVdbPath;
	File mVdbPath;
	// Path to Verity binaries, optional
	File mVBin;

	public static final long MAX_LINE_TIME_MS = 5;

	// public static final String BROWSE_EXE = "C:\\apps\\verity\\K2\\_nti40\\bin\\browse.exe";
	public static final String BROWSE_EXE = "browse";

	// public static final long MAX_RECORD = Long.MAX_VALUE;
	public static final long MAX_RECORD = 1000000000;

	public static final String READY_PROMPT = "Action (? for help): ";
	public static final int PROMPT_LEN = READY_PROMPT.length();
	public static final String _PRE_PROMPT = "s) Dispatch as stream";

	public static final String START_VALUE_PATTERN = ") = ";
	public static final String RECORD_COUNT_PREFIX_PATTERN = "(only ";
	public static final String RECORD_COUNT_SUFFIX_PATTERN = " records)";


	// public static final String VHOME = "c:\\apps\\verity\\...\\browse.exe";
	// public static final String DEF_PART_PATH = "C:\\apps\\verity\\s97is360\\s97is\\colls\\doc\\parts\\00000006.ddd";



}

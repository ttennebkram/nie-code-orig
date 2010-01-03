package nie.core;

import java.io.*;
import java.util.*;
import java.net.*;
// import java.text.DateFormat;
import java.text.*;
import java.util.regex.*;

/**
 * Title:        NIEUtil
 * Description:  A bunch of static, generic utility functions
 * Copyright:    Copyright (c) 2001 - 2009
 * Company:      New Idea Engineering, Inc.
 * @author 	Mark Bennett and NIE Staff
 * @version 2.9
 * @author 	v1 authors Mark Bennett and Kevin-Neil Klop 2001
 */

/**
 * Todo Items
 * See java.text.FormatMessage, some interesting things there
 */


public class NIEUtil
{

	private static final String kClassName = "NIEUtil";
	// This is for when we need to reference ourselves in classforname
	private static final String kFullClassName = "nie.core." + kClassName;

	// The platform specific newline sequence
	// For use by others as well!
	public static final String NL = System.getProperty("line.separator");
	public static final char TAB = '\t';
	public static final char NBSP = '\240';

	private static final String MODULE_DISPLAY_NAME = "NIE Utils Lib";
	private static final String MODULE_DISPLAY_VERSION = "2.9xd";
	private static final String COPYRIGHT_DISPLAY =
		"Copyright 2001-2009 New Idea Engineering, Inc. - http://www.ideaeng.com"
		+ NL + "This product includes some of the excellent software libraries developed by"
		+ NL + "The Apache Software Foundation - http://www.apache.org"
		;
	public static String getModuleBanner()
	{
		return MODULE_DISPLAY_NAME
			+ ", Version "
			+ MODULE_DISPLAY_VERSION
			+ NL
			;
	}
	public static String getCopyrightBanner()
	{
		return COPYRIGHT_DISPLAY + NL;
	}

	// private static boolean debug = true;
	// Don't specifically set it if you want FALSE, it'll default to that
	private static boolean debug;
	public static void setDebug( boolean flag )
	{
		debug = flag;
	}
	public static void setVerbosity( boolean flag )
	{
		debug = flag;
	}


	private static HashSet kNumericTypes;
	static {
		kNumericTypes = new HashSet();
		kNumericTypes.add( "java.lang.Double" );
		kNumericTypes.add( "java.lang.Float" );
		kNumericTypes.add( "java.math.BigDecimal" );
		kNumericTypes.add( "java.math.BigInteger" );
		kNumericTypes.add( "java.lang.Long" );
		kNumericTypes.add( "java.lang.Integer" );
		kNumericTypes.add( "java.lang.Boolean" );
		// Strings may also have numbers, but they are not included here
	}

	private static HashSet kDateTypes;

	static {
		kDateTypes = new HashSet();
		kDateTypes.add( "java.util.Date" );
		// kDateTypes.add( "java.util.Time" );	// No doc for this???
		kDateTypes.add( "java.sql.Date" );
		kDateTypes.add( "java.sql.Time" );
		kDateTypes.add( "java.sql.Timestamp" );
		// kDateTypes.add( "java.lang.Long" );
		// ^^^ Long is handled, but not listed, as it is more likely a date
		// Strings may also have dates, but they are not included here
		// java.math.BigInteger also handled but not listed here
	}


	private static void __Simple_String_Formatting__() {}

	// Give us back either a trimmed string or a true null
	// force "empty" strings to null
	public static String trimmedStringOrNull( String inStr )
	{
		if( inStr == null )
			return null;
		String outStr = inStr.trim();
		outStr = ! outStr.equals("") ? outStr : null;
		return outStr;
	}
	// trimmed string or null, normalized to specific case
	public static String trimmedLowerStringOrNull( String inStr )
	{
		String outStr = trimmedStringOrNull( inStr );
		outStr = ( outStr != null ) ? outStr.toLowerCase() : null;
		return outStr;
	}
	public static String trimmedUpperStringOrNull( String inStr )
	{
		String outStr = trimmedStringOrNull( inStr );
		outStr = ( outStr != null ) ? outStr.toUpperCase() : null;
		return outStr;
	}

	// Always returns an object, but it may be empty
	public static Set trimmedLowerStringsAsSet( Collection inValues )
	{
		return trimmedLowerStringsAsSet( inValues, true );
	}
	// Always returns an object, but it may be empty
	public static Set trimmedLowerStringsAsSet( Collection inValues, boolean inDoErrors )
	{
		final String kFName = "trimmedLowerStringsAsSet";
		boolean debug = shouldDoDebugMsg( kFName );
		Set outSet = new HashSet();
		if( null==inValues ) {
			if( inDoErrors )
				errorMsg( kFName, "Null input set, returning empty set. (1)" );
			else if( debug )
				debugMsg( kFName, "Null input set, returning empty set. (2)" );
			return outSet;
		}
		if( inValues.size() < 1 ) {
			if( inDoErrors )
				warningMsg( kFName, "Empty input set, returning empty set. (1)" );
			else if( debug )
				debugMsg( kFName, "Empty input set, returning empty set. (2)" );
			return outSet;
		}
		for( Iterator it = inValues.iterator(); it.hasNext(); ) {
			String startVal = (String) it.next();
			String endVal = trimmedLowerStringOrNull( startVal );
			if( null!=endVal ) {
				outSet.add( endVal );
			}
			else {
				if( inDoErrors )
					warningMsg( kFName, "Value reduced to null/empty. (1)" );
				else if( debug )
					debugMsg( kFName, "Value reduced to null/empty. (2)" );			
			}
		}
		if( outSet.size() < 1 ) {
			if( inDoErrors )
				warningMsg( kFName, "No non-empty entries. (1)" );
			else if( debug )
				debugMsg( kFName, "No non-empty entries. (2)" );
			return outSet;
		}
		return outSet;
	}
	// Always returns an object, but it may be empty
	public static List trimmedLowerStringsAsList( Collection inValues )
	{
		return trimmedLowerStringsAsList( inValues, true );
	}
	// Always returns an object, but it may be empty
	public static List trimmedLowerStringsAsList( Collection inValues, boolean inDoErrors )
	{
		final String kFName = "trimmedLowerStringsAsList";
		boolean debug = shouldDoDebugMsg( kFName );
		// Set outSet = new HashSet();
		List outList = new Vector();
		if( null==inValues ) {
			if( inDoErrors )
				errorMsg( kFName, "Null input set, returning empty set. (1)" );
			else if( debug )
				debugMsg( kFName, "Null input set, returning empty set. (2)" );
			return outList;
		}
		if( inValues.size() < 1 ) {
			if( inDoErrors )
				warningMsg( kFName, "Empty input set, returning empty set. (1)" );
			else if( debug )
				debugMsg( kFName, "Empty input set, returning empty set. (2)" );
			return outList;
		}
		for( Iterator it = inValues.iterator(); it.hasNext(); ) {
			String startVal = (String) it.next();
			String endVal = trimmedLowerStringOrNull( startVal );
			if( null!=endVal ) {
				outList.add( endVal );
			}
			else {
				if( inDoErrors )
					warningMsg( kFName, "Value reduced to null/empty. (1)" );
				else if( debug )
					debugMsg( kFName, "Value reduced to null/empty. (2)" );			
			}
		}
		if( outList.size() < 1 ) {
			if( inDoErrors )
				warningMsg( kFName, "No non-empty entries. (1)" );
			else if( debug )
				debugMsg( kFName, "No non-empty entries. (2)" );
		}
		return outList;
	}

	// Add pad characters to the left of a string (aka "right justify")
	// to a minimum width.
	// Simple versions set derfaults:
	// Set default: the pad character is a space
	// Set default: do NOT truncate if the input string is longer
	//  than the desired output string
	public static String leftPadString( String inString, int desiredLength )
	{
		return leftPadString( inString, desiredLength,
			' ', false
			);
	}
	public static String leftPadString( String inString, int desiredLength,
		char padChar, boolean doTruncateAtMaxLength
		)
	{
		if( inString == null )
			inString = "";
		if( desiredLength < 1 )
			return inString;

		if( inString.length() == desiredLength )
			return inString;

		if( inString.length() > desiredLength )
			if( ! doTruncateAtMaxLength )
				return inString;
			else
				return inString.substring( 0, desiredLength+1 );

		// How many pad characters will we need?
		int padLength = desiredLength - inString.length();

		// We will build this with a string buffer
		StringBuffer buff = new StringBuffer();

		// Add the appropriate number of pad characters
		for( int i=1; i<=padLength; i++ )
			buff.append( padChar );

		// And now add our string
		buff.append( inString );

		// Return the results
		return new String( buff );
	}

	public static String starBanner( Collection inStrings ) {
		return starBanner( inStrings, null );
	}

	public static String starBanner(
			Collection inStrings, String optBannerBorder
	) {
		final String kFName = "starBanner";
		if( null==inStrings || inStrings.isEmpty() )
			return "";

		String kBannerSeqeuence =
			(null!=optBannerBorder && optBannerBorder.length()>0)
			? optBannerBorder
			: "*";

		final int kOverallLineLen = 79;
		final int kSideWidth = 2; // 3;

		int limit = kOverallLineLen - 2 * ( kSideWidth + 1 );

		int maxLen = 0;
		for( Iterator it = inStrings.iterator() ; it.hasNext() ; ) {
			String line = (String)it.next();
			int len = line.length();
			maxLen = ( len > maxLen && len <= limit ) ? len : maxLen;
		}

		int lineLen = maxLen + ( kSideWidth + 1 ) * 2;


		String roof = dupeSequence( kBannerSeqeuence, lineLen, false );
		roof = centerStringWithPadding( roof, kOverallLineLen, ' ', false, false );
		// statusMsg( kFName, "Roof before = \"" + roof + "\"" );
		// roof = roof.trim() + NL;
		roof += NL;
		// statusMsg( kFName, "Roof after = \"" + roof + "\"" );
		String leftEdge = dupeSequence( kBannerSeqeuence, kSideWidth, true ) + " ";
		String rightEdge = " " + dupeSequence(kBannerSeqeuence, kSideWidth, true );

		StringBuffer buff = new StringBuffer();
		buff.append( roof );
		for( Iterator it2 = inStrings.iterator() ; it2.hasNext() ; ) {
			String midLine = (String)it2.next();
			midLine = centerStringWithPadding( midLine, maxLen );
			String line = leftEdge + midLine + rightEdge;
			// line = centerStringWithPadding( line, kOverallLineLen ).trim();
			line = centerStringWithPadding( line, kOverallLineLen, ' ', false, false );
			buff.append( line ).append( NL );
		}
		buff.append( roof );

		return new String( buff );
	}

	public static String centerStringWithPadding( String inString, int desiredLength )
	{
		return centerStringWithPadding( inString, desiredLength, ' ', false, true );
	}

	public static String centerStringWithPadding( String inString, int desiredLength,
		char padChar, boolean doTruncateAtMaxLength, boolean doPadRightSide
		)
	{
		if( inString == null )
			inString = "";
		if( desiredLength < 1 )
			return inString;

		if( inString.length() == desiredLength )
			return inString;

		if( inString.length() > desiredLength )
			if( ! doTruncateAtMaxLength )
				return inString;
			else
				return inString.substring( 0, desiredLength+1 );

		// How many pad characters will we need?
		int totalPadLength = desiredLength - inString.length();
		int leftPadLength = totalPadLength / 2;
		int rightPadLength = totalPadLength - leftPadLength;

		// We will build this with a string buffer
		StringBuffer buff = new StringBuffer();

		// Add the appropriate number of pad characters
		for( int i=1; i<=leftPadLength; i++ )
			buff.append( padChar );

		// And now add our string
		buff.append( inString );

		if( doPadRightSide )
			for( int j=1; j<=rightPadLength; j++ )
				buff.append( padChar );

		// Return the results
		return new String( buff );
	}

	public static String dupeSequence( String inString, int desiredLength,
		boolean doTruncateAtMaxLength
		)
	{
		if( desiredLength < 1 )
			return "";
		if( null==inString || inString.length() < 1 )
			inString = "*";

		// We will build this with a string buffer
		StringBuffer buff = new StringBuffer();
		while( buff.length() < desiredLength )
			buff.append( inString );
		if( buff.length() > desiredLength && doTruncateAtMaxLength )
			buff.delete( desiredLength, buff.length() );

		return new String( buff );
	}

	// Left pad an integer with 0's
	// Don't truncate
	// leftPadInt( 17, 3 ) => 017
	// LeftPadInt( 0, 3 ) => 000
	// leftPadInt( 17293, 3 ) => 17293
	// If int < 0, pad mantissa to n-1 then re-add "-"
	// leftPadInt( -17, 3 ) => -17
	// leftPadInt(  17, 5 ) => 00017
	// leftPadInt( -17, 5 ) => -0017
	public static String leftPadInt( int theInteger, int desiredLength )
	{
		if( theInteger >= 0 )
		{
			return leftPadString( "" + theInteger, desiredLength,
				'0', false
				);
		}
		// Else integer is < 0
		else
		{
			int tmpInt = theInteger * -1;
			String tmpIntStr = "" + tmpInt;
			int newLen = desiredLength - 1;
			String tmpAns = leftPadString( tmpIntStr, newLen,
				'0', false
				);
			tmpAns = "-" + tmpAns;
			return tmpAns;
		}
	}

	public static String leftPadInt( long theLong, int desiredLength )
	{
		if( theLong >= 0L )
		{
			return leftPadString( "" + theLong, desiredLength,
				'0', false
				);
		}
		// Else integer is < 0
		else
		{
			long tmpLong = theLong * -1L;
			String tmpIntStr = "" + tmpLong;
			int newLen = desiredLength - 1;
			String tmpAns = leftPadString( tmpIntStr, newLen,
				'0', false
				);
			tmpAns = "-" + tmpAns;
			return tmpAns;
		}
	}

	// Will always return a valid string, ""
	public static String zapChars( String source )
	{
		return zapChars( source, ' ' );
	}
	public static String zapChars( String source, char c )
	{
		final String kFName = "zapChars(2)";

		if( source == null )
		{
			errorMsg( kFName, "null source text passed in, returning empty string." );
			return "";
		}
		String answer = source;
		// Usually Get rid of white space
		if( c!=' ' && c!='\t' && c!='\r' && c!='\n' )
		{
			answer = answer.replace( ' ', c );
			answer = answer.replace( '\t', c );
		}
		// Other bad chars to look out for
		// Note that we do allow for - and _
		String badList = "\"'\\/\r\n:{}()[]<>&%#~`!@$^*+=?|,.";
		for( int i=0; i<badList.length(); i++ ) {
			char badC = badList.charAt(i);
			if( badC != c )
				answer = answer.replace( badC, c );
			// Todo: also check for out of range characters
		}

		return answer;
	}

	public static String replaceChars(
		String inSource, char inOldC, char inNewC
		)
	{
		final String kFName = "replaceChars";

		if( null == inSource ) {
			errorMsg( kFName,
				"Null source string passed in, returning null."
				);
			return null;
		}
		if( inOldC == inNewC ) {
			errorMsg( kFName,
				"Original and new characters are the same '" + inNewC +"'"
				+ " Nothing to do, returning original string."
				);
			return inSource;
		}

		// Skip entirely if the old char isn't even in the string
		if( inSource.indexOf(inOldC) < 0 )
			return inSource;

		StringBuffer inBuff = new StringBuffer( inSource );
		StringBuffer outBuff = new StringBuffer();
		for( int i=0; i < inBuff.length(); i++ )
		{
			char c = inBuff.charAt( i );
			if( c == inOldC ) {
				if( '\000' != inNewC )
					outBuff.append( inNewC );
			}
			else
				outBuff.append( c );
		}

		return new String( outBuff );
	}

	// Escape apostrophies with extra apostrophies for strings that
	// will be used in SQL statements
	public static String sqlEscapeString( String inString, boolean inDoWarnings )
	{
		final String kFName = "sqlEscapeString";
		if( inString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName, "Null string passed in, returning null." );
			return inString;
		}
		// Quickie escape if nothing to do
		if( inString.indexOf( '\'' ) < 0 )
			return inString;
		// OK, we have a string, and it has at least one of the little buggers
		StringBuffer fromBuff = new StringBuffer( inString );
		StringBuffer toBuff = new StringBuffer();
		// Loop from in to out
		for( int i=0; i<fromBuff.length(); i++ )
		{
			// Get from in buff
			char c = fromBuff.charAt( i );
			// If it's an apos, add an extra one first
			if( c == '\'' )
				toBuff.append( '\'' );
			// always copy over the actual character to out buff
			toBuff.append( c );
		}
		// And our answer
		return new String( toBuff );
	}

	// Convenience wrapper so that it's
	// symetrical with Decode
	public static String htmlEncodeString(
		String inString, boolean inDoWarnings )
	{
		return htmlEscapeString( inString, inDoWarnings );
	}
	public static String htmlEscapeString(
		String inString, boolean inDoWarnings )
	{
		final String kFName = "htmlEscapeString";
		if( inString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName, "Null string passed in, returning null." );
			return inString;
		}
		// Quickie escape if nothing to do
		if( inString.indexOf( '<' ) < 0
			&& inString.indexOf( '>' ) < 0
			&& inString.indexOf( '"' ) < 0
			&& inString.indexOf( '&' ) < 0
			)
		{
			return inString;
		}

		// OK, we have a string, and it has at least one of the little buggers
		StringBuffer fromBuff = new StringBuffer( inString );
		StringBuffer toBuff = new StringBuffer();
		// Loop from in to out
		for( int i=0; i<fromBuff.length(); i++ )
		{
			// Get from in buff
			char c = fromBuff.charAt( i );
			// If it's an apos, add an extra one first
			if( c == '<' )
				toBuff.append( "&lt;" );
			else if( c == '>' )
				toBuff.append( "&gt;" );
			else if( c == '"' )
				toBuff.append( "&quot;" );
			else if( c == '&' )
				toBuff.append( "&amp;" );
			else
				toBuff.append( c );
		}
		// And our answer
		return new String( toBuff );
	}

	// This is just replace &amp; back into &
	public static String decodeUrlFromInsideHtmlOrNull( String inUrl )
	{
		if( null==inUrl )
			return null;
		return inUrl.replaceAll( "[&][aA][mM][pP][;]", "&" );
	}
	public static String encodeUrlForInsideHtmlOrNull( String inUrl )
	{
		if( null==inUrl )
			return null;
		return inUrl.replaceAll( "[&]", "&amp;" );
	}

	// Encode an ENTIRE URL up into a string that can be passed as
	// a CGI argument in another PARENT URL
	// Very aggressive encoding
	// basically anything not a letter or digit is encoded as %NN
	// Java's built in routines are not that aggressive
	// but we want to make sure there are spurious colons, dots or slashes
	// to throw off parsing of PARENET URL
	public static String encodeUrlAllPunctOrNull( String inUrl )
	{
		final String kFName = "encodeUrlAllPunctOrNull";
		if( null==inUrl )
			return null;
		final String nonAlphaNumPatternStr = "[^A-Za-z0-9]";
		final Pattern nonAalphaNumPattern = Pattern.compile(nonAlphaNumPatternStr);
	     // Get a matcher object - we cover this next.
		Matcher m = nonAalphaNumPattern.matcher( inUrl );
		StringBuffer outBuff = new StringBuffer();
		boolean gotAMatch = m.find();
		// Loop through and create a new String 
		// with the replacements
		while( gotAMatch )
		{
			// m.start(), .end(), .group()
			String charStr = m.group();
			try {
				byte [] byteAry = charStr.getBytes( AuxIOInfo.CHAR_ENCODING_UTF8 );
				StringBuffer tmpBuff = new StringBuffer();
				for( int i=0; i<byteAry.length; i++ )
				{
					byte b = byteAry[i];
					// convert to 2 digit hex
					String bStr = Integer.toHexString( b ).toUpperCase();
					if( bStr.length() < 2 )
						bStr = "0" + bStr;
					// append % nn to str
					tmpBuff.append( '%' );
					tmpBuff.append( bStr );
				}
	
				m.appendReplacement(outBuff, new String(tmpBuff) );
			}
			catch( UnsupportedEncodingException e )
			{
				errorMsg( kFName, "Exception with encoding: " + e
					+ ", Encoding = '" + AuxIOInfo.CHAR_ENCODING_UTF8 + "'"
					+ ", charStr='" + charStr + "'"
					+ ", inUrl='" + inUrl + "'"
					+ ". Returning null"
					);
				return null;
			}
			gotAMatch = m.find();
		}
        // Add the last segment of input to the new String
		m.appendTail( outBuff );
		// We're done
		return new String( outBuff );
	}
	public static String decodeUrlAllPunctOrNull( String inUrl )
	{
		final String kFName = "decodeUrlAllPunctOrNull";
		if( null==inUrl )
			return null;
		final String percentHexPatternStr = "([%][A-Fa-f0-9][A-Fa-f0-9])+";
		final Pattern percentHexPattern = Pattern.compile(percentHexPatternStr);
	     // Get a matcher object - we cover this next.
		Matcher m = percentHexPattern.matcher( inUrl );
		StringBuffer outBuff = new StringBuffer();
		boolean gotAMatch = m.find();
		// Loop through and create a new String 
		// with the replacements
		while( gotAMatch )
		{
			// m.start(), .end(), .group()
			String charStr = m.group();
			String [] octets = charStr.split("[%]");
			byte [] bytes = new byte[ octets.length ];
			String valStr = null;
			for( int i=0; i<octets.length; i++ )
			{
				valStr = octets[i];
				int iVal = Integer.parseInt( valStr, 16 );
				bytes[i] = (byte) iVal;
			}
			try {
				String chars = new String( bytes, AuxIOInfo.CHAR_ENCODING_UTF8 );
				m.appendReplacement(outBuff, chars );
			}
			catch( UnsupportedEncodingException e )
			{
				errorMsg( kFName, "Exception with decoding: " + e
					+ ", Encoding = '" + AuxIOInfo.CHAR_ENCODING_UTF8 + "'"
					+ ", valStr='" + valStr + "'"
					+ ", charStr='" + charStr + "'"
					+ ", inUrl='" + inUrl + "'"
					+ ". Returning null"
					);
				return null;
			}
			gotAMatch = m.find();
		}
        // Add the last segment of input to the new String
		m.appendTail( outBuff );
		// We're done
		return new String( outBuff );
	}
	
	public static String htmlDecodeString(
		String inString, boolean inDoWarnings )
	{
		final String kFName = "htmlDecodeString";
		if( inString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName, "Null string passed in, returning null." );
			return inString;
		}
		// Quickie escape if nothing to do
		if( inString.indexOf( '<' ) < 0
			&& inString.indexOf( '>' ) < 0
			&& inString.indexOf( '"' ) < 0
			&& inString.indexOf( '&' ) < 0
			)
		{
			return inString;
		}

		// OK, we have a string, and it has at least one of the little buggers
		StringBuffer fromBuff = new StringBuffer( inString );
		StringBuffer toBuff = new StringBuffer();
		// Loop from in to out
		for( int i=0; i<fromBuff.length(); i++ )
		{
			// Get from in buff
			char c = fromBuff.charAt( i );
			// If it's an apos, add an extra one first
			if( c == '<' )
				toBuff.append( "&lt;" );
			else if( c == '>' )
				toBuff.append( "&gt;" );
			else if( c == '"' )
				toBuff.append( "&quot;" );
			else if( c == '&' )
				toBuff.append( "&amp;" );
			else
				toBuff.append( c );
		}
		// And our answer
		return new String( toBuff );
	}

	public static Hashtable sqlEscapeStringHash( Hashtable inHash )
	{
		return sqlEscapeStringHash( inHash, true );
	}
	public static Hashtable sqlEscapeStringHash(
		Hashtable inHash, boolean inDisplayErrors
		)
	{
		final String kFName = "sqlEscapeStringHash";
		Hashtable outHash = new Hashtable();
		if( null == inHash )
		{
			if( inDisplayErrors )
				errorMsg( kFName,
					"Null input hash passed in. Returning empty hash."
					);
			return outHash;
		}
		List keys = new Vector();
		keys.addAll( inHash.keySet() );
		for( Iterator it = keys.iterator(); it.hasNext(); )
		{
			String key = (String) it.next();
			String value = (String) inHash.get( key );
			String newValue = sqlEscapeString( value, inDisplayErrors );
			if( null==newValue )	// should not be possibe
			{
				if( inDisplayErrors )
					errorMsg( kFName,
						"Null value for key \"" + key + "\""
						+ " Skipping."
						);
				continue;
			}
			outHash.put( key, newValue );
		}
		return outHash;
	}

	public static Hashtable htmlEscapeStringHash( Hashtable inHash )
	{
		return htmlEscapeStringHash( inHash, true );
	}
	public static Hashtable htmlEscapeStringHash(
		Hashtable inHash, boolean inDisplayErrors
		)
	{
		final String kFName = "htmlEscapeStringHash";
		Hashtable outHash = new Hashtable();
		if( null == inHash )
		{
			if( inDisplayErrors )
				errorMsg( kFName,
					"Null input hash passed in. Returning empty hash."
					);
			return outHash;
		}
		List keys = new Vector();
		keys.addAll( inHash.keySet() );
		for( Iterator it = keys.iterator(); it.hasNext(); )
		{
			String key = (String) it.next();
			String value = (String) inHash.get( key );
			String newValue = htmlEscapeString( value, inDisplayErrors );
			if( null==newValue )	// should not be possibe
			{
				if( inDisplayErrors )
					errorMsg( kFName,
						"Null value for key \"" + key + "\""
						+ " Skipping."
						);
				continue;
			}
			outHash.put( key, newValue );
		}
		return outHash;
	}

	public static double formatDoubleToDisplayPrecision( double inValue )
	{
		long lValue;
		double outValue;
		if( inValue >= 10 || inValue <= -10 )
		{
			lValue = Math.round( inValue );
			outValue = lValue;
		}
		else
		{
			lValue = Math.round( inValue * 10.0 );
			outValue = lValue / 10.0;
		}
		return outValue;
	}

	public static String formatPercentage(
			long inNumerator, long inDenominator,
			int inDecimalPlaces
		)
	{
		// If zero on bottom, give back ######
		if( 0 == inDenominator ) {
			int goalLen = 3 + 1;	// 1 0 0 %
			if( inDecimalPlaces > 0 )
				goalLen += inDecimalPlaces + 1;	// .00
			return dupeSequence( "#", goalLen, true );
		}

		double ratio = (double) inNumerator / (double) inDenominator;

		return formatPercentage( ratio, inDecimalPlaces );
	}

	public static double calcPercentage(
			long inNumerator, long inDenominator,
			int inDecimalPlaces
		)
	{
		// If zero on bottom, give back ######
		if( 0 == inDenominator ) {
			return Double.MAX_VALUE;
		}

		if( inDecimalPlaces < 0 )
			inDecimalPlaces = 0;

		double ratio = (double) inNumerator / (double) inDenominator;

		ratio = ratio * 100.0;
		if( inDecimalPlaces >= 0 ) {
			ratio = NIEUtil.round( ratio, inDecimalPlaces );
		}

		return ratio;
	}


	public static String formatPercentage( double inNumber, int inDecimalPlaces )
	{

		if( inDecimalPlaces < 0 )
			inDecimalPlaces = 0;

		inNumber = inNumber * 100.0;
		if( inDecimalPlaces >= 0 ) {
			inNumber = NIEUtil.round( inNumber, inDecimalPlaces );
		}
		String outStr = ""+inNumber;
		if( inDecimalPlaces <= 0 ) {
			// By default we drop trailing .0
			if( outStr.endsWith(".0") )
				outStr = outStr.substring( 0, outStr.length()-2 );
		}
		// we do want trailing zeros
		else {
			int dotAt = outStr.lastIndexOf('.');
			if( dotAt < 0 ) {
				outStr += '.';
				dotAt = outStr.length()-1;
			}
			int padCount = inDecimalPlaces - ( outStr.length() - dotAt - 1 );
			for( int i=0; i<padCount ; i++ )
				outStr += '0';
		}

		outStr += '%';

		return outStr;
	}



	// This is basically a convernience wrapper around
	// Integer.parseInt which avoids thrown NumberFormatException exceptions
	// Use this if you DON'T CARE whether a string was present or not
	// We will still warn about an invalid format, etc.
	// You can also ask it to throw a warning if you really want,
	// but it does NOT throw a warning by default - typically if you care
	// about such things you'll check the string and parse exception yourself.
	// This one defaults to zero
	public static int stringToIntOrDefaultValue( String inStr )
	{
		return stringToIntOrDefaultValue( inStr, 0, false, true );
	}
	// This one defaults to no warnings for empty valuues
	public static int stringToIntOrDefaultValue( String inStr, int inDefault )
	{
		return stringToIntOrDefaultValue( inStr, inDefault, false, true );
	}
	public static int stringToIntOrDefaultValue(
		String inStr, int inDefault,
		boolean inDoEmptyStringWarnings,
		boolean inDoFormatWarnings
		)
	{
		final String kFName = "stringToIntOrDefaultValue";
		String tmpStr = trimmedStringOrNull( inStr );
		if( tmpStr == null )
		{
			if( inDoEmptyStringWarnings )
				errorMsg( kFName,
					"Was passed in null or empty string."
					+ " Will return default value of " + inDefault + "."
					);
			// throw new Error( "null value" );
			return inDefault;
		}

		int outInt = inDefault;
		try
		{
			outInt = Integer.parseInt( tmpStr );
		}
		catch (Exception e)  // Actually NumberFormatException
		{
			if( inDoFormatWarnings )
				errorMsg( kFName,
					"Unable to parse an integer from the input string."
					+ " Exception was \"" + e + "\"."
					+ " Input string was \"" + inStr + "\"."
					+ " Will return default value of " + inDefault + "."
					);
			outInt = inDefault;
		}
		// return whatever's left
		return outInt;
	}


	// This is basically a convernience wrapper around
	// Integer.parseInt which avoids thrown NumberFormatException exceptions
	// Use this if you DON'T CARE whether a string was present or not
	// We will still warn about an invalid format, etc.
	// You can also ask it to throw a warning if you really want,
	// but it does NOT throw a warning by default - typically if you care
	// about such things you'll check the string and parse exception yourself.
	// This one defaults to zero
	public static long stringToLongOrDefaultValue( String inStr )
	{
		return stringToLongOrDefaultValue( inStr, 0, false, true );
	}

	// This one defaults to no warnings for empty valuues
	public static long stringToLongOrDefaultValue( String inStr, int inDefault )
	{
		return stringToLongOrDefaultValue( inStr, inDefault, false, true );
	}

	public static long stringToLongOrDefaultValue(
		String inStr, long inDefault,
		boolean inDoEmptyStringWarnings,
		boolean inDoFormatWarnings
		)
	{
		final String kFName = "stringToLongOrDefaultValue";
		String tmpStr = trimmedStringOrNull( inStr );
		if( tmpStr == null )
		{
			if( inDoEmptyStringWarnings )
				errorMsg( kFName,
					"Was passed in null or empty string."
					+ " Will return default value of " + inDefault + "."
					);
			// throw new Error( "null value" );
			return inDefault;
		}

		long outInt = inDefault;
		try
		{
			outInt = Long.parseLong( tmpStr );
		}
		catch (Exception e)  // Actually NumberFormatException
		{
			if( inDoFormatWarnings )
				errorMsg( kFName,
					"Unable to parse an long-int from the input string."
					+ " Exception was \"" + e + "\"."
					+ " Input string was \"" + inStr + "\"."
					+ " Will return default value of " + inDefault + "."
					);
			outInt = inDefault;
		}
		// return whatever's left
		return outInt;
	}





	// We always return a string, maybe just "<font>"
	public static String makeFontTagString(
		String inSize, String inColor, String inFace
		)
	{
		StringBuffer outBuff = new StringBuffer();
		outBuff.append( "<font" );

		// the Size
		inSize = trimmedStringOrNull( inSize );
		if( inSize != null )
		{
			outBuff.append( " size=\"" );
			outBuff.append( inSize );
			outBuff.append( '"' );
		}

		// the Color
		inColor = trimmedStringOrNull( inColor );
		if( inColor != null )
		{
			outBuff.append( " color=\"" );
			outBuff.append( inColor );
			outBuff.append( '"' );
		}

		// Font face
		inFace = trimmedStringOrNull( inFace );
		if( inFace != null )
		{
			outBuff.append( " face=\"" );
			outBuff.append( inFace );
			outBuff.append( '"' );
		}

		outBuff.append( '>' );

		return new String( outBuff );
	}

	// We always return a string, maybe just "<font>"
	public static String makeLinkedImageTagString(
				String inSrc, String inAlt,
				String inHref, String inTarget,
				String inWidth, String inHeight,
				String inBorder, String inHSpace, String inVSpace,
				boolean inDoWarnings
		)
	{
		final String kFName = "makeLinkedImageTagString";

		// We do not want a border by default
		String tmpBorder = inBorder != null ? inBorder : "0";

		String imgTag = makeImageTagString(
				inSrc, inAlt,
				inWidth, inHeight,
				tmpBorder, inHSpace, inVSpace
			);

		String outTag = null;

		inHref = trimmedStringOrNull( inHref );
		if( inHref == null )
		{
			outTag = imgTag;
			if( inDoWarnings )
				errorMsg( kFName,
					"No href given, will ruturn just the image tag."
					);
		}
		else    // Else we do have a src
		{
			// Buil the full string
			StringBuffer outBuff = new StringBuffer();
			// First, the opening anchor
			outBuff.append( "<a href=\"" );
			outBuff.append( inHref );
			outBuff.append( '"' );
			// The target, if any
			inTarget = trimmedStringOrNull( inTarget );
			if( inTarget != null )
			{
				outBuff.append( " target=\"" );
				outBuff.append( inTarget );
				outBuff.append( '"' );
			}
			// end of opening anchor
			outBuff.append( '>' );
			// Now the image
			outBuff.append( imgTag );
			// And then the closing anchor tag
			outBuff.append( "</a>" );
			// And prepare the answer
			outTag = new String( outBuff );
		}   // End else we DID have an href

		// Return the answer
		return outTag;
	}

	// We always return a string, maybe just "<img>"
	public static String makeImageTagString(
				String inSrc, String inAlt,
				String inWidth, String inHeight,
				String inBorder, String inHSpace, String inVSpace
		)
	{
		final String kFName = "makeImageTagString";

		StringBuffer outBuff = new StringBuffer();
		outBuff.append( "<img" );

		// the src itself
		inSrc = trimmedStringOrNull( inSrc );
		if( inSrc != null )
		{
			outBuff.append( " src=\"" );
			outBuff.append( inSrc );
			outBuff.append( '"' );
		}
		else
		{
			warningMsg( kFName,
				"No src field passed in for image tag?"
				+ " Will finish making tag but doubt it will work correctly."
				);
		}

		// the Alt tag
		// TODO: maybe encode this at some point???
		inAlt = trimmedStringOrNull( inAlt );
		if( inAlt != null )
		{
			outBuff.append( " alt=\"" );
			outBuff.append( inAlt );
			outBuff.append( '"' );
		}

		// border
		inBorder = trimmedStringOrNull( inBorder );
		if( inBorder != null )
		{
			outBuff.append( " border=\"" );
			outBuff.append( inBorder );
			outBuff.append( '"' );
		}


		// the height
		inHeight = trimmedStringOrNull( inHeight );
		if( inHeight != null )
		{
			outBuff.append( " height=\"" );
			outBuff.append( inHeight );
			outBuff.append( '"' );
		}

		// the Width
		inWidth = trimmedStringOrNull( inWidth );
		if( inWidth != null )
		{
			outBuff.append( " width=\"" );
			outBuff.append( inWidth );
			outBuff.append( '"' );
		}

		// the HSpace
		inHSpace = trimmedStringOrNull( inHSpace );
		if( inHSpace != null )
		{
			outBuff.append( " hspace=\"" );
			outBuff.append( inHSpace );
			outBuff.append( '"' );
		}

		// the VSpace
		inVSpace = trimmedStringOrNull( inVSpace );
		if( inVSpace != null )
		{
			outBuff.append( " vspace=\"" );
			outBuff.append( inVSpace );
			outBuff.append( '"' );
		}

		outBuff.append( " />" );

		return new String( outBuff );
	}


	// We always return a string, maybe just "<table>"
	public static String makeTableTagString(
		String inWidth, String inBorder,
		String inCellPadding, String inCellSpacing,
		String inBGColor, String inBGImage
		)
	{

		StringBuffer outBuff = new StringBuffer();
		outBuff.append( "<table" );

		// the Width tag
		inWidth = trimmedStringOrNull( inWidth );
		if( inWidth != null )
		{
			outBuff.append( " width=\"" );
			outBuff.append( inWidth );
			outBuff.append( '"' );
		}


		// the Border tag
		inBorder = trimmedStringOrNull( inBorder );
		if( inBorder != null )
		{
			outBuff.append( " border=\"" );
			outBuff.append( inBorder );
			outBuff.append( '"' );
		}



		// Cell padding
		inCellPadding = trimmedStringOrNull( inCellPadding );
		if( inCellPadding != null )
		{
			outBuff.append( " cellpadding=\"" );
			outBuff.append( inCellPadding );
			outBuff.append( '"' );
		}

		// Cell spacing
		inCellSpacing = trimmedStringOrNull( inCellSpacing );
		if( inCellSpacing != null )
		{
			outBuff.append( " cellspacing=\"" );
			outBuff.append( inCellSpacing );
			outBuff.append( '"' );
		}


		// Cell padding
		inBGColor = trimmedStringOrNull( inBGColor );
		if( inBGColor != null )
		{
			outBuff.append( " bgcolor=\"" );
			outBuff.append( inBGColor );
			outBuff.append( '"' );
		}

		// Cell spacing
		inBGImage = trimmedStringOrNull( inBGImage );
		if( inBGImage != null )
		{
			outBuff.append( " background=\"" );
			outBuff.append( inBGImage );
			outBuff.append( '"' );
		}

		// Wrap up
		outBuff.append( " />" );

		return new String( outBuff );
	}


	public static String listOfStringsToSingleString( List inList )
	{
		final String kFName = "listOfStringsToSingleString";
		String separator = ", ";
		boolean addQuotes = true;

		if( inList == null || inList.size() < 1 )
		{
			warningMsg( kFName,
				"Null or empty list, returning empty string."
				);
			return "";
		}

		StringBuffer buff = new StringBuffer();
		boolean isFirst = true;
		// For each value in the list
		for( Iterator it = inList.iterator(); it.hasNext() ; )
		{
			// Get the value
			String value = (String) it.next();
			// If not the first, add the separator after the previous value
			if( ! isFirst )
				buff.append( separator );
			// Quote, value, quote
			if( addQuotes )
				buff.append( '"' );
			buff.append( value );
			if( addQuotes )
				buff.append( '"' );
			// And we're not the first
			isFirst = false;
		}
		return new String( buff );
	}

	public static String listOfStringsToSingleString2(
		List inList,
		boolean inNullTrimValues, boolean inAddQuotes, boolean inReturnTrueNull,
		String optSeparator,
		boolean inDoWarnings
		)
	{
		final String kFName = "listOfStringsToSingleString2";
		String separator = null==optSeparator ? ", " : optSeparator;

		if( inList == null || inList.size() < 1 )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Null or empty list, returning empty string."
					);
			if( inReturnTrueNull )
				return null;
			else
				return "";
		}

		StringBuffer buff = new StringBuffer();
		boolean isFirst = true;
		// For each value in the list
		for( Iterator it = inList.iterator(); it.hasNext() ; )
		{
			// Get the value
			String value = (String) it.next();
			if( inNullTrimValues ) {
				value = trimmedStringOrNull(value);
				if( null==value ) {
					if( inDoWarnings )
						errorMsg( kFName,
							"Skipping null/empty value."
							);
					continue;
				}
			}
			// If not the first, add the separator after the previous value
			if( ! isFirst )
				buff.append( separator );
			// Quote, value, quote
			if( inAddQuotes )
				buff.append( '"' );
			buff.append( value );
			if( inAddQuotes )
				buff.append( '"' );
			// And we're not the first
			isFirst = false;
		}
		if( buff.length() < 1 ) {
			if( inDoWarnings )
				errorMsg( kFName,
					"No buffer to return."
					);
			if( inReturnTrueNull )
				return null;
			else
				return "";
		}
		else
			return new String( buff );
	}



	public static List singleCommaStringToUniqueListOfStrings( String inSourceString,
		boolean inDoWarnings
		)
	{
		return singleStringToListOfStrings( inSourceString,
			",",	// String optDelimiters,
			true,	// boolean inDoTrimNull,
			true,	// boolean inDumpOuterQuotes,
			false,	// boolean inNormalizeToLowerCase,
			false,	// boolean inAllowDupes,
			false,	// boolean inIsDupeCheckCasen,
			inDoWarnings
			);
	}

	// Does a single string subsitution
	// TODO: do one that does them all
	// Returns null if no substitution was made
	// replace with can be null
	// IS case sensitive
	public static String simpleSubstitution( String inSourceString,
		String inPattern, String inReplaceWith
		)
	{
		final String kFName = "simpleSubstitution";
		if( null==inSourceString || null==inPattern || inPattern.length()<1 ) {
			errorMsg( kFName,
				"Null/empty input(s), at least the first two must be given."
				+ " inSourceString=\"" + inSourceString + "\""
				+ ", inPattern=\"" + inPattern + "\""
				+ ", inReplaceWith=\"" + inReplaceWith + "\""
				);
			return null;
		}
		int patternAt = inSourceString.indexOf(inPattern);
		// Return a null if the pattern is not found
		if( patternAt < 0 )
			return null;
		StringBuffer buff = new StringBuffer( inSourceString );
		int endsAt = patternAt + inPattern.length();
		if( null!=inReplaceWith )
			buff.replace( patternAt, endsAt, inReplaceWith );
		else
			buff.delete( patternAt, endsAt );
		return new String( buff );
	}


	public static String hashKeysToSingleString( Hashtable inHash )
	{
		final String kFName = "hashKeysToSimgleString";

		if( inHash == null )
		{
			warningMsg( kFName,
				"Null or empty hash, returning empty string."
				);
			return "";
		}
		Set keySet = inHash.keySet();
		List keyList = new Vector(keySet);
		return listOfStringsToSingleString( keyList );
	}




	private static void __Dates_and_Times__() {}
	//////////////////////////////////////////////////////////////


	// NIE usually SQL reports usually 'MM/DD/YY HH:MI:SS am'
	// In SimpleDateFormat that would be: "MM/dd/yy HH:mm:ss a"  // but will give upper case AM/PM
	// But then we have/had:
	// DateFormat formatter = DateFormat.getDateTimeInstance(
	// 	DateFormat.SHORT, DateFormat.LONG );
	// Gives: 7/15/02 4:34:45 PM PDT
	// Also DateFormat.FULL, DateFormat.FULL
	// Example: July 15, 2002 4:33:39 PM PDT
	// Also DateFormat.SHORT, DateFormat.SHORT
	// Example: 7/15/02 4:32 PM
	//
	//Java date format characters
	//(complete list in SimpleDateFormat class doc)
	//http://javaalmanac.com/egs/java.text/FormatDate.html
	//==============================
	//	Format formatter;
	//
	//	// The year
	//	formatter = new SimpleDateFormat("yy");    // 02
	//	formatter = new SimpleDateFormat("yyyy");  // 2002
	//
	//	// The month
	//	formatter = new SimpleDateFormat("M");     // 1
	//	formatter = new SimpleDateFormat("MM");    // 01
	//	formatter = new SimpleDateFormat("MMM");   // Jan
	//	formatter = new SimpleDateFormat("MMMM");  // January
	//
	//	// The day
	//	formatter = new SimpleDateFormat("d");     // 9
	//	formatter = new SimpleDateFormat("dd");    // 09
	//
	//	// The day in week
	//	formatter = new SimpleDateFormat("E");     // Wed
	//	formatter = new SimpleDateFormat("EEEE");  // Wednesday
	//
	// h H for 5pm = 5 17
	// hh HH for 5pm = 05 17
	// hh HH for 5am = 05 05

	//	// Get today's date
	//	Date date = new Date();
	//
	//	// Some examples
	//	formatter = new SimpleDateFormat("MM/dd/yy");
	//	String s = formatter.format(date);
	//	// 01/09/02
	//
	//	formatter = new SimpleDateFormat("dd-MMM-yy");
	//	s = formatter.format(date);
	//	// 29-Jan-02
	//
	//	// Examples with date and time; see also
	//	// e316 Formatting the Time Using a Custom Format
	//	formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	//	s = formatter.format(date);
	//	// 2002.01.29.08.36.33
	//
	//	formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
	//	s = formatter.format(date);
	//	// Tue, 09 Jan 2002 22:14:02 -0500
	//==============================
	//
	//With a Locale / country specific
	//====================================
	//http://javaalmanac.com/egs/java.text/FormatDateLoc.html?l=rel
	//
	//To format and parse in a particular locale, specify the locale when creating the SimpleDateFormat object. 
	//	Locale locale = Locale.FRENCH;
	//
	//	// Format with a custom format
	//	DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy", locale);
	//	String s = formatter.format(new Date());
	//	// mar., 29 janv. 2002
	//
	//	// Format with a default format
	//	s = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(new Date());
	//	// 29 janv. 2002
	//
	//
	//	try {
	//		// Parse with a custom format
	//		formatter = new SimpleDateFormat("E, dd MMM yyyy", locale);
	//		Date date = (Date)formatter.parse("mar., 29 janv. 2002");
	//
	//		// Parse with a default format
	//		date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).parse("29 janv. 2002");
	//	} catch (ParseException e) {
	//	}

	public static long getCurrTimeMillis()
	{
		return System.currentTimeMillis();
	}

	public static String formatDateToString( long inDate )
	{
		return formatDateToString( new Date( inDate ) );
	}

	public static String formatDateToString( Date inDate )
	{
		return formatDateToString( inDate, DEFAULT_NIE_DATETIME_FORMAT );
	}

	public static String formatDateToString( Date inDate, String inFormat )
	{
		return formatDateToString( inDate, inFormat, true );
	}


	// See SimpleDateFormat and sn.ReportConstants
	// we take quite a bit of liberties with the format string Java expects
	// Of note:
	// mm = month number, with 0
	// mi = minute
	// _m = month, with no leading 0 if < 10
	public static String formatDateToString( Date inDate, String inFormat, boolean inComplainOnError )
	{
		final String kFName = "formatDateToString";
		if( null==inDate || null==inFormat ) {
			if( inComplainOnError )
				errorMsg( kFName,
					"Null input(s), returning null."
					+ " inDate=\"" + inDate + "\""
					+ ", inFormat=\"" + inFormat + "\""
					);
			return null;
		}




		// Fix and remember special casing issues
		// We do this inline vs. another method because we sometimes
		// set some state variables
		// am/pm
		// Name of day (Monday, Tuesday, etc.)
		// months
		// minutes
		// Name of month
		// seconds
		// years
		// days
		// hours

		// Fix common format aliases
		// We need to get to "MM/dd/yy hh:mm:ss a"
		String fixedUpFormat = inFormat;
		String tmpStr = null;

		// First, fix up the AM/PM bs
		boolean forceAMtoLower = false;
		// Set AM to a
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"AM", "a")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// Set PM to a
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"PM", "a")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// Set am to a
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"am", "a")) != null ) {
			fixedUpFormat = tmpStr;
			forceAMtoLower = true;
		}
		// Set pm to a
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"pm", "a")) != null ) {
			fixedUpFormat = tmpStr;
			forceAMtoLower = true;
		}

		// The NAMES of days
		boolean forceDayNameToUpperCase = false;
		boolean forceDayNameToLowerCase = false;
		boolean usingDayNameAbbrev = false;
		boolean forceSuperShortDayNameAbbrev = false;
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"Dy", "EEE")) != null ) {
			fixedUpFormat = tmpStr;
			usingDayNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"DY", "EEE")) != null ) {
			fixedUpFormat = tmpStr;
			forceDayNameToUpperCase = true;
			usingDayNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"dy", "EEE")) != null ) {
			fixedUpFormat = tmpStr;
			forceDayNameToLowerCase = true;
			usingDayNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"Day", "EEEE")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"DAY", "EEEE")) != null ) {
			fixedUpFormat = tmpStr;
			forceDayNameToUpperCase = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"day", "EEEE")) != null ) {
			fixedUpFormat = tmpStr;
			forceDayNameToLowerCase = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"Da", "E")) != null ) {
			fixedUpFormat = tmpStr;
			forceSuperShortDayNameAbbrev = true;
			usingDayNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"DA", "E")) != null ) {
			fixedUpFormat = tmpStr;
			forceSuperShortDayNameAbbrev = true;
			forceDayNameToUpperCase = true;
			usingDayNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"da", "E")) != null ) {
			fixedUpFormat = tmpStr;
			forceSuperShortDayNameAbbrev = true;
			forceDayNameToLowerCase = true;
			usingDayNameAbbrev = true;
		}


		// Handle Months before Minutes
		// force mm to MM
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"mm", "MM")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"_m", "M")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"_M", "M")) != null ) {
			fixedUpFormat = tmpStr;
		}

		// Handle minutes
		// Force mi and MI to mm
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"mi", "mm")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"MI", "mm")) != null ) {
			fixedUpFormat = tmpStr;
		}


		// The NAMES of Months
		boolean forceMonthNameToUpperCase = false;
		boolean forceMonthNameToLowerCase = false;
		boolean usingMonthNameAbbrev = false;
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"Month", "MMMM")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"MONTH", "MMMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToUpperCase = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"month", "MMMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToLowerCase = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"Mon", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			usingMonthNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"MON", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToUpperCase = true;
			usingMonthNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"mon", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToLowerCase = true;
			usingMonthNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"Mmm", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			usingMonthNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"MMM", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToUpperCase = true;
			usingMonthNameAbbrev = true;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"mmm", "MMM")) != null ) {
			fixedUpFormat = tmpStr;
			forceMonthNameToLowerCase = true;
			usingMonthNameAbbrev = true;
		}

		/*** this conflicts with our plans for Day, DAY, day etc.
		// Set a to a, but remmber they wanted lowercase
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"a", "a")) != null ) {
			fixedUpFormat = tmpStr;
			forceAMtoLower = true;
		}
		// Set A to a, but no lowercase
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"A", "a")) != null ) {
			fixedUpFormat = tmpStr;
		}
		***/


		// Force Seconds to lowercase
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"SS", "ss")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"S", "s")) != null ) {
			fixedUpFormat = tmpStr;
		}

		// Force Years to lowercase
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"YYYY", "yyyy")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"YY", "yy")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// else if( (tmpStr=simpleSubstitution(fixedUpFormat,"Y", "y")) != null ) {
		//	fixedUpFormat = tmpStr;
		//}


		// Force 1-31 Days to lowercase, force Julian to upper case
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"ddd", "DDD")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// keep the two day match from messing up the 3 day
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"DDD", "DDD")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"DD", "dd")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"_D", "d")) != null ) {
		 	fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"_d", "d")) != null ) {
			fixedUpFormat = tmpStr;
		}

		// fix Hours
		// 24 hour formats
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"H24", "H")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"HH24", "HH")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"h24", "H")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"hh24", "HH")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// 12 hour formats
		if( (tmpStr=simpleSubstitution(fixedUpFormat,"H12", "h")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"HH12", "hh")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"h12", "h")) != null ) {
			fixedUpFormat = tmpStr;
		}
		else if( (tmpStr=simpleSubstitution(fixedUpFormat,"hh12", "hh")) != null ) {
			fixedUpFormat = tmpStr;
		}
		// Else hopefully you know what you're doing!


		String answer = null;


		debugMsg( kFName,
			"Normalized date format \"" + inFormat + "\""
			+ " to \"" + fixedUpFormat + "\""
			);

		try {
			SimpleDateFormat formatter = new SimpleDateFormat(fixedUpFormat);
			answer = formatter.format(inDate);
		}
		catch( Throwable e ) {
			if( inComplainOnError )
				errorMsg( kFName,
					"Unable to format date, returning null String."
					+ " Input format string = \"" + inFormat + "\""
					+ ", normalized format = \"" + fixedUpFormat + "\""
					+ ", inDate=\"" + inDate + "\""
					+ " Error: " + e
					);
			return null;
		}

		// Fixup case of answers
		if( null!=answer && forceAMtoLower ) {
			// Set AM to am
			if( (tmpStr=simpleSubstitution(answer,"AM", "am")) != null ) {
				answer = tmpStr;
			}
			// Set PM to pm
			else if( (tmpStr=simpleSubstitution(answer,"PM", "pm")) != null ) {
				answer = tmpStr;
			}
			// No substitutions made?
			else
				errorMsg( kFName,
					"Could not find AM/PM to recase to am/pm; returning formatted date as-is."
					+ " Formatted date = \"" + answer + "\""
					+ ", orig date obj =\"" + inDate + "\""
					+ ", normalized format = \"" + fixedUpFormat + "\""
					+ " original format = \"" + inFormat + "\""
					);
		}


		final String [][] kDaysOfWeek = {
			{ "Sunday","Su" },
			{ "Monday","M" },
			{ "Tuesday","Tu" },
			{ "Wednesday","W" },
			{ "Thursday","Th" },
			{ "Friday","F" },
			{ "Saturday","Sa" },
		};

		// Fix the case of the names of days if requested
		if( null!=answer && (forceDayNameToUpperCase || forceDayNameToLowerCase) ) {
			// try a subst for each of the known days
			for( int i=0; i<kDaysOfWeek.length ; i++ ) {
				String origDay = kDaysOfWeek[i][0];
				// If abbreviations, then only the first 3 chars
				if( usingDayNameAbbrev )
					origDay = origDay.substring(0,3);
				// convert to upper or lower case case
				String newDay = forceDayNameToUpperCase ? origDay.toUpperCase() : origDay.toLowerCase();
				// Try the substitution
				String tmpStr2 = null;
				if( (tmpStr2=simpleSubstitution( answer, origDay, newDay )) != null ) {
					answer = tmpStr2;
					// Since there should only be one day, once we've done a subst we're done!
					break;
				}

			}
		}

		// Do the super day abbreviations if requested
		debugMsg( kFName, "forceSuperShortDayNameAbbrev=" + forceSuperShortDayNameAbbrev );
		if( null!=answer && forceSuperShortDayNameAbbrev ) {
			// try a subst for each of the known days
			for( int i=0; i<kDaysOfWeek.length ; i++ ) {
				String origDay = kDaysOfWeek[i][0];
				// If abbreviations, then only the first 3 chars
				if( usingDayNameAbbrev )
					origDay = origDay.substring(0,3);
				// And get what we will replace it with
				String newDay = kDaysOfWeek[i][1];
				// convert BOTH to upper or lower case case
				if( forceDayNameToUpperCase ) {
					origDay = origDay.toUpperCase();
					newDay = newDay.toUpperCase();
				}
				// Else maybe both to lower case
				if( forceDayNameToLowerCase ) {
					origDay = origDay.toLowerCase();
					newDay = newDay.toLowerCase();
				}
				// Else if mixed case, leave them alone

				// Try the substitution
				// debugMsg( kFName, "Day Abrev: answer=" + answer + " origDay=" + origDay + " newDay=" + newDay );
				String tmpStr3 = null;
				if( (tmpStr3=simpleSubstitution( answer, origDay, newDay )) != null ) {
					answer = tmpStr3;
					// Since there should only be one day, once we've done a subst we're done!
					break;
				}
			}
		}



		final String [] kMonthsOfYear = {
			"January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December"
		};

		// Fix the case of the names of Months if requested
		if( null!=answer && (forceMonthNameToUpperCase || forceMonthNameToLowerCase) ) {
			// try a subst for each of the known days
			for( int i=0; i<kMonthsOfYear.length ; i++ ) {
				String origMonth = kMonthsOfYear[i];
				// If abbreviations, then only the first 3 chars
				if( usingMonthNameAbbrev )
					origMonth = origMonth.substring(0,3);
				// convert to upper or lower case case
				String newMonth = forceMonthNameToUpperCase ? origMonth.toUpperCase() : origMonth.toLowerCase();
				// Try the substitution
				String tmpStr3 = null;
				if( (tmpStr3=simpleSubstitution( answer, origMonth, newMonth )) != null ) {
					answer = tmpStr3;
					// Since there should only be one month, once we've done a subst we're done!
					break;
				}
			}
		}




		return answer;
	}


	public static String formatTimeIntervalToNDaysFromMS(
		long inTime, boolean doAddCorrectPluralDays
		)
	{
		return formatTimeIntervalToNDaysFromMS(
			inTime, doAddCorrectPluralDays,
			null, null, null, null, null
			);
	}

	public static String formatTimeIntervalToNDaysFromMS(
		long inTime, boolean doAddCorrectPluralDays,
		String optPositivePrefix, String optPositiveSuffix,
		String optNegativePrefix, String optNegativeSuffix,
		String optNowStr
		)
	{

		double dTime = ( (double) inTime / (double) MS_PER_DAY);

		double dTime2 = formatDoubleToDisplayPrecision( dTime );

		String daysStr = ""+dTime2;
		if( daysStr.endsWith(".0") && daysStr.length()>2 )
			daysStr = daysStr.substring( 0, daysStr.length()-2 );

		String outStr = daysStr;
		if( doAddCorrectPluralDays ) {
			outStr += " day" + (daysStr.equals("1") || daysStr.equals("-1") ? "" : "s" );
		}

		if( null!=optNowStr && daysStr.equals("0") )
			outStr = optNowStr;
		else if( dTime2 > 0.0 && (null!=optPositivePrefix || null!=optPositiveSuffix) ) {
			outStr = ( null!=optPositivePrefix ? optPositivePrefix : "" )
				+ outStr
				+ ( null!=optPositiveSuffix ? optPositiveSuffix : "" )
				;
		}
		else if( dTime2 < 0.0 && (null!=optNegativePrefix || null!=optNegativeSuffix) ) {
			outStr = ( null!=optNegativePrefix ? optNegativePrefix : "" )
				+ outStr
				+ ( null!=optNegativeSuffix ? optNegativeSuffix : "" )
				;
		}

		return outStr;
	}

	// Convert milliseconds into a friendlier human readable string
	public static String formatTimeIntervalFancyMS( long inTime )
	{
		double currNum = inTime;

		String units = null;
		boolean doPluralCheck = true;

		// ms
		if( currNum < 1000.0 )
		{
			units = "ms";
			doPluralCheck = false;
		}
		else
		{
			currNum /= 1000.0;
			// seconds
			if( currNum < 60.0 )
			{
				units = "sec";
			}
			else
			{
				currNum /= 60.0;
				// mins
				if( currNum < 60.0 )
				{
					units = "min";
				}
				else
				{
					currNum /= 60.0;
					// hours
					if( currNum < 24.0 )
					{
						units = "hour";
					}
					else
					{
						currNum /= 24.0;
						units = "day";
					}
				}
			}
		}

		// Get the decimal number, rounded to int if > 10, to .1 if < 10
		double displayValue = formatDoubleToDisplayPrecision( currNum );
		String displayValueStr = "" + displayValue;
		if( displayValueStr.endsWith(".0") )
		{
			if( displayValueStr.length() > 2 )
				displayValueStr = displayValueStr.substring(
					0, displayValueStr.length()-2
					);
			// ^^^ it was length - 2 (".0".len) -1 (zero based offset) + 1
			// because substr( ... endIndex) means endIndex-1
			// so the -1 and +1 cancel out
		}

		if( doPluralCheck )
		{
			if( ! displayValueStr.equals("1")
				&& ! displayValueStr.equals("-1")
				)
			{
				units += "s";
			}
		}



		// Put together the final answer
		String outTimeStr = displayValueStr + " " + units;

		return outTimeStr;
	}





	public static String getTimestamp()
	{
		DateFormat formatter = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.LONG
			);
		// Gives: 7/15/02 4:34:45 PM PDT

		// Also DateFormat.FULL, DateFormat.FULL
		// Example: July 15, 2002 4:33:39 PM PDT
		// Also DateFormat.SHORT, DateFormat.SHORT
		// Example: 7/15/02 4:32 PM

		return formatter.format( new Date() );
	}
	public static String getCompactTimestamp()
	{
		// DateFormat formatter = DateFormat.getDateTimeInstance(
		//	DateFormat.SHORT, DateFormat.LONG
		//	);

		Format formatter = new SimpleDateFormat("yyyyMMddHHmmss");

		return formatter.format( new Date() );
	}
	public static String getCompactDateOnlyTimestamp()
	{
		// DateFormat formatter = DateFormat.getDateTimeInstance(
		//	DateFormat.SHORT, DateFormat.LONG
		//	);

		Format formatter = new SimpleDateFormat("yyyyMMdd");

		return formatter.format( new Date() );
	}
	// Returns in 2004... long format I think
	public static long getTimestampLong()
	{
		final String kFName = "getTimestampLong";
		String tmpStr = getCompactTimestamp();
		long outAnswer = 0L;
		try
		{
			outAnswer = Long.parseLong( tmpStr );
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Can't convert date tag to long, returning 0."
				+ " Date=\"" + tmpStr + "\""
				+ " Error: " + e
				);
		}
		return outAnswer;
	}
	public static String getSQLDateOnly()
	{
		return getSQLDateOnly( new Date() );
	}
	public static String getSQLDateOnly( Date inDate )
	{
		Format formatter = new SimpleDateFormat("dd-MMM-yyyy");
		return formatter.format( inDate );
	}


	// Normal ones you'd expect are true
	// null and Sting are FALSE
	// long and bigint and other numbers are FALSE but may be handled
	public static boolean isDateType( Object inObject )
	{
		if( null==inObject || null==kDateTypes )
			return false;
		// Decided by looking at class string
		String classStr = inObject.getClass().getName();
		return kDateTypes.contains( classStr );
	}

	public static java.util.Date objectToDateOrNull( Object inObject,
		boolean inComplainAboutNulls,
		boolean inComplainAboutConversionErrors
		)
	{
		return objectToDateOrNull( inObject,
			inComplainAboutNulls, inComplainAboutConversionErrors, 0
			);
	}

	public static java.util.Date objectToDateOrNull( Object inObject,
		boolean inComplainAboutNulls,
		boolean inComplainAboutConversionErrors,
		long optEpochOffsetCorrectionInMS
		)
	{
		String kFName = "objectToDateOrNull";
		String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inObject ) {
			if( inComplainAboutNulls )
				errorMsg( kFName, "Null value passed in, returning null.");
			return null;
		}
		java.util.Date answer = null;
		try {
			answer = objectToDate( inObject, optEpochOffsetCorrectionInMS );
		}
		catch(Exception e) {
			if( inComplainAboutConversionErrors )
				errorMsg( kFName, "Error converting object: " + e);
			answer = null;
		}
		return answer;
	}


	// TODO: Could support floats and doubles as Lotus/Excel style dates
	// ^^^ Reminder that there was a bug in Lotus that is often maintained by
	// vendors for backwards compatibility
	// TODO: What about dates before 1970???
	public static java.util.Date objectToDate( Object inObject )
		throws Exception
	{
		return objectToDate( inObject, 0 );
	}

	// Java assumes Epoch is Jan 1st, 1970
	// Databases like MS Sql Server think it's Jan 1st, 1900
	public static java.util.Date objectToDate(
		Object inObject, long optEpochOffsetCorrectionInMS
		)
			throws Exception
	{
		String kFName = "objectToDate";
		String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inObject )
			throw new Exception( kExTag + "Null object passed in." );

		debugMsg( kFName, "optEpochOffsetCorrectionInMS=" + optEpochOffsetCorrectionInMS );

		java.util.Date answer = null;

		// Decided by looking at class string
		String lObjClass = inObject.getClass().getName();

		// Is is already a Date ?
		if( lObjClass.equals( "java.util.Date" ) ) {
			answer = (java.util.Date)inObject;
			debugMsg( kFName,
				"true date, millies=" + answer.getTime()
				);
		}
		// Or maybe a sql Date or Time, which is easy to convert
		else if( lObjClass.equals( "java.sql.Date" ) ) {
			answer = (java.util.Date)inObject;
			debugMsg( kFName,
				"sql date, millis = " + answer.getTime()
				);
		}
		else if( lObjClass.equals( "java.sql.Time" ) ) {
			answer = (java.util.Date)inObject;
			debugMsg( kFName,
				"sql time, millis = " + answer.getTime()
				);
		}
		else if( lObjClass.equals( "java.sql.Timestamp" ) ) {
			answer = (java.util.Date)inObject;
			debugMsg( kFName,
				"sql timestamp, millis = " + answer.getTime()
				);
		}
		else if( lObjClass.equals( "java.lang.Long" ) ) {
			long tmpLong = ((Long)inObject).longValue();
			if( tmpLong < 0 )
				throw new Exception( kExTag +
					"Unable to Long to date; can't be a negative number."
					+ " Object=\"" + inObject + "\""
					);
			tmpLong += optEpochOffsetCorrectionInMS;
			debugMsg( kFName, "for Long val = " + tmpLong );
			answer = new java.util.Date( tmpLong );
		}
		// Some of the math types
		else if( lObjClass.equals( "java.math.BigInteger" ) ) {
			java.math.BigInteger tmpObj = (java.math.BigInteger)inObject;
			long tmpLong =  tmpObj.longValue();
			if( tmpLong < 0 )
				throw new Exception( kExTag +
					"Unable to BigInteger to date; can't be a negative number."
					+ " Object=\"" + inObject + "\""
					);
			tmpLong += optEpochOffsetCorrectionInMS;
			debugMsg( kFName, "for BigInteger val = " + tmpLong );
			answer = new java.util.Date( tmpLong );
		}


		// Some of the math types
		else if( lObjClass.equals( "java.lang.Double" ) ) {
			java.lang.Double tmpObj = (java.lang.Double)inObject;
			double newVal = tmpObj.doubleValue() * (double)MS_PER_DAY;
			long tmpLong =  (long) newVal;
			debugMsg( kFName,
				"Uncorrected:"
				+ " double val = " + newVal
				+ " long val = " + tmpLong
				);

			/***
			if( tmpLong < 0 )
				throw new Exception( kExTag +
					"Unable to java.lang.Double to date; can't be a negative number."
					+ " Object=\"" + inObject + "\""
					);
			***/
			tmpLong += optEpochOffsetCorrectionInMS;
			debugMsg( kFName, "for Double val = " + tmpLong );
			answer = new java.util.Date( tmpLong );
		}



		// Else is it a String?
		else if( lObjClass.equals( "java.lang.String" ) )
		{
			String dateStr = (String) lObjClass;
			answer = stringToDate( dateStr );
			/***
			throw new Exception( kExTag +
				"Support for converting Strings to dates not implemented yet."
				+ " Object=\"" + inObject + "\""
				);
			***/
		}	// End else is it a String?
		else {
			throw new Exception( kExTag +
				"Don't know how to convert object of type \"" + lObjClass + "\" to a date, obj=\"" + inObject + "\"."
				);
		}
		return answer;
	}


	// Try to convert string to unix
	// ^^^ ideas: use java.sql.date and time constructors first
	// NIE reports are in 'MM/DD/YY HH:MI:SS am'
	// then have some other standard formats we understand
	// Also:
	// try {
	//	date = DateFormat.getDateInstance(DateFormat.DEFAULT).parse("Feb 16, 2002");
	// } catch (ParseException e) {
	//	DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
	//	Date date = (Date)formatter.parse("01/29/02");
	//	formatter = new SimpleDateFormat("dd-MMM-yy");
	//	date = (Date)formatter.parse("29-Jan-02");
	////		   Parse a date and time; see also
	////		   e317 Parsing the Time Using a Custom Format
	//	formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	//	date = (Date)formatter.parse("2002.01.29.08.36.33");
	//	formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
	//	date = (Date)formatter.parse("Tue, 29 Jan 2002 22:14:02 -0500");
	public static java.util.Date stringToDate( String inDateStr )
			// throws Exception
			throws java.text.ParseException
	{
		
		inDateStr = trimmedStringOrNull( inDateStr );
		if( null == inDateStr )
			throw new java.text.ParseException( "Can't parse null/empty date strings", -1 );
		SimpleDateFormat df = new SimpleDateFormat(DATETIME_PARSE_FORMATS[0], Locale.US);
		boolean parsed = false;
		for( int i=0; i<DATETIME_PARSE_FORMATS.length; i++ )
		{
			String patternStr = DATETIME_PARSE_FORMATS[ i ];
	        df.applyPattern( patternStr );
	        try {
	        	java.util.Date answer = df.parse( inDateStr );
	        	return answer;
	        	// System.out.println(df.parse(d));
	        	// parsed = true;
	        } catch(ParseException pe) {
	        	continue;
	        }
		}
		throw new java.text.ParseException( "Unable to parse date string '" + inDateStr + "'", 0 );
	}

	public static java.sql.Timestamp stringToSqlTimestamp( String inDateStr )
		// throws Exception
		throws java.text.ParseException
	{
		return dateToSqlTimestamp( stringToDate( inDateStr ) );
	}

	public static java.sql.Timestamp dateToSqlTimestamp( java.util.Date inDate )
	{
		// Use intermediate conversion to milliseconds since 1970 (a Java long)
		return new java.sql.Timestamp( inDate.getTime() );
	}

	private static void __More_Elaborate_String_Methods__() {}

	// Markup a string with another string based on a literal pattern
	// By default, we place the new string after the first match
	// By default we are NOT case sensitive
	// If you give a list of patterns, we'll use the first one that matches
	// *** NOTE ***
	// If NO SUBSTITUSIONS ARE MADE YOU GET BACK A true _NULL_ STRING.
	// This is so you can easily tell if a match was done, because you may care.
	// Todo:
	// Support regex
	// Support nth match
	// Support doing more than one match


	public static String markupStringWtihString(
		String inMainDoc,
		String inNewText,
		String inPattern
		)
	{
		final String kFName = "markupStringWtihString";

		// We must do a sanity check to avoid a null exception
		if( inPattern == null )
		{
			errorMsg( kFName,
				"Was passed in a null scalar value."
				+ " Returning NULL (because no substituions were done)."
				);
			return null;
			// return inMainDoc;
		}
		// Convert the pattern to a list
		List lPatterns = new Vector();
		lPatterns.add( inPattern );

		// Now call the main method, let it assign defaults
		return markupStringWtihString(
				inMainDoc, inNewText, lPatterns
				);
	}

	public static String markupStringWtihString(
		String inMainDoc,
		String inNewText,
		String inPattern,
		boolean inInsertAfter,
		boolean inReplaceMatchedText,
		boolean inIsCaseSensitive
		)
	{
		final String kFName = "markupStringWtihString";

		// We must do a sanity check to avoid a null exception
		if( inPattern == null )
		{
			errorMsg( kFName,
				"Was passed in a null scalar value."
				+ " Returning NULL (since no substitutions were done)."
				);
			return null;
			// return inMainDoc;
		}
		// Convert the pattern to a list
		List lPatterns = new Vector();
		lPatterns.add( inPattern );

		// Now call the main method
		return markupStringWtihString(
				inMainDoc, inNewText, lPatterns,
				inInsertAfter, inReplaceMatchedText, inIsCaseSensitive
				);
	}



	public static String markupStringWtihString(
		String inMainDoc,
		String inNewText,
		List inPatterns
		)
	{
		return markupStringWtihString(
			inMainDoc, inNewText, inPatterns,
			true, false, false
			);
	}

	public static String markupStringWtihString(
		String inMainDoc,
		String inNewText,
		List inPatterns,
		boolean inInsertAfter,
		boolean inReplaceMatchedText,
		boolean inIsCaseSensitive
		)
	{
		final String kFName = "markupStringWtihString";

		// For sanity checking it's nice to know WHICH item was null
		// so break these into 3 checks
		if( inMainDoc==null || inMainDoc.length()<1 )
		{
			String tmpMsg = inMainDoc==null ? "inMainDoc is NULL" :
				"inMainDoc is an empty string."
				;
			errorMsg( kFName,
				tmpMsg
				+ " Returning NULL (since no substitutions were done)."
				);
			return null;
		}
		if( inNewText==null || inNewText.length()<1 )
		{
			String tmpMsg = inNewText==null ? "inNewText is NULL" :
				"inNewText is an empty string."
				;
			errorMsg( kFName,
				tmpMsg
				+ " Returning NULL (since no substitutions were done)."
				);
			return null;
		}
		if( inPatterns==null || inPatterns.size()<1 )
		{
			String tmpMsg = inPatterns==null ? "inPatterns is NULL" :
				"inPatterns has zero elements."
				;
			errorMsg( kFName,
				tmpMsg
				+ " Returning NULL (since no substitutions were done)."
				);
			return null;
		}

		// Start the outbuffer as null
		StringBuffer outBuff = null;

		// Get a version of the doc that we will search
		String searchDoc = inMainDoc;
		if( ! inIsCaseSensitive )
			searchDoc = searchDoc.toLowerCase();

		// Now loop through each pattern until we do a markup
		for( Iterator it = inPatterns.iterator(); it.hasNext(); )
		{
			// Get the pattern
			String currPattern = (String)it.next();
			// Normalize if needed
			if( ! inIsCaseSensitive )
				currPattern = currPattern.toLowerCase();
			// sanity check
			if( currPattern == null || currPattern.length()<1 )
			{
				warningMsg( kFName,
					"Encountered null/empty pattern in list."
					+ " Ignoring this pattern; will continue checking any"
					+ " remaining patterns."
					);
				continue;
			}

			// now see if we have a match
			int patternAt = searchDoc.indexOf( currPattern );

			// Try next pattern if this one didn't match
			if( patternAt < 0 )
				continue;

			// Separate out portions of the source document
			// Note: lengths are 1 based whereas offsets are zero based,
			// and the SECOND argument to substring is actually off by 1
			// by design (see Java Doc).
			// These things tend to cancel each other out, so I have
			// ommitted the +1 -1 BS below.

			// The part before the matched pattern
			String partA = "";
			// Grab it (if pattern not at start of doc)
			if( patternAt > 0 )
				partA = inMainDoc.substring( 0, patternAt );

			// The matched pattern itself
			// We want to extract original, non-normalized form from
			// the original docuemnt
			int endPartAt = patternAt + currPattern.length();
			String partB = inMainDoc.substring( patternAt, endPartAt );

			// The final part of the document, AFTER the matched pattern
			String partC = "";
			// Grab it (if pattern not at end of document)
			// (Note: NOT <= length )
			if( endPartAt < inMainDoc.length() )
					partC = inMainDoc.substring( endPartAt );

			// Piece things together

			// Init the buffer
			outBuff = new StringBuffer();

			// Add in part A, the part before the pattern
			outBuff.append( partA );

			// If we're NOT including the new text AFTER the pattern
			// then we are including it BEFORE the pattern
			if( ! inInsertAfter )       // Insert BEFORE
				outBuff.append( inNewText );

			// Now, do we want to include the actual matched pattern
			// itself?  Well if we're NOT REPLACING it then we ARE
			// going to add it.
			if( ! inReplaceMatchedText )    // we're keeping it
				outBuff.append( partB );

			// If we're including the new text AFTER the pattern
			// then go ahead and add it
			if( inInsertAfter )       // Insert AFTER
				outBuff.append( inNewText );

			// And finally, add the remainder of the document
			outBuff.append( partC );

			// OK, we found a match and handled it, we're done
			break;

		}   // End of loop for each pattern to check


		// Return the results if we have any, or the original document
		if( outBuff != null )
			return new String( outBuff );
		// Or send back a null, indicating that no substitutions were done
		else
			return null;
		// Or send back the original
			// return inMainDoc;

	}

	// with warnings
	public static boolean decodeBooleanStringWithExceptions( String inData )
		throws Exception
	{
		final String kFName = "decodeBooleanStringWithExceptions";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Normalize and sanity check
		inData = trimmedLowerStringOrNull( inData );
		if( inData == null )
			throw new Exception( kExTag + "Null/empty String passed in." );

		// Todo: other ideas: good, pass, passed
		if( inData.equals("1") ||
			inData.equals("yes") || inData.equals("y") ||
			inData.equals("true") || inData.equals("t") ||
			inData.equals("affirmative") ||
			inData.equals("positive") || inData.equals("+")
			)
		{
			return true;
		}
		// Todo: other ideas: bad, fail, failed
		else if( inData.equals("0") ||
			inData.equals("no") || inData.equals("n") ||
			inData.equals("false") || inData.equals("f") ||
			inData.equals("negative") || inData.equals("-")
			)
		{
			return false;
		}
		else
		{
			throw new Exception( kExTag
				+ "Unable to convert string to a boolean value"
				+ ", string=\"" + inData + "\""
				);
		}
	}


	// No warnings for nulls
	public static boolean decodeBooleanString( String inData, boolean defaultValue )
	{
		final String kFName = "decodeBooleanString";

		inData = trimmedLowerStringOrNull( inData );
		if( inData == null )
			return defaultValue;

		boolean answer = defaultValue;
		try
		{
			answer = decodeBooleanStringWithExceptions( inData );
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Unknown string \"" + inData + "\""
				+ ", returning default value \"" + defaultValue + "\""
				);
			answer = defaultValue;
		}

		// All done
		return answer;
	}



	public static int strBuffIndexOf( StringBuffer inBuff, int inChar )
	{
		final String kFName = "strBuffIndexOf";
		if( null == inBuff )
		{
			errorMsg( kFName, "Null input buffer, returning -1." );
			return -1;
		}

		int outValue = -1;
		for( int i=0; i<inBuff.length(); i++ )
		{
			char lChar = inBuff.charAt( i );
			if( lChar == inChar )
			{
				outValue = i;
				break;
			}
		}
		return outValue;
	}
	public static int strBuffLastIndexOf( StringBuffer inBuff, int inChar )
	{
		final String kFName = "strBuffLastIndexOf";
		if( null == inBuff )
		{
			errorMsg( kFName, "Null input buffer, returning -1." );
			return -1;
		}

		int outValue = -1;
		for( int i=inBuff.length() - 1; i>=0; i-- )
		{
			char lChar = inBuff.charAt( i );
			if( lChar == inChar )
			{
				outValue = i;
				break;
			}
		}
		return outValue;
	}

	public static String markupStringWithVariables(
		String inSourceString,
		Hashtable inValues
		)
	{
		return markupStringWithVariables(
			inSourceString, inValues, false, false, true, true
			);
	}

	public static String markupStringWithVariables(
		String inSourceString,
		Hashtable inValues,
		boolean inCasen,
		boolean inReturnNullOnError,
		boolean inDisplayErrors,
		boolean inDisplayNotFoundErrors
		)
	{
		final String kFName = "markupStringWithVariables";
		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );

		// Should we drop the \ in the "\$" sequence?
		// If folks want to use a $ in a string and don't want
		// us to think it's a variable, they need to escape it
		// with a \.  But that \ is usually only for our benefit,
		// and they likely do NOT want it in their final string,
		// so we should probably remove it.  If they really wanted
		// \$ in the output (unlikely), they could start by sending
		// us "\\$"; the first \ would be passed through, and so would
		// the $, leaving \$ in the output.
		final boolean kShouldUnescapeEscapedDollarSigns = true;

		// Sanity check, though we do not trim
		if( null==inSourceString ) {
			errorMsg( kFName,
				"Null source string, nothing to do, returning null."
				);
			return null;
		}
		if( null==inValues ) {
			if( inDisplayErrors )
				errorMsg( kFName,
					"Null source hash, nothing to do."
					+ ( inReturnNullOnError ?
						" Returning null."
						: " Returning original string."
						)
					);
			else if(debug)
				debugMsg( kFName,
					"Null source hash, nothing to do."
					+ " Told not to complain."
					+ ( inReturnNullOnError ?
						" Returning null."
						: " Returning original string."
						)
					);
			if( inReturnNullOnError )
				return null;
			else
				return inSourceString;
		}

		// Sanity escape hatch if we're sure there's nothing to do
		if( inSourceString.indexOf( '$' ) < 0 )
			return inSourceString;

		// Prepare for the substitutions
		StringBuffer fromBuff = new StringBuffer( inSourceString );
		StringBuffer toBuff = new StringBuffer();
		while( fromBuff.length() > 0 )
		{
			// Look for a dollar sign
			int nextDollarAt = -1;
			while( true )
			{
				int tmpOffset = strBuffIndexOf( fromBuff, '$' );
				// If no dollar sign, we're done
				if( tmpOffset < 0 )
					break;

				if(debug)
					debugMsg( kFName, "Dollar at offset " + tmpOffset );
				if(trace)
					if( tmpOffset >= 0 )
						traceMsg( kFName, "See: "
							+ new String(fromBuff)
							);

				// If the dollar sign is not escaped, use it
				// It's NOT escaped if it's the first character,
				// and it's not preceded by a backslash
				if( tmpOffset == 0
					||
					! fromBuff.substring(tmpOffset-1,tmpOffset).equals("\\")
					)
				{
					nextDollarAt = tmpOffset;
					break;
				}
				// Else we did find a dollar sign, but it was
				// escaped, so it does not count, so copy over
				// the buffer through that character and move on
				if( ! kShouldUnescapeEscapedDollarSigns )
				{
					toBuff.append(
						fromBuff.substring( 0, tmpOffset+1 )
						);
				}
				// Else we SHOULD remove the escpaping backslash
				else
				{
					// Cut off the copy two characters earlier,
					// dropping both the \ and the $ "\$" sequence
					toBuff.append(
						fromBuff.substring( 0, tmpOffset-1 )
						);
					// Then just add a fresh dollar sign
					toBuff.append( '$' );
				}
				// We still delete the entire run of characters
				// no matter the setting of
				// kShouldUnescapeEscapedDollarSigns
				fromBuff.delete( 0, tmpOffset+1 );
			}

			// At this point, we either found a dollar sign
			// or we didn't
			// This is the FIRST MAIN LOOP EXIT POINT
			////////////////////////////////////////////////
			if( nextDollarAt < 0 )
				break;

			// Copy everthing from the left of the dollar sign over
			toBuff.append(
				fromBuff.substring( 0, nextDollarAt )
				);
			// and remove it from src, leaving the dollar sign
			fromBuff.delete( 0, nextDollarAt );

			// Sanity check
			if( fromBuff.length() < 2 )
			{
				if( inDisplayErrors )
					errorMsg( kFName,
						"Nothing after dollar sign."
						+ ( inReturnNullOnError ?
							" Returning null."
							: " Returning original string."
							)
						);
				else if(debug)
					debugMsg( kFName,
						"Nothing after dollar sign."
						+ " Told not to complain."
						+ ( inReturnNullOnError ?
							" Returning null."
							: " Returning original string."
							)
						);

				if( inReturnNullOnError )
					return null;
				else
					break;
			}

			if(trace)
				traceMsg( kFName, "buff now = \""
					+ new String(fromBuff) + "\""
					);

			// Some key offsets, which are INCLUSIVE
			// int varSequenceStartsAt = nextDollarAt;
			// ^^^ NO, we've chopped that part off!
			int varSequenceStartsAt = 0;
			int varSequenceEndsAt = -1;
			int varNameStartsAt = -1;
			int varNameEndsAt = -1;
			boolean usingBraces = false;
			// If using braces
			if( fromBuff.charAt(1) == '{' )
			{
				usingBraces = true;


				// Sanity check
				if( fromBuff.length() < 3 )
				{
					if( inDisplayErrors )
						errorMsg( kFName,
							"Nothing after opening brace."
							+ ( inReturnNullOnError ?
								" Returning null."
								: " Returning original string."
								)
							);
					else if(debug)
						debugMsg( kFName,
							"Nothing after opening brace."
							+ " Told not to complain."
							+ ( inReturnNullOnError ?
								" Returning null."
								: " Returning original string."
								)
							);
					if( inReturnNullOnError )
						return null;
					else
						break;
				}

				varNameStartsAt = 2;
				// This may wind up being < 0
				int tmpEnd = strBuffIndexOf( fromBuff, '}' );
				if( tmpEnd > 0 )
				{
					varNameEndsAt = tmpEnd - 1;
					varSequenceEndsAt = tmpEnd;
				}
				// Else We will do more sanity checking below

			}
			// Else NOT using braces
			else
			{
				final String kValidChars =
					  "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
					+ "abcdefghijklmnopqrstuvwxyz"
					+ "0123456789"
					+ "._"
					;

				// Keep looking at characters until we find
				// one that we don't like, or are out of buffer
				// Start one char after the dollar sign
				if(debug)
					debugMsg( kFName,
						"varNameStartsAt=" + varNameStartsAt
						+ " bufflen=" + fromBuff.length()
						);
				for( int i=1; i<fromBuff.length(); i++ )
				{
					char c = fromBuff.charAt( i );
					// if not a valid char, we're done
					if( kValidChars.indexOf( c ) < 0 )
					{
						if(trace)
							traceMsg( kFName,
							"non var char " + c + " found at " + i
							);
						break;
					}
					// Set left edge, only done once
					if( varNameStartsAt < 0 )
						varNameStartsAt = i;
					// keep advancing right edge
					varSequenceEndsAt = i;
					varNameEndsAt = i;
				}

			}	// End Else not using braces


			// Lots of sanity checking
			// We consider these pretty serious, not
			// recoverable
			if(
				   varNameStartsAt < 0
				|| varNameEndsAt < 0
				|| varNameEndsAt < varNameStartsAt
				|| varNameEndsAt > fromBuff.length()-1
				|| varSequenceStartsAt < 0
				|| varSequenceEndsAt < 0
				|| varSequenceEndsAt < varSequenceStartsAt
				|| varSequenceEndsAt > fromBuff.length()-1
				)
			{
				if( inDisplayErrors || debug )
				{
					String msg =
						"Malformed variable name/expression."
						+ ( ! inDisplayErrors ?
							" Told not to complain."
							: ""
							)
						+ NL
						+ " Zero-based INCLUSIVE markers were: " + NL
						+ " varSequenceStartsAt=" + varSequenceStartsAt + NL
						+ " varSequenceEndsAt=" + varSequenceEndsAt + NL
						+ " varNameStartsAt=" + varNameStartsAt + NL
						+ " varNameEndsAt=" + varNameEndsAt + NL
						+ " usingBraces=" + usingBraces + NL
						+ " fromBuff.length()=" + fromBuff.length() + NL
						+ "(ONE-based)"
						+ ( inReturnNullOnError ?
							" Returning null."
							: " Returning original string."
							) + NL
						;
					if( inDisplayErrors )
						errorMsg( kFName, msg );
					else
						debugMsg( kFName, msg );
				}

				if( inReturnNullOnError )
					return null;
				else
					break;	// final logic will reassemble string

			}



			// varSequenceStartsAt
			// varSequenceEndsAt
			// varNameStartsAt
			// varNameEndsAt

			// Now grab and decode the variable name
			String varName = fromBuff.substring(
				varNameStartsAt, varNameEndsAt+1
				);
			if(debug)
				debugMsg( kFName, "varName=\"" + varName + "\"" );
			// No sanity checking, the resolver method will do that

			// And decode that varaible
			String lValue = resolveNestedHashReference(
				inValues, varName, inCasen, inDisplayErrors
				);

			// If not resolved
			if( null == lValue )
			{
				if( inDisplayNotFoundErrors )
					errorMsg( kFName,
						"Unable to resolve \"" + varName + "\"."
						+ ( inReturnNullOnError ?
							" Returning null."
							: " Removing variable from string."
							)
						);
				else if(debug)
					debugMsg( kFName,
						"Unable to resolve \"" + varName + "\"."
						+ " Told not to complain."
						+ ( inReturnNullOnError ?
							" Returning null."
							: " Removing variable from string."
							)
						);

				// Escape hatch
				if( inReturnNullOnError )
					return null;

				// Else we just don't copy anything over
				// Then continue loop, including the logic below
			}
			// Else it WAS resolved
			else
			{
				if( debug )
					debugMsg( kFName,
						"About to copy over variable's value of \""
						+ lValue + "\""
						);
				// Copy over the value!
				toBuff.append( lValue );
			}

			if(trace)
			{
				traceMsg( kFName,
					"About to remove variable sequence fron source buffer."
					+ " varSequenceStartsAt=" + varSequenceStartsAt
					+ " varSequenceEndsAt=" + varSequenceEndsAt
					+ " varNameStartsAt=" + varNameStartsAt
					+ " varNameEndsAt=" + varNameEndsAt
					);
				traceMsg( kFName,
					"*** BEFORE ***" + new String( fromBuff )
					);
			}


			// No matter what happened, we remove
			// the variable expression we just deal with
			fromBuff.delete( 0, varSequenceEndsAt+1 );

			if(trace)
				traceMsg( kFName,
					"*** AFTER ***" + new String( fromBuff )
					);


			// If we've emptied the buffer the for loop
			// will exit on it's own

		}	// End of Main Substition Loop


		// Wrap up any loose characters left in the source buffer
		if( fromBuff.length() > 0 )
			// toBuff.append( fromBuff );
			// ^^^ I believe this is unknown to earlier JVMs, Oracle JVM for example
			// force it to a string first
			toBuff.append( new String(fromBuff) );

		if(debug)
			debugMsg( kFName,
				"Done, new string = \"" + new String( toBuff ) + "\""
				);

		return new String( toBuff );
	}

	public static String resolveNestedHashReference(
		Hashtable inValues,
		String inVarName,
		boolean inCasen,
		boolean inDisplayErrors
		)
	{
		final String kFName = "resolveNestedHashReference";
		final char kDelimiter = '.';

		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );
		// boolean debug = true;
		// boolean trace = true;

		// Sanity checking
		if( null==inValues )
		{
			if( inDisplayErrors )
				errorMsg( kFName,
					"Null values hash, returning null."
					);
			else
				if(debug)
					debugMsg( kFName,
						"Null/empty variable reference, told not to complain, returning null."
						);
			return null;
		}
		inVarName = trimmedStringOrNull( inVarName );
		if( null==inVarName )
		{
			if( inDisplayErrors )
				errorMsg( kFName,
					"Null/empty variable reference, returning null."
					);
			else
				if(debug)
					debugMsg( kFName,
						"Null/empty variable reference, told not to complain, returning null."
						);

			return null;
		}
		if(debug)
			debugMsg( kFName,
				"Initial var name = \"" + inVarName + "\""
				+ " (remidner, trace info also available)"
				);
		if( ! inCasen )
		{
			inVarName = inVarName.toLowerCase();
			if(debug)
				debugMsg( kFName,
					"Normalized case to \"" + inVarName + "\""
					);
		}

		String currPath = inVarName;
		Hashtable currHash = inValues;
		String outValue = null;
		// Walk the path
		//////////////////////////////////////////////
		while( null != currPath )
		{
			if(trace)
				traceMsg( kFName,
					"Top of path walking loop"
					+ ", currPath=\"" + currPath + "\""
					);

			int delimAt = currPath.indexOf( kDelimiter );
			String currPart = null;
			String restOfPath = null;
			if( delimAt >= 0 )
			{
				if(trace)
					traceMsg( kFName,
						"Found path delim at " + delimAt
						);
				if( delimAt == 0 )
				{
					if( inDisplayErrors )
						errorMsg( kFName,
							"Bare delimiter in start of path \"" + inVarName + "\""
							+ " Rerturning null."
							);
					return null;
				}
				currPart = currPath.substring( 0, delimAt );
				currPart = trimmedStringOrNull( currPart );

				if( delimAt >= currPath.length()-1 )
				{
					if( inDisplayErrors )
						errorMsg( kFName,
							"Bare delimiter at end of path \"" + inVarName + "\""
							+ " Rerturning null."
							);
					return null;
				}
				currPath = currPath.substring( delimAt+1 );
				currPath = trimmedStringOrNull( currPath );
			}
			// Else no delimiter found
			else
			{
				if(trace)
					traceMsg( kFName, "No delim found." );
				currPart = currPath;
				currPath = null;
			}

			if( null == currPart )
			{
				if( inDisplayErrors )
					errorMsg( kFName,
						"Empty path bit in path \"" + inVarName + "\""
						+ " Rerturning null."
						);
				else
					debugMsg( kFName,
						"Empty path bit in path \"" + inVarName + "\""
						+ " Told not to complain."
						+ " Rerturning null."
						);
				return null;
			}

			if(trace)
				traceMsg( kFName, "currPart=\"" + currPart + "\"" );

			// now look it up
			if( currHash.containsKey( currPart ) )
			{
				Object objValue = currHash.get( currPart );
				Class objClass = objValue.getClass();
				// Is it a string?
				if( objClass == String.class )
				{
					// If the rest is null then we were
					// expecting it, so cool, we're done!
					if( null == currPath )
					{
						outValue = (String)objValue;
						break;
					}
					// Else we have more path???
					else
					{
						if( inDisplayErrors )
							errorMsg( kFName,
								"Found scalar while expecting hash."
								+ " Orig path = \"" + inVarName + "\""
								+ " This part = \"" + currPart + "\""
								+ " Unexpected remainder path = \"" + currPath + "\""
								+ " Rerturning null."
								);
						return null;
					}
				}
				// Else it is NOT a string
				// Is it a Hashtable?
				if( objClass == Hashtable.class )
				{
					// Were we expecting that?
					if( null != currPath )
					{
						// Good, set the hash to here and let
						// the loop continue
						currHash = (Hashtable)objValue;
					}
					// Else we have no more path???
					else
					{
						if( inDisplayErrors )
							errorMsg( kFName,
								"Found hash while expecting scalar."
								+ " Orig path = \"" + inVarName + "\""
								+ " This part = \"" + currPart + "\""
								+ " Rerturning null."
								);
						return null;
					}
				}
				// Else we don't know what to do with this object
				else
				{
					if( inDisplayErrors )
						errorMsg( kFName,
							"Can't traverse data structure."
							+ " Orig path = \"" + inVarName + "\""
							+ " This part = \"" + currPart + "\""
							+ " Unexpected object of = \"" + objClass.getName() + "\""
							+ " Rerturning null."
							);
					return null;


				}

			}
			// Else it did not have that key
			else
			{
				if(debug)
				{
					debugMsg( kFName,
						"Didn't find \"" + currPart + "\""
						+ ", returning null, current keys are "
						+ hashKeysToSingleString( currHash )
						);
				}
				// This is not a failure mode
				// If caller thinks it is, they can complain
				return null;
			}


		}	// Done walking path

		// We're done
		return outValue;

	}




	// check for <BASE HREF=URL> in document
	// Add one if there's not already one there
	// We return a null on missing data
	// We can also return a null on not-found patterns
	// IF we are not allowed to add it to the top
	// This is slightly DIFFERENT than the behaivor of the original
	// SnRequestHandler version of this method
	public static String markupHTMLWithBaseHref(
		String inSourceDoc,
		String inBaseURL,
		boolean inDoNoFoundErrors
		)
	{
		final String kFName = "markupHTMLWithBaseHref";

		// I think we want NON case sensitive searches
		final boolean isCasen = false;

		// For now, force the darn thing in at the top if we can't find
		// the "right" place for it
		final boolean okToForceAtTop = true;

		if( null == inSourceDoc )
		{
			errorMsg( kFName, "Null input document, returning null." );
			return null;
		}
		inBaseURL = trimmedStringOrNull( inBaseURL );
		if( null == inBaseURL )
		{
			errorMsg( kFName, "Null/empty base URL, returning null." );
			return null;
		}


		debugMsg( kFName,
			"Processing string with " + inSourceDoc.length() + " characters."
			);

		String searchDoc = inSourceDoc;
		if( ! isCasen )
		{
			searchDoc = searchDoc.toLowerCase();
			debugMsg( kFName,
				"Not case sensitive, so normalizing search doc to lower case."
				+ " (the original document will be left in tact, of course)"
				);
		}
		else
		{
			debugMsg( kFName,
				"IS case sensitive, so NOT normalizing search doc to lower case."
				);
		}

		// Step 1: Check if it's already there
		int baseAt0 = searchDoc.indexOf( "<base" );
		if( baseAt0 >= 0 )
		{
			debugMsg( kFName,
				"This document already has a <base ...> tag"
				+ ", at OFFSET = " + baseAt0 + "."
				+ " Returning original, unmodified document."
				);
			// They have a base tag, bail
			// Todo: could do additioanl context checking
			// Todo: could allow context flags
			return inSourceDoc;
		}

		// Step 2: Find a place to insert it

		// This is actually 2 steps itself:
		// Step 2a: Find the start of the tag
		// Step 2b: Find the closing angle bracket for that tag
		// Remember, sometimes folks have extra attributes in tags

		// Ideally, we'd put it right at the start of the header section
		// int insertBeforeOffset = searchDoc.indexOf( "</head" );
		int startOffset = searchDoc.indexOf( "<head" );
		int insertAfterOffset = -1;
		// But if there's no head, put it after opening html tag
		if( startOffset < 0 )
		{
			debugMsg( kFName,
				"Unable to find opening head tag, will look for opening body tag."
				);
			// insertBeforeOffset = searchDoc.indexOf( "<body" );
			startOffset = searchDoc.indexOf( "<html" );
		}

		// Find the closing angle bracket
		// We will catch the warnings later
		if( startOffset >= 0 )
		{
			insertAfterOffset = searchDoc.indexOf( '>', startOffset );
		}

		// Start preparing the answer
		StringBuffer outDoc = new StringBuffer();

		// If we still can't find it, don't do anything
		if( insertAfterOffset < 0 )
		{
			if( ! okToForceAtTop )
			{
				if( inDoNoFoundErrors )
					errorMsg( kFName, "Unable to find insertion point."
						+ " Returning original, unmodified document."
						+ " This may be OK; it just means that we didn't add"
						+ " a <base> tag to the HTML header section."
						+ " Perhaps there was no HTML or head tag at all."
						+ " This might impact relative images, links or Javascript."
						);
				return null;
			}
			// Else go ahead and jam it in at the top

			// Add the base in at the top
			outDoc.append( "<BASE HREF=\"" );
			outDoc.append(  inBaseURL );
			outDoc.append( "\">" );
			// And add the doc
			outDoc.append( inSourceDoc );

			// Don't flood them with messages, but let them know
			infoMsg( kFName,
				"Added <BASE> tag to very start of page"
				+ " because couldn't find proper HTML headers."
				);
		}
		else    // Else we DID find the proper place
		{
			debugMsg( kFName,
				"Will add base tag just after offset = " + insertAfterOffset + "."
				);

			// Add the first part of the document
			if( insertAfterOffset >= 0 )
				outDoc.append( inSourceDoc.substring( 0, insertAfterOffset+1) );

			// Now add in the base ref of the search engine
			outDoc.append( "<BASE HREF=\"" );
			outDoc.append(  inBaseURL );
			outDoc.append( "\">" );

			// Now add in the second part of the document
			if( insertAfterOffset < inSourceDoc.length()-1 )
				outDoc.append( inSourceDoc.substring( insertAfterOffset+1 ) );
		}

		// Now return the results
		return new String( outDoc );

	}


	public static List singleStringToListOfStrings( String inSourceString,
		String optDelimiters,
		boolean inDoTrimNull,
		boolean inDumpOuterQuotes,
		boolean inNormalizeToLowerCase,
		boolean inAllowDupes,
		boolean inIsDupeCheckCasen,
		boolean inDoWarnings
		)
	{
		final String kFName = "singleStringToListOfStrings";

		List retList = new Vector();
		HashSet checkList = new HashSet();

		if( null==inSourceString ) {
			if( inDoWarnings )
				errorMsg( kFName, "Null input, returning empty list." );
			return retList;
		}

		StringTokenizer st = null;
		if( null==optDelimiters )
			st = new StringTokenizer( inSourceString );
		else
			st = new StringTokenizer( inSourceString, optDelimiters );

		while( st.hasMoreTokens() )
		{
			// Grab the next string, and don't try to process nulls
			String item = st.nextToken();
			if( inDoTrimNull )
				item = trimmedStringOrNull( item );
			if( null==item )
				continue;
			// Check for quotes
			if( inDumpOuterQuotes && item.indexOf('"') >= 0 ) {
				if( item.startsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 1 );
					else
						item = null;
				if( null!=item && item.endsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 0, item.length()-1 );
					else
						item = null;
				// Retrim
				if( inDoTrimNull )
					item = trimmedStringOrNull( item );
			}
			// The "data" and "check value" may need to be normalized
			String checkItem = item;
			// Normalize item
			// Force the data to lower case, if asked to do so
			if( inNormalizeToLowerCase )
				item = item.toLowerCase();
			// If not case sensitive dupe checking, normalize to lower case
			if( ! inAllowDupes && ! inIsDupeCheckCasen )
				checkItem = checkItem.toLowerCase();
			// Time to start adding
			if( inAllowDupes )
				retList.add(item);
			else {
				if( ! checkList.contains(checkItem) ) {
					retList.add(item);
					checkList.add(checkItem);
				}
			}
		}
		return retList;
	}



	// This one allows for a single, MULTI-character string to be
	// used as a delimiter (vs the one above which treats each char
	// as a separate delim)
	public static List singleStringToListOfStringsBigDelim( String inSourceString,
		String inDelimitStr,
		boolean inDoTrimNull,
		boolean inKeepEmpties,
		boolean inDumpOuterQuotes,
		boolean inNormalizeToLowerCase,
		boolean inAllowDupes,
		boolean inIsDupeCheckCasen,
		boolean inDoWarnings
		)
	{
		final String kFName = "singleStringToListOfStringsBigDelim";

		boolean trace = shouldDoTraceMsg( kFName );

		List retList = new Vector();
		HashSet checkList = new HashSet();

		if( null==inSourceString ) {
			if( inDoWarnings )
				errorMsg( kFName, "Null input, returning empty list." );
			return retList;
		}
		if( null==inDelimitStr ) {
			if( inDoWarnings )
				errorMsg( kFName, "Null delimiter, returning empty list." );
			return retList;
		}

		// Start and End refer to offsets of the strings that we want
		// End is always +1 of actual end, like other Java string methods
		int lastStart = -1;
		int lastEnd = -1;
		boolean isDone = false;
		while( true )
		{
			int nextStart = -1;
			int nextEnd = -1;
			// int nextDelim = inSourceString.indexOf( inDelimitStr, lastEnd+1 );
			int nextSearch = lastEnd < 0 ? 0 : lastEnd + inDelimitStr.length();
			int nextDelim = inSourceString.indexOf( inDelimitStr, nextSearch );


			if( trace ) traceMsg( kFName, "Buffer=" + NL + inSourceString + NL );
			if(trace) traceMsg( kFName,
				"Top of loop:" + NL
				+ " lastEnd=" + lastEnd
				+ ", nextStart=" + nextStart
				+ ", nextEnd=" + nextEnd
				+ ", nextSearch was = " + nextSearch
				+ ", nextDelim=" + nextDelim
				);


			// We always start the next good string
			// after the end of the last delimiter, or from zero
			// if there was no prev
			if( lastEnd >= 0 )
				nextStart = lastEnd + inDelimitStr.length();
			else
				nextStart = 0;

			if(trace) traceMsg( kFName,
				"nextStart now = " + nextStart
				);


			// The ending is where-ever we found the next delim,
			// or the end of the string
			if( nextDelim < 0 ) {
				isDone = true;
				nextEnd = inSourceString.length();  // -1 + +1 = +0
			}
			else
				nextEnd = nextDelim;

			if(trace) traceMsg( kFName,
				"nextEnd now = " + nextEnd
				+ ", isDone=" + isDone
				);


			// We're good to grab the item
			String item = inSourceString.substring( nextStart, nextEnd );

			// Normalize
			if( inDoTrimNull )
				// item = trimmedStringOrNull( item );
				item = item.trim();
			// Skip empties if told to
			if( item.equals("") && ! inKeepEmpties ) {
				lastStart = nextStart;
				lastEnd = nextEnd;
				continue;
			}

			// Check for quotes
			if( inDumpOuterQuotes && item.indexOf('"') >= 0 ) {
				if( item.startsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 1 );
					else
						item = null;
				if( null!=item && item.endsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 0, item.length()-1 );
					else
						item = null;
				// Retrim
				if( inDoTrimNull )
					item = trimmedStringOrNull( item );
			}

			// The "data" and "check value" may need to be normalized
			String checkItem = item;

			// Normalize item
			// Force the data to lower case, if asked to do so
			if( inNormalizeToLowerCase )
				item = item.toLowerCase();

			// If not case sensitive dupe checking, normalize to lower case
			if( ! inAllowDupes && ! inIsDupeCheckCasen )
				checkItem = checkItem.toLowerCase();

			// Time to start adding
			if( inAllowDupes )
				retList.add(item);
			else {
				if( ! checkList.contains(checkItem) ) {
					retList.add(item);
					checkList.add(checkItem);
				}
			}

			// Now get ready for the next loop, if any
			if( isDone )
				break;

			lastStart = nextStart;
			lastEnd = nextEnd;

		}
		return retList;
	}



	public static Hashtable singleStringToHashOfWordCounts( String inSourceString,
		String optDelimiters,
		boolean inDoTrimNull,
		boolean inDumpOuterQuotes,
		// boolean inNormalizeToLowerCase,
		// boolean inAllowDupes,
		boolean inIsCasen,
		boolean inDoWarnings
		)
	{
		final String kFName = "singleStringToHashOfWordCounts";

		// List retList = new Vector();
		// HashSet checkList = new HashSet();
		Hashtable outHash = new Hashtable();

		if( null==inSourceString ) {
			if( inDoWarnings )
				errorMsg( kFName, "Null input, returning empty list." );
			return outHash;
		}

		StringTokenizer st = null;
		if( null==optDelimiters )
			st = new StringTokenizer( inSourceString );
		else
			st = new StringTokenizer( inSourceString, optDelimiters );

		while( st.hasMoreTokens() )
		{
			// Grab the next string, and don't try to process nulls
			String item = st.nextToken();
			if( inDoTrimNull )
				item = trimmedStringOrNull( item );
			if( null==item )
				continue;
			// Check for quotes
			if( inDumpOuterQuotes && item.indexOf('"') >= 0 ) {
				if( item.startsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 1 );
					else
						item = null;
				if( null!=item && item.endsWith("\"") )
					if( item.length() > 1 )
						item = item.substring( 0, item.length()-1 );
					else
						item = null;
				// Retrim
				if( inDoTrimNull )
					item = trimmedStringOrNull( item );
			}
			// The "data" and "check value" may need to be normalized
			String checkItem = item;
			// Normalize item
			// Force the data to lower case, if asked to do so
			if( ! inIsCasen )
				item = item.toLowerCase();
			// Time to start adding
			Integer currCount = new Integer( 0 );
			if( outHash.containsKey(item) )
				currCount = (Integer) outHash.get(item);
			Integer newCount = new Integer( currCount.intValue() + 1 );
			outHash.put( item, newCount );
		}
		return outHash;
	}


	public static String cleanupFancyHTML8BitChars( String inString ) {
		final String kFName = "cleanupFancyHTML8BitChars";
		if( null==inString ) {
			errorMsg( kFName, "Passed in null input string, returning null." );
			return null;
		}

		StringBuffer inBuff = new StringBuffer( inString );
		StringBuffer outBuff = new StringBuffer();
		for( int i=0; i<inBuff.length() ; i++ ) {
			char c = inBuff.charAt( i );

			// if( c > 127 )
			//	statusMsg( kFName, "8 bit char = '" + c + "' (" + (int)c + ")" );

			// Microsoft Trademark
			if( 153 == c )
				outBuff.append( "(TM)" );

			// NBSP, 0xA0
			else if( 160 == c )
				outBuff.append( ' ' );

			// Copyright
			else if( 169 == c )
				outBuff.append( "(c)" );

			// Soft hyphen
			else if( 173 == c )
				outBuff.append( '-' );

			// registered trademark
			else if( 174 == c )
				outBuff.append( "(R)" );

			// Degrees
			else if( 176 == c )
				outBuff.append( "(deg)" );

			// plus / minus
			else if( 177 == c )
				outBuff.append( "+/-" );

			// supersript 2
			else if( 178 == c )
				outBuff.append( "(2)" );
			// superscript 3
			else if( 179 == c )
				outBuff.append( "(3)" );
			// superscript 1
			else if( 185 == c )
				outBuff.append( "(1)" );

			// 1/4
			else if( 188 == c )
				outBuff.append( " 1/4" );
			// 1/2
			else if( 189 == c )
				outBuff.append( " 1/2" );
			// 3/4
			else if( 190 == c )
				outBuff.append( " 3/4" );

			else
				outBuff.append( c );

		}
		return new String( outBuff );

	}


	public static Set getEnglishStopWords() {
		return kStopWords;
	}

	public static String longStringChopper( String inText, int inLen, String optLineSep ) {
		final String kFName = "longStringChopper";
		if( null==inText ) {
			errorMsg( kFName, "Null input, returning null." );
			return null;
		}
		if( inLen < 1 ) {
			errorMsg( kFName, "Invalid length " + inLen + ", returning null." );
			return null;
		}
		optLineSep = (null!=optLineSep) ? optLineSep : NL;
		// Quick escape
		if( inText.length() <= inLen )
			return inText;

		StringBuffer outBuff = new StringBuffer();
		while( null!=inText && inText.length() > 0 ) {
			int chopAt = Math.min( inText.length(), inLen );
			String currPart = inText.substring( 0, chopAt );
			if( outBuff.length() > 0 )
				outBuff.append( optLineSep );
			outBuff.append( currPart );
			if( inText.length() > chopAt )
				inText = inText.substring( chopAt );
			else
				inText = null;
		}
		return new String( outBuff );
	}

	private static final void __sep__Lists_and_Hashes__() {}
	//////////////////////////////////////////////////////////////////


	public static List getTopNItems( Collection inItems, int inItemCount ) {
		final String kFName = "getTopNItems";
		if( null==inItems ) {
			errorMsg( kFName, "Null input list. Returning NULL." );
			return null;
		}
		if( inItemCount < 1 ) {
			errorMsg( kFName, "Invalid top count " + inItemCount + ", must be positive integer.  Returning NULL." );
			return null;
		}

		TreeSet sortedSet = new TreeSet();
		sortedSet.addAll( inItems );
		List keepKeys = new Vector();

		for( int i=1; i<=inItemCount ; i++ ) {
			if( ! sortedSet.isEmpty() ) {
				Object key = sortedSet.last();
				sortedSet.remove( key );
				keepKeys.add( key );
			}
			else
				break;
		}
		return keepKeys;
	}
	public static List getBottomNItems( Collection inItems, int inItemCount ) {
		final String kFName = "getBottomNItems";
		if( null==inItems ) {
			errorMsg( kFName, "Null input list. Returning NULL." );
			return null;
		}
		if( inItemCount < 1 ) {
			errorMsg( kFName, "Invalid top count " + inItemCount + ", must be positive integer.  Returning NULL." );
			return null;
		}

		TreeSet sortedSet = new TreeSet();
		sortedSet.addAll( inItems );
		List keepKeys = new Vector();
		for( int i=1; i<=inItemCount ; i++ ) {
			if( ! sortedSet.isEmpty() ) {
				Object key = sortedSet.first();
				sortedSet.remove( key );
				keepKeys.add( key );
			}
			else
				break;
		}
		return keepKeys;
	}


	// values become the keys
	// keys become the values
	// since a value can occur more than once, all VALUES in the NEW
	// hash are actual LISTs, which will contain one or more of the old keys
	public static Hashtable reverseHashKeysAndValues( Hashtable inHashtable ) {
		final String kFName = "reverseHashKeysAndValues";
		if( null==inHashtable ) {
			errorMsg( kFName, "Null input hash, returning null." );
			return null;
		}

		Hashtable outHash = new Hashtable();
		for( Iterator it = inHashtable.keySet().iterator() ; it.hasNext() ; ) {
			Object key = it.next();
			Object value = inHashtable.get( key );

			if( outHash.containsKey( value ) ) {
				List currList = (List) outHash.get( value );
				currList.add( key );
			}
			else {
				List newList = new Vector();
				newList.add( key );
				outHash.put( value, newList );
			}
		}

		return outHash;
	}

	// Sort a list or set
	public static List sortAsc( Collection inSet ) {
		final String kFName = "sortAsc";
		if( null==inSet ) {
			errorMsg( kFName, "Null set passed in, returning NULL." );
			return null;
		}
		TreeSet sortedSet = new TreeSet();
		sortedSet.addAll( inSet );
		List outKeys = new Vector();
		while( ! sortedSet.isEmpty() ) {
			Object key = sortedSet.first();
			outKeys.add( key );
			sortedSet.remove( key );
		}
		return outKeys;
	}
	public static List sortDesc( Collection inSet ) {
		final String kFName = "sortDesc";
		if( null==inSet ) {
			errorMsg( kFName, "Null set passed in, returning NULL." );
			return null;
		}
		TreeSet sortedSet = new TreeSet();
		sortedSet.addAll( inSet );
		List outKeys = new Vector();
		while( ! sortedSet.isEmpty() ) {
			Object key = sortedSet.last();
			outKeys.add( key );
			sortedSet.remove( key );
		}
		return outKeys;
	}

	public static List reverseList( List inList ) {
		final String kFName = "reverseList";
		if( null==inList ) {
			errorMsg( kFName, "Null list passed in, returning NULL." );
			return null;
		}
		List outList = new Vector();
		for( int i = inList.size()-1 ; i >= 0 ; i-- ) {
			Object item = inList.get( i );
			outList.add( item );
		}
		return outList;
	}

	// Shorter vectors are treated as "less than" longer vectors, assuming
	// the same initial elements in the longer vector
	public static List sortIntVectorsAsc( Collection inVectors )
	{
		final String kFName = "sortIntVectorsAsc";
		if( null==inVectors ) {
			errorMsg( kFName, "Null vectors passed in, nothing to do." );
			return null;
		}

		List sortedKeys = new Vector( inVectors );

		// Declare a list comparator
		Comparator comp = new Comparator() {
			public int compare(Object o1, Object o2) {
				if( null==o1 || null==o2 )
					return 0;
				List l1 = (List) o1;
				Iterator i1 = l1.iterator();
				List l2 = (List) o2;
				Iterator i2 = l2.iterator();
				while( i1.hasNext() || i2.hasNext() ) {
					Object obj1 = i1.hasNext() ? i1.next() : null;
					Object obj2 = i2.hasNext() ? i2.next() : null;
					if( null==obj1 )
						return -1;
					if( null==obj2 )
						return 1;
					Comparable c1 = (Comparable) obj1;
					Comparable c2 = (Comparable) obj2;
					int tmpResult = c1.compareTo( c2 );
					if( 0 != tmpResult )
						return tmpResult;
				}
				return 0;
			}
			public boolean equals(Object obj) { return false; }
		};

		// actually sort the list
		Collections.sort( sortedKeys, comp );

		return sortedKeys;
	}

	public static List sortIntVectorsDesc( Collection inVectors )
	{
		final String kFName = "sortIntVectorsDesc";
		if( null==inVectors ) {
			errorMsg( kFName, "Null vectors passed in, nothing to do." );
			return null;
		}

		List sortedKeys = new Vector( inVectors );

		// Declare a list comparator
		Comparator comp = new Comparator() {
			public int compare(Object o1, Object o2) {
				if( null==o1 || null==o2 )
					return 0;
				List l1 = (List) o1;
				Iterator i1 = l1.iterator();
				List l2 = (List) o2;
				Iterator i2 = l2.iterator();
				while( i1.hasNext() || i2.hasNext() ) {
					Object obj1 = i1.hasNext() ? i1.next() : null;
					Object obj2 = i2.hasNext() ? i2.next() : null;
					if( null==obj1 )
						return 1;
					if( null==obj2 )
						return -1;
					Comparable c1 = (Comparable) obj1;
					Comparable c2 = (Comparable) obj2;
					int tmpResult = c2.compareTo( c1 );
					if( 0 != tmpResult )
						return tmpResult;
				}
				return 0;
			}
			public boolean equals(Object obj) { return false; }
		};

		// actually sort the list
		Collections.sort( sortedKeys, comp );

		return sortedKeys;
	}

	public static int maxIntFromCollection( Collection inInts )
	{
		final String kFName = "maxIntFromCollection";
		int outMax = Integer.MIN_VALUE;
		if( null!=inInts && ! inInts.isEmpty() ) {
			for( Iterator it=inInts.iterator() ; it.hasNext() ; ) {
				Integer thisIntObj = (Integer) it.next();
				int thisInt = thisIntObj.intValue();
				if( thisInt > outMax )
					outMax = thisInt;
			}
		}
		else
			errorMsg( kFName, "Null/empty collection/set passed in, returning Integer.MIN_VALUE" );
		return outMax;
	}

	public static int minIntFromCollection( Collection inInts )
	{
		final String kFName = "minIntFromCollection";
		int outMin = Integer.MAX_VALUE;
		if( null!=inInts && ! inInts.isEmpty() ) {
			for( Iterator it=inInts.iterator() ; it.hasNext() ; ) {
				Integer thisIntObj = (Integer) it.next();
				int thisInt = thisIntObj.intValue();
				if( thisInt < outMin )
					outMin = thisInt;
			}
		}
		else
			errorMsg( kFName, "Null/empty collection/set passed in, returning Integer.MAX_VALUE" );
		return outMin;
	}

	// public static List sortHashKeysAsc( Hashtable inHash ) {
	public static List sortHashKeysAsc( Map inHash ) {
		final String kFName = "sortHashKeysAsc";
		if( null==inHash ) {
			errorMsg( kFName, "Null hashtable passed in, returning NULL." );
			return null;
		}
		return sortAsc( inHash.keySet() );
	}
	// public static List sortHashKeysDesc( Hashtable inHash ) {
	public static List sortHashKeysDesc( Map inHash ) {
		final String kFName = "sortHashKeysDesc";
		if( null==inHash ) {
			errorMsg( kFName, "Null hashtable passed in, returning NULL." );
			return null;
		}
		return sortDesc( inHash.keySet() );
	}



	public static String exceptionToString( Exception e )
	{
		final String kFName = "exceptionToString";

		if( null == e )
		{
			errorMsg( kFName,
				"Was passed a NULL exception, returning null."
				);
			return null;
		}

		// Get a stream to string buffer
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );

		// Shove the stack in there
		e.printStackTrace( pw );

		pw.close();

		// Return the string
		return sw.toString();

	}

	// Returns an array of collections:
	// [ Union, Intersection,  A-only, B-only ]
	// nulls are checked and treated as empty sets
	// Will always return a 4 element array with valid collections
	// Does NOT preserve order
	// Does NOT normalize
	public static Collection [] setsCompare( Collection a, Collection b )
	{
		final String kFName = "setsCompare";
		Collection union = new HashSet();
		Collection intersection = new HashSet();
		Collection aOnly = new HashSet();
		Collection bOnly = new HashSet();
		Collection [] outVals = new Collection [] {
			union, intersection, aOnly, bOnly
			};
		if( null != a ) {
			union.addAll( a );
			if( null != b ) {
				intersection.addAll( a );
			}
			aOnly.addAll( a );
			if( null != b ) {
				aOnly.removeAll( b );
			}
		}
		if( null != b ) {
			union.addAll( b );
			intersection.retainAll( b );
			bOnly.addAll( b );
			if( null != a ) {
				bOnly.removeAll( a );
			}
		}
		return outVals;
	}

	
	////////////////////////////////////////////////////////////////
	private static void ___sep__CSV_and_Tab_Delimited__() {}


	// TODO: need similar methods for Tab delimited
	// Given a line from a CSV file, return a work unit or null
	public static Vector parseTabDelimLine( String inputString )
	{
		final String kFName = "parseTabDelimLine";
	
		Vector returnRecord = new Vector();
		if( null==inputString ) {
			warningMsg( kFName, "Null string passed in, returning empty vector." );
			return returnRecord;
		}
	
		boolean trace = shouldDoTraceMsg( kFName );

		String [] parts = inputString.split( "\\t" );
		for( int i=0; i<parts.length; i++ )
			returnRecord.add( parts[i] );
		
		// Deprecated: StringTokenizer tempStringTokenizer = new StringTokenizer( inputString );
		// .hasMoreTokens()
		// .nextElement()
	
		return returnRecord;
	}

	// TODO: handle the nested newlines that Excel can put in mid field, see Perl code
	// Given a line from a CSV file, return a work unit or null
	public static Vector parseCSVLine( String inputString )
	{
		final String kFName = "parseCSVLine";
		// Todo: Obsess over edge cases, null fields, spaces
		// before an opening field quote, etc.

		Vector returnRecord = new Vector();

		if( null==inputString ) {
			warningMsg( kFName, "Null string passed in, returning empty vector." );
			return returnRecord;
		}

		boolean trace = shouldDoTraceMsg( kFName );

		StringBuffer buff1 = new StringBuffer( inputString );
		StringBuffer buff2 = null;
		boolean isInField = false;
		boolean isQuotedField = false;
		// boolean wasInQuotedField = false;
		boolean lastCharWasDelimComma = true;
		char c;
		int fieldCounter = 0;

		for( int i=0; i<buff1.length(); i++ )
		{
			c = buff1.charAt(i);

			// We need to detect trailing empty fields at the end of lines
			// Always start as false
			lastCharWasDelimComma = false;

			if(trace) traceMsg( kFName, "Char "+i+" is '"+c+"'");

			if( ! isInField )
			{
				// Check for null fields, keep them
				// if( c == ',' )
				if( c == ',' )
				{
					lastCharWasDelimComma = true;
					fieldCounter++;
					//returnRecord.addAnonymousField("");
					// registerAField( returnRecord, "", fieldCounter );
					registerACsvField( returnRecord, "" );
					// wasInQuotedField = false;
				}
				// Else not a null field, start recording
				else
				{
					lastCharWasDelimComma = false;
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
					if( isQuotedField ) {
						lastCharWasDelimComma = false;	// this does NOT count
						buff2.append(c);
					}
					// Not in quotes, so it's the end
					// of a field
					else
					{
						lastCharWasDelimComma = true;
						fieldCounter++;
						//returnRecord.addAnonymousField( new String(buff2) );
						// registerAField( returnRecord, new String(buff2),
						//	fieldCounter
						//	);
						registerACsvField( returnRecord, new String(buff2) );
						// wasInQuotedField = false;
						// Adjust states and pointers
						buff2 = null;
						isInField = false;
					}
				}
				// Is it a quote?
				else if( c == '"' ) {
					lastCharWasDelimComma = false;
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
							// registerAField( returnRecord, new String(buff2),
							//	fieldCounter
							//	);
							registerACsvField( returnRecord, new String(buff2) );
							// wasInQuotedField = true;
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
				else {
					lastCharWasDelimComma = false;
					// Keep it
					buff2.append(c);
				}

			} // End Else we're in a field

		} // End while loop

		// Double check that, when got to the end of the buffer
		// we may have been in the middle of a field
		if( isInField )
		{
			if( buff2 != null )
			{
				fieldCounter++;
				//returnRecord.addAnonymousField( new String(buff2) );
				// registerAField( returnRecord, new String(buff2), fieldCounter );
				registerACsvField( returnRecord, new String(buff2) );
			}
		}
		// Make sure to not drop empty fields at the end of lines
		else if( lastCharWasDelimComma ) {
			fieldCounter++;
			registerACsvField( returnRecord, "" );
		}

		return returnRecord;
	}

	// Helper function for CSV methods
	// Basically makes sure we don't stuff a null string into a Collection
	// And a null pointer warning check
	static void registerACsvField( Vector inList, String inValue )
	{
		final String kFName = "registerACsvField";
		if( null==inValue )
			inValue = "";
		if( null!=inList )
			inList.add( inValue );
		else
			errorMsg( kFName, "Null field list passed in, nothing to add value to." );
	}


	// Given a record, produce a CSV style string
	// representing it.
	// BTW, this csv record will, of course, not mention the
	// record name or specific field names
	public static String recordToCSVText( List inFieldValues )
	{
		final String kFName = "recordToCSVText";

		if( null==inFieldValues ) {
			errorMsg( kFName, "Null list of field values passed in, returning null." );
			return null;
		}
		if( inFieldValues.isEmpty() )
			warningMsg( kFName, "Empty (but not null) list of field values passed in, return buffer will also be empty (but not null)." );

		// We will build a return buffer as we go
		// to be converted to a normal string at the very end
		StringBuffer retBuff = new StringBuffer();

		boolean haveAddedField = false;
		Iterator it = inFieldValues.iterator();
		while( it.hasNext() )
		{
			String fieldValue = (String)it.next();
			if( fieldValue == null )
				fieldValue = "";
			boolean tmpReturn = addNewCSVFieldToBuffer( retBuff, fieldValue );
			haveAddedField = haveAddedField || tmpReturn;
		}

		// Add a line feed for good measure
		retBuff.append( NIEUtil.NL );
		// No, let java do this in write buffer

		// Return as a string
		return new String( retBuff );
	}

	static boolean addNewCSVFieldToBuffer(
		StringBuffer ioBuffer, String inData
		)
	{
		final String kFName = "addNewCSVFieldToBuffer";
		boolean outHaveAddedField = false;
		if( null==ioBuffer ) {
			errorMsg( kFName,
				"Null buffer passed in."
				);
			return outHaveAddedField;
		}

		// Nulls are OK
		if( null==inData )
			inData = "";

		// Check a few things
		boolean hasQuote = inData.indexOf('"') >=0 ? true : false;
		boolean hasComma = inData.indexOf(',') >=0 ? true : false;
		boolean hasNewline = inData.indexOf('\r') >=0
			|| inData.indexOf('\n') >=0 ? true : false;
		// Set a flag
		boolean needsQuoting = hasQuote || hasComma || hasNewline;

		// Add a trailing comma if we need it
		if( ioBuffer.length() > 0 )
			ioBuffer.append(',');

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









	private static void __Math__() {}

	// For inDecimals:
	// 2 = 2 decimal places
	// 0 = round to nearest int
	// -2 = nearest hundred
	public static double round( double inStartVal, int inDecimals ) {
		// get a power of 10, negative exponents will give decimals
		double scale = Math.pow( 10.0, (double)inDecimals );
		// slide and round to nearest int
		long round = Math.round( inStartVal * scale );
		// slide it back and return it
		return (double)round / scale;
	}
	public static double roundDown( double inStartVal, int inDecimals ) {
		// get a power of 10, negative exponents will give decimals
		double scale = Math.pow( 10.0, (double)inDecimals );
		// slide and round to nearest int
		// long round = Math.round( inStartVal * scale );
		long round = (long)( inStartVal * scale );
		// slide it back and return it
		return (double)round / scale;
	}

	private static void __Data_Conversion__() {}

	// Normal ones you'd expect are true
	// null and Sting are FALSE
	// dates are FALSE
	public static boolean isNumericType( Object inObject )
	{
		if( null==inObject || null==kNumericTypes )
			return false;
		// Decided by looking at class string
		String classStr = inObject.getClass().getName();
		return kNumericTypes.contains( classStr );
	}

	public static Double objectToDoubleOrNull( Object inObject,
		boolean inComplainAboutNulls,
		boolean inComplainAboutConversionErrors
		)
	{
		String kFName = "objectToDoubleOrNull";
		String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inObject ) {
			if( inComplainAboutNulls )
				errorMsg( kFName, "Null value passed in, returning null.");
			return null;
		}
		Double answer = null;
		try {
			answer = objectToDouble( inObject );
		}
		catch(Exception e) {
			if( inComplainAboutConversionErrors )
				errorMsg( kFName, "Error converting object: " + e);
			answer = null;
		}
		return answer;
	}

	// We don't handle dates or currency at this time
	// TODO: dates
	// TODO: currency
	// TODO: strings witn commas, etc.
	// TODO: Localization for string converting
	// TODO: other string items like words, roman numerals, etc.
	public static Double objectToDouble( Object inObject )
		throws Exception
	{
		String kFName = "objectToDouble";
		String kExTag = kClassName + '.' + kFName + ": ";

		if( null==inObject )
			throw new Exception( kExTag + "Null object passed in." );

		Double answer = null;

		// Decided by looking at class string
		String lObjClass = inObject.getClass().getName();

		// Is is already a Double ?
		if( lObjClass.equals( "java.lang.Double" ) ) {
			answer = (Double)inObject;
		}
		// Or maybe a float?
		else if( lObjClass.equals( "java.lang.Float" ) ) {
			Float fObj = (Float)inObject;
			answer = new Double( fObj.doubleValue() );
		}
		// Some of the math types
		else if( lObjClass.equals( "java.math.BigDecimal" ) ) {
			java.math.BigDecimal tmpObj = (java.math.BigDecimal)inObject;
			double tmpD = tmpObj.doubleValue();
			if( tmpD != Double.NEGATIVE_INFINITY && tmpD != Double.POSITIVE_INFINITY )
			answer = new Double( tmpD );
			else
				throw new Exception( kExTag + "Number too large to convert: " + tmpObj );
		}
		else if( lObjClass.equals( "java.math.BigInteger" ) ) {
			java.math.BigInteger tmpObj = (java.math.BigInteger)inObject;
			double tmpD = tmpObj.doubleValue();
			if( tmpD != Double.NEGATIVE_INFINITY && tmpD != Double.POSITIVE_INFINITY )
				answer = new Double( tmpD );
			else
				throw new Exception( kExTag + "Number too large to convert: " + tmpObj );
		}
		// Is is a genuine long?
		else if( lObjClass.equals( "java.lang.Long" ) ) {
			Long tmpLongObj = (Long)inObject;
			long longVal = tmpLongObj.longValue();
			answer = new Double( tmpLongObj.doubleValue() );
		}
		// Or Int
		else if( lObjClass.equals( "java.lang.Integer" ) ) {
			Integer tmpIntObj = (Integer)inObject;
			answer = new Double( tmpIntObj.doubleValue() );
		}
		// Is is a Boolean?
		// count as 0's and 1's
		else if( lObjClass.equals( "java.lang.Boolean" ) ) {
			Boolean tmpBoolObj = (Boolean)inObject;
			boolean boolVal = tmpBoolObj.booleanValue();
			answer = new Double( (boolVal ? 1.0 : 0.0) );
		}
		// Else is it a String?
		// try to convert it
		else if( lObjClass.equals( "java.lang.String" ) )
		{
			// Try as a double
			String tmpStr = (String)inObject;
			try {
				answer = new Double( tmpStr );
			}
			catch(NumberFormatException e) {
				// OK try as maybe hex or octal or something
				try {
					Long tmpLong = Long.decode(tmpStr);
					answer = new Double( tmpLong.doubleValue() );
				}
				catch(NumberFormatException e2) {
					throw new Exception( kExTag +
						"Unable to convert String to double."
						+ " String=\"" + tmpStr + "\"" + " Errors: " + e + ", " + e2
						);
				}
			}
		}	// End else is it a String?
		else {
			throw new Exception( kExTag +
				"Don't know how to convert object of type \"" + lObjClass + "\" to a number, obj=\"" + inObject + "\"."
				);
		}
		return answer;
	}


	private static void __File_Names_and_URLs__() {}


	/* Convert:
		d:\my\path\file.ext
		To:
		file:///d:/my/path/file.ext
	*/
	public static String convertWindoesPathToFileURI( String inURI )
	{
		final String kFName = "convertWindoesPathToFileURI";
		String newURI = trimmedStringOrNull( inURI );
		if( newURI == null )
		{
			errorMsg( kFName,
				"Null/empty path name passed in, returning null."
				);
			return null;
		}
		// Check if it already has a protocol prefix, in which case
		// we should not touch it.  But a drive letter will ALSO have
		// a colon.  Drive letters are only 1 character long, whereas
		// protocols are several, look for colon past where a drive letter's
		// would be
		if( newURI.indexOf( ':' ) > 1 )
		{
			debugMsg( kFName,
				"Appears to already be a URL."
				+ " A colon was found past where it would be for a drive letter."
				+ " Return it unmodified."
				);
			return inURI;
		}

		// Convert it to a file
		File theFile = new File( newURI );
		URL tmpURL = null;
		try
		{
			tmpURL = theFile.toURL();
		}
		catch(MalformedURLException e)
		{
			errorMsg( kFName,
				"Unable to convert to file URL."
				+ " Returning null."
				+ " Exception was: " + e
				);
			return null;
		}
		String outURI = tmpURL.toExternalForm();

		// Done for now
		return outURI;
	}


	// Is it relative?  Or Absolute?
	// Will return FALSE on null, and by default we do warn you
	// If you send in a null, both isRelative and isAbsolute will
	// return false, since any question about a null object should be false
	public static boolean isRelativeFilePath( String inPath )
	{
		return isRelativeFilePath( inPath, true );
	}
	public static boolean isRelativeFilePath(
		String inPath, boolean inDoWarnings
		)
	{
		final String kFName = "isRelativeFilePath";

		String tmpStr = NIEUtil.trimmedStringOrNull( inPath );
		if( tmpStr == null )
		{
			if( inDoWarnings && tmpStr == null )
			{
				errorMsg( kFName,
					"Was passed in a null or empty path to check."
					+ " Returning false."
					);
			}
			return false;
		}

		// Instantiate a File object so we can use it's methods
		File lTestFile = new File( tmpStr );
		// return the opposite of it's isAbsolute method
		return ! lTestFile.isAbsolute();
	}
	public static boolean isAbsoluteFilePath( String inPath )
	{
		return isAbsoluteFilePath( inPath, true );
	}
	public static boolean isAbsoluteFilePath( String inPath, boolean inDoWarnings )
	{
		final String kFName = "isAbsoluteFilePath";

		String tmpStr = NIEUtil.trimmedStringOrNull( inPath );
		if( tmpStr == null )
		{
			if( inDoWarnings && tmpStr == null )
			{
				errorMsg( kFName,
					"Was passed in a null or empty path to check."
					+ " Returning false."
					);
			}
			return false;
		}

		// workaround on Windows, isabs turns false for /bar
		// possibly because it's ambiguous, BUT assuming it's relative
		// gives even stranger results, so force it to true
		if( tmpStr.startsWith("/") || tmpStr.startsWith("\\") )
			return true;

		// Instantiate a File object so we can use it's methods
		File lTestFile = new File( tmpStr );
		// return the file's isAbsolute method
		return lTestFile.isAbsolute();
	}


	// If one is null then we don't do it, just return the other one
	// If both are null we return null
	// We DO WARN YOU by default
	public static String concatSystemPaths( String inStr1, String inStr2 )
	{
		return concatSystemPaths( inStr1, inStr2, true );
	}
	// We still use this for handing system resource paths
	// Should NOT be used for file paths
	public static String concatSystemPaths(
		String inStr1, String inStr2, boolean inDoWarnings
		)
	{
		final String kFName = "concatSystemPaths";

		String lStr1 = NIEUtil.trimmedStringOrNull( inStr1 );
		String lStr2 = NIEUtil.trimmedStringOrNull( inStr2 );

		boolean debug = shouldDoDebugMsg( kFName );

		// Normalize any backslashes to forward slashes
		if( null != lStr1 )
			lStr1 = replaceChars( lStr1, '\\', '/' );
		if( null != lStr2 )
			lStr2 = replaceChars( lStr2, '\\', '/' );
			
		if( inDoWarnings && ( lStr1 == null || lStr2 == null ) )
		{
			warningMsg( kFName,
				"At least one component was null or empty."
				+ ", inStr1=\"" + inStr1 + "\""
				+ ", inStr2=\"" + inStr2 + "\"."
				+ " Will return remaining none-empty segment, if present,"
				+ " with no concatenation symbol."
				+ " If these items can be null in regular operation"
				+ " then consider using the inDoWarnings=false method flag."
				);
		}

		// Don't add slashes if one was null
		if( lStr1 == null )
			return lStr2;
		if( lStr2 == null )
			return lStr1;

		if(debug)
			debugMsg( kFName,
				"lStr1=\"" + lStr1 + "\", lStr2=\"" + lStr2 + "\""
				);

		// check out the parent path
		// Is there a DOT ?
		int lastDotAt = lStr1.lastIndexOf( '.' );
		if( lastDotAt >= 0 )
		{

			if(debug) debugMsg( kFName, "Dot found at " + lastDotAt );

			// If the dot is in the last portion of the path,
			// that section will be dropped

			// Is there also a slash?
			int lastSlashAt = lStr1.lastIndexOf( '/' );
			// No slash, drop the whole thing
			if( lastSlashAt < 0 )
			{
				lStr1 = null;
				if(debug)
					debugMsg( kFName, "No slash, setting lStr1=null" );
			}
			// Else if dot in last section, drop it
			else if( lastDotAt > lastSlashAt )
			{
				if(debug)
					debugMsg( kFName,
						"Dot found after /."
						+ " lastDotAt=" + lastDotAt
						+ ", lastSlashAt=" + lastSlashAt
						);
				if( lastSlashAt > 0 )
				{
					lStr1 = lStr1.substring( lastSlashAt );
					lStr1 = trimmedStringOrNull( lStr1 );
					if(debug)
						debugMsg( kFName,
							"/ > 0."
							+ "lStr1 now = \"" + lStr1 + "\""
							);
				}
				else
				{
					lStr1 = null;
					if(debug)
						debugMsg( kFName,
							"/ <= 0. Setting lStr1 to null."
							);
				}
			}
			// Else dot is not in last section, so leave it alone
		}
		// Else no dot, nothing to drop, assume it's a dir

		String answer = null;
		if( lStr1 == null )
		{
			answer = lStr2;
			if(debug)
				debugMsg( kFName,
					"lStr1 is null, just returning lStr2."
					+ ", answer=\"" + answer + "\""
					);
		}
		else
		{
			if( lStr1.endsWith( "/" ) )
			{
				answer = lStr1 + lStr2;
				if(debug)
					debugMsg( kFName,
						"lStr1 ends with /, so just adding them together."
						+ " lStr1=\"" + lStr1 + "\""
						+ ", lStr2=\"" + lStr2 + "\""
						+ ", answer=\"" + answer + "\""
						);

			}
			else
			{
				answer = lStr1 + '/' + lStr2;		
				if(debug)
					debugMsg( kFName,
						"lStr1 does NOT end with /"
						+ ", so adding them together with /."
						+ " lStr1=\"" + lStr1 + "\""
						+ ", lStr2=\"" + lStr2 + "\""
						+ ", answer=\"" + answer + "\""
						);
			}
		}

		// Concat and return
		return answer;
		// Todo: find out preferred OS independent way to do this
	}


	// For best reults, should send in full URL or absolute/cannonical file name
	// This assumes that this path from from a readable individual resource
	public static String calculateDirOfURI( String inURI )
	{
		return calculateDirOfURI( inURI, true );
	}
	// A return of null means we either had a problem or found the
	// equivalent of "."
	public static String calculateDirOfURI( String inURI, boolean inDoWarnings )
	{
		final String kFName = "calculateDirOfURI";

		inURI = trimmedStringOrNull( inURI );
		if( inURI == null )
		{
			if( inDoWarnings )
				errorMsg( kFName, "Null/empty input path, returning null." );
			return null;
		}
		debugMsg( kFName, "Considering \"" + inURI + "\"" );

		String outURI = null;
		// If it's a URL
		// Todo: URL Edge cases
		// 1: URL has a ? in it
		//		if getQuery is not null
		//		also getFile is longer than getPath 
		// 2: URL has a # in it
		// 3: URL ends with not slash nor dotted file name
		if( isStringAURL( inURI, false ) )
		{
			debugMsg( kFName, "It's a URL." );

			// Drop any ? or # baggage
			int junkAt = inURI.indexOf('#');
			if( junkAt >= 0 )
			{
				if( 0 ==junkAt ) {
					if( inDoWarnings )
						warningMsg( kFName, "# at start of URL, forcing to '.'" );
					inURI = ".";
				}
				else {
					inURI = inURI.substring( 0, junkAt );
				}
			}
			junkAt = inURI.indexOf('?');
			if( junkAt >= 0 )
			{
				if( 0 ==junkAt ) {
					if( inDoWarnings )
						warningMsg( kFName, "? at start of URL, forcing to '.'" );
					inURI = ".";
				}
				else {
					inURI = inURI.substring( 0, junkAt );
				}
			}


			// Special case of .
			if( inURI.equals(".") ) {
				// return inURI + "/";
				return null;
			}
			// Special case:
			// If it ends in /, it was probably index.html or something so
			// was actually /something.xxx
			// so we return that as just the directory with the ending /
			else if( inURI.endsWith("/") )
			{
				String msg = "Special case: URL ends in \"/\"."
						+ " Returning original URL=\"" + inURI + "\""
						;
				// if( inDoWarnings )
				//	warningMsg( kFName, msg );
				// ^^^ that's not a problem...
				// else
					infoMsg( kFName, msg );
				return inURI;
			}
			// Else it did NOT end with /
			else
			{
				// We're going to use a conbination of Java's URL and File classes to help
				// us calculate this puppy
				try
				{
					URL tmpURL1 = new URL( inURI );
					String path1 = tmpURL1.getPath();
					File tmpFile = new File( path1 );
					String path2 = tmpFile.getParent();
					if( path2 == null )
						path2 = "/";
					// If we're on the PC, we can get backslashes
					if( path2.indexOf('\\') >= 0 )
						path2 = path2.replace( '\\', '/' );
					// We'd like it to end in a / so we make sure that everybody
					// understands that it's a directory
					if( ! path2.endsWith( "/" ) )
						path2 += "/";
					// Create a URL back, use the original as "context" but
					// give it the new path
					URL tmpURL2 = new URL( tmpURL1, path2 );
					outURI = tmpURL2.toExternalForm();
				}
				catch (Exception e)
				{
					
					String msg =
						"Unable to determin parent URL, returning null."
						+ " Original URL=\"" + inURI + "\""
						;
					if( inDoWarnings )
						errorMsg( kFName, msg );
					else
						infoMsg( kFName, msg );
					return null;
				}
				// for now, at least warn about suspect URL's
				// We do this after any possible error, errors take precedence
				/*** ^^^ done above now
				if( inURI.indexOf('?') >= 0 || inURI.indexOf('#') >= 0 )
				{
					String msg =
						"Results Suspect: URL contains ? or #, please verify results."
						+ "Original URL=\"" + inURI + "\""
						+ ", caculated parent=\"" + outURI + "\"."
						;
					if( inDoWarnings )
						warningMsg( kFName, msg );
					else
						infoMsg( kFName, msg );
				}
				***/
			}
		}
		else {
			debugMsg( kFName, "It's a File Name." );
			File tmpFile = new File( inURI );
			outURI = tmpFile.getParent();
			// Some tweaks for the root directory
			// the parent of root is root
			// BUT the parent of the file in the current directory
			// is just ".", which we would return as null
			if( null==outURI )
			{
				// We only tweak it if we started with root
				// we leave it alone if it's the current directory
				if( isAbsoluteFilePath( inURI, false ) )
				{
					outURI = "/";
					String msg =
						"Correcting null parent to /; parent of root is root"
						+ ", for original path \"" + inURI + "\"."
						;
					if( inDoWarnings )
						warningMsg( kFName, msg );
					else
						infoMsg( kFName, msg );
				}
				else
				{
					infoMsg( kFName,
						"Leaving null parent in tact, presumably this means \".\""
						+ ", for original path \"" + inURI + "\"."
						);
				}
			}
			// Some double checking about drive letters on windows
			// Java is a bit inconsistent about leaving or dropping them
			// Is it root?
			if( null!=outURI && (outURI.equals("/") || outURI.equals("\\")) )
			{
				// AND was there a drive letter before?
				if( inURI.indexOf(':') == 1 )
				{
					String oldURI = outURI;
					// OK, add the original drive letter back on (prepend)
					outURI = inURI.substring(0, 2) + outURI;
					String msg =
						"Adding back in Windows drive letter to parent."
						+ " Changed \"" + oldURI + "\" to \"" + outURI + "\""
						+ " for original path \"" + inURI + "\"."
						;
					if( inDoWarnings )
						warningMsg( kFName, msg );
					else
						infoMsg( kFName, msg );
				}
			}
			// Else it doesn't end in slash
			// In order that the directory itself doesn't get dropped, we need
			// to add on a place holder, we'll add a dot
			// NO
			// When you use the 2 element constructor for file it does not
			// just blindly do ge parent, don't worry
			// else	// Else does not end in a slash
			// {
			//	outURI += "/.";
			// }

			// To be consistent with the URL logic above, we terminate with a slash
			// so everbody knows we are a directory
			if( null!=outURI
				&& ! outURI.endsWith(File.separator)
				&& ! outURI.endsWith("/")
				&& ! outURI.endsWith("\\")
				)
			{
				outURI += File.separator;
			}

		}	// End Else it's not a URL, so assumed to be a File name

		// And we're done
		debugMsg( kFName, "Ending with \"" + outURI + "\"" );
		return outURI;
	}



	public static String concatURLAndEncodedCGIBuffer( String inURL,
		String inCGIFieldBuffer
		)
	{
		return concatURLAndEncodedCGIBuffer( inURL, inCGIFieldBuffer, true );
	}
	// This appears to think the buffer is ALREADY encoded
	public static String concatURLAndEncodedCGIBuffer( String inURL,
		String inCGIFieldBuffer, boolean inDoWarnings
		)
	{
		final String kFName = "concatURLAndEncodedCGIBuffer";

		String outURL = trimmedStringOrNull( inURL );
		if( outURL == null )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Input URL was null/empty, will return null."
					);
			return null;
		}
		String cgivars = trimmedStringOrNull( inCGIFieldBuffer );
		if( cgivars == null )
		{
			if( inDoWarnings )
				warningMsg( kFName,
					"No CGI variables to add, buffer was null/empty."
					+ " Will return original URL."
					+ " If it's normal for your app to have no variables at times"
					+ " then consider using the inDoWarnings=false method flag."
					);
			return outURL;
		}

		// If it doesn't have a question mark, then let's put one
		if( outURL.indexOf( '?' ) < 0 )
		{
			outURL = outURL + "?" + cgivars;
		}
		// Else must already have it
		// If it ends with it, just add us on the end
		// Perhaps the admin included the trailing ? when they set the
		// URL in the config file, so no warning mecessary
		else if( outURL.endsWith("?") )
		{
			outURL = outURL + cgivars;
		}
		// Else it has a ?, but not at the end, so add us with &
		// In this case, perhaps the URL already has some hard coded
		// CGI parameters tacked on, we should just add ourselves to the end
		// Case 1: hard coded and they added the trailing &
		else if( outURL.endsWith("&") )
		{
			outURL = outURL + cgivars;
		}
		// Case 2: hard coded cgi params, and they didn't add the trailing &
		else
		{
			outURL = outURL + "&" + cgivars;
		}

		return outURL;
	}


	// Given the name of a file, return it's extension, if any
	// Returns NULL if no extension
	// Simple versions set some default flags for behavior:
	// Set default: Yes, do trim whitespace from the extension
	// Set default: Yes, do normalize to lower case
	// Set default: valid extensions are at most 5 characters long
	public static String extractFileExtension( String inFileName )
	{
		return extractFileExtension( inFileName,
			true, true
		);
	}
	public static String extractFileExtension( String inFileName,
		boolean doTrim, boolean normalizeToLowerCase
		)
	{
		return extractFileExtension( inFileName,
			doTrim, normalizeToLowerCase, 5
			);
	}
	// The full version
	public static String extractFileExtension( String inFileName,
		boolean doTrim, boolean normalizeToLowerCase, int maxLength
		)
	{
		if( inFileName == null )
			return null;

		// Where is our period?
		int lastPeriodAt = inFileName.lastIndexOf( '.' );
		// If there's no period, then there's no extension
		if( lastPeriodAt < 0 )
			return null;
		// If the period is at the very end, then nothing left
		// to form an extension
		if( lastPeriodAt == inFileName.length()-1 )
			return null;

		// Find out where the last slash (of either type) is in the string
		int lastForwardSlashAt = inFileName.lastIndexOf( '/' );
		int lastBackSlashAt = inFileName.lastIndexOf( '\\' );
		int lastSlashAt = lastForwardSlashAt > lastBackSlashAt ?
			lastForwardSlashAt : lastBackSlashAt;

		// Sanity check, the last period should be after the last slash
		if( lastPeriodAt < lastSlashAt )
			return null;

		// OK, we have a period and stuff after it, so we have
		// a potential extension
		// Let's get it!

		String ext = inFileName.substring( lastPeriodAt+1 );

		// Some normalization
		if( doTrim )
			ext = ext.trim();
		if( normalizeToLowerCase )
			ext = ext.toLowerCase();

		// Sanity check, a zero length extension is forced to null
		if( ext.equals("") )
			return null;

		// Sanity check on length, IF we've been told to check
		if( maxLength > 0 && ext.length() > maxLength )
			return null;

		// OK, we've run the gauntlet, go ahead and return the answer
		return ext;

	}


	// Similar to the file version (actually windws up calling it)
	// but removes any CGI or #rel-ref stuff first
	public static String extractExtensionFromURL( String inFileName )
	{
		return extractExtensionFromURL( inFileName,
			true, true
		);
	}
	public static String extractExtensionFromURL( String inFileName,
		boolean doTrim, boolean normalizeToLowerCase
		)
	{
		return extractExtensionFromURL( inFileName,
			doTrim, normalizeToLowerCase, 5
			);
	}
	// The full version
	public static String extractExtensionFromURL( String inFileName,
		boolean doTrim, boolean normalizeToLowerCase, int maxLength
		)
	{

		if( inFileName == null )
			return null;

		// We need to figure out where, if any, the drop-able URL
		// suffix stuff starts at
		int qAt = inFileName.indexOf( '?' );
		int poundAt = inFileName.indexOf( '#' );
		int suffixCharAt;
		if( qAt >= 0 )
			if( poundAt >= 0 )
				suffixCharAt = poundAt > qAt ? qAt : poundAt;
			else
				suffixCharAt = qAt;
		else if( poundAt >= 0 )
			suffixCharAt = poundAt;
		else
			suffixCharAt = -1;

		String shortURL = null;

		if( suffixCharAt == 0 )
			return null;
		else if( suffixCharAt > 0 )
			shortURL = inFileName.substring( 0, suffixCharAt );
		else
			shortURL = inFileName;

		// Now call the regular file name version
		return extractFileExtension( shortURL,
			doTrim, normalizeToLowerCase, maxLength
			);
	}




	// Given a URL string, return directory portion of it.
	// If it's already a directory, just return it in tact.
	// Returns NULL if it has any problems.
	// The ambiguous flag controls how to handle url's like
	// http://foo.com/somename
	public static String extractDirFromURL( String inURL )
	{
		// Call with defaults:
		// Sets default: Ambiguous dirs will be treated as dirs
		// Sets default: We will not include the trailing slash
		return extractDirFromURL( inURL, true, false );
	}
	public static String extractDirFromURL( String inURL,
		boolean assertAmbiguousDirectory,
		boolean includeEndingSlash
		)
	{
		// bypass some of the nullish edge cases
		if( inURL == null )
			return null;
		inURL = inURL.trim();
		if( inURL.equals("") )
			return null;

		// Setup the return value in advance
		String outURL = inURL;

		// look for the opening double slash
		int doubleSlashAt = inURL.indexOf( "//" );
		int firstSingleSlashAt;
		if( doubleSlashAt >= 0 )
			firstSingleSlashAt = inURL.indexOf( '/', doubleSlashAt+2 );
		else
			firstSingleSlashAt = inURL.indexOf( '/' );

		// At this point we know where the first
		// single slash is
		// If there's NO first single slash then we know that:
		// 1: it's a server only url
		// 2: that does not have a slash
		// So in that case we just return it
		// the results

		// If there is no ending slash
		if( firstSingleSlashAt < 0 )
		{
			// Just return it
			if( ! includeEndingSlash )
				return outURL;
			else
				return outURL + "/";
		}

		// Now find the last slash in the URL
		int lastSingleSlashAt = inURL.lastIndexOf( '/' );
		// Sanity check that we did find one
		if( lastSingleSlashAt < 0 )
			if( ! includeEndingSlash )
				return outURL;
			else
				return outURL + "/";

		// If it's at the end, we're OK, just drop
		// the final slash and return it
		if( lastSingleSlashAt == inURL.length()-1 )
		{
			if( includeEndingSlash )
				outURL = outURL.substring( 0, lastSingleSlashAt+1 );
			else
				outURL = outURL.substring( 0, lastSingleSlashAt );
			return outURL;
		}

		// OK we have a path section of the URL
		// and it doesn't end in a slash
		// Lets look at it further

		// Grab the final portion of the path
		String finalPath = inURL.substring( lastSingleSlashAt+1 );

		// If it has a dot OR ? OR # in it we probably have
		// a file name or cgi call that should be dropped
		if( finalPath.indexOf('.') >= 0 ||
			finalPath.indexOf('?') >= 0 ||
			finalPath.indexOf('#') >= 0
			)
		{
			if( includeEndingSlash )
				outURL = outURL.substring( 0, lastSingleSlashAt+1 );
			else
				outURL = outURL.substring( 0, lastSingleSlashAt );
			return outURL;
		}

		// OK at this point we know there is a
		// final bit of path, and it doesn't seem
		// to be a a file name or CGI call so it
		// really looks like it might be a plain old DIR reference
		// with no slash

		// Let the caller decide
		if( assertAmbiguousDirectory )
		{
			if( includeEndingSlash )
				return outURL + "/";
			else
				// so just return it
				return outURL;
		}
		// Else we've been told to assume it's a file
		// so we should dump it and return the stem
		else
		{
			if( includeEndingSlash )
				outURL = outURL.substring( 0, lastSingleSlashAt+1 );
			else
				outURL = outURL.substring( 0, lastSingleSlashAt );
			return outURL;
		}
	}


	// Given a URL and a root, calculate a new URL
	public static String combineParentAndChildURLs(
			String inParent,
			String inChild
		)
		throws IOException
	{
		final String kFName = "combineParentAndChildURLs";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// check and normalize the child
		inChild = trimmedStringOrNull( inChild );
		if( null==inChild )
			throw new IOException( kExTag +
				"NULL/empty child URL passed in."
				);

		// check and normalize the parent
		inParent = trimmedStringOrNull( inParent );
		if( null==inParent )
		{
			errorMsg( kFName,
				"NULL or empty parent URL passed in."
				+ " Ignring empty parent and returning the child as the URL."
				);
			return inChild;
		}
		// inParent = _tweakBaseURL( inParent );
		// ^^^ NO!  Let java and web browsers decide
		// basically foo == foo.html, foo != foo/

		// init the results
		String retURLStr = inChild;

		// Now try to form the new URL
		try
		{
			URL parentURL = new URL( inParent );
			URL childURL = new URL( parentURL, inChild );
			retURLStr = childURL.toExternalForm();
		}
		catch( MalformedURLException mfue )
		{
			throw new IOException( "NIEUtil:combineParentAndChildURLs:"
				+ " Error combing child url '" + inChild + "'"
				+ " with base/parent/reference url '" + inParent + "'"
				+ " Exception was: " + mfue
				);
		}

		// Fix up the double slash problem
		retURLStr = cleanURLDoubleSlash( retURLStr );

		return retURLStr;
	}

	// compares 2 urls, tries to not be fooled by www. prefixes, slashes,
	// and index.html, default.xxx, etc.
	// currently does NOT compare CGI arguments; if that's all that is different
	// then the urls WOULD be similar
	// also currently does NOT do DNS checking or handle IP addresses, etc
	public static boolean calculateAreSimilarUrls( String inURL1, String inURL2 ) {
		final String kFName = "calculateAreSimilarUrls";
		inURL1 = trimmedLowerStringOrNull( inURL1 );
		inURL2 = trimmedLowerStringOrNull( inURL2 );
		// If both null / empty, then the same
		if( null==inURL1 && null==inURL2 )
			return true;
		// If only one is null / empty, but not both, then not the same
		if( null==inURL1 || null==inURL2 )
			return false;
		// Easy Escape case
		if( inURL1.equals(inURL2) )
			return true;

		// Get help from URL library
		try {
			URL url1 = new URL( inURL1 );
			URL url2 = new URL( inURL2 );

			// Check protocols
			String tmpProto1 = url1.getProtocol();
			String tmpProto2 = url2.getProtocol();
			// One is null and one isn't
			if( (null==tmpProto1 && null!=tmpProto2)
				|| (null!=tmpProto1 && null==tmpProto2)
			) {
				return false;
			}
			// both non-null but don't match
			if( null!=tmpProto1 && null!=tmpProto2
				&& ! tmpProto1.equalsIgnoreCase( tmpProto2 )		
			) {
				return false;
			}

			// Check ports
			int port1 = url1.getPort();
			int port2 = url2.getPort();
			// Allow for some defaults
			if( null!=tmpProto1 && tmpProto1.equalsIgnoreCase("http") ) {
				port1 = (port1>0) ? port1 : 80;
				port2 = (port2>0) ? port2 : 80;
			}
			else if( null!=tmpProto1 && tmpProto1.equalsIgnoreCase("https") ) {
				port1 = (port1>0) ? port1 : 443;
				port2 = (port2>0) ? port2 : 443;
			}
			else if( null!=tmpProto1 && tmpProto1.equalsIgnoreCase("ftp") ) {
				port1 = (port1>0) ? port1 : 21;
				port2 = (port2>0) ? port2 : 21;
			}
			// final check
			if( port1 != port2 )
				return false;

			// Check server names
			String serv1 = url1.getHost();
			String serv2 = url2.getHost();
			// One is null and one isn't
			if( (null==serv1 && null!=serv2)
				|| (null!=serv1 && null==serv2)
			) {
				return false;
			}
			// both non-null but don't match
			if( null!=serv1 && null!=serv2 && ! serv1.equals(serv2) ) {
				if( serv1.equals("www."+serv2)
					|| serv2.equals("www."+serv1)
				) {
					debugMsg( kFName, "Servers do match with www prefix." );
				}
				// Not OK
				else {
					// TODO: could also check IP addresses, DNS, etc.
					return false;
				}
			}

			// Check the paths
			// (this excludes ?CGI args and #achnor BS)
			// an empty path defaults to "/", and we will never have nulls
			String path1 = url1.getPath();
			path1 = trimmedStringOrNull( path1 );
			path1 = (null!=path1) ? path1 : "/";
			String path2 = url2.getPath();
			path2 = trimmedStringOrNull( path2 );
			path2 = (null!=path2) ? path2 : "/";

			// Drop any /index.* or /default.*
			// for path1
			int dropFrom = path1.lastIndexOf( "/index." );
			if( dropFrom < 0 )
				dropFrom = path1.lastIndexOf( "/default." );
			// If found and past the start
			if( dropFrom >= 0 )
				// Drop it (but do leave the /)
				path1 = path1.substring( 0, dropFrom+1 );
			// for path2
			dropFrom = path2.lastIndexOf( "/index." );
			if( dropFrom < 0 )
				dropFrom = path2.lastIndexOf( "/default." );
			// If found and past the start
			if( dropFrom >= 0 )
				// Drop it (but do leave the /)
				path2 = path2.substring( 0, dropFrom+1 );

			// Normalize to all end with /
			// not always great if you were going to use the URL
			// for something else, but we aren't
			if( ! path1.endsWith("/") )
				path1 += "/";
			if( ! path2.endsWith("/") )
				path2 += "/";

			// Now, if they are different, then we do not have a match
			if( ! path1.equals(path2) )
				return false;

			// TODO: could also check CGI args
		}
		catch( MalformedURLException e ) {
			warningMsg( kFName,
				"Exception: " + e
				+ " url1=" + inURL1
				+ " url2=" + inURL2
				);
			return false;
		}

		// We haven't prove they are different, so assume they are similar
		return true;
	}


	// Selectively add a trailing slash for URL's that seem
	// to be referencing a simple directory
	// Deprecated: We probably shouldn't be doing this - web browsers treat
	// a bare foo just like foo.html, and NOT like foo/, so we probably
	// should do that either
	// If you want a dir URL, make sure you end it with a /, and then trust
	// Java and web browsers to do the rest
	/*public*/ private static String _tweakBaseURL( String inURL )
	{
		final String kFName = "_tweakBaseURL";

		// bypass some of the nullish edge cases
		if( inURL == null || inURL.trim().equals("") )
		{
			errorMsg( kFName,
				"inURL was NULL or empty.  Returning null."
				);
			return null;
		}
		inURL = inURL.trim();

		// Setup the return value in advance
		String outURL = inURL;

		// look for the opening double slash
		int doubleSlashAt = inURL.indexOf( "//" );
		int firstSingleSlashAt;
		if( doubleSlashAt >= 0 )
			firstSingleSlashAt = inURL.indexOf( '/', doubleSlashAt+2 );
		else
			firstSingleSlashAt = inURL.indexOf( '/' );

		// At this point we know where the first
		// single slash is
		// If there's NO first single slash then we know that:
		// 1: it's a server only url
		// 2: that does not have a slash
		// So in that case we should add it and return
		// the results

		// If there is no ending slash
		if( firstSingleSlashAt < 0 )
		{
			// Add one and return
			outURL = inURL + '/';
			return outURL;
		}

		// Now find the last slash in the URL
		int lastSingleSlashAt = inURL.lastIndexOf( '/' );
		// Sanity check that we did find one
		if( lastSingleSlashAt < 0 )
			return outURL;

		// If it's at the end, we're OK, just return
		// that URL
		if( lastSingleSlashAt == inURL.length()-1 )
			return inURL;

		// OK we have a path section of the URL
		// and it doesn't end in a slash
		// Lets look at it further

		// Grab the final portion of the path
		String finalPath = inURL.substring( lastSingleSlashAt+1 );

		// If it has a dot OR ? OR # in it we probably don't
		// want to add anything to it
		if( finalPath.indexOf('.') >= 0 ||
			finalPath.indexOf('?') >= 0 ||
			finalPath.indexOf('#') >= 0
			)
		{
			// Just return what we got in
			return inURL;
		}

		// OK at this point we know there is a
		// final bit of path, and it doesn't seem
		// to be a a file name or CGI call so it
		// really looks like a plain old DIR reference
		// so 99 out of 100 times we SHOULD add a slash
		outURL = inURL + '/';
		return outURL;
	}

	// Sometimes when combing URL's we get one with a double slash in it
	// Clean that case up.
	// Todo: may be associated with tweak base URL rules
	public static String cleanURLDoubleSlash( String inURL )
	{
		if( inURL == null )
			return inURL;

		int baseSlashesAt = inURL.indexOf( "//" );
		if( baseSlashesAt < 0 )
			return inURL;

		int prefixLen = baseSlashesAt + "//".length() + 1;
		if( prefixLen == inURL.length() )
			return inURL;
		// Get the http:// portion
		// A little weird math
		// zero based offset = desired length - 1
		// sustring offset = final offset + 1
		// so they cancel
		String prefix = inURL.substring( 0, prefixLen );

		// Get the rest, this is what we'll be working on
		// More strange math
		// Start at prefix length + 1
		// but offset = start length - 1
		// so they cancel
		String suffix = inURL.substring( prefixLen );

		// Short circuit the rest of this mess if we know there's
		// nothing to be done
		if( suffix.indexOf('/') != 0 && suffix.indexOf( "//" ) < 0 )
			return inURL;
		// Else yes there is something for us to do
		//else
		//	System.err.p rintln( "URLBuilder:cleanDoubleSlash: Start.  url='" +
		//		inURL + "'"
		//		);

		// Trim off leading /'s, of which there shouldn't be any
		while( suffix.indexOf('/') == 0 )
		{
			if( suffix.length() > 1 )
				suffix = suffix.substring(1);
			else
				suffix = "";
		}

		while( true )
		{
			int doubleSlashAt = suffix.indexOf( "//" );
			if( doubleSlashAt < 0 )
				break;
			int suffixEndingStartsAt = doubleSlashAt + "//".length();
			if( suffixEndingStartsAt < suffix.length()-1 )
			{
				suffix = suffix.substring( 0, doubleSlashAt ) + '/' +
					suffix.substring( suffixEndingStartsAt );
			}
			else
			{
				suffix = suffix.substring( 0, doubleSlashAt ) + '/' ;
			}
		}

		String outURL = prefix + suffix;

		//System.err.p rintln( "URLBuilder:cleanDoubleSlash: End.  url='" +
		//	outURL + "'"
		//	);

		return outURL;
	}


	// Is this a URL or not?
	// We currently only support http:// prefixes
	public static boolean isStringAURL( String inCandidateString )
	{
		return isStringAURL( inCandidateString, true );
	}
	// Is this a URL or not?
	// We currently only support http:// prefixes
	public static boolean isStringAURL( String inCandidateString, boolean inDoWarnings )
	{
		final String kFName = "isStringAURL";

		inCandidateString = trimmedStringOrNull( inCandidateString );
		
		if( inCandidateString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Was passed in a null value, returning false."
					);
			return false;
		}

		String tmpStr = inCandidateString.toLowerCase();

		// Is it a URL?
		if( tmpStr.startsWith("http://")
			|| tmpStr.startsWith("https://")
			|| tmpStr.startsWith("ftp://")
			)
		{
			return true;
		}

		// Try making a URL out of it
		try
		{
			URL tmpURL = new URL( inCandidateString );
		}
		catch( Exception e )
		{
			// No, it isn't
			return false;
		}
		// else yes it is!
		return true;

	}

	// Is this a Windows path?
	// we can't determin Unix paths for sure, because they look like
	// relative URL's as well
	public static boolean isStringAWindowsFile( String inCandidateString )
	{
		return isStringAWindowsFile( inCandidateString, true );
	}
	// Is this a URL or not?
	// We currently only support http:// prefixes
	public static boolean isStringAWindowsFile( String inCandidateString, boolean inDoWarnings )
	{
		final String kFName = "isStringAWindowsFile";

		inCandidateString = trimmedStringOrNull( inCandidateString );
		
		if( inCandidateString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Was passed in a null value, returning false."
					);
			return false;
		}

		// If there's any backslashes, we say YES!
		if( inCandidateString.indexOf( '\\' ) >= 0 )
			return true;

		// If it's less than 2 chars chan it can't start with c:, so NO
		if( inCandidateString.length() < 2 )
			return false;

		// If we have a colon as the 2nd character
		if( inCandidateString.charAt( 1 ) == ':' )
		{
			int c = inCandidateString.charAt( 0 );
			// And a letter as the first character, than YES
			if( (c >= 'a' && c <= 'z') || (c >= 'A' && c<='Z') )
				return true;
		}

		// Otherwise, no conclusive proof, so assume it isn't
		return false;

	}


	// Is this a system URI / Resource / file name or not?
	public static boolean isStringASystemURI( String inCandidateString )
	{
		return isStringASystemURI( inCandidateString, true );
	}
	public static boolean isStringASystemURI( String inCandidateString, boolean inDoWarnings )
	{
		final String kFName = "isStringASystemURI";

		inCandidateString = trimmedStringOrNull( inCandidateString );
		
		if( inCandidateString == null )
		{
			if( inDoWarnings )
				errorMsg( kFName,
					"Was passed in a null value, returning false."
					);
			return false;
		}

		String tmpStr = inCandidateString.toLowerCase();

		// Is it starts with system:, it is, otherwise, no
		if( tmpStr.startsWith( AuxIOInfo.SYSTEM_RESOURCE_PREFIX ) )
			return true;
		else
			return false;

	}

	// NO, you should NOT try to open a URI by translating it from
	// a system:uri to a file:/// URI because that is NOT how the
	// class oriented streams work
	// Eventually you will want to use Class(instance).getResourceAsStream()
	// and that takes a CLASS, not a URI String
	// See openSystemResourceReadBin()
	public static String _NO_defunct_normalizeForSystemUri( String inUri, Class inClass )
	{
		if( isStringASystemURI(inUri) )
		{
			// xyz
			return null;
		}
		else {
			return inUri;
		}
	}


	private static final void __Emulating_Find_Command__() {}
	///////////////////////////////////////////////////////////////////////////////

	/***
	public static List findFiles( String optBase, String optFilePattern )
		throws IOException
	{
		return findFiles( new File(optBase), optFilePattern, false );
	}
	***/

	public static List findFiles( File optBase, String optFilePattern )
		throws IOException
	{
		return findFiles( optBase, optFilePattern, false );
	}

	/***
	public static List findFiles( String optBase, String optFilePattern, boolean inStartAtRootsOnNullBase )
		throws IOException
	{
		return findFiles( new File( optBase), optFilePattern, inStartAtRootsOnNullBase );
	}
	***/

	public static List findFiles( File optBase, String inFilePattern, boolean inStartAtRootsOnNullBase )
		throws IOException
	{
		List tmpFiles = new ArrayList();
		if( null!=optBase ) {
			tmpFiles.add( optBase );
		}
		else {
			if( inStartAtRootsOnNullBase ) {
				File [] roots = File.listRoots();
				for( int i=0; i<roots.length; i++ )
					tmpFiles.add( roots[i] );
			}
			else {
				File tmpFile = new File(".");
				tmpFiles.add( tmpFile );
			}
		}

		return findFiles( tmpFiles, inFilePattern );
	}

	// pattern is just an extension check right now
	public static List findFiles( Collection inBaseFiles, String optFilePattern )
		throws IOException
	{
		if( null==inBaseFiles )
			throw new IOException( "Null base directory list passed in." );
		if( inBaseFiles.isEmpty() )
			throw new IOException( "Empty directory list passed in." );
		List outList = new ArrayList();

		if( null!=optFilePattern ) {
			if( optFilePattern.length() > 0 ) {
				optFilePattern = optFilePattern.toLowerCase();
				if( optFilePattern.indexOf('.') < 0 )
					optFilePattern = "." + optFilePattern;
			}
		}

		for( Iterator it = inBaseFiles.iterator(); it.hasNext() ; ) {
			_traverseFile( (File) it.next(), outList, optFilePattern );
		}

		return outList;

	}

	private static void _traverseFile(
		File inFileOrDir, Collection ioQueue, String optFilePattern
		)
			throws IOException
	{
		// If it's a file
		if( inFileOrDir.isFile() ) {
			// System.out.println( "\t" + inFileOrDir );

			// If no pattern, then we want all of them
			if( null==optFilePattern )
				ioQueue.add( inFileOrDir );
			// Else check pattern
			else {
				String tmpName = inFileOrDir.getName().toLowerCase();
				// special pattern "" is like Windows "*."
				if( optFilePattern.length() < 1 && tmpName.indexOf('.') < 0 )
					ioQueue.add( inFileOrDir );
				// Else if it ends in this pattern, add it
				else if( optFilePattern.length() > 0 && tmpName.endsWith(optFilePattern) )
					ioQueue.add( inFileOrDir );
			}
		}
		// Else it's a directory
		else if( inFileOrDir.isDirectory() ) {
			// System.out.print( '.' );
			File [] entries = inFileOrDir.listFiles();
			// System.out.println( inFileOrDir + " with " + entries.length + " entries" );
			for( int i=0; i < entries.length ; i++ )
				_traverseFile( entries[i], ioQueue, optFilePattern );
		}
		// Else we don't know and I don't think we care
	}



		// File.listRoots()

	private static void __Actual_IO__() {}
	/////////////////////////////////////////////////////////

	private static void __IO__Returning_Strings__() {}

	public static String fetchURIContentsChar( String inBaseName )
			throws IOException
	{
		return fetchURIContentsChar( inBaseName, null, null, null, null, false );
	}
	public static String fetchURIContentsChar(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{
		return fetchURIContentsChar(
			inBaseName, optRelativeRoot,
			null, null, null, false
			);
	}
	public static String fetchURIContentsChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
		throws IOException
	{
		return fetchURIContentsChar( inBaseName, optRelativeRoot,
			optUsername, optPassword, null, false
			);
	}

	
	public static String fetchURIContentsChar_V1(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
		throws IOException
	{
	
		//final boolean debug = true;
		// final String kFName = "Debug: NIEUtil.fetchURIContentsChar";
		final String kFName = "fetchURIContentsChar_V1";
	
		debugMsg( kFName,
			"Start, about to open reader with openURIReadChar."
			);
	
		LineNumberReader lReader = openURIReadChar(
			inBaseName, optRelativeRoot,
			optUsername, optPassword, inoutAuxIOInfo, inUsePost
			);
	
		debugMsg( kFName,
			"Back from openURIReadChar."
			+ " Will start main loop.  Use trace mode to see details."
			);
	
		StringBuffer strBuff = new StringBuffer();
		String line = null;
		while(true)
		{
			traceMsg( kFName, "Top of while read-line loop." );
	
			line=((BufferedReader)lReader).readLine();
			if( line!=null )
			{
				traceMsg( kFName, "Got a line: \"" + line + "\"" );
				strBuff.append( line );
				// aparently we don't get newlines, so add one
				strBuff.append( '\n' );
			}
			else
			{
				traceMsg( kFName, "Got a NULL line, will break from loop." );
				break;
			}
		}
	
		lReader.close();
		lReader = null;
	
		debugMsg( kFName, "Done." );
	
		return new String( strBuff );
	}


	// New version adds auxio use of
	// getIsSuspiciousHttpResponse
	// getSuspiciousHttpReason

	public static String fetchURIContentsChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
		throws IOException
	{

		//final boolean debug = true;
		// final String kFName = "Debug: NIEUtil.fetchURIContentsChar";
		final String kFName = "fetchURIContentsChar";

		boolean trace = shouldDoTraceMsg( kFName );

		debugMsg( kFName,
			"Start, about to open reader with openURIReadChar."
			);

		LineNumberReader lReader = openURIReadChar(
			inBaseName, optRelativeRoot,
			optUsername, optPassword, inoutAuxIOInfo, inUsePost
			);

		debugMsg( kFName,
			"Back from openURIReadChar."
			+ " Will start main loop.  Use trace mode to see details."
			);

		StringBuffer strBuff = new StringBuffer();
		String line = null;
		while(true)
		{
			if(trace) traceMsg( kFName, "Top of while read-line loop." );

			line=((BufferedReader)lReader).readLine();
			if( line!=null )
			{
				if(trace) traceMsg( kFName, "Got a line: \"" + line + "\"" );
				strBuff.append( line );
				// aparently we don't get newlines, so add one
				strBuff.append( '\n' );

				// We check here for a very rare situation where http servers give back bogus results
				// and we don't want to do a blocking read
				if( null!=inoutAuxIOInfo && inoutAuxIOInfo.getSawSuspiciousHttpResponse() )
				{
					if(trace) traceMsg( kFName, "Non-null auxIO, WITH suspicious HTTP headers" );				
					if( line.toLowerCase().indexOf("</html>") >= 0 )
					{
						warningMsg( kFName,
							"Found closing HTML tag in suspicious HTTP response, so forcing end of read loop."
							+ " Suspect reason given: " + inoutAuxIOInfo.getSuspiciousHttpReason()
							);
						break;
					}
					else {
						if(trace) traceMsg( kFName, "Suspect line doesn't have closing HTML tag, so it's OK" );
					}
				}
				// More debugging
				else if( null!=inoutAuxIOInfo ) {
					if(trace) traceMsg( kFName, "Non-null auxIO, but not set as suspicious HTTP" );				
				}
				else {
					if(trace) traceMsg( kFName, "Null auxIO" );									
				}		
			}
			else
			{
				if(trace) traceMsg( kFName, "Got a NULL line, will break from loop." );
				break;
			}
		}

		lReader.close();
		lReader = null;

		debugMsg( kFName, "Done." );

		return new String( strBuff );
	}

	// Forces read from a file, vs. wondering if it might be a URL
	public static String fetchFileContentsChar(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{

		//final boolean debug = true;
		final String kFName = "fetchFileContentsChar";

		debugMsg( kFName,
			"Start, about to open reader with openURIReadChar."
			);

		LineNumberReader lReader = openFileReadChar(
			inBaseName, optRelativeRoot
			);

		debugMsg( kFName,
			"Back from openFileReadChar."
			+ " Will start main loop.  Use trace mode to see details."
			);

		StringBuffer strBuff = new StringBuffer();
		String line = null;
		while(true)
		{
			traceMsg( kFName, "Top of while read-line loop." );

			line=((BufferedReader)lReader).readLine();
			if( line!=null )
			{
				traceMsg( kFName, "Got a line: \"" + line + "\"" );
				strBuff.append( line );
				// aparently we don't get newlines, so add one
				strBuff.append( '\n' );
			}
			else
			{
				traceMsg( kFName, "Got a NULL line, will break from loop." );
				break;
			}
		}

		lReader.close();
		lReader = null;

		debugMsg( kFName, "Done." );

		return new String( strBuff );
	}

	private static void __IO__Returning_Lines__() {}

	public static Vector fetchURIContentsLines( String inBaseName )
			throws IOException
	{
		return fetchURIContentsLines( inBaseName, null, null, null, false, false, null, false );
	}

	public static Vector fetchURIContentsLines(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{
		return fetchURIContentsLines(
			inBaseName, optRelativeRoot,
			null, null, false, false, null, false
			);
	}

	public static Vector fetchURIContentsLines(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
		throws IOException
	{
		return fetchURIContentsLines( inBaseName, optRelativeRoot,
			optUsername, optPassword, false, false, null, false
			);
	}

	public static Vector fetchURIContentsLines(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		boolean inDoTrim,
		boolean inDoSkipBlankLines,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
		throws IOException
	{

		//final boolean debug = true;
		final String kFName = "Debug: NIEUtil.fetchURIContentsLines";

		debugMsg( kFName,
			"Start, about to open reader with openURIReadChar."
			);

		LineNumberReader lReader = openURIReadChar(
			inBaseName, optRelativeRoot,
			optUsername, optPassword, inoutAuxIOInfo,
			inUsePost
			);

		debugMsg( kFName,
			"Back from openURIReadChar."
			+ " Will start main loop.  Use trace mode to see details."
			);

		Vector answer = new Vector();
		String line = null;
		while(true)
		{
			traceMsg( kFName, "Top of while read-line loop." );

			line=((BufferedReader)lReader).readLine();
			if( line!=null )
			{
				traceMsg( kFName, "Got a line: \"" + line + "\"" );
				if( inDoTrim )
					line = line.trim();
				if( ! inDoSkipBlankLines || line.length()>0 )
					answer.add( line );
			}
			else
			{
				traceMsg( kFName, "Got a NULL line, will break from loop." );
				break;
			}
		}

		lReader.close();
		lReader = null;

		debugMsg( kFName, "Done." );

		return answer;
	}








	private static void __IO__Returning_Bytes__() {}

	public static byte [] fetchURIContentsBin( String inBaseName )
			throws IOException
	{
		return fetchURIContentsBin( inBaseName, null, null, null, null, false );
	}
	public static byte [] fetchURIContentsBin(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{
		return fetchURIContentsBin(
			inBaseName, optRelativeRoot,
			null, null, null, false
			);
	}
	public static byte [] fetchURIContentsBin(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
		throws IOException
	{
		return fetchURIContentsBin( inBaseName, optRelativeRoot,
			optUsername, optPassword, null, false
			);
	}
	public static byte [] fetchURIContentsBin(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
		throws IOException
	{

		//final boolean debug = true;
		final String kFName = "fetchURIContentsBin";

		debugMsg( kFName,
			"Start, about to open reader with openURIReadBin."
			);

		InputStream lInStream = openURIReadBin(
			inBaseName, optRelativeRoot,
			optUsername, optPassword, inoutAuxIOInfo, inUsePost
			);

		debugMsg( kFName,
			"Back from openURIReadBin."
			+ " Will start main loop.  Use trace mode to see details."
			);

		ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
		int c;
		while( (c = lInStream.read()) >= 0 )
		{
			outBytes.write( c );
		}

		lInStream.close();
		lInStream = null;

		debugMsg( kFName, "Done." );

		return outBytes.toByteArray();
	}



	// Open Sockets

	private static void __IO__Opening_Char_Sockets__() {}

	public static LineNumberReader openURIReadChar( String inBaseName )
			throws IOException
	{
		return openURIReadChar( inBaseName, null, null, null, null, false );
	}
	public static LineNumberReader openURIReadChar(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{
		return openURIReadChar( inBaseName, optRelativeRoot, null, null, null, false );
	}

	public static LineNumberReader openURIReadChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
		throws IOException
	{
		return openURIReadChar( inBaseName, optRelativeRoot,
			optUsername, optPassword, null, false
			);
	}

	public static LineNumberReader openURIReadChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
			throws IOException
	{

		// final boolean debug = true;
		final String kFName = "openURIReadChar";

		debugMsg( kFName,
			"Start.  About to call openURIReadBin."
			);

		// Try to get a stream
		// If it fails it should throw an exception
		InputStream lStream = openURIReadBin(
			inBaseName, optRelativeRoot,
			optUsername, optPassword,
			inoutAuxIOInfo, inUsePost
			);

		debugMsg( kFName,
			"Back from openURIReadBin, will now create LineNumberReader."
			);


		// Now convert it to a character reading gizmo

		String encoding = calculateEncoding( inBaseName, optRelativeRoot, inoutAuxIOInfo );
		
		// BufferedReader has readLine()
		// LineNumberReader has readLine AND getLineNumber
		LineNumberReader outReader = new LineNumberReader(
			// new InputStreamReader( lStream ), 2048
			// Java 1.4 and beyond accept an encoding
			new InputStreamReader( lStream, encoding ), 2048
			);

		// return it
		return outReader;

	}

	public static String calculateEncoding(
			String inBaseName,
			String optRelativeRoot,
			AuxIOInfo inoutAuxIOInfo
	) {
		if( null!=inoutAuxIOInfo )
			return inoutAuxIOInfo.calculateEncoding( inBaseName, optRelativeRoot );
		else
			return AuxIOInfo.staticCalculateEncoding( inBaseName, optRelativeRoot );
	}

	// Given a FILE name, open it for character reading
	// Don't freak out about the LineNumberReader,
	// you can take the result and assign it to a plain old
	// Reader interface variable and ignore the extra methods.
	// You can supply a base reference path if your file is
	// relative; if you supply a base, you need to tell us whether
	// you insist we use it, or leave it up to our best judgement.
	// YOU need to clean up your stream when you're done!
	// AND you should set the variable to NULL after you close it
	// so that the underlying stream will go away.
	// First signature just sets the options to null/false.
	// Implementation note:
	// This is just a wrapper around the binary versions of openFileReadBin
	// coded below.
	// Just like Reader, except with line numbers.
	public static LineNumberReader openFileReadChar( String inBaseName )
			throws IOException
	{
		return openFileReadChar( inBaseName, null, null );
	}
	public static LineNumberReader openFileReadChar(
		String inBaseName,
		String optRelativeRoot
		)
			throws IOException
	{
		return openFileReadChar( inBaseName, optRelativeRoot, null );
	}
	public static LineNumberReader openFileReadChar(
		String inBaseName,
		String optRelativeRoot,
		AuxIOInfo inoutAuxIOInfo
		)
			throws IOException
	{

		// Try to get a stream
		// If it fails it should throw an exception
		InputStream lStream = openFileReadBin(
			inBaseName, optRelativeRoot, inoutAuxIOInfo
			);

		// Now convert it to a character reading gizmo

		String encoding = calculateEncoding( inBaseName, optRelativeRoot, inoutAuxIOInfo );

		// BufferedReader has readLine()
		// LineNumberReader has readLine AND getLineNumber
		LineNumberReader outReader = new LineNumberReader(
			// new InputStreamReader( lStream ), 2048
			new InputStreamReader( lStream, encoding ), 2048
			);

		// return it
		return outReader;
	}



	private static void __IO__Opening_Binary_Sockets__() {}

	public static InputStream openURIReadBin( String inBaseName )
			throws IOException
	{
		return openURIReadBin( inBaseName, null, null, null, null, false );
	}
	public static InputStream openURIReadBin(
		String inBaseName,
		String optRelativeRoot
		)
		throws IOException
	{
		return openURIReadBin( inBaseName, optRelativeRoot, null, null, null, false );
	}
	public static InputStream openURIReadBin(
		String inBaseName, String optRelativeRoot,
		String optUsername, String optPassword
		)
			throws IOException
	{
		return openURIReadBin( inBaseName, optRelativeRoot,
			optUsername, optPassword, null, false
			);
	}
	public static InputStream openURIReadBin(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
			throws IOException
	{

		// final boolean debug = true;
		final String kFName = "openURIReadBin";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Sanity checking
		inBaseName = trimmedStringOrNull( inBaseName );
		if( inBaseName == null )
			throw new IOException( kExTag
				+ "Null/empty file name passed in, nothing to open!"
				);

		debugMsg( kFName,
			"Start.  URI=\"" + inBaseName + "\""
			+ ", optional relative root=\"" + optRelativeRoot + "\""
			);

		// We need to delegate to one of three sets of open routines
		//	1: system: resource URI's
		//	2: Internet style URL's
		//	3: file names
		// We check for these conditions IN THE ABOVE ORDER

		// First check for a system:uri
		// Two general conditions:
		// 1: we have a URI specifically labelled as system:
		// 2: the parent, if present, was a system URI AND there's
		//		nothing to indicate that this child is not
		// Apolgies for the convoluted if statement below, I've tried
		// to use indenting to make it more "clear"
		if
		(
			// Is it a system URI
			isStringASystemURI(inBaseName)
			||
			// Or it's parent was
			(
				// current URI is not an obvious URL or file name
				// which would disqualify this test
				(
					! isStringAURL( inBaseName )
					&& ! isStringAWindowsFile( inBaseName )
				)
				// and the parent was
				&&
				(
					// check the URL itself
					(
						optRelativeRoot != null
						&& isStringASystemURI( optRelativeRoot )
					)
					||
					// Or Aux IO has some record of it
					(
						inoutAuxIOInfo!=null
						&& inoutAuxIOInfo.getWasSystemResource()
					)
				)
			)
		)
		// ... IF a system: uri
		{
			debugMsg( kFName, "It's a system: URI" );
			// Mark the IO Aux for future use
			if( inoutAuxIOInfo != null )
				inoutAuxIOInfo.setWasSystemResource( true );
			// Call our system resource opener
			return openSystemResourceReadBin(
				inBaseName, optRelativeRoot,
				inoutAuxIOInfo
				);
		}
		// If we have ANY evidence of a URL, call that method
		else if( ( isStringAURL(inBaseName) )
			|| ( optRelativeRoot!=null && isStringAURL(optRelativeRoot) )
			|| ( inoutAuxIOInfo!=null && inoutAuxIOInfo.getWasURL() )
			)
		{
			debugMsg( kFName, "It's a URL" );
			if( isStringAWindowsFile( inBaseName ) )
				warningMsg( kFName,
					"Suspect looking URL, are you sure it's not a Windows file name?"
					+ " File=\"" + inBaseName + "\""
					+ ", optional parent=\"" + optRelativeRoot + "\"."
					+ " Web URL's should not reference file system paths."
					+ " If you insist, please use the file: URL syntax."
					);

			debugMsg( kFName,
				"Appears to be a URL, will call openURLReadBin."
				);

			if( inoutAuxIOInfo != null )
				inoutAuxIOInfo.setWasURL( true );

			return openURLReadBin( inBaseName, optRelativeRoot,
				optUsername, optPassword,
				inoutAuxIOInfo, inUsePost
				);
		}
		// Else no evidence of a URL, try the file related stuff
		else
		{
			debugMsg( kFName,
				"Appears to be a file, will call openFileReadBin."
				);
			if( inoutAuxIOInfo != null )
				inoutAuxIOInfo.setWasFile( true );

			// Call the file based routine
			// Notice that we don't currently pass in the user name
			// and password.
			if( optUsername!=null || optPassword!=null )
				warningMsg( kFName,
					"A username and/or password was passed in"
					+ " but the URI seems to be a file name."
					+ " Usernames/passwords not currently supported"
					+ " for file access, only for URL's."
					+ " Ignoring these extranous values and contineuing."
					+ " File='" + inBaseName + "'"
					+ " Username='" + optUsername + "'"
					+ " (password info suppressed in this warning message)"
					);
			return openFileReadBin( inBaseName, optRelativeRoot,
				inoutAuxIOInfo
				);
		}

	}


	// Given a FILE name, open it for binary reading
	// You can supply a base reference path if your file is
	// relative; if you supply a base, you need to tell us whether
	// you insist we use it, or leave it up to our best judgement.
	// YOU need to clean up your stream when you're done!
	// First signature just sets the options to null/false.
	public static InputStream openFileReadBin( String inBaseName )
			throws IOException
	{
		return openFileReadBin( inBaseName, null, null );
	}
	public static InputStream openFileReadBin(
			String inBaseName, String optRelativeRoot
			)
				throws IOException
	{
		return openFileReadBin( inBaseName, optRelativeRoot, null );
	}

	public static InputStream openFileReadBin(
		String inFileName,
		String optRelativeRoot,
		AuxIOInfo inoutAuxIOInfo
		)
			throws IOException
	{
		final String kFName = "openFileReadBin";
		final String kExTag = kClassName + "." + kFName + ": ";

		File lFile = null;
		try
		{
			lFile = findInputFile( inFileName, optRelativeRoot,
				inoutAuxIOInfo
				);
		}
		catch( FileNotFoundException e )
		{
			throw new IOException( kExTag
				+ "Can't access file.  Error: " + e
				);
		}

		return new FileInputStream( lFile );

	}

	// We assume you are sending in one or two valid system paths
	// Be careful about sending in non system paths
	public static InputStream openSystemResourceReadBin( String inFileName )
			throws IOException
	{
		return openSystemResourceReadBin( inFileName, null, null );
	}
	public static InputStream openSystemResourceReadBin(
		String inFileName,
		String optRelativeRoot
		)
			throws IOException
	{
		return openSystemResourceReadBin(
			inFileName, optRelativeRoot, null
			);
	}
	public static InputStream openSystemResourceReadBin(
		String inFileName,
		String optRelativeRoot,
		AuxIOInfo inoutAuxIOInfo
		)
			throws IOException
	{
		final String kFName = "openSystemResourceReadBin";
		final String kExTag = kClassName + "." + kFName + ": ";

		boolean debug = shouldDoDebugMsg( kFName );

		inFileName = trimmedStringOrNull( inFileName );
		if( inFileName == null )
			throw new IOException( kExTag
				+ "No input file/resource name given"
				);

		String origFile = inFileName;

		// We drop any system: prefix for now
		if( inFileName.toLowerCase().startsWith(
				AuxIOInfo.SYSTEM_RESOURCE_PREFIX
				)
			)
		{
			inFileName = inFileName.substring(
				AuxIOInfo.SYSTEM_RESOURCE_PREFIX.length()
				);
			inFileName = trimmedStringOrNull( inFileName );
			if( inFileName == null )
				throw new IOException( kExTag
					+ "\"system:\", by itself, is not a valid system resource/file name."
					);
		}

		// Normalize any system: root, and temp drop any system: prefix
		optRelativeRoot = trimmedStringOrNull( optRelativeRoot );
		if( optRelativeRoot != null
			&& optRelativeRoot.toLowerCase().startsWith(
				AuxIOInfo.SYSTEM_RESOURCE_PREFIX
				)
			)
		{
			optRelativeRoot = optRelativeRoot.substring(
				AuxIOInfo.SYSTEM_RESOURCE_PREFIX.length()
				);
			optRelativeRoot = trimmedStringOrNull( optRelativeRoot );
			if( optRelativeRoot == null )
				throw new IOException( kExTag
					+ "\"system:\", by itself, is not a valid system resource/file name root."
					);
		}
		// Else opt root is either null or is NOT a system: url
		// so FORCE IT to null
		else
		{
			// This is really important to know if you are debugging
			if( debug )
				if( null != optRelativeRoot )
					debugMsg( kFName,
						"DROPPING non-null relative root because"
						+ " it's not a 'system:' URI, but our current"
						+ " target IS; can't combine these two."
						+ " Current target = \"" + origFile + "\""
						+ ", dropped root = \"" + optRelativeRoot + "\"."
						);

			// zap it
			optRelativeRoot = null;
		}


		// Now we need the final path we will attempt to open
		String lPath = inFileName;
		// If there's still a parent, add it in
		if( optRelativeRoot != null )
		{
			if(debug)
				debugMsg( kFName,
					"lPath before concat = \"" + lPath + "\""
					+ ", relative root = \"" + optRelativeRoot + "\""
					);
			lPath = concatSystemPaths( optRelativeRoot, lPath );
			if(debug)
				debugMsg( kFName,
					"lPath now = \"" + lPath + "\""
					);
		}


		// Figure out which class this resource is relative to
		// String lClassName = kFullClassName;
		String lClassName = null;
		if( inoutAuxIOInfo != null )
		{
			lClassName =
				inoutAuxIOInfo.getSystemRelativeBaseClassName();
			// This may or may not be null, it's OK either way
		}
		// Get the class
		Class lClass = null;
		// Are we to just use the default class?
		if( null==lClassName )
		{
			lClass = NIEUtil.class;
		}
		// Or did they set a specific class
		else
		{
			try
			{
				lClass = Class.forName( lClassName );
			}
			catch( Exception e )
			{
				throw new IOException( kExTag
					+ "Aux IO set a non-default base java class"
					+ " to resolve system: URIs relative to."
					+ " This class could not be loaded."
					+ " Non-default class = \"" + lClassName + "\""
					+ " Error: " + e
					);
			}
		}

		// Use the class structure to open the resource / file stream
		InputStream lStream = lClass.getResourceAsStream( lPath );

		// Save the full name, if they passed us an Aux object
		if( inoutAuxIOInfo != null )
		{
			// Add the system: stuff back in
			inoutAuxIOInfo.setFinalURI(
				AuxIOInfo.SYSTEM_RESOURCE_PREFIX + lPath
				);
			inoutAuxIOInfo.setWasSystemResource( true );
		}

		// Sanity check
		// We do this after we update the aux object
		if( null == lStream )
			throw new IOException( kExTag
				+ "Unable to open system resoure " + lPath
				+ " relative to class " + lClass.getName()
				+ " (not found?)"
				);

		debugMsg( kFName,
			"opened system stream " + lPath
			+ " relative to class " + lClass.getName()
			+ ", stream object = " + lStream
			);
		// We're done
		return lStream;

	}


	public static InputStream openFileReadBinV0(
		String inBaseName,
		String optRelativeRoot,
		AuxIOInfo inoutAuxIOInfo
		)
			throws IOException
	{
		final String kFName = "openFileReadBin";

		if( inBaseName == null )
		{
			throw new IOException( "NIEUtil:openFileReadBin:"
				+ " Was passed a null input."
				);
		}

		// These a bit of logic to decide whether to use the root
		// or not
		boolean useRoot = false;
		// If we have a root and are specifically told to use it,
		// then use it!
		boolean tmpForceUseOfRoot = ( inoutAuxIOInfo!=null &&
					inoutAuxIOInfo.inForceUseOfRoot
				) ? true : false;

		if( tmpForceUseOfRoot && optRelativeRoot != null )
		{
			useRoot = true;
		}
		// Else if a root was passed in we normally only use it
		// if we have a relative path
		else if( optRelativeRoot != null )
		{
			// If the base file does not seem to be absolute
			// AND know that we do have a base, go ahead and use it
			File tmpFile = new File( inBaseName );
			if( ! tmpFile.isAbsolute() )
				useRoot = true;
		}
		// Else we were told to use a root but none was given,
		// warn them and continue.
		else if( tmpForceUseOfRoot )
		{
			warningMsg( kFName,
				"optForceUseOfRoot was set to TRUE"
				+ " but optRelativeRoot was NULL."
				+ " Ignoring this option and continuing."
				);
		}
		
		statusMsg( kFName, "Use root = " + useRoot );

		// If we're using the root, call the file constructor and
		// try to open a stream
		File lFile = null;
		FileInputStream lStream = null;
		if( useRoot )
		{
			// No try block, just toos it up stream
			lFile = new File(
				optRelativeRoot, inBaseName
				);
			lStream = new FileInputStream( lFile );
		}
		// Else not using root
		else
		{
			// We need to catch the first exception in order to retry
			try
			{
				// Try creating with the user.dir current directory
				lFile = new File(
					System.getProperty( "user.dir" ), inBaseName
					);
				lStream = new FileInputStream( lFile );
			}
			// If it didn't work, try by itself
			catch (Exception e)
			{
				lFile = new File( inBaseName );
				lStream = new FileInputStream( lFile );
			}

		}

		return lStream;
	}

	// This is like the file constructor stuff, but it
	// takes the current directory into account
	public static File findInputFile( String inFileName )
		throws FileNotFoundException
	{
		return findInputFile( inFileName, null );
	}




	public static File findInputFile( String inFileName,
		String optRelativeRoot
		)
		throws FileNotFoundException
	{
		return findInputFile( inFileName, optRelativeRoot, null );
	}


	// This is the full version that will also add some details
	// to the aux IO object, if present
	public static File findInputFile( String inFileName,
		String optRelativeRoot, AuxIOInfo ioAuxPathInfo
		)
		throws FileNotFoundException
	{
		final String kFName = "findInputFile";
		final String kExTag = kClassName + "." + kFName + ": ";

		// Sanity check
		inFileName = trimmedStringOrNull( inFileName );
		if( inFileName == null )
			throw new FileNotFoundException( kExTag
				+ "NULL/empty input file name."
				);
		optRelativeRoot = trimmedStringOrNull( optRelativeRoot );

		debugMsg( kFName, "Starting"
			+ ", inFile=\"" + inFileName + "\""
			+ ", optParent=\"" + optRelativeRoot + "\""
			+ ", ioAuxPathInfo=" + ioAuxPathInfo
			);


		File theFile = null;
		boolean securityFault = false;
		String msg = null;


		// If it's an absolute path, try it by itself
		// This is all we will try if it seems to be absolute
		if( isAbsoluteFilePath( inFileName ) )
		{
			debugMsg( kFName, "Trying absolute" );
			try
			{
				theFile = new File( inFileName );
				// InputStream tmpStream = new FileInputStream( theFile );
				// tmpStream.close();
				// Use simple exists, vs forcing it to open the file
				if( ! theFile.exists() )
					theFile = null;
			}
			catch (SecurityException e)
			{
				theFile = null;
				securityFault = true;
				msg = "Security error: " + e
					+ ", file = \"" + inFileName + "\""
					+ "\"";
			}	
		}
		// Else it is NOT absolute, so it is relative
		else
		{

			// If we have a parent for a relative basis, try using it
			if( optRelativeRoot != null )
			{
				debugMsg( kFName, "Trying relative to parent" );
				try
				{
					theFile = new File(
						optRelativeRoot,
						inFileName
						);
					if( ! theFile.exists() )
						theFile = null;
				}
				catch (SecurityException e)
				{
					theFile = null;
					securityFault = true;
					msg = "" + e;
					msg = "Security error: " + e
						+ ", file = \"" + inFileName + "\""
						+ ", relative to \"" + optRelativeRoot + "\""
						;
				}	
			}

			// If we didn't success, try it relative to the system property
			// If we failed for security reasons, do NOT try anything else
			if( theFile == null && ! securityFault )
			{
				debugMsg( kFName, "Trying relative to user.dir" );
				try
				{
					theFile = new File(
							System.getProperty( "user.dir" ),
							inFileName
						);
					// InputStream tmpStream = new FileInputStream( theFile );
					// tmpStream.close();
					// Use simple exists, vs forcing it to open the file
					if( ! theFile.exists() )
						theFile = null;
				}
				catch (SecurityException e)
				{
					theFile = null;
					securityFault = true;
					msg = "Security error: " + e
						+ ", file = \"" + inFileName + "\""
						+ " + user.dir = \"" + System.getProperty( "user.dir" )
						+ "\"";
				}
			}
	
			// If we didn't find it, try it by itself
			if( theFile == null && ! securityFault )
			{
				debugMsg( kFName, "Trying relative just by itself" );
				try
				{
					theFile = new File( inFileName );
					if( ! theFile.exists() )
						theFile = null;
				}
				catch (SecurityException e)
				{
					theFile = null;
					securityFault = true;
					msg = "Security error: " + e
						+ ", file = \"" + inFileName + "\""
						;
				}	
				// If this actually worked it's a pretty odd circumstance,
				// we don't expect this, so mention it

				if( theFile != null )
					warningMsg( kFName,
						"Relative file reference was resolved by tertiary logic."
						+ " This is somewhat unusual, you may want to check your paths."
						+ " File = \"" + inFileName + "\"."
						);
			}
			
			// OK, we've tried everything we can
			// If it's still not right, we'll complain below

		}	// End else it was relative (not absolute)

		if( securityFault )
			throw new FileNotFoundException( kExTag + msg );
		if( theFile == null )
			throw new FileNotFoundException( kExTag
				+ "Could not locate file \"" + inFileName + "\""
				);

		// Tell the caller what we finally wound up with
		if( ioAuxPathInfo != null )
		{
			String tmpName = null;
			try
			{
				// getCanonicalPath() is nice because it
				// removes ../'s, etc.
				tmpName = theFile.getCanonicalPath();
				debugMsg( kFName, "Saving final path of \"" + tmpName + "\"" );
			}
			catch (Exception e)
			{
				tmpName = theFile.getAbsolutePath();
				warningMsg( kFName,
					"Was unable to get canonical path for file"
					+ " \"" + inFileName + "\""
					+ ", error was: " + e
					+ " Will use absolute path instead."
					+ " New path = \"" + tmpName + "\"."
					);
			}
			ioAuxPathInfo.setFinalURI( tmpName );
		}

		// We're done!
		return theFile;
	}

	// This is like the file constructor stuff, but it
	// takes the current directory into account
	public static File findInputFileV0( String inFileName )
		throws FileNotFoundException, IOException
	{
		File theFile = null;
		try
		{
			theFile = new File(
					System.getProperty( "user.dir" ),
					inFileName
				);
			InputStream tmpStream = new FileInputStream( theFile );
			tmpStream.close();
			// theFile.exists();
		}
		catch (SecurityException e)
		{
			theFile = new File( inFileName );
			InputStream tmpStream = new FileInputStream( theFile );
			tmpStream.close();
		}

		return theFile;
	}


	// This is just a wrapper around the binary versions of openFileReadBin
	// coded below.
	// Just like Reader, except with line numbers.
	public static LineNumberReader openURLReadChar( String inBaseName )
			throws IOException
	{
		return openURLReadChar( inBaseName, null, null, null, null, false );
	}
	public static LineNumberReader openURLReadChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
			throws IOException
	{
		return openURLReadChar(
			inBaseName, optRelativeRoot,
			optUsername, optPassword,
			null, false
			);
	}
	public static LineNumberReader openURLReadChar(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxIOInfo,
		boolean inUsePost
		)
			throws IOException
	{

		// Try to get a stream
		// If it fails it should throw an exception
		InputStream lStream = openURLReadBin(
			inBaseName, optRelativeRoot,
			optUsername, optPassword,
			inoutAuxIOInfo, inUsePost
			);

		// Now convert it to a character reading gizmo

		String encoding = calculateEncoding( inBaseName, optRelativeRoot, inoutAuxIOInfo );

		// BufferedReader has readLine()
		// LineNumberReader has readLine AND getLineNumber
		LineNumberReader outReader = new LineNumberReader(
			// new InputStreamReader( lStream ), 2048
			new InputStreamReader( lStream, encoding ), 2048
			);

		// return it
		return outReader;
	}


	// First signature just sets the options to null/false.
	public static InputStream openURLReadBin( String inBaseName )
			throws IOException
	{
		return openURLReadBin( inBaseName, null );
	}
	public static InputStream openURLReadBin(
		String inBaseName,
		String optRelativeRoot
		)
			throws IOException
	{
		return openURLReadBin( inBaseName, optRelativeRoot,
			null, null, null, false
			);
	}

	public static InputStream openURLReadBin(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword
		)
			throws IOException
	{
		return openURLReadBin( inBaseName, optRelativeRoot,
			optUsername, optPassword, null, false
			);
	}

	// Version 1: 2001-2008
	// Ran for a very long time, but was old code and didn't handle
	// mangled HTTP headers very well
	// But given the importance of this routine, leaving this old version
	// in the source for now
	//
	// WARNING: We only set the client-name field if you
	// don't pass us in an auxIO
	// Otherwise we ASSUME you've put them in the http header hash
	public static InputStream openURLReadBin_V1(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxInfo,
		boolean inUsePost
		)
			throws IOException
	{
	
			// final boolean debug = true;
			final String kFName = "openURLReadBin_V1";
			final String kExTag = kClassName + '.' + kFName + ": ";
	
			boolean debug = shouldDoDebugMsg( kFName );
			boolean trace = shouldDoTraceMsg( kFName );
	
			inBaseName = trimmedStringOrNull( inBaseName );
			if( inBaseName == null )
				throw new IOException( kExTag
					+ "Was passed a null input."
					);
			optRelativeRoot = trimmedStringOrNull( optRelativeRoot );
	
	
			if(debug) debugMsg( kFName,
				"Start.  URI=\"" + inBaseName + "\""
				+ ", optional relative root=\"" + optRelativeRoot + "\""
				);
	
			String localClientString = null;
			// If aux info is not null we will IGNORE this setting
			if( inoutAuxInfo != null )
			{
				localClientString = inoutAuxInfo.getClientName();
				debugMsg( kFName,
					"Have a non-null aux object, setting to .getClientName"
					+ " which = \"" + localClientString + "\""
					);
			}
			// If it's null, default to using our default
			else
			{
				localClientString = AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD;
				debugMsg( kFName,
					"Have a NULL aux object, setting to static default"
					+ " which = \"" + localClientString + "\""
					);
			}
			// TODO: way to supprsee field all together
	
	
			// Setup user name and password stuff, if it was indicated
			if( optUsername != null || optPassword != null )
			{
				setupGlobalHTTPAuthentication( optUsername, optPassword );
				debugMsg( kFName,
					"Setting up global http authentication"
					);
			}
			else
			{
				debugMsg( kFName,
					"No username and/or pwd set, not setting up authentication."
					);
			}
	
			// Start with just the base name
			String theURLString = inBaseName;
			// If we have a parent base and it looks like a URL, use it
			// Notice, unlike the file based method, there is no "force" flag
			if( null != optRelativeRoot && isStringAURL(optRelativeRoot))
			{
				debugMsg( kFName,
					"Non null relative URL base = \"" + optRelativeRoot + "\""
					);
				theURLString = combineParentAndChildURLs(
					optRelativeRoot, inBaseName
					);
			}
			// Else if not null, but not a URL, let them know
			else if( null != optRelativeRoot )
			{
				debugMsg( kFName,
					"Ignoring non-null parent \"" + optRelativeRoot + "\""
					+ " because it doesn't appear to be a URL."
					+ " Will just use \"" + inBaseName + "\" by itself."
					);
			}
	
			// Record the URL we will actually attempt
			// We actually store this BEFORE we add any CGI fields
			if( inoutAuxInfo != null )
			{
				inoutAuxInfo.outAttemptedURI = theURLString;
				inoutAuxInfo.setFinalURI( theURLString );
			}
	
			debugMsg( kFName,
				"Final calculated URL = \"" + theURLString + "\""
				);
	
			// Add any CGI fields to URL, if any
	
			// Now get all the variables as an encoded string
			// Will be used for GET or POST
			String lCGIBuffer = null;
			if( inoutAuxInfo != null ) {
			    lCGIBuffer = inoutAuxInfo.getCGIFieldsAsEncodedBuffer();
			    if( lCGIBuffer.length() <= 0 )
			        lCGIBuffer = null;
			}
	
			debugMsg( kFName, "inUsePost=" + inUsePost );
	
			// Only works with GET
			if( null != lCGIBuffer && ! inUsePost )
			{
				debugMsg( kFName,
					"Will add any cgi fields from aux io object."
					+ " Use trace mode to see details."
					);
	
				if(debug) debugMsg( kFName, "CGI Buffer = \"" + lCGIBuffer + "\"" );
	
				// Join the two strings together, usually with a "?"
				theURLString = NIEUtil.concatURLAndEncodedCGIBuffer(
					theURLString, lCGIBuffer
					);
	
				traceMsg( kFName, "theURLString now = \"" + theURLString + "\"" );
	
			}
	
			// Now open the URL
			URL lURL = null;
			URLConnection lURLConnection = null;
			try
			{
				if(debug) debugMsg( kFName,
					"Calling URL constructor."
					+ " Full URL = \"" + theURLString + "\""
					);
	
				// statusMsg( kFName, "inoutAuxInfo=" + inoutAuxInfo );
				// statusMsg( kFName, "URL="+theURLString );
	
				lURL = new URL( theURLString );
				debugMsg( kFName,
					"Calling URL .openConnection()"
					);
				lURLConnection = lURL.openConnection();
			}
	//		catch(MalformedException me)
	//		{
	//			throw new Exception( "Bad URL '" +
	//				inFetchURLStr + "', error was '" + me + "'"
	//				);
	//		}
			catch(IOException ioe)
			{
				throw new IOException( "Error opening URL '" +
					theURLString + "', error was '" + ioe + "'"
					);
			}
	
			debugMsg( kFName,
				"Back from calling URL .openConnection()"
				);
	
			// Setup the user agent field
			if( localClientString != null && inoutAuxInfo == null )
			{
				debugMsg( kFName,
					"Setting request property for user agent field."
					);
				lURLConnection.setRequestProperty(
					AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
					localClientString
					);
			}
	
			// If we have a hash of HTTP info, add it in
			if( inoutAuxInfo != null )
			{
				debugMsg( kFName,
					"Setting addtional HTTP header fields from auxio object."
					+ " Use trace mode to see details."
					);
				List keys = inoutAuxInfo.getHTTPFieldFieldKeys();
				for( Iterator it = keys.iterator(); it.hasNext(); )
				{
					// Get the key and value
					String key = (String)it.next();
					String value = inoutAuxInfo.getScalarHTTPFieldTrimOrNull( key );
					// Add if we got a good value
					if( value != null )
					{
						traceMsg( kFName,
							"Setting HTTP header field: \"" + key + "\"=\"" + value + "\""
							);
						lURLConnection.setRequestProperty(
							key,
							value
							);
					}
				}
			}	// End if we had a Hash of HTTP info
	
	
			// About to connect....
			boolean lMoreHeaders = true;
			int lHeaderNumber = 0;
			boolean saw200Header = false;
			
			// Add the POST header if needed
			OutputStream sout = null;
			Writer wout = null;
			if( null!=lCGIBuffer && inUsePost ) {
			    // Lots of stuff I guess we don't need
			    // See http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html
	
			    // lURLConnection.setRequestProperty(
			    //		"Content-Type",
			    //		"application/x-www-form-urlencoded"
			    //		);
			    // lURLConnection.setRequestProperty(
				//		"Content-Length",
				//		"" + lCGIBuffer.length()
				//		);
			    // lURLConnection.setDoInput( true );
			    lURLConnection.setDoOutput( true );
				debugMsg( kFName,
						"Done with setup, will connect and submit data via POST"
						);
	
				// THIS will actually invoke the connection
				// lURLConnection.connect();
	
				// Send our data to the system
				sout = lURLConnection.getOutputStream();
			    wout = new OutputStreamWriter( sout );
			    // wout.write( "\r\n\r\n" );
			    wout.write( lCGIBuffer );
			    // wout.write( "\r\n" );
			    // Try closing here..., may need to close later
			    wout.close();
			    // sout.close();
			    sout = null;
	
			    debugMsg( kFName,
						"will fetch header fields after POST."
						+ " Use trace mode to see more details."
						);
	
			}
			// Else just a GET (or no fields to worry about)
			else {
				debugMsg( kFName,
					"Done with setup, will connect and fetch header fields via GET (which initiates the connection)."
					+ " Use trace mode to see more details."
					);
				// THIS will actually invoke the connection
				lURLConnection.connect();
			}
			
			// Read until no more headers
			while( lMoreHeaders )
			{
				traceMsg( kFName,
					"Reqeusting header offset " + lHeaderNumber
					);
				String lHeaderKey = lURLConnection.getHeaderFieldKey( lHeaderNumber );
				String lHeader = lURLConnection.getHeaderField( lHeaderNumber );
				lHeaderNumber++;
				traceMsg( kFName, "Header: "
					+ "\"" + lHeaderKey + "\"=\"" + lHeader + "\""
					);
				if( lHeader != null )
				{
					if( inoutAuxInfo != null )
					{
						if( inoutAuxInfo.fIoHTTPHeaders == null )
							inoutAuxInfo.fIoHTTPHeaders = new Vector();
						inoutAuxInfo.fIoHTTPHeaders.add( lHeader );
					}
					lHeader = lHeader.trim().toUpperCase();
				}
	
				// If header not null and starts with http/
				if( lHeader != null && lHeader.startsWith( "HTTP/" ) )
				{
					traceMsg( kFName,
						"HTTP Header Row."
						);
					// lMoreHeaders = false; <<== todo: mbennett: I think this is wrong
					lHeader = lHeader.substring( lHeader.indexOf(' ') ).trim();
					if( lHeader.startsWith("200") )
						saw200Header = true;
					else
						throw new IOException( "Got non-200 HTTP header:" +
							lHeader +
							" URL='" + theURLString + "'"
							);
				}
				if( lHeader == null )
					lMoreHeaders = false;
			}	// End while reading headers
	
			// Were we redirected?
			// OBSESS about what we might have wound up with
			// The Net claims URLConnection's getURL() will give the final
			// URL that was used PROVIDED some intervening calls are made that
			// actually estblish and fetch content or headers
			// We have done that by now by getting the headers
			URL tmpURL = lURLConnection.getURL();
			String finalURLString =  null;
			if( tmpURL != null )
			{
				finalURLString = tmpURL.toExternalForm();
				finalURLString = trimmedStringOrNull( finalURLString );
	
				// Did it change?  Did we get a redirect?
				if( finalURLString != null && ! finalURLString.equals( theURLString ) )
				{
					// Todo: Not sure this is the right thing to do?
					// Issues:
					// 1: The URL's might be different just because of CGI variables.
					//	Currently we grab it before we add on our CGI vars, assuming you
					//	wouldn't want all that lengthy BS for a context URL.
					//	Or do you?
					// 2: If we got a redirect, should we use the new URL or the old
					//	URL as the base for any subsequent relative references?
					//	That might even depend on which URL redirect code was issued.
	
					// Chop them both down
					// The original one
					String urlStr1 = theURLString;
					int at1 = urlStr1.indexOf( '?' );
					if( at1 > 0 )
						urlStr1 = urlStr1.substring( 0, at1 );
					String urlStr2 = finalURLString;
					int at2 = urlStr2.indexOf( '?' );
					if( at2 > 0 )
						urlStr2 = urlStr2.substring( 0, at2 );
	
					// NOW see if they're the same
					if( ! urlStr1.equals( urlStr2 ) )
					{
						// If it's just a trailing slash, don't warn
						if( urlStr1.length() == urlStr2.length()-1
							&& urlStr2.endsWith( "/" )
							)
						{
							// It's just a trailing slash
							// like http://www.kfu.com/~mbennett
							//   to http://www.kfu.com/~mbennett/
							debugMsg( kFName, "New URL just has a trailing slash." );
						}
						else
						{
							infoMsg( kFName,
								"The final URL we read from seems to be different"
								+ " than what we started with."
								+ " This may be OK, perhaps it's the result of redirects."
								+ " Orginal URL (minus any CGI) = \"" + urlStr1 + "\""
								+ " Final URL (minus any CGI) = \"" + urlStr2 + "\""
								);
						}
						// Record the URL we will actually got back
						if( inoutAuxInfo != null )
						{
							// Technically we should obsess about WHICH status code we got back
							// But we don't have access to that, if Java has done the
							// iterations for us, this is the last URL's status, not the first
							// Most of the time it's 301
							debugMsg( kFName, "Overwriting final URL in Aux IO Object." );
							inoutAuxInfo.setFinalURI( urlStr2 );
							// actually the URL we ATTEMPTED is still what it was before
							// inoutAuxInfo.outAttemptedURI = urlStr2;
						}
					}	// End if they still match after dropping the CGI B.S.
	
				}	// End if we got a string from the URL and it didn't match the orig URL
			}	// End if we got back a URL Object from the connection
	
	
			debugMsg( kFName,
				"Done reading header lines, will now call .getInputStream()"
				);
	
	
			//InputStream lStream = lURL.openStream();
			InputStream returnStream = lURLConnection.getInputStream();
	
			debugMsg( kFName,
				"At end, returning stream."
				);
	
			return returnStream;
	
		}
	// The real deal
		// WARNING: We only set the client-name fields if you
		// don't pass us in an auxIO
		// Otherwise we ASSUME you've put them in the http header hash
		// TODO: right now we don't put host
		public static InputStream openURLReadBin(
			String inBaseName,
			String optRelativeRoot,
			String optUsername,
			String optPassword,
			AuxIOInfo inoutAuxInfo,
			boolean inUsePost
			)
				throws IOException
		{
	
			// final boolean debug = true;
			final String kFName = "openURLReadBin";
			final String kExTag = kClassName + '.' + kFName + ": ";

			// Call newer logic if we want fancy redirects
			if( null!=inoutAuxInfo && inoutAuxInfo.getUseCarefulRedirects()
			) {
				debugMsg( kFName, "Will defer to _WithManualRedirects version" );
				return openURLReadBin_WithManualRedirects(
						inBaseName, optRelativeRoot,
						optUsername, optPassword,
						inoutAuxInfo, inUsePost
						);
			}

			boolean debug = shouldDoDebugMsg( kFName );
			boolean trace = shouldDoTraceMsg( kFName );
	
			inBaseName = trimmedStringOrNull( inBaseName );
			if( inBaseName == null )
				throw new IOException( kExTag
					+ "Was passed a null input."
					);
			optRelativeRoot = trimmedStringOrNull( optRelativeRoot );
	
			if(debug) debugMsg( kFName,
				"Start.  URI=\"" + inBaseName + "\""
				+ ", optional relative root=\"" + optRelativeRoot + "\""
				);
	
			String localClientString = null;
			// If aux info is not null we will IGNORE this setting
			if( inoutAuxInfo != null )
			{
				localClientString = inoutAuxInfo.getClientName();
				debugMsg( kFName,
					"Have a non-null aux object, setting to .getClientName"
					+ " which = \"" + localClientString + "\""
					);
			}
			// If it's null, default to using our default
			else
			{
				localClientString = AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD;
				debugMsg( kFName,
					"Have a NULL aux object, setting to static default"
					+ " which = \"" + localClientString + "\""
					);
			}
			// TODO: way to supprsee field all together
	
	
			// Setup user name and password stuff, if it was indicated
			if( optUsername != null || optPassword != null )
			{
				setupGlobalHTTPAuthentication( optUsername, optPassword );
				debugMsg( kFName,
					"Setting up global http authentication"
					);
			}
			else
			{
				debugMsg( kFName,
					"No username and/or pwd set, not setting up authentication."
					);
			}
	
			// Start with just the base name
			String theURLString = inBaseName;
			// If we have a parent base and it looks like a URL, use it
			// Notice, unlike the file based method, there is no "force" flag
			if( null != optRelativeRoot && isStringAURL(optRelativeRoot))
			{
				debugMsg( kFName,
					"Non null relative URL base = \"" + optRelativeRoot + "\""
					);
				theURLString = combineParentAndChildURLs(
					optRelativeRoot, inBaseName
					);
			}
			// Else if not null, but not a URL, let them know
			else if( null != optRelativeRoot )
			{
				debugMsg( kFName,
					"Ignoring non-null parent \"" + optRelativeRoot + "\""
					+ " because it doesn't appear to be a URL."
					+ " Will just use \"" + inBaseName + "\" by itself."
					);
			}
	
			// Record the URL we will actually attempt
			// We actually store this BEFORE we add any CGI fields
			if( inoutAuxInfo != null )
			{
				inoutAuxInfo.outAttemptedURI = theURLString;
				inoutAuxInfo.setFinalURI( theURLString );
			}
	
			debugMsg( kFName,
				"Final calculated URL = \"" + theURLString + "\""
				);
	
			// Add any CGI fields to URL, if any
	
			// Now get all the variables as an encoded string
			// Will be used for GET or POST
			String lCGIBuffer = null;
			if( inoutAuxInfo != null ) {
			    lCGIBuffer = inoutAuxInfo.getCGIFieldsAsEncodedBuffer();
			    if( lCGIBuffer.length() <= 0 )
			        lCGIBuffer = null;
			}
	
			debugMsg( kFName, "inUsePost=" + inUsePost );
	
			// Only works with GET
			if( null != lCGIBuffer && ! inUsePost )
			{
				debugMsg( kFName,
					"Will add any cgi fields from aux io object."
					+ " Use trace mode to see details."
					);
	
				if(debug) debugMsg( kFName, "CGI Buffer = \"" + lCGIBuffer + "\"" );
	
				// Join the two strings together, usually with a "?"
				theURLString = NIEUtil.concatURLAndEncodedCGIBuffer(
					theURLString, lCGIBuffer
					);
	
				traceMsg( kFName, "theURLString now = \"" + theURLString + "\"" );
	
			}
	
			// Now open the URL
			URL lURL = null;
			URLConnection lURLConnection = null;
			try
			{
				if(debug) debugMsg( kFName,
					"Calling URL constructor."
					+ " Full URL = \"" + theURLString + "\""
					);
	
				// statusMsg( kFName, "inoutAuxInfo=" + inoutAuxInfo );
				// statusMsg( kFName, "URL="+theURLString );
	
				lURL = new URL( theURLString );
				debugMsg( kFName,
					"Calling URL .openConnection()"
					);
				lURLConnection = lURL.openConnection();
				
				// In manual redirect version, we also do this:
				// castConn = (HttpURLConnection) lURLConnection;
				// castConn.setInstanceFollowRedirects( false );
			}
	//		catch(MalformedException me)
	//		{
	//			throw new Exception( "Bad URL '" +
	//				inFetchURLStr + "', error was '" + me + "'"
	//				);
	//		}
			catch(IOException ioe)
			{
				throw new IOException( "Error opening URL '" +
					theURLString + "', error was '" + ioe + "'"
					);
			}
	
			debugMsg( kFName,
				"Back from calling URL .openConnection()"
				);
	
			// Setup the user agent field
			if( localClientString != null && inoutAuxInfo == null )
			{
				debugMsg( kFName,
					"Setting request property for user agent field."
					);
				lURLConnection.setRequestProperty(
					AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
					localClientString
					);
			}
	
			// If we have a hash of HTTP info, add it in
			if( inoutAuxInfo != null )
			{
				debugMsg( kFName,
					"Setting addtional HTTP header fields from auxio object."
					+ " Use trace mode to see details."
					);
				List keys = inoutAuxInfo.getHTTPFieldFieldKeys();
				for( Iterator it = keys.iterator(); it.hasNext(); )
				{
					// Get the key and value
					String key = (String)it.next();
					String value = inoutAuxInfo.getScalarHTTPFieldTrimOrNull( key );
					// Add if we got a good value
					if( value != null )
					{
						traceMsg( kFName,
							"Setting HTTP header field: \"" + key + "\"=\"" + value + "\""
							);
						lURLConnection.setRequestProperty(
							key,
							value
							);
					}
				}
			}	// End if we had a Hash of HTTP info
			
			// Add the POST header if needed
			OutputStream sout = null;
			Writer wout = null;
			if( null!=lCGIBuffer && inUsePost )
			{
			    // Lots of stuff I guess we don't need
			    // See http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html
	
			    // lURLConnection.setRequestProperty(
			    //		"Content-Type",
			    //		"application/x-www-form-urlencoded"
			    //		);
			    // lURLConnection.setRequestProperty(
				//		"Content-Length",
				//		"" + lCGIBuffer.length()
				//		);
			    // lURLConnection.setDoInput( true );
			    lURLConnection.setDoOutput( true );
				debugMsg( kFName,
						"Done with setup, will connect and submit data via POST"
						);
	
				// THIS will actually invoke the connection
				// lURLConnection.connect();
	
				// Send our data to the system
				sout = lURLConnection.getOutputStream();
			    wout = new OutputStreamWriter( sout );
			    // wout.write( "\r\n\r\n" );
			    wout.write( lCGIBuffer );
			    // wout.write( "\r\n" );
			    // Try closing here..., may need to close later
			    wout.close();
			    // sout.close();
			    sout = null;
	
			    debugMsg( kFName,
						"will fetch header fields after POST."
						+ " Use trace mode to see more details."
						);
			}
			// Else just a GET (or no fields to worry about)
			else {
				debugMsg( kFName,
					"Done with setup, will connect and fetch header fields via GET (which initiates the connection)."
					+ " Use trace mode to see more details."
					);
				// THIS will actually invoke the connection
				lURLConnection.connect();
			}
	
			// About to connect....
			int headerOffset = 0;
			boolean sawHttpStatusLine = false;
			boolean saw200Header = false;
			
			// if( null!=inoutAuxIOInfo && inoutAuxIOInfo.getIsSuspiciousHttpResponse() )
	
			// Read until no more headers
			while( true )
			{
				if(trace) traceMsg( kFName,
					"Reqeusting header offset " + headerOffset
					);
				String headerKey = lURLConnection.getHeaderFieldKey( headerOffset );
				String headerVal = lURLConnection.getHeaderField( headerOffset );
				if(trace) traceMsg( kFName, "Header: "
					+ "\"" + headerKey + "\"=\"" + headerVal + "\""
					);
	
				// ESCAPE CLAUSE
				if( null==headerVal ) {
					if(debug) debugMsg( kFName, "Null header value, break, header line offset=" + headerOffset );
					break;
				}
	
				// Track headers
				if( null != inoutAuxInfo )
				{
					if( null == inoutAuxInfo.fIoHTTPHeaders )
						inoutAuxInfo.fIoHTTPHeaders = new Vector();
					String headerLine = (null==headerKey)
						? headerVal
						: headerKey + ": " + headerVal;
						;
					if(trace) traceMsg( kFName, "Storing HTTP header '" + headerLine + "'" );
					inoutAuxInfo.fIoHTTPHeaders.add( headerLine );
				}
	
				String tmpVal = headerVal.trim().toUpperCase();
	
				// If header not null and starts with http/
				if( tmpVal.startsWith( "HTTP/" ) )
				{
					if( headerOffset > 0 )
					{
						String msg =
							"HTTP header NOT the first line"
							+ ", base0_offset=" + headerOffset
							+ ", key='" + headerKey + "'"
							+ ", value='" + headerVal + "'"
							;
						if( null != inoutAuxInfo )
						{
							if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
							{
								inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
								warningMsg( kFName, msg );
							}
							else {
								if(debug) debugMsg( kFName,
									"Told to suppress this suspicious HTTP issue: " + msg );
							}
						}
						else {
							warningMsg( kFName, msg );						
						}
					}
					sawHttpStatusLine = true;
	
					if(trace) traceMsg( kFName,
						"HTTP Header Row."
						);
					tmpVal = tmpVal.substring( tmpVal.indexOf(' ') ).trim();
					if( tmpVal.startsWith("200") )
						saw200Header = true;
					else
						throw new IOException( "Got non-200 HTTP header:" +
								headerVal +
							" URL='" + theURLString + "'"
							);
				}	// end if HTTP row
	
				// If header not null and starts with http/
				// Mime-Type visited AGAIN later on
				if( null!=headerKey && headerKey.equalsIgnoreCase("Content-type") )
				{
					// I think this was for another flavor of spurious error we were getting
					if( null!=headerVal && headerVal.equalsIgnoreCase("unknown/unknown") )
					{
						String msg =
							"HTTP header Content-type is UNKNOWN"
							+ ", base0_offset=" + headerOffset
							+ ", key='" + headerKey + "'"
							+ ", value='" + headerVal + "'"
							;
						if( null != inoutAuxInfo )
						{
							if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
							{
								inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
								warningMsg( kFName, msg );
							}
							else {
								if(debug) debugMsg( kFName,
									"Told to suppress this suspicious HTTP issue: " + msg );
							}
						}
						else {
							warningMsg( kFName, msg );						
						}					
					}
				}
	
				// Increment at the end
				headerOffset++;
			}	// End while reading headers
	
			// Double check the headers we saw
			if( ! sawHttpStatusLine )
			{
				String msg = "Did not see HTTP status line in headers";
				if( null != inoutAuxInfo )
				{
					if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
					{
						inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
						warningMsg( kFName, msg );
					}
					else {
						if(debug) debugMsg( kFName,
							"Told to suppress this suspicious HTTP issue: " + msg );
					}
				}
				else {
					warningMsg( kFName, msg );						
				}
			}
	
			// Final check for suspicious HTTP
			// Can optionally be configured to bail at this point
			// We wait until here because:
			// 1: We only do this code once,
			// and
			// 2: There could be multiple issues, so by now we can list them all
			if( null != inoutAuxInfo
					&& inoutAuxInfo.getSawSuspiciousHttpResponse()
					&& inoutAuxInfo.getPromoteSuspiciousHttpToException()
			) {
				throw new IOException( kExTag
					+ "Honoring flag for escalation of suspicious HTTP headers: "
					+ inoutAuxInfo.getSuspiciousHttpReason()
					);
			}
	
			// Were we redirected?
			// OBSESS about what we might have wound up with
			// The Net claims URLConnection's getURL() will give the final
			// URL that was used PROVIDED some intervening calls are made that
			// actually estblish and fetch content or headers
			// We have done that by now by getting the headers
			URL tmpURL = lURLConnection.getURL();
			String finalURLString =  null;
			if( tmpURL != null )
			{
				finalURLString = tmpURL.toExternalForm();
				finalURLString = trimmedStringOrNull( finalURLString );
	
				// Did it change?  Did we get a redirect?
				if( finalURLString != null && ! finalURLString.equals( theURLString ) )
				{
					// Todo: Not sure this is the right thing to do?
					// Issues:
					// 1: The URL's might be different just because of CGI variables.
					//	Currently we grab it before we add on our CGI vars, assuming you
					//	wouldn't want all that lengthy BS for a context URL.
					//	Or do you?
					// 2: If we got a redirect, should we use the new URL or the old
					//	URL as the base for any subsequent relative references?
					//	That might even depend on which URL redirect code was issued.
	
					// Chop them both down
					// The original one
					String urlStr1 = theURLString;
					int at1 = urlStr1.indexOf( '?' );
					if( at1 > 0 )
						urlStr1 = urlStr1.substring( 0, at1 );
					String urlStr2 = finalURLString;
					int at2 = urlStr2.indexOf( '?' );
					if( at2 > 0 )
						urlStr2 = urlStr2.substring( 0, at2 );
	
					// NOW see if they're the same
					if( ! urlStr1.equals( urlStr2 ) )
					{
						// If it's just a trailing slash, don't warn
						if( urlStr1.length() == urlStr2.length()-1
							&& urlStr2.endsWith( "/" )
							)
						{
							// It's just a trailing slash
							// like http://www.kfu.com/~mbennett
							//   to http://www.kfu.com/~mbennett/
							if(debug) debugMsg( kFName, "New URL just has a trailing slash." );
						}
						else
						{
							infoMsg( kFName,
								"The final URL we read from seems to be different"
								+ " than what we started with."
								+ " This may be OK, perhaps it's the result of redirects."
								+ " Orginal URL (minus any CGI) = \"" + urlStr1 + "\""
								+ " Final URL (minus any CGI) = \"" + urlStr2 + "\""
								);
						}
						// Record the URL we will actually got back
						if( inoutAuxInfo != null )
						{
							// Technically we should obsess about WHICH status code we got back
							// But we don't have access to that, if Java has done the
							// iterations for us, this is the last URL's status, not the first
							// Most of the time it's 301
							if(debug) debugMsg( kFName, "Overwriting final URL in Aux IO Object." );
							inoutAuxInfo.setFinalURI( urlStr2 );
							// actually the URL we ATTEMPTED is still what it was before
							// inoutAuxInfo.outAttemptedURI = urlStr2;
						}
					}	// End if they still match after dropping the CGI B.S.
	
				}	// End if we got a string from the URL and it didn't match the orig URL
			}	// End if we got back a URL Object from the connection
	
			// Track Content Type / Mime Type and Character encoding
			if( null != inoutAuxInfo )
			{
				String rawContentType = lURLConnection.getContentType();
				String normContentLine = NIEUtil.trimmedLowerStringOrNull( rawContentType );
				if(debug) debugMsg( kFName, "rawContentType='" + rawContentType + "'" );
				if( null!=normContentLine )
				{
					String contentType = null;
					String encoding = null;
					
					// We need to look at each subfield
					// Normally the first one is content/type
					// and the second is charaset
					// but subfields after the first one are named and there
					// can be more than one
					StringTokenizer st = new StringTokenizer( normContentLine, ";" );
					boolean haveSeenFirst = false;
					// For each subfield
					while( st.hasMoreTokens() )
					{
						String item = st.nextToken();
						item = NIEUtil.trimmedStringOrNull( item );
						// Skip empties
						if( null==item )
						{
							haveSeenFirst = true;
							continue;
						}
						// First in list has no field name and is always mime type
						if( ! haveSeenFirst )
						{
							contentType = item;
							haveSeenFirst = true;
						}
						// Else it's a subsequent field denoted by field name
						else {
						
							int equalsAt = item.indexOf('=');
							String key = null;
							String value = null;
							// No equals or right at the start
							if( equalsAt <= 0 )
							{
								warningMsg( kFName, "Uknown http content-type subfield key, no/misplaced equals sign"
									+ ", item='" + item + "'"
									+ ", parsed from http header line: '" + rawContentType + "'"
									+ " For original URL '" + theURLString + "'"
									+ ", final URL '" + finalURLString + "'."
									);
							}
							// good so far
							else
							{
								// get the key
								key = NIEUtil.trimmedStringOrNull( item.substring(0,equalsAt) );
								// Double check, this is really important
								if( null==key )
								{
									warningMsg( kFName, "Empty http content-type subfield key, trimmed to null"
										+ ", item='" + item + "'"
										+ ", parsed from http header line: '" + rawContentType + "'"
										+ " For original URL '" + theURLString + "'"
										+ ", final URL '" + finalURLString + "'."
										);								
								}
								// And the value, make sure there's something there
								else if( equalsAt<(item.length()-1) )
								{
									value = NIEUtil.trimmedStringOrNull( item.substring(equalsAt+1) );
									if( null==value )
									{
										warningMsg( kFName, "Empty http content-type subfield valued, trimmed to null"
											+ ", item='" + item + "'"
											+ ", parsed from http header line: '" + rawContentType + "'"
											+ " For original URL '" + theURLString + "'"
											+ ", final URL '" + finalURLString + "'."
											);								
									}
									// Else by now we have a key and a value
									else if( key.equals("charset") )
									{
										encoding = value;
									}
									// Unrecognized field
									else
									{
										warningMsg( kFName, "Unusual http content-type subfield name '" + key + "'"
											+ ", from item='" + item + "', ignoring."
											+ "Parsed from http header line: '" + rawContentType + "'"
											+ " For original URL '" + theURLString + "'"
											+ ", final URL '" + finalURLString + "'."
											);																
									}
								}
								// Else there is an equals sign but it's at the very end, so invalid
								else
								{
									warningMsg( kFName, "Empty http content-type subfield value, misplaced equals sign"
										+ ", item='" + item + "'"
										+ ", parsed from http header line: '" + rawContentType + "'"
										+ " For original URL '" + theURLString + "'"
										+ ", final URL '" + finalURLString + "'."
										);
								}
							}	// End else does have equals sign, and not at start
						}	// End else this is NOT the first subfield
					}	// End for each subfield

					// Store the results of what we've found
					if( null!=contentType )
						inoutAuxInfo.setContentType( contentType );
					else
						warningMsg( kFName, "No mime type parsed from http header line: '" + rawContentType + "'"
							+ " For original URL '" + theURLString + "'"
							+ ", final URL '" + finalURLString + "'."
							);				
					if( null!=encoding )
						inoutAuxInfo.setEncoding( encoding );
					else
						warningMsg( kFName, "No encoding parsed from http header line: '" + rawContentType + "'"
							+ " For orig URL '" + theURLString + "'"
							+ ", final URL '" + finalURLString + "'."
							);				

					if(debug) debugMsg( kFName, "Separated contentType='" + contentType + "', encoding='" + encoding + "'" );
				
				}
				// Else not there
				else {
					warningMsg( kFName, "Null/empty http header content-type line."
						+ " For orig URL '" + theURLString + "'"
						+ ", final URL '" + finalURLString + "'."
						);				
				}
			}
			
			if(debug) debugMsg( kFName,
				"Done reading header lines, will now call .getInputStream()"
				);
	
			//InputStream lStream = lURL.openStream();
			InputStream returnStream = lURLConnection.getInputStream();
	
			if(debug) debugMsg( kFName,
				"At end, returning stream."
				);
	
			return returnStream;
	
	}

	// This version iterates through every redirect manually
	// and is usually only called by the other openURLReadBin
	// if the optional auxio object has requested it
	private static InputStream openURLReadBin_WithManualRedirects(
		String inBaseName,
		String optRelativeRoot,
		String optUsername,
		String optPassword,
		AuxIOInfo inoutAuxInfo,
		boolean inUsePost
		)
			throws IOException
	{

		// final boolean debug = true;
		final String kFName = "openURLReadBin_WithManualRedirects";
		final String kExTag = kClassName + '.' + kFName + ": ";

		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );

		inBaseName = trimmedStringOrNull( inBaseName );
		if( inBaseName == null )
			throw new IOException( kExTag
				+ "Was passed a null input."
				);
		optRelativeRoot = trimmedStringOrNull( optRelativeRoot );

		if(debug) debugMsg( kFName,
			"Start.  URI=\"" + inBaseName + "\""
			+ ", optional relative root=\"" + optRelativeRoot + "\""
			);

		String localClientString = null;
		// If aux info is not null we will IGNORE this setting
		if( inoutAuxInfo != null )
		{
			localClientString = inoutAuxInfo.getClientName();
			debugMsg( kFName,
				"Have a non-null aux object, setting to .getClientName"
				+ " which = \"" + localClientString + "\""
				);
		}
		// If it's null, default to using our default
		else
		{
			localClientString = AuxIOInfo.DEFAULT_HTTP_USER_AGENT_FIELD;
			debugMsg( kFName,
				"Have a NULL aux object, setting to static default"
				+ " which = \"" + localClientString + "\""
				);
		}
		// TODO: way to suppress field all together


		// Setup user name and password stuff, if it was indicated
		if( optUsername != null || optPassword != null )
		{
			setupGlobalHTTPAuthentication( optUsername, optPassword );
			debugMsg( kFName,
				"Setting up global http authentication"
				);
		}
		else
		{
			debugMsg( kFName,
				"No username and/or pwd set, not setting up authentication."
				);
		}

		// Start with just the base name
		String theURLString = inBaseName;
		// If we have a parent base and it looks like a URL, use it
		// Notice, unlike the file based method, there is no "force" flag
		if( null != optRelativeRoot && isStringAURL(optRelativeRoot))
		{
			debugMsg( kFName,
				"Non null relative URL base = \"" + optRelativeRoot + "\""
				);
			theURLString = combineParentAndChildURLs(
				optRelativeRoot, inBaseName
				);
		}
		// Else if not null, but not a URL, let them know
		else if( null != optRelativeRoot )
		{
			debugMsg( kFName,
				"Ignoring non-null parent \"" + optRelativeRoot + "\""
				+ " because it doesn't appear to be a URL."
				+ " Will just use \"" + inBaseName + "\" by itself."
				);
		}

		// Record the URL we will actually attempt
		// We actually store this BEFORE we add any CGI fields
		if( inoutAuxInfo != null )
		{
			inoutAuxInfo.outAttemptedURI = theURLString;
			inoutAuxInfo.setFinalURI( theURLString );
		}

		debugMsg( kFName,
			"Intermediate calculated URL = \"" + theURLString + "\""
			);

		// Add any CGI fields to URL, if any

		// Now get all the variables as an encoded string
		// Will be used for GET or POST
		String lCGIBuffer = null;
		if( inoutAuxInfo != null ) {
		    lCGIBuffer = inoutAuxInfo.getCGIFieldsAsEncodedBuffer();
		    if( lCGIBuffer.length() <= 0 )
		        lCGIBuffer = null;
		}

		debugMsg( kFName, "inUsePost=" + inUsePost );

		// Only works with GET
		if( null != lCGIBuffer && ! inUsePost )
		{
			debugMsg( kFName,
				"Will add any cgi fields from aux io object."
				+ " Use trace mode to see details."
				);

			if(debug) debugMsg( kFName, "CGI Buffer = \"" + lCGIBuffer + "\"" );

			// Join the two strings together, usually with a "?"
			theURLString = NIEUtil.concatURLAndEncodedCGIBuffer(
				theURLString, lCGIBuffer
				);

			traceMsg( kFName, "theURLString now = \"" + theURLString + "\"" );

		}

		// Now open the URL
		URL currURL = null;
		URLConnection lURLConnection = null;
		HttpURLConnection castConn = null;
		try
		{
			if(debug) debugMsg( kFName,
				"Calling URL constructor."
				+ " Full URL = \"" + theURLString + "\""
				);

			// statusMsg( kFName, "inoutAuxInfo=" + inoutAuxInfo );
			// statusMsg( kFName, "URL="+theURLString );

			currURL = new URL( theURLString );
			debugMsg( kFName,
				"Calling URL .openConnection()"
				);
			lURLConnection = currURL.openConnection();
			// There is a difference between openConnection() and connect()
			castConn = (HttpURLConnection) lURLConnection;
			castConn.setInstanceFollowRedirects( false );
		}
//		catch(MalformedException me)
//		{
//			throw new Exception( "Bad URL '" +
//				inFetchURLStr + "', error was '" + me + "'"
//				);
//		}
		catch(IOException ioe)
		{
			throw new IOException( "Error opening URL '" +
				theURLString + "', error was '" + ioe + "'"
				);
		}

		debugMsg( kFName,
			"Back from calling URL .openConnection()"
			);

		// Setup the user agent field
		if( localClientString != null && inoutAuxInfo == null )
		{
			debugMsg( kFName,
				"Setting request property for user agent field."
				);
			lURLConnection.setRequestProperty(
				AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
				localClientString
				);
		}

		// If we have a hash of HTTP info to SEND
		// add it in
		if( inoutAuxInfo != null )
		{
			debugMsg( kFName,
				"Setting addtional HTTP header fields from auxio object."
				+ " Use trace mode to see details."
				);
			List keys = inoutAuxInfo.getHTTPFieldFieldKeys();
			for( Iterator it = keys.iterator(); it.hasNext(); )
			{
				// Get the key and value
				String key = (String)it.next();
				String value = inoutAuxInfo.getScalarHTTPFieldTrimOrNull( key );
				// Add if we got a good value
				if( value != null )
				{
					traceMsg( kFName,
						"Setting HTTP header field: \"" + key + "\"=\"" + value + "\""
						);
					lURLConnection.setRequestProperty(
						key,
						value
						);
				}
			}
		}	// End if we had a Hash of HTTP info
		
		// Add the POST header if needed
		OutputStream sout = null;
		Writer wout = null;
		if( null!=lCGIBuffer && inUsePost )
		{
		    // Lots of stuff I guess we don't need
		    // See http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html

		    // lURLConnection.setRequestProperty(
		    //		"Content-Type",
		    //		"application/x-www-form-urlencoded"
		    //		);
		    // lURLConnection.setRequestProperty(
			//		"Content-Length",
			//		"" + lCGIBuffer.length()
			//		);
		    // lURLConnection.setDoInput( true );
		    lURLConnection.setDoOutput( true );
			debugMsg( kFName,
					"Done with setup, will connect and submit data via POST"
					);

			// Send our data to the system
			sout = lURLConnection.getOutputStream();
		    wout = new OutputStreamWriter( sout );
		    wout.write( lCGIBuffer );
		    wout.close();
		    sout = null;

		    debugMsg( kFName,
					"will fetch header fields after POST."
					+ " Use trace mode to see more details."
					);
		}
		// Else just a GET (or no fields to worry about)
		else {
			debugMsg( kFName,
				"Done with setup, will connect and fetch header fields via GET (which initiates the connection)."
				+ " Use trace mode to see more details."
				);
			// THIS will actually invoke the connection
			lURLConnection.connect();
		}

		// Process all redirects
		int redirCounter = 0;
		int redirMax = null==inoutAuxInfo
			? AuxIOInfo.DEFAULT_MAX_REDIRECTS
			: inoutAuxInfo.getMaxDesiredRedirectLevels()
			;
		// Do until no more redirects
		while( true )
		{	

			int response = castConn.getResponseCode();
			debugMsg( kFName,
					"Top of redir loop, response code = " + response
					);
			boolean isRedirect = response >= 300 && response < 400;
			
			int headerOffset = 0;
			boolean sawHttpStatusLine = false;
			boolean saw200Header = false;
			
			
        	// Read headers
			// =================================

			// Read until no more headers
			while( true )
			{
				if(trace) traceMsg( kFName,
					"Reqeusting header offset " + headerOffset
					);
				String headerKey = lURLConnection.getHeaderFieldKey( headerOffset );
				String headerVal = lURLConnection.getHeaderField( headerOffset );
				if(trace) traceMsg( kFName, "Header: "
					+ "\"" + headerKey + "\"=\"" + headerVal + "\""
					);

				// ESCAPE CLAUSE
				if( null==headerVal ) {
					if(debug) debugMsg( kFName, "Null header value, break, header line offset=" + headerOffset );
					break;
				}

				// Track headers
				if( null != inoutAuxInfo )
				{
					if( null == inoutAuxInfo.fIoHTTPHeaders )
						inoutAuxInfo.fIoHTTPHeaders = new Vector();
					String headerLine = (null==headerKey)
						? headerVal
						: headerKey + ": " + headerVal;
						;
					if(trace) traceMsg( kFName, "Storing HTTP header '" + headerLine + "'" );
					inoutAuxInfo.fIoHTTPHeaders.add( headerLine );
				}

				String tmpVal = headerVal.trim().toUpperCase();

				// If header not null and starts with http/
				if( tmpVal.startsWith( "HTTP/" ) )
				{
					if( headerOffset > 0 )
					{
						String msg =
							"HTTP header NOT the first line"
							+ ", base0_offset=" + headerOffset
							+ ", key='" + headerKey + "'"
							+ ", value='" + headerVal + "'"
							;
						if( null != inoutAuxInfo )
						{
							if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
							{
								inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
								warningMsg( kFName, msg );
							}
							else {
								if(debug) debugMsg( kFName,
									"Told to suppress this suspicious HTTP issue: " + msg );
							}
						}
						else {
							warningMsg( kFName, msg );						
						}
					}
					sawHttpStatusLine = true;

					if(trace) traceMsg( kFName,
						"HTTP Header Row."
						);
					tmpVal = tmpVal.substring( tmpVal.indexOf(' ') ).trim();
					if( tmpVal.startsWith("200") )
						saw200Header = true;
					// Status 3xx is OK for now
					else if( tmpVal.startsWith("3") )
					{
						if(debug) debugMsg( kFName, "Starts status with '3': '" + tmpVal + "'" );
					}
					else
					{
						throw new IOException( "Got non-200/non-300 HTTP header:" +
								headerVal +
							" URL='" + theURLString + "'"
							);
					}
				}	// end if HTTP row
				
				if( isRedirect && null!=headerKey && headerKey.equalsIgnoreCase("Set-Cookie") )
				{
					warningMsg( kFName,
						"Cookie detected during rediret, will be dropped."
						+ " (not supported by IE)"
						+ ", key='" + headerKey + "'"
						+ ", value='" + headerVal + "'"
						);
				}
				
				// Mime Type / Content Type
				// Mime-Type visited AGAIN later on
				if( null!=headerKey && headerKey.equalsIgnoreCase("Content-type") )
				{
					// I think this was for another flavor of spurious error we were getting
					if( null!=headerVal && headerVal.equalsIgnoreCase("unknown/unknown") )
					{
						String msg =
							"HTTP header Content-type is UNKNOWN"
							+ ", base0_offset=" + headerOffset
							+ ", key='" + headerKey + "'"
							+ ", value='" + headerVal + "'"
							;
						if( null != inoutAuxInfo )
						{
							if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
							{
								inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
								warningMsg( kFName, msg );
							}
							else {
								if(debug) debugMsg( kFName,
									"Told to suppress this suspicious HTTP issue: " + msg );
							}
						}
						else {
							warningMsg( kFName, msg );						
						}					
					}
				}

				// We want to track this
				// TODO: Or use Java connection... and what about multiple redirs?
				// if( null != inoutAuxInfo )
				// {
				// }
				// Done BELOW, AFTER FINAL REDIR, using Java

				// Increment at the end
				headerOffset++;
			}	// End while reading headers

			// Double check the headers we saw
			if( ! sawHttpStatusLine )
			{
				String msg = "Did not see HTTP status line in headers";
				if( null != inoutAuxInfo )
				{
					if( ! inoutAuxInfo.getIgnoreSuspiciousHttpResponse() )
					{
						inoutAuxInfo.setSawSuspiciousHttpResponse( msg );
						warningMsg( kFName, msg );
					}
					else {
						if(debug) debugMsg( kFName,
							"Told to suppress this suspicious HTTP issue: " + msg );
					}
				}
				else {
					warningMsg( kFName, msg );						
				}
			}

			// Final check for suspicious HTTP
			// Can optionally be configured to bail at this point
			// We wait until here because:
			// 1: We only do this code once,
			// and
			// 2: There could be multiple issues, so by now we can list them all
			if( null != inoutAuxInfo
					&& inoutAuxInfo.getSawSuspiciousHttpResponse()
					&& inoutAuxInfo.getPromoteSuspiciousHttpToException()
			) {
				throw new IOException( kExTag
					+ "Honoring flag for escalation of suspicious HTTP headers: "
					+ inoutAuxInfo.getSuspiciousHttpReason()
					);
			}


			// Now check for redirects
			
	        if( response >= 300 && response < 400 )
	        {
	        	redirCounter++;

	        	debugMsg( kFName, "Requested redirect # " + redirCounter
					+ " of max " + redirMax
					);

	        	if( response != java.net.HttpURLConnection.HTTP_MOVED_PERM
	        		&& response != java.net.HttpURLConnection.HTTP_MOVED_TEMP
	        	) {
	        		warningMsg( kFName, "Non-standard 3xx redirect code " + response );
	        	}
	        		
				// Check redir max, but don't bail just yet
				int maxRedirs = null==inoutAuxInfo
					? AuxIOInfo.DEFAULT_MAX_REDIRECTS
					: inoutAuxInfo.getMaxDesiredRedirectLevels()
					;

				boolean shouldDoRedir = redirCounter <= maxRedirs;
				// statusMsg( kFName, "shouldDoRedir="+shouldDoRedir + " redirCounter="+redirCounter + " maxRedirs="+maxRedirs );
				
	        	String location = castConn.getHeaderField("Location");
	        	URL newURL = null;
	        	if( location.startsWith("http:") || location.startsWith("https:") )
	        	{
	        		newURL = new URL(location);
	        	}
	        	else if (location.startsWith("/"))
	        	{
	        		warningMsg( kFName, "Non-Absolute URL (missing server/port, DOES have absolute path)"
	        				+ " Assumes same machine, http protocol and default port."
	        				);
	        		newURL = new URL("http://" + newURL.getHost() + location );
	        	}
	        	else {
	        		warningMsg( kFName, "Relative URL, using original URL as base." );
	        		newURL = new URL( castConn.getURL(), location );
	        	}

	        	// Safe URL Logic, escape spaces, etc
	        	if( newURL.toExternalForm().indexOf(' ') >= 0 )
	        	{
	        		debugMsg( kFName, "Space detected in destination URL." );
	        		if( (null==inoutAuxInfo && AuxIOInfo.DEFAULT_USE_CAREFUL_REDIRECTS)
	        			|| ( null!=inoutAuxInfo && inoutAuxInfo.getUseCarefulRedirects() )
	        		) {
	        			String tmpUrl = newURL.toExternalForm();
	        			tmpUrl = NIEUtil.replaceChars( tmpUrl, ' ', '+' );
	        			newURL = new URL( tmpUrl );
	        		}
	        		else {
		        		warningMsg( kFName, "Space detected in redirect destination URL and NOT configured to escape it."
		        			+ " URL=" + newURL
		        			);       			
	        		} 		
	        	}
	        	// done with spaces

	        	// Record the redirect
	        	if( null!=inoutAuxInfo && inoutAuxInfo.getShouldLogRedirects() )
	        	{
	        		String msg = shouldDoRedir ? null : "Past max redirs, curr=" + redirCounter + ", max=" + maxRedirs ;
	        		inoutAuxInfo.recordRedirect(
	        			currURL.toExternalForm(), newURL.toExternalForm(),
	        			response, shouldDoRedir, msg
	        			);
	        	}

	        	// Escape at this point
	        	if( ! shouldDoRedir )
	        	{
	        		debugMsg( kFName, "Should not do this redir, breaking out."
	        			+ " shouldDoRedir="+shouldDoRedir + " redirCounter="+redirCounter
	        			+ " maxRedirs="+maxRedirs
	        			);
	        		break;
	        	}
	        	
	        	// Prep headers

	    		// Now open the new URL
    			lURLConnection = newURL.openConnection();
	    		// There is a difference between openConnection() and connect()
	    		castConn = (HttpURLConnection) lURLConnection;
	    		castConn.setInstanceFollowRedirects( false );
		
	    		// Setup the user agent field
	    		if( localClientString != null && inoutAuxInfo == null )
	    		{
	    			debugMsg( kFName,
	    				"Setting request property for user agent field."
	    				);
	    			lURLConnection.setRequestProperty(
	    				AuxIOInfo.HTTP_USER_AGENT_FIELD_SPELLING,
	    				localClientString
	    				);
	    		}

	    		// If we have a hash of HTTP info, add it in
	    		if( inoutAuxInfo != null )
	    		{
	    			debugMsg( kFName,
	    				"Setting addtional HTTP header fields FOR REDIR from auxio object."
	    				+ " Use trace mode to see details."
	    				);
	    			List keys = inoutAuxInfo.getHTTPFieldFieldKeys();
	    			for( Iterator it = keys.iterator(); it.hasNext(); )
	    			{
	    				// Get the key and value
	    				String key = (String)it.next();
	    				String value = inoutAuxInfo.getScalarHTTPFieldTrimOrNull( key );
	    				// Add if we got a good value
	    				if( value != null )
	    				{
	    					traceMsg( kFName,
	    						"HTTP header field (2): \"" + key + "\"=\"" + value + "\""
	    						);
	    					lURLConnection.setRequestProperty(
	    						key,
	    						value
	    						);
	    				}
	    			}
	    		}	// End if we had a Hash of HTTP info

				// THIS will actually invoke the connection
				lURLConnection.connect();

				// Then let it go back to the top of the loop
	        
	        
	        }
	        // Else not a redirect, bail out
	        else {
	        	break;
	        }
		}
         


		// Were we redirected?
		// OBSESS about what we might have wound up with
		// The Net claims URLConnection's getURL() will give the final
		// URL that was used PROVIDED some intervening calls are made that
		// actually estblish and fetch content or headers
		// We have done that by now by getting the headers
		URL tmpURL = lURLConnection.getURL();
		String finalURLString =  null;
		if( tmpURL != null )
		{
			finalURLString = tmpURL.toExternalForm();
			finalURLString = trimmedStringOrNull( finalURLString );

			// Did it change?  Did we get a redirect?
			if( finalURLString != null && ! finalURLString.equals( theURLString ) )
			{
				// Todo: Not sure this is the right thing to do?
				// Issues:
				// 1: The URL's might be different just because of CGI variables.
				//	Currently we grab it before we add on our CGI vars, assuming you
				//	wouldn't want all that lengthy BS for a context URL.
				//	Or do you?
				// 2: If we got a redirect, should we use the new URL or the old
				//	URL as the base for any subsequent relative references?
				//	That might even depend on which URL redirect code was issued.

				// Chop them both down
				// The original one
				String urlStr1 = theURLString;
				int at1 = urlStr1.indexOf( '?' );
				if( at1 > 0 )
					urlStr1 = urlStr1.substring( 0, at1 );
				String urlStr2 = finalURLString;
				int at2 = urlStr2.indexOf( '?' );
				if( at2 > 0 )
					urlStr2 = urlStr2.substring( 0, at2 );

				// NOW see if they're the same
				if( ! urlStr1.equals( urlStr2 ) )
				{
					// If it's just a trailing slash, don't warn
					if( urlStr1.length() == urlStr2.length()-1
						&& urlStr2.endsWith( "/" )
						)
					{
						// It's just a trailing slash
						// like http://www.kfu.com/~mbennett
						//   to http://www.kfu.com/~mbennett/
						if(debug) debugMsg( kFName, "New URL just has a trailing slash." );
					}
					else
					{
						infoMsg( kFName,
							"The final URL we read from seems to be different"
							+ " than what we started with."
							+ " This may be OK, perhaps it's the result of redirects."
							+ " Orginal URL (minus any CGI) = \"" + urlStr1 + "\""
							+ " Final URL (minus any CGI) = \"" + urlStr2 + "\""
							);
					}
					// Record the URL we will actually got back
					if( inoutAuxInfo != null )
					{
						// Technically we should obsess about WHICH status code we got back
						// But we don't have access to that, if Java has done the
						// iterations for us, this is the last URL's status, not the first
						// Most of the time it's 301
						if(debug) debugMsg( kFName, "Overwriting final URL in Aux IO Object." );
						inoutAuxInfo.setFinalURI( urlStr2 );
						// actually the URL we ATTEMPTED is still what it was before
						// inoutAuxInfo.outAttemptedURI = urlStr2;
					}
				}	// End if they still match after dropping the CGI B.S.

			}	// End if we got a string from the URL and it didn't match the orig URL
		}	// End if we got back a URL Object from the connection

		// Track Content Type / Mime Type and Character encoding
		if( null != inoutAuxInfo )
		{
			String rawContentType = lURLConnection.getContentType();
			String normContentLine = NIEUtil.trimmedLowerStringOrNull( rawContentType );
			if(debug) debugMsg( kFName, "rawContentType='" + rawContentType + "'" );
			if( null!=normContentLine )
			{
				String contentType = null;
				String encoding = null;
				
				// We need to look at each subfield
				// Normally the first one is content/type
				// and the second is charaset
				// but subfields after the first one are named and there
				// can be more than one
				StringTokenizer st = new StringTokenizer( normContentLine, ";" );
				boolean haveSeenFirst = false;
				// For each subfield
				while( st.hasMoreTokens() )
				{
					String item = st.nextToken();
					item = NIEUtil.trimmedStringOrNull( item );
					// Skip empties
					if( null==item )
					{
						haveSeenFirst = true;
						continue;
					}
					// First in list has no field name and is always mime type
					if( ! haveSeenFirst )
					{
						contentType = item;
						haveSeenFirst = true;
					}
					// Else it's a subsequent field denoted by field name
					else {
					
						int equalsAt = item.indexOf('=');
						String key = null;
						String value = null;
						// No equals or right at the start
						if( equalsAt <= 0 )
						{
							warningMsg( kFName, "Uknown http content-type subfield key, no/misplaced equals sign"
								+ ", item='" + item + "'"
								+ ", parsed from http header line: '" + rawContentType + "'"
								+ " For original URL '" + theURLString + "'"
								+ ", final URL '" + finalURLString + "'."
								);
						}
						// good so far
						else
						{
							// get the key
							key = NIEUtil.trimmedStringOrNull( item.substring(0,equalsAt) );
							// Double check, this is really important
							if( null==key )
							{
								warningMsg( kFName, "Empty http content-type subfield key, trimmed to null"
									+ ", item='" + item + "'"
									+ ", parsed from http header line: '" + rawContentType + "'"
									+ " For original URL '" + theURLString + "'"
									+ ", final URL '" + finalURLString + "'."
									);								
							}
							// And the value, make sure there's something there
							else if( equalsAt<(item.length()-1) )
							{
								value = NIEUtil.trimmedStringOrNull( item.substring(equalsAt+1) );
								if( null==value )
								{
									warningMsg( kFName, "Empty http content-type subfield valued, trimmed to null"
										+ ", item='" + item + "'"
										+ ", parsed from http header line: '" + rawContentType + "'"
										+ " For original URL '" + theURLString + "'"
										+ ", final URL '" + finalURLString + "'."
										);								
								}
								// Else by now we have a key and a value
								else if( key.equals("charset") )
								{
									encoding = value;
								}
								// Unrecognized field
								else
								{
									warningMsg( kFName, "Unusual http content-type subfield name '" + key + "'"
										+ ", from item='" + item + "', ignoring."
										+ "Parsed from http header line: '" + rawContentType + "'"
										+ " For original URL '" + theURLString + "'"
										+ ", final URL '" + finalURLString + "'."
										);																
								}
							}
							// Else there is an equals sign but it's at the very end, so invalid
							else
							{
								warningMsg( kFName, "Empty http content-type subfield value, misplaced equals sign"
									+ ", item='" + item + "'"
									+ ", parsed from http header line: '" + rawContentType + "'"
									+ " For original URL '" + theURLString + "'"
									+ ", final URL '" + finalURLString + "'."
									);
							}
						}	// End else does have equals sign, and not at start
					}	// End else this is NOT the first subfield
				}	// End for each subfield

				// Store the results of what we've found
				if( null!=contentType )
					inoutAuxInfo.setContentType( contentType );
				else
					warningMsg( kFName, "No mime type parsed from http header line: '" + rawContentType + "'"
						+ " For original URL '" + theURLString + "'"
						+ ", final URL '" + finalURLString + "'."
						);				
				if( null!=encoding )
					inoutAuxInfo.setEncoding( encoding );
				else
					warningMsg( kFName, "No encoding parsed from http header line: '" + rawContentType + "'"
						+ " For orig URL '" + theURLString + "'"
						+ ", final URL '" + finalURLString + "'."
						);				

				if(debug) debugMsg( kFName, "Separated contentType='" + contentType + "', encoding='" + encoding + "'" );
			
			}
			// Else not there
			else {
				warningMsg( kFName, "Null/empty http header content-type line."
					+ " For orig URL '" + theURLString + "'"
					+ ", final URL '" + finalURLString + "'."
					);				
			}
		}

		if(debug) debugMsg( kFName,
			"Done reading header lines, will now call .getInputStream()"
			);

		//InputStream lStream = lURL.openStream();
		InputStream returnStream = lURLConnection.getInputStream();

		if(debug)
		{
			if( null!=inoutAuxInfo
					&& inoutAuxInfo.getUseCarefulRedirects()
					&& inoutAuxInfo.getFollowRedirects()
			) {
				String redirs = inoutAuxInfo.getRedirectsSoFarAsStringOrNull();
				debugMsg( kFName, "Redirects: " + redirs );
			}
			debugMsg( kFName,
				"At end, returning stream."
				);
		}
			
		return returnStream;

	}




//	// Obs?
//	// Download the actual content, return it as a string
//	public static String fetchCharContentFromURI( String inFetchURI,
//		String optURIRelativeBase,
//		String optUsername,
//		String optPassword
//		)
//		throws Exception
//	{
//
//		final boolean debug = false;
//
//		InputStream lStream = lURLConnection.getInputStream();
//		StringBuffer strBuff = new StringBuffer();
//
//		int c;
//		while( (c=lStream.read()) != -1 )
//		{
//			strBuff.append( (char)c );
//		}
//
//		lStream.close();
//		lStream = null;
//		lURLConnection = null;
//
//		return new String( strBuff );
//
//	}

	private static void __Writing_Files__() {}

	public static void writeBinaryFile( File inFileObj, byte [] inContent )
		throws IOException
	{
		if( null==inFileObj )
			throw new IOException( "Null file object passed in." );
		if( null==inContent )
			throw new IOException( "Null content passed in." );

		OutputStream fout = new FileOutputStream( inFileObj );
		fout.write( inContent );
		fout.close();

	}

	// Wrapper for PRE 2.9 style calls
	public static void writeCharFile( File inFileObj, String inContent )
		throws IOException
	{
		String encoding = calculateEncoding( inFileObj.toString(), null, null );
		writeCharFile( inFileObj, inContent, encoding );
	}
	public static void writeCharFile( File inFileObj, String inContent, AuxIOInfo ioInfo )
		throws IOException
	{
		String encoding = calculateEncoding( inFileObj.toString(), null, ioInfo );
		writeCharFile( inFileObj, inContent, encoding );
	}

	// Updated for Java 1.4 / SearchTrack 2.9xc to REQUIRE encoding
	public static void writeCharFile( File inFileObj, String inContent, String encoding )
		throws IOException
	{
		if( null==inFileObj )
			throw new IOException( "Null file object passed in." );
		if( null==inContent )
			throw new IOException( "Null content passed in." );
		if( null==encoding )
			throw new IOException( "Null encoding passed in." );

		// DANGER: This version let the system guess what encoding to use
		// FileWriter fout = new FileWriter( inFileObj );
		// NO! FileWriter fout = new FileWriter( inFileObj, encoding );

		// OutputStreamWriter(OutputStream out, String charsetName) 
		// java.nio.charset.Charset.forName( encoding )
		BufferedWriter fout = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(inFileObj), encoding ));
	
		fout.write( inContent );
		fout.close();
	}


	public static boolean clearDirectoryContents( File inDir /*, String optPattern*/ ) {
		final String kFName = "clearDirectoryContents";
		if( null==inDir ) {
			errorMsg( kFName, "Null directory passed in." );
			return false;
		}
		if( ! inDir.exists() ) {
			errorMsg( kFName, "No such directory: \"" + inDir + "\"" );
			return false;
		}
		if( ! inDir.isDirectory() ) {
			errorMsg( kFName, "Not a directory: \"" + inDir + "\"" );
			return false;
		}
		if( ! inDir.canRead() ) {
			errorMsg( kFName, "Can't scan directory: \"" + inDir + "\"" );
			return false;
		}
		if( ! inDir.canWrite() ) {
			errorMsg( kFName, "Can't modify directory: \"" + inDir + "\"" );
			return false;
		}
		String fileNames[] = null;
		boolean hadAProblem = false;
		// if( null!=optPattern )
		//	fileNames = inDir.list( new FilenameFilter(optPattern) );
		// else
		//	fileNames = inDir.list();
		File files[] = inDir.listFiles();
		// if( null!=fileNames ) {
		//   for( int i=0; i<fileNames.length; i++ ) {
		if( null!=files ) {
			for( int i=0; i<files.length; i++ ) {
				// File targetFile = new File( inDir, fileNames[i] );
				// if( ! targetFile.delete() ) {
				if( ! files[i].delete() ) {
					errorMsg( kFName, "File not deleted: " + files[i] );
					hadAProblem = true;
				}
			}
		}
		else {
			errorMsg( kFName, "Problem scanning directory: \"" + inDir + "\"" );
			hadAProblem = true;
		}
		return ! hadAProblem;
	}

	private static void __CGI_and_HTTP_Stuff__() {}

	// Read in an HTTP style request, return an AuxIOInfo structure
	// with the fully parsed request.
	// Pass in an input stream.
	// By default we close it when we're done.
	// This one gets a stream from the socket and then calls
	// the readHTTPRequest with that stream
	public static AuxIOInfo readHTTPRequestFromSocket(
		Socket inSocket, boolean closeSocketWhenDone,
		boolean inIsCasenCGI
		)
			throws SpuriousHTTPRequestException
	{
		final String kFName = "readHTTPRequestFromSocket";
		boolean debug = shouldDoDebugMsg( kFName );
		// Get an input stream from the Socket
		InputStream lStream = null;
		try {
			lStream = inSocket.getInputStream();
		}
		catch (Exception e) {
			errorMsg( kFName,
				"Unable to get input stream from socket."
				+ " Exception was \"" + e + "\"."
				+ " Nothing to do."
				);
			return null;
		}

		if(debug) debugMsg( kFName, "Calling readHTTPRequestFromStream" );
		AuxIOInfo outInfo = readHTTPRequestFromStream(
			// lStream, true, inIsCasenCGI
			lStream, false, inIsCasenCGI
			);
		// ^^^ OUCH!  For some reason closing the imput stream messes up the output

		if( null!=outInfo )
		{
			if(debug) debugMsg( kFName, "Got back non-null auxio object" );
			// Remember the IP address
			outInfo.setIPAddressFromSocket( inSocket );
	
			if( closeSocketWhenDone ) {
				if(debug) debugMsg( kFName, "Will try to close socket" );
				// Shut down
				try {
					// inSocket.shutdownOutput();
					// inSocket.shutdownInput();
					inSocket.close();
					inSocket = null;	// superstitous
				}
				catch( IOException eIO ) {
					errorMsg( kFName, "Got exception when shutting down socket. Exception=\"" + eIO + "\"." );
				}
			}
			else {
				if(debug) debugMsg( kFName, "Will NOT try to close socket" );
			}
		}
		else
			errorMsg( kFName, "Got back null from readHTTPRequestFromSocket; returning null." );

		return outInfo;
	}

	/***
	public static AuxIOInfo readHTTPRequestFromSocket(
		InputStream inStream
		)
	{
		return readHTTPRequestFromSocket( inStream, true );
	}
	public static AuxIOInfo readHTTPRequestFromSocket(
		InputStream inStream, boolean closeSocketWhenDone
		)
	{
		return readHTTPRequestFromSocket(
			inStream, closeSocketWhenDone, false
			);
	}
	***/
	public static AuxIOInfo readHTTPRequestFromStream(
		InputStream inStream, boolean closeStreamWhenDone,
		boolean inIsCasenCGI
		)
			throws SpuriousHTTPRequestException
	{
		final String kFName = "readHTTPRequestFromStream";

		if( inStream == null )
		{
			errorMsg( kFName,
					"NULL socket passed in, returning null AuxIOInfo."
				);
			return null;
		}

		// allocate an AuxIOInfo structure
		AuxIOInfo outInfo = new AuxIOInfo();

		// Get all the lines from the header, as a simple string
		List lRawLines = readAllHTTPHeaderLinesFromSocket(inStream);
		// Double check what we got back
		if( lRawLines == null || lRawLines.size() < 1 )
		{
			errorMsg( kFName,
					"No HTTP haeader lines found, returning null AuxIOInfo."
				);
			// Were we asked to clean up?
			if( closeStreamWhenDone )
			{
				try { inStream.close(); } catch (Exception e) {};
			}
			return null;
		}
		outInfo.fIoHTTPHeaders = lRawLines;

		// Break apart the first line
		String lFirstLine = (String)lRawLines.get(0);

		// set the request type
		// Will normalize to upper case by default
		String lRequestType = cgiParseRequestTypeFromCGIHeader( lFirstLine );
		outInfo.fIoTransactionType = lRequestType;
		// Sanity check on the request type
		if( lRequestType == null ||
			( ! lRequestType.equals("GET") && ! lRequestType.equals("POST") )
			)
		{
			errorMsg( kFName,
				"Unknown / unhandled / null request type in HTTP header,"
				+ " requested type = '" + lRequestType + "'"
				+ " Currently only support GET and POST."
				+ " WILL attempt to continue, but most of the remaining code"
				+ " is specific to just GET and POST."
				);
		}

		// set the request path
		String lRequestedPath = cgiParsePathSectionFromCGIHeader( lFirstLine );
		// Double check what we got back
		if( lRequestedPath == null || lRawLines.size() < 1 )
		{
			errorMsg( kFName,
					"No requested path found in first line of HTTP haeader"
					+ ", line was '" + lFirstLine + "'."
					+ " This is an INVALID HTTP request, returning null AuxIOInfo."
				);
			// Were we asked to clean up?
			if( closeStreamWhenDone )
			{
				try { inStream.close(); } catch (Exception e) {};
			}
			return null;
		}
		outInfo.fOutRequestedPath = lRequestedPath;

		// parse the headers, store results in outInfo
		parseHTTPHeaderFields( outInfo, lRawLines );
//		// It skips the first line by default
//		Hashtable lHeaderHash = parseHTTPHeaderFields( lRawLines );
//		// It's nice to have at least something in the aux out
//		lHeaderHash = ( lHeaderHash != null ) ? lHeaderHash : new Hashtable();
//		// and then store it
//		outInfo.fIoHTTPHeaderFieldHash = lHeaderHash;


		// read the content
		// and decide on which string buffer to parse
		// Annoint the character buffer that will hold encoded CGI variables
		// We start with an empty buffer, and it's OK if we don't get anything,
		// some valid web requests don't pass in any variables
		String encodedBuffer = "";

		if( lRequestType.equals("POST") )
		{
			// Go for the content length
			// long lContentLength = cgiGetContentLengthFromHTTPHeaderHash( lHeaderHash );
			// long lContentLength = cgiGetContentLengthFromHTTPHeaderHash( outInfo );
			int lContentLength = outInfo.getExpectedContentLength();
			// outInfo.fOutContentLength = lContentLength;
			// Sanity check
			if( lContentLength < 1 )
				warningMsg( kFName,
					"Did not get back a valid content length for a POST."
					+ " Will attempt to read content until EOF"
					+ " but may hang if sender keeps socket open."
					);

			// Read bytes from stream
			// And tell him it's to read to EOF if length < 0
			encodedBuffer = readNBytesFromSocket(
				inStream, lContentLength, true
				);
			if( encodedBuffer != null )
				outInfo.setContent( encodedBuffer );

		}
		// If it's a GET, pull the variables from after the ?
		// in the requested path
		else if( lRequestType.equals("GET") )
		{
			// See if there's a question mark
			int questionMarkAt = lRequestedPath.indexOf( '?' );
			if( questionMarkAt >= 0
				&& questionMarkAt < lRequestedPath.length()-1
				)
			{
				// Grab it
				encodedBuffer = lRequestedPath.substring( questionMarkAt+1 );
			}
		}
		// Else it's not a GET or a POST, but we've already warned
		// them about that above

		// parse and store the variables
		outInfo.setCGIFieldsCaseSensitive( inIsCasenCGI );
		cgiDecodeVarsBuffer( outInfo, encodedBuffer );

//		// parse and store the variables
//		Hashtable cgiVarHash = cgiDecodeVarsBuffer( encodedBuffer );
//		// It's OK if we didn't get any variables, but it's nice
//		// to have a least an empty hash in place
//		cgiVarHash = ( cgiVarHash != null ) ? cgiVarHash : new Hashtable();
//		// store it in the aux object
//		outInfo.fIoCGIFieldHash = cgiVarHash;

		// close the socket unless asked not to do so
		// Were we asked to clean up?
		if( closeStreamWhenDone )
		{
			try { inStream.close(); } catch (Exception e) {};
		}

		// done
		return outInfo;

	}



	// Returns a simple list of header strings, unparsed
	// Reads until it gets a "blank line", which it does not keep
	// Strings will not have their End-Of-Line sequences, they are stripped
	// Handles CR/LF (spec), LF
	// Does NOT handle sole CR End-Of-Line sequences
	// Does NOT close the socket
	// ?? DOES throw IO Exceptions
	// Will break for:
	// simple CR lines (mac software maybe?)
	// LF/CR, which nobody has ever heard of
	// inconsistent LF, CR, CR/LF
	// If you need to handle CR revive one of the uncompleted
	// fancier versions of this routine below, with the OBS1 and OBS2
	// prefixes - OBS2 is probably easier to understand.
	static List readAllHTTPHeaderLinesFromSocket( InputStream inStream )
//		throws IOException
		throws SpuriousHTTPRequestException
	{
		final String kFName = "readAllHTTPHeaderLinesFromSocket";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );
		boolean trace = shouldDoTraceMsg( kFName );

		int charCount = 0;

		if( inStream == null )
		{
//			throw new IOException( "Error:NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//					+ " Null socket passed in, returning null lines list."
//				);
			errorMsg( kFName,
					"Null socket passed in, returning null lines list."
				);
			return null;
		}

		List lLines = new Vector();
		int lineCount = 0;
		StringBuffer lLine = new StringBuffer();
		boolean hasSeenEOF = false;
//		boolean hasSeenBlankLine = false;

		// For each line
		while( true )
		{
			if(trace) traceMsg( kFName, "Top of Read-each-line loop, lineCount=" + lineCount );
			// For each character within the line
			while( true )
			{
				// Get the next character and check if it's EOF
				int lReadInt;
				try
				{
					lReadInt = inStream.read();
					if(trace) traceMsg( kFName, "Read int " + lReadInt );
				}
				catch (IOException e)
				{
					// Special case for spurious requests
					if( lineCount < 1 && lLine.length() < 1 )
					{
						if(debug) debugMsg( kFName, "IO Exception appears to be Spurious read, check 1: zero lines and bytes read so far." );
						throw new SpuriousHTTPRequestException( kExTag +
							"Zero bytes read, spurious request?"
							);
					}
						
					// Another special case
					final String otherBogusError =
						"java.net.SocketException: Connection reset by peer: JVM_recv in socket input stream read"
						;
					if( e.toString().indexOf(otherBogusError) >=0 )
					{
						if(debug) debugMsg( kFName, "IO Exception appears to be Spurious read, check 2: JVM_recv connection reset." );
						throw new SpuriousHTTPRequestException( kExTag +
							"JVM_recv connection reset, spurious request?"
							);
					}
						
					// Else finish up gracefully
					errorMsg( kFName,
						"Got exception on socket/stream read, treating as EOF."
						+ " Previous lines read: " + lineCount
						+ ", Buffer(" + lLine.length() + ")=\"" + new String(lLine) + "\""
						+ " Exception was: '" + e + "'."
						);
					if(debug) debugMsg( kFName, "Setting read int TO EOF" );

					lReadInt = -1;
				}

				// if( lReadInt == -1 )
				if( lReadInt < 0 )
				{
					if(debug) debugMsg( kFName, "Have read EOF, marking have seen EOF" );
					hasSeenEOF = true;
					break;
				}

				// Total chars read
				charCount++;

				// Check if we read a possible EOL
				if( lReadInt == '\r' )
				{
					if(trace) traceMsg( kFName, "Read CR, ignoring" );
					// Just throw it away and continue
					continue;
				}

				// Check if we read a possible EOL
				if( lReadInt == '\n' )
				{
					if(trace) traceMsg( kFName, "Read NL, breaking inner loop" );
					// Let the logic at the end of the innnter for
					// loop handle it
					break;
				}

				// Else it's a regular character
				// TODO use a character buffer
				lLine.append( (char)lReadInt );

			}   // End for each character in a line

			if(debug) debugMsg( kFName, "Done reading line, length = " + lLine.length() );

			// Did we have anything to worry about?
			if( lLine.length() > 0 )
			{
				lLines.add( new String(lLine) );
				lineCount++;
				// Reset the line buffer
				lLine = new StringBuffer();
			}
			else
			{
				// Blank line, we're done
				if(debug) debugMsg( kFName, "Have seen Blank line, breaking main loop" );
				break;
			}

			if( hasSeenEOF )
			{
				if(debug) debugMsg( kFName, "Have seen EOF, breaking main loop" );
				break;
			}

		}   // End outer for each line in header

		if( charCount <= 0 )
		{
			if(debug) debugMsg( kFName, "Appears to be Spurious read, check 3: zero header characters read." );
			throw new SpuriousHTTPRequestException( kExTag +
				"Zero header characters read, spurious request?"
				);
		}

		return lLines;

	}


	// Returns a simple list of header strings, unparsed
	// Reads until it gets a "blank line", which it does not keep
	// Strings will not have their End-Of-Line sequences, they are stripped
	// Handles CR/LF (spec), LF, or CR End-Of-Line sequences
	// Does NOT close the socket
	// DOES throw IO Exceptions
	// Will break for:
	// LF/CR, which nobody has ever heard of
	// inconsistent LF, CR, CR/LF
	// I know the logic seems overly convoluted.... but trying to handle
	// all cases correctly
	static List OBS2readAllHTTPHeaderLinesFromSocket( InputStream inStream )
//		throws IOException
	{
//		if( inStream == null )
//		{
//			throw new IOException( "Error:NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//					+ " Null socket passed in, returning null lines list."
//				);
//		}

//		lLines = new Vector();

		// while true for lines
			// while true within a line
				// zero out eol count
				// lasteolchar = null
				// read a char
				// if eof
					// set have seen eof
					// break
				// if eol char
					// increment eol count for this line
					// if eol char == last eol char
						// set blank line
						// break
					// not have completed first line
						// set flag am processing end of first line
						// increment expected eol count
						// if eol count > 2
							// set blank line
							// read one more char, better be a newline
							// break
						// if buffer not null
							// break (and will be nulled out)
					// else not first line
						// if greater than expected eol count
							// warn?
							// set blank line
							// count = count - expected eol count
							// if count < expected eol count
								// read one more char
							// break
				// if char
					// add to line





//		String lLine = "";
//		boolean hasCR = false;
//		boolean hasLF = false;
//		boolean hasSeenFirstLine = false;
//		boolean atEndOfFirstLine = false;
//		boolean hasSeenBlankLine = false;
//		boolean haveSeenEOF = false;
		// this one starts out as TRUE and is set to false
		// when we read a character
//		boolean atEndOfLine = true;

		// a char we may have read and would like to "push back"
//		int reserveChar = -2;

		// For each line
//		while( true )
//		{
//
//			// For each character within the line
//			while( true )
//			{
//				/////
//				// Get the next character and check if it's EOS
//				/////
//
//				int lReadInt;
//				if( reseveChar != -2 )
//				{
//					lReadInt = reseveChar;
//					reserveChar = -2;
//				}
//				else
//				{
//					fInputStream.read();
//				}
//
//				if( lReadInt == -1 )
//				{
//					hasSeenEOF = true;
//					break;
//				}
//
//				// ^^^^ similar logic below as well in CR section
//
//				/////
//				// Check if we read a possible EOL
//				/////
//
//				if( lReadInt == '\r' )
//				{
//					/////
//					// Read new line in - check if this is a
//					// continuation of a CRLF EOL
//					/////
//
//					if( ! hasSeenFirstLine )
//					{
//						//hasSeenFirstLine = true;
//						if( ! hasCR )
//						{
//							atEndOfFirstLine = true;
//							hasCR = true;
//						}
//						else  // We've just already seen a CR, so now have 2
//						{
//							hasSeenBlankLine = true;
//							break;
//						}
//					}
//					else    // We're past the end of the first line
//					{
//						// we can just throw them away
//						// If the first line didn't end with one, we shouldn't
//						// be seeing them at all
//						if( ! hasCR )
//						{
//							System.err.p rintln(
//								"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//								+ " encountered CR, which is inconsistent with the first header line"."
//								+ ", will ignore it"
//								);
//						}
//
//						// Since this is not the first line, we already know
//						// whether there should or should not be a LF
//						// So go ahead and grab it now
//
//						if( hasLF )
//						{
//							// Get the next character
//							if( reseveChar != -2 )
//							{
//								lReadInt = reseveChar;
//								reserveChar = -2;
//							}
//							else
//							{
//								fInputStream.read();
//							}
//
//							if( lReadInt == -1 )
//							{
//								hasSeenEOF = true;
//								System.err.p rintln(
//									"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//									+ " got EOF instead of LF after CR, which is inconsistent with the first header line"."
//									+ ", will ignore this and finish."
//									);
//								break;
//							}
//							else if( lReadInt == '\n' )
//							{
//								// This is what we expcted, just throw it away
//								break;
//							}
//							else    // Else we got some other weird character?
//							{
//								// OK, shove it back into the stream
//								reserveChar = lReadInt;
//								// Complain about it
//								System.err.p rintln(
//									"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//									+ " got unexpected char instead of LF after CR, which is inconsistent with the first header line"."
//									+ ", char(int) was '" + lReadInt + "'"
//									+ ", will ignore this and proceed with this character."
//									);
//								// and we still want to break out for now
//								// to finish up the current line
//								break;
//							}
//
//						}   // End if has LF
//
//
//
//					}
//				}
//				// Else is it a line feed
//				else if( lReadInt == '\n' )
//				{
//
//					if( ! hasSeenFirstLine )
//					{
//						//hasSeenFirstLine = true;
//						if( ! hasLF )
//						{
//							atEndOfFirstLine = true;
//							hasLF = true;
//						}
//						else  // We've just already seen a LF, so now have 2
//						{
//							hasSeenBlankLine = true;
//							break;
//						}
//					}
//					else    // We're past the end of the first line
//					{
//						// we can just throw them away
//						// If the first line didn't end with one, we shouldn't
//						// be seeing them at all
//						if( ! hasLF )
//						{
//							System.err.p rintln(
//								"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//								+ " encountered LF, which is inconsistent with the first header line"."
//								+ ", will ignore it"
//								);
//						}
//
//						break;
//					}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//					fReadCR = true;
//					return lLine;
//				}
//				else
//				{
//					fReadCR = false;
//					lLine += (char)lReadInt;
//				}
//			}  // End while true
//
//
//			// are we at the end of a line
//			if( atEndOfLine )
//			{
//				if( lLine.equals("") || lLine == null )
//				{
//					hasSeenBlankLine = true;
//					break;
//				}
//				else
//				{
//					lLines.add( lLine );
//					lLine = "";
//					haveSeenFirstLine = true;
//				}
//				// setting atEndOfLine to false requires that we read
//				// another character
//			}
//
//			if( haveSeenEOF )
//				break;
//
//
//		}  // End for each line

		// return results
		// set the request type
		// set the request path
		// set the bare request path?
		// read the headers
		// parse the headers and get the content length, if any
		// read the content
		// decide on which string buffer to parse
		// parse and store the variables
		// done
		return null;
	}







	static List OBS1readAllHTTPHeaderLinesFromSocket( InputStream inStream )
		throws IOException
	{
//		if( inStream == null )
//		{
//			throw new IOException( "Error:NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//					+ " Null socket passed in, returning null lines list."
//				);
//		}
//
//		lLines = new Vector();
//
//		String lLine = "";
//		boolean hasCR = false;
//		boolean hasLF = false;
//		boolean hasSeenFirstLine = false;
//		boolean atEndOfFirstLine = false;
//		boolean hasSeenBlankLine = false;
//		boolean haveSeenEOF = false;
//		// this one starts out as TRUE and is set to false
//		// when we read a character
//		boolean atEndOfLine = true;
//
//		// a char we may have read and would like to "push back"
//		int reserveChar = -2;
//
//		// For each line
//		while( true )
//		{
//
//			// For each character within the line
//			while( true )
//			{
//				/////
//				// Get the next character and check if it's EOS
//				/////
//
//				int lReadInt;
//				if( reseveChar != -2 )
//				{
//					lReadInt = reseveChar;
//					reserveChar = -2;
//				}
//				else
//				{
//					fInputStream.read();
//				}
//
//				if( lReadInt == -1 )
//				{
//					hasSeenEOF = true;
//					break;
//				}
//
//				// ^^^^ similar logic below as well in CR section
//
//				/////
//				// Check if we read a possible EOL
//				/////
//
//				if( lReadInt == '\r' )
//				{
//					/////
//					// Read new line in - check if this is a
//					// continuation of a CRLF EOL
//					/////
//
//					if( ! hasSeenFirstLine )
//					{
//						//hasSeenFirstLine = true;
//						if( ! hasCR )
//						{
//							atEndOfFirstLine = true;
//							hasCR = true;
//						}
//						else  // We've just already seen a CR, so now have 2
//						{
//							hasSeenBlankLine = true;
//							break;
//						}
//					}
//					else    // We're past the end of the first line
//					{
//						// we can just throw them away
//						// If the first line didn't end with one, we shouldn't
//						// be seeing them at all
//						if( ! hasCR )
//						{
//							System.err.p rintln(
//								"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//								+ " encountered CR, which is inconsistent with the first header line"."
//								+ ", will ignore it"
//								);
//						}
//
//						// Since this is not the first line, we already know
//						// whether there should or should not be a LF
//						// So go ahead and grab it now
//
//						if( hasLF )
//						{
//							// Get the next character
//							if( reseveChar != -2 )
//							{
//								lReadInt = reseveChar;
//								reserveChar = -2;
//							}
//							else
//							{
//								fInputStream.read();
//							}
//
//							if( lReadInt == -1 )
//							{
//								hasSeenEOF = true;
//								System.err.p rintln(
//									"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//									+ " got EOF instead of LF after CR, which is inconsistent with the first header line"."
//									+ ", will ignore this and finish."
//									);
//								break;
//							}
//							else if( lReadInt == '\n' )
//							{
//								// This is what we expcted, just throw it away
//								break;
//							}
//							else    // Else we got some other weird character?
//							{
//								// OK, shove it back into the stream
//								reserveChar = lReadInt;
//								// Complain about it
//								System.err.p rintln(
//									"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//									+ " got unexpected char instead of LF after CR, which is inconsistent with the first header line"."
//									+ ", char(int) was '" + lReadInt + "'"
//									+ ", will ignore this and proceed with this character."
//									);
//								// and we still want to break out for now
//								// to finish up the current line
//								break;
//							}
//
//						}   // End if has LF
//
//
//
//					}
//				}
//				// Else is it a line feed
//				else if( lReadInt == '\n' )
//				{
//
//					if( ! hasSeenFirstLine )
//					{
//						//hasSeenFirstLine = true;
//						if( ! hasLF )
//						{
//							atEndOfFirstLine = true;
//							hasLF = true;
//						}
//						else  // We've just already seen a LF, so now have 2
//						{
//							hasSeenBlankLine = true;
//							break;
//						}
//					}
//					else    // We're past the end of the first line
//					{
//						// we can just throw them away
//						// If the first line didn't end with one, we shouldn't
//						// be seeing them at all
//						if( ! hasLF )
//						{
//							System.err.p rintln(
//								"Warning: NIEUtil:readAllHTTPHeaderLinesFromSocket:"
//								+ " encountered LF, which is inconsistent with the first header line"."
//								+ ", will ignore it"
//								);
//						}
//
//						break;
//					}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//					fReadCR = true;
//					return lLine;
//				}
//				else
//				{
//					fReadCR = false;
//					lLine += (char)lReadInt;
//				}
//			}  // End while true
//
//
//			// are we at the end of a line
//			if( atEndOfLine )
//			{
//				if( lLine.equals("") || lLine == null )
//				{
//					hasSeenBlankLine = true;
//					break;
//				}
//				else
//				{
//					lLines.add( lLine );
//					lLine = "";
//					haveSeenFirstLine = true;
//				}
//				// setting atEndOfLine to false requires that we read
//				// another character
//			}
//
//			if( haveSeenEOF )
//				break;
//
//
//		}  // End for each line
//
//		// return results
//		// set the request type
//		// set the request path
//		// set the bare request path?
//		// read the headers
//		// parse the headers and get the content length, if any
//		// read the content
//		// decide on which string buffer to parse
//		// parse and store the variables
//		// done
		return null;
	}


	// Set the global authentication stuff
	// Todo: Revisit, how to set login info for each job?
	// Maybe check Java 1.4
	static void setupGlobalHTTPAuthentication( String username, String password )
	{
		final String kFName = "setupGlobalHTTPAuthentication";

		// If they're both null then never mind
		if( username == null && password == null )
		{
			errorMsg( kFName,
				"Both parameters are null?"
				+ " Not doing anything."
				);
			return;
		}

		// If only one of the two is missing, set the other to ""
		if( username == null )
		{
			warningMsg( kFName,
				"password set but no username"
				+ ", setting username to empty string, hope that's OK"
				);
			username = "";
		}
		if( password == null )
		{
			warningMsg( kFName,
				"username set but no password"
				+ ", setting password to empty string, hope that's OK"
				);
			username = "";
		}

		// Install Authenticator
//		Authenticator.setDefault(
//			new NIEAuthenticator( username, password )
//			);
		//Authenticator tmpAuth;
		NIEAuthenticator2 tmpAuth;
		// test tmpTest;
		traceMsg( kFName, "username=" + username + ", password=" + password );
		//String tmpString = new String( "foo" );
		// tmpAuth = new NIEAuthenticator( username, password );
		// tmpTest = new test();
		tmpAuth = new NIEAuthenticator2( "foo", "bar" );
		//Authenticator.setDefault( tmpAuth );

	}

	public static String cgiParseRequestTypeFromCGIHeader(
		String inHeaderLine
		)
	{
		// so by default we normalize the request to upper case
		return cgiParseRequestTypeFromCGIHeader( inHeaderLine, true );
	}
	public static String cgiParseRequestTypeFromCGIHeader(
		String inHeaderLine, boolean inForceToUpperCase
		)
	{
		final String kFName = "cgiParseRequestTypeFromCGIHeader";

		if( inHeaderLine == null )
		{
			errorMsg( kFName,
				"Was passed a NULL header line."
				);
			return null;
		}
		int firstSpace = inHeaderLine.indexOf(' ');

		if( firstSpace < 2 )
		{
			errorMsg( kFName,
				"Malformed header line."
				+ " Should be of the general form METHOD /path HTTP/version"
				+ " where METHOD is something like GET or POST."
				+ " Header Line: \"" + inHeaderLine + "\""
				);
			return null;
		}

		String outType = inHeaderLine.substring( 0, firstSpace );
		if( inForceToUpperCase )
			outType = outType.toUpperCase();
		return outType;
	}

	// Return the business section of a GET request
	public static String cgiParsePathSectionFromCGIHeader(
		String headerLine
		)
	{
		final String kFName = "cgiParsePathSectionFromCGIHeader";

		if( headerLine == null )
		{
			errorMsg( kFName,
				"Was passed a NULL header line."
				);
			return null;
		}
		int firstSpace = headerLine.indexOf(' ');
		int secondSpace = -1;
		if( firstSpace >= 0 )
		{
			secondSpace = headerLine.indexOf( ' ', firstSpace+1 );
		}
		int length = firstSpace>=0 && secondSpace>=0
			? secondSpace - firstSpace - 1 : -1;
		if( firstSpace < 2 || secondSpace < 0 || length < 1 )
		{
			errorMsg( kFName,
				"Malformed header line."
				+ " Should be of the general form METHOD /path HTTP/version"
				+ " where METHOD is something like GET or POST."
				+ " Header Line: \"" + headerLine + "\""
				);
			return null;

		}

		String outPath = headerLine.substring( firstSpace+1, secondSpace );
		return outPath;
	}

	// Given a hash of HTTP header variables, see if we can
	// find the content length.  By default we don't warn
	// if we don't get it.
	// We DO warn, no matter what, if the field is present but is invalid.
	// If absent we return -1
	// We'll look for an accept 3 variations in case:
	//  Content-Length, content-length and CONTENT-LENGTH
	// By default, HTTP headers are normalized to lower case anyway, so
	// unless you've messed with that, it should be caught correctly.
	static long cgiGetContentLengthFromHTTPHeaderHash( AuxIOInfo inAuxInfo )
	{
		return cgiGetContentLengthFromHTTPHeaderHash( inAuxInfo, false );
	}
	static long cgiGetContentLengthFromHTTPHeaderHash( AuxIOInfo inAuxInfo,
		boolean inWarnIfMissing
		)
	{
		final String kFName = "cgiGetContentLengthFromHTTPHeaderHash";

		long outLength = -1;
		if( inAuxInfo == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL Aux IO object."
				+ " Will return -1."
				);
			return outLength;
		}
		// The proper mixed case version of the string
		String targetString = "Content-Length";
		// Look for the proper mixed case version
		String valueString = inAuxInfo.getScalarHTTPField( targetString );
		// If null, try the lower case version
		if( valueString == null )
		{
			valueString = inAuxInfo.getScalarHTTPField(
				targetString.toLowerCase()
				);
		}
		// If still null, try the upper case version
		if( valueString == null )
		{
			valueString = inAuxInfo.getScalarHTTPField(
				targetString.toUpperCase()
				);
		}


		// Now do a sanity check on the string, did we find it?
		if( valueString == null || valueString.trim().equals("") )
		{
			// We didn't find it
			if( inWarnIfMissing )
			{
				warningMsg( kFName,
					"We did not find Content-Length in the HTTP headers"
					+ " or it was an empty string."
					+ " Programmers can suppress this warning with inWarnIfMissing=false."
					+ " Will return -1."
					);
			}
			return outLength;
		}   // End if we didn't find the field

		// Now let's try to convert it to a long
		valueString = valueString.trim();
		try
		{
			outLength = Long.parseLong( valueString );
		}
		catch (NumberFormatException e)
		{
			outLength = -1;
			errorMsg( kFName,
				"Could not parse field Content-Length in the HTTP headers."
				+ " String was: '" + valueString + "'"
				+ ", Exception was '" + e + "'."
				+ " Will return -1."
				);
		}

		return outLength;

	}

	// Given a vector of strings, convert it into a hash
	// by default we normalize everything to lower case
	// and only keep the first header, and by default we skip
	// the very first line which mormally doesn't have fields
	public static void parseHTTPHeaderFields( AuxIOInfo inAux, List inHeaderLines )
	{
		// return parseHTTPHeaderFields( inAux, inHeaderLines, true, true, true );
		parseHTTPHeaderFields( inAux, inHeaderLines, true );
	}
	public static void parseHTTPHeaderFields(
		AuxIOInfo inAux, List inHeaderLines,
		boolean inSkipFirstLine
		//, boolean inForceToLowerCase,	boolean inKeepOnlyFirstInstance
		)
	{
		final String kFName = "parseHTTPHeaderFields";

		if( inAux == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL aux info object."
				+ " No where to store fields."
				);
			return;
		}
		if( inHeaderLines == null )
		{
			errorMsg( kFName,
				"Was passed in a NULL list of header lines."
				+ " Nothing to parse."
				);
			return;
		}

		if( inHeaderLines.size() < 1 && inSkipFirstLine )
		{
			errorMsg( kFName,
				"Was passed in a zero length list of header lines"
				+ " and inSkipFirstLine was set"
				+ ", so there should have been at least that one line."
				+ " Invalid headers, nothing to parse."
				);
			return;
		}

		// Where to start looking in the list
		int lStartAt = ! inSkipFirstLine ? 0 : 1;

		Hashtable outHash = new Hashtable();

		// For each line
		for( int i=lStartAt; i < inHeaderLines.size(); i++ )
		{
			String theLine = (String)inHeaderLines.get(i);
			// Look for proper colon-space sequence
			int colonAt = theLine.indexOf( ": " );
			int colonLength = 2;
			// Or try for technically incorrect colon with no space
			if( colonAt < 0 )
			{
				colonAt = theLine.indexOf( ':' );
				colonLength = 1;
			}

			// First sanity check
			if( colonAt < 0 )
			{
				String tmpMsg = "";
				// Extra info/reminder about how to skip the first line
				if( i==0 && ! inSkipFirstLine )
					tmpMsg = " This appears to be the first header line"
						+ " and therefore perhaps it should not have"
						+ " any fields anway;"
						+ " you might try setting inSkipFirstLine=true."
						;
				warningMsg( kFName,
					"No colon was found on header line " + (i+1)
					+ " Line was: '" + theLine + "'"
					+ tmpMsg
					+ " Ignoring this line."
					);
				continue;
			}

			String key = "";
			String value = "";

			// Grab the key
			if( colonAt > 0 )
				key = theLine.substring( 0, colonAt).trim();

			// Grab the value
			int valueStartsAt = colonAt + colonLength;
			// if( valueStartsAt < theLine.length()-2 )
			if( valueStartsAt <= theLine.length()-1 )
				value = theLine.substring( valueStartsAt ).trim();

			// Whine if either of them is bogus
			if( key.length() < 1 || value.length() < 1 )
			{
				warningMsg( kFName,
					"Malformed header line:"
					+ " it has a colon separater, but a null key or value,"
					+ " Header line " + (i+1)
					+ " was: '" + theLine + "'"
					+ " Parsed key = '" + key + "'"
					+ " Parsed value = '" + value + "'"
					+ " Some debug info:"
					+ " colonAt=" + colonAt
					+ ", colonLength=" + colonLength
					+ ", valueStartsAt=" + valueStartsAt
					+ ", theLine.length()=" + theLine.length()
					+ " Ignoring this line."
					);
				continue;
			}

			// Add this to AuxIO info
			inAux.addHTTPHeaderField( key, value );

			// Older code

//			// Normalize, if asked to do so
//			if( inForceToLowerCase )
//				key = key.toLowerCase();
//
//			// Do we already have an entry in the hash for this?
//			if( outHash.containsKey(key) )
//			{
//				// If they didn't tell us to keep just the first instance,
//				// then they want all instances.
//				// We don't currently support that!
//				// TODO: Support it!  Replace a string entry with a vector
//				if( ! inKeepOnlyFirstInstance )
//				{
//					System.err.p rintln(
//						"Warning: NIEUtil:parseHTTPHeaderFields:"
//						+ " Multiple values for header field found:"
//						+ " Found more than one header line with the same field name"
//						+ " and was asked to keep all instances;"
//						+ " this insn't supported yet."
//						+ " Suggest implementing it or setting"
//						+ " inKeepOnlyFirstInstance=true."
//						+ " This header line " + (i+1)
//						+ " was: '" + theLine + "'"
//						+ " Parsed key = '" + key + "'"
//						+ " Parsed value = '" + value + "'"
//						+ " Ignoring this line."
//						);
//					continue;
//				}
//				// Nothing else to do, we're just ignoring the subsequent value
//				continue;
//			}
//			else    // Else it's new to the hash
//			{
//				// So just add it
//				outHash.put( key, value );
//			}

		}   // End for each line

//		// check results, warn, return
//		if( outHash.size() < 1 )
//			System.err.p rintln(
//				"Warning: NIEUtil:parseHTTPHeaderFields:"
//				+ " Didn't find any valud key/value pairs in header."
//				+ " Started with " + inHeaderLines.size() + " lines."
//				+ " This may be OK, but seems odd."
//				+ " Returning zero item hash."
//				);
//
//		return outHash;

	}

	// Read the requested number of bytes from a socket
	// By default, demand a reasonable expected count.
	// Zero is NOT a valid number of bytes to read
	// If no reasoable count and told to read anyway, read until EOF
	// This could cause a problem if the other end likes to leave a socket open
	// Warn if fewer bytes read before EOF
	static String readNBytesFromSocket( InputStream inStream,
		long inExpectedCount
		)
	{
		return readNBytesFromSocket( inStream, inExpectedCount, false );
	}
	static String readNBytesFromSocket( InputStream inStream,
		long inExpectedCount, boolean inReadToEOFOnBadExprectedCount
		)
	{
		final String kFName = "readNBytesFromSocket";
		final boolean trace = shouldDoTraceMsg( kFName );

		if( inStream == null )
		{
			errorMsg( kFName,
				"NULL stream passed in, returning NULL string."
				);
			return null;
		}
		// Also check if we're supposed to have a valid byte count and
		// we don't
		if( inExpectedCount <= 0L && ! inReadToEOFOnBadExprectedCount )
		{
			errorMsg( kFName,
				"Unreasonable value given for inExpectedCount"
				+ "=" + inExpectedCount
				+ ", and told to demand a valid number, > 0."
				+ " To read to EOF with out giving a valid count"
				+ " set inReadToEOFOnBadExprectedCount=true."
				+ " Returning NULL string."
				);
			return null;
		}

		// Init a buffer and loop variables
		StringBuffer buffer = new StringBuffer();
		int theChar;

		// For each character
		while( true )
		{
			// Get the next character
			try
			{
				theChar = inStream.read();
			}
			catch (IOException e)
			{
				errorMsg( kFName,
					"Got exception on socket/stream read, treating as EOF."
					+ " Exception was: '" + e + "'."
					+ " Treating as end-of-file marker."
					);
				theChar = -1;
			}

			// Check for EOF
			if( theChar < 0 )
				break;
			// Add the character to the buffer
			buffer.append( (char)theChar );
			// Break out if we were given a target count and have met it
			if( inExpectedCount > 0 && buffer.length() >= inExpectedCount )
				break;
		}

		int bytesLeft = 0;
		try
		{
			bytesLeft = inStream.available();
		}
		catch (IOException ae)
		{
			warningMsg( kFName,
				"Got exception on socket/stream available(), ignoring."
				+ " Exception was: '" + ae + "'."
				);
		}
		if( bytesLeft > 0 )
		{
			String tmpMsg = "Done reading, but still have "
				+ bytesLeft + " bytes, will add those."
				+ " Use trace mode to see detailed buffer contents, before and after."
				;
			if( bytesLeft > 2 )
				warningMsg( kFName, tmpMsg );
			else    // We often have 2 left, no big deal
				infoMsg( kFName, tmpMsg );

			if( trace )
				traceMsg( kFName, "Before adding *extra* bytes, buffer is: \""
					+ (new String(buffer)) + "\""
					);
			// For each byte, read it
			for( int i=0; i<bytesLeft; i++ )
			{
				// Get the next character
				try
				{
					theChar = inStream.read();
				}
				catch (IOException e2)
				{
					errorMsg( kFName,
						"Warning:NIEUtil:readNBytesFromSocket:"
						+ " (2) Got exception on socket/stream read, treating as EOF."
						+ " Exception was: '" + e2 + "'."
						);
					theChar = -1;
				}

				// Check for EOF
				if( theChar < 0 )
				{
					warningMsg( kFName,
						"Was reading *extra* types, but got EOF."
						+ " Was expecting " + bytesLeft
						+ " but have only read " + i + " bytes."
						+ " Will accept what we've already got."
						);
						// ^^^ actually i +1 -1 = just i
					break;
				}
				// Add the character to the buffer
				buffer.append( (char)theChar );
			}   // End read all the extra bytes
			traceMsg( kFName, "After adding *extra* bytes, buffer is: \""
				+ (new String(buffer)) + "\""
				);

		}


		// Give appropriate warning messages
		// No data
		if( buffer.length() <= 0 )
		{
			warningMsg( kFName,
				"No data read from socket?"
				+ " Will be returning zero length string."
				);
		}
		// Not enough data
		else if( inExpectedCount > 0 && buffer.length() < inExpectedCount )
		{
			warningMsg( kFName,
				"Read fewer bytes from socket than expected."
				+ " inExpectedCount = " + inExpectedCount
				+ ", Actual number of bytes read = " + buffer.length()
				+ " Will return the forshortened data that we did get."
				);
		}

		// Convert and return the results
		return new String( buffer );
	}

	public static String strongUrlPercentEncoderOrNull( String inStr )
	{
		final String kFName = "strongUrlPercentEncoderOrNull";
		if( null==inStr )
			return null;
		StringBuffer inBuff = new StringBuffer( inStr );
		StringBuffer outBuff = new StringBuffer();

		for( int i=0; i<inBuff.length(); i++ )
		{
			String inC = inBuff.substring(i, i+1 );
			String outC = null;
			// Force space to encoding
			if( inC.equals(" ") )
			{
				outC = "%20";
			}
			// Also for period/dot, hyphen/dash, start/asterisk and underscore
			else if( inC.equals(".") )
			{
				outC = "%2E";
			}
			else if( inC.equals("*") )
			{
				outC = "%2A";
			}
			else if( inC.equals("-") )
			{
				outC = "%2D";
			}
			else if( inC.equals("_") )
			{
				outC = "%5F";
			}
			else {
				try {
					// Sun says W3C says to use UTF-8, see
					// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
					// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
					outC = URLEncoder.encode( inC, AuxIOInfo.CHAR_ENCODING_UTF8 );
				}
				catch( UnsupportedEncodingException e )
				{
					outC = URLEncoder.encode( inC );
					warningMsg( kFName, "URLEncoder.encode threw exception for standard encoding, resorting to older encode, for key '" + inC + "', exception: " + e );
				}
			}
			outBuff.append( outC );
		}
		return new String( outBuff );
	}

//	// Return a hash of strings from a GET style submit
//	// static Hashtable cgiParseGETVariablesFromCGIHeader(
//	// Public for now, referenced in XPump/procs/Monitor
//	public static Hashtable cgiParseGETVariablesFromCGIHeader(
//		String headerLine
//		)
//	{
//		Hashtable outHash = new Hashtable();
//		String pathSection = cgiParsePathSectionFromCGIHeader( headerLine );
//		if( pathSection == null )
//		{
//			System.err.p rintln(
//				"Warning: NIEUtil:cgiParseGETVariablesFromCGIHeader:"
//				+ " Got null path section from cgiParsePathSectionFromCGIHeader"
//				);
//			return outHash;
//		}
//
//		// Todo: finish implementing
//
//		// Search for the infamous question mark!
//		int questionMarkAt = pathSection.indexOf( '?' );
//		// Bail if don't have a question mark or it's null
//		// Note that this is NOT an error, url's often don't have one
//		if( questionMarkAt < 0 || questionMarkAt > pathSection.length()-1 )
//			return outHash;
//
//		String queryString = pathSection.substring( questionMarkAt+1 );
//
//		cgiDecodeVarsBuffer( queryString );
//
//		return outHash;
//	}


	// Given a string, decode CGI style variables
	// By default we normalize to lower case
	// Currently we don't keep multiple values????
//	public static void cgiDecodeVarsBuffer( AuxIOInfo inAux, String inBuffer )
//	{
//		cgiDecodeVarsBuffer( inAux, inBuffer, true );
//		// , false );
//	}
	public static int cgiDecodeVarsBuffer( AuxIOInfo inAux, String inBuffer
		// boolean inIsCasenFields
		// , boolean inKeepOnlyFirstInstance
		)
	{
		final String kFName = "cgiDecodeVarsBuffer";

		debugMsg( kFName, "inIsCasenFields=" + inAux.isCGIFieldsCaseSensitive() );

		boolean trace = shouldDoTraceMsg( kFName );

		int outCount = 0;

		// Whether cgi variable names are case sensitive or not
		// will lower case them if not sensative
		// final boolean lCaseSenVarNames = false;
		// replaced by opposite var inForceToLowerCase

		// Whether or not to trim key names before looking them up
		final boolean lTrimKeyNames = true;

		// Whether to trim values, this can affect the empty logic
		final boolean lTrimValues = true;

		// Whether we will bother to put something in the hash
		// even if it's a null string
		final boolean lKeepEmptyStrings = false;

		// If somebody says foo= with nothing after should we default
		// to adding a "1" or something
		final boolean lDoDefaultValue = false;

		// We have an internal null marker
		final boolean lDoDiscardInternalNullMarker = true;


//		// How to join multiple values
//		// NULL means DON'T keep additional ones
//		final String lMultiDelimSeq = null;

//		Hashtable outHash = new Hashtable();

		if( inAux == null || inBuffer == null )
		{
			errorMsg( kFName,
				"Was passed a NULL Aux structore or string buffer."
				+ " Nothing to do."
				);
			return -1;
		}

		// Bail on internal null marker
		if( lDoDiscardInternalNullMarker ) {
			if( inBuffer.trim().equalsIgnoreCase( nie.sr2.ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) ) {
				return 0;
			}
		}

		StringTokenizer st = new StringTokenizer( inBuffer, "&" );
		while( st.hasMoreTokens() )
		{
			String item = st.nextToken();
			int equalsAt = item.indexOf('=');
			String key = null;
			String value = null;
			// No equals or right at the start
			if( equalsAt < 0 )
			{
				if( lDoDefaultValue )
				{
					key = item;
					value = "1";
				}
				else
				{
					warningMsg( kFName,
						"A buffer section had no equals sign."
						+ " Section=\"" + item + "\""
						+ " Buffer=\"" + inBuffer + "\""
						+ " Will skip this part and continue looking at parts."
						);
					continue;
				}
			}
			else if( equalsAt < 1 )
			{
				warningMsg( kFName,
					"A buffer section had a poorly placed equals sign."
					+ " Section=\"" + item + "\""
					+ " Buffer=\"" + inBuffer + "\""
					+ " Will skip this part and continue looking at parts."
					);
				continue;
			}
			// Else we seem to have a well placed = sign
			else
			{
				key = item.substring( 0, equalsAt );
				if( equalsAt < item.length()-1 )
					value = item.substring( equalsAt+1 );
				else
					value = "";
			}

			// At this point we have *something* for key and value

			// Obsess about the key
			key = key != null ? key : "" ;

			// Decode the key
			if(trace) traceMsg(kFName, "predec key=\"" + key + "\"");

			// We need to dencode the string from x-www-form-urlencoded format
			// key = URLDecoder.decode( key );
			// Use the correct encoding, Java 1.4
			try {
				// Sun says W3C says to use UTF-8, see
				// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
				// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
				key = URLEncoder.encode( key, AuxIOInfo.CHAR_ENCODING_UTF8 );
			}
			catch( UnsupportedEncodingException e )
			{
				key = URLEncoder.encode( key );
				warningMsg( kFName, "URLEncoder.decode threw exception for standard encoding, resorting to older encode, for key '" + key + "', exception: " + e );
			}

			
			if(trace) traceMsg(kFName, "pstdec key=\"" + key + "\"");

			/*** no, this is handled further on, so we can presve orig case
			// Normalize, if asked to do so
			if( ! inAux.isCGIFieldsCaseSensitive() )
			{
				key = key.toLowerCase();
				debugMsg( kFName, "Normalied key to \"" + key + "\"." );
			}
			else
				debugMsg( kFName,
					"Not normalizing key, leaving as \"" + key + "\"."
					);
			***/

			// And trim if asked to do so
			if( lTrimKeyNames )
				key = key.trim();
			// Bail on an empty key
			if( key.equals("") )
			{
				warningMsg( kFName,
					"A buffer section had an empty variable name."
					+ " Section=\"" + item + "\""
					+ " Buffer=\"" + inBuffer + "\""
					+ " Will skip this part and continue looking at parts."
					);
				continue;
			}

			// Obsess about the value
			value = value != null ? value : "" ;

			// Tweak for weird Solaris bug
			// boolean is_hex_A_Zero = value.equalsIgnoreCase( "%A0" );

			// Here's where we actually do the decoding
			if(trace) traceMsg(kFName, "predec val=\"" + value + "\"");
			// value = URLDecoder.decode( value );
			// value = URLDecoder.decode( value, "iso-8859-1" );
			// Issues with Java 1.4 on Solaris
			// Use ours, which forces a single byte decide
			// value = urlDecoder( value );
			// I think the Solaris warning had been a deprecation warning, when
			// it was first compiled in 1.4, which happened to be on a solaris box
			// Changed to using proper 1.4
			try {
				// Sun says W3C says to use UTF-8, see
				// http://java.sun.com/j2se/1.4.2/docs/api/java/net/URLDecoder.html#decode(java.lang.String,%20java.lang.String)
				// http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
				value = URLEncoder.encode( value, AuxIOInfo.CHAR_ENCODING_UTF8 );
			}
			catch( UnsupportedEncodingException e )
			{
				value = URLEncoder.encode( value );
				warningMsg( kFName, "URLEncoder.decode threw exception for standard encoding, resorting to older encode, for value '" + value + "', exception: " + e );
			}

			// And a double check for problems
			value = value != null ? value : "" ;
			// ^^^ TODO: should this throw an exception?

			if(trace) traceMsg(kFName, "pstdec val=\"" + value + "\"");

			// Tweak for weird Solaris bug, part B
			// if( is_hex_A_Zero ) {
			//	debugMsg( kFName, "Forcing 0xA0 to empty string after decode, was=\"" + value + "\"" );
			//	value = "";
			// }

			// We now also record unnormalized values
			inAux.addCGIField_UnnormalizedValue( key, value );

			// Now we normalize, and may or may not keep the result
			if( lTrimValues )
				value = value.trim();

			// Bail if we have an unwanted empty value string
			// This is so common we don't issue a warning
			if( ! lKeepEmptyStrings && value.equals("") )
				continue;

			// Also skip our null marker stuff
			if( lDoDiscardInternalNullMarker ) {
				if( value.trim().equalsIgnoreCase( nie.sr2.ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) ) {
					continue;
				}
			}


			// So by now we have a normalized key and value that we like

			// Tell the aux IO info object to store it
			inAux.addCGIField( key, value );
			outCount++;

//			// We'll look for old values
//			String prevValue = null;
//			if( outHash.containsKey( key ) )
//				prevValue = (String)outHash.get( key );


//          // Some logic from the HTTP header parsing routine
//          // At some point the idea is to maybe put a vector in place
//          // of a string if there's more than one value
//				// If they didn't tell us to keep just the first instance,
//				// then they want all instances.
//				// We don't currently support that!
//				// TODO: Support it!  Replace a string entry with a vector
//				if( ! inKeepOnlyFirstInstance )
//				{
//					System.err.p rintln(
//						"Warning: NIEUtil:parseHTTPHeaderFields:"
//						+ " Multiple values for header field found:"
//						+ " Found more than one header line with the same field name"
//						+ " and was asked to keep all instances;"
//						+ " this insn't supported yet."
//						+ " Suggest implementing it or setting"
//						+ " inKeepOnlyFirstInstance=true."
//						+ " This header line " + (i+1)
//						+ " was: '" + theLine + "'"
//						+ " Parsed key = '" + key + "'"
//						+ " Parsed value = '" + value + "'"
//						+ " Ignoring this line."
//						);
//					continue;
//				}


//			// Create the new value
//			String newValue = null;
//
//			// Did we have something from before?
//			if( prevValue != null )
//			{
//
//				// Have we been told to only keep the first instance?
//				if( inKeepOnlyFirstInstance )
//				{
//					// warning about discarding this new value
//					System.err.p rintln(
//						"Warning: NIEUtil:cgiDecodeVarsBuffer:"
//						+ " Found a repeating variable but told to only keep first;"
//						+ " (inKeepOnlyFirstInstance was true)"
//						+ " will ignore/drop this newer value."
//						+ " normalized variable name=\"" + key + "\""
//						+ ", previously found value=\"" + prevValue + "\""
//						+ ", new (ignored) value=\"" + value + "\""
//						+ ", Section=\"" + item + "\""
//						+ ", Buffer=\"" + inBuffer + "\""
//						);
//					continue;
//				}
//				else    // Else we're keeping all instances
//				{
//					// Do we have a separator?
//					// Todo: at some point we'll have a full policy in place
//					// to allow for tabbed lists or vectors
//					if( lMultiDelimSeq != null )
//					{
//						newValue = prevValue + lMultiDelimSeq + value;
//					}
//					// Else we were not given a delimiter (or policy)
//					else
//					{
//						System.err.p rintln(
//							"Warning: NIEUtil:cgiDecodeVarsBuffer:"
//							+ " Found a repeating variable but not told what the delim is;"
//							+ " (lMultiDelimSeq was NULL)"
//							+ " will ignore/drop this newer value."
//							+ " normalized variable name=\"" + key + "\""
//							+ ", previously found value=\"" + prevValue + "\""
//							+ ", new (ignored) value=\"" + value + "\""
//							+ ", Section=\"" + item + "\""
//							+ ", Buffer=\"" + inBuffer + "\""
//							);
//						continue;
//					}   // End else we were not given a delimiter
//				}   // Else we're keeping all instances
//			}
//			// Else there was no old value, so just use this
//			else
//				newValue = value;
//
//			// Save the new/revised value
//			outHash.put( key, newValue );

		}   // End of the & tokenizer loop

		// And finally we return the fruit of our labor
		// return outHash;

		return outCount;
	}

	// Deprecated, just use URLEncoder.encode( value, AuxIOInfo.CHAR_ENCODING_UTF8 );
	public static String _urlDecoder( String inString ) {
		final String kFName = "urlDecoder";
		if( null==inString ) {
			errorMsg( kFName, "Null string passed in, returning null." );
			return null;
		}
		StringBuffer inBuff = new StringBuffer( inString );
		StringBuffer outBuff = new StringBuffer();
		for( int i=0; i<inBuff.length(); i++ ) {
			char c = inBuff.charAt( i );
			// If it's a regular char, just add it!
			if( c!='%' && c!='+' ) {
				outBuff.append( c );
			}
			// If it's a +, that means a sapce
			else if( c=='+' ) {
				outBuff.append( ' ' );
			}
			// Else it's a %HH hex encoded sequence
			else {
				// If we don't have enough buffer left, bail
				if( i >= inBuff.length() - 2 ) {
					errorMsg( kFName,
						"Invalid placement of % in encoded cgi buffer"
						+ " in buff = \"" + inString + "\""
						+ ", offset=" + i + " (zero based)"
						+ ", buff len = " + inBuff.length() + " (one based)"
						+ ", Returning null."
						);
					return null;
				}
				// Else we do have at least two more characters
				else {
					// Grab each one and convert to a value
					char highC = inBuff.charAt( ++i );
					int highDig = Character.digit( highC, 16 );
					char lowC = inBuff.charAt( ++i );
					int lowDig = Character.digit( lowC, 16 );
					if( highDig < 0 || lowDig < 0 ) {
						errorMsg( kFName,
							"Invalid hex character after % in encoded cgi buffer"
							+ " in buff = \"" + inString + "\""
							+ ", post-offset=" + i + " (zero based)"
							+ ", high hexit char = '" + highC + "' ASCII(" + (int)highC + ") = " + highDig
							+ ", low hexit char = '" + lowC + "' ASCII(" + (int)lowC + ") = " + lowDig
							+ ", Returning null."
							);
						return null;
					}
					// Combine them
					int newNum = ( (highDig << 4) & 0xf0) | lowDig;
					char newC = (char)newNum;
					// And we're done
					outBuff.append( newC );
				}
			}	// End else it's a % sign

		}	// End for each char in the input buffer

		return new String( outBuff );
	}

	private static void __DNS_and_Networking__() {}

	// Fast lookup version
	public static Set lookupIpAddressesAsSet( Collection inAddresses )
	{
		final String kFName = "lookupIpAddressesAsSet";
		Set outAddrs = new HashSet();
		if( null==inAddresses || inAddresses.size()<1 )
			return outAddrs;
		for( Iterator it = inAddresses.iterator(); it.hasNext(); ) {
			String startAddr = (String) it.next();
			String endAddr = lookupIpAddressOrNull( startAddr );
			if( null!=endAddr )
				outAddrs.add( endAddr );
		}
		return outAddrs;
	}

	// preserve order version
	public static List lookupIpAddresses( List inAddresses )
	{
		final String kFName = "lookupIpAddresses";
		List outAddrs = new Vector();
		if( null==inAddresses || inAddresses.size()<1 )
			return outAddrs;
		for( Iterator it = inAddresses.iterator(); it.hasNext(); ) {
			String startAddr = (String) it.next();
			String endAddr = lookupIpAddressOrNull( startAddr );
			if( null!=endAddr )
				outAddrs.add( endAddr );
		}
		return outAddrs;
	}
	public static String lookupIpAddressOrNull( String inAddress )
	{
		final String kFName = "lookupIpAddressOrNull";
		if( null==inAddress )
			return null;

		InetAddress lAddress = null;
	    try {
	    	lAddress = InetAddress.getByName( inAddress );
		}
		catch( UnknownHostException uhe ) {
			debugMsg( kFName,
				"Unable to create IP address object from string"
				+ " \"" + inAddress + "\", returning null."
				+ " Error was: " + uhe
				);
			return null;
		}
		if( null!=lAddress )
			return lAddress.getHostAddress();
		else
			return null;
	}
	
	
	private static void __Printing_and_Debugging__() {}

	public static void printStringList( List inList )
	{
		printStringList( inList, null );
	}
	public static void printStringList( List inList, String optListName )
	{
		String buff = printStringListToBuffer( inList, optListName );
		System.err.print( buff );
	}
	public static String printStringListToBuffer( List inList )
	{
		return printStringListToBuffer( inList, null );
	}
	public static String printStringListToBuffer(
		List inList, String optListName
		)
	{
		final String kFName = "printStringListToBuffer";

		final String nl = "\r\n";

		if( inList == null )
		{
			String msg =
				"Null list passed in, nothing to print."
				+ " Will return this error message."
				;
			System.err.println( msg );
			return msg + nl;
		}

		StringBuffer buff = new StringBuffer();

		// The heading
		if( optListName != null )
			buff.append( "Displaying \"" + optListName + "\"" );
		else
			buff.append( "Debug: NIEUtil:printStringListToBuffer: list" );

		// The number of elements
		buff.append(
			" with " + inList.size() + " Strings"
			+ " (base 0 offsets)"
			+ nl
			);

		// Setup loop and itterate
		for( int i = 0; i < inList.size(); i++ )
		{
			String item = (String)inList.get(i);
			buff.append( "\t" + i + ": \"" + item + "\"" + nl );
		}

		// Return the results
		return new String( buff );
	}
	public static String printStringListToBufferCompact( List inList )
	{
		return printStringListToBufferCompact( inList, true );
	}
	public static String printStringListToBufferCompact(
		List inList, boolean inUseQuotes
		)
	{
		StringBuffer buff = new StringBuffer();
		if( inList == null )
		{
			buff.append( "(NULL list)" );
		}
		else if( inList.size() < 1 )
		{
			buff.append( "(empty list)" );
		}
		else
		{
			// Setup loop and itterate
			for( int i = 0; i < inList.size(); i++ )
			{
				String item = (String)inList.get(i);
				if( i>0 )
					buff.append( ", " );
				if( inUseQuotes )
					buff.append( '"' );
				buff.append( item );
				if( inUseQuotes )
					buff.append( '"' );
			}
		}

		// Return the results
		return new String( buff );
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
	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName,
			inFromRoutine );
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


	public static String DEFAULT_NIE_DATETIME_FORMAT = "MM/dd/yy HH:mm:ss a";
	public static final long MS_PER_DAY = 24 * 60 * 60 * 1000;
	// Some particular epoch's we know about
	// There are 25,567 days between Jan 1st 1900 and Jan 1st 1970
	public static final long EPOCH_CORRECTION_SQL_SERVER = -25567 * MS_PER_DAY;

	// I believe Verity is:
	// 23 Sep 2002 20:22:30 pm
	//	Kevin's Example:
	//		for 3:38 and 22 seconds, Pacific Daylight Time, on December 12, 2002
	//	PostGreSQL time string:
	//		`12-27-2002 15:38:22 -07'
	//	Oracle:
	//		`27-DEC-02 03.38.22.0000 PM -07:00'
	//	MySQL
	//		2007-12-15 23:50:26
	//  Dates from Export
	//		2008-03-19 14:49:24.721682-07
	//		^^^ from JDBC ResultsSet .getString( field num )
	//	Mark found
	//	JDBC timestamp escape format.
	//		yyyy-mm-dd hh:mm:ss.fffffffff
	// NIE usually SQL reports usually 'MM/DD/YY HH:MI:SS am'
	// But then we have/had:
	// DateFormat formatter = DateFormat.getDateTimeInstance(
	// 	DateFormat.SHORT, DateFormat.LONG );
	// Gives: 7/15/02 4:34:45 PM PDT
	// Also DateFormat.FULL, DateFormat.FULL
	// Example: July 15, 2002 4:33:39 PM PDT
	// Also DateFormat.SHORT, DateFormat.SHORT
	// Example: 7/15/02 4:32 PM
	//
	// Note: These us the Java format, NOT THE NIE SHORTHAND
	// See Java SimpleDateFormat
	// http://java.sun.com/j2se/1.5.0/docs/api/java/text/SimpleDateFormat.html
	public static final String [] DATETIME_PARSE_FORMATS = {
		"yyyy-MM-dd H:mm:ss.SSSZ",		// Default from ResultSet getString()
		"yyyy-MM-dd H:mm:ss.SSS",			// no timezone
		"yyyy-MM-dd H:mm:ss",			// MySQL
		"MM/dd/yy H:mm:ss a",			// SearchTrack default format
		"dd-MMM-yyyy HH.mm.ss.SSS a Z",	// Oracle long
		"dd-MMM-yy HH.mm.ss.SSSS a Z",		// Oracle 2 year
		"MM-dd-yyyy H:mm:ss Z",			// PostgreSQL
		"dd MMM yyyy H:mm:ss a",		// Verity K2
		"dd/MMM/yyyy:H:mm:ss Z",		// FAST ESP query_log format
		// "dd.MM.yyyy",
		"M/dd/yyyy",					// Generic US shorthand
		"M-dd-yyyy",
		"MMM dd, yyyy",
		"M/dd/yy"
	};

	private static final String [] STOP_WORDS = {
		"a", "i", "c","f",
		"and", "as", "did", "do", "if", "in", "is", "of",
		"no", "not", "isn't", "didn't", "don't",
		"than", "that", "the", "there", "this", "to", "too", "tm",
		"was", "wasn't",
		"$", "&", "-", "/"
		// "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
		};
	private static final Set kStopWords = new HashSet();
	static {
		for( int i=0; i<STOP_WORDS.length; i++ )
			kStopWords.add( STOP_WORDS[i] );
	}

}

//	class test
//	{
//		public test()
//		{
//		}
//	}

	// Helper class used for HTTP Basic authentication
	class NIEAuthenticator2 extends Authenticator
	{
//		private String username;
//		private String password;
		public NIEAuthenticator2( String u, String p )
		{
//			super();
//			this.username = u;
//			this.password = p;
		}
		protected PasswordAuthentication getPasswordAuthentication()
		{
//			return new PasswordAuthentication(
//				username, password.toCharArray()
//				);
			return null;
		}
	}



package nie._lic;

import java.util.*;
import java.io.*;
import java.text.*;

import nie.core.*;
import nie.sn.SearchTuningConfig;

import org.jdom.Element;
import org.jdom.Attribute;


public class LicGen {

	public static final String kClassName = "LicGen";

	public static void main( String [] args )
		throws LicException
	{
		if( args.length != 1 ) {
			System.err.println(
				"Syntax: script licfile.xml"
				+ NIEUtil.NL
				+ "Skeleton licfile.xml looks like:" + NIEUtil.NL
				// + blankLic().JDOMToString( true )
				+ formatLic( blankLic() )
				+ NIEUtil.NL
				+ "This script will fill in the key= field when run."
				+ NIEUtil.NL
				+ "Reminders:"
				+ NIEUtil.NL
				+ "Field \"" + LRV + "\" must always be \"1\"."
				+ NIEUtil.NL
				+ "Fields \"" + LCO + "\" and \"" + LND + "\" are mandatory."
				+ NIEUtil.NL
				+ "Fields \"" + LSRV + "\" and \"" + LST + "\" are optional."
				+ NIEUtil.NL
				+ "Field \"" + LKY + "\" is filled in by this utility."
				+ NIEUtil.NL
				);
			System.exit(1);
		}

		String licFile = args[0];
		JDOMHelper licTree = loadLic( licFile );
		calculateAndAddKey( licTree );
		System.err.println(
			"License for licfile \"" + licFile + "\" is:"
			+ NIEUtil.NL
			+ formatLic( licTree )
			+ NIEUtil.NL
			);

	}


	// WARNING!!!!!!
	// Changes to the algorithm here must match up with:
	//	nie.sn.SearchTuningConfig.readGlobalOptions()
	//	AND
	//	nie.config_ui.Configurator.vl()
	//////////////////////////////////////////////////////////////////
	static void calculateAndAddKey( JDOMHelper inLicElem )
		throws LicException
	{
		final String kFName = "calculateAndAddKey";
		final String kExTag = kClassName + '.' + kFName + ": ";

		final String msg = "Bad license field: ";
		if( null==inLicElem )
			throw new LicException( kExTag + "NULL Config passed in" );
		StringBuffer buff = new StringBuffer();
		String r = inLicElem.getStringFromAttributeTrimOrNull( LRV );
		if( null==r || ! r.equals("1") ) throw new LicException( kExTag + msg + LRV );
		buff.append(r).append('-');
		String lCo = inLicElem.getStringFromAttributeTrimOrNull( LCO );
		if( null==lCo ) throw new LicException( kExTag + msg + '(' + 1 + ") " + LCO );
		int ccnt = 0;
		for( int i=0; i<lCo.length(); i++ ) {
			char c = lCo.charAt( i );
			if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
				buff.append(c);
				ccnt++;
			}
			else if( c>='A' && c<='Z' ) {
				buff.append( Character.toLowerCase(c) );
				ccnt++;
			}
		}
		if( ccnt<3 ) throw new LicException( kExTag + msg + '(' + 2 + ") " + LCO );
		String lStStr = inLicElem.getStringFromAttributeTrimOrNull( LST );
		Date lSt = null;
		// if( null==fStStr ) throw new SearchTuningConfigException( kExTag + msg + '(' + 1 + ") " + LST );
		if( null!=lStStr ) {
			try {
				DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
				if( ! lStStr.toLowerCase().endsWith(" gmt") )
					lStStr += " GMT";
				lSt = fmt.parse(lStStr);
			} catch (ParseException e1) {
				throw new LicException( kExTag + msg + '(' + 2 + ") " + LST + ": \"" + lStStr + "\" err: " + e1 );
			}
			buff.append('-').append( lSt.getTime() );
		}
		else {
			buff.append("-nst");
		}
		String lNdStr = inLicElem.getStringFromAttributeTrimOrNull( LND );
		if( null==lNdStr ) throw new LicException( kExTag + msg + '(' + 1 + ") " + LND );
		Date lNd = null;
		try {
			DateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy z");
			if( ! lNdStr.toLowerCase().endsWith(" gmt") )
				lNdStr += " GMT";
			lNd = fmt.parse(lNdStr);
		} catch (ParseException e2) {
			throw new LicException( kExTag + msg + '(' + 2 + ") " + LND + ": \"" + lNdStr + "\" err: " + e2 );
		}
		buff.append('-').append( lNd.getTime() );

		// Some sanity checks on the dates they've given us
		if( null!=lSt && lSt.getTime() >= lNd.getTime() )
			throw new LicException( kExTag + msg + '(' + 3 + ") Dates: " + LST + ">=" + LND );
		// Some warnings
		final long MSID = 1000 * 3600 * 24;
		if( lNd.getTime() < (new Date()).getTime() )
			System.err.println( "ERROR: The end date has already passed!" );
		else if( lNd.getTime() < (new Date()).getTime() + MSID * 32 )
			System.err.println( "Warning: The end date will expire in less than a month!" );
		else if( lNd.getTime() > (new Date()).getTime() + MSID * 455 )
			System.err.println( "Warning: The end date expires in way more than one year!" );




		String lSrv = inLicElem.getStringFromAttributeTrimOrNull( LSRV );
		buff.append('-');
		if( null!=lSrv ) {
			ccnt = 0;
			for( int i=0; i<lSrv.length(); i++ ) {
				char c = lSrv.charAt( i );
				if( (c>='a' && c<='z') || (c>='0' && c<='9') ) {
					buff.append(c);
					ccnt++;
				}
				else if( c>='A' && c<='Z' ) {
					buff.append( Character.toLowerCase(c) );
					ccnt++;
				}
			}
			if( ccnt<1 ) throw new LicException( kExTag + msg + '(' + 2 + ") " + LSRV );
		}
		else {
			buff.append( "ns" );
		}
		buff.append('-');
		int sce1 = 1; int sce2 = 1;
		for( int i=0; i<20; i++ ) {
			buff.append(sce1);
			int tmp = sce2; sce2 += sce1; sce1 = tmp;
		}

// statusMsg( kFName, "lstr=\"" + buff + "\"" );


		final int kyln = 4 * 5;
		/***
		String xky = inLicElem.getStringFromAttributeTrimOrNull( LKY );
		if( null==xky ) throw new LicException( kExTag + msg + '(' + 1 + "): " + LKY );
		StringBuffer buff2 = new StringBuffer();
		for( int i=0; i<xky.length(); i++ ) {
			char c = xky.charAt( i );
			if( (c>='a' && c<='f') || (c>='0' && c<='9') ) {
				buff2.append(c);
			}
			else if( c>='A' && c<='F' ) {
				buff2.append( Character.toLowerCase(c) );
			}
		}
		if( buff2.length() != kyln ) throw new LicException( kExTag + msg + '(' + 2 + "): " + LKY );
		***/

		String cky2 = null;
		try {
			Chap cp = new Chap();
			int [] cky1 = cp.sign( new ByteArrayInputStream( (new String(buff)).getBytes()) );
			cky2 = cp.md5string(cky1);
		}
		catch( IOException e ) {
			throw new LicException( kExTag + msg + '(' + 3 + "): " + LKY + ": " + e );
		}
		final int st = 7;
		if( null==cky2 || cky2.length() < (st + kyln) )
			throw new LicException( kExTag + msg + '(' + 4 + "): " + LKY + ": " + st + '/' + kyln + '/' + ( null==cky2 ? -1 : cky2.length() ) );
		cky2 = cky2.substring( st-1, st+kyln-1 );

// statusMsg( kFName, "cky2=\"" + cky2 + "\" (" + cky2.length() + ')' );

		// check against today
		// check that start < end
		// check start is way too big

		StringBuffer cky3 = new StringBuffer( cky2 );
		StringBuffer cky4 = new StringBuffer();
		for( int j=1; j<=cky3.length() ; j++ ) {
			char c = cky3.charAt( j-1 );
			cky4.append( c );
			if( (j%4) == 0 && j<cky3.length() )
				cky4.append( '-' );
		}
		String cky5 = new String( cky4 );
		inLicElem.setAttributeString( LKY, cky5 );

	}


	static String formatLic( JDOMHelper inLicElem )
		throws LicException
	{
		final String kFName = "formatLic";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==inLicElem )
			throw new LicException( kExTag + "Null lic passed in." );
		StringBuffer outBuff = new StringBuffer();
		outBuff.append( '<' ).append( inLicElem.getJdomElement().getName() );
		outBuff.append( NIEUtil.NL );
		for( Iterator attrs=inLicElem.getJdomElement().getAttributes().iterator(); attrs.hasNext() ; ) {
			Attribute attr = (Attribute) attrs.next();
			outBuff.append('\t').append( attr.getName() ).append('=');
			outBuff.append('"').append( attr.getValue() ).append('"');
			outBuff.append( NIEUtil.NL );
		}
		outBuff.append("/>").append( NIEUtil.NL );
		return new String( outBuff );
	}

	static JDOMHelper blankLic()
		throws LicException
	{
		final String kFName = "blankLic";
		final String kExTag = kClassName + '.' + kFName + ": ";
		Element root = new Element( LNODE );
		root.setAttribute( LRV, "1" );
		root.setAttribute( LCO, "Company, Inc." );
		root.setAttribute( LST, "1-Jan-2001" );
		root.setAttribute( LND, "1-Jan-2002" );
		root.setAttribute( LSRV, "servername.domain" );
		root.setAttribute( LKY, "a1a1-b2b2-c3c3-d4d4-e5e5" );
		JDOMHelper outLicElem = null;
		// Instantiate and store the main JDOMHelper a
		try
		{
			// fConfigTree = new JDOMHelper( inURI );
			// use the one that handles includes
			outLicElem = new JDOMHelper(root);

		}
		catch (JDOMHelperException e)
		{
			throw new LicException( kExTag,
				"Got JDOMHelper Exception: "
				+ e );
		}
		return outLicElem;
	}


	static JDOMHelper loadLic( String inURI )
		throws LicException
	{
		final String kFName = "loadLic";
		final String kExTag = kClassName + '.' + kFName + ": ";

		JDOMHelper outLicElem = null;

		// create jdom element and store info
		// Sanity checks
		if( null == inURI )
			throw new LicException( kExTag,
				"Constructor was passed in a NULL URI (file name, url, etc)."
				);

		// Instantiate and store the main JDOMHelper a
		try
		{
			// fConfigTree = new JDOMHelper( inURI );
			// use the one that handles includes
			outLicElem = new JDOMHelper( inURI, null, 0, null );

		}
		catch (JDOMHelperException e)
		{
			throw new LicException( kExTag,
				"Got JDOMHelper Exception (1): "
				+ e );
		}

		if( ! outLicElem.getElementName().equals(MAIN_ELEMENT_NAME) ) {
			// Try the secondary path
			Element tmpElem = outLicElem.findElementByPath( PATH2 );
			// IF we found a node...
			if( null!=tmpElem )
			{
//				statusMsg( kFName,
//					"Examining Embedded License in file \"" + inURI + "\""
//					);
				try
				{
					// fConfigTree = new JDOMHelper( inURI );
					// use the one that handles includes
					outLicElem = new JDOMHelper( tmpElem );

				}
				catch (JDOMHelperException e)
				{
					throw new LicException( kExTag,
						"Got JDOMHelper Exception (2): "
						+ e );
				}
			}
			else
				throw new LicException( kExTag,
					"Not a valid license or application configuration file."
					+ " File/URL = \"" + inURI + "\""
					);
		}
		else {
//			statusMsg( kFName,
//				"Examining Stand-Alone Database Configuration file \"" + inURI + "\""
//				);
		}

		return outLicElem;
	}


	void displayLic( Element inElem ) {
	}

	// checks the root itself
	static final String MAIN_ELEMENT_NAME = "license"; // = SearchTuningConfig.LNODE;
	// or for a node directly under the root
	static final String PATH2 = MAIN_ELEMENT_NAME;

	// These should be kept in sync with SearchTuningConfig
	// not making them public over there for security reasons
	// not having them reference here since this will not be shipped

	// a little obscrty
	private static final String LNODE = "l" + "i"	// <license ...>
		+ "ce" + "n"
		+ "se";
	private static final String LRV = "r"			// rev="1"
		+ "e" + "v";
	private static final String LCO = "co"			// company=
		+ "mpa" + "ny";
	private static final String LST = "s" + "t"	// start_date="dd-MMM-yyyy" (we add gmt)
		+ "ar" + "t_d"
		+ "ate";
	private static final String LND = "e" + "n"	// end_date="dd-MMM-yyyy" (we add gmt)
		+ "d_da" + "te";
	private static final String LSRV = "s"			// server=
		+ "er" + "ve" + "r";
	private static final String LKY = "k"			// key="1111-2222-3333-4444-5555"
		+ "e"
		+ "y";

}

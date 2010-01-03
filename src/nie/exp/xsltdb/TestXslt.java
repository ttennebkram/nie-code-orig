package nie.exp.xsltdb;

import java.util.*;
import java.io.*;
import java.sql.*;
import org.jdom.Element;
import org.jdom.Document;

import nie.core.*;


/**
 * @author mbennett
 *
 * A class that encapsulates NIE's XML table definitions
 */
public class TestXslt
{
	private static final String kClassName = "TestXslt";
	private static final String kStaticFullClassName = "nie.exp.xsltdb." + kClassName;

	final static String TEST_XML = "system:test.xml";
	final static String TEST_XSLT = "system:/nie/exp/xsltdb/test.xslt";

	public static void main( String [] args ) {
		final String kFName = "main";

		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName( kStaticFullClassName );


		JDOMHelper mainNode = null;
		try
		{
			mainNode = new JDOMHelper( TEST_XML, null, 0, tmpAuxInfo );

		}
		catch (JDOMHelperException e)
		{
			errorMsg( kFName,
				"Error loading config file (1)."
				+ " JDOMHelperException: " + e
				);
			System.exit(2);
		}
		// More sanity checks
		if( null == mainNode ) {
			errorMsg( kFName,
				"Got back a NULL xml tree"
				+ " from file \"" + TEST_XML + "\""
				);
			System.exit(3);
		}

		Document statements = null;
		try
		{
			statements = JDOMHelper.xsltElementToDoc(
				mainNode.getJdomElement(),
				TEST_XSLT,
				null,
				false
				);
		}
		catch( JDOMHelperException e )
		{
			errorMsg( kFName,
				"Unable to run xslt"
				+ " Reason: " + e
				);
			System.exit(4);
		}

		statusMsg( kFName,
			"Result = " + NIEUtil.NL
			+ JDOMHelper.JDOMToString( statements, true )
			);


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

}

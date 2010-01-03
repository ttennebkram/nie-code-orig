/*
 * Created on Aug 31, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.webui.xml_screens;
import org.jdom.Element;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import nie.core.*;
import nie.webui.*;
import java.util.*;
import java.sql.*;

import nie.sn.SearchTuningApp;
import nie.sn.BaseMapRecord;
import nie.sn.DbMapRecord;
import nie.sn.SnURLRecord;
import nie.sr2.ReportConstants;
import nie.sr2.ReportLink;



/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
// public class QueryMaps extends BaseScreen
public class MetaRedirector extends XMLDefinedScreen
{
	public final static String kStaticClassName = "MetaRedirector";
	public String kClassName()
	{
		return kStaticClassName;
	}

	// NO, we still use the parent's full class for locating system resources
	// YES, we are putting our XML relative to here, so css must as well
	public String kFullClassName()
	{
		return "nie.webui.xml_screens." + kStaticClassName;
	}


	public MetaRedirector(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper inScreenDefinitionElement,
		String inScreenName
		)
			throws UIConfigException, UIException
	{
		super( inMainConfig, inScreenDefinitionElement, inScreenName );
	}

	public boolean augmentFormFromExistingData(
			Element ioBlankForm, String inTargetTerm,
			int optTargetID
		) throws UIException
	{
		// nothing to do
		return true;
	}

	// Fill in field from submitted CGI parameters
	// usually as part of REDISPLAYING a form that was just submitted,
	// typically because of an input error
	public void augmentFormFromCGIInput( Element ioBlankForm, AuxIOInfo inRequestObject )
		throws UIException
	{
		// nada
	}






	public Element processDataSubmission( AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
		boolean inDoFullPage, String inMode
		)
			throws UIException
	{
		final String kFName = "processSubmission";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );
		if( null==ioResponseObject )
		   throw new UIException( kExTag + "Null response object passed in." );
		inMode = NIEUtil.trimmedLowerStringOrNull( inMode );
		if( null==inMode )
		   throw new UIException( kExTag + "Null mode passed in." );

		// We expand out meta fields
		int metaFieldCount = inRequestObject.processMetaField( UILink.META_CGI_FIELD /*, true */ );
		if( metaFieldCount < 1 ) 
			// warningMsg( kFName,
			infoMsg( kFName,
				"No meta fields for meta-redirector, so we will try to send the user back to their original URL."
				+ " Note: This is usually caused by a user having \"(choose an action)\" in the drop down box of a Take Action mini-form."
				);

		// Which operation is being attempted
		// get the mode, typically "forgen" or "submit"
		/***
		String operation = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
		operation = NIEUtil.trimmedLowerStringOrNull( operation );
		if( null==operation )
			throw new UIException( kExTag +
				"Null operation in CGI parameters."
				+ " Expected CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
				+ " to be one of " + UILink.kValidOperations
				);
		***/

		// Setup where we are supposed to return them to, either after
		// a success or cancel (an error brings them back here)
		String returnURL = inRequestObject.getScalarCGIFieldTrimOrNull( RETURN_URL_CGI_FIELD );
		// If this is the first time through, grab the referer
		if( null==returnURL )
			returnURL = inRequestObject.getReferer();
		if( null==returnURL ) {
			// throw new UIException( kExTag +
			errorMsg( kFName,
				"Unable to add get return URL."
				+ " Expected CGI field \"" + RETURN_URL_CGI_FIELD + "\""
				+ " or valid referer field."
				);
		}

		inMode = UILink.UI_MODE_COMMIT;

		// If should be a commit
		if( metaFieldCount > 0 && inMode.equals( UILink.UI_MODE_COMMIT ) )
		{

			String screenName = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.META_CGI_FIELD_SCREEN_PARM );
			String term = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.TERM_FORMGEN_CGI_FIELD );
			int id = inRequestObject.getIntFromCGIField( UILink.MAP_ID_FORMGEN_CGI_FIELD, -1 );

			// We give preference to a real ID over a term
			String keyField = id > 0 ? UILink.MAP_ID_FORMGEN_CGI_FIELD : UILink.TERM_FORMGEN_CGI_FIELD;
			String keyValue = id > 0 ? ""+id : term;


			try {

				nie.webui.UILink uiLink = new nie.webui.UILink(
					getMainConfig(),
					screenName,				// String Screen Name
					keyField,					// String inParmCGIName,
					"goto " + screenName,	// String optLinkTitleText
					null
					);

				// Make sure we have the correct "return" URL passed through
				if( null!=returnURL )
					inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

				// The link text and mode (Create or Edit)
				String linkText = "goto " + screenName;
				String operation = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
				// Create the link
				Element linkElem = uiLink.generateLinkElement(
					inRequestObject,
					linkText,
					operation,
					keyValue
					);

				// Sanity
				if( null==linkElem )
					throw new UIException( kExTag +
						"Got back null link"
						);
				String newURL = linkElem.getAttributeValue( "href" );
				newURL = NIEUtil.trimmedStringOrNull( newURL );
				if( null==newURL )
					throw new UIException( kExTag +
						"Got back link with null/empty href"
						);

				debugMsg( kFName, "newURL=" + newURL );

				// Now we recycle it and turn it into a redirect
				Element outElem = new Element( "redirect" );
				outElem.addContent( newURL );
				return outElem;
		
			}
			catch( Throwable t ) {
				throw new UIException( kExTag +
					"Error updating database."
					+ " Error: " + t
					);		
			}

		}	// End if Commit

		// Get them back to a safe place
		if( null!=returnURL ) {
			Element outElem = new Element( "redirect" );
			outElem.addContent( returnURL );
			return outElem;
		}
		else
			return null;
	}

	static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	static boolean staticTransactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kStaticClassName, inFromRoutine );
	}


	static boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	static boolean staticDebugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kStaticClassName, inFromRoutine );
	}


	static boolean staticTraceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kStaticClassName, inFromRoutine );
	}
	static boolean staticWarningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	boolean mUseCache;
	JDOMHelper cXML;
	Transformer cXsltTransformer;

	public static final String XSLT_FORMGEN_SHEET = "generate_form";

	public static final String _MAP_SELECTOR_REPORT_NAME = "ListMapsForTerm";

	// static final String __DEFAULT_IMAGE_URL_PREFIX = "/files/images/webui/";
	// static final String __DEFAULT_HELP_URL_PREFIX = "/files/help/webui/";

	// The name of the submit button
	public static final String BUTTON_CGI_FIELD = "button";

	public static final String _MAP_ID_FORMGEN_CGI_FIELD = "map";

	public static final String _TERM_FORMGEN_CGI_FIELD = "term";
	public static final String TERM_SUBMIT_CGI_FIELD = "term";
	public static final String TERM_XML_FORM_PATH = "section[1]/field[1]/@name=term";
	public static final String URL_SUBMIT_CGI_FIELD = "url";
	public static final String URL_XML_FORM_PATH = "section[1]/field[2]/@name=url";

	public static final String MAP_ID_CGI_FIELD = "map_id";
	public static final String MAP_ID_XML_FORM_PATH = "section[2]/field[1]/@name=map_id";

	public static final String NEW_TERMS_XML_FORM_PATH = "section[3]/field[1]/@name=seed_terms";
	public static final String NEW_TERMS_SUBMIT_CGI_FIELD = "seed_terms";

	// public static final String RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_FIELD = UILink.RETURN_URL_CGI_FIELD;
	public static final String RETURN_URL_XML_FORM_PATH = "section[4]/field[2]/@type=hidden/@name=return_url";

}

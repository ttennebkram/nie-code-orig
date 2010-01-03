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
import nie.sn.SnRequestHandler;
import nie.sn.SnURLRecord;
import nie.sr2.ReportConstants;
import nie.sr2.ReportDispatcher;
import nie.sr2.ReportLink;



/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
// public class QueryMaps extends BaseScreen
public class Login extends XMLDefinedScreen
{
	public final static String kStaticClassName = "Login";
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


	public Login(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper inScreenDefinitionElement,
		String inScreenName
		)
			throws UIConfigException, UIException
	{
		super( inMainConfig, inScreenDefinitionElement, inScreenName );
		// getXML();
		// getCompiledXSLTDoc();
	}

	// We always allow access to the login screen
	public boolean verifySecurityLevelAccess( AuxIOInfo inRequest ) {
		return true;
	}


	public Element processRequest(
			AuxIOInfo inRequestObject,
			AuxIOInfo ioResponseObject,
			boolean inDoFullPage
		)
			throws UIException // , UIConfigException
	{
		final String kFName = "processRequest";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// Sanity checks
		if( null==inRequestObject )
			throw new UIException( kExTag + "Null request object passed in." );

		// default mode to formgen
		String mode = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.MODE_CGI_FIELD );
		mode = NIEUtil.trimmedLowerStringOrNull( mode );
		if( null==mode ) {
			inRequestObject.setOrOverwriteCGIField( UILink.MODE_CGI_FIELD, UILink.UI_MODE_FORMGEN);
		}

		// default operation to add
		String op = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
		op = NIEUtil.trimmedLowerStringOrNull( op );
		if( null==op ) {
			inRequestObject.setOrOverwriteCGIField( UILink.OPERATION_CGI_FIELD, UILink.UI_OPERATION_ADD );
		}

		// If any issue, return to this page
		inRequestObject.setOrOverwriteCGIField( UILink.RETURN_URL_CGI_FIELD, UILink.RETURN_URL_CGI_SELF_MARKER );

		return super.processRequest( inRequestObject, ioResponseObject, inDoFullPage );
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
		final String kFName = "augmentFormFromCGIInput";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null form passed in."
				);
		if( null==inRequestObject )
			throw new UIException( kExTag +
				"Null CGI request object passed in."
				);

		// Many of the values we pull may be null, if we're re-displaying
		// due to missing data, so don't worry about it

		// Get the main items
		String username = inRequestObject.getScalarCGIFieldTrimOrNull( USERNAME_SUBMIT_CGI_FIELD );
		String password = inRequestObject.getScalarCGIFieldTrimOrNull( PASSWORD_SUBMIT_CGI_FIELD );

		// Add these to the form
		genericAugmentFormFromFromMethodParms(
			ioBlankForm,
			username,
			password
			);

	}






	void genericAugmentFormFromFromMethodParms(
			Element ioBlankForm,
			String inUsername,
			String inPassword
		)
			throws UIException
	{
		final String kFName = "genericAugmentFormFromFromMethodParms";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null form passed in."
				);

		Element tmpElem = null;

		if( null!=inUsername )
			setFormFieldValue( ioBlankForm, USERNAME_SUBMIT_CGI_FIELD, inUsername );

		if( null!=inPassword )
			setFormFieldValue( ioBlankForm, PASSWORD_SUBMIT_CGI_FIELD, inPassword );

	}



	// Get a list of ONLY the Webmaster Suggests URL Objects
	SnURLRecord findPrinaryWmsURLInList( List inURLObjs )
	{
		SnURLRecord outURL = null;
		for( Iterator it = inURLObjs.iterator(); it.hasNext() ; )
		{
			SnURLRecord url = (SnURLRecord) it.next();
			if( url.getIsASuggestion() ) {
				outURL = url;
				break;
			}
		}
		return outURL;
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

		String button = inRequestObject.getScalarCGIFieldTrimOrNull( BUTTON_CGI_FIELD );
		button = NIEUtil.trimmedLowerStringOrNull( button );
//		if( null==button )
//			throw new UIException( kExTag +
//				"Null button CGI parameters."
//				+ " Expected CGI field \"" + BUTTON_CGI_FIELD + "\""
//				);
		// ^^^ per aopa, on this screen, we accept Enter as query

//		if( inMode.equals(UILink.UI_MODE_SUBMIT)
//						|| inMode.equals(UILink.UI_MODE_COMMIT)
//						|| inMode.equals(UILink.UI_MODE_CANCEL)

		// Check for cancel states
		if( null==button ) {
			inMode = UILink.UI_MODE_COMMIT;
		}
		else if( button.indexOf("cancel") >= 0
			|| button.indexOf("back") >= 0
			|| button.indexOf("return") >= 0
			|| button.indexOf('<') >= 0
			)
		{
			inMode = UILink.UI_MODE_CANCEL;
		}
		// Check for submit states
		else if( button.indexOf("ok") >= 0
			|| button.indexOf("create") >= 0
			|| button.indexOf("save") >= 0
			|| button.indexOf("next") >= 0
			|| button.indexOf("login") >= 0
			|| button.indexOf('>') >= 0
			)
		{
			inMode = UILink.UI_MODE_COMMIT;
		}
		// We really shouldn't procede
		else {
			throw new UIException( kExTag +
				"Unknown button state in CGI parameters."
				+ " CGI field \"" + BUTTON_CGI_FIELD + "\""
				+ ", unknown state = \"" + button + "\""
				);
		}

		debugMsg( kFName, "mode=" + inMode );

		// If we will commit
		if( inMode.equals( UILink.UI_MODE_COMMIT ) )
		{

			// Which operation is being attempted
			// get the mode, typically "forgen" or "submit"
			// Some other strings
			String username = inRequestObject.getScalarCGIFieldTrimOrNull( USERNAME_SUBMIT_CGI_FIELD );
			String password = inRequestObject.getScalarCGIFieldTrimOrNull( PASSWORD_SUBMIT_CGI_FIELD );

			try {
				if( null==username )
					throw new InvalidFormInputException(
						USERNAME_SUBMIT_CGI_FIELD,
						"Username is a required field"
						);
				if( null==password )
					throw new InvalidFormInputException(
						PASSWORD_SUBMIT_CGI_FIELD,
						"Password is a required field"
						);


				if( ! verifyPassword( username, password ) )
					throw new InvalidFormInputException(
						PASSWORD_SUBMIT_CGI_FIELD,
						"Invalid password, please try again"
						);

				// Default parameter for that report
				String parmName = null;
				String parmValue = null;
				if( null!=nie.sr2.ReportDispatcher.DEFAULT_REPORT_PARM_NAME ) {
					parmName = nie.sr2.ReportDispatcher.DEFAULT_REPORT_PARM_NAME;
					if( null!=nie.sr2.ReportDispatcher.DEFAULT_REPORT_PARM_VALUE )
						parmValue = nie.sr2.ReportDispatcher.DEFAULT_REPORT_PARM_VALUE;
					else
						parmValue = "";
				}

				// Make sure we have the correct "return" URL passed through
				if( null!=returnURL )
					inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

				// Add the verified password, and update the request object
				// inRequestObject.setOrOverwriteCGIField( SnRequestHandler.ADMIN_CGI_PWD_FIELD, password );
				// int accessLevel = getMainConfig().passwordToAccessLevel( password );
				// July 2008 don't keep passing around password field in plain sight
				String key = getMainConfig().passwordToKeyOrNull( password );
				inRequestObject.clearCGIField( SnRequestHandler.ADMIN_CGI_PWD_FIELD, false );
				inRequestObject.setOrOverwriteCGIField( SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD, key );
				int accessLevel = getMainConfig().tokenToAccessLevel( password, false );
				inRequestObject.setAccessLevel( accessLevel );
				
				ReportLink rptLink = new ReportLink(
					getMainConfig(),				// nie.sn.SearchTuningConfig inMainConfig,
					getInitialReportName( inRequestObject ),	// String inReportName,
					"report",						// String inLinkText,
					null,							// String optLinkTitle,
					null,							// String optCssClass,
					parmName,						// String optParmName,
					parmValue						// String optParmDefaultValue
				);

				// Generate the new link
				Element reportLinkElem = rptLink.generateRichLink(
					inRequestObject,	// AuxIOInfo inRequest,
					null,				// String optReportName,
					null,				// String optFilterName,
					true,				// boolean inIsMenuLink (clear vars)
					null,		// String optNewParmValue
					null,		// Hashtable optVariables
					null		// String optNewLinkText
					);
				// Sanity
				if( null==reportLinkElem )
					throw new UIException( kExTag +
						"Got back null link"
						);
				String newURL = reportLinkElem.getAttributeValue( "href" );
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
			// We can handle specific isses with well defined mangled fields
			catch( InvalidFormInputException badFieldException ) {
				return redisplayBadFormSubmission(
					inRequestObject,
					ioResponseObject,
					// inMode,
					inDoFullPage,
					badFieldException
					);
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

	boolean verifyPassword( String inUsername, String inPassword ) {
		// return getMainConfig().passwordToAccessLevel(inPassword) >= 0;
		return getMainConfig().tokenToAccessLevel( inPassword, false ) >= 0;
	}

	String getInitialReportName( AuxIOInfo inRequestInfo ) {
		final String kFName = "getInitialReportName";
		// TODO: this is slightly wrong, we're hard coded to check the
		// permissions on PopSearchLinked
		String reportName = null;

		debugMsg( kFName, "db="+getMainConfig().getHasDbBasedMaps()
			+", access="+inRequestInfo.getAccessLevel()
			+", req="+nie.sr2.java_reports.PopSearchLinked.ACCESS_LEVEL
			// +", pwd="+inRequestInfo.getScalarCGIField( SnRequestHandler.ADMIN_CGI_PWD_FIELD )
			);

		if( getMainConfig().getHasDbBasedMaps()
			&& inRequestInfo.getAccessLevel() >= nie.sr2.java_reports.PopSearchLinked.ACCESS_LEVEL
			)
		{
			reportName = nie.sr2.ReportDispatcher.DEFAULT_REPORT_NAME_WITH_UI; // "PopSearchLinked";
		}
		else {
			reportName = nie.sr2.ReportDispatcher.DEFAULT_REPORT_NAME_NO_UI; // "popular_searches_ndays";
		}

		return reportName;
	}

	public String getTitle( Hashtable inVars )
	{
		// return "Map Form";
		// return "Manage Directed Results";
		// return "or Manage Directed Results";
		return "Session Login";
	}
	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}
	public String getSubtitleOrNull( Hashtable inVars )
	{
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

	// The name of the submit button
	public static final String BUTTON_CGI_FIELD = "button";

	public static final String USERNAME_SUBMIT_CGI_FIELD = "login_username";
	public static final String PASSWORD_SUBMIT_CGI_FIELD = "login_password";

	public static final String _TERM_FORMGEN_CGI_FIELD = "term";
	public static final String _TERM_SUBMIT_CGI_FIELD = "term";
	public static final String _URL_SUBMIT_CGI_FIELD = "url";
	public static final String _MAP_ID_CGI_FIELD = "map_id";
	public static final String _NEW_TERMS_SUBMIT_CGI_FIELD = "seed_terms";

	// public static final String RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_FIELD = UILink.RETURN_URL_CGI_FIELD;
}

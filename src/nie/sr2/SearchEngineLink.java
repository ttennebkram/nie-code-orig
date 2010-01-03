package nie.sr2;

import java.util.*;
// import java.sql.*;
import org.jdom.Element;
// import org.jdom.Comment;
import nie.core.*;
import nie.sn.CSSClassNames;
import nie.sn.SearchEngineConfig;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SearchEngineLink
{
	private final static String kClassName = "SearchEngineLink";


	public SearchEngineLink(
		Element inLinkDefinitionElement,
		// nie.sn.SearchTuningApp inMainApp
		nie.sn.SearchTuningConfig inMainConfig
		)
			throws ReportException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new ReportException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// we also need a report definition
		if( null==inLinkDefinitionElement )
			throw new ReportException( kExTag
				+ "Null XML report definition passed in."
				);
		fMainElem = inLinkDefinitionElement;

		// Fill in the cache
		initCachedFields();

		throw new ReportException( kExTag + "This constructor not currently supported.");
	}

	public SearchEngineLink(
		nie.sn.SearchTuningConfig inMainConfig,
		String optWindowTarget,
		String optLinkTitleText,
		String optCssClass,
		boolean inSearchViaOurProxy
		)
			throws ReportException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		fSearchViaOurProxy = inSearchViaOurProxy;

		// we need the main app for various info
		if( null==inMainConfig )
			throw new ReportException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// Fill in the cache
		// initCachedFields();

		// Init cached fields
		getSearchEngineURL();
		getSearchTermCGIField();


		cTitle = NIEUtil.trimmedStringOrNull(optLinkTitleText);
		cTarget = NIEUtil.trimmedStringOrNull(optWindowTarget);


	}

	String getSearchEngineURL() {
		if( null==cURL ) {
			// Use the native searcvh engine?
			if( ! fSearchViaOurProxy ) {
				// Native Search Engine
				// cURL = getMainConfig().getSearchEngine().getSearchEngineTestDriveURL();
				cURL = getMainConfig().getSearchEngineURL();
			}
			// Or run the search through our proxy
			else {
				// Or via our Server
				// cURL = getMainConfig().getSearchNamesTestDriveURL();
				cURL = getMainConfig().getSearchNamesURL();
			}
		}
		return cURL;
	}

	String getSearchTermCGIField() {
		if( null==cQueryField )
			cQueryField = getMainConfig().getSearchEngine().getQueryField();
		return cQueryField;
	}
	String getWindowTargetOrNull() {
		return cTarget;
	}
	private void initCachedFields()
		throws ReportException
	{
		final String kFName = "initCachedFields";
		final String kExTag = kClassName + '.' + kFName + ": ";

		fUseCache = false;

		/***
		if( null == getLinkText() )
			throw new UIConfigException( kExTag
				+ "No link text given for this link."
				);
		***/

		getTitle();
		getCssClass();

		// Cache the paramter hash
		// getParameters();


		fUseCache = true;
	}





	// Search term can be null, although many search engines won't allow it
	public Element generateLinkElement(
		AuxIOInfo ioRequestObject,
		String inLinkText,
		String optSearchTerm
		)
			throws ReportException
	{
		final String kFName = "generateLinkElement";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==ioRequestObject )
			throw new ReportException( kExTag
				+ "Null request object passed in."
				);

		inLinkText = NIEUtil.trimmedStringOrNull(inLinkText);
		if( null==inLinkText )
			throw new ReportException( kExTag
				+ "Null link text passed in."
				);

		optSearchTerm = NIEUtil.trimmedStringOrNull( optSearchTerm) ;
		if( null==optSearchTerm )
			debugMsg( kFName, "Search term is null." );


		// Construct the link
		// =========================================
		try {

			// Start creating a repository for link info
			AuxIOInfo linkInfo = new AuxIOInfo();
			// Can get either search TUNING URL / NIE's server URL
			// or native search engine URL, controlled by fSearchViaOurProxy
			// which is set in the constructor
			String searchURL = getSearchEngineURL();

			debugMsg( kFName, "searchURL=" + searchURL );

			linkInfo.setBasicURL( searchURL );



			// We'll be copying over cgi fields in a bit, but there's some
			// we won't want to copy over
			List fieldsToExclude = new Vector();

			// What we are searching for
			String cgiField = getSearchTermCGIField();
			// The main param, if any
			if( null!=optSearchTerm && ! optSearchTerm.equals(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
				linkInfo.addCGIField( cgiField, optSearchTerm );
			fieldsToExclude.add(cgiField);


			// Avoid lots of the normal fields
			fieldsToExclude.addAll( ReportConstants.fMiscReportFields );
			// We also need to exclude the session ID
			fieldsToExclude.add( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD );

			// ^^^ this precludes the need for the hard coded stuff below
			/***
			// We don't want to send their password out
			// These are the ones the control URL/CGI issues
			// SnRequestHandler.ADMIN_PWD_FIELD
			// SnRequestHandler.OLD_ADMIN_PWD_FIELD
			// These only control the config file XML paths
			// SearchTuningConfig.ADMIN_PWD_ATTR
			// SearchTuningConfig.OLD_ADMIN_PWD_ATTR
			fieldsToExclude.add( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_FIELD );
			fieldsToExclude.add( nie.sn.SnRequestHandler.OLD_ADMIN_PWD_FIELD );

			// Need to clear out the Admin context, etc
			fieldsToExclude.add( nie.sn.SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD );
			fieldsToExclude.add( nie.sn.SnRequestHandler.SN_OLD_CONTEXT_REQUEST_FIELD );
			fieldsToExclude.add( nie.sn.SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD );

			// Clear out some of the reporting stuff
			fieldsToExclude.add( nie.sr2.ReportConstants.REPORT_NAME_CGI_FIELD );
			// a couple other ones
			// TODO: when these become vars vs hard coded elsewhere, update these
			fieldsToExclude.add( ReportConstants._SITE_ID_CGI_FIELD_NAME );
			fieldsToExclude.add( ReportConstants.DAYS_OLD_CGI_FIELD_NAME );
			***/


			// Copy over any additional search engine fields we might need
			Hashtable searchEngineFields = getSearchEngineTestDriveURLFieldsOrNull();
			if( null!=searchEngineFields ) {
				for( Iterator it = searchEngineFields.keySet().iterator() ; it.hasNext() ; ) {
					String key = (String)it.next();
					String value = (String)searchEngineFields.get( key );
					linkInfo.addCGIField( key, value );
					// And don't copy them over from wheatever was sent in
					fieldsToExclude.add( key );
				}
			}

			// Copy over the stuff we do want
			// This will pickup the password
			// TODO: this will also copy over some BS from the report...
			linkInfo.copyInCGIFields( ioRequestObject, fieldsToExclude );
			// linkInfo.copyInCGIFields( ioRequestObject, ReportConstants.fMiscReportFields );

			// Get the full URL
			String href = linkInfo.getFullCGIEncodedURL();

			debugMsg( kFName, "href=" + href );

			Element outElem = new Element( "a" );
			// outElem class
			// outElem.setAttribute(
			//	"class", ReportDispatcher.NIE_REPORT_LINK_CSS_CLASS
			//	);
			if( null!=getCssClass() )
				outElem.setAttribute(
					"class", getCssClass()
					);

			// Add the link
			outElem.setAttribute( "href", href );
			if( null != getTitle() )
				outElem.setAttribute( "title", getTitle() );
			// Add the link text
			outElem.addContent( inLinkText );

			// TODO: Maybe add some Java script stuff to
			// force the window to the front
			if( null != getWindowTargetOrNull() )
				outElem.setAttribute( "target", getWindowTargetOrNull() );

			// Return the <a> tag
			return outElem;
		}
		catch( Exception e )
		{
			throw new ReportException( kExTag
				+ "Error preparing UI link: " + e
				);

			/***
			errorMsg( kFName,
				"Error preparing UI link."
				// + " Will add UNlinked text instead."
				+ " Returning null."
				+ " Error: " + e
				);
			return null;
			***/
		}

	}



	Hashtable getSearchEngineTestDriveURLFieldsOrNull() {
		final String kFName = "getSearchEngineTestDriveURLFieldsOrNull";
		SearchEngineConfig se = getMainConfig().getSearchEngine();
		if( null!=se )
			return se.getSearchEngineTestDriveURLFields();
		else {
			errorMsg( kFName, "Null search engine config." );
			return null;
		}
	}

	public String getTitle( /*Hashtable inValuesHash*/ )
	{
		/***
		if( ! fUseCache && null==cTitle )
		{
			final String kFName = "getTitle";
			cTitle = JDOMHelper.getTextByPathTrimOrNull(
				fMainElem, TITLE_PATH
				);
			// if( null!=cTitle && cTitle.indexOf( '$' ) >= 0 )
			//	fHaveSeenDollarSigns = true;
		}
		***/
		return cTitle;

		/****
		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == inValuesHash
			)
		{
			return cTitle;
		}
		// If there IS var subst, apply it to the values
		else
		{
			return NIEUtil.markupStringWithVariables(
				cTitle, inValuesHash
				);
		}
		****/

	}

	public String _getLinkText( /*Hashtable inValuesHash*/ )
	{
		return null;

		/***
		^^^ for now it's just passed in, and we don't have xml to pull it from
		if( ! fUseCache && null==cLinkText )
		{
			final String kFName = "getLinkText";
			cLinkText = JDOMHelper.getTextByPathTrimOrNull(
				fMainElem, LINK_TEXT_PATH
				);
			// if( null!=cTitle && cTitle.indexOf( '$' ) >= 0 )
			//	fHaveSeenDollarSigns = true;
		}
		return cLinkText;
		***/

		/****
		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == inValuesHash
			)
		{
			return cTitle;
		}
		// If there IS var subst, apply it to the values
		else
		{
			return NIEUtil.markupStringWithVariables(
				cTitle, inValuesHash
				);
		}
		****/

	}



	String getCssClass()
	{
		if( ! fUseCache && null == cCssClass )
		{
			/***
			cCssClass =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					CSS_CLASS_ATTR,
					DEFAULT_CSS_CLASS
					);
			***/
			// cCssClass = DEFAULT_CSS_CLASS;
			cCssClass = CSSClassNames.ACTIVE_RPT_LINK;
		}
		return cCssClass;

	}

	public Hashtable _getParameters()
	{
		if( ! fUseCache && null==cParameters )
		{
			final String kFName = "getParameters";
			final boolean kCasen = false;

			// We always create a hash, it may remain empty
			cParameters = new Hashtable();

			// Lookup the list of parameter elements
			List tmpList = JDOMHelper.findElementsByPath(
				fMainElem, PARM_PATH
				);
			// Loop through, if any; it's OK if there isn't
			if( null!=tmpList && tmpList.size() > 0 )
			{
				for( int i = 0; i < tmpList.size(); i++ )
				{
					Element elem = (Element) tmpList.get( i );

					// First obsess about the parameter name
					String origKey = JDOMHelper.getStringFromAttributeTrimOrNull(
						elem, PARM_NAME_ATTR
						);
					if( null==origKey )
					{
						errorMsg( kFName,
							"Parameter # " + (i+1) + " has no name, skipping."
							);
						continue;
					}
					// Normalize if needed
					String key = kCasen ? origKey : origKey.toLowerCase();
					// Check the hash
					if( cParameters.containsKey( key ) )
					{
						errorMsg( kFName,
							"Parameter # " + (i+1) + "/\"" + key + "\""
							+ " is a duplicate paramter name, skipping."
							);
						continue;
					}

					// Obsess about the value
					String value = JDOMHelper.getStringFromAttributeTrimOrNull(
						elem, PARM_VALUE_ATTR
						);
					if( null==value )
					{
						errorMsg( kFName,
							"Parameter # " + (i+1) + "/\"" + key + "\""
							+ " has a null/empty value, skipping."
							);
						continue;
					}

					// OK, add it
					cParameters.put( key, value );
				}
			}

		}
		return cParameters;
	}







	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fMainApp;
	}
	nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}

	String getMainAppURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}


	boolean fSearchViaOurProxy;

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
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
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



	// private JDOMHelper fMainElem;
	private Element fMainElem;
	private nie.sn.SearchTuningApp _fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;
	private String fShortReportName;

	boolean fUseCache;
	boolean fHaveSeenDollarSigns;
	String cTitle;
	String _cLinkText;
	String cURL;
	String cTarget;
	String cQueryField;

	String cCssClass;
	Hashtable cParameters;
	String cScreenName;
	String _cMode;
	String cParmCGIName;
	String _cParmValue;



	public static final String MAIN_ELEM_NAME = "link";
	public static final String TITLE_PATH = "title";

	public static final String CSS_CLASS_ATTR = "css_class";
	public static final String LINK_TEXT_PATH = "text";
	public static final String PARM_PATH = "parameter";
	public static final String PARM_NAME_ATTR = "name";
	public static final String PARM_VALUE_ATTR = "value";

	public static final String _DEFAULT_CSS_CLASS =
		CSSClassNames.ACTIVE_RPT_LINK;

	public static final String UI_MODE_ADD = "add";
	public static final String UI_MODE_EDIT = "edit";
	public static final String UI_MODE_DELETE = "delete";
	public static final String UI_MODE_VIEW = "view";

	static HashSet kValidModes;

	static {
		kValidModes = new HashSet();
		kValidModes.add(UI_MODE_ADD);
		kValidModes.add(UI_MODE_EDIT);
		kValidModes.add(UI_MODE_DELETE);
		kValidModes.add(UI_MODE_VIEW);
	}

	// Todo: allow for is_null option
}

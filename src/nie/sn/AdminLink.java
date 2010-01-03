package nie.sn;

import java.util.*;
// import java.sql.*;
import org.jdom.Element;
// import org.jdom.Comment;
import nie.core.*;
import nie.sn.SnRequestHandler;
import nie.sn.CSSClassNames;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class AdminLink
{
	private final static String kClassName = "AdminLink";

	public AdminLink(
		Element inLinkDefinitionElement,
		nie.sn.SearchTuningConfig inMainConfig
	)
		throws SearchTuningConfigException
	{
		this( inLinkDefinitionElement, inMainConfig, true );
	}

	public AdminLink(
		Element inLinkDefinitionElement,
		// nie.sn.SearchTuningApp inMainApp
		nie.sn.SearchTuningConfig inMainConfig,
		boolean inDoWarnAboutMissingData
		)
			throws SearchTuningConfigException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new SearchTuningConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// we also need a report definition
		if( null==inLinkDefinitionElement )
			throw new SearchTuningConfigException( kExTag
				+ "Null XML report definition passed in."
				);
		fMainElem = inLinkDefinitionElement;

		// Fill in the cache
		initCachedFields( inDoWarnAboutMissingData );

	}


	public AdminLink(
		SearchTuningConfig inMainConfig,
		String inCommand,
		String inLinkText,
		String optLinkTitle,
		String optCssClass,
		String optParmName,
		String optParmDefaultValue
		)
			throws SearchTuningConfigException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new SearchTuningConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		cShortReportName = NIEUtil.trimmedStringOrNull( inCommand );
		if( null==cShortReportName )
			throw new SearchTuningConfigException( kExTag
				+ "Null command passed in."
				);

		cLinkText = NIEUtil.trimmedStringOrNull( inLinkText );
		if( null==cLinkText )
			throw new SearchTuningConfigException( kExTag
				+ "Null link text passed in."
				);

		// Some optional stuff
		cTitle = NIEUtil.trimmedStringOrNull( optLinkTitle );
		cCssClass = NIEUtil.trimmedStringOrNull( optCssClass );

		cSingleParmName = NIEUtil.trimmedStringOrNull( optParmName );
		if( null!=cSingleParmName && IS_CASEN )
			cSingleParmName = cSingleParmName.toLowerCase();
		String tmpParmValue = NIEUtil.trimmedStringOrNull( optParmDefaultValue );
		if( null!=cSingleParmName && null!=tmpParmValue ) {
			cParameters = new Hashtable();
			cParameters.put( cSingleParmName, tmpParmValue );
		}

		fMainElem = null;

		// Fill in the cache
		// initCachedFields();
		fUseCache = true;

	}


	private void initCachedFields(
			boolean inDoWarnAboutMissingData
		)
		throws SearchTuningConfigException
	{
		final String kFName = "initCachedFields";
		final String kExTag = kClassName + '.' + kFName + ": ";

		fUseCache = false;

		if( null == getLinkText() )
			if( inDoWarnAboutMissingData )
				throw new SearchTuningConfigException( kExTag
					+ "No link text given for this link."
					);

		getTitle();
		// getCssClass();

		getCommand();

		// Cache the paramter hash
		getCachedParameters();

		fUseCache = true;
	}


	public Element generateRichLink(
		AuxIOInfo inRequest, String inAdminCommand //, boolean inIsMenuLink
		)
	{
		return generateRichLink( inRequest, inAdminCommand, /*inIsMenuLink,*/ null, null, null );
	}

	public Element generateRichLink(
		AuxIOInfo inRequest,
		String inAdminCommand,
		String optFilterName
		// boolean inIsMenuLink
		)
	{
		return generateRichLink( inRequest, inAdminCommand, /*inIsMenuLink,*/ null, null, null );
	}

	// TODO: revisit this code  :-(
	public Element generateRichLink(
		AuxIOInfo inRequest,
		String inAdminCommand,
		// boolean inIsMenuLink,
		String optNewParmValue,
		Hashtable optVarsHash,
		String optNewLinkTextValue
		)
	{
		final String kFName = "generateRichLink(2)";

		// Start creating a repository for link info
		AuxIOInfo linkInfo = new AuxIOInfo();
		linkInfo.setBasicURL( getMainAppURL() );


		// List fieldsToExclude = new Vector();
		Set fieldsToExclude = new HashSet();
		boolean didParms = false;
		if( ! fHaveSeenDollarSigns ) {
			// Add the paramters from this link, and make sure
			// that we don't leave any old ones leftover
			Hashtable parms = getCachedParameters();
			// The fields we will NOT copy over
			// If we got some parameters
			if( null!=parms ) {
				didParms = true;
				List keys = new Vector( parms.keySet() );
				// For each parameter
				for( Iterator it = keys.iterator(); it.hasNext(); )
				{
					String key = (String) it.next();
					String value = (String) parms.get( key );
	
					// Did we want to override one of the parameters?
					// If yes, we just swap in the value passed in
					// for the value that we had from before
					if( null!=optNewParmValue
						&& null!=cSingleParmName && cSingleParmName.equals(key)
					) {
						value = optNewParmValue;
						optNewParmValue = null;
					}
	
					linkInfo.addCGIField( key, value );
					fieldsToExclude.add( key );

				}		
			}
		}
		// Else we have seen dollar signs, indicating dynamic variables
		else {

			// Give a warning, we can't handle this right now
			if( null!=optNewParmValue || null!=cSingleParmName )
				warningMsg( kFName,
					"Can't combine method parameters with dynamic XML parameters"
					+ " Ignoring passed in \"" + optNewParmValue + "\" = \"" + cSingleParmName + "\""
					);			


			// Lookup the list of parameter elements
			List parmList = JDOMHelper.findElementsByPath(
				fMainElem, PARM_PATH
				);
			// Loop through, if any; it's OK if there isn't
			if( null!=parmList && parmList.size() > 0 )
			{
				didParms = true;
				// For each parameter element
				for( int i = 0; i < parmList.size(); i++ )
				{
					Element elem = (Element) parmList.get( i );

					// First obsess about the parameter name
					String origKey = JDOMHelper.getStringFromAttributeTrimOrNull(
						elem, PARM_NAME_ATTR
						);
					if( null==origKey ) {
						errorMsg( kFName,
							"Parameter # " + (i+1) + " has no name, skipping."
							);
						continue;
					}
					// Normalize if needed
					String key = IS_CASEN ? origKey : origKey.toLowerCase();

					// No matter what, we don't want this copied over
					fieldsToExclude.add( key );


					// Obsess about the value
					// String value = JDOMHelper.getStringFromAttributeTrimOrNull(
					String value = JDOMHelper.getStringFromAttribute(
						elem, PARM_VALUE_ATTR
						);

					/*** No, w're not going to allow this
							^^^ See the warning we issue above
					// Did we want to override one of the parameters?
					if( null!=optNewParmValue
						&& null!=cSingleParmName && cSingleParmName.equals(key)
					) {
						value = optNewParmValue;
						optNewParmValue = null;
					}
					***/


					// There is a big difference between a true null
					// and an empty string
					if( null==value ) {
						value = nie.sr2.ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
					}
					else {
						String oldValue = value;
						value = NIEUtil.markupStringWithVariables(
							value,			// String inSourceString,
							optVarsHash,	// Hashtable inValues,
							false,			// boolean inCasen,
							true,			// boolean inReturnNullOnError,
							true,			// boolean inDisplayErrors,
							true			// boolean inDisplayNotFoundErrors
							);
						// Sanity check
						if( null==value ) {
							errorMsg( kFName,
								"Unable to do variable substitution."
								+ " orig value = \"" + oldValue + "\""
								+ ", var hash = " + optVarsHash + "\""
								+ " Returning a null element."
								);
							return null;
						}
					}

					// Add the value
					linkInfo.addCGIField( key, value );

				}	// End for each parameter element
			}	// End if we did find parameters

		}	// End Else we have seen dollar signs, indicating dynamic variables

		// a double check to make sure we added the possible override parameter
		// even if there was no parms hash
		if( ! fHaveSeenDollarSigns && ! didParms
			&& null!=optNewParmValue && null!=cSingleParmName
			)
		{
			linkInfo.addCGIField( cSingleParmName, optNewParmValue );
			fieldsToExclude.add( cSingleParmName );
		}

		// And if we're doing something like a menu link there's probably
		// a bunch of stuff we DON't want
		// if( inIsMenuLink )
			fieldsToExclude.addAll( nie.sr2.ReportConstants.fMiscReportFields );

		// If there's a request, copy everything else in
		if( null!=inRequest )
			linkInfo.copyInCGIFields( inRequest, fieldsToExclude );

		// set the command
		linkInfo.setOrOverwriteCGIField(
			SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD,
			inAdminCommand
			);
		// set the context
		linkInfo.setOrOverwriteCGIField(
			SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD,
			SnRequestHandler.SN_CONTEXT_ADMIN
			);

		// Get the full URL
		String href = linkInfo.getFullCGIEncodedURL();

		Element outElem = new Element( "a" );
		outElem.setAttribute( "target", "_blank" );
		// outElem class
		// outElem.setAttribute(
		//	"class", ReportDispatcher.NIE_REPORT_LINK_CSS_CLASS
		//	);
		if( null != getCssClass( /*inIsMenuLink*/ ) )
			outElem.setAttribute(
				"class", getCssClass( /*inIsMenuLink*/ )
				);
		// Add the link
		outElem.setAttribute( "href", href );
		if( null != getTitle() )
			outElem.setAttribute( "title", getTitle() );
		// Add the link text
		String linkText = optNewLinkTextValue;
		if( null==linkText )
			linkText = getLinkText();
		outElem.addContent( linkText );

		// Return the <a> tag
		return outElem;
	}

	/***
	// For a specific Term
	public void generateLinkListingCompact(
		AuxIOInfo ioRequestObject,
		String inTopLinkText,
		String inBottomLinkText,
		String inOperation,
		String inParmValue,
		Element inTopRow, Element inBottomRow,
		int optTopRowSpan
		)
			throws SearchTuningConfigException
	{
		final String kFName = "generateLinkElement(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==ioRequestObject )
			throw new SearchTuningConfigException( kExTag
				+ "Null request object passed in."
				);

		inOperation = NIEUtil.trimmedLowerStringOrNull( inOperation );
		if( null==inOperation )
			throw new SearchTuningConfigException( kExTag
				+ "Null operation passed in."
				);
		if( ! kValidOperations.contains( inOperation ) )
			throw new SearchTuningConfigException( kExTag
				+ "Invalid operation \"" + inOperation + "\" passed in."
				+ " Valid values are:" + kValidOperations
				);

		inParmValue = NIEUtil.trimmedStringOrNull(inParmValue);
		if( ! inOperation.equals( UI_OPERATION_ADD ) 
				&& ! inOperation.equals( UI_OPERATION_FORMGEN )
				&& null==inParmValue
			)
			throw new SearchTuningConfigException( kExTag
				+ "Null key value passed in."
				);


		// Construct the link
		// =========================================
		try {

			// Start creating a repository for link info
			AuxIOInfo linkInfo = new AuxIOInfo();
			String uiURL = getMainConfig().getUIRequestDispatcher().getMainUiUrl();
			// We need the /ui/ScreenName.cgi suffix (/ui/ added getMainUiUrl abve)
			// uiURL += cScreenName + ".cgi";
			uiURL += cScreenName + UI_SCREEN_EXTENSION;
			linkInfo.setBasicURL( uiURL );

			// We'll be copying over cgi fields in a bit, but there's some
			// we won't want to copy over
			List fieldsToExclude = new Vector();

			// Tell them add, edit, etc.
			linkInfo.addCGIField( OPERATION_CGI_FIELD, inOperation );
			fieldsToExclude.add( OPERATION_CGI_FIELD );

			// Tell them we are generating a form (vs processing a submission)
			linkInfo.addCGIField( MODE_CGI_FIELD, UI_MODE_FORMGEN );
			fieldsToExclude.add( MODE_CGI_FIELD );

			// The main param, if any
			if( null!=inParmValue )
				linkInfo.addCGIField( cParmCGIName, inParmValue );
			fieldsToExclude.add( cParmCGIName );

			// Just to double check, there a couple fields we KNOW
			// we don't want copied over (can be redundant, but that's OK)
			fieldsToExclude.add( MAP_ID_FORMGEN_CGI_FIELD );
			fieldsToExclude.add( TERM_FORMGEN_CGI_FIELD );

			// We want reset the return path
			fieldsToExclude.add( UILink.RETURN_URL_CGI_FIELD );

			// Clear out all report cruft
			fieldsToExclude.addAll( nie.sr2.ReportConstants.fMiscReportFields );


			// Copy over the stuff we do want
			// This will pickup the password
			// TODO: this will also copy over some BS from the report...
			linkInfo.copyInCGIFields( ioRequestObject, fieldsToExclude );


			// Get the full URL
			String href = linkInfo.getFullCGIEncodedURL();

			Element outElem = new Element( "a" );
			// outElem class
			// outElem.setAttribute(
			//	"class", ReportDispatcher.NIE_REPORT_LINK_CSS_CLASS
			//	);
			if( null!=getCssClass() )
				outElem.setAttribute(
					// "class", getCssClass()
					// "class", CSSClassNames.MENU_CELL
					"class", CSSClassNames.ACTIVE_MENU_LINK
					);

			// Add the link
			outElem.setAttribute( "href", href );
			if( null != getTitle() )
				outElem.setAttribute( "title", getTitle() );
			// Add the link text
			outElem.addContent( inBottomLinkText );

			Element headerCell = JDOMHelper.findOrCreateElementByPath(
				inTopRow,
				"th[+]"
				+ ( optTopRowSpan > 1 ? "/@colspan=" + optTopRowSpan : "" )
				+ "/@align=center" // /@class=" + CSSClassNames.CONTAINER_CELL
				+ "/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
				,
				true
				);
			headerCell.addContent( inTopLinkText );


			Element linkCell = JDOMHelper.findOrCreateElementByPath(
				inBottomRow,
				"td[+]/@align=center" // /@class=" + CSSClassNames.CONTAINER_CELL
				+ "/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
				,
				true
				);
			linkCell.addContent( outElem );

		}
		catch( Exception e )
		{
			throw new SearchTuningConfigException( kExTag
				+ "Error preparing UI link: " + e
				);
		}

	}
	***/



	public int getNDaysIfApplicable()
	{
		return cNDays;
	}

	public String getTitle( /*Hashtable inValuesHash*/ )
	{
		if( ! fUseCache && null==cTitle )
		{
			final String kFName = "getTitle";
			cTitle = JDOMHelper.getTextByPathTrimOrNull(
				fMainElem, TITLE_PATH
				);
			// if( null!=cTitle && cTitle.indexOf( '$' ) >= 0 )
			//	fHaveSeenDollarSigns = true;
		}
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


	public String getCommand()
	{
		if( ! fUseCache && null==cShortReportName )
		{
			final String kFName = "getCommand";
			cShortReportName = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINKED_COMMAND_ATTR
				);
		}
		return cShortReportName;

	}


	public String getLinkText( /*Hashtable inValuesHash*/ )
	{
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



	String getCssClass( /*boolean inIsMenuLink*/ )
	{
		// if( inIsMenuLink )
			return CSSClassNames.ACTIVE_MENU_LINK;
		// else
		//	return CSSClassNames.ACTIVE_RPT_LINK;

		/***
		if( ! fUseCache && null == cCssClass )
		{
			cCssClass =
				JDOMHelper.getStringFromAttributeOrDefaultValue(
					fMainElem,
					CSS_CLASS_ATTR,
					CSSClassNames.ACTIVE_RPT_LINK
					);
		}
		return cCssClass;
		***/

	}

	public boolean getAppearsToHaveDynamicVariables()
	{
		return fHaveSeenDollarSigns;
	}

	public Hashtable getCachedParameters()
	{
		if( ! fUseCache && null==cParameters )
		{
			final String kFName = "getParameters";
			// final boolean kCasen = false;

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
					String key = IS_CASEN ? origKey : origKey.toLowerCase();
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

					// NO CACHING IF ANY DOLLAR SIGNS
					if( value.indexOf('$') >=0 ) {
						fHaveSeenDollarSigns = true;
						// Kill the cache
						cParameters = null;
						infoMsg( kFName,
							"No caching of parameters because a dollar sign was seen in paramter "
							+ key
							);
						return null;
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
	private String cShortReportName;

	boolean fUseCache;
	boolean fHaveSeenDollarSigns;
	String cTitle;
	String cLinkText;
	String cCssClass;
	Hashtable cParameters;
	String cSingleParmName;
	int cNDays;


	public static final boolean IS_CASEN = false;

	public static final String MAIN_ELEM_NAME = "link";
	public static final String TITLE_PATH = "title";

	public static final String CSS_CLASS_ATTR = "css_class";
	public static final String LINK_TEXT_PATH = "text";
	public static final String PARM_PATH = "parameter";
	public static final String PARM_NAME_ATTR = "name";
	public static final String PARM_VALUE_ATTR = "value";
	// When we do string varaible substitution we use a double
	// layer hash, so this is the prefix name
	public static final String RAW_FIELDS_HASH_NAME = "raw_fields";

	public static final String _DEFAULT_CSS_CLASS =
		CSSClassNames.ACTIVE_RPT_LINK;

	public static final String _LINKED_REPORT_ATTR = null; // XMLDefinedField.LINKED_REPORT_ATTR;
	public static final String LINKED_COMMAND_ATTR = "command";

	// Todo: allow for is_null option
}

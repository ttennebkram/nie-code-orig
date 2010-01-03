package nie.webui;

import java.util.*;
// import java.sql.*;
import org.jdom.Element;
// import org.jdom.Comment;
import nie.core.*;
import nie.sn.CSSClassNames;
import nie.sr2.ReportConstants;
// import nie.webui.xml_screens.CreateMapForm;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class UILink
{
	private final static String kClassName = "UILink";


	public UILink(
		Element inLinkDefinitionElement,
		// nie.sn.SearchTuningApp inMainApp
		nie.sn.SearchTuningConfig inMainConfig
		)
			throws UIConfigException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new UIConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// we also need a report definition
		if( null==inLinkDefinitionElement )
			throw new UIConfigException( kExTag
				+ "Null XML report definition passed in."
				);
		fMainElem = inLinkDefinitionElement;

		// Fill in the cache
		initCachedFields();

		throw new UIConfigException( kExTag + "This constructor not currently supported.");
	}

	public UILink(
		nie.sn.SearchTuningConfig inMainConfig,
		String inScreenName,
		// String inLinkText,
		// String inMode,
		String inParmCGIName,
		// String inParmValue,
		String optLinkTitleText,
		String optCssClass
		)
			throws UIConfigException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new UIConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// Fill in the cache
		// initCachedFields();

		inScreenName = NIEUtil.trimmedStringOrNull(inScreenName);
		if( null==inScreenName )
			throw new UIConfigException( kExTag
				+ "Null UI Screen name passed in."
				);
		cScreenName = inScreenName;

		/***
		inLinkText = NIEUtil.trimmedStringOrNull(inLinkText);
		if( null==inLinkText )
			throw new UIConfigException( kExTag
				+ "Null link text passed in."
				);
		cLinkText = inLinkText;
		***/

		inParmCGIName = NIEUtil.trimmedStringOrNull(inParmCGIName);
		if( null==inParmCGIName )
//			throw new UIConfigException( kExTag
//				+ "Null parameter name passed in."
//				);
			infoMsg( kFName, "Null paramer name passed in." );
		cParmCGIName = inParmCGIName;

		// cParmValue = NIEUtil.trimmedStringOrNull(inParmValue);

		/***
		inMode = NIEUtil.trimmedLowerStringOrNull(inMode);
		if( null==inMode )
			throw new UIConfigException( kExTag
				+ "Null mode passed in."
				);
		if( ! kValidModes.contains( inMode ) )
			throw new UIConfigException( kExTag
				+ "Invalid mode \"" + inMode + "\" passed in."
				+ " Valid values are:" + kValidModes
				);
		if( ! inMode.equals( UI_MODE_ADD ) && null==cParmValue )
			throw new UIConfigException( kExTag
				+ "Null key value passed in."
				);
		cMode = inMode;
		***/


		cTitle = NIEUtil.trimmedStringOrNull(optLinkTitleText);


	}

	private void initCachedFields()
		throws UIConfigException
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
			throws UIException, UIConfigException
	{
		final String kFName = "generateLinkElement(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==ioRequestObject )
			throw new UIConfigException( kExTag
				+ "Null request object passed in."
				);

		inOperation = NIEUtil.trimmedLowerStringOrNull( inOperation );
		if( null==inOperation )
			throw new UIConfigException( kExTag
				+ "Null operation passed in."
				);
		if( ! kValidOperations.contains( inOperation ) )
			throw new UIException( kExTag
				+ "Invalid operation \"" + inOperation + "\" passed in."
				+ " Valid values are:" + kValidOperations
				);

		inParmValue = NIEUtil.trimmedStringOrNull(inParmValue);
		if( ! inOperation.equals( UI_OPERATION_ADD ) 
				&& ! inOperation.equals( UI_OPERATION_FORMGEN )
				&& null==inParmValue
			)
			throw new UIException( kExTag
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
			// We'll copy in the version we DO want
			linkInfo.addCGIField( OPERATION_CGI_FIELD, inOperation );
			// And then later keep them from overwriting it
			fieldsToExclude.add( OPERATION_CGI_FIELD );

			// Tell them we are generating a form (vs processing a submission)
			// The one we DO want
			linkInfo.addCGIField( MODE_CGI_FIELD, UI_MODE_FORMGEN );
			// and ignore any later ones
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
				+ ( optTopRowSpan > 1 ? "/@colspan=" + optTopRowSpan : "" )				+ "/@align=center" // /@class=" + CSSClassNames.CONTAINER_CELL
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
			throw new UIException( kExTag
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

	// For a specific Term
	public Element generateLinkElement(
		AuxIOInfo ioRequestObject,
		String inLinkText,
		String inOperation,
		String inParmValue
		)
			throws UIException, UIConfigException
	{
		final String kFName = "generateLinkElement(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		if( null==ioRequestObject )
			throw new UIConfigException( kExTag
				+ "Null request object passed in."
				);
	
		inLinkText = NIEUtil.trimmedStringOrNull(inLinkText);
		if( null==inLinkText )
			throw new UIConfigException( kExTag
				+ "Null link text passed in."
				);
	
		inOperation = NIEUtil.trimmedLowerStringOrNull( inOperation );
		if( null==inOperation )
			throw new UIConfigException( kExTag
				+ "Null operation passed in."
				);
		if( ! kValidOperations.contains( inOperation ) )
			throw new UIException( kExTag
				+ "Invalid operation \"" + inOperation + "\" passed in."
				+ " Valid values are:" + kValidOperations
				);
	
		inParmValue = NIEUtil.trimmedStringOrNull(inParmValue);
		if( ! inOperation.equals( UI_OPERATION_ADD ) 
				&& ! inOperation.equals( UI_OPERATION_FORMGEN )
				&& null==inParmValue
			)
			throw new UIException( kExTag
				+ "Null key value passed in."
				);
	
	
		// Construct the link
		// =========================================
		try {
	
			// Start creating a repository for link info
			AuxIOInfo linkInfo = new AuxIOInfo();
			String uiURL = getMainConfig().getUIRequestDispatcher().getMainUiUrl();
			// We need the /ui/ScreenName.cgi suffix (/ui/ added getMainUiUrl abve)
			uiURL += cScreenName + UI_SCREEN_EXTENSION;
			linkInfo.setBasicURL( uiURL );

			// We'll be copying over cgi fields in a bit, but there's some
			// we won't want to copy over
			// List fieldsToExclude = new Vector();
	
			// Tell them add, edit, etc.
			linkInfo.addCGIField( OPERATION_CGI_FIELD, inOperation );
			// fieldsToExclude.add( OPERATION_CGI_FIELD );
	
			// Tell them we are generating a form (vs processing a submission)
			linkInfo.addCGIField( MODE_CGI_FIELD, UI_MODE_FORMGEN );
			// fieldsToExclude.add( MODE_CGI_FIELD );
	
			// The main param, if any
			if( null!=inParmValue )
				// linkInfo.addCGIField( cParmCGIName, inParmValue );
				linkInfo.setOrOverwriteCGIField( cParmCGIName, inParmValue );
			// fieldsToExclude.add( cParmCGIName );
	
			// Just to double check, there a couple fields we KNOW
			// we don't want copied over (can be redundant, but that's OK)
			// fieldsToExclude.add( MAP_ID_FORMGEN_CGI_FIELD );
			// fieldsToExclude.add( TERM_FORMGEN_CGI_FIELD );
	
	
			// Copy over the stuff we do want
			// This will pickup the password
			// TODO: this will also copy over some BS from the report...
			// linkInfo.copyInCGIFields( ioRequestObject, fieldsToExclude );
			linkInfo.copyInCGIFields( ioRequestObject, ReportConstants.fMiscReportFields );
	
			// Get the full URL
			String href = linkInfo.getFullCGIEncodedURL();
	
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
	
			// Return the <a> tag
			return outElem;
		}
		catch( Exception e )
		{
			throw new UIException( kExTag
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


	// For a specific Map ID
	public Element generateLinkElement(
		AuxIOInfo ioRequestObject,
		String inLinkText,
		String inOperation,
		int inMapID
		)
			throws UIException, UIConfigException
	{
		return generateLinkElement(
			ioRequestObject,
			inLinkText, inOperation, ""+inMapID
			);
	}



	// For a specific Map ID
	public Element _generateLinkElement(
		AuxIOInfo ioRequestObject,
		String inLinkText,
		String inOperation,
		int inMapID
		)
			throws UIException, UIConfigException
	{
		final String kFName = "generateLinkElement(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null==ioRequestObject )
			throw new UIConfigException( kExTag
				+ "Null request object passed in."
				);

		inLinkText = NIEUtil.trimmedStringOrNull(inLinkText);
		if( null==inLinkText )
			throw new UIConfigException( kExTag
				+ "Null link text passed in."
				);

		inOperation = NIEUtil.trimmedLowerStringOrNull( inOperation );
		if( null==inOperation )
			throw new UIConfigException( kExTag
				+ "Null mode passed in."
				);
		if( ! kValidOperations.contains( inOperation ) )
			throw new UIException( kExTag
				+ "Invalid operation \"" + inOperation + "\" passed in."
				+ " Valid values are:" + kValidOperations
				);

		if( ! inOperation.equals( UI_OPERATION_ADD ) && inMapID < 1 )
			throw new UIException( kExTag
				+ "Invalid map ID " + inMapID + " passed in."
				);


		// Construct the link
		// =========================================
		try {

			// Start creating a repository for link info
			AuxIOInfo linkInfo = new AuxIOInfo();
			String uiURL = getMainConfig().getUIRequestDispatcher().getMainUiUrl();
			// We need the /ui/ScreenName.cgi suffix (/ui/ added getMainUiUrl abve)
			uiURL += cScreenName + ".cgi";
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
			if( inMapID >= 1 )
				linkInfo.addCGIField( cParmCGIName, ""+inMapID );
			fieldsToExclude.add( cParmCGIName );

			// Just to double check, there a couple fields we KNOW
			// we don't want copied over (can be redundant, but that's OK)
			fieldsToExclude.add( MAP_ID_FORMGEN_CGI_FIELD );
			fieldsToExclude.add( TERM_FORMGEN_CGI_FIELD );


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
					"class", getCssClass()
					);

			// Add the link
			outElem.setAttribute( "href", href );
			if( null != getTitle() )
				outElem.setAttribute( "title", getTitle() );
			// Add the link text
			outElem.addContent( inLinkText );

			// Return the <a> tag
			return outElem;
		}
		catch( Exception e )
		{
			throw new UIException( kExTag
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
	String cCssClass;
	Hashtable cParameters;
	String cScreenName;
	String _cMode;
	String cParmCGIName;
	String _cParmValue;


	public static final String CLASSIC_CREATE_MAP_UI_SCREEN = "CreateMapForm";

	public static final String UI_SCREEN_EXTENSION = ".cgi";

	public static final String MAP_ID_FORMGEN_CGI_FIELD = "map_id";
	public static final String TERM_FORMGEN_CGI_FIELD = "term";


	public static final String MAIN_ELEM_NAME = "link";
	public static final String TITLE_PATH = "title";

	public static final String CSS_CLASS_ATTR = "css_class";
	public static final String LINK_TEXT_PATH = "text";
	public static final String PARM_PATH = "parameter";
	public static final String PARM_NAME_ATTR = "name";
	public static final String PARM_VALUE_ATTR = "value";

	public static final String RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_SELF_MARKER = "(self)";
	public static final String OPERATION_CGI_FIELD = "operation";
	public static final String MODE_CGI_FIELD = "mode";


	public static final String _DEFAULT_CSS_CLASS =
		CSSClassNames.ACTIVE_RPT_LINK;

	public static final String UI_OPERATION_ADD = "add";
	public static final String UI_OPERATION_EDIT = "edit";
	public static final String _UI_OPERATION_REDISPLAY_EDIT = "redisplay";
	public static final String UI_OPERATION_DELETE = "delete";
	public static final String UI_OPERATION_VIEW = "view";
	public static final String _UI_OPERATION_CONFIRM_VIEW = "confirm";
	public static final String UI_OPERATION_FORMGEN = "formgen";

	public static HashSet kValidOperations;

	static {
		kValidOperations = new HashSet();
		kValidOperations.add(UI_OPERATION_ADD);
		kValidOperations.add(UI_OPERATION_EDIT);
		kValidOperations.add(UI_OPERATION_DELETE);
		kValidOperations.add(UI_OPERATION_VIEW);
		kValidOperations.add(UI_OPERATION_FORMGEN);
	}

	public static final String UI_MODE_FORMGEN = "formgen";
	public static final String UI_MODE_SUBMIT = "submit";
	public static final String UI_MODE_COMMIT = "commit";
	public static final String UI_MODE_CANCEL = "cancel";
	public static final String UI_MODE_REDISPLAY = "redisplay";

	public static HashSet kValidModes;
	static {
		kValidModes = new HashSet();
		kValidModes.add(UI_MODE_FORMGEN);
		kValidModes.add(UI_MODE_SUBMIT);	// Will be turned into commit or cancel
		kValidModes.add(UI_MODE_COMMIT);
		kValidModes.add(UI_MODE_CANCEL);
		kValidModes.add(UI_MODE_REDISPLAY);
	}

	public static final String META_REDIR_SCREEN = "MetaRedirector";
	public static final String META_CGI_FIELD = "meta_field";
	public static final String META_CGI_FIELD_SCREEN_PARM = "screen";

	// Todo: allow for is_null option
}

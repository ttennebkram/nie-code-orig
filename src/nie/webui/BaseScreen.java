package nie.webui;

import java.util.*;
import java.sql.*;

import org.jdom.Element;
import org.jdom.Comment;
import org.jdom.xpath.*;
import org.jdom.JDOMException;

import nie.core.*;
import nie.sn.CSSClassNames;
import nie.sn.SearchTuningConfig;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class BaseScreen implements UIScreenInterface
{

	public String kClassName() {
		return "BaseScreen";
	}

	protected String kFullClassName() {
		return "nie.webui.BaseScreen";
	}



	public void _addHR( Element inContentTable, int inColumnCount )
		throws UIConfigException, UIException
	{
		final String kFName = "addHR";
		final String kExTag = kClassName() + '.' + kFName + ": ";
	
		if( null==inContentTable )
			throw new UIConfigException( kExTag +
				"Null parent content container passed in."
				);
		if( inColumnCount < 1 )
			throw new UIConfigException( kExTag +
				"Invalid column span/count=" + inColumnCount + " passed in; must be >= 1."
				);
	
		// Now add a horizontal rule at the bottom of the table
		// First the row that will carry it
		Element hrRow = new Element( "tr" );
		// mainContentTable.addContent( hrRow );
		inContentTable.addContent( hrRow );
		// the cell in the row
		Element hrCell = new Element( "td" );
		hrCell.setAttribute( "class", CSSClassNames.HR_CELL );
		hrCell.setAttribute( "valign", "top" );
		// hrCell.setAttribute( "colspan", "2" );
		hrCell.setAttribute( "colspan", ""+inColumnCount );
		hrRow.addContent( hrCell );
		// the hr attribute
		Element hrElem = new Element( "hr" );
		hrElem.setAttribute( "width", "100%" );
		hrElem.setAttribute( "size", "1" );
		hrElem.setAttribute( "noshade", "1" );
		hrCell.addContent( hrElem );

		// The second HR element
		// at some point we might want to let them add a double
		// I had tried it before and didn't like it
		/***
		Element hrElemb = new Element( "hr" );
		hrElemb.setAttribute( "width", "100%" );
		hrElemb.setAttribute( "size", "1" );
		hrElemb.setAttribute( "noshade", "1" );
		hrCella.addContent( hrElemb );
		***/


	}




	public BaseScreen(
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortScreenName
		)
			throws UIConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// We'd like at least a minimal report name
		inShortScreenName = NIEUtil.trimmedStringOrNull(
			inShortScreenName
			);
		if( null==inShortScreenName )
			throw new UIConfigException( kExTag
				+ "Null/empty UI screen name passed in."
				);
		fShortScreenName = inShortScreenName;

		// we need the main app for various info
		if( null==inMainConfig )
			throw new UIConfigException( kExTag
				+ "Null application configuration object passed in"
				+ " for UI screen " + getScreenName()
				);
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		// Cache a couple values
		fUseCache = false;
		getCssStyleText();
		fUseCache = true;
	}

	protected Element prepareCSSElement() {
		// return prepareCSSElement( false );
		return prepareCSSElement( true );
	}

	// Will return null if no css
	protected Element prepareCSSElement( boolean inEmbedInComments )
	{
		Element styleElem = null;
		String css = getCssStyleText();

		if( null!=css )
		{
			// Add newlines to it for source readability
			css = NIEUtil.NL + css + NIEUtil.NL;

			// Create the style element and add the content
			styleElem = new Element( "style" );
			styleElem.setAttribute( "type", "text/css" );

			// Should we embed it in a comment?
			if( inEmbedInComments ) {
				Comment lComment = new Comment( css );
				styleElem.addContent( lComment );
			}
			// Else don't embed in a comment tag
			else {
				styleElem.addContent( css );
			}

		}

		return styleElem;
	}



	protected String _getCssStyleSheetURI()
	{
		return _DEFAULT_CSS_URI;
	}



	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// Todo: let them load their own from elsewhere
	protected String getCssStyleText()
	{
		return getMainConfig().getDefaultCssStyleTextOrNull();

		/***
		if( ! fUseCache && cCssText==null ) {
			final String kFName = "getCssStyleText";
	
			String uri = getCssStyleSheetURI();
			if( null==uri )
			{
				infoMsg( kFName,
					"No CSS URI defined, returning null."
					);
				cCssText = null;
			}
			else {
				AuxIOInfo tmpAuxInfo = new AuxIOInfo();
				tmpAuxInfo.setSystemRelativeBaseClassName(
					kFullClassName()
					);
				try
				{
					cCssText = NIEUtil.fetchURIContentsChar(
						uri,
						// getMainApp().getConfigFileURI(),
						getMainConfig().getConfigFileURI(),
						null, null,	// optUsername, optPassword,
						tmpAuxInfo
						);
				}
				catch( Exception e )
				{
					errorMsg( kFName,
						"Error opening CSS URI \"" + uri + "\"."
						+ " Returning null."
						+ " Error: " + e
						);
					cCssText = null;
				}
				// Normalize and check
				cCssText = NIEUtil.trimmedStringOrNull( cCssText );
				if( null==cCssText )
					errorMsg( kFName,
						"Null/empty default CSS style sheet contents read"
						+ " from URI \"" + uri + "\", returning null."
						);
			}

			debugMsg( kFName,
				"Got"
				+ ( null!=cCssText ? " " + cCssText.length() + " char" : " NO" )
				+ " CSS text"
				);
		}

		return cCssText;
		***/

		// Good resource on tables and CSS, from Nick Sayer
		// http://www.w3.org/TR/REC-CSS2/tables.html
		// And overall CSS info
		// http://www.w3.org/TR/REC-CSS2/cover.html#minitoc
		// Selectors / pattern matching
		// http://www.w3.org/TR/REC-CSS2/selector.html

//		Inside HTML:
//		<head>
//			...
//			<STYLE type="text/css">
//				H1 { color: blue }
//			</STYLE>
//		</head>

	}



	abstract public String getTitle( Hashtable inValuesHash );


	public boolean verifySecurityLevelAccess( AuxIOInfo inRequestInfo ) {
		final String kFName = "verifySecurityLevelAccess";
		int currentLevel = -1;
		if( null!=inRequestInfo )
			currentLevel = inRequestInfo.getAccessLevel();
		else
			errorMsg( kFName, "Null reqeust info sent in, defaulting to access level " + currentLevel );
		int requiredLevel = getRequiredAccessLevel();
		return currentLevel >=  requiredLevel;
	}
	public int getRequiredAccessLevel() {
		return ACCESS_LEVEL;
	}
	public static final int ACCESS_LEVEL = SearchTuningConfig.ADMIN_PWD_SECURITY_LEVEL;


	public abstract Element processRequest(
			AuxIOInfo inRequestObject, AuxIOInfo inResponseObject,
			boolean inDoFullPage
			)
				throws UIException
			;

	protected List getSuggestedLinksOrNull()
	{
		return null;
	}


	Element generateLinkToMainMenu(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws UIException
	{
		final String kFName = "generateMainMenuLink";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		final String kLinkText = "Return to the Main Menu";

		if( null == inRequestInfo
			|| null == inResponseInfo
			)
		{
			throw new UIException( kExTag
				+ "Null request/reponse object."
				);
		}

		// Our eventual response, barring any exceptions
		Element outElem = new Element( "a" );


		// Get the link
		////////////////////////////////////////////

		// We use AuxIOInfo to build a link
		AuxIOInfo newLinkInfo = new AuxIOInfo();
		// Prime it with the basic URL we want
		newLinkInfo.setBasicURL( getMainAppURL() );

		// Copy over existing CGI values
		// We have to say which fields we DON'T want
		List excludeFields = new Vector();
		// excludeFields.add( ReportDispatcher.REPORT_NAME_CGI_FIELD );
		excludeFields.add( START_ROW_CGI_FIELD_NAME );
		excludeFields.add( DESIRED_ROW_COUNT_CGI_FIELD_NAME );
		excludeFields.add( SORT_SPEC_CGI_FIELD_NAME );
		excludeFields.add( FILTER_NAME_CGI_FIELD_NAME );
		excludeFields.add( FILTER_PARAM_CGI_FIELD_NAME );
		// Now do the copy
		newLinkInfo.copyInCGIFields( inRequestInfo, excludeFields );

		// Now get the full href back
		String href = newLinkInfo.getFullCGIEncodedURL();

		if( null==href )
			throw new UIException( kExTag
				+ "Got back null href, this link will not be created."
				);


		// Now create the final XHTML element
		// we use <div> if not an active link
		outElem.setAttribute( "href", href );
		// outElem.setAttribute( "class", "nie_menu_link" );
		/***
		outElem.setAttribute(
			"class", UIRequestDispatcher.NIE_REPORT_LINK_CSS_CLASS
			);
		***/

		// And add the display text
		outElem.addContent( kLinkText );

		// Return the tag!
		return outElem;
	}

	// We need a base element, either the top of an HTML page, or at least the part
	// we will jam ourselves into
	// Also, we need to know the main TABLE element we are to put our new stuff into
	// Since the 2nd element is a TABLE, it should have <tr>'s added to it
	protected Element [] prepareBaseOutputTree(
		boolean inDoFullPage,
		Hashtable optVariablesHash
		)
	{

		// Whether or not to force a style element into the
		// stream, even if we're not formulating the entire page
		final boolean kForceCSS = true;

		// The two key points in the tree
		Element outElem = null;
		Element contentHanger = null;
		String title = getTitle( optVariablesHash );

		// Also, have the CSS stuff ready to go if we need it
		Element styleElem = null;
		String css = getCssStyleText();
		if( null!=css )
		{
			// Add newlines to it for source readability
			css = NIEUtil.NL + css + NIEUtil.NL;
			// Todo: this should go inside HTML comments as well
			// Create the style element and add the content
			styleElem = new Element( "style" );
			styleElem.setAttribute( "type", "text/css" );
			Comment lComment = new Comment( css );
			// styleElem.addContent( css );
			styleElem.addContent( lComment );
		}


		// If no template, build from scratch
		if( inDoFullPage )
		{
			// The eventual answer
			outElem = new Element( "html" );
			// Build up the heading
			Element headElem = new Element( "head" );
			// Hold off adding it to the main doc until we see
			// if we add anything
			boolean haveAddedAnythingToHead = false;
			// Add the CSS element, if any
			if( null!=styleElem )
			{
				headElem.addContent( styleElem );
				haveAddedAnythingToHead = true;
			}
			// Add the title, if any
			if( null!=title )
			{
				Element titleElem = new Element( "title" );
				titleElem.addContent( title );
				headElem.addContent( titleElem );
				haveAddedAnythingToHead = true;
			}
			// Only add the heading to the document if we've actually
			// put something in it
			if( haveAddedAnythingToHead )
				outElem.addContent( headElem );
	
			// Start building the body tag
			Element bodyElem = new Element( "body" );
			outElem.addContent( bodyElem );

			// We like the main layout centered
			Element mainCenterElem = new Element( "center" );
			bodyElem.addContent( mainCenterElem );
			contentHanger = mainCenterElem;
	
		}
		// Else there IS a template, so we build a much
		// shorter document
		else
		{
			// We like the main layout centered
			Element mainCenterElem = new Element( "center" );
			outElem = mainCenterElem;
			contentHanger = mainCenterElem;

			// Add the CSS element, if any
			if( null!=styleElem && kForceCSS )
			{
				contentHanger.addContent( styleElem );
			}
		}


		// Add the title, if any
		if( null!=title )
		{
			Element titleElem = new Element( "h2" );
			titleElem.setAttribute( "class", CSSClassNames.RPT_TITLE_TEXT );
			titleElem.addContent( title );
			// bodyElem.addContent( titleElem );
			contentHanger.addContent( titleElem );
		}
		// Add the subtitle, if any
		/***
		String subtitle = getSubtitleOrNull( optVariablesHash );
		if( null!=subtitle )
		{
			Element subtitleElem = new Element( "h3" );
			subtitleElem.setAttribute( "class", CSSClassNames.RPT_SUBTITLE_TEXT );
			subtitleElem.addContent( subtitle );
			// bodyElem.addContent( subtitleElem );
			contentHanger.addContent( subtitleElem );
		}
		***/

		// The main-content table helps place the
		// results grid, statistics message and
		// paging links
		Element mainContentTable = new Element( "table" );
		mainContentTable.setAttribute( "class", CSSClassNames.MAIN_CONTENT_TABLE );
		// Add it to the overall content stream
		// bodyElem.addContent( mainContentTable );
		contentHanger.addContent( mainContentTable );
		// NOTE:
		// We hold off adding rows to this, so we can put them in
		// the proper order AFTER the main loop runs

		// We send back a two item array
		Element [] answer = new Element [] { outElem, mainContentTable };
		return answer;

	}



	Hashtable getAllRequestHashes( AuxIOInfo inRequest )
	{
		final String kFName = "getAllRequestHashes";
		Hashtable outHash = new Hashtable();
		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in. Returning empty hash."
				);
			return outHash;
		}

		Hashtable hash1 = getRequestAsSingletonHash( inRequest );
		if( null == hash1 )
		{
			errorMsg( kFName,
				"Null hash returned of values. Returning empty hash."
				);
			return outHash;
		}

		// Start buidling our list
		outHash.put( REQUEST_VARS_HASH_NAME, hash1 );
		// Get the two other versions and save those
		Hashtable hash2 = NIEUtil.sqlEscapeStringHash( hash1, true );
		if( null != hash2 )
			outHash.put( REQUEST_VARS_HASH_NAME + SQL_ESC_SUFFIX,
				hash2
				);
		Hashtable hash3 = NIEUtil.htmlEscapeStringHash( hash1, true );
		if( null != hash3 )
			outHash.put( REQUEST_VARS_HASH_NAME + HTML_ESC_SUFFIX,
				hash3
				);

		return outHash;
	}

	Hashtable getRequestAsSingletonHash( AuxIOInfo inRequest )
	{
		final String kFName = "getRequestAsSingletonHash";
		Hashtable outHash = new Hashtable();
		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in. Returning empty hash."
				);
			return outHash;
		}
		List keys = inRequest.getCGIFieldKeys();
		if( null == keys )
		{
			errorMsg( kFName,
				"Null key-set from request object. Returning empty hash."
				);
			return outHash;
		}
		for( Iterator it = keys.iterator(); it.hasNext(); )
		{
			String key = (String) it.next();
			String value = inRequest.getScalarCGIField( key );
			if( null==value )	// should not be possibe
			{
				errorMsg( kFName,
					"Null value for key \"" + key + "\""
					+ " Skipping."
					);
				continue;
			}
			outHash.put( key, value );
		}

		return outHash;

	}

	protected String getScreenName()
	{
		return fShortScreenName;
	}








	public nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	protected DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}
	protected nie.sn.SearchEngineConfig getSearchEngineConfig() {
		return getMainConfig().getSearchEngine();
	}


	String getMainAppURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}




	// This gets us to the logging object
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	protected boolean stackTrace( String inFromRoutine, Exception e, String optMessage )
	{
		return getRunLogObject().stackTrace( kClassName(), inFromRoutine,
			e, optMessage
			);
	}

	protected boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}
	protected boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}
	protected boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}
	protected boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}
	protected boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}


	protected boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			inMessage
			);
	}




	private nie.sn.SearchTuningApp _fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;
	private String fShortScreenName;

	boolean fUseCache;
	boolean fHaveSeenDollarSigns;



	boolean cShouldDoVarSubst;
	String cCssText;

	Hashtable cFormFieldNameToNPath;
	Hashtable cFormFieldNameToFieldLabel;


	// Desired starting and stopping row count
	public static final String START_ROW_CGI_FIELD_NAME = "start_row";
	public static final String DESIRED_ROW_COUNT_CGI_FIELD_NAME =
		"num_rows";

	public static final String SORT_SPEC_CGI_FIELD_NAME = "sort";
	// public static final String FILTER_SPEC_CGI_FIELD_NAME = "filter";
	public static final String FILTER_NAME_CGI_FIELD_NAME = "filter";
	public static final String FILTER_PARAM_CGI_FIELD_NAME = "parm";

	// Having to do with variable hashes
	static final String REQUEST_VARS_HASH_NAME = "cgi";
	static final String SQL_ESC_SUFFIX = "_sqlesc";
	static final String HTML_ESC_SUFFIX = "_htmlesc";



	// The default number of rows
	static final int DEFAULT_DESIRED_ROW_COUNT = 25; // 25; // 25;

	// Where CSS style sheets come from
	public static final String _DEFAULT_CSS_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "style_sheets/nie2.css"
		;

	static final boolean DEFAULT_SHOULD_DO_VAR_SUBST = true;

	// Some of the class tags we use, others are hard coded if
	// used only once
	private static final String _ACTIVE_PAGING_CSS_CLASS =
		"nie_active_paging_link";
	private static final String _INACTIVE_PAGING_CSS_CLASS =
		"nie_inactive_paging_link";
	private static final String _STAT_NUMBER_CSS_CLASS =
		"nie_stat_number";
	public static final String _CONAINER_CELL_CSS_CLASS =
		"nie_container_cell";



}

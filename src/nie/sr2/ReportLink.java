package nie.sr2;

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
public class ReportLink
{
	private final static String kClassName = "ReportLink";

	public ReportLink(
		Element inLinkDefinitionElement,
		nie.sn.SearchTuningConfig inMainConfig
	)
		throws ReportConfigException
	{
		this( inLinkDefinitionElement, inMainConfig, true );
	}

	public ReportLink(
		Element inLinkDefinitionElement,
		// nie.sn.SearchTuningApp inMainApp
		nie.sn.SearchTuningConfig inMainConfig,
		boolean inDoWarnAboutMissingData
		)
			throws ReportConfigException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// we need the main app for various info
		if( null==inMainConfig )
			throw new ReportConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		// we also need a report definition
		if( null==inLinkDefinitionElement )
			throw new ReportConfigException( kExTag
				+ "Null XML link definition passed in."
				);
		fMainElem = inLinkDefinitionElement;

		if(debug) debugMsg( kFName,
		        "Link def = " + NIEUtil.NL
		        + JDOMHelper.JDOMToString( inLinkDefinitionElement, true )
		        );
		// Fill in the cache
		initCachedFields( inDoWarnAboutMissingData );

	}


	public ReportLink(
		nie.sn.SearchTuningConfig inMainConfig,
		String inReportName,
		String inLinkText,
		String optLinkTitle,
		String optCssClass,
		String optParmName,
		String optParmDefaultValue
		)
			throws ReportConfigException
	{
		final String kFName = "constructor(2)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// we need the main app for various info
		if( null==inMainConfig )
			throw new ReportConfigException( kExTag
				+ "Null application configuration object passed in."
				);
		fMainConfig = inMainConfig;

		cShortReportName = NIEUtil.trimmedStringOrNull( inReportName );
		if( null==cShortReportName )
			throw new ReportConfigException( kExTag
				+ "Null report name passed in."
				);

		cLinkText = NIEUtil.trimmedStringOrNull( inLinkText );
		if( null==cLinkText )
			throw new ReportConfigException( kExTag
				+ "Null link text passed in."
				);

		// Some optional stuff
		cTitle = NIEUtil.trimmedStringOrNull( optLinkTitle );
		cCssClass = NIEUtil.trimmedStringOrNull( optCssClass );

		cPrimaryParmName = NIEUtil.trimmedStringOrNull( optParmName );
		if( null!=cPrimaryParmName && IS_CASEN )
			cPrimaryParmName = cPrimaryParmName.toLowerCase();
		cPrimaryParmValue = NIEUtil.trimmedStringOrNull( optParmDefaultValue );
		if( null!=cPrimaryParmName && null!=cPrimaryParmValue ) {
			cParameters = new Hashtable();
			cParameters.put( cPrimaryParmName, cPrimaryParmValue );
		}

		fMainElem = null;

		// If this is nDays, cache it
		if( null!=optParmName
			&& null!=optParmDefaultValue
			&& ! optParmDefaultValue.equalsIgnoreCase(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE)
			&& optParmName.equalsIgnoreCase(ReportConstants.DAYS_OLD_CGI_FIELD_NAME)
			)
				cNDays = NIEUtil.stringToIntOrDefaultValue( optParmDefaultValue, 0, true, true );


		// Fill in the cache
		// initCachedFields();
		fUseCache = true;

	}


	private void initCachedFields(
			boolean inDoWarnAboutMissingData
		)
		throws ReportConfigException
	{
		final String kFName = "initCachedFields";
		final String kExTag = kClassName + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		fUseCache = false;

		if( null == getLinkText() )
			if( inDoWarnAboutMissingData )
				throw new ReportConfigException( kExTag
					+ "No link text given for this link."
					);

		String tmpStr = getDestReportName();
		if(debug) debugMsg( kFName, "Dest report = " + tmpStr );

		tmpStr = getTitle();
		if(debug) debugMsg( kFName, "Title = " + tmpStr );

		tmpStr = getLinkText();
		if(debug) debugMsg( kFName, "Link text = " + tmpStr );

		tmpStr = getCssClass( false );
		if(debug) debugMsg( kFName, "CSS Class = " + tmpStr );

		// Cache the paramter hash
		getCachedParameters();
		// ^^^ that also caches the primary parm name and value, if present
		if(debug) debugMsg( kFName, "Primary parm name / value = "
		        + getPrimaryParmName() + " / " + getPrimaryParmValue() );

		tmpStr = getFilterName();
		if(debug) debugMsg( kFName, "Filter = " + tmpStr );

		int tmpVal = cacheNDays();
		if(debug) debugMsg( kFName, "NDays = " + tmpVal );

		fUseCache = true;
	}


	public Element generateRichLink(
		AuxIOInfo inRequest, boolean inIsMenuLink
		)
	{
		return generateRichLink( inRequest, null, null, inIsMenuLink, null, null, null );
	}

	public Element generateRichLink(
		AuxIOInfo inRequest,
		String optReportName,
		String optFilterName,
		boolean inIsMenuLink
		)
	{
		return generateRichLink( inRequest, optReportName,
		        optFilterName, inIsMenuLink, null, null, null
		        );
	}

	// TODO: revisit this code  :-(
	public Element generateRichLink(
		AuxIOInfo inRequest,
		String optReportName,
		String optFilterName,
		boolean inIsMenuLink,
		String optNewParmValue,
		Hashtable optVarsHash,
		String optNewLinkTextValue
		)
	{
		final String kFName = "generateRichLink(2)";
		boolean debug = shouldDoDebugMsg( kFName );
	    if(debug) debugMsg( kFName,
	            "optReportName='" + optReportName + '\''
	            + ", optFilterName='" + optFilterName + '\''
	            + ", inIsMenuLink='" + inIsMenuLink + '\''
	            + ", optNewParmValue='" + optNewParmValue + '\''
	            + ", optVarsHash='" + optVarsHash + '\''
	            + ", optNewLinkTextValue='" + optNewLinkTextValue + '\''
	            );

		// Start creating a repository for link info
		AuxIOInfo linkInfo = new AuxIOInfo();
		linkInfo.setBasicURL( getMainAppURL() );

		// There may be a number CGI parameters that we WON'T want to copy forward
		Set fieldsToExclude = new HashSet();
		boolean didParms = false;
		boolean haveAddedPrimaryParm = false;
		// If no variable expansion is requested, this will be
		// a more simple link
		if( ! fHaveSeenDollarSigns ) {
		    debugMsg( kFName, "have not seen dollar signs" );
			// Add the paramters from this link, and make sure
			// that we don't leave any old ones leftover
			Hashtable cachedParms = getCachedParameters();
			// The fields we will NOT copy over
			// If we got some parameters
			if( null!=cachedParms ) {
			    debugMsg( kFName, "cached parms" );
				didParms = true;

				List keys = new Vector( cachedParms.keySet() );
				// For each parameter
				for( Iterator it = keys.iterator(); it.hasNext(); )
				{
					String key = (String) it.next();
					String value = (String) cachedParms.get( key );
	
					// Did we want to override one of the parameters?
					// If yes, we just swap in the value passed in
					// for the value that we had from before
					if( null!=getPrimaryParmName() && getPrimaryParmName().equals(key) )
					{
					    if( null!=optNewParmValue )
					    {
							value = optNewParmValue;
							optNewParmValue = null;
					    }
						haveAddedPrimaryParm = true;
					}

					debugMsg( kFName, "Cache Hash add: " + key + '=' + value );
					linkInfo.addCGIField( key, value );
					fieldsToExclude.add( key );

					/***
					// We need to point out this filter
					String filterName = getFilterName();
					if( null==filterName )
					    filterName = key;
					linkInfo.addCGIField(
						ReportConstants.FILTER_NAME_CGI_FIELD_NAME,
						filterName
						);
					fieldsToExclude.add( ReportConstants.FILTER_NAME_CGI_FIELD_NAME );
					***/

				}	// end if cached parms

				/***
				if( ! haveAddedPrimaryParm
				    && null!=getPrimaryParmName()
				    && null!=getPrimaryParmValue()
				    )
				{
					debugMsg( kFName, "post-cache Hash add: " + getPrimaryParmName() + '=' + getPrimaryParmValue() );
					linkInfo.addCGIField( getPrimaryParmName(), getPrimaryParmValue() );
					fieldsToExclude.add( getPrimaryParmName() );
				}
				***/

			}	// End if has cached parms
			else {
			    debugMsg( kFName, "no cached parms" );
			}
		}
		// Else we have seen dollar signs, indicating dynamic variables
		else {
		    debugMsg( kFName, "HAVE seen dollar signs" );

			// Give a warning, we can't handle this right now
			if( null!=optNewParmValue || null!=getPrimaryParmName() )
				warningMsg( kFName,
					"Can't combine method parameters with dynamic XML parameters"
					+ " Ignoring passed in \"" + optNewParmValue + "\" = \"" + getPrimaryParmName() + "\""
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

					if( null!=getPrimaryParmName() && getPrimaryParmName().equals(key) )
					{
					    if( null!=optNewParmValue )
					    {
							value = optNewParmValue;
							optNewParmValue = null;
					    }
						haveAddedPrimaryParm = true;
					}


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
						value = ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
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

					// haveAddedPrimaryParm = true;

					/***
					// We need to point out this filter
					linkInfo.addCGIField(
						ReportConstants.FILTER_NAME_CGI_FIELD_NAME,
						key
						);
					fieldsToExclude.add( ReportConstants.FILTER_NAME_CGI_FIELD_NAME );
					***/

				}	// End for each parameter element
			}	// End if we did find parameters

		}	// End Else we have seen dollar signs, indicating dynamic variables


		/***
		// a double check to make sure we added the possible override parameter
		// even if there was no parms hash
		if( ! fHaveSeenDollarSigns && ! didParms
			&& null!=optNewParmValue && null!=getPrimaryParmName()
			)
        ***/
		if( null!=optNewParmValue
		        && getPrimaryParmName()!=null
		        && ! haveAddedPrimaryParm )
		{
			linkInfo.addCGIField( getPrimaryParmName(), optNewParmValue );
			fieldsToExclude.add( getPrimaryParmName() );
		    if(debug) debugMsg( kFName,
			    	"double check logic: added parm from args"
			        + ": optNewParmValue=" + optNewParmValue
			    	);

		}
		else {
		    if(debug) debugMsg( kFName,
		    	"double check logic: NOT setting parm from args"
		        + ": fHaveSeenDollarSigns=" + fHaveSeenDollarSigns
		        + ", didParms=" + didParms
		        + ", haveAddedPrimaryParm=" + haveAddedPrimaryParm
		        + ", optNewParmValue=" + optNewParmValue
		        + ", getPrimaryParmName()=" + getPrimaryParmName()
		    	);
		}
		
		// Add Filter Info
		// ============================
	    // possiblitiies for filter
	    // 1: For common items like days and site, leave it off
	    // 2: can be passed in
	    // 3: Or fall back to what was set in the xml link config
	    // We need to point out the filter
	    // Let the caller tell us
		String filterName = optFilterName;
		// Or look a the config
		if( null==filterName )
		    filterName = getFilterName();
		// Or as a last ditch, use the name of the primary field
		if( null==filterName )
		    filterName = getPrimaryParmName();
		if( null!=filterName
			    && ! filterName.equalsIgnoreCase( ReportConstants.DAYS_OLD_CGI_FIELD_NAME)
			) {
		    linkInfo.setOrOverwriteCGIField(
				ReportConstants.FILTER_NAME_CGI_FIELD_NAME,
				filterName
				);
		}
		fieldsToExclude.add( ReportConstants.FILTER_NAME_CGI_FIELD_NAME );


		// And if we're doing something like a menu link there's probably
		// a bunch of stuff we DON't want
		if( inIsMenuLink )
			fieldsToExclude.addAll( ReportConstants.fMiscReportFields );

		// We know, for a fact, that we want to clear the start row stuff
		fieldsToExclude.add( ReportConstants.START_ROW_CGI_FIELD_NAME );

		// If there's a request, copy everything else in
		if( null!=inRequest )
			linkInfo.copyInCGIFields( inRequest, fieldsToExclude );

		// If a report name was given, use it
		if( null != optReportName )
			linkInfo.setOrOverwriteCGIField(
				ReportConstants.REPORT_NAME_CGI_FIELD, optReportName
				);
		else if( null != getDestReportName() )
			linkInfo.setOrOverwriteCGIField(
				ReportConstants.REPORT_NAME_CGI_FIELD, getDestReportName()
				);

		// Make sure we have the admin context and report command
		linkInfo.setOrOverwriteCGIField(
			SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD,
			SnRequestHandler.SN_CONTEXT_ADMIN
			);
		linkInfo.setOrOverwriteCGIField(
			SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD,
			SnRequestHandler.ADMIN_CONTEXT_REPORT
			);

		// Get the full URL
		String href = linkInfo.getFullCGIEncodedURL();

		Element outElem = new Element( "a" );
		// outElem class
		// outElem.setAttribute(
		//	"class", ReportDispatcher.NIE_REPORT_LINK_CSS_CLASS
		//	);
		if( null != getCssClass( inIsMenuLink ) )
			outElem.setAttribute(
				"class", getCssClass( inIsMenuLink )
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


	public String getDestReportName()
	{
		if( ! fUseCache && null==cShortReportName )
		{
			final String kFName = "getDestReportName";
			cShortReportName = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, LINKED_REPORT_ATTR
				);
		}
		return cShortReportName;
	}

	public String getPrimaryParmName()
	{
	    /*** now cached in cacheParms
	    if( ! fUseCache && null==cSingleParmName )
		{
			final String kFName = "getPrimaryParmName";
			cSingleParmName = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, PARM_NAME_ATTR
				);
			if( null!=cSingleParmName && IS_CASEN )
				cSingleParmName = cSingleParmName.toLowerCase();
		}
		***/
		return cPrimaryParmName;
	}

	public String getFilterName()
	{
	    if( ! fUseCache && null==cFilterName )
		{
			final String kFName = "getFilterName";
			cFilterName = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, PARM_NAME_ATTR
				);
			if( null==cFilterName )
			    cFilterName = getPrimaryParmName();
		}
		return cFilterName;
	}

	public String getPrimaryParmValue()
	{
	    /*** now cached in cacheParms
		if( ! fUseCache && null==cSingleParmValue )
		{
			final String kFName = "getPrimaryParmValue";
			cSingleParmValue = JDOMHelper.getStringFromAttributeTrimOrNull(
				fMainElem, PARM_VALUE_ATTR
				);
		}
		***/
		return cPrimaryParmValue;
	}

	public int cacheNDays()
	{
		// If this is nDays, cache it
		if( null!=cPrimaryParmName
			&& null!=cPrimaryParmValue
			&& ! cPrimaryParmValue.equalsIgnoreCase(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE)
			&& cPrimaryParmName.equalsIgnoreCase(ReportConstants.DAYS_OLD_CGI_FIELD_NAME)
			)
				cNDays = NIEUtil.stringToIntOrDefaultValue( cPrimaryParmValue, 0, true, true );
		return cNDays;
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



	String getCssClass( boolean inIsMenuLink )
	{
		if( inIsMenuLink )
			return CSSClassNames.ACTIVE_MENU_LINK;
		else
			return CSSClassNames.ACTIVE_RPT_LINK;

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
			final String kFName = "getCachedParameters";
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
					debugMsg( kFName, "key=\"" + key + "\"" );

					// Obsess about the value
					String value = JDOMHelper.getStringFromAttributeTrimOrNull(
						elem, PARM_VALUE_ATTR
						);

					// Pause to store primary / first parm name and value
					if( 0==i ) {
					    cPrimaryParmName = key;
					    cPrimaryParmValue = value;
					}

					if( null==value )
					{
						// errorMsg( kFName,
						//	"Parameter # " + (i+1) + "/\"" + key + "\""
						//	+ " has a null/empty value, skipping."
						//	);
						// continue;
						// Similar to dollar signs, NO CACHING
						// if we have null values, which means late binding
						cParameters = null;
						infoMsg( kFName,
								"No caching of parameters because a null value was seen in paramter "
								+ key
								);
						return null;
					}

					// If this is nDays, cache it
					if( value.equalsIgnoreCase(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE)
						&& key.equalsIgnoreCase(ReportConstants.DAYS_OLD_CGI_FIELD_NAME)
						)
							cNDays = NIEUtil.stringToIntOrDefaultValue( value, 0, true, true );

					// NO CACHING IF ANY DOLLAR SIGNS
					if( null!=value && value.indexOf('$') >=0 ) {
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
	String cPrimaryParmName;
	String cPrimaryParmValue;
	String cFilterName;
	int cNDays;


	public static final boolean IS_CASEN = false;

	public static final String MAIN_ELEM_NAME = "link";
	public static final String TITLE_PATH = "title";

	public static final String CSS_CLASS_ATTR = "css_class";
	public static final String LINK_TEXT_PATH = "text";
	public static final String PARM_PATH = "parameter";
	public static final String PARM_NAME_ATTR = "name";
	public static final String PARM_VALUE_ATTR = "value";
	public static final String FILTER_ATTR = "filter";
	// When we do string varaible substitution we use a double
	// layer hash, so this is the prefix name
	public static final String RAW_FIELDS_HASH_NAME = "raw_fields";

	public static final String _DEFAULT_CSS_CLASS =
		CSSClassNames.ACTIVE_RPT_LINK;

	public static final String LINKED_REPORT_ATTR = XMLDefinedField.LINKED_REPORT_ATTR;

	// Todo: allow for is_null option
}

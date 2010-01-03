package nie.sn;

import java.io.*;
import java.util.*;
import nie.core.*;
import nie.sr2.ReportConstants;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.*;

import com.sun.org.apache.bcel.internal.generic.FMUL;

// Todo: options to prevent fields from being copied over

/**
 * <p>Title: SearchNames</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Mark Bennett and Kevin Klop
 * @version 1.0
 */

public class SearchEngineConfig
{

	private final static String kClassName = "SearchEngineConfig";

//	// private static boolean debug = true;
//	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}
//	// ^^^ nothing currently uses this

	public SearchEngineConfig( Element inElement )
		throws SearchEngineConfigException,
			SearchTuningConfigFatalException
	{

		// create jdom element and store info
		// Sanity checks
		if( inElement == null )
			throw new SearchTuningConfigFatalException(
				"SearchEngineConfig: Constructor was passed in a NULL element."
				);

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new SearchTuningConfigFatalException(
				"SearchEngineConfig: constructor: got JDOMHelper Exception: "
				+ e );
		}
		if( fConfigTree == null )
			throw new SearchTuningConfigFatalException(
				"Got back a NULL xml tree when trying to create"
				+ " a Search Engine Configuration object."
				);

		// Init the cached settings
		reinitFieldCache();

		// Check that a couple critical items exist
		String tmpStr;

		// Query field is required
		tmpStr = getQueryField();
		if( tmpStr == null )
			throw new SearchEngineConfigException(
				"Must specifiy the host search engine's query field in config."
				+ " It's \"" + QUERY_FIELD_PATH + "\""
				+ " under the search engine configuration area."
				);

		// the URL of the host search engine is required
		tmpStr = getSearchEngineURL();
		if( tmpStr == null )
			throw new SearchTuningConfigFatalException(
				"Must specifiy the host search engine's entry/CGI URL."
				+ " It's \"" + SEARCH_ENGINE_URL_PATH + "\""
				+ " under the search engine configuration area."
				);

		// Due to its overhead, we cache the list of fields that
		// indicate no SearchNames actions should be taken
		// This is most certainly an OPTIONAL parameter(s)
		// If there are some listed, they are stored in fNoActionFieldList
		setupNoActionIndicatorFields();

		// And the rest is handled by methods
	}

	private void reinitFieldCache()
		throws SearchEngineConfigException
	{
		final String kFName = "reinitFieldCache";
		final String kExTag = kClassName + '.' + kFName + ": ";
		debugMsg( kFName, "Start" );

		mIsFreshCache = false;

		isCGIFeildsCaseSensitive();
		getIsSuggestionMarkerCaseSensitive();
		getIsSuggestionMarkerReplaced();
		getIsSuggestionMarkerNewTextInsertAfter();
		getVendor();
		getSearchEngineURL();
		getBaseURL();
		getSearchEngineMethod();
		getSearchEngineUseCarefulRedirects();
		getSearchEngineTestDriveURLFields();
		getSearchFormOptionFieldsHash();
		getQueryField();
		getSuggestionMarkerLiteralText();
		/***
		List tmpMarkers = getSuggestionMarkerLiteralText();
		if( null==tmpMarkers || tmpMarkers.isEmpty() )
			throw new SearchEngineConfigException( kExTag +
				"No suggestion marker pattern specified."
				);
		***/

		getSnippetPrefixText();
		getSnippetSuffixText();

		getResultsFormsTweakPolicy();
		_getResultsFormsJsTweakPolicy();
		getResultsFormsTweakArg1();
		getResultsFormsTweakArg2();
		getResultsDocLinksTweakPolicy();
		_getResultsDocLinksJsTweakPolicy();
		getResultsDocLinksArg1();
		getResultsDocLinksArg2();
		getResultsDocLinksCgiIdField();

		getPagingFieldOrNull();
		getDocsPerPage();
		getShouldCalcWithPageNum();
		getDocsFirstOrdinal();

		setupDocLinkMatchRulesIfAny();

		mIsFreshCache = true;

	}

	// Given a random URL we found on a search results list
	// see if it matches any of our rules
	public MatchRule findMatchingRuleOrNull( String inUrl )
	{
		if( null==fDocLinkRules || fDocLinkRules.isEmpty() )
			return null;
		for( Iterator it = fDocLinkRules.iterator(); it.hasNext(); )
		{
			MatchRule rule = (MatchRule) it.next();
			if( rule.matches(inUrl) )
				return rule;
		}
		return null;
	}
	// Which document links, if any, appearing on the results list page
	// should we try to tweak?
	void setupDocLinkMatchRulesIfAny()
		throws SearchEngineConfigException
	{
		if( ! mIsFreshCache && null==fDocLinkRules )
		{
			List ruleElems = fConfigTree.findElementsByPath( DOC_LINKS_RULES_PATH );
			if( null!=ruleElems && ! ruleElems.isEmpty() )
			{
				fDocLinkRules = new Vector();
				for( Iterator it = ruleElems.iterator(); it.hasNext(); )
				{
					Element elem = (Element) it.next();
					MatchRule rule = new MatchRule( elem );
					fDocLinkRules.add( rule );
				}
			}			
		}
	}
	List getDocLinkRulesOrNull()
	{
		return fDocLinkRules;
	}
	
	public String getQueryField()
	{
		if( ! mIsFreshCache )
		{
			// The query field
			cQueryField = fConfigTree.getTextByPathTrimOrNull( QUERY_FIELD_PATH );
		}
		return cQueryField;
	}

	public String getSearchEngineURL()
	{
		if( ! mIsFreshCache )
		{
			// The URL to direct to
			cSearchEngineURL = fConfigTree.getTextByPathTrimOrNull(
				SEARCH_ENGINE_URL_PATH
				);
		}
		return cSearchEngineURL;
	}
	public String getBaseURL()
	{
		if( ! mIsFreshCache )
		{
			// The URL to direct to
			cBaseURL = fConfigTree.getTextByPathTrimOrNull(
				BASE_URL_PATH
				);
			if( null==cBaseURL )
				cBaseURL = getSearchEngineURL();
		}
		return cBaseURL;
	}
	public String getSearchEngineMethod()
	{
	    final String kFName = "getSearchEngineMethod";
		if( ! mIsFreshCache )
		{
			// The URL to direct to
			// cSearchEngineMethod = fConfigTree.getTextByPathTrimOrNull(
			//	SEARCH_ENGINE_METHOD_PATH
			//	);
			cSearchEngineMethod = fConfigTree.getTextFromSinglePathAttrTrimOrNull(
			        SEARCH_ENGINE_URL_PATH,
					SEARCH_ENGINE_METHOD_ATTR
					);
			debugMsg( kFName, "SEARCH_ENGINE_METHOD_PATH="+SEARCH_ENGINE_METHOD_PATH );
			debugMsg( kFName, "cSearchEngineMethod="+cSearchEngineMethod );

			if( null==cSearchEngineMethod )
			    cSearchEngineMethod = DEFAULT_SEARCH_ENGINE_METHOD;
		}
		return cSearchEngineMethod;
	}
	public boolean getSearchEngineUseCarefulRedirects()
	{
	    final String kFName = "getSearchEngineUseCarefulRedirects";
		if( ! mIsFreshCache )
		{
			// NOT! fConfigTree.getBooleanFromPathText( SEARCH_ENGINE_CAREFUL_REDIR_PATH
			cSearchEngineCarefulRedir = fConfigTree.getBooleanFromSinglePathAttr(
					SEARCH_ENGINE_URL_PATH,
					SEARCH_ENGINE_CAREFUL_REDIR_ATTR,
					DEFAULT_SEARCH_ENGINE_CAREFUL_REDIR
					);
			// debugMsg( kFName, "SEARCH_ENGINE_CAREFUL_REDIR_PATH="+SEARCH_ENGINE_CAREFUL_REDIR_PATH+", cSearchEngineCarefulRedir="+cSearchEngineCarefulRedir);
		}
		return cSearchEngineCarefulRedir;
	}

	public String getPagingFieldOrNull()
	{
		if( ! mIsFreshCache )
		{
			// The query field
			cPagingField = fConfigTree.getTextByPathTrimOrNull( PAGING_FIELD_PATH );
		}
		return cPagingField;
	}

	// Docs Per Page vs. Page Scale
	// Everybody knows how many docs they have on one page
	// BUT when looking at the hyperlinks they may not notice
	// that the scale IN THE HYPERLINK can be different than
	// that; it could be page number or doc number
	public int getDocsPerPage()
	{
		if( ! mIsFreshCache ) {
			cDocsPerPage = fConfigTree.getIntFromPathText(
				DOCS_PER_PAGE_PATH, DEFAULT_DOCS_PER_PAGE );
		}
		return cDocsPerPage;		
	}
	// The actual CGI parameter
	// If it's BY PAGE then this will usually be 10
	// If it's by DOC then this will be 1
	// Yes, it's confusing to have both this and
	// docs per page
	public boolean getShouldCalcWithPageNum()
	{
		if( ! mIsFreshCache ) {
			cShouldCalcWithPageNum = fConfigTree.getBooleanFromPathText(
				DOC_PAGE_USE_PAGE_NUM_PATH, DEFAULT_DOCS_CALC_USING_PAGE_NUM
				);
		}
		return cShouldCalcWithPageNum;		
	}
	public int getDocsFirstOrdinal()
	{
		if( ! mIsFreshCache ) {
			cDocsBaseOrdinal = fConfigTree.getIntFromPathText(
					FIRST_DOC_PATH, DEFAULT_DOCS_FIRST_ORDINAL );
		}
		return cDocsBaseOrdinal;		
	}

	public Hashtable getSearchEngineTestDriveURLFields()
	{
		final String kFName = "getSearchEngineTestDriveURLFields";
		if( ! mIsFreshCache )
		{
			// The URL to direct to
			List tmpList = fConfigTree.findElementsByPath(
				SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_PATH
				);
			// If we found some
			if( null!=tmpList && ! tmpList.isEmpty() ) {
				cSearchEngineTestDriveFields = new Hashtable();
				for( Iterator it = tmpList.iterator(); it.hasNext() ; ) {
					Element field = (Element)it.next();
					String fieldName = JDOMHelper.getStringFromAttributeTrimOrNull( field, NAME_ATTR, false );
					// String fieldValue = JDOMHelper.getStringFromAttributeTrimOrNull( field, "value", false );
					String fieldValue = JDOMHelper.getStringFromAttribute( field, VALUE_ATTR, false );
					if( null!=fieldName && null!=fieldValue )
						cSearchEngineTestDriveFields.put( fieldName, fieldValue );
					else if( null==NIEUtil.trimmedStringOrNull(fieldName) && null==NIEUtil.trimmedStringOrNull(fieldValue) )
						infoMsg( kFName,
							"Ingoring null/empty hidden field and value pair"
							);
					else
						errorMsg( kFName,
							"Null field name and/or value: name=" + fieldName + ", value=" + fieldValue
							);
				}
				// OK, there were supposed to be some, complain if none were valid
				if( cSearchEngineTestDriveFields.isEmpty() ) {
					cSearchEngineTestDriveFields = null;
					// No longer an error, since UI can produce empty nodes
					infoMsg( kFName,
						"Found some nodes but none were valid in search engine config \"" + SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_PATH + "\""
						);
				}
			}	// End if we found some
		}
		return cSearchEngineTestDriveFields;
	}


	private Element getFormOptionsTreeOrNull() {
		if( ! mIsFreshCache && null==cOptionFieldsTree )
			cOptionFieldsTree = fConfigTree.findElementByPath(
				SEARCH_FORM_OPTIONS_ROOT_PATH
				);
		return cOptionFieldsTree;
	}

	// Give them a safe, modifyable deep clone
	public Element getFormOptionsTreeCloneOrNull() {
		final String kFName = "getFormOptionsTreeCopyOrNull";
		Element origElem = getFormOptionsTreeOrNull();
		Element outElem = null;
		if( null==origElem )
			return null;
		try {
			outElem = (Element) origElem.clone();
		}
		catch( Exception e ) {
			outElem = null;
			errorMsg( kFName, "Error cloning options tree: " + e );
		}
		outElem.detach();
		return outElem;
	}

	public Collection getSearchFormOptionFieldNames() {
		Hashtable tmpHash = getSearchFormOptionFieldsHash();
		if( null==tmpHash )
			return null;
		return tmpHash.keySet();
	}

	// returns a hashtable by name of LISTS of valid values
	// also populates a desc hash
	public Hashtable getSearchFormOptionFieldsHash()
	{
		final String kFName = "getSearchFormOptionFieldsHash";
		if( ! mIsFreshCache )
		{
			boolean debug = shouldDoDebugMsg( kFName );

			if(debug) debugMsg( kFName, "Start to calculate cached value." );

			// The URL to direct to
			// List tmpList = fConfigTree.findElementsByPath(
			//	SEARCH_FORM_OPTION_FIELDS_PATH
			//	);

			Element rootNode = getFormOptionsTreeOrNull();
			// If we found a root node, look a bit further for option fields
			if( null!=rootNode ) {
				if(debug) debugMsg( kFName, "Found a root element." );

				List fieldList = JDOMHelper.findElementsByPath(
					// rootNode, SEARCH_FORM_OPTIONS_ROOT_PATH
					rootNode, FIELD_ELEM
					);

				// If we found some feilds
				if( null!=fieldList && ! fieldList.isEmpty() ) {
					if(debug) debugMsg( kFName, "Found " + fieldList.size() + " fields." );
					// By name, value is a LIST
					cSearchFormOptionFields = new Hashtable();
					// By option_name + option_value = String
					cSearchFormOptionValueDescriptions = new Hashtable();
					cSearchFormOptionFieldDescriptions = new Hashtable();

					// cSearchFormOptionFields, Hashtable cSearchFormOptionDescriptions
					// For each option field
					for( Iterator it = fieldList.iterator(); it.hasNext() ; ) {
						Element fieldElem = (Element)it.next();
						String fieldName = JDOMHelper.getStringFromAttributeTrimOrNull( fieldElem, NAME_ATTR, false );
						if( null==fieldName ) {
							if( JDOMHelper.getBooleanFromAttribute(
									fieldElem, SKIP_BLANK_NAME_ATTR, DEFAULT_BLANK_NAME_OK
									)
							) {
								continue; 
							}
							errorMsg( kFName,
								"No fieldName in this element, skipping: "
								+ " At: " + JDOMHelper.getPathToElement( fieldElem )
								+ " = " + JDOMHelper.JDOMToString( fieldElem, false )
								);
							continue;
						}

						// The Field description
						String fieldDesc = JDOMHelper.getStringFromAttributeTrimOrNull( fieldElem, DESC_ATTR, false );
						// Store the description!
						if( null!=fieldDesc ) {
							String fieldDescKey = fieldName.toLowerCase();
							if( ! cSearchFormOptionFieldDescriptions.containsKey(fieldDescKey) )
								cSearchFormOptionFieldDescriptions.put( fieldDescKey, fieldDesc );
							else
								warningMsg( kFName,
									"Duplicate field name/description:"
									+ " fieldName=\"" + fieldName + "\""
									+ ", prev fieldDesc=\""
									+ (String) cSearchFormOptionFieldDescriptions.get( fieldDescKey )
									+ "\""
									+ ", new fieldDesc=\"" + fieldDesc + "\""
									+ " Ignoring this new description and keeping the old one."
									);
						}
						/*** huh?
						else
							warningMsg( kFName,
								"Duplicate field description for "
								+ " fieldName=\"" + fieldName + "\""
								+ ", will still add this field but it may look odd."
								);
						***/

						// For each option
						List optionElems = JDOMHelper.findElementsByPath(
							fieldElem, OPTION_ELEM
							);

						List optionStrings = new Vector();

						// For each option value
						for( Iterator it2 = optionElems.iterator(); it2.hasNext() ; ) {
							Element optionElem = (Element)it2.next();
							// String optionValue = JDOMHelper.getStringFromAttributeTrimOrNull( optionElem, VALUE_ATTR, false );
							String optionValue = JDOMHelper.getStringFromAttribute(
								optionElem, VALUE_ATTR, false
								);
							// We don't trim, and we DO keep nulls
							optionValue = (null!=optionValue) ? optionValue
								: nie.sr2.ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
								;

							// Store description if any
							String description = JDOMHelper.getStringFromAttributeTrimOrNull(
								optionElem, DESC_ATTR, false
								);

							// If both are empty/null, don't keep them
							// this can happen with the Configurator
							if( optionValue.equals("") && null==description ) {
								infoMsg( kFName, "Ignoring empty sub-site field element.");
								continue;
							}


							// Add to the list
							optionStrings.add( optionValue );


							if( null!=description ) {
								// Form descrption key
								String descKey = fieldName.toLowerCase() + HASH_KEY_SEP + optionValue;
								if( cSearchFormOptionValueDescriptions.containsKey(descKey) )
									errorMsg( kFName,
										"Attempt to override option description:"
										+ " field=\"" + fieldName + "\""
										+ " optionValue=\"" + optionValue + "\""
										+ ", previous description =\"" + (String)cSearchFormOptionValueDescriptions.get(descKey) + "\""
										+ ", new description =\"" + description + "\""
										+ " Ignoring this new description and keeping the old one."
										);
								else
									cSearchFormOptionValueDescriptions.put( descKey, description );
							}

						}	// End for each individual option

						// Record the valid options for this field
// statusMsg( kFName, "cSearchFormOptionFields=" + cSearchFormOptionFields );
debugMsg( kFName, "fieldName=" + fieldName );
						if( cSearchFormOptionFields.containsKey(fieldName) ) {
							List oldValues = (List) cSearchFormOptionFields.get(fieldName);
							oldValues.addAll( optionStrings );
							warningMsg( kFName,
								"Duplicate multivalue options field \"" + fieldName + "\""
								+ " Keeping all values from both, but config should be checked."
								);
						}
						else
							cSearchFormOptionFields.put( fieldName, optionStrings );

					}	// End for each field

					// OK, there were supposed to be some, complain if none were valid
					if( cSearchFormOptionFields.isEmpty() ) {
						cSearchFormOptionFields = null;
						cSearchFormOptionValueDescriptions = null;
						// No longer really an error since UI can produce empty elements
						infoMsg( kFName,
							"Found some nodes but none were valid in search engine config \"" + SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_PATH + "\""
							);
					}
				}	// End if we found some
				else {
					warningMsg( kFName, "No fields found under field options node, check search engine configuration." );
				}

			}	// End if we found a root node
			else {
				if(debug) debugMsg( kFName, "Did NOT find a root config element for modes." );
			}

			if(debug) debugMsg( kFName,
				"Recorded: " + cSearchFormOptionFields
				);

		}
		return cSearchFormOptionFields;
	}

	public String getFormOptionFieldValueDescOrNull(
		String inFieldName, String inFieldValue
		)
	{
		final String kFName = "getFormOptionFieldValueDescOrNull";
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in, returning null." );
			return null;
		}
		/***
		if( null==inFieldValue ) {
			errorMsg( kFName, "Null field value passed in, returning null." );
			return null;
		}
		***/
		inFieldValue = (null!=inFieldValue) ? inFieldValue : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
		String descKey = inFieldName.toLowerCase() + HASH_KEY_SEP + inFieldValue;
		if( cSearchFormOptionValueDescriptions.containsKey(descKey) )
			return (String) cSearchFormOptionValueDescriptions.get( descKey );
		else
			return null;
	}
	public String getFormOptionFieldDescOrNull(
		String inFieldName
		)
	{
		final String kFName = "getFormOptionFieldDescOrNull";
		if( null==inFieldName ) {
			errorMsg( kFName, "Null field name passed in, returning null." );
			return null;
		}
		String descKey = inFieldName.toLowerCase();
		if( cSearchFormOptionFieldDescriptions.containsKey(descKey) )
			return (String) cSearchFormOptionFieldDescriptions.get( descKey );
		else
			return null;
	}

	public String getVendor()
	{
		if( ! mIsFreshCache )
		{
			// The Vendor, optional
			cVendor = fConfigTree.getTextByPathTrimOrNull( VENDOR_PATH );
		}
		return cVendor;
	}

	private boolean _TODO_acceptsNullSearches()
	{
		return false;
	}

	public List getSuggestionMarkerLiteralText()
	{
		// The string to look for when we want to insert a webmaster suggests
		// Todo: would be nice to support more than one of these
		// Todo: would be nice to support literal or regex
		// Todo: would be nice to say before or after
		// Todo: would be nice to say "keep this text" or dump it after subst
		if( ! mIsFreshCache )
		{
			cMarkerList = fConfigTree.getTextListByPathNotNullTrim(
				SUGGESTION_MARKER_TEXT_PATH
				);
		}
		return cMarkerList;
	}


	public String getSnippetPrefixText()
	{
		if( ! mIsFreshCache )
		{
			// cPrefixText = fConfigTree.getTextFromSinglePathAttrTrimOrNull(
			//	SUGGESTION_MARKER_MODIFIER_PATH,
			//	PREFIX_TEXT_ATTR
			//	);
			cPrefixText = fConfigTree.getTextByPathTrimOrNull(
				PREFIX_TEXT_PATH
				);
		}
		return cPrefixText;
	}

	public String getSnippetSuffixText()
	{
		if( ! mIsFreshCache )
		{
			// cSuffixText = fConfigTree.getTextFromSinglePathAttrTrimOrNull(
			//	SUGGESTION_MARKER_MODIFIER_PATH,
			//	SUFFIX_TEXT_ATTR
			//	);
			cSuffixText = fConfigTree.getTextByPathTrimOrNull(
				SUFFIX_TEXT_PATH
				);
		}
		return cSuffixText;
	}


	public boolean getIsSuggestionMarkerNewTextInsertAfter()
	{
		final String kFName = "getIsSuggestionMarkerNewTextInsertAfter";
		if( ! mIsFreshCache )
		{
			cIsInsertAfter = fConfigTree.getBooleanFromSinglePathAttr(
				SUGGESTION_MARKER_MODIFIER_PATH,
				MARKER_TEXT_AFTER_ATTR,
				DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
				);

//statusMsg( kFName, "tree=" + fConfigTree.JDOMToString( true ) );
//statusMsg( kFName, "SUGGESTION_MARKER_MODIFIER_PATH=" + SUGGESTION_MARKER_MODIFIER_PATH );
//statusMsg( kFName, "MARKER_TEXT_AFTER_ATTR=" + MARKER_TEXT_AFTER_ATTR );
//statusMsg( kFName, "DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER=" + DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER );

		}

//statusMsg( kFName, "cIsInsertAfter=" + cIsInsertAfter );
		return cIsInsertAfter;
	}

	public boolean getIsSuggestionMarkerReplaced()
	{
		if( ! mIsFreshCache )
		{
			cIsMarkerReplaced = fConfigTree.getBooleanFromSinglePathAttr(
				SUGGESTION_MARKER_MODIFIER_PATH,
				MARKER_TEXT_REPLACE_ATTR,
				DEFAULT_SUGGESTION_MARKER_IS_REPLACED
				);
		}
		return cIsMarkerReplaced;
	}

	public boolean getIsSuggestionMarkerCaseSensitive()
	{
		if( ! mIsFreshCache )
		{
			cIsMarkerCasen = fConfigTree.getBooleanFromSinglePathAttr(
				SUGGESTION_MARKER_MODIFIER_PATH,
				MARKER_TEXT_CASEN_ATTR,
				DEFAULT_SUGGESTION_MARKER_IS_CASEN
				);
		}
		return cIsMarkerCasen;
	}

	public boolean isCGIFeildsCaseSensitive()
	{
		final String kFName = "isCGIFeildsCaseSensitive";
		if( ! mIsFreshCache )
		{
			cCGIFieldsCasen = fConfigTree.getBooleanFromPathText(
				CGI_FIELDS_CASEN_PATH,
				DEFAULT_CGI_FIELDS_CASEN
				);
			debugMsg( kFName,
				CGI_FIELDS_CASEN_PATH + " = " + cCGIFieldsCasen
				);
		}
		return cCGIFieldsCasen;
	}




	// For a hash of CGI variables, see if ther are any fields matching
	// the list of "no action indicator" fields we may have stored.
	public boolean setupNoActionIndicatorFields( Hashtable inCheckHash )
	{
		final String kFName = "setupNoActionIndicatorFields";

		if( inCheckHash == null )
		{
			errorMsg( kFName,
				"Error: SearchEngineConfig:setupNoActionIndicatorFields:"
				+ " Passed in a NULL hash to check on, will return false."
				);
			return false;
		}

		// We can also bail if there's no list of fields to check
		if( fNoActionFieldList == null )
		{
			debugMsg( kFName, "There are no no-action fields to check, returning false." );
			return false;
			// And no warning is needed, it's perfectly fine to to have
			// any to check against
		}

		debugMsg( kFName,
			"Will check " + fNoActionFieldList.size() + " no-action fields."
			+ " Use trace mode for more details."
			);

		// So we have a hash and a list to check

		// Iterate through our list
		for( Iterator it = fNoActionFieldList.iterator(); it.hasNext(); )
		{
			String checkField = (String)it.next();
			traceMsg( kFName, "Checking field \"" + checkField + "\"." );
			if( ! isCGIFeildsCaseSensitive() )
				checkField = checkField.toLowerCase();
			if( inCheckHash.containsKey( checkField ) )
			{
				traceMsg( kFName, "Found matching field, returning true." );
				return true;
			}
			// Otherwise, keep checking
			traceMsg( kFName, "Field didn't match, will keep checking." );
		}

		debugMsg( kFName, "After checking all fields, found no matches, returning false." );

		// Else none found, so return false
		return false;
	}

	private void setupNoActionIndicatorFields()
	{
		final String kFName = "setupNoActionIndicatorFields";
		// A list of indicators that mean we should not touch this
		List tmpList = fConfigTree.getTextListByPathNotNullTrim(
			NO_ACTION_INDICATOR_PATH
			);

		if( tmpList != null && tmpList.size() > 0 )
		{
			debugMsg( kFName, "Found " + tmpList.size() + " no-action indicator fields." );
			fNoActionFieldList = tmpList;
		}
		else
		{
			debugMsg( kFName, "Did not find any no-action indicator fields." );
			fNoActionFieldList = null;
		}
		// Todo: Minor optimiation
		// We could lower case the fields once here and store them
		// in a new list.
	}


	// Results lists usually have mini search forms
	// and document hyperlinks
	//
	// If subsequent searches are to be routed through SearchTrack
	// then we need to tweak those forms
	//
	// And if we're going to track which documents were clicked on
	// we need to change those document links

	// Which forms, if any, appearing on the results list page
	// should we try to tweak?
	public String getResultsFormsTweakPolicy()
	{
		if( ! mIsFreshCache )
		{
			cFormPolicy = fConfigTree.getTextByPathTrimOrDefault(
				FORMS_TWEAK_POLICY_PATH,
				DEFAULT_FORMS_TWEAK_CHOICE
				);
		}
		return cFormPolicy;
	}
	// Should we blow away any JavaScript actions?
	public String _getResultsFormsJsTweakPolicy()
	{
		if( ! mIsFreshCache )
		{
			cFormJsPolicy = fConfigTree.getTextByPathTrimOrDefault(
				FORMS_JS_TWEAK_POLICY_PATH,
				DEFAULT_FORMS_JS_CHOICE
				);
		}
		return cFormJsPolicy;
	}
	// Some of our policies need one or more arguments
	// in order to function
	public String getResultsFormsTweakArg1()
	{
		if( ! mIsFreshCache )
		{
			cFormArg1 = fConfigTree.getTextByPathTrimOrNull(
					FORMS_TWEAK_ARG1_PATH
				);
		}
		return cFormArg1;
	}
	public String getResultsFormsTweakArg2()
	{
		if( ! mIsFreshCache )
		{
			cFormArg2 = fConfigTree.getTextByPathTrimOrNull(
					FORMS_TWEAK_ARG2_PATH
				);
		}
		return cFormArg2;
	}

	// Which document links, if any, appearing on the results list page
	// should we try to tweak?
	public String getResultsDocLinksTweakPolicy()
	{
		if( ! mIsFreshCache )
		{
			cDocPolicy = fConfigTree.getTextByPathTrimOrDefault(
				DOCS_TWEAK_POLICY_PATH,
				DEFAULT_DOCS_TWEAK_CHOICE
				);
		}
		return cDocPolicy;
	}
	// Should we blow away any JavaScript actions?
	public String _getResultsDocLinksJsTweakPolicy()
	{
		if( ! mIsFreshCache )
		{
			cDocJsPolicy = fConfigTree.getTextByPathTrimOrDefault(
				DOCS_JS_TWEAK_POLICY_PATH,
				DEFAULT_DOCS_JS_CHOICE
				);
		}
		return cDocJsPolicy;
	}
	// Some of our policies need one or more arguments
	// in order to function
	public String getResultsDocLinksArg1()
	{
		if( ! mIsFreshCache )
		{
			cDocArg1 = fConfigTree.getTextByPathTrimOrNull(
					DOCS_TWEAK_ARG1_PATH
				);
		}
		return cDocArg1;
	}
	public String getResultsDocLinksArg2()
	{
		if( ! mIsFreshCache )
		{
			cDocArg2 = fConfigTree.getTextByPathTrimOrNull(
					DOCS_TWEAK_ARG2_PATH
				);
		}
		return cDocArg2;
	}
	// When we look at a URL to be logged as a click through, we normally
	// just want that URL.
	// However, if the site has some weird CMS or other intermediate server
	// then we can look at a specific CGI field instead
	public String getResultsDocLinksCgiIdField()
	{
		if( ! mIsFreshCache )
		{
			cDocCgiIdField = fConfigTree.getTextByPathTrimOrNull(
					DOC_KEY_CGI_PATH
				);
		}
		return cDocCgiIdField;
	}

	
	public static String tryFetchingSearchEngineURLFromConfig( String inConfigFileURI ) {
		final String kFName = "tryFetchingSearchEngineURLFromConfig";
		if( null==inConfigFileURI ) {
			warningMsg( kFName, "Null config file URI passed in; will try default." );
			// return null;
			inConfigFileURI = SearchTuningConfig.DEFAULT_CONFIG_FILE_URI;
		}

		JDOMHelper tree = null;
		// Try fancy jdom
		try
		{
			tree = new JDOMHelper(
				inConfigFileURI, null, 0, null
				);
		}
		catch (JDOMHelperException e1)
		{
			errorMsg( kFName, 
				"Error loading config file with includes."
				+ " JDOMHelperException: " + e1
				+ " Will try without includes."
				);
			// OK, try it the old fashioned way, with no includes
			try
			{
				tree = new JDOMHelper( inConfigFileURI );
			}
			catch (JDOMHelperException e2)
			{
				errorMsg( kFName, 
					"Error loading config file wihthout includes."
					+ " JDOMHelperException: " + e2
					// + " Will try as plain text."
					+ " Returning null."
					);
				tree = null;
			}
		}

		// If we got a tree back, use xpath to search it
		if( null!=tree ) {
			final String kPath = "//" + SEARCH_ENGINE_URL_PATH;
			try {
				XPath xpath = XPath.newInstance( kPath );
				List results = xpath.selectNodes( tree.getJdomElement() );
	
				// For each form
				for( Iterator it = results.iterator() ; it.hasNext() ; ) {
					Element currElem = (Element) it.next();
					String candidateURL = currElem.getTextNormalize();
					candidateURL = NIEUtil.trimmedStringOrNull( candidateURL );
					if( null!=candidateURL )
						return candidateURL;
				}
			}
			catch( JDOMException e3 ) {
				errorMsg( kFName, 
					"Error searching config file with xpath for \"" + kPath + "\""
					+ " Exception: " + e3
					// + " Will try as plain text."
					+ " Returning null."
					);
			}

		}

		/***
		No, can't retrieve as text, could be cdata, etc, too weird
		// Else no jdom, try loading as a string
		try {
			String fileContents = NIEUtil.fetchURIContentsChar( inConfigFileURI );
			if( null!=fileContents && fileContents.length() > 0 ) {

				String pattern1 = SEARCH_ENGINE_URL_PATH + "=\"";
				int pattern1At = fileContents.indexOf( pattern1 );
				if( pattern1At>=0 ) {
					int urlStart = pattern1At + pattern1.length();


				}

			}
			else {
				errorMsg( kFName,
					"Got back null/empty string when loading config file \"" + inConfigFileURI + "\" as text."
					+ " We give up, returning null."
					);
			}

		}
		catch( IOException e4 ) {
			errorMsg( kFName, 
				"Error loading config file as text."
				+ " Exception: " + e4
				+ " We give up, returning null."
				);
		}
		***/

		return null;
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



	// The main JDOM configuration tree
	private JDOMHelper fConfigTree;


	// Field caching fields
	private boolean mIsFreshCache;
	private Element cOptionFieldsTree;
	private String cPrefixText;
	private String cSuffixText;
	private boolean cCGIFieldsCasen;
	private boolean cIsMarkerCasen;
	private boolean cIsMarkerReplaced;
	private boolean cIsInsertAfter;
	private String cVendor;
	private String cSearchEngineURL;
	private String cBaseURL;
	private String cSearchEngineMethod;
	private boolean cSearchEngineCarefulRedir;
	private Hashtable cSearchEngineTestDriveFields;
	private Hashtable cSearchFormOptionFields;
	private Hashtable cSearchFormOptionFieldDescriptions;
	private Hashtable cSearchFormOptionValueDescriptions;
	private String cQueryField;
	// CGI field, if present, represent a page link
	private String cPagingField;
	private int cDocsPerPage = DEFAULT_DOCS_PER_PAGE;
	// What's the first document in the list (usually 1 or 0)
	// This really only matters when we get to page 2 of the results
	private int cDocsBaseOrdinal = DEFAULT_DOCS_FIRST_ORDINAL;
	private boolean cShouldCalcWithPageNum = DEFAULT_DOCS_CALC_USING_PAGE_NUM;

	private String cFormPolicy = DEFAULT_FORMS_TWEAK_CHOICE;
	private String cFormJsPolicy = DEFAULT_FORMS_JS_CHOICE;
	private String cFormArg1;
	private String cFormArg2;
	private String cDocPolicy = DEFAULT_DOCS_TWEAK_CHOICE;
	private String cDocJsPolicy = DEFAULT_DOCS_JS_CHOICE;
	private String cDocCgiIdField;
	private String cDocArg1;
	private String cDocArg2;

	
	private List cMarkerList;

	// A bunch of Search Engine Information that we'll need
	private List fNoActionFieldList;

	// An optional list of rules for categorizing hyperlinks
	// found in a results list
	private List fDocLinkRules;

	public static final String MAIN_ELEM_NAME = "search_engine_info";


	// The name of the query field to look for
	// For example, with Verity, it would be QueryText
	// This one is REQUIRED
	public static final String QUERY_FIELD_PATH =
		"search_term_field_name";
	// This one is REQUIRED
	// Keep this a single element level with no /
	public static final String SEARCH_ENGINE_URL_PATH =
		"search_url";
	// Get vs. Post
	public static final String SEARCH_ENGINE_METHOD_ATTR =
		"method";
	public static final String SEARCH_ENGINE_METHOD_PATH =
	    SEARCH_ENGINE_URL_PATH + "/@" + SEARCH_ENGINE_METHOD_ATTR;
	public static final String DEFAULT_SEARCH_ENGINE_METHOD =
		"GET";

	public static final String PAGING_FIELD_PATH =
		"results_paging_field_name";
	// The number of docs on a page a user will actually see
	// which, at the moment, is not very useful, but easy to understand
	public static final String DOCS_PER_PAGE_ATTR =
		"docs_per_page";
	public static final String DOCS_PER_PAGE_PATH =
		PAGING_FIELD_PATH + "/@" + DOCS_PER_PAGE_ATTR;
	// These next two args are for translating the CGI page offset
	// into a document offset
	// Some engines use "page number"
	// Others use the actual document offset
	public static final String DOC_PAGE_USE_PAGE_NUM_ATTR =
		"calc_using_page_number";
	public static final String DOC_PAGE_USE_PAGE_NUM_PATH =
		PAGING_FIELD_PATH + "/@" + DOC_PAGE_USE_PAGE_NUM_ATTR;
	public static final String FIRST_DOC_ATTR =
		"first_doc_num";
	public static final String FIRST_DOC_PATH =
		PAGING_FIELD_PATH + "/@" + FIRST_DOC_ATTR;
	
	private static final int DEFAULT_DOCS_PER_PAGE = 10;
	private static final int DEFAULT_DOCS_FIRST_ORDINAL = 0;
	private static final boolean DEFAULT_DOCS_CALC_USING_PAGE_NUM = false;

	// Weird redirects, for example Inquira
	public static final String SEARCH_ENGINE_CAREFUL_REDIR_ATTR =
		"use_careful_redirects";
	public static final String SEARCH_ENGINE_CAREFUL_REDIR_PATH =
	    SEARCH_ENGINE_URL_PATH + "/@" + SEARCH_ENGINE_CAREFUL_REDIR_ATTR;
	// Not making the default for now since code is new
	public static final boolean DEFAULT_SEARCH_ENGINE_CAREFUL_REDIR =
		false;
	// Base URL other than search engine
	public static final String BASE_URL_PATH =
		"base_url";
	// Sentinel Sentinal, marker
	// It means don't add base href in proxied results lists
	public static final String NO_URL_MARKER = "-";

	// This is optional, defaults to above
	public static final String FIELD_ELEM = "field";
	public static final String OPTION_ELEM = "option";
	public static final String NAME_ATTR = "name";
	public static final String VALUE_ATTR = "value";
	public static final String DESC_ATTR = "label";
	public static final String SELECTED_ATTR = "selected";


	public static final String SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_ELEM =
		"search_url_test_drive_fields";
	public static final String SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_ROOT_PATH =
		SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_ELEM;
	public static final String SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_PATH =
		SEARCH_ENGINE_URL_TEST_DRIVE_FIELDS_ROOT_PATH + "/" + FIELD_ELEM;

	public static final String SEARCH_FORM_OPTIONS_ELEM =
		"search_form_option_fields";
//	   public static final String __SEARCH_FORM_OPTION_FIELDS_PATH =
//		"search_form_option_fields/" + FIELD_ELEM;
	public static final String SEARCH_FORM_OPTIONS_ROOT_PATH =
		SEARCH_FORM_OPTIONS_ELEM;

	public static final String VENDOR_ELEM =
		"vendor";
	public static final String VENDOR_PATH =
		VENDOR_ELEM;
	public static final String NO_ACTION_INDICATOR_PATH =
		"no_action_indicator_field";
	public static final String CGI_FIELDS_CASEN_PATH =
		"case_sensitive_cgi_fields";
	public static final boolean DEFAULT_CGI_FIELDS_CASEN = false;

	// Markers for figuring out where to put the various suggestions
	// Can have more than one!
	public static final String SUGGESTION_MARKER_TEXT_PATH =
		"suggestion_marker_text";
	// A flag that modifies the behavior of all the markers specified
	public static final String SUGGESTION_MARKER_MODIFIER_PATH =
		SUGGESTION_MARKER_TEXT_PATH + "_modifiers";

	// This section is for the LITERAL TEXT that will be placed
	// before or after the suggestion
	public final static String _PREFIX_TEXT_ATTR = "markup_before";
	public final static String _SUFFIX_TEXT_ATTR = "markup_after";
	public final static String PREFIX_TEXT_PATH = "markup_before";
	public final static String SUFFIX_TEXT_PATH = "markup_after";

	// This section affects WHERE the suggestion will be placed
	// relative to the pattern that was found, and about that
	// literal pattern
	// Atributes to change the modified behavior
	private static final String MARKER_TEXT_AFTER_ATTR = "after";
	private static final String MARKER_TEXT_REPLACE_ATTR = "replace";
	private static final String MARKER_TEXT_CASEN_ATTR = "casen";

	// By default, where do we put the new text
	private static final boolean DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
		= true;
	// By default, should we replace the marker text when we find it
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_REPLACED
		= false;
	// By default, are the patterns case sensitive
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_CASEN
		= false;

	// Options affecting tweaks we will make to the results list
	// Handling search forms on results list
	public final static String RESULTS_OPTIONS_PATH = "results";
	// results/forms
	public final static String FORMS_OPTIONS_ELEM = "forms";
	public final static String FORMS_OPTIONS_PATH =
		RESULTS_OPTIONS_PATH + '/' + FORMS_OPTIONS_ELEM;

	// results/doc_links
	public final static String DOCS_OPTIONS_ELEM = "doc_links";
	// results/doc_links
	public final static String DOCS_OPTIONS_PATH =
		RESULTS_OPTIONS_PATH + '/' + DOCS_OPTIONS_ELEM;


	public final static String TWEAK_POLICY_ELEM = "tweak_policy";
	// results/forms/tweak_policy (text) </tweak_policy>
	public final static String FORMS_TWEAK_POLICY_PATH =
		FORMS_OPTIONS_PATH + '/' + TWEAK_POLICY_ELEM;
	// results/doc_links/tweak_policy (text) </tweak_policy>
	public final static String DOCS_TWEAK_POLICY_PATH =
		DOCS_OPTIONS_PATH + '/' + TWEAK_POLICY_ELEM;

	private static final void ___Tweak_Policies___(){}
	///////////////////////////////////////////////////////////////

	// Keep in sync with search-engine.xml UI file AND config_ui/tabs.../click-through.xml
	// Policies for tweaking various items including search forms and doc links
	public final static String TWEAK_DISABLED = "none";
	// You can enable reports if you're using the API to log stuff and then
	// we don't need to tweak anything
	public final static String TWEAK_LOG_ONLY = "log_only";
	public final static String TWEAK_CHOICE_SEARCH_URL = "search_url";
	public final static String TWEAK_CHOICE_ALL_ITEMS = "all";
	public final static String TWEAK_CHOICE_SPECIFIC_URL = "specific_url";
	public final static String TWEAK_CHOICE_EXCLUDE_URL = "exclude_url";
	public final static String TWEAK_CHOICE_AFTER_MARKER = "after_marker";
	public final static String TWEAK_CHOICE_BEFORE_MARKER = "before_marker";
	public final static String TWEAK_CHOICE_BETWEEN_MARKERS = "between_markers";

	public final static String DEFAULT_FORMS_TWEAK_CHOICE = TWEAK_CHOICE_SEARCH_URL;
	public final static String DEFAULT_DOCS_TWEAK_CHOICE = TWEAK_DISABLED;

	private final static String TWEAK_ARG_PREFIX = "tweak_arg_";
	public final static String TWEAK_ARG1_ELEM = TWEAK_ARG_PREFIX + "1";
	public final static String TWEAK_ARG2_ELEM = TWEAK_ARG_PREFIX + "2";

	// results/forms/tweak_arg_1 (text) </tweak_policy>
	public final static String FORMS_TWEAK_ARG1_PATH =
		FORMS_OPTIONS_PATH + '/' + TWEAK_ARG1_ELEM;
	public final static String FORMS_TWEAK_ARG2_PATH =
		FORMS_OPTIONS_PATH + '/' + TWEAK_ARG2_ELEM;

	// results/doc_links/tweak_arg_1 (text) </tweak_policy>
	public final static String DOCS_TWEAK_ARG1_PATH =
		DOCS_OPTIONS_PATH + '/' + TWEAK_ARG1_ELEM;
	public final static String DOCS_TWEAK_ARG2_PATH =
		DOCS_OPTIONS_PATH + '/' + TWEAK_ARG2_ELEM;

	public final static String TWEAK_JS_POLICY_ELEM = "js_policy";
	// results/forms/js_policy (text) </tweak_policy>
	public final static String FORMS_JS_TWEAK_POLICY_PATH =
		FORMS_OPTIONS_PATH + '/' + TWEAK_JS_POLICY_ELEM;
	// results/doc_links/js_policy (text) </tweak_policy>
	public final static String DOCS_JS_TWEAK_POLICY_PATH =
		DOCS_OPTIONS_PATH + '/' + TWEAK_JS_POLICY_ELEM;

	public final static String DOC_KEY_CGI_ELEM = "cgi_field";
	// results/doc_links/cgi_field (text) </tweak_policy>
	public final static String DOC_KEY_CGI_PATH =
		DOCS_OPTIONS_PATH + '/' + DOC_KEY_CGI_ELEM;

	public final static String DEFAULT_FORMS_JS_CHOICE = TWEAK_DISABLED;
	public final static String DEFAULT_DOCS_JS_CHOICE = TWEAK_DISABLED;

	public final static String ___DOC_LINK_TYPE_UNKNOWN = "unknown";
	public final static String DOC_LINK_TYPE_GENERIC = "generic";
	// The PROBLEM with these is that all custom user classes would
	// need to match exactly
	// INSTEAD we will use the prefix notation, see next section
	public final static String _DOC_LINK_TYPE_RESULT = "result";
	public final static String _DOC_LINK_TYPE_SITE_NAV = "site_nav";
	public final static String _DOC_LINK_TYPE_SEARCH_ENGINE_GENERAL = "search";
	public final static String _DOC_LINK_TYPE_SEARCH_ENGINE_PAGE_NAV = "search_page_nav";
	public final static String _DOC_LINK_TYPE_SEARCH_ENGINE_SORT = "search_sort";
	public final static String _DOC_LINK_TYPE_SEARCH_ENGINE_DRILL_DOWN = "search_drill_down";
	// Special prefixes for classes of links
	// This allows users to create their own classes but we can still
	// understand what to do with certain special ones
	public final static String DOC_LINK_TYPE_SEARCH_PREFIX = "search";			// no trailing _
	// public final static String DOC_LINK_TYPE_SEARCH_NAV_PREFIX =
	//	DOC_LINK_TYPE_SEARCH_PREFIX + "_nav";  // no trailing _
	public final static String DOC_LINK_TYPE_RESULT_PREFIX = "result";
	// Other types will be generic, which we can still log, but not our problem otherwise

	// results/doc_links/match_rule[*]
	public final static String DOC_LINKS_RULES_PATH =
		DOCS_OPTIONS_PATH + '/' + MatchRule.RULE_ELEM;

	public static final String HASH_KEY_SEP = "___sep___";

	public static final String SKIP_BLANK_NAME_ATTR = "skip_blank_name";
	public static final boolean DEFAULT_BLANK_NAME_OK = false;

}
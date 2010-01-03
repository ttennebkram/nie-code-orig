package nie.sn;

import nie.core.*;

import org.jdom.Element;
import java.util.*;

// The original / current use of this is to classify
// hyperlinks that we find on a web page.
// But it could be extended

public class MatchRule
{
	private static final String kClassName = "MatchRule";

	public MatchRule( /*SearchTuningConfig inMainConfig,*/ Element inElement )
		throws SearchEngineConfigException
	{
		final String kFName = "constructor(1)";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Sanity checks
		/***
		if( null == inMainConfig )
			throw new SearchEngineConfigException( kExTag +
				"NULL application config passed in."
				);
		fMainConfig = inMainConfig;
		***/
		
		// Sanity checks
		if( inElement == null )
			throw new SearchEngineConfigException( kExTag +
				"Constructor was passed in a NULL element."
				);

		// Instantiate and store the main JDOMHelper a
		fMainElement = null;
		try
		{
			fMainElement = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new SearchEngineConfigException( kExTag +
				"JDOM Helper Exception: " + e
				);
		}
		if( fMainElement == null )
			throw new SearchEngineConfigException( kExTag +
				"Got back a NULL xml tree when trying to create a redirect record"
				);

	
	}

	private void reinitFieldCache()
		throws SearchEngineConfigException
	{
		final String kFName = "reinitFieldCache";
		final String kExTag = kClassName + '.' + kFName + ": ";
		debugMsg( kFName, "Start" );
	
		mIsFreshCache = false;

		// No action
		if( null==getAssignToType() )
		{
			throw new SearchEngineConfigException(
				"No type assignment configured in rule"
				);			
		}
		boolean tmpBool1 = getIsDefault();
		String tmpStr = getCgiFieldToMatch();
		List tmpList = getPrefixPatternsOrNull();

		// No criteria
		if( !tmpBool1 && null==tmpStr && null==tmpList )
		{
			throw new SearchEngineConfigException(
				"No match conditions set.  Must CGI field or at least one prefix or default"
				);
		}

		mIsFreshCache = true;
	}

	public boolean matches( String inUrl /*, String optRoot*/ )
	{
		final String kFName = "matches";
		// Bail early if we can
		if( getIsDefault() )
			return true;

		// Our eventual answer
		boolean outResult = false;
		// what we'll compare
		String normUrl = inUrl.toLowerCase();

		// Sometimes patterns are enough
		boolean hadPrefixPattern = false;
		List tmpList = getPrefixPatternsOrNull();
		// If there's patterns, check them
		if( null!=tmpList )
		{
			hadPrefixPattern = true;
			for( Iterator it = tmpList.iterator(); it.hasNext() ; )
			{
				String prefix = (String) it.next();
				// Any matching pattern is fine
				if( normUrl.startsWith(prefix) )
				{
					outResult = true;
					break;
				}
			}
		}

		// TODO: perhapsp rewrite clearer

		String cgiName = getCgiFieldToMatch();
		// statusMsg( kFName, "CGI pattern = '" + cgiName + "'" );

		// If no CGI name, then it's whatever URL prefix found
		if( null==cgiName )
			return outResult;
		// else CGI not null
		// but that can't override failure to match a pattern
		// IF there were patterns
		else if( hadPrefixPattern && ! outResult )
			return false;

		// So we DO have a CGI name
		String key = cgiName.toLowerCase() + '=';
		// It's all down to whether they have a CGI field or not
		return normUrl.indexOf("?"+key)>=0 || normUrl.indexOf("&"+key)>=0 || normUrl.indexOf("&amp;"+key)>=0;
	}

	public static String testPages(
			String inSourceDoc, SearchEngineConfig inConfig
			)
	{
		final String kFName = "testPages";


		// How much markup do we want to do?
		String policy = inConfig.getResultsDocLinksTweakPolicy();

		System.out.println( kFName + ": Start, policy='" + policy + "'" );

		// If no policy, bail out
		// In this case NULL should not be returned, we should get the default
		// if they had nothing to say
		// If it is, 
		if( null==policy
			|| SearchEngineConfig.TWEAK_DISABLED.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_LOG_ONLY.equalsIgnoreCase(policy)
		) {
			System.out.println( kFName + ": Disabled, exiting routine, policy='" + policy + "'" );
			return inSourceDoc;
		}

		// The Main Document Iterator
		HtmlTagIterator it = new HtmlTagIterator(
			inSourceDoc,
			SnRequestHandler.HTML_ANCHOR_TAG_START,
			SnRequestHandler.HTML_HREF_ATTR_START
			);

		// Set the starting gate / fence, if any was requested
		// Here we check for the starting point
		// So for "between", it's when to start looking
		// and for "after", it's also when to start looking
		// "start looking after you see this pattern"
		if( SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			String origPattern = NIEUtil.trimmedStringOrNull(
					inConfig.getResultsFormsTweakArg1()
				);
			// Set the pattern and complain if it fails
			// This will also check and report nulls, etc.
			// It also normalized (case wise) as needed
			if( ! it.setStartPattern(origPattern) )
			{
				System.out.println( kFName + ": Error setting start pattern '" + origPattern + "', returning original document." );
				return inSourceDoc;
			}
		}

		// Set the finish line / fence, if requested
		// Check for the ending point
		// For "between", it's when to stop looking
		// and for "before", it's when to stop looking
		// "only look for tags until you see this pattern; check the part before the pattern"
		if( SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
			|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
			)
		{
			// If they JUST wanted a finish line, technically that would be the
			// first parameter, but if they were switching back and forth it'd
			// be easy to get confused.

			// "between": arg1=start, arg2=stop
			// "before": arg1=stop, arg2=ignored
			// BUT if you changed from "between" to "before"
			// you might still have "stop" pattern in arg2
			// So we will also accept:
			// "before": arg1=empty, arg2=stop

			// if between, then should be the second one, otherwise the first one
			String origPattern = SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
				? NIEUtil.trimmedStringOrNull( inConfig.getResultsFormsTweakArg2() )
				: NIEUtil.trimmedStringOrNull( inConfig.getResultsFormsTweakArg1() )
				;

			// Give them a second chance if it's just "before", they might have put it
			// in either box
			if( null==origPattern && SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy) )
				origPattern = NIEUtil.trimmedStringOrNull( inConfig.getResultsFormsTweakArg2() );

			// Set the pattern and complain if it fails
			// This will also check and report nulls, etc.
			// It also normalized (case wise) as needed
			if( ! it.setEndPattern(origPattern) )
			{
				System.out.println( kFName + ": Error setting end pattern '" +  origPattern + "', returning original document." );
				return inSourceDoc;
			}
		}
		
		System.out.println( kFName + ": Scan fences = "
			+ it.getStartDocBoundary() + '/' + it.getEndDocBoundary() + "'"
			+ " (reminder: -1  = no end limit)"
			);

		// AND we will be modifying the buffers, possibly changing their
		// length, as we do substitutions.

		// For each form tag
		while( it.next() )
		{
			String normUrl = it.getNormalizedAttrValueOrNull();
			// won't be null in this implementation
			// attrVal = NIEUtil.trimmedStringOrNull( attrVal );
			// if( null==attrVal )

			// Assess this URL, see if it's worth messing with
			boolean doSubst = false;
			// Which type of link / what to do with it...
			String substType = SearchEngineConfig.DOC_LINK_TYPE_GENERIC;

			// We may want to do all of them
			// OR if we were inside of a fence and got this far
			// then we'll do it as well
			if( SearchEngineConfig.TWEAK_CHOICE_ALL_ITEMS.equalsIgnoreCase(policy)
				|| SearchEngineConfig.TWEAK_CHOICE_AFTER_MARKER.equalsIgnoreCase(policy)
				|| SearchEngineConfig.TWEAK_CHOICE_BEFORE_MARKER.equalsIgnoreCase(policy)
				|| SearchEngineConfig.TWEAK_CHOICE_BETWEEN_MARKERS.equalsIgnoreCase(policy)
				)
			{
				doSubst = true;
				// System.out.println( kFName + ": Configured to match all in-fence-range values" );
			}
			// Search engine URL?
			else if( SearchEngineConfig.TWEAK_CHOICE_SEARCH_URL.equalsIgnoreCase(policy) )
			{
				String compUrl = NIEUtil.trimmedLowerStringOrNull( inConfig.getSearchEngineURL() );
				if( null!=compUrl && normUrl.startsWith(compUrl) )
				{
					doSubst = true;
					System.out.println( kFName + ": Matches normalized search engine URL '" + compUrl + "'" );
				}
				else {
					System.out.println( kFName + ": Does NOT match normalized search engine URL '" + compUrl + "'" );				
				}
			}
			// A specific URL prefix?
			else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
			{
				String compUrl = NIEUtil.trimmedLowerStringOrNull(
						inConfig.getResultsFormsTweakArg1()
					);
				if( null!=compUrl && normUrl.startsWith(compUrl) )
				{
					doSubst = true;
					System.out.println( kFName + ": Matches normalized URL prefix '" + compUrl + "'" );
				}
				else {
					System.out.println( kFName + ": Does NOT match normalized URL prefix '" + compUrl + "'" );					
				}
			}
			// EXCLUDE a specific URL prefix?
			else if( SearchEngineConfig.TWEAK_CHOICE_SPECIFIC_URL.equalsIgnoreCase(policy) )
			{
				String compUrl = NIEUtil.trimmedLowerStringOrNull(
						inConfig.getResultsFormsTweakArg1()
					);
				if( null!=compUrl && ! normUrl.startsWith(compUrl) )
				{
					doSubst = true;	
					System.out.println( kFName + ": Matches normalized Exclude prefix, so will NOT tweak, pattern='" + compUrl + "'" );
				}
				else {
					System.out.println( kFName + ": Does NOT match normalized Exclude prefix, so WILL tweak, pattern='" + compUrl + "'" );					
				}
			}

			// We're probably going to do something with this
			String origUrl = it.getOriginalAttrValueOrNull();

			// If we're supposed to do the substitution
			if( doSubst && null!=origUrl )
			{

				// String newValue = getSearchNamesURL();
				// String newValue = "http://foobar.com/index.html";
				// Figure out what type
				/***
				String compUrl = NIEUtil.trimmedLowerStringOrNull( getSearchEngineURL() );
				if( null!=compUrl && normUrl.startsWith(compUrl) )
				{
					substType = SearchEngineConfig.DOC_LINK_TYPE_SEARCH_ENGINE_GENERAL;		
				}
				// Relative values are navigators
				else if( normUrl.indexOf("://") < 0 )
				{
					substType = SearchEngineConfig.DOC_LINK_TYPE_SITE_NAV;
				}
				else
				{
					substType = SearchEngineConfig.DOC_LINK_TYPE_RESULT;
				}
				***/
				
				MatchRule rule = inConfig.findMatchingRuleOrNull( normUrl );
				if( null!=rule )
					substType = rule.getAssignToType();

				// System.out.println( kFName + ": URL type '" + substType + "' for url '" + origUrl + "'" );
				System.out.println( "" + substType + " for " + origUrl );

				/***
				LoggingLink link = null;
				String newValue = null;
				try {
					link = new LoggingLink( getMainConfig(), fRequestInfo );
					link.setDestinationURL( origUrl );
					link.setTransactionType( SearchLogger.TRANS_TYPE_LOG_DOC_CLICK );
					newValue = link.generateURL();

					if(debug) debugMsg( kFName, "WILL replace with new value='" + newValue + "'" );					

					if( ! it.replaceAttrValue(newValue) )
					{
						errorMsg( kFName, "Failed to replace with new attribute value, ignoring." );
					}			
				}
				catch( SearchLoggerException sle )
				{
					errorMsg( kFName, "Error adding click through links: " + sle );
				}
				***/
			}
			else if( null==origUrl )
			{
				System.out.println( kFName + ": Encountered null original URL, will not substituee." );
			}
			// Else not substition
			else {
				System.out.println( kFName + ": Not doing substituion, leaving old value='" + origUrl + "'" );					
			}	

		}	// End for each tag in document
		
		return it.getDocument();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		final String kFName = "main";
		if( args.length<2 )
		{
			System.err.println( "Test URL rule matching" );
			System.err.println( "Syntax: MatchRule search-engine-config.xml  uri1|file1.html [file2.html|uri2 ...]");
			System.exit( 1 );
		}
		try {
			JDOMHelper searchXml = new JDOMHelper( args[0] );
			SearchEngineConfig searchConfig = new SearchEngineConfig( searchXml.getJdomElement() );
			for( int i=1; i<args.length; i++ )
			{
				String fileName = args[i];
				System.out.println( "Checking '" + fileName + "'" );
				String contents = NIEUtil.fetchURIContentsChar( fileName );
				testPages( contents, searchConfig );
			}
		}
		catch( Exception e )
		{
			e.printStackTrace( System.err );
		}

	}

	private static void ___Simple_Setters_and_Getters___(){}
	////////////////////////////////////////////////////////////////////

	// Will return null if no list or empty list
	public List getPrefixPatternsOrNull()
	{
		if( ! mIsFreshCache )
		{
			// The NIEUtil method is OK with nulls, etc
			// TODO: case sensitivity
			cPrefixList = NIEUtil.trimmedLowerStringsAsList(
				fMainElement.getTextListByPathNotNullTrim( MATCH_PREFIX_ELEM )
				, false
				);
			// Normalize to null if empty
			if( null!=cPrefixList && cPrefixList.isEmpty() )
				cPrefixList = null;
		}		
		return cPrefixList;
	}
	public String getAssignToType()
	{
		if( ! mIsFreshCache ) {
			cAssignType = fMainElement.getTextByPathTrimOrNull( ASSIGN_TYPE_ELEM );
		}
		return cAssignType;
	}
	public String getCgiFieldToMatch()
	{
		if( ! mIsFreshCache ) {
			cCgiField = fMainElement.getTextByPathTrimOrNull( MATCH_CGI_FIELD_ELEM );
		}
		return cCgiField;
	}
	
	public boolean getIsDefault()
	{
		if( ! mIsFreshCache ) {
			List tmpList = fMainElement.findElementsByPath( MATCH_ALL_ELEM );
			if( null!=tmpList && ! tmpList.isEmpty() )
				cIsDefault = true;
		}
		return cIsDefault;
	}
	
	private static void ___Runtime_Logging___(){}
	////////////////////////////////////////////////////////////////////

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

	private static void ___Fields_and_Constants___(){}
	////////////////////////////////////////////////////////////////////


	private JDOMHelper fMainElement;
	private SearchTuningConfig _fMainConfig;

	// Field caching fields
	private boolean mIsFreshCache;

	private List cPrefixList;
	private String cCgiField;
	private String cAssignType;
	private boolean cIsDefault;
	
	public static final String RULE_ELEM = "match_rule";
	public static final String MATCH_PREFIX_ELEM = "prefix";

	public static final String MATCH_ALL_ELEM = "default_match";
	public static final String MATCH_CGI_FIELD_ELEM = "cgi_field";
	public static final String ASSIGN_TYPE_ELEM = "assign_type";

	/***
	public static final String IS_PAGE_FIELD_ATTR = "is_page_field";
	public static final String IS_PAGE_FIELD_PATH =
		MATCH_CGI_FIELD_ELEM + "/@" + IS_PAGE_FIELD_ATTR;
	// The number of docs on a page a user will actually see
	// which, at the moment, is not very useful, but easy to understand
	public static final String DOCS_PER_PAGE_ATTR = "docs_per_page";
	public static final String DOCS_PER_PAGE_PATH =
		MATCH_CGI_FIELD_ELEM + "/@" + DOCS_PER_PAGE_ATTR;
	// These next two args are for translating the CGI page offset
	// into a document offset
	// Some engines use "page number"
	// Others use the actual document offset
	public static final String DOC_PAGE_SCALE_ATTR = "doc_to_page_scale";
	public static final String DOC_PAGE_SCALE_PATH =
		MATCH_CGI_FIELD_ELEM + "/@" + DOC_PAGE_SCALE_ATTR;
	public static final String FIRST_DOC_ATTR = "first_doc_num";
	public static final String FIRST_DOC_PATH =
		MATCH_CGI_FIELD_ELEM + "/@" + FIRST_DOC_ATTR;
	
	private static final int DEFAULT_DOCS_PER_PAGE = 10;
	private static final int DEFAULT_DOCS_FIRST_ORDINAL = 1;
	***/
	
}

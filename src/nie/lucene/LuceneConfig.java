package nie.lucene;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

import nie.core.*;
import nie.pump.base.PumpConstants;
// import nie.sn.*;
import org.jdom.Element;
// import org.jdom.Document;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;

// A class to encapsulate all global config data.
// This will allow the main app to swap out configs easily
public class LuceneConfig
{

	private final static String kClassName = "LuceneConfig";


	// This is where the work actually gets done
	// to start up the server.
	// IT"S OK TO HAVE A NULL CONFIG, MEANS USE DEFAULTS
	///////////////////////////////////////////
	public LuceneConfig(
		Element inElement,
		// nie.sn.SearchTuningApp inApp
		nie.sn.SearchTuningConfig inConfig
		)
		throws LuceneConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// if( null == inApp )
		if( null == inConfig )
			throw new LuceneConfigException( kExTag
				+ "Null application configuration passed in."
				);
		// Store a reference to the main application
		// fApp = inApp;
		fConfig = inConfig;

		if( null == inElement )
			throw new LuceneConfigException( kExTag
				+ "Null Lucene configuration passed in."
				);

		// Instantiate and store the main JDOMHelper a
		fConfigTree = null;
		try
		{
			fConfigTree = new JDOMHelper( inElement );
		}
		catch (JDOMHelperException e)
		{
			throw new LuceneConfigException( kExTag
				+ "got JDOMHelper Exception: "
				+ e );
		}
		if( null == fConfigTree )
			throw new LuceneConfigException( kExTag
				+ "Got back a NULL xml tree when trying to create"
				+ " a Search Engine Configuration object."
				);

		// Old stuff from originally cloned class?
		// _initBasicFieldCache();

		// Parse "global" options in the config file
		// initFieldCache();
		if( getSearchFieldsAsList().size() < 1 )
			throw new LuceneConfigException( kExTag
					+ "Must set which fields to search via setting "
					+ SEARCH_FIELDS_PATH
					);
		// The default is all of them
		if( getDisplayFields().size() < 1 )
			warningMsg( kFName,
				"No display fields set via setting "
				+ DISPLAY_FIELDS_PATH
				+ "; will show ALL fields, which may slow system or kill memory."
				);

		try {
			initLucene();
			// openLuceneIndex();
			// getLuceneIndex();
		}
		catch( LuceneException le ) {
			throw new LuceneConfigException( kExTag
					+ "Error initializing Lucene: " + le
					);
		}
	}

	// ... OLD... from donor clone...
	private void _initBasicFieldCache()
		throws LuceneConfigException
	{
		final String kFName = "initBasicFieldCache";
	
		// Turn off Caching
		fUseCache = false;

		getConfiguredReportsDir();
	
		getAndSetupOptionalReportTemplate();
		// ^^^ This also caches these:
		// getTemplateURI();
		// getMarkerLiteralText();
		// getIsMarkerCaseSensitive();
		// getIsMarkerNewTextInsertAfter();
		// getIsMarkerReplaced();
	
		// Turn ON field/member Caching
		fUseCache = true;

		// Additional caching done in initLucene()
	}

	void initLucene()
		throws LuceneConfigException, LuceneException
	{
		final String kFName = "initLucene";
		final String kExTag = kClassName + '.' + kFName + ": ";

		String indexPath = getLuceneIndexPath();
		File filePath = new File( indexPath );
		if( ! filePath.exists() )
			throw new LuceneConfigException( kExTag
				+ "Lucene index path not found."
				+ " " + INDEX_LOCATION_FILE_PATH + "=\"" + indexPath + "\""
				);
		if( ! filePath.canRead() )
			throw new LuceneConfigException( kExTag
				+ "Cannot read Lucene index path."
				+ " " + INDEX_LOCATION_FILE_PATH + "=\"" + indexPath + "\""
				);
		if( ! filePath.isDirectory() )
			throw new LuceneConfigException( kExTag
				+ "Lucene index path is not a directory."
				+ " " + INDEX_LOCATION_FILE_PATH + "=\"" + indexPath + "\""
				);

		try {
			fIndexSearcher = new IndexSearcher( indexPath );
		}
		catch( IOException ioe ) {
			throw new LuceneConfigException( kExTag
				+ "Error opening Lucene index"
				+ " \"" + indexPath + "\"."
				+ " Error was: " + ioe
				);
		
		}

		// Init other items
		getLuceneAnalyzer();
		getLuceneFieldFilterOrNull();

		// Parser is not thread safe, must be instantiated for each search
	}

	void closeLucene()
		throws IOException
	{
		fIndexSearcher.close();
	}
	void _closeLuceneIndex()
		throws LuceneException
	{
		final String kFName = "closeLuceneIndex";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		if( null!=fIndexSearcher ) {
			try {
				fIndexSearcher.close();
			}
			catch( IOException ioe ) {
				throw new LuceneException( kExTag
					+ "Error closing Lucene index."
					+ " Error was: " + ioe
					);
			}
			fIndexSearcher = null;
		}
	}


	public String getLuceneIndexPath()
		throws LuceneConfigException
	{
		final String kFName = "getLuceneIndexPath";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( ! fUseCache && null == cIndexPath )
		{
			cIndexPath = fConfigTree.getTextByPathTrimOrNull(
				INDEX_LOCATION_FILE_PATH
				);
			if( null==cIndexPath )
				throw new LuceneConfigException( kExTag
					+ "No Lucene index path given."
					+ " Must set \"" + INDEX_LOCATION_FILE_PATH + "\""
					);
		}
		return cIndexPath;

	}

	public List getSearchFieldsAsList()
		throws LuceneConfigException
	{
		final String kFName = "getSearchFieldsAsList";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( ! fUseCache && null == cSearchFieldsAsList )
		{
			// was .getTextByPathTrimOrNull
			cSearchFieldsAsList = fConfigTree.getListFromCsvTextByPathTrim(
				SEARCH_FIELDS_PATH
				);
			if( cSearchFieldsAsList.size() < 1 )
				throw new LuceneConfigException( kExTag
						+ "No Lucene fields configured for searching."
						+ " Must set \"" + SEARCH_FIELDS_PATH + "\""
						);
			cSearchFieldsAsArray = new String [ cSearchFieldsAsList.size() ];
			cSearchFieldsAsArray = (String []) cSearchFieldsAsList.toArray( cSearchFieldsAsArray );
			/***
			for( int i = 0; i < cSearchFieldsAsList.size() ; i++ ) {
				cSearchFieldsAsArray[ i ] = (String) cSearchFieldsAsList.get( i );
			}
			cSearchFieldsAsArray = cSearchFieldsAsList.toArray( java.lang.String [] );
			***/
		}
		return cSearchFieldsAsList;
	}
	String [] getSearchFieldsAsArray()
		throws LuceneConfigException
	{
		if( null==cSearchFieldsAsArray )
			getSearchFieldsAsList();
		return cSearchFieldsAsArray;
	}
	public List getDisplayFields()
		throws LuceneConfigException
	{
		final String kFName = "getDispalyFields";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( ! fUseCache && null == cDisplayFields )
		{
			// was .getTextByPathTrimOrNull
			cDisplayFields = fConfigTree.getListFromCsvTextByPathTrim(
				DISPLAY_FIELDS_PATH
				);
			// if( cDisplayFields.size() < 1 )
			//	cDisplayFields.add( DEFAULT_DISPLAY_FIELD );
		}
		return cDisplayFields;
	}

	Analyzer getLuceneAnalyzer()
		throws LuceneConfigException, LuceneException
	{
		if( null==cAnalyzer ) {
			cAnalyzer = new StandardAnalyzer();
		}
		return cAnalyzer;
	}
	FieldSelector getLuceneFieldFilterOrNull()
		throws LuceneConfigException, LuceneException
	{
		if( null==cFieldFilter ) {
			if( getDisplayFields().size() > 0 ) {
				cFieldFilter = new MapFieldSelector( getDisplayFields() );
			}
		}
		return cFieldFilter;
	}

	/***
	public String getDocumentContentField()
		throws LuceneConfigException
	{
		final String kFName = "getDocumentContentField";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( ! fUseCache && null == cContentField )
		{
			cContentField = fConfigTree.getTextByPathTrimOrNull(
				CONTENT_FIELD_PATH
				);
			if( null==cContentField )
				cContentField = DEFAULT_SEARCH_FIELD;
		}
		return cContentField;
	}
	***/

	IndexSearcher getLuceneIndex()
		throws LuceneException
	{
		final String kFName = "getLuceneIndex";
		final String kExTag = kClassName + '.' + kFName + ": ";
		if( null==fIndexSearcher )
			throw new LuceneException( kExTag + "Lunene index is not initialized." );
		return fIndexSearcher;
	}

	String getAndSetupOptionalReportTemplate()
	{
		if( ! fUseCache
			&& null != fConfigTree
			&& null == cTweakedTemplate
			)
		{
			final String kFName = "getAndSetupOptionalReportTemplate";
	
			// Get the URI, if any
			String templateURI = getTemplateURI();
			// It's perfectly fine no to have one
			if( null == templateURI )
				return null;
	
			// Now get the contents
			String templateContents = null;
			AuxIOInfo aux = new AuxIOInfo();
			try
			{
				templateContents = NIEUtil.fetchURIContentsChar(
					// templateURI, getMainApp().getConfigFileURI(),
					templateURI, getMainConfig().getConfigFileURI(),
					null, null,	// optUsername, optPassword,
					aux, false
					);
			}
			catch( IOException e )
			{
				errorMsg( kFName,
					"Error fetching report template URI " + templateURI
					+ " Will use default report template."
					+ " Error: " + e
					);
				return null;
			}
			// A sanity check, I really hate null pointers
			if( null == templateContents )
			{
				errorMsg( kFName,
					"Got back a null report template from URI " + templateURI
					+ " Will use default report template."
					);
				return null;
			}
	
			// Add the base href stuff, if possible
			templateContents = NIEUtil.markupHTMLWithBaseHref(
				templateContents, templateURI, true
				);
			// Sanity check, this may have failed
			if( null == templateContents )
			{
				errorMsg( kFName,
					"Got back a null report template from URI " + templateURI
					+ " Will use default report template."
					);
				return null;
			}
	
			// We're getting really close...
	
			// get the marker text and options
			List patterns = getMarkerLiteralText();
			if( null == patterns || patterns.size() < 1 )
			{
				errorMsg( kFName,
					"No patterns defined to mark substitution, returning null."
					);
				return null;
			}
	
			// Get some other flags from the Search Engine config
			boolean goesAfter =
				getIsMarkerNewTextInsertAfter();
			// statusMsg( kFName, "goesAfter=" + goesAfter );
			boolean doesReplace =
				getIsMarkerReplaced();
			boolean isCasen =
				getIsMarkerCaseSensitive();
	
	
			/****
			debugMsg( kFName,
				"Will process input doc with " + inSourceDoc.length() + " chars"
				+ ", Snippet with " + inSnippet.length() + " chars"
				+ ", and " + patterns.size() + " pattern(s)."
				+ " Options:"
				+ " goesAfter=" + goesAfter
				+ " doesReplace=" + doesReplace
				+ " isCasen=" + isCasen
				);
			*******/
	
	
	
	
	
			// call the substituion routine
			// Do the markup
			String testDoc = NIEUtil.markupStringWtihString(
				templateContents, "test", patterns,
				goesAfter, doesReplace, isCasen
				);
	
	
			if( null == testDoc )
			{
				errorMsg( kFName,
					"Did not find target pattern in report template."
					+ " Reports will still work, but will NOT"
					+ " have a custom look and feel."
					+ " Template = \"" + templateURI + "\""
					);
				return null;
			}
	
			// cache the results
			cTweakedTemplate = templateContents;
	
	
			statusMsg( kFName,
				"Will use report template \"" + aux.getFinalURI() + "\""
				);

		}	// End if not already cached

		// return the answer
		return cTweakedTemplate;

	}









	public String getConfiguredReportsDir()
	{
		if( ! fUseCache && null == cInitReportDir )
		{
			if( null != fConfigTree )
				cInitReportDir = fConfigTree.getTextByPathTrimOrNull(
					REPORT_DIR_PATH
					);
		}
		return cInitReportDir;
	}

	public String getTemplateURI()
	{
		if( ! fUseCache && null == cTemplateURI )
		{
			if( null != fConfigTree )
				cTemplateURI = fConfigTree.getTextByPathTrimOrNull(
					TEMPLATE_URI_PATH
					);
		}
		return cTemplateURI;
	}

	public List getMarkerLiteralText()
	{
		// The string to look for when we want to insert a webmaster suggests
		// Todo: would be nice to support more than one of these
		// Todo: would be nice to support literal or regex
		// Todo: would be nice to say before or after
		// Todo: would be nice to say "keep this text" or dump it after subst
		if( ! fUseCache )
		{
			if( null != fConfigTree )
				cMarkerList = fConfigTree.getTextListByPathNotNullTrim(
					TARGET_MARKER_TEXT_PATH
					);
		}
		return cMarkerList;
	}

	public boolean getIsMarkerNewTextInsertAfter()
	{
		if( ! fUseCache )
		{
			if( null != fConfigTree )
				cIsInsertAfter = fConfigTree.getBooleanFromSinglePathAttr(
					TARGET_MARKER_MODIFIER_PATH,
					MARKER_TEXT_AFTER_ATTR,
					DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
					);
			else
				cIsInsertAfter = DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER;
		}
		return cIsInsertAfter;
	}



	public boolean getIsMarkerReplaced()
	{
		if( ! fUseCache )
		{
			if( null != fConfigTree )
				cIsMarkerReplaced = fConfigTree.getBooleanFromSinglePathAttr(
					TARGET_MARKER_MODIFIER_PATH,
					MARKER_TEXT_REPLACE_ATTR,
					DEFAULT_SUGGESTION_MARKER_IS_REPLACED
					);
			else
				cIsMarkerReplaced = DEFAULT_SUGGESTION_MARKER_IS_REPLACED;
		}
		return cIsMarkerReplaced;
	}

	public boolean getIsMarkerCaseSensitive()
	{
		if( ! fUseCache )
		{
			if( null != fConfigTree )
				cIsMarkerCasen = fConfigTree.getBooleanFromSinglePathAttr(
					TARGET_MARKER_MODIFIER_PATH,
					MARKER_TEXT_CASEN_ATTR,
					DEFAULT_SUGGESTION_MARKER_IS_CASEN
					);
			else
				cIsMarkerCasen = DEFAULT_SUGGESTION_MARKER_IS_CASEN;
		}
		return cIsMarkerCasen;
	}

	public nie.sn.SearchTuningConfig getMainConfig()
	{
		return fConfig;
	}
	public nie.sn.SearchTuningApp _getMainApplication()
	{
		return _fApp;
	}

	public String getMainURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}

	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fApp;
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


	nie.sn.SearchTuningApp _fApp;
	nie.sn.SearchTuningConfig fConfig;


	JDOMHelper fConfigTree;

	// Cached variable flag
	// Off by default
	private boolean fUseCache;


	String cInitReportDir;
	String cTemplateURI;
	String cTweakedTemplate;
	List cMarkerList;
	boolean cIsInsertAfter;
	boolean cIsMarkerReplaced;
	boolean cIsMarkerCasen;

	String cIndexPath;
	public static final String INDEX_LOCATION_FILE_PATH = "index_directory";

	String cDefaultSearchField;
	static final String SETTABLE_DEFAULT_SEARCH_FIELD_PATH = "default_search_field";

	public static final String DEFAULT_SEARCH_FIELD =
		PumpConstants.DEFAULT_CONTENT_FIELD_NAME
		;

	// private IndexSearcher _cIndexSearcher;
	// private IndexSearcher _fLuceneIndexSearcher;
	// ^^^ Two older names
	// 1: "c" prefix implies "cache", which implies good at any time
	//    where as f implies a fully and verfied/initialized field.
	//    We want the latter in this case.
	// 2: Everything in this class is "Lucene", no need to keep adding
	//    it to field names
	private IndexSearcher fIndexSearcher; // * good
	private Analyzer cAnalyzer;
	// private QueryParser cParser;  // Query parser is NOT thread safe, all else OK
	private FieldSelector cFieldFilter;


	// String cContentField;
	List cSearchFieldsAsList;
	String [] cSearchFieldsAsArray;
	List cDisplayFields;
	// public static final String CONTENT_FIELD_PATH = "content_field";
	public static final String SEARCH_FIELDS_PATH = "search_fields";
	public static final String DISPLAY_FIELDS_PATH = "display_fields";

	public static final String _DEFAULT_DISPLAY_FIELD = "title";
	// ^^^ NO, not standard, they must set it
	
	final static String REPORT_DIR_PATH = "reports_directory";
	final static String TEMPLATE_URI_PATH = "surround_template";

	// Markers for figuring out where to put the various suggestions
	// Can have more than one!
	private static final String TARGET_MARKER_TEXT_PATH =
		"marker_text";
	// A flag that modifies the behavior of all the markers specified
	private static final String TARGET_MARKER_MODIFIER_PATH =
		TARGET_MARKER_TEXT_PATH;
		// TARGET_MARKER_TEXT_PATH + "_modifiers";
	// Atributes to change the modified behavior


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

};


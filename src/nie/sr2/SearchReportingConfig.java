package nie.sr2;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;


import nie.core.*;
// import nie.sn.*;
import org.jdom.Element;
import org.jdom.Document;

// A class to encapsulate all global config data.
// This will allow the main app to swap out configs easily
public class SearchReportingConfig
{

	private final static String kClassName = "SearchReportingConfig";


	// This is where the work actually gets done
	// to start up the server.
	// IT"S OK TO HAVE A NULL CONFIG, MEANS USE DEFAULTS
	///////////////////////////////////////////
	public SearchReportingConfig(
		Element inElement,
		// nie.sn.SearchTuningApp inApp
		nie.sn.SearchTuningConfig inConfig
		)
		throws ReportConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// if( null == inApp )
		if( null == inConfig )
			throw new ReportConfigException( kExTag
				+ "Null application configuration passed in."
				);
		// Store a reference to the main application
		// fApp = inApp;
		fConfig = inConfig;

		// create jdom element and store info
		// Sanity checks
		// IT"S OK TO HAVE A NULL CONFIG, MEANS USE DEFAULTS
		// if( inElement == null )
		//	throw new ReportConfigException( kExTag
		//		+ "Constructor was passed in a NULL element."
		//		);

		if( null != inElement )
		{
			// Instantiate and store the main JDOMHelper a
			fConfigTree = null;
			try
			{
				fConfigTree = new JDOMHelper( inElement );
			}
			catch (JDOMHelperException e)
			{
				throw new ReportConfigException( kExTag
					+ "got JDOMHelper Exception: "
					+ e );
			}
			if( null == fConfigTree )
				throw new ReportConfigException( kExTag
					+ "Got back a NULL xml tree when trying to create"
					+ " a Search Engine Configuration object."
					);

			statusMsg( kFName,
				"FYI: User configured reports."
				);
		}
		else
		{
			statusMsg( kFName,
				"FYI: Using default reports configuration."
				);
		}


		// Parse "global" options in the config file
		initFieldCache();

	};



	private void initFieldCache()
		throws ReportConfigException
	{
		final String kFName = "initFieldCache";

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



	final static String REPORT_DIR_PATH = "reports_directory";
	final static String TEMPLATE_URI_PATH = "surround_template";

	// Markers for figuring out where to put the various suggestions
	// Can have more than one!
	private static final String TARGET_MARKER_TEXT_PATH =
		"marker_text";
	// A flag that modifies the behavior of all the markers specified


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


	// By default, where do we put the new text
	private static final boolean DEFAULT_SUGGESTION_MARKER_NEW_TEXT_AFTER
		= true;
	// By default, should we replace the marker text when we find it


	// By default, should we replace the marker text when we find it
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_REPLACED
		= false;
	// By default, are the patterns case sensitive


	// By default, are the patterns case sensitive
	private static final boolean DEFAULT_SUGGESTION_MARKER_IS_CASEN
		= false;






};


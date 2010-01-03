package nie.webui;

import java.util.*;
import java.sql.*;
import java.lang.reflect.*;

import org.jdom.Element;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.xpath.*;

import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import nie.core.*;
import nie.sn.CSSClassNames;
import nie.sn.SnRequestHandler;
import nie.sr2.ReportConstants;
import nie.sr2.ReportLink;


/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public abstract class XMLDefinedScreen
	extends BaseScreen
	// implements UiXmlScreenHelperInterface
{


	public static final String kStaticClassName = "XMLDefinedScreen";
	public String kClassName() {
		return "XMLDefinedScreen";
	}



	public static final String kStaticFullClassName = "nie.webui.XMLDefinedScreen";
	protected String kFullClassName() {
		return "nie.webui.XMLDefinedScreen";
	}


	// Todo:
	// \ reverse dns
	// filter expressions
	// optional filter if no cgi field
	// \ column ID
	// \ ! paging links
	// cgi vars to clear when returning to main menu
	// "TOP" N rows optimization
	// \ overall content table
	// \ settable desired row count by report, and enforceable
	// utility to carefully check and add if needed the from, where,
	// 	sort by, order by, etc. to clauses
	// gropu by, vs breaks???
	// desired table tag TABLE
	// Easy settings for callpadding and cellspacing, alignment, width
	// desired header row tag TR
	// desired data row tag TR
	// desired data cell TD
	// desired header cell TH
	// settable cell classes
	// \ null value
	// \ reformat of boolean
	// reformat of numbers
	// \ data types, reformat of dates, numbers, etc.
	// sort options
	// sort defs
	// filter options, filter defs?
	// \ filter fields, click data links
	// link gen?: join text, like "... in last:"
	// ?\ link targets (_blank, _top, etc.)
	// \ external links, maybe with raw href??? but with parms?
	// image links
	// derived fields
	// report breaks, before and after
	// handling * and other arbitrary field list issues
	// "content goes here"
	// default settings, maybe a report that is used as a template
	// \ Surround templates for look and feel
	// named CSS sheets
	// external vs embedded CSS
	// later, named XSLT sheets
	// . menu bar!
	// security levels

	// Far cosmetic:
	// ---------------
	// allow for no column headings
	// allow for blank column headings
	// allow XHTML tags in column headings
	// spanning column headings?


	public static XMLDefinedScreen screenFactory(
			nie.sn.SearchTuningConfig inMainConfig,
			String inScreenName
		)
		throws UIException
	{
		final String kFName = "XMLDefinedScreen";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		inScreenName = NIEUtil.trimmedStringOrNull( inScreenName );
		if( null==inScreenName )
			throw new UIException( kExTag + "Null/empty screen name passed in." );

		// One time setup
		if( null==fsCachedScreens )
			fsCachedScreens = new Hashtable();
		if( null==fsSearchedForScreens )
			fsSearchedForScreens = new HashSet();

		// Return previously cached success or failure
		if( fsCachedScreens.containsKey( inScreenName ) )
			return (XMLDefinedScreen) fsCachedScreens.get( inScreenName );
		if( fsSearchedForScreens.contains( inScreenName ) )
			throw new UIException( kExTag + "Previous attempt to get screen \"" + inScreenName + "\" failed." );

		// Remember we've seen this one
		fsSearchedForScreens.add( inScreenName );

		// Find the XML definition
		JDOMHelper formElem = getXML( inScreenName );


		// Find the class
		// call the contructor
		XMLDefinedScreen newScreen = null;

		String screenURI = SUBCLASS_PREFIX + inScreenName;
		// debugMsg( kFName, "Screen class is \"" + screenURI + "\"." );


		// nie.sn.SearchTuningApp inMainApp,
		//		String
		Class lScreenClass;

		try {
			lScreenClass = Class.forName( screenURI );
		} catch( ClassNotFoundException cnfe ) {
			// if( inShowNotFoundErrors )
				throw new UIException( "Can't load UI screen class \"" + screenURI + "\", error:" + cnfe );
			// else
			//	return null;
		}

		// Class[] lConstructorSignature = new Class[3];
		// lConstructorSignature[0] = lQueueClassName.getClass();
		// lConstructorSignature[1] = this.getClass();
		// lConstructorSignature[2] = inElement.getClass();

		Class[] lConstructorSignature = new Class [] {
			// nie.sn.SearchTuningApp.class,
			nie.sn.SearchTuningConfig.class,
			JDOMHelper.class,
			String.class
		};


		Constructor lConstructor = null;

		try {
			lConstructor = lScreenClass.getConstructor( lConstructorSignature );
		} catch( NoSuchMethodException nsme ) {
			throw new UIException( "Could not find appropriate constructor for report class \"" + screenURI + "\"." );
		}

		try {
			// Create the args we will pass to the constructor
			// Object [] lParams = new Object[2];
			// lParams[0] = fMainApp;
			// lParams[1] = inReportName;
			Object [] lParams = new Object[] {
				// fMainApp,
				// getMainConfig(),
				inMainConfig,
				formElem,
				inScreenName
			};
			newScreen = (XMLDefinedScreen) lConstructor.newInstance( lParams );
		} catch( Exception ie ) {
ie.printStackTrace( System.err );
			throw new UIException( "Unable to instantiate object for UI screen class \"" + screenURI + "\". Error: " + ie );
		}

		// Save screen in the cache
		fsCachedScreens.put( inScreenName, newScreen );


		return newScreen;

	}


	public XMLDefinedScreen(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper inScreenDefinitionElement,
		String inShortScreenName
		)
			throws UIConfigException, UIException
	{
		// super( inMainApp, inShortReportName );
		super( inMainConfig, inShortScreenName );
		// super( inMainConfig );

		final String kFName = "constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// we also need a report definition
		if( null==inScreenDefinitionElement )
			throw new UIConfigException( kExTag
				+ "Null XML screen definition passed in"
				+ " for report " + getScreenName()
				);
		fMainElem = inScreenDefinitionElement;

		// Fill in the cache
		initCachedFields();

		registerFormFields( getXML() );
	}

	private void initCachedFields()
		throws UIConfigException, UIException
	{
		// fUseCache = false;
		setDontUseCache();

		getTitle( null );


		getNewRecordCgiCopyOverField();
		getLookupKeyCgiField();
		getLookupReportName();
		getIdCgiField();



		/***
		generateSQL( null, null );
		// getFieldObjects();
		// ^^^ called and cached by getSQLFieldsSelectString()

		getLinkText( null );
		getSubtitleOrNull( null );

		// Whether any of the fields have sum/count/avg/min/max
		getShouldDoStatistics();
		// also caches cFirstStatsFieldOffset
		getGroupStatsRowLabel();

		// Do this one last
		getShouldDoVariableSubstitutions();

		// It's OK not to have them
		// and it's not fatal if one of
		// them is mangled
		getSuggestedLinksOrNull();
		***/

		// fUseCache = true;
		setUseCache();
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
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==inRequestObject )
			throw new UIException( kExTag + "Null request object passed in." );
		if( null==ioResponseObject )
			throw new UIException( kExTag + "Null response object passed in." );

		// get the mode, typically "forgen" or "submit"
		String mode = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.MODE_CGI_FIELD );
		mode = NIEUtil.trimmedLowerStringOrNull( mode );
		if( null==mode )
			throw new UIException( kExTag +
				"Null submit mode in CGI parameters."
				+ " Expected CGI field \"" + UILink.MODE_CGI_FIELD + "\""
				+ " to be one of " + UILink.kValidModes
				);

		debugMsg( kFName, "mode=" + mode );

		// We're either generating a form or processing a submitted form
		if( mode.equals(UILink.UI_MODE_FORMGEN) )
		{
			return processFormGen( inRequestObject, ioResponseObject, inDoFullPage );
		}
		else if( mode.equals(UILink.UI_MODE_SUBMIT)
				|| mode.equals(UILink.UI_MODE_COMMIT)
				|| mode.equals(UILink.UI_MODE_CANCEL)
			)
		{
			return processDataSubmission( inRequestObject, ioResponseObject, inDoFullPage, mode );
		}
		else
		{
			throw new UIException( kExTag +
				"Invalid submit mode \"" + mode + "\""
				+ " in CGI field \"" + UILink.MODE_CGI_FIELD + "\""
				+ ", should be one of " + UILink.kValidModes
				);
		}

	}



	public abstract Element processDataSubmission(
			AuxIOInfo inRequestInfo, AuxIOInfo inResponsInfo,
			boolean inIsFullPage, String inMode
		) throws UIException
		;


	// Generate, or RE-generate, a form, either to add or edit (or eventually view)
	public Element processFormGen( AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
		boolean inDoFullPage
		)
			throws UIException // , UIConfigException
	{
		final String kFName = "processFormGen";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );
		if( null==ioResponseObject )
		   throw new UIException( kExTag + "Null response object passed in." );

		// Which operation is being attempted
		// get the mode, typically "forgen" or "submit"
		String operation = inRequestObject.getScalarCGIFieldTrimOrNull(
			UILink.OPERATION_CGI_FIELD
			);
		operation = NIEUtil.trimmedLowerStringOrNull( operation );
		if( null==operation )
			throw new UIException( kExTag +
				"Null operation in CGI parameters."
				+ " Expected CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
				+ " to be one of " + UILink.kValidOperations
				);


		// Get the form ready
		Element formData = getBlankForm();
		setupReturnUrlInFormIfDesired( formData, inRequestObject );

		// In order to edit a map, we have to locate it.
		// To locate a map, you either need to give us a map ID or
		// a term that would be in that map
		// We check for the term here, but can also check for a map ID
		String possibleTargetKey = getTargetKeyOrNullFromCgi( inRequestObject );

		// They may have sent us a target ID
		int possibleTargetID  = getPossibleIDFromCgi( inRequestObject );

		// Whether we need to redirect to selector screen or stay here
		boolean doRedirToItemSelector = false;

		// We're either generating a form or processing a submitted form
		if( operation.equals(UILink.UI_OPERATION_ADD) )
		{
			// Mostly leave blank
			augmentWithMiscDefaults( formData, inRequestObject );
			// This is for things like the term that was passed in
			augmentDefaultsFromCGI( formData, inRequestObject );
			// Tweak the dropdown lists to reflect the current defaults
			augmentFormFromSystemDefaults( formData );

		}
		// This is an edit operation
		else if( operation.equals(UILink.UI_OPERATION_EDIT) )
		{

			// Pre-fills the form with data for that map
			// also signals with a false if there's no specific map ID
			// sent AND therer's MULTIPLE maps matching the target term
			doRedirToItemSelector = ! augmentFormFromExistingData(
				formData, possibleTargetKey, possibleTargetID
				);

		}
		else if( operation.equals(UILink.UI_OPERATION_DELETE)
			|| operation.equals(UILink.UI_OPERATION_VIEW)
			)
		{
			doRedirToItemSelector = ! augmentFormFromExistingData( formData, possibleTargetKey, possibleTargetID );


		}
		else
		{
			throw new UIException( kExTag +
				"Invalid operation \"" + operation + "\""
				+ " in CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
				+ ", should be one of " + UILink.kValidOperations
				);
		}


		// Return the results
		if( ! doRedirToItemSelector ) {
			return displayForm( formData, operation, inRequestObject, inDoFullPage );
		}
		// Else we want a redirect
		else {
			return setupSelectorRedirect( inRequestObject );
		}
	}


	// We have decided that there is nore than one object that they
	// may wish to edit, it's ambiguous.
	// So we will redirect them to a report that lists all the choices.
	Element setupSelectorRedirect( AuxIOInfo inRequestObject )
		throws UIException
	{
		final String kFName = "setupSelectorRedirect";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inRequestObject )
			throw new UIException( kExTag +
				"Null request object passed in."
				);

		String reportName = getLookupReportName();
		if( null==reportName )
			throw new UIException( kExTag +
				"No disambiguation report found."
				);

		String keyField = getLookupKeyCgiField();
		String value = (null!=keyField)
			? inRequestObject.getScalarCGIFieldTrimOrNull( keyField )
			: null
			;


		try {
			ReportLink rptLink = new ReportLink(
				getMainConfig(),				// nie.sn.SearchTuningConfig inMainConfig,
				reportName,	// String inReportName,
				"Choose an item",				// String inLinkText,
				null,							// String optLinkTitle,
				null,							// String optCssClass,
				keyField,		// String optParmName,
				null							// String optParmDefaultValue
			);

			// Make sure we have the correct "return" URL passed through
			/*** already take care of by call to setupReturnUrlInFormIfDesired in formGen
			if( null!=returnURL )
				inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );
			***/

			// Generate the new link
			Element reportLinkElem = rptLink.generateRichLink(
				inRequestObject,	// AuxIOInfo inRequest,
				null,				// String optReportName,
				null,				// String optFilterName,
				false,				// boolean inIsMenuLink,
				value,		// String optNewParmValue
				null,		// Hashtable optVariables
				null		// String optNewLinkText
				);
			// Sanity
			if( null==reportLinkElem )
				throw new UIException( kExTag +
					"Got back null link for item \"" + value + "\""
					);
			String newURL = reportLinkElem.getAttributeValue( "href" );
			newURL = NIEUtil.trimmedStringOrNull( newURL );
			if( null==newURL )
				throw new UIException( kExTag +
					"Got back link with null/empty href for item \"" + value + "\""
					);
			// Now we recycle it and turn it into a redirect
			Element outElem = new Element( "redirect" );
			outElem.addContent( newURL );
			return outElem;

		}
		catch( Exception e ) {
			throw new UIException( kExTag +
				"Error generating link to item Selector report. Error: " + e
				);
		}
	}


	public void augmentWithMiscDefaults(
			Element inFormData, AuxIOInfo inRequest
		)
			throws UIException
	{
		// do nothing, override if you want to do something
	}
	// Setup things like drop down lists, etc.
	public void augmentFormFromSystemDefaults(
		Element inFormData //, AuxIOInfo inRequest
	)
		throws UIException
	{
		// do nothing, override if you want to do something
	}

	abstract public void augmentFormFromCGIInput(
			Element ioBlankForm, AuxIOInfo inRequestObject
		)
		throws UIException;

	public void augmentDefaultsFromCGI(
			Element inFormData, AuxIOInfo inRequest
		)
		throws UIException
	{
		final String kFName = "augmentDefaultsFromCGI";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// Do we have a field to look at?
		String cgiFieldName = getNewRecordCgiCopyOverField();
		if( null!=cgiFieldName ) {

			String fieldValue = inRequest.getScalarCGIFieldTrimOrNull( cgiFieldName );

			// We have a value
			if( null!=fieldValue ) {

				setFormFieldValue( inFormData, cgiFieldName, fieldValue );

			}
		}
	}


	public abstract boolean augmentFormFromExistingData(
		Element ioBlankForm, String inTargetTerm,
		int optTargetID
		) throws UIException
		;

	// Given a specific field exception, markup the form
	// with that field, and with any error message
	// Note that this method is not responsible for adding in the values
	// from the CGI request itself, see augment from cgi method for that
	public void augmentFromFieldException(
		Element ioBlankForm,
		InvalidFormInputException inBadFieldException
		)
			throws UIException
	{
		final String kFName = "augmentFromFieldError";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);
		if( null==inBadFieldException )
			throw new UIException( kExTag +
				"Null field error data passed in."
				);

		// Tweak the dropdown lists to reflect the current defaults
		augmentFormFromSystemDefaults( ioBlankForm );

		String msg = inBadFieldException.getShortMessage();
		if( null==msg )
			throw new UIException( kExTag +
				"Null error message in exception."
				);

		String cgi = inBadFieldException.getCGIFieldName();
		String label = null;
		String xml = null;
		if( null!=cgi ) {
			// String label = inBadFieldException.getFieldLabel();
			label = getFormLabelForFieldOrNull( cgi );
			// once in a blue moon they tell us a special path
			xml = inBadFieldException.getOptXMLFormPath();
			// But most of the time we look it up ourselves
			if( null==xml )
				xml = getFormPathForField( cgi );
				// ^^^ throws exception if it is not happy
		}

		// Add the overall message, if any
		if( null!=msg ) {
			if( null!=label )
				msg = label + ": " + msg;
			msg = "Error: " + msg;
			// Error message is an attribute of the very top element
			ioBlankForm.setAttribute( "message", msg );
			ioBlankForm.setAttribute( "severity", "error" );
		}

		if( null!=xml ) {
			Element tmpElem = JDOMHelper.findOrCreateElementByPath(
				ioBlankForm,
				xml
				// + "/@is_error=TRUE"
				+ "/@is_flagged=TRUE"
				+ "/@severity=error"
				,
				true
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add field flag."
					+ " XML form path was " + xml
					);
		}


	}


	// Given a specific message and possibly a field, markup the form
	// with that field, and with any error message
	// Note that this method is not responsible for adding in the values
	// from the CGI request itself, see augment from cgi method for that
	public void augmentWithTopMessage(
		Element ioBlankForm,
		String inMessage,
		String optSeverity,
		String optFieldName
		)
			throws UIException
	{
		final String kFName = "augmentWithTopMessage";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);
		inMessage = NIEUtil.trimmedStringOrNull( inMessage );
		if( null==inMessage )
			throw new UIException( kExTag +
				"Null field error data passed in."
				);

		optSeverity = NIEUtil.trimmedStringOrNull( optSeverity );
		optSeverity = (null!=optSeverity) ? optSeverity : "warning";

		optFieldName = NIEUtil.trimmedStringOrNull( optFieldName );


		// Tweak the dropdown lists to reflect the current defaults
		augmentFormFromSystemDefaults( ioBlankForm );

		String label = null;
		String xml = null;
		if( null!=optFieldName ) {
			// String label = inBadFieldException.getFieldLabel();
			label = getFormLabelForFieldOrNull( optFieldName );
			xml = getFormPathForField( optFieldName );
			// ^^^ throws exception if it is not happy
		}

		// Add the overall message, if any
		// messages are an attribute of the very top element
		ioBlankForm.setAttribute( "message", inMessage );
		ioBlankForm.setAttribute( "severity", optSeverity );

		if( null!=xml ) {
			Element tmpElem = JDOMHelper.findOrCreateElementByPath(
				ioBlankForm,
				xml
				+ "/@is_flagged=TRUE"
				+ "/@severity=" + optSeverity
				,
				true
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add field flag."
					+ " XML form path was " + xml
					);
		}


	}







	public Element redisplayBadFormSubmission(
		AuxIOInfo inRequestObject,
		AuxIOInfo inResponseObject,
		// Element inForm,
		// String inMode,	// NO, mode is implicitly changing from submit to formgen
		// String inOperation,
		boolean inDoFullPage,
		InvalidFormInputException optBadFieldException
		)
			throws UIException
	{
		final String kFName = "redisplayBadFormSubmission";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==inRequestObject )
			throw new UIException( kExTag +
				"Null CGI request object passed in."
				);
		if( null==inResponseObject )
			throw new UIException( kExTag +
				"Null CGI response object passed in."
				);
		// if( null==inMode )
		//	throw new UIException( kExTag +
		//		"Null mode passed in."
		//		);

		// Which operation is being attempted
		// get the mode, typically "forgen" or "submit"
		String operation = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
		operation = NIEUtil.trimmedLowerStringOrNull( operation );
		if( null==operation )
			throw new UIException( kExTag +
				"Null operation in CGI parameters."
				+ " Expected CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
				+ " to be one of " + UILink.kValidOperations
				);

		/***
		// We start with a blank form
		Element tmpBlankForm = getXML().getJdomElement();
		// We always make our own copy
		Element formData = (Element)tmpBlankForm.clone();
		***/
		Element formData = getBlankForm();


		// Setup where we are supposed to return them to, either after
		// a success or cancel (an error brings them back here)
		String returnURL = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.RETURN_URL_CGI_FIELD );
		// If this is the first time through, grab the referer
		if( null==returnURL )
			returnURL = inRequestObject.getReferer();
		if( null==returnURL ) {
			// throw new UIException( kExTag +
			errorMsg( kFName,
				"Unable to add get return URL."
				+ " Expected CGI field \"" + UILink.RETURN_URL_CGI_FIELD + "\""
				+ " or valid referer field."
				);
		}
		else
		{
			setFormFieldValue( formData, UILink.RETURN_URL_CGI_FIELD, returnURL );
		}

		debugMsg( kFName, "returnURL=" + returnURL );

		// We re-prefill the form with what they had just typed in
		augmentFormFromCGIInput( formData, inRequestObject );

		// Augment the form with user error information, if any
		if( null!=optBadFieldException )
			augmentFromFieldException(
				formData,
				optBadFieldException
				);

		// And let displayForm do the rest
		return displayForm( formData, operation, inRequestObject, inDoFullPage );
	}




	String getNewRecordCgiCopyOverField()
		throws UIException
	{
		if( getShouldUseCache() )
			cCgiCopyOverField = getMainElem().getStringFromAttributeTrimOrNull( CGI_NEW_RECORD_COPY_OVER_FIELD_ATTR );
		return cCgiCopyOverField;
	}
	String getLookupKeyCgiField()
		throws UIException
	{
		if( getShouldUseCache() )
			cCgiLookupField = getMainElem().getStringFromAttributeTrimOrNull( CGI_LOOKUP_FIELD_ATTR );
		return cCgiLookupField;
	}
	String getLookupReportName()
		throws UIException
	{
		if( getShouldUseCache() )
			cLookupReport = getMainElem().getStringFromAttributeTrimOrNull( LOOKUP_REPORT_ATTR );
		return cLookupReport;
	}
	String getIdCgiField()
		throws UIException
	{
		if( getShouldUseCache() )
			cIdCgiField = getMainElem().getStringFromAttributeTrimOrNull( CGI_ID_FIELD_ATTR );
		return cIdCgiField;
	}

	boolean getShouldUseCache()
	{
		return fUseCache;
	}
	void setUseCache()
	{
		fUseCache = true;
	}
	void setDontUseCache()
	{
		fUseCache = false;
	}

	JDOMHelper getMainElem()
		throws UIException
	{
		final String kFName = "getMainElem";
		final String kExTag = kClassName() + '.' + kFName + " ";
		if( null==fMainElem )
			throw new UIException( kExTag + "Null configuation tree; object not initialized.");
		return fMainElem;		
	}

	String getTargetKeyOrNullFromCgi( AuxIOInfo inRequestObject )
		throws UIException
	{
		String keyField = getLookupKeyCgiField();
		if( null!=keyField )
			return inRequestObject.getScalarCGIFieldTrimOrNull( keyField );
		else
			return null;
	}


	int getPossibleIDFromCgi( AuxIOInfo inRequestObject )
		throws UIException
	{
		String idField = getIdCgiField();

		String tmpStr = null;
		if( null!=idField )
			tmpStr = inRequestObject.getScalarCGIFieldTrimOrNull( idField );

		return NIEUtil.stringToIntOrDefaultValue( tmpStr, -1, false, true );
	}


	public Element displayForm(
		Element inFormTree, String inOperation, AuxIOInfo inRequestObject, boolean inDoFullPage
		)
		throws UIException // , UIConfigException
	{
		final String kFName = "displayForm";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );
	
		// Sanity checks
		if( null==inFormTree )
			throw new UIException( kExTag + "Null form passed in." );
	
	
		// Get compiled and cached XSLT
		Transformer formatter = null;
		try {
			formatter = getCompiledXSLTDoc();
		}
		catch( UIConfigException e1 ) {
			throw new UIException( kExTag
				+ "Got back error when getting XSLT formatter: " + e1
				);	
		}
		if( formatter == null )
		{
			throw new UIException( kExTag
				+ "Could not obtain XSLT formatting rules."
				);
		}
	
		debugMsg( kFName, "Formatting XML ..." );	
	
		// Prepare a hash with important values
		Hashtable xsltVars = new Hashtable();
		xsltVars.put( "title", getTitle(null) );
		// TODO: this won't work with templates if they change the relative thing...
		xsltVars.put( "submit_link", kClassName() + ".cgi" );
		xsltVars.put( "image_dir", DEFAULT_IMAGE_URL_PREFIX );

		xsltVars.put( "help_dir", DEFAULT_HELP_URL_PREFIX );
		// xsltVars.put( "help_dir", "http://north:9000" + DEFAULT_HELP_URL_PREFIX );

		// WARNING!!!!!
		// MUST ALSO be coordinated with XSLT sheet
		// nie.webui.xml_screens.generate_form.xslt
		// String tmpPwd = inRequestObject.getAccessPassword();  // Was getMainConfig().getAdminPwd()
		// tmpPwd = (null==tmpPwd) ? "" : tmpPwd;
		//// xsltVars.put( "password", tmpPwd );
		// xsltVars.put( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_FIELD, tmpPwd );
		//
		// Changed from Password to scrambled session id / key
		String tmpKey = inRequestObject.getAccessKey();  // Was getMainConfig().getAdminPwd()
		tmpKey = (null==tmpKey) ? "" : tmpKey;
		// WARNING!!!!!
		// MUST ALSO be coordinated with XSLT sheet
		// nie.webui.xml_screens.generate_form.xslt
		xsltVars.put( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD, tmpKey );
		// ^^^ currently set to "s"

		xsltVars.put( "operation", (inOperation!=null ? inOperation : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) );
	
		// Now Transform it!
		Document newJDoc = null;
		try {
			newJDoc = JDOMHelper.xsltElementToDoc(
				// getXML().getJdomElement(),
				inFormTree,
				formatter,
				xsltVars
				);
		}
		catch( JDOMHelperException e2 ) {
			throw new UIException( kExTag
				+ "Got back error from XSLT formatter: " + e2
				);	
		}
	
				
		if( newJDoc == null )
			throw new UIException( kExTag
				+ "Got back null document from XSLT formatter."
				);	
	
		Element outElem = newJDoc.getRootElement();
	
	
		// Markup with CSS if appropriate
		if( inDoFullPage ) {
			Element styleElem = prepareCSSElement();
			if( null!=styleElem ) {
				Element headElem = JDOMHelper.findElementByPath(outElem, "/html/head", false );
				if( null!=headElem )
					headElem.addContent( styleElem );
				else
					errorMsg( kFName, "Unable to add CSS to form; didn't find html/head." );
			}
		}
	
		debugMsg( kFName, "Done." );	
	
		return outElem;
	
	}

	public void setupReturnUrlInFormIfDesired(
			Element inForm, AuxIOInfo inRequest
		)
		throws UIException
	{
		final String kFName = "setupReturnUrlInFormIfDesired";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inRequest )
			throw new UIException( kExTag +
				"Null request passed in"
				);
		if( null==inForm )
			throw new UIException( kExTag +
				"Null form passed in"
				);

		if( hasFormField( UILink.RETURN_URL_CGI_FIELD ) ) {

			// Setup where we are supposed to return them to, either after
			// a success or cancel (an error brings them back here)
			String returnURL = inRequest.getScalarCGIFieldTrimOrNull( UILink.RETURN_URL_CGI_FIELD );
			// If this is the first time through, grab the referer
			if( null==returnURL )
				returnURL = inRequest.getReferer();
			if( null==returnURL ) {
				// throw new UIException( kExTag +
				errorMsg( kFName,
					"Unable to add get return URL."
					+ " Expected CGI field \"" + UILink.RETURN_URL_CGI_FIELD + "\""
					+ " or valid referer field."
					);
			}
			else
			{
				debugMsg( kFName, "Setting form " + UILink.RETURN_URL_CGI_FIELD + " = " + returnURL );
				setFormFieldValue( inForm, UILink.RETURN_URL_CGI_FIELD, returnURL );
			}

		}
	}



	private static void __sep__Blank_Form_and_Init__() {}
	/////////////////////////////////////////////



	public JDOMHelper _getXML()
		throws UIException
	{
		if( null == cXML )
		{
			final String kFName = "getXML";
			final String kExTag = kClassName() + '.' + kFName + ": ";

			String dataURI = AuxIOInfo.SYSTEM_RESOURCE_PREFIX
				+ kClassName() + ".xml";

			debugMsg( kFName, "Looking for xml " + dataURI );

			AuxIOInfo tmpAuxInfo = new AuxIOInfo();
			tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName() );

			try {
				cXML = new JDOMHelper(
					dataURI,
					getMainConfig().getConfigFileURI(), // optRelativeRoot
					0,
					tmpAuxInfo
					);
			}
			catch( Exception e )
			{
				errorMsg( kFName, "Stacktrace of JDOMHelper constructor:");
				e.printStackTrace( System.err );
				throw new UIException( kExTag + "z Error loading XML: " + e );
			}
		}
		return cXML;
	}

	public JDOMHelper getXML()
		throws UIException
	{
		final String kFName = "getXML(1)";
		if( null == cXML )
		{
			cXML = getXML( kClassName() );
		}
		return cXML;
	}


	public static JDOMHelper getXML( String inBaseName )
		throws UIException
	{
		final String kFName = "getXML(2)";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";

		String dataURI = AuxIOInfo.SYSTEM_RESOURCE_PREFIX
			+ SUBCLASS_RESOURCE_PREFIX
			+ inBaseName + ".xml";

		// debugMsg( kFName, "Looking for xml " + dataURI );

		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName( kStaticFullClassName );

		JDOMHelper outXML = null;
		try {
			outXML = new JDOMHelper(
				dataURI,
				null, // optRelativeRoot
				0,
				tmpAuxInfo
				);
		}
		catch( Exception e )
		{
			// errorMsg( kFName, "Stacktrace of JDOMHelper constructor:");
			// e.printStackTrace( System.err );
			throw new UIException( kExTag + "Error loading XML: " + e );
		}

		return outXML;
	}


	public void registerFormFields( JDOMHelper inForm )
		throws UIException
	{
		registerFormFields( inForm.getJdomElement() );
	}

	public void registerFormFields( Element inForm )
		throws UIException
	{
		final String kFName = "registerFormFields";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// String path = JDOMHelper.getPathToElement( inForm );
		// debugMsg( kFName, "path=\"" + path + "\"" );

		cFormFieldNameToNPath = new Hashtable();
		cFormFieldNameToFieldLabel = new Hashtable();

		try {
			// inForm.getDescendants();	// Only in Beta 10 rc1, which is currently broken, Feb 03
			// XPath xpath = new JDOMXPath( "//foo" );
			XPath xpath = XPath.newInstance("//field[@name]");
			List results = xpath.selectNodes(inForm);
			// For each field element we found
			for( Iterator it = results.iterator() ; it.hasNext() ; ) {
				Element currElem = (Element) it.next();

				// Get and check the name
				String fieldName = currElem.getAttributeValue( "name" );
				fieldName = NIEUtil.trimmedStringOrNull( fieldName );
				if( null==fieldName ) {
					if( JDOMHelper.getBooleanFromAttribute(
							currElem, SKIP_BLANK_NAME_ATTR, DEFAULT_BLANK_NAME_OK
							)
					) {
						continue; 
					}
					throw new UIException( kExTag +
						"Empty field name in element " + currElem
						+ " at " + JDOMHelper.getPathToElement( currElem )
						+ " = " + JDOMHelper.JDOMToString( currElem, false )
						);
				}
				if( cFormFieldNameToNPath.containsKey( fieldName ) )
					throw new UIException( kExTag +
						"Duplicate field name \"" + fieldName + "\" for element " + currElem
						);

				// Get and check it's path
				String currPath = JDOMHelper.getPathToElement( currElem );
				if(debug) debugMsg( kFName,
					"Element name + path="
					+ NIEUtil.NL + '\t'
					+ "\"" + fieldName + "\" @ \"" + currPath + "\""
					);
				if( null==currPath )
					throw new UIException( kExTag +
						"Empty npath for element " + currElem
						);
				// currPath += "/@name=" + fieldName;
				// ^^^ NO, we have to check this later manually

				// And store it!
				cFormFieldNameToNPath.put( fieldName, currPath );

				// If it has a label, store that as well
				String label = JDOMHelper.getStringFromAttributeTrimOrNull( currElem, "label" );
				if( null!=label )
					cFormFieldNameToFieldLabel.put( fieldName, label );
			}
		}
		// catch( JDOMException e ) {
		catch( Throwable e ) {
		// catch( Exception e ) {
			// e.printStackTrace( System.err );
			if( e instanceof Exception )
				stackTrace( kFName, (Exception)e, "Error traversing form during init and registering fields." );
			else
				errorMsg( kFName, "Error, showing stack trace." );
			errorMsg( kFName, "Back..." );
			throw new UIException( kExTag +
				"Error traversing form during init: " + e
				);
		}

	}

	// Get's a fresh CLONE of the form
	public Element getBlankForm()
		throws UIException
	{
		final String kFName = "getBlankForm";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		try {
			// We start with a blank form
			Element tmpBlankForm = getXML().getJdomElement();
			// We always make our own copy
			Element formData = (Element)tmpBlankForm.clone();
			// done
			return formData;
		}
		catch( Throwable t ) {
			throw new UIException( kExTag +
				"Error getting form: " + t
				);
		}
	}



	private static void __sep__Form_Fields__() {}
	/////////////////////////////////////////////

	public Element getFormField( Element inForm, String inFieldName )
		throws UIException
	{
		final String kFName = "getFormField";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inForm )
			throw new UIException( kExTag +
				"Null form passed in"
				);

		// Will throw exception if not found
		String formPath = getFormPathForField( inFieldName );

		// Do it
		// Element tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
		//	inForm, formPath, inFieldValue
		//	);
		Element outElem = JDOMHelper.findElementByPath( inForm, formPath, true );
		// Sanity check
		if( null==outElem )
			throw new UIException( kExTag +
				"Unable to find field on form."
				+ " Field \"" + inFieldName + "\""
				+ " XML path was supposed to be \"" + formPath + "\""
				);

		return outElem;
	}



	public Element getFormFieldOrNull( Element inForm, String inFieldName )
	{
		final String kFName = "getFormFieldOrNull";

		// Will give errors if bogus inputs
		String formPath = getFormPathForFieldOrNull( inFieldName );
		if( null==formPath )
			return null;

		// Do it
		// Element tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
		//	inForm, formPath, inFieldValue
		//	);
		Element outElem = JDOMHelper.findElementByPath( inForm, formPath, true );
		// Sanity check
		if( null==outElem )
			errorMsg( kFName,
				"Unable to find field on form."
				+ " Field \"" + inFieldName + "\""
				+ " XML path was supposed to be \"" + formPath + "\""
				);

		return outElem;
	}


	public String getFormPathForField( String inFieldName )
		throws UIException
	{
		final String kFName = "getFormPathForField";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inFieldName )
			throw new UIException( kExTag +
				"Null/empty field name passed in"
				);
		if( null==cFormFieldNameToNPath )
			throw new UIException( kExTag +
				"Field paths not initialized, for field \"" + inFieldName + "\""
				);
		if( ! cFormFieldNameToNPath.containsKey( inFieldName ) )
			throw new UIException( kExTag +
				"No such field \"" + inFieldName + "\" on form."
				);
		return (String) cFormFieldNameToNPath.get( inFieldName );
	}

	public String getFormPathForFieldOrNull( String inFieldName )
	{
		final String kFName = "getFormPathForFieldOrNull";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inFieldName ) {
			errorMsg( kFName,
				"Null/empty field name passed in"
				);
			return null;
		}
		if( null==cFormFieldNameToNPath ) {
			errorMsg( kFName,
				"Field paths not initialized, for field \"" + inFieldName + "\""
				);
			return null;
		}
		if( ! cFormFieldNameToNPath.containsKey( inFieldName ) )
			return null;
		return (String) cFormFieldNameToNPath.get( inFieldName );
	}


	public boolean hasFormField( String inFieldName )
		throws UIException
	{
		final String kFName = "hasFormField";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inFieldName )
			throw new UIException( kExTag +
				"Null/empty field name passed in"
				);
		if( null==cFormFieldNameToNPath )
			throw new UIException( kExTag +
				"Field paths not initialized, for field \"" + inFieldName + "\""
				);
		return cFormFieldNameToNPath.containsKey( inFieldName );
	}
	public String getFormLabelForFieldOrNull( String inFieldName )
		throws UIException
	{
		final String kFName = "getFormLabelForFieldOrNull";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inFieldName )
			throw new UIException( kExTag +
				"Null/empty field name passed in"
				);
		if( null==cFormFieldNameToNPath )
			throw new UIException( kExTag +
				"Field labels not initialized, for field \"" + inFieldName + "\""
				);
		if( cFormFieldNameToFieldLabel.containsKey( inFieldName ) )
			return (String) cFormFieldNameToFieldLabel.get( inFieldName );
		else
			return null;
	}


	public String getFormFieldValue( Element inForm, String inFieldName )
		throws UIException
	{
		final String kFName = "getFormFieldValue";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inForm )
			throw new UIException( kExTag +
				"Null form passed in"
				);

		// Will throw exception if not found
		String formPath = getFormPathForField( inFieldName );

		// Do it
		// Element tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
		//	inForm, formPath, inFieldValue
		//	);
		Element tmpElem = JDOMHelper.findElementByPath( inForm, formPath, true );
		// Sanity check
		if( null==tmpElem )
			throw new UIException( kExTag +
				"Unable to find field on form."
				+ " Field \"" + inFieldName + "\""
				+ " XML path was supposed to be \"" + formPath + "\""
				);
		String chkName = JDOMHelper.getStringFromAttributeTrimOrNull( tmpElem, "name" );
		if( null==chkName )
			throw new UIException( kExTag +
				"Unable to find correct field on form; no name attribute."
				+ " Field \"" + inFieldName + "\""
				+ " XML path was supposed to be \"" + formPath + "\""
				);
		if( ! chkName.equals(inFieldName) )
			throw new UIException( kExTag +
				"Unable to find field on form; name attribute does not match."
				+ " Tried to find field \"" + inFieldName + "\""
				+ " Found field named \"" + chkName + "\""
				+ " XML path was \"" + formPath + "\""
				);

		return JDOMHelper.getTextTrimOrNull( tmpElem );
	}


	public void setFormFieldValue( Element inForm, String inFieldName, String inFieldValue )
		throws UIException
	{
		final String kFName = "setFormFieldValue";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		inFieldName = NIEUtil.trimmedLowerStringOrNull( inFieldName );
		if( null==inForm )
			throw new UIException( kExTag +
				"Null form passed in"
				);

		// Will throw exception if not found
		String formPath = getFormPathForField( inFieldName );

		inFieldValue = NIEUtil.trimmedStringOrNull( inFieldValue );
		if( null==inFieldValue )
			throw new UIException( kExTag +
				"Null/empty field value passed in for field \"" + inFieldName + "\""
				);

		// Do it
		// Element tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
		//	inForm, formPath, inFieldValue
		//	);
		Element tmpElem = JDOMHelper.findElementByPath( inForm, formPath, true );
		// Sanity check
		if( null==tmpElem )
			throw new UIException( kExTag +
				"Unable to set field on form."
				+ " Field \"" + inFieldName + "\" = \"" + inFieldValue + "\""
				+ " XML path was \"" + formPath + "\""
				);
		String chkName = JDOMHelper.getStringFromAttributeTrimOrNull( tmpElem, "name" );
		if( null==chkName )
			throw new UIException( kExTag +
				"Unable to set field on form; no name attribute."
				+ " Field \"" + inFieldName + "\" = \"" + inFieldValue + "\""
				+ " XML path was \"" + formPath + "\""
				);
		if( ! chkName.equals(inFieldName) )
			throw new UIException( kExTag +
				"Unable to set field on form; name attribute does not match."
				+ " Tried to set field \"" + inFieldName + "\" = \"" + inFieldValue + "\""
				+ " Found field named \"" + chkName + "\""
				+ " XML path was \"" + formPath + "\""
				);

		// We still want to use this method because it handles clearning any
		// existing text and also puts long strings in cdata sections
		JDOMHelper.updateSimpleTextToExistingOrNewPath( tmpElem, null, inFieldValue );

	}


	public Transformer getCompiledXSLTDoc()
		throws UIConfigException
	{
		// if( ! mUseCache )
		if( null==cXsltTransformer )
		{
			final String kFName = "getCompiledXSLTDoc";
			try
			{
				String dataURI = AuxIOInfo.SYSTEM_RESOURCE_PREFIX
					// + kClassName() + ".xslt";
					+ XSLT_FORMGEN_SHEET + ".xslt";

				debugMsg( kFName, "Looking for xslt " + dataURI );

				AuxIOInfo tmpAuxInfo = new AuxIOInfo();
				tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName() );

				byte [] contents = NIEUtil.fetchURIContentsBin(
					dataURI,
					getMainConfig().getConfigFileURI(), // optRelativeRoot
					null, null,	// username, pwd
					tmpAuxInfo, false
					);
				if( contents.length < 1 )
					throw new UIConfigException(
						"Unable to load XSLT file"
						+ ", filename=\"" + dataURI + "\""
						+ ", base URI=\"" + getMainConfig().getConfigFileURI() + "\""
						);

				cXsltTransformer = JDOMHelper.compileXSLTString( contents );
			}
			catch(Exception e)
			{
				String msg = "Error getting / compiling XSLT: " + e;
				errorMsg( kFName, msg );
				throw new UIConfigException( msg );
			}
		}
		return cXsltTransformer;
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
			String value = (null!=key) ? inRequest.getScalarCGIField( key ) : null;
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

	public String getTitle( Hashtable inValuesHash )
	{
		if( ! fUseCache && null==cTitle )
		{
			final String kFName = "getTitle";
			cTitle = fMainElem.getTextByPathTrimOrNull(
				TITLE_PATH
				);
			if( null==cTitle )
				cTitle = "Report: " + getScreenName();

			if( null!=cTitle && cTitle.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;

		}

		/***
		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! _getShouldDoVariableSubstitutions()
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
		***/

		return cTitle;

	}

	static boolean _staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}

	// Holds screebs we have located,
	private static Hashtable fsCachedScreens;
	// Keeps track of whether we've checked for a particular
	// report or not, holds type Boolean
	private static HashSet fsSearchedForScreens;



	private JDOMHelper fMainElem;
	boolean fUseCache;

	List cSuggestedLinks;
	String cTitle;
	String cLinkText;
	String cSubtitle;

	JDOMHelper cXML;
	Transformer cXsltTransformer;

	private String cCgiCopyOverField;
	private String cCgiLookupField;
	private String cLookupReport;
	private String cIdCgiField;

	public static final String SUBCLASS_PREFIX = "nie.webui.xml_screens.";
	public static final String SUBCLASS_RESOURCE_PREFIX = "xml_screens/";

	// If we are asked to view, edit or delete, what form/cgi field to look at
	public static final String CGI_ID_FIELD_ATTR = "object_id_field";
	// What to use if more than one matching record
	public static final String LOOKUP_REPORT_ATTR = "multiple_match_selector_report";
	// For example, term
	public static final String CGI_LOOKUP_FIELD_ATTR = "cgi_edit_or_add_key_field";
	// For example, term
	public static final String CGI_NEW_RECORD_COPY_OVER_FIELD_ATTR = "cgi_add_record_carry_over_field";


	// How to do from XML to HTML
	public static final String XSLT_FORMGEN_SHEET = "generate_form";


	// the list of full XMLDefinedField objects
	List _cFieldList;
	// The list of fields stored by their ID
	Hashtable _cFieldHash;
	String _cSqlFieldsString;
	String _cSqlFromString;


	public static final String _CGI_FIELD_FOR_DEFAULT_VAL_ATTR = "cgi_add_record_carry_over_field";

	// Title and subtitle settings
	private static final String TITLE_PATH = "title";

	// static final String DEFAULT_IMAGE_URL_PREFIX = "/files/images/webui/";
	// ^^^ NO, now /file/, use variable
	static final String DEFAULT_IMAGE_URL_PREFIX =
		nie.sn.SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX
		+ "images/webui/"
		;
	// static final String DEFAULT_HELP_URL_PREFIX = "/files/help/webui/";
	// ^^^ NO, now /file/, use variable
	static final String DEFAULT_HELP_URL_PREFIX =
		nie.sn.SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX
		+ "help/webui/"
		;

	public static final String DEFAULT_URL_FORM_FIELD_PREFIX = "http://";


	private static final String _LINK_TEXT_PATH = "link_text";
	private static final String _SUBTITLE_PATH = "subtitle";

	static final String _STATS_LABEL_PATH = "stats_label";
	private static final String _RAW_SQL_PATH = "raw_sql";

	private static final String _SELECT_MODIFIER_PATH = "select_modifier";
	private static final String _RAW_FROM_PATH = "raw_from";

	private static final String _RAW_WHERE_JOIN_PATH = "raw_where_join";
	private static final String _RAW_WHERE_FILTER_PATH =
		"raw_where_filter";
	private static final String _LINKS_PATH =
		"suggested_links/" /* + ReportLink.MAIN_ELEM_NAME */;
	private static final String _RAW_ORDER_BY_PATH = "raw_sort";
	private static final String _RAW_GROUP_BY_PATH = "raw_grouping";


	// Field tag related strings
	private static final String FIELD_ELEM_PATH = "field";

	// private static final String FIELD_HEADING_ATTR = "heading";
	// private static final String FIELD_SQL_ALIAS_ATTR = "sql_alias";
	// private static final String FIELD_SHOULD_DISPLAY_ATTR = "show";

	static final String _x_SHOULD_DO_VAR_SUBST_ATTR =
		"variable_substitutions";

	// Desired starting and stopping row count
	public static final String _START_ROW_CGI_FIELD_NAME = "start_row";
	public static final String _DESIRED_ROW_COUNT_CGI_FIELD_NAME =
		"num_rows";

	public static final String _SORT_SPEC_CGI_FIELD_NAME = "sort";
	// public static final String FILTER_SPEC_CGI_FIELD_NAME = "filter";
	public static final String _FILTER_NAME_CGI_FIELD_NAME = "filter";
	public static final String _FILTER_PARAM_CGI_FIELD_NAME = "parm";

	// Having to do with variable hashes
	static final String _REQUEST_VARS_HASH_NAME = "cgi";
	static final String _SQL_ESC_SUFFIX = "_sqlesc";
	static final String _HTML_ESC_SUFFIX = "_htmlesc";



	// The default number of rows
	static final int _DEFAULT_DESIRED_ROW_COUNT = 25; // 25; // 25;

	// We show fields by default
	static final boolean _x_DEFAULT_SHOULD_DISPLAY_FIELD = true;

	// Where CSS style sheets come from
	public static final String DEFAULT_CSS_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "style_sheets/default_xml_defined_report.css"
		;

	static final boolean _DEFAULT_SHOULD_DO_VAR_SUBST = true;
	static final String _DEFAULT_STATS_LABEL = "Totals:";


	// Some of the class tags we use, others are hard coded if
	// used only once
	private static final String _ACTIVE_PAGING_CSS_CLASS =
		"nie_active_paging_link";
	private static final String _INACTIVE_PAGING_CSS_CLASS =
		"nie_inactive_paging_link";
	private static final String _STAT_NUMBER_CSS_CLASS =
		"nie_stat_number";
	public static final String _CONTAINER_CELL_CSS_CLASS =
		"nie_container_cell";

	public static final String SKIP_BLANK_NAME_ATTR = "skip_blank_name";
	public static final boolean DEFAULT_BLANK_NAME_OK = false;





}

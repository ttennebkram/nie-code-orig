/*
 * Created on Aug 31, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.webui.xml_screens;
import org.jdom.Element;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import nie.core.*;
import nie.webui.*;
import java.util.*;
import java.sql.*;

import nie.sn.SearchTuningApp;
import nie.sn.BaseMapRecord;
import nie.sn.DbMapRecord;
import nie.sn.SnURLRecord;
import nie.sr2.ReportConstants;
import nie.sr2.ReportLink;



/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
// public class QueryMaps extends BaseScreen
public class QueryMaps extends XMLDefinedScreen
{
	public final static String kStaticClassName = "QueryMaps";
	public String kClassName()
	{
		return kStaticClassName;
	}

	// NO, we still use the parent's full class for locating system resources
	// YES, we are putting our XML relative to here, so css must as well
	public String kFullClassName()
	{
		return "nie.webui.xml_screens." + kStaticClassName;
	}


	public QueryMaps(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper inScreenDefinitionElement,
		String inScreenName
		)
			throws UIConfigException, UIException
	{
		super( inMainConfig, inScreenDefinitionElement, inScreenName );
		// getXML();
		// getCompiledXSLTDoc();
	}



	public boolean augmentFormFromExistingData(
			Element ioBlankForm, String inTargetTerm,
			int optTargetID
		) throws UIException
	{
		// nothing to do
		return true;
	}

	// Fill in field from submitted CGI parameters
	// usually as part of REDISPLAYING a form that was just submitted,
	// typically because of an input error
	public void augmentFormFromCGIInput( Element ioBlankForm, AuxIOInfo inRequestObject )
		throws UIException
	{
		final String kFName = "augmentFormFromCGIInput";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);
		if( null==inRequestObject )
			throw new UIException( kExTag +
				"Null CGI request object passed in."
				);

		// Many of the values we pull may be null, if we're re-displaying
		// due to missing data, so don't worry about it

		// Get the main items
		String term = inRequestObject.getScalarCGIFieldTrimOrNull( TERM_SUBMIT_CGI_FIELD );
		String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_SUBMIT_CGI_FIELD );

		String mapID = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );

		String newTerms = inRequestObject.getScalarCGIFieldTrimOrNull( NEW_TERMS_SUBMIT_CGI_FIELD );

		// Update URL
		// String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
		// String title = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
		// String desc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
		// String urlIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_ID_CGI_FIELD );

		// Add these to the form
		genericAugmentFormFromFromMethodParms(
			ioBlankForm,
			mapID,
			term,
			url,
			newTerms
			);

	}






	void genericAugmentFormFromFromMethodParms(
			Element ioBlankForm,
			String mapID,
			String term,
			String url,
			String newTerms
		)
			throws UIException
	{
		final String kFName = "genericAugmentFormFromFromMethodParms";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);

		Element tmpElem = null;

		// Add this ID to the tree
		if( null!=mapID )
			setFormFieldValue( ioBlankForm, MAP_ID_CGI_FIELD, mapID );

		// Add the terms
		if( null!=term )
			setFormFieldValue( ioBlankForm, TERM_SUBMIT_CGI_FIELD, term );

		// Add the URL (this is checked by constructor)
		if( null!=url )
			setFormFieldValue( ioBlankForm, URL_SUBMIT_CGI_FIELD, url );

		// Add the seed terms, if any
		if( null!=newTerms )
			setFormFieldValue( ioBlankForm, NEW_TERMS_SUBMIT_CGI_FIELD, newTerms );

	}



	// Get a list of ONLY the Webmaster Suggests URL Objects
	SnURLRecord findPrinaryWmsURLInList( List inURLObjs )
	{
		SnURLRecord outURL = null;
		for( Iterator it = inURLObjs.iterator(); it.hasNext() ; )
		{
			SnURLRecord url = (SnURLRecord) it.next();
			if( url.getIsASuggestion() ) {
				outURL = url;
				break;
			}
		}
		return outURL;
	}


	public Element processDataSubmission( AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
		boolean inDoFullPage, String inMode
		)
			throws UIException
	{
		final String kFName = "processSubmission";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );
		if( null==ioResponseObject )
		   throw new UIException( kExTag + "Null response object passed in." );
		inMode = NIEUtil.trimmedLowerStringOrNull( inMode );
		if( null==inMode )
		   throw new UIException( kExTag + "Null mode passed in." );

		// Which operation is being attempted
		// get the mode, typically "forgen" or "submit"
		/***
		String operation = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
		operation = NIEUtil.trimmedLowerStringOrNull( operation );
		if( null==operation )
			throw new UIException( kExTag +
				"Null operation in CGI parameters."
				+ " Expected CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
				+ " to be one of " + UILink.kValidOperations
				);
		***/

		// Setup where we are supposed to return them to, either after
		// a success or cancel (an error brings them back here)
		String returnURL = inRequestObject.getScalarCGIFieldTrimOrNull( RETURN_URL_CGI_FIELD );
		// If this is the first time through, grab the referer
		if( null==returnURL )
			returnURL = inRequestObject.getReferer();
		if( null==returnURL ) {
			// throw new UIException( kExTag +
			errorMsg( kFName,
				"Unable to add get return URL."
				+ " Expected CGI field \"" + RETURN_URL_CGI_FIELD + "\""
				+ " or valid referer field."
				);
		}


		String button = inRequestObject.getScalarCGIFieldTrimOrNull( BUTTON_CGI_FIELD );
		button = NIEUtil.trimmedLowerStringOrNull( button );
//		if( null==button )
//			throw new UIException( kExTag +
//				"Null button CGI parameters."
//				+ " Expected CGI field \"" + BUTTON_CGI_FIELD + "\""
//				);
		// ^^^ per aopa, on this screen, we accept Enter as query

//		if( inMode.equals(UILink.UI_MODE_SUBMIT)
//						|| inMode.equals(UILink.UI_MODE_COMMIT)
//						|| inMode.equals(UILink.UI_MODE_CANCEL)

		// Check for cancel states
		if( null==button ) {
			inMode = UILink.UI_MODE_COMMIT;
		}
		else if( button.indexOf("cancel") >= 0
			|| button.indexOf("back") >= 0
			|| button.indexOf("return") >= 0
			|| button.indexOf('<') >= 0
			)
		{
			inMode = UILink.UI_MODE_CANCEL;
		}
		// Check for submit states
		else if( button.indexOf("ok") >= 0
			|| button.indexOf("create") >= 0
			|| button.indexOf("save") >= 0
			|| button.indexOf("next") >= 0
			|| button.indexOf('>') >= 0
			)
		{
			inMode = UILink.UI_MODE_COMMIT;
		}
		// We really shouldn't procede
		else {
			throw new UIException( kExTag +
				"Unknown button state in CGI parameters."
				+ " CGI field \"" + BUTTON_CGI_FIELD + "\""
				+ ", unknown state = \"" + button + "\""
				);
		}

		// If we will commit (run the query)
		if( inMode.equals( UILink.UI_MODE_COMMIT ) )
		{
			if(debug) debugMsg( kFName, "Form submitted, commit mode (which in this case means run-query mode)" );
			
			// Which operation is being attempted
			// get the mode, typically "forgen" or "submit"
			// Some other strings
			String term = inRequestObject.getScalarCGIFieldTrimOrNull( TERM_SUBMIT_CGI_FIELD );
			String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_SUBMIT_CGI_FIELD );
			String newTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull( NEW_TERMS_SUBMIT_CGI_FIELD );
			String mapIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );
			int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, false, true );

			// TODO: This code is very verbose, it really needs shortening
			try {

				// Lookup a TERM
				// Do we have a term
				if( null!=term ) {
					if(debug) debugMsg( kFName, "term='" + term + "'" );

					if( null!=url )
						throw new InvalidFormInputException(
							URL_SUBMIT_CGI_FIELD,
							"Cannot lookup by term and url at the same time; perhaps clear the URL field."	// String inMessage,
							// "URL",								// String inFieldLabel,
							// URL_XML_FORM_PATH,								// String inXMLFieldPath,
							// URL_SUBMIT_CGI_FIELD							// String inCGIFieldName
							);

					if( mapID > 0 )
						throw new InvalidFormInputException(
							MAP_ID_CGI_FIELD,
							"Cannot lookup by term and Map ID at the same time; perhaps clear the Map ID field."	// String inMessage,
							// "Map ID",								// String inFieldLabel,
							// MAP_ID_XML_FORM_PATH,								// String inXMLFieldPath,
							// MAP_ID_CGI_FIELD							// String inCGIFieldName
							);
	
					if( null!=newTermsStr )
						throw new InvalidFormInputException(
							NEW_TERMS_SUBMIT_CGI_FIELD,
							"Cannot lookup by term and create a new map with terms at the same time; perhaps clear the Seed Terms field.."	// String inMessage,
							// "Seed Terms",								// String inFieldLabel,
							// NEW_TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
							// NEW_TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
							);

					if(debug) debugMsg( kFName, "Generating report link..." );

					ReportLink rptLink = new ReportLink(
						getMainConfig(),				// nie.sn.SearchTuningConfig inMainConfig,
						ReportConstants.MAP_SELECTOR_REPORT_NAME,	// String inReportName,
						"query by term",				// String inLinkText,
						null,							// String optLinkTitle,
						null,							// String optCssClass,
						"term",		// String optParmName,
						null							// String optParmDefaultValue
					);

					// Make sure we have the correct "return" URL passed through
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

					// Generate the new link
					Element reportLinkElem = rptLink.generateRichLink(
						inRequestObject,	// AuxIOInfo inRequest,
						null,				// String optReportName,
						null,				// String optFilterName,
						false,				// boolean inIsMenuLink,
						term,		// String optNewParmValue
						null,		// Hashtable optVariables
						null		// String optNewLinkText
						);
					// Sanity
					if( null==reportLinkElem )
						throw new UIException( kExTag +
							"Got back null link for term \"" + term + "\""
							);
					String newURL = reportLinkElem.getAttributeValue( "href" );
					newURL = NIEUtil.trimmedStringOrNull( newURL );
					if( null==newURL )
						throw new UIException( kExTag +
							"Got back link with null/empty href for term \"" + term + "\""
							);
					// Now we recycle it and turn it into a redirect
					Element outElem = new Element( "redirect" );
					if(debug) debugMsg( kFName, "Sending them to URL ='" + newURL + "'" );
					outElem.addContent( newURL );
					return outElem;

				}	// End if we have a term
				// Lookup a URL
				// Do we have a URL?
				else if( null!=url ) {

					if(debug) debugMsg( kFName, "Null term, lookup mode, URL='" + url + "'" );
					
					if( mapID > 0 )
						throw new InvalidFormInputException(
							MAP_ID_CGI_FIELD,
							"Cannot lookup by URL and Map ID at the same time; perhaps clear the Map ID field."	// String inMessage,
							// "Map ID",								// String inFieldLabel,
							// MAP_ID_XML_FORM_PATH,								// String inXMLFieldPath,
							// MAP_ID_CGI_FIELD							// String inCGIFieldName
							);
	
					if( null!=newTermsStr )
						throw new InvalidFormInputException(
							NEW_TERMS_SUBMIT_CGI_FIELD,
							"Cannot lookup by URL and create a new map with terms at the same time; perhaps clear the Seed Terms field.."	// String inMessage,
							// "Seed Terms",								// String inFieldLabel,
							// NEW_TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
							// NEW_TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
							);

					ReportLink rptLink = new ReportLink(
						getMainConfig(),				// nie.sn.SearchTuningConfig inMainConfig,
						ReportConstants.MAP_SELECTOR_REPORT_NAME,	// String inReportName,
						"query by url",				// String inLinkText,
						null,							// String optLinkTitle,
						null,							// String optCssClass,
						"url",		// String optParmName,
						null							// String optParmDefaultValue
					);

					// Make sure we have the correct "return" URL passed through
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

					// Generate the new link
					Element reportLinkElem = rptLink.generateRichLink(
						inRequestObject,	// AuxIOInfo inRequest,
						null,				// String optReportName,
						null,				// String optFilterName,
						false,				// boolean inIsMenuLink,
						url,		// String optNewParmValue
						null,		// Hashtable optVariables
						null		// String optNewLinkText
						);
					// Sanity
					if( null==reportLinkElem )
						throw new UIException( kExTag +
							"Got back null link for url \"" + url + "\""
							);
					String newURL = reportLinkElem.getAttributeValue( "href" );
					newURL = NIEUtil.trimmedStringOrNull( newURL );
					if( null==newURL )
						throw new UIException( kExTag +
							"Got back link with null/empty href for url \"" + url + "\""
							);
					// Now we recycle it and turn it into a redirect
					Element outElem = new Element( "redirect" );
					outElem.addContent( newURL );
					return outElem;

				}	// End else if we have a url
				// Lookup a MAP ID
				// Do we have a term
				else if( mapID > 0 ) {

					if(debug) debugMsg( kFName, "Null term, lookup mode, map ID ='" + mapID + "'" );

					if( null!=newTermsStr )
						throw new InvalidFormInputException(
							NEW_TERMS_SUBMIT_CGI_FIELD,
							"Cannot lookup by Map ID and create a new map with terms at the same time; perhaps clear the Seed Terms field.."	// String inMessage,
							// "Seed Terms",								// String inFieldLabel,
							// NEW_TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
							// NEW_TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
							);

					nie.webui.UILink uiLink = new nie.webui.UILink(
							getMainConfig(),
							// "CreateMapForm",	// String Screen Name
							UILink.CLASSIC_CREATE_MAP_UI_SCREEN,	// String Screen Name
							UILink.MAP_ID_FORMGEN_CGI_FIELD,			// String inParmCGIName,
							"Set Directed Results for this map",	// String optLinkTitleText
							null
							);

					// Make sure we have the correct "return" URL passed through
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

					// The link text and mode (Create or Edit)
					String linkText = "create map";
					String mode = nie.webui.UILink.UI_OPERATION_EDIT;
					// Create the link
					Element linkElem = uiLink.generateLinkElement(
						inRequestObject,
						linkText,
						mode,
						""+mapID
						);

					// Sanity
					if( null==linkElem )
						throw new UIException( kExTag +
							"Got back null link for map ID \"" + mapID + "\""
							);
					String newURL = linkElem.getAttributeValue( "href" );
					newURL = NIEUtil.trimmedStringOrNull( newURL );
					if( null==newURL )
						throw new UIException( kExTag +
							"Got back link with null/empty href for map ID \"" + mapID + "\""
							);
					// Now we recycle it and turn it into a redirect
					Element outElem = new Element( "redirect" );
					outElem.addContent( newURL );
					return outElem;

				}	// End else if we have a map ID
				// Else do we have new terms to create a new map with???
				else if( null!=newTermsStr ) {
					debugMsg( kFName, "Have new term(s) \"" + newTermsStr + "\"" );
					nie.webui.UILink uiLink = new nie.webui.UILink(
							getMainConfig(),
							// "CreateMapForm",	// String Screen Name
							UILink.CLASSIC_CREATE_MAP_UI_SCREEN,	// String Screen Name
							UILink.TERM_FORMGEN_CGI_FIELD,			// String inParmCGIName,
							"Set Directed Results for this Term",	// String optLinkTitleText
							null
							);

					// Make sure we have the correct "return" URL passed through
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField(
							RETURN_URL_CGI_FIELD, returnURL
							);

					// The link text and mode (Create or Edit)
					String linkText = "create map";
					String mode = nie.webui.UILink.UI_OPERATION_ADD;
					// Create the link
					Element linkElem = uiLink.generateLinkElement(
						inRequestObject,
						linkText,
						mode,
						newTermsStr
						);

					// Sanity
					if( null==linkElem )
						throw new UIException( kExTag +
							"Got back null link for adding terms \"" + newTermsStr + "\""
							);
					String newURL = linkElem.getAttributeValue( "href" );
					newURL = NIEUtil.trimmedStringOrNull( newURL );
					if( null==newURL )
						throw new UIException( kExTag +
							"Got back link with null/empty href for adding terms \"" + newTermsStr + "\""
							);
					debugMsg( kFName, "Doing redirect to " + NIEUtil.NL + newURL );
					// Now we recycle it and turn it into a redirect
					Element outElem = new Element( "redirect" );
					outElem.addContent( newURL );
					return outElem;

				}	// End else if we have a map ID
				// Else we weren't given anything
				// so do a null search!
				// (was give them an error)
				else {
					debugMsg( kFName, "No fields entered, running report to show all maps." );
					/***
					throw new InvalidFormInputException(
						"No search criteria for locating maps.."	// String inMessage,
						+ " Please specify a term, a URL, or Map ID to lookup"
						+ "; or give one or more seed terms to create a new Map with."
						+ " (Or click Cancel)",
						"Term",								// String inFieldLabel,
						TERM_XML_FORM_PATH,								// String inXMLFieldPath,
						TERM_SUBMIT_CGI_FIELD							// String inCGIFieldName
						);
					***/
					ReportLink rptLink = new ReportLink(
						getMainConfig(),				// nie.sn.SearchTuningConfig inMainConfig,
						ReportConstants.MAP_SELECTOR_REPORT_NAME,	// String inReportName,
						"show all maps",				// String inLinkText,
						null,							// String optLinkTitle,
						null,							// String optCssClass,
						null,		// String optParmName,
						null							// String optParmDefaultValue
					);

					// Make sure we have the correct "return" URL passed through
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField( RETURN_URL_CGI_FIELD, returnURL );

					// Generate the new link
					Element reportLinkElem = rptLink.generateRichLink(
						inRequestObject,	// AuxIOInfo inRequest,
						null,				// String optReportName,
						null,				// String optFilterName,
						false,				// boolean inIsMenuLink,
						null,		// String optNewParmValue,
						null,		// Hashtable optVariables,
						null		// String optNewLinkText
						);
					// Sanity
					if( null==reportLinkElem )
						throw new UIException( kExTag +
							"Got back null link"
							);
					String newURL = reportLinkElem.getAttributeValue( "href" );
					newURL = NIEUtil.trimmedStringOrNull( newURL );
					if( null==newURL )
						throw new UIException( kExTag +
							"Got back link with null/empty href"
							);
					// Now we recycle it and turn it into a redirect
					Element outElem = new Element( "redirect" );
					outElem.addContent( newURL );
					return outElem;
				
				}
	
			}
			// We can handle specific isses with well defined mangled fields
			catch( InvalidFormInputException badFieldException ) {
				return redisplayBadFormSubmission(
					inRequestObject,
					ioResponseObject,
					// inMode,
					inDoFullPage,
					badFieldException
					);
			}
			catch( Throwable t ) {
				throw new UIException( kExTag +
					"Error updating database."
					+ " Error: " + t
					);		
			}



		}	// End if Commit

		// Get them back to a safe place
		if( null!=returnURL ) {
			Element outElem = new Element( "redirect" );
			outElem.addContent( returnURL );
			return outElem;
		}
		else
			return null;
	}

	public String getTitle( Hashtable inVars )
	{
		// return "Map Form";
		// return "Manage Directed Results";
		return "or Manage Directed Results";
	}
	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}
	public String getSubtitleOrNull( Hashtable inVars )
	{
		return null;
	}

	static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	static boolean staticTransactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kStaticClassName, inFromRoutine );
	}


	static boolean staticInfoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}


	static boolean staticDebugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kStaticClassName, inFromRoutine );
	}


	static boolean staticTraceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticShouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kStaticClassName, inFromRoutine );
	}
	static boolean staticWarningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	static boolean staticErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}















	boolean mUseCache;
	JDOMHelper cXML;
	Transformer cXsltTransformer;

	// The name of the submit button
	public static final String BUTTON_CGI_FIELD = "button";

	public static final String TERM_FORMGEN_CGI_FIELD = "term";
	public static final String TERM_SUBMIT_CGI_FIELD = "term";
	public static final String URL_SUBMIT_CGI_FIELD = "url";
	public static final String MAP_ID_CGI_FIELD = "map_id";
	public static final String NEW_TERMS_SUBMIT_CGI_FIELD = "seed_terms";

	// public static final String RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_FIELD = UILink.RETURN_URL_CGI_FIELD;
}

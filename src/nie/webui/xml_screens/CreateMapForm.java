/*
 * Created on Aug 31, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.webui.xml_screens;

import java.util.*;
import java.sql.*;
import java.net.*;

import org.jdom.Element;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.transform.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.jdom.xpath.*;

import sun.rmi.runtime.NewThreadAction;

import nie.core.*;
import nie.webui.*;

import nie.sn.SearchTuningApp;
import nie.sn.SearchEngineConfig;
import nie.sn.BaseMapRecord;
import nie.sn.DbMapRecord;
import nie.sn.SnURLRecord;
import nie.sr2.ReportConfigException;
import nie.sr2.ReportConstants;
import nie.sr2.ReportLink;



/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
// public class CreateMapForm extends BaseScreen
public class CreateMapForm extends XMLDefinedScreen
{
	public final static String kStaticClassName = "CreateMapForm";
	public String kClassName()
	{
		return kStaticClassName;
	}

	// NO, we still use the parent's full class for locating system resources
	// YES, we are putting our XML relative to here, so css must as well
	public String kFullClassName()
	{
		return "nie.webui.xml_screens.CreateMapForm";
	}


	public CreateMapForm(
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper optConfigElem,
		String inShortReportName
		)
			throws UIConfigException, UIException
	{
		super( inMainConfig, optConfigElem, inShortReportName );
		// getModeFieldNames();
	}

	// Override the base class method so we can fold
	// in the optional search options section
	public JDOMHelper getXML()
		throws UIException
	{
		final String kFName = "getXML";

		JDOMHelper outXML = super.getXML();
		if( ! mHaveAugmentedForm )
		{
			Element modesTree = getModedOptionsTreeOrNull();
			if( null!=modesTree ) {
				final int kInsertAfterSection = 2;
				List children = outXML.getJdomElement().getChildren();
				children.add( kInsertAfterSection, modesTree );
				// inFormData.addContent( modesTree );
			}
			mHaveAugmentedForm = true;
		}
		return outXML;
	}

	public void augmentWithMiscDefaults(
			Element inFormData, AuxIOInfo inRequest
		)
		throws UIException
	{
		setFormFieldValue( inFormData,
			URL_HREF_CGI_FIELD,
			XMLDefinedScreen.DEFAULT_URL_FORM_FIELD_PREFIX
			);

		/***
		Element modesTree = getModedOptionsTreeOrNull();
		if( null!=modesTree ) {
			final int kInsertAfterSection = 1;
			List children = inFormData.getChildren();
			children.add( kInsertAfterSection, modesTree );
			// inFormData.addContent( modesTree );
		}
		^^^ moved to constructor get xml since only done once
		***/
	}

	Element getModedOptionsTreeOrNull()
		throws UIException
	{
		final String kFName = "getModedOptionsTreeOrNull";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		Element outElem = getMainConfig().getSearchEngine().getFormOptionsTreeCloneOrNull();
		// We tweak the descriptions
		if( null!=outElem ) {
			outElem.setName( "section" );
			// outElem.setAttribute( "title", "Additional Matching Contraints" );
			outElem.setAttribute( "title", "Optional Sub-Site / Filter Constraints" );
			outElem.setAttribute( "help", "create_map_moded_matching.html" );

			try {
				String kPath = ".//option";
				XPath xpath = XPath.newInstance( kPath );
	
				// List results = xpath.selectNodes( inDoc );
				List results = xpath.selectNodes( outElem );
	
				debugMsg( kFName,
					"Looking for \"" + kPath + "\" from node \"" + outElem.getName() + "\""
					// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
					+ " Found " + results.size() + " options."
					);
	
				// For each option
				int optionCounter = 0;
				for( Iterator it = results.iterator() ; it.hasNext() ; ) {
					Object currObj = it.next();
					if( currObj instanceof org.jdom.Element ) {
						Element currElem = (Element) currObj;

						/***
						String value = JDOMHelper.getStringFromAttributeTrimOrNull(
							currElem, "value"
							);
						***/
						String desc = JDOMHelper.getStringFromAttributeTrimOrNull(
							currElem, "label"
							);

						/***
						if( null==value && null==desc ) {
							infoMsg( kFName, "Ignoring empty option" );
							continue;
						}
						***/

						optionCounter++;

						if( null!=desc ) {
							currElem.addContent( desc );
							currElem.removeAttribute( "label" );
						}
					}
					else {
						throw new UIException( kExTag +
							"Was not expecting node type (1): " + currObj.getClass().getName()
							);
					}
				}

				// For each field
				List fieldList = new Vector();
				kPath = ".//field";
				xpath = XPath.newInstance( kPath );
	
				// List results = xpath.selectNodes( inDoc );
				results = xpath.selectNodes( outElem );
	
				debugMsg( kFName,
					"Looking for \"" + kPath + "\" from node \"" + outElem.getName() + "\""
					// "Looking for \"" + inPath + "\" from node \"" + inDoc.getRootElement().getName() + "\""
					+ " Found " + results.size() + " fields."
					);
	
				// For each option
				int fieldCounter = 0;
				for( Iterator it = results.iterator() ; it.hasNext() ; ) {
					Object currObj = it.next();
					if( currObj instanceof org.jdom.Element ) {
						fieldCounter++;
						Element currElem = (Element) currObj;
						// currElem.setAttribute( "type", "select" );
						currElem.setAttribute( "type", "checkbox_set" );
					}
					else {
						throw new UIException( kExTag +
							"Was not expecting node type (2): " + currObj.getClass().getName()
							);
					}
				}

			}
			catch( Exception e ) {
				throw new UIException( kExTag +
					"Error normalizing form moded options: " + e
					);
			}
		}

		// statusMsg( kFName, "Mode tree=" + NIEUtil.NL
		//	+ JDOMHelper.JDOMToString( outElem, true )
		//	);
		return outElem;
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

		// Get the main ID
		String mapID = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );

		// Get the new terms
		String terms = inRequestObject.getScalarCGIFieldTrimOrNull( TERMS_SUBMIT_CGI_FIELD );

		// Field modes
		// We want to present a hash of valid form option field names and the values
		// that were found in the CGI request
		Hashtable fieldModes = null;
		if( null!=mapID ) {
			Collection critFieldNames = getMainConfig().getSearchEngine().getSearchFormOptionFieldNames();
			if( null!=critFieldNames && ! critFieldNames.isEmpty() ) {
				fieldModes = new Hashtable();
				for( Iterator fit=critFieldNames.iterator(); fit.hasNext() ; ) {
					String critFieldName = (String) fit.next();
					// List cgiValues = inRequestObject.getMultivalueCGIField( critFieldName );
					// We really need to know even about blank/empty values
					List cgiValues = inRequestObject.getMultivalueCGIField_UnnormalizedValues( critFieldName );
					if( null!=cgiValues && ! cgiValues.isEmpty() )
						fieldModes.put( critFieldName, cgiValues );
				}
			}
		}

		// Get the alt terms while we're here (which there may be none)
		String altTerms = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_SUBMIT_CGI_FIELD );
		String altTermsHeadingText = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );
		String altTermsHeadingColor = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );

		// Update URL
		// String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
		// String title = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
		// String desc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
		// String urlIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_ID_CGI_FIELD );

		// Add these to the form
		genericAugmentFormFromMethodParms(
			ioBlankForm,
			mapID,
			terms,
			fieldModes,
			altTerms,
			altTermsHeadingText,
			altTermsHeadingColor,
			null, // urlIDStr,
			null, // url,
			null, // title,
			null  // desc
			);

		augmentFromCGIUrls( ioBlankForm, inRequestObject );
	}


	// This form can have more than 1 URL/title/desc
	// this fills in all the boxes from multiple urls
	private void augmentFromCGIUrls( Element ioBlankForm, AuxIOInfo inRequestObject )
		throws UIException
	{
		final String kFName = "augmentFromCGIUrls";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==ioBlankForm )
		   throw new UIException( kExTag + "Null blank form passed in." );
		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );

		Element sectionTemplate = JDOMHelper.findElementByPath(
			ioBlankForm, URL_SECTION_XML_FORM_PATH, true
			);
		if( null==sectionTemplate )
			throw new UIException( kExTag +
				"Unable to find section template \"" + URL_SECTION_XML_FORM_PATH + "\""
				);
		try {
			sectionTemplate = (Element)sectionTemplate.clone();
		}
		catch( Exception e ) {
			throw new UIException( kExTag +
				"Unable to clone section template \"" + URL_SECTION_XML_FORM_PATH + "\""
				+ " Exception: " + e
				);
		}
		if( null==sectionTemplate )
			throw new UIException( kExTag +
				"Null clone of section template \"" + URL_SECTION_XML_FORM_PATH + "\""
				);

		String indexStr, cgiName, xmlPath;
		// For each possible URL
		final int indexStringLength = (""+MAX_URLS_PER_MAP).length();
		for( int i=1; i<=MAX_URLS_PER_MAP ; i++ ) {

			indexStr = NIEUtil.leftPadInt( i, indexStringLength );

			// URL
			cgiName = URL_HREF_NTH_CGI_FIELD;
			String urlCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String url = inRequestObject.getScalarCGIFieldTrimOrNull( urlCgi );

			debugMsg( kFName, "urlCgi=" + urlCgi );

			// TITLE
			cgiName = URL_TITLE_NTH_CGI_FIELD;
			String titleCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String title = inRequestObject.getScalarCGIFieldTrimOrNull( titleCgi );

			// Description
			cgiName = URL_DESC_NTH_CGI_FIELD;
			String descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String desc = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );

			// ID
			cgiName = URL_ID_NTH_CGI_FIELD;
			String idCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String idStr = inRequestObject.getScalarCGIFieldTrimOrNull( idCgi );
			int urlID = NIEUtil.stringToIntOrDefaultValue(
				idStr, -1, false, true
				);	

			debugMsg( kFName,
				"URL # " + i + " id/href/title/des=" + urlID + '/' + url + '/' + title + '/' + desc
				);

			// If all blank, then done
			if( urlID < 1 && null==url && null==title && null==desc )
				continue;
				// break;


			augmentWithNthURL(
				ioBlankForm,
				i,
				urlID,
				url,
				title,
				desc,
				sectionTemplate,
				i,
				true
				);

		}	// End for each possible field

	}




	// returns true if it correctly found the map to edit
	// If it finds more than one then it returns false, indicating
	// none more multiple were found
	public boolean augmentFormFromExistingData(
			Element ioBlankForm,
			String inTargetTerm,
			int optTargetID
		)
		throws UIException
	{
		final String kFName = "augmentFormFromExistingData";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				+ " Target term was \"" + inTargetTerm + "\""
				);

		// Tweak the dropdown lists to reflect the current defaults
		augmentFormFromSystemDefaults( ioBlankForm );

		// Figure out which map to edit
		int targetID = -1;
		// If they gave us one, use it!
		if( optTargetID > 0 ) {
			targetID = optTargetID;
		}
		// Else no specific ID, hopefully they have us a term
		else {
			// We need to figure out the ID for the map we want to edit
			// but what they will have passed us is a term
			if( null==inTargetTerm || inTargetTerm.equals(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
				throw new UIException( kExTag +
					"Null term in CGI parameters."
					+ " Reminder: make sure XML form sets cgi name."
					// + " Expected CGI field \"" + UILink.TERM_FORMGEN_CGI_FIELD + "\""
					// + " to specify term that is to be edited."
					);

			// Now get the ID of the map we are to edit
			/***
			targetID = DbMapRecord.static_getFirstMapIDForTerm( getDBConfig(), inTargetTerm );
			if( targetID < 1 )
				throw new UIException( kExTag +
					"Invalid map ID for existing term."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);
			***/

			List mapIDs = DbMapRecord.static_getMapIDsForTerm( getDBConfig(), inTargetTerm );
			if( null==mapIDs || mapIDs.isEmpty() )
				throw new UIException( kExTag +
					"No matching Maps for this term."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);

			// This is not an error condition, just indicates
			// that there's more than one match
			if( mapIDs.size() > 1 ) {
				debugMsg( kFName,
					"Multiple matching Maps for this term, returnfing false."
					+ " Should call with a specific Map ID."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map IDs = " + mapIDs + "."
					);
				return false;
			}

			Integer mapIDObj = (Integer) mapIDs.iterator().next();
			targetID = mapIDObj.intValue();

			if( targetID < 1 )
				throw new UIException( kExTag +
					"Invalid map ID for existing term."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);	

		}

		// String mapIDStr = ""+targetID;

		// We get ALL the terms related to this map ID
		// which may just give us back the one we started with
		List terms = DbMapRecord.static_getTermsForMapID( getDBConfig(), targetID, false );
		if( null==terms || terms.size() < 1 )
			throw new UIException( kExTag +
				"Didn't get map terms."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		// Get back a string list of all the terms for this map
		// String termsStr = NIEUtil.listOfStringsToSingleString( terms );
		String termsStr = NIEUtil.listOfStringsToSingleString2(
				terms,		// List inList,
				true,		// boolean inNullTrimValues,
				false,		// boolean inAddQuotes,
				true,		// boolean inReturnTrueNull,
				null,		// String optSeparator,
				true		// boolean inDoWarnings
				);
		if( null==termsStr )
			throw new UIException( kExTag +
				"Got back null composite term list."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		Hashtable fieldModesHash = DbMapRecord.static_getMatchModesForMapID(
			getDBConfig(), getSearchEngineConfig(), targetID
			);
		debugMsg( kFName, "db mode fields = "
			+ ( null==fieldModesHash ? "(none)" : ""+fieldModesHash.keySet() )
			);


		// We get the related/alternative terms for this map ID
		// which may be none
		String altTermsStr = null;
		List altTerms = DbMapRecord.static_getAltTermsForMapID( getDBConfig(), targetID, false );
		// If there are some alt terms to add
		if( null!=altTerms && altTerms.size() > 0 ) {
			// Get back a string list of all the terms for this map
			// String termsStr = NIEUtil.listOfStringsToSingleString( terms );
			altTermsStr = NIEUtil.listOfStringsToSingleString2(
					altTerms,		// List inList,
					true,		// boolean inNullTrimValues,
					false,		// boolean inAddQuotes,
					true,		// boolean inReturnTrueNull,
					null,		// String optSeparator,
					true		// boolean inDoWarnings
					);
			if( null==altTermsStr )
				throw new UIException( kExTag +
					"Got back null composite term list."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);
		}

		// Additional properties for the alt text
		String altTermHeadingText = DbMapRecord.genericGetMetaPropertyOrNull(
			BaseMapRecord.ALT_SLOGAN_PATH,
			targetID,
			"text",
			getDBConfig(), null
			);
		String altTermHeadingColor = DbMapRecord.genericGetMetaPropertyOrNull(
			BaseMapRecord.ALT_SLOGAN_PATH,
			targetID,
			"color",
			getDBConfig(), null
			);

		debugMsg( kFName, "alt text=\"" + altTermHeadingText + "\"" );
		debugMsg( kFName, "alt text color=\"" + altTermHeadingColor + "\"" );

		/*** moved to late in the code to support multiple URLs
		// We get URLs for this map ID
		// which may just give us back the one we started with
		String urlIDStr = null;
		String href = null;
		String title = null;
		String desc = null;
		List urls = DbMapRecord.static_getURLObjects( getMainConfig(), targetID );
		if( null==urls || urls.size() < 1 ) {
			// throw new UIException( kExTag +
			warningMsg( kFName,
				"Didn't get any urls."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);
		}
		// Else we did get a valid ID
		else {
			// We only use the first one
			SnURLRecord primaryURL = findPrinaryWmsURLInList( urls );
			if( null==primaryURL )
				throw new UIException( kExTag +
					"No primary WMS URL for this map."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);
	
			// Add their data to the form
	
			// Add the URL (this is checked by constructor)
			href = primaryURL.getURL();
	
			// Add the title (this is checked by constructor)
			title = primaryURL.getTitle();
	
			// Add the Description (which may be null)
			desc = primaryURL.getDescription();
	
			// Add the ID
			int urlID = primaryURL.getID();
			if( urlID < 1 )
				throw new UIException( kExTag + "Invalid URL ID for URL \"" + href + "\"" );
			urlIDStr = ""+urlID;
		}
		***/


		// TODO: Will add in more fields

		// Fill in the form
		genericAugmentFormFromMethodParms(
			ioBlankForm,
			""+targetID,
			termsStr,
			fieldModesHash,
			altTermsStr,
			altTermHeadingText,
			altTermHeadingColor,
			null, // urlIDStr,
			null, // href,
			null, // title,
			null  // desc
			);




		// We get URLs for this map ID
		// which may just give us back the one we started with
		// List urls = DbMapRecord.static_getURLObjects( getMainConfig(), targetID );

		DbMapRecord lMap;
		try {
			lMap = DbMapRecord.static_getDbMapRecordFromDatabase(
				getMainConfig(), targetID
				);
		}
		catch(Exception e) {
			throw new UIException( kExTag +
				"Error getting map for \"" + targetID + "\""
				+ " Error: " + e
				);
		}

		List urls = lMap.getWmsURLObjects();
		if( null==urls || urls.size() < 1 ) {
			// throw new UIException( kExTag +
			warningMsg( kFName,
				"Didn't get any Webmaster Suggests urls."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);
		}
		// Else we did get a valid ID
		else {

			Element sectionTemplate = JDOMHelper.findElementByPath(
				ioBlankForm, URL_SECTION_XML_FORM_PATH, true
				);
			if( null==sectionTemplate )
				throw new UIException( kExTag +
					"Unable to find section template \"" + URL_SECTION_XML_FORM_PATH + "\""
					);
			try {
				sectionTemplate = (Element)sectionTemplate.clone();
			}
			catch( Exception e ) {
				throw new UIException( kExTag +
					"Unable to clone section template \"" + URL_SECTION_XML_FORM_PATH + "\""
					+ " Exception: " + e
					);
			}
			if( null==sectionTemplate )
				throw new UIException( kExTag +
					"Null clone of section template \"" + URL_SECTION_XML_FORM_PATH + "\""
					);

			int howMany = urls.size();
			// For each URL
			// for( Iterator it = urls.iterator() ; it.hasNext() ; ) {
			//	SnURLRecord url = (SnURLRecord) it.next();
			for( int i=0; i<howMany; i++ ) {
				SnURLRecord url = (SnURLRecord) urls.get(i);

				// Add the URL (this is checked by constructor)
				String href = url.getURL();
		
				// Add the title (this is checked by constructor)
				String title = url.getTitle();
		
				// Add the Description (which may be null)
				String desc = url.getDescription();
		
				// Add the ID
				int urlID = url.getID();
				if( urlID < 1 )
					throw new UIException( kExTag + "Invalid URL ID for URL \"" + href + "\"" );

				// Actually add it!
				augmentWithNthURL(
					ioBlankForm,
					(i+1),
					urlID,
					href,
					title,
					desc,
					sectionTemplate,
					howMany,
					false
					);

			}	// End for each URL
		}

		return true;	
	}

	public void augmentFormFromSystemDefaults( Element ioBlankForm )
		throws UIException
	{
		final String kFName = "augmentFormFromSystemDefaults";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// The slogan
		// ALT_TERMS_HEADING_TEXT_DEFAULT_VALUE_XML_FORM_PATH
		String headingText = BaseMapRecord.getAltSloganDefaultPropertyOrNull( "text" );
		if( null!=headingText ) {
			/***
			Element tmpElem = JDOMHelper.findElementByPath(
				ioBlankForm, ALT_TERMS_HEADING_TEXT_DEFAULT_VALUE_XML_FORM_PATH, true
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to set default value for alt term slogan text on edit form."
					// + " XML path was \"" + ALT_TERMS_HEADING_TEXT_DEFAULT_VALUE_XML_FORM_PATH + "\""
					);
			***/
			// Will throw exception if not found
			Element tmpElem = getFormField( ioBlankForm, ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );

			// String existingText = tmpElem.getTextNormalize();
			final String optPath = "option[1]";
			String existingText = JDOMHelper.getTextByPathSuperTrimOrNull( tmpElem, optPath );

			// statusMsg( kFName, "existingText=" + existingText );
			String newText =
				( null!=existingText ? existingText + ' ' : "" )
				// + "currrent default \""
				// + "\""
				+ headingText
				// + "\""
				;
			// tmpElem.setText( newText );
			JDOMHelper.updateSimpleTextToExistingOrNewPath(tmpElem, optPath, newText );
		}

		// The color of the slogan
		// ALT_TERMS_HEADING_TEXT_DEFAULT_VALUE_XML_FORM_PATH
		String headingColor = BaseMapRecord.getAltSloganDefaultPropertyOrNull( "color" );
		if( null!=headingText ) {
			/***
			Element tmpElem = JDOMHelper.findElementByPath(
				ioBlankForm, ALT_TERMS_HEADING_COLOR_DEFAULT_VALUE_XML_FORM_PATH, true
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to set default value for alt term slogan color on edit form."
					+ " XML path was \"" + ALT_TERMS_HEADING_COLOR_DEFAULT_VALUE_XML_FORM_PATH + "\""
					);
			***/
			Element tmpElem = getFormField( ioBlankForm, ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );

			// String existingText = tmpElem.getTextNormalize();
			final String optPath = "option[1]";
			String existingText = JDOMHelper.getTextByPathSuperTrimOrNull( tmpElem, optPath );

			if( null!=headingColor ) {
				// Translate known color values into friendlier display names
				if( kColorValuesToNames.containsKey(headingColor.toLowerCase()) ) {
					headingColor = (String) kColorValuesToNames.get(headingColor.toLowerCase());
					debugMsg( kFName, "Color translated to word \"" + headingColor + "\"");
				}
				else {
					debugMsg( kFName,
						"Color not translated"
						+ " color code =\"" + headingColor.toLowerCase() + "\""
						+ " keys=" + kColorValuesToNames.keySet()
						);
				}
			}

			String newText =
				( null!=existingText ? existingText + ' ' : "" )
				+ headingColor;

			// tmpElem.setText( newText );
			JDOMHelper.updateSimpleTextToExistingOrNewPath(tmpElem, optPath, newText );
		}
		
	}


	private void augmentWithNthURL(
			Element ioBlankForm,
			int inPosition,
			int inUrlID,
			String inURL,
			String inTitle,
			String inDescription,
			Element optSectionTemplate,
			int optMaxPositionCount,
			boolean inAreBlanksExpected
		)
			throws UIException
	{
		final String kFName = "augmentWithNthURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);
		if( inPosition < 1 || inPosition > MAX_URLS_PER_MAP )
			throw new UIException( kExTag +
				"Invalid URL position " + inPosition
				+ " Must be between 1 and " + MAX_URLS_PER_MAP + ", inclusive."
				);
		if( null==inURL && ! inAreBlanksExpected )
			throw new UIException( kExTag +
				"Null URL href passed in."
				);

		int baseIndex = ( (inPosition-1) * NUM_URL_FORM_FIELDS ) + 1;

		final int indexStringLength = (""+MAX_URLS_PER_MAP).length();
		int fieldIndex;
		String cgiName, xmlPath, indexStr;
		Element targetElem, templateElem;

		// Even if the value is null, we still need to create the field

		// The URL
		//////////////
		fieldIndex = baseIndex + URL_HREF_NTH_OFFSET;
		cgiName = URL_HREF_NTH_CGI_FIELD;
		xmlPath = URL_HREF_NTH_XML_FORM_PATH;
		// Do the substitutions
		indexStr = NIEUtil.leftPadInt( inPosition, indexStringLength );
		cgiName = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
		debugMsg( kFName,
			"inPosition=" + inPosition + ", baseIndex=" + baseIndex
			+ ", fieldIndex=" + fieldIndex + ", cgi=" + cgiName + ", xml=" + xmlPath
			);
		// Locate the Element
		targetElem = JDOMHelper.findOrCreateElementByPath(
			ioBlankForm, xmlPath, false
			);
		if( null==targetElem )
			throw new UIException( kExTag +
				"Unable to augment form with node (1): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( (URL_HREF_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_HREF_NTH_OFFSET+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (1)"
					+ " Template = "
					+ JDOMHelper.JDOMToString( optSectionTemplate, true )
					);
			JDOMHelper.copyAttributes( templateElem, targetElem );
			// Now we need to tweak a couple of them
			targetElem.setAttribute( "name", cgiName );
			// Tweak the label, if needed
			if( optMaxPositionCount > 1 ) {
				String label = targetElem.getAttributeValue( "label" );
				if( null!=label ) {
					label += " # " + inPosition;
					targetElem.setAttribute( "label", label );
				}
			}
		}
		if( null!=inURL )
			targetElem.setText( inURL );

		// The TITLE
		//////////////
		fieldIndex = baseIndex + URL_TITLE_NTH_OFFSET;
		cgiName = URL_TITLE_NTH_CGI_FIELD;
		xmlPath = URL_TITLE_NTH_XML_FORM_PATH;
		// Do the substitutions
		indexStr = NIEUtil.leftPadInt( inPosition, indexStringLength );
		cgiName = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
		// Locate the Element
		targetElem = JDOMHelper.findOrCreateElementByPath(
			ioBlankForm, xmlPath, false
			);
		if( null==targetElem )
			throw new UIException( kExTag +
				"Unable to augment form with node (2): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_TITLE_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_TITLE_NTH_OFFSET+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (2)"
					+ " Template = "
					+ JDOMHelper.JDOMToString( optSectionTemplate, true )
					);
			JDOMHelper.copyAttributes( templateElem, targetElem );
			// Now we need to tweak a couple of them
			targetElem.setAttribute( "name", cgiName );
			// Tweak the label, if needed
			if( optMaxPositionCount > 1 ) {
				String label = targetElem.getAttributeValue( "label" );
				if( null!=label ) {
					label += " # " + inPosition;
					targetElem.setAttribute( "label", label );
				}
			}
		}
		if( null!=inTitle )
			targetElem.setText( inTitle );

		// Description
		//////////////////
		fieldIndex = baseIndex + URL_DESC_NTH_OFFSET;
		cgiName = URL_DESC_NTH_CGI_FIELD;
		xmlPath = URL_DESC_NTH_XML_FORM_PATH;
		// Do the substitutions
		indexStr = NIEUtil.leftPadInt( inPosition, indexStringLength );
		cgiName = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
		// Locate the Element
		targetElem = JDOMHelper.findOrCreateElementByPath(
			ioBlankForm, xmlPath, false
			);
		if( null==targetElem )
			throw new UIException( kExTag +
				"Unable to augment form with node (3): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_DESC_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_DESC_NTH_OFFSET+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (3)"
					+ " Template = "
					+ JDOMHelper.JDOMToString( optSectionTemplate, true )
					);
			JDOMHelper.copyAttributes( templateElem, targetElem );
			// Now we need to tweak a couple of them
			targetElem.setAttribute( "name", cgiName );
			// Tweak the label, if needed
			if( optMaxPositionCount > 1 ) {
				String label = targetElem.getAttributeValue( "label" );
				if( null!=label ) {
					label += " # " + inPosition;
					targetElem.setAttribute( "label", label );
				}
			}
		}
		if( null!=inDescription )
			targetElem.setText( inDescription );

		// ID
		//////////////////
		fieldIndex = baseIndex + URL_ID_NTH_OFFSET;
		cgiName = URL_ID_NTH_CGI_FIELD;
		xmlPath = URL_ID_NTH_XML_FORM_PATH;
		// Do the substitutions
		indexStr = NIEUtil.leftPadInt( inPosition, indexStringLength );
		cgiName = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
		xmlPath = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
		// Locate the Element
		targetElem = JDOMHelper.findOrCreateElementByPath(
			ioBlankForm, xmlPath, false
			);
		if( null==targetElem )
			throw new UIException( kExTag +
				"Unable to augment form with node (4): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_ID_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_ID_NTH_OFFSET+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (4)"
					+ " Template = "
					+ JDOMHelper.JDOMToString( optSectionTemplate, true )
					);
			JDOMHelper.copyAttributes( templateElem, targetElem );
			// Now we need to tweak a couple of them
			targetElem.setAttribute( "name", cgiName );
			// Tweak the label, if needed
			// normally wouldn't for a hidden field
			if( optMaxPositionCount > 1 ) {
				String label = targetElem.getAttributeValue( "label" );
				if( null!=label ) {
					label += " " + inPosition;
					targetElem.setAttribute( "label", label );
				}
			}
		}

		if( inUrlID >= 1 )
			targetElem.setText( ""+inUrlID );



	}


	private void genericAugmentFormFromMethodParms(
			Element ioBlankForm,
			String mapID,
			String terms,
			Hashtable inFieldMatchModes,
			String altTerms,
			String altTermsHeadingText,
			String altTermsHeadingColor,
			String urlID,
			String url,
			String title,
			String description
		)
			throws UIException
	{
		final String kFName = "genericAugmentFormFromMethodParms";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				);

		Element tmpElem = null;

		// Add this ID to the tree
		if( null!=mapID ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, MAP_ID_XML_FORM_PATH, mapID
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add map ID to edit form."
					+ " URL ID was \"" + mapID + "\"."
					+ " XML path was \"" + MAP_ID_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, MAP_ID_CGI_FIELD, mapID );
		}

		// Add the terms
		if( null!=terms ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, TERMS_XML_FORM_PATH, terms
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add term(s) to edit form."
					+ " Term(s) was/were \"" + terms + "\"."
					+ " XML path was \"" + TERMS_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, TERMS_SUBMIT_CGI_FIELD, terms );
		}

		// Add in the values from the hashtable
		if( null!=inFieldMatchModes && ! inFieldMatchModes.isEmpty() ) {
			// For each field
			for( Iterator fit = inFieldMatchModes.keySet().iterator() ; fit.hasNext() ; ) {
				String fieldName = (String) fit.next();

				// Get the values that are to be selected
				List fieldValuesMaster = (List) inFieldMatchModes.get( fieldName );
				List fieldValuesCopy = new Vector();
				fieldValuesCopy.addAll( fieldValuesMaster );
				List processedValues = new Vector();

				// Get the actual element for this field on THIS COPY of the form
				// Will throw exception if not found
				// Due to config changes they might have old values from modes
				// no longer defined, give a nasty error but don't crash
				Element fieldElem = null;
				try {
					debugMsg( kFName, "getting form field " + fieldName );
					fieldElem = getFormField( ioBlankForm, fieldName );
				}
				catch( UIException e ) {
					errorMsg( kFName,
						"Error matching database match field to form/defined match fields."
						+ " Field=\"" + fieldName + "\""
						+ ", value(s)=" + fieldValuesMaster
						+ " Error was \"" + e + "\""
						+ " This field will not be displayed on the form and will not be editable."
						);
					continue;
				}

				// Get the Element options
				List optionElems = fieldElem.getChildren( SearchEngineConfig.OPTION_ELEM );

				// If there are any options
				if( null!=optionElems && ! optionElems.isEmpty() ) {
					// For each option element
					for( Iterator oit = optionElems.iterator() ; oit.hasNext() ; ) {
						Element optionElem = (Element) oit.next();
						String value = null;
						// Does it match the node value
						String text1 = optionElem.getAttributeValue( SearchEngineConfig.VALUE_ATTR );
						boolean matches1 =
							( null!=text1 && fieldValuesCopy.contains( text1 ) )
							|| ( null==text1 && fieldValuesCopy.contains( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
							;
						if( matches1 ) 
							value = (null!=text1) ? text1 : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
						// Does it match the node text
						String text2 = optionElem.getText();
						boolean matches2 =
							// Only considered if value test didn't match
							! matches1
							// And the value was null
							&& ( null==text1 || text1.equals( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
							// And only then did the node text match in the list
							&& (	( null!=text2 && fieldValuesCopy.contains( text2 ) )
									|| ( null==text1 && fieldValuesCopy.contains( ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
								);
						if( matches2 ) 
							value = (null!=text2) ? text2 : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
						// If we have a match!
						if( matches1 || matches2 ) {
							processedValues.add( value );
							optionElem.setAttribute( SearchEngineConfig.SELECTED_ATTR, "TRUE" );
						}
					}	// end for each option

					// We double check for options that were not on the form
					fieldValuesCopy.removeAll( processedValues );
					if( ! fieldValuesCopy.isEmpty() ) {
						warningMsg( kFName,
							"Unhandled/orphan field-match/moded value(s):"
							+ " field name = \"" + fieldName + "\""
							+ " with value(s) = " + fieldValuesCopy
							+ " Adding to form as auxillary field(s)."
							+ " You can fix this be defining the additional values in '" + SearchEngineConfig.SEARCH_FORM_OPTIONS_ROOT_PATH + "' of the search engine config for this field."
							);
						List existingChildren = fieldElem.getChildren();
						int auxNum = fieldValuesCopy.size();
						int auxCounter = 0;
						// For each unhandled option
						for( Iterator ait=fieldValuesCopy.iterator(); ait.hasNext() ; ) {
							String auxValue = (String) ait.next();
							auxCounter++;
							Element auxOptionElem = new Element( SearchEngineConfig.OPTION_ELEM );
							auxOptionElem.setAttribute( SearchEngineConfig.VALUE_ATTR, auxValue );
							auxOptionElem.setAttribute( SearchEngineConfig.SELECTED_ATTR, "TRUE" );
							auxOptionElem.setAttribute( SearchEngineConfig.DESC_ATTR,
								"Aux value"
								+ ( auxNum>1 ? " # " + auxCounter : "" )
								+ " (\"" + auxValue + "\")"
								);
							existingChildren.add( auxOptionElem );
						}
					}
				}	// End if there were any options
				else {
					warningMsg( kFName,
						"No valid options for field \"" + fieldName + "\""
						);
				}

				/***
				// For each input value
				if( null!=fieldValues && ! fieldValues.isEmpty() ) {
					for( Iterator vit = fieldValues.keySet().iterator() ; vit.hasNext() ; ) {
						String fieldValue = (String) vit.next();
						List formFields =...
				}
				***/
			}	// End for each field
		}	// End if there were any field options at all

		// Add the alt terms
		if( null!=altTerms ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, ALT_TERMS_XML_FORM_PATH, altTerms
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add alt term(s) to edit form."
					+ " Alt term(s) was/were \"" + altTerms + "\"."
					+ " XML path was \"" + ALT_TERMS_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, ALT_TERMS_SUBMIT_CGI_FIELD, altTerms );
		}
		if( null!=altTermsHeadingText ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, ALT_TERMS_HEADING_TEXT_XML_FORM_PATH, altTermsHeadingText
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add alt term(s) heading to edit form."
					+ " Alt term(s) heading was \"" + altTermsHeadingText + "\"."
					+ " XML path was \"" + ALT_TERMS_HEADING_TEXT_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD, altTermsHeadingText );
		}
		if( null!=altTermsHeadingColor ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, ALT_TERMS_HEADING_COLOR_XML_FORM_PATH , altTermsHeadingColor
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add alt term(s) heading color to edit form."
					+ " Alt term(s) heading color was \"" + altTermsHeadingColor + "\"."
					+ " XML path was \"" + ALT_TERMS_HEADING_COLOR_XML_FORM_PATH  + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD, altTermsHeadingColor );
		}


		// Add URL data to the form

		// Add the URL ID
		if( null!=urlID ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, URL_ID_XML_FORM_PATH, urlID
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add URL ID to edit form."
					+ " URL ID was \"" + urlID + "\"."
					+ " XML path was \"" + URL_ID_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, URL_ID_CGI_FIELD, urlID );
		}


		// Add the URL (this is checked by constructor)
		if( null!=url ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, URL_HREF_XML_FORM_PATH, url
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add URL to edit form."
					+ " URL was \"" + url + "\"."
					+ " XML path was \"" + URL_HREF_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, URL_HREF_CGI_FIELD, url );
		}

		// Add the title (this is checked by constructor)
		if( null!=title ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, URL_TITLE_XML_FORM_PATH, title
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add title to edit form."
					+ " Title was \"" + title + "\"."
					+ " XML path was \"" + URL_TITLE_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, URL_TITLE_CGI_FIELD, title );
		}

		// Add the Description (which may be null)
		if( null!=description ) {
			/***
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, URL_DESC_XML_FORM_PATH, description
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add description to edit form."
					+ " Description was \"" + description + "\"."
					+ " XML path was \"" + URL_DESC_XML_FORM_PATH + "\""
					);
			***/
			setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD, description );
		}

		// statusMsg( kFName, JDOMHelper.JDOMToString( ioBlankForm, true ) );
	}



	private void _augmentFormFromExistingDataOBS( String inTargetTerm, Element ioBlankForm )
		throws UIException
	{
		final String kFName = "_augmentFormFromExistingDataOBS";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// sanity
		if( null==ioBlankForm )
			throw new UIException( kExTag +
				"Null blank form passed in."
				+ " Target term was \"" + inTargetTerm + "\""
				);

		/***

		// We need to figure out the ID for the map we want to edit
		// but what they will have passed us is a term
		if( null==inTargetTerm || inTargetTerm.equals(ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE) )
			throw new UIException( kExTag +
				"Null term in CGI parameters."
				+ " Expected CGI field \"" + UILink.TERM_FORMGEN_CGI_FIELD + "\""
				+ " to specify term that is to be edited."
				);

		// Now get the ID of the map we are to edit
		int targetID = DbMapRecord.static_getFirstMapIDForTerm( getDBConfig(), inTargetTerm );
		if( targetID < 1 )
			throw new UIException( kExTag +
				"Invalid map ID for existing term."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		// Add this ID to the tree
		// ioBlankForm
		Element tmpElem = JDOMHelper.findOrCreateElementByPath(
			ioBlankForm,
			MAP_ID_XML_FORM_PATH,	// NOT + "/@value=" + targetID,
			true
			);
		if( null==tmpElem )
			throw new UIException( kExTag + "Unable to add map ID to edit form." );
		// Even hidden fields carry their value as text, not as an attribute
		tmpElem.addContent( "" + targetID );

		// Fill in the XML tree with data from the database

		// We get ALL the terms related to this map ID
		// which may just give us back the one we started with
		List terms = DbMapRecord.static_getTermsForMapID( getDBConfig(), targetID, false );
		if( null==terms || terms.size() < 1 )
			throw new UIException( kExTag +
				"Didn't get map terms."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		// Get back a string list of all the terms for this map
		// String termsStr = NIEUtil.listOfStringsToSingleString( terms );
		String termsStr = NIEUtil.listOfStringsToSingleString2(
				terms,		// List inList,
				true,		// boolean inNullTrimValues,
				false,		// boolean inAddQuotes,
				true,		// boolean inReturnTrueNull,
				null,		// String optSeparator,
				true		// boolean inDoWarnings
				);
		if( null==termsStr )
			throw new UIException( kExTag +
				"Got back null composite term list."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		// Add in this text
		Element tmpElem2 = JDOMHelper.updateSimpleTextToExistingOrNewPath(
			ioBlankForm, TERMS_XML_FORM_PATH, termsStr
			);
		if( null==tmpElem2 )
			throw new UIException( kExTag +
				"Unable to add term(s) to edit form."
				+ " Term(s) was/were \"" + termsStr + "\"."
				+ " XML path was \"" + TERMS_XML_FORM_PATH + "\""
				);

		// And do more stuff

		// We get URLs for this map ID
		// which may just give us back the one we started with
		List urls = DbMapRecord.static_getURLObjects( getMainConfig(), targetID );
		if( null==urls || urls.size() < 1 )
			throw new UIException( kExTag +
				"Didn't get any urls."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);
		// We only use the first one
		SnURLRecord primaryURL = findPrinaryWmsURLInList( urls );
		if( null==primaryURL )
			throw new UIException( kExTag +
				"No primary WMS URL for this map."
				+ " Target term = \"" + inTargetTerm + "\""
				+ ", map ID = \"" + targetID + "\"."
				);

		// Add their data to the form

		// Add the URL (this is checked by constructor)
		String href = primaryURL.getURL();
		tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
			ioBlankForm, URL_HREF_XML_FORM_PATH, href
			);
		if( null==tmpElem )
			throw new UIException( kExTag +
				"Unable to add URL to edit form."
				+ " URL was \"" + href + "\"."
				+ " XML path was \"" + URL_HREF_XML_FORM_PATH + "\""
				);

		// Add the title (this is checked by constructor)
		String title = primaryURL.getTitle();
		tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
			ioBlankForm, URL_TITLE_XML_FORM_PATH, title
			);
		if( null==tmpElem )
			throw new UIException( kExTag +
				"Unable to add title to edit form."
				+ " Title was \"" + title + "\"."
				+ " XML path was \"" + URL_TITLE_XML_FORM_PATH + "\""
				);

		// Add the Description (which may be null)
		String desc = primaryURL.getDescription();
		if( null!=desc ) {
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, URL_DESC_XML_FORM_PATH, desc
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add description to edit form."
					+ " Description was \"" + desc + "\"."
					+ " XML path was \"" + URL_DESC_XML_FORM_PATH + "\""
					);
		}

		// Add the ID
		int urlID = primaryURL.getID();
		if( urlID < 1 )
			throw new UIException( kExTag + "Invalid URL ID for URL \"" + href + "\"" );
		tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
			ioBlankForm, URL_ID_XML_FORM_PATH, ""+urlID
			);
		if( null==tmpElem )
			throw new UIException( kExTag +
				"Unable to add URL ID to edit form."
				+ " URL ID was \"" + urlID + "\"."
				+ " XML path was \"" + URL_ID_XML_FORM_PATH + "\""
				);

		// We get the related/alternative terms for this map ID
		// which may be none
		List altTerms = DbMapRecord.static_getAltTermsForMapID( getDBConfig(), targetID, false );
		// If there are some alt terms to add
		if( null!=altTerms && altTerms.size() > 0 ) {

			// Get back a string list of all the terms for this map
			// String termsStr = NIEUtil.listOfStringsToSingleString( terms );
			String altTermsStr = NIEUtil.listOfStringsToSingleString2(
					altTerms,		// List inList,
					true,		// boolean inNullTrimValues,
					false,		// boolean inAddQuotes,
					true,		// boolean inReturnTrueNull,
					null,		// String optSeparator,
					true		// boolean inDoWarnings
					);
			if( null==altTermsStr )
				throw new UIException( kExTag +
					"Got back null composite term list."
					+ " Target term = \"" + inTargetTerm + "\""
					+ ", map ID = \"" + targetID + "\"."
					);
	
			// Add in this text
			tmpElem = JDOMHelper.updateSimpleTextToExistingOrNewPath(
				ioBlankForm, ALT_TERMS_XML_FORM_PATH, altTermsStr
				);
			if( null==tmpElem )
				throw new UIException( kExTag +
					"Unable to add alt term(s) to edit form."
					+ " Alt term(s) was/were \"" + altTermsStr + "\"."
					+ " XML path was \"" + ALT_TERMS_XML_FORM_PATH + "\""
					);

		}

		***/

		// statusMsg( kFName, JDOMHelper.JDOMToString( ioBlankForm, true ) );

		
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


	// TODO: Should change from using direct SQL logic here to
		//		creating/updating via the DbMapRecord class.
		public Element processDataSubmission(
				AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
				boolean inDoFullPage, String inMode
			)
				throws UIException
		{
			final String kFName = "processDataSubmission";
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
			String operation = inRequestObject.getScalarCGIFieldTrimOrNull( UILink.OPERATION_CGI_FIELD );
			operation = NIEUtil.trimmedLowerStringOrNull( operation );
			if( null==operation )
				throw new UIException( kExTag +
					"Null operation in CGI parameters."
					+ " Expected CGI field \"" + UILink.OPERATION_CGI_FIELD + "\""
					+ " to be one of " + UILink.kValidOperations
					);
	
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
			if( null==button && ! operation.equals(UILink.UI_OPERATION_VIEW) ) {
	
				return redisplayBadFormSubmission(
					inRequestObject,
					ioResponseObject,
					// inMode,
					inDoFullPage,
					new InvalidFormInputException( "Please select a specific Form button." )
					);
	
				/***
				throw new UIException( kExTag +
					"Null button CGI parameters."
					+ " Expected CGI field \"" + BUTTON_CGI_FIELD + "\""
					);
				***/
			}
	
	
	
	//		if( inMode.equals(UILink.UI_MODE_SUBMIT)
	//						|| inMode.equals(UILink.UI_MODE_COMMIT)
	//						|| inMode.equals(UILink.UI_MODE_CANCEL)
	
			// Check for cancel states
			// We force view to cancel
			if( ! operation.equals(UILink.UI_OPERATION_VIEW) ) {
				if( button.indexOf("verify url") >= 0
					|| button.indexOf("check url") >= 0
					|| button.indexOf("fetch url") >= 0
					|| button.indexOf("get url") >= 0
					|| button.indexOf("try url") >= 0
				) {
					inMode = UILink.UI_MODE_REDISPLAY;
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
			}
			// Else it is view, so for cancel
			else {
				inMode = UILink.UI_MODE_CANCEL;
			}
	
	
			debugMsg( kFName,
				"mode=" + inMode
				+ ", operation=" + operation
				+ ", return URL = " + NIEUtil.NL
				+ returnURL
				);
	
			// If we will commit
			if( inMode.equals( UILink.UI_MODE_COMMIT ) )
			{
				if(debug) debugMsg( kFName, "Mode=commit" );
	
				// Which operation is being attempted
				// get the mode, typically "forgen" or "submit"
				String mapIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );
				int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, false, true );
				if( mapID < 1 && ! operation.equals(UILink.UI_OPERATION_ADD) )
					throw new UIException( kExTag +
						"Missing/Invalid map ID."
						);
		
				Connection manualCommitConn = null;
				// ResultSet tmpRes = null;
				try {
					List newTermsDebug = null;
	
					// Only one thread gets to perform this operation at a time
					synchronized( SearchTuningApp.globalMapUpdateLock ) {
		
						if(debug) debugMsg( kFName, "Start Sync on SearchTuningApp.globalMapUpdateLock" );
	
						// Get a non-auto-commit database connection
						manualCommitConn = getDBConfig().getConnection( false );
	
						// Are we deleting?
						if( operation.equals(UILink.UI_OPERATION_DELETE) ) {
							if(debug) debugMsg( kFName, "DELETE" );
	
							String qry = "DELETE FROM nie_map WHERE id = " + mapID;
							getDBConfig().executeStatementWithConnection(
								qry, manualCommitConn, true
								);
	
							// TODO: cleanup other orphan data like terms and URLs
							// if not used by other maps
	
						}
						// Else we're adding or editing
						else {
							if(debug) debugMsg( kFName, "Add/Edit" );
	
							// Get a non-auto-sync connection
							// Connection lConn = getDBConfig().getConnection( false );
							// if( null==lConn )
							//	throw new UIException( kExTag + "Got back null connection from DB config." );
		
							// First, the main Map
		
							// If new, create the new master record
							if( operation.equals(UILink.UI_OPERATION_ADD) ) {
								int newMapID = getNextMapID( manualCommitConn );
								// TODO: MOVE THIS LOGIC TO DbMapRecord !!!!! (yelling at myself)
								// TODO: Add updates to fields created_date(timestamp),
								// created_by_person/text/64, created_comment/text/1024
								String qry = "INSERT INTO nie_map (id) VALUES (" + newMapID + ")";
								if(debug) debugMsg( kFName, "Running SQL \"" + qry + "\"" );
		
								// This will throw an exception if there's any problem
								// tmpRes = getDBConfig().runQueryWithConnection( qry, manualCommitConn );
								// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
								getDBConfig().executeStatementWithConnection(
									qry, manualCommitConn, true
									);
		
								mapID = newMapID;
								if(debug) debugMsg( kFName, "NEW Map ID = " + mapID );
							}
							else {
								if(debug) debugMsg( kFName, "Existing Map ID = " + mapID );
							}
		
							// Get the new terms
							String newTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull(TERMS_SUBMIT_CGI_FIELD);
							// We'll complain here if none, so no need for them to complain as well
							List newTerms = NIEUtil.singleCommaStringToUniqueListOfStrings( newTermsStr, false );
							// For some extra debugging later
							if( null!=newTermsStr ) {
								// Lower case version
								newTermsDebug = NIEUtil.singleCommaStringToUniqueListOfStrings( newTermsStr.toLowerCase(), false );
							}
							if( null==newTerms || newTerms.size() < 1 )
								// throw new UIException( kExTag +
								//	"No terms given for this map."
								//	);
								throw new InvalidFormInputException(
									TERMS_SUBMIT_CGI_FIELD,
									"No terms given for this map; this is a required field"	// String inMessage,
									// "Search Term(s)",								// String inFieldLabel,
									// TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
									// TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
									);
		
							if(debug) debugMsg( kFName, "Map terms = " + newTerms );
			
							// Update the terms associated with this record
							if(debug) debugMsg( kFName, "Calling updateTermsForMapInDb ..." );
							updateTermsForMapInDb(
								getDBConfig(), manualCommitConn, mapID, newTerms, true
								);
	
							// Update the modes
							if(debug) debugMsg( kFName, "Calling processFieldModes with terms ..." );
							processFieldModes(
								getDBConfig(), manualCommitConn, mapID, inRequestObject
								);
			
							// Get the alt terms while we're here (which there may be none)
							String newAltTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull(ALT_TERMS_SUBMIT_CGI_FIELD);
		
							/***
							statusMsg( kFName,
								"newAltTermsStr=\"" + newAltTermsStr + "\" "
								+ (null!=newAltTermsStr ? ""+newAltTermsStr.toCharArray()[0] : "")
								);
							***/
							// since there may be none, tell them not to complain
							List newAltTerms = NIEUtil.singleCommaStringToUniqueListOfStrings( newAltTermsStr, false );
							// This should not come back as a true null, empty is OK
							if( null==newAltTerms )
								throw new UIException( kExTag +
									"Got null for alt terms for this map."
									);
							if(debug) debugMsg( kFName,
								"Alt terms (if any) = " + newAltTerms
								);
							// Update the alt terms associated with this record,
							// this may including remove them all if there are none in this list
							if(debug) debugMsg( kFName, "Calling updateTermsForMapInDb with ALT terms ..." );
							updateTermsForMapInDb(
								getDBConfig(), manualCommitConn, mapID, newAltTerms, false
								);
			
		
		
							// Cosmetics for alt terms
							if(debug) debugMsg( kFName, "Storing options for ALT terms ..." );
							// The intro text
							String altTermsHeadingText = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );
							DbMapRecord.genericSetOrClearSingularMetaProperty(
								BaseMapRecord.ALT_SLOGAN_PATH, mapID,
								"text", altTermsHeadingText,
								getDBConfig(), manualCommitConn
								);
							// The color of the intro text
							String altTermsHeadingColor = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );
							DbMapRecord.genericSetOrClearSingularMetaProperty(
								BaseMapRecord.ALT_SLOGAN_PATH, mapID,
								"color", altTermsHeadingColor,
								getDBConfig(), manualCommitConn
								);
		
		
		
							/***
							// Update URL
							String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
							String title = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
							String desc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
							String urlIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_ID_CGI_FIELD );
		
							// Update the URL info
							updatePrimaryWmsURL( manualCommitConn, mapID,
								url, title, desc,
								urlIDStr, operation.equals(UILink.UI_OPERATION_ADD)
								);
							***/
							// Process potentially more than one
							int numProcessed = processUrls(
								inRequestObject, ioResponseObject,
								manualCommitConn,
								mapID, inMode, operation,
								operation.equals(UILink.UI_OPERATION_ADD)
								);
		
							// Double check that we did at least one
							// It's OK to have zero IF they added alt terms
							if( numProcessed < 1 && newAltTerms.isEmpty() ) {
								errorMsg( kFName, "numProcessed=" + numProcessed );
								throw new InvalidFormInputException(
									URL_HREF_CGI_FIELD,
									"No URL given for this map; this is a required field"	// String inMessage,
									// "URL",								// String inFieldLabel,
									// URL_HREF_XML_FORM_PATH,			// String inXMLFieldPath,
									// URL_HREF_CGI_FIELD				// String inCGIFieldName
									);
							}
		
							if(debug) debugMsg( kFName, "Processed " + numProcessed + " URLs" );
	
	
						}	// End else Add or Edit
	
	
	
						// If we got to here, no errors, go ahead and commit !!!
						if(debug) debugMsg( kFName, "Commit ..." );
						manualCommitConn.commit();
	
						// Some issues possibly related to caching
						// Clear the write connection
						if(debug) debugMsg( kFName, "Closing and clearing DB connetion ..." );
						getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
						manualCommitConn = null;
						// Just to be safe, close the main connection as well
						DBConfig.closeConnection( getDBConfig().getConnectionOrNull(), kClassName(), kFName, false );
	
						
						// If we got this far we believe we have everything ok
						// getMainConfig().readAndSetupMapping();
						if(debug) debugMsg( kFName, "Updating mainConfig mappings for mapID " + mapID );
						getMainConfig().updateMapping( operation, mapID, newTermsDebug );
						// TODO: odd... if this fails, we can't roll back, but if we
						// don't commit.... hmm... maybe pass in connection
	
						
						if(debug) debugMsg( kFName, "End Sync on SearchTuningApp.globalMapUpdateLock" );
	
					}	// End global sync block
		
				}
				// We can handle specific isses with well defined mangled fields
				catch( InvalidFormInputException badFieldException ) {
					if( null!=manualCommitConn ) {
						getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
						manualCommitConn = null;
					}
					return redisplayBadFormSubmission(
						inRequestObject,
						ioResponseObject,
						// inMode,
						inDoFullPage,
						badFieldException
						);
				}
				catch( Throwable t ) {
					try {
						if( null!=manualCommitConn )
							manualCommitConn.rollback();
					}
					catch( Exception e ) {
						errorMsg( kFName, "Error rolling back failed transation(s): Error:" + e );
					}
					if( null!=manualCommitConn ) {
						getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
						manualCommitConn = null;
					}
					errorMsg( kFName, "Error updating, showing stack: " );
					t.printStackTrace( System.err );
					throw new UIException( kExTag +
						"Error updating database."
						+ " Error: " + t
						);		
				}
				finally {
					manualCommitConn = DBConfig.closeConnection( manualCommitConn, kClassName(), kFName, false );
				}
	
				// TODO: probably need finally block, not sure
				// it works with new throws and returns????
				if( null!=manualCommitConn ) {
					getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
					manualCommitConn = null;
				}
	
			}	// End if Commit
			// Else we're just checking the URL
			/////////////////////////////////////////////////////////////
			else if( inMode.equals( UILink.UI_MODE_REDISPLAY ) ) {
	
				// If we encounter problems we'll bail later
				InvalidFormInputException pendingException = null;
				// buffer for the page's content
				byte [] pageContent = null;
				// if we get a redirect, let them know
				String finalURL = null;
	
				// get the URL
				String urlStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
				if( null==urlStr ) {
					pendingException = new InvalidFormInputException(
						URL_HREF_CGI_FIELD,
						"No URL given for this map; nothing to verify."
						);
				}
				// Else was not null
				else {
	
					// We also want to check that it looks like a valid URL
					try
					{
						URL tmpURL =  new URL( urlStr );
					}
					catch(MalformedURLException e)
					{
						pendingException = new InvalidFormInputException(
							URL_HREF_CGI_FIELD,
							"Invalid URL format - this does not look like a valid URL"
							);
					}
	
					// If still OK try to fetch it!
					if( null==pendingException ) {
						try {
							AuxIOInfo tmpIO = new AuxIOInfo();
							pageContent = NIEUtil.fetchURIContentsBin( urlStr, null, null, null, tmpIO, false );
							// We may have got a redir
							finalURL = tmpIO.getFinalURI();
						}
						catch( java.io.IOException ioe ) {
							pendingException = new InvalidFormInputException(
								URL_HREF_CGI_FIELD,
								"Couldn't fetch \"" + urlStr + "\""
								// + " - Error: " + ioe
								);
						}
					}
				}
				// At this point, bail if there was a basic problem
				if( null!=pendingException ) {
					return redisplayBadFormSubmission(
						inRequestObject,
						ioResponseObject,
						inDoFullPage,
						pendingException
						);
				}
	
				String pendingMsg = null;
				String pendingMsgLevel = null;
				String pendingMsgField = null;
	
				if( null!=finalURL && ! finalURL.equalsIgnoreCase(urlStr) ) {
					if( ! NIEUtil.calculateAreSimilarUrls( finalURL, urlStr ) ) {
						// pendingException = new InvalidFormInputException(
						//	URL_HREF_CGI_FIELD,
						pendingMsg =
							"Was redirected to \"" + finalURL + "\""
							// + "; not an error, but you might want to check"
							+ "; you might want to" + /*" double"+*/ " check"
							// + " which URL should be used."
							+ " your URL."
							;
						pendingMsgLevel = "warning";
						pendingMsgField = URL_HREF_CGI_FIELD;
					}
					else {
						pendingMsg = "URL successfully retrieved";
						pendingMsgLevel = "info";
					}
				}
				else {
					pendingMsg = "URL successfully retrieved";
					pendingMsgLevel = "info";
				}
	
				debugMsg( kFName, "Will check submitted meta data" );
	
				// get the submitted title and description, if any
				String submittedTitle = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
				String submittedDesc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
				String fetchedTitle = null;
				String fetchedDesc = null;
	
				// Possibly try to fetch a description and title
				if( null==submittedTitle || submittedTitle.length() < 2
					|| null==submittedDesc || submittedDesc.length() < 2 )
				{
					if( null!=pageContent ) {
						debugMsg( kFName, "Extracting meta data" );
						try {
							nie.spider.PageInfo page = new nie.spider.PageInfo( pageContent );
							fetchedTitle = page.getTitle(true);
							fetchedDesc = page.getDescription(true);
						}
						catch( Exception e ) {
							warningMsg( kFName,
								"Was unable to parse as HTML; will not suggest a title or description."
								+ " URL = \"" + finalURL + "\""
								+ " Exception: " + e
								);
						}
					}
					else
						debugMsg( kFName, "null page content" );
				}
				else
					debugMsg( kFName, "acceptable submitted meta data" );
	
	
				if(debug) debugMsg( kFName, NIEUtil.NL
					+ "final URL=\"" + finalURL + "\""
					+ NIEUtil.NL
					+ "fetchedTitle=\"" + fetchedTitle + "\""
					+ NIEUtil.NL
					+ "submittedTitle=\"" + submittedTitle + "\""
					+ NIEUtil.NL
					+ "fetchedDesc=\"" + fetchedDesc + "\""
					+ NIEUtil.NL
					+ "submittedDesc=\"" + submittedDesc + "\""
					);
	
				return redisplayWithUrlInfo(
					inRequestObject,
					ioResponseObject,
					inDoFullPage,
					// pendingException,
					fetchedTitle,
					fetchedDesc,
					pendingMsg,
					pendingMsgLevel,
					pendingMsgField
					);
	
			}
	
			// Get them back to a safe place
			if( null!=returnURL ) {
				Element outElem = new Element( "redirect" );
				outElem.addContent( returnURL );
				return outElem;
			}
			else
				return null;
		}

	// TODO: Should change from using direct SQL logic here to
	//		creating/updating via the DbMapRecord class.
	public Element _addMap(
			AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
			boolean inDoFullPage, String inMode
		)
			throws UIException
	{
		return null;
	}
	/***
		final String kFName = "addMap";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// Sanity checks
		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );
		if( null==ioResponseObject )
		   throw new UIException( kExTag + "Null response object passed in." );


			// Which operation is being attempted
			// get the mode, typically "forgen" or "submit"
			String mapIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );
			int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, false, true );
			if( mapID < 1 )
				throw new UIException( kExTag +
					"Missing/Invalid map ID."
					);
	
			Connection manualCommitConn = null;
			// ResultSet tmpRes = null;
			try {
				List newTermsDebug = null;

				// Only one thread gets to perform this operation at a time
				synchronized( SearchTuningApp.globalMapUpdateLock ) {
	
					if(debug) debugMsg( kFName, "Start Sync on SearchTuningApp.globalMapUpdateLock" );

					// Get a non-auto-commit database connection
					manualCommitConn = getDBConfig().getConnection( false );

					// First, the main Map
	
					int newMapID = getNextMapID( manualCommitConn );
					String qry = "INSERT INTO nie_map (id) VALUES (" + newMapID + ")";
					if(debug) debugMsg( kFName, "Running SQL \"" + qry + "\"" );
	
					// This will throw an exception if there's any problem
					getDBConfig().executeStatementWithConnection(
						qry, manualCommitConn, true
						);
	
					mapID = newMapID;
					if(debug) debugMsg( kFName, "NEW Map ID = " + mapID );
	
					// Get the new terms
					String newTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull(TERMS_SUBMIT_CGI_FIELD);
					// We'll complain here if none, so no need for them to complain as well
						List newTerms = NIEUtil.singleCommaStringToUniqueListOfStrings( newTermsStr, false );
						// For some extra debugging later
						if( null!=newTermsStr ) {
							// Lower case version
							newTermsDebug = NIEUtil.singleCommaStringToUniqueListOfStrings( newTermsStr.toLowerCase(), false );
						}
						if( null==newTerms || newTerms.size() < 1 )
							// throw new UIException( kExTag +
							//	"No terms given for this map."
							//	);
							throw new InvalidFormInputException(
								TERMS_SUBMIT_CGI_FIELD,
								"No terms given for this map; this is a required field"	// String inMessage,
								// "Search Term(s)",								// String inFieldLabel,
								// TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
								// TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
								);
	
						if(debug) debugMsg( kFName, "Map terms = " + newTerms );
		
						// Update the terms associated with this record
						if(debug) debugMsg( kFName, "Calling updateTermsForMapInDb ..." );
						updateTermsForMapInDb(
							getDBConfig(), manualCommitConn, mapID, newTerms, true
							);

						// Update the modes
						if(debug) debugMsg( kFName, "Calling processFieldModes with terms ..." );
						processFieldModes(
							getDBConfig(), manualCommitConn, mapID, inRequestObject
							);
		
						// Get the alt terms while we're here (which there may be none)
						String newAltTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull(ALT_TERMS_SUBMIT_CGI_FIELD);
	
						// since there may be none, tell them not to complain
						List newAltTerms = NIEUtil.singleCommaStringToUniqueListOfStrings( newAltTermsStr, false );
						// This should not come back as a true null, empty is OK
						if( null==newAltTerms )
							throw new UIException( kExTag +
								"Got null for alt terms for this map."
								);
						if(debug) debugMsg( kFName,
							"Alt terms (if any) = " + newAltTerms
							);
						// Update the alt terms associated with this record,
						// this may including remove them all if there are none in this list
						if(debug) debugMsg( kFName, "Calling updateTermsForMapInDb with ALT terms ..." );
						updateTermsForMapInDb(
							getDBConfig(), manualCommitConn, mapID, newAltTerms, false
							);
		
	
	
						// Cosmetics for alt terms
						if(debug) debugMsg( kFName, "Storing options for ALT terms ..." );
						// The intro text
						String altTermsHeadingText = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );
						DbMapRecord.genericSetOrClearSingularMetaProperty(
							BaseMapRecord.ALT_SLOGAN_PATH, mapID,
							"text", altTermsHeadingText,
							getDBConfig(), manualCommitConn
							);
						// The color of the intro text
						String altTermsHeadingColor = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );
						DbMapRecord.genericSetOrClearSingularMetaProperty(
							BaseMapRecord.ALT_SLOGAN_PATH, mapID,
							"color", altTermsHeadingColor,
							getDBConfig(), manualCommitConn
							);
	
	
	
						// Process potentially more than one
						int numProcessed = processUrls(
							inRequestObject, ioResponseObject,
							manualCommitConn,
							mapID, inMode, operation,
							operation.equals(UILink.UI_OPERATION_ADD)
							);
	
						// Double check that we did at least one
						// It's OK to have zero IF they added alt terms
						if( numProcessed < 1 && newAltTerms.isEmpty() ) {
							errorMsg( kFName, "numProcessed=" + numProcessed );
							throw new InvalidFormInputException(
								URL_HREF_CGI_FIELD,
								"No URL given for this map; this is a required field"	// String inMessage,
								// "URL",								// String inFieldLabel,
								// URL_HREF_XML_FORM_PATH,			// String inXMLFieldPath,
								// URL_HREF_CGI_FIELD				// String inCGIFieldName
								);
						}
	
						if(debug) debugMsg( kFName, "Processed " + numProcessed + " URLs" );


					}	// End else Add or Edit



					// If we got to here, no errors, go ahead and commit !!!
					if(debug) debugMsg( kFName, "Commit ..." );
					manualCommitConn.commit();

					// Some issues possibly related to caching
					// Clear the write connection
					if(debug) debugMsg( kFName, "Closing and clearing DB connetion ..." );
					getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
					manualCommitConn = null;
					// Just to be safe, close the main connection as well
					DBConfig.closeConnection( getDBConfig().getConnectionOrNull(), kClassName(), kFName, false );

					
					// If we got this far we believe we have everything ok
					// getMainConfig().readAndSetupMapping();
					if(debug) debugMsg( kFName, "Updating mainConfig mappings for mapID " + mapID );
					getMainConfig().updateMapping( operation, mapID, newTermsDebug );
					// TODO: odd... if this fails, we can't roll back, but if we
					// don't commit.... hmm... maybe pass in connection

					
					if(debug) debugMsg( kFName, "End Sync on SearchTuningApp.globalMapUpdateLock" );

				}	// End global sync block
	
			}
			// We can handle specific isses with well defined mangled fields
			catch( InvalidFormInputException badFieldException ) {
				if( null!=manualCommitConn ) {
					getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
					manualCommitConn = null;
				}
				return redisplayBadFormSubmission(
					inRequestObject,
					ioResponseObject,
					// inMode,
					inDoFullPage,
					badFieldException
					);
			}
			catch( Throwable t ) {
				try {
					if( null!=manualCommitConn )
						manualCommitConn.rollback();
				}
				catch( Exception e ) {
					errorMsg( kFName, "Error rolling back failed transation(s): Error:" + e );
				}
				if( null!=manualCommitConn ) {
					getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
					manualCommitConn = null;
				}
				errorMsg( kFName, "Error updating, showing stack: " );
				t.printStackTrace( System.err );
				throw new UIException( kExTag +
					"Error updating database."
					+ " Error: " + t
					);		
			}
			finally {
				manualCommitConn = DBConfig.closeConnection( manualCommitConn, kClassName(), kFName, false );
			}

			// TODO: probably need finally block, not sure
			// it works with new throws and returns????
			if( null!=manualCommitConn ) {
				getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
				manualCommitConn = null;
			}		
			
	}
	***/

	public Element redisplayWithUrlInfo(
		AuxIOInfo inRequestObject,
		AuxIOInfo inResponseObject,
		boolean inDoFullPage,
		// InvalidFormInputException optBadFieldException,
		String optTitle,
		String optDesc,
		String optMessage,
		String optMessageSeverity,
		String optMessageField
		)
			throws UIException
	{
		final String kFName = "redisplayWithUrlInfo";
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

		// We re-prefill the form with what they had just typed in
		augmentFormFromCGIInput( formData, inRequestObject );

		// Augment from any optional title or descritpion we were given, if needed
		// Perhaps add in the fetched meta items
		boolean addedTitle = false;
		boolean addedDesc = false;
		if( null!=optTitle ) {
			String submittedTitle = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
			if( null==submittedTitle || submittedTitle.equals(""+NIEUtil.NBSP) ) {
				setFormFieldValue( formData,
					URL_TITLE_CGI_FIELD,
					optTitle
					);
				addedTitle = true;
			}
		}
		if( null!=optDesc ) {
			String submittedDesc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
			if( null==submittedDesc || submittedDesc.equals(""+NIEUtil.NBSP) ) {
				setFormFieldValue( formData,
					URL_DESC_CGI_FIELD,
					optDesc
					);
				addedDesc = true;
			}
		}
		// Let them know what we did
		if( addedTitle || addedDesc ) {
			// We don't mess around if there is already a warning or error pending
			if( null==optMessageSeverity
				|| ( ! optMessageSeverity.equalsIgnoreCase("warning") && ! optMessageSeverity.equalsIgnoreCase("error") )
			) {
				StringBuffer newMsg = new StringBuffer();
				if( null!=optMessage ) {
					newMsg.append(optMessage);
					// Continue the sentence with ; c(opied)
					newMsg.append("; c");
				}
				else
					// Start the sentence with C(opied)
					newMsg.append(optMessage).append('C');
				// newMsg.append( "Copied over the " );
				newMsg.append( "opied over the" );
				if( addedTitle )
					newMsg.append( " Title" );
				if( addedTitle && addedDesc )
					newMsg.append( " and" );
				if( addedDesc )
					newMsg.append( " Description" );
				// newMsg.append( " from the page we checked." );
				optMessage = new String( newMsg );

				// We can also highlight a field
				// but don't overwrite anything that's already there, these are
				// not that important
				if( null==optMessageField )
					if( addedTitle )
						optMessageField = URL_TITLE_CGI_FIELD;
					else if( addedDesc )
						optMessageField = URL_DESC_CGI_FIELD;
			}
		}

		// One last time on the message field
		// we'd like to point out that we checked it
		if( null==optMessageField )
			optMessageField = URL_HREF_CGI_FIELD;

		// Augment the form with user error information, if any
		//	if( null!=optBadFieldException )
		//		augmentFromFieldException(
		//			formData,
		//			optBadFieldException
		//			);

		if( null!=optMessage )
			augmentWithTopMessage(
				formData,
				optMessage,
				optMessageSeverity,
				optMessageField
				);

		// And let displayForm do the rest
		return displayForm( formData, operation, inRequestObject, inDoFullPage );
	}








	private int processUrls( AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
		Connection inConn,
		int inMapID, String inMode, String inOperation, boolean inIsNewMap
		)
			throws UIException, InvalidFormInputException
	{
		final String kFName = "processUrls";
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
		if( null==inOperation )
		   throw new UIException( kExTag + "Null operation passed in." );

		String indexStr, cgiName, xmlPath;
		// For each possible URL
		final int indexStringLength = (""+MAX_URLS_PER_MAP).length();
		int fieldIndex;
		int numProcessed = 0;

		// We will track the URLs that we DO want associated with this map
		HashSet goodURLs = new HashSet();
		for( int i=1; i<=MAX_URLS_PER_MAP ; i++ ) {

			indexStr = NIEUtil.leftPadInt( i, indexStringLength );

			// URL
			cgiName = URL_HREF_NTH_CGI_FIELD;
			xmlPath = URL_HREF_NTH_XML_FORM_PATH;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_HREF_NTH_OFFSET;
			String urlCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String urlXml = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String url = inRequestObject.getScalarCGIFieldTrimOrNull( urlCgi );

			// statusMsg( kFName, "urlCgi=" + urlCgi );

			// TITLE
			cgiName = URL_TITLE_NTH_CGI_FIELD;
			xmlPath = URL_TITLE_NTH_XML_FORM_PATH;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_TITLE_NTH_OFFSET;
			String titleCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String titleXml = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String title = inRequestObject.getScalarCGIFieldTrimOrNull( titleCgi );

			// Description
			cgiName = URL_DESC_NTH_CGI_FIELD;
			xmlPath = URL_DESC_NTH_XML_FORM_PATH;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_DESC_NTH_OFFSET;
			String descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String descXml = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String desc = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );

			// ID
			cgiName = URL_ID_NTH_CGI_FIELD;
			xmlPath = URL_ID_NTH_XML_FORM_PATH;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_ID_NTH_OFFSET;
			String idCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String idXml = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String idStr = inRequestObject.getScalarCGIFieldTrimOrNull( idCgi );
			int urlID = NIEUtil.stringToIntOrDefaultValue(
				idStr, -1, false, true
				);	

			// If all blank, then done
			if( urlID < 1
				&& ( null==url || url.equalsIgnoreCase( XMLDefinedScreen.DEFAULT_URL_FORM_FIELD_PREFIX ) )
				&& null==title && null==desc )
				continue;
				// break;

			debugMsg( kFName,
				"URL # " + i + " id/href/title/des=" + urlID + '/' + url + '/' + title + '/' + desc
				);

			// Do the update
			updateNthWmsURL( inConn, inMapID,
					url, urlCgi, urlXml,
					title, titleCgi, titleXml,
					desc, descCgi, descXml,
					idStr, idCgi, idXml,
					(numProcessed+1), inIsNewMap,
					goodURLs
					);

			numProcessed++;
		}	// End for each possible field


		// Double check the mappings
		finalMapWmsUrlCheck( inConn, inMapID, goodURLs );


		// Tell them how many we did
		return numProcessed;

		/***
		// Update URL
		String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
		String title = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
		String desc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
		String urlIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_ID_CGI_FIELD );

		// Update the URL info
		updatePrimaryWmsURL( manualCommitConn, mapID,
			url, title, desc,
			urlIDStr, operation.equals(UILink.UI_OPERATION_ADD)
			);
		***/
		/***
		throw new InvalidFormInputException(
			"No terms given for this map; this is a required field",	// String inMessage,
			"Search Term(s)",								// String inFieldLabel,
			TERMS_XML_FORM_PATH,								// String inXMLFieldPath,
			TERMS_SUBMIT_CGI_FIELD							// String inCGIFieldName
			);
		***/

	}

	void processFieldModes(
		DBConfig inDBConfig, Connection optConn, int inMapID, AuxIOInfo inRequestObject
		)
			throws UIException
	{

		final String kFName = "processFieldModes";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		boolean debug = staticShouldDoDebugMsg(kFName);

		if( null==inRequestObject )
		   throw new UIException( kExTag + "Null request object passed in." );
		if(debug)
			debugMsg( kFName,
				"CGI request =" + NIEUtil.NL
				+ inRequestObject.displayCGIFieldsIntoBuffer()
				);

		// Sanity checks
		if( null==optConn ) {
			try {
				optConn = inDBConfig.getConnection( true );
			}
			catch( Exception e ) {
				throw new UIException( kExTag + "Error getting connection: " + e );
			}
			staticInfoMsg( kFName, "Supplying my own connection." );
		}

		if( inMapID < 1 )
		   throw new UIException( kExTag + "Invalid map ID " + inMapID + " passed in." );


		// Get the previous modes for this record, if any
		Hashtable previousModes = DbMapRecord.static_getMatchModesForMapID(
			inDBConfig, getSearchEngineConfig(), inMapID
			);

		// Process any criteria fields, if any defined
		Collection critFieldNames = getMainConfig().getSearchEngine().getSearchFormOptionFieldNames();
		if( null!=critFieldNames && ! critFieldNames.isEmpty() ) {
			if(debug) debugMsg( kFName,
					"Checking criteria fields: " + critFieldNames
					);

			// For each possible criteria field
			for( Iterator fit=critFieldNames.iterator(); fit.hasNext() ; ) {
				// The field we want to check
				String critFieldName = (String) fit.next();

				// What has been submitted
				// List cgiValues = inRequestObject.getMultivalueCGIField( critFieldName );
				List cgiValues = inRequestObject.getMultivalueCGIField_UnnormalizedValues( critFieldName );
				cgiValues = (null!=cgiValues) ? cgiValues : new Vector();

				// What was already present, if anything?
				List dbValues = null;
				if( null!=previousModes ) {
					if( previousModes.containsKey( critFieldName ) )
						dbValues = (List) previousModes.get( critFieldName );
				}
				dbValues = (null!=dbValues) ? dbValues : new Vector();

				// What's to be added
				Set addValues = new HashSet();
				addValues.addAll( cgiValues );
				addValues.removeAll( dbValues );

				// What's to be removed
				Set removeValues = new HashSet();
				removeValues.addAll( dbValues );
				removeValues.removeAll( cgiValues );



				if( debug ) {
					staticDebugMsg( kFName,
						"Updating map-field-mode associations for field \"" + critFieldName + "\""
						+ " Database previously had value(s): " + dbValues
						+ ", new submitted value(s): " + cgiValues
						+ " So will add value(s): " + addValues
						+ " and remove values(s): " + removeValues
						);
				}


				// Add what's to be added
				for( Iterator itAdd = addValues.iterator(); itAdd.hasNext() ; ) {
					String value = (String) itAdd.next();
					DbMapRecord.updateMultiMetaProperty(
						DbMapRecord.META_DATA_FIELD_MODE_OWNER_NAME, inMapID,
						critFieldName,
						value,
						inDBConfig, optConn,
						false	// boolean inIsDelete
						);
				}

				// What's to be removed
				for( Iterator itDel = removeValues.iterator(); itDel.hasNext() ; ) {
					String value = (String) itDel.next();
					DbMapRecord.updateMultiMetaProperty(
						DbMapRecord.META_DATA_FIELD_MODE_OWNER_NAME, inMapID,
						critFieldName,
						value,
						inDBConfig, optConn,
						true	// boolean inIsDelete
						);
				}


			}	// End for each criteria field
		}	// End if there are any criteria / Mode fields
		else
			if(debug)
				debugMsg( kFName, "No criteria fields to check for." );

	}


	void finalMapWmsUrlCheck( Connection inConn, int inMapID, HashSet inGoodUrlIds )
		throws UIException
	{
		final String kFName = "finalMapWmsUrlCheck";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		// Sanity checks
		if( null==inConn )
			throw new UIException( kExTag + "Null connection object passed in." );
		if( null==inGoodUrlIds )
			throw new UIException( kExTag + "Null master URL ID set passed in." );
		if( inMapID < 1 )
			throw new UIException( kExTag + "Invalid Map ID passed in:" + inMapID );

		boolean debug = shouldDoDebugMsg( kFName );

		// Now we go through a song and dance to see what the database thinks
		// we have, via this connection
		String qry = "SELECT m.id " + getDBConfig().getVendorAliasString() + " map_id"
			+ ", u.id " + getDBConfig().getVendorAliasString() + " url_id"
			+ ", u.type " + getDBConfig().getVendorAliasString() + " type"
			// TODO: Add other advertisement related items
			+ " FROM nie_map m,"
			+ " 	nie_url u,"
			+ " 	nie_map_url_assoc mua"
			+ " WHERE m.id = mua.map_id"
			+ " AND u.id = mua.url_id"
			+ " AND m.id = " + inMapID
			;
		// TODO: Join with site table and also check for active status from there

		HashSet dbUrlIds = new HashSet();

		ResultSet records = null;
		Statement myStatement = null;
		Connection myConnection = null;

		try {
			// ResultSet records = dbConfig.runQueryOrNull( qry );
			Object [] objs = getDBConfig().runQueryWithConnection(qry, inConn, true );
	
			if( null==objs )
				throw new UIException( kExTag +
					"Got back Null results set when querying database."
					);

			records = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];

			// For each mapping record
			while( records.next() )
			{
				int mapID = records.getInt("map_id");
				int urlID = records.getInt("url_id");
				int typeCode = records.getInt("type");

				// We only consider WMS
				if( typeCode != SnURLRecord.TYPE_WMS )
					continue;

				// Save it in th elist
				dbUrlIds.add( new Integer(urlID) );
			}	// End For each url record
		}
		// catch (SQLException e)
		catch( Exception e )
		{
			throw new UIException( kExTag +
				"Error while reading through records: " + e
				);
		}
		finally {
			records = DBConfig.closeResults( records, kClassName(), kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName(), kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kStaticClassName, kFName, false );
		}


		// So, we have two lists of URL ids
		// We were given the list of the WMS that SHOULD be present
		// and we have a list that he DB currently has
		// so we take the DB list, remove what should be there, and the
		// remainder is the list in the DB that should NOT be there
		if( debug ) {
			debugMsg( kFName, "Should have: " + inGoodUrlIds );
			debugMsg( kFName, "The DB has: " + dbUrlIds );
		}
		// Remove
		dbUrlIds.removeAll( inGoodUrlIds );

		// Delete if there are any
		if( ! dbUrlIds.isEmpty() ) {
			debugMsg( kFName, "Need to remove: " + dbUrlIds );
			for( Iterator it=dbUrlIds.iterator() ; it.hasNext() ; ) {
				Integer idObj = (Integer) it.next();
				updateMapUrlAssociation( inConn,
					inMapID, idObj.intValue(),
					true
					);
			}
		}

	}

	void _updatePrimaryWmsURL( Connection inConn, int inMapID,
		String inHref, String inTitle, String inDescription,
		String optUrlIdStr, boolean inIsNewMap
		)
			throws UIException,
				InvalidFormInputException
	{
		final String kFName = "updatePrimaryWmsURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg(kFName);

		// Sanity checks
		if( null==inConn )
			throw new UIException( kExTag + "Null connection object passed in." );
		if( null==inHref )
			// throw new UIException( kExTag + "Null URL/href passed in." );
			throw new InvalidFormInputException(
			URL_HREF_CGI_FIELD,
				"No URL given for this map; this is a required field"	// String inMessage,
				// "URL",								// String inFieldLabel,
				// URL_HREF_XML_FORM_PATH,			// String inXMLFieldPath,
				// URL_HREF_CGI_FIELD				// String inCGIFieldName
				);

		if( null==inTitle )
			// throw new UIException( kExTag + "Null title passed in." );
			throw new InvalidFormInputException(
				URL_TITLE_CGI_FIELD,
				"You must provide a title for the URL"	// String inMessage,
				// "Title",							// String inFieldLabel,
				// URL_TITLE_XML_FORM_PATH,			// String inXMLFieldPath,
				// URL_TITLE_CGI_FIELD				// String inCGIFieldName
				);

		if( null==inDescription )
			warningMsg( kFName,
				"Null description passed in."
				+ " URL = \"" + inHref + "\""
				);

		if(debug) debugMsg( kFName, "Start:"
			+ " inMapID=" + inMapID
			+ ", optUrlIdStr=" + optUrlIdStr
			+ ", inIsNewMap=" + inIsNewMap
			);


		// First of all, we need to decide whether we should add a new
		// URL, or just update an existing URL.

		// If adding a new map, then we add a new URL

		// If updating an existing map we check the actual URL:
		// * If the URL has changed, we create a different URL record
		// * If the URL is the same, then we might just update the desc and title

		boolean doCreateNewURL = false;
		boolean doUpdateExistingURL = false;
		int previouslyAssociatedUrlId = NIEUtil.stringToIntOrDefaultValue(
			optUrlIdStr, -1, false, true
			);

		if( ! inIsNewMap && previouslyAssociatedUrlId < 1 ) {
			// throw new UIException( kExTag + "Invalid map ID " + inMapID + " passed in." );
			warningMsg( kFName,
				"Map had no existing URLs, map=" + inMapID
				+ ", will tread as new URL (or at least for this map)."
				);
			inIsNewMap = true;
		}



		// We will use the full URL record objects
		SnURLRecord existingURL = null;
		// If it's not a new map, then we may not be adding a new URL either
		if( ! inIsNewMap ) {
			if( previouslyAssociatedUrlId < 1 )
				throw new UIException( kExTag + "Null/Invalid URL ID passed in to update URL." );
			try {
				existingURL = DbMapRecord.static_getASpecificURLObject(
						getMainConfig(), inConn, previouslyAssociatedUrlId
						);
			}
			catch( Exception e ) {
				throw new UIException( kExTag + "Error getting existing URL, ID " + optUrlIdStr + ". Error: " + e );
			}
			if( ! existingURL.getIsASuggestion() ) {
				warningMsg( kFName, "Previous URL is not a suggestion. url=" + existingURL.getURL() );
				// doCreateNewURL = true;
				doUpdateExistingURL = true;
				previouslyAssociatedUrlId = existingURL.getID();
			}
			else {
				String oldHRef = existingURL.getURL();
				// Is it the same record?
				if( inHref.equals(oldHRef) ) {
					doUpdateExistingURL = true;
				}
				else {
					doCreateNewURL = true;
					previouslyAssociatedUrlId = existingURL.getID();
				}
			}
		}
		// Else no pre-existing map ID, always create new URL
		else {
			doCreateNewURL = true;
		}

		if(debug) debugMsg( kFName, "Done intro logic:"
			+ " doCreateNewURL=" + doCreateNewURL
			+ ", doUpdateExistingURL=" + doUpdateExistingURL
			+ ", previouslyAssociatedUrlId=" + previouslyAssociatedUrlId
			);

		// Do the final updates we need
		if( doCreateNewURL ) {
			// create new URL in database
			int newUrlId = addNewWmsURL( inConn,
				inHref, inHref,
				inTitle, null,
				inDescription,
				false
				);

			// Add new map / URL association
			updateMapUrlAssociation( inConn,
				inMapID, newUrlId,
				false
				);

			// If there was a previous association to an old URL, remove that association
			if( previouslyAssociatedUrlId > 0 )
				updateMapUrlAssociation( inConn,
					inMapID, previouslyAssociatedUrlId,
					true
					);

		}
		// Else keep the same URL
		else {
			// has title or desc changed?
			// Just go ahead and update it for now
			// TODO: be more careful and only update if it's changed
			ResultSet theResults = null;
			try {
				inTitle = NIEUtil.sqlEscapeString( inTitle, true );
				if( null==inTitle )
					throw new UIException( kExTag + "Null sql escaped title." );
				inDescription = NIEUtil.sqlEscapeString( inDescription, false );

				StringBuffer buff = new StringBuffer();

				/***
				buff.append( "UPDATE nie_url SET (title, description) = ('" );
				buff.append( inTitle ).append("',");
				if( null!=inDescription )
					buff.append('\'').append( inDescription ).append("')");
				else
					buff.append("NULL)");
				buff.append( " WHERE id=" + previouslyAssociatedUrlId );
				***/

				buff.append( "UPDATE nie_url SET" );
				buff.append( " title = '").append( inTitle ).append( '\'' );
				buff.append( ", description = ");
				if( null!=inDescription )
					buff.append('\'').append( inDescription ).append( '\'' );
				else
					buff.append("NULL");
				// type
				buff.append( ", type = " ).append( SnURLRecord.TYPE_WMS );
				// do_show_text
				buff.append( ", do_show_text = 1" );

				buff.append( " WHERE id=" + previouslyAssociatedUrlId );


				String qry = new String( buff );
				if(debug) debugMsg( kFName, "Updating title/desc w/ SQL=\"" + qry + "\"" );

				// theResults = getDBConfig().runQueryWithConnection( qry, inConn );
				getDBConfig().executeStatementWithConnection(
					qry, inConn, true
					);
			}
			catch( Exception e ) {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
				throw new UIException( kExTag + "Error updating URL info: " + e );
			}
			finally {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
			}
		}
	}

	void updateNthWmsURL( Connection inConn, int inMapID,
		String inHref, String inHrefCgi, String inHrefXml,
		String inTitle, String inTitleCgi, String inTitleXml,
		String inDescription, String inDescriptionCgi, String inDescriptionXml,
		String optUrlIdStr, String inUrlIDCgi, String inUrlIDXml,
		int urlNum, boolean inIsNewMap,
		HashSet inMasterWmsUrlIds
		)
			throws UIException,
				InvalidFormInputException
	{
		final String kFName = "updateNthWmsURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg(kFName);
	
		// Sanity checks
		if( null==inConn )
			throw new UIException( kExTag + "Null connection object passed in." );

		if( null==inMasterWmsUrlIds )
			throw new UIException( kExTag + "Null master URL ID set passed in." );
	
		inHref = NIEUtil.trimmedStringOrNull( inHref );
		// hold off sql escaping the href until we've compared it to any old href
		if( null==inHref || inHref.equalsIgnoreCase( XMLDefinedScreen.DEFAULT_URL_FORM_FIELD_PREFIX ) )
			// throw new UIException( kExTag + "Null URL/href passed in." );
			throw new InvalidFormInputException(
				inHrefCgi,
				"No URL given for this url; this is a required field",	// String inMessage,
				// "URL" + (urlNum>1 ? " # " + urlNum : "" ),					// String inFieldLabel,
				inHrefXml													// String inXMLFieldPath,
				// inHrefCgi														// String inCGIFieldName
				);
		// We also want to check that it looks like a valid URL
		try
		{
			URL tmpURL =  new URL( inHref );
		}
		catch(MalformedURLException e)
		{
			throw new InvalidFormInputException(
				inHrefCgi,
				"Invalid URL format - this does not look like a URL",	// String inMessage,
				// "URL" + (urlNum>1 ? " # " + urlNum : "" ),					// String inFieldLabel,
				inHrefXml													// String inXMLFieldPath,
				// inHrefCgi														// String inCGIFieldName
				);
		}



		/*** we postpone this until we know about redirects first
		if( null==inTitle )
			// throw new UIException( kExTag + "Null title passed in." );
			throw new InvalidFormInputException(
				"You must provide a title for the URL",		// String inMessage,
				"Title" + (urlNum>1 ? " # " + urlNum : "" ),	// String inFieldLabel,
				inTitleXml,										// String inXMLFieldPath,
				inTitleCgi										// String inCGIFieldName
				);
		if( null==inDescription )
			warningMsg( kFName,
				"Null description passed in."
				+ " URL " + (urlNum>1 ? " # " + urlNum : "" ) + " = \"" + inHref + "\""
				);
		***/
	
		if(debug) debugMsg( kFName, "Start:"
			+ " URL # " + urlNum
			+ ", inMapID=" + inMapID
			+ ", optUrlIdStr=" + optUrlIdStr
			+ ", inIsNewMap=" + inIsNewMap
			);
	
	
		// First of all, we need to decide whether we should add a new
		// URL, or just update an existing URL.
	
		// If adding a new map, then we add a new URL
	
		// If updating an existing map we check the actual URL:
		// * If the URL has changed, we create a different URL record
		// * If the URL is the same, then we might just update the desc and title
	
		boolean doCreateNewURL = false;
		boolean doUpdateExistingURL = false;
		int previouslyAssociatedUrlId = NIEUtil.stringToIntOrDefaultValue(
			optUrlIdStr, -1, false, true
			);
	
		if( ! inIsNewMap && previouslyAssociatedUrlId < 1 ) {
			// throw new UIException( kExTag + "Invalid map ID " + inMapID + " passed in." );
			warningMsg( kFName,
				"Map had no existing URLs, map=" + inMapID
				+ ", will tread as new URL (or at least for this map)."
				);
			inIsNewMap = true;
		}
	
	
	
		// We will use the full URL record objects
		SnURLRecord existingURL = null;
		boolean wasARedirect = false;
		// If it's not a new map, then we may not be adding a new URL either
		if( ! inIsNewMap ) {
			if( previouslyAssociatedUrlId < 1 )
				throw new UIException( kExTag + "Null/Invalid URL ID passed in to update URL." );
			try {
				existingURL = DbMapRecord.static_getASpecificURLObject(
						getMainConfig(), inConn, previouslyAssociatedUrlId
						);
			}
			catch( Exception e ) {
				throw new UIException( kExTag + "Error getting existing URL, ID " + optUrlIdStr + ". Error: " + e );
			}
			if( ! existingURL.getIsASuggestion() && ! existingURL.getIsARedirect() ) {
				warningMsg( kFName, "Previous URL is not a suggestion or redirect. url=" + existingURL.getURL() );
				// doCreateNewURL = true;
				doUpdateExistingURL = true;
				previouslyAssociatedUrlId = existingURL.getID();
			}
			else {
				String oldHRef = existingURL.getURL();
				// Is it the same record?
				if( inHref.equals(oldHRef) ) {
					doUpdateExistingURL = true;
				}
				else {
					doCreateNewURL = true;
					previouslyAssociatedUrlId = existingURL.getID();
				}
			}
		}
		// Else no pre-existing map ID, always create new URL
		else {
			doCreateNewURL = true;
		}
		// inHref = NIEUtil.sqlEscapeString( inHref, true );
		// ^^^ no, postpone until update, would double escape if we passed this to create new

		// We need to obsess about redirects so we don't
		// break folks using  them
		if( null!=existingURL && existingURL.getIsARedirect() )
			wasARedirect = true;

	
		if(debug) debugMsg( kFName, "Done intro logic:"
			+ " doCreateNewURL=" + doCreateNewURL
			+ ", doUpdateExistingURL=" + doUpdateExistingURL
			+ ", previouslyAssociatedUrlId=" + previouslyAssociatedUrlId
			);

		// If we're creating a new URL, or updating
		// a url that wasn't specifically a redirect, then
		// we need to obsess about titles and descriptions
		inTitle = NIEUtil.trimmedStringOrNull(inTitle);
		inDescription = NIEUtil.trimmedStringOrNull(inDescription);
		if( ! wasARedirect ) {
			if( null==inTitle )
				// throw new UIException( kExTag + "Null title passed in." );
				throw new InvalidFormInputException(
					inTitleCgi,
					"You must provide a title for the URL"		// String inMessage,
					// "Title" + (urlNum>1 ? " # " + urlNum : "" ),	// String inFieldLabel,
					// inTitleXml,										// String inXMLFieldPath,
					// inTitleCgi										// String inCGIFieldName
					);
			if( null==inDescription )
				warningMsg( kFName,
					"Null description passed in."
					+ " URL " + (urlNum>1 ? " # " + urlNum : "" ) + " = \"" + inHref + "\""
					);
		}
		// Title and description are escaped below
	
		// Do the final updates we need
		if( doCreateNewURL ) {
			// create new URL in database
			int newUrlId = addNewWmsURL( inConn,
				inHref, inHref,
				inTitle, null,
				inDescription,
				wasARedirect
				);
	
			// Add new map / URL association
			updateMapUrlAssociation( inConn,
				inMapID, newUrlId,
				false
				);
	
			// If there was a previous association to an old URL, remove that association
			if( previouslyAssociatedUrlId > 0 )
				updateMapUrlAssociation( inConn,
					inMapID, previouslyAssociatedUrlId,
					true
					);

			// We know we want this one
			inMasterWmsUrlIds.add( new Integer( newUrlId ) );
		}
		// Else keep the same URL
		else {
			// has title or desc changed?
			// Just go ahead and update it for now
			// TODO: be more careful and only update if it's changed
			ResultSet theResults = null;
			try {
				if( null!=inTitle ) {
					inTitle = NIEUtil.sqlEscapeString( inTitle, true );
					if( null==inTitle )
						throw new UIException( kExTag + "Null sql escaped title." );
				}
				if( null!=inDescription )
					inDescription = NIEUtil.sqlEscapeString( inDescription, false );
				// No sql escape for href since we won't be updating it

				StringBuffer buff = new StringBuffer();
	
				/***
				buff.append( "UPDATE nie_url SET (title, description) = ('" );
				buff.append( inTitle ).append("',");
				if( null!=inDescription )
					buff.append('\'').append( inDescription ).append("')");
				else
					buff.append("NULL)");
				buff.append( " WHERE id=" + previouslyAssociatedUrlId );
				***/

				// TODO: MOVE THIS LOGIC TO DbMapRecord !!!!! (yelling at myself)
				// TODO: Add updates to fields last_edit_date(timestamp),
				// last_edit_by_person/text/64, last_edit_comment/text/1024

				buff.append( "UPDATE nie_url SET" );
				buff.append( " title = ");
				if( null!=inTitle )
					buff.append('\'').append( inTitle ).append( '\'' );
				else
					buff.append( "NULL" );
				buff.append( ", description = ");
				if( null!=inDescription )
					buff.append('\'').append( inDescription ).append( '\'' );
				else
					buff.append("NULL");
				// type
				// don't break existing redirects
				if( wasARedirect )
					buff.append( ", type = " ).append( SnURLRecord.TYPE_REDIRECT );
				// We default to WMS
				else
					buff.append( ", type = " ).append( SnURLRecord.TYPE_WMS );
				// do_show_text
				buff.append( ", do_show_text = 1" );
	
				buff.append( " WHERE id=" + previouslyAssociatedUrlId );
	
				String qry = new String( buff );
				if(debug) debugMsg( kFName, "Updating title/desc w/ SQL=\"" + qry + "\"" );
	
				// theResults = getDBConfig().runQueryWithConnection( qry, inConn );
				getDBConfig().executeStatementWithConnection(
					qry, inConn, true
					);
			}
			catch( Exception e ) {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
				throw new UIException( kExTag + "Error updating URL info: " + e );
			}
			finally {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
			}

			// We know we want this one
			inMasterWmsUrlIds.add( new Integer( previouslyAssociatedUrlId ) );

		}	// End if update previous URL
	}

	public static void updateTermsForMapInDb(
		DBConfig inDBConfig, Connection optConn,
		int inMapID, List inNewTerms, boolean inIsMatchTerm
		)
			throws UIException
	{
		final String kFName = "updateTermsForMapInDb";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		boolean debug = staticShouldDoDebugMsg(kFName);

		// Sanity checks
		// if( null==inConn )
		//   throw new UIException( kExTag + "Null connection object passed in." );
		if( null==optConn ) {
			try {
				optConn = inDBConfig.getConnection( true );
			}
			catch( Exception e ) {
				throw new UIException( kExTag + "Error getting connection: " + e );
			}
			staticInfoMsg( kFName, "Supplying my own connection." );
		}

		if( null==inNewTerms )
		   throw new UIException( kExTag + "Null list of new terms passed in." );
		if( inMapID < 1 )
		   throw new UIException( kExTag + "Invalid map ID " + inMapID + " passed in." );

		// We need a normalized set of the new terms
		Hashtable normalizedNewTerms = new Hashtable();
		for( Iterator it0 = inNewTerms.iterator() ; it0.hasNext() ; ) {
			String tmpTerm = (String)it0.next();
			String tmpTerm2 = NIEUtil.trimmedLowerStringOrNull( tmpTerm );
			if( null!=tmpTerm2 )
				normalizedNewTerms.put( tmpTerm2, tmpTerm );
			else
				throw new UIException( kExTag + "Null normalized term from term \"" + tmpTerm + "\"" );
		}


		final String termTable = "nie_term";
		final String termDataField = "text_as_entered";
		final String termCheckField = "text_normalized";
		String assocTable = inIsMatchTerm ? "nie_map_term_assoc" : "nie_map_relterm_assoc" ;
		final String assocMapID = "map_id";
		String assocTermID = inIsMatchTerm ? "term_id" : "related_term_id" ;

		// Maps terms/queries to associated IDs
		Hashtable termsInDB = new Hashtable();

		// For each of the new terms, see if it is already in the database
		if( ! normalizedNewTerms.isEmpty() ) {
			// First we need a list of all the terms that DO exist already
			// TODO: Move this logic to DbMapRecord
			StringBuffer buff = new StringBuffer();
			buff.append( "SELECT id," ).append( termCheckField );
			buff.append( " FROM " ).append( termTable );
			buff.append( " WHERE " ).append( termCheckField ).append( " IN (" );
			boolean isFirst = true;
			for( Iterator it=normalizedNewTerms.keySet().iterator() ; it.hasNext() ; ) {
				String term = (String)it.next();
				// term = NIEUtil.trimmedLowerStringOrNull( term );
				term = NIEUtil.trimmedStringOrNull( term );
				// Escape apostrophies
				term = NIEUtil.sqlEscapeString( term, true );
				if( null==term )
					throw new UIException( kExTag +
						"Error prepping search term \"" + term + "\""
						);
				if( ! isFirst )
					buff.append(',');
				else
					isFirst = false;
				buff.append('\'').append(term).append('\'');
			}
			buff.append(')');
			String qry = new String( buff );
			if(debug) staticDebugMsg( kFName, "Existing terms query = " + qry );
			ResultSet myResults = null;
			Statement myStatement = null;
			Connection myConnection = null;
			try {
				// tmpRes = getDBConfig().runQueryWithConnection( qry, inConn );
				Object [] objs = inDBConfig.runQueryWithConnection( qry, optConn, true );
				myResults = (ResultSet) objs[0];
				myStatement = (Statement) objs[1];
				myConnection = (Connection) objs[2];

				// For each mapping record
				while( myResults.next() )
				{
					int termID = myResults.getInt("id");
					String term = myResults.getString( termCheckField );
					staticTraceMsg( kFName, "DB has term/id = '" + term + "'(id="+termID+")");
					termsInDB.put( term, new Integer(termID) );
				}
			}
			catch( Exception e ) {
				// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
				throw new UIException( kExTag + "Error finding existing terms: " + e );
			}
			finally {
				myResults = DBConfig.closeResults( myResults, kStaticClassName, kFName, false );
				myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
				// myConnection = DBConfig.closeConnection( myConnection, kClassName(), kFName, false );
			}
		}

		// Now we need to find out which terms are NOT yet in the database
		List missingTerms = new Vector();
		// Add in the all the terms we need and then subtract
		// the ones that are already there
		missingTerms.addAll( normalizedNewTerms.keySet() );
		missingTerms.removeAll( termsInDB.keySet() );

		staticDebugMsg( kFName,
			"(assoc table = " + assocTable + ")"
			+ " Adding missing terms (if any): " + missingTerms
			);

		// Add each of the new terms, if any, and remember its ID
		for( Iterator it2 = missingTerms.iterator() ; it2.hasNext() ; ) {
			String addTermKey = (String)it2.next();
			// Get back the original value, as it was typed in
			String addTerm = (String)normalizedNewTerms.get( addTermKey );
			// TODO: this will add the lower case version in
			// TODO: Move this to DbMapRecord
			int addedID = addNewTerm( inDBConfig, optConn, addTerm );
			staticTraceMsg( kFName, "Adding to DB term/id = '" + addTerm + "'(id="+addedID+")");
			termsInDB.put( addTermKey, new Integer(addedID) );
		}

		// At this point termsInDB has ALL the terms that they wanted
		// and the terms' ID's

		// Now we get the existing associations for this term
		List existingAssociations = getMapTermAssociations(
			inDBConfig, optConn, inMapID, assocTable, assocTermID
			);

		// We need 2 lists:
		// associations that should now be removed
		// associations that need to be created
		// Most of the time, when editing, there may be nothing to do!
		// The ones that need to be added

		// What's to be added =
		// What's been requested
		Set assocToAdd = new HashSet( termsInDB.values() );
		// minus what's already in the database
		assocToAdd.removeAll( existingAssociations );
		// = stuff that should be there but isn't yet

		// What's to be removed =
		// What's was in the database
		Set assocToRemove = new HashSet( existingAssociations );
		// minus what's now being requested
		assocToRemove.removeAll( termsInDB.values() );
		// = stuff that was there but is no longer desired

		if( debug ) {
			staticDebugMsg( kFName,
				"Updating map-term associations:"
				+ " (assoc table = " + assocTable + ")"
				+ " Add term IDs: " + assocToAdd
				+ ", remove term IDs: " + assocToRemove
				);
		}


		// Add what's to be added
		for( Iterator itAdd = assocToAdd.iterator(); itAdd.hasNext() ; ) {
			Integer addTermID = (Integer)itAdd.next();
			staticTraceMsg( kFName, "Adding map/term assoc for term id=" + addTermID );
			updateMapTermAssociation(
				inDBConfig, optConn, inMapID, addTermID.intValue(),
				assocTable, assocTermID,
				false
				);
		}

		// Remove what's to be removed
		for( Iterator itDel = assocToRemove.iterator(); itDel.hasNext() ; ) {
			Integer delTermID = (Integer)itDel.next();
			staticTraceMsg( kFName, "Removing map/term assoc for term id=" + delTermID );
			updateMapTermAssociation(
				inDBConfig, optConn, inMapID, delTermID.intValue(),
				assocTable, assocTermID,
				true
				);
		}

	}


	void updateMapUrlAssociation(
		Connection inDBConnection, int inMapID, int inUrlId,
		boolean inIsADeletion
		)
			throws UIException
	{
		final String kFName = "updateMapUrlAssociation";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );
		if( inMapID < 1 )
			throw new UIException( kExTag + "Invalid map ID passed in: " + inMapID );
		if( inUrlId < 1 )
			throw new UIException( kExTag + "Invalid URL ID passed in: " + inUrlId );

		// TODO: MOVE THIS LOGIC TO Url object class! (yelling at myself)

		String qry = null;
		if( inIsADeletion )
			qry = "DELETE FROM ";
		else
			qry = "INSERT INTO ";
		qry += "nie_map_url_assoc";

		// Deleting
		if( inIsADeletion ) {
			qry += " WHERE map_id=" + inMapID;
			qry += " AND url_id=" + inUrlId;
		}
		// Adding
		else {
			qry += " (map_id,url_id) VALUES (";
			qry += "" + inMapID + "," + inUrlId + ")";
		}

		debugMsg( kFName, "Will run SQL \"" + qry + "\"" );

		// ResultSet theResults = null;
		try {
			// theResults = getDBConfig().runQueryWithConnection( qry, inDBConnection );
			getDBConfig().executeStatementWithConnection(
				qry, inDBConnection, true
				);

		}
		catch( Exception e ) {
			// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
			throw new UIException( kExTag + "Error getting IDs: " + e );
		}
		finally {
			// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
		}
	}

	static void updateMapTermAssociation(
		DBConfig inDBConfig, Connection inDBConnection, int inMapID, int inTermID,
		String inAssocTable, String inTermIDField, boolean inIsADeletion
		)
			throws UIException
	{
		final String kFName = "updateMapTermAssociation";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );
		if( null==inAssocTable )
			throw new UIException( kExTag + "Null term table name passed in." );
		if( null==inTermIDField )
			throw new UIException( kExTag + "Null term ID field name passed in." );
		if( inMapID < 1 )
			throw new UIException( kExTag + "Invalid map ID passed in: " + inMapID );
		if( inTermID < 1 )
			throw new UIException( kExTag + "Invalid term ID passed in: " + inTermID );

		// TODO: MOVE THIS LOGIC TO DbMapRecord !!!!! (yelling at myself)
		// TODO: Add updates to fields last_edit_date(timestamp),
		// last_edit_by_person/text/64, last_edit_comment/text/1024

		String qry = null;
		if( inIsADeletion )
			qry = "DELETE FROM ";
		else
			qry = "INSERT INTO ";
		qry += inAssocTable;

		// Deleting
		if( inIsADeletion ) {
			qry += " WHERE map_id=" + inMapID;
			qry += " AND " + inTermIDField + "=" + inTermID;
		}
		// Adding
		else {
			qry += " (map_id," + inTermIDField + ") VALUES (";
			qry += "" + inMapID + "," + inTermID + ")";
		}

		staticDebugMsg( kFName, "SQL = \"" + qry + "\"" );
		// theResults = getDBConfig().runQueryWithConnection( qry, inDBConnection );
		try {
			inDBConfig.executeStatementWithConnection(
				qry, inDBConnection, true
				);
		}
		catch( Exception e ) {
			throw new UIException( kExTag + "Error Map to Term association: " + e );
		}
	}


	static List getMapTermAssociations(
		DBConfig inDBConfig, Connection inDBConnection, int inMapID,
		String inAssocTable, String inTermIDField
		)
			throws UIException
	{
		final String kFName = "getMapTermAssociations";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );
		if( null==inAssocTable )
			throw new UIException( kExTag + "Null term table name passed in." );
		if( null==inTermIDField )
			throw new UIException( kExTag + "Null term ID field name passed in." );
		if( inMapID < 1 )
			throw new UIException( kExTag + "Invalid map ID passed in: " + inMapID );

		String qry = "SELECT " + inTermIDField + inDBConfig.getVendorAliasString() + " tid";
		qry += " FROM " + inAssocTable;
		qry += " WHERE map_id=" + inMapID;

		HashSet outTermIDs = null;
		ResultSet theResults = null;
		Statement theStatement = null;
		Connection theConnection = null;
		try {
			// theResults = getDBConfig().runQueryWithConnection( qry, inDBConnection );
			Object [] objs = inDBConfig.runQueryWithConnection( qry, inDBConnection, true );
			theResults = (ResultSet) objs[0];
			theStatement = (Statement) objs[1];
			theConnection = (Connection) objs[2];

			outTermIDs = new HashSet();
			while( theResults.next() ) {
				int termID = theResults.getInt( "tid" );
				outTermIDs.add( new Integer(termID) );
			}
		}
		catch( Exception e ) {
			throw new UIException( kExTag + "Error getting IDs: " + e );
		}
		finally {
			theResults = DBConfig.closeResults( theResults, kStaticClassName, kFName, false );
			theStatement = DBConfig.closeStatement( theStatement, kStaticClassName, kFName, false );
			// theConnection = DBConfig.closeConnection( theConnection, kClassName(), kFName, false );
		}
		return new Vector( outTermIDs );
	}

	// TODO: this should move to SnURLRecord
	int addNewWmsURL( Connection inDBConnection,
			String inHref, String inDisplayURL,
			String inTitle, String inHoverTitle,
			String inDesc,
			boolean inIsARedirect
		)
			throws UIException
	{
		final String kFName = "addNewURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );

		// Normalize and check inputs

		String href = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inHref), true );
		if( null==href )
			throw new UIException( kExTag + "Null href passed in." );

		String title = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inTitle), false );
		if( null==title && ! inIsARedirect )
			throw new UIException( kExTag + "Null title passed in." );

		String hoverTitle = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inHoverTitle), false );

		String displayURL = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inDisplayURL), false );
		if( null==inDisplayURL )
			displayURL = href;

		String desc = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inDesc), false );

		// Get the next ID and build the SQL insert statement

		int newID = getNextUrlId( inDBConnection );

		StringBuffer buff = new StringBuffer();
		buff.append( "INSERT INTO nie_url (id,type,do_show_text,href_url,display_url,title" );
		if( null!=hoverTitle )
			buff.append( ",hover_title" );
		if( null!=desc )
			buff.append( ",description" );
		// ID
		buff.append( ") VALUES (" ).append(newID);
		// type
		if( inIsARedirect )
			buff.append( ',' ).append( SnURLRecord.TYPE_REDIRECT );
		else
			buff.append( ',' ).append( SnURLRecord.TYPE_WMS );
		// do_show_text
		buff.append( ",1" );
		// href_url
		buff.append( ",'" ).append( href ).append( '\'' );
		// display_url
		buff.append( ",'" ).append( displayURL ).append( '\'' );
		// title
		if( null!=title )
			buff.append( ",'" ).append( title ).append( '\'' );
		else
			buff.append( ", NULL" );
		// hover_title
		if( null!=hoverTitle )
			buff.append( ",'" ).append( hoverTitle ).append( '\'' );
		// description
		if( null!=desc )
			buff.append( ",'" ).append( desc ).append( '\'' );
		buff.append( ')' );

		// TODO: Add other advertisement related items

		String sql = new String( buff );
		debugMsg( kFName, "Will add URL to database with query " + sql );

		// Run the query and return the new ID

		// ResultSet tmpRes = null;
		try {
			// tmpRes = getDBConfig().runQueryWithConnection( sql, inDBConnection );
			getDBConfig().executeStatementWithConnection(
				sql, inDBConnection, true
				);
		}
		catch( Exception e ) {
			// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
			throw new UIException( kExTag + "Error creating new URL: " + e );
		}
		finally {
			// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
		}
		return newID;
	}


	static int addNewTerm(
		DBConfig inDBConfig, Connection inDBConnection, String inTermAsEntered
		)
			throws UIException
	{
		final String kFName = "addNewTerm";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );
		if( null==inTermAsEntered )
			throw new UIException( kExTag + "Null term passed in." );
		String valueText = NIEUtil.trimmedStringOrNull( inTermAsEntered );
		valueText = NIEUtil.sqlEscapeString( valueText, true );
		String keyText = NIEUtil.trimmedLowerStringOrNull( inTermAsEntered );
		keyText = NIEUtil.sqlEscapeString( keyText, true );
		if( null==valueText || null==keyText )
			throw new UIException( kExTag + "Null term passed in (2)." );
		int nextID = getNextTermID( inDBConfig, inDBConnection );
		if( nextID < 1 )
			throw new UIException( kExTag + "Invalid new term ID :" + nextID );
		String sql = "INSERT INTO nie_term (id, text_normalized, text_as_entered)";
		sql += " VALUES (" + nextID + ", '" + keyText + "', '" + valueText + "')";
		staticDebugMsg( kFName, "Will add term with query " + sql );
		// ResultSet tmpRes = null;
		try {
			// tmpRes = getDBConfig().runQueryWithConnection( sql, inDBConnection );
			inDBConfig.executeStatementWithConnection(
				sql, inDBConnection, true
				);
		}
		catch( Exception e ) {
			// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
			throw new UIException( kExTag + "Error creating new term (here): " + e );
		}
		finally {
			// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
		}
		return nextID;
	}

	int getNextUrlId( Connection inDBConnection )
		throws UIException
	{
		return getNextIDFromTable( getDBConfig(), inDBConnection, "nie_url", null );
	}

	static int getNextTermID( DBConfig inDBConfig, Connection inDBConnection )
		throws UIException
	{
		return getNextIDFromTable(
			inDBConfig, inDBConnection, "nie_term", null
			);
	}


	int getNextMapID( Connection inDBConnection )
		throws UIException
	{
		return getNextIDFromTable( getDBConfig(), inDBConnection, "nie_map", null );
	}

	static int getNextIDFromTable( DBConfig inDBConfig, Connection inDBConnection, String inTableName, String optIDField )
		throws UIException
	{
		final String kFName = "getNextIDFromTable";
		final String kExTag = kStaticClassName + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );
		if( null==inTableName )
			throw new UIException( kExTag + "Null table name passed in." );
		// The default field name is "id"
		optIDField = null!=optIDField ? optIDField : "id";

		String qry = "SELECT count(*) " + inDBConfig.getVendorAliasString() + " how_many"
			+ ", max(" + optIDField + ") " + inDBConfig.getVendorAliasString() + " max_id";
		qry += " FROM " + inTableName;
		// qry += " ORDER BY " + optIDField;
		int outID = -1;
		ResultSet myResults = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {
			// theResults = getDBConfig().runQueryWithConnection( qry, inDBConnection );
			Object [] objs = inDBConfig.runQueryWithConnection( qry, inDBConnection, true );
			myResults = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];


			if( ! myResults.next() ) {
				// myResults = DBConfig.closeResults( myResults, kClassName(), kFName, false );
				throw new UIException( kExTag +
					"Unable to get max ID of existing items in table."
					+ " Table=" + inTableName
					+ ", ID field=" + optIDField
					);
			}
			// Check if there are any records
			int recordCount = myResults.getInt( "how_many" );
			// No?  Then start at 1
			if( 0==recordCount )
				outID = 1;
			// Yes?  Get the max ID in use and then add 1
			else
				outID = myResults.getInt( "max_id" ) + 1;
		}
		catch( Exception e ) {
			// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
			throw new UIException( kExTag + "Error getting next ID: " + e );
		}
		finally {
			myResults = DBConfig.closeResults( myResults, kStaticClassName, kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kStaticClassName, kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName(), kFName, false );
		}
		if( outID < 1 )
			throw new UIException( kExTag +
				"Derived invalid ID: " + outID
				+ " table=\"" + inTableName + "\", id=\"" + optIDField + "\""
				);
		return outID;
	}


	public String getTitle( Hashtable inVars )
	{
		// return "Map Form";
		return "Directed Results";
	}
	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}
	public String getSubtitleOrNull( Hashtable inVars )
	{
		return null;
	}

	/***
	static boolean staticStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}
	***/


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















	boolean _mUseCache;
	JDOMHelper _cXML;
	Transformer _cXsltTransformer;
	boolean mHaveAugmentedForm;

	public static final String _XSLT_FORMGEN_SHEET = "generate_form";

	public static final String _MAP_SELECTOR_REPORT_NAME = "ListMapsForTerm";

	// static final String __DEFAULT_IMAGE_URL_PREFIX = "/files/images/webui/";
	// static final String __DEFAULT_HELP_URL_PREFIX = "/files/help/webui/";

	// The name of the submit button
	public static final String BUTTON_CGI_FIELD = "button";

	// public static final String _MAP_ID_FORMGEN_CGI_FIELD = "map";

	// public static final String _TERM_FORMGEN_CGI_FIELD = "term";

	// public static final String TERMS_SUBMIT_CGI_FIELD = "terms";
	// public static final String TERMS_SUBMIT_CGI_FIELD = "term";
	public static final String TERMS_SUBMIT_CGI_FIELD = UILink.TERM_FORMGEN_CGI_FIELD;
	public static final String _TERMS_XML_FORM_PATH = "section[1]/field[1]/@name=terms";

	// public static final String MAP_ID_CGI_FIELD = "map_id";
	public static final String MAP_ID_CGI_FIELD = UILink.MAP_ID_FORMGEN_CGI_FIELD;
	public static final String _MAP_ID_XML_FORM_PATH = "section[1]/field[2]/@name=map_id";

	public static final String _ALT_TERMS_XML_FORM_PATH = "section[3]/field[1]/@name=related_terms";
	public static final String ALT_TERMS_SUBMIT_CGI_FIELD = "related_terms";

	public static final String _ALT_TERMS_HEADING_TEXT_XML_FORM_PATH =
		"section[3]/field[2]/@name=related_terms_heading";
	public static final String ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD =
		"related_terms_heading";
	public static final String _ALT_TERMS_HEADING_TEXT_DEFAULT_VALUE_XML_FORM_PATH =
		_ALT_TERMS_HEADING_TEXT_XML_FORM_PATH
		+ "/option[1]/@value=" + ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;


	public static final String _ALT_TERMS_HEADING_COLOR_XML_FORM_PATH =
		"section[3]/field[3]/@name=related_terms_color";
	public static final String ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD =
		"related_terms_color";
	public static final String _ALT_TERMS_HEADING_COLOR_DEFAULT_VALUE_XML_FORM_PATH =
		_ALT_TERMS_HEADING_COLOR_XML_FORM_PATH
		+ "/option[1]/@value=" + ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;

	// public static final String _RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_FIELD = UILink.RETURN_URL_CGI_FIELD;
	public static final String _RETURN_URL_XML_FORM_PATH = "section[4]/field[2]/@type=hidden/@name=return_url";

	public static final String _DEFAULT_URL_FORM_FIELD_PREFIX = "http://";

	public static final String URL_HREF_CGI_FIELD = "url_01";
	public static final String _URL_HREF_XML_FORM_PATH = "section[2]/field[1]/@name=url_01";
	public static final String URL_TITLE_CGI_FIELD = "title_01";
	public static final String _URL_TITLE_XML_FORM_PATH = "section[2]/field[2]/@name=title_01";
	public static final String URL_DESC_CGI_FIELD = "desc_01";
	public static final String _URL_DESC_XML_FORM_PATH = "section[2]/field[3]/@name=desc_01";
	public static final String URL_ID_CGI_FIELD = "url_id_01";
	public static final String _URL_ID_XML_FORM_PATH = "section[2]/field[4]/@name=url_id_01";

	// Templates for supporting multiple URLs

	public static final String URL_SECTION_XML_FORM_PATH =
		"section[2]";

	public static final String URL_HREF_NTH_CGI_FIELD = "url_NN";
	public static final String URL_HREF_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_HREF_NTH_CGI_FIELD;
	public static final int URL_HREF_NTH_OFFSET = 0;

	// Then the "Verify URL Button" goes here

	public static final String URL_TITLE_NTH_CGI_FIELD = "title_NN";
	public static final String URL_TITLE_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_TITLE_NTH_CGI_FIELD;
	public static final int URL_TITLE_NTH_OFFSET = 2; // 1;

	public static final String URL_DESC_NTH_CGI_FIELD = "desc_NN";
	public static final String URL_DESC_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_DESC_NTH_CGI_FIELD;
	public static final int URL_DESC_NTH_OFFSET = 3; // 2;

	public static final String URL_ID_NTH_CGI_FIELD = "url_id_NN";
	public static final String URL_ID_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_ID_NTH_CGI_FIELD;
	public static final int URL_ID_NTH_OFFSET = 4; // 3;


	public static final int NUM_URL_FORM_FIELDS = 5; // 4;
	public static final int MAX_URLS_PER_MAP = 20; // 99;


	public static Hashtable kColorValuesToNames;

	static {
		kColorValuesToNames = new Hashtable();
		kColorValuesToNames.put( "#000000", "Black" );
		kColorValuesToNames.put( "#800000", "Dark Red" );
		kColorValuesToNames.put( "#ff0000", "Red" );
		kColorValuesToNames.put( "#000080", "Dark Blue" );
		kColorValuesToNames.put( "#330088", "Dark Purple" );
		kColorValuesToNames.put( "#008000", "Dark Green" );
	}

	


}

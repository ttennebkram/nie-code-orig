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

import nie.core.*;
import nie.webui.*;

import nie.sn.SearchTuningApp;
import nie.sn.BaseMapRecord;
import nie.sn.DbMapRecord;
import nie.sn.SnURLRecord;
import nie.sn.UserDataItem;
import nie.sr2.ReportConstants;
import nie.sr2.ReportLink;



/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
// public class CreateMapForm extends BaseScreen
public class CreateTextAdForm extends XMLDefinedScreen
{
	public final static String kStaticClassName = "CreateTextAdForm";
	public String kClassName()
	{
		return kStaticClassName;
	}

	// NO, we still use the parent's full class for locating system resources
	// YES, we are putting our XML relative to here, so css must as well
	public String kFullClassName()
	{
		return "nie.webui.xml_screens.CreateTextAdForm";
	}


	public CreateTextAdForm(
		nie.sn.SearchTuningConfig inMainConfig,
		JDOMHelper optConfigElem,
		String inShortScreenName
		)
			throws UIConfigException, UIException
	{
		super( inMainConfig, optConfigElem, inShortScreenName );
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

		/***
		// Get the alt terms while we're here (which there may be none)
		String altTerms = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_SUBMIT_CGI_FIELD );
		String altTermsHeadingText = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );
		String altTermsHeadingColor = inRequestObject.getScalarCGIFieldTrimOrNull( ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );
		***/

		// Update URL
		// String url = inRequestObject.getScalarCGIFieldTrimOrNull( URL_HREF_CGI_FIELD );
		// String title = inRequestObject.getScalarCGIFieldTrimOrNull( URL_TITLE_CGI_FIELD );
		// String desc = inRequestObject.getScalarCGIFieldTrimOrNull( URL_DESC_CGI_FIELD );
		// String urlIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( URL_ID_CGI_FIELD );

		// Add these to the form
		genericAugmentFormFromFromMethodParms(
			ioBlankForm,
			mapID,
			terms,
			// altTerms,
			// altTermsHeadingText,
			// altTermsHeadingColor,
			null, // urlIDStr,
			null, // url,
			null, // title,
			null, // desc1,
			null, // desc2,
			null, // desc3,
			null, // desc4,
			null  // Ad Code
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

			// Ad Code
			cgiName = AD_CODE_NTH_CGI_FIELD;
			String adCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String adCode = inRequestObject.getScalarCGIFieldTrimOrNull( adCgi );

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
			cgiName = URL_DESC_NTH_CGI_FIELD1;
			String descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String desc1 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );
			cgiName = URL_DESC_NTH_CGI_FIELD2;
			descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String desc2 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );
			cgiName = URL_DESC_NTH_CGI_FIELD3;
			descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String desc3 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );
			cgiName = URL_DESC_NTH_CGI_FIELD4;
			descCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String desc4 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi );

			// ID
			cgiName = URL_ID_NTH_CGI_FIELD;
			String idCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			String idStr = inRequestObject.getScalarCGIFieldTrimOrNull( idCgi );
			int urlID = NIEUtil.stringToIntOrDefaultValue(
				idStr, -1, false, true
				);	

			/***
			debugMsg( kFName,
				"URL # " + i + " id/href/title/des=" + urlID + '/' + url + '/' + title + '/' + desc
				);
			***/

			// If all blank, then done
			if( urlID < 1 && null==adCode && null==url && null==title
					&& null==desc1 && null==desc2 && null==desc3 && null==desc4
				)
				continue;
				// break;


			augmentWithNthURL(
				ioBlankForm,
				i,
				urlID,
				adCode,
				url,
				title,
				desc1,
				desc2,
				desc3,
				desc4,
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

		boolean trace = shouldDoTraceMsg( kFName );

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

			debugMsg( kFName, "mapIDs=" + mapIDs );

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

		/***
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
		***/


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
		genericAugmentFormFromFromMethodParms(
			ioBlankForm,
			""+targetID,
			termsStr,
			// altTermsStr,
			// altTermHeadingText,
			// altTermHeadingColor,
			null, // urlIDStr,
			null, // href,
			null, // title,
			null,  // desc1,
			null,  // desc2,
			null,  // desc3,
			null,  // desc4,
			null  // ad code
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

		// List urls = lMap.getWmsURLObjects();
		List ads = lMap.getUserDataItems();
		if( null==ads || ads.size() < 1 ) {
			// throw new UIException( kExTag +
			warningMsg( kFName,
				"Didn't get any User Defined items / ads."
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

			int howMany = ads.size();
			// For each URL
			// for( Iterator it = urls.iterator() ; it.hasNext() ; ) {
			//	SnURLRecord url = (SnURLRecord) it.next();
			for( int i=0; i<howMany; i++ ) {
				// SnURLRecord url = (SnURLRecord) urls.get(i);
				UserDataItem ad = (UserDataItem) ads.get(i);

				// Add the URL (this is checked by constructor)
				String adCode = ad.getAdCode();

				// Add the URL (this is checked by constructor)
				String href = ad.getURL();
		
				// Add the title (this is checked by constructor)
				String title = ad.getTitle();
		
				// Add the Description (which may be null)
				List descList = ad.getDescriptionLines();
				String desc1 = (null!=descList) && descList.size()>0 ?
					(String) descList.get(0) : null ;
				String desc2 = (null!=descList) && descList.size()>1 ?
					(String) descList.get(1) : null ;
				String desc3 = (null!=descList) && descList.size()>2 ?
					(String) descList.get(2) : null ;
				String desc4 = (null!=descList) && descList.size()>3 ?
					(String) descList.get(3) : null ;

				if( trace )
					traceMsg( kFName,
						"DescList=" + descList
						+ ", desc1=" + desc1
						+ ", desc4=" + desc4
						);
	
				// Add the ID
				int urlID = ad.getUrlId();
				if( urlID < 1 )
					throw new UIException( kExTag + "Invalid URL ID for URL \"" + href + "\"" );

				// Actually add it!
				augmentWithNthURL(
					ioBlankForm,
					(i+1),
					urlID,
					adCode,
					href,
					title,
					desc1,
					desc2,
					desc3,
					desc4,
					sectionTemplate,
					howMany,
					false
					);

			}	// End for each URL
		}

		return true;	
	}

	public void _augmentFormFromSystemDefaults( Element ioBlankForm )
		throws UIException
	{
		// nada
	}

	private void augmentWithNthURL(
			Element ioBlankForm,
			int inPosition,
			int inUrlID,
			String inAdCode,
			String inURL,
			String inTitle,
			String inDescription1,
			String inDescription2,
			String inDescription3,
			String inDescription4,
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


		// The Ad Code
		//////////////
		fieldIndex = baseIndex + AD_CODE_NTH_OFFSET;
		cgiName = AD_CODE_NTH_CGI_FIELD;
		xmlPath = AD_CODE_NTH_XML_FORM_PATH;
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
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (AD_CODE_NTH_OFFSET+1) + "]", true );
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
		if( null!=inAdCode )
			targetElem.setText( inAdCode );




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
				"Unable to augment form with node (2): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( (URL_HREF_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_HREF_NTH_OFFSET+1) + "]", true );
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
				"Unable to augment form with node (3): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_TITLE_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_TITLE_NTH_OFFSET+1) + "]", true );
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
		if( null!=inTitle )
			targetElem.setText( inTitle );

		// Description 1
		//////////////////
		fieldIndex = baseIndex + URL_DESC_NTH_OFFSET1;
		cgiName = URL_DESC_NTH_CGI_FIELD1;
		xmlPath = URL_DESC_NTH_XML_FORM_PATH1;
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
			// templateElem = optSectionTemplate.getChild( ""+(URL_DESC_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_DESC_NTH_OFFSET1+1) + "]", true );
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
			if( optMaxPositionCount > 1 ) {
				String label = targetElem.getAttributeValue( "label" );
				if( null!=label ) {
					label += " # " + inPosition;
					targetElem.setAttribute( "label", label );
				}
			}
		}
		if( null!=inDescription1 )
			targetElem.setText( inDescription1 );
		// Description 2
		//////////////////
		fieldIndex = baseIndex + URL_DESC_NTH_OFFSET2;
		cgiName = URL_DESC_NTH_CGI_FIELD2;
		xmlPath = URL_DESC_NTH_XML_FORM_PATH2;
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
				"Unable to augment form with node (5): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_DESC_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_DESC_NTH_OFFSET2+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (5)"
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
		if( null!=inDescription2 )
			targetElem.setText( inDescription2 );
		// Description 3
		//////////////////
		fieldIndex = baseIndex + URL_DESC_NTH_OFFSET3;
		cgiName = URL_DESC_NTH_CGI_FIELD3;
		xmlPath = URL_DESC_NTH_XML_FORM_PATH3;
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
				"Unable to augment form with node (6): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_DESC_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_DESC_NTH_OFFSET3+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (6)"
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
		if( null!=inDescription3 )
			targetElem.setText( inDescription3 );
		// Description 4
		//////////////////
		fieldIndex = baseIndex + URL_DESC_NTH_OFFSET4;
		cgiName = URL_DESC_NTH_CGI_FIELD4;
		xmlPath = URL_DESC_NTH_XML_FORM_PATH4;
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
				"Unable to augment form with node (7): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_DESC_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_DESC_NTH_OFFSET4+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (7)"
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
		if( null!=inDescription4 )
			targetElem.setText( inDescription4 );

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
				"Unable to augment form with node (8): xmlPath=\"" + xmlPath + "\""
				);
		// Tweak with attrs from the template, if we can find it
		if( null!=optSectionTemplate ) {
			// templateElem = optSectionTemplate.getChild( ""+(URL_ID_NTH_OFFSET+1) );
			templateElem = JDOMHelper.findElementByPath(optSectionTemplate, "field[" + (URL_ID_NTH_OFFSET+1) + "]", true );
			if( null==templateElem )
				throw new UIException( kExTag +
					"Unable to find template field (8)"
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


	private void genericAugmentFormFromFromMethodParms(
			Element ioBlankForm,
			String mapID,
			String terms,
			// String altTerms,
			// String altTermsHeadingText,
			// String altTermsHeadingColor,
			String urlID,
			String url,
			String title,
			String desc1,
			String desc2,
			String desc3,
			String desc4,
			String adCode
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
		if( null!=terms )
			setFormFieldValue( ioBlankForm, TERMS_SUBMIT_CGI_FIELD, terms );


		// Add URL/Ad data to the form

		// Add the URL ID
		if( null!=urlID ) {
			setFormFieldValue( ioBlankForm, URL_ID_CGI_FIELD, urlID );
		}


		// Add the URL (this is checked by constructor)
		if( null!=url )
			setFormFieldValue( ioBlankForm, URL_HREF_CGI_FIELD, url );

		// Add the title (this is checked by constructor)
		if( null!=title )
			setFormFieldValue( ioBlankForm, URL_TITLE_CGI_FIELD, title );

		// Add the Description (which may be null)
		// if( null!=description )
		//	setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD, description );
		if( null!=desc1 )
			setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD1, desc1 );
		if( null!=desc2 )
			setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD2, desc2 );
		if( null!=desc3 )
			setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD3, desc3 );
		if( null!=desc4 )
			setFormFieldValue( ioBlankForm, URL_DESC_CGI_FIELD4, desc4 );

		// Advertising code
		if( null!=adCode )
			setFormFieldValue( ioBlankForm, URL_AD_CODE_CGI_FIELD, adCode );

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

		debugMsg( kFName,
			"button=" + button
			+ ", operation=" + operation
			);

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
			if( button.indexOf("cancel") >= 0
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

			// Which operation is being attempted
			// get the mode, typically "forgen" or "submit"
			String mapIDStr = inRequestObject.getScalarCGIFieldTrimOrNull( MAP_ID_CGI_FIELD );
			// int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, true, true );
			int mapID = NIEUtil.stringToIntOrDefaultValue( mapIDStr, -1, false, true );
			if( mapID < 1 && ! operation.equals(UILink.UI_OPERATION_ADD) )
				throw new UIException( kExTag +
					"Missing/Invalid map ID."
					);
	
			Connection manualCommitConn = null;
			// ResultSet tmpRes = null;
			try {
				// Only one thread gets to perform this operation at a time
				synchronized( SearchTuningApp.globalMapUpdateLock ) {
	
					// Get a non-auto-commit database connection
					manualCommitConn = getDBConfig().getConnection( false );

					// Are we deleting?
					if( operation.equals(UILink.UI_OPERATION_DELETE) ) {

						String qry = "DELETE FROM nie_map WHERE id = " + mapID;
						getDBConfig().executeStatementWithConnection(
							qry, manualCommitConn, true
							);

						// TODO: cleanup other orphan data like terms and URLs
						// if not used by other maps

					}
					// Else we're adding or editing
					else {

						// Get a non-auto-sync connection
						// Connection lConn = getDBConfig().getConnection( false );
						// if( null==lConn )
						//	throw new UIException( kExTag + "Got back null connection from DB config." );
	
						// First, the main Map
	
						// If new, create the new master record
						if( operation.equals(UILink.UI_OPERATION_ADD) ) {
							int newMapID = getNextMapID( manualCommitConn );
							String qry = "INSERT INTO nie_map (id) VALUES (" + newMapID + ")";
							debugMsg( kFName, "Running SQL \"" + qry + "\"" );
	
							// This will throw an exception if there's any problem
							// tmpRes = getDBConfig().runQueryWithConnection( qry, manualCommitConn );
							// tmpRes = DBConfig.closeResults( tmpRes, kClassName(), kFName, false );
							getDBConfig().executeStatementWithConnection(
								qry, manualCommitConn, true
								);
	
							mapID = newMapID;
							debugMsg( kFName, "NEW Map ID = " + mapID );
						}
						else {
							debugMsg( kFName, "Existing Map ID = " + mapID );
						}
	
						// Get the new terms
						String newTermsStr = inRequestObject.getScalarCGIFieldTrimOrNull(TERMS_SUBMIT_CGI_FIELD);
						// We'll complain here if none, so no need for them to complain as well
						List newTerms = NIEUtil.singleCommaStringToUniqueListOfStrings( newTermsStr, false );
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
	
						if(debug) debugMsg( kFName,
							"Map terms = " + newTerms
							);
		
						// Update the terms associated with this record
						updateTermsForMap(
							getDBConfig(), manualCommitConn, mapID, newTerms, true
							);
		
		
						// Get the object subtype they are trying to create
						String userClass = inRequestObject.getScalarCGIFieldTrimOrNull(
							"user_class"
							);
						if( null==userClass )
							throw new UIException( kExTag +
								"No user alias class given; should be a hidden field on the form."
								);


						// Process potentially more than one
						int numProcessed = processUrls(
							inRequestObject, ioResponseObject,
							manualCommitConn,
							mapID,
							userClass,
							inMode, operation,
							operation.equals(UILink.UI_OPERATION_ADD)
							);
	
						// Double check that we did at least one
						// It's OK to have zero IF they added alt terms
						if( numProcessed < 1 ) {
							errorMsg( kFName, "numProcessed=" + numProcessed );
							throw new InvalidFormInputException(
								URL_HREF_CGI_FIELD,
								"No URL given for this map; this is a required field"	// String inMessage,
								// "URL",								// String inFieldLabel,
								// URL_HREF_XML_FORM_PATH,			// String inXMLFieldPath,
								// URL_HREF_CGI_FIELD				// String inCGIFieldName
								);
						}
	


					}	// End else Add or Edit



					// If we got to here, no errors, go ahead and commit !!!
					manualCommitConn.commit();
	
					// Some issues possibly related to caching
					// Clear the write connection
					getDBConfig().closeAndClearAnyManualCommitConnections( manualCommitConn );
					manualCommitConn = null;
					// Just to be safe, close the main connection as well
					DBConfig.closeConnection( getDBConfig().getConnectionOrNull(), kClassName(), kFName, false );
	
					// If we got this far we believe we have everything ok
					// getMainConfig().readAndSetupMapping();
					getMainConfig().updateMapping( operation, mapID, null );
					// TODO: odd... if this fails, we can't roll back, but if we
					// don't commit.... hmm... maybe pass in connection
	
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

		// Get them back to a safe place
		if( null!=returnURL ) {
			Element outElem = new Element( "redirect" );
			outElem.addContent( returnURL );
			return outElem;
		}
		else
			return null;
	}

	private int processUrls( AuxIOInfo inRequestObject, AuxIOInfo ioResponseObject,
		Connection inConn,
		int inMapID,
		String inUserClass,
		String inMode, String inOperation, boolean inIsNewMap
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

			// Ad Code
			cgiName = AD_CODE_NTH_CGI_FIELD;
			xmlPath = AD_CODE_NTH_XML_FORM_PATH;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + AD_CODE_NTH_OFFSET;
			String codeCgi = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String codeXml = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String code = inRequestObject.getScalarCGIFieldTrimOrNull( codeCgi );

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

			// Description 1
			cgiName = URL_DESC_NTH_CGI_FIELD1;
			xmlPath = URL_DESC_NTH_XML_FORM_PATH1;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_DESC_NTH_OFFSET1;
			String descCgi1 = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String descXml1 = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String desc1 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi1 );
			// Description 2
			cgiName = URL_DESC_NTH_CGI_FIELD2;
			xmlPath = URL_DESC_NTH_XML_FORM_PATH2;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_DESC_NTH_OFFSET2;
			String descCgi2 = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String descXml2 = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String desc2 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi2 );
			// Description 3
			cgiName = URL_DESC_NTH_CGI_FIELD3;
			xmlPath = URL_DESC_NTH_XML_FORM_PATH3;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_DESC_NTH_OFFSET3;
			String descCgi3 = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String descXml3 = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String desc3 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi3 );
			// Description 4
			cgiName = URL_DESC_NTH_CGI_FIELD4;
			xmlPath = URL_DESC_NTH_XML_FORM_PATH4;
			fieldIndex = ( (i-1) * NUM_URL_FORM_FIELDS ) + 1 + URL_DESC_NTH_OFFSET4;
			String descCgi4 = NIEUtil.simpleSubstitution( cgiName, "NN", indexStr );
			xmlPath = NIEUtil.simpleSubstitution( xmlPath, "NN", indexStr );
			String descXml4 = NIEUtil.simpleSubstitution( xmlPath, "OO", ""+fieldIndex );
			String desc4 = inRequestObject.getScalarCGIFieldTrimOrNull( descCgi4 );

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
				&& null==code & null==title
				&& null==desc1 && null==desc2 && null==desc3 && null==desc4 )
				continue;
				// break;

			debugMsg( kFName,
				"URL # " + i + " id/href/title/des=" + urlID + '/' + url + '/' + title + '/' + desc1 + " ..."
				);

			debugMsg( kFName, "inUserClass=" + inUserClass );

			// Do the update
			updateNthAdURL( inConn, inMapID,
					inUserClass,
					code, codeCgi, codeXml,
					url, urlCgi, urlXml,
					title, titleCgi, titleXml,
					desc1, descCgi1, descXml1,
					desc2, descCgi2, descXml2,
					desc3, descCgi3, descXml3,
					desc4, descCgi4, descXml4,
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
		String inUserClass,
		String inAdCode,
		String inHref, String inTitle,
		String inDesc1, String inDesc2, String inDesc3, String inDesc4,
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

		if( null==inDesc1 )
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
			int newUrlId = addNewAdURL( inConn,
				inUserClass,
				inAdCode,
				inHref, inHref,
				inTitle, null,
				inDesc1, inDesc2, inDesc3, inDesc4
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
				// inDescription = NIEUtil.sqlEscapeString( inDescription, false );
				inDesc1 = NIEUtil.sqlEscapeString( inDesc1, false );

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
				// if( null!=inDescription )
				//	buff.append('\'').append( inDescription ).append( '\'' );
				if( null!=inDesc1 )
					buff.append('\'').append( inDesc1 ).append( '\'' );
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

	void updateNthAdURL( Connection inConn, int inMapID,
		String inUserClass,
		String inAdCode, String inAdCodeCgi, String inAdCodeXml,
		String inHref, String inHrefCgi, String inHrefXml,
		String inTitle, String inTitleCgi, String inTitleXml,
		String inDesc1, String inDesCgi1, String inDescXml1,
		String inDesc2, String inDesCgi2, String inDescXml2,
		String inDesc3, String inDesCgi3, String inDescXml3,
		String inDesc4, String inDesCgi4, String inDescXml4,
		String optUrlIdStr, String inUrlIDCgi, String inUrlIDXml,
		int urlNum, boolean inIsNewMap,
		HashSet inMasterWmsUrlIds
		)
			throws UIException,
				InvalidFormInputException
	{
		final String kFName = "updateNthAdURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg(kFName);

		// statusMsg( kFName, "desc1=" + inDesc1 );

		// Sanity checks
		if( null==inConn )
			throw new UIException( kExTag + "Null connection object passed in." );

		if( null==inMasterWmsUrlIds )
			throw new UIException( kExTag + "Null master URL ID set passed in." );

		inUserClass = NIEUtil.trimmedStringOrNull( inUserClass );
		// inUserClass = NIEUtil.sqlEscapeString( inUserClass, false );
		if( null==inUserClass )
			throw new UIException( kExTag +
				"No User Class given for this url; this is a required (and usually hidden) field"
				);

		// Note: sql escaping is done way below, only in the update branch,
		// if adding a new url, that routine is repsonsible for it

		inHref = NIEUtil.trimmedStringOrNull( inHref );
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
		// inHref = NIEUtil.sqlEscapeString( inHref, false );

		inAdCode = NIEUtil.trimmedUpperStringOrNull( inAdCode );
		// inAdCode = NIEUtil.sqlEscapeString( inAdCode, false );
		if( null==inAdCode )
			// throw new UIException( kExTag + "Null URL/href passed in." );
			throw new InvalidFormInputException(
				inAdCodeCgi,
				"No Advertisement Code given for this url; this is a required field",	// String inMessage,
				// "URL" + (urlNum>1 ? " # " + urlNum : "" ),					// String inFieldLabel,
				inAdCodeXml													// String inXMLFieldPath,
				// inHrefCgi														// String inCGIFieldName
				);

		// Title
		inTitle = NIEUtil.trimmedStringOrNull( inTitle );
		// inTitle = NIEUtil.sqlEscapeString( inTitle, false );
		if( null==inTitle )
			// throw new UIException( kExTag + "Null title passed in." );
			throw new InvalidFormInputException(
				inTitleCgi,
				"You must provide a title for this Ad",		// String inMessage,
				// "Title" + (urlNum>1 ? " # " + urlNum : "" ),	// String inFieldLabel,
				inTitleXml										// String inXMLFieldPath,
				// inTitleCgi										// String inCGIFieldName
				);
		/***
		if( null==inDescription )
			warningMsg( kFName,
				"Null description passed in."
				+ " URL " + (urlNum>1 ? " # " + urlNum : "" ) + " = \"" + inHref + "\""
				);
		***/

		inDesc1 = NIEUtil.trimmedStringOrNull( inDesc1 );
		inDesc2 = NIEUtil.trimmedStringOrNull( inDesc2 );
		inDesc3 = NIEUtil.trimmedStringOrNull( inDesc3 );
		inDesc4 = NIEUtil.trimmedStringOrNull( inDesc4 );
		if( null==inDesc1 && null==inDesc2 && null==inDesc3 && null==inDesc4 )
			warningMsg( kFName,
				"Null description passed in (0)."
				+ " URL " + (urlNum>1 ? " # " + urlNum : "" ) + " = \"" + inHref + "\""
				);



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
		// SnURLRecord existingURL = null;
		UserDataItem existingURL = null;
		// boolean wasARedirect = false;
		// If it's not a new map, then we may not be adding a new URL either
		if( ! inIsNewMap ) {
			if( previouslyAssociatedUrlId < 1 )
				throw new UIException( kExTag + "Null/Invalid URL ID passed in to update URL." );
			try {
				// existingURL = DbMapRecord.static_getASpecificURLObject(
				//		getMainConfig(), inConn, previouslyAssociatedUrlId
				//		);
				existingURL = DbMapRecord.static_getASpecificAdObject(
						getMainConfig(), inConn, previouslyAssociatedUrlId
						);
			}
			catch( Exception e ) {
				throw new UIException( kExTag + "Error getting existing URL, ID " + optUrlIdStr + ". Error: " + e );
			}

			String oldHRef = existingURL.getURL();
			// Is it the same record?
			if( inHref.equals(oldHRef) ) {
				doUpdateExistingURL = true;
			}
			else {
				doCreateNewURL = true;
				previouslyAssociatedUrlId = existingURL.getUrlId();
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

		// If we're creating a new URL, or updating
		// a url that wasn't specifically a redirect, then
		// we need to obsess about titles and descriptions
		/***
		if( null==inTitle )
			// throw new UIException( kExTag + "Null title passed in." );
			throw new InvalidFormInputException(
				inTitleCgi,
				"You must provide a title for the URL"		// String inMessage,
				// "Title" + (urlNum>1 ? " # " + urlNum : "" ),	// String inFieldLabel,
				// inTitleXml,										// String inXMLFieldPath,
				// inTitleCgi										// String inCGIFieldName
				);
		***/

		/***
		if( null==inDesc1 )
			warningMsg( kFName,
				"Null description passed in (1)."
				+ " URL " + (urlNum>1 ? " # " + urlNum : "" ) + " = \"" + inHref + "\""
				);
		***/
	
		// Do the final updates we need
		if( doCreateNewURL ) {
			// create new URL in database
			int newUrlId = addNewAdURL( inConn,
				inUserClass,
				inAdCode,
				inHref, inHref,
				inTitle, null,
				inDesc1, inDesc2, inDesc3, inDesc4
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
			String qry = null;
			try {

				if( null!=inTitle ) {
					inTitle = NIEUtil.sqlEscapeString( inTitle, true );
					if( null==inTitle )
						throw new UIException( kExTag + "Null sql escaped title." );
				}

				inAdCode = NIEUtil.sqlEscapeString( inAdCode, true );
				inUserClass = NIEUtil.sqlEscapeString( inUserClass, true );



				StringBuffer descriptionBuff = new StringBuffer();
				if( null!=inDesc1 )
					descriptionBuff.append( inDesc1 );
				if( null!=inDesc2 || null!=inDesc3 || null!=inDesc4 )
					descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
				if( null!=inDesc2 )
					descriptionBuff.append( inDesc2 );
				if( null!=inDesc3 || null!=inDesc4 )
					descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
				if( null!=inDesc3 )
					descriptionBuff.append( inDesc3 );
				if( null!=inDesc4 ) {
					descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
					descriptionBuff.append( inDesc4 );
				}
				
				String description = descriptionBuff.length() > 0 ? new String( descriptionBuff ) : null;
				description = NIEUtil.sqlEscapeString( description, false );

				if(debug) debugMsg( kFName, "description=" + description );

				StringBuffer sqlBuff = new StringBuffer();
	
				/***
				buff.append( "UPDATE nie_url SET (title, description) = ('" );
				buff.append( inTitle ).append("',");
				if( null!=inDescription )
					buff.append('\'').append( inDescription ).append("')");
				else
					buff.append("NULL)");
				buff.append( " WHERE id=" + previouslyAssociatedUrlId );
				***/
	
				sqlBuff.append( "UPDATE nie_url SET" );
				sqlBuff.append( " advertisement_code = ");
				if( null!=inAdCode )
					sqlBuff.append('\'').append( inAdCode ).append( '\'' );
				else
					sqlBuff.append( "NULL" );
				sqlBuff.append( ", title = ");
				if( null!=inTitle )
					sqlBuff.append('\'').append( inTitle ).append( '\'' );
				else
					sqlBuff.append( "NULL" );
				sqlBuff.append( ", description = ");
				if( null!=description )
					sqlBuff.append('\'').append( description ).append( '\'' );
				else
					sqlBuff.append("NULL");
				// type
				/***
				// don't break existing redirects
				if( wasARedirect )
					buff.append( ", type = " ).append( SnURLRecord.TYPE_REDIRECT );
				// We default to WMS
				else
					buff.append( ", type = " ).append( SnURLRecord.TYPE_WMS );
				***/
					sqlBuff.append( ", type = " ).append( SnURLRecord.TYPE_AD );
				// do_show_text
				sqlBuff.append( ", do_show_text = 1" );
	
				sqlBuff.append( " WHERE id=" + previouslyAssociatedUrlId );

				qry = new String( sqlBuff );
				if(debug) debugMsg( kFName, "Updating title/desc w/ SQL=\"" + qry + "\"" );
	
				// theResults = getDBConfig().runQueryWithConnection( qry, inConn );
				getDBConfig().executeStatementWithConnection(
					qry, inConn, true
					);
			}
			catch( Exception e ) {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
				errorMsg( kFName, "Error on SQL = " + qry );
				throw new UIException( kExTag +
					"Error updating URL info:" + e
					);
			}
			finally {
				// theResults = DBConfig.closeResults( theResults, kClassName(), kFName, false );
			}

			// We know we want this one
			inMasterWmsUrlIds.add( new Integer( previouslyAssociatedUrlId ) );

		}	// End if update previous URL
	}

	public static void updateTermsForMap(
		DBConfig inDBConfig, Connection optConn,
		int inMapID, List inNewTerms, boolean inIsMatchTerm
		)
			throws UIException
	{
		final String kFName = "updateTermsForMap";
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


		Hashtable termsInDB = new Hashtable();

		// For each of the new terms, see if it is already in the database
		if( ! normalizedNewTerms.isEmpty() ) {
			// First we need a list of all the terms that DO exist already
			StringBuffer buff = new StringBuffer();
			buff.append( "SELECT id," ).append( termCheckField );
			buff.append( " FROM " ).append( termTable );
			buff.append( " WHERE " ).append( termCheckField ).append( " IN (" );
			boolean isFirst = true;
			for( Iterator it=normalizedNewTerms.keySet().iterator() ; it.hasNext() ; ) {
				String term = (String)it.next();
				// term = NIEUtil.trimmedLowerStringOrNull( term );
				term = NIEUtil.trimmedStringOrNull( term );
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
			int addedID = addNewTerm( inDBConfig, optConn, addTerm );
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
			updateMapTermAssociation(
				inDBConfig, optConn, inMapID, addTermID.intValue(),
				assocTable, assocTermID,
				false
				);
		}

		// Remove what's to be removed
		for( Iterator itDel = assocToRemove.iterator(); itDel.hasNext() ; ) {
			Integer delTermID = (Integer)itDel.next();
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
	int addNewAdURL( Connection inDBConnection,
			String inUserClass,
			String inAdCode,
			String inHref, String inDisplayURL,
			String inTitle, String inHoverTitle,
			String inDesc1, String inDesc2, String inDesc3, String inDesc4
			// boolean inIsARedirect
		)
			throws UIException
	{
		final String kFName = "addNewAdURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		if( null==inDBConnection )
			throw new UIException( kExTag + "Null connection passed in." );

		// Normalize and check inputs

		String userClass = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inUserClass), true );
		if( null==userClass )
			throw new UIException( kExTag + "Null User Class passed in." );

		String adCode = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inAdCode), true );
		if( null==adCode )
			throw new UIException( kExTag + "Null Ad Code passed in." );
		adCode = adCode.toUpperCase();

		String href = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inHref), true );
		if( null==href )
			throw new UIException( kExTag + "Null href passed in." );

		String title = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inTitle), false );
		if( null==title /* && ! inIsARedirect*/ )
			throw new UIException( kExTag + "Null title passed in." );

		String hoverTitle = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inHoverTitle), false );

		String displayURL = NIEUtil.sqlEscapeString( NIEUtil.trimmedStringOrNull(inDisplayURL), false );
		if( null==inDisplayURL )
			displayURL = href;



		StringBuffer descriptionBuff = new StringBuffer();
		if( null!=inDesc1 )
			descriptionBuff.append( inDesc1 );
		if( null!=inDesc2 || null!=inDesc3 || null!=inDesc4 )
			descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
		if( null!=inDesc2 )
			descriptionBuff.append( inDesc2 );
		if( null!=inDesc3 || null!=inDesc4 )
			descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
		if( null!=inDesc3 )
			descriptionBuff.append( inDesc3 );
		if( null!=inDesc4 ) {
			descriptionBuff.append( UserDataItem.LINE_BREAK_MARKER );
			descriptionBuff.append( inDesc4 );
		}
		String description = descriptionBuff.length() > 0 ? new String( descriptionBuff ) : null;

		String desc = NIEUtil.sqlEscapeString( description, false );


		// Get the next ID and build the SQL insert statement
		int newID = getNextUrlId( inDBConnection );

		debugMsg( kFName, "newID=" + newID );

		StringBuffer buff = new StringBuffer();
		// buff.append( "INSERT INTO nie_url (id,type,do_show_text,href_url,display_url,title" );
		buff.append( "INSERT INTO nie_url (id,type,user_class,advertisement_code,do_show_text,href_url,display_url,title" );
		if( null!=hoverTitle )
			buff.append( ",hover_title" );
		if( null!=desc )
			buff.append( ",description" );
		// ID
		buff.append( ") VALUES (" ).append(newID);
		// type
		/***
		if( inIsARedirect )
			buff.append( ',' ).append( SnURLRecord.TYPE_REDIRECT );
		else
			buff.append( ',' ).append( SnURLRecord.TYPE_WMS );
		***/
		buff.append( ',' ).append( SnURLRecord.TYPE_AD );
		// user_class
		buff.append( ",'" ).append( userClass ).append( '\'' );
		// ad code
		buff.append( ",'" ).append( adCode ).append( '\'' );
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

	public static final String XSLT_FORMGEN_SHEET = "generate_form";

	// The name of the submit button
	public static final String BUTTON_CGI_FIELD = "button";

	// public static final String _MAP_ID_FORMGEN_CGI_FIELD = "map";

	// public static final String _TERM_FORMGEN_CGI_FIELD = "term";

	// public static final String TERMS_SUBMIT_CGI_FIELD = "terms";
	// public static final String TERMS_SUBMIT_CGI_FIELD = "term";
	public static final String TERMS_SUBMIT_CGI_FIELD = UILink.TERM_FORMGEN_CGI_FIELD;
	// public static final String MAP_ID_CGI_FIELD = "map_id";
	public static final String MAP_ID_CGI_FIELD = UILink.MAP_ID_FORMGEN_CGI_FIELD;
	// public static final String _RETURN_URL_CGI_FIELD = "return_url";
	public static final String RETURN_URL_CGI_FIELD = UILink.RETURN_URL_CGI_FIELD;
	public static final String URL_HREF_CGI_FIELD = "url_01";
	public static final String URL_TITLE_CGI_FIELD = "title_01";
	public static final String URL_DESC_CGI_FIELD1 = "desc_01_line_01";
	public static final String URL_DESC_CGI_FIELD2 = "desc_01_line_02";
	public static final String URL_DESC_CGI_FIELD3 = "desc_01_line_03";
	public static final String URL_DESC_CGI_FIELD4 = "desc_01_line_04";
	public static final String URL_AD_CODE_CGI_FIELD = "ad_code_01";

	public static final String URL_ID_CGI_FIELD = "url_id_01";
	// Templates for supporting multiple URLs

	public static final String URL_SECTION_XML_FORM_PATH =
		"section[2]";

	public static final String AD_CODE_NTH_CGI_FIELD = "ad_code_NN";
	public static final String AD_CODE_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + AD_CODE_NTH_CGI_FIELD;
	public static final int AD_CODE_NTH_OFFSET = 0;


	public static final String URL_HREF_NTH_CGI_FIELD = "url_NN";
	public static final String URL_HREF_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_HREF_NTH_CGI_FIELD;
	public static final int URL_HREF_NTH_OFFSET = 1;

	public static final String URL_TITLE_NTH_CGI_FIELD = "title_NN";
	public static final String URL_TITLE_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_TITLE_NTH_CGI_FIELD;
	public static final int URL_TITLE_NTH_OFFSET = 2;

	public static final String URL_DESC_NTH_CGI_FIELD1 = "desc_NN_line_01";
	public static final String URL_DESC_NTH_XML_FORM_PATH1 =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_DESC_NTH_CGI_FIELD1;
	public static final int URL_DESC_NTH_OFFSET1 = 3;
	public static final String URL_DESC_NTH_CGI_FIELD2 = "desc_NN_line_02";
	public static final String URL_DESC_NTH_XML_FORM_PATH2 =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_DESC_NTH_CGI_FIELD2;
	public static final int URL_DESC_NTH_OFFSET2 = 4;
	public static final String URL_DESC_NTH_CGI_FIELD3 = "desc_NN_line_03";
	public static final String URL_DESC_NTH_XML_FORM_PATH3 =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_DESC_NTH_CGI_FIELD3;
	public static final int URL_DESC_NTH_OFFSET3 = 5;
	public static final String URL_DESC_NTH_CGI_FIELD4 = "desc_NN_line_04";
	public static final String URL_DESC_NTH_XML_FORM_PATH4 =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_DESC_NTH_CGI_FIELD4;
	public static final int URL_DESC_NTH_OFFSET4 = 6;

	public static final String URL_ID_NTH_CGI_FIELD = "url_id_NN";
	public static final String URL_ID_NTH_XML_FORM_PATH =
		URL_SECTION_XML_FORM_PATH + "/field[OO]/@name=" + URL_ID_NTH_CGI_FIELD;
	public static final int URL_ID_NTH_OFFSET = 7;


	public static final int NUM_URL_FORM_FIELDS = 8;
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

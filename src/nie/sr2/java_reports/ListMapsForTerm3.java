/*
 * Created on Aug 31, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.sr2.java_reports;
import org.jdom.Element;
import org.jdom.Comment;
import nie.core.*;
import nie.sr2.*;

import java.util.*;
import java.sql.*;
import nie.sn.CSSClassNames;
import nie.sn.DbMapRecord;
import nie.sn.SnURLRecord;
import nie.sn.BaseMarkup;
import nie.sn.UserDataItem;
import nie.webui.xml_screens.CreateMapForm;
import nie.webui.UILink;


/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ListMapsForTerm3 extends BaseReport
{
	// Note: Which class is controlled by
	// sr2.ReportConstants.MAP_SELECTOR_REPORT_NAME


	public String kClassName()
	{
		return "ListMapsForTerm3";
	}

	public int getRequiredAccessLevel() {
		return ReportConstants.DEFAULT_WRITEABLE_ACCESS_LEVEL;
	}

	public ListMapsForTerm3(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortReportName
		)
			throws ReportConfigException
	{
		// super( inMainApp, inShortReportName );
		super( inMainConfig, inShortReportName );
	}




	private void fixSelfPointingReturnURL( AuxIOInfo inRequestObject )
		throws ReportException
	{
		final String kFName = "fixSelfPointingReturnURL";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inRequestObject )
			throw new ReportException( kExTag +
				"Null request object passed in."
				);

		// Now we obsess about the return URL a bit
		String returnURL = inRequestObject.getScalarCGIFieldTrimOrNull(
			UILink.RETURN_URL_CGI_FIELD
			);
		if( null!=returnURL ) {
			if( returnURL.equals( UILink.RETURN_URL_CGI_SELF_MARKER) ) {
				inRequestObject.clearCGIField( UILink.RETURN_URL_CGI_FIELD );
				String baseURL = inRequestObject.getLocalURLPath();
				debugMsg( kFName, "baseURL=" + baseURL );
				if( null!=baseURL ) {
					int questAt = baseURL.indexOf( '?' );
					if( questAt > 0 )
						baseURL = baseURL.substring( 0, questAt );
					else if( questAt == 0 )
						baseURL = "/";
					// Get the new URL
					debugMsg( kFName, "baseURL now =" + baseURL );
					inRequestObject.setBasicURL( baseURL );
					returnURL = inRequestObject.getFullCGIEncodedURL();
					debugMsg( kFName, "returnURL =" + returnURL );
					if( null!=returnURL )
						inRequestObject.setOrOverwriteCGIField( UILink.RETURN_URL_CGI_FIELD, returnURL );
					else
						errorMsg( kFName, "Got back null trying to set return URL to me." );
				}
				else
					errorMsg( kFName, "No base URL?" );
			}
		}

	}


	public Element runReport(
			AuxIOInfo inRequestObject,
			AuxIOInfo inResponseObject,
			boolean inDoFullPage
		)
			throws ReportException
	{
		final String kFName = "runReport";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		int interval = DEFAULT_INTERVAL;
		int howMany = DEFAULT_HOW_MANY_ROWS;

		int kWrapUrlLength = 65; // 80; // 100;

		if( null==inRequestObject )
			throw new ReportException( kExTag +
				"Null request object passed in."
				);

		fixSelfPointingReturnURL( inRequestObject );

		// We'll use this hash for varaible substitution in Strings
		Hashtable lMasterValuesHash = getVariables( inRequestObject );

		// See if we have a subtitle
		String term = inRequestObject.getScalarCGIFieldTrimOrNull("term");
		String subtitle = null;
		cSubtitleB = null;
		if( null!=term ) {
			subtitle = "For the term \"" + term + "\"";
		}
		// else
		//	cSubtitleB = null;
		debugMsg( kFName, "cSubtitleB=\"" + cSubtitleB + "\"" );
		// Normalize wildcard from * to %
		boolean termHadWildcards = false;
		if( null!=term && term.indexOf('*') >= 0 ) {
			term = NIEUtil.replaceChars( term, '*', '%' );
			termHadWildcards = true;
		}

		String keyUrl = inRequestObject.getScalarCGIFieldTrimOrNull("url");
		if( null!=keyUrl ) {
			if( null==subtitle )
				subtitle = "For the URL \"" + keyUrl + "\"";
			else
				subtitle += " and the URL \"" + keyUrl + "\"";
		}
		// else
		// 	cSubtitleB = null;
		debugMsg( kFName, "subtitle=\"" + subtitle + "\"" );
		// Normalize wildcard from * to %
		boolean keyUrlHadWildcards = false;
		if( null!=keyUrl && keyUrl.indexOf('*') >= 0 ) {
			keyUrl = NIEUtil.replaceChars( keyUrl, '*', '%' );
			keyUrlHadWildcards = true;
		}
		cSubtitleB = subtitle;

		// The two key points in the tree
		Element [] points = prepareBaseOutputTree(
			inDoFullPage, lMasterValuesHash,
			inRequestObject, inResponseObject
			);
		Element outElem = points[0];
		// Element contentHanger = points[1];

		Element mainContentTable = points[1];
		mainContentTable.setAttribute( "class", CSSClassNames.MAIN_CONTENT_TABLE );

		// This holds all the individual rows
		// We need to do this because the MAIN content table has to have stuff in
		// it before the results, but we need get the results to have that data,
		// so we hold results here, then add the header stuff, then this
		Element resultsTableElement = new Element( "table" );
		resultsTableElement.setAttribute( "class", CSSClassNames.RESULTS_TABLE );
		// resultsTableElement.setAttribute( "border", "1" );

		// What are the user defined classes that we know about
		Set allUserClassNames = getMainConfig().getAllUserClassNames();
		// Whether to offer CLASSIC or new drop down style action links
		boolean hasUserClasses = null!=allUserClassNames && ! allUserClassNames.isEmpty();

		//
		// Prepare Column Headings
		// =================================
		//
		///////////////////////////////////////////////

		// Add the header row
		Element headerRowElem = new Element( "tr" );
		headerRowElem.setAttribute( "class", CSSClassNames.HEADER_ROW );
		String thClass = CSSClassNames.HEADER_CELL;

		// String kTermWidth = "300";
		// String kTermWidth = "375";
		String kTermWidth = "320";
		// And each field
		// terms
		Element th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[1]/@width=" + kTermWidth + "/@class=" + thClass,
			true
			);
		th.addContent("Search Term(s)");

		// # of searches
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[2]/@class=" + thClass,
			true
			);
		th.addContent("URL(s)");

		// Action
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[3]/@class=" + thClass,
			true
			);
		// th.addContent("Action");
		th.addContent("Take Action");

		// Add this header row to the table
		// mainContentTable.addContent(headerRowElem);
		resultsTableElement.addContent(headerRowElem);

		// We need to know how many display colunns
		int lReportColumnCount = 3;


		// Now we start working on the query
		//////////////////////////////////////////////////

		// Now get the main results

		// Basic query
		String qry =
			// "SELECT UNIQUE m.id AS map_id"
			"SELECT DISTINCT m.id " + getDBConfig().getVendorAliasString() + " map_id"
			+ " FROM nie_map m"
			;
		if( null!=term )
			qry += ", nie_term t, nie_map_term_assoc mta";
		if( null!=keyUrl )
			qry += ", nie_url u, nie_map_url_assoc mua";


		if( null!=term || null!=keyUrl )
			qry += " WHERE ";

		if( null!=term )
			qry += "mta.term_id = t.id"
				+ " AND mta.map_id = m.id"
				;
		if( null!=keyUrl ) {
			if( null!=term )
				qry += " AND ";
			qry += "mua.url_id = u.id"
				+ " AND mua.map_id = m.id"
				;
		}

		// add the filter
		if( null!=term ) {
			String normTerm = NIEUtil.trimmedLowerStringOrNull( term );
			normTerm = NIEUtil.sqlEscapeString( normTerm, false );
			if( null!=normTerm ) {
				String op = termHadWildcards ? "LIKE" : "=" ;
				qry += " AND t.text_normalized " + op + " '" + normTerm + "'"; 
			}
			else {
				errorMsg( kFName,
					"Unable to sql-normalize term string \"" + term + "\""
					+ " Will run report without term filter criteria."
					);
			}
		}
		if( null!=keyUrl ) {
			keyUrl = NIEUtil.sqlEscapeString( keyUrl, false );
			if( null!=keyUrl ) {
				String op = keyUrlHadWildcards ? "LIKE" : "=" ;
				qry += " AND ( ";
				qry += " u.href_url " + op + " '" + keyUrl + "'"; 
				qry += " OR ";
				qry += " u.display_url " + op + " '" + keyUrl + "'"; 
				qry += " )";
			}
			else {
				errorMsg( kFName,
					"Unable to sql-normalize term string \"" + term + "\""
					+ " Will run report without term filter criteria."
					);
			}
		}


		// grouping and sorting
		// qry += " ORDER BY m.id";
		qry += " ORDER BY m.id DESC";

		debugMsg( kFName, "Will run SQL=" + qry );


		//
		// RUN the query!!!
		// ==================================================
		// (and only build the headers if we had success)
		//
		ResultSet results = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try {
			// results = getDBConfig().runQuery( qry );
			Object [] objs = getDBConfig().runQuery( qry, true );
			results = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];


			debugMsg( kFName, "starting loop" );

			// Some Pre-Loop Calculations
			///////////////////////////////////////////
			int desiredStartRow = getStartRow( inRequestObject );
			int desiredRowCount = getDesiredRowCount( inRequestObject );
			int desiredEndRow = -1;
			if( desiredRowCount > 0 )
				desiredEndRow = desiredStartRow + desiredRowCount - 1;
			// Whether or not we even care about paging
			// USUALLY desiredRowCount WILL BE > 0 so this
			// will almost always be true, the other test is
			// just in case somebody is looking at "all" results
			// but for some reason starts in mid list
			boolean isDoingPaging =
				desiredRowCount > 0	// Usually TRUE
				|| desiredStartRow > 1	// Backup sanity test
				;

			// Some state fields, easier to set them specifically
			// then to try and derive them after the fact
			int actualFirstRowDisplayed = -1;
			int actualLastRowDisplayed = -1;
			boolean hadAnotherRow = false;
			boolean atEndOfRecords = false;

			// This row count is the ACTUAL db results
			// row counter
			// It is used and carried forward in
			// BOTH loops below
			int rowCount = 0;

			// we need this for even/odd stuff
			// and if we displayed 5 records at a time
			// record 6, though technically, would be the 1st
			// record displayed on page 2, and would therefore
			// be treated as odd dispaly-style-wise
			int displayedRowCount = 0;


			// Help with Bulding Links
			// =====================================
			// We'll be building links to the UI
			// we'll start with a template link
			nie.webui.UILink uiLink = null;
			try {
				uiLink = new nie.webui.UILink(
					getMainConfig(),
					// "CreateMapForm",							// String Screen Name
					UILink.CLASSIC_CREATE_MAP_UI_SCREEN,		// String Screen Name
					// CreateMapForm.TERM_FORMGEN_CGI_FIELD,	// String inParmCGIName,
					UILink.MAP_ID_FORMGEN_CGI_FIELD,	// String inParmCGIName,
					"Edit this term Map",	// String optLinkTitleText
					null
					);
			}
			catch( nie.webui.UIConfigException e ) {
				throw new ReportException( kExTag
					+ "Error creating UI linking object (1): " + e
					);
			}

			// We need another UI link for creating a new term
			nie.webui.UILink uiLink2 = null;
			try {
				uiLink2 = new nie.webui.UILink(
					getMainConfig(),
					// "CreateMapForm",							// String Screen Name
					UILink.CLASSIC_CREATE_MAP_UI_SCREEN,		// String Screen Name
					UILink.TERM_FORMGEN_CGI_FIELD,	// String inParmCGIName,
					// CreateMapForm.MAP_ID_FORMGEN_CGI_FIELD,	// String inParmCGIName,
					"Create a new Term Map",	// String optLinkTitleText
					null
					);
			}
			catch( nie.webui.UIConfigException e ) {
				throw new ReportException( kExTag
					+ "Error creating UI linking object (2): " + e
					);
			}

			// Links to the search engine for a test drive
			// Don't need try/catch because it also throws ReportException
			/***
			SearchEngineLink searchLink = new SearchEngineLink(
				getMainConfig(),
				"_blank",										// String optWindowTarget,
				"Run this search now (in a new window)",	// String optLinkTitleText,
				null											// String optCssClass
				);
			***/

			//
			// Catch up to the start of the part we want
			//
			//////////////////////////////////////////////////
			// Will skip if desiredStartRow == 1
			// Remember, row count hasn't been incremented yet at the
			// start of the loop
			while( ! atEndOfRecords && (rowCount+1) < desiredStartRow )
			{
				if( ! results.next() )
				{
					atEndOfRecords = true;
					break;
				}
				// OK, count this row
				rowCount++;
			}

			/////////////////////////////////////////
			//
			//	****************************
			//	*****    MAIN LOOP    ******
			//	****************************
			//
			//////////////////////////////////////////
			// Step through the results
			int runningNumSearchesTotal = 0;
			while( ! atEndOfRecords )
			{

				// Do some break testing if end row is set
				if( desiredEndRow > 0 ) {
					// Remember, row count hasn't been incremented yet
					// at the start of the loop
					if( (rowCount+1) > desiredEndRow )
						break;
				}

				// Attempt to goto the next record
				if( ! results.next() ) {
					atEndOfRecords = true;
					break;
				}
				// OK, we have a record
				rowCount++;

				// This is the count of records displayed
				// ON THIS PAGE
				displayedRowCount++;

				// Create the results row
				Element rowElem = new Element( "tr" );
				if( displayedRowCount % 2 == 0 )
					rowElem.setAttribute( "class", CSSClassNames.EVEN_ROW );
				else
					rowElem.setAttribute( "class", CSSClassNames.ODD_ROW );

				// Get the only field from the main map, the Map ID
				int mapID = results.getInt( "map_id" );
				if( mapID < 1 )
					throw new ReportException( kExTag +
						"Invalid map ID " + mapID
						);

				int colCount = 0;

				// Column 1, the terms for this map
				// String mapIDStr = ""+mapID;

				// Do we have classic Web Master Suggests or Alt Terms to suggest for this term?
				boolean hasClassicSuggestions = DbMapRecord.static_hasWmsOrAltTermsForMap(
						getMainConfig(), mapID
						);
				// all the user classes currently in use for this term
				Set existingAdClasses = null;
				if( hasUserClasses ) {
					existingAdClasses = DbMapRecord.static_getUserDataClassNamesForMap(
							getMainConfig(), mapID
							);
					debugMsg( kFName, "classes for map \"" + mapID + "\" = " + existingAdClasses );
				}



				// We get ALL the terms related to this map ID
				// which may just give us back the one we started with
				List terms = DbMapRecord.static_getTermsForMapID( getDBConfig(), mapID, false );
				if( null==terms || terms.size() < 1 )
					throw new ReportException( kExTag +
						"Didn't get map terms."
						+ " Target term = \"" + term + "\""
						+ ", map ID = \"" + mapID + "\"."
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
					throw new ReportException( kExTag +
						"Got back null composite term list."
						+ " Target term = \"" + term + "\""
						+ ", map ID = \"" + mapID + "\"."
						);



				// We get ALL the terms related to this map ID
				// which may just give us back the one we started with
				String altTermsStr = null;
				List altTerms = DbMapRecord.static_getAltTermsForMapID( getDBConfig(), mapID, false );
				if( null!=altTerms && ! altTerms.isEmpty() ) {
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
						throw new ReportException( kExTag +
							"Got back null composite alt term list."
							+ " Target term = \"" + term + "\""
							+ ", map ID = \"" + mapID + "\"."
							);
					// altTermsStr = "Alt Terms: " + altTermsStr;
				}



				// We will generate a hyperlink
				// This will already be inside of a <td> tag
				/***
				Element tmpElem = generateHyperlinkedSearchTerm(
					inRequestObject,
					lQueryDisplay, isNullSearch,
					termCSSClass, ndays
					);
				colCount++;
				rowElem.addContent( tmpElem );
				***/
				// Create the first cell and add the query term to it
				Element tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]"					+ "/@width=" + kTermWidth
					+ "/@valign=top"
					+ "/@class=" + CSSClassNames.DATA_CELL
					,
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( termsStr );
				if( null!=altTermsStr ) {
					tmpElem.addContent( new Element("br") );
					Element sElem = new Element("small");
					Element iElem = new Element("i");
					sElem.addContent( iElem );
					Element bElem = new Element("b");
					bElem.addContent( "Alt terms: " );
					iElem.addContent( bElem );
					iElem.addContent( altTermsStr );
					tmpElem.addContent( sElem );
				}


				// Create the 2nd cell showing the URLs associated
				// with this map

				// We get URLs for this map ID
				// which may just give us back the one we started with
				Element urlsTag = null;
				boolean hadSomeUrls = false;
				urlsTag = new Element( "small" );
				List urls = DbMapRecord.static_getURLObjects( getMainConfig(), mapID );
				if( null==urls || urls.size() < 1 ) {
					// throw new ReportException( kExTag +
					// warningMsg( kFName,
					infoMsg( kFName,
						"Didn't get any urls."
						+ " Target term = \"" + term + "\""
						+ ", map ID = \"" + mapID + "\"."
						);
				}
				// Else we did find some URLs
				else {
					for( Iterator it = urls.iterator(); it.hasNext() ; ) {
						SnURLRecord url = (SnURLRecord) it.next();
						// Add the title (this is checked by constructor)
						String title = url.getTitle();
						title = NIEUtil.trimmedStringOrNull( title );
						if( null!=title ) {
							Element bElem = new Element( "b" );
							bElem.addContent( title );
							urlsTag.addContent( bElem );
							urlsTag.addContent( new Element("br") );
						}
						// Add the URL (this is checked by constructor)
						String href = url.getURL();
						href = NIEUtil.trimmedStringOrNull( href );
						String color = "blue";
						if( null!=href ) {
							if( url.getIsARedirect() )
								color = "red";
							Element fontElem = new Element( "font" );
							fontElem.setAttribute( "color", color );
							// We'll need to chop long urls for display
							// See also Ads
							if( href.length() <= kWrapUrlLength )
								fontElem.addContent( href );
							else {
								List hrefParts = urlChopper( href, kWrapUrlLength );
								boolean isFirst = true;
								for( Iterator pit=hrefParts.iterator(); pit.hasNext() ; ) {
									String part = (String) pit.next();
									// Indent subsequent lines
									if( ! isFirst ) {
										fontElem.addContent( new Element( "br" ) );
										fontElem.addContent( ""+NIEUtil.NBSP );
										// fontElem.addContent( ""+NIEUtil.NBSP );
									}
									fontElem.addContent( part );
									/***
									if( isFirst )
										fontElem.addContent( part );
									else {
										Element tmpE = new Element( "dd" );
										tmpE.addContent( part );
										fontElem.addContent( tmpE );
									}
									***/

									isFirst = false;
								}
							}

							urlsTag.addContent( fontElem );
							urlsTag.addContent( new Element("br") );
							hadSomeUrls = true;
						}
						// String desc = primaryURL.getDescription();
						// int urlID = primaryURL.getID();
					}
				}

				// Add user data items such as Advertisements
				List items = DbMapRecord.static_getUserDataItems( getMainConfig(), mapID );
				if( null==items || items.size() < 1 ) {
					// throw new ReportException( kExTag +
					// warningMsg( kFName,
					infoMsg( kFName,
						"Didn't get any user data urls."
						+ " Target term = \"" + term + "\""
						+ ", map ID = \"" + mapID + "\"."
						);
				}
				// Else we did find some URLs
				else {
					for( Iterator it = items.iterator(); it.hasNext() ; ) {
						UserDataItem item = (UserDataItem) it.next();
						// Add the title (this is checked by constructor)
						String title = item.getTitle();
						title = NIEUtil.trimmedStringOrNull( title );
						if( null!=title ) {
							Element bElem = new Element( "b" );
							bElem.addContent( title );
							urlsTag.addContent( bElem );
							urlsTag.addContent( new Element("br") );
						}
						// Add the URL (this is checked by constructor)
						String href = item.getURL();
						href = NIEUtil.trimmedStringOrNull( href );
						String color = "green";
						if( null!=href ) {
							Element fontElem = new Element( "font" );
							fontElem.setAttribute( "color", color );
							// break long urls
							if( href.length() <= kWrapUrlLength )
								fontElem.addContent( href );
							else {
								List hrefParts = urlChopper( href, kWrapUrlLength );
								boolean isFirst = true;
								for( Iterator pit=hrefParts.iterator(); pit.hasNext() ; ) {
									String part = (String) pit.next();
									// Indent subsequent lines
									if( ! isFirst ) {
										fontElem.addContent( new Element( "br" ) );
										fontElem.addContent( ""+NIEUtil.NBSP );
										// fontElem.addContent( ""+NIEUtil.NBSP );
									}
									fontElem.addContent( part );
									/***
									if( isFirst )
										fontElem.addContent( part );
									else {
										Element tmpE = new Element( "dd" );
										tmpE.addContent( part );
										fontElem.addContent( tmpE );
									}
									***/

									isFirst = false;
								}
							}




							urlsTag.addContent( fontElem );
							urlsTag.addContent( new Element("br") );
						}
						hadSomeUrls = true;
						// String desc = primaryURL.getDescription();
						// int urlID = primaryURL.getID();
					}
				}


				// Add the content
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]"					+ "/@class=" + CSSClassNames.DATA_CELL
					+ "/@valign=top"
					,
					true	// Yes, tell us about errors
					);
				if( hadSomeUrls && null!=urlsTag )
					tmpElem.addContent( urlsTag );
				else
					tmpElem.addContent( ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT );



				/* Add the Links or Drop down actions */
				//////////////////////////////////////
				//////////////////////////////////////
				//////////////////////////////////////

				// If no user classes, just use classic Links
				if( ! hasUserClasses ) {

					// Add the Create / Edit Hyperlink
					// We don't offer this for null searches
					tmpElem = JDOMHelper.findOrCreateElementByPath(
						rowElem,		// Starting at
						// "td[" + (++colCount) + "]/@class=" + CSSClassNames.DATA_CELL,
						"td[" + (++colCount) + "]"
						+ "/@class=" + CSSClassNames.MENU_CELL
						// + "/@valign=top"
						+ "/nobr"
						,
						// + "/div/@class=",
						true	// Yes, tell us about errors
						);


					// final String kVertBar = "" + AuxIOInfo.K_NBSP + '|' + AuxIOInfo.K_NBSP;
					final String kVertBar = " | ";

					// The link text and mode (Create or Edit)

					String linkText2 = ReportConstants.EDIT_TEXT;
					String mode = nie.webui.UILink.UI_OPERATION_EDIT;
					// ReportConstants.CREATE_TEXT
					// nie.webui.UILink.UI_OPERATION_ADD
					Element linkElem2 = uiLink.generateLinkElement(inRequestObject, linkText2, mode, mapID );
					tmpElem.addContent( linkElem2 );

					tmpElem.addContent( kVertBar );

					linkText2 = ReportConstants.VIEW_TEXT;
					mode = nie.webui.UILink.UI_OPERATION_VIEW;
					linkElem2 = uiLink.generateLinkElement(inRequestObject, linkText2, mode, mapID );
					tmpElem.addContent( linkElem2 );

					tmpElem.addContent( kVertBar );

					linkText2 = ReportConstants.DELETE_TEXT;
					mode = nie.webui.UILink.UI_OPERATION_DELETE;
					linkElem2 = uiLink.generateLinkElement(inRequestObject, linkText2, mode, mapID );
					tmpElem.addContent( linkElem2 );


				}
				// Else does have user classes, so use drop down list
				else {


					// Add the Edit Hyperlinks
	
					// NOTE!!!!!
					// We need
					// <table>
					//		<tr>
					//			... other <td's>
					//			<form>
					//				<td>
					// vs <td><form>
					// in order for IE to not insert a ton of extra white space
					// and mess up the table; I know it looks really odd but
					// if you nest it the other way it looks really bad in IE
	
					boolean haveSetSelection = false;
	
					Element formElem = JDOMHelper.findOrCreateElementByPath(
						rowElem,		// Starting at
						"form[+]/@method=GET"
						,
						true	// Yes, tell us about errors
						);
						colCount++;
					String actionStr = getMainConfig().getUIRequestDispatcher().getMainUiUrl()
						+ UILink.META_REDIR_SCREEN + UILink.UI_SCREEN_EXTENSION
						;
					formElem.setAttribute( "action", actionStr );
	
					// set up hidden fields
					AuxIOInfo miscFields = new AuxIOInfo();
					// copy over misc fields from this request
					miscFields.copyInCGIFields( inRequestObject,
						ReportConstants.fMiscReportFields
						);
	
						// Add return URL
						/***
						// ^^^ NO!  We want THIS page to be the return URL, which will
						// will happen by default on the next screen, since we've cleared					// Setup where we are supposed to return them to, either after
						// any previous URL and we will be the referer!
						//
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
						else {
							debugMsg( kFName, "Setting form " + UILink.RETURN_URL_CGI_FIELD + " = " + returnURL );
							miscFields.setOrOverwriteCGIField( UILink.RETURN_URL_CGI_FIELD, returnURL );
						}
						***/
	
	
					// Add the ID we are looking at
					// miscFields.setOrOverwriteCGIField( UILink.TERM_FORMGEN_CGI_FIELD, lQueryTerm );
					miscFields.setOrOverwriteCGIField( UILink.MAP_ID_FORMGEN_CGI_FIELD, ""+mapID );
					// don't need to add term here since we're only editing by Map ID
	
					// And we want to initiate a "commit" (vs "formgen"),
					// in fact user won't see either
					miscFields.setOrOverwriteCGIField( UILink.MODE_CGI_FIELD, UILink.UI_MODE_COMMIT );
	
					// Add all these hidden fields
					miscFields.addCGIFieldsToFormElemAsHiddenFields( formElem );
	
					// Redefine the form content hanger to actually be a sub TD tag
					formElem = JDOMHelper.findOrCreateElementByPath(
						formElem,		// Starting at
						"td[+]/@class=" + CSSClassNames.COMPACT_DATA_CELL // + CSSClassNames.DATA_CELL
						,
						true	// Yes, tell us about errors
						);
						colCount++;
	
	
					// The list of options
					Element select = JDOMHelper.findOrCreateElementByPath(
						formElem,		// Starting at
						"select"
						+ "/@class=" + CSSClassNames.COMPACT_FORM_ELEMENT
						+ "/@name=" + UILink.META_CGI_FIELD
						,
						true	// Yes, tell us about errors
						);
	
					// instruction
					Element opt0 = JDOMHelper.findOrCreateElementByPath(
						select,		// Starting at
						"option[+]/@value=" + ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
						,
						true	// Yes, tell us about errors
						);
					opt0.addContent( "(choose an action)" );
	
	
					// Only offer to let them edit if there is in fact something
					// to edit
					if( hasClassicSuggestions ) {
	
						/***
						Element opt1 = JDOMHelper.findOrCreateElementByPath(
							select,		// Starting at
							"option[+]"
							+ "/@value=" + UILink.OPERATION_CGI_FIELD + '=' + UILink.UI_OPERATION_ADD
							+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							// + "/font/@size=" + selectFontSize
							,
							true	// Yes, tell us about errors
							);
						// opt1.addContent( "Immediate Redirect" );
						opt1.addContent( "Suggest a URL or Related Term" );
						if( ! hasClassicSuggestions )
							opt1.setAttribute( "selected", "TRUE" );
						***/
		
						// edit
						Element opt2 = JDOMHelper.findOrCreateElementByPath(
							select,		// Starting at
							"option[+]"
							+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_EDIT
							// + "&screen=" + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							// + "/font/@size=" + selectFontSize
							,
							true	// Yes, tell us about errors
							);
						opt2.addContent( "Edit URL and Related Terms" );
						opt2.setAttribute( "selected", "TRUE" );
						haveSetSelection = true;
		
						// view
						Element opt3 = JDOMHelper.findOrCreateElementByPath(
							select,		// Starting at
							"option[+]"
							+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_VIEW
							// + "&screen=" + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							// + "/font/@size=" + selectFontSize
							,
							true	// Yes, tell us about errors
							);
						opt3.addContent( "View URL and Related Terms details" );
		
						// delete
						Element opt4 = JDOMHelper.findOrCreateElementByPath(
							select,		// Starting at
							"option[+]"
							+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_DELETE
							// + "&screen=" + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							// + "/font/@size=" + selectFontSize
							,
							true	// Yes, tell us about errors
							);
						opt4.addContent( "Delete URL and Related Terms" );
	
					}	// End if there are classic suggestions
	
					// All other user classes!
					if( null!=allUserClassNames ) {
	
						for( Iterator uit=allUserClassNames.iterator(); uit.hasNext() ; ) {
							String className = (String) uit.next();
	
							// Only offer to edit if there is in fact something
							// to edit
							if( null!=existingAdClasses && existingAdClasses.contains(className) ) {
	
								BaseMarkup marker = getMainConfig().getUserClassDefByNameOrNull(
									className
									);
		
								if( null==marker ) {
									errorMsg( kFName,
										"Got null marker object back for user alias " + className
										);
								}
								else {
									String desc = marker.getShortDesc();
									String screen = marker.getUIScreenName();
		
									/***
									Element addOpt = JDOMHelper.findOrCreateElementByPath(
										select,		// Starting at
										"option[+]"
										+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_ADD
										// + "&screen=" + screen
										+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
										// + "/font/@size=" + selectFontSize
										,
										true	// Yes, tell us about errors
										);
									// addOpt.addContent( "Create a Text Ad" );
									addOpt.addContent( "Create a " + desc );
									***/
		
		
									// Only offer to edit if there is in fact something
									// to edit
									Element edtOpt = JDOMHelper.findOrCreateElementByPath(
										select,		// Starting at
										"option[+]"
										+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_EDIT
										// + "&screen=" + screen
										+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
										// + "/font/@size=" + selectFontSize
										,
										true	// Yes, tell us about errors
										);
									edtOpt.addContent( "Edit this " + desc );
									if( ! haveSetSelection ) {
										edtOpt.setAttribute( "selected", "TRUE" );
										haveSetSelection = true;
									}
	
									// View
									Element vwOpt = JDOMHelper.findOrCreateElementByPath(
										select,		// Starting at
										"option[+]"
										+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_VIEW
										// + "&screen=" + screen
										+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
										// + "/font/@size=" + selectFontSize
										,
										true	// Yes, tell us about errors
										);
									vwOpt.addContent( "View this " + desc );
		
									// Delete
									Element dlOpt = JDOMHelper.findOrCreateElementByPath(
										select,		// Starting at
										"option[+]"
										+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_DELETE
										// + "&screen=" + screen
										+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
										// + "/font/@size=" + selectFontSize
										,
										true	// Yes, tell us about errors
										);
									dlOpt.addContent( "Delete this " + desc + " ..." );
		
								}	// Else we DID get a non-null marker
	
							}	// End if there are user classes associated with this map
	
						}	// end for each user defined class
	
					}	// Done with user classes
	
					// The submit button
					Element button = JDOMHelper.findOrCreateElementByPath(
						formElem,		// Starting at
						// "font[+]/@size=1/input/@type=submit/@value=Go >>"
						"input[+]/@type=submit/@value=Go >>"
						+ "/@class=" + CSSClassNames.COMPACT_FORM_ELEMENT
						,
						true	// Yes, tell us about errors
						);



				}	// End Else they DO have user classes


				// mainContentTable.addContent(rowElem);
				resultsTableElement.addContent(rowElem);

				// Update our displayed statistics

				// If this is the first actual row displayed
				// then remember it
				if( actualFirstRowDisplayed < 0 )
					actualFirstRowDisplayed = rowCount;

				// This is always the last one we've displayed
				actualLastRowDisplayed = rowCount;

			}	// End for each record

			debugMsg( kFName, "Completed Loop." );


			if( displayedRowCount < 1 ) {
				addNoRecordsMsg( resultsTableElement,
					lReportColumnCount, null
					);
				/***
				Element tmpElem = JDOMHelper.findOrCreateElementByPath(
					resultsTableElement,
					"tr[+]/@class=" + CSSClassNames.ODD_ROW					+ "/td/@align=center"
					+ "/@class=" + CSSClassNames.DATA_CELL
					+ "/@colspan=" + lReportColumnCount
					+ "/small/i"
					,
					true	// Yes, tell us about errors
					);
				if( null!=tmpElem )
					tmpElem.addContent( "- - (no matching Directed Results Maps to display) - -" );
				***/
			}

			// Add a spacer
			Element blankRowElem = new Element( "tr" );
			Element blankCell = JDOMHelper.findOrCreateElementByPath(
				blankRowElem,
				"td/@colspan=3/@class=" + CSSClassNames.SPACER_CELL,
				true
				);
			resultsTableElement.addContent( blankRowElem );

			// Add the CREATE a new map section

			// Add the anoter header row
			Element spacerRowElem = new Element( "tr" );
			spacerRowElem.setAttribute( "class", CSSClassNames.HEADER_ROW );
			Element th2 = JDOMHelper.findOrCreateElementByPath(
				spacerRowElem,
				"th[1]/@colspan=3/@class=" + CSSClassNames.HEADER_CELL,
				true
				);
			th2.addContent("Choose a Map from above or Create a new one:");
			resultsTableElement.addContent( spacerRowElem );

			// We add an extra row for the create new link
			// Create the results row
			Element rowElem = new Element( "tr" );
			// if( (displayedRowCount+1) % 2 == 0 )
			//	rowElem.setAttribute( "class", CSSClassNames.EVEN_ROW );
			// else
				rowElem.setAttribute( "class", CSSClassNames.ODD_ROW );

			// Add the first column, the term
			int colCount = 0;
			Element tmpElem = JDOMHelper.findOrCreateElementByPath(
				rowElem,		// Starting at
				"td[" + (++colCount) + "]/@class=" + CSSClassNames.DATA_CELL,
				true	// Yes, tell us about errors
				);
			// Cleanup the term if it has wildcards in it!
			if( termHadWildcards && null!=term ) {
				term = NIEUtil.replaceChars( term, '%', ' ' );
				term = NIEUtil.trimmedStringOrNull( term );
			}
			if( null!=term )
				tmpElem.addContent( term );
			else
				tmpElem.addContent( ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT );

			// The second colunn
			// Instead of URL's, we display a message
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				rowElem,		// Starting at
				"td[" + (++colCount) + "]/@class=" + CSSClassNames.DATA_CELL,
				true	// Yes, tell us about errors
				);
			tmpElem.addContent(
				"Create a NEW Directed Results Map"
				);




			// Third column, the link
			/* Add the Drop down actions */
			//////////////////////////////////////
			//////////////////////////////////////
			//////////////////////////////////////


			// If no user classes, just use CLASSIC Links
			if( ! hasUserClasses ) {

				// Add the Create / Edit Hyperlink
				// We don't offer this for null searches
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@align=center/@class=" + CSSClassNames.DATA_CELL
					+ "/nobr"
					,
					true	// Yes, tell us about errors
					);

				// The link text and mode (Create or Edit)
				// String linkText2 = ReportConstants.EDIT_TEXT;
				// String mode = nie.webui.UILink.UI_OPERATION_EDIT;
				String linkText2 = ReportConstants.CREATE_TEXT_FANCY;
				String mode = nie.webui.UILink.UI_OPERATION_ADD;
				// Create the link
				Element linkElem2 = uiLink2.generateLinkElement(
					inRequestObject, linkText2, mode, term
					);
				tmpElem.addContent( linkElem2 );

			}
			// Else do have user classes, so need dropdown
			else {


				// NOTE!!!!!
				// We need
				// <table>
				//		<tr>
				//			... other <td's>
				//			<form>
				//				<td>
				// vs <td><form>
				// in order for IE to not insert a ton of extra white space
				// and mess up the table; I know it looks really odd but
				// if you nest it the other way it looks really bad in IE
	
				Element formElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"form[+]/@method=GET"
					,
					true	// Yes, tell us about errors
					);
					colCount++;
				String actionStr = getMainConfig().getUIRequestDispatcher().getMainUiUrl()
					+ UILink.META_REDIR_SCREEN + UILink.UI_SCREEN_EXTENSION
					;
				formElem.setAttribute( "action", actionStr );
	
				// set up hidden fields
				AuxIOInfo miscFields = new AuxIOInfo();
				// copy over misc fields from this request
				miscFields.copyInCGIFields( inRequestObject, ReportConstants.fMiscReportFields );
	
					// Add return URL
					/***
					// ^^^ NO!  We want THIS page to be the return URL, which will
					// will happen by default on the next screen, since we've cleared					// Setup where we are supposed to return them to, either after
					// any previous URL and we will be the referer!
					//
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
					else {
						debugMsg( kFName, "Setting form " + UILink.RETURN_URL_CGI_FIELD + " = " + returnURL );
						miscFields.setOrOverwriteCGIField( UILink.RETURN_URL_CGI_FIELD, returnURL );
					}
					***/
	
	
				// Add the ID we are looking at
				// -1 because these are all to create NEW links
				miscFields.setOrOverwriteCGIField( UILink.MAP_ID_FORMGEN_CGI_FIELD, "-1" );
	
				// Add the TERM we are looking at, if any
				if( null!=term )
					miscFields.setOrOverwriteCGIField( UILink.TERM_FORMGEN_CGI_FIELD, term );
	
	
				// And we want to initiate a "commit" (vs "formgen"),
				// in fact user won't see either
				miscFields.setOrOverwriteCGIField( UILink.MODE_CGI_FIELD, UILink.UI_MODE_COMMIT );
	
				// Add all these hidden fields
				miscFields.addCGIFieldsToFormElemAsHiddenFields( formElem );
	
				// Redefine the form content hanger to actually be a sub TD tag
				formElem = JDOMHelper.findOrCreateElementByPath(
					formElem,		// Starting at
					"td[+]/@class=" + CSSClassNames.COMPACT_DATA_CELL // + CSSClassNames.DATA_CELL
					,
					true	// Yes, tell us about errors
					);
					colCount++;
	
	
				// The list of options
				Element select = JDOMHelper.findOrCreateElementByPath(
					formElem,		// Starting at
					"select"
					+ "/@class=" + CSSClassNames.COMPACT_FORM_ELEMENT
					+ "/@name=" + UILink.META_CGI_FIELD
					,
					true	// Yes, tell us about errors
					);
	
				// instruction
				Element opt0 = JDOMHelper.findOrCreateElementByPath(
					select,		// Starting at
					"option[+]/@value=" + ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
					// + "/font/@size=" + selectFontSize
					,
					true	// Yes, tell us about errors
					);
				opt0.addContent( "(choose an action)" );
	
				Element opt1 = JDOMHelper.findOrCreateElementByPath(
					select,		// Starting at
					"option[+]"
					+ "/@value=" + UILink.OPERATION_CGI_FIELD + '=' + UILink.UI_OPERATION_ADD
					+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
					,
					true	// Yes, tell us about errors
					);
				// opt1.addContent( "Immediate Redirect" );
				opt1.addContent( "Suggest a URL or Related Term" );
				opt1.setAttribute( "selected", "TRUE" );
		
	
				// All other user classes!
				if( null!=allUserClassNames ) {
	
					for( Iterator uit=allUserClassNames.iterator(); uit.hasNext() ; ) {
						String className = (String) uit.next();
	
						BaseMarkup marker = getMainConfig().getUserClassDefByNameOrNull(
							className
							);
	
						if( null==marker ) {
							errorMsg( kFName,
								"Got null marker object back for user alias " + className
								);
						}
						else {
							String desc = marker.getShortDesc();
							String screen = marker.getUIScreenName();
	
							Element addOpt = JDOMHelper.findOrCreateElementByPath(
								select,		// Starting at
								"option[+]"
								+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_ADD
								// + "&screen=" + screen
								+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
								// + "/font/@size=" + selectFontSize
								,
								true	// Yes, tell us about errors
								);
							// addOpt.addContent( "Create a Text Ad" );
							addOpt.addContent( "Create a " + desc );
	
	
						}	// Else we DID get a non-null marker
	
					}	// end for each user defined class
	
				}	// Done with user classes
	
	
				// The submit button
				Element button = JDOMHelper.findOrCreateElementByPath(
					formElem,		// Starting at
					"input[+]/@type=submit/@value=Go >>"
					+ "/@class=" + CSSClassNames.COMPACT_FORM_ELEMENT
					,
					true	// Yes, tell us about errors
					);


			}	// End Else there were custom user classes, needed dropdown





			// mainContentTable.addContent(rowElem);
			resultsTableElement.addContent(rowElem);

			// Paging Links statistics
			////////////////////////////////////////
			Element pagingLinksRows = null;
			boolean hasMoreRecords = false;	// arbitrary initial value
			boolean hasPrevRecords = false;	// arbitrary initial value
			boolean isShowingAllRecords = false;
			if( isDoingPaging )
			{

				// Todo: Could actually figure out number
				// of records for prev and next links
				// overkill at this point

				// First, figure out if there are more records
				hasMoreRecords = false;	// arbitrary initial value
				if( atEndOfRecords )
				{
					hasMoreRecords = false;	// redundant but clear
				}
				// Else we're not specifically at the end of records
				// We need to try fetching one more record
				else
				{
					// If there is another record, then we have more
					if( results.next() )
						hasMoreRecords = true;
					// OK, we specifically checked and there are no more
					else
						hasMoreRecords = false;
				}

				// There are prev records if we're not starting
				// at record 1
				hasPrevRecords = (actualFirstRowDisplayed > 1);

				// tell us whether we're showing all rows
				if( ! hasPrevRecords && ! hasMoreRecords )
					isShowingAllRecords = true;

				// Add the paging links
				// We do this at the bottom

			}
			// Else we're NOT doing paging
			else
			{
				// Not much to do here

				// we will assume all rows are shown
				isShowingAllRecords = true;
			}

			// Create and add the "stats" message
			// showing records n of m
			// and the "Return to the main menu" link
			////////////////////////////////////////////////////
			addStatusRowAndMenuLink( mainContentTable,
				inRequestObject, inResponseObject,
				displayedRowCount, desiredStartRow,
				actualFirstRowDisplayed, actualLastRowDisplayed,
				isShowingAllRecords
				);



			// Then add the results table to the content table
			///////////////////////////////////////////////////////
			Element recordsTableRowElem = new Element( "tr" );
			// recordsTableRowElem.setAttribute( "class", "nie_..." );
			// Add this row to the content
			mainContentTable.addContent( recordsTableRowElem );
			// Now create the cell for the message
			Element recordsTableCellElem = new Element( "td" );
			recordsTableCellElem.setAttribute( "align", "center" );
			recordsTableCellElem.setAttribute( "class",
				CSSClassNames.CONTAINER_CELL
				);
			// Add the cell to the row
			recordsTableRowElem.addContent( recordsTableCellElem );
			// Now add the resutls table to the holding cell
			recordsTableCellElem.addContent( resultsTableElement );


			// No statistics in this report


			// Now add a horizontal rule at the bottom of the table
			addHR( resultsTableElement, lReportColumnCount );

			// Then add the paging stuff, if any
			//////////////////////////////////////////////////
			addPagingLinksIfNeeded( resultsTableElement,
			// addPagingLinksIfNeeded( mainContentTable,
				inRequestObject, inResponseObject,
				isDoingPaging, isShowingAllRecords,
				hasPrevRecords, hasMoreRecords, 
				actualFirstRowDisplayed, actualLastRowDisplayed,
				desiredRowCount,
				lReportColumnCount
				);


		// } catch (SQLException e) {
		} catch (Exception e) {
			// e.printStackTrace( System.err );
			throw new ReportException( kExTag
				+ "Error running report."
				+ " Report = \"" + getReportName() + "\""
				+ " SQL Query =\"" + qry + "\""
				+ " Error: " + e
				);
		}
		finally {
			// quickie cleanup!
			results = DBConfig.closeResults( results, kClassName(), kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName(), kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName(), kFName, false );
		}

		return outElem;
	}

	private static final List urlChopper( String inHref, int inChopLen ) {
		List outList = new Vector();
		while( null != inHref ) {
			String part = inHref.substring( 0, Math.min(inChopLen, inHref.length()) );
			outList.add( part );
			if( inHref.length() > inChopLen )
				inHref = inHref.substring( inChopLen );
			else
				inHref = null;
		}
		return outList;
	}

	Element generateHyperlinkedSearchTerm(
		AuxIOInfo ioRequestObject,
		String inTerm, boolean inWasNull,
		String optCSSClass, String optNDaysStr
	) {
		final String kFName = "generateHyperlinkedSearchTerm";

		// Start building the output element
		Element outElem = new Element( "td" );
		outElem.setAttribute(
			"class",
			( null!=optCSSClass ? optCSSClass : CSSClassNames.DATA_CELL )
			);

		// Create a report link XML Element to pass
		// to it's contructor
		Element constructorElem = new Element(
			ReportLink.MAIN_ELEM_NAME
			);

		// Set some attributes that we need
		constructorElem.setAttribute(
			ReportLink.LINK_TEXT_PATH, inTerm
			);

		// Add the actual TEXT for this field
		Element linkTextElem =
			JDOMHelper.findOrCreateElementByPath(
				constructorElem,
				ReportLink.LINK_TEXT_PATH,
				true
				);
		linkTextElem.addContent( inTerm );

		// Create a parameter node and add it
		Element parmElem = new Element( ReportLink.PARM_PATH );
		constructorElem.addContent( parmElem );

		// Figure out which parameter name to use
		// If not null, use it, otherwise, system null
		String parmName = "search";
		parmElem.setAttribute(
			ReportLink.PARM_NAME_ATTR, parmName
			);

		// TODO: set subtitle, not currently supported by ReportLink
		// ReportLink.TITLE_PATH


		// Figure out which value to use
		String parmValue = ! inWasNull
			? inTerm : ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE
			;
		parmElem.setAttribute(
			ReportLink.PARM_VALUE_ATTR, parmValue
			);

		// The report name to use
		String lDestReport = "search_details_ndays";

		// Construct the link
		try
		{

			ioRequestObject.setOrOverwriteCGIField( "days",
				( null!=optNDaysStr ? optNDaysStr : "365" )
				);
			// Todo: ??? pass in values to clear

			// Create the object
			ReportLink link = new ReportLink(
				// constructorElem, getMainApp()
				constructorElem, getMainConfig()
				);
			// Request the anchor element
			Element anchor = link.generateRichLink(
				ioRequestObject, lDestReport, parmName, false
				);

			// Add it to the answer
			outElem.addContent( anchor );
		}
		catch( Exception e )
		{
			errorMsg( kFName,
				"Error preparing report link for search term."
				+ " Will add UNlinked text instead."
				+ " Error: " + e
				);
			outElem.addContent( inTerm );
		}

		return outElem;
	}



	public Element generateMenuLinksToThisReport( AuxIOInfo inRequest )
	{
		final int kIndentPixels = 30; // 20

		Element outElem = new Element( "table" );
		// outElem.setAttribute( "border", "1" );
		outElem.setAttribute( "cellpadding", "0" );
		outElem.setAttribute( "cellspacing", "0" );

		// for debug
		// outElem.setAttribute( "border", "1" );


			// Put the header row up
			Element titleElem = JDOMHelper.findOrCreateElementByPath(
				outElem,
				// "tr/th/@colspan=2/@align=left"
				"tr/td/@colspan=2/@align=left"
				+ "/@class=" + CSSClassNames.CONTAINER_CELL
				+ "/div/@class="
				// + CSSClassNames.INACTIVE_RPT_LINK
				+ CSSClassNames.INACTIVE_MENU_LINK
				,
				true
				);
			titleElem.addContent( getLinkText(null) + ReportConstants.SUGGESTED_MULTI_ITEM_MENU_SUFFIX );

			// For each link
			// for( int i=0; i<links.size(); i++ )
			for( int i=0; i<kReportDays.length; i++ )
			{
				// Get the link
				// ReportLink link = (ReportLink) links.get( i );
				String daysStr = kReportDays[i];

				// Create and link the row
				Element row = JDOMHelper.findOrCreateElementByPath(
					outElem, "tr[" + (i+2) + ']', true
					);

				// Add an indent cell, we don't need to save this
				JDOMHelper.findOrCreateElementByPath(
					row,
					"td/@class=" + CSSClassNames.CONTAINER_CELL
					+ "/@valign=top"
					+ "/img/@height=0/@width=" + kIndentPixels
					,
					true
					);

				// Add a cell on this row for the link
				Element linkCell = JDOMHelper.findOrCreateElementByPath(
					row,
					"td[2]/@class=" + CSSClassNames.CONTAINER_CELL
					+ "/@valign=top"
					+ "/@width=100%"
					,
					true
					);
				// Get the link and add the link to it
				// Element linkElem = link.generateRichLink(
				//	inRequest, getReportName(), null, true
				//	);
				Element linkElem = generateSublink( inRequest, daysStr );

				linkCell.addContent( linkElem );
			}

		// And we're done!
		return outElem;

	}









	Element generateSublink( AuxIOInfo inRequestObject, String inDays )
	{
		// Create a new anchor tag
		Element anchor = new Element( "a" );
		anchor.setAttribute(
			"class",
			CSSClassNames.ACTIVE_MENU_LINK
			);

		String linkText = null;
		if( inDays.equals("1") )
			linkText = "day";
		else if( inDays.equals("7") )
			linkText = "week";
		else if( inDays.equals("30") )
			linkText = "month";
		else if( inDays.equals("90") )
			linkText = "quarter";
		else if( inDays.equals("365") )
			linkText = "year";
		else
			linkText = inDays + " days";

		// Add the link text
		anchor.addContent( linkText );

		// Create a repository for link info
		AuxIOInfo linkInfo = new AuxIOInfo();
		linkInfo.setBasicURL( getMainAppURL() );
		linkInfo.copyInCGIFields( inRequestObject );

		// Set the report name to us
		linkInfo.setOrOverwriteCGIField(
			ReportConstants.REPORT_NAME_CGI_FIELD,
			kClassName()
			);

		linkInfo.setOrOverwriteCGIField(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME, inDays
			);

		// Get the full URL and add it to the anchor
		String href = linkInfo.getFullCGIEncodedURL();
		anchor.setAttribute( "href", href );

		return anchor;
	}

	boolean getIsADefinedTerm( String inTerm ) {
		final String kFName = "getIsADefinedTerm";
		inTerm = NIEUtil.trimmedLowerStringOrNull( inTerm );
		inTerm = NIEUtil.sqlEscapeString( inTerm, true );
		if( null==inTerm ) {
			errorMsg( kFName, "Null/empty input term passed in." );
			return false;
		}
		String qry = "SELECT count(*)"
			+ " FROM nie_term t, nie_map_term_assoc mta"
			+ " WHERE t.id = mta.term_id"
			+ " AND t.text_normalized ='" + inTerm + "'"
			;
		int howMany = getDBConfig().simpleCountQuery( qry, true, true );
		return howMany > 0;
	}

	public String getTitle( Hashtable inVars )
	{
		// return "Most Popular Searches";
		return "Manage Directed Results Mappings";
	}
	public String getSubtitleOrNull( Hashtable inHash ) {
		return cSubtitleB;
	}

	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}

	String cSubtitleB;

	static final String TREND_BOX_SHADE_COLOR = "#bfd2e3";

	static final int DISPLAY_QUERY_TRUNCATION_LENGTH = 22;

	static final int DEFAULT_INTERVAL = 180;
	static final int DEFAULT_HOW_MANY_ROWS = 10;

	static final int DEFAULT_ICON_WIDTH = 16;
	static final int DEFAULT_ICON_HEIGHT = 16;

	// static final String DEFAULT_IMAGE_URL_PREFIX = "http://foo/";
	// static final String _DEFAULT_IMAGE_URL_PREFIX = "/files/images/sr2/";
	// ^^^ Look in ReportConstants now if you need this

	// The list of link choices we will offer
	static final String [] kReportDays = { "1", "7", "30" };;




}

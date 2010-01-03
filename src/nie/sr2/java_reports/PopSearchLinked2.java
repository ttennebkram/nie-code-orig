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
import nie.sn.BaseMarkup;
import nie.sn.SnURLRecord;
import nie.webui.UILink;


/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PopSearchLinked2 extends BaseReport
{
	public String kClassName()
	{
		return "PopSearchLinked2";
	}

	public int getRequiredAccessLevel() {
		return ACCESS_LEVEL;
	}
	// We need a static variable for easy checking elsewhere
	public static final int ACCESS_LEVEL = ReportConstants.DEFAULT_WRITEABLE_ACCESS_LEVEL;


	public PopSearchLinked2(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortReportName
		)
			throws ReportConfigException
	{
		// super( inMainApp, inShortReportName );
		super( inMainConfig, inShortReportName );
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

		// int interval = 30;
		// int interval = 180;
		// int howMany = 10;

		int interval = DEFAULT_INTERVAL;
		int howMany = DEFAULT_HOW_MANY_ROWS;

		// int iconImgWidth = DEFAULT_ICON_WIDTH;
		// int iconImgHeight = DEFAULT_ICON_HEIGHT;

		// String lGraphicURLPrefix = DEFAULT_IMAGE_URL_PREFIX;
		// todo: obviously update this ^^^

		// We'll use this hash for varaible substitution in Strings
		Hashtable lMasterValuesHash = getVariables( inRequestObject );

		// See if we have a subtitle
		String ndays = inRequestObject.getScalarCGIFieldTrimOrNull(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME
			);
		if( null!=ndays ) {
			cSubtitleB = "For the past " + ndays + " day(s)";
		}
		else
			cSubtitleB = null;
		debugMsg( kFName, "cSubtitleB=\"" + cSubtitleB + "\"" );

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


		//
		// Prepare Column Headings
		// =================================
		//
		///////////////////////////////////////////////

		// Add the header row
		Element headerRowElem = new Element( "tr" );
		headerRowElem.setAttribute( "class", CSSClassNames.HEADER_ROW );
		String thClass = CSSClassNames.HEADER_CELL;

		// And each field
		// terms
		Element th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[1]/@class=" + thClass,
			true
			);
		th.addContent("Search Term(s)");

		// # of searches
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[2]/@class=" + thClass,
			true
			);
		th.addContent("Count");

		// % of total searches
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[3]/@class=" + thClass,
			true
			);
		th.addContent("% of Total");

		// Pages found
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[4]/@colspan=2/@class=" + thClass,
			// "th[4]/@colspan=1/@class=" + thClass,
			true
			);
		th.addContent("Pages Found");


		// Status
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[5]/@class=" + thClass,
			true
			);
		th.addContent("Status");


		// Action
		th = JDOMHelper.findOrCreateElementByPath(
			headerRowElem,
			"th[6]/@class=" + thClass,
			true
			);
		// th.addContent("Action");
		th.addContent("Take Action");

		// Add this header row to the table
		// mainContentTable.addContent(headerRowElem);
		resultsTableElement.addContent(headerRowElem);

		// We need to know how many display colunns
		// Keep in mind on of our headers was colspan=2
		int lReportColumnCount = 7;
		// temp removing spanning colunn	
		// int lReportColumnCount = 6;	


		// Now we start working on the query
		//////////////////////////////////////////////////

		// Query 1: Get the total number of queries
		String qry =
			"SELECT count(*)"
			+ " FROM " + getDefaultTableName()
			+ " WHERE transaction_type = 1"
			;
		// ndays calculated near top now
		if( null!=ndays ) {
			// qry += " AND start_time >= " + getDBConfig().getVendorSysdateString() + " - " + ndays; 
			int tmpDays = NIEUtil.stringToIntOrDefaultValue( ndays, 0, true, true );
			if( tmpDays > 0 ) {
				String fullStartStr = getDBConfig().calculateDateTimeStringForNDaysPast( tmpDays, false, true );
				if( null!=fullStartStr )
					qry += " AND start_time >= " + fullStartStr;
				else
					errorMsg( kFName, "Null formatted date string returned; no date filter will be applied." );
			}
			else
				errorMsg( kFName, "Invalid date offset from \"" + ndays + "\"; no date filter will be applied." );
		}

		// Get a simple count
		int totalSearches = getDBConfig().simpleCountQuery( qry, true, true );

		// Now get the main results

		// Basic query
		qry =
			"SELECT"
			+ " NORMALIZED_QUERY " + getDBConfig().getVendorAliasString() + " search"
			+ ", count(*) " + getDBConfig().getVendorAliasString() + " num_times"
			+ ", max(NUM_RESULTS) " + getDBConfig().getVendorAliasString() + " num_found"
			+ ", max(was_search_names_term) " + getDBConfig().getVendorAliasString() + " is_mapped"
			+ "	FROM " + getDefaultTableName()
			+ " WHERE transaction_type = 1"
			;
		// add the filter
		if( null!=ndays ) {
			// qry += " AND start_time >= " + getDBConfig().getVendorSysdateString() + " - " + ndays; 
			int tmpDays = NIEUtil.stringToIntOrDefaultValue( ndays, 0, true, true );
			if( tmpDays > 0 ) {
				String fullStartStr = getDBConfig().calculateDateTimeStringForNDaysPast( tmpDays, false, true );
				if( null!=fullStartStr )
					qry += " AND start_time >= " + fullStartStr;
				else
					errorMsg( kFName, "Null formatted date string returned; no date filter will be applied." );
			}
			else
				errorMsg( kFName, "Invalid date offset from \"" + ndays + "\"; no date filter will be applied." );
		}

		// grouping and sorting
		qry += " GROUP BY NORMALIZED_QUERY"
			+ " ORDER BY count(*) DESC"
			+ ", max(start_time) DESC"
			;

		debugMsg( kFName, "Will run SQL=" + qry );


		// What are the user defined classes that we know about
		// Set _userClasses = getMainConfig().getUserClassesHashMap().keySet();
		Set allUserClassNames = getMainConfig().getAllUserClassNames();

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
			/***
			nie.webui.UILink uiLink = null;
			try {
				uiLink = new nie.webui.UILink(
					getMainConfig(),
					// "CreateMapForm",							// String Screen Name
					UILink.CLASSIC_CREATE_MAP_UI_SCREEN,	// String Screen Name
					UILink.TERM_FORMGEN_CGI_FIELD,			// String inParmCGIName,
					"Set Directed Results for this Term",	// String optLinkTitleText
					null
					);
			}
			catch( nie.webui.UIConfigException e ) {
				throw new ReportException( kExTag
					+ "Error creating UI linking object: " + e
					);
			}
			***/

			// Links to the search engine for a test drive
			// Don't need try/catch because it also throws ReportException
			SearchEngineLink searchLink = new SearchEngineLink(
				getMainConfig(),
				"_blank",										// String optWindowTarget,
				"Run this search now (in a new window)",	// String optLinkTitleText,
				null,											// String optCssClass
				shouldRouteSearchLinksViaOurProxy()
				);


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

				// We need to know about mapped terms
				// (but now we get this from the term tables, not the log table)
				// boolean isMapped = false;
				boolean oldLog_isMapped = false;
				Object tmpObj = results.getObject( "is_mapped" );
				if( null!=tmpObj ) {
					Double tmpDouble = NIEUtil.objectToDoubleOrNull( tmpObj, true, true );
					if( null!=tmpDouble && tmpDouble.intValue() > 0 )
						// isMapped = true;
						oldLog_isMapped = true;
				}


				// New is_mapped is based on the term itself
				String lQueryTerm = results.getString( "search" );
				lQueryTerm = NIEUtil.trimmedStringOrNull( lQueryTerm );

				// boolean isMapped = (null==lQueryTerm) ? false : getIsADefinedTerm( lQueryTerm );
				// Are there ANY maps for this term?
				boolean isMapped = (null==lQueryTerm) ? false :
					DbMapRecord.static_isADefinedTerm(
						getMainConfig(), lQueryTerm
						);
				// Do we have classic Web Master Suggests or Alt Terms to suggest for this term?
				boolean hasClassicSuggestions = (null==lQueryTerm) ? false :
					DbMapRecord.static_hasWmsOrAltTermsForTerm(
						getMainConfig(), lQueryTerm
						);
				// all the user classes currently in use for this term
				Set existingAdClasses = (null==lQueryTerm) ? null :
					DbMapRecord.static_getUserDataClassNamesForTerm(
						getMainConfig(), lQueryTerm
						);
				debugMsg( kFName, "classes for term \"" + lQueryTerm + "\" = " + existingAdClasses );


				// Add the Search Term(s) column
				// =====================================
				boolean isNullSearch = false;
				// String lQueryTerm = results.getString( "search" );
				// lQueryTerm = NIEUtil.trimmedStringOrNull( lQueryTerm );
				// ^^^ moved up in code to earlier spot
				String lQueryDisplay = lQueryTerm;
				if( null==lQueryDisplay ) {
					// lQueryDisplay = XMLDefinedField.DEFAULT_NULL_VALUE_CONSTANT;
					lQueryDisplay = ReportConstants.SUGGESTED_NULL_SEARCH_TEXT;
					isNullSearch = true;
					// If we use terms in a link later, we'll need this
					lQueryTerm = ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE;
				}
				// if( lQueryDisplay.length() > DISPLAY_QUERY_TRUNCATION_LENGTH )
				//	lQueryDisplay = lQueryDisplay.substring( 0, DISPLAY_QUERY_TRUNCATION_LENGTH ) + "...";

				// We display previusly mapped terms differently than unmapped
				String termCSSClass = isMapped ?
					CSSClassNames.SPECIAL_DATA_CELL
					: CSSClassNames.DATA_CELL
					;

				int colCount = 0;

				// We will generate a hyperlink
				// !!! This will already be inside of a <td> tag
				Element tmpElem = generateHyperlinkedSearchTerm(
					inRequestObject,
					lQueryDisplay, isNullSearch,
					termCSSClass, ndays
					);
				colCount++;
				rowElem.addContent( tmpElem );
				/***
				// Create the first cell and add the query term to it
				Element tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@class=" + termCSSClass,
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( lQueryDisplay );
				***/


				// Create the 2nd cell and add the number of times it was searched for
				int numTimes = results.getInt( "num_times" );
				String numStr = XMLDefinedField.formatNumericTypeToString_static(
					new Double(numTimes),				// Numerical object
					true,									// boolean inComplainOnError,
					XMLDefinedField.FORMAT_AS_NUMBER,	// String optExplicitFormat,
					-1, -1									// int optMinDec, int optMaxDec
					);
				runningNumSearchesTotal += numTimes;
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@class=" + CSSClassNames.NUMERIC_CELL,
					// "td[" + (++colCount) + "]/@class=" + CSSClassNames.NUMERIC_CELL + "/div",
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( numStr );


				// 3rd cell percent of total
				// Calculate it
				double percent = 0.0;
				if( totalSearches > 0 )
					percent = (double)numTimes / (double)totalSearches;
				// Format it
				String percentStr = XMLDefinedField.formatNumericTypeToString_static(
					new Double(percent),					// Numerical object
					true,									// boolean inComplainOnError,
					XMLDefinedField.FORMAT_AS_PERCENT,	// String optExplicitFormat,
					2, 2									// int optMinDec, int optMaxDec
					);
				// Add the cell
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					// "td[" + (++colCount) + "]/@class=nie_percentage",
					"td[" + (++colCount) + "]/@class=" + CSSClassNames.PERCENTAGE_CELL,
					true	// Yes, tell us about errors
					);
				// Add the formatted answer
				if( null!=percentStr )
					tmpElem.addContent( percentStr );


				// the number of documents that were found
				int numFound = results.getInt( "num_found" );
				Object obj = results.getObject( "num_found" );
				traceMsg( kFName,
					"num_found=" + numFound
					+ ", num_found obj=" + obj
					);
				String numStr2 = XMLDefinedField.formatNumericTypeToString_static(
					new Double(numFound),				// Numerical object
					true,									// boolean inComplainOnError,
					XMLDefinedField.FORMAT_AS_NUMBER,	// String optExplicitFormat,
					-1, -1									// int optMinDec, int optMaxDec
					);
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@class=" + CSSClassNames.NUMERIC_CELL,
					true	// Yes, tell us about errors
					);
				if( null!=obj )
					tmpElem.addContent( "" + numStr2 );
				else
					tmpElem.addContent( ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT );


				// the Show Search Results link
				// ============================================
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@class=" + CSSClassNames.DATA_CELL,
					true	// Yes, tell us about errors
					);

				// The link text and mode (Create or Edit)
				String linkText = ReportConstants.SHOW_RESULTS_TEXT;
				// Create the link
				// Element linkElem = uiLink.generateLinkElement(
				//	inRequestObject, linkText, mode, lQueryTerm
				//	);
				Element linkElem = searchLink.generateLinkElement(
					inRequestObject, linkText, lQueryTerm
					);
				tmpElem.addContent( linkElem );
				/***																																								
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@class=" + XMLDefinedField.DEFAULT_TD_CSS_CLASS
					+ "/a/@class=nie_report_link/@target=_blank/@href=foo"
					,
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( "Show results >>" );
				***/


				// Figure out which graphic to use
				String lGraphicName = null;
				String lGraphicDescription = null;
				if( isMapped ) {
					lGraphicName = ReportConstants.STATUS_ICON_IS_MAPPED;
					lGraphicDescription = ReportConstants.STATUS_ICON_IS_MAPPED_MSG;
				}
				else {
					if( numFound > 0 ) {
						lGraphicName = ReportConstants.STATUS_ICON_NOT_MAPPED;
						lGraphicDescription = ReportConstants.STATUS_ICON_NOT_MAPPED_MSG;
					} else {
						lGraphicName = ReportConstants.STATUS_ICON_NO_HITS;
						lGraphicDescription = ReportConstants.STATUS_ICON_NO_HITS_MSG;
					}
				}
				// Fix up the query and image strings
				lGraphicName = ReportConstants.IMAGE_URL_PREFIX + lGraphicName;

				// Add the status image
				Element imgElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[" + (++colCount) + "]/@align=center/img/@alt=" + lGraphicDescription
					+ "/@width=" + ReportConstants.STATUS_ICON_WIDTH
					+ "/@height=" + ReportConstants.STATUS_ICON_HEIGHT,
					true	// Yes, tell us about errors
					);
				// Must add src= separately since that URL has slashes!
				imgElem.setAttribute( "src", lGraphicName );



				/* Add the Drop down actions */
				//////////////////////////////////////
				//////////////////////////////////////
				//////////////////////////////////////



				// Add the Create / Edit Hyperlink
				// We don't offer this for null searches
				if( ! isNullSearch /*&& false*/ ) {

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

					/***
					Element formElem = JDOMHelper.findOrCreateElementByPath(
						rowElem,		// Starting at
						// "form[+]/td[" + (++colCount) + "]/@class=" + CSSClassNames.DATA_CELL
						"form[+]/td[+]/@class=" + CSSClassNames.COMPACT_DATA_CELL // + CSSClassNames.DATA_CELL
						,
						true	// Yes, tell us about errors
						);
						colCount++;
					***/

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


					// Add the TERM we are looking at
					miscFields.setOrOverwriteCGIField( UILink.TERM_FORMGEN_CGI_FIELD, lQueryTerm );
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
						// "font[+]/@size=-3/select/@name=meta_field"
						"select"
						+ "/@class=" + CSSClassNames.COMPACT_FORM_ELEMENT						+ "/@name=" + UILink.META_CGI_FIELD
						,
						true	// Yes, tell us about errors
						);

					String _selectFontSize = "10";

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
						"option[+]"						+ "/@value=" + UILink.OPERATION_CGI_FIELD + '=' + UILink.UI_OPERATION_ADD
						+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
						// + "/font/@size=" + selectFontSize
						,
						true	// Yes, tell us about errors
						);
					// opt1.addContent( "Immediate Redirect" );
					opt1.addContent( "Suggest a URL or Related Term" );
					if( ! hasClassicSuggestions )
						opt1.setAttribute( "selected", "TRUE" );

					// Only offer to let them edit if there is in fact something
					// to edit
					if( hasClassicSuggestions ) {
						Element opt2 = JDOMHelper.findOrCreateElementByPath(
							select,		// Starting at
							"option[+]"							+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_EDIT							// + "&screen=" + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + UILink.CLASSIC_CREATE_MAP_UI_SCREEN
							// + "/font/@size=" + selectFontSize
							,
							true	// Yes, tell us about errors
							);
						opt2.addContent( "Edit URLs and Related Terms" );
						opt2.setAttribute( "selected", "TRUE" );
					}


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
									"option[+]"									+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_ADD
									// + "&screen=" + screen
									+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
									// + "/font/@size=" + selectFontSize
									,
									true	// Yes, tell us about errors
									);
								// addOpt.addContent( "Create a Text Ad" );
								addOpt.addContent( "Create a " + desc );
	
								// Only offer to edit if there is in fact something
								// to edit
								if( existingAdClasses.contains(className) ) {
									Element edtOpt = JDOMHelper.findOrCreateElementByPath(
										select,		// Starting at
										"option[+]"										+ "/@value=" + UILink.OPERATION_CGI_FIELD + "=" + UILink.UI_OPERATION_EDIT
										// + "&screen=" + screen
										+ '&' + UILink.META_CGI_FIELD_SCREEN_PARM + '=' + screen
										// + "/font/@size=" + selectFontSize
										,
										true	// Yes, tell us about errors
										);
									edtOpt.addContent( "Edit a " + desc );
								}
							}


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


					/***
					// The link text and mode (Create or Edit)
					String linkText2 = isMapped ?
						ReportConstants.EDIT_TEXT_FANCY : ReportConstants.CREATE_TEXT_FANCY
						;
					String mode = isMapped ? nie.webui.UILink.UI_OPERATION_EDIT : nie.webui.UILink.UI_OPERATION_ADD;
					// Create the link
					Element linkElem2 = uiLink.generateLinkElement(inRequestObject, linkText2, mode, lQueryTerm );
					tmpElem.addContent( linkElem2 );
					***/


				}
				// Else it's null, don't offer it
				else {
					tmpElem = JDOMHelper.findOrCreateElementByPath(
						rowElem,		// Starting at
						"td[" + (++colCount) + "]/@align=center"
						+ "/@class=" + CSSClassNames.DATA_CELL,
						true
						);
					tmpElem.addContent( ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT );
				}


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


			// Add a helpful message if no records display
			if( actualFirstRowDisplayed < 1 )
				addNoRecordsMsg( resultsTableElement,
					lReportColumnCount, null
					);


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


			///////////////////////////////////////////////////////////
			//
			// Now we add the statistics, if any
			// =================================================
			//
			////////////////////////////////////////////////////////
			// if( getShouldDoStatistics() ) {
			if( totalSearches > 0 ) {

				// add the double horizontal rule
				addHR( resultsTableElement, lReportColumnCount );

				// add the row for the actual stats
				// First the row that will carry it
				Element groupStatsRow = new Element( "tr" );
				// mainContentTable.addContent( hrRow );
				resultsTableElement.addContent( groupStatsRow );

				// The label cell
				/////////////////////////////////

				// Build the left message, it starts in the extra left most skid column
				// we put in, but will push to the right if the first data column(s) was/were
				// not having stats tabulated
				// String leftMsg = "Cummulative Coverage:";
				// if( ! isShowingAllRecords )
				//	leftMsg += " (for displayed reocrds)";
				String leftMsg = "Search Coverage:";
				Element leftMsgElem = new Element( "th" );
				leftMsgElem.setAttribute( "colspan", "2" );
				leftMsgElem.setAttribute( "align", "right" );
				// Add the actual message
				leftMsgElem.addContent(leftMsg);
				// And add this gutter cell to the row
				groupStatsRow.addContent(leftMsgElem);


				// Now the percentage
				double percent = (double)runningNumSearchesTotal / (double)totalSearches;
				debugMsg( kFName,
					"runningNumSearchesTotal=" + runningNumSearchesTotal
					+ " totalSearches=" + totalSearches
					);
				// Format it
				String percentStr = XMLDefinedField.formatNumericTypeToString_static(
					new Double(percent),					// Numerical object
					true,									// boolean inComplainOnError,
					XMLDefinedField.FORMAT_AS_PERCENT,	// String optExplicitFormat,
					2, 2									// int optMinDec, int optMaxDec
					);
				// Add the cell
				Element tmpElem = JDOMHelper.findOrCreateElementByPath(
					groupStatsRow,		// Starting at
					// "td[2]/@class=nie_percentage",
					// No, it's 1 because th (above) != td
					// "td[1]/@class=nie_percentage",
					"td[1]/@class=" + CSSClassNames.PERCENTAGE_CELL,
					true	// Yes, tell us about errors
					);
				// Add the formatted answer
				if( null!=percentStr )
					tmpElem.addContent( percentStr );

			}

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

	public void generateMenuLinksToThisReportCompact(
		AuxIOInfo inRequest, Element inTopRow, Element inBottomRow
		)
	{
		// Put the header row up
		Element titleElem = JDOMHelper.findOrCreateElementByPath(
			inTopRow,
			"th[+]/@colspan=" + kReportDays.length
			+ "/@align=center"
			+ "/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
			// + "/@class=" + CSSClassNames.CONTAINER_CELL
			// + "/div/@class="
			//// + CSSClassNames.INACTIVE_RPT_LINK
			// + CSSClassNames.INACTIVE_MENU_LINK
			,
			true
			);
		// titleElem.addContent( getLinkText(null) + ReportConstants.SUGGESTED_MULTI_ITEM_MENU_SUFFIX );
		titleElem.addContent( getLinkTextCompact(null) );

		// For each link
		// for( int i=0; i<links.size(); i++ )
		for( int i=0; i<kReportDays.length; i++ )
		{
			// Get the link
			// ReportLink link = (ReportLink) links.get( i );
			String daysStr = kReportDays[i];

			// Create and link the row
			Element cell = JDOMHelper.findOrCreateElementByPath(
				inBottomRow,
				"td[+]"
				+ "/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
				,
				true
				);

			// Get the link and add the link to it
			// Element linkElem = link.generateRichLink(
			//	inRequest, getReportName(), null, true
			//	);
			Element linkElem = generateSublink( inRequest, daysStr );

			cell.addContent( linkElem );
		}

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
		linkInfo.copyInCGIFields( inRequestObject, ReportConstants.fMiscReportFields );


		// Set the report name to us
		linkInfo.setOrOverwriteCGIField(
			ReportConstants.REPORT_NAME_CGI_FIELD,
			kClassName()
			);

		linkInfo.setOrOverwriteCGIField(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME, inDays
			);

		// We need the propper context and command
		linkInfo.setOrOverwriteCGIField(
			nie.sn.SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD,
			nie.sn.SnRequestHandler.SN_CONTEXT_ADMIN
			);
		linkInfo.setOrOverwriteCGIField(
			nie.sn.SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD,
			nie.sn.SnRequestHandler.ADMIN_CONTEXT_REPORT
			);

		// Get the full URL and add it to the anchor
		String href = linkInfo.getFullCGIEncodedURL();
		anchor.setAttribute( "href", href );

		return anchor;
	}

	public String getTitle( Hashtable inVars )
	{
		return "Most Popular Searches";
	}
	public String getTitleCompact( Hashtable inVars )
	{
		// return "Searches";
		return "Popular";
	}
	public String getSubtitleOrNull( Hashtable inHash ) {
		return cSubtitleB;
	}

	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}
	public String getLinkTextCompact( Hashtable inVars )
	{
		return getTitleCompact( inVars );
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
	// ^^^ See ReportConstants if you ever need this

	// The list of link choices we will offer
	static final String [] kReportDays = { "1", "7", "30" };;




}

package nie.sr2;

import java.util.*;
import java.sql.*;
import org.jdom.Element;
import org.jdom.Comment;
import nie.core.*;
import nie.sn.CSSClassNames;

/**
 * @author mbennett
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class XMLDefinedReport extends BaseReport
{

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


	public String kClassName() {
		return "XMLDefinedReport";
	}



	protected String kFullClassName() {
		return "nie.sr2.XMLDefinedReport";
	}


	public XMLDefinedReport(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortReportName,
		JDOMHelper inReportDefinitionElement
		)
			throws ReportConfigException
	{
		// super( inMainApp, inShortReportName );
		super( inMainConfig, inShortReportName );

		final String kFName = "constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// we also need a report definition
		if( null==inReportDefinitionElement )
			throw new ReportConfigException( kExTag
				+ "Null XML report definition passed in"
				+ " for report " + getReportName()
				);

		fMainElem = inReportDefinitionElement;

		// Translate the report for this dialect
		try {
			fMainElem = getDBConfig().applyDbVendorFilter( fMainElem );
		}
		catch( DBConfigException e ) {
			throw new ReportConfigException( kExTag +
				"Unable to translate report for this vendor's SQL dialect."
				+ " Error: " + e
				);
		}


		// Fill in the cache
		initCachedFields();
	}

	private void initCachedFields()
		throws ReportConfigException
	{
		fUseCache = false;

		getRequiredAccessLevel();

		generateSQL( null, null );
		// getFieldObjects();
		// ^^^ called and cached by getSQLFieldsSelectString()

		getTitle( null );
		getLinkText( null );
		getLinkTextCompact( null );
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

		fUseCache = true;
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


		List links = getSuggestedLinksOrNull();
		// If no links, just generate the title as a hyperlink
		if( null==links || links.isEmpty() )
		{
			Element anchor = generatePlainLinkToThisReport( inRequest, true );

			// Create a table cell for the link and add it
			Element containerElem = JDOMHelper.findOrCreateElementByPath(
				outElem,
				"tr/td/@class=" + CSSClassNames.CONTAINER_CELL
				,
				true
				);
			containerElem.addContent( anchor );

		}
		// Else we do have some suggested links to present
		else
		{
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
			titleElem.addContent( getLinkText(null) );

			// For each link
			for( int i=0; i<links.size(); i++ )
			{
				// Get the link
				ReportLink link = (ReportLink) links.get( i );

				// Create and link the row
				Element row = JDOMHelper.findOrCreateElementByPath(
					outElem, "tr[" + (i+2) + ']', true
					);

				// Add an indent cell, we don't need to save this
				JDOMHelper.findOrCreateElementByPath(
					row,
					"td/@class=" + CSSClassNames.CONTAINER_CELL
					+ "/@valign=top"
					// + "/li"	// A link element
					// see if an image tag would be more compact
					+ "/img/@height=0/@width=" + kIndentPixels
					// See if just a width will work
					// + "/@width=" + kIndentPixels
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
				Element linkElem = link.generateRichLink(
					inRequest, getReportName(), null, true
					);
				linkCell.addContent( linkElem );
			}

		}

		// And we're done!
		return outElem;

	}


	public void generateMenuLinksToThisReportCompact(
		AuxIOInfo inRequest, Element inTopRow, Element inBottomRow
		)
	{

		List links = getSuggestedLinksOrNull();
		// If no links, just generate the title as a hyperlink
		if( null==links || links.isEmpty() ) {
			// Element anchor = generatePlainLinkToThisReport( inRequest, true );
			Element anchor = generatePlainLinkToThisReportCompact( inRequest, true );

			// Create a table cell for the link and add it
			Element containerElem = JDOMHelper.findOrCreateElementByPath(
				inTopRow,
				"th[+]/@rowspan=2" // /@class=" + CSSClassNames.CONTAINER_CELL
				+ "/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
				,
				true
				);
			containerElem.addContent( anchor );

		}
		// Else we do have some suggested links to present
		else {
			// Put the header row up
			Element titleElem = JDOMHelper.findOrCreateElementByPath(
				inTopRow,
				"th[+]/@colspan=" + links.size()				+ "/@align=center"
				+ "/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
				// + "/@class=" + CSSClassNames.CONTAINER_CELL
				// + "/div/@class="
				// // + CSSClassNames.INACTIVE_RPT_LINK
				// + CSSClassNames.INACTIVE_MENU_LINK
				,
				true
				);
			// titleElem.addContent( getLinkText(null) );
			titleElem.addContent( getLinkTextCompact(null) );
			// titleElem.addContent( getLinkText(null) );

			// For each link
			for( int i=0; i<links.size(); i++ )
			{
				// Get the link
				ReportLink link = (ReportLink) links.get( i );

				// Create and link the row
				Element linkCell = JDOMHelper.findOrCreateElementByPath(
					inBottomRow,
					"td[+]/@align=center"
					+ "/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
					// + "/@class=" + CSSClassNames.CONTAINER_CELL
					, true
					);

				// Add Sean's time period background colors
				int nDays = link.getNDaysIfApplicable();
				if( nDays > 0 ) {
					if( 1 == nDays )
						linkCell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_DAY_BGC );
					else if( 7 == nDays )
						linkCell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_WEEK_BGC );
					else if( 30 == nDays )
						linkCell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_MONTH_BGC );
					else if( 90 == nDays )
						linkCell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_QUARTER_BGC );
					else if( 365 == nDays )
						linkCell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_YEAR_BGC );
				}


				// Get the link and add the link to it
				Element linkElem = link.generateRichLink(
					inRequest, getReportNameCompact(), null, true
					);
				linkCell.addContent( linkElem );
			}
		}

	}






	private static void __Main_Logic__() {}

	// This may NOT be the version that is run if it's a Java report
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

		// We'll use this hash for varaible substitution in Strings
		Hashtable lMasterValuesHash = getVariables( inRequestObject );

		// The two key points in the tree
		Element [] points = prepareBaseOutputTree(
			inDoFullPage, lMasterValuesHash,
			inRequestObject, inResponseObject
			);
		Element outElem = points[0];
		// Element contentHanger = points[1];
		Element mainContentTable = points[1];

		// Prepare the query
		String qry = null;
		try {
			qry = generateSQL( lMasterValuesHash, inRequestObject );
			if(debug)
				debugMsg( kFName, "QRY=\"" + qry + "\"" );
		}
		catch( ReportConfigException e ) {
			// This really shouldn't happen, by now this was
			// tested by init cache, but anyway, recast as runtime now
			throw new ReportException( kExTag
				+ "Error generating SQL for this report."
				+ " Error: " + e
				);
		}


		//
		// RUN the query!!!
		// ==================================================
		// (and only build the headers if we had success)
		//
		//////////////////////////////////////////////////////

		ResultSet results = null;
		Statement myStatement = null;
		Connection myConnection = null;
		try
		{

			// This actually runs the query
			// results = getDBConfig().runQuery( qry );
			Object [] objs = getDBConfig().runQuery( qry, true );
			results = (ResultSet) objs[0];
			myStatement = (Statement) objs[1];
			myConnection = (Connection) objs[2];


			// Start building the output document
			Element resultsTableElem = new Element( "table" );
			resultsTableElem.setAttribute( "class", CSSClassNames.RESULTS_TABLE );
			// Hold off adding it until we know how well we did

			// Other subroutines may also call this, don't worry
			List fields = getFieldObjects();


			// We add an extra column to the left if we're doing stats,
			// to maybe make room for a left hanging label
			// And we make this a plain td with no markup, hoping that it
			// (the extra space) will not really be needed
			boolean useGutter =
				getShouldDoStatistics() && (0 == cFirstStatsFieldOffsetDisplayed);

			// Init where we will store stats, if keeping them
			double [][] summaryStats = initSummaryStatsArray();

			//
			// Prepare Column Headings
			// =================================
			//
			///////////////////////////////////////////////
			int lReportColumnCount = addHeader( resultsTableElem, useGutter );
			// Used to be a lot of inline code here

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
			// while( ! atEndOfRecords && results.next() )
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

				// We may add an extra column to the left if we're doing stats,
				// to maybe make room for a left hanging label
				// And we make this a plain td with no markup
				// If we need a gutter column, add it to the header
				if( useGutter ) {
					rowElem.addContent( new Element("td") );
				}

				final boolean kUseLateBinding = true;
				// hashes to cache field values
				// Hashtable fieldObjectHash = null;
				Hashtable fieldRawStringHash = null;
				Object [] fieldObjectAry = null;
				// If we're doing late binding, then cache these
				if( kUseLateBinding ) {
					// fieldObjectHash = new Hashtable();
					fieldRawStringHash = new Hashtable();
					fieldObjectAry = new Object[ fields.size() ];
	
					// For each column, mixed Zero and One based indexing
					for( int i=0; i<fields.size(); i++ )
					{
						// Get the field definition
						XMLDefinedField fieldDef =
							(XMLDefinedField)fields.get(i);	// Zero-based

						// Get the value
						Object valueObject = results.getObject( i+1 ); // One-Based
						fieldObjectAry[ i ] = valueObject;

						// Get the key, normalization controlled in getFieldID
						String fieldKey = fieldDef.getFieldID();
						if( null==fieldKey ) {
							errorMsg( kFName,
								"No key for field # " + (i+1) + ", skipping."
								);
							continue;
						}

						if( fieldRawStringHash.containsKey(fieldKey) ) {
							errorMsg( kFName,
								"Duplicate field # " + (i+1) + ", ignoring new value."
								+ " name=\"" + fieldKey + "\""
								+ ", new value=\"" + valueObject + "\""
								+ ", previous value=\"" + (String)fieldRawStringHash.get(fieldKey) + "\""
								);
							continue;
						}
						if( null!=valueObject ) {
							fieldRawStringHash.put( fieldKey, "" + valueObject );
						}
						else {
							fieldRawStringHash.put( fieldKey, ReportConstants.INTERNAL_NULL_MARKER_SEQUENCE );
							// If null, ignore it
							continue;
						}

						// And we store the true object
						// fieldObjectHash.put( normKey, valueObject );
					}
				}


				// For each column, mixed Zero and One based indexing
				for( int i=0; i<fields.size(); i++ )
				{
					// Get the field definition
					XMLDefinedField fieldDef =
						(XMLDefinedField)fields.get(i);	// Zero-based

					// Get the value
					Object valueObject = null;
					if( kUseLateBinding )
						valueObject = fieldObjectAry[ i ];
					else
						valueObject = results.getObject( i+1 ); // One-Based

					// Display, if we're supposed to
					if( fieldDef.getShouldDisplay() ) {

						// Have the field class generate the display
						// element that we need
						Element valueElment =
							fieldDef.generateDataElement(
								valueObject,
								inRequestObject,
								fieldRawStringHash
								);


						// Add, if we got something
						if( null!=valueElment )
						{
							// Add the cell to the row
							rowElem.addContent( valueElment );
						}
						// Else something is very wrong
						else
						{
							errorMsg( kFName,
								"No data cell for field # " + (i+1)
								+ " in report \"" + getReportName() + "\""
								+ " Adding empty td cell."
								// + " Report may be missing columns."
								);
							rowElem.addContent( new Element("td") );
						}

						// Keep track of statistics if needed
						if( getShouldDoStatistics() && fieldDef.getShouldDoStatistics() ) {
							summaryStats[i] = fieldDef.tabulateStats(valueObject, inRequestObject, summaryStats[i] );
						}

					}	// End if should be displayed

				}	// End for each field


				// Add the row to the table
				resultsTableElem.addContent( rowElem );

				// Update our displayed statistics

				// If this is the first actual row displayed
				// then remember it
				if( actualFirstRowDisplayed < 0 )
					actualFirstRowDisplayed = rowCount;

				// This is always the last one we've displayed
				actualLastRowDisplayed = rowCount;


			}	// End for each row


			// Add a helpful message if no records display
			if( actualFirstRowDisplayed < 1 )
				addNoRecordsMsg( resultsTableElem,
					lReportColumnCount, null
					);


			// Now add the results set to the main document
			// bodyElem.addContent( resultsTableElem );
			// No, will be added to mainContentTable

			// We postpone doing the "stats" row until
			// the paging logic has had a chance to set
			// isShowingAllRecords 



			// Paging Links
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
			recordsTableCellElem.addContent( resultsTableElem );


			///////////////////////////////////////////////////////////
			//
			// Now we add the statistics, if any
			// =================================================
			//
			////////////////////////////////////////////////////////
			if( getShouldDoStatistics() ) {

				// add the double horizontal rule
				addHR( resultsTableElem, lReportColumnCount );

				// add the row for the actual stats
				// First the row that will carry it
				Element groupStatsRow = new Element( "tr" );
				// mainContentTable.addContent( hrRow );
				resultsTableElem.addContent( groupStatsRow );

				// The fields:
				// cFirstStatsFieldOffsetActual
				// cFirstStatsFieldOffsetDisplayed
				// Have info for us about which column the stats start in

				// Build the left message, it starts in the extra left most skid column
				// we put in, but will push to the right if the first data column(s) was/were
				// not having stats tabulated
				String leftMsg = getGroupStatsRowLabel();
				if( ! isShowingAllRecords )
					leftMsg += " (for displayed reocrds)";
				Element leftMsgElem = new Element( "th" );
				leftMsgElem.setAttribute( "align", "right" );

				// The label cell
				/////////////////////////////////
				// How big the label column should be:
				int colSpan = useGutter ? 1 : cFirstStatsFieldOffsetDisplayed; // +1-1=+0=nothing
				// colspan is gutter plus columns before first tabulation
				leftMsgElem.setAttribute( "colspan", ""+colSpan );
				// Add the actual message
				leftMsgElem.addContent(leftMsg);
				// And add this gutter cell to the row
				groupStatsRow.addContent(leftMsgElem);


				// For each column, mixed Zero and One based indexing
				for( int i=cFirstStatsFieldOffsetActual; i<fields.size(); i++ )
				{
					// Get the field definition
					XMLDefinedField fieldDef =
						(XMLDefinedField)fields.get(i);	// Zero-based

					// Display, if we're supposed to
					if( fieldDef.getShouldDisplay() ) {
						Element statsElem = fieldDef.generateStatsElement( inRequestObject, summaryStats[i] );
						groupStatsRow.addContent( statsElem );
					}
				}

			}

			// Now add a horizontal rule at the bottom of the table
			addHR( resultsTableElem, lReportColumnCount );

			// Then add the paging stuff, if any
			//////////////////////////////////////////////////
			addPagingLinksIfNeeded( resultsTableElem,
			// addPagingLinksIfNeeded( mainContentTable,
				inRequestObject, inResponseObject,
				isDoingPaging, isShowingAllRecords,
				hasPrevRecords, hasMoreRecords, 
				actualFirstRowDisplayed, actualLastRowDisplayed,
				desiredRowCount,
				lReportColumnCount
				);

		}
		catch( Exception e )
		{
		    stackTrace( kFName, e, "Exception Running Report" );
			throw new ReportException( kExTag
				+ "Error running report."
				+ " Report = \"" + getReportName() + "\""
				+ " SQL Query =\"" + qry + "\""
				+ " Error: " + e
				);
		}
		finally {
			results = DBConfig.closeResults( results, kClassName(), kFName, false );
			myStatement = DBConfig.closeStatement( myStatement, kClassName(), kFName, false );
			// myConnection = DBConfig.closeConnection( myConnection, kClassName(), kFName, false );
		}

		// Return the top HTML element
		return outElem;

	}

	private static void __Init_and_Setup__() {}

	double [][] initSummaryStatsArray()
		throws ReportConfigException
	{
		List fields = getFieldObjects();
		double [][] answer = null;
		if( getShouldDoStatistics() ) {
			answer = new double[fields.size()][4];
			for( int i=0; i<fields.size(); i++ )
				answer[i] = XMLDefinedField.initStats();
		}
		return answer;
	}






	int addHeader( Element inContentTable, boolean inUseGutter )
		throws ReportConfigException
	{
		final String kFName = "addHeader";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inContentTable )
			throw new ReportConfigException( kExTag +
			"Null parent content container passed in."
			);

		// Add the header row
		Element headerRowElem = new Element( "tr" );
		headerRowElem.setAttribute( "class", CSSClassNames.HEADER_ROW );

		int lReportColumnCount = 0;

		List fields = getFieldObjects();

		// If we need a gutter column, add it to the header
		if( inUseGutter ) {
			lReportColumnCount++;
			headerRowElem.addContent( new Element("th") );
		}

		// For each column
		for( int i=0; i<fields.size(); i++ )
		{
			// Get the field definition
			XMLDefinedField fieldDef =
				(XMLDefinedField)fields.get(i);	// Zero-based

			// Display, if we're supposed to
			if( fieldDef.getShouldDisplay() )
			{

				// Get the element this header
				Element headingElem =
					fieldDef.generateHeaderElement();
				if( null!=headingElem )
				{
					// Add the cell to the row
					headerRowElem.addContent( headingElem );
					// lReportColumnCount++;
				}
				// Else something is very wrong
				else
				{
					errorMsg( kFName,
						"No heading cell for field # " + (i+1)
						+ " in report \"" + getReportName() + "\""
						+ " Adding empty th cell."
						// + " Report may be missing columns."
						);
					headerRowElem.addContent( new Element("th") );
				}

				// To avoid missing data below, we always put
				// at least a holder in and allow the column
				// count to increment
				// lReportColumnCount++;
				lReportColumnCount += fieldDef.getHeadingColSpan();


			}	// End if we are supposed to display
		}
		// Add the row to the table
		inContentTable.addContent( headerRowElem );

		return lReportColumnCount;
	}

	private static void __SQL__() {}

	XMLReportFilterInterface locateFilter( String inFieldID )
	{
		final String kFName = "locateFilter";
		final boolean kCasen = false;

		if( kCasen )
			inFieldID = NIEUtil.trimmedStringOrNull( inFieldID );
		else
			inFieldID = NIEUtil.trimmedLowerStringOrNull( inFieldID );

		if( null == inFieldID )
		{
			errorMsg( kFName,
				"Null/empty filter name passed in, returning null."
				);
			return null;
		}


		XMLReportFilterInterface outFiler = null;



		// Todo: First, lookup in filter objects


		// Then lookup in fields
		if( cFieldHash.containsKey( inFieldID ) )
		{
			XMLDefinedField tmpField =
				(XMLDefinedField) cFieldHash.get( inFieldID );
			if( tmpField != null && tmpField.getIsFilterField() )
			{
				outFiler = (XMLReportFilterInterface)tmpField;
			}
			// Else it's not a filter field
			else
			{
				errorMsg( kFName,
					"Requested filter field \"" + inFieldID + "\""
					+ " is not a filter-field (either missing or not a filter)."
					+ " Cached field names=" + cFieldHash.keySet()
					+ " Will return null field."
					+ " Field=" + tmpField
					);
			}
		}

		// done
		return outFiler;

	}



	String calculateAutomaticFilterString( AuxIOInfo inRequest )
	{
		final String kFName = "calculateAutomaticFilterString";
		boolean debug = shouldDoDebugMsg( kFName );

		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in, returning null."
				);
			return null;
		}

		if(debug) debugMsg( kFName, "FILTER_NAME_CGI_FIELD_NAME=" + ReportConstants.FILTER_NAME_CGI_FIELD_NAME );

		/***
		String filterName = inRequest.getScalarCGIFieldTrimOrNull(
			ReportConstants.FILTER_NAME_CGI_FIELD_NAME
			);

		// No big deal if no filter
		if( null == filterName )
			return null;
		***/

		// Allow for MULTIPLE filters
		List filterNames = inRequest.getMultivalueCGIField(
			ReportConstants.FILTER_NAME_CGI_FIELD_NAME
			);

		// No big deal if no filter
		if( null == filterNames || filterNames.isEmpty() ) {
			debugMsg( kFName, "No filteres specified in CGI request, returning null." );
			return null;
		}

		StringBuffer outBuff = new StringBuffer();
		if(debug) debugMsg( kFName, "CGI filters = " + filterNames );
		// For each filter
		for( Iterator it=filterNames.iterator() ; it.hasNext() ; ) {
			String filterName = (String) it.next();

			// look it up
			// (locate method will normalize if needed)
			XMLReportFilterInterface filter = locateFilter( filterName );
			if( null == filter )
			{
				if( null!=filterName && ! filterName.equalsIgnoreCase( ReportConstants.DAYS_OLD_CGI_FIELD_NAME) )
					errorMsg( kFName,
						"Unable to locate a valid filter named \"" + filterName + "\""
						+ ", returning null."
						+ " Reminder: If you're trying to filter on a field but"
						+ " don't want it to displayed, you should still include it"
						+ " in the report and just set its "
						+ XMLDefinedField.SHOULD_DISPLAY_ATTR
						+ " attribute to FASLE."
						);
				return null;
			}
	
			String filterStr = filter.calculateFilterExpression( inRequest );
			if( null == filterStr ) {
				errorMsg( kFName,
					"Null filter expression for filter \"" + filterName + "\""
					+ " Ignoring this filter name."
					);
				continue;
			}

			// Add this filter
			if( outBuff.length() > 0 )
				outBuff.append( " AND " );
			outBuff.append( filterStr );

		}	// end for each filter


		if(debug) debugMsg( kFName, "Returning " + outBuff );

		if( outBuff.length() > 0 )
			return new String( outBuff );
		else
			return null;

	}








	// Todo: settable row counts by report, and enforceable



	String generateSQL(
			Hashtable inValuesHash,
			AuxIOInfo inRequest
		)
		throws ReportConfigException
	{
		final String kFName = "generateSQL";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		// if( ! fUseCache && null==cSqlText )
		//{
		boolean debug = shouldDoDebugMsg( kFName );

			cSqlText = getRawSQL();

			if( null==cSqlText )
			{
				StringBuffer buff = new StringBuffer();

				// Select
				buff.append( "SELECT " );
				if( null != getSelectModifier() )
				{
					buff.append( getSelectModifier() );
					buff.append( ' ' );
				}

				// The field clause
				buff.append( getSqlFieldsAsSelectString() );

				// From
				buff.append( " FROM " );
				buff.append( getSqlFromString() );

				// Where (join and filter)
				String tmpStr = calculateSqlWhereString(
					/* inValuesHash, */
					inRequest
					);
				if( null!=tmpStr )
				{
					buff.append( " WHERE " );
					buff.append( tmpStr );
				}

				// group by
				tmpStr = getSqlGroupByString();
				if( null!=tmpStr )
				{
					buff.append( " GROUP BY " );
					buff.append( tmpStr );
				}

				// order by
				tmpStr = getSqlOrderByString();
				if( null!=tmpStr )
				{
					buff.append( " ORDER BY " );
					buff.append( tmpStr );
				}

				// Add done!
				cSqlText = new String( buff );
			// }

			if( null!=cSqlText && cSqlText.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;
		}	// Done caching logic


		// statusMsg( kFName, "SQL = " + cSqlText );
		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache || ! getShouldDoVariableSubstitutions() )
		{
			if(debug)
				debugMsg( kFName, "Returning cached sql:" + NIEUtil.NL + cSqlText );
			return cSqlText;
		}
		// If there IS var subst, apply it to the values
		else
		{
			String tmpStr = NIEUtil.markupStringWithVariables(
				cSqlText, inValuesHash
				);
			if(debug)
				debugMsg( kFName, "Returning dynamic sql:" + NIEUtil.NL + tmpStr );
			return tmpStr;
		}
	}

	private static void __Setters_and_Getters__() {}

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
			String value = inRequest.getScalarCGIField( key );
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


	// It's OK not to have a subtitle
	public int getRequiredAccessLevel()
	{
		if( ! fUseCache )
		{
			final String kFName = "getRequiredAccessLevel";
			cAccessLevel = fMainElem.getIntFromAttribute(
				ACCESS_LEVEL_ATTR, ReportConstants.DEFAULT_ACCESS_LEVEL
				);
			if( cAccessLevel < 1 )
				infoMsg( kFName,
					"FYI: report \"" + getReportName() + "\""
					+ " is set for PUBLIC ACCESS, visible to anybody, with no password required."
					+ " (this was probably done intentionally, and it's OK, but just letting you know)"
					);
		}
		return cAccessLevel;
	}

	// It's OK not to have a subtitle
	String getRawSQL()
	{
		if( ! fUseCache && null==cRawSql )
		{
			cRawSql = fMainElem.getTextByPathTrimOrNull(
				RAW_SQL_PATH
				);
		}
		return cRawSql;
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
				cTitle = "Report: " + getReportName();

			if( null!=cTitle && cTitle.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;

		}

		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
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

	}

	public String getLinkText( Hashtable inValuesHash )
	{
		if( ! fUseCache && null==cLinkText )
		{
			final String kFName = "getLinkTitle";
			cLinkText = fMainElem.getTextByPathTrimOrNull(
				LINK_TEXT_PATH
				);
			if( null==cLinkText )
				cLinkText = getTitle( inValuesHash );

			if( null!=cLinkText && cLinkText.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;

		}

		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == inValuesHash
			)
		{
			return cLinkText;
		}
		// If there IS var subst, apply it to the values
		else
		{
			return NIEUtil.markupStringWithVariables(
				cLinkText, inValuesHash
				);
		}

	}

	public String getLinkTextCompact( Hashtable inValuesHash )
	{
		if( ! fUseCache && null==cLinkTextCompact ) {
			final String kFName = "getLinkTextCompact";
			cLinkTextCompact = fMainElem.getTextByPathTrimOrNull(
				LINK_TEXT_COMPACT_PATH
				);
			if( null==cLinkTextCompact )
				cLinkTextCompact = getLinkText( inValuesHash );

			if( null!=cLinkTextCompact && cLinkTextCompact.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;

		}

		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == inValuesHash
			)
		{
			return cLinkTextCompact;
		}
		// If there IS var subst, apply it to the values
		else {
			return NIEUtil.markupStringWithVariables(
				cLinkTextCompact, inValuesHash
				);
		}

	}




	// It's OK not to have a subtitle
	public String getSubtitleOrNull( Hashtable inValuesHash )
	{
		if( ! fUseCache && null==cSubtitle )
		{
			final String kFName = "getSubitle";
			cSubtitle = fMainElem.getTextByPathTrimOrNull(
				SUBTITLE_PATH
				);

			if( null!=cSubtitle && cSubtitle.indexOf( '$' ) >= 0 )
				fHaveSeenDollarSigns = true;

		}
		// return cSubtitle;


		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == cSubtitle
			|| null == inValuesHash
			)
		{
			return cSubtitle;
		}
		// If there IS var subst, apply it to the values
		else
		{
			return NIEUtil.markupStringWithVariables(
				cSubtitle, inValuesHash
				);
		}



	}


	String getGroupStatsRowLabel()
	{
		if( ! fUseCache )
		{
			// we do NOT trim or null, an empty string is acceptable to override a default
			cStatsLabel = fMainElem.getTextByPath(
				STATS_LABEL_PATH
				);
			if( null==cStatsLabel )
				cStatsLabel = DEFAULT_STATS_LABEL;
		}
		return cStatsLabel;
	}

	String getSelectModifier( /*Hashtable inValuesHash*/ )
	{
		if( ! fUseCache && null==cSelectModifier )
		{
			final String kFName = "getSelectModifier";
			cSelectModifier = fMainElem.getTextByPathTrimOrNull(
				SELECT_MODIFIER_PATH
				);

			// if( null!=cSubtitle && cSubtitle.indexOf( '$' ) >= 0 )
			//	fHaveSeenDollarSigns = true;

		}
		return cSelectModifier;

		/***
		// If no variable substitution, just return the query
		// Or if we're just priming the cache and have no vars anyway
		if( ! fUseCache
			|| ! getShouldDoVariableSubstitutions()
			|| null == cSubtitle
			|| null == inValuesHash
			)
		{
			return cSubtitle;
		}
		// If there IS var subst, apply it to the values
		else
		{
			return NIEUtil.markupStringWithVariables(
				cSubtitle, inValuesHash
				);
		}
		***/


	}


	String getSqlFromString()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cSqlFromString )
		{
			final String kFName = "getSqlFromString";
			final String kExTag = kClassName() + '.' + kFName + ": ";

			cSqlFromString = getRawFrom();
			if( null==cSqlFromString )
				cSqlFromString = getDefaultTableName();

			// Todo: EXPAND THIS!!!!

		}
		return cSqlFromString;
	}


	// It's OK not to have a subtitle
	String getRawFrom()
	{
		if( ! fUseCache && null==cRawFrom )
		{
			cRawFrom = fMainElem.getTextByPathTrimOrNull(
				RAW_FROM_PATH
				);
		}
		return cRawFrom;
	}


	// OK to return null
	String calculateSqlWhereString( AuxIOInfo inRequest )
		throws ReportConfigException
	{
		final String kFName = "calculateSqlWhereString";

		// if( ! fUseCache && null==cSqlWhere )
		// {
			int clauseCount = 0;

			String tmpStr1 = getRawWhereJoin();
			clauseCount += null!=tmpStr1 ? 1 : 0;
			debugMsg( kFName, "Str1=\"" + tmpStr1 + "\"" );

			String tmpStr2 = getRawWhereFilter();
			clauseCount += null!=tmpStr2 ? 1 : 0;
			debugMsg( kFName, "Str2=\"" + tmpStr2 + "\"" );

			String tmpStr3 = null;
			if( null != inRequest )
			{
				tmpStr3 = calculateAutomaticFilterString( inRequest );
				clauseCount += null!=tmpStr3 ? 1 : 0;
			}
			debugMsg( kFName, "Str3=\"" + tmpStr3 + "\"" );

			if( clauseCount < 1 )
				return null;

			StringBuffer outBuff = new StringBuffer();

			if( null != tmpStr1 )
			{
				if( clauseCount > 1 )
					outBuff.append( '(' );
				outBuff.append( tmpStr1 );
				if( clauseCount > 1 )
					outBuff.append( ')' );
			}

			if( null != tmpStr2 )
			{
				if( outBuff.length() > 0 )
					outBuff.append( " AND " );
				if( clauseCount > 1 )
					outBuff.append( '(' );
				outBuff.append( tmpStr2 );
				if( clauseCount > 1 )
					outBuff.append( ')' );
			}

			if( null != tmpStr3 )
			{
				if( outBuff.length() > 0 )
					outBuff.append( " AND " );
				if( clauseCount > 1 )
					outBuff.append( '(' );
				outBuff.append( tmpStr3 );
				if( clauseCount > 1 )
					outBuff.append( ')' );
			}

			if( outBuff.length() > 0 )
				return new String( outBuff );
			else
			{
				errorMsg( kFName,
					"Unexpected empty output buffer, returning null."
					);
				return null;
			}

			// Todo: EXPAND THIS!!!!

		// }
		// return cSqlWhere;
	}



	// It's OK not to have a subtitle
	String getRawWhereJoin()
	{
		if( ! fUseCache && null==cRawWhereJoin )
		{
			cRawWhereJoin = fMainElem.getTextByPathTrimOrNull(
				RAW_WHERE_JOIN_PATH
				);
		}
		return cRawWhereJoin;
	}
	// It's OK not to have a subtitle
	String getRawWhereFilter()
	{
		if( ! fUseCache && null==cRawWhereFilter )
		{
			cRawWhereFilter = fMainElem.getTextByPathTrimOrNull(
				RAW_WHERE_FILTER_PATH
				);
		}
		return cRawWhereFilter;
	}
	// Null is OK
	String getSqlOrderByString()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cSqlOrderBy )
		{
			cSqlOrderBy = getRawOrderBy();

			// Todo: EXPAND THIS!!!!

		}
		return cSqlOrderBy;
	}

	// It's OK not to have a subtitle
	String getRawOrderBy()
	{
		if( ! fUseCache && null==cRawOrderBy )
		{
			cRawOrderBy = fMainElem.getTextByPathTrimOrNull(
				RAW_ORDER_BY_PATH
				);
		}
		return cRawOrderBy;
	}
	// Null is OK
	String getSqlGroupByString()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cSqlGroupBy )
		{
			cSqlGroupBy = getRawGroupBy();

			// Todo: EXPAND THIS!!!!

		}
		return cSqlGroupBy;
	}

	// It's OK not to have a subtitle
	String getRawGroupBy()
	{
		if( ! fUseCache && null==cRawGroupBy )
		{
			cRawGroupBy = fMainElem.getTextByPathTrimOrNull(
				RAW_GROUP_BY_PATH
				);
		}
		return cRawGroupBy;
	}


	boolean getShouldDoStatistics()
		throws ReportConfigException
	{
		if( ! fUseCache ) {
			List fields = getFieldObjects();
			int actualCounter = 0;
			int displayCounter = 0;
			for( Iterator it=fields.iterator() ; it.hasNext() ; ) {
				XMLDefinedField field = (XMLDefinedField) it.next();
				cDoStats |= field.getShouldDoStatistics();
				actualCounter++;
				if( field.getShouldDisplay() )
					displayCounter++;
				// Once we've seen one, we have to do it
				if( cDoStats ) {
					cFirstStatsFieldOffsetActual = actualCounter-1;
					cFirstStatsFieldOffsetDisplayed = displayCounter-1;
					break;
				}
			}
		}
		return cDoStats;
	}

	List getFieldObjects()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cFieldList )
		{
			final String kFName = "getFieldObjects";
			final String kExTag = kClassName() + '.' + kFName + ": ";

			String reportName = getReportName();

			List elements = fMainElem.findElementsByPath(
				FIELD_ELEM_PATH
				);
			if( null==elements || elements.size()<1 )
				throw new ReportConfigException( kExTag
					+ "This report has no fields defined."
					+ " Report = \"" + reportName + "\""
					);

			cFieldList = new Vector();
			cFieldHash = new Hashtable();
			// Loop throug the list, we also want a field counter
			for( int i=0; i<elements.size(); i++ )
			{
				Element elem = (Element)elements.get(i);
				// Instantiate an actual object from the XML elememt
				XMLDefinedField newField = null;
				try
				{
					newField = new XMLDefinedField(
						elem, this, getMainConfig()
						);
				}
				catch( Exception e )
				{
					throw new ReportConfigException( kExTag
						+ "Field # " + (i+1) + " is invalid."
						+ " Report = \"" + reportName + "\""
						+ " Error: " + e
						);
				}
				// Add it to the list
				// Note: we CAN have duplicate field names, that
				// is perfectly valid in SQL, to repeat a field
				cFieldList.add( newField );


				// We also add these to a hash
				String fieldID = newField.getFieldID();
				// If we've not seen this before, then just add it
				if( ! cFieldHash.containsKey( fieldID ) )
				{
					cFieldHash.put( fieldID, newField );
				}
				// Else we have seen this Field ID before
				else
				{
					// Let's look at the previous one as well
					XMLDefinedField oldField =
						(XMLDefinedField)cFieldHash.get( fieldID )
						;

					// If the previous field was specifically set
					// then it wins, first come first serve
					if( oldField.getIsExplicitID() )
					{
						// We will take no action in terms of the hash
						// but we might like to generate some warnings

						// Is the new field ID specifically declared?
						if( newField.getIsExplicitID() )
						{
							errorMsg( kFName,
								"Two fields have both been declared"
								+ " with the same specific ID."
								+ " Field ID=\"" + fieldID + "\""
								+ " Report = \"" + reportName + "\""
								+ " The earliest field with this specific"
								+ " ID will be kept; the later field will"
								+ " not be referencable by name for things"
								+ " like sorting and filtering, though its"
								+ " data WILL still appear in the report"
								+ " assuming that it is set as visible."
								);
						}
						// Else the new field ID was NOT specifically declared
						else
						{
							infoMsg( kFName,
								"A subsequent field had an auto-generated"
								+ " field ID that was the same as a previous"
								+ " field's specifically set ID."
								+ " Field ID=\"" + fieldID + "\""
								+ " Report=\"" + reportName + "\""
								+ " The eariler field will be kept;"
								+ " the later field will"
								+ " not be referencable by name for things"
								+ " like sorting and filtering, though its"
								+ " data WILL still appear in the report"
								+ " assuming that it is set as visible."
								);
						}	// End else the new ID was not specific

					}
					// Else the old one was not specifically set
					else
					{

						// If the newer/later field IS specifically
						// set, we will keep it
						if( newField.getIsExplicitID() )
						{
							// Keep the NEW ONE
							cFieldHash.put( fieldID, newField );

							infoMsg( kFName,
								"A subsequent field has a specifically"
								+ " declared ID that is the same as a previous"
								+ " field's auto-generated ID."
								+ " Field ID=\"" + fieldID + "\"."
								+ " Report=\"" + reportName + "\""
								+ " This later field with this specific"
								+ " ID will be kept; the earlier field will"
								+ " not be referencable by name for things"
								+ " like sorting and filtering, though its"
								+ " data WILL still appear in the report"
								+ " assuming that it is set as visible."
								);
						}
						// The newer field is NOT specifically declared
						else
						{
							infoMsg( kFName,
								"A subsequent field had an auto-generated"
								+ " field ID that was the same as a previous"
								+ " field's auto-generated ID."
								+ " Field ID=\"" + fieldID + "\"."
								+ " Report = \"" + reportName + "\""
								+ " The eariler field will be kept;"
								+ " the later field will"
								+ " not be referencable by name for things"
								+ " like sorting and filtering, though its"
								+ " data WILL still appear in the report"
								+ " assuming that it is set as visible."
								);
						}	// End else the newer field is not specific

					}	// End else old field wasn't specifically declared

				}	// End else we have seen this Field ID before

			}	// Finished looping through the fields
		}	// End if not using cache yet

		// Return cached answer
		return cFieldList;
	}

	String getSqlFieldsAsSelectString()
		throws ReportConfigException
	{
		if( ! fUseCache && null==cSqlFieldsString )
		{
			final String kFName = "getSqlFieldsAsSelectString";
			final String kExTag = kClassName() + '.' + kFName + ": ";

			// get the fields, and that routine will throw
			// an exception if there's anything wrong
			List fields = getFieldObjects();
			// Loop through the fields, and we do need to
			// know if we are first or not
			StringBuffer buff = new StringBuffer();
			for( int i=0; i<fields.size(); i++ )
			{
				XMLDefinedField field = (XMLDefinedField)fields.get(i);
				// If not first, add the delimiter
				if( i>0 )
					buff.append( ", " );
				// Now add the field name and possible SQL alias
				String tmpStr = field.getSqlFieldAsSelectString();
				buff.append( tmpStr );
			}
			// Convert our buffer into its final String
			cSqlFieldsString = new String( buff );
		}
		return cSqlFieldsString;
	}


	protected List getSuggestedLinksOrNull()
	{
		if( ! fUseCache && null==cSuggestedLinks )
		{
			final String kFName = "getSuggestedLinks";
			final boolean kCasen = false;

			// We always create a hash, it may remain empty
			cSuggestedLinks = new Vector();

			// Lookup the list of parameter elements
			List tmpList = fMainElem.findElementsByPath(
				LINKS_PATH );
			// Loop through, if any; it's OK if there isn't
			if( null!=tmpList && tmpList.size() > 0 )
			{
				for( int i = 0; i < tmpList.size(); i++ )
				{
					Element elem = (Element) tmpList.get( i );

					ReportLink link = null;
					try
					{
						link = new ReportLink(
							elem, getMainConfig()
							);
					}
					catch( ReportConfigException e )
					{
						errorMsg( kFName,
							"Could not instantiate suggested link # " + (i+1)
							+ ", skipping."
							+ " Error: " + e
							);
						continue;
					}

					// OK, add it
					cSuggestedLinks.add( link );
				}
			}

		}
		return cSuggestedLinks;
	}

	private JDOMHelper fMainElem;

	List cSuggestedLinks;
	String cTitle;
	String cLinkText;
	String cLinkTextCompact;
	String cSubtitle;
	boolean cDoStats;
	// If we do group stats like sum/avg/ etc we will need to know how many columns
	// we have to play with the the left of the first stats column
	int cFirstStatsFieldOffsetActual = -1;
	int cFirstStatsFieldOffsetDisplayed = -1;

	String cStatsLabel;
	String cRawSql;
	String cSelectModifier;
	String cRawFrom;

	String cRawWhereJoin;
	String cRawWhereFilter;
	String cRawOrderBy;
	String cRawGroupBy;

	String cSqlFrom;
	String cSqlWhere;
	String cSqlOrderBy;
	String cSqlGroupBy;



	String cSqlText;
	// the list of full XMLDefinedField objects
	List cFieldList;
	// The list of fields stored by their ID
	Hashtable cFieldHash;
	String cSqlFieldsString;
	String cSqlFromString;

	int cAccessLevel;

	private static final String ACCESS_LEVEL_ATTR = "access_level";

	// Title and subtitle settings
	private static final String TITLE_PATH = "title";
	private static final String LINK_TEXT_PATH = "link_text";
	private static final String LINK_TEXT_COMPACT_PATH = "link_text_compact";
	private static final String SUBTITLE_PATH = "subtitle";

	static final String STATS_LABEL_PATH = "stats_label";
	private static final String RAW_SQL_PATH = "raw_sql";

	private static final String SELECT_MODIFIER_PATH = "select_modifier";
	private static final String RAW_FROM_PATH = "raw_from";

	private static final String RAW_WHERE_JOIN_PATH = "raw_where_join";
	private static final String RAW_WHERE_FILTER_PATH =
		"raw_where_filter";
	private static final String LINKS_PATH =
		"suggested_links/" + ReportLink.MAIN_ELEM_NAME;
	private static final String RAW_ORDER_BY_PATH = "raw_sort";
	private static final String RAW_GROUP_BY_PATH = "raw_grouping";


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
	static final String REQUEST_VARS_HASH_NAME = "cgi";
	static final String SQL_ESC_SUFFIX = "_sqlesc";
	static final String HTML_ESC_SUFFIX = "_htmlesc";



	// The default number of rows
	static final int DEFAULT_DESIRED_ROW_COUNT = 25; // 25; // 25;

	// We show fields by default
	static final boolean _x_DEFAULT_SHOULD_DISPLAY_FIELD = true;

	// Where CSS style sheets come from
	public static final String DEFAULT_CSS_URI =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "style_sheets/default_xml_defined_report.css"
		;

	static final boolean DEFAULT_SHOULD_DO_VAR_SUBST = true;
	static final String DEFAULT_STATS_LABEL = "Totals:";

	// Some of the class tags we use, others are hard coded if
	// used only once
	private static final String ACTIVE_PAGING_CSS_CLASS =
		"nie_active_paging_link";
	private static final String INACTIVE_PAGING_CSS_CLASS =
		"nie_inactive_paging_link";
	private static final String STAT_NUMBER_CSS_CLASS =
		"nie_stat_number";
	public static final String CONTAINER_CELL_CSS_CLASS =
		"nie_container_cell";






}

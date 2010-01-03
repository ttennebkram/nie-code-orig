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
public abstract class BaseReport implements ReportInterface
{

	public String kClassName() {
		return "BaseReport";
	}

	protected String kFullClassName() {
		return "nie.sr2.BaseReport";
	}


	// Jan '04, no longer adds main menu link
	public void addStatusRowAndMenuLink( Element inContentTable,
		AuxIOInfo inRequestObject, AuxIOInfo inResponseObject,
		int inDisplayedRowCount, int inDesiredStartRow,
		int inActualFirstRowDisplayed, int inActualLastRowDisplayed,
		boolean inIsShowingAllRecords
		)
			throws ReportConfigException, ReportException
	{
		final String kFName = "addStatusRow";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inContentTable )
			throw new ReportConfigException( kExTag +
			"Null parent content container passed in."
			);

		Element msgRowElem = new Element( "tr" );
		msgRowElem.setAttribute( "class", CSSClassNames.RPT_STATISTICS_ROW );
		// Add this row to the content
		// mainContentTable.addContent( statsRowElem );
		inContentTable.addContent( msgRowElem );
		// Now create the cell for the message
		Element outerStatsCellElem = new Element( "td" );
		// Add the cell to the row
		msgRowElem.addContent( outerStatsCellElem );
	
		// Now create the inner table for this cell
		Element innerStatsTable = new Element( "table" );
		// A hard coded style item
		innerStatsTable.setAttribute( "width", "100%" );
		// Add this table to the bounding cell
		outerStatsCellElem.addContent( innerStatsTable );
			
		// Now create the bounding row
		Element innerStatsRow = new Element( "tr" );
		// And add that row to the table
		innerStatsTable.addContent( innerStatsRow );
	
		// Create the first cell, for the stats
		Element innerStatsCell = new Element( "td" );
		innerStatsCell.setAttribute( "align", "left" );
		innerStatsCell.setAttribute( "class", CSSClassNames.RPT_STATISTICS_CELL );
		// Add the cell to the row
		innerStatsRow.addContent( innerStatsCell );
	
		// Now figure out the message
		// String statsMsg = null;
		if( inDisplayedRowCount < 1 )
		{
			if( inDesiredStartRow > 1 || ! inIsShowingAllRecords )
			{
				innerStatsCell.addContent(
					"There are no more records to display."
					);
			}
			else
			{
				innerStatsCell.addContent(
					"There are no records to display."
					);
			}
		}
		else if( inActualFirstRowDisplayed == inActualLastRowDisplayed )
		{
			if( inIsShowingAllRecords )
			{
				// statsMsg = "Showing the only record.";
				innerStatsCell.addContent(
					"Showing the only record."
					);
			}
			else
			{
				// statsMsg = "Showing record " + actualFirstRowDisplayed;
				innerStatsCell.addContent(
					"Showing record "
					);
				Element boldTag = new Element( "b" );
				boldTag.setAttribute(
					"class", CSSClassNames.RPT_PAGING_STATS_MSG_NUMBER_TEXT
					);
				boldTag.addContent( "" + inActualFirstRowDisplayed );
				innerStatsCell.addContent( boldTag );
			}
		}
		// Else displayed a range of rows
		else
		{
	//		statsMsg = "Showing records "
	//			+ actualFirstRowDisplayed
	//			+ " through "
	//			+ actualLastRowDisplayed
	//			;
	//		if( isShowingAllRecords )
	//			statsMsg += " (all records shown)";
	
			// "Showing records "
			innerStatsCell.addContent(
				"Showing records "
				);
	
			// 1
			Element boldTag1 = new Element( "b" );
			boldTag1.setAttribute(
				"class", CSSClassNames.RPT_PAGING_STATS_MSG_NUMBER_TEXT
				);
			boldTag1.addContent( "" + inActualFirstRowDisplayed );
			innerStatsCell.addContent( boldTag1 );
	
			// " through "
			innerStatsCell.addContent(
				" through "
				);
	
			// 10
			Element boldTag2 = new Element( "b" );
			boldTag2.setAttribute(
				"class", CSSClassNames.RPT_PAGING_STATS_MSG_NUMBER_TEXT
				);
			boldTag2.addContent( "" + inActualLastRowDisplayed );
			innerStatsCell.addContent( boldTag2 );
	
			if( inIsShowingAllRecords )
			{
				Element iTag = new Element( "i" );
				iTag.setAttribute(
					"class", CSSClassNames.RPT_PAGING_STATS_QUALIFIER_MSG_TEXT
					);
				iTag.addContent( " (all records shown)" );
				innerStatsCell.addContent( iTag );
			}
	
		}

		/***
		// Create the second cell, for the link to the main menu
		Element innerMenuCell = new Element( "td" );
		innerMenuCell.setAttribute( "align", "right" );
		innerMenuCell.setAttribute( "class", CSSClassNames.MENU_CELL );
		// Add the cell to the row
		innerStatsRow.addContent( innerMenuCell );

		// Now get the main menu link
		Element mainMenuElem = generateLinkToMainMenu(
			inRequestObject, inResponseObject
			);
		// And add that to this cell
		innerMenuCell.addContent( mainMenuElem );
		***/

	}




	public void addHR( Element inContentTable, int inColumnCount )
		throws ReportConfigException, ReportException
	{
		final String kFName = "addHR";
		final String kExTag = kClassName() + '.' + kFName + ": ";
	
		if( null==inContentTable )
			throw new ReportConfigException( kExTag +
				"Null parent content container passed in."
				);
		if( inColumnCount < 1 )
			throw new ReportConfigException( kExTag +
				"Invalid column span/count=" + inColumnCount + " passed in; must be >= 1."
				);


		// Add a blank spacer cell if using black cell instead of HR
		Element spacerRow = new Element( "tr" );
		inContentTable.addContent( spacerRow );
		Element spacerCell = new Element( "td" );
		spacerCell.setAttribute( "height", "1" );
		spacerRow.addContent( spacerCell );
	
		// Now add a horizontal rule at the bottom of the table
		// First the row that will carry it
		Element hrRow = new Element( "tr" );
		// mainContentTable.addContent( hrRow );
		inContentTable.addContent( hrRow );

		// the cell in the row
		Element hrCell = new Element( "td" );
		hrCell.setAttribute( "class", CSSClassNames.HR_CELL );
		hrCell.setAttribute( "valign", "top" );
		// hrCell.setAttribute( "colspan", "2" );
		hrCell.setAttribute( "colspan", ""+inColumnCount );
		hrRow.addContent( hrCell );

		// the hr attribute
		/***
		Element hrElem = new Element( "hr" );
		hrElem.setAttribute( "width", "100%" );
		hrElem.setAttribute( "size", "1" );
		hrElem.setAttribute( "noshade", "1" );
		hrCell.addContent( hrElem );
		***/

		// Try using a black cell rather than an HR
		hrCell.setAttribute( "bgcolor", "#555555" );
		hrCell.setAttribute( "height", "1" );



		// The second HR element
		// at some point we might want to let them add a double
		// I had tried it before and didn't like it
		/***
		Element hrElemb = new Element( "hr" );
		hrElemb.setAttribute( "width", "100%" );
		hrElemb.setAttribute( "size", "1" );
		hrElemb.setAttribute( "noshade", "1" );
		hrCella.addContent( hrElemb );
		***/


	}


	public void addNoRecordsMsg( Element inResultsTableElement,
		int inColSpan, String optMsg
	) {
		final String kFName = "addNoRecordsMsg";
		if( null==inResultsTableElement ) {
			errorMsg( kFName, "Null container element passed in; no action will be taken." );
			return;
		}

		optMsg = NIEUtil.trimmedStringOrNull( optMsg );
		if( inColSpan < 1 ) {
			inColSpan = 1;
			warningMsg( kFName, "Defaulting colspan to 1, this may distrupt results table display format." );
		}
		if( null==optMsg )
			optMsg = "- - (no matching records to display) - -";

		Element tmpElem = JDOMHelper.findOrCreateElementByPath(
			inResultsTableElement,
			"tr[+]/@class=" + CSSClassNames.ODD_ROW
			+ "/td/@align=center/@height=40"
			+ "/@class=" + CSSClassNames.DATA_CELL
			+ "/@colspan=" + inColSpan
			+ "/small/i"
			,
			true	// Yes, tell us about errors
			);
		if( null!=tmpElem )
			tmpElem.addContent( optMsg );
		else
			errorMsg( kFName, "Got null when creating message element; no action taken." );

	}



	// Then add the paging stuff, if any
	// return value says whether it did it or not
	public boolean addPagingLinksIfNeeded( Element inContentTable,
		AuxIOInfo inRequestObject, AuxIOInfo inResponseObject,
		boolean inIsDoingPaging, boolean inIsShowingAllRecords,
		boolean inHasPrevRecords, boolean inHasMoreRecords,
		int inActualFirstRowDisplayed, int inActualLastRowDisplayed,
		int inDesiredRowCount,
		int inColumnCount
		)
			throws ReportConfigException, ReportException
	{
		final String kFName = "addPagingLinksIfNeeded";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inContentTable )
			throw new ReportConfigException( kExTag +
			"Null parent content container passed in."
			);

		//////////////////////////////////////////////////
		if( inIsDoingPaging && ! inIsShowingAllRecords )
		{
			Element prevElem = generatePrevPageLink(
				inActualFirstRowDisplayed, inDesiredRowCount,
				inHasPrevRecords, inRequestObject
				);
			Element nextElem = generateNextPageLink(
				inActualLastRowDisplayed, inDesiredRowCount,
				inHasMoreRecords, inRequestObject
				);
	
			// Do it if at least one is not null, they BOTH should
			// not be null, but if there was a problem, one might,
			// but it will have generated warnings already
			if( null != prevElem || null != nextElem )
			{
	
				// The stats row
				Element pagingRowElem = new Element( "tr" );
				pagingRowElem.setAttribute(
					"class", CSSClassNames.PAGING_LINK_ROW
					);
				// Add this row to the content
				inContentTable.addContent( pagingRowElem );
				// Now create the single cell for this row
				Element outerPagingCell = new Element( "td" );
				outerPagingCell.setAttribute( "colspan", ""+inColumnCount );
				// Attach it to the row
				pagingRowElem.addContent( outerPagingCell );
			
				// Now create the inner table for this cell
				Element innerTable = new Element( "table" );
				// A hard coded style item
				innerTable.setAttribute( "width", "100%" );
				// Add this table to the bounding cell
				outerPagingCell.addContent( innerTable );
			
				// Now create the top level row for the inner table
				Element innerPagingRowElem = new Element( "tr" );
				// Add that row to the table
				innerTable.addContent( innerPagingRowElem );
	
				// Now create the cell for the prev page message
				Element prevCellElem = new Element( "td" );
				prevCellElem.setAttribute(
					"class", CSSClassNames.PAGING_LINK_CELL
					);
				// Another hard coded style item
				prevCellElem.setAttribute(
					"align", "left"
					);
				// Add this cell to the row
				innerPagingRowElem.addContent( prevCellElem );
				// Add the content to this cell
				prevCellElem.addContent( prevElem );
			
				// Now create the cell for the next page message
				Element nextCellElem = new Element( "td" );
				nextCellElem.setAttribute(
					"class", CSSClassNames.PAGING_LINK_CELL
					);
				// Another hard coded style item
				nextCellElem.setAttribute(
					"align", "right"
					);
				// Add this cell to the row
				innerPagingRowElem.addContent( nextCellElem );
				// Add the content to this cell
				nextCellElem.addContent( nextElem );

				return true;
			}
			else	// Else no, somehow we didn't add any
				return false;
		}
		else	// Else no paging links required
			return false;
	}






	public BaseReport(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortReportName
		)
			throws ReportConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// We'd like at least a minimal report name
		inShortReportName = NIEUtil.trimmedStringOrNull(
			inShortReportName
			);
		if( null==inShortReportName )
			throw new ReportConfigException( kExTag
				+ "Null/empty report name passed in."
				);
		fShortReportName = inShortReportName;

		// we need the main app for various info
		if( null==inMainConfig )
			throw new ReportConfigException( kExTag
				+ "Null application configuration object passed in"
				+ " for report " + getReportName()
				);
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		// Cache some values
		fUseCache = false;
		getCssStyleText();
		fUseCache = true;
	}



	abstract public String getTitle( Hashtable inValuesHash );

	abstract public String getLinkText( Hashtable inValuesHash );

	// It's OK not to have a subtitle
	public abstract String getSubtitleOrNull( Hashtable inValuesHash );




	public boolean verifyAccessLevel( AuxIOInfo inRequestInfo ) {
		final String kFName = "verifyAccessSecurityLevel";
		int currentLevel = -1;
		if( null!=inRequestInfo )
			currentLevel = inRequestInfo.getAccessLevel();
		else
			errorMsg( kFName, "Null reqeust info sent in, defaulting to access level " + currentLevel );
		int requiredLevel = getRequiredAccessLevel();
		return currentLevel >= requiredLevel;
	}
	abstract public int getRequiredAccessLevel();



	public abstract Element runReport(
			AuxIOInfo inRequestObject, AuxIOInfo inResponseObject,
			boolean inDoFullPage
			)
				throws ReportException
			;

	protected List getSuggestedLinksOrNull()
	{
		return null;
	}

	public Element generateLinkToMainMenu(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException
	{
		final String kFName = "generateMainMenuLink";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		final String kLinkText = "Return to the Main Menu";

		if( null == inRequestInfo
			|| null == inResponseInfo
			)
		{
			throw new ReportException( kExTag
				+ "Null request/reponse object."
				);
		}

		// Our eventual response, barring any exceptions
		Element outElem = new Element( "a" );


		// Get the link
		////////////////////////////////////////////

		// We use AuxIOInfo to build a link
		AuxIOInfo newLinkInfo = new AuxIOInfo();
		// Prime it with the basic URL we want
		newLinkInfo.setBasicURL( getMainAppURL() );

		// Copy over existing CGI values
		// We have to say which fields we DON'T want
		//	List excludeFields = new Vector();
		//	excludeFields.add( ReportDispatcher.REPORT_NAME_CGI_FIELD );
		//	excludeFields.add( ReportConstants.START_ROW_CGI_FIELD_NAME );
		//	excludeFields.add( ReportConstants.DESIRED_ROW_COUNT_CGI_FIELD_NAME );
		//	excludeFields.add( ReportConstants.SORT_SPEC_CGI_FIELD_NAME );
		//	excludeFields.add( ReportConstants.FILTER_NAME_CGI_FIELD_NAME );
		//	excludeFields.add( ReportConstants.FILTER_PARAM_CGI_FIELD_NAME );
		//	// Now do the copy
		//	newLinkInfo.copyInCGIFields( inRequestInfo, excludeFields );
		newLinkInfo.copyInCGIFields( inRequestInfo, ReportConstants.fMiscReportFields );

		// Now get the full href back
		String href = newLinkInfo.getFullCGIEncodedURL();

		if( null==href )
			throw new ReportException( kExTag
				+ "Got back null href, this link will not be created."
				);


		// Now create the final XHTML element
		// we use <div> if not an active link
		outElem.setAttribute( "href", href );
		// outElem.setAttribute( "class", "nie_menu_link" );
		outElem.setAttribute(
			"class", CSSClassNames.ACTIVE_MENU_LINK
			);


		// And add the display text
		outElem.addContent( kLinkText );

		// Return the tag!
		return outElem;
	}



	Element generateMainMenuInset(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException
	{
		nie.sr2.ReportDispatcher dispatcher =
			getMainConfig().getReportDispatcher();
		return dispatcher.generateMainMenuCompact( inRequestInfo, inResponseInfo );
	}



	protected Element generatePlainLinkToThisReport(
		AuxIOInfo inRequest, boolean inIsForMenu
		)
	{
		// Create a new anchor tag
		Element anchor = new Element( "a" );
		anchor.setAttribute(
			"class",
			( inIsForMenu ? CSSClassNames.ACTIVE_MENU_LINK : CSSClassNames.ACTIVE_RPT_LINK )
			);

		// Add the link text
		anchor.addContent( getLinkText(null) );

		// Create a repository for link info
		AuxIOInfo linkInfo = new AuxIOInfo();
		linkInfo.setBasicURL( getMainAppURL() );

		linkInfo.copyInCGIFields( inRequest, (inIsForMenu ? ReportConstants.fMiscReportFields : null) );

		// Set the report name to us
		linkInfo.setOrOverwriteCGIField(
			ReportConstants.REPORT_NAME_CGI_FIELD,
			getReportName()
			);

		// Get the full URL and add it to the anchor
		String href = linkInfo.getFullCGIEncodedURL();
		anchor.setAttribute( "href", href );


		return anchor;
	}


	protected Element generatePlainLinkToThisReportCompact(
		AuxIOInfo inRequest, boolean inIsForMenu
		)
	{
		// Create a new anchor tag
		Element anchor = new Element( "a" );
		anchor.setAttribute(
			"class",
			( inIsForMenu ? CSSClassNames.ACTIVE_MENU_LINK : CSSClassNames.ACTIVE_RPT_LINK )
			);

		// Add the link text
		anchor.addContent( getLinkText(null) );

		// Create a repository for link info
		AuxIOInfo linkInfo = new AuxIOInfo();
		linkInfo.setBasicURL( getMainAppURL() );
		linkInfo.copyInCGIFields( inRequest, (inIsForMenu ? ReportConstants.fMiscReportFields : null) );

		// Set the report name to us
		linkInfo.setOrOverwriteCGIField(
			ReportConstants.REPORT_NAME_CGI_FIELD,
			getReportNameCompact()
			);

		// Get the full URL and add it to the anchor
		String href = linkInfo.getFullCGIEncodedURL();
		anchor.setAttribute( "href", href );


		return anchor;
	}




	public Element generateMenuLinksToThisReport( AuxIOInfo inRequest )
	{
		return generatePlainLinkToThisReport( inRequest, true );
	}


	public void generateMenuLinksToThisReportCompact(
		AuxIOInfo inRequest, Element inTopRow, Element inBottomRow
		)
	{
		Element link = generatePlainLinkToThisReport( inRequest, true );

		Element linkElem = JDOMHelper.findOrCreateElementByPath(
			inTopRow,
			"th[+]/@rowspan=2/@align=center/@valign=middle"
			// + "/@class=" + CSSClassNames.CONTAINER_CELL
			// + "/div/@class="
			// + CSSClassNames.INACTIVE_RPT_LINK
			// + CSSClassNames.INACTIVE_MENU_LINK
			,
			true
			);
		linkElem.addContent( link );
	}



	protected Element [] prepareBaseOutputTree(
		boolean inDoFullPage,
		Hashtable optVariablesHash,
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException
	{

		return prepareBaseOutputTree(
			inDoFullPage,
			optVariablesHash,
			true,	// Usually we DO add titles
			inRequestInfo,
			inResponseInfo
			);

	}

	// We need a base element, either the top of an HTML page, or at least the part
	// we will jam ourselves into
	// Also, we need to know the main TABLE element we are to put our new stuff into
	// Since the 2nd element is a TABLE, it should have <tr>'s added to it
	protected Element [] prepareBaseOutputTree(
		boolean inDoFullPage,
		Hashtable optVariablesHash,
		boolean inAddTitles,
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException
	{
		final String kFName = "prepareBaseOutputTree";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		// Whether or not to force a style element into the
		// stream, even if we're not formulating the entire page
		final boolean kForceCSS = true;

		// The two key points in the tree
		Element outElem = null;
		Element contentHanger = null;
		String title = getTitle( optVariablesHash );

		// Also, have the CSS stuff ready to go if we need it
		Element styleElem = null;
		String css = getCssStyleText();
		if( null!=css )
		{
			// Add newlines to it for source readability
			css = NIEUtil.NL + css + NIEUtil.NL;
			// Todo: this should go inside HTML comments as well
			// Create the style element and add the content
			styleElem = new Element( "style" );
			styleElem.setAttribute( "type", "text/css" );
			Comment lComment = new Comment( css );
			// styleElem.addContent( css );
			styleElem.addContent( lComment );
		}


		// If no template, build from scratch
		if( inDoFullPage )
		{
			// The eventual answer
			outElem = new Element( "html" );
			// Build up the heading
			Element headElem = new Element( "head" );
			// Hold off adding it to the main doc until we see
			// if we add anything
			boolean haveAddedAnythingToHead = false;
			// Add the CSS element, if any
			if( null!=styleElem )
			{
				headElem.addContent( styleElem );
				haveAddedAnythingToHead = true;
			}
			// Add the title, if any
			if( inAddTitles && null!=title )
			{
				Element titleElem = new Element( "title" );
				titleElem.addContent( title );
				headElem.addContent( titleElem );
				haveAddedAnythingToHead = true;
			}
			// Only add the heading to the document if we've actually
			// put something in it
			if( haveAddedAnythingToHead )
				outElem.addContent( headElem );
	
			// Start building the body tag
			Element bodyElem = new Element( "body" );
			outElem.addContent( bodyElem );

			// We like the main layout centered
			Element mainCenterElem = new Element( "center" );
			bodyElem.addContent( mainCenterElem );
			contentHanger = mainCenterElem;
	
		}
		// Else there IS a template, so we build a much
		// shorter document
		else
		{
			// We like the main layout centered
			Element mainCenterElem = new Element( "center" );
			outElem = mainCenterElem;
			contentHanger = mainCenterElem;

			// Add the CSS element, if any
			if( null!=styleElem && kForceCSS )
			{
				contentHanger.addContent( styleElem );
			}
		}

		// The TOP menu
		Element menuInset = generateMainMenuInset( inRequestInfo, inResponseInfo );
		Element menuInsetClone = null;
		if( null!=menuInset ) {
			try {
				menuInsetClone = (Element) menuInset.clone();
			}
			catch( Exception e ) {
				throw new ReportException( kExTag +
					"Error cloning menu bar: " + e
					);
			}
			contentHanger.addContent( menuInset );
		}

		// Add the title, if any
		if( inAddTitles && null!=title )
		{
			// A little vertical space
			/***
			Element spacer = JDOMHelper.findOrCreateElementByPath(
				contentHanger,
				"tr[+]/td",
				true
				);
			spacer.addContent( ""+ NIEUtil.NBSP );
			spacer.addContent( new Element( "br") );
			***/
			contentHanger.addContent( ""+ NIEUtil.NBSP );
			contentHanger.addContent( new Element( "br") );

			// Element titleElem = new Element( "h2" );
			Element titleElem = new Element( "b" );
			titleElem.setAttribute( "class", CSSClassNames.RPT_TITLE_TEXT );
			titleElem.addContent( title );
			// bodyElem.addContent( titleElem );
			contentHanger.addContent( titleElem );
		}
		// Add the subtitle, if any
		String subtitle = getSubtitleOrNull( optVariablesHash );
		if( inAddTitles && null!=subtitle )
		{
			contentHanger.addContent( new Element( "br" ) );
			// Element subtitleElem = new Element( "h3" );
			Element subtitleElem = new Element( "b" );
			subtitleElem.setAttribute( "class", CSSClassNames.RPT_SUBTITLE_TEXT );
			subtitleElem.addContent( subtitle );
			// bodyElem.addContent( subtitleElem );
			contentHanger.addContent( subtitleElem );
			contentHanger.addContent( new Element( "br" ) );
		}
		// A little vertical space
		/***
		Element spacer = JDOMHelper.findOrCreateElementByPath(
			contentHanger,
			"tr[+]/td",
			true
			);
		spacer.addContent( ""+ NIEUtil.NBSP );
		spacer.addContent( new Element( "br") );
		***/
		// contentHanger.addContent( ""+ NIEUtil.NBSP );
		// contentHanger.addContent( new Element( "br") );


		// The main-content table helps place the
		// results grid, statistics message and
		// paging links

		Element mainContentTable = new Element( "table" );
		mainContentTable.setAttribute( "class", CSSClassNames.MAIN_CONTENT_TABLE );
		// Add it to the overall content stream
		// bodyElem.addContent( mainContentTable );
		contentHanger.addContent( mainContentTable );

		/***
		Element mainContentTable = JDOMHelper.findOrCreateElementByPath(
			contentHanger,
			"tr[+]/td/table/@class=" + CSSClassNames.MAIN_CONTENT_TABLE
			,
			true
			);
		***/



		// NOTE:
		// We hold off adding rows to this, so we can put them in
		// the proper order AFTER the main loop runs

		// Add the BOTTOM horizontal menu
		////////////////////////////////////////////
		/***
		spacer = JDOMHelper.findOrCreateElementByPath(
			contentHanger,
			"tr[+]/td",
			true
			);
		spacer.addContent( ""+ NIEUtil.NBSP );
		spacer.addContent( new Element( "br" ) );
		***/
		if( null!=menuInsetClone ) {
			contentHanger.addContent( ""+ NIEUtil.NBSP );
			// contentHanger.addContent( new Element( "br" ) );
			contentHanger.addContent( new Element( "p" ) );
			contentHanger.addContent( ""+ NIEUtil.NBSP );
			// contentHanger.addContent( new Element( "p" ) );
			contentHanger.addContent( new Element( "br" ) );
			contentHanger.addContent( menuInsetClone );
		}

		// We send back a two item array
		Element [] answer = new Element [] { outElem, mainContentTable };
		return answer;

	}


	protected Hashtable getVariables( AuxIOInfo inRequestObject )
	{
		Hashtable outMasterValuesHash = null;
		if( getShouldDoVariableSubstitutions() )
		{
			outMasterValuesHash = new Hashtable();

			// Start assmbling the hash

			// The cgi variables
			Hashtable cgiHash = getAllRequestHashes( inRequestObject );
			outMasterValuesHash.putAll( cgiHash );

			// System like hashes, sometimes based on transformed CGI fields
			Hashtable sysHash = getAllSystemHashes( inRequestObject );
			outMasterValuesHash.putAll( sysHash );

			// TODO: others...
		}
		return outMasterValuesHash;
	}


	Element generatePrevPageLink(
		int inCurrFirstRecord, int inNumRequested,
		boolean hasPrevRecords, AuxIOInfo inRequest
		)
	{
		final String kFName = "generatePrevPageLink";

		final String kLinkText = "<< Prev";

		String href = null;

		if( hasPrevRecords )
		{

			// We use AuxIOInfo to build a link
			AuxIOInfo newLinkInfo = new AuxIOInfo();
			// Prime it with the basic URL we want
			newLinkInfo.setBasicURL( getMainAppURL() );
	
			// Copy over existing CGI values
			newLinkInfo.copyInCGIFields( inRequest );
	
			// Now figure out which row we want to ask for
			int newRow = inCurrFirstRecord - inNumRequested;
			if( newRow < 1 )
				newRow = 1;

			// If this would actually expose previous records,
			// go ahead and create the link
			if( newRow < inCurrFirstRecord )
			{
				// Put our new start row in place of any old one
				newLinkInfo.setOrOverwriteCGIField(
					ReportConstants.START_ROW_CGI_FIELD_NAME,
					"" + newRow
					);
		
				// Now get the full href back
				href = newLinkInfo.getFullCGIEncodedURL();
	
				if( null==href)
					errorMsg( kFName,
						"Got back null href, this link will not be active."
						);
			}
		}

		// Now create the final XHTML element
		// we use <div> if not an active link
		Element newTag = null;
		if( null!=href )
		{
			newTag = new Element( "a" );
			newTag.setAttribute( "href", href );
			newTag.setAttribute( "class", CSSClassNames.ACTIVE_PAGING_LINK );
		}
		// Inactive link
		else
		{
			newTag = new Element( "div" );
			newTag.setAttribute( "class", CSSClassNames.INACTIVE_PAGING_LINK );
		}
		// And add the display text
		newTag.addContent( kLinkText );

		// Return the tag!
		return newTag;

	}


	Element generateNextPageLink(
		int inCurrLastRecord, int inNumRequested,
		boolean hasMoreRecords, AuxIOInfo inRequest
		)
	{
		final String kFName = "generateNextPageLink";

		final String kLinkText = "Next >>";

		String href = null;

		if( hasMoreRecords )
		{

			// We use AuxIOInfo to build a link
			AuxIOInfo newLinkInfo = new AuxIOInfo();
			// Prime it with the basic URL we want
			newLinkInfo.setBasicURL( getMainAppURL() );
	
			// Copy over existing CGI values
			newLinkInfo.copyInCGIFields( inRequest );
	
			// Now figure out which row we want to ask for
			int newRow = inCurrLastRecord + 1;

			// Put our new start row in place of any old one
			newLinkInfo.setOrOverwriteCGIField(
				ReportConstants.START_ROW_CGI_FIELD_NAME,
				"" + newRow
				);
	
			// Now get the full href back
			href = newLinkInfo.getFullCGIEncodedURL();

			if( null==href)
				errorMsg( kFName,
					"Got back null href, this link will not be active."
					);
		}

		// Now create the final XHTML element
		// we use <div> if not an active link
		Element newTag = null;
		if( null!=href )
		{
			newTag = new Element( "a" );
			newTag.setAttribute( "href", href );
			newTag.setAttribute( "class", CSSClassNames.ACTIVE_PAGING_LINK );
		}
		// Else inactive link
		else
		{
			newTag = new Element( "div" );
			newTag.setAttribute( "class", CSSClassNames.INACTIVE_PAGING_LINK );
		}
		// And add the display text
		newTag.addContent( kLinkText );

		// Return the tag!
		return newTag;

	}






	// If -1, then they DON'T WANT row counts
	// Todo: revisit in case this is a security issue, let it
	// be settable and enforceable
	public int getStartRow( AuxIOInfo inRequest )
	{
		final String kFName = "getStartRow";
		final int kDefaultStartRow = 1;
		// ^^^ No this does not need to be a class constant
		if( null==inRequest )
		{
			errorMsg( kFName,
				"No request object given."
				+ " Returning default start row of " + kDefaultStartRow
				);
			return kDefaultStartRow;
		}
		int outRow = inRequest.getIntFromCGIField(
			ReportConstants.START_ROW_CGI_FIELD_NAME, kDefaultStartRow
			);
		return outRow;
	}

	public int getDesiredRowCount( AuxIOInfo inRequest )
	{
		final String kFName = "getDesiredRowCount";
		if( null==inRequest )
		{
			errorMsg( kFName,
				"No request object given."
				+ " Returning default number of rows "
				+ DEFAULT_DESIRED_ROW_COUNT
				);
			return DEFAULT_DESIRED_ROW_COUNT;
		}
		int outRows = inRequest.getIntFromCGIField(
			ReportConstants.DESIRED_ROW_COUNT_CGI_FIELD_NAME, DEFAULT_DESIRED_ROW_COUNT
			);
		// They may NOT want row counting
		if( outRows <= 0 )
			outRows = -1;
		return outRows;
	}
	// Todo: settable row counts by report, and enforceable




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


	static private void __sep__Special_Date_Time_DB_Vendor_handling__(){}
	//////////////////////////////////////////////////////////////////////////


	Hashtable getAllSystemHashes( AuxIOInfo inRequest )
	{
		final String kFName = "getAllSystemHashes";
		Hashtable outHash = new Hashtable();
		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in. Returning empty hash."
				);
			return outHash;
		}

		int days = inRequest.getIntFromCGIField( ReportConstants.DAYS_OLD_CGI_FIELD_NAME, 0 );

		// Get the dates
		String fullStartStr = getDBConfig().calculateDateTimeStringForNDaysPast( days, false, true );
		String truncStartStr = getDBConfig().calculateDateTimeStringForNDaysPast( days, true, true );
		// Ending dates are for today
		String fullEndStr = getDBConfig().calculateDateTimeStringForNDaysPast( 0, false, true );
		String truncEndStr = getDBConfig().calculateDateTimeStringForNDaysPast( 0, true, true );

		Hashtable myHash = new Hashtable();
		if( null != fullStartStr )
			myHash.put( ReportConstants.FILTER_DATETIME_START_FIELD_NAME, fullStartStr );
		if( null != truncStartStr )
			myHash.put( ReportConstants.FILTER_DATE_ONLY_START_FIELD_NAME, truncStartStr );
		if( null != fullEndStr )
			myHash.put( ReportConstants.FILTER_DATETIME_END_FIELD_NAME, fullEndStr );
		if( null != truncEndStr )
			myHash.put( ReportConstants.FILTER_DATE_ONLY_END_FIELD_NAME, truncEndStr );

		// The nulll value method name
		myHash.put( ReportConstants.SYSTEM_VAR_FOR_DB_VENDOR_NVL_METHOD,
			getDBConfig().getVendorNullValueMethodName()
			);

		// Store all this as the system hash
		outHash.put( SYSTEM_VARS_HASH_NAME, myHash );

		// Todo: could do other hashes

		return outHash;
	}


	Hashtable _getAllSystemHashesV1( AuxIOInfo inRequest )
	{
		final String kFName = "getAllSystemHashes";
		Hashtable outHash = new Hashtable();
		if( null == inRequest )
		{
			errorMsg( kFName,
				"Null request object passed in. Returning empty hash."
				);
			return outHash;
		}

		int days = inRequest.getIntFromCGIField( ReportConstants.DAYS_OLD_CGI_FIELD_NAME, Integer.MIN_VALUE );
		// If an Interval was set, make the strings
		if( days > Integer.MIN_VALUE ) {

			// We start with a calendar to help with date math
			Calendar lCalendar = Calendar.getInstance();

			// This gives us "now"
			// we consider now to be the END date, if anybody wants to use it

			// String pastDate = getDateString( lCalendar );
			java.util.Date endDate = lCalendar.getTime();

			// This is for the truncated stuff
			String truncEndStr = NIEUtil.formatDateToString(
				endDate, ReportConstants.DEFAULT_SQL_DATE_FORMAT, true
				);
			truncEndStr = "'" + truncEndStr + "'";

			// We use Oracle's format here, everybody else seems to get it
			String fullEndStr = NIEUtil.formatDateToString(
				endDate, ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT, true
				);
			fullEndStr = "'" + fullEndStr + "'";

			// For Oracle full date parsing, we need to wrap this in their to_date function
			if( getDBConfig().getConfiguredVendorTag().startsWith(DBConfig.VENDOR_TAG_ORACLE) )
				fullEndStr = "TO_DATE( " + fullEndStr + ", '"
					+ ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT + "' )"
					;

			// Now we SUBTRACT the number of days
			lCalendar.add( Calendar.DATE, - days );
			java.util.Date startDate = lCalendar.getTime();

			// This is for the truncated stuff
			String truncStartStr = NIEUtil.formatDateToString(
				startDate, ReportConstants.DEFAULT_SQL_DATE_FORMAT, true
				);
			truncStartStr = "'" + truncStartStr + "'";

			// We use Oracle's format here, everybody else seems to get it
			String fullStartStr = NIEUtil.formatDateToString(
				startDate, ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT, true
				);
			fullStartStr = "'" + fullStartStr + "'";

			// For Oracle full date parsing, we need to wrap this in their to_char function
			if( getDBConfig().getConfiguredVendorTag().startsWith(DBConfig.VENDOR_TAG_ORACLE) )
			fullStartStr = "TO_DATE( " + fullStartStr + ", '"
					+ ReportConstants.DEFAULT_ORACLE_DATETIME_FORMAT + "' )"
					;

			Hashtable myHash = new Hashtable();
			myHash.put( ReportConstants.FILTER_DATETIME_START_FIELD_NAME, fullStartStr );
			myHash.put( ReportConstants.FILTER_DATE_ONLY_START_FIELD_NAME, truncStartStr );
			myHash.put( ReportConstants.FILTER_DATETIME_END_FIELD_NAME, fullEndStr );
			myHash.put( ReportConstants.FILTER_DATE_ONLY_END_FIELD_NAME, truncEndStr );

			outHash.put( SYSTEM_VARS_HASH_NAME, myHash );
		}

		return outHash;
	}

	public static String getDateString( Calendar inCalendar )
	{
		int lDay = inCalendar.get(Calendar.DAY_OF_MONTH);
		int lMonth = inCalendar.get(Calendar.MONTH);
		int lYear = inCalendar.get(Calendar.YEAR);
	
		String retString = "" + lDay + "-" + MONTH_NAMES_ARRAY[lMonth] + "-" + lYear;

		return retString;
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

	protected String getReportName()
	{
		return fShortReportName;
	}

	protected String getReportNameCompact()
	{
		if( null!=fTerseReportName )
			return fTerseReportName;
		else
			return getReportName();
	}




	public boolean shouldRouteSearchLinksViaOurProxy() {
	    return 	ReportConstants.DEFAULT_SEARCH_LINK_VIA_OUR_PROXY
	    	|| ! getMainConfig().getSearchEngineMethod().equalsIgnoreCase("GET")
	    	;
	}



	protected String getDefaultTableName()
	{
		return DBConfig.LOG_TABLE;
	}
	
	
	// protected String _getCssStyleSheetURI()
	// {
	// 	return _DEFAULT_CSS_URI;
	// }
	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// Todo: let them load their own from elsewhere
	protected String getCssStyleText()
	{
		return getMainConfig().getDefaultCssStyleTextOrNull();

		/***
		if( ! fUseCache && cCssText==null ) {
			final String kFName = "getCssStyleText";
	
			String uri = _getCssStyleSheetURI();
			if( null==uri )
			{
				infoMsg( kFName,
					"No CSS URI defined, returning null."
					);
				cCssText = null;
			}
			else {
				AuxIOInfo tmpAuxInfo = new AuxIOInfo();
				// tmpAuxInfo.setSystemRelativeBaseClassName( kFullClassName() );
				// Resolve system URLs relative to the main application config
				tmpAuxInfo.setSystemRelativeBaseClassName(
					getMainConfig().getClass().getName()
					);
				try
				{
					cCssText = NIEUtil.fetchURIContentsChar(
						uri,
						// getMainApp().getConfigFileURI(),
						getMainConfig().getConfigFileURI(),
						null, null,	// optUsername, optPassword,
						tmpAuxInfo
						);
				}
				catch( Exception e )
				{
					errorMsg( kFName,
						"Error opening CSS URI \"" + uri + "\"."
						+ " Returning null."
						+ " Error: " + e
						);
					cCssText = null;
				}
				// Normalize and check
				cCssText = NIEUtil.trimmedStringOrNull( cCssText );
				if( null==cCssText )
					errorMsg( kFName,
						"Null/empty default CSS style sheet contents read"
						+ " from URI \"" + uri + "\", returning null."
						);
			}

		}

		return cCssText;
		***/

		// Good resource on tables and CSS, from Nick Sayer
		// http://www.w3.org/TR/REC-CSS2/tables.html
		// And overall CSS info
		// http://www.w3.org/TR/REC-CSS2/cover.html#minitoc
		// Selectors / pattern matching
		// http://www.w3.org/TR/REC-CSS2/selector.html

//		Inside HTML:
//		<head>
//			...
//			<STYLE type="text/css">
//				H1 { color: blue }
//			</STYLE>
//		</head>

	}

	boolean getShouldDoVariableSubstitutions()
	{

//		if( ! fUseCache )
//		{
//			cShouldDoVarSubst = fMainElem.getBooleanFromAttribute(
//				SHOULD_DO_VAR_SUBST_ATTR, DEFAULT_SHOULD_DO_VAR_SUBST
//				);
//		}
//		return cShouldDoVarSubst;

		return fHaveSeenDollarSigns;
	}






	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fMainApp;
	}
	public nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	protected DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}

	public String getMainAppURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}




	// This gets us to the logging object
	public static RunLogInterface getRunLogObject()
	{
		return RunLogBasicImpl.getRunLogObject();
	}
	protected boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean shouldDoTransactionStatusMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTransactionStatusMsg( kClassName(), inFromRoutine );
	}
	protected boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean debugMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().debugMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName(), inFromRoutine );
	}
	protected boolean traceMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().traceMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean shouldDoTraceMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoTraceMsg( kClassName(), inFromRoutine );
	}
	protected boolean warningMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().warningMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}
	protected boolean errorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().errorMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}


	protected boolean stackTrace( String inFromRoutine,
	        Exception e, String optMessage
	) {
	    return getRunLogObject().stackTrace( kClassName(), inFromRoutine,
	            e, optMessage
	            );
	}

	protected boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName(), inFromRoutine,
			"" + getReportName() + ": " +
			inMessage
			);
	}




	private nie.sn.SearchTuningApp _fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;
	private String fShortReportName;
	private String fTerseReportName;

	boolean fUseCache;
	boolean fHaveSeenDollarSigns;
	String cCssText;



	boolean cShouldDoVarSubst;


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

	static final String SYSTEM_VARS_HASH_NAME = "system";



	// The default number of rows
	static final int DEFAULT_DESIRED_ROW_COUNT = 25; // 25; // 25;

	// Where CSS style sheets come from
	// public static final String _x_DEFAULT_CSS_URI =
	//	AuxIOInfo.SYSTEM_RESOURCE_PREFIX
	//	// + "style_sheets/default_xml_defined_report.css"
	//	+ "static_files/style_sheets/nie.css"
	//	;
	// ^^^ see SearchTuningConfig

	static final boolean DEFAULT_SHOULD_DO_VAR_SUBST = true;

	// Some of the class tags we use, others are hard coded if
	// used only once
	private static final String _ACTIVE_PAGING_CSS_CLASS =
		"nie_active_paging_link";
	private static final String _INACTIVE_PAGING_CSS_CLASS =
		"nie_inactive_paging_link";
	private static final String _STAT_NUMBER_CSS_CLASS =
		"nie_stat_number";
	public static final String _CONAINER_CELL_CSS_CLASS =
		"nie_container_cell";

	static final String MONTH_NAMES_ARRAY [] = {
		"JAN",	"FEB",	"MAR",	"APR",	"MAY",	"JUN",
		"JUL",	"AUG",	"SEP",	"OCT",	"NOV",	"DEC"
		};



}

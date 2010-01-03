package nie.sr2;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.jdom.Element;
import org.jdom.Comment;
import nie.core.*;
import nie.sn.SearchTuningApp;
import nie.sn.CSSClassNames;
import nie.sn.SnRequestHandler;


public class ReportDispatcher
{
	private static final String kClassName = "ReportDispatcher";
	// We need this when referencing our class by name
	private final static String kFullClassName = "nie.sr2." + kClassName;

	public ReportDispatcher(
		Element inConfigElem,
		nie.sn.SearchTuningConfig inMainConfig
		// nie.sn.SearchTuningApp inMainApp
		)
			throws ReportConfigException
	{
		final String kFName = "constructor";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// This will throw an exception if anything is
		// wrong, it also checks fMainApp for null
		fReportsConfig = new SearchReportingConfig(
			inConfigElem, inMainConfig // inMainApp
			);

		// Looking good

		// Save this
		// fMainApp = inMainApp;
		fMainConfig = inMainConfig;

		// Init some hashes
		fCachedXMLReports = new Hashtable();
		fSearchedForXMLReports = new HashSet();
		fCachedJavaReports = new Hashtable();
		fSearchedForJavaReports = new HashSet();

	}

	public String dispatch(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException, ReportConfigException
	{
		final String kFName = "dispatch";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null == inRequestInfo
			|| null == inResponseInfo
			)
		{
			throw new ReportException( kExTag
				+ "Null request/reponse object."
				);
		}

		// Our eventual response, barring any exceptions
		Element outElem = null;

		String reportName = inRequestInfo.getScalarCGIField(
			ReportConstants.REPORT_NAME_CGI_FIELD
			);
		reportName = NIEUtil.trimmedStringOrNull( reportName );

		// An overall display template
		String reportTemplate = getReportTemplate();

		// If no report, give the menu
		if( null == reportName )
		{
			// Todo: Give list of reports
			// throw new ReportException( kExTag
			//	+ "No report name given."
			//	);

			/***
			outElem = generateMainMenuFullPage(
				inRequestInfo,
				inResponseInfo,
				( null != reportTemplate )
				);
			***/

			// TODO: this is slightly wrong, we're hard coded to check the
			// permissions on PopSearchLinked
			if( getMainConfig().getHasDbBasedMaps()
				&& inRequestInfo.getAccessLevel() >= nie.sr2.java_reports.PopSearchLinked.ACCESS_LEVEL
				)
			{
				reportName = DEFAULT_REPORT_NAME_WITH_UI; // "PopSearchLinked";
			}
			else {
				reportName = DEFAULT_REPORT_NAME_NO_UI; // "popular_searches_ndays";
			}

			// The default report
			// reportName = getMainConfig().getHasDbBasedMaps()
			//	? DEFAULT_REPORT_NAME_WITH_UI : DEFAULT_REPORT_NAME_NO_UI;

			// Default parameter for that report
			if( null!=DEFAULT_REPORT_PARM_NAME ) {
				if( null!=DEFAULT_REPORT_PARM_VALUE )
					inRequestInfo.setOrOverwriteCGIField( 
						DEFAULT_REPORT_PARM_NAME, DEFAULT_REPORT_PARM_VALUE
						);
				else
					inRequestInfo.setOrOverwriteCGIField( 
						DEFAULT_REPORT_PARM_NAME, ""
						);
			}

		}
		// // Else a specific report was requested
		// else
		// {

			// Track down the report
			ReportInterface theReport = locateReport( reportName );
			if( null == theReport )
				throw new ReportException( kExTag
					+ "Unable to locate/load/configure report \""
					+ reportName + "\". Error - got back a null report."
					);

			// Now run it!
			// It may throw plain old report run time exceptions
			try {

				// We check security here to save each reprot from worrying about it
				// if( ! getHasProperPassword() )
				// if( inRequestInfo.getAccessLevel() < theReport.getMinimumSecurityLevelRequired() )
				if( ! theReport.verifyAccessLevel(inRequestInfo) )
					throw new ReportException( kExTag
						+ "Insufficient priveledges to run report \"" + reportName + "\"."
						+ " Please check that you are using the correct password for this report."
						+ " Reqquired = " + theReport.getRequiredAccessLevel()
						+ ", presented = " + inRequestInfo.getAccessLevel()
						);

				outElem = theReport.runReport(
					inRequestInfo,
					inResponseInfo,
					( null == reportTemplate )
					);
			}
			catch( Throwable e ) {
				String msg = null;
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter( sw );
					e.printStackTrace(pw);
					sw.close();
					msg = e.toString() + " STACK: " + sw.toString();
				}
				catch( Exception e2 ) {
					msg = e.toString() + " - NO STACK TRACE, Error: " + e2;
				}
				throw new ReportException( kExTag
					+ " Error running Report \"" + reportName + "\", Error: " + msg
					);
			}
	
			// Sanity check
			if( null==outElem )
				throw new ReportException( kExTag
					+ "Got back null XHTML report element."
					);


		// }
		
		// By now we have a jdom tree that is either
		// a menu of reports, or the results of a specific report

		// Convert it to a String (XHTML)
		String outDoc = null;

		// If there's NO template
		if( null == reportTemplate )
		{
		 	outDoc = JDOMHelper.JDOMToString( outElem, true );
		}
		// Else they ARE using a template
		else
		{

			// Format what we got back as a string to
			// substitute into the final page
		 	String snippet = JDOMHelper.JDOMToString( outElem, true );

			// get the marker text and options
			List patterns = getReportingConfig().getMarkerLiteralText();
			if( null == patterns || patterns.size() < 1 )
			{
				errorMsg( kFName,
					"No patterns defined to mark substitution, returning null."
					);
				return null;
			}
	
			// Get some other flags from the Search Engine config
			boolean goesAfter =
				getReportingConfig().getIsMarkerNewTextInsertAfter();
			// statusMsg( kFName, "goesAfter=" + goesAfter );
			boolean doesReplace =
				getReportingConfig().getIsMarkerReplaced();
			boolean isCasen =
				getReportingConfig().getIsMarkerCaseSensitive();
	
	
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
			outDoc = NIEUtil.markupStringWtihString(
				reportTemplate, snippet, patterns,
				goesAfter, doesReplace, isCasen
				);
	
			// A final escape hatch to at least give them something
			if( null == outDoc )
			{
				errorMsg( kFName,
					"Did not find target pattern in report template."
					+ " Will return unformatted report."
					);
				// Just slap some HTML tags onto the snippet
				outDoc = "<html>" + snippet + "</html>";
			}

		}	// End if there was a template

		// Sanity check
		if( null==outDoc )
			throw new ReportException( kExTag
				+ "Got back null string from XHTML-to-String conversion."
				);

		// OK, we're done, return it!
		return outDoc;
	}


	Element generateMainMenuCompact(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo
		)
			throws ReportException
	{
		final String kFName = "generateMainMenuCompact";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( inRequestInfo.getAccessLevel() < ReportConstants.DEFAULT_ACCESS_LEVEL ) {
			infoMsg( kFName, "Access level for menu is too low, returning null." );
			return null;
		}

		// The list of reports
		// Todo: change this later
		Element mainTable = new Element( "table" );
		mainTable.setAttribute( "class", CSSClassNames.COMPACT_MENU_TOP_ROW);
		mainTable.setAttribute( "border", "" + ReportConstants.COMPACT_MENU_BORDER );

		// mainTable.setAttribute( "width", "100%" );
		// ^^^ No, spreads out menu too far to left and right
		mainTable.setAttribute( "cellpadding", "" + ReportConstants.COMPACT_MENU_CELL_PADDING );
		mainTable.setAttribute( "cellspacing", "" + ReportConstants.COMPACT_MENU_CELL_SPACING );
		mainTable.setAttribute( "valign", "center" );

		Element topRow = JDOMHelper.findOrCreateElementByPath(
			mainTable,
			"tr[+]/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW,
			true
			);

		Element bottomRow = JDOMHelper.findOrCreateElementByPath(
			mainTable,
			"tr[+]/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW,
			true
			);

		/***
		// Left and right filler cells
		// get rid of, per Miles and Sean

		String kLeftSpacerWidth="30%";
		String kRightSpacerWidth="30%";

		Element tlSpacerCell = JDOMHelper.findOrCreateElementByPath(
			topRow,
			// "td[+]/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
			"td[+]/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
			+ "/@width=" + kLeftSpacerWidth
			,
			true
			);
		Element blSpacerCell = JDOMHelper.findOrCreateElementByPath(
			bottomRow,
			"td[+]/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
			+ "/@width=" + kLeftSpacerWidth
			,
			true
			);
		***/

		Element tmpElem = null;

		// Popular Searches
		String reportName = null;
		if( getMainConfig().getHasDbBasedMaps()
			&& inRequestInfo.getAccessLevel() >= nie.sr2.java_reports.PopSearchLinked.ACCESS_LEVEL
			)
		{
			// reportName = "PopSearchLinked";
			reportName = "PopSearchLinked3";
		}
		else {
			reportName = "popular_searches_ndays";
		}
		// String reportName = getMainConfig().getHasDbBasedMaps()
		//	? DEFAULT_REPORT_NAME_WITH_UI : DEFAULT_REPORT_NAME_NO_UI;
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			// "popular_searches_ndays", null, null, null
			// "PopSearchLinked", null, null, null, topRow, bottomRow
			reportName, null, null, null, topRow, bottomRow
			);


		// No Hits
		if( getMainConfig().getHasDbBasedMaps()
			&& inRequestInfo.getAccessLevel() >= nie.sr2.java_reports.NoHitsLinked.ACCESS_LEVEL
			)
		{
			// reportName = "NoHitsLinked";
			reportName = "NoHitsLinked3";
		}
		else {
			reportName = "no_hits_ndays";
		}
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			// "no_hits_ndays", null, null, null
			// "NoHitsLinked", null, null, null, topRow, bottomRow
			reportName, null, null, null, topRow, bottomRow
			);

		// Too Many Hits
		if( getMainConfig().getHasDbBasedMaps()
			&& inRequestInfo.getAccessLevel() >= nie.sr2.java_reports.NoHitsLinked.ACCESS_LEVEL
			)
		{
			// reportName = "NoHitsLinked";
			reportName = "ManyHitsLinked3";
		}
		else {
			reportName = "many_hits_ndays";
		}
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			// "no_hits_ndays", null, null, null
			// "NoHitsLinked", null, null, null, topRow, bottomRow
			reportName, null, null, null, topRow, bottomRow
			);

		// Trend
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			// "no_hits_ndays", null, null, null
			"ActivityTrend", null, null, null, topRow, bottomRow
			);

		// Moded search, if active
		if( getMainConfig().hasSearchModes() ) {
			generateReportListingCompact(
				inRequestInfo, inResponseInfo,
				"moded_search_summary_ndays", null, null, null, topRow, bottomRow
				);
		}


		// Frequent Vistors
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			"frequent_visitors_ndays", null, null, null, topRow, bottomRow
			);

		// Number of searches
		generateReportListingCompact(
			inRequestInfo, inResponseInfo,
			"number_of_searches_ndays", null, null, null, topRow, bottomRow
			);


		// Advertisements, if active
		if( getMainConfig().hasUserClasses() ) {
			generateReportListingCompact(
				inRequestInfo, inResponseInfo,
				"advertiser_summary_ndays", null, null, null, topRow, bottomRow
				);
		}

		// raw_log_ndays
		// gone from here, per Miles and Sean

		// Admin link
		if( getMainConfig().getHasDbBasedMaps()
			&& inRequestInfo.getAccessLevel() >= nie.webui.xml_screens.QueryMaps.ACCESS_LEVEL
			)
		{



			try {
				nie.webui.UILink uiLink = new nie.webui.UILink(
					getMainConfig(),
					"QueryMaps",							// String Screen Name
					null,	// String inParmCGIName,
					"Mappings",	// String optLinkTitleText
					null
					);
	
				// Create the link
				// String topLinkText = "Administration";
				// String topLinkText = "Admin";
				// String bottomLinkText = "maps";
				String topLinkText = "Maps"; // "Administration";
				String bottomLinkText = "Find"; // "find map";
				String operation = nie.webui.UILink.UI_OPERATION_ADD;
				// mapFormLink = uiLink.generateLinkElement( inRequestInfo, linkText, operation, null );
				uiLink.generateLinkListingCompact(
					inRequestInfo, topLinkText, bottomLinkText,
					operation, null, topRow, bottomRow,
					2	// override top row colspan
					);

				ReportLink repLink = new ReportLink(
					getMainConfig(),		// nie.sn.SearchTuningConfig inMainConfig,
					"ListMapsForTerm3",	// String inReportName,
					"All",	// "all maps",			// String inLinkText,
					"Click to see all defined Maps",	// String optLinkTitle,
					CSSClassNames.ACTIVE_MENU_LINK,		//	String optCssClass,
					null,	//	String optParmName,
					null	//	String optParmDefaultValue
					);
				Element tmpLink = repLink.generateRichLink(
					inRequestInfo, null, null, true, null, null, null
					);

				// statusMsg( kFName, "all link" + NIEUtil.NL + JDOMHelper.JDOMToString( tmpLink, true) );

				Element tmpTd = new Element("td");
				tmpTd.setAttribute( "align", "center" );
				tmpTd.setAttribute( "class", CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL );
				bottomRow.addContent( tmpTd );
				tmpTd.addContent( tmpLink );

			}
			catch( Exception e ) {
				throw new ReportException( kExTag
					+ "Error creating UI linking object (1): " + e
					);
			}


			// Show All Maps
			/***
			generateReportListingCompact(
				inRequestInfo, inResponseInfo,
				"ListMapsForTerm3", null, "term", "*", topRow, bottomRow
				);
			***/

			// Track down the report
			/***
			final String tmpRptName = "ListMapsForTerm3";
			ReportInterface xReport = null;
			try {
				xReport = locateReport( tmpRptName );
			}
			catch( Exception e ) {
				throw new ReportException( kExTag
					+ "Unable to locate/load/configure report \""
					+ tmpRptName + "\". Error: " + e
					);
			}
			if( null == xReport )
				throw new ReportException( kExTag
					+ "Unable to locate/load/configure report \""
					+ tmpRptName + "\". Error - report was null."
					);
			xReport.generateMenuLinksToThisReportCompact(
				inRequestInfo,
				topRow, bottomRow
				);
			***/


		}


		// Admin Commands
		if( inRequestInfo.getAccessLevel() >= nie.webui.xml_screens.QueryMaps.ACCESS_LEVEL )
		{



			try {
				nie.sn.AdminLink refreshLink = new nie.sn.AdminLink(
					getMainConfig(),
					nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH,
					"Refresh",
					"Refresh/Re-Read Server Config (confirmation in new window)",
					null,
					null,
					null
					);

				Element refreshLinkElem = refreshLink.generateRichLink(
					inRequestInfo,
					nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH,
					null, null, null
					);

				nie.sn.AdminLink logsLink = new nie.sn.AdminLink(
					getMainConfig(),
					nie.sn.SnRequestHandler.ADMIN_CONTEXT_SHOW_MESSAGES,
					"Log",
					"View recent Server Log entries (opens in new window)",
					null,
					null,
					null
					);

				Element logsLinkElem = logsLink.generateRichLink(
					inRequestInfo,
					nie.sn.SnRequestHandler.ADMIN_CONTEXT_SHOW_MESSAGES,
					null, null, null
					);

				/***	
				// Create the link
				String topLinkText = "Server";
				String bottomLinkText = "Refresh";
				String command = nie.sn.SnRequestHandler.ADMIN_CONTEXT_REFRESH;
				// mapFormLink = uiLink.generateLinkElement( inRequestInfo, linkText, operation, null );
				adminLink.generateLinkListingCompact(
					inRequestInfo,
					topLinkText, bottomLinkText,
					command, null, topRow, bottomRow,
					2	// override top row colspan
					);
				***/

				Element topTh = new Element("th");
				topTh.setAttribute( "colspan", "2" );
				topTh.setAttribute( "align", "center" );
				topTh.setAttribute( "class", CSSClassNames.COMPACT_MENU_TOP_ROW_CELL );
				topRow.addContent( topTh );
				topTh.addContent( "Server" );



				// statusMsg( kFName, "all link" + NIEUtil.NL + JDOMHelper.JDOMToString( tmpLink, true) );

				Element tmpTd = new Element("td");
				tmpTd.setAttribute( "align", "center" );
				tmpTd.setAttribute( "class", CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL );
				bottomRow.addContent( tmpTd );
				tmpTd.addContent( refreshLinkElem );
				tmpTd = new Element("td");
				tmpTd.setAttribute( "align", "center" );
				tmpTd.setAttribute( "class", CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL );
				bottomRow.addContent( tmpTd );
				tmpTd.addContent( logsLinkElem );

			}
			catch( Exception e ) {
				throw new ReportException( kExTag
					+ "Error creating UI linking object (1): " + e
					);
			}




		}



		// Others?

		/***
		Element trSpacerCell = JDOMHelper.findOrCreateElementByPath(
			topRow,
			// "td[+]/@class=" + CSSClassNames.COMPACT_MENU_TOP_ROW_CELL
			"td[+]/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
			+ "/@width=" + kRightSpacerWidth
			,
			true
			);
		Element brSpacerCell = JDOMHelper.findOrCreateElementByPath(
			bottomRow,
			"td[+]/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
			+ "/@width=" + kRightSpacerWidth
			,
			true
			);
		***/

		// Done
		return mainTable;
	}

	Element generateMainMenuFullPage(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo,
		boolean inIsUsingReportTemplate
		)
			throws ReportException
	{
		final String kFName = "generateMainMenuFullPage";
		final String kExTag = kClassName + '.' + kFName + ": ";
	
		final String kTitle = "SearchTrack Reporting Main Menu";
	
		final int kCellPadding = 5; // 10
		final int kVerticalIndentPixels = 10; // 10
		// ^^^ there is also cell padding
	
		Element outElem = null;
		Element contentHanger = null;
	
		// If NOT using a template
		if( ! inIsUsingReportTemplate )
		{
	
			// The eventual answer
			outElem = new Element( "html" );
			// Build up the heading
			Element headElem = new Element( "head" );
			// Add the CSS text, if any
			String css = getCssStyleText();
			if( null!=css )
			{
				// Add newlines to it for source readability
				css = NIEUtil.NL + css + NIEUtil.NL;
				// Todo: this should go inside HTML comments as well
				// Create the style element and add the content
				Element styleElem = new Element( "style" );
				styleElem.setAttribute( "type", "text/css" );
				Comment lComment = new Comment( css );
				// styleElem.addContent( css );
				styleElem.addContent( lComment );
				headElem.addContent( styleElem );
			}
			Element titleElem = new Element( "title" );
			titleElem.addContent( kTitle );
			headElem.addContent( titleElem );
			outElem.addContent( headElem );
	
			// Start building the body tag
			Element bodyElem = new Element( "body" );
			outElem.addContent( bodyElem );
	
			// Center the whole thing
			Element centerElem = new Element( "center" );
			bodyElem.addContent( centerElem );
	
			contentHanger = centerElem;
	
		}
		// Else they ARE using a template, so output much simpler tree
		else
		{
			// Center the whole thing
			outElem = new Element( "center" );
			contentHanger = outElem;
		}
	
	
		// Add the title
		Element titleElem2 = new Element( "h1" );
		titleElem2.setAttribute( "class", CSSClassNames.RPT_TITLE_TEXT );
		titleElem2.addContent( kTitle );
		// bodyElem.addContent( titleElem2 );
		contentHanger.addContent( titleElem2 );
	
	
		// The list of reports
		// Todo: change this later
		Element mainTable = new Element( "table" );
	
		// for debug
		// mainTable.setAttribute( "border", "3" );
	
		// mainTable.setAttribute( "width", "100%" );
		// ^^^ No, spreads out menu too far to left and right
		mainTable.setAttribute( "cellpadding", "" + kCellPadding );
		mainTable.setAttribute( "valign", "top" );
		// bodyElem.addContent( mainTable );
		contentHanger.addContent( mainTable );
		// Element liReport = null;
	
		// Make room for the trend
		Element trendCell = JDOMHelper.findOrCreateElementByPath(
			mainTable,	// Starting at
			"tr/td[1]/@valign=top/table/tr/td"	// path
			,
			true	// Yes, tell us about errors
			);
		try {
			Element trendElem = nie.sr2.java_reports.ActivityTrend.runReport(
				getMainConfig(),
				inRequestInfo, inResponseInfo,
				7
				);
			if( null!=trendElem )
				trendCell.addContent( trendElem );
			else
				errorMsg( kFName, "Got back null trend snippet" );
		}
		catch( Exception e ) {
			errorMsg( kFName, "Error getting trend report: " + e );
		}
		// Add a spacer cell
		Element spacerCell = JDOMHelper.findOrCreateElementByPath(
			mainTable,	// Starting at
			"tr/td[1]/@valign=top/table/tr/td[2]/@width=20"	// path
			,
			true	// Yes, tell us about errors
			);
	
		// We will have a left and right column
		Element leftTable = JDOMHelper.findOrCreateElementByPath(
			mainTable,	// Starting at
			"tr/td[2]/@valign=top/table"	// path
			// "tr/td/@valign=top/table/@border=1"	// path
			,
			true	// Yes, tell us about errors
			);
		Element rightTable = JDOMHelper.findOrCreateElementByPath(
			mainTable,	// Starting at
			"tr/td[3]/@valign=top/table",	// path
			true	// Yes, tell us about errors
			);
	
	
		// Start adding individual reports
		////////////////////////////////////////////////////
	
		// The LEFT HAND Column / Table
		////////////////////////////////////////////////
	
	
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						// "popular_searches_ndays", null, null, null
						"PopSearchLinked", null, null, null
						)
					)
				)
			);		
	
	
		// Add a vertical spacer cell
		JDOMHelper.findOrCreateElementByPath(
			leftTable,
			// "tr[2]/td/img/@width=0/@height=" + kVerticalIndentPixels
			"tr[2]/td/@height=" + kVerticalIndentPixels
			, true
			);
	
	
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						// "no_hits_ndays", null, null, null
						"NoHitsLinked", null, null, null
						)
					)
				)
			);		
	
	
		// Add a vertical spacer cell
		JDOMHelper.findOrCreateElementByPath(
			leftTable,
			// "tr[4]/td/img/@width=0/@height=" + kVerticalIndentPixels
			"tr[4]/td/@height=" + kVerticalIndentPixels
			, true
			);
	
	
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"number_of_searches_ndays", null, null, null
						)
					)
				)
			);		
	
	
	
	
	
	
	
		/***
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"raw_log_ndays", " for yesterday and today", "days", "1"
						)
					)
				)
			);		
	
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"raw_log_ndays", " for the past week", "days", "7"
						)
					)
				)
			);		
	
		leftTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"raw_log_ndays", " for the past 30 days", "days", "30"
						)
					)
				)
			);		
		****/
	
	
		// The RIGHT HAND Column / Table
		////////////////////////////////////////////////
	
		int spacerCount = 0;
		if( getMainConfig().hasUserClasses() ) {
	
			rightTable.addContent(
				(new Element("tr")).addContent(
					(new Element("td")).addContent(
						generateReportListingElement(
							inRequestInfo, inResponseInfo,
							"advertiser_summary_ndays", null, null, null
							)
						)
					)
				);		
	
			// Add a vertical spacer cell
			JDOMHelper.findOrCreateElementByPath(
				rightTable,
				// "tr[2]/td/img/@width=0/@height=" + kVerticalIndentPixels
				"tr[" + (++spacerCount*2) + "]/td/@height=" + kVerticalIndentPixels
				, true
				);
	
		}
	
	
	
		rightTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"frequent_visitors_ndays", null, null, null
						)
					)
				)
			);		
	
	
	
		/***
		// Add a vertical spacer cell
		JDOMHelper.findOrCreateElementByPath(
			rightTable,
			// "tr[4]/td/img/@width=0/@height=" + kVerticalIndentPixels
			"tr[" + (++spacerCount*2) + "]/td/@height=" + kVerticalIndentPixels
			, true
			);
		rightTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"raw_log_ndays", " so far today", "days", "0"
						)
					)
				)
			);		
		***/
	
	
		/***
		rightTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"raw_log1", null, null, null
						)
					)
				)
			);		
		rightTable.addContent(
			(new Element("tr")).addContent(
				(new Element("td")).addContent(
					generateReportListingElement(
						inRequestInfo, inResponseInfo,
						"exposures", null, null, null
						)
					)
				)
			);
		***/		
	
	
	
	
		// We'll be building links to the UI
		// we'll start with a template link
		nie.webui.UILink uiLink = null;
		Element mapFormLink = null;
		try {
			uiLink = new nie.webui.UILink(
				getMainConfig(),
				"QueryMaps",							// String Screen Name
				null,	// String inParmCGIName,
				"Find and Manage Mappings",	// String optLinkTitleText
				null
				);
	
			// Create the link
			String linkText = "Find / Edit Mappings";
			// String operation = nie.webui.UILink.UI_OPERATION_FORMGEN;
			String operation = nie.webui.UILink.UI_OPERATION_ADD;
			mapFormLink = uiLink.generateLinkElement( inRequestInfo, linkText, operation, null );
	
		}
		catch( Exception e ) {
			throw new ReportException( kExTag
				+ "Error creating UI linking object (1): " + e
				);
		}
	
	
		// Add a vertical spacer cell
		JDOMHelper.findOrCreateElementByPath(
			rightTable,
			"tr[" + (++spacerCount*2) + "]/td/@height=" + kVerticalIndentPixels
			, true
			);
		// Add the Maps form link
	
		Element adminCell = JDOMHelper.findOrCreateElementByPath(
			rightTable,
			"tr[" + (spacerCount*2+1) + "]/td"
			, true
			);
		adminCell.addContent( mapFormLink );
	
	
	
	
	
	
		// Others
	
	
	
	
		// Done
		return outElem;
	
	
	}



	Element generateReportListingElement(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo,
		String inReportName,
		String optExtraText,
		String optParmName,
		String optParmValue
		)
			throws ReportException
	{
		final String kFName = "generateReportListElement";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null == inRequestInfo
			|| null == inResponseInfo
			)
		{
			throw new ReportException( kExTag
				+ "Null request/reponse object."
				);
		}


		inReportName = NIEUtil.trimmedStringOrNull( inReportName );

		// If no report, give the menu
		if( null == inReportName )
		{
			throw new ReportException( kExTag
				+ "No report name given."
				);
		}

		// Our eventual response, barring any exceptions
		// Element outElem = new Element( "li" );
		// Element outElem = new Element( "div" );


		// Track down the report
		ReportInterface xReport = null;
		try {
			xReport = locateReport( inReportName );
		}
		catch( Exception e ) {
			throw new ReportException( kExTag
				+ "Unable to locate/load/configure report \""
				+ inReportName + "\". Error: " + e
				);
		}
		if( null == xReport )
			throw new ReportException( kExTag
				+ "Unable to locate/load/configure report \""
				+ inReportName + "\". Error - report was null."
				);


		Element outElem = xReport.generateMenuLinksToThisReport(
			inRequestInfo
			);

		return outElem;

		/*******
		// Get the title
		String lTitle = xReport.getTitle( null );
		if( null != optExtraText )
			lTitle += " " + optExtraText;

		// Get the link
		////////////////////////////////////////////

		// We use AuxIOInfo to build a link
		AuxIOInfo newLinkInfo = new AuxIOInfo();
		// Prime it with the basic URL we want
		newLinkInfo.setBasicURL( getMainAppURL() );

		// Copy over existing CGI values
		// We have to say which fields we DON'T want
		List excludeFields = new Vector();
		excludeFields.add( REPORT_NAME_CGI_FIELD );
		excludeFields.add( XMLDefinedReport.START_ROW_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.DESIRED_ROW_COUNT_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.SORT_SPEC_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.FILTER_SPEC_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.FILTER_PARAM_CGI_FIELD_NAME );
		// Now do the copy
		newLinkInfo.copyInCGIFields( inRequestInfo, excludeFields );
		// And set the report name
		newLinkInfo.addCGIField(
			REPORT_NAME_CGI_FIELD, inReportName
			);

		// Add the parameter, if any
		if( null != optParmName && null != optParmValue )
		{
			newLinkInfo.setOrOverwriteCGIField(
				optParmName , optParmValue
				);
		}

		// Now get the full href back
		String href = newLinkInfo.getFullCGIEncodedURL();

		if( null==href )
			throw new ReportException( kExTag
				+ "Got back null href, this link will not be created."
				);


		// Now create the final XHTML element
		// we use <div> if not an active link
		Element newTag = null;
		newTag = new Element( "a" );
		newTag.setAttribute( "href", href );
		newTag.setAttribute( "class", NIE_REPORT_LINK_CSS_CLASS );

		// And add the display text
		newTag.addContent( lTitle );

		// Add this to the top <li> tag
		outElem.addContent( newTag );

		// Return the tag!
		return outElem;
		***/

	}

	void generateReportListingCompact(
		AuxIOInfo inRequestInfo,
		AuxIOInfo inResponseInfo,
		String inReportName,
		String optExtraText,
		String optParmName,
		String optParmValue,
		Element inTopRow,
		Element inBottomRow
		)
			throws ReportException
	{
		final String kFName = "generateReportListingElementCompact";
		final String kExTag = kClassName + '.' + kFName + ": ";

		if( null == inRequestInfo
				|| null == inResponseInfo
			)
			throw new ReportException( kExTag
				+ "Null request/reponse object."
				);

		inReportName = NIEUtil.trimmedStringOrNull( inReportName );

		// If no report, give the menu
		if( null == inReportName ) {
			throw new ReportException( kExTag
				+ "No report name given."
				);
		}

		// Our eventual response, barring any exceptions
		// Element outElem = new Element( "li" );
		// Element outElem = new Element( "div" );


		// Track down the report
		ReportInterface xReport = null;
		try {
			xReport = locateReport( inReportName );
		}
		catch( Exception e ) {
			throw new ReportException( kExTag
				+ "Unable to locate/load/configure report \""
				+ inReportName + "\". Error: " + e
				);
		}
		if( null == xReport )
			throw new ReportException( kExTag
				+ "Unable to locate/load/configure report \""
				+ inReportName + "\". Error - report was null."
				);

		xReport.generateMenuLinksToThisReportCompact(
			inRequestInfo,
			inTopRow, inBottomRow
			);

		/*******
		// Get the title
		String lTitle = xReport.getTitle( null );
		if( null != optExtraText )
			lTitle += " " + optExtraText;

		// Get the link
		////////////////////////////////////////////

		// We use AuxIOInfo to build a link
		AuxIOInfo newLinkInfo = new AuxIOInfo();
		// Prime it with the basic URL we want
		newLinkInfo.setBasicURL( getMainAppURL() );

		// Copy over existing CGI values
		// We have to say which fields we DON'T want
		List excludeFields = new Vector();
		excludeFields.add( REPORT_NAME_CGI_FIELD );
		excludeFields.add( XMLDefinedReport.START_ROW_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.DESIRED_ROW_COUNT_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.SORT_SPEC_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.FILTER_SPEC_CGI_FIELD_NAME );
		excludeFields.add( XMLDefinedReport.FILTER_PARAM_CGI_FIELD_NAME );
		// Now do the copy
		newLinkInfo.copyInCGIFields( inRequestInfo, excludeFields );
		// And set the report name
		newLinkInfo.addCGIField(
			REPORT_NAME_CGI_FIELD, inReportName
			);

		// Add the parameter, if any
		if( null != optParmName && null != optParmValue )
		{
			newLinkInfo.setOrOverwriteCGIField(
				optParmName , optParmValue
				);
		}

		// Now get the full href back
		String href = newLinkInfo.getFullCGIEncodedURL();

		if( null==href )
			throw new ReportException( kExTag
				+ "Got back null href, this link will not be created."
				);


		// Now create the final XHTML element
		// we use <div> if not an active link
		Element newTag = null;
		newTag = new Element( "a" );
		newTag.setAttribute( "href", href );
		newTag.setAttribute( "class", NIE_REPORT_LINK_CSS_CLASS );

		// And add the display text
		newTag.addContent( lTitle );

		// Add this to the top <li> tag
		outElem.addContent( newTag );

		// Return the tag!
		return outElem;
		***/

	}


	String _getCssStyleSheetURI()
	{
		return XMLDefinedReport.DEFAULT_CSS_URI;
	}

	// Not getting a style sheet should not be a fatal error
	// though we will log error messages
	// Todo: let them load their own from elsewhere
	String getCssStyleText()
	{

		return getMainConfig().getDefaultCssStyleTextOrNull();

		/***
		final String kFName = "getCssStyleText";

		String uri = getCssStyleSheetURI();
		if( null==uri )
		{
			infoMsg( kFName,
				"No CSS URI defined, returning null."
				);
			return null;
		}

		AuxIOInfo tmpAuxInfo = new AuxIOInfo();
		tmpAuxInfo.setSystemRelativeBaseClassName(
			kFullClassName
			);
		String answer = null;
		try
		{
			answer = NIEUtil.fetchURIContentsChar(
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
			return null;
		}
		// Normalize and check
		answer = NIEUtil.trimmedStringOrNull( answer );
		if( null==answer )
			errorMsg( kFName,
				"Null/empty default CSS style sheet contents read"
				+ " from URI \"" + uri + "\", returning null."
				);

		return answer;
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





	String getMainAppURL()
	{
		// return getMainApp().getSearchNamesURL();
		return getMainConfig().getSearchNamesURL();
	}
	public SearchReportingConfig getReportingConfig()
	{
		return fReportsConfig;
	}



	ReportInterface locateReport(
			String inReportName
			//, boolean inShowNotFoundErrors
		)
			throws ReportConfigException, ReportException
	{
		final String kFName = "locateReport";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// Do we have an XML report?
		// XMLDefinedReport xReport = null;
		ReportInterface theReport = null;
		try
		{
			// Three possible outcomes:
			// 1: Found report and it's OK, return report
			// 2: Found report BUT IT HAD ERRORS, throw exception
			// 3: Did not find report, return Null here
			theReport = locateXMLReport(
				inReportName,
				false	// inShowNotFoundErrors
				);
		}
		catch( Exception e1 )
		{
			throw new ReportException( kExTag
				+ "Unable to configure XML report \""
				+ inReportName + "\". Error: " + e1
				);
		}

		// OR how about a Java report?
		if( null == theReport )
		{
			try
			{
				theReport = locateJavaReport(
					inReportName,
					false	// inShowNotFoundErrors
					);
			}
			catch( Exception e2 )
			{
				throw new ReportException( kExTag
					+ "Unable to configure Java report \""
					+ inReportName + "\". Error: " + e2
					);
			}
		}

		return theReport;
	}

	// This will return a null if not found, and maybe an error
	// If a report IS found, but has a PROBLEM, you will get an exception
	// I know it seems odd to have two failure modes, but one is
	// sometimes expected, whereras the other is ALWAYS a serious error
	// and we need to know the difference
	ReportInterface locateXMLReport(
		String inReportName,
		boolean inShowNotFoundErrors
		)
			throws ReportConfigException
	{
		final String kFName = "locateXMLReport";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// fCachedXMLReports
		// fSearchedForXMLReports

		// Sanity check
		inReportName = NIEUtil.trimmedStringOrNull( inReportName );
		if( null == inReportName ) {
			throw new ReportConfigException( kExTag
				+ "Null/empty report name passed in."
				);
		}

		debugMsg( kFName, "Looking for report " + inReportName );

		// If it's in the cache, return it!
		if( fCachedXMLReports.containsKey( inReportName ) ) {
			debugMsg( kFName, "Returning previously cached report." );
			return (ReportInterface)fCachedXMLReports.get(
				inReportName
				);
		}

		// If we've tried before, don't bother again
		// If it's in this hash, and not in the other, then
		// we have tried before and failed
		if( fSearchedForXMLReports.contains( inReportName ) ) {
			debugMsg( kFName, "We have tried to load this report before." );
			if( inShowNotFoundErrors )
				throw new ReportConfigException( kExTag
					+ "Previously failed to load XML Report \""
					+ inReportName + "\"."
					);
			// Else just quietly return null
			return null;
		}
		// Remember that at least we've tried before
		fSearchedForXMLReports.add( inReportName );

		ReportInterface newReport = null;
		try
		{
			String reportURI = generateDefaultXMLReportURI(
				inReportName
				);
			debugMsg( kFName, "Report URI is \"" + reportURI + "\"." );


			AuxIOInfo tmpAuxInfo = new AuxIOInfo();
			tmpAuxInfo.setSystemRelativeBaseClassName(
				kFullClassName
				);

			InputStream stream = NIEUtil.openURIReadBin(
				reportURI,
				getMainConfig().getConfigFileURI(), // optRelativeRoot
				null, // String optUsername,
				null, // String optPassword,
				tmpAuxInfo,  // AuxIOInfo inoutAuxIOInfo
				false			// use POST
				);
			// Close it and reopen with recursive open
			stream.close();

			debugMsg( kFName, "Found it, will now try JDOM constructor" );

			// JDOMHelper elem = new JDOMHelper( reportURI, null, 0 );
			JDOMHelper elem = new JDOMHelper(
				reportURI,
				// getMainApp().getSearchTuningConfig().getConfigFileURI(),
				// getMainApp().getConfigFileURI(), // optRelativeRoot
				getMainConfig().getConfigFileURI(), // optRelativeRoot
				0,
				tmpAuxInfo
				);

			debugMsg( kFName,
				"Have created JDOM config tree, will instantiate report object."
				);


			// OK so far, so create the report
			newReport = new XMLDefinedReport(
				// fMainApp, inReportName, elem
				getMainConfig(), inReportName, elem
				);

			debugMsg( kFName,
				"Have created report object, storing in cache."
				);

			// Save report in the cache
			fCachedXMLReports.put( inReportName, newReport );


		}
		// OK, so we just couldn't locate it
		catch( IOException ioe )
		{
			if( inShowNotFoundErrors )
				errorMsg( kFName,
					"Error locating XML Report \"" + inReportName + "\"."
					+ " Returning null."
					+ " Error was: " + ioe
					);
			return null;
		}
		// This is always an error
		catch( JDOMHelperException jde )
		{
			throw new ReportConfigException( kExTag
				+ "Error parsing XML in Report \"" + inReportName + "\"."
				+ " Returning null."
				+ " Error was: " + jde
				);
		}
		// catch( ReportConfigException e )
		// We let this bubble up the food chain

		return newReport;

	}

	// If a report IS found, but has a PROBLEM, you will get an exception
	// I know it seems odd to have two failure modes, but one is
	// sometimes expected, whereras the other is ALWAYS a serious error
	// and we need to know the difference
	ReportInterface locateJavaReport(
		String inReportName,
		boolean inShowNotFoundErrors
		)
			throws ReportConfigException
	{
		final String kFName = "locateJavaReport";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// fCachedXMLReports
		// fSearchedForXMLReports

		// Sanity check
		inReportName = NIEUtil.trimmedStringOrNull( inReportName );
		if( null == inReportName )
			throw new ReportConfigException( kExTag + "Null/empty report name passed in." );

		debugMsg( kFName, "Looking for report " + inReportName );

		// If it's in the cache, return it!
		if( fCachedJavaReports.containsKey( inReportName ) ) {
			debugMsg( kFName, "Returning previously cached report." );
			return (ReportInterface)fCachedJavaReports.get( inReportName );
		}

		// If we've tried before, don't bother again
		// If it's in this hash, and not in the other, then
		// we have tried before and failed
		if( fSearchedForJavaReports.contains( inReportName ) ) {
			debugMsg( kFName, "We have tried to load this report before." );
			if( inShowNotFoundErrors )
				throw new ReportConfigException( kExTag + "Previously failed to load XML Report \"" + inReportName + "\"." );
			// Else just quietly return null
			return null;
		}
		// Remember that at least we've tried before
		fSearchedForJavaReports.add( inReportName );

		ReportInterface newReport = null;

		debugMsg( kFName, "inReportName=" + inReportName );


		String reportURI = generateDefaultJavaReportClassURI(
			inReportName
			);
		debugMsg( kFName, "Report class is \"" + reportURI + "\"." );


		// nie.sn.SearchTuningApp inMainApp,
		//		String
		Class lReportClass;

		try {
			// debugMsg( kFName, "trying class.forName for reportURI=" + reportURI );
			lReportClass = Class.forName( reportURI );
		} catch( ClassNotFoundException cnfe ) {
			if( inShowNotFoundErrors )
				throw new ReportConfigException( "Can't load report class \"" + reportURI + "\", error:" + cnfe );
			else
				return null;
		}

		// Class[] lConstructorSignature = new Class[3];
		// lConstructorSignature[0] = lQueueClassName.getClass();
		// lConstructorSignature[1] = this.getClass();
		// lConstructorSignature[2] = inElement.getClass();

		Class[] lConstructorSignature = new Class [] {
			// nie.sn.SearchTuningApp.class,
			nie.sn.SearchTuningConfig.class,
			String.class
		};


		Constructor lConstructor = null;

		try {
			lConstructor = lReportClass.getConstructor( lConstructorSignature );
		} catch( NoSuchMethodException nsme ) {
			throw new ReportConfigException( "Could not find appropriate constructor for report class \"" + reportURI + "\"." );
		}

		try {
			// Create the args we will pass to the constructor
			// Object [] lParams = new Object[2];
			// lParams[0] = fMainApp;
			// lParams[1] = inReportName;
			Object [] lParams = new Object[] {
				// fMainApp,
				getMainConfig(),
				inReportName
			};
			newReport = (ReportInterface)lConstructor.newInstance( lParams );
		} catch( Exception ie ) {
			throw new ReportConfigException( "Unable to instantiate object for report class \"" + reportURI + "\". Error: " + ie );
		}

		// Save report in the cache
		fCachedJavaReports.put( inReportName, newReport );

		debugMsg( kFName,
			"Have created report object, storing in cache."
			);

		// Save report in the cache
		fCachedJavaReports.put( inReportName, newReport );

		return newReport;

	}

	private static String generateDefaultXMLReportURI( String inReportName )
			throws ReportConfigException
	{
		final String kFName = "generateDefaultXXMLReportURI";
		final String kExTag = kClassName + '.' + kFName + ": ";

		// somewhat redundant sanity checks
		// inTableName = NIEUtil.trimmedStringOrNull( inTableName );
		inReportName = NIEUtil.trimmedStringOrNull( inReportName );
		if( inReportName == null )
			throw new ReportConfigException( kExTag
				+ "Null report name passed in."
				);

		// Form the name and return it
		return DEFAULT_XML_REPORT_PREFIX
			+ inReportName
			+ DEFAULT_XML_REPORT_SUFFIX
			;

	}

	private static String generateDefaultJavaReportClassURI( String inReportName )
		throws ReportConfigException
	{
		return DEFAULT_JAVA_REPORT_PREFIX + inReportName;
	}



	nie.sn.SearchTuningApp _getMainApp()
	{
		return _fMainApp;
	}

	nie.sn.SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	DBConfig getDBConfig()
	{
		// return getMainApp().getDBConfig();
		return getMainConfig().getDBConfig();
	}


	String getReportTemplate()
	{
		if( null != getReportingConfig() )
			return getReportingConfig().getAndSetupOptionalReportTemplate();
		else
			return null;
	}

	// private Element fConfigElem;
	private nie.sn.SearchTuningApp _fMainApp;
	private nie.sn.SearchTuningConfig fMainConfig;

	// Search reporting configuration
	SearchReportingConfig fReportsConfig;





	// Holds JDOMHelper trees of reports we have located,
	private Hashtable fCachedXMLReports;
	// Keeps track of whether we've checked for a particular
	// report or not, holds type Boolean
	private HashSet fSearchedForXMLReports;

	// Holds ReportInterface objects of Java reports we have located,
	private Hashtable fCachedJavaReports;
	// Keeps track of whether we've checked for a particular
	// report or not, holds type Boolean
	private HashSet fSearchedForJavaReports;



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


	public static final String _REPORT_NAME_CGI_FIELD = "report";

	private final static String DEFAULT_XML_REPORT_PREFIX =
		AuxIOInfo.SYSTEM_RESOURCE_PREFIX
		+ "xml_reports/"
		;
	private final static String DEFAULT_JAVA_REPORT_PREFIX = "nie.sr2.java_reports.";
	private final static String DEFAULT_XML_REPORT_SUFFIX = ".xml";

	// See CSSClassNames ACTIVE_RPT_LINK and INACTIVE_RPT_LINK
	public final static String _NIE_REPORT_LINK_CSS_CLASS =
		"nie_report_link";
	public final static String _NIE_INACTIVE_REPORT_LINK_CSS_CLASS =
		"nie_inactive_report_link";

	// Instead of seeing the main menu, we go to here
	// public final static String DEFAULT_REPORT_NAME_WITH_UI = "PopSearchLinked";
	public final static String DEFAULT_REPORT_NAME_WITH_UI = "PopSearchLinked3";
	public final static String DEFAULT_REPORT_NAME_NO_UI = "popular_searches_ndays";
	public final static String DEFAULT_REPORT_PARM_NAME = "days";
	public final static String DEFAULT_REPORT_PARM_VALUE = "7";

		// The name of the system XSLT template we will call
		// String templateName = "admin_showall";
		// The resulting document
		// Document formattedDoc = null;
		// Try to format it
//		try
//		{
//			formattedDoc = JDOMHelper.xsltDocToDoc(
//				mainDoc,
//				templateName,
//				null, true
//				);
//		}
//		catch (JDOMHelperException e)
//		{
//
//			String msg = "SnRequestHandler:adminShowCompleteConfig:"
//				+ " Got exception while formatting XML into HTML."
//				+ " Exception=\"" + e + "\""
//				;
//			errorMsg( kFName, msg );
//			setupTextErrorResponse( msg );
//			return;
//		}
//		String response = JDOMHelper.JDOMToString( formattedDoc, true );

}
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
import java.io.*;
import nie.sn.CSSClassNames;


/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ActivityTrend extends BaseReport
{

	static final String kStaticClassName = "ActivityTrend";

	public String kClassName()
	{
		// return "ActivityTrend";
		return kStaticClassName;
	}

	public int getRequiredAccessLevel() {
		return ReportConstants.DEFAULT_ACCESS_LEVEL;
	}

	public ActivityTrend(
		// nie.sn.SearchTuningApp inMainApp,
		nie.sn.SearchTuningConfig inMainConfig,
		String inShortReportName
		)
			throws ReportConfigException
	{
		// super( inMainApp, inShortReportName );
		super( inMainConfig, inShortReportName );
	}

	// This gets a report snippet
	public static Element runReport(
			nie.sn.SearchTuningConfig inMainConfig,
			AuxIOInfo inReuqestObject, AuxIOInfo inResponseObject,
			int inNDays
		)
			throws ReportConfigException, ReportException
	{
		ActivityTrend report = new ActivityTrend( inMainConfig, "ActivityTrend" );

		/***
		AuxIOInfo myRequestObj = new AuxIOInfo();
		myRequestObj.addCGIField(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME, inNDays
			);
		AuxIOInfo myResponseObj = new AuxIOInfo();
		***/
		inReuqestObject.setOrOverwriteCGIField( ReportConstants.DAYS_OLD_CGI_FIELD_NAME, ""+inNDays );


		return report.runReport( inReuqestObject, inResponseObject, false );
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

		int interval = inRequestObject.getIntFromCGIField(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME, DEFAULT_INTERVAL
			);

		Hashtable lMasterValuesHash = getVariables( inRequestObject );

		// The two key points in the tree
		Element [] points = prepareBaseOutputTree(
			inDoFullPage, lMasterValuesHash,
			inDoFullPage,
			inRequestObject, inResponseObject
			);

		Element outElem = points[0];
		// Element contentHanger = points[1];
		Element mainContentTable = points[1];

		// Add main menu link, if doing full table
		/***
		if( inDoFullPage ) {

			// Now one table in, to allow for main menu link
			Element linkCell = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr/td/@align=center",	// path
				true	// Yes, tell us about errors
				);

			// Now get the main menu link
			Element mainMenuElem = generateLinkToMainMenu(
				inRequestObject, inResponseObject
				);
			// And add that to this cell
			linkCell.addContent( mainMenuElem );


			// Now one table in, to allow for main menu link
			mainContentTable = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[2]/td/table",	// path
				true	// Yes, tell us about errors
				);
		}
		***/

		// Add the outer table border, and then the inner table
		mainContentTable.setAttribute( "class", CSSClassNames.TREND_TABLE );
		mainContentTable.setAttribute( "bgColor", TREND_BOX_SHADE_COLOR );
		mainContentTable.setAttribute( "border", "1" );

		// We need this later to add the navigation tabs
		Element borderTable = mainContentTable;

		Element targetCell = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr/td/@colspan=3",	// path
			true	// Yes, tell us about errors
			);

		// get (or caculate) the main content
		// THIS is the actual report!!!!
		// ==================================================
		mainContentTable =  getOrCalculateMainContent(
			inRequestObject, inResponseObject,
			lMasterValuesHash, interval
			);
		targetCell.addContent( mainContentTable );


		// Now add the navigation links
		try {
			// Where in the border table this stuff goes
			ReportLink myLink = new ReportLink(
				getMainConfig(),
				kClassName(),
				"trend",
				null,
				null,
				ReportConstants.DAYS_OLD_CGI_FIELD_NAME,
				null
				);

			int linkRow = 2;
			int linkCol = 0;

			// Previous Day
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				1, "Day",
				interval
				);

			// Previous Week
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				7, "Week",
				interval
				);

			// Previous Month
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				30, "Month",
				interval
				);

		}
		catch( Exception e ) {
			errorMsg( kFName, "Unable to add navigation links.  Error: " + e );
		}

		return outElem;
	}

	public Element _runReportV1(
			AuxIOInfo inRequestObject,
			AuxIOInfo inResponseObject,
			boolean inDoFullPage
		)
			throws ReportException
	{
		final String kFName = "runReportV1";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// int interval = 30;
		// int interval = 180;
		// int howMany = 10;

		int interval = inRequestObject.getIntFromCGIField(
			ReportConstants.DAYS_OLD_CGI_FIELD_NAME, DEFAULT_INTERVAL
			);
		// int howMany = DEFAULT_HOW_MANY_ROWS;
		int howMany = getDesiredRowCount( inRequestObject );


		int movementImgWidth = DEFAULT_MOVEMENT_ICON_WIDTH;
		int movementImgHeight = DEFAULT_MOVEMENT_ICON_HEIGHT;

		// String lGraphicURLPrefix = DEFAULT_IMAGE_URL_PREFIX;
		String lGraphicURLPrefix = ReportConstants.IMAGE_URL_PREFIX;

		// We'll use this hash for varaible substitution in Strings
		Hashtable lMasterValuesHash = getVariables( inRequestObject );

		// The two key points in the tree
		Element [] points = prepareBaseOutputTree(
			inDoFullPage, lMasterValuesHash,
			inDoFullPage,
			inRequestObject, inResponseObject
			);

		Element outElem = points[0];
		// Element contentHanger = points[1];
		Element mainContentTable = points[1];

		// Add main menu link, if doing full table
		if( inDoFullPage ) {

			// Now one table in, to allow for main menu link
			Element linkCell = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr/td/@align=right",	// path
				true	// Yes, tell us about errors
				);

			// Now get the main menu link
			Element mainMenuElem = generateLinkToMainMenu(
				inRequestObject, inResponseObject
				);
			// And add that to this cell
			linkCell.addContent( mainMenuElem );


			// Now one table in, to allow for main menu link
			mainContentTable = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[2]/td/table",	// path
				true	// Yes, tell us about errors
				);
		}


		// Add the outer table border, and then the inner table
		mainContentTable.setAttribute( "class", CSSClassNames.TREND_TABLE );
		mainContentTable.setAttribute( "bgColor", TREND_BOX_SHADE_COLOR );
		mainContentTable.setAttribute( "border", "1" );

		// We need this later to add the navigation tabs
		Element borderTable = mainContentTable;

		// Now go another table in
		mainContentTable = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr/td/@colspan=3/table",	// path
			true	// Yes, tell us about errors
			);

		// Now we start working on the query
		//////////////////////////////////////////////////
		Calendar lCalendar = Calendar.getInstance();
		String lCurrentIntervalEnd = getDateString( lCalendar );

		lCalendar.add( Calendar.DATE, - interval );
		String lCurrentIntervalStart = getDateString( lCalendar );

		lCalendar.add( Calendar.DATE, - interval );
		String lPreviousIntervalStart = getDateString( lCalendar );

		/***
		String qry =
			"SELECT *\n" +
			"FROM (\n" +
			"	SELECT  query,\n" +
			"		num_requests,\n" +
			"		ROWNUM " + getDBConfig().getVendorAliasString() + " thePosition\n" +
			"	FROM (\n" +
			"		SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n" +
			"			count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n" +
			"		FROM " + getDefaultTableName() + "\n" +
			"		WHERE start_time >= '" + lCurrentIntervalStart + "'\n" +
			"				     AND start_time <='" + lCurrentIntervalEnd + "'\n" +
			"		GROUP BY normalized_query\n" +
			"		ORDER BY count(*) DESC, normalized_query\n" +
			"	) innter_select\n" +
			") mid_select\n" +
			"WHERE thePosition < " + howMany + "\n"
			;
		***/

		String qry =
			"SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n"
			+ " count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n"
			+ " FROM " + getDefaultTableName() + "\n"
			+ " WHERE start_time >= '" + lCurrentIntervalStart + "'\n"
			+ " AND start_time <='" + lCurrentIntervalEnd + "'\n"
			+ " GROUP BY normalized_query\n"
			// + " ORDER BY count(*) DESC, normalized_query\n"
			+ " ORDER BY count(*) DESC, max(start_time) DESC, normalized_query"
			;

		debugMsg( kFName, "Will run SQL=" + qry );




		//
		// RUN the query!!!
		// ==================================================
		// (and only build the headers if we had success)
		//
		ResultSet lThisTimeIntervalResultSet = null;
		Statement lThisStatement = null;
		Connection lThisConnection = null;


		ResultSet lOldTimeIntervalResultSet = null;
		Statement lOldStatement = null;
		Connection lOldConnection = null;


		try {
			// lThisTimeIntervalResultSet = getDBConfig().runQuery( qry );
			Object [] objs1 = getDBConfig().runQuery( qry, true );
			lThisTimeIntervalResultSet = (ResultSet) objs1[0];
			lThisStatement = (Statement) objs1[1];
			lThisConnection = (Connection) objs1[2];

			// Start building the output document
			// Element resultsTableElem = new Element( "table" );
			// resultsTableElem.setAttribute( "class", "nie_results_table" );
			// Hold off adding it until we know how well we did

			// font stuff we will use
			final String kFontClass = CSSClassNames.TREND_FONT;
			final String kFontSize = "-1";
			final String kFontTag = "/font/@class=" + kFontClass + "/@size=" + kFontSize;

			// The temp element we will reuse as we build
			Element tmpElem = null;

			// The first row is the main title
			// it spans four columns
			int row=1;
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/th/@colspan=4/@align=center",	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( getTitle(null) );

			// The second row gives the time interval
			// since it compares two invervals, it's twice what you might initially expect
			row++;
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td/@colspan=4/@align=center" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			// tmpElem.addContent( "for the last " + (interval*2) + " days" );
			// String tmpMsg = "for the last " + (interval*2) + " days";
			// Per Miles and Sean, though I disagree, don't double interval for subtitle
			// even though technically it does show data for 2 N back...
			String tmpMsg = "for the last" + (1==interval ? "" : " "+interval)
				+ " day" + (1==interval ? "" : "s")
				;
			tmpElem.addContent( tmpMsg );

			// The third row is more header text "Results List Ranking"
			// the third row has a spacer
			row++;
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td",	// path
				true	// Yes, tell us about errors
				);
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[2]/@colspan=3/@align=center" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( "Results List Ranking" );


			// The fourth row has the 4 main column headings
			row++;
			// Heading 1: "Search Term" (align left)
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[1]/@align=left" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( "Search Term" );
			// Heading 2: "Change" (align center)
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[2]/@align=center" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( "Change" );
			// Heading 3: "Last N Days" (align center)
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[3]/@align=center" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( "Last " + interval + " days" );
			// Heading 4: "Prior N Days" (align center)
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[4]/@align=center" + kFontTag,	// path
				true	// Yes, tell us about errors
				);
			tmpElem.addContent( "Prior " + interval + " days" );

			// The fifth row just has the four <hr> tags
			row++;
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[1]/hr",	// path
				true	// Yes, tell us about errors
				);
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[2]/hr",	// path
				true	// Yes, tell us about errors
				);
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[3]/hr",	// path
				true	// Yes, tell us about errors
				);
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td[4]/hr",	// path
				true	// Yes, tell us about errors
				);

			debugMsg( kFName, "starting loop" );

			int lCurrentPosition = 0;	// simulates rownum
			while( lThisTimeIntervalResultSet.next() ) {
				lCurrentPosition++;	// simulates rownum

				// these rows are for the results
				row++;
				Element rowElem = JDOMHelper.findOrCreateElementByPath(
					mainContentTable,		// Starting at
					"tr[" + row + "]",	// path
					true	// Yes, tell us about errors
					);

				String lQueryTerm = lThisTimeIntervalResultSet.getString( "query" );
				String lQueryDisplay = null==lQueryTerm ? "(null)" : lQueryTerm;
				if( lQueryDisplay.length() > DISPLAY_QUERY_TRUNCATION_LENGTH )
					lQueryDisplay = lQueryDisplay.substring( 0, DISPLAY_QUERY_TRUNCATION_LENGTH ) + "...";

				int lNumResults = lThisTimeIntervalResultSet.getInt( "num_requests" );
				// int lCurrentPosition = lThisTimeIntervalResultSet.getInt( "thePosition" );

				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[1]/@align=left" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( lQueryDisplay );


				String qry2 = null;
				if( getDBConfig().getConfiguredVendorTag().equals(DBConfig.VENDOR_TAG_ORACLE) ) {
					qry2 =
						"SELECT *\n" +
						"FROM (\n" +
						"	SELECT query,\n" +
						"		num_requests,\n" +
						"		ROWNUM " + getDBConfig().getVendorAliasString() + " thePosition\n" +
						"	FROM (\n" +
						"		SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n" +
						"			count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n" +
						"		FROM " + getDefaultTableName() + "\n" +
						"		WHERE start_time BETWEEN '" + lPreviousIntervalStart + "'\n" +
						"				     AND '" + lCurrentIntervalStart + "'\n" +
						"		GROUP BY normalized_query\n" +
						"		ORDER BY count(*) DESC, normalized_query\n" +
						"	)\n" +
						")\n"
						;
						// Add the query term, or null
						if( null!=lQueryTerm )
							qry2 += "WHERE query='" + lQueryTerm + "'\n";
						else
							qry2 += "WHERE query IS NULL\n";
				}
				else {
					qry2 =
						"SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query"
						+ ", count(*) " + getDBConfig().getVendorAliasString() + " num_requests"
						+ " FROM " + getDefaultTableName()
						+ " WHERE start_time >= '" + lPreviousIntervalStart
						+ " AND start_time < '" + lCurrentIntervalStart
						+ " GROUP BY normalized_query"
						+ " ORDER BY count(*) DESC, normalized_query"
						;
				}

				debugMsg( kFName, qry2 );

				int lOldPosition = -1;
				String lOldPositionString = "";
				String lGraphicName;
				String lGraphicDescription;

				try {
		    
					// lOldTimeIntervalResultSet = getDBConfig().runQuery( qry2 );
					Object [] objs2 = getDBConfig().runQuery( qry2, true );
					lOldTimeIntervalResultSet = (ResultSet) objs2[0];
					lOldStatement = (Statement) objs2[1];
					lOldConnection = (Connection) objs2[2];

					// It's Oracle, we can read the results directly
					if( getDBConfig().getConfiguredVendorTag().equals(DBConfig.VENDOR_TAG_ORACLE) ) {
						lOldTimeIntervalResultSet.next();
						lOldPosition = lOldTimeIntervalResultSet.getInt( "thePosition" );
					}
					// Else not oracle, scan the results set
					else {
						// Maintain the 2nd rownum counter simulator
						int tmpCounter = 0;
						// For each record
						while( lOldTimeIntervalResultSet.next() ) {
							tmpCounter++;
							String tmpTerm = lOldTimeIntervalResultSet.getString( "query" );
							tmpTerm = NIEUtil.trimmedStringOrNull( tmpTerm );

							// If both null
							if( null==lQueryTerm && null==tmpTerm ) {
								lOldPosition = tmpCounter;
								break;
							}

							// If not null and match
							if( null!=lQueryTerm && null!=tmpTerm && lQueryTerm.equals(tmpTerm) ) {
								lOldPosition = tmpCounter;
								break;
							}

							// Else keep looking
						}	// End for each record
						// In keeping with older Oracle logic, throw an exception
						// if we didn't find anything
						if( lOldPosition < 0 )
							throw new Exception( "not found" );
					}

					lOldPositionString = "" + lOldPosition;

					if( lOldPosition > lCurrentPosition ) {
						lGraphicName = "moving-up.gif";
						lGraphicDescription = "Moved Up";
					}
					else if( lOldPosition < lCurrentPosition ) {
						lGraphicName = "moving-down.gif";
						lGraphicDescription = "Moved Down";
					} else {
						lGraphicName = "did-not-move.gif";
						lGraphicDescription = "No Change / Same as Before";
					}
				} catch( Exception e ) {
					lGraphicName = "new-1.gif";
					lGraphicDescription = "New Term";
					lOldPositionString = "-";
				}
				finally {
					// quickie cleanup!
					lOldTimeIntervalResultSet = DBConfig.closeResults( lOldTimeIntervalResultSet, kClassName(), kFName, false );
					lOldStatement = DBConfig.closeStatement( lOldStatement, kClassName(), kFName, false );
				}

				// Fix up the query and image strings
				lGraphicName = lGraphicURLPrefix + lGraphicName;

				// Add the 2nd column, the marker image
				Element imgElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[2]/@align=center"					+ "/img/@width=" + movementImgWidth + "/@height=" + movementImgHeight,
					true	// Yes, tell us about errors
					);
				// Must add src= separately since that URL has slashes!
				imgElem.setAttribute( "alt", lGraphicDescription );
				imgElem.setAttribute( "src", lGraphicName );

				// The 3rd and 4th columns are the counts
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[3]/@align=center" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( "" + lCurrentPosition );

				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[4]/@align=center" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( lOldPositionString );

			}	// End for each row in the trend report

			debugMsg( kFName, "Completed Loop." );

			// Add the bottom <hr> 
			row++;
			JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td/@colspan=4/@align=center/hr/@width=70%",	// path
				true	// Yes, tell us about errors
				);
  
			// Add the legend
			row++;
			String legendGraphic = "legend.gif";
			legendGraphic = lGraphicURLPrefix + legendGraphic;
			tmpElem = JDOMHelper.findOrCreateElementByPath(
				mainContentTable,		// Starting at
				"tr[" + row + "]/td/@colspan=4/@align=center/img/@alt=Icon Legend",	// path
				true	// Yes, tell us about errors
				);
			tmpElem.setAttribute( "src", legendGraphic );

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
			lThisTimeIntervalResultSet = DBConfig.closeResults( lThisTimeIntervalResultSet, kClassName(), kFName, false );
			lThisStatement = DBConfig.closeStatement( lThisStatement, kClassName(), kFName, false );
			// lThisConnection = DBConfig.closeConnection( lThisConnection, kClassName(), kFName, false );

			lOldTimeIntervalResultSet = DBConfig.closeResults( lOldTimeIntervalResultSet, kClassName(), kFName, false );
			lOldStatement = DBConfig.closeStatement( lOldStatement, kClassName(), kFName, false );
			// lOldConnection = DBConfig.closeConnection( lOldConnection, kClassName(), kFName, false );
		}


		// interval

		try {
			// Where in the border table this stuff goes
			ReportLink myLink = new ReportLink(
				getMainConfig(),
				kClassName(),
				"trend",
				null,
				null,
				ReportConstants.DAYS_OLD_CGI_FIELD_NAME,
				null
				);

			int linkRow = 2;
			int linkCol = 0;

			// Previous Day
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				1, "Day",
				interval
				);

			// Previous Week
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				7, "Week",
				interval
				);

			// Previous Month
			addTabLink(
				inRequestObject,
				borderTable, myLink,
				linkRow, ++linkCol,
				30, "Month",
				interval
				);

		}
		catch( Exception e ) {
			errorMsg( kFName, "Unable to add navigation links.  Error: " + e );
		}

		return outElem;
	}


	public static String calcCacheKey( int inInterval ) {
		return kStaticClassName
			+ '_' + NIEUtil.getCompactDateOnlyTimestamp()
			+ '_' + ReportConstants.DAYS_OLD_CGI_FIELD_NAME
			+ '_' + inInterval
			;
	}
	public static File calcCacheDir( String inConfigURI ) {
		File baseFile = null;
		File answer = null;
		if( ! NIEUtil.isStringAURL( inConfigURI, false )
			&& ! NIEUtil.isStringASystemURI( inConfigURI, false )
		) {
			baseFile = new File( inConfigURI );
			baseFile = baseFile.getParentFile();
		}

		if( null!=baseFile )
			answer = new File( baseFile, DEFAULT_CACHE_DIR );
		else
			answer = new File( DEFAULT_CACHE_DIR );
		return answer;
	}

	// Clear memory and disk cache
	public static boolean clearReportCache( String inConfigURI )
	{
		final String kFName = "clearReportCache";
		cReportCache = null;

		File cacheDir = calcCacheDir( inConfigURI );
		boolean hadAProblem = false;
		if( cacheDir.exists() ) {
			String fileNames[] = cacheDir.list();
			if( null!=fileNames ) {
				for( int i=0; i<fileNames.length; i++ ) {
					File targetFile = new File( cacheDir, fileNames[i] );
					if( ! targetFile.delete() ) {
						staticErrorMsg( kFName, "Cache file not deleted." );
						hadAProblem = true;
					}
				}
			}
		}

		// Do it a second time
		cReportCache = null;
		return hadAProblem;
	}

	public Element getOrCalculateMainContent(
			AuxIOInfo inRequestObject,
			AuxIOInfo inResponseObject,
			Hashtable inValuesHash,
			int inInterval
		)
			throws ReportException
	{
		final String kFName = "getOrCalculateMainContent";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		String cacheKey = calcCacheKey( inInterval );

		debugMsg( kFName, "Cache key = " + cacheKey );

		// Step 1: Check memory

		// Return the answer if cached in memory
		if( null!=cReportCache && cReportCache.contains( cacheKey ) ) {
			Element tmpElem = (Element) cReportCache.get( cacheKey );
			try {
				Element tmpElem2 = (Element) tmpElem.clone();
				return tmpElem2;
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Error cloning answer from memory cache."
					+ " Will check disk cache or rerun report instead."
					+ " Error: " + e
					);
			}
		}


		if( null==cReportCache )
			cReportCache = new Hashtable();

		// Step 2: Check Disk

		File cacheDir = null;
		String cacheDirStr = null;
		try {
			// Now we check the disk cache
			// cacheDir = new File( DEFAULT_CACHE_DIR, getMainConfig().getConfigFileURI() );
			String baseURI = getMainConfig().getConfigFileURI();

			cacheDir = calcCacheDir( baseURI );

			if( ! cacheDir.exists() )
				cacheDir.mkdirs();	
			if( ! cacheDir.exists() || ! cacheDir.isDirectory() || ! cacheDir.canRead() || ! cacheDir.canWrite() ) {
				String msg = "Problem with cache directory (1):"
					+ " path=" + cacheDir.getCanonicalPath()
					+ ", exists=" + cacheDir.exists()
					+ ", isDir=" + cacheDir.isDirectory()
					+ ", canRead=" + cacheDir.canRead()
					+ ", canWrite=" + cacheDir.canWrite()
					;
				throw new IOException( "IO: " + msg );
			}
			cacheDirStr = cacheDir.getCanonicalPath();
		}
		catch( Exception io1 ) {
			errorMsg( kFName,
				"Problem with cache dir, reports will not be cached."
				+ " cacheDir=" + cacheDir
				+ " Error: " + io1
				);
			cacheDir = null;
		}

		String fileName = cacheKey + ".xml";
		File cacheFileObj = null;
		Element outElem = null;
		if( null!=cacheDir ) {
			try {
				// cacheFileObj = new File( fileName, cacheDirStr );
				cacheFileObj = new File( cacheDirStr, fileName );
				JDOMHelper cachedTree = new JDOMHelper( cacheFileObj );
				outElem = cachedTree.getJdomElement();
				outElem.detach();
			}
			catch( Exception io2 ) {
				infoMsg( kFName,
					"Did not load from disk cache, mostly likely not there."
					+ " Error: " + io2
					);
			}
		}

		// Save and return the answer, if we have one
		if( null!=outElem ) {
			try {
				Element tmpElem = (Element) outElem.clone();
				cReportCache.put( cacheKey, tmpElem );
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Error cloning answer into memory cache. (1)"
					+ " Will still return the disk-cached answer."
					+ " Error: " + e
					);
			}
			return outElem;
		}

		// OK, we need to actually go caculate it
		// =============================================
		outElem = calculateMainContent(
			inRequestObject, inResponseObject, inValuesHash, inInterval
			);

		// Save anser in memory and to disk
		if( null!=outElem ) {
			outElem.detach();
			// Clone and cache in memory
			try {
				Element tmpElem = (Element) outElem.clone();
				cReportCache.put( cacheKey, tmpElem );
			}
			catch( Exception e ) {
				errorMsg( kFName,
					"Error cloning answer into memory cache. (2)"
					+ " Will still return the calculated answer."
					+ " Error: " + e
					);
			}
			// And cache to disk
			if( null!=cacheFileObj )
				JDOMHelper.writeToFile( outElem, cacheFileObj );
		}

		// We're done!
		return outElem;
	}


	public Element calculateMainContent(
			AuxIOInfo inRequestObject,
			AuxIOInfo inResponseObject,
			Hashtable inValuesHash,
			int inInterval
		)
			throws ReportException
	{
		final String kFName = "calculateInnterContent";
		final String kExTag = kClassName() + '.' + kFName + ": ";
		boolean debug = shouldDoDebugMsg( kFName );

		// int howMany = DEFAULT_HOW_MANY_ROWS;
		int howMany = getDesiredRowCount( inRequestObject );


		int movementImgWidth = DEFAULT_MOVEMENT_ICON_WIDTH;
		int movementImgHeight = DEFAULT_MOVEMENT_ICON_HEIGHT;

		// String lGraphicURLPrefix = DEFAULT_IMAGE_URL_PREFIX;
		String lGraphicURLPrefix = ReportConstants.IMAGE_URL_PREFIX;

		// We'll use this hash for varaible substitution in Strings
		// Hashtable lMasterValuesHash = getVariables( inRequestObject );
		Hashtable lMasterValuesHash = inValuesHash;


		Element mainContentTable = new Element( "table" );

		// Now we start working on the query
		//////////////////////////////////////////////////

		/***
		Calendar lCalendar = Calendar.getInstance();
		String lCurrentIntervalEnd = getDateString( lCalendar );

		lCalendar.add( Calendar.DATE, - inInterval );
		String lCurrentIntervalStart = getDateString( lCalendar );

		lCalendar.add( Calendar.DATE, - inInterval );
		String lPreviousIntervalStart = getDateString( lCalendar );
		***/

		// Get the range dates, truncated to midnight
		String lCurrentIntervalEnd = getDBConfig().calculateDateTimeStringForNDaysPast(
			0, true, true
			);
		String lCurrentIntervalStart = getDBConfig().calculateDateTimeStringForNDaysPast(
			inInterval, true, true
			);
		String lPreviousIntervalStart = getDBConfig().calculateDateTimeStringForNDaysPast(
			2 * inInterval, true, true
			);


		/***
		String qry =
			"SELECT *\n" +
			"FROM (\n" +
			"	SELECT  query,\n" +
			"		num_requests,\n" +
			"		ROWNUM " + getDBConfig().getVendorAliasString() + " thePosition\n" +
			"	FROM (\n" +
			"		SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n" +
			"			count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n" +
			"		FROM " + getDefaultTableName() + "\n" +
			"		WHERE start_time >= '" + lCurrentIntervalStart + "'\n" +
			"				     AND start_time <='" + lCurrentIntervalEnd + "'\n" +
			"		GROUP BY normalized_query\n" +
			"		ORDER BY count(*) DESC, normalized_query\n" +
			"	) innter_select\n" +
			") mid_select\n" +
			"WHERE thePosition < " + howMany + "\n"
			;
		***/

		String qry =
			"SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n"
			+ " count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n"
			+ " FROM " + getDefaultTableName() + "\n"
			+ " WHERE start_time >= " + lCurrentIntervalStart + "\n"
			+ " AND start_time < " + lCurrentIntervalEnd + "\n"
			+ " GROUP BY normalized_query\n"
			+ " ORDER BY count(*) DESC, normalized_query\n"
			;

		debugMsg( kFName, "Will run SQL=" + qry );




		//
		// RUN the query!!!
		// ==================================================
		// (and only build the headers if we had success)
		//
		ResultSet lThisTimeIntervalResultSet = null;
		Statement lThisStatement = null;
		Connection lThisConnection = null;


		ResultSet lOldTimeIntervalResultSet = null;
		Statement lOldStatement = null;
		Connection lOldConnection = null;

		// How many columns we expect
		int numCols = 4;


		try {
			// lThisTimeIntervalResultSet = getDBConfig().runQuery( qry );
			Object [] objs1 = getDBConfig().runQuery( qry, true );
			lThisTimeIntervalResultSet = (ResultSet) objs1[0];
			lThisStatement = (Statement) objs1[1];
			lThisConnection = (Connection) objs1[2];

			debugMsg( kFName,
				"lThisTimeIntervalResultSet=" + lThisTimeIntervalResultSet
				+ ", lThisStatement=" + lThisStatement
				+ ", lThisConnection" + lThisConnection
				);

			// Start building the output document
			// Element resultsTableElem = new Element( "table" );
			// resultsTableElem.setAttribute( "class", "nie_results_table" );
			// Hold off adding it until we know how well we did

			// font stuff we will use
			final String kFontClass = CSSClassNames.TREND_FONT;
			final String kFontSize = "-1";
			final String kFontTag = "/font/@class=" + kFontClass + "/@size=" + kFontSize;

			// The temp element we will reuse as we build
			Element tmpElem = null;

			int row=0;

			// We forego adding the headers until we have at least
			// one row, see below


			debugMsg( kFName, "starting loop" );

			int desiredRowCount = getDesiredRowCount( inRequestObject );
			int minRowCount = getMinRowCount( inRequestObject );


			int lCurrentPosition = 0;	// simulates rownum
			while( lThisTimeIntervalResultSet.next() ) {

				debugMsg( kFName, "Top of main Loop" );

				String lQueryTerm = lThisTimeIntervalResultSet.getString( "query" );
				if( null==lQueryTerm && SHOULD_SKIP_NULL_QUERIES )
					continue;

				lCurrentPosition++;	// simulates rownum

				if( lCurrentPosition > desiredRowCount )
					break;


				// If we're about to add the first row, go ahead and
				// add the headers as well
				if( 1==lCurrentPosition )
					row = addHeaders( mainContentTable, inInterval, numCols, kFontTag );


				String lQueryDisplay = null==lQueryTerm ? "(null)" : lQueryTerm;
				if( lQueryDisplay.length() > DISPLAY_QUERY_TRUNCATION_LENGTH )
					lQueryDisplay = lQueryDisplay.substring( 0, DISPLAY_QUERY_TRUNCATION_LENGTH ) + "...";

				// these rows are for the results
				row++;
				Element rowElem = JDOMHelper.findOrCreateElementByPath(
					mainContentTable,		// Starting at
					"tr[" + row + "]",	// path
					true	// Yes, tell us about errors
					);

				int lNumResults = lThisTimeIntervalResultSet.getInt( "num_requests" );
				// int lCurrentPosition = lThisTimeIntervalResultSet.getInt( "thePosition" );

				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[1]/@align=left" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( lQueryDisplay );


				String qry2 = null;
				if( getDBConfig().getConfiguredVendorTag().equals(DBConfig.VENDOR_TAG_ORACLE) ) {
					qry2 =
						"SELECT *\n" +
						"FROM (\n" +
						"	SELECT query,\n" +
						"		num_requests,\n" +
						"		ROWNUM " + getDBConfig().getVendorAliasString() + " thePosition\n" +
						"	FROM (\n" +
						"		SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query,\n" +
						"			count(*) " + getDBConfig().getVendorAliasString() + " num_requests\n" +
						"		FROM " + getDefaultTableName() + "\n" +

						// "		WHERE start_time BETWEEN '" + lPreviousIntervalStart + "'\n" +
						// "				     AND '" + lCurrentIntervalStart + "'\n" +

						"		WHERE start_time >= " + lPreviousIntervalStart + "\n" +
						"				     AND start_time < " + lCurrentIntervalStart + "\n" +


						"		GROUP BY normalized_query\n" +
						"		ORDER BY count(*) DESC, normalized_query\n" +
						"	)\n" +
						")\n"
						;
						// Add the query term, or null
						if( null!=lQueryTerm )
							qry2 += "WHERE query='" + lQueryTerm + "'\n";
						else
							qry2 += "WHERE query IS NULL\n";
				}
				else {
					qry2 =
						"SELECT normalized_query " + getDBConfig().getVendorAliasString() + " query"
						+ ", count(*) " + getDBConfig().getVendorAliasString() + " num_requests"
						+ " FROM " + getDefaultTableName()
						+ " WHERE start_time >= " + lPreviousIntervalStart
						+ " AND start_time < " + lCurrentIntervalStart
						+ " GROUP BY normalized_query"
						+ " ORDER BY count(*) DESC, normalized_query"
						;
				}

				debugMsg( kFName, "qry2=" + qry2 );

				int lOldPosition = -1;
				String lOldPositionString = "";
				String lGraphicName;
				String lGraphicDescription;

				try {

					debugMsg( kFName, "Attempting to get oldInterval objects" );
		    
					// lOldTimeIntervalResultSet = getDBConfig().runQuery( qry2 );
					Object [] objs2 = getDBConfig().runQuery( qry2, true );
					lOldTimeIntervalResultSet = (ResultSet) objs2[0];
					lOldStatement = (Statement) objs2[1];
					lOldConnection = (Connection) objs2[2];

					debugMsg( kFName,
						"lOldTimeIntervalResultSet=" + lOldTimeIntervalResultSet
						+ ", lOldStatement=" + lOldStatement
						+ ", lOldConnection" + lOldConnection
						);

					// It's Oracle, we can read the results directly
					if( getDBConfig().getConfiguredVendorTag().equals(DBConfig.VENDOR_TAG_ORACLE) ) {
						lOldTimeIntervalResultSet.next();
						lOldPosition = lOldTimeIntervalResultSet.getInt( "thePosition" );
					}
					// Else not oracle, scan the results set
					else {
						// Maintain the 2nd rownum counter simulator
						int tmpCounter = 0;
						debugMsg( kFName, "non-Oracle scan of oldResults" );

						// For each record
						while( lOldTimeIntervalResultSet.next() ) {
							tmpCounter++;
							String tmpTerm = lOldTimeIntervalResultSet.getString( "query" );
							tmpTerm = NIEUtil.trimmedStringOrNull( tmpTerm );

							// If both null
							if( null==lQueryTerm && null==tmpTerm ) {
								lOldPosition = tmpCounter;
								break;
							}

							// If not null and match
							if( null!=lQueryTerm && null!=tmpTerm && lQueryTerm.equals(tmpTerm) ) {
								lOldPosition = tmpCounter;
								break;
							}

							// Else keep looking
						}	// End for each record
						// In keeping with older Oracle logic, throw an exception
						// if we didn't find anything
						if( lOldPosition < 0 )
							throw new Exception( "Term not found in previous interval." );
					}

					lOldPositionString = "" + lOldPosition;

					if( lOldPosition > lCurrentPosition ) {
						lGraphicName = "moving-up.gif";
						lGraphicDescription = "Moved Up";
					}
					else if( lOldPosition < lCurrentPosition ) {
						lGraphicName = "moving-down.gif";
						lGraphicDescription = "Moved Down";
					} else {
						lGraphicName = "did-not-move.gif";
						lGraphicDescription = "No Change / Same as Before";
					}
				} catch( Exception e ) {
					debugMsg( kFName, "Caught excption :" + e );
					if( debug )
						e.printStackTrace( System.out );

					lGraphicName = "new-1.gif";
					lGraphicDescription = "New Term";
					lOldPositionString = ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT;
				}
				finally {
					// quickie cleanup!
					debugMsg( kFName, "closing oldResults" );
					lOldTimeIntervalResultSet = DBConfig.closeResults( lOldTimeIntervalResultSet, kClassName(), kFName, false );
					debugMsg( kFName, "closing oldStatement" );
					lOldStatement = DBConfig.closeStatement( lOldStatement, kClassName(), kFName, false );
					debugMsg( kFName, "Have been closed" );
				}

				// Fix up the query and image strings
				lGraphicName = lGraphicURLPrefix + lGraphicName;

				// Add the 2nd column, the marker image
				Element imgElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[2]/@align=center"					+ "/img/@width=" + movementImgWidth + "/@height=" + movementImgHeight,
					true	// Yes, tell us about errors
					);
				// Must add src= separately since that URL has slashes!
				imgElem.setAttribute( "alt", lGraphicDescription );
				imgElem.setAttribute( "src", lGraphicName );
				debugMsg( kFName, "graphic name = \"" + lGraphicName + "\"" );

				// The 3rd and 4th columns are the counts
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[3]/@align=center" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( "" + lCurrentPosition );

				tmpElem = JDOMHelper.findOrCreateElementByPath(
					rowElem,		// Starting at
					"td[4]/@align=center" + kFontTag,	// path
					true	// Yes, tell us about errors
					);
				tmpElem.addContent( lOldPositionString );

				debugMsg( kFName, "Bottom of main Loop." );

			}	// End for each row in the trend report

			debugMsg( kFName, "Completed Loop." );

			// If no records, add special message
			if( 0==lCurrentPosition ) {
				// these rows are for the results
				row++;
				Element noDataElem = JDOMHelper.findOrCreateElementByPath(
					mainContentTable,		// Starting at
					"tr[" + row + "]/td/@width=200/@colspan=" + numCols,	// path
					true	// Yes, tell us about errors
					);
				noDataElem.addContent( getNoDataElement() );
			}
			// If too few records, add additional rows
			else if( lCurrentPosition < minRowCount ) {
				for( int i=lCurrentPosition+1 ; i<=minRowCount ; i++ ) {
					// these rows are for the results
					row++;
					Element rowElem = JDOMHelper.findOrCreateElementByPath(
						mainContentTable,		// Starting at
						"tr[" + row + "]",	// path
						true	// Yes, tell us about errors
						);
					// For each column
					for( int j=1; j<=numCols ; j++ ) {
						tmpElem = JDOMHelper.findOrCreateElementByPath(
							rowElem,		// Starting at
							"td[" + j + "]/@align=center" + kFontTag,	// path
							true	// Yes, tell us about errors
							);
						tmpElem.addContent( ReportConstants.DEFAULT_DISPLAY_NULL_VALUE_CONSTANT );
					}
				}
			}


			// Add the bottom HR and legend, if there were some records
			if( lCurrentPosition > 0 ) {
				// Add the bottom <hr> 
				row++;
				JDOMHelper.findOrCreateElementByPath(
					mainContentTable,		// Starting at
					"tr[" + row + "]/td/@colspan=" + numCols + "/@align=center/hr/@width=70%",	// path
					true	// Yes, tell us about errors
					);

				// Add the legend
				row++;
				String legendGraphic = "legend.gif";
				legendGraphic = lGraphicURLPrefix + legendGraphic;
				tmpElem = JDOMHelper.findOrCreateElementByPath(
					mainContentTable,		// Starting at
					"tr[" + row + "]/td/@colspan=" + numCols + "/@align=center/img/@alt=Icon Legend",	// path
					true	// Yes, tell us about errors
					);
				tmpElem.setAttribute( "src", legendGraphic );
			}

		// } catch (SQLException e) {
		} catch (Exception e) {
			e.printStackTrace( System.err );
			throw new ReportException( kExTag
				+ "Error running report."
				+ " Report = \"" + getReportName() + "\""
				+ " SQL Query =\"" + qry + "\""
				+ " Error: " + e
				);
		}
		finally {
			// quickie cleanup!
			lThisTimeIntervalResultSet = DBConfig.closeResults( lThisTimeIntervalResultSet, kClassName(), kFName, false );
			lThisStatement = DBConfig.closeStatement( lThisStatement, kClassName(), kFName, false );
			// lThisConnection = DBConfig.closeConnection( lThisConnection, kClassName(), kFName, false );

			// Done above, don't do it here
			// lOldTimeIntervalResultSet = DBConfig.closeResults( lOldTimeIntervalResultSet, kClassName(), kFName, false );
			// lOldStatement = DBConfig.closeStatement( lOldStatement, kClassName(), kFName, false );
			// lOldConnection = DBConfig.closeConnection( lOldConnection, kClassName(), kFName, false );
		}

		return mainContentTable;
	}






	private int addHeaders( Element mainContentTable,
		int inInterval, int numCols,
		String inFontStr
		)
	{
		// The first row is the main title
		// it spans four columns
		int row=1;

		String lPluralS = inInterval!=1 ? "s" : "";

		Element tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/th/@colspan=" + numCols + "/@align=center",	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( getTitle(null) );

		// The second row gives the time interval
		// since it compares two invervals, it's twice what you might initially expect
		row++;
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td/@colspan=" + numCols + "/@align=center" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		// tmpElem.addContent( "for the last " + (inInterval*2) + " day" + lPluralS );
		// tmpElem.addContent( "for the last " + (inInterval*2) + " days" );
		// ^^^ ALWAYS plural, even if 1 day, this is interval times 2
		// tmpElem.addContent( "for the last " + (interval*2) + " days" );
		// String tmpMsg = "for the last " + (interval*2) + " days";
		// Per Miles and Sean, though I disagree, don't double interval for subtitle
		// even though technically it does show data for 2 N back...
		String tmpMsg = "for the last" + (1==inInterval ? "" : " "+inInterval)
			+ " day" + (1==inInterval ? "" : "s")
			;
		tmpElem.addContent( tmpMsg );



		// The third row is more header text "Results List Ranking"
		// the third row has a spacer
		row++;
		JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td",	// path
			true	// Yes, tell us about errors
			);
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[2]/@colspan=3/@align=center" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( "Results List Ranking" );


		// The fourth row has the 4 main column headings
		row++;
		// Heading 1: "Search Term" (align left)
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[1]/@align=left" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( "Search Term" );
		// Heading 2: "Change" (align center)
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[2]/@align=center" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( "Change" );
		// Heading 3: "Last N Days" (align center)
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[3]/@align=center" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( "Last " + (inInterval!=1 ? ""+inInterval : "") + " day" + lPluralS );
		// Heading 4: "Prior N Days" (align center)
		tmpElem = JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[4]/@align=center" + inFontStr,	// path
			true	// Yes, tell us about errors
			);
		tmpElem.addContent( "Prior " + (inInterval!=1 ? ""+inInterval : "") + " day" + lPluralS );

		// The fifth row just has the four <hr> tags
		row++;
		JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[1]/hr",	// path
			true	// Yes, tell us about errors
			);
		JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[2]/hr",	// path
			true	// Yes, tell us about errors
			);
		JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[3]/hr",	// path
			true	// Yes, tell us about errors
			);
		JDOMHelper.findOrCreateElementByPath(
			mainContentTable,		// Starting at
			"tr[" + row + "]/td[4]/hr",	// path
			true	// Yes, tell us about errors
			);

		return row;
	}

	// Check your args
	private void addTabLink(
		AuxIOInfo inRequestObject,
		Element inMainTable, ReportLink inBaseLink,
		int inTargetRow, int inTargetTab,
		int inTargetDays, String inTargetName, int inCurrDays
		)
			throws ReportException
	{
		final String kFName = "addTabLink";
		final String kExTag = kClassName() + '.' + kFName + ": ";

		if( null==inRequestObject )
			throw new ReportException( "Null request object passed in." );
		if( null==inMainTable )
			throw new ReportException( "Null main table passed in." );
		if( null==inBaseLink )
			throw new ReportException( "Null base link passed in." );
		if( null==inTargetName )
			throw new ReportException( "Null target name passed in." );


		String path = "tr[" + inTargetRow + "]/td[" + inTargetTab + "]/@align=center/@valign=top";
		Element navTab = JDOMHelper.findOrCreateElementByPath(
			inMainTable,		// Starting at
				path,	// path
				true	// Yes, tell us about errors
				);
		if( null==navTab )
			throw new ReportException( "Got null tab, path=" + path );

		String linkText = inTargetName;
		Element linkElem = null;
		if( inTargetDays==inCurrDays ) {
			// linkElem = new Element( "div" );
			linkElem = new Element( "font" );
			linkElem.setAttribute( "class", nie.sn.CSSClassNames.INACTIVE_MENU_LINK );
			// linkElem.setAttribute( "class", nie.sn.CSSClassNames.ACTIVE_MENU_LINK );
			linkElem.addContent( linkText );
		}
		else {
			navTab.setAttribute( "bgcolor", TREND_BOX_OTHER_TAB_COLOR );
			linkElem = inBaseLink.generateRichLink(
					inRequestObject,
					null,
					null,
					true,
					"" + inTargetDays,
					null,
					null
					);
			if( null==linkElem )
				throw new ReportException( "Got null tab, path=" + path );
			linkElem.setText( inTargetName );
		}
		navTab.addContent( linkElem );
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
				+ "/@align=center"
				+ "/@class=" + CSSClassNames.COMPACT_MENU_BOTTOM_ROW_CELL
				,
				true
				);
			// Add Sean's time period background colors
			if( daysStr.equals("1") )
				cell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_DAY_BGC );
			else if( daysStr.equals("7") )
				cell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_WEEK_BGC );
			else if( daysStr.equals("30") )
				cell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_MONTH_BGC );
			else if( daysStr.equals("90") )
				cell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_QUARTER_BGC );
			else if( daysStr.equals("365") )
				cell.setAttribute( "bgcolor", ReportConstants.MENU_TEXT_COMPACT_YEAR_BGC );

			// Get the link and add the link to it
			// Element linkElem = link.generateRichLink(
			//	inRequest, getReportName(), null, true
			//	);
			Element linkElem = generateSublink( inRequest, daysStr );

			cell.addContent( linkElem );
		}

	}







	public void _generateMenuLinksToThisReportCompact(
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
				, true
				);

			// Get the link and add the link to it
			// Element linkElem = link.generateRichLink(
			//	inRequest, getReportName(), null, true
			//	);
			Element linkElem = generateSublink( inRequest, daysStr );

			cell.addContent( linkElem );
		}

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
			// linkText = "day";
			linkText = ReportConstants.MENU_TEXT_COMPACT_DAY;
		else if( inDays.equals("7") )
			// linkText = "week";
			linkText = ReportConstants.MENU_TEXT_COMPACT_WEEK;
		else if( inDays.equals("30") )
			// linkText = "month";
			linkText = ReportConstants.MENU_TEXT_COMPACT_MONTH;
		else if( inDays.equals("90") )
			// linkText = "quarter";
			linkText = ReportConstants.MENU_TEXT_COMPACT_QUARTER;
		else if( inDays.equals("365") )
			// linkText = "year";
			linkText = ReportConstants.MENU_TEXT_COMPACT_YEAR;
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




	Element _generateSublink( AuxIOInfo inRequestObject, String inDays )
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
		return "Search Activity Trends";
	}
	public String getTitleCompact( Hashtable inVars )
	{
		return "Trend";
	}
	public String getLinkText( Hashtable inVars )
	{
		return getTitle( inVars );
	}
	public String getLinkTextCompact( Hashtable inVars )
	{
		return getTitleCompact( inVars );
	}
	public String getSubtitleOrNull( Hashtable inVars )
	{
		return null;
	}

	public int getDesiredRowCount( AuxIOInfo inRequest )
	{
		final String kFName = "getDesiredRowCount";
		if( null==inRequest )
		{
			errorMsg( kFName,
				"No request object given."
				+ " Returning default number of rows "
				+ DEFAULT_TREND_DESIRED_ROW_COUNT
				);
			return DEFAULT_TREND_DESIRED_ROW_COUNT;
		}
		int outRows = inRequest.getIntFromCGIField(
			ReportConstants.DESIRED_ROW_COUNT_CGI_FIELD_NAME, DEFAULT_TREND_DESIRED_ROW_COUNT
			);
		// They may NOT want row counting
		if( outRows <= 0 )
			outRows = -1;
		return outRows;
	}

	public int getMinRowCount( AuxIOInfo inRequest )
	{
		return DEFAULT_TREND_MIN_ROW_COUNT;
	}

	public static Element getNoDataElement() {
		Element outElem = new Element( "center" );
		Element tmpElem = new Element( "b" );
		tmpElem.addContent(
			"No Trend Data Available"
			);
		outElem.addContent( tmpElem );

		// Add the bottom <hr> 
		Element hr = JDOMHelper.findOrCreateElementByPath(
			outElem,		// Starting at
			"hr/@width=70%",	// path
			true	// Yes, tell us about errors
			);

		tmpElem = new Element( "small" );
		outElem.addContent( tmpElem );

		// Element tmpElem2 = new Element( "p" );
		Element tmpElem2 = new Element( "div" );
		tmpElem2.addContent( "No trend data is available" );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " for the requested time period." );
		tmpElem.addContent( tmpElem2 );

		tmpElem2 = new Element( "p" );
		tmpElem2.addContent( "This is quite normal for a new installation," );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " or for sites with extremely light" );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " search traffic." );
		tmpElem.addContent( tmpElem2 );

		tmpElem2 = new Element( "p" );
		tmpElem2.addContent( "Note: Trend Reports operate on a" );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " midnight-to-midnight time interval," );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " and therefore will NOT show activity" );
		// tmpElem2.addContent( new Element( "br") );
		tmpElem2.addContent( " since 12AM today." );
		tmpElem.addContent( tmpElem2 );

		outElem.addContent( new Element( "br") );


		return outElem;
	}

	private static void staticErrorMsg(
			String inFromRoutine, String inMessage
			)
	{
		getRunLogObject().errorMsg( kStaticClassName, inFromRoutine,
			inMessage
			);
	}

	static Hashtable cReportCache;

	static final String TREND_BOX_SHADE_COLOR = "#bfd2e3";
	static final String TREND_BOX_OTHER_TAB_COLOR = "#bfbfbf";

	static final int DISPLAY_QUERY_TRUNCATION_LENGTH = 22;

	static final int DEFAULT_INTERVAL = 7;
	static final int _DEFAULT_HOW_MANY_ROWS = 10;

	public static final int DEFAULT_TREND_DESIRED_ROW_COUNT = 10;
	public static final int DEFAULT_TREND_MIN_ROW_COUNT = 5;

	static final int DEFAULT_MOVEMENT_ICON_WIDTH = 12;
	static final int DEFAULT_MOVEMENT_ICON_HEIGHT = 12;

	// static final String DEFAULT_IMAGE_URL_PREFIX = "http://foo/";
	// static final String DEFAULT_IMAGE_URL_PREFIX = "/files/images/sr2/";
	// static final String _DEFAULT_IMAGE_URL_PREFIX =
	//	nie.sn.SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX
	//	+ "images/sr2/"
	//	;

	static final String DEFAULT_CACHE_DIR = "report_cache";

	public static final boolean SHOULD_SKIP_NULL_QUERIES = true;

	// The list of link choices we will offer
	static final String [] kReportDays = { "1", "7", "30" };

}

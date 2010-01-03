/*
 * Created on Nov 18, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.sr2;

import java.util.*;

import nie.webui.xml_screens.CreateMapForm;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ReportConstants
{

	public static final boolean DEFAULT_SEARCH_LINK_VIA_OUR_PROXY = true;
	// ^^^ Even if this is set to FALSE, we must override it if the host
	// search application requires a POST
	// If they need a POST, but we need to present links (GETs), then the links
	// MUST come through us, so we can translate GET->POST

	private static void __Toolbar__() {}
	// public static final String MENU_TEXT_COMPACT_YEAR = "year";
	public static final String MENU_TEXT_COMPACT_YEAR = "Y";
	public static final String MENU_TEXT_COMPACT_YEAR_BGC = "#d0d0d0";
	// public static final String MENU_TEXT_COMPACT_YEAR_BGC = "#d0ffd0";
	// public static final String MENU_TEXT_COMPACT_YEAR_BGC = "#eeddff"; // light purple
	// public static final String MENU_TEXT_COMPACT_QUARTER = "quarter";
	public static final String MENU_TEXT_COMPACT_QUARTER = "Q";
	public static final String MENU_TEXT_COMPACT_QUARTER_BGC = "#d7d7d7";
	// public static final String MENU_TEXT_COMPACT_MONTH = "month";
	public static final String MENU_TEXT_COMPACT_MONTH = "M";
	public static final String MENU_TEXT_COMPACT_MONTH_BGC = "#e0e0e0";
	// public static final String MENU_TEXT_COMPACT_MONTH_BGC = "#c0ffff";
	// public static final String MENU_TEXT_COMPACT_MONTH_BGC = "#ddddff"; // light blue
	// public static final String MENU_TEXT_COMPACT_WEEK = "week";
	public static final String MENU_TEXT_COMPACT_WEEK = "W";
	public static final String MENU_TEXT_COMPACT_WEEK_BGC = "#f0f0f0";
	// public static final String MENU_TEXT_COMPACT_WEEK_BGC = "#e0e0ff";
	// public static final String MENU_TEXT_COMPACT_WEEK_BGC = "#ffffdd"; // yellow
	// public static final String MENU_TEXT_COMPACT_DAY = "day";
	public static final String MENU_TEXT_COMPACT_DAY = "D";
	public static final String MENU_TEXT_COMPACT_DAY_BGC = "#ffffff"; // white
	// public static final String MENU_TEXT_COMPACT_DAY_BGC = "#ffdddd"; // pink/red

	private static void __Access_Control__() {}
	// By default, most reports are available to read-only logged in users
	public static final int DEFAULT_ACCESS_LEVEL =
		nie.sn.SearchTuningConfig.BROWSE_PWD_SECURITY_LEVEL;
	public static final int DEFAULT_WRITEABLE_ACCESS_LEVEL =
		nie.sn.SearchTuningConfig.ADMIN_PWD_SECURITY_LEVEL;

	private static void __Markers__() {}
	// The internal tag WE USE to indicate a null value when creating
	// CGI filter links; this is referenced by some of the UI stuff as well
	// used to be in class XMLDefinedField
	public static final String INTERNAL_NULL_MARKER_SEQUENCE = "(null)";
	public static final String DEFAULT_DISPLAY_NULL_VALUE_CONSTANT = " - "; // Per Miles

	private static void __Dates_and_Times__() {}

	// Warning: the literal version of this is also used in many XML reports
	// so if you change it, you'll need to search those too
	public static final String DAYS_OLD_CGI_FIELD_NAME = "days";
	public static final String FILTER_DATETIME_START_FIELD_NAME = "from_date";
	public static final String FILTER_DATE_ONLY_START_FIELD_NAME = "from_date_truncated";
	public static final String FILTER_DATETIME_END_FIELD_NAME = "to_date";
	public static final String FILTER_DATE_ONLY_END_FIELD_NAME = "to_date_truncated";
	// What the local sysdate function is
	public static final String SYSTEM_VAR_FOR_DB_VENDOR_NVL_METHOD = "null_value_method";

	public static final String DEFAULT_DISPLAY_DATE_FORMAT = "mm/dd/yy";
	public static final String DEFAULT_DISPLAY_TIME_FORMAT = "hh12:mi:ss am";
	// public static final String DEFAULT_DISPLAY_TIME_FORMAT = "hh12:mi:ss am z";
	public static final String DEFAULT_DISPLAY_DATETIME_FORMAT = DEFAULT_DISPLAY_DATE_FORMAT
		+ ' ' + DEFAULT_DISPLAY_TIME_FORMAT;

	// !!NOTE!! If you change these, ALSO check DEFAULT_ORACLE_DATETIME_FORMAT below
	// TODO: For those that are for internal use, should be moved to DBConfig
	public static final String DEFAULT_SQL_DATE_FORMAT = "DD-MON-YYYY";
	public static final String DEFAULT_MYSQL_DATE_FORMAT = "YYYY-MM-DD";
	public static final String DEFAULT_SQL_TIME_FORMAT = "HH24:MI:SS";
	public static final String DEFAULT_SQL_DATETIME_FORMAT = DEFAULT_SQL_DATE_FORMAT
		+ ' ' + DEFAULT_SQL_TIME_FORMAT;
	public static final String DEFAULT_MYSQL_DATETIME_FORMAT = DEFAULT_MYSQL_DATE_FORMAT
		+ ' ' + DEFAULT_SQL_TIME_FORMAT;

	// NOTE: it is a CONCIDENCE that the format is the same
	public static final String DEFAULT_ORACLE_DATETIME_FORMAT = DEFAULT_SQL_DATETIME_FORMAT;

	private static void __Special_Reports__() {}
	// Finding maps
	// public static final String MAP_SELECTOR_REPORT_NAME = "ListMapsForTerm";
	// public static final String MAP_SELECTOR_REPORT_NAME = "ListMapsForTerm3";
	// In 2.9x we want to allow wildcards by default
	public static final String MAP_SELECTOR_REPORT_NAME = "ListMapsForTerm4";

	private static void __Menu_Display__() {}
	// Controlling the top menu of reports
	public static final int COMPACT_MENU_BORDER = 1;
	public static final int COMPACT_MENU_CELL_PADDING = 0; // 1, 2;
	public static final int COMPACT_MENU_CELL_SPACING = 0; // 2;

	private static void __Form_Labels__() {}
	// For hard coded Java reports
	public final static String SUGGESTED_NULL_SEARCH_TEXT = "(null search)";
	public final static String SHOW_RESULTS_TEXT = "Show results >>";
	public final static String CREATE_TEXT = "Create";
	public final static String CREATE_TEXT_FANCY = "Create >>";
	public final static String EDIT_TEXT = "Edit";
	public final static String EDIT_TEXT_FANCY = "Edit >>";
	public final static String VIEW_TEXT = "View";
	public final static String DELETE_TEXT = "Delete...";

	private static void __Other_Labels_and_Graphics__() {}
	// This is used in the java compiled reports
	public final static String SUGGESTED_MULTI_ITEM_MENU_SUFFIX = " in the last:";

	// public final static String IMAGE_URL_PREFIX = "/files/images/sr2/";
	public final static String IMAGE_URL_PREFIX =
		nie.sn.SnRequestHandler.FILE_CONTEXT_CGI_PATH_PREFIX
		+ "images/sr2/"
		;

	public final static String STATUS_ICON_IS_MAPPED = "green-checkmark-indicator.gif";
	public final static String STATUS_ICON_IS_MAPPED_MSG = "Term is Already Mapped";
	public final static String STATUS_ICON_NOT_MAPPED = "yellow-caution-indicator3.gif";
	public final static String STATUS_ICON_NOT_MAPPED_MSG = "Term is Not Mapped";
	public final static String STATUS_ICON_NO_HITS = "no-hits-indicator2.gif";
	public final static String STATUS_ICON_NO_HITS_MSG = "Term Not Mapped and NO HITS!";

	public final static int STATUS_ICON_WIDTH = 16;
	public final static int STATUS_ICON_HEIGHT = 16;

	private static void __CGI_Feild_Names__() {}
	public static final String _SITE_ID_CGI_FIELD_NAME = "site_id";

	public static final String REPORT_NAME_CGI_FIELD = "report";
	// Desired starting and stopping row count
	public static final String START_ROW_CGI_FIELD_NAME = "start_row";
	public static final String DESIRED_ROW_COUNT_CGI_FIELD_NAME =
		"num_rows";
	// TODO: ^^^ Could alias how_many and count
	public static final String SORT_SPEC_CGI_FIELD_NAME = "sort";
	// public static final String FILTER_SPEC_CGI_FIELD_NAME = "filter";
	public static final String FILTER_NAME_CGI_FIELD_NAME = "filter";
	public static final String _FILTER_PARAM_CGI_FIELD_NAME = "parm";

	// A global list of known CGI report fields
	public static List fMiscReportFields;
	static {
		fMiscReportFields = new Vector();

		// special field names
		fMiscReportFields.add( START_ROW_CGI_FIELD_NAME );
		fMiscReportFields.add( DESIRED_ROW_COUNT_CGI_FIELD_NAME );
		fMiscReportFields.add( SORT_SPEC_CGI_FIELD_NAME );
		fMiscReportFields.add( FILTER_NAME_CGI_FIELD_NAME );		// filter=
		fMiscReportFields.add( _FILTER_PARAM_CGI_FIELD_NAME );
		fMiscReportFields.add( REPORT_NAME_CGI_FIELD );	// report=
		fMiscReportFields.add( nie.sn.SnRequestHandler.NIE_CONTEXT_REQUEST_FIELD );	// nie_context=
		fMiscReportFields.add( nie.sn.SnRequestHandler.SN_OLD_CONTEXT_REQUEST_FIELD );
		fMiscReportFields.add( nie.sn.SnRequestHandler.ADMIN_CONTEXT_REQUEST_FIELD );	// command=
		// fMiscReportFields.add( SITE_ID_CGI_FIELD_NAME );	// site
		// ^^^ actually we might want to NOT clear this field if somebody has
		// logged into a site

		// General filter related fields
		// Some of these are now duplicated in the constants listed later
		fMiscReportFields.add( DAYS_OLD_CGI_FIELD_NAME );	// "days"
		fMiscReportFields.add( "term" );
		fMiscReportFields.add( "search" );
		fMiscReportFields.add( "url" );
		fMiscReportFields.add( "map_id" );
		fMiscReportFields.add( "ad_code" );
		fMiscReportFields.add( "client_host" );

		// And some stuff from the UI half of the house
		fMiscReportFields.add( nie.webui.xml_screens.Login.USERNAME_SUBMIT_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.Login.PASSWORD_SUBMIT_CGI_FIELD );

		// We do NOT INLCUDE the normal password / session IDs here
		// because this list is used as a copy mask later on when
		// generating links, and so if we list it here, then the password
		// will NOT be copied over
		// ??? fMiscReportFields.add( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_OBSCURED_FIELD );
		// Not fMiscReportFields.add( nie.sn.SnRequestHandler.ADMIN_CGI_PWD_FIELD );
		

		fMiscReportFields.add( nie.webui.UILink.OPERATION_CGI_FIELD );
		fMiscReportFields.add( nie.webui.UILink.MODE_CGI_FIELD );

		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.RETURN_URL_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.BUTTON_CGI_FIELD );
		fMiscReportFields.add( nie.webui.UILink.TERM_FORMGEN_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.TERMS_SUBMIT_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.MAP_ID_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.ALT_TERMS_SUBMIT_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.ALT_TERMS_HEADING_TEXT_SUBMIT_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.ALT_TERMS_HEADING_COLOR_SUBMIT_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.URL_HREF_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.URL_TITLE_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.URL_DESC_CGI_FIELD );
		fMiscReportFields.add( nie.webui.xml_screens.CreateMapForm.URL_ID_CGI_FIELD );

		fMiscReportFields.add( nie.webui.xml_screens.QueryMaps.NEW_TERMS_SUBMIT_CGI_FIELD );

		fMiscReportFields.add( nie.webui.UILink.META_CGI_FIELD );
		fMiscReportFields.add( nie.webui.UILink.META_CGI_FIELD_SCREEN_PARM );

	}


}

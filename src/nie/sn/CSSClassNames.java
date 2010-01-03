/*
 * Created on Nov 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package nie.sn;

/**
 * @author mbennett
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface CSSClassNames
{
	// Results list markup, wms = Web Master Suggests
	// USED BY OUR MARKUP
	public static final String WMS_SLOGAN_FONT = "nie_wms_slogan";
	public static final String ALT_SLOGAN_FONT = "nie_alt_term_slogan";
	public static final String ALT_TERM = "nie_alt_term";
	public static final String WMS_BOX1 = "nie_wms_outer_table";
	public static final String WMS_BOX2 = "nie_wms_inner_table";
	public static final String WMS_ICON = "nie_wms_icon";

	// The rest of these are mostly used in our own reporting system
	
	// General table building elements
	public static final String MAIN_CONTENT_TABLE = "nie_main_content_table";
	public static final String RESULTS_TABLE = "nie_results_table";
	public static final String TREND_TABLE = "nie_trend_table";
	public static final String HEADER_ROW = "nie_header_row";
	public static final String HEADER_CELL = "nie_header_cell";
	public static final String EVEN_ROW = "nie_even_row";
	public static final String ODD_ROW = "nie_odd_row";
	public static final String CONTAINER_CELL = "nie_container_cell";
	public static final String SPACER_CELL = "nie_spacer_cell";
	
	// Titles
	public static final String RPT_TITLE_TEXT = "nie_report_title";
	public static final String RPT_SUBTITLE_TEXT = "nie_report_subtitle";

	public static final String COMPACT_MENU_TABLE = "nie_cmenu_table";
	public static final String COMPACT_MENU_TOP_ROW = "nie_cmenu_row1";
	public static final String COMPACT_MENU_BOTTOM_ROW = "nie_cmenu_row2";
	public static final String COMPACT_MENU_TOP_ROW_CELL = "nie_cmenu_r1_cell";
	public static final String COMPACT_MENU_BOTTOM_ROW_CELL = "nie_cmenu_r2_cell";

	// Navigation links
	public static final String PAGING_LINK_ROW = "nie_paging_links_row";
	public static final String PAGING_LINK_CELL = "nie_paging_links_cell";

	public static final String ACTIVE_PAGING_LINK = "nie_active_paging_link";
	public static final String INACTIVE_PAGING_LINK = "nie_inactive_paging_link";

	public static final String MENU_CELL = "nie_menu_cell";

	public static final String ACTIVE_MENU_LINK = "nie_menu_link";
	public static final String INACTIVE_MENU_LINK = "nie_inactive_menu_link";

	public static final String ACTIVE_RPT_LINK = "nie_report_link";
	public static final String INACTIVE_RPT_LINK = "nie_inactive_report_link";

	// Misc
	public static final String HR_CELL = "nie_hr_cell";
	public static final String TREND_FONT = "nie_trend_font";

	// Used for for rows that have averages, counts, etc
	public final static String RPT_STATISTICS_ROW = "nie_stats_row";
	public final static String RPT_STATISTICS_CELL = "nie_stats_cell";

	public final static String _SEARCH_STATS_MSG_NUMBER_TEXT = "nie_stat_number";
	public final static String RPT_PAGING_STATS_MSG_NUMBER_TEXT = "nie_stat_number";
	public final static String RPT_PAGING_STATS_QUALIFIER_MSG_TEXT = "nie_stats_qualifier";

	public final static String NUMBER_TEXT = "nie_number";

	public final static String DATA_CELL = "nie_data_cell";
	public final static String SPECIAL_DATA_CELL = "nie_special_data_cell";
	public final static String COMPACT_DATA_CELL = "nie_compact_data_cell";
	public final static String NUMERIC_CELL = "nie_numeric_cell";
	public final static String DATETIME_CELL = "nie_datetime_cell";
	public final static String PERCENTAGE_CELL = "nie_percentage_cell";

	// For tight input forms
	public final static String COMPACT_FORM_ELEMENT = "nie_compact_form_element";


	/***
	public final static String _DEFAULT_TD_CSS_CLASS = "nie_data_cell";
	public final static String _DEFAULT_SPECIAL_TD_CSS_CLASS = "nie_special_data_cell";
	public final static String _DEFAULT_NUMERIC_TD_CSS_CLASS = "nie_numeric_cell";
	public final static String _DEFAULT_DATETIME_TD_CSS_CLASS = "nie_datetime_cell";
	public final static String _DEFAULT_PERCENTAGE_TD_CSS_CLASS = "nie_percentage_cell";
	public final static String _DEFAULT_TH_CSS_CLASS = "nie_header_cell";
	***/
}

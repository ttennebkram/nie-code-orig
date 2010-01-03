/*
 * Created on Dec 16, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.sr2.util;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportCache_Trend_30 extends ReportCacherBase {

	static final long MY_INTERVAL = nie.sn.CronLite.THREE_HOURS
		+ nie.sn.CronLite.MINUTE * 17L;	// week+7, month+17
	// ^^^ don't want all reports caching at the same time

	static final String REPORT_NAME = "ActivityTrend";
	static final String PARM_NAME = nie.sr2.ReportConstants.DAYS_OLD_CGI_FIELD_NAME;
	static final String PARM_VALUE = "30";

	// How often to run
	public long getRunIntervalInMS() {
		return MY_INTERVAL;
	}

	public String getReportName() {
		return REPORT_NAME;
	}
	public String getFilterName() {
		return PARM_NAME;
	}
	public String getFilterValue() {
		return PARM_VALUE;
	}

}

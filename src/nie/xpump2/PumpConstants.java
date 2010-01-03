/*
 * Created on Jun 2, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.xpump2;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface PumpConstants {

	public static final String DEFAULT_URL_FIELD_NAME = "url";
	public static final String DEFAULT_KEY_FIELD_NAME = "key";
	public static final String DEFAULT_KEY_FIELD_NAME2 = "id";
	public static final String DEFAULT_TITLE_FIELD_NAME = "title";
	public static final String DEFAULT_TITLE_FIELD_NAME2 = "subject";
	public static final String DEFAULT_DESC_FIELD_NAME = "description";
	public static final String DEFAULT_DESC_FIELD_NAME2 = "summary";
	public static final String _DEFAULT_DATE_FIELD_NAME = "date";
	public static final String DEFAULT_RAW_CONTENT_FIELD_NAME = "_content";
	public static final String DEFAULT_CONTENT_FIELD_NAME = "content";
	public static final String DEFAULT_TREE_FIELD_NAME = null;  // Default to none, html at root "_tree";
	public static final String LIKELY_TREE_FIELD_NAME = "html";
	public static final String DEFAULT_STATS_FIELD_NAME = "_stats";

	// By default, do ALL fields that match a name
	public static final int DEFAULT_HOW_MANY = -1;  // Negative and Zero means ALL, not None
	public static final int DEFAULT_KEEP_WHICH = 0;  // Zero means ALL, Negative might mean count from end of list at some point
	
}

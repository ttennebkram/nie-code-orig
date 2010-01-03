/*
 * Created on Dec 16, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.sn;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface CronLiteJob extends Runnable {

	// We will need the main config
	public void setMainConfig( SearchTuningConfig inConfig );
	// How often to run
	public long getRunIntervalInMS();
	// Was there a problem
	public boolean hadError();
}

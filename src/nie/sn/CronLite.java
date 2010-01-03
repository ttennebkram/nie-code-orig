/*
 * Created on Dec 15, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.sn;

import java.util.*;
import nie.core.*;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CronLite implements Runnable {

	// Useful for logging functions
	private static final String kClassName = "CronLite";

	public CronLite( SearchTuningApp inMainApp )
		throws Exception
	{
		mMainApp = inMainApp;
		mCronStarted = System.currentTimeMillis();
		mLastCompletedTimes = new Hashtable(); 
		mCurrentlyRunningStartTimes = new Hashtable();
		mActiveThreads = new Hashtable();
		instantiateJobs();
	}

	void instantiateJobs()
		throws Exception
	{
		mCronJobs = new Hashtable();
		for( int i=0; i<kCronTab.length; i++ ) {
			Class jobClass = kCronTab[i];
			Object classObj = jobClass.newInstance();
			CronLiteJob job = (CronLiteJob) classObj;
			mCronJobs.put( jobClass, job );
		}
	}

	public void run() {
		sleep( CRON_STARTUP_LAG );
		while( true ) {
			checkActiveThreads();
			checkCronTab();
			sleep( CRON_HEARTBEAT );
		}
	}

	void checkActiveThreads() {
		/***
		Collection classes = new ArrayList();
		classes.addAll( mActiveThreads.keySet() );
		for( Iterator cit = classes.iterator(); cit.hasNext() ; ) {
			Class thisClass = (Class) cit.next();
		***/

		// Traverse in a predictable order
		for( int i=0; i<kCronTab.length; i++ ) {
			Class thisClass = kCronTab[i];
			if( mActiveThreads.containsKey( thisClass ) ) {

				Thread thisThread = (Thread) mActiveThreads.get( thisClass );
				if( ! thisThread.isAlive() ) {
					recordJobEnded( thisClass );
				}

			}
		}
	}

	void checkCronTab() {
		// For each cron job class
		/***
		for( Iterator cit=mCronJobs.keySet().iterator(); cit.hasNext(); ) {
			Class thisClass = (Class) cit.next();
		***/
		// Traverse in a predictable order
		for( int i=0; i<kCronTab.length; i++ ) {
			Class thisClass = kCronTab[i];

			// Bail if it's currently running
			if( mCurrentlyRunningStartTimes.containsKey(thisClass) )
				continue;
			// Decide if we should run it
			boolean shouldRun = false;
			// First the initial timings
			long lastRanTime = -1L;
			if( mLastCompletedTimes.containsKey(thisClass) ) {
				Long longObj = (Long) mLastCompletedTimes.get(thisClass);
				lastRanTime = longObj.longValue();
			}
			// Get the job
			CronLiteJob thisJob = (CronLiteJob) mCronJobs.get( thisClass );
			// Before we ask it anything, give it a chance to look
			// at the latest config
			thisJob.setMainConfig( getMainConfig() );
			// Now ask it how often it likes to run
			long desiredInterval = thisJob.getRunIntervalInMS();
			// What time is it now?
			long currTime = System.currentTimeMillis();
			// And now the big question
			// Is it time to start?
			if( lastRanTime < 0 || lastRanTime+desiredInterval < currTime ) {
				startAndRecordJob( thisClass );	
				sleep( CRON_INTERJOB_STARTUP_MIN );
			}
		}
	}


	void startAndRecordJob( Class inClass ) {
		final String kFName = "startAndRecordJob";
		// Actually create the thread
		CronLiteJob thisJob = (CronLiteJob) mCronJobs.get( inClass );
		Thread thisThread = new Thread( thisJob );
		// Record it
		mActiveThreads.put( inClass, thisThread );
		// Start it!
		thisThread.start();
		// Update the timers
		mCurrentlyRunningStartTimes.put( inClass, new Long(System.currentTimeMillis()) );
		transactionStatusMsg( kFName, "Started job " + inClass.getName() );
	}

	void recordJobEnded( Class inClass ) {
		final String kFName = "recordJobEnded";
		// Update the timers
		long currTime = System.currentTimeMillis();
		mLastCompletedTimes.put( inClass, new Long( currTime ) );
		Long startedObj = (Long) mCurrentlyRunningStartTimes.get( inClass );
		long startedAt = startedObj.longValue();
		long duration = currTime - startedAt;
		mCurrentlyRunningStartTimes.remove( inClass );
		// Actually get rid of the thread
		mActiveThreads.remove( inClass );
		// Check and report status
		CronLiteJob thisJob = (CronLiteJob) mCronJobs.get( inClass );
		String runDurationStr = NIEUtil.formatTimeIntervalFancyMS( duration );
		if( thisJob.hadError() )
			warningMsg( kFName,
				"Job " + inClass.getName() + " ended with an error; check logs for details."
				+ " Runtime approx. " + runDurationStr
				);
		else
			infoMsg( kFName,
				"Job " + inClass.getName() + " has ended."
				+ " Runtime approx. " + runDurationStr
				);
	}

	void sleep( long inTime ) {
		try {
			Thread.sleep( inTime );
		}
		// No biggie
		catch( InterruptedException e ) {}
	}

	private SearchTuningApp getMainApp()
	{
		return mMainApp;
	}

	private SearchTuningConfig getMainConfig()
	{
		return getMainApp().getSearchTuningConfig();
	}

	private static void ___Sep_Run_Logging__(){}

	//////////////////////////////////////////////////////////////////

	// Handling Errors and warning messages
	// ************* NEW LOGGING STUFF *************

	private static RunLogInterface getRunLogObject()
	// can't access some of impl's extensions with interface reference
	//private static RunLogBasicImpl getRunLogObject()
	{
		// return RunLogBasicImpl.getRunLogObject();
		return RunLogBasicImpl.getRunLogObject();
	}

	// New style
	private static boolean statusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().statusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean transactionStatusMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().transactionStatusMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// New style
	private static boolean infoMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().infoMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean shouldDoInfoMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoInfoMsg( kClassName, inFromRoutine );
	}

	// Old style
	private void debugMsg( String inFromRoutine, String inMessage )
	{
		staticDebugMsg( inFromRoutine, inMessage );
	}

	private static void staticDebugMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().debugMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private static boolean shouldDoDebugMsg( String inFromRoutine )
	{
		return getRunLogObject().shouldDoDebugMsg( kClassName, inFromRoutine );
	}

	// New style
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


	// Old style
	private void warningMsg( String inFromRoutine, String inMessage )
	{
		staticWarningMsg( inFromRoutine, inMessage );
	}

	private static void staticWarningMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().warningMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	private void errorMsg( String inFromRoutine, String inMessage )
	{
		staticErrorMsg( inFromRoutine, inMessage );
	}

	private static void staticErrorMsg(
		String inFromRoutine, String inMessage
		)
	{
		getRunLogObject().errorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	// Newer style
	private static boolean fatalErrorMsg( String inFromRoutine, String inMessage )
	{
		return getRunLogObject().fatalErrorMsg( kClassName, inFromRoutine,
			inMessage
			);
	}

	Hashtable mCronJobs;
	Hashtable mActiveThreads;
	long mCronStarted;
	Hashtable mLastCompletedTimes;
	Hashtable mCurrentlyRunningStartTimes;

	private static void ___Sep_Fields_and_Constants__(){}
	//////////////////////////////////////////////////////////////

	SearchTuningApp mMainApp;

	public static final long SECOND			= 1000L;
	public static final long TEN_SECONDS	= SECOND * 10L;
	public static final long MINUTE			= SECOND * 60L;
	public static final long FIVE_MINUTES	= MINUTE * 5L;
	public static final long TEN_MINUTES	= MINUTE * 10L;
	public static final long FIFTEEN_MINUTES = MINUTE * 15L;
	public static final long TWENTY_MINUTES = MINUTE * 20L;
	public static final long HALF_HOUR		= MINUTE * 30L;
	public static final long HOUR			= MINUTE * 60L;
	public static final long THREE_HOURS	= HOUR * 3L;
	public static final long DAY				= HOUR * 24L;
	public static final long WEEK			= DAY * 7L;
	public static final long MONTH			= DAY * 30L;
	public static final long QUARTER		= DAY * 90L;
	public static final long YEAR			= DAY * 365L;


	public static final long CRON_STARTUP_LAG = HALF_HOUR; //TEN_SECONDS; // MINUTE;
	public static final long CRON_HEARTBEAT = MINUTE; // TEN_SECONDS;
	public static final long CRON_INTERJOB_STARTUP_MIN = MINUTE * 3L; // TEN_SECONDS;

	private static Class [] kCronTab = {
		nie.sr2.util.DNSLookup2.class,
		nie.sr2.util.ReportCache_Trend_1.class,
		nie.sr2.util.ReportCache_Trend_7.class,
		nie.sr2.util.ReportCache_Trend_30.class
		};

}

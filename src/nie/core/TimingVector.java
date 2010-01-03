/*
 * Created on Dec 10, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nie.core;

import java.util.*;

/**
 * @author Mark Bennett
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TimingVector {

	// These are used by SnRequestHandler when talking to the reporting system
	public static final String REPORT_CLASS_NAME = "timing";
	public static final String REPORT_METHOD_NAME = "report";

	public TimingVector() {
		// nothing special here
	}
	public TimingVector( String inEventName ) {
		this();
		this.addEvent( inEventName );
	}
	public TimingVector( String inEventName, long inTime ) {
		this();
		this.addEvent( inEventName, inTime );
	}

	public void addEvent( String inEventName ) {
		addEvent( inEventName, System.currentTimeMillis() );
	}
	public void addEvent( String inEventName, long inTime ) {
		inEventName = null==inEventName ? "(unknown)" : inEventName;
		mTimes.add( new Long(inTime) );
		mEventNames.add( inEventName );
	}

	public String reportStr() {
		StringBuffer outBuff = new StringBuffer();
		boolean isFirst = true;
		long baseTime = 0;
		long lastTime = 0;
		for( int i=0; i<mEventNames.size(); i++ ) {
			if( ! isFirst )
				outBuff.append( ' ' );	

			String eventStr = (String)mEventNames.get(i);
			outBuff.append( eventStr );

			Long timeObj = (Long)mTimes.get(i);
			long time = timeObj.longValue();

			outBuff.append( '(' );
			if( isFirst ) {
				baseTime = time;
				outBuff.append( baseTime );
			}
			else {
				outBuff.append( '+' );
				outBuff.append( time - lastTime );
				outBuff.append( '/' );
				outBuff.append( time - baseTime );
			}
			outBuff.append( ')' );

			lastTime = time;
			isFirst = false;
		}
		return new String( outBuff );
	}


	List mTimes = new ArrayList();
	List mEventNames = new ArrayList();

}

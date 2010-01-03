// A very simple class that does important work
// Basically, sit on a port and wait for a request
// When it gets a request, instantiate a hanlder object and run it
// as a thread
// And then wait for another request.

package nie.sn;

import java.net.*;
import java.util.*;
import java.io.*;

// for things like NIEUtil
import nie.core.*;

class SnHTTPServer implements Runnable
{

	private static final String kClassName = "SnHTTPServer";

	// private static boolean debug = true;
	// Don't specifically set it if you want FALSE, it'll default to that
//	private static boolean debug;
//	public static void setDebug( boolean flag )
//	{
//		debug = flag;
//	}

	public SnHTTPServer(
		int inServerPort,
		SearchTuningApp inSearchNamesApp,
		SearchTuningConfig inConfig,
		boolean inShouldStartServer
//		Hashtable inSearchNameHashMap,
//		SearchEngineConfig inSearchEngineConfig
		)
	{

		final String kFName = "constructor";

		// Store the main map dictionary so we can pass it to the
		// handler threads we will spawn
		// This is now handled via a getxxx method of the app
		// fHashMap = inSearchNameHashMap;

		// And store the VERY valuable information about the search engine
		// we'll be working with
		// fSearchEngine = inSearchEngineConfig;

		// We get anything we need to know from the main app
		fMainConfig = inConfig;
		if( null == fMainConfig )
		{
//			fatalErrorMsg( kFName,
//				"A null search config was passed in."
//				+ "Will exit application now."
//				);
//			System.exit( 4 );
			errorMsg( kFName,
				"A null SearchTrack config was passed in."
				+ " Server will try to enter pass-through mode if possible."
				);
		}
		fSearchNamesApp = inSearchNamesApp;
		if( fSearchNamesApp == null )
		{
			fatalErrorMsg( kFName,
				"A null search engine app was passed in."
				+ "Will exit application now."
				);
			System.exit( 5 );
		}

		// Only open the port if we really are supposed to start up
		if( inShouldStartServer )
		{
			// Startup a port to listen on
			try
			{
				fServerSocket = new ServerSocket( inServerPort );
			}
			catch( IOException eIO )
			{
				fatalErrorMsg( kFName,
					"Could not create the server socket."
					+ " Exception was: \"" + eIO + "\""
					+ " Some possible causes are:"
					+ " 1. Port already in use (port " + inServerPort + ")"
					+ ", perhaps the server is ALREADY RUNNING???"
					+ " 2. Insufficient privileges to create/serve in this port number range"
					+ "; some operating systems place restrictions on certain low numbered ports"
					+ ", or have a maximum valid port number they will accept."
					+ " 3. Insufficient privileges to create ANY port."
					+ " Until this error is corrected, the server can not start."
					);
				System.exit( 6 );
			}
		}
		else
		{
			statusMsg( kFName,
				"Told to NOT startup Server (presumably we're in test mode)"
				+ " so will NOT create socket on port " + inServerPort + "."
				);
		}
	}

//	// Help us display an octet as 0-255 vs -128 to 127
//	public static int octet( int inOctet )
//	{
//		return (inOctet < 0) ? (256 + inOctet) : inOctet;
//	}

	public void run()
	{
		final String kFName = "run";

		// Do forever!
		while( true )
		{

			debugMsg( kFName,
				"main loop:"
				+ " Top of main loop, about to call blocking .accept(). "
				);

			// Hang out on a port and listen for a request
			try
			{

				// This is a BLOCKING call
				// Socket lHandlerSocket = fServerSocket.accept();
				fHandlerSocket = fServerSocket.accept();

				long startTime = System.currentTimeMillis();

				// See if we should stop
				if( getShouldStopNow() )
				{
					debugMsg( kFName,
						"main loop:"
						+ " Exiting main loop as part of normal shutdown (A)."
						+ " And discarding anything that may have come over the socket."
						);
					break;
				}
				else
				{
					debugMsg( kFName,
						"main loop:"
						+ " Have accepted a connection."
						);
				}


				if( shouldDoTransactionStatusMsg( kFName ) )
				{

					transactionStatusMsg( kFName,
						// "New connection established from "
						"In from "
						+ AuxIOInfo.getIPAddressStringFromSocket( fHandlerSocket )
						);

				}

				// Instantiate a handler object and "run" it
				// And yes, he does need both the app and config, unlike
				// most other objects who just need the config
				SnRequestHandler lHandler = new SnRequestHandler(
					fHandlerSocket,
					getMainApplication(),
					// getMainConfig()
					getMainApplication().getSearchTuningConfig(),
					startTime
					);
//					lHandlerSocket, fHashMap,
//					fSearchEngine
//					);
				Thread lHandlerThread = new Thread( lHandler );
				debugMsg( kFName, "calling .start() on handler thread." );
				lHandlerThread.start();
			}
			catch( IOException eIO )
			{
				errorMsg( kFName,
					"Unable to read from socket."
					+ " Will continue to listen on this port."
					+ " Exception was \"" + eIO + "\""
					);
			}

			// See if we should stop
			if( getShouldStopNow() )
			{
				debugMsg( kFName, "main loop:"
					+ " Exiting main loop as part of normal shutdown (B)."
					);
				break;
			}
			else
			{
				debugMsg( kFName, "main loop:"
					+ " At bottom of main loop, will continue."
					);
			}

		}   // End while True
	}


	private SearchTuningApp getMainApplication()
	{
		return fSearchNamesApp;
	}
	private SearchTuningConfig getMainConfig()
	{
		return fMainConfig;
	}

	private boolean getShouldStopNow()
	{
		return getMainApplication().getShouldStopNow();
	}

	public void closeMainSocket( Thread inSocketThread )
	{
		final String kFName = "SnHTTPServer:closeMainSocket: ";

		final boolean doPreClosingActions = false;

		if( fHandlerSocket != null )
		{

			// As a workaround to stubborn sockets, we have some code to
			// try and smack it enough to pay attention
			// Since it's a bit of a kludge, it can be turned on and off
			// for experimenting
			if( doPreClosingActions )
			{

				debugMsg( kFName,
					"Performing pre-clsoing actions on main socket."
					+ " Use trace mode to see more details."
					);
				try
				{
					// if(debug) System.err.println( kFName + "Invoking: .setKeepAlive( false )" );
					// fHandlerSocket.setKeepAlive( false );

					traceMsg( kFName, "Invoking: .setTcpNoDelay( false )" );
					fHandlerSocket.setTcpNoDelay( false );

					traceMsg( kFName, "Invoking: .setSoTimeout( 0 )" );
					fHandlerSocket.setSoTimeout( 0 );
					// if(debug) System.err.println( kFName + "Invoking: .setSoLinger( false, 0 )" );
					// fHandlerSocket.setSoLinger( false, 0 );
					// Can't do set timeout at this point
					traceMsg( kFName, "Invoking: .shutdownInput()" );
					fHandlerSocket.shutdownInput();
					traceMsg( kFName, "Invoking: .shutdownOutput()" );
					fHandlerSocket.shutdownOutput();
					traceMsg( kFName, "No exceptions encountered on preclosing." );
				}
				catch (IOException ioe1)
				{
					errorMsg( kFName,
						"Unable to finish pre-close socket actiosn."
						+ " You will may have to kill the process manually."
						+ " Or you could try resubmitting the shutdown command with a browser refresh."
						+ " The exception was: \"" + ioe1 + "\""
						);
				}

			}
			else
				debugMsg( kFName, "Not asked to perform pre-clsoing actions on main socket." );

			debugMsg( kFName,
				"Closing main socket that we listen on."
				+ " Use trace mode for more details."
				);
			try
			{
				traceMsg( kFName, "Invoking: .close()" );
				fHandlerSocket.close();
				traceMsg( kFName, "No exceptions encountered on socket close." );

				// fHandlerSocket.
			}
			catch (IOException ioe2)
			{
				errorMsg( kFName,
					"Unable to close main server socket."
					+ " You will may have to kill the process manually."
					+ " Or you could try resubmitting the shutdown command with a browser refresh."
					+ " The exception was: \"" + ioe2 + "\""
					);
			}
			// Force it to null
			fHandlerSocket = null;
			debugMsg( kFName,
				"Have actually closed main socket that we listen on."
				);
		}
		else
		{
			errorMsg( kFName,
				"This method was called, but fHandlerSocket is NULL"
				+ ", can't close a null socket, so taking no action."
				);
		}



		// vvv None of this seems to work
		// socket.accept() doesn't seem to respond to such things
		// Try it again with the close option

		// Make sure we unblock the server if it's locked in .accept()
		if( inSocketThread != null )
		{
			debugMsg( kFName,
				"Shutting down socket " + inSocketThread
				+ " Use trace mode for more details."
				);
			traceMsg( kFName,
				"About to call .interrupt() for SnServer " + inSocketThread
				);
			inSocketThread.interrupt();
			traceMsg( kFName,
				"Back from call to .interrupt() for SnServer"
				);
			traceMsg( kFName,
				"Also calling .interrupt() for current thread " + Thread.currentThread()
				);
			Thread.currentThread().interrupt();
			traceMsg( kFName,
				"Back from calling .interrupt() for current thread"
				);
		}
		else
		{
			debugMsg( kFName,
				"Not calling .interrupt() for SnServer"
				+ " because fSnServerThread is null."
				+ " This may be normal if the application is just starting up."
				);
		}


	}

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











//	// The information about the search engine we'll be working with
//	private SearchEngineConfig fSearchEngine;

	// The main socket that we listen on
	private volatile Socket fHandlerSocket;

	// Where to go for anything we need to know
	private SearchTuningApp fSearchNamesApp;
	private SearchTuningConfig fMainConfig;

	private ServerSocket fServerSocket;
	// private Hashtable fHashMap;


}

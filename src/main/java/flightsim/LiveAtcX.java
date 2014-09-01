package flightsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectDataType;
import flightsim.simconnect.SimObjectType;
import flightsim.simconnect.recv.DispatcherTask;
import flightsim.simconnect.recv.EventHandler;
import flightsim.simconnect.recv.OpenHandler;
import flightsim.simconnect.recv.RecvEvent;
import flightsim.simconnect.recv.RecvOpen;
import flightsim.simconnect.recv.RecvSimObjectDataByType;
import flightsim.simconnect.recv.SimObjectDataTypeHandler;

/**
 * very simple utility to monitor the Primary radio frequency in FSX and play a
 * live stream based on the frequency code is based on jsimconnect example
 * snippet from the jsimconnect website
 * 
 */
public class LiveAtcX {

	private static int open_freq = 0; // last opened frequency
	private Player player = null; // object to play a live audio stream

	// Properties file
	static Properties props = new Properties();

	private static Logger log = Logger.getLogger("LiveAtcX");

	public static void main(String[] args) throws Exception {
		
		if( args.length > 0 && !args[0].isEmpty())
		{
			loadProperties();
			String url = props.getProperty(args[0]);
			log.info("Url=" + url);

			if (url != null) {

				// there is a URL, so start streaming from it
				Player player = new Player(url);
				player.start();
				player.join();
				System.exit(0);
			}
		}
		new LiveAtcX().run();

	}
	
	static private void loadProperties() throws Exception
	{
		/*
		 * try to open a properties file containing lines with frequency=url
		 * i.e. 132800=http://liveatc.net/something
		 * 
		 * The frequency is the radio freq * 1000
		 */
		String home = System.getProperty("user.home", "");
		File file = new File(home + "/liveatcx.properties");
		if (file.exists() && file.canRead()) {
			FileInputStream fileInput = new FileInputStream(file);
			props.load(fileInput);
			fileInput.close();
		}
		else
		{
			// if no properties file found, default to a built in one
			InputStream is = LiveAtcX.class.getResource("/properties").openStream();
			props.load(is);
			is.close();
		}
		
		props.list(System.out);

	}

	private void run() throws Exception {

		loadProperties();
		
		String host = props.getProperty("host", "localhost");
		String port = props.getProperty("port", "9017");

		int p = Integer.parseInt(port);

		final int dataDefID = 1;
		final int fourSeceventID = 1;

		// connect to simconnect
		SimConnect sc = new SimConnect("LiveAtcX", host, p);

		// build data definition
		sc.addToDataDefinition(dataDefID, "ATC TYPE", null,
				SimConnectDataType.STRING32);
		sc.addToDataDefinition(dataDefID, "ATC ID", null,
				SimConnectDataType.STRING32);
		sc.addToDataDefinition(dataDefID, "COM ACTIVE FREQUENCY:1", null,
				SimConnectDataType.INT32);

		// get warned every 4 seconds when in sim mode
		sc.subscribeToSystemEvent(fourSeceventID, "4sec");
		DispatcherTask dt = new DispatcherTask(sc);

		dt.addOpenHandler(new OpenHandler() {
			public void handleOpen(SimConnect sender, RecvOpen e) {
				System.out.println("Connected to " + e.getApplicationName());
			}
		});
		// add an event handler to receive events every 4 seconds
		dt.addEventHandler(new EventHandler() {
			public void handleEvent(SimConnect sender, RecvEvent e) {
				if (e.getEventID() == fourSeceventID) {
					// request data for the user's aircraft
					try {
						sender.requestDataOnSimObjectType(1, dataDefID, 1,
								SimObjectType.USER);
					} catch (IOException ioe) {
					}
				}
			}
		});
		// handler called when received data requested by the call to
		// requestDataOnSimObjectType
		dt.addSimObjectDataTypeHandler(new SimObjectDataTypeHandler() {
			public void handleSimObjectType(SimConnect sender,
					RecvSimObjectDataByType e) {

				String atcType = e.getDataString32();
				String atcID = e.getDataString32();
				int freq = e.getDataInt32();
				// print to users
				log.fine("Plane id#" + e.getObjectID() + " no "
						+ e.getEntryNumber() + "/" + e.getOutOf());
				log.fine("\tType/ID: " + atcType + " " + atcID);
				log.fine("\nFreq: " + freq);

				// if the radio frequency is different from the last one that
				// was processed, then
				// see if we need to stream
				if (open_freq != freq) {

					open_freq = freq;
					log.info("new freq=" + freq);

					// kill the existing stream player if one is active
					if (player != null && player.isAlive()) {
						log.info("Kill old player");
						player.terminate();

						try {
							player.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}

					// lookup the new frequency to see if there is a URL for it
					String url = props.getProperty(Integer
							.toString(freq / 1000));
					log.info("New url=" + url);

					if (url != null) {

						// there is a URL, so start streaming from it
						player = new Player(url);
						player.start();
					}

				} else if (player != null && !player.isAlive()) {

					// the player died, which happens eveyr so often in the
					// google libs, so restart
					// a player on the same freq
					String url = props.getProperty(Integer
							.toString(freq / 1000));
					log.info("New url=" + url);

					if (url != null) {
						player = new Player(url);
						player.start();
					}
				}
			}
		});
		// spawn receiver thread
		new Thread(dt).start();
	}

}

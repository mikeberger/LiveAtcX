package flightsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
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

	private static int open_freq[] = new int[] { 0, 0 }; // last opened
															// frequency
	private Player player[] = new Player[2]; // object to play a live audio
												// stream

	private static int paused = 0;

	// Properties file
	static Properties props = new Properties();

	private static Logger log = Logger.getLogger("LiveAtcX");

	public static void main(String[] args) throws Exception {

		if (args.length > 0 && !args[0].isEmpty()) {
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

	static private void loadProperties() throws Exception {
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
		} else {
			// if no properties file found, default to a built in one
			InputStream is = LiveAtcX.class.getResource("/properties")
					.openStream();
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
		final int pauseId = 2;

		// connect to simconnect
		SimConnect sc = null;
		while (sc == null) {
			log.info("Trying to connect...");
			try {
				sc = new SimConnect("LiveAtcX", host, p);

				// build data definition
				sc.addToDataDefinition(dataDefID, "ATC TYPE", null,
						SimConnectDataType.STRING32);
				sc.addToDataDefinition(dataDefID, "ATC ID", null,
						SimConnectDataType.STRING32);
				sc.addToDataDefinition(dataDefID, "COM ACTIVE FREQUENCY:1",
						null, SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM STATUS:1", null,
						SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM ACTIVE FREQUENCY:2",
						null, SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM STATUS:2", null,
						SimConnectDataType.INT32);

				// get warned every 4 seconds when in sim mode
				sc.subscribeToSystemEvent(fourSeceventID, "4sec");
				sc.subscribeToSystemEvent(pauseId, "Pause");
				DispatcherTask dt = new DispatcherTask(sc);

				dt.addOpenHandler(new OpenHandler() {
					public void handleOpen(SimConnect sender, RecvOpen e) {
						log.info("Connected to " + e.getApplicationName());
					}
				});
				// add an event handler to receive events every 4 seconds
				dt.addEventHandler(new EventHandler() {
					public void handleEvent(SimConnect sender, RecvEvent e) {
						if (e.getEventID() == fourSeceventID) {
							// request data for the user's aircraft
							try {
								sender.requestDataOnSimObjectType(1, dataDefID,
										1, SimObjectType.USER);
							} catch (IOException ioe) {
							}
						}
						if (e.getEventID() == pauseId) {
							// System.out.print("Paused: " + e.getData());
							paused = e.getData();
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
						int freq1 = e.getDataInt32();
						int status1 = e.getDataInt32();
						int freq2 = e.getDataInt32();
						int status2 = e.getDataInt32();

						log.fine("Plane id#" + e.getObjectID() + " no "
								+ e.getEntryNumber() + "/" + e.getOutOf());
						log.fine("\tType/ID: " + atcType + " " + atcID);
						log.fine("\nFreq1: " + freq1);
						log.fine("\nStatus1: " + status1);
						log.fine("\nFreq2: " + freq2);
						log.fine("\nStatus2: " + status2);

						if (status1 != 0) {
							freq1 = 0;
						}
						if (status2 != 0) {
							freq2 = 0;
						}

						updatePlayer(0, freq1);
						updatePlayer(1, freq2);

					}
				});
				// spawn receiver thread
				Thread t = new Thread(dt);
				t.start();
				t.join();
				sc = null;
				killPlayer(0);
				killPlayer(1);
			} catch (ConnectException e) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}

	}

	public void killPlayer(int playerId) {
		if (player[playerId] != null && player[playerId].isAlive()) {
			log.info("Kill old player:" + playerId);
			player[playerId].terminate();

			try {
				player[playerId].join();
				player[playerId] = null;
				open_freq[playerId] = 0;
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void startPlayer(int playerId, String url) {
		if (url != null) {
			log.info("New url=" + url);

			// there is a URL, so start streaming from it
			player[playerId] = new Player(url);
			player[playerId].start();
		}
	}

	private void updatePlayer(int playerId, int freq) {

		if (paused == 1) {
			
			killPlayer(playerId);

		} else if (open_freq[playerId] != freq) {

			/*
			 * if the radio frequency is different from the last one that was
			 * processed, then see if we need to stream
			 */
			open_freq[playerId] = freq;
			log.info("new freq=" + freq);

			// kill the existing stream player if one is active
			killPlayer(playerId);

			// lookup the new frequency to see if there is a URL
			// for it
			String url = props.getProperty(Integer.toString(freq / 1000));

			startPlayer(playerId, url);

		} else if (player[playerId] != null && !player[playerId].isAlive()) {

			// the player died, which happens eveyr so often in
			// the
			// google libs, so restart
			// a player on the same freq
			String url = props.getProperty(Integer.toString(freq / 1000));

			startPlayer(playerId, url);

		}

	}
}

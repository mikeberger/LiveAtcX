package flightsim;

import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;

import flightsim.simconnect.SimConnect;
import flightsim.simconnect.SimConnectConstants;
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
 * This class will set the active COM1 to the value of COM2.
 * It's only use is to allow the use of COM2 to tune COM1.
 * This is useful when using radar contact - which only uses COM1, and using the GTN 750 as COM1 - which can be more bothersome to 
 * tune during flight than COM2.
 * @author I
 *
 */
public class ComSync {

	private static int paused = 0;
	private static Logger log = Logger.getLogger("ComSync");
	public static void main(String[] args) throws Exception {
		new ComSync().run();
	}

	private void run() throws Exception {

		final int dataDefID = 1;
		final int eventID = 1;
		final int pauseId = 2;

		// connect to simconnect
		SimConnect sc = null;
		while (sc == null) {
			log.info("Trying to connect...");
			try {
		
				sc = new SimConnect("ComSync");

				sc.addToDataDefinition(dataDefID, "COM ACTIVE FREQUENCY:1",
						null, SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM STATUS:1", null,
						SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM ACTIVE FREQUENCY:2",
						null, SimConnectDataType.INT32);
				sc.addToDataDefinition(dataDefID, "COM STATUS:2", null,
						SimConnectDataType.INT32);

				sc.subscribeToSystemEvent(eventID, "1sec");
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
						if (e.getEventID() == eventID) {
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

						if( paused == 1) return;
						
						int freq1 = e.getDataInt32();
						int status1 = e.getDataInt32();
						int freq2 = e.getDataInt32();
						int status2 = e.getDataInt32();

						log.fine("Freq1: " + freq1);
						log.fine("Status1: " + status1);
						log.fine("Freq2: " + freq2);
						log.fine("Status2: " + status2);

						if (status1 != 0 || status2 != 0)
							return;
						

						if( freq2 != 0 && freq1 != freq2 )
						{
							try {
								
								String fs = Integer.toString(freq2);
								String hex = fs.substring(0, 5);
								log.fine(hex);
								
								sender.mapClientEventToSimEvent(99,"COM_RADIO_SET");
								sender.transmitClientEvent(SimConnect.OBJECT_ID_USER, 99, Integer.parseInt(hex,16), 1900000000, SimConnectConstants.EVENT_FLAG_GROUPID_IS_PRIORITY);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						

					}
				});
				// spawn receiver thread
				Thread t = new Thread(dt);
				t.start();
				t.join();
				sc = null;
				
			} catch (ConnectException e) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}

	}


}

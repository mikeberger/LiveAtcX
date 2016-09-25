package flightsim;

import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;

import flightsim.simconnect.NotificationPriority;
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
 * mask the elevator trim when autopilot is on.
 * 
 * Some aircraft do not mask the
 * elevator trim when autopilot is on. If the trim is tied to a wheel or other
 * lever, and a potentiometer sends in any signals while the autopilot is on,
 * the aircraft will shoot upwards or downwards depending on the difference
 * between the current auto trim and the bad signal.
 * 
 * Also, add a key assignment that will manually turn trim on/off for planes
 * that have custom auto pilots.
 *
 */
public class AutoMask {

	private static Logger log = Logger.getLogger("AutoMask");

	private boolean ap_on = false;

	public static void main(String[] args) throws Exception {
		new AutoMask().run();
	}

	public enum GROUP {
		GROUP1, GROUP2
	}

	public enum EVENT {
		AP_ON, AP_OFF, AP_TOGGLE, TRIM, MANUAL_TOGGLE,
	}

	private final static int data_id = 1;

	private void run() throws Exception {

		// connect to simconnect
		SimConnect sc = null;
		while (sc == null) {
			log.info("Trying to connect...");
			try {

				sc = new SimConnect("AutoMask");

				// to retrieve AP setting
				sc.addToDataDefinition(data_id, "AUTOPILOT MASTER", null, SimConnectDataType.INT32);

				// intercept AP on/off events
				sc.mapClientEventToSimEvent(EVENT.AP_OFF, "AUTOPILOT_OFF");
				sc.mapClientEventToSimEvent(EVENT.AP_ON, "AUTOPILOT_ON");
				sc.mapClientEventToSimEvent(EVENT.AP_TOGGLE, "AP_MASTER");
				sc.addClientEventToNotificationGroup(GROUP.GROUP1, EVENT.AP_OFF);
				sc.addClientEventToNotificationGroup(GROUP.GROUP1, EVENT.AP_ON);
				sc.addClientEventToNotificationGroup(GROUP.GROUP1, EVENT.AP_TOGGLE);
				
				// use ununsed TUG event to trigger manual on/off of trim
				// some aircraft do not set AP MASTER due to their custom AP
				sc.mapClientEventToSimEvent(EVENT.MANUAL_TOGGLE, "TUG_DISABLE");
				sc.addClientEventToNotificationGroup(GROUP.GROUP1, EVENT.MANUAL_TOGGLE);
				sc.mapInputEventToClientEvent(GROUP.GROUP2, "ctrl+z",
						EVENT.MANUAL_TOGGLE);
				sc.setInputGroupState(GROUP.GROUP2, true);
				
				// intercept TRIM events - and mask them from the sim
				sc.mapClientEventToSimEvent(EVENT.TRIM, "AXIS_ELEV_TRIM_SET");
				sc.addClientEventToNotificationGroup(GROUP.GROUP1, EVENT.TRIM, true);

				sc.setNotificationGroupPriority(0, NotificationPriority.HIGHEST_MASKABLE);

				DispatcherTask dt = new DispatcherTask(sc);
				dt.addSimObjectDataTypeHandler(new SimObjectDataTypeHandler() {
					public void handleSimObjectType(SimConnect sender, RecvSimObjectDataByType e) {

						int ap = e.getDataInt32();
						log.info("Autopilot: " + ap);

						// set AP flag based on AP on/off setting from sim
						if (ap == 0)
							ap_on = false;
						else
							ap_on = true;
					}
				});

				dt.addOpenHandler(new OpenHandler() {
					public void handleOpen(SimConnect sender, RecvOpen e) {
						log.info("Connected to " + e.getApplicationName());
						try {

							// fetch the AP setting on startup
							sender.requestDataOnSimObjectType(999, data_id, 1, SimObjectType.USER);
						} catch (IOException e1) {
							e1.printStackTrace();
						}

					}
				});
				dt.addEventHandler(new EventHandler() {
					public void handleEvent(SimConnect sender, RecvEvent e) {
						//log.info(e.getEventID() + " " + e.getData());

						// for AP events, just trigger a fetch of the current AP
						// value from the sim
						if (e.getEventID() == EVENT.AP_OFF.ordinal() || e.getEventID() == EVENT.AP_ON.ordinal()
								|| e.getEventID() == EVENT.AP_TOGGLE.ordinal()) {
							try {
								sender.requestDataOnSimObjectType(999, data_id, 1, SimObjectType.USER);
							} catch (IOException ioe) {
							}
						} else if (e.getEventID() == EVENT.TRIM.ordinal()) {

							// for TRIM events, forward the event to the SIM if
							// AP is off, otherwise, throw it away
							if (!ap_on) {
								try {
									sender.transmitClientEvent(SimConnect.OBJECT_ID_USER, e.getEventID(), e.getData(),
											e.getGroupID(), SimConnectConstants.EVENT_FLAG_DEFAULT);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
						} else if (e.getEventID() == EVENT.MANUAL_TOGGLE.ordinal()) {
							// toggle trim if the user presses ctrl-z
							ap_on = !ap_on;
							log.info("Manual Set Trim: " + !ap_on);
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

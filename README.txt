This package contains 3 tools:

LiveAtcX - a simple tool to allow live streaming of audio feeds based on the settings of the FSX radios. It can be used to tune Live ATC or any stream while flying.
ComSync - a tool to always set the com 1 frequencies to whatever is tuned to com 2. It's only use is to allow com 2 to be used to tune frequencies for RC4 - which only uses com 1. Sometimes, it's nicer to use com2 - i.e. when com1 is in the GTN750 and you would rather use a plain old radio with knobs.
AutoMask - a tool to mask the elevator trim input when auto pilot is on. Some aircraft do not mask the elevator trim when autopilot is on. If the trim is tied to a wheel or other lever, and a potentiometer sends in any signals while the autopilot is on, the aircraft will shoot upwards or downwards depending on the difference between the current auto pilot trim and the bad signal.

====

LiveAtcX is a super-primitive tool to allow live streaming of audio feeds based on the settings of the FSX radios.
It can be used to tune Live ATC or any stream while flying.

There are 2 reasons for this tool:

1. The joystick disconnect problem: Windows 8.1 users of FSX experience disconnects of USB devices when leaving the FSX window to focus on another program.
Otherwise, it would be simple to tune these streams in a browser window.
2. It is cool to have the plane's radio tuning real ATC when not using it for RC4 ATC or vatsim.

Note: I've been able to fix the joystick disconnect problem - but it's still cool to use the radios.

To run, unzip the zip into a folder and run the run.bat file.

To customize feeds, create a file called liveatcx.properties in your home folder. On windows, that is in you user folder, not your documents or desktop.

The properties file contains the FSX simconnect host and port and then a list of frequency/url pairs.

Here is the default built-in version that is used if you do not create your own file:

#host=localhost
#port=9017
132000=http://d.liveatc.net/zbw_remsen
132100=http://d.liveatc.net/kblm1
132200=http://d.liveatc.net/kpbi
132300=http://d.liveatc.net/zjx_kmyr
132400=http://d.liveatc.net/kmia_app
132500=http://d.liveatc.net/kmia_twr
132600=http://d.liveatc.net/kjfk_gnd_twr
132700=http://d.liveatc.net/kjfk_app_final

You can find your favorite streams on liveatc.net and create your own liveatcx.properties file.

LICENSES:
The jsimconnect library is LGPL. The rest of the source is unlicensed.

================================================
To build from source, you need the JDK and Maven.
From the top folder, run: mvn clean package

This creates a zip file in the target folder.



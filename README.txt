LiveAtcX is a super-primitive tool to allow live streaming of audio feeds based on the settings of the FSX radio 1.
It can be used to tune Live ATC or any stream.

There are 2 reasons for this tool:

1. The joystick disconnect problem: Windows 8.1 users of FSX experience disconnects of USB devices when leaving the FSX window to focus on another program.
Otherwise, it would be simple to tune these streams in a browser window.
2. It is cool to have the plane's radio tuning real ATC when not using it for RC4 ATC or vatsim.


================================================
To build, you need the JDK and Maven.
From the top folder, run: mvn clean package

This creates a zip file in the target folder.

To run, unzip the zip into a folder and run the run.bat file.

To customize feeds, create a file called liveatcx.properties in your home folder. On windows, that is in you user folder, not your documents or desktop.

The properties file can contain the FSX simconnect host and port and then a list of frequency/url pairs.

Here is the default built-in version that is used if you do not create your own file:

host=localhost
port=9017
132000=http://d.liveatc.net/zbw_remsen
132100=http://d.liveatc.net/kblm1
132200=http://d.liveatc.net/kpbi
132300=http://d.liveatc.net/zjx_kmyr
132400=http://d.liveatc.net/kmia_app
132500=http://d.liveatc.net/kmia_twr
132600=http://d.liveatc.net/kjfk_gnd_twr
132700=http://d.liveatc.net/kjfk_app_final
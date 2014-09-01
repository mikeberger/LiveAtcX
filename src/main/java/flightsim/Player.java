package flightsim;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/* public domain - cut&paste from stack overflow */
public class Player extends Thread {

	private String url;
	private volatile boolean running = true;

	Player(String u) {
		url = u;
	}

	public void terminate() {
		running = false;
	}

	@Override
	public void run() {
		play();
	}

	private void play() {
		try {
			AudioInputStream in = AudioSystem.getAudioInputStream(new URL(url));
			AudioInputStream din = null;
			AudioFormat baseFormat = in.getFormat();
			AudioFormat decodedFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
					baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
					false);
			din = AudioSystem.getAudioInputStream(decodedFormat, in);
			// Play now.
			rawplay(decodedFormat, din);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void rawplay(AudioFormat targetFormat, AudioInputStream din)
			throws LineUnavailableException, IOException {
		try {
			byte[] data = new byte[4096];
			SourceDataLine line = getLine(targetFormat);
			if (line != null) {
				// Start
				line.start();
				int nBytesRead = 0;
				try {
					while (running && nBytesRead != -1) {
						nBytesRead = din.read(data, 0, data.length);
						if (nBytesRead != -1)
							line.write(data, 0, nBytesRead);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Stop
				line.drain();
				line.stop();
				line.close();
				din.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static SourceDataLine getLine(AudioFormat audioFormat)
			throws LineUnavailableException {
		try {
			SourceDataLine res = null;
			DataLine.Info info = new DataLine.Info(SourceDataLine.class,
					audioFormat);
			res = (SourceDataLine) AudioSystem.getLine(info);
			res.open(audioFormat);
			return res;
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return null;
		}
	}

}

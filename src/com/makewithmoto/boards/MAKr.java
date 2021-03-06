package com.makewithmoto.boards;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.InvalidParameterException;
import java.util.Vector;

import com.makewithmoto.example.Application;

import android.app.Activity;
import android.util.Log;
import android_serialport_api.SerialPort;

public class MAKr {

	protected Application mApplication;
	protected Activity c;
	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;

	Vector<MAKrListener> listeners = new Vector<MAKr.MAKrListener>();

	private static final String TAG = "SerialReader";
	private static final String MAKR_ENABLE = "/sys/class/makr/makr/5v_enable";

	private String receivedData;
	private boolean isStarted = false;

	public interface MAKrListener {
		public void onMessageReceived(String value);
		public void onRawDataReceived(final byte[] buffer, final int size);
	}

	public MAKr(Activity c) {
		this.c = c;
		mApplication = (Application) ((Activity) c).getApplication();

		try {
			mSerialPort = getSerialPort();
			mOutputStream = mSerialPort.getOutputStream();
			mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
			mReadThread = new ReadThread();
			mReadThread.start();
		} catch (SecurityException e) {
			// DisplayError(R.string.error_security);
		} catch (IOException e) {
			// DisplayError(R.string.error_unknown);
		} catch (InvalidParameterException e) {
			// DisplayError(R.string.error_configuration);
		}

	}
	
	

	public void start() {
		initializeMAKR();
		isStarted = true;
	}

	public void stop() {
		if (mReadThread != null)
			mReadThread.interrupt();
		closeSerialPort();
		mSerialPort = null;
	}

	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				final int size;
				try {
					final byte[] buffer = new byte[64];
					if (mInputStream == null)
						return;
					size = mInputStream.read(buffer);
					if (size > 0) {

						for (final MAKrListener l : listeners) {
							l.onRawDataReceived(buffer, size);

							c.runOnUiThread(new Runnable() {
								public void run() {

									receivedData = new String(buffer, 0, size);
									
									l.onMessageReceived(receivedData);
								}
							});

						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	/*
	 * private void DisplayError(int resourceId) { AlertDialog.Builder b = new
	 * AlertDialog.Builder(c); b.setTitle("Error"); b.setMessage(resourceId);
	 * b.setPositiveButton("OK", new OnClickListener() { public void
	 * onClick(DialogInterface dialog, int which) { c.finish(); } }); b.show();
	 * }
	 */

	public boolean isEnabled() {
		boolean status = false;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(MAKR_ENABLE));
			String rv = br.readLine();
			if (rv.matches("on")) {
				return true;
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		return status;
	}

	/*
	 * Turn on or off the device
	 */
	public void enable(boolean value) {
		BufferedWriter writer = null;
		try {
			FileOutputStream fos = new FileOutputStream(MAKR_ENABLE);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			writer = new BufferedWriter(osw);
			if (value)
				writer.write("on\n");
			else
				writer.write("off\n");
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void initializeMAKR() {

		// enable the device
		if (!isEnabled()) {
			enable(true);
		}
	}

	public void writeSerial(String cmd) {

		try {
			cmd = cmd + "\n";
			mOutputStream.write(cmd.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void addListener(MAKrListener makrListener) {
		listeners.add(makrListener);
	}

	public void removeListener(MAKrListener makrListener) {
		listeners.remove(makrListener);
	}

	public void resume() {
		if (isStarted ) { 
			start();		
		}
	}
	
	public void pause() {
		if (isStarted ) { 
			stop();		
		}
	}

	
	
	
	/* Serial port stuff */ 
	public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
		if (mSerialPort == null) {
			String path = "/dev/ttyHSL2";
			int baudrate = 9600;

			/* Check parameters */
			if ( (path.length() == 0) || (baudrate == -1)) {
				throw new InvalidParameterException();
			}

			/* Open the serial port */
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
		}
		return mSerialPort;
	}

	public void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}
	
	
}

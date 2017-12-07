package cl.cromer.ubb.attendance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import cl.cromer.ubb.attendance.Progress;

/**
 * This class handles reading and writing to the file system.
 * @author Chris Cromer
 * Copyright 2013 -2016
 */
public class FileSystem {
	/**
	 * This method checks the memory card to see if it is mounted and writable.
	 * @return boolean Returns true if readable or false if it's not.
	 */
	public static boolean isWritable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	/**
	 * This method checks the memory card to see if it is readable.
	 * @return boolean Returns true if it's readable or false if it's not.
	 */
	public static boolean isReadable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
	}

	/**
	 * This method checks if a file exists on the memory card.
	 * @param context The context of the calling activity.
	 * @param fileName The name of the file that you want to see if it exists.
	 * @return boolean Returns true if it exists or false if it doesn't.
	 */
	public static boolean fileExists(Context context, String fileName) {
		File file = new File(context.getExternalFilesDir(null), fileName);
		return file.isFile();
	}

	/**
	 * This method returns the mime type of the file based off it's extension
	 * @param url The url of the file to check
	 * @return String returns the mime type or null if it is invalid
	 */
	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	/**
	 * This method attempts to open a file for viewing/editing using a 3rd party application.
	 * This method shows a toast if there isn't an application installed for the mime type.
	 * @param context The context of the activity that wants to open the file.
	 * @param fileName The name of the file you want to open.
	 * @param mimeType The mime type of the file you want to open.
	 */
	public void openFile(Context context, String fileName, String mimeType) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);

		File file = new File(context.getExternalFilesDir(null), fileName);
		intent.setDataAndType(Uri.fromFile(file), mimeType);

		final PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() == 0) {
			Toast.makeText(context, context.getString(R.string.general_no_app), Toast.LENGTH_SHORT).show();
		}
		else {
			context.startActivity(intent);
		}
	}

	public boolean renameFile(Context context, String oldFileName, String newFileName) {
		if (fileExists(context, oldFileName)) {
			File oldFile = new File(context.getExternalFilesDir(null), oldFileName);
			File newFile = new File(context.getExternalFilesDir(null), newFileName);
			return oldFile.renameTo(newFile);
		}
		return false;
	}

	/**
	 * This method deletes the file passed
	 * @param context The context of the application.
	 * @param fileName The name of the file you want to delete.
	 * @return boolean True on success or false if it fails.
	 */
	public boolean deleteFile(Context context, String fileName) {
		if (fileExists(context, fileName)) {
			File file = new File(context.getExternalFilesDir(null), fileName);
			return file.delete();
		}
		return false;
	}

	/**
	 * This method returns the size of the file.
	 * @param context The context of the application.
	 * @param fileName The name of the file you want the size of.
	 * @return long The size of the file in bytes.
	 */
	public long fileSize(Context context, String fileName) {
		if (fileExists(context, fileName)) {
			File file = new File(context.getExternalFilesDir(null), fileName);
			return file.length();
		}
		return 0;
	}

	/**
	 * This method returns the file path based on the version of android that is in use.
	 * @param context The context of the application.
	 * @param fileName The name of the file you want to get the path for.
	 * @return String This string contains the full path to the file.
	 */
	public String getFilePath(Context context, String fileName) {
		return context.getExternalFilesDir(null) + "/" + fileName;
	}

	/**
	 * This class is an AsycnTask used to download a file to the memory card.
	 * The construct requires 2 fields. Context and Filename.
	 * In the execute command; a URL is needed to download.
	 * This class will call a listener if you pass one through downloadListener.
	 * @author cromer
	 */
	public class Download extends AsyncTask<String, Integer, Boolean> {
		private Context context;
	    private PowerManager.WakeLock wakeLock;
	    private String fileName;
	    public Progress progress = null; // If this is set the progress bar will be updated
	    public ListenerDownload downloadListener = null; // This needs to be set from the parent activity

	    /**
	     * This construct is used to configure the class to run.
	     * @param context The context of the parent activity.
	     * @param fileName The name of the file you want to save to the memory card.
	     */
		public Download(Context context, String fileName) {
	        this.context = context;
	        this.fileName = fileName;
	    }

		@Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        // Let's make sure the CPU doesn't go to sleep
	        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
	        if (!wakeLock.isHeld()) {
	        	wakeLock.acquire();
	        }
	    }

		@Override
	    protected Boolean doInBackground(String... receivedUrl) {
			InputStream inStream;
			OutputStream outStream;

			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

			if (activeNetwork == null) {
				// Couldn't get a connection
				return false;
			}

			if (!activeNetwork.isConnected()) {
				// Not connected
				return false;
			}

			try {
				URL url = new URL(receivedUrl[0]);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setReadTimeout(10000);
				connection.setConnectTimeout(15000);
				connection.connect();

				// Problem with the server and file
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return false;
	            }

				int fileLength = connection.getContentLength();

				inStream = connection.getInputStream();

				File file = new File(context.getExternalFilesDir(null), fileName);
				outStream = new FileOutputStream(file);

	            byte data[] = new byte[4096];
	            long total = 0;
	            int count;
	            while ((count = inStream.read(data)) != -1) {
	                // They cancelled the download
	                if (isCancelled()) {
	                    inStream.close();
	                    outStream.close();
	                	return file.delete(); // If they cancelled the download, delete what was downloaded
	                }
	                total += count;

	                if (fileLength > 0) {
		                // Update the progress meter if we know how large the file is
	                    publishProgress((int) (total * 100 / fileLength));
	                }
	                outStream.write(data, 0, count);
	            }
				outStream.close();
				inStream.close();
	            connection.disconnect();
	            return true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return false;
		}

		@Override
	    protected void onProgressUpdate(Integer... progressUpdate) {
	        super.onProgressUpdate(progressUpdate);
	        // if we get here, length is known, now set indeterminate to false and show how long till it's finished
	        if (progress != null) {
	        	progress.setIndeterminate(false);
	        	progress.setProgress(progressUpdate[0]);
	        }
	    }

		@Override
	    protected void onPostExecute(Boolean result) {
	        wakeLock.release();
	        if (downloadListener != null) {
	        	// Call the listener if one was set
	        	downloadListener.onDownloadComplete(result);
	        }
	    }
	}
}
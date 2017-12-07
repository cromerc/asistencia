package cl.cromer.ubb.attendance;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.Surface;

/**
 * This class handles the display of the progress bar.
 * @author Chris Cromer
 * Copyright 2013 - 2016
 */
public final class Progress {

	protected ProgressDialog progressBar = null;
	public Activity activity = null;
	protected Context context = null;

	/**
	 * This method will show a progress bar on the screen.
	 * @param context The context of the parent activity.
	 * @param type If 1 show loading dialog if 2 show a downloading dialog. If not set there
	 * won't be any style or message.
	 */
	public void show(Context context, int type) {
		this.context = context;
		if (progressBar != null && progressBar.isShowing()) {
			progressBar.dismiss();
		}

		progressBar = new ProgressDialog(this.context);

		if (type == 1) {
			progressBar.setMessage(this.context.getString(R.string.general_loading));
			progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		else if (type == 2) {
			progressBar.setIndeterminate(true);
			progressBar.setMessage(this.context.getString(R.string.general_downloading));
			progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}
		progressBar.setProgress(0);
		progressBar.setMax(100);
		progressBar.show();
	}

	/**
	 * This method sets the message in the progress bar.
	 * @param message The message you want to show.
	 */
	public void setMessage(String message) {
		progressBar.setMessage(message);
	}

	/**
	 * This method sets if the progress is indeterminate or not.
	 * @param indeterminate True or false. If true we don't know the size of the file we are
	 * downloading. If false we can predict how long the download will take.
	 */
	public void setIndeterminate(boolean indeterminate) {
		progressBar.setIndeterminate(indeterminate);
	}

	/**
	 * This method updates the progress percentage.
	 * @param progress The percentage of the download that is complete out of 100%.
	 */
	public void setProgress(int progress) {
		progressBar.setProgress(progress);
	}

	/**
	 * This method sets whether or not the progress bar is cancelable or not .
	 * @param cancelable True or false.
	 */
	public void setCancelable(boolean cancelable) {
		progressBar.setCancelable(cancelable);
		if (cancelable) {
			progressBar.setCanceledOnTouchOutside(false);
		}
	}

	/**
	 * This method is used to set a listener for when a user wants to cancel a download.
	 * @param listener The listener to pass to the progress bar.
	 */
	public void setOnCancelListener(DialogInterface.OnCancelListener listener) {
		progressBar.setOnCancelListener(listener);
	}

	/**
	 * This method checks to see if the progress bar is drawn on the screen.
	 * @return boolean Returns true if showing or false if it isn't.
	 */
	public boolean isShowing() {
		return progressBar.isShowing();
	}

	/**
	 * This method erases the progress bar from the screen if it is showing.
	 */
	public void dismiss() {
		if (progressBar != null && progressBar.isShowing()) {
			progressBar.dismiss();
		}
	}

	/**
	 * This method will lock the screen rotation. Important to prevent force closes when the user
	 * changes orientation.
	 */
	public void lockRotation() {
		Display display = activity.getWindowManager().getDefaultDisplay();
	    int rotation = display.getRotation();
	    int height;
	    int width;
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
			//noinspection deprecation
			height = display.getHeight();
			//noinspection deprecation
	        width = display.getWidth();
	    }
	    else {
	        Point size = new Point();
	        display.getSize(size);
	        height = size.y;
	        width = size.x;
	    }
	    switch (rotation) {
	    case Surface.ROTATION_90:
	        if (width > height) {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        }
	        else {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
	        }
	        break;
	    case Surface.ROTATION_180:
	        if (height > width) {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
	        }
	        else {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
	        }
	        break;
	    case Surface.ROTATION_270:
	        if (width > height) {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
	        }
	        else {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        }
	        break;
	    default :
	        if (height > width) {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        }
	        else {
	            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        }
	    }
	}

	/**
	 * This method will unlock the screen rotation.
	 */
	public void unlockRotation() {
		activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
}
package cl.cromer.ubb.attendance;

/**
 * This interface is used to listen for a file download to complete.
 * It returns true on success. It returns false on failure or if the user cancels the download.
 * @author Chris Cromer
 * Copyright 2013 - 2016
 */
public interface ListenerDownload {
    void onDownloadComplete(boolean result);
}
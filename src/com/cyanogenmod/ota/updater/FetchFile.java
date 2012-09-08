package com.cyanogenmod.ota.updater;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

public class FetchFile {

    public static Context cx;
    public static final String PATH = "/CyanogenMod/ROMs/";

    public FetchFile (String... params) {
        try {
            final String ROMname = params[0];
            final String URL = params[1];
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(URL));
            request.setDescription(ROMname);
            request.setTitle("CyanogenMod OTA Updater");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }
            request.setDestinationInExternalPublicDir(PATH, ROMname + ".zip");

            DownloadManager manager = (DownloadManager) cx.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        } catch(RuntimeException e) {
            e.printStackTrace();
        }
    }
}

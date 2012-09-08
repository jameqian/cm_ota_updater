/*
 * "Credits needs to be given to the right people"
 *  Credits:
 *  JieeHD - HTTP/downloading files parts
 *  Google - for letting me use their search engine ;)
 */

package com.cyanogenmod.ota.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class OTAUpdater extends PreferenceActivity {
    private static String version = ShellCommand.execCMD("getprop ro.cm.version");
    public static String[] version_split = version.split("-");
    public static String device = android.os.Build.MODEL;
    public final static String MANIFEST_URL = "http://loota.org/~netchip/w00t-g0t-r00t.json";
    public static File externalStorage = (File) Environment.getExternalStorageDirectory();
    public static boolean externalStorageAvailable = false;
    JSONObject json;
    public static final String LOGTAG = "CM OTA Updater";
    public static Context cx;
    public static final File SDDIR = Environment.getExternalStorageDirectory();
    public static final File ROMDIR = new File(SDDIR + "/CyanogenMod/ROMs");
    boolean saveROM = new File(SDDIR + "/CyanogenMod/ROMs").mkdirs();

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.main);

        externalStorageAvailable = externalStorage.exists();

        setPreferenceSummary("device_pref", device);
        setPreferenceSummary("cm_version_pref", (version_split[0] + "-" + version_split[1]));
        setPreferenceSummary("cm_type_pref", version_split[2]);
        setPreferenceSummary("ota_support_pref", "Unknown. Press on 'Check for new version'");
        Preference checkUpdate = findPreference("ota_pref");
        checkUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                setPreferenceSummary("ota_support_pref", supported() ? "Yes, " + version_split[2] + " builds are supported" : "No, " + version_split[2] + " builds aren't supported");
                //if(!supported()) {
                //    return false;
                //}
                if(!saveROM) {
                    Log.d(LOGTAG, "Error while making directory");
                    if(!ROMDIR.exists())
                        return false;
                }
                new updateROM().execute(device);
                Log.d(LOGTAG, "updaterom executed");
                return true;
                }
            });
    }

    public static boolean supported() {
         return version_split[2].equals("NIGHTLY") || version_split[2].equals("STABLE") ? true : false;
    }

    public void writeFile() {
        try {
            FileOutputStream fos = openFileOutput("device_name", Context.MODE_PRIVATE);
            fos.write(device.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public void setPreferenceSummary(String preference, String message) {
        try {
            findPreference(preference).setSummary(message);
        } catch(RuntimeException e) {
            findPreference(preference).setSummary("");
        }
    }

    public JSONObject getVersion() throws Exception {
        HttpClient client = new DefaultHttpClient();
        StringBuilder url = new StringBuilder(MANIFEST_URL);
        HttpGet get = new HttpGet(url.toString());
        HttpResponse r = client.execute(get);
        int status = r.getStatusLine().getStatusCode();
        if(status == 200) {
            HttpEntity e = r.getEntity();
            String data = EntityUtils.toString(e);
            JSONObject stream = new JSONObject(data);
            JSONObject quote = stream.getJSONObject("cyanogenmod");
            return quote;
        } else {
            return null;
        }
    }
    public class Display {
        public String mVersion;
        public String mType_rom;
        public String mUrl;
        public String mChangelog;

        public Display(String newVersion, String type_rom, String url, String changelog) {
            mVersion = newVersion;
            mType_rom = type_rom;
            mUrl = url;
            mChangelog = changelog;
        }
    }

    public class updateROM extends AsyncTask<String, Integer, Display> {
        @Override
        protected Display doInBackground(String... params) {
            Log.d(LOGTAG, "doInBackground");
            String device = params[0];
            try {
                json = getVersion();
                String version = json.getJSONObject("device").getJSONArray(device).getJSONObject(0).getString("version");
                String changelog = json.getJSONObject("device").getJSONArray(device).getJSONObject(0).getString("changelog");
                String downurl = json.getJSONObject("device").getJSONArray(device).getJSONObject(0).getString("url");
                String build_type = json.getJSONObject("device").getJSONArray(device).getJSONObject(0).getString("build");
                Log.d(LOGTAG, version + changelog + downurl + build_type);
                return new Display(version, build_type, downurl, changelog);
            } catch (JSONException e) {
                e.printStackTrace();
                setPreferenceSummary("ota_support_pref", "No OTA support available for this device.");
            } catch (Exception e) {
                e.printStackTrace();
                setPreferenceSummary("ota_support_pref", "No OTA support available for this device.");
            }
            return new Display("", "", "", "");
        }

        @SuppressWarnings("deprecation")
        public void onPostExecute(final Display result) {
            Log.d(LOGTAG, "onPostExecute");
            final String VERSION = result.mVersion;
            final String TYPE_ROM = result.mType_rom;
            final String URL = result.mUrl;
            final String CHANGELOG = result.mChangelog;

            if(VERSION.equals(OTAUpdater.version)) {
                Toast toast = Toast.makeText(getApplicationContext(), "No new version available!", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                //Toast toast = Toast.makeText(getApplicationContext(), "New version is available!", Toast.LENGTH_SHORT);
                //toast.show();
                Log.d(LOGTAG, "Current version doesn't equals new version");
                final AlertDialog alertdialog = new AlertDialog.Builder(OTAUpdater.this).create();
                alertdialog.setTitle("New update available!");
                alertdialog.setMessage("CM version: " + VERSION + "\n" + "Changelog: " + CHANGELOG);
                alertdialog.setButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new FetchFile(VERSION, URL);
                    }
                });
                alertdialog.setButton2("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertdialog.dismiss();
                    }
                });
                alertdialog.show();
            }
        }
    }
}

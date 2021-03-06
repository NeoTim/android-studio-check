// ---------------------------------------------------------------------
// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ---------------------------------------------------------------------

package com.example.androidstudiocheck;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;


public class MainActivity extends Activity {

    public EditText emailText;
    public TextView registrationText;
    public boolean foundWearDevice;
    public boolean foundWearCompanion;
    public boolean foundAndroidEmulator;

    class DetectWearTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... unused) {
            foundAndroidEmulator = Build.HARDWARE.contains("goldfish");
            if (foundAndroidEmulator) {
                Log.d("AndroidStudioCheck", "Discovered that device is an emulator");
            } else {
                Log.d("AndroidStudioCheck", "Discovered that device is a real phone/tablet");
            }

            try {
                getPackageManager().getPackageInfo("com.google.android.wearable.app", PackageManager.GET_META_DATA);
                Log.d("AndroidStudioCheck", "Detected Android Wear companion app");
                foundWearCompanion = true;
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("AndroidStudioCheck", "Failed to detect Android Wear companion app");
                foundWearCompanion = false;
            }

            GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                    .addApi(Wearable.API)
                    .build();
            ConnectionResult result = mGoogleApiClient.blockingConnect();
            if (!result.isSuccess()) {
                Log.d("AndroidStudioCheck", "Failed to connect to Play Services for Wearable.API");
                foundWearDevice = false;
            } else {
                List<Node> connectedNodes =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
                if (connectedNodes.size() >= 1) {
                    Log.d("AndroidStudioCheck", "Found " + connectedNodes.size() + " wearablew nodes");
                    foundWearDevice = true;
                } else {
                    Log.d("AndroidStudioCheck", "Did not find any wearable nodes");
                    foundWearDevice = false;
                }
            }
            // Do not update the UI here, it must be done in the UI thread from onPostExecute()
            Log.d("AndroidStudioCheck", "DetectWearTask completed");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            refreshOutput();
        }
    }

    public String getAttributeString() {
        String out = "";
        if (foundWearDevice)
            return getString(R.string.found_wearable);
        else if (foundWearCompanion)
            return getString(R.string.found_companion);
        else if (foundAndroidEmulator)
            return getString(R.string.found_emulator);
        else
            return getString(R.string.found_android);
    }

    public void refreshOutput() {
        String out = getAttributeString();
        Log.d("AndroidStudioCheck", "Refreshing with registration code [" + out + "]");
        registrationText.setText(out);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle to update the registration code as the user enters their email address
        registrationText = (TextView)findViewById(R.id.registrationText);
        registrationText.setText("");

        // Schedule a background check to see if we have a wear device
        new DetectWearTask().execute();
    }
}

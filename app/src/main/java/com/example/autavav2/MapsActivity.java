package com.example.autavav2;

import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.autavav2.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private SearchView searchView;
    private Button buttonGenRoute;
    private FloatingActionButton buttonStartRoute, buttonConfig, buttonAddDestination;
    private LinearLayout dataBox;
    private LinearLayout dataBox2;
    private TextView textDataDistance, textDataTime, textDataSpeed;
    private TextView textDataDistance2, textDataTime2, textDataSpeed2;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng origin, dest;
    private Localizacao localizacao;
    private ColorStateList colorRed = new ColorStateList( new int[][]{ new int[]{} }, new int[]{ Color.RED } );
    private ColorStateList colorGreen = new ColorStateList( new int[][]{ new int[]{} }, new int[]{ Color.GREEN } );
    private DownloadTask downloadTask;
    private String url;
    private ArrayList<LatLng> points = null;
    private PolylineOptions lineOptions = null;
    private Polyline route;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchView = findViewById(R.id.idSearchView);
        searchView.clearFocus();
        buttonGenRoute = findViewById(R.id.buttonGenRoute);
        buttonStartRoute = findViewById(R.id.buttonStartRoute);
        buttonAddDestination = findViewById(R.id.buttonAddDestination);
        buttonConfig = findViewById(R.id.buttonConfig);
        dataBox = findViewById(R.id.data_box);
        dataBox2 = findViewById(R.id.data_box2);
        textDataDistance = findViewById(R.id.textDataDistance);
        textDataTime = findViewById(R.id.textDataTime);
        textDataSpeed = findViewById(R.id.textDataSpeed);
        textDataDistance2 = findViewById(R.id.textDataDistance2);
        textDataTime2 = findViewById(R.id.textDataTime2);
        textDataSpeed2 = findViewById(R.id.textDataSpeed2);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        buttonGenRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                url = getUrl(origin, dest, "driving");

                handler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        // Handle updates from the thread here
                        String updateMessage = (String) msg.obj;
                        if (updateMessage.equals("att")){
                            textDataDistance2.setText(localizacao.printRemainingDistance());
                            textDataTime2.setText(localizacao.printRemainingTime());
                            textDataSpeed2.setText(localizacao.printAverageSpeed(true));
                        } else if (updateMessage.equals("getDistance")) {
                            new UpdateDistance().execute();
                        }
                    }
                };
                localizacao = new Localizacao(fusedLocationClient, mapFragment.getActivity(), handler);

                downloadTask = new DownloadTask();

                downloadTask.execute(url);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
                buttonGenRoute.setVisibility(View.INVISIBLE);
                buttonStartRoute.setVisibility(View.VISIBLE);
                dataBox.setVisibility(View.VISIBLE);
            }
        });

        buttonStartRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonStartRoute.getBackgroundTintList() == colorRed){
                    Snackbar.make(v, "Rota cancelada!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    localizacao.interrupt();
                    buttonStartRoute.setVisibility(View.INVISIBLE);
                    dataBox.setVisibility(View.INVISIBLE);
                    dataBox2.setVisibility(View.INVISIBLE);
                    textDataDistance.setText("Dist:");
                    textDataTime.setText("Tempo:");
                    buttonStartRoute.setBackgroundTintList(colorGreen);
                    buttonStartRoute.setImageResource(android.R.drawable.ic_media_play);
                    mMap.clear();
                } else {
                    //Snackbar.make(v, "Rota iniciada!", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    localizacao.start();
                    buttonStartRoute.setBackgroundTintList(colorRed);
                    buttonStartRoute.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    textDataDistance2.setText(localizacao.printRemainingDistance());
                    textDataTime2.setText(localizacao.printRemainingTime());
                    textDataSpeed2.setText(localizacao.printAverageSpeed(true));
                    dataBox2.setVisibility(View.VISIBLE);
                }
            }
        });

        buttonAddDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                String location = searchView.getQuery().toString();

                List<Address> addressList = null;

                if (location != null || location.equals("")) {

                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    assert addressList != null;
                    Address address = addressList.get(0);

                    dest = new LatLng(address.getLatitude(), address.getLongitude());

                    mMap.addMarker(new MarkerOptions().position(dest).title(location));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 17));
                    buttonStartRoute.setVisibility(View.INVISIBLE);
                    dataBox.setVisibility(View.INVISIBLE);
                    dataBox2.setVisibility(View.INVISIBLE);
                    textDataDistance.setText("Dist:");
                    textDataTime.setText("Tempo:");
                    buttonGenRoute.setVisibility(View.VISIBLE);
                    //buttonAddDestination.setVisibility(View.VISIBLE);

                }
                return false;
            }


            @Override
            public boolean onQueryTextChange(String newText) {
                mMap.clear();
                buttonGenRoute.setVisibility(View.INVISIBLE);
                buttonStartRoute.setVisibility(View.INVISIBLE);
                dataBox.setVisibility(View.INVISIBLE);
                dataBox2.setVisibility(View.INVISIBLE);
                textDataDistance.setText("Dist:");
                textDataTime.setText("Tempo:");
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
                return false;
            }
        });

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMinZoomPreference(0.0f);
        mMap.setMaxZoomPreference(20.0f);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            origin = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 17));
                        }
                    }
                });

        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);

                localizacao.setTotalDistance(parser.getDistance());
                localizacao.setTotalTime(parser.getDuration());
                localizacao.calculateAverageSpeed();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                textDataDistance.setText(localizacao.printTotalDistance());
                textDataTime.setText(localizacao.printTotalTime());
                textDataSpeed.setText(localizacao.printAverageSpeed(false));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }
            // Drawing polyline in the Google Map for the i-th route
            route = mMap.addPolyline(lineOptions);
        }
    }

    private class UpdateDistance extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String url = getUrl(new LatLng(localizacao.getLatitude(),localizacao.getLongitude()), dest, "driving");
            String urlJson = null;
            JSONObject jObject = null;
            try {
                urlJson = downloadUrl(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                jObject = new JSONObject(urlJson);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            DirectionsJSONParser parser = new DirectionsJSONParser();
            parser.parse(jObject);
            Log.i("teste","" + parser.getDistance());
            localizacao.setRemainingDistance(parser.getDistance());
            localizacao.liberaProssiga();
            return null;
        }
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}


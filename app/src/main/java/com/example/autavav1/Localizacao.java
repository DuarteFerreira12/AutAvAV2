package com.example.autavav1;


import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;

import java.util.ArrayList;

public class Localizacao extends Thread {

    private ArrayList<Data> dataSet = new ArrayList<>();
    private FusedLocationProviderClient client;
    private LocationRequest locationRequest;
    private Context mapActivity;

    public Localizacao(FusedLocationProviderClient client, Context mapActivity) {
        this.client = client;
        this.mapActivity = mapActivity;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Log.i("Teste", "rodei");
            final double[] latitude = {0};
            final double[] longitude = {0};
            final double[] speed = {0};
            double travelTime = 0;
            double travelDistance = 0;
            double fuelConsumption = 0;
            double timestamp = 0;

            if (ActivityCompat.checkSelfPermission(mapActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mapActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            latitude[0] = location.getLatitude();
                            longitude[0] = location.getLongitude();
                            speed[0] = location.getSpeed();
                        }
                    });

            Data d = new Data(latitude[0], longitude[0], speed[0], travelTime, travelDistance, fuelConsumption, timestamp);

            dataSet.add(d);


            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

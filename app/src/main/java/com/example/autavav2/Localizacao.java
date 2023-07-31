package com.example.autavav2;


import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Localizacao extends Thread {

    private final ArrayList<Data> dataSet = new ArrayList<>();
    private final FusedLocationProviderClient client;
    private final Context mapActivity;
    private double totalDistance;
    private double remainingDistance;
    private double totalTime;
    private double remainingTime;
    private double averageSpeed;
    private double newAverageSpeed;
    private double latitude = 0;
    private double longitude = 0;
    private double speed = 0;
    private double travelTime = 0;
    private double travelDistance = 0;
    private double fuelConsumption = 0;
    private double timestamp = 0;
    private DecimalFormat numberFormat;
    private int nMedidas;
    private int interval = 10;
    private boolean prossiga = false;
    private Handler handler;

    public Localizacao(FusedLocationProviderClient client, Context mapActivity, Handler handler) {
        this.client = client;
        this.mapActivity = mapActivity;
        this.numberFormat = new DecimalFormat("#.00");
        this.handler = handler;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
        this.remainingDistance = totalDistance;
    }

    public void setTotalTime(int totalTime) {
        this.nMedidas = totalTime/interval;
        this.totalTime = nMedidas * interval;
        this.remainingTime = this.totalTime;
    }
    public void calculateAverageSpeed(){
        this.averageSpeed = Double.parseDouble(numberFormat.format((totalDistance/totalTime)*3.6));
        this.newAverageSpeed = averageSpeed;
    }

    public void setRemainingDistance(double remainingDistance) {
        this.remainingDistance = remainingDistance;
    }

    public double getTotalDistance() {
        return totalDistance;
    }
    public String printTotalDistance() {
        if (totalDistance < 1000) {
            return "Dist: " + numberFormat.format(totalDistance) + "m";
        } else {
            return "Dist: " + numberFormat.format(totalDistance/1000) + "Km";
        }
    }
    public String printRemainingDistance() {
        if (remainingDistance < 0) {
            return "Dist: " + 0.0 + "m";
        } else if (remainingDistance < 1000) {
            return "Dist: " + numberFormat.format(remainingDistance) + "m";
        } else {
            return "Dist: " + numberFormat.format(remainingDistance/1000) + "Km";
        }
    }

    public double getTotalTime() {
        return totalTime;
    }
    public String printTotalTime() {
        if (totalTime < 60){
            return "Tempo: " + totalTime + "s";
        } else if (totalTime < 3600) {
            return "Tempo: " + numberFormat.format(totalTime/60)  + "min";
        } else {
            return "Tempo: " + numberFormat.format(totalTime/3600) + "h";
        }
    }
    public String printRemainingTime() {
        if (remainingTime < 60){
            return "Tempo: " + remainingTime + "s";
        } else if (remainingTime < 3600) {
            return "Tempo: " + numberFormat.format(remainingTime/60)  + "min";
        } else {
            return "Tempo: " + numberFormat.format(remainingTime/3600) + "h";
        }
    }

    public String printAverageSpeed(boolean newSpeed) {
        return newSpeed ? "Vel. alvo:" + newAverageSpeed  + "Km/h" : "Vel. média:" + averageSpeed  + "Km/h" ;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void liberaProssiga(){
        prossiga = true;
    }
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000*interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (ActivityCompat.checkSelfPermission(mapActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mapActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            travelTime += interval;
            remainingTime -= interval;

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            speed = location.getSpeed();

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Send updates back to the main activity here
                                    String updateMessage = "getDistance";
                                    // Send the update message back to the main activity
                                    Message message = handler.obtainMessage(0, updateMessage);
                                    message.sendToTarget();
                                }
                            });
                        }
                    });
            while(!prossiga){};
            prossiga = false;
            travelDistance = totalDistance - remainingDistance;
            Log.i("teste","calculou");
            if (travelDistance == 0) travelDistance = Math.pow(10, -10);

            double[] y = new double[nMedidas+1]; //Vetor de medidas
            y[0] = travelDistance;
            y[nMedidas] = totalDistance;
            for (int i = 1; i < nMedidas; i++) {
                y[i] = remainingDistance/nMedidas;
            }

            double[] v = new double[nMedidas+1]; //Vetor de variancias
            v[0] = Math.pow(10, -10);
            v[nMedidas] = Math.pow(10, -10);
            for (int i = 1; i < nMedidas; i++) {
                v[i] = 1;
            }

            double[] A = new double[nMedidas+1]; //Vetor de restrições
            A[nMedidas] = 1;
            for (int i = 0; i < nMedidas; i++) {
                A[i] = -1;
            }

            Reconciliacao rec = new Reconciliacao(y, v, A);

            this.newAverageSpeed = Double.parseDouble(numberFormat.format((rec.getReconciledFlow()[1]/interval)*3.6));

            nMedidas--;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    // Send updates back to the main activity here
                    String updateMessage = "att";
                    // Send the update message back to the main activity
                    Message message = handler.obtainMessage(0, updateMessage);
                    message.sendToTarget();
                }
            });


            if(nMedidas == 0){
                return;
            }

            Data d = new Data(latitude, longitude, speed, travelTime, travelDistance, fuelConsumption, timestamp);
            dataSet.add(d);

        }
    }
}

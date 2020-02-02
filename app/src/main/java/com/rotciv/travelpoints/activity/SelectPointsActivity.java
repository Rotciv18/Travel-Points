package com.rotciv.travelpoints.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import com.rotciv.travelpoints.R;

import java.util.ArrayList;
import java.util.List;

public class SelectPointsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean isMyPlace = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private List<LatLng> locations = new ArrayList<>();
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_points);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        requestStartingPoint();
    }

    public void requestStartingPoint() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.request_title);
        builder.setMessage(R.string.request_message);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.request_my_place, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getMyPlace();
                isMyPlace = true;
            }
        }).setNegativeButton(R.string.request_set_a_location, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Definir um local de partida
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void getMyPlace() {
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location == null) {
                    // :(
                } else {
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    locations.add(myLocation);

                    addMapMarker(myLocation, "Meu Local");
                }
            }
        });
    }

    public void addMapMarker(LatLng location, String title) {
        //Adds a marker to location
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(title)
        );

        //Zooms to location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 20));
    }

}

package com.rotciv.travelpoints.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.rotciv.travelpoints.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectPointsActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*
    * Components
    */
    private GoogleMap mMap;
    private boolean isMyPlace = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private List<LatLng> locations = new ArrayList<>();
    FusedLocationProviderClient mFusedLocationClient;
    private EditText editnewLocation;
    private ProgressBar progressLoading;

    /*
    *
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_points);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editnewLocation = findViewById(R.id.editNewLocation);
        progressLoading = findViewById(R.id.progressLoading);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setEditTextOnTouchListener();
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
                requestStartingLocation();
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

        Log.d("Mizera", locations.toString());
    }

    public void addLocationWithAddress (String address) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(address, 1);
            if (listaEnderecos != null && listaEnderecos.size() > 0) {
                Address geocodedAddress = listaEnderecos.get(0);

                LatLng location = new LatLng(geocodedAddress.getLatitude(), geocodedAddress.getLongitude());
                locations.add(location);
                Log.d("Mizera", geocodedAddress.toString());
                addMapMarker(location, geocodedAddress.getSubThoroughfare() + ", " + geocodedAddress.getFeatureName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestStartingLocation() {
        final EditText editAddress = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.insert_place_address);
        builder.setMessage(R.string.insert_place_address_message);
        builder.setView(editAddress);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String address = editAddress.getText().toString();

                if (address.isEmpty()) {
                    Toast.makeText(SelectPointsActivity.this, R.string.empty_field, Toast.LENGTH_SHORT).show();
                    requestStartingLocation();
                } else {
                    addLocationWithAddress(address);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setEditTextOnTouchListener() {
        editnewLocation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (editnewLocation.getRight() - editnewLocation.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                        String address = editnewLocation.getText().toString();
                        addLocationWithAddress(address);
                        editnewLocation.setText("");

                        closeKeyboardWindow();

                        return true;
                    }
                }
                return false;
            }
        });
    }

    public void closeKeyboardWindow() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

}

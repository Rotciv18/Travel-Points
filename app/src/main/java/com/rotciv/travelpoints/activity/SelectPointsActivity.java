package com.rotciv.travelpoints.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.StrictMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.rotciv.travelpoints.R;
import com.rotciv.travelpoints.helper.Graphs;
import com.rotciv.travelpoints.helper.Maps;
import com.rotciv.travelpoints.service.DownloadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class SelectPointsActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*
    * Components
    */
    private GoogleMap mMap;
    private boolean isMyPlace = false;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private List<Location> locations = new ArrayList<>();
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

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRouteToLocations();
            }
        });

        editnewLocation = findViewById(R.id.editNewLocation);
        progressLoading = findViewById(R.id.progressLoading);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setEditTextOnTouchListener();
    }
    
    public void showRouteToLocations() {
        if (locations.size() < 0) {
            Toast.makeText(this, R.string.few_locations, Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.show_route);
            builder.setMessage(R.string.show_route_message);

            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    double[][] adjacentMatrix = Graphs.mountAdjacencyMatrix(locations);
                    int[] way = Graphs.nearestNeightbor(adjacentMatrix);

                    LatLng origin = new LatLng(locations.get(0).getLatitude(), locations.get(0).getLongitude());
                    LatLng dest = new LatLng(locations.get(1).getLatitude(), locations.get(1).getLongitude());
                    ArrayList<LatLng> markerPoints = new ArrayList<>();
                    markerPoints.add(origin);
                    markerPoints.add(dest);

                    // Getting URL to the Google Directions API
                    List<String> urls = Maps.getDirectionsUrl(markerPoints);
                    String url = urls.get(0);

                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                    DownloadTask downloadTask = new DownloadTask();

                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);

                }
            }).setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // does nothing
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                Location location = new Location("");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);

                addLocationWithMapClick(location);

            }
        });

        requestStartingPoint();


    }

    public void addLocationWithMapClick(final Location location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_locations);
        builder.setMessage(R.string.add_locations_message);

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                locations.add(location);

                addMapMarker(location, "Adicionado ao Clique");

            }
        }).setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // does nothing
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
                    locations.add(location);

                    addMapMarker(location, "Meu Local");
                }
            }
        });
    }

    public void addMapMarker(Location location, String title) {
        //Adds a marker to location
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
        );

        //Zooms to location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        Log.d("Mizera", locations.toString());
    }

    public void addLocationWithAddress (String address) {

        if (!address.isEmpty()) {

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> listaEnderecos = geocoder.getFromLocationName(address, 1);
                if (listaEnderecos != null && listaEnderecos.size() > 0) {
                    Address geocodedAddress = listaEnderecos.get(0);

                    Location location = new Location("");
                    location.setLatitude(geocodedAddress.getLatitude());
                    location.setLongitude(geocodedAddress.getLongitude());
                    locations.add(location);
                    Log.d("Mizera", geocodedAddress.toString());
                    addMapMarker(location, geocodedAddress.getSubThoroughfare() + ", " + geocodedAddress.getFeatureName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, R.string.empty_field, Toast.LENGTH_SHORT).show();
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
                        try {
                            addLocationWithAddress(address);
                            editnewLocation.setText("");

                            closeKeyboardWindow();

                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

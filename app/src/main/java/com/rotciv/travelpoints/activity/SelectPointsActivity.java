package com.rotciv.travelpoints.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.logicbeanzs.uberpolylineanimation.MapAnimator;
import com.rotciv.travelpoints.R;
import com.rotciv.travelpoints.directionhelpers.FetchURL;
import com.rotciv.travelpoints.directionhelpers.TaskLoadedCallback;
import com.rotciv.travelpoints.helper.Graphs;
import com.rotciv.travelpoints.helper.VND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.graphics.Bitmap.Config.ARGB_8888;

public class SelectPointsActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {

    /*
    * Components
    */
    private static GoogleMap mMap;
    private List<Location> locations = new ArrayList<>();
    FusedLocationProviderClient mFusedLocationClient;
    private EditText editnewLocation;
    private ProgressBar progressLoading;
    private Integer markerCounter = 0;

    private Polyline currentPolyline;

    /*
    *
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_points);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.select_points_activity_title);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public void clearMap () {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.request_clear_map);
        builder.setMessage(R.string.request_clear_map_message);

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mMap.clear();
                locations.clear();
                markerCounter = 0;
                requestStartingPoint();
            }
        }).setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // does nothing.
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.itemClear:
                clearMap();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getUrl(List<Location> newLocations, String directionMode ) {
        int destIndex = newLocations.size() - 1;

        // Origin of route
        String str_origin = "origin=" + newLocations.get(0).getLatitude() + "," + newLocations.get(0).getLongitude();
        // Destination of route
        String str_dest = "destination=" + newLocations.get(destIndex).getLatitude() + "," + newLocations.get(destIndex).getLongitude();

        //Waypoints
        String waypoints = "waypoints=";
        for (int i = 1; i < destIndex; i++) {
            Location location = newLocations.get(i);
            waypoints += location.getLatitude() + "," + location.getLongitude();

            if (i+1 < destIndex) {
                waypoints += "|";
            }
        }

        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + waypoints;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        Log.d("DebugURL", url);
        return url;
    }
    
    public void showRouteToLocations() {
        final CheckBox returnToOriginCheckBox = new CheckBox(SelectPointsActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        returnToOriginCheckBox.setLayoutParams(lp);
        returnToOriginCheckBox.setText(R.string.return_to_place);

        if (locations.size() < 5) {
            Toast.makeText(this, R.string.few_locations, Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.show_route);
            builder.setMessage(R.string.show_route_message);
            builder.setView(returnToOriginCheckBox);

            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    double[][] adjacentMatrix = Graphs.mountAdjacencyMatrix(locations);
                    int[] way = Graphs.nearestNeightbor(adjacentMatrix, returnToOriginCheckBox.isChecked());

                    double solution = 0;
                    for (int i = 0; i < way.length - 1; i++) {
                        solution += adjacentMatrix[way[i]][way[i+1]];
                    }

                    VND bestNeighbor = new VND(way, adjacentMatrix, solution, returnToOriginCheckBox.isChecked());

                    List<Location> newLocations = new ArrayList<>();

                    int[] bestNeighborWay = bestNeighbor.getWay();
                    int size = bestNeighborWay.length;
                    for (int i = 0; i < size; i++) {

                        newLocations.add(locations.get(bestNeighborWay[i]));

                    }

                    new FetchURL(SelectPointsActivity.this).execute(getUrl(newLocations,
                            "driving"), "driving");

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

        String text = markerCounter.toString();

        Bitmap bmp = makeBitmap(this, text);

        //Adds a marker to location
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp))

        );

        //Zooms to location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

        markerCounter++;
    }

    public Bitmap makeBitmap(Context context, String text)
    {
        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.marker);
        bitmap = bitmap.copy(ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED); // Text color
        paint.setTextSize(16 * scale); // Text size
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE); // Text shadow
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        int x = bitmap.getWidth() - bounds.width() - 10; // 10 for padding from right
        int y = bounds.height();
        canvas.drawText(text, x, y, paint);

        return  bitmap;
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

    @Override
    public void onTaskDone(Object... values) {

        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        PolylineOptions options = (PolylineOptions) values[0];
        options.width(5).color(Color.RED).geodesic(false);
        // currentPolyline = mMap.addPolyline(options);
        MapAnimator.getInstance().animateRoute(mMap, options.getPoints());
    }

}

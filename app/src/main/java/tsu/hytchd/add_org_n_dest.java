package tsu.hytchd;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tsu.hytchd.R.id.map;
import static tsu.hytchd.R.layout.activity_add_org_n_dest;

public class add_org_n_dest extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;
    private Location location = new Location("service Provider");
    MarkerOptions mp = new MarkerOptions();
    private GoogleMap mMap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_add_org_n_dest);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mapFragment.getMapAsync(this);

        mp.position(new LatLng(location.getLatitude(), location.getLongitude()));

        final EditText locationSearch = (EditText) findViewById(R.id.destination_input);
        final Button addRoute = (Button) findViewById(R.id.add_route_button);
        final Button confirmRoute = (Button) findViewById(R.id.confirm_button);
        final Button editRoute = (Button) findViewById(R.id.edit_button);
        final Button saveRoute = (Button) findViewById(R.id.save_button);
        //button UI
        confirmRoute.setVisibility(View.INVISIBLE);
        editRoute.setVisibility(View.INVISIBLE);
        saveRoute.setVisibility(View.INVISIBLE);

        addRoute.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideVirtualKeyboard();
                        String loc = locationSearch.getText().toString();
                        if (loc != null && !loc.isEmpty()) {
                            LatLng ll = onMapSearch(loc);
                            String url = makeURL(location.getLatitude(), location.getLongitude(), ll.latitude, ll.longitude);
                            drawPath(url);

                            locationSearch.setFocusable(false);
                            confirmRoute.setVisibility(View.VISIBLE);
                            editRoute.setVisibility(View.VISIBLE);
                            addRoute.setVisibility(View.INVISIBLE);
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(add_org_n_dest.this).create();
                            alertDialog.setTitle("Alert");
                            alertDialog.setMessage("Please enter your desired destination.");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                        }
                    }
                }
        );

        editRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVirtualKeyboard();
                locationSearch.setFocusableInTouchMode(true);
                editRoute.setVisibility(View.INVISIBLE);
                confirmRoute.setVisibility(View.INVISIBLE);
                saveRoute.setVisibility(View.VISIBLE);
            }
        });

        saveRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideVirtualKeyboard();
                locationSearch.setFocusableInTouchMode(false);
                saveRoute.setVisibility(View.INVISIBLE);
                editRoute.setVisibility(View.VISIBLE);
                confirmRoute.setVisibility(View.VISIBLE);
                String location = locationSearch.getText().toString();
                if (location != null && !location.isEmpty()) {
                    onMapSearch(location);
                    locationSearch.setFocusable(false);
                    confirmRoute.setVisibility(View.VISIBLE);
                    editRoute.setVisibility(View.VISIBLE);
                    addRoute.setVisibility(View.INVISIBLE);
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(add_org_n_dest.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Please enter your desired destination.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });

        confirmRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent("tsu.hytchd.activity_selection_menu");
                startActivity(i);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null  && isLocationServiceEnabled()) {
            mMap.setMyLocationEnabled(true);
            mMap.clear();
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        mp.position(new LatLng(location.getLatitude(), location.getLongitude()));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    public void hideVirtualKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void showVirtualKeyboard() {
        EditText locationSearch = (EditText) findViewById(R.id.destination_input);
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(locationSearch, InputMethodManager.SHOW_IMPLICIT);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    public boolean isLocationServiceEnabled() {
        LocationManager lm = (LocationManager)
                this.getSystemService(Context.LOCATION_SERVICE);
        String provider = lm.getBestProvider(new Criteria(), true);
        return (StringUtils.isNotBlank(provider) &&
                !LocationManager.PASSIVE_PROVIDER.equals(provider));
    }
    public LatLng onMapSearch(String location) {
        mMap.clear();
        List<Address>addressList = null;
        Geocoder geocoder = new Geocoder(this);
        try {
            addressList = geocoder.getFromLocationName(location, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address = addressList.get(0);
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        return latLng;
    }
    public void drawPath(String result){
        try {
            final JSONObject jsonObject = new JSONObject(result);

            JSONArray routeArray = jsonObject.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);


            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");

            String statusString = jsonObject.getString("status");

            Log.d("test: ", encodedString);
            List<LatLng> list = decodePoly(encodedString);

            LatLng last = null;
            for (int i = 0; i < list.size()-1; i++) {
                LatLng src = list.get(i);
                LatLng dest = list.get(i+1);
                last = dest;
                Log.d("Last latLng:", last.latitude + ", " + last.longitude );
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude, dest.longitude))
                        .width(4)
                        .color(Color.GREEN));
            }

            Log.d("Last latLng:", last.latitude + ", " + last.longitude );
        }catch (JSONException e){
            e.printStackTrace();
        }
        catch(ArrayIndexOutOfBoundsException e) {
            System.err.println("Caught ArrayIndexOutOfBoundsException: "+ e.getMessage());
        }
    }

    private List<LatLng> decodePoly(String encoded){


        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0;
        int length = encoded.length();

        int latitude = 0;
        int longitude = 0;

        while(index < length){
            int b;
            int shift = 0;
            int result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int destLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            latitude += destLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b > 0x20);

            int destLong = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            longitude += destLong;

            poly.add(new LatLng((latitude / 1E5),(longitude / 1E5) ));
        }
        return poly;
    }
    public String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");//
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString( sourcelog));
        urlString.append("&destination=");
        urlString .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=AIzaSyDb498hSFJucJIlAs9SsgMbV1G4eEvHKAM");
        return urlString.toString(); }

}
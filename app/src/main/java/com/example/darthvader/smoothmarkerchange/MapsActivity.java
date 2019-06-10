package com.example.darthvader.smoothmarkerchange;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager manager;
    List<LatLng> latLngs = new ArrayList<>();
    float v;
    double lat, lng;
    Handler handler;
    LatLng start, end;
    int index, next;
    private Marker mMarker;
    PolylineOptions polylineOptions, blackpolyLineOptions;
    Polyline blackPolyline, greyPolyline;
    String destination = "Ancia+Beach";
    IGoogleAPI mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        mService = Common.getGoogleAPI();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final LatLng sydney = new LatLng(11.2557365, 75.7725404);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney,13));


        String requestURL;
        try {
            requestURL = "https://maps.googleapis.com/maps/api/directions/json?mode=driving&" +
                    "transit_routing_preference=less_driving&"
                    + "origin=" + sydney.latitude + "," + sydney.longitude + "&" +
                    "destination=" + destination + "&" +
                    "key="+getResources().getString(R.string.google_maps_key);
            mService.getDataFromGoogleApi(requestURL)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response.body());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    latLngs = decodePoly(polyline);

                                    LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                                    for (LatLng latLng : latLngs)
                                        bounds.include(latLng);
                                    LatLngBounds bounds1 = bounds.build();
                                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds1, 2);
                                    mMap.moveCamera(cameraUpdate);

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);

                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.endCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(latLngs);
                                    greyPolyline = mMap.addPolyline(polylineOptions);

                                    blackpolyLineOptions = new PolylineOptions();
                                    blackpolyLineOptions.color(Color.BLACK);

                                    blackpolyLineOptions.startCap(new SquareCap());
                                    blackpolyLineOptions.endCap(new SquareCap());
                                    blackpolyLineOptions.jointType(JointType.ROUND);
                                    blackpolyLineOptions.addAll(latLngs);
                                    blackPolyline = mMap.addPolyline(blackpolyLineOptions);

                                    mMap.addMarker(new MarkerOptions().position(latLngs.get(latLngs.size() - 1)));

                                    ValueAnimator polylineAnimator = ValueAnimator.ofInt(0, 100);
                                    polylineAnimator.setDuration(2000);
                                    polylineAnimator.setInterpolator(new LinearInterpolator());
                                    polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            List<LatLng> points = greyPolyline.getPoints();
                                            int percentValue = (int) animation.getAnimatedValue();
                                            int size = points.size();
                                            int newPoints = (int) (size * (percentValue / 100f));
                                            List<LatLng> p = points.subList(0, newPoints);
                                            blackPolyline.setPoints(p);
                                        }
                                    });
                                    polylineAnimator.start();

                                    mMarker = mMap.addMarker(new MarkerOptions().position(sydney)
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    handler = new Handler();
                                    index = -1;
                                    next = 1;
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (index < latLngs.size() - 1) {
                                                index++;
                                                next = index + 1;
                                            }
                                            if (index < latLngs.size() - 1) {
                                                start = latLngs.get(index);
                                                end = latLngs.get(next);
                                            }
                                            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                                            valueAnimator.setDuration(3000);
                                            valueAnimator.setInterpolator(new LinearInterpolator());
                                            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                                @Override
                                                public void onAnimationUpdate(ValueAnimator animation) {
                                                    v = valueAnimator.getAnimatedFraction();
                                                    lng = v * end.longitude + (1 - v) * start.longitude;
                                                    lat = v * end.latitude + (1 - v) * start.latitude;

                                                    LatLng newPos = new LatLng(lat, lng);
                                                    mMarker.setPosition(newPos);
                                                    mMarker.setAnchor(0.5f, 0.5f);
                                                    mMarker.setRotation(getBearing(start, newPos));
                                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,13));
                                                }
                                            });
                                            valueAnimator.start();
                                            handler.postDelayed(this, 3000);
                                        }
                                    }, 3000);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                            Log.e("ERORRRRRR", t.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e("EROR", e.getMessage());

        }
    }

    public static List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permissions Denied.Please provide permissions in settings", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }


}

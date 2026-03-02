package com.example.travelbuddy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final double TUNISIA_LAT = 34.0;
    private static final double TUNISIA_LON = 9.0;
    private static final double DEFAULT_ZOOM = 5.0;

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private final List<Tip> tipsToGeocode = new ArrayList<>();
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration OSMDroid requise (gratuit, pas d'API key)
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_maps);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(new GeoPoint(TUNISIA_LAT, TUNISIA_LON));
        mapView.setMultiTouchControls(true);

        // Overlay de localisation (si permission accordée)
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        checkLocationPermission();
        loadTipsAndAddMarkers();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            myLocationOverlay.enableFollowLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableFollowLocation();
        }
    }

    private void loadTipsAndAddMarkers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tips");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clearTipMarkers();
                tipsToGeocode.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Tip tip = data.getValue(Tip.class);
                    if (tip != null && tip.location != null && !tip.location.trim().isEmpty()) {
                        tipsToGeocode.add(tip);
                    }
                }
                geocodeAndAddMarkers(0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void geocodeAndAddMarkers(int index) {
        if (index >= tipsToGeocode.size()) return;

        Tip tip = tipsToGeocode.get(index);
        geocodeExecutor.execute(() -> {
            try {
                double[] coords = geocodeWithNominatim(tip.location);
                if (coords != null) {
                    mainHandler.post(() -> addMarker(tip, coords[0], coords[1]));
                }
                // Respecter la limite Nominatim: 1 requête/seconde
                Thread.sleep(1100);
                geocodeAndAddMarkers(index + 1);
            } catch (Exception e) {
                mainHandler.post(() -> geocodeAndAddMarkers(index + 1));
            }
        });
    }

    private double[] geocodeWithNominatim(String location) {
        try {
            String encoded = URLEncoder.encode(location, "UTF-8");
            String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded
                    + "&format=json&limit=1";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "TravelBuddy/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                String json = sb.toString();
                if (json.startsWith("[") && json.length() > 2) {
                    int latStart = json.indexOf("\"lat\":\"") + 7;
                    int latEnd = json.indexOf("\"", latStart);
                    int lonStart = json.indexOf("\"lon\":\"") + 7;
                    int lonEnd = json.indexOf("\"", lonStart);
                    if (latStart > 6 && lonStart > 6) {
                        double lat = Double.parseDouble(json.substring(latStart, latEnd));
                        double lon = Double.parseDouble(json.substring(lonStart, lonEnd));
                        return new double[]{lat, lon};
                    }
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {
        }
        return null;
    }

    private void clearTipMarkers() {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();
    }

    private void addMarker(Tip tip, double lat, double lon) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(tip.title);
        marker.setSnippet(tip.description != null ? tip.description : tip.location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdown();
    }
}

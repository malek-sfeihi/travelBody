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

/*
 * Écran Carte : affiche une carte OpenStreetMap (via OSMDroid, pas besoin de clé API Google).
 * On récupère tous les tips depuis Firebase, puis pour chaque tip qui a un lieu,
 * on fait un géocodage avec Nominatim (service gratuit d'OpenStreetMap)
 * pour obtenir les coordonnées GPS et placer un marqueur sur la carte.
 */
public class MapsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1;

    // Position par défaut : centre de la Tunisie
    private static final double TUNISIA_LAT = 34.0;
    private static final double TUNISIA_LON = 9.0;
    private static final double DEFAULT_ZOOM = 5.0;

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay; // overlay pour afficher la position de l'utilisateur

    private final List<Tip> tipsToGeocode = new ArrayList<>(); // file d'attente des tips à géocoder

    // On utilise un thread séparé pour le géocodage (interdit de faire du réseau sur le thread principal)
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    // Handler pour revenir sur le thread principal quand on veut toucher à l'UI
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuration obligatoire d'OSMDroid avant d'utiliser la carte
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_maps);

        // Toolbar avec bouton retour
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialisation de la carte
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);  // tuiles OpenStreetMap classiques
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(new GeoPoint(TUNISIA_LAT, TUNISIA_LON));
        mapView.setMultiTouchControls(true); // zoom avec deux doigts

        // Overlay qui montre où se trouve l'utilisateur sur la carte
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // On vérifie la permission GPS puis on charge les tips
        checkLocationPermission();
        loadTipsAndAddMarkers();
    }

    // Demande la permission de localisation si elle n'est pas encore accordée
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            // Permission déjà accordée, on active le suivi de position
            myLocationOverlay.enableFollowLocation();
        }
    }

    // Callback après que l'utilisateur accepte ou refuse la permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableFollowLocation();
        }
    }

    // Écoute le noeud "tips" dans Firebase et lance le géocodage pour chaque tip
    private void loadTipsAndAddMarkers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tips");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clearTipMarkers(); // on enlève les anciens marqueurs
                tipsToGeocode.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Tip tip = data.getValue(Tip.class);
                    // On ne géocode que les tips qui ont un lieu renseigné
                    if (tip != null && tip.location != null && !tip.location.trim().isEmpty()) {
                        tipsToGeocode.add(tip);
                    }
                }
                // On lance le géocodage un par un (à cause de la limite Nominatim)
                geocodeAndAddMarkers(0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Géocode les tips un par un de manière récursive
    // On attend 1,1s entre chaque requête pour respecter la politique de Nominatim
    private void geocodeAndAddMarkers(int index) {
        if (index >= tipsToGeocode.size()) return; // on a tout traité

        Tip tip = tipsToGeocode.get(index);
        geocodeExecutor.execute(() -> {
            try {
                // Appel réseau vers Nominatim pour convertir le nom du lieu en coordonnées
                double[] coords = geocodeWithNominatim(tip.location);
                if (coords != null) {
                    // Retour sur le thread principal pour ajouter le marqueur à la carte
                    mainHandler.post(() -> addMarker(tip, coords[0], coords[1]));
                }
                // Pause obligatoire : Nominatim limite à 1 requête par seconde
                Thread.sleep(1100);
                // On passe au tip suivant
                geocodeAndAddMarkers(index + 1);
            } catch (Exception e) {
                // En cas d'erreur, on saute ce tip et on continue
                mainHandler.post(() -> geocodeAndAddMarkers(index + 1));
            }
        });
    }

    // Appel HTTP vers l'API Nominatim d'OpenStreetMap
    // Envoie le nom du lieu, reçoit du JSON avec lat/lon
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
                // On lit la réponse JSON
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                // Parsing manuel du JSON pour extraire lat et lon
                // (on aurait pu utiliser Gson mais ici c'est simple donc ça suffit)
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

    // Supprime tous les marqueurs de la carte (avant de recharger)
    private void clearTipMarkers() {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate(); // rafraîchit l'affichage de la carte
    }

    // Ajoute un marqueur sur la carte pour un tip donné
    private void addMarker(Tip tip, double lat, double lon) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(tip.title);
        marker.setSnippet(tip.description != null ? tip.description : tip.location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    // Cycle de vie : on resume/pause la carte avec l'activité
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

    // Bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // On arrête proprement le thread de géocodage quand l'activité est détruite
    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdown();
    }
}

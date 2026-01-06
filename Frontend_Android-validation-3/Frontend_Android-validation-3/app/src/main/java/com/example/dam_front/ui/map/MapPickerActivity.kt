package com.example.dam_front.ui.map

import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dam_front.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.util.Locale
import kotlinx.coroutines.*

class MapPickerActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var addressText: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private var selectedPoint: GeoPoint? = null
    private var selectedAddress: String = ""
    private lateinit var geocoder: Geocoder
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Charger la configuration osmdroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        
        // Cacher complètement la barre de statut
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        setContentView(R.layout.activity_map_picker)
        
        map = findViewById(R.id.mapView)
        addressText = findViewById(R.id.addressText)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)
        
        geocoder = Geocoder(this, Locale.getDefault())
        
        // Configurer la carte
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)
        
        // Position initiale : Tunis
        val startPoint = GeoPoint(36.8065, 10.1815)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)
        
        // Gérer les clics sur la carte
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { point ->
                    selectedPoint = point
                    updateMarker(point)
                    reverseGeocode(point)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        })
        map.overlays.add(mapEventsOverlay)
        
        // Bouton Annuler
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        // Bouton Confirmer
        btnConfirm.setOnClickListener {
            if (selectedAddress.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra("address", selectedAddress)
                    selectedPoint?.let { point ->
                        putExtra("latitude", point.latitude)
                        putExtra("longitude", point.longitude)
                    }
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Veuillez sélectionner un lieu sur la carte", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateMarker(point: GeoPoint) {
        // Supprimer les anciens marqueurs
        map.overlays.removeAll { it is Marker }
        
        // Ajouter un nouveau marqueur
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Lieu sélectionné"
        map.overlays.add(marker)
        map.invalidate()
    }
    
    private fun reverseGeocode(point: GeoPoint) {
        addressText.text = "Chargement de l'adresse..."
        coroutineScope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    if (!Geocoder.isPresent()) {
                        android.util.Log.w("MapPickerActivity", "Geocoder non disponible")
                        return@withContext null
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Utiliser la nouvelle API asynchrone pour Android 13+
                        try {
                            var result: List<android.location.Address>? = null
                            val latch = java.util.concurrent.CountDownLatch(1)
                            geocoder.getFromLocation(point.latitude, point.longitude, 1) { addresses ->
                                result = addresses
                                latch.countDown()
                            }
                            // Attendre le callback (max 3 secondes)
                            if (!latch.await(3, java.util.concurrent.TimeUnit.SECONDS)) {
                                android.util.Log.w("MapPickerActivity", "Timeout géocodage")
                            }
                            result
                        } catch (e: Exception) {
                            android.util.Log.e("MapPickerActivity", "Erreur géocodage async: ${e.message}")
                            null
                        }
                    } else {
                        // Utiliser l'ancienne API synchrone
                        try {
                            geocoder.getFromLocation(point.latitude, point.longitude, 1)
                        } catch (e: Exception) {
                            android.util.Log.e("MapPickerActivity", "Erreur géocodage sync: ${e.message}")
                            null
                        }
                    }
                }
                
                if (addresses != null && addresses.isNotEmpty()) {
                    processAddresses(addresses, point)
                } else {
                    // Fallback : utiliser Nominatim API si le géocodage local échoue
                    fetchAddressFromNominatim(point)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapPickerActivity", "Erreur géocodage: ${e.message}")
                fetchAddressFromNominatim(point)
            }
        }
    }
    
    private fun fetchAddressFromNominatim(point: GeoPoint) {
        coroutineScope.launch {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=${point.latitude}&lon=${point.longitude}&zoom=18&addressdetails=1"
                val response = withContext(Dispatchers.IO) {
                    java.net.URL(url).openConnection().apply {
                        setRequestProperty("User-Agent", "SportyKidsApp/1.0")
                        connectTimeout = 5000
                        readTimeout = 5000
                    }.getInputStream().bufferedReader().use { it.readText() }
                }
                
                val json = org.json.JSONObject(response)
                val address = json.optJSONObject("address")
                
                // Fonction pour vérifier si une chaîne est un code (comme V564+GX8)
                fun isCode(str: String): Boolean {
                    return str.matches(Regex("[A-Z]\\d+[+][A-Z]\\d+")) || 
                           str.matches(Regex("[A-Z]\\d+")) ||
                           str.matches(Regex("\\d+[A-Z]+\\d+"))
                }
                
                if (address != null) {
                    val addressParts = mutableListOf<String>()
                    address.optString("road")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    address.optString("house_number")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    address.optString("city")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    address.optString("town")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    address.optString("state")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    address.optString("country")?.takeIf { it.isNotEmpty() && !isCode(it) }?.let { addressParts.add(it) }
                    
                    selectedAddress = if (addressParts.isNotEmpty()) {
                        addressParts.joinToString(", ")
                    } else {
                        // Filtrer les codes de display_name
                        val displayName = json.optString("display_name", "")
                        if (displayName.isNotEmpty()) {
                            displayName.split(", ")
                                .filter { part -> !isCode(part.trim()) }
                                .joinToString(", ")
                                .takeIf { it.isNotEmpty() } ?: "${point.latitude}, ${point.longitude}"
                        } else {
                            "${point.latitude}, ${point.longitude}"
                        }
                    }
                } else {
                    // Filtrer les codes de display_name
                    val displayName = json.optString("display_name", "")
                    selectedAddress = if (displayName.isNotEmpty()) {
                        displayName.split(", ")
                            .filter { part -> !part.matches(Regex("[A-Z]\\d+[+][A-Z]\\d+")) && 
                                            !part.matches(Regex("[A-Z]\\d+")) &&
                                            !part.matches(Regex("\\d+[A-Z]+\\d+")) }
                            .joinToString(", ")
                            .takeIf { it.isNotEmpty() } ?: "${point.latitude}, ${point.longitude}"
                    } else {
                        "${point.latitude}, ${point.longitude}"
                    }
                }
                
                addressText.text = selectedAddress
            } catch (e: Exception) {
                android.util.Log.e("MapPickerActivity", "Erreur Nominatim: ${e.message}")
                selectedAddress = "${point.latitude}, ${point.longitude}"
                addressText.text = "Coordonnées: ${point.latitude}, ${point.longitude}"
            }
        }
    }
    
    private fun processAddresses(addresses: List<android.location.Address>?, point: GeoPoint) {
        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val addressLines = mutableListOf<String>()
            
            // Fonction pour vérifier si une chaîne est un code (comme V564+GX8)
            fun isCode(str: String): Boolean {
                // Vérifier si la chaîne contient un pattern de code (lettres + chiffres + +)
                return str.matches(Regex("[A-Z]\\d+[+][A-Z]\\d+")) || 
                       str.matches(Regex("[A-Z]\\d+")) ||
                       str.matches(Regex("\\d+[A-Z]+\\d+"))
            }
            
            // Construire l'adresse de manière claire en excluant les codes
            address.featureName?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            address.thoroughfare?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            address.subThoroughfare?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            address.locality?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            address.adminArea?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            address.countryName?.let { 
                if (it.isNotEmpty() && !isCode(it)) {
                    addressLines.add(it)
                }
            }
            
            // Si on a des lignes d'adresse, les joindre
            // Sinon, essayer getAddressLine(0) et filtrer les codes
            selectedAddress = if (addressLines.isNotEmpty()) {
                addressLines.joinToString(", ")
            } else {
                val fullAddress = address.getAddressLine(0)
                if (fullAddress != null) {
                    // Filtrer les codes de l'adresse complète
                    fullAddress.split(", ")
                        .filter { part -> !isCode(part.trim()) }
                        .joinToString(", ")
                        .takeIf { it.isNotEmpty() } ?: "${point.latitude}, ${point.longitude}"
                } else {
                    "${point.latitude}, ${point.longitude}"
                }
            }
            
            addressText.text = selectedAddress
        } else {
            // Fallback si le géocodage échoue
            selectedAddress = "${point.latitude}, ${point.longitude}"
            addressText.text = "Coordonnées: ${point.latitude}, ${point.longitude}"
        }
    }
    
    override fun onResume() {
        super.onResume()
        map.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        map.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

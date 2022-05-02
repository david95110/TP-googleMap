package com.example.tp_googlemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.tp_googlemap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.ClusterManager

const val LOCATION_REQ_CODE = 456
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
    GoogleMap.InfoWindowAdapter /*, GoogleMap.OnPoiClickListener */ {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    // On definit un array de station que l’on peut recuperer d’un web service
    private lateinit var stations: ArrayList<Station>
    private val bicycleIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.purple_200)
        BitmapHelper.vectorToBitmap(this, R.drawable.ic_baseline_directions_bike_24, color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stations= arrayListOf()
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //4) Si on n'a pas la permission on la demande
        if ((ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    ) && (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ), LOCATION_REQ_CODE
            );
        }


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        //2) on s’abonne  au clic sur la fenêtre d'un marker
        mMap.setOnInfoWindowClickListener(this);
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        //Créer sa propre fenêtre lors d'un clic sur un marker
        mMap.setInfoWindowAdapter(this);
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        if (getLocation())  mMap.setMyLocationEnabled(true);
    }

    fun afficher(view: View) {
        val lieu = LatLng(45.45, 4.50)
        mMap.addMarker(MarkerOptions().position(lieu).title("Marker in lyon"))
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(lieu))
        //Pour obtenir une animation plutot qu’un saut on remplace
        mMap.animateCamera(CameraUpdateFactory.newLatLng(lieu));
      //  addMarkers(mMap)
        addClusteredMarkers(mMap)
        if (!getLocation()) return
        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location=locationManager.getLastKnownLocation(locationManager.getBestProvider(Criteria(),true)!!)
        if (location!=null){
            val position = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(position).title("Ma position"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lieu))
            Toast.makeText(this,location.longitude.toString()+" "+location.latitude.toString(),Toast.LENGTH_LONG).show()
        }



    }

    override fun onInfoWindowClick(marker: Marker) {
        Toast.makeText(this, marker.getTitle(), Toast.LENGTH_SHORT).show();
    }

    override fun getInfoContents(p0: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        // 1. Get tag
        val station = marker?.tag as? Station ?: return null

        // 2. Inflate view and set title, address, and rating
        val view = LayoutInflater.from(applicationContext).inflate(
            R.layout.marker_layout, null
        )
        view.findViewById<TextView>(
            R.id.text_view_title
        ).text = station.name
        view.findViewById<TextView>(
            R.id.text_view_address
        ).text = station.address
        return view


    }
    fun getLocation(): Boolean {
        return  ((ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ) && (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                ))
    }
    ////
    //3) Au retour de la permission j'active ma localisation
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (getLocation()) {
            if (mMap != null) mMap.isMyLocationEnabled = true
        }

    }
/*
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        if (getLocation())  mMap.setMyLocationEnabled(true);
    }
*/
    /*
    override fun onPoiClick(p0: PointOfInterest) {
        Toast.makeText(this, pointOfInterest.name, Toast.LENGTH_SHORT).show()
        //On recupere la longitude et latitude du point d’interet avec un zoomb
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pointOfInterest.latLng, 18f))
    }
*/
// on definit une methode qui va ajouter des markers aux points d’interets
private fun addMarkers(googleMap: GoogleMap) {
    addItems()
    stations.forEach { station ->
        val marker = googleMap.addMarker(
            MarkerOptions()
                .title(station.name)
                .position(station.latLng)
                .icon(bicycleIcon)
        )
        //On ajoute la station comme tag au marker pour l’afficher
        marker?.tag = station
    }
    //On definit la position de depart de la camera qui peut etre le point initial des poi
    val position=LatLng(51.5145160, -0.1270060)
    mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
}
///
//On simule ici le remplissage des points d’interets
private fun addItems() {

    // Set some lat/lng coordinates to start with.
    var lat = 51.5145160
    var lng = -0.1270060

    // Add ten cluster items in close proximity, for purposes of this example.
    for (i in 0..9) {
        val offset = i / 60.0
        lat += offset
        lng += offset
        val offsetItem =
            stations.add(Station("Title $i", LatLng(lat,lng),"Adresse $i"))

    }
}
////
private fun addClusteredMarkers(googleMap: GoogleMap) {
    // Create the ClusterManager class and set the custom renderer.
    val clusterManager = ClusterManager<Station>(this, googleMap)
    clusterManager.renderer =
        StationRenderer(
            this,
            googleMap,
            clusterManager
        )

    // Add the places to the ClusterManager.
    addItems()
    clusterManager.addItems(stations)
    clusterManager.cluster()

    // Set ClusterManager as the OnCameraIdleListener so that it
    // can re-cluster when zooming in and out.
    googleMap.setOnCameraIdleListener {
        clusterManager.onCameraIdle()
    }
    val position=LatLng(51.5145160, -0.1270060)
    mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
}
 /////
}
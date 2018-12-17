package ar.edu.utn.frsf.isi.dam.laboratorio05;



import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.MyDatabase;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.ReclamoDao;


/**
 * A simple {@link Fragment} subclass.
 */

public class MapaFragment extends SupportMapFragment implements OnMapReadyCallback{

            public interface OnMapaListener{
                public void coordenadasSeleccionadas(LatLng c);
            }

    public void setListener (OnMapaListener listener){
           this.listener=listener;
    }

    private GoogleMap miMapa;
    private int tipoMapa = 0;
    private Boolean permission=false;
    private OnMapaListener listener;
    private ReclamoDao reclamoDao;
    private ArrayList<Reclamo> listaReclamos= new ArrayList<Reclamo>();
    public MapaFragment() {
    }

    @Override
    public
    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);


        Bundle argumentos = getArguments();
        if (argumentos != null){
            tipoMapa = argumentos.getInt("tipo_mapa", 0);
        }
        reclamoDao = MyDatabase.getInstance(this.getActivity()).getReclamoDao();
        cargarReclamos();
        getMapAsync(this);
        return rootView;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void
    onMapReady(GoogleMap map) {

        miMapa = map;
        miMapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        getPermission();
        try{
            if(permission){
                miMapa.setMyLocationEnabled(true);
                miMapa.getUiSettings().setMyLocationButtonEnabled(true);
            }
            else{
                getPermission();
            }
        }catch (Exception e){
            Log.e("Excepción: %s", e.getMessage());
        }
        if (tipoMapa==5){
            miMapa.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    listener.coordenadasSeleccionadas(latLng);
                }
            });
        }
        if (tipoMapa==2){

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            if (!listaReclamos.isEmpty()) {
                for (Reclamo r : listaReclamos) {
                    miMapa.addMarker(new MarkerOptions().position(r.getPosition())
                            .title(r.getId() + "[" + r.getTipo().toString() + "]")
                            .snippet(r.getReclamo())
                    );
                    builder.include(r.getPosition());
                }
                miMapa.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
            }

        }
        if (tipoMapa==4){
            List<LatLng> list = null;

            //leer lat longs reclamos
            if (!listaReclamos.isEmpty()) {

                for (Reclamo r: listaReclamos){
                    double lat = r.getLatitud();
                    double lon = r.getLongitud();
                    list.add(new LatLng(lat,lon));
                }

            }

            HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                    .data(list)
                    .build();

            miMapa.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }

    }

    public void getPermission(){
        if (ActivityCompat.checkSelfPermission(this.getContext(),android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ) {
            permission = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        permission = false;
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission = true;
                }
            }
        }

    }
    public void cargarReclamos(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(!listaReclamos.isEmpty())listaReclamos.clear();
                listaReclamos.addAll(reclamoDao.getAll());
            }
        };
        Thread t1 = new Thread(r);
        t1.start();
    }




}

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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;


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


}

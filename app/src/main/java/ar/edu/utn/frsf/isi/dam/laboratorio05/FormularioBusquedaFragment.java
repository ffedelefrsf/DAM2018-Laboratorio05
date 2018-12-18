package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;

/**
 * A simple {@link Fragment} subclass.
 */
public class FormularioBusquedaFragment extends Fragment {

    public interface onBusquedaListener{
        public void buscar(Reclamo.TipoReclamo reclamo);
    }


    private Spinner tipoReclamo;
    private Button btnBuscar;
    private ArrayAdapter<Reclamo.TipoReclamo> tipoReclamoAdapter;
    private onBusquedaListener listener;


    public void setListener(onBusquedaListener listener){
        this.listener=listener;
    }
    public FormularioBusquedaFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_busqueda, container, false);

        tipoReclamo= (Spinner) v.findViewById(R.id.busq_reclamo_tipo);
        btnBuscar= (Button) v.findViewById(R.id.btnBuscar);

        tipoReclamoAdapter = new ArrayAdapter<Reclamo.TipoReclamo>(getActivity(),android.R.layout.simple_spinner_item,Reclamo.TipoReclamo.values());
        tipoReclamoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoReclamo.setAdapter(tipoReclamoAdapter);

        btnBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tipoReclamo.getSelectedItemPosition()>=0){
                    listener.buscar(tipoReclamoAdapter.getItem(tipoReclamo.getSelectedItemPosition()));
                }
                else{
                    Toast.makeText(getActivity(), "Debe elegir un tipo de reclamo",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        return v;
    }

}

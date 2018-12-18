package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.MyDatabase;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.Reclamo;
import ar.edu.utn.frsf.isi.dam.laboratorio05.modelo.ReclamoDao;

import static android.app.Activity.RESULT_OK;

public class NuevoReclamoFragment extends Fragment {

    public interface OnNuevoLugarListener {
        public void obtenerCoordenadas();
    }

    public void setListener(OnNuevoLugarListener listener) { this.listener = listener;
    }

    private Reclamo reclamoActual;
    private ReclamoDao reclamoDao;

    private EditText reclamoDesc;
    private EditText mail;
    private Spinner tipoReclamo;
    private TextView tvCoord;
    private Button buscarCoord;
    private Button btnGuardar;
    private OnNuevoLugarListener listener;
    private ImageView image;
    private ImageButton btnCamera;
    private String imagePath="";
    static final int REQUEST_IMAGE_SAVE = 1;

    private ArrayAdapter<Reclamo.TipoReclamo> tipoReclamoAdapter;
    public NuevoReclamoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        reclamoDao = MyDatabase.getInstance(this.getActivity()).getReclamoDao();

        View v = inflater.inflate(R.layout.fragment_nuevo_reclamo, container, false);

        reclamoDesc = (EditText) v.findViewById(R.id.reclamo_desc);
        mail= (EditText) v.findViewById(R.id.reclamo_mail);
        tipoReclamo= (Spinner) v.findViewById(R.id.reclamo_tipo);
        tvCoord= (TextView) v.findViewById(R.id.reclamo_coord);
        buscarCoord= (Button) v.findViewById(R.id.btnBuscarCoordenadas);
        btnGuardar= (Button) v.findViewById(R.id.btnGuardar);
        btnCamera = (ImageButton) v.findViewById(R.id.editButton);
        image= (ImageView) v.findViewById(R.id.imageView);

        tipoReclamoAdapter = new ArrayAdapter<Reclamo.TipoReclamo>(getActivity(),android.R.layout.simple_spinner_item,Reclamo.TipoReclamo.values());
        tipoReclamoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoReclamo.setAdapter(tipoReclamoAdapter);

        int idReclamo =0;
        if(getArguments()!=null)  {
            idReclamo = getArguments().getInt("idReclamo",0);
        }

        cargarReclamo(idReclamo);


        boolean edicionActivada = !tvCoord.getText().toString().equals("0;0");
        reclamoDesc.setEnabled(edicionActivada );
        mail.setEnabled(edicionActivada );
        tipoReclamo.setEnabled(edicionActivada);
        btnGuardar.setEnabled(edicionActivada);

        buscarCoord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.obtenerCoordenadas();

            }
        });
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveOrUpdateReclamo();
            }
        });
        return v;
    }

    private void cargarReclamo(final int id){
        if( id >0){
            Runnable hiloCargaDatos = new Runnable() {
                @Override
                public void run() {
                    reclamoActual = reclamoDao.getById(id);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mail.setText(reclamoActual.getEmail());
                            tvCoord.setText(reclamoActual.getLatitud()+";"+reclamoActual.getLongitud());
                            reclamoDesc.setText(reclamoActual.getReclamo());
                            Reclamo.TipoReclamo[] tipos= Reclamo.TipoReclamo.values();
                            for(int i=0;i<tipos.length;i++) {
                                if(tipos[i].equals(reclamoActual.getTipo())) {
                                    tipoReclamo.setSelection(i);
                                    break;
                                }
                            }
                            if (reclamoActual.getImagePath()!= null) {
                                File file = new File(reclamoActual.getImagePath());
                                Bitmap imageBitmap = null;
                                try {
                                    imageBitmap = MediaStore.Images.Media
                                            .getBitmap(getActivity().getContentResolver(),
                                                    Uri.fromFile(file));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                if (imageBitmap != null) {
                                    image.setImageBitmap(imageBitmap);
                                }
                            }
                        }
                    });
                }
            };
            Thread t1 = new Thread(hiloCargaDatos);
            t1.start();
        }else{
            String coordenadas = "0;0";
            if(getArguments()!=null) coordenadas = getArguments().getString("latLng","0;0");
            tvCoord.setText(coordenadas);
            reclamoActual = new Reclamo();
        }

    }

    private void saveOrUpdateReclamo(){
        reclamoActual.setEmail(mail.getText().toString());
        reclamoActual.setReclamo(reclamoDesc.getText().toString());
        reclamoActual.setTipo(tipoReclamoAdapter.getItem(tipoReclamo.getSelectedItemPosition()));
        reclamoActual.setImagePath(imagePath);
        if(tvCoord.getText().toString().length()>0 && tvCoord.getText().toString().contains(";")) {
            String[] coordenadas = tvCoord.getText().toString().split(";");
            reclamoActual.setLatitud(Double.valueOf(coordenadas[0]));
            reclamoActual.setLongitud(Double.valueOf(coordenadas[1]));
        }
        Runnable hiloActualizacion = new Runnable() {
            @Override
            public void run() {

                if(reclamoActual.getId()>0) reclamoDao.update(reclamoActual);
                else reclamoDao.insert(reclamoActual);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // limpiar vista
                        mail.setText(R.string.texto_vacio);
                        tvCoord.setText(R.string.texto_vacio);
                        reclamoDesc.setText(R.string.texto_vacio);
                        getActivity().getFragmentManager().popBackStack();
                    }
                });
            }
        };
        Thread t1 = new Thread(hiloActualizacion);
        t1.start();
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) { }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this.getContext(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_SAVE);
            }
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File dir = this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                dir /* directory */
        );
        imagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {

        if (reqCode == REQUEST_IMAGE_SAVE && resCode == RESULT_OK) {
            File file = new File(imagePath);
            Bitmap imageBitmap = null;
            try {
                imageBitmap = MediaStore.Images.Media
                        .getBitmap(getActivity().getContentResolver(),
                                Uri.fromFile(file));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (imageBitmap != null) {
                image.setImageBitmap(imageBitmap);
            }
        }
    }


}

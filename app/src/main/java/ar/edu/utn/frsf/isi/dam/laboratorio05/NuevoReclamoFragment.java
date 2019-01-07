package ar.edu.utn.frsf.isi.dam.laboratorio05;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
    private ImageView image, imageViewRecord, imageViewPlay, imageViewStop;
    private ImageButton btnCamera;
    private String imagePath="";
    private String audioPath="";
    static final int REQUEST_IMAGE_SAVE = 1;
    private int RECORD_AUDIO_REQUEST_CODE =123 ;
    private Boolean permission=false;
    private Chronometer chronometer;
    private SeekBar seekBar;
    private LinearLayout linearLayoutPlay;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String fileName = "";
    private int lastProgress = 0;
    private Handler mHandler = new Handler();
    private boolean isPlaying = false;

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
        chronometer = (Chronometer) v.findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord = (ImageView) v.findViewById(R.id.imageViewRecord);
        imageViewStop = (ImageView) v.findViewById(R.id.imageViewStop);
        imageViewPlay = (ImageView) v.findViewById(R.id.imageViewPlay);
        linearLayoutPlay = (LinearLayout) v.findViewById(R.id.linearLayoutPlay);
        seekBar = (SeekBar) v.findViewById(R.id.seekBar);

        View.OnClickListener listenerPlayer = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.imageViewRecord:
                        imageViewRecord.setVisibility(View.GONE);
                        imageViewStop.setVisibility(View.VISIBLE);
                        imageViewStop.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_stop));
                        imageViewStop.refreshDrawableState();
                        startRecording();
                        break;
                    case R.id.imageViewStop:
                        imageViewStop.setVisibility(View.GONE);
                        imageViewPlay.setVisibility(View.VISIBLE);
                        imageViewPlay.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_play));
                        imageViewPlay.refreshDrawableState();
                        stopRecording();
                        break;
                    case R.id.imageViewPlay:
                        if(!isPlaying){
                            isPlaying = true;
                            startPlaying();
                        }else {
                            isPlaying = false;
                            stopPlaying();
                        }
                        break;
                }
            }
        };

        imageViewPlay.setOnClickListener(listenerPlayer);
        imageViewRecord.setOnClickListener(listenerPlayer);
        imageViewStop.setOnClickListener(listenerPlayer);

        tipoReclamoAdapter = new ArrayAdapter<Reclamo.TipoReclamo>(getActivity(),android.R.layout.simple_spinner_item,Reclamo.TipoReclamo.values());
        tipoReclamoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoReclamo.setAdapter(tipoReclamoAdapter);

        int idReclamo =0;
        if(getArguments()!=null)  {
            idReclamo = getArguments().getInt("idReclamo",0);
        }

        cargarReclamo(idReclamo);

        getPermission();
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

    private void getPermission(){
        if (ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this.getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {

            permission=true;


        }
        else{
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                            String permissions[],
                                            int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                permission=true;

            } else {
                Toast.makeText(this.getContext(), "Necesitamos los permisos para que la app funcione correctamente.", Toast.LENGTH_LONG).show();
                permission=false;
            }
        }
    }


    private void startRecording() {

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        File root = android.os.Environment.getExternalStorageDirectory();
        File file = new File(root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios");
        if (!file.exists()) {
            file.mkdirs();
        }
        audioPath=file.getAbsolutePath();
        fileName =  root.getAbsolutePath() + "/VoiceRecorderSimplifiedCoding/Audios/" +
                String.valueOf(System.currentTimeMillis() + ".mp3");
        Log.d("filename",fileName);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastProgress = 0;
        seekBar.setProgress(0);
        stopPlaying();
        //Incia cronometro
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }


    private void stopPlaying() {
        try{
            mPlayer.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mPlayer = null;
        // Se muestra boton para reproducir
        imageViewPlay.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_play));
        imageViewPlay.refreshDrawableState();
        chronometer.stop();
    }

    private void stopRecording() {

        try{
            mRecorder.stop();
            mRecorder.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mRecorder = null;
        //Detener cronometro
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());

        Toast.makeText(this.getContext(), "Audio grabado con éxito.", Toast.LENGTH_SHORT).show();
    }
    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("LOG_TAG", "prepare() failed");
        }

        imageViewPlay.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_pause));
        imageViewPlay.refreshDrawableState();

        seekBar.setProgress(lastProgress);
        mPlayer.seekTo(lastProgress);
        seekBar.setMax(mPlayer.getDuration());
        seekUpdation();
        chronometer.start();


        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imageViewPlay.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_play));
                imageViewPlay.refreshDrawableState();
                isPlaying = false;
                chronometer.stop();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if( mPlayer!=null && fromUser ){
                    mPlayer.seekTo(progress);
                    chronometer.setBase(SystemClock.elapsedRealtime() - mPlayer.getCurrentPosition());
                    lastProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seekUpdation();
        }
    };

    private void seekUpdation() {
        if(mPlayer != null){
            int mCurrentPosition = mPlayer.getCurrentPosition() ;
            seekBar.setProgress(mCurrentPosition);
            lastProgress = mCurrentPosition;
        }
        mHandler.postDelayed(runnable, 100);
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
        reclamoActual.setAudioPath(audioPath);
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

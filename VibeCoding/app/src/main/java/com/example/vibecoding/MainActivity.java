package com.example.vibecoding;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    // Variables de UI
    private TextView tvTiempoIris, tvTiempoHuellas;
    private Spinner spinnerNacionalidad;
    private ImageView ivCheckIris, ivCheckHuellas;
    private Button btnGuardar, btnVerPorcentaje, btnNuevoRegistro;

    // Temporizadores
    private Handler handler = new Handler();
    private long startTimeIris = 0L, elapsedTimeIris = 0L;
    private long startTimeHuellas = 0L, elapsedTimeHuellas = 0L;
    private boolean irisFinalizado = false, huellasFinalizado = false;

    // Base de datos
    private BioScanDbHelper dbHelper;

    // Nacionalidades (ejemplo, puedes agregar más)
    private final String[] nacionalidades = new String[]{
            "México", "Brasil", "España", "Colombia", "Argentina", "Chile", "Perú", "Ecuador", "Canadá", "Francia"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar UI
        tvTiempoIris = findViewById(R.id.tvTiempoIris);
        tvTiempoHuellas = findViewById(R.id.tvTiempoHuellas);
        spinnerNacionalidad = findViewById(R.id.spinnerNacionalidad);
        ivCheckIris = findViewById(R.id.ivCheckIris);
        ivCheckHuellas = findViewById(R.id.ivCheckHuellas);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVerPorcentaje = findViewById(R.id.btnVerPorcentaje);
        btnNuevoRegistro = findViewById(R.id.btnNuevoRegistro);

        // Inicializar base de datos
        dbHelper = new BioScanDbHelper(this);

        // Llenar spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nacionalidades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNacionalidad.setAdapter(adapter);

        // Listeners para iniciar/detener temporizadores
        ivCheckIris.setOnClickListener(v -> {
            if (!irisFinalizado) {
                if (startTimeIris == 0L) {
                    startTimeIris = System.currentTimeMillis();
                    handler.post(updateIrisTimer);
                    Toast.makeText(this, "Iniciando toma de iris", Toast.LENGTH_SHORT).show();
                } else {
                    elapsedTimeIris = System.currentTimeMillis() - startTimeIris;
                    handler.removeCallbacks(updateIrisTimer);
                    irisFinalizado = true;
                    if (android.os.Build.VERSION.SDK_INT >= 11) {
                        ivCheckIris.setAlpha(0.5f);
                    }
                    Toast.makeText(this, "Toma de iris finalizada", Toast.LENGTH_SHORT).show();
                }
            }
        });
        ivCheckHuellas.setOnClickListener(v -> {
            if (!huellasFinalizado) {
                if (startTimeHuellas == 0L) {
                    startTimeHuellas = System.currentTimeMillis();
                    handler.post(updateHuellasTimer);
                    Toast.makeText(this, "Iniciando toma de huellas", Toast.LENGTH_SHORT).show();
                } else {
                    elapsedTimeHuellas = System.currentTimeMillis() - startTimeHuellas;
                    handler.removeCallbacks(updateHuellasTimer);
                    huellasFinalizado = true;
                    if (android.os.Build.VERSION.SDK_INT >= 11) {
                        ivCheckHuellas.setAlpha(0.5f);
                    }
                    Toast.makeText(this, "Toma de huellas finalizada", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Guardar registro
        btnGuardar.setOnClickListener(v -> guardarRegistro());

        // Ver porcentaje/promedio
        btnVerPorcentaje.setOnClickListener(v -> mostrarPromedio());

        // Nuevo registro
        btnNuevoRegistro.setOnClickListener(v -> reiniciarRegistro());
    }

    // Runnable para actualizar el tiempo de iris
    private Runnable updateIrisTimer = new Runnable() {
        @Override
        public void run() {
            long tiempo = System.currentTimeMillis() - startTimeIris;
            tvTiempoIris.setText(getString(R.string.tiempo, formatTime(tiempo)));
            handler.postDelayed(this, 1000);
        }
    };

    // Runnable para actualizar el tiempo de huellas
    private Runnable updateHuellasTimer = new Runnable() {
        @Override
        public void run() {
            long tiempo = System.currentTimeMillis() - startTimeHuellas;
            tvTiempoHuellas.setText(getString(R.string.tiempo, formatTime(tiempo)));
            handler.postDelayed(this, 1000);
        }
    };

    // Formatear tiempo en HH:mm:ss
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }

    // Guardar registro en la base de datos
    private void guardarRegistro() {
        if (!irisFinalizado || !huellasFinalizado) {
            Toast.makeText(this, getString(R.string.finaliza_ambas), Toast.LENGTH_SHORT).show();
            return;
        }
        String tiempoIris = formatTime(elapsedTimeIris);
        String tiempoHuellas = formatTime(elapsedTimeHuellas);
        String nacionalidad = spinnerNacionalidad.getSelectedItem().toString();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BioScanDbHelper.COLUMN_TIEMPO_IRIS, tiempoIris);
        values.put(BioScanDbHelper.COLUMN_TIEMPO_HUELLAS, tiempoHuellas);
        values.put(BioScanDbHelper.COLUMN_NACIONALIDAD, nacionalidad);
        long newRowId = db.insert(BioScanDbHelper.TABLE_NAME, null, values);
        if (newRowId != -1) {
            Toast.makeText(this, getString(R.string.registro_guardado), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.error_guardar), Toast.LENGTH_SHORT).show();
        }
    }

    // Mostrar promedio de tiempos
    private void mostrarPromedio() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT AVG(strftime('%s', '1970-01-01 ' || " + BioScanDbHelper.COLUMN_TIEMPO_IRIS + ")) as avg_iris, " +
                "AVG(strftime('%s', '1970-01-01 ' || " + BioScanDbHelper.COLUMN_TIEMPO_HUELLAS + ")) as avg_huellas FROM " +
                BioScanDbHelper.TABLE_NAME;
        android.database.Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            long avgIris = cursor.getLong(cursor.getColumnIndexOrThrow("avg_iris"));
            long avgHuellas = cursor.getLong(cursor.getColumnIndexOrThrow("avg_huellas"));
            String msg = getString(R.string.promedio_iris, formatTime(avgIris * 1000)) + "\n" +
                    getString(R.string.promedio_huellas, formatTime(avgHuellas * 1000));
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.sin_registros), Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    // Reiniciar registro
    private void reiniciarRegistro() {
        startTimeIris = 0L;
        elapsedTimeIris = 0L;
        startTimeHuellas = 0L;
        elapsedTimeHuellas = 0L;
        irisFinalizado = false;
        huellasFinalizado = false;
        tvTiempoIris.setText(getString(R.string.tiempo_inicial));
        tvTiempoHuellas.setText(getString(R.string.tiempo_inicial));
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ivCheckIris.setAlpha(1.0f);
            ivCheckHuellas.setAlpha(1.0f);
        }
        handler.removeCallbacks(updateIrisTimer);
        handler.removeCallbacks(updateHuellasTimer);
        Toast.makeText(this, getString(R.string.nuevo_registro_iniciado), Toast.LENGTH_SHORT).show();
    }
}
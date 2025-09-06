package com.example.vibecoding;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BioScanDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "bioscan.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "registros_bio";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIEMPO_IRIS = "tiempo_iris";
    public static final String COLUMN_TIEMPO_HUELLAS = "tiempo_huellas";
    public static final String COLUMN_NACIONALIDAD = "nacionalidad";
    public static final String COLUMN_FECHA_REGISTRO = "fecha_registro";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TIEMPO_IRIS + " TEXT NOT NULL, " +
                    COLUMN_TIEMPO_HUELLAS + " TEXT NOT NULL, " +
                    COLUMN_NACIONALIDAD + " TEXT NOT NULL, " +
                    COLUMN_FECHA_REGISTRO + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    public BioScanDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Clase para transportar datos de país y promedio
    public static class PaisPromedio {
        public String pais;
        public float promedioTotalSegundos;
        public PaisPromedio(String pais, float promedioTotalSegundos) {
            this.pais = pais;
            this.promedioTotalSegundos = promedioTotalSegundos;
        }
    }

    // Método para convertir HH:mm:ss a segundos
    public static int tiempoToSegundos(String tiempo) {
        if (tiempo == null) return 0;
        String[] partes = tiempo.split(":");
        if (partes.length != 3) return 0;
        try {
            int horas = Integer.parseInt(partes[0]);
            int minutos = Integer.parseInt(partes[1]);
            int segundos = Integer.parseInt(partes[2]);
            return horas * 3600 + minutos * 60 + segundos;
        } catch (Exception e) {
            return 0;
        }
    }

    // Método para obtener el top 5 países con mejor rendimiento
    public java.util.List<PaisPromedio> obtenerTop5PaisesMejorRendimiento() {
        java.util.List<PaisPromedio> lista = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Consulta: obtener promedios de iris y huellas por país
        String query = "SELECT " + COLUMN_NACIONALIDAD + ", " +
                "AVG(" + COLUMN_TIEMPO_IRIS + ") as avg_iris, " +
                "AVG(" + COLUMN_TIEMPO_HUELLAS + ") as avg_huellas FROM (" +
                "SELECT " + COLUMN_NACIONALIDAD + ", " +
                "(strftime('%H', " + COLUMN_TIEMPO_IRIS + ") * 3600 + strftime('%M', " + COLUMN_TIEMPO_IRIS + ") * 60 + strftime('%S', " + COLUMN_TIEMPO_IRIS + ")) as " + COLUMN_TIEMPO_IRIS + ", " +
                "(strftime('%H', " + COLUMN_TIEMPO_HUELLAS + ") * 3600 + strftime('%M', " + COLUMN_TIEMPO_HUELLAS + ") * 60 + strftime('%S', " + COLUMN_TIEMPO_HUELLAS + ")) as " + COLUMN_TIEMPO_HUELLAS +
                " FROM " + TABLE_NAME + ") " +
                "GROUP BY " + COLUMN_NACIONALIDAD + " ORDER BY (avg_iris + avg_huellas) ASC LIMIT 5;";
        android.database.Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String pais = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NACIONALIDAD));
                float avgIris = cursor.getFloat(cursor.getColumnIndexOrThrow("avg_iris"));
                float avgHuellas = cursor.getFloat(cursor.getColumnIndexOrThrow("avg_huellas"));
                float promedioTotal = avgIris + avgHuellas;
                lista.add(new PaisPromedio(pais, promedioTotal));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }
}

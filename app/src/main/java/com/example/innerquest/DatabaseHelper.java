package com.example.innerquest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log; // Importar para logging de errores

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "InnerQuestDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD_HASH = "password_hash"; // Renombrado para indicar que es un hash

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL para crear la tabla de usuarios
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE," // UNIQUE para asegurar que no haya nombres de usuario duplicados
                + COLUMN_PASSWORD_HASH + " TEXT" + ")"; // Almacenaremos el hash aquí
        db.execSQL(CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Eliminar tabla antigua si existe
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        // Crear tablas de nuevo
        onCreate(db);
    }

    /**
     * Genera un hash SHA-256 de la contraseña dada.
     * @param password La contraseña en texto plano.
     * @return El hash SHA-256 de la contraseña como una cadena hexadecimal, o null si falla.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            // Convertir el array de bytes a una cadena hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("DatabaseHelper", "Error al aplicar SHA-256: " + e.getMessage());
            return null; // Devolver null si el algoritmo no está disponible (raro)
        }
    }

    // Método para añadir un nuevo usuario
    public boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase(); // Obtener una instancia de la base de datos para escribir
        ContentValues values = new ContentValues();

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            db.close();
            return false; // Falló el hashing
        }

        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD_HASH, hashedPassword); // Guardar el hash de la contraseña

        // Insertar la fila en la tabla
        long result = db.insert(TABLE_USERS, null, values);
        db.close(); // Cerrar la conexión a la base de datos

        // Si el id es -1, significa que hubo un error (ej. usuario ya existe si COLUMN_USERNAME es UNIQUE)
        return result != -1;
    }

    // Método para verificar credenciales de usuario
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase(); // Obtener una instancia de la base de datos para leer
        Cursor cursor = null;
        boolean userExists = false;

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            db.close();
            return false; // Falló el hashing
        }

        try {
            // Definir las columnas que queremos recuperar
            String[] columns = {COLUMN_ID};
            // Cláusula WHERE para buscar el usuario y el hash de la contraseña
            String selection = COLUMN_USERNAME + " = ?" + " AND " + COLUMN_PASSWORD_HASH + " = ?"; // Ahora compara con el hash
            // Argumentos para la cláusula WHERE
            String[] selectionArgs = {username, hashedPassword}; // Usar el hash de la contraseña

            // Ejecutar la consulta
            cursor = db.query(TABLE_USERS, // Tabla a consultar
                    columns,            // Columnas a devolver
                    selection,          // Cláusula WHERE
                    selectionArgs,      // Argumentos para la cláusula WHERE
                    null,               // GROUP BY
                    null,               // HAVING
                    null);              // ORDER BY

            // Si el cursor tiene al menos una fila, significa que el usuario existe con esas credenciales
            userExists = (cursor != null && cursor.moveToFirst());
        } finally {
            if (cursor != null) {
                cursor.close(); // Cerrar el cursor para liberar recursos
            }
            db.close(); // Cerrar la conexión a la base de datos
        }
        return userExists;
    }
}
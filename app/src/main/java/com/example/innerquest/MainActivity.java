package com.example.innerquest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton; // Nuevo botón para registrar
    private DatabaseHelper dbHelper; // Instancia del ayudante de la base de datos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Usará el layout para la pantalla de login

        // Inicializar el ayudante de la base de datos
        dbHelper = new DatabaseHelper(this);

        usernameEditText = findViewById(R.id.username_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button); // Encontrar el nuevo botón

        loginButton.setOnClickListener(v -> attemptLogin());
        registerButton.setOnClickListener(v -> registerUser()); // Asignar listener al botón de registro
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar credenciales contra la base de datos
        if (dbHelper.checkUser(username, password)) {
            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
            // Iniciar la actividad principal de la aplicación (AppActivity)
            Intent intent = new Intent(MainActivity.this, AppActivity.class);
            startActivity(intent);
            finish(); // Cierra la actividad de login para que el usuario no pueda volver a ella con el botón "atrás"
        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un usuario y contraseña para registrar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.addUser(username, password)) {
            Toast.makeText(this, "Usuario " + username + " registrado exitosamente. Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show();
            // Opcional: Limpiar los campos o redirigir
            usernameEditText.setText("");
            passwordEditText.setText("");
        } else {
            Toast.makeText(this, "Error al registrar usuario. Posiblemente el usuario ya existe.", Toast.LENGTH_LONG).show();
        }
    }
}
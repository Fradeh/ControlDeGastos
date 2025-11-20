package com.freddy.controldegastos.AUTH;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.text.InputType;
import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.GASTOS.MainActivity;
import com.freddy.controldegastos.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText edtCorreoLogin, edtContrasenaLogin;
    private CheckBox chkMostrarLogin;
    private Button btnIniciarSesion;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtCorreoLogin = findViewById(R.id.edtCorreoLogin);
        edtContrasenaLogin = findViewById(R.id.edtContrasenaLogin);
        chkMostrarLogin = findViewById(R.id.chkMostrarLogin);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);

        mAuth = FirebaseAuth.getInstance();

        //  Mostrar u ocultar contraseña
        chkMostrarLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtContrasenaLogin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                edtContrasenaLogin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        //  Iniciar sesión
        btnIniciarSesion.setOnClickListener(v -> {
            String correo = edtCorreoLogin.getText().toString().trim();
            String contrasena = edtContrasenaLogin.getText().toString().trim();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Sesión iniciada", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            Exception exception = task.getException();
                            String mensajeError = "Error al iniciar sesión";

                            if (exception != null) {
                                String error = exception.getMessage();

                                if (error.contains("password is invalid") || error.contains("auth credential is incorrect")) {
                                    mensajeError = "La contraseña es incorrecta.";
                                } else if (error.contains("no user record")) {
                                    mensajeError = "El correo no está registrado.";
                                } else if (error.contains("email address is badly formatted")) {
                                    mensajeError = "El formato del correo es inválido.";
                                } else {
                                    mensajeError = "No se pudo iniciar sesión. Verifica tus datos.";
                                }
                            }

                            Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
                        }

        });
        });

        // Ir a pantalla de registro
        TextView txtIrRegistro = findViewById(R.id.txtIrRegistro);
        txtIrRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroActivity.class));
            finish();
        });

        TextView txtOlvidarContrasena = findViewById(R.id.txtOlvidarContrasena);
        txtOlvidarContrasena.setOnClickListener(v -> {
            mostrarDialogoRecuperarContrasena();
        });

    }

    private void mostrarDialogoRecuperarContrasena() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recuperar contraseña");
        builder.setMessage("Ingresa tu correo para restablecer la contraseña:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String correo = input.getText().toString().trim();
            if (correo.isEmpty()) {
                Toast.makeText(this, "Debes ingresar tu correo", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Revisa tu bandeja de spam, el correo fue enviado", Toast.LENGTH_LONG).show();

                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancelar", null);

        builder.show();
    }

}

package com.freddy.controldegastos.AUTH;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

        chkMostrarLogin.setOnCheckedChangeListener((buttonView, isChecked) ->
                edtContrasenaLogin.setTransformationMethod(isChecked
                        ? HideReturnsTransformationMethod.getInstance()
                        : PasswordTransformationMethod.getInstance()));

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());

        TextView txtIrRegistro = findViewById(R.id.txtIrRegistro);
        txtIrRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistroActivity.class));
            finish();
        });

        TextView txtOlvidarContrasena = findViewById(R.id.txtOlvidarContrasena);
        txtOlvidarContrasena.setOnClickListener(v -> mostrarDialogoRecuperarContrasena());
    }

    private void iniciarSesion() {
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
                        Toast.makeText(this, mensajeErrorLogin(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String mensajeErrorLogin(Exception exception) {
        if (exception == null || exception.getMessage() == null) {
            return "No se pudo iniciar sesión. Verifica tus datos.";
        }

        String error = exception.getMessage();
        if (error.contains("password is invalid") || error.contains("auth credential is incorrect")) {
            return "La contraseña es incorrecta.";
        }
        if (error.contains("no user record")) {
            return "El correo no está registrado.";
        }
        if (error.contains("email address is badly formatted")) {
            return "El formato del correo es inválido.";
        }
        return "No se pudo iniciar sesión. Verifica tus datos.";
    }

    private void mostrarDialogoRecuperarContrasena() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recuperar_contrasena, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        EditText input = dialogView.findViewById(R.id.edtCorreoRecuperar);
        dialogView.findViewById(R.id.btnCancelarRecuperar).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnEnviarRecuperar).setOnClickListener(v -> {
            String correo = input.getText().toString().trim();
            if (correo.isEmpty()) {
                Toast.makeText(this, "Debes ingresar tu correo", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(this, "Revisa tu bandeja de spam, el correo fue enviado", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }
}

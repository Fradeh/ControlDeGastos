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

import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText edtNombre, edtCorreo, edtContrasena, edtConfirmar;
    private Button btnRegistrarse;
    private CheckBox chkMostrar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        edtNombre = findViewById(R.id.edtNombre);
        edtCorreo = findViewById(R.id.edtCorreo);
        edtContrasena = findViewById(R.id.edtContrasena);
        edtConfirmar = findViewById(R.id.edtConfirmarContrasena);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        chkMostrar = findViewById(R.id.chkMostrarContrasena);

        mAuth = FirebaseAuth.getInstance();

        chkMostrar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtContrasena.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                edtConfirmar.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                edtContrasena.setTransformationMethod(PasswordTransformationMethod.getInstance());
                edtConfirmar.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        btnRegistrarse.setOnClickListener(v -> {
            String nombre = edtNombre.getText().toString().trim();
            String correo = edtCorreo.getText().toString().trim();
            String contrasena = edtContrasena.getText().toString().trim();
            String confirmar = edtConfirmar.getText().toString().trim();

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || confirmar.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!contrasena.equals(confirmar)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            if (contrasena.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> datosUsuario = new HashMap<>();
                            datosUsuario.put("nombre", nombre);
                            datosUsuario.put("correo", correo);
                            datosUsuario.put("esPremium", false);

                            FirebaseDatabase.getInstance().getReference("usuarios")
                                    .child(uid)
                                    .setValue(datosUsuario);

                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        TextView txtIrALogin = findViewById(R.id.txtIrALogin);
        txtIrALogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}

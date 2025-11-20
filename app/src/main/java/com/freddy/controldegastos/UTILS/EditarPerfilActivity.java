package com.freddy.controldegastos.UTILS;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.freddy.controldegastos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final String TAG = "EditarPerfilActivity";

    private EditText edtNombre;
    private ImageView imgPerfil;
    private Button btnSeleccionar, btnGuardar;

    private Uri imageUri;
    private FirebaseUser usuario;
    private DatabaseReference usuariosRef;

    // Photo picker sin permisos (GetContent)
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this)
                            .load(imageUri)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(imgPerfil);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        edtNombre = findViewById(R.id.edtNombre);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnSeleccionar = findViewById(R.id.btnSeleccionarFoto);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        usuariosRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(usuario.getUid());

        // Precarga nombre/foto actuales
        usuariosRef.get().addOnSuccessListener(s -> {
            Object n = s.child("nombre").getValue();
            Object f = s.child("fotoPerfil").getValue();
            if (n != null) edtNombre.setText(String.valueOf(n));
            if (f != null) {
                String fotoUrl = String.valueOf(f);
                if (!fotoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(fotoUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(imgPerfil);
                }
            }
        });

        btnSeleccionar.setOnClickListener(v -> pickImage.launch("image/*"));
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void guardarCambios() {
        String nuevoNombre = edtNombre.getText().toString().trim();

        // Permitir: solo nombre, solo foto o ambos. No permitir ambos vacíos.
        if (nuevoNombre.isEmpty() && imageUri == null) {
            Toast.makeText(this, "Ingresa un nombre o selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Guardando...");
        dialog.setCancelable(false);
        dialog.show();

        Map<String, Object> updates = new HashMap<>();
        if (!nuevoNombre.isEmpty()) {
            updates.put("nombre", nuevoNombre);
        }

        if (imageUri != null) {
            // Sube a Storage en carpeta del usuario: perfiles/<uid>/<uuid>.jpg
            String uid = usuario.getUid();
            String filename = "perfiles/" + uid + "/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = FirebaseStorage.getInstance().getReference(filename);

            StorageMetadata meta = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            ref.putFile(imageUri, meta)
                    .addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        updates.put("fotoPerfil", uri.toString());
                        aplicarUpdates(dialog, updates, nuevoNombre, uri.toString());
                    }))
                    .addOnFailureListener(e -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            // Solo nombre
            aplicarUpdates(dialog, updates, nuevoNombre, null);
        }
    }

    private void aplicarUpdates(ProgressDialog dialog,
                                Map<String, Object> updates,
                                String nombreActualizado,
                                @Nullable String fotoUrlActualizada) {

        if (updates.isEmpty()) {
            dialog.dismiss();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        usuariosRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    dialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();

                        // Devolver datos para refrescar el header sin reconsultar
                        Intent result = new Intent();
                        if (nombreActualizado != null && !nombreActualizado.isEmpty()) {
                            result.putExtra("nombre_actualizado", nombreActualizado);
                        }
                        if (fotoUrlActualizada != null) {
                            result.putExtra("foto_actualizada", fotoUrlActualizada);
                        }
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        Toast.makeText(this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

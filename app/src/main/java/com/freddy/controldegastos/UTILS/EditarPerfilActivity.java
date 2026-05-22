package com.freddy.controldegastos.UTILS;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

    private EditText edtNombre;
    private ImageView imgPerfil;
    private Button btnSeleccionar, btnGuardar, btnQuitarFoto;
    private ImageButton btnAtras;

    private Uri imageUri;
    private boolean quitarFoto = false;
    private FirebaseUser usuario;
    private DatabaseReference usuariosRef;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    quitarFoto = false;
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
        btnQuitarFoto = findViewById(R.id.btnQuitarFoto);
        btnGuardar = findViewById(R.id.btnGuardarPerfil);
        btnAtras = findViewById(R.id.btnAtrasEditarPerfil);

        usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usuariosRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(usuario.getUid());

        cargarPerfilActual();

        btnAtras.setOnClickListener(v -> finish());
        btnSeleccionar.setOnClickListener(v -> pickImage.launch("image/*"));
        btnQuitarFoto.setOnClickListener(v -> {
            imageUri = null;
            quitarFoto = true;
            imgPerfil.setImageResource(R.drawable.ic_person);
            Toast.makeText(this, "La foto se quitará al guardar", Toast.LENGTH_SHORT).show();
        });
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarPerfilActual() {
        usuariosRef.get().addOnSuccessListener(snapshot -> {
            Object nombre = snapshot.child("nombre").getValue();
            Object foto = snapshot.child("fotoPerfil").getValue();

            if (nombre != null) edtNombre.setText(String.valueOf(nombre));
            if (foto != null) {
                String fotoUrl = String.valueOf(foto);
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
    }

    private void guardarCambios() {
        String nuevoNombre = edtNombre.getText().toString().trim();

        if (nuevoNombre.isEmpty() && imageUri == null && !quitarFoto) {
            Toast.makeText(this, "Ingresa un nombre, selecciona una imagen o quita la foto", Toast.LENGTH_SHORT).show();
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
        if (quitarFoto) {
            updates.put("fotoPerfil", "");
        }

        if (imageUri != null) {
            subirNuevaFoto(dialog, updates, nuevoNombre);
        } else {
            aplicarUpdates(dialog, updates, nuevoNombre, quitarFoto ? "" : null);
        }
    }

    private void subirNuevaFoto(ProgressDialog dialog, Map<String, Object> updates, String nuevoNombre) {
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

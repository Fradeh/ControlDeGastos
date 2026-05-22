package com.freddy.controldegastos.UTILS;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.BD.GastoFijoDao;
import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GASTOS.GastoAdapterRecycler;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.freddy.controldegastos.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BackupHelper {

    public static class BackupData {
        public List<Gasto> gastos;
        public List<GastoFijo> gastos_fijos;

        public BackupData() {}

        public BackupData(List<Gasto> gastos, List<GastoFijo> gastos_fijos) {
            this.gastos = gastos;
            this.gastos_fijos = gastos_fijos;
        }
    }

    public static void hacerBackup(Context context) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(context, "No hay sesión iniciada", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("_backups")
                .child(usuario.getUid());

        AppDatabase db = AppDatabase.obtenerInstancia(context);
        List<Gasto> gastos = db.gastoDao().obtenerTodos();
        List<GastoFijo> gastosFijos = db.gastoFijoDao().obtenerTodos();

        BackupData data = new BackupData(gastos, gastosFijos);
        ref.setValue(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Backup guardado correctamente", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error al guardar backup: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public static void confirmarYRestaurarBackup(Context context, GastoAdapterRecycler adapter, List<Gasto> listaGastos, Runnable actualizarResumenCallback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_restaurar_backup, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelarRestaurarBackup).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmarRestaurarBackup).setOnClickListener(v -> {
            dialog.dismiss();
            restaurarBackup(context, adapter, listaGastos, actualizarResumenCallback);
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    public static void restaurarBackup(Context context, GastoAdapterRecycler adapter, List<Gasto> listaGastos, Runnable actualizarResumenCallback) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(context, "No hay sesión iniciada", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference backupRef = FirebaseDatabase.getInstance()
                .getReference("_backups")
                .child(usuario.getUid());

        backupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()
                        || (!snapshot.child("gastos").exists() && !snapshot.child("gastos_fijos").exists())) {
                    Toast.makeText(context, "No hay backup disponible para restaurar", Toast.LENGTH_LONG).show();
                    return;
                }

                List<Gasto> gastosRestaurados = new ArrayList<>();
                List<GastoFijo> fijosRestaurados = new ArrayList<>();

                for (DataSnapshot gastoSnap : snapshot.child("gastos").getChildren()) {
                    Gasto gasto = gastoSnap.getValue(Gasto.class);
                    if (gasto != null) {
                        gastosRestaurados.add(gasto);
                    }
                }

                for (DataSnapshot fijoSnap : snapshot.child("gastos_fijos").getChildren()) {
                    GastoFijo fijo = fijoSnap.getValue(GastoFijo.class);
                    if (fijo != null) {
                        fijosRestaurados.add(fijo);
                    }
                }

                if (gastosRestaurados.isEmpty() && fijosRestaurados.isEmpty()) {
                    Toast.makeText(context, "El backup está vacío. No se modificaron tus datos.", Toast.LENGTH_LONG).show();
                    return;
                }

                AppDatabase db = AppDatabase.obtenerInstancia(context);
                GastoDao gastoDao = db.gastoDao();
                GastoFijoDao gastoFijoDao = db.gastoFijoDao();

                gastoDao.eliminarTodos();
                gastoFijoDao.eliminarTodos();

                for (Gasto gasto : gastosRestaurados) {
                    gastoDao.insertar(gasto);
                }

                for (GastoFijo fijo : fijosRestaurados) {
                    gastoFijoDao.insertar(fijo);
                }

                listaGastos.clear();
                listaGastos.addAll(gastoDao.obtenerTodos());
                adapter.notifyDataSetChanged();

                if (actualizarResumenCallback != null) {
                    actualizarResumenCallback.run();
                }

                Log.d("BACKUP", "Snapshot recibido: " + snapshot.exists());
                Log.d("BACKUP", "Gastos recibidos: " + snapshot.child("gastos").getChildrenCount());
                Log.d("BACKUP", "Fijos recibidos: " + snapshot.child("gastos_fijos").getChildrenCount());

                Toast.makeText(context, "Backup restaurado: " + gastosRestaurados.size() + " gastos y " + fijosRestaurados.size() + " fijos", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error al restaurar backup: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

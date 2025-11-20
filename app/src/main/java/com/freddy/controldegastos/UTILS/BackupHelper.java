package com.freddy.controldegastos.UTILS;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.BD.GastoFijoDao;
import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GASTOS.GastoAdapterRecycler;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.List;

public class BackupHelper {

    // Modelo para subir todo en una sola llamada
    public static class BackupData {
        public List<Gasto> gastos;
        public List<GastoFijo> gastos_fijos; // mismo nombre que usas en la restauración

        public BackupData() {} // requerido por Firebase
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

        String uid = usuario.getUid();


        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("_backups")
                .child(uid);

        AppDatabase db = AppDatabase.obtenerInstancia(context);
        List<Gasto> gastos = db.gastoDao().obtenerTodos();
        List<GastoFijo> gastosFijos = db.gastoFijoDao().obtenerTodos();

        BackupData data = new BackupData(gastos, gastosFijos);

        // Escribir TODO en una sola operación
        ref.setValue(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Backup guardado correctamente", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error al guardar backup: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public static void confirmarYRestaurarBackup(Context context, GastoAdapterRecycler adapter, List<Gasto> listaGastos, Runnable actualizarResumenCallback) {
        new AlertDialog.Builder(context)
                .setTitle("Restaurar Backup")
                .setMessage("Esto reemplazará tus gastos actuales por el backup guardado en la nube. ¿Deseas continuar?")
                .setPositiveButton("Sí", (dialog, which) -> restaurarBackup(context, adapter, listaGastos, actualizarResumenCallback))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static void restaurarBackup(Context context, GastoAdapterRecycler adapter, List<Gasto> listaGastos, Runnable actualizarResumenCallback) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(context, "No hay sesión iniciada", Toast.LENGTH_SHORT).show();
            return;
        }

        // Igual que en backup: apunta a _backups
        DatabaseReference backupRef = FirebaseDatabase.getInstance()
                .getReference("_backups")
                .child(usuario.getUid());

        backupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                AppDatabase db = AppDatabase.obtenerInstancia(context);
                GastoDao gastoDao = db.gastoDao();
                GastoFijoDao gastoFijoDao = db.gastoFijoDao();

                gastoDao.eliminarTodos();
                gastoFijoDao.eliminarTodos();

                int countGastos = 0;
                int countFijos = 0;

                for (DataSnapshot gastoSnap : snapshot.child("gastos").getChildren()) {
                    Gasto g = gastoSnap.getValue(Gasto.class);
                    if (g != null) {
                        gastoDao.insertar(g);
                        countGastos++;
                    }
                }

                for (DataSnapshot fijoSnap : snapshot.child("gastos_fijos").getChildren()) {
                    GastoFijo gf = fijoSnap.getValue(GastoFijo.class);
                    if (gf != null) {
                        gastoFijoDao.insertar(gf);
                        countFijos++;
                    }
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

                Toast.makeText(context, "Backup restaurado: " + countGastos + " gastos y " + countFijos + " fijos", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error al restaurar backup: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

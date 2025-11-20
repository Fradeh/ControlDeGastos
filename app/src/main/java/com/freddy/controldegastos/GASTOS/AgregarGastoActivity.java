package com.freddy.controldegastos.GASTOS;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.R;

import java.util.Calendar;

public class AgregarGastoActivity extends AppCompatActivity {

    private EditText edtDescripcion, edtMonto, edtFecha;
    private Spinner spinnerCategoria;
    private Button btnGuardarGasto, btnCancelar;
    private TextView txtTituloGasto;

    private boolean modoEdicion = false;
    private boolean modoIngreso = false; // ← clave
    private int gastoId = -1;
    private GastoDao gastoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_gasto);

        edtDescripcion = findViewById(R.id.edtDescripcion);
        edtMonto = findViewById(R.id.edtMonto);
        edtFecha = findViewById(R.id.edtFecha);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        btnGuardarGasto = findViewById(R.id.btnGuardarGasto);
        btnCancelar = findViewById(R.id.btnCancelar);
        txtTituloGasto = findViewById(R.id.txtTituloGasto);

        gastoDao = AppDatabase.obtenerInstancia(this).gastoDao();

        // Spinner de categorías
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categorias_gasto, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        // Fecha por defecto = hoy
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String fechaHoy = String.format("%04d-%02d-%02d", year, month + 1, day);
        edtFecha.setText(fechaHoy);

        edtFecha.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
                String fecha = String.format("%04d-%02d-%02d", y, m + 1, d);
                edtFecha.setText(fecha);
            }, year, month, day);
            dpd.show();
        });

        // ----- Detectar modos (edición / ingreso) -----
        Intent intent = getIntent();
        if (intent != null) {
            modoIngreso = intent.getBooleanExtra("modo_ingreso", false);

            if (intent.getBooleanExtra("modo_edicion", false)) {
                modoEdicion = true;
                gastoId = intent.getIntExtra("id", -1);

                // Prefill desde el intent (si vienen)
                edtDescripcion.setText(intent.getStringExtra("descripcion"));
                edtMonto.setText(String.valueOf(intent.getDoubleExtra("monto", 0)));
                String fechaIntent = intent.getStringExtra("fecha");
                if (fechaIntent != null && !fechaIntent.isEmpty()) {
                    edtFecha.setText(fechaIntent);
                }

                String categoria = intent.getStringExtra("categoria");
                if (categoria == null) categoria = "";
                int pos = adapter.getPosition(categoria);
                if (pos >= 0) spinnerCategoria.setSelection(pos);

                // Si NO nos mandaron modo_ingreso en el intent, lo inferimos desde la BD
                if (!intent.hasExtra("modo_ingreso") && gastoId != -1) {
                    Gasto existente = gastoDao.obtenerPorId(gastoId);
                    if (existente != null) {
                        modoIngreso = existente.isEsIngreso();
                    }
                }
            }
        }

        // UI según modo ingreso
        aplicarUIporModo();

        btnCancelar.setOnClickListener(v -> finish());

        btnGuardarGasto.setOnClickListener(v -> guardar());
    }

    private void aplicarUIporModo() {
        if (modoIngreso) {
            setTitle(modoEdicion ? "Editar Ingreso" : "Nuevo Ingreso");
            txtTituloGasto.setText(modoEdicion ? "Editar Ingreso" : "Nuevo Ingreso");
            spinnerCategoria.setVisibility(View.GONE);
        } else {
            setTitle(modoEdicion ? "Editar Gasto" : "Nuevo Gasto");
            txtTituloGasto.setText(modoEdicion ? "Editar Gasto" : "Nuevo Gasto");
            spinnerCategoria.setVisibility(View.VISIBLE);
        }
    }

    private void guardar() {
        String descripcion = edtDescripcion.getText().toString().trim();
        String montoStr = edtMonto.getText().toString().trim();
        String fecha = edtFecha.getText().toString().trim();
        String categoria = modoIngreso ? "Ingreso extra" : (spinnerCategoria.getSelectedItem() != null
                ? spinnerCategoria.getSelectedItem().toString() : "");

        if (descripcion.isEmpty() || montoStr.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(this, "¡Todos los campos son obligatorios!", Toast.LENGTH_SHORT).show();
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modoEdicion && gastoId != -1) {
            Gasto gastoEditado = new Gasto(descripcion, monto, fecha, categoria);
            gastoEditado.setId(gastoId);
            gastoEditado.setEsIngreso(modoIngreso); // ← clave
            gastoDao.actualizar(gastoEditado);
            Toast.makeText(this, modoIngreso ? "Ingreso actualizado" : "Gasto actualizado", Toast.LENGTH_SHORT).show();
        } else {
            Gasto nuevo = new Gasto(descripcion, monto, fecha, categoria);
            nuevo.setEsIngreso(modoIngreso); // ← clave
            gastoDao.insertar(nuevo);
            Toast.makeText(this, modoIngreso ? "Ingreso guardado" : "Gasto guardado", Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        finish();
    }
}

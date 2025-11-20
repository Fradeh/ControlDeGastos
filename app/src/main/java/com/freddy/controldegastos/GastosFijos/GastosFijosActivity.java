package com.freddy.controldegastos.GastosFijos;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.app.AlertDialog;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.BD.GastoFijoDao;
import com.freddy.controldegastos.R;
import java.util.Locale;

import java.util.List;

public class GastosFijosActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFijos;
    private List<GastoFijo> listaGastosFijos;
    private GastoFijoAdapterRecycler gastoFijoAdapter;
    private TextView txtSaldoFijos;
    private SharedPreferences prefs;
    private EditText edtNombreFijo, edtMontoFijo;
    private Button btnAgregarFijo, btnAtras;
    private GastoFijoDao gastoFijoDao;

    private void actualizarSaldoFijos() {
        float ingresoMensual = prefs.getFloat("ingreso_mensual", 0f);

        GastoDao gastoDao = AppDatabase.obtenerInstancia(this).gastoDao();
        double totalGastosNormales = gastoDao.sumaGastos();   // solo gastos
        double ingresosExtra = gastoDao.sumaIngresos();       // solo ingresos

        double totalFijosPagados = 0;
        for (GastoFijo g : listaGastosFijos) {
            if (g.isPagado()) totalFijosPagados += g.getMonto();
        }

        double saldo = ingresoMensual + ingresosExtra - (totalGastosNormales + totalFijosPagados);
        txtSaldoFijos.setText("Saldo disponible: $" + String.format(Locale.getDefault(),"%.2f", saldo));
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gastos_fijos);

        txtSaldoFijos = findViewById(R.id.txtSaldoFijos);
        prefs = getSharedPreferences("mis_datos", MODE_PRIVATE);

        recyclerViewFijos = findViewById(R.id.recyclerViewFijos);
        edtNombreFijo = findViewById(R.id.edtNombreFijo);
        edtMontoFijo = findViewById(R.id.edtMontoFijo);
        btnAgregarFijo = findViewById(R.id.btnAgregarFijo);
        btnAtras = findViewById(R.id.btnAtras);

        btnAtras.setOnClickListener(v -> finish());

        gastoFijoDao = AppDatabase.obtenerInstancia(this).gastoFijoDao();
        listaGastosFijos = gastoFijoDao.obtenerTodos();

        gastoFijoAdapter = new GastoFijoAdapterRecycler(listaGastosFijos, () -> {
            for (GastoFijo g : listaGastosFijos) {
                gastoFijoDao.actualizar(g);
            }
            actualizarSaldoFijos();
        });

        recyclerViewFijos.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFijos.setAdapter(gastoFijoAdapter);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private int recentlySwipedPosition = -1;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                recentlySwipedPosition = viewHolder.getAdapterPosition();
                new AlertDialog.Builder(GastosFijosActivity.this)
                        .setTitle("Eliminar gasto fijo")
                        .setMessage("¿Seguro que deseas eliminar este gasto fijo?")
                        .setPositiveButton("Borrar", (dialog, which) -> {
                            GastoFijo eliminado = listaGastosFijos.remove(recentlySwipedPosition);
                            gastoFijoDao.eliminar(eliminado);
                            gastoFijoAdapter.notifyItemRemoved(recentlySwipedPosition);
                            actualizarSaldoFijos();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> {
                            gastoFijoAdapter.notifyItemChanged(recentlySwipedPosition);
                            recentlySwipedPosition = -1;
                        })
                        .setCancelable(false)
                        .show();
            }
        };
        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerViewFijos);

        btnAgregarFijo.setOnClickListener(v -> {
            String nombre = edtNombreFijo.getText().toString().trim();
            String montoStr = edtMontoFijo.getText().toString().trim();

            if (!nombre.isEmpty() && !montoStr.isEmpty()) {
                try {
                    double monto = Double.parseDouble(montoStr);
                    GastoFijo nuevo = new GastoFijo(nombre, monto, "Sin categoría", false);
                    gastoFijoDao.insertar(nuevo);
                    listaGastosFijos.clear();
                    listaGastosFijos.addAll(gastoFijoDao.obtenerTodos());
                    gastoFijoAdapter.notifyDataSetChanged();
                    edtNombreFijo.setText("");
                    edtMontoFijo.setText("");
                } catch (NumberFormatException e) {
                    edtMontoFijo.setError("Monto inválido");
                }
            } else {
                if (nombre.isEmpty()) edtNombreFijo.setError("Requerido");
                if (montoStr.isEmpty()) edtMontoFijo.setError("Requerido");
            }
            actualizarSaldoFijos();
        });

        actualizarSaldoFijos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listaGastosFijos.clear();
        listaGastosFijos.addAll(gastoFijoDao.obtenerTodos());
        gastoFijoAdapter.notifyDataSetChanged();
        actualizarSaldoFijos();
    }
}

    package com.freddy.controldegastos.GastosFijos;

    import android.app.AlertDialog;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.widget.TextView;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.ItemTouchHelper;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.freddy.controldegastos.BD.AppDatabase;
    import com.freddy.controldegastos.GASTOS.Gasto;
    import com.freddy.controldegastos.GASTOS.GastoAdapterRecycler;
    import com.freddy.controldegastos.BD.GastoDao;
    import com.freddy.controldegastos.R;

    import java.util.List;

    public class GastosActivity extends AppCompatActivity {

        private RecyclerView recyclerViewGastos;
        private List<Gasto> listaGastos;
        private GastoAdapterRecycler gastoAdapter;
        private SharedPreferences prefs;
        private TextView txtSaldo;
        private GastoDao gastoDao;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_gastos); // Asegúrate de tener este layout

            txtSaldo = findViewById(R.id.txtSaldo);
            recyclerViewGastos = findViewById(R.id.recyclerViewGastos);
            prefs = getSharedPreferences("mis_datos", MODE_PRIVATE);

            // Obtener instancia de Room y cargar lista de gastos
            gastoDao = AppDatabase.obtenerInstancia(this).gastoDao();
            listaGastos = gastoDao.obtenerTodos();

            // Adaptador para RecyclerView
            gastoAdapter = new GastoAdapterRecycler(listaGastos, null);
            recyclerViewGastos.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewGastos.setAdapter(gastoAdapter);

            // Swipe para eliminar con confirmación
            ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                private int pos = -1;

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    pos = viewHolder.getAdapterPosition();

                    new AlertDialog.Builder(GastosActivity.this)
                            .setTitle("Eliminar gasto")
                            .setMessage("¿Seguro que deseas eliminar este gasto?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                Gasto eliminado = listaGastos.get(pos);
                                gastoDao.eliminar(eliminado);

                                // Recargar lista desde base de datos
                                listaGastos.clear();
                                listaGastos.addAll(gastoDao.obtenerTodos());
                                gastoAdapter.notifyDataSetChanged();
                                actualizarSaldo();
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                gastoAdapter.notifyItemChanged(pos);
                            })
                            .setCancelable(false)
                            .show();
                }
            };

            new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerViewGastos);

            // Mostrar saldo
            actualizarSaldo();
        }

        private void actualizarSaldo() {
            float ingreso = prefs.getFloat("ingreso_mensual", 0f);
            double total = 0;
            for (Gasto g : listaGastos) {
                total += g.getMonto();
            }
            double saldo = ingreso - total;
            txtSaldo.setText("Saldo disponible: $" + String.format("%.2f", saldo));
        }
    }

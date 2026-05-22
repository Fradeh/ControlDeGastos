package com.freddy.controldegastos.GASTOS;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.BD.GastoFijoDao;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.freddy.controldegastos.R;
import com.freddy.controldegastos.UTILS.GraficasUtils;
import com.github.mikephil.charting.charts.PieChart;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GraficasActivity extends AppCompatActivity {

    private PieChart pieChart;          // Distribución de gastos por categoría
    private LinearLayout categoryRankingList;
    private LinearLayout monthlyBalancePanel;
    private GastoDao gastoDao;
    private GastoFijoDao gastoFijoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficas);

        pieChart       = findViewById(R.id.pieChartCategorias);
        categoryRankingList = findViewById(R.id.categoryRankingList);
        monthlyBalancePanel = findViewById(R.id.monthlyBalancePanel);

        ImageButton btnAtras = findViewById(R.id.btnAtras);
        btnAtras.setOnClickListener(v -> finish());

        AppDatabase db = AppDatabase.obtenerInstancia(this);
        gastoDao      = db.gastoDao();
        gastoFijoDao  = db.gastoFijoDao();

        cargarGraficas();
    }

    private void cargarGraficas() {
        ExecutorService io = Executors.newSingleThreadExecutor();
        io.execute(() -> {
            List<Gasto> gastos            = gastoDao.obtenerTodos();
            List<GastoFijo> fijos         = gastoFijoDao.obtenerTodos();
            double ingresoMensual         = getSharedPreferences("mis_datos", MODE_PRIVATE).getFloat("ingreso_mensual", 0f);
            boolean soloPagadosFijos      = true; // igual que tu saldo disponible

            runOnUiThread(() -> {
                // 1) Configura el donut y el ranking legible de categorías.
                GraficasUtils.configurarResumenCategorias(
                        pieChart,
                        categoryRankingList,
                        gastos,
                        fijos,
                        soloPagadosFijos
                );

                // 2) Panel comparativo mensual: ingresos extras vs gastos.
                GraficasUtils.configurarPanelIngresosVsGastos(
                        monthlyBalancePanel,
                        gastos,
                        fijos,
                        true // incluir gastos fijos en el mes actual; pon false si no quieres
                );

                // 3) Oculta gráficos sin datos
                int vacias = 0;
                if (pieChart.getData() == null || pieChart.getData().getEntryCount() == 0) { pieChart.setVisibility(android.view.View.GONE); vacias++; }
                if (categoryRankingList.getChildCount() == 0) { categoryRankingList.setVisibility(android.view.View.GONE); vacias++; }
                if (monthlyBalancePanel.getChildCount() == 0) { monthlyBalancePanel.setVisibility(android.view.View.GONE); vacias++; }
                if (vacias == 3) {
                    Toast.makeText(this, "Sin datos suficientes para mostrar gráficas", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}

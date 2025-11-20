package com.freddy.controldegastos.GASTOS;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.freddy.controldegastos.BD.AppDatabase;
import com.freddy.controldegastos.BD.GastoDao;
import com.freddy.controldegastos.BD.GastoFijoDao;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.freddy.controldegastos.R;
import com.freddy.controldegastos.UTILS.GraficasUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart; // ← usamos un LineChart “dummy” para setupCharts
import com.github.mikephil.charting.charts.PieChart;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GraficasActivity extends AppCompatActivity {

    private PieChart pieChart;          // Distribución de gastos por categoría
    private BarChart barChartTop;       // Top categorías por gasto
    private BarChart barChartMensual;   // Ingresos vs Gastos por mes (antes era LineChart en XML)
    private GastoDao gastoDao;
    private GastoFijoDao gastoFijoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficas);

        pieChart       = findViewById(R.id.pieChartCategorias);
        barChartTop    = findViewById(R.id.barChartCategorias);
        barChartMensual= findViewById(R.id.lineChartCategorias); // mismo id, ahora es BarChart

        Button btnAtras = findViewById(R.id.btnAtras);
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
                // 1) Configura Pie + Bar (top categorías) usando tu utilidad
                //    setupCharts exige un LineChart, así que le pasamos uno “dummy” que no está en el layout.
                LineChart dummy = new LineChart(this);
                GraficasUtils.setupCharts(pieChart, barChartTop, dummy, gastos, fijos, ingresoMensual, soloPagadosFijos);

                // 2) El tercer gráfico ahora es Barras agrupadas: Ingresos vs Gastos por mes
                GraficasUtils.configurarBarIngresosVsGastosPorMes(
                        barChartMensual,
                        gastos,
                        fijos,
                        true // incluir gastos fijos en el mes actual; pon false si no quieres
                );

                // 3) Oculta gráficos sin datos
                int vacias = 0;
                if (pieChart.getData() == null || pieChart.getData().getEntryCount() == 0) { pieChart.setVisibility(android.view.View.GONE); vacias++; }
                if (barChartTop.getData() == null || barChartTop.getData().getEntryCount() == 0) { barChartTop.setVisibility(android.view.View.GONE); vacias++; }
                if (barChartMensual.getData() == null || barChartMensual.getData().getEntryCount() == 0) { barChartMensual.setVisibility(android.view.View.GONE); vacias++; }
                if (vacias == 3) {
                    Toast.makeText(this, "Sin datos suficientes para mostrar gráficas", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}

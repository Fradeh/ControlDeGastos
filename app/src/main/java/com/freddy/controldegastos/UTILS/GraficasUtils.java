package com.freddy.controldegastos.UTILS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utils para configurar y renderizar gráficas (MPAndroidChart) con propósito:
 * - Pie: distribución de GASTOS por categoría (ingresos excluidos, fijos como “Gasto fijo”).
 * - Bar: top categorías por gasto.
 * - Line: saldo acumulado por fecha.
 */
public class GraficasUtils {

    // ==========================
    // Configuración EN PANTALLA
    // ==========================
    public static void setupCharts(
            PieChart pieChart,
            BarChart barChart,
            LineChart lineChart,
            List<Gasto> gastos,
            List<GastoFijo> fijos,
            double ingresoMensual,
            boolean soloPagadosFijos
    ) {
        Map<String, Float> totalesGastoPorCategoria = agruparSoloGastosPorCategoria(gastos);
        sumarFijosEnCategoria(totalesGastoPorCategoria, fijos, soloPagadosFijos);

        configurarPieChart(pieChart, totalesGastoPorCategoria);
        configurarBarChart(barChart, totalesGastoPorCategoria);
        configurarLineChart(lineChart, gastos, fijos, ingresoMensual, soloPagadosFijos);
    }

    // ==========================
// PIE CHART
// ==========================
    private static void configurarPieChart(PieChart chart, Map<String, Float> totales) {
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(false);

        // Mostramos el texto con nuestro ValueFormatter (no las entry labels por defecto)
        chart.setDrawEntryLabels(false);

        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(60f);
        chart.setCenterText("Distribución\nde gastos");
        chart.setCenterTextSize(12f);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(Color.GRAY);

        // Evita que se corten etiquetas fuera del pastel
        chart.setExtraOffsets(8f, 8f, 8f, 16f);

        if (totales.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> e : totales.entrySet()) {
            entries.add(new PieEntry(e.getValue(), e.getKey())); // label = categoría
        }

        // Valor de "Gasto fijo" para poder identificar su porción aunque venga sin label
        final float fijoVal = totales.containsKey("Gasto fijo") ? totales.get("Gasto fijo") : Float.NaN;

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setSliceSpace(2f);

        // Etiquetas fuera de la porción con línea guía
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        set.setValueLineColor(Color.DKGRAY);
        set.setValueLinePart1Length(0.55f);
        set.setValueLinePart2Length(0.45f);

        set.setValueTextSize(11f);
        set.setValueTextColor(Color.DKGRAY);

        // ⬇️ Asegura mostrar "Gasto fijo" aunque el label venga vacío
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry entry) {
                String label = entry.getLabel();
                if (label == null || label.trim().isEmpty()) {
                    if (!Float.isNaN(fijoVal) && Math.abs(value - fijoVal) < 0.0001f) {
                        label = "Gasto fijo";
                    } else {
                        label = "Otros";
                    }
                }
                return String.format(Locale.getDefault(), "%s\n$%.2f", label, value);
            }
        });

        PieData data = new PieData(set);
        chart.setData(data);

        // Leyenda abajo, con wrap
        Legend l = chart.getLegend();
        l.setEnabled(true);
        l.setWordWrapEnabled(true);
        l.setTextSize(12f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        chart.animateY(800);
        chart.invalidate();
    }



    // ==========================
    // BAR CHART (Top categorías)
    // ==========================
    private static void configurarBarChart(BarChart chart, Map<String, Float> totales) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(Color.GRAY);

        if (totales.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        // Ordenamos por gasto desc y top 6
        List<Map.Entry<String, Float>> lista = new ArrayList<>(totales.entrySet());
        Collections.sort(lista, (a, b) -> Float.compare(b.getValue(), a.getValue()));
        if (lista.size() > 6) lista = lista.subList(0, 6);

        ArrayList<BarEntry> entries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            entries.add(new BarEntry(i, lista.get(i).getValue()));
            labels.add(lista.get(i).getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Top categorías");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new CurrencyFormatter());

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setGranularity(1f);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setTextSize(11f);

        chart.getAxisLeft().setValueFormatter(new CurrencyFormatter());
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        chart.animateY(800);
        chart.invalidate();
    }

    // ==========================
// LINE CHART (Gasto diario + Saldo acumulado)
// ==========================
    private static void configurarLineChart(LineChart chart, List<Gasto> gastos,
                                            List<GastoFijo> fijos,
                                            double ingresoMensual,
                                            boolean soloPagadosFijos) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(Color.GRAY);

        // Mapa fecha -> delta diario (ingreso +, gasto -)
        Map<String, Double> deltaPorFecha = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 1) variables (gastos/ingresos)
        for (Gasto g : gastos) {
            String fecha = g.getFecha();
            if (fecha == null || fecha.isEmpty()) continue;

            boolean esIngreso = false;
            try { esIngreso = g.isEsIngreso(); } catch (Exception ignored) {}
            double monto = g.getMonto();

            deltaPorFecha.put(fecha, deltaPorFecha.getOrDefault(fecha, 0d) + (esIngreso ? +monto : -monto));
        }

        // Si no hay fechas, añadimos al menos "hoy" para que el gráfico exista
        if (deltaPorFecha.isEmpty()) {
            String hoy = sdf.format(new java.util.Date());
            deltaPorFecha.put(hoy, 0d);
        }

        // 2) fijos (restan) en la ÚLTIMA fecha disponible (o hoy)
        String ultimaFecha = null;
        for (String f : deltaPorFecha.keySet()) ultimaFecha = f; // último insertado (aun no ordenado)
        if (ultimaFecha == null) ultimaFecha = sdf.format(new java.util.Date());

        double totalFijos = 0d;
        if (fijos != null) {
            for (GastoFijo gf : fijos) {
                if (soloPagadosFijos && !gf.isPagado()) continue;
                totalFijos += gf.getMonto();
            }
        }
        if (totalFijos != 0d) {
            deltaPorFecha.put(ultimaFecha, deltaPorFecha.getOrDefault(ultimaFecha, 0d) - totalFijos);
        }

        // Orden por fecha asc
        List<Map.Entry<String, Double>> orden = new ArrayList<>(deltaPorFecha.entrySet());
        Collections.sort(orden, Comparator.comparing(e -> parseDate(e.getKey())));

        // Construimos 2 series: gasto del día y saldo acumulado
        final ArrayList<String> labels = new ArrayList<>();
        ArrayList<Entry> puntosSaldo = new ArrayList<>();
        ArrayList<Entry> puntosGasto = new ArrayList<>();

        double saldo = ingresoMensual;
        int i = 0;
        for (Map.Entry<String, Double> e : orden) {
            String fecha = e.getKey();
            double delta = e.getValue();

            // Serie "Gasto del día" (muestra solo la parte negativa como gasto visual)
            double soloGasto = Math.min(0d, delta); // si delta fue ingreso, no baja
            puntosGasto.add(new Entry(i, (float) Math.abs(soloGasto)));

            // Saldo acumulado (ingreso +, gasto -)
            saldo += delta;
            puntosSaldo.add(new Entry(i, (float) saldo));

            labels.add(fecha);
            i++;
        }

        // Serie 1: Saldo acumulado (línea suave con relleno)
        LineDataSet setSaldo = new LineDataSet(puntosSaldo, "Saldo acumulado");
        setSaldo.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        setSaldo.setLineWidth(2.2f);
        setSaldo.setColor(ColorTemplate.MATERIAL_COLORS[0]);
        setSaldo.setCircleColor(ColorTemplate.MATERIAL_COLORS[0]);
        setSaldo.setCircleRadius(3.2f);
        setSaldo.setDrawFilled(true);
        setSaldo.setFillColor(ColorTemplate.MATERIAL_COLORS[0]);
        setSaldo.setDrawValues(true);
        setSaldo.setValueFormatter(new CurrencyFormatter());

        // Serie 2: Gasto del día (línea fina)
        LineDataSet setGastoDia = new LineDataSet(puntosGasto, "Gasto del día");
        setGastoDia.setMode(LineDataSet.Mode.LINEAR);
        setGastoDia.setLineWidth(1.6f);
        setGastoDia.setColor(ColorTemplate.MATERIAL_COLORS[2]);
        setGastoDia.setCircleColor(ColorTemplate.MATERIAL_COLORS[2]);
        setGastoDia.setCircleRadius(2.6f);
        setGastoDia.setDrawFilled(false);
        setGastoDia.setDrawValues(true);
        setGastoDia.setValueFormatter(new CurrencyFormatter());

        LineData data = new LineData(setSaldo, setGastoDia);
        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setGranularity(1f);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setTextSize(11f);
        x.setLabelRotationAngle(labels.size() > 5 ? -30f : 0f);

        chart.getAxisLeft().setValueFormatter(new CurrencyFormatter());
        chart.getAxisRight().setEnabled(false);

        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);

        chart.animateX(800);
        chart.invalidate();
    }
    // ==========================
    // Helpers de datos
    // ==========================
    private static Map<String, Float> agruparSoloGastosPorCategoria(List<Gasto> gastos) {
        Map<String, Float> totales = new LinkedHashMap<>();
        if (gastos == null) return totales;

        for (Gasto g : gastos) {
            boolean esIngreso = false;
            try { esIngreso = g.isEsIngreso(); } catch (Exception ignored) {}
            if (esIngreso) continue; // no mezclar ingresos

            String cat = g.getCategoria() == null ? "" : g.getCategoria().trim();
            if (cat.isEmpty()) cat = "Otros";

            float acum = totales.containsKey(cat) ? totales.get(cat) : 0f;
            totales.put(cat, acum + (float) g.getMonto());
        }
        return totales;
    }

    private static void sumarFijosEnCategoria(Map<String, Float> totales, List<GastoFijo> fijos, boolean soloPagados) {
        if (fijos == null) return;
        float sumaFijos = 0f;
        for (GastoFijo gf : fijos) {
            if (soloPagados && !gf.isPagado()) continue;
            sumaFijos += (float) gf.getMonto();
        }
        if (sumaFijos > 0f) {
            float actual = totales.containsKey("Gasto fijo") ? totales.get("Gasto fijo") : 0f;
            totales.put("Gasto fijo", actual + sumaFijos);
        }
    }

    private static java.util.Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s);
        } catch (ParseException e) {
            return new java.util.Date(0);
        }
    }

    // ==========================
    // Formatters
    // ==========================
    private static class CurrencyFormatter extends ValueFormatter {
        @Override public String getFormattedValue(float value) {
            return String.format(Locale.getDefault(), "$%.2f", value);
        }
    }

    private static class IndexAxisValueFormatter extends ValueFormatter {
        private final List<String> labels;
        IndexAxisValueFormatter(List<String> labels) { this.labels = labels; }
        @Override public String getAxisLabel(float value, AxisBase axis) {
            int i = (int) value;
            if (i >= 0 && i < labels.size()) return labels.get(i);
            return "";
        }
    }

    // ==========================
    // RENDER A BITMAP (para PDF)
    // ==========================
    public static Bitmap renderPieToBitmap(Context ctx, Map<String, Float> totales, int w, int h) {
        PieChart chart = new PieChart(ctx);
        chart.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        configurarPieChart(chart, totales);
        return drawToBitmap(chart, w, h);
    }

    public static Bitmap renderBarToBitmap(Context ctx, Map<String, Float> totales, int w, int h) {
        BarChart chart = new BarChart(ctx);
        chart.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        configurarBarChart(chart, totales);
        return drawToBitmap(chart, w, h);
    }

    public static Bitmap renderLineSaldoToBitmap(Context ctx, List<Gasto> gastos, List<GastoFijo> fijos,
                                                 double ingresoMensual, boolean soloPagados, int w, int h) {
        LineChart chart = new LineChart(ctx);
        chart.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        configurarLineChart(chart, gastos, fijos, ingresoMensual, soloPagados);
        return drawToBitmap(chart, w, h);
    }

    private static Bitmap drawToBitmap(View chart, int w, int h) {
        chart.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        chart.layout(0, 0, w, h);

        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        chart.draw(canvas);
        return bm;
    }

    // ==========================
    // WRAPPERS para mantener compatibilidad con MainActivity (PDF)
    // ==========================
    public static Bitmap generarGraficoPastel(Context context, List<Gasto> gastos) {
        Map<String, Float> totales = agruparSoloGastosPorCategoria(gastos);
        // sin fijos en el PDF por compatibilidad; si quieres, súmalos aquí
        return renderPieToBitmap(context, totales, 800, 600);
    }

    public static Bitmap generarGraficoBarras(Context context, List<Gasto> gastos) {
        Map<String, Float> totales = agruparSoloGastosPorCategoria(gastos);
        return renderBarToBitmap(context, totales, 800, 600);
    }

    public static void configurarBarIngresosVsGastosPorMes(BarChart chart,
                                                           List<com.freddy.controldegastos.GASTOS.Gasto> gastos,
                                                           List<com.freddy.controldegastos.GastosFijos.GastoFijo> fijos,
                                                           boolean incluirFijosEnMesActual) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(Color.GRAY);

        LinkedHashMap<String, Float> ingresosMes = new LinkedHashMap<>();
        LinkedHashMap<String, Float> gastosMes   = new LinkedHashMap<>();

        SimpleDateFormat sdfIn  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        if (gastos != null) {
            for (com.freddy.controldegastos.GASTOS.Gasto g : gastos) {
                String f = g.getFecha();
                if (f == null || f.isEmpty()) continue;
                String mes;
                try { mes = sdfOut.format(sdfIn.parse(f)); } catch (Exception e) { mes = f.substring(0, Math.min(7, f.length())); }

                boolean esIngreso = false;
                try { esIngreso = g.isEsIngreso(); } catch (Exception ignored) {}

                if (esIngreso) {
                    ingresosMes.put(mes, ingresosMes.getOrDefault(mes, 0f) + (float) g.getMonto());
                } else {
                    gastosMes.put(mes, gastosMes.getOrDefault(mes, 0f) + (float) g.getMonto());
                }
            }
        }

        if (incluirFijosEnMesActual && fijos != null && !fijos.isEmpty()) {
            String mesActual = sdfOut.format(new java.util.Date());
            float sumaFijos = 0f;
            for (com.freddy.controldegastos.GastosFijos.GastoFijo gf : fijos) {
                sumaFijos += (float) gf.getMonto();
            }
            gastosMes.put(mesActual, gastosMes.getOrDefault(mesActual, 0f) + sumaFijos);
        }

        ArrayList<String> meses = new ArrayList<>(new java.util.TreeSet<>(mesesUnion(ingresosMes, gastosMes)));
        if (meses.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        ArrayList<BarEntry> entradasIngresos = new ArrayList<>();
        ArrayList<BarEntry> entradasGastos   = new ArrayList<>();
        for (int i = 0; i < meses.size(); i++) {
            String m = meses.get(i);
            entradasIngresos.add(new BarEntry(i, ingresosMes.getOrDefault(m, 0f)));
            entradasGastos.add(new BarEntry(i,   gastosMes.getOrDefault(m, 0f)));
        }

        BarDataSet dsIngresos = new BarDataSet(entradasIngresos, "Ingresos Extras");
        dsIngresos.setColor(Color.parseColor("#4CAF50"));
        dsIngresos.setValueTextSize(11f);
        dsIngresos.setValueFormatter(new CurrencyFormatter());

        BarDataSet dsGastos = new BarDataSet(entradasGastos, "Gastos");
        dsGastos.setColor(Color.parseColor("#F44336"));
        dsGastos.setValueTextSize(11f);
        dsGastos.setValueFormatter(new CurrencyFormatter());

        BarData data = new BarData(dsIngresos, dsGastos);
        float groupSpace = 0.20f, barSpace = 0.02f, barWidth = 0.38f;
        data.setBarWidth(barWidth);

        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setCenterAxisLabels(true);
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getAxisLabel(float value, AxisBase axis) {
                int i = (int) value;
                return (i >= 0 && i < meses.size()) ? meses.get(i) : "";
            }
        });

        chart.getAxisLeft().setValueFormatter(new CurrencyFormatter());
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);

        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(0f + data.getGroupWidth(groupSpace, barSpace) * meses.size());
        chart.groupBars(0f, groupSpace, barSpace);

        chart.animateY(800);
        chart.invalidate();
    }

    private static java.util.Set<String> mesesUnion(Map<String, Float> a, Map<String, Float> b) {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
        s.addAll(a.keySet());
        s.addAll(b.keySet());
        return s;
    }

}

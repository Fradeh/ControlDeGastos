package com.freddy.controldegastos.UTILS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
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
    private static final int COLOR_TEXT = Color.parseColor("#27313F");
    private static final int COLOR_MUTED = Color.parseColor("#6E7781");
    private static final int COLOR_GRID = Color.parseColor("#EEE7F0");
    private static final int COLOR_GREEN = Color.parseColor("#2ECC71");
    private static final int COLOR_RED = Color.parseColor("#FF4D45");
    private static final int[] CATEGORY_COLORS = new int[] {
            Color.parseColor("#2ECC71"),
            Color.parseColor("#2F80ED"),
            Color.parseColor("#F2C94C"),
            Color.parseColor("#EB5757"),
            Color.parseColor("#9B51E0"),
            Color.parseColor("#56CCF2")
    };

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

    public static void configurarResumenCategorias(
            PieChart pieChart,
            LinearLayout rankingContainer,
            List<Gasto> gastos,
            List<GastoFijo> fijos,
            boolean soloPagadosFijos
    ) {
        Map<String, Float> totalesGastoPorCategoria = agruparSoloGastosPorCategoria(gastos);
        sumarFijosEnCategoria(totalesGastoPorCategoria, fijos, soloPagadosFijos);

        configurarPieChart(pieChart, totalesGastoPorCategoria);
        configurarRankingCategorias(rankingContainer, totalesGastoPorCategoria);
    }

    // ==========================
// PIE CHART
// ==========================
    private static void configurarPieChart(PieChart chart, Map<String, Float> totales) {
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(true);

        // Mostramos el texto con nuestro ValueFormatter (no las entry labels por defecto)
        chart.setDrawEntryLabels(false);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setHoleRadius(68f);
        chart.setTransparentCircleRadius(72f);
        chart.setTransparentCircleColor(COLOR_GREEN);
        chart.setTransparentCircleAlpha(22);
        chart.setCenterText("Distribución\nde gastos");
        chart.setCenterTextColor(COLOR_TEXT);
        chart.setCenterTextSize(13f);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(COLOR_MUTED);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setDrawMarkers(false);

        // Evita que se corten etiquetas fuera del pastel
        chart.setExtraOffsets(4f, 4f, 4f, 4f);

        if (totales.isEmpty()) {
            chart.clear();
            chart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        float total = 0f;
        for (Map.Entry<String, Float> e : totales.entrySet()) {
            total += e.getValue();
            entries.add(new PieEntry(e.getValue(), e.getKey())); // label = categoría
        }

        // Valor de "Gasto fijo" para poder identificar su porción aunque venga sin label
        chart.setCenterText(String.format(Locale.getDefault(), "Total\n%s", formatMoney(total)));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(CATEGORY_COLORS);
        set.setSliceSpace(3f);
        set.setSelectionShift(5f);

        // Etiquetas fuera de la porción con línea guía
        set.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        set.setDrawValues(false);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);

        // ⬇️ Asegura mostrar "Gasto fijo" aunque el label venga vacío
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry entry) {
                return value >= 6f ? String.format(Locale.getDefault(), "%.0f%%", value) : "";
            }
        });

        PieData data = new PieData(set);
        chart.setData(data);

        // Leyenda abajo, con wrap
        Legend l = chart.getLegend();
        l.setEnabled(false);
        l.setWordWrapEnabled(true);
        l.setTextSize(12.5f);
        l.setTextColor(COLOR_TEXT);
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setFormSize(8f);
        l.setXEntrySpace(12f);
        l.setYEntrySpace(6f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        chart.animateY(650);
        chart.invalidate();
    }



    // ==========================
    // BAR CHART (Top categorías)
    // ==========================
    private static void configurarRankingCategorias(LinearLayout container, Map<String, Float> totales) {
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        if (totales.isEmpty()) {
            return;
        }

        List<Map.Entry<String, Float>> lista = new ArrayList<>(totales.entrySet());
        Collections.sort(lista, (a, b) -> Float.compare(b.getValue(), a.getValue()));

        float total = 0f;
        float max = 0f;
        for (Map.Entry<String, Float> item : lista) {
            total += item.getValue();
            max = Math.max(max, item.getValue());
        }

        int maxItems = Math.min(lista.size(), 6);
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Float> item = lista.get(i);
            int color = CATEGORY_COLORS[i % CATEGORY_COLORS.length];
            float value = item.getValue();
            float percent = total == 0f ? 0f : (value / total) * 100f;
            float weight = max == 0f ? 0f : Math.max(0.035f, value / max);

            LinearLayout row = new LinearLayout(container.getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) rowParams.topMargin = dp(container, 14);
            row.setLayoutParams(rowParams);

            LinearLayout header = new LinearLayout(container.getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView dot = new TextView(container.getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(container, 10), dp(container, 10));
            dotParams.rightMargin = dp(container, 8);
            dot.setLayoutParams(dotParams);
            dot.setBackground(makeRoundDrawable(color, dp(container, 10)));

            TextView label = new TextView(container.getContext());
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(labelParams);
            label.setText(item.getKey());
            label.setTextColor(COLOR_TEXT);
            label.setTextSize(14f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setSingleLine(false);

            TextView amount = new TextView(container.getContext());
            amount.setText(String.format(Locale.getDefault(), "%s  %.0f%%", formatMoney(value), percent));
            amount.setTextColor(COLOR_MUTED);
            amount.setTextSize(13f);
            amount.setGravity(Gravity.END);

            header.addView(dot);
            header.addView(label);
            header.addView(amount);

            FrameLayout track = new FrameLayout(container.getContext());
            LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(container, 12)
            );
            trackParams.topMargin = dp(container, 8);
            track.setLayoutParams(trackParams);
            track.setBackground(makeRoundDrawable(Color.parseColor("#F1ECF4"), dp(container, 12)));

            View fill = new View(container.getContext());
            FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(
                    0,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            fillParams.gravity = Gravity.START;
            fill.setLayoutParams(fillParams);
            fill.setBackground(makeRoundDrawable(color, dp(container, 12)));
            track.addView(fill);

            row.addView(header);
            row.addView(track);
            container.addView(row);

            final float finalWeight = weight;
            track.post(() -> {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fill.getLayoutParams();
                params.width = Math.max(dp(container, 18), Math.round(track.getWidth() * finalWeight));
                fill.setLayoutParams(params);
            });
        }
    }

    private static void configurarBarChart(BarChart chart, Map<String, Float> totales) {
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("Sin datos para mostrar");
        chart.setNoDataTextColor(COLOR_MUTED);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setFitBars(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setExtraOffsets(8f, 12f, 12f, 8f);
        chart.setDrawValueAboveBar(true);

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
        dataSet.setColors(CATEGORY_COLORS);
        dataSet.setValueTextSize(10.5f);
        dataSet.setValueTextColor(COLOR_TEXT);
        dataSet.setValueFormatter(new NonZeroCurrencyFormatter());

        BarData data = new BarData(dataSet);
        data.setBarWidth(chart instanceof HorizontalBarChart ? 0.48f : (lista.size() == 1 ? 0.42f : 0.56f));
        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setGranularity(1f);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setTextSize(11f);
        x.setTextColor(COLOR_TEXT);
        x.setDrawGridLines(false);
        x.setAxisLineColor(COLOR_GRID);
        x.setLabelRotationAngle(chart instanceof HorizontalBarChart ? 0f : (labels.size() > 3 ? -20f : 0f));
        x.setLabelCount(labels.size());

        chart.getAxisLeft().setValueFormatter(new CurrencyFormatter());
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setTextColor(COLOR_MUTED);
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisLeft().setGridColor(COLOR_GRID);
        chart.getAxisLeft().setAxisLineColor(COLOR_GRID);
        chart.getAxisLeft().setGranularityEnabled(true);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        chart.animateY(650);
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
        chart.setNoDataTextColor(COLOR_MUTED);
        chart.setDrawGridBackground(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setExtraOffsets(8f, 12f, 12f, 8f);

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

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable makeRoundDrawable(int color, int radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    // ==========================
    // Formatters
    // ==========================
    private static String formatMoney(float value) {
        float abs = Math.abs(value);
        if (abs >= 1000000f) {
            return String.format(Locale.getDefault(), "$%.1fM", value / 1000000f);
        }
        if (abs >= 10000f) {
            return String.format(Locale.getDefault(), "$%.0fk", value / 1000f);
        }
        if (Math.abs(value - Math.round(value)) < 0.01f) {
            return String.format(Locale.getDefault(), "$%d", Math.round(value));
        }
        return String.format(Locale.getDefault(), "$%.2f", value);
    }

    private static class CurrencyFormatter extends ValueFormatter {
        @Override public String getFormattedValue(float value) {
            return formatMoney(value);
        }
    }

    private static class NonZeroCurrencyFormatter extends ValueFormatter {
        @Override public String getFormattedValue(float value) {
            return Math.abs(value) < 0.01f ? "" : formatMoney(value);
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
        chart.setNoDataTextColor(COLOR_MUTED);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setExtraOffsets(8f, 12f, 12f, 8f);

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
        dsIngresos.setColor(COLOR_GREEN);
        dsIngresos.setValueTextSize(10.5f);
        dsIngresos.setValueTextColor(COLOR_TEXT);
        dsIngresos.setValueFormatter(new NonZeroCurrencyFormatter());

        BarDataSet dsGastos = new BarDataSet(entradasGastos, "Gastos");
        dsGastos.setColor(COLOR_RED);
        dsGastos.setValueTextSize(10.5f);
        dsGastos.setValueTextColor(COLOR_TEXT);
        dsGastos.setValueFormatter(new NonZeroCurrencyFormatter());

        BarData data = new BarData(dsIngresos, dsGastos);
        float groupSpace = 0.20f, barSpace = 0.02f, barWidth = 0.38f;
        data.setBarWidth(barWidth);

        chart.setData(data);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f);
        x.setCenterAxisLabels(true);
        x.setTextColor(COLOR_TEXT);
        x.setTextSize(10.5f);
        x.setDrawGridLines(false);
        x.setAxisLineColor(COLOR_GRID);
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getAxisLabel(float value, AxisBase axis) {
                int i = (int) value;
                return (i >= 0 && i < meses.size()) ? meses.get(i) : "";
            }
        });

        chart.getAxisLeft().setValueFormatter(new CurrencyFormatter());
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setTextColor(COLOR_MUTED);
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisLeft().setGridColor(COLOR_GRID);
        chart.getAxisLeft().setAxisLineColor(COLOR_GRID);
        chart.getAxisLeft().setGranularityEnabled(true);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);
        chart.getLegend().setTextColor(COLOR_TEXT);
        chart.getLegend().setForm(Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(8f);
        chart.getLegend().setXEntrySpace(14f);

        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(0f + data.getGroupWidth(groupSpace, barSpace) * meses.size());
        chart.groupBars(0f, groupSpace, barSpace);

        chart.animateY(800);
        chart.invalidate();
    }

    public static void configurarPanelIngresosVsGastos(LinearLayout container,
                                                       List<com.freddy.controldegastos.GASTOS.Gasto> gastos,
                                                       List<com.freddy.controldegastos.GastosFijos.GastoFijo> fijos,
                                                       boolean incluirFijosEnMesActual) {
        container.removeAllViews();
        container.setVisibility(View.VISIBLE);

        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String mesActual = monthFormat.format(new java.util.Date());

        float ingresos = 0f;
        float gastosTotales = 0f;

        if (gastos != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (com.freddy.controldegastos.GASTOS.Gasto gasto : gastos) {
                String fecha = gasto.getFecha();
                if (fecha == null || fecha.isEmpty()) continue;

                String mes;
                try {
                    mes = monthFormat.format(dateFormat.parse(fecha));
                } catch (Exception e) {
                    mes = fecha.substring(0, Math.min(7, fecha.length()));
                }
                if (!mesActual.equals(mes)) continue;

                boolean esIngreso = false;
                try { esIngreso = gasto.isEsIngreso(); } catch (Exception ignored) {}
                if (esIngreso) {
                    ingresos += (float) gasto.getMonto();
                } else {
                    gastosTotales += (float) gasto.getMonto();
                }
            }
        }

        if (incluirFijosEnMesActual && fijos != null) {
            for (com.freddy.controldegastos.GastosFijos.GastoFijo fijo : fijos) {
                gastosTotales += (float) fijo.getMonto();
            }
        }

        if (ingresos == 0f && gastosTotales == 0f) {
            return;
        }

        float balance = ingresos - gastosTotales;
        float max = Math.max(ingresos, gastosTotales);

        LinearLayout summary = new LinearLayout(container.getContext());
        summary.setOrientation(LinearLayout.VERTICAL);
        summary.setGravity(Gravity.CENTER_HORIZONTAL);
        summary.setPadding(0, 0, 0, dp(container, 16));

        TextView month = new TextView(container.getContext());
        month.setText(mesActual);
        month.setTextColor(COLOR_MUTED);
        month.setTextSize(13f);
        month.setGravity(Gravity.CENTER);

        TextView balanceText = new TextView(container.getContext());
        balanceText.setText(formatMoney(balance));
        balanceText.setTextColor(balance >= 0f ? COLOR_GREEN : COLOR_RED);
        balanceText.setTextSize(28f);
        balanceText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        balanceText.setGravity(Gravity.CENTER);

        TextView balanceLabel = new TextView(container.getContext());
        balanceLabel.setText(balance >= 0f ? "Balance positivo" : "Balance negativo");
        balanceLabel.setTextColor(COLOR_TEXT);
        balanceLabel.setTextSize(14f);
        balanceLabel.setGravity(Gravity.CENTER);

        summary.addView(month);
        summary.addView(balanceText);
        summary.addView(balanceLabel);
        container.addView(summary);

        addMonthlyMetricRow(container, "Ingresos extras", ingresos, max, COLOR_GREEN);
        addMonthlyMetricRow(container, "Gastos", gastosTotales, max, COLOR_RED);

        TextView hint = new TextView(container.getContext());
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.topMargin = dp(container, 14);
        hint.setLayoutParams(hintParams);
        hint.setText(gastosTotales > ingresos
                ? "Los gastos superan los ingresos extra este mes."
                : "Los ingresos extra cubren los gastos registrados este mes.");
        hint.setTextColor(COLOR_MUTED);
        hint.setTextSize(13f);
        hint.setGravity(Gravity.CENTER);
        container.addView(hint);
    }

    private static void addMonthlyMetricRow(LinearLayout container, String labelText, float value, float max, int color) {
        LinearLayout row = new LinearLayout(container.getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(container, 12);
        row.setLayoutParams(rowParams);

        LinearLayout header = new LinearLayout(container.getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = new TextView(container.getContext());
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        label.setText(labelText);
        label.setTextColor(COLOR_TEXT);
        label.setTextSize(14f);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView amount = new TextView(container.getContext());
        amount.setText(formatMoney(value));
        amount.setTextColor(COLOR_TEXT);
        amount.setTextSize(14f);
        amount.setGravity(Gravity.END);

        header.addView(label);
        header.addView(amount);

        FrameLayout track = new FrameLayout(container.getContext());
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(container, 14)
        );
        trackParams.topMargin = dp(container, 8);
        track.setLayoutParams(trackParams);
        track.setBackground(makeRoundDrawable(Color.parseColor("#F1ECF4"), dp(container, 14)));

        View fill = new View(container.getContext());
        FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT);
        fillParams.gravity = Gravity.START;
        fill.setLayoutParams(fillParams);
        fill.setBackground(makeRoundDrawable(color, dp(container, 14)));
        track.addView(fill);

        row.addView(header);
        row.addView(track);
        container.addView(row);

        float weight = max == 0f ? 0f : Math.max(0.035f, value / max);
        track.post(() -> {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fill.getLayoutParams();
            params.width = value == 0f ? 0 : Math.max(dp(container, 18), Math.round(track.getWidth() * weight));
            fill.setLayoutParams(params);
        });
    }

    private static java.util.Set<String> mesesUnion(Map<String, Float> a, Map<String, Float> b) {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
        s.addAll(a.keySet());
        s.addAll(b.keySet());
        return s;
    }

}

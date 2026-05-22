package com.freddy.controldegastos.PREMIUM;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.freddy.controldegastos.GASTOS.Gasto;
import com.freddy.controldegastos.GastosFijos.GastoFijo;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportarPdfHelper {
    private static final BaseColor COLOR_PRIMARY = new BaseColor(39, 49, 63);
    private static final BaseColor COLOR_MUTED = new BaseColor(101, 113, 128);
    private static final BaseColor COLOR_GREEN = new BaseColor(46, 204, 113);
    private static final BaseColor COLOR_GREEN_DARK = new BaseColor(24, 122, 69);
    private static final BaseColor COLOR_RED = new BaseColor(255, 77, 69);
    private static final BaseColor COLOR_PANEL = new BaseColor(248, 250, 252);
    private static final BaseColor COLOR_BORDER = new BaseColor(226, 232, 240);
    private static final BaseColor COLOR_HEADER = new BaseColor(38, 55, 70);
    private static final BaseColor[] CATEGORY_COLORS = new BaseColor[]{
            new BaseColor(46, 204, 113),
            new BaseColor(47, 128, 237),
            new BaseColor(242, 201, 76),
            new BaseColor(235, 87, 87),
            new BaseColor(155, 81, 224),
            new BaseColor(86, 204, 242)
    };

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_MUTED);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_PRIMARY);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_PRIMARY);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_MUTED);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);

    public static void exportarPDF(Context context, List<Gasto> listaGastos, Bitmap graficoPastel, Bitmap graficoBarras) {
        SharedPreferences prefs = context.getSharedPreferences("mis_datos", Context.MODE_PRIVATE);
        double ingresoMensual = prefs.getFloat("ingreso_mensual", 0f);
        exportarPDF(context, listaGastos, null, null, true, ingresoMensual, graficoPastel, graficoBarras);
    }

    public static void exportarPDF(Context context,
                                   List<Gasto> listaGastos,
                                   String fechaParaFijos,
                                   List<GastoFijo> listaFijos,
                                   boolean soloPagadosFijos,
                                   double ingresoMensual,
                                   Bitmap graficoPastel,
                                   Bitmap graficoBarras) {
        try {
            File directorio = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (directorio == null) {
                Toast.makeText(context, "No se pudo acceder al directorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String hoyNombre = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            File archivoPDF = new File(directorio, "reporte_gastos_" + hoyNombre + ".pdf");

            FileOutputStream fos = new FileOutputStream(archivoPDF);
            Document documento = new Document(PageSize.A4, 36f, 36f, 36f, 34f);
            PdfWriter.getInstance(documento, fos);
            documento.open();

            documento.addTitle("Reporte de Gastos");
            documento.addAuthor("App Control de Gastos");

            List<ReportRow> filas = new ArrayList<>();
            double totalGastos = 0d;
            double totalIngresos = 0d;

            if (listaGastos != null) {
                for (Gasto gasto : listaGastos) {
                    boolean esIngreso = false;
                    try { esIngreso = gasto.isEsIngreso(); } catch (Exception ignored) {}

                    double monto = safeMonto(gasto);
                    if (esIngreso) {
                        totalIngresos += monto;
                        continue;
                    }

                    totalGastos += monto;
                    filas.add(new ReportRow(
                            safe(gasto.getFecha()),
                            safe(gasto.getDescripcion()),
                            safe(gasto.getCategoria()),
                            monto,
                            false
                    ));
                }
            }

            if (listaFijos != null && !listaFijos.isEmpty()) {
                String fechaFijo = fechaParaFijos != null ? fechaParaFijos
                        : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                for (GastoFijo fijo : listaFijos) {
                    if (soloPagadosFijos && !fijo.isPagado()) continue;

                    double monto = safeMonto(fijo);
                    totalGastos += monto;

                    String categoria = safe(fijo.getCategoria());
                    if (categoria.isEmpty()
                            || "sin categoria".equalsIgnoreCase(categoria)
                            || "sin categoría".equalsIgnoreCase(categoria)) {
                        categoria = "Gasto fijo";
                    }

                    filas.add(new ReportRow(
                            fechaFijo,
                            safe(fijo.getDescripcion()),
                            categoria,
                            monto,
                            true
                    ));
                }
            }

            double saldoNeto = ingresoMensual + totalIngresos - totalGastos;

            addHeader(documento, hoyNombre);
            addSummary(documento, totalGastos, totalIngresos, ingresoMensual, saldoNeto);
            addVisualSummary(documento, filas, totalGastos);
            addSectionTitle(documento, "Detalle de gastos");
            addExpenseTable(documento, filas);

            Paragraph footer = new Paragraph("\"La mejor inversión es la que haces en tu bienestar financiero.\"",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, COLOR_MUTED));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(18f);
            documento.add(footer);

            documento.close();
            fos.close();

            Toast.makeText(context, "PDF exportado correctamente", Toast.LENGTH_LONG).show();

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", archivoPDF);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al exportar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void addHeader(Document document, String dateLabel) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2.4f, 1f});
        header.setSpacingAfter(14f);

        PdfPCell titleCell = noBorderCell();
        Paragraph title = new Paragraph("Reporte de gastos", FONT_TITLE);
        Paragraph subtitle = new Paragraph("Resumen financiero generado por Control de Gastos", FONT_SUBTITLE);
        subtitle.setSpacingBefore(4f);
        titleCell.addElement(title);
        titleCell.addElement(subtitle);

        PdfPCell dateCell = noBorderCell();
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph date = new Paragraph("Fecha del reporte\n" + dateLabel, FONT_BODY_BOLD);
        date.setAlignment(Element.ALIGN_RIGHT);
        dateCell.addElement(date);

        header.addCell(titleCell);
        header.addCell(dateCell);
        document.add(header);
    }

    private static void addSummary(Document document,
                                   double totalGastos,
                                   double totalIngresos,
                                   double ingresoMensual,
                                   double saldoNeto) throws Exception {
        PdfPTable summary = new PdfPTable(4);
        summary.setWidthPercentage(100);
        summary.setWidths(new float[]{1f, 1f, 1f, 1f});
        summary.setSpacingAfter(16f);

        summary.addCell(summaryCell("Total gastado", money(totalGastos), COLOR_RED));
        summary.addCell(summaryCell("Ingresos extra", money(totalIngresos), COLOR_GREEN_DARK));
        summary.addCell(summaryCell("Ingreso mensual", money(ingresoMensual), COLOR_PRIMARY));
        summary.addCell(summaryCell("Saldo neto", money(saldoNeto), saldoNeto >= 0 ? COLOR_GREEN_DARK : COLOR_RED));

        document.add(summary);
    }

    private static PdfPCell summaryCell(String label, String value, BaseColor valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_BORDER);
        cell.setBackgroundColor(COLOR_PANEL);
        cell.setPadding(10f);
        cell.setUseAscender(true);
        cell.setUseDescender(true);

        Paragraph labelParagraph = new Paragraph(label, FONT_SMALL);
        labelParagraph.setSpacingAfter(5f);
        cell.addElement(labelParagraph);
        cell.addElement(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, valueColor)));
        return cell;
    }

    private static void addVisualSummary(Document document, List<ReportRow> rows, double totalGastos) throws Exception {
        if (rows.isEmpty() || totalGastos <= 0d) return;

        addSectionTitle(document, "Resumen visual");

        LinkedHashMap<String, Double> totals = new LinkedHashMap<>();
        for (ReportRow row : rows) {
            String category = row.categoria == null || row.categoria.trim().isEmpty()
                    ? "Otros"
                    : row.categoria.trim();
            totals.put(category, totals.containsKey(category) ? totals.get(category) + row.monto : row.monto);
        }

        List<Map.Entry<String, Double>> categories = new ArrayList<>(totals.entrySet());
        Collections.sort(categories, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        PdfPTable visual = new PdfPTable(2);
        visual.setWidthPercentage(100);
        visual.setWidths(new float[]{1f, 1.25f});
        visual.setSpacingAfter(16f);
        visual.addCell(distributionCell(categories, totalGastos));
        visual.addCell(rankingCell(categories, totalGastos));
        document.add(visual);
    }

    private static PdfPCell distributionCell(List<Map.Entry<String, Double>> categories, double total) throws Exception {
        PdfPCell cell = panelCell("Distribución por categoría");

        for (int i = 0; i < categories.size() && i < 6; i++) {
            Map.Entry<String, Double> item = categories.get(i);
            double percent = total == 0d ? 0d : (item.getValue() / total) * 100d;

            PdfPTable row = new PdfPTable(4);
            row.setWidthPercentage(100);
            row.setWidths(new float[]{0.16f, 1.6f, 0.75f, 0.55f});
            row.setSpacingBefore(i == 0 ? 2f : 7f);

            row.addCell(colorDot(CATEGORY_COLORS[i % CATEGORY_COLORS.length]));
            row.addCell(noBorderPhraseCell(item.getKey(), FONT_BODY, Element.ALIGN_LEFT));
            row.addCell(noBorderPhraseCell(money(item.getValue()), FONT_BODY_BOLD, Element.ALIGN_RIGHT));
            row.addCell(noBorderPhraseCell(String.format(Locale.getDefault(), "%.0f%%", percent), FONT_SMALL, Element.ALIGN_RIGHT));
            cell.addElement(row);
        }

        return cell;
    }

    private static PdfPCell rankingCell(List<Map.Entry<String, Double>> categories, double total) throws Exception {
        PdfPCell cell = panelCell("Categorías con más gasto");
        double max = categories.isEmpty() ? 0d : categories.get(0).getValue();

        for (int i = 0; i < categories.size() && i < 6; i++) {
            Map.Entry<String, Double> item = categories.get(i);
            double percent = total == 0d ? 0d : (item.getValue() / total) * 100d;
            BaseColor color = CATEGORY_COLORS[i % CATEGORY_COLORS.length];

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.8f, 1f});
            header.setSpacingBefore(i == 0 ? 2f : 8f);
            header.setSpacingAfter(7f);
            header.addCell(noBorderPhraseCell(item.getKey(), FONT_BODY_BOLD, Element.ALIGN_LEFT));
            header.addCell(noBorderPhraseCell(money(item.getValue()) + "  " + String.format(Locale.getDefault(), "%.0f%%", percent),
                    FONT_SMALL, Element.ALIGN_RIGHT));
            cell.addElement(header);

            PdfPTable bar = barTable(max == 0d ? 0d : item.getValue() / max, color);
            cell.addElement(bar);
        }

        return cell;
    }

    private static PdfPCell panelCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_BORDER);
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setPadding(10f);
        Paragraph paragraph = new Paragraph(title, FONT_BODY_BOLD);
        paragraph.setSpacingAfter(8f);
        cell.addElement(paragraph);
        return cell;
    }

    private static PdfPTable barTable(double ratio, BaseColor color) throws Exception {
        int filled = Math.max(1, Math.min(24, (int) Math.round(ratio * 24d)));
        int empty = Math.max(0, 24 - filled);

        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[]{filled, empty == 0 ? 0.001f : empty});

        PdfPCell fill = new PdfPCell(new Phrase(" "));
        fill.setFixedHeight(8f);
        fill.setBorder(Rectangle.NO_BORDER);
        fill.setBackgroundColor(color);
        bar.addCell(fill);

        PdfPCell rest = new PdfPCell(new Phrase(" "));
        rest.setFixedHeight(8f);
        rest.setBorder(Rectangle.NO_BORDER);
        rest.setBackgroundColor(COLOR_PANEL);
        bar.addCell(rest);
        return bar;
    }

    private static PdfPCell colorDot(BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(color);
        cell.setFixedHeight(8f);
        cell.setPadding(0f);
        return cell;
    }

    private static PdfPCell noBorderPhraseCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(0f);
        return cell;
    }

    private static void addCharts(Document document, Bitmap graficoPastel, Bitmap graficoBarras) throws Exception {
        if (graficoPastel == null && graficoBarras == null) return;

        addSectionTitle(document, "Resumen visual");
        PdfPTable charts = new PdfPTable(graficoPastel != null && graficoBarras != null ? 2 : 1);
        charts.setWidthPercentage(100);
        charts.setSpacingAfter(16f);
        if (graficoPastel != null && graficoBarras != null) {
            charts.setWidths(new float[]{1f, 1f});
        }

        if (graficoPastel != null) charts.addCell(chartCell(graficoPastel, "Distribución por categoría"));
        if (graficoBarras != null) charts.addCell(chartCell(graficoBarras, "Top categorías"));
        document.add(charts);
    }

    private static PdfPCell chartCell(Bitmap bitmap, String title) throws Exception {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8f);
        cell.setBackgroundColor(BaseColor.WHITE);

        Paragraph paragraph = new Paragraph(title, FONT_BODY_BOLD);
        paragraph.setSpacingAfter(6f);
        cell.addElement(paragraph);

        Image image = imageFromBitmap(bitmap);
        image.scaleToFit(230f, 150f);
        image.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(image);
        return cell;
    }

    private static void addSectionTitle(Document document, String title) throws Exception {
        Paragraph section = new Paragraph(title, FONT_SECTION);
        section.setSpacingBefore(4f);
        section.setSpacingAfter(8f);
        document.add(section);
    }

    private static void addExpenseTable(Document document, List<ReportRow> rows) throws Exception {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.05f, 2.5f, 1.45f, 1f, 1.05f});
        table.setHeaderRows(1);

        addHeaderCell(table, "Fecha");
        addHeaderCell(table, "Descripción");
        addHeaderCell(table, "Categoría");
        addHeaderCell(table, "Tipo");
        addHeaderCell(table, "Monto");

        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No hay gastos para mostrar.", FONT_BODY));
            empty.setColspan(5);
            empty.setPadding(12f);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setBorderColor(COLOR_BORDER);
            table.addCell(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                ReportRow row = rows.get(i);
                BaseColor background = i % 2 == 0 ? BaseColor.WHITE : new BaseColor(250, 250, 250);
                addBodyCell(table, row.fecha, Element.ALIGN_LEFT, background, FONT_BODY);
                addBodyCell(table, row.descripcion, Element.ALIGN_LEFT, background, FONT_BODY);
                addBodyCell(table, row.categoria, Element.ALIGN_LEFT, background, FONT_BODY);
                addBodyCell(table, row.esFijo ? "Fijo" : "Variable", Element.ALIGN_CENTER, background, FONT_BODY);
                addBodyCell(table, money(row.monto), Element.ALIGN_RIGHT, background, FONT_BODY_BOLD);
            }
        }

        document.add(table);
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER);
        cell.setBorderColor(COLOR_HEADER);
        cell.setPaddingTop(7f);
        cell.setPaddingBottom(7f);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, int align, BaseColor background, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(background);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(5f);
        cell.setPaddingRight(5f);
        table.addCell(cell);
    }

    private static PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private static Image imageFromBitmap(Bitmap bitmap) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return Image.getInstance(stream.toByteArray());
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static double safeMonto(Gasto gasto) { try { return gasto.getMonto(); } catch (Exception e) { return 0d; } }
    private static double safeMonto(GastoFijo fijo) { try { return fijo.getMonto(); } catch (Exception e) { return 0d; } }
    private static String money(double value) { return String.format(Locale.getDefault(), "$%.2f", value); }

    private static class ReportRow {
        final String fecha;
        final String descripcion;
        final String categoria;
        final double monto;
        final boolean esFijo;

        ReportRow(String fecha, String descripcion, String categoria, double monto, boolean esFijo) {
            this.fecha = fecha;
            this.descripcion = descripcion;
            this.categoria = categoria;
            this.monto = monto;
            this.esFijo = esFijo;
        }
    }
}

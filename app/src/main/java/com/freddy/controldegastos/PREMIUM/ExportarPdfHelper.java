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
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportarPdfHelper {

    // ===== lee ingreso_mensual de SharedPreferences y usa solo fijos pagados =====
    public static void exportarPDF(Context context, List<Gasto> listaGastos, Bitmap graficoPastel, Bitmap graficoBarras) {
        SharedPreferences prefs = context.getSharedPreferences("mis_datos", Context.MODE_PRIVATE);
        double ingresoMensual = prefs.getFloat("ingreso_mensual", 0f);
        exportarPDF(context, listaGastos, null, null, true, ingresoMensual, graficoPastel, graficoBarras);
    }

    /**
     * Versión completa.
     * @param fechaParaFijos     fecha para filas de fijos (ej: "2025-08-09"); null = hoy (yyyy-MM-dd)
     * @param listaFijos         lista de GastoFijo (puede ser null)
     * @param soloPagadosFijos   true = solo fijos marcados como pagados; false = todos
     * @param ingresoMensual     ingreso mensual guardado por el usuario
     */
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
            Document documento = new Document();
            PdfWriter.getInstance(documento, fos);
            documento.open();

            documento.addTitle("Reporte de Gastos");
            documento.addAuthor("App Control de Gastos");

            Paragraph titulo = new Paragraph("Reporte de Gastos - " + hoyNombre,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);
            documento.add(new Paragraph("\n"));

            // ==== Tabla: Fecha / Descripción / Categoría / Monto (mismo formato) ====
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.3f, 2.6f, 1.6f, 1.2f});
            tabla.setHeaderRows(1);

            addHeaderCell(tabla, "Fecha");
            addHeaderCell(tabla, "Descripción");
            addHeaderCell(tabla, "Categoría");
            addHeaderCell(tabla, "Monto");

            double totalGastos   = 0d;
            double totalIngresos = 0d;

            // 1) Gastos variables (excluye ingresos)
            if (listaGastos != null) {
                for (Gasto g : listaGastos) {
                    boolean esIngreso = false;
                    try { esIngreso = g.isEsIngreso(); } catch (Exception ignored) {}
                    double monto = safeMonto(g);

                    if (esIngreso) {
                        totalIngresos += monto;   // no va en tabla de gastos
                        continue;
                    } else {
                        totalGastos += monto;
                    }

                    addBodyCell(tabla, safe(g.getFecha()), Element.ALIGN_LEFT);
                    addBodyCell(tabla, safe(g.getDescripcion()), Element.ALIGN_LEFT);
                    addBodyCell(tabla, safe(g.getCategoria()), Element.ALIGN_LEFT);
                    addBodyCell(tabla, money(monto), Element.ALIGN_RIGHT);
                }
            }

            // 2) Gastos fijos (sumar a total) — categoría visible “Gasto fijo” si viene vacía o "Sin categoría"
            if (listaFijos != null && !listaFijos.isEmpty()) {
                String fechaFijo = fechaParaFijos != null ? fechaParaFijos
                        : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                for (GastoFijo gf : listaFijos) {
                    if (soloPagadosFijos && !gf.isPagado()) continue;

                    double monto = safeMonto(gf);
                    totalGastos += monto;

                    String categoria = safe(gf.getCategoria());
                    if (categoria.isEmpty() ||
                            "sin categoría".equalsIgnoreCase(categoria) ||
                            "sin categoria".equalsIgnoreCase(categoria)) {
                        categoria = "Gasto fijo";
                    }

                    addBodyCell(tabla, fechaFijo, Element.ALIGN_LEFT);
                    addBodyCell(tabla, safe(gf.getDescripcion()), Element.ALIGN_LEFT);
                    addBodyCell(tabla, categoria, Element.ALIGN_LEFT);
                    addBodyCell(tabla, money(monto), Element.ALIGN_RIGHT);
                }
            }

            documento.add(tabla);
            documento.add(new Paragraph("\n"));

            // ===== Totales =====
            double saldoNeto = ingresoMensual + totalIngresos - totalGastos;

            documento.add(new Paragraph("Total gastado: " + money(totalGastos),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            documento.add(new Paragraph("Ingresos extra: " + money(totalIngresos),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            documento.add(new Paragraph("Ingreso mensual: " + money(ingresoMensual),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            documento.add(new Paragraph("Saldo neto: " + money(saldoNeto),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

            documento.add(new Paragraph("\n"));
            Paragraph pie = new Paragraph("\"La mejor inversión es la que haces en tu bienestar financiero.\"",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);

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

    // ===== Helpers tabla =====
    private static void addHeaderCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBackgroundColor(new BaseColor(245, 245, 245));
        c.setPaddingTop(6f);
        c.setPaddingBottom(6f);
        table.addCell(c);
    }

    private static void addBodyCell(PdfPTable table, String text, int align) {
        PdfPCell c = new PdfPCell(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        c.setHorizontalAlignment(align);
        c.setPaddingTop(5f);
        c.setPaddingBottom(5f);
        table.addCell(c);
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static double safeMonto(Gasto g) { try { return g.getMonto(); } catch (Exception e) { return 0d; } }
    private static double safeMonto(GastoFijo gf) { try { return gf.getMonto(); } catch (Exception e) { return 0d; } }
    private static String money(double v) { return String.format(Locale.getDefault(), "$%.2f", v); }
}

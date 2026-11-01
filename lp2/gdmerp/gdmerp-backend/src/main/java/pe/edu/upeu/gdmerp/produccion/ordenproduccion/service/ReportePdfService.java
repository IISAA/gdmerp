package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReportePdfService {

    // Paleta de Colores Corporativos Agro Peruvian Foods
    private static final Color COLOR_PRIMARIO = new Color(30, 81, 40);       // Verde Bosque Corporativo
    private static final Color COLOR_SECUNDARIO = new Color(79, 111, 82);   // Verde Oliva Suave
    private static final Color COLOR_FONDO_TABLA = new Color(245, 247, 248);// Gris neutro claro
    private static final Color COLOR_TEXTO_OSCURO = new Color(33, 37, 41);  // Carbón
    private static final Color COLOR_BORDE = new Color(222, 226, 230);      // Borde sutil

    // Fuentes
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_PRIMARIO);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_SECUNDARIO);
    private static final Font FONT_HEADER_TABLA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXTO_OSCURO);
    private static final Font FONT_VALOR = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXTO_OSCURO);
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY);

    // Formateadores
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

    public byte[] generarReporteOrdenProduccion(OrdenProduccion orden) {
        if (orden == null) {
            throw new IllegalArgumentException("La orden de producción no puede ser nula.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Numeración y pie de página de auditoría
            writer.setPageEvent(new AuditoriaPageEvent());

            document.open();

            // 1. Cabecera Institucional
            agregarCabecera(document, orden);
            document.add(new Paragraph(" "));

            // 2. Trazabilidad y Fechas
            agregarSeccionTrazabilidadYFechas(document, orden);
            document.add(new Paragraph(" "));

            // 3. Métricas de Costo y Centro de Trabajo
            agregarSeccionCostos(document, orden);
            document.add(new Paragraph(" "));

            // 4. Detalle de Materiales / Insumos Consumidos (si existen)
            if (orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
                agregarSeccionMateriales(document, orden);
                document.add(new Paragraph(" "));
            }

            // 5. Bloque de Firmas y Validación
            agregarFirmasAuditoria(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte PDF para la orden #" + orden.getId(), e);
        }
    }

    private void agregarCabecera(Document doc, OrdenProduccion orden) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{65f, 35f});

        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("AGRO PERUVIAN FOODS S.A.C.", FONT_SUBTITULO));
        cellLeft.addElement(new Paragraph("Reporte de Orden de Producción #" + orden.getId(), FONT_TITULO));
        cellLeft.addElement(new Paragraph("RUC: 20601234567 | Planta Industrial Pisco", FONT_FOOTER));
        table.addCell(cellLeft);

        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph emision = new Paragraph("Fecha Emisión: " + LocalDateTime.now().format(DATE_FORMATTER), FONT_VALOR);
        emision.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(emision);

        String estadoTexto = orden.getEstado() != null ? orden.getEstado().name() : "N/D";
        Paragraph estado = new Paragraph("Estado: " + estadoTexto, FONT_LABEL);
        estado.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(estado);

        table.addCell(cellRight);
        doc.add(table);

        // Barra divisoria decorativa
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(6f);
        PdfPCell sepCell = new PdfPCell();
        sepCell.setFixedHeight(2f);
        sepCell.setBackgroundColor(COLOR_PRIMARIO);
        sepCell.setBorder(Rectangle.NO_BORDER);
        separator.addCell(sepCell);
        doc.add(separator);
    }

    private void agregarSeccionTrazabilidadYFechas(Document doc, OrdenProduccion orden) throws DocumentException {
        doc.add(new Paragraph("1. TRAZABILIDAD Y PROGRAMACIÓN", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25f, 25f, 25f, 25f});

        agregarHeaderCelda(table, "Producto");
        agregarHeaderCelda(table, "Lote Generado");
        agregarHeaderCelda(table, "Cant. Planificada");
        agregarHeaderCelda(table, "Estado");

        agregarDatoCelda(table, orden.getProducto());
        agregarDatoCelda(table, orden.getLoteGenerado());
        agregarDatoCelda(table, orden.getCantidadPlanificada() != null ? orden.getCantidadPlanificada().toString() : "-");
        agregarDatoCelda(table, orden.getEstado() != null ? orden.getEstado().name() : "-");

        agregarHeaderCelda(table, "Fecha Inicio Plan.");
        agregarHeaderCelda(table, "Fecha Fin Estimada");
        agregarHeaderCelda(table, "Centro de Trabajo");
        agregarHeaderCelda(table, "Horas Trabajadas");

        agregarDatoCelda(table, orden.getFechaPlanificadaInicio() != null ? orden.getFechaPlanificadaInicio().format(DATE_FORMATTER) : "N/D");
        agregarDatoCelda(table, orden.getFechaEstimadaFin() != null ? orden.getFechaEstimadaFin().format(DATE_FORMATTER) : "N/D");
        agregarDatoCelda(table, orden.getCentroTrabajo() != null ? orden.getCentroTrabajo().getNombre() : "N/D");
        agregarDatoCelda(table, orden.getHorasTrabajadas() != null ? orden.getHorasTrabajadas() + " hrs" : "0.00 hrs");

        doc.add(table);
    }

    private void agregarSeccionCostos(Document doc, OrdenProduccion orden) throws DocumentException {
        doc.add(new Paragraph("2. MÉTRICAS DE COSTO", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{34f, 33f, 33f});

        agregarHeaderCelda(table, "Centro de Trabajo Asignado");
        agregarHeaderCelda(table, "Costo Operativo");
        agregarHeaderCelda(table, "Costo Total Liquidado");

        agregarDatoCelda(table, orden.getCentroTrabajo() != null ? orden.getCentroTrabajo().getNombre() : "N/D");
        agregarDatoMonedaCelda(table, orden.getCostoOperativo());

        PdfPCell totalCell = new PdfPCell(new Phrase(orden.getCostoTotal() != null ? CURRENCY_FORMATTER.format(orden.getCostoTotal()) : CURRENCY_FORMATTER.format(0), FONT_LABEL));
        totalCell.setBackgroundColor(new Color(230, 244, 234));
        totalCell.setPadding(5f);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBorderColor(COLOR_BORDE);
        table.addCell(totalCell);

        doc.add(table);
    }

    private void agregarSeccionMateriales(Document doc, OrdenProduccion orden) throws DocumentException {
        doc.add(new Paragraph("3. DESGLOSE DE INSUMOS Y MATERIALES", FONT_SUBTITULO));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40f, 20f, 20f, 20f});

        agregarHeaderCelda(table, "Insumo / Materia Prima");
        agregarHeaderCelda(table, "Cant. Requerida");
        agregarHeaderCelda(table, "Costo Unitario");
        agregarHeaderCelda(table, "Subtotal");

        BigDecimal totalMateriales = BigDecimal.ZERO;

        for (DetalleOrdenProduccion det : orden.getDetalles()) {
            agregarDatoCelda(table, det.getNombreProducto() != null ? det.getNombreProducto() : "Insumo #" + det.getMateriaPrimaId());
            agregarDatoCelda(table, det.getCantidadRequerida() != null ? det.getCantidadRequerida().toString() : "0");
            agregarDatoMonedaCelda(table, det.getCostoUnitario());

            BigDecimal subtotal = BigDecimal.ZERO;
            if (det.getCantidadRequerida() != null && det.getCostoUnitario() != null) {
                subtotal = det.getCantidadRequerida().multiply(det.getCostoUnitario());
                totalMateriales = totalMateriales.add(subtotal);
            }
            agregarDatoMonedaCelda(table, subtotal);
        }

        // Fila Total Insumos
        PdfPCell labelTotal = new PdfPCell(new Phrase("Total Insumos Requeridos", FONT_LABEL));
        labelTotal.setColspan(3);
        labelTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelTotal.setBackgroundColor(COLOR_FONDO_TABLA);
        labelTotal.setPadding(4f);
        labelTotal.setBorderColor(COLOR_BORDE);
        table.addCell(labelTotal);

        PdfPCell cellTotal = new PdfPCell(new Phrase(CURRENCY_FORMATTER.format(totalMateriales), FONT_LABEL));
        cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellTotal.setBackgroundColor(COLOR_FONDO_TABLA);
        cellTotal.setPadding(4f);
        cellTotal.setBorderColor(COLOR_BORDE);
        table.addCell(cellTotal);

        doc.add(table);
    }

    private void agregarFirmasAuditoria(Document doc) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(30f);
        table.setWidths(new float[]{50f, 50f});

        PdfPCell firma1 = new PdfPCell();
        firma1.setBorder(Rectangle.NO_BORDER);
        firma1.setHorizontalAlignment(Element.ALIGN_CENTER);
        firma1.addElement(new Paragraph("____________________________________", FONT_VALOR));
        firma1.addElement(new Paragraph("Supervisor de Producción", FONT_LABEL));
        firma1.addElement(new Paragraph("Agro Peruvian Foods S.A.C.", FONT_FOOTER));

        PdfPCell firma2 = new PdfPCell();
        firma2.setBorder(Rectangle.NO_BORDER);
        firma2.setHorizontalAlignment(Element.ALIGN_CENTER);
        firma2.addElement(new Paragraph("____________________________________", FONT_VALOR));
        firma2.addElement(new Paragraph("Auditor de Control de Calidad", FONT_LABEL));
        firma2.addElement(new Paragraph("Dpto. Aseguramiento de Calidad", FONT_FOOTER));

        table.addCell(firma1);
        table.addCell(firma2);
        doc.add(table);
    }

    private void agregarHeaderCelda(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_HEADER_TABLA));
        cell.setBackgroundColor(COLOR_SECUNDARIO);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private void agregarDatoCelda(PdfPTable table, String valor) {
        PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "-", FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(4f);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private void agregarDatoMonedaCelda(PdfPTable table, BigDecimal monto) {
        String texto = monto != null ? CURRENCY_FORMATTER.format(monto) : CURRENCY_FORMATTER.format(0);
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_VALOR));
        cell.setBackgroundColor(COLOR_FONDO_TABLA);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(COLOR_BORDE);
        table.addCell(cell);
    }

    private static class AuditoriaPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                String.format("Reporte de Auditoría Interna - Pág. %d | Agro Peruvian Foods S.A.C. - Sistema ERP GDM", writer.getPageNumber()),
                FONT_FOOTER
            );
            ColumnText.showTextAligned(
                cb,
                Element.ALIGN_CENTER,
                footer,
                (document.right() - document.left()) / 2 + document.leftMargin(),
                document.bottom() - 15,
                0
            );
        }
    }
}

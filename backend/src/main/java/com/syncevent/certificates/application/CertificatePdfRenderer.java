package com.syncevent.certificates.application;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.syncevent.certificates.domain.Certificate;

/**
 * Genera el PDF del certificado de asistencia usando fuentes TrueType (LiberationSans)
 * para soportar caracteres Unicode latinos completos: tildes, ñ, diéresis, etc.
 * <p>
 * Incluye ajuste automático de línea (word-wrap) para nombres y títulos extensos.
 */
@Component
public class CertificatePdfRenderer {

    /** Rutas candidatas de LiberationSans en sistemas Linux/Mac/Windows. */
    private static final String[] FONT_CANDIDATES = {
        "/usr/share/fonts/liberation-sans-fonts/LiberationSans-Regular.ttf",
        "/usr/share/fonts/liberation/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "/Library/Fonts/Arial.ttf",
        "C:/Windows/Fonts/arial.ttf"
    };

    private static final String[] FONT_BOLD_CANDIDATES = {
        "/usr/share/fonts/liberation-sans-fonts/LiberationSans-Bold.ttf",
        "/usr/share/fonts/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/Library/Fonts/Arial Bold.ttf",
        "C:/Windows/Fonts/arialbd.ttf"
    };

    /** Anchura máxima de texto en puntos (dentro del borde interior de 90..545). */
    private static final float MAX_TEXT_WIDTH = 450f;

    @Value("${app.certificates.verification-base-url:http://localhost:8080/api/public/certificates}")
    private String verificationBaseUrl = "http://localhost:8080/api/public/certificates";

    // ─────────────────────────────────────────────────────────────────────────
    // Punto de entrada
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] render(Certificate certificate) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Intentar cargar fuentes Unicode; si no están disponibles, usar Type1 estándar.
            PDFont headingFont = loadTrueTypeFont(document, FONT_BOLD_CANDIDATES);
            PDFont bodyFont    = loadTrueTypeFont(document, FONT_CANDIDATES);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // ── Borde decorativo ─────────────────────────────────────────
                content.setNonStrokingColor(new Color(13, 69, 31));
                content.setLineWidth(1f);
                content.addRect(45, 45, 500, 760);
                content.stroke();

                // ── Título ───────────────────────────────────────────────────
                float titleY = 760f;
                drawWrappedText(content, headingFont, 22f,
                        new Color(13, 69, 31), "Certificado de Asistencia",
                        90f, titleY, MAX_TEXT_WIDTH, 28f);

                // ── Cuerpo ───────────────────────────────────────────────────
                float bodyY = 710f;
                float lineH = 25f;

                bodyY = drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Participante", certificate.participantName(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                bodyY = drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Documento",
                        certificate.participantDocumentType() + " " + certificate.participantDocumentNumber(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                bodyY = drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Evento", certificate.eventName(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                bodyY = drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Fecha", certificate.eventStartAt() + " - " + certificate.eventEndAt(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                bodyY = drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Código de verificación", certificate.verificationCode(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                drawLabeledField(content, bodyFont, 12f, Color.BLACK,
                        "Fecha de emisión", certificate.issuedAt().toString(),
                        90f, bodyY, MAX_TEXT_WIDTH, lineH);

                // ── Marca de agua "ORIGINAL" ──────────────────────────────────
                PDFont watermarkFont = headingFont;
                content.setNonStrokingColor(new Color(200, 200, 200, 120));
                content.beginText();
                content.setFont(watermarkFont, 60f);
                content.setTextMatrix(new Matrix(1, 0, 0, 1, 130f, 330f));
                content.showText("ORIGINAL");
                content.endText();

                // ── Código QR ─────────────────────────────────────────────────
                String verificationUrl = verificationBaseUrl + "/" + certificate.verificationCode();
                PDImageXObject qrImage = createQrCode(document, verificationUrl);
                content.drawImage(qrImage, 430f, 70f, 110f, 110f);

                // Etiqueta bajo el QR
                content.beginText();
                content.setFont(bodyFont, 9f);
                content.setNonStrokingColor(Color.DARK_GRAY);
                content.newLineAtOffset(438f, 58f);
                content.showText("Verificar en línea");
                content.endText();
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }

        } catch (IOException | WriterException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del certificado.", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de layout
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dibuja un campo "Etiqueta: valor", con word-wrap automático si el valor
     * supera {@code maxWidth}. Devuelve la coordenada Y posterior a la última
     * línea dibujada (ya desplazada {@code lineHeight} hacia abajo).
     */
    private float drawLabeledField(PDPageContentStream content, PDFont font, float fontSize,
            Color color, String label, String value,
            float x, float y, float maxWidth, float lineHeight) throws IOException {

        String prefix = label + ": ";
        List<String> lines = wrapText(font, fontSize, prefix + value, maxWidth);

        content.setNonStrokingColor(color);
        for (String line : lines) {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(line);
            content.endText();
            y -= lineHeight;
        }
        return y;
    }

    /**
     * Dibuja texto con word-wrap, sin etiqueta de prefijo. Devuelve la Y posterior.
     */
    private float drawWrappedText(PDPageContentStream content, PDFont font, float fontSize,
            Color color, String text, float x, float y, float maxWidth, float lineHeight)
            throws IOException {

        List<String> lines = wrapText(font, fontSize, text, maxWidth);
        content.setNonStrokingColor(color);
        for (String line : lines) {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(line);
            content.endText();
            y -= lineHeight;
        }
        return y;
    }

    /**
     * Parte {@code text} en líneas que no superen {@code maxWidth} puntos
     * al tamaño {@code fontSize} con la fuente {@code font}.
     */
    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth)
            throws IOException {

        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width > maxWidth && !current.isEmpty()) {
                result.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result.isEmpty() ? List.of("") : result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Carga de fuentes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Intenta cargar la primera fuente TrueType encontrada de la lista de candidatos.
     * Si ninguna existe en el sistema, hace fallback a Helvetica (Type1, sin unicode).
     */
    private PDFont loadTrueTypeFont(PDDocument document, String[] candidates) throws IOException {
        for (String path : candidates) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return PDTrueTypeFont.load(document, file, WinAnsiEncoding.INSTANCE);
            }
        }
        // Fallback: fuente estándar Type1 (sin soporte unicode completo).
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación QR
    // ─────────────────────────────────────────────────────────────────────────

    private PDImageXObject createQrCode(PDDocument document, String verificationUrl)
            throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(verificationUrl, BarcodeFormat.QR_CODE, 220, 220);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        return LosslessFactory.createFromImage(document, image);
    }
}

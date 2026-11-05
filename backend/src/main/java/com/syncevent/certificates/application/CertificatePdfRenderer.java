package com.syncevent.certificates.application;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

@Component
public class CertificatePdfRenderer {

    @Value("${app.certificates.verification-base-url:http://localhost:8080/api/public/certificates}")
    private String verificationBaseUrl = "http://localhost:8080/api/public/certificates";

    public byte[] render(Certificate certificate) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font headingFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                content.setNonStrokingColor(new Color(13, 69, 31));
                content.setLineWidth(1f);
                content.addRect(45, 45, 500, 760);
                content.stroke();

                content.beginText();
                content.setFont(headingFont, 22);
                content.newLineAtOffset(90, 760);
                content.showText("Certificado de Asistencia");
                content.endText();

                content.beginText();
                content.setFont(bodyFont, 12);
                content.newLineAtOffset(90, 710);
                content.showText("Participante: " + certificate.participantName());
                content.newLineAtOffset(0, -25);
                content.showText("Documento: " + certificate.participantDocumentType() + " "
                    + certificate.participantDocumentNumber());
                content.newLineAtOffset(0, -25);
                content.showText("Evento: " + certificate.eventName());
                content.newLineAtOffset(0, -25);
                content.showText("Fecha: " + certificate.eventStartAt() + " - " + certificate.eventEndAt());
                content.newLineAtOffset(0, -25);
                content.showText("Código de verificación: " + certificate.verificationCode());
                content.newLineAtOffset(0, -25);
                content.showText("Fecha de emisión: " + certificate.issuedAt());
                content.endText();

                content.setNonStrokingColor(new Color(90, 90, 90, 200));
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 60);
                content.setTextMatrix(new Matrix(1, 0, 0, 1, 150, 330));
                content.showText("ORIGINAL");
                content.endText();

                String verificationUrl = verificationBaseUrl + "/" + certificate.verificationCode();
                PDImageXObject qrImage = createQrCode(document, verificationUrl);
                content.drawImage(qrImage, 430, 120, 120, 120);
                content.beginText();
                content.setFont(bodyFont, 10);
                content.newLineAtOffset(430, 105);
                content.showText("Verificación");
                content.endText();
            }

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("No se pudo generar el PDF del certificado.", exception);
        }
    }

    private PDImageXObject createQrCode(PDDocument document, String verificationUrl) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(verificationUrl, BarcodeFormat.QR_CODE, 220, 220);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        byte[] bytes = output.toByteArray();
        return LosslessFactory.createFromImage(document, image);
    }
}

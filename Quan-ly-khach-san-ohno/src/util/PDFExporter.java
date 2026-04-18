package util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import entity.HoaDon;
import entity.ChiTietHoaDon;

import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Xử lý xuất hóa đơn ra file PDF bằng iText 5.
 */
public class PDFExporter {

    public static void exportHoaDon(Component parent, HoaDon hoaDon, List<ChiTietHoaDon> chiTietList) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("L\u01b0u h\u00f3a \u0111\u01a1n PDF");
        chooser.setSelectedFile(new File("HoaDon_" + hoaDon.getMaHoaDon() + ".pdf"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Cấu hình Font Unicode mặc định (Font Arial)
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            try {
                // Load Arial \u0111\u1ec3 h\u1ed7 tr\u1ee3 Ti\u1ebfng Vi\u1ec7t
                BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                fontTitle = new Font(bf, 18, Font.BOLD);
                fontHeader = new Font(bf, 12, Font.BOLD);
                fontNormal = new Font(bf, 12, Font.NORMAL);
                fontBold = new Font(bf, 14, Font.BOLD);
            } catch (Exception e) {
                // Fallback v\u1ec1 default
            }

            // Header H\u00f3a \u0110\u01a1n
            Paragraph title = new Paragraph("HOA DON THANH TOAN", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Ma Hoa Don: " + hoaDon.getMaHoaDon(), fontNormal));
            document.add(new Paragraph("Ngay lap: " + (hoaDon.getNgayLap() != null ? hoaDon.getNgayLap().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""), fontNormal));
            document.add(new Paragraph(" ", fontNormal));

            // B\u1ea3ng Chi ti\u1ebft H\u00f3a \u0111\u01a1n
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setWidths(new float[]{4f, 2f, 1f, 2f});

            table.addCell(new PdfPCell(new Phrase("Khoan muc / Dich vu", fontHeader)));
            table.addCell(new PdfPCell(new Phrase("Don gia", fontHeader)));
            table.addCell(new PdfPCell(new Phrase("SL", fontHeader)));
            table.addCell(new PdfPCell(new Phrase("Thanh tien", fontHeader)));

            for (ChiTietHoaDon ct : chiTietList) {
                table.addCell(new Phrase(ct.getNoiDung(), fontNormal));
                table.addCell(new Phrase(String.format("%,.0f d", ct.getDonGia()), fontNormal));
                table.addCell(new Phrase(String.valueOf(ct.getSoLuong()), fontNormal));
                table.addCell(new Phrase(String.format("%,.0f d", ct.getThanhTien()), fontNormal));
            }
            document.add(table);

            // T\u1ed5ng h\u1ee3p
            document.add(new Paragraph("Tong tien phong: " + String.format("%,.0f d", hoaDon.getTongTienPhong()), fontNormal));
            document.add(new Paragraph("Tong tien dich vu: " + String.format("%,.0f d", hoaDon.getTongTienDichVu()), fontNormal));
            
            if (hoaDon.getTienGiamKhuyenMai() > 0) {
                document.add(new Paragraph("Giam gia / Khuyen mai: -" + String.format("%,.0f d", hoaDon.getTienGiamKhuyenMai()), fontNormal));
            }
            
            document.add(new Paragraph(" ", fontNormal));
            Paragraph total = new Paragraph("TONG THANH TOAN: " + String.format("%,.0f d", hoaDon.getTongThanhToan()), fontBold);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);
            
            document.add(new Paragraph(" ", fontNormal));
            Paragraph thanks = new Paragraph("Xin cam on quy khach!", fontNormal);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();
            JOptionPane.showMessageDialog(parent, "Xu\u1ea5t PDF th\u00e0nh c\u00f4ng!\nFile: " + file.getAbsolutePath(), "Th\u00e0nh c\u00f4ng", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "L\u1ed7i xu\u1ea5t file PDF: " + e.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
        }
    }
}

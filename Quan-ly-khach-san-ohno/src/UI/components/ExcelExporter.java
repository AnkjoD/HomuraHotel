package ui.components;

import entity.KhachHang;
import entity.NhanVien;
import entity.Phong;
import entity.LoaiPhong;
import entity.NhanVien;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Xuất dữ liệu ra file Excel (CSV format, mở được bằng Excel).
 * Không cần thư viện ngoài (Apache POI), dùng CSV UTF-8 với BOM.
 */
public class ExcelExporter {

    /**
     * Xuất JTable ra file CSV (Excel-compatible).
     * @param parent  parent component để hiện dialog
     * @param model   table model
     * @param title   tên gợi ý cho file
     * @param hiddenCols  các cột ẩn cần bỏ qua (chỉ số)
     */
    public static void exportTable(Component parent, DefaultTableModel model, String title, int... hiddenCols) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file Excel");
        chooser.setSelectedFile(new File(title + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".csv"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            // BOM để Excel nhận UTF-8
            pw.write('\ufeff');

            // Header row
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (isHidden(c, hiddenCols)) continue;
                if (sb.length() > 0) sb.append(",");
                sb.append(escapeCSV(model.getColumnName(c)));
            }
            pw.println(sb.toString());

            // Data rows
            for (int r = 0; r < model.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (isHidden(c, hiddenCols)) continue;
                    if (sb.length() > 0) sb.append(",");
                    Object val = model.getValueAt(r, c);
                    sb.append(escapeCSV(val != null ? val.toString() : ""));
                }
                pw.println(sb.toString());
            }

            JOptionPane.showMessageDialog(parent,
                "✓ Xuất Excel thành công!\nFile: " + file.getAbsolutePath(),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                "Lỗi xuất file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Xuất danh sách KhachHang ra CSV
     */
    public static void exportKhachHang(Component parent, List<KhachHang> list) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất danh sách khách hàng");
        chooser.setSelectedFile(new File("DanhSachKhachHang_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.write('\ufeff');
            pw.println("Mã KH,Họ tên,SĐT,CCCD");
            for (KhachHang kh : list) {
                pw.println(String.join(",",
                    e(kh.getMaKhachHang()), e(kh.getHoTen()),
                    e(kh.getSdt()), e(kh.getCccd())
                ));
            }
            JOptionPane.showMessageDialog(parent,
                "✓ Xuất " + list.size() + " khách hàng thành công!\n" + file.getAbsolutePath(),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Xuất danh sách NhanVien ra CSV
     */
    public static void exportNhanVien(Component parent, List<NhanVien> list) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất danh sách nhân viên");
        chooser.setSelectedFile(new File("DanhSachNhanVien_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.write('\ufeff');
            pw.println("Mã NV,Họ tên,SĐT,Chức vụ,Vai trò");
            for (NhanVien nv : list) {
                String vaiTro = (nv.getTaiKhoan() != null && nv.getTaiKhoan().getVaiTro() != null) 
                                ? nv.getTaiKhoan().getVaiTro().name() : "N/A";
                pw.println(String.join(",",
                    e(nv.getMaNhanVien()), e(nv.getHoTen()),
                    e(nv.getSdt()), e(nv.getChucVu()),
                    e(vaiTro)
                ));
            }
            JOptionPane.showMessageDialog(parent,
                "✓ Xuất " + list.size() + " nhân viên thành công!\n" + file.getAbsolutePath(),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- Helpers ----
    private static boolean isHidden(int col, int[] hidden) {
        for (int h : hidden) if (h == col) return true;
        return false;
    }

    private static String escapeCSV(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            val = val.replace("\"", "\"\"");
            return "\"" + val + "\"";
        }
        return val;
    }

    // Short alias
    private static String e(String s) { return escapeCSV(s); }
}


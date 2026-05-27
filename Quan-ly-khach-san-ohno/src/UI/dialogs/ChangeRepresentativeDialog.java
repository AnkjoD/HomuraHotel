package ui.dialogs;

import service.*;
import entity.*;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChangeRepresentativeDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final KhachHangService khService = new KhachHangService();
    
    private final DatPhong datPhong;
    private final String soPhong;
    private boolean confirmed = false;

    private ModernTextField txtHoTen, txtSDT, txtCCCD;
    private ModernComboBox<String> cboQuocTich;
    private JLabel errHoTen, errSDT, errCCCD;

    // ID Document capture
    private java.util.List<String> selectedImagePaths = new java.util.ArrayList<>();
    private JLabel lblImagePreview;
    private boolean isImageCleared = false;

    public ChangeRepresentativeDialog(Frame owner, DatPhong dp, String soPhong) {
        super(owner, "Thay đổi người đại diện — Phòng " + soPhong, true);
        this.datPhong = dp;
        this.soPhong = soPhong;
        
        setSize(500, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xF8FAFC));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)));
            
        JLabel lblTitle = new JLabel("Cập nhật thông tin Người đại diện");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(lblTitle, BorderLayout.NORTH);
        
        JLabel lblSub = new JLabel("Phòng " + soPhong + " | Đơn đặt: " + datPhong.getMaDatPhong());
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(UIConstants.TEXT_SECONDARY);
        header.add(lblSub, BorderLayout.SOUTH);
        
        root.add(header, BorderLayout.NORTH);

        // Form
        ScrollablePanel form = new ScrollablePanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 16, 0);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1.0;

        int row = 0;

        // Current Rep info
        g.gridy = row++;
        JLabel lblCurrent = new JLabel("Người đại diện hiện tại: " + (datPhong.getKhachHang() != null ? datPhong.getKhachHang().getHoTen() : "N/A"));
        lblCurrent.setFont(UIConstants.FONT_SMALL_BOLD);
        lblCurrent.setForeground(UIConstants.PRIMARY);
        form.add(lblCurrent, g);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE2E8F0));
        g.gridy = row++;
        form.add(sep, g);

        // SDT Search
        errSDT = inlineErr();
        txtSDT = new ModernTextField("Nhập số điện thoại để tìm...");
        if (datPhong.getKhachHang() != null) txtSDT.setText(datPhong.getKhachHang().getSoDienThoai());
        
        g.gridy = row++;
        form.add(fieldGroup("Số điện thoại (Tìm kiếm) *", txtSDT, errSDT), g);

        // Ho Ten
        errHoTen = inlineErr();
        txtHoTen = new ModernTextField("Họ và tên khách hàng...");
        if (datPhong.getKhachHang() != null) txtHoTen.setText(datPhong.getKhachHang().getHoTen());
        
        g.gridy = row++;
        form.add(fieldGroup("Họ và tên *", txtHoTen, errHoTen), g);

        // CCCD & Quoc Tich
        JPanel row4 = new JPanel(new GridLayout(1, 2, 16, 0));
        row4.setOpaque(false);
        
        errCCCD = inlineErr();
        txtCCCD = new ModernTextField("CCCD/Hộ chiếu...");
        if (datPhong.getKhachHang() != null) txtCCCD.setText(datPhong.getKhachHang().getCccd());
        row4.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD, errCCCD));
        
        cboQuocTich = new ModernComboBox<>();
        for (String country : util.CountryList.getCountriesForInput()) {
            cboQuocTich.addItem(country);
        }
        cboQuocTich.setSelectedItem("Việt Nam");
        if (datPhong.getKhachHang() != null && datPhong.getKhachHang().getQuocTich() != null) {
            cboQuocTich.setSelectedItem(datPhong.getKhachHang().getQuocTich());
        }
        row4.add(fieldGroup("Quốc tịch", cboQuocTich, inlineErr())); // Thêm label trống để đồng bộ layout
        
        g.gridy = row++;
        form.add(row4, g);

        // Photo upload box
        JPanel idWrapper = new JPanel(new BorderLayout(0, 8));
        idWrapper.setOpaque(false);
        JLabel lblIdTitle = new JLabel("XÁC MINH DANH TÍNH (ẢNH CCCD/PASSPORT)");
        lblIdTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblIdTitle.setForeground(UIConstants.TEXT_SECONDARY);
        idWrapper.add(lblIdTitle, BorderLayout.NORTH);

        JPanel idBox = new JPanel(new BorderLayout(15, 0));
        idBox.setBackground(Color.WHITE);
        idBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        lblImagePreview = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
        lblImagePreview.setPreferredSize(new Dimension(120, 80));
        lblImagePreview.setOpaque(true);
        lblImagePreview.setBackground(new Color(0xF1F5F9));
        lblImagePreview.setFont(UIConstants.FONT_SMALL);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 1, true));

        // Load existing image if any
        if (datPhong.getKhachHang() != null && datPhong.getKhachHang().getAnhCCCD() != null && !datPhong.getKhachHang().getAnhCCCD().isEmpty()) {
            displayExistingCCCD(datPhong.getKhachHang().getAnhCCCD());
        }

        JPanel idActions = new JPanel();
        idActions.setLayout(new BoxLayout(idActions, BoxLayout.Y_AXIS));
        idActions.setOpaque(false);

        JPanel btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setOpaque(false);

        RoundedButton btnUpload = new RoundedButton("Chụp / Tải lên ảnh mới", UIConstants.PRIMARY, Color.WHITE);
        btnUpload.setPreferredSize(new Dimension(180, 36));
        btnUpload.setMinimumSize(new Dimension(180, 36));
        btnUpload.setMaximumSize(new Dimension(180, 36));
        btnUpload.addActionListener(e -> chooseCCCDImage());
        btnRow.add(btnUpload);

        btnRow.add(Box.createHorizontalStrut(10));

        RoundedButton btnClear = new RoundedButton("Xóa ảnh", UIConstants.DANGER, Color.WHITE);
        btnClear.setPreferredSize(new Dimension(90, 36));
        btnClear.setMinimumSize(new Dimension(90, 36));
        btnClear.setMaximumSize(new Dimension(90, 36));
        btnClear.addActionListener(e -> {
            selectedImagePaths.clear();
            isImageCleared = true;
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Chưa có ảnh");
            NotificationManager.showSuccess("Đã xóa", "Đã xóa ảnh xác minh.");
        });
        btnRow.add(btnClear);

        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        idActions.add(btnRow);

        idActions.add(Box.createVerticalStrut(8));

        JLabel idHint = new JLabel(
                "<html>Định dạng: JPG, PNG. Yêu cầu rõ nét mặt<br>và đầy đủ thông tin trên giấy tờ.</html>");
        idHint.setFont(UIConstants.FONT_TINY);
        idHint.setForeground(UIConstants.TEXT_MUTED);
        idHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        idActions.add(idHint);

        idBox.add(lblImagePreview, BorderLayout.WEST);
        idBox.add(idActions, BorderLayout.CENTER);
        idWrapper.add(idBox, BorderLayout.CENTER);

        g.gridy = row++;
        form.add(idWrapper, g);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(15);

        root.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btns.setBackground(new Color(0xF8FAFC));
        btns.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)));
            
        RoundedButton btnCancel = new RoundedButton("Hủy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(120, 40));
        RoundedButton btnSave = new RoundedButton("Xác nhận thay đổi", UIConstants.PRIMARY, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(160, 40));
        
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> handleSave());
        
        btns.add(btnCancel);
        btns.add(btnSave);
        root.add(btns, BorderLayout.SOUTH);

        // Logic search & validation
        txtSDT.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateSDT();
                String sdt = txtSDT.getText().trim();
                if (sdt.length() >= 10) {
                    KhachHang kh = khService.getByPhone(sdt);
                    if (kh != null) {
                        txtHoTen.setText(kh.getHoTen());
                        txtCCCD.setText(kh.getCccd());
                        if (kh.getQuocTich() != null) cboQuocTich.setSelectedItem(kh.getQuocTich());
                        displayExistingCCCD(kh.getAnhCCCD());
                        errSDT.setText(" ");
                    }
                }
            }
        });

        txtHoTen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateHoTen();
            }
        });

        txtCCCD.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateCCCD();
            }
        });

        setContentPane(root);
    }

    private void handleSave() {
        if (!validateForm()) return;

        String sdt = txtSDT.getText().trim();
        KhachHang kh = khService.getByPhone(sdt);
        
        if (kh == null) {
            kh = new KhachHang();
            kh.setHoTen(txtHoTen.getText().trim());
            kh.setSdt(sdt);
            kh.setCccd(txtCCCD.getText().trim());
            kh.setQuocTich((String) cboQuocTich.getSelectedItem());
            
            if (!selectedImagePaths.isEmpty()) {
                String savedPath = saveCCCDImage("GUEST_" + System.currentTimeMillis());
                kh.setAnhCCCD(savedPath);
            } else if (isImageCleared) {
                kh.setAnhCCCD(null);
            }
            
            String khErr = khService.them(kh);
            if (khErr != null) {
                NotificationManager.showError("Lỗi khách hàng", "Không thể tạo khách hàng mới: " + khErr);
                return;
            }
            kh = khService.getByPhone(sdt);
        } else {
            String currentName = kh.getHoTen().trim();
            String inputName = txtHoTen.getText().trim();
            if (!stripAccents(currentName).equals(stripAccents(inputName))) {
                NotificationManager.showError("Trùng số điện thoại", 
                    "Số điện thoại này đang được sử dụng bởi khách hàng khác: " + currentName + ".\n" +
                    "Vui lòng dùng số điện thoại khác cho người đại diện mới.");
                return;
            }

            kh.setHoTen(txtHoTen.getText().trim());
            kh.setCccd(txtCCCD.getText().trim());
            kh.setQuocTich((String) cboQuocTich.getSelectedItem());
            
            if (!selectedImagePaths.isEmpty()) {
                String savedPath = saveCCCDImage(kh.getMaKhachHang());
                kh.setAnhCCCD(savedPath);
            } else if (isImageCleared) {
                kh.setAnhCCCD(null);
            }
            
            String suaErr = khService.sua(kh);
            if (suaErr != null) {
                NotificationManager.showError("Lỗi khách hàng", suaErr);
                return;
            }
        }

        String err = thuePhongService.updateRepresentative(datPhong.getMaDatPhong(), kh, soPhong);
        if (err == null) {
            NotificationManager.showSuccess("Cập nhật thành công", "Đã thay đổi người đại diện sang: " + kh.getHoTen());
            confirmed = true;
            dispose();
        } else {
            NotificationManager.showError("Lỗi cập nhật", err);
        }
    }

    private boolean validateForm() {
        boolean vSDT = validateSDT();
        boolean vTen = validateHoTen();
        boolean vCCCD = validateCCCD();
        return vSDT && vTen && vCCCD;
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) {
            errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại");
            return false;
        } else if (!sdt.matches("^(0[35789])\\d{8}$")) {
            errSDT.setText("SĐT không hợp lệ (10 số, đầu 03/05/07/08/09)");
            return false;
        }
        return true;
    }

    private boolean validateHoTen() {
        String ten = txtHoTen.getText().trim();
        ten = formatName(ten);
        txtHoTen.setText(ten);
        errHoTen.setText(" ");
        if (ten.isEmpty()) {
            errHoTen.setText("Bắt buộc - Vui lòng nhập họ tên");
            return false;
        } else if (!ten.matches("^[\\p{L} .'-]{2,50}$")) {
            errHoTen.setText("Họ tên không hợp lệ (2-50 ký tự)");
            return false;
        }
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (!cccd.isEmpty() && !cccd.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            errCCCD.setText("CCCD/Passport không hợp lệ");
            return false;
        }
        return true;
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private JPanel fieldGroup(String label, JComponent comp) { return fieldGroup(label, comp, null); }
    private JPanel fieldGroup(String label, JComponent comp, JLabel errLbl) {
        comp.setPreferredSize(new Dimension(100, 38)); 
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); 
        
        JPanel g = new JPanel(new BorderLayout(0, 4));
        g.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        
        g.add(lbl, BorderLayout.NORTH);
        
        // Wrap component in a panel that doesn't stretch it vertically
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(comp, BorderLayout.NORTH);
        g.add(wrapper, BorderLayout.CENTER);
        
        if (errLbl != null) {
            g.add(errLbl, BorderLayout.SOUTH);
        }
        return g;
    }

    private JLabel inlineErr() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        return l;
    }

    private void displayExistingCCCD(String paths) {
        if (paths == null || paths.isEmpty()) {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Chưa có ảnh");
            return;
        }
        String firstPath = paths.split(";")[0];
        java.io.File file = new java.io.File(firstPath);
        if (file.exists()) {
            try {
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image img = icon.getImage().getScaledInstance(100, 64, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(img));
                lblImagePreview.setText("Đã có: " + paths.split(";").length + " ảnh");
                lblImagePreview.setHorizontalTextPosition(SwingConstants.CENTER);
                lblImagePreview.setVerticalTextPosition(SwingConstants.BOTTOM);
            } catch (Exception e) {
                lblImagePreview.setText("Lỗi ảnh");
            }
        } else {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Không tìm thấy file");
        }
    }

    private void chooseCCCDImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn các ảnh CCCD/Passport");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File[] files = chooser.getSelectedFiles();
            selectedImagePaths.clear();
            isImageCleared = false;
            for (java.io.File f : files) {
                selectedImagePaths.add(f.getAbsolutePath());
            }

            // Preview thumbnail
            if (!selectedImagePaths.isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon(selectedImagePaths.get(0));
                    Image img = icon.getImage().getScaledInstance(100, 64, Image.SCALE_SMOOTH);
                    lblImagePreview.setIcon(new ImageIcon(img));
                    lblImagePreview.setText("Đã chọn: " + selectedImagePaths.size() + " ảnh");
                    lblImagePreview.setHorizontalTextPosition(SwingConstants.CENTER);
                    lblImagePreview.setVerticalTextPosition(SwingConstants.BOTTOM);
                } catch (Exception e) {
                    lblImagePreview.setText("Lỗi ảnh");
                }
            } else {
                lblImagePreview.setIcon(null);
                lblImagePreview.setText("Chưa có ảnh");
            }
        }
    }

    private String saveCCCDImage(String maKH) {
        if (selectedImagePaths.isEmpty())
            return null;
        try {
            java.nio.file.Path destDir = java.nio.file.Paths.get("img", "customers");
            if (!java.nio.file.Files.exists(destDir))
                java.nio.file.Files.createDirectories(destDir);

            java.util.List<String> savedPaths = new java.util.ArrayList<>();
            for (int i = 0; i < selectedImagePaths.size(); i++) {
                String path = selectedImagePaths.get(i);
                java.io.File source = new java.io.File(path);
                String ext = path.substring(path.lastIndexOf("."));
                String fileName = maKH + "_CCCD_" + i + ext;
                java.nio.file.Path destPath = destDir.resolve(fileName);
                java.nio.file.Files.copy(source.toPath(), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                savedPaths.add("img/customers/" + fileName);
            }
            return String.join(";", savedPaths);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isConfirmed() { return confirmed; }

    private static class ScrollablePanel extends JPanel implements javax.swing.Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        String normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d").toLowerCase().trim();
    }
}

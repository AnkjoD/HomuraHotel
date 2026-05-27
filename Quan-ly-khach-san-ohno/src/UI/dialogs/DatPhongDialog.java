package ui.dialogs;

import service.*;
import entity.*;
import entity.enums.TrangThaiDatPhong;
import ui.components.NotificationManager;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.List;

public class DatPhongDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private final KhachHangService khService = new KhachHangService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private final List<Phong> selectedRoomsList;
    private final Date checkIn;
    private final Date checkOut;
    private final KhachHang prefilledKhachHang;
    private final DatPhong editingDatPhong;
    private final Runnable onSuccess;

    private JComboBox<String> cboLoaiKhach;
    private ModernComboBox<String> cboTenDoan;
    private ModernTextField txtGiamGia;
    private ModernTextField txtSDT;
    private ModernTextField txtHoTen;
    private ModernTextField txtCCCD;
    private ModernComboBox<String> cboQuocTich;
    private ModernSpinner spnKhach;
    private JComboBox<entity.KenhDatPhong> cboKenh;
    private ModernTextField txtOTA;
    private ui.components.DateTimePicker txtHanCoc;
    private ModernTextField txtVoucher;
    private ModernTextField txtTienCocTay;
    private JTextArea txtGhiChu;

    public DatPhongDialog(Frame owner, List<Phong> selectedRoomsList, Date checkIn, Date checkOut, Runnable onSuccess) {
        this(owner, selectedRoomsList, checkIn, checkOut, (KhachHang) null, onSuccess);
    }

    public DatPhongDialog(Frame owner, List<Phong> selectedRoomsList, Date checkIn, Date checkOut, KhachHang prefilledKhachHang, Runnable onSuccess) {
        super(owner, true);
        this.selectedRoomsList = selectedRoomsList;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.prefilledKhachHang = prefilledKhachHang;
        this.editingDatPhong = null;
        this.onSuccess = onSuccess;

        String titleStr = selectedRoomsList.size() == 1
                ? "P." + selectedRoomsList.get(0).getSoPhongDisplay()
                : selectedRoomsList.size() + " phòng";
        setTitle("Đặt phòng — " + titleStr);
        setSize(600, 720);
        setLocationRelativeTo(owner);
        setResizable(false);

        buildUI(titleStr);
    }

    public DatPhongDialog(Frame owner, List<Phong> selectedRoomsList, Date checkIn, Date checkOut, DatPhong editingDatPhong, Runnable onSuccess) {
        super(owner, true);
        this.selectedRoomsList = selectedRoomsList;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.editingDatPhong = editingDatPhong;
        this.prefilledKhachHang = editingDatPhong != null ? editingDatPhong.getKhachHang() : null;
        this.onSuccess = onSuccess;

        String titleStr = selectedRoomsList.size() == 1
                ? "P." + selectedRoomsList.get(0).getSoPhongDisplay()
                : selectedRoomsList.size() + " phòng";
        
        if (editingDatPhong != null) {
            setTitle("Chỉnh sửa đơn đặt phòng — " + editingDatPhong.getMaDatPhong());
        } else {
            setTitle("Đặt phòng — " + titleStr);
        }
        setSize(600, 720);
        setLocationRelativeTo(owner);
        setResizable(false);

        buildUI(titleStr);
    }

    private void buildUI(String titleStr) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ---- Top Summary Card ----
        JPanel summaryCard = new JPanel(new BorderLayout(0, 10));
        summaryCard.setBackground(new Color(0xF8FAFC));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);

        int totalSucChua = 0;
        int totalAbsMax = 0;
        double pricePerNight = 0;
        StringBuilder roomNames = new StringBuilder();
        for (Phong p : selectedRoomsList) {
            totalSucChua += p.getSucChua();
            totalAbsMax += (p.getSucChua() + 2);
            double giaBook = bangGiaService.layGiaVaoThoiDiem(
                    p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "",
                    checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            pricePerNight += giaBook;
            roomNames.append("P.").append(p.getSoPhongDisplay()).append(", ");
        }
        if (roomNames.length() > 2)
            roomNames.setLength(roomNames.length() - 2);

        JLabel bannerTitle = new JLabel(editingDatPhong != null ? "Cập nhật đặt phòng " + editingDatPhong.getMaDatPhong() : "Xác nhận đặt phòng " + titleStr);
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        bannerTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel detailsGrid = new JPanel(new GridLayout(2, 2, 20, 8));
        detailsGrid.setOpaque(false);

        String[][] details = {
                { "Phòng:", roomNames.toString() },
                { "Thời gian:", sdf.format(checkIn) + " - " + sdf.format(checkOut) + " (" + soNgay + " đêm)" },
                { "Sức chứa:", totalSucChua + " người (Tối đa " + totalAbsMax + ")" },
                { "Giá tạm tính:", String.format("%,.0fđ", pricePerNight) }
        };

        for (String[] d : details) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p.setOpaque(false);
            JLabel lblKey = new JLabel(d[0] + " ");
            lblKey.setFont(UIConstants.FONT_SMALL);
            lblKey.setForeground(UIConstants.TEXT_SECONDARY);
            JLabel lblVal = new JLabel(d[1]);
            lblVal.setFont(UIConstants.FONT_SMALL_BOLD);
            lblVal.setForeground(UIConstants.TEXT_PRIMARY);
            if (d[0].startsWith("Gia"))
                lblVal.setForeground(UIConstants.SUCCESS);
            p.add(lblKey);
            p.add(lblVal);
            detailsGrid.add(p);
        }

        summaryCard.add(bannerTitle, BorderLayout.NORTH);
        summaryCard.add(detailsGrid, BorderLayout.CENTER);
        root.add(summaryCard, BorderLayout.NORTH);

        // ---- Form content (wrapped in ScrollPane) ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 16, 0);
        g.anchor = GridBagConstraints.NORTHWEST;

        int rowIdx = 0;

        // Section: Loai khach
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        form.add(modernSectionLabel("Loại khách"), g);

        cboLoaiKhach = new ModernComboBox<>(new String[] { "Khách lẻ (Cá nhân)", "Khách đoàn" });
        cboTenDoan = new ModernComboBox<>(new String[] {
                "Đoàn tự do",
                "FPT Software",
                "Viettel Group",
                "Vingroup",
                "Samsung Vietnam",
                "Vietravel",
                "Saigontourist"
        });
        cboTenDoan.setEditable(true);
        cboTenDoan.setEnabled(false);

        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Phân loại *", cboLoaiKhach), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Tên đoàn / Công ty", cboTenDoan), g);
        rowIdx++;

        txtGiamGia = new ModernTextField("Giảm giá (%) - VD: 10");
        txtGiamGia.setEnabled(false);
        txtGiamGia.setBackground(new Color(0xF1F5F9));

        // Group discount is commented out/hidden as requested
        /*
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Giảm giá đoàn (%)", txtGiamGia), g);
        */

        cboLoaiKhach.addActionListener(e -> {
            boolean isDoan = cboLoaiKhach.getSelectedIndex() == 1;
            cboTenDoan.setEnabled(isDoan);
            txtGiamGia.setEnabled(isDoan);
            txtGiamGia.setBackground(isDoan ? Color.WHITE : new Color(0xF1F5F9));
        });

        // Section: Thong tin khach hàng
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        form.add(modernSectionLabel("Thông tin khách hàng (Người đại diện)"), g);

        JLabel errHoTen = inlineErr();
        txtHoTen = new ModernTextField("Nhập họ và tên...");
        JLabel errSDT = inlineErr();
        txtSDT = new ModernTextField("Nhập số điện thoại...");

        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 12, 8);
        form.add(fieldGroup("Số điện thoại *", txtSDT, errSDT), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 12, 0);
        form.add(fieldGroup("Họ và tên *", txtHoTen, errHoTen), g);
        rowIdx++;

        // Real-time validation for Name and Phone
        txtHoTen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateTenKH(txtHoTen, errHoTen);
            }
        });
        txtSDT.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateSDT(txtSDT, errSDT);
            }
        });
        KeyAdapter liveVal = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getSource() == txtHoTen && !errHoTen.getText().equals(" "))
                    validateTenKH(txtHoTen, errHoTen);
                if (e.getSource() == txtSDT && !errSDT.getText().equals(" "))
                    validateSDT(txtSDT, errSDT);
            }
        };
        txtHoTen.addKeyListener(liveVal);
        txtSDT.addKeyListener(liveVal);

        JLabel errCCCD = inlineErr();
        txtCCCD = new ModernTextField("CCCD/Hộ chiếu (tùy chọn)...");
        cboQuocTich = new ModernComboBox<>();
        for (String country : util.CountryList.getCountriesForInput()) {
            cboQuocTich.addItem(country);
        }
        cboQuocTich.setSelectedItem("Việt Nam");

        g.gridy = rowIdx;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD, errCCCD), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Quốc tịch", cboQuocTich), g);
        rowIdx++;

        if (prefilledKhachHang != null) {
            txtSDT.setText(prefilledKhachHang.getSdt() != null ? prefilledKhachHang.getSdt() : "");
            txtHoTen.setText(prefilledKhachHang.getHoTen() != null ? prefilledKhachHang.getHoTen() : "");
            txtCCCD.setText(prefilledKhachHang.getCccd() != null ? prefilledKhachHang.getCccd() : "");
            if (prefilledKhachHang.getQuocTich() != null && !prefilledKhachHang.getQuocTich().isBlank()) {
                cboQuocTich.setSelectedItem(prefilledKhachHang.getQuocTich());
            }
        }

        txtCCCD.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateCCCD(txtCCCD, errCCCD);
            }
        });
        txtCCCD.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (!errCCCD.getText().equals(" "))
                    validateCCCD(txtCCCD, errCCCD);
            }
        });

        spnKhach = new ModernSpinner(new SpinnerNumberModel(1, 1, totalAbsMax > 0 ? totalAbsMax : 50, 1));
        spnKhach.setToolTipText("Lưu ý: Nếu số khách nhập vào lớn hơn tiêu chuẩn (" + totalSucChua
                + "), phụ thu sẽ áp dụng khi nhận phòng.");

        cboKenh = new ModernComboBox<>();
        try {
            for (entity.KenhDatPhong kenh : new dao.KenhDatPhongDAO().getAll()) {
                cboKenh.addItem(kenh);
            }
        } catch (Exception e) {
        }

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Khách dự kiến", spnKhach), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Kênh đặt phòng", cboKenh), g);
        rowIdx++;

        txtOTA = new ModernTextField("Mã xác nhận OTA (nếu có)...");
        txtOTA.setEnabled(false);
        txtOTA.setBackground(new Color(0xF1F5F9));

        cboKenh.addActionListener(ev -> {
            entity.KenhDatPhong k = (entity.KenhDatPhong) cboKenh.getSelectedItem();
            boolean isOta = k != null && ("OTA".equals(k.getLoaiKenh()) || "TA".equals(k.getLoaiKenh()));
            txtOTA.setEnabled(isOta);
            txtOTA.setBackground(isOta ? Color.WHITE : new Color(0xF1F5F9));
        });

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Mã OTA / TA", txtOTA), g);
        g.gridx = 1;
        form.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, g);
        rowIdx++;

        // Auto-fill logic + Blacklist check
        txtSDT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String sdt = txtSDT.getText().trim();
                if (sdt.length() >= 10) {
                    KhachHang kh = khService.getByPhone(sdt);
                    if (kh != null) {
                        txtHoTen.setText(kh.getHoTen());
                        txtCCCD.setText(kh.getCccd());
                        if (kh.getQuocTich() != null)
                            cboQuocTich.setSelectedItem(kh.getQuocTich());

                        // Cảnh báo Blacklist
                        if (kh.isBlacklist()) {
                            NotificationManager.showWarning("CẢNH BÁO",
                                    "Khách hàng này đang nằm trong DANH SÁCH ĐEN!\nGhi chú: " + kh.getPreferences());
                            txtHoTen.setForeground(Color.RED);
                        } else {
                            txtHoTen.setForeground(UIConstants.TEXT_PRIMARY);
                            if (kh.getPreferences() != null && !kh.getPreferences().isBlank()) {
                                NotificationManager.showInfo("Sở thích/Lưu ý", kh.getPreferences());
                            }
                        }
                    }
                }
            }
        });

        // Section: Lịch trình từng phòng
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(8, 0, 16, 0);
        form.add(modernSectionLabel("Lịch trình lưu trú từng phòng"), g);

        final java.util.List<ui.components.DateTimePicker[]> roomDates = new java.util.ArrayList<>();
        final Runnable[] updateEstTotal = new Runnable[1];
        final Runnable[] updateHanCocHint = new Runnable[1];

        for (Phong ph : selectedRoomsList) {
            ui.components.DateTimePicker pickNhan = new ui.components.DateTimePicker(checkIn);
            ui.components.DateTimePicker pickTra = new ui.components.DateTimePicker(checkOut);
            roomDates.add(new ui.components.DateTimePicker[] { pickNhan, pickTra });

            JLabel lblStatus = new JLabel("Phòng trống");
            lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblStatus.setForeground(new Color(0x10B981)); // Success green

            Runnable checkAvailability = () -> {
                LocalDateTime ldtNhan = pickNhan.getLocalDateTime();
                LocalDateTime ldtTra = pickTra.getLocalDateTime();
                if (!ldtTra.isAfter(ldtNhan)) {
                    lblStatus.setText("Ngày trả phải sau ngày nhận");
                    lblStatus.setForeground(new Color(0xEF4444)); // Error red
                } else if (!new dao.PhongDAO().isRoomAvailable(ph.getMaPhong(), ldtNhan, ldtTra)) {
                    lblStatus.setText("Bị trùng lịch đặt phòng!");
                    lblStatus.setForeground(new Color(0xEF4444)); // Error red
                } else {
                    lblStatus.setText("Phòng trống sẵn sàng");
                    lblStatus.setForeground(new Color(0x10B981)); // Success green
                }
            };

            g.gridwidth = 1;
            g.weightx = 0.5;
            g.gridy = rowIdx;
            g.gridx = 0;
            g.insets = new Insets(0, 0, 12, 8);
            form.add(fieldGroup("Nhận phòng " + ph.toString() + " *", pickNhan), g);

            g.gridx = 1;
            g.insets = new Insets(0, 8, 12, 0);
            form.add(fieldGroup("Trả phòng " + ph.toString() + " *", pickTra, lblStatus), g);
            rowIdx++;

            pickNhan.addChangeListener(ev -> {
                checkAvailability.run();
                if (updateEstTotal[0] != null)
                    updateEstTotal[0].run();
                if (updateHanCocHint[0] != null)
                    updateHanCocHint[0].run();
            });
            pickTra.addChangeListener(ev -> {
                checkAvailability.run();
                if (updateEstTotal[0] != null)
                    updateEstTotal[0].run();
                if (updateHanCocHint[0] != null)
                    updateHanCocHint[0].run();
            });

            // Initial check
            checkAvailability.run();
        }

        // Section: Thanh toan
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(12, 0, 16, 0);
        form.add(modernSectionLabel("Thông tin thanh toán"), g);

        double tongTien = pricePerNight * soNgay;
        ModernTextField txtTongTien = new ModernTextField(String.format("%,.0fđ", tongTien));
        txtTongTien.setEditable(false);
        txtTongTien.setBackground(new Color(0xF1F5F9));

        txtTienCocTay = new ModernTextField("Nhập số tiền đặt cọc (VNĐ)...");
        txtTienCocTay.setVisible(false);
        if (editingDatPhong != null && editingDatPhong.getTienDatCoc() > 0) {
            txtTienCocTay.setText(String.format("%.0f", editingDatPhong.getTienDatCoc()));
        }

        JComboBox<String> cboCoc = new ModernComboBox<>();

        JPanel pnlCocContainer = new JPanel(new GridBagLayout());
        pnlCocContainer.setOpaque(false);
        GridBagConstraints gcCoc = new GridBagConstraints();
        gcCoc.fill = GridBagConstraints.HORIZONTAL;
        gcCoc.weightx = 1.0;
        gcCoc.gridx = 0;
        gcCoc.gridy = 0;
        gcCoc.insets = new Insets(0, 0, 0, 0);
        pnlCocContainer.add(cboCoc, gcCoc);

        gcCoc.gridy = 1;
        gcCoc.insets = new Insets(8, 0, 0, 0);
        pnlCocContainer.add(txtTienCocTay, gcCoc);

        updateEstTotal[0] = () -> {
            double total = 0;
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                Phong ph = selectedRoomsList.get(i);
                ui.components.DateTimePicker[] pickers = roomDates.get(i);
                Date rCheckIn = pickers[0].getDate();
                Date rCheckOut = pickers[1].getDate();
                long rNights = new ThuePhongService().tinhSoNgay(rCheckIn, rCheckOut);
                double base = bangGiaService.layGiaVaoThoiDiem(
                        ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "",
                        rCheckIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                total += base * rNights;
            }
            txtTongTien.setText(String.format("%,.0fđ", total));

            int selIdx = cboCoc.getSelectedIndex();
            cboCoc.removeAllItems();
            cboCoc.addItem("Không đặt cọc");
            cboCoc.addItem("30% (" + String.format("%,.0fđ", total * 0.3) + ")");
            cboCoc.addItem("50% (" + String.format("%,.0fđ", total * 0.5) + ")");
            cboCoc.addItem("100% (" + String.format("%,.0fđ", total) + ")");
            cboCoc.addItem("Nhập số tiền...");

            if (editingDatPhong != null) {
                double oldTotal = 0;
                if (editingDatPhong.getDsChiTiet() != null) {
                    for (ChiTietDatPhong ct : editingDatPhong.getDsChiTiet()) {
                        LocalDateTime rInLDT = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : editingDatPhong.getNgayNhanDuKien();
                        LocalDateTime rOutLDT = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : editingDatPhong.getNgayTraDuKien();
                        Date rCI = java.sql.Timestamp.valueOf(rInLDT);
                        Date rCO = java.sql.Timestamp.valueOf(rOutLDT);
                        long rNights = new ThuePhongService().tinhSoNgay(rCI, rCO);
                        oldTotal += ct.getGiaThucTeChot() * rNights;
                    }
                }
                if (oldTotal > 0) {
                    double pct = editingDatPhong.getTienDatCoc() / oldTotal;
                    cboCoc.addItem(String.format("Theo tỷ lệ cũ %.0f%% (%,.0fđ)", pct * 100, total * pct));
                }
            }

            if (selIdx >= 0 && selIdx < cboCoc.getItemCount()) {
                cboCoc.setSelectedIndex(selIdx);
            } else if (editingDatPhong != null && cboCoc.getItemCount() > 5) {
                cboCoc.setSelectedIndex(5); // Select custom ratio by default
            } else {
                cboCoc.setSelectedIndex(0);
            }
        };

        // Initialize cboCoc options
        updateEstTotal[0].run();

        g.gridy = rowIdx;
        g.gridwidth = 1;
        g.weightx = 0.5;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 16, 8);
        form.add(fieldGroup("Tổng tiền phòng (ước tính)", txtTongTien), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 16, 0);
        form.add(fieldGroup("Đặt cọc", pnlCocContainer), g);
        rowIdx++;

        // Hạn nộp cọc
        Date defaultHanCoc;
        long nowMs = System.currentTimeMillis();
        long checkInMs = checkIn.getTime();
        if (checkInMs - nowMs > 86400000) {
            defaultHanCoc = new Date(nowMs + 86400000);
        } else {
            long targetMs = checkInMs - 2 * 3600000; // Check-in minus 2 hours
            if (targetMs > nowMs) {
                defaultHanCoc = new Date(targetMs);
            } else {
                defaultHanCoc = new Date(nowMs); // If too close, set to now
            }
        }

        txtHanCoc = new ui.components.DateTimePicker(defaultHanCoc);
        txtHanCoc.setEnabled(false); // Default disabled because "Không đặt cọc" is selected
        txtVoucher = new ModernTextField("Mã voucher...");

        JLabel lblHanCocHint = new JLabel(" ");
        lblHanCocHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHanCocHint.setForeground(UIConstants.TEXT_MUTED);

        updateHanCocHint[0] = () -> {
            if (!txtHanCoc.isEnabled()) {
                lblHanCocHint.setText(" ");
                return;
            }
            LocalDateTime hanCoc = txtHanCoc.getLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            
            LocalDateTime minIn = null;
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                LocalDateTime rCI = roomDates.get(i)[0].getLocalDateTime();
                if (minIn == null || rCI.isBefore(minIn)) {
                    minIn = rCI;
                }
            }
            
            if (minIn == null) {
                lblHanCocHint.setText(" ");
                return;
            }
            
            if (hanCoc.isAfter(minIn)) {
                lblHanCocHint.setText("Lỗi: Hạn nộp cọc phải TRƯỚC ngày nhận phòng!");
                lblHanCocHint.setForeground(new Color(0xEF4444)); // Error red
                return;
            }
            
            if (hanCoc.isBefore(now.minusHours(2))) {
                lblHanCocHint.setText("Lỗi: Hạn nộp cọc không được ở quá khứ!");
                lblHanCocHint.setForeground(new Color(0xEF4444)); // Error red
                return;
            }

            if (hanCoc.isBefore(now)) {
                lblHanCocHint.setText("Đến hạn nộp cọc (hạn nộp lúc " + hanCoc.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + ").");
                lblHanCocHint.setForeground(new Color(0xF59E0B)); // Warning orange
                return;
            }
            
            java.time.Duration duration = java.time.Duration.between(now, hanCoc);
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            
            String text = "Còn ";
            if (days > 0) {
                text += days + " ngày " + hours + " giờ";
            } else if (hours > 0) {
                text += hours + " giờ " + minutes + " phút";
            } else {
                text += minutes + " phút";
            }
            text += " đến hạn nộp cọc.";
            
            lblHanCocHint.setText(text);
            lblHanCocHint.setForeground(new Color(0x10B981)); // Success green
        };

        txtHanCoc.addChangeListener(ev -> {
            if (updateHanCocHint[0] != null) {
                updateHanCocHint[0].run();
            }
        });

        cboCoc.addActionListener(e -> {
            String selCoc = (String) cboCoc.getSelectedItem();
            boolean isCustom = selCoc != null && selCoc.startsWith("Nhập số tiền");
            txtTienCocTay.setVisible(isCustom);

            boolean hasCoc = cboCoc.getSelectedIndex() > 0;
            txtHanCoc.setEnabled(hasCoc);
            
            if (updateHanCocHint[0] != null) {
                updateHanCocHint[0].run();
            }

            form.revalidate();
            form.repaint();
        });

        // Run initial update for Han Coc Hint
        updateHanCocHint[0].run();

        g.gridy = rowIdx;
        g.gridx = 0;
        g.insets = new Insets(0, 0, 24, 8);
        form.add(fieldGroup("Hạn nộp cọc (Deadline)", txtHanCoc, lblHanCocHint), g);
        g.gridx = 1;
        g.insets = new Insets(0, 8, 24, 0);
        form.add(fieldGroup("Mã voucher", txtVoucher), g);
        rowIdx++;

        // Section: Ghi chu
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 16, 0);
        form.add(modernSectionLabel("Ghi chú / Yêu cầu đặc biệt"), g);

        txtGhiChu = new JTextArea(3, 0);
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtGhiChu.setBackground(Color.WHITE);
        String hint = "VD: Cần giường phụ, yêu cầu tầng cao, nhận phòng muộn...";
        txtGhiChu.setText(hint);
        txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
        txtGhiChu.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtGhiChu.getText().equals(hint)) {
                    txtGhiChu.setText("");
                    txtGhiChu.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtGhiChu.getText().isBlank()) {
                    txtGhiChu.setText(hint);
                    txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });

        JScrollPane spGhiChu = new JScrollPane(txtGhiChu);
        g.gridx = 0;
        g.gridy = rowIdx++;
        g.gridwidth = 2;
        g.weightx = 1.0;
        g.weighty = 0.1;
        g.fill = GridBagConstraints.BOTH;
        g.insets = new Insets(0, 0, 0, 0);
        form.add(spGhiChu, g);

        // ---- Buttons ----
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setBackground(new Color(0xF8FAFC));
        btns.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        RoundedButton btnCancel = new RoundedButton("Hủy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(90, 38));
        RoundedButton btnOk = new RoundedButton(editingDatPhong != null ? "Xác nhận cập nhật" : "Xác nhận đặt phòng", UIConstants.PRIMARY, Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setPreferredSize(new Dimension(200, 38));

        btnCancel.addActionListener(e -> dispose());

        final int finalTotalAbsMax = totalAbsMax;
        btnOk.addActionListener(e -> {
            String hoTen = txtHoTen.getText().trim();
            String sdt = txtSDT.getText().trim();
            String cccd = txtCCCD.getText().trim();
            int soKhach = (int) spnKhach.getValue();
            String ghiChuText = txtGhiChu.getText().trim();
            if (ghiChuText.equals(hint))
                ghiChuText = "";

            if (!validateTenKH(txtHoTen, errHoTen) || !validateSDT(txtSDT, errSDT) || !validateCCCD(txtCCCD, errCCCD)) {
                NotificationManager.showError("Lỗi", "Vui lòng kiểm tra lại tính hợp lệ của thông tin!");
                return;
            }

            // 1. Kiểm tra ngày nhận/trả và sẵn sàng cho từng phòng
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                Phong ph = selectedRoomsList.get(i);
                ui.components.DateTimePicker[] pickers = roomDates.get(i);
                Date rCheckIn = pickers[0].getDate();
                Date rCheckOut = pickers[1].getDate();

                if (!rCheckOut.after(rCheckIn)) {
                    NotificationManager.showError("Lỗi",
                            "Ngày trả của phòng " + ph.getSoPhongDisplay() + " phải sau ngày nhận!");
                    return;
                }

                LocalDateTime ldtNhan = pickers[0].getLocalDateTime();
                LocalDateTime ldtTra = pickers[1].getLocalDateTime();
                if (!new dao.PhongDAO().isRoomAvailable(ph.getMaPhong(), ldtNhan, ldtTra)) {
                    NotificationManager.showError("Lỗi", "Phòng " + ph.getSoPhongDisplay()
                            + " đã có lịch đặt hoặc đang sử dụng trong khoảng thời gian này!");
                    return;
                }
            }

            // 2. Lưu thông tin khách hàng trước
            KhachHang kh = khService.getByPhone(sdt);
            if (kh == null) {
                kh = new KhachHang();
                kh.setHoTen(hoTen);
                kh.setSoDienThoai(sdt);
                kh.setCccd(cccd);
                kh.setQuocTich((String) cboQuocTich.getSelectedItem());
                khService.them(kh);
                kh = khService.getByPhone(sdt);
            } else {
                if ("Ngừng".equals(kh.getTrangThai())) {
                    khService.activate(kh.getMaKhachHang());
                    kh = khService.getByPhone(sdt);
                }
                if (!cccd.isEmpty() && (kh.getCccd() == null || kh.getCccd().isBlank())) {
                    kh.setCccd(cccd);
                    String suaErr = khService.sua(kh);
                    if (suaErr != null) {
                        NotificationManager.showError("Lỗi khách hàng", suaErr);
                        return;
                    }
                }
            }

            // Tính toán tổng tiền thực tế trên tất cả phòng
            double overallTotal = 0;
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                Phong ph = selectedRoomsList.get(i);
                ui.components.DateTimePicker[] pickers = roomDates.get(i);
                Date rCheckIn = pickers[0].getDate();
                Date rCheckOut = pickers[1].getDate();
                long rNights = new ThuePhongService().tinhSoNgay(rCheckIn, rCheckOut);
                double base = bangGiaService.layGiaVaoThoiDiem(
                        ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "",
                        rCheckIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                overallTotal += base * rNights;
            }

            double totalCoc = 0;
            String selCoc = (String) cboCoc.getSelectedItem();
            if (selCoc != null) {
                if (selCoc.startsWith("30%")) {
                    totalCoc = overallTotal * 0.3;
                } else if (selCoc.startsWith("50%")) {
                    totalCoc = overallTotal * 0.5;
                } else if (selCoc.startsWith("100%")) {
                    totalCoc = overallTotal;
                } else if (selCoc.startsWith("Nhập số tiền")) {
                    String rawVal = txtTienCocTay.getText().trim();
                    if (rawVal.isEmpty()) {
                        NotificationManager.showError("Lỗi đặt cọc", "Vui lòng nhập số tiền đặt cọc!");
                        return;
                    }
                    String cleanVal = rawVal.replace(".", "").replace(",", "").replace("đ", "").replace("đ", "").trim();
                    try {
                        totalCoc = Double.parseDouble(cleanVal);
                    } catch (Exception ex) {
                        NotificationManager.showError("Lỗi đặt cọc", "Số tiền đặt cọc nhập tay không hợp lệ! Vui lòng chỉ nhập số.");
                        return;
                    }
                    if (totalCoc < 0) {
                        NotificationManager.showError("Lỗi đặt cọc", "Số tiền đặt cọc không được nhỏ hơn 0đ!");
                        return;
                    }
                    if (totalCoc > overallTotal) {
                        NotificationManager.showError("Lỗi đặt cọc", "Số tiền đặt cọc không được vượt quá tổng tiền phòng (" + String.format("%,.0fđ", overallTotal) + ")!");
                        return;
                    }
                } else if (selCoc.startsWith("Theo tỷ lệ cũ")) {
                    if (editingDatPhong != null) {
                        double oldTotal = 0;
                        if (editingDatPhong.getDsChiTiet() != null) {
                            for (ChiTietDatPhong ct : editingDatPhong.getDsChiTiet()) {
                                LocalDateTime rInLDT = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : editingDatPhong.getNgayNhanDuKien();
                                LocalDateTime rOutLDT = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : editingDatPhong.getNgayTraDuKien();
                                Date rCI = java.sql.Timestamp.valueOf(rInLDT);
                                Date rCO = java.sql.Timestamp.valueOf(rOutLDT);
                                long rNights = new ThuePhongService().tinhSoNgay(rCI, rCO);
                                oldTotal += ct.getGiaThucTeChot() * rNights;
                            }
                        }
                        if (oldTotal > 0) {
                            double pct = editingDatPhong.getTienDatCoc() / oldTotal;
                            totalCoc = overallTotal * pct;
                        }
                    }
                }
            }

            double discount = 0;
            boolean isDoan = cboLoaiKhach.getSelectedIndex() == 1;
            if (isDoan) {
                try {
                    discount = Double.parseDouble(txtGiamGia.getText().trim());
                } catch (Exception ignored) {
                }
            }

            // 3. Tạo MỘT đơn đặt phòng duy nhất — ngày cha là MIN check-in / MAX check-out
            LocalDateTime minCheckIn = null, maxCheckOut = null;
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                LocalDateTime rCI = roomDates.get(i)[0].getLocalDateTime();
                LocalDateTime rCO = roomDates.get(i)[1].getLocalDateTime();
                if (minCheckIn == null || rCI.isBefore(minCheckIn))
                    minCheckIn = rCI;
                if (maxCheckOut == null || rCO.isAfter(maxCheckOut))
                    maxCheckOut = rCO;
            }

            if (cboCoc.getSelectedIndex() > 0) {
                LocalDateTime hanCoc = txtHanCoc.getLocalDateTime();
                LocalDateTime now = LocalDateTime.now();
                if (hanCoc.isAfter(minCheckIn)) {
                    NotificationManager.showError("Lỗi hạn nộp cọc", "Hạn nộp cọc phải trước ngày nhận phòng (check-in)!");
                    return;
                }
                if (hanCoc.isBefore(now.minusHours(2))) {
                    NotificationManager.showError("Lỗi hạn nộp cọc", "Hạn nộp cọc không được ở quá khứ!");
                    return;
                }
            }

            DatPhong dp = new DatPhong();
            dp.setKhachHang(kh);
            dp.setNgayNhanDuKien(minCheckIn);
            dp.setNgayTraDuKien(maxCheckOut);
            dp.setSoLuongKhach(soKhach);
            dp.setTienDatCoc(totalCoc);
            dp.setTongTienTamTinh(overallTotal);
            dp.setGhiChu(ghiChuText.isEmpty() ? null : ghiChuText);
            dp.setTrangThai(TrangThaiDatPhong.PENDING);
            dp.setHanNopCoc(txtHanCoc.getLocalDateTime());

            entity.KenhDatPhong kenh = (entity.KenhDatPhong) cboKenh.getSelectedItem();
            dp.setMaKenh(kenh != null ? kenh.getMaKenh() : "DIR");
            String ota = txtOTA.getText().trim();
            dp.setMaXacNhanKenh(ota.isEmpty() ? null : ota);

            dp.setHanCheckIn(minCheckIn.toLocalDate().atTime(18, 0));
            if (dp.getHanCheckIn().isBefore(minCheckIn))
                dp.setHanCheckIn(minCheckIn.plusHours(4));

            dp.setLoaiKhach(isDoan ? "DOAN" : "CA_NHAN");
            if (isDoan) {
                Object selItem = cboTenDoan.getSelectedItem();
                dp.setTenDoan(selItem != null ? selItem.toString().trim() : "");
            }

            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(AuthService.getInstance().getCurrentMaNV());
            dp.setNhanVien(nv);

            String vCode = txtVoucher.getText().trim();
            if (!vCode.isEmpty())
                dp.setMaKhuyenMai(vCode);

            // Tạo ChiTietDatPhong cho từng phòng, mỗi phòng có ngày dự kiến riêng
            final double finalDiscount = discount;
            for (int i = 0; i < selectedRoomsList.size(); i++) {
                Phong ph = selectedRoomsList.get(i);
                LocalDateTime rCheckIn = roomDates.get(i)[0].getLocalDateTime();
                LocalDateTime rCheckOut = roomDates.get(i)[1].getLocalDateTime();

                ChiTietDatPhong ct = new ChiTietDatPhong();
                ct.setPhong(ph);
                ct.setDatPhong(dp);
                // ★ Ngày dự kiến riêng của phòng này (khác nhau khi lệch lịch)
                ct.setNgayNhanDuKien(rCheckIn);
                ct.setNgayTraDuKien(rCheckOut);

                double base = bangGiaService.layGiaVaoThoiDiem(
                        ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "", rCheckIn);
                ct.setGiaThucTeChot(finalDiscount > 0 ? base * (1 - finalDiscount / 100.0) : base);
                dp.getDsChiTiet().add(ct);
            }

            String err;
            if (editingDatPhong != null) {
                // Map the updated values onto editingDatPhong
                editingDatPhong.setKhachHang(kh);
                editingDatPhong.setNgayNhanDuKien(minCheckIn);
                editingDatPhong.setNgayTraDuKien(maxCheckOut);
                editingDatPhong.setSoLuongKhach(soKhach);
                editingDatPhong.setTienDatCoc(totalCoc);
                editingDatPhong.setTongTienTamTinh(overallTotal);
                editingDatPhong.setGhiChu(ghiChuText.isEmpty() ? null : ghiChuText);
                editingDatPhong.setHanNopCoc(txtHanCoc.getLocalDateTime());

                editingDatPhong.setMaKenh(kenh != null ? kenh.getMaKenh() : "DIR");
                editingDatPhong.setMaXacNhanKenh(ota.isEmpty() ? null : ota);

                editingDatPhong.setLoaiKhach(isDoan ? "DOAN" : "CA_NHAN");
                if (isDoan) {
                    editingDatPhong.setTenDoan(cboTenDoan.getSelectedItem() != null ? cboTenDoan.getSelectedItem().toString().trim() : "");
                } else {
                    editingDatPhong.setTenDoan(null);
                }

                editingDatPhong.setMaKhuyenMai(vCode.isEmpty() ? null : vCode);

                // Map ChiTietDatPhong onto editingDatPhong
                editingDatPhong.getDsChiTiet().clear();
                for (int i = 0; i < selectedRoomsList.size(); i++) {
                    Phong ph = selectedRoomsList.get(i);
                    LocalDateTime rCheckIn = roomDates.get(i)[0].getLocalDateTime();
                    LocalDateTime rCheckOut = roomDates.get(i)[1].getLocalDateTime();

                    ChiTietDatPhong ct = new ChiTietDatPhong();
                    ct.setPhong(ph);
                    ct.setDatPhong(editingDatPhong);
                    ct.setNgayNhanDuKien(rCheckIn);
                    ct.setNgayTraDuKien(rCheckOut);

                    double base = bangGiaService.layGiaVaoThoiDiem(
                            ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "", rCheckIn);
                    ct.setGiaThucTeChot(finalDiscount > 0 ? base * (1 - finalDiscount / 100.0) : base);
                    editingDatPhong.getDsChiTiet().add(ct);
                }

                err = datPhongService.capNhatDatPhongToanDien(editingDatPhong);
            } else {
                err = datPhongService.datPhong(dp);
            }

            if (err == null) {
                String successMsg = editingDatPhong != null
                        ? "Đã cập nhật đơn đặt phòng thành công!"
                        : (selectedRoomsList.size() == 1
                                ? "Đặt phòng thành công!"
                                : "Đã đặt " + selectedRoomsList.size() + " phòng trong 1 đơn (mỗi phòng có thể lệch ngày)!");
                NotificationManager.showSuccess("Thành công", successMsg);
                dispose();
                if (onSuccess != null)
                    onSuccess.run();
            } else {
                NotificationManager.showError("Lỗi", "Gặp sự cố khi lưu đơn đặt phòng: " + err);
            }
        });

        btns.add(btnCancel);
        btns.add(btnOk);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(10);

        root.add(formScroll, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);

        if (editingDatPhong != null) {
            if ("DOAN".equals(editingDatPhong.getLoaiKhach())) {
                cboLoaiKhach.setSelectedIndex(1);
                cboTenDoan.setSelectedItem(editingDatPhong.getTenDoan());
                cboTenDoan.setEnabled(true);
                double pctGiam = 0;
                if (editingDatPhong.getDsChiTiet() != null && !editingDatPhong.getDsChiTiet().isEmpty()) {
                    ChiTietDatPhong ct = editingDatPhong.getDsChiTiet().get(0);
                    double base = bangGiaService.layGiaVaoThoiDiem(
                            ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null ? ct.getPhong().getLoaiPhong().getMaLoaiPhong() : "",
                            ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : editingDatPhong.getNgayNhanDuKien());
                    if (base > 0) {
                        pctGiam = Math.round((1.0 - ct.getGiaThucTeChot() / base) * 100.0);
                    }
                }
                if (pctGiam > 0) {
                    txtGiamGia.setText(String.valueOf((int) pctGiam));
                    txtGiamGia.setEnabled(true);
                    txtGiamGia.setBackground(Color.WHITE);
                }
            } else {
                cboLoaiKhach.setSelectedIndex(0);
            }

            spnKhach.setValue(editingDatPhong.getSoLuongKhach());

            if (editingDatPhong.getMaKenh() != null) {
                for (int i = 0; i < cboKenh.getItemCount(); i++) {
                    entity.KenhDatPhong k = cboKenh.getItemAt(i);
                    if (k != null && editingDatPhong.getMaKenh().equals(k.getMaKenh())) {
                        cboKenh.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (editingDatPhong.getMaXacNhanKenh() != null) {
                txtOTA.setText(editingDatPhong.getMaXacNhanKenh());
                txtOTA.setEnabled(true);
                txtOTA.setBackground(Color.WHITE);
            }

            if (editingDatPhong.getHanNopCoc() != null) {
                txtHanCoc.setDate(java.sql.Timestamp.valueOf(editingDatPhong.getHanNopCoc()));
            }

            if (editingDatPhong.getMaKhuyenMai() != null) {
                txtVoucher.setText(editingDatPhong.getMaKhuyenMai());
            }

            if (editingDatPhong.getGhiChu() != null && !editingDatPhong.getGhiChu().isBlank()) {
                txtGhiChu.setText(editingDatPhong.getGhiChu());
                txtGhiChu.setForeground(UIConstants.TEXT_PRIMARY);
            }
        }

        setContentPane(root);

        // Focus first field
        SwingUtilities.invokeLater(() -> {
            txtSDT.requestFocusInWindow();
        });
    }

    private JPanel fieldGroup(String label, JComponent comp) {
        return fieldGroup(label, comp, null);
    }

    private JPanel fieldGroup(String label, JComponent comp, JLabel errLbl) {
        JPanel g = new JPanel(new BorderLayout(0, 2));
        g.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        g.add(lbl, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);
        center.add(comp, BorderLayout.NORTH);
        if (errLbl != null)
            center.add(errLbl, BorderLayout.CENTER);
        g.add(center, BorderLayout.CENTER);
        return g;
    }

    private JLabel inlineErr() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        return l;
    }

    private String formatName(String name) {
        if (name == null || name.isBlank())
            return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1)
                    sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean validateTenKH(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        val = formatName(val);
        txt.setText(val);
        err.setText(" ");
        if (val.isEmpty()) {
            err.setText("Bắt buộc nhập họ tên");
            return false;
        }
        if (!val.matches("^[\\p{L} .'-]{2,50}$")) {
            err.setText("Họ tên 2-50 ký tự, không chứa số");
            return false;
        }
        return true;
    }

    private boolean validateSDT(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        err.setText(" ");
        if (val.isEmpty()) {
            err.setText("Bắt buộc nhập SĐT");
            return false;
        }
        if (!val.matches("^(0[35789])\\d{8}$")) {
            err.setText("SĐT 10 số, đầu 03/05/07/08/09");
            return false;
        }
        return true;
    }

    private boolean validateCCCD(ModernTextField txt, JLabel err) {
        String val = txt.getText().trim();
        err.setText(" ");
        if (!val.isEmpty() && !val.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            err.setText("9/12 số hoặc A1234567");
            return false;
        }
        return true;
    }

    private JPanel modernSectionLabel(String text) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(UIConstants.PRIMARY);
        p.add(lbl, BorderLayout.WEST);
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE2E8F0));
        JPanel sepWrap = new JPanel(new BorderLayout());
        sepWrap.setOpaque(false);
        sepWrap.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        sepWrap.add(sep, BorderLayout.CENTER);
        p.add(sepWrap, BorderLayout.CENTER);
        return p;
    }
}

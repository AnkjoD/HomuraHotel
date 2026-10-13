package ui.dialogs;

import entity.*;
import entity.enums.TrangThaiDatPhong;
import entity.enums.TrangThaiPhong;
import service.*;
import ui.components.*;
import static ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

/**
 * BatchCheckinDialog - Nhận phòng đồng loạt cho đơn đặt nhiều phòng.
 * Cho phép lễ tân check-in tất cả phòng chưa nhận trong một đơn đặt cùng lúc,
 * dùng chung thông tin khách hàng - tương tự hệ thống Opera PMS.
 */
public class BatchCheckinDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final KhachHangService khService = new KhachHangService();
    private final DatPhongService datPhongService = new DatPhongService();
    private final service.BangGiaService bangGiaService = new service.BangGiaService();

    private final DatPhong datPhong;
    private final List<ChiTietDatPhong> pendingRooms; // Rooms not yet checked in
    private final List<ChiTietDatPhong> doneRooms;    // Already done

    private KhachHang selectedKhach;
    private boolean confirmed = false;

    // Form fields
    private ModernTextField txtCCCD, txtTenKH, txtSDT, txtEmail;
    private ModernComboBox<String> cboGioiTinh, cboQuocTich;
    private JLabel errCCCD, errTenKH, errSDT, errEmail;
    private ui.components.DateTimePicker pickerNgayNhan; // Only checkin time is shared; each room has its own checkout date

    // Room list panel
    private JPanel roomsPanel;

    public BatchCheckinDialog(Frame parent, DatPhong dp) {
        super(parent, "Nhận phòng đồng loạt — " + dp.getMaDatPhong(), true);
        this.datPhong = dp;

        pendingRooms = new ArrayList<>();
        doneRooms = new ArrayList<>();

        if (dp.getDsChiTiet() != null) {
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                boolean isDone = ct.isDaThanhToan() || ct.getNgayTraThucTe() != null;
                boolean isCheckedIn = ct.getPhong() != null
                        && ct.getPhong().getTrangThai() == TrangThaiPhong.OCCUPIED
                        && ct.getNgayNhanThucTe() != null;
                if (isDone || isCheckedIn) {
                    doneRooms.add(ct);
                } else {
                    pendingRooms.add(ct);
                }
            }
        }

        // Pre-fill guest info from booking
        if (dp.getMaKH() != null) {
            selectedKhach = khService.getById(dp.getMaKH());
        }

        setSize(720, 750);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // =========================================================================
    // UI BUILDING
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel title = new JLabel("Nhận phòng đồng loạt");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        String subText = pendingRooms.isEmpty()
                ? "Tất cả phòng đã được nhận"
                : pendingRooms.size() + " phòng chờ nhận · " + doneRooms.size() + " phòng đã xử lý";
        JLabel sub = new JLabel(subText);
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(new Color(255, 255, 255, 200));

        // Info badge
        JLabel badge = new JLabel("Đơn: " + datPhong.getMaDatPhong());
        badge.setFont(UIConstants.FONT_SMALL_BOLD);
        badge.setForeground(new Color(255, 255, 255, 180));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(sub);

        header.add(textPanel, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        // === SECTION 1: Room Overview ===
        body.add(sectionLabel("DANH SÁCH PHÒNG TRONG ĐƠN"));
        body.add(Box.createVerticalStrut(8));
        roomsPanel = buildRoomsPanel();
        body.add(roomsPanel);
        body.add(Box.createVerticalStrut(20));

        if (!pendingRooms.isEmpty()) {
            // === SECTION 2: Guest Info ===
            body.add(sectionLabel("THÔNG TIN KHÁCH HÀNG (DÙNG CHUNG CHO TẤT CẢ PHÒNG)"));
            body.add(Box.createVerticalStrut(8));
            body.add(buildGuestForm());
            body.add(Box.createVerticalStrut(20));

            // === SECTION 3: Dates ===
            body.add(sectionLabel("NGÀY NHẬN & TRẢ PHÒNG"));
            body.add(Box.createVerticalStrut(8));
            body.add(buildDatesPanel());
            body.add(Box.createVerticalStrut(20));

            // === SECTION 4: Policy Notice ===
            body.add(buildPolicyCard());
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Pending rooms (to be checked in)
        for (ChiTietDatPhong ct : pendingRooms) {
            panel.add(buildRoomRow(ct, false));
            panel.add(Box.createVerticalStrut(8));
        }

        // Already done rooms
        for (ChiTietDatPhong ct : doneRooms) {
            panel.add(buildRoomRow(ct, true));
            panel.add(Box.createVerticalStrut(8));
        }

        return panel;
    }

    private JPanel buildRoomRow(ChiTietDatPhong ct, boolean isDone) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color bgColor = isDone ? new Color(0xF0FDF4) : UIConstants.PRIMARY_LIGHT;
        Color borderColor = isDone ? new Color(0x86EFAC) : UIConstants.PRIMARY;
        row.setBackground(bgColor);
        row.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, borderColor),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        // Room number badge
        Phong p = ct.getPhong();
        String roomNum = p != null ? "P." + p.getSoPhongDisplay() : "—";
        JLabel lblRoom = new JLabel(roomNum);
        lblRoom.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblRoom.setForeground(isDone ? new Color(0x166534) : UIConstants.PRIMARY);
        lblRoom.setPreferredSize(new Dimension(70, 36));

        // Room info: type + per-room checkout date
        String typeName = p != null ? p.getTenLoaiPhong() : "";
        double gia = p != null && p.getLoaiPhong() != null
                ? bangGiaService.layGiaHienHanh(p.getLoaiPhong().getMaLoaiPhong()) : 0;
        int capacity = p != null ? p.getSucChua() : 0;
        JLabel lblType = new JLabel(typeName + " — " + String.format("%,.0fđ/đêm", gia) + " (Tối đa " + capacity + " người)");
        lblType.setFont(UIConstants.FONT_BODY);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);

        // Per-room checkout date from original booking
        String traDK = "—";
        if (ct.getNgayTraDuKien() != null) {
            traDK = ct.getNgayTraDuKien().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } else if (datPhong.getNgayTraDK_Date() != null) {
            traDK = new java.text.SimpleDateFormat("dd/MM/yyyy")
                    .format(datPhong.getNgayTraDK_Date());
        }
        JLabel lblTra = new JLabel("Trả: " + traDK);
        lblTra.setFont(UIConstants.FONT_SMALL);
        lblTra.setForeground(UIConstants.TEXT_MUTED);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        lblType.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTra.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(lblType);
        info.add(lblTra);

        // Status badge
        String statusText = isDone ? "Đã xử lý" : "Chờ nhận phòng";
        JLabel lblStatus = new JLabel(statusText);
        lblStatus.setFont(UIConstants.FONT_SMALL_BOLD);
        lblStatus.setForeground(isDone ? new Color(0x166534) : UIConstants.WARNING);
        lblStatus.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lblRoom, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(lblStatus, BorderLayout.EAST);
        return row;
    }

    private JPanel buildGuestForm() {
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(new Color(0xF8FAFC));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Form fields
        txtCCCD = new ModernTextField("VD: 001234567890");
        txtTenKH = new ModernTextField("VD: Nguyen Van A");
        txtSDT = new ModernTextField("VD: 0901234567");
        txtEmail = new ModernTextField("VD: email@gmail.com");

        errCCCD = makeErrLabel();
        errTenKH = makeErrLabel();
        errSDT = makeErrLabel();
        errEmail = makeErrLabel();

        // Pre-fill if guest known
        if (selectedKhach != null) {
            txtCCCD.setText(selectedKhach.getCccd() != null ? selectedKhach.getCccd() : "");
            txtTenKH.setText(selectedKhach.getHoTen() != null ? selectedKhach.getHoTen() : "");
            txtSDT.setText(selectedKhach.getSdt() != null ? selectedKhach.getSdt() : "");
            txtEmail.setText(selectedKhach.getEmail() != null ? selectedKhach.getEmail() : "");
        }

        // Search button
        JButton btnFind = new JButton("Tìm khách");
        btnFind.setFont(UIConstants.FONT_SMALL_BOLD);
        btnFind.setBackground(UIConstants.PRIMARY);
        btnFind.setForeground(Color.WHITE);
        btnFind.setBorderPainted(false);
        btnFind.setFocusPainted(false);
        btnFind.setPreferredSize(new Dimension(90, 36));
        btnFind.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFind.addActionListener(e -> {
            String cccd = txtCCCD.getText().trim();
            if (cccd.isEmpty()) {
                errCCCD.setText("Vui lòng nhập CCCD/Passport");
                return;
            }
            KhachHang kh = khService.getByCCCD(cccd);
            if (kh != null) {
                selectedKhach = kh;
                txtTenKH.setText(kh.getHoTen() != null ? kh.getHoTen() : "");
                txtSDT.setText(kh.getSdt() != null ? kh.getSdt() : "");
                txtEmail.setText(kh.getEmail() != null ? kh.getEmail() : "");
                if (cboQuocTich != null && kh.getQuocTich() != null)
                    cboQuocTich.setSelectedItem(kh.getQuocTich());
                if (cboGioiTinh != null && kh.getGioiTinh() != null)
                    cboGioiTinh.setSelectedItem(kh.getGioiTinh());
                NotificationManager.showSuccess("Tìm thấy khách", "Đã điền thông tin: " + kh.getHoTen());
            } else {
                selectedKhach = null;
                NotificationManager.showInfo("Không tìm thấy", "Vui lòng điền thông tin khách mới bên dưới.");
            }
        });

        JPanel cccdRow = new JPanel(new BorderLayout(8, 0));
        cccdRow.setOpaque(false);
        cccdRow.add(txtCCCD, BorderLayout.CENTER);
        cccdRow.add(btnFind, BorderLayout.EAST);

        // Gender + Nationality row
        cboGioiTinh = new ModernComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cboGioiTinh.setPreferredSize(new Dimension(0, 40));
        cboQuocTich = new ModernComboBox<>();
        for (String c : util.CountryList.getCountriesForInput())
            cboQuocTich.addItem(c);
        cboQuocTich.setSelectedItem("Việt Nam");
        cboQuocTich.setPreferredSize(new Dimension(0, 40));

        if (selectedKhach != null) {
            if (selectedKhach.getGioiTinh() != null) cboGioiTinh.setSelectedItem(selectedKhach.getGioiTinh());
            if (selectedKhach.getQuocTich() != null) cboQuocTich.setSelectedItem(selectedKhach.getQuocTich());
        }

        JPanel rowGender = new JPanel(new GridLayout(1, 2, 16, 0));
        rowGender.setOpaque(false);
        rowGender.add(makeFieldBlock("Giới tính", cboGioiTinh, null));
        rowGender.add(makeFieldBlock("Quốc tịch", cboQuocTich, null));

        JPanel rowPhone = new JPanel(new GridLayout(1, 2, 16, 0));
        rowPhone.setOpaque(false);
        rowPhone.add(makeFieldBlock("Số điện thoại *", txtSDT, errSDT));
        rowPhone.add(makeFieldBlock("Email", txtEmail, errEmail));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 12, 0);

        formCard.add(makeFieldBlock("Số CCCD / Passport *", cccdRow, errCCCD), gc);
        gc.gridy++;
        formCard.add(makeFieldBlock("Họ và tên khách hàng *", txtTenKH, errTenKH), gc);
        gc.gridy++;
        formCard.add(rowPhone, gc);
        gc.gridy++;
        formCard.add(rowGender, gc);

        return formCard;
    }

    private JPanel buildDatesPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Chỉ picker ngày NHẬN chung — mỗi phòng giữ ngày TRẢ riêng từ đơn đặt
        Date defaultNhan = datPhong.getNgayNhanDK_Date();
        if (defaultNhan == null) defaultNhan = new Date();

        pickerNgayNhan = new ui.components.DateTimePicker(defaultNhan);

        JPanel left = fieldRow("Thời điểm nhận phòng thực tế (dùng chung) *", pickerNgayNhan);

        // Info box: explain per-room checkout
        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBackground(new Color(0xEFF6FF));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBFDBFE)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel infoTitle = new JLabel("Ngày trả phòng");
        infoTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        infoTitle.setForeground(new Color(0x1E40AF));
        JLabel infoDetail = new JLabel("<html>Mỗi phòng giữ <b>ngày trả riêng</b><br>theo đơn đặt phòng ban đầu.<br>Xem cột &quot;Trả&quot; bên trên.</html>");
        infoDetail.setFont(UIConstants.FONT_SMALL);
        infoDetail.setForeground(new Color(0x1E40AF));
        infoBox.add(infoTitle);
        infoBox.add(Box.createVerticalStrut(4));
        infoBox.add(infoDetail);

        card.add(left, BorderLayout.CENTER);
        card.add(infoBox, BorderLayout.EAST);
        return card;
    }

    private JPanel buildPolicyCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0xFFFBEB));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFCD34D)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Lưu ý khi nhận phòng đồng loạt");
        title.setFont(UIConstants.FONT_SMALL_BOLD);
        title.setForeground(new Color(0x92400E));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] notes = {
                "• Thời điểm nhận phòng và thông tin khách được dùng chung cho " + pendingRooms.size() + " phòng.",
                "• Ngày trả mỗi phòng được giữ nguyên theo đơn đặt (có thể khác nhau giữa các phòng).",
                "• Phụ phí nhận phòng sớm (trước 14:00) sẽ được tính riêng cho từng phòng nếu có.",
                "• Hệ thống sẽ tự động kiểm tra tình trạng phòng trước khi xác nhận."
        };

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        for (String note : notes) {
            JLabel lbl = new JLabel(note);
            lbl.setFont(UIConstants.FONT_SMALL);
            lbl.setForeground(new Color(0x78350F));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(lbl);
            card.add(Box.createVerticalStrut(3));
        }
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));

        JLabel lblCount = new JLabel(pendingRooms.isEmpty()
                ? "Không có phòng nào cần nhận"
                : "Sẽ nhận " + pendingRooms.size() + " phòng cùng lúc");
        lblCount.setFont(UIConstants.FONT_SMALL);
        lblCount.setForeground(UIConstants.TEXT_MUTED);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton btnCancel = new RoundedButton("Đóng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.addActionListener(e -> dispose());

        if (!pendingRooms.isEmpty()) {
            RoundedButton btnCheckin = new RoundedButton(
                    "Nhận " + pendingRooms.size() + " phòng ngay", UIConstants.SUCCESS, Color.WHITE);
            btnCheckin.setFont(UIConstants.FONT_BODY_BOLD);
            btnCheckin.setPreferredSize(new Dimension(220, 40));
            btnCheckin.addActionListener(e -> doBatchCheckin());
            btnPanel.add(btnCancel);
            btnPanel.add(btnCheckin);
        } else {
            btnPanel.add(btnCancel);
        }

        footer.add(lblCount, BorderLayout.WEST);
        footer.add(btnPanel, BorderLayout.EAST);
        return footer;
    }

    // =========================================================================
    // HELPER UI METHODS
    // =========================================================================

    private JLabel makeErrLabel() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(UIConstants.DANGER);
        return lbl;
    }

    private JPanel makeFieldBlock(String labelText, JComponent comp, JLabel errLbl) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(3));
        p.add(comp);
        if (errLbl != null) {
            errLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(errLbl);
        }
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel fieldRow(String label, JComponent comp) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(comp);
        return p;
    }

    // =========================================================================
    // BATCH CHECK-IN LOGIC
    // =========================================================================

    private void doBatchCheckin() {
        // Validate guest info
        String cccd = txtCCCD.getText().trim();
        String tenKH = txtTenKH.getText().trim();
        String sdt = txtSDT.getText().trim();
        String email = txtEmail.getText().trim();

        boolean hasError = false;
        if (cccd.isEmpty() || !cccd.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            errCCCD.setText("CCCD/Passport không hợp lệ (9 số, 12 số, hoặc dạng A1234567)");
            hasError = true;
        } else {
            errCCCD.setText(" ");
        }
        if (tenKH.isEmpty() || !tenKH.matches("^[\\p{L} .'-]{2,50}$")) {
            errTenKH.setText("Vui lòng nhập họ tên hợp lệ (2-50 ký tự)");
            hasError = true;
        } else {
            errTenKH.setText(" ");
        }
        if (sdt.isEmpty() || !sdt.matches("^(0[35789])\\d{8}$")) {
            errSDT.setText("SĐT không hợp lệ (10 số, bắt đầu 03-09)");
            hasError = true;
        } else {
            errSDT.setText(" ");
        }
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            errEmail.setText("Email không hợp lệ");
            hasError = true;
        } else {
            errEmail.setText(" ");
        }

        if (hasError) {
            NotificationManager.showError("Thông tin chưa hợp lệ", "Vui lòng kiểm tra lại các trường bắt buộc!");
            return;
        }

        java.util.Date ngayNhan = pickerNgayNhan.getDate();
        if (ngayNhan == null) {
            NotificationManager.showWarning("Lỗi", "Vui lòng chọn thời điểm nhận phòng!");
            return;
        }

        java.time.LocalDateTime ldtNhan = ngayNhan.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

        // Early check-in fee warning
        LocalDateTime now = LocalDateTime.now();
        double earlyFeePerRoom = 0;
        String policyLabel = "";
        if (now.getHour() < 14) {
            // Calculate for first pending room as reference
            if (!pendingRooms.isEmpty() && pendingRooms.get(0).getPhong() != null) {
                earlyFeePerRoom = thuePhongService.tinhPhuPhiEarlyCheckin(
                        pendingRooms.get(0).getPhong(), now);
            }
            if (earlyFeePerRoom > 0) {
                policyLabel = now.getHour() < 5 ? "Trước 05:00 (100%)"
                        : now.getHour() < 9 ? "05:00–09:00 (50%)"
                        : "09:00–12:00 (30%)";
                int confirm = JOptionPane.showConfirmDialog(this,
                        "<html><b style='color:red;'>CẢNH BÁO NHẬN PHÒNG SỚM</b><br>"
                                + "Thời điểm nhận: <b>" + now.getHour() + "h" + now.getMinute() + "</b><br>"
                                + "Chính sách: " + policyLabel + "<br>"
                                + "Phụ phí sớm mỗi phòng: <b>" + String.format("%,.0f VNĐ", earlyFeePerRoom) + "</b><br>"
                                + "Tổng phụ phí cho <b>" + pendingRooms.size() + "</b> phòng: <b>"
                                + String.format("%,.0f VNĐ", earlyFeePerRoom * pendingRooms.size()) + "</b><br><br>"
                                + "Bạn có muốn tiếp tục nhận phòng đồng loạt?</html>",
                        "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
        }

        // Resolve or create guest
        KhachHang kh = selectedKhach;
        if (kh == null) kh = khService.getByCCCD(cccd);
        if (kh == null) {
            KhachHang newKh = new KhachHang();
            newKh.setHoTen(formatName(tenKH));
            newKh.setSdt(sdt);
            newKh.setCccd(cccd);
            newKh.setEmail(email);
            newKh.setQuocTich(cboQuocTich.getSelectedItem() != null ? cboQuocTich.getSelectedItem().toString() : "Việt Nam");
            newKh.setGioiTinh(cboGioiTinh.getSelectedItem() != null ? cboGioiTinh.getSelectedItem().toString() : "Nam");
            String errKh = khService.them(newKh);
            if (errKh != null) {
                NotificationManager.showError("Lỗi", "Không thể tạo khách: " + errKh);
                return;
            }
            kh = khService.getByCCCD(cccd);
        } else {
            // Update existing
            kh.setHoTen(formatName(tenKH));
            kh.setSdt(sdt);
            kh.setEmail(email);
            if (cboQuocTich.getSelectedItem() != null) kh.setQuocTich(cboQuocTich.getSelectedItem().toString());
            if (cboGioiTinh.getSelectedItem() != null) kh.setGioiTinh(cboGioiTinh.getSelectedItem().toString());
            khService.sua(kh);
        }

        if (kh == null) {
            NotificationManager.showError("Lỗi", "Không thể xác định thông tin khách hàng!");
            return;
        }

        // Update DatPhong status to CHECKED_IN before proceeding
        datPhong.setTrangThai(TrangThaiDatPhong.CHECKED_IN);
        datPhongService.sua(datPhong);

        // Batch check-in each pending room
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (ChiTietDatPhong ct : pendingRooms) {
            Phong phong = ct.getPhong();
            if (phong == null) {
                errors.add("Phòng không xác định — bỏ qua");
                continue;
            }

            // Build ChiTietDatPhong for checkin
            ChiTietDatPhong checkinCt = new ChiTietDatPhong();
            checkinCt.setPhong(phong);
            checkinCt.setDatPhong(datPhong);
            checkinCt.setNgayNhanThucTe(ldtNhan);
            checkinCt.setKhachHang(kh);
            double gia = phong.getLoaiPhong() != null
                    ? bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()) : 0;
            checkinCt.setGiaThucTeChot(gia);

            // Copy existing maChiTiet if we are updating
            if (ct.getMaChiTiet() != null && !ct.getMaChiTiet().isBlank()) {
                checkinCt.setMaChiTiet(ct.getMaChiTiet());
            }

            String err = thuePhongService.checkIn(checkinCt);
            if (err != null) {
                errors.add("P." + phong.getSoPhongDisplay() + ": " + err);
            } else {
                successCount++;
                // Add early check-in fee as service if applicable
                final double fee = earlyFeePerRoom;
                if (fee > 0) {
                    SuDungDichVu sddv = new SuDungDichVu();
                    DichVu dv = new DichVu();
                    dv.setMaDichVu("DV_EXTRA_EARLY");
                    dv.setTenDichVu("Phụ phí nhận phòng sớm");
                    sddv.setDichVu(dv);
                    sddv.setSoLuong(1);
                    sddv.setDonGiaLuu(fee);
                    sddv.setThoiDiem(now);
                    thuePhongService.themDichVu(sddv);
                }
            }
        }

        confirmed = successCount > 0;

        if (errors.isEmpty()) {
            NotificationManager.showSuccess(
                    "Nhận phòng đồng loạt thành công",
                    "Đã nhận " + successCount + "/" + pendingRooms.size() + " phòng thành công!");
        } else {
            String errMsg = successCount + " phòng thành công.\nLỗi:\n- " + String.join("\n- ", errors);
            if (successCount > 0) {
                NotificationManager.showWarning("Nhận phòng có lỗi", errMsg);
            } else {
                NotificationManager.showError("Thất bại", errMsg);
            }
        }

        if (confirmed) dispose();
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}

package ui.dialogs;

import service.DatPhongService;
import service.KhachHangService;
import service.ThuePhongService;
import service.DichVuService;
import service.PhongService;
import entity.*;
import entity.enums.TrangThaiDatPhong;
import entity.enums.TrangThaiPhong;
import ui.components.DateTimePicker;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class CheckinDialog extends JDialog {

    private final ThuePhongService  thuePhongService = new ThuePhongService();
    private final DichVuService     dichVuService    = new DichVuService();
    private final KhachHangService  khService        = new KhachHangService();
    private final PhongService      phongService     = new PhongService();
    private final DatPhongService   datPhongService  = new DatPhongService();

    private Phong      selectedPhong;
    private KhachHang  selectedKhach;
    private DatPhong   selectedDatPhong;
    private JComboBox<Phong> cboPhong;
    private JLabel     lblInfo;

    private int     currentStep = 1;
    private static final int TOTAL_STEPS = 5;
    private JPanel  stepContent;
    private JLabel  lblStepInfo;
    private boolean confirmed = false;

    // Step inputs
    private JTextField    txtTenKH, txtSDT, txtCCCD, txtEmail;
    private JLabel        errTenKH, errSDT, errCCCD, errEmail;
    private DateTimePicker pickerNgayNhan, pickerNgayTra;
    private JSpinner      spnSoKhach;
    private JComboBox<String> cboDV;
    private JSpinner      spnSLDV;
    private JPanel        dvListPanel;
    private JLabel        lblTongDV;
    private java.util.List<DichVu>  allDichVu = new ArrayList<>();
    private final java.util.List<SuDungDichVu> selectedDV = new ArrayList<>();

    // Bottom bar buttons
    private RoundedButton btnBack, btnNext;

    public CheckinDialog(Frame parent, Phong phong) {
        super(parent,
            "Nhan phong (Check-in)" + (phong != null ? " - P." + phong.getSoPhong() : ""),
            true);
        this.selectedPhong = phong;
        setSize(660, 620);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        buildUI();
    }

    public CheckinDialog(Frame parent, Phong phong, KhachHang kh, DatPhong dp) {
        this(parent, phong);
        this.selectedKhach = kh;
        this.selectedDatPhong = dp;

        if (kh != null) {
            if (txtTenKH != null) txtTenKH.setText(kh.getHoTen());
            if (txtSDT != null) txtSDT.setText(kh.getSoDienThoai());
            if (txtCCCD != null) txtCCCD.setText(kh.getCccd());
            if (txtEmail != null) txtEmail.setText(kh.getEmail() != null ? kh.getEmail() : "");
        }
        if (dp != null) {
            if (pickerNgayNhan != null) pickerNgayNhan.setDate(dp.getNgayNhanDK_Date() != null ? dp.getNgayNhanDK_Date() : new Date());
            if (pickerNgayTra != null) pickerNgayTra.setDate(dp.getNgayTraDK_Date() != null ? dp.getNgayTraDK_Date() : new Date(System.currentTimeMillis() + 86_400_000L));
            if (spnSoKhach != null) spnSoKhach.setValue(dp.getSoLuongKhach() > 0 ? dp.getSoLuongKhach() : 1);
        }
    }

    // =====================================================================
    // BUILD UI
    // =====================================================================
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildTopBar(),    BorderLayout.NORTH);
        stepContent = new JPanel(new CardLayout());
        stepContent.setBackground(Color.WHITE);
        stepContent.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 32));
        stepContent.add(buildStep1(), "1");
        stepContent.add(buildStep2(), "2");
        stepContent.add(buildStep3(), "3");
        stepContent.add(buildStep4(), "4");
        stepContent.add(buildStep5(), "5");
        add(stepContent,      BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        showStep(1);
    }

    // ---- Top bar: title + stepper ----
    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(20, 32, 4, 32));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel("Nhan phong - Check-in");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        String info = selectedPhong != null
            ? "P." + selectedPhong.getSoPhong() + " - " + selectedPhong.getTenLoaiPhong()
              + " - " + String.format("%,.0fd/dem", (double) selectedPhong.getGiaTheoNgay())
            : "Chon phong de bat dau";
        lblInfo = new JLabel(info);
        lblInfo.setFont(UIConstants.FONT_BODY);
        lblInfo.setForeground(UIConstants.TEXT_SECONDARY);
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblInfo);
        top.add(titlePanel, BorderLayout.WEST);

        top.add(buildStepper(), BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildStepper() {
        String[] names = {"Phong", "Khach hang", "Ngay & Gio", "Dich vu", "Xac nhan"};
        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        stepper.setOpaque(false);

        for (int i = 0; i < TOTAL_STEPS; i++) {
            final int step = i + 1;

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean done   = step < currentStep;
                    boolean active = step == currentStep;
                    Color bg = (done || active) ? UIConstants.PRIMARY : new Color(0xCBD5E1);
                    g2.setColor(bg);
                    g2.fillOval(0, 0, 28, 28);
                    g2.setColor(Color.WHITE);
                    if (done) {
                        // Ve dau check bang duong ke
                        g2.setStroke(new java.awt.BasicStroke(2.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                        g2.drawLine(8, 14, 12, 18);
                        g2.drawLine(12, 18, 20, 10);
                    } else {
                        g2.setFont(UIConstants.FONT_SMALL_BOLD);
                        String t = String.valueOf(step);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(t, (28 - fm.stringWidth(t)) / 2, (28 + fm.getAscent() - fm.getDescent()) / 2);
                    }
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(28, 28));

            JLabel lbl = new JLabel(names[i], SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_SMALL);
            lbl.setForeground(step == currentStep ? UIConstants.PRIMARY : UIConstants.TEXT_MUTED);

            JPanel item = new JPanel();
            item.setOpaque(false);
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            dot.setAlignmentX(Component.CENTER_ALIGNMENT);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            item.add(dot);
            item.add(Box.createVerticalStrut(2));
            item.add(lbl);
            stepper.add(item);

            if (i < TOTAL_STEPS - 1) {
                JPanel line = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        g.setColor(step < currentStep ? UIConstants.PRIMARY : new Color(0xCBD5E1));
                        g.fillRect(0, getHeight() / 2 - 1, getWidth(), 2);
                    }
                };
                line.setOpaque(false);
                line.setPreferredSize(new Dimension(55, 28));
                stepper.add(line);
            }
        }
        return stepper;
    }

    // =====================================================================
    // STEPS
    // =====================================================================
    private JPanel buildStep1() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JLabel title = new JLabel("Thong tin phong"); title.setFont(UIConstants.FONT_HEADER);
        JLabel sub   = new JLabel("Xac nhan phong truoc khi tien hanh check-in");
        sub.setFont(UIConstants.FONT_SMALL); sub.setForeground(UIConstants.TEXT_MUTED);

        JPanel phongCard = new JPanel(new BorderLayout());
        phongCard.setBackground(UIConstants.PRIMARY_LIGHT);
        phongCard.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.CARD_RADIUS, UIConstants.PRIMARY),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        if (selectedPhong == null) {
            java.util.List<Phong> rooms = phongService.getAllPhong();
            rooms.removeIf(r -> r.getTrangThai() == TrangThaiPhong.OCCUPIED);
            cboPhong = new JComboBox<>(rooms.toArray(new Phong[0]));
            cboPhong.setFont(UIConstants.FONT_BODY);
            cboPhong.setPreferredSize(new Dimension(0, 34));
            cboPhong.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Phong) {
                        Phong ph = (Phong) value;
                        String view = ph.getView();
                        lbl.setText(String.format("P.%s - %s (%s) - %s", ph.getSoPhong(), ph.getTenLoaiPhong(), view, ph.getTrangThai()));
                    }
                    return lbl;
                }
            });
            if (!rooms.isEmpty()) {
                cboPhong.setSelectedIndex(0);
                selectedPhong = rooms.get(0);
            }
            cboPhong.addActionListener(e -> {
                selectedPhong = (Phong) cboPhong.getSelectedItem();
                refreshHeaderInfo();
            });

            JPanel selectRow = new JPanel(new BorderLayout(6, 0));
            selectRow.setOpaque(false);
            selectRow.add(new JLabel("Chon phong:"), BorderLayout.WEST);
            selectRow.add(cboPhong, BorderLayout.CENTER);
            JPanel filling = new JPanel(); filling.setOpaque(false); filling.setLayout(new BoxLayout(filling, BoxLayout.Y_AXIS));
            filling.add(selectRow);
            filling.add(Box.createVerticalStrut(12));
            phongCard.add(filling, BorderLayout.NORTH);
        }

        if (selectedPhong != null) {
            JLabel num   = new JLabel("Phong " + selectedPhong.getSoPhong()); num.setFont(new Font("Segoe UI",Font.BOLD,20));
            JLabel type  = new JLabel(selectedPhong.getTenLoaiPhong() + " - Tang " + selectedPhong.getTang() + " - " + selectedPhong.getSucChua() + " nguoi");
            type.setFont(UIConstants.FONT_BODY); type.setForeground(UIConstants.TEXT_SECONDARY);
            JLabel price = new JLabel(String.format("%,.0f d / dem", (double) selectedPhong.getGiaTheoNgay()));
            price.setFont(new Font("Segoe UI",Font.BOLD,15)); price.setForeground(UIConstants.PRIMARY);
            JLabel view  = new JLabel("View: " + selectedPhong.getView());
            view.setFont(UIConstants.FONT_SMALL); view.setForeground(UIConstants.TEXT_MUTED);
            JLabel desc  = selectedPhong.getMoTa()!=null ? new JLabel(selectedPhong.getMoTa()) : new JLabel("");
            desc.setFont(UIConstants.FONT_SMALL); desc.setForeground(UIConstants.TEXT_MUTED);

            JPanel info = new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
            info.add(num); info.add(Box.createVerticalStrut(4)); info.add(type); info.add(view); info.add(desc); info.add(Box.createVerticalStrut(8)); info.add(price);
            phongCard.add(info, BorderLayout.CENTER);
        }

        // Checklist
        JPanel checklist = new JPanel(); checklist.setOpaque(false); checklist.setLayout(new BoxLayout(checklist,BoxLayout.Y_AXIS));
        checklist.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblChk = new JLabel("KIEM TRA TRUOC KHI NHAN KHACH"); lblChk.setFont(UIConstants.FONT_SMALL_BOLD); lblChk.setForeground(UIConstants.TEXT_SECONDARY);
        lblChk.setAlignmentX(Component.LEFT_ALIGNMENT);
        checklist.add(Box.createVerticalStrut(12)); checklist.add(lblChk); checklist.add(Box.createVerticalStrut(6));
        for (String c : new String[]{"Phong sach se, ve sinh day du", "Dieu hoa, TV, den hoat dong binh thuong", "Minibar day du", "Khan tam, ga giuong da thay moi"}) {
            JCheckBox cb = new JCheckBox(c); cb.setFont(UIConstants.FONT_BODY); cb.setOpaque(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            checklist.add(cb);
        }

        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        JPanel hdr = new JPanel(); hdr.setOpaque(false); hdr.setLayout(new BoxLayout(hdr,BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        hdr.add(title); hdr.add(Box.createVerticalStrut(2)); hdr.add(sub);
        phongCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hdr); content.add(Box.createVerticalStrut(14)); content.add(phongCard); content.add(checklist);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStep2() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel title = new JLabel("Thong tin khach hang"); title.setFont(UIConstants.FONT_HEADER);
        JLabel sub   = new JLabel("Nhap CCCD/Passport de tim khach cu, hoac dien thong tin khach moi");
        sub.setFont(UIConstants.FONT_SMALL); sub.setForeground(UIConstants.TEXT_MUTED);

        txtCCCD  = styledFieldFocus("VD: 001234567890");
        txtTenKH = styledFieldFocus("VD: Nguyen Van A");
        txtSDT   = styledFieldFocus("VD: 0901234567");
        txtEmail = styledFieldFocus("VD: email@gmail.com");

        errCCCD  = inlineErr(); errTenKH = inlineErr();
        errSDT   = inlineErr(); errEmail = inlineErr();

        txtTenKH.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateTenKH(); }
        });
        txtSDT.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateSDT(); }
        });
        txtCCCD.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateCCCD(); }
        });
        txtEmail.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateEmail(); }
        });

        if (selectedKhach != null) {
            txtCCCD.setText(selectedKhach.getCccd());
            txtTenKH.setText(selectedKhach.getHoTen());
            txtSDT.setText(selectedKhach.getSoDienThoai() != null ? selectedKhach.getSoDienThoai() : "");
            txtEmail.setText(selectedKhach.getEmail() != null ? selectedKhach.getEmail() : "");
        }

        JButton btnFind = new JButton("Tim khach");
        btnFind.setFont(UIConstants.FONT_SMALL_BOLD);
        btnFind.setBackground(UIConstants.PRIMARY); btnFind.setForeground(Color.WHITE);
        btnFind.setBorderPainted(false); btnFind.setFocusPainted(false);
        btnFind.setPreferredSize(new Dimension(90, 34));
        btnFind.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel findResultCard = new JPanel(new BorderLayout());
        findResultCard.setBackground(new Color(0xF0FDF4));
        findResultCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x86EFAC), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        findResultCard.setVisible(false);
        JLabel findResultLbl = new JLabel();
        findResultLbl.setFont(UIConstants.FONT_BODY);
        findResultLbl.setForeground(new Color(0x166534));
        findResultCard.add(findResultLbl, BorderLayout.CENTER);

        btnFind.addActionListener(e -> {
            String cccd = txtCCCD.getText().trim();
            errCCCD.setText(" ");
            if (cccd.isEmpty()) { errCCCD.setText("Vui lòng nhập CCCD/Passport trước khi tìm"); return; }
            KhachHang kh = khService.getByCCCD(cccd);
            if (kh != null) {
                selectedKhach = kh;
                txtTenKH.setText(kh.getHoTen());
                txtSDT.setText(kh.getSdt() != null ? kh.getSdt() : "");
                txtEmail.setText(kh.getEmail() != null ? kh.getEmail() : "");
                errTenKH.setText(" "); errSDT.setText(" "); errEmail.setText(" ");
                findResultLbl.setText("[OK] Tìm thấy: " + kh.getHoTen() + " - Đã điền tự động");
                findResultCard.setBackground(new Color(0xF0FDF4));
                findResultCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x86EFAC), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                findResultLbl.setForeground(new Color(0x166534));
            } else {
                selectedKhach = null;
                findResultLbl.setText("Không tìm thấy khách. Vui lòng điền thông tin bên dưới.");
                findResultCard.setBackground(new Color(0xFFFBEB));
                findResultCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xFCD34D), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                findResultLbl.setForeground(new Color(0x92400E));
            }
            findResultCard.setVisible(true);
            findResultCard.revalidate();
        });

        JPanel cccdRow = new JPanel(new BorderLayout(6, 0)); cccdRow.setOpaque(false);
        cccdRow.add(txtCCCD, BorderLayout.CENTER); cccdRow.add(btnFind, BorderLayout.EAST);

        // Form card - gon nhe, khong co hint de tiet kiem khong gian
        JPanel formCard = new JPanel();
        formCard.setBackground(new Color(0xF8FAFC));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(fieldBlock("CCCD / Passport",     cccdRow,   errCCCD,  null, false));
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(fieldBlock("Ho va ten *",          txtTenKH, errTenKH, null, true));
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(fieldBlock("So dien thoai *",      txtSDT,   errSDT,   null, true));
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(fieldBlock("Email",                txtEmail, errEmail, null, false));

        // Dung JScrollPane de dam bao khong bi cat
        JPanel scrollContent = new JPanel(); scrollContent.setOpaque(false);
        scrollContent.setBackground(Color.WHITE);
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        JPanel hdr = new JPanel(); hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        hdr.add(title); hdr.add(Box.createVerticalStrut(2)); hdr.add(sub);
        findResultCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollContent.add(hdr);
        scrollContent.add(Box.createVerticalStrut(8));
        scrollContent.add(findResultCard);
        scrollContent.add(Box.createVerticalStrut(8));
        scrollContent.add(formCard);

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStep3() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JLabel title = new JLabel("Ngay & Gio luu tru"); title.setFont(UIConstants.FONT_HEADER);
        JLabel sub   = new JLabel("Nhan vao o ngay de mo lich chon");
        sub.setFont(UIConstants.FONT_SMALL); sub.setForeground(UIConstants.TEXT_MUTED);

        pickerNgayNhan = new DateTimePicker(selectedDatPhong != null && selectedDatPhong.getNgayNhanDK_Date() != null
            ? selectedDatPhong.getNgayNhanDK_Date() : new Date());
        pickerNgayTra  = new DateTimePicker(selectedDatPhong != null && selectedDatPhong.getNgayTraDK_Date() != null
            ? selectedDatPhong.getNgayTraDK_Date() : new Date(System.currentTimeMillis() + 86_400_000L));

        spnSoKhach = new JSpinner(new SpinnerNumberModel(
            selectedDatPhong != null && selectedDatPhong.getSoKhach() > 0 ? selectedDatPhong.getSoKhach() : 1,
            1, 10, 1));
        spnSoKhach.setFont(UIConstants.FONT_BODY);

        JPanel fields = new JPanel(new GridLayout(3, 1, 0, 14)); fields.setOpaque(false);
        fields.add(fieldRow("Ngay & gio nhan phong *",    pickerNgayNhan));
        fields.add(fieldRow("Ngay & gio tra (du kien) *", pickerNgayTra));
        fields.add(fieldRow("So khach",                   spnSoKhach));

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel hdr = new JPanel(); hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.add(title); hdr.add(Box.createVerticalStrut(2)); hdr.add(sub);
        content.add(hdr); content.add(Box.createVerticalStrut(18)); content.add(fields);
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildStep4() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16,0,0,0));
        JLabel title = new JLabel("Them dich vu (neu co)"); title.setFont(UIConstants.FONT_HEADER);

        allDichVu = dichVuService.getActive();
        String[] names = allDichVu.stream().map(DichVu::getTenDV).toArray(String[]::new);
        cboDV  = new JComboBox<>(names.length > 0 ? names : new String[]{"Khong co dich vu"}); cboDV.setFont(UIConstants.FONT_BODY);
        spnSLDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)); spnSLDV.setFont(UIConstants.FONT_BODY);
        spnSLDV.setPreferredSize(new Dimension(70, 34));

        JButton btnAddDV = new JButton("+ Them"); btnAddDV.setFont(UIConstants.FONT_SMALL_BOLD);
        btnAddDV.setBackground(UIConstants.SUCCESS); btnAddDV.setForeground(Color.WHITE);
        btnAddDV.setBorderPainted(false); btnAddDV.setFocusPainted(false);
        btnAddDV.addActionListener(e -> {
            int idx = cboDV.getSelectedIndex();
            if (idx < 0 || allDichVu.isEmpty()) return;
            DichVu dv = allDichVu.get(idx);
            int sl = (int) spnSLDV.getValue();
            SuDungDichVu sddv = new SuDungDichVu();
            sddv.setDichVu(dv);
            sddv.setSoLuong(sl);
            sddv.setDonGiaLuu(dv.getGia());
            sddv.setThoiDiem(java.time.LocalDateTime.now());
            selectedDV.add(sddv);
            refreshDVList();
        });

        JPanel addRow = new JPanel(new BorderLayout(8, 0)); addRow.setOpaque(false);
        addRow.add(cboDV, BorderLayout.CENTER);
        JPanel slBtn = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); slBtn.setOpaque(false);
        JLabel lblSL = new JLabel("SL:"); lblSL.setFont(UIConstants.FONT_BODY);
        slBtn.add(lblSL); slBtn.add(spnSLDV); slBtn.add(btnAddDV);
        addRow.add(slBtn, BorderLayout.EAST);

        dvListPanel = new JPanel(); dvListPanel.setOpaque(false); dvListPanel.setLayout(new BoxLayout(dvListPanel, BoxLayout.Y_AXIS));
        lblTongDV   = new JLabel("Tong dich vu: 0d"); lblTongDV.setFont(UIConstants.FONT_BODY_BOLD); lblTongDV.setForeground(UIConstants.PRIMARY);

        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.add(title); content.add(Box.createVerticalStrut(16)); content.add(addRow);
        content.add(Box.createVerticalStrut(12)); content.add(dvListPanel);
        content.add(Box.createVerticalStrut(8)); content.add(lblTongDV);
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private void refreshDVList() {
        dvListPanel.removeAll();
        double total = 0;
        for (int i = 0; i < selectedDV.size(); i++) {
            SuDungDichVu sddv = selectedDV.get(i);
            JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            String dvName = sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dich vu";
            JLabel info = new JLabel("* " + dvName + "  x" + sddv.getSoLuong()); info.setFont(UIConstants.FONT_BODY);
            JLabel amt  = new JLabel(String.format("%,.0fd", sddv.tinhThanhTien())); amt.setFont(UIConstants.FONT_BODY_BOLD); amt.setForeground(UIConstants.PRIMARY);
            final int idx = i;
            JButton del = new JButton("X"); del.setFont(UIConstants.FONT_SMALL); del.setForeground(UIConstants.DANGER);
            del.setBorderPainted(false); del.setContentAreaFilled(false); del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            del.addActionListener(e -> { selectedDV.remove(idx); refreshDVList(); });
            JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); r.setOpaque(false); r.add(amt); r.add(del);
            row.add(info, BorderLayout.WEST); row.add(r, BorderLayout.EAST);
            dvListPanel.add(row);
            total += sddv.tinhThanhTien();
        }
        lblTongDV.setText(String.format("Tong dich vu: %,.0fd", total));
        dvListPanel.revalidate(); dvListPanel.repaint();
    }

    private JPanel buildStep5() {
        JPanel panel = new JPanel(new BorderLayout()); panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel title = new JLabel("Xac nhan nhan phong"); title.setFont(UIConstants.FONT_HEADER);
        JLabel sub   = new JLabel("Kiem tra lai thong tin truoc khi hoan tat check-in");
        sub.setFont(UIConstants.FONT_SMALL); sub.setForeground(UIConstants.TEXT_MUTED);

        // Doc du lieu tu cac field hien tai
        String phongInfo  = selectedPhong  != null ? "P." + selectedPhong.getSoPhong()  + " - " + selectedPhong.getTenLoaiPhong()  : "Chua chon";
        String giaInfo    = selectedPhong  != null ? String.format("%,.0f d/dem", (double) selectedPhong.getGiaTheoNgay()) : "--";
        String khachInfo  = txtTenKH != null && !txtTenKH.getText().isBlank() ? txtTenKH.getText().trim() : "--";
        String sdtInfo    = txtSDT   != null && !txtSDT.getText().isBlank()   ? txtSDT.getText().trim()   : "--";
        String cccdInfo   = txtCCCD  != null && !txtCCCD.getText().isBlank()  ? txtCCCD.getText().trim()  : "--";
        String emailInfo  = txtEmail != null && !txtEmail.getText().isBlank() ? txtEmail.getText().trim() : "--";

        JPanel card = new JPanel();
        card.setBackground(new Color(0xF8FAFC));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header card - ve dau check bang icon
        JPanel readyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        readyRow.setOpaque(false);
        readyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Icon check xanh
        JPanel checkIcon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.SUCCESS);
                g2.fillOval(0, 0, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.drawLine(6, 11, 9, 15);
                g2.drawLine(9, 15, 16, 7);
                g2.dispose();
            }
        };
        checkIcon.setOpaque(false);
        checkIcon.setPreferredSize(new Dimension(22, 22));
        JLabel readyTxt = new JLabel("San sang check-in");
        readyTxt.setFont(new Font("Segoe UI", Font.BOLD, 15));
        readyTxt.setForeground(UIConstants.SUCCESS);
        readyRow.add(checkIcon);
        readyRow.add(readyTxt);

        card.add(readyRow);
        card.add(Box.createVerticalStrut(12));
        card.add(sepLine());
        card.add(Box.createVerticalStrut(10));

        card.add(summaryRow("Phong",       phongInfo,  UIConstants.PRIMARY));
        card.add(Box.createVerticalStrut(4));
        card.add(summaryRow("Gia/dem",     giaInfo,    UIConstants.SUCCESS));
        card.add(Box.createVerticalStrut(10));
        card.add(sepLine());
        card.add(Box.createVerticalStrut(10));
        card.add(summaryRow("Khach hang",  khachInfo,  new Color(0x1E2337)));
        card.add(Box.createVerticalStrut(4));
        card.add(summaryRow("SDT",         sdtInfo,    new Color(0x374151)));
        card.add(Box.createVerticalStrut(4));
        card.add(summaryRow("CCCD",        cccdInfo,   new Color(0x374151)));
        card.add(Box.createVerticalStrut(4));
        card.add(summaryRow("Email",       emailInfo,  new Color(0x374151)));

        if (!selectedDV.isEmpty()) {
            card.add(Box.createVerticalStrut(10));
            card.add(sepLine());
            card.add(Box.createVerticalStrut(8));
            double totalDV = 0;
            for (SuDungDichVu s : selectedDV) {
                String dvLine = s.getDichVu() != null
                    ? s.getDichVu().getTenDV() + " x" + s.getSoLuong()
                      + "  =  " + String.format("%,.0f d", s.tinhThanhTien())
                    : "Dich vu";
                card.add(summaryRow("DV", dvLine, new Color(0x374151)));
                card.add(Box.createVerticalStrut(3));
                totalDV += s.tinhThanhTien();
            }
            card.add(summaryRow("Tong DV", String.format("%,.0f d", totalDV), UIConstants.PRIMARY));
        }

        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel hdr = new JPanel(); hdr.setOpaque(false); hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.add(title); hdr.add(Box.createVerticalStrut(2)); hdr.add(sub);
        hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hdr); content.add(Box.createVerticalStrut(14)); content.add(card);
        panel.add(content, BorderLayout.NORTH);
        return panel;
    }

    private JSeparator sepLine() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(0xE2E8F0));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JPanel summaryRow(String key, String val, Color valColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel k = new JLabel(key); k.setFont(UIConstants.FONT_SMALL_BOLD); k.setForeground(UIConstants.TEXT_MUTED);
        k.setPreferredSize(new Dimension(88, 18));
        JLabel v = new JLabel(val); v.setFont(UIConstants.FONT_BODY); v.setForeground(valColor);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.CENTER);
        return row;
    }

    // =====================================================================
    // BOTTOM BAR & NAVIGATION
    // =====================================================================
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(12, 32, 12, 32)));

        lblStepInfo = new JLabel();
        lblStepInfo.setFont(UIConstants.FONT_SMALL); lblStepInfo.setForeground(UIConstants.TEXT_MUTED);

        btnBack = new RoundedButton("Huy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnNext = new RoundedButton("Tiep theo >>", UIConstants.PRIMARY, Color.WHITE);

        btnBack.addActionListener(e -> { if (currentStep > 1) showStep(currentStep - 1); else dispose(); });
        btnNext.addActionListener(e -> { if (currentStep < TOTAL_STEPS) showStep(currentStep + 1); else doCheckin(); });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); btns.setOpaque(false);
        btns.add(btnBack); btns.add(btnNext);

        bar.add(lblStepInfo, BorderLayout.WEST); bar.add(btns, BorderLayout.EAST);
        return bar;
    }

    private void showStep(int step) {
        currentStep = step;

        // Rebuild step 5 moi lan navigate den de cap nhat du lieu moi nhat
        if (step == TOTAL_STEPS) {
            for (java.awt.Component c : stepContent.getComponents()) {
                if ("step5".equals(c.getName())) {
                    stepContent.remove(c);
                    break;
                }
            }
            JPanel newStep5 = buildStep5();
            newStep5.setName("step5");
            stepContent.add(newStep5, String.valueOf(TOTAL_STEPS));
        }

        ((CardLayout) stepContent.getLayout()).show(stepContent, String.valueOf(step));
        refreshHeaderInfo();

        btnBack.setText(step == 1 ? "Huy" : "<< Quay lai");
        btnNext.setText(step == TOTAL_STEPS ? "Xac nhan Check-in" : "Tiep theo >>");

        if (step == TOTAL_STEPS) {
            btnNext.setBackground(UIConstants.SUCCESS);
        } else {
            btnNext.setBackground(UIConstants.PRIMARY);
        }
        if (lblStepInfo != null) lblStepInfo.setText("Buoc " + step + "/" + TOTAL_STEPS);
        stepContent.revalidate();
        stepContent.repaint();
        repaint();
    }

    private boolean validateTenKH() {
        if (txtTenKH == null || errTenKH == null) return true;
        String tenKH = txtTenKH.getText().trim();
        errTenKH.setText(" ");
        if (tenKH.isEmpty()) {
            errTenKH.setText("Bắt buộc - Vui lòng nhập họ và tên khách hàng");
            return false;
        } else if (!tenKH.matches("^[\\p{L} .'-]{2,50}$")) {
            errTenKH.setText("Họ tên không hợp lệ (2-50 ký tự, chỉ gồm chữ và dấu cách)");
            return false;
        }
        return true;
    }

    private boolean validateSDT() {
        if (txtSDT == null || errSDT == null) return true;
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) {
            errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại");
            return false;
        } else if (!sdt.matches("^(0[35789])\\d{8}$")) {
            errSDT.setText("SĐT không đúng định dạng (10 số, bắt đầu 03/05/07/08/09)");
            return false;
        }
        return true;
    }

    private boolean validateCCCD() {
        if (txtCCCD == null || errCCCD == null) return true;
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (!cccd.isEmpty() && !cccd.matches("^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$")) {
            errCCCD.setText("CCCD/Passport không hợp lệ (9 số, 12 số hoặc dạng A1234567)");
            return false;
        }
        return true;
    }

    private boolean validateEmail() {
        if (txtEmail == null || errEmail == null) return true;
        String email = txtEmail.getText().trim();
        errEmail.setText(" ");
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            errEmail.setText("Email không đúng định dạng (VD: ten@gmail.com)");
            return false;
        }
        return true;
    }

    private void refreshHeaderInfo() {
        if (lblInfo == null) return;
        String info = selectedPhong != null
            ? "P." + selectedPhong.getSoPhong() + " - " + selectedPhong.getTenLoaiPhong()
              + " - " + String.format("%,.0fd/dem", (double) selectedPhong.getGiaTheoNgay())
            : "Chon phong de bat dau";
        lblInfo.setText(info);
    }

    // =====================================================================
    // DO CHECK-IN
    // =====================================================================
    private void doCheckin() {
        String tenKH = txtTenKH != null ? txtTenKH.getText().trim() : "";
        String sdt   = txtSDT   != null ? txtSDT.getText().trim()   : "";
        String cccd  = txtCCCD  != null ? txtCCCD.getText().trim()  : "";
        String email = txtEmail != null ? txtEmail.getText().trim()  : "";

        boolean vTen = validateTenKH();
        boolean vSdt = validateSDT();
        boolean vCccd = validateCCCD();
        boolean vEmail = validateEmail();
        
        if (!vTen || !vSdt || !vCccd || !vEmail) {
            showStep(2); return;
        }
        if (selectedPhong == null) { JOptionPane.showMessageDialog(this, "Chua co phong duoc chon!"); return; }

        Date ngayNhan = pickerNgayNhan.getDate();
        Date ngayTra  = pickerNgayTra.getDate();
        if (!ngayTra.after(ngayNhan)) {
            JOptionPane.showMessageDialog(this, "Ngay tra phai sau ngay nhan!");
            showStep(3);
            return;
        }
        int soKhach = (int) spnSoKhach.getValue();

        // Tao / tim khach hang
        if (selectedKhach == null) {
            KhachHang kh = new KhachHang();
            kh.setHoTen(tenKH); kh.setSdt(sdt);
            kh.setCccd(txtCCCD != null ? txtCCCD.getText().trim() : "");
            String err = khService.them(kh);
            if (err != null) { JOptionPane.showMessageDialog(this, "Loi tao khach: " + err); return; }
            selectedKhach = khService.getByCCCD(kh.getCccd());
            if (selectedKhach == null) { selectedKhach = kh; }
        } else {
            selectedKhach.setTrangThai("Dang o");
            khService.sua(selectedKhach);
        }

        ChiTietDatPhong ct = new ChiTietDatPhong();
        ct.setPhong(selectedPhong);

        java.time.LocalDateTime ldtNhan = ngayNhan.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        java.time.LocalDateTime ldtTra  = ngayTra.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

        if (selectedDatPhong == null) {
            DatPhong dp = new DatPhong();
            dp.setMaDatPhong(datPhongService.generateMaDatPhong());
            dp.setKhachHang(selectedKhach);
            dp.setNgayDat(java.time.LocalDateTime.now());
            dp.setNgayNhanDuKien(ldtNhan);
            dp.setNgayTraDuKien(ldtTra);
            dp.setSoLuongKhach(soKhach);
            dp.setTrangThai(TrangThaiDatPhong.CHECKED_IN);

            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(service.AuthService.getInstance().getCurrentMaNV());
            dp.setNhanVien(nv);

            String errDp = datPhongService.them(dp);
            if (errDp != null) {
                JOptionPane.showMessageDialog(this, "Loi tao dat phong: " + errDp);
                return;
            }
            selectedDatPhong = dp;
        } else {
            selectedDatPhong.setTrangThai(TrangThaiDatPhong.CHECKED_IN);
            String errDp = datPhongService.sua(selectedDatPhong);
            if (errDp != null) {
                JOptionPane.showMessageDialog(this, "Loi cap nhat dat phong: " + errDp);
                return;
            }
        }

        ct.setDatPhong(selectedDatPhong);
        ct.setNgayNhanThucTe(ldtNhan);
        ct.setGiaThucTeChot(selectedPhong.getGiaTheoNgay());

        String err = thuePhongService.checkIn(ct);
        if (err != null) { JOptionPane.showMessageDialog(this, "Loi check-in: " + err); return; }

        java.util.List<String> failedServices = new ArrayList<>();
        for (SuDungDichVu sddv : selectedDV) {
            String errDv = thuePhongService.themDichVu(
                ct.getMaChiTiet(),
                sddv.getDichVu() != null ? sddv.getDichVu().getMaDV() : null,
                sddv.getSoLuong(),
                sddv.getDonGiaLuu()
            );
            if (errDv != null) {
                String dvName = sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dich vu";
                failedServices.add(dvName + ": " + errDv);
            }
        }

        if (!failedServices.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Check-in thanh cong nhung mot so dich vu chua luu duoc:\n- " + String.join("\n- ", failedServices),
                "Canh bao",
                JOptionPane.WARNING_MESSAGE);
        }

        confirmed = true;

        JOptionPane.showMessageDialog(this,
            "Check-in thanh cong!\nMa phieu: " + ct.getMaChiTiet(),
            "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }

    // ---- Helpers ----
    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(UIConstants.FONT_BODY);
        f.setToolTipText(placeholder);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private JTextField styledFieldFocus(String tooltip) {
        JTextField f = styledField(tooltip);
        final javax.swing.border.Border normal = f.getBorder();
        final javax.swing.border.Border focused = BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.PRIMARY),
            BorderFactory.createEmptyBorder(4, 8, 4, 8));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { f.setBorder(focused); }
            @Override public void focusLost(java.awt.event.FocusEvent  e) { f.setBorder(normal);  }
        });
        return f;
    }

    private JPanel fieldBlock(String label, JComponent comp, JLabel err, JLabel hintLbl, boolean required) {
        JPanel g = new JPanel(); g.setOpaque(false);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cap = new JLabel(label);
        cap.setFont(UIConstants.FONT_SMALL_BOLD);
        cap.setForeground(new Color(0x374151));
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (hintLbl != null) hintLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (err != null)     err.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.add(cap);
        g.add(Box.createVerticalStrut(4));
        g.add(comp);
        if (hintLbl != null) g.add(hintLbl);
        if (err != null)     g.add(err);
        return g;
    }

    private JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setForeground(new Color(0x94A3B8));
        return l;
    }

    private JLabel inlineErr() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        return l;
    }

    private JPanel fieldRow(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3)); p.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setPreferredSize(new Dimension(160, 16));
        p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER);
        return p;
    }
}


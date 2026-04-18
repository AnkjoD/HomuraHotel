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
import ui.components.NotificationManager;
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
    private final service.BangGiaService bangGiaService = new service.BangGiaService();

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
    private ModernComboBox<String> cboQuocTich;
    private JLabel        errTenKH, errSDT, errCCCD, errEmail;
    private DateTimePicker pickerNgayNhan, pickerNgayTra;
    private JSpinner      spnSoKhach;
    private JComboBox<String> cboDV;
    private JSpinner      spnSLDV;
    private JPanel        dvListPanel;
    private JLabel        lblTongDV;
    private java.util.List<DichVu>  allDichVu = new ArrayList<>();
    private final java.util.List<SuDungDichVu> selectedDV = new ArrayList<>();
    
    // ID Document capture
    private String selectedImagePath;
    private JLabel lblImagePreview;

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 2) + "..";
    }

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
            if (kh.getEmail() != null) txtEmail.setText(kh.getEmail());
            if (kh.getQuocTich() != null && cboQuocTich != null) cboQuocTich.setSelectedItem(kh.getQuocTich());
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
              + " - " + String.format("%,.0fd/dem", (double) bangGiaService.layGiaHienHanh(selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : ""))
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
                        double gia = bangGiaService.layGiaHienHanh(ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "");
                        lbl.setText(String.format("P.%s - %s - %,.0fd", ph.getSoPhong(), ph.getTenLoaiPhong(), gia));
                        if (index == -1) lbl.setForeground(UIConstants.PRIMARY);
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
            double giaHH = bangGiaService.layGiaHienHanh(selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : "");
            JLabel price = new JLabel(String.format("%,.0f d / dem", giaHH));
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
                if (kh.getQuocTich() != null) cboQuocTich.setSelectedItem(kh.getQuocTich());
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

        cboQuocTich = new ModernComboBox<>(new String[]{"Việt Nam", "Quốc tế"});
        formCard.add(fieldBlock("Quoc tich",            cboQuocTich, null, null, false));
        formCard.add(Box.createVerticalStrut(8));
        
        formCard.add(fieldBlock("Email",                txtEmail, errEmail, null, false));
        formCard.add(Box.createVerticalStrut(12));

        // Identity Verification Image Section
        JLabel lblIdTitle = new JLabel("XAC MINH DANH TINH (ANH CCCD/PASSPORT)");
        lblIdTitle.setFont(UIConstants.FONT_SMALL_BOLD); lblIdTitle.setForeground(UIConstants.TEXT_SECONDARY);
        formCard.add(lblIdTitle);
        formCard.add(Box.createVerticalStrut(8));

        JPanel idBox = new JPanel(new BorderLayout(12, 0));
        idBox.setOpaque(true); idBox.setBackground(Color.WHITE);
        idBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        lblImagePreview = new JLabel("Chua co anh", SwingConstants.CENTER);
        lblImagePreview.setPreferredSize(new Dimension(100, 64));
        lblImagePreview.setOpaque(true); lblImagePreview.setBackground(new Color(0xF1F5F9));
        lblImagePreview.setFont(UIConstants.FONT_SMALL);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 1, true));

        RoundedButton btnUpload = new RoundedButton("Chup / Tai len anh", UIConstants.PRIMARY, Color.WHITE);
        btnUpload.setPreferredSize(new Dimension(140, 32));
        btnUpload.addActionListener(e -> chooseCCCDImage());

        JPanel idActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4)); idActions.setOpaque(false);
        idActions.add(btnUpload);
        JLabel idHint = new JLabel("Dinh dang: JPG, PNG. Yeu cau ro net."); 
        idHint.setFont(UIConstants.FONT_SMALL); idHint.setForeground(UIConstants.TEXT_MUTED);
        idActions.add(idHint);

        idBox.add(lblImagePreview, BorderLayout.WEST);
        idBox.add(idActions, BorderLayout.CENTER);
        formCard.add(idBox);

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

        // Sức chứa tiêu chuẩn + 2 người (giường phụ/nệm) = tối đa tuyệt đối
        int maxPax = selectedPhong != null ? selectedPhong.getSucChua() + 2 : 10;
        int initVal = selectedDatPhong != null && selectedDatPhong.getSoKhach() > 0 
            ? Math.min(selectedDatPhong.getSoKhach(), maxPax) : 1;
        spnSoKhach = new JSpinner(new SpinnerNumberModel(initVal, 1, maxPax, 1));
        spnSoKhach.setFont(UIConstants.FONT_BODY);
        spnSoKhach.setToolTipText("Sức chứa tiêu chuẩn: " + (selectedPhong != null ? selectedPhong.getSucChua() : "?") 
            + " | Tối đa cho phép: " + maxPax);

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
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        allDichVu = dichVuService.getActive();

        // --- LEFT: Service Catalog (Grid of Cards) ---
        JPanel catalogPanel = new JPanel(new BorderLayout(0, 10));
        catalogPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh muc dich vu");
        lblTitle.setFont(UIConstants.FONT_HEADER);
        catalogPanel.add(lblTitle, BorderLayout.NORTH);

        // Container for cards
        JPanel grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        grid.setOpaque(false);
        // We'll set a preferred width to force wrapping in the scroll pane
        grid.setPreferredSize(new Dimension(310, (allDichVu.size() / 2 + 1) * 110));

        for (DichVu dv : allDichVu) {
            grid.add(createServiceCard(dv));
        }

        JScrollPane scrollCatalog = new JScrollPane(grid);
        scrollCatalog.setBorder(null);
        scrollCatalog.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollCatalog.setOpaque(false);
        scrollCatalog.getViewport().setOpaque(false);
        scrollCatalog.getVerticalScrollBar().setUnitIncrement(12);
        catalogPanel.add(scrollCatalog, BorderLayout.CENTER);

        // --- RIGHT: Shopping Cart (Sidebar) ---
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBackground(new Color(0xF8FAFC));
        cartPanel.setPreferredSize(new Dimension(240, 0));
        cartPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Modern Header for Cart
        JPanel cartHdr = new JPanel(new BorderLayout());
        cartHdr.setOpaque(false);
        cartHdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel lblCartTitle = new JLabel("Giỏ hàng");
        lblCartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCartTitle.setForeground(new Color(0x334155));
        
        lblTongDV = new JLabel("0đ");
        lblTongDV.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTongDV.setForeground(UIConstants.PRIMARY);
        
        cartHdr.add(lblCartTitle, BorderLayout.WEST);
        cartHdr.add(lblTongDV, BorderLayout.EAST);
        cartPanel.add(cartHdr, BorderLayout.NORTH);

        dvListPanel = new JPanel();
        dvListPanel.setOpaque(false);
        dvListPanel.setLayout(new BoxLayout(dvListPanel, BoxLayout.Y_AXIS));
        
        JScrollPane scrollCart = new JScrollPane(dvListPanel);
        scrollCart.setOpaque(false);
        scrollCart.getViewport().setOpaque(false);
        scrollCart.setBorder(null);
        cartPanel.add(scrollCart, BorderLayout.CENTER);

        // Bottom action (optional, can stay empty or add clear all)
        JPanel cartBottom = new JPanel(new BorderLayout());
        cartBottom.setOpaque(false);
        cartPanel.add(cartBottom, BorderLayout.SOUTH);

        panel.add(catalogPanel, BorderLayout.CENTER);
        panel.add(cartPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createServiceCard(DichVu dv) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setPreferredSize(new Dimension(140, 95));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xEDF2F7)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // Info
        String name = dv.getTenDV();
        JLabel lblName = new JLabel("<html><body style='width: 100px;'>" + name + "</body></html>");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(new Color(0x1A202C));

        JLabel lblPrice = new JLabel(String.format("%,.0fd", dv.getDonGia()));
        lblPrice.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPrice.setForeground(UIConstants.SUCCESS);

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        info.add(lblName);
        info.add(lblPrice);

        // Add Button
        JButton btnAdd = new JButton("Them");
        btnAdd.setFont(UIConstants.FONT_SMALL_BOLD);
        btnAdd.setBackground(UIConstants.PRIMARY);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> addOrIncrementDV(dv, 1));

        card.add(info, BorderLayout.CENTER);
        card.add(btnAdd, BorderLayout.SOUTH);

        return card;
    }

    private void addOrIncrementDV(DichVu dv, int sl) {
        // Check if already in cart
        for (SuDungDichVu sddv : selectedDV) {
            String mid = sddv.getDichVu() != null ? sddv.getDichVu().getMaDV() : "";
            if (mid.equals(dv.getMaDV())) {
                sddv.setSoLuong(sddv.getSoLuong() + sl);
                refreshDVList();
                return;
            }
        }
        // New item
        SuDungDichVu sddv = new SuDungDichVu();
        sddv.setDichVu(dv);
        sddv.setSoLuong(sl);
        sddv.setDonGiaLuu(dv.getGia());
        sddv.setThoiDiem(java.time.LocalDateTime.now());
        selectedDV.add(sddv);
        refreshDVList();
    }

    private void refreshDVList() {
        dvListPanel.removeAll();
        double total = 0;
        
        for (int i = 0; i < selectedDV.size(); i++) {
            final int idx = i;
            SuDungDichVu sddv = selectedDV.get(i);
            
            // Row container - Compact height 42px
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF1F5F9)));

            // Left Stack: Name & Subtotal
            JPanel left = new JPanel(new GridLayout(2, 1, 0, 0));
            left.setOpaque(false);
            
            JLabel lblName = new JLabel(truncate(sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ", 20));
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblName.setForeground(new Color(0x334155));
            
            JLabel lblSub = new JLabel(String.format("%,.0f đ", sddv.tinhThanhTien()));
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblSub.setForeground(UIConstants.PRIMARY);
            
            left.add(lblName);
            left.add(lblSub);

            // Right Stack: Pill Qty Picker & Delete
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

            // Modern Pill Picker
            JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            pill.setBackground(Color.WHITE);
            pill.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0), 1));
            
            JButton btnMin = createPillBtn("-", false);
            JLabel lblQty = new JLabel(String.valueOf(sddv.getSoLuong()), SwingConstants.CENTER);
            lblQty.setPreferredSize(new Dimension(28, 22));
            lblQty.setFont(new Font("Segoe UI", Font.BOLD, 11));
            JButton btnPlus = createPillBtn("+", true);

            btnMin.addActionListener(e -> { if (sddv.getSoLuong() > 1) { sddv.setSoLuong(sddv.getSoLuong() - 1); } else { selectedDV.remove(idx); } refreshDVList(); });
            btnPlus.addActionListener(e -> { sddv.setSoLuong(sddv.getSoLuong() + 1); refreshDVList(); });

            pill.add(btnMin);
            pill.add(lblQty);
            pill.add(btnPlus);

            // Simple Delete Icon
            JButton btnDel = new JButton("\u00d7");
            btnDel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            btnDel.setForeground(new Color(0x94A3B8));
            btnDel.setBorder(null);
            btnDel.setOpaque(false);
            btnDel.setContentAreaFilled(false);
            btnDel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDel.addActionListener(e -> { selectedDV.remove(idx); refreshDVList(); });
            btnDel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { btnDel.setForeground(new Color(0xEF4444)); }
                public void mouseExited(java.awt.event.MouseEvent e) { btnDel.setForeground(new Color(0x94A3B8)); }
            });

            actions.add(pill);
            actions.add(btnDel);

            row.add(left, BorderLayout.CENTER);
            row.add(actions, BorderLayout.EAST);

            dvListPanel.add(row);
            total += sddv.tinhThanhTien();
        }

        dvListPanel.add(Box.createVerticalGlue());

        if (lblTongDV != null) {
            lblTongDV.setText(String.format("%,.0f đ", total));
        }

        if (selectedDV.isEmpty()) {
            JLabel empty = new JLabel("Giỏ hàng trống", SwingConstants.CENTER);
            empty.setFont(UIConstants.FONT_SMALL);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            dvListPanel.add(Box.createVerticalStrut(60));
            dvListPanel.add(empty);
        }

        dvListPanel.revalidate();
        dvListPanel.repaint();
    }

    private JButton createPillBtn(String text, boolean plus) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(22, 22));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setBackground(new Color(0xF8FAFC));
        btn.setForeground(new Color(0x64748B));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(new Color(0xF1F5F9)); btn.setForeground(UIConstants.PRIMARY); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(new Color(0xF8FAFC)); btn.setForeground(new Color(0x64748B)); }
        });
        return btn;
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
                String dvName = s.getDichVu() != null ? s.getDichVu().getTenDV() : "DV";
                card.add(summaryRow(dvName + " x" + s.getSoLuong(), 
                    String.format("%,.0f d", s.tinhThanhTien()), UIConstants.TEXT_SECONDARY));
                totalDV += s.tinhThanhTien();
            }
            card.add(Box.createVerticalStrut(10));
            card.add(summaryRow("Tổng dịch vụ", String.format("%,.0f d", totalDV), UIConstants.PRIMARY));
            
            double giaHH = bangGiaService.layGiaHienHanh(selectedPhong != null && selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : "");
            
            card.add(summaryRow("TIEN PHONG (TAM TINH)", String.format("%,.0f d", giaHH), UIConstants.TEXT_PRIMARY));
            card.add(summaryRow("TIEN DICH VU", String.format("%,.0f d", totalDV), UIConstants.TEXT_PRIMARY));
            card.add(Box.createVerticalStrut(15));
            card.add(new JSeparator());
            card.add(Box.createVerticalStrut(12));
            card.add(summaryRow("T?NG THANH TOAN", String.format("%,.0f d", giaHH + totalDV), UIConstants.PRIMARY));
        } else if (selectedPhong != null) {
            double giaHH = bangGiaService.layGiaHienHanh(selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : "");
            card.add(summaryRow("T?NG THANH TOAN", String.format("%,.0f d", giaHH), UIConstants.PRIMARY));
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
        btnNext.addActionListener(e -> { 
            // ----- Tích hợp Logic Phụ Thu Ở Ghép / Giường Phụ (Bước 3) -----
            if (currentStep == 3) {
                int soKhach = (int) spnSoKhach.getValue();
                if (selectedPhong != null) {
                    int sucChuaTieuChuan = selectedPhong.getSucChua(); // Sức chứa tiêu chuẩn
                    if (soKhach > sucChuaTieuChuan + 2) {
                        // Hard Block: Không cho phép thuê quá đông so với tiêu chuẩn an toàn PCCC
                        NotificationManager.showError("Quá tải sức chứa", 
                            "LỖI: Sức chứa của phòng tối đa chỉ cho phép " + (sucChuaTieuChuan + 2) + " người.\n"
                            + "Vui lòng nâng hạng phòng!");
                        return;
                    } else if (soKhach > sucChuaTieuChuan) {
                        // Soft Warning + Tự động thêm phụ thu
                        int extra = soKhach - sucChuaTieuChuan;
                        int confirm = JOptionPane.showConfirmDialog(this,
                            "CẢNH BÁO QUY ĐỊNH:\n"
                            + "Phòng đang chọn có sức chứa tiêu chuẩn là " + sucChuaTieuChuan + " người.\n"
                            + "Dữ liệu nhập: " + soKhach + " người (Vượt mức " + extra + " người).\n\n"
                            + "Hệ thống sẽ TỰ ĐỘNG thêm phí phụ thu " + extra + " người lớn vào danh sách dịch vụ.\n"
                            + "Bạn có thể điều chỉnh (người lớn/trẻ em) ở Bước 4 - Dịch vụ.\n\n"
                            + "Bạn có muốn tiếp tục?",
                            "Phụ thu ghép người", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                        // Tự động thêm dịch vụ "Phụ thu người lớn" (DV009) nếu chưa có
                        autoAddSurcharge(extra);
                    }
                }
            }
            
            if (currentStep < TOTAL_STEPS) showStep(currentStep + 1); 
            else doCheckin(); 
        });

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

    private boolean validateTenKH() {
        if (txtTenKH == null || errTenKH == null) return true;
        String tenKH = txtTenKH.getText().trim();
        tenKH = formatName(tenKH);
        txtTenKH.setText(tenKH);
        
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
              + " - " + String.format("%,.0fd/dem", (double) bangGiaService.layGiaHienHanh(selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : ""))
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
        if (selectedPhong == null) { NotificationManager.showWarning("Cảnh báo", "Chưa có phòng được chọn!"); return; }

        Date ngayNhan = pickerNgayNhan.getDate();
        Date ngayTra  = pickerNgayTra.getDate();
        if (!ngayTra.after(ngayNhan)) {
            NotificationManager.showWarning("Lỗi ngày tháng", "Ngày trả phải sau ngày nhận!");
            showStep(3);
            return;
        }
        int soKhach = (int) spnSoKhach.getValue();

        // Tao / tim khach hang
        if (selectedKhach == null) {
            KhachHang kh = new KhachHang();
            kh.setHoTen(tenKH); kh.setSdt(sdt);
            kh.setCccd(txtCCCD != null ? txtCCCD.getText().trim() : "");
            kh.setQuocTich(cboQuocTich.getSelectedItem().toString());
            
            // Luu anh CCCD neu co
            String savedPath = saveCCCDImage(kh.getMaKhachHang() != null ? kh.getMaKhachHang() : "NEW_GUEST");
            kh.setAnhCCCD(savedPath);

            String err = khService.them(kh);
            if (err != null) { NotificationManager.showError("Lỗi", "Lỗi tạo khách: " + err); return; }
            selectedKhach = khService.getByCCCD(kh.getCccd());
            if (selectedKhach == null) { selectedKhach = kh; }
        } else {
            // Cap nhat anh CCCD moi neu duoc chon
            if (selectedImagePath != null) {
                String savedPath = saveCCCDImage(selectedKhach.getMaKhachHang());
                selectedKhach.setAnhCCCD(savedPath);
            }
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
                NotificationManager.showError("Lỗi", "Lỗi tạo đặt phòng: " + errDp);
                return;
            }
            selectedDatPhong = dp;
        } else {
            // Logic cho khách đoàn/nhiều phòng: Chỉ chuyển hẳn sang CHECKED_IN khi tất cả phòng đã nhận
            boolean allCheckedIn = true;
            if (selectedDatPhong.getDsChiTiet() != null && selectedDatPhong.getDsChiTiet().size() > 1) {
                for (ChiTietDatPhong item : selectedDatPhong.getDsChiTiet()) {
                    if (item.getPhong().getMaPhong().equals(selectedPhong.getMaPhong())) continue;
                    
                    Phong p = phongService.getPhongById(item.getPhong().getMaPhong());
                    if (p != null && p.getTrangThai() != entity.enums.TrangThaiPhong.OCCUPIED) {
                        allCheckedIn = false;
                        break;
                    }
                }
            }
            
            selectedDatPhong.setTrangThai(allCheckedIn ? TrangThaiDatPhong.CHECKED_IN : TrangThaiDatPhong.PARTIALLY_CHECKED_IN);
            String errDp = datPhongService.sua(selectedDatPhong);
            if (errDp != null) {
                NotificationManager.showError("Lỗi", "Lỗi cập nhật trạng thái đơn đặt: " + errDp);
                return;
            }
        }

        ct.setDatPhong(selectedDatPhong);
        ct.setNgayNhanThucTe(ldtNhan);
        ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(selectedPhong.getLoaiPhong() != null ? selectedPhong.getLoaiPhong().getMaLoaiPhong() : ""));
        ct.setKhachHang(selectedKhach); // Gán người lưu trú thực tế cho phòng này

        String err = thuePhongService.checkIn(ct);
        if (err != null) { NotificationManager.showError("Lỗi", "Lỗi check-in: " + err); return; }

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
            NotificationManager.showWarning("Cảnh báo dịch vụ",
                "Check-in thành công nhưng một số dịch vụ chưa lưu được:\n- " + String.join("\n- ", failedServices));
        }

        confirmed = true;
        NotificationManager.showSuccess("Check-in thành công", "Phòng " + selectedPhong.getSoPhong() + " đã bắt đầu lưu trú.");
        dispose();
    }

    private void chooseCCCDImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chon anh CCCD/Passport");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            selectedImagePath = file.getAbsolutePath();
            
            // Preview thumbnail
            try {
                ImageIcon icon = new ImageIcon(selectedImagePath);
                Image img = icon.getImage().getScaledInstance(100, 64, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(img));
                lblImagePreview.setText("");
            } catch (Exception e) {
                lblImagePreview.setText("Loi anh");
            }
        }
    }

    private String saveCCCDImage(String maKH) {
        if (selectedImagePath == null) return null;
        try {
            java.io.File source = new java.io.File(selectedImagePath);
            String ext = selectedImagePath.substring(selectedImagePath.lastIndexOf("."));
            String fileName = maKH + "_CCCD" + ext;
            java.nio.file.Path destDir = java.nio.file.Paths.get("img", "customers");
            if (!java.nio.file.Files.exists(destDir)) java.nio.file.Files.createDirectories(destDir);
            
            java.nio.file.Path destPath = destDir.resolve(fileName);
            java.nio.file.Files.copy(source.toPath(), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "img/customers/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isConfirmed() { return confirmed; }

    /**
     * Tự động thêm dịch vụ "Phụ thu người lớn" vào selectedDV khi số khách vượt sức chứa.
     * Nếu đã có sẵn dịch vụ phụ thu, cập nhật số lượng thay vì thêm mới.
     */
    private void autoAddSurcharge(int extraPeople) {
        // Tìm dịch vụ "Phụ thu người lớn" trong danh sách dịch vụ
        DichVu dvPhuThu = null;
        for (DichVu dv : allDichVu) {
            if (dv.getTenDV() != null && dv.getTenDV().contains("Phụ thu người lớn")) {
                dvPhuThu = dv;
                break;
            }
        }
        if (dvPhuThu == null) {
            // Fallback: tìm bất kỳ dịch vụ có tên chứa "Phụ thu"
            for (DichVu dv : allDichVu) {
                if (dv.getTenDV() != null && dv.getTenDV().toLowerCase().contains("phụ thu")) {
                    dvPhuThu = dv;
                    break;
                }
            }
        }
        if (dvPhuThu == null) return; // Không tìm thấy dịch vụ phụ thu

        // Kiểm tra xem đã có phụ thu trong danh sách chưa
        for (SuDungDichVu sddv : selectedDV) {
            if (sddv.getDichVu() != null && sddv.getDichVu().getTenDV() != null
                    && sddv.getDichVu().getTenDV().contains("Phụ thu")) {
                // Đã có → cập nhật số lượng
                sddv.setSoLuong(extraPeople);
                refreshDVList();
                return;
            }
        }

        // Chưa có → thêm mới
        SuDungDichVu sddv = new SuDungDichVu();
        sddv.setDichVu(dvPhuThu);
        sddv.setSoLuong(extraPeople);
        sddv.setDonGiaLuu(dvPhuThu.getGia());
        sddv.setThoiDiem(java.time.LocalDateTime.now());
        selectedDV.add(sddv);
        refreshDVList();
    }

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


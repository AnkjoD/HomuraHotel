package ui.panels;

import ui.MainFrame;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.*;
import entity.*;
import entity.enums.TrangThaiPhong;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.*;

public class QLHeThongPanel extends JPanel {

    @SuppressWarnings("unused")
    private final MainFrame mainFrame;
    private final PhongService phongSvc = new PhongService();
    private final DichVuService dvSvc = new DichVuService();
    private final BangGiaService bgSvc = new BangGiaService();

    private String currentSub = "";

    public QLHeThongPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        main.add(currentSub.isEmpty() ? buildHub() : buildSubPanel(currentSub), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    // ======================================================
    // HUB
    // ======================================================
    private JPanel buildHub() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JPanel hdr = new JPanel();
        hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel("Chọn một chức năng để quản lý");
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        hdr.add(Box.createVerticalStrut(30));
        hdr.add(lbl);
        hdr.add(Box.createVerticalStrut(30));

        JPanel cards = new JPanel(new GridLayout(1, 4, 20, 0));
        cards.setOpaque(false);
        cards.add(hubCard("Cách tính tiền", "Bảng giá & phụ thu", "banggia", "./icon/save-money.png"));
        cards.add(hubCard("Loại phòng", "Danh mục loại phòng", "loaiphong", "./icon/bed.png"));
        cards.add(hubCard("Phòng", "Quản lý từng phòng", "phong", "./icon/home.png"));
        cards.add(hubCard("Dịch vụ", "Quản lý menu dịch vụ", "dichvu", "./icon/room-service.png"));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(cards);
        panel.add(hdr, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private RoundedPanel hubCard(String title, String desc, String key, String iconPath) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(28, 24, 28, 24));
        card.setPreferredSize(new Dimension(180, 160));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadIconToLabel(iconLabel, iconPath);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(UIConstants.FONT_BODY_BOLD);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel d = new JLabel(desc, SwingConstants.CENTER);
        d.setFont(UIConstants.FONT_SMALL);
        d.setForeground(UIConstants.TEXT_MUTED);
        d.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel ct = new JPanel();
        ct.setOpaque(false);
        ct.setLayout(new BoxLayout(ct, BoxLayout.Y_AXIS));
        ct.add(iconLabel);
        ct.add(Box.createVerticalStrut(12));
        ct.add(t);
        ct.add(Box.createVerticalStrut(4));
        ct.add(d);
        card.add(ct, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navSub(key);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(UIConstants.PRIMARY_LIGHT);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.repaint();
            }
        });
        return card;
    }

    private void navSub(String key) {
        currentSub = key;
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    // ======================================================
    // SUB-PANEL DISPATCHER
    // ======================================================
    private JPanel buildSubPanel(String key) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(buildSubHeader(key), BorderLayout.NORTH);

        JPanel subContent;
        if ("loaiphong".equals(key))
            subContent = buildLoaiPhongSub();
        else if ("phong".equals(key))
            subContent = buildPhongSub();
        else if ("dichvu".equals(key))
            subContent = buildDichVuSub();
        else if ("banggia".equals(key))
            subContent = buildBangGiaSub();
        else
            subContent = new JPanel();
        panel.add(subContent, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSubHeader(String key) {
        // [0]=title, [1]=sub, [2]=addLabel
        String[] m;
        if ("loaiphong".equals(key))
            m = new String[] { "Quản lý loại phòng", "Cấu hình danh mục, tiện nghi và mức giá", "loại phòng" };
        else if ("phong".equals(key))
            m = new String[] { "Quản lý phòng", "Quản lý tình trạng và thông tin từng phòng", "phòng" };
        else if ("dichvu".equals(key))
            m = new String[] { "Quản lý dịch vụ", "Danh sách và cấu hình các dịch vụ bổ sung", "dịch vụ" };
        else if ("banggia".equals(key))
            m = new String[] { "Quản lý cách tính tiền", "Cấu hình giá phòng và chính sách giá đặc biệt", "bảng giá" };
        else
            m = new String[] { "", "", "" };

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(m[0]);
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel(m[1]);
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        RoundedButton btnBack = new RoundedButton("← Quay lại", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnAdd = new RoundedButton("+ Thêm " + m[2], UIConstants.PRIMARY, Color.WHITE);
        btnBack.addActionListener(e -> {
            currentSub = "";
            removeAll();
            buildUI();
            revalidate();
            repaint();
        });
        btnAdd.addActionListener(e -> handleAdd(key));
        btns.add(btnBack);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    // ======================================================
    // LOẠI PHÒNG SUB
    // ======================================================
    private JPanel buildLoaiPhongSub() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Stats
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(miniStat("Tổng loại phòng", phongSvc.getAllLoaiPhong().size() + " loại", UIConstants.PRIMARY));
        stats.add(miniStat("Đang hoạt động", phongSvc.countLoaiPhongActive() + " loại", UIConstants.SUCCESS));
        stats.add(miniStat("Ngừng sử dụng", phongSvc.countLoaiPhongNgung() + " loại", UIConstants.WARNING));

        // Toolbar
        DefaultTableModel[] modelRef = { null };
        JTable[] tableRef = { null };

        String[] cols = { "_ma", "Tên loại phòng", "Danh mục", "Sức chứa", "Giá từ (đ)", "Giá cao (đ)", "Tiện nghi",
                "Trạng thái" };
        modelRef[0] = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tableRef[0] = styledTable(modelRef[0]);
        tableRef[0].getColumn("_ma").setMaxWidth(0);
        tableRef[0].getColumn("_ma").setMinWidth(0);
        reloadLoaiPhong(modelRef[0]);

        tableRef[0].addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableRef[0].getSelectedRow();
                    if (row < 0)
                        return;
                    LoaiPhong lp = phongSvc.getLoaiPhongById((String) modelRef[0].getValueAt(row, 0));
                    showLoaiPhongDlg(lp, modelRef[0]);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = tableRef[0].rowAtPoint(e.getPoint());
                if (r >= 0)
                    tableRef[0].setRowSelectionInterval(r, r);
            }
        });
        tableRef[0].setComponentPopupMenu(ctxMenu(
                () -> {
                    int r = tableRef[0].getSelectedRow();
                    if (r >= 0) {
                        LoaiPhong lp = phongSvc.getLoaiPhongById((String) modelRef[0].getValueAt(r, 0));
                        showLoaiPhongDlg(lp, modelRef[0]);
                    }
                },
                () -> deleteLoaiPhong(tableRef[0], modelRef[0])));

        JScrollPane sp = new JScrollPane(tableRef[0]);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        JLabel footer = footerLabel("Right-click chọn Xem/Sửa, Xoá • Double-click để xem chi tiết");

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(sp, BorderLayout.CENTER);
        tableWrap.add(footer, BorderLayout.SOUTH);

        panel.add(stats, BorderLayout.NORTH);
        panel.add(tableWrap, BorderLayout.CENTER);
        return panel;
    }

    private void reloadLoaiPhong(DefaultTableModel model) {
        model.setRowCount(0);
        for (LoaiPhong lp : phongSvc.getAllLoaiPhong()) {
            model.addRow(new Object[] { lp.getMaLoai(), lp.getTenLoai(), lp.getDanhMuc(),
                    lp.getSucChua() + " người",
                    String.format("%,.0f", (double) lp.getGiaThapNhat()),
                    String.format("%,.0f", (double) lp.getGiaCaoNhat()),
                    lp.getTiNghi() != null && lp.getTiNghi().length() > 30 ? lp.getTiNghi().substring(0, 30) + "..."
                            : lp.getTiNghi(),
                    lp.getTrangThai() });
        }
    }

    private void showLoaiPhongDlg(LoaiPhong lp, DefaultTableModel model) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        LoaiPhongDialog dlg = new LoaiPhongDialog(owner, lp);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            reloadLoaiPhong(model);
        }
    }

    private void deleteLoaiPhong(JTable tbl, DefaultTableModel model) {
        int row = tbl.getSelectedRow();
        if (row < 0)
            return;
        String ma = (String) model.getValueAt(row, 0);
        String ten = (String) model.getValueAt(row, 1);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa loại phòng \"" + ten + "\"?\nCác phòng thuộc loại này sẽ bị ảnh hưởng!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = phongSvc.xoaLoaiPhong(ma);
            if (err == null) {
                reloadLoaiPhong(model);
                JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ======================================================
    // PHÒNG SUB
    // ======================================================
    private JPanel buildPhongSub() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        Map<String, Integer> tt = phongSvc.getThongKeTrangThai();
        JPanel stats = new JPanel(new GridLayout(1, 5, 10, 0));
        stats.setOpaque(false);
        stats.add(miniStat("Có sẵn", tt.getOrDefault("Có sẵn", 0) + " phòng", UIConstants.SUCCESS));
        stats.add(miniStat("Đang thuê", tt.getOrDefault("Đang thuê", 0) + " phòng", UIConstants.PRIMARY));
        stats.add(miniStat("Đã đặt", tt.getOrDefault("Đã đặt", 0) + " phòng", UIConstants.WARNING));
        stats.add(miniStat("Vệ sinh", tt.getOrDefault("Vệ sinh", 0) + " phòng", new Color(0x06B6D4)));
        stats.add(miniStat("Bảo trì", tt.getOrDefault("Bảo trì", 0) + " phòng", UIConstants.DANGER));

        String[] cols = { "_sp", "Số phòng", "Loại phòng", "View", "Tầng", "Sức chứa", "Giá/đêm", "Khách hiện tại",
                "Trạng thái" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tbl = styledTable(model);
        tbl.getColumn("_sp").setMaxWidth(0);
        tbl.getColumn("_sp").setMinWidth(0);
        reloadPhong(model);

        tbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = tbl.getSelectedRow();
                    if (r >= 0)
                        showPhongDlg(phongSvc.getPhongById((String) model.getValueAt(r, 0)), model);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = tbl.rowAtPoint(e.getPoint());
                if (r >= 0)
                    tbl.setRowSelectionInterval(r, r);
            }
        });
        tbl.setComponentPopupMenu(ctxMenu(
                () -> {
                    int r = tbl.getSelectedRow();
                    if (r >= 0)
                        showPhongDlg(phongSvc.getPhongById((String) model.getValueAt(r, 0)), model);
                },
                () -> deletePhong(tbl, model)));

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        JLabel footer = footerLabel("Right-click chọn Xem/Sửa, Xoá • Double-click để chỉnh sửa");

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);
        panel.add(stats, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    private void reloadPhong(DefaultTableModel model) {
        model.setRowCount(0);
        for (Phong p : phongSvc.getAllPhong()) {
            model.addRow(new Object[] { p.getSoPhong(), p.getSoPhong(), p.getTenLoaiPhong(), p.getView(),
                    "Tầng " + p.getTang(),
                    p.getSucChua() + " người", String.format("%,.0fđ", (double) p.getGiaTheoNgay()),
                    p.getTenKhachHienTai() != null ? p.getTenKhachHienTai() : "—",
                    UIConstants.getTrangThaiPhongLabel(p.getTrangThai()) });
        }
    }

    private void showPhongDlg(Phong p, DefaultTableModel model) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        PhongDialog dlg = new PhongDialog(owner, p);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            reloadPhong(model);
    }

    private void deletePhong(JTable tbl, DefaultTableModel model) {
        int row = tbl.getSelectedRow();
        if (row < 0)
            return;
        String sp = (String) model.getValueAt(row, 0);
        Phong p = phongSvc.getPhongById(sp);
        TrangThaiPhong tt = p.getTrangThai();
        if (tt == TrangThaiPhong.OCCUPIED || tt == TrangThaiPhong.CLEANING) { // Actually OCCUPIED or CLEANING (if in
                                                                              // progress)
            JOptionPane.showMessageDialog(this, "Không thể xóa phòng đang có khách hoặc đã đặt!", "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Xóa phòng " + sp + "?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = phongSvc.xoaPhong(sp);
            if (err == null) {
                reloadPhong(model);
                JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ======================================================
    // DỊCH VỤ SUB
    // ======================================================
    private JPanel buildDichVuSub() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        int all = dvSvc.countAll(), active = dvSvc.countActive();
        double avg = dvSvc.getGiaTrungBinh();
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(miniStat("Tổng dịch vụ", all + " dịch vụ", UIConstants.PRIMARY));
        stats.add(miniStat("Đang cung cấp", active + " dịch vụ", UIConstants.SUCCESS));
        stats.add(miniStat("Tạm ngừng", (all - active) + " dịch vụ", UIConstants.DANGER));
        stats.add(miniStat("Giá trung bình", String.format("%,.0fđ", (double) avg), UIConstants.WARNING));

        // Search + filter
        ModernTextField txtSearch = new ModernTextField("Tìm tên dịch vụ...");
        txtSearch.setPreferredSize(new Dimension(220, 34));
        JComboBox<String> cboLoai = new JComboBox<>(
                new String[] { "Tất cả loại", "Ăn uống", "Spa & Làm đẹp", "Vận chuyển", "Dịch vụ phòng", "Khác" });
        cboLoai.setFont(UIConstants.FONT_BODY);
        JComboBox<String> cboTT = new JComboBox<>(new String[] { "Tất cả", "Hoạt động", "Tạm ngừng" });
        cboTT.setFont(UIConstants.FONT_BODY);

        String[] cols = { "_ma", "#", "Tên dịch vụ", "Loại", "Giá", "Đơn vị", "SL tối thiểu", "Trạng thái" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tbl = styledTable(model);
        tbl.getColumn("_ma").setMaxWidth(0);
        tbl.getColumn("_ma").setMinWidth(0);
        reloadDichVu(model, null, null, null);

        // Search/filter listeners
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                reloadDichVu(model, txtSearch.getText(), cboLoai.getSelectedItem().toString(),
                        cboTT.getSelectedItem().toString());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                reloadDichVu(model, txtSearch.getText(), cboLoai.getSelectedItem().toString(),
                        cboTT.getSelectedItem().toString());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        cboLoai.addActionListener(e -> reloadDichVu(model, txtSearch.getText(), cboLoai.getSelectedItem().toString(),
                cboTT.getSelectedItem().toString()));
        cboTT.addActionListener(e -> reloadDichVu(model, txtSearch.getText(), cboLoai.getSelectedItem().toString(),
                cboTT.getSelectedItem().toString()));

        tbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = tbl.getSelectedRow();
                    if (r >= 0)
                        showDVDlg(dvSvc.getById((String) model.getValueAt(r, 0)), model, txtSearch, cboLoai, cboTT);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = tbl.rowAtPoint(e.getPoint());
                if (r >= 0)
                    tbl.setRowSelectionInterval(r, r);
            }
        });
        tbl.setComponentPopupMenu(ctxMenu(
                () -> {
                    int r = tbl.getSelectedRow();
                    if (r >= 0)
                        showDVDlg(dvSvc.getById((String) model.getValueAt(r, 0)), model, txtSearch, cboLoai, cboTT);
                },
                () -> deleteDichVu(tbl, model, txtSearch, cboLoai, cboTT)));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setOpaque(false);
        searchBar.add(txtSearch);
        searchBar.add(cboLoai);
        searchBar.add(cboTT);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.add(searchBar, BorderLayout.WEST);

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        JLabel footer = footerLabel("Right-click chọn Xem/Sửa, Xoá • Double-click để chỉnh sửa");

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(toolbar, BorderLayout.NORTH);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);
        panel.add(stats, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    private void reloadDichVu(DefaultTableModel model, String kw, String loai, String tt) {
        model.setRowCount(0);
        String l = loai == null || loai.equals("Tất cả loại") ? null : loai;
        String t = tt == null || tt.equals("Tất cả") ? null : tt;
        List<DichVu> list = dvSvc.search(kw == null || kw.isBlank() ? null : kw, l, t);
        int i = 1;
        for (DichVu dv : list) {
            model.addRow(new Object[] { dv.getMaDV(), i++, dv.getTenDV(), dv.getLoai(),
                    String.format("%,.0fđ", (double) dv.getGia()), dv.getDonVi(), dv.getSoLuongMin(),
                    dv.getTrangThai() });
        }
    }

    private void showDVDlg(DichVu dv, DefaultTableModel model, ModernTextField txt, JComboBox<String> loai,
            JComboBox<String> tt) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        DichVuDialog dlg = new DichVuDialog(owner, dv);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            reloadDichVu(model, txt.getText(), loai.getSelectedItem().toString(), tt.getSelectedItem().toString());
    }

    private void deleteDichVu(JTable tbl, DefaultTableModel model, ModernTextField txt, JComboBox<String> loai,
            JComboBox<String> tt) {
        int row = tbl.getSelectedRow();
        if (row < 0)
            return;
        String ma = (String) model.getValueAt(row, 0);
        String ten = (String) model.getValueAt(row, 2);
        int ok = JOptionPane.showConfirmDialog(this, "Xóa dịch vụ \"" + ten + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = dvSvc.xoa(ma);
            if (err == null) {
                reloadDichVu(model, txt.getText(), loai.getSelectedItem().toString(), tt.getSelectedItem().toString());
                JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ======================================================
    // BẢNG GIÁ SUB
    // ======================================================
    private JPanel buildBangGiaSub() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        String[] cols = { "_ma", "#", "Tên bảng giá", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tbl = styledTable(model);
        tbl.getColumn("_ma").setMaxWidth(0);
        tbl.getColumn("_ma").setMinWidth(0);
        reloadBangGia(model);

        tbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = tbl.getSelectedRow();
                    if (r >= 0)
                        doBangGiaSua(tbl, model);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = tbl.rowAtPoint(e.getPoint());
                if (r >= 0)
                    tbl.setRowSelectionInterval(r, r);
            }
        });
        
        tbl.setComponentPopupMenu(ctxMenu(
                () -> doBangGiaSua(tbl, model),
                () -> {
                    int row = tbl.getSelectedRow();
                    if (row < 0) return;
                    String ma = (String) model.getValueAt(row, 0);
                    int ok = JOptionPane.showConfirmDialog(panel, "Xóa bảng giá này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (ok == JOptionPane.YES_OPTION) {
                        String err = bgSvc.xoa(ma);
                        if (err == null) {
                            reloadBangGia(model);
                        } else JOptionPane.showMessageDialog(panel, "Lỗi: " + err);
                    }
                }
        ));

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        JLabel footer = footerLabel("Right-click chọn Xem/Sửa, Xoá • Double-click để sửa");

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    private void doBangGiaSua(JTable tbl, DefaultTableModel model) {
        int row = tbl.getSelectedRow();
        if (row < 0)
            return;
        String ma = (String) model.getValueAt(row, 0);
        BangGia bg = bgSvc.getById(ma);
        if (bg == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy bảng giá!");
            return;
        }
        ui.dialogs.BangGiaDialog dlg = new ui.dialogs.BangGiaDialog((Frame) SwingUtilities.getWindowAncestor(this), bg);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            reloadBangGia(model);
    }

    private void reloadBangGia(DefaultTableModel model) {
        model.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int i = 1;
        for (BangGia bg : bgSvc.getAll()) {
            model.addRow(new Object[] { bg.getMaBangGia(), i++, bg.getTenBangGia(),
                    bg.getNgayBatDau() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayBatDau())) : "",
                    bg.getNgayKetThuc() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayKetThuc())) : "",
                    bg.getTrangThai() });
        }
    }

    // ======================================================
    // SHARED HELPERS
    // ======================================================
    private void handleAdd(String key) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        if ("loaiphong".equals(key)) {
            LoaiPhongDialog d = new LoaiPhongDialog(owner, null);
            d.setVisible(true);
            if (d.isConfirmed())
                navSub("loaiphong");
        } else if ("phong".equals(key)) {
            PhongDialog d = new PhongDialog(owner, null);
            d.setVisible(true);
            if (d.isConfirmed())
                navSub("phong");
        } else if ("dichvu".equals(key)) {
            DichVuDialog d = new DichVuDialog(owner, null);
            d.setVisible(true);
            if (d.isConfirmed())
                navSub("dichvu");
        } else if ("banggia".equals(key)) {
            ui.dialogs.BangGiaDialog dlg = new ui.dialogs.BangGiaDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
            dlg.setVisible(true);
            if (dlg.isConfirmed())
                navSub("banggia");
        }
    }

    private RoundedPanel miniStat(String label, String val, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel l2 = new JLabel(val);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(c);
                g2.fillRoundRect(0, 3, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 8));
        JPanel ct = new JPanel();
        ct.setOpaque(false);
        ct.setLayout(new BoxLayout(ct, BoxLayout.Y_AXIS));
        ct.add(l1);
        ct.add(Box.createVerticalStrut(4));
        ct.add(l2);
        card.add(ct, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(UIConstants.FONT_BODY);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        t.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);
                String v = val != null ? val.toString() : "";
                if (v.equals("Hoạt động") || v.equals("ACTIVE")) {
                    lbl.setForeground(UIConstants.SUCCESS);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                } else if (v.equals("Tạm ngừng") || v.equals("Ngừng") || v.equals("INACTIVE")) {
                    lbl.setForeground(UIConstants.DANGER);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }
                return lbl;
            }
        });
        return t;
    }

    private JLabel footerLabel(String hint) {
        JLabel lbl = new JLabel("  " + hint);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setBackground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        return lbl;
    }

    private void loadIconToLabel(JLabel lbl, String iconPath) {
        File f = new File(iconPath);
        if (f.exists()) {
            try {
                ImageIcon img = new ImageIcon(iconPath);
                Image scaledImg = img.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                lbl.setIcon(new ImageIcon(scaledImg));
            } catch (Exception e) {
                lbl.setText("□");
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 36));
                lbl.setForeground(UIConstants.PRIMARY);
            }
        } else {
            lbl.setText("□");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 36));
            lbl.setForeground(UIConstants.PRIMARY);
        }
    }

    /** Build context menu with edit / delete actions */
    private JPopupMenu ctxMenu(Runnable onEdit, Runnable onDelete) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("◎ Xem / Sửa");
        JMenuItem miDelete = new JMenuItem("✕ Xóa");
        miEdit.setFont(UIConstants.FONT_BODY);
        miDelete.setFont(UIConstants.FONT_BODY);
        miDelete.setForeground(UIConstants.DANGER);
        miEdit.addActionListener(e -> { if (onEdit != null) onEdit.run(); });
        miDelete.addActionListener(e -> { if (onDelete != null) onDelete.run(); });
        menu.add(miEdit);
        menu.addSeparator();
        menu.add(miDelete);
        return menu;
    }

    public void refresh() {
        currentSub = "";
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }
}


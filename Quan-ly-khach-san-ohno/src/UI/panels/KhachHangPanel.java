package ui.panels;

import ui.MainFrame;
import ui.components.ExcelExporter;
import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.KhachHangDialog;
import entity.KhachHang;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.KhachHangService;

public class KhachHangPanel extends JPanel {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(KhachHangPanel.class.getName());

    private final MainFrame mainFrame;
    private final KhachHangService service = new KhachHangService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboGioiTinh;
    private ModernComboBox<String> cboQuocTich;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblFooter;
    private JLabel lblPage;
    private int currentPage = 1;
    private int pageSize = 12;
    private int totalPages = 1;
    
    private List<KhachHang> tatCaKhachHang;
    private List<KhachHang> filteredList;

    private JLabel lblTongKH, lblKhachNam, lblKhachNu, lblQuocTe;

    public KhachHangPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        
        main.add(buildHeader(), BorderLayout.NORTH);
        
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);
        
        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);
        bot.add(buildTableArea(), BorderLayout.SOUTH);
        
        center.add(bot, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);
        
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Hồ sơ Khách hàng"); 
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Lưu trữ, tra cứu và quản lý thông tin lưu trú của khách hàng (CRM)");
        s.setFont(UIConstants.FONT_BODY); 
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t); 
        left.add(Box.createVerticalStrut(2)); 
        left.add(s);
        
        RoundedButton btnThem = new RoundedButton("+ Thêm khách hàng", UIConstants.PRIMARY, Color.WHITE);
        RoundedButton btnExcel = new RoundedButton("↓ Xuất danh sách", new Color(0x16A34A), Color.WHITE);
        
        btnThem.addActionListener(e -> showDialog(null));
        btnExcel.addActionListener(e -> {
            if (filteredList != null && !filteredList.isEmpty()) {
                ExcelExporter.exportKhachHang(KhachHangPanel.this, filteredList);
            } else {
                JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất!");
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExcel); 
        btns.add(btnThem);
        
        p.add(left, BorderLayout.WEST); 
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false); 
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        lblTongKH = new JLabel("...");
        lblKhachNam = new JLabel("...");
        lblKhachNu = new JLabel("...");
        lblQuocTe = new JLabel("...");

        row.add(statCard("Tổng khách hàng", lblTongKH, UIConstants.PRIMARY));
        row.add(statCard("Nam giới", lblKhachNam, new Color(0x3B82F6)));
        row.add(statCard("Nữ giới", lblKhachNu, new Color(0xEC4899)));
        row.add(statCard("Khách quốc tế", lblQuocTe, new Color(0xF59E0B)));
        
        return row;
    }

    private RoundedPanel statCard(String label, JLabel lblValue, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE); 
        card.setShadow(true);
        card.setLayout(new BorderLayout()); 
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));
        
        JLabel l1 = new JLabel(label); 
        l1.setFont(UIConstants.FONT_SMALL); 
        l1.setForeground(UIConstants.TEXT_SECONDARY);
        
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);
        
        JPanel bar = new JPanel() { 
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c); 
                g2.fillRoundRect(0,4,getWidth(),4,4,4); 
                g2.dispose(); 
            } 
        };
        bar.setOpaque(false); 
        bar.setPreferredSize(new Dimension(0,10));
        
        JPanel ct = new JPanel(); 
        ct.setOpaque(false); 
        ct.setLayout(new BoxLayout(ct,BoxLayout.Y_AXIS));
        ct.add(l1); 
        ct.add(Box.createVerticalStrut(4)); 
        ct.add(lblValue);
        
        card.add(ct, BorderLayout.CENTER); 
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false); 
        p.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        
        txtSearch = new ModernTextField("Tìm Mã, Họ Tên, SĐT, CCCD...");
        txtSearch.setPreferredSize(new Dimension(280, 36));
        
        cboGioiTinh = new ModernComboBox<>();
        cboGioiTinh.setPreferredSize(new Dimension(140, 36));
        cboGioiTinh.addItem("Giới tính (Tất cả)");
        cboGioiTinh.addItem("Nam");
        cboGioiTinh.addItem("Nữ");
        
        cboQuocTich = new ModernComboBox<>();
        cboQuocTich.setPreferredSize(new Dimension(160, 36));
        cboQuocTich.addItem("Quốc tịch (Tất cả)");
        cboQuocTich.addItem("Nội địa (Việt Nam)");
        cboQuocTich.addItem("Quốc tế");

        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { runFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { runFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { runFilter(); }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        
        ActionListener al = e -> runFilter();
        cboGioiTinh.addActionListener(al);
        cboQuocTich.addActionListener(al);

        p.add(txtSearch);
        p.add(cboGioiTinh);
        p.add(cboQuocTich);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = {"Mã KH", "Họ tên", "Giới tính", "Số điện thoại", "Giấy tờ (CCCD/HC)", "Quốc tịch"};
        tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel);
        styleTable(); 
        
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount()==2) editSelected(); }
            @Override public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });
        
        table.setComponentPopupMenu(buildContextMenu());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 420));

        lblFooter = new JLabel("Đang tải dữ liệu...");
        lblFooter.setFont(UIConstants.FONT_SMALL); 
        lblFooter.setForeground(UIConstants.TEXT_MUTED);
        
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        paginationPanel.setOpaque(false);
        RoundedButton btnPrev = new RoundedButton("◀", Color.WHITE, UIConstants.TEXT_PRIMARY);
        RoundedButton btnNext = new RoundedButton("▶", Color.WHITE, UIConstants.TEXT_PRIMARY);
        btnPrev.setPreferredSize(new Dimension(32, 28));
        btnNext.setPreferredSize(new Dimension(32, 28));
        
        lblPage = new JLabel("Trang 1/1");
        lblPage.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPage.setForeground(UIConstants.TEXT_PRIMARY);
        
        btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; displayCurrentPage(); } });
        btnNext.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; displayCurrentPage(); } });
        
        paginationPanel.add(btnPrev); 
        paginationPanel.add(lblPage); 
        paginationPanel.add(btnNext);

        JLabel hint = new JLabel("Double-click hoặc Left/Right-click để Tương tác  ");
        hint.setFont(UIConstants.FONT_SMALL); 
        hint.setForeground(UIConstants.TEXT_MUTED);
        
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        footer.add(lblFooter, BorderLayout.WEST); 
        footer.add(paginationPanel, BorderLayout.CENTER);
        footer.add(hint, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout()); 
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER); 
        wrap.add(footer, BorderLayout.SOUTH);
        
        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miXem  = new JMenuItem("👤 Xem / Sửa hồ sơ KH");
        JMenuItem miDat  = new JMenuItem("📅 Đặt phòng cho KH này");
        JMenuItem miThem = new JMenuItem("＋ Thêm khách hàng mới");
        JMenuItem miXoa  = new JMenuItem("✕ Khóa / Xóa khách hàng");
        
        miXem.setFont(UIConstants.FONT_BODY); 
        miDat.setFont(UIConstants.FONT_BODY); 
        miThem.setFont(UIConstants.FONT_BODY); 
        miXoa.setFont(UIConstants.FONT_BODY);
        miXoa.setForeground(UIConstants.DANGER);
        
        miXem.addActionListener(e  -> editSelected());
        miDat.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một khách hàng từ bảng!");
                return;
            }
            mainFrame.navigateTo("datphong", () -> {
                mainFrame.getDatPhongPanel().prefillKhachHang(kh);
            });
        });
        miThem.addActionListener(e -> showDialog(null));
        miXoa.addActionListener(e  -> deleteSelected());
        
        menu.add(miXem); 
        menu.add(miDat); 
        menu.addSeparator(); 
        menu.add(miThem); 
        menu.addSeparator(); 
        menu.add(miXoa);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY); 
        table.setRowHeight(50);
        table.setShowGrid(false); 
        table.setIntercellSpacing(new Dimension(0,0));
        table.setBackground(Color.WHITE); 
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, UIConstants.BORDER));
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int row,int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setForeground(UIConstants.TEXT_PRIMARY);
                
                // Gender coloring natively
                if (col == 2 && val != null) {
                    String gt = val.toString();
                    if (gt.equalsIgnoreCase("Nam")) {
                        lbl.setForeground(new Color(0x2563EB));
                        lbl.setText("👨 " + gt);
                    } else if (gt.equalsIgnoreCase("Nữ")) {
                        lbl.setForeground(new Color(0xDB2777));
                        lbl.setText("👩 " + gt);
                    }
                }
                
                // Nationality coloring natively
                if (col == 5 && val != null) {
                    String qt = val.toString();
                    if (qt.equalsIgnoreCase("Việt Nam")) {
                        lbl.setText("🇻🇳 " + qt);
                        lbl.setForeground(UIConstants.TEXT_PRIMARY);
                    } else if (!qt.isBlank()) {
                        lbl.setText("🌍 " + qt);
                        lbl.setForeground(new Color(0xD97706)); // Gold for Int'l
                    }
                }
                
                return lbl;
            }
        });
    }

    private void loadData() {
        SwingWorker<List<KhachHang>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return service.getAll();
            }

            @Override
            protected void done() {
                try {
                    tatCaKhachHang = get();
                    updateStats();
                    runFilter();
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "UI error in KhachHangPanel", e);
                    lblFooter.setText("Lỗi tải dữ liệu khách hàng.");
                }
            }
        };
        worker.execute();
    }
    
    private void updateStats() {
        if (tatCaKhachHang == null) return;
        int total = tatCaKhachHang.size();
        long nam = tatCaKhachHang.stream().filter(k -> "Nam".equalsIgnoreCase(k.getGioiTinh())).count();
        long nu = tatCaKhachHang.stream().filter(k -> "Nữ".equalsIgnoreCase(k.getGioiTinh())).count();
        long qte = tatCaKhachHang.stream().filter(k -> k.getQuocTich() != null && !k.getQuocTich().equalsIgnoreCase("Việt Nam")).count();
        
        lblTongKH.setText(String.format("%,d", total));
        lblKhachNam.setText(String.format("%,d", nam));
        lblKhachNu.setText(String.format("%,d", nu));
        lblQuocTe.setText(String.format("%,d", qte));
    }

    private void runFilter() {
        if (tatCaKhachHang == null) return;
        
        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.equals("tìm mã, họ tên, sđt, cccd...")) kw = "";
        
        int idxGt = cboGioiTinh.getSelectedIndex();
        int idxQt = cboQuocTich.getSelectedIndex();
        
        final String finalKw = kw;
        
        filteredList = tatCaKhachHang.stream()
            .filter(kh -> {
                boolean matchKw = finalKw.isEmpty() || 
                    (kh.getMaKhachHang() != null && kh.getMaKhachHang().toLowerCase().contains(finalKw)) || 
                    (kh.getHoTen() != null && kh.getHoTen().toLowerCase().contains(finalKw)) ||
                    (kh.getSdt() != null && kh.getSdt().contains(finalKw)) ||
                    (kh.getCccd() != null && kh.getCccd().contains(finalKw));
                
                boolean matchGt = true;
                if (idxGt == 1 && !"Nam".equalsIgnoreCase(kh.getGioiTinh())) matchGt = false;
                if (idxGt == 2 && !"Nữ".equalsIgnoreCase(kh.getGioiTinh())) matchGt = false;
                
                boolean matchQt = true;
                boolean isVN = "Việt Nam".equalsIgnoreCase(kh.getQuocTich()) || kh.getQuocTich() == null || kh.getQuocTich().isBlank();
                if (idxQt == 1 && !isVN) matchQt = false;
                if (idxQt == 2 && isVN) matchQt = false;
                
                return matchKw && matchGt && matchQt;
            })
            .collect(Collectors.toList());

        totalPages = (int) Math.ceil((double) filteredList.size() / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        tableModel.setRowCount(0);
        if (filteredList == null) return;
        
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredList.size());
        
        for (int i = start; i < end; i++) {
            KhachHang kh = filteredList.get(i);
            String gt = kh.getGioiTinh() != null ? kh.getGioiTinh() : "Khác";
            String qte = (kh.getQuocTich() == null || kh.getQuocTich().isBlank()) ? "Việt Nam" : kh.getQuocTich();
            
            tableModel.addRow(new Object[]{
                kh.getMaKhachHang(),
                kh.getHoTen(),
                gt,
                kh.getSdt(), 
                kh.getCccd() != null ? kh.getCccd() : "",
                qte
            });
        }
        
        int showStart = filteredList.isEmpty() ? 0 : start + 1;
        if (lblFooter!=null) lblFooter.setText(" Hiển thị " + showStart + " - " + end + " trong tổng số " + filteredList.size() + " Khách hàng | Được lọc theo tiêu chí");
        if (lblPage!=null) lblPage.setText("Trang " + currentPage + " / " + totalPages);
    }

    private KhachHang getSelected() {
        int row = table.getSelectedRow(); 
        if (row < 0) return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(KhachHang kh) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        KhachHangDialog dlg = new KhachHangDialog(owner, kh);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) refresh();
    }

    private void editSelected() {
        KhachHang kh = getSelected();
        if (kh == null) { JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để mở!"); return; }
        showDialog(kh);
    }

    private void deleteSelected() {
        KhachHang kh = getSelected();
        if (kh == null) { JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để xóa/khóa!"); return; }
        
        int ok = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa hồ sơ khách hàng: \"" + kh.getHoTen() + "\" (" + kh.getMaKhachHang() + ")?\n"
          + "Dữ liệu hóa đơn và phòng liên kết cũng có thể bị ảnh hưởng. Hành động này không thể hoàn tác!",
            "Xác nhận vô hiệu hóa hồ sơ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(kh.getMaKhachHang());
            if (err == null) { 
                JOptionPane.showMessageDialog(this, "Đã xóa hồ sơ khách hàng vĩnh viễn!"); 
                refresh(); 
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi: " + err + "\nKhách hàng này có thể đang có Hóa đơn hoặc Phòng đang thuê.", "Không thể xóa", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() { 
        currentPage = 1;
        loadData(); 
        // We do not run removeAll()/buildUI() again, we simply re-fetch DAO via SwingWorker to keep UI smooth.
    }
}


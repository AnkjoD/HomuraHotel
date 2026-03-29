package UI.panels;

import UI.MainFrame;
import UI.components.ExcelExporter;
import UI.components.RoundedComponents.ModernTextField;
import UI.components.RoundedComponents.RoundedButton;
import UI.components.RoundedComponents.RoundedPanel;
import UI.components.UIConstants;
import UI.dialogs.KhachHangDialog;
import entity.KhachHang;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.KhachHangService;

public class KhachHangPanel extends JPanel {

    @SuppressWarnings("unused")
    private final MainFrame        mainFrame;
    private final KhachHangService service = new KhachHangService();

    private ModernTextField   txtSearch;
    private JTable            table;
    private DefaultTableModel tableModel;
    private String            currentHang = "Tất cả";
    private JLabel            lblFooter;
    private JLabel            lblPage;
    private int               currentPage = 1;
    private int               pageSize    = 8;
    private int               totalPages  = 1;
    private List<KhachHang>   allFilteredList;

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
        main.add(buildHeader(),  BorderLayout.NORTH);
        main.add(buildStats(),   BorderLayout.CENTER);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);
        bot.add(buildTableArea(), BorderLayout.SOUTH);
        main.add(bot, BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Khách hàng"); t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Quản lý hồ sơ và lịch sử lưu trú");
        s.setFont(UIConstants.FONT_BODY); s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t); left.add(Box.createVerticalStrut(2)); left.add(s);
        RoundedButton btnThem  = new RoundedButton("+ Thêm khách hàng", UIConstants.PRIMARY, Color.WHITE);
        RoundedButton btnExcel = new RoundedButton("↓ Xuất Excel", new Color(0x16A34A), Color.WHITE);
        btnThem.addActionListener(e  -> showDialog(null));
        btnExcel.addActionListener(e -> {
            String kw   = txtSearch!=null ? txtSearch.getText().trim() : "";
            String hang = "Tất cả".equals(currentHang) ? null : currentHang;
            ExcelExporter.exportKhachHang(KhachHangPanel.this,
                service.search(kw.isEmpty()?null:kw, hang));
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExcel); btns.add(btnThem);
        p.add(left, BorderLayout.WEST); p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 1, 12, 0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        int tong = service.getAll().size();
        row.add(statCard("Tổng khách hàng", tong    + " KH", UIConstants.PRIMARY));
        return row;
    }

    private RoundedPanel statCard(String label, String value, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE); card.setShadow(true);
        card.setLayout(new BorderLayout()); card.setBorder(BorderFactory.createEmptyBorder(14,16,8,16));
        JLabel l1 = new JLabel(label); l1.setFont(UIConstants.FONT_SMALL); l1.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel l2 = new JLabel(value); l2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JPanel bar = new JPanel() { @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create(); g2.setColor(c); g2.fillRoundRect(0,3,getWidth(),3,3,3); g2.dispose(); } };
        bar.setOpaque(false); bar.setPreferredSize(new Dimension(0,8));
        JPanel ct = new JPanel(); ct.setOpaque(false); ct.setLayout(new BoxLayout(ct,BoxLayout.Y_AXIS));
        ct.add(l1); ct.add(Box.createVerticalStrut(4)); ct.add(l2);
        card.add(ct, BorderLayout.CENTER); card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false); p.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        txtSearch = new ModernTextField("Tìm tên, SĐT, CCCD...");
        txtSearch.setPreferredSize(new Dimension(260, 36));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e)  { loadTable(); }
            @Override
            public void removeUpdate(DocumentEvent e)  { loadTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { loadTable(); }
        });
        p.add(txtSearch, BorderLayout.WEST);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = {"_ma","Mã KH","Họ tên","SĐT","CCCD"};
        tableModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel);
        styleTable(); loadTable();

        // Selection listener
        // (Removed btns enabler)
        // Double-click = edit
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount()==2) editSelected(); }
            @Override public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });
        // Context menu
        table.setComponentPopupMenu(buildContextMenu());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 400));

        lblFooter = new JLabel();
        lblFooter.setFont(UIConstants.FONT_SMALL); lblFooter.setForeground(UIConstants.TEXT_MUTED);
        
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
        
        paginationPanel.add(btnPrev); paginationPanel.add(lblPage); paginationPanel.add(btnNext);

        JLabel hint = new JLabel("Double-click hoặc Chuột phải để Xem, Sửa, Xóa  ");
        hint.setFont(UIConstants.FONT_SMALL); hint.setForeground(UIConstants.TEXT_MUTED);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        footer.add(lblFooter, BorderLayout.WEST); 
        footer.add(paginationPanel, BorderLayout.CENTER);
        footer.add(hint, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER); wrap.add(footer, BorderLayout.SOUTH);
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miXem  = new JMenuItem("◎ Xem / Sửa thông tin");
        JMenuItem miThem = new JMenuItem("+ Thêm khách hàng mới");
        JMenuItem miXoa  = new JMenuItem("✕ Xóa khách hàng này");
        miXem.setFont(UIConstants.FONT_BODY); miThem.setFont(UIConstants.FONT_BODY); miXoa.setFont(UIConstants.FONT_BODY);
        miXoa.setForeground(UIConstants.DANGER);
        miXem.addActionListener(e  -> editSelected());
        miThem.addActionListener(e -> showDialog(null));
        miXoa.addActionListener(e  -> deleteSelected());
        menu.add(miXem); menu.addSeparator(); menu.add(miThem); menu.addSeparator(); menu.add(miXoa);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY); table.setRowHeight(48);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setBackground(Color.WHITE); table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, UIConstants.BORDER));
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(0));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int row,int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
                lbl.setBackground(sel?UIConstants.PRIMARY_LIGHT:(row%2==0?Color.WHITE:new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY); lbl.setForeground(UIConstants.TEXT_PRIMARY);
                return lbl;
            }
        });
    }

    private void loadTable() {
        String kw = txtSearch!=null ? txtSearch.getText().trim() : "";
        allFilteredList = service.search(kw.isEmpty() ? null : kw, null);
        totalPages = (int) Math.ceil((double) allFilteredList.size() / pageSize);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        tableModel.setRowCount(0);
        if (allFilteredList == null) return;
        
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allFilteredList.size());
        
        for (int i = start; i < end; i++) {
            KhachHang kh = allFilteredList.get(i);
            tableModel.addRow(new Object[]{
                kh.getMaKhachHang(), kh.getMaKhachHang(), kh.getHoTen(),
                kh.getSoDienThoai(), kh.getCccd()!=null?kh.getCccd():""
            });
        }
        
        int showStart = allFilteredList.isEmpty() ? 0 : start + 1;
        if (lblFooter!=null) lblFooter.setText("Hiển thị " + showStart + " - " + end + " trong tổng " + allFilteredList.size() + " KH");
        if (lblPage!=null) lblPage.setText("Trang " + currentPage + " / " + totalPages);
    }

    private KhachHang getSelected() {
        int row = table.getSelectedRow(); if (row<0) return null;
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
        if (kh==null) { JOptionPane.showMessageDialog(this,"Chọn một khách hàng để sửa!"); return; }
        showDialog(kh);
    }

    private void deleteSelected() {
        KhachHang kh = getSelected();
        if (kh==null) { JOptionPane.showMessageDialog(this,"Chọn một khách hàng để xóa!"); return; }
        int ok = JOptionPane.showConfirmDialog(this,
            "Xóa khách hàng: \"" + kh.getHoTen() + "\" (" + kh.getMaKhachHang() + ")?\nHành động này không thể hoàn tác!",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(kh.getMaKhachHang());
            if (err==null) { JOptionPane.showMessageDialog(this,"Đã xóa thành công!"); refresh(); }
            else JOptionPane.showMessageDialog(this,"Lỗi: "+err,"Không thể xóa",JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() { removeAll(); buildUI(); revalidate(); repaint(); }
}

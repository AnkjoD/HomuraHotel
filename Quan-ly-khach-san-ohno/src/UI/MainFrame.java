package ui;

import ui.panels.*;
import entity.NhanVien;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import service.AuthService;

public class MainFrame extends JFrame {

    private JPanel contentArea;
    private CardLayout cardLayout;
    private JLabel lblCurrentTime;
    private JLabel hotelLogoLabel;
    private String activeMenu = "tongquan";
    private final java.util.Map<String, Long> lastRefreshAt = new java.util.HashMap<>();
    private final java.util.List<Runnable> menuUpdaters = new java.util.ArrayList<>();
    private static final long REFRESH_COOLDOWN_MS = 7000;

    private TongQuanPanel pTongQuan;
    private ThuePhongPanel pThuePhong;
    private KhachHangPanel pKhachHang;
    private QLHeThongPanel pQLHeThong;
    private ThongKePanel pThongKe;
    private NhanVienPanel pNhanVien;
    private DatPhongPanel pDatPhong;
    private HoaDonPanel pHoaDon;
    // private TaiKhoanPanel pTaiKhoan; // TODO next

    public MainFrame() {
        setTitle("Hotel MS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 820);
        setLocationRelativeTo(null);
        initUI();
        startClock();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> navigateTo("tongquan"));
            }
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 246, 250));

        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildContent(), BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
    }

    // ===== HEADER =====
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        header.setBackground(new Color(0x1E2337));
        header.setPreferredSize(new Dimension(0, 60));

        // ── Góc TRÁI: logo / avatar khách sạn ──
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        leftPanel.setOpaque(false);

        // ★ THAY ĐỔI ĐƯỜNG DẪN LOGO TẠI ĐÂY ★
        final String LOGO_PATH = "d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/img/logo.png";

        java.io.File logoFile = new java.io.File(LOGO_PATH);
        JLabel logoLbl;
        if (logoFile.exists()) {
            ImageIcon raw = new ImageIcon(logoFile.getAbsolutePath());
            Image scaled = raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            logoLbl = new JLabel(new ImageIcon(scaled));
        } else {
            // Placeholder hình vuông bo góc với chữ H
            logoLbl = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(67, 97, 238));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    FontMetrics fm2 = g2.getFontMetrics();
                    String t = "H";
                    g2.drawString(t, (getWidth() - fm2.stringWidth(t)) / 2,
                            (getHeight() + fm2.getAscent() - fm2.getDescent()) / 2);
                    g2.dispose();
                }
            };
            logoLbl.setToolTipText("Không tìm thấy: " + LOGO_PATH);
        }
        hotelLogoLabel = logoLbl;
        hotelLogoLabel.setPreferredSize(new Dimension(40, 40));
        leftPanel.add(hotelLogoLabel);

        // ── Góc PHẢI: đồng hồ + avatar + tên (click → dropdown menu) ──
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        right.setOpaque(false);

        lblCurrentTime = new JLabel();
        lblCurrentTime.setForeground(new Color(0xA0A8C0));
        lblCurrentTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        NhanVien user = AuthService.getInstance().getCurrentUser();
        String fullName = (user != null && user.getHoTen() != null && !user.getHoTen().trim().isEmpty())
                ? user.getHoTen().trim()
                : "Admin";
        String initials = fullName.substring(0, 1).toUpperCase();
        final NhanVien finalUser = user;

        // Avatar tròn
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(67, 97, 238));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (getWidth() - fm.stringWidth(initials)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);
        avatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatar.setToolTipText(fullName);

        JLabel lblName = new JLabel("<html>" + fullName + " <span style='font-size:9px'>&#9660;</span></html>");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(Color.WHITE);
        lblName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Vùng click chung (avatar + tên)
        MouseAdapter openMenu = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserDropdownMenu(avatar, finalUser);
            }
        };
        avatar.addMouseListener(openMenu);
        lblName.addMouseListener(openMenu);

        right.add(lblCurrentTime);
        right.add(avatar);
        right.add(lblName);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Dropdown 3 lựa chọn khi click vào avatar / tên */
    private void showUserDropdownMenu(java.awt.Component invoker, NhanVien user) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));

        // ★ ĐƯỜNG DẪN ICON CHO TỪNG MỤC MENU — SỬA TẠI ĐÂY ★
        final String ICON_INFO = "d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/canhan.png";
        final String ICON_PWD = "d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/doimk.png";
        final String ICON_OUT = "d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/sign_out.png";

        JMenuItem miInfo = styledMenuItem("Xem thông tin", new Color(0x1E2337), false);
        JMenuItem miPwd = styledMenuItem("Đổi mật khẩu", new Color(0x1E2337), false);
        JMenuItem miOut = styledMenuItem("Đăng xuất", new Color(0xEF4444), true);

        setMenuIcon(miInfo, ICON_INFO, 18);
        setMenuIcon(miPwd, ICON_PWD, 18);
        setMenuIcon(miOut, ICON_OUT, 18);

        miInfo.addActionListener(e -> showUserProfileDialog(user));
        miPwd.addActionListener(e -> showChangePasswordDialog());
        miOut.addActionListener(e -> doLogout());

        menu.add(miInfo);
        menu.addSeparator();
        menu.add(miPwd);
        menu.addSeparator();
        menu.add(miOut);

        // Căn phải: popup xuất hiện ngay dưới invoker, mép phải căn theo mép phải
        // invoker
        menu.pack(); // tính preferred size trước
        int menuW = menu.getPreferredSize().width;
        menu.show(invoker, invoker.getWidth() - menuW, invoker.getHeight() + 4);
    }

    private JMenuItem styledMenuItem(String text, Color fg, boolean bold) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 13));
        item.setForeground(fg);
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 32));
        item.setPreferredSize(new Dimension(200, 36));
        item.setOpaque(true);
        return item;
    }

    /** Load icon từ đường dẫn file và gắn vào JMenuItem với kích thước size×size */
    private void setMenuIcon(JMenuItem item, String path, int size) {
        java.io.File f = new java.io.File(path);
        if (f.exists()) {
            ImageIcon raw = new ImageIcon(f.getAbsolutePath());
            Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            item.setIcon(new ImageIcon(scaled));
        }
        // Nếu file không tồn tại → giữ nguyên không có icon (không báo lỗi)
    }

    /** Dialog xem thông tin chi tiết nhân viên */
    private void showUserProfileDialog(NhanVien user) {
        JDialog dlg = new JDialog(this, "Hồ sơ cá nhân", true);
        dlg.setSize(420, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        // ── Dữ liệu ──
        String name = user != null && user.getHoTen() != null ? user.getHoTen() : "Admin";
        String maNV = user != null ? user.getMaNhanVien() : "—";
        String sdt = user != null && user.getSdt() != null ? user.getSdt() : "—";
        String chucVu = user != null && user.getChucVu() != null ? user.getChucVu() : "—";
        entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
        String tenDN = acc != null ? acc.getTenDangNhap() : "—";
        String vaiTro = acc != null && acc.getVaiTro() != null
                ? (acc.getVaiTro() == entity.enums.VaiTro.ADMIN ? "Quản trị viên"
                        : acc.getVaiTro() == entity.enums.VaiTro.MANAGER ? "Quản lý"
                                : "Nhân viên")
                : "Nhân viên";
        // Màu badge vai trò
        Color badgeColor = acc != null && acc.getVaiTro() != null
                && acc.getVaiTro() == entity.enums.VaiTro.ADMIN ? new Color(0x7C3AED)
                        : acc != null && acc.getVaiTro() != null
                                && acc.getVaiTro() == entity.enums.VaiTro.MANAGER ? new Color(0x0EA5E9)
                                        : new Color(0x10B981);

        // Initials avatar
        String initials = name.trim().isEmpty() ? "?"
                : (name.contains(" ")
                        ? ("" + name.charAt(0) + name.charAt(name.lastIndexOf(' ') + 1)).toUpperCase()
                        : name.substring(0, Math.min(2, name.length())).toUpperCase());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ─────────────────────────────────────────
        // A. HEADER — avatar + tên + vai trò
        // ─────────────────────────────────────────
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient nền header
                java.awt.GradientPaint gp = new java.awt.GradientPaint(
                        0, 0, new Color(0x1E2337), getWidth(), getHeight(), new Color(0x2D3875));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Vòng tròn trang trí góc phải
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(getWidth() - 120, -60, 200, 200);
                g2.fillOval(getWidth() - 60, getHeight() - 40, 120, 120);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 175));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(28, 0, 20, 0));

        // Avatar tròn lớn
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Vòng ngoài (ring)
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                // Nền avatar
                g2.setColor(new Color(67, 97, 238));
                g2.fillOval(6, 6, getWidth() - 12, getHeight() - 12);
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials,
                        (getWidth() - fm.stringWidth(initials)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(72, 72));
        avatarPanel.setMaximumSize(new Dimension(72, 72));
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Badge vai trò (pill shape)
        JLabel badge = new JLabel(vaiTro) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(badgeColor.getRed(), badgeColor.getGreen(), badgeColor.getBlue(), 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(new Color(200, 220, 255));
        badge.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        badge.setOpaque(false);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(avatarPanel);
        header.add(Box.createVerticalStrut(10));
        header.add(lblName);
        header.add(Box.createVerticalStrut(6));
        header.add(badge);

        // ─────────────────────────────────────────
        // B. BODY — card thông tin
        // ─────────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        // Card thông tin với border nhẹ
        JPanel card = new JPanel();
        card.setBackground(new Color(0xF8FAFC));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Các dòng thông tin
        String[][] rows = {
                { "NV", "Mã nhân viên", maNV },
                { "ID", "Chức vụ", chucVu },
                { "PH", "SĐT", sdt },
                { "TK", "Tài khoản", tenDN },
        };
        for (int i = 0; i < rows.length; i++) {
            JPanel row = new JPanel(new BorderLayout(0, 0));
            row.setOpaque(i % 2 == 0);
            row.setBackground(Color.WHITE);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

            // Icon tag nhỏ
            JLabel tag = new JLabel(rows[i][0]);
            tag.setFont(new Font("Segoe UI", Font.BOLD, 9));
            tag.setForeground(new Color(67, 97, 238));
            tag.setBackground(new Color(67, 97, 238, 18));
            tag.setOpaque(true);
            tag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            tag.setPreferredSize(new Dimension(32, 20));
            tag.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel tagWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            tagWrap.setOpaque(false);
            tagWrap.setPreferredSize(new Dimension(50, 24));
            tagWrap.add(tag);

            JLabel keyLbl = new JLabel(rows[i][1]);
            keyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            keyLbl.setForeground(new Color(0x64748B));
            keyLbl.setPreferredSize(new Dimension(100, 24));

            JLabel valLbl = new JLabel(rows[i][2]);
            valLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            valLbl.setForeground(new Color(0x1E2337));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            left.setOpaque(false);
            left.add(tagWrap);
            left.add(keyLbl);

            row.add(left, BorderLayout.WEST);
            row.add(valLbl, BorderLayout.EAST);

            card.add(row);
            if (i < rows.length - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(0xE2E8F0));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                card.add(sep);
            }
        }

        body.add(card);

        // ─────────────────────────────────────────
        // C. FOOTER — buttons
        // ─────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)));

        JButton btnChangePwd = new JButton("Đổi mật khẩu");
        btnChangePwd.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnChangePwd.setBackground(new Color(0xF1F5F9));
        btnChangePwd.setForeground(new Color(0x374151));
        btnChangePwd.setFocusPainted(false);
        btnChangePwd.setBorderPainted(false);
        btnChangePwd.setPreferredSize(new Dimension(130, 36));
        btnChangePwd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnChangePwd.addActionListener(e -> {
            dlg.dispose();
            showChangePasswordDialog();
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setBackground(new Color(67, 97, 238));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setPreferredSize(new Dimension(90, 36));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnChangePwd);
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ===== SIDEBAR =====
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0x2D3452));
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(new Color(0x1E2337)); // thay bằng màu tương ứng
        sidebar.setLayout(new BorderLayout());

        // ── Logo
        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        JLabel hotelName = new JLabel("Hotel MS Pro");
        hotelName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        hotelName.setForeground(Color.WHITE); // thay bằng màu tương ứng
        JLabel sub = new JLabel("Enterprise Edition");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(0x8892B0)); // thay bằng màu tương ứng
        textBlock.add(hotelName);
        textBlock.add(sub);

        JPanel logoWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 20));
        logoWrap.setOpaque(false);
        logoWrap.add(textBlock);

        // ── Menu items
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        String[][] menus = {
                { "tongquan", "Tổng quan", "\uD83C\uDFE0" },
                { "datphong", "Đặt phòng", "\uD83D\uDCC5" },
                { "thuephong", "Thuê Phòng", "\uD83D\uDECE\uFE0F" },
                { "hoadon", "Quản lý hóa đơn", "\uD83D\uDCC4" },
                { "SEP", "BỘ PHẬN QUẢN LÝ", "" },
                { "khachhang", "Khách hàng", "\uD83D\uDC65" },
                { "qlhethong", "Hệ thống & Dịch vụ", "\u2699\uFE0F" },
                { "thongke", "Báo cáo thống kê", "\uD83D\uDCC8" },
                { "nhanvien", "Nhân sự & Tài khoản", "\uD83D\uDEE1\uFE0F" }
        };

        for (String[] m : menus) {
            if ("SEP".equals(m[0])) {
                menu.add(Box.createVerticalStrut(14));
                JLabel lblGrp = new JLabel("  " + m[1]);
                lblGrp.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lblGrp.setForeground(new Color(0x6A7599));
                lblGrp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                lblGrp.setAlignmentX(Component.LEFT_ALIGNMENT);
                menu.add(lblGrp);
                menu.add(Box.createVerticalStrut(6));
                continue;
            }
            JPanel item = buildMenuItem(m[0], m[1], m[2]);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            menu.add(item);
            menu.add(Box.createVerticalStrut(6));
        }

        // ── Settings at bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 18));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2D3452)));
        JLabel settings = new JLabel("Cài đặt phần mềm");
        settings.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        settings.setForeground(new Color(0x8892B0)); // thay bằng màu tương ứng
        settings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bottom.add(settings);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(logoWrap, BorderLayout.NORTH);
        topSection.add(menu, BorderLayout.CENTER);

        sidebar.add(topSection, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildMenuItem(String key, String label, String icon) {
        boolean[] hover = { false };
        JPanel item = new JPanel(new BorderLayout(14, 0)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (key.equals(activeMenu)) {
                    g2.setColor(new Color(0x4361EE)); // Nền active màu xanh dương nổi bật
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                } else if (hover[0]) {
                    g2.setColor(new Color(0x2A314D)); // Nền hover
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                }
            }
        };

        item.setOpaque(false);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        item.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        lblIcon.setPreferredSize(new Dimension(32, 32));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblText = new JLabel(label);

        Runnable updateSelf = () -> {
            boolean active = key.equals(activeMenu);
            lblIcon.setForeground(active ? Color.WHITE : new Color(0x8892B0));
            lblText.setForeground(active ? Color.WHITE : new Color(0xA0A8C0));
            lblText.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
            item.repaint();
        };
        menuUpdaters.add(updateSelf);
        updateSelf.run();

        item.add(lblIcon, BorderLayout.WEST);
        item.add(lblText, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover[0] = true;
                item.repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover[0] = false;
                item.repaint();
            }

            public void mouseClicked(MouseEvent e) {
                navigateTo(key);
            }
        });

        return item;
    }

    // ===== CONTENT (GIỮ NGUYÊN) =====
    private JPanel buildContent() {
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);

        // Lazy-load: render shell first to avoid freezing right after login.
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setOpaque(false);
        JLabel lbl = new JLabel("Đang tải giao diện...");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(new Color(0x64748B));
        loading.add(lbl);
        contentArea.add(loading, "loading");

        // Do not construct heavy panels here to keep login transition responsive.
        cardLayout.show(contentArea, "loading");
        return contentArea;
    }

    public DatPhongPanel getDatPhongPanel() {
        return pDatPhong;
    }

    public void navigateTo(String key) {
        navigateTo(key, null);
    }

    public void navigateTo(String key, Runnable postAction) {
        if (activeMenu.equals(key) && contentArea.getComponentCount() > 1)
            return;

        long now = System.currentTimeMillis();
        boolean timeToRefresh = (now - lastRefreshAt.getOrDefault(key, 0L)) > REFRESH_COOLDOWN_MS;

        Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        Cursor def = Cursor.getDefaultCursor();
        setCursor(wait);

        // Chạy trên thread riêng để UI vẫn smooth
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ensurePanelLoaded(key, timeToRefresh);
                return null;
            }

            @Override
            protected void done() {
                cardLayout.show(contentArea, key);
                activeMenu = key;
                if (timeToRefresh)
                    lastRefreshAt.put(key, System.currentTimeMillis());

                // Mặc định refresh Tổng Quan mỗi lần chuyển lại
                if (key.equals("tongquan") && pTongQuan != null)
                    pTongQuan.refresh();

                // Cập nhật lại màu sắc của Sidebar Items!
                for (Runnable r : menuUpdaters)
                    r.run();

                // Lật lại menu sidebar state
                Container parent = contentArea.getParent();
                if (parent != null) {
                    parent.repaint();
                }
                getRootPane().repaint();
                setCursor(def);

                if (postAction != null) {
                    postAction.run();
                }
            }
        };
        worker.execute();
    }

    private void ensurePanelLoaded(String key, boolean refreshData) {
        switch (key) {
            case "tongquan":
                if (pTongQuan == null) {
                    pTongQuan = new TongQuanPanel(this);
                    contentArea.add(pTongQuan, key);
                } else if (refreshData)
                    pTongQuan.refresh();
                break;
            case "thuephong":
                if (pThuePhong == null) {
                    pThuePhong = new ThuePhongPanel(this);
                    contentArea.add(pThuePhong, key);
                } else if (refreshData)
                    pThuePhong.refresh();
                break;
            case "khachhang":
                if (pKhachHang == null) {
                    pKhachHang = new KhachHangPanel(this);
                    contentArea.add(pKhachHang, key);
                } else if (refreshData)
                    pKhachHang.refresh();
                break;
            case "qlhethong":
                if (pQLHeThong == null) {
                    pQLHeThong = new QLHeThongPanel(this);
                    contentArea.add(pQLHeThong, key);
                } else if (refreshData)
                    pQLHeThong.refresh();
                break;
            case "thongke":
                if (pThongKe == null) {
                    pThongKe = new ThongKePanel(this);
                    contentArea.add(pThongKe, key);
                } else if (refreshData)
                    pThongKe.refresh();
                break;
            case "nhanvien":
                if (pNhanVien == null) {
                    pNhanVien = new NhanVienPanel(this);
                    contentArea.add(pNhanVien, key);
                } else if (refreshData)
                    pNhanVien.refresh();
                break;
            case "datphong":
                if (pDatPhong == null) {
                    pDatPhong = new DatPhongPanel(this);
                    contentArea.add(pDatPhong, key);
                } else if (refreshData)
                    pDatPhong.refresh();
                break;
            case "hoadon":
                if (pHoaDon == null) {
                    pHoaDon = new HoaDonPanel(this);
                    contentArea.add(pHoaDon, key);
                } else if (refreshData)
                    pHoaDon.refresh();
                break;
            case "taikhoan":
                if (contentArea.getComponentCount() == 1) {
                    contentArea.add(new JPanel(), key); // Temporarily dummy
                }
                break;
        }
    }

    public void refreshThongKe() {
        ensurePanelLoaded("thongke", false);
        if (pThongKe != null)
            pThongKe.refresh();
    }

    private void startClock() {
        new Timer(1000, e -> {
            String time = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
            lblCurrentTime.setText(time);
        }).start();
    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            AuthService.getInstance().dangXuat();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void showChangePasswordDialog() {
        JDialog dlg = new JDialog(this, "Đổi mật khẩu", true);
        dlg.setSize(420, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(Color.WHITE);

        // ── Layout gốc ──
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ── Header xanh đậm ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x1E2337));
        header.setPreferredSize(new Dimension(0, 90));
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel iconLbl = new JLabel("\uD83D\uDD12") {
            @Override
            protected void paintComponent(Graphics g) {
                // Vẽ vòng tròn nền
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(67, 97, 238, 60));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        iconLbl.setPreferredSize(new Dimension(54, 54));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("Đổi mật khẩu");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Cập nhật mật khẩu tài khoản của bạn");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t2.setForeground(new Color(0xA0A8C0));
        titleBox.add(t1);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(t2);

        header.add(iconLbl, BorderLayout.WEST);
        header.add(Box.createHorizontalStrut(14), BorderLayout.CENTER);
        JPanel titleWrap = new JPanel(new GridBagLayout());
        titleWrap.setOpaque(false);
        titleWrap.add(titleBox);
        header.add(titleWrap, BorderLayout.CENTER);

        // ── Body form ──
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(22, 28, 8, 28));

        // Helper: tạo field group (label + field + error label)
        JPasswordField txtOld = makePwdField();
        JPasswordField txtNew = makePwdField();
        JPasswordField txtCfm = makePwdField();

        JLabel errOld = errLabel(); // lỗi mật khẩu cũ
        JLabel errNew = errLabel(); // lỗi mật khẩu mới
        JLabel errCfm = errLabel(); // lỗi xác nhận

        // Thanh độ mạnh mật khẩu
        JPanel strengthBar = new JPanel(new GridLayout(1, 4, 4, 0));
        strengthBar.setOpaque(false);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel[] segs = new JPanel[4];
        for (int i = 0; i < 4; i++) {
            segs[i] = new JPanel();
            segs[i].setBackground(new Color(0xE2E8F0));
            segs[i].setOpaque(true);
            segs[i].setPreferredSize(new Dimension(0, 5));
            strengthBar.add(segs[i]);
        }
        JLabel strengthTxt = new JLabel(" ");
        strengthTxt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        strengthTxt.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Lắng nghe mật khẩu mới → cập nhật thanh độ mạnh
        txtNew.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                String p = new String(txtNew.getPassword());
                int score = 0;
                if (p.length() >= 8)
                    score++;
                if (p.matches(".*[A-Z].*"))
                    score++;
                if (p.matches(".*[0-9].*"))
                    score++;
                if (p.matches(".*[^A-Za-z0-9].*"))
                    score++;
                Color[] clrs = { new Color(0xEF4444), new Color(0xF97316), new Color(0xEAB308), new Color(0x22C55E) };
                String[] txts = { "Rất yếu", "Yếu", "Trung bình", "Mạnh" };
                for (int i = 0; i < 4; i++)
                    segs[i].setBackground(i < score ? clrs[score - 1] : new Color(0xE2E8F0));
                strengthTxt.setText(p.isEmpty() ? " " : txts[Math.max(0, score - 1)]);
                strengthTxt.setForeground(p.isEmpty() ? Color.GRAY : clrs[Math.max(0, score - 1)]);
                strengthBar.repaint();
                // xoá lỗi khi đang nhập
                errNew.setText(" ");
                errCfm.setText(" ");
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }
        });

        body.add(fieldGroup("Mật khẩu hiện tại", txtOld, errOld));
        body.add(Box.createVerticalStrut(10));
        body.add(fieldGroup("Mật khẩu mới", txtNew, null));
        // Thêm thanh độ mạnh
        body.add(Box.createVerticalStrut(5));
        body.add(strengthBar);
        body.add(Box.createVerticalStrut(2));
        body.add(strengthTxt);
        body.add(errNew);
        body.add(Box.createVerticalStrut(10));
        body.add(fieldGroup("Xác nhận mật khẩu mới", txtCfm, errCfm));

        // Ghi chú yêu cầu
        JLabel hint = new JLabel("<html><font color='#64748B'>Mật khẩu phải có ít nhất <b>8 ký tự</b>,"
                + " gồm <b>chữ hoa</b>, <b>chữ số</b> và <b>ký tự đặc biệt</b>.</font></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        body.add(Box.createVerticalStrut(10));
        body.add(hint);

        // ── Buttons ──
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 16));
        btnRow.setBackground(Color.WHITE);
        btnRow.setBorder(BorderFactory.createEmptyBorder(0, 20, 4, 20));

        JButton btnCancel = new JButton("Huỷ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setBackground(new Color(0xF1F5F9));
        btnCancel.setForeground(new Color(0x374151));
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setPreferredSize(new Dimension(90, 36));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSave = new JButton("Lưu thay đổi");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(67, 97, 238));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setPreferredSize(new Dimension(130, 36));
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Regex ràng buộc mật khẩu mới
        // Ít nhất 8 ký tự, có chữ hoa, chữ thường, chữ số, ký tự đặc biệt
        final java.util.regex.Pattern PWD_PATTERN = java.util.regex.Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

        btnSave.addActionListener(e -> {
            boolean ok = true;
            String oldPwd = new String(txtOld.getPassword());
            String newPwd = new String(txtNew.getPassword());
            String cfmPwd = new String(txtCfm.getPassword());

            // Validate mật khẩu cũ
            if (oldPwd.isEmpty()) {
                errOld.setText("Vui lòng nhập mật khẩu hiện tại");
                ok = false;
            } else {
                entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
                if (acc == null || !acc.getMatKhau().equals(oldPwd)) {
                    errOld.setText("Mật khẩu hiện tại không đúng");
                    ok = false;
                } else {
                    errOld.setText(" ");
                }
            }

            // Validate mật khẩu mới
            if (newPwd.isEmpty()) {
                errNew.setText("Vui lòng nhập mật khẩu mới");
                ok = false;
            } else if (!PWD_PATTERN.matcher(newPwd).matches()) {
                errNew.setText("Mật khẩu phải có ít nhất 8 ký tự, chữ hoa, chữ số và ký tự đặc biệt");
                ok = false;
            } else if (newPwd.equals(oldPwd)) {
                errNew.setText("Mật khẩu mới không được trùng mật khẩu cũ");
                ok = false;
            } else {
                errNew.setText(" ");
            }

            // Validate xác nhận
            if (cfmPwd.isEmpty()) {
                errCfm.setText("Vui lòng xác nhận mật khẩu mới");
                ok = false;
            } else if (!newPwd.equals(cfmPwd)) {
                errCfm.setText("Mật khẩu xác nhận không khớp");
                ok = false;
            } else {
                errCfm.setText(" ");
            }

            if (!ok)
                return;

            entity.TaiKhoan acc = AuthService.getInstance().getCurrentAccount();
            acc.setMatKhau(newPwd);
            new dao.TaiKhoanDAO().doiMatKhau(acc.getTenDangNhap(), newPwd);

            // Hiển thị thành công ngay trong dialog thay vì JOptionPane
            JOptionPane.showMessageDialog(dlg,
                    "<html><b>Đổi mật khẩu thành công!</b><br>Vui lòng dùng mật khẩu mới khi đăng nhập lần sau.</html>",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dlg.dispose();
        });

        btnRow.add(btnCancel);
        btnRow.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    /** Tạo JPasswordField chuẩn style */
    private JPasswordField makePwdField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCBD5E1), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        // Hiệu ứng focus: border đổi màu xanh
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(67, 97, 238), 2),
                        BorderFactory.createEmptyBorder(3, 9, 3, 9)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xCBD5E1), 1),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)));
            }
        });
        return f;
    }

    /** Tạo label lỗi inline (đỏ, nhỏ) */
    private JLabel errLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0xEF4444));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Tạo nhóm label + field + errLabel */
    private JPanel fieldGroup(String labelText, JPasswordField field, JLabel errLbl) {
        JPanel g = new JPanel();
        g.setOpaque(false);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cap = new JLabel(labelText);
        cap.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cap.setForeground(new Color(0x374151));
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.add(cap);
        g.add(Box.createVerticalStrut(5));
        g.add(field);
        if (errLbl != null)
            g.add(errLbl);
        return g;
    }
}

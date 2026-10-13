package ui;

import entity.NhanVien;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.AuthService;
import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import ui.theme.ThemeManager;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private RoundedButton btnLogin;
    private RoundedButton btnTheme;
    private final java.util.function.Consumer<ui.theme.Theme> themeListener = t -> refreshThemeUI();
    private JLabel lblError;
    private JLabel lblAttempts;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private volatile boolean loginInProgress = false;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_SECONDS = 30;
    private static final String LOCKOUT_FILE = "login_lockout.dat";
    private int failedAttempts = 0;
    private boolean isLockedOut = false;
    private Timer lockoutTimer;
    private int lockoutRemaining = 0;
    private long lockoutEndTime = 0;

    public LoginFrame() {
        setTitle("Homura Hotel – Đăng nhập hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 540);
        setLocationRelativeTo(null);
        setResizable(false);
        loadLockoutState();
        initUI();

        ThemeManager.addThemeChangeListener(themeListener);
    }

    private void initUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(UIConstants.BG_MAIN);

        leftPanel = buildLeftPanel();
        rightPanel = buildRightPanel();

        root.add(leftPanel);
        root.add(rightPanel);

        setContentPane(root);
    }

    private void refreshThemeUI() {
        // Only the illustration needs rebuilding; retain input, errors and lockout state.
        Container root = getContentPane();
        root.remove(leftPanel);
        leftPanel = buildLeftPanel();
        root.add(leftPanel, 0);
        btnTheme.setText(UIConstants.isDark() ? "Madoka (Sáng)" : "Homura (Tối)");
        revalidate();
        repaint();
    }

    @Override
    public void dispose() {
        ThemeManager.removeThemeChangeListener(themeListener);
        if (lockoutTimer != null) lockoutTimer.stop();
        super.dispose();
    }

    private JPanel buildLeftPanel() {
        boolean dark = UIConstants.isDark();
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color grad1 = dark ? new Color(0x13, 0x0E, 0x24) : new Color(0xFB, 0x71, 0x85);
                Color grad2 = dark ? new Color(0x4C, 0x1D, 0x95) : new Color(0xEC, 0x48, 0x99);

                GradientPaint gp = new GradientPaint(0, 0, grad1, getWidth(), getHeight(), grad2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative ambient circles
                g2.setColor(new Color(255, 255, 255, dark ? 12 : 24));
                g2.fillOval(-60, -60, 260, 260);
                g2.fillOval(getWidth() - 100, getHeight() - 100, 240, 240);
                g2.dispose();
            }
        };
        panel.setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Brand Emblem
        JPanel logoBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, dark ? 40 : 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // Shield star outline
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
                FontMetrics fm = g2.getFontMetrics();
                String mark = dark ? "H" : "M";
                g2.drawString(mark, (getWidth() - fm.stringWidth(mark)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logoBox.setOpaque(false);
        logoBox.setPreferredSize(new Dimension(78, 78));
        logoBox.setMaximumSize(new Dimension(78, 78));
        logoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Homura Hotel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Hệ thống quản lý khách sạn cao cấp");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 210));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(Box.createVerticalStrut(10));
        content.add(logoBox);
        content.add(Box.createVerticalStrut(18));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(6));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(32));

        String[] features = {
                "• Sơ đồ phòng & đặt phòng thông minh",
                "• Dịch vụ, hóa đơn & thanh toán đa kênh",
                "• Báo cáo doanh thu & giao ca chuẩn xác"
        };
        for (String f : features) {
            JLabel lbl = new JLabel(f);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(new Color(255, 255, 255, 190));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(lbl);
            content.add(Box.createVerticalStrut(8));
        }

        content.add(Box.createVerticalStrut(28));
        JLabel lblVer = new JLabel("v2.0 – Homura & Madoka Edition");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(new Color(255, 255, 255, 120));
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblVer);

        panel.add(content);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 28, 20, 28));

        // Top bar: Theme Toggle
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);

        String themeText = UIConstants.isDark() ? "Madoka (Sáng)" : "Homura (Tối)";
        btnTheme = new RoundedButton(themeText, UIConstants.PRIMARY_LIGHT, UIConstants.PRIMARY);
        btnTheme.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTheme.setPreferredSize(new Dimension(145, 30));
        btnTheme.addActionListener(e -> ThemeManager.toggleTheme());
        topBar.add(btnTheme);

        // Center form
        JPanel centerBox = new JPanel(new GridBagLayout());
        centerBox.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(320, 410));

        JLabel lblWelcome = new JLabel("Chào mừng trở lại!");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(UIConstants.TEXT_PRIMARY);
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNote = new JLabel("Vui lòng đăng nhập để bắt đầu phiên làm việc");
        lblNote.setFont(UIConstants.FONT_BODY);
        lblNote.setForeground(UIConstants.TEXT_SECONDARY);
        lblNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblWelcome);
        form.add(Box.createVerticalStrut(4));
        form.add(lblNote);
        form.add(Box.createVerticalStrut(20));

        // Username
        form.add(buildFieldLabel("Tên đăng nhập"));
        form.add(Box.createVerticalStrut(4));
        txtUsername = new JTextField();
        styleField(txtUsername);
        txtUsername.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateUsername();
            }
        });
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(12));

        // Password
        form.add(buildFieldLabel("Mật khẩu"));
        form.add(Box.createVerticalStrut(4));
        txtPassword = new JPasswordField();
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(wrapPasswordField(txtPassword));
        form.add(Box.createVerticalStrut(10));

        // Error & Attempts label
        lblError = new JLabel(" ");
        lblError.setFont(UIConstants.FONT_SMALL);
        lblError.setForeground(UIConstants.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);

        lblAttempts = new JLabel(" ");
        lblAttempts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblAttempts.setForeground(UIConstants.TEXT_MUTED);
        lblAttempts.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblAttempts);
        form.add(Box.createVerticalStrut(6));

        // Login button
        btnLogin = new RoundedButton("Đăng nhập", UIConstants.PRIMARY, Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(12));

        // Quick demo fill buttons
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoPanel.setOpaque(false);
        demoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDemoHint = new JLabel("Demo:");
        lblDemoHint.setFont(UIConstants.FONT_TINY);
        lblDemoHint.setForeground(UIConstants.TEXT_MUTED);
        demoPanel.add(lblDemoHint);

        JButton btnDemoAdmin = createQuickFillBtn("admin (Admin)", "admin", "admin");
        JButton btnDemoManager = createQuickFillBtn("manager (Quản lý)", "manager", "123");
        demoPanel.add(btnDemoAdmin);
        demoPanel.add(btnDemoManager);
        form.add(demoPanel);

        centerBox.add(form);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(centerBox, BorderLayout.CENTER);

        // Actions
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        return panel;
    }

    private JButton createQuickFillBtn(String text, String u, String p) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        b.setForeground(UIConstants.PRIMARY);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER, 1));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            txtUsername.setText(u);
            txtPassword.setText(p);
            lblError.setText(" ");
        });
        return b;
    }

    private JLabel buildFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setFont(UIConstants.FONT_BODY);
        field.setPreferredSize(new Dimension(320, 38));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setBackground(UIConstants.INPUT_BG);
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setCaretColor(UIConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                resetFieldBorder(field);
                field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER_FOCUS),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                if (!isLockedOut) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText(" ");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                Boolean hasError = (Boolean) field.getClientProperty("hasError");
                if (hasError == null || !hasError) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                }
            }
        });
    }

    private boolean validateUsername() {
        String u = txtUsername.getText().trim();
        if (u.isEmpty()) {
            lblError.setText("Vui lòng nhập tên đăng nhập.");
            setErrorBorder(txtUsername);
            return false;
        }
        resetFieldBorder(txtUsername);
        return true;
    }

    private boolean validatePassword() {
        String p = new String(txtPassword.getPassword());
        if (p.isEmpty()) {
            lblError.setText("Vui lòng nhập mật khẩu.");
            setErrorBorder(txtPassword);
            return false;
        }
        resetFieldBorder(txtPassword);
        return true;
    }

    private boolean validateFields() {
        boolean u = validateUsername();
        boolean p = validatePassword();
        return u && p;
    }

    private void setErrorBorder(JTextField field) {
        field.putClientProperty("hasError", true);
        Component target = field;
        int padding = 6;
        if (Boolean.TRUE.equals(field.getClientProperty("isWrapped"))) {
            target = field.getParent().getParent();
            padding = 0;
        }
        ((JComponent) target).setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.DANGER),
                BorderFactory.createEmptyBorder(padding, 10, padding, 10)));
    }

    private void resetFieldBorder(JTextField field) {
        field.putClientProperty("hasError", false);
        Component target = field;
        int padding = 6;
        if (Boolean.TRUE.equals(field.getClientProperty("isWrapped"))) {
            target = field.getParent().getParent();
            padding = 0;
        }
        ((JComponent) target).setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(padding, 10, padding, 10)));
    }

    private JPanel wrapPasswordField(JPasswordField field) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(UIConstants.INPUT_BG);
        wrap.setPreferredSize(new Dimension(320, 38));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        field.setFont(UIConstants.FONT_BODY);
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setCaretColor(UIConstants.TEXT_PRIMARY);
        field.setOpaque(false);
        field.setBorder(null);
        field.putClientProperty("isWrapped", true);

        JButton btnEye = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.TEXT_MUTED);
                int w = getWidth(), h = getHeight();
                int ew = 20, eh = 12;
                int x = (w - ew) / 2, y = (h - eh) / 2;
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                path.moveTo(x, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y - 2, x + ew, y + eh / 2.0);
                path.quadTo(x + ew / 2.0, y + eh + 2, x, y + eh / 2.0);
                g2.draw(path);
                g2.fillOval(x + ew / 2 - 3, y + eh / 2 - 3, 6, 6);

                if (field.getEchoChar() != (char) 0) {
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.drawLine(x, y + eh - 1, x + ew, y + 1);
                }
                g2.dispose();
            }
        };
        btnEye.setPreferredSize(new Dimension(38, 38));
        btnEye.setContentAreaFilled(false);
        btnEye.setBorderPainted(false);
        btnEye.setFocusPainted(false);
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEye.addActionListener(e -> {
            field.setEchoChar(field.getEchoChar() == (char) 0 ? '•' : (char) 0);
            btnEye.repaint();
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                wrap.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER_FOCUS),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                if (!isLockedOut) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText(" ");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                validatePassword();
                Boolean hasError = (Boolean) field.getClientProperty("hasError");
                if (hasError == null || !hasError) {
                    wrap.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(0, 0, 0, 0)));
                }
            }
        });

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        inner.add(field, BorderLayout.CENTER);

        wrap.add(inner, BorderLayout.CENTER);
        wrap.add(btnEye, BorderLayout.EAST);
        return wrap;
    }

    private void doLogin() {
        if (loginInProgress || isLockedOut || !validateFields()) return;

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        setLoginLoading(true);
        lblError.setForeground(UIConstants.TEXT_SECONDARY);
        lblError.setText("Đang xác thực thông tin...");

        SwingWorker<NhanVien, Void> worker = new SwingWorker<NhanVien, Void>() {
            @Override
            protected NhanVien doInBackground() {
                return AuthService.getInstance().dangNhap(username, password);
            }

            @Override
            protected void done() {
                try {
                    NhanVien nv = get();
                    if (nv != null) {
                        failedAttempts = 0;
                        saveLockoutState();
                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                        dispose();
                    } else {
                        failedAttempts++;
                        saveLockoutState();

                        if (AuthService.getInstance().isAccountDisabled(username, password)) {
                            lblError.setForeground(UIConstants.WARNING);
                            lblError.setText("Tài khoản đã bị vô hiệu hóa.");
                            setErrorBorder(txtUsername);
                        } else if (failedAttempts >= MAX_ATTEMPTS) {
                            setLoginLoading(false);
                            startLockout();
                            return;
                        } else {
                            lblError.setForeground(UIConstants.DANGER);
                            lblError.setText("Tên đăng nhập hoặc mật khẩu không chính xác.");
                            setErrorBorder(txtUsername);
                            setErrorBorder(txtPassword);
                        }

                        updateAttemptsLabel();
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        setLoginLoading(false);
                    }
                } catch (Exception ex) {
                    lblError.setForeground(UIConstants.DANGER);
                    lblError.setText("Lỗi xử lý đăng nhập.");
                    setLoginLoading(false);
                }
            }
        };

        Timer watchdog = new Timer(8000, e -> {
            if (!worker.isDone()) {
                lblError.setForeground(UIConstants.WARNING);
                lblError.setText("Kết nối quá hạn. Vui lòng thử lại.");
                setLoginLoading(false);
            }
        });
        watchdog.setRepeats(false);
        watchdog.start();

        worker.execute();
    }

    private void setLoginLoading(boolean loading) {
        loginInProgress = loading;
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void updateAttemptsLabel() {
        int remaining = MAX_ATTEMPTS - failedAttempts;
        if (remaining > 0 && remaining < MAX_ATTEMPTS) {
            lblAttempts.setText("Còn " + remaining + " lần thử trước khi bị khóa tạm thời");
            lblAttempts.setForeground(remaining <= 2 ? UIConstants.DANGER : UIConstants.TEXT_MUTED);
        } else {
            lblAttempts.setText(" ");
        }
    }

    private void startLockout() {
        isLockedOut = true;
        if (lockoutRemaining <= 0) {
            lockoutRemaining = LOCKOUT_SECONDS;
            lockoutEndTime = System.currentTimeMillis() + (LOCKOUT_SECONDS * 1000L);
            saveLockoutState();
        }

        txtUsername.setEnabled(false);
        txtPassword.setEnabled(false);
        btnLogin.setEnabled(false);
        updateLockoutUI();

        if (lockoutTimer != null && lockoutTimer.isRunning()) lockoutTimer.stop();
        lockoutTimer = new Timer(1000, e -> {
            lockoutRemaining--;
            if (lockoutRemaining <= 0) {
                endLockout();
            } else {
                updateLockoutUI();
            }
        });
        lockoutTimer.start();
    }

    private void updateLockoutUI() {
        lblError.setForeground(UIConstants.DANGER);
        lblError.setText("Đăng nhập sai quá nhiều lần!");
        lblAttempts.setText("Vui lòng thử lại sau " + lockoutRemaining + " giây");
        btnLogin.setText("Khóa tạm thời (" + lockoutRemaining + "s)");
    }

    private void endLockout() {
        if (lockoutTimer != null) lockoutTimer.stop();
        isLockedOut = false;
        failedAttempts = 0;
        lockoutRemaining = 0;
        lockoutEndTime = 0;
        saveLockoutState();

        txtUsername.setEnabled(true);
        txtPassword.setEnabled(true);
        btnLogin.setEnabled(true);
        btnLogin.setText("Đăng nhập");
        lblError.setText(" ");
        lblAttempts.setText(" ");
        resetFieldBorder(txtUsername);
        resetFieldBorder(txtPassword);
    }

    private void saveLockoutState() {
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(LOCKOUT_FILE))) {
            out.writeInt(failedAttempts);
            out.writeLong(lockoutEndTime);
        } catch (Exception ignored) { }
    }

    private void loadLockoutState() {
        java.io.File file = new java.io.File(LOCKOUT_FILE);
        if (!file.exists()) return;
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            failedAttempts = in.readInt();
            lockoutEndTime = in.readLong();
            long now = System.currentTimeMillis();
            if (lockoutEndTime > now) {
                lockoutRemaining = (int) ((lockoutEndTime - now) / 1000);
                if (lockoutRemaining > 0) {
                    SwingUtilities.invokeLater(this::startLockout);
                }
            }
        } catch (Exception ignored) { }
    }
}

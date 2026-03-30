package ui;

import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import entity.NhanVien;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.AuthService;
public class LoginFrame extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private RoundedButton  btnLogin;
    private JLabel         lblError;
    private volatile boolean loginInProgress = false;

    public LoginFrame() {
        setTitle("Đăng nhập – Hotel MS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);

        root.add(buildLeftPanel());
        root.add(buildRightPanel());

        setContentPane(root);
    }

    // ---- Panel trái: logo + branding (xanh gradient) ----
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(0x3B82F6),
                    getWidth(), getHeight(), new Color(0x1E40AF));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Circles decoration
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillOval(-60, -60, 220, 220);
                g2.fillOval(getWidth() - 80, getHeight() - 80, 200, 200);
                g2.dispose();
            }
        };
        panel.setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Logo
        JPanel logoBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("HI", (getWidth()-fm.stringWidth("HI"))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        logoBox.setOpaque(false);
        logoBox.setPreferredSize(new Dimension(72, 72));
        logoBox.setMaximumSize(new Dimension(72, 72));
        logoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Hotel MS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Hệ thống quản lý khách sạn");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(255, 255, 255, 200));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(Box.createVerticalStrut(10));
        content.add(logoBox);
        content.add(Box.createVerticalStrut(16));
        content.add(lblTitle);
        content.add(Box.createVerticalStrut(6));
        content.add(lblSub);
        content.add(Box.createVerticalStrut(30));

        // Features list
        String[] features = {" Quản lý phòng & đặt phòng", " Theo dõi khách hàng", " Báo cáo & thống kê"};
        for (String f : features) {
            JLabel lbl = new JLabel(f);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(new Color(255, 255, 255, 180));
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(lbl);
            content.add(Box.createVerticalStrut(6));
        }

        panel.add(content);
        return panel;
    }

    // ---- Panel phải: form đăng nhập ----
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(300, 360));

        JLabel lblWelcome = new JLabel("Chào mừng trở lại!");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(UIConstants.TEXT_PRIMARY);
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblNote = new JLabel("Vui lòng đăng nhập để tiếp tục");
        lblNote.setFont(UIConstants.FONT_BODY);
        lblNote.setForeground(UIConstants.TEXT_SECONDARY);
        lblNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblWelcome);
        form.add(Box.createVerticalStrut(4));
        form.add(lblNote);
        form.add(Box.createVerticalStrut(28));

        // Username
        form.add(buildFieldLabel("Tên đăng nhập"));
        form.add(Box.createVerticalStrut(4));
        txtUsername = new JTextField();
        styleField(txtUsername);
        txtUsername.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { validateUsernameRealTime(); }
        });
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(14));

        // Password
        form.add(buildFieldLabel("Mật khẩu"));
        form.add(Box.createVerticalStrut(4));
        txtPassword = new JPasswordField();
        styleField(txtPassword);
        txtPassword.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { validatePasswordRealTime(); }
        });
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(10));

        // Remember me
        JCheckBox chkRemember = new JCheckBox("Ghi nhớ đăng nhập");
        chkRemember.setFont(UIConstants.FONT_BODY);
        chkRemember.setForeground(UIConstants.TEXT_SECONDARY);
        chkRemember.setOpaque(false);
        chkRemember.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(chkRemember);
        form.add(Box.createVerticalStrut(18));

        // Error label
        lblError = new JLabel(" ");
        lblError.setFont(UIConstants.FONT_SMALL);
        lblError.setForeground(UIConstants.DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);
        form.add(Box.createVerticalStrut(8));

        // Login button
        btnLogin = new RoundedButton("Đăng nhập", UIConstants.PRIMARY, Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(14));

        // Demo hint
        JLabel lblDemo = new JLabel("Demo: admin / admin");
        lblDemo.setFont(UIConstants.FONT_SMALL);
        lblDemo.setForeground(UIConstants.TEXT_MUTED);
        lblDemo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblDemo);

        panel.add(form);

        // Events
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        return panel;
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
        field.setPreferredSize(new Dimension(300, 38));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                resetFieldBorder(field);
                field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.PRIMARY),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                lblError.setText(" ");
            }
            @Override
            public void focusLost(FocusEvent e) {
                // Chỉ reset về border bình thường nếu không có lỗi
                Boolean hasError = (Boolean) field.getClientProperty("hasError");
                if (hasError == null || !hasError) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                }
            }
        });
    }
    
    private boolean validateUsernameRealTime() {
        String username = txtUsername.getText().trim();
        resetFieldBorder(txtUsername);
        
        if (username.isEmpty()) {
            setErrorBorder(txtUsername);
            lblError.setText("Vui lòng nhập tên đăng nhập!");
            return false;
        }
        
        if (username.length() < 3 || username.length() > 20) {
            setErrorBorder(txtUsername);
            lblError.setText("Độ dài tên đăng nhập phải từ 3 đến 20 ký tự!");
            return false;
        }

        if (username.contains(" ")) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập không được chứa khoảng trắng!");
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            setErrorBorder(txtUsername);
            lblError.setText("Tên đăng nhập không được chứa ký tự đặc biệt hoặc có dấu!");
            return false;
        }
        
        return true;
    }

    private boolean validatePasswordRealTime() {
        String password = new String(txtPassword.getPassword()).trim();
        resetFieldBorder(txtPassword);
        
        if (password.isEmpty()) {
            setErrorBorder(txtPassword);
            lblError.setText("Vui lòng nhập mật khẩu!");
            return false;
        }
        
        if (password.length() < 3) {
            setErrorBorder(txtPassword);
            lblError.setText("Mật khẩu quá ngắn (phải có ít nhất 3 ký tự)!");
            return false;
        }

        if (password.length() > 20) {
            setErrorBorder(txtPassword);
            lblError.setText("Mật khẩu quá dài (tối đa 20 ký tự)!");
            return false;
        }
        
        return true;
    }

    private boolean validateFields() {
        boolean isUserValid = validateUsernameRealTime();
        boolean isPassValid = validatePasswordRealTime();
        
        boolean valid = isUserValid && isPassValid;
        if (valid) {
            lblError.setText(" ");
        }
        return valid;
    }

    private void setErrorBorder(JTextField field) {
        field.putClientProperty("hasError", true); // đánh dấu có lỗi
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.DANGER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void resetFieldBorder(JTextField field) {
        field.putClientProperty("hasError", false);
        field.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void doLogin() {
        if (loginInProgress) return;
        if (!validateFields()) return;

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        setLoginLoading(true);
        lblError.setText("Đang đăng nhập...");

        SwingWorker<NhanVien, Void> worker = new SwingWorker<NhanVien, Void>() {
            @Override protected NhanVien doInBackground() {
                return AuthService.getInstance().dangNhap(username, password);
            }
            @Override protected void done() {
                try {
                    NhanVien nv = get();
                    if (nv != null) {
                        try {
                            MainFrame mainFrame = new MainFrame();
                            mainFrame.setVisible(true);
                            dispose();
                        } catch (Exception uiEx) {
                            java.util.logging.Logger.getLogger(LoginFrame.class.getName())
                                .log(java.util.logging.Level.SEVERE, "Lỗi mở MainFrame", uiEx);
                            lblError.setText("Không thể mở màn hình chính. Kiểm tra dữ liệu nhân viên.");
                            setLoginLoading(false);
                        }
                    } else {
                        lblError.setText("Tên đăng nhập hoặc mật khẩu không đúng!");
                        setErrorBorder(txtUsername);
                        setErrorBorder(txtPassword);
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        setLoginLoading(false);
                    }
                } catch (Exception ex) {
                    lblError.setText("Lỗi kết nối: " + ex.getMessage());
                    setLoginLoading(false);
                }
            }
        };

        // UI safety timeout: never keep button locked forever when DB hangs.
        Timer watchdog = new Timer(12000, e -> {
            if (!worker.isDone()) {
                lblError.setText("Kết nối chậm/timeout. Vui lòng thử lại.");
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
}


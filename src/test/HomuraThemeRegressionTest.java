package test;

import database.DatabaseConnection;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import service.AuthService;
import ui.LoginFrame;
import ui.MainFrame;
import ui.panels.PhongPanel;
import ui.theme.Theme;
import ui.theme.ThemeManager;

/** Run with a desktop JDK; frames are rendered off screen and never shown. */
public class HomuraThemeRegressionTest {
    private static MainFrame main;
    private static LoginFrame login;

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static <T> List<T> controls(Component root, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (type.isInstance(root)) found.add(type.cast(root));
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) found.addAll(controls(child, type));
        }
        return found;
    }

    public static void main(String[] args) {
        int exitCode = 0;
        Theme original = ThemeManager.getCurrentTheme();
        try {
            DatabaseConnection.setDemoMode(true);
            AuthService.getInstance().dangNhap("manager", "123");
            ThemeManager.setTheme(Theme.HOMURA);
            SwingUtilities.invokeAndWait(() -> {
                login = new LoginFrame();
                main = new MainFrame();
                main.navigateTo("tongquan");
            });
            // Wait off the EDT so the asynchronous dashboard can finish.
            JLabel[] revenue = new JLabel[1];
            for (int i = 0; i < 100; i++) {
                SwingUtilities.invokeAndWait(() -> {
                    for (JLabel label : controls(main, JLabel.class)) {
                        if ("0.0M".equals(label.getText())) revenue[0] = label;
                    }
                });
                if (revenue[0] != null) break;
                Thread.sleep(50);
            }
            require(revenue[0] != null, "Demo dashboard did not finish loading revenue");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    JTextField username = (JTextField) field(login, "txtUsername");
                    JPasswordField password = (JPasswordField) field(login, "txtPassword");
                    username.setText("manager");
                    password.setText("keep-input");
                    JButton loginButton = (JButton) field(login, "btnLogin");
                    loginButton.setEnabled(false);
                    PhongPanel rooms = new PhongPanel();
                    main.getContentPane().add(rooms, java.awt.BorderLayout.SOUTH);
                    JTable table = controls(rooms, JTable.class).get(0);
                    JTextField search = controls(rooms, JTextField.class).get(0);
                    search.setText("P101");
                    for (Theme theme : new Theme[] { Theme.MADOKA, Theme.HOMURA, Theme.MADOKA }) {
                        ThemeManager.setTheme(theme);
                        require(revenue[0].getForeground() == theme.textPrimary, "Dashboard text kept old theme");
                        require("0.0M".equals(revenue[0].getText()), "Theme switch reset dashboard data");
                        require(username == field(login, "txtUsername"), "Theme switch replaced login input");
                        require("manager".equals(username.getText()), "Theme switch erased username");
                        require("keep-input".equals(new String(password.getPassword())), "Theme switch erased password");
                        require(!loginButton.isEnabled(), "Theme switch enabled blocked login button");
                        require("P101".equals(search.getText()), "Theme switch erased room search");
                        require(table.getBackground() == theme.bgCard, "Room table kept old background");
                        Component cell = table.getDefaultRenderer(Object.class)
                                .getTableCellRendererComponent(table, "P101", false, false, 0, 0);
                        require(cell.getBackground() == theme.tableRowEven, "Table renderer used white background");
                        require(cell.getForeground() == theme.textPrimary, "Table renderer kept old text");
                        JButton toggle = (JButton) field(main, "btnToggleTheme");
                        require(toggle.getText().contains(theme.isDark ? "Madoka" : "Homura"), "Theme button label stale");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("PASS: Demo dashboard loads; themes update repeatedly; login/search state and table contrast preserved.");
        } catch (Throwable e) {
            e.printStackTrace();
            exitCode = 1;
        } finally {
            ThemeManager.setTheme(original);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    if (login != null) login.dispose();
                    if (main != null) main.dispose();
                });
            } catch (Exception e) {
                e.printStackTrace();
                exitCode = 1;
            }
        }
        // MainFrame owns repeating timers, so terminate this standalone test process.
        System.exit(exitCode);
    }
}

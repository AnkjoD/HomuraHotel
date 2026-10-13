package ui.theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import javax.swing.UIManager;
import ui.components.UIConstants;

public class ThemeManager {
    private static final String PROP_FILE = "resources/theme.properties";
    private static final String PROP_FILE_ALT = "theme.properties";
    private static Theme currentTheme = Theme.HOMURA;
    private static final List<Consumer<Theme>> listeners = new ArrayList<>();

    static {
        loadPreference();
        applySwingDefaults(currentTheme);
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme.isDark;
    }

    public static void setTheme(Theme theme) {
        if (theme == null) return;
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> setTheme(theme));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Theme update interrupted", e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new IllegalStateException("Theme update failed", e.getCause());
            }
            return;
        }
        Theme previous = currentTheme;
        currentTheme = theme;
        UIConstants.applyTheme(theme);
        applySwingDefaults(theme);
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            ThemeSupport.apply(window, previous, theme);
        }
        savePreference();
        for (Consumer<Theme> listener : new ArrayList<>(listeners)) {
            try {
                listener.accept(theme);
            } catch (Exception ignored) {
            }
        }
    }

    public static void toggleTheme() {
        setTheme(currentTheme.isDark ? Theme.MADOKA : Theme.HOMURA);
    }

    public static synchronized void addThemeChangeListener(Consumer<Theme> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void removeThemeChangeListener(Consumer<Theme> listener) {
        listeners.remove(listener);
    }

    private static void applySwingDefaults(Theme t) {
        try {
            UIManager.put("Panel.background", t.bgMain);
            UIManager.put("Label.foreground", t.textPrimary);
            UIManager.put("Table.background", t.bgCard);
            UIManager.put("Table.foreground", t.textPrimary);
            UIManager.put("Table.selectionBackground", t.tableRowSelected);
            UIManager.put("Table.selectionForeground", t.textPrimary);
            UIManager.put("TableHeader.background", t.bgTableHeader);
            UIManager.put("TableHeader.foreground", t.textPrimary);
            UIManager.put("ScrollPane.background", t.bgMain);
            UIManager.put("Viewport.background", t.bgMain);
            UIManager.put("ScrollBarUI", "ui.theme.ThemedScrollBarUI");
            UIManager.put("TextField.background", t.inputBg);
            UIManager.put("TextField.foreground", t.textPrimary);
            UIManager.put("TextField.caretForeground", t.textPrimary);
            UIManager.put("PasswordField.background", t.inputBg);
            UIManager.put("PasswordField.foreground", t.textPrimary);
            UIManager.put("PasswordField.caretForeground", t.textPrimary);
            UIManager.put("ComboBox.background", t.inputBg);
            UIManager.put("ComboBox.foreground", t.textPrimary);
            UIManager.put("ToolTip.background", t.bgCardElevated);
            UIManager.put("ToolTip.foreground", t.textPrimary);
        } catch (Exception ignored) {
        }
    }

    private static void loadPreference() {
        File file = new File(PROP_FILE);
        if (!file.exists()) {
            file = new File(PROP_FILE_ALT);
        }
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                String name = props.getProperty("app.theme", "Homura");
                if ("Madoka".equalsIgnoreCase(name) || "light".equalsIgnoreCase(name)) {
                    currentTheme = Theme.MADOKA;
                } else {
                    currentTheme = Theme.HOMURA;
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static void savePreference() {
        File file = new File(PROP_FILE);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        Properties props = new Properties();
        props.setProperty("app.theme", currentTheme.name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Homura Hotel Theme Configuration");
        } catch (IOException e) {
            try (FileOutputStream fosAlt = new FileOutputStream(PROP_FILE_ALT)) {
                props.store(fosAlt, "Homura Hotel Theme Configuration");
            } catch (IOException ignored) {
            }
        }
    }
}

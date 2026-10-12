package ui.theme;

import java.awt.Color;

public class Theme {
    public final String name;
    public final boolean isDark;

    public final Color bgMain;
    public final Color bgSidebar;
    public final Color bgHeader;
    public final Color bgCard;
    public final Color bgCardElevated;
    public final Color bgTableHeader;

    public final Color tableRowEven;
    public final Color tableRowOdd;
    public final Color tableRowSelected;

    public final Color inputBg;
    public final Color inputBorder;
    public final Color inputFocusBorder;

    public final Color textPrimary;
    public final Color textSecondary;
    public final Color textMuted;

    public final Color primary;
    public final Color primaryLight;
    public final Color primaryDark;
    public final Color accent;

    public final Color border;
    public final Color borderFocus;

    public final Color sidebarActive;
    public final Color sidebarHover;
    public final Color sidebarText;
    public final Color sidebarTextActive;

    public final Color success;
    public final Color successLight;
    public final Color warning;
    public final Color warningLight;
    public final Color danger;
    public final Color dangerLight;
    public final Color info;
    public final Color infoLight;

    public Theme(
            String name,
            boolean isDark,
            Color bgMain,
            Color bgSidebar,
            Color bgHeader,
            Color bgCard,
            Color bgCardElevated,
            Color bgTableHeader,
            Color tableRowEven,
            Color tableRowOdd,
            Color tableRowSelected,
            Color inputBg,
            Color inputBorder,
            Color inputFocusBorder,
            Color textPrimary,
            Color textSecondary,
            Color textMuted,
            Color primary,
            Color primaryLight,
            Color primaryDark,
            Color accent,
            Color border,
            Color borderFocus,
            Color sidebarActive,
            Color sidebarHover,
            Color sidebarText,
            Color sidebarTextActive,
            Color success,
            Color successLight,
            Color warning,
            Color warningLight,
            Color danger,
            Color dangerLight,
            Color info,
            Color infoLight) {
        this.name = name;
        this.isDark = isDark;
        this.bgMain = bgMain;
        this.bgSidebar = bgSidebar;
        this.bgHeader = bgHeader;
        this.bgCard = bgCard;
        this.bgCardElevated = bgCardElevated;
        this.bgTableHeader = bgTableHeader;
        this.tableRowEven = tableRowEven;
        this.tableRowOdd = tableRowOdd;
        this.tableRowSelected = tableRowSelected;
        this.inputBg = inputBg;
        this.inputBorder = inputBorder;
        this.inputFocusBorder = inputFocusBorder;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textMuted = textMuted;
        this.primary = primary;
        this.primaryLight = primaryLight;
        this.primaryDark = primaryDark;
        this.accent = accent;
        this.border = border;
        this.borderFocus = borderFocus;
        this.sidebarActive = sidebarActive;
        this.sidebarHover = sidebarHover;
        this.sidebarText = sidebarText;
        this.sidebarTextActive = sidebarTextActive;
        this.success = success;
        this.successLight = successLight;
        this.warning = warning;
        this.warningLight = warningLight;
        this.danger = danger;
        this.dangerLight = dangerLight;
        this.info = info;
        this.infoLight = infoLight;
    }

    public static final Theme HOMURA = new Theme(
            "Homura",
            true,
            new Color(0x13, 0x11, 0x1C),
            new Color(0x18, 0x15, 0x24),
            new Color(0x18, 0x15, 0x24),
            new Color(0x1E, 0x1B, 0x2E),
            new Color(0x28, 0x23, 0x3D),
            new Color(0x24, 0x1F, 0x36),
            new Color(0x1E, 0x1B, 0x2E),
            new Color(0x18, 0x15, 0x24),
            new Color(0x3B, 0x2B, 0x61),
            new Color(0x25, 0x20, 0x38),
            new Color(0x3D, 0x35, 0x58),
            new Color(0x8B, 0x5C, 0xF6),
            new Color(0xF3, 0xF0, 0xFA),
            new Color(0xA2, 0x9D, 0xB8),
            new Color(0x6D, 0x67, 0x85),
            new Color(0x8B, 0x5C, 0xF6),
            new Color(0x2E, 0x24, 0x4E),
            new Color(0x7C, 0x3A, 0xED),
            new Color(0xA7, 0x8B, 0xFA),
            new Color(0x2E, 0x28, 0x45),
            new Color(0xA7, 0x8B, 0xFA),
            new Color(0x31, 0x25, 0x54),
            new Color(0x22, 0x1C, 0x34),
            new Color(0xC4, 0xBF, 0xD9),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0x10, 0xB9, 0x81),
            new Color(0x06, 0x4E, 0x3B),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0x78, 0x35, 0x0F),
            new Color(0xEF, 0x44, 0x44),
            new Color(0x7F, 0x1D, 0x1D),
            new Color(0x8B, 0x5C, 0xF6),
            new Color(0x2E, 0x24, 0x4E)
    );

    public static final Theme MADOKA = new Theme(
            "Madoka",
            false,
            new Color(0xFF, 0xF5, 0xF8),
            new Color(0xFF, 0xF0, 0xF5),
            new Color(0xFF, 0xF0, 0xF5),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0xFF, 0xF8, 0xFA),
            new Color(0xFD, 0xF2, 0xF8),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0xFF, 0xF9, 0xFB),
            new Color(0xFC, 0xE7, 0xF3),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0xFB, 0xCF, 0xE8),
            new Color(0xEC, 0x48, 0x99),
            new Color(0x3B, 0x1E, 0x2B),
            new Color(0x6B, 0x4E, 0x5D),
            new Color(0x9E, 0x7B, 0x8D),
            new Color(0xEC, 0x48, 0x99),
            new Color(0xFC, 0xE7, 0xF3),
            new Color(0xDB, 0x27, 0x77),
            new Color(0xF4, 0x3F, 0x5E),
            new Color(0xFC, 0xE7, 0xF0),
            new Color(0xEC, 0x48, 0x99),
            new Color(0xFC, 0xE7, 0xF3),
            new Color(0xFF, 0xF0, 0xF5),
            new Color(0x5B, 0x3A, 0x4C),
            new Color(0xBE, 0x18, 0x5D),
            new Color(0x05, 0x96, 0x69),
            new Color(0xD1, 0xFA, 0xE5),
            new Color(0xD9, 0x77, 0x06),
            new Color(0xFE, 0xF3, 0xC7),
            new Color(0xE1, 0x1D, 0x48),
            new Color(0xFF, 0xE4, 0xE6),
            new Color(0xEC, 0x48, 0x99),
            new Color(0xFC, 0xE7, 0xF3)
    );
}

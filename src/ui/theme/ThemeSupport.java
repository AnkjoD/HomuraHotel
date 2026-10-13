package ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.*;
import javax.swing.border.*;
import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;

/** Recolors existing controls without replacing their data, input or filters. */
public final class ThemeSupport {
    private ThemeSupport() {}

    public static Color remap(Color color, Theme from, Theme to) {
        Color[] oldColors = palette(from);
        Color[] newColors = palette(to);
        for (int i = 0; i < oldColors.length; i++) {
            // Identity preserves roles even when a theme gives two roles the same RGB.
            if (color == oldColors[i]) return newColors[i];
        }
        return color;
    }

    private static Color[] palette(Theme t) {
        return new Color[] { t.bgMain, t.bgSidebar, t.bgHeader, t.bgCard,
            t.bgCardElevated, t.bgTableHeader, t.tableRowEven, t.tableRowOdd,
            t.tableRowSelected, t.inputBg, t.inputBorder, t.inputFocusBorder,
            t.textPrimary, t.textSecondary, t.textMuted, t.primary, t.primaryLight,
            t.primaryDark, t.accent, t.border, t.borderFocus, t.sidebarActive,
            t.sidebarHover, t.sidebarText, t.sidebarTextActive, t.success,
            t.successLight, t.warning, t.warningLight, t.danger, t.dangerLight,
            t.info, t.infoLight };
    }

    public static void apply(Component component, Theme from, Theme to) {
        component.setBackground(remap(component.getBackground(), from, to));
        component.setForeground(remap(component.getForeground(), from, to));
        if (component instanceof JComponent control) {
            control.setBorder(remapBorder(control.getBorder(), from, to));
        }
        if (component instanceof RoundedButton button) button.applyTheme(from, to);
        if (component instanceof javax.swing.text.JTextComponent text) {
            text.setCaretColor(remap(text.getCaretColor(), from, to));
            text.setSelectionColor(remap(text.getSelectionColor(), from, to));
            text.setSelectedTextColor(remap(text.getSelectedTextColor(), from, to));
        }
        if (component instanceof JTable table) {
            table.setSelectionBackground(remap(table.getSelectionBackground(), from, to));
            table.setSelectionForeground(remap(table.getSelectionForeground(), from, to));
            table.setGridColor(remap(table.getGridColor(), from, to));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) apply(child, from, to);
        }
        component.repaint();
    }

    private static Border remapBorder(Border border, Theme from, Theme to) {
        if (border instanceof RoundedBorder rounded) return rounded.withTheme(from, to);
        if (border instanceof CompoundBorder compound) {
            return new CompoundBorder(remapBorder(compound.getOutsideBorder(), from, to),
                    remapBorder(compound.getInsideBorder(), from, to));
        }
        if (border instanceof LineBorder line) {
            return new LineBorder(remap(line.getLineColor(), from, to),
                    line.getThickness(), line.getRoundedCorners());
        }
        if (border instanceof MatteBorder matte && matte.getMatteColor() != null) {
            java.awt.Insets i = matte.getBorderInsets();
            return new MatteBorder(i.top, i.left, i.bottom, i.right,
                    remap(matte.getMatteColor(), from, to));
        }
        if (border instanceof TitledBorder titled) {
            titled.setBorder(remapBorder(titled.getBorder(), from, to));
            titled.setTitleColor(remap(titled.getTitleColor(), from, to));
        }
        return border;
    }
}

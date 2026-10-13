package ui.theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Compact scrollbar that follows the palette at paint time. */
public class ThemedScrollBarUI extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent component) {
        return new ThemedScrollBarUI();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        g.setColor(ThemeManager.getCurrentTheme().bgCard);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (!c.isEnabled() || bounds.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() ? ThemeManager.getCurrentTheme().textMuted
                : ThemeManager.getCurrentTheme().border);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        g2.dispose();
    }

    private JButton emptyButton() {
        JButton button = new JButton();
        Dimension size = new Dimension(0, 0);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    @Override protected JButton createDecreaseButton(int direction) { return emptyButton(); }
    @Override protected JButton createIncreaseButton(int direction) { return emptyButton(); }
}

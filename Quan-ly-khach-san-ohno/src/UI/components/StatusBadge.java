package ui.components;
 
import javax.swing.*;
import java.awt.*;
 
/**
 * A modern, rounded status badge component for labels.
 */
public class StatusBadge extends JPanel {
    private final String text;
    private final Color bgColor;
    private final Color fgColor;
    private final Font font = new Font("Segoe UI", Font.BOLD, 11); // Cỡ chữ 11 phù hợp cho hiển thị bảng biểu
 
    public StatusBadge(String text, Color bgColor, Color fgColor) {
        this.text = text;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        setOpaque(false);
    }
 
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 
        FontMetrics fm = g2.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent(); // Chiều cao font thực tế
        
        // Khống chế kích thước badge theo kích thước thực tế của Component để tránh vẽ tràn coordinate âm
        int badgeWidth = Math.min(getWidth(), textWidth + 14); // 7px padding mỗi bên
        int badgeHeight = Math.min(getHeight(), 22);
        
        // Tránh tọa độ âm gây lỗi mất chữ bên trái
        int x = Math.max(0, (getWidth() - badgeWidth) / 2);
        int y = Math.max(0, (getHeight() - badgeHeight) / 2);
 
        // Vẽ nền bo góc
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, badgeWidth, badgeHeight, 8, 8);
 
        // Vẽ chữ
        g2.setColor(fgColor);
        g2.setFont(font);
        
        // Rút gọn chữ có dấu ba chấm bên phải nếu chiều rộng cột bị co quá nhỏ
        String dispText = text;
        if (textWidth > badgeWidth - 8) {
            while (dispText.length() > 1 && fm.stringWidth(dispText + "..") > badgeWidth - 4) {
                dispText = dispText.substring(0, dispText.length() - 1);
            }
            dispText += "..";
            textWidth = fm.stringWidth(dispText);
        }
 
        int tx = x + (badgeWidth - textWidth) / 2;
        int ty = y + (badgeHeight + textHeight) / 2 - 1; // Căn giữa dọc chữ
        g2.drawString(dispText, tx, ty);
 
        g2.dispose();
    }
 
    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm != null ? fm.stringWidth(text) : text.length() * 7;
        return new Dimension(textWidth + 18, 24); // Tính toán preferred width động theo text
    }
}

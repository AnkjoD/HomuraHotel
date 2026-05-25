package ui.panels;

import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.NotificationManager;
import ui.components.RoundedComponents;
import static ui.components.RoundedComponents.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import javax.swing.*;
import service.ThongKeService;
import dao.ThongKeDAO;

// Apache POI Imports - Avoid wildcard to prevent 'Color' and 'Font' ambiguity
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class ThongKePanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        currentKy = "thang";
        refresh();
    }

    private final ThongKeService service = new ThongKeService();
    private final ThongKeDAO thongKeDAO = new ThongKeDAO();
    private String currentKy = "thang";

    public ThongKePanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildContent());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        ScrollablePanel content = new ScrollablePanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(20));
        content.add(buildKPIRow());
        content.add(Box.createVerticalStrut(20));
        content.add(buildChartRow());
        content.add(Box.createVerticalStrut(20));
        content.add(buildBottomRow());

        // Final safety: ensure content doesn't exceed viewport unnecessarily
        content.setMaximumSize(new Dimension(1600, Short.MAX_VALUE));
        return content;
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Báo cáo & Thống kê");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Tổng quan hiệu quả kinh doanh khách sạn");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Đổi size 10 thành 12 cho rõ ràng
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(title);
        left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        String[] kys = { "7 ngày", "Tháng này", "Quý này", "Năm nay" };
        String[] keys = { "7ngay", "thang", "quy", "nam" };
        ButtonGroup kyGroup = new ButtonGroup();
        for (int i = 0; i < kys.length; i++) {
            final String k = keys[i];
            JToggleButton btn = new JToggleButton(kys[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean hover = getModel().isRollover();
                    if (isSelected()) {
                        g2.setColor(UIConstants.PRIMARY);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    } else if (hover) {
                        g2.setColor(new Color(0xE2E8F0));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    } else {
                        g2.setColor(new Color(0xF1F5F9));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(UIConstants.FONT_SMALL_BOLD);
            btn.setFocusPainted(false);
            styleKyBtn(btn, k.equals(currentKy));
            btn.addActionListener(e -> {
                currentKy = k;
                refresh();
            });
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    btn.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    btn.repaint();
                }
            });
            kyGroup.add(btn);
            right.add(btn);
        }

        JToggleButton btnCustom = new JToggleButton("Tùy chỉnh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                if (isSelected()) {
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (hover) {
                    g2.setColor(new Color(0xE2E8F0));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCustom.setFont(UIConstants.FONT_SMALL_BOLD);
        btnCustom.setFocusPainted(false);
        styleKyBtn(btnCustom, currentKy.startsWith("custom:"));
        btnCustom.addActionListener(e -> showCustomDateDialog(btnCustom, right));
        btnCustom.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnCustom.repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnCustom.repaint();
            }
        });
        kyGroup.add(btnCustom);
        right.add(btnCustom);

        RoundedButton btnExport = new RoundedButton("Xuất Excel", new Color(0x107C41), Color.WHITE) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(115, 32);
            }
        };
        btnExport.setFont(UIConstants.FONT_SMALL_BOLD);
        btnExport.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, 12, 12, 2, 2);
                g2.drawLine(x + 3, y + 3, x + 9, y + 3);
                g2.drawLine(x + 3, y + 6, x + 9, y + 6);
                g2.drawLine(x + 3, y + 9, x + 9, y + 9);
                g2.drawLine(x + 3, y + 2, x + 3, y + 10);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 13;
            }

            @Override
            public int getIconHeight() {
                return 13;
            }
        });
        btnExport.addActionListener(e -> showExportDialog());
        right.add(btnExport);

        panel.add(left, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private void styleKyBtn(JToggleButton btn, boolean active) {
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setSelected(active);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        if (active) {
            btn.setForeground(Color.WHITE);
        } else {
            btn.setForeground(UIConstants.TEXT_SECONDARY);
        }
    }

    private JPanel buildKPIRow() {
        Map<String, Object> stats = service.getThongKeKy(currentKy);

        // Fetch metrics
        double revpar = (double) stats.getOrDefault("revpar", 0.0);
        double revparTruoc = (double) stats.getOrDefault("revparTruoc", 0.0);

        double occupancyRate = (double) stats.getOrDefault("occupancyRate", 0.0);
        double occupancyRateTruoc = (double) stats.getOrDefault("occupancyRateTruoc", 0.0);

        double adr = (double) stats.getOrDefault("adr", 0.0);
        double adrTruoc = (double) stats.getOrDefault("adrTruoc", 0.0);

        double alos = (double) stats.getOrDefault("alos", 0.0);
        double alosTruoc = (double) stats.getOrDefault("alosTruoc", 0.0);

        double repeatRate = (double) stats.getOrDefault("repeatRate", 0.0);
        double repeatRateTruoc = (double) stats.getOrDefault("repeatRateTruoc", 0.0);

        int luot = (int) stats.getOrDefault("luotDatPhong", 0);
        int luotTruoc = (int) stats.getOrDefault("luotDatPhongTruoc", 0);

        long dt = (long) stats.getOrDefault("doanhThu", 0L);
        long dtTruoc = (long) stats.getOrDefault("doanhThuTruoc", 0L);

        long dtDV = (long) stats.getOrDefault("doanhThuDV", 0L);
        long dtDVTruoc = (long) stats.getOrDefault("doanhThuDVTruoc", 0L);

        long operatingCost = (long) stats.getOrDefault("operatingCost", 0L);
        long operatingCostTruoc = (long) stats.getOrDefault("operatingCostTruoc", 0L);

        long netProfit = (long) stats.getOrDefault("netProfit", 0L);
        long netProfitTruoc = (long) stats.getOrDefault("netProfitTruoc", 0L);

        double netProfitMargin = (double) stats.getOrDefault("netProfitMargin", 0.0);
        double netProfitMarginTruoc = (double) stats.getOrDefault("netProfitMarginTruoc", 0.0);

        int phongDangThue = (int) stats.getOrDefault("phongDangThue", 0);

        // Chuyển thành lưới 3 hàng x 4 cột để các thẻ cực kỳ rộng rãi, không bị lỗi
        // form
        JPanel row = new JPanel(new GridLayout(3, 4, 10, 10));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 260));
        row.setPreferredSize(new Dimension(1000, 260));

        String tk = getTenKy();
        String displayKy = (tk.length() > 12) ? tk : "Kỳ " + tk;

        // Hàng 1: Vận hành phòng
        row.add(buildKpiCard("Doanh thu trên phòng khả dụng (RevPAR)", String.format("%,.0fđ", revpar), displayKy,
                UIConstants.PRIMARY, calcTrendDouble(revpar, revparTruoc)));
        row.add(buildKpiCard("Công suất sử dụng phòng", String.format("%.1f%%", occupancyRate), displayKy,
                UIConstants.SUCCESS, calcTrendDouble(occupancyRate, occupancyRateTruoc)));
        row.add(buildKpiCard("Giá trung bình mỗi ngày (ADR)", String.format("%,.0fđ", adr), displayKy,
                UIConstants.INFO, calcTrendDouble(adr, adrTruoc)));
        row.add(buildKpiCard("Thời gian lưu trú trung bình (ALOS)", String.format("%.1f ngày", alos), displayKy,
                UIConstants.WARNING, calcTrendDouble(alos, alosTruoc)));

        // Hàng 2: Trạng thái & Khách hàng
        row.add(buildKpiCard("Tỷ lệ khách quay lại", String.format("%.1f%%", repeatRate), displayKy,
                new Color(0x8B5CF6), calcTrendDouble(repeatRate, repeatRateTruoc)));
        row.add(buildKpiCard("Lượt nhận phòng", luot + " lượt", displayKy,
                UIConstants.DANGER, calcTrend(luot, luotTruoc)));
        row.add(buildKpiCard("Phòng đang thuê", phongDangThue + " phòng", "Hiện tại",
                new Color(0xF97316), Integer.MIN_VALUE));
        row.add(buildKpiCard("Tỷ suất LN ròng", String.format("%.1f%%", netProfitMargin), displayKy,
                new Color(0xEC4899), calcTrendDouble(netProfitMargin, netProfitMarginTruoc)));

        // Hàng 3: Tài chính
        row.add(buildKpiCard("Tổng doanh thu", String.format("%,.0fđ", (double) dt), displayKy,
                UIConstants.PRIMARY, calcTrend(dt, dtTruoc)));
        row.add(buildKpiCard("Doanh thu DV", String.format("%,.0fđ", (double) dtDV), displayKy,
                UIConstants.SUCCESS, calcTrend(dtDV, dtDVTruoc)));
        row.add(buildKpiCard("Chi phí vận hành", String.format("%,.0fđ", (double) operatingCost), displayKy,
                UIConstants.DANGER, calcTrend(operatingCost, operatingCostTruoc)));
        row.add(buildKpiCard("Lợi nhuận ròng", String.format("%,.0fđ", (double) netProfit), displayKy,
                UIConstants.INFO, calcTrend(netProfit, netProfitTruoc)));

        return row;
    }

    private int calcTrend(long current, long previous) {
        if (previous == 0)
            return current > 0 ? 100 : 0;
        return (int) ((current - previous) * 100 / previous);
    }

    private int calcTrendDouble(double current, double previous) {
        if (previous == 0.0)
            return current > 0.0 ? 100 : 0;
        return (int) Math.round((current - previous) * 100.0 / previous);
    }

    private void drawIcon(Graphics2D g2, String label, int x, int y, int w, int h) {
        String l = label.toLowerCase();
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (l.contains("revpar")) {
            // Sparkline arrow
            g2.drawLine(x + 2, y + h - 4, x + 7, y + h - 8);
            g2.drawLine(x + 7, y + h - 8, x + 13, y + h - 6);
            g2.drawLine(x + 13, y + h - 6, x + w - 3, y + 4);
            // Arrow head
            g2.drawLine(x + w - 8, y + 4, x + w - 3, y + 4);
            g2.drawLine(x + w - 3, y + 9, x + w - 3, y + 4);
        } else if (l.contains("occupancy")) {
            // Bed icon
            g2.drawLine(x + 2, y + 5, x + 2, y + h - 4); // Headboard
            g2.drawRoundRect(x + 2, y + 9, w - 4, h - 13, 2, 2); // Bed frame
            g2.drawRoundRect(x + 4, y + 6, 5, 3, 1, 1); // Pillow 1
            g2.drawRoundRect(x + 11, y + 6, 5, 3, 1, 1); // Pillow 2
        } else if (l.contains("đang thuê")) {
            // House/Lodging icon
            g2.drawLine(x + 2, y + 9, x + w / 2, y + 2); // Roof left
            g2.drawLine(x + w / 2, y + 2, x + w - 2, y + 9); // Roof right
            g2.drawRect(x + 4, y + 9, w - 8, h - 11); // Base
            g2.drawRect(x + w / 2 - 2, y + 14, 4, 4); // Door
        } else if (l.contains("adr")) {
            // Price tag
            int xc = x + w / 2;
            int[] tx = { xc, x + w - 2, x + w - 2, x + 2, x + 2 };
            int[] ty = { y + 2, y + 7, y + h - 2, y + h - 2, y + 7 };
            g2.drawPolygon(tx, ty, 5);
            g2.drawOval(xc - 1, y + 5, 2, 2); // Hole
        } else if (l.contains("alos")) {
            // Calendar
            g2.drawRoundRect(x + 2, y + 3, w - 4, h - 5, 2, 2);
            g2.drawLine(x + 2, y + 8, x + w - 2, y + 8);
            g2.drawLine(x + 6, y + 1, x + 6, y + 4);
            g2.drawLine(x + 14, y + 1, x + 14, y + 4);
            g2.fillRect(x + 5, y + 11, 2, 2);
            g2.fillRect(x + 9, y + 11, 2, 2);
            g2.fillRect(x + 13, y + 11, 2, 2);
            g2.fillRect(x + 5, y + 15, 2, 2);
            g2.fillRect(x + 9, y + 15, 2, 2);
            g2.fillRect(x + 13, y + 15, 2, 2);
        } else if (l.contains("quay lại")) {
            // Circular Return Arrow
            g2.drawArc(x + 2, y + 2, w - 4, h - 4, 50, 270);
            g2.drawLine(x + w / 2 - 1, y + 2, x + w / 2 - 4, y - 1);
            g2.drawLine(x + w / 2 - 1, y + 2, x + w / 2 + 2, y - 1);
        } else if (l.contains("check-in") || l.contains("lượt")) {
            // Hotel Desk Bell
            g2.drawArc(x + 2, y + 5, w - 4, h - 8, 0, 180); // Dome
            g2.drawLine(x, y + h - 3, x + w, y + h - 3); // Base stand
            g2.drawLine(x + w / 2, y + 1, x + w / 2, y + 5); // Stand stick
            g2.fillRect(x + w / 2 - 1, y + 1, 3, 2); // Bell button
        } else if (l.contains("doanh thu dv")) {
            // Shopping Bag
            g2.drawRoundRect(x + 4, y + 6, w - 8, h - 8, 2, 2);
            g2.drawArc(x + w / 2 - 3, y + 2, 6, 8, 0, 180);
        } else if (l.contains("doanh thu")) {
            // VND 'đ' character
            g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            String text = "đ";
            int tx = x + (w - fm.stringWidth(text)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g2.drawString(text, tx, ty);
        } else if (l.contains("lợi nhuận")) {
            // Money bag
            g2.drawOval(x + 4, y + 7, w - 8, h - 9);
            g2.drawLine(x + 7, y + 7, x + 6, y + 3);
            g2.drawLine(x + w - 7, y + 7, x + w - 6, y + 3);
            g2.drawLine(x + 6, y + 3, x + w - 6, y + 3);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString("$", x + w / 2 - 3, y + h / 2 + 5);
        } else if (l.contains("chi phí")) {
            // Gear
            g2.drawOval(x + 5, y + 5, w - 10, h - 10);
            g2.drawOval(x + w / 2 - 1, y + h / 2 - 1, 2, 2);
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                int x1 = (int) (x + w / 2 + Math.cos(rad) * 4);
                int y1 = (int) (y + h / 2 + Math.sin(rad) * 4);
                int x2 = (int) (x + w / 2 + Math.cos(rad) * 7);
                int y2 = (int) (y + h / 2 + Math.sin(rad) * 7);
                g2.drawLine(x1, y1, x2, y2);
            }
        } else if (l.contains("tỷ suất")) {
            // Net profit margin percent symbol
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            String text = "%";
            int tx = x + (w - fm.stringWidth(text)) / 2;
            int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g2.drawString(text, tx, ty);
        } else {
            // Default elegant dot-circle
            g2.drawOval(x + 2, y + 2, w - 4, h - 4);
            g2.fillOval(x + w / 2 - 2, y + h / 2 - 2, 4, 4);
        }
    }

    private RoundedPanel buildKpiCard(String label, String value, String subtitle, java.awt.Color accent,
            int trendPct) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dải accent color dọc sang trọng bên trái
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 2, 2);

                // Live pulsing dot cho real-time card
                if (subtitle.equals("Hiện tại")) {
                    float alpha = (float) (Math.sin(System.currentTimeMillis() / 400.0) * 0.5 + 0.5);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(UIConstants.SUCCESS);
                    g2.fillOval(getWidth() - 12, 10, 6, 6);
                }
                g2.dispose();
            }
        };
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout(8, 0));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setToolTipText(label + ": " + value);

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Nhãn chỉ tiêu KPI - hiển thị dạng text ngắn hoặc có tooltip
        String displayLabel = label;
        if (label.contains("(")) {
            displayLabel = label.substring(0, label.indexOf("(")).trim();
        }
        JLabel lblLabel = new JLabel(displayLabel);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(lblLabel);
        leftPanel.add(Box.createVerticalStrut(4));

        // Giá trị lớn nổi bật
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblVal.setForeground(UIConstants.TEXT_PRIMARY);
        lblVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(lblVal);
        leftPanel.add(Box.createVerticalStrut(5));

        // Huy hiệu xu hướng dạng viên thuốc (Pill Badge)
        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgePanel.setOpaque(false);
        badgePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (trendPct != Integer.MIN_VALUE) {
            String arrow = trendPct > 0 ? "↑" : trendPct < 0 ? "↓" : "—";
            java.awt.Color bgPill;
            java.awt.Color fgPill;
            if (trendPct > 0) {
                bgPill = UIConstants.SUCCESS_LIGHT;
                fgPill = UIConstants.SUCCESS;
            } else if (trendPct < 0) {
                bgPill = UIConstants.DANGER_LIGHT;
                fgPill = UIConstants.DANGER;
            } else {
                bgPill = new Color(0xF1F5F9);
                fgPill = new Color(0x64748B);
            }
            String trendText = trendPct == 0 ? "—" : arrow + " " + Math.abs(trendPct) + "%";
            StatusBadge trendBadge = new StatusBadge(trendText, bgPill, fgPill);
            trendBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
            trendBadge.setBackground(bgPill);
            badgePanel.add(trendBadge);

            JLabel lblSub = new JLabel(" so với trước");
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            lblSub.setForeground(UIConstants.TEXT_SECONDARY);
            badgePanel.add(lblSub);
        } else {
            StatusBadge liveBadge = new StatusBadge("Trực tiếp", new Color(0xFFEDD5), new Color(0xEA580C));
            liveBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
            liveBadge.setBackground(new Color(0xFFEDD5));
            badgePanel.add(liveBadge);

            JLabel lblSub = new JLabel(" " + subtitle);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            lblSub.setForeground(UIConstants.TEXT_SECONDARY);
            badgePanel.add(lblSub);
        }
        leftPanel.add(badgePanel);

        // Panel bên phải vẽ icon tròn
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);

        final String iconLabel = label;
        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(accent);
                drawIcon(g2, iconLabel, 12, 12, getWidth() - 24, getHeight() - 24);
                g2.dispose();
            }
        };
        iconCircle.setPreferredSize(new Dimension(44, 44));
        iconCircle.setOpaque(false);
        rightPanel.add(iconCircle);

        card.add(leftPanel, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);
        return card;
    }

    private JPanel buildChartRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 480));
        row.setPreferredSize(new Dimension(1000, 480));
        row.add(buildBarChart());
        row.add(buildDonutChart());
        return row;
    }

    private RoundedPanel buildBarChart() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        String unitLabel = ("nam".equals(currentKy) || "quy".equals(currentKy)) ? "tháng" : "ngày";
        String tk = getTenKy();
        JLabel title = new JLabel(
                "Doanh thu theo " + unitLabel + " — " + (tk.length() > 25 ? tk.substring(0, 22) + "..." : tk));
        title.setToolTipText("Doanh thu theo " + unitLabel + " — " + tk);
        title.setFont(UIConstants.FONT_HEADER);
        JLabel unit = new JLabel("Đơn vị: triệu đồng");
        unit.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        unit.setForeground(UIConstants.TEXT_MUTED);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(unit, BorderLayout.EAST);

        java.util.List<long[]> data = service.getDoanhThuTheoNgay(currentKy);

        card.add(hdr, BorderLayout.NORTH);
        card.add(new ModernBarChart(data), BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildDonutChart() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Công suất phòng (%)");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        // Lấy data thật từ DB
        Map<String, Object> dash = thongKeDAO.getDashboardStats();
        int dangO = (int) dash.getOrDefault("dangO", 0);
        int phongT = (int) dash.getOrDefault("phongTrong", 0);
        int daDat = (int) dash.getOrDefault("daDat", 0);
        int tongP = Math.max(1, (int) dash.getOrDefault("tongPhong", 1));
        int baoTri = Math.max(0, tongP - dangO - phongT - daDat);

        double[] values = { (double) dangO, (double) phongT, (double) daDat, (double) baoTri };
        java.awt.Color[] colors = { UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.WARNING, UIConstants.DANGER };
        String[] labels = { "Đang ở", "Trống", "Đặt trước", "Bảo trì" };

        card.add(new ModernDonutChart(values, colors, labels, tongP), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 18, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 350));
        row.setPreferredSize(new Dimension(1000, 350));
        row.add(buildTopPhongCard());
        row.add(buildTopDichVuCard());
        row.add(buildNguonDatPhongCard());
        return row;
    }

    private RoundedPanel buildTopPhongCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Top phòng doanh thu");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        java.util.List<Map<String, Object>> tops = service.getTopPhong(5, currentKy);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (tops.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu trong kỳ này");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            long maxDT = tops.stream()
                    .mapToLong(t -> t.containsKey("doanhThu") ? (long) t.get("doanhThu") : 0L)
                    .max().orElse(1L);

            java.awt.Color[] barColors = { UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.INFO,
                    UIConstants.WARNING, UIConstants.TEXT_SECONDARY };

            for (int i = 0; i < tops.size(); i++) {
                final Map<String, Object> t = tops.get(i);
                final String soPhong = String.valueOf(t.getOrDefault("soPhong", ""));
                final String tenLoai = String.valueOf(t.getOrDefault("tenLoai", ""));
                final long dt = t.containsKey("doanhThu") ? (long) t.get("doanhThu") : 0L;
                final int luot = t.containsKey("luot") ? (int) t.get("luot") : 0;
                final int pct = (int) (100L * dt / maxDT);
                final int rank = i + 1;

                final java.awt.Color barColor = barColors[i % barColors.length];
                String cleanedTenLoai = tenLoai;
                if (cleanedTenLoai.startsWith("Phòng ")) {
                    cleanedTenLoai = cleanedTenLoai.substring(6);
                } else if (cleanedTenLoai.startsWith("phòng ")) {
                    cleanedTenLoai = cleanedTenLoai.substring(6);
                }
                final String roomLabel = "P." + soPhong + " (" + cleanedTenLoai + ")";
                final String dtStr = String.format("%,.0f tr", dt / 1_000_000.0);

                JPanel item = new JPanel(new BorderLayout(6, 2));
                item.setOpaque(false);
                item.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

                JPanel nameRow = new JPanel(new BorderLayout());
                nameRow.setOpaque(false);

                // Badge xếp hạng
                JPanel badge = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        Color bg;
                        Color fg = Color.WHITE;
                        if (rank == 1)
                            bg = new Color(0xF59E0B);
                        else if (rank == 2)
                            bg = new Color(0x94A3B8);
                        else if (rank == 3)
                            bg = new Color(0xD97706);
                        else {
                            bg = new Color(0xF1F5F9);
                            fg = new Color(0x64748B);
                        }
                        g2.setColor(bg);
                        g2.fillOval(0, 0, getWidth(), getHeight());

                        g2.setColor(fg);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        String txt = String.valueOf(rank);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2,
                                (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1);
                        g2.dispose();
                    }
                };
                badge.setPreferredSize(new Dimension(18, 18));
                badge.setOpaque(false);

                JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                leftPart.setOpaque(false);
                leftPart.add(badge);
                JLabel lblName = new JLabel(roomLabel);
                lblName.setFont(UIConstants.FONT_SMALL_BOLD);
                leftPart.add(lblName);

                JLabel lblInfo = new JLabel(luot + " lượt  " + dtStr);
                lblInfo.setFont(UIConstants.FONT_SMALL);
                lblInfo.setForeground(UIConstants.TEXT_MUTED);

                nameRow.add(leftPart, BorderLayout.WEST);
                nameRow.add(lblInfo, BorderLayout.EAST);

                JPanel bar = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new java.awt.Color(0xF1F5F9));
                        g2.fillRoundRect(0, 5, getWidth(), 10, 5, 5);

                        GradientPaint gp = new GradientPaint(0, 0, barColor, getWidth(), 0,
                                new java.awt.Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 120));
                        g2.setPaint(gp);
                        g2.fillRoundRect(0, 5, getWidth() * pct / 100, 10, 5, 5);
                        g2.dispose();
                    }
                };
                bar.setOpaque(false);
                bar.setPreferredSize(new Dimension(0, 20));

                item.add(nameRow, BorderLayout.NORTH);
                item.add(bar, BorderLayout.CENTER);
                list.add(item);
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildTopDichVuCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Dịch vụ bán chạy");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        java.util.List<Map<String, Object>> dvList = service.getTopDichVu(5, currentKy);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (dvList.isEmpty()) {
            JLabel empty = new JLabel("Chưa có dữ liệu dịch vụ");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            java.awt.Color[] dvColors = { UIConstants.PRIMARY, UIConstants.INFO, UIConstants.WARNING,
                    UIConstants.SUCCESS, UIConstants.ORANGE };

            for (int i = 0; i < dvList.size(); i++) {
                final Map<String, Object> dv = dvList.get(i);
                final String tenDV = String.valueOf(dv.getOrDefault("tenDV", ""));
                final int soLan = dv.containsKey("soLan") ? (int) dv.get("soLan") : 0;
                final long dtDV = dv.containsKey("doanhThu") ? (long) dv.get("doanhThu") : 0L;
                final int rank = i + 1;
                final java.awt.Color c = dvColors[i % dvColors.length];

                JPanel item = new JPanel(new BorderLayout(8, 0));
                item.setOpaque(false);
                item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF1F5F9)));
                item.setPreferredSize(new Dimension(300, 48));

                JPanel left = new JPanel(new GridBagLayout());
                left.setOpaque(false);

                // Badge hình vuông tròn
                JPanel dot = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                        g2.setColor(c);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        String txt = String.valueOf(rank);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(txt, (getWidth() - fm.stringWidth(txt)) / 2,
                                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                        g2.dispose();
                    }
                };
                dot.setOpaque(false);
                dot.setPreferredSize(new Dimension(20, 20));

                GridBagConstraints gbcLeft = new GridBagConstraints();
                gbcLeft.gridx = 0;
                gbcLeft.gridy = 0;
                gbcLeft.anchor = GridBagConstraints.CENTER;
                gbcLeft.insets = new Insets(0, 8, 0, 8);
                left.add(dot, gbcLeft);

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                JLabel l1 = new JLabel(tenDV);
                l1.setFont(UIConstants.FONT_SMALL_BOLD);
                l1.setForeground(UIConstants.TEXT_PRIMARY);
                JLabel l2 = new JLabel(soLan + " lượt bán");
                l2.setFont(UIConstants.FONT_SMALL);
                l2.setForeground(UIConstants.TEXT_MUTED);
                info.add(l1);
                info.add(l2);

                gbcLeft.gridx = 1;
                gbcLeft.anchor = GridBagConstraints.WEST;
                gbcLeft.insets = new Insets(0, 0, 0, 0);
                left.add(info, gbcLeft);

                JPanel rightPanel = new JPanel(new GridBagLayout());
                rightPanel.setOpaque(false);

                JLabel lDt = new JLabel(String.format("%,.0fđ", (double) dtDV));
                lDt.setFont(UIConstants.FONT_SMALL_BOLD);
                lDt.setForeground(UIConstants.PRIMARY);
                lDt.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));

                GridBagConstraints gbcRight = new GridBagConstraints();
                gbcRight.gridx = 0;
                gbcRight.gridy = 0;
                gbcRight.anchor = GridBagConstraints.CENTER;
                rightPanel.add(lDt, gbcRight);

                item.add(left, BorderLayout.WEST);
                item.add(rightPanel, BorderLayout.EAST);
                list.add(item);
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel buildNguonDatPhongCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(java.awt.Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Nguồn đặt phòng");
        title.setFont(UIConstants.FONT_HEADER);
        JLabel note = new JLabel("(Demo)");
        note.setFont(UIConstants.FONT_SMALL);
        note.setForeground(UIConstants.TEXT_MUTED);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(note, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        String[][] nguons = { { "Trực tiếp", "38%" }, { "Booking.com", "28%" }, { "Agoda", "18%" }, { "Airbnb", "10%" },
                { "Khác", "6%" } };
        java.awt.Color[] cs = { UIConstants.PRIMARY, UIConstants.SUCCESS, UIConstants.WARNING, UIConstants.INFO,
                UIConstants.TEXT_MUTED };

        for (int i = 0; i < nguons.length; i++) {
            final java.awt.Color barC = cs[i];
            final int pct = Integer.parseInt(nguons[i][1].replace("%", ""));
            JPanel item = new JPanel(new BorderLayout(8, 0));
            item.setOpaque(false);
            item.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));
            JLabel lName = new JLabel(nguons[i][0]);
            lName.setFont(UIConstants.FONT_SMALL_BOLD);
            lName.setForeground(UIConstants.TEXT_PRIMARY);
            lName.setPreferredSize(new Dimension(95, 16));

            JPanel bar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new java.awt.Color(0xF1F5F9));
                    g2.fillRoundRect(0, 5, getWidth(), 10, 5, 5);

                    GradientPaint gp = new GradientPaint(0, 0, barC, getWidth(), 0,
                            new java.awt.Color(barC.getRed(), barC.getGreen(), barC.getBlue(), 120));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 5, (int) (getWidth() * pct / 100.0), 10, 5, 5);
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setPreferredSize(new Dimension(0, 20));

            JLabel lPct = new JLabel(nguons[i][1]);
            lPct.setFont(UIConstants.FONT_SMALL_BOLD);
            lPct.setForeground(barC);
            lPct.setPreferredSize(new Dimension(34, 16));

            item.add(lName, BorderLayout.WEST);
            item.add(bar, BorderLayout.CENTER);
            item.add(lPct, BorderLayout.EAST);
            list.add(item);
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private void showExportDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel lbTitle = new JLabel("Chọn các nội dung báo cáo muốn xuất:");
        lbTitle.setFont(UIConstants.FONT_BODY_BOLD);
        lbTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbTitle);
        panel.add(Box.createVerticalStrut(12));

        JCheckBox chkOverview = new JCheckBox("1. Báo cáo tổng quan (Overview)");
        JCheckBox chkRoom = new JCheckBox("2. Hiệu suất kinh doanh phòng (Room Performance)");
        JCheckBox chkService = new JCheckBox("3. Thống kê sử dụng dịch vụ (Service Performance)");
        JCheckBox chkChannel = new JCheckBox("4. Hiệu quả kênh phân phối (Channel Performance)");
        JCheckBox chkCustomer = new JCheckBox("5. Phân tích phân khúc khách hàng (Customer Segments)");
        JCheckBox chkYoY = new JCheckBox("6. So sánh cùng kỳ năm trước (YoY Comparison)");

        for (JCheckBox cb : new JCheckBox[] { chkOverview, chkRoom, chkService, chkChannel, chkCustomer, chkYoY }) {
            cb.setSelected(true);
            cb.setFont(UIConstants.FONT_BODY);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(cb);
            panel.add(Box.createVerticalStrut(6));
        }

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Xuất báo cáo Excel chuyên nghiệp",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
            return;
        if (!chkOverview.isSelected() && !chkRoom.isSelected() && !chkService.isSelected()
                && !chkChannel.isSelected() && !chkCustomer.isSelected() && !chkYoY.isSelected()) {
            NotificationManager.showWarning("Cảnh báo", "Vui lòng chọn ít nhất 1 sheet để xuất!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu file báo cáo");
        chooser.setSelectedFile(new File("BaoCaoKhachSan_" + getTenKy().replace(" - ", "_").replace("/", "") + "_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx"))
            file = new File(file.getAbsolutePath() + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Style definitions
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle subtitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setFontHeightInPoints((short) 10);
            subFont.setItalic(true);
            subtitleStyle.setFont(subFont);
            subtitleStyle.setAlignment(HorizontalAlignment.LEFT);

            CellStyle sectionStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font sectFont = workbook.createFont();
            sectFont.setFontHeightInPoints((short) 12);
            sectFont.setBold(true);
            sectFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
            sectionStyle.setFont(sectFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setFont(workbook.createFont());

            CellStyle dataCenterStyle = workbook.createCellStyle();
            dataCenterStyle.cloneStyleFrom(dataStyle);
            dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"đ\""));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.cloneStyleFrom(dataStyle);
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0%"));
            percentStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle doubleStyle = workbook.createCellStyle();
            doubleStyle.cloneStyleFrom(dataStyle);
            doubleStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0"));
            doubleStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(dataStyle);
            org.apache.poi.ss.usermodel.Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            CellStyle totalCurrencyStyle = workbook.createCellStyle();
            totalCurrencyStyle.cloneStyleFrom(currencyStyle);
            totalCurrencyStyle.setFont(totalFont);

            CellStyle totalPercentStyle = workbook.createCellStyle();
            totalPercentStyle.cloneStyleFrom(percentStyle);
            totalPercentStyle.setFont(totalFont);

            CellStyle trendUpStyle = workbook.createCellStyle();
            trendUpStyle.cloneStyleFrom(dataCenterStyle);
            org.apache.poi.ss.usermodel.Font greenFont = workbook.createFont();
            greenFont.setColor(IndexedColors.GREEN.getIndex());
            greenFont.setBold(true);
            trendUpStyle.setFont(greenFont);

            CellStyle trendDownStyle = workbook.createCellStyle();
            trendDownStyle.cloneStyleFrom(dataCenterStyle);
            org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            redFont.setBold(true);
            trendDownStyle.setFont(redFont);

            // Fetch common statistics
            Map<String, Object> stats = service.getThongKeKy(currentKy);

            // Sheet 1: BÁO CÁO TỔNG QUAN
            if (chkOverview.isSelected()) {
                Sheet sheet = workbook.createSheet("1. Tổng Quan");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("BÁO CÁO HIỆU QUẢ HOẠT ĐỘNG TỔNG QUAN");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ thống kê: " + getTenKy() + " | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row rSectA = sheet.createRow(rIdx++);
                rSectA.createCell(0).setCellValue("A. CÁC CHỈ SỐ KPI CHÍNH");
                rSectA.getCell(0).setCellStyle(sectionStyle);

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "Chỉ tiêu KPI", "Kỳ này", "Kỳ trước", "Thay đổi (%)", "Xu hướng" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                Object[][] kpiData = {
                        { "RevPAR (Doanh thu trên phòng khả dụng)", stats.getOrDefault("revpar", 0.0),
                                stats.getOrDefault("revparTruoc", 0.0), "currency" },
                        { "Công suất phòng trung bình", (double) stats.getOrDefault("occupancyRate", 0.0) / 100.0,
                                (double) stats.getOrDefault("occupancyRateTruoc", 0.0) / 100.0, "percent" },
                        { "Giá phòng trung bình ngày (ADR)", stats.getOrDefault("adr", 0.0),
                                stats.getOrDefault("adrTruoc", 0.0), "currency" },
                        { "Độ dài lưu trú trung bình (ALOS)", stats.getOrDefault("alos", 0.0),
                                stats.getOrDefault("alosTruoc", 0.0), "double" },
                        { "Tỷ lệ khách hàng quay lại", (double) stats.getOrDefault("repeatRate", 0.0) / 100.0,
                                (double) stats.getOrDefault("repeatRateTruoc", 0.0) / 100.0, "percent" },
                        { "Tổng số lượt đặt phòng / nhận phòng", (double) (int) stats.getOrDefault("luotDatPhong", 0),
                                (double) (int) stats.getOrDefault("luotDatPhongTruoc", 0), "number" },
                        { "Tổng doanh thu (Gross Revenue)", (double) (long) stats.getOrDefault("doanhThu", 0L),
                                (double) (long) stats.getOrDefault("doanhThuTruoc", 0L), "currency" },
                        { "Doanh thu dịch vụ (Service Revenue)", (double) (long) stats.getOrDefault("doanhThuDV", 0L),
                                (double) (long) stats.getOrDefault("doanhThuDVTruoc", 0L), "currency" },
                        { "Chi phí hoạt động (Operating Cost)", (double) (long) stats.getOrDefault("operatingCost", 0L),
                                (double) (long) stats.getOrDefault("operatingCostTruoc", 0L), "currency" },
                        { "Lợi nhuận ròng (Net Profit)", (double) (long) stats.getOrDefault("netProfit", 0L),
                                (double) (long) stats.getOrDefault("netProfitTruoc", 0L), "currency" },
                        { "Tỷ suất lợi nhuận ròng (Profit Margin)",
                                (double) stats.getOrDefault("netProfitMargin", 0.0) / 100.0,
                                (double) stats.getOrDefault("netProfitMarginTruoc", 0.0) / 100.0, "percent" }
                };

                for (Object[] kpi : kpiData) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue((String) kpi[0]);
                    r.getCell(0).setCellStyle(dataStyle);

                    double curr = ((Number) kpi[1]).doubleValue();
                    double prev = ((Number) kpi[2]).doubleValue();
                    String type = (String) kpi[3];

                    Cell cCurr = r.createCell(1);
                    Cell cPrev = r.createCell(2);
                    cCurr.setCellValue(curr);
                    cPrev.setCellValue(prev);

                    if ("currency".equals(type)) {
                        cCurr.setCellStyle(currencyStyle);
                        cPrev.setCellStyle(currencyStyle);
                    } else if ("percent".equals(type)) {
                        cCurr.setCellStyle(percentStyle);
                        cPrev.setCellStyle(percentStyle);
                    } else if ("double".equals(type)) {
                        cCurr.setCellStyle(doubleStyle);
                        cPrev.setCellStyle(doubleStyle);
                    } else {
                        cCurr.setCellStyle(dataStyle);
                        cPrev.setCellStyle(dataStyle);
                    }

                    double diff = prev != 0.0 ? (curr - prev) / prev : (curr > 0.0 ? 1.0 : 0.0);
                    Cell cDiff = r.createCell(3);
                    cDiff.setCellValue(diff);
                    cDiff.setCellStyle(percentStyle);

                    Cell cTrend = r.createCell(4);
                    if (diff > 0.001) {
                        cTrend.setCellValue("↑ Tăng");
                        cTrend.setCellStyle(trendUpStyle);
                    } else if (diff < -0.001) {
                        cTrend.setCellValue("↓ Giảm");
                        cTrend.setCellStyle(trendDownStyle);
                    } else {
                        cTrend.setCellValue("— Không đổi");
                        cTrend.setCellStyle(dataCenterStyle);
                    }
                }
                rIdx++;

                Row rSectB = sheet.createRow(rIdx++);
                rSectB.createCell(0).setCellValue("B. PHÂN PHÚC KHÁCH HÀNG & HÀNH VI");
                rSectB.getCell(0).setCellStyle(sectionStyle);

                Row hRowB = sheet.createRow(rIdx++);
                String[] colsB = { "Chỉ số phân khúc", "Kỳ này", "Kỳ trước", "Thay đổi (%)" };
                for (int i = 0; i < colsB.length; i++) {
                    Cell c = hRowB.createCell(i);
                    c.setCellValue(colsB[i]);
                    c.setCellStyle(headerStyle);
                }

                Map<String, Object> segCurrent = thongKeDAO.getKhachHangSegments(currentKy);
                Map<String, Object> segPrev = thongKeDAO.getKhachHangSegmentsPrev(currentKy);

                Object[][] segData = {
                        { "Tổng số khách lưu trú", (double) (int) segCurrent.getOrDefault("totalGuests", 0),
                                (double) (int) segPrev.getOrDefault("totalGuests", 0), "number" },
                        { "Khách hàng VIP (Bạc trở lên)", (double) (int) segCurrent.getOrDefault("vipGuests", 0),
                                (double) (int) segPrev.getOrDefault("vipGuests", 0), "number" },
                        { "Khách hàng quốc tế", (double) (int) segCurrent.getOrDefault("internationalGuests", 0),
                                (double) (int) segPrev.getOrDefault("internationalGuests", 0), "number" },
                        { "Booking qua doanh nghiệp (Corporate)",
                                (double) (int) segCurrent.getOrDefault("corporateBookings", 0),
                                (double) (int) segPrev.getOrDefault("corporateBookings", 0), "number" },
                        { "Booking trực tiếp (Direct/Web)", (double) (int) segCurrent.getOrDefault("directBookings", 0),
                                (double) (int) segPrev.getOrDefault("directBookings", 0), "number" },
                        { "Tỷ lệ đính kèm dịch vụ bán thêm",
                                (double) segCurrent.getOrDefault("serviceAttachmentRate", 0.0) / 100.0,
                                (double) segPrev.getOrDefault("serviceAttachmentRate", 0.0) / 100.0, "percent" }
                };

                for (Object[] seg : segData) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue((String) seg[0]);
                    r.getCell(0).setCellStyle(dataStyle);

                    double curr = ((Number) seg[1]).doubleValue();
                    double prev = ((Number) seg[2]).doubleValue();
                    String type = (String) seg[3];

                    Cell cCurr = r.createCell(1);
                    Cell cPrev = r.createCell(2);
                    cCurr.setCellValue(curr);
                    cPrev.setCellValue(prev);

                    if ("percent".equals(type)) {
                        cCurr.setCellStyle(percentStyle);
                        cPrev.setCellStyle(percentStyle);
                    } else {
                        cCurr.setCellStyle(dataStyle);
                        cPrev.setCellStyle(dataStyle);
                    }

                    double diff = prev != 0.0 ? (curr - prev) / prev : (curr > 0.0 ? 1.0 : 0.0);
                    Cell cDiff = r.createCell(3);
                    cDiff.setCellValue(diff);
                    cDiff.setCellStyle(percentStyle);
                }
                rIdx++;

                Row rSectC = sheet.createRow(rIdx++);
                rSectC.createCell(0).setCellValue("C. GỢI Ý VẬN HÀNH & KHUYẾN NGHỊ");
                rSectC.getCell(0).setCellStyle(sectionStyle);

                double occupancyRate = (double) stats.getOrDefault("occupancyRate", 0.0);
                double repeatRate = (double) stats.getOrDefault("repeatRate", 0.0);
                double serviceAttachmentRate = (double) segCurrent.getOrDefault("serviceAttachmentRate", 0.0);
                double netProfitMargin = (double) stats.getOrDefault("netProfitMargin", 0.0);

                java.util.List<String> recs = new ArrayList<>();
                if (occupancyRate < 50.0) {
                    recs.add(String.format(
                            "- Tỷ lệ công suất phòng đang ở mức thấp (%.1f%%). Khuyến nghị: Thiết lập các chương trình khuyến mãi giờ vàng, flash sale trên OTA hoặc giảm giá ngày thường để kích cầu.",
                            occupancyRate));
                }
                if (repeatRate < 15.0) {
                    recs.add(String.format(
                            "- Tỷ lệ khách quay lại ở mức hạn chế (%.1f%%). Khuyến nghị: Triển khai thẻ thành viên loyalty, khảo sát ý kiến khách sau trả phòng để cải thiện dịch vụ, gửi voucher giảm giá qua email/zalo cho lần lưu trú tiếp theo.",
                            repeatRate));
                }
                if (serviceAttachmentRate < 20.0) {
                    recs.add(String.format(
                            "- Tỷ lệ khách sử dụng dịch vụ bán thêm thấp (%.1f%%). Khuyến nghị: Đào tạo nhân viên lễ tân chào mời dịch vụ (combo phòng kèm ăn sáng, spa) tại thời điểm check-in; tăng cường tiếp thị dịch vụ tại phòng thông qua mã QR.",
                            serviceAttachmentRate));
                }
                if (netProfitMargin < 20.0) {
                    recs.add(String.format(
                            "- Tỷ suất lợi nhuận ròng thấp (%.1f%%). Khuyến nghị: Rà soát lại các khoản chi phí vận hành cố định, tối ưu hóa ca trực của nhân viên, kiểm tra hao phí điện nước tại các phòng trống.",
                            netProfitMargin));
                }
                if (recs.isEmpty()) {
                    recs.add(
                            "- Các chỉ số vận hành đang ở mức rất tốt! Khuyến nghị: Tiếp tục duy trì chất lượng dịch vụ cao, tập trung chăm sóc khách hàng VIP để tối ưu hóa RevPAR.");
                }

                for (String rec : recs) {
                    Row r = sheet.createRow(rIdx++);
                    Cell cRec = r.createCell(0);
                    cRec.setCellValue(rec);
                    cRec.setCellStyle(dataStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 4));
                }

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Sheet 2: HIỆU SUẤT PHÒNG
            if (chkRoom.isSelected()) {
                Sheet sheet = workbook.createSheet("2. Chi Tiết Phòng");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("CHI TIẾT HIỆU SUẤT KINH DOANH PHÒNG");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ thống kê: " + getTenKy() + " | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "STT", "Số phòng", "Loại phòng", "Số đêm đã thuê", "Doanh thu phòng",
                        "Giá trung bình (ADR)", "RevPAR ngày" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                java.util.List<Map<String, Object>> list = service.getChiTietPhongStats(currentKy);
                int stt = 1;
                int startRow = rIdx + 1;
                for (Map<String, Object> m : list) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataCenterStyle);

                    r.createCell(1).setCellValue((String) m.getOrDefault("soPhong", ""));
                    r.getCell(1).setCellStyle(dataCenterStyle);

                    r.createCell(2).setCellValue((String) m.getOrDefault("tenLoai", ""));
                    r.getCell(2).setCellStyle(dataStyle);

                    r.createCell(3).setCellValue((int) m.getOrDefault("occupiedNights", 0));
                    r.getCell(3).setCellStyle(dataCenterStyle);

                    Cell cRev = r.createCell(4);
                    cRev.setCellValue(((Number) m.getOrDefault("revenue", 0.0)).doubleValue());
                    cRev.setCellStyle(currencyStyle);

                    Cell cAdr = r.createCell(5);
                    cAdr.setCellValue(((Number) m.getOrDefault("adr", 0.0)).doubleValue());
                    cAdr.setCellStyle(currencyStyle);

                    Cell cRevpar = r.createCell(6);
                    cRevpar.setCellValue(((Number) m.getOrDefault("revpar", 0.0)).doubleValue());
                    cRevpar.setCellStyle(currencyStyle);
                }
                int endRow = rIdx;

                Row rTotal = sheet.createRow(rIdx++);
                Cell cTotalLbl = rTotal.createCell(0);
                cTotalLbl.setCellValue("Tổng cộng / Trung bình");
                cTotalLbl.setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));

                Cell cTotalNights = rTotal.createCell(3);
                cTotalNights.setCellFormula("SUM(D" + startRow + ":D" + endRow + ")");
                cTotalNights.setCellStyle(totalStyle);

                Cell cTotalRev = rTotal.createCell(4);
                cTotalRev.setCellFormula("SUM(E" + startRow + ":E" + endRow + ")");
                cTotalRev.setCellStyle(totalCurrencyStyle);

                Cell cTotalAdr = rTotal.createCell(5);
                cTotalAdr.setCellFormula("AVERAGE(F" + startRow + ":F" + endRow + ")");
                cTotalAdr.setCellStyle(totalCurrencyStyle);

                Cell cTotalRevpar = rTotal.createCell(6);
                cTotalRevpar.setCellFormula("AVERAGE(G" + startRow + ":G" + endRow + ")");
                cTotalRevpar.setCellStyle(totalCurrencyStyle);

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Sheet 3: CHI TIẾT DỊCH VỤ
            if (chkService.isSelected()) {
                Sheet sheet = workbook.createSheet("3. Chi Tiết Dịch Vụ");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("THỐNG KÊ CHI TIẾT SỬ DỤNG DỊCH VỤ");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ thống kê: " + getTenKy() + " | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "STT", "Tên dịch vụ", "Đơn giá (cao nhất)", "Số lượng sử dụng",
                        "Tổng doanh thu dịch vụ" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                java.util.List<Map<String, Object>> list = service.getChiTietDichVuStats(currentKy);
                int stt = 1;
                int startRow = rIdx + 1;
                for (Map<String, Object> m : list) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataCenterStyle);

                    r.createCell(1).setCellValue((String) m.getOrDefault("tenDV", ""));
                    r.getCell(1).setCellStyle(dataStyle);

                    Cell cPrice = r.createCell(2);
                    cPrice.setCellValue(((Number) m.getOrDefault("unitPrice", 0.0)).doubleValue());
                    cPrice.setCellStyle(currencyStyle);

                    r.createCell(3).setCellValue((int) m.getOrDefault("quantity", 0));
                    r.getCell(3).setCellStyle(dataCenterStyle);

                    Cell cRev = r.createCell(4);
                    cRev.setCellValue(((Number) m.getOrDefault("revenue", 0.0)).doubleValue());
                    cRev.setCellStyle(currencyStyle);
                }
                int endRow = rIdx;

                Row rTotal = sheet.createRow(rIdx++);
                Cell cTotalLbl = rTotal.createCell(0);
                cTotalLbl.setCellValue("Tổng cộng");
                cTotalLbl.setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));

                Cell cTotalQty = rTotal.createCell(3);
                cTotalQty.setCellFormula("SUM(D" + startRow + ":D" + endRow + ")");
                cTotalQty.setCellStyle(totalStyle);

                Cell cTotalRev = rTotal.createCell(4);
                cTotalRev.setCellFormula("SUM(E" + startRow + ":E" + endRow + ")");
                cTotalRev.setCellStyle(totalCurrencyStyle);
                rIdx++;

                Map<String, Object> segCurrent = thongKeDAO.getKhachHangSegments(currentKy);
                Row rRate = sheet.createRow(rIdx++);
                rRate.createCell(0).setCellValue("Tỷ lệ đính kèm dịch vụ bán thêm:");
                rRate.getCell(0).setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));

                Cell cRateVal = rRate.createCell(3);
                cRateVal.setCellValue(
                        ((Number) segCurrent.getOrDefault("serviceAttachmentRate", 0.0)).doubleValue() / 100.0);
                cRateVal.setCellStyle(totalPercentStyle);

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Sheet 4: HIỆU QUẢ KÊNH
            if (chkChannel.isSelected()) {
                Sheet sheet = workbook.createSheet("4. Phân Tích Kênh");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("HIỆU QUẢ KINH DOANH THEO KÊNH PHÂN PHỐI");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ thống kê: " + getTenKy() + " | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "STT", "Kênh đặt phòng", "Số lượng đặt phòng", "Doanh thu phòng (Gross)",
                        "Tỷ lệ hoa hồng", "Tiền hoa hồng", "Doanh thu thực nhận (Net)" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                java.util.List<Map<String, Object>> list = service.getPhanTichKenhStats(currentKy);
                int stt = 1;
                int startRow = rIdx + 1;
                for (Map<String, Object> m : list) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataCenterStyle);

                    r.createCell(1).setCellValue((String) m.getOrDefault("tenKenh", ""));
                    r.getCell(1).setCellStyle(dataStyle);

                    r.createCell(2).setCellValue((int) m.getOrDefault("bookingsCount", 0));
                    r.getCell(2).setCellStyle(dataCenterStyle);

                    Cell cGross = r.createCell(3);
                    cGross.setCellValue(((Number) m.getOrDefault("roomRevenue", 0.0)).doubleValue());
                    cGross.setCellStyle(currencyStyle);

                    String code = (String) m.getOrDefault("maKenh", "DIRECT");
                    double commRate = 0.0;
                    switch (code.toUpperCase()) {
                        case "BOOKING":
                        case "AGODA":
                        case "TRAVELOKA":
                        case "EXPEDIA":
                            commRate = 0.15;
                            break;
                        case "CORPORATE":
                            commRate = 0.05;
                            break;
                        case "TRAVEL_AGT":
                            commRate = 0.10;
                            break;
                    }
                    Cell cRate = r.createCell(4);
                    cRate.setCellValue(commRate);
                    cRate.setCellStyle(percentStyle);

                    Cell cComm = r.createCell(5);
                    cComm.setCellFormula("D" + rIdx + "*E" + rIdx);
                    cComm.setCellStyle(currencyStyle);

                    Cell cNet = r.createCell(6);
                    cNet.setCellFormula("D" + rIdx + "-F" + rIdx);
                    cNet.setCellStyle(currencyStyle);
                }
                int endRow = rIdx;

                Row rTotal = sheet.createRow(rIdx++);
                Cell cTotalLbl = rTotal.createCell(0);
                cTotalLbl.setCellValue("Tổng cộng");
                cTotalLbl.setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 1));

                Cell cTotalBookings = rTotal.createCell(2);
                cTotalBookings.setCellFormula("SUM(C" + startRow + ":C" + endRow + ")");
                cTotalBookings.setCellStyle(totalStyle);

                Cell cTotalGross = rTotal.createCell(3);
                cTotalGross.setCellFormula("SUM(D" + startRow + ":D" + endRow + ")");
                cTotalGross.setCellStyle(totalCurrencyStyle);

                rTotal.createCell(4).setCellValue("");
                rTotal.getCell(4).setCellStyle(totalStyle);

                Cell cTotalComm = rTotal.createCell(5);
                cTotalComm.setCellFormula("SUM(F" + startRow + ":F" + endRow + ")");
                cTotalComm.setCellStyle(totalCurrencyStyle);

                Cell cTotalNet = rTotal.createCell(6);
                cTotalNet.setCellFormula("SUM(G" + startRow + ":G" + endRow + ")");
                cTotalNet.setCellStyle(totalCurrencyStyle);

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Sheet 5: PHÂN TÍCH KHÁCH HÀNG
            if (chkCustomer.isSelected()) {
                Sheet sheet = workbook.createSheet("5. Phân Tích Khách Hàng");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("TOP 50 KHÁCH HÀNG CHI TIÊU NHIỀU NHẤT");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ thống kê: " + getTenKy() + " | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "STT", "Họ và tên", "Số điện thoại", "CCCD / Passport", "Quốc tịch", "Cấp VIP",
                        "Số lượt lưu trú", "Tổng chi tiêu" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                java.util.List<Map<String, Object>> list = service.getPhanTichKhachStats(currentKy);
                int stt = 1;
                int startRow = rIdx + 1;
                for (Map<String, Object> m : list) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataCenterStyle);

                    r.createCell(1).setCellValue((String) m.getOrDefault("hoTen", ""));
                    r.getCell(1).setCellStyle(dataStyle);

                    r.createCell(2).setCellValue((String) m.getOrDefault("sdt", ""));
                    r.getCell(2).setCellStyle(dataCenterStyle);

                    r.createCell(3).setCellValue((String) m.getOrDefault("cccd", ""));
                    r.getCell(3).setCellStyle(dataCenterStyle);

                    r.createCell(4).setCellValue((String) m.getOrDefault("quocTich", ""));
                    r.getCell(4).setCellStyle(dataStyle);

                    r.createCell(5).setCellValue((String) m.getOrDefault("vipLevel", ""));
                    r.getCell(5).setCellStyle(dataCenterStyle);

                    r.createCell(6).setCellValue((int) m.getOrDefault("visits", 0));
                    r.getCell(6).setCellStyle(dataCenterStyle);

                    Cell cSpent = r.createCell(7);
                    cSpent.setCellValue(((Number) m.getOrDefault("totalSpent", 0.0)).doubleValue());
                    cSpent.setCellStyle(currencyStyle);
                }
                int endRow = rIdx;

                Row rTotal = sheet.createRow(rIdx++);
                Cell cTotalLbl = rTotal.createCell(0);
                cTotalLbl.setCellValue("Tổng cộng");
                cTotalLbl.setCellStyle(totalStyle);
                sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 5));

                Cell cTotalVisits = rTotal.createCell(6);
                cTotalVisits.setCellFormula("SUM(G" + startRow + ":G" + endRow + ")");
                cTotalVisits.setCellStyle(totalStyle);

                Cell cTotalSpent = rTotal.createCell(7);
                cTotalSpent.setCellFormula("SUM(H" + startRow + ":H" + endRow + ")");
                cTotalSpent.setCellStyle(totalCurrencyStyle);

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Sheet 6: SO SÁNH YoY
            if (chkYoY.isSelected()) {
                Sheet sheet = workbook.createSheet("6. So Sánh YoY");
                sheet.setDisplayGridlines(true);

                int rIdx = 0;
                Row rCompany = sheet.createRow(rIdx++);
                rCompany.createCell(0).setCellValue("HOTEL OHNO - HỆ THỐNG QUẢN LÝ KHÁCH SẠN");
                rCompany.getCell(0).setCellStyle(sectionStyle);

                Row rTitle = sheet.createRow(rIdx++);
                Cell cTitle = rTitle.createCell(0);
                cTitle.setCellValue("SO SÁNH HOẠT ĐỘNG CÙNG KỲ NĂM TRƯỚC (YoY)");
                cTitle.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

                Row rSub = sheet.createRow(rIdx++);
                rSub.createCell(0).setCellValue("Kỳ báo cáo: " + getTenKy() + " vs Cùng kỳ năm ngoái | Ngày xuất: "
                        + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
                rSub.getCell(0).setCellStyle(subtitleStyle);
                rIdx++;

                Row hRow = sheet.createRow(rIdx++);
                String[] cols = { "Chỉ số KPI", "Kỳ này", "Cùng kỳ năm trước", "Thay đổi tuyệt đối", "Thay đổi (%)" };
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hRow.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(headerStyle);
                }

                Map<String, Object> yoyStats = service.getThongKeKy("yoy:" + currentKy);

                Object[][] yoyData = {
                        { "Tổng doanh thu (Total Revenue)", (double) (long) stats.getOrDefault("doanhThu", 0L),
                                (double) (long) yoyStats.getOrDefault("doanhThu", 0L), "currency" },
                        { "Doanh thu phòng (Room Revenue)", (double) (long) stats.getOrDefault("doanhThuPhong", 0L),
                                (double) (long) yoyStats.getOrDefault("doanhThuPhong", 0L), "currency" },
                        { "Doanh thu dịch vụ (Service Revenue)", (double) (long) stats.getOrDefault("doanhThuDV", 0L),
                                (double) (long) yoyStats.getOrDefault("doanhThuDV", 0L), "currency" },
                        { "Doanh thu trên phòng có sẵn (RevPAR)", (double) stats.getOrDefault("revpar", 0.0),
                                (double) yoyStats.getOrDefault("revpar", 0.0), "currency" },
                        { "Giá phòng trung bình ngày (ADR)", (double) stats.getOrDefault("adr", 0.0),
                                (double) yoyStats.getOrDefault("adr", 0.0), "currency" },
                        { "Công suất phòng trung bình", (double) stats.getOrDefault("occupancyRate", 0.0) / 100.0,
                                (double) yoyStats.getOrDefault("occupancyRate", 0.0) / 100.0, "percent" },
                        { "Độ dài lưu trú trung bình (ALOS)", (double) stats.getOrDefault("alos", 0.0),
                                (double) yoyStats.getOrDefault("alos", 0.0), "double" },
                        { "Tổng số lượt nhận phòng", (double) (int) stats.getOrDefault("luotDatPhong", 0),
                                (double) (int) yoyStats.getOrDefault("luotDatPhong", 0), "number" },
                        { "Tỷ lệ khách hàng quay lại", (double) stats.getOrDefault("repeatRate", 0.0) / 100.0,
                                (double) yoyStats.getOrDefault("repeatRate", 0.0) / 100.0, "percent" },
                        { "Chi phí hoạt động", (double) (long) stats.getOrDefault("operatingCost", 0L),
                                (double) (long) yoyStats.getOrDefault("operatingCost", 0L), "currency" },
                        { "Lợi nhuận ròng", (double) (long) stats.getOrDefault("netProfit", 0L),
                                (double) (long) yoyStats.getOrDefault("netProfit", 0L), "currency" }
                };

                for (Object[] item : yoyData) {
                    Row r = sheet.createRow(rIdx++);
                    r.createCell(0).setCellValue((String) item[0]);
                    r.getCell(0).setCellStyle(dataStyle);

                    double curr = ((Number) item[1]).doubleValue();
                    double prev = ((Number) item[2]).doubleValue();
                    String type = (String) item[3];

                    Cell cCurr = r.createCell(1);
                    Cell cPrev = r.createCell(2);
                    cCurr.setCellValue(curr);
                    cPrev.setCellValue(prev);

                    if ("currency".equals(type)) {
                        cCurr.setCellStyle(currencyStyle);
                        cPrev.setCellStyle(currencyStyle);
                    } else if ("percent".equals(type)) {
                        cCurr.setCellStyle(percentStyle);
                        cPrev.setCellStyle(percentStyle);
                    } else if ("double".equals(type)) {
                        cCurr.setCellStyle(doubleStyle);
                        cPrev.setCellStyle(doubleStyle);
                    } else {
                        cCurr.setCellStyle(dataStyle);
                        cPrev.setCellStyle(dataStyle);
                    }

                    Cell cDiffAbs = r.createCell(3);
                    cDiffAbs.setCellFormula("B" + rIdx + "-C" + rIdx);
                    if ("currency".equals(type)) {
                        cDiffAbs.setCellStyle(currencyStyle);
                    } else if ("percent".equals(type)) {
                        cDiffAbs.setCellStyle(percentStyle);
                    } else if ("double".equals(type)) {
                        cDiffAbs.setCellStyle(doubleStyle);
                    } else {
                        cDiffAbs.setCellStyle(dataStyle);
                    }

                    Cell cDiffPct = r.createCell(4);
                    cDiffPct.setCellFormula("IF(C" + rIdx + "=0, IF(B" + rIdx + ">0, 1, 0), (B" + rIdx + "-C" + rIdx
                            + ")/C" + rIdx + ")");
                    cDiffPct.setCellStyle(percentStyle);
                }

                for (int i = 0; i < cols.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            NotificationManager.showSuccess("Thành công", "✓ Xuất báo cáo Excel thành công!");

        } catch (Exception ex) {
            NotificationManager.showError("Lỗi", "Lỗi xuất file Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String getTenKy() {
        if (currentKy != null && currentKy.startsWith("custom:")) {
            try {
                String[] parts = currentKy.split(":");
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd/MM/yyyy");
                return out.format(in.parse(parts[1])) + " - " + out.format(in.parse(parts[2]));
            } catch (Exception ex) {
            }
            return "Tùy chỉnh";
        }
        switch (currentKy != null ? currentKy : "thang") {
            case "7ngay":
                return "7 ngày";
            case "quy":
                return "Quý này";
            case "nam":
                return "Năm nay";
            default:
                return "Tháng này";
        }
    }

    private void showCustomDateDialog(JToggleButton btnCustom, JPanel right) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Tùy chỉnh thời gian",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel pnl = new JPanel(new GridLayout(2, 2, 10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        pnl.setBackground(java.awt.Color.WHITE);

        JLabel lbTu = new JLabel("Từ ngày:");
        lbTu.setFont(UIConstants.FONT_BODY);
        ui.components.DatePicker dpTu = new ui.components.DatePicker(new java.util.Date());
        dpTu.setPreferredSize(new Dimension(140, 38));

        JLabel lbDen = new JLabel("Đến ngày:");
        lbDen.setFont(UIConstants.FONT_BODY);
        ui.components.DatePicker dpDen = new ui.components.DatePicker(new java.util.Date());
        dpDen.setPreferredSize(new Dimension(140, 38));

        pnl.add(lbTu);
        pnl.add(dpTu);
        pnl.add(lbDen);
        pnl.add(dpDen);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(java.awt.Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));

        RoundedButton btnOk = new RoundedButton("Xác nhận", UIConstants.PRIMARY, java.awt.Color.WHITE);
        btnOk.setFont(UIConstants.FONT_BODY_BOLD);
        btnOk.setPreferredSize(new Dimension(100, 38));
        btnOk.addActionListener(ev -> {
            java.util.Date d1 = dpTu.getDate();
            java.util.Date d2 = dpDen.getDate();
            if (d1.after(d2)) {
                JOptionPane.showMessageDialog(dialog, "Ngày bắt đầu không được lớn hơn ngày kết thúc!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            currentKy = "custom:" + sdf.format(d1) + ":" + sdf.format(d2);
            dialog.dispose();

            for (Component c : right.getComponents()) {
                if (c instanceof JToggleButton) {
                    JToggleButton tb = (JToggleButton) c;
                    styleKyBtn(tb, tb == btnCustom);
                }
            }
            refresh();
        });
        btnPanel.add(btnOk);

        dialog.add(pnl, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setSize(new Dimension(Math.max(dialog.getWidth(), 350), Math.max(dialog.getHeight(), 180)));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private class ModernBarChart extends JPanel {
        private final java.util.List<long[]> data;
        private double animationProgress = 0;
        private int hoverIndex = -1;
        private Point mousePos = null;

        public ModernBarChart(java.util.List<long[]> data) {
            this.data = data;
            setOpaque(false);

            javax.swing.Timer timer = new javax.swing.Timer(15, null);
            timer.addActionListener(e -> {
                animationProgress += 0.05;
                if (animationProgress >= 1) {
                    animationProgress = 1;
                    timer.stop();
                }
                repaint();
            });
            timer.start();

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    mousePos = e.getPoint();
                    int w = getWidth();
                    int padL = 36, padR = 10;
                    int chartW = w - padL - padR;
                    if (data.isEmpty())
                        return;

                    int index = (e.getX() - padL) / (chartW / data.size());
                    if (index >= 0 && index < data.size()) {
                        if (hoverIndex != index) {
                            hoverIndex = index;
                            repaint();
                        }
                    } else {
                        if (hoverIndex != -1) {
                            hoverIndex = -1;
                            repaint();
                        }
                    }
                }
            });

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 36, padB = 24, padT = 10, padR = 10;
            int chartW = w - padL - padR;
            int chartH = h - padB - padT;

            g2.setColor(new java.awt.Color(0xF1F5F9));
            long maxVal = data.stream().mapToLong(d -> d[1]).max().orElse(1L);
            long stepVal = (long) Math.ceil(maxVal / 4.0 / 1_000_000.0) * 1_000_000;
            if (stepVal == 0)
                stepVal = 10_000_000;

            for (int i = 0; i <= 4; i++) {
                int y = padT + chartH - (chartH * i / 4);
                g2.drawLine(padL, y, w - padR, y);
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_MUTED);
                g2.drawString(String.format("%.0f", (stepVal * i) / 1_000_000.0), 2, y + 4);
                g2.setColor(new java.awt.Color(0xF1F5F9));
            }

            if (data.isEmpty()) {
                g2.dispose();
                return;
            }

            int barW = Math.max(6, chartW / data.size() - 8);

            for (int i = 0; i < data.size(); i++) {
                long val = data.get(i)[1];
                int barH = (int) (chartH * (val / (double) (stepVal * 4)) * animationProgress);
                int x = padL + i * (chartW / data.size()) + 3;
                int y = padT + chartH - barH;

                // Enhanced gradient - more prominent on hover
                Color c1 = (i == hoverIndex) ? UIConstants.PRIMARY : new java.awt.Color(0x60A5FA);
                Color c2 = (i == hoverIndex) ? new java.awt.Color(0x1E3A8A) : new java.awt.Color(0x93C5FD);

                GradientPaint gp = new GradientPaint(x, y, c1, x, h - padB, c2);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 6, 6));

                // Subtle border on hover
                if (i == hoverIndex) {
                    g2.setColor(UIConstants.PRIMARY);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(x, y, barW, barH, 6, 6));
                }

                if (data.size() < 15 || (data.size() < 40 && i % 5 == 0) || (data.size() >= 40 && i % 10 == 0)
                        || i == data.size() - 1) {
                    g2.setColor(UIConstants.TEXT_MUTED);
                    long val0 = data.get(i)[0];
                    String lbl = String.valueOf(val0);

                    if ("nam".equals(currentKy) || "quy".equals(currentKy)) {
                        lbl = "T" + val0;
                    } else if (currentKy.startsWith("custom:") && data.get(i).length > 2) {
                        long raw = data.get(i)[2];
                        if (raw > 10000 && raw < 1000000) { // YYYYMM
                            lbl = (raw % 100) + "/" + String.valueOf(raw / 100).substring(2);
                        } else if (raw > 1000000) { // YYYYMMDD
                            lbl = (raw % 100) + "/" + ((raw / 100) % 100);
                        }
                    }
                    g2.drawString(lbl, x + barW / 2 - g2.getFontMetrics().stringWidth(lbl) / 2, h - 6);
                }
            }

            // Draw Trend Line (Spline/Line Chart) connecting the tops of the bars
            if (data.size() > 1) {
                g2.setColor(new Color(0xF59E0B)); // Orange/Warning color for line chart
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int[] px = new int[data.size()];
                int[] py = new int[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    long val = data.get(i)[1];
                    int barH = (int) (chartH * (val / (double) (stepVal * 4)) * animationProgress);
                    int x = padL + i * (chartW / data.size()) + 3;
                    int y = padT + chartH - barH;
                    px[i] = x + barW / 2;
                    py[i] = y;
                }

                // Draw path
                for (int i = 0; i < data.size() - 1; i++) {
                    g2.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
                }

                // Draw dots at vertices (only if value is greater than 0)
                for (int i = 0; i < data.size(); i++) {
                    long val = data.get(i)[1];
                    if (val > 0) {
                        // White fill
                        g2.setColor(Color.WHITE);
                        g2.fillOval(px[i] - 4, py[i] - 4, 8, 8);
                        // Orange outline
                        g2.setColor(new Color(0xF59E0B));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawOval(px[i] - 4, py[i] - 4, 8, 8);
                    }
                }
            }

            if (hoverIndex != -1 && mousePos != null) {
                long[] entry = data.get(hoverIndex);
                String timeText = "";
                if (currentKy.startsWith("custom:") && entry.length > 2) {
                    long raw = entry[2];
                    if (raw > 1000000) { // YYYYMMDD
                        String s = String.valueOf(raw);
                        timeText = s.substring(6, 8) + "/" + s.substring(4, 6) + "/" + s.substring(0, 4);
                    } else if (raw > 10000) { // YYYYMM
                        timeText = "Tháng " + (raw % 100) + "/" + (raw / 100);
                    }
                } else {
                    String prefix = ("nam".equals(currentKy) || "quy".equals(currentKy)) ? "Tháng " : "Ngày ";
                    timeText = prefix + entry[0];
                }

                String amount = String.format("%,.0f VNĐ", (double) entry[1]);
                String txt = timeText + ": " + amount;
                g2.setFont(UIConstants.FONT_SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(txt) + 16;
                int th = 28;
                int tx = Math.min(w - tw - 5, Math.max(5, mousePos.x - tw / 2));
                int ty = mousePos.y - th - 10;

                g2.setColor(new java.awt.Color(0x1E293B));
                g2.fillRoundRect(tx, ty, tw, th, 8, 8);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(txt, tx + 8, ty + 18);
            }

            g2.dispose();
        }
    }

    private class ModernDonutChart extends JPanel {
        private final double[] values;
        private final java.awt.Color[] colors;
        private final String[] labels;
        private final int total;
        private int hoverIndex = -1;
        private double animationProgress = 0;

        public ModernDonutChart(double[] values, java.awt.Color[] colors, String[] labels, int total) {
            this.values = values;
            this.colors = colors;
            this.labels = labels;
            this.total = total;
            setOpaque(false);

            javax.swing.Timer timer = new javax.swing.Timer(20, null);
            timer.addActionListener(e -> {
                animationProgress += 0.04;
                if (animationProgress >= 1) {
                    animationProgress = 1;
                    timer.stop();
                }
                repaint();
            });
            timer.start();

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int w = getWidth(), h = getHeight();
                    int cx = (int) (w * 0.40);
                    int cy = h / 2;
                    int maxR1 = 80;
                    int maxR2 = cx - 25;
                    int maxR3 = cy - 20;
                    int maxR4 = (int) (w * 0.55) - cx - 10;
                    int r = Math.min(maxR1, Math.min(maxR2, Math.min(maxR3, maxR4)));
                    if (r < 30)
                        r = 30;
                    int innerR = (int) (r * 0.65);
                    int lx = (int) (w * 0.58);

                    double dx = e.getX() - cx;
                    double dy = e.getY() - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    int newHover = -1;

                    // 1. Check hover on Donut chart
                    if (dist > innerR && dist < r + 5) {
                        double angle = Math.toDegrees(Math.atan2(-dy, dx));
                        if (angle < 0)
                            angle += 360;
                        double currentAngle = 90;
                        for (int i = 0; i < values.length; i++) {
                            double sweep = (360.0 * values[i] / total);
                            double endAngle = currentAngle - sweep;
                            if (isAngleBetween(angle, endAngle, currentAngle)) {
                                newHover = i;
                                break;
                            }
                            currentAngle -= sweep;
                        }
                    }
                    // 2. Check hover on Legend on the right
                    else {
                        int ly = cy - (labels.length * 25) / 2;
                        for (int i = 0; i < labels.length; i++) {
                            int itemY = ly + i * 25;
                            if (e.getX() >= lx && e.getX() <= w - 10 && e.getY() >= itemY && e.getY() <= itemY + 22) {
                                newHover = i;
                                break;
                            }
                        }
                    }

                    if (hoverIndex != newHover) {
                        hoverIndex = newHover;
                        repaint();
                    }
                }

                private boolean isAngleBetween(double a, double start, double end) {
                    a = (a % 360 + 360) % 360;
                    start = (start % 360 + 360) % 360;
                    end = (end % 360 + 360) % 360;
                    if (start <= end)
                        return a >= start && a <= end;
                    return a >= start || a <= end;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int cx = (int) (w * 0.40);
            int cy = h / 2;
            int maxR1 = 80;
            int maxR2 = cx - 25;
            int maxR3 = cy - 20;
            int maxR4 = (int) (w * 0.55) - cx - 10;
            int r = Math.min(maxR1, Math.min(maxR2, Math.min(maxR3, maxR4)));
            if (r < 30)
                r = 30;
            int innerR = (int) (r * 0.65);
            int lx = (int) (w * 0.58);

            double currentAngle = 90;
            double[] startAngles = new double[values.length];
            double[] sweepAngles = new double[values.length];

            // Draw filled arcs
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0)
                    continue;
                double sweep = (360.0 * values[i] / total) * animationProgress;
                int drawR = r;
                if (i == hoverIndex)
                    drawR += 5;

                g2.setColor(colors[i]);
                g2.fillArc(cx - drawR, cy - drawR, drawR * 2, drawR * 2, (int) currentAngle, (int) -sweep);

                startAngles[i] = currentAngle;
                sweepAngles[i] = sweep;

                currentAngle -= (360.0 * values[i] / total);
            }

            // Draw white separation borders (2.0f thick)
            g2.setColor(java.awt.Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0)
                    continue;
                int drawR = r;
                if (i == hoverIndex)
                    drawR += 5;

                g2.drawArc(cx - drawR, cy - drawR, drawR * 2, drawR * 2, (int) startAngles[i], (int) -sweepAngles[i]);

                double radStart = Math.toRadians(startAngles[i]);
                double radEnd = Math.toRadians(startAngles[i] - sweepAngles[i]);
                g2.drawLine(cx, cy, (int) (cx + drawR * Math.cos(radStart)), (int) (cy - drawR * Math.sin(radStart)));
                g2.drawLine(cx, cy, (int) (cx + drawR * Math.cos(radEnd)), (int) (cy - drawR * Math.sin(radEnd)));
            }

            // Center white cutout
            g2.setColor(java.awt.Color.WHITE);
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            // Subtle gray stroke inside the donut cutout
            g2.setColor(new Color(0xE2E8F0));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            if (hoverIndex != -1) {
                g2.setColor(colors[hoverIndex]);
                g2.setFont(UIConstants.FONT_SMALL_BOLD);
                String label = labels[hoverIndex];
                g2.drawString(label, cx - g2.getFontMetrics().stringWidth(label) / 2, cy - 5);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                String val = String.format("%.0f", values[hoverIndex]);
                g2.drawString(val, cx - g2.getFontMetrics().stringWidth(val) / 2, cy + 15);
            } else {
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                String ct = (int) values[0] + "/" + total;
                g2.drawString(ct, cx - g2.getFontMetrics().stringWidth(ct) / 2, cy + 5);
                g2.setFont(UIConstants.FONT_SMALL);
                g2.setColor(UIConstants.TEXT_MUTED);
                g2.drawString("phòng đang ở", cx - g2.getFontMetrics().stringWidth("phòng đang ở") / 2, cy + 20);
            }

            int ly = cy - (labels.length * 25) / 2;
            for (int i = 0; i < labels.length; i++) {
                // Highlight legend row on hover
                if (i == hoverIndex) {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(lx - 8, ly + i * 25, w - lx - 4, 22, 6, 6);
                }

                g2.setColor(colors[i]);
                g2.fillOval(lx, ly + i * 25 + 6, 10, 10);

                g2.setColor(i == hoverIndex ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_SECONDARY);
                g2.setFont(i == hoverIndex ? UIConstants.FONT_SMALL_BOLD : UIConstants.FONT_SMALL);
                g2.drawString(labels[i] + ": " + (int) values[i], lx + 18, ly + i * 25 + 15);
            }
            g2.dispose();
        }
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    revalidate();
                }
            });
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}

package ui.panels;

import service.*;
import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TongQuanPanel extends JPanel {

    private final MainFrame      mainFrame;
    private final ThongKeService thongKeService = new ThongKeService();

    public TongQuanPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JScrollPane scroll = new JScrollPane(buildInner());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildInner() {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        inner.add(buildTitleRow());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildStatsRow());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildMiddleRow());
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildBottomRow());

        return inner;
    }

    // ------------------------------------------------------------------ TITLE
    private JPanel buildTitleRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
        String dateStr = sdf.format(new Date());
        service.AuthService auth = service.AuthService.getInstance();
        String name = auth.getCurrentUser() != null ? auth.getCurrentUser().getHoTen() : "Quản trị viên";

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Chào buổi sáng" : hour < 18 ? "Chào buổi chiều" : "Chào buổi tối";

        JLabel lblGreet = new JLabel(greet + ", " + name + "!");
        lblGreet.setFont(UIConstants.FONT_TITLE);
        lblGreet.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblDate = new JLabel(dateStr + " | Admin");
        lblDate.setFont(UIConstants.FONT_BODY);
        lblDate.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(lblGreet);
        left.add(Box.createVerticalStrut(4));
        left.add(lblDate);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        RoundedButton btnCheckout = new RoundedButton("Trả phòng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnCheckin  = new RoundedButton("+ Nhận phòng", UIConstants.PRIMARY, Color.WHITE);
        btnCheckout.addActionListener(e -> mainFrame.navigateTo("thuephong"));
        btnCheckin .addActionListener(e -> mainFrame.navigateTo("thuephong"));
        btnPanel.add(btnCheckout);
        btnPanel.add(btnCheckin);

        row.add(left,     BorderLayout.WEST);
        row.add(btnPanel, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------ STATS ROW
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        Map<String, Object> stats = thongKeService.getDashboardStats();
        int  phongTrong = (int)  stats.getOrDefault("phongTrong",    0);
        int  dangO      = (int)  stats.getOrDefault("dangO",         0);
        int  daDat      = (int)  stats.getOrDefault("daDat",         0);
        int  checkin    = (int)  stats.getOrDefault("checkinHomNay",  0);
        int  checkout   = (int)  stats.getOrDefault("checkoutHomNay", 0);
        long doanhThu   = (long) stats.getOrDefault("doanhThuHomNay", 0L);

        row.add(createStatCard("Phòng trống",       phongTrong + " phòng",                          UIConstants.SUCCESS));
        row.add(createStatCard("Đang ở",            dangO      + " phòng",                          UIConstants.PRIMARY));
        row.add(createStatCard("Đã đặt trước",      daDat      + " phòng",                          UIConstants.WARNING));
        row.add(createStatCard("Check-in hôm nay",  checkin    + " lượt",                           UIConstants.SUCCESS));
        row.add(createStatCard("Check-out hôm nay", checkout   + " lượt",                           UIConstants.ORANGE));
        row.add(createStatCard("Doanh thu hôm nay", String.format("%,.1f tr đ", doanhThu / 1_000_000.0), UIConstants.DANGER));
        return row;
    }

    private RoundedPanel createStatCard(String label, String value, Color accent) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(UIConstants.FONT_SMALL);
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.fillRoundRect(0, 3, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 8));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(lblLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(lblValue);

        card.add(center, BorderLayout.CENTER);
        card.add(bar,    BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------ MIDDLE ROW
    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        // Bỏ setMaximumSize cứng — để card tự co giãn theo nội dung
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        row.add(buildRoomStatusCard());
        row.add(buildCheckinCard());
        return row;
    }

    private JPanel buildRoomStatusCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        Map<String, Object> stats = thongKeService.getDashboardStats();
        int phongTrong = (int) stats.getOrDefault("phongTrong", 0);
        int dangO      = (int) stats.getOrDefault("dangO",      0);
        int daDat      = (int) stats.getOrDefault("daDat",      0);
        int tongPhong  = Math.max(1, (int) stats.getOrDefault("tongPhong", 50));

        JLabel title = new JLabel("Tình trạng phòng");
        title.setFont(UIConstants.FONT_HEADER);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel total = new JLabel("Tổng " + tongPhong + " phòng");
        total.setFont(UIConstants.FONT_SMALL);
        total.setForeground(UIConstants.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(total, BorderLayout.EAST);

        final int pt = phongTrong, dO = dangO, dD = daDat, tp = tongPhong;
        JPanel barPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = 12;
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(0, 0, w, h, h, h);
                int w1 = (int)(w * pt / (double) tp);
                int w2 = (int)(w * dO / (double) tp);
                int w3 = (int)(w * dD / (double) tp);
                g2.setColor(UIConstants.SUCCESS); g2.fillRoundRect(0,     0, w1, h, h, h);
                g2.setColor(UIConstants.PRIMARY); g2.fillRect(w1,         0, w2, h);
                g2.setColor(UIConstants.WARNING); g2.fillRoundRect(w1+w2, 0, w3, h, 0, 0);
                g2.dispose();
            }
        };
        barPanel.setOpaque(false);
        barPanel.setPreferredSize(new Dimension(0, 18));

        JPanel legend = new JPanel(new GridLayout(3, 1, 0, 4));
        legend.setOpaque(false);
        legend.add(buildLegendItem("Phòng trống", UIConstants.SUCCESS, phongTrong + " phòng", (int)(100.0*phongTrong/tongPhong) + "%"));
        legend.add(buildLegendItem("Đang ở",      UIConstants.PRIMARY, dangO      + " phòng", (int)(100.0*dangO/tongPhong)      + "%"));
        legend.add(buildLegendItem("Đã đặt trước",UIConstants.WARNING, daDat      + " phòng", (int)(100.0*daDat/tongPhong)      + "%"));

        JButton btnSoDo = new JButton("Xem sơ đồ phòng →");
        btnSoDo.setFont(UIConstants.FONT_SMALL);
        btnSoDo.setForeground(UIConstants.TEXT_MUTED);
        btnSoDo.setBorderPainted(false);
        btnSoDo.setContentAreaFilled(false);
        btnSoDo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSoDo.addActionListener(e -> mainFrame.navigateTo("thuephong"));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(Box.createVerticalStrut(12));
        center.add(barPanel);
        center.add(Box.createVerticalStrut(12));
        center.add(legend);
        center.add(Box.createVerticalStrut(8));
        center.add(btnSoDo);

        card.add(header, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLegendItem(String label, Color color, String count, String percent) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                g2.fillOval(0, 2, 10, 10);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 14));
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(dot); left.add(lbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel cnt = new JLabel(count); cnt.setFont(UIConstants.FONT_SMALL_BOLD); cnt.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel pct = new JLabel(percent); pct.setFont(UIConstants.FONT_SMALL_BOLD); pct.setForeground(color);
        right.add(cnt); right.add(pct);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ---- Check-in card (FIX: scroll đặt CENTER thay vì SOUTH) ----
    private JPanel buildCheckinCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        java.util.List<Map<String, Object>> checkins = thongKeService.getCheckinHomNay();

        JLabel title = new JLabel("Check-in hôm nay");
        title.setFont(UIConstants.FONT_HEADER);
        JLabel count = new JLabel(checkins.size() + " lượt");
        count.setFont(UIConstants.FONT_SMALL);
        count.setForeground(UIConstants.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
        if (checkins.isEmpty()) {
            JLabel empty = new JLabel("Chưa có check-in hôm nay");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(20));
            listPanel.add(empty);
        } else {
            for (Map<String, Object> ci : checkins) {
                listPanel.add(buildCheckinRow(
                    (String) ci.get("hoTen"),
                    ci.get("soPhong") + " – " + ci.get("tenLoai"),
                    ci.get("ngayNhan") != null ? timeFmt.format(ci.get("ngayNhan")) : "--:--",
                    "Chờ xác nhận"
                ));
            }
        }

        // FIX: scroll đặt CENTER để không bị che
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(header,                    BorderLayout.NORTH);
        card.add(Box.createVerticalStrut(8), BorderLayout.CENTER);
        card.add(scroll,                    BorderLayout.CENTER); // FIX
        return card;
    }

    private JPanel buildCheckinRow(String name, String room, String time, String status) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?";
        Color[] colors = {UIConstants.SUCCESS, UIConstants.PRIMARY, UIConstants.WARNING, UIConstants.DANGER, UIConstants.INFO};
        Color avatarColor = colors[Math.abs((name != null ? name : "?").hashCode()) % colors.length];

        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(avatarColor);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(UIConstants.FONT_BODY_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial, (32-fm.stringWidth(initial))/2, (32+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(32, 32));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel lblName = new JLabel(name != null ? name : ""); lblName.setFont(UIConstants.FONT_BODY_BOLD);
        JLabel lblRoom = new JLabel(room);                      lblRoom.setFont(UIConstants.FONT_SMALL); lblRoom.setForeground(UIConstants.TEXT_MUTED);
        info.add(lblName); info.add(lblRoom);

        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        JLabel lblTime   = new JLabel(time,   SwingConstants.RIGHT); lblTime.setFont(UIConstants.FONT_SMALL);       lblTime.setForeground(UIConstants.TEXT_MUTED);
        JLabel lblStatus = new JLabel(status, SwingConstants.RIGHT); lblStatus.setFont(UIConstants.FONT_SMALL_BOLD); lblStatus.setForeground(UIConstants.WARNING);
        right.add(lblTime); right.add(lblStatus);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(av); left.add(info);

        row.add(left,  BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ------------------------------------------------------------------ BOTTOM ROW
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280)); // tăng chiều cao
        row.add(buildActivityCard());
        row.add(buildCheckoutCard());
        row.add(buildAlertCard());
        return row;
    }

    // ---- Hoạt động gần đây (FIX: thêm scroll) ----
    private JPanel buildActivityCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Hoạt động gần đây");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);
        card.add(Box.createVerticalStrut(10), BorderLayout.CENTER);

        java.util.List<Map<String, Object>> acts = thongKeService.getHoatDongGanDay(10);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (acts.isEmpty()) {
            JLabel empty = new JLabel("Chưa có hoạt động");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm dd/MM");
            for (Map<String, Object> act : acts) {
                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                JPanel dot = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        String loai = String.valueOf(act.get("loai"));
                        g2.setColor("Nhận phòng".equals(loai) ? UIConstants.SUCCESS : UIConstants.ORANGE);
                        g2.fillOval(0, 4, 8, 8);
                        g2.dispose();
                    }
                };
                dot.setOpaque(false);
                dot.setPreferredSize(new Dimension(14, 16));

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                JLabel lblAct  = new JLabel(act.get("loai") + " – P." + act.get("soPhong"));
                lblAct.setFont(UIConstants.FONT_SMALL_BOLD);
                JLabel lblName = new JLabel(act.get("hoTen") != null ? String.valueOf(act.get("hoTen")) : "");
                lblName.setFont(UIConstants.FONT_SMALL);
                lblName.setForeground(UIConstants.TEXT_MUTED);
                info.add(lblAct); info.add(lblName);

                String thoiGianStr = "--:--";
                if (act.get("thoiGian") instanceof java.util.Date) {
                    thoiGianStr = timeFmt.format((java.util.Date) act.get("thoiGian"));
                }
                JLabel time = new JLabel(thoiGianStr);
                time.setFont(UIConstants.FONT_SMALL);
                time.setForeground(UIConstants.TEXT_MUTED);

                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                left.setOpaque(false);
                left.add(dot); left.add(info);
                item.add(left, BorderLayout.WEST);
                item.add(time, BorderLayout.EAST);
                list.add(item);
            }
        }

        // FIX: thêm scroll
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ---- Check-out hôm nay (FIX: thêm scroll) ----
    private JPanel buildCheckoutCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        java.util.List<Map<String, Object>> outs = thongKeService.getCheckoutHomNay();

        JLabel title = new JLabel("Check-out hôm nay");
        title.setFont(UIConstants.FONT_HEADER);
        JLabel cnt = new JLabel(outs.size() + " lượt");
        cnt.setFont(UIConstants.FONT_SMALL);
        cnt.setForeground(UIConstants.TEXT_MUTED);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(cnt,   BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");

        if (outs.isEmpty()) {
            JLabel empty = new JLabel("Chưa có check-out hôm nay");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            for (Map<String, Object> co : outs) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

                JLabel room = new JLabel("P." + co.get("soPhong"));
                room.setFont(UIConstants.FONT_BODY_BOLD);
                room.setForeground(UIConstants.PRIMARY);
                room.setPreferredSize(new Dimension(50, 20));

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                JLabel name = new JLabel(String.valueOf(co.get("hoTen")));
                name.setFont(UIConstants.FONT_SMALL_BOLD);
                String timeStr = co.get("ngayTraDK") != null ? timeFmt.format(co.get("ngayTraDK")) : "";
                JLabel t = new JLabel(timeStr);
                t.setFont(UIConstants.FONT_SMALL);
                t.setForeground(UIConstants.TEXT_MUTED);
                info.add(name); info.add(t);

                String trangThai = String.valueOf(co.get("trangThai"));
                boolean daTraP = "Đã trả".equals(trangThai);
                JLabel badge = new JLabel(daTraP ? "Đã trả" : "Chờ trả");
                badge.setFont(UIConstants.FONT_SMALL_BOLD);
                badge.setForeground(daTraP ? UIConstants.SUCCESS : UIConstants.WARNING);

                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                left.setOpaque(false);
                left.add(room); left.add(info);
                row.add(left,  BorderLayout.WEST);
                row.add(badge, BorderLayout.EAST);
                list.add(row);
            }
        }

        // FIX: thêm scroll
        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ---- Cảnh báo & Nhắc nhở (kết nối DB thật qua getAlerts()) ----
    private JPanel buildAlertCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        java.util.List<Map<String, Object>> alerts = thongKeService.getAlerts();

        JLabel title = new JLabel("Cảnh báo & Nhắc nhở");
        title.setFont(UIConstants.FONT_HEADER);
        JLabel cnt = new JLabel(alerts.size() + " mục");
        cnt.setFont(UIConstants.FONT_SMALL);
        cnt.setForeground(alerts.isEmpty() ? UIConstants.SUCCESS : UIConstants.DANGER);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title, BorderLayout.WEST);
        hdr.add(cnt,   BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (alerts.isEmpty()) {
            JLabel empty = new JLabel("✓ Không có cảnh báo nào");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.SUCCESS);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(Box.createVerticalStrut(16));
            list.add(empty);
        } else {
            for (Map<String, Object> alert : alerts) {
                String type     = String.valueOf(alert.getOrDefault("type",  "warning"));
                String titleStr = String.valueOf(alert.getOrDefault("title", ""));
                String descStr  = String.valueOf(alert.getOrDefault("desc",  ""));

                Color barColor;
                switch (type) {
                    case "danger":  barColor = UIConstants.DANGER;  break;
                    case "info":    barColor = UIConstants.INFO;     break;
                    default:        barColor = UIConstants.WARNING;  break;
                }

                JPanel item = new JPanel(new BorderLayout());
                item.setOpaque(false);
                item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, barColor),
                    BorderFactory.createEmptyBorder(6, 8, 6, 0)));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                JPanel info = new JPanel();
                info.setOpaque(false);
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                JLabel l1 = new JLabel(titleStr); l1.setFont(UIConstants.FONT_SMALL_BOLD); l1.setForeground(barColor);
                JLabel l2 = new JLabel(descStr);  l2.setFont(UIConstants.FONT_SMALL);      l2.setForeground(UIConstants.TEXT_MUTED);
                info.add(l1); info.add(l2);
                item.add(info, BorderLayout.CENTER);
                list.add(item);
                list.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }
}           

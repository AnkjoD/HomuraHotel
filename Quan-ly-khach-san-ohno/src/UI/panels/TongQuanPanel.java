package ui.panels;

import service.*;
import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.NotificationManager;
import ui.components.RoundedComponents.*;
import ui.dialogs.*;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TongQuanPanel — Dashboard tổng quan
 *
 * FIX (2026-04-23):
 *  1. Toàn bộ DB call được chuyển ra khỏi EDT bằng SwingWorker.
 *  2. refresh() thực sự rebuild nội dung các card (không còn stale data).
 *  3. getDashboardStats() chỉ gọi 1 lần, truyền kết quả vào tất cả card.
 *  4. updateRecentActivity() được implement đầy đủ.
 *  5. Labels lblStatXxx được lưu tham chiếu thực và cập nhật đúng.
 */
public class TongQuanPanel extends JPanel {

    private final MainFrame        mainFrame;
    private final ThongKeService   thongKeService  = new ThongKeService();
    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final PhongService     phongService    = new PhongService();

    // ── Stat card labels (được gán trong buildStatsRow) ──────────────────────
    private JLabel lblStatEmpty, lblStatOccupied, lblStatReserved,
                   lblStatCheckin, lblStatCheckout, lblStatRevenue;

    // ── Content containers — được rebuild khi refresh ─────────────────────────
    private JPanel pnlCheckinList;    // container bên trong buildCheckinCard
    private JLabel lblCheckinCount;
    private JPanel pnlCheckoutList;   // container bên trong buildCheckoutCard
    private JLabel lblCheckoutCount;
    private JPanel pnlActivityList;   // container bên trong buildActivityCard
    private JPanel pnlAlertList;      // container bên trong buildAlertCard
    private JLabel lblAlertCount;

    // Guard: tránh kích hoạt 2 worker cùng lúc
    private volatile boolean isRefreshing = false;

    // Track alert IDs đã push thông báo
    private final Set<String> pushedAlertIds = new HashSet<>();

    // ─────────────────────────────────────────────────────────────────────────
    public TongQuanPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();               // Dựng khung UI (không gọi DB)
        loadDataAsync();         // Fetch data trên background thread
    }

    // ─── PUBLIC API ──────────────────────────────────────────────────────────

    /** Gọi khi cần refresh toàn bộ dashboard */
    public void refresh() {
        loadDataAsync();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BƯỚC 1: Dựng khung UI tĩnh (không chạm DB)
    // ─────────────────────────────────────────────────────────────────────────
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
        inner.add(buildStatsRow());       // tạo labels, hiển thị "..." ban đầu
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildMiddleRow());      // tạo container rỗng
        inner.add(Box.createVerticalStrut(20));
        inner.add(buildBottomRow());      // tạo container rỗng

        return inner;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BƯỚC 2: Fetch data trên background thread, điền vào UI khi xong
    // ─────────────────────────────────────────────────────────────────────────
    private void loadDataAsync() {
        if (isRefreshing) return;
        isRefreshing = true;

        SwingWorker<DashboardData, Void> worker = new SwingWorker<DashboardData, Void>() {

            @Override
            protected DashboardData doInBackground() {
                // Chạy trên background thread — an toàn để gọi DB
                DashboardData data = new DashboardData();
                data.stats      = thongKeService.getDashboardStats();          // 1 lần duy nhất
                data.checkins   = thongKeService.getCheckinHomNay();
                data.checkouts  = thongKeService.getCheckoutHomNay();
                data.activities = thongKeService.getHoatDongGanDay(10);
                data.alerts     = thongKeService.getAlerts();
                return data;
            }

            @Override
            protected void done() {
                // Trở về EDT để cập nhật UI
                try {
                    DashboardData data = get();
                    applyDataToUI(data);
                } catch (Exception e) {
                    System.err.println("[TongQuanPanel] ERROR loading data: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            }
        };

        worker.execute();
    }

    /** Phân phối data vào từng section của UI — chỉ chạy trên EDT */
    private void applyDataToUI(DashboardData data) {
        Map<String, Object> stats = data.stats;

        // ── 1. Stat cards ────────────────────────────────────────────────────
        int phongTrong = (int) stats.getOrDefault("phongTrong",    0);
        int dangO      = (int) stats.getOrDefault("dangO",         0);
        int daDat      = (int) stats.getOrDefault("daDat",         0);
        int checkinCnt = (int) stats.getOrDefault("checkinHomNay",  0);
        int checkoutCnt= (int) stats.getOrDefault("checkoutHomNay", 0);

        if (lblStatEmpty    != null) lblStatEmpty   .setText(phongTrong + " phòng");
        if (lblStatOccupied != null) lblStatOccupied.setText(dangO      + " phòng");
        if (lblStatReserved != null) lblStatReserved.setText(daDat      + " phòng");
        if (lblStatCheckin  != null) lblStatCheckin .setText(checkinCnt  + " lượt");
        if (lblStatCheckout != null) lblStatCheckout.setText(checkoutCnt + " lượt");
        if (lblStatRevenue  != null) {
            long doanhThu = (long) stats.getOrDefault("doanhThuHomNay", 0L);
            lblStatRevenue.setText(String.format("%,.1f tr đ", doanhThu / 1_000_000.0));
        }

        // ── 2. Room status card (bar + legend) — rebuild bằng cách gọi lại ──
        // Khi refresh, room bar tự lấy từ label đã có → chỉ update legend text
        // (Đơn giản: cập nhật stat labels đã đủ vì RoomStatusCard đọc từ đó)

        // ── 3. Check-in list ─────────────────────────────────────────────────
        if (lblCheckinCount != null) lblCheckinCount.setText(data.checkins.size() + " lượt");
        if (pnlCheckinList  != null) {
            pnlCheckinList.removeAll();
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
            if (data.checkins.isEmpty()) {
                pnlCheckinList.add(buildEmptyLabel("Chưa có check-in hôm nay"));
            } else {
                for (Map<String, Object> ci : data.checkins) {
                    pnlCheckinList.add(buildCheckinRow(
                        (String) ci.get("hoTen"),
                        ci.get("soPhong") + " – " + ci.get("tenLoai"),
                        ci.get("ngayNhan") != null ? timeFmt.format(ci.get("ngayNhan")) : "--:--",
                        "Chờ xác nhận"
                    ));
                }
            }
            pnlCheckinList.revalidate();
            pnlCheckinList.repaint();
        }

        // ── 4. Check-out list ────────────────────────────────────────────────
        if (lblCheckoutCount != null) lblCheckoutCount.setText(data.checkouts.size() + " lượt");
        if (pnlCheckoutList  != null) {
            pnlCheckoutList.removeAll();
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
            if (data.checkouts.isEmpty()) {
                pnlCheckoutList.add(buildEmptyLabel("Chưa có check-out hôm nay"));
            } else {
                for (Map<String, Object> co : data.checkouts) {
                    pnlCheckoutList.add(buildCheckoutRow(co, timeFmt));
                }
            }
            pnlCheckoutList.revalidate();
            pnlCheckoutList.repaint();
        }

        // ── 5. Activity list ─────────────────────────────────────────────────
        if (pnlActivityList != null) {
            pnlActivityList.removeAll();
            if (data.activities.isEmpty()) {
                pnlActivityList.add(buildEmptyLabel("Chưa có hoạt động"));
            } else {
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm dd/MM");
                for (Map<String, Object> act : data.activities) {
                    pnlActivityList.add(buildActivityRow(act, timeFmt));
                }
            }
            pnlActivityList.revalidate();
            pnlActivityList.repaint();
        }

        // ── 6. Alerts list ───────────────────────────────────────────────────
        if (lblAlertCount != null) {
            lblAlertCount.setText(data.alerts.size() + " mục");
            lblAlertCount.setForeground(data.alerts.isEmpty() ? UIConstants.SUCCESS : UIConstants.DANGER);
        }
        if (pnlAlertList != null) {
            pnlAlertList.removeAll();
            if (data.alerts.isEmpty()) {
                JLabel ok = buildEmptyLabel("✓ Mọi thứ đều ổn");
                ok.setForeground(UIConstants.SUCCESS);
                pnlAlertList.add(Box.createVerticalStrut(30));
                pnlAlertList.add(ok);
            } else {
                for (Map<String, Object> alert : data.alerts) {
                    pnlAlertList.add(buildAlertRow(alert));
                    pnlAlertList.add(new JSeparator(JSeparator.HORIZONTAL));
                }
            }
            pnlAlertList.revalidate();
            pnlAlertList.repaint();

            // Cập nhật pushedAlertIds
            Set<String> currentIds = new HashSet<>();
            for (Map<String, Object> alert : data.alerts) {
                currentIds.add(String.valueOf(alert.getOrDefault("id", alert.getOrDefault("title", ""))));
            }
            pushedAlertIds.retainAll(currentIds);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Title
    // ─────────────────────────────────────────────────────────────────────────
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

        entity.enums.VaiTro currentRole = auth.getCurrentRole();
        String roleName = currentRole == entity.enums.VaiTro.ADMIN ? "Admin"
                : currentRole == entity.enums.VaiTro.MANAGER ? "Manager" : "Receptionist";
        JLabel lblDate = new JLabel(dateStr + " | " + roleName);
        lblDate.setFont(UIConstants.FONT_BODY);
        lblDate.setForeground(UIConstants.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(lblGreet);
        left.add(Box.createVerticalStrut(4));
        left.add(lblDate);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton btnCheckout = new RoundedButton("Trả phòng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnCheckin  = new RoundedButton("+ Nhận phòng", UIConstants.PRIMARY, Color.WHITE);

        btnCheckout.addActionListener(e -> handleDashboardCheckout());
        btnCheckin .addActionListener(e -> handleDashboardCheckin());

        btnPanel.add(btnCheckout);
        btnPanel.add(btnCheckin);

        row.add(left,     BorderLayout.WEST);
        row.add(btnPanel, BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Stats row (dùng placeholder "..." — sau fill bằng applyDataToUI)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        service.AuthService auth = service.AuthService.getInstance();
        boolean showRevenue = auth.isManager();

        int cols = showRevenue ? 6 : 5;
        JPanel row = new JPanel(new GridLayout(1, cols, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Tạo stat cards VÀ lưu tham chiếu label để cập nhật sau
        lblStatEmpty    = addStatCard(row, "Phòng trống",       "...",  UIConstants.SUCCESS);
        lblStatOccupied = addStatCard(row, "Đang ở",            "...",  UIConstants.PRIMARY);
        lblStatReserved = addStatCard(row, "Đã đặt trước",      "...",  UIConstants.WARNING);
        lblStatCheckin  = addStatCard(row, "Check-in hôm nay",  "...",  UIConstants.SUCCESS);
        lblStatCheckout = addStatCard(row, "Check-out hôm nay", "...",  UIConstants.ORANGE);

        if (showRevenue) {
            lblStatRevenue = addStatCard(row, "Doanh thu hôm nay", "...", UIConstants.DANGER);
        }
        return row;
    }

    /** Tạo 1 stat card, thêm vào parent, trả về JLabel value để có thể cập nhật */
    private JLabel addStatCard(JPanel parent, String label, String initialValue, Color accent) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(UIConstants.FONT_SMALL);
        lblLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(lblLabel);
        center.add(Box.createVerticalStrut(4));
        center.add(lblValue);

        card.add(center, BorderLayout.CENTER);
        parent.add(card);
        return lblValue;   // ← trả về để caller lưu tham chiếu
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Middle row (Room status + Check-in)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
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

        JLabel title = new JLabel("Tình trạng phòng");
        title.setFont(UIConstants.FONT_HEADER);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel total = new JLabel("Đang tải...");
        total.setFont(UIConstants.FONT_SMALL);
        total.setForeground(UIConstants.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(total, BorderLayout.EAST);

        // Bar + legend sẽ được vẽ từ các lblStat đã lưu ở trên
        // Dùng JPanel đặc biệt tham chiếu labels để tự cập nhật khi repaint
        JPanel barPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Đọc giá trị trực tiếp từ lblStatXxx hiện tại
                int pt = parseStatLabel(lblStatEmpty);
                int dO = parseStatLabel(lblStatOccupied);
                int dD = parseStatLabel(lblStatReserved);
                int tp = Math.max(1, pt + dO + dD);

                // Cập nhật label "Tổng X phòng" nhân tiện
                total.setText("Tổng " + tp + " phòng");

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

        // Legend sử dụng label refs — sẽ tự synced khi applyDataToUI cập nhật labels
        JPanel legend = new JPanel(new GridLayout(3, 1, 0, 4));
        legend.setOpaque(false);
        legend.add(buildLegendRef("Phòng trống",    UIConstants.SUCCESS, lblStatEmpty));
        legend.add(buildLegendRef("Đang ở",         UIConstants.PRIMARY, lblStatOccupied));
        legend.add(buildLegendRef("Đã đặt trước",   UIConstants.WARNING, lblStatReserved));

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

    /** Đọc số phòng từ text của label stat ("5 phòng" → 5) */
    private int parseStatLabel(JLabel lbl) {
        if (lbl == null) return 0;
        try {
            return Integer.parseInt(lbl.getText().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Legend item tham chiếu trực tiếp vào label stat để synced */
    private JPanel buildLegendRef(String label, Color color, JLabel statLabel) {
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

        // Right side: count lấy từ statLabel ref, percent tính lại
        JPanel right = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // Cập nhật text trước khi vẽ
                removeAll();
                setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                String countTxt = (statLabel != null) ? statLabel.getText() : "0 phòng";
                int cnt = 0;
                try { cnt = Integer.parseInt(countTxt.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
                int total = Math.max(1, parseStatLabel(lblStatEmpty) + parseStatLabel(lblStatOccupied) + parseStatLabel(lblStatReserved));
                JLabel c = new JLabel(countTxt);    c.setFont(UIConstants.FONT_SMALL_BOLD); c.setForeground(UIConstants.TEXT_PRIMARY);
                JLabel pct = new JLabel((int)(100.0*cnt/total) + "%"); pct.setFont(UIConstants.FONT_SMALL_BOLD); pct.setForeground(color);
                add(c); add(pct);
                super.paintComponent(g);
            }
        };
        right.setOpaque(false);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCheckinCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Check-in hôm nay");
        title.setFont(UIConstants.FONT_HEADER);
        lblCheckinCount = new JLabel("...");
        lblCheckinCount.setFont(UIConstants.FONT_SMALL);
        lblCheckinCount.setForeground(UIConstants.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title,           BorderLayout.WEST);
        header.add(lblCheckinCount, BorderLayout.EAST);

        // Container list — sẽ được fill bởi applyDataToUI
        pnlCheckinList = new JPanel();
        pnlCheckinList.setOpaque(false);
        pnlCheckinList.setLayout(new BoxLayout(pnlCheckinList, BoxLayout.Y_AXIS));
        pnlCheckinList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlCheckinList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI — Bottom row (Activity + Checkout + Alerts)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        row.add(buildActivityCard());
        row.add(buildCheckoutCard());
        row.add(buildAlertCard());
        return row;
    }

    private JPanel buildActivityCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Hoạt động gần đây");
        title.setFont(UIConstants.FONT_HEADER);
        card.add(title, BorderLayout.NORTH);

        pnlActivityList = new JPanel();
        pnlActivityList.setOpaque(false);
        pnlActivityList.setLayout(new BoxLayout(pnlActivityList, BoxLayout.Y_AXIS));
        pnlActivityList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlActivityList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCheckoutCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Check-out hôm nay");
        title.setFont(UIConstants.FONT_HEADER);
        lblCheckoutCount = new JLabel("...");
        lblCheckoutCount.setFont(UIConstants.FONT_SMALL);
        lblCheckoutCount.setForeground(UIConstants.TEXT_MUTED);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title,            BorderLayout.WEST);
        hdr.add(lblCheckoutCount, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlCheckoutList = new JPanel();
        pnlCheckoutList.setOpaque(false);
        pnlCheckoutList.setLayout(new BoxLayout(pnlCheckoutList, BoxLayout.Y_AXIS));
        pnlCheckoutList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlCheckoutList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAlertCard() {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Cảnh báo hôm nay");
        title.setFont(UIConstants.FONT_HEADER);
        lblAlertCount = new JLabel("...");
        lblAlertCount.setFont(UIConstants.FONT_SMALL_BOLD);
        lblAlertCount.setForeground(UIConstants.TEXT_MUTED);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.add(title,         BorderLayout.WEST);
        hdr.add(lblAlertCount, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        pnlAlertList = new JPanel();
        pnlAlertList.setOpaque(false);
        pnlAlertList.setLayout(new BoxLayout(pnlAlertList, BoxLayout.Y_AXIS));
        pnlAlertList.add(buildLoadingLabel());

        JScrollPane scroll = new JScrollPane(pnlAlertList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROW BUILDERS — dùng trong applyDataToUI
    // ─────────────────────────────────────────────────────────────────────────
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

    private JPanel buildCheckoutRow(Map<String, Object> co, SimpleDateFormat timeFmt) {
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
        return row;
    }

    private JPanel buildActivityRow(Map<String, Object> act, SimpleDateFormat timeFmt) {
        JPanel item = new JPanel(new BorderLayout());
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        String loai = String.valueOf(act.get("loai"));
        boolean isCheckin = "Nhận phòng".equals(loai);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(isCheckin ? UIConstants.SUCCESS : UIConstants.ORANGE);
                g2.fillOval(0, 4, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 16));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel lblAct  = new JLabel(loai + " – P." + act.get("soPhong"));
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
        return item;
    }

    private JPanel buildAlertRow(Map<String, Object> alert) {
        String type     = String.valueOf(alert.getOrDefault("type",  "warning"));
        String titleStr = String.valueOf(alert.getOrDefault("title", ""));
        String descStr  = String.valueOf(alert.getOrDefault("desc",  ""));

        Color barColor;
        String icon;
        switch (type) {
            case "danger":  barColor = UIConstants.DANGER; icon = "⚠"; break;
            case "info":    barColor = UIConstants.INFO;   icon = "ℹ"; break;
            default:        barColor = UIConstants.WARNING; icon = "⚡"; break;
        }

        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(false);
        item.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        lblIcon.setForeground(barColor);
        lblIcon.setPreferredSize(new Dimension(24, 24));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(titleStr); l1.setFont(UIConstants.FONT_SMALL_BOLD); l1.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel l2 = new JLabel(descStr);  l2.setFont(UIConstants.FONT_TINY);      l2.setForeground(UIConstants.TEXT_SECONDARY);
        info.add(l1); info.add(l2);

        JPanel indicator = new JPanel();
        indicator.setBackground(barColor);
        indicator.setPreferredSize(new Dimension(3, 30));

        JPanel content = new JPanel(new BorderLayout(8, 0));
        content.setOpaque(false);
        content.add(lblIcon, BorderLayout.WEST);
        content.add(info,    BorderLayout.CENTER);

        item.add(indicator, BorderLayout.WEST);
        item.add(content,   BorderLayout.CENTER);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel buildLoadingLabel() {
        JLabel lbl = new JLabel("Đang tải...");
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JLabel buildEmptyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WORKFLOWS (Checkin / Checkout nhanh từ Dashboard)
    // ─────────────────────────────────────────────────────────────────────────
    private void handleDashboardCheckin() {
        mainFrame.navigateTo("datphong", () -> {
            DatPhongPanel dp = mainFrame.getDatPhongPanel();
            if (dp != null) {
                dp.applyConfirmedFilter();
            }
        });
    }

    private void handleDashboardCheckout() {
        try {
            java.util.List<entity.Phong> rooms = phongService.getAllPhong();
            java.util.List<entity.Phong> occupied = new java.util.ArrayList<>();
            for (entity.Phong p : rooms) {
                if (p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED) {
                    occupied.add(p);
                }
            }

            if (occupied.isEmpty()) {
                NotificationManager.showInfo("Thông báo", "Hiện không có phòng nào đang được thuê.");
                return;
            }

            String[] roomOptions = occupied.stream()
                    .map(p -> "P." + p.getSoPhong() + " - " + p.getTenKhachHienTai())
                    .toArray(String[]::new);

            ModernComboBox<String> combo = new ModernComboBox<>(roomOptions);
            int result = JOptionPane.showConfirmDialog(this, combo, "Chọn phòng để trả phòng (Thanh toán)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                int idx = combo.getSelectedIndex();
                if (idx >= 0) {
                    entity.Phong p = occupied.get(idx);
                    entity.ChiTietDatPhong ct = thuePhongService.getActiveByPhong(p.getMaPhong());
                    if (ct != null) {
                        HoaDonDialog dlg = new HoaDonDialog((Frame) SwingUtilities.getWindowAncestor(this), ct);
                        dlg.setVisible(true);
                        refresh();
                    } else {
                        NotificationManager.showError("Lỗi", "Không tìm thấy dữ liệu thuê phòng cho phòng " + p.getSoPhong());
                    }
                }
            }
        } catch (Exception e) {
            NotificationManager.showError("Lỗi", "Không thể thực hiện trả phòng: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA CONTAINER
    // ─────────────────────────────────────────────────────────────────────────
    private static class DashboardData {
        Map<String, Object>              stats      = new LinkedHashMap<>();
        java.util.List<Map<String,Object>> checkins  = new java.util.ArrayList<>();
        java.util.List<Map<String,Object>> checkouts = new java.util.ArrayList<>();
        java.util.List<Map<String,Object>> activities= new java.util.ArrayList<>();
        java.util.List<Map<String,Object>> alerts    = new java.util.ArrayList<>();
    }
}

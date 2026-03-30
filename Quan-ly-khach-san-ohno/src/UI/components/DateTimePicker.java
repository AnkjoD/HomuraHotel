package ui.components;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DateTimePicker – Component chọn ngày giờ có calendar popup.
 * Dùng thay thế JTextField nhập tay ngày trong CheckinDialog.
 *
 * Cách dùng:
 *   DateTimePicker picker = new DateTimePicker(new Date());
 *   panel.add(picker);
 *   Date selectedDate = picker.getDate();        // lấy Date
 *   String text = picker.getFormattedDate();     // lấy String "dd/MM/yyyy HH:mm"
 */
public class DateTimePicker extends JPanel {

    private final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private Calendar selectedCal;
    private JTextField txtDisplay;
    private JPopupMenu popup;
    private JLabel lblMonthYear;
    private JPanel calendarGrid;
    private JSpinner spnHour, spnMin;

    // Màu sắc
    private static final Color PRIMARY      = new Color(0x4361EE);
    private static final Color TEXT_MAIN    = new Color(0x1E293B);
    private static final Color TEXT_MUTED   = new Color(0x94A3B8);
    private static final Color BORDER_COLOR = new Color(0xE2E8F0);
    private static final Color HOVER_COLOR  = new Color(0xF1F5F9);
    private static final Color TODAY_COLOR  = new Color(0xFEF3C7);

    public DateTimePicker(Date initialDate) {
        setLayout(new BorderLayout());
        setOpaque(false);

        selectedCal = Calendar.getInstance();
        if (initialDate != null) selectedCal.setTime(initialDate);

        buildTextField();
        buildPopup();
    }

    // ---- Text field với icon lịch ----
    private void buildTextField() {
        txtDisplay = new JTextField(SDF.format(selectedCal.getTime()));
        txtDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDisplay.setEditable(false);
        txtDisplay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtDisplay.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(7, 12, 7, 36)
        ));
        txtDisplay.setBackground(Color.WHITE);

        // Vẽ icon calendar bên phải
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Icon 📅 đơn giản
                int x = getWidth() - 28, y = getHeight() / 2 - 8;
                g2.setColor(TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, 16, 14, 3, 3);
                g2.drawLine(x + 4,  y - 2, x + 4,  y + 2);
                g2.drawLine(x + 12, y - 2, x + 12, y + 2);
                g2.drawLine(x, y + 4, x + 16, y + 4);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.add(txtDisplay, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, 38));

        // Click → hiện popup
        txtDisplay.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { togglePopup(); }
        });
        wrapper.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { togglePopup(); }
        });
    }

    // ---- Popup calendar ----
    private void buildPopup() {
        popup = new JPopupMenu();
        popup.setBorder(new LineBorder(BORDER_COLOR, 1));
        popup.setBackground(Color.WHITE);

        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Header: nút prev/next + tháng năm
        JPanel header = buildCalendarHeader();
        container.add(header, BorderLayout.NORTH);

        // Lưới ngày
        calendarGrid = new JPanel(new GridLayout(7, 7, 4, 4));
        calendarGrid.setOpaque(false);
        calendarGrid.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
        buildCalendarGrid();
        container.add(calendarGrid, BorderLayout.CENTER);

        // Chọn giờ phút
        JPanel timeRow = buildTimeRow();
        container.add(timeRow, BorderLayout.SOUTH);

        popup.add(container);
    }

    private JPanel buildCalendarHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton btnPrev = arrowBtn("‹");
        JButton btnNext = arrowBtn("›");

        lblMonthYear = new JLabel("", SwingConstants.CENTER);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMonthYear.setForeground(TEXT_MAIN);
        updateMonthYearLabel();

        btnPrev.addActionListener(e -> {
            selectedCal.add(Calendar.MONTH, -1);
            updateMonthYearLabel();
            buildCalendarGrid();
            calendarGrid.revalidate();
            calendarGrid.repaint();
        });
        btnNext.addActionListener(e -> {
            selectedCal.add(Calendar.MONTH, 1);
            updateMonthYearLabel();
            buildCalendarGrid();
            calendarGrid.revalidate();
            calendarGrid.repaint();
        });

        header.add(btnPrev,      BorderLayout.WEST);
        header.add(lblMonthYear, BorderLayout.CENTER);
        header.add(btnNext,      BorderLayout.EAST);
        return header;
    }

    private void buildCalendarGrid() {
        calendarGrid.removeAll();

        // Tên các ngày trong tuần
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(TEXT_MUTED);
            calendarGrid.add(lbl);
        }

        // Xác định ngày đầu tháng
        Calendar cal = (Calendar) selectedCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int startDow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=CN
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int today      = -1;
        int selectedDay = selectedCal.get(Calendar.DAY_OF_MONTH);
        Calendar now   = Calendar.getInstance();
        boolean sameMonth = now.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
                         && now.get(Calendar.YEAR)  == selectedCal.get(Calendar.YEAR);
        if (sameMonth) today = now.get(Calendar.DAY_OF_MONTH);

        // Ô trống đầu
        for (int i = 0; i < startDow; i++) calendarGrid.add(new JLabel());

        // Các ngày
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            boolean isSelected = (d == selectedDay);
            boolean isToday    = (d == today);

            JLabel lbl = new JLabel(String.valueOf(d), SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSelected) {
                        g2.setColor(PRIMARY);
                        g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                    } else if (isToday) {
                        g2.setColor(TODAY_COLOR);
                        g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 12));
            lbl.setForeground(isSelected ? Color.WHITE : TEXT_MAIN);
            lbl.setOpaque(false);
            lbl.setPreferredSize(new Dimension(28, 28));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Hover
            lbl.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) { lbl.setOpaque(true); lbl.setBackground(HOVER_COLOR); }
                }
                public void mouseExited(MouseEvent e) {
                    lbl.setOpaque(false); lbl.repaint();
                }
                public void mouseClicked(MouseEvent e) {
                    selectedCal.set(Calendar.DAY_OF_MONTH, d);
                    syncTimeFromSpinner();
                    buildCalendarGrid();
                    calendarGrid.revalidate();
                    calendarGrid.repaint();
                    updateDisplay();
                }
            });

            calendarGrid.add(lbl);
        }

        // Ô trống cuối để đủ 6 hàng
        int total = startDow + daysInMonth;
        int remaining = (total % 7 == 0) ? 0 : 7 - (total % 7);
        for (int i = 0; i < remaining; i++) calendarGrid.add(new JLabel());
    }

    private JPanel buildTimeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 0, 0, 0)
        ));

        JLabel lblTime = new JLabel("⏰  Giờ:");
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTime.setForeground(TEXT_MUTED);

        spnHour = timeSpinner(0, 23, selectedCal.get(Calendar.HOUR_OF_DAY));
        JLabel sep = new JLabel(":");
        sep.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spnMin  = timeSpinner(0, 59, selectedCal.get(Calendar.MINUTE));

        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOk.setBackground(PRIMARY);
        btnOk.setForeground(Color.WHITE);
        btnOk.setBorderPainted(false);
        btnOk.setFocusPainted(false);
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOk.setPreferredSize(new Dimension(60, 28));
        btnOk.addActionListener(e -> {
            syncTimeFromSpinner();
            updateDisplay();
            popup.setVisible(false);
        });

        row.add(lblTime);
        row.add(spnHour);
        row.add(sep);
        row.add(spnMin);
        row.add(Box.createHorizontalStrut(10));
        row.add(btnOk);
        return row;
    }

    // ---- Helpers ----
    private JButton arrowBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setForeground(TEXT_MAIN);
        btn.setBackground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(32, 28));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(HOVER_COLOR); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private JSpinner timeSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sp.setPreferredSize(new Dimension(52, 28));
        // Format 2 chữ số
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(sp, "00");
        sp.setEditor(editor);
        return sp;
    }

    private void updateMonthYearLabel() {
        String[] months = {"Tháng 1","Tháng 2","Tháng 3","Tháng 4","Tháng 5","Tháng 6",
                           "Tháng 7","Tháng 8","Tháng 9","Tháng 10","Tháng 11","Tháng 12"};
        lblMonthYear.setText(months[selectedCal.get(Calendar.MONTH)]
                             + " " + selectedCal.get(Calendar.YEAR));
    }

    private void syncTimeFromSpinner() {
        if (spnHour != null) selectedCal.set(Calendar.HOUR_OF_DAY, (int) spnHour.getValue());
        if (spnMin  != null) selectedCal.set(Calendar.MINUTE,      (int) spnMin.getValue());
        selectedCal.set(Calendar.SECOND, 0);
    }

    private void updateDisplay() {
        txtDisplay.setText(SDF.format(selectedCal.getTime()));
    }

    private void togglePopup() {
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            popup.show(this, 0, getHeight());
        }
    }

    // ---- Public API ----
    /** Lấy ngày đã chọn dưới dạng Date */
    public Date getDate() {
        syncTimeFromSpinner();
        return selectedCal.getTime();
    }

    /** Lấy ngày đã chọn dưới dạng String "dd/MM/yyyy HH:mm" */
    public String getFormattedDate() {
        return SDF.format(getDate());
    }

    /** Set ngày từ bên ngoài */
    public void setDate(Date date) {
        if (date != null) {
            selectedCal.setTime(date);
            if (spnHour != null) spnHour.setValue(selectedCal.get(Calendar.HOUR_OF_DAY));
            if (spnMin  != null) spnMin.setValue(selectedCal.get(Calendar.MINUTE));
            updateDisplay();
            buildCalendarGrid();
        }
    }
}


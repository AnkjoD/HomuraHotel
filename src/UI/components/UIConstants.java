package ui.components;

import entity.enums.*;
import java.awt.*;
import ui.theme.Theme;
import ui.theme.ThemeManager;

public class UIConstants {

    private static Theme current = ThemeManager.getCurrentTheme();

    public static Color PRIMARY       = current.primary;
    public static Color PRIMARY_LIGHT = current.primaryLight;
    public static Color PRIMARY_DARK  = current.primaryDark;
    public static Color ACCENT        = current.accent;

    public static Color BG_MAIN          = current.bgMain;
    public static Color BG_SIDEBAR       = current.bgSidebar;
    public static Color BG_HEADER        = current.bgHeader;
    public static Color BG_CARD          = current.bgCard;
    public static Color BG_CARD_ELEVATED = current.bgCardElevated;
    public static Color BG_TABLE_HEADER  = current.bgTableHeader;
    public static Color INPUT_BG         = current.inputBg;

    public static Color TEXT_PRIMARY   = current.textPrimary;
    public static Color TEXT_SECONDARY = current.textSecondary;
    public static Color TEXT_MUTED     = current.textMuted;

    public static Color SUCCESS        = current.success;
    public static Color SUCCESS_LIGHT  = current.successLight;
    public static Color WARNING        = current.warning;
    public static Color WARNING_LIGHT  = current.warningLight;
    public static Color DANGER         = current.danger;
    public static Color DANGER_LIGHT   = current.dangerLight;
    public static Color INFO           = current.info;
    public static Color INFO_LIGHT     = current.infoLight;
    public static Color ORANGE         = new Color(0xF97316);
    public static Color ORANGE_LIGHT   = new Color(0xFED7AA);

    public static Color CARD_INDIGO    = current.primary;
    public static Color CARD_AMBER     = current.warning;
    public static Color CARD_BLUE      = current.accent;
    public static Color CARD_GREEN     = current.success;
    public static Color CARD_TEAL      = new Color(0x06B6D4);
    public static Color CARD_RED       = current.danger;

    public static Color BORDER         = current.border;
    public static Color BORDER_FOCUS   = current.borderFocus;

    public static Color SIDEBAR_ACTIVE = current.sidebarActive;
    public static Color SIDEBAR_HOVER  = current.sidebarHover;
    public static final int SIDEBAR_WIDTH = 240;

    public static final int HEADER_HEIGHT = 64;
    public static final int CARD_RADIUS   = 12;
    public static final int BTN_RADIUS    = 8;

    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADER     = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_TINY       = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font FONT_CARD_NUM   = new Font("Segoe UI", Font.BOLD, 28);

    public static void applyTheme(Theme t) {
        current = t;
        PRIMARY = t.primary;
        PRIMARY_LIGHT = t.primaryLight;
        PRIMARY_DARK = t.primaryDark;
        ACCENT = t.accent;

        BG_MAIN = t.bgMain;
        BG_SIDEBAR = t.bgSidebar;
        BG_HEADER = t.bgHeader;
        BG_CARD = t.bgCard;
        BG_CARD_ELEVATED = t.bgCardElevated;
        BG_TABLE_HEADER = t.bgTableHeader;
        INPUT_BG = t.inputBg;

        TEXT_PRIMARY = t.textPrimary;
        TEXT_SECONDARY = t.textSecondary;
        TEXT_MUTED = t.textMuted;

        BORDER = t.border;
        BORDER_FOCUS = t.borderFocus;

        SIDEBAR_ACTIVE = t.sidebarActive;
        SIDEBAR_HOVER = t.sidebarHover;

        SUCCESS = t.success;
        SUCCESS_LIGHT = t.successLight;
        WARNING = t.warning;
        WARNING_LIGHT = t.warningLight;
        DANGER = t.danger;
        DANGER_LIGHT = t.dangerLight;
        INFO = t.info;
        INFO_LIGHT = t.infoLight;

        CARD_INDIGO = t.primary;
        CARD_AMBER = t.warning;
        CARD_BLUE = t.accent;
        CARD_GREEN = t.success;
        CARD_RED = t.danger;
    }

    public static Theme getTheme() {
        return current;
    }

    public static boolean isDark() {
        return current.isDark;
    }

    public static String getTrangThaiPhongLabel(TrangThaiPhong tt) {
        if (tt == null) return "Không xác định";
        switch (tt) {
            case AVAILABLE:   return "Có sẵn";
            case OCCUPIED:    return "Đang thuê";
            case MAINTENANCE: return "Bảo trì";
            case CLEANING:    return "Vệ sinh";
            case DELETED:     return "Ngưng hoạt động";
            default:          return tt.toString();
        }
    }

    public static Color getTrangThaiPhongColor(TrangThaiPhong tt) {
        if (tt == null) return TEXT_MUTED;
        switch (tt) {
            case AVAILABLE:   return SUCCESS;
            case OCCUPIED:    return PRIMARY;
            case MAINTENANCE: return DANGER;
            case CLEANING:    return new Color(0x06B6D4);
            case DELETED:     return TEXT_MUTED;
            default:          return TEXT_MUTED;
        }
    }

    public static Color getTrangThaiPhongBg(TrangThaiPhong tt) {
        if (tt == null) return BG_CARD;
        switch (tt) {
            case AVAILABLE:   return SUCCESS_LIGHT;
            case OCCUPIED:    return PRIMARY_LIGHT;
            case MAINTENANCE: return DANGER_LIGHT;
            case CLEANING:    return isDark() ? new Color(0x083344) : new Color(0xCFFAFE);
            case DELETED:     return isDark() ? new Color(0x1F2937) : new Color(0xF1F5F9);
            default:          return BG_CARD;
        }
    }

    public static String getTrangThaiDatPhongLabel(TrangThaiDatPhong tt) {
        if (tt == null) return "—";
        switch (tt) {
            case PENDING:     return "Chờ xác nhận";
            case CONFIRMED:   return "Đã xác nhận";
            case PARTIALLY_CHECKED_IN: return "Đang nhận phòng";
            case CHECKED_IN:  return "Đã nhận phòng";
            case CHECKED_OUT: return "Đã trả phòng";
            case CANCELLED:   return "Đã hủy";
            case NO_SHOW:     return "Khách không đến";
            case WAITLIST:    return "Chờ xếp phòng";
            default:          return tt.toString();
        }
    }

    public static Color getTrangThaiDatPhongColor(TrangThaiDatPhong tt) {
        if (tt == null) return TEXT_MUTED;
        switch (tt) {
            case PENDING:     return WARNING;
            case CONFIRMED:   return SUCCESS;
            case PARTIALLY_CHECKED_IN: return INFO;
            case CHECKED_IN:  return PRIMARY;
            case CHECKED_OUT: return TEXT_SECONDARY;
            case CANCELLED:   return DANGER;
            case NO_SHOW:     return new Color(0xDC2626);
            case WAITLIST:    return ORANGE;
            default:          return TEXT_MUTED;
        }
    }

    public static String getVaiTroLabel(VaiTro v) {
        if (v == null) return "—";
        switch (v) {
            case ADMIN:        return "Quản trị viên";
            case MANAGER:      return "Quản lý khách sạn";
            case RECEPTIONIST: return "Lễ tân";
            default:           return v.toString();
        }
    }

    public static String getTrangThaiThanhToanLabel(TrangThaiThanhToan tt) {
        if (tt == null) return "Chưa thanh toán";
        switch (tt) {
            case PAID:           return "Đã thanh toán";
            case PARTIALLY_PAID: return "Thanh toán một phần";
            case UNPAID:         return "Chưa thanh toán";
            case REFUNDED:       return "Đã hoàn tiền";
            default:             return tt.toString();
        }
    }

    public static Color getTrangThaiPhongColor(String tt) {
        try { return getTrangThaiPhongColor(TrangThaiPhong.valueOf(tt)); } catch(Exception e) {
            if ("Có sẵn".equals(tt))    return SUCCESS;
            if ("Đang thuê".equals(tt)) return PRIMARY;
            if ("Đã đặt".equals(tt))    return WARNING;
            if ("Vệ sinh".equals(tt))   return new Color(0x06B6D4);
            if ("Bảo trì".equals(tt))   return DANGER;
            return TEXT_MUTED;
        }
    }

    public static Color getTrangThaiPhongBg(String tt) {
        try { return getTrangThaiPhongBg(TrangThaiPhong.valueOf(tt)); } catch(Exception e) {
            if ("Có sẵn".equals(tt))    return SUCCESS_LIGHT;
            if ("Đang thuê".equals(tt)) return PRIMARY_LIGHT;
            if ("Đã đặt".equals(tt))    return WARNING_LIGHT;
            if ("Vệ sinh".equals(tt))   return isDark() ? new Color(0x083344) : new Color(0xCFFAFE);
            if ("Bảo trì".equals(tt))   return DANGER_LIGHT;
            return BG_CARD;
        }
    }

    public static Color getHangKhachColor(String hang) {
        if (hang == null)          return TEXT_MUTED;
        if ("VIP".equals(hang))    return new Color(0x8B5CF6);
        if ("Gold".equals(hang))   return new Color(0xD97706);
        if ("Silver".equals(hang)) return new Color(0x64748B);
        return TEXT_MUTED;
    }

    public static String formatRoomNumber(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.toUpperCase().startsWith("P") && trimmed.length() > 1 && Character.isDigit(trimmed.charAt(1))) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}

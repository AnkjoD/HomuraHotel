package entity.enums;

public enum LoaiGiam {
    PERCENT("Phần trăm (%)"),
    FIXED("Tiền mặt (₫)");

    private final String description;

    LoaiGiam(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}

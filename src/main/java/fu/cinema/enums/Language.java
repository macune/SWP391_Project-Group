package fu.cinema.enums;

public enum Language {
    EN_SUB("Tiếng Anh - Phụ đề VN"),
    KR_SUB("Tiếng Hàn - Phụ đề VN"),
    JP_SUB("Tiếng Nhật - Phụ đề VN"),
    TH_SUB("Tiếng Thái - Phụ đề VN"),
    VI_ORIGINAL("Phim Tiếng Việt"),
    VI_DUBBED("Lồng tiếng Tiếng Việt");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
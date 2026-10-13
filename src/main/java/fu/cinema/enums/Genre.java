package fu.cinema.enums;

public enum Genre {
    ACTION("Hành động"),
    COMEDY("Hài hước"),
    ROMANCE("Tình cảm"),
    HORROR("Kinh dị"),
    ANIMATION("Hoạt hình"),
    SCI_FI("Khoa học viễn tưởng"),
    THRILLER("Tâm lý - Giật gân"),
    ADVENTURE("Phiêu lưu"),
    DOCUMENTARY("Tài liệu");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
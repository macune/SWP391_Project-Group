package fu.cinema.enums;

public enum AgeRating {
    P("P - Phổ biến (Mọi độ tuổi)"),
    T13("T13 - Khán giả từ 13 tuổi trở lên"),
    T16("T16 - Khán giả từ 16 tuổi trở lên"),
    T18("T18 - Khán giả từ 18 tuổi trở lên");

    private final String displayName;

    AgeRating(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
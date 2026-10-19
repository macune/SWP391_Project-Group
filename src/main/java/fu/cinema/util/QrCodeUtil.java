package fu.cinema.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import fu.cinema.exception.AppException;
import fu.cinema.exception.ErrorCode;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class QrCodeUtil {

    private static final String SECRET_SALT = "CINEFLOW_SECRET_2026";

    private QrCodeUtil() {
    }

    /**
     * Sinh mã tạm thời khi ghế mới ở trạng thái LOCKED (chưa thanh toán)
     * để thỏa mãn ràng buộc NOT NULL, UNIQUE của cột qr_code trong bảng tickets.
     */
    public static String generateLockedPlaceholder(Long bookingId, Long showtimeId, Long seatId) {
        return String.format("LOCK-B%d-S%d-SEAT%d-%s",
                bookingId, showtimeId, seatId, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    /**
     * Sinh chuỗi định danh QR Code chính thức (duy nhất, có băm SHA-256) khi phát hành vé điện tử.
     */
    public static String generateOfficialQrCode(Long bookingId, Long showtimeId, Long seatId) {
        try {
            String rawPayload = bookingId + ":" + showtimeId + ":" + seatId + ":"
                    + System.nanoTime() + ":" + UUID.randomUUID() + ":" + SECRET_SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPayload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexHash = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                hexHash.append(String.format("%02X", hashBytes[i]));
            }
            return String.format("CF-TKT-B%d-S%d-G%d-%s", bookingId, showtimeId, seatId, hexHash);
        } catch (Exception e) {
            throw new AppException(ErrorCode.QR_GENERATION_FAILED);
        }
    }

    /**
     * Chuyển chuỗi nội dung QR thành chuỗi ảnh Base64 (Data URI) để nhúng thẳng vào thẻ <img>.
     */
    public static String generateQrCodeBase64(String qrText, int width, int height) {
        if (qrText == null || qrText.isBlank()) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            throw new AppException(ErrorCode.QR_GENERATION_FAILED);
        }
    }
}
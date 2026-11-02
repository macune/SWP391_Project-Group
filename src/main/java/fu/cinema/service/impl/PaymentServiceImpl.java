package fu.cinema.service.impl;

import fu.cinema.config.VNPayConfig;
import fu.cinema.dto.request.PaymentCreateRequest;
import fu.cinema.dto.response.PaymentCallbackResponse;
import fu.cinema.dto.response.PaymentUrlResponse;
import fu.cinema.entity.*;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.PaymentMethod;
import fu.cinema.exception.AppException;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.BranchStockRepository;
import fu.cinema.repository.PaymentTransactionRepository;
import fu.cinema.service.NotificationService;
import fu.cinema.service.PaymentService;
import fu.cinema.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final TicketService ticketService;
    private final BranchStockRepository branchStockRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final NotificationService notificationService;

    @Override
    public PaymentUrlResponse createVNPayUrl(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException("Không tìm thấy đơn đặt vé."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Đơn hàng này đã được xử lý hoặc đã hết hạn.");
        }

        // BẮT BUỘC CỦA VNPAY: Số tiền gửi đi phải được nhân với 100 (VD: 100k -> 10.000.000)
        long amount = booking.getTotalAmount().longValue() * 100;

        // Chuẩn bị một Map chứa các Key-Value cấu hình gửi cho VNPay
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode); // Mã Website của rạp
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        // Mã giao dịch nội bộ (Nối ID đơn hàng với Timestamp để luôn luôn là duy nhất)
        vnp_Params.put("vnp_TxnRef", request.getBookingId() + "_" + System.currentTimeMillis());
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang CineFlow " + request.getBookingId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl); // Nơi VNPay sẽ trả kết quả về
        vnp_Params.put("vnp_IpAddr", request.getIpAddress());

        // Cài đặt thời gian tạo và thời gian hết hạn của link thanh toán (10 phút)
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 10);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // BƯỚC MÃ HÓA BẢO MẬT BẮT BUỘC
        // Lấy tất cả tên các tham số ra một mảng và SẮP XẾP theo thứ tự Alphabet (A-Z)
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        // Vòng lặp để nối các tham số thành một chuỗi URL (Ví dụ: vnp_Amount=10000&vnp_Command=pay...)
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append("=").append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append("&");
                    hashData.append("&");
                }
            }
        }
        String queryUrl = query.toString();

        // Dùng hàm băm thuật toán SHA-512 để băm chuỗi URL trên cùng với MÃ BÍ MẬT (Secret Key)
        // Tạo ra 1 đoạn mã Hash ngẫu nhiên không thể dịch ngược, đính kèm vào URL
        String vnp_SecureHash = VNPayConfig.hashAllFields(vnp_Params);
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;

        return PaymentUrlResponse.builder()
                .paymentUrl(paymentUrl)
                .message("Tạo URL thanh toán thành công")
                .build();
    }

    @Override
    public PaymentCallbackResponse validateVNPayCallback(Map<String, String> params) {
        // Tách riêng chữ ký bảo mật mà VNPay đã tạo ra
        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        params.remove("vnp_SecureHash");

        // Hệ thống của mình tự băm lại mảng dữ liệu VNPay gửi sang (Bằng Secret Key của mình)
        String signValue = VNPayConfig.hashAllFields(params);
        Long bookingId = Long.parseLong(params.get("vnp_TxnRef").split("_")[0]);
        String transactionNo = params.get("vnp_TransactionNo");

        // So sánh 2 chữ ký: Nếu KHÔNG GIỐNG NHAU -> Có hacker chèn dữ liệu hoặc sửa URL
        if (!signValue.equals(vnp_SecureHash)) {
            notificationService.sendPaymentFailedAlert(bookingId, "Lỗi xác thực chữ ký VNPay (Dữ liệu bị can thiệp).");
            return PaymentCallbackResponse.builder().success(false).bookingId(bookingId)
                    .message("Lỗi xác thực chữ ký VNPay (Dữ liệu bị can thiệp).").build();
        }

        // Nếu chữ ký khớp, kiểm tra mã phản hồi vnp_ResponseCode
        // Chuẩn VNPay: "00" nghĩa là Giao dịch thành công, tiền đã trừ.
        if ("00".equals(params.get("vnp_ResponseCode"))) {
            return PaymentCallbackResponse.builder().success(true).bookingId(bookingId)
                    .transactionNo(transactionNo).message("Thanh toán thành công.").build();
        } else {
            // Khách hàng bấm Hủy thanh toán hoặc thẻ không đủ tiền
            notificationService.sendPaymentFailedAlert(bookingId,
                    "Giao dịch thanh toán VNPay bị hủy hoặc thất bại (Mã phản hồi: " + params.get("vnp_ResponseCode") + ").");
            return PaymentCallbackResponse.builder().success(false).bookingId(bookingId)
                    .message("Giao dịch thanh toán bị hủy hoặc thất bại.").build();
        }
    }

    @Override
    @Transactional
    public void processSuccessfulPayment(Long bookingId, PaymentMethod method, String transactionNo) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn hàng"));

        // 1. CHỐT CHẶN RACE CONDITION TẠI ĐÂY
        int updatedRows = bookingRepository.confirmBookingIfPending(bookingId);

        if (updatedRows == 0) {
            // Nếu 2 người cùng thanh toán, người thứ 2 sẽ nhận updatedRows = 0 và bị quăng lỗi, Rollback toàn bộ
            throw new AppException("Giao dịch từ chối: Đơn hàng này vừa được thanh toán hoặc đang xử lý!");
        }

        // Đồng bộ lại trạng thái object trên RAM (vì DB đã được cập nhật ngầm bên dưới)
        booking.setStatus(BookingStatus.CONFIRMED);

        String finalTxNo = transactionNo != null ? transactionNo : "CASH-" + System.currentTimeMillis();

        // 2. LƯU LỊCH SỬ DÒNG TIỀN
        if (!paymentTransactionRepository.existsByBooking_BookingId(bookingId)) {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .paymentMethod(method)
                    .transactionNo(finalTxNo)
                    .paymentTime(LocalDateTime.now())
                    .build();
            paymentTransactionRepository.save(transaction);
        }

        // 3. XUẤT VÉ XEM PHIM
        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            ticketService.generateETicketsForBooking(bookingId);
        }

        // 4. XUẤT KHO ĐỒ ĂN THỨC UỐNG
        if (booking.getBookingFnbs() != null && !booking.getBookingFnbs().isEmpty()) {
            Branch branch = null;
            if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
                branch = booking.getTickets().get(0).getShowtime().getHall().getBranch();
            } else if (booking.getAccount() != null && booking.getAccount().getStaff() != null) {
                branch = booking.getAccount().getStaff().getBranch();
            }

            if (branch != null) {
                for (BookingFnB fnB : booking.getBookingFnbs()) {
                    BranchStock stock = branchStockRepository.findByBranch_BranchIdAndItem_ItemId(
                                    branch.getBranchId(), fnB.getFnbItem().getItemId())
                            .orElseThrow(() -> new AppException("Kho không đủ mặt hàng " + fnB.getFnbItem().getName()));

                    if (stock.getQuantity() < fnB.getQuantity()) {
                        throw new AppException("Số lượng " + fnB.getFnbItem().getName() + " trong kho không đủ!");
                    }
                    stock.setQuantity(stock.getQuantity() - fnB.getQuantity());
                    branchStockRepository.save(stock);
                }
            }
        }

        // 5. KÍCH HOẠT THÔNG BÁO TỰ ĐỘNG
        notificationService.sendPaymentSuccessAlert(booking, method, finalTxNo);
        notificationService.sendBookingConfirmedAlert(booking);
    }
}
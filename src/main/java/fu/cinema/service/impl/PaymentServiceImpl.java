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

    @Override
    public PaymentUrlResponse createVNPayUrl(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException("Không tìm thấy đơn đặt vé."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Đơn hàng này đã được xử lý hoặc đã hết hạn.");
        }

        // Đã sửa lỗi mất dấu nhân ở đây
        long amount = booking.getTotalAmount().longValue() * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", request.getBookingId() + "_" + System.currentTimeMillis());
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang CineFlow " + request.getBookingId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", request.getIpAddress());

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 10);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
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
        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        params.remove("vnp_SecureHash");

        String signValue = VNPayConfig.hashAllFields(params);
        Long bookingId = Long.parseLong(params.get("vnp_TxnRef").split("_")[0]);
        String transactionNo = params.get("vnp_TransactionNo");

        if (!signValue.equals(vnp_SecureHash)) {
            return PaymentCallbackResponse.builder()
                    .success(false)
                    .bookingId(bookingId)
                    .message("Lỗi xác thực chữ ký VNPay (Dữ liệu bị can thiệp).")
                    .build();
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            return PaymentCallbackResponse.builder()
                    .success(true)
                    .bookingId(bookingId)
                    .transactionNo(transactionNo)
                    .message("Thanh toán thành công.")
                    .build();
        } else {
            return PaymentCallbackResponse.builder()
                    .success(false)
                    .bookingId(bookingId)
                    .message("Giao dịch thanh toán bị hủy hoặc thất bại.")
                    .build();
        }
    }

    @Override
    @Transactional
    public void processSuccessfulPayment(Long bookingId, PaymentMethod method, String transactionNo) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return;
        }
        //1. Lưu Transaction
        if (!paymentTransactionRepository.existsByBooking_BookingId(bookingId)) {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .paymentMethod(method)
                    .transactionNo(transactionNo != null ? transactionNo : "CASH-" + System.currentTimeMillis())
                    .paymentTime(LocalDateTime.now())
                    .build();
            paymentTransactionRepository.save(transaction);
        }

        if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
            ticketService.generateETicketsForBooking(bookingId);
        } else {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

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
                    // Đã sửa lỗi mất dấu trừ ở đây
                    stock.setQuantity(stock.getQuantity() - fnB.getQuantity());
                    branchStockRepository.save(stock);
                }
            }
        }
    }
}
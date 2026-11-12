package fu.cinema.service;

import fu.cinema.dto.request.PromotionRequest;
import fu.cinema.dto.response.PromotionResponse;
import java.util.List;

public interface PromotionService {

    // Lấy danh sách mã khuyến mãi còn hạn và đang mở
    List<PromotionResponse> getActivePromotions();

    // Lấy toàn bộ lịch sử mã khuyến mãi
    List<PromotionResponse> getAllPromotions();

    // Tìm kiếm mã khuyến mãi theo từ khóa
    List<PromotionResponse> searchPromotions(String keyword);

    // Lấy thông tin chi tiết 1 mã khuyến mãi
    PromotionResponse getPromotionById(Long id);

    // Lưu mã khuyến mãi Thêm mới và Cập nhật
    PromotionResponse savePromotion(PromotionRequest request);

    // Xóa mã khuyến mãi khỏi hệ thống
    void deletePromotion(Long id);
}
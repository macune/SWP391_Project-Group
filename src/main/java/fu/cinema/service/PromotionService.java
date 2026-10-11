package fu.cinema.service;
import fu.cinema.entity.Promotion;
import java.util.List;

public interface PromotionService {
    List<Promotion> getActivePromotions();
    Object validatePromotion(String code);
}
package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.entity.*;
import fu.cinema.repository.*;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BranchStockServiceImpl implements BranchStockService {

    private final BranchStockRepository branchStockRepository;
    private final BranchRepository branchRepository;
    private final FnBItemRepository fnbItemRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    // LẤY DANH SÁCH KHO
    @Override
    public List<BranchStockResponse> getStockByBranch(Long branchId) {
        List<BranchStock> stocks = branchStockRepository.findByBranch_BranchId(branchId);

        return stocks.stream()
                .filter(stock -> stock.getItem().getIsAvailable())
                .map(stock -> BranchStockResponse.builder()
                        .id(stock.getId())
                        .branchId(stock.getBranch().getBranchId())
                        .branchName(stock.getBranch().getBranchName())
                        .itemId(stock.getItem().getItemId())
                        .itemName(stock.getItem().getName())
                        .itemCategory(stock.getItem().getCategory().name())
                        .quantity(stock.getQuantity())
                        .lastUpdated(stock.getLastUpdated())
                        .price(stock.getItem().getPrice())
                        .imageUrl(stock.getItem().getImageUrl())
                        .isAvailable(stock.getItem().getIsAvailable())
                        .build())
                .toList();
    }

    // NHẬP KHO
    @Override
    @Transactional
    public void importStock(BranchStockRequest request) {
        BranchStock stock = branchStockRepository
                .findByBranch_BranchIdAndItem_ItemId(request.getBranchId(), request.getItemId())
                .orElse(null);

        // Kiểm tra xem món F&B này có đang bị Admin khóa không
        FnBItem itemToCheck = fnbItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món Bắp/Nước"));

        if (!itemToCheck.getIsAvailable()) {
            throw new RuntimeException("Lỗi: Món này đã bị Admin ngừng bán, không thể nhập thêm vào kho!");
        }

        //Món này đã có sẵn trong rạp
        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + request.getQuantityToAdd());
            stock.setLastUpdated(LocalDateTime.now());
            branchStockRepository.save(stock);
        }
        //Món này rạp chưa từng nhập bao giờ
        else {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

            BranchStock newStock = BranchStock.builder()
                    .branch(branch)
                    .item(itemToCheck)
                    .quantity(request.getQuantityToAdd())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            branchStockRepository.save(newStock);
        }
    }

    // KIỂM KÊ KHO
    @Override
    @Transactional
    public void updateStock(Long id, Integer newQuantity) {
        BranchStock stock = branchStockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi tồn kho"));

        stock.setQuantity(newQuantity);
        stock.setLastUpdated(LocalDateTime.now());
        branchStockRepository.save(stock); // Lưu thẳng số mới, xóa sổ con số cũ
    }

    // LẤY ID RẠP TỪ QUẢN LÝ
    @Override
    @Transactional(readOnly = true)
    public Long getCurrentManagerBranchId(String username) {
        Account acc = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Staff manager = staffRepository.findById(acc.getAccountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        return manager.getBranch().getBranchId();
    }

    // LẤY MENU TỔNG
    @Override
    @Transactional(readOnly = true)
    public List<FnBItem> getAvailableFnBItems() {
        return fnbItemRepository.findAll().stream()
                .filter(FnBItem::getIsAvailable)
                .toList();
    }

    // SỔ TRA CỨU SỐ LƯỢNG (Dùng cho POS)
    @Override
    public Map<Long, Integer> getStockQuantityMap(Long branchId) {
        Map<Long, Integer> map = new HashMap<>();

        branchStockRepository.findByBranch_BranchId(branchId).forEach(stock -> {
            map.put(stock.getItem().getItemId(), stock.getQuantity() != null ? stock.getQuantity() : 0);
        });

        return map;
    }

    // 7. LẤY MENU POS CHO RẠP
    @Override
    public List<FnBItemResponse> getAvailableFnBItemsForBranch(Long branchId) {
        // Lấy danh sách Mã số (ID) của những món đang nằm trong kho rạp
        List<Long> availableItemIds = branchStockRepository.findByBranch_BranchId(branchId).stream()
                .map(stock -> stock.getItem().getItemId())
                .toList();

        //  Menu tổng
        return fnbItemRepository.findAll().stream()
                .filter(FnBItem::getIsAvailable)
                .filter(item -> availableItemIds.contains(item.getItemId()))
                .map(item -> FnBItemResponse.builder()
                        .id(item.getItemId())
                        .name(item.getName())
                        .category(item.getCategory())
                        .price(item.getPrice())
                        .imageUrl(item.getImageUrl())
                        .isAvailable(item.getIsAvailable())
                        .build())
                .toList();
    }
}
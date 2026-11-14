package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.entity.*;
import fu.cinema.repository.*;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchStockServiceImpl implements BranchStockService {

    private final BranchStockRepository branchStockRepository;
    private final BranchRepository branchRepository;
    private final FnBItemRepository fnbItemRepository;
    private final AccountRepository accountRepository;
    private final StaffRepository staffRepository;

    // Lấy danh sách tồn kho của rạp, chỉ hiển thị các món đang được phép bán (isAvailable = true)
    @Override
    public List<BranchStockResponse> getStockByBranch(Long branchId) {
        List<BranchStock> stocks = branchStockRepository.findByBranch_BranchId(branchId);

        return stocks.stream()
                .filter(stock -> stock.getItem().getIsAvailable()) // Bỏ qua các món Admin đã ngừng bán
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

    // Nhập thêm hàng vào kho (Cộng dồn nếu món đã có sẵn, hoặc tạo mới nếu chưa có)
    @Override
    @Transactional
    public void importStock(BranchStockRequest request) {
        BranchStock stock = branchStockRepository
                .findByBranch_BranchIdAndItem_ItemId(request.getBranchId(), request.getItemId())
                .orElse(null);

        //Đảm bảo món F&B vẫn đang được Admin cho phép bán
        FnBItem itemToCheck = fnbItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món Bắp/Nước"));

        if (!itemToCheck.getIsAvailable()) {
            throw new RuntimeException("Lỗi: Món này đã bị Admin ngừng bán, không thể nhập thêm vào kho!");
        }

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + request.getQuantityToAdd());
            stock.setLastUpdated(LocalDateTime.now());
            branchStockRepository.save(stock);
        }
        //Tạo bản ghi tồn kho mới khi chưa có món ăn
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

    // Ghi đè số lượng tồn kho thực tế
    @Override
    @Transactional
    public void updateStock(Long id, Integer newQuantity) {
        BranchStock stock = branchStockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi tồn kho"));

        stock.setQuantity(newQuantity);
        stock.setLastUpdated(LocalDateTime.now());
        branchStockRepository.save(stock);
    }

    // Tìm ID chi nhánh dựa vào username của Manager đang đăng nhập
    @Override
    @Transactional(readOnly = true)
    public Long getCurrentManagerBranchId(String username) {
        Account acc = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Staff manager = staffRepository.findById(acc.getAccountId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        return manager.getBranch().getBranchId();
    }

    // Lấy danh sách tất cả các món F&B đang được phép bán
    @Override
    @Transactional(readOnly = true)
    public List<FnBItem> getAvailableFnBItems() {
        return fnbItemRepository.findAll().stream()
                .filter(FnBItem::getIsAvailable)
                .toList();
    }

    // Xóa hoàn toàn một bản ghi tồn kho khỏi chi nhánh
    @Override
    @Transactional
    public void deleteStock(Long id) {
        branchStockRepository.deleteById(id);
    }
}
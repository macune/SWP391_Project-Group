package fu.cinema.service.impl;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.entity.Branch;
import fu.cinema.entity.BranchStock;
import fu.cinema.entity.FnBItem;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.BranchStockRepository;
import fu.cinema.repository.FnBItemRepository;
import fu.cinema.service.BranchStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchStockServiceImpl implements BranchStockService {

    private final BranchStockRepository branchStockRepository;
    private final BranchRepository branchRepository;
    private final FnBItemRepository fnbItemRepository;

    @Override
    public List<BranchStockResponse> getStockByBranch(Long branchId) {
        List<BranchStock> stocks = branchStockRepository.findByBranch_BranchId(branchId);

        return stocks.stream().map(stock -> BranchStockResponse.builder()
                .id(stock.getId())
                .branchId(stock.getBranch().getBranchId())
                .branchName(stock.getBranch().getBranchName())
                .itemId(stock.getItem().getItemId()) // Sửa lại thành getItem()
                .itemName(stock.getItem().getName())
                .itemCategory(stock.getItem().getCategory().name())
                .quantity(stock.getQuantity())
                .lastUpdated(stock.getLastUpdated())
                .price(stock.getItem().getPrice())
                .imageUrl(stock.getItem().getImageUrl())
                .isAvailable(stock.getItem().getIsAvailable())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void importStock(BranchStockRequest request) {
        BranchStock stock = branchStockRepository
                .findByBranch_BranchIdAndItem_ItemId(request.getBranchId(), request.getItemId())
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + request.getQuantityToAdd());
            stock.setLastUpdated(LocalDateTime.now());
            branchStockRepository.save(stock);
        } else {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
            FnBItem item = fnbItemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món Bắp/Nước"));

            BranchStock newStock = BranchStock.builder()
                    .branch(branch)
                    .item(item) // Sửa lại thành .item()
                    .quantity(request.getQuantityToAdd())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            branchStockRepository.save(newStock);
        }
    }

    @Override
    @Transactional
    public void updateStock(Long id, Integer newQuantity) {
        BranchStock stock = branchStockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi tồn kho"));
        stock.setQuantity(newQuantity);
        stock.setLastUpdated(LocalDateTime.now());
        branchStockRepository.save(stock);
    }

    @Override
    @Transactional
    public void deleteStock(Long id) {
        branchStockRepository.deleteById(id);
    }
}
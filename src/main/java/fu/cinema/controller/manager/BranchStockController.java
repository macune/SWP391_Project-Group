package fu.cinema.controller.manager;


import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.service.BranchStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class BranchStockController {

    private final BranchStockService branchStockService;

    //API lấy danh sách kho theo Chi nhánh
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')") // Chỉ Manager hoặc Admin mới được xem
    public ResponseEntity<?> getStockByBranch(@PathVariable Long branchId) {
        try {
            return ResponseEntity.ok(branchStockService.getStockByBranch(branchId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //API nhập hàng vào kho
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')") // Chỉ Manager hoặc Admin mới được nhập hàng
    public ResponseEntity<?> importStock(@Valid @RequestBody BranchStockRequest request) {
        try {
            branchStockService.importStock(request);
            return ResponseEntity.ok("Nhập kho thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi nhập kho: " + e.getMessage());
        }
    }
}

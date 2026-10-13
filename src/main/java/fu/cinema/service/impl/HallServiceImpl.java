package fu.cinema.service.impl;

import fu.cinema.dto.request.HallRequest;
import fu.cinema.dto.request.SeatUpdateItem;
import fu.cinema.dto.response.HallResponse;
import fu.cinema.dto.response.SeatResponse;
import fu.cinema.entity.Branch;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Seat;
import fu.cinema.enums.HallStatus;
import fu.cinema.enums.SeatStatus;
import fu.cinema.enums.SeatType;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.HallRepository;
import fu.cinema.repository.SeatRepository;
import fu.cinema.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HallServiceImpl implements HallService {

    private final HallRepository hallRepository;
    private final BranchRepository branchRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public Branch getBranchById(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseGet(() -> branchRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Chưa có chi nhánh nào trong hệ thống!")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HallResponse> getHallsByBranch(Long branchId) {
        return hallRepository.findByBranchBranchId(branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HallResponse getHallById(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu #" + hallId));
        return toResponse(hall);
    }

    @Override
    public HallResponse createHallWithSeats(Long branchId, HallRequest request) {
        Branch branch = getBranchById(branchId);
        String cleanName = request.getHallName().trim();

        if (hallRepository.existsByBranchBranchIdAndHallName(branch.getBranchId(), cleanName)) {
            throw new IllegalArgumentException("Phòng chiếu '" + cleanName + "' đã tồn tại trong chi nhánh này!");
        }

        int rows = request.getTotalRows();
        int cols = request.getSeatsPerRow();
        int totalCapacity = rows * cols;

        Hall hall = Hall.builder()
                .branch(branch)
                .hallName(cleanName)
                .capacity(totalCapacity)
                .status(request.getStatus() != null ? request.getStatus() : HallStatus.ACTIVE)
                .build();

        Hall savedHall = hallRepository.save(hall);

        // Tự động sinh ma trận ghế mặc định (100% STANDARD & ACTIVE) cho phòng mới tạo
        generateStandardSeats(savedHall, rows, cols);

        return toResponse(savedHall);
    }

    @Override
    public HallResponse updateHall(Long branchId, HallRequest request) {
        Hall existing = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu cần sửa!"));

        String cleanName = request.getHallName().trim();
        if (!existing.getHallName().equalsIgnoreCase(cleanName)
                && hallRepository.existsByBranchBranchIdAndHallName(branchId, cleanName)) {
            throw new IllegalArgumentException("Tên phòng chiếu '" + cleanName + "' đã bị trùng trong chi nhánh!");
        }

        existing.setHallName(cleanName);
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        // Cập nhật ma trận ghế THÔNG MINH khi thay đổi số hàng / số ghế trên hàng
        if (request.getTotalRows() != null && request.getSeatsPerRow() != null) {
            int newRows = request.getTotalRows();
            int newCols = request.getSeatsPerRow();

            adjustSeatsMatrix(existing, newRows, newCols);
        }

        return toResponse(hallRepository.save(existing));
    }

    /**
     * Thuật toán điều chỉnh ma trận ghế thông minh:
     * 1. Ghế cũ nằm trong phạm vi (newRows x newCols) -> Giữ nguyên loại ghế (VIP, SWEETBOX, BROKEN...) & trạng thái.
     * 2. Vị trí mới mở rộng -> Tạo ghế mới (loại mặc định STANDARD & ACTIVE).
     * 3. Ghế cũ vượt quá kích thước mới -> Xóa bỏ.
     */
    private void adjustSeatsMatrix(Hall hall, int newRows, int newCols) {
        List<Seat> existingSeats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hall.getHallId());

        // Đánh chỉ mục ghế hiện có theo tọa độ "HÀNG_SỐGHẾ" (Ví dụ: "A_1", "B_5")
        Map<String, Seat> seatMap = existingSeats.stream()
                .collect(Collectors.toMap(
                        s -> s.getRowCode() + "_" + s.getNumber(),
                        Function.identity()
                ));

        List<Seat> seatsToSave = new ArrayList<>();
        List<Seat> seatsToDelete = new ArrayList<>();

        // Duyệt ma trận theo kích thước mới
        for (int r = 0; r < newRows; r++) {
            String rowCode = String.valueOf((char) ('A' + r));
            for (int num = 1; num <= newCols; num++) {
                String key = rowCode + "_" + num;
                if (seatMap.containsKey(key)) {
                    // Đã có ghế cũ ở vị trí này -> Giữ nguyên cài đặt (Bảo lưu VIP, BROKEN...)
                    seatsToSave.add(seatMap.get(key));
                    seatMap.remove(key); // Đánh dấu đã giữ lại
                } else {
                    // Vị trí mở rộng chưa có ghế -> Sinh ghế mới mặc định STANDARD
                    Seat newSeat = Seat.builder()
                            .hall(hall)
                            .rowCode(rowCode)
                            .number(num)
                            .type(SeatType.STANDARD)
                            .status(SeatStatus.ACTIVE)
                            .build();
                    seatsToSave.add(newSeat);
                }
            }
        }

        // Các ghế còn lại trong seatMap là ghế dôi ra ngoài kích thước mới -> Xóa bớt
        seatsToDelete.addAll(seatMap.values());

        if (!seatsToDelete.isEmpty()) {
            seatRepository.deleteAll(seatsToDelete);
            seatRepository.flush();
        }

        if (!seatsToSave.isEmpty()) {
            seatRepository.saveAll(seatsToSave);
        }

        // Cập nhật lại sức chứa phòng chiếu dựa trên số ghế ACTIVE
        int activeSeats = (int) seatsToSave.stream()
                .filter(s -> s.getStatus() == SeatStatus.ACTIVE)
                .count();
        hall.setCapacity(activeSeats);
    }

    @Override
    public void deleteHall(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu để xóa!"));

        if (!hall.getShowtimes().isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa phòng chiếu đang chứa suất chiếu! Vui lòng hủy các suất chiếu trước.");
        }

        seatRepository.deleteByHallHallId(hallId);
        hallRepository.deleteById(hallId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<SeatResponse>> getSeatMapByHall(Long hallId) {
        List<Seat> seats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hallId);
        Map<String, List<SeatResponse>> seatMap = new LinkedHashMap<>();

        for (Seat seat : seats) {
            SeatResponse dto = SeatResponse.builder()
                    .seatId(seat.getSeatId())
                    .rowCode(seat.getRowCode())
                    .number(seat.getNumber())
                    .type(seat.getType())
                    .status(seat.getStatus())
                    .build();
            seatMap.computeIfAbsent(seat.getRowCode(), k -> new ArrayList<>()).add(dto);
        }
        return seatMap;
    }

    @Override
    public void updateSeatMatrix(Long hallId, List<SeatUpdateItem> seatUpdates) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu!"));

        List<Seat> seats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hallId);
        Map<Long, Seat> seatById = seats.stream()
                .collect(Collectors.toMap(Seat::getSeatId, Function.identity()));

        for (SeatUpdateItem item : seatUpdates) {
            Seat seat = seatById.get(item.getSeatId());
            if (seat != null) {
                if (item.getType() != null) {
                    seat.setType(item.getType());
                }
                if (item.getStatus() != null) {
                    seat.setStatus(item.getStatus());
                }
            }
        }

        seatRepository.saveAll(seats);

        int activeSeats = (int) seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.ACTIVE)
                .count();
        hall.setCapacity(activeSeats);
        hallRepository.save(hall);
    }

    private void generateStandardSeats(Hall hall, int totalRows, int seatsPerRow) {
        List<Seat> newSeats = new ArrayList<>(totalRows * seatsPerRow);
        for (int r = 0; r < totalRows; r++) {
            String rowCode = String.valueOf((char) ('A' + r));
            for (int num = 1; num <= seatsPerRow; num++) {
                Seat seat = Seat.builder()
                        .hall(hall)
                        .rowCode(rowCode)
                        .number(num)
                        .type(SeatType.STANDARD)
                        .status(SeatStatus.ACTIVE)
                        .build();
                newSeats.add(seat);
            }
        }
        seatRepository.saveAll(newSeats);
    }

    private HallResponse toResponse(Hall hall) {
        List<Seat> seats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hall.getHallId());
        int totalRows = (int) seats.stream().map(Seat::getRowCode).distinct().count();
        int seatsPerRow = seats.stream().mapToInt(Seat::getNumber).max().orElse(0);
        int activeCount = (int) seats.stream().filter(s -> s.getStatus() == SeatStatus.ACTIVE).count();

        return HallResponse.builder()
                .hallId(hall.getHallId())
                .branchId(hall.getBranch() != null ? hall.getBranch().getBranchId() : null)
                .branchName(hall.getBranch() != null ? hall.getBranch().getBranchName() : "CineFlow")
                .hallName(hall.getHallName())
                .capacity(hall.getCapacity())
                .totalRows(totalRows)
                .seatsPerRow(seatsPerRow)
                .activeSeatCount(activeCount)
                .status(hall.getStatus())
                .build();
    }
}
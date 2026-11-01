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
import fu.cinema.exception.AppException;
import fu.cinema.repository.BranchRepository;
import fu.cinema.repository.HallRepository;
import fu.cinema.repository.SeatRepository;
import fu.cinema.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
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

    private void adjustSeatsMatrix(Hall hall, int newRows, int newCols) {
        List<Seat> existingSeats = seatRepository.findByHallHallIdOrderByRowCodeAscNumberAsc(hall.getHallId());

        Map<String, Seat> seatMap = existingSeats.stream()
                .collect(Collectors.toMap(
                        s -> s.getRowCode() + "_" + s.getNumber(),
                        Function.identity()
                ));

        List<Seat> seatsToSave = new ArrayList<>();
        List<Seat> seatsToDelete = new ArrayList<>();

        for (int r = 0; r < newRows; r++) {
            String rowCode = String.valueOf((char) ('A' + r));
            for (int num = 1; num <= newCols; num++) {
                String key = rowCode + "_" + num;
                if (seatMap.containsKey(key)) {
                    seatsToSave.add(seatMap.get(key));
                    seatMap.remove(key);
                } else {
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

        seatsToDelete.addAll(seatMap.values());

        if (!seatsToDelete.isEmpty()) {
            seatRepository.deleteAll(seatsToDelete);
            seatRepository.flush();
        }

        if (!seatsToSave.isEmpty()) {
            seatRepository.saveAll(seatsToSave);
        }

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

        // Tạm gán dữ liệu mới vào List để kiểm tra logic trước khi lưu
        List<Seat> updatedSeats = new ArrayList<>();

        for (SeatUpdateItem item : seatUpdates) {
            Seat seat = seatById.get(item.getSeatId());
            if (seat != null) {
                if (item.getType() != null) {
                    seat.setType(item.getType());
                }
                if (item.getStatus() != null) {
                    seat.setStatus(item.getStatus());
                }
                updatedSeats.add(seat);
            }
        }

        // ====================================================================
        // CHỐT CHẶN BACKEND: KIỂM TRA SƠ ĐỒ GHẾ ĐÔI LÚC TẠO/CẬP NHẬT
        // ====================================================================
        List<Seat> sweetboxes = updatedSeats.stream()
                .filter(s -> s.getType() == SeatType.SWEETBOX && s.getStatus() != SeatStatus.BROKEN)
                .toList();

        Map<String, List<Seat>> sweetboxByRow = sweetboxes.stream()
                .collect(Collectors.groupingBy(Seat::getRowCode));

        for (Map.Entry<String, List<Seat>> entry : sweetboxByRow.entrySet()) {
            String rowCode = entry.getKey();
            List<Seat> rowSeats = entry.getValue();

            // Ràng buộc A: Tổng số ghế Sweetbox trong 1 hàng phải là số CHẴN
            if (rowSeats.size() % 2 != 0) {
                throw new AppException("Cấu hình lỗi: Hàng " + rowCode + " đang có " + rowSeats.size() + " ghế đôi. Bắt buộc phải là số chẵn để tạo thành các cặp!");
            }

            // Ràng buộc B: 2 ghế tạo thành 1 cặp phải LIỀN KỀ NHAU (VD: H1-H2 là đúng, H1-H3 là sai)
            rowSeats.sort(Comparator.comparing(Seat::getNumber));

            for (int i = 0; i < rowSeats.size(); i += 2) {
                if (rowSeats.get(i + 1).getNumber() - rowSeats.get(i).getNumber() != 1) {
                    throw new AppException("Cấu hình lỗi: Tại hàng " + rowCode + ", ghế số "
                            + rowSeats.get(i).getNumber() + " và " + rowSeats.get(i + 1).getNumber()
                            + " được gán là ghế đôi nhưng không nằm liền kề nhau!");
                }
            }
        }
        // ====================================================================

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
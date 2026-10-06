// Logic JS riêng cho Actor: BRANCH MANAGER (Cấu hình sơ đồ ghế, xếp lịch chiếu...)
/* =========================================================
   1. LOGIC TRANG DANH SÁCH PHÒNG CHIẾU (hall-list.html)
   ========================================================= */
function calcPreviewCapacity(prefix) {
    const rowsInput = document.getElementById(prefix + 'Rows');
    const colsInput = document.getElementById(prefix + 'Cols');
    const previewEl = document.getElementById(prefix + 'CapacityPreview');

    if (!rowsInput || !colsInput || !previewEl) return;

    const rows = parseInt(rowsInput.value) || 0;
    const cols = parseInt(colsInput.value) || 0;
    previewEl.innerText = (rows * cols) + ' ghế';
}

function openEditHallModal(btn) {
    document.getElementById('editHallId').value = btn.getAttribute('data-id');
    document.getElementById('editHallName').value = btn.getAttribute('data-name');
    document.getElementById('editRows').value = btn.getAttribute('data-rows');
    document.getElementById('editCols').value = btn.getAttribute('data-cols');
    document.getElementById('editStatus').value = btn.getAttribute('data-status');
    calcPreviewCapacity('edit');

    const modal = new bootstrap.Modal(document.getElementById('editHallModal'));
    modal.show();
}

/* =========================================================
   2. LOGIC TRANG THIẾT KẾ SƠ ĐỒ GHẾ (hall-seats.html)
   ========================================================= */
let currentBrush = 'STANDARD';

function selectBrush(brush, btnElement) {
    currentBrush = brush;
    document.querySelectorAll('.brush-btn').forEach(b => b.classList.remove('active'));
    btnElement.classList.add('active');
}

function applyBrushToSeat(seatEl) {
    if (currentBrush === 'BROKEN') {
        const currentStatus = seatEl.getAttribute('data-status');
        const newStatus = currentStatus === 'BROKEN' ? 'ACTIVE' : 'BROKEN';
        seatEl.setAttribute('data-status', newStatus);
    } else {
        seatEl.setAttribute('data-type', currentBrush);
        seatEl.setAttribute('data-status', 'ACTIVE');
    }
    renderSeatVisual(seatEl);
    updateSeatCounters();
}

function applyBrushToRowBtn(btnEl) {
    const rowCode = btnEl.getAttribute('data-row-code');
    const seatsInRow = document.querySelectorAll(`.seat-box[data-row="${rowCode}"]`);
    seatsInRow.forEach(seatEl => {
        if (currentBrush === 'BROKEN') {
            seatEl.setAttribute('data-status', 'BROKEN');
        } else {
            seatEl.setAttribute('data-type', currentBrush);
            seatEl.setAttribute('data-status', 'ACTIVE');
        }
        renderSeatVisual(seatEl);
    });
    updateSeatCounters();
}

function resetAllToStandard() {
    document.querySelectorAll('.seat-box').forEach(seatEl => {
        seatEl.setAttribute('data-type', 'STANDARD');
        seatEl.setAttribute('data-status', 'ACTIVE');
        renderSeatVisual(seatEl);
    });
    updateSeatCounters();
}

function renderSeatVisual(seatEl) {
    const type = seatEl.getAttribute('data-type');
    const status = seatEl.getAttribute('data-status');
    seatEl.classList.remove('seat-STANDARD', 'seat-VIP', 'seat-SWEETBOX', 'seat-BROKEN');

    if (status === 'BROKEN') {
        seatEl.classList.add('seat-BROKEN');
    } else {
        seatEl.classList.add('seat-' + type);
    }
}

function updateSeatCounters() {
    const countStandardEl = document.getElementById('countStandard');
    if (!countStandardEl) return; // Không phải trang sơ đồ ghế thì bỏ qua

    let standard = 0, vip = 0, sweetbox = 0, broken = 0, active = 0;
    document.querySelectorAll('.seat-box').forEach(seatEl => {
        const type = seatEl.getAttribute('data-type');
        const status = seatEl.getAttribute('data-status');
        if (status === 'BROKEN') {
            broken++;
        } else {
            active++;
            if (type === 'STANDARD') standard++;
            else if (type === 'VIP') vip++;
            else if (type === 'SWEETBOX') sweetbox++;
        }
    });

    countStandardEl.innerText = standard;
    document.getElementById('countVip').innerText = vip;
    document.getElementById('countSweetbox').innerText = sweetbox;
    document.getElementById('countBroken').innerText = broken;
    document.getElementById('countActive').innerText = active;
}

function saveSeatConfiguration(btn) {
    const hallId = btn.getAttribute('data-hall-id');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Đang lưu...';

    const payload = [];
    document.querySelectorAll('.seat-box').forEach(seatEl => {
        payload.push({
            seatId: parseInt(seatEl.getAttribute('data-seat-id')),
            type: seatEl.getAttribute('data-type'),
            status: seatEl.getAttribute('data-status')
        });
    });

    fetch(`/manager/halls/${hallId}/seats/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(async res => {
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Lỗi khi lưu sơ đồ ghế');
            alert('✅ ' + data.message);
            window.location.href = '/manager/halls';
        })
        .catch(err => {
            alert('❌ ' + err.message);
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-floppy-disk me-1"></i> Lưu sơ đồ ghế';
        });
}

document.addEventListener('DOMContentLoaded', () => {
    updateSeatCounters();
});
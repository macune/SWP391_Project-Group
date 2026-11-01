// Logic JS riêng cho Actor: STAFF (Tính tổng tiền vé + bắp nước tại quầy POS, soát vé...)

// ---------------------------------------------------------
// MODULE: QUẢN LÝ GIỎ HÀNG (POS CART)
// ---------------------------------------------------------
let cart = []; // Mảng lưu trữ các món trong giỏ

// Hàm định dạng tiền VNĐ
const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
};

// Hàm thêm vào giỏ
function addToCart(button) {
    const id = button.getAttribute('data-id');
    const name = button.getAttribute('data-name');
    const price = parseFloat(button.getAttribute('data-price'));

    // Kiểm tra món đã có trong giỏ chưa
    const existingItem = cart.find(item => item.id === id);
    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({ id, name, price, quantity: 1 });
    }

    renderCart();
}

// Hàm tăng giảm số lượng
function updateQuantity(id, change) {
    const item = cart.find(item => item.id === id);
    if (item) {
        item.quantity += change;
        if (item.quantity <= 0) {
            cart = cart.filter(i => i.id !== id); // Xóa khỏi giỏ nếu số lượng về 0
        }
        renderCart();
    }
}

// Hàm vẽ lại giỏ hàng ra màn hình
function renderCart() {
    const cartContainer = document.getElementById('cart-items');
    const emptyMsg = document.getElementById('empty-cart-msg');
    const totalEl = document.getElementById('cart-total');
    const btnCheckout = document.getElementById('btn-checkout');

    // Reset container
    cartContainer.innerHTML = '';
    let total = 0;

    if (cart.length === 0) {
        emptyMsg.style.display = 'block';
        btnCheckout.disabled = true;
    } else {
        emptyMsg.style.display = 'none';
        btnCheckout.disabled = false;

        cart.forEach(item => {
            total += item.price * item.quantity;
            cartContainer.innerHTML += `
                <div class="d-flex justify-content-between align-items-center mb-2 p-2 border rounded bg-light">
                    <div style="flex: 1;">
                        <h6 class="mb-0 fs-6">${item.name}</h6>
                        <small class="text-danger fw-bold">${formatCurrency(item.price)}</small>
                    </div>
                    <div class="d-flex align-items-center">
                        <button class="btn btn-sm btn-outline-secondary px-2 py-0" onclick="updateQuantity('${item.id}', -1)">-</button>
                        <span class="mx-2 fw-bold">${item.quantity}</span>
                        <button class="btn btn-sm btn-outline-secondary px-2 py-0" onclick="updateQuantity('${item.id}', 1)">+</button>
                    </div>
                </div>
            `;
        });
    }

    totalEl.innerText = formatCurrency(total);
}

// ---------------------------------------------------------
// MODULE: SOÁT VÉ VÀO PHÒNG BẰNG CAMERA QR & IN VÉ (FE-06.2)
// ---------------------------------------------------------
let html5QrCodeScanner = null;
let isCameraRunning = false;
let isProcessingScan = false;
let currentValidatedTicket = null;

// Phát âm thanh bíp nhẹ khi Camera quét trúng mã QR
function playScanBeep(isSuccess = true) {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(isSuccess ? 880 : 300, ctx.currentTime);
        gain.gain.setValueAtTime(0.15, ctx.currentTime);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.15);
    } catch (e) {
        // Bỏ qua nếu trình duyệt chặn tự phát âm thanh
    }
}

// Khởi tạo danh sách Camera của thiết bị khi vào trang Soát vé
document.addEventListener('DOMContentLoaded', () => {
    const readerEl = document.getElementById('qr-reader');
    if (!readerEl || typeof Html5Qrcode === 'undefined') return;

    Html5Qrcode.getCameras().then(devices => {
        const cameraSelect = document.getElementById('camera-select');
        if (!cameraSelect) return;
        cameraSelect.innerHTML = '';

        if (devices && devices.length > 0) {
            devices.forEach((device, idx) => {
                const opt = document.createElement('option');
                opt.value = device.id;
                opt.text = device.label || `Camera thiết bị #${idx + 1}`;
                cameraSelect.appendChild(opt);
            });
        } else {
            const opt = document.createElement('option');
            opt.value = '';
            opt.text = 'Không tìm thấy camera trên thiết bị';
            cameraSelect.appendChild(opt);
        }
    }).catch(() => {
        const cameraSelect = document.getElementById('camera-select');
        if (cameraSelect) {
            cameraSelect.innerHTML = '<option value="">Vui lòng cấp quyền truy cập Camera trên trình duyệt</option>';
        }
    });
});

// Bật Camera để quét mã QR
function startQrCamera() {
    if (typeof Html5Qrcode === 'undefined') {
        alert('Không tải được thư viện quét QR. Vui lòng kiểm tra kết nối mạng.');
        return;
    }

    const placeholder = document.getElementById('camera-placeholder');
    const btnStart = document.getElementById('btn-start-camera');
    const btnStop = document.getElementById('btn-stop-camera');
    const cameraSelect = document.getElementById('camera-select');

    if (!html5QrCodeScanner) {
        html5QrCodeScanner = new Html5Qrcode('qr-reader');
    }

    const selectedCameraId = cameraSelect && cameraSelect.value
        ? cameraSelect.value
        : { facingMode: 'environment' };

    const config = {
        fps: 10,
        qrbox: { width: 230, height: 230 },
        aspectRatio: 1.333334
    };

    html5QrCodeScanner.start(
        selectedCameraId,
        config,
        (decodedText) => {
            if (isProcessingScan) return;
            isProcessingScan = true;
            playScanBeep(true);

            const manualInput = document.getElementById('manual-qr-input');
            if (manualInput) manualInput.value = decodedText;

            lookupTicketByQrCode(decodedText).finally(() => {
                setTimeout(() => { isProcessingScan = false; }, 2200);
            });
        },
        () => {} // Bỏ qua lỗi từng khung hình khi chưa đưa QR vào
    ).then(() => {
        isCameraRunning = true;
        if (placeholder) placeholder.classList.add('d-none');
        if (btnStart) btnStart.classList.add('d-none');
        if (btnStop) btnStop.classList.remove('d-none');
    }).catch(err => {
        alert('Không thể mở Camera: ' + err + '\nBạn có thể cấp quyền Camera trên thanh địa chỉ trình duyệt hoặc dùng tính năng Quét từ ảnh QR.');
    });
}

// Tắt Camera
function stopQrCamera() {
    if (html5QrCodeScanner && isCameraRunning) {
        html5QrCodeScanner.stop().then(() => {
            isCameraRunning = false;
            const placeholder = document.getElementById('camera-placeholder');
            const btnStart = document.getElementById('btn-start-camera');
            const btnStop = document.getElementById('btn-stop-camera');
            if (placeholder) placeholder.classList.remove('d-none');
            if (btnStart) btnStart.classList.remove('d-none');
            if (btnStop) btnStop.classList.add('d-none');
        }).catch(err => console.error(err));
    }
}

// Quét mã QR từ file ảnh tải lên
function scanQrFromImageFile(inputEl) {
    if (!inputEl.files || inputEl.files.length === 0) return;
    const imageFile = inputEl.files[0];

    const proceedScanFile = () => {
        if (!html5QrCodeScanner) {
            html5QrCodeScanner = new Html5Qrcode('qr-reader');
        }
        html5QrCodeScanner.scanFile(imageFile, true)
            .then(decodedText => {
                playScanBeep(true);
                const manualInput = document.getElementById('manual-qr-input');
                if (manualInput) manualInput.value = decodedText;
                lookupTicketByQrCode(decodedText);
            })
            .catch(() => {
                playScanBeep(false);
                alert('Không nhận diện được mã QR trong ảnh này. Vui lòng chọn ảnh mã QR rõ nét hơn.');
            })
            .finally(() => {
                inputEl.value = '';
            });
    };

    if (isCameraRunning && html5QrCodeScanner) {
        html5QrCodeScanner.stop().then(() => {
            isCameraRunning = false;
            document.getElementById('btn-start-camera')?.classList.remove('d-none');
            document.getElementById('btn-stop-camera')?.classList.add('d-none');
            proceedScanFile();
        });
    } else {
        proceedScanFile();
    }
}

// Xử lý khi nhân viên bấm nút Tra cứu thủ công
function handleManualQrSubmit(event) {
    event.preventDefault();
    const input = document.getElementById('manual-qr-input');
    if (!input || !input.value.trim()) {
        alert('Vui lòng nhập mã định danh QR của vé!');
        return;
    }
    lookupTicketByQrCode(input.value.trim());
}

// Gọi API kiểm tra mã QR và hiển thị thông tin vé
async function lookupTicketByQrCode(qrCode) {
    try {
        const response = await fetch(`/staff/api/tickets/validate?qrCode=${encodeURIComponent(qrCode)}`);
        const data = await response.json();
        renderTicketValidationResult(data);
    } catch (error) {
        console.error('Lỗi tra cứu vé:', error);
        alert('Có lỗi xảy ra khi kết nối máy chủ để kiểm tra vé.');
    }
}

// Gọi API xác nhận Check-in cho khách vào phòng chiếu
async function confirmCustomerCheckIn() {
    if (!currentValidatedTicket || !currentValidatedTicket.ticketId) return;

    const btnCheckIn = document.getElementById('btn-confirm-checkin');
    if (btnCheckIn) btnCheckIn.disabled = true;

    try {
        const response = await fetch(`/staff/api/tickets/${currentValidatedTicket.ticketId}/check-in`, {
            method: 'POST'
        });
        const data = await response.json();
        playScanBeep(response.ok);
        renderTicketValidationResult(data);
    } catch (error) {
        console.error('Lỗi xác nhận soát vé:', error);
        alert('Không thể xác nhận soát vé. Vui lòng thử lại.');
        if (btnCheckIn) btnCheckIn.disabled = false;
    }
}

// Định dạng ngày giờ kiểu Việt Nam
function formatDateTimeVN(isoStr, onlyTime = false, onlyDate = false) {
    if (!isoStr) return '--';
    const d = new Date(isoStr);
    const pad = (n) => String(n).padStart(2, '0');
    const timePart = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const datePart = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
    if (onlyTime) return timePart;
    if (onlyDate) return datePart;
    return `${timePart} - ${datePart}`;
}

// Hiển thị toàn bộ kết quả kiểm tra và chi tiết vé lên giao diện
function renderTicketValidationResult(data) {
    const emptyState = document.getElementById('ticket-empty-state');
    const resultContainer = document.getElementById('ticket-result-container');
    const alertBanner = document.getElementById('validation-alert-banner');
    const detailsCard = document.getElementById('ticket-details-card');
    const btnCheckIn = document.getElementById('btn-confirm-checkin');
    const btnPrint = document.getElementById('btn-print-ticket');

    if (emptyState) emptyState.classList.add('d-none');
    if (resultContainer) resultContainer.classList.remove('d-none');

    // Thiết lập màu sắc banner theo trạng thái xác thực
    let alertClass = 'alert-secondary';
    let iconHtml = '<i class="fa-solid fa-circle-info me-2"></i>';

    if (data.validationState === 'VALID' || data.validationState === 'CHECKED_IN_SUCCESS') {
        alertClass = 'alert-success';
        iconHtml = '<i class="fa-solid fa-circle-check fs-5 me-2"></i>';
    } else if (data.validationState === 'ALREADY_USED' || data.validationState === 'WRONG_BRANCH' || data.validationState === 'CANCELLED' || data.validationState === 'NOT_FOUND' || data.validationState === 'ERROR') {
        alertClass = 'alert-danger';
        iconHtml = '<i class="fa-solid fa-triangle-exclamation fs-5 me-2"></i>';
    } else if (data.validationState === 'UNPAID') {
        alertClass = 'alert-warning text-dark';
        iconHtml = '<i class="fa-solid fa-clock fs-5 me-2"></i>';
    }

    alertBanner.className = `alert ${alertClass} d-flex align-items-center fw-semibold shadow-sm mb-3`;
    alertBanner.innerHTML = `${iconHtml}<span>${data.message}</span>`;

    // Nếu mã QR không tồn tại trong hệ thống
    if (!data.ticket) {
        currentValidatedTicket = null;
        if (detailsCard) detailsCard.classList.add('d-none');
        return;
    }

    const t = data.ticket;
    currentValidatedTicket = t;
    if (detailsCard) detailsCard.classList.remove('d-none');

    // Gán dữ liệu vé lên màn hình Staff
    document.getElementById('res-ticket-id').innerText = `#${t.ticketId}`;
    document.getElementById('res-booking-id').innerText = `#${t.bookingId}`;
    document.getElementById('res-movie-title').innerText = t.movieTitle || '--';
    document.getElementById('res-age-rating').innerText = t.ageRating || 'P';
    document.getElementById('res-genre').innerText = t.genre || '--';
    document.getElementById('res-duration').innerText = (t.duration || '--') + ' phút';
    document.getElementById('res-branch-name').innerText = t.branchName || '--';
    document.getElementById('res-hall-name').innerText = t.hallName || '--';
    document.getElementById('res-show-date').innerText = formatDateTimeVN(t.startTime, false, true);
    document.getElementById('res-show-time').innerText = `${formatDateTimeVN(t.startTime, true, false)} - ${formatDateTimeVN(t.endTime, true, false)}`;
    document.getElementById('res-seat-label').innerText = t.seatLabel || '--';
    document.getElementById('res-seat-type').innerText = t.seatTypeDisplay || t.seatType || '--';
    document.getElementById('res-price').innerText = formatCurrency(t.price || 0);
    document.getElementById('res-customer').innerText = `${t.customerUsername || 'Khách lẻ'}${t.customerEmail ? ' (' + t.customerEmail + ')' : ''}`;
    document.getElementById('res-qr-code').innerText = t.qrCode || '--';
    document.getElementById('res-checkedin-at').innerText = t.checkedInAt ? formatDateTimeVN(t.checkedInAt) : 'Chưa soát vé';

    const posterEl = document.getElementById('res-poster');
    if (posterEl) {
        posterEl.src = t.posterUrl && t.posterUrl.trim() !== '' ? t.posterUrl : '/images/posters/default-poster.jpg';
    }

    // Nhãn trạng thái vé
    const statusBadge = document.getElementById('res-status-badge');
    if (t.status === 'BOOKED') {
        statusBadge.className = 'badge bg-success px-3 py-2';
        statusBadge.innerHTML = '<i class="fa-solid fa-check me-1"></i>CHƯA SỬ DỤNG (BOOKED)';
    } else if (t.status === 'CHECKED_IN') {
        statusBadge.className = 'badge bg-secondary px-3 py-2';
        statusBadge.innerHTML = '<i class="fa-solid fa-user-check me-1"></i>ĐÃ SOÁT VÉ (CHECKED_IN)';
    } else if (t.status === 'LOCKED') {
        statusBadge.className = 'badge bg-warning text-dark px-3 py-2';
        statusBadge.innerHTML = '<i class="fa-solid fa-lock me-1"></i>CHỜ THANH TOÁN (LOCKED)';
    } else {
        statusBadge.className = 'badge bg-danger px-3 py-2';
        statusBadge.innerHTML = '<i class="fa-solid fa-ban me-1"></i>ĐÃ HỦY (CANCELLED)';
    }

    // Bật/Tắt nút "Xác nhận Soát vé" và nút "In vé vật lý"
    if (btnCheckIn) {
        btnCheckIn.disabled = !data.validForCheckIn;
    }
    if (btnPrint) {
        // Cho phép in vé đối với vé hợp lệ (BOOKED) hoặc vừa/đã soát vé (CHECKED_IN)
        btnPrint.disabled = !(t.status === 'BOOKED' || t.status === 'CHECKED_IN');
    }

    // Đồng bộ dữ liệu sang khu vực phiếu in (#printable-ticket-stub)
    populatePrintableTicketStub(t);
}

// Điền thông tin vào mẫu phiếu in vé giấy tại rạp
function populatePrintableTicketStub(t) {
    document.getElementById('print-branch-name').innerText = t.branchName || 'CINEFLOW CINEMA';
    document.getElementById('print-branch-address').innerText = t.branchAddress || '';
    document.getElementById('print-movie-title').innerText = t.movieTitle || '';
    document.getElementById('print-age-duration').innerText = `Phân loại: ${t.ageRating || 'P'} | Thời lượng: ${t.duration || '--'} phút`;
    document.getElementById('print-show-date').innerText = formatDateTimeVN(t.startTime, false, true);
    document.getElementById('print-show-time').innerText = `${formatDateTimeVN(t.startTime, true, false)} - ${formatDateTimeVN(t.endTime, true, false)}`;
    document.getElementById('print-hall-name').innerText = t.hallName || '';
    document.getElementById('print-seat-label').innerText = `${t.seatLabel || ''} (${t.seatTypeDisplay || t.seatType || ''})`;
    document.getElementById('print-ticket-price').innerText = formatCurrency(t.price || 0);
    document.getElementById('print-ticket-id').innerText = `#${t.ticketId} (Đơn #${t.bookingId})`;
    document.getElementById('print-qr-code-text').innerText = t.qrCode || '';
    document.getElementById('print-issued-time').innerText = formatDateTimeVN(new Date().toISOString());

    const qrImg = document.getElementById('print-qr-img');
    if (qrImg && t.qrCodeBase64) {
        qrImg.src = t.qrCodeBase64;
        qrImg.style.display = 'inline-block';
    } else if (qrImg) {
        qrImg.style.display = 'none';
    }
}

// In vé vật lý tại quầy/cổng soát vé
function printPhysicalTicket() {
    if (!currentValidatedTicket) {
        alert('Chưa có thông tin vé để in!');
        return;
    }
    populatePrintableTicketStub(currentValidatedTicket);
    window.print();
}
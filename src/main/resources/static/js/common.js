document.addEventListener("DOMContentLoaded", function () {
    console.log("Cineplex Common JS Loaded.");
});

/**
 * Hàm dùng chung: Định dạng số thành tiền tệ VNĐ (VD: 85000 -> 85.000 ₫)
 */
function formatCurrencyVND(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function triggerPrintReceipt() {
    console.log("Đang chuẩn bị dữ liệu in...");

    let grandTotal = 0;

    // 1. Lấy tổng tiền vé (Lấy từ thẻ ẩn chứa dữ liệu)
    const rawTicketTotalEl = document.getElementById('raw-ticket-total');
    if (rawTicketTotalEl) {
        const ticketTotal = parseFloat(rawTicketTotalEl.innerText) || 0;
        grandTotal += ticketTotal;

        // Điền thành tiền vé lên giao diện in
        const printTicketTotalEl = document.getElementById('print-ticket-total');
        if (printTicketTotalEl && ticketTotal > 0) {
            printTicketTotalEl.innerText = new Intl.NumberFormat('vi-VN').format(ticketTotal) + ' ₫';
        }
    }

    // 2. Xử lý F&B (Tách chuỗi, điền cột và cộng tiền)
    const fnbRows = document.querySelectorAll('.print-fnb-row');
    fnbRows.forEach(row => {
        const nameCell = row.querySelector('.fnb-name-cell');
        const qtyCell = row.querySelector('.fnb-qty-cell');
        const priceCell = row.querySelector('.fnb-price-cell');

        if (nameCell && qtyCell && priceCell) {
            const rawText = nameCell.innerText.trim();
            let qty = "-";
            let name = rawText;
            let price = "-";

            const matchQty = rawText.match(/^(\d+)x\s*/i);
            if (matchQty) {
                qty = matchQty[1];
                name = rawText.replace(matchQty[0], '');
            }

            const matchPrice = name.match(/\(([\d.,]+[đ₫dD])\)$/i) || name.match(/\(([\d.,]+)\)$/i);
            if (matchPrice) {
                price = matchPrice[1];
                name = name.replace(matchPrice[0], '').trim();
            }

            nameCell.innerText = name;
            qtyCell.innerText = qty;

            if (price !== "-") {
                let numericPrice = parseFloat(price.replace(/[^0-9.-]+/g,""));
                if(!isNaN(numericPrice)){
                    priceCell.innerText = new Intl.NumberFormat('vi-VN').format(numericPrice) + ' ₫';
                    grandTotal += numericPrice; // CỘNG DỒN TIỀN BẮP NƯỚC
                }
            }
        }
    });

    // 3. Hiển thị Tổng thanh toán cuối cùng
    const finalTotalEl = document.getElementById('print-final-total');
    if (finalTotalEl) {
        finalTotalEl.innerText = new Intl.NumberFormat('vi-VN').format(grandTotal) + ' ₫';
        finalTotalEl.classList.remove('text-danger'); // Đổi màu cho giống Staff
    }

    // Gọi lệnh in
    window.print();
}
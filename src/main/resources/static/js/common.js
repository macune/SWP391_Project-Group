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
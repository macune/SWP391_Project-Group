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
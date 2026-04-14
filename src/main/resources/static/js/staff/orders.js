(() => {
    const money = new Intl.NumberFormat("vi-VN");

    const refs = {
        tabButtons: document.querySelectorAll(".pos-tab"),
        views: document.querySelectorAll(".pos-view"),
        productSearch: document.getElementById("productSearch"),
        categoryStrip: document.getElementById("categoryStrip"),
        productGrid: document.getElementById("productGrid"),
        productEmptyState: document.getElementById("productEmptyState"),
        cartItems: document.getElementById("cartItems"),
        cartSubtotal: document.getElementById("cartSubtotal"),
        cartTotal: document.getElementById("cartTotal"),
        cartPayload: document.getElementById("cartPayload"),
        orderAction: document.getElementById("orderAction"),
        orderForm: document.getElementById("orderForm"),
        saveOrderBtn: document.getElementById("saveOrderBtn"),
        checkoutBtn: document.getElementById("checkoutBtn"),
        clearCartBtn: document.getElementById("clearCartBtn"),
        toolbarClearCartBtn: document.getElementById("toolbarClearCartBtn"),
        focusCustomerBtn: document.getElementById("focusCustomerBtn"),
        printDraftBtn: document.getElementById("printDraftBtn"),
        customerId: document.getElementById("customerId"),
        clearCartModal: document.getElementById("clearCartModal"),
        closeClearModal: document.getElementById("closeClearModal"),
        confirmClearCart: document.getElementById("confirmClearCart"),
        invoiceModal: document.getElementById("invoiceModal"),
        closeInvoiceModal: document.getElementById("closeInvoiceModal"),
        invoiceOrderNumber: document.getElementById("invoiceOrderNumber"),
        invoiceCustomer: document.getElementById("invoiceCustomer"),
        invoiceLines: document.getElementById("invoiceLines"),
        invoiceTotal: document.getElementById("invoiceTotal"),
        voucherModal: document.getElementById("voucherModal"),
        voucherForm: document.getElementById("voucherForm"),
        voucherCode: document.getElementById("voucherCode"),
        closeVoucherModal: document.getElementById("closeVoucherModal"),
        cancelVoucherBtn: document.getElementById("cancelVoucherBtn"),
        availableVoucherList: document.getElementById("availableVoucherList"),
        modalVoucherList: document.getElementById("modalVoucherList"),
        appliedVoucherCodeInput: document.getElementById("appliedVoucherCodeInput"),
        appliedVoucherCodeLabel: document.getElementById("appliedVoucherCodeLabel"),
        toastStack: document.getElementById("toastStack"),
        checkoutLowStockToastPayload: document.getElementById("checkoutLowStockToastPayload")
    };

    if (!refs.orderForm) {
        return;
    }

    const cart = new Map();
    const productCards = Array.from(document.querySelectorAll("#productGrid .product-card[data-id]"));
    let selectedCategory = "";

    function formatMoney(value) {
        return money.format(Math.max(0, Number(value) || 0)) + " VND";
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function toast(message, isError = false, variant = "") {
        const item = document.createElement("div");
        item.className = "toast"
            + (isError ? " error" : "")
            + (variant ? " " + variant : "");
        item.textContent = message;
        refs.toastStack.appendChild(item);

        requestAnimationFrame(() => item.classList.add("show"));
        setTimeout(() => {
            item.classList.remove("show");
            setTimeout(() => item.remove(), 250);
        }, 2400);
    }

    function showCheckoutLowStockToasts() {
        if (!refs.checkoutLowStockToastPayload) {
            return;
        }
        const toastItems = Array.from(
            refs.checkoutLowStockToastPayload.querySelectorAll(".low-stock-toast-item[data-message]")
        );
        toastItems.slice(0, 3).forEach((node, index) => {
            const message = (node.dataset.message || "").trim();
            if (!message) {
                return;
            }
            setTimeout(() => {
                toast("Canh bao ton kho: " + message, false, "warning");
            }, index * 260);
        });
    }

    function currentCartTotal() {
        let total = 0;
        cart.forEach(item => {
            total += item.price * item.quantity;
        });
        return total;
    }

    let currentVoucher = null;
    let currentDiscount = 0;
    let availableVouchers = [];

    function updateCartDisplay() {
        const subtotal = currentCartTotal();
        const total = Math.max(0, subtotal - currentDiscount);
        
        if (refs.cartSubtotal) {
            refs.cartSubtotal.textContent = formatMoney(subtotal);
        }
        if (refs.cartTotal) {
            refs.cartTotal.textContent = formatMoney(total);
        }
        
        const discountDisplay = document.getElementById("discountDisplay");
        const displayDiscountAmount = document.getElementById("displayDiscountAmount");
        
        if (currentDiscount > 0) {
            if (discountDisplay) discountDisplay.style.display = "block";
            if (displayDiscountAmount) displayDiscountAmount.textContent = "-" + formatMoney(currentDiscount);
        } else {
            if (discountDisplay) discountDisplay.style.display = "none";
        }
    }

    function syncActionState() {
        const disabled = cart.size === 0;
        refs.saveOrderBtn.disabled = disabled;
        refs.checkoutBtn.disabled = disabled;
        refs.printDraftBtn.disabled = disabled;
        refs.clearCartBtn.disabled = disabled;
        if (refs.toolbarClearCartBtn) {
            refs.toolbarClearCartBtn.disabled = disabled;
        }
    }

    function updatePayload() {
        refs.cartPayload.value = JSON.stringify(Array.from(cart.values()).map(item => ({
            productId: item.id,
            quantity: item.quantity
        })));
    }

    function renderCart() {
        if (cart.size === 0) {
            refs.cartItems.innerHTML = '<p class="empty-row">Chưa có sản phẩm trong đơn hàng.</p>';
        } else {
            refs.cartItems.innerHTML = Array.from(cart.values()).map(item => `
                <article class="cart-item">
                    <div class="cart-item__row">
                        <span class="cart-item__name">${escapeHtml(item.name)}</span>
                        <button class="icon-btn" type="button" data-action="remove" data-id="${item.id}" aria-label="Xóa sản phẩm">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                    <p class="cart-item__meta">${escapeHtml(item.code)} · ${escapeHtml(item.category)}</p>
                    <div class="cart-item__row">
                        <div class="qty-control">
                            <button type="button" data-action="decrease" data-id="${item.id}">-</button>
                            <span>${item.quantity}</span>
                            <button type="button" data-action="increase" data-id="${item.id}">+</button>
                        </div>
                        <span class="cart-line-total">${formatMoney(item.price * item.quantity)}</span>
                    </div>
                </article>
            `).join("");
        }

        updateCartDisplay();
        updatePayload();
        syncActionState();
    }

    function buildCategoryPills() {
        const categorySet = new Set();
        productCards.forEach(card => {
            categorySet.add((card.dataset.category || "Khác").trim());
        });

        Array.from(categorySet)
            .sort((a, b) => a.localeCompare(b, "vi"))
            .forEach(category => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "category-pill";
                button.dataset.category = category.toLowerCase();
                button.textContent = category;
                refs.categoryStrip.appendChild(button);
            });
    }

    function applyFilter() {
        const keyword = refs.productSearch.value.trim().toLowerCase();
        const category = selectedCategory;
        let visibleCount = 0;

        productCards.forEach(card => {
            const name = (card.dataset.name || "").toLowerCase();
            const code = (card.dataset.code || "").toLowerCase();
            const type = (card.dataset.category || "").toLowerCase();
            const matchKeyword = !keyword || name.includes(keyword) || type.includes(keyword);
            const matchCode = !keyword || code.includes(keyword);
            const matchCategory = !category || type === category;
            const visible = (matchKeyword || matchCode) && matchCategory;
            card.classList.toggle("is-hidden", !visible);
            if (visible) {
                visibleCount += 1;
            }
        });

        refs.productEmptyState.classList.toggle("is-hidden", visibleCount !== 0);
    }

    function openModal(modal) {
        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
    }

    function closeModal(modal) {
        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
    }

    function renderInvoice(orderNumber, customerName, lines, totalValue) {
        refs.invoiceOrderNumber.textContent = orderNumber || "Tạm tính";
        refs.invoiceCustomer.textContent = customerName || "Khách lẻ";

        if (!lines || lines.length === 0) {
            refs.invoiceLines.innerHTML = '<p class="empty-row">Không có dữ liệu hóa đơn.</p>';
        } else {
            refs.invoiceLines.innerHTML = lines.map(line => {
                const qty = Number(line.quantity) || 0;
                const unitPrice = Number(line.unitPrice) || 0;
                const lineTotal = Number(line.lineTotal != null ? line.lineTotal : qty * unitPrice);
                return `
                    <div class="invoice-line-row">
                        <span>${escapeHtml(line.productName)} x${qty}</span>
                        <strong>${formatMoney(lineTotal)}</strong>
                    </div>
                `;
            }).join("");
        }
        refs.invoiceTotal.textContent = formatMoney(totalValue);
        openModal(refs.invoiceModal);
    }

    function createVoucherChip(voucher, options = {}) {
        const button = document.createElement("button");
        const infoParts = [];
        if (voucher.description) {
            infoParts.push(voucher.description);
        }
        if (voucher.startDate || voucher.endDate) {
            const start = voucher.startDate ? new Date(voucher.startDate).toLocaleString("vi-VN") : "không giới hạn";
            const end = voucher.endDate ? new Date(voucher.endDate).toLocaleString("vi-VN") : "không giới hạn";
            infoParts.push(`Hiệu lực: ${start} - ${end}`);
        }

        button.type = "button";
        button.textContent = `${voucher.code} (-${formatMoney(voucher.discountValue)})`;
        button.title = infoParts.join(" | ");
        button.style.border = "1px solid #cdd5df";
        button.style.borderRadius = "14px";
        button.style.background = "#fff";
        button.style.padding = "4px 10px";
        button.style.fontSize = "12px";
        button.style.cursor = "pointer";
        button.style.color = "#334155";

        if (options.isSelected) {
            button.style.borderColor = "#2563eb";
            button.style.background = "#e7f0ff";
            button.style.color = "#1d4ed8";
        }

        button.addEventListener("click", () => {
            if (typeof options.onClick === "function") {
                options.onClick(voucher);
            }
        });
        return button;
    }

    function renderAvailableVoucherLists() {
        const renderTo = (container, mode) => {
            if (!container) {
                return;
            }

            if (!availableVouchers.length) {
                container.innerHTML = '<span style="font-size: 12px; color: #999;">Không có voucher khả dụng.</span>';
                return;
            }

            container.innerHTML = "";
            availableVouchers.forEach(voucher => {
                const chip = createVoucherChip(voucher, {
                    isSelected: currentVoucher && currentVoucher.code === voucher.code,
                    onClick: selected => {
                        if (mode === "current") {
                            const input = document.getElementById("currentVoucherCode");
                            const applyBtn = document.getElementById("applyCurrentVoucherBtn");
                            if (input) {
                                input.value = selected.code;
                            }
                            if (applyBtn && !input.readOnly) {
                                applyBtn.click();
                            }
                        } else if (refs.voucherCode) {
                            refs.voucherCode.value = selected.code;
                            refs.voucherCode.focus();
                        }
                    }
                });
                container.appendChild(chip);
            });
        };

        renderTo(refs.availableVoucherList, "current");
        renderTo(refs.modalVoucherList, "modal");
    }

    function loadAvailableVouchers() {
        fetch("/api/vouchers/available")
            .then(response => response.json())
            .then(data => {
                if (!data || !data.success || !Array.isArray(data.vouchers)) {
                    availableVouchers = [];
                } else {
                    availableVouchers = data.vouchers
                        .filter(item => item && item.code)
                        .map(item => ({
                            code: item.code,
                            voucherId: item.voucherId,
                            discountValue: Number(item.discountValue || 0),
                            description: item.description || "",
                            startDate: item.startDate || null,
                            endDate: item.endDate || null
                        }))
                        .sort((a, b) => b.discountValue - a.discountValue);
                }
                renderAvailableVoucherLists();
            })
            .catch(() => {
                availableVouchers = [];
                renderAvailableVoucherLists();
            });
    }

    refs.tabButtons.forEach(button => {
        button.addEventListener("click", () => {
            refs.tabButtons.forEach(item => item.classList.remove("is-active"));
            refs.views.forEach(view => view.classList.remove("is-active"));
            button.classList.add("is-active");

            const target = document.getElementById(button.dataset.target);
            if (target) {
                target.classList.add("is-active");
            }
        });
    });

    refs.productSearch.addEventListener("input", applyFilter);

    refs.categoryStrip.addEventListener("click", event => {
        const button = event.target.closest(".category-pill");
        if (!button) {
            return;
        }
        selectedCategory = button.dataset.category || "";
        refs.categoryStrip.querySelectorAll(".category-pill").forEach(pill => pill.classList.remove("is-active"));
        button.classList.add("is-active");
        applyFilter();
    });

    refs.productGrid.addEventListener("click", event => {
        const button = event.target.closest(".js-add-product");
        if (!button) {
            return;
        }

        const card = button.closest(".product-card[data-id]");
        if (!card) {
            return;
        }

        const id = Number(card.dataset.id);
        const stock = Number(card.dataset.stock || 0);
        const current = cart.get(id);
        if (current && current.quantity >= stock) {
            toast("Số lượng vượt quá tồn kho hiện tại.", true);
            return;
        }

        if (stock <= 0) {
            toast("Sản phẩm đã hết tồn kho.", true);
            return;
        }

        if (current) {
            current.quantity += 1;
        } else {
            cart.set(id, {
                id,
                code: card.dataset.code || "SP",
                name: card.dataset.name || "Sản phẩm",
                category: card.dataset.category || "Khác",
                price: Number(card.dataset.price || 0),
                stock,
                quantity: 1
            });
        }
        renderCart();
        toast("Đã thêm sản phẩm vào đơn.");
    });

    refs.cartItems.addEventListener("click", event => {
        const button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }

        const id = Number(button.dataset.id);
        const action = button.dataset.action;
        const item = cart.get(id);
        if (!item) {
            return;
        }

        if (action === "increase") {
            if (item.quantity >= item.stock) {
                toast("Không thể vượt quá tồn kho.", true);
                return;
            }
            item.quantity += 1;
        }
        if (action === "decrease") {
            item.quantity -= 1;
            if (item.quantity <= 0) {
                cart.delete(id);
            }
        }
        if (action === "remove") {
            cart.delete(id);
        }

        renderCart();
    });

    refs.orderForm.addEventListener("submit", event => {
        if (cart.size === 0) {
            event.preventDefault();
            toast("Vui lòng thêm ít nhất một sản phẩm.", true);
            return;
        }

        const submitter = event.submitter;
        const action = submitter && submitter.dataset.orderAction ? submitter.dataset.orderAction : "checkout";
        refs.orderAction.value = action;
        if (refs.appliedVoucherCodeInput) {
            refs.appliedVoucherCodeInput.value = currentVoucher && currentVoucher.code ? currentVoucher.code : "";
        }
        updatePayload();

        if (action === "checkout") {
            const ok = confirm("Xác nhận thanh toán đơn hàng này?");
            if (!ok) {
                event.preventDefault();
            }
        }
    });

    function openClearModal() {
        if (cart.size === 0) {
            toast("Giỏ hàng đang trống.", true);
            return;
        }
        openModal(refs.clearCartModal);
    }

    refs.clearCartBtn.addEventListener("click", openClearModal);
    if (refs.toolbarClearCartBtn) {
        refs.toolbarClearCartBtn.addEventListener("click", openClearModal);
    }

    if (refs.focusCustomerBtn) {
        refs.focusCustomerBtn.addEventListener("click", () => {
            refs.customerId.focus();
        });
    }

    refs.closeClearModal.addEventListener("click", () => closeModal(refs.clearCartModal));
    refs.confirmClearCart.addEventListener("click", () => {
        cart.clear();
        currentVoucher = null;
        currentDiscount = 0;
        if (refs.appliedVoucherCodeInput) {
            refs.appliedVoucherCodeInput.value = "";
        }
        if (refs.appliedVoucherCodeLabel) {
            refs.appliedVoucherCodeLabel.textContent = "";
        }
        if (currentVoucherCodeInput) {
            currentVoucherCodeInput.readOnly = false;
            currentVoucherCodeInput.value = "";
        }
        const currentVoucherInfo = document.getElementById("currentVoucherInfo");
        if (currentVoucherInfo) {
            currentVoucherInfo.style.display = "none";
        }
        if (applyCurrentVoucherBtn) {
            applyCurrentVoucherBtn.style.display = "block";
        }
        if (removeCurrentVoucherBtn) {
            removeCurrentVoucherBtn.style.display = "none";
        }
        renderCart();
        closeModal(refs.clearCartModal);
        toast("Đã hủy đơn hiện tại.");
    });

    refs.printDraftBtn.addEventListener("click", () => {
        if (cart.size === 0) {
            toast("Chưa có dữ liệu để in hóa đơn.", true);
            return;
        }

        const customerOption = document.querySelector("#customerId option:checked");
        const customerName = customerOption && customerOption.value ? customerOption.textContent : "Khách lẻ";
        const lines = Array.from(cart.values()).map(item => ({
            productName: item.name,
            quantity: item.quantity,
            unitPrice: item.price,
            lineTotal: item.price * item.quantity
        }));
        renderInvoice("Tạm tính", customerName, lines, currentCartTotal());
    });

    document.querySelectorAll(".js-print-existing").forEach(button => {
        button.addEventListener("click", () => {
            const orderId = button.dataset.orderId;
            const orderNumber = button.dataset.orderNumber;
            const customerName = button.dataset.customer;
            const total = Number(button.dataset.total || 0);
            const script = document.getElementById("invoice-lines-" + orderId);

            let lines = [];
            if (script && script.textContent) {
                try {
                    lines = JSON.parse(script.textContent);
                } catch (error) {
                    lines = [];
                }
            }
            renderInvoice(orderNumber, customerName, lines, total);
        });
    });

    refs.closeInvoiceModal.addEventListener("click", () => closeModal(refs.invoiceModal));

    // Xử lý áp dụng voucher
    let currentOrderIdForVoucher = null;

    document.querySelectorAll(".js-apply-voucher").forEach(button => {
        button.addEventListener("click", () => {
            currentOrderIdForVoucher = button.dataset.orderId;
            refs.voucherCode.value = "";
            openModal(refs.voucherModal);
        });
    });

    if (refs.voucherForm) {
        refs.voucherForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const voucherCode = refs.voucherCode.value.trim();
            
            if (!voucherCode) {
                alert("Vui lòng nhập mã voucher.");
                return;
            }

            if (!currentOrderIdForVoucher) {
                alert("Không xác định được đơn hàng.");
                closeModal(refs.voucherModal);
                return;
            }

            const form = document.createElement("form");
            form.method = "POST";
            form.action = `/staff/orders/${currentOrderIdForVoucher}/apply-voucher`;
            
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = "voucherCode";
            input.value = voucherCode;
            
            form.appendChild(input);
            document.body.appendChild(form);
            form.submit();
        });
    }

    if (refs.closeVoucherModal) {
        refs.closeVoucherModal.addEventListener("click", () => closeModal(refs.voucherModal));
    }

    if (refs.cancelVoucherBtn) {
        refs.cancelVoucherBtn.addEventListener("click", () => closeModal(refs.voucherModal));
    }

    [refs.invoiceModal, refs.clearCartModal, refs.voucherModal].forEach(modal => {
        if (modal) {
            modal.addEventListener("click", event => {
                if (event.target === modal) {
                    closeModal(modal);
                }
            });
        }
    });

    // Xử lý áp dụng voucher cho đơn hiện tại
    const applyCurrentVoucherBtn = document.getElementById("applyCurrentVoucherBtn");
    const removeCurrentVoucherBtn = document.getElementById("removeCurrentVoucherBtn");
    const currentVoucherCodeInput = document.getElementById("currentVoucherCode");

    if (applyCurrentVoucherBtn) {
        applyCurrentVoucherBtn.addEventListener("click", () => {
            const voucherCode = currentVoucherCodeInput.value.trim();
            
            console.log("DEBUG: Applying voucher with code:", voucherCode);
            
            if (!voucherCode) {
                toast("Vui lòng nhập mã voucher.", true);
                return;
            }

            // Kiểm tra voucher hợp lệ
            const url = `/api/vouchers/check?code=${encodeURIComponent(voucherCode)}`;
            console.log("DEBUG: Fetching URL:", url);
            
            fetch(url)
                .then(response => {
                    console.log("DEBUG: Response status:", response.status);
                    return response.json();
                })
                .then(data => {
                    console.log("DEBUG: Response data:", data);
                    
                    // Kiểm tra xem API có trả về success không
                    if (!data.success || !data.voucherId) {
                        console.log("DEBUG: Voucher not valid - success:", data.success, "voucherId:", data.voucherId);
                        toast("Mã voucher không hợp lệ hoặc không tồn tại.", true);
                        currentVoucherCodeInput.value = "";
                        if (refs.appliedVoucherCodeInput) {
                            refs.appliedVoucherCodeInput.value = "";
                        }
                        if (refs.appliedVoucherCodeLabel) {
                            refs.appliedVoucherCodeLabel.textContent = "";
                        }
                        return;
                    }

                    // Voucher hợp lệ - áp dụng giảm giá
                    console.log("DEBUG: Voucher is valid, applying discount:", data.discountValue);
                    
                    currentVoucher = data;
                    currentDiscount = parseFloat(data.discountValue) || 0;
                    
                    console.log("DEBUG: Current discount set to:", currentDiscount);
                    
                    // Cập nhật giao diện
                    if (refs.appliedVoucherCodeLabel) {
                        refs.appliedVoucherCodeLabel.textContent = data.code || "";
                    }
                    document.getElementById("discountAmount").textContent = formatMoney(currentDiscount);
                    document.getElementById("currentVoucherInfo").style.display = "block";
                    
                    applyCurrentVoucherBtn.style.display = "none";
                    removeCurrentVoucherBtn.style.display = "block";
                    currentVoucherCodeInput.readOnly = true;
                    if (refs.appliedVoucherCodeInput) {
                        refs.appliedVoucherCodeInput.value = data.code || "";
                    }
                    
                    updateCartDisplay();
                    renderAvailableVoucherLists();
                    toast("✓ Áp dụng voucher thành công! Giảm " + formatMoney(currentDiscount));
                })
                .catch(error => {
                    console.error("DEBUG: Fetch error:", error);
                    toast("Lỗi khi kiểm tra voucher. Vui lòng thử lại.", true);
                    currentVoucherCodeInput.value = "";
                    if (refs.appliedVoucherCodeInput) {
                        refs.appliedVoucherCodeInput.value = "";
                    }
                    if (refs.appliedVoucherCodeLabel) {
                        refs.appliedVoucherCodeLabel.textContent = "";
                    }
                });
        });
    }

    if (removeCurrentVoucherBtn) {
        removeCurrentVoucherBtn.addEventListener("click", () => {
            currentVoucher = null;
            currentDiscount = 0;
            
            document.getElementById("currentVoucherInfo").style.display = "none";
            applyCurrentVoucherBtn.style.display = "block";
            removeCurrentVoucherBtn.style.display = "none";
            currentVoucherCodeInput.readOnly = false;
            currentVoucherCodeInput.value = "";
            if (refs.appliedVoucherCodeInput) {
                refs.appliedVoucherCodeInput.value = "";
            }
            if (refs.appliedVoucherCodeLabel) {
                refs.appliedVoucherCodeLabel.textContent = "";
            }
            
            updateCartDisplay();
            renderAvailableVoucherLists();
            toast("Đã hủy voucher.");
        });
    }

    loadAvailableVouchers();
    buildCategoryPills();
    applyFilter();
    renderCart();
    showCheckoutLowStockToasts();
 })();

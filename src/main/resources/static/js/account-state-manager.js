/**
 * State Pattern - Account State Management
 * Xử lý các hành động quản lý trạng thái tài khoản (ACTIVE, SUSPENDED, LOCKED, INACTIVE)
 */

class AccountStateManager {
    constructor() {
        this.apiBaseUrl = '/api/accounts';
        this.init();
    }

    init() {
        this.attachStateActionListeners();
        console.log('✨ AccountStateManager initialized');
    }

    /**
     * Gán sự kiện cho các nút quản lý state
     */
    attachStateActionListeners() {
        document.querySelectorAll('.state-action-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const action = btn.getAttribute('data-action');
                const accountId = btn.closest('.state-actions').getAttribute('data-account-id');
                
                this.showStateActionModal(action, accountId);
            });
        });
    }

    /**
     * Hiển thị modal xác nhận hành động state
     */
    showStateActionModal(action, accountId) {
        const actionTexts = {
            suspend: {
                title: 'Tạm khóa tài khoản',
                message: 'Tài khoản sẽ không thể login hoặc truy cập hệ thống. Bạn có chắc muốn tiếp tục?',
                confirmText: 'Tạm khóa'
            },
            activate: {
                title: 'Kích hoạt tài khoản',
                message: 'Tài khoản sẽ được kích hoạt và có thể login bình thường.',
                confirmText: 'Kích hoạt'
            },
            lock: {
                title: 'Khóa tài khoản',
                message: 'Tài khoản sẽ bị khóa và không thể login. Bạn có chắc muốn tiếp tục?',
                confirmText: 'Khóa'
            }
        };

        const actionText = actionTexts[action];
        if (!actionText) return;

        const modalHtml = `
            <div class="state-action-modal" id="stateActionModal">
                <div class="state-action-modal-content">
                    <h3>${actionText.title}</h3>
                    <p>${actionText.message}</p>
                    <div class="modal-actions">
                        <button class="modal-actions-btn btn-confirm" 
                                onclick="accountStateManager.executeStateAction('${action}', ${accountId})">
                            ${actionText.confirmText}
                        </button>
                        <button class="modal-actions-btn btn-cancel" 
                                onclick="accountStateManager.closeStateActionModal()">
                            Hủy
                        </button>
                    </div>
                </div>
            </div>
        `;

        // Xóa modal cũ nếu có
        const oldModal = document.getElementById('stateActionModal');
        if (oldModal) oldModal.remove();

        // Thêm modal mới
        document.body.insertAdjacentHTML('beforeend', modalHtml);

        // Gán sự kiện đóng modal khi click ngoài
        document.getElementById('stateActionModal').addEventListener('click', (e) => {
            if (e.target.id === 'stateActionModal') {
                this.closeStateActionModal();
            }
        });
    }

    /**
     * Đóng modal
     */
    closeStateActionModal() {
        const modal = document.getElementById('stateActionModal');
        if (modal) {
            modal.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => modal.remove(), 300);
        }
    }

    /**
     * Thực hiện hành động state thông qua API
     */
    async executeStateAction(action, accountId) {
        const endpoint = `${this.apiBaseUrl}/${accountId}/${action}`;
        
        try {
            this.closeStateActionModal();
            this.showLoading(true, `Đang ${action === 'suspend' ? 'tạm khóa' : action === 'activate' ? 'kích hoạt' : 'khóa'} tài khoản...`);

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const data = await response.json();

            if (data.success) {
                this.showToast('success', data.message || `Hành động ${action} thành công!`);
                
                // Cập nhật UI sau 1 giây
                setTimeout(() => {
                    location.reload();
                }, 1500);
            } else {
                this.showToast('error', data.message || 'Có lỗi xảy ra!');
            }
        } catch (error) {
            console.error('Error executing state action:', error);
            this.showToast('error', 'Lỗi kết nối tới server. Vui lòng thử lại!');
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Lấy thông tin state của tài khoản
     */
    async getAccountState(accountId) {
        const endpoint = `${this.apiBaseUrl}/${accountId}/state`;
        
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const data = await response.json();
            
            if (data.success) {
                console.log('Account State Info:', data);
                return data;
            } else {
                console.error('Error:', data.message);
                return null;
            }
        } catch (error) {
            console.error('Error getting account state:', error);
            return null;
        }
    }

    /**
     * Hiển thị toast notification
     */
    showToast(type, message) {
        const toastContainer = document.querySelector('.toast-container') || this.createToastContainer();
        
        const toastHtml = `
            <div class="toast ${type}">
                <strong>${type === 'success' ? '✅' : type === 'error' ? '❌' : type === 'warning' ? '⚠️' : 'ℹ️'}</strong>
                ${message}
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toast = toastContainer.lastElementChild;
        setTimeout(() => {
            toast.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    /**
     * Tạo toast container
     */
    createToastContainer() {
        const container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
        return container;
    }

    /**
     * Hiển thị/ẩn loading
     */
    showLoading(show, message = 'Đang xử lý...') {
        let loadingDiv = document.getElementById('stateActionLoading');
        
        if (show) {
            if (!loadingDiv) {
                loadingDiv = document.createElement('div');
                loadingDiv.id = 'stateActionLoading';
                loadingDiv.className = 'state-action-loading';
                document.body.appendChild(loadingDiv);
            }
            
            loadingDiv.innerHTML = `
                <div class="loading-spinner">
                    <div class="spinner"></div>
                    <p>${message}</p>
                </div>
            `;
            loadingDiv.style.display = 'flex';
        } else {
            if (loadingDiv) {
                loadingDiv.style.display = 'none';
            }
        }
    }
}

// Khởi tạo AccountStateManager khi DOM sẵn sàng
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.accountStateManager = new AccountStateManager();
    });
} else {
    window.accountStateManager = new AccountStateManager();
}

// Export cho sử dụng toàn cục
window.AccountStateManager = AccountStateManager;

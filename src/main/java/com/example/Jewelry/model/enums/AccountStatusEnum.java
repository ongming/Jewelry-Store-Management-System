package com.example.Jewelry.model.enums;

/**
 * Enum định nghĩa các trạng thái của Account (Tài khoản) - 3 trạng thái chính
 * Sử dụng trong Staff Management thay vì String
 */
public enum AccountStatusEnum {

	/**
	 * ACTIVE - Tài khoản đang hoạt động bình thường - Có thể login - Có thể truy
	 * cập hệ thống - Có thể sửa đổi dữ liệu
	 */
	ACTIVE("Hoạt động", "✅", "Tài khoản đang hoạt động bình thường"),

	/**
	 * SUSPENDED - Tài khoản bị tạm khóa bởi admin - Không thể login - Không thể
	 * truy cập hệ thống - Không thể sửa đổi dữ liệu
	 */
	SUSPENDED("Tạm khóa", "⏸️", "Tài khoản bị tạm khóa bởi quản trị viên"),

	/**
	 * LOCKED - Tài khoản bị khóa do sai mật khẩu quá nhiều lần - Không thể login -
	 * Không thể truy cập hệ thống - Không thể sửa đổi dữ liệu
	 */
	LOCKED("Khóa", "🔒", "Tài khoản bị khóa do sai mật khẩu quá nhiều lần"),

	/**
	 * INACTIVE - Tài khoản không hoạt động (bị vô hiệu hóa) - Không thể login -
	 * Không thể truy cập hệ thống - Không thể sửa đổi dữ liệu
	 */
	INACTIVE("Không hoạt động", "❌", "Tài khoản không hoạt động");

	private final String displayName; // Tên hiển thị (Tiếng Việt)
	private final String icon; // Icon emoji
	private final String description; // Mô tả chi tiết

	/**
	 * Constructor
	 */
	AccountStatusEnum(String displayName, String icon, String description) {
		this.displayName = displayName;
		this.icon = icon;
		this.description = description;
	}

	/**
	 * Lấy tên hiển thị
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Lấy icon
	 */
	public String getIcon() {
		return icon;
	}

	/**
	 * Lấy mô tả
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Lấy enum từ string
	 */
	public static AccountStatusEnum fromString(String status) {
		if (status == null) {
			return ACTIVE;
		}
		try {
			return AccountStatusEnum.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			return ACTIVE;
		}
	}

	/**
	 * Kiểm tra xem có thể login hay không
	 */
	public boolean canLogin() {
		return this == ACTIVE;
	}

	/**
	 * Kiểm tra xem có thể truy cập hệ thống hay không
	 */
	public boolean canAccessSystem() {
		return this == ACTIVE;
	}

	/**
	 * Kiểm tra xem có thể sửa đổi dữ liệu hay không
	 */
	public boolean canModifyData() {
		return this == ACTIVE;
	}

	/**
	 * Lấy hiển thị dạng "ACTIVE" hoặc "SUSPENDED", v.v.
	 */
	@Override
	public String toString() {
		return this.name();
	}
}

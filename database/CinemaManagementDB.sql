-- =============================================
-- Database: CinemaManagementDB
-- Project: Cinema Management System (SWP391)
-- Standard: Aligned 100% with SDS Document (Shared PK)
-- DBMS: Microsoft SQL Server
-- =============================================

IF NOT EXISTS (SELECT name FROM master.dbo.sysdatabases WHERE name = N'CinemaManagementDB')
BEGIN
    CREATE DATABASE [CinemaManagementDB];
END
GO

USE [CinemaManagementDB];
GO

-- =============================================
-- XÓA BẢNG THEO THỨ TỰ TỪ CON ĐẾN CHA (TRÁNH LỖI KHÓA NGOẠI KHI CHẠY LẠI)
-- =============================================
IF OBJECT_ID('dbo.booking_fnb', 'U') IS NOT NULL DROP TABLE dbo.booking_fnb;
IF OBJECT_ID('dbo.fnb_items', 'U') IS NOT NULL DROP TABLE dbo.fnb_items;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.invoices', 'U') IS NOT NULL DROP TABLE dbo.invoices;
IF OBJECT_ID('dbo.tickets', 'U') IS NOT NULL DROP TABLE dbo.tickets;
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL DROP TABLE dbo.bookings;
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;
IF OBJECT_ID('dbo.showtimes', 'U') IS NOT NULL DROP TABLE dbo.showtimes;
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL DROP TABLE dbo.movies;
IF OBJECT_ID('dbo.seats', 'U') IS NOT NULL DROP TABLE dbo.seats;
IF OBJECT_ID('dbo.halls', 'U') IS NOT NULL DROP TABLE dbo.halls;
IF OBJECT_ID('dbo.staff_manager', 'U') IS NOT NULL DROP TABLE dbo.staff_manager;
IF OBJECT_ID('dbo.customers', 'U') IS NOT NULL DROP TABLE dbo.customers;
IF OBJECT_ID('dbo.branches', 'U') IS NOT NULL DROP TABLE dbo.branches;
IF OBJECT_ID('dbo.accounts', 'U') IS NOT NULL DROP TABLE dbo.accounts;
GO

-- =============================================
-- 3.1. ACCOUNT & USERS CLUSTER
-- =============================================

-- 1. Bảng ACCOUNT
CREATE TABLE dbo.accounts (
                              account_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              username VARCHAR(100) NOT NULL UNIQUE,
                              password_hash VARCHAR(255) NOT NULL,
                              email VARCHAR(255) NOT NULL UNIQUE,
                              role VARCHAR(50) NOT NULL, -- ENUM: ADMIN, MANAGER, STAFF, CUSTOMER
                              status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' -- ENUM: ACTIVE, INACTIVE, LOCKED
);
GO

-- 2. Bảng CUSTOMER (Shared Primary Key với ACCOUNT)
CREATE TABLE dbo.customers (
                               customer_id BIGINT PRIMARY KEY, -- Không dùng IDENTITY, lấy trực tiếp từ account_id
                               full_name NVARCHAR(255) NOT NULL,
                               phone VARCHAR(20) NOT NULL,
                               email_verified BIT NOT NULL DEFAULT 0, -- 0 = FALSE, 1 = TRUE
                               CONSTRAINT FK_Customers_Accounts FOREIGN KEY (customer_id) REFERENCES dbo.accounts(account_id) ON DELETE CASCADE
);
GO

-- =============================================
-- 3.2. INFRASTRUCTURE CLUSTER
-- =============================================

-- 3. Bảng BRANCH (Chi nhánh rạp - Tạo trước để STAFF_MANAGER tham chiếu)
CREATE TABLE dbo.branches (
                              branch_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              branch_name NVARCHAR(255) NOT NULL UNIQUE,
                              address NVARCHAR(500) NOT NULL,
                              hotline VARCHAR(20) NOT NULL,
                              opening_time TIME NOT NULL,
                              closing_time TIME NOT NULL,
                              status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' -- ENUM: ACTIVE, CLOSED, MAINTENANCE
);
GO

-- 4. Bảng STAFF_MANAGER (Shared Primary Key với ACCOUNT)
CREATE TABLE dbo.staff_manager (
                                   staff_id BIGINT PRIMARY KEY, -- Không dùng IDENTITY, lấy trực tiếp từ account_id
                                   branch_id BIGINT NULL, -- NULL cho Admin, có giá trị cho Manager/Staff chi nhánh
                                   full_name NVARCHAR(255) NOT NULL,
                                   email VARCHAR(255) NOT NULL UNIQUE,
                                   phone VARCHAR(20) NOT NULL,
                                   status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ENUM: ACTIVE, INACTIVE
                                   CONSTRAINT FK_StaffManager_Accounts FOREIGN KEY (staff_id) REFERENCES dbo.accounts(account_id) ON DELETE CASCADE,
                                   CONSTRAINT FK_StaffManager_Branches FOREIGN KEY (branch_id) REFERENCES dbo.branches(branch_id)
);
GO

-- 5. Bảng HALL (Phòng chiếu)
CREATE TABLE dbo.halls (
                           hall_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           branch_id BIGINT NOT NULL,
                           hall_name NVARCHAR(100) NOT NULL,
                           capacity INT NOT NULL,
                           status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ENUM: ACTIVE, MAINTENANCE
                           CONSTRAINT FK_Halls_Branches FOREIGN KEY (branch_id) REFERENCES dbo.branches(branch_id),
                           CONSTRAINT UQ_Hall_Branch_Name UNIQUE (branch_id, hall_name)
);
GO

-- 6. Bảng SEAT (Ghế ngồi)
CREATE TABLE dbo.seats (
                           seat_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                           hall_id BIGINT NOT NULL,
                           row_code VARCHAR(10) NOT NULL,
                           number INT NOT NULL,
                           type VARCHAR(50) NOT NULL DEFAULT 'STANDARD', -- ENUM: STANDARD, VIP, SWEETBOX
                           status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ENUM: ACTIVE, BROKEN
                           CONSTRAINT FK_Seats_Halls FOREIGN KEY (hall_id) REFERENCES dbo.halls(hall_id) ON DELETE CASCADE,
                           CONSTRAINT UQ_Seat_Hall_Position UNIQUE (hall_id, row_code, number)
);
GO

-- =============================================
-- 3.3. MOVIE & SHOWTIME CLUSTER
-- =============================================

-- 7. Bảng MOVIE (Phim)
CREATE TABLE dbo.movies (
                            movie_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                            title NVARCHAR(255) NOT NULL,
                            description NVARCHAR(MAX) NULL,
                            duration INT NOT NULL, -- Tính bằng phút
                            release_date DATE NOT NULL,
                            language NVARCHAR(100) NOT NULL,
                            genre NVARCHAR(100) NOT NULL,
                            age_rating VARCHAR(20) NOT NULL, -- e.g., C13, C18, P, K
                            poster_url VARCHAR(500) NULL,
                            trailer_url VARCHAR(500) NULL,
                            status VARCHAR(50) NOT NULL DEFAULT 'COMING_SOON' -- ENUM: COMING_SOON, NOW_SHOWING, ENDED
);
GO

-- 8. Bảng SHOWTIME (Suất chiếu)
CREATE TABLE dbo.showtimes (
                               showtime_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               movie_id BIGINT NOT NULL,
                               hall_id BIGINT NOT NULL,
                               start_time DATETIME2 NOT NULL,
                               end_time DATETIME2 NOT NULL,
                               base_price DECIMAL(18,2) NOT NULL,
                               status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- ENUM: SCHEDULED, SHOWING, COMPLETED, CANCELLED
                               CONSTRAINT FK_Showtimes_Movies FOREIGN KEY (movie_id) REFERENCES dbo.movies(movie_id),
                               CONSTRAINT FK_Showtimes_Halls FOREIGN KEY (hall_id) REFERENCES dbo.halls(hall_id)
);
GO

-- =============================================
-- 3.6. PROMOTION CLUSTER (Tạo trước để BOOKING tham chiếu)
-- =============================================

-- 9. Bảng PROMOTION (Khuyến mãi)
CREATE TABLE dbo.promotions (
                                promotion_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                code VARCHAR(50) NULL, -- NULL cho các chiến dịch tự động áp dụng (auto-applied campaigns)
                                discount_type VARCHAR(50) NOT NULL, -- ENUM: PERCENT, FIXED_AMOUNT
                                discount_percent INT NOT NULL DEFAULT 0, -- Lưu % giảm hoặc giá trị giảm
                                max_discount DECIMAL(18,2) NOT NULL DEFAULT 0, -- Mức giảm tối đa
                                valid_from DATETIME2 NOT NULL,
                                valid_to DATETIME2 NOT NULL,
                                status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' -- ENUM: ACTIVE, EXPIRED, DISABLED
);
GO

-- Cho phép nhiều dòng có code = NULL, nhưng nếu có code thì phải UNIQUE
CREATE UNIQUE NONCLUSTERED INDEX UQ_Promotion_Code_NotNull
ON dbo.promotions(code)
WHERE code IS NOT NULL;
GO

-- =============================================
-- 3.4. BOOKING & PAYMENT CLUSTER
-- =============================================

-- 10. Bảng BOOKING (Đơn đặt vé - nối với ACCOUNT theo ERD & SDS)
CREATE TABLE dbo.bookings (
                              booking_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              account_id BIGINT NOT NULL,
                              promotion_id BIGINT NULL,
                              booking_time DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                              discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                              created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- ENUM: PENDING, CONFIRMED, CANCELLED, EXPIRED
                              CONSTRAINT FK_Bookings_Accounts FOREIGN KEY (account_id) REFERENCES dbo.accounts(account_id),
                              CONSTRAINT FK_Bookings_Promotions FOREIGN KEY (promotion_id) REFERENCES dbo.promotions(promotion_id)
);
GO

-- 11. Bảng TICKET (Vé xem phim)
CREATE TABLE dbo.tickets (
                             ticket_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                             booking_id BIGINT NOT NULL,
                             seat_id BIGINT NOT NULL,
                             showtime_id BIGINT NOT NULL,
                             qr_code VARCHAR(500) NOT NULL UNIQUE,
                             price DECIMAL(18,2) NOT NULL,
                             status VARCHAR(50) NOT NULL DEFAULT 'LOCKED', -- ENUM: LOCKED, BOOKED, CHECKED_IN
                             checked_in_at DATETIME2 NULL,
                             CONSTRAINT FK_Tickets_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id) ON DELETE CASCADE,
                             CONSTRAINT FK_Tickets_Seats FOREIGN KEY (seat_id) REFERENCES dbo.seats(seat_id),
                             CONSTRAINT FK_Tickets_Showtimes FOREIGN KEY (showtime_id) REFERENCES dbo.showtimes(showtime_id),
                             CONSTRAINT UQ_Ticket_Showtime_Seat UNIQUE (showtime_id, seat_id) -- Chống double-booking cùng 1 ghế trong 1 suất chiếu
);
GO

-- 12. Bảng PAYMENT (Thanh toán)
CREATE TABLE dbo.payments (
                              payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              booking_id BIGINT NOT NULL UNIQUE, -- Quan hệ 1-1 với BOOKING theo ERD
                              amount DECIMAL(18,2) NOT NULL,
                              payment_method VARCHAR(50) NOT NULL, -- ENUM: ONLINE_QR, E_WALLET, CASH
                              transaction_time DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- ENUM: SUCCESS, FAILED, PENDING
                              CONSTRAINT FK_Payments_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- 13. Bảng INVOICE (Hóa đơn)
CREATE TABLE dbo.invoices (
                              invoice_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                              booking_id BIGINT NOT NULL UNIQUE, -- Quan hệ 1-1 với BOOKING
                              tax_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
                              total_amount DECIMAL(18,2) NOT NULL,
                              issued_date DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT FK_Invoices_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- =============================================
-- 3.5. FOOD & BEVERAGE (F&B) CLUSTER
-- =============================================

-- 14. Bảng FNB_ITEM (Đồ ăn thức uống)
CREATE TABLE dbo.fnb_items (
                               item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                               name NVARCHAR(255) NOT NULL,
                               category VARCHAR(50) NOT NULL, -- ENUM: FOOD, DRINK, COMBO
                               price DECIMAL(18,2) NOT NULL,
                               is_available BIT NOT NULL DEFAULT 1, -- 1 = TRUE, 0 = FALSE (Out of stock / Disabled)
                               image_url VARCHAR(500) NULL
);
GO

-- 15. Bảng BOOKING_FNB (Chi tiết đồ ăn thức uống theo đơn)
CREATE TABLE dbo.booking_fnb (
                                 booking_fnb_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                                 booking_id BIGINT NOT NULL,
                                 item_id BIGINT NOT NULL,
                                 quantity INT NOT NULL DEFAULT 1,
                                 price_per_item DECIMAL(18,2) NOT NULL,
                                 subtotal DECIMAL(18,2) NOT NULL,
                                 CONSTRAINT FK_BookingFnB_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id) ON DELETE CASCADE,
                                 CONSTRAINT FK_BookingFnB_Items FOREIGN KEY (item_id) REFERENCES dbo.fnb_items(item_id)
);
GO
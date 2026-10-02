-- =============================================
-- Database: CinemaManagementDB
-- Project: Cinema Management System (SWP391)
-- DBMS: Microsoft SQL Server
-- =============================================

IF NOT EXISTS (SELECT name FROM master.dbo.sysdatabases WHERE name = N'CinemaManagementDB')
BEGIN
    CREATE DATABASE [CinemaManagementDB];
END
GO

USE [CinemaManagementDB];
GO

-- 1. Bảng ACCOUNT
IF OBJECT_ID('dbo.accounts', 'U') IS NOT NULL DROP TABLE dbo.accounts;
CREATE TABLE dbo.accounts (
    account_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL, -- CUSTOMER, STAFF, MANAGER, ADMIN
    status VARCHAR(50) DEFAULT 'ACTIVE'
);
GO

-- 2. Bảng BRANCH (Chi nhánh rạp)
IF OBJECT_ID('dbo.branches', 'U') IS NOT NULL DROP TABLE dbo.branches;
CREATE TABLE dbo.branches (
    branch_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_name NVARCHAR(255) NOT NULL,
    address NVARCHAR(500),
    hotline VARCHAR(20),
    opening_time TIME,
    closing_time TIME,
    status VARCHAR(50) DEFAULT 'ACTIVE'
);
GO

-- 3. Bảng CUSTOMER (Khách hàng)
IF OBJECT_ID('dbo.customers', 'U') IS NOT NULL DROP TABLE dbo.customers;
CREATE TABLE dbo.customers (
    customer_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_id BIGINT UNIQUE,
    full_name NVARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email_verified BIT DEFAULT 0,
    CONSTRAINT FK_Customers_Accounts FOREIGN KEY (account_id) REFERENCES dbo.accounts(account_id) ON DELETE CASCADE
);
GO

-- 4. Bảng STAFF / MANAGER (Nhân viên / Quản lý)
IF OBJECT_ID('dbo.staff', 'U') IS NOT NULL DROP TABLE dbo.staff;
CREATE TABLE dbo.staff (
    staff_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_id BIGINT UNIQUE,
    branch_id BIGINT,
    full_name NVARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    CONSTRAINT FK_Staff_Accounts FOREIGN KEY (account_id) REFERENCES dbo.accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT FK_Staff_Branches FOREIGN KEY (branch_id) REFERENCES dbo.branches(branch_id)
);
GO

-- 5. Bảng HALL (Phòng chiếu)
IF OBJECT_ID('dbo.halls', 'U') IS NOT NULL DROP TABLE dbo.halls;
CREATE TABLE dbo.halls (
    hall_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    hall_name NVARCHAR(100) NOT NULL,
    capacity INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    CONSTRAINT FK_Halls_Branches FOREIGN KEY (branch_id) REFERENCES dbo.branches(branch_id)
);
GO

-- 6. Bảng SEAT (Ghế ngồi)
IF OBJECT_ID('dbo.seats', 'U') IS NOT NULL DROP TABLE dbo.seats;
CREATE TABLE dbo.seats (
    seat_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hall_id BIGINT NOT NULL,
    row_code VARCHAR(10) NOT NULL,
    number INT NOT NULL,
    type VARCHAR(50) DEFAULT 'STANDARD', -- STANDARD, VIP, COUPLE
    status VARCHAR(50) DEFAULT 'AVAILABLE', -- AVAILABLE, BOOKED, MAINTENANCE
    CONSTRAINT FK_Seats_Halls FOREIGN KEY (hall_id) REFERENCES dbo.halls(hall_id)
);
GO

-- 7. Bảng MOVIE (Phim)
IF OBJECT_ID('dbo.movies', 'U') IS NOT NULL DROP TABLE dbo.movies;
CREATE TABLE dbo.movies (
    movie_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    duration INT, -- tính bằng phút
    release_date DATE,
    language NVARCHAR(100),
    genre NVARCHAR(100),
    age_rating VARCHAR(20),
    poster_url VARCHAR(500),
    trailer_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'COMING_SOON' -- COMING_SOON, NOW_SHOWING, STOP_SHOWING
);
GO

-- 8. Bảng SHOWTIME (Suất chiếu)
IF OBJECT_ID('dbo.showtimes', 'U') IS NOT NULL DROP TABLE dbo.showtimes;
CREATE TABLE dbo.showtimes (
    showtime_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NOT NULL,
    base_price DECIMAL(18,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, CLOSED, CANCELLED
    CONSTRAINT FK_Showtimes_Movies FOREIGN KEY (movie_id) REFERENCES dbo.movies(movie_id),
    CONSTRAINT FK_Showtimes_Halls FOREIGN KEY (hall_id) REFERENCES dbo.halls(hall_id)
);
GO

-- 9. Bảng PROMOTION (Khuyến mãi)
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;
CREATE TABLE dbo.promotions (
    promotion_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(50) DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED_AMOUNT
    discount_percent DECIMAL(5,2),
    max_discount_amount DECIMAL(18,2),
    valid_from DATETIME2,
    valid_to DATETIME2,
    status VARCHAR(50) DEFAULT 'ACTIVE'
);
GO

-- 10. Bảng BOOKING (Đơn đặt vé)
IF OBJECT_ID('dbo.bookings', 'U') IS NOT NULL DROP TABLE dbo.bookings;
CREATE TABLE dbo.bookings (
    booking_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT,
    promotion_id BIGINT,
    booking_time DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, CONFIRMED, CANCELLED, EXPIRED
    CONSTRAINT FK_Bookings_Customers FOREIGN KEY (customer_id) REFERENCES dbo.customers(customer_id),
    CONSTRAINT FK_Bookings_Promotions FOREIGN KEY (promotion_id) REFERENCES dbo.promotions(promotion_id)
);
GO

-- 11. Bảng TICKET (Vé xem phim)
IF OBJECT_ID('dbo.tickets', 'U') IS NOT NULL DROP TABLE dbo.tickets;
CREATE TABLE dbo.tickets (
    ticket_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    showtime_id BIGINT NOT NULL,
    qr_code VARCHAR(500),
    price DECIMAL(18,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ISSUED', -- ISSUED, CHECKED_IN, CANCELLED
    checked_in_at DATETIME2,
    CONSTRAINT FK_Tickets_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id),
    CONSTRAINT FK_Tickets_Seats FOREIGN KEY (seat_id) REFERENCES dbo.seats(seat_id),
    CONSTRAINT FK_Tickets_Showtimes FOREIGN KEY (showtime_id) REFERENCES dbo.showtimes(showtime_id)
);
GO

-- 12. Bảng INVOICE (Hóa đơn)
IF OBJECT_ID('dbo.invoices', 'U') IS NOT NULL DROP TABLE dbo.invoices;
CREATE TABLE dbo.invoices (
    invoice_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    tax_amount DECIMAL(18,2) DEFAULT 0,
    total_amount DECIMAL(18,2) DEFAULT 0,
    issued_date DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_Invoices_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- 13. Bảng PAYMENT (Thanh toán)
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
CREATE TABLE dbo.payments (
    payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(18,2) DEFAULT 0,
    payment_method VARCHAR(50), -- VNPAY, MOMO, CASH
    transaction_time DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED
    CONSTRAINT FK_Payments_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id)
);
GO

-- 14. Bảng FNB_ITEM (Đồ ăn thức uống)
IF OBJECT_ID('dbo.fnb_items', 'U') IS NOT NULL DROP TABLE dbo.fnb_items;
CREATE TABLE dbo.fnb_items (
    item_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    category NVARCHAR(100),
    price DECIMAL(18,2) DEFAULT 0,
    is_available BIT DEFAULT 1,
    image_url VARCHAR(500)
);
GO

-- 15. Bảng BOOKING_FNB (Chi tiết đồ ăn thức uống theo đơn)
IF OBJECT_ID('dbo.booking_fnb', 'U') IS NOT NULL DROP TABLE dbo.booking_fnb;
CREATE TABLE dbo.booking_fnb (
    booking_fnb_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price_per_item DECIMAL(18,2) DEFAULT 0,
    subtotal DECIMAL(18,2) DEFAULT 0,
    CONSTRAINT FK_BookingFnB_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.bookings(booking_id),
    CONSTRAINT FK_BookingFnB_Items FOREIGN KEY (item_id) REFERENCES dbo.fnb_items(item_id)
);
GO
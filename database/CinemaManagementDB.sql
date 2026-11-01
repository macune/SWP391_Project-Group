/* =====================================================================
   HỆ THỐNG QUẢN LÝ RẠP CHIẾU PHIM ĐA CHI NHÁNH - CINEMA MANAGEMENT SYSTEM
   DATABASE: CinemaManagementDB
   ---------------------------------------------------------------------
   * Chuẩn hóa 100% theo DataInitializer.java (admin, manager, staff, customer)
   * Mật khẩu của TẤT CẢ tài khoản: 123456
   * Chạy lại nhiều lần (F5) không bao giờ bị lỗi Duplicate Key.
   ===================================================================== */

SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. TẠO DATABASE CinemaManagementDB NẾU CHƯA CÓ
IF NOT EXISTS (SELECT name FROM master.dbo.sysdatabases WHERE name = N'CinemaManagementDB')
BEGIN
    CREATE DATABASE [CinemaManagementDB];
END
GO

USE [CinemaManagementDB];
GO

-- 3. BẮT ĐẦU NẠP DỮ LIỆU
BEGIN TRY
BEGIN TRANSACTION;

-- Mật khẩu 123456 mã hóa BCrypt
DECLARE @pwd VARCHAR(255) = '$2b$12$iUWUG/q3VXkltGcTKZPZv.Fv9HPlMdeNZIkZ7.4Px3jw0tTIJc6Ei';

-- 3.1. DỌN SẠCH DỮ LIỆU CŨ AN TOÀN TRÁNH LỖI TRÙNG KHÓA PRIMARY KEY
IF OBJECT_ID('dbo.notifications', 'U')        IS NOT NULL DELETE FROM dbo.notifications;
IF OBJECT_ID('dbo.payment_transactions', 'U') IS NOT NULL DELETE FROM dbo.payment_transactions;
IF OBJECT_ID('dbo.branch_stocks', 'U')        IS NOT NULL DELETE FROM dbo.branch_stocks;
IF OBJECT_ID('dbo.booking_fnb', 'U')          IS NOT NULL DELETE FROM dbo.booking_fnb;
IF OBJECT_ID('dbo.payments', 'U')             IS NOT NULL DELETE FROM dbo.payments;
IF OBJECT_ID('dbo.invoices', 'U')             IS NOT NULL DELETE FROM dbo.invoices;
IF OBJECT_ID('dbo.tickets', 'U')              IS NOT NULL DELETE FROM dbo.tickets;
IF OBJECT_ID('dbo.bookings', 'U')             IS NOT NULL DELETE FROM dbo.bookings;
IF OBJECT_ID('dbo.fnb_items', 'U')            IS NOT NULL DELETE FROM dbo.fnb_items;
IF OBJECT_ID('dbo.promotions', 'U')           IS NOT NULL DELETE FROM dbo.promotions;
IF OBJECT_ID('dbo.showtimes', 'U')            IS NOT NULL DELETE FROM dbo.showtimes;
IF OBJECT_ID('dbo.seats', 'U')                IS NOT NULL DELETE FROM dbo.seats;
IF OBJECT_ID('dbo.halls', 'U')                IS NOT NULL DELETE FROM dbo.halls;
IF OBJECT_ID('dbo.movies', 'U')               IS NOT NULL DELETE FROM dbo.movies;
IF OBJECT_ID('dbo.staff_manager', 'U')        IS NOT NULL DELETE FROM dbo.staff_manager;
IF OBJECT_ID('dbo.customers', 'U')            IS NOT NULL DELETE FROM dbo.customers;
IF OBJECT_ID('dbo.verification_tokens', 'U')  IS NOT NULL DELETE FROM dbo.verification_tokens;
IF OBJECT_ID('dbo.branches', 'U')             IS NOT NULL DELETE FROM dbo.branches;
IF OBJECT_ID('dbo.accounts', 'U')             IS NOT NULL DELETE FROM dbo.accounts;

-- 3.2. BRANCHES (Chi nhánh 1 chuẩn theo DataInitializer.java)
SET IDENTITY_INSERT branches ON;
INSERT INTO branches (branch_id, branch_name, address, hotline, opening_time, closing_time, [status]) VALUES
(1, N'CineFlow Hà Nội',     N'Cầu Giấy, Thành phố Hà Nội',                '0901234567', '08:00', '23:30', 'ACTIVE'),
(2, N'CineFlow Quận 1',     N'Số 25 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh', '0902234567', '08:00', '23:59', 'ACTIVE'),
(3, N'CineFlow Hải Châu',   N'Số 10 Bạch Đằng, Hải Châu, Đà Nẵng',        '0903234567', '08:30', '23:00', 'ACTIVE'),
(4, N'CineFlow Ninh Kiều',  N'Số 5 Hòa Bình, Ninh Kiều, Cần Thơ',         '0904234567', '09:00', '22:30', 'MAINTENANCE'),
(5, N'CineFlow Lê Chân',    N'Số 88 Lạch Tray, Lê Chân, Hải Phòng',       '0905234567', '09:00', '22:30', 'CLOSED');
SET IDENTITY_INSERT branches OFF;

-- 3.3. ACCOUNTS (4 tài khoản đầu chuẩn 100% theo DataInitializer.java)
SET IDENTITY_INSERT accounts ON;
INSERT INTO accounts (account_id, username, password_hash, email, [role], [status]) VALUES
(1,  'admin',               @pwd, 'admin@cineflow.vn',         'ADMIN',    'ACTIVE'),
(2,  'manager',             @pwd, 'manager@cineflow.vn',       'MANAGER',  'ACTIVE'),
(3,  'staff',               @pwd, 'staff@cineflow.vn',         'STAFF',    'ACTIVE'),
(4,  'customer',            @pwd, 'customer@cineflow.vn',      'CUSTOMER', 'ACTIVE'),
(5,  'manager_hcm',         @pwd, 'manager.hcm@cineflow.vn',   'MANAGER',  'ACTIVE'),
(6,  'manager_danang',      @pwd, 'manager.danang@cineflow.vn','MANAGER',  'ACTIVE'),
(7,  'staff_hcm',           @pwd, 'staff.hcm@cineflow.vn',     'STAFF',    'ACTIVE'),
(8,  'staff_danang',        @pwd, 'staff.danang@cineflow.vn',  'STAFF',    'ACTIVE'),
(9,  'customer01',          @pwd, 'an.nguyen@example.com',     'CUSTOMER', 'ACTIVE'),
(10, 'customer02',          @pwd, 'binh.tran@example.com',     'CUSTOMER', 'ACTIVE'),
(11, 'customer03',          @pwd, 'cuong.le@example.com',      'CUSTOMER', 'ACTIVE'),
(12, 'customer04',          @pwd, 'dung.pham@example.com',     'CUSTOMER', 'ACTIVE'),
(13, 'customer_unverified', @pwd, 'phuc.do@example.com',       'CUSTOMER', 'INACTIVE'),
(14, 'customer_locked',     @pwd, 'quan.ly@example.com',       'CUSTOMER', 'LOCKED');
SET IDENTITY_INSERT accounts OFF;

-- 3.4. STAFF & MANAGER (Chuẩn DataInitializer.java)
INSERT INTO staff_manager (staff_id, branch_id, full_name, phone) VALUES
(1, NULL, N'Quản Trị Viên Hệ Thống', '0900000001'),
(2, 1,    N'Nguyễn Quản Lý Rạp',     '0900000002'),
(3, 1,    N'Trần Nhân Viên Quầy Vé', '0900000003'),
(5, 2,    N'Trần Quản Lý Sài Gòn',   '0912000005'),
(6, 3,    N'Lê Quản Lý Đà Nẵng',     '0912000006'),
(7, 2,    N'Đỗ Nhân Viên Sài Gòn',   '0912000007'),
(8, 3,    N'Ngô Nhân Viên Đà Nẵng',  '0912000008');

-- 3.5. CUSTOMERS (Chuẩn DataInitializer.java)
INSERT INTO customers (customer_id, full_name, phone, email_verified) VALUES
(4,  N'Lê Khách Hàng Thân Thiết', '0900000004', 1),
(9,  N'Nguyễn Văn An',            '0901000011', 1),
(10, N'Trần Thị Bình',            '0901000012', 1),
(11, N'Lê Hoàng Cường',           '0901000013', 1),
(12, N'Phạm Minh Dũng',           '0901000014', 1),
(13, N'Đỗ Văn Phúc',              '0901000021', 0),
(14, N'Lý Quốc Quân',             '0901000022', 1);

-- 3.6. VERIFICATION TOKENS
SET IDENTITY_INSERT verification_tokens ON;
INSERT INTO verification_tokens (id, token, account_id, expiry_date) VALUES
(1, 'token-verify-sample-123456789-abcdef', 13, DATEADD(HOUR, 24, SYSDATETIME()));
SET IDENTITY_INSERT verification_tokens OFF;

-- 3.7. HALLS (9 phòng chiếu)
SET IDENTITY_INSERT halls ON;
INSERT INTO halls (hall_id, branch_id, hall_name, capacity, [status]) VALUES
(1, 1, N'Phòng 01', 80, 'ACTIVE'),
(2, 1, N'Phòng 02', 80, 'ACTIVE'),
(3, 1, N'Phòng 03', 80, 'MAINTENANCE'),
(4, 2, N'Phòng 01', 80, 'ACTIVE'),
(5, 2, N'Phòng 02', 80, 'ACTIVE'),
(6, 2, N'Phòng 03', 80, 'ACTIVE'),
(7, 3, N'Phòng 01', 80, 'ACTIVE'),
(8, 3, N'Phòng 02', 80, 'ACTIVE'),
(9, 4, N'Phòng 01', 80, 'MAINTENANCE');
SET IDENTITY_INSERT halls OFF;

-- 3.8. SEATS (720 ghế đầy đủ Standard, VIP, Sweetbox)
SET IDENTITY_INSERT seats ON;
;WITH h AS (SELECT hall_id FROM halls),
r AS (SELECT i, code FROM (VALUES (0,'A'),(1,'B'),(2,'C'),(3,'D'),(4,'E'),(5,'F'),(6,'G'),(7,'H')) v(i, code)),
n AS (SELECT num FROM (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) v(num))
INSERT INTO seats (seat_id, hall_id, row_code, [number], [type], [status])
SELECT (h.hall_id - 1) * 80 + r.i * 10 + n.num,
       h.hall_id,
       r.code,
       n.num,
       CASE WHEN r.i <= 2 THEN 'STANDARD' WHEN r.i <= 6 THEN 'VIP' ELSE 'SWEETBOX' END,
       CASE WHEN r.i = 0 AND n.num = 1 AND h.hall_id IN (2, 5) THEN 'BROKEN' ELSE 'ACTIVE' END
FROM h CROSS JOIN r CROSS JOIN n;
SET IDENTITY_INSERT seats OFF;

-- 3.9. MOVIES (12 phim)
SET IDENTITY_INSERT movies ON;
INSERT INTO movies (movie_id, title, [description], duration, release_date, [language], genre, age_rating, poster_url, trailer_url, [status]) VALUES
(1,  N'Biệt Đội Bóng Đêm',
     N'Một đội đặc nhiệm bí mật phải ngăn chặn âm mưu đánh sập hệ thống điện của cả thành phố trong đúng một đêm.',
     128, CAST(DATEADD(DAY, -20, GETDATE()) AS DATE), 'EN_SUB', 'ACTION', 'T16',
     'https://placehold.co/300x450?text=Biet+Doi+Bong+Dem', 'https://www.youtube.com/watch?v=sample0001', 'NOW_SHOWING'),
(2,  N'Mùa Hoa Anh Đào Muộn',
     N'Câu chuyện tình đẹp và buồn giữa một nhiếp ảnh gia người Việt và cô giáo dạy gốm tại Kyoto.',
     112, CAST(DATEADD(DAY, -14, GETDATE()) AS DATE), 'JP_SUB', 'ROMANCE', 'T13',
     'https://placehold.co/300x450?text=Hoa+Anh+Dao', 'https://www.youtube.com/watch?v=sample0002', 'NOW_SHOWING'),
(3,  N'Hành Tinh Cuối Cùng',
     N'Nhân loại gửi một phi hành đoàn đến hành tinh duy nhất còn khả năng sinh sống, nhưng nơi đó không hề trống rỗng.',
     145, CAST(DATEADD(DAY, -10, GETDATE()) AS DATE), 'EN_SUB', 'SCI_FI', 'T13',
     'https://placehold.co/300x450?text=Hanh+Tinh+Cuoi+Cung', 'https://www.youtube.com/watch?v=sample0003', 'NOW_SHOWING'),
(4,  N'Ngôi Nhà Ma Ám',
     N'Gia đình nhỏ chuyển đến căn nhà cổ giá rẻ và dần phát hiện ra bí mật kinh hoàng dưới tầng hầm.',
     98,  CAST(DATEADD(DAY, -7, GETDATE()) AS DATE), 'VI_ORIGINAL', 'HORROR', 'T18',
     'https://placehold.co/300x450?text=Ngoi+Nha+Ma+Am', 'https://www.youtube.com/watch?v=sample0004', 'NOW_SHOWING'),
(5,  N'Cuộc Phiêu Lưu Của Mèo Mun',
     N'Chú mèo đen Mun lạc khỏi nhà và có chuyến hành trình đầy màu sắc xuyên qua thành phố để tìm lại chủ nhân.',
     95,  CAST(DATEADD(DAY, -25, GETDATE()) AS DATE), 'VI_DUBBED', 'ANIMATION', 'P',
     'https://placehold.co/300x450?text=Meo+Mun', 'https://www.youtube.com/watch?v=sample0005', 'NOW_SHOWING'),
(6,  N'Tiếng Cười Cuối Năm',
     N'Ba anh em tranh nhau tổ chức bữa cơm tất niên và gây ra hàng loạt tình huống dở khóc dở cười.',
     105, CAST(DATEADD(DAY, -5, GETDATE()) AS DATE), 'VI_ORIGINAL', 'COMEDY', 'T13',
     'https://placehold.co/300x450?text=Tieng+Cuoi+Cuoi+Nam', 'https://www.youtube.com/watch?v=sample0006', 'NOW_SHOWING'),
(7,  N'Seoul Trong Mưa',
     N'Một thám tử bị đình chỉ phải điều tra chuỗi vụ mất tích bí ẩn xảy ra vào mỗi đêm mưa tại Seoul.',
     118, CAST(DATEADD(DAY, -12, GETDATE()) AS DATE), 'KR_SUB', 'THRILLER', 'T16',
     'https://placehold.co/300x450?text=Seoul+Trong+Mua', 'https://www.youtube.com/watch?v=sample0007', 'NOW_SHOWING'),
(8,  N'Kho Báu Đảo Rồng',
     N'Nhóm bạn trẻ lần theo tấm bản đồ cổ để tìm kho báu huyền thoại trên hòn đảo không có trên bản đồ.',
     122, CAST(DATEADD(DAY, 14, GETDATE()) AS DATE), 'EN_SUB', 'ADVENTURE', 'P',
     'https://placehold.co/300x450?text=Kho+Bau+Dao+Rong', 'https://www.youtube.com/watch?v=sample0008', 'COMING_SOON'),
(9,  N'Bangkok Đêm Không Ngủ',
     N'Một tay đua xe ngầm bị cuốn vào cuộc chiến giành quyền lực giữa các băng nhóm tại Bangkok.',
     110, CAST(DATEADD(DAY, 21, GETDATE()) AS DATE), 'TH_SUB', 'ACTION', 'T18',
     'https://placehold.co/300x450?text=Bangkok+Dem', 'https://www.youtube.com/watch?v=sample0009', 'COMING_SOON'),
(10, N'Hành Trình Đại Dương Xanh',
     N'Bộ phim tài liệu khám phá hệ sinh thái dưới đáy biển sâu mà con người chưa từng chạm tới.',
     90,  CAST(DATEADD(DAY, 30, GETDATE()) AS DATE), 'EN_SUB', 'DOCUMENTARY', 'P',
     'https://placehold.co/300x450?text=Dai+Duong+Xanh', 'https://www.youtube.com/watch?v=sample0010', 'COMING_SOON'),
(11, N'Mùa Hè Năm Ấy',
     N'Những ký ức tuổi học trò của nhóm bạn thân trong mùa hè cuối cấp ba.',
     108, CAST(DATEADD(DAY, -120, GETDATE()) AS DATE), 'VI_ORIGINAL', 'ROMANCE', 'T13',
     'https://placehold.co/300x450?text=Mua+He+Nam+Ay', 'https://www.youtube.com/watch?v=sample0011', 'ENDED'),
(12, N'Chiến Binh Thép',
     N'Người lính già tái xuất để bảo vệ thị trấn nhỏ trước một băng cướp vũ trang.',
     135, CAST(DATEADD(DAY, -150, GETDATE()) AS DATE), 'EN_SUB', 'ACTION', 'T16',
     'https://placehold.co/300x450?text=Chien+Binh+Thep', 'https://www.youtube.com/watch?v=sample0012', 'ENDED');
SET IDENTITY_INSERT movies OFF;

-- 3.10. SHOWTIMES (Suất chiếu động quanh thời điểm chạy script)
SET IDENTITY_INSERT showtimes ON;
INSERT INTO showtimes (showtime_id, movie_id, hall_id, start_time, end_time, base_price, [status])
SELECT v.id, v.movie_id, v.hall_id, s.start_time, DATEADD(MINUTE, m.duration + 15, s.start_time), v.base_price, v.[status]
FROM (VALUES
    (1,  1, 1, -1,  '19:00',  90000, 'COMPLETED'),
    (2,  5, 2, -1,  '15:00',  80000, 'COMPLETED'),
    (3,  7, 4, -1,  '20:00',  95000, 'COMPLETED'),
    (4,  1, 1,  1,  '10:00',  90000, 'SCHEDULED'),
    (5,  1, 1,  1,  '14:00',  90000, 'SCHEDULED'),
    (6,  1, 1,  1,  '19:30', 100000, 'SCHEDULED'),
    (7,  3, 2,  1,  '10:00', 100000, 'SCHEDULED'),
    (8,  3, 2,  1,  '15:30', 100000, 'SCHEDULED'),
    (9,  5, 2,  2,  '09:00',  80000, 'SCHEDULED'),
    (10, 4, 2,  1,  '20:00',  95000, 'SCHEDULED'),
    (11, 2, 1,  2,  '18:00',  90000, 'SCHEDULED'),
    (12, 6, 2,  2,  '19:00',  85000, 'SCHEDULED'),
    (13, 4, 3,  1,  '20:00',  95000, 'CANCELLED'),
    (14, 6, 4,  1,  '09:30',  85000, 'SCHEDULED'),
    (15, 7, 4,  1,  '13:00',  95000, 'SCHEDULED'),
    (16, 1, 4,  1,  '18:00', 100000, 'SCHEDULED'),
    (17, 3, 5,  1,  '11:00', 110000, 'SCHEDULED'),
    (18, 4, 5,  1,  '16:00',  95000, 'SCHEDULED'),
    (19, 5, 6,  1,  '09:00',  80000, 'SCHEDULED'),
    (20, 5, 6,  1,  '13:30',  80000, 'SCHEDULED'),
    (21, 2, 6,  2,  '19:00',  90000, 'SCHEDULED'),
    (22, 7, 4,  2,  '20:00',  95000, 'SCHEDULED'),
    (23, 1, 7,  1,  '10:30',  85000, 'SCHEDULED'),
    (24, 6, 7,  1,  '15:00',  80000, 'SCHEDULED'),
    (25, 3, 8,  1,  '19:30', 100000, 'SCHEDULED'),
    (26, 5, 8,  2,  '10:00',  75000, 'SCHEDULED'),
    (27, 7, 7,  3,  '20:00',  90000, 'SCHEDULED'),
    (28, 12, 4, -30,'19:00',  90000, 'COMPLETED'),
    (29, 11, 5, -40,'19:00',  90000, 'COMPLETED')
) AS v(id, movie_id, hall_id, day_offset, start_hm, base_price, [status])
JOIN movies m ON m.movie_id = v.movie_id
CROSS APPLY (SELECT DATEADD(MINUTE,
                            DATEDIFF(MINUTE, CAST('00:00' AS TIME), CAST(v.start_hm AS TIME)),
                            CAST(DATEADD(DAY, v.day_offset, CAST(GETDATE() AS DATE)) AS DATETIME2)) AS start_time) s;

-- Suất chiếu đang chiếu & sắp chiếu trong ngày
INSERT INTO showtimes (showtime_id, movie_id, hall_id, start_time, end_time, base_price, [status])
SELECT x.id, x.movie_id, x.hall_id,
       DATEADD(MINUTE, x.offset_min, SYSDATETIME()),
       DATEADD(MINUTE, x.offset_min + m.duration + 15, SYSDATETIME()),
       x.base_price, x.[status]
FROM (VALUES
    (30, 6, 5, -30,  85000, 'SHOWING'),
    (31, 1, 1, 180,  90000, 'SCHEDULED')
) AS x(id, movie_id, hall_id, offset_min, base_price, [status])
JOIN movies m ON m.movie_id = x.movie_id;

-- Suất chiếu lịch sử 14 ngày qua (để test Dashboard doanh thu)
;WITH d AS (SELECT n FROM (VALUES (2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14)) v(n)),
h AS (SELECT hall_id FROM (VALUES (1),(4),(7)) v(hall_id)),
x AS (
    SELECT ROW_NUMBER() OVER (ORDER BY d.n, h.hall_id) AS rn,
           d.n, h.hall_id,
           DATEADD(HOUR, 19, CAST(DATEADD(DAY, -d.n, CAST(GETDATE() AS DATE)) AS DATETIME2)) AS st
    FROM d CROSS JOIN h
)
INSERT INTO showtimes (showtime_id, movie_id, hall_id, start_time, end_time, base_price, [status])
SELECT 100 + x.rn,
       ((x.n + x.hall_id) % 7) + 1,
       x.hall_id,
       x.st,
       DATEADD(MINUTE, m.duration + 15, x.st),
       90000,
       'COMPLETED'
FROM x
JOIN movies m ON m.movie_id = ((x.n + x.hall_id) % 7) + 1;
SET IDENTITY_INSERT showtimes OFF;

-- 3.11. PROMOTIONS
SET IDENTITY_INSERT promotions ON;
INSERT INTO promotions (promotion_id, code, discount_type, discount_percentage, max_discount, valid_from, valid_to, [status]) VALUES
(1, 'WELCOME10', 'PERCENTAGE',   10,  50000.00, DATEADD(DAY, -60, SYSDATETIME()), DATEADD(DAY,  60, SYSDATETIME()), 'ACTIVE'),
(2, 'SALE20',    'PERCENTAGE',   20, 100000.00, DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY,  30, SYSDATETIME()), 'ACTIVE'),
(3, 'GIAM50K',   'FIXED_AMOUNT',  50000,  0.00, DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY,  90, SYSDATETIME()), 'ACTIVE'),
(4, 'TET2026',   'PERCENTAGE',   15,  80000.00, DATEADD(DAY, -120, SYSDATETIME()),DATEADD(DAY, -60, SYSDATETIME()), 'EXPIRED'),
(5, 'VIP30',     'PERCENTAGE',   30, 150000.00, DATEADD(DAY, -10, SYSDATETIME()), DATEADD(DAY,  90, SYSDATETIME()), 'DISABLED'),
(6, 'PROMO5',    'PERCENTAGE',    5,  30000.00, DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY,  90, SYSDATETIME()), 'ACTIVE'),
(7, 'NEWYEAR27', 'PERCENTAGE',   25, 120000.00, DATEADD(DAY,  60, SYSDATETIME()), DATEADD(DAY, 120, SYSDATETIME()), 'ACTIVE');
SET IDENTITY_INSERT promotions OFF;

-- 3.12. FNB ITEMS
SET IDENTITY_INSERT fnb_items ON;
INSERT INTO fnb_items (item_id, name, category, price, is_available, image_url) VALUES
(1,  N'Bắp rang bơ phô mai',          'FOOD',  45000.00, 1, '/images/FnB/Coca.jpg'),
(2,  N'Bắp rang bơ caramel',          'FOOD',  49000.00, 1, '/images/FnB/Coca.jpg'),
(3,  N'Xúc xích nướng',               'FOOD',  30000.00, 1, '/images/FnB/Coca.jpg'),
(4,  N'Khoai tây chiên',              'FOOD',  35000.00, 1, '/images/FnB/Coca.jpg'),
(5,  N'Coca-Cola (L)',                'DRINK', 30000.00, 1, '/images/FnB/Coca.jpg'),
(6,  N'Pepsi (L)',                    'DRINK', 30000.00, 1, '/images/FnB/Coca.jpg'),
(7,  N'Trà đào cam sả',               'DRINK', 42000.00, 1, '/images/FnB/Coca.jpg'),
(8,  N'Nước suối',                    'DRINK', 15000.00, 1, '/images/FnB/Coca.jpg'),
(9,  N'Combo 1 (1 bắp + 1 nước)',     'COMBO', 69000.00, 1, '/images/FnB/Coca.jpg'),
(10, N'Combo 2 (1 bắp lớn + 2 nước)', 'COMBO', 99000.00, 1, '/images/FnB/Coca.jpg'),
(11, N'Combo Gia đình (2 bắp + 4 nước)','COMBO',179000.00,1, '/images/FnB/Coca.jpg'),
(12, N'Combo Halloween (ngừng bán)',  'COMBO',129000.00, 0, NULL);
SET IDENTITY_INSERT fnb_items OFF;

-- 3.13. BRANCH STOCKS
INSERT INTO branch_stocks (branch_id, item_id, quantity, last_updated)
SELECT b.branch_id, f.item_id,
       CASE WHEN f.is_available = 0 THEN 0 ELSE 150 - (b.branch_id * 10) END,
       SYSDATETIME()
FROM branches b
CROSS JOIN fnb_items f;

-- 3.14. BOOKINGS
-- Gán đơn đặt vé cho tài khoản 'customer' (ID 4) và các khách hàng khác
SET IDENTITY_INSERT bookings ON;
INSERT INTO bookings (booking_id, account_id, promotion_id, booking_time, total_amount, discount_amount, created_at, [status])
SELECT v.booking_id, v.account_id, v.promotion_id,
       DATEADD(MINUTE, -v.minutes_ago, SYSDATETIME()), 0, 0,
       DATEADD(MINUTE, -v.minutes_ago, SYSDATETIME()), v.[status]
FROM (VALUES
    (1,   4, 1,    2880,  'CONFIRMED'),
    (2,   3, NULL, 2880,  'CONFIRMED'), -- bán tại quầy bởi staff (ID 3)
    (3,   4, 2,     600,  'CONFIRMED'),
    (4,   9, NULL,  300,  'CONFIRMED'),
    (5,  10, NULL,    3,  'PENDING'),
    (6,  11, NULL,  120,  'EXPIRED'),
    (7,  12, NULL, 1440,  'CANCELLED'),
    (8,   4, 3,     200,  'CONFIRMED'),
    (9,   9, 6,      90,  'CONFIRMED'),
    (10,  4, NULL, 1440,  'CONFIRMED'),
    (11, 10, 2,      60,  'CONFIRMED'),
    (12, 11, NULL, 44640, 'CONFIRMED'),
    (13, 12, NULL, 60480, 'CONFIRMED'),
    (14,  4, NULL,  120,  'CONFIRMED'),
    (15,  3, NULL,   20,  'CONFIRMED'), -- bán tại quầy bởi staff
    (16,  4, NULL,   10,  'PENDING')
) AS v(booking_id, account_id, promotion_id, minutes_ago, [status]);

-- Bookings lịch sử (id 1001..1078) xoay quanh các khách hàng
;WITH s AS (
    SELECT showtime_id, start_time,
           ROW_NUMBER() OVER (ORDER BY showtime_id) - 1 AS k
    FROM showtimes WHERE showtime_id BETWEEN 101 AND 139
),
j AS (SELECT j FROM (VALUES (1),(2)) v(j))
INSERT INTO bookings (booking_id, account_id, promotion_id, booking_time, total_amount, discount_amount, created_at, [status])
SELECT 1000 + s.k * 2 + j.j,
       CASE WHEN (s.k * 2 + j.j) % 3 = 0 THEN 4 ELSE 9 + ((s.k * 2 + j.j) % 4) END,
       CASE WHEN (s.k * 2 + j.j) % 10 = 0 THEN 1 ELSE NULL END,
       DATEADD(HOUR, -(j.j * 5), s.start_time), 0, 0,
       DATEADD(HOUR, -(j.j * 5), s.start_time),
       'CONFIRMED'
FROM s CROSS JOIN j;
SET IDENTITY_INSERT bookings OFF;

-- 3.15. TICKETS (Mã QR chuẩn CF-TKT-... tương thích chức năng quét vé)
SET IDENTITY_INSERT tickets ON;
INSERT INTO tickets (ticket_id, booking_id, seat_id, showtime_id, qr_code, price, [status], checked_in_at)
SELECT v.ticket_id, v.booking_id, se.seat_id, v.showtime_id,
       'CF-TKT-' + RIGHT('000000' + CAST(v.ticket_id AS VARCHAR(10)), 6) + '-' + REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''),
       CAST(st.base_price * CASE se.[type] WHEN 'VIP' THEN 1.2 WHEN 'SWEETBOX' THEN 1.5 ELSE 1.0 END AS DECIMAL(18,2)),
       v.[status],
       CASE WHEN v.[status] = 'CHECKED_IN' THEN DATEADD(MINUTE, -10, st.start_time) END
FROM (VALUES
    (1,  1,  1,  'D', 5,  'CHECKED_IN'), (2,  1,  1,  'D', 6,  'CHECKED_IN'),
    (3,  2,  2,  'B', 3,  'CHECKED_IN'), (4,  2,  2,  'B', 4,  'CHECKED_IN'), (5,  2,  2,  'B', 5,  'CHECKED_IN'),
    (6,  3,  4,  'E', 3,  'BOOKED'),     (7,  3,  4,  'E', 4,  'BOOKED'),
    (8,  4,  7,  'H', 1,  'BOOKED'),     (9,  4,  7,  'H', 2,  'BOOKED'),
    (10, 5,  6,  'C', 5,  'LOCKED'),     (11, 5,  6,  'C', 6,  'LOCKED'),
    (12, 6,  8,  'C', 1,  'CANCELLED'),  (13, 6,  8,  'C', 2,  'CANCELLED'),
    (14, 7,  15, 'D', 4,  'CANCELLED'),  (15, 7,  15, 'D', 5,  'CANCELLED'),
    (16, 8,  17, 'D', 5,  'BOOKED'),     (17, 8,  17, 'D', 6,  'BOOKED'),
    (18, 8,  17, 'D', 7,  'BOOKED'),     (19, 8,  17, 'D', 8,  'BOOKED'),
    (20, 9,  21, 'E', 5,  'BOOKED'),     (21, 9,  21, 'E', 6,  'BOOKED'),
    (22, 10, 30, 'F', 4,  'CHECKED_IN'), (23, 10, 30, 'F', 5,  'CHECKED_IN'),
    (24, 11, 16, 'H', 3,  'BOOKED'),     (25, 11, 16, 'H', 4,  'BOOKED'),
    (26, 12, 28, 'D', 5,  'CHECKED_IN'), (27, 12, 28, 'D', 6,  'CHECKED_IN'),
    (28, 13, 29, 'C', 5,  'CHECKED_IN'), (29, 13, 29, 'C', 6,  'CHECKED_IN'), (30, 13, 29, 'C', 7, 'CHECKED_IN'),
    (31, 14, 31, 'E', 5,  'BOOKED'),     (32, 14, 31, 'E', 6,  'BOOKED'),
    (33, 15, 31, 'G', 5,  'BOOKED'),     (34, 15, 31, 'G', 6,  'BOOKED'),
    (35, 16, 18, 'D', 3,  'LOCKED'),     (36, 16, 18, 'D', 4,  'LOCKED')
) AS v(ticket_id, booking_id, showtime_id, row_code, seat_no, [status])
JOIN showtimes st ON st.showtime_id = v.showtime_id
JOIN seats se ON se.hall_id = st.hall_id AND se.row_code = v.row_code AND se.[number] = v.seat_no;

-- Tickets lịch sử
;WITH s AS (
    SELECT showtime_id, hall_id, base_price, start_time,
           ROW_NUMBER() OVER (ORDER BY showtime_id) - 1 AS k
    FROM showtimes WHERE showtime_id BETWEEN 101 AND 139
),
j AS (SELECT j FROM (VALUES (1),(2)) v(j)),
p AS (SELECT p FROM (VALUES (1),(2)) v(p)),
x AS (
    SELECT s.*, j.j, p.p, 1000 + s.k * 2 + j.j AS booking_id
    FROM s CROSS JOIN j CROSS JOIN p
)
INSERT INTO tickets (ticket_id, booking_id, seat_id, showtime_id, qr_code, price, [status], checked_in_at)
SELECT 10000 + (x.booking_id - 1000) * 2 + x.p,
       x.booking_id,
       se.seat_id,
       x.showtime_id,
       'CF-TKT-H' + CAST(10000 + (x.booking_id - 1000) * 2 + x.p AS VARCHAR(10)) + '-' + REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''),
       CAST(x.base_price * CASE se.[type] WHEN 'VIP' THEN 1.2 WHEN 'SWEETBOX' THEN 1.5 ELSE 1.0 END AS DECIMAL(18,2)),
       'CHECKED_IN',
       DATEADD(MINUTE, -15, x.start_time)
FROM x
JOIN seats se ON se.hall_id = x.hall_id
             AND se.row_code = CASE x.j WHEN 1 THEN 'E' ELSE 'F' END
             AND se.[number] = x.p + 3;
SET IDENTITY_INSERT tickets OFF;

-- 3.16. BOOKING FNB
SET IDENTITY_INSERT booking_fnb ON;
INSERT INTO booking_fnb (booking_fnb_id, booking_id, item_id, quantity, price_per_item, subtotal)
SELECT v.id, v.booking_id, v.item_id, v.qty, f.price, f.price * v.qty
FROM (VALUES
    (1,  1,  9,  1),
    (2,  2,  1,  1), (3,  2,  5,  2),
    (4,  3,  10, 1),
    (5,  5,  9,  2),
    (6,  8,  11, 1), (7,  8,  4,  2),
    (8,  9,  7,  2),
    (9,  10, 2,  1), (10, 10, 8,  2),
    (11, 11, 10, 1),
    (12, 14, 9,  2),
    (13, 15, 3,  1), (14, 15, 6,  1),
    (15, 13, 11, 1)
) AS v(id, booking_id, item_id, qty)
JOIN fnb_items f ON f.item_id = v.item_id;

INSERT INTO booking_fnb (booking_fnb_id, booking_id, item_id, quantity, price_per_item, subtotal)
SELECT 20000 + (b.booking_id - 1000), b.booking_id,
       CASE WHEN b.booking_id % 2 = 1 THEN 9 ELSE 10 END,
       1, f.price, f.price
FROM bookings b
JOIN fnb_items f ON f.item_id = CASE WHEN b.booking_id % 2 = 1 THEN 9 ELSE 10 END
WHERE b.booking_id BETWEEN 1001 AND 1078;
SET IDENTITY_INSERT booking_fnb OFF;

-- 3.17. CẬP NHẬT TỔNG TIỀN VÀ KHUYẾN MÃI CHO TẤT CẢ BOOKING
UPDATE b
SET b.total_amount = ISNULL(t.ticket_sum, 0) + ISNULL(f.fnb_sum, 0)
FROM bookings b
LEFT JOIN (SELECT booking_id, SUM(price)    AS ticket_sum FROM tickets     GROUP BY booking_id) t ON t.booking_id = b.booking_id
LEFT JOIN (SELECT booking_id, SUM(subtotal) AS fnb_sum    FROM booking_fnb GROUP BY booking_id) f ON f.booking_id = b.booking_id;

UPDATE b
SET b.discount_amount =
    CASE WHEN p.discount_type IN ('PERCENT', 'PERCENTAGE') THEN
            CASE WHEN ROUND(b.total_amount * p.discount_percentage / 100.0, 0) > p.max_discount
                 THEN p.max_discount
                 ELSE ROUND(b.total_amount * p.discount_percentage / 100.0, 0) END
        ELSE
            CASE WHEN p.max_discount > b.total_amount THEN b.total_amount ELSE p.max_discount END
    END
FROM bookings b
JOIN promotions p ON p.promotion_id = b.promotion_id;

-- 3.18. PAYMENTS
SET IDENTITY_INSERT payments ON;
INSERT INTO payments (payment_id, booking_id, amount, payment_method, transaction_time, [status])
SELECT b.booking_id, b.booking_id, b.total_amount - b.discount_amount, v.pay_method,
       DATEADD(MINUTE, 1, b.booking_time), v.[status]
FROM (VALUES
    (1,  'ONLINE_QR', 'SUCCESS'),
    (2,  'CASH',      'SUCCESS'),
    (3,  'E_WALLET',  'SUCCESS'),
    (4,  'ONLINE_QR', 'SUCCESS'),
    (5,  'ONLINE_QR', 'PENDING'),
    (7,  'E_WALLET',  'FAILED'),
    (8,  'ONLINE_QR', 'SUCCESS'),
    (9,  'E_WALLET',  'SUCCESS'),
    (10, 'ONLINE_QR', 'SUCCESS'),
    (11, 'E_WALLET',  'SUCCESS'),
    (12, 'ONLINE_QR', 'SUCCESS'),
    (13, 'CASH',      'SUCCESS'),
    (14, 'E_WALLET',  'SUCCESS'),
    (15, 'CASH',      'SUCCESS'),
    (16, 'E_WALLET',  'FAILED')
) AS v(booking_id, pay_method, [status])
JOIN bookings b ON b.booking_id = v.booking_id;

INSERT INTO payments (payment_id, booking_id, amount, payment_method, transaction_time, [status])
SELECT b.booking_id, b.booking_id, b.total_amount - b.discount_amount,
       CASE b.booking_id % 3 WHEN 0 THEN 'ONLINE_QR' WHEN 1 THEN 'E_WALLET' ELSE 'CASH' END,
       DATEADD(MINUTE, 1, b.booking_time), 'SUCCESS'
FROM bookings b
WHERE b.booking_id BETWEEN 1001 AND 1078;
SET IDENTITY_INSERT payments OFF;

-- 3.19. PAYMENT TRANSACTIONS
INSERT INTO payment_transactions (booking_id, amount, payment_method, transaction_no, payment_time)
SELECT p.booking_id, p.amount, p.payment_method,
       'TXN-' + CAST(p.payment_id AS VARCHAR(10)) + '-' + REPLACE(CONVERT(VARCHAR(36), NEWID()), '-', ''),
       p.transaction_time
FROM payments p
WHERE p.[status] = 'SUCCESS';

-- 3.20. INVOICES
SET IDENTITY_INSERT invoices ON;
INSERT INTO invoices (invoice_id, booking_id, tax_amount, total_amount, issued_date)
SELECT b.booking_id, b.booking_id,
       ROUND((b.total_amount - b.discount_amount) * 8.0 / 108.0, 0),
       b.total_amount - b.discount_amount,
       DATEADD(MINUTE, 2, b.booking_time)
FROM bookings b
WHERE b.[status] = 'CONFIRMED';
SET IDENTITY_INSERT invoices OFF;

-- 3.21. NOTIFICATIONS
SET IDENTITY_INSERT notifications ON;
INSERT INTO notifications (notification_id, account_id, recipient_email, title, [message], email_content, [type], template_code, reference_id, [status], is_read, email_sent, sent_at, created_at)
VALUES
(1, 4, 'customer@cineflow.vn', N'Đặt vé thành công #1', N'Đơn đặt vé #1 cho phim Biệt Đội Bóng Đêm đã được xác nhận thành công.', N'<p>Cảm ơn quý khách đã đặt vé tại CineFlow.</p>', 'BOOKING', 'TPL_BOOKING_SUCCESS', 1, 'SENT', 0, 1, DATEADD(MINUTE, -2870, SYSDATETIME()), DATEADD(MINUTE, -2870, SYSDATETIME())),
(2, 4, 'customer@cineflow.vn', N'Thanh toán thành công #1', N'Giao dịch thanh toán 216.000 VNĐ cho đơn hàng #1 qua VNPay thành công.', N'<p>Thanh toán thành công.</p>', 'PAYMENT', 'TPL_PAYMENT_SUCCESS', 1, 'SENT', 1, 1, DATEADD(MINUTE, -2869, SYSDATETIME()), DATEADD(MINUTE, -2869, SYSDATETIME())),
(3, 4, 'customer@cineflow.vn', N'Nhắc nhở giờ chiếu phim', N'Suất chiếu Biệt Đội Bóng Đêm của bạn sắp diễn ra. Vui lòng có mặt trước 15 phút.', N'<p>Nhắc giờ chiếu phim.</p>', 'SHOWTIME', 'TPL_SHOWTIME_REMIND', 1, 'SENT', 0, 1, DATEADD(MINUTE, -120, SYSDATETIME()), DATEADD(MINUTE, -120, SYSDATETIME())),
(4, 4, 'customer@cineflow.vn', N'Ưu đãi chào mừng thành viên mới', N'Tặng bạn mã WELCOME10 giảm ngay 10% cho lần đặt vé tiếp theo.', N'<p>Mã giảm giá cho bạn.</p>', 'PROMOTION', 'TPL_PROMO_NEW', 1, 'SENT', 0, 1, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME())),
(5, 4, 'customer@cineflow.vn', N'Đặt vé thành công #3', N'Đơn đặt vé #3 cho phim Mùa Hoa Anh Đào Muộn đã sẵn sàng.', N'<p>Đặt vé thành công.</p>', 'BOOKING', 'TPL_BOOKING_SUCCESS', 3, 'SENT', 0, 1, DATEADD(MINUTE, -590, SYSDATETIME()), DATEADD(MINUTE, -590, SYSDATETIME())),
(6, 9, 'an.nguyen@example.com', N'Đặt vé thành công #4', N'Đơn đặt vé #4 đã hoàn tất. Cảm ơn bạn đã lựa chọn CineFlow!', N'<p>Đặt vé thành công.</p>', 'BOOKING', 'TPL_BOOKING_SUCCESS', 4, 'SENT', 0, 1, DATEADD(MINUTE, -290, SYSDATETIME()), DATEADD(MINUTE, -290, SYSDATETIME())),
(7, 10, 'binh.tran@example.com', N'Đặt vé thành công #11', N'Đơn đặt vé #11 đã xác nhận thành công. Xem vé tại mục Vé của tôi.', N'<p>Đặt vé thành công.</p>', 'BOOKING', 'TPL_BOOKING_SUCCESS', 11, 'SENT', 1, 1, DATEADD(MINUTE, -55, SYSDATETIME()), DATEADD(MINUTE, -55, SYSDATETIME())),
(8, 1, 'admin@cineflow.vn', N'Báo cáo doanh thu tuần', N'Hệ thống đã tự động kết toán doanh thu tuần vừa qua của toàn bộ 5 chi nhánh.', N'<p>Báo cáo quản trị.</p>', 'BOOKING', 'TPL_ADMIN_REPORT', NULL, 'SENT', 0, 0, DATEADD(HOUR, -6, SYSDATETIME()), DATEADD(HOUR, -6, SYSDATETIME()));
SET IDENTITY_INSERT notifications OFF;

-- 3.22. ĐẶT LẠI IDENTITY (RESEED)
DECLARE @m BIGINT;
SELECT @m = MAX(account_id)           FROM accounts;            IF @m IS NOT NULL DBCC CHECKIDENT ('accounts',            RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(id)                   FROM verification_tokens; IF @m IS NOT NULL DBCC CHECKIDENT ('verification_tokens', RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(branch_id)            FROM branches;            IF @m IS NOT NULL DBCC CHECKIDENT ('branches',            RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(hall_id)              FROM halls;               IF @m IS NOT NULL DBCC CHECKIDENT ('halls',               RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(seat_id)              FROM seats;               IF @m IS NOT NULL DBCC CHECKIDENT ('seats',               RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(movie_id)             FROM movies;              IF @m IS NOT NULL DBCC CHECKIDENT ('movies',              RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(showtime_id)          FROM showtimes;           IF @m IS NOT NULL DBCC CHECKIDENT ('showtimes',           RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(promotion_id)         FROM promotions;          IF @m IS NOT NULL DBCC CHECKIDENT ('promotions',          RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(item_id)              FROM fnb_items;           IF @m IS NOT NULL DBCC CHECKIDENT ('fnb_items',           RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(id)                   FROM branch_stocks;       IF @m IS NOT NULL DBCC CHECKIDENT ('branch_stocks',       RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(booking_id)           FROM bookings;            IF @m IS NOT NULL DBCC CHECKIDENT ('bookings',            RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(ticket_id)            FROM tickets;             IF @m IS NOT NULL DBCC CHECKIDENT ('tickets',             RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(booking_fnb_id)       FROM booking_fnb;         IF @m IS NOT NULL DBCC CHECKIDENT ('booking_fnb',         RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(payment_id)           FROM payments;            IF @m IS NOT NULL DBCC CHECKIDENT ('payments',            RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(id)                   FROM payment_transactions;IF @m IS NOT NULL DBCC CHECKIDENT ('payment_transactions',RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(invoice_id)           FROM invoices;            IF @m IS NOT NULL DBCC CHECKIDENT ('invoices',            RESEED, @m) WITH NO_INFOMSGS;
SELECT @m = MAX(notification_id)     FROM notifications;       IF @m IS NOT NULL DBCC CHECKIDENT ('notifications',       RESEED, @m) WITH NO_INFOMSGS;

COMMIT TRANSACTION;

-- 3.23. BÁO CÁO THỐNG KÊ SỐ LƯỢNG BẢN GHI
SELECT 'accounts' AS tbl, COUNT(*) AS total FROM accounts
UNION ALL SELECT 'customers',            COUNT(*) FROM customers
UNION ALL SELECT 'staff_manager',        COUNT(*) FROM staff_manager
UNION ALL SELECT 'verification_tokens',  COUNT(*) FROM verification_tokens
UNION ALL SELECT 'branches',             COUNT(*) FROM branches
UNION ALL SELECT 'halls',                COUNT(*) FROM halls
UNION ALL SELECT 'seats',                COUNT(*) FROM seats
UNION ALL SELECT 'movies',               COUNT(*) FROM movies
UNION ALL SELECT 'showtimes',            COUNT(*) FROM showtimes
UNION ALL SELECT 'promotions',           COUNT(*) FROM promotions
UNION ALL SELECT 'fnb_items',            COUNT(*) FROM fnb_items
UNION ALL SELECT 'branch_stocks',        COUNT(*) FROM branch_stocks
UNION ALL SELECT 'bookings',             COUNT(*) FROM bookings
UNION ALL SELECT 'tickets',              COUNT(*) FROM tickets
UNION ALL SELECT 'booking_fnb',          COUNT(*) FROM booking_fnb
UNION ALL SELECT 'invoices',             COUNT(*) FROM invoices
UNION ALL SELECT 'payments',             COUNT(*) FROM payments
UNION ALL SELECT 'payment_transactions', COUNT(*) FROM payment_transactions
UNION ALL SELECT 'notifications',        COUNT(*) FROM notifications;

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
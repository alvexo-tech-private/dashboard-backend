-- =============================================================================
-- Admin Portal Dashboard — sample data
-- =============================================================================
-- Plain SQL, not a Liquibase changeset. Run it yourself against your LOCAL
-- Postgres (bike_service_db_to) with e.g.:
--   psql -h localhost -U postgres -d bike_service_db_to -f sample-data.sql
-- or paste it into your DB client. Not executed by Claude.
--
-- Assumes:
--   * admin_users already seeded (dashboard-backend changeset 002) — used only
--     for workshop_verifications.decided_by_admin_id.
--   * vehicle-service-booking-api's dev vehicle catalogue seed (022-seed-
--     vehicle-catalogue) has already run, i.e. `SELECT COUNT(*) FROM vehicles`
--     is non-zero. If it's empty, run that service once first — bookings/
--     reviews below need existing vehicles rows.
--
-- Covers every tile in the Admin Dashboard KPI summary
-- (dashboard-frontend/src/app/features/dashboard/dashboard.ts KPI_SECTIONS),
-- 3–10 records per category. Counts produced, matching DashboardService's
-- actual query logic:
--
--   Rider Summary
--     Registered Riders ......... 6   (users role=VEHICLE_USER, deleted=false)
--     Deleted Riders ............ 3   (deleted=true)
--   Workshop Summary
--     Registered Workshops ...... 10  (role=MECHANIC, deleted=false; 7 active + 3 suspended)
--     Listed Workshops .......... 4   (workshop_verifications LISTED/APPROVED)
--     Platform Trusted Workshops . 3  (workshop_verifications TRUST/APPROVED)
--     Pickup & Drop Enabled ...... 4  (mechanic_configuration_settings)
--     Advance Payment Enabled .... 3  (mechanic_configuration_settings)
--     Suspended Workshops ........ 3  (active=false, deleted=false)
--     Deleted Workshops .......... 3  (deleted=true)
--   Sales Agent Summary
--     Registered Sales Agents .... 8  (role=SALES_REPRESENTATIVE, deleted=false; 5 active + 3 suspended)
--     Suspended Sales Agents ..... 3
--     Deleted Sales Agents ....... 3
--   Booking Summary (not yet wired to a backend query, seeded for the next epic)
--     Today's Scheduled Bookings . 5   (status=CONFIRMED, scheduled today)
--     Today's Cancelled Bookings . 3   (status=CANCELLED, cancelled today)
--   Finance Summary (not yet wired to a backend query, seeded for the next epic)
--     Workshops — Pending Settlement ... 6  (settlements status=PENDING)
--     Workshops — Completed Settlement . 5  (settlements status=PAID)
--   Support Summary (not yet wired to a backend query, seeded for the next epic)
--     Open Complaints ............ 4   (settlement_queries status=OPEN; +2 IN_PROGRESS, +2 RESOLVED for queue variety)
--     New Feedback ................ 5  (reviews, on completed bookings)
--
-- All sample rows are tagged with a distinctive @sample.dev email domain and
-- 90X00000xx mobile-number ranges so they're easy to find and delete later:
--   DELETE FROM users WHERE email LIKE '%@sample.dev';  (cascades per FKs below)
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Riders (users.role = VEHICLE_USER)
-- -----------------------------------------------------------------------------
-- password/hash column is required by the physical `users` table (owned by
-- vehicle-service-booking-api) even though the Admin Portal never reads it.
-- Reusing the same dev-only bcrypt hash used for the admin seed accounts.

INSERT INTO users (email, mobile_number, password, first_name, last_name, role, active, deleted, deleted_at, city, state, area, email_verified, mobile_verified)
VALUES
  ('rider01@sample.dev', '9010000001', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Arjun',   'Mehta',  'VEHICLE_USER', true, false, NULL, 'Bengaluru', 'Karnataka',      'Indiranagar', true, true),
  ('rider02@sample.dev', '9010000002', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Priya',   'Nair',   'VEHICLE_USER', true, false, NULL, 'Chennai',   'Tamil Nadu',     'Anna Nagar',  true, true),
  ('rider03@sample.dev', '9010000003', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Rohit',   'Sharma', 'VEHICLE_USER', true, false, NULL, 'Hyderabad', 'Telangana',      'Banjara Hills', true, true),
  ('rider04@sample.dev', '9010000004', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Sneha',   'Iyer',   'VEHICLE_USER', true, false, NULL, 'Mumbai',    'Maharashtra',    'Andheri',     true, true),
  ('rider05@sample.dev', '9010000005', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Karan',   'Verma',  'VEHICLE_USER', true, false, NULL, 'Pune',      'Maharashtra',    'Kothrud',     true, true),
  ('rider06@sample.dev', '9010000006', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Ananya',  'Rao',    'VEHICLE_USER', true, false, NULL, 'Delhi',     'Delhi',          'Karol Bagh',  true, true),
  ('rider07@sample.dev', '9010000007', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Vikram',  'Singh',  'VEHICLE_USER', false, true, NOW() - INTERVAL '10 days', 'Kolkata',    'West Bengal',  'Salt Lake',   true, true),
  ('rider08@sample.dev', '9010000008', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Meera',   'Joshi',  'VEHICLE_USER', false, true, NOW() - INTERVAL '20 days', 'Coimbatore', 'Tamil Nadu',   'RS Puram',    true, true),
  ('rider09@sample.dev', '9010000009', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Aditya',  'Kapoor', 'VEHICLE_USER', false, true, NOW() - INTERVAL '30 days', 'Ahmedabad',  'Gujarat',      'Navrangpura', true, true);

-- -----------------------------------------------------------------------------
-- 2. Workshops (users.role = MECHANIC) — 7 active, 3 suspended, 3 deleted
-- -----------------------------------------------------------------------------

INSERT INTO users (email, mobile_number, password, first_name, last_name, role, active, deleted, deleted_at, workshop_name, specialization, experience_years, rating, total_reviews, city, state, area, email_verified, mobile_verified)
VALUES
  ('workshop01@sample.dev', '9020000001', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Ramesh',  'Kumar',   'MECHANIC', true, false, NULL, 'Turbo Bike Works',       'General Repair',    12, 4.7, 5, 'Bengaluru',  'Karnataka',    'Indiranagar',   true, true),
  ('workshop02@sample.dev', '9020000002', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Suresh',  'Babu',    'MECHANIC', true, false, NULL, 'Speedy Wheels Garage',   'Engine Diagnostic', 8,  4.3, 2, 'Chennai',    'Tamil Nadu',   'Anna Nagar',    true, true),
  ('workshop03@sample.dev', '9020000003', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Deepak',  'Nair',    'MECHANIC', true, false, NULL, 'City Motor Care',        'Brake Service',     10, 4.5, 0, 'Hyderabad',  'Telangana',    'Banjara Hills', true, true),
  ('workshop04@sample.dev', '9020000004', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Manoj',   'Pillai',  'MECHANIC', true, false, NULL, 'Reliable Auto Service',  'General Service',   15, 4.8, 0, 'Mumbai',     'Maharashtra',  'Andheri',       true, true),
  ('workshop05@sample.dev', '9020000005', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Ajay',    'Menon',   'MECHANIC', true, false, NULL, 'Highway Bike Clinic',    'Tire Service',      6,  4.1, 0, 'Pune',       'Maharashtra',  'Kothrud',       true, true),
  ('workshop06@sample.dev', '9020000006', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Sanjay',  'Reddy',   'MECHANIC', true, false, NULL, 'Green Valley Workshop',  'AC Service',        4,  3.9, 0, 'Delhi',      'Delhi',        'Karol Bagh',    true, true),
  ('workshop07@sample.dev', '9020000007', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Vinod',   'Rao',     'MECHANIC', true, false, NULL, 'Prime Motors',           'General Repair',    9,  4.4, 0, 'Kolkata',    'West Bengal',  'Salt Lake',     true, true),
  ('workshop08@sample.dev', '9020000008', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Farhan',  'Khan',    'MECHANIC', false, false, NULL, 'Metro Bike Station',    'Electrical Repair', 7,  3.6, 0, 'Coimbatore', 'Tamil Nadu',   'RS Puram',      true, true),
  ('workshop09@sample.dev', '9020000009', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Naveen',  'Kumar',   'MECHANIC', false, false, NULL, 'Sunrise Auto Works',    'General Repair',    5,  3.4, 0, 'Ahmedabad',  'Gujarat',      'Navrangpura',   true, true),
  ('workshop10@sample.dev', '9020000010', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Rajesh',  'Iyer',    'MECHANIC', false, false, NULL, 'Elite Garage Hub',      'Body Work',         11, 3.2, 0, 'Jaipur',     'Rajasthan',    'Malviya Nagar', true, true),
  ('workshop11@sample.dev', '9020000011', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Prakash', 'Menon',   'MECHANIC', false, true, NOW() - INTERVAL '40 days', 'Old Town Garage',   'General Repair',    14, 3.0, 0, 'Lucknow',    'Uttar Pradesh','Hazratganj',    true, true),
  ('workshop12@sample.dev', '9020000012', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Anil',    'Kumar',   'MECHANIC', false, true, NOW() - INTERVAL '55 days', 'Budget Bike Fix',   'General Repair',    3,  2.8, 0, 'Nagpur',     'Maharashtra',  'Sitabuldi',     true, true),
  ('workshop13@sample.dev', '9020000013', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Ravi',    'Shankar', 'MECHANIC', false, true, NOW() - INTERVAL '70 days', 'Downtown Motors',   'Tire Service',      6,  2.9, 0, 'Indore',     'Madhya Pradesh','Vijay Nagar',  true, true);

-- -----------------------------------------------------------------------------
-- 3. Sales Agents (users.role = SALES_REPRESENTATIVE) — 5 active, 3 suspended, 3 deleted
-- -----------------------------------------------------------------------------

INSERT INTO users (email, mobile_number, password, first_name, last_name, role, active, deleted, deleted_at, referral_code, total_referrals, city, state, email_verified, mobile_verified)
VALUES
  ('agent01@sample.dev', '9030000001', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Kavya',  'Menon',    'SALES_REPRESENTATIVE', true, false, NULL, 'REF-SAG-01', 14, 'Bengaluru', 'Karnataka',   true, true),
  ('agent02@sample.dev', '9030000002', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Rahul',  'Desai',    'SALES_REPRESENTATIVE', true, false, NULL, 'REF-SAG-02', 9,  'Ahmedabad', 'Gujarat',     true, true),
  ('agent03@sample.dev', '9030000003', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Neha',   'Kulkarni', 'SALES_REPRESENTATIVE', true, false, NULL, 'REF-SAG-03', 21, 'Pune',      'Maharashtra', true, true),
  ('agent04@sample.dev', '9030000004', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Amit',   'Trivedi',  'SALES_REPRESENTATIVE', true, false, NULL, 'REF-SAG-04', 6,  'Jaipur',    'Rajasthan',   true, true),
  ('agent05@sample.dev', '9030000005', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Pooja',  'Bhatt',    'SALES_REPRESENTATIVE', true, false, NULL, 'REF-SAG-05', 17, 'Lucknow',   'Uttar Pradesh', true, true),
  ('agent06@sample.dev', '9030000006', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Sameer', 'Gupta',    'SALES_REPRESENTATIVE', false, false, NULL, 'REF-SAG-06', 4, 'Nagpur',    'Maharashtra', true, true),
  ('agent07@sample.dev', '9030000007', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Divya',  'Pillai',   'SALES_REPRESENTATIVE', false, false, NULL, 'REF-SAG-07', 2, 'Indore',    'Madhya Pradesh', true, true),
  ('agent08@sample.dev', '9030000008', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Yash',   'Malhotra', 'SALES_REPRESENTATIVE', false, false, NULL, 'REF-SAG-08', 1, 'Kolkata',   'West Bengal', true, true),
  ('agent09@sample.dev', '9030000009', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Ritu',   'Chawla',   'SALES_REPRESENTATIVE', false, true, NOW() - INTERVAL '15 days', 'REF-SAG-09', 3, 'Chennai',    'Tamil Nadu', true, true),
  ('agent10@sample.dev', '9030000010', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Gaurav', 'Sethi',    'SALES_REPRESENTATIVE', false, true, NOW() - INTERVAL '25 days', 'REF-SAG-10', 5, 'Mumbai',     'Maharashtra', true, true),
  ('agent11@sample.dev', '9030000011', '$2b$10$gcuFyTBv.jioJhys9vUo1udEvaalB/PxKrCx1n0jQ9dr2QK1RVwWm', 'Isha',   'Bansal',   'SALES_REPRESENTATIVE', false, true, NOW() - INTERVAL '35 days', 'REF-SAG-11', 0, 'Hyderabad',  'Telangana', true, true);

-- -----------------------------------------------------------------------------
-- 4. Workshop verifications (ADMIN-US-06) — only on the 7 active workshops
-- -----------------------------------------------------------------------------
-- Listed/APPROVED -> WS1,WS2,WS3,WS5 (4)   Trust/APPROVED -> WS1,WS4,WS7 (3)

INSERT INTO workshop_verifications (mechanic_id, verification_type, status, requested_at, decision_reason, decided_by_admin_id, decided_at)
VALUES
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), 'LISTED', 'APPROVED', NOW() - INTERVAL '25 days', 'Meets Listed criteria.',   (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '20 days'),
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), 'TRUST',  'APPROVED', NOW() - INTERVAL '25 days', 'Meets Platform Trust bar.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '18 days'),

  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), 'LISTED', 'APPROVED', NOW() - INTERVAL '22 days', 'Meets Listed criteria.',   (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '19 days'),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), 'TRUST',  'PENDING',  NOW() - INTERVAL '22 days', NULL, NULL, NULL),

  ((SELECT id FROM users WHERE email = 'workshop03@sample.dev'), 'LISTED', 'APPROVED', NOW() - INTERVAL '20 days', 'Meets Listed criteria.',   (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '17 days'),

  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), 'LISTED', 'PENDING',  NOW() - INTERVAL '5 days',  NULL, NULL, NULL),
  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), 'TRUST',  'APPROVED', NOW() - INTERVAL '18 days', 'Meets Platform Trust bar.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '15 days'),

  ((SELECT id FROM users WHERE email = 'workshop05@sample.dev'), 'LISTED', 'APPROVED', NOW() - INTERVAL '16 days', 'Meets Listed criteria.',   (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '13 days'),
  ((SELECT id FROM users WHERE email = 'workshop05@sample.dev'), 'TRUST',  'REJECTED', NOW() - INTERVAL '16 days', 'Insufficient completed bookings for Trust tier.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '12 days'),

  ((SELECT id FROM users WHERE email = 'workshop06@sample.dev'), 'LISTED', 'ADDITIONAL_INFO_REQUESTED', NOW() - INTERVAL '3 days', 'Please resubmit ownership proof.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '1 days'),

  ((SELECT id FROM users WHERE email = 'workshop07@sample.dev'), 'LISTED', 'REJECTED', NOW() - INTERVAL '14 days', 'Duplicate registration.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '11 days'),
  ((SELECT id FROM users WHERE email = 'workshop07@sample.dev'), 'TRUST',  'APPROVED', NOW() - INTERVAL '14 days', 'Meets Platform Trust bar.', (SELECT id FROM admin_users WHERE email = 'superadmin@alvexo.com'), NOW() - INTERVAL '10 days');

-- -----------------------------------------------------------------------------
-- 5. Mechanic configuration settings — Pickup&Drop / Advance-Payment toggles
-- -----------------------------------------------------------------------------
-- Pickup&Drop enabled -> WS1,WS2,WS4,WS7 (4)   Advance enabled -> WS1,WS3,WS4 (3)
-- (WS6 deliberately has no row here — "never touched settings" = both off.)

INSERT INTO mechanic_configuration_settings (mechanic_id, pickup_drop_facility_enabled, repairs_require_advance)
VALUES
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), true,  true),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), true,  false),
  ((SELECT id FROM users WHERE email = 'workshop03@sample.dev'), false, true),
  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), true,  true),
  ((SELECT id FROM users WHERE email = 'workshop05@sample.dev'), false, false),
  ((SELECT id FROM users WHERE email = 'workshop07@sample.dev'), true,  false);

-- -----------------------------------------------------------------------------
-- 6. Bookings — Today's Scheduled (5), Today's Cancelled (3), plus 5 completed
--    bookings (past few days) to attach reviews to in section 8.
-- -----------------------------------------------------------------------------
-- vehicle_id cycles through the existing vehicle catalogue by offset.

INSERT INTO bookings (vehicle_user_id, mechanic_id, vehicle_id, booking_number, scheduled_date_time, status, service_type, booking_type, channel, description, pickup_required, drop_required)
VALUES
  -- Today's Scheduled Bookings (5)
  ((SELECT id FROM users WHERE email = 'rider01@sample.dev'), (SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 0), 'SMP-BKG-0001', CURRENT_DATE + TIME '09:00', 'CONFIRMED', 'GENERAL_SERVICE',    'STANDARD', 'ONLINE',    'Routine service and inspection.', false, false),
  ((SELECT id FROM users WHERE email = 'rider02@sample.dev'), (SELECT id FROM users WHERE email = 'workshop02@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 1), 'SMP-BKG-0002', CURRENT_DATE + TIME '10:30', 'CONFIRMED', 'OIL_CHANGE',         'EXPRESS',  'RIDER_APP', 'Engine oil replacement requested.', false, false),
  ((SELECT id FROM users WHERE email = 'rider03@sample.dev'), (SELECT id FROM users WHERE email = 'workshop03@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 2), 'SMP-BKG-0003', CURRENT_DATE + TIME '12:00', 'CONFIRMED', 'BRAKE_SERVICE',      'STANDARD', 'WALK_IN',   'Front brake pads need replacement.', false, false),
  ((SELECT id FROM users WHERE email = 'rider04@sample.dev'), (SELECT id FROM users WHERE email = 'workshop04@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 3), 'SMP-BKG-0004', CURRENT_DATE + TIME '14:30', 'CONFIRMED', 'TIRE_SERVICE',       'STANDARD', 'ONLINE',    'Rear tyre puncture repair.', true,  true),
  ((SELECT id FROM users WHERE email = 'rider05@sample.dev'), (SELECT id FROM users WHERE email = 'workshop05@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 4), 'SMP-BKG-0005', CURRENT_DATE + TIME '16:00', 'CONFIRMED', 'ENGINE_DIAGNOSTIC',  'STANDARD', 'RIDER_APP', 'Unusual engine noise, needs diagnosis.', false, false),

  -- Completed bookings, feeding Reviews in section 8 (5)
  ((SELECT id FROM users WHERE email = 'rider01@sample.dev'), (SELECT id FROM users WHERE email = 'workshop02@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 0), 'SMP-BKG-0006', CURRENT_DATE - INTERVAL '2 days' + TIME '09:00', 'COMPLETED', 'GENERAL_SERVICE', 'STANDARD', 'ONLINE',  'Routine service and inspection.', false, false),
  ((SELECT id FROM users WHERE email = 'rider02@sample.dev'), (SELECT id FROM users WHERE email = 'workshop03@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 1), 'SMP-BKG-0007', CURRENT_DATE - INTERVAL '3 days' + TIME '11:00', 'COMPLETED', 'OIL_CHANGE',       'EXPRESS',  'RIDER_APP', 'Engine oil replacement.', false, false),
  ((SELECT id FROM users WHERE email = 'rider03@sample.dev'), (SELECT id FROM users WHERE email = 'workshop04@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 2), 'SMP-BKG-0008', CURRENT_DATE - INTERVAL '4 days' + TIME '13:00', 'COMPLETED', 'BRAKE_SERVICE',    'STANDARD', 'WALK_IN',   'Brake pad replacement.', false, false),
  ((SELECT id FROM users WHERE email = 'rider04@sample.dev'), (SELECT id FROM users WHERE email = 'workshop05@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 3), 'SMP-BKG-0009', CURRENT_DATE - INTERVAL '5 days' + TIME '15:00', 'COMPLETED', 'TIRE_SERVICE',     'STANDARD', 'ONLINE',    'Tyre replacement.', true, false),
  ((SELECT id FROM users WHERE email = 'rider05@sample.dev'), (SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 4), 'SMP-BKG-0010', CURRENT_DATE - INTERVAL '6 days' + TIME '10:00', 'COMPLETED', 'ENGINE_DIAGNOSTIC','STANDARD', 'RIDER_APP', 'Engine diagnostic and tune-up.', false, false);

UPDATE bookings SET completed_at = scheduled_date_time + INTERVAL '90 minutes', actual_cost = 850.00
WHERE booking_number IN ('SMP-BKG-0006','SMP-BKG-0007','SMP-BKG-0008','SMP-BKG-0009','SMP-BKG-0010');

INSERT INTO bookings (vehicle_user_id, mechanic_id, vehicle_id, booking_number, scheduled_date_time, status, service_type, booking_type, channel, description, cancellation_reason, cancelled_by, cancelled_at)
VALUES
  -- Today's Cancelled Bookings (3)
  ((SELECT id FROM users WHERE email = 'rider06@sample.dev'), (SELECT id FROM users WHERE email = 'workshop06@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 0), 'SMP-BKG-0011', CURRENT_DATE + TIME '09:30', 'CANCELLED', 'GENERAL_SERVICE', 'STANDARD', 'ONLINE',  'Routine service and inspection.', 'Rider changed plans.',        (SELECT id FROM users WHERE email = 'rider06@sample.dev'), CURRENT_DATE + TIME '08:00'),
  ((SELECT id FROM users WHERE email = 'rider01@sample.dev'), (SELECT id FROM users WHERE email = 'workshop07@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 1), 'SMP-BKG-0012', CURRENT_DATE + TIME '11:30', 'CANCELLED', 'OIL_CHANGE',       'EXPRESS',  'RIDER_APP', 'Engine oil replacement requested.', 'Found a closer workshop.', (SELECT id FROM users WHERE email = 'rider01@sample.dev'), CURRENT_DATE + TIME '09:15'),
  ((SELECT id FROM users WHERE email = 'rider02@sample.dev'), (SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM vehicles ORDER BY id LIMIT 1 OFFSET 2), 'SMP-BKG-0013', CURRENT_DATE + TIME '17:00', 'CANCELLED', 'AC_SERVICE',       'STANDARD', 'WALK_IN',   'AC not cooling, needs check.', 'Workshop unavailable at slot.', (SELECT id FROM users WHERE email = 'workshop01@sample.dev'), CURRENT_DATE + TIME '13:45');

-- -----------------------------------------------------------------------------
-- 7. Settlements — Pending (6), Paid (5)
-- -----------------------------------------------------------------------------

INSERT INTO settlements (mechanic_id, settlement_date, net_pay, settlement_released, status)
VALUES
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 12500.00, 0, 'PENDING'),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 8300.00,  0, 'PENDING'),
  ((SELECT id FROM users WHERE email = 'workshop03@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 9600.00,  0, 'PENDING'),
  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 15200.00, 0, 'PENDING'),
  ((SELECT id FROM users WHERE email = 'workshop05@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 6100.00,  0, 'PENDING'),
  ((SELECT id FROM users WHERE email = 'workshop06@sample.dev'), CURRENT_DATE - INTERVAL '7 days', 4400.00,  0, 'PENDING');

INSERT INTO settlements (mechanic_id, settlement_date, net_pay, settlement_released, status, transaction_number, payment_gateway, payment_gateway_reference, payment_date, payment_time, payment_status)
VALUES
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), CURRENT_DATE - INTERVAL '14 days', 11800.00, 11800.00, 'PAID', 'TXN-SMP-0001', 'Razorpay', 'rzp_ref_smp_0001', CURRENT_DATE - INTERVAL '13 days', TIME '18:00', 'SUCCESS'),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), CURRENT_DATE - INTERVAL '14 days', 7900.00,  7900.00,  'PAID', 'TXN-SMP-0002', 'Razorpay', 'rzp_ref_smp_0002', CURRENT_DATE - INTERVAL '13 days', TIME '18:05', 'SUCCESS'),
  ((SELECT id FROM users WHERE email = 'workshop03@sample.dev'), CURRENT_DATE - INTERVAL '14 days', 9000.00,  9000.00,  'PAID', 'TXN-SMP-0003', 'Razorpay', 'rzp_ref_smp_0003', CURRENT_DATE - INTERVAL '13 days', TIME '18:10', 'SUCCESS'),
  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), CURRENT_DATE - INTERVAL '14 days', 14100.00, 14100.00, 'PAID', 'TXN-SMP-0004', 'Razorpay', 'rzp_ref_smp_0004', CURRENT_DATE - INTERVAL '13 days', TIME '18:15', 'SUCCESS'),
  ((SELECT id FROM users WHERE email = 'workshop07@sample.dev'), CURRENT_DATE - INTERVAL '14 days', 5600.00,  5600.00,  'PAID', 'TXN-SMP-0005', 'Razorpay', 'rzp_ref_smp_0005', CURRENT_DATE - INTERVAL '13 days', TIME '18:20', 'SUCCESS');

-- -----------------------------------------------------------------------------
-- 8. Settlement queries — "Open Complaints" (4 OPEN, +2 IN_PROGRESS, +2 RESOLVED)
-- -----------------------------------------------------------------------------

INSERT INTO settlement_queries (mechanic_id, settlement_id, reference_id, description, status)
VALUES
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop01@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0001', 'Net pay looks lower than expected for this cycle.', 'OPEN'),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop02@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0002', 'Missing reliability adjustment for a cancelled booking.', 'OPEN'),
  ((SELECT id FROM users WHERE email = 'workshop03@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop03@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0003', 'One completed booking missing from this settlement.', 'OPEN'),
  ((SELECT id FROM users WHERE email = 'workshop04@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop04@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0004', 'Requesting breakdown of the reliability adjustment.', 'OPEN'),
  ((SELECT id FROM users WHERE email = 'workshop05@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop05@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0005', 'Query on advance-payment deduction amount.', 'IN_PROGRESS'),
  ((SELECT id FROM users WHERE email = 'workshop06@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop06@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '7 days'), 'SQ-SMP-0006', 'Asking for gateway reference for this cycle.', 'IN_PROGRESS'),
  ((SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop01@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '14 days'), 'SQ-SMP-0007', 'Confirmed payout amount matched, closing query.', 'RESOLVED'),
  ((SELECT id FROM users WHERE email = 'workshop02@sample.dev'), (SELECT id FROM settlements WHERE mechanic_id = (SELECT id FROM users WHERE email = 'workshop02@sample.dev') AND settlement_date = CURRENT_DATE - INTERVAL '14 days'), 'SQ-SMP-0008', 'Transaction reference shared, closing query.', 'RESOLVED');

-- -----------------------------------------------------------------------------
-- 9. Reviews — "New Feedback" (5), one per completed booking from section 6
-- -----------------------------------------------------------------------------

INSERT INTO reviews (booking_id, mechanic_id, vehicle_user_id, rating, comment)
VALUES
  ((SELECT id FROM bookings WHERE booking_number = 'SMP-BKG-0006'), (SELECT id FROM users WHERE email = 'workshop02@sample.dev'), (SELECT id FROM users WHERE email = 'rider01@sample.dev'), 5, 'Quick and thorough service, very happy.'),
  ((SELECT id FROM bookings WHERE booking_number = 'SMP-BKG-0007'), (SELECT id FROM users WHERE email = 'workshop03@sample.dev'), (SELECT id FROM users WHERE email = 'rider02@sample.dev'), 4, 'Good service, slightly delayed start.'),
  ((SELECT id FROM bookings WHERE booking_number = 'SMP-BKG-0008'), (SELECT id FROM users WHERE email = 'workshop04@sample.dev'), (SELECT id FROM users WHERE email = 'rider03@sample.dev'), 5, 'Brake work done perfectly, highly recommend.'),
  ((SELECT id FROM bookings WHERE booking_number = 'SMP-BKG-0009'), (SELECT id FROM users WHERE email = 'workshop05@sample.dev'), (SELECT id FROM users WHERE email = 'rider04@sample.dev'), 3, 'Decent job but pickup was late.'),
  ((SELECT id FROM bookings WHERE booking_number = 'SMP-BKG-0010'), (SELECT id FROM users WHERE email = 'workshop01@sample.dev'), (SELECT id FROM users WHERE email = 'rider05@sample.dev'), 5, 'Diagnosed the issue correctly on the first try.');

COMMIT;

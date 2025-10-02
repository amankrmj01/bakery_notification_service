-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create notification types enum
CREATE TYPE notification_type AS ENUM (
    'EMAIL', 'SMS', 'PUSH', 'IN_APP'
);

-- Create notification status enum
CREATE TYPE notification_status AS ENUM (
    'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED', 'CANCELLED'
);

-- Create notification priority enum
CREATE TYPE notification_priority AS ENUM (
    'LOW', 'NORMAL', 'HIGH', 'URGENT'
);

-- Create template type enum
CREATE TYPE template_type AS ENUM (
    'ORDER_CONFIRMATION', 'ORDER_STATUS_UPDATE', 'DELIVERY_NOTIFICATION',
    'CART_ABANDONMENT', 'MARKETING_CAMPAIGN', 'PASSWORD_RESET',
    'WELCOME', 'RECEIPT', 'FEEDBACK_REQUEST', 'PROMOTION'
);

-- Ensure proper permissions
GRANT ALL PRIVILEGES ON DATABASE bakery_notifications TO notification_user;

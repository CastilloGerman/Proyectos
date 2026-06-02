-- Railway → PostgreSQL → Query: ejecuta TODO el bloque de una vez.
-- (Varios SELECT con distinto nº de columnas rompen el parser de Railway.)

UPDATE usuarios
SET subscription_status = 'ACTIVE',
    stripe_customer_id = NULL,
    stripe_subscription_id = NULL,
    subscription_current_period_end = NULL,
    stripe_price_id = NULL,
    subscription_cancel_at_period_end = FALSE,
    subscription_requires_payment_action = FALSE
WHERE email = 'germancastillodkno@gmail.com';

SELECT id,
       email,
       subscription_status,
       trial_end_date,
       stripe_subscription_id
FROM usuarios
WHERE email = 'germancastillodkno@gmail.com';

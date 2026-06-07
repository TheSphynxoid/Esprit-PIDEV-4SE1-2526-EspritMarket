ALTER TABLE booking
    DROP CONSTRAINT IF EXISTS booking_status_check;

ALTER TABLE booking
    ADD CONSTRAINT booking_status_check
    CHECK (
        status IN (
            'PENDING',
            'PENDING_EVALUATION',
            'TENTATIVE',
            'APPROVED',
            'CONFIRMED',
            'REJECTED',
            'IN_PROGRESS',
            'PENDING_REVIEW',
            'COMPLETED',
            'CANCELLED',
            'DISPUTED'
        )
    );

-- Execute this in the Supabase SQL Editor

CREATE TABLE IF NOT EXISTS public.society_fund_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount DOUBLE PRECISION NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ADDITION', 'DEDUCTION')),
    reference_id UUID, -- Optional, links to foreign key like expense_id or payment_submission_id
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID REFERENCES public.profiles(id)
);

-- Enable RLS
ALTER TABLE public.society_fund_audit ENABLE ROW LEVEL SECURITY;

-- Allow read access to authenticated users
CREATE POLICY "Allow read access to authenticated users" 
ON public.society_fund_audit 
FOR SELECT 
TO authenticated 
USING (true);

-- Allow admins to insert
CREATE POLICY "Allow insert access to admins" 
ON public.society_fund_audit 
FOR INSERT 
TO authenticated 
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.profiles 
        WHERE profiles.id = auth.uid() AND profiles.role = 'ADMIN'
    )
);

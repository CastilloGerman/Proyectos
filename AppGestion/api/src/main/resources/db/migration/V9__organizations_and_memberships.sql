-- Organización (tenant B2B) y membresías; cada usuario existente recibe una organización "Personal".

CREATE TABLE IF NOT EXISTS public.organizations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.organization_members (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES public.usuarios(id) ON DELETE CASCADE,
    role             VARCHAR(50)  NOT NULL DEFAULT 'OWNER',
    UNIQUE (organization_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_organization_members_user_id ON public.organization_members(user_id);
CREATE INDEX IF NOT EXISTS idx_organization_members_org_id ON public.organization_members(organization_id);

ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS organization_id BIGINT REFERENCES public.organizations(id);

DO $$
DECLARE
    r RECORD;
    new_org_id BIGINT;
BEGIN
    FOR r IN SELECT id FROM public.usuarios WHERE organization_id IS NULL LOOP
        INSERT INTO public.organizations (name, created_at)
        VALUES ('Personal', NOW())
        RETURNING id INTO new_org_id;

        INSERT INTO public.organization_members (organization_id, user_id, role)
        VALUES (new_org_id, r.id, 'OWNER');

        UPDATE public.usuarios SET organization_id = new_org_id WHERE id = r.id;
    END LOOP;
END $$;

ALTER TABLE public.usuarios
    ALTER COLUMN organization_id SET NOT NULL;

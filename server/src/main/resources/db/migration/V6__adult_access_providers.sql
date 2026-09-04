-- La liste d'accès 18+ ne connaissait que Discord. Twitch s'y ajoute, et comme les deux
-- fournisseurs distribuent de simples nombres, un identifiant n'a de sens qu'accompagné
-- du fournisseur qui l'a émis : la clé primaire devient le couple. Les entrées existantes
-- sont, par construction, des comptes Discord.

ALTER TABLE adult_pack_access
    ADD COLUMN provider VARCHAR(16) NOT NULL DEFAULT 'DISCORD';

ALTER TABLE adult_pack_access
    RENAME COLUMN discord_id TO account_id;

ALTER TABLE adult_pack_access
    DROP CONSTRAINT pk_adult_pack_access;

ALTER TABLE adult_pack_access
    ADD CONSTRAINT pk_adult_pack_access PRIMARY KEY (provider, account_id);

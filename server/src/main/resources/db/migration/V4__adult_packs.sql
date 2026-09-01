-- Un pack peut désormais être marqué « interdit aux mineurs ». Ces packs ne sont
-- proposés qu'aux hôtes connectés avec Discord dont l'identifiant figure dans la
-- liste d'accès ci-dessous (les administrateurs y ont droit d'office). Les packs
-- existants restent tout public (valeur par défaut).

ALTER TABLE card_packs
    ADD COLUMN adult_only BOOLEAN NOT NULL DEFAULT FALSE;

-- Identifiants Discord autorisés à voir et sélectionner les packs 18+.
-- Gérée depuis la section administrateur.
CREATE TABLE adult_pack_access (
    discord_id      VARCHAR(64)  NOT NULL,
    label           VARCHAR(120) NOT NULL DEFAULT '',
    added_at_millis BIGINT       NOT NULL,
    CONSTRAINT pk_adult_pack_access PRIMARY KEY (discord_id)
);

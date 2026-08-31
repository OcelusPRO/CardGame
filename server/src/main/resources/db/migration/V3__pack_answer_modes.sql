-- Un pack peut désormais être réservé à certains modes de réponse : « Cartes
-- distribuées » et/ou « Sans limites ». Les packs existants restent utilisables
-- dans les deux modes (valeur par défaut).

ALTER TABLE card_packs
    ADD COLUMN answer_mode_cards     BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE card_packs
    ADD COLUMN answer_mode_free_text BOOLEAN NOT NULL DEFAULT TRUE;

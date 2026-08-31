-- Combien de fois une carte réponse est arrivée dans la main d'un joueur, en plus
-- des fois où elle a été jouée. Les parties déjà comptées repartent de zéro sur ce
-- compteur : l'information n'existait pas avant.

ALTER TABLE card_usage
    ADD COLUMN deals BIGINT NOT NULL DEFAULT 0;

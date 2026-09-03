-- Un pack peut désormais porter un « code secret ». Tant qu'un code est renseigné,
-- le pack est masqué de la liste des decks du salon et n'est jamais activé par défaut :
-- l'hôte le débloque pour sa partie en écrivant le code sur une ligne de la zone
-- « Vos situations ». La ligne du code n'entre pas dans le jeu comme une carte.
-- Les packs existants restent visibles (code nul = pack public).

ALTER TABLE card_packs
    ADD COLUMN secret_code VARCHAR(64);

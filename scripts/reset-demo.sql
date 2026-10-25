SET NAMES latin1;

-- ---------------------------------------------------------------------------
-- 0. Referentiel des 10 livreurs de la demonstration
--    MySQL 5.6 interdit DELETE ... WHERE x NOT IN (SELECT sur la meme table) :
--    le referentiel est donc materialise dans une table temporaire, utilise
--    pour ne supprimer que nos livreurs de demonstration (jamais ceux d'un
--    utilisateur, ni ceux references par une commande).
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_livreurs;
CREATE TEMPORARY TABLE tmp_demo_livreurs (
  prenom VARCHAR(60) NOT NULL,
  nom    VARCHAR(60) NOT NULL,
  PRIMARY KEY (prenom, nom)
) ENGINE=InnoDB;

INSERT INTO tmp_demo_livreurs (prenom, nom) VALUES
 ('Naina','Rakotoarisoa'),
 ('Fandresena','Rasoanaivo'),
 ('Mamy','Randriamampionona'),
 ('Tiana','Razafindrakoto'),
 ('Haja','Ratsimihandrina'),
 ('Faniry','Rakotondramanana'),
 ('Tovo','Andrianjaka'),
 ('Lova','Rabetokotany'),
 ('Miora','Ranaivoson'),
 ('Tojo','Rasoloarison');

-- ---------------------------------------------------------------------------
-- 1. Purge des donnees de demonstration
--    Ordre impose par les cles etrangeres :
--    historique_statuts -> incidents -> colis -> commandes -> livreurs
-- ---------------------------------------------------------------------------
DELETE FROM historique_statuts
 WHERE commande_id IN (SELECT id FROM commandes
                        WHERE numero LIKE 'DEMO-%' OR numero LIKE 'API-%');

DELETE FROM incidents
 WHERE commande_id IN (SELECT id FROM commandes
                        WHERE numero LIKE 'DEMO-%' OR numero LIKE 'API-%');

DELETE FROM colis
 WHERE commande_id IN (SELECT id FROM commandes
                        WHERE numero LIKE 'DEMO-%' OR numero LIKE 'API-%');

DELETE FROM commandes
 WHERE numero LIKE 'DEMO-%' OR numero LIKE 'API-%';

-- Supprime nos livreurs de demonstration uniquement lorsqu'ils ne sont plus
-- rattaches a une commande, un incident ou un compte utilisateur.
DELETE l FROM livreurs l
  JOIN tmp_demo_livreurs d
    ON d.prenom = l.prenom AND d.nom = l.nom
 WHERE NOT EXISTS (SELECT 1 FROM commandes c WHERE c.livreur_id = l.id)
   AND NOT EXISTS (SELECT 1 FROM incidents i WHERE i.livreur_id = l.id)
   AND NOT EXISTS (SELECT 1 FROM utilisateurs u WHERE u.livreur_id = l.id);

-- ---------------------------------------------------------------------------
-- 2. Baremes de facturation (9 zones malgaches)
--    Une ligne par zone ; TOUTES sert de repli pour les zones sans bareme.
--    Les colonnes doivent rester alignees sur l'entite Tarif.
-- ---------------------------------------------------------------------------
INSERT INTO tarifs (zone, prix_fixe, prix_par_km, prix_par_kg, prix_par_m3,
                    majoration_urgente_pct, majoration_express_pct) VALUES
  ('TOUTES',      8000.00, 1500.00, 2000.00, 3000.00, 15.00, 30.00),
  ('ambatomainty',7500.00, 1400.00, 1900.00, 2800.00, 15.00, 30.00),
  ('ivato',       9000.00, 1800.00, 2400.00, 3500.00, 15.00, 30.00),
  ('anosy',       7000.00, 1300.00, 1800.00, 2600.00, 15.00, 30.00),
  ('isoraka',     7000.00, 1200.00, 1800.00, 2500.00, 15.00, 30.00),
  ('mahamasina',  7500.00, 1350.00, 1850.00, 2700.00, 15.00, 30.00),
  ('ankorondrano',8500.00, 1600.00, 2200.00, 3200.00, 15.00, 30.00),
  ('itaosy',      8200.00, 1550.00, 2100.00, 3100.00, 15.00, 30.00),
  ('ivandry',     8800.00, 1700.00, 2300.00, 3300.00, 15.00, 30.00)
ON DUPLICATE KEY UPDATE
  prix_fixe = VALUES(prix_fixe), prix_par_km = VALUES(prix_par_km),
  prix_par_kg = VALUES(prix_par_kg), prix_par_m3 = VALUES(prix_par_m3),
  majoration_urgente_pct = VALUES(majoration_urgente_pct),
  majoration_express_pct = VALUES(majoration_express_pct);

-- ---------------------------------------------------------------------------
-- 3. Clients de demonstration (email = cle unique)
--    Noms, prenoms et quartiers malgaches d'Antananarivo.
-- ---------------------------------------------------------------------------
INSERT INTO clients (prenom, nom, telephone, email, adresse) VALUES
 ('Fanja','Rakotomalala',   '+261 34 12 345 67', 'demo.fanja.rakotomalala@exemple.mg',  'Lot II M 45 Bis Analakely, Antananarivo 101'),
 ('Tiana','Rasoanaivo',     '+261 32 45 678 90', 'tiana.rasoanaivo@exemple.mg',         'Rue Rainandriamampandry, Ampefiloha, Antananarivo 101'),
 ('Hanta','Randrianasolo',  '+261 33 78 901 23', 'hanta.randrianasolo@exemple.mg',      'Immeuble Fiaro, Ankadifotsy, Antananarivo 101'),
 ('Mamy','Ratsimbazafy',    '+261 34 56 789 01', 'mamy.ratsimbazafy@exemple.mg',        'Lot IVK 27 Ter Ambohijatovo, Antananarivo 101'),
 ('Toky','Andrianina',      '+261 32 98 765 43', 'toky.andrianina@exemple.mg',          'Villa Soafaniry, Ivato, Antananarivo 105'),
 ('Lalao','Rakotoarivelo',  '+261 34 22 334 55', 'lalao.rakotoarivelo@exemple.mg',      'Lot 12 Ankorondrano, Antananarivo 101'),
 ('Nirina','Razafimahatratra','+261 33 44 556 66','nirina.razafimahatratra@exemple.mg', 'Cite Ambohipo, Antananarivo 101'),
 ('Vonjy','Randrianarisoa', '+261 32 66 778 88', 'vonjy.randrianarisoa@exemple.mg',     'Lot 45 Ivandry, Antananarivo 101')
ON DUPLICATE KEY UPDATE
 prenom = VALUES(prenom), nom = VALUES(nom), telephone = VALUES(telephone),
 adresse = VALUES(adresse);

-- ---------------------------------------------------------------------------
-- 4. Livreurs de demonstration (les 4 statuts du referentiel sont couverts)
--    Insertion simple : les anciens livreurs de demo non references ont ete
--    supprimes a la section 1.
-- ---------------------------------------------------------------------------
INSERT INTO livreurs
 (prenom, nom, telephone, zone_activite, capacite_max, vehicule_type, vehicule_immatriculation, vehicule_capacite, statut, latitude, longitude)
VALUES
 ('Naina','Rakotoarisoa',     '+261 34 11 223 45','ambatomainty', 25.00,'Moto',       '1842 ABC',25.00,'DISPONIBLE',   -18.9130,47.5220),
 ('Fandresena','Rasoanaivo',  '+261 33 22 334 56','ivato',        40.00,'Voiture',    '5078 BCD',40.00,'EN_LIVRAISON', -18.9250,47.5300),
 ('Mamy','Randriamampionona', '+261 32 44 556 67','anosy',        18.00,'Moto',       '7312 EFG',18.00,'EN_LIVRAISON', -18.9180,47.5210),
 ('Tiana','Razafindrakoto',   '+261 34 66 778 89','isoraka',      30.00,'Velivole',   '9654 HIJ',30.00,'PAUSE',        -18.8950,47.5400),
 ('Haja','Ratsimihandrina',   '+261 33 87 654 32','mahamasina',   20.00,'Voiture',    '2468 KLM',20.00,'DISPONIBLE',   -18.9000,47.5120),
 ('Faniry','Rakotondramanana','+261 34 23 887 21','ivandry',      22.00,'Moto',       '1122 MNO',22.00,'DISPONIBLE',   -18.8780,47.5180),
 ('Tovo','Andrianjaka',       '+261 32 55 443 77','ambohijatovo', 35.00,'Voiture',    '3344 PQR',35.00,'INDISPONIBLE', -18.9130,47.5310),
 ('Lova','Rabetokotany',      '+261 33 11 998 44','itaosy',       15.00,'Moto',       '5566 STU',15.00,'DISPONIBLE',   -18.9230,47.5320),
 ('Miora','Ranaivoson',       '+261 34 77 665 33','ankorondrano', 60.00,'Camionnette','7788 VWX',60.00,'DISPONIBLE',   -18.8940,47.5230),
 ('Tojo','Rasoloarison',      '+261 32 33 221 99','ambatomainty', 28.00,'Moto',       '9900 YZA',28.00,'EN_LIVRAISON', -18.9150,47.5250);

-- ---------------------------------------------------------------------------
-- 5. Identifiants resolus une fois pour toutes
-- ---------------------------------------------------------------------------
SET @c_fanja  = (SELECT id FROM clients WHERE email='demo.fanja.rakotomalala@exemple.mg');
SET @c_tiana  = (SELECT id FROM clients WHERE email='tiana.rasoanaivo@exemple.mg');
SET @c_hanta  = (SELECT id FROM clients WHERE email='hanta.randrianasolo@exemple.mg');
SET @c_mamy   = (SELECT id FROM clients WHERE email='mamy.ratsimbazafy@exemple.mg');
SET @c_toky   = (SELECT id FROM clients WHERE email='toky.andrianina@exemple.mg');
SET @c_lalao  = (SELECT id FROM clients WHERE email='lalao.rakotoarivelo@exemple.mg');
SET @c_nirina = (SELECT id FROM clients WHERE email='nirina.razafimahatratra@exemple.mg');
SET @c_vonjy  = (SELECT id FROM clients WHERE email='vonjy.randrianarisoa@exemple.mg');

SET @l_naina     = (SELECT id FROM livreurs WHERE prenom='Naina'      AND nom='Rakotoarisoa');
SET @l_fandresena= (SELECT id FROM livreurs WHERE prenom='Fandresena' AND nom='Rasoanaivo');
SET @l_mamy      = (SELECT id FROM livreurs WHERE prenom='Mamy'       AND nom='Randriamampionona');
SET @l_tiana     = (SELECT id FROM livreurs WHERE prenom='Tiana'      AND nom='Razafindrakoto');
SET @l_haja      = (SELECT id FROM livreurs WHERE prenom='Haja'       AND nom='Ratsimihandrina');
SET @l_faniry    = (SELECT id FROM livreurs WHERE prenom='Faniry'     AND nom='Rakotondramanana');
SET @l_tovo      = (SELECT id FROM livreurs WHERE prenom='Tovo'       AND nom='Andrianjaka');
SET @l_lova      = (SELECT id FROM livreurs WHERE prenom='Lova'       AND nom='Rabetokotany');
SET @l_miora     = (SELECT id FROM livreurs WHERE prenom='Miora'      AND nom='Ranaivoson');
SET @l_tojo      = (SELECT id FROM livreurs WHERE prenom='Tojo'       AND nom='Rasoloarison');

-- ---------------------------------------------------------------------------
-- 6. Commandes : les 8 etats du workflow (16 commandes)
--    CREEE(001,002) ASSIGNEE(003,004) PRISE_EN_CHARGE(005,006)
--    EN_TRANSIT(007,008) LIVREE(009,010,011) ANNULEE(012,013)
--    ECHEC_LIVRAISON(014,015) RETOURNEE(016)
-- ---------------------------------------------------------------------------
INSERT INTO commandes
 (numero, client_id, livreur_id, statut, priorite, poids, volume, prix, distance,
  zone_livraison, adresse_enlevement, adresse_livraison,
  latitude_enlevement, longitude_enlevement, latitude_livraison, longitude_livraison,
  date_creation, date_miseajour, date_prevue,
  preuve_date_heure, preuve_nom_receptionnaire, preuve_code_confirmation, preuve_commentaire)
VALUES
 -- 001 CREEE
 ('DEMO-CMD-001', @c_fanja, NULL, 'CREEE', 'NORMALE', 3.50, 0.12, 21000.00, 2.10,
  'ambatomainty', 'Analakely, Antananarivo', 'Ambatomainty, Antananarivo',
  -18.9089, 47.5259, -18.9089, 47.5241,
  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
  NULL, NULL, NULL, NULL),

 -- 002 CREEE
 ('DEMO-CMD-002', @c_tiana, NULL, 'CREEE', 'URGENTE', 6.00, 0.30, 46000.00, 8.10,
  'ivato', 'Ivato, Antananarivo', 'Ankorondrano, Antananarivo',
  -18.9170, 47.5220, -18.8940, 47.5230,
  DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 003 ASSIGNEE
 ('DEMO-CMD-003', @c_hanta, @l_naina, 'ASSIGNEE', 'NORMALE', 2.00, 0.08, 19000.00, 3.50,
  'ambatomainty', 'Anosy, Antananarivo', 'Ambatomainty, Antananarivo',
  -18.9180, 47.5210, -18.9089, 47.5241,
  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 004 ASSIGNEE
 ('DEMO-CMD-004', @c_mamy, @l_faniry, 'ASSIGNEE', 'NORMALE', 4.50, 0.20, 38000.00, 6.70,
  'ivandry', 'Ambohijatovo, Antananarivo', 'Ivandry, Antananarivo',
  -18.9130, 47.5310, -18.8780, 47.5180,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
  NULL, NULL, NULL, NULL),

 -- 005 PRISE_EN_CHARGE
 ('DEMO-CMD-005', @c_toky, @l_mamy, 'PRISE_EN_CHARGE', 'EXPRESS', 1.80, 0.06, 26000.00, 2.90,
  'anosy', 'Analakely, Antananarivo', 'Anosy, Antananarivo',
  -18.9089, 47.5259, -18.9180, 47.5210,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 006 PRISE_EN_CHARGE
 ('DEMO-CMD-006', @c_lalao, @l_tojo, 'PRISE_EN_CHARGE', 'URGENTE', 7.20, 0.40, 44000.00, 5.60,
  'itaosy', 'Ankorondrano, Antananarivo', 'Itaosy, Antananarivo',
  -18.8940, 47.5230, -18.9230, 47.5320,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 007 EN_TRANSIT
 ('DEMO-CMD-007', @c_nirina, @l_fandresena, 'EN_TRANSIT', 'URGENTE', 8.00, 0.35, 42000.00, 7.20,
  'ivato', 'Ambatomainty, Antananarivo', 'Ivato, Antananarivo',
  -18.9089, 47.5241, -18.9170, 47.5220,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW(),
  NULL, NULL, NULL, NULL),

 -- 008 EN_TRANSIT
 ('DEMO-CMD-008', @c_vonjy, @l_miora, 'EN_TRANSIT', 'NORMALE', 12.00, 0.80, 27000.00, 5.10,
  'ankorondrano', 'Ivandry, Antananarivo', 'Ankorondrano, Antananarivo',
  -18.8780, 47.5180, -18.8940, 47.5230,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 009 LIVREE
 ('DEMO-CMD-009', @c_fanja, @l_haja, 'LIVREE', 'EXPRESS', 3.20, 0.10, 33000.00, 4.60,
  'mahamasina', 'Analakely, Antananarivo', 'Mahamasina, Antananarivo',
  -18.9089, 47.5259, -18.9080, 47.5160,
  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY), 'Fanja Rakotomalala', 'CONF-009', 'Colis remis a la reception'),

 -- 010 LIVREE
 ('DEMO-CMD-010', @c_hanta, @l_faniry, 'LIVREE', 'NORMALE', 2.40, 0.09, 29000.00, 3.30,
  'isoraka', 'Ambohijatovo, Antananarivo', 'Isoraka, Antananarivo',
  -18.9130, 47.5310, -18.9000, 47.5320,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY), 'Hanta Randrianasolo', 'CONF-010', 'Recu par le responsable'),

 -- 011 LIVREE
 ('DEMO-CMD-011', @c_toky, @l_tiana, 'LIVREE', 'NORMALE', 5.00, 0.22, 25000.00, 5.90,
  'isoraka', 'Mahamasina, Antananarivo', 'Isoraka, Antananarivo',
  -18.9080, 47.5160, -18.9000, 47.5320,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR),
  DATE_SUB(NOW(), INTERVAL 12 HOUR), 'Toky Andrianina', 'CONF-011', 'Livraison confirmee'),

 -- 012 ANNULEE
 ('DEMO-CMD-012', @c_tiana, NULL, 'ANNULEE', 'URGENTE', 1.20, 0.05, 18000.00, 2.10,
  'anosy', 'Anosy, Antananarivo', 'Analakely, Antananarivo',
  -18.9180, 47.5210, -18.9089, 47.5259,
  DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY),
  NULL, NULL, NULL, NULL),

 -- 013 ANNULEE
 ('DEMO-CMD-013', @c_mamy, NULL, 'ANNULEE', 'NORMALE', 3.00, 0.15, 30000.00, 6.40,
  'itaosy', 'Itaosy, Antananarivo', 'Mahamasina, Antananarivo',
  -18.9230, 47.5320, -18.9080, 47.5160,
  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY),
  NULL, NULL, NULL, NULL),

 -- 014 ECHEC_LIVRAISON
 ('DEMO-CMD-014', @c_lalao, @l_lova, 'ECHEC_LIVRAISON', 'URGENTE', 9.00, 0.50, 41000.00, 5.60,
  'itaosy', 'Ankorondrano, Antananarivo', 'Itaosy, Antananarivo',
  -18.8940, 47.5230, -18.9230, 47.5320,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL),

 -- 015 ECHEC_LIVRAISON
 ('DEMO-CMD-015', @c_vonjy, @l_mamy, 'ECHEC_LIVRAISON', 'NORMALE', 4.00, 0.18, 36000.00, 7.80,
  'anosy', 'Ivandry, Antananarivo', 'Anosy, Antananarivo',
  -18.8780, 47.5180, -18.9180, 47.5210,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR),
  NULL, NULL, NULL, NULL),

 -- 016 RETOURNEE
 ('DEMO-CMD-016', @c_nirina, @l_naina, 'RETOURNEE', 'EXPRESS', 2.80, 0.11, 34000.00, 6.90,
  'ambohijatovo', 'Ivato, Antananarivo', 'Ambohijatovo, Antananarivo',
  -18.9170, 47.5220, -18.9130, 47.5310,
  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
  NULL, NULL, NULL, NULL);

-- ---------------------------------------------------------------------------
-- 6b. Position des livreurs qui transportent un colis
--
-- Un livreur en PRISE_EN_CHARGE ou EN_TRANSIT vient de charger le colis : il se
-- trouve donc au point d'enlevement. Sans cela sa position n'appartient pas a
-- l'itineraire enlevement -> livraison, et la barre d'avancement du tableau de
-- bord reste bloquee a 0 %. Un livreur en ASSIGNEE conserve sa position : il est
-- encore en chemin vers l'enlevement. Calcul refait a chaque execution.
-- ---------------------------------------------------------------------------
UPDATE livreurs l
JOIN commandes c ON c.livreur_id = l.id
SET l.latitude  = c.latitude_enlevement,
    l.longitude = c.longitude_enlevement
WHERE c.statut IN ('PRISE_EN_CHARGE', 'EN_TRANSIT')
  AND c.latitude_enlevement IS NOT NULL
  AND c.longitude_enlevement IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 7. Colis : un colis par commande
-- ---------------------------------------------------------------------------
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Produits alimentaires',     3.50, 0.12, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-001';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Equipement medical',        6.00, 0.30, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-002';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Vetements',                 2.00, 0.08, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-003';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Materiel informatique',     4.50, 0.20, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-004';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Produits cosmetiques',      1.80, 0.06, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-005';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Piece automobile',          7.20, 0.40, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-006';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Colis pharmaceutique',      8.00, 0.35, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-007';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Mobilier de bureau',        12.00, 0.80, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-008';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Produits alimentaires',     3.20, 0.10, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-009';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Livres et fournitures',     2.40, 0.09, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-010';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Appareil electronique',     5.00, 0.22, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-011';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Accessoires telephoniques', 1.20, 0.05, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-012';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Materiaux de construction', 3.00, 0.15, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-013';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Machine industrielle',      9.00, 0.50, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-014';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Textiles',                  4.00, 0.18, b'0' FROM commandes c WHERE c.numero='DEMO-CMD-015';
INSERT INTO colis (commande_id, description, poids, volume, fragile)
SELECT c.id, 'Vins et spiritueux',        2.80, 0.11, b'1' FROM commandes c WHERE c.numero='DEMO-CMD-016';

-- ---------------------------------------------------------------------------
-- 8. Historique des statuts
--
--    Une entree de creation (statut_precedent NULL) puis une entree par
--    transition reelle, dans l'ordre du workflow. Les dates reprennent celles
--    des commandes (section 6) pour que le detail soit coherent.
-- ---------------------------------------------------------------------------
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, NULL, 'CREEE', c.date_creation, 'WEB', 'Creation de la commande'
  FROM commandes c WHERE c.numero LIKE 'DEMO-%';

-- CREEE -> ASSIGNEE (toute commande qui a un livreur)
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'CREEE', 'ASSIGNEE', DATE_ADD(c.date_creation, INTERVAL 2 HOUR), 'AFFECTATION',
       'Affectation automatique'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.livreur_id IS NOT NULL
   AND c.statut IN ('ASSIGNEE', 'PRISE_EN_CHARGE', 'EN_TRANSIT', 'LIVREE',
                    'ECHEC_LIVRAISON', 'RETOURNEE');

-- ASSIGNEE -> PRISE_EN_CHARGE : date_miseajour pour une commande encore a ce
-- statut, 6 h plus tot pour une commande ayant deja progresse.
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'ASSIGNEE', 'PRISE_EN_CHARGE', c.date_miseajour, 'WEB', 'Colis pris en charge'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'PRISE_EN_CHARGE';

INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'ASSIGNEE', 'PRISE_EN_CHARGE', DATE_SUB(c.date_miseajour, INTERVAL 6 HOUR),
       'WEB', 'Colis pris en charge'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%'
   AND c.statut IN ('EN_TRANSIT', 'LIVREE', 'ECHEC_LIVRAISON', 'RETOURNEE');

-- PRISE_EN_CHARGE -> EN_TRANSIT
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'PRISE_EN_CHARGE', 'EN_TRANSIT', c.date_miseajour, 'API',
       'Position mise a jour par le livreur'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'EN_TRANSIT';

INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'PRISE_EN_CHARGE', 'EN_TRANSIT', DATE_SUB(c.date_miseajour, INTERVAL 2 HOUR),
       'API', 'Position mise a jour par le livreur'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%'
   AND c.statut IN ('LIVREE', 'ECHEC_LIVRAISON');

INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'PRISE_EN_CHARGE', 'EN_TRANSIT', DATE_SUB(c.date_miseajour, INTERVAL 4 HOUR),
       'API', 'Position mise a jour par le livreur'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'RETOURNEE';

-- EN_TRANSIT -> LIVREE
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'EN_TRANSIT', 'LIVREE', c.date_miseajour, 'WEB',
       CONCAT('Remis a ', COALESCE(c.preuve_nom_receptionnaire, 'la reception'))
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'LIVREE';

-- EN_TRANSIT -> ECHEC_LIVRAISON
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'EN_TRANSIT', 'ECHEC_LIVRAISON', c.date_miseajour, 'INCIDENT',
       'Echec de livraison signale par un incident'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'ECHEC_LIVRAISON';

INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'EN_TRANSIT', 'ECHEC_LIVRAISON', DATE_SUB(c.date_miseajour, INTERVAL 2 HOUR),
       'INCIDENT', 'Echec de livraison signale par un incident'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'RETOURNEE';

-- ECHEC_LIVRAISON -> RETOURNEE
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'ECHEC_LIVRAISON', 'RETOURNEE', c.date_miseajour, 'INCIDENT',
       'Retour du colis au depot'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'RETOURNEE';

-- CREEE -> ANNULEE (commande annulee avant affectation)
INSERT INTO historique_statuts
 (commande_id, statut_precedent, statut_nouveau, date_changement, origine, commentaire)
SELECT c.id, 'CREEE', 'ANNULEE', c.date_miseajour, 'WEB', 'Annulation a la demande du client'
  FROM commandes c
 WHERE c.numero LIKE 'DEMO-%' AND c.statut = 'ANNULEE' AND c.livreur_id IS NULL;

-- ---------------------------------------------------------------------------
-- 9. Incidents de demonstration
--
--    Les 5 types et les 4 statuts sont representes. Le livreur est repris de la
--    commande comme le fait IncidentService. Certains incidents resolus portent
--    une transition appliquee a la commande (ANNULEE, ECHEC_LIVRAISON,
--    RETOURNEE), ce qui illustre le traitement d'incident de bout en bout.
-- ---------------------------------------------------------------------------
-- SIGNALEE
INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'ADRESSE_INCORRECTE', DATE_SUB(NOW(), INTERVAL 5 HOUR),
       'Le livreur signale que l''adresse de destination est incomplete a Ambatomainty.',
       'SIGNALEE', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-001';

INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'VEHICULE_EN_PANNE', DATE_SUB(NOW(), INTERVAL 3 HOUR),
       'Panne de batterie signalee avant le depart vers Ankorondrano.',
       'SIGNALEE', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-002';

INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'CLIENT_ABSENT', DATE_SUB(NOW(), INTERVAL 2 HOUR),
       'Personne au point de livraison d''Itaosy, deux passages effectues.',
       'SIGNALEE', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-014';

-- EN_TRAITEMENT
INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'COLIS_ENDOMMAGE', DATE_SUB(NOW(), INTERVAL 1 DAY),
       'Emballage dechire constate au chargement, contenu a verifier.',
       'EN_TRAITEMENT', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-003';

INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'REFUS_CLIENT', DATE_SUB(NOW(), INTERVAL 20 HOUR),
       'Le client conteste le montant et refuse de signer dans l''immediat.',
       'EN_TRAITEMENT', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-004';

INSERT INTO incidents (type, date, description, statut, commande_id, livreur_id)
SELECT 'VEHICULE_EN_PANNE', DATE_SUB(NOW(), INTERVAL 8 HOUR),
       'Crevaison sur la route d''Anosy, vehicule en attente de reparation.',
       'EN_TRAITEMENT', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-015';

-- RESOLU (sans impact sur le workflow de la commande)
INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       commande_id, livreur_id)
SELECT 'CLIENT_ABSENT', DATE_SUB(NOW(), INTERVAL 3 DAY),
       'Premier passage : personne ne repond a l''adresse.',
       'RESOLU', 'Client present au second passage, colis remis contre confirmation.',
       DATE_SUB(NOW(), INTERVAL 2 DAY), c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-009';

INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       commande_id, livreur_id)
SELECT 'COLIS_ENDOMMAGE', DATE_SUB(NOW(), INTERVAL 2 DAY),
       'Colis constate abime a l''arrivee a Isoraka.',
       'RESOLU', 'Colis remplace par le depot, incident classe sans impact sur le client.',
       DATE_SUB(NOW(), INTERVAL 1 DAY), c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-010';

INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       commande_id, livreur_id)
SELECT 'ADRESSE_INCORRECTE', DATE_SUB(NOW(), INTERVAL 1 DAY),
       'Numero de rue errone dans la commande.',
       'RESOLU', 'Adresse corrigee avec le client, livraison effectuee normalement.',
       DATE_SUB(NOW(), INTERVAL 12 HOUR), c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-011';

-- RESOLU avec transition appliquee a la commande
INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       transition_proposee, commande_id, livreur_id)
SELECT 'REFUS_CLIENT', DATE_SUB(NOW(), INTERVAL 4 DAY),
       'Le client refuse le colis et signe le refus.',
       'RESOLU', 'Refus enregistre, retour non demande : commande annulee.',
       DATE_SUB(NOW(), INTERVAL 4 DAY), 'ANNULEE', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-012';

INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       transition_proposee, commande_id, livreur_id)
SELECT 'VEHICULE_EN_PANNE', DATE_SUB(NOW(), INTERVAL 2 DAY),
       'Panne mecanique bloquant la tournee vers Itaosy.',
       'RESOLU', 'Panne non reparee a temps : livraison declaree en echec.',
       DATE_SUB(NOW(), INTERVAL 1 DAY), 'ECHEC_LIVRAISON', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-014';

INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       transition_proposee, commande_id, livreur_id)
SELECT 'CLIENT_ABSENT', DATE_SUB(NOW(), INTERVAL 2 DAY),
       'Destinataire injoignable sur plusieurs jours a Ambohijatovo.',
       'RESOLU', 'Retour du colis au depot valide par le dispatcher.',
       DATE_SUB(NOW(), INTERVAL 1 DAY), 'RETOURNEE', c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-016';

-- REJETE
INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       commande_id, livreur_id)
SELECT 'ADRESSE_INCORRECTE', DATE_SUB(NOW(), INTERVAL 1 DAY),
       'Le client signale une erreur de saisie dans son adresse.',
       'REJETE', 'Verification faite : l''adresse enregistree est correcte, aucune erreur.',
       DATE_SUB(NOW(), INTERVAL 20 HOUR), c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-005';

INSERT INTO incidents (type, date, description, statut, resolution, date_resolution,
                       commande_id, livreur_id)
SELECT 'COLIS_ENDOMMAGE', DATE_SUB(NOW(), INTERVAL 12 HOUR),
       'Le client affirme avoir recu un colis abime mais n''a pas fourni de photo.',
       'REJETE', 'Aucun element probant : reclamation classee sans suite.',
       DATE_SUB(NOW(), INTERVAL 6 HOUR), c.id, c.livreur_id
  FROM commandes c WHERE c.numero = 'DEMO-CMD-006';

-- ---------------------------------------------------------------------------
-- 10. Verification
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_demo_livreurs;

SELECT 'clients' AS table_demo, COUNT(*) AS n FROM clients
UNION ALL SELECT 'livreurs', COUNT(*) FROM livreurs
UNION ALL SELECT 'commandes', COUNT(*) FROM commandes
UNION ALL SELECT 'colis', COUNT(*) FROM colis
UNION ALL SELECT 'historique_statuts', COUNT(*) FROM historique_statuts
UNION ALL SELECT 'incidents', COUNT(*) FROM incidents
UNION ALL SELECT 'tarifs', COUNT(*) FROM tarifs;

SELECT c.statut, COUNT(*) AS n
  FROM commandes c WHERE c.numero LIKE 'DEMO-%'
 GROUP BY c.statut ORDER BY c.statut;

SELECT i.type, i.statut, COUNT(*) AS n
  FROM incidents i JOIN commandes c ON c.id = i.commande_id
 WHERE c.numero LIKE 'DEMO-%'
 GROUP BY i.type, i.statut ORDER BY i.type, i.statut;

SELECT c.numero,
       COUNT(h.id) AS entrees,
       GROUP_CONCAT(CONCAT(COALESCE(h.statut_precedent, '-'), '>', h.statut_nouveau)
                    ORDER BY h.date_changement, h.id SEPARATOR ' | ') AS parcours
  FROM commandes c
  LEFT JOIN historique_statuts h ON h.commande_id = c.id
 WHERE c.numero LIKE 'DEMO-%'
 GROUP BY c.numero
 ORDER BY c.id;

-- Prix calcule d'apres le bareme : la formule de reference est
-- PrixCalculator, donc ce controle ne remplace pas les tests unitaires.
SELECT c.numero,
       c.zone_livraison AS zone,
       COALESCE(t.prix_fixe, d.prix_fixe) AS prix_fixe,
       ROUND(c.distance * COALESCE(t.prix_par_km, d.prix_par_km), 2) AS part_km,
       ROUND(c.poids    * COALESCE(t.prix_par_kg, d.prix_par_kg), 2) AS part_kg,
       ROUND(IFNULL(c.volume, 0) * COALESCE(t.prix_par_m3, d.prix_par_m3), 2) AS part_m3,
       ROUND(
         (COALESCE(t.prix_fixe, d.prix_fixe)
          + c.distance * COALESCE(t.prix_par_km, d.prix_par_km)
          + c.poids    * COALESCE(t.prix_par_kg, d.prix_par_kg)
          + IFNULL(c.volume, 0) * COALESCE(t.prix_par_m3, d.prix_par_m3))
         * (1 + (CASE c.priorite
                   WHEN 'URGENTE' THEN COALESCE(t.majoration_urgente_pct, d.majoration_urgente_pct)
                   WHEN 'EXPRESS' THEN COALESCE(t.majoration_express_pct, d.majoration_express_pct)
                   ELSE 0 END) / 100), 2) AS prix_calcule,
       c.prix AS prix_enregistre
  FROM commandes c
  LEFT JOIN tarifs t ON LOWER(t.zone) = LOWER(c.zone_livraison)
  JOIN tarifs d ON d.zone = 'TOUTES'
 WHERE c.numero LIKE 'DEMO-%'
 ORDER BY c.id;

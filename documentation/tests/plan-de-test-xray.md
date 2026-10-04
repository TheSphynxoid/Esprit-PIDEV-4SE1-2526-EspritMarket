# EspritMarket — Plan de test Xray

Contenu de référence pour l'Atelier « Test et validation de logiciel ».
Structure conforme au guide Xray : Préconditions → Suites de tests → Cas de test → Plan de test → Exécution.

- **Projet Jira :** ES (EspritMarket)
- **Exigences du backlog :** ES-10 à ES-47 (38 Stories sur 9 Epics)
- **Périmètre d'exécution :** 12 cas de test couvrant 12 exigences
- **Type de test :** Manuel
- **Total :** 7 préconditions, 9 suites, 12 cas, 1 plan, 1 exécution

> Un référentiel complet de 47 cas de test a été rédigé puis réduit à 12 pour tenir dans le périmètre de l'atelier. Les 26 exigences non couvertes restent documentées dans le backlog et seront traitées lors d'une campagne ultérieure.

---

## 1. Préconditions

| Clé Jira | ID | Titre |
|---|---|---|
| ES-48 | PRE-01 | Environnement de test déployé |
| ES-50 | PRE-02 | Base de données initialisée |
| ES-51 | PRE-03 | Comptes de test créés pour chaque acteur |
| ES-52 | PRE-04 | Vendeur approuvé et boutique peuplée |
| ES-53 | PRE-05 | Livreur validé et véhicule enregistré |
| ES-54 | PRE-06 | Service ML disponible |
| ES-55 | PRE-07 | Clé Google Maps configurée |

---

## 2. Matrice de couverture

| Suite | Clé | Cas de test | Exigences couvertes |
|---|---|---|---|
| TS-COMM | ES-56 | ES-49, ES-67 | ES-10, ES-12 |
| TS-MKTP | ES-57 | ES-72, ES-73 | ES-16, ES-17 |
| TS-LIVR | ES-58 | ES-75 | ES-18 |
| TS-EVNT | ES-59 | ES-84 | ES-25 |
| TS-SRVP | ES-60 | ES-89 | ES-29 |
| TS-PART | ES-61 | ES-95 | ES-34 |
| TS-ADMI | ES-62 | ES-98 | ES-36 |
| TS-IA | ES-63 | ES-103 | ES-40 |
| TS-NFR | ES-64 | ES-105, ES-108 | ES-42, ES-44 |

**12 exigences couvertes sur 38.** Chaque Epic a au moins un cas exécuté.

**Exigences non couvertes par un cas exécuté (26) :** ES-11, ES-13, ES-14, ES-15, ES-19, ES-20, ES-21, ES-22, ES-23, ES-24, ES-26, ES-27, ES-28, ES-30, ES-31, ES-32, ES-33, ES-35, ES-37, ES-38, ES-39, ES-41, ES-43, ES-45, ES-46, ES-47.

---

## 3. Cas de test

Format : chaque cas indique l'exigence couverte, les préconditions, puis des étapes avec **Action** et **Résultat attendu**.

### TS-COMM — Module Commun

#### ES-49 — TC-COMM-01 Création de compte réussie
- **Couverture :** ES-10 · **Préconditions :** PRE-01, PRE-02

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Ouvrir la page d'inscription | Le formulaire affiche les champs nom, e-mail, mot de passe et confirmation |
| 2 | Saisir un e-mail non encore utilisé et un mot de passe valide | Aucune erreur de validation ne s'affiche |
| 3 | Cliquer sur « Créer un compte » | Un message de confirmation s'affiche et un jeton JWT est retourné |
| 4 | Se connecter avec les identifiants créés | L'accès au tableau de bord est accordé |

#### ES-67 — TC-COMM-04 Contrôle d'accès par rôle
- **Couverture :** ES-12 · **Préconditions :** PRE-01, PRE-03

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Se connecter avec un compte de rôle Acheteur | L'interface ne présente pas les menus Livreur, Vendeur et Administrateur |
| 2 | Appeler directement un endpoint réservé à l'administrateur | La requête est refusée avec un code HTTP 403 |
| 3 | Se connecter avec un compte Administrateur | Les menus administrateur sont accessibles et les mêmes endpoints répondent |

### TS-MKTP — Module Marketplace

#### ES-72 — TC-MKTP-05 Passage de commande avec paiement
- **Couverture :** ES-16 · **Préconditions :** PRE-01, PRE-04

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Ajouter deux produits au panier | Le panier affiche les quantités et le total recalculé |
| 2 | Modifier la quantité de l'un des produits | Le total est recalculé immédiatement |
| 3 | Valider la commande puis effectuer le paiement | Une référence de transaction est enregistrée |
| 4 | Consulter le suivi de commande | La commande apparaît avec son statut initial et la livraison associée est créée |
| 5 | Vérifier le stock du produit commandé | Le stock est décrémenté |

#### ES-73 — TC-MKTP-06 Dépôt d'un avis
- **Couverture :** ES-17 · **Préconditions :** PRE-01, PRE-04

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Depuis une commande livrée, déposer un avis avec note et image | L'avis est enregistré et rattaché au produit |
| 2 | Consulter la fiche produit | Le nouvel avis et la nouvelle note moyenne apparaissent |
| 3 | Tenter de déposer un second avis sur le même produit | La soumission est refusée |
| 4 | Tenter de déposer un avis sur un produit non acheté | La soumission est refusée |

### TS-LIVR — Module Livraison

#### ES-75 — TC-LIVR-01 Création d'une demande de livraison
- **Couverture :** ES-18 · **Préconditions :** PRE-01, PRE-05

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Depuis une commande, créer une demande de livraison | Le formulaire d'adresse et de mode de livraison s'affiche |
| 2 | Renseigner l'adresse et le mode de livraison souhaités | Le prix est calculé automatiquement, sans saisie manuelle |
| 3 | Valider la demande | La demande est créée et un véhicule ainsi qu'un livreur sont affectés |
| 4 | Tenter de valider la demande sans renseigner d'adresse | La validation est bloquée avec un message explicite |

### TS-EVNT — Module Événements

#### ES-84 — TC-EVNT-04 Inscription et achat de billet
- **Couverture :** ES-25 · **Préconditions :** PRE-01, PRE-03

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | S'inscrire à un événement disposant de places | L'inscription est confirmée et la capacité diminue |
| 2 | Acheter un billet pour cet événement | Le billet est consultable après la transaction |
| 3 | Annuler son inscription | La place est libérée |

### TS-SRVP — Module Services et Projets

#### ES-89 — TC-SRVP-03 Réservation et annulation d'une prestation
- **Couverture :** ES-29 · **Préconditions :** PRE-01, PRE-03

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Parcourir le catalogue et consulter le détail d'une prestation | Les informations de la prestation s'affichent |
| 2 | Réserver un créneau libre | La réservation est créée et apparaît dans l'agenda |
| 3 | Tenter de réserver un créneau déjà pris | Le créneau n'est pas sélectionnable |
| 4 | Annuler la réservation avant le délai limite | La réservation est annulée et le créneau redevient disponible |

### TS-PART — Module Partenariats

#### ES-95 — TC-PART-03 Postulation et score de correspondance
- **Couverture :** ES-34 · **Préconditions :** PRE-01, PRE-03, PRE-06

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Parcourir les offres et consulter le détail d'une offre | Les informations de l'offre s'affichent |
| 2 | Postuler à l'offre | La candidature est enregistrée |
| 3 | Consulter son score de correspondance | Un score et un rapport explicatif s'affichent |
| 4 | Tenter de postuler une seconde fois à la même offre | La soumission est refusée |

### TS-ADMI — Administration et Modération

#### ES-98 — TC-ADMI-01 Approbation des demandes d'accès
- **Couverture :** ES-36 · **Préconditions :** PRE-01, PRE-03

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Ouvrir la liste des demandes en attente | Les demandes sont listées avec le profil du demandeur |
| 2 | Approuver une demande de vendeur | Le rôle est attribué et le module Marketplace devient accessible au demandeur |
| 3 | Rejeter une demande en saisissant un motif | Le demandeur reçoit une notification motivant le rejet |

> Cas prioritaire. Les approbations conditionnent l'accès aux modules Marketplace et Livraison : les échecs constatés ensuite sur des cas liés à l'accès doivent être distingués des défauts fonctionnels.

### TS-IA — Service IA

#### ES-103 — TC-IA-02 Indisponibilité du service ML
- **Couverture :** ES-40 · **Préconditions :** PRE-01, PRE-06

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Arrêter le service ML | Le service est injoignable |
| 2 | Utiliser les modules Services et Marketplace | Aucun blocage, les parcours restent utilisables de bout en bout |
| 3 | Redémarrer le service ML | Les prédictions redeviennent disponibles |

### TS-NFR — Exigences Non Fonctionnelles

#### ES-105 — TC-NFR-01 Performance des requêtes de consultation
- **Couverture :** ES-42 · **Préconditions :** PRE-01, PRE-02

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Ouvrir le catalogue produits en charge nominale | Le temps d'affichage total reste sous 2 secondes |
| 2 | Ouvrir la fiche produit détaillée | Le temps de réponse reste sous 2 secondes |
| 3 | Relever les temps de réponse sur les principaux endpoints | Le temps médian reste inférieur à 2 secondes |

#### ES-108 — TC-NFR-04 Sécurité de l'accès et des données
- **Couverture :** ES-44 · **Préconditions :** PRE-01, PRE-02

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1 | Appeler un endpoint protégé sans jeton | Une erreur HTTP 401 est retournée |
| 2 | Appeler un endpoint protégé avec un jeton expiré | Une erreur HTTP 401 est retournée |
| 3 | Interroger la table des utilisateurs en base | Aucun mot de passe n'est stocké en clair et un sel est présent |
| 4 | Inspecter le dépôt Git et le fichier `.env.example` | La clé JWT et les identifiants de base sont absents du dépôt |
| 5 | Tenter d'accéder à une ressource sans rôle compatible | Une erreur HTTP 403 est retournée |

---

## 4. Plan de test

**ES-111 — Plan de test — EspritMarket (Atelier Test et validation)**

Rattacher les 9 suites : ES-56 à ES-64.

## 5. Exécution des tests

**ES-112 — Exécution des tests — EspritMarket Sprint 1**

Inclure les 12 cas : ES-49, ES-67, ES-72, ES-73, ES-75, ES-84, ES-89, ES-95, ES-98, ES-103, ES-105, ES-108.

Puis `Start execution`.

**Ordre recommandé :** commencer par ES-98 (approbations administrateur), qui conditionne l'accès aux modules Marketplace et Livraison.

Statuts à renseigner : **Passé**, **Échoué**, **Bloqué**.

Pour un cas Bloqué, créer un ticket de type `Bug` dans le projet `ES` et le référencer dans le commentaire d'exécution. Ce ticket ferme la chaîne de traçabilité **exigence (Story) → cas de test → bug**.

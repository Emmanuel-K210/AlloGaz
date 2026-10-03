# AlloGaz — backend

API de la marketplace AlloGaz : elle met en relation des acheteurs et des dépôts de gaz (achat et recharge
de bouteilles) en Côte d'Ivoire. Elle est consommée par une PWA React (dossier [`frontend/`](frontend/README.md)).

- Java 21, Spring Boot 4.1, Maven
- PostgreSQL 16 + PostGIS (Hibernate Spatial), migrations Flyway
- Redis : cache du référentiel, compteurs OTP, relais pub/sub du temps réel
- Authentification par téléphone et code OTP SMS, JWT court + refresh token à rotation
- Suivi de commande en temps réel par **SSE**
- Fuseau `Africa/Abidjan`, locale `fr`, montants en **F CFA entiers**

## Démarrage rapide

```bash
docker compose up --build
```

| URL | |
|---|---|
| http://localhost:8080/swagger-ui.html | Documentation interactive (OpenAPI) |
| http://localhost:8080/v3/api-docs | Spécification OpenAPI (JSON) |
| http://localhost:8080/actuator/health | Santé |

Docker Compose démarre PostgreSQL/PostGIS, Redis et l'API, et charge les **données de démonstration**.
Il n'y a pas de mot de passe : demandez un code OTP pour un numéro de démo. Le SMS est écrit dans les logs
(`docker compose logs -f app`, ligne `[SMS -> +225...]`).

Parcours complet en une commande (recherche, commande, acceptation, paiement, code de livraison, libération
des fonds) : `./scripts/demo.sh` (nécessite `curl` et `jq`).

### Comptes de démonstration

| Téléphone | Rôle |
|---|---|
| +225 07 00 00 00 01 | Administrateur |
| +225 07 00 00 00 11 | Vendeur, « Dépôt Gaz Riviera » (Cocody, livraison 500 F, horaires 7 h-20 h, dim. 8 h-13 h) |
| +225 05 00 00 00 12 | Vendeur, « Yop Gaz Service » (Yopougon, livraison incluse, ouvert en continu) |
| +225 01 00 00 00 13 | Vendeur, « Marcory Gaz Express » (Marcory, livraison 700 F, 6 h 30-21 h) |
| +225 07 00 00 00 14 | Vendeur, « Dépôt Anoumabo » (en attente de validation, retrait sur place) |
| +225 07 00 00 00 21 / 05 00 00 00 22 / 01 00 00 00 23 | Acheteurs (Cocody, Yopougon, Marcory) |

Les prix de démonstration sont indicatifs.

## Tests

```bash
mvn test
```

Docker doit être disponible : les tests d'intégration lancent PostgreSQL/PostGIS et Redis réels avec
Testcontainers. La suite (environ 250 tests) couvre notamment :

- la **machine à états** : table de transitions vérifiée sur toutes les paires de statuts, règles de l'agrégat ;
- le **workflow de commande** de bout en bout par l'API : cas nominal, prix figé à l'acceptation, expiration,
  mauvais code puis blocage, validation automatique, litige (remboursement ou libération), deux paiements
  simultanés sur la dernière bouteille, droits d'accès ;
- la **recherche par proximité** sur PostGIS : classement, filtres, exclusions (non vérifié, en pause, fermé,
  hors rayon de recherche) ;
- l'OTP, la rotation des refresh tokens, le séquestre et le grand livre, le flux SSE, les données de démo ;
- l'architecture (ArchUnit) : le domaine ne dépend ni de Spring, ni de JPA, ni de Jackson.

## Variables d'environnement

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/allogaz` / `allogaz` / `allogaz` | Base PostgreSQL + PostGIS |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `FLYWAY_LOCATIONS` | `classpath:db/migration` | Ajouter `,classpath:db/demo` pour les données de démo |
| `JWT_SECRET` | valeur de dev | Clé HMAC des JWT (≥ 32 octets). **À changer en production** |
| `HASH_PEPPER` | valeur de dev | Secret des hachages de codes OTP et de livraison. **À changer en production** |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `PT15M` / `P30D` | Durées de vie des jetons |
| `OTP_TTL` / `OTP_MAX_ATTEMPTS` | `PT5M` / `5` | Durée de vie et essais du code OTP |
| `OTP_RESEND_COOLDOWN` / `OTP_MAX_SENDS_PER_HOUR` | `PT60S` / `5` | Anti-abus des envois de SMS |
| `ORDER_SELLER_RESPONSE_TIMEOUT` | `PT10M` | Délai de réponse du vendeur avant expiration |
| `ORDER_AUTO_VALIDATION_DELAY` | `PT24H` | Délai après livraison avant validation automatique |
| `ORDER_MAX_DELIVERY_CODE_ATTEMPTS` | `5` | Essais pour le code de livraison |
| `COMMISSION_RATE_BPS` | `500` | Commission en points de base (500 = 5 %) |
| `PAYMENT_GATEWAY_FEE_RATE_BPS` | `150` | Estimation des frais d'agrégateur Mobile Money (encaissement + reversement), en points de base. Prélevée sur la commission (jamais sur l'acheteur ni le vendeur) : à ajuster avec les tarifs réels une fois CinetPay/PayDunya branché |
| `PAYMENT_PROVIDER` | `fake` | Agrégateur Mobile Money (seule la passerelle factice existe pour l'instant) |
| `PAYMENT_FAKE_AUTO_SUCCEED` | `true` | Passerelle factice : confirmation immédiate, ou attente de la simulation |
| `SEARCH_MAX_RADIUS_M` | `30000` | Rayon maximal de recherche des dépôts |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Origines autorisées (PWA) |
| `SCHEDULING_ENABLED` | `true` | Tâches planifiées (expiration, validation automatique) |
| `PORT` | `8080` | Port HTTP |

Les pondérations du classement sont réglables sous `allogaz.search.weights` (`application.yml`).

## Endpoints principaux

Tous préfixés par `/api/v1`. Authentification : `Authorization: Bearer <accessToken>`.
Les erreurs suivent la RFC 9457 (`application/problem+json`) avec un champ `code` stable, par exemple
`OTP_INVALID`, `INSUFFICIENT_STOCK` ou `WRONG_DELIVERY_CODE`.

**Authentification et compte**

| | |
|---|---|
| `POST /auth/otp/request` `{phone}` | Envoie un code OTP par SMS (inscription ou connexion) |
| `POST /auth/otp/verify` `{phone, code}` | Renvoie les jetons ; crée le compte au premier passage |
| `POST /auth/refresh` `{refreshToken}` | Nouvelle paire de jetons (l'ancien refresh token est invalidé) |
| `POST /auth/logout` `{refreshToken}` | Révoque le refresh token |
| `GET/PATCH /me` | Mon compte |

**Catalogue et recherche** (publics)

| | |
|---|---|
| `GET /catalog/categories` | Catégories |
| `GET /catalog/products?category&brand&company&color&sizeKg` | Produits (société de provenance, couleurs, contenance) |
| `GET /catalog/sellers/{id}` | Fiche d'un dépôt vérifié et ses offres |
| `GET /search/sellers?lat&lon&productId&brand&company&color&sizeKg&type=REFILL\|PURCHASE&limit` | Dépôts vérifiés et ouverts, classés par score |

Un dépôt « échange toutes marques » (`universalExchange`, réglable via `POST/PUT /seller/profile`) reprend une
bouteille vide de n'importe quelle société en échange d'une des siennes. Il ressort donc dans `/search/sellers`
même quand `brand`/`company`/`color`/`productId` ne correspond pas à ce qu'il vend : seuls la contenance
(`sizeKg`) et le type (`REFILL`/`PURCHASE`) restent filtrants pour lui. Un dépôt classique n'est remonté que
s'il vend effectivement la société/couleur demandée.

**Vendeur** (rôle `SELLER`, sauf la création du dépôt)

| | |
|---|---|
| `POST /seller/profile` | Ouvre un dépôt (en attente de validation) et donne le rôle `SELLER` (rafraîchir le jeton ensuite) |
| `GET/PUT /seller/profile`, `POST /seller/profile/open`, `POST /seller/profile/pause` | Profil, ouverture et pause |
| `GET /seller/offers`, `PUT /seller/offers/{productId}` | Offres : stock et visibilité (le prix est national, non modifiable ici) |
| `GET /seller/orders?status=` | Commandes reçues |
| `POST /seller/orders/{id}/accept` \| `reject` \| `prepare` \| `dispatch` \| `delivered` | Cycle de la commande |
| `POST /seller/orders/{id}/delivery-code` `{code}` | Code donné par l'acheteur : valide la commande et libère les fonds |
| `GET /seller/balance` | Solde (fonds libérés, commission déduite) |

**Acheteur**

| | |
|---|---|
| `POST /orders` | Crée la commande (`submit: true` envoie directement l'intention au vendeur) |
| `GET /orders`, `GET /orders/{id}` | Mes commandes, détail |
| `POST /orders/{id}/submit` \| `pay` \| `confirm-receipt` \| `cancel` \| `dispute` \| `rating` | Actions |
| `POST /orders/{id}/delivery-code` | Nouveau code de livraison (en cas de perte) |

**Temps réel (SSE)**

| | |
|---|---|
| `GET /orders/{id}/events` | Flux d'une commande : événement `snapshot` à la connexion, puis `order-status` |
| `GET /me/events` | Flux de toutes mes commandes (le vendeur y reçoit les nouvelles intentions) |

```js
// EventSource n'envoie pas d'en-tête : le jeton passe en paramètre, accepté uniquement sur ces flux.
const es = new EventSource(`${API}/orders/${id}/events?access_token=${accessToken}`);
es.addEventListener('order-status', (e) => console.log(JSON.parse(e.data))); // {orderId, from, to, at}
```

**Paiement et administration**

| | |
|---|---|
| `POST /payments/callback/{provider}` `{reference}` | Notification de l'agrégateur (le statut est revérifié auprès de lui) |
| `POST /payments/fake/{reference}/complete?success=` | Simulation de la validation Mobile Money (passerelle factice uniquement) |
| `GET /admin/sellers?status=PENDING`, `POST /admin/sellers/{id}/verify` \| `suspend` | Validation des dépôts |
| `POST /admin/products` | Ajout au référentiel, avec son tarif national |
| `PUT /admin/products/{id}/price` `{refillPrice, purchasePrice}` | Change le tarif national d'un produit (recharge et/ou achat) |
| `GET /admin/disputes`, `POST /admin/orders/{id}/resolve` `{outcome: RELEASE_TO_SELLER\|REFUND_BUYER, note}` | Litiges |
| `GET /admin/orders/{id}/ledger` | Mouvements du grand livre d'une commande |

## Architecture

Architecture hexagonale découpée en modules métier (`ci.allogaz.<module>`), chacun en trois couches :

```
identity   comptes, OTP, jetons                      domain/          entités et règles, Java pur
catalog    référentiel, dépôts (PostGIS), offres      application/     cas d'usage, ports (port/out)
geo        recherche par proximité, classement        infrastructure/  REST, JPA/JDBC, Redis, adaptateurs
ordering   commande, machine à états, temps réel
payment    paiement, séquestre, grand livre
shared     erreurs, GeoPoint, configuration
```

- Le **domaine** ne dépend d'aucun framework. ArchUnit l'impose, ainsi que l'interdiction pour `application`
  de dépendre d'`infrastructure`. La couche application utilise les annotations Spring de transaction et de service.
- Les modules se parlent par des **ports** : `ordering` déclare `CatalogGateway`, `EscrowPort`, `PaymentPort`,
  `BuyerNotifier`, `SellerReputation`, et leurs adaptateurs (dans `ordering.infrastructure`) appellent les
  services des autres modules. La confirmation d'un paiement remonte de `payment` vers `ordering` par un
  événement synchrone, dans la même transaction.
- Services externes derrière des ports : `PaymentGateway` (implémentation factice ; CinetPay ou PayDunya à
  brancher) et `SmsSender` (implémentation qui écrit dans les logs).

### Choix notables

- **SSE plutôt que WebSocket** : le suivi de commande est un flux à sens unique (serveur → client). SSE passe
  par le HTTP standard et les proxys, et `EventSource` le gère nativement dans la PWA, avec reconnexion
  automatique. Les événements sont publiés après validation de la transaction et relayés par Redis pub/sub :
  plusieurs instances de l'API peuvent tourner.
- **Machine à états sans framework** : un `enum OrderStatus` porte la table des transitions autorisées, et
  l'agrégat `Order` passe par une seule méthode de transition qui la vérifie et enregistre un événement.
  Une douzaine de statuts ne justifie pas Spring Statemachine : la table reste lisible et testée exhaustivement
  (toutes les paires de statuts).
- **Séquestre en grand livre à partie double** (`ledger_entries`) : chaque mouvement s'équilibre à zéro
  (Mobile Money → séquestre → vendeur + commission, ou retour à l'acheteur). Le grand livre est en ajout seul
  (un déclencheur interdit `UPDATE` et `DELETE`), et des index uniques garantissent un seul dépôt et une seule
  sortie de séquestre par commande. Un litige gèle simplement les fonds jusqu'à la décision de l'administrateur.
- **Frais de passerelle Mobile Money** (`PLATFORM:GATEWAY_FEES`) : l'agrégateur (CinetPay/PayDunya) facture
  un coût à l'encaissement et au reversement, sans que l'acheteur ou le vendeur ne le voie. Ce coût est estimé
  (`gateway-fee-rate-bps`, § variables d'environnement) et prélevé sur la commission de la plateforme à la
  libération et au remboursement, pour que la ligne `PLATFORM:COMMISSION` reflète la marge nette réelle plutôt
  que la commission brute — sans ça, la plateforme peut perdre de l'argent sans que ça se voie nulle part.
- **Stock et concurrence** : le stock est décrémenté à la confirmation du paiement, sous verrouillage optimiste
  (`@Version`). En cas de conflit, la confirmation est rejouée ; si le stock manque alors, la commande est
  annulée et l'acheteur remboursé automatiquement.
- **Codes courts hachés** (OTP, code de livraison à 4 chiffres) en HMAC-SHA256 avec un secret serveur : sans
  ce secret, un code à 4 chiffres haché se retrouverait par force brute. Les essais sont limités (OTP dans Redis,
  code de livraison sur la commande). Une erreur de code est enregistrée avant d'être signalée, pour que la
  limite ne puisse pas être contournée.
- **Référentiel des bouteilles** : en Côte d'Ivoire, la bouteille se reconnaît à sa **société de provenance**
  et à sa **couleur**. Comme plusieurs couleurs circulent pour une même société (flottes grises ou bleues),
  le produit porte une liste de couleurs et une description d'apparence (capsule, nuance). Une recharge
  correspond à l'échange d'une bouteille vide de même société et de même contenance (sauf dépôt « échange
  toutes marques », voir plus haut).
- **Prix national, pas fixé par le dépôt** : le gaz est un produit réglementé — le tarif (recharge et/ou
  achat) est le même partout et seul le syndicat des gaziers le fait évoluer. Le prix est donc porté par le
  **produit** du référentiel (`refill_price`/`purchase_price`), pas par l'offre d'un vendeur en particulier ;
  seul un administrateur le modifie (`PUT /admin/products/{id}/price`). L'offre d'un dépôt ne porte plus que
  son stock et s'il l'affiche aux acheteurs.

### Cycle de vie d'une commande

```
DRAFT → INTENT_SENT → ACCEPTED → PAID → IN_PREPARATION → OUT_FOR_DELIVERY → DELIVERED → VALIDATED → FUNDS_RELEASED
             ├→ REJECTED                ↘                    ↘                  ↘            ↘
             └→ EXPIRED                  DISPUTED (fonds gelés) → FUNDS_RELEASED | CANCELLED (remboursement)
CANCELLED : par l'acheteur avant paiement ; par le vendeur avant expédition (remboursement)
```

- Le vendeur répond dans un délai configurable (10 min) ; sinon une tâche planifiée expire la commande.
- À l'acceptation, les prix des produits et du transport sont recalculés puis **figés**.
- Pour un retrait sur place, `OUT_FOR_DELIVERY` signifie « prête à être retirée » et le transport vaut 0.
- À la remise, le vendeur saisit le code à 4 chiffres reçu par SMS par l'acheteur. Le bon code valide la
  commande et libère les fonds, moins la commission. Après 5 erreurs, le code est bloqué : l'acheteur
  confirme alors la réception dans l'application.
- Si le vendeur déclare la livraison sans code, la commande est validée automatiquement après 24 h sans
  réaction de l'acheteur.

## Points ouverts

- **Référentiel** : couleurs et sociétés renseignées d'après les indications reçues. Il reste à confirmer les
  contenances réellement commercialisées par chaque société (6 kg et 12,5 kg ont été supposées partout).
- **Tarifs nationaux de démo** : la migration qui a introduit le prix au niveau du produit (plutôt que de
  l'offre) a repris le prix le plus fréquent observé dans les données de démo comme valeur de départ. À
  remplacer par les tarifs réellement homologués par le syndicat des gaziers avant toute mise en production.
- **Paiement réel** : brancher CinetPay ou PayDunya derrière `PaymentGateway` (vérification de signature des
  notifications), puis le reversement Mobile Money des soldes vendeurs (le grand livre tient déjà ces soldes).
- **SMS réel** : brancher un fournisseur derrière `SmsSender`.
- Le premier administrateur est créé par les données de démo ; en production, prévoir un amorçage dédié.

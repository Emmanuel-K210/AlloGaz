# AlloGaz — frontend

PWA React qui consomme l'API AlloGaz (voir `../README.md`) : recherche de dépôts par proximité, commande,
suivi en temps réel, espace vendeur et administration.

- React 19, TypeScript, Vite, React Router
- Design : charte graphique AlloGaz (logo, mascotte animée, palette, police Baloo 2)
- PWA installable (`vite-plugin-pwa`)

## Démarrage rapide

Le backend doit tourner (`docker compose up --build` à la racine du dépôt, voir le README principal).

```bash
cp .env.example .env.local   # si l'API n'est pas sur http://localhost:8080
npm install
npm run dev
```

Ouvre http://localhost:5173. Connecte-toi avec un numéro de démo (voir le README du backend pour la liste) ;
le code OTP est écrit dans les logs du backend (`docker compose logs -f app`, ligne `[SMS -> +225...]`).

## Scripts

| Commande | |
|---|---|
| `npm run dev` | Serveur de développement |
| `npm run build` | Vérification des types (`tsc -b`) puis build de production |
| `npm run preview` | Sert le build de production localement |
| `npm run lint` | Lint (oxlint) |

## Structure

```
src/
  api/        client HTTP (JWT + refresh rotatif), types alignés sur les DTO backend, un module par domaine
  auth/       contexte d'authentification, garde de route (RequireAuth)
  cart/       panier acheteur (un dépôt à la fois), persisté en localStorage
  components/ Logo, Mascotte (poses animées), éléments d'UI partagés
  layout/     AppShell (en-tête, barre d'onglets selon le rôle)
  pages/      écrans acheteur, vendeur (pages/seller) et administration (pages/admin)
  styles/     thème (variables CSS, police, composants de base)
  utils/      formatage (F CFA, distance, dates), géolocalisation, libellés de statut de commande
```

## Ce qui est branché

- **Acheteur** : recherche par proximité, fiche dépôt, panier, commande, paiement (passerelle factice),
  suivi en temps réel par SSE, code de livraison, confirmation de réception, litige, notation.
- **Vendeur** : ouverture de dépôt, ouverture/pause, offres (prix recharge/neuve, stock), commandes reçues
  et cycle de vie complet (accepter/refuser/préparer/expédier/code de livraison), solde.
- **Administration** : validation des dépôts en attente, résolution des litiges.

## Points ouverts

- Horaires d'ouverture du dépôt : non éditables depuis l'interface pour l'instant (un dépôt sans horaires
  renseignés est considéré ouvert en continu côté backend) ; à ajouter dans le formulaire vendeur si besoin.
- Icônes PWA : le manifest utilise le logo SVG pour toutes les tailles ; des PNG dédiés (192/512, maskable)
  amélioreraient l'installation sur certains Android.
- Paiement et SMS réels : dépendent du branchement backend (voir « Points ouverts » du README principal) ;
  le flux de paiement factice est entièrement fonctionnel côté interface (y compris la simulation succès/échec).

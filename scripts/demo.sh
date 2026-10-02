#!/usr/bin/env bash
set -euo pipefail
# Parcours de démonstration de bout en bout sur la stack Docker Compose (données de démo).
# Prérequis : docker compose up --build, curl et jq. Les codes OTP et de livraison sont lus dans les logs.
API=${API:-http://localhost:8080/api/v1}
logs() { docker compose -f "$(dirname "$0")/../docker-compose.yml" logs app 2>/dev/null; }
login() {  # $1 = téléphone
  curl -sf -X POST $API/auth/otp/request -H 'Content-Type: application/json' -d "{\"phone\":\"$1\"}" >/dev/null
  sleep 1
  local code; code=$(logs | grep "SMS -> $1" | tail -1 | grep -oE 'code de connexion est [0-9]{6}' | grep -oE '[0-9]{6}')
  curl -sf -X POST $API/auth/otp/verify -H 'Content-Type: application/json' -d "{\"phone\":\"$1\",\"code\":\"$code\"}" | jq -r .accessToken
}
AWA=$(login +2250700000021); YAO=$(login +2250700000011)
echo "== Recherche autour de Riviera 3 (TotalEnergies 12,5 kg, recharge)"
curl -sf "$API/search/sellers?lat=5.3650&lon=-3.9580&brand=TotalEnergies&sizeKg=12.5&type=REFILL" \
  | jq -c '.[] | {shopName, distanceMeters, deliversToYou, score, prix: .offers[0].refillPrice, couleurs: .offers[0].bottleColors}'
echo "== Commande (intention envoyée)"
ORDER=$(curl -sf -X POST $API/orders -H "Authorization: Bearer $AWA" -H 'Content-Type: application/json' -d '{
  "sellerId":"5e000000-0000-0000-0000-000000000001","fulfillment":"DELIVERY","deliveryAddress":"Riviera 3",
  "deliveryLatitude":5.3650,"deliveryLongitude":-3.9580,"submit":true,
  "lines":[{"offerId":"0f000000-0000-0000-0000-000000000102","type":"REFILL","quantity":1}]}')
ID=$(echo "$ORDER" | jq -r .id); echo "$ORDER" | jq -c '{status, responseDeadline, total}'
echo "== Acceptation (prix figé)"
curl -sf -X POST $API/seller/orders/$ID/accept -H "Authorization: Bearer $YAO" | jq -c '{status, itemsTotal, transportFee, total, pricesFrozen}'
echo "== Paiement (séquestre)"
curl -sf -X POST $API/orders/$ID/pay -H "Authorization: Bearer $AWA" | jq -c .
sleep 1; CODE=$(logs | grep "SMS -> +2250700000021" | tail -1 | grep -oE 'livraison [0-9]{4}' | grep -oE '[0-9]{4}')
echo "== Préparation, livraison, mauvais code puis bon code"
curl -sf -X POST $API/seller/orders/$ID/prepare -H "Authorization: Bearer $YAO" | jq -r .status
curl -sf -X POST $API/seller/orders/$ID/dispatch -H "Authorization: Bearer $YAO" | jq -r .status
WRONG=$([ "$CODE" = "0000" ] && echo 1111 || echo 0000)
curl -s -X POST $API/seller/orders/$ID/delivery-code -H "Authorization: Bearer $YAO" -H 'Content-Type: application/json' -d "{\"code\":\"$WRONG\"}" | jq -c '{status, code, detail}'
curl -sf -X POST $API/seller/orders/$ID/delivery-code -H "Authorization: Bearer $YAO" -H 'Content-Type: application/json' -d "{\"code\":\"$CODE\"}" | jq -c '{status, total}'
echo "== Solde vendeur (commission 5 % déduite)"
curl -sf $API/seller/balance -H "Authorization: Bearer $YAO" | jq -c .

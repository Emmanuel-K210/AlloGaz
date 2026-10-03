import type { MascotPose } from '../components/Mascot';
import type { OrderStatus } from '../api/types';

export interface StatusInfo {
  label: string;
  description: string;
  pose: MascotPose;
  tone: 'bleu' | 'vert' | 'rouge' | 'neutre';
}

export const ORDER_STATUS_INFO: Record<OrderStatus, StatusInfo> = {
  DRAFT: { label: 'Brouillon', description: "Commande en préparation, pas encore envoyée.", pose: 'think', tone: 'neutre' },
  INTENT_SENT: { label: 'Envoyée au dépôt', description: 'Le dépôt doit accepter ou refuser.', pose: 'think', tone: 'bleu' },
  ACCEPTED: { label: 'Acceptée', description: 'Le dépôt a accepté. Il ne reste plus qu\'à payer.', pose: 'wave', tone: 'bleu' },
  REJECTED: { label: 'Refusée', description: 'Le dépôt a refusé cette commande.', pose: 'sorry', tone: 'rouge' },
  EXPIRED: { label: 'Expirée', description: "Le dépôt n'a pas répondu à temps.", pose: 'sorry', tone: 'rouge' },
  PAID: { label: 'Payée', description: 'Le paiement est protégé. Le dépôt prépare ta commande.', pose: 'wave', tone: 'bleu' },
  IN_PREPARATION: { label: 'En préparation', description: 'Ta bouteille est en cours de préparation.', pose: 'think', tone: 'bleu' },
  OUT_FOR_DELIVERY: { label: 'En livraison', description: 'En route vers chez toi (ou prête à retirer).', pose: 'wave', tone: 'bleu' },
  DELIVERED: { label: 'Livrée', description: 'Confirme la réception ou donne le code au livreur.', pose: 'wave', tone: 'bleu' },
  VALIDATED: { label: 'Validée', description: 'Réception confirmée, les fonds sont libérés.', pose: 'party', tone: 'vert' },
  FUNDS_RELEASED: { label: 'Terminée', description: 'Commande terminée avec succès.', pose: 'party', tone: 'vert' },
  CANCELLED: { label: 'Annulée', description: 'Cette commande a été annulée.', pose: 'sorry', tone: 'rouge' },
  DISPUTED: { label: 'En litige', description: "Un litige est ouvert, l'équipe AlloGaz va trancher.", pose: 'sorry', tone: 'rouge' },
};

export function statusInfo(status: OrderStatus): StatusInfo {
  return ORDER_STATUS_INFO[status];
}

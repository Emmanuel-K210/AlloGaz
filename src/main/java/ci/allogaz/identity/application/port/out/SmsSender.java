package ci.allogaz.identity.application.port.out;

import ci.allogaz.identity.domain.PhoneNumber;

/** Envoi de SMS (fournisseur réel à brancher plus tard). */
public interface SmsSender {

    void send(PhoneNumber to, String message);
}

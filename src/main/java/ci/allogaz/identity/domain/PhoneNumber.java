package ci.allogaz.identity.domain;

import java.util.regex.Pattern;

import ci.allogaz.shared.domain.DomainException;

/**
 * Numéro mobile ivoirien au format E.164 (+225 suivi de 10 chiffres, depuis 2021).
 * Préfixes mobiles : 01 (Moov), 05 (MTN), 07 (Orange).
 */
public record PhoneNumber(String value) {

    private static final Pattern NATIONAL_MOBILE = Pattern.compile("0[157]\\d{8}");

    public PhoneNumber {
        if (value == null || !value.startsWith("+225") || !NATIONAL_MOBILE.matcher(value.substring(4)).matches()) {
            throw invalid();
        }
    }

    /** Accepte « 07 01 02 03 04 », « +225 0701020304 », « 0022507... ». */
    public static PhoneNumber parse(String raw) {
        if (raw == null) {
            throw invalid();
        }
        String digits = raw.replaceAll("[\\s.\\-()]", "");
        if (digits.startsWith("+225")) {
            digits = digits.substring(4);
        } else if (digits.startsWith("00225")) {
            digits = digits.substring(5);
        } else if (digits.startsWith("225") && digits.length() == 13) {
            digits = digits.substring(3);
        }
        if (!NATIONAL_MOBILE.matcher(digits).matches()) {
            throw invalid();
        }
        return new PhoneNumber("+225" + digits);
    }

    private static DomainException invalid() {
        return new DomainException("INVALID_PHONE", "Numéro de téléphone ivoirien invalide.");
    }

    @Override
    public String toString() {
        return value;
    }
}

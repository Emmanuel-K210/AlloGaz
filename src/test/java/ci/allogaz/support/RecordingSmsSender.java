package ci.allogaz.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ci.allogaz.identity.application.port.out.SmsSender;
import ci.allogaz.identity.domain.PhoneNumber;

/** Capture les SMS envoyés pendant les tests. */
public class RecordingSmsSender implements SmsSender {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{4,8})\\b");

    private final Map<String, List<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void send(PhoneNumber to, String message) {
        messages.computeIfAbsent(to.value(), k -> new CopyOnWriteArrayList<>()).add(message);
    }

    public String lastMessage(String phone) {
        List<String> list = messages.getOrDefault(phone, List.of());
        if (list.isEmpty()) {
            throw new AssertionError("Aucun SMS envoyé à " + phone);
        }
        return list.getLast();
    }

    /** Premier nombre de 4 à 8 chiffres du dernier SMS. */
    public String lastCode(String phone) {
        Matcher m = CODE.matcher(lastMessage(phone));
        if (!m.find()) {
            throw new AssertionError("Pas de code dans le SMS");
        }
        return m.group(1);
    }
}

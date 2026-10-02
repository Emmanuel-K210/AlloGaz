package ci.allogaz.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ci.allogaz.shared.domain.DomainException;

class PhoneNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"0701020304", "07 01 02 03 04", "+225 07 01 02 03 04", "002250701020304", "2250701020304"})
    void normalizes_ivorian_mobile_numbers(String raw) {
        assertThat(PhoneNumber.parse(raw).value()).isEqualTo("+2250701020304");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "070102030", "07010203045", "0901020304", "+33612345678", "abc"})
    void rejects_invalid_numbers(String raw) {
        assertThatThrownBy(() -> PhoneNumber.parse(raw)).isInstanceOf(DomainException.class)
                .hasMessageContaining("invalide");
    }
}

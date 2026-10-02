package ci.allogaz.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestBeans {

    @Bean
    @Primary
    public RecordingSmsSender recordingSmsSender() {
        return new RecordingSmsSender();
    }
}

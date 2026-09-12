package gg.sanitycapped.emotes.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClockConfig {

    /** Injected rather than called statically, so tests can hold time still. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

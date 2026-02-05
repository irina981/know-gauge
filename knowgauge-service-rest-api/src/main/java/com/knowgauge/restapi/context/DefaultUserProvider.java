package com.knowgauge.restapi.context;

import java.util.OptionalLong;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentUserProvider;

@Component
@Profile({"dev", "docker"})
public class DefaultUserProvider implements CurrentUserProvider {
    @Override public OptionalLong userId() { return OptionalLong.of(1L); }
}

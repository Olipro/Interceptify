package net.uptheinter.interceptify.interfaces;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestStartupConfig {

    @Test
    void getClasspaths() {
        var conf = mock(StartupConfig.class, RETURNS_DEEP_STUBS);
        when(conf.getJarFilesToInject()
                .stream()
                .map(any())
                .filter(any())
                .collect(any())).thenReturn(new ArrayList<>());
        when(conf.getClasspaths()).thenCallRealMethod();
        assertNotNull(conf.getClasspaths());
    }
}
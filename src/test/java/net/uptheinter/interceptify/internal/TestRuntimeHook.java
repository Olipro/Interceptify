package net.uptheinter.interceptify.internal;

import net.uptheinter.interceptify.interfaces.StartupConfig;
import net.uptheinter.interceptify.internal.RuntimeHook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.Instrumentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestRuntimeHook {
    @Mock Instrumentation mockInstr;
    @Mock StartupConfig mockConf;

    @Test
    void init() {
        RuntimeHook.premain("none", mockInstr);
        // prevents us going into the actual ClassInjector call.
        var exc = new RuntimeException("this is fake");
        when(mockConf.getClasspaths()).thenThrow(exc);
        boolean failed = true;
        try {
            RuntimeHook.init(mockConf);
        } catch (RuntimeException e) {
            assertEquals(exc, e);
            failed = false;
        }
        assertFalse(failed);
        verify(mockConf).getJarFilesToInject();
        verify(mockConf).getClasspaths();
    }
}
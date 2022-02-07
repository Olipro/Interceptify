package net.uptheinter.interceptify.internal;

import net.uptheinter.interceptify.interfaces.StartupConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.Instrumentation;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestRuntimeHook {
    @Mock Instrumentation mockInstr;
    @Mock(answer = RETURNS_DEEP_STUBS) ClassInjector mockInjector;
    @Mock StartupConfig mockConf;

    @Test
    void init() throws NoSuchFieldException, IllegalAccessException {
        RuntimeHook.premain(null, mockInstr);
        RuntimeHook.premain("15", mockInstr);
        var ci = RuntimeHook.class.getDeclaredField("ci");
        ci.setAccessible(true);
        ci.set(null, mockInjector);
        RuntimeHook.init(mockConf);
        verify(mockConf).getJarFilesToInject();
        verify(mockConf).getClasspaths();
        verify(mockInjector.setClassPath(any())
                .defineMakePublicList(any())
                .defineMakePublicPredicate(any())
                .collectMetadataFrom(any())).applyAnnotationsAndIntercept();
    }
}
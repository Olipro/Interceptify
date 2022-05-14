package net.uptheinter.interceptify;

import net.uptheinter.interceptify.interfaces.StartupConfig;
import net.uptheinter.interceptify.util.JarFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.Instrumentation;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TestEntryPoint implements StartupConfig {
    @Mock Instrumentation mockInstr;
    private boolean epSucceeded;

    @Test
    void entryPoint() {
        EntryPoint.premain(null, mockInstr);
        EntryPoint.entryPoint(this, new String[]{"TestArg"});
        assertTrue(epSucceeded);
    }

    @Override
    public JarFiles getJarFilesToInject() {
        return new JarFiles();
    }

    @Override
    public Consumer<String[]> getRealMain() {
        return (String[] args) -> {
            assertEquals(1, args.length);
            assertEquals("TestArg", args[0]);
            epSucceeded = true;
        };
    }
}

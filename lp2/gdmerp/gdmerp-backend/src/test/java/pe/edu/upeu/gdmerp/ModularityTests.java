package pe.edu.upeu.gdmerp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(GdmerpBackendApplication.class);

    @Test
    void verifyModularity() {
        modules.verify();
    }

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new org.springframework.modulith.docs.Documenter(modules)
                .writeDocumentation();
    }

}

package com.syncevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class FlywayMigrationsValidationTest {

    private static final Pattern MIGRATION_PATTERN = Pattern.compile("^V(\\d+)__.*\\.sql$");

    @Test
    void exactamente23MigracionesFlywayExistenYEstanOrdenadasSecuencialmente() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/V*.sql");

        assertEquals(23, resources.length, "Deben existir exactamente 23 migraciones Flyway.");

        List<Integer> versions = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            assertNotNull(filename);
            Matcher matcher = MIGRATION_PATTERN.matcher(filename);
            assertTrue(matcher.matches(), "El archivo de migración no sigue el formato V<num>__<desc>.sql: " + filename);
            int version = Integer.parseInt(matcher.group(1));
            versions.add(version);

            // Verificar que el script no esté vacío
            try (InputStream is = resource.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.trim().length() > 10, "El script de migración está vacío: " + filename);
            }
        }

        versions.sort(Integer::compareTo);

        for (int i = 1; i <= 23; i++) {
            assertEquals(i, versions.get(i - 1), "Falta o está desordenada la migración V" + i);
        }
    }
}

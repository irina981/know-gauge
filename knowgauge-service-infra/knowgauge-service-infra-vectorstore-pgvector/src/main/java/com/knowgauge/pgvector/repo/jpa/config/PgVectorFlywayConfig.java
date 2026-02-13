package com.knowgauge.pgvector.repo.jpa.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgVectorFlywayConfig {

    @Bean(name = "pgvectorFlyway", initMethod = "migrate")
    @ConditionalOnProperty(prefix = "kg.pgvector.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Flyway pgvectorFlyway(@Qualifier("vectorDataSource") DataSource dataSource,
                                 PgVectorFlywayProperties props) {

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(props.getLocations())
                .schemas(props.getSchemas())
                .baselineOnMigrate(true) // useful when DB might already exist
                .load();
    }

    @Bean
    @ConfigurationProperties(prefix = "kg.pgvector.flyway")
    public PgVectorFlywayProperties pgVectorFlywayProperties() {
        return new PgVectorFlywayProperties();
    }

    public static class PgVectorFlywayProperties {
        private String[] locations = new String[] {"classpath:db/migration/pgvector"};
        private String[] schemas = new String[] {"public"};

        public String[] getLocations() { return locations; }
        public void setLocations(String[] locations) { this.locations = locations; }
        public String[] getSchemas() { return schemas; }
        public void setSchemas(String[] schemas) { this.schemas = schemas; }
    }
}

package com.knowgauge.pgvector.repo.jpa.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "com.knowgauge.pgvector.repo.jpa.repository", entityManagerFactoryRef = "vectorEntityManagerFactory", transactionManagerRef = "vectorTransactionManager")
@EntityScan(basePackages = "com.knowgauge.pgvector.repo.jpa.entity")
public class PgVectorJpaConfig {

	@Bean(name = "vectorDataSourceProperties")
	@ConfigurationProperties("kg.pgvector.datasource")
	DataSourceProperties pgvectorProps() {
		return new DataSourceProperties();
	}

	@Bean(name = "vectorDataSource")
	DataSource vectorDataSource(@Qualifier("vectorDataSourceProperties") DataSourceProperties pgvectorProps) {
		return pgvectorProps.initializeDataSourceBuilder().build();
	}

	@Bean(name = "vectorEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean vectorEntityManagerFactory(
			@Qualifier("vectorDataSource") DataSource dataSource) {

		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setDataSource(dataSource);
		emf.setPackagesToScan("com.knowgauge.pgvector.repo.jpa.entity");
		emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		Map<String, Object> props = new HashMap<>();
		// optional - only if you want separate hibernate props:
		// props.put("hibernate.hbm2ddl.auto", "none");
		// props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		emf.setJpaPropertyMap(props);
		
		 emf.setPersistenceUnitName("pgvector"); 

		return emf;
	}

	@Bean(name = "vectorTransactionManager")
	public PlatformTransactionManager vectorTransactionManager(
			@Qualifier("vectorEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
		return new JpaTransactionManager(emf.getObject());
	}
}

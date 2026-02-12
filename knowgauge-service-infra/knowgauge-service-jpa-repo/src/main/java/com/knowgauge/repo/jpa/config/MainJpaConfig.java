package com.knowgauge.repo.jpa.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(basePackages = "com.knowgauge.repo.jpa.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class MainJpaConfig {
	@Bean(name = "mainDataSourceProperties")
	@Primary
	@ConfigurationProperties("spring.datasource")
	public DataSourceProperties mainDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean(name = "dataSource")
	@Primary
	public DataSource dataSource(@Qualifier("mainDataSourceProperties") DataSourceProperties props) {
		return props.initializeDataSourceBuilder().build();
	}

	@Bean(name = "entityManagerFactory")
	@Primary
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("dataSource") DataSource dataSource) {

		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setDataSource(dataSource);
		emf.setPackagesToScan("com.knowgauge.repo.jpa.entity");
		emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

		Map<String, Object> props = new HashMap<>();
		emf.setJpaPropertyMap(props);

		return emf;
	}

	@Bean(name = "transactionManager")
	@Primary
	public PlatformTransactionManager transactionManager(
			@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
		return new JpaTransactionManager(emf.getObject());
	}
}

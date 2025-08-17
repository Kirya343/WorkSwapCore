package org.workswap.config.datasource;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "org.workswap.datasource.cloud.repository",
    entityManagerFactoryRef = "cloudEntityManagerFactory",
    transactionManagerRef = "cloudTransactionManager"
)
@Profile("cloud")
public class CloudDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.cloud-datasource")
    public DataSource cloudDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean cloudEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {
        return builder
                .dataSource(cloudDataSource())
                .packages("org.workswap.datasource.cloud.model") // Пакет с @Entity
                .persistenceUnit("cloud")
                .build();
    }

    @Bean
    public PlatformTransactionManager cloudTransactionManager(
            @Qualifier("cloudEntityManagerFactory") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}

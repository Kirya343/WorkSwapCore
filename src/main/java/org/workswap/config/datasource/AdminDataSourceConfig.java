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
    basePackages = "org.workswap.datasource.admin.repository",
    entityManagerFactoryRef = "adminEntityManagerFactory",
    transactionManagerRef = "adminTransactionManager"
)
@Profile({"backoffice", "statistic"})
public class AdminDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.admin-datasource")
    public DataSource adminDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {
        return builder
                .dataSource(adminDataSource())
                .packages("org.workswap.datasource.admin.model") // Пакет с @Entity
                .persistenceUnit("admin")
                .build();
    }

    @Bean
    public PlatformTransactionManager adminTransactionManager(
            @Qualifier("adminEntityManagerFactory") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}

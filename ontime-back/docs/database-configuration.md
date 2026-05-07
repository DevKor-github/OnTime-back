# Database Configuration

Production uses MySQL with environment-provided database credentials.

## Production

- Spring profile: `prod`
- Datasource URL: `SPRING_DATASOURCE_URL`
- Datasource username: `SPRING_DATASOURCE_USERNAME`
- Datasource password: `SPRING_DATASOURCE_PASSWORD`
- Datasource driver: `com.mysql.cj.jdbc.Driver`
- Hibernate DDL mode: `validate`
- SQL logging: disabled
- Formatted SQL logging: disabled
- Flyway: enabled
- Flyway baseline on migrate: disabled

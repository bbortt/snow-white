## PostgreSQL

The following lines will get you started (execute them from within the `project.rootDirectory`):

```shell
[docker/podman] run \
  -p 5432:5432 -d \
  --name snow_white \
  -e POSTGRES_DB=snow_white \
  -e POSTGRES_USER=snow_white \
  -e POSTGRES_PASSWORD=snow_white_password \
  postgres:14.6-alpine

./gradlew :flywayMigrateDev
```

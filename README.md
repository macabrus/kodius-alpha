## Pokretanje projekta
```bash
docker-compose -f postgres_dockercompose.yaml up -d
mvn exec:java
```

## Dijelovi rješenja
- Frontend - Pebble template engine (u /src/main/resources)
- Backend:
    - Javalin (podržava Swagger) na [localhost:7000/swagger](http://localhost:7000/swagger) REST/RPC fasada - lakše upravljanje Jetty servletima
    - Servisi domene - obična behavioralna Java klasa
    - Domenski modeli - podatkovne klase modelirane pomoću Immutables)
    - Sloj pristupa bazi podataka (Data Access Layer, JDBI knjižnica za jednostavnije SQL upite i mapiranje podataka)
    - Baza podataka - Postgres + pgAdmin4 GUI u docker compose-u

## JSR-330 kompatibilan DI kontejner:
- Google Guice

## Pretprocesor anotacija
- Immutables za reduciranje boilerplatea

_Napomena: korištena Java 17_
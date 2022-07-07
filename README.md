## Pokretanje projekta
```bash
docker-compose -f postgres_dockercompose.yaml up -d
mvn exec:java
```

## Dijelovi rješenja
- Frontend - Pebble template engine (u /src/main/resources)
- Backend:
    - Javalin REST/RPC fasada - lakše upravljanje Jetty servletima
    - Servisi domene - obična behavioralna Java klasa
    - Domenski modeli - podatkovne klase modelirane pomoću Immutables)
    - Sloj pristupa bazi podataka (Data Access Layer, JDBI knjižnica za jednostavnije SQL upite i mapiranje podataka)
    - Baza podataka - Postgres + pgAdmin4 GUI u docker compose-u

## Dependency Injection kontejner:
- Google Guice

## Pretprocesor anotacija
- Immutables za reduciranje boilerplatea

_Napomena: korištena Java 17_
# Ataru

[![Build Status](https://travis-ci.org/Opetushallitus/ataru.svg?branch=master)](https://travis-ci.org/Opetushallitus/ataru)
![NPM Dependencies Status](https://david-dm.org/Opetushallitus/ataru.svg)

A system for creating custom forms, applying to education and handling applications.

## How to start

Start all Ataru processes and docker containers using command

    make start

Stop all Ataru processes and docker containers using command

    make stop

See `make help` for details

## Running custom configurations

If you need to run your custom configuration, you may configure the
configuration files the makefile system uses to start the services.

First, you must kill existing pm2 instance, since it caches the environment
variables.

    make kill

Then, you can start the system using your own configuration files.

    make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-my-config.edn HAKIJA_CONFIG=../ataru-secrets/hakija-my-config.edn

Now your local instances are running using your custom configuration.

### AWS service integration

Currently S3 integration is used in non-dev environments for storage of
temporary files accrued when uploading attachments in parts (for upload resume
support). By default the local file system (/tmp) is used for temporary file
storage.
  
In order to use S3 in development environments, add the following to your
configuration file:

```
:aws {:region "eu-west-1"
      :temp-files {:bucket "opintopolku-<env>-ataru-temp-files"}}
```

and provide credentials when running the hakija application, e.g.

```
AWS_ACCESS_KEY_ID=abc AWS_SECRET_ACCESS_KEY=xyz CONFIG=../ataru-secrets/hakija-<env>.edn lein hakija-dev
```

## Running tests

### Running Cypress tests

Start the service locally with make start command as usual. Then either open Cypress with command

    npm run cypress:open

or run it headless using command

    npm run cypress:run

### Cypress tests in Travis

Travis runs Cypress tests with separate configuration (ClojureScript is compiled with `:advanced` optimizations for improved page load performance). All server logs, captured screenshots and recorded videos are automatically uploaded to S3.

### Running legacy browser tests

Tests require a database, a Redis and a FTPS server. Here is an example of
running those with Docker:

```
docker run -d --name ataru-test-db -p 5433:5432 -e POSTGRES_DB=ataru-test -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph ataru-test-db
docker run --name ataru-test-redis -p 6380:6379 -d redis
docker run -d --name ataru-test-ftpd -p 2221:21 -p 30000-30009:30000-30009 ataru-test-ftpd
```

The tests also require the `lftp` command to be available.

Tests assume some fixtures in the db. To clear the test db, run migrations and
insert the required fixtures by running:

```
./bin/cibuild.sh reset-test-database-with-fixture
```

For Travis CI the ataru-test-db and ataru-test-ftpd images have to be
available as
`190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres`
and
`190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd`.
To make that happen, first login by executing the output of:

```
aws ecr get-login --region eu-west-1 --profile oph-utility --no-include-email
```

You should then be able to push images to the repository:

```
docker tag ataru-test-db 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres
docker tag ataru-test-ftpd 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd
```

To build and run all the tests in the system:

```
./bin/cibuild.sh run-tests
```

### All tests

```
make test
```

### Backend unit tests

```
make start-docker test-clojure
```

### ClojureScript unit tests

```
make test-clojurescript
```

### Browser tests

To run only browser tests (headless, using puppeteer):

```
make start-docker test-browser
```

To run browser tests using a real browser start both virkailija and hakija
applications with `lein hakija-dev` and `lein virkailija-dev`.

Then navigate to a test suite of your choosing, e.g. [http://localhost:8350/lomake-editori/virkailija-test.html](http://localhost:8350/lomake-editori/virkailija-test.html)

### Running browser tests locally

This process is highly fragile and the test running should be rewritten.

First, stop all running systems:

```
make stop
```

Start the servers using following configuration overrides:

```
make start VIRKAILIJA_CONFIG=$PWD/config/test.edn HAKIJA_CONFIG=$PWD/config/test.edn
```

Then, load the test data fixture to database:

```
make load-test-fixture
```

After the service starts and fixture data is loaded, navigate to local URL to run the tests: http://localhost:8350/lomake-editori/virkailija-test.html

Test suites can be found in karma-runner.js

**NOTE**: after each run you need to manually compile test code and load the fixture data to test database using following command, otherwise the test will fail.

```
make compile-test-code load-test-fixture
```

## API documentation

Swagger specs for the APIs can be found at

* <http://localhost:8351/hakemus/swagger.json>
* <http://localhost:8350/lomake-editori/swagger.json>

## Anonymize data

Before transfering data between environments one can anonymize the data by running

```
CONFIG=path-to-application-config.edn lein anonymize-data fake-person-file.txt
```

## Troubleshooting

### `make start` hangs in container creation

If your build gets stuck in the phase where all containers are listed by `docker-compose` like so:

```bash
Step 6/7 : RUN chmod a=,u=rw /etc/ssl/private/pure-ftpd.pem
 ---> Using cache
 ---> c6033ca419e9
Step 7/7 : CMD /run.sh -l puredb:/etc/pure-ftpd/pureftpd.pdb -E -j -R -P $PUBLICHOST -s -A -j -Z -H -4 -E -R -X -x -d -d --tls 3
 ---> Using cache
 ---> 1818a90a9990
Successfully built 1818a90a9990
Successfully tagged ataru_ataru-test-ftpd:latest
COMPOSE_PARALLEL_LIMIT=8 docker-compose up -d
Creating network "ataru_ataru-test-network" with the default driver
Creating network "ataru_cypress-http-proxy-network" with driver "bridge"
Creating ataru_ataru-dev-db_1 ...
Creating ataru_ataru-cypress-test-db_1 ...
Creating ataru_ataru-test-redis_1      ...
Creating ataru-cypress-http-proxy      ...
Creating ataru_ataru-test-db_1         ...
Creating ataru_ataru-dev-redis_1       ...
Creating ataru_ataru-test-ftpd_1       ...
Creating ataru-cypress-test-redis      ...
```

and there's no containers running as shown by `docker ps`:

```bash
➜  ~ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

try running Docker Compose manually with
```bash
docker-compose up -d
```

If everything starts, run `make stop` and now `make start` should work as expected. Why? Who knows...
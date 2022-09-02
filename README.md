# Tribe Automation

## Running

- assuming you got sbt installed and mongodb running running

```
sbt update
sbt run
```

int the root should do the job.

make sure to provide proper env vars. (see `application.conf` inside `resources` directory)

## Running using docker

```
CLIENT_SECRET=<secret> PORT=<port> docker-compose up
```

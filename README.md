# Tribe Automation

## Running

assuming you got sbt installed and mongodb running following commands should do the job.

make sure to provide proper env vars. (see `application.conf` inside `resources` directory)

```
sbt update
sbt run
```

## Running using docker

```
CLIENT_SECRET=<secret> PORT=<port> docker-compose up
```

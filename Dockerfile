FROM hseeberger/scala-sbt:11.0.14.1_1.6.2_2.13.8

WORKDIR /app
COPY build.sbt .
COPY project project
COPY src src
RUN sbt clean universal:stage
CMD ["sh", "-c", "/app/target/universal/stage/bin/tribe-automation"]

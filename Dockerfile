FROM hseeberger/scala-sbt:11.0.2_2.12.8_1.2.8 as builder

COPY . /app

WORKDIR /app

RUN sbt clean test assembly

FROM java:8-jre-alpine

COPY --from=builder /app /app

WORKDIR /app

CMD java -jar target/reviews-service.jar /data/reviews.json
# Multi-stage build for all services using sbt-native-packager

# Stage 0: Build stage with SBT
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.11.7_3.7.3 AS builder

WORKDIR /app

# Copy build definition files
COPY build.sbt .
COPY project ./project

# Copy all module sources
COPY modules ./modules

# Stage all projects (creates optimized packages with start scripts)
RUN sbt stubs/stage && \
  sbt checkout/stage

# Build integration tests
RUN sbt integrationTests/compile

# Base stage with JRE and curl for health checks
FROM eclipse-temurin:17-jre AS base
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Stage 1: Checkout Service
FROM base AS checkout
WORKDIR /app
COPY --from=builder /app/modules/checkout/target/universal/stage ./
CMD ["./bin/checkout-service"]

# Stage 2: create stub container
FROM base AS stubs
WORKDIR /app
COPY --from=builder /app/modules/stubs/target/universal/stage ./
# Default to payment stub, can be overridden
CMD ["./bin/stub-services"]

# Stage 3: Integration Tests
# This stage runs the integration tests from the separate integration-tests module
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.11.7_3.7.3 AS tests
WORKDIR /app

COPY build.sbt .
COPY project ./project
COPY modules ./modules

CMD ["sbt", "integrationTests/run"]

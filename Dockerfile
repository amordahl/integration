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
RUN sbt database/stage && \
  sbt payment/stage && \
  sbt inventory/stage && \
  sbt checkout/stage

# Build drivers
RUN sbt drivers/compile

# Build integration tests
RUN sbt integrationTests/compile

# Base stage with JRE and curl for health checks
FROM eclipse-temurin:17-jre AS base
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Stage 1: Database Service
FROM base AS database
WORKDIR /app
COPY --from=builder /app/modules/database/target/universal/stage ./
CMD ["./bin/database-service"]

# Stage 2: Payment Service
FROM base AS payment
WORKDIR /app
COPY --from=builder /app/modules/payment/target/universal/stage ./
CMD ["./bin/payment-service"]

# Stage 3: Inventory Service
FROM base AS inventory
WORKDIR /app
COPY --from=builder /app/modules/inventory/target/universal/stage ./
CMD ["./bin/inventory-service"]

# Stage 4: Checkout Service
FROM base AS checkout
WORKDIR /app
COPY --from=builder /app/modules/checkout/target/universal/stage ./
CMD ["./bin/checkout-service"]

# Stage 5: Test Drivers
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.11.7_3.7.3 AS drivers
WORKDIR /app

COPY build.sbt .
COPY project ./project
COPY modules ./modules

# Default command can be overridden
CMD ["sbt", "drivers/run"]


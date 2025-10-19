# Big Bang Integration Testing

# Integration Testing Demo - Big Bang Branch

## Overview
This branch demonstrates the **Big Bang** integration testing approach where all services are developed independently and then integrated all at once.

## Architecture
```
[Checkout Service :8083]
         ↓
[Payment :8081] | [Inventory :8082]
         ↓
[Database :8080]
```

## Quick Start

1. Build and start all services:
```bash
docker-compose up --build
```

2. Run integration tests:
```bash
docker-compose run tests
```

3. Test manually:
```bash
# Test a complete checkout flow
curl -X POST http://localhost:8083/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "items":[{"productId":"P1","quantity":1,"price":999.99}],
    "cardNumber":"1234567890123456"
  }'
```

## Project Structure

This is a multi-module SBT project with the following modules:
- **domain**: Shared domain models
- **database**: Database service (port 8080)
- **payment**: Payment service (port 8081)
- **inventory**: Inventory service (port 8082)
- **checkout**: Checkout service (port 8083)
- **tests**: Integration tests

## The Big Bang Challenge

When you run the tests, if something fails, it's difficult to determine which service is causing the issue:
- Is it the checkout orchestration?
- Is payment processing failing?
- Is inventory management broken?
- Is the database not responding?

This demonstrates why incremental integration strategies can be beneficial.

## Building Individual Services

```bash
# Build database service
docker build --target database -t database-service .

# Build payment service
docker build --target payment -t payment-service .

# Build inventory service
docker build --target inventory -t inventory-service .

# Build checkout service
docker build --target checkout -t checkout-service .

# Build tests
docker build --target tests -t integration-tests .
```

## Cleanup

```bash
docker-compose down -v
```
```

---

# Bottom Up Integration Testing

## Overview
This branch demonstrates the **bottom-up** integration testing approach where higher-level modules are represented as drivers.

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

## Cleanup

```bash
docker-compose down -v
```
```

---

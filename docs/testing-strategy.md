# Testing Strategy

## Objectives
- Validate client lifecycle behaviour
- Verify message routing logic
- Ensure graceful handling of disconnects

## Approach
- Client tests simulate server behaviour using temporary local sockets
- Server tests use dummy handlers to validate broadcast and private messaging
- Tests run without requiring the GUI

## Running Tests
```bash
mvn test

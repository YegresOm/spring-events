# spring-events
I just wanted to play with Spring events

Resource pool in memory DB, resource could be booked and released with HTTP request.
After release resource is back in pool.
Remote host is used as holder id.

Core classes: ResourcePool, ResourceController
Communication is async and based on events

To run: mvn spring-boot:run
Application will be available on http://localhost:8080

GET /pool/{size}    resource pool initialization
GET /book           book resource
GET /release        release resource

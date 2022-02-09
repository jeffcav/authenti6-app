# Authenti6 applications

## How to run backend
Redis: `docker run -d --name redis --rm -p 6379:6379 -i -t redis:3.2.5-alpine`
Services provider: `flask run -p 5050 -h 0.0.0.0 --cert=adhoc`


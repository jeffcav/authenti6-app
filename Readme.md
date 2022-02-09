# Authenti6 applications

Before starting services, go to `backend/`, create a python virtual environment and install requirements as shown below:
```
cd backend
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

## How to run services provider
```
cd backend/services-provider
docker run -d --name redis --rm -p 6379:6379 -i -t redis:3.2.5-alpine
FLASK_ENV=development flask run -p 5050 -h 0.0.0.0 --cert=adhoc
```

## how to run the Ambient Sound service:
```
cd backend/services/ambient-sound
source credentials.rc # contains sensitive credentials
FLASK_ENV=development flask run -p 5051 -h 0.0.0.0
```

## how to run the Monitoring System service:
```
cd backend/services/ambient-sound
FLASK_ENV=development flask run -p 5052 -h 0.0.0.0
```
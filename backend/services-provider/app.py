import json
import redis
import netifaces as nif
from flask import Flask, request
from datetime import datetime, timedelta

app = Flask(__name__)

def mac_for_ip(ip):
    for i in nif.interfaces():
        addrs = nif.ifaddresses(i)
        try:
            if_mac = addrs[nif.AF_LINK][0]['addr']
            if_ip = addrs[nif.AF_INET][0]['addr']
        except (IndexError, KeyError):
            if_mac = if_ip = None
        if if_ip == ip:
            return if_mac
    return None

def get_services(services_file="services.json"):
    with open(services_file, "r") as read_file:
        return json.load(read_file)

@app.route('/index')
@app.route('/')
def index():
    return get_services()

@app.route('/auth-status')
def auth_status():
    client_ip = request.remote_addr
    client_mac = mac_for_ip(client_ip)
    
    database = redis.Redis()
    try:
        auth_data = json.loads(database.get(client_mac))
    except TypeError:
        return "{auth-status: \"ERROR\"}"

    return auth_data

# For debug purposes only
@app.route('/add-device')
def add_device():
    client_ip = request.remote_addr
    client_mac = mac_for_ip(client_ip)

    database = redis.Redis()
    expiration_time = datetime.now() + timedelta(hours=1)
    entry = {
        "auth-status": "OK",
        "expires":  expiration_time.isoformat()
    }

    database.set(client_mac, json.dumps(entry))
    return "Device: " + client_mac + " was added with " + json.dumps(entry)

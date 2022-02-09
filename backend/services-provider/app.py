import json
import redis
import netifaces as nif
from flask import Flask, request
from datetime import datetime, timedelta
from python_arptable import get_arp_table

app = Flask(__name__)

def mac_from_ip(ip):
    arp_table = get_arp_table()

    for entry in arp_table:
        if entry['IP address'] == ip:
            return entry['HW address']
    return None

def get_services(services_file="services.json"):
    with open(services_file, "r") as read_file:
        return str(json.load(read_file))

@app.route('/index')
@app.route('/')
def index():
    return get_services()

@app.route('/auth-status')
def auth_status():
    client_mac = mac_from_ip(request.remote_addr)
    
    database = redis.Redis()
    try:
        auth_status = database.get(client_mac)
    except TypeError:
        return str({"auth-status": "ERROR"})

    return auth_status

# For debug purposes only
@app.route('/add-device')
def add_device():
    client_mac = mac_from_ip(request.remote_addr)

    database = redis.Redis()
    entry = {
        "auth-status": "OK",
    }

    database.set(client_mac, str(entry))
    return "Device: " + client_mac + " was added with " + json.dumps(entry)

import json
import netifaces as nif
from flask import Flask, request, send_file

app = Flask(__name__)

def mac_for_ip(ip):
    'Returns a list of MACs for interfaces that have given IP, returns None if not found'
    for i in nif.interfaces():
        addrs = nif.ifaddresses(i)
        try:
            if_mac = addrs[nif.AF_LINK][0]['addr']
            if_ip = addrs[nif.AF_INET][0]['addr']
        except (IndexError, KeyError): #ignore ifaces that dont have MAC or IP
            if_mac = if_ip = None
        if if_ip == ip:
            return if_mac
    return None


def get_services(services_file="services.json"):
    with open(services_file, "r") as read_file:
        return json.load(read_file)


@app.route('/')
def get_data():
    #client_ip = request.remote_addr
    #client_mac = mac_for_ip(client_ip)
    return send_file("services.json")

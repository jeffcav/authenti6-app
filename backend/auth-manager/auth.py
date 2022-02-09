import os
import ast
import errno
import redis

'''
event = {
    "event-type": "NEW_DEVICE",
    "event-type": "MEASURED_DEVICE"

    "mac-address": "d1:d1:d1:d1:d1",
    "ipv6-address": "fe80::abcd",
}
'''


EVENTS_FIFO = '/tmp/events-pipe'
REQUESTS_FIFO = '/tmp/requests-pipe'

def create_pipes():
    print("Creating pipes for communication with the authentication daemon")

    try:
        os.unlink(EVENTS_FIFO)
    except Exception:
        pass
    
    try:
        os.unlink(REQUESTS_FIFO)
    except Exception:
        pass

    try:
        os.mkfifo(EVENTS_FIFO)
    except OSError as oe: 
        if oe.errno != errno.EEXIST:
            raise

    try:
        os.mkfifo(REQUESTS_FIFO)
    except OSError as oe: 
        if oe.errno != errno.EEXIST:
            raise

def handle_new_device(event):
    print("Handling newly connected device...")

    # save current status in the database
    database = redis.Redis()
    
    mac_address = event['mac-address']
    auth_status = str({"auth-status": "IN_PROGRESS"})
    database.set(mac_address, auth_status)
    
    # inform the ipv6-agent to measure this device
    with open(REQUESTS_FIFO, mode='w') as reqs:
        request_data = event['ipv6-address']
        reqs.write(request_data)
    print("Done")

def handle_device_measurements(event):
    print("Handling measured device")

    # TODO: compute aggregations from measurements and use
    # the random  forest model to determine device identity

    # for testing purposes, accept all authentications
    database = redis.Redis()
    mac_address = event['mac-address']
    auth_status = str({"auth-status": "OK"})
    database.set(mac_address, auth_status)

    print("Measurements done")

def process_event(event):
    if event['event-type'] == "NEW_DEVICE":
        handle_new_device(event)
    else:
        handle_device_measurements(event)

def monitor_devices():
    print("Listening to events")
    with open(EVENTS_FIFO, mode='r') as fifo:
        while True:
            data = fifo.read()
            if len(data) == 0:
                continue
            
            event = ast.literal_eval(str(data))
            process_event(event)


def main():
    create_pipes()
    monitor_devices()

main()
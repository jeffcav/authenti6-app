import json
import spotipy
from flask import Flask, request, render_template, flash, g
from spotipy.oauth2 import SpotifyOAuth

app = Flask(__name__)

# This is not actually secret, used for testing purposes only
app.secret_key = "0aJrYfARfCKzv6gXjhs4SZ"

cameras_available = {
    '//www.cameraftp.com/Camera/CameraPlayer.aspx/parentID346255304/shareID17125813/cam1': "Front door",
    '//www.cameraftp.com/Camera/CameraPlayer.aspx/parentID310699673/shareID14763999': "Hall",
    '//www.cameraftp.com/Camera/CameraPlayer.aspx/parentID320334912/shareID14909872/Vialfrè - camera Valle dAosta': "Roof",
    '//www.cameraftp.com/Camera/CameraPlayer.aspx/parentID338732674/shareID16967851/DLAH Foscam': "Back door"
}

@app.route('/index')
@app.route('/')
def index():
    return render_template('cameras.html', cameras = cameras_available)

@app.route('/play', methods=['GET'])
def play():
    camera_id = request.args.get('camera')
    camera_name = cameras_available[camera_id]
    return render_template('play.html', camera_id = camera_id, camera_name=camera_name)

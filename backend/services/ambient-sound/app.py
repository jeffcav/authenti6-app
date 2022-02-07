import json
import spotipy
from flask import Flask, url_for, request
from spotipy.oauth2 import SpotifyOAuth

app = Flask(__name__)

available_songs = {
    'spotify:track:0aJrYfARfCKzv6gXjhs4SZ': "Jack Jonhson - Upside down",
    'spotify:track:0vT797IpZpF5o1XLCnWCUL': "Ferrugem - É Natural",
    'spotify:track:3tc8Z4lGzOIwmIVftsxO6o': "Marilia Mendonça - Graveto"
}

HTML_HEAD = "<html><body style=\"background-color:powderblue; font-family:verdana;\"><h1>HOME VICTROLA</h1><br/>"
HTML_TAIL = "</body></html>\n"

def get_credentials(services_file="credentials.json"):
    with open(services_file, "r") as read_file:
        return json.load(read_file)

def play_song(song_id):
    scope = "user-read-playback-state,user-modify-playback-state"
    sp = spotipy.Spotify(client_credentials_manager=SpotifyOAuth(scope=scope))

    # Get available devices
    available_devices = sp.devices()
    try:
        device_id = available_devices['devices'][0]['id']
    except:
        return "<html><body style=\"background-color:powderblue; font-family:verdana;\">No device available</body></html>\n"

    # Play song
    try:
        sp.start_playback(device_id = device_id, uris=[song_id])
    except spotipy.exceptions.SpotifyException:
        return "<html><body style=\"background-color:powderblue; font-family:verdana;\">Could not play song</body></html>\n"

    return "<html><body style=\"background-color:powderblue; font-family:verdana;\">Now playing: {}<br/><a href={}>See songs list</a></body></html>\n".format(available_songs[song_id], url_for('index'))

@app.route('/index')
@app.route('/')
def index():
    songs_list = ""
    for song_id, song_name in available_songs.items():
        songs_list += "<a href=\"{}?song={}\">{}<br/><br/>".format(url_for('play'), song_id, song_name)

    html = HTML_HEAD + "{}".format(songs_list) + HTML_TAIL
    return html

@app.route('/play', methods=['GET'])
def play():
    song_id = request.args.get('song')
    return play_song(song_id)

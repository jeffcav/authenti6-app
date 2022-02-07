import json
import spotipy
from flask import Flask, url_for, request, render_template, flash, g
from spotipy.oauth2 import SpotifyOAuth

app = Flask(__name__)

# This is not actually secret, used for testing purposes only
app.secret_key = "0aJrYfARfCKzv6gXjhs4SZ"

songs_available = {
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
    error = None
    scope = "user-read-playback-state,user-modify-playback-state"
    sp = spotipy.Spotify(client_credentials_manager=SpotifyOAuth(scope=scope))

    available_devices = sp.devices()
    try:
        device_id = available_devices['devices'][0]['id']
    except:
        error = "No device available"
        
    if error is None:
        try:
            sp.start_playback(device_id = device_id, uris=[song_id])
            print("Playing: ", sp.currently_playing())
        except spotipy.exceptions.SpotifyException:
            error = "Could not play song"
    
    return error

def get_currently_playing_song():
    scope = "user-read-playback-state,user-modify-playback-state"
    sp = spotipy.Spotify(client_credentials_manager=SpotifyOAuth(scope=scope))
    try:
        name = sp.currently_playing()['item']['name']
        return name
    except:
        return None

@app.route('/index')
@app.route('/')
def index():
    return render_template('songs.html', songs = songs_available)

@app.route('/play', methods=['GET'])
def play():
    song_id = request.args.get('song')
    
    error = play_song(song_id)
    if error is None:
        g.is_playing = True
    else:
        flash(error)
        g.is_playing = False

    current_song = get_currently_playing_song()
    if current_song is None:
        flash("Could not determine currently playing song")
        g.current_song = None
    else:
        g.current_song = current_song

    return render_template('play.html')

# Spotify Slideshow

Sportify Slideshow is a simple application that associates a track with a dance type,
and then shows an image for that dance type.
So suppose you have a playlist that current is playing a salsa track, then you can show -for example- on a beamer that this is a salsa.

The [example configuration file](src/main/resources/example.config.tecl) shows how this works, 
it primarily has two tables:
* _**tracks**_ - associates a track id with a dance id.
* _**dances**_ - associates the dance id with an image and a text.

TODO: The slideshow will also show the upcoming tracks, if the used connection provides this information (see below).

## Connecting to spotify
The connection to Spotify can be made in two ways, as shown in the spotify group in the configuration file.
1. _**connect: local**_ - directly connect to a locally running player. This connection cannot show the next track.
2. _**connect: webapi**_ - use Spotify's webapi. This requires the creation of an "app" described below.

How to create an "app" is described on Spotify's [website](https://developer.spotify.com/documentation/web-api/tutorials/getting-started#create-an-app).
Make sure to setup the same redirect URL in both the app and in the configuration file.
Using https://nyota.softworks.nl/SpotifySlideshow.html as the redirect url will give you an easy way to copy the authorization token.
Inspect the web page to verify that nothing fishy is done with the token. 
But of course you are free to setup your own redirect url, as long as the code in the URL is copied into the prompt window.

When starting Spotify Slideshow using the webapi, it will immediately prompt for an authorization token needed to access the webapi.
It also will automatically open a browser to obtain that token, which then needs to be copied into the prompt.
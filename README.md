# Spotify Dance Info

Spotify Dance Info is a simple application that associates a track with a dance type.
The currently playing song is shown prominent, a number of upcoming songs a bit more subdued.
It can also generate a background image from the covert art, given that the used API provides that data.

The [example configuration file](src/main/resources/example.config.tecl) shows how this works, 
it primarily has two tables:
* _**tracks**_ - associates a track id with a dance id.
* _**dances**_ - associates the dance id with a text.

## Connecting to spotify
The connection to Spotify can be made in two ways, as shown in the spotify group in the configuration file.
1. _**connect: local**_ - directly connect to a locally running player (default). This connection cannot show the next tracks or cover art.
2. _**connect: webapi**_ - use Spotify's webapi. This requires the creation of an "app" described below.

How to create an "app" is described on Spotify's [website](https://developer.spotify.com/documentation/web-api/tutorials/getting-started#create-an-app).
Make sure to setup the same redirect URL in both the app and in the configuration file.
Using https://nyota.softworks.nl/SpotifyDanceInfo.html as the redirect url will give you an easy way to copy the authorization token.
You can inspect the web page to verify that nothing fishy is done with the token. 
But of course you are free to setup your own redirect url, as long as the code in the URL is copied into the prompt window.

When starting Spotify Dance Info using the webapi, it will immediately prompt for an authorization token needed to access the webapi.
It also will automatically open a browser to obtain that token, which then needs to be copied into the prompt.

## Starting
The build produces an executeable jar. 
So if Java is formally installed, double clicking it from the same folder where the config.tecl file is, starts the application.
But `...\java -jar SpotifyDanceInfo.jar` from the command line in the folder where the config.tecl is, should always work.
If there is no config.tecl, the application connects in local mode to spotify and just lists the currently playing song.

## Support
There is no formal support for this application: this library is an open source hobby project and no claims can be made.
Asking for help is always an option. But so is participating, creating pull requests, and other ways of contributing.


if exist javaHome.cmd (
    call javaHome.cmd
)

call %JAVA_HOME%\bin\java -DspotifySlideshow.tecl=C:\Users\tom\prj\spotifySlideshow.tecl -jar target\spotifySlideshow-1.0-SNAPSHOT-boot.jar
pause

rem run with: java -jar target/spotifySlideshow-*.jar

if exist javaHome.cmd (
    call javaHome.cmd
)

call %JAVA_HOME%\bin\java -Dconfig.tecl=..\spotifyDanceInfo.tecl -jar target\spotifyDanceInfo.jar
pause

rem run with: java -jar target/spotifyDanceInfo.jar

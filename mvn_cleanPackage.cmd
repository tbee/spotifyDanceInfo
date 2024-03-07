if exist javaHome.cmd (
    call javaHome.cmd
)
call mvnw.cmd clean package 
pause

rem run with: java -jar target/spotifyDanceInfo.jar

if exist javaHome.cmd (
    call javaHome.cmd
)
call mvnw.cmd versions:set
call mvnw.cmd clean package
pause

rem -Dquarkus.native.additional-build-args="--initialize-at-run-time=org.apache.poi.util.RandomSingleton"  -P native
rem --trace-object-instantiation=java.security.SecureRandom 

rem run with: java -jar target/spotifyDanceInfo.jar

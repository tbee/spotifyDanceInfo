if exist javaHome.cmd (
    call ..\javaHome.cmd
)
call ..\mvnw.cmd clean test 
pause

rem run with: java -jar target/calendarAggregator-*.jar

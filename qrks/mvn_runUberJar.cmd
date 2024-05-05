if exist ..\javaHome.cmd (
    call ..\javaHome.cmd
)

for %%f in (target\spotifydanceinfoqrks-*-uber.jar) do (
    echo %%f
	call %JAVA_HOME%\bin\java -Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -jar %%f
	rem -Dquarkus.http.port=8770
)
pause

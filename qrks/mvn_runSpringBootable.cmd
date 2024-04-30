if exist ..\javaHome.cmd (
    call ..\javaHome.cmd
)

for %%f in (target\spotifydanceinfoqrks-*-boot.jar) do (
    echo %%f
	call %JAVA_HOME%\bin\java -Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -Dconfig.tecl=..\..\spotifyDanceInfo.tecl -Dloader.path=. -Dloader.home=. -jar %%f
)
pause

@ECHO OFF
cd /d "%~dp0"
java -server -Dfile.encoding=UTF-8 -cp "20.jar;lib/*" nro.models.server.ServerManager
PAUSE

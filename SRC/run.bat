@ECHO OFF
cd /d "%~dp0"
java -server -Dfile.encoding=UTF-8 -Xlog:gc*:file=log/gc.log:time,uptime,level,tags:filecount=5,filesize=20M -cp "20.jar;lib/*" nro.models.server.ServerManager
PAUSE

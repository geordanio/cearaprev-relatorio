@echo off
cd /d "%~dp0"
java -jar "target\cearaprev-report-jar-with-dependencies.jar"
echo.
echo.
java -jar "target\cearaprev-report-jar-with-dependencies.jar" inativos
echo.
echo CONCLUIDO!

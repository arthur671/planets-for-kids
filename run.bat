@echo off
rem Builds and starts the planet simulation from source. Needs Java 21 or newer.
cd /d "%~dp0"
call gradlew.bat -q run

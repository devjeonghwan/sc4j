@echo off
@rem Starts the SC4J launcher with the Java runtime of this package.
call "%~dp0launcher\bin\sc4j-launcher.bat" %*
exit /b %ERRORLEVEL%

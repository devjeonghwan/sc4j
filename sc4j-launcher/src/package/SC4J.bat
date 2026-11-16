@echo off
@rem Starts the SC4J launcher of this install.
call "%~dp0launcher\bin\sc4j-launcher.bat" %*
exit /b %ERRORLEVEL%

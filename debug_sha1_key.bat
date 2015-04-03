@echo off
keytool -exportcert -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -list -v
pause
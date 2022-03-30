@echo off
mode con lines=30 cols=60
%1 mshta vbscript:CreateObject("Shell.Application").ShellExecute("cmd.exe","/c %~s0 ::","","runas",1)(window.close)&&exit
cd /d "%~dp0"
rem ----

C:\Windows\Microsoft.NET\Framework\v4.0.30319\RegAsm.exe /unregister /nologo VCam.dll
C:\Windows\Microsoft.NET\Framework64\v4.0.30319\RegAsm.exe /unregister /nologo VCam.dll

pause
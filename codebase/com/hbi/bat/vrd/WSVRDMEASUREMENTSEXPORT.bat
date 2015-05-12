@echo off
SET PATH=%PATH%;
D:\ptc\Windchill_9.1\Windchill\bin\windchill.exe -w "D:\ptc\Windchill_9.1\Windchill" --java="D:\ptc\Windchill_9.1\Java\jre\bin\java.exe" com.hbi.wc.interfaces.outbound.webservices.processor.HBIGenericProcessor
if ERRORLEVEL 1 goto last
exit 0

:last
exit 99
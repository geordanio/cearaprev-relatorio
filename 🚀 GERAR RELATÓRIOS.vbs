Set oShell = CreateObject("WScript.Shell")
oShell.CurrentDirectory = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
oShell.Run "cmd /k ""java -jar target\cearaprev-report-jar-with-dependencies.jar && echo. && echo. && java -jar target\cearaprev-report-jar-with-dependencies.jar inativos && echo. && echo RELATORIOS GERADOS! && pause""", 1, True

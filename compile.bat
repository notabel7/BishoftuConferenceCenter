@echo off
echo ================================================
echo   Bishoftu Conference Center - Build Script
echo ================================================

if not exist "lib\sqlite-jdbc.jar" (
    echo.
    echo ERROR: lib\sqlite-jdbc.jar not found!
    echo.
    echo  1. Download from: https://github.com/xerial/sqlite-jdbc/releases
    echo  2. Place the .jar file inside the 'lib' folder
    echo  3. Rename it to sqlite-jdbc.jar
    echo.
    pause
    exit /b 1
)

if not exist "lib\svgSalamander.jar" (
    echo.
    echo ERROR: lib\svgSalamander.jar not found!
    echo.
    echo  1. Download from: https://github.com/blackears/svgSalamander/releases
    echo  2. Place the .jar file inside the 'lib' folder
    echo  3. Rename it to svgSalamander.jar
    echo.
    pause
    exit /b 1
)

if not exist "out" mkdir out

echo Copying icon resources...
if exist "resources\icons" (
    if not exist "out\icons" mkdir "out\icons"
    xcopy /Y /Q "resources\icons\*.png" "out\icons\" 2>nul
    xcopy /Y /Q "resources\icons\*.svg" "out\icons\" 2>nul
    echo   Icons copied to out\icons\
) else (
    echo   No resources\icons folder found - buttons will use text only.
)
rem Also copy any PNG or SVG at the root of resources/ (e.g. app_logo placed there)
if exist "resources\*.png" (
    xcopy /Y /Q "resources\*.png" "out\" 2>nul
    echo   Root-level PNG resources copied to out\
)
if exist "resources\*.svg" (
    xcopy /Y /Q "resources\*.svg" "out\" 2>nul
    echo   Root-level SVG resources copied to out\
)

echo Compiling sources...
javac -g -encoding UTF-8 ^
      -cp "lib\sqlite-jdbc.jar;lib\svgSalamander.jar" ^
      -d out ^
      src\com\conferenceCenter\model\Person.java ^
      src\com\conferenceCenter\model\Employee.java ^
      src\com\conferenceCenter\model\Admin.java ^
      src\com\conferenceCenter\model\AssignedEmployee.java ^
      src\com\conferenceCenter\model\Hall.java ^
      src\com\conferenceCenter\model\Event.java ^
      src\com\conferenceCenter\util\UIConstants.java ^
      src\com\conferenceCenter\dao\DatabaseConnection.java ^
      src\com\conferenceCenter\dao\HallDAO.java ^
      src\com\conferenceCenter\dao\AdminDAO.java ^
      src\com\conferenceCenter\dao\EmployeeDAO.java ^
      src\com\conferenceCenter\dao\EventDAO.java ^
      src\com\conferenceCenter\gui\DataChangeListener.java ^
      src\com\conferenceCenter\gui\LoginFrame.java ^
      src\com\conferenceCenter\gui\HallPanel.java ^
      src\com\conferenceCenter\gui\EmployeePanel.java ^
      src\com\conferenceCenter\gui\EventPanel.java ^
      src\com\conferenceCenter\gui\MainFrame.java ^
      src\Main.java

if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED. Check errors above.
    pause
    exit /b 1
)

echo.
echo BUILD SUCCESSFUL!
echo Run the app with:  run.bat
echo.
pause

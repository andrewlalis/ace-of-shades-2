# This script prepares and runs the jpackage command to generate a Windows AOS Client installer.

$projectDir = $PSScriptRoot
Push-Location $projectDir\target

# Remove existing file if it exists.
Write-Output "Removing existing exe file."
Get-ChildItem *.exe | ForEach-Object { Remove-Item -Path $_.FullName -Force }
Write-Output "Done."

# Run the build
Write-Output "Building the project."
Push-Location $projectDir
mvn clean package

# Get list of dependency modules that maven copied into the lib directory.
Push-Location $projectDir\target
$modules = Get-ChildItem -Path lib -Name | ForEach-Object { "lib\$_" }
# Add our own main module.
$mainModuleJar = Get-ChildItem -Name -Include "aos2-launcher-*.jar" -Exclude "*-jar-with-dependencies.jar"
$modules += $mainModuleJar
Write-Output "Found modules: $modules"
$modulePath = $modules -join ';'

Write-Output "Running jpackage..."
jpackage `
 --type msi `
 --name "Ace-of-Shades" `
 --app-version "1.0.0" `
 --description "Top-down 2D shooter game inspired by Ace of Spades." `
 --icon ..\icon.ico `
 --win-shortcut `
 --win-dir-chooser `
 --win-per-user-install `
 --win-menu `
 --win-shortcut `
 --win-menu-group "Game" `
 --module-path "$modulePath" `
 --module aos2_launcher/nl.andrewl.aos2_launcher.Launcher `
 --add-modules jdk.crypto.cryptoki

Write-Output "Done!"
# Triforce-Listener
WebSocket Server for Triforce Control to listen for game controller input and map them to keyboard/mouse inputs.

## How can I use it?
Download the JAR file from [here](https://github.com/tejashah88/Triforce-Listener/releases) onto the computer in which you have Dolphin installed and run `java -jar TriforceListener.jar`. I'd recommend that you keep the JAR file in it's own folder, since it may make additional files while running.

Download the INI file from the same place to prepare Dolphin for handling the directed inputs from the server.  Go to `Documents\Dolphin Emulator\Config\Profiles\GCPad` and place the .ini file there. Then go to Dolphin and load the the profile called `Triforce-Profile`. Additionally, make sure that the Dolphin window is maximized to ensure that the joystick will work properly, as the joystick input will be mapped to your mouse!

Setup the android app and then you'll be ready to play! Have fun! :)

### What is it?
As the description implies, it's the server portion on the Triforce Control project, which is a system to connect the ADT-1 dev game controller (or really any Android TV based game controller) to a computer via an Android a phone. You can check out the Android app project here: https://github.com/tejashah88/Triforce-Control

### Why did you make it?
The primary purpose was that I wanted to play GameCube/Wii Games with the ADT-1 dev kit's game controller, and everything but the joysticks were working. Since the dev kit was "no longer available" about 3 months after I got it, and most solutions applied more for the newer NVIDIA shield TV controller or similar variants, I decided to create a psuedo-driver for the controller and my computer using an Android phone as a bridge.

### How does it work?
The project consists of 2 main parts: the Android app and the Server. The Android app is responsible for retrieving inputs from the game controller and relaying them to the server over web sockets, and the server is responsible for translating said inputs from the controller into keyboard/mouse inputs in order to emulate a GameCube controller.
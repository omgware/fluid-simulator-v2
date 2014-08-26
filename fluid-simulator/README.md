fluid-simulator
===============

Current revision: 1.0

Author: Simone Autore (aka Sippolo)




Viscoelastic fluid simulator in Java and OpenGL.
The software makes use of the most awesome cross-platform Java game/graphics development framework: LibGDX Framework.
Here is a link to the official website: http://libgdx.badlogicgames.com/

Note that the simulation is rendered on one CPU only.

BIG NOTE: currently the code could seem a bit messy, I didn't have time to make it pretty because of the huge testing I've been doing on it, so please forgive me for this ^^

- Paper source:

The simulation algorithm is inspired from the paper "Particle-based Viscoelastic Fluid Simulation" by Simon Clavet, Philippe Beaudoin, and Pierre Poulin, which can be found here: http://www.ligum.umontreal.ca/Clavet-2005-PVFS/pvfs.pdf

I've also made two videos of early revisions of this simulator (currently it's even more optimized and faster at runtime)
so you could have a look at how it works:

http://www.youtube.com/watch?v=i65V8J1P044

http://www.youtube.com/watch?v=9bdH3mB2LdY


- IDE Integration:

You should be able to import the project directly into Eclipse and compile it without issues.


- Android Platform

If you wish to run this simulator on Android platform, you'll have to create and compile a new Android Project making use of LibGDX, and just import this project as the main.
Make sure also to change the static variables within the main project source to make sure it works on Android without issues.


- Run instructions:

Make sure you have jre6+ installed, then:

32 bit OS: just run the FluidSimulator.jar file within the project root directory.

64 bit OS: please run this simulation on a 64bit jre by running the "FluidSimulator x64.bat" file, making sure to set the correct jre6+ x64 path within (just edit it with a plain text editor).
Note that the simulation is really faster and smoother on 64 bit environments, so if you have one, for your best experience you really want to run it on jre6+ x64.

Download Builds: you can find the zip file of the build in the Downloads section on this repository, just make sure you extract all the files before executing the jar/bat, otherwise it may not run at all.


- Commands

Keyboard commands:

When you start the simulator, you'll see instructions on the lower screen, with features followed by command key (within parenthesis) to activate them.

F1-F9 and 1-9: on the bottom there are parameters which can be tuned up and down with respectively F1-F9 and 1-9 keys on the keyboard.
Some simulation variables can be only tuned within the source code because they're statics, so you'll need to recompile the project in that case.

H: hide the "hud".

Backspace: clean everything and restart the simulator.

Mouse commands:

Left click WHILE moving the mouse: populate the screen with "drops" of fluid.

Right click: Attracts drops towards mouse pointer.

Middle click: Repulses drops outwards.

Right click + Middle click: creates a sort of circular ring with drops around mouse pointer.

Right click + Backspace: just remove the attracted drops around the mouse pointer.

That should be all!
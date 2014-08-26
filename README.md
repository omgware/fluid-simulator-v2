fluid-simulator-v2
==================

2D Fluid Simulator implementing all the main algorithms like SPH, MPM and PBF.

Current revision: 2.1

Author: Simone Autore (aka Sippolo)

2D Fluid simulator developed in Java and OpenGL. The software makes use of the awesome OpenGL LibGDX Framework. Here is a link to the official website: http://libgdx.badlogicgames.com/

This simulator implements the following solvers:

- MPM - Material Point Method
- SPH - Viscoelastic Smoothed Particle Hydrodynamics
- LIQUID - Heavily customized and optimized SPH (NOTE: Box2D two-way coupling currently works only with this solver)
- PBF - Position Based Fluid

You will find the config.properties and the v2.1 (as well as older versions) jar files in the binaries directory, use the properties to select the solver algorithm and simply execute the jar to test the simulator.

Note that the simulation is rendered on CPU only, precisely one core.

BIG NOTE: just like the fluid simulator v1, this project has always been under development and not much effort was put in making it very clean and beautiful, therefore please forgive me if you will find a lot of commented and unoptimized code, although I tried my best to maintain very good performance for a java simulation like this.

    Paper sources:

Viscoelastic Smoothed Particle Hydrodynamics - "Particle-based Viscoelastic Fluid Simulation" by Simon Clavet, Philippe Beaudoin, and Pierre Poulin, which can be found here: http://www.ligum.umontreal.ca/Clavet-2005-PVFS/pvfs.pdf
Material Point Method - credits and many thanks go to Grant Kot who implemented the original algorithm, which I have ported to Java. You can find his Github profile here: https://github.com/kotsoft
Position Based Fluids - from Mathias Muller and Miles Macklin, you can find the original article here: http://matthias-mueller-fischer.ch/publications/pbf_sig_preprint.pdf
Liquid Custom SPH - The basic idea comes from the LiquidTest offered in jbox2D (http://www.jbox2d.org/liquid/), although I heavily modified it, including viscoelasticity, plasticity, custom box2d coupling and other optimizations.

You can find various video clips of this project on my YouTube channel, for example:

http://www.youtube.com/watch?v=q-7Z8bRgAfM

http://www.youtube.com/watch?v=UKlkk3uCdJs

I will likely make a new video showing most of v2.1 features coming soon.

<b>IDE Integration</b>

You should be able to import the project directly into Eclipse and compile it without issues.

<b>Android Platform</b>

This release is mostly aimed to Desktop, but if you wish to run the simulator on Android platform, you just need to compile the android project against the Android SDK and LibGDX libraries (included). Also make sure to change the static variables within the main project source (mainly IS_DESKTOP=false) and you should be good to go.

<b>Run instructions</b>

Make sure you have jre6+ installed, then:

Open the config.properties file inside binaries folder, and set the solver algorithm to use.
Finally just run the FluidSimulator_v2.1.jar file within the binaries directory.
Also note that the config.properties only works with v2.1 jar file.

If you have both 32 and 64 bit Java jre installed on your machine: please run this simulation on a 64bit jre by running the "FluidSimulator x64.bat" file, making sure to set the correct jre6+ x64 path within (just edit it with a plain text editor) and the right fluid simulator jar. Note that the simulation is really faster and smoother on 64 bit environments, so if you have one, for your best experience you really want to run it on jre6+ x64.

Note that the hud is mostly tailored for 1920x1080 full screen resolution, but you can easily tweak it from the source code as you like.

<b>Commands</b>

Keyboard commands:

When you start the simulator, you'll see instructions on the lower/higher part of the screen, with features followed by command key (within parenthesis) to activate them.
The following commands might not be all available for every solver, please take a look at the commands listed in the "public boolean keyUp(int keycode)" method in each solver class to have a detailed overview of available commands for each solver.

F1-F9 and 1-9: on the bottom there are parameters which can be tuned up and down with respectively F1-F9 and 1-9 keys on the keyboard. Some simulation variables can be only tuned within the source code because they're statics, so you'll need to recompile the project in that case.

SPACE: Change particle color

P: Plasticity mode

V: Viscoelasticity mode

X: White background mode

B: Box2D Interaction mode

N: Refracting Shaders with colored background mode

S: Slow motion mode

E: Expanding mode

C: Crazy mode - 3D Viewer camera controller enable/disable (works when 3D Viewer is enabled on some solvers)

K: Smoke mode

R: Particle render enable/disable - 3D Viewer enable/disable

L: Lines mode

Q: Shapes mode

M: Fluid mass mode

PLUS/MINUS: Increase/Decrease gravity

UP/DOWN: Increase/Decrease emitter size

LEFT/RIGHT: Increase/Decrease Resolution/Radius

PAGE_UP/PAGE_DOWN: Increase/Decrease time step

H: hide/show the "hud".

Backspace: clean everything and restart the simulator - while right clicking it deletes only selected particles


Mouse commands:

Left click WHILE moving the mouse: populate the screen with "drops" of fluid - in MPM solver this also moves the particles.

Right click: Attracts drops towards mouse pointer.

Middle click: Repulses drops outwards.

Right click + Middle click: creates a sort of circular ring with drops around mouse pointer.

Right click + Backspace: just remove the attracted drops around the mouse pointer.



That should be all, hopefully deciding to open source this project will help many other developers in the journey of understanding fluid dynamics :)

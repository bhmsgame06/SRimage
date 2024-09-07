# SRimage
Tool for creating and converting PIM/PPL images from SolaRola game files.



During the working on image files for SolaRola you need program BFCTool at first, for extracting PIM/PPL files from .bfc files.
Download BFCTool from this repository: https://github.com/bhmsgame06/BFCTool
Let's get started.



At first, do some manipulations with .bfc files as shown [here](https://github.com/bhmsgame06/BFCTool/blob/main/README.md).
Find any PIM and PPL chain you want, for example, type commands to BFCTool CMD:
```
findfn gamelogo.pim
findfn gamelogo.ppl
```
You should see indices to these files, for me it's:
```
394 for gamelogo.pim
395 for gamelogo.ppl
```
Go to `head.cfg` file in the project directory and find `# INDEX=394` and `# INDEX=395` lines in the file.
And see for `PATH` key below `# INDEX=***` line.
```
PATH=11.bfc/394.bin
```
```
PATH=11.bfc/395.bin
```
Go to `ext_bfc` directory, then go to `11.bfc` directory, and pull `394.bin` and `395.bin` files out of there.
So, these `394.bin` and `395.bin` are literally `gamelogo.pim` and `gamelogo.ppl`.

Use SRimage program to convert PIM and PPL to single PNG file.
Use this command:
```
java -jar SRimage.jar --reverse 394.bin 395.bin
```

PNG will appear in the same folder where PIM file was.

To convert PNG to PIM/PPL chain, use this command:
```
java -jar SRimage.jar --make image.png
```

PIM and PPL files will appear in the same folder where PNG file was.


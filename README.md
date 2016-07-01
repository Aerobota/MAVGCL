# MAVGAnalysis

## In-Flight/PX4Log Analysis for PX4

[![Build Status](https://travis-ci.org/ecmnet/MAVGCL.svg?branch=master)](https://travis-ci.org/ecmnet/MAVGCL) [![Build status](https://ci.appveyor.com/api/projects/status/jqo0dnkcksaj6b3s?svg=true)](https://ci.appveyor.com/project/ecmnet/mavgcl)



This JavaFx based tool enables PX4 Users to record and analyse data published via UDP during flight or based on PX4Logs. It is not intended to replace the QGC. 

Any feedback, comments and contributions are very welcome.

**Status:** Last updated 01/07/2016 

- Binaries for Linux, OSX and Windows available 

**Features:**

- Realtime data acquisition (50ms sampling) based on MAVLink messages
- Timechart annotated by messages and parameter changes
- Trigger recording manually or by selectable flight-mode/state changes with adjustable stop-recording delay
- Display of  key-figures during and after recording (with 'Replay')
- XY Analysis for selected key-figures
- MAVLink inspector
- Easy to use parameter editor and In-Flight-Tuning-Widget
- Map viewer of global position and raw gps data with option to record path (cached)
- Import of selected key-figures from PX4Log (file or last log from device via WiFi)
- Save and load of collected data 
- FrSky Taranis USB supported in SITL
- Low latency MJPEG based video stream display based on [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14)  (recording and replay in preparation)

**Requirements:**

- requires **Java 8** JRE
- A companion proxy (either MAVComm or MAVROS, not required for PIXRacer)
- Video streaming requires  [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14) running on companion 

**Binaries:**

![alt tag](https://img.shields.io/github/release/ecmnet/MAVGCL.svg)

Binaries can be found [here](https://github.com/ecmnet/MAVGCL/releases).

**How to build on OSX** *(other platforms may need adjustments in* `build.xml`*)*:

- Clone repository
- Goto main directory  `cd MAVGCL-master/MAVGCL`
- Run `ant all`

**How to start after build  (all platforms):**

- Goto directory `/dist`
- Start with `java -jar MAVGAnalysis.jar`
- Set IP address and port in `File->Preferences` and restart (For local SITL use 127.0.0.1:14556 or start with `java -jar MAVGAnalysis.jar --SITL=true`)
- Open `demo_data.mgc`, import PX4Log file or collect data directly from your vehicle
- For video (mjpeg), setup  [uv4l](http://www.linux-projects.org/modules/sections/index.php?op=viewarticle&artid=14) at port 8080 on your companion with :
  ​
  `uv4l --auto-video_nr --sched-rr --mem-lock --driver uvc --server-option '--port=8080'`

**How to deploy on OSX:**

- Run `ant_deploy`


**Limitations:**

- Limited to one device (MAVLink-ID '1')
- Currently does not support USB or any serial connection (should be easy to add, so feel free to implement it)
- PX4Log keyfigure mapping not complete (let me know, which I should add)


**Note for developers:**

MAVGAnalysis depends heavily on https://github.com/ecmnet/MAVComm for MAVLink parsing.
​

**Screenshot**:

![alt tag](https://raw.github.com/ecmnet/MAVGCL/master/MAVGCL/screenshot7.png)



Please note the [License terms](https://github.com/ecmnet/MAVGCL/blob/master/MAVGCL/LICENSE.md).


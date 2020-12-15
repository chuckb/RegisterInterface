[![Release](https://jitpack.io/v/chuckb/RegisterInterface.svg)]
(https://jitpack.io/#chuckb/RegisterInterface)

# Register Interface Class for Alchitry FPGA - Java

This is a library sourced from the [Alchitry Labs site](https://alchitry.com/blogs/tutorials/register-interface) packaged for easy consumption within gradle or maven. It is used to communicate over USB serial in a master/slave configuration to an [Alchitry FPGA Board](https://www.sparkfun.com/search/results?term=alchitry).

It has been tested to work on Windows 10 and should work on [OSX with some workarounds](https://forum.alchitry.com/thread-264.html?highlight=mac).

## Usage

In your build.gradle file:
```
allprojects {
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
}
dependencies {
    implementation 'com.chuckstechtalk:RegisterInterface:1.0.0'
}
```

Next, import like so:
```Java
import com.chuckstechtalk.alchitry.*;
```

Finally, instantiate:
```Java
RegisterInterface reg = new RegisterInterface();
reg.connect("COMx", 1000000);
```

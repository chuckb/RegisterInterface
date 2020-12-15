[![](https://jitpack.io/v/com.chuckstechtalk/RegisterInterface.svg)](https://jitpack.io/#com.chuckstechtalk/RegisterInterface)

# Register Interface Class for Alchitry FPGA - Java

This is a library sourced from the [Alchitry Labs site](https://alchitry.com/blogs/tutorials/register-interface) packaged for easy consumption within gradle or maven. It is used to communicate over USB serial in a master/slave configuration to an [Alchitry FPGA Board](https://www.sparkfun.com/search/results?term=alchitry).

It has been tested to work on Windows 10 and should work on [OSX with some workarounds](https://forum.alchitry.com/thread-264.html?highlight=mac).

See [Javadocs.](https://jitpack.io/com/chuckstechtalk/RegisterInterface/latest/javadoc/)

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
    implementation 'com.chuckstechtalk:RegisterInterface:1.+'
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
// Write the value 10 to register 1
reg.write(1, 10);
```

## Dependencies

This lib uses the [jssc serial library](https://github.com/java-native/jssc) and it is automatically included as a dependency, so you should not need to do so in your project. This caveat is here because I am not sure if all native binaries are included in the resulting dist jar. Certainly the on the target in which your app is built, the jars will include the appropriate native binaries.
# FrameCamera [![](https://jitpack.io/v/fukutan-org/FrameCamera.svg)](https://jitpack.io/#fukutan-org/FrameCamera) [![Apache License](http://img.shields.io/badge/license-Apache-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) [![](http://img.shields.io/badge/APILevel-22orlater-red.svg?style=flat)](https://github.com/fukutan-org/FrameCamera)

Wrapper library of camera2 api to use easily.

## Description

Are you looking for a lightweight camera library that you can use in your application?  
We recommend this library.
Be able to make picture with frame and support clipping of photo.


## Feature

* make picture with frame and decoration.  
* support switching camera direction for front and back.  
* Touch focus, auto focus, continuous shooting.  
* File explorer

(above is not created yet but will add soon...maybe)

## Requirements
Android API level 22 or later.
Front camera or Back camera.

## How to install

Add it in your root build.gradle at the end of repositories:
```groovy
    allprojects {
    		repositories {
    			...
    			maven { url 'https://jitpack.io' }
    		}
    }
```

Add the dependency
```groovy
    dependencies {
    		implementation 'com.github.fukutan-org:FrameCamera:0.1.0'
    }
```
(latest version [![Release](https://jitpack.io/v/fukutan-org/FrameCamera.svg)](https://jitpack.io/#fukutan-org/FrameCamera))

## Usage

it simply call method to launch camera activity. need activity instance for argument.
```groovy
	CameraActivity.startCameraActivity(this)

```

how to get result picture?  
call below method as single line within activity result
```groovy
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

	val bmp = CameraActivity.getResult(requestCode, resultCode, data)
    }
```


## License
Copyright 2019 fukutan-org.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    (http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
# FrameCamera

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

(above is not creat yet but will add soon...maybe)


## usage

it simply call method to launch camera activity. need activity instance for argument.
```groovy
	CameraActivity.startCameraActivity(this)

```

how to get result picture?  
simply call method as single line on activity result!
```groovy
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

	val bmp = CameraActivity.getResult(requestCode, resultCode, data)
    }
```
  
### Support 
Android API level 21 or later.  
front camera, back camera.
